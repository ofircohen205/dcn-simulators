package ch.ethz.systems.netbench.xpt.xpander;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.semi.SemiRemoteRoutingSwitch;
import ch.ethz.systems.netbench.xpt.sourcerouting.exceptions.NoPathException;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;

import java.util.List;
import java.util.Map;

public class SemiXpander extends XpanderRouter {
    public SemiXpander(Map<Integer, NetworkDevice> idToNetworkDevice, NBProperties configuration) {
        super(idToNetworkDevice, configuration);
    }

    /**
     * generates a paths from a pre-existing set of paths.
     * @param source
     * @param dest
     * @return
     */
    @Override
    protected Path generatePathFromGraph(int source, int dest) {
        if(source==dest && trivialPathAllowed()){
            return allocateTrivialPath(source,dest);
        }
        List<List<Integer>> paths = getPathsFromDevice(source,dest);
        Path ret;
        for(List<Integer> p : paths){
            try{
                ret = checkPath(p);
            }catch(NoPathException e){
                continue;
            }
            return ret;
        }
        return new Path(0);
    }

    protected List<List<Integer>> getPathsFromDevice(int source, int dest) {
        SemiRemoteRoutingSwitch sourceSwitch = (SemiRemoteRoutingSwitch) mIdToNetworkDevice.get(source);
        return sourceSwitch.getPathsTo(dest);
    }


    /**
     * checks paths availability on all colors
     * @param p
     * @return
     */
    private Path checkPath(List<Integer> p) {
        Path ret;
        for(int c = 0;c<mGraphs.length; c++){
            try{
                ret = checkPathInGraph(p,c);
            }catch(NoPathException e){
                continue;
            }
            return ret;
        }
        throw new NoPathException();
    }

    /**
     * checks paths availability on a specific path
     * @param p
     * @param color
     * @return
     */
    protected Path checkPathInGraph(List<Integer> p, int color) {
        int curr = p.get(0);
        Path ret = new Path(0,color);
        ret.add(curr);
        for(int i = 0;i < p.size()-1; i++){
            if(mGraphs[color].getEdgeCapacity(new Vertex(curr),new Vertex(p.get(i+1)))==0){
                throw new NoPathException();
            }
            curr = p.get(i+1);
            ret.addVertex(new Vertex(curr),1);
        }
        return ret;
    }
}
