package ch.ethz.systems.netbench.ext.simpletraffic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.poissontraffic.PoissonArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.RandomCollection;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FlowSizeDistribution;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleTrafficPlanner extends PoissonArrivalPlanner{
    RandomCollection<Integer> randomServerGenerator;

    public SimpleTrafficPlanner(Map<Integer, TransportLayer> idToTransportLayer, int traffic_lambda_flow_starts_per_s, FlowSizeDistribution flowSizeDistribution, PairDistribution allToAllServerFraction,NBProperties configuration) {
        super(idToTransportLayer,traffic_lambda_flow_starts_per_s,flowSizeDistribution,allToAllServerFraction,configuration);
    }

    @Override
    protected List<Integer> initRandomServerSet(List<Integer> servers, int numChosenServers){
        randomServerGenerator = new RandomCollection<Integer>(Simulator.selectIndependentRandom("simple traffic server generator"));
        List<Integer> chosen = new ArrayList<>();
        // Probability between each server pair
        double serverPairProb = 1.0 / (numChosenServers );

        for(int i = 0;i< numChosenServers ; i++){
            chosen.add(servers.get(i));
            randomServerGenerator.add(serverPairProb,servers.get(i));
        }
        return chosen;
    }

    @Override
    protected Pair<Integer, Integer> choosePair() {
        int source = randomServerGenerator.next();
        int dest = source;
        while(dest == source){
            dest = randomServerGenerator.next();
        }
        return new ImmutablePair<Integer, Integer>(source,dest);
    }
}
