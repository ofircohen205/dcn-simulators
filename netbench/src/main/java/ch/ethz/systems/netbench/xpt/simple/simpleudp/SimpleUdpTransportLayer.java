package ch.ethz.systems.netbench.xpt.simple.simpleudp;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

import java.util.HashMap;

public class SimpleUdpTransportLayer extends TransportLayer {
    protected HashMap<Long, Long> flowIdToSize;
    protected HashMap<Long, Long> flowIdToStartTime;

    public SimpleUdpTransportLayer(int identifier, NBProperties configuration) {

        super(identifier, configuration);
        flowIdToSize = new HashMap<>();
        flowIdToStartTime = new HashMap<>();
    }


    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        long startTime = -1;
        if (flowSizeByte == -1) {
            flowSizeByte = flowIdToSize.get(flowId);
            startTime = flowIdToStartTime.get(flowId);
        }

        return new SimpleUDPSocket(this, flowId, identifier, destinationId, flowSizeByte, startTime, configuration);
    }

    @Override
    public void registerAsDest(long flowId, long flowSizeByte) {
        flowIdToStartTime.put(flowId, Simulator.getCurrentTime());
        flowIdToSize.put(flowId, flowSizeByte);
    }

}
