package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.infrastructure.*;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.OpticElectronicHybrid;
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

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class MegaSwitchTest {
    private File tempRunConfig;

    @Before
    public void setup() throws IOException {
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
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
                        return new OpticElectronicHybrid(i, null, new Intermediary() {
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
                            protected void receiveFromIntermediary(Packet packet) {

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

    }

    @Test
    public void addSecondNetwork() throws IOException {
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n2_v2.topology\n");
        runConfigWriter.write("network_type=circuit_switch\n");
        runConfigWriter.write("link_bandwidth_bit_per_ns=50\n");
        runConfigWriter.write("link_delay_ns=10\n");
        runConfigWriter.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter.write("output_port_ecn_threshold_k_bytes=30000\n");

        runConfigWriter.close();
        NBProperties conf = new NBProperties(
                tempRunConfig2.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );
        BaseInitializer initializer = BaseInitializer.getInstance();

        initializer.extend(
                1,
                new OutputPortGenerator(conf) {
                    @Override
                    public OutputPort generate(NetworkDevice networkDevice, NetworkDevice networkDevice1, Link link) {
                        return new OutputPort(networkDevice,networkDevice , link, null) {
                            @Override
                            public void enqueue(Packet packet) {

                            }
                        };
                    }
                },
                new NetworkDeviceGenerator(conf) {
                    @Override
                    public NetworkDevice generate(int i) {
                        return new NetworkDevice(i, null, new Intermediary() {
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
                            protected void receiveFromIntermediary(Packet packet) {

                            }
                        };
                    }

                    @Override
                    public NetworkDevice generate(int i, TransportLayer transportLayer) {
                        return null;
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
        assertTrue(tempRunConfig2.delete());
        initializer.createInfrastructure(conf);


    }

    @After
    public void cleanup() {
        Simulator.reset();
        assertTrue(tempRunConfig.delete());
    }
}
