package ch.ethz.systems.netbench.xpt.dynamic.controller;

import ch.ethz.systems.netbench.core.network.NetworkDevice;

public interface DynamicDevice {

	/**
	 * add a connection to device dest
	 * @param dest
	 */
	public void addConnection(NetworkDevice  dest, long jumboFlowId);

	/**
	 * remove a connection by jumbo flow id
	 * @param jumboFlowId
	 */
	public void removeConnection(long jumboFlowId);
	

}
