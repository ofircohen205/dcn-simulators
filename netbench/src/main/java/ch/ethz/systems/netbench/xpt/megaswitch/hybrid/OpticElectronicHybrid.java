package ch.ethz.systems.netbench.xpt.megaswitch.hybrid;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.FlowPathExists;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;

public class OpticElectronicHybrid extends NetworkDevice implements MegaSwitch {
    private final HashMap<Long, CircuitTimeoutEvent> tearDownMap;
    private final long circiutTimeout;
    private final boolean useDummyServers;
    protected long circuitThreshold;
    protected NetworkDevice electronic;
    protected NetworkDevice optic;
    protected HashMap<Pair<Integer, Integer>, JumboFlow> mJumboFlowMap;
    private long mNumAllocatedFlows;
    private long mNumDeAllocatedFlows;
    protected ConversionUnit conversionUnit;

    public OpticElectronicHybrid(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        circuitThreshold = configuration.getLongPropertyOrFail(Constants.Circuit.HYBRID_CIRCUIT_THRESHOLD_BYTE);
        mJumboFlowMap = new HashMap<>();
        mNumAllocatedFlows = 0;
        circiutTimeout = configuration.getLongPropertyWithDefault(
                Constants.Optic.CIRCUIT_TEARDOWN_TIMEOUT_NS,
                30000);
        useDummyServers = configuration.getBooleanPropertyWithDefault(
                Constants.NetworkDeviceGenerator.USE_DUMMY_SERVERS,
                false);
        mNumDeAllocatedFlows = 0;
        tearDownMap = new HashMap();

    }

    @Override
    public void receive(Packet genericPacket) {

        Encapsulatable packet = (Encapsulatable) genericPacket;


        TcpPacket encapsulated = encapsulatePacket(packet); // encapsulate the packet
        JumboFlow jumboFlow = getJumboFlow(getSourceToRWithDefault(packet.getSourceId(), encapsulated.getSourceId()),
                getDestToRWithDefault(packet.getDestinationId(), encapsulated.getDestinationId()),
                packet.getSourceId(), packet.getDestinationId());
        jumboFlow.onPacketDispatch(encapsulated);

        /**
         * if is circuitable then try to route through the circuit
         */
        if (isCircuitable(jumboFlow, encapsulated)) {
            try {
                routeThroughCircuit(encapsulated, jumboFlow);
                jumboFlow.onCircuitEntrance(packet.getFlowId());
                SimulationLogger.increaseStatisticCounter("PACKET_ROUTED_THROUGH_CIRCUIT");
                return;
            } catch (NoPathException e) {
            }
        }
        routeThroughtPacketSwitch(encapsulated);
    }

    protected int getDestToRWithDefault(int serverId, int defToRId) {
        return defToRId;
    }

    protected int getSourceToRWithDefault(int serverId, int defToRId) {
        return defToRId;
    }

    /**
     * encapsulate the packet
     *
     * @param packet
     * @return
     */
    protected TcpPacket encapsulatePacket(Encapsulatable packet) {
//        if(configuration.getGraphDetails().getTorIdOfServer(packet.getDestinationId())==null){
//            System.out.println(packet.getDestinationId());
//            System.out.print(packet);
//        }
        int destinationToR = configuration.getGraphDetails().getTorIdOfServer(packet.getDestinationId());
        /**
         * encapsulate the packet with new source and destination ids
         */
        return (TcpPacket) packet.encapsulate(this.identifier, destinationToR);
    }

    /**
     * get the jumbo flow for this class's aggregation scheme
     *
     * @param sourceToR
     * @param destToR
     * @param serverSource
     * @param serverDest
     * @return
     */
    protected JumboFlow getJumboFlow(int sourceToR, int destToR, int serverSource, int serverDest) {
        return getJumboFlow(serverSource, serverDest).setSourceToR(sourceToR).setDestToR(destToR);
    }


