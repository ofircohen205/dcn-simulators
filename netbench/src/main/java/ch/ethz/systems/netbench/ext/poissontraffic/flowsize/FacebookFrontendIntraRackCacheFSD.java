package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraRackCacheFSD {

    public FacebookFrontendIntraRackCacheFSD() {
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-rack cache discrete"
        );
    }

    public long generateFlowSizeByte(double outcome) {
        if (outcome <= 0.000699301) {
            return 72;
        } else if (outcome <= 0.024475524) {
            return 77;
        } else if (outcome <= 0.03076923) {
            return 142;
        } else if (outcome <= 0.045454545) {
            return 144;
        } else if (outcome <= 0.102758517) {
            return 157;
        } else if (outcome <= 0.140382221) {
            return 266;
        } else if (outcome <= 0.20417975) {
            return 325;
        } else if (outcome <= 0.269316026) {
            return 401;
        } else if (outcome <= 0.319042905) {
            return 508;
        } else if (outcome <= 0.39709369) {
            return 598;
        } else if (outcome <= 0.50605351) {
            return 718;
        } else if (outcome <= 0.608283345) {
            return 818;
        } else if (outcome <= 0.704909763) {
            return 1135;
        } else if (outcome <= 0.796815403) {
            return 1512;
        } else if (outcome <= 0.857509234) {
            return 1908;
        } else if (outcome <= 0.915261069) {
            return 2871;
        } else if (outcome <= 0.950537756) {
            return 5224;
        } else if (outcome <= 0.97838098) {
            return 7857;
        } else {
            return 10000;
        }
    }

    public double getMeanFlowSizeByte() {
        return 0.000699301 * 72 + 0.023776223 * 77 + 0.006293706 * 142
                + 0.014685315 * 144 + 0.057254057 * 157 + 0.037623623 * 266
                + 0.063793207 * 325 + 0.065193065 * 401 + 0.04968532 * 508
                + 0.078150578 * 598 + 0.108959609 * 718 + 0.102205602 * 818
                + 0.096706096 * 1135 + 0.091500591 * 1512 + 0.06063586 * 1908
                + 0.057626057 * 2871 + 0.027783528 * 5224 + 0.027783528 * 7857
                + 0.027783528 * 10000;
    }

    public String toString() {
        return "Facebook Frontend Intra Rack Cache FSD(mean=" + getMeanFlowSizeByte() + ")";
    }

}
