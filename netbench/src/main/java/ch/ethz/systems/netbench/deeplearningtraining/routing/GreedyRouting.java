package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.deeplearningtraining.utils.RoutingUtility;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Minimize Crossing Virtual Link Collisions (MCVLC) algorithm to determine the path assignment for each flow.
 * The algorithm will assign a path for each flow as follows:
 * 1. For each virtual links under ToR i, find the core node with the least
 * number of assigned virtual links.
 * 2. Assign the virtual link to the core node with the least number of assigned
 * virtual links.
 * 3. Repeat for all virtual links.
 */
public class GreedyRouting extends CentralizedController {

    private final Map<Integer, Set<Flow>> uplinkTorFlows = new HashMap<>();
    private final Map<Integer, Set<Flow>> downlinkTorFlows = new HashMap<>();

    public GreedyRouting() {
        super();
    }

    @Override
    public void addCommodity(Flow flow) {
        int srcId = flow.getSrcId();
        int srcTorId = graphDetails.getTorIdOfServer(srcId);
        int dstId = flow.getDstId();
        int dstTorId = graphDetails.getTorIdOfServer(dstId);
        ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(srcId, dstId);
        uplinkTorFlows.computeIfAbsent(srcTorId, k -> new HashSet<>()).add(flow);
        downlinkTorFlows.computeIfAbsent(dstTorId, k -> new HashSet<>()).add(flow);
    }

    @Override
    public void clearResources(Flow flow) {
        super.clearResources(flow);
        int srcId = flow.getSrcId();
        int dstId = flow.getDstId();
        uplinkTorFlows.get(graphDetails.getTorIdOfServer(srcId)).remove(flow);
        downlinkTorFlows.get(graphDetails.getTorIdOfServer(dstId)).remove(flow);
    }

    @Override
    public void determinePathAssignments() {
        if (uplinkTorFlows.isEmpty() && downlinkTorFlows.isEmpty()) {
            return;
        }

        Set<Flow> assignedFlows = new HashSet<>();
        Map<ImmutablePair<Integer, Integer>, Integer> linkAssignmentsCounter = new HashMap<>();
        List<Set<Flow>> torFlows = sortTorFlows();
        List<Integer> coreIds = getCoreIds();
        long start = System.currentTimeMillis();
        torFlows.forEach(flows -> {
            flows.removeAll(assignedFlows); // Remove already assigned connections
            flows.forEach(flow -> assignPath(flow, linkAssignmentsCounter, coreIds));
            assignedFlows.addAll(flows);
        });
        durations.add(System.currentTimeMillis() - start);
    }

    private void assignPath(
            Flow flow,
            Map<ImmutablePair<Integer, Integer>, Integer> linkAssignmentsCounter,
            List<Integer> coreIds
    ) {
        List<Integer> path = RoutingUtility.determinePath(graphDetails, flow, linkAssignmentsCounter, coreIds);
        Job job = Simulator.getJobs().get(flow.getJobId());
        ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(flow.getSrcId(), flow.getDstId());
        job.getCommoditiesPaths().put(commodity, path);
    }

    private List<Set<Flow>> sortTorFlows() {
        TreeMap<String, Set<Flow>> torLinksSrcDstMap = new TreeMap<>();
        graphDetails.getTorNodeIds().forEach(torId -> {
            Optional.ofNullable(uplinkTorFlows.get(torId))
                    .ifPresent(uplinkCommodities -> torLinksSrcDstMap.put("a_" + torId, uplinkCommodities));
            Optional.ofNullable(downlinkTorFlows.get(torId))
                    .ifPresent(downlinkCommodities -> torLinksSrcDstMap.put("b_" + torId, downlinkCommodities));
        });

        // Sorted map
        Map<String, Set<Flow>> sortedMap = torLinksSrcDstMap.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Set<Flow>>>comparingInt(e -> e.getValue().size()).reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return new ArrayList<>(sortedMap.values());
    }


}
