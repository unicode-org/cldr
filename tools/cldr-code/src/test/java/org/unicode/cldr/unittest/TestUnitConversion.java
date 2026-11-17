package org.unicode.cldr.unittest;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.OutputInt;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.tool.InvestigateUnitRounding;
import org.unicode.cldr.tool.InvestigateUnitRounding.BoundDouble;
import org.unicode.cldr.tool.InvestigateUnitRounding.BoundDouble.PrecisionType;
import org.unicode.cldr.tool.InvestigateUnitRounding.Converter;
import org.unicode.cldr.tool.InvestigateUnitRounding.Converter.Converted;
import org.unicode.cldr.tool.InvestigateUnitRounding.HackLocalizedNumberFormatter;
import org.unicode.cldr.util.Joiners;

public class TestUnitConversion extends TestFmwk {

    // Settings for investigation
    private static final boolean MATCH_FORMATTED = true;
    private static final int lineToBreakAt = 0; // 0 for no lines
    private static final int maxSigSamples = 14;

    public static void main(String[] args) {
        new TestUnitConversion().run(args);
    }

    public void testARow() {
        System.out.println();
        String[][] tests = {
            {"10.185916357881302", "radian", "2", "degree"},
            //	{"0.3183098861837907", "pound-force-per-square-inch", "10", "bar"},
            //	{"10.185916357881302", "meter-per-second", "2", "minute-per-mile"},
        };

        for (String[] test : tests) {
            double amount = Double.parseDouble(test[0]);
            String sourceUnit = test[1];
            int sigDigit = Integer.parseInt(test[2]);
            String targetUnit = test[3];

            Converter converter = Converter.getConverter(sourceUnit, targetUnit, MATCH_FORMATTED);
            OutputInt line = new OutputInt();

            errorCount(line, converter, amount, sigDigit, sigDigit, true);
            errorCount(line, converter, amount, sigDigit, sigDigit, true);
        }
    }

    public void testSamples() {
        System.out.println();
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
            {"mile-per-gallon", "5", "liter-per-100-kilometer"},
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
                    errors += errorCount(line, converter, amount, minMax, minMax, false);
                    ok += 1 - errors;
                    totalErrors += errors;
                }
                int errors =
                        errorCount(line, converter, amount, maxSigSamples, maxSigSamples, false);
                ok += 1 - errors;
                totalErrors += errors;

