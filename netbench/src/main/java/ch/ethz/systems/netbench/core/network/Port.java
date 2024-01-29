package ch.ethz.systems.netbench.core.network;

public abstract class Port {
    protected NetworkDevice ownNetworkDevice; // Network device this port is attached to

    public String getTechnology() {
        return ownNetworkDevice.getTechnology();
    }


}
