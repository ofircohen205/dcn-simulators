package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraDCCacheFSD extends FlowSizeDistribution {

    private final FacebookFrontendIntraRackCacheFSD intraRackCacheFSD;
    private final FacebookFrontendIntraClusterCacheFSD intraClusterCacheFSD;

    public FacebookFrontendIntraDCCacheFSD() {
        super();
        intraRackCacheFSD = new FacebookFrontendIntraRackCacheFSD();
        intraClusterCacheFSD = new FacebookFrontendIntraClusterCacheFSD();
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-dc cache discrete"
        );
    }

    @Override
    public long generateFlowSizeByte() {
        double outcome = independentRng.nextDouble();
        if (outcome <= 0.039938517) {
            return 102;
        } else if (outcome <= 0.047077838) {
            return 197;
        } else if (outcome <= 0.075998127) {
            return 335;
        } else if (outcome <= 0.139485122) {
            return 450;
        } else if (outcome <= 0.181767962) {
            return 727;
        } else if (outcome <= 0.209923064) {
            return 1323;
        } else if (outcome <= 0.24793998) {
            return 2408;
        } else if (outcome <= 0.288365889) {
            return 4382;
        } else if (outcome <= 0.344575719) {
            return 7554;
        } else if (outcome <= 0.395310108) {
            return 12333;
        } else if (outcome <= 0.450295367) {
            return 19070;
        } else if (outcome <= 0.492989385) {
            return 27922;
        } else if (outcome <= 0.558361273) {
            return 41648;
        } else if (outcome <= 0.607575243) {
            return 53723;
        } else if (outcome <= 0.682730226) {
            return 81427;
        } else if (outcome <= 0.728257993) {
            return 106223;
        } else if (outcome <= 0.788622394) {
            return 147350;
        } else if (outcome <= 0.840425711) {
            return 209996;
        } else if (outcome <= 0.887527541) {
            return 315964;
        } else if (outcome <= 0.941504034) {
            return 559674;
        } else if (outcome <= 0.977638924) {
            return 1018328;
        } else if (outcome <= 0.991157245) {
            return 1661278;
        } else {
            return 2000000;
        }
    }

    @Override
    public double getMeanFlowSizeByte() {
        return 0.039938517 * 102 + 0.007139321 * 197 + 0.028920289 * 335
                + 0.063486995 * 450 + 0.04228284 * 727 + 0.028155102 * 1323
                + 0.038016916 * 2408 + 0.040425909 * 4382 + 0.05620983 * 7554
                + 0.050734389 * 12333 + 0.054985259 * 19070 + 0.042694018 * 27922
                + 0.065371888 * 41648 + 0.04921397 * 53723 + 0.075154983 * 81427
                + 0.045527767 * 106223 + 0.060364401 * 147350 + 0.051803317 * 209996
                + 0.051803317 * 315964 + 0.05400233 * 559674 + 0.03612183 * 1018328
                + 0.013818821 * 1661278 + 0.013518755 * 2000000;
    }

    public String toString() {
        return "Facebook Frontend Intra DC Cache FSD(mean=" + getMeanFlowSizeByte() + ")";
    }

    public FacebookFrontendIntraRackCacheFSD getIntraRackCacheFSD() {
        return intraRackCacheFSD;
    }

    public FacebookFrontendIntraClusterCacheFSD getIntraClusterCacheFSD() {
        return intraClusterCacheFSD;
    }
}
