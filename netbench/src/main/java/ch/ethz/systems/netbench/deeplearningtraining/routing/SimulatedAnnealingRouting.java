package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.deeplearningtraining.utils.RoutingUtility;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.stream.Collectors;

public class SimulatedAnnealingRouting extends CentralizedController {

    private final Set<Flow> flows = new HashSet<>();
    private final Random random;

    public SimulatedAnnealingRouting() {
        random = new Random();
    }

    @Override
    public void addCommodity(Flow flow) {
        flows.add(flow);
    }

    @Override
    public void clearResources(Flow flow) {
        super.clearResources(flow);
        flows.remove(flow);
    }

    @Override
    public void determinePathAssignments() {
        if (flows.isEmpty()) {
            return;
        } else if (flows.size() == 1) {
            Flow flow = flows.iterator().next();
            List<Integer> path = RoutingUtility.constructPath(graphDetails, flow, getCoreIds().get(0));
            Job job = Simulator.getJob(flow.getJobId());
            ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(flow.getSrcId(), flow.getDstId());
            job.getCommoditiesPaths().put(commodity, path);
            return;
        }

        long start = System.currentTimeMillis();
        Map<Integer, Integer> currentState = getInitialState();
        int currentEnergy = calculateEnergy(currentState);

        // Simulated annealing
        double temperature = 1000;
        double coolingRate = 0.01;
        while (temperature > 0.1) {
            Map<Integer, Integer> neighbourState = getNeighbour(currentState);
            int neighbourEnergy = calculateEnergy(neighbourState);
            if (neighbourEnergy < currentEnergy) {
                currentState = new HashMap<>(neighbourState);
                currentEnergy = neighbourEnergy;
            } else if (acceptanceProbability(currentEnergy, neighbourEnergy, temperature) > random.nextDouble()) {
                currentState = new HashMap<>(neighbourState);
                currentEnergy = neighbourEnergy;
            }
            temperature *= 1 - coolingRate;
        }

        // Set the path assignments
        List<Integer> coreIds = getCoreIds();

        GraphDetails graphDetails = Simulator.getConfiguration().getGraphDetails();
        int numCores = graphDetails.getNumCores() - graphDetails.getFailedCores().size();
        currentState.forEach((dstNodeId, selectedCoreId) -> {
            Set<Flow> dstFlows = flows
                    .stream().filter(flow -> flow.getDstId() == dstNodeId)
                    .collect(Collectors.toSet());
            dstFlows.forEach(dstFlow -> {
                int coreId = selectedCoreId % numCores;
                List<Integer> path = RoutingUtility.constructPath(graphDetails, dstFlow, coreId);
                Job job = Simulator.getJobs().get(dstFlow.getJobId());
                ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(dstFlow.getSrcId(), dstFlow.getDstId());
                job.getCommoditiesPaths().put(commodity, path);
            });
        });
        durations.add(System.currentTimeMillis() - start);
    }

    private Map<Integer, Integer> getInitialState() {
        Map<Integer, Integer> initialState = new HashMap<>();
        GraphDetails graphDetails = Simulator.getConfiguration().getGraphDetails();
        flows.forEach(flow -> {
            int dstId = flow.getDstId();
            int dstTorId = graphDetails.getTorIdOfServer(dstId);
            List<Integer> torHostIds = new ArrayList<>(graphDetails.getServersOfTor(dstTorId));
            int coreId = torHostIds.indexOf(dstId);
            initialState.putIfAbsent(dstId, coreId);
        });

        return initialState;
    }

    private Map<Integer, Integer> getNeighbour(Map<Integer, Integer> currentState) {
        return random.nextDouble() > 0.5 ? swapConnections(currentState) : swapDestinationsCores(currentState);
    }

