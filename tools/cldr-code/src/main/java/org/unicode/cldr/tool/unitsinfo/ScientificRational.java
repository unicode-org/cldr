package org.unicode.cldr.tool.unitsinfo;

import java.math.BigInteger;
import org.unicode.cldr.util.Rational;

/**
 * A positive number as {@code numerator / denominator × 10^exponent}, with no trailing zeros in
 * either part. Decimal factors get denominator 1: foot = 3048 × 10^-4.
 */
record ScientificRational(BigInteger numerator, BigInteger denominator, int exponent) {

    private static final BigInteger TWO = BigInteger.TWO;
    private static final BigInteger FIVE = BigInteger.valueOf(5);

    static ScientificRational of(Rational value) {
        BigInteger num = value.numerator;
        BigInteger den = value.denominator;
        int exponent = 0;
        // Make the denominator coprime to 10, using x/2 = 5x/10 and x/5 = 2x/10.
        while (isMultiple(den, TWO)) {
            den = den.divide(TWO);
            num = num.multiply(FIVE);
            exponent--;
        }
        while (isMultiple(den, FIVE)) {
            den = den.divide(FIVE);
            num = num.multiply(TWO);
            exponent--;
        }
        while (num.signum() != 0 && isMultiple(num, BigInteger.TEN)) {
            num = num.divide(BigInteger.TEN);
            exponent++;
        }
        // If the numerator is too big for a long, trade its 2s (or 5s) for 5s (or 2s) in the
        // denominator, using 2x/y = x/5y × 10. Only parsec needs this today.
        if (num.bitLength() >= Long.SIZE) {
            while (isMultiple(num, TWO)) {
                num = num.divide(TWO);
                den = den.multiply(FIVE);
                exponent++;
            }
            while (isMultiple(num, FIVE)) {
                num = num.divide(FIVE);
                den = den.multiply(TWO);
                exponent++;
            }
        }
        return new ScientificRational(num, den, exponent);
    }

    private static boolean isMultiple(BigInteger value, BigInteger factor) {
        return value.mod(factor).signum() == 0;
    }
}
