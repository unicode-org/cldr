package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComparisonChain;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/**
 * Adds conversion info and a baseIdentifier to MeasureUnit, and supplies utility functions for
 * conversion. MixedUnits are supported by having a (normally null) subunits field.
 */
public class MeasureUnit2 implements Comparable<MeasureUnit2> {
    /**
     * Create a MeasureUnit from a unicode_unit-identifier. Internally there is a cache, so in the
     * normal case this should be fast.
     */
    public static MeasureUnit2 from(String unitId) {
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
    public boolean isConvertibleTo(MeasureUnit2 other) {
        return isDirectlyConvertibleTo(other)
                || (!isMixed() && isDirectlyConvertibleTo(other.reciprocal()));
    }

    public boolean isDirectlyConvertibleTo(MeasureUnit2 other) {
        // could be optimized slightly if we 'interned' the baseIdentifier
        return getBaseIdentifier().equals(other.getBaseIdentifier());
    }

    /** Get the reciprocal; eg, foot-per-second ⇒ second-per-foot */
    public MeasureUnit2 reciprocal() {
        try {
            return unmixedCache.get(reciprocalOf(normalizedIdentifier));
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public boolean isMixed() {
        return subunits != null;
    }

    public List<MeasureUnit2> getSubunits() {
        return subunits;
    }

    public String getNormalizedIdentifier() {
        return normalizedIdentifier;
    }

    public String getBaseIdentifier() {
        return baseIdentifier;
    }

    /** Returns just the string name, not the conversion info or base identifier */
    @Override
    public String toString() {
        return normalizedIdentifier;
    }

    @Override
    public int hashCode() {
        return normalizedIdentifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof MeasureUnit2)
                        && normalizedIdentifier.equals(((MeasureUnit2) obj).normalizedIdentifier);
    }

    @Override
    public int compareTo(MeasureUnit2 o) {
        return ComparisonChain.start()
                .compare(baseIdentifier, o.baseIdentifier)
                .compare(conversionInfo, o.conversionInfo)
                .compare(normalizedIdentifier, normalizedIdentifier) // break ties
                .result();
    }

    // Conversions

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds
     */
    public static double convert(double amount, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        boolean reverse = reverseNeededAndThrowNonConvertible(sourceUnit, targetUnit);
        double intermediate = sourceUnit.convertToBase(amount);
        if (reverse) {
            intermediate = 1d / intermediate;
        }
        double result = targetUnit.convertFromBase(intermediate);
        return result;
    }

    /**
     * Convert amounts with sourceUnits into result targetUnits. Use if either source or target is a
     * MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.) A negative
     * value in a list is represented by *every* value being negative; formatting would change that
     * as necessary.
     */
    public static List<Double> convertDoubles(
            List<Double> amounts, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        // for now, just call the Rational version
        return doubleList(convert(listDoubleToRational(amounts), sourceUnit, targetUnit));
    }

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds
     */
    public static BigDecimal convert(
            BigDecimal amount, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        boolean reverse = reverseNeededAndThrowNonConvertible(sourceUnit, targetUnit);
        BigDecimal intermediate = sourceUnit.convertToBase(amount);
        if (reverse) {
            intermediate = BigDecimal.ONE.divide(intermediate, MathContext.DECIMAL64);
        }
        BigDecimal result = targetUnit.convertFromBase(intermediate);
        return result;
    }

    /**
     * Convert amounts with sourceUnits into result targetUnits. Use if either source or target is a
     * MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.) A negative
     * value in a list is represented by *every* value being negative; formatting would change that
     * as necessary.
     */
    public static List<BigDecimal> convertBigDecimals(
            List<BigDecimal> amounts, MeasureUnit2 sourceUnits, MeasureUnit2 targetUnits) {
        // for now, just call the Rational version
        return bigDecimalList(convert(listBigDecimalToRational(amounts), sourceUnits, targetUnits));
    }

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds.<br>
     * [Rationals are the CLDR implementation's native numeric form, to preserve precision.]
     */
    public static Rational convert(
            Rational amountRational, MeasureUnit2 sourceUnits, MeasureUnit2 targetUnits) {
        boolean reverse = reverseNeededAndThrowNonConvertible(sourceUnits, targetUnits);
        Rational intermediate = sourceUnits.convertToBase(amountRational);
        if (reverse) {
            intermediate = intermediate.reciprocal();
        }
        Rational result = targetUnits.convertFromBase(intermediate);
        return result;
    }

    /**
     * Convert amounts with sourceUnits into result targetUnits. Use if either source or target is a
     * MixedMeasureUnit. (Can be used if both are MeasureUnits, but less efficient.)<br>
     * [Rationals are the CLDR implementation's native numeric form, to preserve precision.] A
     * negative value in a list is represented by *every* value being negative; formatting would
     * change that as necessary.
     */
    public static List<Rational> convert(
            List<Rational> amounts, MeasureUnit2 sourceUnits, MeasureUnit2 targetUnits) {
        throwNonConvertible(sourceUnits, targetUnits);
        return targetUnits.convertFromBaseToMixed(sourceUnits.convertToBase(amounts));
    }

    // INTERNALS

    static final UnitConverter unitConverter =
            CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

    private final String normalizedIdentifier;

    // Is null iff non mixed units
    private final List<MeasureUnit2> subunits;

    // If subunits ≠ null, identical to first subunit's value
    private final String baseIdentifier;

    // If subunits ≠ null, identical to first subunit's values
    // We only need this for the numeric values we support;
    // it saves on conversion performance

    private final ConversionInfo conversionInfo;
    private final DoubleConversionInfo doubleConversionInfo;
    private final BigDecimalConversionInfo bigDecimalConversionInfo;

    private final Rational convertToBase(Rational other) {
        return conversionInfo.convert(other);
    }

    private final Rational convertFromBase(Rational other) {
        return conversionInfo.convertBackwards(other);
    }

    private double convertToBase(double other) {
        return doubleConversionInfo.convertToBase(other);
    }

    private double convertFromBase(double other) {
        return doubleConversionInfo.convertFromBase(other);
    }

    private BigDecimal convertToBase(BigDecimal other) {
        return bigDecimalConversionInfo.convertToBase(other);
    }

    private BigDecimal convertFromBase(BigDecimal other) {
        return bigDecimalConversionInfo.convertFromBase(other);
    }

    // a negative value in a list is represented by *every* value being negative;
    // formatting  will change that as necessary

    private Rational convertToBase(List<Rational> amounts) {
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
            MeasureUnit2 unit = subunits.get(i);
            sum = sum.add(unit.convertToBase(amount));
        }
        return sum;
    }

    // a negative value in a list is represented by *every* value being negative;
    // formatting  will change that as necessary

    private List<Rational> convertFromBaseToMixed(Rational amount) {
        if (subunits == null) {
            return List.of(conversionInfo.convertBackwards(amount));
        }

        List<Rational> result = new ArrayList<>();
        int last = subunits.size() - 1;
        for (int i = 0; i <= last; ++i) {
            MeasureUnit2 unit = subunits.get(i);
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

    private static boolean reverseNeededAndThrowNonConvertible(
            MeasureUnit2 source, MeasureUnit2 target) {
        if (!source.isDirectlyConvertibleTo(target)) {
            // check for unusual case: eg, meter-per-second to second-per-meter
            MeasureUnit2 reciprocalUnit = source.reciprocal();
            throwNonConvertible(source, reciprocalUnit, target);
            return true;
        }
        return false;
    }

    private static void throwNonConvertible(MeasureUnit2 a, MeasureUnit2 b) {
        throwNonConvertible(a, a, b);
    }

    private static void throwNonConvertible(MeasureUnit2 original, MeasureUnit2 a, MeasureUnit2 b) {
        if (!a.isDirectlyConvertibleTo(b)) {
            throw new IllegalArgumentException(original + " and " + b + " are not convertible");
        }
    }

    static List<Rational> listDoubleToRational(List<Double> amounts) {
        return amounts.stream().map(d -> Rational.of(d.doubleValue())).collect(Collectors.toList());
    }

    static List<Rational> listBigDecimalToRational(List<BigDecimal> amounts) {
        return amounts.stream().map(d -> Rational.of(d)).collect(Collectors.toList());
    }

    static List<Double> doubleList(List<Rational> amounts) {
        return amounts.stream().map(r -> r.doubleValue()).collect(Collectors.toList());
    }

    static List<BigDecimal> bigDecimalList(List<Rational> amounts) {
        return amounts.stream().map(r -> r.toBigDecimal()).collect(Collectors.toList());
    }

    private static final LoadingCache<String, MeasureUnit2> unmixedCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .build(
                            new CacheLoader<String, MeasureUnit2>() {
                                @Override
                                public MeasureUnit2 load(String unitId) {
                                    Output<String> baseUnitOut = new Output<>();
                                    ConversionInfo ci =
                                            unitConverter.parseUnitId(unitId, baseUnitOut, false);
                                    String normalized =
                                            unitConverter.createUnitId(unitId).toString();
                                    String baseUnit =
                                            unitConverter
                                                    .createUnitId(baseUnitOut.value)
                                                    .resolve()
                                                    .toString();
                                    return new MeasureUnit2(normalized, ci, baseUnit, null);
                                }
                            });
    private static final LoadingCache<String, MeasureUnit2> mixedCache =
            CacheBuilder.newBuilder()
                    .maximumSize(50)
                    .build(
                            new CacheLoader<String, MeasureUnit2>() {
                                @Override
                                public MeasureUnit2 load(String unitId) {
                                    List<MeasureUnit2> parts = getParts(unitId);
                                    String normalized = Joiner.on("-and-").join(parts);
                                    MeasureUnit2 part0 = parts.get(0);
                                    return new MeasureUnit2(
                                            normalized,
                                            part0.conversionInfo,
                                            part0.baseIdentifier,
                                            List.copyOf(parts));
                                }
                            });

    /*
     * Note:  In CLDR, ConversionInfo contains:
     *    private final Rational factor;
     *    private final Rational offset;
     *    private String special;
     *    private boolean specialInverse; // only used with special
     *
     *    convert(amount) is normally return amount.multiply(factor).add(offset);
     */

    private MeasureUnit2(
            String normalizedIdentifier,
            ConversionInfo conversionInfo,
            String baseIdentifier,
            List<MeasureUnit2> subunits) {
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

    private static List<MeasureUnit2> getParts(String unitId) {
        // normalize by sorting the segments between -and- and dropping duplicates
        Set<MeasureUnit2> sortedParts = new TreeSet<>(Comparator.reverseOrder());
        Splitter.on("-and-").split(unitId).forEach(x -> sortedParts.add(MeasureUnit2.from(x)));
        List<MeasureUnit2> parts = new ArrayList<>(sortedParts);
        if (parts.size() < 2) {
            throw new IllegalArgumentException(
                    TestConverterAPI.JOIN_EMPTY.join("«", unitId, "» has ≤1 unit"));
        }

        // Verify that the units are all comparable
        MeasureUnit2 lastPart = null;
        for (int i = 0; i < parts.size(); ++i) {
            MeasureUnit2 part = parts.get(i);
            if (lastPart != null) {
                if (!part.isConvertibleTo(lastPart)) {
                    throw new IllegalArgumentException(
                            TestConverterAPI.JOIN_EMPTY.join(
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
            throw new IllegalArgumentException(
                    TestConverterAPI.JOIN_EMPTY.join("«", unitId, "» has no units"));
        }

        return parts;
    }

    private static class DoubleConversionInfo {
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

    private static class BigDecimalConversionInfo {
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
}
