package ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed;

import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.ext.poissontraffic.RandomCollection;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.megaswitch.server_optic.distributed.metrics.Evaluation;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * a reservation packet is used to assign circuits online.
 */
public class ReservationPacket extends TcpPacket {
    private int mServerDest; // server destination
    private int mServerSource; // server origin
    int ToRdest; // the ToR on the destination
    long flowid;
    List<Integer> mPath; // the circuit path to try and assign
    private long delayed; // delayed incurred typically from reconfiguration events
    private int mSourceToR; // the ToR on the source
    private boolean mSuccess; // if the reservation succeeded to assign a full circuit
    private boolean mFailure; // if the reservation failed somewhere
    private boolean mDealocation; // marked to tell the switches to deallocate resources
    private boolean reversed; // has the path been reversed
    private long mId = -1; // an id for the reservation, typically the path id
    private boolean mFinishedDealloc; // is the deallocation finished
    private LinkedList<Evaluation> mEvaluations;

    public ReservationPacket(TcpPacket packet, int sourceToR,int dest, List<Integer> path, int color,boolean allocationReques) {
        super(
                packet
        );
        ToRdest = dest;
        this.mSourceToR = sourceToR;
        this.mPath = path;
        this.mColor = color;
        this.mDealocation = !allocationReques;
        this.mServerDest = packet.getDestinationId();
        mServerSource = packet.getSourceId();
        mSuccess = false;
        mFailure = false;
        flowid = packet.getFlowId();
        reversed = false;
        mFinishedDealloc = false;
        delayed = 0;
        mEvaluations = new LinkedList();
    }

    @Override
    public Encapsulatable encapsulate(int newSource, int newDestination) {
        return null;
    }

    @Override
    public Encapsulatable deEncapsualte() {
        return null;
    }

    @Override
    public int getDestinationId() {
        return ToRdest;
    }

    public boolean isSuccess() {
        return mSuccess;
    }


    /**
     * get the next hop of the path based on the curr node
     * @param curr
     * @return
     */
    public int getNextHop(int curr) {
        int currIndex = mPath.indexOf(curr);
        if(currIndex + 1 == mPath.size()){
            return getServerDest();
        }
        return mPath.get(currIndex+1);
    }

    public int getServerDest(int ToR) {
        if(ToR==mSourceToR){
            return mServerSource;
        }
        else{
            return mServerDest;
        }
    }

    @Override
    public long getFlowId(){
        return flowid;
    }

    /**
     * if reversed return server source else return server dest
     * @return
     */
    public int getServerDest() {
        return reversed ? mServerSource : mServerDest;
    }

    public int getOriginalServerDest() {
        return mServerDest;
    }

    public void markSuccess() {
        mSuccess = true;
    }

    public void markFailure() {
        mFailure = true;
    }

    /**
     * reverses the path
     */
    public void reverse() {

        Collections.reverse(mPath);

        reversed = !reversed;
    }

    public boolean idDeAllocation() {
        return mFailure || mDealocation;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setDeallocation() {
        this.mDealocation = true;
    }

    @Override
    public long getDataSizeByte() {
        return 120;
    }
    
    @Override
    public long getSizeBit() {
        return 960;
    }

    @Override
    public String toString(){
        return "Reservation packet " + " server dest " + mServerDest + " serverSource " + mServerSource + " isSuccess " + isSuccess() +
                " isFailure " + mFailure + " isDeallocation " + mDealocation +" color " + getColor() + " reversed " + reversed
                + "path id " + getId() + " path " + mPath.toString();
    }

    public int getSourceToR() {
        return mSourceToR;
    }

    public boolean isFailure() {
        return mFailure;
    }

    public List<Integer> getPath(){
        return mPath;
    }

    public void setId(long id){
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public void onFinishDeallocation() {
        mFinishedDealloc = true;
    }

    public boolean finishedDealloc() {
        return mFinishedDealloc;
    }

    public void markDelayed(long time) {
        delayed += time;
    }

    public long getDelay(){
        return delayed;
    }

    public void addEvaluation(Evaluation evaluation) {
        mEvaluations.add(evaluation);
    }

    public LinkedList<Evaluation> getEvaluations(){
        return mEvaluations;
    }
}
