package ch.ethz.systems.netbench.xpt.voijslav.tcp.pfabric;


import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class PfabricTransportLayer extends TransportLayer {

    private long seed;

    /**
     * Create the TCP transport layer with the given network device identifier.
     * The network device identifier is used to create unique flow identifiers.
     *
     * @param identifier Parent network device identifier
     */
    public PfabricTransportLayer(int identifier, long seed, NBProperties configuration) {
        super(identifier, configuration);
        this.seed = seed;
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new PfabricSocket(this, flowId, this.identifier, destinationId, flowSizeByte, seed, configuration);
    }

}
