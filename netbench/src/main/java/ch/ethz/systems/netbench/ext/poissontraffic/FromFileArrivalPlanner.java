package ch.ethz.systems.netbench.ext.poissontraffic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class FromFileArrivalPlanner extends TrafficPlanner {
    private final String fileName;

    /**
     * Constructor.
     *
     * @param idToTransportLayerMap Maps a network device identifier to its corresponding transport layer
     * @param fileName              File name of arrival plan
     */
    public FromFileArrivalPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, String fileName, NBProperties configuration) {
        super(idToTransportLayerMap, configuration);
        this.fileName = fileName;
        SimulationLogger.logInfo("Flow planner", "FROM_FILE_ARRIVAL_PLANNER(fileName=" + fileName + ")");
    }

    /**
     * Creates plan based on the given string:
     * (start_time, src_id, dst_id, flow_size_byte);(start_time, src_id, dst_id, flow_size_byte);...
     *
     * @param durationNs Duration in nanoseconds
     */
    @Override
    public void createPlan(long durationNs) {
        try (BufferedReader br = new BufferedReader(new FileReader(this.fileName))) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] values = line.split(",");
                if (!StringUtils.isNumeric(values[0])) {
                    continue;
                }
                if (values.length == 4) {
                    long time = Long.parseLong(values[0].trim());
                    int source = Integer.parseInt(values[1].trim());
                    int destination = Integer.parseInt(values[2].trim());
                    long flowSizeByte = Long.parseLong(values[3].trim());
                    this.registerFlow(time, source, destination, flowSizeByte, -1);
                    continue;
                }
                long time = Long.parseLong(values[8].trim());
                int source = Integer.parseInt(values[1].trim());
                int destination = Integer.parseInt(values[3].trim());
                long flowSizeByte = Long.parseLong(values[7].trim());
                this.registerFlow(time, source, destination, flowSizeByte, -1);
            }
            // line is not visible here.
        } catch (IOException e) {
            throw new RuntimeException("cannot access csv file " + fileName);
        }
    }
}
