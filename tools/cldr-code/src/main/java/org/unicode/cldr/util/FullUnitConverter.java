package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/**
 * Code for converting from an unmixed unit to either an unmixed or mixed unit, and back,
 * maintaining exact Rational numbers throughout the pipeline. TODO: extend to mixed/unmixed <-->
 * mixed/unmixed
 */
public class FullUnitConverter {

    private static final Rational MAX_LONG_FROM_RATIONAL =
            Rational.of(BigInteger.valueOf(1L << 53));

    private static final UnitConverterPU UNIT_CONVERTER = new UnitConverterPU();

    private static final boolean DOUBLE_CHECK = false;

    private final boolean invertSource;
    private final Rational[] conversionFactors;
    private final String sourceUnitId;
    private final String[] targetUnitIds;
    private final UnitConverterPU unitConverter; // note, this is mutable; TODO add clone

    public static FullUnitConverter getConverter(final String sourceUnit, final String targetUnit) {
        return getConverter(sourceUnit, targetUnit, UNIT_CONVERTER);
    }

    public FullUnitConverter getInverseConverter() {
        return getConverter(targetUnitIds[0], sourceUnitId, unitConverter); // for now, just unmixed
    }

    public static FullUnitConverter getConverter(
            final String sourceUnit, final String targetUnit, UnitConverterPU unitConverter) {
        List<Rational> conversions = new ArrayList<>();
        List<String> units = Splitter.on("-and-").splitToList(targetUnit);

        Output<String> baseUnitId = new Output<>();
        ConversionInfo info = unitConverter.parseUnitId(sourceUnit, baseUnitId, false);
        if (baseUnitId.value == null) {
            throw new UnitException("Unit not recognized: " + sourceUnit);
        }
        String normalizedSourceUnitId = unitConverter.getReducedUnit(baseUnitId.value);

        Rational lastFactor = null;
        String lastUnit = null;
        boolean invertSource = false;

        for (String unit : units) {
            info = unitConverter.parseUnitId(unit, baseUnitId, false);
            if (baseUnitId.value == null) {
                throw new UnitException("Unit not recognized: " + unit);
            }
            String normalizedBaseUnitID = unitConverter.getReducedUnit(baseUnitId.value);
            if (!normalizedBaseUnitID.equals(normalizedSourceUnitId)) {
                if (lastFactor == null) {
                    String reciprocalUnit = unitConverter.reciprocalOf(normalizedBaseUnitID);
                    if (reciprocalUnit.equals(normalizedSourceUnitId)) {
                        invertSource = true;
                    }
                }
                if (!invertSource) {
                    throw new UnitException(
                            mismatched(
                                    sourceUnit,
                                    normalizedSourceUnitId,
                                    unit,
                                    normalizedBaseUnitID));
                }
            }
            if (lastFactor == null) {
                lastFactor = info.factor;
                lastUnit = unit;
            } else if (info.factor.compareTo(lastFactor) >= 0) {
                throw new UnitException(badMixed(info, lastFactor, lastUnit, unit));
            }
        }

        List<String> targetUnits = new ArrayList<>();
        targetUnits.add(sourceUnit);
        targetUnits.addAll(units);

        for (int i = 1; i < targetUnits.size(); ++i) {
            Rational conversion =
                    unitConverter.convert(
                            Rational.ONE, targetUnits.get(i - 1), targetUnits.get(i), false);
            conversions.add(conversion); // Keep conversion factor exact as Rational
        }

        return new FullUnitConverter(sourceUnit, invertSource, conversions, units, unitConverter);
    }

    private static String badMixed(
            ConversionInfo info, Rational lastFactor, String lastUnit, String unit) {
        return "Bad mixed units: "
                + lastUnit
                + " ("
                + lastFactor
                + ")"
                + " <= "
                + unit
                + " ("
                + info.factor
                + ")";
    }

    private static String mismatched(
            final String sourceUnit,
            String normalizedSourceUnitId,
            String unit,
            String normalizedBaseUnitID) {
        return "Mismatched baseUnitIds for "
                + sourceUnit
                + "&"
                + unit
                + ": "
                + normalizedSourceUnitId
                + "≠"
                + normalizedBaseUnitID;
    }

    public String getSourceUnitId() {
        return sourceUnitId;
    }

