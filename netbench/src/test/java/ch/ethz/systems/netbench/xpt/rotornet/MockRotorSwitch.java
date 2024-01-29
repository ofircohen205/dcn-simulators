package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.PerfectSimpleLinkGenerator;
import ch.ethz.systems.netbench.xpt.dynamic.rotornet.RotorSwitch;

public class MockRotorSwitch extends RotorSwitch {
    protected MockRotorSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        mRotorMap = new MockRotorMap(new MockRotorOutputPortGenerator(configuration) , new PerfectSimpleLinkGenerator(configuration),this);
        mRotorMap.setCurrentDevice(this);
    }

    @Override
    public void receive(Packet p){
        try{
            MockRotorPacket rp = (MockRotorPacket) p;
            rp.traversed(this.getIdentifier());
        }catch (ClassCastException e){
            //catching the exception that might be thrown from other testing for general packets
        }
        if(((IpPacket) p).getDestinationId()==this.identifier ){

            if(isServer()){
                getTransportLayer().receive(p);
                return;
            }else{
                return;
            }

        }
        super.receive(p);
    }

    protected void sendToRandomDestination(IpPacket ipPacket) {
        super.sendToRandomDestination(ipPacket);
    }

    protected boolean hasResources(Packet genericPacket) {
        return super.hasResources(genericPacket.getSizeBit());
    }

    public void setBufferSize(int bufferSize) {
        this.mPoolBufferSize = bufferSize;
    }

    public long getBufferSize() {
        return mPoolBufferSize;
    }
}
