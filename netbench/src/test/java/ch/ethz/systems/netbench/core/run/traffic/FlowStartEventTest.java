package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.MockedFlowStartEvent;
import ch.ethz.systems.netbench.core.network.MockedNetworkDevice;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FlowStartEventTest {
	BaseInitializer baseInitializer;
	
    @Before
    public void setup() {
    	BaseInitializer.getInstance().extend(null, null, null, null);
    	baseInitializer = BaseInitializer.getInstance();
        Simulator.setup(0);
        
    }

    @After
    public void cleanup() {
        Simulator.reset();
    }

    @Test
    public void testTriggerIsolated() {
        TransportLayer transportLayer = mock(TransportLayer.class);
        NetworkDevice networkDevice = new MockedNetworkDevice(-1, transportLayer, mock(Intermediary.class));
        baseInitializer.getIdToNetworkDevice().put(-1, networkDevice);
        FlowStartEvent event = new MockedFlowStartEvent(1000, transportLayer, 98, 100000);
        event.trigger();
        verify(transportLayer, times(1)).startFlow(98, 100000);
    }

    @Test
    public void testTriggerInSimulation() {
        TransportLayer transportLayer = mock(TransportLayer.class);
        NetworkDevice networkDevice = new MockedNetworkDevice(-1, transportLayer, mock(Intermediary.class));
        baseInitializer.getIdToNetworkDevice().put(-1, networkDevice);
        FlowStartEvent event = new MockedFlowStartEvent(1000, transportLayer, 23, 56737);
        Simulator.registerEvent(event);
        Simulator.runNs(2000);
        verify(transportLayer, times(1)).startFlow(23, 56737);
    }

    @Test
    public void testTriggerInSimulationJustNot() {
        TransportLayer transportLayer = mock(TransportLayer.class);
        FlowStartEvent event = new MockedFlowStartEvent(1000, transportLayer, 98, 100000);
        Simulator.registerEvent(event);
        Simulator.runNs(999);
        verify(transportLayer, times(0)).startFlow(98, 100000);
    }

    @Test
    public void testTriggerInSimulationJust() {
        TransportLayer transportLayer = mock(TransportLayer.class);
        NetworkDevice networkDevice = new MockedNetworkDevice(-1, transportLayer, mock(Intermediary.class));
        baseInitializer.getIdToNetworkDevice().put(-1, networkDevice);
        FlowStartEvent event = new MockedFlowStartEvent(999, transportLayer, 98, 100000);
        Simulator.registerEvent(event);
        Simulator.runNs(999);
        verify(transportLayer, times(1)).startFlow(98, 100000);
    }

    @Test
    public void testToString() {
        TransportLayer transportLayer = mock(TransportLayer.class);
        FlowStartEvent event = new MockedFlowStartEvent(999, transportLayer, 98, 100000);
        event.toString();
    }

}
