package org.unicode.cldr.tool;

import com.google.common.primitives.Doubles;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.OutputInt;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.tool.InvestigateUnitRounding.Converter.Processed;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/**
 * This is for investigating how we can handle unit conversion rounding, including both unmixed and
 * mixed units. The code is not intended to be final production code.
 */
public class InvestigateUnitRounding {
    private static final boolean VERBOSE = true;

    // Settings for investigation
    private static final boolean MATCH_FORMATTED = true;
    private static final int lineToBreakAt = 0; // 0 for no lines
    private static final int maxSigSamples = 14;
    private static final boolean EXPERIMENTAL_ROUND = false;

    private static final UnitConverter UNIT_CONVERTER =
            SupplementalDataInfo.getInstance().getUnitConverter();

    public static void main(String[] args) {
        checkRounding();
        checkSamples();
    }

    private static void checkSamples() {
        final int minSigTesting = 1;
        String[][] samples = {
            // Deliberate errors
            {
                "furlong",
                "1",
                "meter-and-fortnight",
                "Mismatched baseUnitIds for furlong&fortnight: meter≠second"
            },
            {"meter", "1", "foot-and-yard", "Bad mixed units: foot (381/1250) <= yard (1143/1250)"},
            {"meter", "1", "foot-and-foot", "Bad mixed units: foot (381/1250) <= foot (381/1250)"},

            // Unmixed units
            {"pound-force-per-square-inch", "5", "bar"},
            {"meter", "5", "yard"},

            // Mixed units
            {"meter", "3", "yard-and-foot"},
            {"radian", "6", "degree-and-arc-minute-and-arc-second"},
            {"week", "5", "day-and-hour-and-minute"},
            {"kilometer", "6", "mile-and-rod-and-foot-and-inch"},
            // not yet working right
            // {"radian", "3", "degree-and-arc-minute-and-arc-second"},

            // Inverse units
            {"meter-per-second", "5", "minute-per-mile"},
            {"meter-per-second", "5", "minute-per-mile-and-second-per-mile"},
        };

        OutputInt line = new OutputInt(1);

        for (String[] sample : samples) {
            String sourceUnit = sample[0];
            int topPrecision = Integer.parseInt(sample[1]);
            String targetUnit = sample[2];
            String expectedError = sample.length < 4 ? null : sample[3];

            System.out.println("Sample: " + Arrays.asList(sample));

            Converter converter = null;
            String errorMessage = null;
            try {
                converter = Converter.getConverter(sourceUnit, targetUnit, MATCH_FORMATTED);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
            if (!Objects.equals(errorMessage, expectedError)) {
                System.out.println("Expected error: " + expectedError + ", got: " + errorMessage);
                continue;
            }
            if (converter == null) {
                continue; // error caught above
            }

            System.out.println("Converter: " + converter + "\n");
            int totalErrors = 0;
            int ok = 0;

            int start = 0;
            int increment = 1;
            int limit = start + 32 * increment;

            double fuzzier = 1 / Math.PI;

            for (int sourceAmount = start; sourceAmount <= limit; sourceAmount += increment) {
                double amount = sourceAmount * fuzzier;
                for (int minMax = minSigTesting; minMax <= topPrecision; ++minMax) {
                    // for now, min=max
                    int errors = 0;
                    // errors += errorCount(line, converter, amount / FUDGE_FACTOR, minMax, minMax);
                    errors += errorCount(line, converter, amount, minMax, minMax);
                    // errors += errorCount(line, converter, amount * FUDGE_FACTOR, minMax, minMax);
                    ok += 1 - errors;
                    totalErrors += errors;
                }
                int errors = errorCount(line, converter, amount, maxSigSamples, maxSigSamples);
                ok += 1 - errors;
                totalErrors += errors;

                if (VERBOSE && lineToBreakAt <= 0) {
                    System.out.println();
                }
            }
            if (totalErrors != 0) {
                System.out.println("Errors: " + totalErrors + "\tOK: " + ok);
            }
        }
    }

    private static void checkRounding() {
        double source = Math.PI * 1000;

        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(Locale.ENGLISH), 5, 5, Locale.ENGLISH);

        for (int sig = 1; sig <= 15; ++sig) {
            hackLnf = hackLnf.precision(sig, sig);
            String lnfFormatted = hackLnf.format(source).toString();
            double rounded = roundToSignificant(source, sig);
            String roundedValue = getPlainString(rounded);
            System.out.println(Joiners.COMMA_SP.join(sig, lnfFormatted, roundedValue));
        }
        int debug = 0;

        //		for (double multiplier : Arrays.asList(10d, 1d, 0.1d)) {
        //			for (int sd = 1; sd < 4; ++sd) {
        //		for (int i = 1; i < 10; ++i) {
        //				double amount = multiplier*i;
        //				double delta = Converter.getBestDelta(amount, sd);
        //				double delta2 = amount * 0.5 / Math.pow(10,sd);
        //				System.out.println(sd + ", " + amount + ", " + delta + ", " + delta2 + ", " +
        // delta2*100/amount + "%");
        //				// sig=1; 1=>~0.05, 9=>~0.5
        //		}}
        //		}
        //		double test2 = 0.995d;
        //    	System.out.println(BigDecimal.valueOf(test2).toPlainString());
        //    	LocalizedNumberFormatter nf1 =
        // NumberFormatter.with().precision(Precision.minMaxSignificantDigits(2,
        // 2)).locale(Locale.ENGLISH);
        //    	String formatted1 = nf1.format(test2).toString();
        //    	System.out.println(formatted1);
    }

