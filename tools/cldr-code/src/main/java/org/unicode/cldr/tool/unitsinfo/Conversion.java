package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import org.unicode.cldr.util.Rational;

/**
 * Whether and how a unit converts to its base unit, as TSV cells: {@code base = value × factorNum /
 * factorDen × 10^factorExp + offsetNum / offsetDen}. A format-only unit has no conversion, so its
 * other cells are empty. Cells that don't apply are empty too.
 */
public record Conversion(
        boolean formatOnly,
        String quantityId,
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
    public static final Conversion NONE =
            new Conversion(true, "", "", "", "", "", "", "", "", "", "");

    /** A conversion that isn't linear, such as beaufort. */
    static Conversion special(String quantity, String baseUnit, String special) {
        return new Conversion(false, "", quantity, baseUnit, "", "", "", "", "", "", special);
    }

    /**
     * A linear conversion. {@link UnitsInfo#integerProblems} checks that its numbers fit their
     * integer sizes.
     */
    static Conversion linear(
            String quantity,
            String baseUnit,
            Rational factor,
            Rational offset,
            boolean approximate) {
        ScientificRational f = ScientificRational.of(factor);
        boolean hasOffset = !offset.equals(Rational.ZERO);
        return new Conversion(
                false,
                "",
                quantity,
                baseUnit,
                f.numerator().toString(),
                f.denominator().toString(),
                Integer.toString(f.exponent()),
                hasOffset ? offset.numerator.toString() : "",
                hasOffset ? offset.denominator.toString() : "",
                approximate ? "approximate" : "exact",
                "");
    }

    /** This conversion with the stable ID of its quantity. */
    Conversion withQuantityId(int id) {
        return new Conversion(
                formatOnly,
                Integer.toString(id),
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

    String toTsv() {
        return UnitsInfo.TAB.join(
                formatOnly,
                quantityId,
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
                cells.get(9),
                cells.get(10));
    }
}
