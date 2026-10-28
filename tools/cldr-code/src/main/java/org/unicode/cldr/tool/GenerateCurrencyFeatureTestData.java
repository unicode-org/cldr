package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.FeatureRegistry.Feature;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.TempPrintWriter;

/**
 * Generates the feature-based currency test data in {@code common/testData/currency}: for each
 * feature in {@code features.tsv}, the file {@code core_<feature_id>.tsv} with every combination of
 * the feature's core set and the value that ICU4J formats for it.
 *
 * <p>The generator only has the dimensions, and dimension values, that some feature needs. The code
 * that a feature needs has a comment with the feature ID and its clause in the TR35 currency
 * coverage doc (for example S1.1b).
 */
public class GenerateCurrencyFeatureTestData {
    /** The test data category, which is also the name of its directory. */
    public static final String CATEGORY = "currency";

    /** A dimension or dimension value, with its label in the TSV files. */
    interface Labeled {
        String label();
    }

    /** The dimensions, in column order. An empty cell means the default value of the dimension. */
    public enum Dimension implements Labeled {
        /** CLDR locale ID. Required. */
        LOCALE("locale"),
        /** ISO 4217 currency code. Required. */
        CURRENCY("currency"),
        /** A {@link CurrencyFormatLength}. The default is the non-compact format. */
        CURRENCY_FORMAT_LENGTH("currency_format_length"),
        /** A {@link CurrencyDisplay}. The default is {@code symbol}. */
        CURRENCY_DISPLAY("currency_display"),
        /** The amount to format. Required. */
        INPUT("input");

        private final String label;

