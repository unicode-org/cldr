/*
 * Copyright (C) 2004-2011, Unicode, Inc., Google, Inc., and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool.resolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.FilterFactory;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SimpleXMLSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Class designed for the resolution of CLDR XML Files (e.g., removing aliases
 * but leaving the inheritance structure intact).
 *
 * Instances of this class are not thread-safe. Any attempts to use an object of
 * this class in multiple threads must be externally synchronized.
 *
 * @author ryanmentley@google.com (Ryan Mentley), jchye@google.com (Jennifer Chye)
 *
 */
public class CldrResolver {
    /**
     * The name of the code-fallback locale
     */
    public static final String CODE_FALLBACK = "code-fallback";

    /**
     * The name of the root locale
     */
    public static final String ROOT = "root";

    /* The command-line options. */
    private static final Options options = new Options(
        "This program is used to convert CLDR XML files into their resolved versions.\n" +
            "Please refer to the following options. Options are not case sensitive.\n" +
            "\texample: org.unicode.cldr.tool.resolver.CldrResolver -s xxx -d yyy -l en")
                .add("locale", 'l', ".*", ".*", "The locales to generate resolved files for")
                .add("sourcedir", ".*", "Source directory for CLDR files")
                .add("destdir", ".*", "Destination directory for output files")
                .add("resolutiontype", 'r', "\\w+", "simple", "The resolution type to be used")
                .add("mindraftstatus", 'm', ".*", "unconfirmed", "The minimum draft status")
                .add("verbosity", 'v', "\\d", "2", "The verbosity level for comments during generation")
                .add("usealtvalues", 'a', null, null, "Use alternate values in FilterFactory for the locale data to be resolved.")
                .add("organization", 'o', ".*", null, "Filter by this organization's coverage level");

    /* Private instance variables */
    private Factory cldrFactory;
    private ResolutionType resolutionType;
    // Cache for resolved CLDRFiles.
    // This is most useful for simple resolution, where the resolved locales are
    // required to resolve their children.
    //private Map<String, CLDRFile> resolvedCache = new LruMap<String, CLDRFile>(5);

    /**
     * The initial size of the resolved cache
     */
    private final int INITIAL_RESOLVED_CACHE_SIZE = 10;
    private Cache<String, CLDRFile> resolvedCache = CacheBuilder.newBuilder().initialCapacity(INITIAL_RESOLVED_CACHE_SIZE).build();

    public static void main(String[] args) {
        options.parse(args, true);

        // Parse the options
        ResolutionType resolutionType = ResolutionType.SIMPLE;
        Option option = options.get("resolutiontype");
        if (option.doesOccur()) {
            try {
                resolutionType = ResolutionType.forString(option.getValue());
            } catch (IllegalArgumentException e) {
                ResolverUtils.debugPrintln("Warning: " + e.getMessage(), 1);
                ResolverUtils.debugPrintln("Using default resolution type " + resolutionType.toString(), 1);
            }
        }

        String srcDir = null;
        option = options.get("sourcedir");
        if (option.doesOccur()) {
            srcDir = option.getValue();
        } else {
            srcDir = CLDRPaths.MAIN_DIRECTORY;
        }

        option = options.get("destdir");
        File dest;
        if (option.doesOccur()) {
            dest = new File(option.getValue());
        } else {
            dest = new File(CLDRPaths.GEN_DIRECTORY, "resolver");
        }
        if (!dest.exists()) {
            dest.mkdir();
        }
        String destDir = dest.getAbsolutePath();

        int verbosityParsed = Integer.parseInt(options.get("verbosity").getValue());
        if (verbosityParsed < 0 || verbosityParsed > 5) {
            ResolverUtils.debugPrintln(
                "Warning: Verbosity must be between 0 and 5, inclusive.  Using default value "
                    + ResolverUtils.verbosity,
                1);
        } else {
            ResolverUtils.verbosity = verbosityParsed;
        }

        option = options.get("mindraftstatus");
        DraftStatus minDraftStatus = option.doesOccur() ? DraftStatus.forString(option.getValue()) : DraftStatus.unconfirmed;
        Factory factory = Factory.make(srcDir, ".*", minDraftStatus);
        boolean useAltValues = options.get("usealtvalues").doesOccur();
        String org = options.get("organization").getValue();
        if (useAltValues || org != null) {
            factory = FilterFactory.load(factory, org, useAltValues);
        }
        CldrResolver resolver = new CldrResolver(factory, resolutionType);

        // Perform the resolution
        String localeRegex = options.get("locale").getValue();
        resolver.resolve(localeRegex, destDir);
        ResolverUtils.debugPrintln("Execution complete.", 3);
    }

