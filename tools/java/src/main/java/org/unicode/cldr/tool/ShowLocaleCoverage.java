package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR.InputMethod;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.PathValueInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoreCoverageInfo;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.LookupType;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class ShowLocaleCoverage {
    private static final String VXML_CONSTANT = CLDRPaths.AUX_DIRECTORY + "voting/" + CLDRFile.GEN_VERSION + "/vxml/common/";
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final String TSV_MISSING_SUMMARY_HEADER =
        "#Path Level"
            + "\t#Locales"
            + "\tLocales"
            + "\tSection"
            + "\tPage"
            + "\tHeader"
            + "\tCode"
            ;
    private static final String TSV_LOCALE_COVERAGE_HEADER =
        "#Dir"
            + "\tCode"
            + "\tEnglish Name"
            + "\tNative Name"
            + "\tScript"
            + "\tCLDR Target"
            + "\tSublocales"
            + "\tFields\tUC\tMissing"
            + "\tModern\tMiss +UC"
            + "\tModerate\tMiss +UC"
            + "\tBasic\tMiss +UC"
            + "\tCore\tMiss +UC"
            + "\tCore-Missing";

    private static final String TSV_MISSING_HEADER =
        "#LCode"
            + "\tEnglish Name"
            + "\tScript"
            //        + "\tEnglish Value"
            //        + "\tNative Value"
            + "\tLocale Level"
            + "\tPath Level"
            //        + "\tStatus"
            //        + "\tAction"
            + "\tSTStatus"
            + "\tBailey"
            + "\tVxml"
            + "\tVStatus"
            //        + "\tST Link"
            + "\tSection"
            + "\tPage"
            + "\tHeader"
            + "\tCode"
            + "\tST Link"
            + "\tConfig Action"
            ;
    private static final String TSV_MISSING_BASIC_HEADER = "";

    private static final boolean DEBUG = true;
    private static final char DEBUG_FILTER = 0; // use letter to only load locales starting with that letter

    private static final String LATEST = ToolConstants.CHART_VERSION;
    private static final double CORE_SIZE = CoreItems.values().length - CoreItems.ONLY_RECOMMENDED.size();
    public static CLDRConfig testInfo = ToolConfig.getToolInstance();
    private static final StandardCodes SC = StandardCodes.make();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    private static final StandardCodes STANDARD_CODES = SC;

    static org.unicode.cldr.util.Factory factory = testInfo.getCommonAndSeedAndMainAndAnnotationsFactory();
    private static final CLDRFile ENGLISH = factory.make("en", true);

    private static UnicodeSet ENG_ANN = Annotations.getData("en").keySet();

    // added info using pattern in VettingViewer.

    static final RegexLookup<Boolean> HACK = RegexLookup.<Boolean> of(LookupType.STANDARD, RegexLookup.RegexFinderTransformPath)
        .add("//ldml/localeDisplayNames/keys/key[@type=\"(d0|em|fw|i0|k0|lw|m0|rg|s0|ss|t0|x0)\"]", true)
        .add("//ldml/localeDisplayNames/types/type[@key=\"(em|fw|kr|lw|ss)\"].*", true)
        .add("//ldml/localeDisplayNames/languages/language[@type=\".*_.*\"]", true)
        .add("//ldml/localeDisplayNames/languages/language[@type=\".*\"][@alt=\".*\"]", true)
        .add("//ldml/localeDisplayNames/territories/territory[@type=\".*\"][@alt=\".*\"]", true)
        .add("//ldml/localeDisplayNames/territories/territory[@type=\"EZ\"]", true);

    //private static final String OUT_DIRECTORY = CLDRPaths.GEN_DIRECTORY + "/coverage/"; // CldrUtility.MAIN_DIRECTORY;

    final static Options myOptions = new Options();

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument."),
        //        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft status."),
        chart(null, null, "chart only"),
        growth("true", "true", "Compute growth data"),
        organization(".+", null, "Only locales for organization"),
        version(".+",
            LATEST, "To get different versions"),
        rawData(null, null, "Output the raw data from all coverage levels"),
        targetDir(".*",
            CLDRPaths.GEN_DIRECTORY + "/statistics/", "target output file."),
        directories("(.*:)?[a-z]+(,[a-z]+)*", "common",
            "Space-delimited list of main source directories: common,seed,exemplar.\n" +
            "Optional, <baseDir>:common,seed"),;

        // targetDirectory(".+", CldrUtility.CHART_DIRECTORY + "keyboards/", "The target directory."),
        // layouts(null, null, "Only create html files for keyboard layouts"),
        // repertoire(null, null, "Only create html files for repertoire"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static final RegexLookup<Boolean> SUPPRESS_PATHS_CAN_BE_EMPTY = new RegexLookup<Boolean>()
        .add("\\[@alt=\"accounting\"]", true)
        .add("\\[@alt=\"variant\"]", true)
        .add("^//ldml/localeDisplayNames/territories/territory.*@alt=\"short", true)
        .add("^//ldml/localeDisplayNames/languages/language.*_", true)
        .add("^//ldml/numbers/currencies/currency.*/symbol", true)
        .add("^//ldml/characters/exemplarCharacters", true);

    static DraftStatus minimumDraftStatus = DraftStatus.unconfirmed;
    static final Factory pathHeaderFactory = PathHeader.getFactory(ENGLISH);

    static boolean RAW_DATA = true;
    private static Set<String> COMMON_LOCALES;

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.filter, args, true);

        Matcher matcher = PatternCache.get(MyOptions.filter.option.getValue()).matcher("");

        if (MyOptions.chart.option.doesOccur()) {
            showCoverage(null, matcher);
            return;
        }


        if (MyOptions.growth.option.doesOccur()) {
            try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-growth.tsv")) {
                doGrowth(matcher, out);
                return;
            }
        }

        Set<String> locales = null;
        String organization = MyOptions.organization.option.getValue();
        boolean useOrgLevel = MyOptions.organization.option.doesOccur();
        if (useOrgLevel) {
            locales = STANDARD_CODES.getLocaleCoverageLocales(organization);
        }

        if (MyOptions.version.option.doesOccur()) {
            String number = MyOptions.version.option.getValue().trim();
            if (!number.contains(".")) {
                number += ".0";
            }
            factory = org.unicode.cldr.util.Factory.make(
                CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + number + "/common/main/", ".*");
        } else {
            if (MyOptions.directories.option.doesOccur()) {
                String directories = MyOptions.directories.option.getValue().trim();
                CLDRConfig cldrConfig = CONFIG;
                String base = null;
                int colonPos = directories.indexOf(':');
                if (colonPos >= 0) {
                    base = directories.substring(0, colonPos).trim();
                    directories = directories.substring(colonPos + 1).trim();
                } else {
                    base = cldrConfig.getCldrBaseDirectory().toString();
                }
                String[] items = directories.split(",\\s*");
                File[] fullDirectories = new File[items.length];
                int i = 0;
                for (String item : items) {
                    fullDirectories[i++] = new File(base + "/" + item + "/main");
                }
                factory = SimpleFactory.make(fullDirectories, ".*");
                COMMON_LOCALES = SimpleFactory.make(base + "/" + "common" + "/main", ".*").getAvailableLanguages();
            }
        }
        fixCommonLocales();

        RAW_DATA = MyOptions.rawData.option.doesOccur();

        //showEnglish();

        showCoverage(null, matcher, locales, useOrgLevel);
    }

    public static void fixCommonLocales() {
        if (COMMON_LOCALES == null) {
            COMMON_LOCALES = factory.getAvailableLanguages();
        }
    }

    private static void doGrowth(Matcher matcher, PrintWriter out) {
        TreeMap<String, List<Double>> growthData = new TreeMap<>(Ordering.natural().reverse()); // sort by version, descending
//        if (DEBUG) {
//            for (String dir : new File(CLDRPaths.ARCHIVE_DIRECTORY).list()) {
//                if (!dir.startsWith("cldr")) {
//                    continue;
//                }
//                String version = getNormalizedVersion(dir);
//                if (version == null) {
//                    continue;
//                }
//                org.unicode.cldr.util.Factory newFactory = org.unicode.cldr.util.Factory.make(
//                    CLDRPaths.ARCHIVE_DIRECTORY + "/" + dir + "/common/main/", ".*");
//                System.out.println("Reading: " + version);
//                Map<String, FoundAndTotal> currentData = addGrowth(newFactory, matcher);
//                System.out.println("Read: " + version + "\t" + currentData);
//                break;
//            }
//        }
        Map<String, FoundAndTotal> latestData = null;
        for (ReleaseInfo versionNormalizedVersionAndYear : versionToYear) {
            VersionInfo version = versionNormalizedVersionAndYear.version;
            int year = versionNormalizedVersionAndYear.year;
            String dir = ToolConstants.getBaseDirectory(version.getVersionString(2, 3));
            Map<String, FoundAndTotal> currentData = addGrowth(factory, dir, matcher, false);
            System.out.println("year: " + year + "; version: " + version + "; size: " + currentData);
            if (latestData == null) {
                latestData = currentData;
            }
            Counter2<String> completionData = getCompletion(latestData, currentData);
            addCompletionList(year+"", completionData, growthData);
            if (DEBUG) System.out.println(currentData);
        }
//        Map<String, FoundAndTotal> latestData = addGrowth(factory, null, matcher, false);
//        addCompletionList(getYearFromVersion(LATEST, false), getCompletion(latestData, latestData), growthData);
//        if (DEBUG) System.out.println(latestData);
//        //System.out.println(growthData);
//        List<String> dirs = new ArrayList<>(Arrays.asList(new File(CLDRPaths.ARCHIVE_DIRECTORY).list()));
//        Collections.reverse(dirs);
//        for (String dir : dirs) {
//            if (!dir.startsWith("cldr")) {
//                continue;
//            }
//            String version = getNormalizedVersion(dir);
//            if (version == null) {
//                continue;
//            }
////            if (version.compareTo("12") < 0) {
////                continue;
////            }
//            System.out.println("Reading: " + version);
//            if (version.equals("2008")) {
//                int debug = 0;
//            }
//            Map<String, FoundAndTotal> currentData = addGrowth(factory, dir, matcher, false);
//            System.out.println("Read: " + version + "\t" + currentData);
//            Counter2<String> completionData = getCompletion(latestData, currentData);
//            //System.out.println(version + "\t" + completionData);
//            addCompletionList(version, completionData, growthData);
//            if (DEBUG) System.out.println(currentData);
//        }
        boolean first = true;
        for (Entry<String, List<Double>> entry : growthData.entrySet()) {
            if (first) {
                for (int i = 0; i < entry.getValue().size(); ++i) {
                    out.print("\t" + i);
                }
                out.println();
                first = false;
            }
            out.println(entry.getKey() + "\t" + Joiner.on("\t").join(entry.getValue()));
        }
    }

    static final class ReleaseInfo {
        public ReleaseInfo(VersionInfo versionInfo, int year) {
            this.version = versionInfo;
            this.year = year;
        }
        VersionInfo version;
        int year;
    }

    // TODO merge this into ToolConstants, and have the version expressed as VersionInfo.
    static final List<ReleaseInfo> versionToYear;
    static {
        Object[][] mapping = {
            { VersionInfo.getInstance(37), 2020 },
            { VersionInfo.getInstance(36), 2019 },
            { VersionInfo.getInstance(34), 2018 },
            { VersionInfo.getInstance(32), 2017 },
            { VersionInfo.getInstance(30), 2016 },
            { VersionInfo.getInstance(28), 2015 },
            { VersionInfo.getInstance(26), 2014 },
            { VersionInfo.getInstance(24), 2013 },
            { VersionInfo.getInstance(22,1), 2012 },
            { VersionInfo.getInstance(2,0,1), 2011 },
            { VersionInfo.getInstance(1,9,1), 2010 },
            { VersionInfo.getInstance(1,7,2), 2009 },
            { VersionInfo.getInstance(1,6,1), 2008 },
            { VersionInfo.getInstance(1,5,1), 2007 },
            { VersionInfo.getInstance(1,4,1), 2006 },
            { VersionInfo.getInstance(1,3), 2005 },
            { VersionInfo.getInstance(1,2), 2004 },
            { VersionInfo.getInstance(1,1,1), 2003 },
        };
        List<ReleaseInfo> _versionToYear = new ArrayList<>();
        for (Object[] row : mapping) {
            _versionToYear.add(new ReleaseInfo((VersionInfo)row[0], (int)row[1]));
        }
        versionToYear = ImmutableList.copyOf(_versionToYear);
    }

