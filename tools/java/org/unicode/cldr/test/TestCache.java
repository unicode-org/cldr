package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.SublocaleProvider;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

/**
 * Caches tests
 * Call XMLSource.addListener() on the instance to notify it of changes to the XMLSource.
 *
 * @author srl
 * @see XMLSource#addListener(org.unicode.cldr.util.XMLSource.Listener)
 */
public class TestCache implements XMLSource.Listener {
    public class TestResultBundle {
        final private CheckCLDR cc = CheckCLDR.getCheckAll(getFactory(), nameMatcher);
        final CLDRFile file;
        final private CheckCLDR.Options options;
        final private ConcurrentHashMap<Pair<String, String>, List<CheckStatus>> pathCache;
        final protected List<CheckStatus> possibleProblems = new ArrayList<CheckStatus>();
        
        protected TestResultBundle(CheckCLDR.Options cldrOptions) {
            options = cldrOptions;
            pathCache = new ConcurrentHashMap<Pair<String, String>, List<CheckStatus>>();
            file = getFactory().make(options.getLocale().getBaseName(), true);
            cc.setCldrFileToCheck(file, options, possibleProblems);
        }

        /**
         * Check the given value for the given path, using this TestResultBundle for
         * options, pathCache and cc (CheckCLDR).
         * 
         * @param path the path
         * @param result the list to which CheckStatus objects may be added; this function
         *               clears any objects that might already be in it
         * @param value the value to be checked
         */
         public void check(String path, List<CheckStatus> result, String value) {
             /*
              * result.clear() is needed to avoid phantom warnings in the Info Panel, if we're called
              * with non-empty result (leftover from another row) and we get cachedResult != null.
              * cc.check() also calls result.clear() (at least as of 2018-11-20) so in that case it's
              * currently redundant here. Clear it here unconditionally to be sure.
              */
             result.clear();
             Pair<String, String> key = new Pair<String, String>(path, value);
             List<CheckStatus> cachedResult = pathCache.get(key);
             if (cachedResult != null) {
                 result.addAll(cachedResult);
             }
             else {
                 cc.check(path, file.getFullXPath(path), value, options, result);
                 pathCache.put(key, ImmutableList.copyOf(result));
             }
         }

         public void getExamples(String path, String value, List<CheckStatus> result) {
             cc.getExamples(path, file.getFullXPath(path), value, options, result);
         }

         public List<CheckStatus> getPossibleProblems() {
             return possibleProblems;
         }
    }

    private static final boolean DEBUG = false;

    private Cache<CheckCLDR.Options, TestResultBundle> cache = CacheBuilder.newBuilder().maximumSize(CLDRConfig.getInstance()
        .getProperty("CLDR_TESTCACHE_SIZE", 12)).softValues().build();

    private Factory factory = null;

    private String nameMatcher = null;

    /**
     * Get the bundle for this test
     */
    public TestResultBundle getBundle(CheckCLDR.Options options) {
        TestResultBundle b = cache.getIfPresent(options);
        if (DEBUG && b != null) System.err.println("Bundle refvalid: " + options + " -> " + (b != null));
        if (DEBUG) System.err.println("Bundle " + b + " for " + options + " in " + this.toString());
        if (b == null) {
            // ElapsedTimer et = new ElapsedTimer("New test bundle " + locale + " opt " + options);
            b = new TestResultBundle(options);
            // System.err.println(et.toString());
            cache.put(options, b);
        }
        return b;
    }

    protected Factory getFactory() {
        return factory;
    }

    /**
     * Set up the basic info needed for tests
     *
     * @param factory
     * @param nameMatcher
     * @param displayInformation
     */
    public void setFactory(Factory factory, String nameMatcher) {
        if (this.factory != null) {
            throw new InternalError("setFactory() can only be called once.");
        }
        this.factory = factory;
        this.nameMatcher = nameMatcher;
    }

    /**
     * Convert this TestCache to a string
     * 
     * Used only for debugging?
     */
    @Override
    public String toString() {
        StringBuilder stats = new StringBuilder();
        stats.append("{" + this.getClass().getSimpleName() + super.toString() + " Size: " + cache.size() + " (");
        int good = 0;
        int total = 0;
        for (Entry<Options, TestResultBundle> k : cache.asMap().entrySet()) {
            Options key = k.getKey();
            TestResultBundle bundle = k.getValue();
            if (bundle != null) good++;
            if (DEBUG && true) stats.append("," + k.getKey() + "=" + key);
            total++;
        }
        stats.append(" " + good + "/" + total + "}");
        return stats.toString();
    }

    /**
     * Update the cache as needed, given that the value has changed for this xpath and locale.
     * 
     * Called by void org.unicode.cldr.test.TestCache.valueChanged(String xpath, XMLSource source),
     * and also calls itself recursively for sublocales
     *
     * @param xpath
     * @param locale
     */
    private void valueChanged(String xpath, final CLDRLocale locale) {
        if (DEBUG) System.err.println("BundDelLoc " + locale + " @ " + xpath);
        for (CLDRLocale sub : ((SublocaleProvider) getFactory()).subLocalesOf(locale)) {
            valueChanged(xpath, sub);
        }
        if (cache.asMap().isEmpty()) {
            return;
        }
        // Filter the cache to only remove the items where the locale matches
        List<Options> toRemove = new ArrayList<>();
        for (Options k : cache.asMap().keySet()) {
            if (k.getLocale().equals(locale)) {
                toRemove.add(k);
            }
        }
        if (!DEBUG) {
            // no logging is done, simply invalidate all items
            cache.invalidateAll(toRemove);
        } else {
            // avoid concurrent remove
            for (CheckCLDR.Options k : toRemove) {
                cache.invalidate(k);
                System.err.println("BundDel " + k);
            }
        }
    }

    /**
     * Update the cache as needed, given that the value has changed for this xpath and source.
     *
     * @param xpath the xpath
     * @param source the XMLSource
     */
    @Override
    public void valueChanged(String xpath, XMLSource source) {
        CLDRLocale locale = CLDRLocale.getInstance(source.getLocaleID());
        valueChanged(xpath, locale);
    }
}
