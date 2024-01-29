package ch.ethz.systems.netbench.xpt.meta_node.v1;

import ch.ethz.systems.netbench.core.Simulator;

public class MetaNodeToken {
    private int middleHop;
    int dest;
    long bytes;
    private long timeoutNs;
    private boolean expired;
    private long lastUsed;
    private final long originalBytesAllocated;
    private MetaNodeTokeTimeoutEvent timeoutEvent;
    private int source;
    public MetaNodeToken(long bytes, int source, int middleHop, int dest, long tokenTimeout) {
        this.dest = dest;
        this.source = source;
        this.middleHop = middleHop;
        this.bytes = bytes;
        this.originalBytesAllocated = bytes;
        this.timeoutNs = tokenTimeout;
        timeoutEvent = new MetaNodeTokeTimeoutEvent(timeoutNs,this);
        Simulator.registerEvent(timeoutEvent);
        lastUsed = Simulator.getCurrentTime();

    }

    public boolean expired() {
        return this.expired;
    }

    public void setExpired(){
        getController().releaseToken(this);
        this.expired = true;
    }

    public void nextBytes(long nextBytes){
        bytes -=nextBytes;
        lastUsed = Simulator.getCurrentTime();
        if(bytes<=0) setExpired();

    }

    public long getTimeout(){
        return timeoutNs;
    }

    public long lastUsed() {
        return lastUsed;
    }

    public boolean active() {
        return (Simulator.getCurrentTime() - lastUsed()) <= getTimeout();
    }

    public int getMiddleHop(){
        return middleHop;
    }

    public int getDest(){
        return this.dest;
    }

    public long getBytes(){
        return bytes;
    }

    public long getOriginalBytesAllocated(){
        return originalBytesAllocated;
    }

    @Override
    public String toString(){
        return "bytes: " + bytes + " middleHop: "+ middleHop + " MNDest: " + getMiddleHop() + " expired: " + expired + "\n";
    }

    protected MNController getController(){
        return MNController.getInstance();
    }

    public int getSource() {
        return this.source;
    }
}
