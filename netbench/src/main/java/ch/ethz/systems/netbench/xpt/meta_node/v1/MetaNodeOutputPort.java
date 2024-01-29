package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;

public class MetaNodeOutputPort extends EcnTailDropOutputPort {
    public MetaNodeOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
    }

    @Override
    protected void dispatch(Packet packet) {
        super.dispatch(packet);
        try{
            MetaNodePacket metaNodePacket = (MetaNodePacket) packet;
            if(getOwnDevice().isServer()){
                metaNodePacket.serverToken.onSend(metaNodePacket.getSizeBit()/8);

            }
        }catch (ClassCastException e){

        }


    }
}
