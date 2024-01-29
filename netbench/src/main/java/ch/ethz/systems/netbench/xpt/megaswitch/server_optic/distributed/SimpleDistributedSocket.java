package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.simple.simpledctcp.SimpleDctcpSocket;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

public class SimpleDistributedSocket extends SimpleDctcpSocket {

    private boolean mOnCircuit;

    /**
     * Create a TCP socket. By default, it is the receiver.
     * Use the {@link #start() start} method to make the socket a
     * sender and initiate the communication handshake.
     *
     * @param transportLayer Transport layer
     * @param flowId         Flow identifier
     * @param sourceId       Source network device identifier
     * @param destinationId  Target network device identifier
     * @param flowSizeByte   Size of the flow in bytes
     * @param configuration
     */
    SimpleDistributedSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, NBProperties configuration, int jobId) {
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte, -1, configuration, jobId);
//        mOnCircuit = false;
//        DistributedOpticServer dos  = (DistributedOpticServer) transportLayer.getNetworkDevice();
//        if(dos.hasCircuitTo(destinationId)){
//            mOnCircuit = true;
//        }
    }

//    @Override
//    protected void registerResendEvent(TcpPacket tcpPacket) {
//        if(!mOnCircuit) super.registerResendEvent(tcpPacket);
//    }

//    @Override
//    protected void handleECEMarkedPacket() {
//        if(!mOnCircuit){
//            super.handleECEMarkedPacket();
//        }
//    }

//    @Override
//    protected void incrementCongestionWindow(int newPacketsAcked) {
//
//        if(mOnCircuit){
//            if(congestionWindow<30000){
//                congestionWindow = 30000;
//            }
//
//        }
//        super.incrementCongestionWindow(newPacketsAcked);
//        if(flowId==5339L){
//            System.out.println(5339L + " is on circuit? " + mOnCircuit);
//            System.out.println(5339L + " is on circuit: " + SimulationLogger.isFlowOnCircuit(5339L));
//            System.out.println("incrementCongestionWindow result " + congestionWindow);
//        }
//
//    }

//    @Override
//    protected void sendPendingData() {
//        if(!mOnCircuit) super.sendPendingData();
//    }
//
//    public void markOnCircuit() {
//        mOnCircuit = true;
//    }

//    public void sendNextDataPacket() {
//        long amountToSendByte = getFlowSizeByte(sendNextNumber);;
//        while (acknowledgedSegStartSeqNumbers.contains(sendNextNumber)) {
//            amountToSendByte = getFlowSizeByte(sendNextNumber);
//            sendNextNumber += amountToSendByte;
//        }
//        if(!finSent) super.sendOutDataPacket(sendNextNumber,amountToSendByte);
//    }

//    @Override
//    protected void sendAcknowledgment(TcpPacket packet) {
//
//        if(!packet.isOnCircuit() || finReceived){
//            super.sendAcknowledgment(packet);
//        }
//
//    }

//    @Override
//    protected void onEstablishedHandle() {
//        if(mOnCircuit) {
//            sendNextDataPacket();
//        }else{
//            super.onEstablishedHandle();
//        }
//    }

//    @Override
//    protected void cancelResendEvent(long seq) {
//        try{
//            super.cancelResendEvent(seq);
//        }catch(IllegalStateException e){
//
//        }
//    }

    @Override
    protected void handleECEMarkedPacket(TcpPacket packet) {
        if (!packet.isOnCircuit()) super.handleECEMarkedPacket(packet);
    }

    @Override
    protected void incrementCongestionWindow(int newPacketsAcked) {
        if (mOnCircuit) return;
        super.incrementCongestionWindow(newPacketsAcked);
    }

    @Override
    protected void handleAcknowledgment(FullExtTcpPacket packet) {
        if (packet.isOnCircuit()) {
            mOnCircuit = true;
            congestionWindow = 75000;
        }
        super.handleAcknowledgment(packet);
    }
}
