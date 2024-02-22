package org.unicode.cldr.test;

import com.ibm.icu.dev.tool.shared.UOption;
import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CompoundCheckCLDR;
import org.unicode.cldr.test.CheckCLDR.FormatDemo;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.SimpleDemo;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.tool.ShowData;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.CandidateInfo;
import org.unicode.cldr.util.VoteResolver.UnknownVoterException;
import org.unicode.cldr.util.VoterInfoList;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XMLSource.SourceLocation;
import org.unicode.cldr.util.XPathParts;

/**
 * Console test for CheckCLDR. <br>
 * Some common source directories:
 *
 * <pre>
 *  -s C:/cvsdata/unicode/cldr/incoming/vetted/main
 *  -s C:/cvsdata/unicode/cldr/incoming/proposed/main
 *  -s C:/cvsdata/unicode/cldr/incoming/proposed/main
 *  -s C:/cvsdata/unicode/cldr/testdata/main
 * </pre>
 *
 * @author markdavis
 */
@CLDRTool(alias = "check", description = "Run CheckCLDR against CLDR data")
public class ConsoleCheckCLDR {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    public static boolean showStackTrace = false;
    public static boolean errorsOnly = false;
    static boolean SHOW_LOCALE = true;
    static boolean SHOW_EXAMPLES = false;
    private static boolean CLDR_GITHUB_ANNOTATIONS =
            (Boolean.parseBoolean(System.getProperty("CLDR_GITHUB_ANNOTATIONS", "false")));

    // TODO get ride of these in favor of MyOptions

    private static final int COVERAGE = 2,
            EXAMPLES = 3,
            FILE_FILTER = 4,
            TEST_FILTER = 5,
            DATE_FORMATS = 6,
            ORGANIZATION = 7,
            SHOWALL = 8,
            PATH_FILTER = 9,
            ERRORS_ONLY = 10,
            CHECK_ON_SUBMIT = 11,
            NO_ALIASES = 12,
            SOURCE_DIRECTORY = 13,
            USER = 14,
            PHASE = 15,
            GENERATE_HTML = 16,
            VOTE_RESOLVE = 17,
            ID_VIEW = 18,
            SUBTYPE_FILTER = 19,
            BAILEY = 21,
            SINGLE_THREAD = 24;

    static final String SOURCE_DIRS =
            CLDRPaths.MAIN_DIRECTORY
                    + ","
                    + CLDRPaths.ANNOTATIONS_DIRECTORY
                    + ","
                    + CLDRPaths.SEED_DIRECTORY;