//    public static String getNormalizedVersion(String dir) {
//        String rawVersion = dir.substring(dir.indexOf('-') + 1);
//        int firstDot = rawVersion.indexOf('.');
//        int secondDot = rawVersion.indexOf('.', firstDot + 1);
//        if (secondDot > 0) {
//            rawVersion = rawVersion.substring(0, firstDot) + rawVersion.substring(firstDot + 1, secondDot);
//        } else {
//            rawVersion = rawVersion.substring(0, firstDot);
//        }
//        String result = getYearFromVersion(rawVersion, true);
//        return result == null ? null : result.toString();
//    }

//    private static String getYearFromVersion(String version, boolean allowNull) {
//        String result = versionToYear.get(version);
//        if (!allowNull && result == null) {
//            throw new IllegalArgumentException("No year for version: " + version);
//        }
//        return result;
//    }
//
//    private static String getVersionFromYear(String year, boolean allowNull) {
//        String result = versionToYear.inverse().get(year);
//        if (!allowNull && result == null) {
//            throw new IllegalArgumentException("No version for year: " + year);
//        }
//        return result;
//    }

    public static void addCompletionList(String version, Counter2<String> completionData, TreeMap<String, List<Double>> growthData) {
        List<Double> x = new ArrayList<>();
        for (String key : completionData.getKeysetSortedByCount(false)) {
            x.add(completionData.getCount(key));
        }
        growthData.put(version, x);
        System.out.println(version + "\t" + x.size());
    }

    public static Counter2<String> getCompletion(Map<String, FoundAndTotal> latestData, Map<String, FoundAndTotal> currentData) {
        Counter2<String> completionData = new Counter2<>();
        for (Entry<String, FoundAndTotal> entry : latestData.entrySet()) {
            final String locale = entry.getKey();
            final FoundAndTotal currentRecord = currentData.get(locale);
            if (currentRecord == null) {
                continue;
            }
            double total = entry.getValue().total;
            if (total == 0) {
                continue;
            }
            double completion = currentRecord.found / total;
            completionData.add(locale, completion);
        }
        return completionData;
    }

    static class FoundAndTotal {
        final int found;
        final int total;

        public FoundAndTotal(Counter<Level>... counters) {
            final int[] count = { 0, 0, 0 };
            for (Level level : Level.values()) {
                if (level == Level.COMPREHENSIVE || level == Level.OPTIONAL) {
                    continue;
                }
                int i = 0;
                for (Counter<Level> counter : counters) {
                    count[i++] += counter.get(level);
                }
            }
            found = count[0];
            total = found + count[1] + count[2];
        }

        @Override
        public String toString() {
            return found + "/" + total;
        }
    }

    private static Map<String, FoundAndTotal> addGrowth(org.unicode.cldr.util.Factory latestFactory, String dir, Matcher matcher, boolean showMissing) {
        final File mainDir = new File(dir + "/common/main/");
        final File annotationDir = new File(dir + "/common/annotations/");
        File[] paths = annotationDir.exists() ? new File[] {mainDir, annotationDir} : new File[] {mainDir};
        org.unicode.cldr.util.Factory newFactory = SimpleFactory.make(paths, ".*");
        Map<String, FoundAndTotal> data = new HashMap<>();
        char c = 0;
        Set<String> latestAvailable = newFactory.getAvailableLanguages();
        for (String locale : newFactory.getAvailableLanguages()) {
            if (!matcher.reset(locale).matches()) {
                continue;
            }
            if (!latestAvailable.contains(locale)) {
                continue;
            }
            if (SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales().contains(locale)
                || locale.equals("root")
                || locale.equals("supplementalData")) {
                continue;
            }
            char nc = locale.charAt(0);
            if (nc != c) {
                System.out.println("\t" + locale);
                c = nc;
            }
            if (DEBUG_FILTER != 0 && DEBUG_FILTER != nc) {
                continue;
            }
            CLDRFile latestFile = null;
            try {
                latestFile = latestFactory.make(locale, true);
            } catch (Exception e2) {
                continue;
            }
            final CLDRFile file = newFactory.make(locale, true);
            // HACK check bogus
//            Collection<String> extra = file.getExtraPaths();
//
//            final Iterable<String> fullIterable = file.fullIterable();
//            for (String path : fullIterable) {
//                if (path.contains("\"one[@")) {
//                    boolean inside = extra.contains(path);
//                    Status status = new Status();
//                    String loc = file.getSourceLocaleID(path, status );
//                    int debug = 0;
//                }
//            }
            // END HACK
            Counter<Level> foundCounter = new Counter<>();
            Counter<Level> unconfirmedCounter = new Counter<>();
            Counter<Level> missingCounter = new Counter<>();
            Set<String> unconfirmedPaths = null;
            Relation<MissingStatus, String> missingPaths = null;
            unconfirmedPaths = new LinkedHashSet<>();
            missingPaths = Relation.of(new LinkedHashMap(), LinkedHashSet.class);
            VettingViewer.getStatus(latestFile.fullIterable(), file,
                pathHeaderFactory, foundCounter, unconfirmedCounter,
                missingCounter, missingPaths, unconfirmedPaths);

            // HACK
            Set<Entry<MissingStatus, String>> missingRemovals = new HashSet<>();
            for (Entry<MissingStatus, String> e : missingPaths.keyValueSet()) {
                if (e.getKey() == MissingStatus.ABSENT) {
                    final String path = e.getValue();
                    if (HACK.get(path) != null) {
                        missingRemovals.add(e);
                        missingCounter.add(Level.MODERN, -1);
                        foundCounter.add(Level.MODERN, 1);
                    } else {
                        Status status = new Status();
                        String loc = file.getSourceLocaleID(path, status);
                        int debug = 0;
                    }
                }
            }
            for (Entry<MissingStatus, String> e : missingRemovals) {
                missingPaths.remove(e.getKey(), e.getValue());
            }
            // END HACK

            if (showMissing) {
                int count = 0;
                for (String s : unconfirmedPaths) {
                    System.out.println(++count + "\t" + locale + "\tunconfirmed\t" + s);
                }
                for (Entry<MissingStatus, String> e : missingPaths.keyValueSet()) {
                    String path = e.getValue();
                    Status status = new Status();
                    String loc = file.getSourceLocaleID(path, status);
                    int debug = 0;

                    System.out.println(++count + "\t" + locale + "\t" + CldrUtility.toString(e));
                }
                int debug = 0;
            }

            data.put(locale, new FoundAndTotal(foundCounter, unconfirmedCounter, missingCounter));
        }
        return Collections.unmodifiableMap(data);
    }

    public static void showCoverage(Anchors anchors, Matcher matcher) throws IOException {
        showCoverage(anchors, matcher, null, false);
    }

    public static void showCoverage(Anchors anchors, Matcher matcher, Set<String> locales, boolean useOrgLevel) throws IOException {
        final String title = "Locale Coverage";
        try (PrintWriter pw = new PrintWriter(new FormattedFileWriter(null, title, null, anchors));
            PrintWriter tsv_summary = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-coverage.tsv");
            PrintWriter tsv_missing = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing.tsv");
            PrintWriter tsv_missing_summary = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing-summary.tsv");
            PrintWriter tsv_missing_basic = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing-basic.tsv");
            ){
            tsv_summary.println(TSV_LOCALE_COVERAGE_HEADER);
            tsv_missing_summary.println(TSV_MISSING_SUMMARY_HEADER);
            tsv_missing.println(TSV_MISSING_HEADER);
            tsv_missing_basic.println(TSV_MISSING_BASIC_HEADER);

            Set<String> checkModernLocales = STANDARD_CODES.getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
            Set<String> availableLanguages = new TreeSet<>(factory.getAvailableLanguages());
            availableLanguages.addAll(checkModernLocales);
            Relation<String, String> languageToRegion = Relation.of(new TreeMap(), TreeSet.class);
            LanguageTagParser ltp = new LanguageTagParser();
            LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer(true);
            for (String locale : factory.getAvailable()) {
                String country = ltp.set(locale).getRegion();
                if (!country.isEmpty()) {
                    languageToRegion.put(ltc.transform(ltp.getLanguageScript()), country);
                }
            }

            fixCommonLocales();

            System.out.println(Joiner.on("\n").join(languageToRegion.keyValuesSet()));

            System.out.println("# Checking: " + availableLanguages);

            pw.println("<p style='text-align: left'>This chart shows the coverage levels for this release. </p>" +
                "<ol>"
                + "<li>Fields = fields found at a modern level</li>"
                + "<li>UC = unconfirmed values: typically treated as missing by implementations</li>"
                + "<li>Miss = missing values</li>"
                + "<li>Modern%, etc = fields/(fields + missing + unconfirmed) — at that level</li>"
                + "<li>Core Missing = missing core fields — optionals marked with *</li></ol>"
                + "<p>A high-level summary of the meaning of the coverage values are at " +
                "<a target='_blank' href='http://www.unicode.org/reports/tr35/tr35-info.html#Coverage_Levels'>Coverage Levels</a>. " +
                "The Core values are described on " +
                "<a target='_blank' href='http://cldr.unicode.org/index/cldr-spec/minimaldata'>Core Data</a>." +
                "</p>");

            Relation<MissingStatus, String> missingPaths = Relation.of(new EnumMap<MissingStatus, Set<String>>(
                MissingStatus.class), TreeSet.class, CLDRFile.getComparator(DtdType.ldml));
            Set<String> unconfirmed = new TreeSet<>(CLDRFile.getComparator(DtdType.ldml));

            //Map<String, String> likely = testInfo.getSupplementalDataInfo().getLikelySubtags();
            Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();

            // Map<String,Counter<Level>> counts = new HashMap();
            //        System.out.print("Script\tEnglish\tNative\tCode\tCode*");
            //        for (Level level : Level.values()) {
            //            if (skipPrintingLevels.contains(level)) {
            //                continue;
            //            }
            //            System.out.print("\t≤" + level + " (f)\t(u)\t(m)");
            //        }
            //        System.out.println();
            // Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getCldrFactory().make("en", true));

            Counter<Level> foundCounter = new Counter<>();
            Counter<Level> unconfirmedCounter = new Counter<>();
            Counter<Level> missingCounter = new Counter<>();

            List<Level> levelsToShow = new ArrayList<>(EnumSet.allOf(Level.class));
            levelsToShow.remove(Level.COMPREHENSIVE);
            levelsToShow.remove(Level.UNDETERMINED);
            levelsToShow = ImmutableList.copyOf(levelsToShow);
            List<Level> reversedLevels = new ArrayList<>(levelsToShow);
            Collections.reverse(reversedLevels);
            reversedLevels = ImmutableList.copyOf(reversedLevels);


            //        PrintWriter out2;
            //        try {
            //            out2 = FileUtilities.openUTF8Writer(CLDRPaths.CHART_DIRECTORY + "tsv/", "showLocaleCoverage.tsv");
            //        } catch (IOException e1) {
            //            throw new ICUUncheckedIOException(e1);
            //        }
            //
            //        out2.print("Code\tCom?\tEnglish Name\tNative Name\tScript\tSublocales\tStrings");
            //        for (Level level : reversedLevels) {
            //            out2.print("\t" + level + " %\t" + level + " UC%");
            //        }
            //        out2.println();
            //System.out.println("\tCore*\nCore* Missing");
            int localeCount = 0;

            final TablePrinter tablePrinter = new TablePrinter()
                .addColumn("Direct.", "class='source'", null, "class='source'", true)
                .setBreakSpans(true).setSpanRows(false)
                .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true).setBreakSpans(true)
                .addColumn("English Name", "class='source'", null, "class='source'", true).setBreakSpans(true)
                .addColumn("Native Name", "class='source'", null, "class='source'", true).setBreakSpans(true)
                .addColumn("Script", "class='source'", null, "class='source'", true).setBreakSpans(true)
                .addColumn("CLDR target", "class='source'", null, "class='source'", true).setBreakSpans(true).setSortPriority(0).setSortAscending(false)
                .addColumn("ICU", "class='source'", null, "class='source'", true).setBreakSpans(true).setSortPriority(1).setSortAscending(false)
                .addColumn("Sublocales", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
                .setCellPattern("{0,number}")
                .addColumn("Fields", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
                .setCellPattern("{0,number}")
                .addColumn("UC", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
                .setCellPattern("{0,number}")
                .addColumn("Miss", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
                .setCellPattern("{0,number}")
                //.addColumn("Target Level", "class='target'", null, "class='target'", true).setBreakSpans(true)
                ;

            NumberFormat tsvPercent = NumberFormat.getPercentInstance(Locale.ENGLISH);
            tsvPercent.setMaximumFractionDigits(2);

            for (Level level : reversedLevels) {
                String titleLevel = level.toString();
                tablePrinter.addColumn(UCharacter.toTitleCase(titleLevel, null) + "%", "class='target'", null, "class='targetRight'", true)
                .setCellPattern("{0,number,0.0%}")
                .setBreakSpans(true);
                switch(level) {
                case CORE:
                    tablePrinter.setSortPriority(5).setSortAscending(false);
                    break;
                case BASIC:
                    tablePrinter.setSortPriority(4).setSortAscending(false);
                    break;
                case MODERATE:
                    tablePrinter.setSortPriority(3).setSortAscending(false);
                    break;
                case MODERN:
                    tablePrinter.setSortPriority(2).setSortAscending(false);
                    break;
                }
                //            tablePrinter
                //            .addColumn("∪ UC%", "class='target'", null, "class='targetRight'", true)
                //            .setCellPattern("{0,number,0.0%}")
                //            .setBreakSpans(true)

            }
            tablePrinter.addColumn("Core Missing", "class='target'", null, "class='targetRight'", true)
            .setBreakSpans(true);

            long start = System.currentTimeMillis();
            LikelySubtags likelySubtags = new LikelySubtags();

            EnumMap<Level, Double> targetLevel = new EnumMap<>(Level.class);
            targetLevel.put(Level.CORE, 2 / 100d);
            targetLevel.put(Level.BASIC, 16 / 100d);
            targetLevel.put(Level.MODERATE, 33 / 100d);
            targetLevel.put(Level.MODERN, 100 / 100d);

            //        NumberFormat percentFormat = NumberFormat.getPercentInstance(ULocale.ENGLISH);
            //        percentFormat.setMaximumFractionDigits(2);
            //        percentFormat.setMinimumFractionDigits(2);
            //        NumberFormat intFormat = NumberFormat.getIntegerInstance(ULocale.ENGLISH);

            Multimap<String, String> pathToLocale = TreeMultimap.create();

            int counter = 0;
            for (String locale : availableLanguages) {
                try {
                    if (locale.contains("supplemental") // for old versionsl
                        || locale.startsWith("sr_Latn")) {
                        continue;
                    }
                    if (locales != null && !locales.contains(locale)) {
                        String base = CLDRLocale.getInstance(locale).getLanguage();
                        if (!locales.contains(base)) {
                            continue;
                        }
                    }
                    if (matcher != null && !matcher.reset(locale).matches()) {
                        continue;
                    }
                    if (defaultContents.contains(locale) || "root".equals(locale) || "und".equals(locale)) {
                        continue;
                    }

                    CLDRFile vxmlCldrFile2 = null; // getVxmlCldrFile(locale); TODO clean this up

                    tsv_summary.flush();
                    tsv_missing_summary.flush();
                    tsv_missing.flush();
                    tsv_missing_basic.flush();

                    boolean isSeed = new File(CLDRPaths.SEED_DIRECTORY, locale + ".xml").exists();

                    //boolean capture = locale.equals("en");
                    String region = ltp.set(locale).getRegion();
                    if (!region.isEmpty()) continue; // skip regions

                    final Level cldrLocaleLevelGoal = SC.getLocaleCoverageLevel(Organization.cldr.toString(), locale);
                    final boolean cldrLevelGoalBasicToModern = Level.CORE_TO_MODERN.contains(cldrLocaleLevelGoal);

                    String isCommonLocale = Level.MODERN == cldrLocaleLevelGoal ? "C*"
                        : COMMON_LOCALES.contains(locale) ? "C"
                            : "";

                    String max = likelySubtags.maximize(locale);
                    String script = ltp.set(max).getScript();

                    String language = likelySubtags.minimize(locale);
                    //                Level otherLevel = STANDARD_CODES.getLocaleCoverageLevel("apple", locale);
                    //                if (otherLevel.compareTo(currentLevel) > 0
                    //                    && otherLevel.compareTo(Level.MODERN) <= 0) {
                    //                    currentLevel = otherLevel;
                    //                }

                    missingPaths.clear();
                    unconfirmed.clear();

                    final CLDRFile file = factory.make(locale, true, minimumDraftStatus);

                    if (locale.equals("af")) {
                        int debug = 0;
                    }

                    Iterable<String> pathSource = new IterableFilter(file.fullIterable());

                    VettingViewer.getStatus(pathSource, file,
                        pathHeaderFactory, foundCounter, unconfirmedCounter,
                        missingCounter, missingPaths, unconfirmed);

                    // HACK Fix up missing items. Remove once vxml is ok.
                    if (vxmlCldrFile != null) {
                        Multimap<MissingStatus,String> toRemove = HashMultimap.create();
                        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                            String mPath = entry.getValue();
                            String vxmlValue = vxmlCldrFile.getStringValue(mPath);
                            if (vxmlValue != null) {
                                if (vxmlValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                                    vxmlValue = vxmlCldrFile.getBaileyValue(mPath, null, null);
                                }
                                if (vxmlValue != null) {
                                    String bailey = file.getStringValue(mPath);
                                    if (vxmlValue.equals(bailey)) {
                                        String fullPath = vxmlCldrFile.getFullXPath(mPath);
                                        if (!fullPath.contains("provisional") && !fullPath.contains("unconfirmed")) {
                                            toRemove.put(entry.getKey(), mPath);
                                            Level level = coverageInfo.getCoverageLevel(mPath, locale);
                                            missingCounter.add(level, -1);
                                        }
                                    }
                                }
                            }
                        }
                        for (Entry<MissingStatus, String> entry : toRemove.entries()) {
                            missingPaths.remove(entry.getKey(), entry.getValue());
                        }
                    }

                    Set<String> sublocales = languageToRegion.get(language);
                    if (sublocales == null) {
                        //System.err.println("No Sublocales: " + language);
                        sublocales = Collections.EMPTY_SET;
                    }

                    //                List s = Lists.newArrayList(file.fullIterable());

                    String seedString = isSeed ? "seed" : "common";
                    tablePrinter.addRow()
                    .addCell(seedString)
                    .addCell(language)
                    .addCell(ENGLISH.getName(language))
                    .addCell(file.getName(language))
                    .addCell(script)
                    .addCell(cldrLocaleLevelGoal == Level.UNDETERMINED ? "" : cldrLocaleLevelGoal.toString())
                    .addCell(getIcuValue(language))
                    .addCell(sublocales.size());

                    String s = TSV_LOCALE_COVERAGE_HEADER; // make sure sync'ed (and below)
                    if (cldrLevelGoalBasicToModern) {
                        tsv_summary
                        .append(seedString)
                        .append('\t').append(language)
                        .append('\t').append(ENGLISH.getName(language))
                        .append('\t').append(file.getName(language))
                        .append('\t').append(script)
                        .append('\t').append(cldrLocaleLevelGoal.toString())
                        .append('\t').append(sublocales.size()+"");

                    }

                    //                String header = language
                    //                    + "\t" + isCommonLocale
                    //                    + "\t" + ENGLISH.getName(language)
                    //                    + "\t" + file.getName(language)
                    //                    + "\t" + script
                    //                    + "\t" + sublocales.size()
                    //                    //+ "\t" + currentLevel
                    //                    ;

                    int sumFound = 0;
                    int sumMissing = 0;
                    int sumUnconfirmed = 0;

                    // get the totals

                    EnumMap<Level, Integer> totals = new EnumMap<>(Level.class);
                    EnumMap<Level, Integer> confirmed = new EnumMap<>(Level.class);
                    //                EnumMap<Level, Integer> unconfirmedByLevel = new EnumMap<>(Level.class);
                    Set<String> coreMissing = new LinkedHashSet<>();

                    if (locale.equals("af")) {
                        int debug = 0;
                    }

                    Counter<String> starredCounter = new Counter<>();

                    { // CORE
                        long missingExemplarCount = missingCounter.get(Level.CORE);
                        if (missingExemplarCount > 0) {
                            for (Entry<MissingStatus, String> statusAndPath : missingPaths.entrySet()) {
                                String path = statusAndPath.getValue();
                                if (path.startsWith("//ldml/characters/exemplarCharacters")) {
                                    PathHeader ph = pathHeaderFactory.fromPath(path);
                                    String problem = ph.getCode().replaceAll("Others: ","").replaceAll("Main Letters", "main-letters");
                                    coreMissing.add(problem);
                                    // String line = spreadsheetLine(locale, script, language, cldrLevelGoal, foundLevel, missingStatus.toString(), path, file.getStringValue(path));
                                    if (cldrLevelGoalBasicToModern) {
                                        String line = spreadsheetLine(locale, language, script, "«No " + problem + "»", cldrLocaleLevelGoal, Level.CORE, "ABSENT", path, file, vxmlCldrFile2, pathToLocale);
                                        tsv_missing.println(line);
                                    } else {
                                        gatherStarred(path, starredCounter);
                                    }
                                }
                            }
                        }
                        Multimap<CoreItems, String> detailedErrors = LinkedHashMultimap.create();
                        Set<CoreItems> coverage = new TreeSet<>(
                            CoreCoverageInfo.getCoreCoverageInfo(file, detailedErrors));
                        Set<CoreItems> missing = EnumSet.allOf(CoreItems.class);
                        missing.removeAll(coverage);
                        for (Entry<CoreItems, String> entry : detailedErrors.entries()) {
                            CoreItems coreItem = entry.getKey();
                            String path = entry.getValue();
                            coreMissing.add(coreItem.toString());
                            //String line = spreadsheetLine(language, script, "n/a", detailedErrors.get(entry).toString(), level, "ABSENT", "n/a", "n/a", "n/a");
                            if (cldrLevelGoalBasicToModern) {
                                String line = spreadsheetLine(locale, language, script, "«No " + coreItem + "»", cldrLocaleLevelGoal, coreItem.desiredLevel, "ABSENT", path, null, vxmlCldrFile2, pathToLocale);
                                tsv_missing.println(line);
                            } else {
                                gatherStarred(path, starredCounter);
                            }
                        }
                        missing.removeAll(CoreItems.ONLY_RECOMMENDED);
                        foundCounter.add(Level.CORE, coverage.size());
                        missingCounter.add(Level.CORE, missing.size());

                        //                    sumFound += coverage.size();
                        //                    sumMissing += missing.size();

                        //                    confirmed.put(Level.CORE, (int) coverage.size());
                        ////                    unconfirmedByLevel.put(level, (int)(foundCount + unconfirmedCount));
                        //                    totals.put(Level.CORE, (int)(coverage.size() + missing.size()));

                    }

                    if (cldrLevelGoalBasicToModern) {
                        Level goalLevel = cldrLocaleLevelGoal;
                        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                            String path = entry.getValue();
                            String status = entry.getKey().toString();
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                String line = spreadsheetLine(locale, language, script, file.getStringValue(path), goalLevel, foundLevel, status, path, file, vxmlCldrFile2, pathToLocale);
                                tsv_missing.println(line);
                            }
                        }
                        for (String path : unconfirmed) {
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                String line = spreadsheetLine(locale, language, script, file.getStringValue(path), goalLevel, foundLevel, "n/a", path, file, vxmlCldrFile2, pathToLocale);
                                tsv_missing.println(line);
                            }
                        }
                    } else {
                        Level goalLevel = Level.BASIC;
                        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                            String path = entry.getValue();
                            String status = entry.getKey().toString();
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                gatherStarred(path, starredCounter);
                            }
                        }
                        for (String path : unconfirmed) {
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                gatherStarred(path, starredCounter);
                            }
                        }
                    }

                    tsv_missing_basic.println(TSV_MISSING_BASIC_HEADER);
                    for (R2<Long, String> starred : starredCounter.getEntrySetSortedByCount(false, null)) {
                        // PathHeader ph = pathHeaderFactory.fromPath(starred.get1());
                        tsv_missing_basic.println(locale + "\t" + starred.get0() + "\t" + starred.get1().replace("\"*\"", "'*'"));
                    }

                    for (Level level : levelsToShow) {
                        long foundCount = foundCounter.get(level);
                        long unconfirmedCount = unconfirmedCounter.get(level);
                        long missingCount = missingCounter.get(level);

                        sumFound += foundCount;
                        sumUnconfirmed += unconfirmedCount;
                        sumMissing += missingCount;

                        confirmed.put(level, sumFound);
                        //                    unconfirmedByLevel.put(level, (int)(foundCount + unconfirmedCount));
                        totals.put(level, sumFound + sumUnconfirmed + sumMissing);
                    }

                    double modernTotal = totals.get(Level.MODERN);

                    tablePrinter
                    .addCell(sumFound)
                    .addCell(sumUnconfirmed)
                    .addCell(sumMissing)
                    ;

                    if (cldrLevelGoalBasicToModern) {
                        tsv_summary
                        .append('\t').append(sumFound+"")
                        .append('\t').append(sumUnconfirmed+"")
                        .append('\t').append(sumMissing+"")
                        ;
                    }

                    //                header += "\t" + sumFound;
                    //                header += "\t" + (sumFound + sumUnconfirmed);

                    // print the totals

                    for (Level level : reversedLevels) {
                        if (useOrgLevel && cldrLocaleLevelGoal != level) {
                            continue;
                        }
                        int confirmedCoverage = confirmed.get(level);
                        //                    int unconfirmedCoverage = unconfirmedByLevel.get(level);
                        double total = totals.get(level);

                        tablePrinter
                        .addCell(confirmedCoverage / total)
                        //                    .addCell(unconfirmedCoverage / total)
                        ;

                        if (cldrLevelGoalBasicToModern) {
                            tsv_summary
                            .append('\t').append(String.valueOf(confirmedCoverage))
                            .append('\t').append(String.valueOf((int)total - confirmedCoverage))
                            ;
                        }

                        //                    if (RAW_DATA) {
                        //                        header += "\t" + confirmedCoverage / total
                        //                            + "\t" + unconfirmedCoverage / total;
                        //                    } else {
                        //                        Double factor = targetLevel.get(level) / (total / modernTotal);
                        //                        header += "\t" + factor * confirmedCoverage / modernTotal
                        ////                            + "\t" + factor * unconfirmedCoverage / modernTotal
                        //                            ;
                        //                    }
                    }
                    String coreMissingString =
                        Joiner.on(", ").join(coreMissing);

                    tablePrinter
                    .addCell(coreMissingString)
                    .finishRow();

                    if (cldrLevelGoalBasicToModern) {
                        tsv_summary
                        .append('\t')
                        .append(coreMissingString)
                        .append('\n');
                    }

                    // Write missing paths (for >99% and specials

                    //                if (false) { // checkModernLocales.contains(locale)
                    //                    CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(locale);
                    //
                    //                    for (String path : unconfirmed) {
                    //                        Level level = coverageLevel2.getLevel(path);
                    //                        if (level.compareTo(cldrLocaleLevelGoal) > 0) {
                    //                            continue;
                    //                        }
                    //                        String line = spreadsheetLine(locale, language, script, file.getStringValue(path), cldrLocaleLevelGoal, level, "UNCONFIRMED", path, pathToLocale);
                    //                        if (SUPPRESS_PATHS_CAN_BE_EMPTY.get(path) != null) {
                    //                            //System.out.println("\nSKIP: " + line);
                    //                        } else {
                    //                            tsv_missing.println(line);
                    //                        }
                    //                    }
                    //                    for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                    //                        String path = entry.getValue();
                    //                        Level level = coverageLevel2.getLevel(path);
                    //                        if (level.compareTo(cldrLocaleLevelGoal) > 0) {
                    //                            continue;
                    //                        }
                    //                        MissingStatus missingStatus = entry.getKey();
                    //                        String line = spreadsheetLine(locale, language, script, "???", cldrLocaleLevelGoal, level, missingStatus.toString(), path, pathToLocale);
                    //                        if (SUPPRESS_PATHS_CAN_BE_EMPTY.get(path) != null) {
                    //                            //System.out.println("\nSKIP: " + line);
                    //                        } else {
                    //                            tsv_missing.println(line);
                    //                        }
                    //                    }
                    //                }

                    localeCount++;
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            pw.println(tablePrinter.toTable());

            Multimap<Level, String> levelToLocales = TreeMultimap.create();

            for ( Entry<String, Collection<String>> entry : pathToLocale.asMap().entrySet()) {
                String path = entry.getKey();
                Collection<String> localeSet = entry.getValue();
                levelToLocales.clear();
                for (String locale : localeSet) {
                    Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                    levelToLocales.put(foundLevel, locale);
                }
                String phString = "n/a\tn/a\tn/a\tn/a";
                try {
                    PathHeader ph = pathHeaderFactory.fromPath(path);
                    phString = ph.toString();
                } catch (Exception e) {
                }
                for (Entry<Level, Collection<String>> entry2 : levelToLocales.asMap().entrySet()) {
                    Level level = entry2.getKey();
                    localeSet = entry2.getValue();
                    String s = TSV_MISSING_SUMMARY_HEADER; // check for changes
                    tsv_missing_summary.println(
                        level
                        + "\t" + localeSet.size()
                        + "\t" + Joiner.on(" ").join(localeSet)
                        + "\t" + phString
                        );
                }
            }
            //        out2.close();

            long end = System.currentTimeMillis();
            System.out.println((end - start) + " millis = "
                + ((end - start) / localeCount) + " millis/locale");
            ShowPlurals.appendBlanksForScrolling(pw);
        }
    }

