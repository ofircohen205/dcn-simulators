package ch.ethz.systems.netbench.xpt.simple.simpleudp;

import ch.ethz.systems.netbench.core.log.FlowLogger;

public class UDPFlowLogger extends FlowLogger {
    public UDPFlowLogger(long flowId, int sourceId, int targetId, long flowSizeByte, long flowStartTime) {
        super(flowId, sourceId, targetId, flowSizeByte, -1, -1);
        this.flowStartTime = flowStartTime;
//        this.flowStartTime = TransportLayer.flowMap.get(flowId).startTime;
//        this.measureStartTime = TransportLayer.flowMap.get(flowId).startTime;
    }


}
