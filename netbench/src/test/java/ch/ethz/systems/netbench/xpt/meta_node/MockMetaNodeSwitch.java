package ch.ethz.systems.netbench.xpt.meta_node;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.TcpHeader;

import java.util.List;
import java.util.Map;

public class MockMetaNodeSwitch extends MetaNodeSwitch {
    /**
     * Constructor for MetaNodeSwitch
     *
     * @param identifier     Network device identifier
     * @param transportLayer Underlying server transport layer instance (set null, if none)
     * @param n              Number of network devices in the entire network (for routing table size)
     * @param intermediary   Flowlet intermediary instance (takes care of hash adaptation for flowlet support)
     * @param configuration
     */
    public MockMetaNodeSwitch(int identifier, TransportLayer transportLayer, int n, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, n, intermediary, configuration);
    }


    public List<Integer> getDestinationToMNMock(int MNId){
        return super.getDestinationToMN(MNId);
    }

    @Override
    protected MNController getController() {
        return MockMNController.getInstance();
    }

    public  MetaNodeToken mockGetToken(int destinationId){
        return super.getToken(destinationId);
    }

    public Map<Integer, MetaNodeToken> getTokenMap(){
        return tokenMap;
    }
}