//    public static void showEnglish() {
//        Map<PathHeader,String> sorted = new TreeMap<>();
//        CoverageInfo coverageInfo=CLDRConfig.getInstance().getCoverageInfo();
//        for (String path : ENGLISH) {
////            Level currentLevel = SUPPLEMENTAL_DATA_INFO.getCoverageLevel(path, "en");
//            Level currentLevel=coverageInfo.getCoverageLevel(path, "en");
//            if (currentLevel.compareTo(Level.MINIMAL) <= 0) {
//                PathHeader ph = pathHeaderFactory.fromPath(path);
//                sorted.put(ph, currentLevel + "\t" + ENGLISH.getStringValue(path));
//            }
//        }
//        for (Entry<PathHeader, String> entry : sorted.entrySet()) {
//            System.out.println(entry.getKey() + "\t" + entry.getValue());
//        }
//    }

    static class IterableFilter implements Iterable<String> {
        private Iterable<String> source;

        IterableFilter(Iterable<String> source) {
            this.source = source;
        }

        /**
         * When some paths are defined after submission, we need to change them to COMPREHENSIVE in computing the vetting status.
         */

        static final Set<String> SUPPRESS_PATHS_AFTER_SUBMISSION = ImmutableSet.of(
            "//ldml/localeDisplayNames/languages/language[@type=\"ccp\"]",
            "//ldml/localeDisplayNames/territories/territory[@type=\"XA\"]",
            "//ldml/localeDisplayNames/territories/territory[@type=\"XB\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Gy\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Gy\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Gy\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Gy\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyM\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMEd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMM\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMd\"]/greatestDifference[@id=\"y\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"d\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"G\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"M\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"GyMMMEd\"]/greatestDifference[@id=\"y\"]"
            );
        @Override
        public Iterator<String> iterator() {
            return new IteratorFilter(source.iterator());
        }

        static class IteratorFilter implements Iterator<String> {
            Iterator<String> source;
            String peek;

            public IteratorFilter(Iterator<String> source) {
                this.source = source;
                fillPeek();
            }
            @Override
            public boolean hasNext() {
                return peek != null;
            }
            @Override
            public String next() {
                String result = peek;
                fillPeek();
                return result;
            }

            private void fillPeek() {
                peek = null;
                while (source.hasNext()) {
                    peek = source.next();
                    // if it is ok to assess, then break
                    if (!SUPPRESS_PATHS_AFTER_SUBMISSION.contains(peek)
                        && SUPPRESS_PATHS_CAN_BE_EMPTY.get(peek) != Boolean.TRUE) {
                        break;
                    }
                    peek = null;
                }
            }
        }

    }
    static final CoverageInfo coverageInfo = new CoverageInfo(SUPPLEMENTAL_DATA_INFO);

