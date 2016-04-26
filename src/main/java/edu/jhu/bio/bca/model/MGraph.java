package edu.jhu.bio.bca.model;

import java.util.HashMap;
import java.util.Map;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * @author C. Savkli, Feb 11, 2015
 * @version 1.0
 */

public class MGraph extends TinkerGraph {
	private static final long serialVersionUID = 1L;
	private DoubleMatrix2D adjacency = null;
	private Map<String, Integer> vertexIndices = null;
	private Map<Integer, String> vertexIDs = null;
	private int vertexCount = 0;

	public DoubleMatrix2D getAdjacency() {
		if (adjacency == null)
			initAdjacency();
		return adjacency;
	}

	private void initAdjacency() {
		initVertexIndices();

		adjacency = new SparseDoubleMatrix2D(vertexCount, vertexCount);

		for (Edge e : getEdges()) {
			Vertex from = e.getVertex(Direction.OUT);
			int i = vertexIndices.get(from.getId().toString());
			Vertex to = e.getVertex(Direction.IN);
			int j = vertexIndices.get(to.getId().toString());
			if (i != j) {
				adjacency.set(i, j, 1.0);
				adjacency.set(j, i, 1.0);
			}
		}
	}

	public String getVertexIDForMatrixElement(int j) {
		return vertexIDs.get(j);
	}

	private void initVertexIndices() {
		vertexIndices = new HashMap<>();
		vertexIDs = new HashMap<>();

		int counter = 0;
		for (Vertex v : getVertices()) {
			vertexIndices.put(v.getId().toString(), counter);
			vertexIDs.put(counter, v.getId().toString());
			counter++;
		}
		vertexCount = counter;
	}
}
