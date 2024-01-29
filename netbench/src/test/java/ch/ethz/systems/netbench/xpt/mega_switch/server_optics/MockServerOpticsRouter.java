package ch.ethz.systems.netbench.xpt.mega_switch.server_optics;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.xpander.SemiXpanderServerOptics;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MockServerOpticsRouter extends SemiXpanderServerOptics {
    HashMap<Pair<Integer,Integer>,Boolean> routed;
    HashMap<Pair<Integer,Integer>,Boolean> recovered;
    public MockServerOpticsRouter(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
        routed = new HashMap<>();
        recovered = new HashMap<>();
    }

    @Override
    public void recoverPath(int src, int dst,int serverSource,int destServer, long jumboFlowId){
        super.recoverPath(src, dst, serverSource, destServer, jumboFlowId);
        recovered.put(new ImmutablePair<Integer, Integer>(serverSource, destServer), true);
    }

    protected List<List<Integer>> getPathsFromDevice(int source, int dest) {
        List<List<Integer>> paths = new LinkedList<>();
        LinkedList<Integer> p = new LinkedList<>();
        p.add(source);
        p.add(dest);
        paths.add(p);
        return paths;
    }

    @Override
    protected Path generatePathFromGraph(int source, int dest) {

        return super.generatePathFromGraph(source, dest);
    }



    protected Path getPath(int src,int dst) {
        return  mPaths.get(new ImmutablePair<>(src,dst));
    }

    @Override
    protected void updateForwardingTables(int source, int dest, Path p, long flowId) {
        super.updateForwardingTables(source, dest, p, flowId);
        routed.put(new ImmutablePair<Integer, Integer>(source, dest), true);
    }

    public boolean routed(int source, int dest) {
        // TODO Auto-generated method stub
        return routed.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
    }

    public boolean recovered(int source, int dest) {
        // TODO Auto-generated method stub
        return recovered.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
    }

    @Override
    public void reset(){
        super.reset();
        recovered.clear();
        routed.clear();
    }
}
