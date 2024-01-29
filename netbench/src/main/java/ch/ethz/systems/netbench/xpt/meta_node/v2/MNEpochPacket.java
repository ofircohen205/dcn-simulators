package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

public class MNEpochPacket extends Packet {
    RoutingAlg.RoutingRule routingRule;
    public final int sourceMN;
    public final int destMN;
    private final long dataSizeByte;
    private final long sequenceNumber;
    private final int sourceId;
    private final int destinationId;
    public MNEpochPacket(long flowId, long dataSizeByte, int sourceId, int destinationId, long sequenceNumber, int sourceMN, int destMN) {
        super(flowId, dataSizeByte*8);
        this.dataSizeByte = dataSizeByte;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.sequenceNumber = sequenceNumber;
        this.destMN = destMN;
        this.sourceMN = sourceMN;
        routingRule = null;
    }

    public void setRoutingRule(RoutingAlg.RoutingRule routingRule) {
        if(this.routingRule!=null){
            throw new IllegalStateException("routing rull must be set once");
        }
        this.routingRule = routingRule;
    }

    public RoutingAlg.RoutingRule getRoutingRule() {
        return routingRule;
    }

    @Override
    public String toString(){
        return super.toString() + " " + " MNSource " + sourceMN + " MNDest " + destMN;
    }

    public int getSourceId(){
        return sourceId;
    }

    public int getDestinationId(){
        return destinationId;
    }

    public long getDataSizeByte() {
        return this.dataSizeByte;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
