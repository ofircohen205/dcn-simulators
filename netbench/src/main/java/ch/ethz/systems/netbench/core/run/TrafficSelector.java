package ch.ethz.systems.netbench.core.run;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.poissontraffic.FromFileArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.FromStringArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.PoissonArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.*;
import ch.ethz.systems.netbench.ext.simpletraffic.SimpleTrafficPlanner;
import ch.ethz.systems.netbench.ext.trafficpair.TrafficPairPlanner;
import ch.ethz.systems.netbench.xpt.fluidflow.FluidFlowTrafficPlanner;
import ch.ethz.systems.netbench.xpt.meta_node.MetaNodeA2ATrafficPlanner;
import ch.ethz.systems.netbench.xpt.meta_node.MetaNodePermutationTrafficPlanner;
import ch.ethz.systems.netbench.xpt.meta_node.v1.MockMetaNodePermutationTrafficPlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TrafficSelector {

    /**
     * Select the traffic planner which creates and registers the start
     * of flows during the run.
     * <p>
     * Selected using following property:
     * traffic=...
     *
     * @param idToTransportLayer Node identifier to transport layer mapping
     * @return Traffic planner
     */
    static TrafficPlanner selectPlanner(Map<Integer, TransportLayer> idToTransportLayer,
                                        NBProperties configuration) {
        switch (configuration.getPropertyOrFail(Constants.Traffic.TRAFFIC)) {
            case Constants.Traffic.POISSON_ARRIVAL:
                FlowSizeDistribution flowSizeDistribution = getPoissonArrivalFlowSizeDistribution(
                        configuration);
                System.out.println(flowSizeDistribution);
                // Attempt to retrieve pair probabilities file
                String pairProbabilitiesFile = configuration.getPropertyWithDefault(
                        Constants.PoissonArrival.TRAFFIC_PROBABILITIES_FILE, null);
                if (pairProbabilitiesFile != null) {
                    // Create poisson arrival plan from file
                    return new PoissonArrivalPlanner(
                            idToTransportLayer,
                            configuration.getIntegerPropertyOrFail(
                                    Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                            flowSizeDistribution,
                            configuration.getPropertyOrFail(
                                    Constants.PoissonArrival.TRAFFIC_PROBABILITIES_FILE),
                            configuration);
                } else {
                    // If we don't supply the pair probability file we fallback to all-to-all
                    return getGenerativePairProbabilities(idToTransportLayer, configuration,
                            flowSizeDistribution);
                }
            case Constants.Traffic.TRAFFIC_PAIR:
                return getTrafficPairPlanner(idToTransportLayer, configuration);
            case Constants.Traffic.TRAFFIC_ARRIVALS_STRING:
                return new FromStringArrivalPlanner(idToTransportLayer, configuration.getPropertyOrFail(
                        Constants.TrafficArrivals.TRAFFIC_ARRIVALS_LIST), configuration);
            case Constants.Traffic.TRAFFIC_ARRIVALS_FILE:
                return new FromFileArrivalPlanner(idToTransportLayer, configuration.getPropertyOrFail(
                        Constants.TrafficArrivals.TRAFFIC_ARRIVALS_FILE_NAME), configuration);
            default:
                throw new PropertyValueInvalidException(configuration, Constants.Traffic.TRAFFIC);
        }
    }

    private static FlowSizeDistribution getPoissonArrivalFlowSizeDistribution(NBProperties configuration) {
        switch (configuration.getPropertyOrFail(Constants.PoissonArrival.TRAFFIC_FLOW_SIZE_DIST)) {
            case Constants.PoissonArrival.DistributionName.ORIGINAL_SIMON:
                return new OriginalSimonFSD();
            case Constants.PoissonArrival.DistributionName.PFABRIC_DATA_MINING_LOWER_BOUND:
                return new PFabricDataMiningLowerBoundFSD();
            case Constants.PoissonArrival.DistributionName.PFABRIC_DATA_MINING_UPPER_BOUND:
                return new PFabricDataMiningUpperBoundFSD();
            case Constants.PoissonArrival.DistributionName.PFABRIC_WEB_SEARCH_LOWER_BOUND:
                return new PFabricWebSearchLowerBoundFSD();
            case Constants.PoissonArrival.DistributionName.PFABRIC_WEB_SEARCH_UPPER_BOUND:
                return new PFabricWebSearchUpperBoundFSD();
            case Constants.PoissonArrival.DistributionName.PARETO:
                return new ParetoFSD(
                        configuration
                                .getDoublePropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_FLOW_SIZE_DIST_PARETO_SHAPE),
                        configuration
                                .getLongPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_FLOW_SIZE_DIST_PARETO_MEAN_KB));
            case Constants.PoissonArrival.DistributionName.FLOW_SIZE_UNIFORM:
                return new UniformFSD(
                        configuration.getLongPropertyOrFail(
                                Constants.PoissonArrival.TRAFFIC_FLOW_SIZE_DIST_UNIFORM_MEAN_BYTES));
            case Constants.PoissonArrival.DistributionName.FROM_CSV:
                return new FromCSV(configuration
                        .getProperty(Constants.PoissonArrival.CSV_SIZE_DIST_FILE_BYTES));
            case Constants.PoissonArrival.DistributionName.ALIBABA_MACHINE_LEARNING:
                return new AlibabaMachineLearningFSD();
            case Constants.PoissonArrival.DistributionName.FB_FRONTEND_INTRA_DC_CACHE:
                return new FacebookFrontendIntraDCCacheFSD();
            case Constants.PoissonArrival.DistributionName.FB_FRONTEND_INTRA_DC_WEB:
                return new FacebookFrontendIntraDCWebFSD();
            default:
                throw new PropertyValueInvalidException(configuration,
                        Constants.PoissonArrival.TRAFFIC_FLOW_SIZE_DIST);
        }
    }

    private static TrafficPlanner getGenerativePairProbabilities(Map<Integer, TransportLayer> idToTransportLayer,
                                                                 NBProperties configuration, FlowSizeDistribution flowSizeDistribution) {
        String generativePairProbabilities = configuration.getPropertyWithDefault(
                Constants.PoissonArrival.TRAFFIC_PROBABILITIES_GENERATOR,
                Constants.PoissonArrival.ALL_TO_ALL);
        switch (generativePairProbabilities) {
            case Constants.PoissonArrival.ALL_TO_ALL:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.ALL_TO_ALL,
                        configuration);
            case Constants.PoissonArrival.ALL_TO_ALL_FRACTION:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.ALL_TO_ALL_FRACTION,
                        configuration);
            case Constants.PoissonArrival.ALL_TO_ALL_SERVER_FRACTION:
                return new SimpleTrafficPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.ALL_TO_ALL_SERVER_FRACTION,
                        configuration);
            case Constants.PoissonArrival.ALL_TO_ONE:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.ALL_TO_ONE,
                        configuration);
            case Constants.PoissonArrival.ONE_TO_ALL:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.ONE_TO_ALL,
                        configuration);
            case Constants.PoissonArrival.PAIRINGS_FRACTION:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.PAIRINGS_FRACTION,
                        configuration);
            case Constants.PoissonArrival.SKEW_PARETO_DISTRIBUTION:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getDoublePropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.PARETO_SKEW_DISTRIBUTION,
                        configuration);
            case Constants.PoissonArrival.SKEW_PARETO_DISTRIBUTION_SERVER:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getDoublePropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.PARETO_SKEW_DISTRIBUTION_SERVER,
                        configuration);
            case Constants.PoissonArrival.DUAL_ALL_TO_ALL_FRACTION:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.DUAL_ALL_TO_ALL_FRACTION,
                        configuration);
            case Constants.PoissonArrival.DUAL_ALL_TO_ALL_SERVER_FRACTION:
                return new PoissonArrivalPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.DUAL_ALL_TO_ALL_SERVER_FRACTION,
                        configuration);
            case Constants.PoissonArrival.FLUID_FLOW:
                return new FluidFlowTrafficPlanner(idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        configuration.getIntegerPropertyWithDefault(
                                Constants.ScenarioTopology.EXTEND_SERVERS_PER_TL_NODE,
                                0) == 0
                                ? PoissonArrivalPlanner.PairDistribution.ALL_TO_ALL
                                : PoissonArrivalPlanner.PairDistribution.ALL_TO_ALL_FRACTION,
                        configuration);
            case Constants.PoissonArrival.FLUID_FLOW_PERM:
                return new FluidFlowTrafficPlanner(idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.PAIRINGS_FRACTION,
                        configuration);
            case Constants.PoissonArrival.FLUID_FLOW_DENSITY_MATRIX:
                return new FluidFlowTrafficPlanner(idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        PoissonArrivalPlanner.PairDistribution.DENSITY_MATRIX,
                        configuration);
            case Constants.PoissonArrival.META_NODE_PREMUTATION_TRAFFIC:
                return new MetaNodePermutationTrafficPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        configuration);
            case Constants.PoissonArrival.META_NODE_A2A_TRAFFIC:
                return new MetaNodeA2ATrafficPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        configuration);
            case Constants.PoissonArrival.MOCK_META_NODE_PREMUTATION_TRAFFIC:
                return new MockMetaNodePermutationTrafficPlanner(
                        idToTransportLayer,
                        configuration
                                .getIntegerPropertyOrFail(
                                        Constants.PoissonArrival.TRAFFIC_LAMBDA_FLOW_STARTS_PER_S),
                        flowSizeDistribution,
                        configuration);
            default:
                throw new PropertyValueInvalidException(configuration,
                        Constants.PoissonArrival.TRAFFIC_PROBABILITIES_GENERATOR);
        }
    }

    private static TrafficPlanner getTrafficPairPlanner(Map<Integer, TransportLayer> idToTransportLayer,
                                                        NBProperties configuration) {
        switch (configuration.getPropertyOrFail(Constants.TrafficPair.TRAFFIC_PAIR_TYPE)) {
            case Constants.TrafficPair.ALL_TO_ALL:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateAllToAll(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.ALL_TO_ONE:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateAllToOne(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.ONE_TO_ALL:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateOneToAll(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.ALL_TO_ONE_TOR:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateAllToOneTor(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.ONE_TO_ALL_TOR:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateOneToAllTor(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.ONE_TO_ONE:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateOneToOne(configuration),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.STRIDE:
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        TrafficPairPlanner.generateStride(
                                configuration.getGraphDetails().getNumNodes(),
                                configuration.getIntegerPropertyOrFail(
                                        Constants.TrafficPair.TRAFFIC_PAIR_STRIDE)),
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.TrafficPair.CUSTOM:
                List<Integer> list = configuration
                        .getDirectedPairsListPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIRS);
                List<TrafficPairPlanner.TrafficPair> pairs = new ArrayList<>();
                for (int i = 0; i < list.size(); i += 2) {
                    pairs.add(new TrafficPairPlanner.TrafficPair(list.get(i), list.get(i + 1)));
                }
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        pairs,
                        configuration.getLongPropertyOrFail(
                                Constants.TrafficPair.TRAFFIC_PAIR_FLOW_SIZE_BYTE),
                        configuration);
            case Constants.DistributedTraining.DATA_PARALLEL:
            case Constants.DistributedTraining.PIPELINE_PARALLEL:
            case Constants.DistributedTraining.HYBRID_PARALLEL:
                String baseTrafficPairsDir = configuration.getPropertyOrFail(
                        Constants.DistributedTraining.BASE_TRAFFIC_PAIRS_DIR);
                return new TrafficPairPlanner(
                        idToTransportLayer,
                        baseTrafficPairsDir,
                        0,
                        configuration);
            default:
                throw new PropertyValueInvalidException(configuration,
                        Constants.TrafficPair.TRAFFIC_PAIR_TYPE);
        }
    }
}
