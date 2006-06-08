package org.unicode.cldr.icu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;

public class ConvertTransforms extends CLDRConverterTool{
	
	private static final int
	HELP1 = 0,
	HELP2 = 1,
	SOURCEDIR = 2,
	DESTDIR = 3,
	MATCH = 4
	;
	
	private static final UOption[] options = {
		UOption.HELP_H(),
		UOption.HELP_QUESTION_MARK(),
		UOption.SOURCEDIR().setDefault(Utility.COMMON_DIRECTORY + "transforms/"),
		UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "transforms/"),
		UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	};
	
	static final String HELP_TEXT1 = "Use the following options" + XPathParts.NEWLINE
	+ "-h or -?\t for this message" + XPathParts.NEWLINE
	+ "-"+options[SOURCEDIR].shortName + "\t source directory. Default = -s" 
	+ Utility.getCanonicalName(Utility.MAIN_DIRECTORY) + XPathParts.NEWLINE
	+ "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
	+ "-"+options[DESTDIR].shortName + "\t destination directory. Default = -d"
	+ Utility.getCanonicalName(Utility.GEN_DIRECTORY + "main/") + XPathParts.NEWLINE
	+ "-m<regex>\t to restrict the files to what matches <regex>" + XPathParts.NEWLINE;

	// TODO add options to set input and output directories, matching pattern
	public static void main(String[] args) throws Exception {
	    ConvertTransforms ct = new ConvertTransforms();
        ct.processArgs(args);
	}
	
	public void writeTransforms(String inputDirectory, String matchingPattern, String outputDirectory) throws IOException {
		System.out.println(new File(inputDirectory).getCanonicalPath());
		Factory cldrFactory = CLDRFile.Factory.make(inputDirectory, matchingPattern);
		Set ids = cldrFactory.getAvailable();
		PrintWriter index = BagFormatter.openUTF8Writer(outputDirectory, "root.txt");
		try {
			index.println("// File: root.txt");
			index.println("// Generated from CLDR: " + new Date());
			index.println();
			index.println("root {");
			index.println("\tRuleBasedTransliteratorIDs {");
			for (Iterator idIterator = ids.iterator(); idIterator.hasNext();) {
				String id = (String) idIterator.next();
				if (id.equals("All")) continue;
				try {
					convertFile(cldrFactory, id, outputDirectory, index);
				} catch (IOException e) {
					System.err.println("Failure in: " + id);
					throw e;
				}
			}
			index.println("\t}");
			index.println("\tTransliteratorNamePattern {");
			index.println("\t\t// Format for the display name of a Transliterator.");
			index.println("\t\t// This is the language-neutral form of this resource.");
			index.println("\t\t\"{0,choice,0#|1#{1}|2#{1}-{2}}\" // Display name");
			index.println("\t}");
			index.println("\t// Transliterator display names");
			index.println("\t// This is the English form of this resource.");
			index.println("\t\"%Translit%Hex\"         { \"%Translit%Hex\" }");
			index.println("\t\"%Translit%UnicodeName\" { \"%Translit%UnicodeName\" }");
			index.println("\t\"%Translit%UnicodeChar\" { \"%Translit%UnicodeChar\" }");
			index.println("\tTransliterateLATIN{        ");
			index.println("\t\"\",");
			index.println("\t\"\"");
			index.println("\t}");
			index.println("}");
		} finally {
			index.close();
		}	
	}

	private void convertFile(Factory cldrFactory, String id, String outputDirectory, PrintWriter index) throws IOException {
		PrintWriter output = null;
		String filename = null;
		CLDRFile cldrFile = cldrFactory.make(id, false);
		boolean first = true;
		for (Iterator it = cldrFile.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
			String path = (String) it.next();
			String value = cldrFile.getStringValue(path);
			if (first) {
				filename = addIndexInfo(index, path);
				if (filename == null) return; // not a transform file!
				output = BagFormatter.openUTF8Writer(outputDirectory, filename);
				output.println("# File: " + id + ".txt");
				output.println("# Generated from CLDR: " + new Date());
				output.println("#");
				first = false;
			}
			if (path.indexOf("/comment") >= 0) {
				output.println(value);
			} else if (path.indexOf("/tRule") >= 0) {
				output.println(value);
			} else {
				throw new IllegalArgumentException("Unknown element: " + path + "\t " + value);
			}
		}
		output.close();
	}
	
	static XPathParts parts = new XPathParts();
	
	private String addIndexInfo(PrintWriter index, String path) {
		parts.set(path);
		Map attributes = parts.findAttributes("transform");
		if (attributes == null) return null; // error, not a transform file
		String source = (String) attributes.get("source");
		String target = (String) attributes.get("target");
		String variant = (String) attributes.get("variant");
		String direction = (String) attributes.get("direction");
		String visibility = (String) attributes.get("visibility");
		
		String status = "internal".equals(visibility) ? "internal" : "file";

		String id = source + "-" + target;
		String rid = target + "-" + source;
		String filename = source + "_" + target;
		if (variant != null) {
			id += "/" + variant;
			rid += "/" + variant;
			filename += "_" + variant;
		}
		filename += ".txt";
		if (direction.equals("both") || direction.equals("forward")) {
			index.println("\t\t" + id + " {");
			index.println("\t\t\t" + status + " {");
			index.println("\t\t\t\tresource:process(transliterator){\"" + filename + "\"}");
			index.println("\t\t\t\tdirection{\"FORWARD\"}");
			index.println("\t\t\t}");
			index.println("\t\t}");
		}
		if (direction.equals("both") || direction.equals("backward")) {
			
			index.println("\t\t" + rid + "{");
			index.println("\t\t\t" + status + " {");
			index.println("\t\t\t\tresource:process(transliterator){\"" + filename + "\"}");
			index.println("\t\t\t\tdirection{\"BACKWARD\"}");
		}
		index.println();
		return filename;
	}
	
	// fixData ONLY NEEDED TO FIX FILE PROBLEM
	
	private void fixData(String inputDirectory, String matchingPattern, String outputDirectory) throws IOException {
		File dir = new File(inputDirectory);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; ++i) {
			if (files[i].isDirectory()) continue;
			BufferedReader input = BagFormatter.openUTF8Reader("", files[i].getCanonicalPath());
			PrintWriter output = BagFormatter.openUTF8Writer("", outputDirectory + files[i].getName());
			while (true) {
				String line = input.readLine();
				if (line == null) break;
				if (line.indexOf("DOCTYPE") >= 0) {
					line = line.replaceAll(" ldml ", " supplementalData ");
				}
				output.println(line);
			}
			input.close();
			output.close();
		}
	}

    public void processArgs(String[] args) {
        // TODO Auto-generated method stub
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
            System.out.println(HELP_TEXT1);
            return;
        }
        
        String sourceDir = options[SOURCEDIR].value;    // Utility.COMMON_DIRECTORY + "transforms/";
        String targetDir = options[DESTDIR].value;  // Utility.GEN_DIRECTORY + "main/";
        String match = options[MATCH].value;
        
        try {
//          fixData(sourceDir, match, targetDir);
            writeTransforms(sourceDir, match, targetDir+File.separator);
        }catch(IOException ex){
            RuntimeException e = new RuntimeException();
            e.initCause(ex.getCause());
            throw e;
        }finally {
            System.out.println("DONE");
        }
    }
}