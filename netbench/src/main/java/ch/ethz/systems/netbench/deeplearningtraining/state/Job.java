package ch.ethz.systems.netbench.deeplearningtraining.state;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.JobLogger;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.deeplearningtraining.routing.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;


public class Job {
    private final int jobId;
    private final String modelName;
    private final String parallelizationType;
    private final List<JobEpoch> epochs;
    private final Map<ImmutablePair<Integer, Integer>, List<Flow>> commoditiesFlowsMap;
    private final Map<ImmutablePair<Integer, Integer>, List<Integer>> commoditiesPaths;
    private final long startTime;
    private final int numMiniBatches;
    private final int microBatchSize;
    private final Map<Integer, Set<ImmutablePair<Integer, Integer>>> stageCommoditiesMap;
    private final long flowSize;
    private final long computeTime;
    private final JobLogger logger;
    private RoutingStrategy routingStrategy;
    private int currentEpoch;
    private DataParallelism dataParallelism;
    private boolean computeMode; // whether the job is in forward and backpropagation mode or in all-reduce mode


    public Job(int jobId, String modelName, String parallelizationType, long flowSize, long computeTime, long startTime, int numMiniBatches, int microBatchSize) {
        this.jobId = jobId;
        this.modelName = modelName;
        this.parallelizationType = parallelizationType;
        this.flowSize = flowSize;
        this.computeTime = computeTime;
        this.startTime = startTime;
        this.numMiniBatches = numMiniBatches;
        this.microBatchSize = microBatchSize;
        this.currentEpoch = 0;
        this.computeMode = true;
        this.epochs = new ArrayList<>();
        this.commoditiesFlowsMap = new HashMap<>();
        this.commoditiesPaths = new HashMap<>();
        this.stageCommoditiesMap = new HashMap<>();
        this.logger = new JobLogger(this);
        this.initRoutingStrategy();
    }

    private void initRoutingStrategy() {
        switch (Simulator.getConfiguration().getPropertyOrFail("routing_scheme")) {
            case Constants.RoutingStrategies.ECMP:
                routingStrategy = new EcmpRouting();
                break;
            case Constants.RoutingStrategies.EDGE_COLORING:
                routingStrategy = new EdgeColoringRouting();
                break;
            case Constants.RoutingStrategies.MCVLC:
                routingStrategy = new GreedyRouting();
                break;
            case Constants.RoutingStrategies.SIMULATED_ANNEALING:
                routingStrategy = new SimulatedAnnealingRouting();
                break;
            case Constants.RoutingStrategies.ILP_SOLVER:
                routingStrategy = new IlpSolverRouting();
                break;
            default:
                throw new IllegalArgumentException("Unknown routing scheme: " + Simulator.getConfiguration().getPropertyOrFail("routing_scheme"));
        }
    }

    public void initWrapper() {
        switch (parallelizationType) {
            case Constants.DistributedTraining.DATA_PARALLEL:
                dataParallelism = new DataParallelism(stageCommoditiesMap);
                break;
            case Constants.DistributedTraining.PIPELINE_PARALLEL:
            case Constants.DistributedTraining.HYBRID_PARALLEL:
            default:
                throw new IllegalArgumentException("Unknown parallelization type: " + parallelizationType);
        }
    }

    public void initializeEpoch(long startTime, Map<Integer, Set<Flow>> flows) {
        JobEpoch jobEpoch = new JobEpoch(startTime, flowSize, flows);
        epochs.add(jobEpoch);
        currentEpoch = epochs.size() - 1;
        computeMode = false;
    }

    public void update(Flow flow) {
        if (isFinished()) {
            return;
        }

        int stageIndex;
        switch (parallelizationType) {
            case Constants.DistributedTraining.DATA_PARALLEL:
                stageIndex = dataParallelism.update(this, flow);
                break;
            case Constants.DistributedTraining.PIPELINE_PARALLEL:
            case Constants.DistributedTraining.HYBRID_PARALLEL:
            default:
                throw new IllegalArgumentException("Unknown parallelization type: " + parallelizationType);
        }
        epochs.get(currentEpoch).getStageFlows().get(stageIndex).add(flow);
    }

    public boolean isFinished() {
        return currentEpoch > 10;
    }

    public int getJobId() {
        return jobId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getParallelizationType() {
        return parallelizationType;
    }

    public List<JobEpoch> getEpochs() {
        return epochs;
    }

    public Map<ImmutablePair<Integer, Integer>, List<Flow>> getCommoditiesFlowsMap() {
        return commoditiesFlowsMap;
    }

    public Map<ImmutablePair<Integer, Integer>, List<Integer>> getCommoditiesPaths() {
        return commoditiesPaths;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getNumMiniBatches() {
        return numMiniBatches;
    }

    public int getMicroBatchSize() {
        return microBatchSize;
    }

    public Map<Integer, Set<ImmutablePair<Integer, Integer>>> getStageCommoditiesMap() {
        return stageCommoditiesMap;
    }

    public long getFlowSize() {
        return flowSize;
    }

    public long getComputeTime() {
        return computeTime;
    }

    public JobEpoch getCurrentEpoch() {
        return epochs.get(currentEpoch);
    }

    public DataParallelism getDataParallelism() {
        return dataParallelism;
    }

    public boolean isComputeMode() {
        return computeMode;
    }

    public void setComputeMode(boolean computeMode) {
        this.computeMode = computeMode;
    }

    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    public JobLogger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        String result = "Job{" +
                "jobId=" + jobId + ", " +
                "modelName='" + modelName + '\'' + ", " +
                " [epochs=" + epochs +
                ", computeTime=" + computeTime +
                ", numMiniBatches=" + numMiniBatches +
                ", ";
        switch (parallelizationType) {
            case Constants.DistributedTraining.DATA_PARALLEL:
                result += dataParallelism.toString();
                break;
            case Constants.DistributedTraining.HYBRID_PARALLEL:
            case Constants.DistributedTraining.PIPELINE_PARALLEL:
            default:
                throw new IllegalStateException("Unknown parallelization type: " + parallelizationType);
        }
        result += "}";
        return result;
    }
}
