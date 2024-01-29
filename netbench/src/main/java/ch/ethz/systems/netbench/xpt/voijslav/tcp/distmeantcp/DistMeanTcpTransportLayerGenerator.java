package ch.ethz.systems.netbench.xpt.voijslav.tcp.distmeantcp;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.TransportLayerGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class DistMeanTcpTransportLayerGenerator extends TransportLayerGenerator {

    public DistMeanTcpTransportLayerGenerator(NBProperties configuration) {
        super(configuration);
        // No parameters needed
        SimulationLogger.logInfo("Transport layer", "DistMeanTcp");
    }

    @Override
    public TransportLayer generate(int identifier) {
        return new DistMeanTcpTransportLayer(
                identifier,
                Simulator.getConfiguration().getLongPropertyOrFail(Constants.Simulation.SEED),
                configuration
        );
    }

}