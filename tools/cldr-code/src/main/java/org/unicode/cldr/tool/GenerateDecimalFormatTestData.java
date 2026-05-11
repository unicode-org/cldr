package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateDecimalFormatTestData {

    private static final String OUTPUT_SUBDIR = "decimal";

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

    // Minimal lists
    private static final ImmutableSet<String> MINIMAL_LOCALES =
            ImmutableSet.of("en_US", "fr", "de_CH", "ar", "hi", "bn", "zh", "ru", "ja");

    private static final ImmutableSet<Double> MINIMAL_NUMBERS =
            ImmutableSet.of(
                    0.0, 1.0, 1.2, 12.0, 123.0, 1234.56, 1234567.0, -1234.56, 0.000123, -0.0);

    enum Style {
        DECIMAL("decimal"),
        PERCENT("percent"),
        SCIENTIFIC("scientific"),
        COMPACT_SHORT("compact-short"),
        COMPACT_LONG("compact-long");

        final String label;

        Style(String label) {
            this.label = label;
        }
    }

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

    private static Set<Double> getCompleteNumbers() {
        Set<Double> results = new TreeSet<>();
        // Add powers of 10
        for (int i = -6; i <= 12; i++) {
            results.add(Math.pow(10, i));
            results.add(Math.pow(10, i) * 1.5);
            results.add(Math.pow(10, i) * 5);
        }
        // Add some standard test numbers
        results.addAll(MINIMAL_NUMBERS);
        results.add(0.5);
        results.add(1.5);
        results.add(2.5);
        results.add(3.5);
        results.add(0.125);
        results.add(0.135);
        results.add(999.9);
        results.add(999999.9);

        // Add negatives
        Set<Double> negatives = new TreeSet<>();
        for (Double d : results) {
            if (d > 0) {
                negatives.add(-d);
            }
        }
        results.addAll(negatives);
        return results;
    }

    static class Combination {
        String locale;
        Style style;
        Double number;

        Combination(String locale, Style style, Double number) {
            this.locale = locale;
            this.style = style;
            this.number = number;
        }
    }

    private static String format(ULocale locale, Style style, double number) {
        com.ibm.icu.number.LocalizedNumberFormatter lnf;
        switch (style) {
            case DECIMAL:
                lnf = NumberFormatter.withLocale(locale);
                break;
            case PERCENT:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .unit(com.ibm.icu.util.MeasureUnit.PERCENT)
                                .scale(com.ibm.icu.number.Scale.powerOfTen(2));
                break;
            case SCIENTIFIC:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.scientific());
                break;
            case COMPACT_SHORT:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.compactShort());
                break;
            case COMPACT_LONG:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.compactLong());
                break;
            default:
                throw new IllegalArgumentException("Unknown style: " + style);
        }
        return lnf.format(number).toString();
    }

    private static void generateAndWrite(
            Iterable<String> locales,
            Iterable<Style> styles,
            Iterable<Double> numbers,
            String filename,
            Predicate<Combination> filter)
            throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println("locale\tstyle\tinput\texpected");

            for (String localeStr : locales) {
                ULocale locale = new ULocale(localeStr);
                for (Style style : styles) {
                    for (Double number : numbers) {
                        Combination combo = new Combination(localeStr, style, number);
                        if (!filter.test(combo)) {
                            continue;
                        }

                        String expected = format(locale, style, number);
                        pw.println(
                                localeStr + "\t" + style.label + "\t" + number + "\t" + expected);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Set<String> completeLocales = getCompleteLocales();
        Set<Double> completeNumbers = getCompleteNumbers();
        Set<Style> allStyles = ImmutableSet.copyOf(Style.values());

        // Filter to skip redundant entries covered in decimal.tsv
        Predicate<Combination> skipRedundant =
                combo ->
                        !(MINIMAL_LOCALES.contains(combo.locale)
                                && MINIMAL_NUMBERS.contains(combo.number));

        // Filter for 5% random subset of missing tests
        Predicate<Combination> filterRandom5Percent =
                combo -> {
                    // Skip if at most one axis is non-minimal
                    int nonMinimalCount = 0;
                    if (!MINIMAL_LOCALES.contains(combo.locale)) nonMinimalCount++;
                    if (!MINIMAL_NUMBERS.contains(combo.number)) nonMinimalCount++;
                    // Style is always "minimal" since we use all styles in baseline

                    if (nonMinimalCount <= 1) {
                        return false;
                    }

                    // Compute a stable hash and apply 5% rule
                    String key = combo.locale + "\t" + combo.style.label + "\t" + combo.number;
                    int hash = key.hashCode();
                    return Math.abs(hash % 20) == 0;
                };

        // 0. Minimal product (baseline)
        generateAndWrite(
                MINIMAL_LOCALES,
                allStyles,
                MINIMAL_NUMBERS,
                "decimal.tsv",
                combo -> true); // Include all

        // 1. Complete Locales, Minimal Numbers
        generateAndWrite(
                completeLocales,
                allStyles,
                MINIMAL_NUMBERS,
                "decimal_all_locales.tsv",
                skipRedundant);

        // 2. Complete Numbers, Minimal Locales
        generateAndWrite(
                MINIMAL_LOCALES,
                allStyles,
                completeNumbers,
                "decimal_all_numbers.tsv",
                skipRedundant);

        // 3. 5% random subset of missing tests
        generateAndWrite(
                completeLocales,
                allStyles,
                completeNumbers,
                "decimal_random_5percent.tsv",
                filterRandom5Percent);

        System.out.println("Decimal format test data generation completed.");
    }
}
