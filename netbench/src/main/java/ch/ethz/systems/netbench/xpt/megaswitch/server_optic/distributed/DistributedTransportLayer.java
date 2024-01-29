package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpTransportLayer;

public class DistributedTransportLayer extends SimpleDctcpTransportLayer {

    /**
     * Create the DCTCP transport layer with the given network device identifier.
     * The network device identifier is used to create unique flow identifiers.
     *
     * @param identifier    Parent network device identifier
     * @param configuration
     */
    public DistributedTransportLayer(int identifier, NBProperties configuration) {
        super(identifier, configuration);
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new SimpleDistributedSocket(this, flowId, this.identifier, destinationId, flowSizeByte, configuration, jobId);
    }


//    public void onCircuitEntrance(long flowId) {
//        SimpleDistributedSocket socket = (SimpleDistributedSocket) this.flowIdToSocket.get(flowId);
//        socket.markOnCircuit();
//    }

    public SimpleDistributedSocket getSocket(long flowId) {
        return (SimpleDistributedSocket) flowIdToSocket.get(flowId);
    }
}
