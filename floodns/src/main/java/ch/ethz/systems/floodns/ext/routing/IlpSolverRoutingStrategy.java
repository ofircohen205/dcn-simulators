package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.deeplearningtraining.Job;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.sysutils.SharedMemory;
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
public class IlpSolverRoutingStrategy extends CentralizedRoutingStrategy {

    private final Map<Integer, ImmutablePair<Integer, Integer>> commodities = new HashMap<>();
    private final Map<Integer, Connection> connections = new HashMap<>();
    private final Map<Integer, Integer> connIdToJobId = new HashMap<>();
    private final String runDirectory;
    private final String sharedMemoryPythonPath;
    private final String sharedMemoryJavaPath;

//    private final MPSolver solver;
//    private final Map<Integer, List<MPVariable>> commodityPathVariables = new HashMap<>();

//    private final Map<Integer, MPVariable> linkVariables = new HashMap<>();
//    private final Map<Integer, List<MPVariable>> linkPathVariables = new HashMap<>();
//    private final MPVariable alphaVariable;

    public IlpSolverRoutingStrategy(Simulator simulator, Topology topology, String runDirectory) {
        super(simulator, topology);
        this.runDirectory = runDirectory;
        this.sharedMemoryJavaPath = runDirectory + "/shared_memory_java.json";
        this.sharedMemoryPythonPath = runDirectory + "/shared_memory_python.json";

//        Loader.loadNativeLibraries();
//        solver = MPSolver.createSolver("SCIP");
//        if (solver == null) {
//            throw new RuntimeException("Solver is null");
//        }
//        alphaVariable = solver.makeIntVar(0, Double.POSITIVE_INFINITY, "alpha");
//
//        // Add link variables
//        for (int coreId : getCoreIds()) {
//            for (int torId : topologyDetails.getTorNodeIds()) {
//                Link link = network.getPresentLinksBetween(coreId, torId).iterator().next();
//                MPVariable coreToTorVar = solver.makeIntVar(0, Double.POSITIVE_INFINITY, "x_e[" + coreId + "," + torId + "]");
//                linkVariables.put(link.getLinkId(), coreToTorVar);
//                linkPathVariables.put(link.getLinkId(), new ArrayList<>());
//
//                link = network.getPresentLinksBetween(torId, coreId).iterator().next();
//                MPVariable torToCoreVar = solver.makeIntVar(0, Double.POSITIVE_INFINITY, "x_e[" + torId + "," + coreId + "]");
//                linkVariables.put(link.getLinkId(), torToCoreVar);
//                linkPathVariables.put(link.getLinkId(), new ArrayList<>());
//            }
//        }
    }

    @Override
    public void addSrcDst(Connection connection) {
        int srcId = connection.getSrcNodeId();
        int dstId = connection.getDstNodeId();
        int connId = connection.getConnectionId();
        commodities.put(connId, ImmutablePair.of(srcId, dstId));
        connIdToJobId.put(connId, connection.getJobId());
        connections.put(connId, connection);

//        int srcTorId = topologyDetails.getTorIdOfServer(srcId);
//        int dstTorId = topologyDetails.getTorIdOfServer(dstId);
//        commodityPathVariables.put(connection.getConnectionId(), new ArrayList<>());
//        for (int coreId : getCoreIds()) {
//            String varName = "x_p_l_" + connId + "[" + coreId + "]";
//            MPVariable var = solver.makeIntVar(0, 1, varName);
//            commodityPathVariables.get(connId).add(var);
//
//            Link link = network.getPresentLinksBetween(srcTorId, coreId).iterator().next();
//            linkPathVariables.get(link.getLinkId()).add(var);
//
//            link = network.getPresentLinksBetween(coreId, dstTorId).iterator().next();
//            linkPathVariables.get(link.getLinkId()).add(var);
//        }
    }

    @Override
    public void clearResources(Connection connection) {
        super.clearResources(connection);
        commodities.remove(connection.getConnectionId());
        connIdToJobId.remove(connection.getConnectionId());
        connections.remove(connection.getConnectionId());
//        commodityPathVariables.remove(connection.getConnectionId());
    }

