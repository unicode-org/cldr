package org.unicode.cldr.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    private final static int INITIAL_CAPACITY=30;
    private final static int MAX_CAPACITY=1000;
    
    /** 
     * Variable to control whether patterns are cached (true);
     *  or whether they are created all the time */
    private final static boolean USE_CACHE=true;
    
    /**
     * Variable that controls whether statistics are recorded for the caching.
     */
    private final static boolean RECORD_STATISTICS=true;
  
    /**
     * The cache object
     */
    private final static PatternCacheInterface cache=new PatternCacheInternal(
        new PatternCacheConfigBean(INITIAL_CAPACITY, MAX_CAPACITY, USE_CACHE, RECORD_STATISTICS));
    
    /**
     * Interface governing access to the cache
     * @author ribnitz
     *
     */
    public static interface PatternCacheInterface {
        /**
         * Obtain a compiled pattern from the cache
         * @param patternStr
         * @return
         */
        Pattern get(String patternStr);
        
        /**
         * Get statistics from this cache object
         * @return
         */
        CacheStats getStatistics();
        
        /**
         * Query whether caching is enabled
         * @return
         */
        boolean isCachingEnabled();
        
        /**
         * Query whether statistics are recorded
         * @return
         */
        boolean isRecordStatistics();
        
        /**
         * Get the current size of the cache
         * @return
         */
        long size();
    }
   
    /**
     * Object to configure the behaviour of the cache;
     * @author ribnitz
     *
     */
    private static class PatternCacheConfigBean  {
        /** 
         * Initial capacity
         */
        private final int cacheInitialCapacity;
        
        /**
         * Maximum capacity
         */
        private final int cacheMaxCapacity;
        
        /**
         * Whether caching is enabled; if disabled, new Patterns will be compiled on every call
         */
        private final boolean isCaching;
        
        /**
         * Whether cache statistics are recorded
         */
        private final boolean isRecordStatistics;
        
        /**
         * Initialize the configuration with the given value
         * @param cacheInitialCapacity initial capacity
         * @param cacheMaxCapacity maximum capacity
         * @param isCaching whether caching should be enabled
         * @param isRecordStatistics whether statistics should be recorded
         * @throws IllegalArgumentException if any of the capacities is negative, or maximum capacity is less than initial capacity
         */
        public PatternCacheConfigBean(int cacheInitialCapacity, int cacheMaxCapacity, boolean isCaching, boolean isRecordStatistics) {
            if (cacheInitialCapacity<0) {
                throw new IllegalArgumentException("Unable to handle negative initial capacity "+cacheInitialCapacity);
            }
            if (cacheMaxCapacity<0) {
                throw new IllegalArgumentException("Unable to handle negative max capacity "+cacheMaxCapacity);
            }
            if (cacheMaxCapacity<cacheInitialCapacity) {
                throw new IllegalArgumentException("The maximum capacity cannot be less than the initial capacity; maximum is currently "+
                    cacheMaxCapacity+", initial capacity "+cacheInitialCapacity);
            }
            this.cacheInitialCapacity = cacheInitialCapacity;
            this.cacheMaxCapacity = cacheMaxCapacity;
            this.isCaching = cacheMaxCapacity==0?false:isCaching;
            this.isRecordStatistics = isRecordStatistics;
        }

        public int getInitialCapacity() {
            return cacheInitialCapacity;
        }

        public int getMaxCapacity() {
            return cacheMaxCapacity;
        }

        public boolean isCaching() {
            return isCaching;
        }

        public boolean isRecordStatistics() {
            return isRecordStatistics;
        }
        
    }
    /**
     * Implementation of the interface
     * @author ribnitz
     *
     */
    private static class PatternCacheInternal implements PatternCacheInterface {
        
        /**
         * The cache object
         */
        private final Cache<String,Pattern> cache;
        /**
         * The configuration
         */
        private final PatternCacheConfigBean config;
        
        public PatternCacheInternal(PatternCacheConfigBean config) {
            // internal object, no null check 
            if (config.isCaching()) {
                if (config.isRecordStatistics()) {
                    cache=CacheBuilder.newBuilder().
                        initialCapacity(config.getInitialCapacity()).
                        maximumSize(config.getMaxCapacity()). recordStats().
                        build();
                } else {
                    cache=CacheBuilder.newBuilder().
                        initialCapacity(config.getInitialCapacity()).
                        maximumSize(config.getMaxCapacity()).
                        build();
                }
            } else {
                cache=null;
            }
            this.config=config;
        }
        
        /**
         * Obtain a compiled Pattern from the String given; results of the lookup are cached, a cached result will be returned if
         * possible. 
         * @param patternStr the string to use for compilation
         * @throws IllegalArgumentException The string provided was null or empty, or there was a problem compiling the Pattern from the String
         */
        public  Pattern get(final String patternStr) {
            // Pre-conditions: non-null, non-empty string
            if (patternStr==null) {
                throw new IllegalArgumentException("Please call with non-null argument");
            }
            if (patternStr.isEmpty()) {
                throw new IllegalArgumentException("Please call with non-empty argument");
            }
            // If patterns are not cached, simply return a new compiled Pattern
            if (!config.isCaching() || cache == null) {
                try {
                    return Pattern.compile(patternStr);
                } catch (PatternSyntaxException pse) {
                    throw new IllegalArgumentException("The supplied pattern is not valid: "+patternStr,pse);
                }
            }
            Pattern result=null;
            try {
                result=cache.get(patternStr, new Callable<Pattern>() {

                    @Override
                    public Pattern call() throws Exception {
                        return Pattern.compile(patternStr);
                    }
                });
            } catch (ExecutionException e) {
                // realistically, this is a PatternSyntaxException
                throw new IllegalArgumentException("The supplied pattern is not valid: "+patternStr,e);
            }
            return result;
        }

        @Override
        public CacheStats getStatistics() {
            if (cache!=null) {
                return cache.stats();
            }
            return new CacheStats(0, 0, 0, 0, 0, 0);
        }

        @Override
        public boolean isCachingEnabled() {
           return config.isCaching();
        }

        @Override
        public boolean isRecordStatistics() {
          return config.isRecordStatistics();
        }
        
        public long size() {
            return cache.size();
        }
        
    }
    /**
     * Obtain a compiled Pattern from the String given; results of the lookup are cached, a cached result will be returned if
     * possible. 
     * @param patternStr the string to use for compilation
     * @throws IllegalArgumentException The string provided was null or empty, or there was a problem compiling the Pattern from the String
     */
    public static Pattern get(String patternStr) {
        return cache.get(patternStr);
    }
    
    /**
     * Obtain collected statistics about this cache object
     * @return
     */
    public static CacheStats getStatistics() {
        return cache.getStatistics();
    }
    
    /**
     * Obtain information whether this cache is caching the patterns it returns
     * @return
     */
    public static boolean isCachingEnabled() {
        return cache.isCachingEnabled();
    }
    
    /**
     * Obtain information about whether this cache is recording statistics
     * @return
     */
    public static boolean isRecordStatistics() {
        return cache.isRecordStatistics();
    }
    
    /**
     * Obtain a new instance configured with the parameters given
     * @param initialCapacity
     * @param maxCapacity
     * @param doCache
     * @param doRecordStatistics
     * @return
     */
    public static PatternCacheInterface newInstance(int initialCapacity, int maxCapacity, boolean doCache, boolean doRecordStatistics) {
        return new PatternCacheInternal(new PatternCacheConfigBean(initialCapacity, maxCapacity, doCache, doRecordStatistics));
    }
    
    /**
     * Get the current size of the cache
     * @return
     */
    public static long size() {
        return cache.size();
    }
   
}