package ch.ethz.systems.netbench.xpt.dynamic.device;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;

public class DynamicOutuptPort extends EcnTailDropOutputPort {

	protected DynamicOutuptPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link,
			long maxQueueSizeBytes, long ecnThresholdKBytes) {
		super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
		// TODO Auto-generated constructor stub
	}

}
