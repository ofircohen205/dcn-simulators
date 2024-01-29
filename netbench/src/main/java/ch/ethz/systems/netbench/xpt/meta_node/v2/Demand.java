package ch.ethz.systems.netbench.xpt.meta_node.v2;

import java.util.LinkedList;
import java.util.List;

public class Demand {
    private int sourceServer;
    private int destServer;
    private long bits;
    private long flowId;
    private boolean routeAllocated;
    public final int MNSource;
    public final int MNDest;
    private CongestionAlg.TransportRule trasportRule;
    public final long priority;
    public Demand(int sourceServer, int destServer, long bits, long flowId, int MNSource, int MNDest, long priority) {
        this.sourceServer = sourceServer;
        this.destServer = destServer;
        this.bits = bits;
        this.flowId = flowId;
        this.MNSource = MNSource;
        this.MNDest = MNDest;
        this.routeAllocated = false;
        this.trasportRule = null;
        this.priority = priority;
    }

    public int getSourceServer(){
        return sourceServer;
    }
    
    public int getDestServer(){
        return destServer;
    }
    
    public long getBits(){
        return bits;
    }

    @Override
    public boolean equals(Object other){
        if(other==null || !(other instanceof Demand))
            return false;
        Demand demand = (Demand) other;
        return demand.sourceServer == sourceServer && demand.destServer == destServer;
    }

    public long getFlowId(){
        return flowId;
    }

    public void onRouteAllocated() {
//        routeAllocated = true;
        trasportRule.allow();
    }

    public boolean hasTransportRule(){
        return trasportRule != null && trasportRule.bytesToSend > 0;
    }

    public boolean isRouteAllocated(){
        return trasportRule != null && trasportRule.isAllowed();
    }

    public void addTransportRule(CongestionAlg.TransportRule transportRule) {
        this.trasportRule = transportRule;
    }

    public CongestionAlg.TransportRule getTrasportRule() {
        return trasportRule;
    }

    public static class AggregatedDemand {

        public final int MNSource;
        public final int MNDest;
        private List<Demand> demands;


        public AggregatedDemand(int MNSource, int MNDest){
            this.MNDest = MNDest;
            this.MNSource = MNSource;
            demands = new LinkedList<>();
        }



        public void addDemand(Demand demand) {
            demands.add(demand);;
        }

        public void onRouteAllocated() {
            for (Demand demand:
                    demands) {
                demand.onRouteAllocated();
            }
        }

        public boolean isRouteAllocated() {
            return demands.get(0).isRouteAllocated();
        }
    }


    @Override
    public String toString(){
        return "Demand " + bits + " flowId " + flowId  + " MNSource " + MNSource + " MNDest " + MNDest + " priority "+ priority + "\n";
    }
}
