package ch.ethz.systems.netbench.xpt.sourcerouting.exceptions;

import ch.ethz.systems.netbench.core.config.NBProperties;

public class NoPathException extends RuntimeException {

	public NoPathException(int source, int dest) {
        super("No path from source " + source + " to dest " + dest);
    }

	public NoPathException() {
		// TODO Auto-generated constructor stub
	}
}
