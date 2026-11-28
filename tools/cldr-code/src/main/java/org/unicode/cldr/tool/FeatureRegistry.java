package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRPaths;

/**
 * Reads {@code features.tsv} (one row per feature) and {@code coverage.tsv} (one row per clause and
 * feature) of a test data category, in {@code common/testData/<category>/features}.
 *
 * <p>A feature is a behavior that UTS #35 requires, and its core set is the {@code dimension=value}
 * pairs that exercise it. The category's generator writes the test cases of each core set to {@code
 * core_<feature_id>.tsv}. {@code coverage.tsv} says which features test each clause, or why none
 * does.
 */
public final class FeatureRegistry {
    /** The name of the registry file in the category's feature directory. */
    public static final String FILE_NAME = "features.tsv";

    /** The registry columns, in order. The header line is "# " followed by these, tab-separated. */
    public static final List<String> COLUMNS =
            ImmutableList.of("feature_id", "core_set", "summary");

    /** The name of the coverage file in the category's feature directory. */
    public static final String COVERAGE_FILE_NAME = "coverage.tsv";

    /** The coverage columns, in order. The header line is "# " followed by these, tab-separated. */
    public static final List<String> COVERAGE_COLUMNS =
            ImmutableList.of(
                    "clause", "spec", "snippet", "summary", "feature_id", "status", "note");

    /** In the snippet column of {@code coverage.tsv}: the same snippet as in the row above. */
    public static final String SAME_SNIPPET = "↑↑↑↑↑";

