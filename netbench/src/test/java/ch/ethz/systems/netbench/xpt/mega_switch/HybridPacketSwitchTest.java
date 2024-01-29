package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitchGenerator;
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
import java.util.PriorityQueue;

@RunWith(MockitoJUnitRunner.class)
public class HybridPacketSwitchTest {

    @Before
    public void setup() throws IOException {
        // main network
        File tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n2x2_v1.topology\n");
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
                return new OutputPort(own,target,link,new PriorityQueue<Packet>()) {
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
                return new MockElectricHybrid(i,null,ig.generate(i),configuration);
            }

            @Override
            public NetworkDevice generate(int i, TransportLayer transportLayer) {
                return new MockSimpleServer(i,transportLayer,ig.generate(i),configuration);
            }
        };
        LinkGenerator lg = new PerfectSimpleLinkGenerator(0, 10);
        TransportLayerGenerator tlg = new TransportLayerGenerator(conf) {
            @Override
            public TransportLayer generate(int i) {
                return new TransportLayer(i,conf) {

                    @Override
                    protected Socket createSocket(long l, int i, long l1) {
                        return new SimpleSocket(this,l,this.identifier,i,l1);
                    }
                };
            }
        };
        initializer.extend(portGen,ndg,lg,tlg);
        initializer.createInfrastructure(conf);


        //creating network 2:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/simple/simple_n2_v2.topology\n");
        runConfigWriter2.write("network_device_routing=ecmp\n");
        runConfigWriter2.write("network_type=packet_switch");
        runConfigWriter2.close();
        NBProperties conf2 = new NBProperties(
                tempRunConfig2.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );


        OutputPortGenerator portGen2 = new OutputPortGenerator(conf2) {
            @Override
            public OutputPort generate(NetworkDevice own, NetworkDevice target, Link link) {
                return new OutputPort(own,target,link,new PriorityQueue<Packet>()) {
                    @Override
                    public void enqueue(Packet packet) {
                        guaranteedEnqueue(packet);
                    }
                };
            }
        };
        IntermediaryGenerator ig2 = new DemoIntermediaryGenerator(conf2);
        TransportLayerGenerator tlg2 = new TransportLayerGenerator(conf2) {
            @Override
            public TransportLayer generate(int i) {
                return new TransportLayer(i,conf2) {

                    @Override
                    protected Socket createSocket(long l, int i, long l1) {
                        return new SimpleSocket(this,l,this.identifier,i,l1);
                    }
                };
            }
        };

        initializer.extend(1,portGen2,new EcmpSwitchGenerator(new DemoIntermediaryGenerator(conf2),2, conf2),
                lg,tlg2);
        HashMap<Integer,NetworkDevice> hm = initializer.createInfrastructure(conf2);
        RoutingSelector.selectPopulator(hm, conf2).populateRoutingTables();
        initializer.finalize();
        tempRunConfig.delete();
        tempRunConfig2.delete();
    }

    @Test
    public void sendOnePacket(){
        MockSimpleServer source = (MockSimpleServer) BaseInitializer.getInstance().getNetworkDeviceById(2);
        MockSimpleServer dest =(MockSimpleServer) (BaseInitializer.getInstance().getNetworkDeviceById(3));
        MockDemoPacket p = new MockDemoPacket(0, 1000, 2, 3, 10, 0);

        source.receive(p);
        Simulator.runNs(1000000000);
        assert(dest.received);
    }

    @After
    public void clear() {
        Simulator.reset();
    }
}
