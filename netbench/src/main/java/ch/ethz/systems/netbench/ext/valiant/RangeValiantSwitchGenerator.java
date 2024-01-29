package ch.ethz.systems.netbench.ext.valiant;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.core.utility.Constants;

public class RangeValiantSwitchGenerator extends NetworkDeviceGenerator {

    private final int numNodes;
    private final IntermediaryGenerator intermediaryGenerator;
    private final int nodeRangeLower;
    private final int nodeRangeUpper;

    public RangeValiantSwitchGenerator(IntermediaryGenerator intermediaryGenerator, int numNodes, NBProperties configuration) {
        super(configuration);
        SimulationLogger.logInfo("Network device", "RANGE_VALIANT_SWITCH(numNodes=" + numNodes + ")");

        // Standard fields
        this.numNodes = numNodes;
        this.intermediaryGenerator = intermediaryGenerator;

        // Range of [lower, upper] for which nodes are eligible to be chosen as valiant node
        this.nodeRangeLower = configuration.getIntegerPropertyOrFail(Constants.Valiant.NODE_RANGE_LOWER_INCL);
        this.nodeRangeUpper = configuration.getIntegerPropertyOrFail(Constants.Valiant.NODE_RANGE_UPPER_INCL);

        // Check range
        if (nodeRangeLower < 0 || nodeRangeUpper < 0 || nodeRangeLower > numNodes - 1 || nodeRangeUpper > numNodes - 1 || nodeRangeLower > nodeRangeUpper) {
            throw new IllegalArgumentException("Invalid valiant node range [" + nodeRangeLower + ", " + nodeRangeUpper + "] for n=" + numNodes);
        }

    }

    @Override
    public NetworkDevice generate(int identifier) {
        return this.generate(identifier, null);
    }

    @Override
    public NetworkDevice generate(int identifier, TransportLayer transportLayer) {
        return new RangeValiantSwitch(identifier, transportLayer, numNodes, intermediaryGenerator.generate(identifier), nodeRangeLower, nodeRangeUpper, configuration);
    }

}
