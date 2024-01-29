package ch.ethz.systems.netbench.xpt.dynamic.controller;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.utility.Constants;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.VariableGraph;
import edu.asu.emit.algorithm.graph.Vertex;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class DynamicController extends RemoteRoutingController {
    protected Map<Integer, NetworkDevice> mIdToNetworkDevice;
    protected int max_degree;

    public DynamicController(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(configuration);
        mPaths = new HashMap<>();
        mIdToNetworkDevice = idToNetworkDevice;
        this.mMainGraph = new VariableGraph(configuration.getGraph(), "graph_weight");
        max_degree = configuration.getIntegerPropertyOrFail(Constants.DynamicController.MAX_DYNAMIC_SWITCH_DEGREE);
        System.out.println("Setting max dynamic degree to " + max_degree);

    }

    @Override
    protected void reset_state(NBProperties configuration) {
        // TODO Auto-generated method stub

    }


    @Override
    protected void removePathFromGraph(Path p) {

    }

    /**
     * update a devices forwarding table with source dest key
     * with first id of path to last id
     *
     * @param sourceKey
     * @param destKey
     * @param p
     * @param jumboFlowId
     */
    @Override
    protected void updateForwardingTables(int sourceKey, int destKey, Path p, long jumboFlowId) {
        DynamicDevice sourceDevice = (DynamicDevice) mIdToNetworkDevice.get(p.getFirstVertex().getId());
        sourceDevice.addConnection(mIdToNetworkDevice.get(p.getLastVertex().getId()), jumboFlowId);
    }

    @Override
    protected Path generatePathFromGraph(int sourceToR, int destToR) {
        Vertex sourceVertex = new Vertex(sourceToR);
        Vertex destVertex = new Vertex(destToR);
        LinkedList<Vertex> path = new LinkedList<Vertex>();
        path.add(sourceVertex);
        path.add(destVertex);
        Path finalPath = new Path(path, 1);
        return finalPath;
    }

    @Override
    public int getCircuitFlowLimit() {
        return max_degree;
    }


    @Override
    public void reset() {
        ((VariableGraph) mMainGraph).clear();

    }


    /**
     * remove a dynamic connection of source dest keys
     *
     * @param p
     * @param sourceKey
     * @param destKey
     * @param transimttingSource
     * @param receivingDest
     * @param jumboFlowId
     */
    @Override
    protected void returnPathToGraph(Path p, int sourceKey, int destKey, int transimttingSource, int receivingDest, long jumboFlowId) {
        DynamicDevice sourceDevice = (DynamicDevice) mIdToNetworkDevice.get(transimttingSource);
        sourceDevice.removeConnection(jumboFlowId);
    }


    @Override
    public void dumpState(String dumpFolderName) throws IOException {
        // TODO Auto-generated method stub

    }


}
