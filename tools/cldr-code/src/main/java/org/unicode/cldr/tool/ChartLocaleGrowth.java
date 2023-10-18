package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.LookupType;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

public class ChartLocaleGrowth {

    private static final boolean DEBUG = true;
    private static final char DEBUG_FILTER =
            0; // use letter to only load locales starting with that letter

    private static CLDRConfig testInfo = ToolConfig.getToolInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            testInfo.getSupplementalDataInfo();
    static final Set<String> CldrModernLocales =
            StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, Set.of(Level.MODERN));
    static final Set<String> SpecialLocales =
            StandardCodes.make()
                    .getLocaleCoverageLocales(Organization.special, Set.of(Level.MODERN));

    private static org.unicode.cldr.util.Factory factory =
            testInfo.getCommonAndSeedAndMainAndAnnotationsFactory();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();

    // added info using pattern in VettingViewer.

    private static final RegexLookup<Boolean> COUNT_AS_MISSING_WHEN_ABSENT =
            RegexLookup.<Boolean>of(LookupType.STANDARD, RegexLookup.RegexFinderTransformPath)
                    .add(
                            "//ldml/localeDisplayNames/keys/key[@type=\"(d0|em|fw|i0|k0|lw|m0|rg|s0|ss|t0|x0)\"]",
                            true)
                    .add("//ldml/localeDisplayNames/types/type[@key=\"(em|fw|kr|lw|ss)\"].*", true)
                    .add("//ldml/localeDisplayNames/languages/language[@type=\".*_.*\"]", true)
                    .add(
                            "//ldml/localeDisplayNames/languages/language[@type=\".*\"][@alt=\".*\"]",
                            true)
                    .add(
                            "//ldml/localeDisplayNames/territories/territory[@type=\".*\"][@alt=\".*\"]",
                            true)
                    .add("//ldml/localeDisplayNames/territories/territory[@type=\"EZ\"]", true);

    // private static final String OUT_DIRECTORY = CLDRPaths.GEN_DIRECTORY + "/coverage/"; //
    // CldrUtility.MAIN_DIRECTORY;

    static final Options myOptions = new Options();

    private enum MyOptions {
        filter(".+", ".*", "Filter the information based on locale, using a regex argument."),
        Versions(
                ".+",
                ".*",
                "Filter the information based on cldr version, using a regex argument."),
    //        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft status."),
    ;

        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    private static final Factory pathHeaderFactory = PathHeader.getFactory(ENGLISH);

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.filter, args, true);

        Matcher localeMatcher = PatternCache.get(MyOptions.filter.option.getValue()).matcher("");
        Matcher versionMatcher = PatternCache.get(MyOptions.Versions.option.getValue()).matcher("");

        try (TempPrintWriter out =
                        new TempPrintWriter(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-growth.tsv");
                TempPrintWriter log =
                        new TempPrintWriter(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-growth-log.tsv");
                TempPrintWriter logPaths =
                        new TempPrintWriter(
                                CLDRPaths.CHART_DIRECTORY + "tsv/", "locale-growth-paths.tsv"); ) {
            doGrowth(localeMatcher, versionMatcher, out, log, logPaths);
            return;
        }
    }

    private static void doGrowth(
            Matcher localeMatcher,
            Matcher versionMatcher,
            TempPrintWriter out,
            TempPrintWriter log,
            TempPrintWriter logPaths) {
        TreeMap<String, List<Double>> growthData =
                new TreeMap<>(Ordering.natural().reverse()); // sort by version, descending
        Map<String, FoundAndTotal> latestData = null;
        ReleaseInfo last = versionToYear.get(0);
        for (ReleaseInfo versionNormalizedVersionAndYear : versionToYear) {
            if (versionMatcher != null
                    && !versionMatcher
                            .reset(versionNormalizedVersionAndYear.version.getVersionString(1, 2))
                            .matches()) {
                continue;
            }
            VersionInfo version = versionNormalizedVersionAndYear.version;
            int year = versionNormalizedVersionAndYear.year;
            String dir = ToolConstants.getBaseDirectory(version.getVersionString(2, 3));
            boolean showMissing = last == versionNormalizedVersionAndYear;
            Map<String, FoundAndTotal> currentData =
                    addGrowth(factory, dir, localeMatcher, showMissing, log, logPaths);
            long found = 0;
            long total = 0;
            for (Entry<String, FoundAndTotal> entry : currentData.entrySet()) {
                found += entry.getValue().found;
                total += entry.getValue().total;
            }
            System.out.println(
                    "year\t"
                            + year
                            + "\tversion\t"
                            + version
                            + "\tlocales\t"
                            + currentData.size()
                            + "\tfound\t"
                            + found
                            + "\ttotal\t"
                            + total
                            + "\tdetails\t"
                            + currentData);
            out.flush();
            if (latestData == null) {
                latestData = currentData;
            }
            Counter2<String> completionData = getCompletion(latestData, currentData);
            addCompletionList(year + "", completionData, growthData);
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
            out.println(entry.getKey() + "\t" + Joiner.on("\t").join(entry.getValue()));
        }
    }

    private static final class ReleaseInfo {
        private ReleaseInfo(VersionInfo versionInfo, int year) {
            this.version = versionInfo;
            this.year = year;
        }

        VersionInfo version;
        int year;
    }

    // TODO merge this into ToolConstants, and have the version expressed as VersionInfo.
    private static final List<ReleaseInfo> versionToYear;

    static {
        Object[][] mapping = {
            {VersionInfo.getInstance(44), 2023},
            {VersionInfo.getInstance(42), 2022},
            {VersionInfo.getInstance(40), 2021},
            {VersionInfo.getInstance(38), 2020},
            {VersionInfo.getInstance(36), 2019},
            {VersionInfo.getInstance(34), 2018},
            {VersionInfo.getInstance(32), 2017},
            {VersionInfo.getInstance(30), 2016},
            {VersionInfo.getInstance(28), 2015},
            {VersionInfo.getInstance(26), 2014},
            {VersionInfo.getInstance(24), 2013},
            {VersionInfo.getInstance(22, 1), 2012},
            {VersionInfo.getInstance(2, 0, 1), 2011},
            {VersionInfo.getInstance(1, 9, 1), 2010},
            {VersionInfo.getInstance(1, 7, 2), 2009},
            {VersionInfo.getInstance(1, 6, 1), 2008},
            {VersionInfo.getInstance(1, 5, 1), 2007},
            {VersionInfo.getInstance(1, 4, 1), 2006},
            {VersionInfo.getInstance(1, 3), 2005},
            {VersionInfo.getInstance(1, 2), 2004},
            {VersionInfo.getInstance(1, 1, 1), 2003},
        };
        List<ReleaseInfo> _versionToYear = new ArrayList<>();
        for (Object[] row : mapping) {
            _versionToYear.add(new ReleaseInfo((VersionInfo) row[0], (int) row[1]));
        }
        versionToYear = ImmutableList.copyOf(_versionToYear);
    }

    private static void addCompletionList(
            String version,
            Counter2<String> completionData,
            TreeMap<String, List<Double>> growthData) {
        List<Double> x = new ArrayList<>();
        for (String key : completionData.getKeysetSortedByCount(false)) {
            x.add(completionData.getCount(key));
        }
        growthData.put(version, x);
        System.out.println(version + "\t" + x.size());
    }

    private static Counter2<String> getCompletion(
            Map<String, FoundAndTotal> latestData, Map<String, FoundAndTotal> currentData) {
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

    private static class FoundAndTotal {
        final long found;
        final long total;

        @SafeVarargs
        private FoundAndTotal(Counter<Level>... counters) {
            final long[] count = {0, 0, 0};
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

    private static Map<String, FoundAndTotal> addGrowth(
            org.unicode.cldr.util.Factory latestFactory,
            String dir,
            Matcher localeMatcher,
            boolean showMissing,
            TempPrintWriter log,
            TempPrintWriter logPaths) {
        final File mainDir = new File(dir + "/common/main/");
        final File annotationDir = new File(dir + "/common/annotations/");
        File[] paths =
                annotationDir.exists() ? new File[] {mainDir, annotationDir} : new File[] {mainDir};
        org.unicode.cldr.util.Factory newFactory;
        try {
            newFactory = SimpleFactory.make(paths, ".*");
        } catch (RuntimeException e1) {
            throw e1;
        }
        Map<String, FoundAndTotal> data = new HashMap<>();
        char c = 0;
        Set<String> latestAvailable = newFactory.getAvailableLanguages();
        boolean firstShowMissing = true;
        for (String locale : newFactory.getAvailableLanguages()) {
            if (!localeMatcher.reset(locale).matches()) {
                continue;
            }
            if (!latestAvailable.contains(locale)) {
                continue;
            }
            if (SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales().contains(locale)
                    || locale.equals(LocaleNames.ROOT)
                    || locale.equals(LocaleNames.UND)
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
                System.out.println(
                        "Can't make latest CLDRFile for: "
                                + locale
                                + "\tpast: "
                                + mainDir
                                + "\tlatest: "
                                + Arrays.asList(latestFactory.getSourceDirectories()));
                continue;
            }
            CLDRFile file = null;
            try {
                file = newFactory.make(locale, true);
            } catch (Exception e2) {
                System.out.println("Can't make CLDRFile for: " + locale + "\tpast: " + mainDir);
                continue;
            }

            Counter<Level> foundCounter = new Counter<>();
            Counter<Level> unconfirmedCounter = new Counter<>();
            Counter<Level> missingCounter = new Counter<>();
            Set<String> unconfirmedPaths = null;
            Relation<MissingStatus, String> missingPaths = null;
            unconfirmedPaths = new LinkedHashSet<>();
            missingPaths =
                    Relation.of(
                            new LinkedHashMap<MissingStatus, Set<String>>(), LinkedHashSet.class);
            VettingViewer.getStatus(
                    latestFile.fullIterable(),
                    file,
                    pathHeaderFactory,
                    foundCounter,
                    unconfirmedCounter,
                    missingCounter,
                    missingPaths,
                    unconfirmedPaths);

            if (showMissing) {
                if (CldrModernLocales.contains(locale)) {
                    final boolean isSpecial = SpecialLocales.contains(locale);
                    if (firstShowMissing) {
                        firstShowMissing = false;
                        log.printlnWithTabs(
                                16,
                                "Locale\tTC"
                                        + "\tCore\tUnc\tMiss"
                                        + "\tBasic\tUnc\tMiss"
                                        + "\tModer\tUnc\tMiss"
                                        + "\tModern\tUnc\tMiss"
                                        + "\tTotal\tUnc\tMiss");
                        logPaths.printlnWithTabs(3, "Locale\tLevel\tStatus\tPath");
                    }
                    log.printlnWithTabs(
                            16,
                            locale
                                    + "\t"
                                    + (isSpecial ? "" : "TC")
                                    + show(
                                            Level.CORE,
                                            foundCounter,
                                            unconfirmedCounter,
                                            missingCounter)
                                    + show(
                                            Level.BASIC,
                                            foundCounter,
                                            unconfirmedCounter,
                                            missingCounter)
                                    + show(
                                            Level.MODERATE,
                                            foundCounter,
                                            unconfirmedCounter,
                                            missingCounter)
                                    + show(
                                            Level.MODERN,
                                            foundCounter,
                                            unconfirmedCounter,
                                            missingCounter)
                                    + show(
                                            null, // total
                                            foundCounter,
                                            unconfirmedCounter,
                                            missingCounter));
                    if (!isSpecial) {
                        long count = unconfirmedCounter.getTotal() + missingCounter.getTotal();
                        for (Entry<MissingStatus, String> statusAndPath : missingPaths.entrySet()) {
                            logPaths.printlnWithTabs(
                                    3,
                                    locale
                                            + "\t"
                                            + count
                                            + "\t"
                                            + statusAndPath.getKey()
                                            + "\t"
                                            + statusAndPath.getValue());
                        }
                        for (String path : unconfirmedPaths) {
                            logPaths.printlnWithTabs(
                                    3, locale + "\t" + count + "\tunconfirmed\t" + path);
                        }
                    }
                    int line = 0;
                }
            }

            // HACK
            Set<Entry<MissingStatus, String>> missingRemovals = new HashSet<>();
            for (Entry<MissingStatus, String> e : missingPaths.keyValueSet()) {
                if (e.getKey() == MissingStatus.ABSENT) {
                    final String path = e.getValue();
                    if (COUNT_AS_MISSING_WHEN_ABSENT.get(path) != null) {
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

            if (false && showMissing) {
                int count = 0;
                for (String s : unconfirmedPaths) {
                    System.out.println(
                            ++count
                                    + "\t"
                                    + locale
                                    + "\t"
                                    + CLDRFile.DraftStatus.unconfirmed.name()
                                    + "\t"
                                    + s);
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

    /** "\tCore\tUnc\tMiss" */
    private static String show(
            Level level,
            Counter<Level> foundCounter,
            Counter<Level> unconfirmedCounter,
            Counter<Level> missingCounter) {
        return "\t"
                + (level != null ? foundCounter.get(level) : foundCounter.getTotal())
                + "\t"
                + (level != null ? unconfirmedCounter.get(level) : unconfirmedCounter.getTotal())
                + "\t"
                + (level != null ? missingCounter.get(level) : missingCounter.getTotal());
    }
}
