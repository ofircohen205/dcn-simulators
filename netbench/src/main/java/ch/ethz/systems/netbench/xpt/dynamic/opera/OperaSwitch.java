package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.basic.TcpHeader;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.ReconfigurationDeadlineException;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public class OperaSwitch extends NetworkDevice {

    private LinkedList<OperaPacket> directCircuitBuffers;
    private LinkedList<OperaPacket> inDirectCircuitBuffers;
    private HashSet<Long> flowFinishedSet;
    private HashMap<Long, Long> flowSizeMapBit;
    private long directCircuitThresholdBit;

    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     * @param configuration
     */
    public OperaSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        directCircuitThresholdBit = configuration.getLongPropertyOrFail(Constants.Opera.DIRECT_CIRCUIT_THRESHOLD_BYTE) * 8L;
        directCircuitBuffers = new LinkedList<>();
        inDirectCircuitBuffers = new LinkedList<>();
        flowSizeMapBit = new HashMap<>();
        flowFinishedSet = new HashSet<>();

    }

    private void routeDirectly(OperaPacket op) throws OperaNoPathException {
        OperaController controller = OperaController.getInstance();
        int dest = configuration.getGraphDetails().getTorIdOfServer(op.getDestinationId());
        OperaOutputPort outputPort = null;
        try {

            outputPort = controller.getOutpuPort(this.identifier, dest);
            outputPort.enqueue(op);
            SimulationLogger.increaseStatisticCounter("OPERA_PACKET_DIRECT_FORWARD_SUCCESS");
            return;
        } catch (OperaNoPathException e) {
//            System.out.println("trying to directly pass from " + this.identifier + " to " + dest);
//            System.out.println(controller);
            assert (controller.hasDirectConnection(this.identifier, dest) == null);
            SimulationLogger.increaseStatisticCounter("OPERA_PACKET_DIRECT_FORWARD_NO_PATH");
            throw e;

        } catch (ReconfigurationDeadlineException e) {
            SimulationLogger.increaseStatisticCounter("OPERA_PACKET_DIRECT_FORWARD_DEADLINE");
            assert (outputPort.configurationTimeExceeded(op));
            throw new OperaNoPathException();
        }
    }

    @Override
    public void receive(Packet genericPacket) {
        if (genericPacket instanceof OperaPacket) {
            receive((OperaPacket) genericPacket);
            return;
        }
        receive(new OperaPacket((FullExtTcpPacket) genericPacket));
    }

    public void receive(OperaPacket op) {
        Set<Integer> servers = configuration.getGraphDetails().getServersOfTor(this.identifier);
        TcpHeader header = (TcpHeader) op;
        boolean isSourceTor = servers.contains(header.getSourceId());
        boolean isDestToR = servers.contains(header.getDestinationId());
        if (isDestToR) {
            if (op.isFIN() && op.isACK()) {

                flowSizeMapBit.remove(op.getFlowId());
//                flowFinishedSet.add(op.getFlowId());
            }
            getTargetOuputPort(header.getDestinationId()).enqueue(op);
            return;
        }

        if (this.identifier == header.getDestinationId()) {

            this.passToIntermediary(op);
            return;
        }

        if (op.isSecondHop()) {
            try {
                routeDirectly(op);
            } catch (OperaNoPathException e) {
                directCircuitBuffers.addLast(op);
            }
            return;
        }

        OperaController controller = OperaController.getInstance();
        if (isSourceTor) {


            try {

                int destToR = configuration.getGraphDetails().getTorIdOfServer(header.getDestinationId());
                ArrayList<ImmutablePair<Integer, Integer>> path = null;
                path = controller.getRandomPath(this.identifier, destToR);
                op.setPath(path);
                if (op.isACK()) {

                    routeInDirectly(op);
                    return;
                }

                updateFlowSize(op);
            } catch (OperaNoPathException e) {
                inDirectCircuitBuffers.addLast(op);
                return;
            }


        }
        if (flowExceedsThreshold(op) && isSourceTor) {
            try {
                routeDirectly(op);
            } catch (OperaNoPathException e) {
                try {
                    routeToRandomHop(op);
                } catch (OperaNoPathException e1) {
                    directCircuitBuffers.addLast(op);
                }
            }
            return;
        }
        routeInDirectly(op);

    }

    private void routeToRandomHop(OperaPacket op) throws OperaNoPathException {


        OperaController.getInstance().getRandomOutputPort(this.identifier, op.getSizeBit()).enqueue(op);
        op.markSecondHop();
        SimulationLogger.increaseStatisticCounter("OPERA_RANDOM_FORWARD");

    }

    @Override
    public InputPort getSourceInputPort(int sourceNetworkDeviceId) {
        return OperaController.getInstance().getInputPort(this.identifier, sourceNetworkDeviceId);
    }

    private void routeInDirectly(OperaPacket op) {

        OperaController controller = OperaController.getInstance();
        TcpHeader header = (TcpHeader) op;


        try {

            ImmutablePair<Integer, Integer> nextHop = op.getNextHop();
            controller.getOutpuPort(this.identifier, nextHop.getRight(), nextHop).enqueue(op);


        } catch (OperaNoPathException e) {
            throw new RuntimeException();
        } catch (ReconfigurationDeadlineException e) {

            inDirectCircuitBuffers.addLast(op);
        }
    }

    private boolean flowExceedsThreshold(Packet genericPacket) {

        long flowSize = flowSizeMapBit.getOrDefault(genericPacket.getFlowId(), 0L);
        return flowSize > directCircuitThresholdBit;
    }

    @Override
    protected void receiveFromIntermediary(Packet genericPacket) {
        // TcpHeader header = (TcpHeader) genericPacket;
        receive(genericPacket);
    }

    private void updateFlowSize(Packet packet) {

        long flowSize = flowSizeMapBit.getOrDefault(packet.getFlowId(), 0L);
        flowSize = flowSize + packet.getSizeBit();
        flowSizeMapBit.put(packet.getFlowId(), flowSize);
    }

    public void sendPending() {


        int size = inDirectCircuitBuffers.size();
        for (int i = 0; i < size; i++) {
            OperaPacket p = inDirectCircuitBuffers.pollFirst();
            int destToR = configuration.getGraphDetails().getTorIdOfServer(((TcpHeader) p).getDestinationId());
            try {
                ((OperaPacket) p).setPath(OperaController.getInstance().getRandomPath(this.identifier, destToR));
            } catch (OperaNoPathException e) {
                System.out.println("Oh no!");
                throw new RuntimeException();
            }
            receive(p);
        }

        size = directCircuitBuffers.size();
        for (int i = 0; i < size; i++) {
            OperaPacket op = directCircuitBuffers.pollFirst();
            receive(op);
        }


    }

    public boolean flowExceedsThreshold(long flowId) {
        return this.flowSizeMapBit.getOrDefault(flowId, 0l) >= directCircuitThresholdBit;
    }
}
