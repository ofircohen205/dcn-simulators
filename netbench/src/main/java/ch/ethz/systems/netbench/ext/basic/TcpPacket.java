package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

public abstract class TcpPacket extends IpPacket implements TcpHeader, Encapsulatable {

    // TCP header is [20, 60] bytes, assume maximum: 60 * 8
    private static final long TCP_HEADER_SIZE_BIT = 480L;

    // Actual fields
    private final int sourcePort;
    private final int destinationPort;
    private final long sequenceNumber;
    private final long acknowledgementNumber;
    private final boolean NS;
    private final boolean CWR;
    private final boolean ECE;
    private final boolean URG;
    private final boolean ACK;
    private final boolean PSH;
    private final boolean RST;
    private final boolean SYN;
    private final boolean FIN;
    private final double windowSize;
    private final long dataSizeByte;
    public boolean resent = false;
    protected int mColor = -1;
    // Mechanisms fields
    private int nonSequentialHash = -1;
    private int mPrevHop = -1;
    private boolean mOnCircuit;
    private long mJumboFlowId;
    
    public TcpPacket(
            long flowId, long dataSizeByte,
            int sourceId, int destinationId, int TTL, // IP header fields
            int sourcePort, int destinationPort, long sequenceNumber, long acknowledgementNumber,
            boolean NS, boolean CWR, boolean ECE, boolean URG, boolean ACK, boolean PSH,
            boolean RST, boolean SYN, boolean FIN, double windowSize
    ) {
        super(flowId, TCP_HEADER_SIZE_BIT + dataSizeByte * 8L, sourceId, destinationId, TTL);
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.sequenceNumber = sequenceNumber;
        this.acknowledgementNumber = acknowledgementNumber;
        this.NS = NS;
        this.CWR = CWR;
        this.ECE = ECE;
        this.URG = URG;
        this.ACK = ACK;
        this.PSH = PSH;
        this.RST = RST;
        this.SYN = SYN;
        this.FIN = FIN;
        this.windowSize = windowSize;
        this.dataSizeByte = dataSizeByte;
        mOnCircuit = false;
        mJumboFlowId = -1;

    }

    public TcpPacket(TcpPacket tcpPacket) {
        super(tcpPacket);
        this.sourcePort = tcpPacket.sourcePort;
        this.destinationPort = tcpPacket.destinationPort;
        this.sequenceNumber = tcpPacket.sequenceNumber;
        this.acknowledgementNumber = tcpPacket.acknowledgementNumber;
        this.NS = tcpPacket.NS;
        this.CWR = tcpPacket.CWR;
        this.ECE = tcpPacket.ECE;
        this.URG = tcpPacket.URG;
        this.ACK = tcpPacket.ACK;
        this.PSH = tcpPacket.PSH;
        this.RST = tcpPacket.RST;
        this.SYN = tcpPacket.SYN;
        this.FIN = tcpPacket.FIN;
        this.windowSize = tcpPacket.windowSize;
        this.dataSizeByte = tcpPacket.dataSizeByte;
        this.nonSequentialHash = tcpPacket.nonSequentialHash;
        this.resent = tcpPacket.resent;
        this.mPrevHop = tcpPacket.mPrevHop;
        this.mColor = tcpPacket.mColor;
        this.mOnCircuit = tcpPacket.mOnCircuit;
        this.mJumboFlowId = tcpPacket.mJumboFlowId;
    }

    @Override
    public long getDataSizeByte() {
        return dataSizeByte;
    }

    @Override
    public int getSourcePort() {
        return sourcePort;
    }

    @Override
    public int getDestinationPort() {
        return destinationPort;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public long getAcknowledgementNumber() {
        return acknowledgementNumber;
    }

    @Override
    public boolean isNS() {
        return NS;
    }

    @Override
    public boolean isCWR() {
        return CWR;
    }

    @Override
    public boolean isECE() {
        return ECE;
    }

    @Override
    public boolean isURG() {
        return URG;
    }

    @Override
    public boolean isACK() {
        return ACK;
    }

    @Override
    public boolean isPSH() {
        return PSH;
    }

    @Override
    public boolean isRST() {
        return RST;
    }

    @Override
    public boolean isSYN() {
        return SYN;
    }

    @Override
    public boolean isFIN() {
        return FIN;
    }

    @Override
    public double getWindowSize() {
        return windowSize;
    }

    @Override
    public int getNonSequentialHash() {
        return nonSequentialHash;
    }

    @Override
    public void setNonSequentialHash(int nonSeqHash) {
        nonSequentialHash = nonSeqHash;
    }

    @Override
    public String toString() {
        return "TCPPacket[" + getSourceId() + " -> " + getDestinationId() + ", DATA=" + this.getDataSizeByte() + "b, ACK=" + ACK + " (exp. ack.: " + (this.getSequenceNumber() + this.getDataSizeByte()) + "), createdAt=" + this.getDepartureTime() + ", seq: " + getSequenceNumber() + ", carryingAck: " + this.getAcknowledgementNumber() + ", SYN=" + this.isSYN() + ", FIN=" + this.isFIN() + "]";
    }


    public void markResent() {
        resent = true;
    }

    public int getPrevHop() {
        // TODO Auto-generated method stub
        return mPrevHop;
    }

    public void setPrevHop(int identifier) {
        mPrevHop = identifier;

    }

    public int getColor() {
        // TODO Auto-generated method stub
        return mColor;
    }

    public void color(int color) {
        mColor = color;

    }

    public TcpPacket markOnCircuit(boolean onCircuit) {
        mOnCircuit = onCircuit;
        return this;
    }

    public boolean isOnCircuit() {
        return mOnCircuit;
    }

    public long getJumboFlowId() {
        return mJumboFlowId;
    }

    public void setJumboFlowId(long id) {
        mJumboFlowId = id;
    }

}
