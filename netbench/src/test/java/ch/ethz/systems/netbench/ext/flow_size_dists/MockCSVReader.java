package ch.ethz.systems.netbench.ext.flow_size_dists;

import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FromCSV;

public class MockCSVReader extends FromCSV {
    public MockCSVReader(String csvFile) {
        super(csvFile);
    }

    protected long getFlowSize(double prob){
        return super.getFlowSize(prob);
    }
}
