package ch.ethz.systems.netbench.xpt.mega_switch.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.dynamic.controller.DynamicController;

public class MockDynamicController extends DynamicController {
	HashMap<Pair<Integer,Integer>,Boolean> routed;
	HashMap<Pair<Integer,Integer>,Boolean> recovered;
	public MockDynamicController(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
		super(idToNetworkDevice, configuration);
		routed = new HashMap<>();
		recovered = new HashMap<>();
	}
	
	@Override
	public void initRoute(int transimttingSource, int receivingDest, int sourceKey, int destKey, long jumboFlowId, long sizeBit) {
		super.initRoute(transimttingSource, receivingDest, sourceKey, destKey, jumboFlowId, sizeBit);

		routed.put(new ImmutablePair<Integer, Integer>(transimttingSource, receivingDest), true);

	}
	
	@Override
	public void recoverPath(int src, int dst,int serverSource,int destServer, long jumboFlowId) {
		super.recoverPath(src, dst,serverSource,destServer, jumboFlowId);
		recovered.put(new ImmutablePair<Integer, Integer>(src, dst), true);

	}
	
	public boolean routed(int source, int dest) {
		// TODO Auto-generated method stub
		return routed.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
	}
	
	public boolean recovered(int source, int dest) {
		// TODO Auto-generated method stub
		return recovered.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
	}

}
