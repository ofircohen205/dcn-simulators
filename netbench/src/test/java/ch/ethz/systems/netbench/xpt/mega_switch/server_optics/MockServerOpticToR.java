package ch.ethz.systems.netbench.xpt.mega_switch.server_optics;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.megaswitch.MegaSwitch;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.OpticServer;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.OpticServerToR;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.MockRemoteRouter;

public class MockServerOpticToR extends OpticServerToR{
    static RemoteRoutingController remoteRouter;
    public MockServerOpticToR(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
    }

    public static void setRemoteRouter(RemoteRoutingController remoteRouter) {
        MockServerOpticToR.remoteRouter = remoteRouter;
    }

    protected RemoteRoutingController getRemoteRouter() {
        return remoteRouter;
    }
}
