package ch.ethz.systems.netbench.xpt.dynamic.device;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.dynamic.controller.DynamicDevice;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * a dynamic switch can connect directly to other switches
 */
public class DynamicSwitch extends NetworkDevice implements DynamicDevice {


	LinkGenerator mLinkGenerator; // dynamic link generator
	OutputPortGenerator mOutputPortGenerator; // dynamic output port generator
	private InputPort inputPort;
	private Map<Long,OutputPort> forwardingTable;

	protected DynamicSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary,
			NBProperties configuration) {
		super(identifier, transportLayer, intermediary, configuration);
		mLinkGenerator = new DynamicLinkGenerator(configuration);
		mOutputPortGenerator = new DynamicOutputPortGenerator(configuration);
		inputPort = new InputPort(this,null,mLinkGenerator.generate(null,null));
		this.forwardingTable = new HashMap();

	}

	@Override
	public void receive(Packet genericPacket) {
		TcpPacket tcpPacket = (TcpPacket) genericPacket;
//		ImmutablePair pair = super.getSourceDestinationEncapsulated((IpPacket) genericPacket);
		forwardingTable.get(tcpPacket.getJumboFlowId()).enqueue(genericPacket);

	}

	public void addConnection(NetworkDevice dest,long jumboFlowId) {
		Link link = mLinkGenerator.generate(this, dest);
		forwardingTable.put(jumboFlowId, mOutputPortGenerator.generate(this, dest, link));
	}

	public void removeConnection( long jumboFlowId) {
		forwardingTable.remove(jumboFlowId);
	}


	private void removeInputPort(int identifier) {
		// the dynamic switch has only one input port to receive all traffic.
		this.sourceIdToInputPort.remove(identifier);

	}

	@Override
	protected void receiveFromIntermediary(Packet genericPacket) {
		// TODO Auto-generated method stub

	}

	public InputPort getSourceInputPort(int sourceNetworkDeviceId) {
		return inputPort;
	}

	private void setInputPort(InputPort inputPort) {
		this.sourceIdToInputPort.put(inputPort.getSourceNetworkDevice().getIdentifier(), inputPort);
	}


}
