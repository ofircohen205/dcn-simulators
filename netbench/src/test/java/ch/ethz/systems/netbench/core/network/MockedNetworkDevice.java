package ch.ethz.systems.netbench.core.network;

public class MockedNetworkDevice extends NetworkDevice {

	public MockedNetworkDevice(int identifier, TransportLayer transportLayer, Intermediary intermediary) {
		super(identifier, transportLayer, intermediary,null);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void receive(Packet genericPacket) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void receiveFromIntermediary(Packet genericPacket) {
		// TODO Auto-generated method stub

	}

}