    /**
     * gets the jumbo flow with source-dest pair, or create a new one
     *
     * @param source
     * @param dest
     * @return
     */
    protected JumboFlow getJumboFlow(int source, int dest) {
        JumboFlow jumboFlow = mJumboFlowMap.get(new ImmutablePair<>(source, dest));
        if (jumboFlow == null) {
            jumboFlow = new JumboFlow(source, dest);
            mJumboFlowMap.put(new ImmutablePair<>(source, dest), jumboFlow);
        }
        return jumboFlow;
    }

    protected void initConversionUnit() {
        conversionUnit = new ConversionUnit(configuration, this, optic);
    }

    protected void routeThroughtPacketSwitch(TcpPacket packet) {

        electronic.receiveFromEncapsulating(packet);

    }

    protected void routeThroughCircuit(IpPacket packet, JumboFlow jFlow) {

        try {
            int transmittingSource = getTransmittingSource(jFlow);
            int receiveingDest = getReceivingDest(jFlow);
            /**
             * init the route
             */
            getRemoteRouter().initRoute(transmittingSource, receiveingDest, jFlow.getSource(), jFlow.getDest(), jFlow.getId(), packet.getSizeBit());
        } catch (FlowPathExists e) {

        }

        this.conversionUnit.enqueue(jFlow.getSource(), jFlow.getDest(), packet);
        cancelCircuitTimeout(jFlow);
        setupCircuitTimeout(jFlow);

    }

    protected void cancelCircuitTimeout(JumboFlow jFlow) {
        CircuitTimeoutEvent prevEvent = tearDownMap.get(jFlow.getId());
        if (prevEvent != null) prevEvent.cancel();
    }

    protected void setupCircuitTimeout(JumboFlow jFlow) {

        CircuitTimeoutEvent e = new CircuitTimeoutEvent(circiutTimeout, this, jFlow);
        Simulator.registerEvent(e);
        tearDownMap.put(jFlow.getId(), e);
    }

    /**
     * get the receiving destination by this class's aggregation scheme
     *
     * @param jFlow
     * @return
     */
    protected int getReceivingDest(JumboFlow jFlow) {
        return jFlow.getDestToR();
    }

    /**
     * get the transmitting source by this class's aggregation scheme
     *
     * @param jFlow
     * @return
     */
    protected int getTransmittingSource(JumboFlow jFlow) {
        return jFlow.getSourceToR();
    }

    @Override
    protected void receiveFromIntermediary(Packet genericPacket) {
        throw new RuntimeException("Hybrid switch is not a server");
    }

    @Override
    public void extend(NetworkDevice networkDevice, NBProperties networkConf) {
        String networkType = networkConf.getPropertyOrFail(Constants.NetworkType.NETWORK_TYPE);
        /*
         * set the new device according to the network type
         */
        switch (networkType) {
            case Constants.NetworkType.CIRCUIT_SWITCH:
                this.optic = networkDevice;
                this.optic.setEncapsulatingDevice(this);
                initConversionUnit();
                break;
            case Constants.NetworkType.PACKET_SWITCH:
                this.electronic = networkDevice;
                this.electronic.setEncapsulatingDevice(this);
                break;
            default:
                throw new RuntimeException("bad network type " + networkType);
        }
    }

    @Override
    public void receiveFromEncapsulatedDevice(Packet packet) {
        Encapsulatable ipPacket = (Encapsulatable) packet;

        if (ipPacket.getDestinationId() == this.identifier) {
            /**
             * de-encapsulate the packet first, then send to the origin server
             */
            TcpPacket deEncapse = (TcpPacket) ipPacket.deEncapsualte();
            if (deEncapse.isACK() && deEncapse.isFIN()) {
                // use to be how we check for finished flow

            }
            sendToServer(deEncapse, deEncapse.getDestinationId());
        }
    }

    protected void sendToServer(Packet packet, int serverId) {
        if (useDummyServers) {
            //treat as the server.
            getTargetOuputPort(serverId).getTargetDevice().getSourceInputPort(identifier).receive(packet);
            return;
        }
        getTargetOuputPort(serverId).enqueue(packet);
    }

