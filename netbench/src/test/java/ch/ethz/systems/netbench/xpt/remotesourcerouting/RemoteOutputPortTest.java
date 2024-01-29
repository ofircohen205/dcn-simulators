package ch.ethz.systems.netbench.xpt.remotesourcerouting;

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
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.mega_switch.MockOpticalHybrid;
import ch.ethz.systems.netbench.xpt.mega_switch.MockSimpleServer;
import ch.ethz.systems.netbench.xpt.mega_switch.SimpleSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@RunWith(MockitoJUnitRunner.class)
public class RemoteOutputPortTest {
    @Mock
    private TcpPacket packet;
    NBProperties conf;
    private File tempRunConfig;
    private MockRemoteRouter remoteRouter;
    private Map<Integer, NetworkDevice> idToNetworkDevice;

    @Before
    public void setup() throws IOException {

        // main network
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/chain/chain_2_servers.topology\n");
        runConfigWriter.write("hybrid_circuit_threshold_byte=0\n");
        runConfigWriter.write("circuit_wave_length_num=1\n");


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
                return new OutputPort(own, target, link, new PriorityQueue<Packet>()) {
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
        LinkGenerator lg2 = new PerfectSimpleLinkGenerator(0, 20);
        TransportLayerGenerator tlg = new TransportLayerGenerator(conf) {
            @Override
            public TransportLayer generate(int i) {
                return new TransportLayer(i, conf) {

                    @Override
                    protected Socket createSocket(long l, int i, long l1) {
                        return new SimpleSocket(this, l, this.identifier, i, l1);
                    }
                };
            }
        };
        initializer.extend(portGen, ndg, lg, tlg);
        idToNetworkDevice = initializer.createInfrastructure(conf);


        //creating network 2:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/chain/chain_10_ToRs.topology\n");
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
                lg2, new RemoteRoutingTransportLayerGenerator(conf2));
        HashMap<Integer, NetworkDevice> hm = initializer.createInfrastructure(conf2);
        RoutingSelector.selectPopulator(hm, conf2);
        initializer.finalize();
        tempRunConfig.delete();
        tempRunConfig2.delete();
    }

    @Test
    public void testNoQueues() {
        Event event = new FlowStartEvent(0, idToNetworkDevice.get(11).getTransportLayer(), 10, 1000, jobId);
        Simulator.registerEvent(event);
        Simulator.runNs(100000000);
    }

    @After
    public void clear() {
        Simulator.reset();
    }
}
