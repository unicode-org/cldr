package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.CldrDateTimePatternGenerator;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateDateSkeletonTestData {

    private static final String OUTPUT_SUBDIR = "datetime";

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

    // Minimal lists
    private static final ImmutableSet<String> MINIMAL_LOCALES =
            ImmutableSet.of("en_US", "zh_Hant_TW", "ko", "eu", "ja", "ru", "vi", "ar", "fr");

    private static final ImmutableSet<String> MINIMAL_SKELETONS =
            ImmutableSet.of(
                    "yMd",
                    "yMMMMd",
                    "yMMMMEEEEd",
                    "GyMd",
                    "HmsS",
                    "Cms",
                    "yMdHmsv",
                    "Bh",
                    "MMMM",
                    "jjm");

    private static final ImmutableSet<String> MINIMAL_CALENDARS =
            ImmutableSet.of("gregorian", "japanese", "buddhist", "chinese");

    // Complete lists
    private static Set<String> getCompleteLocales() {
        Set<String> results = new TreeSet<>();
        for (String locale : CLDR_FACTORY.getAvailableLanguages()) {
            Level coverageLevel =
                    CalculatedCoverageLevels.getInstance().getEffectiveCoverageLevel(locale);
            if (coverageLevel != null && coverageLevel.isAtLeast(Level.MODERN)) {
                results.add(locale);
            }
        }
        return results;
    }

    private static Set<String> getCompleteSkeletons() {
        Set<String> results = new TreeSet<>(MINIMAL_SKELETONS);
        for (String skeleton : CldrDateTimePatternGeneratorCompare.SKELETONS) {
            results.add(skeleton);
        }
        return results;
    }

    private static Set<String> getCompleteCalendars() {
        Set<String> results = new TreeSet<>();
        org.unicode.cldr.util.SupplementalDataInfo sdi = CLDR_CONFIG.getSupplementalDataInfo();
        for (String territory : sdi.getTerritoriesWithPopulationData()) {
            List<String> calendars = sdi.getCalendars(territory);
            if (calendars != null) {
                results.addAll(calendars);
            }
        }
        results.addAll(MINIMAL_CALENDARS);
        return results;
    }

    private static void generateAndWrite(
            Iterable<String> locales,
            Iterable<String> calendars,
            Iterable<String> skeletons,
            String filename,
            boolean skipRedundant)
            throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println("locale\tcalendar\tskeleton\tpattern");

            for (String locale : locales) {
                CLDRFile cldrFile = null;
                try {
                    cldrFile = CLDR_FACTORY.make(locale, true, DraftStatus.contributed);
                } catch (Exception e) {
                    // Some locales might not have contributed data or fail to load
                    continue;
                }
                for (String calendar : calendars) {
                    CldrDateTimePatternGenerator generator = null;
                    try {
                        generator = new CldrDateTimePatternGenerator(cldrFile, calendar, false);
                    } catch (Exception e) {
                        // Some calendars might not be supported for all locales
                        continue;
                    }
                    for (String skeleton : skeletons) {
                        if (skipRedundant
                                && MINIMAL_LOCALES.contains(locale)
                                && MINIMAL_CALENDARS.contains(calendar)
                                && MINIMAL_SKELETONS.contains(skeleton)) {
                            continue;
                        }
                        List<String> trace = new ArrayList<>();
                        String pattern = generator.getBestPattern(skeleton, trace);

                        pw.println(locale + "\t" + calendar + "\t" + skeleton + "\t" + pattern);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Set<String> completeLocales = getCompleteLocales();
        Set<String> completeSkeletons = getCompleteSkeletons();
        Set<String> completeCalendars = getCompleteCalendars();

        // 0. Minimal product (baseline)
        generateAndWrite(
                MINIMAL_LOCALES, MINIMAL_CALENDARS, MINIMAL_SKELETONS, "skeletons.tsv", false);

        // 1. Complete Locales, Minimal Calendars, Minimal Skeletons
        generateAndWrite(
                completeLocales,
                MINIMAL_CALENDARS,
                MINIMAL_SKELETONS,
                "skeletons_all_locales.tsv",
                true);

        // 2. Complete Calendars, Minimal Locales, Minimal Skeletons
        generateAndWrite(
                MINIMAL_LOCALES,
                completeCalendars,
                MINIMAL_SKELETONS,
                "skeletons_all_calendars.tsv",
                true);

        // 3. Complete Skeletons, Minimal Locales, Minimal Calendars
        generateAndWrite(
                MINIMAL_LOCALES,
                MINIMAL_CALENDARS,
                completeSkeletons,
                "skeletons_all_skeletons.tsv",
                true);
    }
}
