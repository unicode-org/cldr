package org.unicode.cldr.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Transforms a path by replacing attributes with .*
 *
 * @author markdavis
 */
public class PathStarrer {
    public static final String PERCENT_A_PATTERN = "%A";
    public static final String SIMPLE_STAR_PATTERN = "*";

    /**
     * The default pattern to replace attribute values in paths to make starred paths. This pattern
     * is assumed never to occur literally in an ordinary non-starred path. Cached starred paths use
     * this pattern, and starred paths with other patterns may be produced by replacing all
     * occurrences of STAR_PATTERN with another pattern. In this context, the replacement is
     * literal, not using regular expressions.
     */
    private static final String STAR_PATTERN = "([^\"]*+)";

    private static final ConcurrentHashMap<String, String> STAR_CACHE = new ConcurrentHashMap<>();

    /**
     * Get a starred version of the given path, using the default STAR_PATTERN
     *
     * @param path the original path
     * @return the starred path
     */
    public static String get(String path) {
        return STAR_CACHE.computeIfAbsent(
                path,
                x -> {
                    XPathParts parts = XPathParts.getFrozenInstance(x).cloneAsThawed();
                    for (int i = 0; i < parts.size(); ++i) {
                        for (String key : parts.getAttributeKeys(i)) {
                            parts.setAttribute(i, key, STAR_PATTERN);
                        }
                    }
                    return parts.toString();
                });
    }

    /**
     * Get a starred version of the given path, using the given pattern
     *
     * @param path the original path
     * @return the starred path
     */
    public static String getWithPattern(String path, String pattern) {
        return get(path).replace(STAR_PATTERN, pattern);
    }
}
