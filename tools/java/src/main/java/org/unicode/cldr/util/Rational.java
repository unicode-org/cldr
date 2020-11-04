package org.unicode.cldr.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;

/**
 * Very basic class for rational numbers. No attempt to optimize, since it will just
 * be used for testing within CLDR.
 *
 * @author markdavis
 *
 */
public final class Rational implements Comparable<Rational> {
    private static final Pattern INT_POWER_10 = Pattern.compile("10*");
    public final BigInteger numerator;
    public final BigInteger denominator;

    // Constraints:
    //   always stored in normalized form.
    //   no common factor > 1 (reduced)
    //   denominator never negative
    //   if numerator is zero, denominator is 1 or 0
    //   if denominator is zero, numerator is 1, -1, or 0

    public static final Rational ZERO = Rational.of(0);
    public static final Rational ONE = Rational.of(1);
    public static final Rational NEGATIVE_ONE = ONE.negate();

    public static final Rational INFINITY = Rational.of(1,0);
    public static final Rational NEGATIVE_INFINITY = INFINITY.negate();
    public static final Rational NaN = Rational.of(0,0);

    public static final Rational TEN = Rational.of(10, 1);
    public static final Rational TENTH = TEN.reciprocal();

    public static final char REPTEND_MARKER = '˙';

    public static class RationalParser implements Freezable<RationalParser>{

        public static RationalParser BASIC = new RationalParser().freeze();

        private static Splitter slashSplitter = Splitter.on('/').trimResults();
        private static Splitter starSplitter = Splitter.on('*').trimResults();

        private Map<String,Rational> constants = new LinkedHashMap<>();
        private Map<String,String> constantStatus = new LinkedHashMap<>();

        public RationalParser addConstant(String id, String value, String status) {
            if (constants.put(id, parse(value)) != null) {
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
            case "NaN" : return NaN;
            case "INF" : return INFINITY;
            case "-INF": return NEGATIVE_INFINITY;
            }
            List<String> comps = slashSplitter.splitToList(input.replace(",", "")); // allow commas anywhere
            try {
                switch (comps.size()) {
                case 1: return process(comps.get(0));
                case 2: return process(comps.get(0)).divide(process(comps.get(1)));
                default: throw new IllegalArgumentException("too many slashes in " + input);
                }
            } catch (Exception e) {
                throw new ICUException("bad input: " + input, e);
            }
        }

        private  Rational process(String string) {
            Rational result = null;
            for (String comp : starSplitter.split(string)) {
                Rational ratComp = process2(comp);
                result = result == null ? ratComp : result.multiply(ratComp);
            }
            return result;
        }

        static final UnicodeSet ALLOWED_CHARS = new UnicodeSet("[-A-Za-z0-9_]").add(REPTEND_MARKER).freeze();

        private Rational process2(String input) {
            final char firstChar = input.charAt(0);
            if (firstChar == '-' || (firstChar >= '0' && firstChar <= '9')) {
                int pos = input.indexOf(REPTEND_MARKER);
                if (pos < 0) {
                    return Rational.of(new BigDecimal(input));
                }
                // handle repeating fractions
                String reptend = input.substring(pos+1);
                int rlen = reptend.length();
                input = input.substring(0,pos) + reptend;

                BigDecimal rf = new BigDecimal(input);
                BigDecimal rfPow = new BigDecimal(input+reptend).scaleByPowerOfTen(rlen);
                BigDecimal num = rfPow.subtract(rf);

                Rational result = Rational.of(num);
                Rational den = Rational.of(BigDecimal.ONE.scaleByPowerOfTen(rlen).subtract(BigDecimal.ONE)); // could optimize
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
            throw new UnsupportedOperationException();
        }

        public Map<String, Rational> getConstants() {
            return constants;
        }
    }

