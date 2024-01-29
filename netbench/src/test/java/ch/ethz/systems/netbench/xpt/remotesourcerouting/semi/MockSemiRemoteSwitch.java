package ch.ethz.systems.netbench.xpt.remotesourcerouting.semi;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.TransportLayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MockSemiRemoteSwitch extends SemiRemoteRoutingSwitch{
    MockSemiRemoteSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
    }

    HashMap<Integer,List<List<Integer>>> getMap(){
        return this.mPathMap;
    }

    @Override
    protected HashMap<Integer, List<List<Integer>>> readMap(String path) {
        HashMap<Integer, List<List<Integer>>> map = super.readMap(path);
        if(map.get(4) != null){
            assert(map.get(4).size()==10);
        }
        return map;
    }

    @Override
    protected List<Integer>[] readListFromFile(String path) throws IOException, ClassNotFoundException {
        List<Integer>[] paths =  super.readListFromFile(path);
        assert(paths.length==2880);

        return paths;
    }
}
