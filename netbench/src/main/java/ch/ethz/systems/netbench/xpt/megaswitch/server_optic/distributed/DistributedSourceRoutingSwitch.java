package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import org.apache.commons.lang3.tuple.ImmutablePair;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.semi.SemiRemoteRoutingSwitch;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class DistributedSourceRoutingSwitch extends SemiRemoteRoutingSwitch {
	protected Map<Pair<Integer,Integer>,OutputPort> forwardingTable;

	public DistributedSourceRoutingSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary,
                                          NBProperties configuration) {
		super(identifier, transportLayer, intermediary, configuration);
		this.forwardingTable = new HashMap<>();
	}

	/**
	 * while it has the same signature as super the reconfiguration is done
	 * via the prevhop color pair.
	 * @param prevHop
	 * @param nextHop
	 * @param color
	 */
	public void updateForwardingTable(int prevHop, int nextHop, int color) {
		forwardingTable.put(new ImmutablePair<>(prevHop, color), getTargetOuputPort(nextHop));
		
	}
	
	public OutputPort getNextHop(int prevHop, int color) {
		// TODO Auto-generated method stub
		return forwardingTable.get(new ImmutablePair(prevHop,color));
	}

	/**
	 * forward to next switch by previous hop color pair.
	 * @param packet
	 */
	protected void forwardToNextSwitch(IpPacket packet) {
    	TcpPacket tcpPacket = (TcpPacket) packet;
    	int prevHop = tcpPacket.getPrevHop();
    	tcpPacket.setPrevHop(identifier);
    	forwardingTable.get(new ImmutablePair(prevHop,tcpPacket.getColor())).enqueue(packet);
		
	}


}
