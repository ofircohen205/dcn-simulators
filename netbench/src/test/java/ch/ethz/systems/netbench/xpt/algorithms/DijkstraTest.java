package ch.ethz.systems.netbench.xpt.algorithms;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.config.GraphReader;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Paths;
import edu.asu.emit.algorithm.graph.Vertex;
import edu.asu.emit.algorithm.graph.algorithms.DijkstraKShortestPathAlg;
import edu.asu.emit.algorithm.graph.algorithms.DijkstraShortestPathAlg;
import edu.asu.emit.algorithm.utils.VertexWeightTieBreaker;

@RunWith(MockitoJUnitRunner.class)
public class DijkstraTest {

	 Graph g;
	    @Before
	    public void setup() throws IOException {
	    	
	        Pair<Graph, GraphDetails> result = GraphReader.read("example/topologies/simple/simple_n5.topology");
	        g = result.getLeft();
	        Vertex.setTieBreaker(new VertexWeightTieBreaker() {
	            @Override
	            public int breakTie(Vertex vertex, Vertex vertex1) {
	                return 0;
	            }
	        });
	    }
	    
	    @Test
	    public void testCantPassWeight() {
	        DijkstraShortestPathAlg alg = new DijkstraShortestPathAlg(g,2,null);
	        Paths ps = alg.getShortestPath(new Vertex(0),new Vertex(4));
	        assert(ps.getPaths().get(0).getVertexList().size()==0);

	    }
	    
	    @Test
	    public void canFindPath() {
	        DijkstraShortestPathAlg alg = new DijkstraShortestPathAlg(g,3,null);
	        Paths ps = alg.getShortestPath(new Vertex(0),new Vertex(4));
	        assert(ps.getPaths().size()==1);

	    }
}
