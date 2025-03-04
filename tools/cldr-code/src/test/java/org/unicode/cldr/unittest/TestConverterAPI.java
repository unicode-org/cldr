package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.unittest.MeasureUnit2.Measure2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.UnitConverter;

public class TestConverterAPI extends TestFmwk {

    static final Joiner JOIN_EMPTY = Joiner.on("");
    private static final Splitter SPLIT_SEMI = Splitter.on(';');
    static final UnitConverter unitConverter =
            CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

    public static void main(String[] args) {
        new TestConverterAPI().run(args);
    }

    public void testIds() {
        String[][] tests = {
            {"foot-per-minute", "foot-per-minute", "meter-per-second", "null"},
            {"foot-foot", "square-foot", "square-meter", "null"}, // normalized to use powers
            {
                "coulomb-per-second", "coulomb-per-second", "ampere", "null"
            }, // normalized to resolve powers in base
            {"foot-and-inch", "foot-and-inch", "meter", "[foot, inch]"},
            {"inch-and-foot", "foot-and-inch", "meter", "[foot, inch]"}, // normalized to fix order
            {
                "foot-and-foot-and-inch", "foot-and-inch", "meter", "[foot, inch]"
            }, // normalized to remove duplicates
            {"foot-and-meter", "meter-and-foot", "meter", "[meter, foot]"}, // weird but allowed

            // errors
            // {"xxx-foobar", "xxx-foobar", null, null}, // private use, not quite working yet
            {"foot-and-foot", "«foot-and-foot» has ≤1 unit", "", ""},
            {"foot-and-pound", "«foot-and-pound»: foot & pound are not convertible", "", ""},
        };
        for (String[] test : tests) {
            MeasureUnit2 unitId = null;
            String actual = null;
            try {
                unitId = MeasureUnit2.forIdentifier(test[0]);
            } catch (UncheckedExecutionException e) {
                actual = e.getCause().getMessage();
            } catch (Exception e) {
                actual = e.getMessage();
            }
            if (actual != null) {
                assertEquals(Arrays.asList(test) + "", test[1], actual);
                continue;
            }
            assertEquals(
                    Arrays.asList(test) + " normalized", test[1], unitId.getNormalizedIdentifier());
            assertEquals(
                    Arrays.asList(test) + " baseIdentifier", test[2], unitId.getBaseIdentifier());
            assertEquals(
                    Arrays.asList(test) + " subunits",
                    test[3],
                    String.valueOf(unitId.getSubunits()));
        }
    }

    enum Numeric {
        doubleFloat,
        bigDecimal,
        rational
    }

    public void testConversions() {
        String[][] tests = {
            {"3", "foot", "meter", "0.9144"},
            {"0.9144", "meter", "foot", "3"},
            {"3", "foot-per-second", "kilometer-per-hour", "3.29184"},
            {"3", "foot-per-second", "hour-per-kilometer", "0.3037814718"},
            {"3", "newton", "pound-mile-per-square-minute", "14.79480106"},
            {"1.27", "meter", "foot-and-inch", "4;2"},
            {"-1.27", "meter", "foot-and-inch", "-4;-2"},
            {"4;2", "foot-and-inch", "meter", "1.27"},
            {"-4;-2", "foot-and-inch", "meter", "-1.27"},
            {"20;9", "foot-and-inch", "yard-and-foot", "6;2.75"},
            {"6;2.75", "yard-and-foot", "foot-and-inch", "20;9"},

            // errors
            {
                "3",
                "foot-per-second",
                "kilogram",
                "foot-per-second and kilogram are not convertible"
            },
            {"3", "foot-and-inch", "kilogram", "foot-and-inch and kilogram are not convertible"},
        };
        int testNumber = 0;
        for (String[] test : tests) {
            ++testNumber;
            for (Numeric numeric : Numeric.values()) {
                String actual = null;
                String expected = test[3];
                String title = " " + numeric.toString();
                try {
                    MeasureUnit2 source = MeasureUnit2.forIdentifier(test[1]);
                    MeasureUnit2 target = MeasureUnit2.forIdentifier(test[2]);
                    if (!source.isMixed() && !target.isMixed()) { // unmixed units
                        switch (numeric) {
                            case doubleFloat:
                                {
                                    title = " double";
                                    // do unmixed units
                                    double amount = Double.parseDouble(test[0]);
                                    double result = MeasureUnit2.convert(amount, source, target);
                                    actual = nf10.format(result).toString();
                                    break;
                                }
                            case bigDecimal:
                                {
                                    BigDecimal amount = new BigDecimal(test[0]);
                                    BigDecimal result =
                                            MeasureUnit2.convert(amount, source, target);
                                    actual = nf10.format(result).toString();
                                    break;
                                }
                            case rational:
                                {
                                    Rational amount = Rational.of(test[0]);
                                    Rational result =
                                            unitConverter.convert(amount, test[1], test[2], false);
                                    actual =
                                            result.equals(Rational.NaN)
                                                    ? source.getNormalizedIdentifier()
                                                            + " and "
                                                            + target.getNormalizedIdentifier()
                                                            + " are not convertible"
                                                    : nf10.format(result.doubleValue()).toString();
                                    break;
                                }
                        }
                    } else { // do mixed units
                        switch (numeric) {
                            case doubleFloat:
                                {
                                    title = " double";
                                    List<Double> amounts = listDoubleFrom(test[0]);
                                    List<Double> result =
                                            MeasureUnit2.convertDoubles(amounts, source, target);
                                    actual = stringFromListNumber(result);
                                    break;
                                }
                            case bigDecimal:
                                {
                                    List<BigDecimal> amount = listBigDecimalFrom(test[0]);
                                    List<BigDecimal> result =
                                            MeasureUnit2.convertBigDecimals(amount, source, target);
                                    actual = stringFromListNumber(result);
                                    break;
                                }
                            case rational:
                                {
                                    List<Rational> amount = listRationalFrom(test[0]);
                                    List<Rational> result =
                                            MeasureUnit2.convert(amount, source, target);
                                    actual = stringFromListNumber(result);
                                }
                                break;
                        }
                    }
                } catch (UncheckedExecutionException e) {
                    actual = e.getCause().getMessage();
                } catch (Exception e) {
                    actual = e.getMessage();
                }
                assertEquals(
                        testNumber + ") " + Arrays.asList(test).toString() + title,
                        expected,
                        actual);
            }
        }
    }

