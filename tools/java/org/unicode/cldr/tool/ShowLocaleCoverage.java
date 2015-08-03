package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

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
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

import com.google.common.collect.Ordering;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.lang.UCharacter;

public class ShowLocaleCoverage {
    private static final boolean DEBUG = false;

    private static final String LATEST = ToolConstants.CHART_VERSION;
    private static final double CORE_SIZE = (double) (CoreItems.values().length - CoreItems.ONLY_RECOMMENDED.size());
    public static CLDRConfig testInfo = ToolConfig.getToolInstance();
    private static final StandardCodes SC = testInfo.getStandardCodes();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final StandardCodes STANDARD_CODES = SC;
    // added info using pattern in VettingViewer.

    //private static final String OUT_DIRECTORY = CLDRPaths.GEN_DIRECTORY + "/coverage/"; // CldrUtility.MAIN_DIRECTORY;

    final static Options myOptions = new Options();

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument."),
        //        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft status."),
        growth("true", "true", "Compute growth data"),
        organization(".+", null, "Only locales for organization"),
        version(".+", LATEST, "To get different versions"),
        rawData(null, null, "Output the raw data from all coverage levels"),
        targetDir(".*", CLDRPaths.GEN_DIRECTORY + "/statistics/", "target output file."),
        directories("(.*:)?[a-z]+(,[a-z]+)*", "common", "Space-delimited list of main source directories: common,seed,exemplar.\n" +
            "Optional, <baseDir>:common,seed"), ;

        // targetDirectory(".+", CldrUtility.CHART_DIRECTORY + "keyboards/", "The target directory."),
        // layouts(null, null, "Only create html files for keyboard layouts"),
        // repertoire(null, null, "Only create html files for repertoire"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static RegexLookup<Boolean> SKIP_PATHS = new RegexLookup<Boolean>()
        .add("\\[@alt=\"accounting\"]", true)
        .add("\\[@alt=\"variant\"]", true)
        .add("^//ldml/localeDisplayNames/territories/territory.*@alt=\"short", true)
        .add("^//ldml/localeDisplayNames/languages/language.*_", true)
        .add("^//ldml/numbers/currencies/currency.*/symbol", true)
        .add("^//ldml/characters/exemplarCharacters", true);

    static org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
    static DraftStatus minimumDraftStatus = DraftStatus.unconfirmed;
    static final Factory pathHeaderFactory = PathHeader.getFactory(ENGLISH);

