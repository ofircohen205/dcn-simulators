package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Event;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class EpochEvent  extends Event {

    private long recurrenceTime;

    private int MNNum;
    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs Time it will take before happening from now in nanoseconds
     */
    public EpochEvent(long timeFromNowNs, int MNNum, long recurrenceTime) {
        super(timeFromNowNs);
        this.recurrenceTime = recurrenceTime;
        this.MNNum = MNNum;
    }

    @Override
    public void trigger() {
        List<Demand> demandList = MNEpochController.getInstance().getDemandList();
        Collections.sort(demandList, (a,b) ->  new Long(a.priority - b.priority).intValue());
        long bitsCapacity = MNEpochController.getInstance().getEpochBits();


        CongestionAlg.initTransportRules(demandList, bitsCapacity);

        HashMap<Pair<Integer, Integer>, Demand.AggregatedDemand> aggregatedDemands = new HashMap<>();

        for(Demand demand: demandList){
            if(!demand.hasTransportRule()) continue;
            Demand.AggregatedDemand aggregatedDemand =
                    aggregatedDemands.getOrDefault(new ImmutablePair<>(demand.MNSource,demand.MNDest),
                            new Demand.AggregatedDemand(demand.MNSource, demand.MNDest));
            aggregatedDemand.addDemand(demand);
            aggregatedDemands.put(new ImmutablePair<>(demand.MNSource,demand.MNDest), aggregatedDemand );
        }

        List<RoutingAlg.RoutingRule> routingRules = RoutingAlg.getInstance().getRules(aggregatedDemands);

        System.out.println("Demand list before " + demandList);
        System.out.println("Demand list after " + demandList.stream().filter(demand -> demand.isRouteAllocated()).collect(Collectors.toList()));


        MNEpochController.getInstance().updateRules(routingRules);
        MNEpochController.getInstance().startEpoch(demandList.stream().filter(demand -> demand.isRouteAllocated())
                .map(demand -> demand.getTrasportRule()).collect(Collectors.toList()));
    }

    @Override
    public boolean retrigger(){
        time = Simulator.getTimeFromNow(recurrenceTime);
        return true;
    }
}
