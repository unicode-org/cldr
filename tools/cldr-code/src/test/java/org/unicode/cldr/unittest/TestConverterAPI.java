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
import java.math.MathContext;
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

        // NOTE there are extra methods, etc for testing

        public final Rational convertToBase(Rational other) {
            return conversionInfo.convert(other);
        }

        public final Rational convertFromBase(Rational other) {
            return conversionInfo.convertBackwards(other);
        }

        public final double convertToBase(double other) {
            return doubleConversionInfo.convertToBase(other);
        }

        public final double convertFromBase(double other) {
            return doubleConversionInfo.convertFromBase(other);
        }

        public final BigDecimal convertToBase(BigDecimal other) {
            return bigDecimalConversionInfo.convertToBase(other);
        }

        public final BigDecimal convertFromBase(BigDecimal other) {
            return bigDecimalConversionInfo.convertFromBase(other);
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

        private final DoubleConversionInfo doubleConversionInfo;
        private final BigDecimalConversionInfo bigDecimalConversionInfo;

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
            doubleConversionInfo = new DoubleConversionInfo(conversionInfo);
            bigDecimalConversionInfo = new BigDecimalConversionInfo(conversionInfo);
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

    static class DoubleConversionInfo {
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

    static class BigDecimalConversionInfo {
        final BigDecimal factor;
        final BigDecimal offset;

        BigDecimalConversionInfo(ConversionInfo other) {
            factor = other.factor.toBigDecimal();
            offset = other.offset.toBigDecimal();
        }

        BigDecimal convertToBase(BigDecimal source) {
            return source.multiply(factor).add(offset);
        }

        BigDecimal convertFromBase(BigDecimal source) {
            return source.subtract(offset).divide(factor, MathContext.DECIMAL64);
        }
    }

    /**
     * Prototype converter, using CLDR internal classes. Note: these could also be moved to
     * MeasureUnit
     */
    public static class UnitConverter2 {

        /**
         * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
         * conversion cost is 2 multiplies and 2 adds
         */
        public static double convert(
                double amount, MeasureUnit sourceUnit, MeasureUnit targetUnit) {
            return convert(Rational.of(amount), sourceUnit, targetUnit).doubleValue();
        }

        public static double convert2(
                double amount, MeasureUnit sourceUnit, MeasureUnit targetUnit) {
            boolean reverse = checkAndReverseIfNeeded(sourceUnit, targetUnit);
            double intermediate = sourceUnit.convertToBase(amount);
            if (reverse) {
                intermediate = 1d / intermediate;
            }
            double result = targetUnit.convertFromBase(intermediate);
            return result;
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

        public static BigDecimal convert2(
                BigDecimal amount, MeasureUnit sourceUnit, MeasureUnit targetUnit) {
            boolean reverse = checkAndReverseIfNeeded(sourceUnit, targetUnit);
            BigDecimal intermediate = sourceUnit.convertToBase(amount);
            if (reverse) {
                intermediate = BigDecimal.ONE.divide(intermediate, MathContext.DECIMAL64);
            }
            BigDecimal result = targetUnit.convertFromBase(intermediate);
            return result;
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
            boolean reverse = checkAndReverseIfNeeded(sourceUnits, targetUnits);
            Rational intermediate = sourceUnits.convertToBase(amountRational);
            if (reverse) {
                intermediate = intermediate.reciprocal();
            }
            Rational result = targetUnits.convertFromBase(intermediate);
            return result;
        }

        // INTERNALS

        private static boolean checkAndReverseIfNeeded(MeasureUnit source, MeasureUnit target) {
            if (!source.isConvertibleTo(target)) {
                // check for unusual case: eg, meter-per-second to second-per-meter
                MeasureUnit reciprocalUnit = source.reciprocal();
                if (!reciprocalUnit.isConvertibleTo(target)) {
                    throw new IllegalArgumentException(
                            source.normalizedIdentifier
                                    + " is not convertible to "
                                    + target.baseIdentifier);
                }
                return true;
            }
            return false;
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
            {"foot-and-meter", "meter-and-foot", "meter", "[meter, foot]"}, // weird but allowed

            // errors
            {"xxx-foobar", "xxx-foobar", null, null}, // private use, not quite working yet
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

    public void testSpeed() {
        // warmup
        boolean result = true;
        double amount = 3.4d;
        BigDecimal bd3_4 = BigDecimal.valueOf(amount);
        Rational r3_4 = Rational.of(amount);

        final int iterations = 10_000_000;
        warnln("iterations: " + nf.format(iterations));

        for (int i = 0; i < 1000; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result ^= 0d == UnitConverter2.convert(amount, mu1, mu2);
            result ^= BigDecimal.ZERO.equals(UnitConverter2.convert2(bd3_4, mu1, mu2));
            result ^= Rational.ZERO.equals(UnitConverter2.convert(r3_4, mu1, mu2));
        }

        Timer t = new Timer();

        // Doubles

        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result ^= 0d == UnitConverter2.convert2(amount, mu1, mu2);
        }
        t.stop();
        warnln("Doubles: " + t.getNanoseconds() / (double) iterations + " ns");

        // Big Decimals

        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result ^= BigDecimal.ZERO.equals(UnitConverter2.convert2(bd3_4, mu1, mu2));
        }
        t.stop();
        warnln("BigDecimal64: " + t.getNanoseconds() / (double) iterations + " ns");

        // Rationals

        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit mu1 = MeasureUnit.from("foot-per-second");
            MeasureUnit mu2 = MeasureUnit.from("kilometer-per-hour");
            result ^= Rational.ZERO.equals(UnitConverter2.convert(r3_4, mu1, mu2));
        }
        t.stop();
        warnln("CLDR rationals: " + t.getNanoseconds() / (double) iterations + " ns");
    }

    public void testConversions() {
        String[][] tests = {
            {"3", "foot", "meter", "0.9144"},
            {"0.9144", "meter", "foot", "3"},
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
                            double result = UnitConverter2.convert2(amount, source, target);
                            actual = nf.format(result).toString();
                        } else {
                            title = " BigDecimal";
                            BigDecimal amount = new BigDecimal(test[0]);
                            BigDecimal result = UnitConverter2.convert2(amount, source, target);
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
