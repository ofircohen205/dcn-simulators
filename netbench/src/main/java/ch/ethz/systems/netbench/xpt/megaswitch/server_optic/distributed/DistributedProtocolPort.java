package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;
import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The distributed port's main function is to allow
 * high priority to reservation packets and disallow
 * reservation packets to be dropped.
 */
public class DistributedProtocolPort extends EcnTailDropOutputPort {
	private int numRpPackets; // initally used for debugging
    public DistributedProtocolPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes, new LinkedList<Packet>());
        numRpPackets = 0;
    }
    
    protected void log(Packet packet) {
    	super.log(packet);
		
	}

    /**
     * adds reservation packets to front of queue
     * @param packet
     */
    @Override
    protected void addPacketToQueue(Packet packet){
        try{
            ReservationPacket rp = (ReservationPacket) packet;
            ((LinkedList<Packet>)queue).addFirst(packet);
            numRpPackets++;
            return;
        }catch (ClassCastException e){

        }
        TcpPacket tcpPacket = (TcpPacket) packet;
        if(tcpPacket.isACK()){
            ((LinkedList<Packet>)queue).addFirst(packet);
            return;
        }
        super.addPacketToQueue(packet);
    }
    
    protected Packet popFromQueue() {
		// TODO Auto-generated method stub
    	Packet p = super.popFromQueue();
    	try{
            ReservationPacket rp = (ReservationPacket) p;
            numRpPackets--;
    	}catch(ClassCastException e) {
    		
    	}
    	
		return p;
	}

    /**
     * always return true if packet is a reservation packet
     * @param packet
     * @return
     */
    @Override
    protected boolean hasBufferSpace(Packet packet) {
    	 try{
             ReservationPacket rp = (ReservationPacket) packet;
             return true;
         }catch (ClassCastException e){

         }
         return super.hasBufferSpace(packet);
    	
    }
}
