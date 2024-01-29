package ch.ethz.systems.netbench.xpt.sourcerouting.exceptions;

public class FlowPathExists extends IllegalArgumentException {
	public FlowPathExists(long flowId) {
		super("Already have path with this flow " + flowId);
	}
}
