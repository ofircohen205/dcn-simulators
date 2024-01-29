package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.AssignmentsDurationLogger;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public abstract class CentralizedController extends RoutingStrategy {

    protected final List<Long> durations;
    protected final Set<Long> activeFlows;
    protected AssignmentsDurationLogger logger;

    public CentralizedController() {
        activeFlows = new HashSet<>();
        durations = new ArrayList<>();
        logger = new AssignmentsDurationLogger(this);
    }

    @Override
    public List<Integer> assignSinglePath(Flow flow) {
        activeFlows.add(flow.getFlowId());
        Job job = Simulator.getJob(flow.getJobId());
        ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(flow.getSrcId(), flow.getDstId());
        return job.getCommoditiesPaths().get(commodity);
    }

    public abstract void addCommodity(Flow flow);


    public void clearResources(Flow flow) {
        activeFlows.remove(flow.getFlowId());
    }

    public abstract void determinePathAssignments();

    public AssignmentsDurationLogger getLogger() {
        return logger;
    }

    public double getAverageDuration() {
        return durations.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public int getNumAssignedCommodities() {
        Collection<Job> jobs = Simulator.getJobs().values();
        return jobs.stream().mapToInt(job -> job.getCommoditiesPaths().size()).sum();
    }
}
