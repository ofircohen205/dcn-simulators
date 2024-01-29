package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.bare.BareSocket;
import ch.ethz.systems.netbench.ext.demo.DemoPacket;
import ch.ethz.systems.netbench.ext.demo.DemoSocket;

public class SimpleSocket extends BareSocket{
    public SimpleSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte) {
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte);
    }

    @Override
    public void handle(Packet packet) {

    }
}
