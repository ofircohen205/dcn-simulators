package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.run.traffic.FlowStartEvent;

public class MockedFlowStartEvent extends FlowStartEvent {

    public MockedFlowStartEvent(long timeFromNowNs, TransportLayer transportLayer, int targetId, long flowSizeByte) {
        super(timeFromNowNs, transportLayer, targetId, flowSizeByte, jobId);

    }

    @Override
    protected void setNetworkDeviceId(TransportLayer tl) {
        this.networkDeviceId = -1;
    }

    @Override
    public void trigger() {

        transportLayer.startFlow(targetId, flowSizeByte);
    }

}
