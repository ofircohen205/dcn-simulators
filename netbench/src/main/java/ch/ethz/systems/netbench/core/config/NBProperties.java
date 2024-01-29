package ch.ethz.systems.netbench.core.config;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.exceptions.ConfigurationReadFailException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyMissingException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyNotExistingException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.utility.Constants;
import edu.asu.emit.algorithm.graph.Graph;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * NetBench properties manager.
 * <p>
 * Manages all the properties defined in the NetBench properties
 * configuration file. Exits the program if properties are defined
 * that do not exist in the NetBench system.
 */
public class NBProperties extends Properties {

    // Set of allowed properties
    private final Set<String> allowedProperties;

    // Master run properties file (not the default)
    private final String fileName;
    private LinkedList<NBProperties> subConfigs;
    // Cache properties variables
    private boolean graphIsRead;
    private Graph graph;
    private GraphDetails graphDetails;

    private NBProperties currSubConfig;

    /**
     * Create property manager without anything set.
     *
     * @param additionalAllowedProperties Additional allowed properties
     */
    public NBProperties(String[]... additionalAllowedProperties) {
        this.fileName = null;

        // Cached properties variables
        this.graphIsRead = false;
        this.graph = null;
        this.graphDetails = null;
        this.allowedProperties = new HashSet<>();
        initializeAllowedProperties(additionalAllowedProperties);

        // Make sure all properties are legal
        this.checkProperties();

    }

    /**
     * Create property manager solely from a file.
     *
     * @param fileName                    File name (e.g. C:/utils/run.properties)
     * @param additionalAllowedProperties Additional allowed properties
     */
    public NBProperties(String fileName, String[]... additionalAllowedProperties) {
        this.fileName = fileName;
        // Read completely from file
        try {
            InputStream input = Files.newInputStream(Paths.get(fileName));
            this.load(input);
            input.close();
        } catch (IOException ex) {
            throw new ConfigurationReadFailException(this, ex);
        }

        // Cached properties variables
        this.graphIsRead = false;
        this.graph = null;
        this.graphDetails = null;
        this.allowedProperties = new HashSet<>();
        initializeAllowedProperties(additionalAllowedProperties);
        subConfigs = new LinkedList<NBProperties>();
        // Make sure all properties are legal
        this.checkProperties();


    }

    public NBProperties(NBProperties configuration) {
        super();
        configuration.forEach((key, value) -> {
            this.setProperty((String) key, (String) value);
        });
        fileName = configuration.fileName;
        graphIsRead = configuration.graphIsRead;
        graph = configuration.graph;
        graphDetails = configuration.graphDetails;
        allowedProperties = configuration.allowedProperties;
        subConfigs = configuration.subConfigs;
    }

    public static void constructBaseDir(NBProperties mainProperties, List<NBProperties> propertiesList) {
        String baseDir = mainProperties.getPropertyOrFail(Constants.Simulation.RUN_FOLDER_BASE_DIR);
        System.out.println("Run folder base dir: " + baseDir);
        baseDir = new File(baseDir).getAbsolutePath();
        mainProperties.setProperty("run_folder_base_dir", baseDir);
    }

