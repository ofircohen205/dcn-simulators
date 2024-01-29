package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.ecmp.EcmpRoutingUtility;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MNController extends RoutingPopulator {
    private final int serverDegree;
    protected Map<Integer, NetworkDevice> idToNetworkDevice;
    protected Map<Pair<Integer, Integer>, Long> loadMap;
    protected Map<Integer, Long> serversOutgoingLoadMap;
    protected Map<Integer, Long> serversIncommingLoadMap;

    static MNController sInstance = null;
    private int metaNodeNum;
    private long linkSpeedBpns;
    private int metaNodeSize;
    private int ToRNum;
    protected long initialTokenSizeBytes;
    protected long tokenTimeout;
    private int matchingsNum;
    private String calcTransferTimeBy;
    private Random rand;
    private int serverPerMetaNode;
    private final long maxServerLoad;
    private long serverTokenSizeByte;
    public final long linkSpeedbpns;

    protected MNController(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        super(configuration);
        this.idToNetworkDevice = idToNetworkDevice;
        loadMap = new HashMap<>();
        serversIncommingLoadMap = new HashMap<>();
        serversOutgoingLoadMap = new HashMap<>();
        int ToRnum = configuration.getGraphDetails().getNumTors();
        metaNodeNum = configuration.getGraphDetails().getMetaNodeNum();
        if (metaNodeNum == -1) {
            throw new IllegalStateException("MetaNode num must be set in graph details");
        }
        if (ToRnum % metaNodeNum != 0) {
            throw new IllegalStateException("MetaNode num must perfectly divide network switch num");
        }
        linkSpeedbpns = configuration.getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);

        calcTransferTimeBy = configuration.getPropertyWithDefault(
                Constants.MetaNode.CALC_TRANSFER_TIME_BY,
                "max");
        this.ToRNum = ToRnum;
        metaNodeSize = ToRnum / metaNodeNum;
        serverPerMetaNode = configuration.getGraphDetails().getNumServers() / metaNodeNum;
        linkSpeedBpns = configuration.getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);
        initialTokenSizeBytes = configuration.getLongPropertyWithDefault(
                Constants.MetaNode.DEFAULT_TOKEN_SIZE_BYTES,
                15000);
        serverTokenSizeByte = configuration.getLongPropertyWithDefault(
                Constants.MetaNode.DEFAULT_SERVER_TOKEN_SIZE_BYTES,
                initialTokenSizeBytes);
        tokenTimeout = configuration.getLongPropertyWithDefault(
                Constants.MetaNode.TOKEN_TIMEOUT_NS,
                30000);
        rand = Simulator.selectIndependentRandom("mn_switch_randomizer");
        initMetaNodes();
        initDevices();
        matchingsNum = calcMatchingNum();
        serverDegree = serverPerMetaNode / metaNodeSize;
        long outputPortMaxBytes = configuration.getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES);
        maxServerLoad = configuration.getLongPropertyWithDefault(Constants.MetaNode.SERVER_MAX_LOAD_BYTES, serverDegree * outputPortMaxBytes);
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
        SimulationLogger.logInfo("META_NODE_MATCHING_NUM", Integer.toString(matchingsNum));
        return ToRVertices / (metaNodeNum - 1);
    }

    private void initDevices() {

        for (int i = 0; i < configuration.getGraphDetails().getNumTors(); i++) {
            MetaNodeSwitch mnsw = (MetaNodeSwitch) idToNetworkDevice.get(i);
            int MN = mnsw.getIdentifier() / metaNodeSize;
            mnsw.setMetaNodeId(MN);
            mnsw.setRandomizer(this.rand);
            for (int serverId : configuration.getGraphDetails().getServersOfTor(i)) {

                MetaNodeSwitch server = (MetaNodeSwitch) idToNetworkDevice.get(serverId);
                if (server.getMNId() == -1) {
                    server.setMetaNodeId(MN);

                }
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

    public static MNController getInstance(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        if (sInstance == null) {
            sInstance = new MNController(configuration, idToNetworkDevice);
            return sInstance;
        }
        throw new IllegalStateException("Controller already initialized");
    }

    public static MNController getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Controller not initialized");
        }
        return sInstance;
    }

    private long calcTransferTimeNS(int MNSource, int MNDest, long bytes) {
        assert MNSource != MNDest;
        Pair p = new ImmutablePair(MNSource, MNDest);
        long directBuffer = loadMap.get(p);
        long directTransferTime = calcTransferTimeNS(bytes + directBuffer);
        long transferTime = directTransferTime;
        return transferTime;
    }

    private long calcTransferTimeNS(int MNSource, int middleHop, int MNDest, long bytes) {
        switch (calcTransferTimeBy) {
            case "addition":
                return calcTransferTimeNS(MNSource, middleHop, bytes) + calcTransferTimeNS(middleHop, MNDest, bytes);
            case "max":
                return Math.max(calcTransferTimeNS(MNSource, middleHop, bytes), calcTransferTimeNS(middleHop, MNDest, bytes));
        }
        throw new PropertyValueInvalidException(this.configuration, "meta_node_calc_trasfer_time_by");

    }

    protected int getNextMNDest(int MNSource, int MNDest, long bytes) {

        long minTransferTime = calcTransferTimeNS(MNSource, MNDest, bytes);
        int dest = MNDest;
        for (int i = 0; i < metaNodeNum; i++) {
            if (i == MNDest || i == MNSource) continue;
            long indirectTransferTime = calcTransferTimeNS(MNSource, i, MNDest, bytes);
            if (indirectTransferTime < minTransferTime) {
                minTransferTime = indirectTransferTime;
                dest = i;
            }
        }
        return dest;
    }

    public MetaNodeToken getToken(int MNSource, int MNDest, long bytes) {
        long newBytes = bytes;
        int dest = getNextMNDest(MNSource, MNDest, newBytes);
        return getToken(MNSource, MNDest, dest, newBytes);

    }

    private MetaNodeToken getToken(int MNSource, int MNDest, int middleHop, long bytes) {
        Pair<Integer, Integer> firstHop = new ImmutablePair<>(MNSource, middleHop);
        updateLoadMap(firstHop, bytes);
        if (middleHop != MNDest) {
            Pair<Integer, Integer> secondHop = new ImmutablePair<>(middleHop, MNDest);
            updateLoadMap(secondHop, bytes);
        }

        return getMetaNodeToken(bytes, MNSource, middleHop, MNDest);
    }

    protected void updateLoadMap(Pair pair, long bytes) {

        loadMap.put(pair, loadMap.get(pair) + bytes);
    }

    protected MetaNodeToken getMetaNodeToken(long bytes, int MNSource, int middleHop, int MNDest) {
        SimulationLogger.increaseStatisticCounter("MetaNodeToken " + MNSource + "->" + MNDest + (middleHop == MNDest ? " DIRECT" : (" via " + middleHop)));
        return new MetaNodeToken(bytes, MNSource, middleHop, MNDest, tokenTimeout);
    }

    public int getMetaNodeNum() {
        return metaNodeNum;
    }

    private long calcTransferTimeNS(long bytes) {

        return ((bytes * 8) / linkSpeedBpns) / matchingsNum;
    }


    public int getMetaNodeId(int identifier) {
        MetaNodeSwitch switch1 = (MetaNodeSwitch) idToNetworkDevice.get(identifier);

        return switch1.getMNId();
    }

    public MetaNodeToken getToken(int MNSource, int MNDest) {
        return getToken(MNSource, MNDest, initialTokenSizeBytes);
    }

    public int getMatchingsNum() {
        return this.matchingsNum;
    }

    public int getServerMetaNodeNum(int identifier) {
        assert identifier >= ToRNum;

        int serverPlace = identifier - ToRNum;
        return serverPlace / serverPerMetaNode;
    }

    public int getServerPerMetaNode() {
        return serverPerMetaNode;
    }

    public void releaseToken(MetaNodeToken metaNodeToken) {
        releaseLoad(metaNodeToken.getSource(), metaNodeToken.getMiddleHop(), metaNodeToken.getOriginalBytesAllocated());
        if (metaNodeToken.getMiddleHop() != metaNodeToken.getDest()) {
            releaseLoad(metaNodeToken.getMiddleHop(), metaNodeToken.getDest(), metaNodeToken.getOriginalBytesAllocated());
        }
    }

    private void releaseLoad(int source, int dest, long originalBytesAllocated) {
        Pair<Integer, Integer> pair = new ImmutablePair<>(source, dest);
        long currBytes = loadMap.get(pair);
        // System.out.println(currBytes + " " + originalBytesAllocated);
        assert currBytes >= originalBytesAllocated;
        loadMap.put(pair, currBytes - originalBytesAllocated);
    }


    public ServerToken getServerToken(int sourceId, int destinationId, long flowId, long bytesRequest) throws ServerOverloadedException {
        long incommingLoad = serversIncommingLoadMap.getOrDefault(destinationId, 0l);
        long outgoingLoad = serversOutgoingLoadMap.getOrDefault(sourceId, 0l);
        long bytesToAlloacte = Math.min(serverTokenSizeByte, bytesRequest);

        long maxLoad = maxServerLoad - bytesToAlloacte;

        if (incommingLoad >= maxLoad || outgoingLoad >= maxLoad) {
            SimulationLogger.increaseStatisticCounter("SERVER_OVERLOADED");
            throw new ServerOverloadedException();
        }

        int tor = configuration.getGraphDetails().getTorIdOfServer(sourceId);
        long mnLoad = 0;
        for (int serverId : configuration.getGraphDetails().getServersOfTor(tor)) {
            MetaNodeSwitch metaNodeSwitch = (MetaNodeSwitch) idToNetworkDevice.get(serverId);
            mnLoad += metaNodeSwitch.getLoadByte();
        }


        int sourceMN = getMetaNodeId(sourceId);
        int destMN = getMetaNodeId(destinationId);
        MetaNodeToken token = null;

        if (sourceMN != destMN) {
            token = getToken(sourceMN, destMN, bytesToAlloacte);
        }


        serversOutgoingLoadMap.put(sourceId, outgoingLoad + bytesToAlloacte);


        final ServerToken serverToken = new ServerToken(flowId, bytesToAlloacte, sourceId, destinationId, getServerTokenExpiryTime(outgoingLoad + bytesToAlloacte));
        serverToken.setMetaNodeToken(token);
        serversIncommingLoadMap.put(destinationId, incommingLoad + bytesToAlloacte);


        return serverToken;
    }


    public void releaseServerTokenIncomming(ServerToken serverToken) {
        long currentIncomming = serversIncommingLoadMap.get(serverToken.destinationId);
        serversIncommingLoadMap.put(serverToken.destinationId, currentIncomming - serverToken.bytes);

    }

    public void releaseServerTokenOutgoing(ServerToken serverToken) {
        if (serverToken.isExpired()) throw new IllegalStateException("cant release expired token");
        serverToken.invalidate();

        long currentOutgoing = serversOutgoingLoadMap.get(serverToken.sourceId);
        serversOutgoingLoadMap.put(serverToken.sourceId, currentOutgoing - serverToken.bytes);

    }


    public long getServerTokenExpiryTime(long tokenBytes) {
        return (8 * tokenBytes) / (linkSpeedbpns * serverDegree);
    }

    public long getMNTokenExpiryTime(long tokenBytes) {
        return (8 * tokenBytes) / (linkSpeedbpns * serverDegree);
    }

    public void createReceiverSocket(long flowId, int identifier, int destinationId, long flowSizeByte) {

        MetaNodeSwitch serverDest = (MetaNodeSwitch) idToNetworkDevice.get(destinationId);
        ((MetaNodeTransport) serverDest.getTransportLayer()).createReceiverSocket(flowId, identifier, destinationId, flowSizeByte);
    }
}
