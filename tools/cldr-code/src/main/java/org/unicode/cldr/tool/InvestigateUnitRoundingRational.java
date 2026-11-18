package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.math.BigIntegerMath;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/**
 * Rational-based version of InvestigateUnitRounding. Replaces double-precision floating point
 * arithmetic with exact Rational arithmetic from org.unicode.cldr.util.Rational to prevent rounding
 * error accumulation during unit conversion and bounds calculations.
 */
public class InvestigateUnitRoundingRational {

    private static final boolean EXPERIMENTAL_ROUND = false;
    private static final Rational MAX_LONG_FROM_RATIONAL =
            Rational.of(BigInteger.valueOf(1L << 53));

    private static final UnitConverter UNIT_CONVERTER =
            SupplementalDataInfo.getInstance().getUnitConverter();

    /**
     * Hack because you can't get a Precision from a NumberFormatter, nor can you get the various
     * settings (like minimumSignificantDigits) from a Precision, nor can you get
     * DecimalFormatProperties from a NumberFormatter!
     */
    public static class HackLocalizedNumberFormatter {

        private final int minimumSignificantDigits;
        private final int maximumSignificantDigits;
        private final Locale locale;
        public final LocalizedNumberFormatter lnf;

        public int getMinimumSignificantDigits() {
            return minimumSignificantDigits;
        }

        public int getMaximumSignificantDigits() {
            return maximumSignificantDigits;
        }

        public Locale getLocale() {
            return locale;
        }

        public LocalizedNumberFormatter getLnf() {
            return lnf;
        }

        public HackLocalizedNumberFormatter(
                LocalizedNumberFormatter lnf,
                int minimumSignificantDigits,
                int maximumSignificantDigits,
                Locale locale) {
            if (minimumSignificantDigits < 1
                    || minimumSignificantDigits > maximumSignificantDigits) {
                throw new UnitException(
                        "Bad sig digits: "
                                + formatPrecision(
                                        minimumSignificantDigits, maximumSignificantDigits));
            }
            this.minimumSignificantDigits = minimumSignificantDigits;
            this.maximumSignificantDigits = maximumSignificantDigits;
            this.locale = locale;
            Precision precision =
                    Precision.minMaxSignificantDigits(
                            minimumSignificantDigits, maximumSignificantDigits);
            this.lnf = lnf.withoutLocale().locale(locale).precision(precision);
        }

