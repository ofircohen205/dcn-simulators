package ch.ethz.systems.netbench.xpt.meta_node.v2;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AggregatedDemand {

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
