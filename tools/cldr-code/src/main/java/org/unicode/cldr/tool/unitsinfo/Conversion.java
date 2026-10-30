package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import java.util.List;
import org.unicode.cldr.util.Rational;

/**
 * Whether and how a unit converts to its base unit, as TSV cells: {@code base = value × factorNum /
 * factorDen × 10^factorExp + offsetNum / offsetDen}. A format-only unit has no conversion, so its
 * other cells are empty. Cells that don't apply are empty too.
 */
public record Conversion(
        boolean formatOnly,
        String quantity,
        String baseUnit,
        String factorNum,
        String factorDen,
        String factorExp,
        String offsetNum,
        String offsetDen,
        String exactness,
        String special) {

    /** The conversion of a format-only unit, such as temperature-generic: none. */
    public static final Conversion NONE = new Conversion(true, "", "", "", "", "", "", "", "", "");

    /** A conversion that isn't linear, such as beaufort. */
    static Conversion special(String quantity, String baseUnit, String special) {
        return new Conversion(false, quantity, baseUnit, "", "", "", "", "", "", special);
    }

    /** A linear conversion. Throws if a number doesn't fit in its signed integer type. */
    static Conversion linear(
            String unit,
            String quantity,
            String baseUnit,
            Rational factor,
            Rational offset,
            boolean approximate) {
        ScientificRational f = ScientificRational.of(factor);
        boolean hasOffset = !offset.equals(Rational.ZERO);
        return new Conversion(
                false,
                quantity,
                baseUnit,
                checkFits(unit, "factor_num", f.numerator(), Long.SIZE),
                checkFits(unit, "factor_den", f.denominator(), Long.SIZE),
                Integer.toString(f.exponent()),
                hasOffset ? checkFits(unit, "offset_num", offset.numerator, Integer.SIZE) : "",
                hasOffset ? checkFits(unit, "offset_den", offset.denominator, Integer.SIZE) : "",
                approximate ? "approximate" : "exact",
                "");
    }

    String toTsv() {
        return UnitsInfo.TAB.join(
                formatOnly,
                quantity,
                baseUnit,
                factorNum,
                factorDen,
                factorExp,
                offsetNum,
                offsetDen,
                exactness,
                special);
    }

    static Conversion parse(List<String> cells) {
        String formatOnly = cells.get(0);
        checkArgument(
                formatOnly.equals("true") || formatOnly.equals("false"),
                "format_only must be true or false, not %s",
                formatOnly);
        return new Conversion(
                formatOnly.equals("true"),
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                cells.get(5),
                cells.get(6),
                cells.get(7),
                cells.get(8),
                cells.get(9));
    }

    /**
     * Returns {@code value} as text, or throws if it doesn't fit in a signed {@code bits} integer.
     */
    private static String checkFits(String unit, String column, BigInteger value, int bits) {
        checkArgument(
                value.bitLength() < bits,
                "%s: %s = %s doesn't fit in a signed %s-bit integer",
                unit,
                column,
                value,
                bits);
        return value.toString();
    }
}
