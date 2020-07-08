package org.unicode.cldr.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.PathStarrer;

/**
 * Cache example html strings for ExampleGenerator.
 *
 * Essentially, the cache simply maps from xpath+value to html.
 *
 * The complexity of this class is mostly for the sake of handling dependencies where the
 * example for pathB+valueB depends not only on pathB and valueB, but also on the current
 * <em>winning</em> values of pathA1, pathA2, ...
 *
 * Some examples in the cache must get cleared when a changed winning value for a path makes
 * the cached examples for other paths possibly no longer correct.

 * For example, let pathA = "//ldml/localeDisplayNames/languages/language[@type=\"aa\"]"
 * and pathB = "//ldml/localeDisplayNames/territories/territory[@type=\"DJ\"]". The values,
 * in locale fr, might be "afar" for pathA and "Djibouti" for pathB. The example for pathB
 * might include "afar (Djibouti)", which depends on the values of both pathA and pathB.
 *
 * Each ExampleGenerator object, which is for one locale, has its own ExampleCache object.
 *
 * This cache is internal to each ExampleGenerator. Compare TestCache.exampleGeneratorCache,
 * which is at a higher level, caching entire ExampleGenerator objects, one for each locale.
 *
 * Unlike TestCache.exampleGeneratorCache, this cache doesn't get cleared to conserve memory,
 * only to adapt to changed winning values.
 */
class ExampleCache {
    /**
     * An ExampleCacheItem is a temporary container for the info
     * needed to get and/or put one item in the cache.
     */
    class ExampleCacheItem {
        private String xpath;
        private String value;

        /**
         * starredPath, the "starred" version of xpath, is the key for the highest level
         * of the cache, which is nested.
         *
         * Compare starred "//ldml/localeDisplayNames/languages/language[@type=\"*\"]"
         * with starless "//ldml/localeDisplayNames/languages/language[@type=\"aa\"]".
         * There are fewer starred paths than starless paths.
         * ExampleDependencies.dependencies has starred paths for that reason.
         */
        private String starredPath = null;

        /**
         * The cache maps each starredPath to a pathMap, which in turn maps each starless path
         * to a valueMap.
         */
        private Map<String, Map<String, String>> pathMap = null;

        /**
         * Finally the valueMap maps the value to the example html.
         */
        private Map<String, String> valueMap = null;

        ExampleCacheItem(String xpath, String value) {
            this.xpath = xpath;
            this.value = value;
        }

        /**
         * Get the cached example html for this item, based on its xpath and value
         *
         * The HTML string shows example(s) using that value for that path, for the locale
         * of the ExampleGenerator we're connected to.
         *
         * @return the example html or null
         */
        String getExample() {
            if (!cachingIsEnabled) {
                return null;
            }
            String result = null;
            starredPath = pathStarrer.set(xpath);
            pathMap = cache.get(starredPath);
            if (pathMap != null) {
                valueMap = pathMap.get(xpath);
                if (valueMap != null) {
                    result = valueMap.get(value);
                }
            }
            if (cacheOnly && result == NONE) {
                throw new InternalError("getExampleHtml cacheOnly not found: " + xpath + ", " + value);
            }
            return (result == NONE) ? null : result;
        }

        void putExample(String result) {
            if (cachingIsEnabled) {
                if (pathMap == null) {
                    pathMap = new ConcurrentHashMap<>();
                    cache.put(starredPath, pathMap);
                }
                if (valueMap == null) {
                    valueMap = new ConcurrentHashMap<>();
                    pathMap.put(xpath, valueMap);
                }
                valueMap.put(value, (result == null) ? NONE : result);
            }
        }
    }

    /**
     * AVOID_CLEARING_CACHE: a performance optimization. Should be true except for testing.
     * Only remove keys for which the examples may be affected by this change.
     *
     * All paths of type “A” (i.e., all that have dependencies) have keys in ExampleDependencies.dependencies.
     * For any other path given as the argument to this function, there should be no need to clear the cache.
     * When there are dependencies, only remove the keys for paths that are dependent on this path.
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-13636
     */
    private static final boolean AVOID_CLEARING_CACHE = true;

    /**
     * Avoid storing null in the cache, but do store NONE as a way to remember
     * there is no example html for the given xpath and value. This is probably
     * faster than calling constructExampleHtml again and again to get null every
     * time, if nothing at all were stored in the cache.
     */
    private static final String NONE = "\uFFFF";

    /**
     * The nested cache mapping is: starredPath → (starlessPath → (value → html)).
     */
    private final Map<String, Map<String, Map<String, String>>> cache = new ConcurrentHashMap<>();

    /**
     * The PathStarrer is for getting starredPath from an ordinary (starless) path.
     * Inclusion of starred paths enables performance improvement with AVOID_CLEARING_CACHE.
     */
    private final PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");

    /**
     * For testing, caching can be disabled for some ExampleCaches while still
     * enabled for others.
     */
    private boolean cachingIsEnabled = true;

    void setCachingEnabled(boolean enabled) {
        cachingIsEnabled = enabled;
    }

    /**
     * For testing, we can switch some ExampleCaches into a special "cache only"
     * mode, where they will throw an exception if queried for a path+value that isn't
     * already in the cache. See TestExampleGeneratorDependencies.
     */
    private boolean cacheOnly = false;

    void setCacheOnly(boolean only) {
        this.cacheOnly = only;
    }

    /**
     * Clear the cached examples for any paths whose examples might depend on the
     * winning value of the given path, since the winning value of the given path has changed.
     *
     * There is no need to update the example(s) for the given path itself, since
     * the cache key includes path+value and therefore each path+value has its own
     * example, regardless of which value is winning. There is a need to update
     * the examples for OTHER paths whose examples depend on the winning value
     * of the given path.
     *
     * @param xpath the path whose winning value has changed
     *
     * Called by ExampleGenerator.updateCache
     */
    void update(String xpath) {
        if (AVOID_CLEARING_CACHE) {
            String starredA = pathStarrer.set(xpath);
            for (String starredB : ExampleDependencies.dependencies.get(starredA)) {
                cache.remove(starredB);
            }
        } else {
            cache.clear();
        }
    }
}
