package edu.jhu.bio.bca.parsers;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import edu.jhu.bio.bca.model.MGraph;

public interface GraphParser {

	MGraph parse(String path);

	Options getOptions();

	void init(CommandLine cli);
}
