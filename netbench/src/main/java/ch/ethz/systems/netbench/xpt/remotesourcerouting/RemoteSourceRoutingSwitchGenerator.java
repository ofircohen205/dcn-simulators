package ch.ethz.systems.netbench.xpt.remotesourcerouting;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.IntermediaryGenerator;
import ch.ethz.systems.netbench.xpt.sourcerouting.SourceRoutingSwitchGenerator;

public class RemoteSourceRoutingSwitchGenerator extends
SourceRoutingSwitchGenerator {

	public RemoteSourceRoutingSwitchGenerator(
			IntermediaryGenerator intermediaryGenerator, int numNodes,NBProperties configuration) {
		super(intermediaryGenerator, numNodes,configuration);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void log(){
		SimulationLogger.logInfo("Network device", "SOURCE_ROUTING_SWITCH(numNodes=" + numNodes + ")");
	}

	public NetworkDevice generate(int identifier, TransportLayer transportLayer) {
		
		return new RemoteSourceRoutingSwitch(identifier, transportLayer, intermediaryGenerator.generate(identifier),configuration);


	}

}
