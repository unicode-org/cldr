package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.SearchXml.ConfigOption;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.BaseUrl;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;

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
    // + "\t Remember to put .* on the front and back of any regex if you want to find any
    // occurence."
    // + "-s\t show path"
    // + "-s\t show parent value"
    // + "-s\t show English value"
    // ;

    enum PathStyle {
        none,
        path,
        fullPath,
        pathHeader,
        modify_config
    }

    static final Options myOptions =
            new Options()
                    .add("source", ".*", CLDRPaths.MAIN_DIRECTORY, "source directory")
                    .add("file", ".*", ".*", "regex to filter files/locales.")
                    .add(
                            "path",
                            ".*",
                            null,
                            "regex to filter paths. ! in front selects items that don't match; §§ to separate multiple tests. Example: -p relative.*@type=\\\"-?3\\\"")
                    .add(
                            "value",
                            ".*",
                            null,
                            "regex to filter values. ! in front selects items that don't match")
                    .add(
                            "level",
                            ".*",
                            null,
                            "regex to filter levels. ! in front selects items that don't match")
                    .add("count", null, null, "only count items")
                    .add("organization", ".*", null, "show level for organization")
                    .add("z-showPath", null, null, "show paths")
                    .add("resolved", null, null, "use resolved locales")
                    .add("q-showParent", null, null, "show parent value")
                    .add("english", null, null, "show english value")
                    .add("Verbose", null, null, "verbose output")
                    .add(
                            "PathStyle",
                            Joiner.on('|').join(PathStyle.values()),
                            "path",
                            "show path header")
                    .add("SurveyTool", null, null, "show Survey Tool URL")
                    .add(
                            "diff",
                            "\\d+(\\.\\d+)?",
                            null,
                            "show only paths whose values changed from specified version (and were present in that version)")
                    .add(
                            "Error",
                            ".*",
                            null,
                            "filter by errors, eg CheckForCopy, or CheckForCopy:sameAsCode");

    private static String fileMatcher;
    private static MatcherList pathMatcher;
    private static boolean countOnly;
    private static boolean showPath;
    private static boolean showSurveyToolUrl;
    private static Subtype subtype;
    private static CheckCLDR checkCldr;

    static PathHeader.Factory pathHeaderFactory = PathHeader.getFactory();

    private static Organization organization;

    private static PathStyle showPathHeader;

    public static void main(String[] args) {
        myOptions.parse(args, true);

        long startTime = System.currentTimeMillis();

        String sourceDirectory = myOptions.get("source").getValue();

        Output<Boolean> exclude = new Output<>();
        fileMatcher = myOptions.get("file").getValue();

        pathMatcher = MatcherList.from(myOptions.get("path").getValue());

        Set<Level> levelMatcher = getEnumMatcher(myOptions.get("level").getValue(), exclude);

        Matcher valueMatcher = getMatcher(myOptions.get("value").getValue(), exclude);
        Boolean valueExclude = exclude.value;

        countOnly = myOptions.get("count").doesOccur();
        boolean resolved = myOptions.get("resolved").doesOccur();

        showPath = myOptions.get("z-showPath").doesOccur();
        String orgString = myOptions.get("organization").getValue();
        Organization organization = orgString == null ? null : Organization.fromString(orgString);

        final CLDRFile english = CLDRConfig.getInstance().getEnglish();

        showPathHeader = PathStyle.valueOf(myOptions.get("PathStyle").getValue());

        showSurveyToolUrl = myOptions.get("SurveyTool").doesOccur();

        boolean showParent = myOptions.get("q-showParent").doesOccur();

        boolean showEnglish = myOptions.get("english").doesOccur();

        File[] paths = {new File(CLDRPaths.MAIN_DIRECTORY), new File(sourceDirectory)};

        Factory cldrFactory = SimpleFactory.make(paths, fileMatcher);

        Set<String> locales = new TreeSet<>(cldrFactory.getAvailable());

        String rawVersion = myOptions.get("diff").getValue();

        Factory cldrDiffFactory = null;
        if (rawVersion != null) {
            String base = getArchiveDirectory(VersionInfo.getInstance(rawVersion));
            File[] files = getCorrespondingDirectories(base, cldrFactory);
            cldrDiffFactory = SimpleFactory.make(files, ".*");
        }

        String errorMatcherText = myOptions.get("Error").getValue();
        subtype = null;
        checkCldr = null;
        if (errorMatcherText != null) {
            int errorSepPos = errorMatcherText.indexOf(':');
            if (errorSepPos >= 0) {
                subtype = Subtype.valueOf(errorMatcherText.substring(errorSepPos + 1));
                errorMatcherText = errorMatcherText.substring(0, errorSepPos);
            }
            // TODO create new object using name
            // checkCldr = new CheckForCopy(cldrFactory);
            try {
                Class<?> checkCLDRClass =
                        Class.forName("org.unicode.cldr.test." + errorMatcherText);
                Constructor<?> ctor = checkCLDRClass.getConstructor(Factory.class);
                checkCldr = (CheckCLDR) ctor.newInstance(cldrFactory);
            } catch (Exception e) {
                throw new ICUUncheckedIOException(e);
            }
            CheckCLDR.setDisplayInformation(english);
        }

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

        if (organization != null) {
            locales =
                    Sets.intersection(
                            locales, StandardCodes.make().getLocaleCoverageLocales(organization));
        }

        List<CheckStatus> result = new ArrayList<>();
        Map<String, String> options = new HashMap<>();

        int totalCount = 0;

        for (String locale : locales) {
            int localeCount = 0;
            //            Level organizationLevel = organization == null ? null
            //                : StandardCodes.make().getLocaleCoverageLevel(organization, locale);

            CLDRFile file = cldrFactory.make(locale, resolved);
            CLDRFile resolvedFile =
                    resolved == true ? file : (CLDRFile) cldrFactory.make(locale, true);
            CLDRFile diffFile = null;

            if (checkCldr != null) {
                if (locale.equals("eo")) {
                    int debug = 0;
                }
                result.clear();
                checkCldr.setCldrFileToCheck(resolvedFile, options, result);
            }

            if (cldrDiffFactory != null) {
                try {
                    diffFile = cldrDiffFactory.make(locale, resolved);
                } catch (Exception e) {
                    continue; // no old file, so skip
                }
            }

            Counter<Level> levelCounter = new Counter<>();
            // CLDRFile parent = null;
            boolean headerShown = false;

            // System.out.println("*Checking " + locale);
            CoverageLevel2 level = null;
            Level pathLevel = null;

            level = CoverageLevel2.getInstance(locale);
            Status status = new Status();
            Set<PathHeader> sorted = new TreeSet<>();
            for (String path : file.fullIterable()) {
                if (locale.equals("eo") && path.contains("type=\"MK\"")) {
                    int debug = 0;
                }
                String stringValue = file.getStringValue(path);
                if (stringValue == null) {
                    continue;
                }
                String diffStringValue;
                if (diffFile != null) {
                    diffStringValue = diffFile.getWinningValueWithBailey(path);
                    if (diffStringValue == null) {
                        continue;
                    }
                    String stringValueWithBailey = resolvedFile.getWinningValueWithBailey(path);
                    if (stringValueWithBailey == null) {
                        continue; // strange results; shouldn't have ^^^ with null
                    }
                    if (diffStringValue.equals(stringValueWithBailey)) {
                        continue;
                    }
                    stringValueWithBailey = resolvedFile.getWinningValueWithBailey(path);
                    int debug = 0;
                }
                sorted.add(pathHeaderFactory.fromPath(path));
            }
            for (PathHeader pathHeader : sorted) {
                String path = pathHeader.getOriginalPath();
                String fullPath = file.getFullXPath(path);
                String value = file.getStringValue(path);
                if (locale.equals("eo") && path.contains("type=\"MK\"")) {
                    int debug = 0;
                }

                if (pathMatcher != null && !pathMatcher.find(fullPath)) {
                    continue;
                }

                {
                    pathLevel = level.getLevel(path);
                    levelCounter.add(pathLevel, 1);
                }

                if (levelMatcher != null && !levelMatcher.contains(pathLevel)) {
                    continue;
                }

                if (valueMatcher != null && valueExclude == valueMatcher.reset(value).find()) {
                    continue;
                }

                if (checkCldr != null) {
                    if (checkCldr.isSkipTest()) {
                        continue;
                    }
                    result.clear();

                    checkCldr.check(path, file.getFullXPath(path), value, options, result);
                    if (result.isEmpty()) {
                        continue;
                    }
                    int count = 0;
                    for (CheckStatus item : result) {
                        if (item.getSubtype() == subtype) {
                            ++count;
                        }
                    }
                    if (count == 0) {
                        continue;
                    }
                    // for debugging
                    int debug = 0;
                    checkCldr.check(path, file.getFullXPath(path), value, options, result);
                }

                // made it through the sieve

                if (countOnly) {
                    continue;
                }
                if (!headerShown) {
                    if (showPathHeader == PathStyle.modify_config) {
                        System.out.println();
                    } else {
                        showLine(
                                showPathHeader,
                                showParent,
                                showEnglish,
                                resolved,
                                locale,
                                "Path",
                                "Full-Path",
                                "Value",
                                "Parent-Value",
                                "English-Value",
                                "Source-Locale\tSource-Path",
                                "Org-Level");
                    }
                    headerShown = true;
                }
                //                if (showParent && parent == null) {
                //                    String parentLocale = LocaleIDParser.getParent(locale);
                //                    parent = cldrFactory.make(parentLocale, true);
                //                }
                // String shortPath = pretty.getPrettyPath(path);
                // String cleanShort = pretty.getOutputForm(shortPath);
                String cleanShort = pathHeader.toString().replace('\t', '|');
                final String resolvedSource =
                        !resolved
                                ? null
                                : file.getSourceLocaleID(path, status)
                                        + (path.equals(status.pathWhereFound)
                                                ? "\t≣"
                                                : "\t" + status);
                if (checkCldr != null) {
                    SearchXml.show(
                            ConfigOption.delete,
                            "",
                            locale,
                            CLDRFile.getDistinguishingXPath(fullPath, null),
                            value,
                            null,
                            null,
                            null);
                } else {
                    showLine(
                            showPathHeader,
                            showParent,
                            showEnglish,
                            resolved,
                            locale,
                            path,
                            fullPath,
                            value,
                            !showParent ? null : english.getBaileyValue(path, null, null),
                            english == null ? null : english.getStringValue(path),
                            resolvedSource,
                            Objects.toString(pathLevel));
                }
                totalCount++;
                localeCount++;
            }
            if (countOnly) {
                System.out.print(locale);
                for (Level cLevel : Level.values()) {
                    System.out.print("\t" + levelCounter.get(cLevel));
                }
            }
            if (localeCount != 0 && showPathHeader != PathStyle.modify_config) {
                System.out.println("# " + locale + " Total " + localeCount + " found");
            }
            System.out.flush();
        }
        System.out.println(
                "# All Total "
                        + totalCount
                        + " found\n"
                        + "Done -- Elapsed time: "
                        + ((System.currentTimeMillis() - startTime) / 60000.0)
                        + " minutes");
    }

    private static File[] getCorrespondingDirectories(String base, Factory cldrFactory) {
        File[] sourceDirs = cldrFactory.getSourceDirectories();
        File[] newDirs = new File[sourceDirs.length];
        int item = 0;
        for (File s : sourceDirs) {
            String path = PathUtilities.getNormalizedPathString(s);
            int baseLoc = path.lastIndexOf("/cldr/");
            if (baseLoc < 0) {
                throw new ICUUncheckedIOException("source doesn't contain /cldr/");
            }
            newDirs[item++] = new File(base, path.substring(baseLoc + 5));
        }
        return newDirs;
    }

    private static String getArchiveDirectory(VersionInfo versionInfo) {
        return CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + versionInfo.getVersionString(2, 3) + "/";
    }

    private static void showLine(
            PathStyle showPath,
            boolean showParent,
            boolean showEnglish,
            boolean resolved,
            String locale,
            String path,
            String fullPath,
            String value,
            String parentValue,
            String englishValue,
            String resolvedSource,
            String organizationLevel) {
        String pathHeaderInfo = "";

        if (showPath == PathStyle.modify_config) {
            // locale=en ; action=add ;
            // new_path=//ldml/localeDisplayNames/territories/territory[@type="PS"][@alt="short"] ;
            // new_value=Palestine

            System.out.println(
                    "locale="
                            + locale
                            + " ; action=add"
                            + " ; new_path="
                            + fullPath
                            + " ; new_value="
                            + value);
            return;
        }
        PathHeader pathHeader = pathHeaderFactory.fromPath(path);

        if (showSurveyToolUrl) {
            pathHeaderInfo = "\n\t" + pathHeader.getUrl(BaseUrl.PRODUCTION, locale);
        }
        System.out.println(
                "#\t"
                        + locale
                        + "\t⟪"
                        + value
                        + "⟫"
                        + (showEnglish ? "\t⟪" + englishValue + "⟫" : "")
                        + (!showParent
                                ? ""
                                : Objects.equals(value, parentValue)
                                        ? "\t≣"
                                        : "\t⟪" + parentValue + "⟫")
                        + (showPath == PathStyle.path
                                ? "\t" + path
                                : showPath == PathStyle.fullPath
                                        ? "\t" + fullPath
                                        : showPath == PathStyle.pathHeader ? "\t" + pathHeader : "")
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
