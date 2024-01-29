package ch.ethz.systems.netbench.xpt.newreno.newrenodctcp;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class NewRenoDctcpTransportLayer extends TransportLayer {

    /**
     * Create the DCTCP transport layer with the given network device identifier.
     * The network device identifier is used to create unique flow identifiers.
     *
     * @param identifier Parent network device identifier
     */
    public NewRenoDctcpTransportLayer(int identifier, NBProperties configuration) {
        super(identifier, configuration);
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new NewRenoDctcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte, configuration);
    }

}
