package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.TransportLayerGenerator;

public class EpochMNTransportLayerGenerator extends TransportLayerGenerator {
    public EpochMNTransportLayerGenerator(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public TransportLayer generate(int identifier) {
        return new MetaNodeTransport(identifier, configuration);
    }
}
