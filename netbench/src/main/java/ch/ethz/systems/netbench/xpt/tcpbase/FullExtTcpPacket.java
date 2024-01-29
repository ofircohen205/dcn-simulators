package ch.ethz.systems.netbench.xpt.tcpbase;

import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

import java.util.Collection;

public class FullExtTcpPacket extends TcpPacket implements SelectiveAckHeader, EchoHeader, PriorityHeader {

    private long priority;
    private Collection<AckRange> selectiveAck;
    private long echoDepartureTime;
    private int echoFlowletId;

    public FullExtTcpPacket(long flowId, long dataSizeByte, int sourceId, int destinationId, int TTL, int sourcePort, int destinationPort, long sequenceNumber, long acknowledgementNumber, boolean NS, boolean CWR, boolean ECE, boolean URG, boolean ACK, boolean PSH, boolean RST, boolean SYN, boolean FIN, double windowSize, long priority) {
        super(flowId, dataSizeByte, sourceId, destinationId, TTL, sourcePort, destinationPort, sequenceNumber, acknowledgementNumber, NS, CWR, ECE, URG, ACK, PSH, RST, SYN, FIN, windowSize);
        this.priority = priority;
    }

    public FullExtTcpPacket(FullExtTcpPacket fullExtTcpPacket) {
		super(fullExtTcpPacket);
		this.priority = fullExtTcpPacket.priority;
		this.selectiveAck = fullExtTcpPacket.selectiveAck;
		this.echoDepartureTime = fullExtTcpPacket.echoDepartureTime;
		this.echoFlowletId = fullExtTcpPacket.echoFlowletId;
	}

	@Override
    public TcpPacket setEchoDepartureTime(long echoDepartureTime) {
        this.echoDepartureTime = echoDepartureTime;
        return this;
    }

    @Override
    public long getEchoDepartureTime() {
        return echoDepartureTime;
    }

    @Override
    public TcpPacket setEchoFlowletId(int echoFlowletId) {
        this.echoFlowletId = echoFlowletId;
        return this;
    }

    @Override
    public int getEchoFlowletId() {
        return echoFlowletId;
    }

    @Override
    public TcpPacket setSelectiveAck(Collection<AckRange> selectiveAck) {
        this.selectiveAck = selectiveAck;
        return this;
    }

    @Override
    public Collection<AckRange> getSelectiveAck() {
        return this.selectiveAck;
    }

    @Override
    public long getPriority() {
        return priority;
    }

    @Override
    public void increasePriority() {
        priority++;
    }

    @Override
    public void setPriority(long val) {
        priority = val;
    }

    @Override
	public Encapsulatable encapsulate(final int newSource,final int newDestination) {
		// TODO Auto-generated method stub
		return new FullExtTcpPacket(this){
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public int getDestinationId() {
				return newDestination;
			}

            @Override
            public int getSourceId() {
			    return newSource;
            }
			
		};
	}
    
    @Override
	public Encapsulatable deEncapsualte() {
    	return new FullExtTcpPacket(this);
	}

}
