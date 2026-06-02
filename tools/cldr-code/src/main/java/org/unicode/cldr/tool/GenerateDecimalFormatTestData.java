package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateDecimalFormatTestData {

    /**
     * This class holds all the dimensions that we are testing against for the numberformatter
     *
     * <p>For example, <b>locale identifiers</b> are one dimension for decimal formatting, currency
     * formatting and unit formatting. Test generators for those areas should apply locales (and any
     * other relevant dimensions) as inputs.
     *
     * <h2>Example dimensions</h2>
     *
     * <p>Examples include:
     *
     * <ul>
     *   <li>Locale identifiers
     *   <li>Ranges of numbers
     *   <li>Ranges of dates
     *   <li>Ranges of times
     *   <li>Ranges of durations
     *   <li>Currencies
     *   <li>Units
     *   <li>Others as needed
     * </ul>
     *
     * <p><b>Note:</b> Each CLDR number formatting area or spec only needs a <b>subset</b> of these
     * dimensions. For instance, decimal formatting does not need a currency dimension; currency
     * formatting does.
     */
    public static final class Dimensions {
        private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

        /**
         * Locales used for focused tests: major languages, several scripts, RTL locales, and
         * regional shapes.
         */
        private static final ImmutableSet<String> CORE_LOCALES =
                ImmutableSet.of(
                        // Arabic (Base): Represents RTL locales that default to Latin digits (latn)
                        // in CLDR, providing a contrast with ar_EG.
                        "ar",
                        // Arabic (Egypt): Represents RTL locales with default non-latin numbering
                        // system (Eastern Arabic digits).
                        "ar_EG",
                        // Bengali (Bangladesh): Represents LTR locales with default non-latin
                        // numbering system (Bengali digits) and Indian grouping system
                        // (lakh/crore).
                        "bn",
                        // German (Germany): Represents standard European formatting using comma for
                        // decimal and dot for thousands.
                        "de",
                        // German (Switzerland): High-signal for custom separators, specifically
                        // using apostrophe (') for thousands.
                        "de_CH",
                        // English (US): Baseline LTR locale, representing standard dot for decimal
                        // and comma for thousands, and standard English compact formats.
                        "en_US",
                        // Japanese (Japan): Represents CJK locales using the myriad grouping system
                        // (4-digit) for compact formats.
                        "ja",
                        // Portuguese (Portugal): Represents European Portuguese compact formats and
                        // space separator (differing from pt_BR).
                        "pt_PT",
                        // Russian (Russia): Represents Cyrillic script, space separator, and
                        // complex Slavic plural rules affecting compact formats.
                        "ru");

        // First dimension: locales

        /**
         * Returns all available language locales from CLDR. This is the most comprehensive list of
         * locales to test against.
         *
         * @return a set of locale identifiers
         */
        public static Set<String> getAllLocales() {
            return CLDR_FACTORY.getAvailableLanguages();
        }

        /**
         * Returns a core subset of locales for quick, high-signal testing.
         *
         * <p>This subset mixes Latin, RTL ({@code ar_EG}), South Asian ({@code hi}), CJK ({@code
         * zh_Hans_CN}, {@code zh_Hant_TW}), Cyrillic ({@code ru}), and a non-default grouping style
         * ({@code de_CH}).
         *
         * @return an immutable set of core locale identifiers
         */
        public static ImmutableSet<String> getCoreLocales() {
            return CORE_LOCALES;
        }

        /**
         * Returns the set of extended modern locales (modern locales minus core locales).
         *
         * @return a sorted set of extended modern locale identifiers
         */
        public static Set<String> getExtendedModernLocales() {
            Set<String> modernLocales =
                    StandardCodes.make()
                            .getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
            Set<String> extendedModernLocales = new TreeSet<>(modernLocales);
            extendedModernLocales.removeAll(getCoreLocales());
            return extendedModernLocales;
        }

        // Fourth dimension: currencies
        /**
         * Currencies used for focused tests: standard, no-decimal, custom rounding, alternative
         * grouping, and RTL/non-latin script representative.
         */
        private static final ImmutableSet<String> CORE_CURRENCIES =
                ImmutableSet.of(
                        // US Dollar: Baseline currency, LTR, standard decimal formatting (2
                        // decimals).
                        "USD",
                        // Euro: High-signal for locale-specific formatting variations (symbol
                        // position,
                        // decimal separators vary across eurozone locales).
                        "EUR",
                        // Japanese Yen: High-signal for currencies with no minor units (0
                        // decimals).
                        "JPY",
                        // Swiss Franc: High-signal for custom rounding (rounding to nearest 0.05 is
                        // common).
                        "CHF",
                        // Indian Rupee: High-signal for digit grouping using the lakh/crore system
                        // (e.g., 1,00,000.00).
                        "INR",
                        // Egyptian Pound: High-signal for RTL locales and Arabic-Indic digit
                        // formatting.
                        "EGP");

        public static ImmutableSet<String> getCoreCurrencies() {
            return CORE_CURRENCIES;
        }

        public static Set<String> getExtendedModernCurrencies() {
            Set<String> modernCurrencies = getModernCurrencies();
            Set<String> extendedModernCurrencies = new TreeSet<>(modernCurrencies);
            extendedModernCurrencies.removeAll(getCoreCurrencies());
            return extendedModernCurrencies;
        }

        private static Set<String> getModernCurrencies() {
            SupplementalDataInfo sdi = CLDR_CONFIG.getSupplementalDataInfo();
            Set<String> modernCurrencies = new TreeSet<>();
            Date now = new Date();
            for (String territory : sdi.getCurrencyTerritories()) {
                for (CurrencyDateInfo cdi : sdi.getCurrencyDateInfo(territory)) {
                    if (cdi.getStart().before(now)
                            && cdi.getEnd().after(now)
                            && cdi.isLegalTender()) {
                        modernCurrencies.add(cdi.getCurrency());
                    }
                }
            }
            return modernCurrencies;
        }

        // Second dimension: format type
        /**
         * Core number format type dimension (standard decimal, percent, or scientific notation).
         *
         * @see <a href="https://www.unicode.org/reports/tr35/tr35-numbers.html#Number_Formats">LDML
         *     Number Formats</a>
         */
        public enum NumberFormat {
            DECIMAL("decimal"),
            PERCENT("percent"),
            SCIENTIFIC("scientific"),
            CURRENCY("currency"),
            COMPACT_CURRENCY("compact-currency");

            private final String label;

            NumberFormat(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        // Third dimension: length
        /**
         * Format length dimension (short vs long patterns, or empty for standard).
         *
         * @see <a
         *     href="https://www.unicode.org/reports/tr35/tr35-numbers.html#Compact_Number_Formats">LDML
         *     Compact Number Formats</a>
         */
        public enum FormatLength {
            EMPTY(""),
            SHORT("short"),
            LONG("long");

            private final String label;

            FormatLength(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        public enum CurrencyWidth {
            SHORT("short"),
            LONG("long"),
            ISO_CODE("iso-code");

            private final String label;

            CurrencyWidth(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        // TODO(younies): Revisit these dimension values to ensure comprehensive test coverage.
        // Fifth dimension: numbers
        /** Core subset of test numbers for focused, high-signal testing. */
        public static final ImmutableSet<Double> CORE_NUMBERS =
                ImmutableSet.of(0.0, 1.2, 0.00831765, 1234565.0, -1230.05);

        // TODO(younies): Revisit these dimension values to ensure comprehensive test coverage.
        /**
         * Returns the complete list of test numbers (powers of 10, their multiples, minimal
         * numbers, etc.).
         *
         * @return a sorted set of complete test numbers
         */
        public static Set<Double> getCompleteNumbers() {
            Set<Double> results = new TreeSet<>();
            // Add powers of 10
            for (int i = -6; i <= 12; i++) {
                results.add(Math.pow(10, i));
                results.add(Math.pow(10, i) * 1.5);
                results.add(Math.pow(10, i) * 5);
            }
            // Add some standard test numbers
            results.addAll(CORE_NUMBERS);
            results.add(12.0);
            results.add(123.0);
            results.add(1234.56);
            results.add(1234567.0);
            results.add(0.000123);
            results.add(-0.0);
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

        private Dimensions() {
            // Prevent instantiation
        }
    }

    private static final String OUTPUT_SUBDIR = "decimal";

    static class Combination {
        String locale;
        Dimensions.NumberFormat format;
        Dimensions.FormatLength length;
        Dimensions.CurrencyWidth currencyWidth;
        Double number;
        String currency;

        Combination(
                String locale,
                Dimensions.NumberFormat format,
                Dimensions.FormatLength length,
                Double number) {
            this(locale, format, length, null, number, null);
        }

        Combination(
                String locale,
                Dimensions.NumberFormat format,
                Dimensions.FormatLength length,
                Double number,
                String currency) {
            this(locale, format, length, null, number, currency);
        }

        Combination(
                String locale,
                Dimensions.NumberFormat format,
                Dimensions.FormatLength length,
                Dimensions.CurrencyWidth currencyWidth,
                Double number,
                String currency) {
            this.locale = locale;
            this.format = format;
            this.length = length;
            this.currencyWidth = currencyWidth;
            this.number = number;
            this.currency = currency;
        }
    }

    public static String format(
            ULocale locale,
            Dimensions.NumberFormat format,
            Dimensions.FormatLength length,
            double number) {
        return format(locale, format, length, null, number);
    }

    public static String format(
            ULocale locale,
            Dimensions.NumberFormat format,
            Dimensions.FormatLength length,
            String currencyCode,
            double number) {
        return format(locale, format, length, null, currencyCode, number);
    }

    public static String format(
            ULocale locale,
            Dimensions.NumberFormat format,
            Dimensions.FormatLength length,
            Dimensions.CurrencyWidth currencyWidth,
            String currencyCode,
            double number) {
        // TODO(younies): Check this logic against the TR35 spec to ensure we are applying the
        // correct dimension checks.
        com.ibm.icu.util.Currency currency = null;
        if (format == Dimensions.NumberFormat.CURRENCY
                || format == Dimensions.NumberFormat.COMPACT_CURRENCY) {
            if (currencyCode == null || currencyCode.isEmpty()) {
                throw new IllegalArgumentException(
                        "Currency code is required for currency formats");
            }
            currency = com.ibm.icu.util.Currency.getInstance(currencyCode);
        }

        com.ibm.icu.number.LocalizedNumberFormatter lnf;
        switch (format) {
            case DECIMAL:
                switch (length) {
                    case SHORT:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .notation(com.ibm.icu.number.Notation.compactShort());
                        break;
                    case LONG:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .notation(com.ibm.icu.number.Notation.compactLong());
                        break;
                    case EMPTY:
                        lnf = NumberFormatter.withLocale(locale);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown length: " + length);
                }
                break;
            case PERCENT:
                switch (length) {
                    case EMPTY:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .unit(com.ibm.icu.util.MeasureUnit.PERCENT)
                                        .scale(com.ibm.icu.number.Scale.powerOfTen(2));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "PERCENT only supports EMPTY length, got: " + length);
                }
                break;
            case SCIENTIFIC:
                switch (length) {
                    case EMPTY:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .notation(com.ibm.icu.number.Notation.scientific());
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "SCIENTIFIC only supports EMPTY length, got: " + length);
                }
                break;
            case CURRENCY:
                switch (length) {
                    case EMPTY:
                        lnf = NumberFormatter.withLocale(locale).unit(currency);
                        break;
                    case SHORT:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .unit(currency)
                                        .notation(com.ibm.icu.number.Notation.compactShort());
                        break;
                    case LONG:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .unit(currency)
                                        .notation(com.ibm.icu.number.Notation.compactLong());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown length: " + length);
                }
                break;
            case COMPACT_CURRENCY:
                switch (length) {
                    case SHORT:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .unit(currency)
                                        .notation(com.ibm.icu.number.Notation.compactShort());
                        break;
                    case LONG:
                        lnf =
                                NumberFormatter.withLocale(locale)
                                        .unit(currency)
                                        .notation(com.ibm.icu.number.Notation.compactLong());
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "COMPACT_CURRENCY only supports SHORT or LONG length, got: "
                                        + length);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }

        if (format == Dimensions.NumberFormat.CURRENCY
                || format == Dimensions.NumberFormat.COMPACT_CURRENCY) {
            com.ibm.icu.number.NumberFormatter.UnitWidth icuUnitWidth = null;
            if (currencyWidth != null) {
                switch (currencyWidth) {
                    case SHORT:
                        icuUnitWidth = com.ibm.icu.number.NumberFormatter.UnitWidth.SHORT;
                        break;
                    case LONG:
                        icuUnitWidth = com.ibm.icu.number.NumberFormatter.UnitWidth.FULL_NAME;
                        break;
                    case ISO_CODE:
                        icuUnitWidth = com.ibm.icu.number.NumberFormatter.UnitWidth.ISO_CODE;
                        break;
                }
            }
            if (icuUnitWidth != null) {
                lnf = lnf.unitWidth(icuUnitWidth);
            }
        }

        return lnf.format(number).toString();
    }

    static class TestCase {
        final String locale;
        final String number_format;
        final String format_length;
        final String currency_width;
        final String currency;
        final double input;
        final String expected;

        TestCase(
                String locale,
                String numberFormat,
                String formatLength,
                double input,
                String expected) {
            this(locale, numberFormat, formatLength, null, null, input, expected);
        }

        TestCase(
                String locale,
                String numberFormat,
                String formatLength,
                String currency,
                double input,
                String expected) {
            this(locale, numberFormat, formatLength, null, currency, input, expected);
        }

        TestCase(
                String locale,
                String numberFormat,
                String formatLength,
                String currencyWidth,
                String currency,
                double input,
                String expected) {
            this.locale = locale;
            this.number_format = numberFormat;
            this.format_length = formatLength;
            this.currency_width = currencyWidth;
            this.currency = currency;
            this.input = input;
            this.expected = expected;
        }
    }

    private static List<TestCase> generateTestCases(
            // TODO(younies): Recheck how we apply dimensions here to ensure we generate the correct
            // combinations of test cases.
            Iterable<String> locales,
            Iterable<NumberFormatWithLength> styles,
            Iterable<Double> numbers,
            Predicate<Combination> filter) {
        List<TestCase> results = new ArrayList<>();
        for (String localeStr : locales) {
            ULocale locale = new ULocale(localeStr);
            for (NumberFormatWithLength style : styles) {
                for (Double number : numbers) {
                    Combination combo =
                            new Combination(localeStr, style.format, style.length, number);
                    if (!filter.test(combo)) {
                        continue;
                    }
                    String expected = format(locale, style.format, style.length, number);
                    String numberFormat = style.format.getLabel();
                    String formatLength = style.length.getLabel();
                    results.add(
                            new TestCase(localeStr, numberFormat, formatLength, number, expected));
                }
            }
        }
        return results;
    }

    private static void writeTsv(List<TestCase> testCases, String filename) throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println("locale\tnumber_format\tformat_length\tinput\texpected");
            for (TestCase tc : testCases) {
                pw.println(
                        tc.locale
                                + "\t"
                                + tc.number_format
                                + "\t"
                                + tc.format_length
                                + "\t"
                                + tc.input
                                + "\t"
                                + tc.expected);
            }
        }
    }

    private static List<TestCase> generateCurrencyTestCases(
            Iterable<String> locales,
            Iterable<NumberFormatWithLength> styles,
            Iterable<String> currencies,
            Iterable<Double> numbers,
            Predicate<Combination> filter) {
        List<TestCase> results = new ArrayList<>();
        for (String localeStr : locales) {
            ULocale locale = new ULocale(localeStr);
            for (NumberFormatWithLength style : styles) {
                for (String currency : currencies) {
                    for (Double number : numbers) {
                        Combination combo =
                                new Combination(
                                        localeStr, style.format, style.length, number, currency);
                        if (!filter.test(combo)) {
                            continue;
                        }
                        String expected =
                                format(locale, style.format, style.length, currency, number);
                        String numberFormat = style.format.getLabel();
                        String formatLength = style.length.getLabel();
                        results.add(
                                new TestCase(
                                        localeStr,
                                        numberFormat,
                                        formatLength,
                                        currency,
                                        number,
                                        expected));
                    }
                }
            }
        }
        return results;
    }

    private static void writeCurrencyTsv(List<TestCase> testCases, String filename)
            throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println("locale\tnumber_format\tformat_length\tcurrency\tinput\texpected");
            for (TestCase tc : testCases) {
                pw.println(
                        tc.locale
                                + "\t"
                                + tc.number_format
                                + "\t"
                                + tc.format_length
                                + "\t"
                                + (tc.currency == null ? "" : tc.currency)
                                + "\t"
                                + tc.input
                                + "\t"
                                + tc.expected);
            }
        }
    }

    private static List<TestCase> generateCompactCurrencyTestCases(
            Iterable<String> locales,
            Iterable<Dimensions.FormatLength> compactLengths,
            Iterable<Dimensions.CurrencyWidth> currencyWidths,
            Iterable<String> currencies,
            Iterable<Double> numbers,
            Predicate<Combination> filter) {
        List<TestCase> results = new ArrayList<>();
        for (String localeStr : locales) {
            ULocale locale = new ULocale(localeStr);
            for (Dimensions.FormatLength compactLength : compactLengths) {
                for (Dimensions.CurrencyWidth currencyWidth : currencyWidths) {
                    for (String currency : currencies) {
                        for (Double number : numbers) {
                            Combination combo =
                                    new Combination(
                                            localeStr,
                                            Dimensions.NumberFormat.COMPACT_CURRENCY,
                                            compactLength,
                                            currencyWidth,
                                            number,
                                            currency);
                            if (!filter.test(combo)) {
                                continue;
                            }
                            String expected =
                                    format(
                                            locale,
                                            Dimensions.NumberFormat.COMPACT_CURRENCY,
                                            compactLength,
                                            currencyWidth,
                                            currency,
                                            number);
                            results.add(
                                    new TestCase(
                                            localeStr,
                                            Dimensions.NumberFormat.COMPACT_CURRENCY.getLabel(),
                                            compactLength.getLabel(),
                                            currencyWidth.getLabel(),
                                            currency,
                                            number,
                                            expected));
                        }
                    }
                }
            }
        }
        return results;
    }

    private static void writeCompactCurrencyTsv(List<TestCase> testCases, String filename)
            throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println(
                    "locale\tnumber_format\tcompact_length\tcurrency_length\tcurrency\tinput\texpected");
            for (TestCase tc : testCases) {
                pw.println(
                        tc.locale
                                + "\t"
                                + tc.number_format
                                + "\t"
                                + tc.format_length
                                + "\t"
                                + (tc.currency_width == null ? "" : tc.currency_width)
                                + "\t"
                                + (tc.currency == null ? "" : tc.currency)
                                + "\t"
                                + tc.input
                                + "\t"
                                + tc.expected);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Set<String> coreLocales = Dimensions.getCoreLocales();
        Set<String> extendedModernLocales = Dimensions.getExtendedModernLocales();

        Set<Double> coreNumbers = Dimensions.CORE_NUMBERS;
        Set<Double> extendedNumbers = new TreeSet<>(Dimensions.getCompleteNumbers());
        extendedNumbers.removeAll(coreNumbers);

        Set<NumberFormatWithLength> allStyles =
                ImmutableSet.of(
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.DECIMAL, Dimensions.FormatLength.EMPTY),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.PERCENT, Dimensions.FormatLength.EMPTY),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.SCIENTIFIC, Dimensions.FormatLength.EMPTY),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.DECIMAL, Dimensions.FormatLength.SHORT),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.DECIMAL, Dimensions.FormatLength.LONG));

        // 1. Core Locales, Core Numbers -> decimals.tsv
        List<TestCase> coreCases =
                generateTestCases(coreLocales, allStyles, coreNumbers, combo -> true);
        writeTsv(coreCases, "decimals.tsv");

        // 2. Extended Modern Locales, Core Numbers -> decimals_modern_locales.tsv
        List<TestCase> extendedCases =
                generateTestCases(extendedModernLocales, allStyles, coreNumbers, combo -> true);
        writeTsv(extendedCases, "decimals_modern_locales.tsv");

        // 3. Core Locales, Extended Numbers -> decimals_all_numbers.tsv
        List<TestCase> extendedNumbersCases =
                generateTestCases(coreLocales, allStyles, extendedNumbers, combo -> true);
        writeTsv(extendedNumbersCases, "decimals_all_numbers.tsv");

        System.out.println("Decimal format test data generation completed.");

        // Currency tests (Original - Untouched)
        Set<String> coreCurrencies = Dimensions.getCoreCurrencies();
        Set<String> extendedModernCurrencies = Dimensions.getExtendedModernCurrencies();

        Set<NumberFormatWithLength> currencyStyles =
                ImmutableSet.of(
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.CURRENCY, Dimensions.FormatLength.EMPTY),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.CURRENCY, Dimensions.FormatLength.SHORT),
                        new NumberFormatWithLength(
                                Dimensions.NumberFormat.CURRENCY, Dimensions.FormatLength.LONG));

        // 1. Core Locales, Core Currencies, Core Numbers -> currencies.tsv
        List<TestCase> currencyCoreCases =
                generateCurrencyTestCases(
                        coreLocales, currencyStyles, coreCurrencies, coreNumbers, combo -> true);
        writeCurrencyTsv(currencyCoreCases, "currencies.tsv");

        // 2. Extended Modern Locales, Core Currencies, Core Numbers ->
        // currencies_modern_locales.tsv
        List<TestCase> currencyModernLocalesCases =
                generateCurrencyTestCases(
                        extendedModernLocales,
                        currencyStyles,
                        coreCurrencies,
                        coreNumbers,
                        combo -> true);
        writeCurrencyTsv(currencyModernLocalesCases, "currencies_modern_locales.tsv");

        // 3. Core Locales, Extended Modern Currencies, Core Numbers ->
        // currencies_modern_currencies.tsv
        List<TestCase> currencyModernCurrenciesCases =
                generateCurrencyTestCases(
                        coreLocales,
                        currencyStyles,
                        extendedModernCurrencies,
                        coreNumbers,
                        combo -> true);
        writeCurrencyTsv(currencyModernCurrenciesCases, "currencies_modern_currencies.tsv");

        // 4. Core Locales, Core Currencies, Extended Numbers -> split by style
        // 4a. Standard Currency -> currencies_all_numbers.tsv
        List<TestCase> currencyAllNumbersCases =
                generateCurrencyTestCases(
                        coreLocales,
                        ImmutableSet.of(
                                new NumberFormatWithLength(
                                        Dimensions.NumberFormat.CURRENCY,
                                        Dimensions.FormatLength.EMPTY)),
                        coreCurrencies,
                        extendedNumbers,
                        combo -> true);
        writeCurrencyTsv(currencyAllNumbersCases, "currencies_all_numbers.tsv");

        // 4b. Short Currency -> currencies_all_numbers_short.tsv
        List<TestCase> currencyAllNumbersShortCases =
                generateCurrencyTestCases(
                        coreLocales,
                        ImmutableSet.of(
                                new NumberFormatWithLength(
                                        Dimensions.NumberFormat.CURRENCY,
                                        Dimensions.FormatLength.SHORT)),
                        coreCurrencies,
                        extendedNumbers,
                        combo -> true);
        writeCurrencyTsv(currencyAllNumbersShortCases, "currencies_all_numbers_short.tsv");

        // 4c. Long Currency -> currencies_all_numbers_long.tsv
        List<TestCase> currencyAllNumbersLongCases =
                generateCurrencyTestCases(
                        coreLocales,
                        ImmutableSet.of(
                                new NumberFormatWithLength(
                                        Dimensions.NumberFormat.CURRENCY,
                                        Dimensions.FormatLength.LONG)),
                        coreCurrencies,
                        extendedNumbers,
                        combo -> true);
        writeCurrencyTsv(currencyAllNumbersLongCases, "currencies_all_numbers_long.tsv");

        System.out.println("Original Currency format test data generation completed.");

        // Compact Currency tests (Add-on)
        Set<Dimensions.FormatLength> compactLengths =
                ImmutableSet.of(Dimensions.FormatLength.SHORT, Dimensions.FormatLength.LONG);
        Set<Dimensions.CurrencyWidth> currencyWidths =
                ImmutableSet.of(
                        Dimensions.CurrencyWidth.SHORT,
                        Dimensions.CurrencyWidth.LONG,
                        Dimensions.CurrencyWidth.ISO_CODE);

        // 1. Core Locales, Core Currencies, Core Numbers -> compact_currencies.tsv
        // Contains all combinations (2 compact lengths * 3 currency widths)
        List<TestCase> compactCurrencyCoreCases =
                generateCompactCurrencyTestCases(
                        coreLocales,
                        compactLengths,
                        currencyWidths,
                        coreCurrencies,
                        coreNumbers,
                        combo -> true);
        writeCompactCurrencyTsv(compactCurrencyCoreCases, "compact_currencies.tsv");

        // 2. Extended Modern Locales, Core Currencies, Core Numbers -> split by compact length
        // 2a/2b. compact_currencies_modern_locales_short/long.tsv
        for (Dimensions.FormatLength compactLength : compactLengths) {
            List<TestCase> cases =
                    generateCompactCurrencyTestCases(
                            extendedModernLocales,
                            ImmutableSet.of(compactLength),
                            currencyWidths,
                            coreCurrencies,
                            coreNumbers,
                            combo -> true);
            writeCompactCurrencyTsv(
                    cases,
                    "compact_currencies_modern_locales_" + compactLength.getLabel() + ".tsv");
        }

        // 3. Core Locales, Extended Modern Currencies, Core Numbers -> split by compact length and currency width
        // Generates 6 files: compact_currencies_modern_currencies_[compact]_[currency].tsv
        for (Dimensions.FormatLength compactLength : compactLengths) {
            for (Dimensions.CurrencyWidth currencyWidth : currencyWidths) {
                List<TestCase> cases =
                        generateCompactCurrencyTestCases(
                                coreLocales,
                                ImmutableSet.of(compactLength),
                                ImmutableSet.of(currencyWidth),
                                extendedModernCurrencies,
                                coreNumbers,
                                combo -> true);
                writeCompactCurrencyTsv(
                        cases,
                        "compact_currencies_modern_currencies_"
                                + compactLength.getLabel()
                                + "_"
                                + currencyWidth.getLabel()
                                + ".tsv");
            }
        }

        // 4. Core Locales, Core Currencies, Extended Numbers -> split by compact length and currency width
        // Generates 6 files: compact_currencies_all_numbers_[compact]_[currency].tsv
        for (Dimensions.FormatLength compactLength : compactLengths) {
            for (Dimensions.CurrencyWidth currencyWidth : currencyWidths) {
                List<TestCase> cases =
                        generateCompactCurrencyTestCases(
                                coreLocales,
                                ImmutableSet.of(compactLength),
                                ImmutableSet.of(currencyWidth),
                                coreCurrencies,
                                extendedNumbers,
                                combo -> true);
                writeCompactCurrencyTsv(
                        cases,
                        "compact_currencies_all_numbers_"
                                + compactLength.getLabel()
                                + "_"
                                + currencyWidth.getLabel()
                                + ".tsv");
            }
        }

        System.out.println("Compact Currency format test data generation completed.");
    }

    static class NumberFormatWithLength {
        final Dimensions.NumberFormat format;
        final Dimensions.FormatLength length;

        NumberFormatWithLength(Dimensions.NumberFormat format, Dimensions.FormatLength length) {
            this.format = format;
            this.length = length;
        }
    }
}
