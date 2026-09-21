package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.Output;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.XCoverageLevel.AttributesMatcher;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMultimap2;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.NestedMap.Multimap3;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UPair;

/** Code to generate simple-to-parse coverage files for locales. */
public class GenerateXCoverage {
    private static final boolean DEBUG = XCoverageLevel.DEBUG;
    private static Set<String> TEST_PATHS = XCoverageLevel.TEST_PATHS;
    private static final boolean SHOW_PROGRESS = false;
    private static final boolean GENERATE_DEBUG_FILES = false;

    /** Simple enum for testing. Normally "all" is used */
    private enum Run {
        af,
        tiny,
        tc,
        tcPlus,
        all
    }

    private static final Run SHORT_RUN = Run.valueOf(System.getProperty("run", "tiny"));

    private static final Path OUTPUT_COMPACTED =
            Path.of(CLDRPaths.COMMON_DIRECTORY, "pathCoverage");
    private static final Path OUTPUT_FULL =
            GENERATE_DEBUG_FILES ? Path.of(CLDRPaths.COMMON_DIRECTORY, "pathCoverage-full") : null;
    private static final Path OUTPUT_INFO =
            GENERATE_DEBUG_FILES ? Path.of(CLDRPaths.COMMON_DIRECTORY, "pathCoverage-info") : null;
    // CLDRPaths.GEN_DIRECTORY + "coverage";
    private static final String FILE_SUFFIX = ".txt";
    private static final int MAX_ATTR_SET_SIZE = 31;
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();
    private static final Comparator<Iterable<String>> LEX_ITERABLE_COMPARATOR =
            Ordering.natural().lexicographical();
    private static final Supplier<Map<Object, Object>> TREEMAP_LEXICAL =
            () -> new TreeMap(LEX_ITERABLE_COMPARATOR);

    public static void main(String[] args) throws IOException {

        Variables variableToValue = new Variables();

        Files.createDirectories(OUTPUT_COMPACTED);
        if (GENERATE_DEBUG_FILES) {
            Files.createDirectories(OUTPUT_FULL);
            Files.createDirectories(OUTPUT_INFO);
        }

        Counter<String> pathCounter = new Counter<>();

        Set<String> TCLocales = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);
        Set<String> TCLocalesPlusChildren =
                CLDR_FACTORY.getAvailable().stream()
                        .filter(
                                x ->
                                        TCLocales.contains(
                                                CLDRLocale.getInstance(x).getLanguageScript()))
                        .collect(Collectors.toCollection(TreeSet::new));

        Set<String> localesToCheck;
        switch (SHORT_RUN) {
            case af:
                localesToCheck = Set.of("af");
                break;
            case tiny:
                localesToCheck = ImmutableSet.of("en", "af", "de", "de_CH", "de_AT", "ja");
                break;
            case tc:
                localesToCheck = TCLocales;
                break;
            case tcPlus:
                localesToCheck = TCLocalesPlusChildren;
                break;
            case all:
            default:
                localesToCheck = CLDR_FACTORY.getAvailable();
                break;
        }

        // Create a mapping from a base language to the locales that should be reduced by it

        Multimap<String, String> baseToLocales = LinkedHashMultimap.create();
        final String root = "root";
        for (String locale : localesToCheck) {
            if (locale.equals(root)) {
                continue;
            }
            String lang = CLDRLocale.getInstance(locale).getLanguage();
            if (lang.equals(locale)) {
                baseToLocales.put(
                        lang, ""); // to catch the strange case of no children. Filtered below.
            } else {
                baseToLocales.put(lang, locale);
            }
        }
        baseToLocales = ImmutableMultimap.copyOf(baseToLocales);

        XCoverageLevel rootXCoverage = createXCoverageLevel(root, variableToValue, pathCounter);
        if (GENERATE_DEBUG_FILES) {
            writeXCoverageLevel(rootXCoverage, OUTPUT_FULL, root + FILE_SUFFIX);
            XCoverageLevel xCoverage = XCoverageLevel.fromLocale(OUTPUT_FULL, root);
            checkFile("full", root, xCoverage);
        }

        // write full copy in reduced directory
        writeXCoverageLevel(rootXCoverage, OUTPUT_COMPACTED, root + FILE_SUFFIX);
        XCoverageLevel roundTrip = XCoverageLevel.fromLocale(OUTPUT_COMPACTED, root);
        checkFile("compact", root, roundTrip);