    @Override
    public void determinePathAssignments() {
        if (commodities.isEmpty()) {
            return;
        }

        // Add constraints
//        addCommodityTraverseOnePathConstraints();
//        addMinimizeMostUsedLinkConstraints();
//        setObjective();

//        final MPSolver.ResultStatus resultStatus = solver.solve();
//        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
//            throw new RuntimeException("The problem does not have an optimal solution!");
//        }

        List<Integer> coreIds = getCoreIds();

        // Get assignments
//        for (int connId : commodityPathVariables.keySet()) {
//            List<MPVariable> variables = commodityPathVariables.get(connId);
//            for (int i = 0; i < variables.size(); i++) {
//                if (variables.get(i).solutionValue() == 1) {
//                    Connection connection = connections.get(connId);
//                    int coreId = coreIds.get(i % coreIds.size());
//                    AcyclicPath path = RoutingUtility.constructPath(network, connection, coreId);
//                    Job job = simulator.getJobs().get(connIdToJobId.get(connId));
//                    ImmutablePair<Integer, Integer> commodity = commodities.get(connId);
//                    job.getCommoditiesPathMap().put(commodity, path);
//                    if (activeConnections.contains(connId)) {
//                        RoutingUtility.resetPath(simulator, simulator.getActiveConnection(connId), path);
//                    }
//                    break;
//                }
//            }
//        }

        // Reset solver
//        solver.clear();


        long start = System.currentTimeMillis();
        Map<Integer, Integer> assignedPaths = SharedMemory.receivePathAssignmentsFromController(
                sharedMemoryJavaPath, sharedMemoryPythonPath, getJsonRequest(), runDirectory, true
        );
        for (int connId : assignedPaths.keySet()) {
            Connection connection = connections.get(connId);
            int coreId = coreIds.get(assignedPaths.get(connId) % coreIds.size());
            AcyclicPath path = RoutingUtility.constructPath(network, connection, coreId);
            Job job = simulator.getJobs().get(connIdToJobId.get(connId));
            ImmutablePair<Integer, Integer> commodity = commodities.get(connId);
            job.getCommoditiesPathMap().put(commodity, path);
            if (activeConnections.contains(connId)) {
                RoutingUtility.resetPath(simulator, simulator.getActiveConnection(connId), path);
            }
        }
        durations.add(System.currentTimeMillis() - start);
    }


    private String getJsonRequest() {
        Set<Link> failedLinks = new HashSet<>(network.getFailedLinks());
        Set<ImmutablePair<Integer, Integer>> failedLinkPairs = new HashSet<>();
        failedLinks.forEach(link -> failedLinkPairs.add(ImmutablePair.of(link.getFrom(), link.getTo())));
        failedLinks.forEach(link -> failedLinkPairs.add(ImmutablePair.of(link.getTo(), link.getFrom())));

        Map<String, String> map = new HashMap<>();
        map.put("src_dst_pairs", commodities.toString());
        map.put("failed_links", failedLinkPairs.toString());
        map.put("failed_cores", network.getFailedNodes().toString());
        map.put("num_tors", String.valueOf(topology.getDetails().getNumTors()));
        Gson gson = new Gson();
        return gson.toJson(map);
    }

//    private void addCommodityTraverseOnePathConstraints() {
//        for (int connId : commodityPathVariables.keySet()) {
//            MPConstraint constraint = solver.makeConstraint(0, 1, "commodity_traverse_one_path_" + connId);
//            for (MPVariable variable : commodityPathVariables.get(connId)) {
//                constraint.setCoefficient(variable, 1);
//            }
//        }
//    }
//
//    private void addMinimizeMostUsedLinkConstraints() {
//        for (int linkId : linkVariables.keySet()) {
//            MPVariable linkVar = linkVariables.get(linkId);
//            List<MPVariable> variables = linkPathVariables.get(linkId);
//
//            // Add constraint
//            MPConstraint constraint = solver.makeConstraint(-Double.POSITIVE_INFINITY, 0, "minimize_most_used_link_" + linkId);
//            for (MPVariable variable : variables) {
//                constraint.setCoefficient(variable, 1);
//            }
//            constraint.setCoefficient(linkVar, -1);
//        }
//
//        for (int linkId : linkVariables.keySet()) {
//            MPVariable linkVar = linkVariables.get(linkId);
//            MPConstraint constraint = solver.makeConstraint(-Double.POSITIVE_INFINITY, 0, "minimize_alpha_" + linkId);
//            constraint.setCoefficient(linkVar, 1);
//            constraint.setCoefficient(alphaVariable, -1);
//        }
//    }
//
//    private void setObjective() {
//        MPObjective objective = solver.objective();
//        for (int connId : commodityPathVariables.keySet()) {
//            for (MPVariable variable : commodityPathVariables.get(connId)) {
//                objective.setCoefficient(variable, 0);
//            }
//        }
//        for (int linkId : linkVariables.keySet()) {
//            objective.setCoefficient(linkVariables.get(linkId), 0);
//        }
//
//        objective.setCoefficient(alphaVariable, 1);
//        objective.setMinimization();
//    }
}
