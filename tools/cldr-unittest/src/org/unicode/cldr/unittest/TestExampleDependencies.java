package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
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
import org.unicode.cldr.util.RecordingCLDRFile;
import org.unicode.cldr.util.XMLSource;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;

public class TestExampleDependencies extends TestFmwk {

    private final boolean USE_STARRED_PATHS = true;
    private final boolean USE_RECORDING = true;
    private final String fileExtension = USE_RECORDING ? ".java" : ".json";

    private CLDRConfig info;
    private CLDRFile englishFile;
    private Factory factory;
    private Set<String> locales;
    private String outputDir;
    private PathStarrer pathStarrer;

    public static void main(String[] args) {
        new TestExampleDependencies().run(args);
    }

    /**
     * Test dependencies where changing the value of one path changes example-generation for another path.
     *
     * The goal is to optimize example caching by only regenerating examples when necessary.
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-13636
     *
     * @throws IOException
     */
    public void TestExampleGeneratorDependencies() throws IOException {
        info = CLDRConfig.getInstance();
        englishFile = info.getEnglish();
        factory = info.getCldrFactory();
        locales = factory.getAvailable();
        outputDir = CLDRPaths.GEN_DIRECTORY + "test" + File.separator;
        pathStarrer = USE_STARRED_PATHS ? new PathStarrer().setSubstitutionPattern("*") : null;

        System.out.println("...");
        System.out.println("Looping through " + locales.size() + " locales ...");

        if (USE_RECORDING) {
            /*
             * Fast method: use RecordingCLDRFile to learn which paths are checked
             * to produce the example for a given path. This is fast enough that we
             * can do it for all locales at once to produce a single file.
             */
            useRecording();
        } else {
            /*
             * Slow method: loop through all paths, modifying the value for each path
             * and then doing an inner loop through all paths to see whether the example
             * changed for each other path. This is extremely slow, so we produce one file
             * for each locale, with the intention of merging the files afterwards.
             */
            useModifying();
        }
    }

    private void useRecording() throws IOException {
        final Multimap<String, String> dependencies = TreeMultimap.create();
        for (String localeId : locales) {
            System.out.println(localeId);
            addDependenciesForLocale(dependencies, localeId);
        }
        String fileName = "ExampleDependencies" + fileExtension;
        System.out.println("Creating " + outputDir + fileName + " ...");
        writeDependenciesToFile(dependencies, outputDir, fileName);
    }

