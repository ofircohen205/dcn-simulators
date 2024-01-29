package edu.asu.emit.algorithm.graph;

import java.util.HashSet;
import java.util.LinkedList;

public class Paths {
	LinkedList<Path> mPaths;
	
	public Paths(LinkedList<Path> pathsSet) {
		mPaths = new LinkedList<Path>();
		for(Path p : pathsSet) {
			mPaths.add(new Path(p.getVertexList(),p.getWeight()));
		}
	}
	
	public Paths(Path path) {
		mPaths = new LinkedList<Path>();
		mPaths.add(path);
	}
	
	public Paths() {
		mPaths = new LinkedList<Path>();
	}

	public LinkedList<Path> getPaths() {
		return mPaths;
	}
	
	public String toString() {
		String ret = "";
		for(Path p : mPaths) {
			ret += p.toString() + "\n";
		}
		return ret;
	}

	
	public void clear() {
		mPaths.clear();
		
	}
}
