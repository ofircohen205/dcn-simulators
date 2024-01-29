package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class AlibabaMachineLearningFSD extends FlowSizeDistribution {

    public AlibabaMachineLearningFSD() {
        super();
        SimulationLogger.logInfo("Flow planner flow size dist.", "Alibaba machine learning discrete");
    }

    @Override
    public long generateFlowSizeByte() {
        double outcome = independentRng.nextDouble();
        if (outcome <= 0.00155) {
            return 20;
        } else if (outcome <= 0.04781) {
            return 250;
        } else if (outcome <= 0.12189) {
            return 500;
        } else if (outcome <= 0.22716) {
            return 1_000;
        } else if (outcome <= 0.42732) {
            return 4_000;
        } else if (outcome <= 0.66330) {
            return 10_000;
        } else if (outcome <= 0.98388) {
            return 50_000;
        } else if (outcome <= 0.99232) {
            return 100_000;
        } else if (outcome <= 0.99968) {
            return 500_000;
        } else if (outcome <= 0.99996) {
            return 1_000_000;
        } else {
            return 2_000_000;
        }
    }

    @Override
    public double getMeanFlowSizeByte() {
        return 20 * 0.00155 + 250 * 0.04626 + 500 * 0.07408 + 1_000 * 0.10528 + 4_000 * 0.20016
                + 10_000 * 0.23598 + 50_000 * 0.32058 + 100_000 * 0.00844
                + 500_000 * 0.00744 + 1_000_000 * 0.00744 + 2_000_000 * 0.00028;
    }

    public String toString() {
        return "Alibaba Machine Learning FSD(mean=" + getMeanFlowSizeByte() + ")";
    }

}
