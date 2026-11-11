package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import org.unicode.cldr.util.Rational;

/**
 * Whether and how a unit converts to its base unit, as TSV cells: {@code base = value × factorNum /
 * factorDen × 10^factorExp + offsetNum / offsetDen}. A format-only unit has no conversion, so its
 * other cells are empty. A mixed unit, such as foot-and-inch, has no single factor: it converts
 * through its parts. Cells that don't apply are empty too.
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
        String special,
        String systems) {

    /** The conversion of a format-only unit, such as temperature-generic: none. */
    public static final Conversion NONE =
            new Conversion(true, "", "", "", "", "", "", "", "", "", "", "");

    /** A conversion that isn't linear, such as beaufort. */
    static Conversion special(String quantity, String baseUnit, String special, String systems) {
        return new Conversion(
                false, "", quantity, baseUnit, "", "", "", "", "", "", special, systems);
    }

    /** The conversion of a mixed unit, such as foot-and-inch, which goes through its parts. */
    static Conversion mixed(String quantity, String baseUnit, String systems) {
        return new Conversion(false, "", quantity, baseUnit, "", "", "", "", "", "", "", systems);
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
            boolean approximate,
            String systems) {
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
                "",
                systems);
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
                special,
                systems);
    }

    String toTsv() {
        return UnitsInfo.TAB.join(formatOnly, convertibleCells());
    }

    /** The cells after format_only, which unit_combinations.tsv has without format_only. */
    String convertibleCells() {
        return UnitsInfo.TAB.join(
                quantityId,
                quantity,
                baseUnit,
                factorNum,
                factorDen,
                factorExp,
                offsetNum,
                offsetDen,
                exactness,
                special,
                systems);
    }

    /** Parses the conversion cells of a units_info.tsv row (column name → cell). */
    static Conversion parse(Map<String, String> cells) {
        String formatOnly = cells.get("format_only");
        checkArgument(
                formatOnly.equals("true") || formatOnly.equals("false"),
                "format_only must be true or false, not %s",
                formatOnly);
        return withFormatOnly(formatOnly.equals("true"), cells);
    }

    /** Parses the conversion cells of a unit_combinations.tsv row, which has no format_only. */
    static Conversion parseConvertible(Map<String, String> cells) {
        return withFormatOnly(false, cells);
    }

    private static Conversion withFormatOnly(boolean formatOnly, Map<String, String> cells) {
        return new Conversion(
                formatOnly,
                cells.get("quantity_id"),
                cells.get("quantity"),
                cells.get("base_unit"),
                cells.get("factor_num"),
                cells.get("factor_den"),
                cells.get("factor_exp"),
                cells.get("offset_num"),
                cells.get("offset_den"),
                cells.get("exactness"),
                cells.get("special"),
                cells.get("systems"));
    }
}
