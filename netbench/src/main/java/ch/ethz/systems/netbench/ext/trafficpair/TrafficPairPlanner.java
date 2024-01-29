package ch.ethz.systems.netbench.ext.trafficpair;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.run.traffic.JobArrivalEvent;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TrafficPairPlanner extends TrafficPlanner {

    private final long flowSizeByte;
    private final boolean useFile;
    private String baseDir;
    private List<TrafficPair> trafficPairs;

    /**
     * Constructor.
     * <p>
     * File should have the following structure:
     *
     * <pre>
     *     [srcId] [dstId]
     *     [srcId] [dstId]
     *     ...
     *     [srcId] [dstId]
     * </pre>
     *
     * @param baseTrafficPairsDir Base Directory of the traffic pairs files
     */
    public TrafficPairPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, String baseTrafficPairsDir,
                              long flowSizeByte, NBProperties configuration) {
        super(idToTransportLayerMap, configuration);
        this.baseDir = baseTrafficPairsDir;
        this.useFile = true;
        SimulationLogger.logInfo("Flow planner", "TRAFFIC_PAIR_FILE");
        this.flowSizeByte = flowSizeByte;
    }

    /**
     * Constructor.
     *
     * @param trafficPairs Traffic pairs
     */
    public TrafficPairPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, List<TrafficPair> trafficPairs,
                              long flowSizeByte, NBProperties configuration) {
        super(idToTransportLayerMap, configuration);
        this.trafficPairs = trafficPairs;
        this.useFile = false;
        this.flowSizeByte = flowSizeByte;
        SimulationLogger.logInfo("Flow planner",
                "TRAFFIC_PAIR_LIST(flowSizeByte=" + flowSizeByte + ", pairs=" + trafficPairs + ")");
    }

    /**
     * Generate all-to-one traffic pairs.
     *
     * @param configuration from properties file
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateAllToOne(NBProperties configuration) {
        ArrayList<TrafficPair> ls = new ArrayList<>();

        // Randomly choose target server
        long seed = Long.parseLong(configuration.getPropertyOrFail(Constants.Simulation.SEED));
        Random random = new Random(seed);
        GraphDetails details = configuration.getGraphDetails();
        List<Integer> serverNodeIds = new ArrayList<>(details.getServerNodeIds());
        Collections.shuffle(serverNodeIds, random);
        int target = serverNodeIds.get(0);
        System.out.println("Target server: " + target);

        // Filter pairs based on ToR with multiple servers or ToR as server
        Predicate<Integer> filterPredicate = getFilterPredicateTrafficPair(details, target);
        details.getServerNodeIds().stream().filter(filterPredicate)
                .forEach(serverId -> ls.add(new TrafficPair(serverId, target)));
        configuration.setProperty(Constants.Simulation.TARGET_SERVER, String.valueOf(target));
        return ls;
    }

    /**
     * Generate all-to-one traffic pairs.
     *
     * @param configuration from properties file
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateAllToOneTor(NBProperties configuration) {
        // Randomly choose target server
        GraphDetails details = configuration.getGraphDetails();
        List<Integer> torNodeIds = new ArrayList<>(details.getTorNodeIds());
        int targetTor = torNodeIds.get(torNodeIds.size() - 1);
        System.out.println("Target ToR: " + targetTor);
        List<TrafficPair> ls = getSourceServersAndTargetServers(details, targetTor, false);
        configuration.setProperty(Constants.Simulation.TARGET_TOR, String.valueOf(targetTor));
        return ls;
    }

    /**
     * Generate one-to-all traffic pairs.
     *
     * @param configuration from properties file
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateOneToAll(NBProperties configuration) {
        ArrayList<TrafficPair> ls = new ArrayList<>();

        // Randomly choose target server
        long seed = Long.parseLong(configuration.getPropertyOrFail(Constants.Simulation.SEED));
        Random random = new Random(seed);
        GraphDetails details = configuration.getGraphDetails();
        List<Integer> serverNodeIds = new ArrayList<>(details.getServerNodeIds());
        Collections.shuffle(serverNodeIds, random);
        int source = serverNodeIds.get(0);
        System.out.println("Source server: " + source);

        // Filter pairs based on ToR with multiple servers or ToR as server
        Predicate<Integer> filterPredicate = getFilterPredicateTrafficPair(details, source);
        details.getServerNodeIds().stream().filter(filterPredicate)
                .forEach(serverId -> ls.add(new TrafficPair(source, serverId)));
        configuration.setProperty(Constants.Simulation.SOURCE_SERVER, String.valueOf(source));
        return ls;
    }

    /**
     * Generate one-to-all traffic pairs.
     *
     * @param configuration from properties file
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateOneToAllTor(NBProperties configuration) {
        // Randomly choose target server
        GraphDetails details = configuration.getGraphDetails();
        List<Integer> torNodeIds = new ArrayList<>(details.getTorNodeIds());
        int sourceTor = torNodeIds.get(0);
        System.out.println("Source ToR: " + sourceTor);
        List<TrafficPair> ls = getSourceServersAndTargetServers(details, sourceTor, true);
        configuration.setProperty(Constants.Simulation.SOURCE_TOR, String.valueOf(sourceTor));
        return ls;
    }

    /**
     * Generate stride traffic pairs.
     *
     * @param n      Total number of nodes
     * @param stride Stride
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateStride(int n, int stride) {
        // For each other server, create pair to target
        GraphDetails details = Simulator.getConfiguration().getGraphDetails();
        int start = getStartFromTrafficPair(details);
        if (start > 0) {
            n += details.getNumTors();
        }
        ArrayList<TrafficPair> ls = new ArrayList<>();
        for (int from = 0; from < n; from++) {
            ls.add(new TrafficPair(from, (from + stride) % n));
        }
        return ls;
    }

    /**
     * Generate all-to-all traffic pairs.
     *
     * @param configuration file from properties
     * @return Traffic pair list
     */
    public static List<TrafficPair> generateAllToAll(NBProperties configuration) {
        // For each other server, create pair to target
        ArrayList<TrafficPair> ls = new ArrayList<>();
        for (Integer i : configuration.getGraphDetails().getServerNodeIds()) {
            for (Integer j : configuration.getGraphDetails().getServerNodeIds()) {
                if (!i.equals(j)) {
                    ls.add(new TrafficPair(i, j));
                }
            }
        }
        return ls;
    }

    /**
     * Generate single traffic pairs of src-dst.
     *
     * @param configuration from properties file
     * @return Traffic pair list of size 1
     */
    public static List<TrafficPair> generateOneToOne(NBProperties configuration) {
        ArrayList<TrafficPair> ls = new ArrayList<>();
        GraphDetails details = configuration.getGraphDetails();
        Set<Integer> serverNodeIds = details.getServerNodeIds();
        long seed = Long.parseLong(configuration.getPropertyOrFail(Constants.Simulation.SEED));
        Random random = new Random(seed);
        int source = random.nextInt(serverNodeIds.size());
        int dst;
        do {
            dst = random.nextInt(serverNodeIds.size());
        } while (dst != source);
        configuration.setProperty(Constants.Simulation.SOURCE_SERVER, String.valueOf(source));
        configuration.setProperty(Constants.Simulation.TARGET_SERVER, String.valueOf(dst));
        System.out.println("Source server: " + source + ", Target server: " + dst);
        ls.add(new TrafficPair(source, dst));
        return ls;
    }

    public static int getStartFromTrafficPair(GraphDetails details) {
        return details.isMultipleServersUnderToR() ? details.getNumTors() : 0;
    }

    public static Predicate<Integer> getFilterPredicateTrafficPair(GraphDetails details, int chosenServerId) {
        Predicate<Integer> filterPredicate;
        Predicate<Integer> filterSameIds = serverId -> serverId != chosenServerId;
        if (details.isMultipleServersUnderToR()) {
            Set<Integer> targetTorServers = details.getServersOfTor(details.getTorIdOfServer(chosenServerId));
            Predicate<Integer> filterServersUnderSameToR = serverId -> !targetTorServers.contains(serverId);
            Predicate<Integer> filterServerSameModulus = serverId -> serverId
                    % targetTorServers.size() == chosenServerId % targetTorServers.size();
            filterPredicate = filterSameIds.and(filterServersUnderSameToR).and(filterServerSameModulus);
        } else {
            filterPredicate = filterSameIds;
        }
        return filterPredicate;
    }

    public static List<TrafficPair> getSourceServersAndTargetServers(GraphDetails details, int torId,
                                                                     boolean isSource) {
        String sourceServers = Constants.Simulation.SOURCE_SERVERS;
        String targetServers = Constants.Simulation.TARGET_SERVERS;
        List<TrafficPair> ls = new ArrayList<>();
        Map<String, List<Integer>> map = new HashMap<>();
        map.put(sourceServers, isSource ? new ArrayList<>(details.getServersOfTor(torId)) : new ArrayList<>());
        map.put(targetServers, isSource ? new ArrayList<>() : new ArrayList<>(details.getServersOfTor(torId)));
        for (Integer serverId : details.getServerNodeIds()) {
            if (map.get(isSource ? sourceServers : targetServers).contains(serverId)) {
                continue;
            }
            if (serverId % map.get(isSource ? sourceServers : targetServers).size() == 0) {
                map.get(isSource ? targetServers : sourceServers).add(serverId);
            }
        }
        for (int i = 0; i < map.get(isSource ? targetServers : sourceServers).size(); i++) {
            int sourceServerId = map.get(sourceServers).get(i);
            int targetServerId = map.get(targetServers).get(i);
            ls.add(new TrafficPair(sourceServerId, targetServerId));
        }
        return ls;
    }

    @Override
    public void createPlan(long durationNs) {
        String parallelType = configuration.getPropertyOrFail(Constants.TrafficPair.TRAFFIC_PAIR_TYPE);
        File baseDirFile = new File(baseDir);
        if (!baseDirFile.exists()) {
            throw new RuntimeException("Base traffic pairs dir does not exist: "
                    + baseDirFile.getAbsolutePath());
        }
        File[] files = baseDirFile.listFiles();
        if (files == null) {
            throw new RuntimeException("Base traffic pairs dir is empty: "
                    + baseDirFile.getAbsolutePath());
        }
        for (File file : Arrays.stream(files).filter(file -> file.getName().endsWith(".txt")).collect(Collectors.toList())) {
            try {
                buildJob(file, parallelType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void buildJob(File file, String parallelType) throws Exception {
        // Open input stream
        String[] parts = file.getName().split("-");
        assert parts.length == 2;
        String modelName = parts[1].replace(".txt", "");
        FileReader input = new FileReader(file.getAbsolutePath());
        BufferedReader br = new BufferedReader(input);

        int jobId = Integer.parseInt(parts[0].split("_")[1]);

        long startTime = 0;
        long flowSizeByte = 0;
        int numMiniBatches = 1;
        int microBatchSize = 8;
        long computeTime = 0;
        int stageIndex;

        Set<Integer> tors = new HashSet<>();
        Map<Integer, Set<Flow>> stageFlows = new HashMap<>();


        // Simply read in the node pairs
        String strLine;
        while ((strLine = br.readLine()) != null) {
            if (strLine.startsWith("#")) {
                continue;
            }
            // Split up sentence
            String[] match = strLine.split(" ");

            if (match.length != 8) {
                throw new IllegalArgumentException("Invalid line: " + strLine);
            }

            int srcTorId = Integer.parseInt(match[2]);
            int dstTorId = Integer.parseInt(match[3]);
            tors.add(srcTorId);
            tors.add(dstTorId);

            int srcId = Integer.parseInt(match[0]);
            int dstId = Integer.parseInt(match[1]);
            stageIndex = Integer.parseInt(match[7]);
            stageFlows.putIfAbsent(stageIndex, new HashSet<>());
            Flow flow = new Flow(srcId, dstId, flowSizeByte, jobId);
            Simulator.getFlowIdToFlow().put(flow.getFlowId(), flow);
            stageFlows.get(stageIndex).add(flow);

            startTime = Long.parseLong(match[4]);
            flowSizeByte = Long.parseLong(match[5]);
            computeTime = Long.parseLong(match[6]);

            // Register the flow
//            registerFlow(startTime, srcId, dstId, flowSizeByte, jobId);
        }
        // Close input stream
        br.close();
        if (tors.size() < 2) {
            return;
        }

        // Create job
        Job job = new Job(jobId, modelName, parallelType, flowSizeByte, computeTime, startTime, numMiniBatches, microBatchSize);
        Simulator.getJobs().put(jobId, job);
        stageFlows.forEach((stage, flows) -> {
            flows.forEach(flow -> {
                ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(flow.getSrcId(), flow.getDstId());
                job.getCommoditiesFlowsMap().putIfAbsent(commodity, new ArrayList<>());
                job.getCommoditiesFlowsMap().get(commodity).add(flow);
            });
        });
        job.initWrapper();
        Simulator.registerEvent(new JobArrivalEvent(startTime, jobId, stageFlows));
    }

    public static class TrafficPair {
        private final int from;
        private final int to;

        public TrafficPair(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public String toString() {
            return "(" + from + ", " + to + ")";
        }
    }

}
