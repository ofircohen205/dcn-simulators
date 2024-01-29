package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

public class MetaNodePacket extends FullExtTcpPacket {

    MetaNodeToken token = null;
    ServerToken serverToken = null;

    public MetaNodePacket(long flowId, long dataSizeByte, int sourceId, int destinationId, int TTL, int sourcePort,
            int destinationPort, long sequenceNumber, long acknowledgementNumber, boolean NS, boolean CWR, boolean ECE,
            boolean URG, boolean ACK, boolean PSH, boolean RST, boolean SYN, boolean FIN, double windowSize,
            long priority) {
        super(flowId, dataSizeByte, sourceId, destinationId, TTL, sourcePort, destinationPort, sequenceNumber,
                acknowledgementNumber, NS, CWR, ECE, URG, ACK, PSH, RST, SYN, FIN, windowSize, priority);
        //TODO Auto-generated constructor stub
    }
    
    public void setMetaNodeToken(MetaNodeToken token){
        this.token = token;
    }

    public void setServerToken(ServerToken serverToken){
        this.serverToken = serverToken;
    }
}
