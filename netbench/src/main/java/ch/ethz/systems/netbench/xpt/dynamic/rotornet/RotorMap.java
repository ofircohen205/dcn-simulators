package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import java.lang.Integer;
import java.util.*;

/**
 * each rotor switch has an instance of this map to tell it which other devices it is currently connected to.
 */
public class RotorMap extends LinkedList<Integer> {
    LinkGenerator mLinkGeneraor; // generates new links on demand
    RotorSwitch  mCurrentDevice; // the current source device for this map
    static int sNumOfNodes;
    protected RotorOutputPortGenerator mOutputPortGenerator; // generates output ports on demand
    HashMap<Integer, RotorOutputPort> mOutputPortMap; // a map from ids to target output ports
    RotorSwitch mOriginalDevice; // the orginal device which this map started with
    static Random mRnd;
    public RotorMap(RotorOutputPortGenerator rotorOutputPortGenerator, LinkGenerator linkGenerator, RotorSwitch rotorSwitch){
        mOutputPortGenerator = rotorOutputPortGenerator;
        mLinkGeneraor = linkGenerator;
        mOutputPortMap = new HashMap<>();
        mOriginalDevice = rotorSwitch;
    }

    static void setRandom(Random rnd){
        mRnd = rnd;
    }

    public void setCurrentDevice(RotorSwitch rs){
        mCurrentDevice = rs;

    }

    /**
     * gets the ouput port to dest if it is in the map, else throw exception
     * @param dest
     * @return
     */
    public RotorOutputPort getOutpurPort(int dest){
        if(!this.contains(dest)){
            throw new NoPathException();
        }
//        dest = maybeAddOne(dest);
        RotorOutputPort port = mOutputPortMap.get(dest);
        if(port == null) { // create the port
            RotorNetController controller = (RotorNetController) getController();
            RotorSwitch rs = controller.getDevice(dest);
            port = (RotorOutputPort) mOutputPortGenerator.generate(mCurrentDevice, rs, mLinkGeneraor.generate(mCurrentDevice, rs));
            port.setRotorMap(this);
            mOutputPortMap.put(dest,port);
        }
        return port;
    }

    // allow an extra connection if the source device is in the rotor map
    private int maybeAddOne(int dest){
        if(super.contains(mCurrentDevice.getIdentifier())){

            if(dest == ((mCurrentDevice.getIdentifier()))){
                return (dest + 1) % sNumOfNodes;
            }
        }
        return dest;
    }

    /**
     * if the map contains its own device's id then allow it to contain the next id
     * @param var1
     * @return
     */
    @Override
    public boolean contains(Object var1) {
//        int dest = (Integer) var1;
//        if(super.contains(mCurrentDevice.getIdentifier())){
//
//            if(dest == ((mCurrentDevice.getIdentifier()+1) % sNumOfNodes)) return true;
//        }
        return super.contains(var1);
    }

    public RotorOutputPort getRandomOutputPort(){
        return (RotorOutputPort) this.toArray()[mRnd.nextInt(this.size())];
    }


    void put(int dest){
        this.add(dest);
    }


    public RotorSwitch getOriginalDevice() {
        return mOriginalDevice;
    }

    public void clearOutputPorts() {
        mOutputPortMap.clear();
    }

    protected RemoteRoutingController getController(){
        return RemoteRoutingController.getInstance();
    }


    public void printPortOccupancy() {
        System.out.println("rotor map occupancy for " + this.mCurrentDevice.getIdentifier());
        for(int dest: this){
            System.out.println("to " + dest + " = " + getOutpurPort(dest).mPacketSentCounter);
        }
    }
}
