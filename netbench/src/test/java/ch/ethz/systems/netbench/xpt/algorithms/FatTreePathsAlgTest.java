package ch.ethz.systems.netbench.xpt.algorithms;

import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.config.GraphReader;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;
import edu.asu.emit.algorithm.utils.VertexWeightTieBreaker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class FatTreePathsAlgTest {
    Graph g;
    MockedFatTreeAlg alg;
    @Before
    public void setup() throws IOException {
        
        Vertex.setTieBreaker(new VertexWeightTieBreaker() {
            @Override
            public int breakTie(Vertex vertex, Vertex vertex1) {
                return 0;
            }
        });
        
    }

    @Test
    public void testGetPaths(){
        Pair<Graph, GraphDetails> result = GraphReader.read("example/topologies/fat_tree/fat_tree_k4.topology");
        g = result.getLeft();
        alg = new MockedFatTreeAlg(g,4);
        assertEquals("[0, 8, 17, 14, 7]:4.0",alg.getShortestPath(new Vertex(0),new Vertex(7)).getPaths().get(1).toString());
        assertEquals("[0, 9, 1]:2.0",alg.getShortestPath(new Vertex(0),new Vertex(1)).getPaths().get(1).toString());
        assertEquals("[0]:0.0",alg.getShortestPath(new Vertex(0),new Vertex(0)).getPaths().get(0).toString());

        g.decreaseCapacity(new ImmutablePair<Integer, Integer>(0,8));
        g.decreaseCapacity(new ImmutablePair<Integer, Integer>(0,9));
        assertEquals(0,alg.getShortestPath(new Vertex(0),new Vertex(7)).getPaths().size());
    }

    @Test
    public void testCorrectPathGeneration(){
        Pair<Graph, GraphDetails> result = GraphReader.read("example/topologies/fat_tree/fat_tree_k8.topology");
        g = result.getLeft();
        alg = new MockedFatTreeAlg(g,8);
        Vertex s = new Vertex(5);
        Vertex t = new Vertex(20);
        HashSet<Vertex> sourceCores = alg.initCoreSet(s,2,alg.sourcePredecessorIndex);
        HashSet<Vertex> destCores = alg.initCoreSet(t,2,alg.destPredecessorIndex);
        Path p = alg.getPathForCoreVertex(new Vertex(73),s,t);
        assertEquals(p.toString(),"[5, 38, 73, 54, 20]:4.0");

        s = new Vertex(5);
        t = new Vertex(6);
        sourceCores = alg.initCoreSet(s,1,alg.sourcePredecessorIndex);
        destCores = alg.initCoreSet(t,1,alg.destPredecessorIndex);
        p = alg.getPathForCoreVertex(new Vertex(37),s,t);
        assertEquals(p.toString(),"[5, 37, 6]:2.0");

        s = new Vertex(5);
        t = new Vertex(5);
        sourceCores = alg.initCoreSet(s,0,alg.sourcePredecessorIndex);
        destCores = alg.initCoreSet(t,0,alg.destPredecessorIndex);
        p = alg.getPathForCoreVertex(new Vertex(5),s,t);
        assertEquals(p.toString(),"[5]:0.0");
    }

    @Test
    public void tetsCorrectCoreLevel() {
    	Pair<Graph, GraphDetails> result = GraphReader.read("example/topologies/fat_tree/fat_tree_k8.topology");
    	g = result.getLeft();
        alg = new MockedFatTreeAlg(g,8);
        assertEquals(1, alg.getCoreLevel(new Vertex(5), new Vertex(6)));
        assertEquals(2, alg.getCoreLevel(new Vertex(5), new Vertex(18)));
        assertEquals(0,alg.getCoreLevel(new Vertex(5), new Vertex(5)));
    }

    @Test
    public void testPathFromVToLevel(){
    	Pair<Graph, GraphDetails> result = GraphReader.read("example/topologies/fat_tree/fat_tree_k4.topology");
        g = result.getLeft();
        alg = new MockedFatTreeAlg(g,4);
        HashMap<Vertex,Vertex> predecessors = new HashMap<>();
        Set<Vertex> coreSet = alg.initCoreSet(new Vertex(5),2,predecessors);
        assert(Arrays.toString(coreSet.toArray()).equals("[16, 17, 18, 19]"));
        assert(Arrays.toString(predecessors.entrySet().toArray()).equals("[16=12, 17=12, 18=13, 19=13, 12=5, 13=5]"));
        
        predecessors.clear();
        coreSet = alg.initCoreSet(new Vertex(5),1,predecessors);
        //System.out.println(Arrays.toString(predecessors.entrySet().toArray()));
        assert(Arrays.toString(coreSet.toArray()).equals("[12, 13]"));
        assert(Arrays.toString(predecessors.entrySet().toArray()).equals("[12=5, 13=5]"));
        
        predecessors.clear();
        coreSet = alg.initCoreSet(new Vertex(5),0,predecessors);
        assert(Arrays.toString(coreSet.toArray()).equals("[5]"));
        
        result = GraphReader.read("example/topologies/fat_tree/fat_tree_k8.topology");
        g = result.getLeft();
        alg = new MockedFatTreeAlg(g,8);
        predecessors = new HashMap<>();
        coreSet = alg.initCoreSet(new Vertex(5),2,predecessors);
       // System.out.println(Arrays.toString(predecessors.entrySet().toArray()));
       // System.out.println(Arrays.toString(coreSet.toArray()));
        assert(Arrays.toString(coreSet.toArray()).equals("[64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79]"));
        assert(Arrays.toString(predecessors.entrySet().toArray()).equals("[64=36, 65=36, 66=36, 67=36, 36=5, 68=37, 37=5, 69=37, 38=5, 70=37, 39=5, 71=37, 72=38, 73=38, 74=38, 75=38, 76=39, 77=39, 78=39, 79=39]"));
        
        predecessors.clear();
        coreSet = alg.initCoreSet(new Vertex(5),1,predecessors);
       // System.out.println(Arrays.toString(predecessors.entrySet().toArray()));
       // System.out.println(Arrays.toString(coreSet.toArray()));
        assert(Arrays.toString(coreSet.toArray()).equals("[36, 37, 38, 39]"));
        assert(Arrays.toString(predecessors.entrySet().toArray()).equals("[36=5, 37=5, 38=5, 39=5]"));
        
        predecessors.clear();
        
        coreSet = alg.initCoreSet(new Vertex(5),0,predecessors);
        //System.out.println(Arrays.toString(coreSet.toArray()));
        assert(Arrays.toString(coreSet.toArray()).equals("[5]"));



    }

}
