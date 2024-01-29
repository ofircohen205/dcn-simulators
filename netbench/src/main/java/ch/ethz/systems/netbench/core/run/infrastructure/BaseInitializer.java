package ch.ethz.systems.netbench.core.run.infrastructure;

import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes the entire infrastructure in the most
 * generic way possible using the given generators.
 * <p>
 * Any differentiation between applications, transport layers,
 * output ports, links or nodes are decided by
 * their respective generators.
 */
public class BaseInitializer {

    private static BaseInitializer sInstance;
    // Mappings
    private Map<Integer, NetworkDevice> idToNetworkDevice;
    private Map<Integer, TransportLayer> idToTransportLayer;
    private NetworkDevice[] idtoNetworkDeviceArray;
    // Generators
    private OutputPortGenerator outputPortGenerator;
    private NetworkDeviceGenerator networkDeviceGenerator;
    private LinkGenerator linkGenerator;
    private TransportLayerGenerator transportLayerGenerator;

    // Validation variables
    private int runningNodeId;
    private List<Pair<Integer, Integer>> linkPairs;

    private BaseInitializer(
            OutputPortGenerator outputPortGenerator,
            NetworkDeviceGenerator networkDeviceGenerator,
            LinkGenerator linkGenerator,
            TransportLayerGenerator transportLayerGenerator
    ) {
        this.idToNetworkDevice = new HashMap<>();
        this.idToTransportLayer = new HashMap<>();
        this.outputPortGenerator = outputPortGenerator;
        this.networkDeviceGenerator = networkDeviceGenerator;
        this.linkGenerator = linkGenerator;
        this.transportLayerGenerator = transportLayerGenerator;
        this.runningNodeId = 0;
    }

    private BaseInitializer() {
        clear();
    }

    public static BaseInitializer init() {
        return new BaseInitializer();
    }

    public static BaseInitializer getInstance() {
        if (sInstance == null) {
            sInstance = init();
        }
        return sInstance;
    }

    private void clear() {
        this.idToNetworkDevice = new HashMap<>();
        this.idToTransportLayer = new HashMap<>();
        outputPortGenerator = null;
        networkDeviceGenerator = null;
        linkGenerator = null;
        transportLayerGenerator = null;
        this.linkPairs = new ArrayList<>();
        this.runningNodeId = 0;
    }

    public void extend(OutputPortGenerator outputPortGenerator,
                       NetworkDeviceGenerator networkDeviceGenerator,
                       LinkGenerator linkGenerator,
                       TransportLayerGenerator transportLayerGenerator) {
        extend(0, outputPortGenerator, networkDeviceGenerator, linkGenerator, transportLayerGenerator);
    }

    public void extend(int networkNum, OutputPortGenerator outputPortGenerator,
                       NetworkDeviceGenerator networkDeviceGenerator,
                       LinkGenerator linkGenerator,
                       TransportLayerGenerator transportLayerGenerator) {
        if (networkNum == 0) {
            reset();
        }
        this.outputPortGenerator = outputPortGenerator;
        this.networkDeviceGenerator = networkDeviceGenerator;
        this.linkGenerator = linkGenerator;
        this.transportLayerGenerator = transportLayerGenerator;
        this.linkPairs = new ArrayList<>();

    }

    public void finalization() {
        idtoNetworkDeviceArray = new NetworkDevice[idToNetworkDevice.size()];
        for (int id : idToNetworkDevice.keySet()) {
            idtoNetworkDeviceArray[id] = idToNetworkDevice.get(id);
        }
    }

    /**
     * Read the base network (servers, network devices, links) from the topology file.
     *
     * @return
     */
    public HashMap<Integer, NetworkDevice> createInfrastructure(NBProperties configuration) {

        boolean verifyLinks = configuration.getBooleanPropertyWithDefault(Constants.InfrastructureCreation.VERIFY_LINKS_ON_CREATION, false);
        // Fetch from configuration graph and its details
        Graph graph = configuration.getGraph();
        setVertexTieBreaker(configuration);
        GraphDetails details = configuration.getGraphDetails();
        System.out.println("finished reading graph");
        HashMap<Integer, NetworkDevice> partialMap = new HashMap<Integer, NetworkDevice>();
        // Create nodes
        for (int i = 0; i < details.getNumNodes(); i++) {
            NetworkDevice nd = createNode(i, details.getServerNodeIds().contains(i), configuration);
            partialMap.put(i, nd);
        }
        System.out.println("finished creating nodes");

        // Create edges
        for (Vertex v : graph.getVertexList()) {
            for (Vertex w : graph.getAdjacentVertices(v)) {
                createEdge(v.getId(), +w.getId(), partialMap, verifyLinks);
            }
        }
        System.out.println("finished creating nodes and edges");
        if (verifyLinks) {
            for (int i = 0; i < linkPairs.size(); i++) {
                // Attempt to find the reverse
                boolean found = false;
                for (int j = 0; j < linkPairs.size(); j++) {
                    ImmutablePair<Integer, Integer> tempPair = new ImmutablePair<>(
                            linkPairs.get(i).getRight(),
                            linkPairs.get(i).getLeft()
                    );
                    if (i != j && linkPairs.get(j).equals(tempPair)) {
                        found = true;
                        break;
                    }
                }

                // If reverse not found, it is not bidirectional
                if (!found) {
                    throw new IllegalArgumentException(
                            "Link was added which is not bi-directional: " +
                                    linkPairs.get(i).getLeft() + " -> " + linkPairs.get(i).getRight()
                    );
                }
            }
        }
        // Check the links for bi-directionality
        return partialMap;
    }

