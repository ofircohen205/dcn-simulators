package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;

public class MetaNodeTransport extends TransportLayer {
    public MetaNodeTransport(int identifier, NBProperties configuration) {
        super(identifier, configuration);
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte, long estimatedFlowSizeByte, int jobId) {
        return new MetaNodeSocket(this, flowId, identifier, destinationId, flowSizeByte);
    }

    public void createReceiverSocket(long flowId, int identifier, int destinationId, long flowSizeByte) {

//            flowIdToReceiver.put(packet.getFlowId(), this);
        Socket socket = new MetaNodeSocket(this, flowId, identifier, destinationId, flowSizeByte);
        socket.markAsReceiver();
        flowIdToSocket.put(flowId, socket);
    }

    public class MetaNodeSocket extends Socket {
        ServerToken serverToken;
        long tokenBytes = 0;
        public final long backoffTime;

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
        public MetaNodeSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte) {
            super(transportLayer, flowId, sourceId, destinationId, flowSizeByte, -1, -1);
            serverToken = ServerToken.dummyToken();
            backoffTime = configuration.getLongPropertyOrFail(Constants.MetaNode.TRANSPORT_BACKOFF_TIME_NS);
        }


        @Override
        public void start() {
            MNController.getInstance().createReceiverSocket(flowId, identifier, destinationId, flowSizeByte);
            sendData();
        }

        protected void initLogger() {
            // Initialize logger
            if (sourceId != this.transportLayer.getNetworkDevice().getIdentifier()) {
                super.initLogger();
            }
        }

        private void sendData() {
            long backoffTime = this.backoffTime;
            try {
                serverToken = MNController.getInstance().getServerToken(sourceId, destinationId, flowId, remainderToConfirmFlowSizeByte);
                SimulationLogger.increaseStatisticCounter("CONTROLLER_REQUESTS");
                tokenBytes = serverToken.bytes;
                backoffTime = MNController.getInstance().getMNTokenExpiryTime(tokenBytes);
                long tokenSize = tokenBytes;
                while (tokenSize > 0) {
                    long tcpPacketSize = 1380l;
                    long headers = 120l;
                    long packetSize = Math.min(tcpPacketSize, tokenSize);
                    packetSize = Math.min(packetSize, remainderToConfirmFlowSizeByte);
                    remainderToConfirmFlowSizeByte -= packetSize;

                    MetaNodePacket mnPacket = new MetaNodePacket(flowId, packetSize,
                            sourceId, destinationId, 100, -1, -1,
                            remainderToConfirmFlowSizeByte, -1,
                            false, false, false, false, false, false, false, false, false, 0l, 0l);
                    mnPacket.setMetaNodeToken(serverToken.getMetaNodeToken());
                    mnPacket.setServerToken(serverToken);
                    transportLayer.send(mnPacket);
                    SimulationLogger.increaseStatisticCounterBy("BYTES_SENT", mnPacket.getSizeBit() / 8);

                    tokenSize -= (packetSize + headers);
                    if (remainderToConfirmFlowSizeByte == 0) {
                        transportLayer.cleanupSockets(flowId);
                        return;
                    }
                }
            } catch (ServerOverloadedException e) {

            } finally {

            }

            Simulator.registerEvent(new Event(backoffTime) {
                @Override
                public void trigger() {

                    sendData();
                }
            });

        }

        @Override
        public void handle(Packet genericPacket) {
            TcpPacket tcpPacket = (TcpPacket) genericPacket;
            SimulationLogger.increaseStatisticCounterBy("BYTES_RECEIVED", genericPacket.getSizeBit() / 8);
            logSender(tcpPacket.getDataSizeByte());
            remainderToConfirmFlowSizeByte -= tcpPacket.getDataSizeByte();
            if (isAllFlowConfirmed()) {
                onAllFlowConfirmed();

            }
        }

        @Override
        public void logSender(long bytes) {
            if (!isReceiver) return;
            super.logSender(bytes);
        }


    }


}
