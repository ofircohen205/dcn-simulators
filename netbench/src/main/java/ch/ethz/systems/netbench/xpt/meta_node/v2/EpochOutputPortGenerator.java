package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPortGenerator;

public class EpochOutputPortGenerator extends EcnTailDropOutputPortGenerator {


    public EpochOutputPortGenerator(long maxQueueSizeBytes, long ecnThresholdKBytes, NBProperties configuration) {
        super(maxQueueSizeBytes, ecnThresholdKBytes, configuration);
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new EpochOutputPort(ownNetworkDevice,towardsNetworkDevice,link,maxQueueSizeBytes,ecnThresholdKBytes);
    }
}
