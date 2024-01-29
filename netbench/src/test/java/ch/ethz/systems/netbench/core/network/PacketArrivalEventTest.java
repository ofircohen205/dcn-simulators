package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PacketArrivalEventTest {

    @Mock
    private Packet packet;

    @Before
    public void setup() {
        Simulator.setup(0);
    }

    @After
    public void cleanup() {
        Simulator.reset();
    }

    @Test
    public void testTriggerIsolated() {
        NetworkDevice networkDevice = mock(NetworkDevice.class);
        when(networkDevice.getIdentifier()).thenReturn(1);
        NetworkDevice source = mock(NetworkDevice.class);
        when(source.getIdentifier()).thenReturn(0);

        InputPort ip = new InputPort(networkDevice,source, null);
        networkDevice.addIncomingConnection(ip);
        PacketArrivalEvent event = new MockedArivalEvent(1000, packet, ip);
        event.trigger();
        verify(networkDevice, times(1)).receive(packet);
    }

    @Test
    public void testTriggerInSimulation() {
        NetworkDevice networkDevice = mock(NetworkDevice.class);
        when(networkDevice.getIdentifier()).thenReturn(1);
        NetworkDevice source = mock(NetworkDevice.class);
        when(source.getIdentifier()).thenReturn(0);
        InputPort ip = new InputPort(networkDevice,source, null);

        PacketArrivalEvent event = new MockedArivalEvent(1000, packet, ip);
        Simulator.registerEvent(event);
        Simulator.runNs(2000);
        verify(networkDevice, times(1)).receive(packet);
    }

    @Test
    public void testTriggerInSimulationJustNot() {
        NetworkDevice networkDevice = mock(NetworkDevice.class);
        when(networkDevice.getIdentifier()).thenReturn(1);
        NetworkDevice source = mock(NetworkDevice.class);
        when(source.getIdentifier()).thenReturn(0);
        InputPort ip = new InputPort(networkDevice,source, null);

        PacketArrivalEvent event = new PacketArrivalEvent(1000, packet, ip);
        
        Simulator.registerEvent(event);
        Simulator.runNs(999);
        verify(networkDevice, times(0)).receive(packet);
    }

    @Test
    public void testTriggerInSimulationJust() {
        NetworkDevice networkDevice = mock(NetworkDevice.class);
        when(networkDevice.getIdentifier()).thenReturn(1);
        NetworkDevice source = mock(NetworkDevice.class);
        when(source.getIdentifier()).thenReturn(0);
        InputPort ip = new InputPort(networkDevice,source, null);

        PacketArrivalEvent event = new MockedArivalEvent(999, packet, ip);
        Simulator.registerEvent(event);
        Simulator.runNs(999);
        verify(networkDevice, times(1)).receive(packet);
    }

    @Test
    public void testToString() {

        NetworkDevice networkDevice = mock(NetworkDevice.class);
        when(networkDevice.getIdentifier()).thenReturn(1);
        NetworkDevice source = mock(NetworkDevice.class);
        when(source.getIdentifier()).thenReturn(0);
        InputPort ip = new InputPort(networkDevice,source, null);
        PacketArrivalEvent event = new PacketArrivalEvent(999, packet, ip);
        event.toString();
    }

}
