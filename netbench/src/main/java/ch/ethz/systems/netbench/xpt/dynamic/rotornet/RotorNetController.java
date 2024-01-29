package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.xpt.dynamic.controller.DynamicController;

import java.util.ArrayList;
import java.util.Map;

public class RotorNetController extends DynamicController {
    public static long sNextReconfigurationTime = 0; // the time for the next reconfiguration event
    protected final long mReconfigurationInterval; // how much time between reconfigurations
    protected final long mReconfigurationTime; // the time it takes to do the reconfiguration
    int mNumCycles; // the total num of cycles to go through all permutations
    int mCurrCycle; //
    long randomHops, directHops, indirectHops;
    ArrayList<RotorMap> mRotorMaps;
    RotorSwitch[] mRotorsArray;

    public RotorNetController(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
        if (max_degree >= idToNetworkDevice.size() - 1) {
            throw new RuntimeException("max degree is bigger then network size minus 1");
        }
        if (idToNetworkDevice.size() % max_degree != 0) {
            throw new RuntimeException("max degree must be devisable by network size");
        }
        mReconfigurationTime = configuration.getLongPropertyOrFail(Constants.Rotor.NET_RECONFIGURATION_TIME_NS);
        mReconfigurationInterval = configuration.getLongPropertyOrFail(Constants.Rotor.NET_RECONFIGURATION_INTERVAL_NS);
        RotorSwitch.setMaxBufferSizeByte(configuration.getLongPropertyOrFail(Constants.Rotor.MAX_BUFFER_SIZE_BYTE));
        RotorMap.setRandom(Simulator.selectIndependentRandom(Constants.Rotor.RANDOM_PORT));
        mNumCycles = mIdToNetworkDevice.size() / max_degree;
        RotorMap.sNumOfNodes = idToNetworkDevice.size();
        mRotorMaps = new ArrayList<>();
        mCurrCycle = 0;
        mRotorsArray = (RotorSwitch[]) mIdToNetworkDevice.values().toArray(new RotorSwitch[mIdToNetworkDevice.size()]);
        setInitialConnections();
        registerReconfigurationEvent();
        randomHops = 0;
        directHops = 0;
        indirectHops = 0;

    }

    // the initial configuration
    private void setInitialConnections() {
        RotorSwitch[] devices = mRotorsArray;
        for (int j = 0; j < mIdToNetworkDevice.size(); j++) {
            for (int i = 0; i < max_degree; i += 1) {
                RotorSwitch device = (RotorSwitch) devices[j];
                device.addConnection(devices[(j + mNumCycles * i + 1) % mIdToNetworkDevice.size()].getIdentifier());
            }
            mRotorMaps.add(devices[j].getRotorMap());
        }
    }

    @Override
    public void initRoute(int transimttingSource, int receivingDest, int sourceKey, int destKey, long jumboFlowId, long sizeBit) {


    }

    @Override
    public void recoverPath(int source, int dest, int sourceServer, int destServer, long flowId) {


    }

    /**
     * configures all devices rotor maps
     */
    public void reconfigureRotorSwitches() {
        RotorSwitch[] devices = mRotorsArray;
        mCurrCycle++;
        if (mCurrCycle == mNumCycles) { // if we ran through all cycles
            resetAllMaps();
            mCurrCycle = 0;
            return;
        }

        /**
         *  move the rotor map one down:
         */
        RotorMap tempMap = devices[0].getRotorMap();
        for (int i = 0; i < devices.length - 1; i += 1) {
            RotorSwitch device = devices[i];
            RotorSwitch nextDevice = devices[(i + 1)];
            device.setRotortMap(nextDevice.getRotorMap());
        }
        devices[devices.length - 1].setRotortMap(tempMap);
    }

    /**
     * resets all mpa to their original devices.
     */
    private void resetAllMaps() {
        for (int i = 0; i < mRotorMaps.size(); i += 1) {
            RotorMap map = mRotorMaps.get(i);
            map.getOriginalDevice().setRotortMap(map);
        }
    }

    @Override
    public String toString() {
        String toShow = "";
        for (int i = 0; i < mRotorsArray.length; i++) {
            toShow += mRotorsArray[i].getIdentifier() + " " + mRotorsArray[i].getRotorMap().toString() + "\n";
        }
        return toShow;
    }

    public RotorSwitch getDevice(int dest) {
        RotorSwitch device = mRotorsArray[dest];
        assert (device.getIdentifier() == dest);
        return device;
    }

    /**
     * restart transmissions after a reconfiguration event
     */
    protected void startTransmmisions() {
        for (int i = 0; i < mRotorsArray.length; i++) {
            mRotorsArray[i].resetBuffer();

        }
        int direct = 0;
        for (int i = 0; i < mRotorsArray.length; i++) {

            direct += mRotorsArray[i].sendDirectPendingData();
        }

    }

    public String getCurrentState() {
        long random = SimulationLogger.getStatistic("ROTOR_PACKET_RANDOM_FORWARD") - randomHops;
        long direct = SimulationLogger.getStatistic("ROTOR_PACKET_DIRECT_FORWARD") - directHops;
        long indirect = SimulationLogger.getStatistic("ROTOR_PACKET_INDIRECT_FORWARD") - indirectHops;
        randomHops = SimulationLogger.getStatistic("ROTOR_PACKET_RANDOM_FORWARD");
        directHops = SimulationLogger.getStatistic("ROTOR_PACKET_DIRECT_FORWARD");
        indirectHops = SimulationLogger.getStatistic("ROTOR_PACKET_INDIRECT_FORWARD");
//        for(RotorMap rm: mRotorMaps){
//            rm.printPortOccupancy();
//        }
        return "direct hops " + SimulationLogger.getStatistic("ROTOR_PACKET_DIRECT_FORWARD") + "\n" +
                " indirect hops " + SimulationLogger.getStatistic("ROTOR_PACKET_INDIRECT_FORWARD") + "\n" +
                " direct hops from buffer " + SimulationLogger.getStatistic("ROTOR_PACKET_DIRECT_FORWARD_FROM_BUFFER") + "\n" +
                " indirect hops from buffer " + SimulationLogger.getStatistic("ROTOR_PACKET_INDIRECT_FORWARD_FROM_BUFFER") + "\n" +
                " random hops " + SimulationLogger.getStatistic("ROTOR_PACKET_RANDOM_FORWARD") + "\n";

    }

    /**
     * registers the next reconfiguration event
     */
    protected void registerReconfigurationEvent() {
        Simulator.registerEvent(new RotorReconfigurationEvent(mReconfigurationInterval, mReconfigurationTime));
        sNextReconfigurationTime = Simulator.getCurrentTime() + mReconfigurationInterval;

    }
}
