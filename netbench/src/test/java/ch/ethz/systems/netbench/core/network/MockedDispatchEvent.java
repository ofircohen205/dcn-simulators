package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;

public class MockedDispatchEvent extends PacketDispatchedEvent {
	OutputPort dispatchPort;
	public MockedDispatchEvent(long timeFromNowNs, Packet packet, OutputPort dispatchPort) {
		super(timeFromNowNs, packet, dispatchPort);
		this.dispatchPort = dispatchPort;
	}
	
	protected NetworkDevice getOwnDevice() {
		return BaseInitializer.getInstance().getIdToNetworkDevice().get(this.deviceId);
    }
    
    protected OutputPort getOutputPort(NetworkDevice nd){
    	return dispatchPort;
    }
    
    

}