                if (isVerbose() && lineToBreakAt <= 0) {
                    System.out.println();
                }
            }
            if (totalErrors != 0) {
                System.out.println("Errors: " + totalErrors + "\tOK: " + ok);
            }
        }
    }

    private int errorCount(
            OutputInt line,
            Converter converter,
            double sourceAmount,
            int minSig,
            int maxSig,
            boolean showYourWork) {
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

        Converted converted = converter.convertNew(sourceAmount, hackLnf);
        CheckResult check = new CheckResult(converter, sourceAmount, hackLnf, converted);

        //        Processed processed2 = converter.convert2(sourceAmount, hackLnf);
        //        if (!processed.equals(processed2)) {
        //        	warnln(line + "\t" + processed2 + " ≠ " + processed);
        //        }

        // Check the results
        String formattedWithUnits = converter.formatWithUnits(converted, hackLnf).toString();

        if (isVerbose() || !check.matches || showYourWork) {
            System.out.println(
                    Joiners.ES.join(
                            line.value,
                            "\t",
                            hackLnf,
                            "\t",
                            InvestigateUnitRounding.getPlainString(sourceAmount),
                            "\t«",
                            sourceFormatted,
                            " ",
                            converter.getSourceUnitId(),
                            "»",
                            "\t→ «",
                            formattedWithUnits,
                            "»",
                            "\t→ «",
                            check.getRoundTripFormatted(),
                            "»",
                            "\tConverted: ",
                            converted));
        }
        if (!check.matches) {
            double delta =
                    InvestigateUnitRounding.parseDouble(parser, check.getSourceFormatted())
                            - InvestigateUnitRounding.parseDouble(
                                    parser, check.getRoundTripFormatted());
            double raw = converter.convertBackRaw(converted);
            errln(
                    Joiners.ES.join(
                            "  FAILS ROUNDING ROUNDTRIP, source: ",
                            "«" + sourceFormatted + "»",
                            " roundtripping: «",
                            check.getRoundTripFormatted(),
                            "»",
                            " unrounded difference: ",
                            InvestigateUnitRounding.getPlainString(delta),
                            ", raw return convert: ",
                            InvestigateUnitRounding.getPlainString(raw)));
            return 1;
        }

        return 0;
    }

    public void testRounding() {
        double source = Math.PI * 1000;

        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(Locale.ENGLISH), 5, 5, Locale.ENGLISH);

        for (int sig = 1; sig <= 15; ++sig) {
            hackLnf = hackLnf.precision(sig, sig);
            String lnfFormatted = hackLnf.format(source).toString();
            double rounded = InvestigateUnitRounding.roundToSignificant(source, sig);
            String roundedValue = InvestigateUnitRounding.getPlainString(rounded);
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

    public void testBoundedDouble() {

        int sig = PrecisionType.significant.ordinal();
        int frc = PrecisionType.fraction.ordinal();
        double[][] tests = {
            {1234.5, sig, 5, 1234.45, 1234.5, 1234.55},
            {1234.5, frc, 1, 1234.45, 1234.5, 1234.55},
            {
                -1234.5, sig, 5, 1234.45, 1234.5, 1234.55,
            },
            {-1234.5, frc, 1, 1234.45, 1234.5, 1234.55},
            {0, sig, 5, -0.00005, 0, 0.00005},
            {0, frc, 1, -0.05, 0, 0.05},
        };
        for (double[] test : tests) {
            double value = test[0];
            PrecisionType type = PrecisionType.fromOrdinal(test[1]);
            int digits = (int) test[2];
            double expectedLower = test[3];
            double expectedValue = test[4];
            double expectedUpper = test[5];
            String msg = Arrays.stream(test).boxed().toList().toString();
            if (type == PrecisionType.fraction) {
                warnln(msg + " Fractional min/max digits not handled yet");
                continue;
            }

            BoundDouble bd = BoundDouble.fromDigits(value, type, digits);
            if (bd != null) {
                assertEquals(
                        msg + " negative", value > 0 ? 1 : value < 0 ? -1 : 0, bd.getNegative());
                assertEquals(msg + " lowerBound", expectedLower, bd.getLowerBound());
                assertEquals(msg + " value", expectedValue, bd.getValue());
                assertEquals(msg + " value", expectedUpper, bd.getUpperBound());
            } else {
                assertTrue(
                        "Value is either 0 or not finite: " + value,
                        value == 0 || Double.isFinite(value));
            }
        }

        warnln("Other API not tested yet");
        double[][] test2 = {{}};
    }

    public static class CheckResult {
        private final String sourceFormatted;
        final List<String> numbers;
        final double roundTrip;
        private final String roundTripFormatted;
        public final boolean matches;
        final double rawConvertBack;
        final double source;

        public CheckResult(
                Converter converter,
                double source,
                HackLocalizedNumberFormatter rp,
                Converted converted) {
            this.source = source;
            sourceFormatted = rp.lnf.format(source).toString();

            DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(rp.getLocale());

            // get the formatted numbers from the converted value, with the hackLnf
            // then convert to double
            // then format that double

            numbers = converter.formattedNumbers(converted, rp);
            rawConvertBack = converter.convertBackRaw(converted);
            roundTrip = converter.convertBack(numbers, parser);
            roundTripFormatted = rp.lnf.format(roundTrip).toString();
            matches = getSourceFormatted().equals(getRoundTripFormatted());
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "source:       \t",
                    InvestigateUnitRounding.getPlainString(source),
                    "\nsourceForm.:\t",
                    getSourceFormatted(),
                    "\nnumbers:    \t",
                    numbers,
                    "\nroundTrip:  \t",
                    InvestigateUnitRounding.getPlainString(roundTrip),
                    "\nrawCvttBack:\t",
                    InvestigateUnitRounding.getPlainString(rawConvertBack),
                    "\nrtFormatted:\t",
                    getRoundTripFormatted(),
                    "\nmatches:    \t",
                    matches);
        }

        public String getRoundTripFormatted() {
            return roundTripFormatted;
        }

        public String getSourceFormatted() {
            return sourceFormatted;
        }

        public boolean isMatches() {
            return matches;
        }
    }
}
