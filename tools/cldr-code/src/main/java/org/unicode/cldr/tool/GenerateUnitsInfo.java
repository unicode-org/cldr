package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

/**
 * Generates {@code common/properties/units_info.tsv} and {@code
 * common/properties/unit_categories.tsv} from CLDR locale, validity, and supplemental unit data.
 *
 * <p>This tool exists in CLDR for three purposes:
 *
 * <ol>
 *   <li>Ensure permanent, stable 0-based numeric IDs ({@code unit_id} and {@code category_id}) for
 *       every unit and category across all releases, preserving IDs for deprecated entries and
 *       assigning new IDs only to newly added entries.
 *   <li>Verify that every conversion rate to the base unit ({@code factor_num}, {@code factor_den},
 *       {@code factor_exp}, {@code offset_num}, and {@code offset_den}) fits in signed integers, so
 *       any future CLDR unit that exceeds those bounds fails immediately and signals that
 *       downstream ICU libraries need to be updated.
 *   <li>Provide one canonical list of formattable units across all locales with release history
 *       ({@code since}, {@code deprecated_in}, and {@code replacement}) for generating built-in
 *       units in ICU4C, ICU4J, and ICU4X.
 * </ol>
 */
public class GenerateUnitsInfo {

    public static final String PROPERTIES_DIR = CLDRPaths.COMMON_DIRECTORY + "properties";
    public static final String UNITS_INFO_FILE = "units_info.tsv";
    public static final String UNIT_CATEGORIES_FILE = "unit_categories.tsv";

    public static final String UNITS_HEADER =
            "# unit_id\tcategory_id\tcategory\tunit\tquantity\tbase_unit\tfactor_num\tfactor_den\t"
                    + "factor_exp\toffset_num\toffset_den\texactness\tspecial\tstatus\tsince\t"
                    + "deprecated_in\treplacement";

    public static final String CATEGORIES_HEADER =
            "# category_id\tcategory\tstatus\tsince\tdeprecated_in\treplacement";

    private static final Set<String> NOT_CONVERTIBLE = ImmutableSet.of("generic");
    private static final Joiner TAB_JOINER = Joiner.on('\t');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');
    private static final Pattern UNIT_TYPE_PATTERN = Pattern.compile("<unit type=\"([^\"]+)\"");
    private static final Pattern DEPRECATED_ID_BLOCK_PATTERN =
            Pattern.compile(
                    "<id\\s+type=['\"]unit['\"]\\s+idStatus=['\"]deprecated['\"]>([^<]*)</id>",
                    Pattern.DOTALL);
    private static final Pattern EXPR_TOKEN_SPLITTER = Pattern.compile("[*/\\s]+");

    private static final BigInteger BI_TWO = BigInteger.valueOf(2);
    private static final BigInteger BI_FIVE = BigInteger.valueOf(5);
    private static final BigInteger BI_TEN = BigInteger.valueOf(10);
    private static final BigInteger BI_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private static final List<String> RELEASE_TAGS =
            ImmutableList.of(
                    "release-26",
                    "release-27",
                    "release-29",
                    "release-30",
                    "release-31",
                    "release-32",
                    "release-33",
                    "release-35",
                    "release-36",
                    "release-37",
                    "release-38",
                    "release-39",
                    "release-40",
                    "release-41",
                    "release-42",
                    "release-43",
                    "release-44",
                    "release-45",
                    "release-46",
                    "release-47",
                    "release-48",
                    "release-49-beta2");

    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final UnitConverter CONVERTER = SDI.getUnitConverter();

    public static final class ScientificRational {
        public final long numerator;
        public final long denominator;
        public final int exponent;

        public ScientificRational(long numerator, long denominator, int exponent) {
            this.numerator = numerator;
            this.denominator = denominator;
            this.exponent = exponent;
        }

