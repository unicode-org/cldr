package org.unicode.cldr.unittest;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.OutputInt;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.tool.FullUnitConverter;
import org.unicode.cldr.tool.FullUnitConverter.BoundRational;
import org.unicode.cldr.tool.FullUnitConverter.BoundRational.PrecisionType;
import org.unicode.cldr.tool.FullUnitConverter.Converted;
import org.unicode.cldr.tool.FullUnitConverter.HackLocalizedNumberFormatter;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Rational;

public class TestFullConversion extends TestFmwk {

    // Settings for investigation
    private static final int lineToBreakAt = 0; // 0 for no lines
    private static final int maxSigSamples = 14;

    public static void main(String[] args) {
        new TestFullConversion().run(args);
    }

    public void testARow() {
        System.out.println();
        String[][] tests = {
            {"10.185916357881302", "radian", "2", "degree"},
            //  {"0.3183098861837907", "pound-force-per-square-inch", "10", "bar"},
            //  {"10.185916357881302", "meter-per-second", "2", "minute-per-mile"},
        };

        for (String[] test : tests) {
            Rational amount = Rational.of(test[0]);
            String sourceUnit = test[1];
            int sigDigit = Integer.parseInt(test[2]);
            String targetUnit = test[3];

            FullUnitConverter converter = FullUnitConverter.getConverter(sourceUnit, targetUnit);
            OutputInt line = new OutputInt();
            errorCount(line, converter, amount, sigDigit, sigDigit, true);
            errorCount(line, converter, amount, sigDigit, sigDigit, true);
        }
    }

