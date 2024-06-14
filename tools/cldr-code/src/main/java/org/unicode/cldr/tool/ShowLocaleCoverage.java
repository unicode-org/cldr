package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoreCoverageInfo;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

public class ShowLocaleCoverage {

    private static final String TSV_BASE =
            CLDRURLS.CLDR_STAGING_REPO_MAIN
                    + "docs/charts/"
                    + ToolConstants.CHART_VI.getVersionString(1, 2)
                    + "/tsv/";
    public static final Splitter LF_SPLITTER = Splitter.on('\n');

    // thresholds for measuring Level attainment
    private static final double BASIC_THRESHOLD = 1;
    private static final double MODERATE_THRESHOLD = 0.995;
    private static final double MODERN_THRESHOLD = 0.995;

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final String TSV_MISSING_SUMMARY_HEADER =
            "#Path Level"
                    + "\t#Locales"
                    + "\tLocales"
                    + "\tSection"
                    + "\tPage"
                    + "\tHeader"
                    + "\tCode";

    private static final String TSV_MISSING_HEADER =
            "#LCode"
                    + "\tEnglish Name"
                    + "\tScript"
                    + "\tLocale Level"
                    + "\tPath Level"
                    + "\tSTStatus"
                    + "\tBailey"
                    + "\tSection"
                    + "\tPage"
                    + "\tHeader"
                    + "\tCode"
                    + "\tST Link";

    private static final String PROPERTIES_HEADER =
            "# coverageLevels.txt\n"
                    + "# Copyright © 2023 Unicode, Inc.\n"
                    + "# CLDR data files are interpreted according to the\n"
                    + "# LDML specification: http://unicode.org/reports/tr35/\n"
                    + "# For terms of use, see http://www.unicode.org/copyright.html\n"
                    + "#\n"
                    + "# For format and usage information, see:\n"
                    + "# https://cldr.unicode.org/index/cldr-spec/coverage-levels.\n"
                    + "\n";
    private static final String TSV_MISSING_BASIC_HEADER =
            "#Locale\tProv.\tUnconf.\tMissing\tPath*\tAttributes";
    private static final String TSV_MISSING_COUNTS_HEADER =
            "#Locale\tTargetLevel\t№ Found\t№ Unconfirmed\t№ Missing";

    private static final boolean DEBUG = true;
    private static final char DEBUG_FILTER =
            0; // use letter to only load locales starting with that letter

    private static final String LATEST = ToolConstants.CHART_VERSION;
    private static CLDRConfig testInfo = ToolConfig.getToolInstance();
    private static final StandardCodes SC = StandardCodes.make();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            testInfo.getSupplementalDataInfo();
    private static final StandardCodes STANDARD_CODES = SC;

    private static org.unicode.cldr.util.Factory factory =
            testInfo.getCommonAndSeedAndMainAndAnnotationsFactory();
    private static final CLDRFile ENGLISH = factory.make("en", true);

    static final Options myOptions = new Options();

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument."),
        //        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft
        // status."),
        chart(null, null, "chart only"),
        organization(".+", null, "Only locales for organization"),
        version(".+", LATEST, "To get different versions"),
        rawData(null, null, "Output the raw data from all coverage levels"),
        targetDir(".*", CLDRPaths.GEN_DIRECTORY + "/statistics/", "target output file."),
        directories(
                "(.*:)?[a-z]+(,[a-z]+)*",
                "common",
                "Space-delimited list of main source directories: common,seed,exemplar.\n"
                        + "Optional, <baseDir>:common,seed"),
        ;

        // targetDirectory(".+", CldrUtility.CHART_DIRECTORY + "keyboards/", "The target
        // directory."),
        // layouts(null, null, "Only create html files for keyboard layouts"),
        // repertoire(null, null, "Only create html files for repertoire"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    private static final RegexLookup<Boolean> SUPPRESS_PATHS_CAN_BE_EMPTY =
            new RegexLookup<Boolean>()
                    .add("\\[@alt=\"accounting\"]", true)
                    .add("\\[@alt=\"variant\"]", true)
                    .add("^//ldml/localeDisplayNames/territories/territory.*@alt=\"short", true)
                    .add("^//ldml/localeDisplayNames/languages/language.*_", true)
                    .add("^//ldml/numbers/currencies/currency.*/symbol", true)
                    .add("^//ldml/characters/exemplarCharacters", true);

    private static DraftStatus minimumDraftStatus = DraftStatus.unconfirmed;
    private static final Factory pathHeaderFactory = PathHeader.getFactory(ENGLISH);

    private static Set<String> COMMON_LOCALES;

