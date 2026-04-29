package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
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
                    "yMd", "yMMMMd", "yMMMMEEEEd", "GyMd", "HmsS", "Cms", "yMdHmsv", "Bh", "MMMM", "jjm");

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

    static class Combination {
        String locale;
        String calendar;
        String skeleton;

        Combination(String locale, String calendar, String skeleton) {
            this.locale = locale;
            this.calendar = calendar;
            this.skeleton = skeleton;
        }
    }

    private static void generateAndWrite(
            Iterable<String> locales,
            Iterable<String> calendars,
            Iterable<String> skeletons,
            String filename,
            Predicate<Combination> filter)
            throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println("locale\tcalendar\tskeleton\tpattern");

            for (String locale : locales) {
                CLDRFile cldrFile = null;
                try {
                    cldrFile = CLDR_FACTORY.make(locale, true, DraftStatus.contributed);
                } catch (Exception e) {
                    continue;
                }
                for (String calendar : calendars) {
                    CldrDateTimePatternGenerator generator = null;
                    try {
                        generator = new CldrDateTimePatternGenerator(cldrFile, calendar, false);
                    } catch (Exception e) {
                        continue;
                    }
                    for (String skeleton : skeletons) {
                        Combination combo = new Combination(locale, calendar, skeleton);
                        if (!filter.test(combo)) {
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

        // Filter to skip redundant entries covered in skeletons.tsv
        Predicate<Combination> skipRedundant =
                combo ->
                        !(MINIMAL_LOCALES.contains(combo.locale)
                                && MINIMAL_CALENDARS.contains(combo.calendar)
                                && MINIMAL_SKELETONS.contains(combo.skeleton));

        // Filter for 5% random subset of missing tests
        Predicate<Combination> filterRandom5Percent =
                combo -> {
                    // Skip if at most one axis is non-minimal
                    int nonMinimalCount = 0;
                    if (!MINIMAL_LOCALES.contains(combo.locale)) nonMinimalCount++;
                    if (!MINIMAL_CALENDARS.contains(combo.calendar)) nonMinimalCount++;
                    if (!MINIMAL_SKELETONS.contains(combo.skeleton)) nonMinimalCount++;

                    if (nonMinimalCount <= 1) {
                        return false;
                    }

                    // Compute a stable hash and apply 5% rule
                    String key = combo.locale + "\t" + combo.calendar + "\t" + combo.skeleton;
                    int hash = key.hashCode();
                    return Math.abs(hash % 20) == 0;
                };

        // 0. Minimal product (baseline)
        generateAndWrite(
                MINIMAL_LOCALES,
                MINIMAL_CALENDARS,
                MINIMAL_SKELETONS,
                "skeletons.tsv",
                combo -> true); // Include all

        // 1. Complete Locales, Minimal Calendars, Minimal Skeletons
        generateAndWrite(
                completeLocales,
                MINIMAL_CALENDARS,
                MINIMAL_SKELETONS,
                "skeletons_all_locales.tsv",
                skipRedundant);

        // 2. Complete Calendars, Minimal Locales, Minimal Skeletons
        generateAndWrite(
                MINIMAL_LOCALES,
                completeCalendars,
                MINIMAL_SKELETONS,
                "skeletons_all_calendars.tsv",
                skipRedundant);

        // 3. Complete Skeletons, Minimal Locales, Minimal Calendars
        generateAndWrite(
                MINIMAL_LOCALES,
                MINIMAL_CALENDARS,
                completeSkeletons,
                "skeletons_all_skeletons.tsv",
                skipRedundant);

        // 4. 5% random subset of missing tests
        generateAndWrite(
                completeLocales,
                completeCalendars,
                completeSkeletons,
                "skeletons_random_5percent.tsv",
                filterRandom5Percent);
    }
}
