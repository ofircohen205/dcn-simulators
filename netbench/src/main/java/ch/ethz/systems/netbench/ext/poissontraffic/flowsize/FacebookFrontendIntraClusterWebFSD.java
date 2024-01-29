package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraClusterWebFSD {

    public FacebookFrontendIntraClusterWebFSD() {
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-cluster web discrete"
        );
    }

    public long generateFlowSizeByte(double outcome) {
        if (outcome <= 0.105286344) {
            return 148;
        } else if (outcome <= 0.202202643) {
            return 307;
        } else if (outcome <= 0.244025439) {
            return 639;
        } else if (outcome <= 0.346488951) {
            return 1438;
        } else if (outcome <= 0.466184514) {
            return 2796;
        } else if (outcome <= 0.575824563) {
            return 5300;
        } else if (outcome <= 0.683872812) {
            return 12467;
        } else if (outcome <= 0.775735177) {
            return 35664;
        } else if (outcome <= 0.838329137) {
            return 108757;
        } else if (outcome <= 0.924273373) {
            return 268106;
        } else {
            return 425451;
        }
    }

    public double getMeanFlowSizeByte() {
        return 0.097822884 * 100 + 0.103455553 * 150 + 0.098690587 * 488
                + 0.10579699 * 1695 + 0.1050083 * 2622 + 0.102296766 * 4166
                + 0.104665798 * 6271 + 0.09207479 * 10230 + 0.09775899 * 23065
                + 0.053810591 * 104013 + 0.025015518 * 338094 + 0.013665788 * 6245421;
    }

    public String toString() {
        return "Facebook Frontend Intra Cluster Web FSD(mean=" + getMeanFlowSizeByte() + ")";
    }
}
