package org.unicode.cldr.unittest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Output;

public class TestConverterAPI extends TestFmwk {

    private static final Joiner JOIN_SEMI = Joiner.on(';');
    private static final Splitter SPLIT_SEMI = Splitter.on(';');

    public static interface AbstractMeasureUnit {
        /** Can be converted to other unit */
        public boolean isConvertibleTo(AbstractMeasureUnit other);

        public String getNormalizedIdentifier();

        public String getBaseIdentifier();
    }

    /**
     * Adds conversion info and a baseIdentifier to MeasureUnit, and supplies utility functions for
     * conversion.
     */
    public static class MeasureUnit implements AbstractMeasureUnit {

        /**
         * Create a MeasureUnit from a unicode_unit-identifier. Internally there is a cache, so in
         * the normal case this should be fast.
         */
        public static AbstractMeasureUnit from(String unitId) {
            try {
                if (unitId.contains("-and-")) {
                    return MixedMeasureUnit.from(unitId);
                }
                return cache.get(unitId);
            } catch (ExecutionException e) {
                throw (RuntimeException) e.getCause();
            }
        }

        /** Can be converted to other unit */
        @Override
        public boolean isConvertibleTo(AbstractMeasureUnit other) {
            // could be optimized slightly if we 'interned' the baseIdentifier
            return getBaseIdentifier().equals(other.getBaseIdentifier());
        }

        /** Get the reciprocal; eg, foot-per-second ⇒ second-per-foot */
        public MeasureUnit reciprocal() {
            try {
                return cache.get(reciprocalOf(normalizedIdentifier));
            } catch (ExecutionException e) {
                throw (RuntimeException) e.getCause();
            }
        }

        /** Returns just the string name, not the conversion info or base identifier */
        @Override
        public String toString() {
            return normalizedIdentifier;
        }

        @Override
        public String getNormalizedIdentifier() {
            return normalizedIdentifier;
        }

        public ConversionInfo getConversionInfo() {
            return conversionInfo;
        }

        @Override
        public String getBaseIdentifier() {
            return baseIdentifier;
        }

        public final Rational convertToBase(Rational other) {
            return conversionInfo.convert(other);
        }

        public final Rational convertFromBase(Rational other) {
            return conversionInfo.convertBackwards(other);
        }

        // INTERNALS

        private static final UnitConverter unitConverter =
                CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();
        private static final LoadingCache<String, MeasureUnit> cache =
                CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .build(
                                new CacheLoader<String, MeasureUnit>() {
                                    @Override
                                    public MeasureUnit load(String unitId) {
                                        // normalize later
                                        Output<String> baseUnitOut = new Output<>();
                                        ConversionInfo ci =
                                                unitConverter.parseUnitId(
                                                        unitId, baseUnitOut, false);
                                        return new MeasureUnit(unitId, ci, baseUnitOut.value);
                                    }
                                });