        Dimension(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** The type of {@code <currencyFormatLength>}. The default is the non-compact format. */
    public enum CurrencyFormatLength implements Labeled {
        // currency_decimal_separator_locale_compact (S1.1b)
        SHORT("short", Notation.compactShort());

        private final String label;
        private final Notation notation;

        CurrencyFormatLength(String label, Notation notation) {
            this.label = label;
            this.notation = notation;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** How the currency is shown. The default is {@code symbol}. */
    public enum CurrencyDisplay implements Labeled {
        SYMBOL("symbol", UnitWidth.SHORT),
        // currency_decimal_separator_locale (S1.1a), currency_group_separator_locale (S1.3a)
        SYMBOL_NARROW("symbolNarrow", UnitWidth.NARROW),
        // currency_decimal_separator_locale (S1.1a), currency_group_separator_locale (S1.3a)
        CODE("code", UnitWidth.ISO_CODE),
        // currency_decimal_separator_locale_name (S1.1c)
        NAME("name", UnitWidth.FULL_NAME);

        private final String label;
        private final UnitWidth width;

        CurrencyDisplay(String label, UnitWidth width) {
            this.label = label;
            this.width = width;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** The columns of every core TSV file: the dimensions, then the expected value and features. */
    public static final List<String> COLUMNS =
            ImmutableList.<String>builder()
                    .addAll(Arrays.stream(Dimension.values()).map(Dimension::label).iterator())
                    .add("expected", "main_feature", "other_features")
                    .build();

    private static final List<Dimension> REQUIRED =
            ImmutableList.of(Dimension.LOCALE, Dimension.CURRENCY, Dimension.INPUT);

    /** One test case: the value of each dimension, in column order, or "" for the default. */
    record TestCase(List<String> values) {
        String get(Dimension dimension) {
            return values.get(dimension.ordinal());
        }
    }

    public static void main(String[] args) throws IOException {
        List<Feature> features = FeatureRegistry.read(CATEGORY);
        Set<String> registered = features.stream().map(Feature::id).collect(Collectors.toSet());
        String directory = FeatureRegistry.directory(CATEGORY).toString();
        for (Feature feature : features) {
            try (TempPrintWriter out =
                    TempPrintWriter.openUTF8Writer(
                            directory, FeatureRegistry.coreFileName(feature.id()))) {
                coreFileLines(feature, registered).forEach(out::println);
            }
        }
        System.out.println("Wrote " + features.size() + " core files to " + directory);
    }

    /**
     * Returns the lines of the core TSV file of a feature, starting with the header.
     *
     * @param registered the IDs of all features in the registry
     */
    public static List<String> coreFileLines(Feature feature, Set<String> registered) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + String.join("\t", COLUMNS));
        for (TestCase testCase : testCases(feature)) {
            FormattedNumber value = format(testCase);
            List<String> cells = new ArrayList<>(testCase.values());
            cells.add(value.toString());
            cells.add(feature.id());
            cells.add(String.join(",", otherFeatures(testCase, value, feature.id(), registered)));
            lines.add(String.join("\t", cells));
        }
        return lines;
    }

    /** Returns every combination of the values in the feature's core set. */
    static List<TestCase> testCases(Feature feature) {
        Map<Dimension, List<String>> values = new EnumMap<>(Dimension.class);
        for (Dimension dimension : Dimension.values()) {
            values.put(dimension, List.of(""));
        }
        feature.coreSet()
                .forEach((label, list) -> values.put(fromLabel(Dimension.class, label), list));
        for (Dimension dimension : REQUIRED) {
            if (values.get(dimension).equals(List.of(""))) {
                throw new IllegalArgumentException(
                        feature.id() + ": the core set has no " + dimension.label());
            }
        }
        for (String locale : values.get(Dimension.LOCALE)) {
            if (!CLDRConfig.getInstance().getCldrFactory().getAvailable().contains(locale)) {
                throw new IllegalArgumentException(feature.id() + ": unknown locale " + locale);
            }
        }
        List<TestCase> testCases = new ArrayList<>();
        for (List<String> combination : Lists.cartesianProduct(List.copyOf(values.values()))) {
            testCases.add(new TestCase(combination));
        }
        return testCases;
    }

    /** Formats a test case with ICU4J, which provides the expected values for now. */
    static FormattedNumber format(TestCase testCase) {
        LocalizedNumberFormatter formatter =
                NumberFormatter.withLocale(new ULocale(testCase.get(Dimension.LOCALE)))
                        .unit(Currency.getInstance(testCase.get(Dimension.CURRENCY)));
        String length = testCase.get(Dimension.CURRENCY_FORMAT_LENGTH);
        if (!length.isEmpty()) {
            formatter = formatter.notation(fromLabel(CurrencyFormatLength.class, length).notation);
        }
        String display = testCase.get(Dimension.CURRENCY_DISPLAY);
        if (!display.isEmpty()) {
            formatter = formatter.unitWidth(fromLabel(CurrencyDisplay.class, display).width);
        }
        return formatter.format(Double.parseDouble(testCase.get(Dimension.INPUT)));
    }

    /**
     * Returns the registered features, other than the main one, that a test case exercises: a value
     * with a decimal (or grouping) separator exercises the decimal (or group) separator feature
     * that applies to the test case.
     */
    static List<String> otherFeatures(
            TestCase testCase, FormattedNumber value, String mainFeature, Set<String> registered) {
        boolean decimal = false;
        boolean grouping = false;
        ConstrainedFieldPosition position = new ConstrainedFieldPosition();
        while (value.nextPosition(position)) {
            decimal |= position.getField() == NumberFormat.Field.DECIMAL_SEPARATOR;
            grouping |= position.getField() == NumberFormat.Field.GROUPING_SEPARATOR;
        }
        Set<String> features = new LinkedHashSet<>();
        if (decimal) {
            features.add(decimalSeparatorFeature(testCase));
        }
        if (grouping) {
            features.add(groupSeparatorFeature(testCase));
        }
        features.remove(mainFeature);
        features.retainAll(registered);
        return ImmutableList.copyOf(features);
    }

    private static String decimalSeparatorFeature(TestCase testCase) {
        if (localeSymbol(testCase.get(Dimension.LOCALE), "currencyDecimal") == null) {
            return "currency_decimal_separator_regular"; // S1.2
        }
        if (!testCase.get(Dimension.CURRENCY_FORMAT_LENGTH).isEmpty()) {
            return "currency_decimal_separator_locale_compact"; // S1.1b
        }
        if (testCase.get(Dimension.CURRENCY_DISPLAY).equals(CurrencyDisplay.NAME.label)) {
            return "currency_decimal_separator_locale_name"; // S1.1c
        }
        return "currency_decimal_separator_locale"; // S1.1a
    }

    private static String groupSeparatorFeature(TestCase testCase) {
        if (localeSymbol(testCase.get(Dimension.LOCALE), "currencyGroup") == null) {
            return "currency_group_separator_regular"; // S1.4
        }
        if (!testCase.get(Dimension.CURRENCY_FORMAT_LENGTH).isEmpty()) {
            return "currency_group_separator_locale_compact"; // S1.3b, not a feature yet
        }
        if (testCase.get(Dimension.CURRENCY_DISPLAY).equals(CurrencyDisplay.NAME.label)) {
            return "currency_group_separator_locale_name"; // S1.3c, not a feature yet
        }
        return "currency_group_separator_locale"; // S1.3a
    }

    /**
     * Returns a number symbol of the locale, such as {@code currencyDecimal}, for the locale's
     * default numbering system, or null if CLDR has none.
     */
    public static String localeSymbol(String locale, String symbol) {
        CLDRFile file = CLDRConfig.getInstance().getCLDRFile(locale, true);
        String numberingSystem = file.getStringValue("//ldml/numbers/defaultNumberingSystem");
        return file.getStringValue(
                "//ldml/numbers/symbols[@numberSystem=\"" + numberingSystem + "\"]/" + symbol);
    }

    static <E extends Enum<E> & Labeled> E fromLabel(Class<E> type, String label) {
        for (E value : type.getEnumConstants()) {
            if (value.label().equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + label);
    }
}
