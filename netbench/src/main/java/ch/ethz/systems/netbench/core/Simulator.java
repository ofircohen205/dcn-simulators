package ch.ethz.systems.netbench.core;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.FlowLogger;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.random.RandomManager;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.core.state.SimulatorStateSaver;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.deeplearningtraining.routing.CentralizedController;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import org.json.simple.JSONObject;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The simulator is responsible for offering general
 * services to all other interacting parts, such as
 * configurations and current time management. It uses
 * a deterministic priority queue to precisely execute
 * event after event.
 */
public class Simulator {
    private static final Set<Long> finishedFlows = new HashSet<>();
    private static final Set<Long> activeFlows = new HashSet<>();
    private static final Map<Integer, Job> jobs = new HashMap<>();
    private static final Map<Long, Flow> flowIdToFlow = new HashMap<>();
    // Main ordered event queue (run variable)
    private static PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    // Current time in ns in the simulation (run variable)
    private static long now;
    // Threshold to end
    private static long finishFlowIdThreshold;
    // Whether the simulator is setup
    private static boolean isSetup = false;
    private static long totalRuntimeNs;
    // Randomness manager
    private static RandomManager randomManager;
    // Configuration
    private static NBProperties configuration;
    // Traffic planner
    private static TrafficPlanner trafficPlanner;
    private static Map<Integer, NetworkDevice> idToNetworkDevice;
    private static long flowIdCounter = 0;

    private Simulator() {
        // Static class only
    }

    /**
     * Retrieve the configuration.
     * <p>
     * This contains two categories.
     * <p>
     * (A) Solely data about a specific run,
     * e.g. topology, run time, link type,
     * protocol, etc.
     * <p>
     * (B) Settings for the internal workings, e.g. TCP retransmission
     * time-out, flowlet gap, and model parameters in general.
     *
     * @return Run configuration properties
     */
    public static NBProperties getConfiguration() {
        return configuration;
    }

    /**
     * Setup the simulator with only a seed, leaving out
     * a run configuration specification.
     *
     * @param seed Random seed (set 0 for random)
     */
    public static void setup(long seed) {
        setup(seed, null);
    }

    /**
     * Setup the simulator by giving random seed.
     * Completely resets the simulation by clearing the event queue
     * and resetting the simulation epoch to zero.
     * Also, loads in the default internal configuration.
     *
     * @param seed          Random seed (set 0 for random)
     * @param configuration Configuration instance (set null if there is no
     *                      configuration (an empty one), e.g. in tests)
     */
    public static void setup(long seed, NBProperties configuration) {
        // Prevent double setup
        if (isSetup) {
            throw new RuntimeException(
                    "The simulator can only be setup once. Call reset() before setting it up again.");
        }

        // Open simulation logger
        SimulationLogger.open(configuration);

        // Setup random seed
        if (seed == 0) {
            seed = new Random().nextLong();
            SimulationLogger.logInfo("Seed randomly chosen", "TRUE");
        } else {
            SimulationLogger.logInfo("Seed randomly chosen", "FALSE");
        }
        SimulationLogger.logInfo("Seed", String.valueOf(seed));
        randomManager = new RandomManager(seed);

        // Internal state reset
        now = 0;
        eventQueue.clear();

        // Configuration
        Simulator.configuration = configuration;

        restoreState();

        // It is now officially setup
        isSetup = true;
    }

    private static void restoreState() {
        if (configuration == null) {
            // this can happen in tests.
            return;
        }
        String folderName = configuration.getPropertyWithDefault(Constants.Simulation.FROM_STATE, null);
        if (folderName != null) {
            JSONObject json = SimulatorStateSaver.loadJson(folderName + "/" + "simulator_data.json");
            now = (long) json.get("now");
            eventQueue = (PriorityQueue<Event>) SimulatorStateSaver
                    .readObjectFromFile(folderName + "/" + "simulator_queue.ser");
            TransportLayer.restorState(configuration);
            System.out.println("Done restoring simulator");
        }
    }

    /**
     * Create a random number generator which guarantees the same sequence
     * when the same universal seed is fed in <i>setup()</i>.
     * <p>
     * This is used when a section wants to be independent of whatever configuration
     * has come before it that may have used the universal random number generator.
     * An example is the flow generation.
     *
     * @param name Name of the independent random number generator
     * @return Independent random number generator
     */
    public static Random selectIndependentRandom(String name) {
        return randomManager.getRandom(name);
    }

