package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicOutputPortGenerator;

public class RotorOutputPortGenerator extends DynamicOutputPortGenerator {
    public RotorOutputPortGenerator(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new RotorOutputPort(ownNetworkDevice,towardsNetworkDevice,link,mMaxQueueSizeBytes,mEcnThresholdKBytes);
    }
}