    private static final Pattern FEATURE_ID = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)+");
    private static final Pattern CLAUSE = Pattern.compile("S\\d+\\.\\d+[a-z]?");
    private static final Pattern SPEC = Pattern.compile("tr35(?:-[A-Za-z]+)?\\.md#\\S+");
    private static final Splitter TAB = Splitter.on('\t');
    private static final Splitter COMMA = Splitter.on(',').trimResults();
    private static final Splitter SEMICOLON = Splitter.on(';').trimResults().omitEmptyStrings();

    /**
     * One row of the registry.
     *
     * @param id the stable feature ID, such as {@code currency_decimal_separator_locale}
     * @param coreSet the values of each dimension that exercise the feature, in file order
     * @param summary one sentence that describes the behavior
     */
    public record Feature(String id, Map<String, List<String>> coreSet, String summary) {}

    /** The status of a row of {@code coverage.tsv}. */
    public enum Status {
        /** A feature in the registry tests the clause. */
        COVERED("covered"),
        /** No feature tests the clause yet. */
        TODO("todo"),
        /** The clause waits for a second source of expected values; the note says why. */
        WAITING("waiting"),
        /** The clause can't or needn't be tested; the note says why. */
        OUT_OF_SCOPE("out_of_scope");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        /** Returns the label of the status in {@code coverage.tsv}. */
        public String label() {
            return label;
        }
    }

    /**
     * One row of {@code coverage.tsv}: a clause and one of its features.
     *
     * @param clause the clause ID of the TR35 coverage doc, such as {@code S1.1a}
     * @param spec the UTS #35 link, such as {@code tr35-numbers.md#currencydecimal}
     * @param snippet the UTS #35 text of the clause, with {@code <<} and {@code >>} in place of
     *     straight double quotes
     * @param summary one sentence that says what the clause requires
     * @param featureId the feature that tests the clause, or "" if there is none
     * @param status the status of the row
     * @param note the ticket or the reason for the status, or ""
     */
    public record CoverageRow(
            String clause,
            String spec,
            String snippet,
            String summary,
            String featureId,
            Status status,
            String note) {}

    private FeatureRegistry() {}

    /**
     * Returns the category's feature directory, such as {@code common/testData/currency/features}.
     */
    public static Path directory(String category) {
        return Path.of(CLDRPaths.TEST_DATA, category, "features");
    }

    /** Returns the name of the core TSV file of a feature. */
    public static String coreFileName(String featureId) {
        return "core_" + featureId + ".tsv";
    }

    /**
     * Reads the registry of the category.
     *
     * @throws IllegalArgumentException if the file doesn't have the expected format
     */
    public static List<Feature> read(String category) throws IOException {
        ImmutableList.Builder<Feature> features = ImmutableList.builder();
        Set<String> ids = new HashSet<>();
        for (Line line : lines(category, FILE_NAME, COLUMNS)) {
            String id = line.cell(0);
            checkFeatureId(id, category, line.where());
            if (!ids.add(id)) {
                throw new IllegalArgumentException(line.where() + ": duplicate feature ID: " + id);
            }
            features.add(new Feature(id, parseCoreSet(line.cell(1), line.where()), line.cell(2)));
        }
        return features.build();
    }

    /**
     * Reads the coverage file of the category. A {@code covered} row names a feature, and a {@code
     * waiting} or {@code out_of_scope} row has a note. A row with {@link #SAME_SNIPPET} gets the
     * snippet of the row above.
     *
     * @throws IllegalArgumentException if the file doesn't have the expected format
     */
    public static List<CoverageRow> readCoverage(String category) throws IOException {
        ImmutableList.Builder<CoverageRow> rows = ImmutableList.builder();
        Set<String> pairs = new HashSet<>();
        String snippet = null;
        for (Line line : lines(category, COVERAGE_FILE_NAME, COVERAGE_COLUMNS)) {
            String where = line.where();
            String clause = line.cell(0);
            String featureId = line.cell(4);
            String note = line.cell(6);
            if (!CLAUSE.matcher(clause).matches()) {
                throw new IllegalArgumentException(where + ": invalid clause ID: " + clause);
            }
            if (!SPEC.matcher(line.cell(1)).matches()) {
                throw new IllegalArgumentException(where + ": invalid spec link: " + line.cell(1));
            }
            if (!line.cell(2).equals(SAME_SNIPPET)) {
                snippet = line.cell(2);
            }
            if (snippet == null || snippet.isEmpty()) {
                throw new IllegalArgumentException(where + ": no snippet");
            }
            if (line.cell(3).isEmpty()) {
                throw new IllegalArgumentException(where + ": empty summary");
            }
            if (!featureId.isEmpty()) {
                checkFeatureId(featureId, category, where);
            }
            Status status = status(line.cell(5), where);
            if (status == Status.COVERED && featureId.isEmpty()) {
                throw new IllegalArgumentException(where + ": a covered row needs a feature ID");
            }
            if ((status == Status.WAITING || status == Status.OUT_OF_SCOPE) && note.isEmpty()) {
                throw new IllegalArgumentException(
                        where + ": a " + status.label() + " row needs a note");
            }
            if (!pairs.add(clause + " " + featureId)) {
                throw new IllegalArgumentException(where + ": duplicate row for " + clause);
            }
            rows.add(
                    new CoverageRow(
                            clause, line.cell(1), snippet, line.cell(3), featureId, status, note));
        }
        return rows.build();
    }

    /**
     * Parses a core set such as {@code locale=fr_CH; currency=EUR,USD}: {@code dimension=value}
     * pairs separated by "; ", with several values of one dimension separated by ",".
     */
    static Map<String, List<String>> parseCoreSet(String coreSet, String where) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String pair : SEMICOLON.split(coreSet)) {
            int equals = pair.indexOf('=');
            if (equals <= 0) {
                throw new IllegalArgumentException(where + ": expected dimension=value: " + pair);
            }
            String dimension = pair.substring(0, equals).trim();
            List<String> values = COMMA.splitToList(pair.substring(equals + 1));
            if (values.contains("")) {
                throw new IllegalArgumentException(where + ": empty value in: " + pair);
            }
            if (result.put(dimension, ImmutableList.copyOf(values)) != null) {
                throw new IllegalArgumentException(where + ": duplicate dimension: " + dimension);
            }
        }
        return ImmutableMap.copyOf(result);
    }

    private static Status status(String label, String where) {
        for (Status status : Status.values()) {
            if (status.label().equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException(where + ": invalid status: " + label);
    }

    private static void checkFeatureId(String id, String category, String where) {
        if (!FEATURE_ID.matcher(id).matches() || !id.startsWith(category + "_")) {
            throw new IllegalArgumentException(where + ": invalid feature ID: " + id);
        }
    }

    /** A data line of a TSV file, with its location for error messages. */
    private record Line(String where, List<String> cells) {
        String cell(int index) {
            return cells.get(index);
        }
    }

    /**
     * Returns the data lines of a TSV file in the category's feature directory. Every line has the
     * same number of tabs, and no cell contains {@code "}, so that GitHub shows the file as a
     * table.
     *
     * @throws IllegalArgumentException if the header, a number of columns, or a cell is wrong
     */
    private static List<Line> lines(String category, String fileName, List<String> columns)
            throws IOException {
        Path path = directory(category).resolve(fileName);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String header = "# " + String.join("\t", columns);
        if (lines.isEmpty() || !lines.get(0).equals(header)) {
            throw new IllegalArgumentException(path + ": the first line must be: " + header);
        }
        ImmutableList.Builder<Line> result = ImmutableList.builder();
        for (int i = 1; i < lines.size(); i++) {
            String where = fileName + ":" + (i + 1);
            if (lines.get(i).contains("\"")) {
                throw new IllegalArgumentException(where + ": a cell contains \"");
            }
            List<String> cells = TAB.splitToList(lines.get(i));
            if (cells.size() != columns.size()) {
                throw new IllegalArgumentException(
                        where + ": expected " + columns.size() + " columns, found " + cells.size());
            }
            result.add(new Line(where, cells));
        }
        return result.build();
    }
}