    public static Rational of(long numerator, long denominator) {
        return new Rational(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Rational of(long numerator) {
        return new Rational(BigInteger.valueOf(numerator), BigInteger.ONE);
    }

    public static Rational of(BigInteger numerator, BigInteger denominator) {
        return new Rational(numerator, denominator);
    }

    public static Rational of(BigInteger numerator) {
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
        BigInteger gcd_den = denominator.gcd(other.denominator);
        return new Rational(
            numerator.multiply(other.denominator).divide(gcd_den)
            .add(other.numerator.multiply(denominator).divide(gcd_den)),
            denominator.multiply(other.denominator).divide(gcd_den)
            );
    }

    public Rational subtract(Rational other) {
        BigInteger gcd_den = denominator.gcd(other.denominator);
        return new Rational(
            numerator.multiply(other.denominator).divide(gcd_den)
            .subtract(other.numerator.multiply(denominator).divide(gcd_den)),
            denominator.multiply(other.denominator).divide(gcd_den)
            );
    }

    public Rational multiply(Rational other) {
        BigInteger gcd_num_oden = numerator.gcd(other.denominator);
        boolean isZero = gcd_num_oden.equals(BigInteger.ZERO);
        BigInteger smallNum = isZero ? numerator : numerator.divide(gcd_num_oden);
        BigInteger smallODen = isZero ? other.denominator : other.denominator.divide(gcd_num_oden);

        BigInteger gcd_den_onum = denominator.gcd(other.numerator);
        isZero = gcd_den_onum.equals(BigInteger.ZERO);
        BigInteger smallONum = isZero ? other.numerator : other.numerator.divide(gcd_den_onum);
        BigInteger smallDen = isZero ? denominator : denominator.divide(gcd_den_onum);

        return new Rational(smallNum.multiply(smallONum), smallDen.multiply(smallODen));
    }

    public Rational pow(int i) {
        return new Rational(numerator.pow(i), denominator.pow(i));
    }

    public static Rational pow10(int i) {
        return i > 0 ? TEN.pow(i) : TENTH.pow(-i);
    }

    public Rational divide(Rational other) {
        return multiply(other.reciprocal());
    }

    public Rational reciprocal() {
        return new Rational(denominator, numerator);
    }

    public Rational negate() {
        return new Rational(numerator.negate(), denominator);
    }

    public BigDecimal toBigDecimal(MathContext mathContext) {
        try {
            return new BigDecimal(numerator).divide(new BigDecimal(denominator), mathContext);
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong math context for divide: " + this + ", " + mathContext);
        }
    }

    public double doubleValue() {
        if (denominator.equals(BigInteger.ZERO) && numerator.equals(BigInteger.ZERO)) {
            return Double.NaN;
        }
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), MathContext.DECIMAL64).doubleValue();
    }


    public BigDecimal toBigDecimal() {
        return toBigDecimal(MathContext.UNLIMITED);
    }

    public static Rational of(BigDecimal bigDecimal) {
        // scale()
        // If zero or positive, the scale is the number of digits to the right of the decimal point.
        // If negative, the unscaled value of the number is multiplied by ten to the power of the negation of the scale.
        // For example, a scale of -3 means the unscaled value is multiplied by 1000.
        final int scale = bigDecimal.scale();
        final BigInteger unscaled = bigDecimal.unscaledValue();
        if (scale == 0) {
            return new Rational(unscaled, BigInteger.ONE);
        } else if (scale >= 0) {
            return new Rational(unscaled, BigDecimal.ONE.movePointRight(scale).toBigInteger());
        } else {
            return new Rational(unscaled.multiply(BigDecimal.ONE.movePointLeft(scale).toBigInteger()), BigInteger.ONE);
        }
    }

    public enum FormatStyle {plain, simple, repeating, html}

    @Override
    public String toString() {
        // could also return as "exact" decimal, if only factors of the denominator are 2 and 5
        return toString(FormatStyle.plain);
    }


