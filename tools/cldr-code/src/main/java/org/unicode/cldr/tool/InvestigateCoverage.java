package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMultimap2;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.NestedMap.Multimap3;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UPair;

/** Prototype version of code to generate simple-to-parse coverage files for locales. */
public class InvestigateCoverage {
    private static final String SSV_FILE_SUFFIX = ".ssv";
    private static final boolean DEBUG = XCoverageLevel.DEBUG;
    private static Set<String> TEST_PATHS = XCoverageLevel.TEST_PATHS;
    private static final boolean SHOW_PROGRESS = false;

    private enum Run {
        tiny,
        tc,
        all
    }

    private static final String OUTPUT_DIR = CLDRPaths.COMMON_DIRECTORY + "pathCoverage";
    // CLDRPaths.GEN_DIRECTORY + "coverage";
    private static final String ATTR_PREFIX = "attr";
    private static final String LEVEL_PREFIX = "level=";
    private static final String FINAL_LEVEL_PREFIX = "finalLevel=";
    private static final String PATH_PREFIX = "path=";
    private static final int MAX_REGEX_COUNT = 31;
    private static final Run SHORT_RUN = Run.tc;
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();
    private static final Comparator<Iterable<String>> LEX_ITERABLE_COMPARATOR =
            Ordering.natural().lexicographical();
    private static final Supplier<Map<Object, Object>> TREEMAP_LEXICAL =
            () -> new TreeMap(LEX_ITERABLE_COMPARATOR);

    public static void main(String[] args) {

        Variables variableToValue = new Variables();

        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        char lastChar = 0;
        Set<String> localesToCheck =
                SHORT_RUN == Run.tiny
                        ? Set.of("en", "root", "de", "ja")
                        : SHORT_RUN == Run.tc
                                ? StandardCodes.make().getLocaleCoverageLocales(Organization.cldr)
                                : CLDR_FACTORY.getAvailable();

        for (String locale : localesToCheck) {
            char currChar = locale.charAt(0);
            if (lastChar != currChar) {
                System.out.println(locale);
                lastChar = currChar;
            }

            createFile(locale, outputDir, variableToValue);
            checkFile(locale);
        }

        File newFile = new File(outputDir, "variables.txt");

        try (PrintStream out = new PrintStream(newFile)) {

            // get inverted map of variables
            out.println("# Variables");
            for (String variable : variableToValue.getVariables()) {
                out.println(variable + "=" + variableToValue.getValue(variable));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void checkFile(String locale) {
        Path filepath = Paths.get(OUTPUT_DIR, locale + SSV_FILE_SUFFIX);
        XCoverageLevel xCoverage = XCoverageLevel.fromFile(filepath);
        if (DEBUG) {
            TEST_PATHS.stream().forEach(x -> xCoverage.getPathData(x));
        }

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        Multimap<Boolean, String> okVsNot = LinkedHashMultimap.create();

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            Level realLevel = SDI.getCoverageLevel(path, locale);
            Level xLevel = xCoverage.getCoverage(path);
            if (realLevel == xLevel) {
                okVsNot.put(true, SplitPath.from(path).getChassis());
            } else {
                okVsNot.put(false, SplitPath.from(path).getChassis());
                xLevel = xCoverage.getCoverage(path); // for debugging
            }
        }
        if (!okVsNot.get(false).isEmpty()) {
            System.out.println("\nChecked against orginal:");
            System.out.println(Joiners.TAB.join("ok:", okVsNot.get(true)));
            System.out.println(Joiners.TAB.join("fail:", okVsNot.get(false)));
            okVsNot.get(false).stream().forEach(System.out::println);
        }
    }

    private static void createFile(String locale, File outputDir, Variables allVariables) {
        allVariables.clearVariablesInCurrentFile();
        File newFile = new File(outputDir, locale + SSV_FILE_SUFFIX);
        List<String> ruleList = new ArrayList<>();

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);

        Multimap2<String, Level, List<String>> _chassisToLevelToAttributeList =
                Multimap2.create(TreeMap::new, TreeMap::new, LinkedHashMap::new);

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            Level level = SDI.getCoverageLevel(path, locale);
            SplitPath splitPath = SplitPath.from(path);
            List<String> attributes = splitPath.getAttributeValues();
            _chassisToLevelToAttributeList.put(splitPath.getChassis(), level, attributes);
        }
        ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList =
                _chassisToLevelToAttributeList.createImmutable();

        SortedSet<String> sortedChassises =
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
            System.out.println("Raw Chassises\n" + Joiners.N.join(sortedChassises));
        }

