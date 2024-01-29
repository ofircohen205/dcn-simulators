package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.FlowLogger;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.run.traffic.Flow;
import ch.ethz.systems.netbench.core.state.SimulatorStateSaver;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.deeplearningtraining.state.Job;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * The transport layer represents the entity that communicates
 * over the network. It belongs to a single {@link NetworkDevice network
 * device}.
 * It starts a new {@link Socket socket} for each flow originating from here,
 * and creates a new receiving socket for each packet it receives with
 * an unfamiliar flow identifier.
 *
 * @see NetworkDevice
 * @see Socket
 */
public abstract class TransportLayer {

    // Generator for unique flow identifiers amongst all transport layers
    protected final int identifier;
    // private static Map<Long, TransportLayer> flowIdToReceiver = new HashMap<>();
    protected NBProperties configuration;
    // Map the flow identifier to the responsible socket
    protected Map<Long, Socket> flowIdToSocket;
    protected Set<Long> finishedFlowIds;
    protected NetworkDevice networkDevice;

    public TransportLayer(int identifier, NBProperties configuration) {
        this.configuration = configuration;
        this.identifier = identifier;
        this.flowIdToSocket = new HashMap<>();
        this.finishedFlowIds = new HashSet<>();
    }

    /**
     * Reset the static run state.
     */
    public static void staticReset() {
//        flowIdCounter = 0;
        // flowIdToReceiver.clear();
    }

    public static void dumpState(String dumpFolderName) throws IOException {
        JSONObject obj = new JSONObject();
//        obj.put("flowIdCounter", flowIdCounter);
        FileWriter file = new FileWriter(dumpFolderName + "/" + "transport_layer_data.json");
        file.write(obj.toJSONString());
        file.flush();

    }

    public static void restorState(NBProperties conf) {
        NBProperties configuration = conf;
        String folderName = configuration.getPropertyWithDefault(Constants.Simulation.FROM_STATE, null);
        if (folderName != null) {
            System.out.println("Restoring transport layer");
            JSONObject json = SimulatorStateSaver.loadJson(folderName + "/" + "transport_layer_data.json");
//            flowIdCounter = (long) json.get("flowIdCounter");
            System.out.println("Done restoring simulator");
        }
    }

    /**
     * Pass the packet to the network device.
     *
     * @param packet Packet instance
     */
    public final void send(Packet packet) {
        networkDevice.receiveFromTransportLayer(packet);
    }

    public NetworkDevice getNetworkDevice() {
        return networkDevice;
    }

    /**
     * Set the associated network device.
     * <p>
     * Is not part of constructor because both the network device
     * and the server require a reference to each other.
     *
     * @param networkDevice Network device instance
     */
    public final void setNetworkDevice(NetworkDevice networkDevice) {
        this.networkDevice = networkDevice;
    }

    /**
     * Reception of a packet from its network device (through the intermediary).
     * // TODO: make it package-local?
     *
     * @param genericPacket Packet instance
     */
    public void receive(Packet genericPacket) {
        IpPacket packet = (IpPacket) genericPacket;
        Socket socket = flowIdToSocket.get(packet.getFlowId());
        // If the socket does not yet exist, it is an incoming socket
        if (socket == null && !finishedFlowIds.contains(packet.getFlowId())) {
            FlowLogger flowLogger = SimulationLogger.getActiveFlowLoggerByFlowId().get(packet.getFlowId());
            socket = createSocket(packet.getFlowId(), packet.getSourceId(), -1, -1, flowLogger.getJobId());
            // flowIdToReceiver.put(packet.getFlowId(), this);
            flowIdToSocket.put(packet.getFlowId(), socket);
            socket.markAsReceiver();
        }

        // Give packet to socket (we do not care about stray packets)
        if (socket != null) {
            socket.handle(packet);
        }
    }

    /**
     * Start the sending of a flow to the destination.
     *
     * @param destination  Destination network device identifier
     * @param flowSizeByte Byte size of the flow
     * @param jobId        Job identifier
     */
    public long startFlow(int destination, long flowSizeByte, int jobId) {

        // Create new outgoing socket
        Job job = Simulator.getJob(jobId);
        List<Flow> flowList = job.getCommoditiesFlowsMap().get(ImmutablePair.of(identifier, destination));
        Flow flow = flowList.get(flowList.size() - 1);
        long flowId = flow.getFlowId();
        long estimatedFlowSizeByte = -1;
        Socket socket = createSocket(flowId, destination, flowSizeByte, estimatedFlowSizeByte, jobId);
        flowIdToSocket.put(flowId, socket);

        // Start the socket off as initiator
        socket.markAsSender();
        socket.start();
        return flowId;

    }

    /**
     * Create a socket instance
     * .
     * The created socket should assume it is a receiver, unless their
     * {@link Socket#start() start} method has been called.
     *
     * @param flowId                Flow identifier of the socket
     * @param destinationId         Destination network device identifier
     * @param flowSizeByte          Flow size to be transferred from source to
     *                              destination in bytes
     * @param estimatedFlowSizeByte Estimated flow size to be transferred from
     *                              source to destination in bytes
     * @return Socket instance
     */
    protected abstract Socket createSocket(long flowId, int destinationId, long flowSizeByte,
                                           long estimatedFlowSizeByte, int jobId);

    /**
     * Remove the socket from the transport layer after the flow has been finished.
     *
     * @param flowId Flow identifier
     */
    public void removeSocket(long flowId) {
        this.finishedFlowIds.add(flowId);
        this.flowIdToSocket.remove(flowId);
    }

    /**
     * Clean up the socket references of a specific flow identifier (also
     * overreaches
     * to the receiver).
     *
     * @param flowId Flow identifier
     */
    public void cleanupSockets(long flowId) {
        this.removeSocket(flowId);
        // flowIdToReceiver.get(flowId).removeSocket(flowId);
    }

    public long getFlowSize(long flowId) {
        return flowIdToSocket.get(flowId).flowSizeByte;
    }

    public void registerAsDest(long flowId, long flowSizeByte) {

    }

    public long getNumOpenSockets() {
        return flowIdToSocket.size();
    }

    public int getIdentifier() {
        return identifier;
    }

}