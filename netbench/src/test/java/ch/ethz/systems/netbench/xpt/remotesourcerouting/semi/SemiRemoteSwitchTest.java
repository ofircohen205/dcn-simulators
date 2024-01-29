package ch.ethz.systems.netbench.xpt.remotesourcerouting.semi;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.mega_switch.MockOpticalHybrid;
import ch.ethz.systems.netbench.xpt.mega_switch.MockSimpleServer;
import ch.ethz.systems.netbench.xpt.mega_switch.SimpleSocket;
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
public class SemiRemoteSwitchTest {
    private File tempRunConfig;

    @Before
    public void setup() throws IOException {
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        runConfigWriter.write("scenario_topology_file=example/topologies/xpander/xpander_n288_d11.topology\n");
        runConfigWriter.write("semi_remote_routing_path_dir=/cs/labs/schapiram/inonkp/ksp/paths/xpander_n288_d11/10/\n");

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
        initializer.createInfrastructure(conf);
        initializer.finalize();
    }

    @Test
    public void initilizationTest(){
        MockSemiRemoteSwitch mock = (MockSemiRemoteSwitch) BaseInitializer.getInstance().getNetworkDeviceById(0);
        assert(mock.getPathsTo(1).toString().equals("[[0, 83, 212, 161, 1], [0, 137, 254, 56, 94, 1], [0, 262, 52, 232, 213, 1], [0, 262, 273, 127, 99, 1]," +
                " [0, 267, 151, 54, 30, 1], [0, 267, 200, 94, 1], [0, 201, 172, 1], [0, 267, 63, 93, 99, 1], [0, 83, 212, 161, 1], [0, 262, 80, 24, 99, 1]]"));
    }

    @After
    public void clear(){
        Simulator.reset();
        tempRunConfig.delete();
    }
}