    private static int errorCount(
            OutputInt line, Converter converter, double sourceAmount, int minSig, int maxSig) {
        line.value++;
        if (lineToBreakAt > 0 && lineToBreakAt != line.value) {
            return 0;
        }

        Locale locale = Locale.ENGLISH;

        // Unfortunately, the ICU APIs are more limited than we need, because:
        // You can't get the precision back out of a NumberFormatter
        // Nor the settings from a Precision (like minSigDigits)
        // nor can you get DecimalFormatProperties from a NumberFormatter!

        // Use hack to carry readable precision and locale with LocalizedNumberFormatter
        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(locale), minSig, minSig, locale);

        // We have to guess that the parser parallels the formatter:
        // because we can't build a DecimalFormat from a LocalizedNumberFormatter

        DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(locale);

        // example 1.1 meters => 1.20297 yards => 1 yard 0.60891 feet (but then rounded
        String sourceFormatted = hackLnf.format(sourceAmount).toString();
        Processed processed = converter.convert(sourceAmount, hackLnf);

        // Check the results
        CheckResult check = new CheckResult(converter, sourceAmount, hackLnf, processed);
        String formattedWithUnits = converter.formatWithUnits(processed, hackLnf).toString();

        //        double roundTripUnrounded = converter.convertBackUnrounded(processed);
        //
        //        boolean approximatelyEqual =
        //                approximatelyEqualRelative(sourceAmount, roundTripUnrounded,
        // 0.000_000_000_000_001);
        //
        //        List<String> numbers = converter.formattedNumbers(processed, lnf);
        //
        //        String formattedWithUnits = converter.formatWithUnits(processed, lnf).toString();
        //
        //        double roundTrip = converter.convertBack(numbers, parser);
        //        String roundTripFormatted = lnf.format(roundTrip).toString();
        //
        //        Processed rawProcessed = converter.convertRaw(sourceAmount);
        //
        //        boolean roundedEqual = roundTripFormatted.equals(sourceFormatted);

