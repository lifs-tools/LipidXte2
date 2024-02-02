package de.mpicbg.ms.model;

import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

import java.io.File;
import java.util.Collections;
import java.util.List;


/**
 * This class reads XML-based mass spec data formats (mzData, mzXML) using the
 * jmzreader library and returns a scan colleciton.
 */
public class ScanCollection
{
	private final File sourceFile;
	private MzXMLFile parser;

	private final List<Scan> scans;
	private String name;

	public ScanCollection(File sourceFile, MzXMLFile parser, List<Scan> scans) {
		this.sourceFile = sourceFile;
		this.parser = parser;
		this.name = sourceFile.getName();
		this.scans = scans;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getOriginalFile() {
		return sourceFile;
	}

	public List<Scan> getScans() {
		return Collections.unmodifiableList( scans );
	}

	public void dispose() {
		parser = null;
	}

	MzXMLFile getParser() {
		return parser;
	}

	public Scan getScan(int idx)
	{
		return scans.stream().filter( i -> i.getNum() == idx ).findFirst().get();
	}
}
