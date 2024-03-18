package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.BigIntegerMath;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Basic class for rational numbers. There is little attempt to optimize, since it will just be used
 * for testing and data production within CLDR.
 *
 * @author markdavis
 */
public final class Rational extends Number implements Comparable<Rational> {
    private static final long serialVersionUID = 1L;
    private static final Pattern INT_POWER_10 = Pattern.compile("10*");
    public final BigInteger numerator;
    public final BigInteger denominator;

    static final BigInteger BI_TWO = BigInteger.valueOf(2);
    static final BigInteger BI_FIVE = BigInteger.valueOf(5);
    static final BigInteger BI_MINUS_ONE = BigInteger.valueOf(-1);
    static final BigInteger BI_TEN = BigInteger.valueOf(10);

    static final BigDecimal BD_TWO = BigDecimal.valueOf(2);
    static final BigDecimal BD_FIVE = BigDecimal.valueOf(5);

    // Constraints:
    //   always stored in normalized form.
    //   no common factor > 1 (reduced)
    //   denominator never negative
    //   if numerator is zero, denominator is 1 or 0
    //   if denominator is zero, numerator is 1, -1, or 0

    // NOTE, the constructor doesn't do any checking, so everything other than these goes
    // through Rational.of(...)
    public static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);
    public static final Rational ONE = new Rational(BigInteger.ONE, BigInteger.ONE);
    public static final Rational NaN = new Rational(BigInteger.ZERO, BigInteger.ZERO);
    public static final Rational INFINITY = new Rational(BigInteger.ONE, BigInteger.ZERO);

    public static final Rational NEGATIVE_ONE = ONE.negate();
    public static final Rational NEGATIVE_INFINITY = INFINITY.negate();

    public static final Rational TWO = new Rational(BI_TWO, BigInteger.ONE);
    public static final Rational TEN = new Rational(BI_TEN, BigInteger.ONE);
    public static final Rational TENTH = TEN.reciprocal();

    public static final char REPTEND_MARKER = '˙';
    public static final String APPROX = "~";

    public static class RationalParser implements Freezable<RationalParser> {

        public static final RationalParser BASIC = new RationalParser().freeze();

        private static final Splitter slashSplitter = Splitter.on('/').trimResults();
        private static final Splitter starSplitter = Splitter.on('*').trimResults();

        private Map<String, Rational> constants;
        private Map<String, String> constantStatus;

        public RationalParser() {
            constants = new LinkedHashMap<>();
            constantStatus = new LinkedHashMap<>();
        }

        public RationalParser(
                Map<String, Rational> constants2, Map<String, String> constantStatus2) {
            frozen = false;
            constants = new LinkedHashMap<>(constants2);
            constantStatus = new LinkedHashMap<>(constantStatus2);
        }

        public RationalParser addConstant(String id, String value, String status) {
            final Rational parsed = parse(value);
            if (constants.put(id, parsed) != null) {
                throw new IllegalArgumentException("Can't reset constant " + id + " = " + value);
            }
            if (status != null) {
                constantStatus.put(id, status);
            }
            return this;
        }

        /*
         * input = comp (/ comp)?
         * comp = comp2 (* comp2)*
         * comp2 = digits (. digits)? | constant
         * */

        public Rational parse(String input) {
            switch (input) {
                case "NaN":
                    return NaN;
                case "INF":
                    return INFINITY;
                case "-INF":
                    return NEGATIVE_INFINITY;
            }
            if (input.startsWith(APPROX)) {
                input = input.substring(1);
            }
            input = input.replace(",", ""); // allow commas anywhere
            List<String> comps = slashSplitter.splitToList(input); // get num/den
            try {
                switch (comps.size()) {
                    case 1:
                        return process(comps.get(0));
                    case 2:
                        return process(comps.get(0)).divide(process(comps.get(1)));
                    default:
                        throw new IllegalArgumentException("too many slashes in " + input);
                }
            } catch (Exception e) {
                throw new ICUException("bad input: " + input, e);
            }
        }

        private Rational process(String string) {
            Rational result = null;
            string = string.replace(HUMAN_EXPONENT, "E");
            for (String comp : starSplitter.split(string)) {
                Rational ratComp = process2(comp);
                result = result == null ? ratComp : result.multiply(ratComp);
            }
            return result;
        }

        static final UnicodeSet ALLOWED_CHARS =
                new UnicodeSet("[-A-Za-z0-9_]").add(REPTEND_MARKER).freeze();

        private Rational process2(String input) {
            final char firstChar = input.charAt(0);
            if (firstChar == '-' || (firstChar >= '0' && firstChar <= '9')) {
                int pos = input.indexOf(REPTEND_MARKER);
                if (pos < 0) {
                    return Rational.of(new BigDecimal(input));
                }
                // handle repeating fractions
                String reptend = input.substring(pos + 1);
                int rlen = reptend.length();
                input = input.substring(0, pos) + reptend;

                BigDecimal rf = new BigDecimal(input);
                BigDecimal rfPow = new BigDecimal(input + reptend).scaleByPowerOfTen(rlen);
                BigDecimal num = rfPow.subtract(rf);

                Rational result = Rational.of(num);
                Rational den =
                        Rational.of(
                                BigDecimal.ONE
                                        .scaleByPowerOfTen(rlen)
                                        .subtract(BigDecimal.ONE)); // could optimize
                return result.divide(den);
            } else {
                if (!ALLOWED_CHARS.containsAll(input)) {
                    throw new IllegalArgumentException("Bad characters in: " + input);
                }
                Rational result = constants.get(input);
                if (result == null) {
                    throw new IllegalArgumentException("Constant not defined: " + input);
                }
                return result;
            }
        }

        boolean frozen = false;

        @Override
        public boolean isFrozen() {
            return frozen;
        }

        @Override
        public RationalParser freeze() {
            if (!frozen) {
                frozen = true;
                constants = ImmutableMap.copyOf(constants);
                constantStatus = ImmutableMap.copyOf(constantStatus);
            }
            return this;
        }

        @Override
        public RationalParser cloneAsThawed() {
            return new RationalParser(constants, constantStatus);
        }

        public Map<String, Rational> getConstants() {
            return constants;
        }
    }

    public static Rational of(long numerator, long denominator) {
        return Rational.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Rational of(long numerator) {
        return Rational.of(BigInteger.valueOf(numerator), BigInteger.ONE);
    }

    public static Rational of(BigInteger numerator, BigInteger denominator) {
        int dComparison = denominator.compareTo(BigInteger.ZERO);
        if (dComparison == 0) {
            // catch equivalents to NaN, -INF, +INF
            // 0/0 => NaN
            // +/0 => INF
            // -/0 => -INF
            int nComparison = numerator.compareTo(BigInteger.ZERO);
            return nComparison < 0 ? NEGATIVE_INFINITY : nComparison > 0 ? INFINITY : NaN;
        } else {
            // reduce to lowest form
            BigInteger gcd = numerator.gcd(denominator);
            if (gcd.compareTo(BigInteger.ONE) > 0) {
                numerator = numerator.divide(gcd);
                denominator = denominator.divide(gcd);
            }
            if (dComparison < 0) {
                // ** NOTE: is already reduced, so safe to use constructor
                return new Rational(numerator, denominator);
            } else {
                // ** NOTE: is already reduced, so safe to use constructor
                return new Rational(numerator.negate(), denominator.negate());
            }
        }
    }

    public static Rational of(BigInteger numerator) {
        // ** NOTE: is already reduced, so safe to use constructor
        return new Rational(numerator, BigInteger.ONE);
    }

    public static Rational of(String simple) {
        return RationalParser.BASIC.parse(simple);
    }

    private Rational(BigInteger numerator, BigInteger denominator) {
        if (denominator.compareTo(BigInteger.ZERO) < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        if (gcd.compareTo(BigInteger.ONE) > 0) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Rational add(Rational other) {
        BigInteger newNumerator =
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator));
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return Rational.of(newNumerator, newDenominator);
    }

    public Rational subtract(Rational other) {
        BigInteger newNumerator =
                numerator
                        .multiply(other.denominator)
                        .subtract(other.numerator.multiply(denominator));
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return Rational.of(newNumerator, newDenominator);
    }

    public Rational multiply(Rational other) {
        BigInteger newNumerator = numerator.multiply(other.numerator);
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return Rational.of(newNumerator, newDenominator);
    }

    public Rational divide(Rational other) {
        BigInteger newNumerator = numerator.multiply(other.denominator);
        BigInteger newDenominator = denominator.multiply(other.numerator);
        return Rational.of(newNumerator, newDenominator);
    }

    public Rational pow(int i) {
        return Rational.of(numerator.pow(i), denominator.pow(i));
    }

    public static Rational pow10(int i) {
        return i > 0 ? TEN.pow(i) : TENTH.pow(-i);
    }

    public Rational reciprocal() {
        return Rational.of(denominator, numerator);
    }

    public Rational negate() {
        // ** NOTE: is already reduced, so safe to use constructor
        return new Rational(numerator.negate(), denominator);
    }

    public BigDecimal toBigDecimal(MathContext mathContext) {
        try {
            return new BigDecimal(numerator).divide(new BigDecimal(denominator), mathContext);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Wrong math context for divide: " + this + ", " + mathContext);
        }
    }

    public BigDecimal toBigDecimal() {
        return toBigDecimal(MathContext.DECIMAL128); // prevent failures due to repeating fractions
    }

    @Override
    public double doubleValue() {
        if (denominator.equals(BigInteger.ZERO) && numerator.equals(BigInteger.ZERO)) {
            return Double.NaN;
        }
        return toBigDecimal(MathContext.DECIMAL64).doubleValue();
    }

    @Override
    public float floatValue() {
        if (denominator.equals(BigInteger.ZERO) && numerator.equals(BigInteger.ZERO)) {
            return Float.NaN;
        }
        return toBigDecimal(MathContext.DECIMAL32).floatValue();
    }

    public static Rational of(BigDecimal bigDecimal) {
        // scale()
        // If zero or positive, the scale is the number of digits to the right of the decimal point.
        // If negative, the unscaled value of the number is multiplied by ten to the power of the
        // negation of the scale.
        // For example, a scale of -3 means the unscaled value is multiplied by 1000.
        final int scale = bigDecimal.scale();
        final BigInteger unscaled = bigDecimal.unscaledValue();
        if (scale == 0) {
            // ** NOTE: is already reduced, so safe to use constructor
            return new Rational(unscaled, BigInteger.ONE);
        } else if (scale >= 0) {
            return Rational.of(unscaled, BigDecimal.ONE.movePointRight(scale).toBigInteger());
        } else {
            // ** NOTE: is already reduced, so safe to use constructor
            return new Rational(
                    unscaled.multiply(BigDecimal.ONE.movePointLeft(scale).toBigInteger()),
                    BigInteger.ONE);
        }
    }

    public static Rational of(double doubleValue) {
        return of(new BigDecimal(doubleValue));
    }

    public enum FormatStyle {
        /**
         * Simple numerator / denominator, plain BigInteger.toString(), dropping " / 1". <br>
         * The spaces are thin space (2009).
         */
        plain,
        /**
         * Approximate value if small number of digits, dropping " / 1" <br>
         * The spaces are thin space (2009).
         */
        approx,
        /**
         * Repeating decimal where possible (otherwise = simple). The limit is 30 repeating digits.
         */
        repeating,
        /**
         * Repeating decimal where possible (otherwise = simple). The limit is 1000 repeating
         * digits.
         */
        repeatingAll,
        /**
         * Formatted numerator / denominator, dropping " / 1" <br>
         * The spaces are thin space (2009).
         */
        formatted,
        /** HTML Formatted numerator / denominator, using sup/sub, dropping "/1" */
        html
    }

    @Override
    public String toString() {
        // could also return as "exact" decimal, if only factors of the denominator are 2 and 5
        return toString(FormatStyle.plain);
    }

    private static final BigInteger NUM_OR_DEN_LIMIT = BigInteger.valueOf(10000000);
    private static final BigInteger NUM_TIMES_DEN_LIMIT = BigInteger.valueOf(10000000);
    private static final BigInteger BIG_HIGH = BigInteger.valueOf(10000000);
    private static final double DOUBLE_HIGH = 10000000d;
    private static final double DOUBLE_LOW = 0.001d;

    static final String THIN_SPACE = "\u2009";
    static final String DIVIDER = "/";

    public String toString(FormatStyle style) {
        boolean denIsOne = denominator.equals(BigInteger.ONE);
        BigInteger absNumerator = numerator.abs();
        String result;
        switch (style) {
            case plain:
                result = numerator + (denIsOne ? "" : DIVIDER + denominator);
                break;
            case approx:
                if (denIsOne) {
                    if (absNumerator.compareTo(BIG_HIGH) < 0) {
                        result = formatGroup.format(numerator).toString();
                    } else {
                        result = replaceE(formatSciSigDig5.format(numerator).toString());
                    }
                } else {
                    int log10num = BigIntegerMath.log10(absNumerator, RoundingMode.UP);
                    int log10den = BigIntegerMath.log10(denominator, RoundingMode.UP);
                    if ((log10num <= 1 && log10den <= 4) || (log10num <= 4 && log10den <= 1)) {
                        result =
                                formatGroup.format(numerator)
                                        + DIVIDER
                                        + formatNoGroup.format(denominator);
                    } else {
                        final double doubleValue =
                                numerator.doubleValue() / denominator.doubleValue();
                        double absDoubleValue = Math.abs(doubleValue);
                        if (DOUBLE_LOW < absDoubleValue && absDoubleValue < DOUBLE_HIGH) {
                            result = formatSigDig5.format(doubleValue).toString();
                        } else {
                            result = formatSciSigDig5.format(doubleValue).toString();
                        }
                    }
                }
                break;
            default:
                Output<BigDecimal> newNumerator = new Output<>(new BigDecimal(numerator));
                final BigInteger newDenominator = minimalDenominator(newNumerator, denominator);
                final String numStr = formatGroup.format(newNumerator.value).toString();
                final String denStr = formatNoGroup.format(newDenominator).toString();
                denIsOne = newDenominator.equals(BigInteger.ONE);
                int limit = 1000;

                switch (style) {
                    case repeating:
                        limit = 30;
                        // fall through with smaller limit
                    case repeatingAll:
                        // if we come directly here, the limit is huge
                        result = toRepeating(limit);
                        if (result != null) { // null is returned if we can't fit into the limit
                            break;
                        }
                        // otherwise drop through to simple
                    case formatted:
                        // skip approximate test
                        result = denIsOne ? numStr : numStr + DIVIDER + denStr;
                        break;
                    case html:
                        // skip approximate test
                        return denIsOne
                                ? numStr
                                : "<sup>" + numStr + "</sup>" + "/<sub>" + denStr + "<sub>";
                    default:
                        throw new UnsupportedOperationException();
                }
        }
        Rational roundtrip = Rational.of(result);
        return replaceE(roundtrip.equals(this) ? result : APPROX + result);
    }

    static final String HUMAN_EXPONENT = "×10ˆ";

    private String replaceE(String format2) {
        return format2.replace("E", HUMAN_EXPONENT);
    }

    private static final LocalizedNumberFormatter formatGroup =
            NumberFormatter.with().precision(Precision.unlimited()).locale(Locale.ENGLISH);

    private static final LocalizedNumberFormatter formatNoGroup =
            NumberFormatter.with()
                    .precision(Precision.unlimited())
                    .grouping(GroupingStrategy.OFF)
                    .locale(Locale.ENGLISH);

    private static final LocalizedNumberFormatter formatSigDig5 =
            NumberFormatter.with()
                    .precision(Precision.maxSignificantDigits(5))
                    .locale(Locale.ENGLISH);

    private static final LocalizedNumberFormatter formatSciSigDig5 =
            NumberFormatter.with()
                    .precision(Precision.maxSignificantDigits(5))
                    .notation(Notation.engineering())
                    .locale(Locale.ENGLISH);

    @Override
    public int compareTo(Rational other) {
        return numerator
                .multiply(other.denominator)
                .compareTo(other.numerator.multiply(denominator));
    }

    @Override
    public boolean equals(Object that) {
        return equals((Rational) that); // TODO fix later
    }

    public boolean equals(Rational that) {
        return numerator.equals(that.numerator) && denominator.equals(that.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    public Rational abs() {
        return numerator.signum() >= 0 ? this : this.negate();
    }

    /**
     * Goal is to be able to display rationals in a short but exact form, like 1,234,567/3 or
     * 1.234567E21/3. To do this, find the smallest denominator (excluding powers of 2 and 5), and
     * modify the numerator in the same way.
     *
     * @param denominator TODO
     * @param current
     * @return
     */
    public static BigInteger minimalDenominator(
            Output<BigDecimal> outNumerator, BigInteger denominator) {
        if (denominator.equals(BigInteger.ONE) || denominator.equals(BigInteger.ZERO)) {
            return denominator;
        }
        BigInteger newDenominator = denominator;
        while (newDenominator.mod(BI_TWO).equals(BigInteger.ZERO)) {
            newDenominator = newDenominator.divide(BI_TWO);
            outNumerator.value = outNumerator.value.divide(BD_TWO);
        }
        BigInteger outDenominator = newDenominator;
        while (newDenominator.mod(BI_FIVE).equals(BigInteger.ZERO)) {
            newDenominator = newDenominator.divide(BI_FIVE);
            outNumerator.value = outNumerator.value.divide(BD_FIVE);
        }
        return newDenominator;
    }

    public static class MutableLong {
        public long value;

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class ContinuedFraction {
        public final List<BigInteger> sequence;

        public ContinuedFraction(Rational source) {
            List<BigInteger> _sequence = new ArrayList<>();
            while (true) {
                BigInteger floor = source.floor();
                if (floor.compareTo(BigInteger.ZERO) < 0) {
                    floor = floor.subtract(BigInteger.ONE);
                }
                Rational remainder = source.subtract(Rational.of(floor, BigInteger.ONE));
                _sequence.add(floor);
                if (remainder.equals(Rational.ZERO)) {
                    break;
                }
                source = remainder.reciprocal();
            }
            sequence = ImmutableList.copyOf(_sequence);
        }

        public ContinuedFraction(long... items) {
            List<BigInteger> _sequence = new ArrayList<>();
            int count = 0;
            for (long item : items) {
                if (count != 0 && item < 0) {
                    throw new IllegalArgumentException("Only first item can be negative");
                }
                _sequence.add(BigInteger.valueOf(item));
                count++;
            }
            sequence = ImmutableList.copyOf(_sequence);
        }

        public Rational toRational(List<Rational> intermediates) {
            if (intermediates != null) {
                intermediates.clear();
            }
            BigInteger h0 = BigInteger.ZERO;
            BigInteger h1 = BigInteger.ONE;
            BigInteger k0 = BigInteger.ONE;
            BigInteger k1 = BigInteger.ZERO;
            for (BigInteger item : sequence) {
                BigInteger h2 = item.multiply(h1).add(h0);
                BigInteger k2 = item.multiply(k1).add(k0);
                if (intermediates != null) {
                    intermediates.add(Rational.of(h2, k2));
                }
                h0 = h1;
                h1 = h2;
                k0 = k1;
                k1 = k2;
            }
            if (intermediates != null) {
                Rational last = intermediates.get(intermediates.size() - 1);
                intermediates.remove(intermediates.size() - 1);
                return last;
            } else {
                return Rational.of(h1, k1);
            }
        }

        @Override
        public String toString() {
            return sequence.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return sequence.equals(((ContinuedFraction) obj).sequence);
        }

        @Override
        public int hashCode() {
            return sequence.hashCode();
        }
    }

    public BigInteger floor() {
        return numerator.divide(denominator);
    }

    /** The symmetric difference of a and b is 2 * abs(a - b) / (a + b) */
    public Rational symmetricDiff(Rational b) {
        return equals(b) ? ZERO : subtract(b).abs().multiply(TWO).divide(add(b));
    }

    /** Return repeating fraction, as long as the length is reasonable */
    private String toRepeating(int stringLimit) {
        BigInteger p = numerator;
        BigInteger q = denominator;
        StringBuilder s = new StringBuilder();

        // Edge cases
        final int pTo0 = p.compareTo(BigInteger.ZERO);
        if (q.compareTo(BigInteger.ZERO) == 0) {
            return pTo0 == 0 ? "NaN" : pTo0 > 0 ? "INF" : "-INF";
        }
        if (pTo0 == 0) {
            return "0";
        } else if (pTo0 < 0) {
            p = p.negate();
            s.append('-');
        }
        final int pToq = p.compareTo(q);
        if (pToq == 0) {
            s.append('1');
            return s.toString();
        } else if (pToq > 0) {
            BigInteger intPart = p.divide(q);
            s.append(formatNoGroup.format(intPart));
            p = p.remainder(q);
            if (p.compareTo(BigInteger.ZERO) == 0) {
                return s.toString();
            }
        } else {
            s.append('0');
        }

        // main loop
        s.append(".");
        int pos = -1; // all places are right to the radix point
        Map<BigInteger, Integer> occurs = new HashMap<>();
        while (!occurs.containsKey(p)) {
            occurs.put(p, pos); // the position of the place with remainder p
            BigInteger p10 = p.multiply(BigInteger.TEN);
            BigInteger z = p10.divide(q); // index z of digit within: 0 ≤ z ≤ b-1
            p = p10.subtract(z.multiply(q)); // 0 ≤ p < q
            s = s.append(((char) ('0' + z.intValue()))); // append the character of the digit
            if (p.equals(BigInteger.ZERO)) {
                return s.toString();
            } else if (s.length() > stringLimit) {
                return null;
            }
            pos -= 1;
        }
        int L = occurs.get(p) - pos; // the length of the reptend (being < q)
        s.insert(s.length() - L, REPTEND_MARKER);
        return s.toString();
    }

    public boolean isPowerOfTen() {
        Output<BigDecimal> newNumerator = new Output<>(new BigDecimal(numerator));
        final BigInteger newDenominator = minimalDenominator(newNumerator, denominator);
        if (!newDenominator.equals(BigInteger.ONE)) {
            return false;
        }
        // hack, figure out later
        String str = newNumerator.value.unscaledValue().toString();
        if (INT_POWER_10.matcher(str).matches()) {
            return true;
        }
        return false;
    }

    public String getIntPowerOfTen() {
        // HACK, figure out better later
        if (numerator.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Prefix label must be positive: " + this);
        }
        if (!numerator.equals(BigInteger.ONE) && !denominator.equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("Prefix label must be power of 10: " + this);
        }
        BigInteger value;
        int sign;
        if (numerator.equals(BigInteger.ONE)) {
            value = denominator;
            sign = -1;
        } else {
            value = numerator;
            sign = 1;
        }
        String str = value.toString();
        if (!INT_POWER_10.matcher(str).matches()) {
            throw new IllegalArgumentException("Prefix label must be power of 10: " + this);
        }
        int result = str.length() - 1;
        return String.valueOf(result * sign);
    }

    public static final Rational EPSILON = Rational.of(1, 1000000);

    /** Approximately equal when symmetric difference of a and b < epsilon (default EPSILON) */
    public boolean approximatelyEquals(Rational b, Rational epsilon) {
        return symmetricDiff(b).compareTo(epsilon) < 0;
    }

    public boolean approximatelyEquals(Rational b) {
        return approximatelyEquals(b, EPSILON);
    }

    public boolean approximatelyEquals(Number b) {
        return approximatelyEquals(Rational.of(b.doubleValue()), EPSILON);
    }

    @Override
    public int intValue() {
        return toBigDecimal().intValue();
    }

    @Override
    public long longValue() {
        return toBigDecimal().longValue();
    }
}
