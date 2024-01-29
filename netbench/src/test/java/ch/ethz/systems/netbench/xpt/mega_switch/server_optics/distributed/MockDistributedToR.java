package ch.ethz.systems.netbench.xpt.mega_switch.server_optics.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServerToR;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.ReservationPacket;

public class MockDistributedToR extends DistributedOpticServerToR{
    static RemoteRoutingController remoteRouter;
    public MockDistributedToR(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
    }

    public static void setRemoteRouter(MockDistributedServerOpticsRouter remoteRouter) {
        MockDistributedToR.remoteRouter = remoteRouter;
    }

    @Override
    protected void handleReservationPacket(Packet genericPacket) {
//        System.out.println(genericPacket.toString());
        super.handleReservationPacket(genericPacket);
    }

    @Override
    protected RemoteRoutingController getRemoteRouter() {
        return remoteRouter;
    }
}
