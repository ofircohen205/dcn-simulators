package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;

public class InputPort extends Port {
	int encapsulatingDeviceId;
	NetworkDevice sourceNetworkDevice;
	protected Link link;
	public InputPort(NetworkDevice ownNetworkDevice, NetworkDevice sourceNetworkDevice, Link link) {
		this.ownNetworkDevice = ownNetworkDevice;
		this.sourceNetworkDevice = sourceNetworkDevice;
		this.link = link;
	}
	
	public NetworkDevice getOwnNetworkDevice() {
		return this.ownNetworkDevice;
	}
	
	public NetworkDevice getSourceNetworkDevice() {
		return this.sourceNetworkDevice;
	}

	/**
	 * registers an arrival event of packet
	 * @param packet
	 */
    public void registerPacketArrivalEvent(Packet packet) {
		Simulator.registerEvent(
				new PacketArrivalEvent(
						link.getDelayNs(),
						packet,
						this
                )
        );
    }


    /**
     * receive a packet for preprocessing before moving to device
     * @param packet
     */
	public void receive(Packet packet) {
		if(ownNetworkDevice.getEncapsulatingDevice()!=null) {
			/**
			 * check if the encapsulating device will handle the packet 
			 */
			if(ownNetworkDevice.getEncapsulatingDevice().hadlePacketFromEncapsulating(packet)){
				return;
			}
			ownNetworkDevice.receiveFromEncapsulating(packet);
			return;
		}

		ownNetworkDevice.receive(packet);
	}
}
