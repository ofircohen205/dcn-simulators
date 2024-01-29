package ch.ethz.systems.netbench.deeplearningtraining.routing;

import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.ext.basic.TcpHeader;

import java.util.Arrays;
import java.util.List;

public class EcmpRouting extends RoutingStrategy {


    @Override
    protected List<Integer> assignSinglePath(Flow flow) {
        int srcId = flow.getSrcId();
        int dstId = flow.getDstId();
        int srcTorId = graphDetails.getTorIdOfServer(srcId);
        int dstTorId = graphDetails.getTorIdOfServer(dstId);
        int nonSequentialHash = TcpHeader.absolute(TcpHeader.hash(srcId + TcpHeader.hash(dstId)));
        int hash = TcpHeader.hash(srcTorId + nonSequentialHash);
        List<Integer> coreIds = getCoreIds();
        int coreId = coreIds.get(hash % coreIds.size());
        return Arrays.asList(srcId, srcTorId, coreId, dstTorId, dstId);
    }

}
