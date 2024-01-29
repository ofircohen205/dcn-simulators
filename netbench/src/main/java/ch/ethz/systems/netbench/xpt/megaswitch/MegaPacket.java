package ch.ethz.systems.netbench.xpt.megaswitch;

import ch.ethz.systems.netbench.ext.basic.IpPacket;

/**
 * unused as other solutions have been found.
 */
public class MegaPacket extends IpPacket {

	IpPacket encapsulated;
	public MegaPacket(long flowId, long payloadSizeBit, int sourceId, int destinationId, int TTL, long headerSize) {
		super(flowId, payloadSizeBit, sourceId, destinationId, TTL, headerSize);
		// TODO Auto-generated constructor stub
	}

	
	public MegaPacket(IpPacket p, int source,int dest) {
		super(p.getFlowId(), p.getSizeBit(), source, dest, p.getTTL());
		encapsulated = p;
	}

	public IpPacket getEncapsulated() {
		return encapsulated;
	}
}
