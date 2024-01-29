/*
 *
 * Copyright (c) 2004-2008 Arizona State University.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ARIZONA STATE UNIVERSITY ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL ARIZONA STATE UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package edu.asu.emit.algorithm.graph;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import ch.ethz.systems.netbench.core.Simulator;

/**
 * The class defines a path in graph.
 * 
 * @author yqi
 * @author snkas
 */
public class Path implements BaseElementWithWeight, Serializable {
	private static long pCounter = 0;
	private long id;
	// List of vertices in the path
	protected final List<Vertex> vertexList;
	private long issueTime;
    // Total path weight
	private double weight;
	private int mColor;
	private long mFlowSize = -1; // for debugging

	/**
     * Create a path which should have a pre-determined weight.
     *
     * Afterwards, the vertex list *must* be adapted to include
     * all vertices on the path such that the weight is summed up correctly
     * to the total weight.
     *
     * @param cost    Total path weight
     */
	public Path(double cost) {
        this.vertexList = new Vector<>();
        this.weight = cost;
        this.mColor = -1;
        this.id = ++pCounter;
        this.issueTime = Simulator.getCurrentTime();
    }

	public Path(double cost,int color) {
		this(cost);
		this.mColor = color;
	}

    /**
     * Create a path with a pre-determined vertex list and weight.
     *
     * @param vertexList    Path vertex list
     * @param weight2        Total path weight
     */
	public Path(List<Vertex> vertexList, double weight2) {
		this.vertexList = new Vector<>();
		this.vertexList.addAll(vertexList);
		this.weight = weight2;
		this.mColor = -1;
		this.id = ++pCounter;
		this.issueTime = Simulator.getCurrentTime();
	}

	public Path(List<Integer> list,int color) {
		this(list.size());
		for(int i = 0; i<list.size(); i++){
			vertexList.add(new Vertex(list.get(i)));
		}
		mColor = color;

	}

	public Path(List<Integer> path, int color, long id) {
		this(path,color);
		pCounter--;
		this.id = id;
	}

	public long getId() {
		return this.id;
	}

	
	public long getIssueTime() {
		return this.issueTime;
	}
    /**
     * Retrieve the total weight of the path.
     *
     * @return Total path weight
     */
	public double getWeight() {
		return weight;
	}

    /**
     * Set the total weight of the path.
     *
     * @param d    Total path weight
     */
    public void setWeight(double d) {
        this.weight = d;
    }

    /**
     * Retrieve list of all vertices on the path (source to destination).
     *
     * @return  Path vertex list
     */
	public List<Vertex> getVertexList() {
		return vertexList;
	}
	
	public void addAll(Path orgPath) {
		getVertexList().addAll(orgPath.getVertexList());
		
	}
	
	public void add(int sourceTor) {
		getVertexList().add(new Vertex(sourceTor));
		
	}
	
	public void add(int index,int sourceTor) {
		getVertexList().add(index,new Vertex(sourceTor));
		
	}
	
	@Override
	public boolean equals(Object right) {
		
		if (right instanceof Path) {
			Path rPath = (Path) right;
			return vertexList.equals(rPath.vertexList);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return vertexList.hashCode();
	}

    @Override
	public String toString() {
		return vertexList.toString() + ":" + weight + ":" + this.id + ":" + getColor() + ":" + this.issueTime;
	}

	public boolean endsWith(Vertex endVertex) {
		// TODO Auto-generated method stub
		return vertexList.get(vertexList.size()-1).equals(endVertex);
	}

	public Vertex getLastVertex() {
		// TODO Auto-generated method stub
		return vertexList.get(vertexList.size()-1);
	}

	public void addVertex(Vertex v, double edgeWeight) {
		vertexList.add(v);
		weight+=edgeWeight;
		
	}

	public Vertex getFirstVertex() {
		return vertexList.get(0);
	}

	public void setColor(int c) {
		this.mColor = c;
	}

	public int getColor() {
		return mColor;
	}

	public void setFlowSize(long flowSize) {
		mFlowSize = flowSize;
	}
}
