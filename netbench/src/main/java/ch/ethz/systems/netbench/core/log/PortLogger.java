package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.utility.Constants;

public class PortLogger {

    // Port information
    private final int ownId;
    private final int targetId;
    private final boolean attachedToServer;
    private long packetsDispatched;
    // Utilization logging variables
    private long lastUtilizedChange = 0;
    private long utilizedNs = 0;
    private boolean currentlyBeingUtilized = false;

    // Queue length logging variables
    private static final long STATISTIC_SAMPLE_RATE = 30;
    private final boolean logQueueStateEnabled;
    private long iterator = 0;
    private long ecnMarks;

    /**
     * Create logger for the given port.
     *
     * @param port Output port instance
     */
    public PortLogger(OutputPort port) {
        this.ownId = port.getOwnId();
        this.targetId = port.getTargetId();
        this.attachedToServer = port.getOwnDevice().isServer() || port.getTargetDevice().isServer();
        this.registerSelf();
        this.ecnMarks = 0;
        this.packetsDispatched = 0;
        if (port.getOwnDevice().getConfiguration() != null) {
            this.logQueueStateEnabled = port
                    .getOwnDevice()
                    .getConfiguration()
                    .getBooleanPropertyWithDefault(Constants.Logger.ENABLE_LOG_PORT_QUEUE_STATE, false);

        } else {
            this.logQueueStateEnabled = false;
        }
    }

    protected void registerSelf() {
        SimulationLogger.registerPortLogger(this);
    }

    /**
     * Log the current queue length of the output port.
     *
     * @param length             Current queue length in packets
     * @param bufferOccupiedBits Amount of bits occupied in the buffer
     * @param packet             the most recent packet from the queue
     */
    public void logQueueState(int length, long bufferOccupiedBits, Packet packet) {
        if (this.logQueueStateEnabled) {
            iterator++;
            if (iterator % STATISTIC_SAMPLE_RATE == 0) { // TODO: get rid of statistic sample rate?
                SimulationLogger.logPortQueueState(ownId, targetId, length, bufferOccupiedBits, packet, Simulator.getCurrentTime());
            }
        }
    }

    /**
     * Log the change the state of the port.
     *
     * @param beingUtilized Whether it is being utilized right now
     */
    public void logLinkUtilized(boolean beingUtilized) {
        if (!beingUtilized) {
            utilizedNs += Simulator.getCurrentTime() - lastUtilizedChange;
        }
        currentlyBeingUtilized = beingUtilized;
        lastUtilizedChange = Simulator.getCurrentTime();
    }

    /**
     * Get own network device identifier (where port originates from).
     *
     * @return Own network device identifier
     */
    int getOwnId() {
        return ownId;
    }

    /**
     * Get target network device identifier (where port goes to).
     *
     * @return Target network device identifier
     */
    int getTargetId() {
        return targetId;
    }

    /**
     * Check whether either of the network devices that this port
     * is connected to is a server.
     *
     * @return True iff one or both of the network devices is a server
     */
    boolean isAttachedToServer() {
        return attachedToServer;
    }

    /**
     * Retrieve how much nanoseconds this port has been utilized.
     *
     * @return Utilization in nanoseconds of the total run time
     */
    long getUtilizedNs() {
        return utilizedNs + (currentlyBeingUtilized ? Simulator.getCurrentTime() - lastUtilizedChange : 0);
    }

    public void logECNMark() {
        this.ecnMarks++;
    }

    public long getECNMarks() {
        return this.ecnMarks;
    }

    public void logPacketDispatch(Packet packet) {
        packetsDispatched++;
    }

    public long getPacketsDispatched() {
        return packetsDispatched;
    }

}