package ch.ethz.systems.netbench.xpt.meta_node;

public abstract class MetaNodeToken {

    private long bytes;
    private int source;
    private  int middleHop;
    private  int dest;
    private  long tokenTimeout;

    public MetaNodeToken(long bytes, int source, int middleHop, int dest, long tokenTimeout) {
        this.bytes = bytes;
        this.source = source;
        this.middleHop = middleHop;
        this.dest = dest;
        this.tokenTimeout = tokenTimeout;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getMiddleHop() {
        return middleHop;
    }

    public void setMiddleHop(int middleHop) {
        this.middleHop = middleHop;
    }

    public int getDest() {
        return dest;
    }

    public void setDest(int dest) {
        this.dest = dest;
    }

    public long getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(long tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    protected abstract MockMNController getController();
}
