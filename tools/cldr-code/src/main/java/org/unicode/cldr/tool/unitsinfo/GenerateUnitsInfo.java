package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CategoryRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Status;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.UnitRow;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

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
        history.add(ToolConstants.DEV_VERSION, CldrUnits.current());
        return build(history, previous);
    }

    /**
     * Rebuilds the history from the release tags in {@code gitDir} plus this checkout, keeping the
     * IDs of {@code previous}. Fails if a release tag is missing.
     */
    public static UnitsInfo rebuild(String gitDir, UnitsInfo previous) {
        UnitHistory history = new UnitHistory();
        for (int release : CldrUnits.historyReleases()) {
            history.add(Integer.toString(release), CldrUnits.atRelease(gitDir, release));
        }
        history.add(ToolConstants.DEV_VERSION, CldrUnits.current());
        return build(history, previous);
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

        List<String> unitOrder = history.unitOrder();
        Map<String, Integer> unitIds = StableIds.assign(previous.unitIds(), unitOrder);
        Map<String, String> replacements = new HashMap<>();
        previous.units().forEach(row -> replacements.put(row.unit(), row.replacement()));
        replacements.putAll(CldrUnits.aliases());
        List<UnitRow> units = new ArrayList<>();
        for (String unit : unitOrder) {
            UnitHistory.UnitEntry entry = history.units().get(unit);
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
                            UnitConversions.of(convertible),
                            status,
                            entry.since(),
                            entry.deprecatedIn(),
                            replacement));
        }
        UnitsInfo result = new UnitsInfo(categories, units);
        StableIds.verify(previous, result);
        return result;
    }
}
