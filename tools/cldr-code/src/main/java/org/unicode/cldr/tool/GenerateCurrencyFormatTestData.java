package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateCurrencyFormatTestData {

    // TODO: Remove duplicate dimension definitions (CORE_LOCALES, CORE_NUMBERS, etc.) and
    // deduplicate with GenerateDecimalFormatTestData once both PRs are merged into master.
    public static final class Dimensions {
        private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

        // TODO: Replace with GenerateDecimalFormatTestData.Dimensions.getCoreLocales() once
        // submitted.
        private static final ImmutableSet<String> CORE_LOCALES =
                ImmutableSet.of(
                        "ar", "ar_EG", "bn", "de", "de_CH", "en", "fy", "ja", "pt_PT", "ru");

        // Tiny subset of locales for pairing with extended dimensions to prevent combinatorial
        // explosion
        private static final ImmutableSet<String> TINY_LOCALES = ImmutableSet.of("en", "ar", "de");

        // Representative set of major currencies matching CORE_LOCALES where applicable
        private static final ImmutableSet<String> CORE_CURRENCIES =
                ImmutableSet.of(
                        // Baseline world currency (matches en core locale)
                        "USD",
                        // Major European currency (matches de, pt_PT core locales)
                        "EUR",
                        // Major East Asian currency, notable for zero decimal digits (matches ja
                        // core locale)
                        "JPY",
                        // Cyrillic script representation and complex plurals (matches ru core
                        // locale)
                        "RUB",
                        // Middle Eastern currency with default Eastern Arabic digits (matches ar_EG
                        // core locale)
                        "EGP",
                        // No currency (formats as decimal/accounting number)
                        "");

        // Tiny subset of currencies for pairing with extended dimensions to prevent combinatorial
        // explosion
        private static final ImmutableSet<String> TINY_CURRENCIES = ImmutableSet.of("USD", "EUR");

        public static Set<String> getAllLocales() {
            return CLDR_FACTORY.getAvailableLanguages();
        }

        public static ImmutableSet<String> getCoreLocales() {
            return CORE_LOCALES;
        }

        public static ImmutableSet<String> getTinyLocales() {
            return TINY_LOCALES;
        }

        // TODO: Replace with GenerateDecimalFormatTestData.Dimensions.getExtendedModernLocales()
        // once submitted.
        public static Set<String> getExtendedModernLocales() {
            Set<String> modernLocales =
                    StandardCodes.make()
                            .getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
            Set<String> extendedModernLocales = new TreeSet<>(modernLocales);
            extendedModernLocales.removeAll(getCoreLocales());
            return extendedModernLocales;
        }

        public static ImmutableSet<String> getCoreCurrencies() {
            return CORE_CURRENCIES;
        }

        public static ImmutableSet<String> getTinyCurrencies() {
            return TINY_CURRENCIES;
        }

        public static Set<String> getModernCurrencies() {
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

        public static Set<String> getExtendedModernCurrencies() {
            Set<String> allModern = getModernCurrencies();
            allModern.removeAll(getCoreCurrencies());
            return allModern;
        }

        /**
         * Controls the overall formatting style and length of the currency value.
         *
         * <p>Examples (using US locale and USD currency, input: -1230.05, display: SYMBOL):
         *
         * <ul>
         *   <li>STANDARD: "-$1,230.05"
         *   <li>ACCOUNTING: "($1,230.05)"
         *   <li>SHORT: "-$1.2K" (compact short format. Note: compact long is not supported for
         *       currency in CLDR)
         * </ul>
         */
        public enum CurrencyFormatLength {
            STANDARD("standard"),
            SHORT("short");

            private final String label;

            CurrencyFormatLength(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        public enum CurrencyFormatType {
            STANDARD("standard"),
            ACCOUNTING("accounting");

            private final String label;

            CurrencyFormatType(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        /**
         * Controls how the currency unit itself is displayed (represented) within the formatted
         * string.
         *
         * <p>Examples (using US locale, USD currency, input: 1230.05, style: STANDARD):
         *
         * <ul>
         *   <li>SYMBOL: "$1,230.05"
         *   <li>NARROW_SYMBOL: "$1,230.05" (or localized narrow variant)
         *   <li>ISO_CODE: "USD 1,230.05"
         *   <li>NAME: "1,230.05 US dollars"
         * </ul>
         */
        public enum CurrencyDisplay {
            SYMBOL("symbol"),
            NARROW_SYMBOL("narrowSymbol"),
            ISO_CODE("code"),
            NAME("name"),
            NO_CURRENCY("noCurrency");

            private final String label;

            CurrencyDisplay(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        // TODO: Replace with GenerateDecimalFormatTestData.Dimensions.CORE_NUMBERS once submitted.
        public static final ImmutableSet<Double> CORE_NUMBERS =
                ImmutableSet.of(0.0, 1.2, 0.00831765, 1234565.0, -1230.05);

        // Tiny subset of numbers for pairing with extended dimensions to prevent combinatorial
        // explosion
        private static final ImmutableSet<Double> TINY_NUMBERS = ImmutableSet.of(1.2, -1230.05);

        public static ImmutableSet<Double> getTinyNumbers() {
            return TINY_NUMBERS;
        }

        // TODO: Replace with GenerateDecimalFormatTestData.Dimensions.getExtendedNumbers() once
        // submitted.
        public static Set<Double> getExtendedNumbers() {
            Set<Double> results = new TreeSet<>();
            for (int i = -6; i <= 12; i++) {
                results.add(Math.pow(10, i));
                results.add(Math.pow(10, i) * 1.5);
                results.add(Math.pow(10, i) * 5);
            }
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

            Set<Double> negatives = new TreeSet<>();
            for (Double d : results) {
                if (d > 0) {
                    negatives.add(-d);
                }
            }
            results.addAll(negatives);
            return results;
        }

        /**
         * A mapping from each currency code to the list of locales that use it as their default
         * currency.
         */
        private static final Map<String, List<String>> CURRENCY_TO_LOCALES =
                buildCurrencyToLocalesMap();

        /**
         * Builds the mapping of currency to locales. It iterates over all modern and core locales,
         * resolves their default currency using supplemental data, and populates the map.
         */
        private static Map<String, List<String>> buildCurrencyToLocalesMap() {
            Map<String, List<String>> map = new HashMap<>();
            SupplementalDataInfo sdi = CLDR_CONFIG.getSupplementalDataInfo();
            Set<String> locales = new TreeSet<>();
            locales.addAll(getCoreLocales());
            locales.addAll(getExtendedModernLocales());

            for (String localeStr : locales) {
                ULocale locale = new ULocale(localeStr);
                ULocale maximized = ULocale.addLikelySubtags(locale);
                String territory = maximized.getCountry();
                if (territory == null || territory.isEmpty()) {
                    continue;
                }
                String defaultCurrency = sdi.getDefaultCurrency(territory);
                if (defaultCurrency != null && !defaultCurrency.equals("XXX")) {
                    map.computeIfAbsent(defaultCurrency, k -> new ArrayList<>()).add(localeStr);
                }
            }
            for (List<String> list : map.values()) {
                Collections.sort(list);
            }
            return map;
        }

        /**
         * Computes a stable, deterministic 64-bit hash of the input string using SHA-256. This is
         * used to ensure stable selection of extra locales/currencies and minimize git diff churn.
         */
        private static long stableHash(String input) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes =
                        md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                long result = 0;
                for (int i = 0; i < 8; i++) {
                    result = (result << 8) | (hashBytes[i] & 0xff);
                }
                return result;
            } catch (java.security.NoSuchAlgorithmException e) {
                return input.hashCode();
            }
        }

        /**
         * Selects a representative locale for a currency (the first one alphabetically among those
         * that use it as default). Returns null if the currency is already covered by a locale in
         * TINY_LOCALES.
         */
        public static String getRepresentativeLocale(String currency) {
            List<String> locales = CURRENCY_TO_LOCALES.get(currency);
            if (locales == null || locales.isEmpty()) {
                return null;
            }
            for (String loc : locales) {
                if (TINY_LOCALES.contains(loc)) {
                    return null;
                }
            }
            return locales.get(0);
        }

        /**
         * Deterministically selects an 'extra' locale to test with a currency from all available
         * locales, excluding the specified ones. Uses a consistent hashing style (minimizing hash
         * of currency + locale) to ensure selection stability when the locale set changes.
         */
        public static String getExtraLocale(String currency, Set<String> exclude) {
            Set<String> allLocales = new TreeSet<>();
            allLocales.addAll(getCoreLocales());
            allLocales.addAll(getExtendedModernLocales());

            List<String> candidates = new ArrayList<>();
            for (String loc : allLocales) {
                if (!exclude.contains(loc)) {
                    candidates.add(loc);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            String bestLocale = null;
            long bestHash = Long.MAX_VALUE;
            for (String loc : candidates) {
                long hash = stableHash(currency + "_" + loc);
                if (hash < bestHash) {
                    bestHash = hash;
                    bestLocale = loc;
                }
            }
            return bestLocale;
        }

        /**
         * Deterministically selects an 'extra' currency to test with a locale from all modern
         * currencies, excluding the specified ones. Uses a consistent hashing style (minimizing
         * hash of locale + currency) to ensure selection stability when the currency set changes.
         */
        public static String getExtraCurrency(String locale, Set<String> exclude) {
            Set<String> allCurrencies = new TreeSet<>();
            allCurrencies.addAll(getCoreCurrencies());
            allCurrencies.addAll(getExtendedModernCurrencies());

            List<String> candidates = new ArrayList<>();
            for (String curr : allCurrencies) {
                if (!exclude.contains(curr) && !curr.isEmpty()) {
                    candidates.add(curr);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            String bestCurrency = null;
            long bestHash = Long.MAX_VALUE;
            for (String curr : candidates) {
                long hash = stableHash(locale + "_" + curr);
                if (hash < bestHash) {
                    bestHash = hash;
                    bestCurrency = curr;
                }
            }
            return bestCurrency;
        }

        /**
         * Resolves the default currency for a given locale string using supplemental data. Returns
         * null if the default currency is 'XXX' (no currency).
         */
        public static String getDefaultCurrencyForLocale(String localeStr) {
            ULocale locale = new ULocale(localeStr);
            ULocale maximized = ULocale.addLikelySubtags(locale);
            String territory = maximized.getCountry();
            if (territory == null || territory.isEmpty()) {
                return null;
            }
            String defaultCurrency =
                    CLDR_CONFIG.getSupplementalDataInfo().getDefaultCurrency(territory);
            return "XXX".equals(defaultCurrency) ? null : defaultCurrency;
        }

        private Dimensions() {}
    }

    private static final String OUTPUT_SUBDIR = "currency";

    static class Combination {
        String locale;
        String currency;
        Dimensions.CurrencyFormatLength formatLength;
        Dimensions.CurrencyFormatType formatType;
        Dimensions.CurrencyDisplay currencyDisplay;
        Double number;

        Combination(
                String locale,
                String currency,
                Dimensions.CurrencyFormatLength formatLength,
                Dimensions.CurrencyFormatType formatType,
                Dimensions.CurrencyDisplay currencyDisplay,
                Double number) {
            this.locale = locale;
            this.currency = currency;
            this.formatLength = formatLength;
            this.formatType = formatType;
            this.currencyDisplay = currencyDisplay;
            this.number = number;
        }
    }

    public static String format(
            ULocale locale,
            String currencyCode,
            Dimensions.CurrencyFormatLength formatLength,
            Dimensions.CurrencyFormatType formatType,
            Dimensions.CurrencyDisplay currencyDisplay,
            double number) {
        com.ibm.icu.number.LocalizedNumberFormatter lnf = NumberFormatter.withLocale(locale);

        if (!currencyCode.isEmpty()) {
            Currency currency = Currency.getInstance(currencyCode);
            UnitWidth width;
            switch (currencyDisplay) {
                case SYMBOL:
                    width = UnitWidth.SHORT;
                    break;
                case NARROW_SYMBOL:
                    width = UnitWidth.NARROW;
                    break;
                case ISO_CODE:
                    width = UnitWidth.ISO_CODE;
                    break;
                case NAME:
                    width = UnitWidth.FULL_NAME;
                    break;
                case NO_CURRENCY:
                    width = UnitWidth.HIDDEN;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown currency display: " + currencyDisplay);
            }
            lnf = lnf.unit(currency).unitWidth(width);
        }

        if (formatType == Dimensions.CurrencyFormatType.ACCOUNTING) {
            lnf = lnf.sign(SignDisplay.ACCOUNTING);
        }

        if (formatLength == Dimensions.CurrencyFormatLength.SHORT) {
            lnf = lnf.notation(Notation.compactShort());
        }

        return lnf.format(number).toString();
    }

    static class TestCase {
        final String locale;
        final String currency;
        final String currency_format_length;
        final String currency_format_type;
        final String currency_display;
        final double input;
        final String expected;

        TestCase(
                String locale,
                String currency,
                String currencyFormatLength,
                String currencyFormatType,
                String currencyDisplay,
                double input,
                String expected) {
            this.locale = locale;
            this.currency = currency;
            this.currency_format_length = currencyFormatLength;
            this.currency_format_type = currencyFormatType;
            this.currency_display = currencyDisplay;
            this.input = input;
            this.expected = expected;
        }
    }

    private static List<TestCase> generateTestCases(
            Iterable<String> locales,
            Iterable<String> currencies,
            Iterable<Style> styles,
            Iterable<Double> numbers,
            Predicate<Combination> filter) {
        List<TestCase> results = new ArrayList<>();
        for (String localeStr : locales) {
            ULocale locale = new ULocale(localeStr);
            // Load CLDRFile once per locale to check for currency-specific patterns
            CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make(localeStr, true);
            for (String currency : currencies) {
                for (Style style : styles) {
                    // Workaround for ICU bug: ICU throws AssertionError when formatting with
                    // UnitWidth.FULL_NAME (NAME style)
                    // if the locale has a currency-specific custom pattern defined in CLDR.
                    if (style.currencyDisplay == Dimensions.CurrencyDisplay.NAME) {
                        String customPattern =
                                cldrFile.getStringValue(
                                        "//ldml/numbers/currencies/currency[@type=\""
                                                + currency
                                                + "\"]/pattern[@type=\"standard\"]");
                        if (customPattern != null) {
                            continue;
                        }
                    }
                    for (Double number : numbers) {
                        Combination combo =
                                new Combination(
                                        localeStr,
                                        currency,
                                        style.formatLength,
                                        style.formatType,
                                        style.currencyDisplay,
                                        number);
                        if (!filter.test(combo)) {
                            continue;
                        }
                        String expected =
                                format(
                                        locale,
                                        currency,
                                        style.formatLength,
                                        style.formatType,
                                        style.currencyDisplay,
                                        number);
                        results.add(
                                new TestCase(
                                        localeStr,
                                        currency,
                                        style.formatLength.getLabel(),
                                        style.formatType.getLabel(),
                                        style.currencyDisplay.getLabel(),
                                        number,
                                        expected));
                    }
                }
            }
        }
        return results;
    }

    private static void writeTsv(List<TestCase> testCases, String filenamePrefix)
            throws IOException {
        String filename = filenamePrefix + ".tsv";
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println(
                    "# locale\tcurrency\tcurrency_format_length\tcurrency_format_type\tcurrency_display\tinput\texpected");
            for (TestCase tc : testCases) {
                pw.println(
                        tc.locale
                                + "\t"
                                + tc.currency
                                + "\t"
                                + tc.currency_format_length
                                + "\t"
                                + tc.currency_format_type
                                + "\t"
                                + tc.currency_display
                                + "\t"
                                + tc.input
                                + "\t"
                                + tc.expected);
            }
        }
    }

    static class StylePair {
        final Dimensions.CurrencyFormatLength length;
        final Dimensions.CurrencyFormatType type;

        StylePair(Dimensions.CurrencyFormatLength length, Dimensions.CurrencyFormatType type) {
            this.length = length;
            this.type = type;
        }
    }

    static class Style {
        final Dimensions.CurrencyFormatLength formatLength;
        final Dimensions.CurrencyFormatType formatType;
        final Dimensions.CurrencyDisplay currencyDisplay;

        Style(
                Dimensions.CurrencyFormatLength formatLength,
                Dimensions.CurrencyFormatType formatType,
                Dimensions.CurrencyDisplay currencyDisplay) {
            this.formatLength = formatLength;
            this.formatType = formatType;
            this.currencyDisplay = currencyDisplay;
        }
    }

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR);
        if (Files.exists(outDir)) {
            try (java.nio.file.DirectoryStream<Path> stream =
                    Files.newDirectoryStream(outDir, "*.tsv")) {
                for (Path p : stream) {
                    Files.delete(p);
                }
            }
        }

        Set<String> coreLocales = Dimensions.getCoreLocales();
        Set<String> extendedModernLocales = Dimensions.getExtendedModernLocales();

        Set<String> coreCurrencies = Dimensions.getCoreCurrencies();
        Set<String> extendedModernCurrencies = Dimensions.getExtendedModernCurrencies();

        Set<Double> coreNumbers = Dimensions.CORE_NUMBERS;
        Set<Double> extendedNumbers = Dimensions.getExtendedNumbers();
        extendedNumbers.removeAll(coreNumbers);

        List<StylePair> validPairs =
                List.of(
                        new StylePair(
                                Dimensions.CurrencyFormatLength.STANDARD,
                                Dimensions.CurrencyFormatType.STANDARD),
                        new StylePair(
                                Dimensions.CurrencyFormatLength.STANDARD,
                                Dimensions.CurrencyFormatType.ACCOUNTING),
                        new StylePair(
                                Dimensions.CurrencyFormatLength.SHORT,
                                Dimensions.CurrencyFormatType.STANDARD));

        List<Style> allStyles = new ArrayList<>();
        List<Style> extendedStyles = new ArrayList<>();
        for (StylePair pair : validPairs) {
            for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
                Style style = new Style(pair.length, pair.type, cd);
                allStyles.add(style);
                if (cd != Dimensions.CurrencyDisplay.NO_CURRENCY) {
                    extendedStyles.add(style);
                }
            }
        }

        // 1. Core Locales, Core Currencies, All Styles, Core Numbers -> currencies.tsv
        List<TestCase> coreCases =
                generateTestCases(
                        coreLocales, coreCurrencies, allStyles, coreNumbers, combo -> true);
        writeTsv(coreCases, "currencies");

        // 2. Extended Modern Currencies (optimized with mixing approach, split by CurrencyDisplay)
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            if (cd == Dimensions.CurrencyDisplay.NO_CURRENCY) {
                continue; // Exclude NO_CURRENCY from extended suites
            }
            List<Style> displayStyles = new ArrayList<>();
            for (StylePair pair : validPairs) {
                displayStyles.add(new Style(pair.length, pair.type, cd));
            }

            List<TestCase> cases = new ArrayList<>();
            for (String currency : extendedModernCurrencies) {
                Set<String> localesToTest = new TreeSet<>(Dimensions.getTinyLocales());
                String belongsLocale = Dimensions.getRepresentativeLocale(currency);
                if (belongsLocale != null) {
                    localesToTest.add(belongsLocale);
                }
                String extraLocale = Dimensions.getExtraLocale(currency, localesToTest);
                if (extraLocale != null) {
                    localesToTest.add(extraLocale);
                }

                cases.addAll(
                        generateTestCases(
                                localesToTest,
                                Collections.singletonList(currency),
                                displayStyles,
                                Dimensions.getTinyNumbers(),
                                combo -> true));
            }

            String displayLabel = cd.getLabel();
            if (displayLabel.equals("narrowSymbol")) {
                displayLabel = "narrow";
            }
            writeTsv(cases, "currencies_" + displayLabel + "_modern_currencies");
        }

        // 3. Extended Modern Locales (optimized with mixing approach, consolidated for extended
        // styles)
        List<TestCase> extLocCases = new ArrayList<>();
        for (String locale : extendedModernLocales) {
            Set<String> currenciesToTest = new TreeSet<>(Dimensions.getTinyCurrencies());
            String defaultCurrency = Dimensions.getDefaultCurrencyForLocale(locale);
            if (defaultCurrency != null) {
                currenciesToTest.add(defaultCurrency);
            }
            String extraCurrency = Dimensions.getExtraCurrency(locale, currenciesToTest);
            if (extraCurrency != null) {
                currenciesToTest.add(extraCurrency);
            }

            extLocCases.addAll(
                    generateTestCases(
                            Collections.singletonList(locale),
                            currenciesToTest,
                            extendedStyles,
                            Dimensions.getTinyNumbers(),
                            combo -> true));
        }
        writeTsv(extLocCases, "currencies_modern_locales");

        // 4. Extended Numbers (optimized with Tiny Locales and Tiny Currencies, split by
        // CurrencyDisplay)
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            if (cd == Dimensions.CurrencyDisplay.NO_CURRENCY) {
                continue; // Exclude NO_CURRENCY from extended suites
            }
            List<Style> displayStyles = new ArrayList<>();
            for (StylePair pair : validPairs) {
                displayStyles.add(new Style(pair.length, pair.type, cd));
            }
            List<TestCase> cases =
                    generateTestCases(
                            Dimensions.getTinyLocales(),
                            Dimensions.getTinyCurrencies(),
                            displayStyles,
                            extendedNumbers,
                            combo -> true);
            String displayLabel = cd.getLabel();
            if (displayLabel.equals("narrowSymbol")) {
                displayLabel = "narrow";
            }
            writeTsv(cases, "currencies_" + displayLabel + "_extended_numbers");
        }

        System.out.println("Currency format test data generation completed.");
    }
}
