package org.unicode.cldr.tool;

import static org.unicode.cldr.util.XMLSource.CODE_FALLBACK_ID;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.XMLSource.SourceLocation;

@CLDRTool(alias = "pathinfo", description = "Get information about paths such as inheritance")
public class PathInfo {
    static final Options options =
            new Options(PathInfo.class)
                    .add("locale", ".*", "und", "Locale ID for the path info")
                    .add("infile", ".*", null, "File to read paths from or '--infile=-' for stdin")
                    .add("nosource", "Don’t print the XML Source line")
                    .add("OutputDaip", ".*", "run a string through DAIP for output")
                    .add("InputDaip", ".*", "run a string through DAIP for input");

    public static void main(String args[]) throws IOException {
        final Set<String> paths = options.parse(args, true);

        final String inPath = options.get("infile").getValue();
        if (inPath != null) {
            if (inPath.equals("-")) {
                addLines(System.in, paths);
            } else {
                addLines(inPath, paths);
            }
        }

        if (paths.isEmpty()) {
            System.err.println("Error: No paths were specified.");
            System.out.println(options.getHelp());
            System.exit(1);
        }

        CLDRFile file = null;

        final String locale = options.get("locale").getValue();
        try {
            file = CLDRConfig.getInstance().getCLDRFile(locale, true);
        } catch (Throwable t) {
            System.err.println("Could not load locale " + locale + " due to " + t);
        }

        showPaths(paths, file);
        if (file == null) {
            System.err.println("To enable inheritance lookup, use a different path.");
        }
    }

    private static void showPaths(final Set<String> paths, CLDRFile file) {
        boolean prePopulated = false;
        boolean hadStringMisses = false;

        for (final String path : paths) {
            if (path.startsWith("//")) {
                showPath(path, file);
            } else {
                if (!prePopulated && file != null) {
                    // scan all possible XPaths
                    System.err.println("INFO: Scanning all possible xpaths for hex IDs...");
                    file.getExtraPaths().forEach(x -> StringId.getHexId(x));
                    file.fullIterable().forEach(x -> StringId.getHexId(x));
                    prePopulated = true;
                }
                final String xpath = StringId.getStringFromHexId(path);
                if (xpath == null) {
                    System.err.println(
                            "• ERROR: could not find hex id "
                                    + path
                                    + " - may not have been seen yet.");
                    hadStringMisses = true;
                }
                showPath(xpath, file);
            }
        }
        if (hadStringMisses) {
            System.err.println("ERROR: One or more XPaths could not be converted from hex form.");
            if (file == null) {
                System.err.println("Tip: Set a locale ID with -l when using hex ids");
            }
            System.exit(1);
        }
    }

    private static void showPath(final String path, CLDRFile file) {
        final boolean nosource = options.get("nosource").doesOccur();
        System.out.println("-------------------\n");
        System.out.println("• " + path);
        System.out.println("• " + StringId.getHexId(path));
        // TODO: PathHeader, etc.
        if (file == null) {
            return;
        }
        final String dPath = CLDRFile.getDistinguishingXPath(path, null);
        if (dPath != null && !dPath.equals(path)) {
            System.out.println("• Distinguishing: " + dPath);
            System.out.println("• Distinguishing:" + StringId.getHexId(dPath));
        }
        System.out.println("• Value: " + file.getStringValue(dPath));
        // File lookup
        if (!nosource) {
            SourceLocation source = file.getSourceLocation(path);
            if (source != null) {
                System.out.println(source + " XML Source Location");
            }
        }
        // Inheritance lookup
        final String fullPath = file.getFullXPath(path);
        if (!fullPath.equals(path)) {
            System.out.println("• Full path: " + fullPath);
            System.out.println("• Full path: " + StringId.getHexId(fullPath));
        }

        Option inputDaip = options.get("InputDaip");
        Option outputDaip = options.get("OutputDaip");
        if (inputDaip.doesOccur() || outputDaip.doesOccur()) {
            DisplayAndInputProcessor daip = new DisplayAndInputProcessor(file);
            if (inputDaip.doesOccur()) {
                String input = inputDaip.getValue();
                System.out.println("INPUT: " + input);
                Exception[] e = new Exception[1];
                final String raw = daip.processInput(path, input, e);
                System.out.println("RAW<<: " + raw);
                if (e.length > 0) {
                    for (final Exception ex : e) {
                        System.err.println(ex);
                    }
                }
            }
            if (outputDaip.doesOccur()) {
                String output = outputDaip.getValue();
                System.out.println("RAW   : " + output);
                final String disp = daip.processForDisplay(path, output);
                System.out.println("DISP>>: " + disp);
            }
            return; // skip inheritance chain
        }

        // inheritance
        Status status = new Status();
        if (false) {
            // For debugging: compare the below to calling getSourceLocaleIdExtended directly.
            final String xlocale = file.getSourceLocaleIdExtended(dPath, status, true, null);
            System.out.println("• SLIE = " + xlocale + ":" + status.pathWhereFound);
        }
        System.out.println("• Inheritance chain:");
        for (final LocaleInheritanceInfo e : file.getPathsWhereFound(dPath)) {
            System.out.print("  ");
            if (e.getReason().isTerminal()) {
                System.out.print("•"); // terminal
            } else {
                System.out.print("|"); // non-terminal
            }
            System.out.println(" " + e); // reason : locale + xpath
            if (e.getAttribute() != null) {
                System.out.println("    attribute=" + e.getAttribute());
            }
            if (e.getLocale() != null
                    && e.getPath() != null
                    && !e.getLocale().equals(CODE_FALLBACK_ID)) {
                final CLDRFile subFile = CLDRConfig.getInstance().getCLDRFile(e.getLocale(), false);
                SourceLocation subsource = subFile.getSourceLocation(e.getPath());
                if (subsource != null && !nosource) {
                    System.out.println("    " + subsource + " XML Source");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addLines(final InputStream in, Set<String> set) throws IOException {
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            With.in(new FileReaders.ReadLineSimpleIterator(br))
                    .forEach(
                            str -> {
                                if (!str.isBlank()) {
                                    set.add(str.trim());
                                }
                            });
        }
    }

    private static void addLines(final String path, Set<String> set) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            addLines(fis, set);
        }
    }
}
