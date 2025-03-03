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
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;

/**
 * Adds conversion info and a baseIdentifier to MeasureUnit, and supplies utility functions for
 * conversion. MixedUnits are supported by having a (normally null) subunits field.
 */
public class MeasureUnit2 implements Comparable<MeasureUnit2> {
    static final UnitConverter unitConverter =
            CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

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

    /** Returns just the string name, not the conversion info or base identifier */
    @Override
    public String toString() {
        return normalizedIdentifier;
    }

    // MOST of the following could be package private
    // or even private, if the UnitConverter2 static methods were moved here

    /** Can be converted to other unit */
    public boolean isConvertibleTo(MeasureUnit2 other) {
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

    public final Rational convertToBase(Rational other) {
        return conversionInfo.convert(other);
    }

    public final Rational convertFromBase(Rational other) {
        return conversionInfo.convertBackwards(other);
    }

    public double convertToBase(double other) {
        return doubleConversionInfo.convertToBase(other);
    }

    public double convertFromBase(double other) {
        return doubleConversionInfo.convertFromBase(other);
    }

    public BigDecimal convertToBase(BigDecimal other) {
        return bigDecimalConversionInfo.convertToBase(other);
    }

    public BigDecimal convertFromBase(BigDecimal other) {
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
            MeasureUnit2 unit = subunits.get(i);
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

    @Override
    public int compareTo(MeasureUnit2 o) {
        return ComparisonChain.start()
                .compare(baseIdentifier, o.baseIdentifier)
                .compare(conversionInfo, o.conversionInfo)
                .compare(normalizedIdentifier, normalizedIdentifier) // break ties
                .result();
    }

    // INTERNALS

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
                                    String normalized = normalize(unitId);
                                    return new MeasureUnit2(
                                            normalized, ci, baseUnitOut.value, null);
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

    private final String normalizedIdentifier;
    private final ConversionInfo
            conversionInfo; // if subunits ≠ null, identical to first subunit's value
    final String baseIdentifier; // if subunits ≠ null, identical to first subunit's value
    private final List<MeasureUnit2> subunits; // null except for mixed units

    private final DoubleConversionInfo doubleConversionInfo;
    private final BigDecimalConversionInfo bigDecimalConversionInfo;

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

    private boolean isSmallerThan(MeasureUnit2 other) {
        return conversionInfo == null
                ? other.conversionInfo != null
                : other.conversionInfo == null
                        ? false
                        : conversionInfo.compareTo(other.conversionInfo) < 0;
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

    private static String normalize(String unitId) {
        UnitId id = unitConverter.createUnitId(unitId);
        return id.toString();
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