    public void testOddities() {
        System.out.println();
        Object[][] tests = {
            {Rational.NaN, "radian", 2, "degree", "NaN⟨2⟩"},
            {Rational.NEGATIVE_INFINITY, "radian", 2, "degree", "-INF⟨2⟩"},
            {Rational.INFINITY, "radian", 2, "degree", "INF⟨2⟩"},
            {Rational.ZERO, "radian", 2, "degree", "0⟨2⟩"},
            {Rational.NaN, "radian", 2, "per-radian", "NaN⟨2⟩"},
            {Rational.NEGATIVE_INFINITY, "radian", 2, "per-radian", "0⟨2⟩"},
            {Rational.INFINITY, "radian", 2, "per-radian", "0⟨2⟩"},
            {Rational.ZERO, "radian", 2, "per-radian", "INF⟨2⟩"},
        };

        Locale locale = new Locale("en");
        int testline = 0;

        for (Object[] test : tests) {
            Rational amount = (Rational) test[0];
            String sourceUnit = (String) test[1];
            int sigDigit = (int) test[2];
            String targetUnit = (String) test[3];
            String expected = (String) test[4];

            FullUnitConverter converter = FullUnitConverter.getConverter(sourceUnit, targetUnit);
            OutputInt line = new OutputInt();

            HackLocalizedNumberFormatter hackLnf =
                    new HackLocalizedNumberFormatter(
                            NumberFormatter.with().locale(locale), sigDigit, sigDigit, locale);

            Converted converted = converter.convert(amount, hackLnf);
            ++testline;

            assertEquals(
                    testline + " " + Joiners.COMMA_SP.join(Arrays.asList(test)),
                    expected,
                    converted.toString());
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

            FullUnitConverter converter = null;
            String errorMessage = null;

            try {
                converter = FullUnitConverter.getConverter(sourceUnit, targetUnit);
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
            Rational fuzzier = Rational.ONE.divide(Rational.of("3.14159265358979323846"));

            for (int sourceAmount = start; sourceAmount <= limit; sourceAmount += increment) {
                Rational amount = Rational.of(sourceAmount).multiply(fuzzier);

                for (int minMax = minSigTesting; minMax <= topPrecision; ++minMax) {
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
            FullUnitConverter converter,
            Rational sourceAmount,
            int minSig,
            int maxSig,
            boolean showYourWork) {
        line.value++;
        if (lineToBreakAt > 0 && lineToBreakAt != line.value) {
            return 0;
        }

        Locale locale = Locale.ENGLISH;

        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(locale), minSig, minSig, locale);

        String sourceFormatted = hackLnf.formatToString(sourceAmount);
        Converted converted = converter.convert(sourceAmount, hackLnf);
        CheckResult check = new CheckResult(converter, sourceAmount, hackLnf, converted);

        String formattedWithUnits = converter.formatWithUnits(converted, hackLnf).toString();

        if (isVerbose() || check.matches != CheckResult.Closeness.exact || showYourWork) {
            System.out.println(
                    Joiners.ES.join(
                            line.value,
                            "\t",
                            hackLnf,
                            "\t",
                            FullUnitConverter.getDecimalString(sourceAmount),
                            "\t",
                            FullUnitConverter.getPlainString(sourceAmount),
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

        if (check.matches != CheckResult.Closeness.exact) {
            Rational delta =
                    Rational.of(check.getSourceFormatted())
                            .subtract(Rational.of(check.getRoundTripFormatted()));
            Rational raw = converter.convertBackRaw(converted, false);
            String msg =
                    Joiners.ES.join(
                            " ROUNDING ROUNDTRIP, source: ",
                            "«" + sourceFormatted + "»",
                            " roundtripping: «",
                            check.getRoundTripFormatted(),
                            "»",
                            " unrounded difference: ",
                            FullUnitConverter.getPlainString(delta),
                            ", raw return convert: ",
                            FullUnitConverter.getPlainString(raw));
            if (check.matches == CheckResult.Closeness.near) {
                warnln(msg);
                return 0;
            } else {
                errln(msg);
                return 1;
            }
        }
        return 0;
    }

    public void testRounding() {
        Rational source = Rational.of(Math.PI).multiply(Rational.of(1000));
        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(Locale.ENGLISH), 5, 5, Locale.ENGLISH);

        for (int sig = 1; sig <= 15; ++sig) {
            hackLnf = hackLnf.precision(sig, sig);
            String lnfFormatted = hackLnf.formatToString(source);
            Rational rounded = FullUnitConverter.roundToSignificant(source, sig);
            String roundedValue = FullUnitConverter.getPlainString(rounded);
            System.out.println(Joiners.COMMA_SP.join(sig, lnfFormatted, roundedValue));
        }
    }

    public void testBoundedRational() {
        int sig = PrecisionType.significant.ordinal();
        int frc = PrecisionType.fraction.ordinal();

        Object[][] tests = {
            {
                Rational.of("1234.5"),
                sig,
                5,
                Rational.of("1234.45"),
                Rational.of("1234.5"),
                Rational.of("1234.55")
            },
            {
                Rational.of("1234.5"),
                frc,
                1,
                Rational.of("1234.45"),
                Rational.of("1234.5"),
                Rational.of("1234.55")
            },
            {
                Rational.of("-1234.5"),
                sig,
                5,
                Rational.of("1234.45"),
                Rational.of("1234.5"),
                Rational.of("1234.55")
            },
            {
                Rational.of("-1234.5"),
                frc,
                1,
                Rational.of("1234.45"),
                Rational.of("1234.5"),
                Rational.of("1234.55")
            },
            {Rational.ZERO, sig, 5, Rational.of("-0.00005"), Rational.ZERO, Rational.of("0.00005")},
            {Rational.ZERO, frc, 1, Rational.of("-0.05"), Rational.ZERO, Rational.of("0.05")},
        };

        for (Object[] test : tests) {
            Rational value = (Rational) test[0];
            PrecisionType type = PrecisionType.fromOrdinal((Number) test[1]);
            int digits = (int) test[2];
            Rational expectedLower = (Rational) test[3];
            Rational expectedValue = (Rational) test[4];
            Rational expectedUpper = (Rational) test[5];

            String msg = Arrays.toString(test);
            if (type == PrecisionType.fraction) {
                warnln(msg + " Fractional min/max digits not handled yet");
                continue;
            }

            BoundRational bd = BoundRational.fromDigits(value, type, digits);
            if (bd != null) {
                int expectedNeg =
                        value.compareTo(Rational.ZERO) > 0
                                ? 1
                                : value.compareTo(Rational.ZERO) < 0 ? -1 : 0;
                assertEquals(msg + " negative", expectedNeg, bd.getNegative());
                assertEquals(msg + " lowerBound", expectedLower, bd.getLowerBound());
                assertEquals(msg + " value", expectedValue, bd.getValue());
                assertEquals(msg + " upperBound", expectedUpper, bd.getUpperBound());
            } else {
                assertTrue(
                        "Value is either 0 or not finite: " + value,
                        value.equals(Rational.ZERO) || !Double.isFinite(value.doubleValue()));
            }
        }
    }

    public static class CheckResult {

        public enum Closeness {
            exact,
            near,
            different
        }

        private final String sourceFormatted;
        final List<String> numbers;
        final Rational roundTrip;
        private final String roundTripFormatted;
        public final Closeness matches;
        final Rational rawConvertBack;
        final Rational source;

        public CheckResult(
                FullUnitConverter converter,
                Rational source,
                HackLocalizedNumberFormatter rp,
                Converted converted) {
            this.source = source;
            sourceFormatted = rp.lnf.format(source.doubleValue()).toString();

            numbers = converter.formattedNumbers(converted, rp);
            rawConvertBack = converter.convertBackRaw(converted, false);
            roundTrip = converter.convertBack(numbers);
            roundTripFormatted = rp.lnf.format(roundTrip.doubleValue()).toString();
            matches = matches(getSourceFormatted(), getRoundTripFormatted());
        }

        private static final BigDecimal HALF = new BigDecimal("0.5");

        private Closeness matches(String aString, String bString) {
            if (aString.equals(bString)) {
                return Closeness.exact;
            }

            BigDecimal a = new BigDecimal(aString);
            BigDecimal b = new BigDecimal(bString);

            if (a == null || b == null) {
                throw new IllegalArgumentException("Inputs must not be null");
            }

            if (a.compareTo(b) == 0) {
                return Closeness.exact;
            }

            BigDecimal halfUlpA = HALF.movePointLeft(a.scale());
            BigDecimal halfUlpB = HALF.movePointLeft(b.scale());
            BigDecimal diff = a.subtract(b).abs();

            BigDecimal minHalfUlp = halfUlpA.max(halfUlpB);
            return diff.compareTo(minHalfUlp) <= 0 ? Closeness.near : Closeness.different;
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "source:       \t",
                    FullUnitConverter.getPlainString(source),
                    "\nsourceForm.:\t",
                    getSourceFormatted(),
                    "\nnumbers:    \t",
                    numbers,
                    "\nroundTrip:  \t",
                    FullUnitConverter.getPlainString(roundTrip),
                    "\nrawCvttBack:\t",
                    FullUnitConverter.getPlainString(rawConvertBack),
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

        public Closeness isMatches() {
            return matches;
        }
    }
}
