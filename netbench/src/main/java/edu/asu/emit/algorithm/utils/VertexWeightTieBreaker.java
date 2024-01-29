package edu.asu.emit.algorithm.utils;

import edu.asu.emit.algorithm.graph.Vertex;

public abstract class VertexWeightTieBreaker {
	public abstract int breakTie(Vertex v, Vertex u);
}
