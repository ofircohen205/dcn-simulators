package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.ConversionPort;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.ConversionUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class MockConversionUnit extends ConversionUnit {
    public MockConversionUnit(NBProperties conf, NetworkDevice ownDevice, NetworkDevice opticDevice) {

        super(conf, ownDevice, opticDevice);
    }

    @Override
    protected void initPortParams(){
        mLinkBandwidth = 100;
        mEcnThreshold = 30000;
        mMaxQueueSize = 150000;
    }

    public void enqueue(int src, int dst, Packet packet){
        ConversionPort port = mPortMap.get(new ImmutablePair<>(src,dst));
        if(port==null){
            System.out.println("issuing port to flow " + packet.getFlowId());
        }else{
//            System.out.println("using existing port to flow " + packet.getFlowId());
        }
        super.enqueue(src, dst, packet);
    }

    public int getNumOfPorts(){
        return mPortMap.size();
    }
}
