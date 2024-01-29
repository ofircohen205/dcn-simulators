package ch.ethz.systems.netbench.xpt.dynamic.device;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class DynamicLinkGenerator extends LinkGenerator {
    NBProperties mConfiguration;
    long mDelayNs;
    long mBandwidthBitPerNs;

    public DynamicLinkGenerator(NBProperties conf) {
        this.mConfiguration = conf;
        mDelayNs = conf.getLongPropertyOrFail(Constants.Link.DELAY_NS);
        mBandwidthBitPerNs = conf.getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);
    }

    @Override
    public Link generate(NetworkDevice fromNetworkDevice, NetworkDevice toNetworkDevice) {
        // TODO Auto-generated method stub
        return new DynamicLink(mDelayNs, mBandwidthBitPerNs);
    }

}
