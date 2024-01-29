package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class PerfectSimpleLinkGenerator extends LinkGenerator {

    private final long delayNs;
    private final long bandwidthBitPerNs;
    private int serverLanes = 1;
    private NBProperties configuration;

    public PerfectSimpleLinkGenerator(long delayNs, long bandwidthBitPerNs) {
        super();
        this.delayNs = delayNs;
        this.bandwidthBitPerNs = bandwidthBitPerNs;
        SimulationLogger.logInfo("Link", "PERFECT_SIMPLE_LINK(delayNs=" + delayNs + ", bandwidthBitPerNs=" + bandwidthBitPerNs + ")");
    }

    public PerfectSimpleLinkGenerator(long delayNs, long bandwidthBitPerNs, int serverLanes) {
        this(delayNs, bandwidthBitPerNs);
        this.serverLanes = serverLanes;
        SimulationLogger.logInfo("Link", "PERFECT_SIMPLE_LINK(serverLanes=" + serverLanes + ")");

    }

    public PerfectSimpleLinkGenerator(NBProperties configuration) {
        this(configuration.getLongPropertyOrFail(Constants.Link.DELAY_NS),
                configuration.getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS),
                configuration.getGraphDetails().getServerLanesNum());
        this.configuration = configuration;
    }

    @Override
    public Link generate(NetworkDevice fromNetworkDevice, NetworkDevice toNetworkDevice) {
        long deviceBW = bandwidthBitPerNs;
        if (fromNetworkDevice.isServer() || toNetworkDevice.isServer()) {
            deviceBW /= serverLanes;
        }
        return new PerfectSimpleLink(delayNs, deviceBW);
    }

}
