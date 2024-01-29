package ch.ethz.systems.netbench.core.run;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.core.run.routing.remote.LightOutputPortGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingTransportLayerGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.bare.BareTransportLayerGenerator;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPortGenerator;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoTransportLayerGenerator;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitchGenerator;
import ch.ethz.systems.netbench.ext.ecmp.ForwarderSwitchGenerator;
import ch.ethz.systems.netbench.ext.flowlet.IdentityFlowletIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.flowlet.UniformFlowletIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.hybrid.EcmpThenValiantSwitchGenerator;
import ch.ethz.systems.netbench.ext.valiant.RangeValiantSwitchGenerator;
import ch.ethz.systems.netbench.xpt.asaf.routing.priority.PriorityFlowletIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicSwitchGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.opera.OperaSwitchGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorSwitchGenerator;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.ElectronicOpticHybridGenerator;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.OpticServerGenerator;
import ch.ethz.systems.netbench.xpt.meta_node.v1.MetaNodeSwitchGenerator;
import ch.ethz.systems.netbench.xpt.meta_node.v1.MetaNodeTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.meta_node.v2.EpochMNTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.meta_node.v2.EpochMetaNodeSwitchGenerator;
import ch.ethz.systems.netbench.xpt.meta_node.v2.EpochOutputPortGenerator;
import ch.ethz.systems.netbench.xpt.newreno.newrenodctcp.NewRenoDctcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.newreno.newrenotcp.NewRenoTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.RemoteSourceRoutingSwitchGenerator;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.semi.SemiRemoteRoutingSwitchGenerator;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.simple.simpletcp.SimpleTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.simple.simpleudp.SimpleUdpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.sourcerouting.EcmpThenSourceRoutingSwitchGenerator;
import ch.ethz.systems.netbench.xpt.sourcerouting.SourceRoutingSwitchGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.ports.BoundedPriorityOutputPortGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.ports.PriorityOutputPortGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.ports.UnlimitedOutputPortGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.buffertcp.BufferTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.distmeantcp.DistMeanTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.distrandtcp.DistRandTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.lstftcp.LstfTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.pfabric.PfabricTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.pfzero.PfzeroTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.sparktcp.SparkTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.sphalftcp.SpHalfTcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.voijslav.tcp.sptcp.SpTcpTransportLayerGenerator;

class InfrastructureSelector {

    private InfrastructureSelector() {
        // Only static class
    }

    /**
     * Select the network device generator, which, given its identifier,
     * generates an appropriate network device (possibly with transport layer).
     * <p>
     * Selected using following properties:
     * network_device=...
     * network_device_intermediary=...
     *
     * @param configuration
     * @return Network device generator.
     */
    static NetworkDeviceGenerator selectNetworkDeviceGenerator(NBProperties configuration) {
        /*
         * Select intermediary generator.
         */
        IntermediaryGenerator intermediaryGenerator;
        switch (configuration.getPropertyOrFail(Constants.NetworkDeviceIntermediary.NETWORK_DEVICE_INTERMEDIARY)) {
            case Constants.NetworkDeviceIntermediary.NETWORK_DEVICE_DEMO: {
                intermediaryGenerator = new DemoIntermediaryGenerator(configuration);
                break;
            }
            case Constants.NetworkDeviceIntermediary.IDENTITY: {
                intermediaryGenerator = new IdentityFlowletIntermediaryGenerator(configuration);
                break;
            }
            case Constants.NetworkDeviceIntermediary.NETWORK_DEVICE_UNIFORM: {
                intermediaryGenerator = new UniformFlowletIntermediaryGenerator(configuration);
                break;
            }
            case Constants.NetworkDeviceIntermediary.LOW_HIGH_PRIORITY: {
                intermediaryGenerator = new PriorityFlowletIntermediaryGenerator(configuration);
                break;
            }
            default:
                throw new PropertyValueInvalidException(configuration, Constants.NetworkDeviceIntermediary.NETWORK_DEVICE_INTERMEDIARY);
        }

        /*
         * Select network device generator.
         */
        switch (configuration.getPropertyOrFail(Constants.NetworkDeviceGenerator.NETWORK_DEVICE)) {
            case Constants.NetworkDeviceGenerator.FORWARDER_SWITCH:
                return new ForwarderSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.ECMP_SWITCH:
                return new EcmpSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.RANDOM_VALIANT_ECMP_SWITCH:
                return new RangeValiantSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.ECMP_THEN_RANDOM_VALIANT_ECMP_SWITCH:
                return new EcmpThenValiantSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.SOURCE_ROUTING_SWITCH:
                return new SourceRoutingSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.REMOTE_SOURCE_ROUTING_SWITCH:
                return new RemoteSourceRoutingSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.ECMP_THEN_SOURCE_ROUTING_SWITCH:
                return new EcmpThenSourceRoutingSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.HYBRID_OPTIC_ELECTRONIC:
                return new ElectronicOpticHybridGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.ROTOR_SWITCH:
                return new RotorSwitchGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.DYNAMIC_SWITCH:
                return new DynamicSwitchGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.SEMI_REMOTE_ROUTING_SWITCH:
                return new SemiRemoteRoutingSwitchGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.OPTIC_SERVER:
                return new OpticServerGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.OPERA_SWITCH:
                return new OperaSwitchGenerator(intermediaryGenerator, configuration);
            case Constants.NetworkDeviceGenerator.META_NODE_SWITCH:
                return new MetaNodeSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            case Constants.NetworkDeviceGenerator.EPOCH_META_NODE_SWITCH:
                return new EpochMetaNodeSwitchGenerator(intermediaryGenerator, configuration.getGraphDetails().getNumNodes(), configuration);
            default:
                throw new PropertyValueInvalidException(configuration, Constants.NetworkDeviceGenerator.NETWORK_DEVICE);
        }
    }

