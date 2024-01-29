package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedProtocolPort;
import ch.ethz.systems.netbench.xpt.meta_node.v1.MetaNodeOutputPort;

public class EcnTailDropOutputPortGenerator extends OutputPortGenerator {

    protected final long maxQueueSizeBytes;
    protected final long ecnThresholdKBytes;

    public EcnTailDropOutputPortGenerator(long maxQueueSizeBytes, long ecnThresholdKBytes, NBProperties configuration) {
        super(configuration);
        this.maxQueueSizeBytes = maxQueueSizeBytes;
        this.ecnThresholdKBytes = ecnThresholdKBytes;
        SimulationLogger.logInfo("Port", "ECN_TAIL_DROP(maxQueueSizeBytes=" + maxQueueSizeBytes + ", ecnThresholdKBytes=" + ecnThresholdKBytes + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {

        if (Simulator.getConfiguration().getBooleanPropertyWithDefault(
                Constants.RemoteRoutingPopulator.DISTRIBUTED_PROTOCOL_ENABLED,
                false)) {
            return new DistributedProtocolPort(ownNetworkDevice, towardsNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
        }
        if (configuration.getPropertyWithDefault(
                Constants.NetworkDeviceRouting.NETWORK_DEVICE_ROUTING,
                "").equals(Constants.NetworkDeviceRouting.META_NODE_ROUTER)) {
            return new MetaNodeOutputPort(ownNetworkDevice, towardsNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
        }
        return new EcnTailDropOutputPort(ownNetworkDevice, towardsNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
    }

}
