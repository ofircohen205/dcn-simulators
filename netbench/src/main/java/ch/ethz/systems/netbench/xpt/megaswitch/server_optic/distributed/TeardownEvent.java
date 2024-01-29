package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Event;

/**
 * When a circuit has been idle long enough, we will schedule
 * this event to tear it down
 */
public class TeardownEvent extends Event {
	ReservationPacket mPacket; // the reservation packet which initiated the circuit
	DistributedOpticServer mDevice; // the source device
	boolean active; // is this event still active
	private long mResetTime; // when the event is schedule
	private boolean finished; // is this teardown finished
	public TeardownEvent(long timeFromNowNs,ReservationPacket rp, DistributedOpticServer distributedOpticServer) {
		super(timeFromNowNs);
		mPacket = rp;
		mDevice = distributedOpticServer;
		active = true;
		mResetTime = Long.MAX_VALUE;
		finished = false;
	}

	@Override
	public void trigger() {

		if(active) {

			//this will mimic a call as if the flow has finished since the logic should be the same
			mDevice.onJumboFlowFinished(-1,-1,this.mDevice.getIdentifier(),mPacket.getOriginalServerDest());

			SimulationLogger.increaseStatisticCounter("AUTO_CIRCUIT_TEARDOWN_COUNT");
			finished = true;
		}
			

	}

	/**
	 * resets the evnt to timeFromNow time from now
	 * @param timeFromNow
	 */
	public void reset(long timeFromNow) {
		active = false;
		mResetTime = timeFromNow;
		
	}

	/**
	 * typically meant to be called when the flow is finished
	 */
	public void finish() {
		active = false;
		finished = true;
	}


	/**
	 * if the event is not finished then retrigger
	 * @return
	 */
	@Override
	public boolean retrigger() {
		if(!finished) {
			active = true;
			time = Simulator.getTimeFromNow(mResetTime);
			return true;
		}
		return false;
	}
}
