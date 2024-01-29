package ch.ethz.systems.netbench.core.run.infrastructure;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;

public abstract class IntermediaryGenerator {
	protected NBProperties configuration;
	public IntermediaryGenerator(NBProperties configuration) {
		this.configuration = configuration;
	}
    public abstract Intermediary generate(int networkDeviceIdentifier);
}
