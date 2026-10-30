package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.TempPrintWriter;

/**
 * The rows of {@code units_info.tsv}, {@code unit_categories.tsv}, and {@code unit_quantities.tsv},
 * with their columns, and how the files are read and written.
 */
public record UnitsInfo(
        List<CategoryRow> categories, List<QuantityRow> quantities, List<UnitRow> units) {

    public static final String PROPERTIES_DIR = CLDRPaths.COMMON_DIRECTORY + "properties";
    public static final String UNITS_INFO_FILE = "units_info.tsv";
    public static final String UNIT_CATEGORIES_FILE = "unit_categories.tsv";
    public static final String UNIT_QUANTITIES_FILE = "unit_quantities.tsv";

    private static final List<String> CATEGORY_COLUMNS =
            List.of("category_id", "category", "status", "since", "deprecated_in", "replacement");
    private static final List<String> QUANTITY_COLUMNS =
            List.of("quantity_id", "quantity", "base_unit", "status", "since", "deprecated_in");
    private static final List<String> UNIT_COLUMNS =
            List.of(
                    "unit_id",
                    "category_id",
                    "category",
                    "unit",
                    "format_only",
                    "quantity_id",
                    "quantity",
                    "base_unit",
                    "factor_num",
                    "factor_den",
                    "factor_exp",
                    "offset_num",
                    "offset_den",
                    "exactness",
                    "special",
                    "status",
                    "since",
                    "deprecated_in",
                    "replacement",
                    "reciprocal_quantity",
                    "reciprocal_base_unit");
    public static final String CATEGORIES_HEADER = "# " + String.join("\t", CATEGORY_COLUMNS);
    public static final String QUANTITIES_HEADER = "# " + String.join("\t", QUANTITY_COLUMNS);
    public static final String UNITS_HEADER = "# " + String.join("\t", UNIT_COLUMNS);

    static final Joiner TAB = Joiner.on('\t');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');

    /**
     * The signed integer size, in bits, that each numeric column must fit in, so that ICU and ICU4X
     * can store it in a fixed-size field. The IDs fit ICU4C's MeasureUnit fields (int16_t for the
     * unit, int8_t for the category). The tool won't write a number that doesn't fit, and
     * TestUnitsInfo fails. Raising a size means changing ICU and ICU4X first.
     */
    public static final Map<String, Integer> INTEGER_BITS =
            Map.of(
                    "unit_id", 16,
                    "category_id", 8,
                    "quantity_id", 16,
                    "factor_num", 64,
                    "factor_den", 64,
                    "factor_exp", 32,
                    "offset_num", 32,
                    "offset_den", 32);

    /** The {@code status} column. */
    public enum Status {
        /**
         * First named in the release being developed. Such rows become {@link #REGULAR} when {@link
         * ToolConstants#DEV_VERSION} moves on, so the files must be regenerated then.
         */
        NEW,
        REGULAR,
        DEPRECATED;

        /** The status in {@code release} of an entry; {@code deprecatedIn} is "" while active. */
        public static Status of(String since, String deprecatedIn, String release) {
            if (!deprecatedIn.isEmpty()) {
                return DEPRECATED;
            }
            return since.equals(release) ? NEW : REGULAR;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static Status parse(String cell) {
            return valueOf(cell.toUpperCase(Locale.ROOT));
        }
    }

    /** A row of unit_categories.tsv. */
    public record CategoryRow(
            int id,
            String category,
            Status status,
            String since,
            String deprecatedIn,
            String replacement) {

        private String toTsv() {
            return TAB.join(id, category, status, since, deprecatedIn, replacement);
        }

        private static CategoryRow parse(List<String> cells) {
            return new CategoryRow(
                    Integer.parseInt(cells.get(0)),
                    cells.get(1),
                    Status.parse(cells.get(2)),
                    cells.get(3),
                    cells.get(4),
                    cells.get(5));
        }
    }

    /** A row of unit_quantities.tsv: what a unit measures, such as length (base unit meter). */
    public record QuantityRow(
            int id,
            String quantity,
            String baseUnit,
            Status status,
            String since,
            String deprecatedIn) {

        private String toTsv() {
            return TAB.join(id, quantity, baseUnit, status, since, deprecatedIn);
        }

        private static QuantityRow parse(List<String> cells) {
            return new QuantityRow(
                    Integer.parseInt(cells.get(0)),
                    cells.get(1),
                    cells.get(2),
                    Status.parse(cells.get(3)),
                    cells.get(4),
                    cells.get(5));
        }
    }

    /**
     * A row of units_info.tsv. The {@code unit} is the unit ID without its category prefix. The
     * reciprocal quantity is the quantity in the same category whose base unit is 1 / this unit's
     * base unit (miles per gallon and liters per 100 km), or "" if there is none. Converting
     * between the two takes 1/x.
     */
    public record UnitRow(
            int id,
            int categoryId,
            String category,
            String unit,
            Conversion conversion,
            Status status,
            String since,
            String deprecatedIn,
            String replacement,
            String reciprocalQuantity,
            String reciprocalBaseUnit) {

        private String toTsv() {
            return TAB.join(
                    id,
                    categoryId,
                    category,
                    unit,
                    conversion.toTsv(),
                    status,
                    since,
                    deprecatedIn,
                    replacement,
                    reciprocalQuantity,
                    reciprocalBaseUnit);
        }

        private static UnitRow parse(List<String> cells) {
            return new UnitRow(
                    Integer.parseInt(cells.get(0)),
                    Integer.parseInt(cells.get(1)),
                    cells.get(2),
                    cells.get(3),
                    Conversion.parse(cells.subList(4, 15)),
                    Status.parse(cells.get(15)),
                    cells.get(16),
                    cells.get(17),
                    cells.get(18),
                    cells.get(19),
                    cells.get(20));
        }
    }

    /** Reads the files from {@code dir}, or returns empty tables if they don't exist. */
    public static UnitsInfo read(Path dir) {
        Path categoriesFile = dir.resolve(UNIT_CATEGORIES_FILE);
        Path quantitiesFile = dir.resolve(UNIT_QUANTITIES_FILE);
        Path unitsFile = dir.resolve(UNITS_INFO_FILE);
        if (!Files.exists(categoriesFile) && !Files.exists(unitsFile)) {
            return new UnitsInfo(List.of(), List.of(), List.of());
        }
        return new UnitsInfo(
                readTsv(categoriesFile, CATEGORY_COLUMNS.size()).stream()
                        .map(CategoryRow::parse)
                        .toList(),
                // unit_quantities.tsv was added after the other files, so it may be missing.
                Files.exists(quantitiesFile)
                        ? readTsv(quantitiesFile, QUANTITY_COLUMNS.size()).stream()
                                .map(QuantityRow::parse)
                                .toList()
                        : List.of(),
                readTsv(unitsFile, UNIT_COLUMNS.size()).stream().map(UnitRow::parse).toList());
    }

    public boolean isEmpty() {
        return categories.isEmpty() && quantities.isEmpty() && units.isEmpty();
    }

    public Map<String, Integer> categoryIds() {
        return idsByName(categories, CategoryRow::category, CategoryRow::id);
    }

    public Map<String, Integer> quantityIds() {
        return idsByName(quantities, QuantityRow::quantity, QuantityRow::id);
    }

    public Map<String, Integer> unitIds() {
        return idsByName(units, UnitRow::unit, UnitRow::id);
    }

    /** The lines of unit_categories.tsv, without the header. */
    public List<String> categoryLines() {
        return categories.stream().map(CategoryRow::toTsv).toList();
    }

    /** The lines of unit_quantities.tsv, without the header. */
    public List<String> quantityLines() {
        return quantities.stream().map(QuantityRow::toTsv).toList();
    }

    /** The lines of units_info.tsv, without the header. */
    public List<String> unitLines() {
        return units.stream().map(UnitRow::toTsv).toList();
    }

    /**
     * The numbers that don't fit their column's size in {@link #INTEGER_BITS}, such as "parsec:
     * factor_num = 126993525396961556736 doesn't fit in a signed 64-bit integer (it has 67 bits)".
     */
    public List<String> integerProblems() {
        List<String> problems = new ArrayList<>();
        for (CategoryRow row : categories) {
            checkSize(problems, row.category(), "category_id", Integer.toString(row.id()));
        }
        for (QuantityRow row : quantities) {
            checkSize(problems, row.quantity(), "quantity_id", Integer.toString(row.id()));
        }
        for (UnitRow row : units) {
            String unit = row.unit();
            Conversion c = row.conversion();
            checkSize(problems, unit, "unit_id", Integer.toString(row.id()));
            checkSize(problems, unit, "category_id", Integer.toString(row.categoryId()));
            checkSize(problems, unit, "quantity_id", c.quantityId());
            checkSize(problems, unit, "factor_num", c.factorNum());
            checkSize(problems, unit, "factor_den", c.factorDen());
            checkSize(problems, unit, "factor_exp", c.factorExp());
            checkSize(problems, unit, "offset_num", c.offsetNum());
            checkSize(problems, unit, "offset_den", c.offsetDen());
        }
        return problems;
    }

    private static void checkSize(List<String> problems, String row, String column, String cell) {
        if (cell.isEmpty()) {
            return;
        }
        BigInteger value = new BigInteger(cell);
        int bits = INTEGER_BITS.get(column);
        if (value.bitLength() >= bits) {
            problems.add(
                    String.format(
                            "%s: %s = %s doesn't fit in a signed %s-bit integer (it has %s bits)",
                            row, column, value, bits, value.bitLength()));
        }
    }

    /** Writes the files to {@code dir}. */
    void write(String dir) {
        writeTsv(dir, UNIT_CATEGORIES_FILE, CATEGORIES_HEADER, categoryLines());
        writeTsv(dir, UNIT_QUANTITIES_FILE, QUANTITIES_HEADER, quantityLines());
        writeTsv(dir, UNITS_INFO_FILE, UNITS_HEADER, unitLines());
    }

    /** The data lines of a TSV file, split into cells; each line must have {@code columns}. */
    private static List<List<String>> readTsv(Path file, int columns) {
        List<List<String>> rows = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                List<String> cells = TAB_SPLITTER.splitToList(line);
                checkArgument(
                        cells.size() == columns,
                        "%s: %s cells instead of %s: %s",
                        file,
                        cells.size(),
                        columns,
                        line);
                rows.add(cells);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return rows;
    }

    private static void writeTsv(String dir, String file, String header, List<String> lines) {
        try (TempPrintWriter out = TempPrintWriter.openUTF8Writer(dir, file)) {
            out.println(header);
            lines.forEach(out::println);
        }
    }

    private static <T> Map<String, Integer> idsByName(
            List<T> rows, Function<T, String> name, ToIntFunction<T> id) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (T row : rows) {
            checkState(
                    ids.put(name.apply(row), id.applyAsInt(row)) == null,
                    "Two rows for %s",
                    name.apply(row));
        }
        return ids;
    }
}
