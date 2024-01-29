package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics.AvgSuccessMetric;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics.BFSMetric;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics.Evaluation;
import ch.ethz.systems.netbench.xpt.xpander.SemiXpanderServerOptics;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

/**
 * this class is used mainly for logging, and some global variables
 */
public class DistributedController extends SemiXpanderServerOptics
{
	long doubleSuccesses = 0;
	long successes = 0;
	long failures = 0;
	long torNoPath = 0;
	long destNoPath = 0;
	long sourceNoPath = 0;
	int concurrentPaths = 0;
	int  maxConcurrentPaths = 0;
	long edgesUsed = 0;
    private long edgesUsedPerFailureSum = 0;
    private BFSMetric mBfsMetric;
    private AvgSuccessMetric mAvgSuccessMetric;

    public DistributedController(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
        System.out.println("running distributed controller with max flows on circuit " + mMaxNumJFlowsOncircuit);
        mBfsMetric = new BFSMetric(mGraphs);
        mAvgSuccessMetric = new AvgSuccessMetric();

    }

    public void initRoute(int transimttingSource, int receivingDest, int sourceKey, int destKey, long jumboFlowId, long sizeBit){
        throw new RuntimeException("cant use initRoute for distributed setting!");
    }

    public void recoverPath(int sourceToR, int destToR, int serverSource, int serverDest,long flowId){
        throw new RuntimeException("cant use recoverPath for distributed setting!");
    }

    public Evaluation evaluateRequest(JumboFlow jumboFlow){
        Evaluation evaluation = new Evaluation(mAvgSuccessMetric);
        evaluation.evaluateRequest(jumboFlow);
        return evaluation;
    }

    public void evaluate(LinkedList<Evaluation> evaluations, boolean finalResult){
        evaluations.get(0).evaluate(finalResult);
    }

    /**
     * returns the edge capacity of graph color, and edge source-nextHop
     * @param source
     * @param nextHop
     * @param color
     * @return
     */
    public long checkEdgeCapacity(int source, int nextHop, int color) {
        return mGraphs[color].getEdgeCapacity(new Vertex(source),new Vertex(nextHop));
    }

    /**
     * decreases capacity by 1 for graph color, and edge source-nextHop
     * @param source
     * @param nextHop
     * @param color
     */
    public void decreaseEdgeCapacity(int source, int nextHop, int color) {
        mGraphs[color].decreaseCapacity(new ImmutablePair<>(source,nextHop));
        edgesUsed++;

    }

    /**
     * increase capacity by 1 for graph color, and edge source-nextHop
     * @param source
     * @param nextHop
     * @param color
     */
    public void increaseEdgeCapacity(int source, int nextHop, int color) {
        mGraphs[color].increaseCapacity(new ImmutablePair<>(source,nextHop));
        edgesUsed--;
        assert(edgesUsed>=0);
    }


    /**
     * updates id device such that they will forward packets with prevHop and color to nextHop
     * @param id
     * @param prevHop
     * @param nextHop
     * @param color
     */
    public void updateRoutingTable( int id, int prevHop, int nextHop,int color) {
        DistributedSourceRoutingSwitch drs = (DistributedSourceRoutingSwitch) mIdToNetworkDevice.get(id);
        drs.updateForwardingTable(prevHop,nextHop,color);
    }

    public int getWaveLengthNum() {
        return mGraphs.length;
    }

    public String getCurrentState() {
        // TODO Auto-generated method stub
    	System.out.println("oldes paths");
        SimulationLogger.printOldestPaths();
        System.out.println("oldest states");
        SimulationLogger.printOldestDistProtocolStates();
        String state = " Allocated " + mAllocateddPathsNum + " Deallocated " + mDeAllocatedPathsNum + "\n";
        state += " max concurrent paths " + maxConcurrentPaths + "\n";
        state += "double success count " + (SimulationLogger.getStatistic("DISTRIBUTED_PATH_DOUBLE_SUCCESS_COUNT") - doubleSuccesses) + "\n";
        long failNum = (SimulationLogger.getStatistic("DISTRIBUTED_PATH_FAILURE_COUNT") -failures);
        state += "success count " + (SimulationLogger.getStatistic("DISTRIBUTED_PATH_SUCCESS_COUNT") - successes) + "\n";
        state += "failure count " + failNum  + "\n";
        
        state += "tor no path count " + (SimulationLogger.getStatistic("DISTRIBUTED_TOR_NO_PATH") - torNoPath) + "\n";
        state += "dest no path count " + (SimulationLogger.getStatistic("DISTRIBUTED_DEST_ENDPOINT_NO_PATH") - destNoPath) + "\n";
        state += "source no path count " + (SimulationLogger.getStatistic("DISTRIBUTED_SOURCE_ENDPOINT_NO_PATH") -sourceNoPath) + "\n";
        
        state += "ack loss " + SimulationLogger.getStatistic("ACK_PACKETS_DROPPED") + "\n";
        state += "packet loss " + SimulationLogger.getStatistic("PACKETS_DROPPED") + "\n";
        state += "packet loss at source " + SimulationLogger.getStatistic("PACKETS_DROPPED_AT_SOURCE")+ "\n";
        state += "packet loss at conversion " + SimulationLogger.getStatistic("PACKETS_DROPPED_ON_CONVERSION")+ "\n";
        state += "auto teardowns " + SimulationLogger.getStatistic("AUTO_CIRCUIT_TEARDOWN_COUNT")+ "\n";
        state += "packets on circuit " + SimulationLogger.getStatistic("PACKET_ROUTED_THROUGH_CIRCUIT") + "\n";
        mAvgSuccessMetric.outputMetricPeriodic();
        
        doubleSuccesses = SimulationLogger.getStatistic("DISTRIBUTED_PATH_DOUBLE_SUCCESS_COUNT");
        successes = SimulationLogger.getStatistic("DISTRIBUTED_PATH_SUCCESS_COUNT");
        failures = SimulationLogger.getStatistic("DISTRIBUTED_PATH_FAILURE_COUNT");
        try{
            state += "avg edge used per failure " + edgesUsedPerFailureSum/failures + "\n";
        }catch (ArithmeticException e){

        }

        state += mBfsMetric.toString() + "\n";
        torNoPath = SimulationLogger.getStatistic("DISTRIBUTED_TOR_NO_PATH");
        destNoPath = SimulationLogger.getStatistic("DISTRIBUTED_DEST_ENDPOINT_NO_PATH");
        sourceNoPath = SimulationLogger.getStatistic("DISTRIBUTED_SOURCE_ENDPOINT_NO_PATH");
        
        mAllocateddPathsNum = 0;
        mDeAllocatedPathsNum = 0;
        return state;
    }

    public void onDeallocation() {
        mDeAllocatedPathsNum++;
        concurrentPaths--;
        assert(concurrentPaths>=0);
    }

    public void onAallocation() {
        mAllocateddPathsNum++;
        concurrentPaths++;
        if(concurrentPaths > maxConcurrentPaths) {
        	maxConcurrentPaths = concurrentPaths;
        }
    }



    public void onPathFailure() {
        SimulationLogger.increaseStatisticCounter("DISTRIBUTED_PATH_FAILURE_COUNT");
        edgesUsedPerFailureSum += edgesUsed;
    }
}