    public void testSamples() {
        MeasureUnit icuMeasureUnit;
        Measure icuMeasure;

        double amount = 3.4d;

        MeasureUnit2 mu1 = MeasureUnit2.forIdentifier("foot-per-second");
        MeasureUnit2 mu2 = MeasureUnit2.forIdentifier("kilometer-per-hour");

        double result1static = MeasureUnit2.convert(amount, mu1, mu2);

        double result2nonstatic = mu2.convertFrom(amount, mu1);

        assertEquals("nonstatic is same as static", result1static, result2nonstatic);

        double result3d =
                MeasureUnit2.forIdentifier("kilometer-per-hour")
                        .convertFrom(amount, MeasureUnit2.forIdentifier("foot-per-second"));

        assertEquals("in-place is same as static", result1static, result3d);

        Measure2 measure = new Measure2(amount, mu1);

        double result2measure = mu2.convertFrom(measure);

        assertEquals("nonstatic is same as static", result1static, result2measure);

        MeasureUnit2 mu3 = MeasureUnit2.forIdentifier("foot-and-inch");
        MeasureUnit2 mu4 = MeasureUnit2.forIdentifier("meter-and-centimeter");

        List<Double> resultA = mu4.convertDoublesFrom(List.of(10d, 3d), mu3);
        assertEquals("mixed", List.of(3.0d, 12.42d), resultA);
    }