// userInfo.getVoterInfo().getLevel().compareTo(VoteResolver.Level.tc)
    static final VoterInfo dummyVoterInfo = new VoterInfo(Organization.cldr, org.unicode.cldr.util.VoteResolver.Level.vetter, "somename");

    static final UserInfo dummyUserInfo = new UserInfo() {
        @Override
        public VoterInfo getVoterInfo() {
            return dummyVoterInfo;
        }
    };
    static final PathValueInfo dummyPathValueInfo = new PathValueInfo() {
        // pathValueInfo.getCoverageLevel().compareTo(Level.COMPREHENSIVE)
        @Override
        public Collection<? extends CandidateInfo> getValues() {
            throw new UnsupportedOperationException();
        }
        @Override
        public CandidateInfo getCurrentItem() {
            throw new UnsupportedOperationException();
        }
        @Override
        public String getBaselineValue() {
            throw new UnsupportedOperationException();
        }
        @Override
        public Level getCoverageLevel() {
            return Level.MODERN;
        }
        @Override
        public boolean hadVotesSometimeThisRelease() {
            throw new UnsupportedOperationException();
        }
        @Override
        public CLDRLocale getLocale() {
            throw new UnsupportedOperationException();
        }
        @Override
        public String getXpath() {
            throw new UnsupportedOperationException();
        }
    };


