package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpHeader;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitch;

import java.util.*;

public class MetaNodeSwitch extends EcmpSwitch {
    protected Map<Integer, MetaNodeToken> tokenMap;
    private Random rand;
    private int MNId;
    private int currToRDest;
    /**
     * Constructor for MetaNodeSwitch
     *
     * @param identifier     Network device identifier
     * @param transportLayer Underlying server transport layer instance (set null, if none)
     * @param n              Number of network devices in the entire network (for routing table size)
     * @param intermediary   Flowlet intermediary instance (takes care of hash adaptation for flowlet support)
     * @param configuration
     */
    public MetaNodeSwitch(int identifier, TransportLayer transportLayer, int n, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, n, intermediary, configuration);
        MNId = -1;
        tokenMap = new HashMap<>();
        currToRDest = 0;
        if(!isServer()) currToRDest = -1;

    }

    @Override
    public void receive(Packet genericPacket) {
        super.receive(genericPacket);
        try{
            MetaNodePacket mnPacket = (MetaNodePacket) genericPacket;
            // Check if it has arrived
            if (mnPacket.getDestinationId() == this.identifier) {
                mnPacket.serverToken.onReceive(mnPacket.getSizeBit()/8);
            }
        }catch (ClassCastException e){

        }


    }

    protected void  forwardToNextSwitch(Packet genericPacket){
        // Convert to TCP packet
        TcpHeader tcpHeader = (TcpHeader) genericPacket;
        boolean isSecondHop = getTargetOuputPort(tcpHeader.getSourceId()) == null;
        boolean localPacket = getTargetOuputPort(tcpHeader.getSourceId()) != null;

        if(this.isServer()){
            forwardFromserver(genericPacket);
            return;
        }
        if(isSecondHop){
            forwardFromSecondHop(genericPacket);
            return;
        }
        if(localPacket){
            if(tcpHeader.isFIN() && tcpHeader.isACK()){
                releaseToken(tcpHeader.getSourceId());
            }
            getTargetOuputPort(tcpHeader.getDestinationId()).enqueue(genericPacket);
            return;
        }

        MetaNodeToken token = null;
        try{
            MetaNodePacket metaNodePacket = (MetaNodePacket) genericPacket;
            token = metaNodePacket.token;
        }catch(ClassCastException e){
            token = getToken(tcpHeader.getDestinationId());

        }
        List<Integer> possibilities = getDestinationToMN(token.getMiddleHop());
        int randomNext = possibilities.get(rand.nextInt(possibilities.size()));
        OutputPort out = this.getTargetOuputPort(randomNext);
        out.enqueue(genericPacket);
        token.nextBytes(tcpHeader.getSizeBit()/(8));
        int MNDest = getController().getServerMetaNodeNum(tcpHeader.getDestinationId());
        tokenMap.put(MNDest, token);
    }

    private void releaseToken(int sourceId) {

    }

    protected void forwardFromserver(Packet genericPacket){
        TcpHeader tcpHeader = (TcpHeader) genericPacket;
        assert currToRDest >= 0;
        List<Integer> possibilities = destinationToNextSwitch.get(tcpHeader.getDestinationId());
        int next = possibilities.get(currToRDest);
        this.getTargetOuputPort(next).enqueue(genericPacket);
        currToRDest++;
        currToRDest%=possibilities.size();
        return;
    }

    protected void forwardFromSecondHop(Packet genericPacket){
        TcpHeader tcpHeader = (TcpHeader) genericPacket;
        List<Integer> possibilities = destinationToNextSwitch.get(tcpHeader.getDestinationId());
        int randomNext = possibilities.get(rand.nextInt(possibilities.size()));
        this.getTargetOuputPort(randomNext).enqueue(genericPacket);
        return;

    }
    protected List<Integer> getDestinationToMN(int mnDest) {
        List<Integer> possiblilities = new LinkedList<>();
        for(int i : getOuputPortsMap().keySet()){
            NetworkDevice nd = getTargetOuputPort(i).getTargetDevice();
            if(nd.isServer()) continue;
            MetaNodeSwitch mnsw = (MetaNodeSwitch) nd;
            if(mnsw.MNId == mnDest) possiblilities.add(mnsw.identifier);

        }
        if(possiblilities.size() == 0) throw new IllegalStateException("there are no routes to the MNDest " + mnDest);
        return possiblilities;
    }

    protected MetaNodeToken getToken(int destinationId) {
        MNController controller = getController();
        int MNSource = this.MNId;
        int MNDest = controller.getMetaNodeId(destinationToNextSwitch.get(destinationId).get(0));
        MetaNodeToken currToken = tokenMap.get(MNDest);
        if(currToken==null){
            currToken = getController().getToken(MNSource, MNDest);
        }
        if(currToken.expired()){
            currToken = getController().getToken(MNSource, MNDest, currToken.getOriginalBytesAllocated());
        }

        return currToken;
    }

    protected MNController getController() {
        return MNController.getInstance();
    }

    public void setMetaNodeId(int mn) {
        if(MNId!= -1) throw new IllegalStateException("Meta Node Id cannot be set twice");
        MNId = mn;
    }

    public int getMNId(){
        return MNId;
    }

    public void setRandomizer(Random rand) {
        this.rand = rand;
    }

    public long getLoadByte(){
        long load = 0l;
        for(OutputPort outputPort: getOuputPortsMap().values()){
            load += (outputPort.getQueueSizeBit()/8);
        }
        return load;
    }
}