        protected final String normalizedIdentifier;
        protected final ConversionInfo conversionInfo;
        protected final String baseIdentifier;

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
                String normalizedIdentifier, ConversionInfo conversionInfo, String baseIdentifier) {
            if (conversionInfo == null) {
                throw new IllegalArgumentException(
                        "«" + normalizedIdentifier + "»: Illegal unicode unit identifier");
            }
            this.normalizedIdentifier = normalizedIdentifier;
            this.conversionInfo = conversionInfo;
            this.baseIdentifier = baseIdentifier;
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
            return conversionInfo.compareTo(other.conversionInfo) < 0;
        }
    }

    private static class MixedMeasureUnit implements AbstractMeasureUnit {
        // not caching for now
        public static MixedMeasureUnit from(String unitId) {
            List<MeasureUnit> parts = new ArrayList<>();
            Splitter.on("-and-")
                    .split(unitId)
                    .forEach(x -> parts.add((MeasureUnit) MeasureUnit.from(x)));

            // Verify that the units are in decreasing order, and all comparable
            MeasureUnit lastPart = null;
            for (int i = 0; i < parts.size(); ++i) {
                MeasureUnit part = parts.get(i);
                if (lastPart != null) {
                    if (!part.isConvertibleTo(lastPart)) {
                        throw new IllegalArgumentException(
                                "«"
                                        + unitId
                                        + "»: "
                                        + lastPart
                                        + " & "
                                        + part
                                        + " are not convertible");
                    }
                    if (!part.isSmallerThan(lastPart)) {
                        throw new IllegalArgumentException(
                                "«" + unitId + "»: " + lastPart + " is not smaller than " + part);
                    }
                }
                lastPart = part;
            }
            if (lastPart == null) {
                throw new IllegalArgumentException("«" + unitId + "» has no units");
            }

            return new MixedMeasureUnit(unitId, parts);
        }

        public static MixedMeasureUnit from(MeasureUnit unit) {
            return new MixedMeasureUnit(unit.getNormalizedIdentifier(), ImmutableList.of(unit));
        }

        @Override
        public boolean isConvertibleTo(AbstractMeasureUnit other) {
            return getBaseIdentifier().equals(other.getBaseIdentifier());
        }

        private final String normalizedIdentifier;
        private final List<MeasureUnit> subunits;

        @Override
        public String getNormalizedIdentifier() {
            return normalizedIdentifier;
        }

        @Override
        public String getBaseIdentifier() {
            return subunits.get(0).baseIdentifier;
        }

        public Rational convertToBase(List<Rational> amounts) {
            // a negative value in a list is represented by *every* value being negative
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

        public List<Rational> convertFromBase(Rational amount) {
            // By convention, only the first in the list is negative, eg -3 foot 2 inches
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

        /** Returns just the string name, not the conversion info or base identifier */
        @Override
        public String toString() {
            return normalizedIdentifier;
        }

        private MixedMeasureUnit(String normalizedIdentifier, List<MeasureUnit> subunits) {
            this.normalizedIdentifier = normalizedIdentifier;
            this.subunits = ImmutableList.copyOf(subunits);
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
         * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
         * conversion cost is 2 multiplies and 2 adds
         */
        public static BigDecimal convert(
                BigDecimal amount, MeasureUnit sourceUnits, MeasureUnit targetUnits) {
            return convert(Rational.of(amount), sourceUnits, targetUnits).toBigDecimal();
        }

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

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)
         */
        public static List<Double> convertDoubles(
                List<Double> amounts, MixedMeasureUnit sourceUnit, MixedMeasureUnit targetUnit) {
            return doubleList(convert(listDoubleToRational(amounts), sourceUnit, targetUnit));
        }

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)
         */
        public static List<BigDecimal> convertBigDecimals(
                List<BigDecimal> amounts,
                MixedMeasureUnit sourceUnits,
                MixedMeasureUnit targetUnits) {
            return bigDecimalList(
                    convert(listBigDecimalToRational(amounts), sourceUnits, targetUnits));
        }

        /**
         * Convert amounts with sourceUnits into result targetUnits. Use if either source or target
         * is a MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)
         */
        public static List<Rational> convert(
                List<Rational> amounts,
                MixedMeasureUnit sourceUnits,
                MixedMeasureUnit targetUnits) {
            return targetUnits.convertFromBase(sourceUnits.convertToBase(amounts));
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

    public void testBasic() {
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

            {"1", "foobar", "kilogram", "«foobar»: Illegal unicode unit identifier"},
            {"3", "foot-per-second", "kilogram", "foot-per-second is not convertible to kilogram"},
            {
                "1;2",
                "foot-and-pound",
                "kilogram",
                "«foot-and-pound»: foot & pound are not convertible"
            },
            {"1;2", "inch-and-foot", "kilogram", "«inch-and-foot»: inch is not smaller than foot"},
        };
        int testNumber = 0;
        for (String[] test : tests) {
            ++testNumber;
            for (boolean isDouble : List.of(true, false)) {
                String actual = null;
                String expected = test[3];
                String title = null;
                try {
                    if (!test[0].contains(";") && !test[3].contains(";")) { // unmixed units
                        if (isDouble) {
                            title = " double";
                            // do unmixed units
                            double amount = Double.parseDouble(test[0]);
                            MeasureUnit source = (MeasureUnit) MeasureUnit.from(test[1]);
                            MeasureUnit target = (MeasureUnit) MeasureUnit.from(test[2]);
                            double result = UnitConverter2.convert(amount, source, target);
                            actual = nf.format(result).toString();
                        } else {
                            title = " BigDecimal";
                            BigDecimal amount = new BigDecimal(test[0]);
                            MeasureUnit source = (MeasureUnit) MeasureUnit.from(test[1]);
                            MeasureUnit target = (MeasureUnit) MeasureUnit.from(test[2]);
                            BigDecimal result = UnitConverter2.convert(amount, source, target);
                            actual = nf.format(result).toString(); // downgrade to double for
                            // testing
                        }
                    } else { // do mixed units
                        if (isDouble) {
                            title = " double";
                            List<Double> amounts = listDoubleFrom(test[0]);
                            MixedMeasureUnit source = MixedMeasureUnit.from(test[1]);
                            MixedMeasureUnit target = MixedMeasureUnit.from(test[2]);
                            List<Double> result =
                                    UnitConverter2.convertDoubles(amounts, source, target);
                            actual = stringFromList(result);
                        } else {
                            title = " BigDecimal";
                            List<BigDecimal> amount = listBigDecimalFrom(test[0]);
                            MixedMeasureUnit source = MixedMeasureUnit.from(test[1]);
                            MixedMeasureUnit target = MixedMeasureUnit.from(test[2]);
                            List<BigDecimal> result =
                                    UnitConverter2.convertBigDecimals(amount, source, target);
                            actual = stringFromList(result);
                            // testing
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

    private <N extends Number> String stringFromList(List<N> result) {
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