//    static org.unicode.cldr.util.Factory VXML_FACTORY = SimpleFactory.make(new File[] {
//        new File(VXML_CONSTANT + "main"),
//        new File(VXML_CONSTANT + "annotations") }, ".*");
        static CLDRFile vxmlCldrFile = null;
//    static String vxmlLocale = "";

//    private static CLDRFile getVxmlCldrFile(String locale) {
//        if (!vxmlLocale.equals(locale)) {
//            try {
//                vxmlCldrFile = VXML_FACTORY.make(locale, false);
//            } catch (Exception e) {
//                vxmlCldrFile = null;
//            }
//            vxmlLocale = locale;
//        }
//        return vxmlCldrFile;
//    }

    public static void gatherStarred(String path, Counter<String> starredCounter) {
        starredCounter.add(new PathStarrer().setSubstitutionPattern("*").set(path), 1);
    }

    public static String spreadsheetLine(String locale, String language, String script, String nativeValue, Level cldrLocaleLevelGoal,
        Level itemLevel, String status, String path, CLDRFile resolvedFile, CLDRFile vxmlCldrFile,
        Multimap<String, String> pathToLocale) {
        if (pathToLocale != null) {
            pathToLocale.put(path, locale);
        }
        String stLink = "n/a";
        String englishValue = "n/a";
        StatusAction action = null;
        SurveyToolStatus surveyToolStatus = null;
        String icuValue = getIcuValue(locale);

        String bailey = resolvedFile == null ? "" : resolvedFile.getStringValue(path);
        String vxmlValue = "";
        String vxmlDraftStatus = "";
        if (vxmlCldrFile != null) {
            try {
                vxmlValue = vxmlCldrFile.getStringValue(path);
                if (vxmlValue == null) {
                    vxmlValue = "";
                } else {
                    String fullXPath = vxmlCldrFile.getFullXPath(path);
                    XPathParts parts = XPathParts.getFrozenInstance(fullXPath);
                    vxmlDraftStatus = parts.getAttributeValue(-1, "draft");
                    if (vxmlDraftStatus == null) {
                        vxmlDraftStatus = "";
                    }
                }
            } catch (Exception e) {
            }
        }

        String phString = "na\tn/a\tn/a\t" + path;
        try {
            PathHeader ph = pathHeaderFactory.fromPath(path);
            phString = ph.toString();
            surveyToolStatus = ph.getSurveyToolStatus();
            stLink = URLS.forXpath(locale, path);
            englishValue = ENGLISH.getStringValue(path);
            action = Phase.SUBMISSION.getShowRowAction(dummyPathValueInfo, InputMethod.DIRECT, surveyToolStatus, dummyUserInfo);
        } catch (Exception e) {
            int debug = 0;
        }

        String config_text = vxmlValue.isEmpty() ? "" :
            "locale=" + locale
            + " ; action=add"
            + " ; new_path=" + StringId.getHexId(path)
            + " ; new_value=" + vxmlValue;



        String s = TSV_MISSING_HEADER; // make sure in sync
        String line =
            language
            + "\t" + ENGLISH.getName(language)
            + "\t" + ENGLISH.getName("script", script)
            //+ "\t" + englishValue
            //+ "\t" + nativeValue
            + "\t" + cldrLocaleLevelGoal
            //+ "\t" + icuValue
            + "\t" + itemLevel
            //+ "\t" + status
            //+ "\t" + (action == null ? "?" : action.toString())
            + "\t" + (surveyToolStatus == null ? "?" : surveyToolStatus.toString())
            //+ "\t" + stLink
            + "\t" + bailey
            + "\t" + vxmlValue
            + "\t" + vxmlDraftStatus
            + "\t" + phString
            + "\t" + PathHeader.getUrlForLocalePath(locale, path)
            + "\t" + config_text
            ;
        return line;
    }



    private static String getIcuValue(String locale) {
        return ICU_Locales.contains(new ULocale(locale)) ? "ICU" : "";
    }

    static final Set<ULocale> ICU_Locales = ImmutableSet.copyOf(ULocale.getAvailableLocales());
    private static CLDRURLS URLS = CONFIG.urls();

}
