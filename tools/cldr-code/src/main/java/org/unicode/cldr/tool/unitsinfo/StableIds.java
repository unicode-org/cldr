package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps unit and category IDs stable: an ID never changes or gets reused, and a new unit or
 * category gets the next free ID.
 */
public final class StableIds {

    private StableIds() {}

    /**
     * Keeps the IDs in {@code previous}, and gives the other names, in order, the IDs after the
     * largest previous one (starting at 0).
     */
    public static Map<String, Integer> assign(Map<String, Integer> previous, List<String> names) {
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
    public static void verify(UnitsInfo previous, UnitsInfo current) {
        verify("category", previous.categoryIds(), current.categoryIds());
        verify("unit", previous.unitIds(), current.unitIds());
    }

    /**
     * Throws if a previous name lost or changed its ID, if two names share an ID, or if a new
     * name's ID isn't above every previous one (so IDs are never reused).
     */
    public static void verify(
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
}
