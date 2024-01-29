package ch.ethz.systems.netbench.xpt.xpander;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.RemoteSourceRoutingSwitch;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Path;

import java.util.*;

public class SemiXpanderServerOptics extends SemiXpander {
    protected HashMap<Integer,Set<Integer>> mServerTransmitColorsUsed; // which colors are going into some destination
    protected HashMap<Integer,Set<Integer>> mServerReceiveColorsUsed; // which colors are going out of some source
    int mCurrentServerSource; // helper field - should probably not be global
    int mCurrentServerDest; // --- """" ----
    public SemiXpanderServerOptics(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
        mServerTransmitColorsUsed = new HashMap<>();
        mServerReceiveColorsUsed = new HashMap<>();
        mCurrentServerSource = -1;
        mCurrentServerDest = -1;
    }


    /**
     * specific implementation for server optics setup
     * @param transimttingSource
     * @param receivingDest
     * @param sourceKey
     * @param destKey
     * @param jumboFlowId
     * @param sizeBit
     */
    @Override
    public void initRoute(int transimttingSource, int receivingDest, int sourceKey, int destKey, long jumboFlowId, long sizeBit){
        mCurrentServerDest = receivingDest;
        mCurrentServerSource = transimttingSource;
        Set transimtting = mServerTransmitColorsUsed.get(mCurrentServerSource);
        if(transimtting==null){
            mServerTransmitColorsUsed.put(mCurrentServerSource,new HashSet<>());
        }

        Set receving = mServerReceiveColorsUsed.get(mCurrentServerDest);
        if(receving==null){
            mServerReceiveColorsUsed.put(mCurrentServerDest,new HashSet<>());
        }

        super.initRoute(transimttingSource,receivingDest,sourceKey,destKey,jumboFlowId, sizeBit);


    }

    @Override
    protected boolean trivialPathAllowed() {
        return true;
    }

    @Override
    protected Path generatePathFromGraph(int sourceServer, int destServer) {
        int sourceToR = getToROfServer(sourceServer);
        int destToR = getToROfServer(destServer);
        return super.generatePathFromGraph(sourceToR,destToR);
    }

    protected int getToROfServer(int server) {
        return Simulator.getConfiguration().getGraphDetails().getTorIdOfServer(server);
    }


    @Override
    protected void returnPathToGraph(Path p, int sourceKey, int destKey, int transimttingSource, int receivingDest, long jumboFlowId) {

        super.returnPathToGraph(p,sourceKey,destKey,transimttingSource,receivingDest,jumboFlowId);
        RemoteSourceRoutingSwitch rsrs = (RemoteSourceRoutingSwitch) mIdToNetworkDevice.get(p.getLastVertex().getId());
        rsrs.removeFromForwardingTable(jumboFlowId);
        mServerTransmitColorsUsed.get(transimttingSource).remove(p.getColor());
        mServerReceiveColorsUsed.get(receivingDest).remove(p.getColor());
    }


    @Override
    protected Path allocateTrivialPath(int sourceToR, int destToR ){
        assert(sourceToR==destToR);
        for(int i = 0;i<mGraphs.length; i++){
            if(mServerTransmitColorsUsed.get(mCurrentServerSource).contains(i)){
                continue;
            }
            if(mServerReceiveColorsUsed.get(mCurrentServerDest).contains(i)){
                continue;
            }
            Path p = new Path(2,i);
            p.add(sourceToR);
            return p;
        }
        throw new NoPathException();
    }

    protected Path checkPathInGraph(List<Integer> p, int graphIndex) {

        if(mServerTransmitColorsUsed.get(mCurrentServerSource).contains(graphIndex)){

            throw new NoPathException();
        }
        if(mServerReceiveColorsUsed.get(mCurrentServerDest).contains(graphIndex)){
            throw new NoPathException();
        }
        return super.checkPathInGraph(p,graphIndex);
    }

    @Override
    protected void removePathFromGraph(Path p) {
        mServerReceiveColorsUsed.get(mCurrentServerDest).add(p.getColor());
        mServerTransmitColorsUsed.get(mCurrentServerSource).add(p.getColor());
        super.removePathFromGraph(p);
    }

    /**
     * configures the switches forwarding table according to some path
     * @param source
     * @param dest
     * @param p
     * @param jumboFlowId
     */
    @Override
    protected void updateForwardingTables(int source, int dest, Path p, long jumboFlowId) {
        super.updateForwardingTables(source,dest,p,jumboFlowId);
        RemoteSourceRoutingSwitch rsrs = (RemoteSourceRoutingSwitch) mIdToNetworkDevice.get(p.getLastVertex().getId());
        rsrs.updateForwardingTable(jumboFlowId,dest);
    }
}
