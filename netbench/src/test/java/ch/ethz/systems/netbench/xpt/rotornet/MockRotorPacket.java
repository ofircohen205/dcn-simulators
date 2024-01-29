package ch.ethz.systems.netbench.xpt.rotornet;

import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import sun.awt.image.ImageWatched;

import java.util.LinkedList;

public class MockRotorPacket extends FullExtTcpPacket {
    LinkedList<Integer> path;
    public MockRotorPacket(long flowId, long dataSizeByte, int sourceId, int destinationId) {
        super(flowId, dataSizeByte, sourceId, destinationId, 100, 100, 100, 1000, 0,
                false, false, false, false, false, false, false, false, false, 500, 0);
        path = new LinkedList<>();
    }

    public MockRotorPacket(MockRotorPacket mockRotorPacket) {
        super(mockRotorPacket);
        path = mockRotorPacket.path;
    }

    public Encapsulatable encapsulate(final int newSource, final int newDestination) {
        return new MockRotorPacket(this) {
            private static final long serialVersionUID = 1L;

            public int getDestinationId() {
                return newDestination;
            }

            public int getSourceId() {
                return newSource;
            }
        };
    }

    public Encapsulatable deEncapsualte() {
        return new MockRotorPacket(this);
    }

    public void traversed(int identifier) {
        path.add(identifier);
    }
}
