package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorNetController;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorReconfigurationEvent;

import java.util.Map;

public class MockRotorController extends RotorNetController {
    public MockRotorController(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
    }


    protected void registerReconfigurationEvent() {
        Simulator.registerEvent(new MockReconfigurationEvent(mReconfigurationInterval,mReconfigurationTime));
        sNextReconfigurationTime = Simulator.getCurrentTime() + mReconfigurationInterval;
    }
}
