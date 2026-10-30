package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
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
 * The rows of {@code units_info.tsv} and {@code unit_categories.tsv}, with their columns, and how
 * the files are read and written.
 */
public record UnitsInfo(List<CategoryRow> categories, List<UnitRow> units) {

    public static final String PROPERTIES_DIR = CLDRPaths.COMMON_DIRECTORY + "properties";
    public static final String UNITS_INFO_FILE = "units_info.tsv";
    public static final String UNIT_CATEGORIES_FILE = "unit_categories.tsv";

    private static final List<String> CATEGORY_COLUMNS =
            List.of("category_id", "category", "status", "since", "deprecated_in", "replacement");
    private static final List<String> UNIT_COLUMNS =
            List.of(
                    "unit_id",
                    "category_id",
                    "category",
                    "unit",
                    "format_only",
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
                    "replacement");
    public static final String CATEGORIES_HEADER = "# " + String.join("\t", CATEGORY_COLUMNS);
    public static final String UNITS_HEADER = "# " + String.join("\t", UNIT_COLUMNS);

    static final Joiner TAB = Joiner.on('\t');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');

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

    /** A row of units_info.tsv. The {@code unit} is the unit ID without its category prefix. */
    public record UnitRow(
            int id,
            int categoryId,
            String category,
            String unit,
            Conversion conversion,
            Status status,
            String since,
            String deprecatedIn,
            String replacement) {

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
                    replacement);
        }

        private static UnitRow parse(List<String> cells) {
            return new UnitRow(
                    Integer.parseInt(cells.get(0)),
                    Integer.parseInt(cells.get(1)),
                    cells.get(2),
                    cells.get(3),
                    Conversion.parse(cells.subList(4, 14)),
                    Status.parse(cells.get(14)),
                    cells.get(15),
                    cells.get(16),
                    cells.get(17));
        }
    }

    /** Reads both files from {@code dir}, or returns empty tables if neither exists. */
    public static UnitsInfo read(Path dir) {
        Path categoriesFile = dir.resolve(UNIT_CATEGORIES_FILE);
        Path unitsFile = dir.resolve(UNITS_INFO_FILE);
        if (!Files.exists(categoriesFile) && !Files.exists(unitsFile)) {
            return new UnitsInfo(List.of(), List.of());
        }
        return new UnitsInfo(
                readTsv(categoriesFile, CATEGORY_COLUMNS.size()).stream()
                        .map(CategoryRow::parse)
                        .toList(),
                readTsv(unitsFile, UNIT_COLUMNS.size()).stream().map(UnitRow::parse).toList());
    }

    public boolean isEmpty() {
        return categories.isEmpty() && units.isEmpty();
    }

    public Map<String, Integer> categoryIds() {
        return idsByName(categories, CategoryRow::category, CategoryRow::id);
    }

    public Map<String, Integer> unitIds() {
        return idsByName(units, UnitRow::unit, UnitRow::id);
    }

    /** The lines of unit_categories.tsv, without the header. */
    public List<String> categoryLines() {
        return categories.stream().map(CategoryRow::toTsv).toList();
    }

    /** The lines of units_info.tsv, without the header. */
    public List<String> unitLines() {
        return units.stream().map(UnitRow::toTsv).toList();
    }

    /** Writes both files to {@code dir}. */
    void write(String dir) {
        writeTsv(dir, UNIT_CATEGORIES_FILE, CATEGORIES_HEADER, categoryLines());
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