    /**
     * Calculate the energy of the current state.
     * The energy is the sum of all most utilized links, i.e.,
     * the number of flows that pass through the most utilized links.
     * For example, if we have the following links:
     * - Link 1: 10 flows
     * - Link 2: 5 flows
     * - Link 3: 5 flows
     * - Link 4: 10 flows
     * Then the energy is 10 + 10 = 20.
     *
     * @param state The current state
     * @return The energy of the current state
     */
    private int calculateEnergy(Map<Integer, Integer> state) {
        int energy = 0;
        List<Integer> coreIds = getCoreIds();
        Map<ImmutablePair<Integer, Integer>, Integer> linkNumActiveFlows = new HashMap<>();
        GraphDetails graphDetails = Simulator.getConfiguration().getGraphDetails();
        state.forEach((dstNodeId, coreIndex) -> {
            int coreId = coreIds.get(coreIndex % coreIds.size());
            Set<Flow> dstFlows = flows.stream().filter(flow -> flow.getDstId() == dstNodeId)
                    .collect(Collectors.toSet());
            dstFlows.forEach(dstFlow -> {
                int srcTorId = graphDetails.getTorIdOfServer(dstFlow.getSrcId());
                ImmutablePair<Integer, Integer> srcTorToCore = ImmutablePair.of(srcTorId, coreId);
                linkNumActiveFlows.put(srcTorToCore, linkNumActiveFlows.getOrDefault(srcTorToCore, 0) + 1);

                int dstTorId = graphDetails.getTorIdOfServer(dstFlow.getDstId());
                ImmutablePair<Integer, Integer> coreToDstTor = ImmutablePair.of(coreId, dstTorId);
                linkNumActiveFlows.put(coreToDstTor, linkNumActiveFlows.getOrDefault(coreToDstTor, 0) + 1);
            });
        });
        // Get the most utilized links
        List<Integer> numActiveFlows = new ArrayList<>(linkNumActiveFlows.values());
        numActiveFlows.sort(Collections.reverseOrder());
        int maxNumActiveFlows = numActiveFlows.get(0);
        // Calculate the energy
        for (int numActiveFlow : numActiveFlows) {
            if (numActiveFlow == maxNumActiveFlows) {
                energy += numActiveFlow;
            } else {
                break;
            }
        }
        return energy;
    }

    private double acceptanceProbability(double currentEnergy, double candidateEnergy, double temperature) {
        if (currentEnergy > candidateEnergy) {
            return 1;
        }
        return Math.exp((currentEnergy - candidateEnergy) / temperature);
    }

    private Map<Integer, Integer> swapDestinationsCores(Map<Integer, Integer> currentState) {
        List<Integer> dstIds = new ArrayList<>(currentState.keySet());
        Map<Integer, List<Integer>> torToDstIds = new HashMap<>();
        GraphDetails graphDetails = Simulator.getConfiguration().getGraphDetails();
        dstIds.forEach(dstId -> {
            int dstTorId = graphDetails.getTorIdOfServer(dstId);
            torToDstIds.putIfAbsent(dstTorId, new ArrayList<>());
            torToDstIds.get(dstTorId).add(dstId);
        });
        // keep only the tor ids that have more than one dst ids
        Map<Integer, List<Integer>> finalTorToDstIds = torToDstIds.entrySet().stream().filter(entry -> entry.getValue().size() > 1).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (finalTorToDstIds.isEmpty()) {
            return swapConnections(currentState);
        }

        List<Integer> torIds = new ArrayList<>(finalTorToDstIds.keySet());
        int torId = torIds.get(random.nextInt(torIds.size()));

        Map<Integer, Integer> neighbour = new HashMap<>(currentState);
        List<Integer> hostIds = finalTorToDstIds.get(torId);
        swap(hostIds, neighbour);
        return neighbour;
    }

    private Map<Integer, Integer> swapConnections(Map<Integer, Integer> currentState) {
        Map<Integer, Integer> neighbour = new HashMap<>(currentState);
        List<Integer> dstIds = new ArrayList<>(neighbour.keySet());
        swap(dstIds, neighbour);
        return neighbour;
    }

    private void swap(List<Integer> hostIds, Map<Integer, Integer> neighbour) {
        int firstDstId = hostIds.get(random.nextInt(hostIds.size()));
        int secondDstId = hostIds.get(random.nextInt(hostIds.size()));
        while (firstDstId == secondDstId) {
            secondDstId = hostIds.get(random.nextInt(hostIds.size()));
        }

        // Swap the core ids
        int firstCoreId = neighbour.get(firstDstId);
        int secondCoreId = neighbour.get(secondDstId);
        neighbour.put(firstDstId, secondCoreId);
        neighbour.put(secondDstId, firstCoreId);
    }
}