    public static class StatusData {
        int missing;
        int provisional;
        int unconfirmed;
        Set<List<String>> values =
                new TreeSet<>(Comparators.lexicographical(Comparator.<String>naturalOrder()));
    }

    public static class StatusCounter {
        private static final Set<String> ATTRS_TO_REMOVE = Set.of("standard");
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        Map<String, StatusData> starredPathToData = new TreeMap<>();
        int missingTotal;
        int provisionalTotal;
        int unconfirmedTotal;

        public void gatherStarred(String path, DraftStatus draftStatus) {
            String starredPath = pathStarrer.set(path);
            StatusData statusData = starredPathToData.get(starredPath);
            if (statusData == null) {
                starredPathToData.put(starredPath, statusData = new StatusData());
            }
            if (draftStatus == null) {
                ++statusData.missing;
                ++missingTotal;
            } else {
                switch (draftStatus) {
                    case unconfirmed:
                        ++statusData.unconfirmed;
                        ++unconfirmedTotal;
                        break;
                    case provisional:
                        ++statusData.provisional;
                        ++provisionalTotal;
                        break;
                    default:
                        break;
                }
            }
            final List<String> attributes =
                    CldrUtility.removeAll(
                            new ArrayList<>(pathStarrer.getAttributes()), ATTRS_TO_REMOVE);
            if (!attributes.isEmpty()) {
                statusData.values.add(attributes);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.filter, args, true);

        Matcher matcher = PatternCache.get(MyOptions.filter.option.getValue()).matcher("");

        if (MyOptions.chart.option.doesOccur()) {
            showCoverage(null, matcher);
            return;
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
            factory =
                    org.unicode.cldr.util.Factory.make(
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
                COMMON_LOCALES =
                        SimpleFactory.make(base + "/" + "common" + "/main", ".*")
                                .getAvailableLanguages();
            }
        }
        fixCommonLocales();

        showCoverage(null, matcher, locales, useOrgLevel);
    }

    private static void fixCommonLocales() {
        if (COMMON_LOCALES == null) {
            COMMON_LOCALES = factory.getAvailableLanguages();
        }
    }

    public static class FoundAndTotal {
        final int found;
        final int total;