        public HackLocalizedNumberFormatter(LocalizedNumberFormatter lnf, Locale locale) {
            DecimalFormatProperties dfp = new DecimalFormatProperties();
            String skeleton = lnf.toSkeleton();
            PatternStringParser.parseToExistingProperties(skeleton, dfp);
            this.minimumSignificantDigits = dfp.getMinimumSignificantDigits();
            this.maximumSignificantDigits = dfp.getMaximumSignificantDigits();
            this.locale = locale;
            this.lnf = lnf;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HackLocalizedNumberFormatter)) return false;
            HackLocalizedNumberFormatter that = (HackLocalizedNumberFormatter) obj;
            return minimumSignificantDigits == that.minimumSignificantDigits
                    && maximumSignificantDigits == that.maximumSignificantDigits;
        }

        @Override
        public int hashCode() {
            return Objects.hash(minimumSignificantDigits, maximumSignificantDigits);
        }

        @Override
        public String toString() {
            return formatPrecision(minimumSignificantDigits, maximumSignificantDigits);
        }

        public LocalizedNumberFormatter precision(FractionPrecision precision) {
            return lnf.precision(precision);
        }

        public HackLocalizedNumberFormatter precision(int minSignificant, int maxSignificant) {
            return new HackLocalizedNumberFormatter(lnf, minSignificant, maxSignificant, locale);
        }

        /** Locale-sensitive formatting */
        public String formatToString(Rational finalValue) {
            if (finalValue.denominator == BigInteger.ZERO) {
                return finalValue == Rational.INFINITY
                        ? "INF"
                        : finalValue == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
            }
            return lnf.format(finalValue.doubleValue()).toString();
        }
    }

    /**
     * A Rational value and its exact upper/lower bounds, with lower < value < upper. Replaces
     * BoundDouble to eliminate IEEE 754 precision loss during conversion operations.
     */
    public static class BoundRational {

        private int negative;
        private Rational lowerBound;
        private Rational value;
        private Rational upperBound;

        public enum PrecisionType {
            significant,
            fraction;

            public static PrecisionType fromOrdinal(Number ordinal) {
                return values()[ordinal.intValue()];
            }
        }

        private BoundRational(
                int negative, Rational lowerBound, Rational value, Rational upperBound) {
            if (negative < -1
                    || negative > 1
                    || lowerBound.compareTo(value) >= 0
                    || value.compareTo(upperBound) >= 0) {
                throw new UnitException("Bad value");
            }
            this.negative = negative;
            this.lowerBound = lowerBound;
            this.value = value;
            this.upperBound = upperBound;
        }

        /** Significant digits relative to first digit. */
        public static BoundRational fromDigits(Rational source, PrecisionType type, int digits) {
            if (source == null
                    || source.equals(Rational.ZERO)
                    || source.equals(Rational.NaN)
                    || source.equals(Rational.INFINITY)
                    || source.equals(Rational.NEGATIVE_INFINITY)) {
                return null;
            }

            int negative;
            if (source.compareTo(Rational.ZERO) > 0) {
                negative = 1;
            } else if (source.compareTo(Rational.ZERO) < 0) {
                negative = -1;
                source = source.abs();
            } else {
                negative = 0;
            }

            if (type == PrecisionType.fraction) {
                throw new IllegalArgumentException("do later");
            }

            int firstDigit = firstDigit(source);
            Rational increment = Rational.pow10(afterLastDigit(digits, firstDigit));

            source = roundByIncrement(source, increment);

            // Exact delta = 0.5 / increment = 1 / (2 * increment)
            Rational delta = Rational.ONE.divide(Rational.TWO.multiply(increment));

            Rational lowerBound = source.subtract(delta);
            Rational upperBound = source.add(delta);

            return new BoundRational(negative, lowerBound, source, upperBound);
        }

        public void invert() {
            Rational temp = lowerBound;
            this.lowerBound = upperBound.reciprocal();
            this.value = value.reciprocal();
            this.upperBound = temp.reciprocal();
        }

        public void times(Rational conversionFactor) {
            this.value = value.multiply(conversionFactor);
            this.lowerBound = lowerBound.multiply(conversionFactor);
            this.upperBound = upperBound.multiply(conversionFactor);
        }

        public void divideBy(Rational conversionDivisor) {
            this.value = value.divide(conversionDivisor);
            this.lowerBound = lowerBound.divide(conversionDivisor);
            this.upperBound = upperBound.divide(conversionDivisor);
        }

        public boolean inTightBounds() {
            BigInteger lowerBoundCeiling = lowerBound.floor().add(BigInteger.ONE);
            BigInteger upperBoundFloor =
                    upperBound
                            .toBigDecimal()
                            .setScale(0, RoundingMode.CEILING)
                            .toBigInteger()
                            .subtract(BigInteger.ONE);
            return lowerBoundCeiling.compareTo(upperBoundFloor) < 0;
        }

        public int toSignificantDigits() {
            return getBestRationalScale(lowerBound, value, upperBound);
        }

        public long getFloorAndSubtract() {
            BigInteger floor = value.floor();
            Rational floorRat = Rational.of(floor);
            this.value = value.subtract(floorRat);
            this.lowerBound = lowerBound.subtract(floorRat);
            this.upperBound = upperBound.subtract(floorRat);
            return floor.longValueExact();
        }

        //        public BestLong getBestLongScale() {
        //            return InvestigateUnitRoundingRational.getBestLongScale(
        //                    lowerBound.doubleValue(), value.doubleValue(),
        // upperBound.doubleValue());
        //        }

        public Rational getUpperBound() {
            return upperBound;
        }

        public Rational getValue() {
            return value;
        }

        public Rational getLowerBound() {
            return lowerBound;
        }

        public int getNegative() {
            return negative;
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "lb: ", lowerBound, ", v: ", value, ", ub: ", upperBound, ", s: ", negative);
        }
    }

    /**
     * Code for converting from an unmixed unit to either an unmixed or mixed unit, and back,
     * maintaining exact Rational numbers throughout the pipeline.
     */
    public static class Converter {

        private static final boolean DOUBLE_CHECK = false;

        private final boolean invertSource;
        private final Rational[] conversionFactors;
        private final String sourceUnitId;
        private final String[] unitIds;
        private final boolean matchFormatted;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getSourceUnitId());
            if (invertSource) {
                sb.append(" recip");
            }
            if (!matchFormatted) {
                sb.append("(MS)");
            }
            Rational current = Rational.ONE;
            for (int i = 0; i < conversionFactors.length; ++i) {
                if (i != 0) {
                    sb.append(" rem");
                }
                Rational conversionFactor = conversionFactors[i];
                sb.append(" ÷ ").append(conversionFactor).append(" → ").append(unitIds[i]);
                current = current.multiply(conversionFactor);
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Converter)) return false;
            Converter that = (Converter) obj;
            return getSourceUnitId().equals(that.getSourceUnitId())
                    && invertSource == that.invertSource
                    && Arrays.equals(conversionFactors, that.conversionFactors)
                    && Arrays.equals(unitIds, that.unitIds);
        }

        @Override
        public int hashCode() {
            return getSourceUnitId().hashCode()
                    + (invertSource ? 31 * 31 * 31 : 0)
                    + (Arrays.hashCode(conversionFactors) * 31 + Arrays.hashCode(unitIds)) * 31;
        }

        public static Converter getConverter(
                final String sourceUnit, final String targetUnit, boolean matchFormatted) {
            List<Rational> conversions = new ArrayList<>();
            List<String> units = Splitter.on("-and-").splitToList(targetUnit);

            Output<String> baseUnitId = new Output<>();
            ConversionInfo info = UNIT_CONVERTER.parseUnitId(sourceUnit, baseUnitId, false);
            if (baseUnitId.value == null) {
                throw new UnitException("Unit not recognized: " + sourceUnit);
            }
            String normalizedSourceUnitId = UNIT_CONVERTER.getReducedUnit(baseUnitId.value);

            Rational lastFactor = null;
            String lastUnit = null;
            boolean invertSource = false;

            for (String unit : units) {
                info = UNIT_CONVERTER.parseUnitId(unit, baseUnitId, false);
                if (baseUnitId.value == null) {
                    throw new UnitException("Unit not recognized: " + unit);
                }
                String normalizedBaseUnitID = UNIT_CONVERTER.getReducedUnit(baseUnitId.value);
                if (!normalizedBaseUnitID.equals(normalizedSourceUnitId)) {
                    if (lastFactor == null) {
                        UNIT_CONVERTER.canConvertBetween(lastUnit);
                        String reciprocalUnit = UNIT_CONVERTER.reciprocalOf(normalizedBaseUnitID);
                        if (reciprocalUnit.equals(normalizedSourceUnitId)) {
                            invertSource = true;
                        }
                    }
                    if (!invertSource) {
                        throw new UnitException(
                                "Mismatched baseUnitIds for "
                                        + sourceUnit
                                        + "&"
                                        + unit
                                        + ": "
                                        + normalizedSourceUnitId
                                        + "≠"
                                        + normalizedBaseUnitID);
                    }
                }
                if (lastFactor == null) {
                    lastFactor = info.factor;
                    lastUnit = unit;
                } else if (info.factor.compareTo(lastFactor) >= 0) {
                    throw new UnitException(
                            "Bad mixed units: "
                                    + lastUnit
                                    + " ("
                                    + lastFactor
                                    + ")"
                                    + " <= "
                                    + unit
                                    + " ("
                                    + info.factor
                                    + ")");
                }
            }

            List<String> targetUnits = new ArrayList<>();
            targetUnits.add(sourceUnit);
            targetUnits.addAll(units);

            for (int i = 1; i < targetUnits.size(); ++i) {
                Rational conversion =
                        UNIT_CONVERTER.convert(
                                Rational.ONE, targetUnits.get(i - 1), targetUnits.get(i), false);
                conversions.add(conversion); // Keep conversion factor exact as Rational
            }

            return new Converter(sourceUnit, invertSource, conversions, units, matchFormatted);
        }

        private Converter(
                String sourceUnit,
                boolean invertSource,
                List<Rational> conversions,
                List<String> unitsIds,
                boolean matchFormatted) {
            this.sourceUnitId = sourceUnit;
            this.invertSource = invertSource;
            this.conversionFactors = conversions.toArray(new Rational[0]);
            this.unitIds = unitsIds.toArray(new String[0]);
            this.matchFormatted = matchFormatted;
        }

        /** Convert formatted numeric strings back to the source unit as a Rational */
        public Rational convertBack(List<String> numbers) {
            Rational result = Rational.ZERO;
            for (int i = numbers.size() - 1; i >= 0; --i) {
                Rational parsed = Rational.of(numbers.get(i));
                Rational conversionFactor = conversionFactors[i];
                result = result.add(parsed).divide(conversionFactor);
            }
            return invertSource ? result.reciprocal() : result;
        }

        /** Convert the converted value(s) back to the source unit */
        public Rational convertBackRaw(Converted converted, boolean rounding) {
            Rational conversionFactor = conversionFactors[converted.leadingCount];
            Rational finalValue =
                    !rounding
                            ? converted.finalValue
                            : roundToSignificant(converted.finalValue, converted.maxSignificant);
            Rational result = finalValue.divide(conversionFactor);
            for (int i = converted.leadingCount - 1; i >= 0; --i) {
                conversionFactor = conversionFactors[i];
                result =
                        result.add(Rational.of(converted.leadingValues[i]))
                                .divide(conversionFactor);
            }
            return result;
        }

        /** Convert a Rational value, disregarding precision. */
        public Converted convertRaw(Rational source) {
            Converted result = new Converted();
            Rational converted = source;
            int last = conversionFactors.length - 1;
            for (int i = 0; i <= last; ++i) {
                converted = converted.multiply(conversionFactors[i]);
                if (i == last) {
                    result.setRational(converted, 14, 14);
                    break;
                } else {
                    BigInteger floor = converted.floor();
                    result.addLong(floor.longValueExact());
                    converted = converted.subtract(Rational.of(floor));
                }
            }
            return result;
        }

        /** Version of convert with a simpler code flow using Rational */
        public Converted convert(final Rational source, HackLocalizedNumberFormatter rp) {
            Converted result = new Converted();
            BoundRational boundedRational =
                    BoundRational.fromDigits(
                            source,
                            BoundRational.PrecisionType.significant,
                            rp.getMaximumSignificantDigits());
            if (boundedRational == null) {
                result.setRational(
                        invertSource ? source.reciprocal() : source,
                        rp.getMinimumSignificantDigits(),
                        rp.getMaximumSignificantDigits());
                return result;
            }
            return convert(
                    boundedRational,
                    rp.getMaximumSignificantDigits() - rp.getMinimumSignificantDigits(),
                    result);
        }

        /** Convert a BoundRational, storing the result in 'result' */
        public Converted convert(
                BoundRational boundedRational, int deltaMaxToMin, Converted result) {
            Rational originalLowerBound = boundedRational.getLowerBound();
            Rational originalUpperBound = boundedRational.getUpperBound();

            if (invertSource) {
                boundedRational.invert();
            }

            int last = conversionFactors.length - 1;
            for (int i = 0; i <= last; ++i) {
                Rational conversionFactor = conversionFactors[i];
                boundedRational.times(conversionFactor);
                if (i == last
                        || boundedRational.getValue().compareTo(MAX_LONG_FROM_RATIONAL) > 0
                        || boundedRational.inTightBounds()) {
                    int sigDigits = boundedRational.toSignificantDigits();
                    result.setRational(
                            boundedRational.getValue(), sigDigits - deltaMaxToMin, sigDigits);
                    if (DOUBLE_CHECK) {
                        Rational trial = convertBackRaw(result, true);
                        if (trial.compareTo(originalLowerBound) <= 0
                                || trial.compareTo(originalUpperBound) >= 0) {
                            result.incrementSignificantDigits();
                        }
                    }
                    break;
                } else {
                    result.addLong(boundedRational.getFloorAndSubtract());
                }
            }

            if (boundedRational.getNegative() < 0) {
                result.negate();
            }
            return result;
        }

        public String formatWithUnits(
                Converted rountResult, HackLocalizedNumberFormatter sourceNf) {
            List<String> numberStrings = formattedNumbers(rountResult, sourceNf);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < numberStrings.size(); ++i) {
                String numberString = numberStrings.get(i);
                if (numberString.equals("0")) {
                    if (result.length() != 0 || i != numberStrings.size() - 1) {
                        continue;
                    }
                }
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(numberString);
                result.append(" ");
                result.append(unitIds[i]);
            }
            return result.toString();
        }

        public List<String> formattedNumbers(
                Converted rountResult, HackLocalizedNumberFormatter sourceNf) {
            List<String> result = new ArrayList<>();
            if (rountResult.leadingCount != 0) {
                LocalizedNumberFormatter dfInteger = sourceNf.precision(Precision.integer());
                for (int i = 0; i < rountResult.leadingCount; ++i) {
                    result.add(dfInteger.format(rountResult.leadingValues[i]).toString());
                }
            }
            HackLocalizedNumberFormatter dfFinal =
                    sourceNf.precision(rountResult.maxSignificant, rountResult.maxSignificant);
            result.add(dfFinal.formatToString(rountResult.finalValue));
            return result;
        }

        public String getSourceUnitId() {
            return sourceUnitId;
        }

        /** Container for the results of a Rational conversion. */
        public static class Converted {

            private static final int MAX_MIXED_UNITS = 4;

            long[] leadingValues = null;
            int leadingCount = 0;
            Rational finalValue = Rational.ZERO;
            int maxSignificant = Integer.MIN_VALUE;
            int minSignificant = Integer.MIN_VALUE;

            public void setRational(Rational value, int minSig, int maxSig) {
                this.finalValue = value;
                this.minSignificant = minSig < 1 ? 1 : minSig;
                this.maxSignificant = maxSig < minSig ? minSig : maxSig;
            }

            public void incrementSignificantDigits() {
                this.minSignificant++;
                this.maxSignificant++;
            }

            public void addLong(long source) {
                if (leadingValues == null) {
                    leadingValues = new long[MAX_MIXED_UNITS];
                }
                leadingValues[leadingCount++] = source;
            }

            public void negate() {
                if (leadingValues != null) {
                    leadingValues[0] = -leadingValues[0];
                } else if (finalValue != null) {
                    finalValue = finalValue.negate();
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < leadingCount; ++i) {
                    if (sb.length() != 0) {
                        sb.append(":");
                    }
                    sb.append(leadingValues[i]);
                }
                if (sb.length() != 0) {
                    sb.append(" : ");
                }
                sb.append(getPlainString(finalValue));
                sb.append(formatPrecision(minSignificant, maxSignificant));
                return sb.toString();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Converted)) return false;
                Converted other = (Converted) obj;
                return leadingCount == other.leadingCount
                        && Arrays.equals(leadingValues, other.leadingValues)
                        && Objects.equals(finalValue, other.finalValue)
                        && maxSignificant == other.maxSignificant
                        && minSignificant == other.minSignificant;
            }

            @Override
            public int hashCode() {
                return Objects.hash(
                        leadingCount, leadingValues, finalValue, maxSignificant, minSignificant);
            }
        }
    }

    public static class UnitException extends RuntimeException {

        private static final long serialVersionUID = 302617249795260431L;

        public UnitException(String message) {
            super(message);
        }
    }

    // UTILITIES for Rational conversion

    public static Rational roundToSignificant(Rational source, int significantDigits) {
        int firstDigit = firstDigit(source);
        Rational increment = Rational.pow10(afterLastDigit(significantDigits, firstDigit));
        return roundByIncrement(source, increment);
    }

    private static int getBestRationalScale(
            Rational lowerBound, Rational value, Rational upperBound) {
        Rational lowerDiff = value.subtract(lowerBound);
        Rational upperDiff = upperBound.subtract(value);

        if (lowerDiff.compareTo(Rational.ZERO) <= 0 || upperDiff.compareTo(Rational.ZERO) <= 0) {
            throw new UnitException("bounds problem");
        }

        int valueFirstSigDigitPlace = firstDigit(value);
        int minFirstSigDigitPlace = firstDigit(lowerDiff);
        if (!lowerDiff.equals(upperDiff)) {
            int upperFirstSigDigitPlace = firstDigit(upperDiff);
            minFirstSigDigitPlace = Math.min(minFirstSigDigitPlace, upperFirstSigDigitPlace);
        }

        int result = valueFirstSigDigitPlace - minFirstSigDigitPlace + 1;
        return Math.max(result, 1);
    }

    private static int afterLastDigit(int significantDigits, int firstDigit) {
        return significantDigits - firstDigit;
    }

    private static Rational roundByIncrement(Rational source, Rational increment) {
        return source.roundByIncrement(increment);
    }

    private static int firstDigit(Rational value) {
        if (value == null || value.equals(Rational.ZERO) || !value.isFinite()) return 0;

        BigInteger num = value.numerator.abs();
        BigInteger den = value.denominator;

        // 1. Calculate floor(log10) for numerator and denominator independently
        int logNum = BigIntegerMath.log10(num, RoundingMode.FLOOR);
        int logDen = BigIntegerMath.log10(den, RoundingMode.FLOOR);
        int logDiff = logNum - logDen;

        // 2. Adjust boundary if (num / 10^logNum) < (den / 10^logDen)
        BigInteger lhs = num.multiply(BigInteger.TEN.pow(logDen));
        BigInteger rhs = den.multiply(BigInteger.TEN.pow(logNum));
        if (lhs.compareTo(rhs) < 0) {
            logDiff--;
        }
        return logDiff + 1;
    }

    public static String getPlainString(Rational sourceAmount) {
        if (sourceAmount == null) return null;
        if (sourceAmount.denominator == BigInteger.ZERO) {
            return sourceAmount == Rational.INFINITY
                    ? "INF"
                    : sourceAmount == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
        }
        return sourceAmount.toString();
    }

    public static String getDecimalString(Rational sourceAmount) {
        if (sourceAmount == null) return null;
        if (sourceAmount.denominator == BigInteger.ZERO) {
            return sourceAmount == Rational.INFINITY
                    ? "INF"
                    : sourceAmount == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
        }
        return new BigDecimal(sourceAmount.numerator)
                .divide(new BigDecimal(sourceAmount.denominator), MathContext.DECIMAL64)
                .toPlainString();
    }

    public static String formatPrecision(
            int minimumSignificantDigits, int maximumSignificantDigits) {
        return "⟨"
                + minimumSignificantDigits
                + (minimumSignificantDigits == maximumSignificantDigits
                        ? ""
                        : "‥" + maximumSignificantDigits)
                + "⟩";
    }
}
