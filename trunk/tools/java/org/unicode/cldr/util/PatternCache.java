package org.unicode.cldr.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

/**
 * Simple class for caching Patterns, possibly avoiding the cost of
 * compilation if they are in the cache.
 *
 *
 * @author ribnitz
 *
 */
public class PatternCache {
    private final static int INITIAL_CAPACITY = 30;
    private final static int MAX_CAPACITY = 1000;

    /**
     * Variable to control whether patterns are cached (true);
     *  or whether they are created all the time */
    private final static boolean USE_CACHE = true;

    /**
     * Variable that controls whether statistics are recorded for the caching.
     */
    private final static boolean RECORD_STATISTICS = false;

    /**
     * The cache object
     */
    private final static Cache<String, Pattern> cache;

    /*
     * A static initialization block is used to be able to cleanly handle the three different cases:
     *
     * 1) no caching
     * 2) caching without statistics collection
     * 3) caching with statistics collection
     */
    static {
        if (USE_CACHE) {
            if (RECORD_STATISTICS) {
                cache = CacheBuilder.newBuilder().initialCapacity(INITIAL_CAPACITY).maximumSize(MAX_CAPACITY).recordStats().build();
            } else {
                cache = CacheBuilder.newBuilder().initialCapacity(INITIAL_CAPACITY).maximumSize(MAX_CAPACITY).build();
            }
        } else {
            cache = null;
        }
    }

    /**
     * Obtain a compiled Pattern from the String given; results of the lookup are cached, a cached result will be returned if
     * possible.
     * @param patternStr the string to use for compilation
     * @throws IllegalArgumentException The string provided was null or empty, or there was a problem compiling the Pattern from the String
     */
    public static Pattern get(final String patternStr) {
        // Pre-conditions: non-null, non-empty string
        if (patternStr == null) {
            throw new IllegalArgumentException("Please call with non-null argument");
        }
        if (patternStr.isEmpty()) {
            throw new IllegalArgumentException("Please call with non-empty argument");
        }
        // If patterns are not cached, simply return a new compiled Pattern
        if (!USE_CACHE) {
            return Pattern.compile(patternStr);
        }
        Pattern result = null;
        try {
            result = cache.get(patternStr, new Callable<Pattern>() {

                @Override
                public Pattern call() throws Exception {
                    return Pattern.compile(patternStr);
                }
            });
        } catch (ExecutionException e) {
            // realistically, this is a PatternSyntaxException
            throw new IllegalArgumentException("The supplied pattern is not valid: " + patternStr, e);
        }
        return result;
    }

    /**
     * Return true if the collection of statistics is enabled
     * @return
     */
    public static boolean isRecordStatistics() {
        return RECORD_STATISTICS;
    }

    /**
     * Return true if caching is enabled; in the case it isn't this class acts like a Factory
     * for compiled Patterns
     *
     * @return
     */
    public static boolean isCachingEnabled() {
        return USE_CACHE;
    }

    /**
     * Get Statistics for the Caching operation
     * @return
     */
    public static CacheStats getStatistics() {
        return cache.stats();
    }

}