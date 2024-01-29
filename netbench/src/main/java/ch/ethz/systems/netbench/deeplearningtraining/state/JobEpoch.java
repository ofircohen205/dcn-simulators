package ch.ethz.systems.netbench.deeplearningtraining.state;

import ch.ethz.systems.netbench.core.run.traffic.Flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JobEpoch {

    private final long startTime;
    private final long flowSize;
    private final Map<Integer, Set<Flow>> stageFlows;
    private final Map<Integer, Long> stageEndTime;

    public JobEpoch(long startTime, long flowSize, Map<Integer, Set<Flow>> stageFlows) {
        this.startTime = startTime;
        this.flowSize = flowSize;
        this.stageFlows = new HashMap<>(stageFlows);
        this.stageEndTime = new HashMap<>();
        stageFlows.keySet().forEach(stage -> stageEndTime.put(stage, -1L));
    }

    public JobEpoch(long startTime, JobEpoch other, Map<Integer, Set<Flow>> stageFlows) {
        this.startTime = startTime;
        this.flowSize = other.getFlowSize();
        this.stageFlows = new HashMap<>(stageFlows);
        this.stageEndTime = new HashMap<>();
        stageFlows.keySet().forEach(stage -> stageEndTime.put(stage, -1L));
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFlowSize() {
        return flowSize;
    }

    public Map<Integer, Set<Flow>> getStageFlows() {
        return stageFlows;
    }

    public int getFlowStage(Flow flow) {
        for (Map.Entry<Integer, Set<Flow>> entry : stageFlows.entrySet()) {
            if (entry.getValue().contains(flow)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Flow " + flow.getFlowId() + " not found in any stage of epoch " + this);
    }

    public Map<Integer, Long> getStageEndTime() {
        return stageEndTime;
    }

    public void setEndTime(int stage, long endTime) {
        stageEndTime.put(stage, endTime);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Epoch[");
        result.append("startTime=")
                .append(startTime);
        for (Map.Entry<Integer, Long> entry : stageEndTime.entrySet()) {
            result.append(", stage=[")
                    .append(entry.getKey())
                    .append(", endTime=")
                    .append(entry.getValue())
                    .append(", connections=")
                    .append(stageFlows.get(entry.getKey()).stream().map(Flow::getFlowId).collect(Collectors.toList()))
                    .append("]");
        }
        result.append(", flowSize=")
                .append(flowSize)
                .append("]");
        return result.toString();
    }
}
