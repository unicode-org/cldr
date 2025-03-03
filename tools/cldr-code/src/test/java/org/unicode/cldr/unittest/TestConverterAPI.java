package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;

public class TestConverterAPI extends TestFmwk {

    private static final Joiner JOIN_EMPTY = Joiner.on("");
    private static final Splitter SPLIT_SEMI = Splitter.on(';');
    private static final UnitConverter unitConverter =
            CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

    /**
     * Adds conversion info and a baseIdentifier to MeasureUnit, and supplies utility functions for
     * conversion. MixedUnits are supported by having a (normally null) subunits field.
     */
    public static class MeasureUnit implements Comparable<MeasureUnit> {

        /**
         * Create a MeasureUnit from a unicode_unit-identifier. Internally there is a cache, so in
         * the normal case this should be fast.
         */
        public static MeasureUnit from(String unitId) {
            try {
                if (unitId.contains("-and-")) {
                    return mixedCache.get(unitId);
                }
                return unmixedCache.get(unitId);
            } catch (ExecutionException e) {
                throw (RuntimeException) e.getCause();
            }
        }

        /** Can be converted to other unit */
        public boolean isConvertibleTo(MeasureUnit other) {
            // could be optimized slightly if we 'interned' the baseIdentifier
            return getBaseIdentifier().equals(other.getBaseIdentifier());
        }

        /** Get the reciprocal; eg, foot-per-second ⇒ second-per-foot */
        public MeasureUnit reciprocal() {
            try {
                return unmixedCache.get(reciprocalOf(normalizedIdentifier));
            } catch (ExecutionException e) {
                throw (RuntimeException) e.getCause();
            }
        }

        /** Returns just the string name, not the conversion info or base identifier */
        @Override
        public String toString() {
            return normalizedIdentifier;
        }

        public boolean isMixed() {
            return subunits != null;
        }

        public String getNormalizedIdentifier() {
            return normalizedIdentifier;
        }

        public ConversionInfo getConversionInfo() {
            return conversionInfo;
        }

        public String getBaseIdentifier() {
            return baseIdentifier;
        }

        public final Rational convertToBase(Rational other) {
            return conversionInfo.convert(other);
        }

        public final Rational convertFromBase(Rational other) {
            return conversionInfo.convertBackwards(other);
        }

        // a negative value in a list is represented by *every* value being negative;
        // formatting  will change that as necessary

        public Rational convertToBase(List<Rational> amounts) {
            if (subunits == null) {
                if (amounts.size() != 1) {
                    throw new IllegalArgumentException();
                }
                return convertToBase(amounts.get(0));
            }
            if (amounts.size() > subunits.size()) {
                throw new IllegalArgumentException();
            }
            Rational sum = Rational.ZERO;
            for (int i = 0; i < amounts.size(); ++i) {
                Rational amount = amounts.get(i);
                MeasureUnit unit = subunits.get(i);
                sum = sum.add(unit.convertToBase(amount));
            }
            return sum;
        }

        // a negative value in a list is represented by *every* value being negative;
        // formatting  will change that as necessary

        public List<Rational> convertFromBaseToMixed(Rational amount) {
            if (subunits == null) {
                return List.of(conversionInfo.convertBackwards(amount));
            }

            List<Rational> result = new ArrayList<>();
            int last = subunits.size() - 1;
            for (int i = 0; i <= last; ++i) {
                MeasureUnit unit = subunits.get(i);
                Rational amountRaw = unit.convertFromBase(amount);
                if (i == last) {
                    result.add(amountRaw);
                } else {
                    Rational intPortion = Rational.of(amountRaw.floor());
                    result.add(intPortion);
                    amount = amount.subtract(unit.convertToBase(intPortion)); // could be optimized
                }
            }
            return result;
        }

        public List<MeasureUnit> getSubunits() {
            return subunits;
        }

        @Override
        public int compareTo(MeasureUnit o) {
            return ComparisonChain.start()
                    .compare(baseIdentifier, o.baseIdentifier)
                    .compare(conversionInfo, o.conversionInfo)
                    .compare(normalizedIdentifier, normalizedIdentifier) // break ties
                    .result();
        }

        // INTERNALS

