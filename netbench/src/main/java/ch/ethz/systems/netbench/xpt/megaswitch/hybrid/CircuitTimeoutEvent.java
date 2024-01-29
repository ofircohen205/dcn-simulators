package ch.ethz.systems.netbench.xpt.megaswitch.hybrid;

import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;

public class CircuitTimeoutEvent extends Event{
	OpticElectronicHybrid device;
	JumboFlow jFlow;

	boolean canceled;
	public CircuitTimeoutEvent(long timeFromNowNS, OpticElectronicHybrid device, JumboFlow jFlow){
		super(timeFromNowNS);
		canceled = false;
		this.device = device;
		this.jFlow = jFlow;
	}

	public void cancel(){
		canceled = true;
	}

	@Override
	public void trigger(){
		if(!canceled){
			device.resetJumboFlow(jFlow);
		}
	}	
}
