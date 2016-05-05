package edu.jhu.bio.bca.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import edu.jhu.bio.bca.model.MGraph;

@Service
public class EmailGraphParser extends SimpleFileVisitor<Path> implements GraphParser {

	private static final Logger log = LoggerFactory.getLogger(EmailGraphParser.class);

	private Session s = Session.getDefaultInstance(new Properties());

	private int dirCount;
	private int fileCount;

	private MGraph graph = new MGraph();
	private Options opts = new Options();

	private boolean processing = false;

	private String suffix;
	private String folderNameFilter;
	private int weightThreshold;

	public EmailGraphParser() {
		opts.addOption("email_parser_email_suffix", true,
				"Only the email addresses ending with the given suffix will be processed e.g. enron.com");
		opts.addOption("email_parser_folder_name_filter", true,
				"Only the folders matching the given name will be processed e.g. sent");
		opts.addOption("email_parser_edge_min_weight", true,
				"All edges (and corresponding vertices) in the graph with weight less than the given weight are removed. default is 25");
	}

	@Override
	public Options getOptions() {
		return opts;
	}

	@Override
	public void init(CommandLine cli) {
		suffix = cli.getOptionValue("email_parser_email_suffix", "enron.com");
		folderNameFilter = cli.getOptionValue("email_parser_folder_name_filter", "sent");
		weightThreshold = Integer.parseInt(cli.getOptionValue("email_parser_edge_min_weight", "5"));
	}

	@Override
	public MGraph parse(String path) {
		try {
			Files.walkFileTree(Paths.get(path), this);
			System.out.println("Directories processed = " + dirCount);
			System.out.println("Files processed = " + fileCount);
			System.out.println("Graph generated with " + count(graph.getVertices()) + " vertices and "
					+ count(graph.getEdges()) + " edges ");
		} catch (IOException e) {
			log.error("error in parsing input data", e);
		}

		try {
			for (Edge edge : graph.getEdges()) {
				if ((double) edge.getProperty("weight") < weightThreshold) {
					graph.removeEdge(edge);
				}
			}
			for (Vertex vertex : graph.getVertices()) {
				if (count(vertex.getEdges(Direction.BOTH)) == 0) {
					graph.removeVertex(vertex);
				}
			}
			System.out.println("Graph reduced (based on weight threshold) to " + count(graph.getVertices())
					+ " vertices and " + count(graph.getEdges()) + " edges");
		} catch (Exception e) {
			log.error("error in removing light weight edges from the graph", e);
		}

		return graph;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (!processing) {
			return FileVisitResult.CONTINUE;
		}

		if (attrs.isRegularFile()) {
			fileCount++;

			try (FileInputStream fis = new FileInputStream(file.toFile())) {
				MimeMessage message = new MimeMessage(s, fis);

				for (Address fromEmail : message.getFrom()) {
					String from = fromEmail.toString();
					if (!StringUtils.isEmpty(suffix) && !from.endsWith(suffix)) {
						continue;
					}

					for (RecipientType type : Arrays.asList(RecipientType.TO, RecipientType.CC, RecipientType.BCC)) {

						if (message.getRecipients(type) != null) {
							Address[] recipients = message.getRecipients(type);

							for (Address email : recipients) {
								if (StringUtils.isEmpty(suffix) || email.toString().endsWith(suffix)) {
									addEmailPair(from.toLowerCase(), email.toString(), type, recipients.length);
								}
							}
						}
					}
				}
			} catch (AddressException e) {
				System.err.println("error in parsing email addresses from file " + file + " => " + e.getMessage());
			} catch (MessagingException e) {
				System.err.println("error in parsing email message from file " + file + " => " + e.getMessage());
			} catch (Exception e) {
				System.err.println("unexpected error in file " + file + " => " + e.getMessage());
			}
		}
		return FileVisitResult.CONTINUE;
	}

	private void addEmailPair(String from, String to, RecipientType type, int n) {
		Vertex fromVertex = graph.getVertex(from);
		if (fromVertex == null) {
			fromVertex = graph.addVertex(from);
		}

		Vertex toVertex = graph.getVertex(to);
		if (toVertex == null) {
			toVertex = graph.addVertex(to);
		}

		Edge edge = null;
		for (Edge e : fromVertex.getEdges(Direction.OUT)) {
			if (e.getVertex(Direction.IN) == toVertex) {
				edge = e;
			}
		}

		if (edge == null) {
			edge = graph.addEdge(UUID.randomUUID().toString(), fromVertex, toVertex, "e");
		}

		double weight = type == RecipientType.TO ? 1 : (double) 1 / n;
		if (edge.getProperty("weight") != null) {
			weight += (double) edge.getProperty("weight");
		}
		edge.setProperty("weight", weight);

		// System.out.println(from + " -> " + to + " (type) = " + type +
		// ", (n) = " + n + ", (weight) = " + w
		// + ", (edge-weight) = " + weight);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		String name = dir.getFileName().toString();
		if (StringUtils.isEmpty(folderNameFilter) || name.toLowerCase().contains(folderNameFilter)) {
			processing = true;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		dirCount++;
		processing = false;
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		System.err.println(exc);
		return FileVisitResult.CONTINUE;
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