        if (VERBOSE || !check.matches) {
            System.out.println(
                    Joiners.ES.join(
                            line.value,
                            "\t",
                            hackLnf,
                            "\t",
                            getPlainString(sourceAmount),
                            "\t«",
                            sourceFormatted,
                            " ",
                            converter.sourceUnitId,
                            "»",
                            "\t→ «",
                            formattedWithUnits,
                            "»",
                            "\t→ «",
                            check.roundTripFormatted,
                            "»",
                            "\tProcessed: ",
                            processed));
        }
        if (!check.matches) {
            double delta =
                    parseDouble(parser, check.sourceFormatted)
                            - parseDouble(parser, check.roundTripFormatted);
            System.out.println(
                    Joiners.ES.join(
                            "  FAILS ROUNDING ROUNDTRIP, source: ",
                            "«" + sourceFormatted + "»",
                            " roundtripping: «",
                            check.roundTripFormatted,
                            "»",
                            " unrounded difference: ",
                            getPlainString(delta)));
            return 1;
        }

        return 0;
    }

    private static String getPlainString(double sourceAmount) {
        return Double.isFinite(sourceAmount)
                ? BigDecimal.valueOf(sourceAmount).toPlainString()
                : Double.toString(sourceAmount);
    }

    /**
     * Hack because you can't get a Precision from a NumberFormatter, nor can you get the various
     * settings (like minimumSignificantDigits) from a Precision, nor can you get
     * DecimalFormatProperties from a NumberFormatter!
     */
    public static class HackLocalizedNumberFormatter {
        private final int minimumSignificantDigits;
        private final int maximumSignificantDigits;
        private final Locale locale;
        private final LocalizedNumberFormatter lnf;

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

    public static String formatPrecision(
            int minimumSignificantDigits, int maximumSignificantDigits) {
        return "⟨"
                + minimumSignificantDigits
                + (minimumSignificantDigits == maximumSignificantDigits
                        ? ""
                        : "‥" + maximumSignificantDigits)
                + "⟩";
    }

    public static class CheckResult {
        final String sourceFormatted;
        final List<String> numbers;
        final double roundTrip;
        final String roundTripFormatted;
        final boolean matches;
        final double rawConvertBack;
        final double source;

        public CheckResult(
                Converter converter,
                double source,
                HackLocalizedNumberFormatter rp,
                Processed processed) {
            this.source = source;
            sourceFormatted = rp.lnf.format(source).toString();

            DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(rp.getLocale());

            // get the formatted numbers from the processed value, with the hackLnf
            // then convert to double
            // then format that double

            numbers = converter.formattedNumbers(processed, rp);
            rawConvertBack = converter.convertBackRaw(processed);
            roundTrip = converter.convertBack(numbers, parser);
            roundTripFormatted = rp.lnf.format(roundTrip).toString();
            matches = sourceFormatted.equals(roundTripFormatted);
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "source:       \t",
                    getPlainString(source),
                    "\nsourceForm.:\t",
                    sourceFormatted,
                    "\nnumbers:    \t",
                    numbers,
                    "\nroundTrip:  \t",
                    getPlainString(roundTrip),
                    "\nrawCvttBack:\t",
                    getPlainString(rawConvertBack),
                    "\nrtFormatted:\t",
                    roundTripFormatted,
                    "\nmatches:    \t",
                    matches);
        }
    }

    /**
     * Just an API for investigation; a real API would look different. Not optimized, and has extra
     * API for checking steps.
     */
    public static class Converter {
        private static final double MAX_LONG_FROM_DOUBLE = Math.pow(2, 53);

        private final String sourceUnitId;
        private final boolean invertSource;
        private final double[] conversionFactors;
        private final String[] unitIds;
        private final boolean matchFormatted;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(sourceUnitId);
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
                sb.append("÷").append(conversionFactor).append("→").append(unitIds[i]);
                current *= conversionFactor;
                convertBoth[i] = current;
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            Converter that = (Converter) obj;
            return sourceUnitId.equals(that.sourceUnitId)
                    && Arrays.equals(conversionFactors, that.conversionFactors)
                    && Arrays.equals(unitIds, that.unitIds);
        }

        @Override
        public int hashCode() {
            return sourceUnitId.hashCode()
                    + (Arrays.hashCode(conversionFactors) * 31 + Arrays.hashCode(unitIds)) * 31;
        }

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

        public double convertBack(List<String> numbers, DecimalFormat df) {
            double result = 0;
            for (int i = numbers.size() - 1; i >= 0; --i) {
                double parsed = parseDouble(df, numbers.get(i));
                double conversionFactor = conversionFactors[i];
                result = (result + parsed) / conversionFactor;
            }
            return invertSource ? 1 / result : result;
        }

        public double convertBackRaw(Processed roundResult) {
            double conversionFactor = conversionFactors[roundResult.leadingCount];
            double result = roundResult.finalValue / conversionFactor;
            for (int i = roundResult.leadingCount - 1; i >= 0; --i) {
                conversionFactor = conversionFactors[i];
                result = (result + roundResult.leadingValues[i]) / conversionFactor;
            }
            return result;
        }

        public Processed convertRaw(double source) {
            Processed result = new Processed();
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

        public Processed convert(final double source_, HackLocalizedNumberFormatter rp) {
            Processed result = new Processed();

            final int minSig = rp.minimumSignificantDigits;
            final int maxSig = rp.maximumSignificantDigits;

            if (source_ == 0 || Double.isInfinite(maxSig)) {
                if (invertSource) {
                    result.setDouble(1 / source_, minSig, maxSig);
                    return result;
                }
                result.setDouble(source_, minSig, maxSig);
                return result;
            }

            boolean negative = source_ < 0;
            double source = negative ? -source_ : source_;

            int deltaMaxToMin = maxSig - minSig;

            /*
               Key Section

               Derive a value (converted) which is the source rounded according to the maxSignificant digits.
               Derive a delta value based on that rounded source and the max significant digits.
            The goal for the delta is to get to within ±.5 (scaled by the right power of 10) of the converted value.
            With source=5.6 and maxSigDigits=1, the goal is 6 ± 0.5 — NOT 5.6 ±5.
            With source=0.56 and maxSigDigits=1, the goal is 0.6 ± 0.05 — NOT 0.56 ± 0.05.
               and so on.
               */

            // Determine the position of the lead digit.
            // 1..9.99… -> 1; 10..99.99… -> 2, 0.1..0.999… -> 0; etc.
            int firstDigit = firstDigit(source);
            // Calculate how many decimal places we need to keep
            double increment = getRoundingIncrement(rp.maximumSignificantDigits, firstDigit);

            // Fix the source and upper/lowerbounds accordingly
            double converted;
            if (matchFormatted) {
                converted = roundByIncrement(source, increment);
            } else {
                converted = source;
            }
            // get the bounds
            double convertedDelta = 0.5 / increment;
            double lowerBound, upperBound;
            if (invertSource) {
                lowerBound = 1 / (converted + convertedDelta);
                upperBound = 1 / (converted - convertedDelta);
                converted = 1 / converted;
            } else {
                lowerBound = converted - convertedDelta;
                upperBound = converted + convertedDelta;
            }

            int last = conversionFactors.length - 1;

            for (int i = 0; i <= last; ++i) {
                double conversionFactor = conversionFactors[i];
                converted = converted * conversionFactor;
                lowerBound = lowerBound * conversionFactor;
                upperBound = upperBound * conversionFactor;
                if (i == last || converted > MAX_LONG_FROM_DOUBLE) {
                    // the final value is always a double
                    int bestScale = getBestDoubleScale(lowerBound, converted, upperBound);
                    result.setDouble(converted, bestScale - deltaMaxToMin, bestScale);
                    CheckResult debug = new CheckResult(this, source, rp, result);
                    break;
                } else {
                    double lowerBoundCeiling = Math.floor(lowerBound) + 1;
                    double upperBoundFloor = Math.ceil(upperBound) - 1;
                    if (lowerBoundCeiling < upperBoundFloor) {
                        // It is precise enough; there is an integer that works
                        // find the ones with the longest sequence of zeros
                        // among those, find the one closest to converted.
                        BestLong bestLong = getBestLongScale(lowerBound, converted, upperBound);
                        result.setDouble(
                                bestLong.value,
                                bestLong.significantDigits - deltaMaxToMin,
                                bestLong.significantDigits);
                        new CheckResult(this, source, rp, result);
                        break;
                    }
                    long floor = (long) Math.floor(converted);
                    result.addLong(floor);
                    lowerBound -= floor;
                    converted -= floor;
                    upperBound -= floor;
                }
            }
            if (negative) {
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

        private BestLong getBestLongScale(double lowerBound, double value, double upperbound) {
            if (!EXPERIMENTAL_ROUND) {
                long round = Math.round(value);
                if (lowerBound <= round && round <= upperbound) {
                    int sig = round == 0 ? 1 : firstDigit(round);
                    return new BestLong(round, sig);
                }
            }
            // Estimate an upper limit for E to start the downward scan
            double span = upperbound - lowerBound;
            int startE = firstDigit(span);

            // 3. Scan downwards from the top to guarantee finding the largest working E
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
                    int sigDigits = calculateSignificantDigits(bestK);
                    if (!(lowerBound < resultValue && resultValue < upperbound)) {
                        throw new UnitException("Result value fails: " + resultValue);
                    }
                    return new BestLong(resultValue, sigDigits);
                }
            }
            throw new UnitException("Should never happen");
        }

        /** find number of significant digits */
        private static int calculateSignificantDigits(long bestK) {
            // Avoid string allocation
            int length = 0;
            while (bestK > 0) {
                length++;
                bestK /= 10;
            }
            return length;
        }

        /**
         * Returns the number of significant digits of value needed to get within
         * (lowerBound..upperbound)<br>
         */
        private int getBestDoubleScale(double lowerBound, double value, double upperbound) {
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

        /**
         * This is just an example for now; real code would get the unit patterns and adjust for
         * plurals and case.
         */
        public String formatWithUnits(
                Processed rountResult, HackLocalizedNumberFormatter sourceNf) {
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
                Processed rountResult, HackLocalizedNumberFormatter sourceNf) {
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

        /**
         * Contains
         *
         * <ul>
         *   <li>zero or more leading values,
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
        public class Processed {
            private static final int MAX_MIXED_UNITS = 3;

            long[] leadingValues = null;
            int leadingCount = 0;
            double finalValue = Double.MIN_VALUE;
            int maxSignificant = Integer.MIN_VALUE;
            int minSignificant = Integer.MIN_VALUE;

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

            /** Set the final value, and sig digits */
            public void setDouble(double value, int minSig, int maxSig) {
                this.finalValue = value;
                this.minSignificant = minSig < 1 ? 1 : minSig;
                this.maxSignificant = maxSig < minSig ? minSig : maxSig;
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
        }
    }

    public static double roundToSignificant(double source, int significantDigits) {
        // Calculate how many decimal places we need to keep,
        // and get the right increment
        int firstDigit = firstDigit(source);
        double increment = getRoundingIncrement(significantDigits, firstDigit);
        return roundByIncrement(source, increment);
    }

    private static double getRoundingIncrement(int significantDigits, int firstDigit) {
        return Math.pow(10, afterLastDigit(significantDigits, firstDigit));
    }

    private static int afterLastDigit(int significantDigits, int firstDigit) {
        return significantDigits - firstDigit;
    }

    /** Round to a multiple of the increment. For example, (13, 5) => 15 */
    private static double roundByIncrement(double source, double increment) {
        return Math.round(source * increment) / increment;
    }

    /** The position of the lead digit. 1..9.99… -> 1; 10..99.99… -> 2, 0.1..0.999… -> -1; etc. */
    public static int firstDigit(double value) {
        return (int) Math.floor(Math.log10(value)) + 1;
    }

    public static double parseDouble(DecimalFormat df, String string) {
        try {
            return df.parse(string).doubleValue();
        } catch (ParseException e) {
            throw new ICUException("Illegal number", e);
        }
    }

    public static boolean approximatelyEqualRelative(double a, double b, double epsilon) {
        if (a == b) return true; // Handles infinities and exact matches
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) < epsilon;
    }

    static class UnitException extends RuntimeException {
        private static final long serialVersionUID = 302617249795260431L;

        public UnitException(String message) {
            super(message);
        }
    }
}
