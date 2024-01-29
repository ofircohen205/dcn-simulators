package edu.asu.emit.algorithm.graph.paths_filter;

import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;
import edu.asu.emit.algorithm.graph.Vertex;

public class LeastLoadedPath extends PathsFilter {
	BaseInitializer baseInitiallizer;

	public LeastLoadedPath(Graph g) {
		super(g);
		baseInitiallizer = BaseInitializer.getInstance();

	}

	@Override
	public Path filterPaths(Paths paths) {
		long weight = 0l;
		Path ret = new Path(0);
		for(Path p : paths.getPaths()) {
			long tmp = 0l;
			for(Vertex v : p.getVertexList()) {
				for(Vertex u : G.getAdjacentVertices(v)) {
					if(baseInitiallizer.getNetworkDeviceById(v.getId()).isServer() || baseInitiallizer.getNetworkDeviceById(u.getId()).isServer()) {
						continue;
					}
					long tmpCapacity = G.getEdgeCapacity(v, u);
					tmp+= tmpCapacity;
				}
				
				
			}
			if(weight < tmp) {
				weight = tmp;
				ret = p;
			}
		}
		return ret;
	}

}
