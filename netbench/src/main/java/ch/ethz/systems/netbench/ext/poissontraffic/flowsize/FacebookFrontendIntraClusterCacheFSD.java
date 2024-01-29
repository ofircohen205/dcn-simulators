package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraClusterCacheFSD {

    public FacebookFrontendIntraClusterCacheFSD() {
        super();
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-cluster cache discrete"
        );
    }

    public long generateFlowSizeByte(double outcome) {
        if (outcome <= 0.000699301) {
            return 71;
        } else if (outcome <= 0.027272727) {
            return 72;
        } else if (outcome <= 0.031645229) {
            return 108;
        } else if (outcome <= 0.035421656) {
            return 167;
        } else if (outcome <= 0.176923077) {
            return 336;
        } else if (outcome <= 0.186962353) {
            return 409;
        } else if (outcome <= 0.276258191) {
            return 536;
        } else if (outcome <= 0.353822737) {
            return 851;
        } else if (outcome <= 0.408875749) {
            return 1403;
        } else if (outcome <= 0.445100976) {
            return 2552;
        } else if (outcome <= 0.463620108) {
            return 4642;
        } else if (outcome <= 0.475138104) {
            return 8443;
        } else if (outcome <= 0.484764665) {
            return 14249;
        } else if (outcome <= 0.503520931) {
            return 32430;
        } else if (outcome <= 0.515564013) {
            return 56624;
        } else if (outcome <= 0.534986516) {
            return 102994;
        } else if (outcome <= 0.580819108) {
            return 182407;
        } else if (outcome <= 0.632944691) {
            return 282042;
        } else if (outcome <= 0.682823387) {
            return 448093;
        } else if (outcome <= 0.702787914) {
            return 815053;
        } else if (outcome <= 0.715218316) {
            return 1434756;
        } else if (outcome <= 0.764335664) {
            return 1900265;
        } else if (outcome <= 0.822670252) {
            return 2294411;
        } else if (outcome <= 0.900954449) {
            return 2741807;
        } else if (outcome <= 0.981118881) {
            return 3113431;
        } else {
            return 3520031;
        }
    }

    public double getMeanFlowSizeByte() {
        return 0.000699301 * 71 + 0.026573426 * 72 + 0.004372502 * 108
                + 0.003776427 * 167 + 0.141501421 * 336 + 0.010039276 * 409
                + 0.089295838 * 536 + 0.077564546 * 851 + 0.055064546 * 1403
                + 0.036225227 * 2552 + 0.018519132 * 4642 + 0.011517996 * 8443
                + 0.009626481 * 14249 + 0.018756266 * 32430 + 0.012043162 * 56624
                + 0.019422561 * 102994 + 0.045822908 * 182407 + 0.052125682 * 282042
                + 0.049125682 * 448093 + 0.019964664 * 815053 + 0.010430561 * 1434756
                + 0.049117483 * 1900265 + 0.058334833 * 2294411 + 0.081284282 * 2741807
                + 0.080164164 * 3113431 + 0.040664665 * 3520031;
    }

    public String toString() {
        return "Facebook Frontend Intra Cluster Cache FSD(mean=" + getMeanFlowSizeByte() + ")";
    }
}
