package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.Packet;

import java.util.LinkedList;
import java.util.Queue;

public class EcnTailDropOutputPort extends OutputPort {

    protected final long ecnThresholdKBits;
    protected final long maxQueueSizeBits;
    protected final boolean mPrioritizeAcksOnCircuit;

    protected EcnTailDropOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        this(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes, new LinkedList<>());
    }

    protected EcnTailDropOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes, Queue queue) {
        super(ownNetworkDevice, targetNetworkDevice, link, queue);
        this.maxQueueSizeBits = maxQueueSizeBytes * 8L;
        this.ecnThresholdKBits = ecnThresholdKBytes * 8L;
        NBProperties configs = ownNetworkDevice.getConfiguration();
        if (configs != null) { // it might be null in testing
            mPrioritizeAcksOnCircuit = configs.getBooleanPropertyWithDefault(
                    "prioritize_acks_on_circuit",
                    false);
        } else {
            mPrioritizeAcksOnCircuit = false;
        }

    }

    /**
     * Enqueue the given packet.
     * Drops it if the queue is full (tail drop).
     *
     * @param packet Packet instance
     */
    @Override
    public void enqueue(Packet packet) {
        // Mark congestion flag if size of the queue is too big
        if (getBufferOccupiedBits() >= ecnThresholdKBits) {
            SimulationLogger.increaseStatisticCounter("MARK_CONGESTION_ECN_THRESHOLD");
            markECN(packet);
        }

        // Tail-drop enqueue
        if (hasBufferSpace(packet)) {
            guaranteedEnqueue(packet);
        } else {
            onPacketDropped(packet);
        }
    }

    protected void markECN(Packet packet) {
        // Convert to IP packet
        IpHeader ipHeader = (IpHeader) packet;
        if (!ipHeader.getECN()) logger.logECNMark();
        ipHeader.markCongestionEncountered();
    }

    @Override
    protected void addPacketToQueue(Packet packet) {
        try {
            TcpPacket tcpPacket = (TcpPacket) packet;
            if (mPrioritizeAcksOnCircuit && tcpPacket.isOnCircuit() && tcpPacket.isACK()) {
                ((LinkedList) queue).addFirst(tcpPacket);
                return;
            }
        } catch (ClassCastException e) {
        }
        super.addPacketToQueue(packet);
    }

    protected void onPacketDropped(Packet packet) {
        String suffix = "";
        IpHeader ipHeader = (IpHeader) packet;

        try {
            TcpPacket tcpPacket = (TcpPacket) ipHeader;
            if (tcpPacket.isACK()) {
                suffix = "ACK_";
            }
            tcpPacket = (TcpPacket) tcpPacket.deEncapsualte();
            ipHeader = tcpPacket != null ? tcpPacket : ipHeader;
        } catch (ClassCastException e) {
        }
        SimulationLogger.increaseStatisticCounter(suffix + "PACKETS_DROPPED");
        if (ipHeader.getSourceId() == this.getOwnId()) {
            SimulationLogger.increaseStatisticCounter(suffix + "PACKETS_DROPPED_AT_SOURCE");
        }

        if (ipHeader.getDestinationId() == this.getTargetId()) {
            SimulationLogger.increaseStatisticCounter(suffix + "PACKETS_DROPPED_AT_DESTINATION");
        }
    }

    protected boolean hasBufferSpace(Packet packet) {
        return (getBufferOccupiedBits() + packet.getSizeBit() <= maxQueueSizeBits);
    }

}