        if (DEBUG && SHOW_PROGRESS) {
            System.out.println("\nMinimized");
        }

        for (String chassis : sortedChassises) {
            SortedSet<Level> levelSet =
                    ImmutableSortedSet.copyOf(chassisToLevelToAttributeList.keySet2(chassis));
            minimizeAttributes(
                    chassis, chassisToLevelToAttributeList, allVariables, levelSet, ruleList);
        }
        try (PrintStream out = new PrintStream(newFile)) {
            out.println(
                    "# DRAFT data for coverage. For the file format, see the readme.md in this directory.");

            // get inverted map of variables
            out.println("# Variables");
            for (String variable : allVariables.getVariablesInCurrentFile()) {
                out.println(variable + "=" + allVariables.getValue(variable));
            }
            out.println("\n# Rules");
            ruleList.stream().forEach(out::println);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
        String add(String value, String chassis, UPair<Integer, SortedSet<String>> x) {
            String variableName = valueToVariable.get(value);
            if (variableName == null) {
                String element = SplitPath.findElementForAttribute(chassis, x.first);
                String base =
                        "%"
                                + element
                                + x.second.size(); // String.format("%03d", valueToVariable.size());
                // make sure it is unique
                for (char i = 'a'; ; ++i) {
                    variableName = base + (i == 'a' ? "" : i);
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

        String getValue(String variable) {
            return variableToValue.get(variable);
        }

        Set<String> getVariables() {
            return variableToValue.keySet();
        }

        // TODO fix so it doesn't collide with abov

        public void add(String variable, String value) {
            valueToVariable.put(value, variable);
            variableToValue.put(variable, value);
        }
    }

    private static void minimizeAttributes(
            String chassis,
            ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList,
            Variables variableToValue,
            SortedSet<Level> levelSet,
            List<String> ruleList) {

        boolean debugPath = TEST_PATHS.contains(chassis);
        if (DEBUG && debugPath) {
            show(chassis, chassisToLevelToAttributeList);
        }

        if (levelSet.size() == 1) {
            // everything is at the same level, no need to do any work!
            ruleList.add(PATH_PREFIX + chassis); //  + "\n#\t" + levelSet);
            if (DEBUG && (SHOW_PROGRESS || debugPath)) {
                System.out.println(chassis + "\n\t" + levelSet);
            }
            ruleList.add(FINAL_LEVEL_PREFIX + levelSet.iterator().next());
            return;
        }

        Set<Level> levelSetMinusLast = Sets.newTreeSet(levelSet);
        levelSetMinusLast.remove(Iterables.getLast(levelSet));

        Set<List<String>> firstAttributeSetList =
                chassisToLevelToAttributeList.get(chassis, levelSet.iterator().next());
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
        if (DEBUG && debugPath) {
            System.out.println("attrNumTolevelToAttribute");
            attrNumTolevelToAttribute.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x.getKey1()
                                                    + ", "
                                                    + x.getKey2()
                                                    + ", "
                                                    + x.getValue()));
            System.out.println("\nChecking Single attribute");
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
                    if (DEBUG && debugPath) {
                        System.out.println("FAILS1");
                        System.out.println(level1 + "\t" + atLevel1);
                        System.out.println("REST" + "\t" + aboveLevel1);
                    }
                    int debug = 0;
                }
            }
        }
        // sometimes what distinguishes (eg) basic from moderate is the same as what distinguishes
        // basic from modern.
        // so we need to combine them

        if (DEBUG && debugPath) {
            System.out.println("DISTINGUISHING: " + chassis);
            distinguishes.entrySet().stream()
                    .forEach(x -> System.out.println(x.getKey() + ", " + x.getValue()));
            int debug = 0;
        }

