package ch.ethz.systems.netbench.xpt.voijslav.tcp.distrandtcp;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.TransportLayerGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class DistRandTcpTransportLayerGenerator extends TransportLayerGenerator {

    public DistRandTcpTransportLayerGenerator(NBProperties configuration) {
        super(configuration);
        // No parameters needed
        SimulationLogger.logInfo("Transport layer", "DistRandTcp");
    }

    @Override
    public TransportLayer generate(int identifier) {
        return new DistRandTcpTransportLayer(
                identifier,
                Simulator.getConfiguration().getLongPropertyOrFail(Constants.Simulation.SEED),
                configuration
        );
    }

}