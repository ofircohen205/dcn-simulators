package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.run.MainFromProperties;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics.Metric;
import edu.asu.emit.algorithm.graph.Path;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimulationLogger {
    private static final HashMap<Long, Path> activePathMap = new HashMap<>();
    private static final Stack<Long> oldestActivePaths = new Stack<>();
    private static final Stack<Long> oldestDistProtocolStates = new Stack<>();
    private static final HashMap<Long, JumboFlow> activeDistProtocolStates = new HashMap<>();
    private static final Map<String, BufferedWriter> writersAdded = new HashMap<>();
    // Specific component loggers
    private static final List<PortLogger> portLoggers = new ArrayList<>();
    private static final Map<Long, FlowLogger> flowLoggerByFlowId = new HashMap<>();
    private static final Map<Long, FlowLogger> activeFlowLoggerByFlowId = new HashMap<>();
    private static final List<LoggerCallback> callbacks = new ArrayList<>();
    private static final HashMap<Long, Long> flowsOnCircuit = new HashMap<>();
    private static final HashMap<Long, Set<Long>> flowsRequestCircuit = new HashMap<>();
    // Statistic counters
    private static final Map<String, Long> statisticCounters = new HashMap<>();

    public static int numberOfFlows;
    private static BufferedWriter writerRemainingPaths;
    // Main token identifying the run log folder
    private static String runFolderName;
    private static String baseDir;
    private static String commonBase;
    // Access files for logging (are kept open during simulation run)
    private static BufferedWriter writerRunInfoFile;
    private static BufferedWriter writerFlowCompletionCsvFile;
    private static BufferedWriter writerFlowThroughputFile;
    private static BufferedWriter writerFlowCompletionFile;
    private static BufferedWriter writerPortQueueStateFile;
    private static BufferedWriter writerPortUtilizationFile;
    private static BufferedWriter writerPortUtilizationCsvFile;
    private static BufferedWriter writerRemoteRouterPathLog;
    private static BufferedWriter writerFlowOnCircuit;
    private static BufferedWriter writerFlowsOnCircuitEntranceTime;
    private static BufferedWriter writerRemoteRouterStateLogCSV;
    private static BufferedWriter writerRemoteRouterDropStatisticsCSV;
    private static BufferedWriter writerECNStatistics;
    private static BufferedWriter writerPacketsDispatchedStatistics;
    private static BufferedWriter commonDropStatisticsWriter;
    // Print streams used
    private static PrintStream originalOutOutputStream;
    private static PrintStream originalErrOutputStream;
    private static OutputStream underlyingFileOutputStream;
    // Settings
    private static boolean logHumanReadableFlowCompletionEnabled;
    private static BufferedWriter flowRequestLogWriter;
    private static LinkedList<Metric> sMetrics;
    private static BufferedWriter deltaTStatisticsWriter;
    private static BufferedWriter collisionStatisticsWriter;

    /**
     * Increase a basic statistic counter with the given name by one.
     *
     * @param name Statistic name
     */
    public static void increaseStatisticCounter(String name) {
        statisticCounters.merge(name, 1L, Long::sum);
    }

    public static void increaseStatisticCounterBy(String name, long increaseBy) {
        Long val = statisticCounters.get(name);
        if (val == null) {
            val = 0L;
        }
        statisticCounters.put(name, val + increaseBy);
    }

    /**
     * Register a port logger so that it can be
     * later called after the run is over to collect
     * is statistics.
     *
     * @param logger Port logger instance
     */
    static void registerPortLogger(PortLogger logger) {
        portLoggers.add(logger);
    }

    /**
     * Register a flow logger so that it
     * can be later called after the run is over to
     * collect its statistics.
     *
     * @param logger Flow logger instance
     */
    static void registerFlowLogger(FlowLogger logger) {
        flowLoggerByFlowId.put(logger.getFlowId(), logger);
        activeFlowLoggerByFlowId.put(logger.getFlowId(), logger);
    }

    /**
     * Retrieve the full absolute path of the run folder.
     *
     * @return Full run folder path
     */
    public static String getRunFolderFull() {
        return baseDir + "/" + runFolderName;
    }

    /**
     *
     */
    public static String getLogsFolder() {
        return getRunFolderFull() + "/logs";
    }

    /**
     * Retrieve the run folder name
     *
     * @return run folder name
     */
    public static String getRunFolderName() {
        return runFolderName;
    }

    /**
     * Open log file writer without a specific run folder name.
     */
    public static void open() {
        open(null);
    }

    /**
     * Open log file writers with a specific run folder name.
     *
     * @param tempRunConfiguration Temporary run configuration (not yet centrally
     *                             loaded)
     */
    public static void open(NBProperties tempRunConfiguration) {
        // Settings
        String specificRunFolderName = null;
        String specificRunFolderBaseDirectory = null;
        if (tempRunConfiguration != null) {
            // logPacketBurstGapEnabled =
            // tempRunConfiguration.getBooleanPropertyWithDefault("enable_log_packet_burst_gap",
            // false);

            // Run folder
            specificRunFolderName = tempRunConfiguration.getPropertyWithDefault(
                    Constants.Simulation.RUN_FOLDER_NAME, null);
            specificRunFolderBaseDirectory = tempRunConfiguration.getPropertyWithDefault(
                    Constants.Simulation.RUN_FOLDER_BASE_DIR, null);

            // Enabling human readable version
            logHumanReadableFlowCompletionEnabled = tempRunConfiguration.getBooleanPropertyWithDefault(
                    Constants.Logger.ENABLE_GENERATE_HUMAN_READABLE_FLOW_COMPLETION_LOG,
                    true);
        }

        // Overwrite if run folder name was specified in run configuration
        if (specificRunFolderName == null) {
            runFolderName = "nameless_run_" + new SimpleDateFormat("yyyy-MM-dd--HH'h'mm'm'ss's'").format(new Date());
        } else {
            runFolderName = specificRunFolderName;
        }

        // Overwrite if run folder name was specified in run configuration
        if (specificRunFolderBaseDirectory == null) {
            baseDir = "./temp";
        } else {
            baseDir = specificRunFolderBaseDirectory;
        }

        sMetrics = new LinkedList<>();
        try {

            // Create run token folder
            new File(getLogsFolder()).mkdirs();
            // Copy console output to the run folder
            FileOutputStream fosOS = new FileOutputStream(getLogsFolder() + "/console.txt");
            TeeOutputStream customOutputStreamOut = new TeeOutputStream(System.out, fosOS);
            TeeOutputStream customOutputStreamErr = new TeeOutputStream(System.err, fosOS);
            underlyingFileOutputStream = fosOS;
            originalOutOutputStream = System.out;
            originalErrOutputStream = System.err;
            System.setOut(new PrintStream(customOutputStreamOut));
            System.setErr(new PrintStream(customOutputStreamErr));
            writerRemainingPaths = openWriter("active_paths.log");
            // Info
            writerRunInfoFile = openWriter("initialization.info");
            flowRequestLogWriter = openWriter("flow_circuit_request_log.log");
            writerFlowsOnCircuitEntranceTime = openWriter("flow_circuit_entrance_times.log");
            writerFlowOnCircuit = openWriter("flows_on_circuit.log");
            // Port log writers
            writerPortQueueStateFile = openWriter("port_queue_length.csv");
            writerPortQueueStateFile.write("source_id,target_id,queue_length,buffer_occupied_bits,absolute_time\n");
            writerPortUtilizationCsvFile = openWriter("port_utilization.csv");
            writerPortUtilizationFile = openWriter("port_utilization.log");

            writerRemoteRouterPathLog = openWriter("remote_router_path.log");
            writerRemoteRouterStateLogCSV = openWriter("remote_router_state.csv");
            writerRemoteRouterDropStatisticsCSV = openWriter("remote_router_drop_statistics.csv");
            // Flow log writers
            writerFlowThroughputFile = openWriter("flow_throughput.csv");
            writerFlowCompletionCsvFile = openWriter("flow_info.csv");
            writerFlowCompletionFile = openWriter("flow_completion.log");

            writerECNStatistics = openWriter("ecn_statistics.log");
            writerPacketsDispatchedStatistics = openWriter("packets_dispatched.log");

            deltaTStatisticsWriter = openWriter("delta_t_statistics.csv");
            deltaTStatisticsWriter.write("large_flows,large_finished_flows\n");
            deltaTStatisticsWriter.flush();

            collisionStatisticsWriter = openWriter("delta_t_collision_statistics.log");
            // Writer out the final properties' values
            if (tempRunConfiguration != null) {
                BufferedWriter finalPropertiesInfoFile = openWriter("final_properties.info");
                finalPropertiesInfoFile.write(tempRunConfiguration.getAllPropertiesToString());
                finalPropertiesInfoFile.close();
            }
        } catch (IOException e) {
            throw new LogFailureException(e);
        }

    }

    public static void openCommon(NBProperties tempRunConfiguration) {
        if (!tempRunConfiguration.hasSubConfiguration()) {
            return;
        }
        String base = tempRunConfiguration.getPropertyWithDefault(Constants.Simulation.COMMON_BASE_DIR,
                tempRunConfiguration.getPropertyOrFail(Constants.Simulation.RUN_FOLDER_BASE_DIR));

        new File(base).mkdirs();
        commonBase = base;
        if (tempRunConfiguration.getPropertyWithDefault(
                Constants.RemoteRoutingPopulator.CENTERED_ROUTING_TYPE, null) != null) {
            commonDropStatisticsWriter = openCommonWriter("common_drop_statistics.csv");
            try {
                commonDropStatisticsWriter.write("run_folder,failures,successes,percentage\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("could not open common log");
            }
        }
    }

    public static void closeCommon() {
        if (commonDropStatisticsWriter != null) {
            try {
                commonDropStatisticsWriter.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("could not close common log");
            }
        }
    }

    private static BufferedWriter openCommonWriter(String logFileName) {
        try {
            return new BufferedWriter(
                    new FileWriter(getCommonBaseDir() + "/" + logFileName));
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    public static String getBaseFolder() {
        return baseDir;
    }

    private static String getCommonBaseDir() {
        return commonBase;
    }

    /**
     * Register the call back of a logger before the close of the simulation logger.
     *
     * @param callback Callback instance
     */
    public static void registerCallbackBeforeClose(LoggerCallback callback) {
        callbacks.add(callback);
    }

    /**
     * Open a log writer in the run _path.
     *
     * @param logFileName Log file name
     * @return Writer of the log
     */
    public static BufferedWriter openWriter(String logFileName) throws LogFailureException {
        return openWriter(logFileName, getLogsFolder(), true);
    }

    public static BufferedWriter openWriter(String logFileName, String folderPath, boolean append) {
        try {
            return new BufferedWriter(
                    new FileWriter(folderPath + "/" + logFileName, append));
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Open a log writer in the run _path.
     *
     * @param logFileName Log file name
     * @return Writer of the log
     */
    public static BufferedReader openReader(String logFileName) throws LogFailureException {
        return openReader(logFileName, getRunFolderFull());
    }

    private static BufferedReader openReader(String logFileName, String folderPath) {
        try {
            return new BufferedReader(
                    new FileReader(folderPath + "/" + logFileName));
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Create (or fetch) an external writer, which can be used to create your own
     * personal logs.
     *
     * @param logFileName Log file name
     * @return Writer instance (already opened, is automatically closed when calling
     * {@link #close()})
     */
    public static BufferedWriter getExternalWriter(String logFileName) {
        BufferedWriter writer = writersAdded.get(logFileName);
        if (writer == null) {
            writer = openWriter(logFileName);
            writersAdded.put(logFileName, writer);
        }
        return writer;
    }

    /**
     * Create (or fetch) an external writer, which can be used to create your own
     * personal logs.
     *
     * @param logFileName Log file name
     * @param folderPath  Folder path
     * @return Writer instance (already opened, is automatically closed when calling
     * {@link #close()})
     */
    public static BufferedWriter getExternalWriter(String logFileName, String folderPath) {
        BufferedWriter writer = writersAdded.get(logFileName);
        if (writer == null) {
            writer = openWriter(logFileName, folderPath, true);
            writersAdded.put(logFileName, writer);
        }
        return writer;
    }

    /**
     * Log summaries and close log file writers.
     */
    public static void close() {

        // Callback loggers to finalize their logs
        for (LoggerCallback callback : callbacks) {
            callback.callBeforeClose();
        }
        callbacks.clear();

        // Most important logs
        logFlowSummary();
        logPortUtilization();
        logCircuitFlows();
        logECNStatistics();
        logPacketsDispatched();
        try {
            SortedSet<Long> keys = new TreeSet<>(activePathMap.keySet());
            for (Long key : keys) {
                writerRemainingPaths.write(activePathMap.get(key).toString() + "\n");
            }
            // Write basic statistics about the run
            BufferedWriter writerStatistics = openWriter("statistics.log");
            ArrayList<String> stats = new ArrayList<>(statisticCounters.keySet());
            Collections.sort(stats);
            for (String s : stats) {
                writerStatistics.write(s + ": " + statisticCounters.get(s) + "\n");
            }

            BufferedWriter writerMetrics = openWriter("metrics.log");
            for (Metric metric : sMetrics) {
                writerMetrics.write(metric.toString() + "\n");
            }
            writerMetrics.close();
            writerStatistics.close();
            statisticCounters.clear();
            // Close *all* the running log files
            writerRunInfoFile.close();
            writerFlowCompletionCsvFile.close();
            writerFlowCompletionCsvFile.close();
            writerFlowThroughputFile.close();
            writerPortQueueStateFile.close();
            writerPortUtilizationFile.close();
            writerPortUtilizationCsvFile.close();
            writerFlowCompletionFile.close();
            writerRemoteRouterPathLog.close();
            writerRemoteRouterStateLogCSV.close();
            writerRemoteRouterDropStatisticsCSV.close();
            writerFlowOnCircuit.close();
            writerFlowsOnCircuitEntranceTime.close();
            flowRequestLogWriter.close();
            writerRemainingPaths.close();
            writerECNStatistics.close();
            writerPacketsDispatchedStatistics.close();
            deltaTStatisticsWriter.close();
            collisionStatisticsWriter.close();
            // Also added ones are closed automatically at the end
            for (BufferedWriter writer : writersAdded.values()) {
                writer.close();
            }
            writersAdded.clear();

            // Set diverted print streams back
            System.out.flush();
            System.err.flush();
            System.setOut(originalOutOutputStream);
            System.setErr(originalErrOutputStream);
            underlyingFileOutputStream.close();

            // Clear loggers
            portLoggers.clear();
            flowLoggerByFlowId.clear();
            activeFlowLoggerByFlowId.clear();

            deleteNonimportantFiles();

        } catch (IOException e) {
            throw new LogFailureException(e);
        }

    }

    private static void logPacketsDispatched() {
        try {
            // Header
            writerPacketsDispatchedStatistics.write(
                    String.format(
                            "%-6s%-6s%-9s%-16s\n",
                            "Src",
                            "Dst",
                            "Srvport",
                            "Packets Dispatched"));

            portLoggers.sort((o1, o2) -> {
                int delta = Integer.compare(o1.getOwnId(), o2.getOwnId());
                if (delta != 0) {
                    return delta;
                } else {
                    return Integer.compare(o1.getTargetId(), o2.getTargetId());
                }
            });

            for (PortLogger logger : portLoggers) {
                writerPacketsDispatchedStatistics.write(
                        String.format(
                                "%-6d%-6d%-9s%-16d\n",
                                logger.getOwnId(),
                                logger.getTargetId(),
                                (logger.isAttachedToServer() ? "YES" : "NO"),
                                logger.getPacketsDispatched()));
            }
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    private static void logECNStatistics() {
        try {
            // Header
            writerECNStatistics.write(
                    String.format(
                            "%-6s%-6s%-9s%-16s\n",
                            "Src",
                            "Dst",
                            "Srvport",
                            "ECN Marks"));

            portLoggers.sort((o1, o2) -> {
                long delta = o2.getECNMarks() - o1.getECNMarks();
                if (delta < 0) {
                    return -1;
                } else if (delta > 0) {
                    return 1;
                } else {
                    return 0;
                }
            });

            for (PortLogger logger : portLoggers) {
                writerECNStatistics.write(
                        String.format(
                                "%-6d%-6d%-9s%-16d\n",
                                logger.getOwnId(),
                                logger.getTargetId(),
                                (logger.isAttachedToServer() ? "YES" : "NO"),
                                logger.getECNMarks()));
            }
        } catch (IOException e) {
            throw new LogFailureException(e);
        }

    }

    private static void logCircuitFlows() {
        try {
            for (long id : flowsOnCircuit.keySet()) {
                writerFlowOnCircuit.write(id + "\n");
                writerFlowsOnCircuitEntranceTime.write(id + ":" + flowsOnCircuit.get(id) + "\n");
            }
            for (long id : flowsRequestCircuit.keySet()) {
                flowRequestLogWriter.write(id + ":");
                Set<Long> requests = new TreeSet(flowsRequestCircuit.get(id));
                for (long request : requests) {
                    flowRequestLogWriter.write(request + ",");
                }
                if (flowsOnCircuit.containsKey(id)) {
                    flowRequestLogWriter.write("TRUE");

                } else {
                    flowRequestLogWriter.write("FALSE");
                }
                flowRequestLogWriter.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Log a general parameter to indicate some information
     * about what was done in the run.
     *
     * @param key   Key string
     * @param value Value string
     */
    public static void logInfo(String key, String value) {
        try {
            writerRunInfoFile.write(key + ": " + value + "\n");
            writerRunInfoFile.flush();
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Log that flow <code>flowId</code> originating from network device
     * <code>sourceId</code> has
     * sent a total of <code>amountBytes</code> in the past <code>timeNs</code>
     * nanoseconds.
     *
     * @param flowId         Unique flow identifier
     * @param sourceId       Source network device identifier
     * @param targetId       Target network device identifier
     * @param amountBytes    Amount of bytes sent in the interval
     * @param absStartTimeNs Interval start in nanoseconds
     * @param absEndTimeNs   Interval end in nanoseconds
     */
    static void logFlowThroughput(long flowId, int sourceId, int targetId, long amountBytes, long absStartTimeNs,
                                  long absEndTimeNs) {
        /*
         * try {
         * writerFlowThroughputFile.write(flowId + "," + sourceId + "," + targetId + ","
         * + amountBytes + "," + absStartTimeNs + "," + absEndTimeNs + "\n");
         * } catch (IOException e) {
         * throw new LogFailureException(e);
         * }
         */
    }

    public static void logRemoteRoute(Path p, int source, int dest, long flowId, long time, boolean adding)
            throws IOException {
        String add = adding ? "+" : "-";
        writerRemoteRouterPathLog.write(String.format(
                add + " %-11s %-6s%-6s%-13s%-13s\n",
                p.toString(),
                flowId,
                source,
                dest,
                time));
    }

    /**
     * Log the queue length of a specific output port at a certain point in time.
     *
     * @param ownId              Port source network device identifier (device to
     *                           which it is attached)
     * @param targetId           Port target network device identifier (where the
     *                           other end of the cable is connected to)
     * @param queueLength        Current length of the queue
     * @param bufferOccupiedBits Amount of bits occupied in the buffer
     * @param absTimeNs          Absolute timestamp in nanoseconds since simulation
     *                           epoch
     */
    static void logPortQueueState(long ownId, long targetId, int queueLength, long bufferOccupiedBits, Packet p,
                                  long absTimeNs) {
        try {
            writerPortQueueStateFile.write(ownId + "," + targetId + "," + queueLength + "," + bufferOccupiedBits + ","
                    + absTimeNs + "\n");
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Print a human-readable summary of all the active_flows and whether they were
     * completed.
     */
    private static void logFlowSummary() {
        if (!logHumanReadableFlowCompletionEnabled) {
            return;
        }
        try {
            // Header
            writerFlowCompletionFile.write(
                    String.format(
                            "%-11s%-6s%-6s%-13s%-13s%-15s%-10s\n",
                            "FlowId",
                            "Src",
                            "Dst",
                            "Sent (byte)",
                            "Total (byte)",
                            "Duration (ms)",
                            "Progress"));

            // Sort them based on starting time
            List<FlowLogger> flowLoggers = new ArrayList<>(flowLoggerByFlowId.values());
            flowLoggers.sort((flowLogger1, flowLogger2) -> {
                long delta = flowLogger2.getFlowStartTime() - flowLogger1.getFlowStartTime();
                if (delta < 0) {
                    return 1;
                } else if (delta > 0) {
                    return -1;
                } else {
                    return 0;
                }
            });

            for (FlowLogger logger : flowLoggers) {
                writerFlowCompletionFile.write(
                        String.format(
                                "%-11s%-6s%-6s%-13s%-13s%-8.2f%-7s%.2f%%\n",
                                logger.getFlowId(),
                                logger.getSourceId(),
                                logger.getTargetId(),
                                logger.getTotalBytesReceived(),
                                logger.getFlowSizeByte(),
                                (logger.isCompleted() ? (logger.getFlowEndTime() - logger.getFlowStartTime()) / 1e6
                                        : (Simulator.getCurrentTime() - logger.getFlowStartTime()) / 1e6),
                                (logger.isCompleted() ? "" : " (DNF)"),
                                ((double) logger.getTotalBytesReceived() / (double) logger.getFlowSizeByte())
                                        * 100));
                if (!logger.isCompleted()) {
                    logFinishedFlow(logger);
                }
            }
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Print a human-readable summary of all the port utilization.
     */
    private static void logPortUtilization() {
        if (writerPortUtilizationFile == null)
            return;
        try {
            // Header
            writerPortUtilizationFile.write(
                    String.format(
                            "%-6s%-6s%-9s%-16s%s\n",
                            "Src",
                            "Dst",
                            "Srvport",
                            "Utilized (ns)",
                            "Utilization"));

            // Sort them based on utilization
            portLoggers.sort((o1, o2) -> {
                long delta = o2.getUtilizedNs() - o1.getUtilizedNs();
                if (delta < 0) {
                    return -1;
                } else if (delta > 0) {
                    return 1;
                } else {
                    return 0;
                }
            });

            // Data entries
            for (PortLogger logger : portLoggers) {
                writerPortUtilizationCsvFile.write(
                        logger.getOwnId() + "," +
                                logger.getTargetId() + "," +
                                (logger.isAttachedToServer() ? "Y" : "N") + "," +
                                logger.getUtilizedNs() + "," +
                                (((double) logger.getUtilizedNs() / (double) Simulator.getCurrentTime()) * 100) + "\n");
                writerPortUtilizationFile.write(
                        String.format(
                                "%-6d%-6d%-9s%-16d%.2f%%\n",
                                logger.getOwnId(),
                                logger.getTargetId(),
                                (logger.isAttachedToServer() ? "YES" : "NO"),
                                logger.getUtilizedNs(),
                                ((double) logger.getUtilizedNs() / (double) Simulator.getCurrentTime()) * 100));
            }

        } catch (IOException e) {
            throw new LogFailureException(e);
        }

    }

    public static void logFinishedFlow(FlowLogger logger) {
        long flowEndTime = logger.isCompleted() ? logger.getFlowEndTime() : Simulator.getCurrentTime();
        long flowTotalTime = logger.isCompleted() ? logger.getFlowEndTime() - logger.getFlowStartTime()
                : Simulator.getCurrentTime() - logger.getFlowStartTime();
        try {
            writerFlowCompletionCsvFile.write(
                    logger.getFlowId() + "," +
                            logger.getSourceId() + "," +
                            logger.getSourceTorId() + "," +
                            logger.getTargetId() + "," +
                            logger.getTargetTorId() + "," +
                            logger.getCoreId() + "," +
                            logger.getJobId() + "," +
                            logger.getTotalBytesReceived() + "," +
                            logger.getFlowSizeByte() + "," +
                            logger.getFlowStartTime() + "," +
                            flowEndTime + "," +
                            flowTotalTime + "," +
                            (logger.isCompleted() ? "TRUE" : "FALSE") + "\n");
            writerFlowCompletionCsvFile.flush();
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    /**
     * Completely throw away all the logs generated in this run.
     * <p>
     * Adapted from:
     * http://stackoverflow.com/questions/7768071/how-to-delete-directory-content-in-java
     */
    private static void throwaway() {
        boolean success = false;
        String fol = getRunFolderFull();
        File folder = new File(fol);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    success = f.delete() || success;
                }
            }
        }
        success = folder.delete() || success;

        // Failure to throw away log files
        if (!success) {
            throw new RuntimeException("Throw away failed, could not delete one or more files/directories.");
        }

    }

    /**
     * Copy the configuration files.
     *
     * @param configuration
     */
    public static void copyRunConfiguration(NBProperties configuration) {
        copyFileToRunFolder(configuration.getFileName());
    }

    /**
     * Copy any desired file to the run folder.
     *
     * @param fileName File name
     */
    private static void copyFileToRunFolder(String fileName) {
        System.out.println("Copying file \"" + fileName + "\" to run folder...");
        String[] cmdArray = {"cp", fileName, getRunFolderFull()};
        MainFromProperties.runCommand(cmdArray, false);
    }

    /**
     * Copy any desired file to the run folder under a new name.
     *
     * @param fileName    File name
     * @param newFileName New file name
     */
    public static void copyFileToRunFolder(String fileName, String newFileName) {
        System.out.println(
                "Copying file \"" + fileName + "\" to run folder using new file name \"" + newFileName + "\"...");
        String[] cmdArray = {"cp", fileName, getRunFolderFull() + "/" + newFileName};
        MainFromProperties.runCommand(cmdArray, false);
    }

    /**
     * Close log streams and throw away logs.
     */
    public static void closeAndThrowaway() {
        close();
        throwaway();
    }

    public static void logRemoteRouterState(int currentAllocatedPatsh, int flowFailuresSample, long flowCounter2)
            throws IOException {
        writerRemoteRouterStateLogCSV.write(
                currentAllocatedPatsh + "," +
                        flowFailuresSample + "," +
                        flowCounter2 + "\n");
    }

    public static void logRemoteRouterDropStatistics(long flowId, int source, int dest, int currentAllocPaths)
            throws IOException {
        writerRemoteRouterDropStatisticsCSV.write(
                flowId + "," +
                        source + "," +
                        dest + "," +
                        currentAllocPaths + "\n");
    }

    public static void dumpState(String dumpFolderName) throws IOException {
    }

    public static void logCommon(NBProperties runConfiguration) throws IOException {
        if (commonDropStatisticsWriter != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            long drops = RemoteRoutingController.getInstance().getTotalDrops();
            long flows = RemoteRoutingController.getInstance().getTotalFlows();
            String runFolderName = runConfiguration.getProperty("run_folder_name");
            commonDropStatisticsWriter.write(runFolderName + "," + drops + "," + flows + ","
                    + df.format(((double) drops / (drops + flows)) * 100) + "\n");
        }
        // commonDropStatisticsWriter.write(cbuf);
    }

    public static boolean isFlowOnCircuit(long flowId) {
        return flowsOnCircuit.containsKey(flowId);
    }

    public static void registerFlowOnCircuit(long flowId) {
        if (!flowsOnCircuit.containsKey(flowId)) {
            flowsOnCircuit.put(flowId, Simulator.getCurrentTime());
        }
    }

    public static long getStatistic(String key) {
        return statisticCounters.getOrDefault(key, 0L);
    }

    public static void regiserPathActive(Path p, boolean adding) {
        if (adding) {
            activePathMap.put(p.getId(), p);
            oldestActivePaths.push(p.getId());
            if (oldestActivePaths.size() > 10) {
                oldestActivePaths.pop();
            }
        } else {
            activePathMap.remove(p.getId());
            oldestActivePaths.remove(p.getId());
        }
    }

    public static void printOldestPaths() {
        for (Long id : oldestActivePaths) {
            System.out.println(activePathMap.get(id).toString() + "\n");
        }
    }

    public static void printOldestDistProtocolStates() {
        for (Long id : oldestDistProtocolStates) {
            System.out.println(id.toString() + " = " + activeDistProtocolStates.get(id).getState() + " size byte:"
                    + activeDistProtocolStates.get(id).getSizeByte() +
                    " flow num " + activeDistProtocolStates.get(id).getFlows().size() +
                    "\n");
        }
    }

    public static void distProtocolStateChange(long jFlowId, JumboFlow jFlow) {
        if (jFlow.getState().equals("NO_CIRCUIT")) {
            oldestDistProtocolStates.remove(jFlowId);
            activeDistProtocolStates.remove(jFlowId);
        } else {
            // first remove the old occurrence
            oldestDistProtocolStates.remove(jFlowId);
            // then put the new
            oldestDistProtocolStates.push(jFlowId);
            activeDistProtocolStates.put(jFlowId, jFlow);
            if (oldestDistProtocolStates.size() > 10) {
                oldestDistProtocolStates.pop();
            }
        }
    }

    public static void registerFlowCircuitRequest(long flowId) {
        Set<Long> requests = flowsRequestCircuit.getOrDefault(flowId, new HashSet<>());
        requests.add(Simulator.getCurrentTime());
        flowsRequestCircuit.put(flowId, requests);
    }

    public static void registerMetric(Metric metric) {
        sMetrics.add(metric);
    }

    public static void registerCommodities(String fileName, HashSet<Pair<Integer, Integer>> commodities) {

        BufferedWriter commoditiesWriter = openWriter("commodities/" + fileName);
        try {
            for (Pair commodity : commodities) {
                commoditiesWriter.write(commodity.getLeft() + " " + commodity.getRight() + "\n");
            }
            commoditiesWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void initSubFolder(String folderName) {
        File dir = new File(getRunFolderFull() + "/" + folderName);
        dir.mkdir();
    }

    public static void initCommoditiesFolder() {
        initSubFolder("commodities");
    }

    public static Map<Long, FlowLogger> getFlowLoggerByFlowId() {
        return flowLoggerByFlowId;
    }

    public static Map<Long, FlowLogger> getActiveFlowLoggerByFlowId() {
        return activeFlowLoggerByFlowId;
    }

    private static void deleteNonimportantFiles() {
        String[] fileNames = {
                "active_paths.log",
                "congestion_window.csv",
                "ecn_statistics.log",
                "flow_circuit_entrance_times.log",
                "flow_circuit_request_log.log",
                "flow_throughput.csv",
                "flows_on_circuit.log",
                "metrics.log",
                "packet_burst_gap.csv",
                "packets_dispatched.log",
                "port_queue_length.csv",
                "port_utilization.csv",
                "port_utilization.log",
                "remote_router_drop_statistics.csv",
                "remote_router_path.log",
                "remote_router_state.csv",
        };
        for (String fileName : fileNames) {
            File file = new File(getRunFolderFull() + "/" + fileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
