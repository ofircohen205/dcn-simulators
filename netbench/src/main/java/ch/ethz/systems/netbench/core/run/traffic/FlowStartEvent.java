package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.deeplearningtraining.routing.RoutingStrategy;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class FlowStartEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -953241448509175658L;
    protected final int targetId;
    protected final long flowSizeByte;
    protected final int jobId;
    protected int networkDeviceId;
    protected TransportLayer transportLayer;
    protected TransportLayer dstTransportLayer;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs  Time it will take before happening from now in
     *                       nanoseconds
     * @param transportLayer Source transport layer that wants to send the flow to
     *                       the target
     * @param targetId       Target network device identifier
     * @param flowSizeByte   Size of the flow to send in bytes
     * @param jobId          Job identifier
     */
    public FlowStartEvent(
            long timeFromNowNs,
            TransportLayer transportLayer,
            int targetId,
            long flowSizeByte,
            int jobId) {
        super(timeFromNowNs);
        this.targetId = targetId;
        this.flowSizeByte = flowSizeByte;
        this.transportLayer = transportLayer;
        this.jobId = jobId;
        setNetworkDeviceId(transportLayer);
        // take the above out of comments to re-enable state saving support.
    }

    protected void setNetworkDeviceId(TransportLayer tl) {
        this.networkDeviceId = tl.getNetworkDevice().getIdentifier();
    }

    @Override
    public void trigger() {
        // TransportLayer tl =
        // BaseInitializer.getInstance().getNetworkDeviceById(networkDeviceId).getTransportLayer();
        Job job = Simulator.getJob(jobId);
        List<Flow> flowList = job.getCommoditiesFlowsMap().get(ImmutablePair.of(transportLayer.getIdentifier(), targetId));
        Flow flow = flowList.get(flowList.size() - 1);
        RoutingStrategy routingStrategy = job.getRoutingStrategy();
        routingStrategy.addFlow(flow);
        routingStrategy.assignStartFlow(flow);
        long flowId = transportLayer.startFlow(targetId, flowSizeByte, jobId);
        if (dstTransportLayer != null) {
            dstTransportLayer.registerAsDest(flowId, flowSizeByte);
        }
    }

    public void registerDstTransport(TransportLayer dstTransportLayer) {
        this.dstTransportLayer = dstTransportLayer;
    }

    public int getSourceId() {
        return networkDeviceId;
    }

    public int getTargetId() {
        return targetId;
    }

    public long getFlowSizeByte() {
        return flowSizeByte;
    }

    public TransportLayer getTransportLayer() {
        return transportLayer;
    }

}
