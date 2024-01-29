package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.EmptyPortLogger;
import ch.ethz.systems.netbench.core.log.PortLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.ReconfigurationDeadlineException;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorNetController;

import java.util.Queue;

public class OperaOutputPort extends EcnTailDropOutputPort {
    private long mBitsEnqueued;
    private final long mMaxBitsPerInterval;
    private long nextReconfigurationTime;
    private OperaRotorSwitch rotor;
    /**
     * Constructor.
     *
     * @param ownNetworkDevice    Source network device to which this output port is attached
     * @param targetNetworkDevice Target network device that is on the other side of the link
     * @param link                Link that this output ports solely governs
     * @param queue               Queue that governs how packet are stored queued in the buffer
     * @param rotor
     */
    protected OperaOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, Queue<Packet> queue, OperaRotorSwitch rotor) {
        super(ownNetworkDevice, targetNetworkDevice, link, 15000000000l, 3000000000l, queue);
        mBitsEnqueued = 0;
        this.rotor = rotor;
        mMaxBitsPerInterval = OperaController.getInstance().getRemainingTimeToReconfigure() * link.getBandwidthBitPerNs();
        nextReconfigurationTime = OperaController.getInstance().getRemainingTimeToReconfigure();
    }

    @Override
    protected PortLogger createNewPortLogger() {
        return new EmptyPortLogger(this);
    }

    public void enqueue(Packet packet) {

        if (rotor.isReconfiguring()) {
            onConfigurationTimeExceeded();

        }
        if(!canSend(packet.getSizeBit())){
            onConfigurationTimeExceeded();
        }
        
        super.enqueue(packet);
    }

    protected void onConfigurationTimeExceeded() {
        throw new ReconfigurationDeadlineException();
    }


    @Override
    protected void dispatch(Packet packet) {
        ((OperaPacket)packet).advance();

        super.dispatch(packet);
    }


    @Override
    protected void log(Packet packet) {

    }

    @Override
    public String toString(){
        long finish = (getBufferOccupiedBits() )/link.getBandwidthBitPerNs();
        return super.toString();
    }


    public boolean configurationTimeExceeded(Packet packet) {
        long finish = (getBufferOccupiedBits() + packet.getSizeBit())/link.getBandwidthBitPerNs();
        return Simulator.getTimeFromNow(nextDispatchIn + finish) > OperaController.getInstance().getNextReconfigurationTIme();
    }

    public boolean canSend(long sizeBit) {
        long finish = (getBufferOccupiedBits() + sizeBit)/link.getBandwidthBitPerNs();
        if(Simulator.getTimeFromNow(nextDispatchIn + finish) > OperaController.getInstance().getNextReconfigurationTIme() ){
            return false;

        }
        return true;
    }
}
