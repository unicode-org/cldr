package org.unicode.cldr.tool.unitsinfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CategoryRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CombinedRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Status;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.UnitRow;

/**
 * When each unit and category was added and deprecated. It is built by applying releases in order,
 * starting from the existing files or from the first release tag.
 */
public final class UnitHistory {

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

    /**
     * A combined unit's first release and deprecating release ("" while CLDR data uses it). A
     * combined unit is one that CLDR data uses without display names of its own.
     */
    public record CombinedEntry(String since, String deprecatedIn) {
        public Status status(String release) {
            return Status.of(since, deprecatedIn, release);
        }
    }

    private final Map<String, UnitEntry> units = new TreeMap<>();
    private final Map<String, CategoryEntry> categories = new TreeMap<>();
    private final Map<String, CombinedEntry> combined = new TreeMap<>();

    /** The history recorded in existing files. */
    public static UnitHistory of(UnitsInfo info) {
        UnitHistory history = new UnitHistory();
        for (CategoryRow row : info.categories()) {
            history.categories.put(
                    row.category(),
                    new CategoryEntry(row.since(), row.deprecatedIn(), row.replacement()));
        }
        for (UnitRow row : info.units()) {
            history.units.put(
                    row.unit(), new UnitEntry(row.category(), row.since(), row.deprecatedIn()));
        }
        for (CombinedRow row : info.combined()) {
            history.combined.put(row.unit(), new CombinedEntry(row.since(), row.deprecatedIn()));
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
                            unit, new UnitEntry(category, old == null ? release : old.since(), ""));
                });

        Set<String> activeCategories = new HashSet<>(activeUnits.values());
        categories.replaceAll(
                (category, e) ->
                        activeCategories.contains(category) || !e.deprecatedIn().isEmpty()
                                ? e
                                : new CategoryEntry(e.since(), release, movedTo(category, before)));
        for (String category : activeCategories) {
            CategoryEntry old = categories.get(category);
            categories.put(
                    category, new CategoryEntry(old == null ? release : old.since(), "", ""));
        }
    }

    /**
     * Applies a release's combined units: the units that its data uses without display names of
     * their own, such as foot-and-inch in the unit preferences. Apply {@link #add} first. New ones
     * start in this release, missing ones are deprecated in it, and returning ones are active
     * again. A unit that has had names is not a combined unit: it keeps its row in units_info.tsv.
     */
    public void addCombined(String release, Set<String> combinedUnits) {
        combined.replaceAll(
                (unit, e) ->
                        combinedUnits.contains(unit) || !e.deprecatedIn().isEmpty()
                                ? e
                                : new CombinedEntry(e.since(), release));
        for (String unit : combinedUnits) {
            CombinedEntry old = combined.get(unit);
            combined.put(unit, new CombinedEntry(old == null ? release : old.since(), ""));
        }
        combined.keySet().removeAll(units.keySet());
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

    /** Units in file order, which is also the order that new ones get IDs: category, then ID. */
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

    public Map<String, CombinedEntry> combined() {
        return Collections.unmodifiableMap(combined);
    }
}
