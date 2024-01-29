package ch.ethz.systems.netbench.xpt.megaswitch.server_optic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServer;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServerToR;

public class OpticServerGenerator extends NetworkDeviceGenerator {
    IntermediaryGenerator intermediaryGenerator;

    public OpticServerGenerator(NBProperties configuration) {
        super(configuration);
        intermediaryGenerator = new DemoIntermediaryGenerator(configuration);

    }

    public OpticServerGenerator(IntermediaryGenerator intermediaryGenerator, NBProperties configuration) {
        super(configuration);
        this.intermediaryGenerator = intermediaryGenerator;
    }

    @Override
    public NetworkDevice generate(int identifier) {
        if (configuration.getBooleanPropertyWithDefault(
                Constants.RemoteRoutingPopulator.DISTRIBUTED_PROTOCOL_ENABLED,
                false)) {
            return new DistributedOpticServerToR(identifier, null, intermediaryGenerator.generate(identifier), configuration);
        }
        return new OpticServerToR(identifier, null, intermediaryGenerator.generate(identifier), configuration);
    }

    @Override
    public NetworkDevice generate(int identifier, TransportLayer server) {
        if (configuration.getBooleanPropertyWithDefault(
                Constants.RemoteRoutingPopulator.DISTRIBUTED_PROTOCOL_ENABLED,
                false)) {
            return new DistributedOpticServer(identifier, server, intermediaryGenerator.generate(identifier), configuration);
        }
        return new OpticServer(identifier, server, intermediaryGenerator.generate(identifier), configuration);
    }
}
