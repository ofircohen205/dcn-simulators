package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;

import java.util.ArrayList;
import java.util.List;

public class MetaNodeServer extends MetaNodeSwitch {
    int currToRDest = 0;
    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     * @param configuration
     */
    protected MetaNodeServer(int identifier, TransportLayer transportLayer,  int N, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer,N, intermediary, configuration);
        MNId = -1;
        currToRDest = identifier;
    }

    @Override
    public void receive(Packet genericPacket) {
        MNEpochPacket tcpPacket = (MNEpochPacket) genericPacket;
        if(tcpPacket.getDestinationId() == this.identifier) {
            getTransportLayer().receive(tcpPacket);
        } else {
            forwardFromserver(genericPacket);
        }
        return;
    }

    protected void forwardFromserver(Packet genericPacket){
        MNEpochPacket packet = (MNEpochPacket) genericPacket;
        initPacketMetaRouting(packet);

        assert currToRDest >= 0;

        List<Integer> possibilities = destinationToNextSwitch.get(packet.getDestinationId());
        currToRDest %= possibilities.size();

        int next = possibilities.get(currToRDest);
        currToRDest += 1;
        this.getTargetOuputPort(next).enqueue(genericPacket);


        return;
    }

    private void initPacketMetaRouting(MNEpochPacket packet) {
        List<RoutingAlg.RoutingRule> routingRules = rules.get(MNEpochController.getInstance().getMetaNodeId(packet.getDestinationId()));
        RoutingAlg.RoutingRule routingRule = routingRules.get(rand.nextInt(routingRules.size()));
        packet.setRoutingRule(routingRule);

    }

    @Override
    public void receiveFromIntermediary(Packet genericPacket) {
        this.receive(genericPacket);
    }

    public void startEpoch(long flowId, long bytesToSend){
        MetaNodeTransport transport = (MetaNodeTransport) this.getTransportLayer();
        transport.startEpoch(flowId,bytesToSend);
    }

    @Override
    public void setMetaNodeId(int mn) {
        MNId = mn;
    }


    public void invalidateRules(){
//        if(identifier==0){
//            System.out.println(rules);
//        }
        this.rules.clear();
    }

    public void addRule(RoutingAlg.RoutingRule rule){
        List<RoutingAlg.RoutingRule> routingRules = rules.getOrDefault(rule.MNDest, new ArrayList<>());
        routingRules.add(rule);
        rules.put(rule.MNDest,routingRules);

    }


    public void pullPacket(long flowid) {
        MetaNodeTransport transport = (MetaNodeTransport) getTransportLayer();
        transport.pullPacket(flowid);

    }

    public void startEpoch() {
        MetaNodeTransport transport = (MetaNodeTransport) this.getTransportLayer();
        transport.startEpoch();

    }

    public void pullPackets(long flowId) {
        for(OutputPort port: getOuputPortsMap().values()){
            pullPacket(flowId);
        }
    }
}
