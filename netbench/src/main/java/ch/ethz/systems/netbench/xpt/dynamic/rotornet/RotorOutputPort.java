package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.InputPort;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicOutuptPort;

public class RotorOutputPort extends DynamicOutuptPort {
	private final long mMaxBitsPerInterval;
	RotorSwitch mOriginalDevice;
	RotorMap mRotorMap;
	private long mBitsEnqueued;
	private long mDispatchTime;
	int mPacketSentCounter;

	protected RotorOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link,
			long maxQueueSizeBytes, long ecnThresholdKBytes) {
		super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
		mOriginalDevice = (RotorSwitch) ownNetworkDevice;
		this.ownNetworkDevice = ownNetworkDevice;
		mPacketSentCounter = 0;
		mBitsEnqueued = 0;
		mMaxBitsPerInterval = ((RotorNetController) RemoteRoutingController.getInstance()).mReconfigurationInterval * link.getBandwidthBitPerNs();
	}

	public RotorSwitch getOriginalDevice(){
		return this.mOriginalDevice;
	}
	
	@Override
	protected InputPort getTargetInputPort() {
		return new InputPort(targetNetworkDevice,ownNetworkDevice,link);
	}

	public void setRotorMap(RotorMap rotorMap) {
		this.mRotorMap = rotorMap;
	}

	/**
	 * only send if the next reconfiguration event is not closer then the packet transmission time
	 * @param packet    Packet instance
	 */
	@Override
	public void enqueue(Packet packet) {
		if(mBitsEnqueued + packet.getSizeBit() > mMaxBitsPerInterval){
			onConfigurationTimeExceeded();
		}
		mPacketSentCounter++;
		mBitsEnqueued +=packet.getSizeBit();
		super.enqueue(packet);
	}

	protected void onConfigurationTimeExceeded() {
		throw new ReconfigurationDeadlineException();
	}

	@Override
	protected long getDispatchTime(Packet packet){
		long time = packet.getSizeBit() / link.getBandwidthBitPerNs();
		mDispatchTime = Simulator.getTimeFromNow(time);

		return time;
	}

	@Override
	protected void log(Packet packet) {

	}


}
