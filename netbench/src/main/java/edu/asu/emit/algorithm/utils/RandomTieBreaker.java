package edu.asu.emit.algorithm.utils;

import java.util.Random;

import ch.ethz.systems.netbench.core.Simulator;
import edu.asu.emit.algorithm.graph.Vertex;

public class RandomTieBreaker extends VertexWeightTieBreaker {
    Random rand;
    public RandomTieBreaker() {
    	rand = new Random();
    }
	@Override
	public int breakTie(Vertex v, Vertex u) {
		// TODO Auto-generated method stub
		int r = rand.nextInt(2);
		return r==0 ? -1 : 1;
	}

}