    /**
     * Constructs a CLDR partial resolver given the path to a directory of XML
     * files.
     *
     * @param factory the factory containing the files to be resolved
     * @param resolutionType the resolution type of the resolver.
     */
    public CldrResolver(Factory factory, ResolutionType resolutionType) {
        /*
         * We don't do the regex filter here so that we can still resolve parent
         * files that don't match the regex
         */
        cldrFactory = factory;
        this.resolutionType = resolutionType;
    }

    /**
     * Resolves all locales that match the given regular expression and outputs
     * their XML files to the given directory.
     *
     * @param localeRegex a regular expression that will be matched against the
     *        names of locales
     * @param outputDir the directory to which to output the partially-resolved
     *        XML files
     * @param resolutionType the type of resolution to perform
     * @throws IllegalArgumentException if outputDir is not a directory
     */
    public void resolve(String localeRegex, File outputDir) {
        if (!outputDir.isDirectory()) {
            throw new IllegalArgumentException(outputDir.getPath() + " is not a directory");
        }

        // Iterate through all the locales
        for (String locale : getLocaleNames(localeRegex)) {
            // Resolve the file
            ResolverUtils.debugPrintln("Processing locale " + locale + "...", 2);
            CLDRFile resolved = resolveLocale(locale);

            // Output the file to disk
            printToFile(resolved, outputDir);
        }
    }

    /**
     * Returns the locale names from the resolver that match a given regular
     * expression.
     *
     * @param localeRegex a regular expression to match against
     * @return all of the locales that will be resolved by a call to resolve()
     *         with the same localeRegex
     */
    public Set<String> getLocaleNames(String localeRegex) {
        ResolverUtils.debugPrint("Getting list of locales...", 3);
        Set<String> allLocales = cldrFactory.getAvailable();
        Set<String> locales = new TreeSet<String>();
        // Iterate through all the locales
        for (String locale : allLocales) {
            // Check if the locale name matches the regex
            if (locale.matches(localeRegex)) {
                locales.add(locale);
            } else {
                ResolverUtils.debugPrintln("Locale " + locale
                    + "does not match the pattern.  Skipping...\n", 4);
            }

        }
        ResolverUtils.debugPrintln("done.\n", 3);
        return locales;
    }

    /**
     * Resolves a locale to a {@link CLDRFile} object
     *
     * @param locale the name of the locale to resolve
     * @param resolutionType the type of resolution to perform
     * @return a {@link CLDRFile} containing the resolved data
     */
    public CLDRFile resolveLocale(String locale) {
        // Create CLDRFile for current (base) locale
        CLDRFile base = cldrFactory.make(locale, true);
        CLDRFile resolved = resolvedCache.getIfPresent(locale);
        if (resolved != null) return resolved;

        ResolverUtils.debugPrintln("Processing " + locale + "...", 2);
        resolved = resolveLocaleInternal(base, resolutionType);
        resolvedCache.put(locale, resolved);
        return resolved;
    }

