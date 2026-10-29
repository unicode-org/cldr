package org.unicode.cldr.tool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;

/**
 * Generates {@code common/properties/units_info.tsv} (one row per formattable unit) and {@code
 * unit_categories.tsv} (one row per unit category). ICU4C, ICU4J, and ICU4X generate their built-in
 * units from these files.
 *
 * <p>The tool exists in CLDR for three things:
 *
 * <ol>
 *   <li><b>Stable IDs.</b> Every unit and category keeps its 0-based ID forever, also after it is
 *       deprecated, and a new one gets the next free ID. The tool fails if an ID would change.
 *   <li><b>Integer conversion rates.</b> Each conversion to the base unit is written as {@code
 *       factor_num / factor_den × 10^factor_exp + offset_num / offset_den}. The tool fails if the
 *       factor's numerator or denominator doesn't fit in a signed 64-bit integer, or an offset part
 *       doesn't fit in a signed 32-bit one. A change in these columns tells ICU and ICU4X to update
 *       their data.
 *   <li><b>History.</b> Each row has the release that first named the unit ({@code since}), and the
 *       release that deprecated it with its replacement.
 * </ol>
 *
 * <p>By default the tool updates the existing files: it keeps their IDs and history, and adds this
 * checkout as release {@link ToolConstants#DEV_VERSION}. When the files don't exist, or {@code
 * -DCLDR_HISTORY_GIT_DIR} is set, it rebuilds the history from the release tags (CLDR 24 and later)
 * of that git clone (default: this checkout), still keeping existing IDs.
 */
public class GenerateUnitsInfo {

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

    /** CLDR 24 is the first release with category-prefixed unit IDs, such as "length-meter". */
    private static final int FIRST_HISTORY_RELEASE = 24;

    private static final Joiner TAB = Joiner.on('\t');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');
    private static final Splitter WHITESPACE =
            Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();
    private static final Pattern UNIT_TYPE = Pattern.compile("<unit type=\"([^\"]+)\"");
    private static final Pattern DEPRECATED_UNITS =
            Pattern.compile(
                    "<id\\s+type=['\"]unit['\"]\\s+idStatus=['\"]deprecated['\"]>([^<]*)</id>");
    private static final Pattern EXPRESSION_TOKENS = Pattern.compile("[*/\\s]+");
    private static final BigInteger TWO = BigInteger.TWO;
    private static final BigInteger FIVE = BigInteger.valueOf(5);

    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final UnitConverter CONVERTER = SDI.getUnitConverter();

