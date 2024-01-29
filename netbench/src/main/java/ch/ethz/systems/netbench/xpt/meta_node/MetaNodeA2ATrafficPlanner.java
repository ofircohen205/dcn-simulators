package ch.ethz.systems.netbench.xpt.meta_node;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.poissontraffic.PoissonArrivalPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FlowSizeDistribution;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MetaNodeA2ATrafficPlanner extends PoissonArrivalPlanner {

    public MetaNodeA2ATrafficPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, double lambdaFlowStartsPerSecond, FlowSizeDistribution flowSizeDistribution, NBProperties configuration) {
        super(idToTransportLayerMap, lambdaFlowStartsPerSecond, flowSizeDistribution, configuration);
        this.configuration = configuration;
        this.setTrafficPorbabilities();
    }

    private void setTrafficPorbabilities() {
        System.out.print("Generating permutation traffic between meta nodes");
        int numMetaNodes = getNumMetaNode();


        List<Integer> MNs = new LinkedList<>();

        for (int i = 0; i < numMetaNodes; i++) {
            MNs.add(i);
        }
        Collections.shuffle(MNs, Simulator.selectIndependentRandom("meta_node_permutation_shuffle"));

        double serverPairNum = 0.0;
        for (int i = 0; i < numMetaNodes; i++) {
            for (int j = 0; j < numMetaNodes; j++) {
                if (i == j) continue;
                List<Integer> serversA = getServesPerMNList(MNs.get(i));
                List<Integer> serversB = getServesPerMNList(MNs.get(j));
                SimulationLogger.logInfo("Traffic from " + MNs.get(i) + " To " + MNs.get(j),
                        "Servers " + serversA + " To " + serversB);
                if (configuration.getBooleanPropertyWithDefault(
                        Constants.MetaNode.SAME_RACK_TRAFFIC,
                        false)) {
                    for (int serverA : serversA) {
                        for (int serverB : serversA) {
                            if (serverA == serverB) continue;
                            serverPairNum += 1d;
                        }
                    }

                    for (int serverA : serversB) {
                        for (int serverB : serversB) {
                            if (serverA == serverB) continue;
                            serverPairNum += 1d;
                        }
                    }
                }


                for (int serverA : serversA) {
                    for (int serverB : serversB) {
                        serverPairNum += 2d;
                    }
                }

            }
        }
        double serverPairProb = 1d / serverPairNum;


        for (int i = 0; i < numMetaNodes; i++) {
            for (int j = 0; j < numMetaNodes; j++) {
                if (i == j) continue;
                List<Integer> serversA = getServesPerMNList(MNs.get(i));
                List<Integer> serversB = getServesPerMNList(MNs.get(j));

                if (configuration.getBooleanPropertyWithDefault(
                        Constants.MetaNode.SAME_RACK_TRAFFIC,
                        false)) {
                    for (int serverA : serversA) {
                        for (int serverB : serversA) {
                            if (serverA == serverB) continue;
                            addToPool(serverPairProb, new ImmutablePair<Integer, Integer>(serverA, serverB));
                        }
                    }

                    for (int serverA : serversB) {
                        for (int serverB : serversB) {
                            if (serverA == serverB) continue;
                            addToPool(serverPairProb, new ImmutablePair<Integer, Integer>(serverA, serverB));
                        }
                    }

                }


                for (int serverA : serversA) {
                    for (int serverB : serversB) {
                        addToPool(serverPairProb, new ImmutablePair<Integer, Integer>(serverA, serverB));
                        addToPool(serverPairProb, new ImmutablePair<Integer, Integer>(serverB, serverA));

                    }
                }

            }
            System.out.println(" done.");
        }
    }

    protected int getNumMetaNode() {
        return configuration.getGraphDetails().getMetaNodeNum();
    }

    protected List<Integer> getServesPerMNList(int MNId) {
        int numToRs = configuration.getGraphDetails().getNumTors();
        List<Integer> servers = new LinkedList<>();
        int serversPerMN = getNumServersPerMN();
        for (int i = 0; i < serversPerMN; i++) {
            servers.add(numToRs + MNId * serversPerMN + i);
        }
        return servers;
    }

    protected int getNumServersPerMN() {
        return configuration.getGraphDetails().getNumServers() / getNumMetaNode();
    }
}