        @SafeVarargs
        public FoundAndTotal(Counter<Level>... counters) {
            final int[] count = {0, 0, 0};
            for (Level level : Level.values()) {
                if (level == Level.COMPREHENSIVE) {
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

    static void showCoverage(Anchors anchors, Matcher matcher) throws IOException {
        showCoverage(anchors, matcher, null, false);
    }

    private static void showCoverage(
            Anchors anchors, Matcher matcher, Set<String> locales, boolean useOrgLevel)
            throws IOException {
        final String title = "Locale Coverage";
        try (PrintWriter pw = new PrintWriter(new FormattedFileWriter(null, title, null, anchors));
                PrintWriter tsv_summary =
                        FileUtilities.openUTF8Writer(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-coverage.tsv");
                PrintWriter tsv_missing =
                        FileUtilities.openUTF8Writer(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing.tsv");
                PrintWriter tsv_missing_summary =
                        FileUtilities.openUTF8Writer(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing-summary.tsv");
                PrintWriter tsv_missing_basic =
                        FileUtilities.openUTF8Writer(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing-basic.tsv");
                PrintWriter tsv_missing_counts =
                        FileUtilities.openUTF8Writer(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-missing-counts.tsv");
                TempPrintWriter propertiesCoverage =
                        TempPrintWriter.openUTF8Writer(
                                CLDRPaths.COMMON_DIRECTORY + "properties/",
                                "coverageLevels.txt"); ) {
            tsv_missing_summary.println(TSV_MISSING_SUMMARY_HEADER);
            tsv_missing.println(TSV_MISSING_HEADER);
            tsv_missing_basic.println(TSV_MISSING_BASIC_HEADER);
            tsv_missing_counts.println(TSV_MISSING_COUNTS_HEADER);

            final int propertiesCoverageTabCount = 2;
            propertiesCoverage.printlnWithTabs(propertiesCoverageTabCount, PROPERTIES_HEADER);

            Set<String> checkModernLocales =
                    STANDARD_CODES.getLocaleCoverageLocales(
                            Organization.cldr, EnumSet.of(Level.MODERN));
            Set<String> availableLanguages = new TreeSet<>(factory.getAvailableLanguages());
            availableLanguages.addAll(checkModernLocales);

            Multimap<String, String> languageToRegion = TreeMultimap.create();
            LanguageTagParser ltp = new LanguageTagParser();
            LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer(true);
            for (String locale : factory.getAvailable()) {
                String country = ltp.set(locale).getRegion();
                if (!country.isEmpty()) {
                    languageToRegion.put(ltc.transform(ltp.getLanguageScript()), country);
                }
            }
            languageToRegion = ImmutableMultimap.copyOf(languageToRegion);

            fixCommonLocales();

            System.out.println(Joiner.on("\n").join(languageToRegion.asMap().entrySet()));

            System.out.println("# Checking: " + availableLanguages);

            NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.ENGLISH);
            percentFormat.setMaximumFractionDigits(1);

            pw.println(
                    "<p style='text-align: left'>This chart shows the coverage levels in this release. "
                            + "Totals are listed after the main chart.</p>\n"
                            + "<blockquote><ul>\n"
                            + "<li><a href='#main_table'>Main Table</a></li>\n"
                            + "<li><a href='#level_counts'>Level Counts</a></li>\n"
                            + "</ul></blockquote>\n"
                            + "<h3>Column Key</h3>\n"
                            + "<table class='subtle' style='margin-left:3em; margin-right:3em'>\n"
                            + "<tr><th>Default Region</th><td>The default region for locale code, based on likely subtags</td></tr>\n"
                            + "<tr><th>№ Locales</th><td>Note that the coverage of regional locales inherits from their parents.</td></tr>\n"
                            + "<tr><th>Target Level</th><td>The default target Coverage Level in CLDR. "
                            + "Particular organizations may have different target levels. "
                            + "Languages with high levels of coverage are marked with ‡, even though they are not tracked by the technical committee.</td></tr>\n"
                            + "<tr><th>≟</th><td>Indicates whether the CLDR Target is less than, equal to, or greater than the Computed Level.</td></tr>\n"
                            + "<tr><th>Computed Level</th><td>Computed from the percentage values, "
                            + "taking the first level that meets a threshold (currently 🄼 "
                            + percentFormat.format(MODERN_THRESHOLD)
                            + ", ⓜ "
                            + percentFormat.format(MODERATE_THRESHOLD)
                            + ", ⓑ "
                            + percentFormat.format(BASIC_THRESHOLD)
                            + ").</td></tr>\n"
                            + "<tr><th>ICU</th><td>Indicates whether included in the current version of ICU</td></tr>\n"
                            + "<tr><th>Confirmed</th><td>Confirmed items as a percentage of all supplied items. "
                            + "If low, the coverage can be improved by getting multiple organizations to confirm.</td></tr>\n"
                            + "<tr><th>🄼%, ⓜ%, ⓑ%, ⓒ%</th><td>Coverage at Levels: 🄼 = Modern, ⓜ = Moderate, ⓑ = Basic, ⓒ = Core. "
                            + "The percentage of items at that level and below is computed from <i>confirmed_items/total_items</i>. "
                            + "A high-level summary of the meaning of the coverage values is at "
                            + "<a target='_blank' href='http://www.unicode.org/reports/tr35/tr35-info.html#Coverage_Levels'>Coverage Levels</a>. "
                            + "The Core values are described on <a target='_blank' href='https://cldr.unicode.org/index/cldr-spec/core-data-for-new-locales'>Core Data</a>. "
                            + "</td></tr>\n"
                            + "<tr><th>Missing Features</th><td>These are not single items, but rather specific features, such as plural rules or unit grammar info. "
                            + "They are listed if missing at the computed level. For more information, see <a href='https://cldr.unicode.org/index/locale-coverage'>Missing Features</a><br>"
                            + "Example: <i>ⓜ collation</i> means this feature should be supported at a Moderate level.<br>"
                            + "<ul><li>"
                            + "<i>Except for Core, these are not accounted for in the percent values.</i>"
                            + "</li><li>"
                            + "The information needs to be provided in tickets, not through the Survey Tool."
                            + "</li></ul>"
                            + "</td></tr>\n"
                            + "<tr><th>"
                            + linkTsv("", "TSVFiles")
                            + ":</th><td>\n"
                            + "<ul><li>"
                            + linkTsv("locale-coverage.tsv")
                            + " — A version of this file, suitable for loading into a spreadsheet.</li>\n"
                            + "<li>"
                            + linkTsv("locale-missing.tsv")
                            + " — Missing items for the CLDR target locales.</li>\n"
                            + "<li>"
                            + linkTsv("locale-missing-summary.tsv")
                            + " — Summary of missing items for the CLDR target locales, by Section/Page/Header.</li>\n"
                            + "<li>"
                            + linkTsv("locale-missing-basic.tsv")
                            + " — Missing items that keep locales from reaching the Basic level.</li>\n"
                            + "<li>"
                            + linkTsv("locale-missing-counts.tsv")
                            + " — Counts of items per locale that are found, unconfirmed, or missing, at the target level. "
                            + "(Or at *basic, if there is no target level.)</li>\n"
                            + "</td></tr>\n"
                            + "</table>\n");

            Relation<MissingStatus, String> missingPaths =
                    Relation.of(
                            new EnumMap<MissingStatus, Set<String>>(MissingStatus.class),
                            TreeSet.class,
                            CLDRFile.getComparator(DtdType.ldml));
            Set<String> unconfirmed = new TreeSet<>(CLDRFile.getComparator(DtdType.ldml));

            Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();

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

            int localeCount = 0;

            final TablePrinter tablePrinter =
                    new TablePrinter()
                            .addColumn(
                                    "Language",
                                    "class='source'",
                                    CldrUtility.getDoubleLinkMsg(),
                                    "class='source'",
                                    true)
                            .setBreakSpans(true)
                            .addColumn(
                                    "English Name", "class='source'", null, "class='source'", true)
                            .setBreakSpans(true)
                            .addColumn(
                                    "Native Name", "class='source'", null, "class='source'", true)
                            .setBreakSpans(true)
                            .addColumn("Script", "class='source'", null, "class='source'", true)
                            .setBreakSpans(true)
                            .addColumn(
                                    "Default Region",
                                    "class='source'",
                                    null,
                                    "class='source'",
                                    true)
                            .setBreakSpans(true)
                            .addColumn(
                                    "№ Locales",
                                    "class='source'",
                                    null,
                                    "class='targetRight'",
                                    true)
                            .setBreakSpans(true)
                            .setCellPattern("{0,number}")
                            .addColumn(
                                    "Target Level", "class='source'", null, "class='source'", true)
                            .setBreakSpans(true)
                            .addColumn("≟", "class='target'", null, "class='target'", true)
                            .setBreakSpans(true)
                            .setSortPriority(1)
                            .setSortAscending(false)
                            .addColumn(
                                    "Computed Level",
                                    "class='target'",
                                    null,
                                    "class='target'",
                                    true)
                            .setBreakSpans(true)
                            .setSortPriority(0)
                            .setSortAscending(false)
                            .addColumn("ICU", "class='target'", null, "class='target'", true)
                            .setBreakSpans(true)
                            .addColumn(
                                    "Confirmed",
                                    "class='target'",
                                    null,
                                    "class='targetRight' style='color:gray'",
                                    true)
                            .setBreakSpans(true)
                            .setCellPattern("{0,number,0.0%}");

            NumberFormat tsvPercent = NumberFormat.getPercentInstance(Locale.ENGLISH);
            tsvPercent.setMaximumFractionDigits(2);

            for (Level level : reversedLevels) {
                String titleLevel = level.getAbbreviation() + "%";
                tablePrinter
                        .addColumn(titleLevel, "class='target'", null, "class='targetRight'", true)
                        .setCellPattern("{0,number,0.0%}")
                        .setBreakSpans(true);

                switch (level) {
                    default:
                        tablePrinter.setSortPriority(2).setSortAscending(false);
                        break;
                    case BASIC:
                        tablePrinter.setSortPriority(3).setSortAscending(false);
                        break;
                    case MODERATE:
                        tablePrinter.setSortPriority(4).setSortAscending(false);
                        break;
                    case MODERN:
                        tablePrinter.setSortPriority(5).setSortAscending(false);
                        break;
                }
            }
            tablePrinter
                    .addColumn("Missing Features", "class='target'", null, "class='target'", true)
                    .setBreakSpans(true);

            long start = System.currentTimeMillis();
            LikelySubtags likelySubtags = new LikelySubtags();

            EnumMap<Level, Double> targetLevel = new EnumMap<>(Level.class);
            targetLevel.put(Level.CORE, 2 / 100d);
            targetLevel.put(Level.BASIC, 16 / 100d);
            targetLevel.put(Level.MODERATE, 33 / 100d);
            targetLevel.put(Level.MODERN, 100 / 100d);

            Multimap<String, String> pathToLocale = TreeMultimap.create();

            Counter<Level> computedLevels = new Counter<>();
            Counter<Level> computedSublocaleLevels = new Counter<>();

            for (String locale : availableLanguages) {
                try {
                    if (locale.contains("supplemental") // for old versionsl
                    //                        || locale.startsWith("sr_Latn")
                    ) {
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
                    if (defaultContents.contains(locale)
                            || LocaleNames.ROOT.equals(locale)
                            || LocaleNames.UND.equals(locale)) {
                        continue;
                    }

                    tsv_missing_summary.flush();
                    tsv_missing.flush();
                    tsv_missing_basic.flush();
                    tsv_missing_counts.flush();

                    boolean isSeed = new File(CLDRPaths.SEED_DIRECTORY, locale + ".xml").exists();

                    String region = ltp.set(locale).getRegion();
                    if (!region.isEmpty()) continue; // skip regions

                    final Level cldrLocaleLevelGoal =
                            SC.getLocaleCoverageLevel(Organization.cldr, locale);
                    final String specialFlag = getSpecialFlag(locale);

                    final boolean cldrLevelGoalBasicToModern =
                            Level.CORE_TO_MODERN.contains(cldrLocaleLevelGoal);

                    String max = likelySubtags.maximize(locale);
                    final String script = ltp.set(max).getScript();
                    final String defRegion = ltp.getRegion();

                    final String language = likelySubtags.minimize(locale);

                    missingPaths.clear();
                    unconfirmed.clear();

                    final CLDRFile file = factory.make(locale, true, minimumDraftStatus);

                    if (locale.equals("af")) {
                        int debug = 0;
                    }

                    Iterable<String> pathSource = new IterableFilter(file.fullIterable());

                    VettingViewer.getStatus(
                            pathSource,
                            file,
                            pathHeaderFactory,
                            foundCounter,
                            unconfirmedCounter,
                            missingCounter,
                            missingPaths,
                            unconfirmed);

                    {
                        long found = 0;
                        long unconfirmedc = 0;
                        long missing = 0;
                        Level adjustedGoal =
                                cldrLocaleLevelGoal.compareTo(Level.BASIC) < 0
                                        ? Level.BASIC
                                        : cldrLocaleLevelGoal;
                        for (Level level : Level.values()) {
                            if (level.compareTo(adjustedGoal) <= 0) {
                                found += foundCounter.get(level);
                                unconfirmedc += unconfirmedCounter.get(level);
                                missing += missingCounter.get(level);
                            }
                        }
                        String goalFlag = cldrLocaleLevelGoal == adjustedGoal ? "" : "*";
                        tsv_missing_counts.println(
                                specialFlag
                                        + locale
                                        + "\t"
                                        + goalFlag
                                        + adjustedGoal
                                        + "\t"
                                        + found
                                        + "\t"
                                        + unconfirmedc
                                        + "\t"
                                        + missing);
                    }

                    Collection<String> sublocales = languageToRegion.asMap().get(language);
                    if (sublocales == null) {
                        sublocales = Collections.emptySet();
                    }
                    sublocales = ImmutableSet.copyOf(sublocales);

                    // get the totals

                    EnumMap<Level, Integer> totals = new EnumMap<>(Level.class);
                    EnumMap<Level, Integer> confirmed = new EnumMap<>(Level.class);
                    Set<CoreItems> specialMissingPaths = EnumSet.noneOf(CoreItems.class);

                    StatusCounter starredCounter = new StatusCounter();

                    {
                        Multimap<CoreItems, String> detailedErrors = TreeMultimap.create();
                        Set<CoreItems> coverage =
                                CoreCoverageInfo.getCoreCoverageInfo(file, detailedErrors);
                        for (CoreItems item : coverage) {
                            foundCounter.add(item.desiredLevel, 1);
                        }
                        for (Entry<CoreItems, String> entry : detailedErrors.entries()) {
                            CoreItems coreItem = entry.getKey();
                            String path = entry.getValue();
                            specialMissingPaths.add(coreItem);
                            // if goal (eg modern) >= itemLevel, indicate it is missing
                            if (coreItem.desiredLevel == Level.BASIC) {
                                starredCounter.gatherStarred(path, null);
                            }
                            missingCounter.add(coreItem.desiredLevel, 1);
                        }
                    }

                    if (cldrLevelGoalBasicToModern) {
                        Level goalLevel = cldrLocaleLevelGoal;
                        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                            String path = entry.getValue();
                            String status = entry.getKey().toString();
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                String line =
                                        spreadsheetLine(
                                                locale,
                                                language,
                                                script,
                                                specialFlag,
                                                file.getStringValue(path),
                                                goalLevel,
                                                foundLevel,
                                                status,
                                                path,
                                                file,
                                                pathToLocale);
                                String lineToPrint1 = line;
                                tsv_missing.println(lineToPrint1);
                            }
                        }
                        for (String path : unconfirmed) {
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                String line =
                                        spreadsheetLine(
                                                locale,
                                                language,
                                                script,
                                                specialFlag,
                                                file.getStringValue(path),
                                                goalLevel,
                                                foundLevel,
                                                "n/a",
                                                path,
                                                file,
                                                pathToLocale);
                                tsv_missing.println(line);
                            }
                        }
                    } else {
                        Level goalLevel = Level.BASIC;
                        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                            String path = entry.getValue();
                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                starredCounter.gatherStarred(path, null);
                            }
                        }
                        for (String path : unconfirmed) {
                            String fullPath = file.getFullXPath(path);
                            DraftStatus draftStatus =
                                    fullPath.contains("unconfirmed")
                                            ? DraftStatus.unconfirmed
                                            : DraftStatus.provisional;

                            Level foundLevel = coverageInfo.getCoverageLevel(path, locale);
                            if (goalLevel.compareTo(foundLevel) >= 0) {
                                starredCounter.gatherStarred(path, draftStatus);
                            }
                        }
                    }

                    if (!starredCounter.starredPathToData.isEmpty()) {
                        for (Entry<String, StatusData> starred :
                                starredCounter.starredPathToData.entrySet()) {
                            String starredPath = starred.getKey();
                            StatusData statusData = starred.getValue();
                            String valueString =
                                    statusData.values.stream()
                                            .map(x -> Joiner.on(", ").join(x))
                                            .collect(Collectors.joining("; "));

                            tsv_missing_basic.println(
                                    specialFlag
                                            + locale //
                                            + "\t"
                                            + statusData.missing //
                                            + "\t"
                                            + statusData.provisional //
                                            + "\t"
                                            + statusData.unconfirmed //
                                            + "\t"
                                            + starredPath.replace("\"*\"", "'*'")
                                            + "\t"
                                            + valueString
                                    //
                                    );
                        }
                        tsv_missing_basic.println(
                                specialFlag
                                        + locale //
                                        + "\t"
                                        + starredCounter.missingTotal //
                                        + "\t"
                                        + starredCounter.provisionalTotal //
                                        + "\t"
                                        + starredCounter.unconfirmedTotal //
                                        + "\tTotals\t");
                        tsv_missing_basic.println("\t\t\t\t\t"); // for a proper table in github
                    }

                    int sumFound = 0;
                    int sumMissing = 0;
                    int sumUnconfirmed = 0;

                    for (Level level : levelsToShow) {
                        long foundCount = foundCounter.get(level);
                        long unconfirmedCount = unconfirmedCounter.get(level);
                        long missingCount = missingCounter.get(level);

                        sumFound += foundCount;
                        sumUnconfirmed += unconfirmedCount;
                        sumMissing += missingCount;

                        confirmed.put(level, sumFound);
                        totals.put(level, sumFound + sumUnconfirmed + sumMissing);
                    }

                    // double modernTotal = totals.get(Level.MODERN);

                    // first get the accumulated values
                    EnumMap<Level, Integer> accumTotals = new EnumMap<>(Level.class);
                    EnumMap<Level, Integer> accumConfirmed = new EnumMap<>(Level.class);
                    int currTotals = 0;
                    int currConfirmed = 0;
                    for (Level level : levelsToShow) {
                        currTotals += totals.get(level);
                        currConfirmed += confirmed.get(level);
                        accumConfirmed.put(level, currConfirmed);
                        accumTotals.put(level, currTotals);
                    }

                    // print the totals

                    Level computed = Level.UNDETERMINED;
                    Map<Level, Double> levelToProportion = new EnumMap<>(Level.class);

                    for (Level level : reversedLevels) {
                        int confirmedCoverage = accumConfirmed.get(level);
                        double total = accumTotals.get(level);

                        final double proportion = confirmedCoverage / total;
                        levelToProportion.put(level, proportion);

                        if (computed == Level.UNDETERMINED) {
                            switch (level) {
                                case MODERN:
                                    if (proportion >= MODERN_THRESHOLD) {
                                        computed = level;
                                    }
                                    break;
                                case MODERATE:
                                    if (proportion >= MODERATE_THRESHOLD) {
                                        computed = level;
                                    }
                                    break;
                                case BASIC:
                                    if (proportion >= BASIC_THRESHOLD) {
                                        computed = level;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    Set<CoreItems> shownMissingPaths = EnumSet.noneOf(CoreItems.class);
                    Level computedWithCore =
                            computed == Level.UNDETERMINED ? Level.BASIC : computed;
                    for (CoreItems item : specialMissingPaths) {
                        if (item.desiredLevel.compareTo(computedWithCore) <= 0) {
                            shownMissingPaths.add(item);
                        } else {
                            int debug = 0;
                        }
                    }
                    computedLevels.add(computed, 1);
                    computedSublocaleLevels.add(computed, sublocales.size());

                    final String coreMissingString = Joiner.on(", ").join(shownMissingPaths);
                    final String visibleLevelComputed =
                            computed == Level.UNDETERMINED ? "" : computed.toString();
                    final String visibleLevelGoal =
                            cldrLocaleLevelGoal == Level.UNDETERMINED
                                    ? ""
                                    : specialFlag + cldrLocaleLevelGoal.toString();
                    final String goalComparedToComputed =
                            computed == cldrLocaleLevelGoal
                                    ? " ≡"
                                    : cldrLocaleLevelGoal.compareTo(computed) < 0 ? " <" : " >";

                    tablePrinter
                            .addRow()
                            .addCell(language)
                            .addCell(ENGLISH.getName(language, true, CLDRFile.SHORT_ALTS))
                            .addCell(file.getName(language))
                            .addCell(script)
                            .addCell(defRegion)
                            .addCell(sublocales.size())
                            .addCell(visibleLevelGoal)
                            .addCell(goalComparedToComputed)
                            .addCell(visibleLevelComputed)
                            .addCell(getIcuValue(language))
                            .addCell(sumFound / (double) (sumFound + sumUnconfirmed));

                    // print the totals
                    for (Level level : reversedLevels) {
                        tablePrinter.addCell(levelToProportion.get(level));
                    }

                    tablePrinter.addCell(coreMissingString).finishRow();

                    // now write properties file line

                    if (computed != Level.UNDETERMINED) {
                        propertiesCoverage.printlnWithTabs(
                                propertiesCoverageTabCount,
                                locale
                                        + " ;\t"
                                        + visibleLevelComputed
                                        + " ;\t"
                                        + ENGLISH.getName(locale));
                        // TODO decide whether to restore this
                        //                        Level higher = Level.UNDETERMINED;
                        //                        switch (computed) {
                        //                        default:
                        //                            higher = Level.UNDETERMINED;
                        //                            break;
                        //                        case MODERATE:
                        //                            higher = Level.MODERN;
                        //                            break;
                        //                        case BASIC:
                        //                            higher = Level.MODERATE;
                        //                            break;
                        //                        }
                        //                        double higherProportion = higher ==
                        // Level.UNDETERMINED ? 0d : levelToProportion.get(higher);
                        //
                        //                        if (higherProportion >= THRESHOLD_HIGHER) {
                        //                            propertiesCoverage.println(
                        //                                " ;\t" +
                        // tsvPercent.format(higherProportion) +
                        //                                " ;\t" + higher
                        //                                );
                        //                        } else {
                        //                            propertiesCoverage.println(" ;\t" + "" + "
                        // ;\t" + "");
                        //                        }
                    }
                    localeCount++;
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            String lineToPrint = "\n#EOF";
            propertiesCoverage.printlnWithTabs(propertiesCoverageTabCount, lineToPrint);

            pw.println("<h3><a name='main_table' href='#main_table'>Main Table</a></h3>");
            pw.println(tablePrinter.toTable());

            pw.println(
                    "<h3><a name='level_counts' href='#level_counts'>Level Counts</a></h3>\n"
                            + "<table class='subtle'><tr>\n"
                            + "<th style='text-align:left'>"
                            + "Level"
                            + "</th>"
                            + "<th style='text-align:left'>"
                            + "Languages"
                            + "</th>"
                            + "<th style='text-align:left'>"
                            + "Locales"
                            + "</th>"
                            + "</tr>");
            long totalCount = 0;
            long totalLocaleCount = 0;
            for (Level level : Lists.reverse(Arrays.asList(Level.values()))) {
                final long count = computedLevels.get(level);
                final long localesCount = computedSublocaleLevels.get(level);
                if (count == 0 || level == Level.UNDETERMINED) {
                    continue;
                }
                totalCount += count;
                totalLocaleCount += localesCount;
                String visibleImputed =
                        level == Level.UNDETERMINED
                                ? "<" + Level.BASIC.toString()
                                : level.toString();
                pw.println(
                        "<tr>"
                                + "<th style='text-align:left'>"
                                + visibleImputed
                                + "</th>"
                                + "<td style='text-align:right'>"
                                + count
                                + "</td>"
                                + "<td style='text-align:right'>"
                                + localesCount
                                + "</td>"
                                + "</tr>");
            }
            pw.println(
                    "<tr>"
                            + "<th style='text-align:left'>"
                            + "Total"
                            + "</th>"
                            + "<td style='text-align:right'>"
                            + totalCount
                            + "</td>"
                            + "<td style='text-align:right'>"
                            + totalLocaleCount
                            + "</td>"
                            + "</tr>\n");

            pw.println(
                    "<tr>"
                            + "<th style='text-align:left'>"
                            + "in dev."
                            + "</th>"
                            + "<td style='text-align:right'>"
                            + computedLevels.get(Level.UNDETERMINED)
                            + "</td>"
                            + "<td style='text-align:right'>"
                            + computedSublocaleLevels.get(Level.UNDETERMINED)
                            + "</td>"
                            + "</tr>\n"
                            + "</table>");

            Multimap<Level, String> levelToLocales = TreeMultimap.create();

            for (Entry<String, Collection<String>> entry : pathToLocale.asMap().entrySet()) {
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
                    tsv_missing_summary.println(
                            level
                                    + "\t"
                                    + localeSet.size()
                                    + "\t"
                                    + Joiner.on(" ")
                                            .join(
                                                    localeSet.stream()
                                                            .map(x -> x + getSpecialFlag(x))
                                                            .collect(Collectors.toSet()))
                                    + "\t"
                                    + phString);
                }
            }
            tablePrinter.toTsv(tsv_summary);
            long end = System.currentTimeMillis();
            System.out.println(
                    (end - start)
                            + " millis = "
                            + ((end - start) / localeCount)
                            + " millis/locale");
            ShowPlurals.appendBlanksForScrolling(pw);
        }
    }

    private static String linkTsv(String tsvFileName) {
        return "<a href='" + TSV_BASE + tsvFileName + "' target='cldr-tsv'>" + tsvFileName + "</a>";
    }

    private static String linkTsv(String tsvFileName, String anchorText) {
        return "<a href='" + TSV_BASE + tsvFileName + "' target='cldr-tsv'>" + anchorText + "</a>";
    }

    private static String getSpecialFlag(String locale) {
        return SC.getLocaleCoverageLevel(Organization.special, locale) == Level.UNDETERMINED
                ? ""
                : "‡";
    }

    private static class IterableFilter implements Iterable<String> {
        private Iterable<String> source;

        IterableFilter(Iterable<String> source) {
            this.source = source;
        }

        /**
         * When some paths are defined after submission, we need to change them to COMPREHENSIVE in
         * computing the vetting status.
         */
        private static final Set<String> SUPPRESS_PATHS_AFTER_SUBMISSION = ImmutableSet.of();

        @Override
        public Iterator<String> iterator() {
            return new IteratorFilter(source.iterator());
        }

        private static class IteratorFilter implements Iterator<String> {
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

    private static final CoverageInfo coverageInfo = new CoverageInfo(SUPPLEMENTAL_DATA_INFO);

    private static String spreadsheetLine(
            String locale,
            String language,
            String script,
            String specialFlag,
            String nativeValue,
            Level cldrLocaleLevelGoal,
            Level itemLevel,
            String status,
            String path,
            CLDRFile resolvedFile,
            Multimap<String, String> pathToLocale) {
        if (pathToLocale != null) {
            pathToLocale.put(path, locale);
        }
        //        String stLink = "n/a";
        //        String englishValue = "n/a";
        //        StatusAction action = null;
        //        String icuValue = getIcuValue(locale);

        SurveyToolStatus surveyToolStatus = null;
        String bailey = resolvedFile == null ? "" : resolvedFile.getStringValue(path);

        String phString = "na\tn/a\tn/a\t" + path;
        try {
            PathHeader ph = pathHeaderFactory.fromPath(path);
            phString = ph.toString();
            //            stLink = URLS.forXpath(locale, path);
            //            englishValue = ENGLISH.getStringValue(path);
            //            action = Phase.SUBMISSION.getShowRowAction(dummyPathValueInfo,
            // InputMethod.DIRECT, ph, dummyUserInfo);
        } catch (Exception e) {

        }

        String line =
                specialFlag
                        + language
                        + "\t"
                        + ENGLISH.getName(language)
                        + "\t"
                        + ENGLISH.getName("script", script)
                        + "\t"
                        + cldrLocaleLevelGoal
                        + "\t"
                        + itemLevel
                        + "\t"
                        + (surveyToolStatus == null ? "n/a" : surveyToolStatus.toString())
                        + "\t"
                        + bailey
                        + "\t"
                        + phString
                        + "\t"
                        + PathHeader.getUrlForLocalePath(locale, path);
        return line;
    }

    private static String getIcuValue(String locale) {
        return ICU_Locales.contains(new ULocale(locale)) ? "ICU" : "";
    }

    private static final Set<ULocale> ICU_Locales =
            ImmutableSet.copyOf(ULocale.getAvailableLocales());
}
