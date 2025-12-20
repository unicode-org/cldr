package org.unicode.cldr.util;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

/**
 * An immutable value used for decimal numbers, targeted at use with plurals — NOTE: built for
 * testing: does not support negatives, inf, nan, values > 2^63 or fractions < 2^-63. These are
 * similar to ICU's FixedDecimals, but have additional capabilities, useful for CLDR. The number of
 * digits is limited to 19, whereby some of those can be fractional digits (including trailing
 * zeros). Eg, 1.0 ≠ 1
 */
public class CFixedDecimal implements Comparable<CFixedDecimal>, IFixedDecimal {
    @Override
    public double getPluralOperand(Operand operand) {

        switch (operand) {
            case n:
                return doubleValue();
            case i:
                return intValue();
            case f:
                return fractionDigits();
            case t:
                return getFractionDigitsNTZ();
            case v:
                return visibleDecimalCount;
            case w:
                return visibleDecimalCountNTZ;
            case j:
                return doubleValue(); // deprecated, but return value to match old behavior
            case c:
            case e:
                return exponent;
        }
        return 0;
    }

    public long getDigits() {
        return digits;
    }

    public double doubleValue() {
        return digits / (double) factor;
    }

    public long intValue() {
        return digits / factor;
    }

    public int getIntegerDigitCount() {
        return integerDigitCount;
    }

    public int getVisibleDecimalCount() {
        return visibleDecimalCount;
    }

    public long fractionDigits() {
        return digits % factor;
    }

    public long getFractionDigitsNTZ() {
        return fractionDigits()
                / PluralUtilities.pow10(visibleDecimalCount - visibleDecimalCountNTZ);
    }

    public int getExponent() {
        return exponent;
    }

    private final long digits;
    private final int visibleDecimalCount; // v
    private final int exponent; // e

    private final int factor; // computed at construction
    private final int integerDigitCount; // computed at construction
    private final int visibleDecimalCountNTZ; // computed at construction (without trailing zeros)

    public static CFixedDecimal fromString(String num) {
        Matcher matcher = PluralUtilities.FIXED_DECIMAL_STRING.matcher(num);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Bad format: " + RegexUtilities.showMismatch(matcher, num));
        }
        String integerString = matcher.group(1);
        String fractionDigitStr = matcher.group(2);
        String exponentStr0 = matcher.group(3);

        int exponent = exponentStr0 == null ? 0 : Integer.parseInt(exponentStr0);
        int v = fractionDigitStr == null ? 0 : fractionDigitStr.length();
        long digits =
                fractionDigitStr == null
                        ? Long.parseLong(integerString)
                        : Long.parseLong(integerString + fractionDigitStr);
        if (exponent != 0) {
            v -= exponent;
            if (v < 0) {
                digits *= PluralUtilities.pow10(-v);
                v = 0;
            }
        }
        return new CFixedDecimal(digits, v, exponent);
    }

    public static CFixedDecimal of(double number) {
        return of(number, decimals(number), 0);
    }

    public static CFixedDecimal of(double number, int fractionCount) {
        return of(number, fractionCount, 0);
    }

    private static int decimals(double number) {
        return FixedDecimal.decimals(number);
    }

    public static CFixedDecimal of(double number, int fractionCount, int exponent) {
        return new CFixedDecimal(
                (long) (number * PluralUtilities.pow10(fractionCount)), fractionCount, exponent);
    }

    public CFixedDecimal(long digits, int fractionCount, int exponent) {
        this.exponent = exponent;
        this.digits = digits;
        this.visibleDecimalCount = fractionCount;

        factor = PluralUtilities.pow10(fractionCount);
        integerDigitCount = PluralUtilities.digitCount(digits / factor);
        if (visibleDecimalCount == 0) {
            visibleDecimalCountNTZ = 0;
        } else {
            int visibleDecimalCountNTZ_ = fractionCount;
            long f = digits % factor;
            while (visibleDecimalCountNTZ_ > 0 && (f % 10) == 0) {
                f /= 10;
                --visibleDecimalCountNTZ_;
            }
            visibleDecimalCountNTZ = visibleDecimalCountNTZ_;
        }
    }

    @Override
    public boolean equals(Object obj) {
        CFixedDecimal that = (CFixedDecimal) obj;
        return digits == that.digits
                && visibleDecimalCount == that.visibleDecimalCount
                && exponent == that.exponent;
    }

    @Override
    public int hashCode() {
        return (int) (digits ^ (visibleDecimalCount << 16 + factor << 8 + exponent));
    }

    @Override
    public int compareTo(CFixedDecimal other) {
        return Comparator.comparingInt(CFixedDecimal::getVisibleDecimalCount)
                .thenComparing(CFixedDecimal::getDigits)
                .thenComparing(CFixedDecimal::getExponent)
                .compare(this, other);
    }

    public int compareTo(long digits, int fractionCount) {
        return Comparator.comparingInt(CFixedDecimal::getVisibleDecimalCount)
                .thenComparing(CFixedDecimal::getDigits)
                .thenComparing(CFixedDecimal::getExponent)
                .compare(this, new CFixedDecimal(digits, fractionCount, 0));
    }

    /**
     * Get the value immediately after `this`, according to the visible decimal count: vdc=0 => 1,
     * vdc=1 => 0.1, ...
     */
    public CFixedDecimal next() {
        return new CFixedDecimal(digits + 1, visibleDecimalCount, exponent);
    }

    /**
     * Return a key composed of the integer digit count, visible decimal count, and exponent. Used
     * for buckets with compact decimals
     */
    public Integer getKey() {
        return 10000 * integerDigitCount + 100 * visibleDecimalCount + exponent;
    }

    public Count getPluralCategory(PluralRules pluralRules) {
        String keyword = pluralRules.select(this);
        return Count.valueOf(keyword);
    }

    @Override
    /**
     * Formats a long scaled by visibleDigits. Doesn't use standard double etc formatting, because
     * it is easier to control the formatting this way.
     *
     * @param value
     * @return
     */
    public String toString() {
        final double n = getPluralOperand(Operand.n);
        final int exponent = (int) getPluralOperand(Operand.e);
        final int visibleDecimalDigitCount = (int) getPluralOperand(Operand.v);
        if (exponent == 0) {
            return String.format(Locale.ROOT, "%." + visibleDecimalDigitCount + "f", n);
        } else {
            // we need to slide the exponent back

            int fixedV = visibleDecimalDigitCount + exponent;
            String baseString =
                    String.format(Locale.ROOT, "%." + fixedV + "f", n / Math.pow(10, exponent));

            // HACK
            // However, we don't have enough information to round-trip if v == 0
            // So in that case we choose the shortest form,
            // so we have to have a hack to strip trailing fraction spaces.
            if (visibleDecimalDigitCount == 0) {
                for (int i = visibleDecimalDigitCount; i < fixedV; ++i) {
                    // TODO this code could and should be optimized, but for now...
                    if (baseString.endsWith("0")) {
                        baseString = baseString.substring(0, baseString.length() - 1);
                        continue;
                    }
                    break;
                }
                if (baseString.endsWith(".")) {
                    baseString = baseString.substring(0, baseString.length() - 1);
                }
            }
            return baseString + "c" + exponent;
        }
    }

    @Override
    public boolean isNaN() {
        return false;
    }

    @Override
    public boolean isInfinite() {
        return false;
    }

    @Override
    public boolean isHasIntegerValue() {
        return false;
    }
}
