package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.deeplearningtraining.utils.RoutingUtility;
import ch.ethz.systems.netbench.deeplearningtraining.utils.SharedMemory;
import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

/**
 * Routing strategy that uses an LP problem to determine the path assignments.
 * <p>
 * ILP formulation:
 * <p>
 * \min \alpha
 * <p>
 * Subject to \sum_{p \in P_l} x_p^l = 1 \forall l \in L
 * <p>
 * \sum_{l \in L} \sum_{p \in P_l : e \in p} x_p^l \leq x_e \forall e \in E
 * <p>
 * x_e \leq \alpha \forall e \in E
 * <p>
 * x_p^l = 0 \forall l \in L, p \in P_l : e \in p \land e \in F
 * <p>
 * x_p^l \in \{0,1\} \forall p \in P_l, l \in L
 * <p>
 * x_e \in \mathbb{N}^+ \forall e \in E
 * <p>
 * \alpha \in \mathbb{N}^+
 */
public class IlpSolverRouting extends CentralizedController {

    private final Map<Long, ImmutablePair<Integer, Integer>> commodities = new HashMap<>();
    private final Map<Long, Flow> flows = new HashMap<>();
    private final String runDirectory;
    private final String sharedMemoryPythonPath;
    private final String sharedMemoryJavaPath;

    public IlpSolverRouting() {
        runDirectory = SimulationLogger.getRunFolderFull();
        sharedMemoryJavaPath = runDirectory + "/shared_memory_java.json";
        sharedMemoryPythonPath = runDirectory + "/shared_memory_python.json";
    }

    @Override
    public void addCommodity(Flow flow) {
        int srcId = flow.getSrcId();
        int dstId = flow.getDstId();
        long flowId = flow.getFlowId();
        commodities.put(flowId, ImmutablePair.of(srcId, dstId));
        flows.put(flowId, flow);
    }

    @Override
    public void clearResources(Flow flow) {
        super.clearResources(flow);
        commodities.remove(flow.getFlowId());
        flows.remove(flow.getFlowId());
    }

    @Override
    public void determinePathAssignments() {
        if (commodities.isEmpty()) {
            return;
        }

        List<Integer> coreIds = getCoreIds();
        long start = System.currentTimeMillis();
        Map<Long, Integer> assignedPaths = SharedMemory.receivePathAssignmentsFromController(
                sharedMemoryJavaPath, sharedMemoryPythonPath, getJsonRequest(), runDirectory, true
        );
        for (long flowId : assignedPaths.keySet()) {
            Flow flow = flows.get(flowId);
            int coreId = coreIds.get(assignedPaths.get(flowId) % coreIds.size());
            List<Integer> path = RoutingUtility.constructPath(graphDetails, flow, coreId);
            Job job = Simulator.getJobs().get(flow.getJobId());
            ImmutablePair<Integer, Integer> commodity = commodities.get(flowId);
            job.getCommoditiesPaths().put(commodity, path);
        }
        durations.add(System.currentTimeMillis() - start);
    }

    private String getJsonRequest() {
        GraphDetails graphDetails = Simulator.getConfiguration().getGraphDetails();
        Set<Integer> failedCores = graphDetails.getFailedCores();
        Set<Integer> torIds = graphDetails.getTorNodeIds();
        Set<ImmutablePair<Integer, Integer>> failedLinks = new HashSet<>();
        for (int torId : torIds) {
            for (int coreId : failedCores) {
                failedLinks.add(ImmutablePair.of(torId, coreId));
                failedLinks.add(ImmutablePair.of(coreId, torId));
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("src_dst_pairs", commodities.toString());
        map.put("failed_links", failedLinks.toString());
        map.put("failed_cores", failedCores.toString());
        map.put("num_tors", String.valueOf(graphDetails.getNumTors()));
        Gson gson = new Gson();
        return gson.toJson(map);
    }

}
