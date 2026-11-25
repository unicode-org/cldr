package org.unicode.cldr.unittest;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.OutputInt;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.FullUnitConverter;
import org.unicode.cldr.util.FullUnitConverter.BoundedNumber;
import org.unicode.cldr.util.FullUnitConverter.BoundedNumber.PrecisionType;
import org.unicode.cldr.util.FullUnitConverter.Converted;
import org.unicode.cldr.util.FullUnitConverter.HackLocalizedNumberFormatter;
import org.unicode.cldr.util.FullUnitConverter.Sign;
import org.unicode.cldr.util.FullUnitConverter.UnitConverterPU;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;

public class TestFullConversion extends TestFmwk {

    // Settings for investigation
    private static final int lineToBreakAt = 0; // 0 for no lines
    private static final int maxSigSamples = 14;

    public static void main(String[] args) {
        new TestFullConversion().run(args);
    }

    static final UnitConverterPU ucpu =
            new UnitConverterPU().addPrivateUnit("xxx-private", "meter", Rational.of("3/5"));

    public void testARow() {
        System.out.println();
        String[][] tests = {
            {"2.1", "bar", "20", "pound-force-per-square-inch"},
            {"10.185916357881302", "radian", "2", "degree"},
            //  {"0.3183098861837907", "pound-force-per-square-inch", "10", "bar"},
            //  {"10.185916357881302", "meter-per-second", "2", "minute-per-mile"},
        };

        for (String[] test : tests) {
            Rational amount = Rational.of(test[0]);
            String sourceUnit = test[1];
            int sigDigit = Integer.parseInt(test[2]);
            String targetUnit = test[3];

            FullUnitConverter converter =
                    FullUnitConverter.getConverter(sourceUnit, targetUnit, ucpu);
            OutputInt line = new OutputInt();
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

        Locale locale = Locale.ENGLISH;
        int testline = 0;

        for (Object[] test : tests) {
            Rational amount = (Rational) test[0];
            String sourceUnit = (String) test[1];
            int sigDigit = (int) test[2];
            String targetUnit = (String) test[3];
            String expected = (String) test[4];

            FullUnitConverter converter =
                    FullUnitConverter.getConverter(sourceUnit, targetUnit, ucpu);
            OutputInt line = new OutputInt();

            HackLocalizedNumberFormatter hackLnf =
                    new HackLocalizedNumberFormatter(
                            NumberFormatter.with().locale(locale), locale, sigDigit, sigDigit);

            Converted converted = converter.convert(amount, hackLnf);
            ++testline;

            assertEquals(
                    testline + " " + Joiners.COMMA_SP.join(Arrays.asList(test)),
                    expected,
                    converted.toString());
        }
    }

    static class Progress {
        int errors;
        int ok;
    }

    public void testSamples() {
        System.out.println();

        String[][] samples = {
            {"Individual test cases"},
            {"xxx-private", "5", "meter", null, "1.225"},
            {"meter", "5", "foot", null, "2.1", "0.9"},
            {"Deliberate errors"},
            {
                "furlong",
                "1",
                "meter-and-fortnight",
                "Mismatched baseUnitIds for furlong&fortnight: meter≠second"
            },
            {"meter", "1", "foot-and-yard", "Bad mixed units: foot (381/1250) <= yard (1143/1250)"},
            {"meter", "1", "foot-and-foot", "Bad mixed units: foot (381/1250) <= foot (381/1250)"},
            {"Unmixed units"},
            {"pound-force-per-square-inch", "5", "bar"},
            {"meter", "5", "yard"},
            {"degree", "6", "radian"},
            {"Mixed units"},
            {"meter", "3", "yard-and-foot"},
            {"radian", "6", "degree-and-arc-minute-and-arc-second"},
            {"week", "5", "day-and-hour-and-minute"},
            {"kilometer", "6", "mile-and-rod-and-foot-and-inch"},
            {"Inverse units"},
            {"meter-per-second", "5", "minute-per-mile"},
            {"mile-per-gallon", "5", "liter-per-100-kilometer"},
            {"meter-per-second", "5", "minute-per-mile-and-second-per-mile"},
        };

        OutputInt line = new OutputInt(1);
        List<Rational> defaultSamples2 = getDefaultSamples();
        Progress totalErrors = new Progress();

        for (String[] sample : samples) {
            String sourceUnit = sample[0];
            if (sample.length == 1) {
                System.out.println("*" + sourceUnit); // header
                continue;
            }
            int topPrecision = Integer.parseInt(sample[1]);
            String targetUnit = sample[2];
            String expectedError = sample.length < 4 ? null : sample[3];
            List<Rational> explicitSamples = new ArrayList<>();
            for (int i = 4; i < sample.length; ++i) {
                explicitSamples.add(Rational.of(sample[i]));
            }
            List<Rational> defaultSamples =
                    explicitSamples.isEmpty() ? defaultSamples2 : explicitSamples;

            System.out.println("Sample: " + Arrays.asList(sample));

            FullUnitConverter converter = null;
            String errorMessage = null;

            try {
                converter = FullUnitConverter.getConverter(sourceUnit, targetUnit, ucpu);
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
            FullUnitConverter backConverter = converter.getInverseConverter();

            System.out.println("Converter: " + converter + "\n");

            for (Rational amount : defaultSamples) {
                checkErrors(line, amount, converter, 1, topPrecision, totalErrors);
                checkErrors(line, amount, backConverter, 1, topPrecision, totalErrors);
            }
        }
        System.out.println("Errors: " + totalErrors.errors + "\tOK: " + totalErrors.ok);
    }

    private void checkErrors(
            OutputInt line,
            Rational amount,
            FullUnitConverter converter,
            final int minSigTesting,
            int topPrecision,
            Progress totalErrors) {
        if (isVerbose()) {
            System.out.println(
                    Joiners.ES.join(
                            "Decimal:\t",
                            FullUnitConverter.toDecimalString(amount, MathContext.DECIMAL128),
                            "\nRational: \t",
                            amount));
        }

        int ok = 0;
        for (int minMax = minSigTesting; minMax <= topPrecision; ++minMax) {
            int errors = 0;
            errors += errorCount(line, converter, amount, minMax, minMax, false);
            ok += 1 - errors;
            totalErrors.ok += ok;
            totalErrors.errors += errors;
        }

        int errors = errorCount(line, converter, amount, maxSigSamples, maxSigSamples, false);
        ok += 1 - errors;
        totalErrors.ok += ok;
        totalErrors.errors += errors;

        if (isVerbose() && lineToBreakAt <= 0) {
            System.out.println();
        }
    }

    private List<Rational> getDefaultSamples() {
        List<Rational> defaultSamples_ = new ArrayList<>();
        {
            int start = 0;
            int increment = 1;
            int limit = start + 32 * increment;
            Rational fuzzier = Rational.ONE.divide(Rational.of("3.14159265358979323846"));
            for (int sourceAmount = start; sourceAmount <= limit; sourceAmount += increment) {
                defaultSamples_.add(Rational.of(sourceAmount).multiply(fuzzier));
            }
        }
        List<Rational> defaultSamples = List.copyOf(defaultSamples_);
        return defaultSamples;
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
        if (maxSig >= 20) {
            Converted converted = converter.convertRaw(sourceAmount);
            System.out.println(
                    Joiners.ES.join(
                            line.value,
                            "\t«",
                            sourceAmount,
                            " ",
                            converter.getSourceUnitId(),
                            "»",
                            "\t→ «",
                            converted,
                            " ",
                            converter.getTargetUnitId(),
                            "»"));
        }
        HackLocalizedNumberFormatter hackLnf =
                new HackLocalizedNumberFormatter(
                        NumberFormatter.with().locale(locale), locale, minSig, minSig);

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
                            " ROUNDTRIP to source format",
                            "\n\tconverter: ",
                            converter,
                            "\n\tlog: ",
                            converted.getLog() + "\n\tsource: ",
                            "«" + sourceFormatted + "»",
                            " roundtripping: «",
                            check.getRoundTripFormatted(),
                            "»",
                            " unrounded difference: ",
                            delta.toString(FormatStyle.repeating),
                            ", raw return convert: ",
                            raw.toString(FormatStyle.repeating));
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
                        NumberFormatter.with().locale(Locale.ENGLISH), Locale.ENGLISH, 5, 5);

        for (int sig = 1; sig <= 15; ++sig) {
            hackLnf = hackLnf.precision(sig, sig);
            String lnfFormatted = hackLnf.formatToString(source);
            Rational rounded = source.roundToSignificant(sig);
            String roundedValue = FullUnitConverter.toPlainString(rounded);
            System.out.println(Joiners.COMMA_SP.join(sig, lnfFormatted, roundedValue));
        }
    }

