package edu.jhu.bio.bca;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JFrame;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;

import edu.jhu.bio.bca.graph.BetweennessCentrality;
import edu.jhu.bio.bca.model.MGraph;
import edu.jhu.bio.bca.parsers.GraphParser;
import edu.jhu.bio.bca.utils.GraphUtils;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

@Configuration
@ComponentScan
public class Application implements CommandLineRunner {

	private Options opts = new Options();

	@Autowired
	List<GraphParser> parsers;

	@SuppressWarnings("unchecked")
	@Override
	public void run(String... args) throws Exception {
		opts.addOption("input", true, "Specify the path to the folder that contains the input files");
		opts.addOption("parser", true, "Class name of graph parser to use");
		opts.addOption("help", false, "Display help");

		// collect options from all parsers
		for (GraphParser parser : parsers) {
			parser.getOptions().getOptions().forEach(o -> opts.addOption((Option) o));
		}

		// parse command line options
		CommandLine cli = new GnuParser().parse(opts, args);
		if (args.length == 0 || cli.hasOption("help")) {
			showHelp();
			System.exit(0);
		}

		// input path
		String path = cli.getOptionValue("input");

		// retrieve parser
		String parserClassName = cli.getOptionValue("parser");
		GraphParser parser = getParser(GraphParser.class.getPackage().getName() + "." + parserClassName);
		if (parser == null) {
			System.err.println("Unknown parser " + parserClassName);
		}

		// init the parser
		parser.init(cli);

		// run the parser to generate the graph
		MGraph graph = parser.parse(path);

		// run betweenness centrality on this graph
		BetweennessCentrality bc = new BetweennessCentrality(graph);
		bc.process();

		// save final graph
		GraphUtils.saveGraph(graph, "out.graphml");

		// visuailzation
		GraphJung<MGraph> graph2 = new GraphJung<MGraph>(graph);
		CircleLayout<Vertex, Edge> layout = new CircleLayout<Vertex, Edge>(graph2);
		layout.setSize(new Dimension(300, 300));
		BasicVisualizationServer<Vertex, Edge> viz = new BasicVisualizationServer<Vertex, Edge>(layout);
		viz.setPreferredSize(new Dimension(350, 350));

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(viz);
		frame.pack();
		frame.setVisible(true);
	}

	private GraphParser getParser(String clsName) throws ClassNotFoundException {
		for (GraphParser parser : parsers) {
			if (parser.getClass() == Class.forName(clsName)) {
				return parser;
			}
		}
		return null;
	}

	private void showHelp() {
		new HelpFormatter().printHelp("BC Application", opts);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}