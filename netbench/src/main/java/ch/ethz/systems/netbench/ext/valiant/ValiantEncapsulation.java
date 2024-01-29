package ch.ethz.systems.netbench.ext.valiant;

import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingPacket;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

public class ValiantEncapsulation extends IpPacket implements ValiantEncapsulationHeader, Encapsulatable {

    private TcpPacket packet;
    private final int valiantDestination;
    private boolean passedValiant;

    ValiantEncapsulation(TcpPacket packet, int valiantDestination) {
        super(packet.getFlowId(), packet.getSizeBit() - 480L, packet.getSourceId(), packet.getDestinationId(), packet.getTTL());
        this.packet = packet;
        this.valiantDestination = valiantDestination;
        this.passedValiant = false;
    }


    @Override
    public TcpPacket getPacket() {
        return packet;
    }

    @Override
    public int getValiantDestination() {
        return valiantDestination;
    }

    @Override
    public void markPassedValiant() {
        passedValiant = true;
    }

    @Override
    public boolean passedValiant() {
        return passedValiant;
    }

    @Override
    public void markCongestionEncountered() {
        this.packet.markCongestionEncountered();
    }

    @Override
    public Encapsulatable encapsulate(int newSource,int newDestination) {
        return null; //not sure if you ever use this
    }

    @Override
    public Encapsulatable deEncapsualte() {

        return packet.deEncapsualte();
    }
}
