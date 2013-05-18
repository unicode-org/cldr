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
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.Transform;

public class ShowLocaleCoverage {
    public static TestInfo testInfo = TestInfo.getInstance();
    // added info using pattern in VettingViewer.

    final static Options myOptions = new Options();

    private static final String TEST_PATH = "//ldml/dates/calendars/calendar[@type=\"chinese\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";

    enum MyOptions {
        filter(".+", ".*", "Filter the information based on id, using a regex argument."),
        draftStatus(".+", "unconfirmed", "Filter the information to a minimum draft status."),
        organization(".+", null, "Only locales for organization");

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
            Level.OPTIONAL,
            Level.COMPREHENSIVE
            );

    public static void main(String[] args) {
        myOptions.parse(MyOptions.filter, args, true);
        Matcher matcher = Pattern.compile(MyOptions.filter.option.getValue()).matcher("");
        DraftStatus minimumDraftStatus = DraftStatus.forString(MyOptions.draftStatus.option.getValue());
        Set<String> locales = null;
        String organization = MyOptions.organization.option.getValue();
        boolean useOrgLevel = MyOptions.organization.option.doesOccur();
        if (useOrgLevel) {
            locales = testInfo.getStandardCodes().getLocaleCoverageLocales(organization);
        }

        Relation<MissingStatus, String> missingPaths = Relation.of(new EnumMap<MissingStatus, Set<String>>(
                MissingStatus.class), TreeSet.class);

        LanguageTagParser ltp = new LanguageTagParser();
        Map<String, String> likely = testInfo.getSupplementalDataInfo().getLikelySubtags();
        Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();

        // Map<String,Counter<Level>> counts = new HashMap();
        System.out.print("Script\tEnglish\tNative\tCode\tCode*");
        for (Level level : Level.values()) {
            if (skipPrintingLevels.contains(level)) {
                continue;
            }
            System.out.print("\t≤" + level + " (f)\t(u)\t(m)");
        }
        System.out.println();
        Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getCldrFactory().make("en", true));


        Counter<Level> foundCounter = new Counter<Level>();
        Counter<Level> unconfirmedCounter = new Counter<Level>();
        Counter<Level> missingCounter = new Counter<Level>();

        int localeCount = 0;
        long start = System.currentTimeMillis();
        for (String locale : testInfo.getCldrFactory().getAvailableLanguages()) {
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

            final CLDRFile file = testInfo.getCldrFactory().make(locale, true, minimumDraftStatus);

            VettingViewer.getStatus(file, PathHeader.getFactory(ShowLocaleCoverage.testInfo.getEnglish()), foundCounter, unconfirmedCounter, missingCounter, capture ? missingPaths : null);

            System.out.print(
                    script
                    + "\t" + testInfo.getEnglish().getName(language)
                    + "\t" + file.getName(language)
                    + "\t" + language
                    + "\t" + locale);
            int sumFound = 0;
            int sumMissing = 0;
            int sumUnconfirmed = 0;
            for (Level level : Level.values()) {
                sumFound += foundCounter.get(level);
                sumUnconfirmed += unconfirmedCounter.get(level);
                sumMissing += missingCounter.get(level);
                if (useOrgLevel && testInfo.getStandardCodes().getLocaleCoverageLevel(organization, locale) != level) {
                    continue;
                } else if (skipPrintingLevels.contains(level)) {
                    continue;
                }

                System.out.print("\t" + sumFound + "\t" + sumUnconfirmed + "\t" + sumMissing);
            }
            System.out.println();
            localeCount++;
        }

        long end = System.currentTimeMillis();
        System.out.println((end - start) + " millis = " 
                + ((end - start)/localeCount) + " millis/locale");

        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("en");

        for (Entry<MissingStatus, Set<String>> entity : missingPaths.keyValuesSet()) {
            for (PathHeader s : CldrUtility.transform(entity.getValue(), pathHeaderFactory, new TreeSet<PathHeader>())) {
                System.out.println(entity.getKey() + "\t" + coverageLevel2.getLevel(s.getOriginalPath()) + "\t" + s
                        + "\t\t" + s.getOriginalPath());
            }
        }
    }
}
