package ch.ethz.systems.netbench.deeplearningtraining.utils;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.deeplearningtraining.routing.CentralizedController;
import ch.ethz.systems.netbench.deeplearningtraining.routing.RoutingStrategy;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.ext.trafficpair.TrafficPairPlanner;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class ParallelismUtils {

    public static void triggerNewFlow(Job job, int stageIndex, ImmutablePair<Integer, Integer> commodity, long nextStartTime) {
        int srcNodeId = commodity.getLeft();
        int dstNodeId = commodity.getRight();
        List<Flow> commodityFlows = job.getCommoditiesFlowsMap().get(commodity);
        long flowSize = commodityFlows.get(commodityFlows.size() - 1).getFlowSize();
        Flow newFlow = new Flow(commodity.getLeft(), commodity.getRight(), flowSize, job.getJobId());
        TrafficPlanner trafficPlanner = Simulator.getTrafficPlanner();
        assert trafficPlanner instanceof TrafficPairPlanner;
        TrafficPairPlanner trafficPairPlanner = (TrafficPairPlanner) trafficPlanner;
        trafficPairPlanner.registerFlow(nextStartTime, srcNodeId, dstNodeId, flowSize, job.getJobId());


        RoutingStrategy routingStrategy = job.getRoutingStrategy();
        if (routingStrategy instanceof CentralizedController) {
            ((CentralizedController) routingStrategy).addCommodity(newFlow);
        }
        job.getCommoditiesFlowsMap().get(commodity).add(newFlow);
        job.getCurrentEpoch().getStageFlows().get(stageIndex).add(newFlow);
    }

}