    public void testBoundedRational() {

        String[][] tests = {
            // value, precisionType, sig. Digits, lowerBound, value, upperBound
            {"1234.5", "significant", "5", "1234.45", "1234.5", "1234.55"},
            {"1234.5", "fraction", "1", "1234.45", "1234.5", "1234.55"},
            {"-1234.5", "significant", "5", "1234.45", "1234.5", "1234.55"},
            {"-1234.5", "fraction", "1", "1234.45", "1234.5", "1234.55"},
            {"0", "significant", "5", "-0.00005", "0", "0.00005"},
            {"0", "fraction", "1", "-0.05", "0", "0.05"}
        };
        for (String[] test : tests) {
            Rational value = Rational.of(test[0]);
            PrecisionType type = PrecisionType.valueOf(test[1]);
            int digits = Integer.valueOf(test[2]);
            Rational expectedLower = Rational.of(test[3]);
            Rational expectedValue = Rational.of(test[4]);
            Rational expectedUpper = Rational.of(test[5]);

            String msg = Arrays.toString(test);
            if (type == PrecisionType.fraction) {
                warnln(msg + " Fractional min/max digits not handled yet");
                continue;
            }

            BoundedNumber bd = BoundedNumber.from(value, type, digits);
            if (bd != null) {
                Sign expectedNeg = value.signum() < 0 ? Sign.negative : Sign.positive;
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

    public void testRoundByIncrement() {
        String[][] tests = {
            // source, increment, result
            {"100/3", "100", "0"},
            {"100/3", "10", "30"},
            {"100/3", "1", "33"},
            {"100/3", "0.1", "33.3"},
            {"100/3", "0.01", "33.33"},
        };
        for (String[] test : tests) {
            Rational source = Rational.of(test[0]);
            Rational increment = Rational.of(test[1]);
            Rational expected = Rational.of(test[2]);

            Rational actual = source.roundByIncrement(increment);
            assertEquals(
                    Joiners.ES.join(
                            "src: ", source.doubleValue(), " inc: ", increment.doubleValue()),
                    expected,
                    actual);
        }
    }

    public void oldTestToSignificantDigits() {
        String[][] tests = {
            // sourceValue, conversion
            {"12345", "1.1", "4"},
            {"49/24", "3/5"},
            {"12345", "1.1"},
        };

        for (String[] test : tests) {
            Rational sourceValue = Rational.of(test[0]);
            Rational conversion = Rational.of(test[1]);
            int sigStart = test.length < 3 ? 1 : Integer.parseInt(test[2]);
            int sigEnd = test.length < 3 ? 6 : sigStart;

            ucpu.addPrivateUnit("xxx-private", "meter", conversion);
            FullUnitConverter converter =
                    FullUnitConverter.getConverter("xxx-private", "meter", ucpu);

            for (int sourceSigDigits = sigStart; sourceSigDigits <= sigEnd; ++sourceSigDigits) {

                BoundedNumber sourceBounded =
                        BoundedNumber.from(sourceValue, PrecisionType.significant, sourceSigDigits);

                Converted result = new Converted();
                converter.convert(sourceBounded, 0, result);

                BoundedNumber targetBounded = sourceBounded.clone().multiply(conversion);

                int targetSigDig = targetBounded.toSignificantDigits();

                String targetBDRounded =
                        roundWithBigDecimal(targetBounded.getValue(), targetSigDig);

                // now reverse to get back to source unit space, and round it
                boolean error = false;
                String roundTripSourceBDRounded =
                        getRoundtripRoundedSource(
                                sourceSigDigits, conversion, targetBounded, targetSigDig);
                String sourceBDRounded = roundWithBigDecimal(sourceValue, sourceSigDigits);
                error |=
                        assertEquals(
                                showMessage(sourceSigDigits, targetSigDig, targetBDRounded),
                                sourceBDRounded,
                                roundTripSourceBDRounded);

                if (targetSigDig > 1) {
                    int targetSigDigMinus1 = targetSigDig - 1;
                    String targetRoundedBGMinus =
                            roundWithBigDecimal(targetBounded.getValue(), targetSigDigMinus1);
                    String roundTripSourceRoundedBGMinus =
                            getRoundtripRoundedSource(
                                    sourceSigDigits, conversion, targetBounded, targetSigDigMinus1);
                    error |=
                            assertEquals(
                                    "sd-1: "
                                            + showMessage(
                                                    sourceSigDigits,
                                                    targetSigDigMinus1,
                                                    targetRoundedBGMinus),
                                    sourceBDRounded,
                                    roundTripSourceRoundedBGMinus);
                }
                {
                    int targetSigDigitsPlus1 = targetSigDig + 1;
                    String targetRoundedBGMinus =
                            roundWithBigDecimal(targetBounded.getValue(), targetSigDigitsPlus1);
                    String roundTripSourceRoundedBGMinus =
                            getRoundtripRoundedSource(
                                    sourceSigDigits,
                                    conversion,
                                    targetBounded,
                                    targetSigDigitsPlus1);
                    error |=
                            assertEquals(
                                    "sd+1: "
                                            + showMessage(
                                                    sourceSigDigits,
                                                    targetSigDigitsPlus1,
                                                    targetRoundedBGMinus),
                                    sourceBDRounded,
                                    roundTripSourceRoundedBGMinus);
                }

                if (error || isVerbose())
                    System.out.println(
                            Joiners.ES.join(
                                    "\tConv. Factor:\t",
                                    conversion.doubleValue(),
                                    "\n\t\tSource\tTarget",
                                    "\n\tValue:   \t",
                                    sourceValue.doubleValue(),
                                    "\t",
                                    targetBounded.getValue().toString(FormatStyle.repeating),
                                    "\n\tSigDig:  \t",
                                    sourceSigDigits,
                                    "\t",
                                    targetSigDig,
                                    "\n\tRounded: \t«",
                                    sourceBDRounded,
                                    "»\t«",
                                    targetBDRounded,
                                    "»\n\tRTRounded:\t«",
                                    roundTripSourceBDRounded,
                                    "»"));
            }
        }
    }

    private String showMessage(int sourceSigDigits, int targetSigDig, String targetRoundedBG) {
        return "SourceSG: "
                + sourceSigDigits
                + ", TargetSG: "
                + targetSigDig
                + ", TargetRounded: "
                + targetRoundedBG
                + ", Roundtrip";
    }

    private String getRoundtripRoundedSource(
            int sourceSigDigits,
            Rational conversion,
            BoundedNumber targetBounded,
            int targetSigDig) {
        Rational roundedTarget = targetBounded.getValue().roundToSignificant(targetSigDig);
        BoundedNumber roundedTargetBounded =
                BoundedNumber.from(roundedTarget, PrecisionType.significant, targetSigDig);
        BoundedNumber roundTripSourceBounded = roundedTargetBounded.clone().divideBy(conversion);
        return roundWithBigDecimal(roundTripSourceBounded.getValue(), sourceSigDigits);
    }

    private String roundWithBigDecimal(Rational sourceValue, int sourceSigDigits) {
        return sourceValue.roundToSignificant(sourceSigDigits).toString();
    }

    public static class CheckResult {

        public enum Closeness {
            exact,
            near,
            far
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
            return diff.compareTo(minHalfUlp) <= 0 ? Closeness.near : Closeness.far;
        }

        @Override
        public String toString() {
            return Joiners.ES.join(
                    "source:       \t",
                    FullUnitConverter.toPlainString(source),
                    "\nsourceForm.:\t",
                    getSourceFormatted(),
                    "\nnumbers:    \t",
                    numbers,
                    "\nroundTrip:  \t",
                    FullUnitConverter.toPlainString(roundTrip),
                    "\nrawCvttBack:\t",
                    FullUnitConverter.toPlainString(rawConvertBack),
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

    public void testFloor() {
        assertEquals("ceil 1.5", BigInteger.valueOf(2), Rational.of("1.5").ceil());
        assertEquals("floor 1.5", BigInteger.valueOf(1), Rational.of("1.5").floor());
        assertEquals("ceil 1", BigInteger.valueOf(1), Rational.of(1).ceil());
        assertEquals("floor 1", BigInteger.valueOf(1), Rational.of(1).floor());
        assertEquals("ceil 0", BigInteger.valueOf(0), Rational.of(0).ceil());
        assertEquals("floor 0", BigInteger.valueOf(0), Rational.of(0).floor());
        assertEquals("ceil -1", BigInteger.valueOf(-1), Rational.of(-1).ceil());
        assertEquals("floor -1", BigInteger.valueOf(-1), Rational.of(-1).floor());
        assertEquals("ceil -1.5", BigInteger.valueOf(-1), Rational.of("-1.5").ceil());
        assertEquals("floor -1.5", BigInteger.valueOf(-2), Rational.of("-1.5").floor());
    }

    public void testRoundingModes() {
        String[][] tests = {
            // value, increment, mode, expectedResult
            {"1.5", "1", "HALF_UP", "2"},
            {"1.5", "1", "HALF_EVEN", "2"},
            {"1.5", "1", "HALF_DOWN", "1"},
            {"2.5", "1", "HALF_UP", "3"},
            {"2.5", "1", "HALF_EVEN", "2"},
            {"2.5", "1", "HALF_DOWN", "2"},
            {"-1.5", "1", "HALF_UP", "-2"},
            {"-1.5", "1", "HALF_EVEN", "-2"},
            {"-1.5", "1", "HALF_DOWN", "-1"},
            {"-2.5", "1", "HALF_UP", "-3"},
            {"-2.5", "1", "HALF_EVEN", "-2"},
            {"-2.5", "1", "HALF_DOWN", "-2"},
        };
        for (String[] test : tests) {
            Rational value = Rational.of(test[0]);
            int significant = Integer.parseInt(test[1]);
            RoundingMode mode = RoundingMode.valueOf(test[2]);
            Rational expected = Rational.of(test[3]);
            Rational actual = value.roundToSignificant(significant, mode);
            assertEquals(Joiners.COMMA_SP.join(test), expected, actual);
        }
    }
}
