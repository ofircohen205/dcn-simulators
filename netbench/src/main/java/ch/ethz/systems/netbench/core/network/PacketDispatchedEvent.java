package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;

/**
 * Event for the dispatch of a packet, i.e. when all of the bits
 * of the packet have been written to the link.
 * Currently this does not support serialization:
 * To change this you will need to get the output port,
 * which for MegaSwitch means you will need to mark the packet
 * with the appropriate network interface. Previously this was done
 * with "technology" field, please see in network device.
 */
public class PacketDispatchedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = 8099553808355024992L;
    protected final int deviceId;
    private final Packet packet;
    private final int targetId;
    private final OutputPort dispatchPort;

    /**
     * Packet dispatched event constructor.
     *
     * @param timeFromNowNs Time in simulation nanoseconds from now
     * @param packet        Packet instance which is dispatched
     * @param dispatchPort  Port that has finished writing the packet to the link
     */
    public PacketDispatchedEvent(long timeFromNowNs, Packet packet, OutputPort dispatchPort) {
        super(timeFromNowNs);
        this.packet = packet;
        this.targetId = dispatchPort.getTargetId();
        this.deviceId = dispatchPort.getOwnId();
        this.dispatchPort = dispatchPort;
    }

    @Override
    public void trigger() {
        dispatchPort.dispatch(packet);
        //NetworkDevice nd = getOwnDevice();
        //getOutputPort(nd).dispatch(packet);
    }

    protected NetworkDevice getOwnDevice() {
        return BaseInitializer.getInstance().getNetworkDeviceById(this.deviceId);
    }

    protected OutputPort getOutputPort(NetworkDevice nd) {
        return nd.getTargetOuputPort(targetId);
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "PacketDispatchedEvent<" + deviceId + " -> " + this.targetId + ", " + this.getTime() + ", " + this.packet + ">";
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getTargetId() {
        return targetId;
    }
}