        // find missing
        boolean succeeds = levelSetMinusLast.equals(distinguishes.keySet());

        if (succeeds) {
            showPathRules(
                    variableToValue, chassis, levelSet, levelSetMinusLast, distinguishes, ruleList);
            return;
        }

        // 1 attribute didn't work. Try more
        for (int count = 2; count <= attributeCount && count < 5; ++count) {
            Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                    getAttributeNumToLevelToAttributesCombinations2(
                            chassisToLevelToAttributeList, count, chassis, levelSet);
            if (DEBUG && debugPath) {
                System.out.println("\nChecking " + count + " attributes: " + chassis);
            }

            findDistinguishing(
                    chassis,
                    levelSet,
                    levelSetMinusLast,
                    attrNumTolevelToAttribute2,
                    distinguishes);

            if (DEBUG && debugPath) {
                System.out.println("DISTINGUISHING: " + chassis);
                distinguishes.entrySet().stream().forEach(System.out::println);
            }
            if (levelSetMinusLast.equals(distinguishes.keySet())) {
                showPathRules(
                        variableToValue,
                        chassis,
                        levelSet,
                        levelSetMinusLast,
                        distinguishes,
                        ruleList);
                return;
            } else {
                if (DEBUG && debugPath) {
                    System.out.println("FAILS" + count);
                    System.out.println(distinguishes);
                }
            }
        }
    }

    private static void show(
            String chassis,
            ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList) {
        System.out.println("BASIC: " + chassis);
        Map<Level, Map<List<String>, Boolean>> foo =
                chassisToLevelToAttributeList.getMapMap(chassis);
        for (Entry<Level, Map<List<String>, Boolean>> entry : foo.entrySet()) {
            System.out.println("level=" + entry.getKey());
            entry.getValue().keySet().stream().forEach(System.out::println);
        }
    }

    private static Multimap2<List<Integer>, Level, List<String>>
            getAttributeNumToLevelToAttributesCombinations(
                    Multimap2<String, Level, List<String>> chassisToLevelToAttributeList,
                    int count,
                    String chassis,
                    SortedSet<Level> levelSet,
                    Map<Level, Delta> distinguishes) {
        // try for pairs
        Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                Multimap2.create(TREEMAP_LEXICAL, TreeMap::new, HashMap::new);
        for (Level level : levelSet) {
            Set<List<String>> attributeLists = chassisToLevelToAttributeList.get(chassis, level);
            for (List<String> attributeList : attributeLists) {
                for (int i = 0; i < attributeList.size(); ++i) {
                    for (int j = i + 1; j < attributeList.size(); ++j) {
                        attrNumTolevelToAttribute2.put(
                                List.of(i, j),
                                level,
                                List.of(attributeList.get(i), attributeList.get(j)));
                    }
                }
            }
        }
        return attrNumTolevelToAttribute2;
    }

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

    private static void showPathRules(
            Variables variableToValue,
            String chassis,
            SortedSet<Level> levelSet,
            Set<Level> levelSetMinusLast,
            Map<Level, Delta> distinguishes,
            List<String> ruleList) {
        boolean first = true;
        for (Level level : levelSetMinusLast) {
            if (first) {
                ruleList.add(PATH_PREFIX + chassis); //  + "\n#\t" + levelSet);
                if (DEBUG && TEST_PATHS.contains(chassis)) {
                    System.out.println(chassis);
                }
                first = false;
            }
            ruleList.add(LEVEL_PREFIX + level);
            Delta distinguishingAttributes = distinguishes.get(level);
            // Cases: a-b, a-c => attr1=a, attr2=b|c
            // Cases: a-b, c-b => attr1=a|c, attr2=b
            // Cases: a-b, c-d => attr1=a, attr2=b ; attr1=c, attr2=d
            List<UPair<Integer, SortedSet<String>>> attributeRules;
            switch (distinguishingAttributes.attributeNumbers.size()) {
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
            attributeRules.stream()
                    .forEach(
                            x ->
                                    ruleList.add(
                                            ATTR_PREFIX
                                                    + x.first
                                                    + "="
                                                    + makeItems(chassis, x, variableToValue)));
        }
        ruleList.add(FINAL_LEVEL_PREFIX + Iterables.getLast(levelSet, null));
    }

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
        /*
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
                    if (DEBUG && debugPath) {
                        System.out.println("FAILS1");
                        System.out.println(level1 + "\t" + atLevel1);
                        System.out.println("REST" + "\t" + aboveLevel1);
                    }
                    int debug = 0;
                }
            }
        }

         */
    }

    private static void checkCoalesce(Delta delta, List<UPair<Integer, SortedSet<String>>> result) {
        List<UPair<Integer, SortedSet<String>>> temp = coalesce(delta);
        if (!result.equals(temp)) {
            int debug = 0;
            temp = coalesce(delta);
        }
    }

    private static List<UPair<Integer, SortedSet<String>>> toAttributeRulesFor4(Delta delta) {
        List<UPair<Integer, SortedSet<String>>> result = new ArrayList<>();
        Multimap3<String, String, String, String> map1 = Multimap3.create(TreeMap::new);
        delta.delta1.stream()
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
                    result.add(UPair.of(delta.attributeNumbers.get(0), a1s));
                    result.add(UPair.of(delta.attributeNumbers.get(1), (SortedSet) a2s));
                    result.add(UPair.of(delta.attributeNumbers.get(2), (SortedSet) a3s));
                    result.add(UPair.of(delta.attributeNumbers.get(3), (SortedSet) a4s));
                }
            }
        }

        return result;
    }

    private static List<UPair<Integer, SortedSet<String>>> toAttributeRulesFor3(Delta delta) {
        List<UPair<Integer, SortedSet<String>>> result = new ArrayList<>();
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
                result.add(UPair.of(delta.attributeNumbers.get(0), a1s));
                result.add(UPair.of(delta.attributeNumbers.get(1), (SortedSet) a2s));
                result.add(UPair.of(delta.attributeNumbers.get(2), (SortedSet) a3s));
            }
        }
        return result;
    }

    private static List<UPair<Integer, SortedSet<String>>> toAttributeRulesFor2(Delta delta) {
        List<UPair<Integer, SortedSet<String>>> result = new ArrayList<>();
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
            result.add(UPair.of(delta.attributeNumbers.get(0), Sets.newTreeSet(entry.getValue())));
            result.add(UPair.of(delta.attributeNumbers.get(1), (SortedSet) entry.getKey()));
        }
        return result;
    }

    // TODO: the coalesce functions are not being used; the goal is to clean up and replace
    // the various toAttributeRulesForN methods with a cleaner implementation.
    /**
     * The goal is to coalesce a list of lists of attributes into consistent groupings. Example:
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
     *
     * These can then be turned into regular expressions. The position of items in the list is
     * handled at a higher level.
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
                result.add(UPair.of(delta.attributeNumbers.get(i), row.get(i)));
            }
        }
        return List.copyOf(result);
    }

    /**
     * @param sourceListList Each List<Set<String>> contains a single row, like [[gregorian,
     *     generic], [format], [wide, narrow]] and we have a list of them.
     * @return
     */
    private static List<List<SortedSet<String>>> coalesce2(List<List<String>> sourceListList) {

        // If we only have 1, it is simple
        int size = sourceListList.getFirst().size();
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
            groupFirst.put(allButFirst, list.getFirst());
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

    private static List<UPair<Integer, SortedSet<String>>> toAttributeRulesFor1(Delta delta) {
        SortedSet<String> items =
                delta.delta1.stream()
                        .map(x -> x.get(0))
                        .collect(Collectors.toCollection(TreeSet::new));
        return List.of(UPair.of(delta.attributeNumbers.get(0), items));
        // UPair.of(delta.attributeNumbers.get(0), makeItems(items, variables)));
    }

    private static UPair<Integer, SortedSet<String>> pairUp(
            Integer attNum, SortedSet<String> setFor) {
        return UPair.of(attNum, setFor);
    }

    private static String makeItems(
            String chassis, UPair<Integer, SortedSet<String>> x, Variables variables) {
        String result = Joiners.VBAR.join(x.second);
        if (result.length() > MAX_REGEX_COUNT) {
            return variables.add(result, chassis, x);
        }
        return result;
    }

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