    public void testSpeed() {
        // warmup
        boolean result = true;
        double amount = 3.4d;
        BigDecimal bd3_4 = BigDecimal.valueOf(amount);
        Rational r3_4 = Rational.of(amount);

        // Warmup

        final int iterations =
                getInclusion() < 6
                        ? 1_000_000
                        : 100_000_000; // use larger number with -e6 or greater

        for (int i = 0; i < 1000000; ++i) {
            MeasureUnit2 mu1 = MeasureUnit2.forIdentifier("foot-per-second");
            MeasureUnit2 mu2 = MeasureUnit2.forIdentifier("kilometer-per-hour");
            result ^= 0d == MeasureUnit2.convert(amount, mu1, mu2);
            result ^= BigDecimal.ZERO.equals(MeasureUnit2.convert(bd3_4, mu1, mu2));
            result ^= Rational.ZERO.equals(MeasureUnit2.convert(r3_4, mu1, mu2));
        }

        warnln("iterations: ~" + nf10.format(iterations));
        Timer t = new Timer();

        // Doubles w/ existing

        t.start();
        MeasureUnit2 mu1 = MeasureUnit2.forIdentifier("foot-per-second");
        MeasureUnit2 mu2 = MeasureUnit2.forIdentifier("kilometer-per-hour");

        for (int i = 0; i < iterations; ++i) {
            result ^= 0d == MeasureUnit2.convert(amount, mu1, mu2);
        }
        t.stop();
        showTime("Doubles w/existing MeasureUnit", iterations, t);

        // Doubles w/ cache

        final Map<String, MeasureUnit2> unboundedCache = new ConcurrentHashMap<>();

        t.start();
        try {
            for (int i = 0; i < iterations; ++i) {
                MeasureUnit2 mu1d =
                        unboundedCache.computeIfAbsent(
                                "foot-per-second", x -> MeasureUnit2.forIdentifier(x));
                MeasureUnit2 mu2d =
                        unboundedCache.computeIfAbsent(
                                "kilometer-per-hour", x -> MeasureUnit2.forIdentifier(x));

                result ^= 0d == MeasureUnit2.convert(amount, mu1d, mu2d);
            }
        } catch (Exception e) {
        }

        t.stop();
        showTime("Doubles w/unbounded caller cache — ConcurrentHashMap", iterations, t);

        // Doubles w/ cache

        final LoadingCache<String, MeasureUnit2> cache =
                CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .build(
                                new CacheLoader<String, MeasureUnit2>() {
                                    @Override
                                    public MeasureUnit2 load(String unitId) {
                                        return MeasureUnit2.forIdentifier(unitId);
                                    }
                                });

        t.start();
        try {
            for (int i = 0; i < iterations; ++i) {
                MeasureUnit2 mu1d = cache.get("foot-per-second");
                MeasureUnit2 mu2d = cache.get("kilometer-per-hour");

                result ^= 0d == MeasureUnit2.convert(amount, mu1d, mu2d);
            }
        } catch (Exception e) {
        }

        t.stop();
        showTime("Doubles w/bounded caller cache — LoadingCache", iterations, t);

        // Doubles

        t.start();
        for (int i = 0; i < iterations; ++i) {
            MeasureUnit2 mu1d = MeasureUnit2.forIdentifier("foot-per-second");
            MeasureUnit2 mu2d = MeasureUnit2.forIdentifier("kilometer-per-hour");
            result ^= 0d == MeasureUnit2.convert(amount, mu1d, mu2d);
        }
        t.stop();
        showTime("Doubles", iterations, t);

        // Big Decimals

        int iterations5 = iterations / 5;
        t.start();
        for (int i = 0; i < iterations5; ++i) {
            MeasureUnit2 mu1d = MeasureUnit2.forIdentifier("foot-per-second");
            MeasureUnit2 mu2d = MeasureUnit2.forIdentifier("kilometer-per-hour");
            result ^= BigDecimal.ZERO.equals(MeasureUnit2.convert(bd3_4, mu1d, mu2d));
        }
        t.stop();
        showTime("BigDecimals (64)", iterations5, t);

        // Rationals

        int iterations25 = iterations / 25;
        t.start();
        for (int i = 0; i < iterations25; ++i) {
            MeasureUnit2 mu1d = MeasureUnit2.forIdentifier("foot-per-second");
            MeasureUnit2 mu2d = MeasureUnit2.forIdentifier("kilometer-per-hour");
            result ^= Rational.ZERO.equals(MeasureUnit2.convert(r3_4, mu1d, mu2d));
        }
        t.stop();
        showTime("Rationals", iterations25, t);
    }

    // UTILITIES

    private void showTime(String title, final int iterations, Timer t) {
        warnln(nf2.format(t.getNanoseconds() / (double) iterations) + " ns: \t" + title);
    }

    static final LocalizedNumberFormatter nf10 =
            NumberFormatter.with()
                    .precision(Precision.maxSignificantDigits(10))
                    .locale(Locale.ENGLISH);

    static final LocalizedNumberFormatter nf2 =
            NumberFormatter.with()
                    .precision(Precision.maxSignificantDigits(3))
                    .locale(Locale.ENGLISH);

    private <N extends Number> String stringFromListNumber(List<N> result) {
        return result.stream()
                .map(x -> nf10.format(x.doubleValue()))
                .collect(Collectors.joining(";"));
    }

    // Utilities

    private List<BigDecimal> listBigDecimalFrom(String string) {
        return Streams.stream(SPLIT_SEMI.split(string))
                .map(x -> new BigDecimal(x))
                .collect(Collectors.toList());
    }

    private List<Double> listDoubleFrom(String string) {
        return Streams.stream(SPLIT_SEMI.split(string))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    private List<Rational> listRationalFrom(String string) {
        return Streams.stream(SPLIT_SEMI.split(string))
                .map(Rational::of)
                .collect(Collectors.toList());
    }
}
