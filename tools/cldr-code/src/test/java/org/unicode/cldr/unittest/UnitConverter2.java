package org.unicode.cldr.unittest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Rational;

/**
 * Prototype converter, using CLDR internal classes. Note: these could also be moved to MeasureUnit
 */
public class UnitConverter2 {

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds
     */
    public static double convert(double amount, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        return convert(Rational.of(amount), sourceUnit, targetUnit).doubleValue();
    }

    public static double convert2(double amount, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        boolean reverse = checkAndReverseIfNeeded(sourceUnit, targetUnit);
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
        return doubleList(convert(listDoubleToRational(amounts), sourceUnit, targetUnit));
    }

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds
     */
    public static BigDecimal convert(
            BigDecimal amount, MeasureUnit2 sourceUnits, MeasureUnit2 targetUnits) {
        return convert(Rational.of(amount), sourceUnits, targetUnits).toBigDecimal();
    }

    public static BigDecimal convert2(
            BigDecimal amount, MeasureUnit2 sourceUnit, MeasureUnit2 targetUnit) {
        boolean reverse = checkAndReverseIfNeeded(sourceUnit, targetUnit);
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
        return bigDecimalList(convert(listBigDecimalToRational(amounts), sourceUnits, targetUnits));
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
        return targetUnits.convertFromBaseToMixed(sourceUnits.convertToBase(amounts));
    }

    /**
     * Convert amount sourceUnit into result targetUnits. Only works for non-mixed units. Normal
     * conversion cost is 2 multiplies and 2 adds.<br>
     * [Rationals are the CLDR implementation's native numeric form, to preserve precision.]
     */
    public static Rational convert(
            Rational amountRational, MeasureUnit2 sourceUnits, MeasureUnit2 targetUnits) {
        boolean reverse = checkAndReverseIfNeeded(sourceUnits, targetUnits);
        Rational intermediate = sourceUnits.convertToBase(amountRational);
        if (reverse) {
            intermediate = intermediate.reciprocal();
        }
        Rational result = targetUnits.convertFromBase(intermediate);
        return result;
    }

    // INTERNALS

    private static boolean checkAndReverseIfNeeded(MeasureUnit2 source, MeasureUnit2 target) {
        if (!source.isConvertibleTo(target)) {
            // check for unusual case: eg, meter-per-second to second-per-meter
            MeasureUnit2 reciprocalUnit = source.reciprocal();
            if (!reciprocalUnit.isConvertibleTo(target)) {
                throw new IllegalArgumentException(
                        source.getNormalizedIdentifier()
                                + " is not convertible to "
                                + target.baseIdentifier);
            }
            return true;
        }
        return false;
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
}
