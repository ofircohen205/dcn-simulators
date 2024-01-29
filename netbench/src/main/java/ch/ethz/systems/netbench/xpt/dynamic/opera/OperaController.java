package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.InputPort;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class OperaController extends RoutingPopulator {
    protected Map<Integer, NetworkDevice> idToNetworkDevice;
    protected int ToRNum;
    protected int rotorMatchings;
    protected static OperaController sInstance;
    protected ArrayList<OperaRotorSwitch> rotors;
    protected ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImmutablePair<Integer, Integer>>>>>> routingTables;
    protected ArrayList<ArrayList<ArrayList<ArrayList<ImmutablePair<Integer, Integer>>>>> currTable;
    protected int currCycle;
    protected int rotorParallelConfiguration;
    protected long reconfigurationInterval;
    protected long rotorGuardTime;
    protected LinkGenerator linkGenerator;
    protected long reconfigurationExecutionTime;
    protected Map<ImmutablePair<Integer, Integer>, OperaOutputPort> outputPortMap;
    protected Map<ImmutablePair<Integer, Integer>, InputPort> inputPortMap;
    protected int nextRotorToConfigure;
    private long nextReconfigurationTime;
    private Random rand;

    protected OperaController(NBProperties configuration, Map<Integer, NetworkDevice> idToNetworkDevice) {
        super(configuration);
        this.idToNetworkDevice = idToNetworkDevice;
        rotorParallelConfiguration = configuration.getIntegerPropertyWithDefault(Constants.Opera.PARALLEL_ROTORS_TO_CONFIG, 1);
        reconfigurationInterval = configuration.getLongPropertyOrFail(Constants.Opera.RECONFIGURATION_TIME_NS);
        reconfigurationExecutionTime = configuration.getLongPropertyWithDefault(Constants.Opera.RECONFIGURATION_EXECUTION_TIME, 0L);
        //rotorGuardTime = configuration.getLongPropertyOrFail("opera_rotor_guard_time");
        routingTables = new ArrayList<>();
        rotors = new ArrayList<>();
        outputPortMap = new HashMap<>();
        inputPortMap = new HashMap<>();
        ToRNum = configuration.getGraphDetails().getNumTors();
        String rotorsDir = configuration.getPropertyOrFail(Constants.Opera.ROTORS_DIR_PATH);
        loadRotorsFromDir(rotorsDir);
        assert (ToRNum % rotors.size() == 0);
        rotorMatchings = ToRNum / rotors.size();
        currCycle = 0;
        nextRotorToConfigure = 0;
        linkGenerator = new PerfectSimpleLinkGenerator(configuration);
        registerReconfigurationEvent();
        rand = Simulator.selectIndependentRandom("opera_controller");
    }

    private void registerReconfigurationEvent() {
        OperaReconfigurationEvent ore = new OperaReconfigurationEvent(reconfigurationInterval);
        nextReconfigurationTime = ore.getTime();
        Simulator.registerEvent(ore);
    }

    public static void init(NBProperties conf, Map<Integer, NetworkDevice> idToNetworkDevice) {
        sInstance = new OperaController(conf, idToNetworkDevice);
    }

    public static OperaController getInstance() {
        return sInstance;
    }

    public OperaRotorSwitch getRotor(int id) {
        return rotors.get(id);
    }


    private void loadRotorsFromDir(String rotorsDir) {
        int rotorsId = 0;
        File dir = new File(rotorsDir);
        while (true) {
            try {
                File rotorFile = new File(rotorsDir + "/rotor_" + rotorsId);
                loadRotorFromFile(rotorFile, rotorsId);
            } catch (FileNotFoundException e) {
                break;
            }
            rotorsId++;
        }


    }

    private void loadRotorFromFile(File matchingFile, int rotorNum) throws FileNotFoundException {
        OperaRotorSwitch oprs = new OperaRotorSwitch(rotorNum);
        oprs.loadRotorFromFile(matchingFile);
        rotors.add(oprs);

    }

    @Override
    public void populateRoutingTables() {
        String routinTablesDirPath = configuration.getPropertyOrFail(Constants.Opera.ROUTING_TABLES_DIR_PATH);
        int N = configuration.getGraphDetails().getNumTors();
        for (int i = 0; i < N; i++) {
            File cycleDir = new File(routinTablesDirPath + "/cycle_" + i);
            try {
                routingTables.add(i, loadRoutingTablesFromCycleDir(cycleDir, N));
            } catch (FileNotFoundException e) {
                if (i != N - 1) throw new RuntimeException();

                throw new RuntimeException(e);
            }
            System.out.println(i + " cycle loaded");
            System.out.println("Free memory (bytes): " +
                    Runtime.getRuntime().freeMemory());
        }
        currTable = routingTables.get(0);


    }

    private ArrayList<ArrayList<ArrayList<ArrayList<ImmutablePair<Integer, Integer>>>>> loadRoutingTablesFromCycleDir(File cycleDir, int N) throws FileNotFoundException {
        ArrayList<ArrayList<ArrayList<ArrayList<ImmutablePair<Integer, Integer>>>>> cycleRoutingTable = new ArrayList<>();
        for (int j = 0; j < ToRNum; j++) {
            File nodeFile = new File(cycleDir.getAbsolutePath() + "/n" + j);
            Scanner reader = new Scanner(nodeFile);
            int currNode = Integer.parseInt(nodeFile.getName().substring(1));
            ArrayList<ArrayList<ArrayList<ImmutablePair<Integer, Integer>>>> nodeTable = new ArrayList<>();
            for (int i = 0; i < N; i++) {
                if (i == currNode) {
                    nodeTable.add(currNode, new ArrayList<>());
                    continue;
                }
                String[] possiblitesLine = reader.nextLine().split(":");
                int destNode = Integer.parseInt(possiblitesLine[0]);
                ArrayList<ArrayList<ImmutablePair<Integer, Integer>>> table = parseNextHopPossibilities(possiblitesLine[1]);
                nodeTable.add(destNode, table);
            }
            cycleRoutingTable.add(currNode, nodeTable);
            reader.close();
        }
        return cycleRoutingTable;
    }

    private ArrayList<ArrayList<ImmutablePair<Integer, Integer>>> parseNextHopPossibilities(String possiblitesLine) {
        ArrayList<ArrayList<ImmutablePair<Integer, Integer>>> nodeRoutingTable = new ArrayList<>();
        String[] paths = possiblitesLine.split(",");
        for (String path : paths) {
            path.trim();
            if (path.equals("")) continue;

            String[] nodePairArr = path.split(" ");
            ArrayList<ImmutablePair<Integer, Integer>> pathAsArrayList = new ArrayList<>();
            for (String nodePair : nodePairArr) {
                String[] rotorDestPair = nodePair.split("-");
                //divide to pair, first is rotor second is nextHop
                pathAsArrayList.add(new ImmutablePair<>(Integer.parseInt(rotorDestPair[0]), Integer.parseInt(rotorDestPair[1])));
            }
            nodeRoutingTable.add(pathAsArrayList);

        }


        return nodeRoutingTable;
    }


    public void reconfigure() {
        for (OperaRotorSwitch ors : getNextRotors()) {
            ors.reconfigure();
        }
        registerReconfigurationEndEvent();
    }

    private void registerReconfigurationEndEvent() {
        OperaReconfigurationEndEvent oree = new OperaReconfigurationEndEvent(reconfigurationExecutionTime);
        Simulator.registerEvent(oree);
    }

    public void onReconfigurationEnd() {
        outputPortMap.clear();
        inputPortMap.clear();
        ArrayList<OperaRotorSwitch> nextReconfiguringRotors = getNextRotors();

        for (OperaRotorSwitch ors : nextReconfiguringRotors) {
            ors.start();
        }
        currCycle = (currCycle + nextReconfiguringRotors.size()) % (ToRNum);
        currTable = routingTables.get(currCycle);

        registerReconfigurationEvent();
        for (int id : configuration.getGraphDetails().getTorNodeIds()) {
            OperaSwitch sw = (OperaSwitch) idToNetworkDevice.get(id);
            sw.sendPending();
        }

    }


    public OperaOutputPort getOutpuPort(int source, int dest) throws OperaNoPathException {
        ImmutablePair<Integer, Integer> directHop = hasDirectConnection(source, dest);
        if (directHop == null) {
            throw new OperaNoPathException();
        }
        return getOutpuPort(source, dest, directHop);
    }

    public OperaOutputPort getOutpuPort(int source, int dest, ImmutablePair<Integer, Integer> rotorDevicePair) throws OperaNoPathException {


        ImmutablePair<Integer, Integer> pairKey = new ImmutablePair(source, dest);
        OperaOutputPort port = outputPortMap.get(pairKey);
        if (port == null) {
            OperaRotorSwitch rotor = rotors.get(rotorDevicePair.getLeft());
            NetworkDevice sourceDevice = idToNetworkDevice.get(source);
            NetworkDevice destDevice = idToNetworkDevice.get(dest);
            port = new OperaOutputPort(sourceDevice, destDevice,
                    linkGenerator.generate(sourceDevice, destDevice),
                    new LinkedList<>(), rotor);

            outputPortMap.put(pairKey, port);
        }
        return port;
    }

    public ArrayList<ArrayList<ImmutablePair<Integer, Integer>>> getPossiblities(int source, int dest) {

        return currTable.get(source).get(dest);
    }

    public int getCurrCycle() {
        return currCycle;
    }

    public long getReconfigurationInterval() {
        return reconfigurationInterval;
    }

    public ArrayList<OperaRotorSwitch> getNextRotors() {
        ArrayList<OperaRotorSwitch> nextReconfiguringRotors = new ArrayList<>();
        int firstRotor = nextRotorToConfigure;
        int rotorsInParallel = rotorParallelConfiguration;
        for (int i = 0; i < rotorsInParallel; i++) {
            if (currCycle + i == ToRNum) {
                return nextReconfiguringRotors;
            }
            int rotorIndex = (currCycle + i) % rotors.size();
            nextReconfiguringRotors.add(rotors.get(rotorIndex));

        }

        return nextReconfiguringRotors;
    }

//    public boolean hasPacketPath(int source, int dest, ArrayList<ImmutablePair<Integer,Integer>> path) {
//        ArrayList<OperaRotorSwitch> nextReconfiguringRotors = getNextRotors();
//        while(true){
//            if(source==dest){
//                break;
//            }
//
//
//
//        }
//
//        return true;
//    }

    public long getRotorGuardTime() {
        return rotorGuardTime;
    }

    public InputPort getInputPort(int dest, int source) {
        ImmutablePair<Integer, Integer> p = new ImmutablePair<>(dest, source);
        if (inputPortMap.get(p) == null) {
            NetworkDevice sourceDevice = idToNetworkDevice.get(source);
            NetworkDevice destDevice = idToNetworkDevice.get(dest);
            InputPort inputPort = new InputPort(destDevice, sourceDevice, linkGenerator.generate(sourceDevice, destDevice));
            inputPortMap.put(p, inputPort);
            return inputPort;
        }

        return inputPortMap.get(p);
    }

    public long getRemainingTimeToReconfigure() {
        return nextReconfigurationTime - Simulator.getCurrentTime();
    }

    public long getNextReconfigurationTIme() {
        return nextReconfigurationTime;
    }

    public ArrayList<ImmutablePair<Integer, Integer>> getRandomPath(int source, int dest) throws OperaNoPathException {
        assert (source != dest);
        int pathNum = currTable.get(source).get(dest).size();
        int pathIndex = Math.abs(rand.nextInt()) % pathNum;

        for (int i = 0; i < pathNum; i++) {
            ArrayList<ImmutablePair<Integer, Integer>> path = currTable.get(source).get(dest).get((i + pathIndex) % pathNum);
            if (verifyPath(path, source)) {
                return path;
            }
        }
        throw new OperaNoPathException();
    }

    private boolean verifyPath(ArrayList<ImmutablePair<Integer, Integer>> path, int source) {
        int curr = source;
        for (ImmutablePair<Integer, Integer> rotorDevicePair : path) {
            OperaRotorSwitch rotor = rotors.get(rotorDevicePair.getLeft());
            assert rotor.containsAt(curr, rotorDevicePair.getRight()) : "cycle " + currCycle + " path :" + path + " from " + source + "\n" + rotor + " doesnt connect " + curr + " to " + rotorDevicePair.getRight();
            curr = rotorDevicePair.getRight();
            if (rotor.isReconfiguring()) return false;
            // if(nextReconfiguringRotors.contains(rotors.get(rotor.getId()))) return false;
        }
        return true;
    }

    public ImmutablePair<Integer, Integer> hasDirectConnection(int source, int dest) {
        for (OperaRotorSwitch ors : rotors) {
            if (ors.containsAt(source, dest)) {
                return new ImmutablePair<>(ors.getId(), dest);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String matchings = "";
        for (OperaRotorSwitch ors : rotors) {
            matchings += ors.toString() + "\n";
        }

        return matchings;
    }


    public OutputPort getRandomOutputPort(int source, long sizeBit) throws OperaNoPathException {
        int next = Math.abs(rand.nextInt()) % rotors.size();
        int size = rotors.size();
        for (int i = 0; i < size; i++) {
            OperaRotorSwitch rotor = rotors.get((i + next) % size);
            if (rotor.isReconfiguring()) continue;
            int dest = rotor.getAt(source);
            if (dest == source) {
                continue;
            }
            OperaOutputPort port = getOutpuPort(source, dest);
            if (port.canSend(sizeBit)) {
                return port;
            }
        }
        throw new OperaNoPathException();
    }
}
