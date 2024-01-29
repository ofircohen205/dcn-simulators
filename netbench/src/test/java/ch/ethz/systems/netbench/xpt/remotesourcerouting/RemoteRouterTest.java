package ch.ethz.systems.netbench.xpt.remotesourcerouting;


import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.run.routing.remote.LightOutputPortGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingTransportLayerGenerator;
import ch.ethz.systems.netbench.testutility.TestTopologyPortsConstruction;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.FlowPathExists;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.ext.demo.DemoIntermediaryGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemoteRouterTest {

    /*
     * Topology:
     * 0---1---2
     *     |   |
     *     |   |
     *     3---4
     */
    private TestTopologyPortsConstruction topology;

    @Mock
    private TcpPacket packet;
    NBProperties conf;
    private File tempRunConfig;
    private MockRemoteRouter remoteRouter;
    private Map<Integer, NetworkDevice> idToNetworkDevice;
    @Before
    public void setup() throws IOException {

        // Create temporary run configuration file
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n5.topology\n");
        runConfigWriter.write("centered_routing_type=Xpander\n");
        runConfigWriter.write("dijkstra_vertex_shuffle=false\n");
        runConfigWriter.write("circuit_wave_length_num=1\n");
        runConfigWriter.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter.write("output_port_ecn_threshold_k_bytes=30000\n");

        runConfigWriter.close();
        conf = new NBProperties(
                tempRunConfig.getAbsolutePath(),
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXPERIMENTAL);
        Simulator.setup(0, conf);
        BaseInitializer.getInstance().extend(new LightOutputPortGenerator(conf), new RemoteSourceRoutingSwitchGenerator( new DemoIntermediaryGenerator(conf), 5, conf),
				new PerfectSimpleLinkGenerator(0,10), new RemoteRoutingTransportLayerGenerator(conf));
        BaseInitializer b = BaseInitializer.getInstance() ;
        b.createInfrastructure(conf);
        b.finalize();
        idToNetworkDevice = b.getIdToNetworkDevice();
        this.remoteRouter = spy(new MockRemoteRouter(idToNetworkDevice,conf));
    }

    @After
    public void cleanup() {
        Simulator.reset();
        assertTrue(tempRunConfig.delete());
    }

    @Test
    public void testRemoteRoutingInitilization() {
    	RoutingSelector.selectPopulator(idToNetworkDevice, conf);
    	assertTrue(RemoteRoutingController.getInstance() != null);
    }
    
    

    @Test
    public void testAddRemovePaths() {
    	RemoteSourceRoutingSwitch device =(RemoteSourceRoutingSwitch) idToNetworkDevice.get(1);
    	remoteRouter.initRoute(1, 4,0);
    	boolean thrown = false;
    	try{
			remoteRouter.initRoute(1, 4,0);
		}catch(FlowPathExists e){
			thrown = true;
		}

    	assert(device.getNextHop(0) != null);
		assert(thrown);


    	System.out.println("recovering path one");
    	remoteRouter.recoverPath(1,4, 0);
//        remoteRouter.recoverPath(1,4, 0);
    	remoteRouter.initRoute(1, 4,3);
    	thrown = false;
    	try{
    		remoteRouter.initRoute(1, 3,4);
    		remoteRouter.initRoute(1, 2,4);
    	}catch(NoPathException e){
    		thrown = true;
    	}
    	assert(thrown);

    	System.out.println("reseting all paths");
    	remoteRouter.reset();
    	remoteRouter.initRoute(1, 4,0);
    	remoteRouter.initRoute(1, 2,1);
		assert(device.getNextHop(0) != null);
		assert(device.getNextHop(1) != null);
    	
    }
    
    @Test
    public void testSwitchPath() {
        //not sure this test is true
    	/*remoteRouter.initRoute(1, 4,0);
    	Path oldP = remoteRouter.getPath(1,4);
    	remoteRouter.switchPath(1, 4, remoteRouter.generatePathFromGraph(1, 4), 0);
    	Path newP = remoteRouter.getPath(1,4);
    	assert(!oldP.equals(newP));
    	assert(newP.getVertexList().get(0).getId()==1);
    	assert(newP.getVertexList().get(newP.getVertexList().size()-1).getId()==4);*/

    	
    }




}