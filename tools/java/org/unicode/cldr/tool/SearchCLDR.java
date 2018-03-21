package org.unicode.cldr.tool;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.BaseUrl;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.Output;

public class SearchCLDR {
    // private static final int
    // HELP1 = 0,
    // HELP2 = 1,
    // SOURCEDIR = 2,
    // MATCH_FILE = 3,
    // MATCH_PATH = 4,
    // MATCH_VALUE = 5,
    // SHOW_PATH = 6,
    // SHOW_PARENT_VALUE = 7,
    // SHOW_ENGLISH_VALUE = 8
    // ;
    // private static final UOption[] options = {
    // UOption.HELP_H(),
    // UOption.HELP_QUESTION_MARK(),
    // UOption.SOURCEDIR().setDefault(CldrUtility.MAIN_DIRECTORY),
    // UOption.create("localematch", 'l', UOption.REQUIRES_ARG).setDefault(".*"),
    // UOption.create("pathmatch", 'p', UOption.REQUIRES_ARG).setDefault(".*"),
    // UOption.create("valuematch", 'v', UOption.REQUIRES_ARG).setDefault(".*"),
    // UOption.create("showPath", 'z', UOption.NO_ARG),
    // UOption.create("showParentValue", 'q', UOption.NO_ARG),
    // UOption.create("showEnglishValue", 'e', UOption.NO_ARG),
    // };
    // static final String HELP_TEXT1 = "Use the following options" + XPathParts.NEWLINE
    // + "-h or -?\t for this message" + XPathParts.NEWLINE
    // + "-"+options[SOURCEDIR].shortName + "\t source directory. Default = -s" +
    // CldrUtility.getCanonicalName(CldrUtility.MAIN_DIRECTORY) + XPathParts.NEWLINE
    // + "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
    // + "-l<regex>\t to restrict the locales to what matches <regex>" + XPathParts.NEWLINE
    // + "-p<regex>\t to restrict the paths to what matches <regex>" + XPathParts.NEWLINE
    // + "-v<regex>\t to restrict the values to what matches <regex>" + XPathParts.NEWLINE
    // + "\t Remember to put .* on the front and back of any regex if you want to find any occurence."
    // + "-s\t show path"
    // + "-s\t show parent value"
    // + "-s\t show English value"
    // ;

    final static Options myOptions = new Options()
        .add("source", ".*", CLDRPaths.MAIN_DIRECTORY, "source directory")
        .add("file", ".*", ".*", "regex to filter files/locales.")
        .add("path", ".*", null, "regex to filter paths. ! in front selects items that don't match. example: -p relative.*@type=\\\"-?3\\\"")
        .add("value", ".*", null, "regex to filter values. ! in front selects items that don't match")
        .add("level", ".*", null, "regex to filter levels. ! in front selects items that don't match")
        .add("count", null, null, "only count items")
        .add("organization", ".*", null, "show level for organization")
        .add("z-showPath", null, null, "show paths")
        .add("resolved", null, null, "use resolved locales")
        .add("q-showParent", null, null, "show parent value")
        .add("english", null, null, "show english value")
        .add("Verbose", null, null, "verbose output")
        .add("PathHeader", null, null, "show path header and string ID");

    private static String fileMatcher;
    private static Matcher pathMatcher;
    private static boolean countOnly;
    private static boolean showPath;
    private static PathHeader.Factory PATH_HEADER_FACTORY = null;

    private static String organization;

    public static void main(String[] args) {
        myOptions.parse(args, true);
        // System.out.println("Arguments: " + CollectionUtilities.join(args, " "));

        long startTime = System.currentTimeMillis();

        String sourceDirectory = myOptions.get("source").getValue();

        Output<Boolean> exclude = new Output<Boolean>();
        fileMatcher = myOptions.get("file").getValue();

        pathMatcher = getMatcher(myOptions.get("path").getValue(), exclude);
        Boolean pathExclude = exclude.value;

        Set<Level> levelMatcher = getEnumMatcher(myOptions.get("level").getValue(), exclude);

        Matcher valueMatcher = getMatcher(myOptions.get("value").getValue(), exclude);
        Boolean valueExclude = exclude.value;

        countOnly = myOptions.get("count").doesOccur();
        boolean resolved = myOptions.get("resolved").doesOccur();

        showPath = myOptions.get("z-showPath").doesOccur();
        organization = myOptions.get("organization").getValue();

        if (myOptions.get("PathHeader").doesOccur()) {
            PATH_HEADER_FACTORY = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());
        }

        boolean showParent = myOptions.get("q-showParent").doesOccur();

        boolean showEnglish = myOptions.get("english").doesOccur();

