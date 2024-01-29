package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicSwitchGenerator;

public class RotorSwitchGenerator extends DynamicSwitchGenerator {
    public RotorSwitchGenerator(IntermediaryGenerator intermediaryGenerator, NBProperties configuration) {
        super(intermediaryGenerator,configuration);
    }

    @Override
    public NetworkDevice generate(int identifier) {
        return new RotorSwitch(identifier,null,intermediaryGenerator.generate(identifier),configuration);
    }

    @Override
    public NetworkDevice generate(int identifier, TransportLayer server) {
        return null;
    }
}
