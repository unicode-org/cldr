package org.unicode.cldr.tool;

import com.google.common.primitives.Doubles;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.tool.InvestigateUnitRounding.Converter.BestLong;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/**
 * This code is for investigating how we can handle unit conversion rounding, including both unmixed
 * and mixed units. The code is not intended to be final production code, and is not optimized.
 */
public class InvestigateUnitRounding {
    private static final boolean EXPERIMENTAL_ROUND = false;
    private static final double MAX_LONG_FROM_DOUBLE = Math.pow(2, 53);

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
            // Painful extraction; we have to convert to a skeleton and back;
            // That's got to be a waste of cycles!
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

        public FormattedNumber format(double finalValue) {
            return lnf.format(finalValue);
        }
    }

    /**
     * A value and its upper/lower bounds, with the invariant that lower < value < upper.<br>
     * The value is the absolute value of the source, with a 'negative' flag set if source < 0.<br>
     * The bounds might not be symmetric.<br>
     * For example, 2 meter-per-second with 1 significant digit is 2±0.5 [1.5, 2, 2.5].<br>
     * But inverted to 0.5 second-per-meter, it is [0.4, 0.5, 0.˙6] <br>
     * (where ˙ marks the start of the repeating decimal 0.6666…).
     */
    public static class BoundDouble {
        private int negative;
        private double lowerBound;
        private double value;
        private double upperBound;

        public enum PrecisionType {
            significant,
            fraction;

            public static PrecisionType fromOrdinal(Number ordinal) {
                return values()[ordinal.intValue()];
            }
        };

        private BoundDouble(int negative, double lowerBound, double value, double upperBound) {
            if (negative < -1 || negative > 1 || lowerBound >= value || value >= upperBound) {
                throw new UnitException("Bad value");
            }
            this.negative = negative;
            this.lowerBound = lowerBound;
            this.value = value;
            this.upperBound = upperBound;
        }

        /**
         * Significant digits are relative to first digit, so 1230, 4 digits => 0.5;<br>
         * Fraction digits are relative to the decimal point, so 0 => 0.5.
         */
        public static BoundDouble fromDigits(double source, PrecisionType type, int digits) {
            if (!Double.isFinite(source) || source == 0) {
                return null;
            }
            int negative;
            double delta;
            if (source > 0) {
                negative = 1;
            } else if (source < 0) {
                negative = -1;
                source = -source;
            } else {
                negative = 0;
            }

            if (type == type.fraction) {
                throw new IllegalArgumentException("do later");
            }

            // Calculate how many decimal places we need to keep
            int firstDigit = firstDigit(source);
            double increment = getRoundingIncrement(digits, firstDigit);

            // Fix the source and upper/lowerbounds accordingly
            source = roundByIncrement(source, increment);
            delta = Math.nextDown(0.5 / increment); // bump the value down a bit because of double
            // rounding

            // Now we have the value, and delta, so we can get the bounds
            double lowerBound = source - delta;
            double upperBound = source + delta;
            return new BoundDouble(negative, lowerBound, source, upperBound);
        }

        public void invert() {
            double temp = lowerBound;
            this.lowerBound = 1 / upperBound;
            this.value = 1 / value;
            this.upperBound = 1 / temp;
        }

        public void times(double conversionFactor) {
            this.value = value * conversionFactor;
            this.lowerBound = lowerBound * conversionFactor;
            this.upperBound = upperBound * conversionFactor;
        }

        public void divideBy(double conversionDivisor) {
            this.value = value / conversionDivisor;
            this.lowerBound = lowerBound / conversionDivisor;
            this.upperBound = upperBound / conversionDivisor;
        }

        public boolean inTightBounds() {
            double lowerBoundCeiling = Math.floor(lowerBound) + 1;
            double upperBoundFloor = Math.ceil(upperBound) - 1;
            return lowerBoundCeiling < upperBoundFloor;
        }

        /**
         * Gets corresponding decimal digits
         *
         * @return first
         */
        public int toSignificantDigits() {
            return getBestDoubleScale(lowerBound, value, upperBound);
        }

        public long getFloorAndSubtract() {
            long floor = (long) Math.floor(value);
            this.value = value - floor;
            this.lowerBound = lowerBound - floor;
            this.upperBound = upperBound - floor;
            return floor;
        }

        public BestLong getBestLongScale() {
            return InvestigateUnitRounding.getBestLongScale(lowerBound, value, upperBound);
        }

        public double getUpperBound() {
            return upperBound;
        }

        public double getValue() {
            return value;
        }

        public double getLowerBound() {
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
     * Code for converting from an unmixed unit to either an unmixed or mixed unit, and back.<br>
     * A production API might look quite different, because there are extra APIs for investigation
     * and testing.<br>
     * Not optimized!
     */
    public static class Converter {
        // for testing
        private static final boolean DOUBLE_CHECK = false;

        // used when the source needs to be inverted for compatibility
        private final boolean invertSource;
        // the conversion from source to first unit (only unit if mixed)
        // TODO for mixed to mixed have a second set for combining the source mixed units
        private final double[] conversionFactors;

        // only used in testing and debugging
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

            double[] convertBoth = new double[conversionFactors.length];

            double current = 1;

            for (int i = 0; i < conversionFactors.length; ++i) {
                if (i != 0) {
                    sb.append(" rem");
                }
                double conversionFactor = conversionFactors[i];
                sb.append(" ÷ ").append(conversionFactor).append(" → ").append(unitIds[i]);
                current *= conversionFactor;
                convertBoth[i] = current;
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
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

        /**
         * Creating a converter is straightforward. For example, for converting meter to
         * foot-and-inch, the result has two conversion factors: meter → foot and foot → inch.
         */
        public static Converter getConverter(
                final String sourceUnit, final String targetUnit, boolean matchFormatted) {
            List<Double> conversions = new ArrayList<>();
            List<String> units = Splitter.on("-and-").splitToList(targetUnit);

            // Check units: must be value units, and if mixed, must be descending in value
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
                    // test to see if we can invert;
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

                conversions.add(conversion.doubleValue());
            }
            return new Converter(sourceUnit, invertSource, conversions, units, matchFormatted);
        }

        private Converter(
                String sourceUnit,
                boolean invertSource,
                List<Double> conversions,
                List<String> unitsIds,
                boolean matchFormatted) {
            this.sourceUnitId = sourceUnit;
            this.invertSource = invertSource;
            this.conversionFactors = Doubles.toArray(conversions);
            this.unitIds = unitsIds.toArray(String[]::new);
            this.matchFormatted = matchFormatted;
        }

        /** Convert formatted numeric strings back to the source unit */
        public double convertBack(List<String> numbers, DecimalFormat df) {
            double result = 0;
            for (int i = numbers.size() - 1; i >= 0; --i) {
                double parsed = parseDouble(df, numbers.get(i));
                double conversionFactor = conversionFactors[i];
                result = (result + parsed) / conversionFactor;
            }
            return invertSource ? 1 / result : result;
        }

        /** Convert the converted value(s) back to the source unit, either rounding or unrounded */
        public double convertBackRaw(Converted converted, boolean rounding) {
            double conversionFactor = conversionFactors[converted.leadingCount];
            double finalValue =
                    !rounding
                            ? converted.finalValue
                            : roundToSignificant(converted.finalValue, converted.maxSignificant);
            double result = finalValue / conversionFactor;
            for (int i = converted.leadingCount - 1; i >= 0; --i) {
                conversionFactor = conversionFactors[i];
                result = (result + converted.leadingValues[i]) / conversionFactor;
            }
            return result;
        }

        /** Convert a value, disregarding precision. */
        public Converted convertRaw(double source) {
            Converted result = new Converted();
            double converted = source;
            int last = conversionFactors.length - 1;
            for (int i = 0; i <= last; ++i) {
                converted = converted * conversionFactors[i];
                if (i == last) {
                    // add final double
                    result.setDouble(converted, 14, 14);
                    break;
                } else {
                    // add floor
                    double floor = (long) Math.floor(converted);
                    result.addLong(floor);
                    converted -= floor;
                }
            }
            return result;
        }

        /** Version of converted with a simpler code flow */
        public Converted convert(final double source_, HackLocalizedNumberFormatter rp) {
            Converted result = new Converted();
            BoundDouble boundedDouble =
                    BoundDouble.fromDigits(
                            source_,
                            BoundDouble.PrecisionType.significant,
                            rp.maximumSignificantDigits);
            if (boundedDouble == null) { // zero or not finite
                result.setDouble(
                        invertSource ? 1 / source_ : source_,
                        rp.minimumSignificantDigits,
                        rp.maximumSignificantDigits);
                return result;
            }
            return convert(
                    boundedDouble,
                    rp.maximumSignificantDigits - rp.minimumSignificantDigits,
                    result);
        }

        /** Convert a boundedDouble, with the result in 'result' */
        public Converted convert(BoundDouble boundedDouble, int deltaMaxToMin, Converted result) {
            double originalLowerBound, originalUpperBound;
            if (DOUBLE_CHECK) {
                originalLowerBound = boundedDouble.lowerBound;
                originalUpperBound = boundedDouble.upperBound;
            }
            if (invertSource) {
                boundedDouble.invert();
            }
            int last = conversionFactors.length - 1;

            for (int i = 0; i <= last; ++i) {
                double conversionFactor = conversionFactors[i];
                boundedDouble.times(conversionFactor);
                if (i == last
                        || boundedDouble.getValue() > MAX_LONG_FROM_DOUBLE
                        || boundedDouble.inTightBounds()) { // check this out later) {
                    // the final value is always a double
                    int sigDigits = boundedDouble.toSignificantDigits();
                    result.setDouble(
                            boundedDouble.getValue(), sigDigits - deltaMaxToMin, sigDigits);

                    // double check to see that the rounding is reasonable
                    if (DOUBLE_CHECK) {
                        double trial = convertBackRaw(result, true);
                        if (trial <= originalLowerBound || trial >= originalUpperBound) {
                            // add to sigDigits
                            result.incrementSignificantDigits();
                        }
                    }
                    break;
                } else {
                    result.addLong(boundedDouble.getFloorAndSubtract());
                }
            }
            if (boundedDouble.getNegative() < 0) {
                result.negate();
            }
            return result;
        }

        static class BestLong {
            public final long value;
            public final int significantDigits;

            public BestLong(long value, int significantDigits) {
                this.value = value;
                this.significantDigits = significantDigits;
            }

            @Override
            public String toString() {
                return "Value: " + value + ", Significant Digits: " + significantDigits;
            }
        }

        /**
         * This is just an example for now; real code would get the unit patterns and adjust for
         * plurals and case.
         */
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
            FormattedNumber formatted = dfFinal.format(rountResult.finalValue);
            result.add(formatted.toString());
            return result;
        }

        public String getSourceUnitId() {
            return sourceUnitId;
        }

        /**
         * Container for the results of a conversion. Not immutable: successive operations can
         * modify it.
         *
         * <ul>
         *   <li>zero or more leading values (only used for mixed units),
         *   <li>a final double value, and
         *   <li>min/maxSignificant digits that apply to the final value.
         * </ul>
         *
         * Examples (with the units just for illustration)
         *
         * <ol>
         *   <li>123.45 yards, mxsd:4
         *   <li>15 degrees, 12 minutes, 42.39 seconds, mxsd:3
         */
        public static class Converted {
            private static final int MAX_MIXED_UNITS = 4;

            long[] leadingValues = null;
            int leadingCount = 0;
            double finalValue = Double.MIN_VALUE;
            int maxSignificant = Integer.MIN_VALUE;
            int minSignificant = Integer.MIN_VALUE;

            /** Set the final value, and sig digits */
            public void setDouble(double value, int minSig, int maxSig) {
                this.finalValue = value;
                this.minSignificant = minSig < 1 ? 1 : minSig;
                this.maxSignificant = maxSig < minSig ? minSig : maxSig;
            }

            public void incrementSignificantDigits() {
                this.minSignificant++;
                this.maxSignificant++;
            }

            /** Add a long if we know there will be another value to carry the sig digits */
            public void addLong(double source) {
                // TODO Auto-generated method stub
                if (leadingValues == null) {
                    leadingValues = new long[MAX_MIXED_UNITS];
                }
                leadingValues[leadingCount++] = (long) source;
            }

            public void negate() {
                if (leadingValues != null) {
                    leadingValues[0] = -leadingValues[0];
                } else {
                    finalValue = -finalValue;
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
                Converted other = (Converted) obj;
                return leadingCount == other.leadingCount
                        && Arrays.equals(leadingValues, other.leadingValues)
                        && finalValue == other.finalValue
                        && maxSignificant == other.maxSignificant
                        && minSignificant == other.minSignificant;
            }

            @Override
            public int hashCode() {
                // TODO Auto-generated method stub
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

    // UTILITIES for conversion
    // These are not optimized, nor checked for double rounding issues.
    // Deprecated ones would need fixing for production code

    /** Rounds to significant units. Unoptimized, may differ in final digits! */
    @Deprecated
    public static double roundToSignificant(double source, int significantDigits) {
        // Calculate how many decimal places we need to keep,
        // and get the right increment, then round
        int firstDigit = firstDigit(source);
        double increment = getRoundingIncrement(significantDigits, firstDigit);
        return roundByIncrement(source, increment);
    }

    /** Get a double value, centralized to avoid code clutter. */
    public static double parseDouble(DecimalFormat df, String string) {
        try {
            return df.parse(string).doubleValue();
        } catch (ParseException e) {
            throw new ICUException("Illegal number", e);
        }
    }

    /**
     * Returns the number of significant digits of value needed to get within
     * (lowerBound..upperbound) Unoptimized! <br>
     */
    @Deprecated
    private static int getBestDoubleScale(double lowerBound, double value, double upperbound) {
        // Calculate differences from bounds
        double lowerDiff = value - lowerBound;
        double upperDiff = upperbound - value;

        // Handle edge cases where value falls outside bounds or difference is 0
        if (lowerDiff <= 0 || upperDiff <= 0) {
            throw new UnitException("bounds problem");
        }

        int valueFirstSigDigitPlace = firstDigit(value);
        // Find the place value of the most restrictive difference
        int minFirstSigDigitPlace = firstDigit(lowerDiff);
        if (lowerDiff != upperDiff) {
            int upperFirstSigDigitPlace = firstDigit(upperDiff);
            minFirstSigDigitPlace = Math.min(minFirstSigDigitPlace, upperFirstSigDigitPlace);
        }

        int result = valueFirstSigDigitPlace - minFirstSigDigitPlace + 1;
        if (result <= 0) {
            result = 1;
        }

        return result;
    }

    /** Finds the best value for a long, used for leading units of a mixed unit. Unoptimized! */
    @Deprecated
    private static BestLong getBestLongScale(double lowerBound, double value, double upperbound) {
        if (!EXPERIMENTAL_ROUND) {
            long round = Math.round(value);
            if (lowerBound <= round && round <= upperbound) {
                int sig = round == 0 ? 1 : firstDigit(round);
                return new BestLong(round, sig);
            }
        }
        // Estimate an upper limit for E to start the downward scan
        // Normally this won't take more than a couple of iterations
        double span = upperbound - lowerBound;
        int startE = firstDigit(span);

        // Scan downwards from the top to guarantee finding the largest working E
        for (int E = startE + 1; E >= 0; E--) {
            double divisor = Math.pow(10, E);

            // Compute exclusive bounds in the scaled K space
            long minK = (long) Math.floor(lowerBound / divisor) + 1;
            long maxK = (long) Math.ceil(upperbound / divisor) - 1;

            // The first layer where minK <= maxK is strictly our largest possible E
            if (minK <= maxK) {
                double targetK = value / divisor;
                long bestK = Math.round(targetK);

                // Force clamp to make sure it stays within range
                bestK = Math.max(minK, Math.min(maxK, bestK));

                long resultValue = bestK * (long) divisor;
                int sigDigits = InvestigateUnitRounding.calculateSignificantDigits(bestK);
                if (!(lowerBound < resultValue && resultValue < upperbound)) {
                    throw new UnitException("Result value fails: " + resultValue);
                }
                return new BestLong(resultValue, sigDigits);
            }
        }
        throw new UnitException("Should never happen");
    }

    /** Find number of significant digits. Unoptimized! */
    private static int calculateSignificantDigits(long bestK) {
        // Avoid string allocation
        int length = 0;
        while (bestK > 0) {
            length++;
            bestK /= 10;
        }
        return length;
    }

    /** Rounding increment */
    @Deprecated
    private static double getRoundingIncrement(int significantDigits, int firstDigit) {
        return Math.pow(10, afterLastDigit(significantDigits, firstDigit));
    }

    /** Get the position of the first INsignificant digit */
    private static int afterLastDigit(int significantDigits, int firstDigit) {
        return significantDigits - firstDigit;
    }

    /** Round to a multiple of the increment. For example, (13, 5) => 15 */
    @Deprecated
    private static double roundByIncrement(double source, double increment) {
        return Math.round(source * increment) / increment;
    }

    /** The position of the lead digit. 1..9.99… -> 1; 10..99.99… -> 2, 0.1..0.999… -> -1; etc. */
    @Deprecated
    private static int firstDigit(double value) {
        return (int) Math.floor(Math.log10(value)) + 1;
    }

    /** Are two doubles within epsilon of each other? */
    public static boolean approximatelyEqualRelative(double a, double b, double epsilon) {
        if (a == b) return true; // Handles infinities and exact matches
        if (Double.isNaN(a) && Double.isNaN(b))
            return true; // For our purposes all NaNs are equivalent
        return Math.abs(a - b) < Math.max(Math.abs(a), Math.abs(b)) * epsilon;
    }

    /** Get the value with all digits */
    public static String getPlainString(double sourceAmount) {
        return Double.isFinite(sourceAmount)
                ? BigDecimal.valueOf(sourceAmount).toPlainString()
                : Double.toString(sourceAmount);
    }

    /** Format precision values, just for debugging */
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
