package org.unicode.cldr.test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.ThreadSafeMapOfMapOfMap;

/**
 * Cache example html strings for ExampleGenerator.
 *
 * <p>Essentially, the cache simply maps from xpath+value to html.
 *
 * <p>The complexity of this class is mostly for the sake of handling dependencies where the example
 * for pathB+valueB depends not only on pathB and valueB, but also on the current <em>winning</em>
 * values of pathA1, pathA2, ...
 *
 * <p>Some examples in the cache must get cleared when a changed winning value for a path makes the
 * cached examples for other paths possibly no longer correct.
 *
 * <p>For example, let pathA = "//ldml/localeDisplayNames/languages/language[@type=\"aa\"]" and
 * pathB = "//ldml/localeDisplayNames/territories/territory[@type=\"DJ\"]". The values, in locale
 * fr, might be "afar" for pathA and "Djibouti" for pathB. The example for pathB might include "afar
 * (Djibouti)", which depends on the values of both pathA and pathB.
 *
 * <p>Each ExampleGenerator object, which is for one locale, has its own ExampleCache object.
 *
 * <p>This cache is internal to each ExampleGenerator. Compare TestCache.exampleGeneratorCache,
 * which is at a higher level, caching entire ExampleGenerator objects, one for each locale.
 *
 * <p>Unlike TestCache.exampleGeneratorCache, this cache doesn't get cleared to conserve memory,
 * only to adapt to changed winning values.
 */
public class ExampleCache {

    /**
     * AVOID_CLEARING_CACHE: a performance optimization. Should be true except for testing. Only
     * remove keys for which the examples may be affected by this change.
     *
     * <p>All paths of type “A” (i.e., all that have dependencies) have keys in
     * ExampleDependencies.dependencies. For any other path given as the argument to this function,
     * there should be no need to clear the cache. When there are dependencies, only remove the keys
     * for paths that are dependent on this path.
     *
     * <p>Reference: https://unicode-org.atlassian.net/browse/CLDR-13636
     */
    private static final boolean AVOID_CLEARING_CACHE = true;

    /**
     * Avoid storing null in the cache, but do store NONE as a way to remember there is no example
     * html for the given xpath and value. This is probably faster than calling constructExampleHtml
     * again and again to get null every time, if nothing at all were stored in the cache.
     */
    private static final String NONE = "\uFFFF";

    /**
     * The nested cache mapping is: starredPath → (starlessPath → (value → html)).
     *
     * <p>PathStarrer is used for getting starredPath from an ordinary (starless) path. Inclusion of
     * starred paths enables performance improvement with AVOID_CLEARING_CACHE.
     *
     * <p>starredPath is the key for the highest level of the nested cache.
     *
     * <p>Compare starred "//ldml/localeDisplayNames/languages/language[@type=\"*\"]" with starless
     * "//ldml/localeDisplayNames/languages/language[@type=\"aa\"]". There are fewer starred paths
     * than starless paths. ExampleDependencies.dependencies has starred paths for that reason.
     */
    private final ThreadSafeMapOfMapOfMap<String, String, String, String> cache =
            new ThreadSafeMapOfMapOfMap<>();

    /**
     * A clearable cache is any object that supports being cleared when a path changes. An example
     * is the cache of person name samples.
     */
    interface ClearableCache {
        void clear();
    }

    /**
     * The nested cache mapping is: starredPath → ClearableCache. TODO: because there is no
     * concurrent multimap, use synchronization
     */
    private final Multimap<String, ClearableCache> registeredCache = HashMultimap.create();

    /**
     * Register other caches. This isn't done often, so synchronized should be ok.
     *
     * @return the clearableCache
     */
    <T extends ClearableCache> T registerCache(T clearableCache, String... starredPaths) {
        synchronized (registeredCache) {
            for (String starredPath : starredPaths) {
                registeredCache.put(starredPath, clearableCache);
            }
            return clearableCache;
        }
    }

    /**
     * For testing, caching can be disabled for some ExampleCaches while still enabled for others.
     */
    private boolean cachingIsEnabled = true;

    public void setCachingEnabled(boolean enabled) {
        cachingIsEnabled = enabled;
    }

    /**
     * Get the cached example html for this item, based on its xpath and value. If it is absent from
     * the cache, use the provided function to compute the example html and add it to the cache.
     *
     * <p>The HTML string shows example(s) using that value for that path, for the locale of the
     * ExampleGenerator we're connected to.
     *
     * @param xpath the path
     * @param value the value
     * @param f the function to compute the example if it is absent
     * @return the example html or null
     */
    public String computeIfAbsent(
            String xpath,
            String value,
            ThreadSafeMapOfMapOfMap.TriFunction<String, String, String, String> f) {
        String starredPath = PathStarrer.get(xpath);
        if (!cachingIsEnabled) {
            return f.apply(starredPath, xpath, value);
        }
        return cache.computeIfAbsent(starredPath, xpath, value, f);
    }

    /**
     * Clear the cached examples for any paths whose examples might depend on the winning value of
     * the given path, since the winning value of the given path has changed.
     *
     * <p>There is no need to update the example(s) for the given path itself, since the cache key
     * includes path+value and therefore each path+value has its own example, regardless of which
     * value is winning. There is a need to update the examples for OTHER paths whose examples
     * depend on the winning value of the given path.
     *
     * @param xpath the path whose winning value has changed
     *     <p>Called by ExampleGenerator.updateCache
     */
    void update(String xpath) {
        if (AVOID_CLEARING_CACHE) {
            String starredA = PathStarrer.get(xpath);
            for (String starredB : ExampleDependencies.dependencies.get(starredA)) {
                cache.remove(starredB, null, null);
            }
            // TODO clean up the synchronization
            synchronized (registeredCache) {
                for (ClearableCache item : registeredCache.get(starredA)) {
                    item.clear();
                }
            }
        } else {
            cache.clear();
        }
    }
}
