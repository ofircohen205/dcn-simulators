package edu.asu.emit.algorithm.graph.paths_filter;

import java.util.NoSuchElementException;

import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;

public class PathsFilterFirst extends PathsFilter {

	public PathsFilterFirst(Graph g) {
		super(g);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Path filterPaths(Paths paths) {
		// TODO Auto-generated method stub
		try {
			return paths.getPaths().getFirst();
		}catch(NoSuchElementException e) {
			return new Path(0);
		}
		
	}

}
