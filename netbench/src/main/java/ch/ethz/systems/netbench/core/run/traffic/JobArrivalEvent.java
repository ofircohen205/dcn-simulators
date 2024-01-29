package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.deeplearningtraining.routing.CentralizedController;
import ch.ethz.systems.netbench.deeplearningtraining.routing.RoutingStrategy;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.ext.trafficpair.TrafficPairPlanner;

import java.util.Map;
import java.util.Set;

public class JobArrivalEvent extends Event {

    private final int jobId;
    private final long startTime;
    private final Map<Integer, Set<Flow>> stageFlows;
    private final RoutingStrategy routingStrategy;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs Time it will take before happening from now in nanoseconds
     */
    public JobArrivalEvent(long timeFromNowNs, int jobId, Map<Integer, Set<Flow>> stageFlows) {
        super(timeFromNowNs);
        this.jobId = jobId;
        this.startTime = Simulator.getCurrentTime() + timeFromNowNs;
        this.stageFlows = stageFlows;
        this.routingStrategy = Simulator.getJobs().get(jobId).getRoutingStrategy();
    }

    @Override
    public void trigger() {
        Job job = Simulator.getJob(jobId);
        job.initializeEpoch(startTime, stageFlows);
        if (routingStrategy instanceof CentralizedController) {
            CentralizedController centralizedController = (CentralizedController) routingStrategy;
            stageFlows.values().forEach(flows -> flows.forEach(centralizedController::addCommodity));
            centralizedController.determinePathAssignments();
            centralizedController.getLogger().saveInfo(
                    centralizedController.getAverageDuration(),
                    centralizedController.getNumAssignedCommodities()
            );
        }

        stageFlows.values().forEach(flows -> {
            flows.forEach(flow -> {
                TrafficPlanner trafficPlanner = Simulator.getTrafficPlanner();
                assert trafficPlanner instanceof TrafficPairPlanner;
                TrafficPairPlanner trafficPairPlanner = (TrafficPairPlanner) trafficPlanner;
                trafficPairPlanner.registerFlow(1, flow.getSrcId(), flow.getDstId(), job.getFlowSize(), jobId);
            });
        });
    }

}
