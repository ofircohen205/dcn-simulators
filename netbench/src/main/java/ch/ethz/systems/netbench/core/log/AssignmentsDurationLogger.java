package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.deeplearningtraining.routing.CentralizedController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class AssignmentsDurationLogger {

    // Change track variables
    private final CentralizedController routingStrategy;
    private final Writer writerJobInfoFile;
    private boolean isInfoSavingEnabled;

    public AssignmentsDurationLogger(CentralizedController routingStrategy) {
        this.routingStrategy = routingStrategy;
        this.isInfoSavingEnabled = true;
        String filename = SimulationLogger.getLogsFolder() + "/assignments_duration.csv";
        try {
            this.writerJobInfoFile = new BufferedWriter(new FileWriter(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enable/disable logging the info of flows.
     *
     * @param enabled True iff logging enabled
     */
    public void setInfoSavingEnabled(boolean enabled) {
        this.isInfoSavingEnabled = enabled;
    }

    /**
     * Final flush of logged state.
     */
    public final void finalFlush() {
        if (isInfoSavingEnabled) {
            double averageDuration = routingStrategy.getAverageDuration();
            int numConnections = routingStrategy.getNumAssignedCommodities();
            saveInfo(averageDuration, numConnections);
        }
    }

    /**
     * Save the final flow info.
     *
     * @param averageDuration Average duration
     * @param numConnections  Number of connections
     */
    public void saveInfo(double averageDuration, int numConnections) {
        try {
            writerJobInfoFile.write(averageDuration + "," + numConnections + "\r\n");
            writerJobInfoFile.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
