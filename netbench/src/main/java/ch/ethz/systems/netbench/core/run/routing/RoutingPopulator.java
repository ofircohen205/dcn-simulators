package ch.ethz.systems.netbench.core.run.routing;

import ch.ethz.systems.netbench.core.config.NBProperties;

public abstract class RoutingPopulator {
	protected NBProperties configuration;
	public RoutingPopulator( NBProperties configuration) {
		this.configuration = configuration;
	}
    public abstract void populateRoutingTables();
}
