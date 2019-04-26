package org.unicode.cldr.icu;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.Transliterator;

/**
 * Utility to generate the Tansliteration resource bundle files.
 */
public class ConvertTransforms extends CLDRConverterTool {

    private static final int HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        SKIP_COMMENTS = 5,
        WRITE_INDEX = 6,
        VERBOSE = 7,
        APPROVED_ONLY = 8;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault(CLDRPaths.COMMON_DIRECTORY + "transforms/"),
        UOption.DESTDIR().setDefault(CLDRPaths.GEN_DIRECTORY + "icu-transforms/"),
        UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("commentSkip", 'c', UOption.NO_ARG),
        UOption.create("writeIndex", 'x', UOption.NO_ARG),
        UOption.VERBOSE(),
        UOption.create("approvedOnly", 'a', UOption.NO_ARG),
    };

    static final String HELP_TEXT1 = "Use the following options" + XPathParts.NEWLINE
        + "-h or -?\t for this message" + XPathParts.NEWLINE
        + "-" + options[SOURCEDIR].shortName + "\t source directory. Default = -s"
        + CldrUtility.getCanonicalName(CLDRPaths.MAIN_DIRECTORY) + XPathParts.NEWLINE
        + "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
        + "-" + options[DESTDIR].shortName + "\t destination directory. Default = -d"
        + CldrUtility.getCanonicalName(CLDRPaths.GEN_DIRECTORY + "main/") + XPathParts.NEWLINE
        + "-m<regex>\t to restrict the files to what matches <regex>" + XPathParts.NEWLINE
        + "-a\t to only include transforms with approved/contributed status" + XPathParts.NEWLINE
    // "--writeIndex / -x   to write the index (trnsfiles.mk)"+ XPathParts.NEWLINE
    ;

    // TODO add options to set input and output directories, matching pattern
    public static void main(String[] args) throws Exception {
        ConvertTransforms ct = new ConvertTransforms();
        ct.processArgs(args);
    }

    private boolean skipComments;
    private boolean writeIndex = false;
    private boolean verbose = false;
    private boolean approvedOnly = false;

    int fileCount = 0;

    public void writeTransforms(String inputDirectory, String matchingPattern, String outputDirectory)
        throws IOException {
        System.out.println(new File(inputDirectory).getCanonicalPath());
        Factory cldrFactory = (approvedOnly) ? Factory.make(inputDirectory, matchingPattern, DraftStatus.contributed)
            : Factory.make(inputDirectory, matchingPattern);
        Set<String> ids = cldrFactory.getAvailable();
        PrintWriter index = FileUtilities.openUTF8Writer(outputDirectory, "root.txt");
        doHeader(index, "//", "root.txt");
        try {
            index.println("root {");
            index.println("    RuleBasedTransliteratorIDs {");
            // addAlias(index, "Latin", "el", "", "Latin", "Greek", "UNGEGN");
            // addAlias(index, "el", "Latin", "", "Greek", "Latin", "UNGEGN");
            // addAlias(index, "Latin", "Jamo", "", "Latin", "ConjoiningJamo", "");
            addAlias(index, "Tone", "Digit", "", "Pinyin", "NumericPinyin", "");
            addAlias(index, "Digit", "Tone", "", "NumericPinyin", "Pinyin", "");
            // addAlias(index, "Simplified", "Traditional", "", "Hans", "Hant", "");
            // addAlias(index, "Traditional", "Simplified", "", "Hant", "Hans", "");
            for (String id : ids) {
                if (id.equals("All")) continue;
                try {
                    convertFile(cldrFactory, id, outputDirectory, index);
                } catch (IOException e) {
                    System.err.println("Failure in: " + id);
                    throw e;
                }
            }
            index.println("    }");
            index.println("    TransliteratorNamePattern {");
            index.println("        // Format for the display name of a Transliterator.");
            index.println("        // This is the language-neutral form of this resource.");
            index.println("        \"{0,choice,0#|1#{1}|2#{1}-{2}}\" // Display name");
            index.println("    }");
            index.println("    // Transliterator display names");
            index.println("    // This is the English form of this resource.");
            index.println("    \"%Translit%Hex\"         { \"%Translit%Hex\" }");
            index.println("    \"%Translit%UnicodeName\" { \"%Translit%UnicodeName\" }");
            index.println("    \"%Translit%UnicodeChar\" { \"%Translit%UnicodeChar\" }");
            index.println("    TransliterateLATIN{        ");
            index.println("    \"\",");
            index.println("    \"\"");
            index.println("    }");
            index.println("}");
        } finally {
            index.close();
        }
    }

    public static PrintWriter makePrintWriter(ByteArrayOutputStream bytes) {
        try {
            OutputStreamWriter outStream = new OutputStreamWriter(bytes, "UTF-8");
            BufferedWriter buff = new BufferedWriter(outStream, 4 * 1024);
            PrintWriter p = new PrintWriter(buff);

            return p;
        } catch (Exception e) {
            System.err.println("Error: Could not create OutputStreamWriter.");
        }
        return null;
    }

    private void showComments(PrintWriter toilet, String value) {
        String[] lines = value.trim().split("\\r\\n?|\\n");
        for (String line : lines) {
            if (!line.startsWith("#")) {
                line = "# " + line;
            }
            toilet.println(line);
        }
    }

    private void convertFile(Factory cldrFactory, String id, String outputDirectory, PrintWriter index)
        throws IOException {
        PrintWriter output = null;
        String filename = null;
        CLDRFile cldrFile = cldrFactory.make(id, false);
        boolean first = true;
        for (Iterator<String> it = cldrFile.iterator("", cldrFile.getComparator()); it.hasNext();) {
            String path = it.next();
            if (path.indexOf("/version") >= 0 || path.indexOf("/generation") >= 0) {
                continue;
            }
            String value = cldrFile.getStringValue(path);
            if (first) {
                String fullPath = cldrFile.getFullXPath(path);
                filename = addIndexInfo(index, fullPath);
                if (filename == null) {
                    return; // not a transform file!
                }
                output = FileUtilities.openUTF8Writer(outputDirectory, filename);
                doHeader(output, "#", filename);
                first = false;
            }
            if (path.indexOf("/comment") >= 0) {
                if (!skipComments) {
                    showComments(output, value);
                }
            } else if (path.indexOf("/tRule") >= 0) {
                value = fixup.transliterate(value);
                value = value.replaceAll(CldrUtility.LINE_SEPARATOR, System.lineSeparator());
                output.println(value);
            } else {
                throw new IllegalArgumentException("Unknown element: " + path + "\t " + value);
            }
        }
        if (output != null) { // null for transforms whose draft status is too low
            output.close();
        }
    }

    public static final Transliterator fixup = Transliterator.getInstance("[:Mn:]any-hex/java");

    public static String replaceUnquoted(String value, String toReplace, String replacement) {
        // quick exit in most cases
        if (value.indexOf(toReplace) < 0)
            return value;

        String updatedValue = "";
        int segmentStart = 0;
        boolean inQuotes = false;
        boolean ignoreCharValue = false;
        int length = value.length();

        for (int pos = 0; pos < length; ++pos) {
            char curChar = (char) 0;

            if (ignoreCharValue) {
                ignoreCharValue = false;
            } else {
                curChar = value.charAt(pos);
            }

            if (curChar == '\\') {
                // escape, ignore the value of the next char (actually the next UTF16 code unit, but that works here)
                ignoreCharValue = true;
            }
            boolean isLastChar = (pos + 1 >= length);
            if (curChar == '\'' || isLastChar) {
                // quote, begin or end of a quoted literal (in which no replacement takes place)
                if (inQuotes) {
                    // End of a quoted segment; guaranteed to include at least opening quote.
                    // Just add the segment (including current char) to updatedValue.
                    updatedValue = updatedValue + value.substring(segmentStart, pos + 1);
                    segmentStart = pos + 1;
                } else {
                    if (isLastChar)
                        ++pos;
                    if (pos > segmentStart) {
                        // End of a nonempty unquoted segment; perform requested replacements and
                        // then add segment to updatedValue.
                        String currentSegment = value.substring(segmentStart, pos);
                        updatedValue = updatedValue + currentSegment.replace(toReplace, replacement);
                        segmentStart = pos;
                    }
                }
                inQuotes = !inQuotes;
            }
            // else the char just becomes part of the current segment
        }
        return updatedValue;
    }

    private String addIndexInfo(PrintWriter index, String path) {
        XPathParts parts = XPathParts.getTestInstance(path);
        Map<String, String> attributes = parts.findAttributes("transform");
        if (attributes == null) return null; // error, not a transform file
        String source = attributes.get("source");
        String target = attributes.get("target");
        String variant = attributes.get("variant");
        String direction = attributes.get("direction");
        String alias = attributes.get("alias");
        String backwardAlias = attributes.get("backwardAlias");
        String visibility = attributes.get("visibility");

        String status = "internal".equals(visibility) ? "internal" : "file";

        fileCount++;

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
            if (verbose) {
                System.out.println("    " + id + "    " + filename + "    " + "FORWARD");
            }
            if (alias != null) {
                for (String ali : alias.trim().split("\\s+")) {
                    addAlias(index, ali, id);
                }
            }
            index.println("        " + id + " {");
            index.println("            " + status + " {");
            index.println("                resource:process(transliterator) {\"" + filename + "\"}");
            index.println("                direction {\"FORWARD\"}");
            index.println("            }");
            index.println("        }");
        }
        if (direction.equals("both") || direction.equals("backward")) {
            if (verbose) {
                System.out.println("    " + rid + "    " + filename + "    " + "REVERSE");
            }
            if (backwardAlias != null) {
                for (String bali : backwardAlias.trim().split("\\s+")) {
                    addAlias(index, bali, rid);
                }
            }
            index.println("        " + rid + " {");
            index.println("            " + status + " {");
            index.println("                resource:process(transliterator) {\"" + filename + "\"}");
            index.println("                direction {\"REVERSE\"}");
            index.println("            }");
            index.println("        }");
        }
        index.println();
        return filename;
    }

    void addAlias(PrintWriter index, String aliasSource, String aliasTarget, String aliasVariant,
        String originalSource, String originalTarget, String originalVariant) {
        // Spacedhan-Han {
        // alias {"null"}
        // }
        addAlias(index, getName(aliasSource, aliasTarget, aliasVariant),
            getName(originalSource, originalTarget, originalVariant));
    }

    private void addAlias(PrintWriter index, String alias, String original) {
        index.println("        " + alias + " {");
        index.println("            alias" + " {\"" + original + "\"}");
        index.println("        }");
    }

    String getName(String source, String target, String variant) {
        String id = source + "-" + target;
        if (variant != null && variant.length() != 0) {
            id += "/" + variant;
        }
        return id;
    }

    private void doHeader(PrintWriter output, String quoteSymbol, String filename) {
        output.print('\uFEFF');
        output.println(quoteSymbol + " © 2016 and later: Unicode, Inc. and others.");
        output.println(quoteSymbol + " License & terms of use: http://www.unicode.org/copyright.html#License");
        output.println(quoteSymbol);
        output.println(quoteSymbol + " File: " + filename);
        output.println(quoteSymbol + " Generated from CLDR");
        output.println(quoteSymbol);
    }

    public void processArgs(String[] args) {
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
            System.out.println(HELP_TEXT1);
            return;
        }

        String sourceDir = options[SOURCEDIR].value; // Utility.COMMON_DIRECTORY + "transforms/";
        String targetDir = options[DESTDIR].value; // Utility.GEN_DIRECTORY + "main/";
        String match = options[MATCH].value;
        skipComments = options[SKIP_COMMENTS].doesOccur;
        writeIndex = options[WRITE_INDEX].doesOccur;
        verbose = options[VERBOSE].doesOccur;
        approvedOnly = options[APPROVED_ONLY].doesOccur;

        try {
            if (writeIndex) {
                throw new InternalError("writeIndex not implemented.");
            } else {
                ElapsedTimer et = new ElapsedTimer();
                writeTransforms(sourceDir, match, targetDir + File.separator);
                System.out.println("ConvertTransforms: wrote " + fileCount +
                    " files in " + et);
            }
        } catch (IOException ex) {
            RuntimeException e = new RuntimeException();
            e.initCause(ex.getCause());
            throw e;
        } finally {
            System.out.println("DONE");
        }
    }

    // fixData ONLY NEEDED TO FIX FILE PROBLEM
    /*
     * private void fixData(String inputDirectory, String matchingPattern, String outputDirectory) throws IOException {
     * File dir = new File(inputDirectory);
     * File[] files = dir.listFiles();
     * for (int i = 0; i < files.length; ++i) {
     * if (files[i].isDirectory()) continue;
     * BufferedReader input = FileUtilities.openUTF8Reader("", files[i].getCanonicalPath());
     * PrintWriter output = FileUtilities.openUTF8Writer("", outputDirectory + files[i].getName());
     * while (true) {
     * String line = input.readLine();
     * if (line == null) break;
     * if (line.indexOf("DOCTYPE") >= 0) {
     * line = line.replaceAll(" ldml ", " supplementalData ");
     * }
     * output.println(line);
     * }
     * input.close();
     * output.close();
     * }
     * }
     */

}