    public String toString(FormatStyle style) {
        switch (style) {
        case plain:
            return numerator + (denominator.equals(BigInteger.ONE) ? "" : " / " + denominator);
        }
        Output<BigDecimal> newNumerator = new Output<>(new BigDecimal(numerator));
        final BigInteger newDenominator = minimalDenominator(newNumerator, denominator);
        final String numStr = format(newNumerator.value);
        final String denStr = nf.format(newDenominator).toString();
        final boolean denIsOne = newDenominator.equals(BigInteger.ONE);

        switch (style) {
        case repeating:
            String result = toRepeating(30); // limit of 30 on the repeating length, so we don't get unreasonable
            if (result != null) {
                return result;
            }
            // otherwise drop through to simple
        case simple: {
            return denIsOne ? numStr : numStr + "/" + denStr;
        }
        case html: {
            return denIsOne ? numStr : "<sup>" + numStr + "</sup>" + "/<sub>" + denStr + "<sub>";
        }
        default: throw new UnsupportedOperationException();
        }
    }

    static final LocalizedNumberFormatter nf = NumberFormatter.with().precision(Precision.unlimited()).locale(Locale.ENGLISH);
    static final LocalizedNumberFormatter snf = NumberFormatter.with().precision(Precision.unlimited()).notation(Notation.engineering()).locale(Locale.ENGLISH);

    static String format(BigDecimal value) {
        return nf.format(value).toString();
        /*
         * TODO Change 1000000 to 1×10<sup>-6</sup>, etc.
         * Only for large/small numbers, eg:
         * ≥ 1,000,000.0, => 1×10<sup>6</sup> — more than 6 trailing 0's
         * ≤ 0.0000001 — more than 6 leading zeros
         *
         * relevant BigDecimal APIs:
         * 123.4567 scale: 4 bigint: 1234567
         * 123456700 scale: 0 bigint: 123456700
         * 1234567 scale: 0 bigint: 1234567
         * 0.1234567 scale: 7 bigint: 1234567
         */
        // return (Math.abs(newNumerator.scale()) > 10 ? snf : nf).format(newNumerator).toString();
        //      Only do this for trailing zeros (above test isn't right)
    }

    @Override
    public int compareTo(Rational other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }

    @Override
    public boolean equals(Object that) {
        return equals((Rational)that); // TODO fix later
    }

    public boolean equals(Rational that) {
        return numerator.equals(that.numerator)
            && denominator.equals(that.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    public Rational abs() {
        return numerator.signum() >= 0 ? this : this.negate();
    }

    static final BigInteger BI_TWO = BigInteger.valueOf(2);
    static final BigInteger BI_FIVE = BigInteger.valueOf(5);
    static final BigInteger BI_MINUS_ONE = BigInteger.valueOf(-1);

    static final BigDecimal BD_TWO = BigDecimal.valueOf(2);
    static final BigDecimal BD_FIVE = BigDecimal.valueOf(5);


    /**
     * Goal is to be able to display rationals in a short but exact form, like 1,234,567/3 or 1.234567E21/3.
     * To do this, find the smallest denominator (excluding powers of 2 and 5), and modify the numerator in the same way.
     * @param denominator TODO
     * @param current
     * @return
     */
    public static BigInteger minimalDenominator(Output<BigDecimal> outNumerator, BigInteger denominator) {
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
                Rational last = intermediates.get(intermediates.size()-1);
                intermediates.remove(intermediates.size()-1);
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
            return sequence.equals(((ContinuedFraction)obj).sequence);
        }
        @Override
        public int hashCode() {
            return sequence.hashCode();
        }
    }

    public BigInteger floor() {
        return numerator.divide(denominator);
    }

    public Rational symmetricDiff(Rational b) {
        return this.subtract(b)
            .divide(this.abs().add(b.abs()))
            .multiply(Rational.of(2))
            ;
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
            s.append(nf.format(intPart));
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
            occurs.put(p, pos);  // the position of the place with remainder p
            BigInteger p10 = p.multiply(BigInteger.TEN);
            BigInteger z = p10.divide(q); // index z of digit within: 0 ≤ z ≤ b-1
            p = p10.subtract(z.multiply(q));    // 0 ≤ p < q
            s = s.append(((char)('0' + z.intValue()))); // append the character of the digit
            if (p.equals(BigInteger.ZERO)) {
                return s.toString();
            } else if (s.length() > stringLimit) {
                return null;
            }
            pos -= 1;
        }
        int L = occurs.get(p)-pos; // the length of the reptend (being < q)
        s.insert(s.length()-L, REPTEND_MARKER);
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
}