    static boolean RAW_DATA = true;
    private static Set<String> COMMON_LOCALES;

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.filter, args, true);

        Matcher matcher = PatternCache.get(MyOptions.filter.option.getValue()).matcher("");

        if (MyOptions.growth.option.doesOccur()) {
            try (PrintWriter out 
                = BagFormatter.openUTF8Writer(MyOptions.targetDir.option.getValue(), 
                    "showLocaleGrowth.txt")) {
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
                CLDRConfig cldrConfig = CLDRConfig.getInstance();
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
        Map<String, FoundAndTotal> latestData = addGrowth(factory, matcher);
        addCompletionList(LATEST, getCompletion(latestData, latestData), growthData);
        if (DEBUG) System.out.println(latestData);
        //System.out.println(growthData);
        for (String dir : new File(CLDRPaths.ARCHIVE_DIRECTORY).list()) {
            if (!dir.startsWith("cldr")) {
                continue;
            }
            String version = getNormalizedVersion(dir);
//            if (version.compareTo("12") < 0) {
//                continue;
//            }
            org.unicode.cldr.util.Factory newFactory = org.unicode.cldr.util.Factory.make(
                CLDRPaths.ARCHIVE_DIRECTORY + "/" + dir + "/common/main/", ".*");
            Map<String, FoundAndTotal> currentData = addGrowth(newFactory, matcher);
            if (DEBUG) System.out.println(version + "\t" + currentData);
            Counter2<String> completionData = getCompletion(latestData, currentData);
            //System.out.println(version + "\t" + completionData);
            addCompletionList(version, completionData, growthData);
            if (DEBUG) System.out.println(currentData);
        }
        boolean first = true;
        for (Entry<String, List<Double>> entry : growthData.entrySet()) {
            if (first) {
                for (int i = 0; i < entry.getValue().size(); ++i) {
                    out.print("\t" + i);
                }
                out.println();
                first = false;
            }
            out.println(entry.getKey() + "\t" + CollectionUtilities.join(entry.getValue(), "\t"));
        }
    }

    public static String getNormalizedVersion(String dir) {
        String rawVersion = dir.substring(dir.indexOf('-') + 1);
        int firstDot = rawVersion.indexOf('.');
        int secondDot = rawVersion.indexOf('.', firstDot + 1);
        if (secondDot > 0) {
            rawVersion = rawVersion.substring(0, firstDot) + rawVersion.substring(firstDot + 1, secondDot);
        } else {
            rawVersion = rawVersion.substring(0, firstDot);
        }
        return rawVersion;
    }

    public static void addCompletionList(String version, Counter2<String> completionData, TreeMap<String, List<Double>> growthData) {
        List x = new ArrayList();
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

    private static Map<String, FoundAndTotal> addGrowth(org.unicode.cldr.util.Factory newFactory, Matcher matcher) {
        Map<String, FoundAndTotal> data = new HashMap<>();
        for (String locale : newFactory.getAvailableLanguages()) {
            if (!matcher.reset(locale).matches()) {
                continue;
            }
            if (SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales().contains(locale)
                || locale.equals("root")
                || locale.equals("supplementalData")) {
                continue;
            }
            System.out.println("\t" + locale);
            final CLDRFile file = newFactory.make(locale, true);
            Counter<Level> foundCounter = new Counter<Level>();
            Counter<Level> unconfirmedCounter = new Counter<Level>();
            Counter<Level> missingCounter = new Counter<Level>();
            VettingViewer.getStatus(ENGLISH.fullIterable(), file,
                pathHeaderFactory, foundCounter, unconfirmedCounter,
                missingCounter, null, null);
            data.put(locale, new FoundAndTotal(foundCounter, unconfirmedCounter, missingCounter));
        }
        return Collections.unmodifiableMap(data);
    }

    public static void showCoverage(PrintWriter index) throws IOException {
        showCoverage(index, PatternCache.get(".*").matcher(""), null, false);
    }

    public static void showCoverage(PrintWriter index, Matcher matcher, Set<String> locales, boolean useOrgLevel) throws IOException {
        final String title = "Locale Coverage";
        final PrintWriter pw = new PrintWriter(new FormattedFileWriter(null, title, null, null));
        printData(pw, locales, matcher, useOrgLevel);
        new ShowPlurals().appendBlanksForScrolling(pw);
        pw.close();
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

    static void printData(PrintWriter pw, Set<String> locales, Matcher matcher, boolean useOrgLevel) {
//        Set<String> checkModernLocales = STANDARD_CODES.getLocaleCoverageLocales("google", EnumSet.of(Level.MODERN));
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

        System.out.println(CollectionUtilities.join(languageToRegion.keyValuesSet(), "\n"));

        System.out.println("# Checking: " + availableLanguages);
        pw.println("<p style='text-align: left'>This chart shows the coverage levels for this release. " +
            "The UC% figures include unconfirmed values: these values are typically ignored by implementations. " +
            "A high-level summary of the meaning of the coverage values are at " +
            "<a target='_blank' href='http://www.unicode.org/reports/tr35/tr35-info.html#Coverage_Levels'>Coverage Levels</a>. " +
            "The Core values are described on " +
            "<a target='_blank' href='http://cldr.unicode.org/index/cldr-spec/minimaldata'>Core Data</a>." +
            "</p>");

        Relation<MissingStatus, String> missingPaths = Relation.of(new EnumMap<MissingStatus, Set<String>>(
            MissingStatus.class), TreeSet.class, CLDRFile.getComparator(DtdType.ldml));
        Set<String> unconfirmed = new TreeSet<String>(CLDRFile.getComparator(DtdType.ldml));

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

        PrintWriter out;
        try {
            out = BagFormatter.openUTF8Writer(MyOptions.targetDir.option.getValue(), "simpleCoverage.tsv");
        } catch (IOException e1) {
            throw new IllegalArgumentException(e1);
        }

        out.println("#LCode\tEnglish Name\tScript\tEnglish Value\tNative Value\tStatus\tST Link\tSection\tPage\tHeader\tCode\tPath");

        Counter<Level> foundCounter = new Counter<Level>();
        Counter<Level> unconfirmedCounter = new Counter<Level>();
        Counter<Level> missingCounter = new Counter<Level>();

        List<Level> reversedLevels = new ArrayList();
        reversedLevels.add(Level.MODERN);
        reversedLevels.add(Level.MODERATE);
        reversedLevels.add(Level.BASIC);
        if (RAW_DATA) {
            reversedLevels.add(Level.MINIMAL);
            reversedLevels.add(Level.POSIX);
            reversedLevels.add(Level.CORE);
        }
        PrintWriter out2;
        try {
            out2 = BagFormatter.openUTF8Writer(MyOptions.targetDir.option.getValue(), 
                "showLocaleCoverage.txt");
        } catch (IOException e1) {
            throw new IllegalArgumentException(e1);
        }

        out2.print("Code\tCom?\tEnglish Name\tNative Name\tScript\tSublocales\tStrings");
        for (Level level : reversedLevels) {
            out2.print("\t" + level + " %\t" + level + " UC%");
        }
        out2.println();
        //System.out.println("\tCore*\nCore* Missing");
        int localeCount = 0;

        final TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true).setBreakSpans(true)
        .addColumn("English Name", "class='source'", null, "class='source'", true).setBreakSpans(true)
        .addColumn("Native Name", "class='source'", null, "class='source'", true).setBreakSpans(true)
        .addColumn("Script", "class='source'", null, "class='source'", true).setBreakSpans(true)
        .addColumn("CLDR target", "class='source'", null, "class='source'", true).setBreakSpans(true)
        .addColumn("Sublocales", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
        .setCellPattern("{0,number}")
        .addColumn("Confirmed Fields", "class='target'", null, "class='targetRight'", true).setBreakSpans(true)
        .setCellPattern("{0,number}")
        //.addColumn("Target Level", "class='target'", null, "class='target'", true).setBreakSpans(true)
        ;

        for (Level level : reversedLevels) {
            String titleLevel = level.toString();
            tablePrinter
            .addColumn(UCharacter.toTitleCase(titleLevel, null) + "%", "class='target'", null, "class='targetRight'", true)
            .setCellPattern("{0,number,0%}")
            .setBreakSpans(true);
            if (level == Level.MODERN) {
                tablePrinter.setSortPriority(0).setSortAscending(false);
            }
            tablePrinter
            .addColumn(UCharacter.toTitleCase(titleLevel, null) + " UC%", "class='target'", null, "class='targetRight'", true)
            .setCellPattern("{0,number,0%}")
            .setBreakSpans(true);
        }
        tablePrinter
        .addColumn("Core", "class='target'", null, "class='targetRight'", true)
        .setCellPattern("{0,number,0%}")
        .setBreakSpans(true);

        long start = System.currentTimeMillis();
        LikelySubtags likelySubtags = new LikelySubtags();

        EnumMap<Level, Double> targetLevel = new EnumMap<>(Level.class);
        targetLevel.put(Level.CORE, 2 / 100d);
        targetLevel.put(Level.POSIX, 4 / 100d);
        targetLevel.put(Level.MINIMAL, 6 / 100d);
        targetLevel.put(Level.BASIC, 16 / 100d);
        targetLevel.put(Level.MODERATE, 33 / 100d);
        targetLevel.put(Level.MODERN, 100 / 100d);

//        NumberFormat percentFormat = NumberFormat.getPercentInstance(ULocale.ENGLISH);
//        percentFormat.setMaximumFractionDigits(2);
//        percentFormat.setMinimumFractionDigits(2);
//        NumberFormat intFormat = NumberFormat.getIntegerInstance(ULocale.ENGLISH);

        for (String locale : availableLanguages) {
            try {
                if (locale.contains("supplemental")) { // for old versions
                    continue;
                }
                if (locales != null && !locales.contains(locale)) {
                    String base = CLDRLocale.getInstance(locale).getLanguage();
                    if (!locales.contains(base)) {
                        continue;
                    }
                }
                if (!matcher.reset(locale).matches()) {
                    continue;
                }
                if (defaultContents.contains(locale) || "root".equals(locale) || "und".equals(locale)) {
                    continue;
                }
                //boolean capture = locale.equals("en");
                String region = ltp.set(locale).getRegion();
                if (!region.isEmpty()) continue; // skip regions

                Level cldrLevel = SC.getLocaleCoverageLevel(Organization.cldr.toString(), locale);
                String isCommonLocale =
                    Level.MODERN == cldrLevel ? "C*"
                        : COMMON_LOCALES.contains(locale) ? "C"
                            : "";

                String max = likelySubtags.maximize(locale);
                String script = ltp.set(max).getScript();

                String language = likelySubtags.minimize(locale);
                Level currentLevel = STANDARD_CODES.getLocaleCoverageLevel("cldr", locale);
//                Level otherLevel = STANDARD_CODES.getLocaleCoverageLevel("apple", locale);
//                if (otherLevel.compareTo(currentLevel) > 0 
//                    && otherLevel.compareTo(Level.MODERN) <= 0) {
//                    currentLevel = otherLevel;
//                }

                missingPaths.clear();
                unconfirmed.clear();

                final CLDRFile file = factory.make(locale, true, minimumDraftStatus);

                VettingViewer.getStatus(ENGLISH.fullIterable(), file,
                    pathHeaderFactory, foundCounter, unconfirmedCounter,
                    missingCounter, missingPaths, unconfirmed);

                Set<String> sublocales = languageToRegion.get(language);
                if (sublocales == null) {
                    //System.err.println("No Sublocales: " + language);
                    sublocales = Collections.EMPTY_SET;
                }

//                List s = Lists.newArrayList(file.fullIterable());

                tablePrinter
                .addRow()
                .addCell(language)
                .addCell(ENGLISH.getName(language))
                .addCell(file.getName(language))
                .addCell(script)
                .addCell(currentLevel)
                .addCell(sublocales.size());
                String header =
                    language
                    + "\t" + isCommonLocale
                    + "\t" + ENGLISH.getName(language)
                    + "\t" + file.getName(language)
                    + "\t" + script
                    + "\t" + sublocales.size()
                    //+ "\t" + currentLevel
                    ;

                int sumFound = 0;
                int sumMissing = 0;
                int sumUnconfirmed = 0;
                double modernUnconfirmedCoverage = 0.0d;
                double modernConfirmedCoverage = 0.0d;
                StringBuilder b = new StringBuilder();

                // get the totals

                EnumMap<Level, Integer> totals = new EnumMap<>(Level.class);
                EnumMap<Level, Integer> confirmed = new EnumMap<>(Level.class);
                EnumMap<Level, Integer> unconfirmedByLevel = new EnumMap<>(Level.class);
                for (Level level : Level.values()) {
                    sumFound += foundCounter.get(level);
                    sumUnconfirmed += unconfirmedCounter.get(level);
                    sumMissing += missingCounter.get(level);

                    confirmed.put(level, sumFound);
                    unconfirmedByLevel.put(level, sumFound + sumUnconfirmed);
                    totals.put(level, sumFound + sumUnconfirmed + sumMissing);
                }
                double modernTotal = totals.get(Level.MODERN);
                double modernConfirmed = confirmed.get(Level.MODERN);

                tablePrinter
                .addCell(sumFound);

                header += "\t" + sumFound;

                // print the totals

                for (Level level : reversedLevels) {
                    if (useOrgLevel && currentLevel != level) {
                        continue;
                    }
                    int confirmedCoverage = confirmed.get(level);
                    int unconfirmedCoverage = unconfirmedByLevel.get(level);
                    double total = totals.get(level);

                    tablePrinter
                    .addCell(confirmedCoverage / total)
                    .addCell(unconfirmedCoverage / total);

                    if (RAW_DATA) {
                        header += "\t" + confirmedCoverage / total
                            + "\t" + unconfirmedCoverage / total;
                    } else {
                        Double factor = targetLevel.get(level) / (total / modernTotal);
                        header += "\t" + factor * confirmedCoverage / modernTotal
                            + "\t" + factor * unconfirmedCoverage / modernTotal;
                    }
                }
                Set<String> detailedErrors = new LinkedHashSet<>();
                Set<CoreItems> coverage = new TreeSet<>(
                    CoreCoverageInfo.getCoreCoverageInfo(file, detailedErrors));
                coverage.removeAll(CoreItems.ONLY_RECOMMENDED);
                Set<CoreItems> missing = EnumSet.allOf(CoreItems.class);
                missing.removeAll(coverage);
                missing.removeAll(CoreItems.ONLY_RECOMMENDED);

                double coreValue = coverage.size() / CORE_SIZE;
                tablePrinter
                .addCell(coreValue)
                .finishRow();

                out2.println(header + "\t" + coreValue + "\t" + CollectionUtilities.join(missing, ", "));

                // Write missing paths (for >99% and specials

                if (checkModernLocales.contains(locale)) {
                    for (String path : unconfirmed) {
//                        String header2 = 
//                            language
//                            + "\t" + ENGLISH.getName(language)
//                            + "\t" + script
//                            ;
//                        PathHeader ph = pathHeaderFactory.fromPath(path);
//                        String line = header2 
//                            + "\t" + ENGLISH.getStringValue(path)
//                            + "\t" + file.getStringValue(path)
//                            + "\t" + "UNCONFIRMED"
//                            + "\t" + URLS.forXpath(locale, ph.getOriginalPath())
//                            + "\t" + ph + "\t" + path;
                        String line = spreadsheetLine(locale, script, language, "UNCONFIRMED", path, file.getStringValue(path));
                        if (SKIP_PATHS.get(path) != null) {
                            //System.out.println("\nSKIP: " + line);
                        } else {
                            out.println(line);
                        }
                    }
                    for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                        String line = spreadsheetLine(locale, script, language, entry.getKey().toString(), entry.getValue(), "???");
                        if (SKIP_PATHS.get(entry.getValue()) != null) {
                            //System.out.println("\nSKIP: " + line);
                        } else {
                            out.println(line);
                        }
                    }
                    out.flush();
                }

                localeCount++;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        pw.println(tablePrinter.toTable());
        out.close();
        out2.close();

        long end = System.currentTimeMillis();
        System.out.println((end - start) + " millis = "
            + ((end - start) / localeCount) + " millis/locale");

        //        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("en");
        //
        //        for (Entry<MissingStatus, Set<String>> entity : missingPaths.keyValuesSet()) {
        //            for (PathHeader s : CldrUtility.transform(entity.getValue(), pathHeaderFactory, new TreeSet<PathHeader>())) {
        //                System.out.println(entity.getKey() + "\t" + coverageLevel2.getLevel(s.getOriginalPath()) + "\t" + s
        //                        + "\t\t" + s.getOriginalPath());
        //            }
        //        }
    }

    public static String spreadsheetLine(String locale, String script, String language, String status, String path, String nativeValue) {
        PathHeader ph = pathHeaderFactory.fromPath(path);
        final String stLink = URLS.forXpath(locale, ph.getOriginalPath());
        String englishValue = ENGLISH.getStringValue(path);
        String line = language
            + "\t" + ENGLISH.getName(language)
            + "\t" + ENGLISH.getName("script", script)
            + "\t" + englishValue
            + "\t" + nativeValue
            + "\t" + status
            + "\t" + stLink
            + "\t" + ph
            + "\t" + path;
        return line;
    }

    private static CLDRURLS URLS = CLDRConfig.getInstance().urls();

}
