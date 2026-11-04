package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkState;

import com.ibm.icu.util.VersionInfo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CategoryRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Complexity;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.DisplayName;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.QuantityRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Status;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.UnitRow;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

/**
 * Generates {@code common/properties/units_info.tsv} (one row per unit that CLDR data names),
 * {@code unit_categories.tsv} (one row per unit category), and {@code unit_quantities.tsv} (one row
 * per quantity, such as length). ICU4C, ICU4J, and ICU4X generate their built-in units from these
 * files.
 *
 * <p>The rows are the units with display names of their own, plus the units that CLDR data uses
 * without them: the units in the unit preferences (such as foot-and-inch and millimeter-per-hour)
 * and the base unit of each quantity (such as kilogram-square-meter-per-square-second for energy).
 * The {@code display_name} column tells them apart.
 *
 * <p>The tool exists in CLDR for three things:
 *
 * <ol>
 *   <li><b>Stable IDs.</b> Every unit, category, and quantity keeps its 0-based ID forever, also
 *       after it is deprecated, and a new one gets the next free ID. The tool fails if an ID would
 *       change.
 *   <li><b>Integer conversion rates.</b> Each conversion to the base unit is written as {@code
 *       factor_num / factor_den × 10^factor_exp + offset_num / offset_den}. The tool won't write a
 *       number that doesn't fit its column's signed integer size ({@link UnitsInfo#INTEGER_BITS}:
 *       64 bits for the factor, 32 for the exponent and offset), and TestUnitsInfo fails with a big
 *       warning. A change in these columns tells ICU and ICU4X to update their data.
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

    /** Each unit's base unit, or "" for a format-only unit: the history asks for it often. */
    private static final Map<String, String> BASE_UNITS = new HashMap<>();

    public static void main(String[] args) {
        UnitsInfo previous = UnitsInfo.read(Path.of(UnitsInfo.PROPERTIES_DIR));
        String gitDir = CldrUtility.getProperty("CLDR_HISTORY_GIT_DIR", null);
        UnitsInfo current =
                gitDir == null && !previous.isEmpty()
                        ? update(previous)
                        : rebuild(gitDir != null ? gitDir : CLDRPaths.BASE_DIRECTORY, previous);
        List<String> problems = current.integerProblems();
        checkState(
                problems.isEmpty(),
                "Not written, because these numbers are too big for ICU and ICU4X"
                        + " (UnitsInfo.INTEGER_BITS): %s",
                problems);
        current.write(UnitsInfo.PROPERTIES_DIR);
    }

    /** Adds this checkout to {@code previous} as release {@link ToolConstants#DEV_VERSION}. */
    public static UnitsInfo update(UnitsInfo previous) {
        UnitHistory history = UnitHistory.of(previous);
        addRelease(
                history,
                ToolConstants.DEV_VERSION,
                CldrUnits.current(),
                CldrUnits.preferenceUnits());
        return build(history, previous);
    }

    /**
     * Rebuilds the history from the release tags in {@code gitDir} plus this checkout, keeping the
     * IDs of {@code previous}. Fails if a release tag is missing.
     */
    public static UnitsInfo rebuild(String gitDir, UnitsInfo previous) {
        UnitHistory history = new UnitHistory();
        for (int release : CldrUnits.historyReleases()) {
            addRelease(
                    history,
                    Integer.toString(release),
                    CldrUnits.atRelease(gitDir, release),
                    CldrUnits.preferenceUnitsAtRelease(gitDir, release));
        }
        addRelease(
                history,
                ToolConstants.DEV_VERSION,
                CldrUnits.current(),
                CldrUnits.preferenceUnits());
        return build(history, previous);
    }

    /**
     * Applies a release with its units that have display names (unit ID → category) and the units
     * in its unit preferences. The other units of the release are its preference units and the base
     * unit of each unit, minus deprecated unit IDs.
     */
    private static void addRelease(
            UnitHistory history,
            String release,
            Map<String, String> namedUnits,
            Set<String> preferenceUnits) {
        Set<String> deprecatedIds = CldrUnits.aliases().keySet();
        Set<String> others = new TreeSet<>(preferenceUnits);
        others.removeAll(deprecatedIds);
        Set<String> users = new TreeSet<>(namedUnits.keySet());
        users.addAll(others);
        for (String unit : users) {
            String baseUnit = baseUnitOf(unit);
            if (!baseUnit.isEmpty()) {
                others.add(baseUnit);
            }
        }
        others.removeAll(namedUnits.keySet());
        history.add(release, namedUnits, others);
    }

    /** The base unit of {@code unit}, converting a deprecated ID like its replacement. */
    private static String baseUnitOf(String unit) {
        String baseUnit = BASE_UNITS.get(unit);
        if (baseUnit == null) {
            String convertible = CldrUnits.aliases().getOrDefault(unit, unit);
            baseUnit = UnitConversions.of(convertible).baseUnit();
            BASE_UNITS.put(unit, baseUnit);
        }
        return baseUnit;
    }

    /**
     * Makes the rows for {@code history}, with the IDs and unit replacements of {@code previous},
     * and checks that no previous ID changed.
     */
    private static UnitsInfo build(UnitHistory history, UnitsInfo previous) {
        String release = ToolConstants.DEV_VERSION;
        List<String> categoryOrder = history.categoryOrder();
        Map<String, Integer> categoryIds = StableIds.assign(previous.categoryIds(), categoryOrder);
        List<CategoryRow> categories = new ArrayList<>();
        for (String category : categoryOrder) {
            UnitHistory.CategoryEntry entry = history.categories().get(category);
            categories.add(
                    new CategoryRow(
                            categoryIds.get(category),
                            category,
                            entry.status(release),
                            entry.since(),
                            entry.deprecatedIn(),
                            entry.replacement()));
        }

        Map<String, String> replacements = new HashMap<>();
        previous.units().forEach(row -> replacements.put(row.unit(), row.replacement()));
        replacements.putAll(CldrUnits.aliases());
        Map<String, Conversion> conversions = new HashMap<>();
        Map<String, Complexity> complexities = new HashMap<>();
        for (String unit : history.units().keySet()) {
            String replacement = replacements.getOrDefault(unit, "");
            // A deprecated ID converts like its replacement (pound-per-square-inch meant
            // pound-force).
            boolean deprecated = history.units().get(unit).status(release) == Status.DEPRECATED;
            String convertible = deprecated && !replacement.isEmpty() ? replacement : unit;
            conversions.put(unit, UnitConversions.of(convertible));
            complexities.put(unit, UnitConversions.complexityOf(unit, convertible));
        }
        Map<String, String> unitCategories = unitCategories(history, conversions, release);
        Set<String> namedUnits = new HashSet<>();
        history.units()
                .forEach(
                        (unit, entry) -> {
                            if (entry.named() && entry.status(release) != Status.DEPRECATED) {
                                namedUnits.add(unit);
                            }
                        });

        // Rows by category (units without one last), then unit. New units get IDs in this order.
        List<String> unitOrder =
                history.units().keySet().stream()
                        .sorted(
                                Comparator.comparing(
                                                (String unit) -> unitCategories.get(unit).isEmpty())
                                        .thenComparing(unitCategories::get)
                                        .thenComparing(Comparator.naturalOrder()))
                        .toList();
        Map<String, Integer> unitIds = StableIds.assign(previous.unitIds(), unitOrder);
        Map<String, String> reciprocals = reciprocalQuantities(unitCategories, conversions);
        List<QuantityRow> quantities = quantityRows(history, conversions, previous, release);
        Map<String, Integer> quantityIds = new HashMap<>();
        quantities.forEach(row -> quantityIds.put(row.quantity(), row.id()));

        List<UnitRow> units = new ArrayList<>();
        for (String unit : unitOrder) {
            UnitHistory.UnitEntry entry = history.units().get(unit);
            String category = unitCategories.get(unit);
            Conversion conversion = conversions.get(unit);
            if (!conversion.quantity().isEmpty()) {
                conversion = conversion.withQuantityId(quantityIds.get(conversion.quantity()));
            }
            String reciprocal = reciprocals.get(unit);
            units.add(
                    new UnitRow(
                            unitIds.get(unit),
                            category.isEmpty() ? UnitRow.NO_CATEGORY : categoryIds.get(category),
                            category,
                            unit,
                            complexities.get(unit),
                            entry.named()
                                    ? DisplayName.OWN
                                    : UnitConversions.displayNameOf(unit, namedUnits),
                            conversion,
                            entry.status(release),
                            entry.since(),
                            entry.deprecatedIn(),
                            replacements.getOrDefault(unit, ""),
                            reciprocal,
                            reciprocal.isEmpty()
                                    ? ""
                                    : UnitConversions.reciprocalOf(conversion.baseUnit())));
        }
        UnitsInfo result = new UnitsInfo(categories, quantities, units);
        StableIds.verify(previous, result);
        return result;
    }

    /**
     * Each unit's category. A unit with display names has its own. Another unit gets the category
     * of the active named units with its quantity; if they have several, the one named like the
     * quantity (energy, not torque, for kilogram-square-meter-per-square-second). It gets "" if
     * there is no such category, as for kilogram-per-cubic-meter (mass-density).
     */
    private static Map<String, String> unitCategories(
            UnitHistory history, Map<String, Conversion> conversions, String release) {
        Map<String, Set<String>> categoriesByQuantity = new HashMap<>();
        history.units()
                .forEach(
                        (unit, entry) -> {
                            String quantity = conversions.get(unit).quantity();
                            if (entry.named()
                                    && entry.status(release) != Status.DEPRECATED
                                    && !quantity.isEmpty()) {
                                categoriesByQuantity
                                        .computeIfAbsent(quantity, q -> new TreeSet<>())
                                        .add(entry.category());
                            }
                        });
        Map<String, String> categories = new HashMap<>();
        history.units()
                .forEach(
                        (unit, entry) -> {
                            if (entry.named()) {
                                categories.put(unit, entry.category());
                                return;
                            }
                            String quantity = conversions.get(unit).quantity();
                            Set<String> candidates =
                                    categoriesByQuantity.getOrDefault(quantity, Set.of());
                            categories.put(
                                    unit,
                                    candidates.contains(quantity)
                                            ? quantity
                                            : candidates.size() == 1
                                                    ? candidates.iterator().next()
                                                    : "");
                        });
        return categories;
    }

    /**
     * One row per quantity of a unit, with stable IDs (new ones in alphabetical order). A quantity
     * starts in the first release of any of its units, and is deprecated once none of them is
     * active. A previous row keeps its since and base unit.
     */
    private static List<QuantityRow> quantityRows(
            UnitHistory history,
            Map<String, Conversion> conversions,
            UnitsInfo previous,
            String release) {
        Map<String, QuantityRow> previousRows = new HashMap<>();
        previous.quantities().forEach(row -> previousRows.put(row.quantity(), row));
        Map<String, List<String>> unitsByQuantity = new TreeMap<>();
        previousRows.keySet().forEach(quantity -> unitsByQuantity.put(quantity, new ArrayList<>()));
        conversions.forEach(
                (unit, c) -> {
                    if (!c.quantity().isEmpty()) {
                        unitsByQuantity
                                .computeIfAbsent(c.quantity(), q -> new ArrayList<>())
                                .add(unit);
                    }
                });
        Map<String, Integer> ids =
                StableIds.assign(previous.quantityIds(), List.copyOf(unitsByQuantity.keySet()));

        Comparator<String> byVersion = Comparator.comparing(VersionInfo::getInstance);
        List<QuantityRow> rows = new ArrayList<>();
        unitsByQuantity.forEach(
                (quantity, units) -> {
                    QuantityRow old = previousRows.get(quantity);
                    List<UnitHistory.UnitEntry> entries =
                            units.stream().map(history.units()::get).toList();
                    String since =
                            old != null
                                    ? old.since()
                                    : entries.stream()
                                            .map(UnitHistory.UnitEntry::since)
                                            .min(byVersion)
                                            .orElse(release);
                    String deprecatedIn = "";
                    if (entries.stream().noneMatch(e -> e.deprecatedIn().isEmpty())) {
                        deprecatedIn =
                                old != null && !old.deprecatedIn().isEmpty()
                                        ? old.deprecatedIn()
                                        : entries.stream()
                                                .map(UnitHistory.UnitEntry::deprecatedIn)
                                                .max(byVersion)
                                                .orElse(release);
                    }
                    String baseUnit =
                            units.isEmpty()
                                    ? old.baseUnit()
                                    : conversions.get(units.get(0)).baseUnit();
                    rows.add(
                            new QuantityRow(
                                    ids.get(quantity),
                                    quantity,
                                    baseUnit,
                                    Status.of(since, deprecatedIn, release),
                                    since,
                                    deprecatedIn));
                });
        return rows;
    }

    /**
     * For each unit, the quantity in its category whose base unit is 1 / the unit's base unit, or
     * "" if there is none: consumption-inverse for liter-per-100-kilometer, electric-conductance
     * for ohm. Requiring the same category keeps out pairs that only match by dimension, like
     * second and becquerel, so a unit without a category has no reciprocal.
     */
    private static Map<String, String> reciprocalQuantities(
            Map<String, String> unitCategories, Map<String, Conversion> conversions) {
        Map<List<String>, String> quantityByCategoryAndBase = new HashMap<>();
        conversions.forEach(
                (unit, c) -> {
                    String category = unitCategories.get(unit);
                    if (!category.isEmpty() && !c.baseUnit().isEmpty()) {
                        quantityByCategoryAndBase.put(
                                List.of(category, c.baseUnit()), c.quantity());
                    }
                });
        Map<String, String> reciprocals = new HashMap<>();
        conversions.forEach(
                (unit, c) -> {
                    String category = unitCategories.get(unit);
                    String quantity = null;
                    if (!category.isEmpty() && !c.baseUnit().isEmpty()) {
                        String reciprocalBase = UnitConversions.reciprocalOf(c.baseUnit());
                        if (!reciprocalBase.equals(c.baseUnit())) {
                            quantity =
                                    quantityByCategoryAndBase.get(
                                            List.of(category, reciprocalBase));
                        }
                    }
                    reciprocals.put(unit, quantity == null ? "" : quantity);
                });
        return reciprocals;
    }
}
