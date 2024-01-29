package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.OpticElectronicHybrid;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;

public class MockOpticalHybrid extends OpticElectronicHybrid {
    private static RemoteRoutingController router;
    public boolean noPathExceptionThrown;

    public MockOpticalHybrid(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
    }

    @Override
    public void receive(Packet genericPacket) {
    	super.receive(genericPacket);
    }

    @Override
    protected void routeThroughCircuit(IpPacket packet, JumboFlow jFlow) {
        try{
            super.routeThroughCircuit(packet, jFlow);
        }catch (NoPathException e){
            noPathExceptionThrown = true;
            throw e;
        }

    }

    @Override
    public RemoteRoutingController getRemoteRouter() {
    	return router==null ? RemoteRoutingController.getInstance() : router;
    }
    
    public static void setRouter(RemoteRoutingController r) {
    	router = r;
    }
    
    protected void routeThroughtPacketSwitch(TcpPacket packet) {
		throw new NoPathException();
		
	}
    @Override
    protected boolean isCircuitable(JumboFlow jumboFlow, TcpPacket packet) {
        return true;
    }
    
}