        Factory cldrFactory = Factory.make(sourceDirectory, fileMatcher);
        Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());

        CLDRFile english = cldrFactory.make("en", true);
        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);

        System.out.println("Searching...");
        System.out.println();
        System.out.flush();
        // PrettyPath pretty = new PrettyPath();

        if (countOnly) {
            System.out.print("file");
            for (Level cLevel : Level.values()) {
                System.out.print("\t" + cLevel);
            }
            System.out.println();
        }

        for (String locale : locales) {
            Level organizationLevel = organization == null ? null
                : StandardCodes.make().getLocaleCoverageLevel(organization, locale);

            CLDRFile file = (CLDRFile) cldrFactory.make(locale, resolved);

            Counter<Level> levelCounter = new Counter<Level>();
            //CLDRFile parent = null;
            boolean headerShown = false;

            // System.out.println("*Checking " + locale);
            CoverageLevel2 level = null;
            Level pathLevel = null;

            level = CoverageLevel2.getInstance(locale);
            Status status = new Status();
            Set<PathHeader> sorted = new TreeSet<PathHeader>();
            for (String path : file.fullIterable()) {
                if (file.getStringValue(path) == null) {
                    continue;
                }
                sorted.add(pathHeaderFactory.fromPath(path));
            }
            for (PathHeader pathHeader : sorted) {
                String path = pathHeader.getOriginalPath();
                String fullPath = file.getFullXPath(path);
                String value = file.getStringValue(path);

                if (pathMatcher != null && pathExclude == pathMatcher.reset(fullPath).find()) {
                    continue;
                }

                {
                    pathLevel = level.getLevel(path);
                    levelCounter.add(pathLevel, 1);
                }

                if (!levelMatcher.contains(pathLevel)) {
                    continue;
                }

                if (valueMatcher != null && valueExclude == valueMatcher.reset(value).find()) {
                    continue;
                }

                // made it through the sieve

                if (countOnly) {
                    continue;
                }
                if (!headerShown) {
                    showLine(showPath, showParent, showEnglish, resolved, locale, "Path", "Full-Path", "Value",
                        "PathHeader", "Parent-Value", "English-Value", "Source-Locale\tSource-Path", "Org-Level");
                    headerShown = true;
                }
                //                if (showParent && parent == null) {
                //                    String parentLocale = LocaleIDParser.getParent(locale);
                //                    parent = cldrFactory.make(parentLocale, true);
                //                }
                // String shortPath = pretty.getPrettyPath(path);
                // String cleanShort = pretty.getOutputForm(shortPath);
                String cleanShort = pathHeader.toString().replace('\t', '|');
                final String resolvedSource = !resolved ? null
                    : file.getSourceLocaleID(path, status)
                        + (path.equals(status.pathWhereFound) ? "\t≣" : "\t" + status);
                showLine(showPath, showParent, showEnglish, resolved, locale,
                    path, fullPath, value,
                    cleanShort,
                    !showParent ? null : english.getBaileyValue(path, null, null),
                    english == null ? null : english.getStringValue(path),
                    resolvedSource,
                    Objects.toString(pathLevel));
            }
            if (countOnly) {
                System.out.print(locale);
                for (Level cLevel : Level.values()) {
                    System.out.print("\t" + levelCounter.get(cLevel));
                }
                System.out.println();
            }
            System.out.flush();
        }
        System.out
            .println("Done -- Elapsed time: " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
    }

    private static void showLine(boolean showPath, boolean showParent, boolean showEnglish,
        boolean resolved, String locale, String path, String fullPath, String value,
        String shortPath, String parentValue, String englishValue, String resolvedSource, String organizationLevel) {
        String pathHeaderInfo = "";
        if (PATH_HEADER_FACTORY != null) {
            PathHeader pathHeader = PATH_HEADER_FACTORY.fromPath(path);
            if (pathHeader != null) {
                pathHeaderInfo = "\n\t" + pathHeader
                    + "\n\t" + pathHeader.getUrl(BaseUrl.PRODUCTION, locale);
            }
        }
        System.out.println(
            locale + "\t⟪" + value + "⟫"
                + (showEnglish ? "\t⟪" + englishValue + "⟫" : "")
                + (!showParent ? "" : CollectionUtilities.equals(value, parentValue) ? "\t≣" : "\t⟪" + parentValue + "⟫")
                + "\t" + shortPath
                + (showPath ? "\t" + fullPath : "")
                + (resolved ? "\t" + resolvedSource : "")
                + (organizationLevel != null ? "\t" + organizationLevel : "")
                + pathHeaderInfo);
    }

    private static Matcher getMatcher(String property, Output<Boolean> exclude) {
        exclude.value = false;
        if (property == null) {
            return null;
        }
        if (property.startsWith("!")) {
            exclude.value = true;
            property = property.substring(1);
        }
        return PatternCache.get(property).matcher("");
    }

    private static Set<Level> getEnumMatcher(String property, Output<Boolean> exclude) {
        exclude.value = false;
        if (property == null) {
            return null;
        }
        if (property.startsWith("!")) {
            exclude.value = true;
            property = property.substring(1);
        }
        EnumSet<Level> result = EnumSet.noneOf(Level.class);
        Matcher matcher = Pattern.compile(property, Pattern.CASE_INSENSITIVE).matcher("");

        for (Level level : Level.values()) {
            if (matcher.reset(level.toString()).matches() != exclude.value) {
                result.add(level);
            }
        }
        return ImmutableSet.copyOf(result);
    }
}