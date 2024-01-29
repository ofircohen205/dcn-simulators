package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.utility.Constants;
import edu.asu.emit.algorithm.graph.Vertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlowLogger {

    // Every how many packets does it write the interval
    // of throughput
    private static final long STATISTIC_SAMPLE_INTERVAL_BYTES = 50000;

    private final int sourceId;
    private final int targetId;
    private int coreId;

    // Leaf-spine additional nodes
    private final int sourceTorId;
    private final int targetTorId;
    // Fat-tree additional nodes
    private int sourceEdgeId = -1;
    private int targetEdgeId = -1;
    private int sourceAggregationId = -1;
    private int targetAggregationId = -1;

    private List<Integer> path;

    // Static flow information
    private final long flowId;
    private final long flowSizeByte;
    private final int jobId;
    // Logging
    private final boolean flowThroughputEnabled;
    // Other
    private final long estimatedFlowSizeByte;
    protected long flowStartTime;
    protected long measureStartTime;
    // Statistic tracking variables
    private long totalBytesReceived;
    private long receivedBytes;
    private long flowEndTime;

    public FlowLogger(long flowId, int sourceId, int targetId, long flowSizeByte, long estimatedFlowSizeByte,
                      int jobId) {
        this.flowId = flowId;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.flowSizeByte = flowSizeByte;
        this.estimatedFlowSizeByte = estimatedFlowSizeByte;

        this.flowStartTime = Simulator.getCurrentTime();
        this.measureStartTime = Simulator.getCurrentTime();
        this.totalBytesReceived = 0;
        this.receivedBytes = 0;
        this.flowEndTime = -1;
        this.coreId = -1;
        this.jobId = jobId;

        // Register logger only for sender
        if (this.flowSizeByte != -1) { // Exclude receiving sockets
            SimulationLogger.registerFlowLogger(this);
            Simulator.registerFlow(this.flowId);
        }

        // True iff the flow throughput is enabled (or defaulted)
        this.flowThroughputEnabled = Simulator.getConfiguration().getBooleanPropertyWithDefault(
                Constants.Logger.ENABLE_LOG_FLOW_THROUGHPUT,
                true);
        GraphDetails details = Simulator.getConfiguration().getGraphDetails();
        this.sourceTorId = details.getTorIdOfServer(sourceId);
        this.targetTorId = details.getTorIdOfServer(targetId);

        this.path = new ArrayList<>();
    }

    /**
     * Log that some amount of the flow, <code>sizeByte</code>, has
     * been successfully transmitted and confirmed.
     *
     * @param sizeByte Size of flow successfully transmitted
     */
    public void logFlowAcknowledged(long sizeByte) {
        receivedBytes += sizeByte;
        totalBytesReceived += sizeByte;
        if (isCompleted()) {
            if (flowThroughputEnabled) {
                SimulationLogger.logFlowThroughput(flowId, sourceId, targetId, receivedBytes, measureStartTime,
                        Simulator.getCurrentTime());
            }
            receivedBytes = 0;
            measureStartTime = Simulator.getCurrentTime();
            flowEndTime = Simulator.getCurrentTime();
            SimulationLogger.getActiveFlowLoggerByFlowId().remove(this.getFlowId());
        }
    }

    /**
     * Retrieve flow identifier.
     *
     * @return Flow identifier
     */
    public long getFlowId() {
        return flowId;
    }

    /**
     * Retrieve source node identifier.
     *
     * @return Source node identifier
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * Retrieve target node identifier.
     *
     * @return Target node identifier
     */
    public int getTargetId() {
        return targetId;
    }

    /**
     * Retrieve total amount of bytes received (confirmed).
     *
     * @return Total amount of bytes received
     */
    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    /**
     * Register original starting time of the flow.
     *
     * @return Flow starting time (ns since simulation epoch)
     */
    public long getFlowStartTime() {
        return flowStartTime;
    }

    /**
     * Register original end time of the flow.
     *
     * @return Flow end time (ns since simulation epoch)
     */
    public long getFlowEndTime() {
        return flowEndTime;
    }

    /**
     * Register original flow completion time.
     *
     * @return Flow completion time (ns, flowEndTime - flowStartTime)
     */
    public long getFlowCompletionTime() {
        return flowEndTime - flowStartTime;
    }

    /**
     * Retrieve total size of the flow.
     *
     * @return Flow size
     */
    public long getFlowSizeByte() {
        return flowSizeByte;
    }

    /**
     * Check whether the flow is completed (all bytes acknowledged/confirmed).
     *
     * @return True iff flow has been completed confirmed
     */
    public boolean isCompleted() {
        return totalBytesReceived == flowSizeByte;
    }

    /**
     * Retrieve amount of bytes received (confirmed) within delta-t.
     *
     * @return amount of bytes received within delta-t
     */
    public long getReceivedBytes() {
        return receivedBytes;
    }

    public long getEstimatedFlowSizeByte() {
        return estimatedFlowSizeByte;
    }

    public int getSourceTorId() {
        return sourceTorId;
    }

    public int getTargetTorId() {
        return targetTorId;
    }

    public int getCoreId() {
        return coreId;
    }

    public void setCoreId(int coreId) {
        this.coreId = coreId;
    }

    public void determinePathNodes(List<Vertex> path) {
        this.path = path.stream().map(Vertex::getId).collect(Collectors.toList());
        this.coreId = path.get(path.size() / 2).getId();
        if (path.size() == 7) {
            // Source -> Edge -> Aggregation -> Core -> Aggregation -> Edge -> Target
            this.sourceEdgeId = path.get(1).getId();
            this.sourceAggregationId = path.get(2).getId();
            this.targetAggregationId = path.get(4).getId();
            this.targetEdgeId = path.get(5).getId();
        }
    }

    public List<Integer> getPath() {
        return path;
    }

    public boolean isLeafSpineTopology() {
        return Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file").contains("2_level");
    }

    public boolean isFatTreeTopology() {
        return Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file").contains("3_level");
    }

    public int getSourceEdgeId() {
        return sourceEdgeId;
    }

    public void setSourceEdgeId(int sourceEdgeId) {
        this.sourceEdgeId = sourceEdgeId;
    }

    public int getTargetEdgeId() {
        return targetEdgeId;
    }

    public void setTargetEdgeId(int targetEdgeId) {
        this.targetEdgeId = targetEdgeId;
    }

    public int getSourceAggregationId() {
        return sourceAggregationId;
    }

    public void setSourceAggregationId(int sourceAggregationId) {
        this.sourceAggregationId = sourceAggregationId;
    }

    public int getTargetAggregationId() {
        return targetAggregationId;
    }

    public void setTargetAggregationId(int targetAggregationId) {
        this.targetAggregationId = targetAggregationId;
    }

    public int getJobId() {
        return jobId;
    }

    private void initializePathLeafSpine() {
        this.path = Arrays.asList(sourceId, sourceTorId, coreId, targetTorId, targetId);
    }

    private void initializePathFatTree() {
        this.path = Arrays.asList(sourceId, sourceEdgeId, sourceAggregationId, coreId, targetAggregationId, targetEdgeId, targetId);
    }

    @Override
    public String toString() {
        return "FlowLogger{" +
                "flowId=" + flowId +
                ", jobId=" + jobId +
                ", (src,dst)=" + "(" + sourceId + "," + targetId + ")" +
                ", path=" + path +
                ", totalBytesReceived=" + totalBytesReceived +
                "}";
    }

}