        for (Entry<String, Collection<String>> entry : baseToLocales.asMap().entrySet()) {
            String baseLanguage = entry.getKey();
            Collection<String> children = entry.getValue();
            XCoverageLevel fullBaseLanguageCoverage =
                    createXCoverageLevel(baseLanguage, variableToValue, pathCounter);

            if (GENERATE_DEBUG_FILES) {
                writeXCoverageLevel(
                        fullBaseLanguageCoverage, OUTPUT_FULL, baseLanguage + FILE_SUFFIX);

                // open file and check

                roundTrip = XCoverageLevel.fromLocale(OUTPUT_FULL, baseLanguage);
                checkFile("full", baseLanguage, roundTrip);
            }

            // now write compact version

            XCoverageLevel reducedByRoot = fullBaseLanguageCoverage.subtractSame(rootXCoverage);
            writeFileOrDeleteIfEmpty(baseLanguage, reducedByRoot, root);

            // open file and check

            roundTrip = XCoverageLevel.fromLocaleCompacted(OUTPUT_COMPACTED, baseLanguage);
            if (!checkFile("compact", baseLanguage, roundTrip)) {
                writeXCoverageLevel(roundTrip, OUTPUT_INFO, baseLanguage + FILE_SUFFIX);
                roundTrip.showRuleDifferences(fullBaseLanguageCoverage);
            }

            for (String child : children) {
                if (child.isEmpty()) {
                    continue;
                }
                XCoverageLevel fullLocaleCoverage =
                        createXCoverageLevel(child, variableToValue, pathCounter);
                if (GENERATE_DEBUG_FILES) {

                    writeXCoverageLevel(fullLocaleCoverage, OUTPUT_FULL, child + FILE_SUFFIX);

                    // open file and check
                    roundTrip = XCoverageLevel.fromLocale(OUTPUT_FULL, child);
                    checkFile("full", child, roundTrip);
                }

                // now write compact version

                XCoverageLevel reducedByBase =
                        fullLocaleCoverage.subtractSame(fullBaseLanguageCoverage);
                writeFileOrDeleteIfEmpty(child, reducedByBase, baseLanguage);

                // open file and check
                if (GENERATE_DEBUG_FILES) {

                    roundTrip = XCoverageLevel.fromLocaleCompacted(OUTPUT_FULL, child);
                    if (!checkFile("compact", child, roundTrip)) {
                        if (GENERATE_DEBUG_FILES) {
                            writeXCoverageLevel(
                                    roundTrip, OUTPUT_INFO, child + "-debug" + FILE_SUFFIX);
                        } else {
                            throw new IllegalArgumentException("roundtrip failure");
                        }
                    }
                }
            }
        }

        // Now special files

