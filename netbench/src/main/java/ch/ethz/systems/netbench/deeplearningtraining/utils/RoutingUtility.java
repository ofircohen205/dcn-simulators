package ch.ethz.systems.netbench.deeplearningtraining.utils;

import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RoutingUtility {

    public static List<Integer> determinePath(
            GraphDetails graphDetails,
            Flow flow,
            Map<ImmutablePair<Integer, Integer>, Integer> linkAssignmentsCounter,
            List<Integer> coreIds
    ) {
        int selectedCoreId = coreIds.get(0);
        int srcTorId = graphDetails.getTorIdOfServer(flow.getSrcId());
        ImmutablePair<Integer, Integer> srcTorCoreLink = ImmutablePair.of(srcTorId, selectedCoreId);
        int srcTorCoreLinkCount = linkAssignmentsCounter.getOrDefault(srcTorCoreLink, 0);

        int dstTorId = graphDetails.getTorIdOfServer(flow.getDstId());
        ImmutablePair<Integer, Integer> dstTorCoreLink = ImmutablePair.of(dstTorId, selectedCoreId);
        int dstTorCoreLinkCount = linkAssignmentsCounter.getOrDefault(dstTorCoreLink, 0);

        int torCoreLinkCount = Math.max(srcTorCoreLinkCount, dstTorCoreLinkCount);
        for (int coreId : coreIds) {
            ImmutablePair<Integer, Integer> srcCoreLink = ImmutablePair.of(srcTorId, coreId);
            ImmutablePair<Integer, Integer> dstCoreLink = ImmutablePair.of(dstTorId, coreId);
            int srcCoreLinkCount = linkAssignmentsCounter.getOrDefault(srcCoreLink, 0);
            int dstCoreLinkCount = linkAssignmentsCounter.getOrDefault(dstCoreLink, 0);
            int linkCount = Math.max(srcCoreLinkCount, dstCoreLinkCount);
            if (linkCount < torCoreLinkCount) {
                selectedCoreId = coreId;
                torCoreLinkCount = linkCount;
            }
        }
        List<Integer> path = constructPath(graphDetails, flow, selectedCoreId);
        updateDirectedLinkCount(path, linkAssignmentsCounter, 1);
        return path;
    }

    public static void updateDirectedLinkCount(List<Integer> path, Map<ImmutablePair<Integer, Integer>, Integer> linkAssignmentsCounter, int value) {
        for (int i = 1; i < path.size() - 2; i++) {
            ImmutablePair<Integer, Integer> link = ImmutablePair.of(path.get(i), path.get(i + 1));
            linkAssignmentsCounter.put(link, Math.max(linkAssignmentsCounter.getOrDefault(link, 0) + value, 0));
        }
    }

    public static List<Integer> constructPath(GraphDetails graphDetails, Flow flow, int coreId) {
        return Arrays.asList(flow.getSrcId(), graphDetails.getTorIdOfServer(flow.getSrcId()), coreId, graphDetails.getTorIdOfServer(flow.getDstId()), flow.getDstId());
    }
}
