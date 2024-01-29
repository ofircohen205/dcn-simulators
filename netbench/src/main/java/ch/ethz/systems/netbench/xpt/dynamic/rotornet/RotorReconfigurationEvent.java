package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;

/**
 * the reconfiguration event will reconfigure all rotor switches to the next cycle
 */
public class RotorReconfigurationEvent extends Event {
    long mReconfigurationTime;
    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs Time it will take before happening from now in nanoseconds
     * @param reconfigurationTime
     */
    public RotorReconfigurationEvent(long timeFromNowNs, long reconfigurationTime) {
        super(timeFromNowNs);
        mReconfigurationTime = reconfigurationTime;
    }

    @Override
    public void trigger() {
        RotorNetController controller = (RotorNetController) getController();
        controller.reconfigureRotorSwitches();
        Simulator.registerEvent(new ReconfigurationEndEvent(mReconfigurationTime));
    }

    protected RemoteRoutingController getController() {
        return RemoteRoutingController.getInstance();
    }

    /**
     * an inner class to signal the end of the reconfiguration
     */
    private class ReconfigurationEndEvent extends Event {
        public ReconfigurationEndEvent(long mReconfigurationTime) {
            super(mReconfigurationTime);
        }

        @Override
        public void trigger() {
            RotorNetController rnc = (RotorNetController) getController();
            rnc.registerReconfigurationEvent(); // register another reconfiguration event
            rnc.startTransmmisions(); // start all transmissions again
        }
    }

//    @Override
//    public String toString(){
//        RotorNetController rnc = (RotorNetController) getController();
//        return rnc.getDevice(3).getRotorMap().toString();
//    }
}
