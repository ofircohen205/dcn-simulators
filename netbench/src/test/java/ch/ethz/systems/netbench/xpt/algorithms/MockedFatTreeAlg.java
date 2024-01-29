package ch.ethz.systems.netbench.xpt.algorithms;

import edu.asu.emit.algorithm.graph.BaseGraph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;
import edu.asu.emit.algorithm.graph.algorithms.FatTreeShortestPathAlg;

import java.util.HashMap;
import java.util.HashSet;

public class MockedFatTreeAlg extends FatTreeShortestPathAlg{
    protected HashMap<Vertex,Vertex> sourcePredecessorIndex;
    protected HashMap<Vertex,Vertex> destPredecessorIndex;
    public MockedFatTreeAlg(BaseGraph graph, int deg) {
        super(graph, deg,false);
        this.destPredecessorIndex = super.destPredecessorIndex;
        this.sourcePredecessorIndex = super.sourcePredecessorIndex;
    }


    protected HashSet<Vertex> initCoreSet(Vertex s, int level, HashMap<Vertex, Vertex> predecessorIndex) {
        return super.initCoreSet(s,level,predecessorIndex);
    }

    @Override
    protected int getCoreLevel(Vertex s , Vertex t) {
    	return super.getCoreLevel(s, t);
    }

    @Override
    protected Path getPathForCoreVertex(Vertex coreVertex, Vertex s, Vertex t) {
        return super.getPathForCoreVertex(coreVertex, s, t);
    }
}
