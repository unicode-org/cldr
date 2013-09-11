package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Relation;

public class ShowLocaleCoverage {
    public static TestInfo testInfo = TestInfo.getInstance();
    // added info using pattern in VettingViewer.

    final static Options myOptions = new Options();
    private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/coverage/"; // CldrUtility.MAIN_DIRECTORY;

    private static final String TEST_PATH = "//ldml/dates/calendars/calendar[@type=\"chinese\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument."),
        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft status."),
        organization(".+", null, "Only locales for organization"),
        version(".+", "23.0", "To get different versions");

        // targetDirectory(".+", CldrUtility.CHART_DIRECTORY + "keyboards/", "The target directory."),
        // layouts(null, null, "Only create html files for keyboard layouts"),
        // repertoire(null, null, "Only create html files for repertoire"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static final EnumSet<Level> skipPrintingLevels = EnumSet.of(
        Level.UNDETERMINED,
        Level.CORE,
        Level.POSIX,
        Level.MINIMAL,
        Level.OPTIONAL,
        Level.COMPREHENSIVE
        );

    static RegexLookup<Boolean> SKIP_PATHS = new RegexLookup<Boolean>()
        .add("\\[@alt=\"accounting\"]", true)
        .add("\\[@alt=\"variant\"]", true)
        .add("^//ldml/localeDisplayNames/territories/territory.*@alt=\"short", true)
        .add("^//ldml/localeDisplayNames/languages/language.*_", true)
        .add("^//ldml/numbers/currencies/currency.*/symbol", true)
        .add("^//ldml/characters/exemplarCharacters", true);

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.filter, args, true);
        Matcher matcher = Pattern.compile(MyOptions.filter.option.getValue()).matcher("");
        DraftStatus minimumDraftStatus = DraftStatus.forString(MyOptions.draftStatus.option.getValue());
        Set<String> locales = null;
        String organization = MyOptions.organization.option.getValue();
        boolean useOrgLevel = MyOptions.organization.option.doesOccur();
        if (useOrgLevel) {
            locales = testInfo.getStandardCodes().getLocaleCoverageLocales(organization);
        }

        org.unicode.cldr.util.Factory factory;
        if (MyOptions.version.option.doesOccur()) {
            String number = MyOptions.version.option.getValue().trim();
            if (!number.contains(".")) {
                number += ".0";
            }
            factory = org.unicode.cldr.util.Factory.make(
                CldrUtility.ARCHIVE_DIRECTORY + "cldr-" + number + "/common/main/", ".*");
        } else {
            factory = testInfo.getCldrFactory();
        }
        Set<String> availableLanguages = factory.getAvailableLanguages();
        System.out.println("# Checking: " + availableLanguages);

        Set<String> extraCheckLocales = new HashSet(); // testInfo.getStandardCodes().getLocaleCoverageLocales("google");

        Relation<MissingStatus, String> missingPaths = Relation.of(new EnumMap<MissingStatus, Set<String>>(
            MissingStatus.class), TreeSet.class, CLDRFile.ldmlComparator);
        Set<String> unconfirmed = new TreeSet(CLDRFile.ldmlComparator);

        LanguageTagParser ltp = new LanguageTagParser();
        Map<String, String> likely = testInfo.getSupplementalDataInfo().getLikelySubtags();
        Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();
        CLDRFile english = testInfo.getEnglish();

        // Map<String,Counter<Level>> counts = new HashMap();
        //        System.out.print("Script\tEnglish\tNative\tCode\tCode*");
        //        for (Level level : Level.values()) {
        //            if (skipPrintingLevels.contains(level)) {
        //                continue;
        //            }
        //            System.out.print("\t≤" + level + " (f)\t(u)\t(m)");
        //        }
        //        System.out.println();
        Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getCldrFactory().make("en", true));

        PrintWriter out = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "simpleCoverage.tsv");
        out.println("#Script\tEnglish\tNative\tCode*\tCode*\tEnglish\tNative\tCorrect Value (or ‘OK’ if F is good)\tSection\tPage\tHeader\tCode\tPath");

        Counter<Level> foundCounter = new Counter<Level>();
        Counter<Level> unconfirmedCounter = new Counter<Level>();
        Counter<Level> missingCounter = new Counter<Level>();

        int localeCount = 0;
        System.out
            .println("#Script\tEnglish\tNative\tCode\tCode*\tmodern: f/(f+u+m)\tmodern:(f+u)/(f+u+m)\t≤basic (f)\t(u)\t(m)\t≤moderate (f)\t(u)\t(m)\t≤modern (f)\t(u)\t(m)\tf= found; u = unconfirmed; m = missing");
        long start = System.currentTimeMillis();
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
                if (defaultContents.contains(locale) || "root".equals(locale)) {
                    continue;
                }
                boolean capture = locale.equals("en");
                String region = ltp.set(locale).getRegion();
                if (!region.isEmpty()) continue; // skip regions

                String language = ltp.getLanguage();
                String script = ltp.getScript();
                if (script.isEmpty()) {
                    String likelySubtags = likely.get(language);
                    if (likelySubtags != null) {
                        script = ltp.set(likelySubtags).getScript();
                        if ("bs".equals(language)) {
                            script = "Latn";
                        }
                    }
                }
                missingPaths.clear();
                unconfirmed.clear();

                final CLDRFile file = factory.make(locale, true, minimumDraftStatus);

                VettingViewer.getStatus(testInfo.getEnglish().fullIterable(), file,
                    pathHeaderFactory, foundCounter, unconfirmedCounter,
                    missingCounter, missingPaths, unconfirmed);

                String header = script
                    + "\t" + testInfo.getEnglish().getName(language)
                    + "\t" + file.getName(language)
                    + "\t" + language
                    + "\t" + locale;
                System.out.print(header);

                int sumFound = 0;
                int sumMissing = 0;
                int sumUnconfirmed = 0;
                double modernUnconfirmedCoverage = 0.0d;
                double modernConfirmedCoverage = 0.0d;
                StringBuilder b = new StringBuilder();
                for (Level level : Level.values()) {
                    sumFound += foundCounter.get(level);
                    sumUnconfirmed += unconfirmedCounter.get(level);
                    sumMissing += missingCounter.get(level);
                    if (useOrgLevel && testInfo.getStandardCodes().getLocaleCoverageLevel(organization, locale) != level) {
                        continue;
                    } else if (skipPrintingLevels.contains(level)) {
                        continue;
                    }

                    b.append("\t" + sumFound + "\t" + sumUnconfirmed + "\t" + sumMissing);
                    if (level == Level.MODERN) {
                        modernConfirmedCoverage = (sumFound) / (double) (sumFound + sumUnconfirmed + sumMissing);
                        modernUnconfirmedCoverage = (sumFound + sumUnconfirmed) / (double) (sumFound + sumUnconfirmed + sumMissing);
                        // "modern: f/(f+u+m)"  modern:(f+u)/(f+u+m)
                    }
                }
                System.out.print("\t" + modernConfirmedCoverage + "\t" + modernUnconfirmedCoverage);
                System.out.print(b);

                if (modernUnconfirmedCoverage >= 0.99d
                    || extraCheckLocales.contains(locale)) {
                    for (String path : unconfirmed) {
                        PathHeader ph = pathHeaderFactory.fromPath(path);
                        String line = header + "\t" + english.getStringValue(path)
                            + "\t" + file.getStringValue(path)
                            + "\t" + "UNCONFIRMED"
                            + "\t" + ph + "\t" + path;
                        if (SKIP_PATHS.get(path) != null) {
                            //System.out.println("\nSKIP: " + line);
                        } else {
                            out.println(line);
                        }
                    }
                    for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
                        String path = entry.getValue();
                        PathHeader ph = pathHeaderFactory.fromPath(path);
                        String line = header + "\t" + english.getStringValue(path)
                            + "\t???"
                            + "\t" + entry.getKey()
                            + "\t" + ph + "\t" + path;
                        if (SKIP_PATHS.get(path) != null) {
                            //System.out.println("\nSKIP: " + line);
                        } else {
                            out.println(line);
                        }
                    }
                    out.flush();
                }

                System.out.println();
                localeCount++;
            } catch (Exception e) {
            }
        }
        out.close();

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
}
