package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.NetworkDeviceGenerator;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitch;

public class MetaNodeSwitchGenerator extends NetworkDeviceGenerator {
    IntermediaryGenerator intermediaryGenerator;
    int numNodes;

    public MetaNodeSwitchGenerator(IntermediaryGenerator intermediaryGenerator, int numNodes, NBProperties configuration) {
        super(configuration);
        this.intermediaryGenerator = intermediaryGenerator;
        this.numNodes = numNodes;
    }

    @Override
    public NetworkDevice generate(int identifier) {
        if(configuration.getGraphDetails().getMetaNodeNum() == -1){
            return new EcmpSwitch(identifier,null,numNodes,intermediaryGenerator.generate(identifier),configuration);
        }
        return new MetaNodeSwitch(identifier,null,numNodes,intermediaryGenerator.generate(identifier),configuration);
    }

    @Override
    public NetworkDevice generate(int identifier, TransportLayer server) {
        if(configuration.getGraphDetails().getMetaNodeNum() == -1){
            return new EcmpSwitch(identifier,server,numNodes,intermediaryGenerator.generate(identifier),configuration);
        }
        return new MetaNodeSwitch(identifier,server,numNodes,intermediaryGenerator.generate(identifier),configuration);
    }
}
