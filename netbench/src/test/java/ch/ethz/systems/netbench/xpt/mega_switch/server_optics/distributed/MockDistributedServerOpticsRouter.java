package ch.ethz.systems.netbench.xpt.mega_switch.server_optics.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedController;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MockDistributedServerOpticsRouter extends DistributedController{
    HashSet<Long> routedFlows = new HashSet<>();
    HashMap<Integer,HashMap<Integer,Integer>> reservedColors;
    HashMap<Integer,HashMap<Integer,Integer>> dealloctedColors;
    HashMap<Pair<Integer,Integer>,HashMap<Integer,Integer>> allocatedEdges;
    HashMap<Pair<Integer,Integer>,HashMap<Integer,Integer>> deallocatedEdges;
    public MockDistributedServerOpticsRouter(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
        reservedColors = new HashMap<>();
        dealloctedColors = new HashMap<>();
        allocatedEdges = new HashMap<>();
        deallocatedEdges = new HashMap<>();
    }

    public boolean routedFlow(long i) {
        return routedFlows.contains(i);
    }

    public void markFlowRouted(long flowId) {
        routedFlows.add(flowId);
    }

//    @Override
//    public void deallocateServerColor(int server, int color, boolean incomming) {
//        super.deallocateServerColor(server,color,incomming);
//        HashMap<Integer,Integer> colors = dealloctedColors.getOrDefault(server,new HashMap<>());
//        int times = colors.getOrDefault(color,0);
//        times++;
//        colors.put(color,times);
//        dealloctedColors.put(server,colors);
//        assert(serverColorAvailable(server,color,incomming));
//    }
//    @Override
//    public void reserveServerColor(int server, int color, boolean incomming) {
//        super.reserveServerColor(server,color,incomming);
//        HashMap<Integer,Integer> colors = reservedColors.getOrDefault(server,new HashMap<>());
//        int times = colors.getOrDefault(color,0);
//        times++;
//        colors.put(color,times);
//        reservedColors.put(server,colors);
//        assert(!serverColorAvailable(server,color,incomming));
//
//    }

//    public int allocatedColor(int server, int c) {
//        return reservedColors.getOrDefault(server,new HashMap<>()).getOrDefault(c,0);
//    }
//
//    public int deallocatedColor(int server, int c) {
//        return dealloctedColors.getOrDefault(server,new HashMap<>()).getOrDefault(c,0);
//    }

    @Override
    public void decreaseEdgeCapacity(int source, int dest, int color){
        super.decreaseEdgeCapacity(source,dest,color);
        HashMap<Integer,Integer> colors = allocatedEdges.getOrDefault(new ImmutablePair<>(source,dest), new HashMap<>());
        int times = colors.getOrDefault(color,0);
        times++;
        colors.put(color,times);
        allocatedEdges.put(new ImmutablePair<>(source,dest),colors);
    }

    @Override
    public void increaseEdgeCapacity(int source, int dest, int color){
        super.increaseEdgeCapacity(source,dest,color);
        HashMap<Integer,Integer> colors = deallocatedEdges.getOrDefault(new ImmutablePair<>(source,dest), new HashMap<>());
        int times = colors.getOrDefault(color,0);
        times++;
        colors.put(color,times);
        deallocatedEdges.put(new ImmutablePair<>(source,dest),colors);
    }

    public int allocatedEdge(int source, int dest, int color) {
        return allocatedEdges.getOrDefault(new ImmutablePair<>(source,dest),new HashMap<>()).getOrDefault(color,0);
    }

    public int deallocatedEdge(int source, int dest, int color) {
        return deallocatedEdges.getOrDefault(new ImmutablePair<>(source,dest),new HashMap<>()).getOrDefault(color,0);
    }
}
