package edu.jhu.bio.bca;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGraphML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import edu.jhu.bio.bca.graph.BetweennessCentrality;
import edu.jhu.bio.bca.model.MGraph;
import edu.jhu.bio.bca.parsers.GraphParser;
import edu.jhu.bio.bca.utils.GraphUtils;

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
		Graph g = new DefaultGraph("g");
		FileSource fs = new FileSourceGraphML();
		fs.addSink(g);
		try {
			fs.readAll("out.graphml");
		} catch (IOException e) {
		} finally {
			fs.removeSink(g);
		}
		g.display();
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