    /**
     * recover resources for the conversion unit for this jumbo flow
     *
     * @param jumbo
     */
    protected void conversionUnitRecover(JumboFlow jumbo) {
        conversionUnit.onJumboFlowFinish(jumbo.getSource(), jumbo.getDest(), jumbo.getId());
    }

    /**
     * called when flowId has finished
     * will recover the path if the corresponding jumbo flow finished
     *
     * @param sourceToR
     * @param destToR
     * @param serverSource
     * @param serverDest
     * @param flowId
     */
    public void onFlowFinished(int sourceToR, int destToR, int serverSource, int serverDest, long flowId) {
        JumboFlow jumboFlow = getJumboFlow(sourceToR, destToR, serverSource, serverDest);
        jumboFlow.onFlowFinished(flowId);
        if (hasOpticConnection()) conversionUnit.onFlowFinish(jumboFlow.getSource(), jumboFlow.getDest(), flowId);
        if (jumboFlow.getNumFlows() == 0) {
            resetJumboFlow(jumboFlow);
        }

    }

    protected boolean hasOpticConnection() {
        return conversionUnit != null && conversionUnit.getOptic() != null;
    }


    protected void recoverPath(JumboFlow jFlow) {
        try {
            int transmittingSource = getTransmittingSource(jFlow);
            int receiveingDest = getReceivingDest(jFlow);
            /**
             * recover the path
             */
            getRemoteRouter().recoverPath(transmittingSource, receiveingDest, jFlow.getSource(), jFlow.getDest(), jFlow.getId());
        } catch (NoPathException e) {

        }
    }

    @Override
    public NetworkDevice getAsNetworkDevice() {
        return this;
    }

    @Override
    public NetworkDevice getEncapsulatedDevice(String type) {
        /**
         * get the encapsulating device by this type
         */
        switch (type) {
            case "circuit_switch":
                return optic;
            case "packet_switch":
                return electronic;
            default:
                throw new RuntimeException("bad network type " + type);
        }
    }

    @Override
    public boolean hadlePacketFromEncapsulating(Packet packet) {
        return false;
    }

    @Override
    public OutputPort getTargetOuputPort(int targetId, String technology) {
        if (technology == null) {
            return this.getTargetOuputPort(targetId);
        }
        switch (technology) {
            case "optic":
                return optic.getTargetOuputPort(targetId);
            case "electronic":
                return electronic.getTargetOuputPort(targetId);
            default:
                throw new RuntimeException("bad technology " + technology);
        }
    }

    @Override
    public InputPort getSourceInputPort(int sourceNetworkDeviceId, String technology) {
        if (technology == null) {
            return this.getSourceInputPort(sourceNetworkDeviceId);
        }
        switch (technology) {
            case "optic":
                return optic.getSourceInputPort(sourceNetworkDeviceId);
            case "electronic":
                return electronic.getSourceInputPort(sourceNetworkDeviceId);
            default:
                throw new RuntimeException("bad technology " + technology);
        }
    }

    protected RemoteRoutingController getRemoteRouter() {
        return RemoteRoutingController.getInstance();
    }

    public String getState() {

        return "";
    }

    /**
     * does the jumbFlow and the packet meets the critirea to enter the circuit
     *
     * @param jumboFlow
     * @param packet
     * @return
     */
    protected boolean isCircuitable(JumboFlow jumboFlow, TcpPacket packet) {
        return jumboFlow.getSizeByte() >= circuitThreshold && !jumboFlow.isTrivial() && !(packet.isACK());
    }

    public void onJumboFlowFinished(int sourceToR, int destToR, int serverSource, int serverDest) {
        JumboFlow jumboFlow = getJumboFlow(sourceToR, destToR, serverSource, serverDest);
        resetJumboFlow(jumboFlow);
    }

    void resetJumboFlow(JumboFlow jumboFlow) {
        if (hasOpticConnection()) {
            conversionUnitRecover(jumboFlow);
            recoverPath(jumboFlow);
        }

        cancelCircuitTimeout(jumboFlow);
        jumboFlow.reset();
    }
}