    public void loadSubConfigurtations() {
        if (this.containsKey("sub_configurations_folder")) {
            File dir = new File(this.getProperty("sub_configurations_folder"));
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null && files.length == 0) {
                    throw new RuntimeException("no files in sub configuration folder");
                }
                for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
                    subConfigs.add(new NBProperties(files[i].getAbsolutePath(), allowedProperties.toArray(new String[allowedProperties.size()])));
                }
                currSubConfig = subConfigs.poll();
            } else {
                throw new PropertyValueInvalidException(this, "sub_configurations_folder");

            }
        }

    }

    /**
     * Initialize all allowed properties.
     *
     * @param additionalAllowedProperties Additional allowed properties arrays
     */
    private void initializeAllowedProperties(String[]... additionalAllowedProperties) {

        // Additional properties
        int totalAdded = 0;
        for (String[] addProperties : additionalAllowedProperties) {
            allowedProperties.addAll(Arrays.asList(addProperties));
            totalAdded += addProperties.length;
        }

        // Check for no duplicates
        if (allowedProperties.size() != totalAdded) {
            throw new IllegalArgumentException("Cannot allow a property twice, " +
                    "there is a duplicate between the arrays of allowed properties.");
        }

    }

    /**
     * Check that all properties are indeed allowed.
     * Does not check values.
     * <p>
     * Throws a <i>PropertyNotExistingException</i> if
     * a property defined does not exist in the NetBench system.
     */
    private void checkProperties() {

        // If it was not found among them, it is an illegal property name
        for (Object keyObj : this.keySet()) {
            checkIsAllowedProperty((String) keyObj);
        }

    }

    /**
     * Check whether a key is a valid property.
     * Throws an exception if not.
     *
     * @param key Key
     */
    private void checkIsAllowedProperty(String key) {
        for (String s : allowedProperties) {
            if (key.equals(s)) {
                return;
            }
        }
        throw new PropertyNotExistingException(this, key);
    }

    @Override
    public String getProperty(String key) {
        if (currSubConfig == null) {
            return super.getProperty(key);
        }
        String prop = currSubConfig.getProperty(key);

        if (prop == null) {
            return super.getProperty(key);
        }
        return prop;
    }

    /**
     * Retrieve the property value as a string or throw
     * a runtime exception if the property does not exist.
     *
     * @param key Property key
     * @return Property value
     */
    public String getPropertyOrFail(String key) {
        checkIsAllowedProperty(key);
        String result = this.getProperty(key);
        if (result == null) {
            throw new PropertyMissingException(this, key);
        } else {
            return result;
        }
    }

    /**
     * Check whether a property is defined.
     *
     * @param key Property key
     * @return True iff a property with the given key is defined (not null)
     */
    public boolean isPropertyDefined(String key) {
        checkIsAllowedProperty(key);
        return this.getProperty(key) != null;
    }

    /**
     * Retrieve the property value as an integer or throw
     * a runtime exception if the property does not exist.
     * Can throw number format exception if it fails to convert
     * the value to an integer.
     *
     * @param key Property key
     * @return Property integer value
     */
    public int getIntegerPropertyOrFail(String key) {
        return Integer.parseInt(getPropertyOrFail(key));
    }

    /**
     * Retrieve the property value as an boolean or throw
     * a runtime exception if the property does not exist.
     * Can run time exception if it fails to convert
     * the value to a boolean.
     *
     * @param key Property key
     * @return Property boolean value
     */
    public boolean getBooleanPropertyOrFail(String key) {
        String foundValue = this.getPropertyOrFail(key);
        switch (foundValue) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw new PropertyValueInvalidException(this, key);
        }
    }

    /**
     * Retrieve the property value as a long or throw
     * a runtime exception if the property does not exist.
     * Can throw number format exception if it fails to convert
     * the value to a long.
     *
     * @param key Property key
     * @return Property long value
     */
    public long getLongPropertyOrFail(String key) {
        return Long.parseLong(getPropertyOrFail(key));
    }

    /**
     * Retrieve the property value as a double or throw
     * a runtime exception if the property does not exist.
     * Can throw number format exception if it fails to convert
     * the value to a double.
     *
     * @param key Property key
     * @return Property double value
     */
    public double getDoublePropertyOrFail(String key) {
        return Double.parseDouble(getPropertyOrFail(key));
    }

    /**
     * Retrieve a directed pair list.
     *
     * @param key Property key
     * @return Even-sized list where even are the left side, uneven are the right side,
     * as such "a->3,c->d" returns "[a, 3, c, d]
     */
    public List<Integer> getDirectedPairsListPropertyOrFail(String key) {
        String list = "";
        try {
            list = readPropertyFromFileOrFail(key);
        } catch (Exception e) {
            list = getPropertyOrFail(key);
        }

        List<Integer> integerList = new ArrayList<>();
        for (String s : list.split(",")) {
            for (String s2 : s.split("->")) {
                integerList.add(Integer.valueOf(s2));
            }
        }
        return integerList;
    }

    public String readPropertyFromFileOrFail(String key) throws Exception {
        String fileName = getPropertyOrFail(key);
        String property = "";

        InputStream input = Files.newInputStream(Paths.get(fileName));
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        property = out.toString();   //Prints the string content read from input stream
        reader.close();
        input.close();

        return property;

    }

    public List<Integer> getDirectedPairsListPropertyFromFileOrFail(String key) {
        String list = getPropertyOrFail(key);
        List<Integer> integerList = new ArrayList<>();
        for (String s : list.split(",")) {
            for (String s2 : s.split("->")) {
                integerList.add(Integer.valueOf(s2));
            }
        }
        return integerList;
    }

    /**
     * Retrieve primary file used for the NBProperties (does not include the default file).
     *
     * @return File name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Retrieve the topology (properties key: scenario_topology_file) from file
     *
     * @return Graph instance of the topology
     */
    public Graph getGraph() {
        if (!graphIsRead) {
            readGraph();
        }
        return graph;
    }

    /**
     * Retrieve the graph details from file (properties key: scenario_topology_file).
     *
     * @return Graph instance of the topology
     */
    public GraphDetails getGraphDetails() {
        if (!graphIsRead) {
            readGraph();
        }
        return graphDetails;
    }

    /**
     * Read in the graph and its details.
     */
    private void readGraph() {
        Pair<Graph, GraphDetails> result = GraphReader.read(
                this.getPropertyOrFail(Constants.ScenarioTopology.FILE)
        );
        graph = result.getLeft();
        graphDetails = result.getRight();
        graphIsRead = true;
    }

    /**
     * Retrieve the topology file name (properties key: scenario_topology_file).
     *
     * @return Topology file name
     */
    public String getTopologyFileNameOrFail() {
        return this.getPropertyOrFail(Constants.ScenarioTopology.FILE);
    }

    /**
     * Retrieve a boolean property with a default if it is not found.
     * Will fail if the property value string is anything other than "true" or "false".
     *
     * @param key          Property key
     * @param defaultValue Default boolean value
     * @return Boolean property value
     */
    public boolean getBooleanPropertyWithDefault(String key, boolean defaultValue) {
        checkIsAllowedProperty(key);
        String foundValue = this.getProperty(key);
        if (foundValue != null) {
            switch (foundValue) {
                case "true":
                    return true;
                case "false":
                    return false;
                default:
                    throw new PropertyValueInvalidException(this, key);
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieve a long property with a default if it is not found.
     *
     * @param key          Property key
     * @param defaultValue Default long value
     * @return Long property value
     */
    public long getLongPropertyWithDefault(String key, long defaultValue) {
        checkIsAllowedProperty(key);
        String foundValue = this.getProperty(key);
        if (foundValue != null) {
            return Long.parseLong(foundValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieve a double property with a default if it is not found.
     *
     * @param key          Property key
     * @param defaultValue Default double value
     * @return Double property value
     */
    public double getDoublePropertyWithDefault(String key, double defaultValue) {
        checkIsAllowedProperty(key);
        String foundValue = this.getProperty(key);
        if (foundValue != null) {
            return Double.parseDouble(foundValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieve a integer property with a default if it is not found.
     *
     * @param key          Property key
     * @param defaultValue Default integer value
     * @return Integer property value
     */
    public int getIntegerPropertyWithDefault(String key, int defaultValue) {
        checkIsAllowedProperty(key);
        String foundValue = this.getProperty(key);
        if (foundValue != null) {
            return Integer.parseInt(foundValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieve a (string) property with a default if it is not found.
     *
     * @param key          Property key
     * @param defaultValue Default string value
     * @return String property value
     */
    public String getPropertyWithDefault(String key, String defaultValue) {
        checkIsAllowedProperty(key);
        String foundValue = this.getProperty(key);
        if (foundValue != null) {
            return foundValue;
        } else {
            return defaultValue;
        }
    }

    /**
     * Override a property.
     *
     * @param key           Property key
     * @param overrideValue Value to override with
     */
    public void overrideProperty(String key, String overrideValue) {
        checkIsAllowedProperty(key);

        // Display override message
//        if (!isPropertyDefined(key)) {
//            System.out.println("WARNING: property " + key + " which is being overridden is not previously defined in the configuration.");
//        }
//        System.out.println("Overriding property " + key + ": \"" + getPropertyWithDefault(key, "NON-EXISTING") + "\" (old) -> \"" + overrideValue + "\" (new).");

        // Actually perform override
        this.setProperty(key, overrideValue);

    }

    /**
     * Get all the properties and their values as a string.
     *
     * @return Properties and their values as a string (new line separated)
     */
    public String getAllPropertiesToString() {
        StringBuilder res = new StringBuilder();
        for (Object keyObj : this.keySet()) {
            res.append(keyObj.toString());
            res.append(": \"");
            res.append(this.getProperty((String) keyObj));
            res.append("\"\n");
        }
        return res.toString();
    }

    public void saveToFile(String file) throws IOException {

        FileOutputStream out = new FileOutputStream(file);
        this.store(out, "");
        out.close();


    }

    public boolean hasSubConfiguration() {
        // TODO Auto-generated method stub
        return currSubConfig != null;
    }

    public void nextSubConfiguration() {
        currSubConfig = subConfigs.poll();

    }

    public boolean isExtendedTopology() {
        // TODO Auto-generated method stub
        return getPropertyWithDefault(
                Constants.ScenarioTopology.EXTEND_WITH_SERVERS,
                "none").equals("regular");
    }

    public void markExtended() {
        Simulator.getConfiguration().setProperty(Constants.ScenarioTopology.EXTEND_WITH_SERVERS, "regular");

    }


    public Graph getGraphCopy() {
        return GraphReader.read(this.getPropertyOrFail(Constants.ScenarioTopology.FILE)).getLeft();
    }
}
