package ch.ethz.systems.netbench.xpt.meta_node;


import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.xpt.simple.simpleserver.SimpleServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class MetaNodeControllerTest {
    private File tempRunConfig;
    MockMNController instance;
    @Before
    public void setup() throws IOException {
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        runConfigWriter.write("scenario_topology_file=./src/test/java/ch/ethz/systems/netbench/xpt/meta_node/S50_O0.5_L5_D6.topology\n");
        runConfigWriter.write("link_bandwidth_bit_per_ns=50\n");
        runConfigWriter.write("link_delay_ns=10\n");
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
        BaseInitializer initializer = BaseInitializer.getInstance();
        // 1.1) Generate nodes
        initializer.extend(
                0,
                new OutputPortGenerator(conf) {
                    @Override
                    public OutputPort generate(NetworkDevice networkDevice, NetworkDevice networkDevice1, Link link) {
                        return new OutputPort(networkDevice,networkDevice1 , link, null) {
                            @Override
                            public void enqueue(Packet packet) {

                            }
                        };
                    }
                },
                new NetworkDeviceGenerator(conf) {
                    @Override
                    public NetworkDevice generate(int i) {
                        return new MetaNodeSwitch(i, null, conf.getGraphDetails().getNumTors(), new Intermediary() {
                            @Override
                            public Packet adaptOutgoing(Packet packet) {
                                return null;
                            }

                            @Override
                            public Packet adaptIncoming(Packet packet) {
                                return null;
                            }
                        }, conf) {
                            @Override
                            public void receive(Packet genericPacket) {

                            }

                            @Override
                            public void receiveFromIntermediary(Packet packet) {

                            }
                        };
                    }

                    @Override
                    public NetworkDevice generate(int i, TransportLayer transportLayer) {
                        return new SimpleServer(i, transportLayer, new Intermediary() {
                            @Override
                            public Packet adaptOutgoing(Packet packet) {
                                return null;
                            }

                            @Override
                            public Packet adaptIncoming(Packet packet) {
                                return null;
                            }
                        },conf);
                    }
                },
                new LinkGenerator() {
                    @Override
                    public Link generate(NetworkDevice networkDevice, NetworkDevice networkDevice1) {
                        return null;
                    }
                },
                new TransportLayerGenerator(conf) {
                    @Override
                    public TransportLayer generate(int i) {
                        return new TransportLayer(i,conf) {
                            @Override
                            protected Socket createSocket(long l, int i, long l1) {
                                return null;
                            }
                        };
                    }
                }
        );
        initializer.createInfrastructure(conf);
        instance = MockMNController.getInstance(conf,initializer.getIdToNetworkDevice());
    }

    @Test
    public void calcMMatchingNumTest(){
        assert MockMNController.getInstance().getMatchingsNum() == 1;
    }

    @Test
    public void getTokenTest(){
        MockMNController controller = MockMNController.getInstance();
        MetaNodeToken token = controller.getToken(0,1);
        assert token.getMiddleHop() == 1;
        token = controller.getToken(0,1);
        assert token.getMiddleHop() == 1;
        token = controller.getToken(0,1);
        int remoteDest = token.getMiddleHop();
        assert remoteDest != 1;
        token = controller.getToken(0,1);
        assert token.getMiddleHop() != remoteDest;

    }

    @After
    public void clear(){
        Simulator.reset(true);
    }


}


