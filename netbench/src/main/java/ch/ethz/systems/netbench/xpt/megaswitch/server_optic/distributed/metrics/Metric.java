package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics;

import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;

public interface Metric {
    public double evaluateRequest(JumboFlow jumboFlow);
    public double calculateMetric(String metric);
    public void outputMetricPeriodic();
    void evaluate(Evaluation evaluation, boolean finalResult);
}
