package ch.ethz.systems.netbench.xpt.mega_switch.dynamic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.core.run.traffic.FlowStartEvent;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicSwitchGenerator;
import ch.ethz.systems.netbench.xpt.mega_switch.MockOpticalHybrid;
import ch.ethz.systems.netbench.xpt.mega_switch.MockSimpleServer;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpTransportLayerGenerator;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import org.junit.After;
import org.junit.AfterClass;
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
public class DynamicSwitchTest {

    MockDynamicController router;

    @Before
    public void setup() throws IOException {
        // main network
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n3_e3.topology\n");
        runConfigWriter.write("hybrid_circuit_threshold_byte=0");

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
                return new MockOpticalHybrid(i, null, ig.generate(i), configuration);
            }

            @Override
            public NetworkDevice generate(int i, TransportLayer transportLayer) {
                return new MockSimpleServer(i, transportLayer, ig.generate(i), configuration);
            }
        };
        LinkGenerator lg = new PerfectSimpleLinkGenerator(0, 10);
        TransportLayerGenerator tlg = new SimpleDctcpTransportLayerGenerator(conf);
        initializer.extend(portGen, ndg, lg, tlg);
        initializer.createInfrastructure(conf);


        //creating network 2:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/simple/simple_n3_e0.topology\n");
        runConfigWriter2.write("centered_routing_type=dynamic\n");
        runConfigWriter2.write("max_dynamic_switch_degree=1\n");
        runConfigWriter2.write("link_delay_ns=0\n");
        runConfigWriter2.write("link_bandwidth_bit_per_ns=10\n");
        runConfigWriter2.write("output_port_ecn_threshold_k_bytes=50000\n");
        runConfigWriter2.write("output_port_max_queue_size_bytes=50000\n");
        runConfigWriter2.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter2.write("network_type=circuit_switch");
        runConfigWriter2.close();
        NBProperties conf2 = new NBProperties(
                tempRunConfig2.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );


        initializer.extend(1, null, new DynamicSwitchGenerator(new DemoIntermediaryGenerator(conf2), conf2),
                null, null);
        HashMap<Integer, NetworkDevice> hm = initializer.createInfrastructure(conf2);
        RoutingSelector.selectPopulator(hm, conf2);
        router = new MockDynamicController(hm, conf2);
        MockOpticalHybrid.setRouter(router);
        initializer.finalize();
        tempRunConfig.delete();
        tempRunConfig2.delete();
    }

    @Test
    public void testSingleFlow() {
        MockSimpleServer source = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(3);
        MockSimpleServer dest = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(4));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 2000, jobId);
        Simulator.registerEvent(fse);
        Simulator.runNs(1000000000);
        assert (router.routed(0, 1));
        assert (router.recovered(0, 1));
    }

    @Test
    public void testFlowFailure() {
        MockSimpleServer source = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(3);
        MockSimpleServer dest = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(5));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 2000, jobId);
        Simulator.registerEvent(fse);
        source = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(3);
        dest = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(4));
        FlowStartEvent fse2 = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 2000, jobId);
        Simulator.registerEvent(fse);
        Simulator.registerEvent(fse2);
        boolean thrown = false;
        try {
            Simulator.runNs(1000000000);

        } catch (NoPathException e) {
            thrown = true;
        }
        assert (thrown);

    }

    @Test
    public void testFlowFinish() {
        MockSimpleServer source = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(3);
        MockSimpleServer dest = (MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(5));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 2000, jobId);
        Simulator.registerEvent(fse);

        boolean thrown = false;
        Simulator.runNs(1000000000);


    }

    @After
    public void clear() {
        Simulator.reset();
    }

    @AfterClass
    public static void clearRouter() {
        MockOpticalHybrid.setRouter(null);
    }
}
