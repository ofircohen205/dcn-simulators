package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;

public class AvgSuccessMetric  implements Metric{
    private long mEvaluations;
    private long mSuccesses;
    private long mPeriodicEvaluations;
    private long mPeriodicSuccesses;
    private int fileIndex;
    BufferedWriter periodicWriter;
    private HashSet<Pair<Integer,Integer>> mCommodities;
    public AvgSuccessMetric(){
        mEvaluations = 0;
        mSuccesses = 0;
        fileIndex = 0;
        mPeriodicEvaluations = 0;
        mPeriodicSuccesses = 0;
        mCommodities = new HashSet<>();
        SimulationLogger.initCommoditiesFolder();
        SimulationLogger.registerMetric(this);
        periodicWriter = SimulationLogger.getExternalWriter("commodities/avg_periodic_successes.log");
    }
    @Override
    public double evaluateRequest(JumboFlow jumboFlow) {
        mCommodities.add(new ImmutablePair<>(jumboFlow.getSource(),jumboFlow.getDest()));
        return 1d;
    }

    @Override
    public double calculateMetric(String metric) {
        try{
            System.out.println("successes " + mSuccesses);
            System.out.println("evaluations " + mEvaluations);
            return new Double(mSuccesses)/ new Double(mEvaluations);
        }catch (ArithmeticException e){
            //could be division by zero
        }
        return 0;
    }

    @Override
    public void outputMetricPeriodic() {
        try{
            Double periodicAvg = new Double(mPeriodicSuccesses)/ new Double(mPeriodicEvaluations);
            System.out.println("periodic success rate " + periodicAvg);
            periodicWriter.write(periodicAvg +"\n");

        }catch (ArithmeticException e){
            //could be division by zero
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SimulationLogger.registerCommodities("commodities_" + fileIndex,mCommodities);
        mCommodities.clear();
        mPeriodicEvaluations = 0;
        mPeriodicSuccesses = 0;
        fileIndex++;
    }

    @Override
    public void evaluate(Evaluation evaluation, boolean finalResult) {
        if(finalResult){
            mSuccesses++;
            mPeriodicSuccesses++;
        }
        mEvaluations++;
        mPeriodicEvaluations++;
    }

    @Override
    public String toString(){
        return "AvgSuccesses " + Double.toString(calculateMetric(null));
    }

}
