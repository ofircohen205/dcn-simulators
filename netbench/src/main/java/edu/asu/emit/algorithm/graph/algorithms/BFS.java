package edu.asu.emit.algorithm.graph.algorithms;

import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Vertex;

import java.util.*;

public class BFS {

    private final Graph mGraph;

    public BFS(Graph graph){
        this.mGraph = graph;
    }

    public int getDistance(int source, int dest){
        HashSet<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        HashMap<Integer,Integer> distances = new HashMap<>();
        int curr = source;
        distances.put(curr,0);
        visited.add(curr);
        while(curr != dest){
            Vertex currVertex = new Vertex(curr);
            List<Vertex> neighbours = mGraph.getAdjacentVertices(currVertex);
            for(Vertex v: neighbours){
                if(mGraph.getEdgeCapacity(currVertex,v)==0) continue;;

                if(!visited.contains(v.getId())){
                    distances.put(v.getId(),distances.get(curr)+1);
                    queue.add(v.getId());
                    visited.add(v.getId());
                }
            }
            if(!queue.isEmpty()){
                curr = queue.poll();
            }else {
                throw new NoPathException();
            }
        }
        return distances.get(curr);
    }
}