    /**
     * Select the link generator which creates a link instance given two
     * directed network devices.
     * <p>
     * Selected using following property:
     * link=...
     *
     * @param configuration
     * @return Link generator
     */
    static LinkGenerator selectLinkGenerator(NBProperties configuration) {
        if (Constants.Link.PERFECT_SIMPLE.equals(configuration.getPropertyOrFail(Constants.Link.LINK))) {
            return new PerfectSimpleLinkGenerator(configuration);
        }
        throw new PropertyValueInvalidException(configuration, Constants.Link.LINK);
    }

    /**
     * Select the output port generator which creates a port instance given two
     * directed network devices and the corresponding link.
     * <p>
     * Selected using following property:
     * output_port=...
     *
     * @param configuration
     * @return Output port generator
     */
    static OutputPortGenerator selectOutputPortGenerator(NBProperties configuration) {
        switch (configuration.getPropertyOrFail(Constants.OutputPortGenerators.OUTPUT_PORT)) {
            case Constants.OutputPortGenerators.ECN_TAIL_DROP:
                return new EcnTailDropOutputPortGenerator(
                        configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES),
                        configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.ECN_THRESHOLD_K_BYTES),
                        configuration
                );
            case Constants.OutputPortGenerators.OUTPUT_PORT_META_NODE_EPOCH:
                return new EpochOutputPortGenerator(
                        configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES),
                        configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.ECN_THRESHOLD_K_BYTES),
                        configuration
                );
            case Constants.OutputPortGenerators.PRIORITY:
                return new PriorityOutputPortGenerator(configuration);
            case Constants.OutputPortGenerators.BOUNDED_PRIORITY:
                return new BoundedPriorityOutputPortGenerator(
                        configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES) * 8,
                        configuration
                );
            case Constants.OutputPortGenerators.UNLIMITED:
                return new UnlimitedOutputPortGenerator(configuration);
            case Constants.OutputPortGenerators.OUTPUT_PORT_REMOTE:
            case Constants.OutputPortGenerators.LIGHT_PORT:
                return new LightOutputPortGenerator(configuration);
            default:
                throw new PropertyValueInvalidException(
                        configuration,
                        Constants.OutputPortGenerators.OUTPUT_PORT
                );
        }
    }

    /**
     * Select the transport layer generator.
     *
     * @param configuration
     * @return Transport layer generator
     */
    static TransportLayerGenerator selectTransportLayerGenerator(NBProperties configuration) {

        switch (configuration.getPropertyOrFail(Constants.TransportLayerGenerator.TRANSPORT_LAYER)) {
            case Constants.TransportLayerGenerator.TRANSPORT_LAYER_DEMO:
                return new DemoTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.BARE:
                return new BareTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.TCP:
                return new NewRenoTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.LSTF_TCP:
                return new LstfTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.SP_TCP:
                return new SpTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.SP_HALF_TCP:
                return new SpHalfTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.PFABRIC:
                return new PfabricTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.PFZERO:
                return new PfzeroTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.BUFFER_TCP:
                return new BufferTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.DIST_MEAN:
                return new DistMeanTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.DIST_RAND:
                return new DistRandTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.SPARK_TCP:
                return new SparkTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.DC_TCP:
                return new NewRenoDctcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.SIMPLE_TCP:
                return new SimpleTcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.SIMPLE_DC_TCP:
                return new SimpleDctcpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.TRANSPORT_LAYER_REMOTE:
                return new RemoteRoutingTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.TRANSPORT_LAYER_NULL:
                return new NullTrasportLayer(configuration);
            case Constants.TransportLayerGenerator.SIMPLE_UDP:
                return new SimpleUdpTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.META_NODE:
                return new MetaNodeTransportLayerGenerator(configuration);
            case Constants.TransportLayerGenerator.TRANSPORT_LAYER_META_NODE_EPOCH:
                return new EpochMNTransportLayerGenerator(configuration);
            default:
                throw new PropertyValueInvalidException(configuration, Constants.TransportLayerGenerator.TRANSPORT_LAYER);
        }
    }
}
