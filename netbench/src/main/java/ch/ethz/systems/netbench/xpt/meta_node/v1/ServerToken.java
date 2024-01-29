package ch.ethz.systems.netbench.xpt.meta_node.v1;

public class ServerToken {
    public final long flowId;
    public final long bytes;
    public final int sourceId;
    public final int destinationId;
    private boolean expired;
    public final long expiryTime;
    private long sentBytes;
    private long receivedBytes;
    private MetaNodeToken metaNodeToken;
    public ServerToken(long flowId, long bytes, int sourceId, int destinationId, long expiryTime) {
        this.flowId = flowId;
        this.bytes = bytes;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        expired = false;
        this.expiryTime = expiryTime;
        sentBytes = 0;
        receivedBytes = 0;

    }

    public void invalidate(){
        expired = true;
    }

    public void onSend(long bytes){
        sentBytes+=bytes;

        if(sentBytes>=this.bytes){
            MNController.getInstance().releaseServerTokenOutgoing(this);

        }
    }

    public void onReceive(long bytes){
        receivedBytes+=bytes;
        if(receivedBytes>=this.bytes){
            MNController.getInstance().releaseServerTokenIncomming(this);
        }
    }

    public boolean isExpired(){
        return expired;
    }

    public static ServerToken dummyToken(){
        return new ServerToken(-1,-1,-1,-1, -1);
    }

    public void setMetaNodeToken(MetaNodeToken token){
        this.metaNodeToken = token;
    }

    public MetaNodeToken getMetaNodeToken(){
        return metaNodeToken;
    }
}