    /**
     * Run the simulator for the specified amount of time.
     *
     * @param runtimeNanoseconds Running time in ns
     */
    public static void runNs(long runtimeNanoseconds) {
        runNs(runtimeNanoseconds, -1);
    }

    public static long getTotalRunTimeNs() {
        return totalRuntimeNs;
    }

    /**
     * Run the simulator for at most the specified amount of time, or
     * until the first N active_flows have been finished.
     *
     * @param runtimeNanoseconds     Running time in ns
     * @param flowsFromStartToFinish Number of active_flows from start to finish
     *                               (e.g. 40000 will make the simulation
     *                               run until all active_flows with identifier <
     *                               40000 to finish, or until the runtime
     *                               is exceeded)
     */
    public static void runNs(long runtimeNanoseconds, long flowsFromStartToFinish) {
        // Reset run variables (queue is not cleared because it has to start somewhere,
        // e.g. flow start events)
        // now = 0;
        totalRuntimeNs = runtimeNanoseconds;
        // NonblockingBufferedReader reader = new NonblockingBufferedReader(System.in);
        // Finish flow threshold, if it is negative the flow finish will be very far in
        // the future
        finishFlowIdThreshold = flowsFromStartToFinish;
        if (flowsFromStartToFinish <= 0) {
            flowsFromStartToFinish = Long.MAX_VALUE;
        }
        // Time interval at which to show the percentage of progress (set to be shown
        // every 1s)
        long PROGRESS_SHOW_INTERVAL_NS = 1000000000L;

        // Log start
        System.out.println("Starting simulation (total time: " + runtimeNanoseconds + "ns);...");

        // Time loop
        long startTime = System.currentTimeMillis();
        long realTime = System.currentTimeMillis();
        long nextProgressLog = PROGRESS_SHOW_INTERVAL_NS;
        boolean endedDueToFlowThreshold = false;

        while (!eventQueue.isEmpty() && now <= runtimeNanoseconds) {
            // Go to next event
            Event event = eventQueue.peek();
            now = event.getTime();
            if (now <= runtimeNanoseconds) {
                eventQueue.poll();
                event.trigger();

                if (event.retrigger()) {
                    registerEvent(event);
                }
            }

            // Log elapsed time
            if (now > nextProgressLog) {
                nextProgressLog += PROGRESS_SHOW_INTERVAL_NS;
                long realTimeNow = System.currentTimeMillis();

                BigDecimal intervalPrintValue = BigDecimal
                        .valueOf((double) PROGRESS_SHOW_INTERVAL_NS / (double) 1_000_000_000);
                System.out.println("Elapsed "
                        + intervalPrintValue.toPlainString()
                        + "s simulation in "
                        + ((realTimeNow - realTime) / 1000.0)
                        + "s real.");
                realTime = realTimeNow;

                if (RemoteRoutingController.getInstance() != null) {
                    System.out.print(RemoteRoutingController.getInstance().getCurrentState());
                }
                List<FlowLogger> crossingVirtualLinks = SimulationLogger.getActiveFlowLoggerByFlowId().values().stream()
                        .filter(flowLogger -> flowLogger.getSourceTorId() != flowLogger.getTargetTorId())
                        .sorted(Comparator.comparing(FlowLogger::getFlowId)).collect(Collectors.toList());
                System.out.println("Active commodities: ");
                for (FlowLogger flowLogger : crossingVirtualLinks) {
                    System.out.println(flowLogger);
                }
                System.out.println("Jobs:");
                for (Job job : jobs.values()) {
                    System.out.println(job);
                }
                System.out.println("--------------------------------------------------");
            }

            if (finishedFlows.size() >= flowsFromStartToFinish) {
                endedDueToFlowThreshold = true;
                break;
            }
        }
        // Make sure run ends at the final time if it ended because there were no
        // more events or the runtime was exceeded
        if (!endedDueToFlowThreshold) {
            now = runtimeNanoseconds;
        }
        // If there are no more events, set the current time to the end of the
        // simulation
        if (eventQueue.isEmpty()) {
            runtimeNanoseconds = now;
        }
        // Log end
        System.out.println("Simulation finished (simulated " + (runtimeNanoseconds / 1e9) + "s in a real-world time of "
                + ((System.currentTimeMillis() - startTime) / 1000.0) + "s).");
    }

