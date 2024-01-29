package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.OpticServerToR;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.FlowPathExists;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Path;

/**
 * the DistributedOpticServerToR handles reservation packets.
 * Its never meant to have an optic connection
 */
public class DistributedOpticServerToR extends OpticServerToR {
    public DistributedOpticServerToR(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
    }

    private void tryReserveResources(ReservationPacket rp) {
        if(rp.idDeAllocation()) { // dont reserve for deallocation packets
        	throw new NoPathException();
        }
       
        if(rp.isSuccess()){ // dont do double reservations
            throw new FlowPathExists(rp.getFlowId());
        }

        int color = rp.getColor();
        int nextHop = rp.getNextHop(this.getIdentifier());
        if(nextHop == rp.getServerDest()){
        		
            try{
                // this part doesnt necessarily need to be here. can be moved bellow
                ((DistributedController) getRemoteRouter()).updateRoutingTable(identifier,rp.getPrevHop(),nextHop,rp.getColor()); // update the routing table to point to server dest

            }catch(NoPathException e){
                rp.markFailure();
                rp.reverse();
                throw new NoPathException();
            }
            return;
        }
        long capacity = ((DistributedController) getRemoteRouter()).checkEdgeCapacity(this.identifier,nextHop,color);
        if(capacity==0){ // edge of this color already in use
            rp.markFailure();
            rp.reverse();
            SimulationLogger.increaseStatisticCounter("DISTRIBUTED_TOR_NO_PATH");
            throw new NoPathException();
        }

        assert(capacity==1);
        ((DistributedController) getRemoteRouter()).updateRoutingTable(this.identifier,rp.getPrevHop(),nextHop,rp.getColor()); // update the routing table to point to next hop
        ((DistributedController) getRemoteRouter()).decreaseEdgeCapacity(this.identifier,nextHop,color);

    }


    @Override
    public void receive(Packet genericPacket) {

        try{
            handleReservationPacket(genericPacket);
            SimulationLogger.increaseStatisticCounter("CIRCUIT_RESERVATION_PACKET_PROCESSED");
            return;
        }catch (ClassCastException e){

        }

       super.receive(genericPacket);
    }

    protected void handleReservationPacket(Packet genericPacket) {
        ReservationPacket rp = (ReservationPacket) genericPacket;
        try {
        	tryReserveResources(rp);
        }catch (FlowPathExists e){

        }catch (NoPathException e) {
        	deallocateReservation(rp);

        }

        int nextHop = rp.getNextHop(this.getIdentifier());
        if(getTargetOuputPort(nextHop) != null){//if this is true then we must be at the final stage
            getTargetOuputPort(nextHop).enqueue(rp);
            return;
        }
        rp.setPrevHop(this.identifier);

        DistributedProtocolPort port = (DistributedProtocolPort) this.electronic.getTargetOuputPort(nextHop);
        port.enqueue(rp);
    }

    private void deallocateReservation(ReservationPacket rp) {
        int color = rp.getColor();
        int nextHop = rp.getNextHop(this.getIdentifier());
        if(nextHop != rp.getServerDest()){ // increase the edge capacity for network edges

            /**
             * the edge to increase capacity for depends if we are reversed or not
             */
            int l = rp.isReversed() ? nextHop : this.identifier;
            int r = rp.isReversed() ? this.identifier : nextHop;

            if(((DistributedController) getRemoteRouter()).checkEdgeCapacity(l,r,color)!=0){
                System.out.println(rp);
                System.out.println(((DistributedController) getRemoteRouter()).checkEdgeCapacity(l,r,color));
                System.out.println(rp.getFlowId());
                System.out.println("l " + l + " r " + r);

                throw new RuntimeException();
            }
            ((DistributedController) getRemoteRouter()).increaseEdgeCapacity(l,r,color);
        }else{

            
        }

    }

    @Override
    public boolean hadlePacketFromEncapsulating(Packet packet) {
        try{
            handleReservationPacket(packet);
        }catch (ClassCastException e){
            return false;
        }
        return true;
    }
}