        /**
         * Splits a reduced {@link Rational} into {@code (numerator / denominator) * 10^exponent}
         * where {@code numerator} and {@code denominator} fit in a signed 64-bit {@code long} and
         * have no trailing zeros (unless 0).
         */
        public static ScientificRational from(Rational rational) {
            BigInteger p = rational.numerator;
            BigInteger q = rational.denominator;
            if (p.equals(BigInteger.ZERO)) {
                return new ScientificRational(0L, 1L, 0);
            }
            int a = 0;
            while (q.mod(BI_TWO).equals(BigInteger.ZERO)) {
                q = q.divide(BI_TWO);
                a++;
            }
            int b = 0;
            while (q.mod(BI_FIVE).equals(BigInteger.ZERO)) {
                q = q.divide(BI_FIVE);
                b++;
            }
            int m = Math.max(a, b);
            BigInteger n = p.multiply(BI_TWO.pow(m - a)).multiply(BI_FIVE.pow(m - b));
            int exp = -m;
            while (!n.equals(BigInteger.ZERO) && n.mod(BI_TEN).equals(BigInteger.ZERO)) {
                n = n.divide(BI_TEN);
                exp++;
            }
            if (n.abs().compareTo(BI_LONG_MAX) > 0) {
                while (n.mod(BI_TWO).equals(BigInteger.ZERO)) {
                    n = n.divide(BI_TWO);
                    q = q.multiply(BI_FIVE);
                    exp++;
                }
                while (n.mod(BI_FIVE).equals(BigInteger.ZERO)) {
                    n = n.divide(BI_FIVE);
                    q = q.multiply(BI_TWO);
                    exp++;
                }
            }
            return new ScientificRational(n.longValueExact(), q.longValueExact(), exp);
        }
    }

    public static final class CategoryRow {
        public final int categoryId;
        public final String category;
        public final String status;
        public final String since;
        public final String deprecatedIn;
        public final String replacement;

        public CategoryRow(
                int categoryId,
                String category,
                String status,
                String since,
                String deprecatedIn,
                String replacement) {
            this.categoryId = categoryId;
            this.category = category;
            this.status = status;
            this.since = since;
            this.deprecatedIn = deprecatedIn;
            this.replacement = replacement;
        }

        public String toTsvLine() {
            return TAB_JOINER.join(categoryId, category, status, since, deprecatedIn, replacement);
        }
    }

    public static final class UnitRow {
        public final int unitId;
        public final int categoryId;
        public final String category;
        public final String unit;
        public final String quantity;
        public final String baseUnit;
        public final String factorNum;
        public final String factorDen;
        public final String factorExp;
        public final String offsetNum;
        public final String offsetDen;
        public final String exactness;
        public final String special;
        public final String status;
        public final String since;
        public final String deprecatedIn;
        public final String replacement;

        public UnitRow(
                int unitId,
                int categoryId,
                String category,
                String unit,
                String quantity,
                String baseUnit,
                String factorNum,
                String factorDen,
                String factorExp,
                String offsetNum,
                String offsetDen,
                String exactness,
                String special,
                String status,
                String since,
                String deprecatedIn,
                String replacement) {
            this.unitId = unitId;
            this.categoryId = categoryId;
            this.category = category;
            this.unit = unit;
            this.quantity = quantity;
            this.baseUnit = baseUnit;
            this.factorNum = factorNum;
            this.factorDen = factorDen;
            this.factorExp = factorExp;
            this.offsetNum = offsetNum;
            this.offsetDen = offsetDen;
            this.exactness = exactness;
            this.special = special;
            this.status = status;
            this.since = since;
            this.deprecatedIn = deprecatedIn;
            this.replacement = replacement;
        }

        public String toTsvLine() {
            return TAB_JOINER.join(
                    unitId,
                    categoryId,
                    category,
                    unit,
                    quantity,
                    baseUnit,
                    factorNum,
                    factorDen,
                    factorExp,
                    offsetNum,
                    offsetDen,
                    exactness,
                    special,
                    status,
                    since,
                    deprecatedIn,
                    replacement);
        }
    }

    public static void main(String[] args) {
        GenerateUnitsInfo generator = new GenerateUnitsInfo();
        generator.writeFiles();
    }

