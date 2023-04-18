package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.XMLSource.SourceLocation;
import static org.unicode.cldr.util.XMLSource.CODE_FALLBACK_ID;

@CLDRTool(alias = "pathinfo", description = "Get information about paths such as inheritance")
public class PathInfo {
    final static Options options = new Options(PathInfo.class)
    .add("locale", ".*", "und", "Locale ID for the path info")
    .add("infile", ".*", null, "File to read paths from or '--infile=-' for stdin");

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

        for (final String path : paths ) {
            if (path.startsWith("//")) {
                showPath(path, file);
            } else {
                if (!prePopulated && file != null) {
                    // scan all possible XPaths
                    System.err.println("INFO: Scanning all possible xpaths for hex IDs...");
                    // TODO: include fullpath, etc etc...
                    file.fullIterable().forEach(x -> StringId.getHexId(x));
                    prePopulated = true;
                }
                final String xpath = StringId.getStringFromHexId(path);
                if (xpath == null) {
                    System.err.println("• ERROR: could not find hex id " + path + " - may not have been seen yet.");
                    hadStringMisses = true;
                }
                showPath(xpath, file);
            }
        }
        if (hadStringMisses) {
            System.err.println("ERROR: One or more XPaths could not be converted from hex form.");
            if(file == null) {
                System.err.println("Tip: Set a locale ID with -l when using hex ids");
            }
            System.exit(1);
        }
    }

    private static void showPath(final String path, CLDRFile file) {
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
        SourceLocation source = file.getSourceLocation(path);
        if (source != null) {
            System.out.println(source + " XML Source Location");
        }
        // Inheritance lookup
        final String fullPath = file.getFullXPath(path);
        if (!fullPath.equals(path)) {
            System.out.println("• Full path: " + fullPath);
            System.out.println("• Full path: " + StringId.getHexId(fullPath));
        }

        Status status = new Status();
        if (false) {
            // For debugging: compare the below to calling getSourceLocaleIdExtended directly.
            final String xlocale = file.getSourceLocaleIdExtended(dPath, status, true, null);
            System.out.println("• SLIE = " + xlocale+":"+status.pathWhereFound);
        }
        System.out.println("• Inheritance chain:");
        for (final LocaleInheritanceInfo e : file.getPathsWhereFound(dPath)) {
            System.out.println("\n  • " + e); // reason : locale + xpath
            if (e.getLocale() != null && e.getPath() != null && !e.getLocale().equals(CODE_FALLBACK_ID)) {
                final CLDRFile subFile = CLDRConfig.getInstance().getCLDRFile(e.getLocale(), false);
                SourceLocation subsource = subFile.getSourceLocation(e.getPath());
                if (subsource != null) {
                    System.out.println("    " +subsource + " XML Source");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addLines(final InputStream in, Set<String> set) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            With.in(new FileReaders.ReadLineSimpleIterator(br))
            .forEach(str -> {
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