    public String getTargetUnitId() {
        return Joiner.on("-and-").join(targetUnitIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSourceUnitId());
        if (invertSource) {
            sb.append(" recip");
        }
        Rational current = Rational.ONE;
        for (int i = 0; i < conversionFactors.length; ++i) {
            if (i != 0) {
                sb.append(" rem");
            }
            Rational conversionFactor = conversionFactors[i];
            sb.append(" ÷ ").append(conversionFactor).append(" → ").append(targetUnitIds[i]);
            current = current.multiply(conversionFactor);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FullUnitConverter)) return false;
        FullUnitConverter that = (FullUnitConverter) obj;
        return getSourceUnitId().equals(that.getSourceUnitId())
                && invertSource == that.invertSource
                && Arrays.equals(conversionFactors, that.conversionFactors)
                && Arrays.equals(targetUnitIds, that.targetUnitIds);
    }

    @Override
    public int hashCode() {
        return getSourceUnitId().hashCode()
                + (invertSource ? 31 * 31 * 31 : 0)
                + (Arrays.hashCode(conversionFactors) * 31 + Arrays.hashCode(targetUnitIds)) * 31;
    }

    private FullUnitConverter(
            String sourceUnit,
            boolean invertSource,
            List<Rational> conversions,
            List<String> unitsIds,
            UnitConverterPU unitConverter) {
        this.sourceUnitId = sourceUnit;
        this.invertSource = invertSource;
        this.conversionFactors = conversions.toArray(new Rational[0]);
        this.targetUnitIds = unitsIds.toArray(new String[0]);
        this.unitConverter = unitConverter;
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
                        : converted.finalValue.roundToSignificant(converted.maxSignificant);
        Rational result = finalValue.divide(conversionFactor);
        for (int i = converted.leadingCount - 1; i >= 0; --i) {
            conversionFactor = conversionFactors[i];
            result = result.add(Rational.of(converted.leadingValues[i])).divide(conversionFactor);
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

    /**
     * Convert a rational source, using the precision from a HackLocalizedNumberFormatter. This
     * converts the input into a BoundRational, and processes from there.
     */
    public Converted convert(final Rational source, HackLocalizedNumberFormatter rp) {
        Converted result = new Converted();
        BoundedNumber boundedRational =
                BoundedNumber.from(
                        source,
                        BoundedNumber.PrecisionType.significant,
                        rp.getMaximumSignificantDigits());
        if (boundedRational == null) { // handles special cases like non-finite or zero
            result.setRational(
                    invertSource ? source.reciprocal() : source,
                    rp.getMinimumSignificantDigits(),
                    rp.getMaximumSignificantDigits());
            return result;
        }
        result.log("source bounded", boundedRational);
        return convert(
                boundedRational,
                rp.getMaximumSignificantDigits() - rp.getMinimumSignificantDigits(),
                result);
    }

    /** Convert a BoundRational, storing the result in 'result' */
    public Converted convert(BoundedNumber boundedRational, int deltaMaxToMin, Converted result) {
        Rational originalLowerBound = boundedRational.getLowerBound();
        Rational originalUpperBound = boundedRational.getUpperBound();

        if (invertSource) {
            boundedRational.invert();
            result.log("invert", boundedRational);
        }

        int last = conversionFactors.length - 1;
        for (int i = 0; i <= last; ++i) {
            Rational conversionFactor = conversionFactors[i];
            boundedRational.multiply(conversionFactor);
            result.log("multiply" + i, boundedRational);

            if (i == last
                    || boundedRational.getValue().compareTo(MAX_LONG_FROM_RATIONAL) > 0
                    || boundedRational.boundsContainInteger()) {
                int sigDigits = boundedRational.toSignificantDigits();
                result.log("toSD" + i, sigDigits);
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
                long floor = boundedRational.getFloorAndSubtract();
                result.addLong(floor);
                result.log("remainder" + i, boundedRational);
                result.log("floor" + i, boundedRational);
            }
        }

        if (boundedRational.getNegative() == Sign.negative) {
            result.negate();
        }
        return result;
    }

    public String formatWithUnits(Converted converted, HackLocalizedNumberFormatter sourceNf) {
        List<String> numberStrings = formattedNumbers(converted, sourceNf);
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
            result.append(targetUnitIds[i]);
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

    /**
     * Container for the results of a Rational conversion. It consists of zero or more integer
     * values followed by a final rational.
     */
    public static class Converted {

        private static final int MAX_MIXED_UNITS = 4;

        private long[] leadingValues = null;
        private int leadingCount = 0;
        private Rational finalValue = Rational.ZERO;
        private int maxSignificant = Integer.MIN_VALUE;
        private int minSignificant = Integer.MIN_VALUE;
        private Map<String, String> log = new LinkedHashMap<>();

        public void setRational(Rational value, int minSig, int maxSig) {
            this.finalValue = value;
            this.minSignificant = minSig < 1 ? 1 : minSig;
            this.maxSignificant = maxSig < minSig ? minSig : maxSig;
        }

        public void log(String string, Object object) {
            log.put(string, object.toString());
        }

        public Map<String, String> getLog() {
            return log;
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
                    sb.append(" : ");
                }
                sb.append(leadingValues[i]);
            }
            if (sb.length() != 0) {
                sb.append(" : ");
            }
            String plainString = toPlainString(finalValue);
            sb.append(plainString);
            String decimalString = toDecimalString(finalValue, MathContext.DECIMAL128);
            if (!plainString.equals(decimalString)) {
                sb.append("=");
                sb.append(decimalString);
            }
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

    public enum Sign {
        positive("+"),
        negative("-");
        private final String symbol;

        private Sign(String name) {
            this.symbol = name;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    /**
     * A Rational value and its exact upper/lower bounds, with lower < value < upper. It is mutable
     * so that it can be altered in-place, without having to create new objects.
     */
    public static class BoundedNumber {

        private Sign negative;
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

        private BoundedNumber(
                Sign negative, Rational lowerBound, Rational value, Rational upperBound) {
            if (lowerBound.compareTo(value) > 0 || value.compareTo(upperBound) > 0) {
                throw new UnitException("Bad value");
            }
            this.negative = negative;
            this.lowerBound = lowerBound;
            this.value = value;
            this.upperBound = upperBound;
        }

        public static BoundedNumber from(
                Sign i, Rational lowerBound, Rational value, Rational upperBound) {
            return new BoundedNumber(i, lowerBound, value, upperBound);
        }

        public BoundedNumber clone() {
            return new BoundedNumber(negative, lowerBound, value, upperBound);
        }

        /**
         * Given a source, precision type, and digits, produce the upper and lower bounds. This can
         * then feed into the lower-level conversion method.
         */
        public static BoundedNumber from(Rational source, PrecisionType type, int digits) {
            // we should never be called with these values, but just in case…
            if (source == null || source.equals(Rational.ZERO) || !source.isFinite()) {
                return null;
            }

            Sign negative;
            if (source.compareTo(Rational.ZERO) < 0) {
                negative = Sign.negative;
                source = source.abs();
            } else {
                negative = Sign.positive;
            }

            if (type == PrecisionType.fraction) {
                throw new IllegalArgumentException("do later");
            }

            // So for 1234, with 2 sig digits, the value is 10;
            // For 1.234 with 2 sig digits, the value is 0.01
            Rational increment = Rational.getIncrement(source.firstDigitPosition(), digits);

            source = source.roundByIncrement(increment);

            // Exact delta = 0.5 * increment = 1 / (2 * increment)
            Rational delta =
                    increment.divide(
                            Rational.TWO); // Rational.TWO.multiply(increment).reciprocal(); //
            // 1/(2*increment)

            // find out if the last non-zero digit of rounded value below
            String str = source.divide(increment).floor().abs().toString();
            boolean isLastNonZeroOne = false;
            for (int i = str.length() - 1; i >= 0; i--) {
                char c = str.charAt(i);
                if (c != '0') {
                    isLastNonZeroOne = (c == '1');
                    break;
                }
            }

            Rational lowerBound = isLastNonZeroOne ? source : source.subtract(delta);
            Rational upperBound = source.add(delta);

            return from(negative, lowerBound, source, upperBound);
        }

        public BoundedNumber invert() {
            Rational temp = lowerBound;
            this.lowerBound = upperBound.reciprocal();
            this.value = value.reciprocal();
            this.upperBound = temp.reciprocal();
            return this;
        }

        public BoundedNumber multiply(Rational conversionFactor) {
            this.value = value.multiply(conversionFactor);
            this.lowerBound = lowerBound.multiply(conversionFactor);
            this.upperBound = upperBound.multiply(conversionFactor);
            return this;
        }

        public BoundedNumber divideBy(Rational conversionDivisor) {
            this.value = value.divide(conversionDivisor);
            this.lowerBound = lowerBound.divide(conversionDivisor);
            this.upperBound = upperBound.divide(conversionDivisor);
            return this;
        }

        public boolean boundsContainInteger() {
            BigInteger lowerBoundCeiling = lowerBound.ceil();
            return lowerBoundCeiling.compareTo(upperBound.floor()) <= 0;
        }

        public int toSignificantDigits() {
            Rational lowerDiff = value.subtract(lowerBound);
            Rational upperDiff = upperBound.subtract(value);

            if (lowerDiff.compareTo(Rational.ZERO) < 0 || upperDiff.compareTo(Rational.ZERO) < 0) {
                throw new UnitException("bounds problem");
            }

            int valueFirstSigDigitPlace = value.firstDigitPosition();
            int minFirstSigDigitPlace = lowerDiff.firstDigitPosition();
            if (!lowerDiff.equals(upperDiff)) {
                int upperFirstSigDigitPlace = upperDiff.firstDigitPosition();
                minFirstSigDigitPlace = Math.min(minFirstSigDigitPlace, upperFirstSigDigitPlace);
            }

            int result = valueFirstSigDigitPlace - minFirstSigDigitPlace + 1;
            return Math.max(result, 1);
        }

        public long getFloorAndSubtract() {
            BigInteger floor = value.floor();
            Rational floorRat = Rational.of(floor);
            this.value = value.subtract(floorRat);
            this.lowerBound = lowerBound.subtract(floorRat);
            this.upperBound = upperBound.subtract(floorRat);
            return floor.longValueExact();
        }

        public Rational getUpperBound() {
            return upperBound;
        }

        public Rational getValue() {
            return value;
        }

        public Rational getLowerBound() {
            return lowerBound;
        }

        public Sign getNegative() {
            return negative;
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "⟪",
                    negative,
                    ", ",
                    lowerBound.toString(FormatStyle.repeating),
                    ", ",
                    value.toString(FormatStyle.repeating),
                    ", ",
                    upperBound.toString(FormatStyle.repeating),
                    "⟫");
        }
    }

    public static class UnitException extends RuntimeException {

        private static final long serialVersionUID = 302617249795260431L;

        public UnitException(String message) {
            super(message);
        }
    }

    // Utilities that probably should be moved to Rational

    public static String toPlainString(Rational sourceAmount) {
        if (sourceAmount == null) return null;
        if (sourceAmount.denominator == BigInteger.ZERO) {
            return sourceAmount == Rational.INFINITY
                    ? "INF"
                    : sourceAmount == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
        }
        return sourceAmount.toString();
    }

    public static String toDecimalString(Rational sourceAmount, MathContext mathContext) {
        if (sourceAmount == null) return null;
        if (sourceAmount.denominator == BigInteger.ZERO) {
            return sourceAmount == Rational.INFINITY
                    ? "INF"
                    : sourceAmount == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
        }
        return new BigDecimal(sourceAmount.numerator)
                .divide(new BigDecimal(sourceAmount.denominator), mathContext)
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

    /**
     * Special class for allowing private-use units, to allow for testing. Does not handle units
     * with offsets, etc; just factors.
     */
    public static class UnitConverterPU {
        static final UnitConverter baseUnitConverter =
                SupplementalDataInfo.getInstance().getUnitConverter();

        final ConcurrentHashMap<String, Pair<String, ConversionInfo>> unitMap =
                new ConcurrentHashMap<>();

        public UnitConverterPU addPrivateUnit(
                String privateUnit, String normalizedBaseUnit, Rational conversionFactor) {
            if (!privateUnit.startsWith("xxx-")) {
                throw new UnitException("Private-use units must start with 'xxx-'");
            }
            ConversionInfo conversionInfo = new ConversionInfo(conversionFactor, Rational.ZERO);
            unitMap.put(privateUnit, Pair.of(normalizedBaseUnit, conversionInfo));
            return this;
        }

        public ConversionInfo parseUnitId(
                String sourceUnit, Output<String> baseUnitId, boolean showYourWork) {
            Pair<String, ConversionInfo> pair = unitMap.get(sourceUnit);
            if (pair != null) {
                baseUnitId.value = pair.getFirst();
                return pair.getSecond();
            }
            return baseUnitConverter.parseUnitId(sourceUnit, baseUnitId, showYourWork);
        }

        public String getReducedUnit(String sourceUnit) {
            if (unitMap.containsKey(sourceUnit)) {
                return sourceUnit;
            }
            return baseUnitConverter.getReducedUnit(sourceUnit);
        }

        public String reciprocalOf(String normalizedBaseUnitID) {
            return baseUnitConverter.reciprocalOf(normalizedBaseUnitID); // no change necessary
        }

        public Rational convert(
                Rational sourceValue, String sourceUnit, String targetUnit, boolean showYourWork) {
            Rational fixTarget = null;
            Pair<String, ConversionInfo> pair = unitMap.get(sourceUnit);
            if (pair != null) {
                sourceValue = sourceValue.multiply(pair.getSecond().factor);
                sourceUnit = pair.getFirst();
            }

            pair = unitMap.get(targetUnit);
            if (pair != null) {
                targetUnit = pair.getFirst();
                fixTarget = pair.getSecond().factor;
            }

            Rational targetValue =
                    baseUnitConverter.convert(sourceValue, sourceUnit, targetUnit, showYourWork);

            if (fixTarget != null) {
                targetValue = targetValue.multiply(fixTarget);
            }
            return targetValue;
        }
    }

    /**
     * Ugly hack because you can't get a locale from a LocalizedNumberFormatter, a Precision from a
     * NumberFormatter, nor can you get the various settings (like minimumSignificantDigits) from a
     * Precision, nor can you get DecimalFormatProperties from a NumberFormatter!
     */
    public static class HackLocalizedNumberFormatter {

        private final Locale locale;
        private final DecimalFormatProperties dfp;
        public final LocalizedNumberFormatter lnf;

        public int getMaximumFractionDigits() {
            return dfp.getMaximumFractionDigits();
        }

        public int getMaximumSignificantDigits() {
            return dfp.getMaximumSignificantDigits();
        }

        public int getMinimumFractionDigits() {
            return dfp.getMinimumFractionDigits();
        }

        public int getMinimumSignificantDigits() {
            return dfp.getMinimumSignificantDigits();
        }

        public Locale getLocale() {
            return locale;
        }

        public LocalizedNumberFormatter getLnf() {
            return lnf;
        }

        public HackLocalizedNumberFormatter(
                LocalizedNumberFormatter lnf,
                Locale locale,
                int minimumSignificantDigits,
                int maximumSignificantDigits) {
            this(
                    lnf.withoutLocale()
                            .locale(locale)
                            .precision(
                                    Precision.minMaxSignificantDigits(
                                            minimumSignificantDigits, maximumSignificantDigits)),
                    locale);
        }

        public HackLocalizedNumberFormatter(LocalizedNumberFormatter lnf, Locale locale) {
            dfp = getDecimalFormatProperties(lnf);
            this.locale = locale;
            this.lnf = lnf;
        }

        /** ugly hack */
        private DecimalFormatProperties getDecimalFormatProperties(LocalizedNumberFormatter lnf) {
            DecimalFormatProperties dfp = new DecimalFormatProperties();
            String skeleton = lnf.toSkeleton();
            PatternStringParser.parseToExistingProperties(skeleton, dfp);
            return dfp;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HackLocalizedNumberFormatter)) return false;
            HackLocalizedNumberFormatter that = (HackLocalizedNumberFormatter) obj;
            return getMinimumSignificantDigits() == that.getMinimumSignificantDigits()
                    && getMaximumSignificantDigits() == that.getMaximumSignificantDigits();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getMinimumSignificantDigits(), getMaximumSignificantDigits());
        }

        @Override
        public String toString() {
            return formatPrecision(getMinimumSignificantDigits(), getMaximumSignificantDigits());
        }

        public LocalizedNumberFormatter precision(FractionPrecision precision) {
            return lnf.precision(precision);
        }

        public HackLocalizedNumberFormatter precision(int minSignificant, int maxSignificant) {
            return new HackLocalizedNumberFormatter(lnf, locale, minSignificant, maxSignificant);
        }

        /** Locale-sensitive formatting */
        public String formatToString(Rational finalValue) {
            if (finalValue.denominator == BigInteger.ZERO) {
                return finalValue == Rational.INFINITY
                        ? "INF"
                        : finalValue == Rational.NEGATIVE_INFINITY ? "-INF" : "NaN";
            }
            return lnf.format(finalValue.toBigDecimal(MathContext.DECIMAL128)).toString();
        }
    }
}
