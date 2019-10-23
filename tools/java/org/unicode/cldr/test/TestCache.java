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
 * Caches tests and examples
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

    /*
     * TODO: document whether CLDR_TESTCACHE_SIZE is set on production server, and if so to what, and why;
     * evaluate why the fallback 12 for CLDR_TESTCACHE_SIZE is appropriate or too small. Consider not
     * using maximumSize() at all, depending on softValues() instead to garbage collect only when needed.
     */
    private Cache<CheckCLDR.Options, TestResultBundle> testResultCache = CacheBuilder.newBuilder().maximumSize(CLDRConfig.getInstance()
        .getProperty("CLDR_TESTCACHE_SIZE", 12)).softValues().build();

    private Factory factory = null;

    private String nameMatcher = null;

    /**
     * Get the bundle for this test
     */
    public TestResultBundle getBundle(CheckCLDR.Options options) {
        TestResultBundle b = testResultCache.getIfPresent(options);
        if (DEBUG) {
             if (b != null) {
                 System.err.println("Bundle refvalid: " + options + " -> " + (b != null));
             }
             System.err.println("Bundle " + b + " for " + options + " in " + this.toString());
         }
         if (b == null) {
             // ElapsedTimer et = new ElapsedTimer("New test bundle " + locale + " opt " + options);
             b = new TestResultBundle(options);
             // System.err.println(et.toString());
             testResultCache.put(options, b);
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
        stats.append("{" + this.getClass().getSimpleName() + super.toString() + " Size: " + testResultCache.size() + " (");
        int good = 0;
        int total = 0;
        for (Entry<Options, TestResultBundle> k : testResultCache.asMap().entrySet()) {
            Options key = k.getKey();
            TestResultBundle bundle = k.getValue();
            if (bundle != null) {
                good++;
            }
            if (DEBUG) {
                stats.append("," + k.getKey() + "=" + key);
            }
            total++;
        }
        stats.append(" " + good + "/" + total + "}");
        return stats.toString();
    }

    /**
     * Update the caches as needed, given that the value has changed for this xpath and source.
     *
     * @param xpath the xpath
     * @param source the XMLSource
     */
    @Override
    public void valueChanged(String xpath, XMLSource source) {
        CLDRLocale locale = CLDRLocale.getInstance(source.getLocaleID());
        valueChangedInvalidateRecursively(xpath, locale);
    }

    /**
     * Update the caches as needed, given that the value has changed for this xpath and locale.
     * 
     * Called by valueChanged(String xpath, XMLSource source),
     * and also calls itself recursively for sublocales
     *
     * @param xpath the xpath
     * @param locale the CLDRLocale
     *
     * There is room for improvement of performance. Currently we invalidate all paths for any affected locale.
     * Ideally, cache invalidation should only be done for each locale+path whose test results or examples potentially
     * depend on the given locale+path, due to inheritance, aliasing, logical groups, etc. That could be done using a mapping
     * between paths that are related by such dependencies; that mapping would be constant for as long as the paths and tests
     * (or example-generation methods) don't change.
     */
    private void valueChangedInvalidateRecursively(String xpath, final CLDRLocale locale) {
        if (DEBUG) {
            System.err.println("BundDelLoc " + locale + " @ " + xpath);
        }
        /*
         * Call self recursively for all sub-locales
         */
        for (CLDRLocale sub : ((SublocaleProvider) getFactory()).subLocalesOf(locale)) {
            valueChangedInvalidateRecursively(xpath, sub);
        }
        /*
         * Update caching for TestResultBundle
         */
        updateTestResultCache(xpath, locale);
        /*
         * Update caching for ExampleGenerator
         */
        updateExampleGeneratorCache(xpath, locale);
    }

    /**
     * Update the cache of TestResultBundle objects, per valueChanged
     *
     * @param xpath the xpath whose value has changed
     * @param locale the CLDRLocale
     *
     * Called by valueChangedInvalidateRecursively
     */
    private void updateTestResultCache(@SuppressWarnings("unused") String xpath, CLDRLocale locale) {
        if (!testResultCache.asMap().isEmpty()) {
            // Filter the testResultCache to only remove the items where the locale matches
            List<Options> toRemove = new ArrayList<>();
            for (Options k : testResultCache.asMap().keySet()) {
                if (k.getLocale().equals(locale)) {
                    toRemove.add(k);
                }
            }
            if (!DEBUG) {
                // no logging is done, simply invalidate all items
                testResultCache.invalidateAll(toRemove);
            } else {
                // avoid concurrent remove
                for (CheckCLDR.Options k : toRemove) {
                    testResultCache.invalidate(k);
                    System.err.println("BundDel " + k);
                }
            }
        }
    }

    /**
     * Per-locale testResultCache of ExampleGenerator objects
     *
     * Re-use the TestCache implementation of XMLSource.Listener for ExampleGenerator
     * objects in addition to TestResultBundle objects. The actual caches are distinct,
     * only the Listener interface is shared.
     *
     * ExampleGenerator objects are for generating examples, rather than for
     * checking validity, unlike other TestCache-related objects such as TestResultBundle.
     * Still, ExampleGenerator has similar dependence on locales, paths, and values, and
     * needs similar treatment for caching and performance. ExampleGenerator is in the
     * same package ("test") as TestCache.
     *
     * There are currently unused (?) files Registerable.java and LocaleChangeRegistry.java
     * that appear to have been intended for a similar purpose. They are in the web package.
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-12020
     */
    private static Cache<String, ExampleGenerator> exampleGeneratorCache = CacheBuilder.newBuilder().softValues().build();

    /**
     * Get an ExampleGenerator for the given locale, etc.
     *
     * Use a cache for performance.
     *
     * @param locale the CLDRLocale
     * @param ourSrc the CLDRFile for the locale
     * @param translationHintsFile the CLDRFile for translation hints (English)
     * @param englishPath (a.k.a. supplementalDataDirectory)
     * @return the ExampleGenerator
     *
     * Called by DataSection.make for use in SurveyTool.
     *
     * Note: other objects also have functions named "getExampleGenerator":
     * org.unicode.cldr.unittest.TestExampleGenerator.getExampleGenerator(String)
     * org.unicode.cldr.test.ConsoleCheckCLDR.getExampleGenerator()
     */
    public static ExampleGenerator getExampleGenerator(CLDRLocale locale, CLDRFile ourSrc, CLDRFile translationHintsFile, String englishPath) {
        boolean egCacheIsEnabled = true;
        if (!egCacheIsEnabled) {
            return new ExampleGenerator(ourSrc, translationHintsFile, englishPath);
        }
        /*
         * TODO: consider get(locString, Callable) instead of getIfPresent and put.
         */
        String locString = locale.toString();
        ExampleGenerator eg = exampleGeneratorCache.getIfPresent(locString);
        if (eg == null) {
            synchronized(exampleGeneratorCache) {
                eg = exampleGeneratorCache.getIfPresent(locString);
                if (eg == null) {
                    eg = new ExampleGenerator(ourSrc, translationHintsFile, englishPath);
                    exampleGeneratorCache.put(locString, eg);
                }
            }
        }
        return eg;
    }

    /**
     * Update the cached ExampleGenerator, per valueChanged
     *
     * @param xpath the xpath whose value has changed
     * @param locale the CLDRLocale determining which ExampleGenerator to update
     *
     * Called by valueChangedInvalidateRecursively
     */
    private static void updateExampleGeneratorCache(String xpath, CLDRLocale locale) {
        ExampleGenerator eg = exampleGeneratorCache.getIfPresent(locale.toString());
        if (eg != null) {
            /*
             * Each ExampleGenerator has its own internal cache, which is not the same
             * as exampleGeneratorCache.
             *
             * We could call exampleGeneratorCache.invalidate(locale.toString()) but that would be
             * too drastic, effectively throwing away the ExampleGenerator for the entire locale.
             * Ideally eg.updateCache will only clear the minimum set of examples (in its internal
             * cache) required due to dependence on the given xpath.
             */
            eg.updateCache(xpath);
        }
    }
}
