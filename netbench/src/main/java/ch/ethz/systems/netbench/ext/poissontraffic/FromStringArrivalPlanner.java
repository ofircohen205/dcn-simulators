package ch.ethz.systems.netbench.ext.poissontraffic;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;

import java.util.Map;

public class FromStringArrivalPlanner extends TrafficPlanner {

    private final String arrivals;

    /**
     * Constructor.
     *
     * @param idToTransportLayerMap Maps a network device identifier to its corresponding transport layer
     * @param arrivals              File name of arrival plan
     */
    public FromStringArrivalPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, String arrivals, NBProperties configuration) {
        super(idToTransportLayerMap, configuration);
        this.arrivals = arrivals;
        SimulationLogger.logInfo("Flow planner", "FROM_STRING_ARRIVAL_PLANNER(arrivals=" + arrivals + ")");
    }

    /**
     * Creates plan based on the given string:
     * (start_time, src_id, dst_id, flow_size_byte);(start_time, src_id, dst_id, flow_size_byte);...
     *
     * @param durationNs Duration in nanoseconds
     */
    @Override
    public void createPlan(long durationNs) {

        String[] spl = arrivals.split(";");
        for (String s : spl) {
            s = s.substring(1, s.length() - 1);

            String[] arrivalSpl = s.split(",");
            long startTime = Long.parseLong(arrivalSpl[0].trim());
            int srcId = Integer.parseInt(arrivalSpl[1].trim());
            int dstId = Integer.parseInt(arrivalSpl[2].trim());
            long flowSizeByte = Long.parseLong(arrivalSpl[3].trim());
            this.registerFlow(startTime, srcId, dstId, flowSizeByte, -1);

        }

    }

}
