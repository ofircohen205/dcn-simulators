package ch.ethz.systems.netbench.xpt.dynamic.rotornet;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.device.DynamicSwitch;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * the rotor switch
 */
public class RotorSwitch extends DynamicSwitch {
    protected long mPortsBufferSize;
    protected RotorMap mRotorMap; // the map telling it which devices it is connected to
    protected long mPoolBufferSize; // the rotor buffer, if this is full dont allow forwarding
    private static long sMaxBufferSizeBit;
    private LinkedList<IpPacket> mBuffer;
    /**
     * Constructor of a network device.
     *
     * @param identifier     Network device identifier
     * @param transportLayer Transport layer instance (null, if only router and not a server)
     * @param intermediary   Flowlet intermediary instance (takes care of flowlet support)
     * @param configuration
     */
    protected RotorSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        mRotorMap = new RotorMap(new RotorOutputPortGenerator(configuration) , new PerfectSimpleLinkGenerator(configuration),this);
        mRotorMap.setCurrentDevice(this);
        mBuffer = new LinkedList<>();
        mPoolBufferSize = 0;
        mPortsBufferSize = 0;
        
    }

    @Override
    public void receive(Packet genericPacket) {

        IpPacket ipPacket = (IpPacket) genericPacket;
        boolean deadline = false;
        boolean nopath = false;

        if(ipPacket.getSourceId()==this.getIdentifier()){ // we are the first hop


            try{
                // first try direct forward
                forwardToNextSwitch(ipPacket,ipPacket.getDestinationId());
                SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_DIRECT_FORWARD");

                return;
            }catch (NoPathException e){
                nopath = true;
            }catch (ReconfigurationDeadlineException e){
                deadline = true;
            }
            try{
                // then try random forward
                sendToRandomDestination(ipPacket);
                return;
            }catch(ReconfigurationDeadlineException e){

            }
        }else { // we have already done one random hop
            try {
                forwardToNextSwitch(ipPacket, ipPacket.getDestinationId()); // try forward to destination switch
                SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_INDIRECT_FORWARD");
                return;
            } catch (ReconfigurationDeadlineException e) {
                deadline = true;
            } catch (NoPathException e) {
                nopath = true;
            }
        }

        if(hasPoolResources(genericPacket.getSizeBit())){ // if we have resources then re add packet to buffer
            String counter_name = "ROTOR_PACKET_BUFFERED";
            if(nopath) counter_name = "ROTOR_PACKET_BUFFERED_NO_PATH";
            if(deadline) counter_name = "ROTOR_PACKET_BUFFERED_DEADLINE";
            SimulationLogger.increaseStatisticCounter(counter_name);
            addToBuffer(genericPacket);
            return;
        }

        SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_LOST_AT_" +( (ipPacket.getSourceId()==this.getIdentifier()) ? "SOURCE" : "SECOND_HOP"));

    }

    private boolean hasPoolResources(long sizeBit) {
        return mPoolBufferSize + sizeBit <= sMaxBufferSizeBit;
    }

    private void addToBuffer(Packet genericPacket) {

        mBuffer.addLast((IpPacket) genericPacket);
        mPoolBufferSize += genericPacket.getSizeBit();
    }

    private Packet popFromBuffer(){
        Packet p = mBuffer.pop();
        mPoolBufferSize -= p.getSizeBit();
        return p;
    }

    int sendDirectPendingData(){
        List<IpPacket> directForward = mBuffer.stream()
                .filter(p -> hasDirectLink(p.getDestinationId())).collect(Collectors.toList());

        mBuffer.removeIf(p -> hasDirectLink(p.getDestinationId()));

        int size = directForward.size();
        for(int i = 0; i<size; i++){
            IpPacket p = directForward.get(i);
            mPoolBufferSize -= p.getSizeBit();
            try{
                forwardToNextSwitch(p, p.getDestinationId());
                if(p.getSourceId()==this.identifier)
                    SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_DIRECT_FORWARD_FROM_BUFFER");
                else
                    SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_INDIRECT_FORWARD_FROM_BUFFER");
            }catch (ReconfigurationDeadlineException e){
//                addToBuffer(p);
            }
        }

        return size;
    }

    // send all pending data
    void sendPendingData(){

        int size = mBuffer.size();
        for(int i = 0; i<size; i++){
            IpPacket ipPacket = (IpPacket) popFromBuffer();
            receive(ipPacket);
        }

    }



    /**
     * randomizes a destination and if it has buffer space send to it.
     * @param ipPacket
     */
    protected void sendToRandomDestination(IpPacket ipPacket) {
        Collections.shuffle(mRotorMap,mRotorMap.mRnd);
        for(int i = 0;i<mRotorMap.size();i++){
            RotorSwitch target = (RotorSwitch) mRotorMap.getOutpurPort(mRotorMap.get(i)).getTargetDevice();
            if(target.hasResources(ipPacket.getSizeBit())){
                try{
                    forwardToNextSwitch(ipPacket,target.getIdentifier());
                    SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_RANDOM_FORWARD");

                    return;
                }catch (ReconfigurationDeadlineException e){
                    SimulationLogger.increaseStatisticCounter("ROTOR_PACKET_ALL_DEADLINE");
                }
            }else{

            }
        }


        throw new ReconfigurationDeadlineException();
    }

    protected boolean hasResources(long sizeBit) {
        return  hasPoolResources(sizeBit);
    }

    static void setMaxBufferSizeByte(long sizeByte){
        sMaxBufferSizeBit = sizeByte*8L;
    }

    private void forwardToNextSwitch(IpPacket ipPacket,int destination) {
        if(mRotorMap.contains(destination)){
            RotorOutputPort port = mRotorMap.getOutpurPort(destination);
            port.enqueue(ipPacket);
            if(ipPacket.getSourceId()!=this.identifier){
            }
            mPortsBufferSize += ipPacket.getSizeBit();

            return;
        }

        throw new NoPathException();
    }

    @Override
    protected void receiveFromIntermediary(Packet genericPacket) {
        this.receive(genericPacket);

    }



    protected void addConnection(int dest) {
        mRotorMap.put(dest);
    }

    public RotorMap getRotorMap() {
        return mRotorMap;
    }

    /**
     * sets a new rotor map
     * @param rotortMap
     */
    public void setRotortMap(RotorMap rotortMap) {
//        rotortMap.printPortOccupancy();

        rotortMap.clearOutputPorts(); // first clear the new map ports.
        this.mRotorMap = rotortMap;
        mRotorMap.setCurrentDevice(this);
    }

    public boolean hasDirectLink(int destination) {
        return mRotorMap.contains(destination);
    }



    public boolean hasAvailableSecondHop(long sizeBit) {
        for(int i = 0;i<mRotorMap.size();i++){
            RotorSwitch target = (RotorSwitch) mRotorMap.getOutpurPort(mRotorMap.get(i)).getTargetDevice();
            if(target.hasResources(sizeBit)) return true;
        }
        return false;
    }

    public void resetBuffer() {
        mPortsBufferSize = 0;
    }
}
