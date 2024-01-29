package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.ecmp.EcmpRoutingUtility;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class MNEpochController extends RoutingPopulator {
    protected Map<Integer, NetworkDevice> idToNetworkDevice;
    protected Map<Pair<Integer, Integer>, Long> loadMap;
    static MNEpochController sInstance = null;
    private int metaNodeNum;
    public final long linkSpeedbpns;
    private int metaNodeSize;
    private int ToRNum;
    private ArrayList<Demand> demands;
    private int matchingsNum;
    private int bitsPerEpoch;
    private Random rand;
    private int serverDegree;
    private int serverPerMetaNode;
    public final long epochTime;
    List<RoutingAlg.RoutingRule> currentRules;
    HashMap<Pair<Integer, Integer>, AggregatedDemand> aggregatedDemandMap;

    protected MNEpochController(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        super(configuration);
        demands = new ArrayList<>();
        this.idToNetworkDevice = idToNetworkDevice;
        loadMap = new HashMap<>();
        aggregatedDemandMap = new HashMap<>();
        int ToRnum = configuration.getGraphDetails().getNumTors();
        metaNodeNum = configuration.getGraphDetails().getMetaNodeNum();
        if (metaNodeNum == -1) {
            throw new IllegalStateException("MetaNode num must be set in graph details");
        }
        if (ToRnum % metaNodeNum != 0) {
            throw new IllegalStateException("MetaNode num must perfectly divide network switch num");
        }

        this.ToRNum = ToRnum;
        metaNodeSize = ToRnum / metaNodeNum;
        serverPerMetaNode = configuration.getGraphDetails().getNumServers() / metaNodeNum;
        linkSpeedbpns = configuration.getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);
        rand = Simulator.selectIndependentRandom("mn_switch_randomizer");
        initMetaNodes();
        initDevices();
        matchingsNum = calcMatchingNum();
        serverDegree = serverPerMetaNode / metaNodeSize;
        SimulationLogger.logInfo("META_NODE_MATCHING_NUM", Integer.toString(matchingsNum));
        RoutingAlg.init(metaNodeNum);
        epochTime = configuration.getLongPropertyOrFail(Constants.MetaNode.EPOCH_TIME);
        EpochEvent epochEvent = new EpochEvent(1000, metaNodeNum, epochTime);
        Simulator.registerEvent(epochEvent);
    }

    /**
     * relies on perfect symmetry
     *
     * @return
     */
    private int calcMatchingNum() {
        List<Vertex> vertices = configuration.getGraph().getAdjacentVertices(new Vertex(0));
        int ToRVertices = 0;
        for (Vertex u : vertices) {
            if (!idToNetworkDevice.get(u.getId()).isServer()) {
                ToRVertices++;
            }
        }
        return ToRVertices / (metaNodeNum - 1);
    }

    private void initDevices() {

        for (int i = 0; i < configuration.getGraphDetails().getNumTors(); i++) {
            MetaNodeSwitch mnsw = (MetaNodeSwitch) idToNetworkDevice.get(i);
            int MN = mnsw.getIdentifier() / metaNodeSize;
            mnsw.setMetaNodeId(MN);
            mnsw.setRandomizer(this.rand);
            for (int serverId : configuration.getGraphDetails().getServersOfTor(i)) {
                MetaNodeServer server = (MetaNodeServer) idToNetworkDevice.get(serverId);
                server.setMetaNodeId(MN);
            }
        }
    }

    private void initMetaNodes() {
        for (int i = 0; i < metaNodeNum; i++) {
            for (int j = 0; j < metaNodeNum; j++) {
                if (i == j) continue;

                loadMap.put(new ImmutablePair<>(i, j), 0l);
            }
        }
    }

    @Override
    public void populateRoutingTables() {
        EcmpRoutingUtility.populateShortestPathRoutingTables(this.idToNetworkDevice, true, this.configuration);
    }

    public void updateRules(List<RoutingAlg.RoutingRule> rules) {
        if (currentRules != null) System.out.println(currentRules);
        currentRules = rules;
        for (NetworkDevice device : idToNetworkDevice.values()) {
            if (!device.isServer()) continue;
            MetaNodeServer metaNodeSwitch = (MetaNodeServer) device;
            metaNodeSwitch.invalidateRules();
            for (RoutingAlg.RoutingRule rule : rules) {
                if (metaNodeSwitch.getMNID() == rule.MNSource)
                    metaNodeSwitch.addRule(rule);
            }
        }
    }

    public List<RoutingAlg.RoutingRule> getCurrentRules() {
        return currentRules;
    }

    private HashSet<Integer> getMetaNodeTors(int mnSource) {
        HashSet<Integer> tors = new HashSet<Integer>();
        for (int i = mnSource; i < metaNodeSize * (mnSource + 1); i++) {
            tors.add(i);
        }
        return tors;
    }

    public static MNEpochController getInstance(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        if (sInstance == null) {
            sInstance = new MNEpochController(configuration, idToNetworkDevice);
            return sInstance;
        }
        throw new IllegalStateException("Controller already initialized");
    }

    public static MNEpochController getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Controller not initialized");
        }
        return sInstance;
    }

    public void registerDemand(int sourceServier, int destServer, long bits, long flowId, long startTime) {
        int MNSource = getMetaNodeId(sourceServier);
        int MNDest = getMetaNodeId(destServer);

        Demand demand = new Demand(sourceServier, destServer, bits, flowId, MNSource, MNDest, startTime);
        demands.add(demand);

    }

    public HashMap<Pair<Integer, Integer>, AggregatedDemand> getAggregatedDemandsList() {

        for (Demand demand : demands) {
            int sourceToR = getMetaNodeId(demand.getSourceServer());
            int destToR = getMetaNodeId(demand.getDestServer());
            Pair p = new ImmutablePair<>(sourceToR, destToR);
            AggregatedDemand aggregatedDemand = aggregatedDemandMap.get(p);
            if (aggregatedDemand != null) {
                aggregatedDemand = new AggregatedDemand(sourceToR, destToR);
                aggregatedDemandMap.put(p, aggregatedDemand);
            }

            aggregatedDemand.addDemand(demand);
        }
        return aggregatedDemandMap;
    }

    public int getMetaNodeId(int identifier) {

        MetaNodeSwitch mnsw = (MetaNodeSwitch) idToNetworkDevice.get(identifier);

        return mnsw.getMNID();
    }

    public void invalidateDemands() {
        this.demands.clear();
    }

    public List<Demand> getDemandList() {
        demands = new ArrayList<>();
        for (NetworkDevice device : idToNetworkDevice.values()) {
            if (device.isServer()) {
                MetaNodeTransport transport = (MetaNodeTransport) device.getTransportLayer();
                transport.registerDemands(getEpochBits());
            }
        }
        return demands;
    }

    public void startEpoch(List<CongestionAlg.TransportRule> transportRules) {

        for (NetworkDevice device : idToNetworkDevice.values()) {
            if (!device.isServer()) continue;
            MetaNodeServer metaNodeSwitch = (MetaNodeServer) device;
            metaNodeSwitch.startEpoch();
        }

        for (CongestionAlg.TransportRule rule : transportRules) {
            MetaNodeServer server = (MetaNodeServer) idToNetworkDevice.get(rule.source);
            server.startEpoch(rule.flowId, rule.bytesToSend);
        }
    }

    public long getEpochBits() {
//        return Math.min(epochTime*linkSpeedbpns, configuration.getLongPropertyOrFail("output_port_max_queue_size_bytes") * 8)*serverDegree;
        return epochTime * linkSpeedbpns * serverDegree;
    }

    public MetaNodeSwitch getDevice(int id) {
        return (MetaNodeSwitch) idToNetworkDevice.get(id);
    }
}
