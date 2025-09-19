package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathDescriptionParser {
    private static final String PROLOGUE = "PROLOGUE";
    private static final String VARIABLES = "VARIABLES";
    private static final String REFERENCES = "References";
    private final RegexLookup<Pair<String, String>> lookup =
            new RegexLookup<>(RegexLookup.LookupType.OPTIMIZED_DIRECTORY_PATTERN_LOOKUP);

    // running instance variables
    private String section = ""; // ## title
    private String description = null; // ### title
    private final List<String> patterns = new ArrayList<>();
    private final List<String> text = new ArrayList<>();
    private final List<String> referenceLines = new ArrayList<>();

    public RegexLookup<Pair<String, String>> parse(String fileName) {
        String string = PathDescription.getBigString(fileName);
        final String[] lines = string.split("\n");
        int n = 0;

        try {
            for (final String line : lines) {
                n++;
                processLine(line.trim());
            }
            end(); // process last lines
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Failed parsing " + fileName + ":" + n + ": at " + section + "#" + description,
                    t);
        }

        return lookup;
    }

    /**
     * @return true if we're in the top section
     */
    private boolean inPrologue() {
        return (section.equals(PROLOGUE));
    }

    private boolean inVariables() {
        return (section.equals(VARIABLES));
    }

    private boolean inReferences() {
        return (section.equals(REFERENCES));
    }

    private void processLine(final String line) {
        if (line.startsWith("#")) {
            processHeader(line);
        } else if (inPrologue() || beforePrologue()) {
            return; // skip all other lines until new header
        } else if (inVariables()) {
            processVariable(line);
        } else if (inReferences()) {
            processReference(line);
        } else if (line.startsWith("- `")) {
            if (!line.endsWith("`")) {
                throw new IllegalArgumentException("Bad regex line " + line);
            }
            processRegex(line.substring(3, line.length() - 1));
        } else {
            processBody(line);
        }
    }

    private void processVariable(String line) {
        if (line.isBlank()) {
            return;
        }
        int pos = line.indexOf("=");
        if (pos < 0) {
            throw new IllegalArgumentException("Failed to read variable in " + line);
        }
        String varName = line.substring(0, pos).trim();
        String varValue = line.substring(pos + 1).trim();
        lookup.addVariable(varName, varValue);
    }

    private void processBody(String line) {
        text.add(line);
    }

    private void processRegex(String line) {
        Pattern.compile(line); // make sure it compiles
        patterns.add(line);
    }

    private void processReference(String line) {
        referenceLines.add(line);
    }

    /* return the whole reference section as a string */
    public String getReferences() {
        return String.join("\n", referenceLines).trim();
    }

    private final Pattern HEADER_PATTERN = Pattern.compile("^(#+)(?:[ ]+(.*))?$");
    private final Pattern VARIABLES_PATTERN = Pattern.compile("^# *VARIABLES *$");

    private void processHeader(String line) {
        endHeader(); // process previous content and reset
        if (inReferences()) {
            throw new IllegalArgumentException(
                    "Disallowed: headers after start of # " + REFERENCES);
        }

        final Matcher m = HEADER_PATTERN.matcher(line);
        if (!m.matches()) throw new IllegalArgumentException("Malformed header " + line);
        final int headerLevel = m.group(1).length(); // number of #####
        String title = m.group(2);
        if (title == null) title = "";
        title = title.trim();
        if (headerLevel == 1) {
            if (VARIABLES_PATTERN.matcher(line).matches()) {
                if (beforePrologue()) {
                    throw new IllegalArgumentException("Prologue should precede # " + VARIABLES);
                }
                section = VARIABLES;
            } else {
                if (!beforePrologue()) {
                    throw new IllegalArgumentException("Extra # header after beginning of file");
                }
                section = PROLOGUE;
            }
            description = null;
        } else if (headerLevel == 2) { // ##
            if (beforePrologue()) {
                throw new IllegalArgumentException("Expected header # line");
            }
            if (title.isBlank()) {
                throw new IllegalArgumentException("Sections ## may not be empty.");
            }
            section = title;
            description = null;
        } else if (headerLevel == 3) { // ###
            // retain section
            description = title;
        } else {
            throw new IllegalArgumentException(
                    "Unexpected/unsupported header "
                            + m.group(1)
                            + " (expected ## or ###) "
                            + line);
        }
    }

    private void end() {
        if (!inReferences()) {
            throw new IllegalArgumentException("End of lines when not in # " + REFERENCES);
        }
        // no need to call endHeader here as there isn't anything to terminate in references.
    }

    /** process the end of the previous section */
    private void endHeader() {
        if (inPrologue() || beforePrologue()) return; // nothing to do for prologue

        processContent();
        resetContent();
    }

    private boolean beforePrologue() {
        return section.isEmpty();
    }

    private void processContent() {
        final String textStr = String.join("\n", text).trim();
        if (description == null) {
            // have not had a ### section yet.
            if (!patterns.isEmpty() || !textStr.isBlank()) {
                throw new IllegalArgumentException("Didn't expect content above this line.");
            }
            return;
        }
        // get some bad cases out of the way
        if (patterns.isEmpty() && !textStr.isBlank()) {
            throw new IllegalArgumentException("Content, but no regex patterns.");
        } else if (!patterns.isEmpty() && textStr.isBlank()) {
            throw new IllegalArgumentException("Regex patterns, but no content");
        } else if (patterns.isEmpty() && textStr.isBlank()) {
            throw new IllegalArgumentException("No content nor regex patterns");
        } else if (patterns.size() > 1) {
            throw new IllegalArgumentException("Only one pattern is supported at present.");
        }
        lookup.add(patterns.get(0), Pair.of(description, textStr));
    }

    private void resetContent() {
        patterns.clear();
        text.clear();
    }
}
