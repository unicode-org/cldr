package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.util.XPathParts;

final class SplitPath {
    private final String scaffold;

    public String getScaffold() {
        return scaffold;
    }

    public List<String> getAttributeValues() {
        return attributeValues;
    }

    private final List<String> attributeValues;

    public SplitPath(String scaffold, List<String> attributeValues) {
        this.scaffold = scaffold;
        this.attributeValues = List.copyOf(attributeValues);
    }

    @Override
    public String toString() {
        return scaffold + " " + attributeValues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scaffold, attributeValues);
    }

    private static final ConcurrentHashMap<String, SplitPath> STAR_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Get a version of the given path, split into a 'scaffold' (without attribute values) and a
     * list of the attribute values
     *
     * @param path the original path
     * @return the starred path
     */
    public static SplitPath from(String path) {
        return STAR_CACHE.computeIfAbsent(
                path,
                x -> {
                    List<String> attributeValues = new ArrayList<>();
                    XPathParts parts = XPathParts.getFrozenInstance(x).cloneAsThawed();
                    for (int i = 0; i < parts.size(); ++i) {
                        for (String key : parts.getAttributeKeys(i)) {
                            attributeValues.add(parts.getAttributeValue(i, key));
                            parts.setAttribute(i, key, "%A");
                        }
                    }
                    return new SplitPath(parts.toString(), attributeValues);
                });
    }
}
