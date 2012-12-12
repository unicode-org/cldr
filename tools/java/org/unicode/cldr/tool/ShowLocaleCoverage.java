package org.unicode.cldr.tool;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

import com.ibm.icu.dev.util.Relation;

public class ShowLocaleCoverage {
    private static TestInfo testInfo = TestInfo.getInstance();
    // added info using pattern in VettingViewer.

    final static Options myOptions = new Options();

    private static final String TEST_PATH = "//ldml/dates/calendars/calendar[@type=\"chinese\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument.");
        // targetDirectory(".+", CldrUtility.CHART_DIRECTORY + "keyboards/", "The target directory."),
        // layouts(null, null, "Only create html files for keyboard layouts"),
        // repertoire(null, null, "Only create html files for repertoire"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    public static void main(String[] args) {
        myOptions.parse(MyOptions.filter, args, true);
        Matcher matcher = Pattern.compile(MyOptions.filter.option.getValue()).matcher("");

        Relation<MissingStatus, PathHeader> missingHeaders = Relation.of(new EnumMap<MissingStatus, Set<PathHeader>>(
            MissingStatus.class), TreeSet.class);

        LanguageTagParser ltp = new LanguageTagParser();
        Map<String, String> likely = testInfo.getSupplementalDataInfo().getLikelySubtags();
        Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();

        // Map<String,Counter<Level>> counts = new HashMap();
        System.out.print("Script\tNative\tEnglish\tCode\tCode*");
        for (Level level : Level.values()) {
            System.out.print("\t≤" + level);
        }
        System.out.println();
        Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getCldrFactory().make("en", true));

        EnumSet<Level> skipPrintingLevels = EnumSet.of(
            Level.UNDETERMINED,
            Level.CORE,
            Level.OPTIONAL,
            Level.COMPREHENSIVE);

        for (String locale : testInfo.getCldrFactory().getAvailable()) {
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

            CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(locale);
            Counter<Level> foundCounter = new Counter();
            Counter<Level> missingCounter = new Counter();
            final CLDRFile file = testInfo.getCldrFactory().make(locale, true);
            Matcher altProposed = VettingViewer.ALT_PROPOSED.matcher("");
            Status status = new Status();
            boolean latin = VettingViewer.isLatinScriptLocale(file);

            for (String path : file.fullIterable()) {
                if (path.equals(TEST_PATH)) {
                    int x = 0; // debug
                }
                if (path.contains("unconfirmed")
                    || path.contains("provisional")
                    || path.contains("/alias")
                    || path.contains("/references")
                    || altProposed.reset(path).find()) {
                    continue;
                }

                PathHeader ph = pathHeaderFactory.fromPath(path);
                if (ph.getSectionId() == SectionId.Special) {
                    continue;
                }
                Level level = coverageLevel2.getLevel(path);

                // String value = file.getSourceLocaleID(path, status);
                MissingStatus missingStatus = VettingViewer.getMissingStatus(file, path, status, latin);
                switch (missingStatus) {
                case ABSENT:
                    missingCounter.add(level, 1);
                    if (capture && level.compareTo(Level.MODERN) <= 0) {
                        missingHeaders.put(missingStatus, pathHeaderFactory.fromPath(path));
                    }
                    break;
                case ALIASED:
                case PRESENT:
                    foundCounter.add(level, 1);
                    break;
                case MISSING_OK:
                case ROOT_OK:
                    break;
                default:
                    throw new IllegalArgumentException();
                }
            }
            System.out.print(
                script
                    + "\t" + testInfo.getEnglish().getName(language)
                    + "\t" + file.getName(language)
                    + "\t" + language
                    + "\t" + locale);
            int sumFound = 0;
            int sumMissing = 0;
            for (Level level : Level.values()) {
                sumFound += foundCounter.get(level);
                sumMissing += missingCounter.get(level);
                if (skipPrintingLevels.contains(level)) {
                    continue;
                }
                System.out.print("\t" + sumFound + "\t" + sumMissing);
            }
            System.out.println();
        }

        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("en");

        for (Entry<MissingStatus, Set<PathHeader>> entity : missingHeaders.keyValuesSet()) {
            for (PathHeader s : entity.getValue()) {
                System.out.println(entity.getKey() + "\t" + coverageLevel2.getLevel(s.getOriginalPath()) + "\t" + s
                    + "\t\t" + s.getOriginalPath());
            }
        }
    }
}
