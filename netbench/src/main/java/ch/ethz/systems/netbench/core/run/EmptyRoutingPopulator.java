package ch.ethz.systems.netbench.core.run;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;

public class EmptyRoutingPopulator extends RoutingPopulator {
    public EmptyRoutingPopulator(NBProperties configuration) {
        super(configuration);
    }

    @Override
    public void populateRoutingTables() {

    }
}
