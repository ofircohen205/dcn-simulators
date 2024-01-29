package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.core.run.routing.remote.LightOutputPortGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingTransportLayerGenerator;
import ch.ethz.systems.netbench.core.run.traffic.FlowStartEvent;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitchGenerator;
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
public class JumboFullHybridTest {

    MockRemoteRouter router;

    @Before
    public void setup() throws IOException {
        // main network
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n2x2_v1.topology\n");
        runConfigWriter.write("hybrid_circuit_threshold_byte=1000\n");
        runConfigWriter.write("enable_jumbo_flows=true\n");


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
                return new MockJumboFullHybrid(i, null, ig.generate(i), conf);
            }

            @Override
            public NetworkDevice generate(int i, TransportLayer transportLayer) {
                return new MockSimpleServer(i, transportLayer, ig.generate(i), conf);
            }
        };
        LinkGenerator lg = new PerfectSimpleLinkGenerator(0, 10);
        TransportLayerGenerator tlg = new SimpleDctcpTransportLayerGenerator(conf);
        initializer.extend(portGen, ndg, lg, tlg);
        initializer.createInfrastructure(conf);


        //creating network 2, circuit switching:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/simple/simple_n2_v2.topology\n");
        runConfigWriter2.write("centered_routing_type=Xpander\n");
        runConfigWriter2.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter2.write("circuit_wave_length_num=1\n");
        runConfigWriter2.write("network_type=circuit_switch\n");
        runConfigWriter2.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter2.write("output_port_ecn_threshold_k_bytes=30000\n");
        runConfigWriter2.write("link_bandwidth_bit_per_ns=50\n");
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
        MockJumboFullHybrid.setRemoteRouter(router);
        //creating network 3, packet switching:
        File tempRunConfig3 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter3 = new BufferedWriter(new FileWriter(tempRunConfig3));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter3.write("scenario_topology_file=example/topologies/simple/simple_n2_v2.topology\n");
        runConfigWriter3.write("network_device_routing=ecmp\n");
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


        OutputPortGenerator portGen3 = new OutputPortGenerator(conf3) {
            @Override
            public OutputPort generate(NetworkDevice own, NetworkDevice target, Link link) {
                return new OutputPort(own, target, link, new LinkedList<>()) {
                    @Override
                    public void enqueue(Packet packet) {
                        guaranteedEnqueue(packet);
                    }
                };
            }
        };
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

        initializer.extend(2, portGen3, new EcmpSwitchGenerator(new DemoIntermediaryGenerator(conf3), 2, conf3),
                lg, tlg3);
        HashMap<Integer, NetworkDevice> hm3 = initializer.createInfrastructure(conf3);
        RoutingSelector.selectPopulator(hm3, conf3).populateRoutingTables();

        initializer.finalize();
        tempRunConfig.delete();
        tempRunConfig2.delete();
        tempRunConfig3.delete();
    }

    @Test
    public void testDoubleFlow() {
        MockSimpleServer source1 = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockSimpleServer dest1 = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(3));
        MockSimpleServer source2 = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(4);
        MockSimpleServer dest2 = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(5));
        FlowStartEvent fse = new FlowStartEvent(0, source1.getTransportLayer(), dest1.getIdentifier(), 16000, jobId);
        FlowStartEvent fse2 = new FlowStartEvent(0, source2.getTransportLayer(), dest2.getIdentifier(), 8000, jobId);

        MockJumboFullHybrid tor1 = (MockJumboFullHybrid) (BaseInitializer.getInstance().getNetworkDeviceById(0));
        Simulator.registerEvent(fse);
        Simulator.registerEvent(fse2);
        Simulator.runNs(1000000000);
        assert (tor1.routedThroughCircuit);
        assert (tor1.routedThroughPacketSwitch);
        assert (tor1.recoveredPath);
        assert (router.routed(0, 1));
        assert (router.recovered(0, 1));
        assert (tor1.maxPortsNum == 1);
        tor1.reset();
    }

    @After
    public void cleanup() {
        Simulator.reset();
    }
}
