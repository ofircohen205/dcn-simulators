package ch.ethz.systems.netbench.xpt.sourcerouting;



import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;


public class SourceRoutingPath extends Path {
	SourceRoutingSwitch source;
	private static int classCounter = 0;
	private long id;
	public SourceRoutingPath(long id) {
		super(0);
		this.id = id;
		classCounter++;
		// TODO Auto-generated constructor stub
	}
    // No adaptations needed, it is only created for
    // semantic purpose.

	public SourceRoutingPath(Path p,SourceRoutingSwitch source, long id) {
		super(p.getVertexList(),0);
		this.source = source;
		this.id = id;
		classCounter++;
		// TODO Auto-generated constructor stub
	}
	

	public SourceRoutingPath() {
		super(0);
	}

	public SourceRoutingSwitch getSourceSwitch() {
		return source;
	}

	public long getIdentifier() {
		// TODO Auto-generated method stub
		return id;
	}
	

}
