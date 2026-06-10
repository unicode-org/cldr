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

public class GenerateCurrencyFormatTestData {

    // TODO: Remove duplicate dimension definitions (CORE_LOCALES, CORE_NUMBERS, etc.) and
    // deduplicate with GenerateDecimalFormatTestData once both PRs are merged into master.
    public static final class Dimensions {
        private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

        // TODO: Replace with GenerateDecimalFormatTestData.Dimensions.getCoreLocales() once
        // submitted.
        private static final ImmutableSet<String> CORE_LOCALES =
                ImmutableSet.of("ar", "ar_EG", "bn", "de", "de_CH", "en", "ja", "pt_PT", "ru");

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
                        "EGP");

        public static Set<String> getAllLocales() {
            return CLDR_FACTORY.getAvailableLanguages();
        }

        public static ImmutableSet<String> getCoreLocales() {
            return CORE_LOCALES;
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

        public enum ValueFormatLength {
            DECIMAL("decimal"),
            COMPACT_SHORT("compact-short");

            private final String label;

            ValueFormatLength(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        public enum CurrencyDisplay {
            SYMBOL("symbol"),
            NARROW_SYMBOL("narrowSymbol"),
            CODE("code"),
            NAME("name");

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

        private Dimensions() {}
    }

    private static final String OUTPUT_SUBDIR = "currency";

    static class Combination {
        String locale;
        String currency;
        Dimensions.CurrencyFormatType formatType;
        Dimensions.ValueFormatLength formatLength;
        Dimensions.CurrencyDisplay currencyDisplay;
        Double number;

        Combination(
                String locale,
                String currency,
                Dimensions.CurrencyFormatType formatType,
                Dimensions.ValueFormatLength formatLength,
                Dimensions.CurrencyDisplay currencyDisplay,
                Double number) {
            this.locale = locale;
            this.currency = currency;
            this.formatType = formatType;
            this.formatLength = formatLength;
            this.currencyDisplay = currencyDisplay;
            this.number = number;
        }
    }

    public static String format(
            ULocale locale,
            String currencyCode,
            Dimensions.CurrencyFormatType formatType,
            Dimensions.ValueFormatLength formatLength,
            Dimensions.CurrencyDisplay currencyDisplay,
            double number) {
        Currency currency = Currency.getInstance(currencyCode);
        UnitWidth width;
        switch (currencyDisplay) {
            case SYMBOL:
                width = UnitWidth.SHORT;
                break;
            case NARROW_SYMBOL:
                width = UnitWidth.NARROW;
                break;
            case CODE:
                width = UnitWidth.ISO_CODE;
                break;
            case NAME:
                width = UnitWidth.FULL_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown currency display: " + currencyDisplay);
        }

        com.ibm.icu.number.LocalizedNumberFormatter lnf =
                NumberFormatter.withLocale(locale).unit(currency).unitWidth(width);

        switch (formatType) {
            case STANDARD:
                break;
            case ACCOUNTING:
                lnf = lnf.sign(SignDisplay.ACCOUNTING);
                break;
            default:
                throw new IllegalArgumentException("Unknown format type: " + formatType);
        }

        switch (formatLength) {
            case DECIMAL:
                break;
            case COMPACT_SHORT:
                lnf = lnf.notation(Notation.compactShort());
                break;
            default:
                throw new IllegalArgumentException("Unknown format length: " + formatLength);
        }
        return lnf.format(number).toString();
    }

    static class TestCase {
        final String locale;
        final String currency;
        final String currency_format;
        final String value_format_length;
        final String currency_display;
        final double input;
        final String expected;

        TestCase(
                String locale,
                String currency,
                String currencyFormat,
                String valueFormatLength,
                String currencyDisplay,
                double input,
                String expected) {
            this.locale = locale;
            this.currency = currency;
            this.currency_format = currencyFormat;
            this.value_format_length = valueFormatLength;
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
            for (String currency : currencies) {
                for (Style style : styles) {
                    for (Double number : numbers) {
                        Combination combo =
                                new Combination(
                                        localeStr,
                                        currency,
                                        style.formatType,
                                        style.formatLength,
                                        style.currencyDisplay,
                                        number);
                        if (!filter.test(combo)) {
                            continue;
                        }
                        String expected =
                                format(
                                        locale,
                                        currency,
                                        style.formatType,
                                        style.formatLength,
                                        style.currencyDisplay,
                                        number);
                        results.add(
                                new TestCase(
                                        localeStr,
                                        currency,
                                        style.formatType.getLabel(),
                                        style.formatLength.getLabel(),
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
        String filename = filenamePrefix + "_" + (testCases.size() + 1) + "_lines.tsv";
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename)) {
            pw.println(
                    "locale\tcurrency\tcurrency_format\tvalue_format_length\tcurrency_display\tinput\texpected");
            for (TestCase tc : testCases) {
                pw.println(
                        tc.locale
                                + "\t"
                                + tc.currency
                                + "\t"
                                + tc.currency_format
                                + "\t"
                                + tc.value_format_length
                                + "\t"
                                + tc.currency_display
                                + "\t"
                                + tc.input
                                + "\t"
                                + tc.expected);
            }
        }
    }

    static class Style {
        final Dimensions.CurrencyFormatType formatType;
        final Dimensions.ValueFormatLength formatLength;
        final Dimensions.CurrencyDisplay currencyDisplay;

        Style(
                Dimensions.CurrencyFormatType formatType,
                Dimensions.ValueFormatLength formatLength,
                Dimensions.CurrencyDisplay currencyDisplay) {
            this.formatType = formatType;
            this.formatLength = formatLength;
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
        Set<Double> extendedNumbers = new TreeSet<>(Dimensions.getExtendedNumbers());
        extendedNumbers.removeAll(coreNumbers);

        List<Style> allStyles = new ArrayList<>();
        for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
            for (Dimensions.ValueFormatLength fl : Dimensions.ValueFormatLength.values()) {
                for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
                    allStyles.add(new Style(ft, fl, cd));
                }
            }
        }

        // 1. Core Locales, Core Currencies, All Styles, Core Numbers -> currencies_<count>.tsv
        List<TestCase> coreCases =
                generateTestCases(
                        coreLocales, coreCurrencies, allStyles, coreNumbers, combo -> true);
        writeTsv(coreCases, "currencies");

        // For the extended suites, split by CurrencyDisplay
        for (Dimensions.CurrencyDisplay currencyDisplay : Dimensions.CurrencyDisplay.values()) {
            List<Style> displayStyles = new ArrayList<>();
            for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
                for (Dimensions.ValueFormatLength fl : Dimensions.ValueFormatLength.values()) {
                    displayStyles.add(new Style(ft, fl, currencyDisplay));
                }
            }
            String suffix = "_" + currencyDisplay.getLabel();

            // 2. Extended Modern Currencies split
            List<TestCase> extCurrCases =
                    generateTestCases(
                            coreLocales,
                            extendedModernCurrencies,
                            displayStyles,
                            coreNumbers,
                            combo -> true);
            writeTsv(extCurrCases, "currencies_all_modern_currencies" + suffix);

            // 3. Extended Modern Locales split
            List<TestCase> extLocCases =
                    generateTestCases(
                            extendedModernLocales,
                            coreCurrencies,
                            displayStyles,
                            coreNumbers,
                            combo -> true);
            writeTsv(extLocCases, "currencies_all_modern_locales" + suffix);

            // 4. Extended Numbers split
            List<TestCase> extNumCases =
                    generateTestCases(
                            coreLocales,
                            coreCurrencies,
                            displayStyles,
                            extendedNumbers,
                            combo -> true);
            writeTsv(extNumCases, "currencies_extended_numbers" + suffix);
        }

        System.out.println("Currency format test data generation completed.");
    }
}
