package ch.ethz.systems.netbench.xpt.dynamic.device;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class DynamicOutputPortGenerator extends OutputPortGenerator {
    protected long mMaxQueueSizeBytes;
    protected long mEcnThresholdKBytes;

    public DynamicOutputPortGenerator(NBProperties configuration) {
        super(configuration);
        mEcnThresholdKBytes = configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.ECN_THRESHOLD_K_BYTES);
        mMaxQueueSizeBytes = configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES);
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        // TODO Auto-generated method stub
        return new DynamicOutuptPort(ownNetworkDevice, towardsNetworkDevice, link, mMaxQueueSizeBytes, mEcnThresholdKBytes);
    }

}