    public void writeFiles() {
        Path categoriesPath = Path.of(PROPERTIES_DIR, UNIT_CATEGORIES_FILE);
        Path unitsPath = Path.of(PROPERTIES_DIR, UNITS_INFO_FILE);
        String historyGitDir = CldrUtility.getProperty("CLDR_HISTORY_GIT_DIR", null);

        Map<String, CategoryRow> previousCategories =
                Files.exists(categoriesPath) ? readCategoryRows(categoriesPath) : Map.of();
        Map<String, UnitRow> previousUnits =
                Files.exists(unitsPath) ? readUnitRows(unitsPath) : Map.of();

        List<CategoryRow> categoryRows;
        List<UnitRow> unitRows;
        if (historyGitDir != null || previousCategories.isEmpty() || previousUnits.isEmpty()) {
            String gitDir = historyGitDir != null ? historyGitDir : CLDRPaths.BASE_DIRECTORY;
            Map<String, CategoryRow> catMap = new LinkedHashMap<>();
            unitRows = buildFromGitHistory(gitDir, previousCategories, previousUnits, catMap);
            categoryRows = new ArrayList<>(catMap.values());
        } else {
            categoryRows = buildCategoryRows(previousCategories);
            unitRows = buildUnitRows(categoryRows, previousUnits);
        }
        verifyCategoryIdStability(previousCategories, categoryRows);
        verifyUnitIdStability(previousUnits, unitRows);

        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(PROPERTIES_DIR, UNIT_CATEGORIES_FILE)) {
            pw.println(CATEGORIES_HEADER);
            for (CategoryRow row : categoryRows) {
                pw.println(row.toTsvLine());
            }
        }
        try (TempPrintWriter pw = TempPrintWriter.openUTF8Writer(PROPERTIES_DIR, UNITS_INFO_FILE)) {
            pw.println(UNITS_HEADER);
            for (UnitRow row : unitRows) {
                pw.println(row.toTsvLine());
            }
        }
    }

    public static Map<String, CategoryRow> readCategoryRows(Path path) {
        Map<String, CategoryRow> result = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = TAB_SPLITTER.splitToList(line);
                CategoryRow row =
                        new CategoryRow(
                                Integer.parseInt(parts.get(0)),
                                parts.get(1),
                                parts.get(2),
                                parts.get(3),
                                parts.get(4),
                                parts.get(5));
                result.put(row.category, row);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return result;
    }

    public static Map<String, UnitRow> readUnitRows(Path path) {
        Map<String, UnitRow> result = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = TAB_SPLITTER.splitToList(line);
                UnitRow row =
                        new UnitRow(
                                Integer.parseInt(parts.get(0)),
                                Integer.parseInt(parts.get(1)),
                                parts.get(2),
                                parts.get(3),
                                parts.get(4),
                                parts.get(5),
                                parts.get(6),
                                parts.get(7),
                                parts.get(8),
                                parts.get(9),
                                parts.get(10),
                                parts.get(11),
                                parts.get(12),
                                parts.get(13),
                                parts.get(14),
                                parts.get(15),
                                parts.get(16));
                result.put(row.unit, row);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return result;
    }

    /**
     * Scans {@code common/main/*.xml} to collect all unique formattable {@code <unit type="...">}
     * identifiers and maps each short unit ID to its category prefix.
     */
    public static Map<String, String> collectFormattableUnitCategories() {
        Set<String> localeLongUnits = new TreeSet<>();
        try (Stream<Path> paths = Files.list(Path.of(CLDRPaths.MAIN_DIRECTORY))) {
            paths.filter(p -> p.toString().endsWith(".xml"))
                    .forEach(
                            p -> {
                                try {
                                    String content = Files.readString(p, StandardCharsets.UTF_8);
                                    Matcher m = UNIT_TYPE_PATTERN.matcher(content);
                                    while (m.find()) {
                                        localeLongUnits.add(m.group(1));
                                    }
                                } catch (IOException e) {
                                    throw new ICUUncheckedIOException(e);
                                }
                            });
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }

        Set<String> deprecatedLongUnits =
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.deprecated);
        Map<String, String> unitToCategory = new TreeMap<>();
        for (String longId : localeLongUnits) {
            if (deprecatedLongUnits != null && deprecatedLongUnits.contains(longId)) {
                continue;
            }
            int dash = longId.indexOf('-');
            if (dash > 0) {
                unitToCategory.put(longId.substring(dash + 1), longId.substring(0, dash));
            }
        }
        return unitToCategory;
    }

    public static Map<String, String> getUnitAliases() {
        Map<String, String> aliases = new TreeMap<>();
        Map<String, R2<List<String>, String>> raw = SDI.getLocaleAliasInfo().get("unit");
        if (raw != null) {
            for (Entry<String, R2<List<String>, String>> entry : raw.entrySet()) {
                List<String> replacements = entry.getValue().get0();
                if (replacements != null && !replacements.isEmpty()) {
                    aliases.put(entry.getKey(), replacements.get(0));
                }
            }
        }
        return aliases;
    }

    public List<CategoryRow> buildCategoryRows(Map<String, CategoryRow> previousCategories) {
        return buildCategoryRows(
                previousCategories, collectFormattableUnitCategories(), ToolConstants.DEV_VERSION);
    }

    public List<CategoryRow> buildCategoryRows(
            Map<String, CategoryRow> previousCategories,
            Map<String, String> currentUnitToCategory,
            String currentVersion) {
        Set<String> activeCategories = new TreeSet<>(currentUnitToCategory.values());
        Set<String> allCategories = new TreeSet<>(previousCategories.keySet());
        allCategories.addAll(activeCategories);

        int maxCategoryId =
                previousCategories.values().stream().mapToInt(r -> r.categoryId).max().orElse(-1);

        List<CategoryRow> rows = new ArrayList<>();
        for (String category : allCategories) {
            CategoryRow prev = previousCategories.get(category);
            int id = prev != null ? prev.categoryId : ++maxCategoryId;
            String since = prev != null ? prev.since : currentVersion;
            boolean active = activeCategories.contains(category);
            String status =
                    !active ? "deprecated" : (since.equals(currentVersion) ? "new" : "regular");
            String deprecatedIn =
                    active
                            ? ""
                            : (prev != null && !prev.deprecatedIn.isEmpty()
                                    ? prev.deprecatedIn
                                    : currentVersion);
            String replacement = active ? "" : (prev != null ? prev.replacement : "");
            rows.add(new CategoryRow(id, category, status, since, deprecatedIn, replacement));
        }
        verifyCategoryIdStability(previousCategories, rows);
        return rows;
    }

    public List<UnitRow> buildUnitRows(
            List<CategoryRow> categoryRows, Map<String, UnitRow> previousUnits) {
        return buildUnitRows(
                categoryRows,
                previousUnits,
                collectFormattableUnitCategories(),
                getUnitAliases(),
                ToolConstants.DEV_VERSION);
    }

    public List<UnitRow> buildUnitRows(
            List<CategoryRow> categoryRows,
            Map<String, UnitRow> previousUnits,
            Map<String, String> currentUnitToCategory,
            Map<String, String> aliases,
            String currentVersion) {
        Map<String, Integer> categoryToId = new TreeMap<>();
        for (CategoryRow c : categoryRows) {
            categoryToId.put(c.category, c.categoryId);
        }

        Set<String> approximateConstants = getApproximateConstants();

        Set<String> allUnits = new TreeSet<>(previousUnits.keySet());
        allUnits.addAll(currentUnitToCategory.keySet());

        List<String> sortedUnits = new ArrayList<>(allUnits);
        sortedUnits.sort(
                Comparator.comparing(
                                (String u) ->
                                        currentUnitToCategory.containsKey(u)
                                                ? currentUnitToCategory.get(u)
                                                : previousUnits.get(u).category)
                        .thenComparing(u -> u));

        int maxUnitId = previousUnits.values().stream().mapToInt(r -> r.unitId).max().orElse(-1);

        List<UnitRow> rows = new ArrayList<>();
        for (String unit : sortedUnits) {
            UnitRow prev = previousUnits.get(unit);
            int unitId = prev != null ? prev.unitId : ++maxUnitId;
            String since = prev != null ? prev.since : currentVersion;
            boolean active = currentUnitToCategory.containsKey(unit);
            String category =
                    active ? currentUnitToCategory.get(unit) : (prev != null ? prev.category : "");
            int categoryId = categoryToId.get(category);
            String status =
                    !active ? "deprecated" : (since.equals(currentVersion) ? "new" : "regular");
            String deprecatedIn =
                    active
                            ? ""
                            : (prev != null && !prev.deprecatedIn.isEmpty()
                                    ? prev.deprecatedIn
                                    : currentVersion);
            String replacement = aliases.getOrDefault(unit, prev != null ? prev.replacement : "");

            rows.add(
                    createUnitRow(
                            unitId,
                            categoryId,
                            category,
                            unit,
                            status,
                            since,
                            deprecatedIn,
                            replacement,
                            approximateConstants));
        }
        rows.sort(Comparator.comparing((UnitRow r) -> r.category).thenComparing(r -> r.unit));
        verifyUnitIdStability(previousUnits, rows);
        return rows;
    }

    public static void verifyCategoryIdStability(
            Map<String, CategoryRow> previousCategories, List<CategoryRow> newRows) {
        int prevMaxId =
                previousCategories.values().stream().mapToInt(r -> r.categoryId).max().orElse(-1);
        Map<String, CategoryRow> byName = new LinkedHashMap<>();
        Set<Integer> seenIds = new LinkedHashSet<>();
        for (CategoryRow row : newRows) {
            if (row.categoryId < 0 || !seenIds.add(row.categoryId)) {
                throw new IllegalStateException(
                        "Invalid or duplicate category_id "
                                + row.categoryId
                                + " for "
                                + row.category);
            }
            byName.put(row.category, row);
            CategoryRow prev = previousCategories.get(row.category);
            if (prev != null) {
                if (row.categoryId != prev.categoryId) {
                    throw new IllegalStateException(
                            "category_id changed for "
                                    + row.category
                                    + ": "
                                    + prev.categoryId
                                    + " -> "
                                    + row.categoryId);
                }
                if (!row.since.equals(prev.since)) {
                    throw new IllegalStateException(
                            "since changed for category "
                                    + row.category
                                    + ": "
                                    + prev.since
                                    + " -> "
                                    + row.since);
                }
            } else if (row.categoryId <= prevMaxId) {
                throw new IllegalStateException(
                        "New category "
                                + row.category
                                + " assigned non-new category_id "
                                + row.categoryId
                                + " (prevMaxId="
                                + prevMaxId
                                + ")");
            }
        }
        for (String prevCat : previousCategories.keySet()) {
            if (!byName.containsKey(prevCat)) {
                throw new IllegalStateException("Previous category dropped: " + prevCat);
            }
        }
    }

    public static void verifyUnitIdStability(
            Map<String, UnitRow> previousUnits, List<UnitRow> newRows) {
        int prevMaxId = previousUnits.values().stream().mapToInt(r -> r.unitId).max().orElse(-1);
        Map<String, UnitRow> byName = new LinkedHashMap<>();
        Set<Integer> seenIds = new LinkedHashSet<>();
        for (UnitRow row : newRows) {
            if (row.unitId < 0 || !seenIds.add(row.unitId)) {
                throw new IllegalStateException(
                        "Invalid or duplicate unit_id " + row.unitId + " for " + row.unit);
            }
            byName.put(row.unit, row);
            UnitRow prev = previousUnits.get(row.unit);
            if (prev != null) {
                if (row.unitId != prev.unitId) {
                    throw new IllegalStateException(
                            "unit_id changed for "
                                    + row.unit
                                    + ": "
                                    + prev.unitId
                                    + " -> "
                                    + row.unitId);
                }
                if (!row.since.equals(prev.since)) {
                    throw new IllegalStateException(
                            "since changed for unit "
                                    + row.unit
                                    + ": "
                                    + prev.since
                                    + " -> "
                                    + row.since);
                }
            } else if (row.unitId <= prevMaxId) {
                throw new IllegalStateException(
                        "New unit "
                                + row.unit
                                + " assigned non-new unit_id "
                                + row.unitId
                                + " (prevMaxId="
                                + prevMaxId
                                + ")");
            }
        }
        for (String prevUnit : previousUnits.keySet()) {
            if (!byName.containsKey(prevUnit)) {
                throw new IllegalStateException("Previous unit dropped: " + prevUnit);
            }
        }
    }

    private UnitRow createUnitRow(
            int unitId,
            int categoryId,
            String category,
            String unit,
            String status,
            String since,
            String deprecatedIn,
            String replacement,
            Set<String> approximateConstants) {
        String lookupUnit =
                ("deprecated".equals(status) && !replacement.isEmpty()) ? replacement : unit;
        if (NOT_CONVERTIBLE.contains(lookupUnit)) {
            return new UnitRow(
                    unitId,
                    categoryId,
                    category,
                    unit,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    status,
                    since,
                    deprecatedIn,
                    replacement);
        }

        Output<String> metricUnit = new Output<>();
        ConversionInfo info = CONVERTER.parseUnitId(lookupUnit, metricUnit, false);
        if (info == null || metricUnit.value == null) {
            return new UnitRow(
                    unitId,
                    categoryId,
                    category,
                    unit,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    status,
                    since,
                    deprecatedIn,
                    replacement);
        }

        String quantity = CONVERTER.getQuantityFromBaseUnit(metricUnit.value);
        String baseUnit = quantity != null ? CONVERTER.getBaseUnitFromQuantity(quantity) : "";
        if (info.special != null && !info.special.isEmpty()) {
            return new UnitRow(
                    unitId,
                    categoryId,
                    category,
                    unit,
                    quantity != null ? quantity : "",
                    baseUnit != null ? baseUnit : "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    info.special,
                    status,
                    since,
                    deprecatedIn,
                    replacement);
        }

        ScientificRational sci = ScientificRational.from(info.factor);
        String offsetNum = "";
        String offsetDen = "";
        if (!info.offset.equals(Rational.ZERO)) {
            offsetNum = Long.toString(info.offset.numerator.longValueExact());
            offsetDen = Long.toString(info.offset.denominator.longValueExact());
        }
        String exactness =
                isUnitApproximate(lookupUnit, approximateConstants) ? "approximate" : "exact";
        return new UnitRow(
                unitId,
                categoryId,
                category,
                unit,
                quantity != null ? quantity : "",
                baseUnit != null ? baseUnit : "",
                Long.toString(sci.numerator),
                Long.toString(sci.denominator),
                Integer.toString(sci.exponent),
                offsetNum,
                offsetDen,
                exactness,
                "",
                status,
                since,
                deprecatedIn,
                replacement);
    }

    private static Set<String> getApproximateConstants() {
        Set<String> approx = new LinkedHashSet<>();
        Map<String, String> statusMap = SDI.getRationalParser().getConstantStatus();
        for (Entry<String, String> entry : statusMap.entrySet()) {
            if ("approximate".equals(entry.getValue())) {
                approx.add(entry.getKey());
            }
        }
        return approx;
    }

    private static boolean isUnitApproximate(String unit, Set<String> approximateConstants) {
        Map<String, TargetInfo> conversionData = CONVERTER.getInternalConversionData();
        UnitId unitId = CONVERTER.createUnitId(unit);
        List<Map<String, Integer>> maps =
                ImmutableList.of(unitId.numUnitsToPowers, unitId.denUnitsToPowers);
        for (Map<String, Integer> map : maps) {
            for (String rawSubunit : map.keySet()) {
                String simpleUnit =
                        UnitConverter.stripPrefix(rawSubunit, new Output<>(Rational.ONE));
                TargetInfo targetInfo = conversionData.get(simpleUnit);
                if (targetInfo == null) {
                    continue;
                }
                for (String expr : targetInfo.inputParameters.values()) {
                    if (expr == null) {
                        continue;
                    }
                    for (String token : EXPR_TOKEN_SPLITTER.split(expr)) {
                        if (approximateConstants.contains(token)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private List<UnitRow> buildFromGitHistory(
            String gitDir,
            Map<String, CategoryRow> previousCategories,
            Map<String, UnitRow> previousUnits,
            Map<String, CategoryRow> outCategories) {
        List<R2<String, Map<String, R2<String, String>>>> history = new ArrayList<>();
        for (String tag : RELEASE_TAGS) {
            String ver = tag.substring("release-".length()).split("-")[0];
            history.add(R2.of(ver, shortIdsAtTag(gitDir, tag)));
        }

        Map<String, String> currentFormattable = collectFormattableUnitCategories();
        Map<String, String> currentLongIds = new LinkedHashMap<>();
        for (Entry<String, String> entry : currentFormattable.entrySet()) {
            currentLongIds.put(entry.getValue() + "-" + entry.getKey(), "regular");
        }
        Set<String> deprecatedValidity =
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.deprecated);
        if (deprecatedValidity != null) {
            for (String depLong : deprecatedValidity) {
                currentLongIds.put(depLong, "deprecated");
            }
        }
        history.add(R2.of(ToolConstants.DEV_VERSION, toShortIds(currentLongIds)));

        Map<String, String> catFirst = new TreeMap<>();
        Map<String, String> catLast = new TreeMap<>();
        Map<String, Set<String>> catUnits = new TreeMap<>();
        List<String> versions = new ArrayList<>();
        Set<String> allUnits = new TreeSet<>();

        for (R2<String, Map<String, R2<String, String>>> step : history) {
            String ver = step.get0();
            versions.add(ver);
            allUnits.addAll(step.get1().keySet());
            for (Entry<String, R2<String, String>> e : step.get1().entrySet()) {
                String status = e.getValue().get0();
                String cat = e.getValue().get1();
                if ("regular".equals(status)) {
                    catFirst.putIfAbsent(cat, ver);
                    catLast.put(cat, ver);
                    catUnits.computeIfAbsent(cat, k -> new LinkedHashSet<>()).add(e.getKey());
                }
            }
        }

        Map<String, String> aliases = getUnitAliases();
        Set<String> approximateConstants = getApproximateConstants();
        Map<String, String> unitCategoryNow = new TreeMap<>();
        List<String[]> rawUnitMeta = new ArrayList<>();

        for (String unit : allUnits) {
            String since = null;
            String deprecatedIn = null;
            String category = null;
            for (R2<String, Map<String, R2<String, String>>> step : history) {
                String ver = step.get0();
                R2<String, String> stCat = step.get1().get(unit);
                String st = stCat != null ? stCat.get0() : null;
                String cat = stCat != null ? stCat.get1() : null;
                if ("regular".equals(st) && since == null) {
                    since = ver;
                }
                if (cat != null) {
                    category = cat;
                }
                if (since != null
                        && deprecatedIn == null
                        && ("deprecated".equals(st) || st == null)) {
                    deprecatedIn = ver;
                }
                if ("regular".equals(st)) {
                    deprecatedIn = null;
                }
            }
            if (since == null) {
                continue;
            }
            unitCategoryNow.put(unit, category);
            String status =
                    deprecatedIn != null
                            ? "deprecated"
                            : (since.equals(ToolConstants.DEV_VERSION) ? "new" : "regular");
            String replacement = aliases.getOrDefault(unit, "");
            rawUnitMeta.add(
                    new String[] {
                        category,
                        unit,
                        status,
                        since,
                        deprecatedIn != null ? deprecatedIn : "",
                        replacement
                    });
        }

        int maxCatId =
                previousCategories.values().stream().mapToInt(r -> r.categoryId).max().orElse(-1);
        for (String cat : catFirst.keySet()) {
            CategoryRow prev = previousCategories.get(cat);
            int catId = prev != null ? prev.categoryId : ++maxCatId;
            boolean retired = !ToolConstants.DEV_VERSION.equals(catLast.get(cat));
            Set<String> moved = new LinkedHashSet<>();
            for (String u : catUnits.get(cat)) {
                String nowCat = unitCategoryNow.get(u);
                if (nowCat != null && !nowCat.equals(cat)) {
                    moved.add(nowCat);
                }
            }
            String since = prev != null ? prev.since : catFirst.get(cat);
            String status =
                    retired
                            ? "deprecated"
                            : (ToolConstants.DEV_VERSION.equals(since) ? "new" : "regular");
            String deprecatedIn =
                    retired ? versions.get(versions.indexOf(catLast.get(cat)) + 1) : "";
            String replacement = retired && moved.size() == 1 ? moved.iterator().next() : "";
            outCategories.put(
                    cat, new CategoryRow(catId, cat, status, since, deprecatedIn, replacement));
        }

        rawUnitMeta.sort(Comparator.comparing((String[] a) -> a[0]).thenComparing(a -> a[1]));
        List<UnitRow> unitRows = new ArrayList<>();
        int maxUnitId = previousUnits.values().stream().mapToInt(r -> r.unitId).max().orElse(-1);
        for (String[] meta : rawUnitMeta) {
            String category = meta[0];
            String unit = meta[1];
            UnitRow prev = previousUnits.get(unit);
            int unitId = prev != null ? prev.unitId : ++maxUnitId;
            String status = meta[2];
            String since = prev != null ? prev.since : meta[3];
            String deprecatedIn = meta[4];
            String replacement = meta[5];
            int categoryId = outCategories.get(category).categoryId;
            unitRows.add(
                    createUnitRow(
                            unitId,
                            categoryId,
                            category,
                            unit,
                            status,
                            since,
                            deprecatedIn,
                            replacement,
                            approximateConstants));
        }
        return unitRows;
    }

    private static Map<String, R2<String, String>> shortIdsAtTag(String gitDir, String tag) {
        Map<String, String> longIds = new LinkedHashMap<>();
        for (String path : ImmutableList.of("common/main/root.xml", "common/main/en.xml")) {
            String xml = gitShow(gitDir, tag, path);
            if (xml != null) {
                Matcher m = UNIT_TYPE_PATTERN.matcher(xml);
                while (m.find()) {
                    longIds.put(m.group(1), "regular");
                }
            }
        }
        String validityXml = gitShow(gitDir, tag, "common/validity/unit.xml");
        if (validityXml != null) {
            Matcher blockMatcher = DEPRECATED_ID_BLOCK_PATTERN.matcher(validityXml);
            while (blockMatcher.find()) {
                for (String token : blockMatcher.group(1).trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        longIds.put(token, "deprecated");
                    }
                }
            }
        }
        return toShortIds(longIds);
    }

    private static Map<String, R2<String, String>> toShortIds(Map<String, String> longIds) {
        Map<String, R2<String, String>> out = new LinkedHashMap<>();
        for (Entry<String, String> entry : longIds.entrySet()) {
            String lid = entry.getKey();
            String status = entry.getValue();
            int dash = lid.indexOf('-');
            if (dash > 0) {
                String cat = lid.substring(0, dash);
                String sid = lid.substring(dash + 1);
                if ("regular".equals(status) || !out.containsKey(sid)) {
                    out.put(sid, R2.of(status, cat));
                }
            }
        }
        return out;
    }

    private static String gitShow(String gitDir, String tag, String path) {
        ProcessBuilder pb = new ProcessBuilder("git", "-C", gitDir, "show", tag + ":" + path);
        try {
            Process proc = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                while ((n = reader.read(buf)) >= 0) {
                    sb.append(buf, 0, n);
                }
            }
            int code = proc.waitFor();
            return code == 0 ? sb.toString() : null;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
