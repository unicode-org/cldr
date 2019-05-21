package org.unicode.cldr.util;

import java.io.BufferedReader;

import org.unicode.cldr.util.CldrUtility.VariableReplacer;

/**
 * Parses a file of regexes for use in RegexLookup or any other class that requires
 * a list of regexes.
 *
 * @author jchye
 */
public class RegexFileParser {
    private RegexLineParser lineParser;
    private VariableProcessor varProcessor = DEFAULT_VARIABLE_PROCESSOR;

    /**
     * Parses a line given to it by the RegexFileParser.
     */
    public interface RegexLineParser {
        public void parse(String line);
    }

    /**
     * Stores variables given to it by the RegexFileParser and performs replacements
     * if needed.
     */
    public interface VariableProcessor {
        public void add(String variable, String variableName);

        public String replace(String str);
    }

    private static final VariableProcessor DEFAULT_VARIABLE_PROCESSOR = new VariableProcessor() {
        VariableReplacer variables = new VariableReplacer();

        @Override
        public void add(String variableName, String value) {
            variables.add(variableName, value);
        }

        @Override
        public String replace(String str) {
            return variables.replace(str);
        }
    };

    public void setLineParser(RegexLineParser lineParser) {
        this.lineParser = lineParser;
    }

    public void setVariableProcessor(VariableProcessor varProcessor) {
        this.varProcessor = varProcessor;
    }

    /**
     * Parses the specified text file.
     *
     * @param a
     *            class relative to filename
     * @param filename
     *            the name of the text file to be parsed
     */
    public void parse(Class<?> baseClass, String filename) {
        BufferedReader reader = FileReaders.openFile(baseClass, filename);
        String line = null;
        int lineNumber = 0;
        try {
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                // Skip comments.
                if (line.length() == 0 || line.startsWith("#")) continue;
                // Read variables.
                if (line.charAt(0) == '%') {
                    int pos = line.indexOf("=");
                    if (pos < 0) {
                        throw new IllegalArgumentException("Failed to read variable in " + filename + "\t\t("
                            + lineNumber + ") " + line);
                    }
                    String varName = line.substring(0, pos).trim();
                    String varValue = line.substring(pos + 1).trim();
                    varProcessor.add(varName, varValue);
                    continue;
                }
                if (line.contains("%")) {
                    line = varProcessor.replace(line);
                }
                // Process a line in the input file for xpath conversion.
                lineParser.parse(line);
            }
        } catch (Exception e) {
            System.err.println("Error reading " + filename + " at line " + lineNumber + ": " + line);
            e.printStackTrace();
        }
    }
}