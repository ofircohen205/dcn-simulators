package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics;

import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;

public class Evaluation {

    private boolean mEvaluated;
    private double mEvaluation;
    private Metric mMetric;

    public Evaluation(Metric metric){
        mEvaluated = false;
        mMetric = metric;
    }

    public boolean isEvaluated() {
        return mEvaluated;
    }

    public double getEvaluation() {
        return mEvaluation;
    }

    public void markEvaluated() {
        mEvaluated = true;
    }

    public void evaluateRequest(JumboFlow jumboFlow) {
        mEvaluation = mMetric.evaluateRequest(jumboFlow);
    }

    public void evaluate(boolean finalResult) {
        mMetric.evaluate(this,finalResult);
    }
}
