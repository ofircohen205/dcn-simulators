package ch.ethz.systems.netbench.xpt.remotesourcerouting;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.xpander.XpanderRouter;
import edu.asu.emit.algorithm.graph.Path;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class MockRemoteRouter extends XpanderRouter {

	HashMap<Pair<Integer,Integer>,Boolean> routed;
	HashMap<Pair<Integer,Integer>,Boolean> recovered;

	public MockRemoteRouter(Map<Integer, NetworkDevice> idToNetworkDevice,NBProperties configuration) {
		super(idToNetworkDevice, configuration);
		routed = new HashMap<>();
		recovered = new HashMap<>();
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void switchPath(int src,int dst, Path newPath,long flowId) {
		
		super.switchPath(src, dst, newPath, flowId);
	}
	
	@Override
	public void recoverPath(int src, int dst,int serverSource,int destServer, long jumboFlowId){
		super.recoverPath(src, dst, serverSource, destServer, jumboFlowId);
		recovered.put(new ImmutablePair<Integer, Integer>(serverSource, destServer), true);
	}
	
	@Override
	protected Path generatePathFromGraph(int source,int dest) {
		
		return super.generatePathFromGraph(source, dest);
	}
	
	protected Path getPath(int src,int dst) {
		return  mPaths.get(new ImmutablePair<>(src,dst));
	}
	
	@Override
	protected void updateForwardingTables(int source, int dest, Path p, long flowId) {
		super.updateForwardingTables(source, dest, p, flowId);
		routed.put(new ImmutablePair<Integer, Integer>(source, dest), true);
	}

	public boolean routed(int source, int dest) {
		// TODO Auto-generated method stub
		return routed.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
	}
	
	public boolean recovered(int source, int dest) {
		// TODO Auto-generated method stub
		return recovered.getOrDefault(new ImmutablePair<Integer, Integer>(source, dest), false);
	}
	
	@Override
	public void reset(){
		super.reset();
		recovered.clear();
		routed.clear();
	}


}
