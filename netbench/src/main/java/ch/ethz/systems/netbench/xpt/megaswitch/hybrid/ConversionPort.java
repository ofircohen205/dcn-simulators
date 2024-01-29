package ch.ethz.systems.netbench.xpt.megaswitch.hybrid;

import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServer;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedTransportLayer;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.SimpleDistributedSocket;
import org.apache.commons.lang3.tuple.ImmutablePair;

import ch.ethz.systems.netbench.core.log.EmptyPortLogger;
import ch.ethz.systems.netbench.core.log.PortLogger;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;
import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.ext.basic.IpPacket;

import java.util.HashSet;

/**
 * mimics a E2O conversion port
 * @author IK
 *
 */
public class ConversionPort extends EcnTailDropOutputPort{

    private boolean mDisable;
    private HashSet<Long> finishedFlows;
    private HashSet<Long> flows;
    public ConversionPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
        mDisable = false;
        finishedFlows = new HashSet<>();
        flows = new HashSet<>();

    }

    @Override
    protected void registerPacketArrivalEvent(Packet packet) {

        TcpPacket tcpPacket = (TcpPacket) packet;
        if(finishedFlows.contains(tcpPacket.getJumboFlowId())){
            // this shoud not happen, but might for some end cases
            System.out.println("packet transmitted after flow finished");
            System.out.println(packet.toString());
            System.out.println(packet.getFlowId());
            System.out.println(((TcpPacket)packet).resent);
            return;

        }
        targetNetworkDevice.receiveFromEncapsulating(packet);
    }

    @Override
    public void enqueue(Packet packet) {
        TcpPacket tcpPacket = (TcpPacket) packet;
        flows.add(packet.getFlowId());
        assert(tcpPacket.getJumboFlowId() != -1);
//        if(flows.size()  + 1 < queue.size()){
//            System.out.println("flows " + flows.size());
//            System.out.println("queue " + queue.size());
//            System.out.println("resent " + tcpPacket.resent);
//            System.out.println(tcpPacket.toString());
//            for(Packet p: queue){
//                System.out.println(p.getFlowId());
//            }
//            throw new RuntimeException();
//
//        }
        if(!finishedFlows.contains(tcpPacket.getJumboFlowId())) {
            super.enqueue(packet);
        }
        else{
        	/**
        	 * dont allow packets from finished flow on the circuit
        	 */
            System.out.println("flow is finished for " + packet.getFlowId());
            System.out.println(packet.toString());
        }


    }

    @Override
    protected void dispatch(Packet packet) {

        super.dispatch(packet);
//        try{
//            TcpPacket tcpPacket = (TcpPacket) packet;
//            DistributedOpticServer dos = (DistributedOpticServer) getOwnDevice();
//            DistributedTransportLayer tl = (DistributedTransportLayer) dos.getTransportLayer();
//            SimpleDistributedSocket socket = tl.getSocket(tcpPacket.getFlowId());
//            if(!tcpPacket.isSYN()) socket.sendNextDataPacket();
//
//        }catch (ClassCastException e){
//
//        }
    }

    protected PortLogger createNewPortLogger() {
		// TODO Auto-generated method stub
		return new EmptyPortLogger(this);
	}

    @Override
    protected void onPacketDropped(Packet packet) {
        SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED_ON_CONVERSION");
        super.onPacketDropped(packet);
    }
    


    public void onJumboFlowFinished(long jFlowId) {
        finishedFlows.add(jFlowId);
    }

    public void onFlowFinished(long flowId) {
        flows.remove(flowId);
    }
}
