package com.ibm.icu.text;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;

/**
 * @internal CLDR
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class FixedDecimal extends Number implements Comparable<FixedDecimal>, IFixedDecimal {
    private static final long serialVersionUID = -4756200506571685661L;

    final double source;

    final int visibleDecimalDigitCount;

    final int visibleDecimalDigitCountWithoutTrailingZeros;

    final long decimalDigits;

    final long decimalDigitsWithoutTrailingZeros;

    final long integerValue;

    final boolean hasIntegerValue;

    final boolean isNegative;

    final int exponent;

    private final int baseFactor;

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public double getSource() {
        return source;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getVisibleDecimalDigitCount() {
        return visibleDecimalDigitCount;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getVisibleDecimalDigitCountWithoutTrailingZeros() {
        return visibleDecimalDigitCountWithoutTrailingZeros;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public long getDecimalDigits() {
        return decimalDigits;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public long getDecimalDigitsWithoutTrailingZeros() {
        return decimalDigitsWithoutTrailingZeros;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public long getIntegerValue() {
        return integerValue;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isHasIntegerValue() {
        return hasIntegerValue;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isNegative() {
        return isNegative;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getBaseFactor() {
        return baseFactor;
    }

    static final long MAX = (long)1E18;

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     * @param n is the original number
     * @param v number of digits to the right of the decimal place. e.g 1.00 = 2 25. = 0
     * @param f Corresponds to f in the plural rules grammar.
     *   The digits to the right of the decimal place as an integer. e.g 1.10 = 10
     * @param e Suppressed exponent for scientific and compact notation
     */
    @Deprecated
    public FixedDecimal(double n, int v, long f, int e) {
        isNegative = n < 0;
        source = isNegative ? -n : n;
        visibleDecimalDigitCount = v;
        decimalDigits = f;
        integerValue = n > MAX
            ? MAX
                : (long)n;
        exponent = e;
        hasIntegerValue = source == integerValue;
        // check values. TODO make into unit test.
        //
        //            long visiblePower = (int) Math.pow(10, v);
        //            if (fractionalDigits > visiblePower) {
        //                throw new IllegalArgumentException();
        //            }
        //            double fraction = intValue + (fractionalDigits / (double) visiblePower);
        //            if (fraction != source) {
        //                double diff = Math.abs(fraction - source)/(Math.abs(fraction) + Math.abs(source));
        //                if (diff > 0.00000001d) {
        //                    throw new IllegalArgumentException();
        //                }
        //            }
        if (f == 0) {
            decimalDigitsWithoutTrailingZeros = 0;
            visibleDecimalDigitCountWithoutTrailingZeros = 0;
        } else {
            long fdwtz = f;
            int trimmedCount = v;
            while ((fdwtz%10) == 0) {
                fdwtz /= 10;
                --trimmedCount;
            }
            decimalDigitsWithoutTrailingZeros = fdwtz;
            visibleDecimalDigitCountWithoutTrailingZeros = trimmedCount;
        }
        baseFactor = (int) Math.pow(10, v);
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public FixedDecimal(double n, int v, long f) {
        this(n, v, f, 0);
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static FixedDecimal createWithExponent(double n, int v, int e) {
        return new FixedDecimal(n,v,getFractionalDigits(n, v), e);
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public FixedDecimal(double n, int v) {
        this(n,v,getFractionalDigits(n, v));
    }

    private static int getFractionalDigits(double n, int v) {
        if (v == 0) {
            return 0;
        } else {
            if (n < 0) {
                n = -n;
            }
            int baseFactor = (int) Math.pow(10, v);
            long scaled = Math.round(n * baseFactor);
            return (int) (scaled % baseFactor);
        }
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public FixedDecimal(double n) {
        this(n, decimals(n));
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public FixedDecimal(long n) {
        this(n,0);
    }

    private static final long MAX_INTEGER_PART = 1000000000;
    /**
     * Return a guess as to the number of decimals that would be displayed. This is only a guess; callers should
     * always supply the decimals explicitly if possible. Currently, it is up to 6 decimals (without trailing zeros).
     * Returns 0 for infinities and nans.
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     *
     */
    @Deprecated
    public static int decimals(double n) {
        // Ugly...
        if (Double.isInfinite(n) || Double.isNaN(n)) {
            return 0;
        }
        if (n < 0) {
            n = -n;
        }
        if (n == Math.floor(n)) {
            return 0;
        }
        if (n < MAX_INTEGER_PART) {
            long temp = (long)(n * 1000000) % 1000000; // get 6 decimals
            for (int mask = 10, digits = 6; digits > 0; mask *= 10, --digits) {
                if ((temp % mask) != 0) {
                    return digits;
                }
            }
            return 0;
        } else {
            String buf = String.format(Locale.ENGLISH, "%1.15e", n);
            int ePos = buf.lastIndexOf('e');
            int expNumPos = ePos + 1;
            if (buf.charAt(expNumPos) == '+') {
                expNumPos++;
            }
            String exponentStr = buf.substring(expNumPos);
            int exponent = Integer.parseInt(exponentStr);
            int numFractionDigits = ePos - 2 - exponent;
            if (numFractionDigits < 0) {
                return 0;
            }
            for (int i=ePos-1; numFractionDigits > 0; --i) {
                if (buf.charAt(i) != '0') {
                    break;
                }
                --numFractionDigits;
            }
            return numFractionDigits;
        }
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only
     */
    @Deprecated
    private FixedDecimal (FixedDecimal other) {
        // Ugly, but necessary, because constructors must only call other
        // constructors in the first line of the body, and
        // FixedDecimal(String) was refactored to support exponents.
        this.source = other.source;
        this.visibleDecimalDigitCount = other.visibleDecimalDigitCount;
        this.visibleDecimalDigitCountWithoutTrailingZeros =
            other.visibleDecimalDigitCountWithoutTrailingZeros;
        this.decimalDigits = other.decimalDigits;
        this.decimalDigitsWithoutTrailingZeros =
            other.decimalDigitsWithoutTrailingZeros;
        this.integerValue = other.integerValue;
        this.hasIntegerValue = other.hasIntegerValue;
        this.isNegative = other.isNegative;
        this.exponent = other.exponent;
        this.baseFactor = other.baseFactor;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public FixedDecimal (String n) {
        // Ugly, but for samples we don't care.
        this(parseDecimalSampleRangeNumString(n));
    }

//    /**
//     * @internal CLDR
//     * @deprecated This API is ICU internal only
//     */
//    @Deprecated
//    private static FixedDecimal parseDecimalSampleRangeNumString(String num) {
//        if (num.contains("e")) {
//            int ePos = num.lastIndexOf('e');
//            int expNumPos = ePos + 1;
//            String exponentStr = num.substring(expNumPos);
//            int exponent = Integer.parseInt(exponentStr);
//            String fractionStr = num.substring(0, ePos);
//            return FixedDecimal.createWithExponent(
//                    Double.parseDouble(fractionStr),
//                    getVisibleFractionCount(fractionStr),
//                    exponent);
//        } else {
//            return new FixedDecimal(Double.parseDouble(num), getVisibleFractionCount(num));
//        }
//    }

    // The value of n needs to take the exponent into account
    public static FixedDecimal parseDecimalSampleRangeNumString(String num) {
        double n;
        int v;
        int exponent = 0;
        String fractionStr = num; // default
        if (num.contains("e")) {
            int ePos = num.lastIndexOf('e');
            int expNumPos = ePos + 1;
            String exponentStr = num.substring(expNumPos);
            exponent = Integer.parseInt(exponentStr);
            fractionStr = num.substring(0, ePos);

            // now adjust the fraction string according to the exponent
            // not the most efficient, but more reliable code for testing
            if (exponent != 0) {
                int decimalPos = fractionStr.indexOf('.');
                int decimalCount = 0;
                String integerPart = fractionStr;
                String fractionPart = "";
                if (decimalPos >= 0) {
                    decimalCount = fractionStr.length() - decimalPos - 1;
                    integerPart = fractionStr.substring(0,decimalPos);
                    fractionPart = fractionStr.substring(decimalPos+1);
                }

                if (decimalCount == exponent) { // 2.123e3 => 2123
                    fractionStr = integerPart + fractionPart;
                } else if (decimalCount > exponent) {   // 2.1234e3 => 2123.4
                    fractionStr = integerPart + fractionPart.substring(0,exponent) + "." + fractionPart.substring(exponent);
                } else { // decimalCount < exponent //   // 2.1e3 => 2100
                    fractionStr = integerPart + padEnd(fractionPart, exponent, '0');
                }
            }
        }
        n = Double.parseDouble(fractionStr);
        v = getVisibleFractionCount(fractionStr);
        return new FixedDecimal(n, v, getFractionalDigits(n, v), exponent);
    }


    private static String padEnd(String string, int minLength, char c) {
        StringBuilder sb = new StringBuilder(minLength);
        sb.append(string);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static int getVisibleFractionCount(String value) {
        value = value.trim();
        int decimalPos = value.indexOf('.') + 1;
        if (decimalPos == 0) {
            return 0;
        } else {
            return value.length() - decimalPos;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public double getPluralOperand(Operand operand) {
        switch(operand) {
        case n: return source;
        case i: return integerValue;
        case f: return decimalDigits;
        case t: return decimalDigitsWithoutTrailingZeros;
        case v: return visibleDecimalDigitCount;
        case w: return visibleDecimalDigitCountWithoutTrailingZeros;
        case e: return exponent;
        default: return source;
        }
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Operand getOperand(String t) {
        return Operand.valueOf(t);
    }

    /**
     * We're not going to care about NaN.
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public int compareTo(FixedDecimal other) {
        if (exponent != other.exponent) {
            return exponent < other.exponent ? -1 : 1;
        }
        if (integerValue != other.integerValue) {
            return integerValue < other.integerValue ? -1 : 1;
        }
        if (source != other.source) {
            return source < other.source ? -1 : 1;
        }
        if (visibleDecimalDigitCount != other.visibleDecimalDigitCount) {
            return visibleDecimalDigitCount < other.visibleDecimalDigitCount ? -1 : 1;
        }
        long diff = decimalDigits - other.decimalDigits;
        if (diff != 0) {
            return diff < 0 ? -1 : 1;
        }
        return 0;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) {
            return false;
        }
        if (arg0 == this) {
            return true;
        }
        if (!(arg0 instanceof FixedDecimal)) {
            return false;
        }
        FixedDecimal other = (FixedDecimal)arg0;
        return source == other.source && visibleDecimalDigitCount == other.visibleDecimalDigitCount && decimalDigits == other.decimalDigits
            && exponent == other.exponent;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return (int)(decimalDigits + 37 * (visibleDecimalDigitCount + (int)(37 * source)));
    }

    public static String toSampleString(IFixedDecimal source) {
        final double n = source.getPluralOperand(Operand.n);
        final int exponent = (int) source.getPluralOperand(Operand.e);
        final int visibleDecimalDigitCount = (int) source.getPluralOperand(Operand.v);
        if (exponent == 0) {
            return String.format(Locale.ROOT, "%." + visibleDecimalDigitCount + "f", n);
        } else {
            // we need to slide the exponent back

            int fixedV = visibleDecimalDigitCount + exponent;
            String baseString = String.format(Locale.ROOT, "%." + fixedV + "f",n/Math.pow(10,exponent));

            // HACK
            // However, we don't have enough information to round-trip if v == 0
            // So in that case we choose the shortest form,
            // so we have to have a hack to strip trailing fraction spaces.
            if (visibleDecimalDigitCount == 0) {
                for (int i = visibleDecimalDigitCount; i < fixedV; ++i) {
                    // TODO this code could and should be optimized, but for now...
                    if (baseString.endsWith("0")) {
                        baseString = baseString.substring(0,baseString.length()-1);
                        continue;
                    }
                    break;
                }
                if (baseString.endsWith(".")) {
                    baseString = baseString.substring(0,baseString.length()-1);
                }
            }
            return baseString + "e" + exponent;
        }
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public String toString() {
        return toSampleString(this);
//        if (exponent == 0) {
//            return String.format(Locale.ROOT, "%." + visibleDecimalDigitCount + "f", source);
//        } else {
//            // we need to slide the exponent back
//
//            int fixedV = visibleDecimalDigitCount + exponent;
//            String baseString = String.format(Locale.ROOT, "%." + fixedV + "f", getSource()/Math.pow(10,exponent));
//
//            // However, we don't have enough information to round-trip if v == 0
//            // So in that case we choose the shortest form,
//            // so we have to have a hack to strip trailing fraction spaces.
//            if (visibleDecimalDigitCount == 0) {
//                for (int i = visibleDecimalDigitCount; i < fixedV; ++i) {
//                    // TODO this code could and should be optimized, but for now...
//                    if (baseString.endsWith("0")) {
//                        baseString = baseString.substring(0,baseString.length()-1);
//                        continue;
//                    }
//                    break;
//                }
//                if (baseString.endsWith(".")) {
//                    baseString = baseString.substring(0,baseString.length()-1);
//                }
//            }
//
//            return baseString + "e" + exponent;
//        }
    }

//    // FixedDecimal.toString isn't working right.
//    public String xtoString() {
//        // we need to slide v up
//        final int v = getVisibleDecimalDigitCount();
//        final int exponent = getExponent();
//        if (exponent == 0) {
//            return String.format(Locale.ROOT, "%." + v + "f", getSource());
//        }
//        int fixedV = v + exponent;
//        String baseString = String.format(Locale.ROOT, "%." + fixedV + "f", getSource()/Math.pow(10,exponent));
//        // however, the format does not round trip.
//        // so we have to have a hack to strip trailing fraction spaces.
//        for (int i = v; i < fixedV; ++i) {
//            // TODO this code could and should be optimized, but for now...
//            if (baseString.endsWith("0")) {
//                baseString = baseString.substring(0,baseString.length()-1);
//                continue;
//            }
//            break;
//        }
//        if (baseString.endsWith(".")) {
//            baseString = baseString.substring(0,baseString.length()-1);
//        }
//        return baseString + "e" + exponent;
//    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean hasIntegerValue() {
        return hasIntegerValue;
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public int intValue() {
        // TODO Auto-generated method stub
        return (int) longValue();
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public long longValue() {
        if (exponent == 0) {
            return integerValue;
        } else {
            return (long) (Math.pow(10, exponent) * integerValue);
        }
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public float floatValue() {
        return (float) (source * Math.pow(10, exponent));
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public double doubleValue() {
        return (isNegative ? -source : source);
    }

    /**
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public long getShiftedValue() {
        return integerValue * baseFactor + decimalDigits;
    }

    private void writeObject(
        ObjectOutputStream out)
            throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in
        ) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }

    /**
     * {@inheritDoc}
     *
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public boolean isNaN() {
        return Double.isNaN(source);
    }

    /**
     * {@inheritDoc}
     *
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public boolean isInfinite() {
        return Double.isInfinite(source);
    }

    // would be convenient to have getExponent, like the other methods
    public int getExponent() {
        return exponent;
    }

}