        private static final LoadingCache<String, MeasureUnit> unmixedCache =
                CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .build(
                                new CacheLoader<String, MeasureUnit>() {
                                    @Override
                                    public MeasureUnit load(String unitId) {
                                        Output<String> baseUnitOut = new Output<>();
                                        ConversionInfo ci =
                                                unitConverter.parseUnitId(
                                                        unitId, baseUnitOut, false);
                                        String normalized = normalize(unitId);
                                        return new MeasureUnit(
                                                normalized, ci, baseUnitOut.value, null);
                                    }
                                });
        private static final LoadingCache<String, MeasureUnit> mixedCache =
                CacheBuilder.newBuilder()
                        .maximumSize(50)
                        .build(
                                new CacheLoader<String, MeasureUnit>() {
                                    @Override
                                    public MeasureUnit load(String unitId) {
                                        List<MeasureUnit> parts = getParts(unitId);
                                        String normalized = Joiner.on("-and-").join(parts);
                                        MeasureUnit part0 = parts.get(0);
                                        return new MeasureUnit(
                                                normalized,
                                                part0.conversionInfo,
                                                part0.baseIdentifier,
                                                List.copyOf(parts));
                                    }
                                });

        private final String normalizedIdentifier;
        private final ConversionInfo
                conversionInfo; // if subunits ≠ null, identical to first subunit's value
        private final String
                baseIdentifier; // if subunits ≠ null, identical to first subunit's value
        private final List<MeasureUnit> subunits; // null except for mixed units

        /*
         * Note:  In CLDR, ConversionInfo contains:
         *    public final Rational factor;
         *    public final Rational offset;
         *    public String special;
         *    public boolean specialInverse; // only used with special
         *
         *    convert(amount) is normally return amount.multiply(factor).add(offset);
         */

        private MeasureUnit(
                String normalizedIdentifier,
                ConversionInfo conversionInfo,
                String baseIdentifier,
                List<MeasureUnit> subunits) {
            this.normalizedIdentifier = normalizedIdentifier;
            this.conversionInfo = conversionInfo;
            this.baseIdentifier = baseIdentifier;
            this.subunits = subunits;
        }

        private String reciprocalOf(String value) {
            // This is only called in rare cases, so no need to really optimize
            // Input is guaranteed to be normalized, if original is
            if (value.startsWith("per-")) {
                return value.substring(4);
            }
            int index = value.indexOf("-per-");
            if (index < 0) {
                return "per-" + value;
            }
            return value.substring(index + 5) + "-per-" + value.substring(0, index);
        }

        public boolean isSmallerThan(MeasureUnit other) {
            return conversionInfo == null
                    ? other.conversionInfo != null
                    : other.conversionInfo == null
                            ? false
                            : conversionInfo.compareTo(other.conversionInfo) < 0;
        }

        private static List<MeasureUnit> getParts(String unitId) {
            // normalize by sorting the segments between -and- and dropping duplicates
            Set<MeasureUnit> sortedParts = new TreeSet<>(Comparator.reverseOrder());
            Splitter.on("-and-").split(unitId).forEach(x -> sortedParts.add(MeasureUnit.from(x)));
            List<MeasureUnit> parts = new ArrayList<>(sortedParts);
            if (parts.size() < 2) {
                throw new IllegalArgumentException(JOIN_EMPTY.join("«", unitId, "» has ≤1 unit"));
            }

            // Verify that the units are all comparable
            MeasureUnit lastPart = null;
            for (int i = 0; i < parts.size(); ++i) {
                MeasureUnit part = parts.get(i);
                if (lastPart != null) {
                    if (!part.isConvertibleTo(lastPart)) {
                        throw new IllegalArgumentException(
                                JOIN_EMPTY.join(
                                        "«",
                                        unitId,
                                        "»: ",
                                        lastPart,
                                        " & ",
                                        part,
                                        " are not convertible"));
                    }
                }
                lastPart = part;
            }
            if (lastPart == null) {
                throw new IllegalArgumentException(JOIN_EMPTY.join("«", unitId, "» has no units"));
            }

            return parts;
        }

        public static String normalize(String unitId) {
            UnitId id = unitConverter.createUnitId(unitId);
            return id.toString();
        }
    }

    /** Prototype converter, using CLDR internal classes */
    public static class UnitConverter2 {

        /**
         * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
         * conversion cost is 2 multiplies and 2 adds
         */
        public static double convert(
                double amount, MeasureUnit sourceUnit, MeasureUnit targetUnit) {
            return convert(Rational.of(amount), sourceUnit, targetUnit).doubleValue();
        }

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)
         */
        public static List<Double> convertDoubles(
                List<Double> amounts, MeasureUnit sourceUnit, MeasureUnit targetUnit) {
            return doubleList(convert(listDoubleToRational(amounts), sourceUnit, targetUnit));
        }

