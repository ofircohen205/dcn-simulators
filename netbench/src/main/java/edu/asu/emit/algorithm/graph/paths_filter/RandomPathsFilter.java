package edu.asu.emit.algorithm.graph.paths_filter;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.utility.Constants;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Paths;

import java.util.Random;

public class RandomPathsFilter extends PathsFilter {
    Random rand;

    public RandomPathsFilter(Graph g) {
        super(g);
        rand = new Random(Simulator.getConfiguration().getLongPropertyOrFail(Constants.Simulation.SEED));
        // TODO Auto-generated constructor stub
    }

    @Override
    public Path filterPaths(Paths paths) {
        // TODO Auto-generated method stub
        return paths.getPaths().size() == 0 ? new Path(0) : paths.getPaths().get(rand.nextInt(paths.getPaths().size()));
    }

}
