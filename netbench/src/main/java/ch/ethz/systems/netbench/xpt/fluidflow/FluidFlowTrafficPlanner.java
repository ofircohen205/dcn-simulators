package ch.ethz.systems.netbench.xpt.fluidflow;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.poissontraffic.PoissonArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FlowSizeDistribution;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FluidFlowTrafficPlanner extends PoissonArrivalPlanner {
    Set<ImmutablePair<Integer, Integer>> pairs;
    int numOfFlowsForPair;

    public FluidFlowTrafficPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, double lambdaFlowStartsPerSecond, FlowSizeDistribution flowSizeDistribution, String fileName, NBProperties configuration) {
        super(idToTransportLayerMap, lambdaFlowStartsPerSecond, flowSizeDistribution, fileName, configuration);
    }

    public FluidFlowTrafficPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, double lambdaFlowStartsPerSecond, FlowSizeDistribution flowSizeDistribution, PairDistribution pairDistribution, NBProperties configuration) {
        super(idToTransportLayerMap, lambdaFlowStartsPerSecond, flowSizeDistribution, pairDistribution, configuration);
        numOfFlowsForPair = configuration.getIntegerPropertyWithDefault(
                Constants.FluidFlow.NUM_FLOWS_FOR_PAIR,
                1);

    }


    @Override
    public void createPlan(long durationNs) {
        for (int i = 0; i < numOfFlowsForPair; i++) {
            for (ImmutablePair<Integer, Integer> pair : pairs) {
                registerFlow(0, pair.getLeft(), pair.getRight(), flowSizeDistribution.generateFlowSizeByte(), -1);
            }
        }
        System.out.println("Fluid flow plan created.");

    }

    @Override
    protected void addToPool(double serverProb, ImmutablePair<Integer, Integer> pair) {
        try {
            //ignore the probability
            pairs.add(pair);
        } catch (NullPointerException e) {
            pairs = new HashSet<>();
            pairs.add(pair);
        }


    }
}
