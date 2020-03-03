package org.unicode.cldr.unittest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;

public class TestExampleDependencies extends TestFmwk {
    CLDRConfig info = CLDRConfig.getInstance();

    static boolean testDependencies = false; // make true to test

    public static void main(String[] args) {
        testDependencies = true;
        new TestExampleDependencies().run(args);
        testDependencies = false;
    }

    /**
     * Test dependencies where changing the value of one path changes example-generation for another path.
     *
     * The goal is to optimize example caching by only regenerating examples when necessary.
     *
     * Still under construction. Reference: https://unicode-org.atlassian.net/browse/CLDR-13331
     *
     * @throws IOException
     */
    public void TestExampleGeneratorDependencies() throws IOException {
        if (!testDependencies) {
            return;
        }
        final boolean JUST_LIST_PATHS = false;
        final boolean USE_STARRED_PATHS = true;

        /*
         * Different localeId gives different dependencies.
         * So far, have tested with these locales:
         *   "fr": 650 "type A"
         *   "de": 652 "type A"
         *   "am": 618 "type A"
         *   "zh": 12521 "type A"!
         *   "ar": ?
         */
        final String localeId = "sr_Cyrl_BA";

        CLDRFile englishFile = info.getEnglish();

        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        CLDRFile cldrFile = makeMutableResolved(factory, localeId); // time-consuming
        cldrFile.disableCaching();
        CLDRFile top = cldrFile.getUnresolved(); // can mutate top

        Set<String> paths = new TreeSet<String>(cldrFile.getComparator());
        CollectionUtilities.addAll(cldrFile.iterator(), paths); // time-consuming
        if (JUST_LIST_PATHS) {
            String dir = CLDRPaths.GEN_DIRECTORY + "test/";
            String name = "allpaths_" + localeId + ".txt";
            PrintWriter writer = FileUtilities.openUTF8Writer(dir, name);
            ArrayList<String> list = new ArrayList<String>(paths);
            Collections.sort(list);
            for (String path : list) {
                // writer.println(path);
                writer.println(path.replaceAll("\"", "\\\\\""));
            }
            return;
        }
        final PathStarrer pathStarrer = USE_STARRED_PATHS ? new PathStarrer().setSubstitutionPattern("*") : null;

        ExampleGenerator egBase = new ExampleGenerator(cldrFile, englishFile, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        HashMap<String, String> originalValues = new HashMap<String, String>();

        /*
         * Get all the examples so they'll be added to the cache for egBase.
         */
        for (String path : paths) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            String value = cldrFile.getStringValue(path);
            if (value == null) {
                continue;
            }
            originalValues.put(path, value);
            if (false && path.equals("//ldml/numbers/currencies/currency[@type=\"EUR\"]/symbol")) {
                System.out.println("Got " + path + " in first loop ...");
            }
            egBase.getExampleHtml(path, value);
        }
        /*
         * Make egBase "cacheOnly" so that getExampleHtml will throw an exception if future queries
         * are not found in the cache. Alternatively, could just make a local hashmap originalExamples,
         * similar to originalValues. That might be more robust, require more memory, faster or slower?
         * Should try both ways.
         */
        egBase.makeCacheOnly();

        ExampleGenerator egTest = new ExampleGenerator(cldrFile, englishFile, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        egTest.disableCaching(); // will not employ a cache -- this should save some time, since cache would be wasted

        /*
         * For each path (A), temporarily change its value, and then check each other path (B),
         * to see whether changing the value for A changed the example for B.
         */
        HashMap<String, HashSet<String>> dependenciesA = new HashMap<String, HashSet<String>>();
        // HashMap<String, HashSet<String>> dependenciesB = new HashMap<String, HashSet<String>>();
        long count = 0;
        long skipCount = 0;
        long dependencyCount = 0;

        for (String pathA : paths) {
            if (skipPathForDependencies(pathA, true)) {
                ++skipCount;
                continue;
            }
            String valueA = cldrFile.getStringValue(pathA);
            if (valueA == null) {
                continue;
            }
            if ((++count % 100) == 0) {
                System.out.println(count);
            }
            if (count > 500000) {
                break;
            }
            /*
             * Modify the value for pathA in some random way
             */
            String newValue = modifyValueRandomly(valueA);
            /*
             * cldrFile.add would lead to UnsupportedOperationException("Resolved CLDRFiles are read-only");
             * Instead do top.add(), which works since top.dataSource = cldrFile.dataSource.currentSource.
             * First, need to do valueChanged to clear getSourceLocaleIDCache.
             */
            cldrFile.valueChanged(pathA);
            top.add(pathA, newValue);

            /*
             * Reality check, did we really change the value returned by cldrFile.getStringValue?
             */
            String valueAX = cldrFile.getStringValue(pathA);
            if (valueAX.equals(newValue)) {
                // Good, expected
                // System.out.println("Changing top changed cldrFile: newValue = " + newValue
                //    + "; valueAX = " + valueAX + "; valueA = " + valueA);
            } else {
                // Bad, didn't work as expected
                System.out.println("Changing top did not change cldrFile: newValue = " + newValue
                    + "; valueAX = " + valueAX + "; valueA = " + valueA);
            }
            String starredA = USE_STARRED_PATHS ? pathStarrer.set(pathA) : null;
            HashSet<String> a = USE_STARRED_PATHS ? dependenciesA.get(starredA) : null;
            boolean maybeTypeA = ExampleGenerator.pathMightBeTypeA(pathA);

            for (String pathB : paths) {
                if (pathA.equals(pathB) || skipPathForDependencies(pathB, false)) {
                    continue;
                }
                /*
                 * For valueB, use originalValues.get(pathB), not cldrFile.getStringValue(pathB).
                 * They could be different if changing valueA changes valueB (probably due to aliasing).
                 * In that case, we're not interested in whether changing valueA changes valueB. We need
                 * to know whether changing valueA changes an example that was already cached, keyed by
                 * pathB and the original valueB.
                 */
                String valueB = originalValues.get(pathB);
                // String valueB = cldrFile.getStringValue(pathB);
                if (valueB == null) {
                    continue;
                }
                if (false && pathA.equals("//ldml/localeDisplayNames/languages/language[@type=\"aa\"]")
                    && pathB.equals("//ldml/numbers/currencies/currency[@type=\"EUR\"]/symbol")) {
                    System.out.println("Got our paths in inner loop...");
                }
                pathB = pathB.intern();

                // egTest.icuServiceBuilder.setCldrFile(cldrFile); // clear caches in icuServiceBuilder; has to be public
                String exBase = egBase.getExampleHtml(pathB, valueB); // this will come from cache (or throw cacheOnly exception)
                String exTest = egTest.getExampleHtml(pathB, valueB); // this won't come from cache
                if ((exTest == null) != (exBase == null)) {
                    throw new InternalError("One null but not both? " + pathA + " --- " + pathB);
                } else if (exTest != null && !exTest.equals(exBase)) {
                    if (!maybeTypeA) {
                        System.out.println("Warning: !maybeTypeA: " + pathA);
                    }
                    if (a == null) {
                        a = new HashSet<String>();
                    }
                    a.add(USE_STARRED_PATHS ? pathStarrer.set(pathB).intern() : pathB);

                    /***
                    HashSet<String> b = dependenciesB.get(pathB);
                    if (b == null) {
                        b = new HashSet<String>();
                    }
                    b.add(pathA);
                    dependenciesB.put(pathB, b);
                    ***/

                    ++dependencyCount;
                }
            }
            if (a != null && !a.isEmpty()) {
                dependenciesA.put(USE_STARRED_PATHS ? starredA : pathA, a);
            }
            /*
             * Restore the original value, so that the changes due to this pathA don't get
             * carried over to the next pathA. Again call valueChanged to clear getSourceLocaleIDCache.
             */
            top.add(pathA, valueA);
            cldrFile.valueChanged(pathA);
            String valueAXX = cldrFile.getStringValue(pathA);
            if (!valueAXX.equals(valueA)) {
                System.out.println("Failed to restore original value: valueAXX = " + valueAXX
                    + "; valueA = " + valueA);
            }
        }
        final boolean countOnly = false;
        writeDependenciesToFile(dependenciesA, "example_dependencies_A_" + localeId
                + (USE_STARRED_PATHS ? "_star" : ""), countOnly);
        // writeDependenciesToFile(dependenciesB, "example_dependencies_B_" + localeId, countOnly);
        System.out.println("count = " + count + "; skipCount = " + skipCount + "; dependencyCount = " + dependencyCount);
    }

    /**
     * Modify the given value string for testing dependencies
     *
     * @param value
     * @return the modified value, guaranteed to be different from value
     *
     * TODO: avoid IllegalArgumentException thrown/caught in, e.g., ICUServiceBuilder.getSymbolString;
     * this function might need path as parameter, to generate only "legal" values for specific paths.
     */
    private String modifyValueRandomly(String value) {
        /*
         * Change 1 to 0
         */
        String newValue = value.replace("1", "0");
        if (!newValue.equals(value)) {
            return newValue;
        }
        /*
         * Change 0 to 1
         */
        newValue = value.replace("0", "1");
        if (!newValue.equals(value)) {
            return newValue;
        }
        /*
         * String concatenation, e.g., change "foo" to "foo1"
         */
        return value + "1";
        // return "1".equals(value) ? "2" : "1";
    }

    /**
     * Get a CLDRFile that is mutable yet shares the same dataSource as a pre-existing
     * resolving CLDRFile for the same locale.
     *
     * If cldrFile is the pre-existing resolving CLDRFile, and we return topCldrFile, then
     * we'll end up with topCldrFile.dataSource = cldrFile.dataSource.currentSource, which
     * will be a SimpleXMLSource.
     *
     * @param factory
     * @param localeID
     * @return the CLDRFile
     */
    private static CLDRFile makeMutableResolved(Factory factory, String localeID) {
        XMLSource topSource = factory.makeSource(localeID).cloneAsThawed(); // make top one modifiable
        List<XMLSource> parents = getParentSources(factory, localeID);
        XMLSource[] a = new XMLSource[parents.size()];
        return new CLDRFile(topSource, parents.toArray(a));
    }

    /**
     * Get the parent sources for the given localeId
     *
     * @param factory
     * @param localeID
     * @return the List of XMLSource objects
     *
     * Called only by makeMutableResolved
     */
    private static List<XMLSource> getParentSources(Factory factory, String localeID) {
        List<XMLSource> parents = new ArrayList<>();
        for (String currentLocaleID = LocaleIDParser.getParent(localeID);
            currentLocaleID != null;
            currentLocaleID = LocaleIDParser.getParent(currentLocaleID)) {
            parents.add(factory.makeSource(currentLocaleID));
        }
        return parents;
    }

    /**
     * Should the given path be skipped when testing example-generator path dependencies?
     *
     * @param path
     * @param isTypeA true if path is playing role of pathA not pathB
     * @return true to skip, else false
     */
    private static boolean skipPathForDependencies(String path, boolean isTypeA) {
        if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
            return true;
        }
        if (false && isTypeA) {
            final String[] toSkip = {
                "//ldml/characters/ellipsis",
                "//ldml/characters/exemplarCharacters",
                "//ldml/characters/parseLenients",
                "//ldml/layout/orientation/lineOrder",
                "//ldml/localeDisplayNames/codePatterns/codePattern",
                "//ldml/localeDisplayNames/keys/key",
                "//ldml/localeDisplayNames/languages/language",
                "//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern",
                "//ldml/localeDisplayNames/localeDisplayPattern/localePattern",
                "//ldml/localeDisplayNames/scripts/script",
                "//ldml/localeDisplayNames/territories/territory",
                "//ldml/localeDisplayNames/types/type",
                "//ldml/localeDisplayNames/variants/variant",
            };
            for (String s: toSkip) {
                if (path.startsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Write the given map of example-generator path dependencies to a json file.
     *
     * If this function is to be used and revised long-term, it would be better to use JSONObject,
     * or write a format other than json.
     * JSONObject isn't currently linked to cldr-unittest TestAll, package org.unicode.cldr.unittest.
     *
     * @param dependencies the map of example-generator path dependencies
     * @param fileName the name of the file to create, without path or extension
     * @param countOnly true to show only the count of the set for each key path in the map
     *                  false to include all the paths
     *
     * @throws IOException
     */
    private void writeDependenciesToFile(HashMap<String, HashSet<String>> dependencies, String fileName, boolean countOnly) throws IOException {
        // JSONObject json = new JSONObject(dependencies);
        // json.write(writer);
        String dir = CLDRPaths.GEN_DIRECTORY + "test/";
        String name = fileName + ".json";
        PrintWriter writer = FileUtilities.openUTF8Writer(dir, name);
        writer.println("{");

        ArrayList<String> list = new ArrayList<String>(dependencies.keySet());
        Collections.sort(list);
        boolean firstPathA = true;
        int keysWritten = 0;
        for (String pathA : list) {
            if (firstPathA) {
                firstPathA = false;
            } else {
                writer.println(",");
            }
            HashSet<String> set = dependencies.get(pathA);
            writer.print("  " + "\"" + pathA.replaceAll("\"", "\\\\\"") + "\"" + ": ");
            if (countOnly) {
                Integer count = set.size();
                writer.println(count.toString());
            } else {
                writer.println("[");
                boolean firstPathB = true;
                for (String pathB : set) {
                    if (firstPathB) {
                        firstPathB = false;
                    } else {
                        writer.println(",");
                    }
                    writer.print("    " + "\"" + pathB.replaceAll("\"", "\\\\\"") + "\"");
                }
                writer.println("");
                writer.print("  ]");
            }
            ++keysWritten;
        }

        writer.println("");
        writer.println("}");
        writer.close();
        System.out.println("Wrote " + keysWritten + " keys to " + dir + name);
    }
}
