package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.Simulator;

public class Flow {

    // Properties
    private final long flowId;
    private final int srcId;
    private final int dstId;
    private final long flowSize;
    private final int jobId;
    private boolean reversePath;


    public Flow(int srcId, int dstId, long flowSize, int jobId) {
        // Properties
        this.flowId = Simulator.getNextFlowId();
        this.srcId = srcId;
        this.dstId = dstId;
        this.flowSize = flowSize;
        this.reversePath = false;

        // Job ID
        this.jobId = jobId;
    }

    public long getFlowId() {
        return flowId;
    }

    public int getSrcId() {
        return srcId;
    }

    public int getDstId() {
        return dstId;
    }

    public long getFlowSize() {
        return flowSize;
    }

    public int getJobId() {
        return jobId;
    }

    public boolean isReversePath() {
        return reversePath;
    }

    public void setReversePath(boolean reversePath) {
        this.reversePath = reversePath;
    }
}
