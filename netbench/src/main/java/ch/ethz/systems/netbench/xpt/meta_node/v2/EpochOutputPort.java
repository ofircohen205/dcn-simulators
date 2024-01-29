package ch.ethz.systems.netbench.xpt.meta_node.v2;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.ext.basic.EcnTailDropOutputPort;
import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;

public class EpochOutputPort extends EcnTailDropOutputPort {
    protected EpochOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSizeBytes, long ecnThresholdKBytes) {
        super(ownNetworkDevice, targetNetworkDevice, link, maxQueueSizeBytes, ecnThresholdKBytes);
    }

    @Override
    protected void dispatch(Packet packet){
        super.dispatch(packet);
        if(!ownNetworkDevice.isServer()) return;
        MNEpochPacket mnEpochPacket = (MNEpochPacket) packet;
        MetaNodeServer server = (MetaNodeServer) getOwnDevice();
        server.pullPacket(mnEpochPacket.getFlowId());
    }

    protected void markECN(Packet packet){

    }

    @Override
    protected void onPacketDropped(Packet packet) {
        String suffix = "";
        MNEpochPacket ipHeader = (MNEpochPacket) packet;

        SimulationLogger.increaseStatisticCounter(suffix + "PACKETS_DROPPED");
        if (ipHeader.getSourceId() == this.getOwnId()) {
            SimulationLogger.increaseStatisticCounter(suffix + "PACKETS_DROPPED_AT_SOURCE");
        }
    }


}
