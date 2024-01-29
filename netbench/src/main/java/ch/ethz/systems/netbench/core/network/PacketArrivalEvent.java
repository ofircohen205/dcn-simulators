package ch.ethz.systems.netbench.core.network;

/**
 * Event for the complete arrival of a packet in its entirety.
 * Currently this does not support serialization:
 * To change this you will need to get the input port,
 * which for MegaSwitch means you will need to mark the packet
 * with the appropriate network interface. Previously this was done
 * with "technology" field, please see in network device.
 */
public class PacketArrivalEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -3494066931483272486L;
    private final int arrivalNetworkDeviceId;
    private final Packet packet;
    private final InputPort inputPort;

    /**
     * Packet arrival event constructor.
     *
     * @param timeFromNowNs Time in simulation nanoseconds from now
     * @param packet        Packet instance which will arrive
     * @param inputPort     The input port the package is arriving to
     */
    protected PacketArrivalEvent(long timeFromNowNs, Packet packet, InputPort inputPort) {
        super(timeFromNowNs);
        this.packet = packet;
        this.arrivalNetworkDeviceId = inputPort.getOwnNetworkDevice().getIdentifier();
        this.inputPort = inputPort;
    }

    @Override
    public void trigger() {
        inputPort.receive(packet);
    }

    @Override
    public String toString() {
        return "PacketArrivalEvent<" + arrivalNetworkDeviceId + ", " + this.getTime() + ", " + this.packet + ">";
    }

    /**
     * Get the packet instance which will arrive.
     *
     * @return The packet instance
     */
    public Packet getPacket() {
        return packet;
    }

    public int getArrivalNetworkDeviceId() {
        return arrivalNetworkDeviceId;
    }
}
