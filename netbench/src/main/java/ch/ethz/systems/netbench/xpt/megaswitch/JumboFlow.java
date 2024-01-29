package ch.ethz.systems.netbench.xpt.megaswitch;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * the Jumbo Flow class aggregates flows based on some condition
 */
public class JumboFlow {
    private static long sIdCounter = 0;
    long mId;
    long mSizeByte;

    // a map which keeps track on individual flow sizes.
    HashMap<Long,Long> mFlowIdToSize;

    int mSource;
    int mDest;
    boolean onCircuit;
    HashSet<Long> flows;
    HashSet<Long> flowsOnCircuit;
    private int mSourceToR;
    private int mDestToR;
    private String mState;

    public JumboFlow(int source,int dest){
        mSizeByte = 0;
        mFlowIdToSize = new HashMap<>();
        flowsOnCircuit = new HashSet<>();
        mSource = source;
        mDest = dest;
        mId = ++sIdCounter;
        onCircuit = false;
        mSourceToR = -1;
        mDestToR = -1;
        mState = null;
        flows = new HashSet<>();
    }

    public int getSource(){
        return mSource;
    }

    public int getDest(){
        return mDest;
    }

    /**
     * returns the jumbo flow size
     * @return
     */
    public long getSizeByte(){
        return mSizeByte;
    }

    /**
     * adds the packet size to the jumbo flow size
     * @param packet
     */
    public void onPacketDispatch(TcpPacket packet) {
        packet.setJumboFlowId(this.mId);
        if(packet.isACK()){
            return;
        }
        flows.add(packet.getFlowId());
        long flowSize = mFlowIdToSize.getOrDefault(packet.getFlowId(),0l);
        mSizeByte = mSizeByte + packet.getDataSizeByte();
        mFlowIdToSize.put(packet.getFlowId(),flowSize + packet.getDataSizeByte());

    }

    /**
     * removes a sub flow.
     * @param flowId
     */
    public void onFlowFinished(long flowId){
        if(!mFlowIdToSize.containsKey(flowId)){
            return;
        }
        long flowSize = mFlowIdToSize.remove(flowId);
        flowsOnCircuit.remove(flowId);
        flows.remove(flowId);
        //this.mSizeByte -= flowSize;
    }

    public int getNumFlows() {
        return mFlowIdToSize.size();
    }

	public boolean isTrivial() {
		// TODO Auto-generated method stub
		return mSource==mDest;
	}

	public long getId(){
        return mId;
    }

    /**
     * called when this jumbo flow has entered the circuit (should this go into a sub class)
     */
    public void onCircuitEntrance() {
	    if(!onCircuit){
	        onCircuit = true;

        }
	    Set<Long> flowIds = mFlowIdToSize.keySet();
	    for(long id : flowIds){
            SimulationLogger.registerFlowOnCircuit(id);
        }
    }

    public Set<Long> getFlows(){
        return flows;
    }

    public boolean isOnCircuit() {
        return onCircuit;
    }

    public boolean isOnCircuit(long flowId) {
        return flowsOnCircuit.contains(flowId);
    }

    public boolean hasFlow(long flowId) {
	    return mFlowIdToSize.containsKey(flowId);
    }

    /**
     * gets the size for a specific flow
     * @param flowId
     * @return
     */
    public long getSizeByte(long flowId) {
	    if(!mFlowIdToSize.containsKey(flowId)) return 0;
	    return mFlowIdToSize.get(flowId);
    }

    /**
     * called when a specific flow enters the circuit
     * @param flowId
     */
    public void onCircuitEntrance(long flowId) {
	    SimulationLogger.registerFlowOnCircuit(flowId);
        flowsOnCircuit.add(flowId);

    }

    /**
     * resets flow size in db. if a circuit is established for this flow it may need to be teared down
     * @param flowId
     */
    public void resetFlow(long flowId) {
        long flowSize = mFlowIdToSize.get(flowId);
        mSizeByte -= flowSize;
        assert(mSizeByte>=0);
        mFlowIdToSize.put(flowId,0l);
    }

    public JumboFlow setSourceToR(int sourceToR) {
        mSourceToR = sourceToR;
        return this;
    }

    public JumboFlow setDestToR(int destToR) {
        mDestToR = destToR;
        return this;
    }

    public int getDestToR() {
        return mDestToR;
    }

    public int getSourceToR() {
        return mSourceToR;
    }

    public void setState(String state) {
        mState = state;

    }

    public String getState(){
        return mState;
    }

    public void reset() {
        flowsOnCircuit.clear();
        flows.clear();
        mFlowIdToSize.clear();
        this.mSizeByte = 0;
    }
}
