package edu.jhu.bio.bca.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;

import edu.jhu.bio.bca.model.MGraph;

/**
 * @author C. Savkli, Dec 20, 2013
 * @version 1.0
 */

public class GraphUtils {
	public static MGraph readGraph(String file) {
		MGraph graph = new MGraph();
		System.out.println("Reading graph from file " + file + "\n");

		try {
			GraphMLReader.inputGraph(graph, file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return graph;
	}

	public static void saveGraph(MGraph graph, String file) {
		GraphMLWriter writer = new GraphMLWriter(graph);
		try {
			writer.outputGraph(new FileOutputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Saved graph to " + file + "\n");
	}

	public static void saveGraphForPageRankWithSpark(MGraph graph, String vFile, String eFile) throws IOException {
		int i = 1;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(vFile))) {
			for (Vertex vertex : graph.getVertices()) {
				vertex.setProperty("_id", i);
				writer.write(i + "," + vertex.getId());
				writer.newLine();
				i++;
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(eFile))) {
			for (Edge edge : graph.getEdges()) {
				Vertex t = edge.getVertex(Direction.IN);
				Vertex f = edge.getVertex(Direction.OUT);
				writer.write(f.getProperty("_id") + "\t" + t.getProperty("_id"));
				writer.newLine();
			}
		}
	}
}
