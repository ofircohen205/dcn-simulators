package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorReconfigurationEvent;

public class MockReconfigurationEvent extends RotorReconfigurationEvent {
    static int reconfigurationEventCount = 0;
    private static RemoteRoutingController sController;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs       Time it will take before happening from now in nanoseconds
     * @param reconfigurationTime
     */
    public MockReconfigurationEvent(long timeFromNowNs, long reconfigurationTime) {
        super(timeFromNowNs, reconfigurationTime);
    }
    
    @Override
    protected RemoteRoutingController getController(){
        return sController;
    }

    @Override
    public void trigger() {
        super.trigger();
        reconfigurationEventCount++;
    }
    
    static void setController(RemoteRoutingController r){
        sController = r;
    }
}
