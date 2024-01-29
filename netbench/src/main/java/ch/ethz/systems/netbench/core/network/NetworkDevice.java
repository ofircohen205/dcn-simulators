package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.FlowPathExists;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstraction for a network device.
 * <p>
 * All nodes in the network are instances of this abstraction.
 * It takes care of the definition of network connections and
 * forces its subclasses to be able to handle packets it receives.
 * A network device is a server iff it has a
 * {@link TransportLayer transport layer}.
 * <p>
 * It enables additional modification of packets by placement of a
 * {@link Intermediary intermediary}
 * in between the network device and the transport layer.
 */
public abstract class NetworkDevice {

    protected final int identifier;
    protected final List<Integer> connectedTo;
    protected final Intermediary intermediary;
    protected final NBProperties configuration;
    private final TransportLayer transportLayer;
    private final boolean hasTransportLayer;
    private final long mSwitchTimeNs;
    protected MegaSwitch encapsulatingDevice;
    protected String techonology;
    protected Map<Integer, InputPort> sourceIdToInputPort;
    private Map<Integer, List<OutputPort>> targetIdToOutputPort;

    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     */
    protected NetworkDevice(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        this.configuration = configuration;
        // Permanent unique identifier assigned
        this.identifier = identifier;

        // Initialize internal data structures
        this.connectedTo = new ArrayList<>();
        this.targetIdToOutputPort = new HashMap<>();
        this.sourceIdToInputPort = new HashMap<>();
        //this.techonology  = configuration.getProperty("network_type");
        // Set the server and whether it exists
        this.transportLayer = transportLayer;
        this.hasTransportLayer = (transportLayer != null);

        // Add intermediary
        this.intermediary = intermediary;
        if (intermediary != null) this.intermediary.setNetworkDevice(this);
        this.mSwitchTimeNs = configuration == null ?
                0 : configuration.getLongPropertyWithDefault(
                Constants.NetworkDeviceGenerator.SWITCHING_TIME_NS,
                0);


    }

    public NBProperties getConfiguration() {
        return configuration;
    }

    public MegaSwitch getEncapsulatingDevice() {
        return encapsulatingDevice;
    }

    public void setEncapsulatingDevice(MegaSwitch device) {
        this.encapsulatingDevice = device;
    }

    /**
     * Retrieve the automatically generated unique
     * network device identifier.
     *
     * @return Unique network device identifier
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * Add a port which is a connection to another network device.
     *
     * @param outputPort Output port instance
     */
    public void addConnection(OutputPort outputPort) {

        // Port does not originate from here
        if (getIdentifier() != outputPort.getOwnId()) {
            throw new IllegalArgumentException("Impossible to add output port not originating from " + getIdentifier() + " (origin given: " + outputPort.getOwnId() + ")");
        }

        boolean allowDuplicateEdges = configuration.getBooleanPropertyWithDefault(
                Constants.NetworkDeviceGenerator.ALLOW_DUPLICATE_EDGES,
                false);
        // Port going there already exists
        if (connectedTo.contains(outputPort.getTargetId()) && !allowDuplicateEdges) {
            throw new IllegalArgumentException("Impossible to add a duplicate port from " + outputPort.getOwnId() + " to " + outputPort.getTargetId() + ".");
        }

        // Add to mappings
        if (!connectedTo.contains(outputPort.getTargetId()))
            connectedTo.add(outputPort.getTargetId());

        List<OutputPort> outputPorts = targetIdToOutputPort.getOrDefault(outputPort.getTargetId(), new ArrayList<>());
        outputPorts.add(outputPort);
        targetIdToOutputPort.put(outputPort.getTargetId(), outputPorts);

    }

    /**
     * Check whether this network device has an outgoing port to the target.
     *
     * @param target Target network device identifier
     * @return True iff an outgoing port from this network device exists to the target
     */
    public boolean hasConnection(int target) {
        return connectedTo.contains(target);
    }

    /**
     * Reception of a packet by the network device from another network device.
     * // TODO: make it protected-local?
     *
     * @param genericPacket Packet instance
     */
    public abstract void receive(Packet genericPacket);

    /**
     * Reception of a packet by the network device from the underlying transport layer (adapted by flowlet
     * intermediary if necessary).
     *
     * @param genericPacket Packet instance (with flowlet modified)
     */
    protected abstract void receiveFromIntermediary(Packet genericPacket);

    /**
     * Pass a packet to the underlying intermediary, which adapts it if needed
     * and forwards it to the transport layer of the network device.
     *
     * @param genericPacket Packet instance
     */
    protected void passToIntermediary(Packet genericPacket) {
        intermediary.adaptIncoming(genericPacket);
        transportLayer.receive(genericPacket);
    }

