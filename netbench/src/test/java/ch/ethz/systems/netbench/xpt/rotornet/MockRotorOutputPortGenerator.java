package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorOutputPort;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorOutputPortGenerator;

public class MockRotorOutputPortGenerator extends RotorOutputPortGenerator {
    public MockRotorOutputPortGenerator(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new MockRotorOutputPort(ownNetworkDevice, towardsNetworkDevice, link, this.mMaxQueueSizeBytes, this.mEcnThresholdKBytes);
    }
}
