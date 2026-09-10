package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.util.XPathParts;

final class SplitPath {
    private final String chassis;
    private final List<String> attributeValues;

    public String getChassis() {
        return chassis;
    }

    public List<String> getAttributeValues() {
        return attributeValues;
    }

    public SplitPath(String chassis, List<String> attributeValues) {
        this.chassis = chassis;
        this.attributeValues = List.copyOf(attributeValues);
    }

    @Override
    public String toString() {
        return chassis + " " + attributeValues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chassis, attributeValues);
    }

    private static final ConcurrentHashMap<String, SplitPath> STAR_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Get a version of the given path, split into a 'chassis' (without attribute values) and a list
     * of the attribute values
     *
     * @param path the original path
     * @return the starred path
     */
    public static SplitPath from(String path) {
        return STAR_CACHE.computeIfAbsent(
                path,
                x -> {
                    StringBuilder chassis = new StringBuilder("/");
                    List<String> attributeValues = new ArrayList<>();
                    XPathParts parts = XPathParts.getFrozenInstance(x);
                    for (int i = 0; i < parts.size(); ++i) {
                        String element = parts.getElement(i);
                        chassis.append('/').append(element);
                        for (String key : parts.getAttributeKeys(i)) {
                            attributeValues.add(parts.getAttributeValue(i, key));
                            chassis.append("[@").append(key);
                            chassis.append(']');
                        }
                    }
                    return new SplitPath(chassis.toString(), attributeValues);
                });
    }

    // find the element in the chassis just before the attributeNumber
    public static String findElementForAttribute(String chassis, int attributeNumber) {
        int nth = findNthOccurrence(chassis, '@', attributeNumber + 1);
        int start = chassis.lastIndexOf('/', nth) + 1;
        int end = chassis.indexOf('[', start);
        return chassis.substring(start, end);
    }

    public static int findNthOccurrence(String str, char c, int n) {
        if (str == null || n <= 0) {
            return -1;
        }

        int index = -1;
        while (n > 0) {
            index = str.indexOf(c, index + 1);
            if (index == -1) {
                return -1;
            }
            n--;
        }
        return index;
    }
}
