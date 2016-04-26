package edu.jhu.bio.bca.graph;

import org.junit.Test;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import edu.jhu.bio.bca.model.MGraph;
import edu.jhu.bio.bca.utils.GraphUtils;

public class BetweennessCentralityTests {

	BetweennessCentrality bc;

	private static MGraph graph;
	private static String graphName = "simple";
	public static final String BASE_DIRECTORY = "src/test/resources";
	private static final String GRAPH_FILE = BASE_DIRECTORY + "/" + graphName + ".graphml";

	@Test
	public void test() {
		graph = GraphUtils.readGraph(GRAPH_FILE);

		bc = new BetweennessCentrality(graph);
		bc.process();
	}

	@Test
	public void test2() {
		graph = GraphUtils.readGraph(GRAPH_FILE);

		for (Vertex v : graph.getVertices()) {
			if (v.getId().equals("2")) {
				v.getEdges(Direction.BOTH).forEach(e -> graph.removeEdge(e));
				graph.removeVertex(v);
			}
		}

		for (Vertex v : graph.getVertices()) {
			System.out.println(v.getId());
			for (Edge e : v.getEdges(Direction.BOTH)) {
				System.out.println("\tIN=" + e.getVertex(Direction.IN) + " OUT=" + e.getVertex(Direction.OUT));
			}
		}
	}
}
