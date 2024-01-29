package ch.ethz.systems.netbench.core.run;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitchRouting;
import ch.ethz.systems.netbench.ext.ecmp.ForwarderSwitchRouting;
import ch.ethz.systems.netbench.xpt.dynamic.opera.OperaController;
import ch.ethz.systems.netbench.xpt.meta_node.v1.MNController;
import ch.ethz.systems.netbench.xpt.meta_node.v2.MNEpochController;
import ch.ethz.systems.netbench.xpt.sourcerouting.EcmpThenKspNoShortestRouting;
import ch.ethz.systems.netbench.xpt.sourcerouting.EcmpThenKspRouting;
import ch.ethz.systems.netbench.xpt.sourcerouting.KShortestPathsSwitchRouting;

import java.util.Map;

public class RoutingSelector {

    /**
     * Select the populator which populates the routing state in all network devices.
     * <p>
     * Selected using following property:
     * network_device_routing=...
     *
     * @param idToNetworkDevice Identifier to instantiated network device
     */
    public static RoutingPopulator selectPopulator(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        switch (configuration.getPropertyOrFail(Constants.NetworkDeviceRouting.NETWORK_DEVICE_ROUTING)) {
            case Constants.NetworkDeviceRouting.SINGLE_FORWARD:
                return new ForwarderSwitchRouting(idToNetworkDevice, configuration);
            case Constants.NetworkDeviceRouting.ECMP:
                return new EcmpSwitchRouting(idToNetworkDevice, configuration);
            case Constants.NetworkDeviceRouting.K_SHORTEST_PATHS:
                return new KShortestPathsSwitchRouting(idToNetworkDevice, configuration);
            case Constants.NetworkDeviceRouting.ECMP_THEN_K_SHORTEST_PATHS:
                return new EcmpThenKspRouting(idToNetworkDevice, configuration);
            case Constants.NetworkDeviceRouting.ECMP_THEN_K_SHORTEST_PATHS_WITHOUT_SHORTEST:
                return new EcmpThenKspNoShortestRouting(idToNetworkDevice, configuration);
            case Constants.NetworkDeviceRouting.REMOTE_ROUTING_POPULATOR: {
                String remoteRoutingType = configuration.getPropertyOrFail(
                        Constants.RemoteRoutingPopulator.CENTERED_ROUTING_TYPE);
                long headerSize = configuration.getLongPropertyWithDefault(
                        Constants.RemoteRoutingPopulator.HEADER_SIZE, 0L);
                RemoteRoutingController.initRemoteRouting(remoteRoutingType, idToNetworkDevice, headerSize, configuration);
                return RemoteRoutingController.getInstance();
            }
            case Constants.NetworkDeviceRouting.META_NODE_ROUTER:
                return MNController.getInstance(configuration, idToNetworkDevice);
            case Constants.NetworkDeviceRouting.EPOCH_META_NODE_ROUTER:
                return MNEpochController.getInstance(configuration, idToNetworkDevice);
            case Constants.NetworkDeviceRouting.EMPTY_ROUTING_POPULATOR:
                return new EmptyRoutingPopulator(configuration);
            case Constants.NetworkDeviceRouting.OPERA:
                OperaController.init(configuration, idToNetworkDevice);
                return OperaController.getInstance();
            default:
                throw new PropertyValueInvalidException(configuration, Constants.NetworkDeviceRouting.NETWORK_DEVICE_ROUTING);
        }
    }
}
