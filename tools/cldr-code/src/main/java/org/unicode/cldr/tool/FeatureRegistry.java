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
 * Reads the feature registry of a test data category, {@code
 * common/testData/<category>/features.tsv}.
 *
 * <p>Each row is one feature: a behavior that a part of UTS #35 requires, with the {@code
 * (dimension = value)} pairs (the core set) that exercise it. The generator of the category writes
 * one core TSV file per feature, {@code core_<feature_id>.tsv}.
 */
public final class FeatureRegistry {
    /** The name of the registry file in the category's test data directory. */
    public static final String FILE_NAME = "features.tsv";

    /** The registry columns, in order. The header line is "# " followed by these, tab-separated. */
    public static final List<String> COLUMNS =
            ImmutableList.of("feature_id", "spec", "clause", "core_set", "summary");

    private static final Pattern FEATURE_ID = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)+");
    private static final Splitter TAB = Splitter.on('\t');
    private static final Splitter COMMA = Splitter.on(',').trimResults();
    private static final Splitter SEMICOLON = Splitter.on(';').trimResults().omitEmptyStrings();

    /**
     * One row of the registry.
     *
     * @param id the stable feature ID, such as {@code currency_decimal_separator_locale}
     * @param spec the UTS #35 link, such as {@code tr35-numbers.md#currencydecimal}
     * @param clauses the clauses of the coverage doc that the feature serves, such as {@code S1.1a}
     * @param coreSet the values of each dimension that exercise the feature, in file order
     * @param summary one sentence that describes the behavior
     */
    public record Feature(
            String id,
            String spec,
            List<String> clauses,
            Map<String, List<String>> coreSet,
            String summary) {}

    private FeatureRegistry() {}

    /**
     * Returns the test data directory of the category, such as {@code common/testData/currency}.
     */
    public static Path directory(String category) {
        return Path.of(CLDRPaths.TEST_DATA, category);
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
        Path path = directory(category).resolve(FILE_NAME);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String header = "# " + String.join("\t", COLUMNS);
        if (lines.isEmpty() || !lines.get(0).equals(header)) {
            throw new IllegalArgumentException(path + ": the first line must be: " + header);
        }
        ImmutableList.Builder<Feature> features = ImmutableList.builder();
        Set<String> ids = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String where = FILE_NAME + ":" + (i + 1);
            List<String> cells = TAB.splitToList(lines.get(i));
            if (cells.size() != COLUMNS.size()) {
                throw new IllegalArgumentException(
                        where + ": expected " + COLUMNS.size() + " columns, found " + cells.size());
            }
            String id = cells.get(0);
            if (!FEATURE_ID.matcher(id).matches() || !id.startsWith(category + "_")) {
                throw new IllegalArgumentException(where + ": invalid feature ID: " + id);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException(where + ": duplicate feature ID: " + id);
            }
            features.add(
                    new Feature(
                            id,
                            cells.get(1),
                            COMMA.splitToList(cells.get(2)),
                            parseCoreSet(cells.get(3), where),
                            cells.get(4)));
        }
        return features.build();
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
}
