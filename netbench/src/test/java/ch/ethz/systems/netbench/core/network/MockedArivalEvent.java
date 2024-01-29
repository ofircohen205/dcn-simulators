package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;

public class MockedArivalEvent extends PacketArrivalEvent {
	NetworkDevice arrivalNetworkDevice;
	public MockedArivalEvent(long timeFromNowNs, Packet packet, InputPort ip) {
		super(timeFromNowNs, packet, ip);
		this.arrivalNetworkDevice = arrivalNetworkDevice;
	}
	
	 protected NetworkDevice getNetworkDevice() {
		return arrivalNetworkDevice;
    	
    }

}