    private CLDRFile resolveLocaleInternal(CLDRFile file, ResolutionType resolutionType) {
        String locale = file.getLocaleID();
        // Make parent files for simple resolution.
        List<CLDRFile> ancestors = new ArrayList<CLDRFile>();
        if (resolutionType == ResolutionType.SIMPLE && !locale.equals(ROOT)) {
            String parentLocale = locale;
            do {
                parentLocale = LocaleIDParser.getSimpleParent(parentLocale);
                ancestors.add(resolveLocale(parentLocale));
            } while (!parentLocale.equals(ROOT));
        }

        // Create empty file to hold (partially or fully) resolved data.
        CLDRFile resolved = new CLDRFile(new SimpleXMLSource(locale));

        // Go through the XPaths, filter out appropriate values based on the
        // inheritance model,
        // then copy to the new CLDRFile.
        Set<String> basePaths = ResolverUtils.getAllPaths(file);
        for (String distinguishedPath : basePaths) {
            ResolverUtils.debugPrintln("Distinguished path: " + distinguishedPath, 5);

            if (distinguishedPath.endsWith("/alias")) {
                // Ignore any aliases.
                ResolverUtils.debugPrintln("This path is an alias.  Dropping...", 5);
                continue;
            }

            /*
             * If we're fully resolving the locale (and, if code-fallback suppression
             * is enabled, if the value is not from code-fallback) or the values
             * aren't equal, add it to the resolved file.
             */
            if (resolutionType == ResolutionType.NO_CODE_FALLBACK && file.getSourceLocaleID(
                distinguishedPath, null).equals(CODE_FALLBACK)) {
                continue;
            }

            // For simple resolution, don't add paths to child locales if the parent
            // locale contains the same path with the same value.
            String baseValue = file.getStringValue(distinguishedPath);
            if (resolutionType == ResolutionType.SIMPLE) {
                String parentValue = null;
                for (CLDRFile ancestor : ancestors) {
                    parentValue = ancestor.getStringValue(distinguishedPath);
                    if (parentValue != null) break;
                }
                ResolverUtils.debugPrintln(
                    "    Parent value : " + ResolverUtils.strRep(parentValue), 5);
                if (areEqual(parentValue, baseValue)) continue;
            }

            ResolverUtils.debugPrintln("  Adding to resolved file.", 5);
            // Suppress non-distinguishing attributes in simple inheritance
            String path = resolutionType == ResolutionType.SIMPLE ? distinguishedPath : file.getFullXPath(distinguishedPath);
            ResolverUtils.debugPrintln("Path to be saved: " + path, 5);
            resolved.add(path, baseValue);
        }

        // Sanity check in simple resolution to make sure that all paths in the parent are also in the child.
        if (ancestors.size() > 0) {
            CLDRFile ancestor = ancestors.get(0);
            ResolverUtils.debugPrintln(
                "Adding UNDEFINED values based on ancestor: " + ancestor.getLocaleID(), 3);
            for (String distinguishedPath : ResolverUtils.getAllPaths(ancestor)) {
                // Do the comparison with distinguished paths to prevent errors
                // resulting from duplicate full paths but the same distinguished path
                if (!basePaths.contains(distinguishedPath) &&
                    !ancestor.getStringValue(distinguishedPath).equals(CldrUtility.NO_INHERITANCE_MARKER)) {
                    ResolverUtils.debugPrintln(
                        "Added UNDEFINED value for path: " + distinguishedPath, 4);
                    resolved.add(distinguishedPath, CldrUtility.NO_INHERITANCE_MARKER);
                }
            }
        }
        return resolved;
    }

    /**
     * Resolves all locales that match the given regular expression and outputs
     * their XML files to the given directory.
     *
     * @param localeRegex a regular expression that will be matched against the
     *        names of locales
     * @param outputDir the directory to which to output the partially-resolved
     *        XML files
     * @param resolutionType the type of resolution to perform
     * @throws IllegalArgumentException if outputDir is not a directory
     */
    public void resolve(String localeRegex, String outputDir) {
        resolve(localeRegex, new File(outputDir));
    }

    /**
     * Writes out the given CLDRFile in XML form to the given directory
     *
     * @param cldrFile the CLDRFile to print to XML
     * @param directory the directory to which to add the file
     */
    private static void printToFile(CLDRFile cldrFile, File directory) {
        ResolverUtils.debugPrint("Printing file...", 2);
        try {
            PrintWriter pw = new PrintWriter(new File(directory, cldrFile.getLocaleID() + ".xml"), "UTF-8");
            cldrFile.write(pw);
            pw.close();
            ResolverUtils.debugPrintln("done.\n", 2);
        } catch (FileNotFoundException e) {
            ResolverUtils.debugPrintln("\nFile not found: " + e.getMessage(), 1);
            System.exit(1);
            return;
        } catch (UnsupportedEncodingException e) {
            // This should never ever happen.
            ResolverUtils.debugPrintln("Your system does not support UTF-8 encoding: " + e.getMessage(),
                1);
            System.exit(1);
            return;
        }
    }

    /**
     * Convenience method to compare objects that works with nulls
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return true if objects o1 == o2 or o1.equals(o2); false otherwise
     */
    private static boolean areEqual(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else if (o1 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }
}