    enum MyOptions {
        coverage(
                new Params()
                        .setHelp("Set the coverage: eg -c comprehensive")
                        .setMatch("comprehensive|modern|moderate|basic")), // UOption.REQUIRES_ARG
        examples(
                new Params()
                        .setHelp("Turn on examples (actually a summary of the demo)")
                        .setFlag('x')), // , 'x', UOption.NO_ARG),
        file_filter(
                new Params()
                        .setHelp(
                                "Pick the locales (files) to check: arg is a regular expression, eg -f fr, or -f fr.*, or -f (fr|en-.*)")
                        .setDefault(".*")
                        .setMatch(".*")), // , 'f', UOption.REQUIRES_ARG).setDefault(".*"),
        test_filter(
                new Params()
                        .setHelp(
                                "Filter the Checks: arg is a regular expression, eg -t.*number.*. To check all BUT a given test, use the style -t ((?!.*CheckZones).*)")
                        .setDefault(".*")
                        .setMatch(".*")), // , 't', UOption.REQUIRES_ARG).setDefault(".*"),
        date_formats(
                new Params()
                        .setHelp("Turn on special date format checks")), // , 'd', UOption.NO_ARG),
        organization(
                new Params()
                        .setHelp(
                                "Organization: ibm, google, ....; Uses Locales.txt for to filter locales and set coverage levels")
                        .setDefault(".*")
                        .setMatch(".*")), // , 'o', UOption.REQUIRES_ARG),
        showall(
                new Params()
                        .setHelp("Show all paths, including aliased")
                        .setFlag('a')), // , 'a', UOption.NO_ARG),
        path_filter(
                new Params()
                        .setHelp("Pick the paths to check, eg -p.*languages.*")
                        .setDefault(".*")
                        .setMatch(".*")), // , 'p', UOption.REQUIRES_ARG).setDefault(".*"),
        errors_only(
                new Params()
                        .setHelp("Show errors only (with -ef, only final processing errors)")), // ,
        // 'e', UOption.NO_ARG),
        check_on_submit(new Params().setHelp("").setFlag('k')), // , 'k', UOption.NO_ARG),
        noaliases(new Params().setHelp("No aliases")), // , 'n', UOption.NO_ARG),
        source_directory(
                new Params()
                        .setHelp("Fully qualified source directories. (Conflicts with -S.)")
                        .setDefault(SOURCE_DIRS)
                        .setMatch(".*")), // , 's', UOption.REQUIRES_ARG).setDefault(SOURCE_DIRS),
        user(
                new Params()
                        .setHelp("User, eg -uu148")
                        .setMatch(".*")), // , 'u', UOption.REQUIRES_ARG),
        phase(
                new Params()
                        .setHelp("?")
                        .setMatch(Phase.class)
                        .setFlag('z')), // , 'z', UOption.REQUIRES_ARG),
        generate_html(
                new Params()
                        .setHelp("Generate HTML-style chart in directory.")
                        .setDefault(CLDRPaths.CHART_DIRECTORY + "/errors/")
                        .setMatch(".*")), // , 'g',
        // UOption.OPTIONAL_ARG).setDefault(CLDRPaths.CHART_DIRECTORY + "/errors/"),
        vote_resolution(new Params().setHelp("")), // , 'v', UOption.NO_ARG),
        id_view(new Params().setHelp("")), // , 'i', UOption.NO_ARG),
        subtype_filter(
                new Params()
                        .setHelp("error/warning subtype filter, eg unexpectedOrderOfEraYear")
                        .setDefault(".*")
                        .setMatch(".*")
                        .setFlag('y')), // , 'y', UOption.REQUIRES_ARG),
        source_all(
                new Params()
                        .setHelp(
                                "Partially qualified directories. Standard subdirectories added if not specified (/main, /annotations, /subdivisions). (Conflicts with -s.)")
                        .setMatch(".*")
                        .setFlag('S')
                        .setDefault("common,seed,exemplars")), // , 'S', <changed>),
        bailey(
                new Params()
                        .setHelp(
                                "check bailey values ("
                                        + CldrUtility.INHERITANCE_MARKER
                                        + ")")), // , 'b', UOption.NO_ARG)
        exemplarError(new Params().setFlag('E').setHelp("include to force strict Exemplar check")),
        missingPaths(
                new Params()
                        .setHelp(
                                "include to show missing and provisional paths, at the specified level")),
        singleThread(new Params().setHelp("Run in single-thread mode.").setFlag('1'));

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Option.Options myOptions = new Option.Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    // TODO get rid of these in favor of MyOptions

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("coverage", 'c', UOption.REQUIRES_ARG),
        UOption.create("examples", 'x', UOption.NO_ARG),
        UOption.create("file_filter", 'f', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("test_filter", 't', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("date_formats", 'd', UOption.NO_ARG),
        UOption.create("organization", 'o', UOption.REQUIRES_ARG),
        UOption.create("showall", 'a', UOption.NO_ARG),
        UOption.create("path_filter", 'p', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("errors_only", 'e', UOption.NO_ARG),
        UOption.create("check-on-submit", 'k', UOption.NO_ARG),
        UOption.create("noaliases", 'n', UOption.NO_ARG),
        UOption.create("source_directory", 's', UOption.REQUIRES_ARG).setDefault(SOURCE_DIRS),
        UOption.create("user", 'u', UOption.REQUIRES_ARG),
        UOption.create("phase", 'z', UOption.REQUIRES_ARG),
        UOption.create("generate_html", 'g', UOption.OPTIONAL_ARG)
                .setDefault(CLDRPaths.CHART_DIRECTORY + "/errors/"),
        UOption.create("vote resolution", 'v', UOption.NO_ARG),
        UOption.create("id view", 'i', UOption.NO_ARG),
        UOption.create("subtype_filter", 'y', UOption.REQUIRES_ARG),
        UOption.create("source_all", 'S', UOption.OPTIONAL_ARG).setDefault("common,seed,exemplars"),
        UOption.create("bailey", 'b', UOption.NO_ARG),
        UOption.create("exemplarError", 'E', UOption.NO_ARG),
        UOption.create("missingPaths", 'm', UOption.NO_ARG),
        UOption.create("singleThread", '1', UOption.NO_ARG)
    };

    private static final Comparator<String> baseFirstCollator =
            new Comparator<>() {
                LanguageTagParser languageTagParser1 = new LanguageTagParser();
                LanguageTagParser languageTagParser2 = new LanguageTagParser();

                @Override
                public int compare(String o1, String o2) {
                    String ls1 = languageTagParser1.set(o1).getLanguageScript();
                    String ls2 = languageTagParser2.set(o2).getLanguageScript();
                    int result = ls1.compareTo(ls2);
                    if (result != 0) return result;
                    return o1.compareTo(o2);
                }
            };
    private static final boolean PATH_IN_COUNT = false;

    static Counter<ErrorType> subtotalCount = new Counter<>(true); // new ErrorCount();
    static Counter<ErrorType> totalCount = new Counter<>(true);

    private enum RawStatus {
        missing,
        provisional,
        present
    }

    private static EnumSet<Subtype> calculateSubtypeFilter(final String subtypeFilterString) {
        EnumSet<Subtype> subtypeFilter = null;
        if (subtypeFilterString != null) {
            subtypeFilter = EnumSet.noneOf(Subtype.class);
            Matcher m = PatternCache.get(subtypeFilterString).matcher("");
            for (Subtype value : Subtype.values()) {
                if (m.reset(value.toString()).find() || m.reset(value.name()).find()) {
                    subtypeFilter.add(value);
                }
            }
            if (subtypeFilter.size() == 0) {
                throw new IllegalArgumentException("No subtype match for " + subtypeFilterString);
            }
        }
        return subtypeFilter;
    }

    static Matcher calculatePathFilter(final String pathFilterString) {
        if (!pathFilterString.equals(".*")) {
            return PatternCache.get(pathFilterString).matcher("");
        } else {
            return null;
        }
    }

    static Level calculateCoverageLevel(final String coverageLevelInput, boolean forHtml) {
        Level coverageLevel = null;
        if (forHtml) {
            coverageLevel = Level.MODERN; // reset
        } else if (coverageLevelInput != null) {
            coverageLevel = Level.get(coverageLevelInput);
            if (coverageLevel == Level.UNDETERMINED) {
                throw new IllegalArgumentException(
                        "-c"
                                + coverageLevelInput
                                + "\t is invalid: must be one of: "
                                + "basic,moderate,...");
            }
        }
        return coverageLevel;
    }

    /**
     * This will be the test framework way of using these tests.
     *
     * @param args
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable {
        // turn off logging to not mess up html and other output.
        CheckCLDR.setLoggerLevel(java.util.logging.Level.OFF);
        MyOptions.parse(args, true);
        ElapsedTimer totalTimer = new ElapsedTimer();
        UOption.parseArgs(args, options);
        String factoryFilter = options[FILE_FILTER].value;
        if (factoryFilter.equals("key")) {
            factoryFilter =
                    "(en|ru|nl|fr|de|it|pl|es|tr|th|ja|zh|ko|ar|bg|sr|uk|ca|hr|cs|da|fil|fi|hu|id|lv|lt|no|pt|ro|sk|sl|sv|vi|el|he|fa|hi|am|af|et|is|ms|sw|zu|bn|mr|ta|eu|gl|ur|gu|kn|ml|te|zh_Hant|pt_PT|en_GB)";
        }
        String checkFilter = options[TEST_FILTER].value;
        final String subtypeFilterString = options[SUBTYPE_FILTER].value;
        final EnumSet<Subtype> subtypeFilter = calculateSubtypeFilter(subtypeFilterString);

        errorsOnly = options[ERRORS_ONLY].doesOccur;
        boolean showMissing = MyOptions.missingPaths.option.doesOccur();

        SHOW_EXAMPLES = options[EXAMPLES].doesOccur;
        boolean showAll = options[SHOWALL].doesOccur;
        boolean checkFlexibleDates = options[DATE_FORMATS].doesOccur;
        final String pathFilterString = options[PATH_FILTER].value;
        final Matcher pathFilter = calculatePathFilter(pathFilterString);
        boolean checkOnSubmit = options[CHECK_ON_SUBMIT].doesOccur;
        boolean noaliases = options[NO_ALIASES].doesOccur;

        final Level coverageLevel =
                calculateCoverageLevel(options[COVERAGE].value, options[GENERATE_HTML].doesOccur);

        Organization organization =
                options[ORGANIZATION].value == null
                        ? null
                        : Organization.fromString(options[ORGANIZATION].value);
        if (organization != null) {
            Set<Organization> organizations = StandardCodes.make().getLocaleCoverageOrganizations();
            if (!organizations.contains(organization)) {
                throw new IllegalArgumentException(
                        "-o" + organization + "\t is invalid: must be one of: " + organizations);
            }
        }
        final CLDRConfig cldrConf = CLDR_CONFIG;
        cldrConf.setEnvironment(Environment.UNITTEST);
        final Phase phase;
        if (options[PHASE].doesOccur) {
            String phaseVal = options[PHASE].value;
            try {
                // no null check for argument; if it is is null, Phase.forString would return the
                // one from CLDRConfig
                phase = Phase.forString(phaseVal);
            } catch (IllegalArgumentException e) {
                StringBuilder sb = new StringBuilder("Incorrect Phase value");
                if (phaseVal != null && !phaseVal.isEmpty()) {
                    sb.append(" '");
                    sb.append(phaseVal);
                    sb.append("'");
                }
                sb.append(": should be one of ");
                for (Phase curPhase : Phase.values()) {
                    // implicitly does a toString;
                    sb.append(curPhase);
                    sb.append(", ");
                }
                int lastIdx = sb.lastIndexOf(",");
                // remove the last comma, if it occurs
                if (lastIdx > -1) {
                    String tmpBuf = sb.substring(0, lastIdx);
                    sb.setLength(0);
                    sb.append(tmpBuf);
                }
                sb.append(".");
                // TODO: Reporting should be similar to an error (wrong parameter...), and not
                // actually an Exception
                throw new IllegalArgumentException(sb.toString(), e);
            }
        } else {
            phase = cldrConf.getPhase();
        }

        boolean baileyTest = options[BAILEY].doesOccur;

        File sourceDirectories[] = null;

        if (MyOptions.source_all.option.doesOccur()) {
            if (MyOptions.source_directory.option.doesOccur()) {
                throw new IllegalArgumentException("Don't use -s and -S together.");
            }
            sourceDirectories =
                    cldrConf.addStandardSubdirectories(
                            cldrConf.getCLDRDataDirectories(
                                    MyOptions.source_all.option.getValue()));
        } else {
            String[] sdirs = options[SOURCE_DIRECTORY].value.split(",\\s*");
            sourceDirectories = new File[sdirs.length];
            for (int i = 0; i < sdirs.length; ++i) {
                sourceDirectories[i] =
                        new File(
                                CldrUtility.checkValidDirectory(
                                        sdirs[i], "Fix with -s. Use -h for help."));
            }
        }

        if (options[GENERATE_HTML].doesOccur) {
            ErrorFile.generated_html_directory = options[GENERATE_HTML].value;
            ErrorFile.generated_html_count =
                    FileUtilities.openUTF8Writer(ErrorFile.generated_html_directory, "count.txt");
        }

        final boolean sequential =
                SHOW_EXAMPLES
                        || options[GENERATE_HTML].doesOccur
                        || options[SINGLE_THREAD].doesOccur;

        idView = options[ID_VIEW].doesOccur;

        if (options[VOTE_RESOLVE].doesOccur) {
            resolveVotesDirectory =
                    CldrUtility.checkValidFile(
                            CLDRPaths.BASE_DIRECTORY + "incoming/vetted/votes/", true, null);
            voterInfoList.setVoterToInfo(
                    CldrUtility.checkValidFile(
                            CLDRPaths.BASE_DIRECTORY + "incoming/vetted/usersa/usersa.xml",
                            false,
                            null));
            voteResolver = new VoteResolver<>(voterInfoList);
        }

        String user = options[USER].value;

        System.out.println("Source directories:\n");
        for (File f : sourceDirectories) {
            System.out.println(
                    "    " + f.getPath() + "\t(" + PathUtilities.getNormalizedPathString(f) + ")");
        }

        // set up the test
        Factory cldrFactory =
                SimpleFactory.make(sourceDirectories, factoryFilter)
                        .setSupplementalDirectory(new File(CLDRPaths.SUPPLEMENTAL_DIRECTORY));
        final TestCache testCache = cldrFactory.getTestCache();
        testCache.setNameMatcher(checkFilter);

        {
            // we create an extraneous CompoundCheckCLDR here just to check the filters
            CompoundCheckCLDR checkCldr = CheckCLDR.getCheckAll(cldrFactory, checkFilter);
            if (checkCldr.getFilteredTestList().size() == 0) {
                throw new IllegalArgumentException("The filter doesn't match any tests.");
            }
            System.out.println("filtered tests: " + checkCldr.getFilteredTests());
        }

        Factory backCldrFactory = CLDRConfig.getInstance().getMainAndAnnotationsFactory();
        english = backCldrFactory.make("en", true);

        CheckCLDR.setDisplayInformation(english);
        setExampleGenerator(new ExampleGenerator(english, english));
        PathShower pathShower = new PathShower();

        // call on the files
        Set<String> locales = new TreeSet<>(baseFirstCollator);
        locales.addAll(cldrFactory.getAvailable());

        Set<String> fatalErrors = new TreeSet<>();

        showHeaderLine();

        supplementalDataInfo = SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);

        LocaleIDParser localeIDParser = new LocaleIDParser();
        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);

        final Map<String, Level> locale_status =
                StandardCodes.make().getLocaleToLevel(organization);

        final List<String> specialPurposeLocales = new ArrayList<>(Arrays.asList("en_US_POSIX"));

        // TODO: englishPaths doesn't seem to be used.
        // final HashSet<String> ep = new HashSet<>();
        // final CLDRFile displayFile = CheckCLDR.getDisplayInformation();
        // addPrettyPaths(
        //         displayFile, pathFilter, pathHeaderFactory, noaliases, true, ep);
        // addPrettyPaths(
        //         displayFile,
        //         displayFile.getExtraPaths(),
        //         pathFilter,
        //         pathHeaderFactory,
        //         noaliases,
        //         true,
        //         ep);
        // final Set<String> englishPaths = Collections.unmodifiableSet(ep); // for robustness

        // Set up our stream to use. It will be parallel usually, or sequential for HTML.
        Stream<String> stream;

        if (sequential) {
            System.err.println("# Note: running in sequential mode.");
            stream = locales.stream();
        } else {
            stream = locales.parallelStream();
        }

        // now, run it
        stream.forEach(
                localeID -> {
                    if (ErrorFile.writeError != null) {
                        return; // get out, it's an error.
                    }

                    List<CheckStatus> result = new ArrayList<>();
                    Set<PathHeader> paths = new TreeSet<>(); // CLDRFile.ldmlComparator);
                    Map<String, String> m = new TreeMap<>();
                    Map<String, String> options = new HashMap<>();
                    FlexibleDateFromCLDR fset = new FlexibleDateFromCLDR();

                    if (CLDRFile.isSupplementalName(localeID)) return;
                    if (supplementalDataInfo.getDefaultContentLocales().contains(localeID)) {
                        System.out.println("# Skipping default content locale: " + localeID);
                        return;
                    }

                    // We don't really need to check the POSIX locale, as it is a special purpose
                    // locale
                    if (specialPurposeLocales.contains(localeID)) {
                        System.out.println("# Skipping special purpose locale: " + localeID);
                        return;
                    }

                    boolean isLanguageLocale =
                            localeID.equals(localeIDParser.set(localeID).getLanguageScript());
                    options.clear();

                    if (MyOptions.exemplarError.option.doesOccur()) {
                        options.put(Options.Option.exemplarErrors.toString(), "true");
                    }

                    // if the organization is set, skip any locale that doesn't have a value in
                    // Locales.txt
                    Level level = coverageLevel;
                    if (level == null) {
                        level = Level.MODERN;
                    }
                    if (organization != null) {
                        if (locale_status == null) return;
                        level = locale_status.get(localeID);
                        if (level == null) return;
                        if (level.compareTo(Level.BASIC) < 0) return;
                    } else if (!isLanguageLocale) {
                        // otherwise, skip all language locales
                        options.put(Options.Option.CheckCoverage_skip.getKey(), "true");
                    }

                    // if (organization != null)
                    // options.put(Options.Option.CoverageLevel_localeType.getKey(),
                    // organization.toString());
                    options.put(Options.Option.phase.getKey(), phase.toString());

                    // also need the locale in the options
                    options.put(Options.Option.locale.getKey(), localeID);

                    if (SHOW_LOCALE && sequential) System.out.println();

                    CLDRFile file;
                    CLDRFile englishFile = english;
                    CLDRFile parent = null;

                    ElapsedTimer timer = new ElapsedTimer();
                    try {
                        file = cldrFactory.make(localeID, true);
                        if (ErrorFile.voteFactory != null) {
                            ErrorFile.voteFile = ErrorFile.voteFactory.make(localeID, true);
                        }
                        final String parentID = LocaleIDParser.getParent(localeID);
                        if (parentID != null) {
                            parent = cldrFactory.make(parentID, true);
                        }
                    } catch (RuntimeException e) {
                        fatalErrors.add(localeID);
                        System.out.println("FATAL ERROR: " + localeID);
                        e.printStackTrace(System.out);
                        return;
                    }

                    TestResultBundle bundle = testCache.getBundle(new CheckCLDR.Options(options));

                    // generate HTML if asked for
                    if (ErrorFile.generated_html_directory != null) {
                        String baseLanguage = localeIDParser.set(localeID).getLanguageScript();

                        if (!baseLanguage.equals(ErrorFile.lastBaseLanguage)) {
                            ErrorFile.lastBaseLanguage = baseLanguage;
                            try {
                                ErrorFile.openErrorFile(localeID, baseLanguage);
                            } catch (IOException ioe) {
                                ErrorFile.writeError = ioe;
                                System.err.println(
                                        "Exception "
                                                + ioe
                                                + " while trying to open file "
                                                + localeID);
                                ioe.printStackTrace();
                                return;
                            }
                        }
                    }

                    if (user != null) {
                        file = new CLDRFile.TestUser(file, user, isLanguageLocale);
                        if (parent != null) {
                            parent = new CLDRFile.TestUser(parent, user, isLanguageLocale);
                        }
                    }
                    // checkCldr.setCldrFileToCheck(file, new Options(options), result);

                    subtotalCount.clear();

                    for (Iterator<CheckStatus> it3 = result.iterator(); it3.hasNext(); ) {
                        CheckStatus status = it3.next();
                        String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
                        CheckStatus.Type statusType = status.getType();

                        if (errorsOnly) {
                            if (!statusType.equals(CheckStatus.errorType)) continue;
                        }

                        if (subtypeFilter != null) {
                            if (!subtypeFilter.contains(status.getSubtype())) {
                                continue;
                            }
                        }

                        if (checkOnSubmit) {
                            if (!status.isCheckOnSubmit()
                                    || !statusType.equals(CheckStatus.errorType)) continue;
                        }
                        showValue(
                                file,
                                null,
                                localeID,
                                null,
                                null,
                                null,
                                null,
                                statusString,
                                status.getSubtype());
                    }
                    paths.clear();

                    CoverageInfo covInfo = cldrConf.getCoverageInfo();
                    for (String path : file.fullIterable()) {
                        if (pathFilter != null && !pathFilter.reset(path).find()) {
                            continue;
                        }
                        if (level != null) {
                            Level currentLevel = covInfo.getCoverageLevel(path, localeID);
                            if (currentLevel.compareTo(level) > 0) {
                                continue;
                            }
                        }
                        final PathHeader pathHeader = pathHeaderFactory.fromPath(path);
                        if (pathHeader.getSectionId() != SectionId.Special) {
                            paths.add(pathHeader);
                        }
                    }

                    UnicodeSet missingExemplars = new UnicodeSet();
                    UnicodeSet missingCurrencyExemplars = new UnicodeSet();
                    if (checkFlexibleDates) {
                        fset.set(file);
                    }
                    pathShower.set(localeID);

                    // only create if we are going to use it
                    final ExampleGenerator exampleGenerator =
                            SHOW_EXAMPLES ? new ExampleGenerator(file, englishFile) : null;

                    int pathCount = 0;
                    Status otherPath = new Status();
                    int rawMissingCount = 0;
                    int rawProvisionalCount = 0;
                    CLDRFile unresolved = file.getUnresolved();

                    for (PathHeader pathHeader : paths) {
                        pathCount++;
                        String path = pathHeader.getOriginalPath();
                        String prettyPath =
                                pathHeader.toString().replace('\t', '|').replace(' ', '_');
                        if (!showAll && !file.isWinningPath(path)) {
                            continue;
                        }
                        final String topValue = unresolved.getStringValue(path);
                        RawStatus rawStatus = RawStatus.present;

                        if (topValue == null) {
                            rawStatus = RawStatus.missing;
                            rawMissingCount++;
                        }

                        if (!isLanguageLocale && !baileyTest) {
                            final String sourceLocaleID = file.getSourceLocaleID(path, otherPath);
                            if (!localeID.equals(sourceLocaleID)) {
                                continue;
                            }
                            // also skip aliases
                            if (!path.equals(otherPath.pathWhereFound)) {
                                continue;
                            }
                        }
                        if (path.contains("@alt") && path.contains("proposed")) {
                            continue;
                        }
                        String value = file.getStringValue(path);
                        if (baileyTest) {
                            value = CldrUtility.INHERITANCE_MARKER;
                        }

                        String fullPath = file.getFullXPath(path);
                        if (topValue != null) {
                            XPathParts fullParts = XPathParts.getFrozenInstance(fullPath);
                            String draftStatus = fullParts.getAttributeValue(-1, "draft");
                            if (draftStatus != null && !draftStatus.equals("contributed")) {
                                rawProvisionalCount++;
                                rawStatus = RawStatus.provisional;
                            }
                        }
                        if (showMissing && rawStatus != RawStatus.present) {
                            String englishValue = englishFile.getStringValue(path);
                            if (englishValue == null) {
                                englishValue = "n/a";
                            }
                            System.out.println(
                                    getLocaleAndName(localeID)
                                            + "\tRaw "
                                            + rawStatus
                                            + "\t"
                                            + pathHeader
                                            + "\t"
                                            + englishValue
                                            + "\t"
                                            + path);
                        }

                        String example = "";
                        if (SHOW_EXAMPLES && exampleGenerator != null) {
                            example =
                                    ExampleGenerator.simplify(
                                            exampleGenerator.getExampleHtml(path, value));
                            showExamples(
                                    file, prettyPath, localeID, path, value, fullPath, example);
                        }
                        if (checkFlexibleDates) {
                            fset.checkFlexibles(path, value, fullPath);
                        }
                        int limit = 1;
                        for (int jj = 0; jj < limit; ++jj) {
                            if (jj == 0) {
                                bundle.check(path, result, value);
                            } else {
                                bundle.getExamples(path, value, result);
                            }

                            boolean showedOne = false;
                            for (Iterator<CheckStatus> it3 = result.iterator(); it3.hasNext(); ) {
                                CheckStatus status = it3.next();
                                String statusString =
                                        status.toString(); // com.ibm.icu.impl.Utility.escape(
                                CheckStatus.Type statusType = status.getType();
                                Object[] parameters = status.getParameters();

                                if (parameters != null) {
                                    if (parameters.length >= 1
                                            && status.getCause().getClass()
                                                    == CheckForExemplars.class) {
                                        try {
                                            UnicodeSet set =
                                                    new UnicodeSet(parameters[0].toString());
                                            if (status.getMessage().contains("currency")) {
                                                missingCurrencyExemplars.addAll(set);
                                            } else {
                                                missingExemplars.addAll(set);
                                            }
                                        } catch (RuntimeException e) {
                                        } // skip if not parseable as set
                                    }
                                }

                                if (errorsOnly && !statusType.equals(CheckStatus.errorType)) {
                                    continue;
                                }

                                if (subtypeFilter != null) {
                                    if (!subtypeFilter.contains(status.getSubtype())) {
                                        continue;
                                    }
                                }
                                if (checkOnSubmit) {
                                    if (!status.isCheckOnSubmit()
                                            || !statusType.equals(CheckStatus.errorType)) continue;
                                }

                                if (statusType.equals(CheckStatus.demoType)) {
                                    SimpleDemo d = status.getDemo();
                                    if (d != null && d instanceof FormatDemo) {
                                        FormatDemo fd = (FormatDemo) d;
                                        m.clear();
                                        if (d.processPost(m))
                                            System.out.println("\tDemo:\t" + fd.getPlainText(m));
                                    }
                                    continue;
                                }

                                if (parameters != null) {
                                    for (int i = 0; i < parameters.length; ++i) {
                                        if (showStackTrace && parameters[i] instanceof Throwable) {
                                            ((Throwable) parameters[i]).printStackTrace();
                                        }
                                    }
                                }

                                showValue(
                                        file,
                                        prettyPath,
                                        localeID,
                                        example,
                                        path,
                                        value,
                                        fullPath,
                                        statusString,
                                        status.getSubtype());
                                showedOne = true;
                            }
                            if (!showedOne && phase != Phase.FINAL_TESTING) {
                                if (!showedOne && showAll) {
                                    showValue(
                                            file,
                                            prettyPath,
                                            localeID,
                                            example,
                                            path,
                                            value,
                                            fullPath,
                                            "ok",
                                            Subtype.none);
                                    showedOne = true;
                                }
                            }
                        }
                    }

                    if (resolveVotesDirectory != null) {
                        LocaleVotingData.resolveErrors(localeID);
                    }

                    showSummary(
                            localeID,
                            level,
                            "Items:\t"
                                    + pathCount
                                    + "\tRaw Missing:\t"
                                    + rawMissingCount
                                    + "\tRaw Provisional:\t"
                                    + rawProvisionalCount);

                    if (missingExemplars.size() != 0) {
                        missingExemplars.removeAll(
                                new UnicodeSet("[[:Uppercase:]-[İ]]")); // remove uppercase #4670
                        if (missingExemplars.size() != 0) {
                            showSummary(
                                    localeID,
                                    level,
                                    "Total missing from general exemplars:\t"
                                            + missingExemplars.size()
                                            + "\t"
                                            + UnicodeSetPrettyPrinter.fromIcuLocale(localeID)
                                                    .format(missingExemplars));
                        }
                    }
                    if (missingCurrencyExemplars.size() != 0) {
                        Collator col = Collator.getInstance(new ULocale(localeID));
                        showSummary(
                                localeID,
                                level,
                                "Total missing from currency exemplars:\t"
                                        + UnicodeSetPrettyPrinter.fromIcuLocale(localeID)
                                                .format(missingCurrencyExemplars));
                    }
                    for (ErrorType type : subtotalCount.keySet()) {
                        showSummary(
                                localeID,
                                level,
                                "Subtotal " + type + ":\t" + subtotalCount.getCount(type));
                    }

                    if (checkFlexibleDates) {
                        fset.showFlexibles();
                    }
                    if (SHOW_EXAMPLES && exampleGenerator != null) {
                        // ldml/dates/timeZoneNames/zone[@type="America/Argentina/San_Juan"]/exemplarCity
                        for (String zone : StandardCodes.make().getGoodAvailableCodes("tzid")) {
                            String path =
                                    "//ldml/dates/timeZoneNames/zone[@type=\""
                                            + zone
                                            + "\"]/exemplarCity";
                            PathHeader pathHeader = pathHeaderFactory.fromPath(path);
                            String prettyPath =
                                    pathHeader.toString().replace('\t', '|').replace(' ', '_');
                            if (pathFilter != null && !pathFilter.reset(path).matches()) {
                                continue;
                            }
                            String fullPath = file.getStringValue(path);
                            if (fullPath != null) {
                                continue;
                            }
                            /*
                             * TODO: fix this code. Calling getExampleHtml with value = null will always return null,
                             * so what's this supposed to accomplish?
                             */
                            String example =
                                    ExampleGenerator.simplify(
                                            exampleGenerator.getExampleHtml(
                                                    path, null /* value */));
                            showExamples(file, prettyPath, localeID, path, null, fullPath, example);
                        }
                    }
                    System.out.println("# " + localeID + " Elapsed time: " + timer);
                    System.out.flush();
                });

        if (ErrorFile.errorFileWriter != null) {
            ErrorFile.closeErrorFile();
        }

        // an error occurred opening HTML, rethrow it.
        if (ErrorFile.writeError != null) {
            throw ErrorFile.writeError;
        }

        if (ErrorFile.generated_html_directory != null) {
            ErrorFile.writeErrorCountsText();
            ErrorFile.writeErrorFileIndex();
        }
        System.out.println();
        for (ErrorType type : totalCount.keySet()) {
            System.out.println("# Total " + type + ":\t" + totalCount.getCount(type));
        }

        System.out.println();
        System.out.println("# Total elapsed time: " + totalTimer);
        if (fatalErrors.size() != 0) {
            System.out.println("# FATAL ERRORS:");
        }
        long errorCount = totalCount.getCount(ErrorType.error) + fatalErrors.size();
        if (errorCount != 0) {
            System.out.println();
            System.out.println("<< FAILURE - Error count is " + errorCount + " . >>");
            System.exit(-1);
        } else {
            System.out.println();
            System.out.println("<< SUCCESS - No errors found. >>");
        }
        if (LogicalGrouping.GET_TYPE_COUNTS) {
            for (String s : LogicalGrouping.typeCount.keySet()) {
                System.out.println(s + "=" + LogicalGrouping.typeCount.get(s));
            }
        }
    } // end of main()

    static class LocaleVotingData {
        private int disputedCount = 0;
        Counter<Organization> missingOrganizationCounter = new Counter<>(true);
        Counter<Organization> goodOrganizationCounter = new Counter<>(true);
        Counter<Organization> conflictedOrganizations = new Counter<>(true);
        Counter<VoteResolver.Status> winningStatusCounter = new Counter<>(true);

        static Map<String, LocaleVotingData> localeToErrors = new HashMap<>();
        private static Map<Integer, String> idToPath;

        public static void resolveErrors(String locale) {
            localeToErrors.put(locale, new LocaleVotingData(locale));
        }

        public LocaleVotingData(String locale) {

            Map<Organization, VoteResolver.Level> orgToMaxVote =
                    voterInfoList.getOrganizationToMaxVote(locale);

            Map<Integer, Map<Integer, CandidateInfo>> info =
                    VoteResolver.getBaseToAlternateToInfo(
                            resolveVotesDirectory + locale + ".xml", voterInfoList);

            Map<String, Integer> valueToItem = new HashMap<>();

            for (int basePath : info.keySet()) {
                final Map<Integer, CandidateInfo> itemInfo = info.get(basePath);

                // find the last release status and value
                voteResolver.clear();
                valueToItem.clear();

                for (int item : itemInfo.keySet()) {
                    String itemValue = getValue(item);
                    valueToItem.put(itemValue, item);

                    CandidateInfo candidateInfo = itemInfo.get(item);
                    if (candidateInfo.oldStatus != null) {
                        voteResolver.setBaseline(itemValue, candidateInfo.oldStatus);
                    }
                    voteResolver.add(itemValue);
                    for (int voter : candidateInfo.voters) {
                        try {
                            voteResolver.add(itemValue, voter);
                        } catch (UnknownVoterException e) {
                            // skip
                        }
                    }
                }

                EnumSet<Organization> basePathConflictedOrganizations =
                        voteResolver.getConflictedOrganizations();
                conflictedOrganizations.addAll(basePathConflictedOrganizations, 1);

                VoteResolver.Status winningStatus = voteResolver.getWinningStatus();
                String winningValue = voteResolver.getWinningValue();

                winningStatusCounter.add(winningStatus, 1);

                if (winningStatus == VoteResolver.Status.approved) {
                    continue;
                }

                CandidateInfo candidateInfo = itemInfo.get(valueToItem.get(winningValue));
                Map<Organization, VoteResolver.Level> orgToMaxVoteHere =
                        voterInfoList.getOrganizationToMaxVote(candidateInfo.voters);

                // if the winning item is less than contributed, record the organizations that
                // haven't given their
                // maximum vote to the winning item.
                if (winningStatus.compareTo(VoteResolver.Status.contributed) < 0) {
                    // showPaths(basePath, itemInfo);
                    for (Organization org : orgToMaxVote.keySet()) {
                        VoteResolver.Level maxVote = orgToMaxVote.get(org);
                        VoteResolver.Level maxVoteHere = orgToMaxVoteHere.get(org);
                        if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
                            missingOrganizationCounter.add(org, 1);
                        }
                    }
                    if (voteResolver.isDisputed()) {
                        disputedCount++;
                        String path = getIdToPath(basePath);
                        ErrorFile.addDataToErrorFile(
                                locale, path, ErrorType.disputed, Subtype.none);
                    }
                } else {
                    for (Organization org : orgToMaxVote.keySet()) {
                        VoteResolver.Level maxVote = orgToMaxVote.get(org);
                        VoteResolver.Level maxVoteHere = orgToMaxVoteHere.get(org);
                        if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
                        } else {
                            goodOrganizationCounter.add(org, 1);
                        }
                    }
                }
            }
            System.out.println(
                    getLocaleAndName(locale) + "\tEnabled Organizations:\t" + orgToMaxVote);
            if (disputedCount != 0) {
                System.out.println(
                        getLocaleAndName(locale) + "\tDisputed Items:\t" + disputedCount);
            }

            if (missingOrganizationCounter.size() > 0) {
                System.out.println(
                        getLocaleAndName(locale)
                                + "\tMIA organizations:\t"
                                + missingOrganizationCounter);
                System.out.println(
                        getLocaleAndName(locale)
                                + "\tConflicted organizations:\t"
                                + conflictedOrganizations);
                System.out.println(
                        getLocaleAndName(locale)
                                + "\tCool organizations!:\t"
                                + goodOrganizationCounter);
            }
            System.out.println(
                    getLocaleAndName(locale) + "\tOptimal Status:\t" + winningStatusCounter);
        }

        private static String getIdToPath(int basePath) {
            if (idToPath == null) {
                idToPath = VoteResolver.getIdToPath(resolveVotesDirectory + "xpathTable.xml");
            }
            return idToPath.get(basePath);
        }

        public static LocaleVotingData get(String locale) {
            return localeToErrors.get(locale);
        }

        int getDisputedCount() {
            return disputedCount;
        }

        String getConflictedHTML() {
            String result = conflictedOrganizations.toString();
            if (result.length() == 0) {
                return "";
            }
            result = result.substring(1, result.length() - 1);
            result = result.replace(", ", "<br>");
            return result;
        }
    }

    private static String getValue(int item) {
        return String.valueOf(item);
    }

    static Matcher draftStatusMatcher =
            PatternCache.get("\\[@draft=\"(provisional|unconfirmed)\"]").matcher("");

    enum ErrorType {
        ok,
        error,
        disputed,
        warning,
        core,
        posix,
        minimal,
        basic,
        moderate,
        modern,
        comprehensive,
        optional,
        contributed,
        provisional,
        unconfirmed,
        unknown;
        static EnumSet<ErrorType> unapproved =
                EnumSet.range(ErrorType.contributed, ErrorType.unconfirmed);
        static EnumSet<ErrorType> coverage = EnumSet.range(ErrorType.posix, ErrorType.optional);
        static EnumSet<ErrorType> showInSummary =
                EnumSet.of(
                        ErrorType.error,
                        ErrorType.warning,
                        ErrorType.posix,
                        ErrorType.minimal,
                        ErrorType.basic);

        static ErrorType fromStatusString(String statusString) {
            ErrorType shortStatus =
                    statusString.equals("ok")
                            ? ErrorType.ok
                            : statusString.startsWith("Error")
                                    ? ErrorType.error
                                    : statusString.equals("disputed")
                                            ? ErrorType.disputed
                                            : statusString.startsWith("Warning")
                                                    ? ErrorType.warning
                                                    : statusString.equals("contributed")
                                                            ? ErrorType.contributed
                                                            : statusString.equals("provisional")
                                                                    ? ErrorType.provisional
                                                                    : statusString.equals(
                                                                                    "unconfirmed")
                                                                            ? ErrorType.unconfirmed
                                                                            : ErrorType.unknown;
            if (shortStatus == ErrorType.unknown) {
                throw new IllegalArgumentException("Unknown error type: " + statusString);
            } else if (shortStatus == ErrorType.warning) {
                if (coverageMatcher.reset(statusString).find()) {
                    shortStatus = ErrorType.valueOf(coverageMatcher.group(1));
                }
            }
            return shortStatus;
        }
    }

    static class ErrorFile {
        /** cached error for later rethrow */
        public static Throwable writeError = null;

        public static String lastBaseLanguage = "";
        private static final boolean SHOW_VOTING_INFO = false;
        public static CLDRFile voteFile;
        public static Factory voteFactory;

        private static void openErrorFile(String localeID, String baseLanguage) throws IOException {
            if (ErrorFile.errorFileWriter != null) {
                ErrorFile.closeErrorFile();
            }
            ErrorFile.errorFileWriter =
                    FileUtilities.openUTF8Writer(
                            ErrorFile.generated_html_directory, baseLanguage + ".html");
            ErrorFile.errorFileTable = new TablePrinter();
            errorFileCounter.clear();
            ErrorFile.errorFileTable
                    .setCaption("Problem Details")
                    .addColumn("Problem")
                    .setCellAttributes("align=\"left\" class=\"{0}\"")
                    .setSortPriority(0)
                    .setSpanRows(true)
                    .setBreakSpans(true)
                    .setRepeatHeader(true)
                    .setHeaderCell(true)
                    .addColumn("Subtype")
                    .setCellAttributes("align=\"left\" class=\"{1}\"")
                    .setSortPriority(1)
                    .setSpanRows(true)
                    .setBreakSpans(true)
                    .setRepeatHeader(true)
                    .setHeaderCell(true)
                    .addColumn("Locale")
                    .setCellAttributes("class=\"{1}\"")
                    .setCellPattern("<a href=\"http://unicode.org/cldr/apps/survey?_={0}\">{0}</a>")
                    .setSortPriority(2)
                    .setSpanRows(true)
                    .setBreakSpans(true) // .setRepeatDivider(true)
                    .addColumn("Name")
                    .setCellAttributes("class=\"{1}\"")
                    .setSpanRows(true)
                    .setBreakSpans(true)
                    .addColumn("Section")
                    .setCellAttributes("class=\"{1}\"")
                    .setSortPriority(3)
                    .setCellPattern(
                            "<a href=\"http://unicode.org/cldr/apps/survey?_={3}&x={0}\">{0}</a>")
                    .setSpanRows(true)
                    .addColumn("Count")
                    .setCellAttributes("class=\"{1}\" align=\"right\"");

            showIndexHead("", localeID, ErrorFile.errorFileWriter);
        }

        static TablePrinter errorFileTable = new TablePrinter();
        static Counter<Row.R4<String, String, ErrorType, Subtype>> errorFileCounter =
                new Counter<>(true);

        private static void addDataToErrorFile(
                String localeID, String path, ErrorType shortStatus, Subtype subtype) {
            String section = path == null ? null : XPathToMenu.xpathToMenu(path);
            if (section == null) {
                section = "general";
            }
            errorFileCounter.add(new Row.R4<>(localeID, section, shortStatus, subtype), 1);
            ErrorFile.sectionToProblemsToLocaleToCount.add(
                    new Row.R4<>(section, shortStatus, subtype, localeID), 1);
        }

        private static void closeErrorFile() {
            Set<String> locales = new TreeSet<>();
            for (Row.R4<String, String, ErrorType, Subtype> item : errorFileCounter.keySet()) {
                String localeID = item.get0();
                locales.add(localeID);
                String section = item.get1();
                ErrorType shortStatus = item.get2();
                Subtype subtype = item.get3();
                errorFileTable
                        .addRow()
                        .addCell(shortStatus)
                        .addCell(subtype)
                        .addCell(localeID)
                        .addCell(ConsoleCheckCLDR.getLocaleName(localeID))
                        .addCell(section) // menuPath == null ? "" : "<a href='" + link + "'>" +
                        // menuPath + "</a>"
                        .addCell(errorFileCounter.getCount(item))
                        .finishRow();
            }

            if (SHOW_VOTING_INFO) {
                TablePrinter data =
                        new TablePrinter()
                                .setCaption("Voting Information")
                                .addColumn("Locale")
                                .setHeaderCell(true)
                                .addColumn("Name")
                                .setHeaderCell(true)
                                .addColumn("Organization")
                                .addColumn("Missing")
                                .addColumn("Conflicted");
                for (String localeID : locales) {
                    // now the voting info
                    LocaleVotingData localeVotingData =
                            LocaleVotingData.localeToErrors.get(localeID);
                    if (localeVotingData != null) {
                        // find all the orgs with data
                        EnumSet<Organization> orgs = EnumSet.noneOf(Organization.class);
                        orgs.addAll(localeVotingData.missingOrganizationCounter.keySet());
                        orgs.addAll(localeVotingData.conflictedOrganizations.keySet());
                        orgs.addAll(localeVotingData.goodOrganizationCounter.keySet());
                        for (Organization org : orgs) {
                            data.addRow()
                                    .addCell(ConsoleCheckCLDR.getLinkedLocale(localeID))
                                    .addCell(ConsoleCheckCLDR.getLocaleName(localeID))
                                    .addCell(org)
                                    .addCell(
                                            localeVotingData.missingOrganizationCounter.getCount(
                                                    org))
                                    .addCell(localeVotingData.conflictedOrganizations.getCount(org))
                                    .finishRow();
                        }
                    }
                }
                ErrorFile.errorFileWriter.println(data.toTable());
                ErrorFile.errorFileWriter.println("<p></p>");
            }

            // generated_html.println("<table border='1' style='border-collapse: collapse'
            // bordercolor='#CCCCFF'>");
            // Locale Group Error Warning Missing Votes: Contributed Missing Votes: Provisional
            // Missing Votes:
            // Unconfirmed Missing Coverage: Posix Missing Coverage: Minimal Missing Coverage: Basic
            // Missing Coverage:
            // Moderate Missing Coverage: Modern
            ErrorFile.errorFileWriter.println(ErrorFile.errorFileTable.toTable());
            ErrorFile.errorFileWriter.println(ShowData.dateFooter());
            ErrorFile.errorFileWriter.println(CldrUtility.ANALYTICS);
            ErrorFile.errorFileWriter.println("</body></html>");
            ErrorFile.errorFileWriter.close();
            ErrorFile.errorFileTable = null;
        }

        // ================ Index File ===================

        static void showErrorFileIndex(PrintWriter generated_html_index) {

            // get organizations
            Relation<Organization, String> orgToLocales = getOrgToLocales();

            TablePrinter indexTablePrinter =
                    new TablePrinter()
                            .setCaption("Problem Summary")
                            .setTableAttributes(
                                    "border='1' style='border-collapse: collapse' bordercolor='blue'")
                            .addColumn("BASE")
                            .setHidden(true) // .setRepeatDivider(true)
                            .addColumn("Locale")
                            .setCellPattern(
                                    "<a name=\"{0}\" href=\"{1}.html\">{0}</a>") // link to base,
                            // anchor
                            // with full
                            .addColumn("Name");
            if (SHOW_VOTING_INFO) {
                indexTablePrinter.addColumn("Summary").addColumn("Missing");
            }
            for (Organization org : orgToLocales.keySet()) {
                indexTablePrinter.addColumn(org.toString().substring(0, 2));
            }
            indexTablePrinter
                    .addColumn("Disputed")
                    .setHeaderAttributes("class='disputed'")
                    .setCellAttributes("class='disputed'")
                    .addColumn("Conflicted")
                    .setHeaderAttributes("class='conflicted'")
                    .setCellAttributes("class='conflicted'");

            for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
                String columnTitle = UCharacter.toTitleCase(type.toString(), null);
                final boolean coverage = ConsoleCheckCLDR.ErrorType.coverage.contains(type);
                if (coverage) {
                    columnTitle = "MC: " + columnTitle;
                } else if (ConsoleCheckCLDR.ErrorType.unapproved.contains(type)) {
                    columnTitle = "MV: " + columnTitle;
                }
                indexTablePrinter
                        .addColumn(columnTitle)
                        .setHeaderAttributes("class='" + type + "'")
                        .setCellAttributes("class='" + type + "'");
            }

            // now fill in the data
            LanguageTagParser ltp = new LanguageTagParser();
            for (String key : ErrorFile.errorFileIndexData.keySet()) {
                Pair<String, Counter<ErrorType>> pair = ErrorFile.errorFileIndexData.get(key);
                String htmlOpenedFileLanguage = pair.getFirst();
                Counter<ErrorType> counts = pair.getSecond();
                LocaleVotingData votingData = LocaleVotingData.get(htmlOpenedFileLanguage);
                if (counts.getTotal() == 0) {
                    continue;
                }
                final String baseLanguage = ltp.set(htmlOpenedFileLanguage).getLanguage();
                indexTablePrinter
                        .addRow()
                        .addCell(baseLanguage)
                        .addCell(htmlOpenedFileLanguage)
                        .addCell(ConsoleCheckCLDR.getLocaleName(htmlOpenedFileLanguage));
                if (SHOW_VOTING_INFO) {
                    indexTablePrinter
                            .addCell(
                                    votingData == null
                                            ? ""
                                            : votingData.winningStatusCounter.toString())
                            .addCell(
                                    votingData == null
                                            ? ""
                                            : votingData.missingOrganizationCounter.toString());
                }
                for (Organization org : orgToLocales.keySet()) {
                    indexTablePrinter.addCell(
                            orgToLocales.getAll(org).contains(htmlOpenedFileLanguage)
                                    ? org.toString().substring(0, 2)
                                    : "");
                }
                indexTablePrinter
                        .addCell(
                                votingData == null
                                        ? ""
                                        : formatSkippingZero(votingData.getDisputedCount()))
                        .addCell(votingData == null ? "" : votingData.getConflictedHTML());
                for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
                    indexTablePrinter.addCell(formatSkippingZero(counts.getCount(type)));
                }
                indexTablePrinter.finishRow();
            }
            generated_html_index.println(indexTablePrinter.toTable());
            generated_html_index.println(ShowData.dateFooter());
            generated_html_index.println(CldrUtility.ANALYTICS);
            generated_html_index.println("</body></html>");
        }

        static Relation<Organization, String> orgToLocales;

        private static Relation<Organization, String> getOrgToLocales() {
            if (orgToLocales == null) {
                orgToLocales = Relation.of(new TreeMap<Organization, Set<String>>(), TreeSet.class);
                StandardCodes sc = StandardCodes.make();
                for (Organization org : sc.getLocaleCoverageOrganizations()) {
                    for (String locale : sc.getLocaleCoverageLocales(org)) {
                        Level x = sc.getLocaleCoverageLevel(org, locale);
                        if (x.compareTo(Level.BASIC) > 0) {
                            orgToLocales.put(org, locale);
                        }
                    }
                }
            }
            return orgToLocales;
        }

        static void showSections() throws IOException {
            Relation<Organization, String> orgToLocales = getOrgToLocales();
            TablePrinter indexTablePrinter =
                    new TablePrinter()
                            .setCaption("Problem Summary")
                            .setTableAttributes(
                                    "border='1' style='border-collapse: collapse' bordercolor='blue'")
                            .addColumn("Section")
                            .setSpanRows(true)
                            .setBreakSpans(true) // .setRepeatDivider(true)
                            .addColumn("Problems")
                            .setCellAttributes("style=\"text-align:left\" class=\"{2}\"")
                            .setSpanRows(true)
                            .addColumn("Subtype")
                            .setCellAttributes("style=\"text-align:left\" class=\"{2}\"")
                            .setSpanRows(true)
                            .addColumn("Locale")
                            .setCellAttributes("class=\"{2}\"")
                            .addColumn("Code")
                            .setCellAttributes("class=\"{2}\"")
                            .setCellPattern(
                                    "<a href=\"http://unicode.org/cldr/apps/survey?_={0}&x={1}\">{0}</a>") // TODO: use CLDRConfig.urls()
                            .addColumn("Count")
                            .setCellAttributes("class=\"{2}\"");
            for (Organization org : orgToLocales.keySet()) {
                indexTablePrinter.addColumn(org.toString().substring(0, 2));
            }

            for (Row.R4<String, ErrorType, Subtype, String> sectionAndProblemsAndLocale :
                    ErrorFile.sectionToProblemsToLocaleToCount.getKeysetSortedByKey()) {
                final ErrorType problem = sectionAndProblemsAndLocale.get1();
                final Subtype subtype = sectionAndProblemsAndLocale.get2();
                if (!ConsoleCheckCLDR.ErrorType.showInSummary.contains(problem)) {
                    continue;
                }
                final String locale = sectionAndProblemsAndLocale.get3();
                if (problem != ErrorType.error
                        && problem != ErrorType.disputed
                        && !orgToLocales.containsValue(locale)) {
                    continue;
                }
                long count =
                        ErrorFile.sectionToProblemsToLocaleToCount.getCount(
                                sectionAndProblemsAndLocale);
                final String section = sectionAndProblemsAndLocale.get0();
                indexTablePrinter
                        .addRow()
                        .addCell(section)
                        .addCell(problem)
                        .addCell(subtype)
                        .addCell(ConsoleCheckCLDR.getLocaleName(locale))
                        .addCell(locale)
                        .addCell(count);
                for (Organization org : orgToLocales.keySet()) {
                    indexTablePrinter.addCell(
                            orgToLocales.getAll(org).contains(locale)
                                    ? org.toString().substring(0, 2)
                                    : "");
                }
                indexTablePrinter.finishRow();
            }
            PrintWriter generated_html_index =
                    FileUtilities.openUTF8Writer(
                            ErrorFile.generated_html_directory, "sections.html");
            ConsoleCheckCLDR.ErrorFile.showIndexHead(
                    "Error Report Index by Section", "", generated_html_index);
            generated_html_index.println(indexTablePrinter.toTable());
            generated_html_index.println(ShowData.dateFooter());
            generated_html_index.println(CldrUtility.ANALYTICS);
            generated_html_index.println("</body></html>");
            generated_html_index.close();
        }

        static String formatSkippingZero(long count) {
            if (count == 0) {
                return "";
            }
            return String.valueOf(count);
        }

        static void showIndexHead(String title, String localeID, PrintWriter generated_html_index) {
            final boolean notLocaleSpecific = localeID.length() == 0;
            if ((!notLocaleSpecific)) {
                title = "Errors in " + ConsoleCheckCLDR.getNameAndLocale(localeID, false);
            }
            generated_html_index.println(
                    "<html>"
                            + "<head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>"
                            + CldrUtility.LINE_SEPARATOR
                            + "<title>"
                            + title
                            + "</title>"
                            + CldrUtility.LINE_SEPARATOR
                            + "<link rel='stylesheet' href='errors.css' type='text/css'>"
                            + CldrUtility.LINE_SEPARATOR
                            + "<base target='_blank'>"
                            + CldrUtility.LINE_SEPARATOR
                            + "</head><body>"
                            + CldrUtility.LINE_SEPARATOR
                            + "<h1>"
                            + title
                            + "</h1>"
                            + CldrUtility.LINE_SEPARATOR
                            + "<p>"
                            + "<a href='index.html"
                            + (notLocaleSpecific ? "" : "#" + localeID)
                            + "'>Index</a>"
                            + " | "
                            + "<a href='sections.html"
                            + (notLocaleSpecific ? "" : "#" + localeID)
                            + "'>Index by Section</a>"
                            + " | "
                            + "<a href='http://unicode.org/cldr/data/docs/survey/vetting.html'><b style='background-color: yellow;'><i>Help: How to Vet</i></b></a>"
                            + "</p>"
                            + "<p>The following errors have been detected in the locale"
                            + (notLocaleSpecific
                                    ? "s. "
                                            + org.unicode.cldr.test.HelpMessages.getChartMessages(
                                                    "error_index_header")
                                    : " "
                                            + ConsoleCheckCLDR.getNameAndLocale(localeID, false)
                                            + ". "
                                            + ErrorFile.ERROR_CHART_HEADER));
        }

        private static void writeErrorFileIndex() throws IOException {
            PrintWriter generated_html_index =
                    FileUtilities.openUTF8Writer(ErrorFile.generated_html_directory, "index.html");
            ConsoleCheckCLDR.ErrorFile.showIndexHead(
                    "Error Report Index", "", generated_html_index);
            ConsoleCheckCLDR.ErrorFile.showErrorFileIndex(generated_html_index);
            generated_html_index.close();
            showSections();
        }

        private static void writeErrorCountsText() {
            // do the plain text file
            ErrorFile.generated_html_count.print(ConsoleCheckCLDR.lastHtmlLocaleID + ";\tcounts");
            for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
                ErrorFile.generated_html_count.print(
                        ";\t" + type + "=" + ErrorFile.htmlErrorsPerLocale.getCount(type));
            }
            ErrorFile.generated_html_count.println();
            ErrorFile.generated_html_count.flush();

            // now store the data for the index
            ErrorFile.errorFileIndexData.put(
                    ConsoleCheckCLDR.lastHtmlLocaleID,
                    new Pair<>(ConsoleCheckCLDR.lastHtmlLocaleID, ErrorFile.htmlErrorsPerLocale));
            ErrorFile.htmlErrorsPerLocale = new Counter<>();
        }

        static Counter<ErrorType> htmlErrorsPerLocale =
                new Counter<>(); // ConsoleCheckCLDR.ErrorCount();
        static PrintWriter generated_html_count = null;
        private static TreeMap<String, Pair<String, Counter<ErrorType>>> errorFileIndexData =
                new TreeMap<>();

        static PrintWriter errorFileWriter = null;
        private static final String ERROR_CHART_HEADER =
                org.unicode.cldr.test.HelpMessages.getChartMessages("error_locale_header");
        static String generated_html_directory = null;
        public static Counter<Row.R4<String, ErrorType, Subtype, String>>
                sectionToProblemsToLocaleToCount = new Counter<>();
    }

    private static void showSummary(String localeID, Level level, String value) {
        String line = "# " + getLocaleAndName(localeID) + "\tSummary\t" + level + "\t" + value;
        System.out.println(line);
    }

    private static void showExamples(
            CLDRFile cldrFile,
            String prettyPath,
            String localeID,
            String path,
            String value,
            String fullPath,
            String example) {
        if (example != null) {
            showValue(
                    cldrFile,
                    prettyPath,
                    localeID,
                    example,
                    path,
                    value,
                    fullPath,
                    "ok",
                    Subtype.none);
        }
    }

    private static void addPrettyPaths(
            CLDRFile file,
            Matcher pathFilter,
            PathHeader.Factory pathHeaderFactory,
            boolean noaliases,
            boolean filterDraft,
            Collection<String> target) {
        for (Iterator<String> pit = file.iterator(pathFilter); pit.hasNext(); ) {
            String path = pit.next();
            addPrettyPath(file, pathHeaderFactory, noaliases, filterDraft, target, path);
        }
    }

    private static void addPrettyPaths(
            CLDRFile file,
            Collection<String> paths,
            Matcher pathFilter,
            PathHeader.Factory pathHeaderFactory,
            boolean noaliases,
            boolean filterDraft,
            Collection<String> target) {
        for (String path : paths) {
            if (pathFilter != null && !pathFilter.reset(path).matches()) continue;
            addPrettyPath(file, pathHeaderFactory, noaliases, filterDraft, target, path);
        }
    }

    private static void addPrettyPath(
            CLDRFile file,
            PathHeader.Factory pathHeaderFactory,
            boolean noaliases,
            boolean filterDraft,
            Collection<String> target,
            String path) {
        if (noaliases
                && XMLSource.Alias.isAliasPath(
                        path)) { // this is just for console testing, the survey tool
            // shouldn't do it.
            return;
        }
        if (filterDraft) {
            String newPath = CLDRFile.getNondraftNonaltXPath(path);
            if (!newPath.equals(path)) {
                String value = file.getStringValue(newPath);
                if (value != null) {
                    return;
                }
            }
        }
        String prettyPath = pathHeaderFactory.fromPath(path).toString();
        target.add(prettyPath);
    }

    private static synchronized void setExampleGenerator(ExampleGenerator inputExampleGenerator) {
        englishExampleGenerator = inputExampleGenerator;
    }

    private static synchronized ExampleGenerator getExampleGenerator() {
        return englishExampleGenerator;
    }

    private static ExampleGenerator englishExampleGenerator;

    static Matcher coverageMatcher =
            PatternCache.get("meet ([a-z]*) coverage").matcher(""); // HACK TODO fix

    private static void showHeaderLine() {
        if (SHOW_LOCALE) {
            if (idView) {
                System.out.println(
                        "Locale\tID\tDesc.\t〈Eng.Value〉\t【Eng.Ex.】\t〈Loc.Value〉\t【Loc.Ex】\t⁅error/warning type⁆\t❮Error/Warning Msg❯");
            } else {
                System.out.println(
                        "Locale\tStatus\t▸PPath◂\t〈Eng.Value〉\t【Eng.Ex.】\t〈Loc.Value〉\t«fill-in»\t【Loc.Ex】\t⁅error/warning type⁆\t❮Error/Warning Msg❯\tFull Path\tAliasedSource/Path?");
            }
        }
    }

    private static PathDescription pathDescription = null;

    private static String getIdString(String path, String value) {
        if (pathDescription == null) {
            pathDescription =
                    new PathDescription(
                            supplementalDataInfo,
                            english,
                            null,
                            null,
                            PathDescription.ErrorHandling.CONTINUE);
        }
        final String description = pathDescription.getDescription(path, value, null);
        return "\t" + StringId.getId(path) + "" + "\t" + description + "";
    }

    private static void showValue(
            CLDRFile cldrFile,
            String prettyPath,
            String localeID,
            String example,
            String path,
            String value,
            String fullPath,
            String statusString,
            Subtype subtype) {
        ErrorType shortStatus = ErrorType.fromStatusString(statusString);
        subtotalCount.add(shortStatus, 1);
        totalCount.add(shortStatus, 1);
        if (subtype == null) {
            subtype = Subtype.none;
        }
        final SourceLocation location =
                fullPath == null ? null : cldrFile.getSourceLocation(fullPath);

        if (ErrorFile.errorFileWriter == null) {
            example = example == null ? "" : example;
            String englishExample = null;
            final String englishPathValue = path == null ? null : getEnglishPathValue(path);
            if (SHOW_EXAMPLES && path != null) {
                englishExample =
                        ExampleGenerator.simplify(
                                getExampleGenerator().getExampleHtml(path, englishPathValue));
            }
            englishExample = englishExample == null ? "" : englishExample;
            String cleanPrettyPath = path == null ? null : prettyPath;
            Status status = new Status();
            String sourceLocaleID = path == null ? null : cldrFile.getSourceLocaleID(path, status);
            String fillinValue = path == null ? null : cldrFile.getFillInValue(path);
            fillinValue = fillinValue == null ? "" : fillinValue.equals(value) ? "=" : fillinValue;

            String pathLink = CLDR_CONFIG.urls().forXpath(localeID, path);

            final String otherSource =
                    path == null
                            ? null
                            : (sourceLocaleID.equals(localeID) ? "" : "\t" + sourceLocaleID);
            final String otherPath =
                    path == null
                            ? null
                            : (status.pathWhereFound.equals(path)
                                    ? ""
                                    : "\t" + status.pathWhereFound);
            if (location != null) {
                System.err.println(location.toString() + shortStatus); // print full path here
            }
            String idViewString =
                    idView ? (path == null ? "\tNO_ID" : getIdString(path, value)) : "";
            System.out.println(
                    getLocaleAndName(localeID)
                            + (idViewString.isEmpty()
                                    ?
                                    // + "\t" + subtotalCount.getCount(shortStatus)
                                    "\t"
                                            + shortStatus
                                            + "\t▸"
                                            + cleanPrettyPath
                                            + "◂"
                                            + "\t〈"
                                            + englishPathValue
                                            + "〉"
                                            + "\t【"
                                            + englishExample
                                            + "】"
                                            + "\t〈"
                                            + value
                                            + "〉"
                                            + "\t«"
                                            + fillinValue
                                            + "»"
                                            + "\t【"
                                            + example
                                            + "】"
                                            + "\t⁅"
                                            + subtype
                                            + "⁆"
                                            + "\t❮"
                                            + statusString
                                            + "❯"
                                            + "\t"
                                            + pathLink
                                            + otherSource
                                            + otherPath
                                    : idViewString
                                            + "\t〈"
                                            + englishPathValue
                                            + "〉"
                                            + "\t【"
                                            + englishExample
                                            + "】"
                                            + "\t"
                                            + value
                                            + "〉"
                                            + "\t【"
                                            + example
                                            + "】"
                                            + "\t⁅"
                                            + subtype
                                            + "⁆"
                                            + "\t❮"
                                            + statusString
                                            + "❯"));
        } else if (ErrorFile.errorFileWriter != null) {
            if (shortStatus == ErrorType.contributed) {
                return;
            }
            if (shortStatus == ErrorType.posix) {
                shortStatus = ErrorType.minimal;
            }
            if (!localeID.equals(lastHtmlLocaleID)) {
                ErrorFile.writeErrorCountsText();
                lastHtmlLocaleID = localeID;
            }
            addError(shortStatus);
            ErrorFile.addDataToErrorFile(localeID, path, shortStatus, subtype);
        }
        if (CLDR_GITHUB_ANNOTATIONS) {
            // Annotate anything that needs annotation
            if (shortStatus == ErrorType.error || shortStatus == ErrorType.warning) {
                String filePath = null;
                if (location != null) {
                    // Use accurate location
                    filePath =
                            location.forGitHub(
                                    CLDRPaths.BASE_DIRECTORY); // Trim to CLDR_DIR for GitHub
                } else {
                    // Fallback if SourceLocation fails
                    filePath =
                            "file="
                                    + localeXpathToFilePath.computeIfAbsent(
                                            Pair.of(localeID, path),
                                            locPath -> guessFilePath(locPath));
                }
                System.out.println(
                        "::"
                                + shortStatus
                                + " "
                                + filePath.trim()
                                + ",title="
                                + subtype
                                + ":: "
                                + statusString);
            }
        }
        if (PATH_IN_COUNT && ErrorFile.generated_html_count != null) {
            ErrorFile.generated_html_count.println(lastHtmlLocaleID + ";\tpath:\t" + path);
        }
    }

    private static void addError(ErrorType shortStatus) {
        if (ErrorType.showInSummary.contains(shortStatus)) {
            ErrorFile.htmlErrorsPerLocale.increment(shortStatus);
        }
    }

    static String lastHtmlLocaleID = "";
    private static VoterInfoList voterInfoList;
    private static VoteResolver<String> voteResolver;
    private static String resolveVotesDirectory;
    private static boolean idView;
    private static SupplementalDataInfo supplementalDataInfo;
    private static CLDRFile english;

    public static class PathShower {
        String localeID;
        boolean newLocale = true;
        String lastPath;
        String[] lastSplitPath;
        boolean showEnglish;
        String splitChar = "/";

        static final String lead = "****************************************";

        public void set(String localeID) {
            this.localeID = localeID;
            newLocale = true;
            LocaleIDParser localeIDParser = new LocaleIDParser();
            showEnglish = !localeIDParser.set(localeID).getLanguageScript().equals("en");
            lastPath = null;
            lastSplitPath = null;
        }

        public String getSplitChar() {
            return splitChar;
        }

        public PathShower setSplitChar(String splitChar) {
            this.splitChar = splitChar;
            return this;
        }
    }

    private static String getEnglishPathValue(String path) {
        String englishValue = CheckCLDR.getDisplayInformation().getWinningValue(path);
        if (englishValue == null) {
            String path2 = CLDRFile.getNondraftNonaltXPath(path);
            englishValue = CheckCLDR.getDisplayInformation().getWinningValue(path2);
        }
        return englishValue;
    }

    /**
     * Utility for getting information.
     *
     * @param locale
     * @return
     */
    private static String getLocaleAndName(String locale) {
        String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
        if (localizedName == null || localizedName.equals(locale)) return locale;
        return locale + " [" + localizedName + "]";
    }

    /**
     * Utility for getting information.
     *
     * @param locale
     * @param linkToXml TODO
     * @return
     */
    private static String getNameAndLocale(String locale, boolean linkToXml) {
        String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
        if (localizedName == null || localizedName.equals(locale)) return locale;
        if (linkToXml) {
            locale =
                    "<a href='https://github.com/unicode-org/cldr/tree/main/common/main/"
                            + locale
                            + ".xml'>"
                            + locale
                            + "</a>";
        }
        return localizedName + " [" + locale + "]";
    }

    private static String getLocaleName(String locale) {
        String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
        if (localizedName == null || localizedName.equals(locale)) return locale;
        return localizedName;
    }

    private static String getLinkedLocale(String locale) {
        return "<a href='http://unicode.org/cldr/apps/survey?_=" + locale + "'>" + locale + "</a>";
    }

    /** Approximate xml path */
    private static String guessFilePath(Pair<String, String> locPath) {
        final File base = new File(CLDRPaths.BASE_DIRECTORY);
        final String loc = locPath.getFirst();
        final String path = locPath.getSecond();
        String subdir = "main";
        if (path.startsWith("//ldml/annotations")) {
            subdir = "annotations";
        } else if (path.startsWith("//ldml/subdivisions")) {
            subdir = "subdivisions";
        }
        File inCommon = new File(base, "common");
        File subsub = new File(inCommon, subdir);
        if (subsub.isDirectory()) {
            File subFile = new File(subsub, loc + ".xml");
            if (subFile.canRead())
                return subFile.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
        }

        File inSeed = new File(base, "seed");
        subsub = new File(inSeed, subdir);
        if (subsub.isDirectory()) {
            File subFile = new File(subsub, loc + ".xml");
            if (subFile.canRead())
                return subFile.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
        }
        return loc + ".xml";
    }

    static final ConcurrentHashMap<Pair<String, String>, String> localeXpathToFilePath =
            new ConcurrentHashMap<>();
}
