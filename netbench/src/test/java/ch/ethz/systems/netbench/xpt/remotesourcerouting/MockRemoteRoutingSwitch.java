package ch.ethz.systems.netbench.xpt.remotesourcerouting;

import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class MockRemoteRoutingSwitch extends RemoteSourceRoutingSwitch {

	MockRemoteRoutingSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary) {
		super(identifier, transportLayer, intermediary, null);
		// TODO Auto-generated constructor stub
	}

}
