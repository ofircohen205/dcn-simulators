package ch.ethz.systems.netbench.xpt.meta_node;

import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

public class MockTcpPacket extends TcpPacket {
    public MockTcpPacket() {
        super(0, 0, 0, 0, 50, 0, 0, 0, 0, false,
                false, false, false, false, false, false, false, false, 0);
    }

    @Override
    public Encapsulatable encapsulate(int newSource, int newDestination) {
        return null;
    }

    @Override
    public Encapsulatable deEncapsualte() {
        return null;
    }
}