        if (GENERATE_DEBUG_FILES) {
            // Write root coverage for all paths
            XCoverageLevel xCoverage1 = createXCoverageLevel("mul", variableToValue, pathCounter);
            writeXCoverageLevel(xCoverage1, OUTPUT_INFO, "mul" + FILE_SUFFIX);

            // Write all variables
            Path allVariables = OUTPUT_INFO.resolve("variables.txt");
            try (PrintStream out = new PrintStream(Files.newOutputStream(allVariables))) {

                // get inverted map of variables
                out.println("# Variables");
                for (String variable : variableToValue.getVariables()) {
                    out.println(variable + "=" + variableToValue.getValue(variable));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            CLDRFile cldrFile = CLDR_FACTORY.make(root, true);

            TreeSet<String> rootPaths = Sets.newTreeSet(cldrFile.fullIterable());
            TreeSet<String> rootChassis =
                    rootPaths.stream()
                            .map(x -> SplitPath.from(x).getChassis())
                            .collect(Collectors.toCollection(TreeSet::new));
            Path chassisInfo = OUTPUT_INFO.resolve("chassisInfo.tsv");

            Counter<String> chassisCounter = new Counter<>();
            for (String path : pathCounter.keySet()) {
                String chassis = SplitPath.from(path).getChassis();
                if (!rootChassis.contains(chassis)) {
                    chassisCounter.add(chassis, 1);
                }
            }

            try (PrintStream out = new PrintStream(Files.newOutputStream(chassisInfo))) {
                for (R2<Long, String> entry :
                        chassisCounter.getEntrySetSortedByCount(false, null)) {
                    out.println(entry.get0() + "\t" + entry.get1());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void writeFileOrDeleteIfEmpty(
            String locale, XCoverageLevel reduced, String importLocale) throws IOException {
        if (!reduced.isEmpty()) {
            writeXCoverageLevel(reduced, OUTPUT_COMPACTED, locale + FILE_SUFFIX, importLocale);
        } else {
            Files.deleteIfExists(OUTPUT_COMPACTED.resolve(locale + FILE_SUFFIX));
        }
    }

    /**
     * Verifies that the rules in an derive XCoverageLevel object match what the CLDR
     * coverageLevels.xml has
     *
     * @param title TODO
     * @return
     */
    private static boolean checkFile(String title, String locale, XCoverageLevel xCoverage) {
        if (DEBUG) {
            TEST_PATHS.stream().forEach(x -> xCoverage.getPathData(x));
        }

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        boolean firstHeader = true;
        final Multimap2<String, Pair<Level, Level>, List<String>> chassisSeen =
                Multimap2.create(TreeMap::new, TreeMap::new, LinkedHashMap::new);

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            Level realLevel = SDI.getCoverageLevel(path, locale);
            Level xLevel = xCoverage.getCoverage(path);

            if (realLevel != xLevel) {
                SplitPath split = SplitPath.from(path);
                String chassis = split.getChassis();
                chassisSeen.put(chassis, Pair.of(realLevel, xLevel), split.getAttributeValues());
                if (DEBUG && TEST_PATHS.contains(chassis)) {
                    xLevel = xCoverage.getCoverage(path); // for debugging
                }
            }
        }
        if (chassisSeen.keySet().size() == 0) {
            return true;
        }
        ImmutableMultimap2<String, Pair<Level, Level>, List<String>> failures =
                chassisSeen.createImmutable();
        System.out.println("\nFailing " + title + ":\t" + locale + "\n\tLevel\t\tXLevel\t\tPath");
        for (String chassis : failures.keySet()) {
            Map<Pair<Level, Level>, Map<List<String>, Boolean>> map = failures.getMapMap(chassis);
            for (Entry<Pair<Level, Level>, Map<List<String>, Boolean>> entry : map.entrySet()) {
                System.out.println(
                        Joiners.TAB.join(chassis, entry.getKey(), entry.getValue().keySet()));
            }
            System.out.println(xCoverage.getPathData(chassis));
        }
        System.out.println();
        return false;
    }

    private static void writeXCoverageLevel(
            XCoverageLevel xCoverage, Path outputDir, String fileName) {
        writeXCoverageLevel(xCoverage, outputDir, fileName, null);
    }

    /** Writes an XCoverageLevel to a file */
    private static void writeXCoverageLevel(
            XCoverageLevel xCoverage, Path outputDir, String fileName, String importValue) {
        Path fullFileName = outputDir.resolve(fileName);
        try (PrintStream out2 = new PrintStream(Files.newOutputStream(fullFileName))) {
            out2.print(xCoverage.toString(importValue));
            System.out.println("Writing:\t" + fullFileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create an XCoverageLevel object from the coverage levels determined by the internal CLDR
     * coverageLevels.xml
     *
     * @param locale The locale in question
     * @param allVariables A set of variables, to which this locale will add new ones
     * @param allPaths A counter to get the number of paths in question. Only informational.
     * @return
     */
    private static XCoverageLevel createXCoverageLevel(
            String locale, Variables allVariables, Counter<String> allPaths) {
        boolean rootWithAllPaths = locale.equals("mul");

        allVariables.clearVariablesInCurrentFile();

        CLDRFile cldrFile = CLDR_FACTORY.make(rootWithAllPaths ? "root" : locale, true);

        Multimap2<String, Level, List<String>> _chassisToLevelToAttributeList =
                Multimap2.create(TreeMap::new, TreeMap::new, LinkedHashMap::new);

        TreeSet<String> localePaths = Sets.newTreeSet(cldrFile.fullIterable());

        for (String path : localePaths) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            allPaths.add(path, 1);
            Level level = SDI.getCoverageLevel(path, locale);
            SplitPath splitPath = SplitPath.from(path);
            List<String> attributes = splitPath.getAttributeValues();
            _chassisToLevelToAttributeList.put(splitPath.getChassis(), level, attributes);
        }
        ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList =
                _chassisToLevelToAttributeList.createImmutable();

        Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel =
                Map2.create(LinkedHashMap::new);

        SortedSet<String> sortedChassis =
                ImmutableSortedSet.copyOf(chassisToLevelToAttributeList.keySet());
        if (DEBUG) {
            TEST_PATHS.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x
                                                    + "\n\t"
                                                    + chassisToLevelToAttributeList.getMapMap(x)));
        }
        if (DEBUG && SHOW_PROGRESS) {
            System.out.println("Raw Chassis\n" + Joiners.N.join(sortedChassis));
        }

        if (DEBUG && SHOW_PROGRESS) {
            System.out.println("\nMinimized");
        }

        for (String chassis : sortedChassis) {
            if (DEBUG && TEST_PATHS.contains(chassis)) {
                int debug = 0;
            }
            SortedSet<Level> levelSet =
                    ImmutableSortedSet.copyOf(chassisToLevelToAttributeList.keySet2(chassis));
            minimizeAttributes(
                    chassis,
                    chassisToLevelToAttributeList,
                    allVariables,
                    levelSet,
                    pathChassisToAttributeMatcherToLevel);
        }

        if (DEBUG) {
            TEST_PATHS.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            pathChassisToAttributeMatcherToLevel
                                                    .createImmutable()
                                                    .getMap(x)));
        }

        Map<String, String> localVariables = Maps.newTreeMap();
        for (String variable : allVariables.getVariablesInCurrentFile()) {
            localVariables.put(variable, allVariables.getValue(variable));
        }
        XCoverageLevel xCoverage =
                XCoverageLevel.fromMap2(pathChassisToAttributeMatcherToLevel, localVariables);
        return xCoverage;
    }

    /**
     * Support variables to avoid duplication and make the rules clearer. It keeps a global list of
     * variables across all locales. That enables compaction of variables. There is also a list of
     * just the variables used in a particular file.
     */
    static class Variables {
        final Map<String, String> valueToVariable = Maps.newTreeMap();
        final Map<String, String> variableToValue = Maps.newTreeMap();
        final Set<String> variablesInCurrentFile = Sets.newTreeSet();

        public Set<String> getVariablesInCurrentFile() {
            return variablesInCurrentFile;
        }

        public void clearVariablesInCurrentFile() {
            variablesInCurrentFile.clear();
        }

        // not multithreaded, but we could make it so
        String add(String value, String chassis, Entry<Integer, SortedSet<String>> x) {
            String variableName = valueToVariable.get(value);
            if (variableName == null) {
                String element = SplitPath.findElementForAttribute(chassis, x.getKey());
                String base =
                        "%"
                                + element
                                + x.getValue()
                                        .size(); // String.format("%03d", valueToVariable.size());
                // make sure it is unique
                for (int i = 0; ; ++i) {
                    variableName = base + (i == 0 ? "" : id(i));
                    if (!variableToValue.containsKey(variableName)) {
                        break;
                    }
                }
                valueToVariable.put(value, variableName);
                variableToValue.put(variableName, value);
            }
            variablesInCurrentFile.add(variableName);
            return variableName;
        }

        private String id(int i) {
            if (i == 0) {
                return "";
            }
            --i;
            if (i < 26) {
                return String.valueOf((char) (i + 'a'));
            }
            return String.valueOf((char) ((i / 26) + 'a'))
                    + String.valueOf((char) ((i % 26) + 'a'));
        }

        String getValue(String variable) {
            return variableToValue.get(variable);
        }

        Set<String> getVariables() {
            return variableToValue.keySet();
        }

        public boolean isEmpty() {
            return variableToValue.isEmpty();
        }
    }

    /** This is the core method for finding the differences */
    private static void minimizeAttributes(
            String chassis,
            ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList,
            Variables variableToValue,
            SortedSet<Level> levelSet,
            Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel) {

        boolean debugPath = DEBUG && TEST_PATHS.contains(chassis);
        if (debugPath) {
            show(chassis, chassisToLevelToAttributeList);
        }

        Level firstLevel = levelSet.iterator().next();
        if (levelSet.size() == 1) {
            // everything is at the same level, no need to do any work!
            pathChassisToAttributeMatcherToLevel.put(chassis, AttributesMatcher.EMPTY, firstLevel);
            return;
        }

        SortedSet<Level> levelSetMinusLast = Sets.newTreeSet(levelSet);
        levelSetMinusLast.remove(Iterables.getLast(levelSet));

        Set<List<String>> firstAttributeSetList =
                chassisToLevelToAttributeList.get(chassis, firstLevel);
        final int attributeCount = firstAttributeSetList.iterator().next().size();

        // first step is to see if there is a single attribute that distinguishes all the levels
        // we first get maps from the attribute number to the level to the attribute.

        // example, if basic iff {{1,gregorian, 1,generic}}, we have distinguished basic from other
        // levels
        Multimap2<Integer, Level, String> attrNumTolevelToAttribute =
                Multimap2.create(TreeMap::new);
        for (Level level : levelSet) {
            Set<List<String>> attributeLists = chassisToLevelToAttributeList.get(chassis, level);
            for (List<String> attributeList : attributeLists) {
                for (int i = 0; i < attributeList.size(); ++i) {
                    attrNumTolevelToAttribute.put(i, level, attributeList.get(i));
                }
            }
        }

        // if the attributeNumber & attribute only maps to a single level, we have enough
        // information to distinguish that level

        Map<Level, Delta> distinguishes = Maps.newTreeMap();

        for (Integer attrNum : attrNumTolevelToAttribute.keySet()) {
            for (Level level1 : levelSetMinusLast) {
                if (distinguishes.containsKey(level1)) {
                    continue; // already done
                }
                Set<String> atLevel1 = attrNumTolevelToAttribute.get(attrNum, level1);
                Set<String> aboveLevel1 = Sets.newTreeSet();
                for (Level level2 : levelSet) {
                    if (level2.compareTo(level1) <= 0) {
                        continue;
                    }
                    aboveLevel1.addAll(attrNumTolevelToAttribute.get(attrNum, level2));
                }
                if (Collections.disjoint(atLevel1, aboveLevel1)) {
                    // level1 is distinguished from level2 by set1
                    distinguishes.put(level1, new Delta(attrNum, atLevel1, aboveLevel1));
                } else {
                    if (debugPath) {
                        System.out.println("FAILS1");
                        System.out.println(level1 + "\t" + atLevel1);
                        System.out.println("REST" + "\t" + aboveLevel1);
                    }
                }
            }
        }
        // sometimes what distinguishes (eg) basic from moderate is the same as what distinguishes
        // basic from modern.
        // so we need to combine them

        if (debugPath) {
            System.out.println("DISTINGUISHING: " + chassis);
            distinguishes.entrySet().stream()
                    .forEach(x -> System.out.println(x.getKey() + ", " + x.getValue()));
        }

        // find missing
        boolean succeeds = levelSetMinusLast.equals(distinguishes.keySet());

        if (succeeds) {
            getPathRules(
                    variableToValue,
                    chassis,
                    levelSet,
                    levelSetMinusLast,
                    distinguishes,
                    pathChassisToAttributeMatcherToLevel);
            return;
        }

        // 1 attribute didn't work. Try more
        for (int count = 2; count <= attributeCount && count < 5; ++count) {
            Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                    getAttributeNumToLevelToAttributesCombinations2(
                            chassisToLevelToAttributeList, count, chassis, levelSet);
            if (debugPath) {
                System.out.println("\nChecking " + count + " attributes: " + chassis);
            }

            findDistinguishing(
                    chassis,
                    levelSet,
                    levelSetMinusLast,
                    attrNumTolevelToAttribute2,
                    distinguishes);

            if (debugPath) {
                System.out.println("DISTINGUISHING: " + chassis);
                distinguishes.entrySet().stream().forEach(System.out::println);
            }
            if (levelSetMinusLast.equals(distinguishes.keySet())) {
                getPathRules(
                        variableToValue,
                        chassis,
                        levelSet,
                        levelSetMinusLast,
                        distinguishes,
                        pathChassisToAttributeMatcherToLevel);
                return;
            } else {
                if (debugPath) {
                    System.out.println("FAILS" + count);
                    System.out.println(distinguishes);
                }
            }
        }
    }

    /** Used in debugging */
    private static void show(
            String chassis,
            ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList) {
        System.out.println("BASIC: " + chassis);
        Map<Level, Map<List<String>, Boolean>> foo =
                chassisToLevelToAttributeList.getMapMap(chassis);
        for (Entry<Level, Map<List<String>, Boolean>> entry : foo.entrySet()) {
            Multimap<String, String> fii = LinkedHashMultimap.create();
            entry.getValue().keySet().stream()
                    .forEach(x -> fii.putAll(x.iterator().next(), x.subList(1, x.size())));
            fii.asMap().entrySet().stream().forEach(x -> System.out.println("\t" + x));
            System.out.println("level=" + entry.getKey());
        }
    }

    /** Get the various combinations to try */
    private static Multimap2<List<Integer>, Level, List<String>>
            getAttributeNumToLevelToAttributesCombinations2(
                    Multimap2<String, Level, List<String>> chassisToLevelToAttributeList,
                    int count,
                    String chassis,
                    SortedSet<Level> levelSet) {
        Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                Multimap2.create(TREEMAP_LEXICAL, TreeMap::new, HashMap::new);

        for (Level level : levelSet) {
            Set<List<String>> attributeLists = chassisToLevelToAttributeList.get(chassis, level);
            for (List<String> attributeList : attributeLists) {
                generateCombinations(
                        attributeList,
                        level,
                        0,
                        count,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        attrNumTolevelToAttribute2);
            }
        }
        return attrNumTolevelToAttribute2;
    }

    /**
     * Generate all the combinations that we want to try. Could be faster, but we don't really care
     * enough to optimize
     */
    private static void generateCombinations(
            List<String> attributeList,
            Level level,
            int start,
            int count,
            List<Integer> currentIndices,
            List<String> currentValues,
            Multimap2<List<Integer>, Level, List<String>> resultMultimap) {
        if (currentIndices.size() == count) {
            resultMultimap.put(List.copyOf(currentIndices), level, List.copyOf(currentValues));
            return;
        }

        for (int i = start; i < attributeList.size(); ++i) {
            currentIndices.add(i);
            currentValues.add(attributeList.get(i));

            generateCombinations(
                    attributeList,
                    level,
                    i + 1,
                    count,
                    currentIndices,
                    currentValues,
                    resultMultimap);

            currentIndices.remove(currentIndices.size() - 1);
            currentValues.remove(currentValues.size() - 1);
        }
    }

    /**
     * Get the rules for a level. We try all combinations of levels: first all the single
     * attributes, then all the pairs, then all the triples, then all the quadruples. Once we find a
     * distinction between a level and those above, we skip that level in future passes. When we
     * have all levels distinct, then we put the rules into pathChassisToAttributeMatcherToLevel.
     * The very last level needs no rules (since there is nothing above it).
     */
    private static void getPathRules(
            Variables variableToValue,
            String chassis,
            SortedSet<Level> levelSet,
            SortedSet<Level> levelSetMinusLast,
            Map<Level, Delta> distinguishes,
            Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel) {

        if (DEBUG && TEST_PATHS.contains(chassis)) {
            int debug = 0;
        }

        Level lastHere = Iterables.getLast(levelSetMinusLast);
        for (Level level : levelSetMinusLast) {
            Delta distinguishingAttributes = distinguishes.get(level);
            // Cases: a-b, a-c => attr1=a, attr2=b|c
            // Cases: a-b, c-b => attr1=a|c, attr2=b
            // Cases: a-b, c-d => attr1=a, attr2=b ; attr1=c, attr2=d
            List<Map<Integer, SortedSet<String>>> attributeRules;
            switch (distinguishingAttributes.getAttributeNumbers().size()) {
                default:
                    throw new UnsupportedOperationException();
                case 1:
                    attributeRules = toAttributeRulesFor1(distinguishingAttributes);
                    // checkCoalesce(distinguishingAttributes, attributeRules);
                    break;
                case 2:
                    attributeRules = toAttributeRulesFor2(distinguishingAttributes);
                    break;
                case 3:
                    attributeRules = toAttributeRulesFor3(distinguishingAttributes);
                    break;
                case 4:
                    attributeRules = toAttributeRulesFor4(distinguishingAttributes);
                    break;
            }
            Delta nextItems = level == lastHere ? distinguishes.get(lastHere) : null;
            AttributesMatcher.Builder builder = new AttributesMatcher.Builder();

            final Output<Boolean> positive = new Output<>(true);

            for (Map<Integer, SortedSet<String>> map : attributeRules) {
                map.entrySet().stream()
                        .forEach(
                                entry -> {
                                    SortedSet<String> attributeValues =
                                            fixVariablesAndNegation(
                                                    chassis, nextItems, entry.getValue(), positive);
                                    String result = Joiners.COMMA.join(attributeValues);
                                    String stringMatches =
                                            attributeValues.size() > MAX_ATTR_SET_SIZE
                                                    ? variableToValue.add(result, chassis, entry)
                                                    : result;
                                    builder.add(entry.getKey(), positive.value, stringMatches);
                                });
                pathChassisToAttributeMatcherToLevel.put(chassis, builder.build(), level);
            }
        }
        Level elseLevel = Iterables.getLast(levelSet);
        pathChassisToAttributeMatcherToLevel.put(chassis, AttributesMatcher.EMPTY, elseLevel);
    }

    /**
     * Process a list of attributes to find out whether to represent as set, or a negation of a set
     */
    private static SortedSet<String> fixVariablesAndNegation(
            String chassis,
            Delta nextItems,
            SortedSet<String> attributeValues,
            Output<Boolean> positive) {
        positive.value = true;
        if (nextItems != null && attributeValues.size() > MAX_ATTR_SET_SIZE) {
            // if we are at the last level before elseLevel AND we have a large set
            // see if it makes sense to use a negation
            if (DEBUG && TEST_PATHS.contains(chassis)) {
                int debug = 0;
            }
            Set<List<String>> foo = nextItems.getDelta2();
            if (foo.iterator().next().size() == 1) { // for now, TODO handle 2 attribute sets
                SortedSet<String> temp =
                        foo.stream()
                                .map(y -> y.get(0))
                                .collect(Collectors.toCollection(TreeSet::new));
                if (temp.size() < attributeValues.size() * 2 / 3) {
                    attributeValues = temp;
                    positive.value = false;
                }
            } else {
                if (DEBUG && TEST_PATHS.contains(chassis)) {
                    int debug = 0;
                }
            }
        }
        return attributeValues;
    }

    /** Find the distinguishing attributes between one level and all the subsequent levels. */
    private static void findDistinguishing(
            String chassis,
            Set<Level> levelSet,
            Set<Level> levelSetMinusLast,
            Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2,
            Map<Level, Delta> distinguishes) {
        for (List<Integer> attrNum : attrNumTolevelToAttribute2.keySet()) {
            for (Level level1 : levelSetMinusLast) {
                if (distinguishes.containsKey(level1)) {
                    continue; // already done
                }

                Set<List<String>> atLevel1 = attrNumTolevelToAttribute2.get(attrNum, level1);

                // These are all the sets ABOVE level1
                Set<List<String>> aboveLevel1 = Sets.newHashSet();
                for (Level level2 : levelSet) {
                    if (level2.compareTo(level1) <= 0) {
                        continue;
                    }
                    aboveLevel1.addAll(attrNumTolevelToAttribute2.get(attrNum, level2));
                }
                if (Collections.disjoint(atLevel1, aboveLevel1)) {
                    // level1 is distinguished from level2 AND ABOVE by set1
                    distinguishes.put(level1, new Delta(attrNum, atLevel1, aboveLevel1));
                } else {
                    if (DEBUG && TEST_PATHS.contains(chassis)) {
                        System.out.println("FAILS2");
                        System.out.println(level1 + "\t" + atLevel1);
                        System.out.println("REST" + "\t" + aboveLevel1);
                    }
                }
            }
        }
    }

    /**
     * One of 4 methods to get grouping of attributes, such as: *
     *
     * <pre>
     *
     * [gregorian, format, wide]
     * [gregorian, format, narrow]
     * [generic, format, wide>]
     * [generic, format, narrow>]
     * [generic, stand-alone, wide>]
     * =>
     * [[gregorian, generic], [format], [wide, narrow]]
     * [[generic], [format,stand-alone], [wide]
     *
     * </pre>
     */
    private static List<Map<Integer, SortedSet<String>>> toAttributeRulesFor4(Delta delta) {
        List<Map<Integer, SortedSet<String>>> result = new ArrayList<>();
        Multimap3<String, String, String, String> map1 = Multimap3.create(TreeMap::new);
        delta.getDelta1().stream()
                .forEach(
                        x -> {
                            map1.put(x.get(0), x.get(1), x.get(2), x.get(3));
                        });

        // We want need to collect sets of correspondences, sets mapping to sets.
        // So we create a multimap from the value sets to the values
        Multimap<Set<String>, List<String>> map2 =
                TreeMultimap.create(LEX_ITERABLE_COMPARATOR, LEX_ITERABLE_COMPARATOR);

        for (String a1 : map1.keySet()) {
            for (String a2 : map1.keySet2(a1)) {
                for (String a3 : map1.keySet3(a1, a2)) {
                    // find the set of a4 that this a1, a2, a3 triples share
                    Set<String> set = map1.get(a1, a2, a3);
                    map2.put(set, List.of(a1, a2, a3));
                }
            }
        }
        // we have now collected all the a4 sets that share an a1a2a3Triple
        // we will further collect the a3s that share the same a1s
        for (Entry<Set<String>, Collection<List<String>>> a4sAndTriples : map2.asMap().entrySet()) {
            Set<String> a4s = a4sAndTriples.getKey();

            Multimap2<String, String, String> map1b = Multimap2.create(TreeMap::new);
            for (List<String> list3 : a4sAndTriples.getValue()) {
                map1b.put(list3.get(0), list3.get(1), list3.get(2));
            }

            Multimap<Set<String>, List<String>> map2b =
                    TreeMultimap.create(LEX_ITERABLE_COMPARATOR, LEX_ITERABLE_COMPARATOR);

            for (String a1 : map1b.keySet()) {
                for (String a2 : map1.keySet2(a1)) {
                    // find the set of a3 that this a1, a2 pair shares
                    Set<String> set = map1b.get(a1, a2);
                    map2b.put(set, List.of(a1, a2));
                }
            }

            // we have now collected all the a3 sets that share an a1a2Pair
            // we will further collect the a2s that share the same a1s
            for (Entry<Set<String>, Collection<List<String>>> a3sAndPairs :
                    map2b.asMap().entrySet()) {
                Set<String> a3s = a3sAndPairs.getKey();
                Multimap<String, String> a1a2 = TreeMultimap.create();
                Collection<List<String>> val = a3sAndPairs.getValue();
                for (List<String> a1a2Pairs : val) {
                    a1a2.put(a1a2Pairs.get(0), a1a2Pairs.get(1));
                }
                // we now invert
                Multimap<Set<String>, String> a2a1 =
                        TreeMultimap.create(LEX_ITERABLE_COMPARATOR, Comparator.naturalOrder());
                for (Entry<String, Collection<String>> entry : a1a2.asMap().entrySet()) {
                    a2a1.put(Sets.newTreeSet(entry.getValue()), entry.getKey());
                }

                for (Entry<Set<String>, Collection<String>> a2a1Sets : a2a1.asMap().entrySet()) {
                    TreeSet<String> a1s = Sets.newTreeSet(a2a1Sets.getValue());
                    Set<String> a2s = a2a1Sets.getKey();
                    Map<Integer, SortedSet<String>> midResult = new TreeMap<>();
                    midResult.put(delta.getAttributeNumbers().get(0), a1s);
                    midResult.put(delta.getAttributeNumbers().get(1), (SortedSet) a2s);
                    midResult.put(delta.getAttributeNumbers().get(2), (SortedSet) a3s);
                    midResult.put(delta.getAttributeNumbers().get(3), (SortedSet) a4s);
                    result.add(midResult);
                }
            }
        }

        return result;
    }

    /**
     * One of 4 methods to get grouping of attributes, such as: *
     *
     * <pre>
     *
     * [gregorian, format, wide]
     * [gregorian, format, narrow]
     * [generic, format, wide>]
     * [generic, format, narrow>]
     * [generic, stand-alone, wide>]
     * =>
     * [[gregorian, generic], [format], [wide, narrow]]
     * [[generic], [format,stand-alone], [wide]
     *
     * </pre>
     */
    private static List<Map<Integer, SortedSet<String>>> toAttributeRulesFor3(Delta delta) {
        List<Map<Integer, SortedSet<String>>> result = new ArrayList<>();

        Multimap2<String, String, String> map1 = Multimap2.create(TreeMap::new);
        delta.delta1.stream()
                .forEach(
                        x -> {
                            map1.put(x.get(0), x.get(1), x.get(2));
                        });

        // We want need to collect sets of correspondences, sets mapping to sets.
        // So we create a multimap from the value sets to the values
        Multimap<Set<String>, List<String>> map2 =
                TreeMultimap.create(LEX_ITERABLE_COMPARATOR, LEX_ITERABLE_COMPARATOR);

        for (String a1 : map1.keySet()) {
            for (String a2 : map1.keySet2(a1)) {
                Set<String> set =
                        map1.get(a1, a2); // find the set of a3 that this a1, a2 pairs share
                map2.put(set, List.of(a1, a2));
            }
        }
        // we have now collected all the a3 sets that share an a1a2Pair
        // we will further collect the a2s that share the same a1s
        for (Entry<Set<String>, Collection<List<String>>> a3sAndPairs : map2.asMap().entrySet()) {
            SortedSet<String> a3s = (SortedSet) a3sAndPairs.getKey();
            Multimap<String, String> a1a2 = TreeMultimap.create();
            Collection<List<String>> val = a3sAndPairs.getValue();
            for (List<String> a1a2Pairs : val) {
                a1a2.put(a1a2Pairs.get(0), a1a2Pairs.get(1));
            }
            // we now invert
            Multimap<Set<String>, String> a2a1 =
                    TreeMultimap.create(LEX_ITERABLE_COMPARATOR, Comparator.naturalOrder());
            for (Entry<String, Collection<String>> entry : a1a2.asMap().entrySet()) {
                a2a1.put(Sets.newTreeSet(entry.getValue()), entry.getKey());
            }
            for (Entry<Set<String>, Collection<String>> a2a1Sets : a2a1.asMap().entrySet()) {
                TreeSet<String> a1s = Sets.newTreeSet(a2a1Sets.getValue());
                SortedSet<String> a2s = (SortedSet) a2a1Sets.getKey();
                Map<Integer, SortedSet<String>> midResult = Maps.newLinkedHashMap();
                midResult.put(delta.getAttributeNumbers().get(0), a1s);
                midResult.put(delta.getAttributeNumbers().get(1), a2s);
                midResult.put(delta.getAttributeNumbers().get(2), a3s);
                result.add(midResult);
            }
        }
        return result;
    }

    /**
     * One of 4 methods to get grouping of attributes, such as: *
     *
     * <pre>
     *
     * [gregorian, format, wide]
     * [gregorian, format, narrow]
     * [generic, format, wide>]
     * [generic, format, narrow>]
     * [generic, stand-alone, wide>]
     * =>
     * [[gregorian, generic], [format], [wide, narrow]]
     * [[generic], [format,stand-alone], [wide]
     *
     * </pre>
     */
    private static List<Map<Integer, SortedSet<String>>> toAttributeRulesFor2(Delta delta) {
        List<Map<Integer, SortedSet<String>>> result = new ArrayList<>();

        Multimap<String, String> map1 = TreeMultimap.create();
        delta.delta1.stream()
                .forEach(
                        x -> {
                            map1.put(x.get(0), x.get(1));
                        });

        // We want need to collect sets of correspondences, sets mapping to sets.
        // So we create a multimap from the value sets to the values
        Multimap<Set<String>, String> map2 =
                TreeMultimap.create(LEX_ITERABLE_COMPARATOR, Comparator.naturalOrder());
        for (Entry<String, Collection<String>> entry : map1.asMap().entrySet()) {
            map2.put(Sets.newTreeSet(entry.getValue()), entry.getKey());
        }
        for (Entry<Set<String>, Collection<String>> entry : map2.asMap().entrySet()) {
            // at this point the first attributes are in the values
            //                        String attr1 =
            // makeItems(Sets.newTreeSet(entry.getValue()), variables);
            //                        String attr2 = makeItems(entry.getKey(),
            // variables);
            Map<Integer, SortedSet<String>> midResult = Maps.newTreeMap();
            midResult.put(delta.getAttributeNumbers().get(0), Sets.newTreeSet(entry.getValue()));
            midResult.put(delta.getAttributeNumbers().get(1), (SortedSet) entry.getKey());
            result.add(midResult);
        }
        return result;
    }

    /**
     * One of 4 methods to get grouping of attributes, such as: *
     *
     * <pre>
     *
     * [gregorian, format, wide]
     * [gregorian, format, narrow]
     * [generic, format, wide>]
     * [generic, format, narrow>]
     * [generic, stand-alone, wide>]
     * =>
     * [[gregorian, generic], [format], [wide, narrow]]
     * [[generic], [format,stand-alone], [wide]
     *
     * </pre>
     */
    private static List<Map<Integer, SortedSet<String>>> toAttributeRulesFor1(Delta delta) {
        SortedSet<String> items =
                delta.delta1.stream()
                        .map(x -> x.get(0))
                        .collect(Collectors.toCollection(TreeSet::new));
        Map<Integer, SortedSet<String>> midResult = new TreeMap<>();
        midResult.put(delta.getAttributeNumbers().get(0), items);
        return List.of(midResult);
    }

    // TODO: the coalesce functions are not being used.
    // The goal is to clean up and replace the various toAttributeRulesForN methods
    // with a cleaner implementation.
    // That could be done (especially since the functions can be tested against the existing ones
    // over all locales, but ran out of time.
    /** WIP */
    private static void checkCoalesce(Delta delta, List<UPair<Integer, SortedSet<String>>> result) {
        List<UPair<Integer, SortedSet<String>>> temp = coalesce(delta);
        if (!result.equals(temp)) {
            throw new IllegalArgumentException("Coalesce mismatch");
        }
    }

    /**
     * WIP The goal is to coalesce a list of lists of attributes into consistent groupings. Example:
     *
     * <p>These can then be turned into rules. The position of items in the list is handled at a
     * higher level.
     *
     * <p>there are many different ways to do that. For now we use a relatively simple mechanism; we
     * could optimize later.
     *
     * @param sourceListList
     * @return
     */
    private static List<UPair<Integer, SortedSet<String>>> coalesce(Delta delta) {
        /* delta
         * private final List<Integer> attributeNumbers;
         * private final Set<List<String>> delta1;
         */
        List<List<String>> list = Lists.newArrayList(delta.delta1);
        // Each List<Set<String>> contains a single row, like [[gregorian, generic], [format],
        // [wide, narrow]]
        List<List<SortedSet<String>>> fullList = coalesce2(list);

        List<UPair<Integer, SortedSet<String>>> result = Lists.newArrayList();
        for (List<SortedSet<String>> row : fullList) {
            for (int i = 0; i < row.size(); ++i) {
                result.add(UPair.of(delta.getAttributeNumbers().get(i), row.get(i)));
            }
        }
        return List.copyOf(result);
    }

    /**
     * WIP
     *
     * @param sourceListList Each List<Set<String>> contains a single row, like [[gregorian,
     *     generic], [format], [wide, narrow]] and we have a list of them.
     * @return
     */
    private static List<List<SortedSet<String>>> coalesce2(List<List<String>> sourceListList) {

        // If we only have 1, it is simple
        int size = sourceListList.iterator().next().size();
        if (size == 1) {
            TreeSet<String> unionSet = new TreeSet<>();

            for (List<String> currentSet : sourceListList) {
                unionSet.addAll(currentSet); // Adds all unique elements in sorted order
            }

            return List.of(List.of(unionSet));
        }

        List<List<SortedSet<String>>> result = new ArrayList<>();
        // we gather all the similar firsts
        Multimap<List<String>, String> groupFirst =
                TreeMultimap.create(LEX_ITERABLE_COMPARATOR, Comparator.naturalOrder());
        for (List<String> list : sourceListList) {
            List<String> allButFirst = list.subList(1, list.size());
            groupFirst.put(allButFirst, list.iterator().next());
        }
        // groupFirst.entries now looks like
        // [format, wide] -> [gregorian, generic]
        // [format, narrow] -> [gregorian, generic]
        // [stand-alone, wide] -> [generic]

        // Because it is a multimap, we can get the identical *sets* of values out.
        // We now invert on the *sets* of values, sorting by the Set<Strings> in the value
        Multimap<SortedSet<String>, List<String>> invert =
                TreeMultimap.create(LEX_ITERABLE_COMPARATOR, LEX_ITERABLE_COMPARATOR);
        for (Entry<List<String>, Collection<String>> entry : groupFirst.asMap().entrySet()) {
            invert.put((SortedSet<String>) entry.getValue(), entry.getKey());
        }
        // invert.asMap now looks like
        // [gregorian, generic] -> [[format, wide],[format, narrow]]
        // [generic] -> [stand-alone, wide]

        // We now extract the keys and values, and recurse

        for (Entry<SortedSet<String>, Collection<List<String>>> entry : invert.asMap().entrySet()) {
            SortedSet<String> key = entry.getKey();
            List<List<String>> value = List.copyOf(entry.getValue());
            List<List<SortedSet<String>>> recurse = coalesce2(value);
            for (List<SortedSet<String>> item : recurse) {
                List<SortedSet<String>> temp = new ArrayList<>(item);
                temp.add(0, key);
                result.add(temp);
            }
        }

        return result;
    }

    /**
     * Contains the result of comparing the attribute lists at one coverage level with all those
     * above it. All the lists have the same number of attribute values. The attributeNumbers are in
     * parallel, and map from the list indices back to the original attribute numbers. TODO Convert
     * into two arguments, each a Multimap<Integer,List<String>>
     */
    private static class Delta {
        private final List<Integer> attributeNumbers;
        private final Set<List<String>> delta1;
        private final Set<List<String>> delta2;

        public List<Integer> getAttributeNumbers() {
            return attributeNumbers;
        }

        public Set<List<String>> getDelta1() {
            return delta1;
        }

        public Set<List<String>> getDelta2() {
            return delta2;
        }

        public Delta(
                List<Integer> attributeNumbers,
                Set<List<String>> delta1,
                Set<List<String>> delta2) {
            this.attributeNumbers = attributeNumbers;
            this.delta1 = delta1;
            this.delta2 = delta2;
        }

        public Delta(Integer attrNum, Set<String> set1, Set<String> set2) {
            this(
                    List.of(attrNum),
                    (Set<List<String>>)
                            set1.stream()
                                    .map(x -> List.of(x))
                                    .collect(Collectors.toCollection(LinkedHashSet::new)),
                    (Set<List<String>>)
                            set2.stream()
                                    .map(x -> List.of(x))
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        @Override
        public String toString() {
            return Joiners.TAB.join(attributeNumbers, delta1, delta2);
        }

        @Override
        public boolean equals(Object obj) {
            Delta other = (Delta) obj;
            return Objects.equal(attributeNumbers, other.attributeNumbers)
                    && Objects.equal(delta1, other.delta1)
                    && Objects.equal(delta2, other.delta2);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(attributeNumbers, delta1, delta2);
        }
    }
}
