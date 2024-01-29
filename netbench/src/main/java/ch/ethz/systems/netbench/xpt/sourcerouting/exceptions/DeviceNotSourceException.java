package ch.ethz.systems.netbench.xpt.sourcerouting.exceptions;

public class DeviceNotSourceException extends IllegalStateException {

	public DeviceNotSourceException(long flowId, int identifier) {
	    super("The device " + identifier + " is not the source for flow " + flowId);
	}

}
