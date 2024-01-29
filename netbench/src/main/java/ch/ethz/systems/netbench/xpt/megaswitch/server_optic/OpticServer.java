package ch.ethz.systems.netbench.xpt.megaswitch.server_optic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.ConversionUnit;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.OpticElectronicHybrid;

/**
 * optic serve class
 */
public class OpticServer extends OpticElectronicHybrid {
    private final int ownToRId;
    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     * @param configuration
     */
    protected OpticServer(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        this.ownToRId = configuration.getGraphDetails().getTorIdOfServer(this.identifier);

    }


    /**
     * receive method here, should probably solve code duplication
     * @param genericPacket
     */
    @Override
    public void receive(Packet genericPacket) {
        TcpPacket tcpPacket = (TcpPacket) genericPacket;
        if (tcpPacket.getDestinationId() == this.identifier) {
            if(tcpPacket.isACK() && tcpPacket.isFIN()){
                int destToR = configuration.getGraphDetails().getTorIdOfServer(tcpPacket.getSourceId());

            }
            passToIntermediary(genericPacket);
            return;
        }
        super.receive(genericPacket);

    }

    @Override
    protected TcpPacket encapsulatePacket(Encapsulatable packet) {
        return (TcpPacket) packet;
    }

    @Override
    protected int getDestToRWithDefault(int serverId, int defToRId) {
        int destToRId = configuration.getGraphDetails().getTorIdOfServer(serverId);

        return destToRId;
    }

    @Override
    protected int getReceivingDest(JumboFlow jFlow) {
        return jFlow.getDest();
    }

    @Override
    protected int getTransmittingSource(JumboFlow jFlow) {
        return jFlow.getSource();
    }

    @Override
    protected int getSourceToRWithDefault(int serverId, int defToRId) {
        return this.ownToRId;
    }


    @Override
    protected void routeThroughtPacketSwitch(TcpPacket packet) {
        getTargetOuputPort(ownToRId).enqueue(packet);
    }

    @Override
    protected void receiveFromIntermediary(Packet genericPacket) {
        this.receive(genericPacket);
    }

    /**
     * inits the connection between this and the optic device on the ToR
     * @param optic
     */
    public void createOpticConnection(NetworkDevice optic) {

        conversionUnit = new ConversionUnit(configuration,this,optic);
    }

    @Override
    protected void recoverPath(JumboFlow jumboFlow) {
        assert(this.identifier==jumboFlow.getSource());
        super.recoverPath(jumboFlow);
    }
}
