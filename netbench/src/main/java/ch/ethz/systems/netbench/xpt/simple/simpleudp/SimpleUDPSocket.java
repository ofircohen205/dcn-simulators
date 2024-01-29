package ch.ethz.systems.netbench.xpt.simple.simpleudp;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.bare.BareSocket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;

public class SimpleUDPSocket extends BareSocket {
    private final int mNetworkDeviceLinkBw;
    private final long flowStartTime;
    private long flowByteReceived;
    private boolean enableFairness;

    /**
     * Create a socket. By default, it should be the receiver.
     * Use the {@link #start() start} method to make the socket a
     * sender and initiate the communication handshake.
     *
     * @param transportLayer Transport layer
     * @param flowId
     * @param sourceId       Source network device identifier
     * @param destinationId  Target network device identifier
     * @param flowSizeByte   Size of the flow in bytes
     */
    public SimpleUDPSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, long startTime, NBProperties configuration) {
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte);
        mNetworkDeviceLinkBw = transportLayer.getNetworkDevice()
                .getConfiguration().getIntegerPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);
        flowStartTime = startTime;
        flowByteReceived = 0;
        enableFairness = configuration.getBooleanPropertyWithDefault(Constants.UDP.ENABLE_FAIR_UDP, false);
    }

    @Override
    protected void initLogger() {

    }


    @Override
    public void start() {
        sendPacket();
    }

    private void sendPacket() {
        if (!isAllFlowConfirmed()) {
            final long next = Math.min(this.MAX_SEGMENT_SIZE, getRemainderToConfirmFlowSizeByte());
            long seqNum = getRemainderToConfirmFlowSizeByte() <= MAX_SEGMENT_SIZE ? flowSizeByte : 0;
            Packet p = this.createPacket(next, seqNum, 0, false, false);

//            System.out.println("sending " + next + " for flow " + flowId + " at time " + Simulator.getCurrentTime());
            transportLayer.send(p);

            SimulationLogger.increaseStatisticCounter("UDP_PACKET_SENT");
            long sendTime = p.getSizeBit() / mNetworkDeviceLinkBw;
            if (enableFairness) sendTime *= transportLayer.getNumOpenSockets();

            Simulator.registerEvent(new Event(sendTime) {
                @Override
                public void trigger() {
                    confirmFlow(next);
                    continueFlow();
                }
            });
        }

    }

    @Override
    protected void onAllFlowConfirmed() {

        transportLayer.cleanupSockets(flowId);
        if (!this.isReceiver) return;
        // this is some block that should really be moved
        // but its the best way to tell if a flow is finished
        boolean exendedTopology = true;
        int destToRId = -1;
        int sourceToRId = -1;
        try {
            sourceToRId = transportLayer.getNetworkDevice().getConfiguration().getGraphDetails().getTorIdOfServer(destinationId);
            destToRId = transportLayer.getNetworkDevice().getConfiguration().getGraphDetails().getTorIdOfServer(sourceId);
        } catch (NullPointerException e) {
            exendedTopology = false;
        }
        if (exendedTopology) {
            try {
                MegaSwitch ms = (MegaSwitch) BaseInitializer.getInstance().getIdToNetworkDevice().get(sourceToRId);
                if (ms != null)
                    ms.onFlowFinished(sourceToRId, destToRId, destinationId, sourceId, flowId);
            } catch (ClassCastException e) {

            }
        }


        Simulator.registerFlowFinished(flowId);

    }

    @Override
    public void handle(Packet genericPacket) {
        TcpPacket packet = (TcpPacket) genericPacket;
        privateLogger.logFlowAcknowledged(packet.getDataSizeByte());
        flowByteReceived += packet.getDataSizeByte();
        if (flowByteReceived >= flowSizeByte) {

            this.onAllFlowConfirmed();
        }
    }

    public void continueFlow() {
        sendPacket();
    }

    @Override
    protected void logSender(long newlyConfirmedFlowByte) {

    }

    @Override
    public void markAsReceiver() {
        // Initialize logger
        this.privateLogger = new UDPFlowLogger(flowId, destinationId, sourceId, flowSizeByte, flowStartTime);
    }

}
