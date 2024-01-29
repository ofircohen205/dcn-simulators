package edu.asu.emit.algorithm.utils;

import edu.asu.emit.algorithm.graph.Vertex;

public class IndexTieBreaker extends VertexWeightTieBreaker {

	@Override
	public int breakTie(Vertex v, Vertex u) {
		// TODO Auto-generated method stub
		return Integer.compare(v.getId(), u.getId());
	}

}
