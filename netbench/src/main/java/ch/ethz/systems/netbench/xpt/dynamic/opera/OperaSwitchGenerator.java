package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.xpt.simple.simpleserver.SimpleServer;

public class OperaSwitchGenerator extends NetworkDeviceGenerator {
    IntermediaryGenerator intermediaryGenerator;
    public OperaSwitchGenerator(IntermediaryGenerator intermediaryGenerator, NBProperties configuration) {
        super(configuration);
        this.intermediaryGenerator = intermediaryGenerator;
    }

    @Override
    public NetworkDevice generate(int identifier) {
        return new OperaSwitch(identifier,null,null,configuration);
    }

    @Override
    public NetworkDevice generate(int identifier, TransportLayer server) {
        return new OperaServer(identifier,server,intermediaryGenerator.generate(identifier),configuration);
    }
}
