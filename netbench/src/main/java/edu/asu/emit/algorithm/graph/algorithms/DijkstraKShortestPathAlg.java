package edu.asu.emit.algorithm.graph.algorithms;

import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import edu.asu.emit.algorithm.graph.BaseGraph;
import edu.asu.emit.algorithm.graph.EqualCostPaths;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;
import edu.asu.emit.algorithm.graph.Vertex;

public class DijkstraKShortestPathAlg extends DijkstraShortestPathAlg {
	private PriorityQueue<EqualCostPaths> foundPaths;
	private HashMap<Integer,EqualCostPaths> visitedNodes;
	private int K;
	private double minDistance;
	EqualCostPaths endPaths;
	double maxPathWeight;
	public DijkstraKShortestPathAlg(BaseGraph graph,int k, double max_weigh,String vertexShuffleKey) {
		super(graph, max_weigh,vertexShuffleKey);
		K = k;
		maxPathWeight = max_weigh;
		visitedNodes = new HashMap<Integer,EqualCostPaths>();
		endPaths = new EqualCostPaths(K);
		foundPaths = new PriorityQueue<EqualCostPaths>(new Comparator<EqualCostPaths>() {
			Random rand = new Random();
			@Override
			public int compare(EqualCostPaths p1, EqualCostPaths p2) {
				if(p1.getWeight() > p2.getWeight()) {
					return 1;
				}
				if(p1.getWeight() < p2.getWeight()) {
					return -1;
				}
				//int r =  rand.nextInt(2);
				//return r == 0 ? -1 : 1;
				return 0;
			}
		});
		minDistance = Graph.DISCONNECTED;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void clear() {
		super.clear();
		foundPaths.clear();
		endPaths.clear();
		visitedNodes.clear();
		minDistance = Graph.DISCONNECTED;
	}
	
	@Override
	public Paths getShortestPath(Vertex sourceVertex, Vertex sinkVertex) {
		determineShortestPaths(sourceVertex, sinkVertex, true);
		return endPaths;
		
	}
	
	@Override
	protected void determineShortestPaths(Vertex sourceVertex,
			Vertex sinkVertex, boolean isSource2sink)	{
		// 0. clean up variables
		clear();

		// 1. initialize members
		Vertex endVertex = isSource2sink ? sinkVertex : sourceVertex;
		Vertex startVertex = isSource2sink ? sourceVertex : sinkVertex;
		Path startPath = new Path(new LinkedList<Vertex>(Arrays.asList(startVertex)),0);
		EqualCostPaths startPaths = new EqualCostPaths(startPath,0,K);
		foundPaths.add(startPaths);
		visitedNodes.put(startVertex.getId(),startPaths);
		
		visitedNodes.put(endVertex.getId(), endPaths);
		while (!foundPaths.isEmpty() && endPaths.getPaths().size() < K) {
			EqualCostPaths curCandidate = foundPaths.poll();
			if(curCandidate.getWeight() > endPaths.getWeight()) {
				break;
			}

			updatePath(curCandidate, isSource2sink);
		}
	}
	
	private void updatePath(EqualCostPaths curCandidate, boolean isSource2sink) {
		List<Vertex> neighborVertexList = getVertexNeighbours(curCandidate.getLastVertex(), isSource2sink);
		neighborVertexList.sort(new Comparator<Vertex>() {

			@Override
			public int compare(Vertex v1, Vertex v2) {
				// TODO Auto-generated method stub
				return v1.compareTo(v2);
			}
		});
		//System.out.println("neighbouring vertices " + neighborVertexList);
		for (Vertex curAdjacentVertex : neighborVertexList) {
			if(graph.getEdgeCapacity(curCandidate.getLastVertex(), curAdjacentVertex) == 0) {
				
				continue;
			}
			double edgeWeight = graph.getEdgeWeight(curCandidate.getLastVertex(), curAdjacentVertex);

			if(curCandidate.getWeight() + edgeWeight > maxPathWeight) {
				continue;
			}
			EqualCostPaths pathsToNode = visitedNodes.get(curAdjacentVertex.getId());

			if(pathsToNode!=null) {
				if(pathsToNode.getWeight()<curCandidate.getWeight()+edgeWeight) {
					continue;
				}
			}
			//System.out.println("adding path " + curCandidate);
			EqualCostPaths newPaths = new EqualCostPaths(curCandidate);
			newPaths.addVertex(curAdjacentVertex,edgeWeight);
			if(pathsToNode!=null) {
				pathsToNode.addPaths(newPaths);
			}else {
				visitedNodes.put(curAdjacentVertex.getId(),newPaths);
				foundPaths.add(newPaths);
			}
		
		}
		
	}
}
