package edu.asu.emit.algorithm.graph.paths_filter;

import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;

public abstract class PathsFilter {
	Graph G;
	public PathsFilter(Graph g) {
		G = g;
	}
	
	public abstract Path filterPaths(Paths paths);
	
	
}
