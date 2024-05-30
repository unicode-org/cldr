package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitPreferences;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

/** Quick extraction from TestUnits; TODO pretty it up */
public class GenerateUnitTestData {

    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final UnitConverter converter = SDI.getUnitConverter();
    private static final String TEST_SEP = ";\t";
    private static final Set<String> NOT_CONVERTABLE = ImmutableSet.of("generic");
    private static final Rational R1000 = Rational.of(1000);

    private static final Map<String, String> CORE_TO_TYPE;
    private static final Multimap<String, String> TYPE_TO_CORE;

    public static void main(String[] args) {
        GenerateUnitTestData item = new GenerateUnitTestData();
        item.TestParseUnit();
        item.TestUnitPreferences();
        item.generateUnitLocalePreferences();
    }

    static {
        Set<String> VALID_UNITS =
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);

        Map<String, String> coreToType = new TreeMap<>();
        TreeMultimap<String, String> typeToCore = TreeMultimap.create();
        for (String s : VALID_UNITS) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0, dashPos);
            String coreUnit = s.substring(dashPos + 1);
            coreUnit = converter.fixDenormalized(coreUnit);
            coreToType.put(coreUnit, unitType);
            typeToCore.put(unitType, coreUnit);
        }
        CORE_TO_TYPE = ImmutableMap.copyOf(coreToType);
        TYPE_TO_CORE = ImmutableMultimap.copyOf(typeToCore);
    }

    public void TestParseUnit() {
        Output<String> compoundBaseUnit = new Output<>();
        String[][] tests = {
            {"kilometer-pound-per-hour", "kilogram-meter-per-second", "45359237/360000000"},
            {"kilometer-per-hour", "meter-per-second", "5/18"},
        };
        //        for (String[] test : tests) {
        //            String source = test[0];
        //            String expectedUnit = test[1];
        //            Rational expectedRational = new Rational.RationalParser().parse(test[2]);
        //            ConversionInfo unitInfo = converter.parseUnitId(source, compoundBaseUnit,
        // false);
        //            assertEquals(source, expectedUnit, compoundBaseUnit.value);
        //            assertEquals(source, expectedRational, unitInfo.factor);
        //        }

        // check all
        Set<String> badUnits = new LinkedHashSet<>();
        Set<String> noQuantity = new LinkedHashSet<>();
        Multimap<Pair<String, Double>, String> testPrintout = TreeMultimap.create();

        // checkUnitConvertability(converter, compoundBaseUnit, badUnits, "pint-metric-per-second");

        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            String type = entry.getKey();
            String unit = entry.getValue();
            if (NOT_CONVERTABLE.contains(unit)) {
                continue;
            }
            checkUnitConvertability(
                    converter, compoundBaseUnit, badUnits, noQuantity, type, unit, testPrintout);
        }
        if (true) { // test data
            try (TempPrintWriter pw =
                    TempPrintWriter.openUTF8Writer(
                            CLDRPaths.TEST_DATA + "units", "unitsTest.txt")) {

                pw.println(
                        "# Test data for unit conversions\n"
                                + CldrUtility.getCopyrightString("#  ")
                                + "\n"
                                + "#\n"
                                + "# Format:\n"
                                + "#\tQuantity\t;\tx\t;\ty\t;\tconversion to y (rational)\t;\ttest: 1000 x ⟹ y\n"
                                + "#\n"
                                + "# Use: convert 1000 x units to the y unit; the result should match the final column,\n"
                                + "#   at the given precision. For example, when the last column is 159.1549,\n"
                                + "#   round to 4 decimal digits before comparing.\n"
                                + "# Note that certain conversions are approximate, such as degrees to radians\n"
                                + "#\n"
                                + "# Generation: Use GenerateUnitTestData.java to regenerate unitsTest.txt.\n");
                for (Entry<Pair<String, Double>, String> entry : testPrintout.entries()) {
                    pw.println(entry.getValue());
                }
            }
        }
    }

    public void TestUnitPreferences() {
        UnitPreferences prefs = SDI.getUnitPreferences();
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + "units", "unitPreferencesTest.txt")) {
            pw.println(getHeader("Region"));
            Rational ONE_TENTH = Rational.of(1, 10);

            // Note that for production usage, precomputed data like the
            // prefs.getFastMap(converter) would be used instead of the raw data.

            for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry :
                    prefs.getData().entrySet()) {
                String quantity = entry.getKey();
                String baseUnit = converter.getBaseUnitFromQuantity(quantity);
                for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 :
                        entry.getValue().entrySet()) {
                    String usage = entry2.getKey();

                    // collect samples of base units
                    for (Entry<Set<String>, Collection<UnitPreference>> entry3 :
                            entry2.getValue().asMap().entrySet()) {
                        boolean first = true;
                        Set<Rational> samples = new TreeSet<>(Comparator.reverseOrder());
                        for (UnitPreference pref : entry3.getValue()) {
                            final String topUnit =
                                    UnitPreferences.SPLIT_AND.split(pref.unit).iterator().next();
                            if (first) {
                                samples.add(
                                        converter.convert(
                                                pref.geq.add(ONE_TENTH), topUnit, baseUnit, false));
                                first = false;
                            }
                            samples.add(converter.convert(pref.geq, topUnit, baseUnit, false));
                            samples.add(
                                    converter.convert(
                                            pref.geq.subtract(ONE_TENTH),
                                            topUnit,
                                            baseUnit,
                                            false));
                        }
                        // show samples
                        Set<String> regions = entry3.getKey();
                        String sampleRegion = regions.iterator().next();
                        Collection<UnitPreference> uprefs = entry3.getValue();
                        for (Rational sample : samples) {
                            showSample(quantity, usage, sampleRegion, sample, baseUnit, uprefs, pw);
                        }
                        pw.println();
                    }
                }
            }
        }
    }

    public void generateUnitLocalePreferences() {
        try (TempPrintWriter pwLocale =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + "units", "unitLocalePreferencesTest.txt")) {

            try {
                Set<List<Object>> seen = new HashSet<>();
                // first copy existing lines
                // This includes the header, so modify the old header if changes are needed!
                Files.lines(Path.of(CLDRPaths.TEST_DATA + "units/unitLocalePreferencesTest.txt"))
                        .forEach(line -> formatPwLocale(pwLocale, line, seen));
                // TODO: add more lines
                formatLocaleLine(
                        "byte-per-millisecond", Rational.of(123), "default", "en", "", seen);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static final Splitter SPLIT_SEMI = Splitter.on(Pattern.compile("\\s*;\\s*")).trimResults();

    private void formatPwLocale(TempPrintWriter pwLocale, String rawLine, Set<List<Object>> seen) {
        int hashPos = rawLine.indexOf('#');
        String line = hashPos < 0 ? rawLine : rawLine.substring(0, hashPos);
        String comment = hashPos < 0 ? "" : "#" + rawLine.substring(hashPos + 1);
        if (line.isBlank()) {
            if (!comment.isBlank()) {
                pwLocale.println(comment);
            }
            return;
        }
        List<String> parts = SPLIT_SEMI.splitToList(line);

        String sourceUnit = parts.get(0);
        Rational sourceAmount = Rational.of(parts.get(1));
        String usage = parts.get(2);
        String languageTag = parts.get(3);
        String newLine =
                formatLocaleLine(sourceUnit, sourceAmount, usage, languageTag, comment, seen);
        if (newLine != null) {
            pwLocale.println(newLine);
        }
    }

    public String formatLocaleLine(
            String sourceUnit,
            Rational sourceAmount,
            String usage,
            String languageTag,
            String comment,
            Set<List<Object>> seen) {
        List<Object> bundle = List.of(sourceUnit, sourceAmount, usage, languageTag);
        if (bundle.contains(seen)) {
            return null;
        }
        seen.add(bundle);

        UnitPreferences prefs = SDI.getUnitPreferences();
        final ULocale uLocale = ULocale.forLanguageTag(languageTag);
        UnitPreference unitPreference =
                prefs.getUnitPreference(sourceAmount, sourceUnit, usage, uLocale);
        if (unitPreference == null) { // if the quantity isn't found
            throw new IllegalArgumentException(
                    String.format(
                            "No unit preferences found for unit: %s, usage: %s, locale:%s",
                            sourceUnit, usage, languageTag));
        }
        String actualUnit = unitPreference.unit;
        Rational actualValue =
                converter.convert(sourceAmount, sourceUnit, unitPreference.unit, false);
        // #    input-unit; amount; usage;  languageTag; expected-unit; expected-amount # comment
        final String newFileLine =
                String.format(
                        "%s;\t%s;\t%s;\t%s;\t%s;\t%s%s",
                        sourceUnit,
                        sourceAmount.toString(FormatStyle.formatted),
                        usage,
                        languageTag,
                        actualUnit,
                        actualValue.toString(FormatStyle.formatted),
                        comment.isBlank() ? "" : "\t" + comment);
        return newFileLine;
    }

    static LikelySubtags likely = new LikelySubtags();

    public String getHeader(String regionOrLocale) {
        return "\n# Test data for unit region preferences\n"
                + CldrUtility.getCopyrightString("#  ")
                + "\n"
                + "#\n"
                + "# Format:\n"
                + "#\tQuantity;\tUsage;\t"
                + regionOrLocale
                + ";\tInput (r);\tInput (d);\tInput Unit;\tOutput (r);\tOutput (d);\tOutput Unit\n"
                + "#\n"
                + "# Use: Convert the Input amount & unit according to the Usage and "
                + regionOrLocale
                + ".\n"
                + "#\t The result should match the Output amount and unit.\n"
                + "#\t Both rational (r) and double64 (d) forms of the input and output amounts are supplied so that implementations\n"
                + "#\t have two options for testing based on the precision in their implementations. For example:\n"
                + "#\t   3429 / 12500; 0.27432; meter;\n"
                + "#\t The Output amount and Unit are repeated for mixed units. In such a case, only the smallest unit will have\n"
                + "#\t both a rational and decimal amount; the others will have a single integer value, such as:\n"
                + "#\t   length; person-height; CA; 3429 / 12500; 0.27432; meter; 2; foot; 54 / 5; 10.8; inch\n"
                + "#\t The input and output units are unit identifers; in particular, the output does not have further processing:\n"
                + "#\t\t • no localization\n"
                + "#\t\t • no adjustment for pluralization\n"
                + "#\t\t • no formatted with the skeleton\n"
                + "#\t\t • no suppression of zero values (for secondary -and- units such as pound in stone-and-pound)\n"
                + "#\n"
                + "# Generation: Use GenerateUnitTestData.java to regenerate unitPreferencesTest.txt.\n";
    }

    private void showSample(
            String quantity,
            String usage,
            String sampleRegionOrLocale,
            Rational sampleBaseValue,
            String baseUnit,
            Collection<UnitPreference> prefs,
            TempPrintWriter pw) {
        String lastUnit = null;
        boolean gotOne = false;
        for (UnitPreference pref : prefs) {
            final String topUnit = UnitPreferences.SPLIT_AND.split(pref.unit).iterator().next();
            Rational baseGeq = converter.convert(pref.geq, topUnit, baseUnit, false);
            if (sampleBaseValue.compareTo(baseGeq) >= 0) {
                showSample2(
                        quantity,
                        usage,
                        sampleRegionOrLocale,
                        sampleBaseValue,
                        baseUnit,
                        pref.unit,
                        pw);
                gotOne = true;
                break;
            }
            lastUnit = pref.unit;
        }
        if (!gotOne) {
            showSample2(
                    quantity, usage, sampleRegionOrLocale, sampleBaseValue, baseUnit, lastUnit, pw);
        }
    }

    private void showSample2(
            String quantity,
            String usage,
            String sampleRegionOrLocale,
            Rational sampleBaseValue,
            String baseUnit,
            String lastUnit,
            TempPrintWriter pw) {
        Rational originalSampleBaseValue = sampleBaseValue;
        // Known slow algorithm for mixed values, but for generating tests we don't care.
        final List<String> units = UnitPreferences.SPLIT_AND.splitToList(lastUnit);
        StringBuilder formattedUnit = new StringBuilder();
        int remaining = units.size();
        for (String unit : units) {
            --remaining;
            Rational sample = converter.convert(sampleBaseValue, baseUnit, unit, false);
            if (formattedUnit.length() != 0) {
                formattedUnit.append(TEST_SEP);
            }
            if (remaining != 0) {
                BigInteger floor = sample.floor();
                formattedUnit.append(floor + TEST_SEP + unit);
                // convert back to base unit
                sampleBaseValue =
                        converter.convert(
                                sample.subtract(Rational.of(floor)), unit, baseUnit, false);
            } else {
                formattedUnit.append(sample + TEST_SEP + sample.doubleValue() + TEST_SEP + unit);
            }
        }
        pw.println(
                quantity
                        + TEST_SEP
                        + usage
                        + TEST_SEP
                        + sampleRegionOrLocale
                        + TEST_SEP
                        + originalSampleBaseValue
                        + TEST_SEP
                        + originalSampleBaseValue.doubleValue()
                        + TEST_SEP
                        + baseUnit
                        + TEST_SEP
                        + formattedUnit);
    }

    private void checkUnitConvertability(
            UnitConverter converter,
            Output<String> compoundBaseUnit,
            Set<String> badUnits,
            Set<String> noQuantity,
            String type,
            String unit,
            Multimap<Pair<String, Double>, String> testPrintout) {

        if (converter.isBaseUnit(unit)) {
            String quantity = converter.getQuantityFromBaseUnit(unit);
            if (quantity == null) {
                noQuantity.add(unit);
            }
            if (true) {
                testPrintout.put(
                        new Pair<>(quantity, 1000d),
                        quantity + "\t;\t" + unit + "\t;\t" + unit + "\t;\t1 * x\t;\t1,000.00");
            }
        } else {
            ConversionInfo unitInfo = converter.getUnitInfo(unit, compoundBaseUnit);
            if (unitInfo == null) {
                unitInfo = converter.parseUnitId(unit, compoundBaseUnit, false);
            }
            if (unitInfo == null) {
                badUnits.add(unit);
            } else if (true) {
                String quantity = converter.getQuantityFromBaseUnit(compoundBaseUnit.value);
                if (quantity == null) {
                    noQuantity.add(compoundBaseUnit.value);
                }
                final double testValue =
                        unitInfo.convert(R1000).toBigDecimal(MathContext.DECIMAL32).doubleValue();
                testPrintout.put(
                        new Pair<>(quantity, testValue),
                        quantity
                                + "\t;\t"
                                + unit
                                + "\t;\t"
                                + compoundBaseUnit
                                + "\t;\t"
                                + unitInfo
                                + "\t;\t"
                                + testValue
                        //                    + "\t" +
                        // unitInfo.factor.toBigDecimal(MathContext.DECIMAL32)
                        //                    + "\t" +
                        // unitInfo.factor.reciprocal().toBigDecimal(MathContext.DECIMAL32)
                        );
            }
        }
    }
}
