package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.deeplearningtraining.state.JobEpoch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JobLogger {

    // Properties
    private final int jobId;
    private final Job job;
    private final Writer writerJobInfoFile;
    private boolean isInfoSavingEnabled;

    public JobLogger(Job job) {
        this.jobId = job.getJobId();
        this.job = job;
        this.isInfoSavingEnabled = true;
        String jobInfoFile = SimulationLogger.getLogsFolder() + "/job_info.csv";
        try {
            this.writerJobInfoFile = new BufferedWriter(new FileWriter(jobInfoFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enable/disable logging the info of flows.
     *
     * @param enabled True iff logging enabled
     */
    public void setInfoSavingEnabled(boolean enabled) {
        this.isInfoSavingEnabled = enabled;
    }

    /**
     * Final flush of logged state.
     */
    public final void finalFlush(long runtime) {
        if (isInfoSavingEnabled) {
            JobEpoch lastEpoch = job.getCurrentEpoch();
            Map<Integer, Long> stageEndTime = lastEpoch.getStageEndTime();
            Map<Integer, Long> stageDuration = new HashMap<>();
            lastEpoch.getStageFlows().keySet().forEach(stageIndex -> {
                if (stageEndTime.get(stageIndex) == -1) {
                    stageEndTime.put(stageIndex, Simulator.getCurrentTime());
                    stageDuration.put(stageIndex, Simulator.getCurrentTime() - lastEpoch.getStartTime());
                } else {
                    stageDuration.put(stageIndex, stageEndTime.get(stageIndex) - lastEpoch.getStartTime());
                }
            });
            saveInfo(
                    jobId,
                    job.getEpochs().indexOf(lastEpoch),
                    lastEpoch.getStartTime(),
                    stageEndTime,
                    stageDuration,
                    lastEpoch.getFlowSize(),
                    lastEpoch.getStageFlows(),
                    runtime
            );
        }
    }

    /**
     * Save the final flow info.
     *
     * @param jobId         Job identifier
     * @param epoch         Epoch number
     * @param startTime     Start time
     * @param stageEndTime  End time of each stage
     * @param stageDuration Duration of each stage
     * @param flowSize      Size of the flow in bytes
     * @param stageFlows    Stage flows map (stage index -> flows)
     * @param runtime       Runtime of the simulation
     */
    public void saveInfo(int jobId, int epoch, long startTime, Map<Integer, Long> stageEndTime, Map<Integer, Long> stageDuration, double flowSize, Map<Integer, Set<Flow>> stageFlows, long runtime) {
        stageFlows.forEach((stageIndex, flows) -> {
            boolean isFinished = stageEndTime.containsKey(stageIndex) && stageEndTime.get(stageIndex) != runtime;
            try {
                writerJobInfoFile.write(
                        jobId + "," +
                                epoch + "," +
                                stageIndex + "," +
                                startTime + "," +
                                stageEndTime.get(stageIndex) + "," +
                                stageDuration.get(stageIndex) + "," +
                                (isFinished ? "Y" : "N") + "," +
                                flows.size() + "," +
                                flowSize + "," +
                                flowIdsToString(flows) +
                                "\r\n"
                );
                writerJobInfoFile.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Convert a list of identifiers into a ;-separated string.
     *
     * @param flows Flow identifiers (e.g. [3, 4, 5])
     * @return ;-separated list string (e.g. "3;4;5")
     */
    private String flowIdsToString(Set<Flow> flows) {
        StringBuilder s = new StringBuilder();
        for (Flow flow : flows) {
            s.append(flow.getFlowId());
            s.append(";");
        }
        return s.substring(0, s.length() > 1 ? s.length() - 1 : s.length());
    }
}
