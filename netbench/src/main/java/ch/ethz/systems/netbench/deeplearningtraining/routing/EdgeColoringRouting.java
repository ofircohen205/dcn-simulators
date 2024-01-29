package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.deeplearningtraining.utils.RoutingUtility;
import ch.ethz.systems.netbench.deeplearningtraining.utils.SharedMemory;
import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdgeColoringRouting extends CentralizedController {

    private final Map<Long, Flow> flows = new HashMap<>();
    private final Map<Long, ImmutablePair<Integer, Integer>> torCommodities = new HashMap<>();
    private final String runDirectory;
    private final String sharedMemoryPythonPath;
    private final String sharedMemoryJavaPath;

    public EdgeColoringRouting() {
        runDirectory = SimulationLogger.getRunFolderFull();
        sharedMemoryJavaPath = runDirectory + "/shared_memory_java.json";
        sharedMemoryPythonPath = runDirectory + "/shared_memory_python.json";
    }

    @Override
    public void addCommodity(Flow flow) {
        int srcId = flow.getSrcId();
        int srcTorId = graphDetails.getTorIdOfServer(srcId);
        int dstId = flow.getDstId();
        int dstTorId = graphDetails.getTorIdOfServer(dstId);
        flows.put(flow.getFlowId(), flow);
        torCommodities.put(flow.getFlowId(), ImmutablePair.of(srcTorId, dstTorId));
    }

    @Override
    public void clearResources(Flow flow) {
        super.clearResources(flow);
        flows.remove(flow.getFlowId());
        torCommodities.remove(flow.getFlowId());
    }

    @Override
    public void determinePathAssignments() {
        if (flows.isEmpty()) {
            return;
        }

        List<Integer> coreIds = getCoreIds();
        long start = System.currentTimeMillis();
        Map<Long, Integer> assignments = SharedMemory.receivePathAssignmentsFromController(
                sharedMemoryJavaPath, sharedMemoryPythonPath, getJsonRequest(), runDirectory, false
        );
//        Map<Long, Integer> assignments = EdgeColoring.colorEdges(torCommodities);
        for (long flowId : assignments.keySet()) {
            Flow flow = flows.get(flowId);
            int coreId = coreIds.get(assignments.get(flowId) % coreIds.size());
            List<Integer> path = RoutingUtility.constructPath(graphDetails, flow, coreId);
            Job job = Simulator.getJobs().get(flow.getJobId());
            ImmutablePair<Integer, Integer> commodity = ImmutablePair.of(flow.getSrcId(), flow.getDstId());
            job.getCommoditiesPaths().put(commodity, path);
        }
        durations.add(System.currentTimeMillis() - start);
    }

    private String getJsonRequest() {
        Map<String, String> map = new HashMap<>();
        map.put("src_dst_pairs", torCommodities.toString());
        map.put("output_folder", runDirectory);
        Gson gson = new Gson();
        return gson.toJson(map);
    }

}
