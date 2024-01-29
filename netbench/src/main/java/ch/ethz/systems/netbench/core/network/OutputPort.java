package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.EmptyPortLogger;
import ch.ethz.systems.netbench.core.log.PortLogger;
import ch.ethz.systems.netbench.core.utility.Constants;

import java.util.Queue;

/**
 * Abstraction for an output port on a network device.
 * <p>
 * There is no corresponding InputPort class, as the output
 * port already forces a rate limit. OutputPort's subclasses
 * are forced to handle the enqueuing of packets, and are allowed
 * to drop packets depending on their own drop strategy to handle
 * congestion at the port (e.g. tail-drop, RED, ...).
 */
public abstract class OutputPort extends Port {

    protected long nextDispatchIn;
    // Internal state
    protected boolean isSending;          // True iff the output port is using the medium to send a packet
    protected final Queue<Packet> queue;  // Current queue of packets to send
    private long bufferOccupiedBits;    // Amount of bits currently occupied of the buffer

    // Constants
    private final int ownId;                            // Own network device identifier
    private final int targetId;                         // Target network device identifier
    protected final NetworkDevice targetNetworkDevice;    // Target network device
    protected final Link link;                            // Link type, defines latency and bandwidth of the medium
    // that the output port uses
    // Logging utility
    protected final PortLogger logger;
    private InputPort inputPort;

    /**
     * Constructor.
     *
     * @param ownNetworkDevice    Source network device to which this output port is attached
     * @param targetNetworkDevice Target network device that is on the other side of the link
     * @param link                Link that this output ports solely governs
     * @param queue               Queue that governs how packet are stored queued in the buffer
     */
    protected OutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, Queue<Packet> queue) {
        this.nextDispatchIn = 0;
        // State
        this.queue = queue;
        this.isSending = false;
        this.link = link;
        this.bufferOccupiedBits = 0;

        // References
        this.ownNetworkDevice = ownNetworkDevice;
        this.ownId = this.ownNetworkDevice.getIdentifier();
        this.targetNetworkDevice = targetNetworkDevice;
        this.targetId = this.targetNetworkDevice.getIdentifier();
        if (ownNetworkDevice.configuration != null
                && ownNetworkDevice.configuration.getBooleanPropertyWithDefault(
                Constants.Logger.LOG_PORT_UTILIZATION,
                false)) {
            // Logging
            this.logger = createNewPortLogger();
        } else {
            this.logger = new EmptyPortLogger(this);
        }


    }

    protected PortLogger createNewPortLogger() {
        // TODO Auto-generated method stub
        return new PortLogger(this);
    }

    protected void registerPacketDispatchedEvent(Packet packet) {

        nextDispatchIn = getDispatchTime(packet);
        Simulator.registerEvent(new PacketDispatchedEvent(
                getDispatchTime(packet),
                packet,
                this
        ));
    }

    protected long getDispatchTime(Packet packet) {
        return packet.getSizeBit() / link.getBandwidthBitPerNs();
    }

    /**
     * Enqueue the given packet for sending.
     * There is no guarantee that the packet is actually sent,
     * as the queue buffer's limit might be reached.
     *
     * @param packet Packet instance
     */
    public abstract void enqueue(Packet packet);

    /**
     * Enqueue the given packet.
     *
     * @param packet Packet instance
     */
    protected final void guaranteedEnqueue(Packet packet) {
        // If it is not sending, then the queue is empty at the moment,
        // so this packet can be immediately send
        if (!isSending) {
            // Link is now being utilized
            logger.logLinkUtilized(true);
            // Add event when sending is finished
            registerPacketDispatchedEvent(packet);
            // It is now sending again
            isSending = true;
        } else { // If it is still sending, the packet is added to the queue, making it non-empty
            bufferOccupiedBits += packet.getSizeBit();
            addPacketToQueue(packet);
            log(packet);
        }
    }

    protected void log(Packet packet) {
        logger.logQueueState(queue.size(), bufferOccupiedBits, packet);
    }

    protected void addPacketToQueue(Packet packet) {
        queue.add(packet);
    }

    protected void registerPacketArrivalEvent(Packet packet) {
        getTargetInputPort().registerPacketArrivalEvent(packet);
    }

    protected InputPort getTargetInputPort() {
        return targetNetworkDevice.getSourceInputPort(this.getOwnId());
    }

    /**
     * Called when a packet has actually been send completely.
     * In response, register arrival event at the destination network device,
     * and starts sending another packet if it is available.
     *
     * @param packet Packet instance that was being sent
     */
    protected void dispatch(Packet packet) {
        logger.logPacketDispatch(packet);
        // Finished sending packet, the last bit of the packet should arrive the link-delay later
        if (!link.doesNextTransmissionFail(packet.getSizeBit())) {
            registerPacketArrivalEvent(packet);
        }

        // Again free to send other packets
        isSending = false;
        nextDispatchIn = 0;
        // Check if there are more in the queue to send
        if (!queue.isEmpty()) {
            // Pop from queue
            Packet packetFromQueue = popFromQueue();
            decreaseBufferOccupiedBits(packetFromQueue.getSizeBit());
            logger.logQueueState(queue.size(), bufferOccupiedBits, packetFromQueue);
            // Register when the packet is actually dispatched
            registerPacketDispatchedEvent(packetFromQueue);

            // It is sending again
            isSending = true;
        } else {
            // If the queue is empty, nothing will be sent for now
            logger.logLinkUtilized(false);
        }
    }

    protected Packet popFromQueue() {
        // TODO Auto-generated method stub
        return queue.poll();
    }

    /**
     * Return the network identifier of its own device (to which this output port is attached to).
     *
     * @return Own network device identifier
     */
    public int getOwnId() {
        return ownId;
    }

    /**
     * Return the network identifier of the target device.
     *
     * @return Target network device identifier
     */
    public int getTargetId() {
        return targetId;
    }

    /**
     * Return the network device where this ports originates from.
     *
     * @return Own network device
     */
    public NetworkDevice getOwnDevice() {
        return ownNetworkDevice;
    }

    /**
     * Return the network device at the other end of this port.
     *
     * @return Target network device
     */
    public NetworkDevice getTargetDevice() {
        return targetNetworkDevice;
    }

    /**
     * Retrieve size of the queue in packets.
     *
     * @return Queue size in packets
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Retrieve size of the queue in packets.
     *
     * @return Queue size in packets
     */
    public long getQueueSizeBit() {
        return bufferOccupiedBits;
    }

    /**
     * Determine the amount of bits that the buffer occupies.
     *
     * @return Bits currently occupied in the buffer of this output port.
     */
    protected long getBufferOccupiedBits() {
        return bufferOccupiedBits;
    }

    @Override
    public String toString() {
        return "OutputPort<" +
                ownId + " -> " + targetId +
                ", link: " + link +
                ", occupied: " + bufferOccupiedBits +
                ", queue size: " + getQueueSize() +
                ">";
    }

    /**
     * Retrieve the queue used in the output port.
     * <p>
     * NOTE: adapting the queue will most likely result in strange values
     * in the port queue state log.
     *
     * @return Queue instance
     */
    protected Queue<Packet> getQueue() {
        return queue;
    }

    /**
     * Change the amount of bits occupied in the buffer with a delta.
     * <p>
     * NOTE: adapting the buffer occupancy counter from your own implementation
     * will most likely result in strange values in the port queue state log.
     *
     * @param deltaAmount Amount of bits to from the current occupied counter
     */
    protected void decreaseBufferOccupiedBits(long deltaAmount) {
        bufferOccupiedBits -= deltaAmount;
        assert (bufferOccupiedBits >= 0);
    }

}
