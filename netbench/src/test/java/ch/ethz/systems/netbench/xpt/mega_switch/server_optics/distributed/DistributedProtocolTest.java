package ch.ethz.systems.netbench.xpt.mega_switch.server_optics.distributed;

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
import ch.ethz.systems.netbench.xpt.mega_switch.SimpleSocket;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.OpticServerGenerator;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedProtocolPort;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.semi.SemiRemoteRoutingSwitchGenerator;
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

@RunWith(MockitoJUnitRunner.class)
public class DistributedProtocolTest {
    MockDistributedServerOpticsRouter router;

    @Before
    public void setup() throws IOException {
        // main network
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n2x2_v1.topology\n");
        runConfigWriter.write("switching_time_ns=0\n");
        runConfigWriter.write("hybrid_circuit_threshold_byte=5000\n");
        runConfigWriter.write("run_folder_base_dir=/cs/usr/inonkp/distributed_protocol_testing\n");
        runConfigWriter.write("run_folder_name=results\n");
        runConfigWriter.write("static_configuration_time_ns=1000\n");
        runConfigWriter.write("num_paths_to_randomize=1\n");
        runConfigWriter.write("distributed_protocol_enabled=true\n");


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
                return new DistributedProtocolPort(own, target, link, 150000, 30000);
            }
        };
        IntermediaryGenerator ig = new DemoIntermediaryGenerator(conf);
        NetworkDeviceGenerator ndg = new OpticServerGenerator(conf) {
            @Override
            public NetworkDevice generate(int identifier) {
                return new MockDistributedToR(identifier, null, ig.generate(identifier), conf);
            }

            @Override
            public NetworkDevice generate(int identifier, TransportLayer server) {
                return new MockDistributedOpticServer(identifier, server, ig.generate(identifier), conf);
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
        runConfigWriter2.write("centered_routing_type=distributed_controller\n");
        runConfigWriter2.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter2.write("circuit_wave_length_num=1\n");
        runConfigWriter2.write("max_num_flows_on_circuit=5\n");
        runConfigWriter2.write("host_optics_enabled=true\n");
        runConfigWriter2.write("network_type=circuit_switch\n");
        runConfigWriter2.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter2.write("output_port_ecn_threshold_k_bytes=30000\n");
        runConfigWriter2.write("link_bandwidth_bit_per_ns=50\n");
        runConfigWriter2.write("semi_remote_routing_path_dir=/cs/labs/schapiram/inonkp/ksp/paths/xpander_n333_d8/10\n");
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


        initializer.extend(1, new LightOutputPortGenerator(conf2), new SemiRemoteRoutingSwitchGenerator(new DemoIntermediaryGenerator(conf2), conf2),
                lg, new RemoteRoutingTransportLayerGenerator(conf2));
        HashMap<Integer, NetworkDevice> hm = initializer.createInfrastructure(conf2);
        //RoutingSelector.selectPopulator(hm, conf2);
        router = new MockDistributedServerOpticsRouter(hm, conf2);
        MockDistributedOpticServer.setRemoteRouter(router);
        MockDistributedToR.setRemoteRouter(router);
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
                return new DistributedProtocolPort(own, target, link, 150000, 30000);
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
    public void testSingleFlow() {
        MockDistributedOpticServer source = (MockDistributedOpticServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockDistributedOpticServer dest = (MockDistributedOpticServer) (BaseInitializer.getInstance().getNetworkDeviceById(3));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 20000, jobId);
        Simulator.registerEvent(fse);
        Simulator.runNs(1000000000);
        assert (router.routedFlow(0));
        assert (source.allocatedColor(0) == 1);
        assert (source.deallocatedColor(0) == 1);
        assert (dest.deallocatedColor(0) == 1);
        assert (dest.allocatedColor(0) == 1);
        assert (router.allocatedEdge(0, 1, 0) == 1);
        assert (router.deallocatedEdge(0, 1, 0) == 1);
        assert (source.getResrvationPacket(3).getDelay() == 1000);
//        MockDemoPacket mdp = new MockDemoPacket(0, 1000, 2, 3,100,0);
//        LinkedList<Integer> p = new LinkedList<>();
//        p.add(0,1);
//        ReservationPacket rp = new ReservationPacket(mdp,1,p,0,true);
//        source.routeThroughtPacketSwitch(rp);

    }

    @Test
    public void testDoubleFlowSingleSource() {
        MockDistributedOpticServer source = (MockDistributedOpticServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockDistributedOpticServer dest = (MockDistributedOpticServer) (BaseInitializer.getInstance().getNetworkDeviceById(3));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 20000, jobId);
        FlowStartEvent fse2 = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 20000, jobId);
        Simulator.registerEvent(fse);
        Simulator.registerEvent(fse2);
        Simulator.runNs(1000000000);
//        assert(router.routedFlow(0));
//        assert(source.allocatedColor(0)==1);
//        assert(source.deallocatedColor(0)==1);
//        assert(dest.deallocatedColor(0)==1);
//        assert(dest.allocatedColor(0)==1);
//        assert(router.allocatedEdge(0,1,0)==1);
//        assert(router.deallocatedEdge(0,1,0)==1);
//        assert(source.getResrvationPacket(3).getDelay()==1000);
//        MockDemoPacket mdp = new MockDemoPacket(0, 1000, 2, 3,100,0);
//        LinkedList<Integer> p = new LinkedList<>();
//        p.add(0,1);
//        ReservationPacket rp = new ReservationPacket(mdp,1,p,0,true);
//        source.routeThroughtPacketSwitch(rp);

    }

    @Test
    public void testDoubleFlow() {
        System.out.println("testDoubleFlow start");
        MockDistributedOpticServer source = (MockDistributedOpticServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockDistributedOpticServer dest = (MockDistributedOpticServer) (BaseInitializer.getInstance().getNetworkDeviceById(3));
        MockDistributedOpticServer dest2 = (MockDistributedOpticServer) (BaseInitializer.getInstance().getNetworkDeviceById(5));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 200000, jobId);
        FlowStartEvent fse2 = new FlowStartEvent(0, source.getTransportLayer(), dest2.getIdentifier(), 200000, jobId);
        Simulator.registerEvent(fse);
        Simulator.registerEvent(fse2);
        Simulator.runNs(1000000000);
        System.out.println("testDoubleFlow end");
        assert (router.routedFlow(0));
        assert (router.routedFlow(1));
        assert (source.allocatedColor(0) == 2);
        assert (source.deallocatedColor(0) == 2);
        assert (dest.deallocatedColor(0) == 1);
        assert (dest.allocatedColor(0) == 1);
        assert (dest2.deallocatedColor(0) == 1);
        assert (dest2.allocatedColor(0) == 1);
        assert (router.allocatedEdge(0, 1, 0) == 2);
        assert (router.deallocatedEdge(0, 1, 0) == 2);


//        MockDemoPacket mdp = new MockDemoPacket(0, 1000, 2, 3,100,0);
//        LinkedList<Integer> p = new LinkedList<>();
//        p.add(0,1);
//        ReservationPacket rp = new ReservationPacket(mdp,1,p,0,true);
//        source.routeThroughtPacketSwitch(rp);

    }

    @Test
    public void testTrivialFlow() {
        MockDistributedOpticServer source = (MockDistributedOpticServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockDistributedOpticServer dest = (MockDistributedOpticServer) (BaseInitializer.getInstance().getNetworkDeviceById(4));
        FlowStartEvent fse = new FlowStartEvent(0, source.getTransportLayer(), dest.getIdentifier(), 200000, jobId);
        Simulator.registerEvent(fse);
        Simulator.runNs(1000000000);
        assert (router.routedFlow(0));
        assert (source.allocatedColor(0) == 1);
        assert (source.deallocatedColor(0) == 1);
        assert (dest.deallocatedColor(0) == 1);
        assert (dest.allocatedColor(0) == 1);
        assert (router.allocatedEdge(0, 1, 0) == 0);
        assert (router.deallocatedEdge(0, 1, 0) == 0);
//        MockDemoPacket mdp = new MockDemoPacket(0, 1000, 2, 3,100,0);
//        LinkedList<Integer> p = new LinkedList<>();
//        p.add(0,1);
//        ReservationPacket rp = new ReservationPacket(mdp,1,p,0,true);
//        source.routeThroughtPacketSwitch(rp);

    }

    @After
    public void finish() {
        Simulator.reset(false);
    }
}
