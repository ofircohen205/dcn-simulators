package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class FacebookFrontendIntraDCWebFSD extends FlowSizeDistribution {

    private final FacebookFrontendIntraRackWebFSD intraRackWebFSD;
    private final FacebookFrontendIntraClusterWebFSD intraClusterWebFSD;

    public FacebookFrontendIntraDCWebFSD() {
        super();
        intraRackWebFSD = new FacebookFrontendIntraRackWebFSD();
        intraClusterWebFSD = new FacebookFrontendIntraClusterWebFSD();
        SimulationLogger.logInfo(
                "Flow planner flow size dist.",
                "Facebook frontend intra-dc web discrete"
        );
    }

    @Override
    public long generateFlowSizeByte() {
        double outcome = independentRng.nextDouble();
        if (outcome <= 0.043612335) {
            return 153;
        } else if (outcome <= 0.086787367) {
            return 306;
        } else if (outcome <= 0.198053641) {
            return 513;
        } else if (outcome <= 0.319628026) {
            return 959;
        } else if (outcome <= 0.387914564) {
            return 1329;
        } else if (outcome <= 0.507643547) {
            return 1367;
        } else if (outcome <= 0.609251101) {
            return 1511;
        } else if (outcome <= 0.686607158) {
            return 1965;
        } else if (outcome <= 0.797305171) {
            return 2322;
        } else if (outcome <= 0.917662135) {
            return 4966;
        } else if (outcome <= 0.96811719) {
            return 15439;
        } else if (outcome <= 0.993131183) {
            return 47936;
        } else {
            return 116761;
        }
    }

    @Override
    public double getMeanFlowSizeByte() {
        return 0.043612335 * 153 + 0.043175032 * 306 + 0.111266274 * 513
                + 0.121574385 * 959 + 0.068286538 * 1329 + 0.119728983 * 1367
                + 0.101607554 * 1511 + 0.077356057 * 1965 + 0.110698013 * 2322
                + 0.120356964 * 4966 + 0.050455055 * 15439 + 0.025013993 * 47936
                + 0.006013993 * 116761;
    }

    public String toString() {
        return "Facebook Frontend Intra DC Web FSD(mean=" + getMeanFlowSizeByte() + ")";
    }

    public FacebookFrontendIntraRackWebFSD getIntraRackWebFSD() {
        return intraRackWebFSD;
    }

    public FacebookFrontendIntraClusterWebFSD getIntraClusterWebFSD() {
        return intraClusterWebFSD;
    }
}
