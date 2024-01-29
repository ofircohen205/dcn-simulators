package ch.ethz.systems.netbench.ext.bare;

import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;

public class BarePacket extends TcpPacket {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8622089801582883629L;

	BarePacket(long flowId, long dataSizeByte, int sourceId, int destinationId, long sequenceNumber, long acknowledgementNumber, boolean ECE, boolean ACK, double windowSize) {
        super(
                flowId,
                dataSizeByte,
                sourceId,
                destinationId,
                0,
                0,
                0,
                sequenceNumber,
                acknowledgementNumber,
                false,
                false,
                ECE,
                false,
                ACK,
                false,
                false,
                false,
                false,
                windowSize
        );
    }

	public BarePacket(BarePacket barePacket) {
		super(barePacket);
	}

	@Override
	public Encapsulatable encapsulate(final int newSource,final int newDestination) {
		return new BarePacket(this) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			@Override
			public int getDestinationId() {

				return newDestination;
			}
			@Override
			public int getSourceId() {

				return newSource;
			}
			
		};
	}

	@Override
	public Encapsulatable deEncapsualte() {
		// TODO Auto-generated method stub
		return new BarePacket(this);
	}



}
