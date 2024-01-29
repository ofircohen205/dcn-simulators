package ch.ethz.systems.netbench.xpt.remotesourcerouting.semi;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.network.Intermediary;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.utility.Constants;
import ch.ethz.systems.netbench.xpt.remotesourcerouting.RemoteSourceRoutingSwitch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * this class holds a set of precomputed paths which it
 * will check with a centrelized controller for availability.
 * as opposed to requesting a new path all the time.
 */
public class SemiRemoteRoutingSwitch extends RemoteSourceRoutingSwitch {
    protected HashMap<Integer, List<List<Integer>>> mPathMap; // a map containing lists of paths based on destination id

    public SemiRemoteRoutingSwitch(int identifier, TransportLayer transportLayer, Intermediary intermediary, NBProperties configuration) {
        super(identifier, transportLayer, intermediary, configuration);
        mPathMap = readMap(configuration.getPropertyOrFail(Constants.RemoteRoutingPopulator.SEMI_REMOTE_ROUTING_PATH_DIR));
    }

    /**
     * reads a map of paths stored in path
     *
     * @param path the dir of all paths
     * @return
     */
    protected HashMap<Integer, List<List<Integer>>> readMap(String path) {
        HashMap<Integer, List<List<Integer>>> map = new HashMap<>();
        List<Integer>[] paths;
        try {
            paths = readListFromFile(path + "/" + this.identifier + "_obj");


        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException();
        }

        for (List<Integer> p : paths) {
            if (p == null) continue;
            map.computeIfAbsent(p.get(p.size() - 1), k -> new LinkedList<>());

            map.get(p.get(p.size() - 1)).add(removeCycles(p));
        }
        return map;
    }

    /**
     * this method will remove cycles for some path
     *
     * @param p
     * @return
     */
    private List<Integer> removeCycles(List<Integer> p) {
        HashMap<Integer, Boolean> visited = new HashMap<>();
        LinkedList<Integer> ret = new LinkedList<>();

        for (Integer integer : p) {
            if (visited.get(integer) == null || !visited.get(integer)) {
                ret.addLast(integer);
                visited.put(integer, true);
            } else {
                while (!ret.getLast().equals(integer)) {
                    ret.removeLast();

                }
            }
        }
        return ret;
    }

    protected List<Integer>[] readListFromFile(String path) throws IOException, ClassNotFoundException {
        FileInputStream f = new FileInputStream(new File(path));
        ObjectInputStream inputStream = new ObjectInputStream(f);
        List<Integer>[] paths = (List<Integer>[]) inputStream.readObject();
        return paths;
    }

    public List<List<Integer>> getPathsTo(int dest) {
        return mPathMap.get(dest);
    }
}