    /**
     * Tie breaker is a routing mechanism that decides how to behave when
     * vertices have the same weight
     */
    private void setVertexTieBreaker(NBProperties configuration) {
        String tieBreakRule = configuration.getProperty(Constants.VertexTieBreakRule.VERTEX_TIE_BREAK_RULE);
        if (tieBreakRule != null) {
            throw new RuntimeException("unsupported function setVertexTieBreaker");
        }
    }

    /**
     * Create the implementation of a node in the network.
     *
     * @param id            Node identifier
     * @param isServer      True iff it is a server (a.k.a. will have a transport layer)
     * @param configuration
     * @return
     */
    private NetworkDevice createNode(int id, boolean isServer, NBProperties configuration) {
        runningNodeId++;
        // Create corresponding network device
        NetworkDevice networkDevice;
        if (isServer) {
            // Create server
            TransportLayer transportLayer = transportLayerGenerator.generate(id);
            // Create network device
            networkDevice = networkDeviceGenerator.generate(id, transportLayer);
            idToTransportLayer.put(id, transportLayer);
            // Link transport layer to network device
            if (networkDevice.isServer()) {
                transportLayer.setNetworkDevice(networkDevice);
            }
        } else {
            networkDevice = networkDeviceGenerator.generate(id);
        }

        // Add to mappings
        NetworkDevice existing = idToNetworkDevice.get(id);
        if (existing != null) {
            if (existing.isServer()) return networkDevice;
            try {
                MegaSwitch megaSwitch = (MegaSwitch) existing;
                megaSwitch.extend(networkDevice, configuration);
            } catch (ClassCastException e) {
                throw new RuntimeException("illegal to use extend with this device " + existing.getIdentifier());
            }
        } else {
            idToNetworkDevice.put(id, networkDevice);
        }
        return networkDevice;
    }

    /**
     * Create the implementation of a directed edge in the network.
     *
     * @param startVertexId Origin vertex identifier
     * @param endVertexId   Destination vertex identifier
     * @param partialMap
     * @param verifyLinks
     */
    private void createEdge(int startVertexId, int endVertexId, HashMap<Integer, NetworkDevice> partialMap, boolean verifyLinks) {
        // Select network devices
        NetworkDevice devA = partialMap.get(startVertexId);
        NetworkDevice devB = partialMap.get(endVertexId);
        Link link = linkGenerator.generate(devA, devB);
        // Add connection
        OutputPort portAtoB = outputPortGenerator.generate(
                devA,
                devB,
                link
        );

        devA.addConnection(portAtoB);
        InputPort portBtoA = new InputPort(devB, devA, link);
        devB.addIncomingConnection(portBtoA);
        if (verifyLinks) {
            // Duplicate link
            if (linkPairs.contains(new ImmutablePair<>(startVertexId, endVertexId))) {
                throw new IllegalArgumentException(
                        "Duplicate link (" + startVertexId + " -> + " + endVertexId +
                                ") defined - Please check input topology file."
                );
            }
        }

        linkPairs.add(new ImmutablePair<>(startVertexId, endVertexId));
    }

    /**
     * Retrieve identifier to network device mapping.
     *
     * @return Mapping of node identifier to its network device
     */
    public Map<Integer, NetworkDevice> getIdToNetworkDevice() {
        return idToNetworkDevice;
    }

    public NetworkDevice getNetworkDeviceById(int id) {

        return idtoNetworkDeviceArray[id];
    }

    /**
     * Retrieve identifier to transport layer mapping.
     *
     * @return Mapping of node identifier to its transport layer
     */
    public Map<Integer, TransportLayer> getIdToTransportLayer() {
        return idToTransportLayer;
    }

    public void reset() {
        clear();
    }

}
