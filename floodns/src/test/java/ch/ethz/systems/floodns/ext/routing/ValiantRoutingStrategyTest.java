/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 snkas
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class ValiantRoutingStrategyTest {

    @Test
    public void valiantOnlyTors() throws IOException {

        // 0 - 1
        // |\ /|\
        // | 4 | 5
        // |/ \|/
        // 2 - 3
        Topology topology = constructTopology(
                6,
                10,
                "set()",
                "set(0, 1, 2, 3, 4, 5)",
                "set(0, 1, 3)",
                "set(0-1,1-3,2-3,0-2,0-4,1-4,2-4,3-4,1-5,3-5)",
                6
        );

        // Create strategy
        Set<Integer> valiantNodeIds = new HashSet<>();
        valiantNodeIds.add(2);
        valiantNodeIds.add(4);
        valiantNodeIds.add(5);

        Simulator simulator = new Simulator();

        // Assign acyclic path 2 -> 4 over valiant node 0 (should be shortened)
        Network network = topology.getNetwork();

        // Valiant nodes cannot be the ToRs chosen
        ValiantRoutingStrategy strategy = new ValiantRoutingStrategy(simulator, topology, valiantNodeIds, new Random(12345), false);

        // Endpoints are not ToRs
        boolean thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(2), network.getNode(3), 1000, -1);
            strategy.assignStartFlows(connection);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Endpoints are not ToRs
        thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(3), network.getNode(2), 1000, -1);
            strategy.assignStartFlows(connection);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // One of the three valiant nodes must be chosen
        int routeA = 0;
        int routeB = 0;
        int routeC = 0;
        for (int i = 0; i < 300; i++) {
            AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(0), network.getNode(3), 1000, -1));
            if (PathTestUtility.createAcyclicPath(network, "0-2-3").equals(path)) {
                routeA++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-3").equals(path)) {
                routeB++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-5-3").equals(path)) {
                routeC++;
            } else {
                fail();
            }
        }
        assertTrue(routeA >= 50);
        assertTrue(routeB >= 50);
        assertTrue(routeC >= 50);

        // ToR in valiant nodes but not allowed
        thrown = false;
        try {
            Set<Integer> valiantNodeIds2 = new HashSet<>();
            valiantNodeIds2.add(2);
            valiantNodeIds2.add(4);
            valiantNodeIds2.add(5);
            valiantNodeIds2.add(0);
            new ValiantRoutingStrategy(new Simulator(), topology, valiantNodeIds2, new Random(12345), false);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // ToR in valiant nodes and allowed
        Set<Integer> valiantNodeIds3 = new HashSet<>();
        valiantNodeIds3.add(2);
        valiantNodeIds3.add(4);
        valiantNodeIds3.add(5);
        valiantNodeIds3.add(0);
        new ValiantRoutingStrategy(new Simulator(), topology, valiantNodeIds3, new Random(12345), true);

    }

    @Test
    public void valiantWithServer() throws IOException {

        // 0 - 1        ... with 0, 1 and 3 having 2 servers each
        // |\ /|\
        // | 4 | 5
        // |/ \|/
        // 2 - 3
        Topology topology = constructTopology(
                12,
                16,
                "set(6,7,8,9,10,11)",
                "set(0, 1, 2, 3, 4, 5)",
                "set(0, 1, 3)",
                "set(0-1,1-3,2-3,0-2,0-4,1-4,2-4,3-4,1-5,3-5,0-6,0-7,1-8,1-9,3-10,3-11)",
                55
        );

        // Create strategy
        Set<Integer> valiantNodeIds = new HashSet<>();
        valiantNodeIds.add(2);
        valiantNodeIds.add(4);
        valiantNodeIds.add(5);

        Simulator simulator = new Simulator();

        // Assign acyclic path 2 -> 4 over valiant node 0 (should be shortened)
        Network network = topology.getNetwork();

        // Valiant nodes cannot be the ToRs chosen
        ValiantRoutingStrategy strategy = new ValiantRoutingStrategy(new Simulator(), topology, valiantNodeIds, new Random(12345), false);

        // Endpoints are not servers
        boolean thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(0), network.getNode(3), 1000, -1);
            strategy.assignStartFlows(connection);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // One of the three valiant nodes must be chosen
        int routeA = 0;
        int routeB = 0;
        int routeC = 0;
        for (int i = 0; i < 300; i++) {
            AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(6), network.getNode(11), 1000, -1));
            if (PathTestUtility.createAcyclicPath(network, "6-0-2-3-11").equals(path)) {
                routeA++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-4-3-11").equals(path)) {
                routeB++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-1-5-3-11").equals(path)) {
                routeC++;
            } else {
                fail();
            }
        }
        assertTrue(routeA >= 50);
        assertTrue(routeB >= 50);
        assertTrue(routeC >= 50);

        // ToRs are the only valiant nodes here
        Set<Integer> valiantNodeIds2 = new HashSet<>();
        valiantNodeIds2.add(0);
        valiantNodeIds2.add(1);
        valiantNodeIds2.add(3);
        ValiantRoutingStrategy strategy2 = new ValiantRoutingStrategy(new Simulator(), topology, valiantNodeIds2, new Random(12345), true);
        for (int i = 0; i < 100; i++) {
            AcyclicPath path = strategy2.assignSinglePath(new Connection(simulator, network.getNode(6), network.getNode(11), 1000, -1));
            assertEquals(PathTestUtility.createAcyclicPath(network, "6-0-1-3-11"), path);
        }

    }


    @Test
    public void simpleValiantPathReductionWithOne() throws IOException {

        //              0
        //              |
        //    1 -- 2 -- 3 -- 4
        //
        Topology topology = constructTopology(
                5,
                4,
                "set()",
                "set(0, 1, 2, 3, 4)",
                "set(0, 1, 4)",
                "set(0-3,1-2,2-3,3-4)",
                10.0
        );

        Set<Integer> valiantNodeIds = new HashSet<>();
        valiantNodeIds.add(0);

        // Create strategy
        Simulator simulator = new Simulator();
        ValiantRoutingStrategy strategy = new ValiantRoutingStrategy(new Simulator(), topology, valiantNodeIds, new Random(12345), true);

        // Assign acyclic path 1 -> 4 over valiant node 0 (should be shortened)
        Network network = topology.getNetwork();
        AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(1), network.getNode(4), 1000, -1));
        assertEquals(path, PathTestUtility.createAcyclicPath(network, "1-2-3-4"));

    }

}
