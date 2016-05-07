package edu.jhu.bio.bca.graph;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import edu.jhu.bio.bca.model.MGraph;

/**
 * BetweennessCentrality
 * 
 * Implementation based on Brandes algorithm as described in "Graph algorithms
 * in the Language of Linear Algebra" book. Chapter 6
 * 
 * @author adeelq
 *
 */
public class BetweennessCentrality {

	private final MGraph graph;

	public BetweennessCentrality(MGraph graph) {
		this.graph = graph;
	}

	/**
	 * Process the complete graph and computes betweenness centrality for each
	 * node in the graph
	 */
	public void process() {
		// zero out all centrality values
		resetCentralities();

		// process each node
		for (Vertex s : graph.getVertices()) {
			// reset any shortest path related properties on the node
			resetShortPaths();

			// delta is used in reverse calculation of BC, reset the property on
			// the node
			resetDeltas();
			// calculate shortest paths to all nodes from the node s
			// and store the results in a FILO stack so that we are able to
			// process the last element in the shortest path first
			PriorityQueue<Vertex> stack = calculateShortestPaths(s);

			// process nodes in the shortest path
			while (!stack.isEmpty()) {
				Vertex w = stack.poll();

				// process each predecessor and calculate delta
				for (Vertex v : getPreds(w)) {
					double delta = getDelta(v) + (getSigma(v) / getSigma(w)) * (1 + getDelta(w));
					setDelta(v, delta);
				}

				// update centrlaity
				if (w != s) {
					setCentrality(w, getCentrality(w) + getDelta(w));
				}
			}
		}
	}

	/**
	 * Calculates shortest paths to all other nodes in the graph from the given
	 * node using the Dijkestra algorithm
	 * 
	 * @param vertex
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private PriorityQueue<Vertex> calculateShortestPaths(Vertex vertex) {
		PriorityQueue<Vertex> stack = new PriorityQueue<Vertex>(new StackComparator());
		PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>(new QueueComparator());

		// sigma represents the shortest path from given vertex
		setSigma(vertex, 1);
		// distance represents the number of hops from given vertex
		setDistance(vertex, 0);

		queue.add(vertex);
		while (!queue.isEmpty()) {
			Vertex v = queue.poll();
			stack.add(v);

			// process all neighbors of given vertex
			for (Vertex w : v.getVertices(Direction.IN)) {
				// distance so far
				double currentDistance = getDistance(w);
				// distance to next node
				double forwardDistance = getDistance(v) + getEdgeWeight(v, w);

				// found a shorter path
				if (forwardDistance < currentDistance) {
					// queues need to be updated to reposition elements based on
					// new "dist" property
					updateQueuesWithDistance(w, forwardDistance, queue, stack);
					// unvisited node
					if (currentDistance == Integer.MAX_VALUE) {
						queue.add(w);
					}
					setSigma(w, 0);
					getPreds(w).clear();
				}

				// add current node to the shortest path and update sigma and
				// preds
				if (getDistance(w) == forwardDistance) {
					setSigma(w, getSigma(w) + getSigma(v));
					getPreds(w).add(v);
				}
			}
		}

		return stack;
	}

	/**
	 * Removes and reinserts the given vertices in the queue with the updated
	 * distance. This is a common technique to perform in-place sorting of the
	 * queue elements
	 * 
	 * @param v
	 * @param d
	 * @param queues
	 */
	@SuppressWarnings("unchecked")
	private void updateQueuesWithDistance(Vertex v, double d, PriorityQueue<Vertex>... queues) {
		setDistance(v, d);
		for (PriorityQueue<Vertex> q : queues) {
			if (q.contains(v)) {
				q.remove(v);
				q.add(v);
			}
		}
	}

	private void resetCentralities() {
		for (Vertex v : graph.getVertices()) {
			setCentrality(v, 0);
		}
	}

	private void resetShortPaths() {
		for (Vertex v : graph.getVertices()) {
			setPreds(v, new HashSet<Vertex>());
			setSigma(v, 0);
			setDistance(v, Integer.MAX_VALUE);
		}
	}

	private void resetDeltas() {
		for (Vertex v : graph.getVertices()) {
			setDelta(v, 0);
		}
	}

	/**
	 * Returns edge weight between the given nodes. Default is 1
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private double getEdgeWeight(Vertex from, Vertex to) {
		for (Edge e : from.getEdges(Direction.IN)) {
			if (e.getVertex(Direction.OUT) == to) {
				return e.getProperty("weight") != null ? (double) e.getProperty("weight") : 1;
			}
		}
		return 0;
	}

	private double getDistance(Vertex v) {
		return (double) v.getProperty("dist");
	}

	private void setDistance(Vertex v, double dist) {
		v.setProperty("dist", dist);
	}

	private double getSigma(Vertex v) {
		return (double) v.getProperty("sigma");
	}

	private void setSigma(Vertex v, double sigma) {
		v.setProperty("sigma", sigma);
	}

	@SuppressWarnings("unchecked")
	private Set<Vertex> getPreds(Vertex v) {
		return (Set<Vertex>) v.getProperty("preds");
	}

	private void setPreds(Vertex v, Set<Vertex> preds) {
		v.setProperty("preds", preds);
	}

	private double getCentrality(Vertex v) {
		return (double) v.getProperty("cent");
	}

	private void setCentrality(Vertex v, double cent) {
		v.setProperty("cent", cent);
	}

	private double getDelta(Vertex v) {
		return v.getProperty("delta");
	}

	private void setDelta(Vertex v, double delta) {
		v.setProperty("delta", delta);
	}

	@SuppressWarnings("unused")
	private <T> int count(Iterable<T> recs) {
		int count = 0;
		for (T rec : recs) {
			count++;
		}
		return count;
	}
}