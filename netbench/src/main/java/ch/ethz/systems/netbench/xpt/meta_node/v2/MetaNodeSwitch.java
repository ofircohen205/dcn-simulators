package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitch;

import java.util.*;

public class MetaNodeSwitch extends EcmpSwitch {
    protected int MNId;
    HashMap<Integer, List<RoutingAlg.RoutingRule>> rules;
    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     * @param configuration
     */
    protected MetaNodeSwitch(int identifier, TransportLayer transportLayer, int n, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, n, intermediary, configuration);
        rules = new HashMap<>();
        MNId = -1;

    }

    @Override
    public void receive(Packet genericPacket) {
        MNEpochPacket packet = (MNEpochPacket) genericPacket;
        boolean arrivedAtDest = getOuputPortsMap().containsKey(packet.getDestinationId());
        if(arrivedAtDest){
            getTargetOuputPort(packet.getDestinationId()).enqueue(genericPacket);
            return;
        }

        RoutingAlg.RoutingRule routingRule = packet.getRoutingRule();
        int desinationMN = routingRule.getNextMN(this.getMNID());


        List<Integer> possibilities = getDestinationToMN(desinationMN);
        int randomNext = possibilities.get(rand.nextInt(possibilities.size()));
        OutputPort out = this.getTargetOuputPort(randomNext);
        out.enqueue(genericPacket);
        routingRule.onUtilization(this.identifier, this.getMNID(), desinationMN);
    }

    @Override
    public void receiveFromIntermediary(Packet genericPacket) {
//        receive(genericPacket);
    }


    protected List<Integer> getDestinationToMN(int mnDest) {
        List<Integer> possiblilities = new LinkedList<>();
        for(int i : getOuputPortsMap().keySet()){
            NetworkDevice nd = getTargetOuputPort(i).getTargetDevice();
            if(nd.isServer()) continue;
            MetaNodeSwitch mnsw = (MetaNodeSwitch) nd;
            if(mnsw.MNId == mnDest) possiblilities.add(mnsw.identifier);

        }
        if(possiblilities.size() == 0) {
            throw new IllegalStateException("there are no routes to the MNDest " + mnDest);
        }
        return possiblilities;
    }

    public void setMetaNodeId(int mn) {
        if(MNId!= -1) throw new IllegalStateException("Meta Node Id cannot be set twice");
        MNId = mn;
    }

    public void setRandomizer(Random rand) {
        this.rand = rand;
    }

    public int getMNID(){
        if(MNId==-1) throw new IllegalStateException("No MN ID");
        return MNId;
    }
}
