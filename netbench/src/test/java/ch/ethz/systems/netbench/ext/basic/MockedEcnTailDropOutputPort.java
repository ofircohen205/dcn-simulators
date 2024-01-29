package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.*;

public class MockedEcnTailDropOutputPort extends EcnTailDropOutputPort {

	MockedEcnTailDropOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link,
			long maxQueueSizeBytes, long ecnThresholdKBytes) {
		super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void registerPacketDispatchedEvent(Packet packet) {
		Simulator.registerEvent(new MockedDispatchEvent(
                packet.getSizeBit() / link.getBandwidthBitPerNs(),
                packet,
                this
        ));
	}
	
	@Override
	protected void registerPacketArrivalEvent(Packet packet) {
		Simulator.registerEvent(
                new MockedArivalEvent(
                        link.getDelayNs(),
                        packet,
                        new InputPort(targetNetworkDevice,ownNetworkDevice, link)
                )
        );
	}

}
