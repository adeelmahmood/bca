package edu.jhu.bio.bca.graph;

import java.util.Comparator;

import com.tinkerpop.blueprints.Vertex;

public class QueueComparator implements Comparator<Vertex> {

	public int compare(Vertex o1, Vertex o2) {
		double d1 = o1.getProperty("dist");
		double d2 = o2.getProperty("dist");

		return d1 > d2 ? 1 : d1 < d2 ? -1 : 0;
	};
}
