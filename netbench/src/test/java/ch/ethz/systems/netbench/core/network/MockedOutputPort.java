package ch.ethz.systems.netbench.core.network;

import java.util.Queue;

import ch.ethz.systems.netbench.core.Simulator;

public abstract class MockedOutputPort extends OutputPort {

	protected MockedOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link,
			Queue<Packet> queue) {
		super(ownNetworkDevice, targetNetworkDevice, link, queue);
		// TODO Auto-generated constructor stub
	}

	@Override
	public abstract void enqueue(Packet packet);
	
	@Override
	protected void registerPacketDispatchedEvent(Packet packet) {
		Simulator.registerEvent(new MockedDispatchEvent(
                packet.getSizeBit() / link.getBandwidthBitPerNs(),
                packet,
                this
        ));
	}
}
