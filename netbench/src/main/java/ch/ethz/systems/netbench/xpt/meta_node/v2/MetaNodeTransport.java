package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.xpt.simple.simpletcp.SimpleTcpSocket;

import java.util.HashSet;
import java.util.LinkedList;

public class MetaNodeTransport extends TransportLayer {
    HashSet<Long> flowsInEpoch;

    public MetaNodeTransport(int identifier, NBProperties configuration) {
        super(identifier, configuration);
        flowsInEpoch = new HashSet<>();
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new MetaNodeSocket(this, flowId, this.identifier, destinationId, flowSizeByte, configuration);
    }


    protected Socket createSocket(long flowId, int sourceId, int destinationId, long flowSizeByte) {
        return new MetaNodeSocket(this, flowId, sourceId, destinationId, flowSizeByte, configuration);
    }

    @Override
    public void receive(Packet genericPacket) {

        MNEpochPacket packet = (MNEpochPacket) genericPacket;
        Socket socket = flowIdToSocket.get(packet.getFlowId());
        // If the socket does not yet exist, it is an incoming socket
        if (socket == null && !finishedFlowIds.contains(packet.getFlowId())) {
            socket = createSocket(packet.getFlowId(), packet.getSourceId(), -1, -1);
//            flowIdToReceiver.put(packet.getFlowId(), this);
            flowIdToSocket.put(packet.getFlowId(), socket);
            socket.markAsReceiver();
        }

        // Give packet to socket (we do not care about stray packets)
        if (socket != null) {
            socket.handle(packet);
        }
    }

    public void startEpoch() {
        flowsInEpoch.clear();
    }

    public void startEpoch(long flowId, long dataSizeByte) {
        flowsInEpoch.add(flowId);
        MetaNodeSocket socket = (MetaNodeSocket) flowIdToSocket.get(flowId);
        socket.sendPackets(dataSizeByte);
    }

    public void registerDemands(long maxBits) {
        for (Socket socket : flowIdToSocket.values()) {
            MetaNodeSocket metaNodeSocket = (MetaNodeSocket) socket;
            if (metaNodeSocket.isReceiver()) continue;
            long bits = Math.min(maxBits, metaNodeSocket.getRemainingFlowSize() * 8);
            maxBits -= bits;
            MNEpochController.getInstance().registerDemand(this.identifier, metaNodeSocket.getDestId(), bits, metaNodeSocket.getFlowId(), metaNodeSocket.startTime);
            if (maxBits <= 0) return;
        }
    }

    public void createReceiverSocket(long flowId, int identifier, int destinationId, long flowSizeByte) {

//            flowIdToReceiver.put(packet.getFlowId(), this);
        Socket socket = createSocket(flowId, identifier, this.identifier, flowSizeByte);
        socket.markAsReceiver();
        flowIdToSocket.put(flowId, socket);
    }

    public void pullPacket(long flowId) {
        if (!flowsInEpoch.contains(flowId)) return;
        MetaNodeSocket socket = (MetaNodeSocket) flowIdToSocket.get(flowId);
        socket.pullPacket();
    }

    private class MetaNodeSocket extends SimpleTcpSocket {
        private LinkedList<Packet> queue;
        MetaNodeServer serverDest;
        MetaNodeServer serverSource;
        private final long startTime;

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
        public MetaNodeSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, NBProperties configuration) {
            super(transportLayer, flowId, sourceId, destinationId, flowSizeByte, -1, configuration, -1);
            queue = new LinkedList<>();
            startTime = Simulator.getCurrentTime();
        }

        protected void initLogger() {
            // Initialize logger
            if (sourceId != this.transportLayer.getNetworkDevice().getIdentifier()) {
                super.initLogger();
            }
        }

        @Override
        public void logSender(long bytes) {
            if (!isReceiver) return;
            super.logSender(bytes);
        }


        @Override
        public void start() {
            serverDest = (MetaNodeServer) MNEpochController.getInstance().getDevice(destinationId);
            serverSource = (MetaNodeServer) transportLayer.getNetworkDevice();
            ((MetaNodeTransport) serverDest.getTransportLayer()).createReceiverSocket(flowId, identifier, destinationId, flowSizeByte);
        }

        @Override
        public void handle(Packet genericPacket) {
            MNEpochPacket tcpPacket = (MNEpochPacket) genericPacket;
            remainderToConfirmFlowSizeByte -= tcpPacket.getDataSizeByte();

            SimulationLogger.increaseStatisticCounter("RECEIVED_BYTES");

            logSender(tcpPacket.getDataSizeByte());
            if (isAllFlowConfirmed()) {
                onAllFlowConfirmed();

            }
        }

        @Override
        protected boolean isAllFlowConfirmed() {

            return remainderToConfirmFlowSizeByte == 0;
        }


        public void sendPackets(long dataSizeByte) {
            while (dataSizeByte > 0) {
                if (remainderToConfirmFlowSizeByte <= 0) break;

                long bytesToSend = Math.min(dataSizeByte, 1500l);
                bytesToSend = Math.min(bytesToSend, remainderToConfirmFlowSizeByte);

//                this.remainderToConfirmFlowSizeByte -= bytesToSend;
//                if(remainderToConfirmFlowSizeByte <= 0){
//                    transportLayer.cleanupSockets(flowId);
//                }
                dataSizeByte -= bytesToSend;
//                transportLayer.send(queue.pollFirst());
//                SimulationLogger.increaseStatisticCounter("SENT_OUT_BYTES");
                remainderToConfirmFlowSizeByte -= bytesToSend;
//                if(remainderToConfirmFlowSizeByte <= 0){
//                    transportLayer.cleanupSockets(flowId);
//                }
                queue.add(new MNEpochPacket(
                        flowId, bytesToSend, sourceId, destinationId,
                        remainderToConfirmFlowSizeByte, serverSource.getMNID(), serverDest.getMNID()
                ));
            }
            serverSource.pullPackets(this.getFlowId());

        }

        private void pullPacket() {
            MNEpochPacket packet = (MNEpochPacket) queue.pollFirst();
            if (packet == null) {
                return;
            }

            transportLayer.send(packet);
            SimulationLogger.increaseStatisticCounter("SENT_OUT_BYTES");


//            this.remainderToConfirmFlowSizeByte -= packet.getDataSizeByte();
            if (packet.getSequenceNumber() <= 0) {
                transportLayer.cleanupSockets(flowId);
                flowsInEpoch.remove(flowId);
            }
        }

        public long getFlowId() {
            return flowId;
        }

        public long getRemainingFlowSize() {
            return getRemainderToConfirmFlowSizeByte();
        }

        public int getDestId() {
            return destinationId;
        }

        @Override
        protected boolean isReceiver() {
            return isReceiver;
        }

    }


}
