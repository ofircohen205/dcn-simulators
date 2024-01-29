package ch.ethz.systems.netbench.xpt.opera;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.dynamic.opera.OperaController;

import java.util.Map;

public class MockOperaController extends OperaController {
    public MockOperaController(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        super(configuration, idToNetworkDevice);
    }

    public void setParellelNum(int num){
        rotorParallelConfiguration = num;
    }

    public void reset() {
        this.currCycle = 0;
        this.nextRotorToConfigure = 0;
    }


}