    /**
     * Register to the simulator that a flow has been finished.
     *
     * @param flowId Flow identifier
     */
    public static void registerFlowFinished(long flowId) {
        if (flowId < finishFlowIdThreshold) {
            finishedFlows.add(flowId);
        }
        FlowLogger flowLogger = SimulationLogger.getFlowLoggerByFlowId().get(flowId);
        int jobId = flowLogger.getJobId();
        Job job = jobs.get(jobId);
        if (job.getRoutingStrategy() instanceof CentralizedController) {
            CentralizedController centralizedController = (CentralizedController) job.getRoutingStrategy();
            centralizedController.clearResources(flowIdToFlow.get(flowId));
        }
        SimulationLogger.logFinishedFlow(flowLogger);
    }

    /**
     * Update the simulator that a job needs to be updated.
     *
     * @param flowId Flow identifier
     */
    public static void updateJob(long flowId) {
        FlowLogger flowLogger = SimulationLogger.getFlowLoggerByFlowId().get(flowId);
        Job job = jobs.get(flowLogger.getJobId());
        job.update(flowIdToFlow.get(flowId));
    }

    /**
     * Register an event in the simulation.
     *
     * @param event Event instance
     */
    public static void registerEvent(Event event) {
        eventQueue.add(event);
    }

    public static void registerFlow(long flowId) {
        activeFlows.add(flowId);
    }

    public static void registerJob(Job job) {
        jobs.put(job.getJobId(), job);
    }

    /**
     * Retrieve the current time plus the amount of nanoseconds specified.
     * This is used to plan events in the future.
     *
     * @param nanoseconds Amount of nanoseconds from now
     * @return Time in nanoseconds
     */
    public static long getTimeFromNow(long nanoseconds) {
        return now + nanoseconds;
    }

    /**
     * Retrieve the current time in nanoseconds since simulation start.
     *
     * @return Current time in nanoseconds
     */
    public static long getCurrentTime() {
        return now;
    }

    /**
     * Retrieve the amount of events currently in the event queue.
     *
     * @return Number of events
     */
    public static int getEventSize() {
        return eventQueue.size();
    }

    /**
     * Retrieve the current event queue in the simulation.
     *
     * @return Event Queue
     */
    public static PriorityQueue<Event> getEventQueue() {
        return eventQueue;
    }

    /**
     * Clean up everything of the simulation.
     */
    public static void reset() {
        reset(true);
    }

    /**
     * Clean up everything of the simulation.
     *
     * @param throwawayLogs True iff the logs should be thrown out
     */
    public static void reset(boolean throwawayLogs) {
        // Close logger
        if (throwawayLogs) {
            SimulationLogger.closeAndThrowaway();
        } else {
            SimulationLogger.close();
        }

        // Reset random number generation
        randomManager = null;

        // Reset any run variables
        now = 0;
        eventQueue.clear();
        finishedFlows.clear();
        TransportLayer.staticReset();
        finishFlowIdThreshold = -1;

        // Reset configuration
        configuration = null;
        // No longer setup
        isSetup = false;
    }

    public static void dumpState(String dumpFolderName) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(dumpFolderName + "/" + "simulator_queue.ser"));

        oos.writeObject(eventQueue);
        System.out.println("Done writing queue");
        JSONObject obj = new JSONObject();
        obj.put("now", now);
        FileWriter file = new FileWriter(dumpFolderName + "/" + "simulator_data.json");
        file.write(obj.toJSONString());
        file.flush();

    }

    public static NetworkDevice getNetworkDevice(int id) {
        return Simulator.idToNetworkDevice.get(id);
    }

    public static void setIdToNetworkDevice(Map<Integer, NetworkDevice> idToNetworkDevice) {
        Simulator.idToNetworkDevice = idToNetworkDevice;
    }

    public static TrafficPlanner getTrafficPlanner() {
        return trafficPlanner;
    }

    public static void setTrafficPlanner(TrafficPlanner trafficPlanner) {
        Simulator.trafficPlanner = trafficPlanner;
    }

    public static Map<Integer, Job> getJobs() {
        return jobs;
    }

    public static Job getJob(int jobId) {
        return jobs.get(jobId);
    }

    public static Map<Integer, Job> getActiveJobs() {
        Map<Integer, Job> activeJobs = new HashMap<>();
        for (Job job : jobs.values()) {
            if (!job.isComputeMode()) {
                activeJobs.put(job.getJobId(), job);
            }
        }
        return activeJobs;
    }

    public static long getNextFlowId() {
        return flowIdCounter++;
    }

    public static Map<Long, Flow> getFlowIdToFlow() {
        return flowIdToFlow;
    }
}
