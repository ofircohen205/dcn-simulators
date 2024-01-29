package ch.ethz.systems.netbench.xpt.simple.simpledctcp;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class SimpleDctcpTransportLayer extends TransportLayer {

    /**
     * Create the DCTCP transport layer with the given network device identifier.
     * The network device identifier is used to create unique flow identifiers.
     *
     * @param identifier Parent network device identifier
     */
    public SimpleDctcpTransportLayer(int identifier, NBProperties configuration) {
        super(identifier, configuration);
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new SimpleDctcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte, estimatedFlowSizeByte, configuration, jobId);
    }

}
