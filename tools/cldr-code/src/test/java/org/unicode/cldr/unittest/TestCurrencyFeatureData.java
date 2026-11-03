package org.unicode.cldr.unittest;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.FeatureRegistry;
import org.unicode.cldr.tool.FeatureRegistry.Feature;
import org.unicode.cldr.tool.GenerateCurrencyFeatureTestData;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;

/**
 * Checks the CLDR data that the feature-based currency test data relies on (the data guards), and
 * that every feature has test data and a data guard (the coverage check).
 *
 * <p>A data guard fails when CLDR data changed, so that a feature's core set no longer exercises
 * the feature. To fix it, find a new core set, update the guard and the core set in {@code
 * common/testData/currency/features.tsv}, and run {@link GenerateCurrencyFeatureTestData}. The
 * feature ID stays the same.
 */
public class TestCurrencyFeatureData extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestCurrencyFeatureData().run(args);
    }

    private static final String CATEGORY = GenerateCurrencyFeatureTestData.CATEGORY;
    private static final Splitter TAB = Splitter.on('\t');
    private static final Splitter COMMA = Splitter.on(',').omitEmptyStrings();

    /** The data guard of each feature: the CLDR facts that make its core set exercise it. */
    private final Map<String, Consumer<Feature>> guards =
            ImmutableMap.<String, Consumer<Feature>>builder()
                    .put(
                            "currency_decimal_separator_locale",
                            f -> assertOwnSymbol(f, "currencyDecimal", "decimal"))
                    .put(
                            "currency_decimal_separator_locale_compact",
                            f -> {
                                assertOwnSymbol(f, "currencyDecimal", "decimal");
                                assertCompactPatterns(f, "1000", "1000000");
                            })
                    .put(
                            "currency_decimal_separator_locale_name",
                            f -> assertOwnSymbol(f, "currencyDecimal", "decimal"))
                    .put(
                            "currency_decimal_separator_regular",
                            f -> assertNoSymbol(f, "currencyDecimal"))
                    .put(
                            "currency_group_separator_locale",
                            f -> assertOwnSymbol(f, "currencyGroup", "group"))
                    .put(
                            "currency_group_separator_regular",
                            f -> assertNoSymbol(f, "currencyGroup"))
                    .build();

    public void TestDataGuards() throws IOException {
        for (Feature feature : FeatureRegistry.read(CATEGORY)) {
            Consumer<Feature> guard = guards.get(feature.id());
            if (guard != null) { // A missing guard is reported by TestCoverage.
                guard.accept(feature);
            }
        }
    }

    /**
     * The coverage check: every feature has a data guard and a core TSV file with at least one row,
     * and the core TSV files only name features in the registry. (The check that every clause of
     * the TR35 coverage doc has a feature comes with the coverage doc.)
     */
    public void TestCoverage() throws IOException {
        Path directory = FeatureRegistry.directory(CATEGORY);
        Set<String> ids =
                FeatureRegistry.read(CATEGORY).stream()
                        .map(Feature::id)
                        .collect(Collectors.toSet());
        for (String id : ids) {
            assertTrue(id + " has a data guard", guards.containsKey(id));
            Path file = directory.resolve(FeatureRegistry.coreFileName(id));
            assertTrue(
                    id + " has a core TSV file with at least one row",
                    Files.exists(file) && Files.readAllLines(file).size() > 1);
        }
        for (String id : guards.keySet()) {
            assertTrue("The data guard " + id + " has a feature in features.tsv", ids.contains(id));
        }

        String header = "# " + String.join("\t", GenerateCurrencyFeatureTestData.COLUMNS);
        int columns = GenerateCurrencyFeatureTestData.COLUMNS.size();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "core_*.tsv")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                String id = name.substring("core_".length(), name.length() - ".tsv".length());
                if (!assertTrue(name + " belongs to a feature in features.tsv", ids.contains(id))) {
                    continue;
                }
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                assertEquals(name + ": header", header, lines.isEmpty() ? "" : lines.get(0));
                for (int i = 1; i < lines.size(); i++) {
                    String where = name + ":" + (i + 1);
                    List<String> cells = TAB.splitToList(lines.get(i));
                    if (!assertEquals(where + ": number of columns", columns, cells.size())) {
                        continue;
                    }
                    assertEquals(where + ": main_feature", id, cells.get(columns - 2));
                    for (String other : COMMA.split(cells.get(columns - 1))) {
                        assertTrue(
                                where + ": " + other + " is in features.tsv", ids.contains(other));
                    }
                }
            }
        }
    }

    /** Each locale of the core set has its own monetary symbol, different from the general one. */
    private void assertOwnSymbol(Feature feature, String monetary, String general) {
        for (String locale : feature.coreSet().get("locale")) {
            String own = GenerateCurrencyFeatureTestData.localeSymbol(locale, monetary);
            String regular = GenerateCurrencyFeatureTestData.localeSymbol(locale, general);
            String prefix = feature.id() + ": " + locale + " ";
            if (assertNotNull(prefix + "has " + monetary, own)) {
                assertNotEquals(prefix + monetary + " differs from " + general, regular, own);
            }
        }
    }

    /** No locale of the core set has the monetary symbol. */
    private void assertNoSymbol(Feature feature, String monetary) {
        for (String locale : feature.coreSet().get("locale")) {
            assertNull(
                    feature.id() + ": " + locale + " has no " + monetary,
                    GenerateCurrencyFeatureTestData.localeSymbol(locale, monetary));
        }
    }

    /** Each locale of the core set has a compact currency pattern, not "0", for each type. */
    private void assertCompactPatterns(Feature feature, String... types) {
        for (String locale : feature.coreSet().get("locale")) {
            CLDRFile file = CLDRConfig.getInstance().getCLDRFile(locale, true);
            String numberingSystem = file.getStringValue("//ldml/numbers/defaultNumberingSystem");
            for (String type : types) {
                String pattern =
                        file.getStringValue(
                                "//ldml/numbers/currencyFormats[@numberSystem=\""
                                        + numberingSystem
                                        + "\"]/currencyFormatLength[@type=\"short\"]"
                                        + "/currencyFormat[@type=\"standard\"]/pattern[@type=\""
                                        + type
                                        + "\"][@count=\"other\"]");
                String message =
                        feature.id() + ": " + locale + " has a compact pattern for " + type;
                if (assertNotNull(message, pattern)) {
                    assertNotEquals(message + " that isn't \"0\"", "0", pattern);
                }
            }
        }
    }
}