    private void addDependenciesForLocale(Multimap<String, String> dependencies, String localeId) {
        RecordingCLDRFile cldrFile = makeRecordingCldrFile(localeId);
        cldrFile.disableCaching();

        Set<String> paths = new TreeSet<>(cldrFile.getComparator());
        // time-consuming
        cldrFile.forEach(paths::add);

        ExampleGenerator egTest = new ExampleGenerator(cldrFile, englishFile, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        egTest.setCachingEnabled(false); // will not employ a cache -- this should save some time, since cache would be wasted

        for (String pathB : paths) {
            if (skipPathForDependencies(pathB)) {
                continue;
            }
            String valueB = cldrFile.getStringValue(pathB);
            if (valueB == null) {
                continue;
            }
            String starredB = USE_STARRED_PATHS ? pathStarrer.set(pathB) : null;
            cldrFile.clearRecordedPaths();
            egTest.getExampleHtml(pathB, valueB);
            HashSet<String> pathsA = cldrFile.getRecordedPaths();
            for (String pathA: pathsA) {
                if (pathA.equals(pathB) || skipPathForDependencies(pathA)) {
                    continue;
                }
                String starredA = USE_STARRED_PATHS ? pathStarrer.set(pathA) : null;
                dependencies.put(USE_STARRED_PATHS ? starredA : pathA,
                                  USE_STARRED_PATHS ? starredB : pathB);
            }
        }
    }

    private RecordingCLDRFile makeRecordingCldrFile(String localeId) {
        XMLSource topSource = factory.makeSource(localeId);
        List<XMLSource> parents = getParentSources(factory, localeId);
        XMLSource[] a = new XMLSource[parents.size()];
        return new RecordingCLDRFile(topSource, parents.toArray(a));
    }

    private void useModifying() throws IOException {
        for (String localeId : locales) {
            String fileName = "example_dependencies_A_"
                + localeId
                + (USE_STARRED_PATHS ? "_star" : "")
                + fileExtension;

            if (new File(outputDir, fileName).exists()) {
                System.out.println("Locale: " + localeId + " -- skipping since " +
                    outputDir + fileName + " already exists");
            } else {
                System.out.println("Locale: " + localeId + " -- creating "
                    + outputDir + fileName + " ...");
                writeOneLocale(localeId, outputDir, fileName);
            }
        }
    }

    private void writeOneLocale(String localeId, String outputDir, String fileName) throws IOException {
        CLDRFile cldrFile = makeMutableResolved(factory, localeId); // time-consuming
        cldrFile.disableCaching();

        Set<String> paths = new TreeSet<>(cldrFile.getComparator());
        // time-consuming
        cldrFile.forEach(paths::add);

        ExampleGenerator egBase = new ExampleGenerator(cldrFile, englishFile, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        HashMap<String, String> originalValues = new HashMap<>();

        getExamplesForBase(egBase, cldrFile, paths, originalValues);
        /*
         * Make egBase "cacheOnly" so that getExampleHtml will throw an exception if future queries
         * are not found in the cache. Alternatively, could just make a local hashmap originalExamples,
         * similar to originalValues. That might be more robust, require more memory, faster or slower?
         * Should try both ways.
         */
        egBase.setCacheOnly(true);

        ExampleGenerator egTest = new ExampleGenerator(cldrFile, englishFile, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        egTest.setCachingEnabled(false); // will not employ a cache -- this should save some time, since cache would be wasted

        CLDRFile top = cldrFile.getUnresolved(); // can mutate top

        final Multimap<String, String> dependencies = TreeMultimap.create();
        long count = 0;
        long skipCount = 0;
        long dependencyCount = 0;

        /*
         * For each path (A), temporarily change its value, and then check each other path (B),
         * to see whether changing the value for A changed the example for B.
         */
        for (String pathA : paths) {
            if (skipPathForDependencies(pathA)) {
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
            String starredA = USE_STARRED_PATHS ? pathStarrer.set(pathA) : null;
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
            if (!valueAX.equals(newValue)) {
                // Bad, didn't work as expected
                System.out.println("Changing top did not change cldrFile: newValue = " + newValue
                    + "; valueAX = " + valueAX + "; valueA = " + valueA);
            }

            for (String pathB : paths) {
                if (pathA.equals(pathB) || skipPathForDependencies(pathB)) {
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
                if (valueB == null) {
                    continue;
                }
                pathB = pathB.intern();

                // egTest.icuServiceBuilder.setCldrFile(cldrFile); // clear caches in icuServiceBuilder; has to be public
                String exBase = egBase.getExampleHtml(pathB, valueB); // this will come from cache (or throw cacheOnly exception)
                String exTest = egTest.getExampleHtml(pathB, valueB); // this won't come from cache
                if ((exTest == null) != (exBase == null)) {
                    throw new InternalError("One null but not both? " + pathA + " --- " + pathB);
                } else if (exTest != null && !exTest.equals(exBase)) {
                    dependencies.put(USE_STARRED_PATHS ? starredA : pathA, USE_STARRED_PATHS ? pathStarrer.set(pathB).intern() : pathB);
                    ++dependencyCount;
                }
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
        writeDependenciesToFile(dependencies, outputDir, fileName);
        System.out.println("count = " + count + "; skipCount = " + skipCount + "; dependencyCount = " + dependencyCount);
    }

    /**
     * Get all the examples so they'll be added to the cache for egBase.
     * Also fill originalValues.
     *
     * @param egBase
     * @param cldrFile
     * @param paths
     * @param originalValues
     */
    private void getExamplesForBase(ExampleGenerator egBase, CLDRFile cldrFile, Set<String> paths, HashMap<String, String> originalValues) {
        for (String path : paths) {
            if (skipPathForDependencies(path)) {
                continue;
            }
            String value = cldrFile.getStringValue(path);
            if (value == null) {
                continue;
            }
            originalValues.put(path, value);
            egBase.getExampleHtml(path, value);
        }
    }

    /**
     * Modify the given value string for testing dependencies
     *
     * @param value
     * @return the modified value, guaranteed to be different from value
     *
     * Note: it might be best to avoid IllegalArgumentException thrown/caught in, e.g., ICUServiceBuilder.getSymbolString;
     * in which case this function might need path as parameter, to generate only "legal" values for specific paths.
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
     * @param localeId
     * @return the CLDRFile
     */
    private static CLDRFile makeMutableResolved(Factory factory, String localeId) {
        XMLSource topSource = factory.makeSource(localeId).cloneAsThawed(); // make top one modifiable
        List<XMLSource> parents = getParentSources(factory, localeId);
        XMLSource[] a = new XMLSource[parents.size()];
        return new CLDRFile(topSource, parents.toArray(a));
    }

    /**
     * Get the parent sources for the given localeId
     *
     * @param factory
     * @param localeId
     * @return the List of XMLSource objects
     *
     * Called only by makeMutableResolved
     */
    private static List<XMLSource> getParentSources(Factory factory, String localeId) {
        List<XMLSource> parents = new ArrayList<>();
        for (String currentLocaleID = LocaleIDParser.getParent(localeId);
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
    private static boolean skipPathForDependencies(String path) {
        if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
            return true;
        }
        return false;
    }

    /**
     * Write the given map of example-generator path dependencies to a json or java file.
     *
     * If this function is to be used for json and revised long-term, it would be better to use JSONObject,
     * or write a format other than json.
     * JSONObject isn't currently linked to cldr-unittest TestAll, package org.unicode.cldr.unittest.
     *
     * @param dependencies the multimap of example-generator path dependencies
     * @param dir the directory in which to create the file
     * @param fileName the name of the file to create
     *
     * @throws IOException
     */
    private void writeDependenciesToFile(Multimap<String, String> dependencies, String dir, String name) throws IOException {
        PrintWriter writer = FileUtilities.openUTF8Writer(dir, name);
        if (fileExtension.equals(".json")) {
            writeJson(dependencies, dir, name, writer);
        } else {
            writeJava(dependencies, dir, name, writer);
        }
    }

    private void writeJava(Multimap<String, String> dependencies, String dir, String name, PrintWriter writer) {
        writer.println("package org.unicode.cldr.test;");
        writer.println("import com.google.common.collect.ImmutableSetMultimap;");
        writer.println("public class ExampleDependencies {");
        writer.println("    public static ImmutableSetMultimap<String, String> dependencies");
        writer.println("            = new ImmutableSetMultimap.Builder<String, String>()");
        int dependenciesWritten = 0;
        ArrayList<String> listA = new ArrayList<>(dependencies.keySet());
        Collections.sort(listA);
        for (String pathA : listA) {
            ArrayList<String> listB = new ArrayList<>(dependencies.get(pathA));
            Collections.sort(listB);
            String a = "\"" + pathA.replaceAll("\"", "\\\\\"") + "\"";
            for (String pathB : listB) {
                String b = "\"" + pathB.replaceAll("\"", "\\\\\"") + "\"";
                writer.println("        .put(" + a + ", " + b + ")");
                ++dependenciesWritten;
            }
        }
        writer.println("        .build();");
        writer.println("}");
        writer.close();
        System.out.println("Wrote " + dependenciesWritten + " dependencies to " + dir + name);
    }

    private void writeJson(Multimap<String, String> dependencies, String dir, String name, PrintWriter writer) {
        ArrayList<String> list = new ArrayList<>(dependencies.keySet());
        Collections.sort(list);
        boolean firstPathA = true;
        int keysWritten = 0;
        for (String pathA : list) {
            if (firstPathA) {
                firstPathA = false;
            } else {
                writer.println(",");
            }
            Collection<String> values = dependencies.get(pathA);
            writer.print("  " + "\"" + pathA.replaceAll("\"", "\\\\\"") + "\"" + ": ");
            writer.println("[");
            boolean firstPathB = true;
            for (String pathB : values) {
                if (firstPathB) {
                    firstPathB = false;
                } else {
                    writer.println(",");
                }
                writer.print("    " + "\"" + pathB.replaceAll("\"", "\\\\\"") + "\"");
            }
            writer.println("");
            writer.print("  ]");
            ++keysWritten;
        }
        writer.println("");
        writer.println("}");
        writer.close();
        System.out.println("Wrote " + keysWritten + " keys to " + dir + name);
    }
}
