package ch.ethz.systems.netbench.core.run.infrastructure;


import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public abstract class TransportLayerGenerator {
	protected NBProperties configuration;
	public TransportLayerGenerator(NBProperties configuration) {
		this.configuration = configuration;
	}
    public abstract TransportLayer generate(int identifier);
}
