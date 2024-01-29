package ch.ethz.systems.netbench.xpt.remotesourcerouting.semi;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;

@RunWith(MockitoJUnitRunner.class)
public class SemiXpanderTest {
    private File tempRunConfig;

    @Before
    public void setup() throws IOException {
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        runConfigWriter.write("scenario_topology_file=example/topologies/xpander/xpander_n288_d11.topology\n");
        runConfigWriter.write("semi_remote_routing_path_dir=/cs/labs/schapiram/inonkp/ksp/paths/xpander_n288_d11/10/\n");
        runConfigWriter.write("centered_routing_type=semi_Xpander\n");
        runConfigWriter.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter.write("circuit_wave_length_num=1\n");

        BaseInitializer initializer = BaseInitializer.getInstance();
        runConfigWriter.close();
        NBProperties conf = new NBProperties(
                tempRunConfig.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );
        Simulator.setup(0,conf);
        OutputPortGenerator portGen = new OutputPortGenerator(conf) {
            @Override
            public OutputPort generate(NetworkDevice own, NetworkDevice target, Link link) {
                return new OutputPort(own,target,link,new PriorityQueue<Packet>()) {
                    @Override
                    public void enqueue(Packet packet) {

                    }
                };
            }
        };
        IntermediaryGenerator ig = new DemoIntermediaryGenerator(conf);
        NetworkDeviceGenerator ndg = new NetworkDeviceGenerator(conf) {
            @Override
            public NetworkDevice generate(int i) {
                return new MockSemiRemoteSwitch(i,null,ig.generate(i),configuration);
            }

            @Override
            public NetworkDevice generate(int i, TransportLayer transportLayer) {
                return null;
            }
        };
        LinkGenerator lg = new PerfectSimpleLinkGenerator(0, 10);
        LinkGenerator lg2 = new PerfectSimpleLinkGenerator(0, 20);
        TransportLayerGenerator tlg = new TransportLayerGenerator(conf) {
            @Override
            public TransportLayer generate(int i) {
                return new TransportLayer(i,conf) {

                    @Override
                    protected Socket createSocket(long l, int i, long l1) {
                        return null;
                    }
                };
            }
        };


        initializer.extend(portGen,ndg,lg,tlg);

        RoutingSelector.selectPopulator(initializer.createInfrastructure(conf),conf);
        initializer.finalize();
    }

    @Test
    public void simpleTest(){
        RemoteRoutingController.getInstance().initRoute(0,1,0);
        MockSemiRemoteSwitch semi = (MockSemiRemoteSwitch) BaseInitializer.getInstance().getNetworkDeviceById(0);
        assert(semi.getNextHop(0).getTargetId()==83);
        RemoteRoutingController.getInstance().recoverPath(0,1,0);
    }

    @After
    public void clear(){
        tempRunConfig.delete();
        Simulator.reset();
    }

}
