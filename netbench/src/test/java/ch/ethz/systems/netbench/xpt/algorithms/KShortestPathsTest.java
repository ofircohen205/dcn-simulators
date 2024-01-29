package ch.ethz.systems.netbench.xpt.algorithms;


import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.config.GraphReader;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Paths;
import edu.asu.emit.algorithm.graph.Vertex;
import edu.asu.emit.algorithm.graph.algorithms.DijkstraKShortestPathAlg;
import edu.asu.emit.algorithm.utils.VertexWeightTieBreaker;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class KShortestPathsTest {
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
    public void testGetOnePath() {

        DijkstraKShortestPathAlg alg = new DijkstraKShortestPathAlg(g,1,1000,null);
        Paths ps = alg.getShortestPath(new MockedVertex(0),new MockedVertex(4));
        assert(ps.getPaths().size()==1);


    }

    @Test
    public void testGetTwoPath() {
        DijkstraKShortestPathAlg alg = new DijkstraKShortestPathAlg(g,2,1000,null);
        Paths ps = alg.getShortestPath(new MockedVertex(0),new MockedVertex(4));
        assert(ps.getPaths().size()==2);

    }

    @Test
    public void testCantGetThreePaths() {
        DijkstraKShortestPathAlg alg = new DijkstraKShortestPathAlg(g,5,1000,null);
        Paths ps = alg.getShortestPath(new MockedVertex(0),new MockedVertex(4));
        assert(ps.getPaths().size()==2);

    }

    @Test
    public void testCantPassWeight() {
        DijkstraKShortestPathAlg alg = new DijkstraKShortestPathAlg(g,2,2,null);
        Paths ps = alg.getShortestPath(new MockedVertex(0),new MockedVertex(4));
        assert(ps.getPaths().size()==0);

    }
}
