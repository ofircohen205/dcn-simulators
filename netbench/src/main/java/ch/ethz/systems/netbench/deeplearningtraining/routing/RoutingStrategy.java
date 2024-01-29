package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public abstract class RoutingStrategy {

    protected final Map<Long, Flow> flowToCommodityMap = new HashMap<>();
    protected final GraphDetails graphDetails;

    public RoutingStrategy() {
        graphDetails = Simulator.getConfiguration().getGraphDetails();
    }

    public void addFlow(Flow flow) {
        flowToCommodityMap.put(flow.getFlowId(), flow);
    }

    public final void assignStartFlow(Flow flow) {
        Job job = Simulator.getJob(flow.getJobId());
        List<Integer> path = assignSinglePath(flow);
        job.getCommoditiesPaths().put(ImmutablePair.of(flow.getSrcId(), flow.getDstId()), path);

    }

    protected abstract List<Integer> assignSinglePath(Flow flow);

    public List<Integer> getCoreIds() {
        List<Integer> coreIds = new ArrayList<>(graphDetails.getCoreNodeIds());
        if (!graphDetails.getFailedCores().isEmpty()) {
            Set<Integer> failedCoreIds = graphDetails.getFailedCores();
            coreIds.removeAll(failedCoreIds);
        }
        return coreIds;
    }


}
