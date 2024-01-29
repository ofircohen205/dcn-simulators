package edu.asu.emit.algorithm.graph.algorithms;


import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Graph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class BFSTest {

    BFS bfs;
    Graph g;

    @Before
    public void setup() throws IOException {
        //creating network 2:
        File tempRunConfig2 = File.createTempFile("temp-run-config2", ".tmp");
        BufferedWriter runConfigWriter2 = new BufferedWriter(new FileWriter(tempRunConfig2));
        //runConfigWriter.write("network_device=hybrid_optic_electronic\n");
        runConfigWriter2.write("scenario_topology_file=example/topologies/simple/simple_n6_v2.topology\n");

        runConfigWriter2.close();
        NBProperties conf2 = new NBProperties(
                tempRunConfig2.getAbsolutePath(),
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL,
                BaseAllowedProperties.BASE_DIR_VARIANTS
        );
        g = conf2.getGraph();
        bfs = new BFS(conf2.getGraph());
    }

    @Test
    public void testCorrectDistance() {
        assert (bfs.getDistance(0, 1) == 2);
        assert (bfs.getDistance(2, 4) == 1);
        assert (bfs.getDistance(2, 2) == 0);
        g.decreaseCapacity(new ImmutablePair<>(2, 4));
        assert (bfs.getDistance(2, 4) == 2);
        g.increaseCapacity(new ImmutablePair<>(2, 4));
        assert (bfs.getDistance(2, 4) == 1);
    }

    @Test
    public void testNoPath() {
        boolean thrown = false;
        g.decreaseCapacity(new ImmutablePair<>(2, 3));
        g.decreaseCapacity(new ImmutablePair<>(1, 3));
        try {
            bfs.getDistance(0, 3);
        } catch (NoPathException e) {
            thrown = true;
        }
        assert (thrown);

    }
}
