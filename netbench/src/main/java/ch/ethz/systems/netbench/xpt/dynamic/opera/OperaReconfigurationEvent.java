package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.network.Event;

public class OperaReconfigurationEvent extends Event {

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs Time it will take before happening from now in nanoseconds
     */
    public OperaReconfigurationEvent(long timeFromNowNs) {
        super(timeFromNowNs);
    }

    @Override
    public void trigger() {
        OperaController controller = OperaController.getInstance();
        controller.reconfigure();
    }
}
