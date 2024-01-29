package ch.ethz.systems.netbench.xpt.mega_switch;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import ch.ethz.systems.netbench.ext.basic.IpPacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.JumboFlow;
import ch.ethz.systems.netbench.xpt.megaswitch.hybrid.OpticElectronicHybrid;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.MockRemoteRouter;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;

public class MockFullHybrid extends OpticElectronicHybrid {
	public int maxPortsNum = 0;
	public boolean routedThroughCircuit;
	public boolean routedThroughPacketSwitch;
	public boolean recoveredPath;
	public boolean noPathExceptionThrown;
	static MockRemoteRouter router;
	public MockFullHybrid(int identifier, TransportLayer transportLayer, Intermediary intermediary,
			NBProperties configuration) {
		super(identifier, transportLayer, intermediary, configuration);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void routeThroughCircuit(IpPacket packet, JumboFlow jFlow) {
		routedThroughCircuit = true;
		try{
			super.routeThroughCircuit(packet, jFlow);
			int portsNum = ((MockConversionUnit) conversionUnit).getNumOfPorts();
			if(portsNum>maxPortsNum){
				maxPortsNum = portsNum;
			}
		}catch (NoPathException e){
			noPathExceptionThrown = true;
			throw e;
		}

	}
	
	@Override
	protected void routeThroughtPacketSwitch(TcpPacket packet) {
		routedThroughPacketSwitch = true;
		super.routeThroughtPacketSwitch(packet);
			
	}

	@Override
	protected void initConversionUnit(){
		conversionUnit = new MockConversionUnit(configuration,this,optic);
	}
	
	@Override
	protected void recoverPath(JumboFlow jFlow) {
		recoveredPath = true;
		super.recoverPath(jFlow);
	}
	
	@Override
	protected RemoteRoutingController getRemoteRouter() {
		return router;
	}

	public static void setRemoteRouter(MockRemoteRouter mockRemoteRouter) {
		router = mockRemoteRouter;
		
	}

	public void reset() {
        routedThroughCircuit = false;
        routedThroughPacketSwitch = false;
        recoveredPath = false;
        router.reset();
		
	}

}
