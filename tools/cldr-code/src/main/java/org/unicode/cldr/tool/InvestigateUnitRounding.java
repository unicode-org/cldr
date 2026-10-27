package org.unicode.cldr.tool;

import com.google.common.primitives.Doubles;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.InvestigateUnitRounding.Converter.RawConverted;
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

    private static final UnitConverter UNIT_CONVERTER =
            SupplementalDataInfo.getInstance().getUnitConverter();

    public static void main(String[] args) {

        checkRounding();

        final int minSigTesting = 1;
        String[][] samples = {
            // first, some checks that errors are correctly thrown
            {
                "furlong",
                "1",
                "meter-and-fortnight",
                "Mismatched baseUnitIds for furlong&fortnight: meter≠second"
            },
            {"meter", "1", "foot-and-yard", "Bad mixed units: foot (381/1250) <= yard (1143/1250)"},
            {"meter", "1", "foot-and-foot", "Bad mixed units: foot (381/1250) <= foot (381/1250)"},
            // now some real values
            {"meter", "3", "yard"},
            {"meter", "3", "yard-and-foot"},
            {"radian", "6", "degree-and-arc-minute-and-arc-second"},
            {"week", "5", "day-and-hour-and-minute"},
            // not yet working right
            // {"radian", "3", "degree-and-arc-minute-and-arc-second"},
        };
        for (String[] sample : samples) {
            String sourceUnit = sample[0];
            int top = Integer.parseInt(sample[1]);
            String targetUnit = sample[2];
            String expectedError = sample.length < 4 ? null : sample[3];

            System.out.println("Sample: " + Arrays.asList(sample));

            Converter converter = null;
            String errorMessage = null;
            try {
                converter = Converter.getConverter(sourceUnit, targetUnit);
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

            System.out.println(Joiners.SP.join("Converter: ", converter) + "\n");
            int totalErrors = 0;
            int ok = 0;
            for (int sourceAmount = 0; sourceAmount < 30; ++sourceAmount) {
                for (int minMax = minSigTesting; minMax <= top; ++minMax) {
                    // for now, min=max
                    double amount = sourceAmount / Math.PI;
                    int errors = 0;
                    // errors += errorCount(converter, amount - 0.000001, minMax, minMax);
                    errors += errorCount(converter, amount, minMax, minMax);
                    // errors += errorCount(converter, amount + 0.000001, minMax, minMax);
                    ok += 3 - errors;
                    totalErrors += errors;
                }
                if (VERBOSE) {
                    System.out.println();
                }
            }
            if (totalErrors != 0) {
                System.out.println("Errors: " + totalErrors + "\tOK: " + ok);
            }
        }
    }

    private static void checkRounding() {

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
            Converter converter, double sourceAmount, int minSig, int maxSig) {
        Precision precision = Precision.minMaxSignificantDigits(minSig, minSig);

        Locale locale = Locale.ENGLISH;
        LocalizedNumberFormatter nf = NumberFormatter.with().locale(locale);

        // Unfortunately, the ICU APIs are more limited than we need, because:
        // You can't get the precision back out of a NumberFormatter
        // Nor the settings from a Precision (like minSigDigits)
        // nor can you get DecimalFormatProperties from a NumberFormatter!

        LocalizedNumberFormatter lnf = nf.precision(precision);
        // Use hack to get precision from LocalizedNumberFormatter
        ReadablePrecision readablePrecision = new ReadablePrecision(lnf);

        // We have to guess that the parser parallels the formatter:
        // toFormat() can't be used to build a DecimalFormat.

        DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(locale);

        // example 1.1 meters => 1.20297 yards => 1 yard 0.60891 feet (but then rounded
        String sourceFormatted = lnf.format(sourceAmount).toString();
        RawConverted rawConverted = converter.convert(sourceAmount, readablePrecision);
        double roundTripUnrounded = converter.convertBackUnrounded(rawConverted);

        boolean approximatelyEqual =
                approximatelyEqualRelative(sourceAmount, roundTripUnrounded, 0.000_000_000_000_001);

        List<String> numbers = converter.formattedNumbers(rawConverted, lnf);

        //            showInfo2(meterToYardAndFoot, numbers);

        String formattedWithUnits = converter.formatWithUnits(rawConverted, lnf).toString();

        double roundTrip = converter.convertBack(numbers, parser);
        String roundTripFormatted = lnf.format(roundTrip).toString();

        boolean roundedEqual = roundTripFormatted.equals(sourceFormatted);

        if (VERBOSE || !approximatelyEqual || !roundedEqual) {
            System.out.println(
                    Joiners.ES.join(
                            readablePrecision,
                            "\t",
                            BigDecimal.valueOf(sourceAmount).toPlainString(),
                            "\t«",
                            sourceFormatted,
                            " ",
                            converter.sourceUnitId,
                            "»",
                            "\t→ «",
                            formattedWithUnits,
                            "»",
                            "\tRoundTrip unrounded: ",
                            BigDecimal.valueOf(roundTripUnrounded).toPlainString(),
                            " formatted: ",
                            "«" + roundTripFormatted + "»" + " Raw converted: ",
                            rawConverted));
        }
        if (false && !approximatelyEqual) {
            converter.convert(sourceAmount, readablePrecision);
            // showInfo1(converter, rawConverted);
            System.out.println(
                    Joiners.SP.join(
                            "  FAILS NUMERIC ROUNDTRIP, source: ",
                            sourceAmount,
                            "result:",
                            roundTripUnrounded));
            return 1;
        }
        if (!roundedEqual) {
            double delta = sourceAmount - roundTrip;
            System.out.println(
                    Joiners.ES.join(
                            "  FAILS ROUNDING ROUNDTRIP, source: ",
                            "«" + sourceFormatted + "»",
                            " roundtripping: «",
                            roundTripFormatted,
                            "»",
                            " unrounded difference: ",
                            BigDecimal.valueOf(delta).toPlainString()));
            return 1;
        }
        return 0;
    }

    /**
     * Hack because you can't get a Precision from a NumberFormatter, nor can you get the various
     * settings (like minimumSignificantDigits) from a Precision, nor can you get
     * DecimalFormatProperties from a NumberFormatter!
     */
    private static class ReadablePrecision {
        public final int minimumSignificantDigits;
        public final int maximumSignificantDigits;

        public ReadablePrecision(LocalizedNumberFormatter lnf) {
            // Painful extraction; we have to convert to a skeleton and back;
            // gotta be a waste of cycles.
            String skeleton = lnf.toSkeleton();
            DecimalFormatProperties dfp = new DecimalFormatProperties();
            PatternStringParser.parseToExistingProperties(skeleton, dfp);
            this.minimumSignificantDigits = dfp.getMinimumSignificantDigits();
            this.maximumSignificantDigits = dfp.getMaximumSignificantDigits();
        }

        @Override
        public boolean equals(Object obj) {
            ReadablePrecision that = (ReadablePrecision) obj;
            return minimumSignificantDigits == that.minimumSignificantDigits
                    && maximumSignificantDigits == that.maximumSignificantDigits;
        }

        @Override
        public int hashCode() {
            return Objects.hash(minimumSignificantDigits, maximumSignificantDigits);
        }

        @Override
        public String toString() {
            return "{p:"
                    + minimumSignificantDigits
                    + (minimumSignificantDigits == maximumSignificantDigits
                            ? ""
                            : "," + maximumSignificantDigits)
                    + "}";
        }
    }

    /**
     * Just an API for investigation; a real API would look different. Not optimized, and has extra
     * API for checking steps.
     */
    public static class Converter {
        private String sourceUnitId;
        private double[] conversionFactors;
        private String[] unitIds;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(sourceUnitId).append(" raw: ");

            double[] convertBoth = new double[conversionFactors.length];

            double current = 1;

            for (int i = 0; i < conversionFactors.length; ++i) {
                if (i != 0) {
                    sb.append(" rem");
                }
                double conversionFactor = conversionFactors[i];
                sb.append("÷").append(conversionFactor).append("=").append(unitIds[i]);
                current *= conversionFactor;
                convertBoth[i] = current;
            }
            sb.append("; in source Units: ");

            for (int i = 0; i < convertBoth.length; ++i) {
                if (i != 0) {
                    sb.append(", ");
                }
                double conversionFactor = convertBoth[i];
                sb.append(conversionFactor).append(" ").append(unitIds[i]);
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

        public static Converter getConverter(String sourceUnit, String targetUnit) {
            List<Double> conversions = new ArrayList<>();
            List<String> units = Splitter.on("-and-").splitToList(targetUnit);

            // Converter meterToYardAndFoot = new Converter(1d / 0.9144d, "yard", 3d, "foot");

            // Check units: must be value units, and if mixed, must be descending in value
            Output<String> baseUnitId = new Output<>();
            ConversionInfo info = UNIT_CONVERTER.parseUnitId(sourceUnit, baseUnitId, false);
            String sourceBaseUnitId = baseUnitId.value;
            Rational lastFactor = null;
            String lastUnit = null;
            for (String unit : units) {
                info = UNIT_CONVERTER.parseUnitId(unit, baseUnitId, false);
                if (baseUnitId.value == null) {
                    throw new IllegalArgumentException("Unit not recognized: " + unit);
                }
                if (!baseUnitId.value.equals(sourceBaseUnitId)) {
                    throw new IllegalArgumentException(
                            "Mismatched baseUnitIds for "
                                    + sourceUnit
                                    + "&"
                                    + unit
                                    + ": "
                                    + sourceBaseUnitId
                                    + "≠"
                                    + baseUnitId.value);
                }

                if (lastFactor == null) {
                    lastFactor = info.factor;
                    lastUnit = unit;
                } else if (info.factor.compareTo(lastFactor) >= 0) {
                    throw new IllegalArgumentException(
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
            return new Converter(sourceUnit, conversions, units);
        }

        public Converter(String sourceUnit, List<Double> conversions, List<String> unitsIds) {
            this.sourceUnitId = sourceUnit;
            this.conversionFactors = Doubles.toArray(conversions);
            this.unitIds = unitsIds.toArray(String[]::new);
        }

        public double convertBack(List<String> numbers, DecimalFormat df) {
            try {
                double result = 0;
                for (int i = numbers.size() - 1; i >= 0; --i) {
                    double parsed = df.parse(numbers.get(i)).doubleValue();
                    double conversionFactor = conversionFactors[i];
                    result = (result + parsed) / conversionFactor;
                }
                return result;
            } catch (ParseException e) {
                throw new ICUException("Illegal number", e);
            }
        }

        public double convertBackUnrounded(RawConverted roundResult) {
            double conversionFactor = conversionFactors[roundResult.leadingCount];
            double result = roundResult.finalValue / conversionFactor;
            for (int i = roundResult.leadingCount - 1; i >= 0; --i) {
                conversionFactor = conversionFactors[i];
                result = (result + roundResult.leadingValues[i]) / conversionFactor;
            }
            return result;
        }

        public RawConverted convert(double source, ReadablePrecision rp) {
            RawConverted result = new RawConverted();

            int maxSig = rp.maximumSignificantDigits;
            int minSig = rp.minimumSignificantDigits;

            if (source == 0) {
                result.setDouble(0, 1, 1);
                return result;
            }

            boolean negative = source < 0;
            if (negative) {
                source = -source;
            }
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

            // TBD handle 0

            // Determine the magnitude of the number
            double magnitude = Math.ceil(Math.log10(source));
            // Calculate how many decimal places we need to keep
            int scale = rp.maximumSignificantDigits - (int) magnitude;
            double magnitude2 = Math.pow(10, scale);
            // Fix the source and delta accordingly
            double converted = Math.round(source * magnitude2) / magnitude2;
            double convertedDelta = 0.5 / magnitude2; // get scaled 0.5;

            int last = conversionFactors.length - 1;

            for (int i = 0; i <= last; ++i) {
                double conversionFactor = conversionFactors[i];
                converted = converted * conversionFactor;
                convertedDelta = convertedDelta * conversionFactor;
                if (i == last) { // the final value remains a double
                    maxSig = maxSig(converted, convertedDelta);
                    result.setDouble(converted, maxSig - deltaMaxToMin, maxSig);
                    break;
                } else {
                    long rounded = Math.round(converted);
                    double lowerBound = converted - convertedDelta;
                    double upperbound = converted + convertedDelta;
                    if (lowerBound < rounded && rounded < upperbound) {
                        maxSig = maxSig(converted, convertedDelta);
                        result.setDouble(converted, maxSig - deltaMaxToMin, maxSig);
                        break;
                    }
                    double floor = Math.floor(converted);
                    result.addLong(floor);
                    converted = converted - floor;
                }
            }
            if (negative) {
                result.negate();
            }
            return result;
        }

        /**
         * Used to counteract loss of precision; nudge the value up very slightly in case it is very
         * near a multiple of 10.
         */
        private static final double FUDGE_FACTOR = 1.000_000_000_000_01;

        /**
         * Returns the number of significant digits needed to get within delta<br>
         * TBD WARNING Need to refine the code further, with the goal being to get just enough
         * precision to roundtrip.
         */
        private int maxSig(double value, double delta) {
            // Find the position of the first significant digit of values using log10
            // use a fudge factor so that we err slightly on the side of more significant digits!
            int valueFirstSigDigitPlace = (int) Math.ceil(Math.log10(value * FUDGE_FACTOR));
            int deltaFirstSigDigitPlace = (int) Math.floor(Math.log10(delta));

            int targetScale = valueFirstSigDigitPlace - deltaFirstSigDigitPlace;
            if (targetScale <= 0) {
                targetScale += 1;
            }
            return targetScale;
        }

        /**
         * This is just an example for now; real code would get the unit patterns and adjust for
         * plurals and case.
         */
        public String formatWithUnits(RawConverted rountResult, LocalizedNumberFormatter sourceNf) {
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
                RawConverted rountResult, LocalizedNumberFormatter sourceNf) {
            List<String> result = new ArrayList<>();
            if (rountResult.leadingCount != 0) {
                LocalizedNumberFormatter dfInteger = sourceNf.precision(Precision.integer());
                for (int i = 0; i < rountResult.leadingCount; ++i) {
                    result.add(dfInteger.format(rountResult.leadingValues[i]).toString());
                }
            }
            Precision finalPrecision =
                    Precision.minMaxSignificantDigits(
                            rountResult.maxSignificant, rountResult.maxSignificant);
            LocalizedNumberFormatter dfFinal = sourceNf.precision(finalPrecision);
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
        public class RawConverted {
            private static final int MAX_MIXED_UNITS = 3;

            long[] leadingValues = null;
            int leadingCount = 0;
            double finalValue;
            int maxSignificant;
            int minSignificant;

            @Override
            public String toString() {
                // TODO Auto-generated method stub
                return (leadingValues == null
                                ? "{"
                                : "{leading: "
                                        + Arrays.stream(leadingValues)
                                                .limit(leadingCount)
                                                .boxed()
                                                .collect(Collectors.toList())
                                        + ", ")
                        + Joiners.ES.join(
                                "final: ",
                                finalValue,
                                ", minMaxSig: ",
                                maxSignificant,
                                ",",
                                minSignificant)
                        + "}";
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

    public static boolean approximatelyEqualRelative(double a, double b, double epsilon) {
        if (a == b) return true; // Handles infinities and exact matches
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) < epsilon;
    }
}