        /**
         * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
         * conversion cost is 2 multiplies and 2 adds
         */
        public static BigDecimal convert(
                BigDecimal amount, MeasureUnit sourceUnits, MeasureUnit targetUnits) {
            return convert(Rational.of(amount), sourceUnits, targetUnits).toBigDecimal();
        }

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)
         */
        public static List<BigDecimal> convertBigDecimals(
                List<BigDecimal> amounts, MeasureUnit sourceUnits, MeasureUnit targetUnits) {
            return bigDecimalList(
                    convert(listBigDecimalToRational(amounts), sourceUnits, targetUnits));
        }

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)<br>
         * [Rationals are the CLDR implementation's native numeric form, to preserve precision.]
         */
        public static List<Rational> convert(
                List<Rational> amounts, MeasureUnit sourceUnits, MeasureUnit targetUnits) {
            return targetUnits.convertFromBaseToMixed(sourceUnits.convertToBase(amounts));
        }

        /**
         * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
         * conversion cost is 2 multiplies and 2 adds.<br>
         * [Rationals are the CLDR implementation's native numeric form, to preserve precision.]
         */
        public static Rational convert(
                Rational amountRational, MeasureUnit sourceUnits, MeasureUnit targetUnits) {
            MeasureUnit reciprocalUnit = checkAndReverseIfNeeded(sourceUnits, targetUnits);
            Rational intermediate = sourceUnits.convertToBase(amountRational);
            if (reciprocalUnit != null) {
                intermediate = intermediate.reciprocal();
            }
            Rational result = targetUnits.convertFromBase(intermediate);
            return result;
        }

        // INTERNALS

        private static MeasureUnit checkAndReverseIfNeeded(MeasureUnit source, MeasureUnit target) {
            MeasureUnit reciprocalUnit = null;
            if (!source.isConvertibleTo(target)) {
                // check for unusual case, meter-per-second to second-per-meter
                reciprocalUnit = source.reciprocal();
                if (!reciprocalUnit.isConvertibleTo(target)) {
                    throw new IllegalArgumentException(
                            source.normalizedIdentifier
                                    + " is not convertible to "
                                    + target.baseIdentifier);
                }
            }
            return reciprocalUnit;
        }
    }

    public static void main(String[] args) {
        new TestConverterAPI().run(args);
    }

    static final LocalizedNumberFormatter nf =
            NumberFormatter.with()
                    .precision(Precision.maxSignificantDigits(10))
                    .locale(Locale.ENGLISH);

    public void testIds() {
        String[][] tests = {
            {"foot-per-minute", "foot-per-minute", "meter-per-second", "null"},
            {"foot-foot", "square-foot", "square-meter", "null"}, // normalized to use powers
            {"foot-and-inch", "foot-and-inch", "meter", "[foot, inch]"},
            {"inch-and-foot", "foot-and-inch", "meter", "[foot, inch]"}, // normalized to fix order
            {
                "foot-and-foot-and-inch", "foot-and-inch", "meter", "[foot, inch]"
            }, // normalized to remove duplicates
            {"xxx-foobar", "xxx-foobar", null, "null"}, // private use
            {"foot-and-meter", "meter-and-foot", "meter", "[meter, foot]"}, // weird but allowed

            // errors
            {"foot-and-foot", "«foot-and-foot» has ≤1 unit", "", ""},
            {"foot-and-pound", "«foot-and-pound»: foot & pound are not convertible", "", ""},
        };
        for (String[] test : tests) {
            MeasureUnit unitId = null;
            String actual = null;
            try {
                unitId = MeasureUnit.from(test[0]);
            } catch (UncheckedExecutionException e) {
                actual = e.getCause().getMessage();
            } catch (Exception e) {
                actual = e.getMessage();
            }
            if (actual != null) {
                assertEquals(Arrays.asList(test) + "", test[1], actual);
                continue;
            }
            assertEquals(
                    Arrays.asList(test) + " normalized", test[1], unitId.getNormalizedIdentifier());
            assertEquals(
                    Arrays.asList(test) + " baseIdentifier", test[2], unitId.getBaseIdentifier());
            assertEquals(
                    Arrays.asList(test) + " subunits",
                    test[3],
                    String.valueOf(unitId.getSubunits()));
        }
    }

    class DoubleConversionInfo {
        final double factor;
        final double offset;

        DoubleConversionInfo(ConversionInfo other) {
            factor = other.factor.doubleValue();
            offset = other.offset.doubleValue();
        }

        double convertToBase(double source) {
            return source * factor + offset;
        }

        double convertFromBase(double source) {
            return (source - offset) / factor;
        }
    }

    public void testSpeed() {
        // warmup
        double result = 0;
        for (int i = 0; i < 10; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result += UnitConverter2.convert(3.4, mu1, mu2);
        }

        Timer t = new Timer();

        final int iterations = 1_000_000;
        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result += UnitConverter2.convert(3.4, mu1, mu2);
        }
        t.stop();
        warnln(
                "CLDR rationals: "
                        + t.getNanoseconds() / (double) iterations
                        + " ns/iteration; iterations: "
                        + nf.format(iterations));

        MeasureUnit mu1a = MeasureUnit.from("foot-per-second");
        MeasureUnit mu2a = MeasureUnit.from("kilometer-per-hour");

        // simulate doubles
        DoubleConversionInfo dci1 = new DoubleConversionInfo(mu1a.getConversionInfo());
        DoubleConversionInfo dci2 = new DoubleConversionInfo(mu2a.getConversionInfo());

        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            ConversionInfo x1 = mu1.getConversionInfo();
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            ConversionInfo x2 = mu2.getConversionInfo();
            result += dci2.convertFromBase(dci1.convertToBase(3.4d));
        }
        t.stop();
        warnln(
                "Simulating doubles: "
                        + t.getNanoseconds() / (double) iterations
                        + " ns/iteration; iterations: "
                        + nf.format(iterations));
    }

    public void testConversions() {
        String[][] tests = {
            {"3", "foot-per-second", "kilometer-per-hour", "3.29184"},
            {"3", "foot-per-second", "hour-per-kilometer", "0.3037814718"},
            {"3", "newton", "pound-mile-per-square-minute", "14.79480106"},
            {"1.27", "meter", "foot-and-inch", "4;2"},
            {"-1.27", "meter", "foot-and-inch", "-4;-2"},
            {"4;2", "foot-and-inch", "meter", "1.27"},
            {"-4;-2", "foot-and-inch", "meter", "-1.27"},
            {"20;9", "foot-and-inch", "yard-and-foot", "6;2.75"},
            {"6;2.75", "yard-and-foot", "foot-and-inch", "20;9"},

            // errors
            {"3", "foot-per-second", "kilogram", "foot-per-second is not convertible to kilogram"},
        };
        int testNumber = 0;
        for (String[] test : tests) {
            ++testNumber;
            for (boolean isDouble : List.of(true, false)) {
                String actual = null;
                String expected = test[3];
                String title = null;
                try {
                    MeasureUnit source = MeasureUnit.from(test[1]);
                    MeasureUnit target = MeasureUnit.from(test[2]);
                    if (!source.isMixed() && !target.isMixed()) { // unmixed units
                        if (isDouble) {
                            title = " double";
                            // do unmixed units
                            double amount = Double.parseDouble(test[0]);
                            double result = UnitConverter2.convert(amount, source, target);
                            actual = nf.format(result).toString();
                        } else {
                            title = " BigDecimal";
                            BigDecimal amount = new BigDecimal(test[0]);
                            BigDecimal result = UnitConverter2.convert(amount, source, target);
                            actual = nf.format(result).toString();
                        }
                    } else { // do mixed units
                        if (isDouble) {
                            title = " double";
                            List<Double> amounts = listDoubleFrom(test[0]);
                            List<Double> result =
                                    UnitConverter2.convertDoubles(amounts, source, target);
                            actual = stringFromListNumber(result);
                        } else {
                            title = " BigDecimal";
                            List<BigDecimal> amount = listBigDecimalFrom(test[0]);
                            List<BigDecimal> result =
                                    UnitConverter2.convertBigDecimals(amount, source, target);
                            actual = stringFromListNumber(result);
                        }
                    }
                } catch (UncheckedExecutionException e) {
                    actual = e.getCause().getMessage();
                } catch (Exception e) {
                    actual = e.getMessage();
                }
                assertEquals(
                        testNumber + ") " + Arrays.asList(test).toString() + title,
                        expected,
                        actual);
            }
        }
    }

    private <N extends Number> String stringFromListNumber(List<N> result) {
        return result.stream().map(x -> nf.format(x)).collect(Collectors.joining(";"));
    }

    // Utilities

    private List<BigDecimal> listBigDecimalFrom(String string) {
        return Streams.stream(SPLIT_SEMI.split(string))
                .map(x -> new BigDecimal(x))
                .collect(Collectors.toList());
    }

    private List<Double> listDoubleFrom(String string) {
        return Streams.stream(SPLIT_SEMI.split(string))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    private static List<Rational> listDoubleToRational(List<Double> amounts) {
        return amounts.stream().map(d -> Rational.of(d.doubleValue())).collect(Collectors.toList());
    }

    private static List<Rational> listBigDecimalToRational(List<BigDecimal> amounts) {
        return amounts.stream().map(d -> Rational.of(d)).collect(Collectors.toList());
    }

    private static List<Double> doubleList(List<Rational> amounts) {
        return amounts.stream().map(r -> r.doubleValue()).collect(Collectors.toList());
    }

    private static List<BigDecimal> bigDecimalList(List<Rational> amounts) {
        return amounts.stream().map(r -> r.toBigDecimal()).collect(Collectors.toList());
    }
}
