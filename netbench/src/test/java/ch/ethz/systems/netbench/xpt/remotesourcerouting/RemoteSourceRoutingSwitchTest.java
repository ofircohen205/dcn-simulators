package ch.ethz.systems.netbench.xpt.remotesourcerouting;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.RoutingSelector;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.run.routing.remote.LightOutputPortGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingPacket;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingTransportLayerGenerator;
import ch.ethz.systems.netbench.ext.flowlet.IdentityFlowletIntermediary;
import ch.ethz.systems.netbench.testutility.TestTopologyPortsConstruction;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
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
public class RemoteSourceRoutingSwitchTest {

    /*
     * Topology:
     * 0---1---2
     *     |   |
     *     |   |
     *     3---4
     */
    private TestTopologyPortsConstruction topology;
    private RemoteRoutingController remoteRouter;
    @Mock()
    private RemoteRoutingPacket packet;

    private File tempRunConfig;
    private Map<Integer, NetworkDevice> realIdToNetworkDevice;
    private Map<Integer, NetworkDevice> mockIdToNetworkDevice;
    @Before
    public void setup() throws IOException {

        // Create temporary run configuration file
        tempRunConfig = File.createTempFile("temp-run-config", ".tmp");
        BufferedWriter runConfigWriter = new BufferedWriter(new FileWriter(tempRunConfig));
        runConfigWriter.write("scenario_topology_file=example/topologies/simple/simple_n5.topology\n");
        runConfigWriter.write("centered_routing_type=Xpander\n");
        runConfigWriter.write("network_device_routing=remote_routing_populator\n");
        runConfigWriter.write("circuit_wave_length_num=1\n");
        runConfigWriter.write("output_port_max_queue_size_bytes=150000\n");
        runConfigWriter.write("output_port_ecn_threshold_k_bytes=30000\n");

        runConfigWriter.close();
        NBProperties conf = new NBProperties(
                tempRunConfig.getAbsolutePath(),
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXPERIMENTAL
        );
        
        Simulator.setup(0,conf);
        NetworkDeviceGenerator generator = new NetworkDeviceGenerator(conf) {
        	DemoIntermediaryGenerator inermediaryGenerator = new DemoIntermediaryGenerator(configuration);
			@Override
			public NetworkDevice generate(int identifier, TransportLayer server) {
				// TODO Auto-generated method stub
				return new MockRemoteRoutingSwitch(identifier, server, inermediaryGenerator.generate(identifier));
			}
			
			@Override
			public NetworkDevice generate(int identifier) {
				// TODO Auto-generated method stub
				return generate(identifier,null);
			}
		};
        BaseInitializer.getInstance().extend( new LightOutputPortGenerator(conf), new RemoteSourceRoutingSwitchGenerator( new DemoIntermediaryGenerator(conf), 5, conf),
                new PerfectSimpleLinkGenerator(0,10), new RemoteRoutingTransportLayerGenerator(conf));
        BaseInitializer b = BaseInitializer.getInstance() ;

        b.createInfrastructure(conf);
        b.finalize();
        realIdToNetworkDevice = b.getIdToNetworkDevice();
        
        mockIdToNetworkDevice = (Map<Integer, NetworkDevice>) mock(Map.class);
        for(int i = 0;i<realIdToNetworkDevice.size();i++) {
        	NetworkDevice device = spy(realIdToNetworkDevice.get(i));
        	
        	when(mockIdToNetworkDevice.get(i)).thenReturn(device);
        }
        
        RoutingSelector.selectPopulator(mockIdToNetworkDevice, conf);
        this.remoteRouter = RemoteRoutingController.getInstance();
        topology = new TestTopologyPortsConstruction(
                "0-1,1-2,2-4,4-3,3-1"
        );
    }

    @After
    public void cleanup() {
        Simulator.reset();
        assertTrue(tempRunConfig.delete());
    }

    /**
     * tests the package is received in the source and is transfered correctly twice
     */
    @Test
    public void testDoubleForward() {

    	// extend source
        RemoteSourceRoutingSwitch source = (RemoteSourceRoutingSwitch) mockIdToNetworkDevice.get(0);
        // extend next
        RemoteSourceRoutingSwitch next = (RemoteSourceRoutingSwitch) mockIdToNetworkDevice.get(1);

        // Initialize packet for that destination
        when(packet.getDestinationId()).thenReturn(4);
        when(packet.getFlowId()).thenReturn((long) 0);
        when(packet.deEncapsualte()).thenReturn(packet);
        source.forwardingTable.put(-1L,source.getTargetOuputPort(next.getIdentifier()));
        // Give device the packet
        source.receiveFromTransportLayer(packet);
        verify(source,times(1)).receive(packet);
        verify(source,times(1)).forwardToNextSwitch(packet);
        next.forwardingTable.put(-1L,next.getTargetOuputPort(2));
        next.receive(packet);
        verify(next,times(1)).forwardToNextSwitch(packet);
    }
    
    @Test
    public void testPacketAcceptance() {

    	// extend source
        RemoteSourceRoutingSwitch dest = (RemoteSourceRoutingSwitch) mockIdToNetworkDevice.get(4);
    

        // Initialize packet for that destination
        when(packet.getDestinationId()).thenReturn(4);
        when(packet.getFlowId()).thenReturn((long) 0);
        

        dest.receive(packet);
        verify(dest,times(0)).forwardToNextSwitch(packet);
    }

    @Test
    public void testPassToTransportLayer() {

        

        // Create device 4 with ports 4->2 and 4->3
    	TransportLayer transportLayer = mock(TransportLayer.class);

        // Create device 4 with ports 4->2 and 4->3
        RemoteSourceRoutingSwitch device = new RemoteSourceRoutingSwitch(4, transportLayer, new IdentityFlowletIntermediary(null), null);
        

        // Create encapsulation and hop it two times (such that it "arrives" at 4)
        when(packet.getDestinationId()).thenReturn(4);
        when(packet.getFlowId()).thenReturn((long) 0);
        // Give it to the network device at 4
        device.receive(packet);

        // Assert that it receives the packet itself
        verify(transportLayer, times(1)).receive(packet);
        verify(topology.getPort(4, 2), times(0)).enqueue(packet);
        verify(topology.getPort(4, 3), times(0)).enqueue(packet);
        
    }


    @Test
    public void testToString() {

        // Create device with ports
        RemoteSourceRoutingSwitch device = new RemoteSourceRoutingSwitch(1, null, new IdentityFlowletIntermediary(null), null);
        device.addConnection(topology.getPort(1, 0));
        device.addConnection(topology.getPort(1, 2));
        device.addConnection(topology.getPort(1, 3));

        System.out.println(device.toString());

    }


}