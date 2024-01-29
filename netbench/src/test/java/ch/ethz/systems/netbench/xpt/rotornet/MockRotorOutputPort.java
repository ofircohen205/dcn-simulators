package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.ReconfigurationDeadlineException;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorOutputPort;

public class MockRotorOutputPort extends RotorOutputPort {
    public boolean timeExceptionThrown = false;

    public MockRotorOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        super(ownNetworkDevice,towardsNetworkDevice,link,maxQueueSizeBytes,ecnThresholdKBytes);


    }

    @Override
    protected void onConfigurationTimeExceeded() {
        this.timeExceptionThrown = true;
        super.onConfigurationTimeExceeded();
    }
}
