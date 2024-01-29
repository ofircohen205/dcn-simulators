package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.network.OutputPort;

public class EmptyPortLogger extends PortLogger {
    /**
     * Create logger for the given port.
     *
     * @param port Output port instance
     */
    public EmptyPortLogger(OutputPort port) {
        super(port);
    }

    protected void registerSelf() {

    }

    @Override
    public void logLinkUtilized(boolean beingUtilized) {

    }

}
