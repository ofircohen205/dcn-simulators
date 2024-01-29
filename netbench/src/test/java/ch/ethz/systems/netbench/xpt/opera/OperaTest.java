package ch.ethz.systems.netbench.xpt.opera;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.TopologyServerExtender;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.network.InputPort;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;
import ch.ethz.systems.netbench.ext.flowlet.UniformFlowletIntermediary;
import ch.ethz.systems.netbench.xpt.dynamic.opera.OperaController;
import ch.ethz.systems.netbench.xpt.dynamic.opera.OperaSwitch;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpTransportLayer;
import ch.ethz.systems.netbench.xpt.simple.simpleserver.SimpleServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class OperaTest {
    MockOperaController operaController;
    NBProperties configuration;
    long simulationDuration = 1000000;

    @Before
    public void setup() throws IOException {
        configuration = new NBProperties(BaseAllowedProperties.EXPERIMENTAL, BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION, BaseAllowedProperties.LOG);
        configuration.setProperty("opera_rotors_dir_path", "xpander_generation/opera/testing/n20/rotors");
        configuration.setProperty("opera_direct_circuit_threshold_byte", "40000");
        configuration.setProperty("opera_reconfiguration_time_ns", "20000000");
        configuration.setProperty("opera_parallel_rotors_to_config", "1");
        configuration.setProperty("opera_routing_tables_dir_path", "xpander_generation/opera/testing/n20/k_shortest_routing");
        configuration.setProperty("scenario_topology_file", "xpander_generation/opera/testing/n20/n20.topology");
        configuration.setProperty("link_delay_ns", "0");
        configuration.setProperty("FLOWLET_GAP_NS", "50000");
        configuration.setProperty("run_folder_name", "opera");
        configuration.setProperty("run_folder_base_dir", "temp");
        configuration.setProperty("link_bandwidth_bit_per_ns", "100");
        configuration.setProperty("scenario_topology_extend_with_servers", "regular");
        configuration.setProperty("scenario_topology_extend_servers_per_tl_node", "1");
        Simulator.setup(1, configuration);

        if (configuration.isPropertyDefined("scenario_topology_extend_with_servers")) {
            if (configuration.getPropertyWithDefault("scenario_topology_extend_with_servers", "").equals("regular")) {

                // Number of servers to add to each transport layer node
                int serversPerNodeToExtendWith = configuration.getIntegerPropertyOrFail("scenario_topology_extend_servers_per_tl_node");

                // Extend topology
                new TopologyServerExtender(
                        configuration.getTopologyFileNameOrFail(),
                        "temp/opera/" + "/extended_topology.txt"
                ).extendRegular(serversPerNodeToExtendWith);


                configuration.markExtended();
            } else {
                throw new PropertyValueInvalidException(configuration, "scenario_topology_extend_with_servers");
            }

            // Override configuration property
            configuration.overrideProperty("scenario_topology_file", "temp/opera/" + "/extended_topology.txt");

        }

        HashMap<Integer, TransportLayer> transportLayerHashMap = new HashMap<>();
        HashMap<Integer, NetworkDevice> networkDeviceHashMap = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            SimpleDctcpTransportLayer transportLayer = new SimpleDctcpTransportLayer(i, configuration);
            NetworkDevice os;
            if (i < 20) {
                os = new OperaSwitch(i, null, null, configuration);
            } else {
                os = new SimpleServer(i, transportLayer, new UniformFlowletIntermediary(configuration), configuration);
                InputPort portBtoA = new InputPort(os, networkDeviceHashMap.get(i - 20), new Link() {
                    @Override
                    public long getDelayNs() {
                        return 0;
                    }

                    @Override
                    public long getBandwidthBitPerNs() {
                        return 100;
                    }

                    @Override
                    public boolean doesNextTransmissionFail(long packetSizeBits) {
                        return false;
                    }
                });
                os.addIncomingConnection(portBtoA);
                networkDeviceHashMap.get(i - 20).addConnection(new EcnTailDropOutputPort(networkDeviceHashMap.get(i - 20), os, new Link() {
                    @Override
                    public long getDelayNs() {
                        return 0;
                    }

                    @Override
                    public long getBandwidthBitPerNs() {
                        return 100;
                    }

                    @Override
                    public boolean doesNextTransmissionFail(long packetSizeBits) {
                        return false;
                    }
                }, 150000000, 3000000, new LinkedList<>()) {

                });
                os.addConnection(new EcnTailDropOutputPort(os, networkDeviceHashMap.get(i - 20), new Link() {
                    @Override
                    public long getDelayNs() {
                        return 0;
                    }

                    @Override
                    public long getBandwidthBitPerNs() {
                        return 100;
                    }

                    @Override
                    public boolean doesNextTransmissionFail(long packetSizeBits) {
                        return false;
                    }
                }, 150000000, 3000000, new LinkedList<>()) {

                });
                transportLayer.setNetworkDevice(os);
                transportLayerHashMap.put(i, transportLayer);
            }


            networkDeviceHashMap.put(i, os);

        }

        // operaController = new MockOperaController(configuration,networkDeviceHashMap);
        // operaController.populateRoutingTables();
        OperaController.init(configuration, networkDeviceHashMap);
        OperaController.getInstance().populateRoutingTables();
        TrafficPlanner planner = new TrafficPlanner(transportLayerHashMap, configuration) {
            @Override
            public void createPlan(long durationNs) {
                Random random = Simulator.selectIndependentRandom("opera_test_traffic_planner");

                for (long t = 0; t < durationNs; t += durationNs) {
                    int source = Math.abs(random.nextInt(20)) % 20 + 20;
                    int dest = Math.abs(random.nextInt(20)) % 20 + 20;
                    while (dest == source) {
                        dest = Math.abs(random.nextInt(20)) % 20 + 20;
                    }
                    registerFlow(t, source, dest, 1000000000, -1);
                }
            }
        };

        planner.createPlan(simulationDuration);

    }

    @Test
    public void testInitialization() {
        //empty test just to check we loaded withoug exceptions
    }

    @Test
    public void testSomeRoutes() {
        assert (OperaController.getInstance().getPossiblities(0, 6).toString().equals("[[(2,6)], [(0,2), (1,15), (0,12), (3,6)]]"));
        assert (OperaController.getInstance().getPossiblities(3, 4).toString().equals("[[(2,19), (1,4)], [(0,6), (2,0), (1,17), (0,4)]]"));
    }

    @Test
    public void testConfiguringCorrectRotors() {
        for (int i = 0; i < 30; i++) {
            System.out.println(operaController.getCurrCycle());
            System.out.println(operaController.getNextRotors());
            operaController.reconfigure();
            operaController.onReconfigurationEnd();
        }
        System.out.println();
        operaController.reset();
        operaController.setParellelNum(1);
        for (int i = 0; i < 30; i++) {
            System.out.println(operaController.getCurrCycle());
            System.out.println(operaController.getNextRotors());
            operaController.reconfigure();
            operaController.onReconfigurationEnd();
        }
        System.out.println();
        operaController.reset();

        operaController.setParellelNum(3);
        for (int i = 0; i < 10; i++) {
            System.out.println(operaController.getCurrCycle());
            System.out.println(operaController.getNextRotors());
            operaController.reconfigure();
            operaController.onReconfigurationEnd();
        }


    }

    @Test
    public void testOperaRun() {
        Simulator.runNs(simulationDuration);
    }

    @After
    public void reset() {
        Simulator.reset(false);
    }
}
