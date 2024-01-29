package edu.asu.emit.algorithm.graph.paths_filter;

import java.util.Comparator;
import java.util.List;

import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;
import edu.asu.emit.algorithm.graph.Vertex;

public class LowestIndexFilter extends PathsFilterFirst {

	public LowestIndexFilter(Graph g) {
		super(g);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Path filterPaths(Paths paths) {
		paths.getPaths().sort(new Comparator<Path>() {



			@Override
			public int compare(Path p1, Path p2) {
				// TODO Auto-generated method stub
				List<Vertex> list1 = p1.getVertexList();
				List<Vertex> list2 = p2.getVertexList();
				if(list1.size() != list2.size()) {
					return list1.size() - list2.size();
				}
				for(int i = 0; i<list1.size();i++) {
					if(list1.get(i).getId() - list2.get(i).getId() != 0)
						return list1.get(i).getId() - list2.get(i).getId();
				}
				return 0;
			}
		});
		return super.filterPaths(paths);
	}

}
