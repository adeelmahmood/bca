package edu.jhu.bio.bca.graph;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import edu.jhu.bio.bca.model.MGraph;

public class BetweennessCentrality {

	private final MGraph graph;

	public BetweennessCentrality(MGraph graph) {
		this.graph = graph;
	}

	public void process() {
		resetCentralities();

		for (Vertex s : graph.getVertices()) {
			resetShortPaths();
			// System.out.println("s = " + s.getId());

			resetDeltas();
			PriorityQueue<Vertex> stack = calculateShortestPaths(s);

			while (!stack.isEmpty()) {
				Vertex w = stack.poll();
				// System.out.println(w.getId() + " sigma[" + getSigma(w) +
				// "], dist[" + getDistance(w) + "], pred[" +
				// Arrays.toString(getPreds(w).toArray()));
				// System.out.println("\tw = " + w.getId());

				for (Vertex v : getPreds(w)) {
					// System.out.println("\t\tv = " + v.getId());
					double delta = getDelta(v) + (getSigma(v) / getSigma(w)) * (1 + getDelta(w));
					// System.out.println("\t\tdelta ==> " + getDelta(v) +
					// " + (" + getSigma(v) + "/" + getSigma(w)
					// + ") * (1 + " + getDelta(w) + ") ==> " + delta);
					setDelta(v, delta);
				}

				if (w != s) {
					setCentrality(w, getCentrality(w) + getDelta(w));
				}
			}
		}

		cleanup();
	}

	private void cleanup() {
		for (Vertex vertex : graph.getVertices()) {
			if (getCentrality(vertex) <= 0) {
				vertex.getEdges(Direction.BOTH).forEach(e -> graph.removeEdge(e));
				graph.removeVertex(vertex);
			}
		}
		System.out.println("Graph with Betweenness Centrality reduced to " + count(graph.getVertices())
				+ " vertices and " + count(graph.getEdges()) + " edges");
	}

	@SuppressWarnings("unchecked")
	private PriorityQueue<Vertex> calculateShortestPaths(Vertex vertex) {
		PriorityQueue<Vertex> stack = new PriorityQueue<Vertex>(new StackComparator());
		PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>(new QueueComparator());

		setSigma(vertex, 1);
		setDistance(vertex, 0);

		queue.add(vertex);
		while (!queue.isEmpty()) {
			Vertex v = queue.poll();
			stack.add(v);

			for (Vertex w : v.getVertices(Direction.IN)) {
				double currentDistance = getDistance(w);
				double forwardDistance = getDistance(v) + getEdgeWeight(v, w);

				if (forwardDistance < currentDistance) {
					updateQueuesWithDistance(w, forwardDistance, queue, stack);
					if (currentDistance == Integer.MAX_VALUE) {
						queue.add(w);
					}
					setSigma(w, 0);
					getPreds(w).clear();
				}

				if (getDistance(w) == forwardDistance) {
					setSigma(w, getSigma(w) + getSigma(v));
					getPreds(w).add(v);
				}
			}
		}

		return stack;
	}

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