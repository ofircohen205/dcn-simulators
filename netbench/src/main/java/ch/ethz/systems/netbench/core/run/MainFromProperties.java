package ch.ethz.systems.netbench.core.run;


import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.TopologyServerExtender;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyConflictException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyMissingException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.core.utility.UnitConverter;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

public class MainFromProperties {

    /**
     * Main from properties file.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        Runtime.getRuntime().freeMemory();
        // Load in the configuration properties
        List<NBProperties> runConfigurations = generateRunConfigurationFromArgs(args);

        SimulationLogger.openCommon(runConfigurations.get(0));


        do {
            // General property: random seed
            long seed = runConfigurations.get(0).getLongPropertyOrFail(Constants.Simulation.SEED);

            // General property: running time in nanoseconds
            long runtimeNs = determineRuntimeNs(runConfigurations.get(0));

            // Setup simulator (it is now public known)
            Simulator.setup(seed, runConfigurations.get(0));
            manageTopology(runConfigurations.get(0));

            for (int j = 0; j < runConfigurations.size(); j++) {
                SimulationLogger.copyRunConfiguration(runConfigurations.get(j));
                HashMap<Integer, NetworkDevice> map = extendInfrastructure(runConfigurations.get(j), j);
                populateRoutingState(map, runConfigurations.get(j));
                Simulator.setIdToNetworkDevice(map);
            }
            BaseInitializer.getInstance().finalization();
            // Copy configuration files for reproducibility
            // Manage topology (e.g. extend with servers if said by configuration)
            // Initialization of the three components
            if (runConfigurations.get(0).getPropertyWithDefault(Constants.Simulation.FROM_STATE, null) == null) {
                planTraffic(runtimeNs, BaseInitializer.getInstance().getIdToTransportLayer());
            }

            NBProperties configuration = Simulator.getConfiguration();

            planCoreFailuresEvents(configuration);

            // Perform run
            System.out.println("ACTUAL RUN\n==================");
            try {
                Simulator.runNs(runtimeNs, configuration.getLongPropertyWithDefault(
                        Constants.Simulation.FINISH_WHEN_FIRST_FLOWS_FINISH, -1));
            } catch (Exception e) {
                e.printStackTrace();
            }

            Simulator.reset(false);
            try {
                SimulationLogger.logCommon(runConfigurations.get(0));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            runConfigurations.get(0).nextSubConfiguration();
        } while (runConfigurations.get(0).hasSubConfiguration());
        SimulationLogger.closeCommon();
    }

    /**
     * Generate the configuration from the arguments.
     * <p>
     * Command-line template:
     * java -jar NetBench.jar /path/to/run_config.properties param1=val1 param2=val2
     *
     * @param args Command-line arguments
     * @return Final run configuration
     */
    private static List<NBProperties> generateRunConfigurationFromArgs(String[] args) {

        // Argument length of at least one
        if (args.length < 1) {
            throw new RuntimeException("Expecting first argument to be configuration properties file for the run.");
        }

        List<NBProperties> propertiesList = new LinkedList<NBProperties>();


        // Load in the configuration properties
        NBProperties runConfiguration = new NBProperties(
                args[0],
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.GREEDY_FLOW_SCHEDULING,
                BaseAllowedProperties.BASE_DIR_VARIANTS,
                BaseAllowedProperties.META_NODE
        );
        propertiesList.add(runConfiguration);
        BufferedReader reader;
        //load properties from environment
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("env.properties");
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {


                int index = line.indexOf('=');
                if (index == -1) {
                    throw new InvalidPropertiesFormatException("arg " + line + " is not a valid property format");
                }
                String param = line.substring(0, index);
                String value = line.substring(index + 1);
                runConfiguration.overrideProperty(param, value);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Dynamic overwrite of temporary config using arguments given from command line
        for (int i = 1; i < args.length; i++) {
            try {
                int index = args[i].indexOf('=');
                if (index == -1) {
                    throw new InvalidPropertiesFormatException("arg " + i + " is not a valid property format");
                }
                String param = args[i].substring(0, index);
                String value = args[i].substring(index + 1);
                runConfiguration.overrideProperty(param, value);
            } catch (InvalidPropertiesFormatException e) {
                runConfiguration = new NBProperties(
                        args[i],
                        BaseAllowedProperties.LOG,
                        BaseAllowedProperties.PROPERTIES_RUN,
                        BaseAllowedProperties.EXTENSION,
                        BaseAllowedProperties.EXPERIMENTAL,
                        BaseAllowedProperties.GREEDY_FLOW_SCHEDULING,
                        BaseAllowedProperties.BASE_DIR_VARIANTS
                );
                propertiesList.add(runConfiguration);
            }

        }

        propertiesList.get(0).loadSubConfigurtations();
        NBProperties.constructBaseDir(propertiesList.get(0), propertiesList);
        try {
            String commonBaseDir = propertiesList.get(0).getPropertyWithDefault(
                    Constants.Simulation.COMMON_BASE_DIR,
                    null);
            if (commonBaseDir == null) {
                return propertiesList;
            }

            if (propertiesList.get(0).getBooleanPropertyWithDefault(
                    Constants.NetworkDeviceGenerator.ENABLE_JUMBO_FLOWS,
                    false)) {
                commonBaseDir += "Jumbo";
            }

            new File(commonBaseDir).mkdirs();
            BufferedWriter bw = new BufferedWriter(new FileWriter(commonBaseDir + "/" +
                    propertiesList.get(0).getPropertyOrFail(Constants.Simulation.COMMON_RUN_NAME)));
            for (NBProperties properties : propertiesList) {
                String[] props = properties.toString().split(", ");
                for (String prop : props) {
                    bw.write(prop + "\n");
                }
                bw.write("\n");
            }
            bw.write(propertiesList.get(0).getPropertyOrFail(Constants.Simulation.RUN_FOLDER_BASE_DIR) + "\n");
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException("couldnt open common base folder");
        }
        return propertiesList;

    }

    /**
     * Determine the amount of running time in nanoseconds.
     *
     * @param runConfiguration Run configuration
     * @return Running time in nanoseconds
     */
    private static long determineRuntimeNs(NBProperties runConfiguration) {

        if (runConfiguration.isPropertyDefined(Constants.Simulation.RUN_TIME_S)
                && runConfiguration.isPropertyDefined(Constants.Simulation.RUN_TIME_NS)) {
            throw new PropertyConflictException(
                    runConfiguration,
                    Constants.Simulation.RUN_TIME_S,
                    Constants.Simulation.RUN_TIME_NS);

        } else if (runConfiguration.isPropertyDefined(Constants.Simulation.RUN_TIME_S)) {
            return UnitConverter.convertSecondsToNanoseconds(
                    runConfiguration.getDoublePropertyOrFail(Constants.Simulation.RUN_TIME_S));

        } else if (runConfiguration.isPropertyDefined(Constants.Simulation.RUN_TIME_NS)) {
            return runConfiguration.getLongPropertyOrFail(Constants.Simulation.RUN_TIME_NS);

        } else {
            throw new PropertyMissingException(runConfiguration, Constants.Simulation.RUN_TIME_S);
        }

    }

    /**
     * Generate the infrastructure (network devices, output ports,
     * links and transport layers) of the run.
     *
     * @param configuration
     * @param networkNum
     * @return Initializer of the infrastructure
     */
    private static HashMap<Integer, NetworkDevice> extendInfrastructure(NBProperties configuration, int networkNum) {
        // Start infrastructure
        System.out.println("\nINFRASTRUCTURE\n==================");
        BaseInitializer initializer = BaseInitializer.getInstance();
        // 1.1) Generate nodes
        initializer.extend(
                networkNum,
                InfrastructureSelector.selectOutputPortGenerator(configuration),
                InfrastructureSelector.selectNetworkDeviceGenerator(configuration),
                InfrastructureSelector.selectLinkGenerator(configuration),
                InfrastructureSelector.selectTransportLayerGenerator(configuration)
        );
        // Finished infrastructure
        System.out.println("Finished creating infrastructure for network num " + networkNum + "\n");
        return initializer.createInfrastructure(configuration);
    }

    /**
     * Populate the routing state.
     *
     * @param idToNetworkDevice Mapping of identifier to network device
     * @param configuration
     */
    private static void populateRoutingState(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        // Start routing
        System.out.println("ROUTING STATE\n==================");
        // 2.1) Populate the routing tables in the switches using the topology defined
        RoutingPopulator populator = RoutingSelector.selectPopulator(idToNetworkDevice, configuration);
        populator.populateRoutingTables();
        // Finish routing
        System.out.println("Finished routing state setup.\n");
    }

    /**
     * Plan the traffic.
     *
     * @param runtimeNs          Running time in nanoseconds
     * @param idToTransportLayer Mapping from node identifier to transport layer
     */
    private static void planTraffic(long runtimeNs, Map<Integer, TransportLayer> idToTransportLayer) {
        // Start traffic generation
        System.out.println("TRAFFIC\n==================");
        // 3.1) Create flow plan for the simulator
        TrafficPlanner planner = TrafficSelector.selectPlanner(idToTransportLayer, Simulator.getConfiguration());
        planner.createPlan(runtimeNs);
        Simulator.setTrafficPlanner(planner);
        SimulationLogger.logInfo("TOTAL_BYTES_IN_PLAN", Long.toString(planner.getTotalBytes()));
        System.out.println("Total bytes in traffic plan " + planner.getTotalBytes());
        // Finish traffic generation
        System.out.println("Finished generating traffic flow starts.\n");

    }

    /**
     * Manage the topology, meaning that the topology can be extended with servers.
     * <p>
     * It uses the following properties:
     * scenario_topology_file=/path/to/topology.txt
     * scenario_topology_extend_with_servers=regular
     * scenario_topology_extend_servers_per_tl_node=4
     * <p>
     * It will override the scenario_topology_file in the existing configuration.
     */
    private static void manageTopology(NBProperties configuration) {
        // Copy of original topology to the run folder
        SimulationLogger.copyFileToRunFolder(configuration.getPropertyOrFail(Constants.ScenarioTopology.FILE), "original_topology.txt");

        // Topology extension
        if (configuration.isPropertyDefined(Constants.ScenarioTopology.EXTEND_WITH_SERVERS)) {
            if (configuration.getPropertyWithDefault(Constants.ScenarioTopology.EXTEND_WITH_SERVERS, "").equals("regular")) {
                // Number of servers to add to each transport layer node
                int serversPerNodeToExtendWith = configuration.getIntegerPropertyOrFail(Constants.ScenarioTopology.EXTEND_SERVERS_PER_TL_NODE);
                // Extend topology
                new TopologyServerExtender(
                        configuration.getTopologyFileNameOrFail(),
                        SimulationLogger.getRunFolderFull() + "/extended_topology.txt"
                ).extendRegular(serversPerNodeToExtendWith);
                // Log info about extension
                SimulationLogger.logInfo("OVERRODE_TOPOLOGY_FILE_WITH_SERVER_EXTENSION", "servers/node=" + serversPerNodeToExtendWith);
                configuration.markExtended();
            } else {
                if (!configuration.getPropertyWithDefault(Constants.ScenarioTopology.EXTEND_WITH_SERVERS, "").equals("none"))
                    throw new PropertyValueInvalidException(configuration, Constants.ScenarioTopology.EXTEND_WITH_SERVERS);
                return;
            }
            // Override configuration property
            configuration.overrideProperty(Constants.ScenarioTopology.FILE, SimulationLogger.getRunFolderFull() + "/extended_topology.txt");
            SimulationLogger.logInfo("ARG_OVERRIDE_PARAM(scenario_topology_file)", SimulationLogger.getRunFolderFull() + "/extended_topology.txt");
        }
    }

    /**
     * Run a command in the prompt (e.g. to call a python script).
     * Error write output is always shown.
     *
     * @param cmdArray   Command to run
     * @param showOutput Whether to show the normal write output from the command in the console
     */
    public static void runCommand(String[] cmdArray, boolean showOutput) {
        String output = StringUtils.EMPTY;
        Process p;
        try {

            System.out.println("Running command \"" + Arrays.toString(cmdArray) + "\"...");

            // Start process
            p = Runtime.getRuntime().exec(cmdArray);

            // Fetch input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            String line;
            while ((line = stdInput.readLine()) != null && !showOutput) {
                output = line;
                System.out.println(output);
            }

            String error;
            // Read any errors from the attempted command
            while ((error = stdError.readLine()) != null) {
                System.out.println(error);
            }

            // Wait for the command thread to be ended
            p.waitFor();
            p.destroy();

            System.out.println("... command has been executed (any output is shown above).");

        } catch (Exception e) {
            throw new RuntimeException("Command failed: " + Arrays.toString(cmdArray));
        }
    }

    private static void planCoreFailuresEvents(NBProperties configuration) {
        int numFailedNodes = configuration.getIntegerPropertyWithDefault(Constants.Link.NUM_FAILED_NODES, 0);
        if (numFailedNodes == 0) {
            return;
        }
        System.out.println("PLANNING LINK EVENTS\n==================");
        List<Integer> coreIds = new ArrayList<>(configuration.getGraphDetails().getCoreNodeIds());
        Collections.sort(coreIds);

        for (int i = 0; i < numFailedNodes; i++) {
            int failedCoreId = coreIds.get(i);
            configuration.getGraphDetails().getFailedCores().add(failedCoreId);
        }
    }
}
