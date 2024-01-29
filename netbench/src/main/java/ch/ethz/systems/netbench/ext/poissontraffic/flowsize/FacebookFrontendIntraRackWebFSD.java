package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraRackWebFSD {

    public FacebookFrontendIntraRackWebFSD() {
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-rack web discrete"
        );
    }

    public long generateFlowSizeByte(double outcome) {
        if (outcome <= 0.097822884) {
            return 100;
        } else if (outcome <= 0.201278437) {
            return 150;
        } else if (outcome <= 0.299969024) {
            return 488;
        } else if (outcome <= 0.405766014) {
            return 1695;
        } else if (outcome <= 0.510774314) {
            return 2622;
        } else if (outcome <= 0.61307108) {
            return 4166;
        } else if (outcome <= 0.717737378) {
            return 6271;
        } else if (outcome <= 0.809812168) {
            return 10230;
        } else if (outcome <= 0.907571124) {
            return 23065;
        } else if (outcome <= 0.961381715) {
            return 104013;
        } else if (outcome <= 0.986397233) {
            return 338094;
        } else {
            return 6245421;
        }
    }

    public double getMeanFlowSizeByte() {
        return 0.097822884 * 100 + 0.103455553 * 150 + 0.098690587 * 488
                + 0.10579699 * 1695 + 0.1050083 * 2622 + 0.102296766 * 4166
                + 0.104665798 * 6271 + 0.09207479 * 10230 + 0.09775899 * 23065
                + 0.053810591 * 104013 + 0.025015518 * 338094 + 0.013665788 * 6245421;
    }

    public String toString() {
        return "Facebook Frontend Intra Rack Web FSD(mean=" + getMeanFlowSizeByte() + ")";
    }
}
