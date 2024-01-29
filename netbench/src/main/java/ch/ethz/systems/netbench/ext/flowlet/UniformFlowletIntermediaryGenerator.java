package ch.ethz.systems.netbench.ext.flowlet;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;

public class UniformFlowletIntermediaryGenerator extends IntermediaryGenerator {

    public UniformFlowletIntermediaryGenerator(NBProperties configuration) {
    	super(configuration);
        SimulationLogger.logInfo("Network device flowlet intermediary", "UNIFORM");
    }

    @Override
    public Intermediary generate(int networkDeviceIdentifier) {
        return new UniformFlowletIntermediary(configuration);
    }

}
