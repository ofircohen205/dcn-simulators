package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorMap;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorOutputPortGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorSwitch;

public class MockRotorMap extends RotorMap {
    static RemoteRoutingController sRouter;
    public MockRotorMap(RotorOutputPortGenerator rotorOutputPortGenerator, LinkGenerator linkGenerator, RotorSwitch rotorSwitch) {
        super(rotorOutputPortGenerator, linkGenerator, rotorSwitch);
    }

    static void setRouter(RemoteRoutingController router){
        sRouter = router;
    }

    protected RemoteRoutingController getController(){
        return sRouter;
    }
}
