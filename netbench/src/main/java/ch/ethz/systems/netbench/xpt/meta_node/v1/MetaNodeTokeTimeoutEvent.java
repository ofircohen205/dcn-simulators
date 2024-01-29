package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Event;

public class MetaNodeTokeTimeoutEvent extends Event {

    MetaNodeToken token;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs Time it will take before happening from now in nanoseconds
     */
    public MetaNodeTokeTimeoutEvent(long timeFromNowNs) {
        super(timeFromNowNs);
    }

    public MetaNodeTokeTimeoutEvent(long timeFromNowNs, MetaNodeToken token) {
        super(timeFromNowNs);
        this.token = token;
    }


    @Override
    public void trigger() {
        if(token.active() || token.expired()) return;
        token.setExpired();
    }

    @Override
    public boolean retrigger() {
        if(token.active()){
            time = Simulator.getCurrentTime() + token.getTimeout();
            return true;
        }
        return false;
    }


}
