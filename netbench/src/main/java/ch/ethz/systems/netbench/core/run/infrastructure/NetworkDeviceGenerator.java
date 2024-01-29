package ch.ethz.systems.netbench.core.run.infrastructure;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public abstract class NetworkDeviceGenerator {
	protected NBProperties configuration;
	public NetworkDeviceGenerator(NBProperties configuration) {
		this.configuration = configuration;
	}
    public abstract NetworkDevice generate(int identifier);
    public abstract NetworkDevice generate(int identifier, TransportLayer server);
}
