package ch.ethz.systems.netbench.xpt.meta_node;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.poissontraffic.RandomCollection;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FlowSizeDistribution;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MockMNTrafficPlanner extends MetaNodePermutationTrafficPlanner {
    public double serverProb;
    public List<Pair<Integer, Integer>> pairs;
    public MockMNTrafficPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, double lambdaFlowStartsPerSecond, FlowSizeDistribution flowSizeDistribution, NBProperties configuration) {
        super(idToTransportLayerMap, lambdaFlowStartsPerSecond, flowSizeDistribution, configuration);
    }

    public RandomCollection<Pair<Integer, Integer>> getPool(){
        return randomPairGenerator;
    }

    protected int getNumServersPerMN(){
        return 10;
    }

    protected int getNumMetaNode() {
        return 5;
    }

    @Override
    protected void addToPool(double serverProb, ImmutablePair<Integer, Integer> pair){
        if(pairs == null) pairs = new LinkedList<>();
        this.serverProb = serverProb;
        this.pairs.add(pair);

        super.addToPool(serverProb,pair);
    }
}
