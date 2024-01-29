package ch.ethz.systems.netbench.xpt.mega_switch.ecmp_then_vlb;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.core.run.routing.remote.LightOutputPortGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingTransportLayerGenerator;
import ch.ethz.systems.netbench.core.run.traffic.FlowStartEvent;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPortGenerator;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.flowlet.IdentityFlowletIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.hybrid.EcmpThenValiantSwitchGenerator;
import ch.ethz.systems.netbench.xpt.mega_switch.MockFullHybrid;
import ch.ethz.systems.netbench.xpt.mega_switch.MockSimpleServer;
import ch.ethz.systems.netbench.xpt.mega_switch.SimpleSocket;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.MockRemoteRouter;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.RemoteSourceRoutingSwitchGenerator;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpTransportLayerGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

@RunWith(MockitoJUnitRunner.class)
public class EcmpThenVLBDebugger {
    MockRemoteRouter router;

    @Before
    public void setup() throws IOException {
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n333_e12.topology\n");
        runConfigWriter.write("hybrid_circuit_threshold_byte=99999999\n");
        runConfigWriter.write("run_folder_base_dir=/cs/usr/inonkp/ecmp_then_vlb\n");
        runConfigWriter.write("run_folder_name=results\n");

        runConfigWriter.close();
        NBProperties conf = new NBProperties(
                tempRunConfig.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );
        Simulator.setup(0, conf);

        BaseInitializer initializer = BaseInitializer.getInstance();
        OutputPortGenerator portGen = new OutputPortGenerator(conf) {
            @Override
            public OutputPort generate(NetworkDevice own, NetworkDevice target, Link link) {
                return new OutputPort(own, target, link, new LinkedList<Packet>()) {
                    @Override
                    public void enqueue(Packet packet) {
                        guaranteedEnqueue(packet);
                    }
                };
            }
        };
        IntermediaryGenerator ig = new DemoIntermediaryGenerator(conf);
        NetworkDeviceGenerator ndg = new NetworkDeviceGenerator(conf) {
            @Override
            public NetworkDevice generate(int i) {
                return new MockFullHybrid(i, null, ig.generate(i), configuration);
            }

            @Override
            public NetworkDevice generate(int i, TransportLayer transportLayer) {
                return new MockSimpleServer(i, transportLayer, ig.generate(i), configuration);
            }
        };
        LinkGenerator lg = new PerfectSimpleLinkGenerator(0, 100);
        TransportLayerGenerator tlg = new SimpleDctcpTransportLayerGenerator(conf);
        initializer.extend(portGen, ndg, lg, tlg);
        initializer.createInfrastructure(conf);

        //creating network 2, circuit switching:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/xpander/xpander_n333_d36.topology\n");
        runConfigWriter2.write("centered_routing_type=Xpander\n");
        runConfigWriter2.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter2.write("circuit_wave_length_num=2\n");
        runConfigWriter2.write("network_type=circuit_switch\n");
        runConfigWriter2.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter2.write("output_port_ecn_threshold_k_bytes=30000\n");
        runConfigWriter2.write("link_bandwidth_bit_per_ns=100\n");
        runConfigWriter2.write("link_delay_ns=10\n");
        runConfigWriter2.close();
        NBProperties conf2 = new NBProperties(
                tempRunConfig2.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );


        initializer.extend(1, new LightOutputPortGenerator(conf2), new RemoteSourceRoutingSwitchGenerator(new DemoIntermediaryGenerator(conf2), 2, conf2),
                lg, new RemoteRoutingTransportLayerGenerator(conf2));
        HashMap<Integer, NetworkDevice> hm = initializer.createInfrastructure(conf2);
        //RoutingSelector.selectPopulator(hm, conf2);
        router = new MockRemoteRouter(hm, conf2);
        MockFullHybrid.setRemoteRouter(router);
        //creating network 3, packet switching:
        File tempRunConfig3 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter3 = new BufferedWriter(new FileWriter(tempRunConfig3));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter3.write("scenario_topology_file=example/topologies/xpander/xpander_n333_d36.topology\n");
        runConfigWriter3.write("network_device_routing=ecmp\n");
        runConfigWriter3.write("routing_random_valiant_node_range_lower_incl=0\n");
        runConfigWriter3.write("routing_random_valiant_node_range_upper_incl=332\n");


        runConfigWriter3.write("routing_ecmp_then_valiant_switch_threshold_bytes=200\n");
        runConfigWriter3.write("network_type=packet_switch");
        runConfigWriter3.close();
        NBProperties conf3 = new NBProperties(
                tempRunConfig3.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );


        OutputPortGenerator portGen3 = new EcnTailDropOutputPortGenerator(10000000, 1500000, conf3);
        IntermediaryGenerator ig3 = new DemoIntermediaryGenerator(conf3);
        TransportLayerGenerator tlg3 = new TransportLayerGenerator(conf3) {
            @Override
            public TransportLayer generate(int i) {
                return new TransportLayer(i, conf3) {

                    @Override
                    protected Socket createSocket(long l, int i, long l1) {
                        return new SimpleSocket(this, l, this.identifier, i, l1);
                    }
                };
            }
        };

        initializer.extend(2, portGen3, new EcmpThenValiantSwitchGenerator(new IdentityFlowletIntermediaryGenerator(conf3), 333, conf3),
                lg, tlg3);
        HashMap<Integer, NetworkDevice> hm3 = initializer.createInfrastructure(conf3);
        RoutingSelector.selectPopulator(hm3, conf3).populateRoutingTables();

        initializer.finalize();
        tempRunConfig.delete();
        tempRunConfig2.delete();
        tempRunConfig3.delete();
    }

    @Test
    public void testFlow() {
        FlowStartEvent fse = new FlowStartEvent(0, BaseInitializer.getInstance().getNetworkDeviceById(333).getTransportLayer(), 336, 1000000, jobId);
        FlowStartEvent fse2 = new FlowStartEvent(0, BaseInitializer.getInstance().getNetworkDeviceById(334).getTransportLayer(), 337, 1000000, jobId);
        FlowStartEvent fse3 = new FlowStartEvent(0, BaseInitializer.getInstance().getNetworkDeviceById(335).getTransportLayer(), 338, 1000000, jobId);

        Simulator.registerEvent(fse);
        Simulator.registerEvent(fse2);
        Simulator.registerEvent(fse3);

        Simulator.runNs(20000000);


    }

    @After
    public void clear() {
        Simulator.reset(false);
    }
}
