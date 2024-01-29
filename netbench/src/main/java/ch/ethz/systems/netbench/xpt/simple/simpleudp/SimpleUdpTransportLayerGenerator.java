package ch.ethz.systems.netbench.xpt.simple.simpleudp;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.TransportLayerGenerator;

public class SimpleUdpTransportLayerGenerator extends TransportLayerGenerator {
    public SimpleUdpTransportLayerGenerator(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public TransportLayer generate(int identifier) {
        return new SimpleUdpTransportLayer(identifier,configuration);
    }
}
