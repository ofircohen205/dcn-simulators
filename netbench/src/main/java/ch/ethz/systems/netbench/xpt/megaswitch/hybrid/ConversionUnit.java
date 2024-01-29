package ch.ethz.systems.netbench.xpt.megaswitch.hybrid;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.utility.Constants;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * mimics the E2O unit
 * In the future it may include some logic i.e.
 * number of flows allowed through it.
 *
 * @author IK
 */
public class ConversionUnit {
    NetworkDevice mOptic;
    NBProperties mConf;
    NetworkDevice mOwnDevice;
    protected long mLinkBandwidth;
    protected long mEcnThreshold;
    protected long mMaxQueueSize;
    protected Map<Pair<Integer, Integer>, ConversionPort> mPortMap;

    public ConversionUnit(NBProperties conf, NetworkDevice ownDevice, NetworkDevice opticDevice) {
        mOwnDevice = ownDevice;
        mConf = conf;
        mOptic = opticDevice;
        mPortMap = new HashMap<>();
        initPortParams();


    }

    /**
     * gets the paramets from the target optic device
     */
    protected void initPortParams() {
        long bw = mOptic.getConfiguration().getLongPropertyOrFail(Constants.Link.BANDWIDTH_BIT_PER_NS);
        mLinkBandwidth = mConf.getLongPropertyWithDefault(Constants.Link.CONVERSION_BANDWIDTH_BIT_PER_NS, bw);
        mEcnThreshold = mOptic.getConfiguration().getLongPropertyOrFail(Constants.OutputPortGenerators.ECN_THRESHOLD_K_BYTES);
        mMaxQueueSize = mOptic.getConfiguration().getLongPropertyOrFail(Constants.OutputPortGenerators.MAX_QUEUE_SIZE_BYTES);
    }

    /**
     * enqueues the packt on a port, or if none exists create one.
     *
     * @param src
     * @param dst
     * @param packet
     */
    public void enqueue(int src, int dst, Packet packet) {
        ConversionPort port = mPortMap.get(new ImmutablePair<>(src, dst));
        if (port == null) {
            port = new ConversionPort(mOwnDevice, mOptic, new Link() {
                @Override
                public long getDelayNs() {
                    return 0;
                }

                @Override
                public long getBandwidthBitPerNs() {
                    return mLinkBandwidth;
                }

                @Override
                public boolean doesNextTransmissionFail(long packetSizeBits) {
                    return false;
                }
            }, mMaxQueueSize, mEcnThreshold);
            mPortMap.put(new ImmutablePair<>(src, dst), port);
        }
        port.enqueue(packet);

    }

    /**
     * remove the src-dst pair output port
     *
     * @param src
     * @param dst
     * @param jFlowId
     */
    public void onJumboFlowFinish(int src, int dst, long jFlowId) {
        if (mPortMap.get(new ImmutablePair<>(src, dst)) != null)
            mPortMap.get(new ImmutablePair<>(src, dst)).onJumboFlowFinished(jFlowId);
        mPortMap.remove(new ImmutablePair<>(src, dst));
    }

    public NetworkDevice getOptic() {
        return mOptic;
    }


    public void onFlowFinish(int src, int dst, long flowId) {
        if (mPortMap.get(new ImmutablePair<>(src, dst)) != null)
            mPortMap.get(new ImmutablePair<>(src, dst)).onFlowFinished(flowId);
    }
}
