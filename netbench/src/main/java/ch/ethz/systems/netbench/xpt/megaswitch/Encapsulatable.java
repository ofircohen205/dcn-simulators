package ch.ethz.systems.netbench.xpt.megaswitch;

import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpHeader;

/**
 * allows to encapsulate packets in one another
 */
public interface Encapsulatable extends IpHeader {

	/**
	 * this will encapsulate a packet based on new src/dst
	 * @param newSource
	 * @param newDestination
	 * @return
	 */
	public Encapsulatable encapsulate(int newSource,int newDestination);

	/**
	 * this will de-encapsulate the packet
	 * @return
	 */
	public Encapsulatable deEncapsualte();
}
