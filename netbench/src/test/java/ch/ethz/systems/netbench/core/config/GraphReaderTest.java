package ch.ethz.systems.netbench.core.config;

import edu.asu.emit.algorithm.graph.Graph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class GraphReaderTest {

    @Test
    public void testSimpleGraph() throws IOException {

        // 0 = 1 = 2 = 3 = 4
        Pair<Graph, GraphDetails> result = constructGraph(
                5,
                8,
                "set(0, 1, 3, 4)",
                "set(2)",
                "set(0, 1,3, 4)",
                "0 1\n1 0\n1 2\n2 1\n2 3\n3 2\n3 4\n4 3"
        );

        GraphDetails details = result.getRight();
        assertEquals(details.getNumNodes(), 5);
        assertEquals(details.getNumEdges(), 8);
        assertEquals(createSet(0, 1, 3, 4), details.getServerNodeIds());
        assertEquals(createSet(2), details.getSwitchNodeIds());
        assertEquals(createSet(0, 1, 3, 4), details.getTorNodeIds());
        assertEquals(4, details.getNumServers());
        assertEquals(1, details.getNumSwitches());
        assertEquals(4, details.getNumTors());
        assertFalse(details.isAutoExtended());

    }

    public static Set<Integer> createSet(Integer... integers) {
        Set<Integer> res = new HashSet<>();
        Collections.addAll(res, integers);
        return res;
    }

    public Pair<Graph, GraphDetails> constructGraph(
            int numNodes,
            int numEdges,
            String servers,
            String switches,
            String tors,
            String edges
    ) throws IOException {

        // Create temporary files
        File tempConfig = File.createTempFile("temp-run-config", ".tmp");
        File tempTopology = File.createTempFile("topology", ".tmp");

        // Write temporary config file
        BufferedWriter topologyWriter = new BufferedWriter(new FileWriter(tempTopology));
        topologyWriter.write("# A comment line followed by a white line\n\n");
        topologyWriter.write("|V|=" + numNodes + "\n");
        topologyWriter.write("|E|=" + numEdges + "\n");
        topologyWriter.write("Servers=" + servers + "\n");
        topologyWriter.write("Switches=" + switches + "\n");
        topologyWriter.write("ToRs=" + tors + "\n");
        topologyWriter.write(edges);
        topologyWriter.close();

        // Write temporary config file
        BufferedWriter configWriter = new BufferedWriter(new FileWriter(tempConfig));
        configWriter.write("scenario_topology_file=" + tempTopology.getAbsolutePath().replace("\\", "/"));
        configWriter.close();

        // Create properties
        NBProperties properties = new NBProperties(tempConfig.getAbsolutePath(), BaseAllowedProperties.PROPERTIES_RUN);

        Pair<Graph, GraphDetails> res = new ImmutablePair<>(properties.getGraph(), properties.getGraphDetails());

        // Clean-up
        assertTrue(tempConfig.delete());
        assertTrue(tempTopology.delete());

        return res;

    }

    @Test
    public void readBigGraphTest() {
        FileReader input = null;
        try {
            input = new FileReader("example/topologies/fat_tree/fat_tree_k100.topology");
            BufferedReader br = new BufferedReader(input);
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }

                // Check links
                int index = line.indexOf("=");
                if (index == -1) {
                    String[] spl = line.split(" ");
                    int srcId = Integer.valueOf(spl[0]);
                    int dstId = Integer.valueOf(spl[1]);


                }
                if (lineNum % 10000 == 0) {
                    System.out.println("on line " + lineNum);
                }
                lineNum++;
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