    // ---------------------------------------------------------------- the two files

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
                    Conversion.parse(cells.subList(4, 13)),
                    Status.parse(cells.get(13)),
                    cells.get(14),
                    cells.get(15),
                    cells.get(16));
        }
    }

    /**
     * The conversion to the base unit, as TSV cells: {@code base = value × factorNum / factorDen ×
     * 10^factorExp + offsetNum / offsetDen}. Cells that don't apply are empty.
     */
    public record Conversion(
            String quantity,
            String baseUnit,
            String factorNum,
            String factorDen,
            String factorExp,
            String offsetNum,
            String offsetDen,
            String exactness,
            String special) {

        /** No conversion, as for temperature-generic ("25°", with the scale clear from context). */
        static final Conversion NONE = new Conversion("", "", "", "", "", "", "", "", "");

        /** A conversion that isn't linear, such as beaufort. */
        static Conversion special(String quantity, String baseUnit, String special) {
            return new Conversion(quantity, baseUnit, "", "", "", "", "", "", special);
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
                    quantity,
                    baseUnit,
                    checkFits(unit, "factor_num", f.numerator(), Long.SIZE),
                    checkFits(unit, "factor_den", f.denominator(), Long.SIZE),
                    Integer.toString(f.exponent()),
                    hasOffset ? checkFits(unit, "offset_num", offset.numerator, Integer.SIZE) : "",
                    hasOffset
                            ? checkFits(unit, "offset_den", offset.denominator, Integer.SIZE)
                            : "",
                    approximate ? "approximate" : "exact",
                    "");
        }

        private String toTsv() {
            return TAB.join(
                    quantity, baseUnit, factorNum, factorDen, factorExp, offsetNum, offsetDen,
                    exactness, special);
        }

        private static Conversion parse(List<String> cells) {
            return new Conversion(
                    cells.get(0),
                    cells.get(1),
                    cells.get(2),
                    cells.get(3),
                    cells.get(4),
                    cells.get(5),
                    cells.get(6),
                    cells.get(7),
                    cells.get(8));
        }
    }

    /** The rows of both files. */
    public record UnitsInfo(List<CategoryRow> categories, List<UnitRow> units) {

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

        private void write(String dir) {
            writeTsv(dir, UNIT_CATEGORIES_FILE, CATEGORIES_HEADER, categoryLines());
            writeTsv(dir, UNITS_INFO_FILE, UNITS_HEADER, unitLines());
        }
    }

    // ---------------------------------------------------------------- generating

    public static void main(String[] args) {
        UnitsInfo previous = UnitsInfo.read(Path.of(PROPERTIES_DIR));
        String gitDir = CldrUtility.getProperty("CLDR_HISTORY_GIT_DIR", null);
        UnitsInfo current =
                gitDir == null && !previous.isEmpty()
                        ? update(previous)
                        : rebuild(gitDir != null ? gitDir : CLDRPaths.BASE_DIRECTORY, previous);
        current.write(PROPERTIES_DIR);
    }

    /** Adds this checkout to {@code previous} as release {@link ToolConstants#DEV_VERSION}. */
    public static UnitsInfo update(UnitsInfo previous) {
        History history = History.of(previous);
        history.add(ToolConstants.DEV_VERSION, currentUnits());
        return build(history, previous);
    }

    /**
     * Rebuilds the history from the release tags in {@code gitDir} plus this checkout, keeping the
     * IDs of {@code previous}. Fails if a release tag is missing.
     */
    public static UnitsInfo rebuild(String gitDir, UnitsInfo previous) {
        History history = new History();
        for (int release : historyReleases()) {
            history.add(Integer.toString(release), unitsAtTag(gitDir, "release-" + release));
        }
        history.add(ToolConstants.DEV_VERSION, currentUnits());
        return build(history, previous);
    }

    /**
     * Makes the rows for {@code history}, with the IDs and unit replacements of {@code previous},
     * and checks that no previous ID changed.
     */
    private static UnitsInfo build(History history, UnitsInfo previous) {
        String release = ToolConstants.DEV_VERSION;
        List<String> categoryOrder = history.categoryOrder();
        Map<String, Integer> categoryIds = assignIds(previous.categoryIds(), categoryOrder);
        List<CategoryRow> categories = new ArrayList<>();
        for (String category : categoryOrder) {
            History.CategoryEntry entry = history.categories().get(category);
            categories.add(
                    new CategoryRow(
                            categoryIds.get(category),
                            category,
                            entry.status(release),
                            entry.since(),
                            entry.deprecatedIn(),
                            entry.replacement()));
        }

        List<String> unitOrder = history.unitOrder();
        Map<String, Integer> unitIds = assignIds(previous.unitIds(), unitOrder);
        Map<String, String> replacements = new HashMap<>();
        previous.units().forEach(row -> replacements.put(row.unit(), row.replacement()));
        replacements.putAll(unitAliases());
        Set<String> approximateConstants = approximateConstants();
        List<UnitRow> units = new ArrayList<>();
        for (String unit : unitOrder) {
            History.UnitEntry entry = history.units().get(unit);
            Status status = entry.status(release);
            String replacement = replacements.getOrDefault(unit, "");
            // A deprecated ID converts like its replacement (pound-per-square-inch meant
            // pound-force).
            String convertible =
                    status == Status.DEPRECATED && !replacement.isEmpty() ? replacement : unit;
            units.add(
                    new UnitRow(
                            unitIds.get(unit),
                            categoryIds.get(entry.category()),
                            entry.category(),
                            unit,
                            conversion(convertible, approximateConstants),
                            status,
                            entry.since(),
                            entry.deprecatedIn(),
                            replacement));
        }
        UnitsInfo result = new UnitsInfo(categories, units);
        verifyStableIds(previous, result);
        return result;
    }

    // ---------------------------------------------------------------- stable IDs

    /**
     * Keeps the IDs in {@code previous}, and gives the other names, in order, the IDs after the
     * largest previous one (starting at 0).
     */
    public static Map<String, Integer> assignIds(
            Map<String, Integer> previous, List<String> names) {
        int next = previous.values().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (String name : names) {
            Integer id = previous.get(name);
            ids.put(name, id != null ? id : next++);
        }
        return ids;
    }

    /**
     * Throws unless every category and unit ID of {@code previous} is unchanged in {@code current}.
     */
    public static void verifyStableIds(UnitsInfo previous, UnitsInfo current) {
        verifyStableIds("category", previous.categoryIds(), current.categoryIds());
        verifyStableIds("unit", previous.unitIds(), current.unitIds());
    }

    /**
     * Throws if a previous name lost or changed its ID, if two names share an ID, or if a new
     * name's ID isn't above every previous one (so IDs are never reused).
     */
    public static void verifyStableIds(
            String kind, Map<String, Integer> previous, Map<String, Integer> current) {
        int previousMax = previous.values().stream().mapToInt(Integer::intValue).max().orElse(-1);
        Map<Integer, String> owners = new HashMap<>();
        current.forEach(
                (name, id) -> {
                    String other = owners.put(id, name);
                    checkState(
                            other == null, "%s ID %s is used by %s and %s", kind, id, other, name);
                    Integer old = previous.get(name);
                    if (old != null) {
                        checkState(
                                old.equals(id), "%s %s changed ID: %s -> %s", kind, name, old, id);
                    } else {
                        checkState(
                                id > previousMax,
                                "new %s %s got ID %s, but IDs up to %s are taken",
                                kind,
                                name,
                                id,
                                previousMax);
                    }
                });
        previous.forEach(
                (name, id) ->
                        checkState(
                                current.containsKey(name),
                                "%s %s (ID %s) was dropped",
                                kind,
                                name,
                                id));
    }

    // ---------------------------------------------------------------- history

    /**
     * When each unit and category was added and deprecated. It is built by applying releases in
     * order, starting from the existing files or from the first release tag.
     */
    public static final class History {
        /** A unit's latest category, first release, and deprecating release ("" while active). */
        public record UnitEntry(String category, String since, String deprecatedIn) {
            public Status status(String release) {
                return Status.of(since, deprecatedIn, release);
            }
        }

        /** A category's first release, deprecating release, and replacing category. */
        public record CategoryEntry(String since, String deprecatedIn, String replacement) {
            public Status status(String release) {
                return Status.of(since, deprecatedIn, release);
            }
        }

        private final Map<String, UnitEntry> units = new TreeMap<>();
        private final Map<String, CategoryEntry> categories = new TreeMap<>();

        /** The history recorded in existing files. */
        public static History of(UnitsInfo info) {
            History history = new History();
            for (CategoryRow row : info.categories()) {
                history.categories.put(
                        row.category(),
                        new CategoryEntry(row.since(), row.deprecatedIn(), row.replacement()));
            }
            for (UnitRow row : info.units()) {
                history.units.put(
                        row.unit(), new UnitEntry(row.category(), row.since(), row.deprecatedIn()));
            }
            return history;
        }

        /**
         * Applies a release, given its active units (unit ID → category). New units and categories
         * start in this release, missing ones are deprecated in it, and returning ones are active
         * again.
         */
        public void add(String release, Map<String, String> activeUnits) {
            Map<String, UnitEntry> before = new TreeMap<>(units);
            units.replaceAll(
                    (unit, e) ->
                            activeUnits.containsKey(unit) || !e.deprecatedIn().isEmpty()
                                    ? e
                                    : new UnitEntry(e.category(), e.since(), release));
            activeUnits.forEach(
                    (unit, category) -> {
                        UnitEntry old = units.get(unit);
                        units.put(
                                unit,
                                new UnitEntry(category, old == null ? release : old.since(), ""));
                    });

            Set<String> activeCategories = new HashSet<>(activeUnits.values());
            categories.replaceAll(
                    (category, e) ->
                            activeCategories.contains(category) || !e.deprecatedIn().isEmpty()
                                    ? e
                                    : new CategoryEntry(
                                            e.since(), release, movedTo(category, before)));
            for (String category : activeCategories) {
                CategoryEntry old = categories.get(category);
                categories.put(
                        category, new CategoryEntry(old == null ? release : old.since(), "", ""));
            }
        }

        /** The one category that the units of {@code category} moved to, or "" if not just one. */
        private String movedTo(String category, Map<String, UnitEntry> before) {
            TreeSet<String> targets = new TreeSet<>();
            before.forEach(
                    (unit, e) -> {
                        String now = units.get(unit).category();
                        if (e.category().equals(category) && !now.equals(category)) {
                            targets.add(now);
                        }
                    });
            return targets.size() == 1 ? targets.first() : "";
        }

        /** Categories in the order that new ones get IDs: alphabetical. */
        public List<String> categoryOrder() {
            return List.copyOf(categories.keySet());
        }

        /**
         * Units in file order, which is also the order that new ones get IDs: category, then ID.
         */
        public List<String> unitOrder() {
            return units.keySet().stream()
                    .sorted(
                            Comparator.comparing((String unit) -> units.get(unit).category())
                                    .thenComparing(Comparator.naturalOrder()))
                    .toList();
        }

        public Map<String, UnitEntry> units() {
            return Collections.unmodifiableMap(units);
        }

        public Map<String, CategoryEntry> categories() {
            return Collections.unmodifiableMap(categories);
        }
    }

    // ---------------------------------------------------------------- conversions

    /** The conversion of {@code unit} to its base unit, from CLDR's unit converter. */
    private static Conversion conversion(String unit, Set<String> approximateConstants) {
        if (unit.equals("generic")) {
            return Conversion.NONE; // temperature-generic: Celsius or Fahrenheit isn't known
        }
        Output<String> baseUnitId = new Output<>();
        ConversionInfo info = CONVERTER.parseUnitId(unit, baseUnitId, false);
        checkArgument(info != null && baseUnitId.value != null, "Can't convert %s", unit);
        String quantity = CONVERTER.getQuantityFromBaseUnit(baseUnitId.value);
        String baseUnit = quantity == null ? null : CONVERTER.getBaseUnitFromQuantity(quantity);
        checkArgument(baseUnit != null, "No quantity for %s (%s)", unit, baseUnitId.value);
        if (info.special != null && !info.special.isEmpty()) {
            return Conversion.special(quantity, baseUnit, info.special);
        }
        return Conversion.linear(
                unit,
                quantity,
                baseUnit,
                info.factor,
                info.offset,
                isApproximate(unit, approximateConstants));
    }

    /** True if a simple unit in {@code unit} converts with an approximate constant, like PI. */
    private static boolean isApproximate(String unit, Set<String> approximateConstants) {
        UnitId id = CONVERTER.createUnitId(unit);
        return Stream.concat(
                        id.numUnitsToPowers.keySet().stream(),
                        id.denUnitsToPowers.keySet().stream())
                .map(part -> UnitConverter.stripPrefix(part, new Output<>(Rational.ONE)))
                .map(simpleUnit -> CONVERTER.getInternalConversionData().get(simpleUnit))
                .filter(Objects::nonNull)
                .flatMap(target -> target.inputParameters.values().stream())
                .filter(Objects::nonNull)
                .flatMap(EXPRESSION_TOKENS::splitAsStream)
                .anyMatch(approximateConstants::contains);
    }

    /** Constants marked approximate in units.xml, and constants computed from them. */
    private static Set<String> approximateConstants() {
        return SDI.getRationalParser().getConstantStatus().entrySet().stream()
                .filter(e -> "approximate".equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** Deprecated unit IDs and their replacements, from the unit aliases in units.xml. */
    private static Map<String, String> unitAliases() {
        Map<String, String> aliases = new TreeMap<>();
        SDI.getLocaleAliasInfo()
                .getOrDefault("unit", Map.of())
                .forEach(
                        (unit, alias) -> {
                            List<String> replacements = alias.get0();
                            if (replacements != null && !replacements.isEmpty()) {
                                aliases.put(unit, replacements.get(0));
                            }
                        });
        return aliases;
    }

    /**
     * A positive number as {@code numerator / denominator × 10^exponent}, with no trailing zeros in
     * either part. Decimal factors get denominator 1: foot = 3048 × 10^-4.
     */
    record ScientificRational(BigInteger numerator, BigInteger denominator, int exponent) {

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

    // ---------------------------------------------------------------- reading CLDR data

    /** The active units (unit ID → category) in this checkout, from every locale file. */
    public static Map<String, String> currentUnits() {
        try (Stream<Path> files = Files.list(Path.of(CLDRPaths.MAIN_DIRECTORY))) {
            return activeUnits(
                    files.filter(file -> file.toString().endsWith(".xml"))
                            .map(GenerateUnitsInfo::read),
                    read(Path.of(CLDRPaths.COMMON_DIRECTORY, "validity", "unit.xml")));
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /** The active units at a release tag. Like ICU, it only looks at root.xml and en.xml. */
    private static Map<String, String> unitsAtTag(String gitDir, String tag) {
        List<String> locales =
                List.of(
                        requiredGitFile(gitDir, tag, "common/main/root.xml"),
                        requiredGitFile(gitDir, tag, "common/main/en.xml"));
        String validity = gitFile(gitDir, tag, "common/validity/unit.xml"); // added in CLDR 29
        return activeUnits(locales.stream(), validity == null ? "" : validity);
    }

    /**
     * Maps the unit in each {@code <unit type="category-unit">} to its category, skipping IDs that
     * validity/unit.xml marks deprecated.
     */
    private static Map<String, String> activeUnits(Stream<String> localeXmls, String validityXml) {
        Set<String> deprecated = new HashSet<>();
        Matcher block = DEPRECATED_UNITS.matcher(validityXml);
        while (block.find()) {
            deprecated.addAll(WHITESPACE.splitToList(block.group(1)));
        }
        Map<String, String> units = new TreeMap<>();
        localeXmls.forEach(
                xml -> {
                    Matcher type = UNIT_TYPE.matcher(xml);
                    while (type.find()) {
                        String longId = type.group(1);
                        int dash = longId.indexOf('-');
                        if (dash > 0 && !deprecated.contains(longId)) {
                            units.put(longId.substring(dash + 1), longId.substring(0, dash));
                        }
                    }
                });
        return units;
    }

    /** Major releases from CLDR 24 up to, but not including, the one in development. */
    private static List<Integer> historyReleases() {
        int dev = VersionInfo.getInstance(ToolConstants.DEV_VERSION).getMajor();
        return ToolConstants.CLDR_VERSIONS.stream()
                .map(version -> VersionInfo.getInstance(version).getMajor())
                .filter(major -> major >= FIRST_HISTORY_RELEASE && major < dev)
                .distinct()
                .sorted()
                .toList();
    }

    /** Reads a file at a release tag, and fails with a hint if the tag is missing. */
    private static String requiredGitFile(String gitDir, String tag, String path) {
        String text = gitFile(gitDir, tag, path);
        checkState(
                text != null,
                "Can't read %s at %s in %s. Fetch the release tags with: git -C %s fetch"
                        + " https://github.com/unicode-org/cldr.git"
                        + " 'refs/tags/release-*:refs/tags/release-*'",
                path,
                tag,
                gitDir,
                gitDir);
        return text;
    }

    /** Reads a file at a git tag, or returns null if git can't (missing tag or file). */
    private static String gitFile(String gitDir, String tag, String path) {
        try {
            Process git =
                    new ProcessBuilder("git", "-C", gitDir, "show", tag + ":" + path)
                            .redirectError(Redirect.DISCARD)
                            .start();
            String text = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return git.waitFor() == 0 ? text : null;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- file helpers

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
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
