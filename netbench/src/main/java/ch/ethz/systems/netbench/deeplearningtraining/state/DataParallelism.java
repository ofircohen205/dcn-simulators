package ch.ethz.systems.netbench.deeplearningtraining.state;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.run.traffic.JobArrivalEvent;
import ch.ethz.systems.netbench.deeplearningtraining.routing.CentralizedController;
import ch.ethz.systems.netbench.deeplearningtraining.utils.ParallelismUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataParallelism {

    private final Map<Integer, Set<ImmutablePair<Integer, Integer>>> stageCommodities;
    private final Map<ImmutablePair<Integer, Integer>, Integer> commoditiesToStage;
    private final Map<Integer, Integer> stageTotalFlows;
    private final Map<Integer, Integer> stageFinishedFlows;
    private final Map<Integer, Integer> stageRingSize;
    private final Map<Integer, Boolean> stageAllReduceFinished;

    public DataParallelism(Map<Integer, Set<ImmutablePair<Integer, Integer>>> stageCommodities) {
        this.stageCommodities = new HashMap<>(stageCommodities);
        this.commoditiesToStage = new HashMap<>();
        this.stageTotalFlows = new HashMap<>();
        this.stageFinishedFlows = new HashMap<>();
        this.stageRingSize = new HashMap<>();
        this.stageAllReduceFinished = new HashMap<>();
        for (Map.Entry<Integer, Set<ImmutablePair<Integer, Integer>>> entry : stageCommodities.entrySet()) {
            int stage = entry.getKey();
            Set<ImmutablePair<Integer, Integer>> commodities = entry.getValue();
            int nodeTotalAllReduceFlows = 2 * (commodities.size() - 1);
            this.stageTotalFlows.put(stage, commodities.size() * nodeTotalAllReduceFlows);
            this.stageFinishedFlows.put(stage, 0);
            this.stageRingSize.put(stage, commodities.size());
            this.stageAllReduceFinished.put(stage, false);
            for (ImmutablePair<Integer, Integer> commodity : commodities) {
                commoditiesToStage.put(commodity, stage);
            }
        }
    }

    public int update(Job job, Flow flow) {
        int stageIndex = commoditiesToStage.get(ImmutablePair.of(flow.getSrcId(), flow.getDstId()));
        stageFinishedFlows.put(stageIndex, stageFinishedFlows.get(stageIndex) + 1);
        int numFinishedFlows = stageFinishedFlows.get(stageIndex);
        int totalFlows = stageTotalFlows.get(stageIndex);
        if (numFinishedFlows % stageRingSize.get(stageIndex) == 0) {
            boolean isStageAllReduceFinished = numFinishedFlows >= totalFlows;
            stageAllReduceFinished.put(stageIndex, isStageAllReduceFinished);
            boolean isAllReduceFinished = stageAllReduceFinished.values().stream().allMatch(Boolean::booleanValue);
            if (!isStageAllReduceFinished) {
                stageCommodities.get(stageIndex).forEach(stageCommodity -> ParallelismUtils.triggerNewFlow(job, stageIndex, stageCommodity, 1));
            } else {
                job.getCurrentEpoch().setEndTime(stageIndex, Simulator.getCurrentTime());
            }

            if (isAllReduceFinished) {
                closeEpoch(job);
                triggerNewEpoch(job);
            }
        }
        return stageIndex;
    }

    private void closeEpoch(Job job) {
        stageFinishedFlows.replaceAll((stage, finishedFlows) -> 0);
        stageAllReduceFinished.replaceAll((stage, allReduceFinished) -> false);
        if (job.getRoutingStrategy() instanceof CentralizedController) {
            int numActiveJobs = Simulator.getActiveJobs().size();
            if (numActiveJobs > 1) {
                CentralizedController centralizedRoutingStrategy = (CentralizedController) job.getRoutingStrategy();
                centralizedRoutingStrategy.determinePathAssignments();
                centralizedRoutingStrategy.getLogger().saveInfo(
                        centralizedRoutingStrategy.getAverageDuration(),
                        centralizedRoutingStrategy.getNumAssignedCommodities()
                );
            }
        }
        JobEpoch epoch = job.getCurrentEpoch();
        Map<Integer, Long> stageDuration = new HashMap<>();
        epoch.getStageEndTime().forEach((stageIndex, endTime) -> {
            stageDuration.put(stageIndex, endTime - epoch.getStartTime());
        });
        job.getLogger().saveInfo(
                job.getJobId(),
                job.getEpochs().indexOf(epoch),
                epoch.getStartTime(),
                epoch.getStageEndTime(),
                stageDuration,
                job.getFlowSize(),
                epoch.getStageFlows(),
                -1
        );
    }

    private void triggerNewEpoch(Job job) {
        Map<Integer, Set<Flow>> stageFlows = new HashMap<>();
        job.getStageCommoditiesMap().forEach((stage, commodities) -> {
            stageCommodities.putIfAbsent(stage, new HashSet<>());
            commodities.forEach(commodity -> {
                int srcId = commodity.getLeft();
                int dstId = commodity.getRight();
                Flow flow = new Flow(srcId, dstId, job.getFlowSize(), job.getJobId());
                stageFlows.get(stage).add(flow);
            });
        });
        JobArrivalEvent jobArrivalEvent = new JobArrivalEvent(job.getComputeTime(), job.getJobId(), stageFlows);
        Simulator.registerEvent(jobArrivalEvent);
        job.setComputeMode(true);
    }

    @Override
    public String toString() {
        return "DataParallelism[" +
                "stageCommodities=" + stageCommodities +
                ", commoditiesToStage=" + commoditiesToStage +
                ", stageTotalFlows=" + stageTotalFlows +
                ", stageFinishedFlows=" + stageFinishedFlows +
                ", stageRingSize=" + stageRingSize +
                ", stageAllReduceFinished=" + stageAllReduceFinished +
                ']';
    }
}