    /**
     * Reception of a packet by the network device from the underlying transport layer.
     * Adapts it via the intermediary and then sends it on to the switch.
     * Do not override. // TODO: make it package-local?
     *
     * @param genericPacket Packet instance
     */
    public void receiveFromTransportLayer(Packet genericPacket) {
        intermediary.adaptOutgoing(genericPacket);
        this.receiveFromIntermediary(genericPacket);
    }

    /**
     * Check whether the network device has a transport layer and is thus a potential server.
     * A server is a node which is able to send and/or receive traffic in the network, in essence
     * it can be a source or sink whereas normal routing nodes without transport layer cannot be.
     *
     * @return True iff the network device is a server
     */
    public boolean isServer() {
        return hasTransportLayer;
    }

    /**
     * Retrieve the underlying transport layer of the network device.
     * Must not be called if there is no underlying transport layer (so the network device is not a server).
     *
     * @return Transport layer
     */
    public TransportLayer getTransportLayer() {
        assert (transportLayer != null);
        return transportLayer;
    }


    protected void passToEncapsulatingDevice(Packet packet) {
        this.encapsulatingDevice.receiveFromEncapsulatedDevice(packet);
    }

    public void initCircuit(IpPacket packet) {
        initCircuit(packet.getSourceId(), packet.getDestinationId(), packet.getFlowId());

    }

    public void initCircuit(int source, int dest, long flowId) {
        try {
            RemoteRoutingController.getInstance().initRoute(source, dest, flowId);
        } catch (FlowPathExists e) {

        } catch (NoPathException e) {
            SimulationLogger.increaseStatisticCounter("num_path_failures");
            return;
        }
    }

    @Override
    public String toString() {
        return "Device " + identifier + " with encapsulation" + this.encapsulatingDevice == null ? " none" : "";

    }

    public OutputPort getTargetOuputPort(int targetId, String technology) {

        return getTargetOuputPort(targetId);
    }

    public String getTechnology() {
        return this.techonology;
    }

    public OutputPort getTargetOuputPort(int targetId) {
        return targetIdToOutputPort.get(targetId).get(0);
    }

    public OutputPort getTargetOuputPort(int targetId, int hash) {
        List<OutputPort> outputPorts = targetIdToOutputPort.get(targetId);
        return outputPorts.get(hash % outputPorts.size());
    }

    public Map<Integer, OutputPort> getOuputPortsMap() {
        Map<Integer, OutputPort> portMap = new HashMap<>();
        for (int p : targetIdToOutputPort.keySet()) {
            portMap.put(p, getTargetOuputPort(p));
        }
        return portMap;
    }

    public void addIncomingConnection(InputPort inputPort) {
        sourceIdToInputPort.put(inputPort.getSourceNetworkDevice().identifier, inputPort);
    }

    public InputPort getSourceInputPort(int sourceNetworkDeviceId, String technology) {
        return getSourceInputPort(sourceNetworkDeviceId);
    }

    public InputPort getSourceInputPort(int sourceNetworkDeviceId) {
        return this.sourceIdToInputPort.get(sourceNetworkDeviceId);
    }

    public void receiveFromEncapsulating(Packet packet) {
        Simulator.registerEvent(new Event(mSwitchTimeNs) {
            @Override
            public void trigger() {
                /**
                 * check first if we need pass the packet back to the encapsulating device
                 */
                if (((IpPacket) packet).getDestinationId() == NetworkDevice.this.identifier) {
                    passToEncapsulatingDevice(packet);
                    return;
                }
                receive(packet);
            }
        });

    }


    /**
     * a method to return the correct pair key. should probably think of something smarter here
     *
     * @param packet
     * @return
     */
    public ImmutablePair getSourceDestinationEncapsulated(IpPacket packet) {
        if (Simulator.getConfiguration().getBooleanPropertyWithDefault(
                Constants.NetworkDeviceGenerator.ENABLE_JUMBO_FLOWS,
                false)) {
            /**
             * if jumbo flows enabled dont de-enacpsulate
             */
            return new ImmutablePair(packet.getSourceId(), packet.getDestinationId());
        }
        try {
            /**
             * if not then de-enacpsulate
             */
            IpPacket deEncapse = (IpPacket) (((Encapsulatable) packet).deEncapsualte());
            return new ImmutablePair(deEncapse.getSourceId(), deEncapse.getDestinationId());
        } catch (ClassCastException e) {
            return new ImmutablePair(packet.getSourceId(), packet.getDestinationId());
        }
    }


    public List<Integer> getConnectedTo() {
        return connectedTo;
    }
}
