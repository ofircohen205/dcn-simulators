package ch.ethz.systems.netbench.core.run;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.TransportLayerGenerator;

public class NullTrasportLayer extends TransportLayerGenerator {
    public NullTrasportLayer(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public TransportLayer generate(int identifier) {
        return null;
    }
}
