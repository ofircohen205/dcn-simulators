package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.DistributedOpticServer;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.algorithms.BFS;

import java.util.Map;

public class BFSMetric implements Metric {
    private final Graph[] mGraphs;
    private final BFS[] mBFSs;
    private double mOpportunitiesMissed;
    private double mComparisons;
	private double mNoOpportunity;
    public BFSMetric(Graph[] graphs){
        mGraphs = graphs;
        mComparisons = 0d;
        mOpportunitiesMissed = 0d;
        mNoOpportunity = 0d;
        mBFSs = new BFS[mGraphs.length];
        for(int i=0; i<mBFSs.length;i++){
            mBFSs[i] = new BFS(mGraphs[i]);
        }
        SimulationLogger.registerMetric(this);
    }

    @Override
    public double evaluateRequest(JumboFlow jumboFlow) {
        double evaluation = 0d;
        Map<Integer,NetworkDevice> idToNetworkDevice = BaseInitializer.getInstance().getIdToNetworkDevice();
        DistributedOpticServer serverSourceDevice = (DistributedOpticServer) idToNetworkDevice.get(jumboFlow.getSource());
        DistributedOpticServer serverDestDevice = (DistributedOpticServer) idToNetworkDevice.get(jumboFlow.getDest());
        if(!serverSourceDevice.canAllocateColor(false) || !serverDestDevice.canAllocateColor(true)){
            return evaluation;
        }
        for(int i=0; i<mGraphs.length; i++){

            try{
                if(serverSourceDevice.hasColor(i,false)) continue;
                if(serverDestDevice.hasColor(i,true)) continue;
                int distance = mBFSs[i].getDistance(jumboFlow.getSourceToR(),jumboFlow.getDestToR());
                evaluation = 1d;
                break;
            }catch (NoPathException e){

            }

        }

        return evaluation;
    }

    @Override
    public void evaluate(Evaluation evaluation, boolean result) {
        if(evaluation.isEvaluated()){
            //throw new IllegalStateException("evaluation evaluated more then once");
	    return;
        }
        if(!result && evaluation.getEvaluation()==1d){
            mOpportunitiesMissed +=1d;
        }
        if(!result && evaluation.getEvaluation()==0d){
            mNoOpportunity +=1d;
        }
        mComparisons++;
        evaluation.markEvaluated();
    }

    @Override
    public double calculateMetric(String metric) {
        return mOpportunitiesMissed/mComparisons;
    }

    @Override
    public void outputMetricPeriodic() {
        System.out.println(this.toString());
    }

    @Override
    public String toString() {
        return "BFS (missed opportunities): " + mOpportunitiesMissed/mComparisons + "\nBFS (no opportunity): " + mNoOpportunity/mComparisons;
    }


}
