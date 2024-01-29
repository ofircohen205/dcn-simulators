package ch.ethz.systems.netbench.xpt.megaswitch;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.*;

import java.util.ArrayList;

/**
 * this class is the base class to all complex 2 devices and above switches
 */
public interface MegaSwitch{
    /**
     * adds a new device to the switch
     * @param networkDevice
     * @param conf
     */
    public void extend(NetworkDevice networkDevice, NBProperties conf);

    /**
     * receive a packet from an encapsulated device
     * @param packet
     */
    public void receiveFromEncapsulatedDevice(Packet packet);

    /**
     * deallocate resources when flow finished. probably should go in some sub-interface
     * @param sourceToRId
     * @param destToRId
     * @param sourceServer
     * @param destServerId
     * @param flowId
     */
    public void onFlowFinished(int sourceToRId, int destToRId,int sourceServer,int destServerId, long flowId);

    public NetworkDevice getAsNetworkDevice();

    /**
     * returns some encapsulated device based on type
     * @param type
     * @return
     */
    public NetworkDevice getEncapsulatedDevice(String type);

    /**
     * returns true if the encapsulated device does not need to handle some packet
     * @param packet
     * @return
     */
    boolean hadlePacketFromEncapsulating(Packet packet);
}
