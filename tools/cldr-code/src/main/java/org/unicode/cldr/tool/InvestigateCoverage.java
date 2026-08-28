package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
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
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMultimap2;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UPair;

public class InvestigateCoverage {
    private static final boolean DEBUG = XCoverageLevel.DEBUG;
    private static Set<String> TEST_PATHS = XCoverageLevel.TEST_PATHS;
    private static final boolean SHOW_PROGRESS = false;

    private static final String OUTPUT_DIR = CLDRPaths.GEN_DIRECTORY + "coverage";
    private static final String ATTR_PREFIX = "attr";
    private static final String LEVEL_PREFIX = "level=";
    private static final String FINAL_LEVEL_PREFIX = "finalLevel=";
    private static final String PATH_PREFIX = "path=";
    private static final int MAX_REGEX_LEN = 60;
    private static final boolean SHORT_RUN = true;
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();
    private static final Comparator<Iterable<String>> LEX_ITERABLE_COMPARATOR =
            Ordering.natural().lexicographical();

    public static void main(String[] args) {

        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        char lastChar = 0;
        Set<String> localesToCheck =
                SHORT_RUN
                        ? Set.of("en")
                        : StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);

        for (String locale : localesToCheck) {
            char currChar = locale.charAt(0);
            if (lastChar != currChar) {
                System.out.println(locale);
                lastChar = currChar;
            }

            createFile(locale, outputDir);
            checkFile(locale);
        }
    }

    public static void checkFile(String locale) {
        Path filepath = Paths.get(OUTPUT_DIR, locale + ".txt");
        XCoverageLevel xCoverage = XCoverageLevel.fromFile(filepath);
        if (DEBUG) {
            TEST_PATHS.stream().forEach(x -> xCoverage.getPathData(x));
        }

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        Multimap<Boolean, String> okVsNot = LinkedHashMultimap.create();

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias")) {
                continue;
            }
            Level realLevel = SDI.getCoverageLevel(path, locale);
            Level xLevel = xCoverage.getCoverage(path);
            if (realLevel == xLevel) {
                okVsNot.put(true, SplitPath.from(path).getChassis());
            } else {
                okVsNot.put(false, SplitPath.from(path).getChassis());
                xLevel = xCoverage.getCoverage(path);
            }
        }
        System.out.println("\nChecked against orginal:");
        System.out.println(Joiners.TAB.join("ok:", okVsNot.get(true)));
        System.out.println(Joiners.TAB.join("fail:", okVsNot.get(false)));
        okVsNot.get(false).stream().forEach(System.out::println);
    }

    private static void createFile(String locale, File outputDir) {
        File newFile = new File(outputDir, locale + ".txt");
        List<String> ruleList = new ArrayList<>();

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);

        Multimap2<String, Level, List<String>> _chassisToLevelToAttributeList =
                Multimap2.create(TreeMap::new, TreeMap::new, LinkedHashMap::new);

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias")) {
                continue;
            }
            Level level = SDI.getCoverageLevel(path, locale);
            SplitPath splitPath = SplitPath.from(path);
            List<String> attributes = splitPath.getAttributeValues();
            _chassisToLevelToAttributeList.put(splitPath.getChassis(), level, attributes);
        }
        ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList =
                _chassisToLevelToAttributeList.createImmutable();

        SortedSet<String> sortedScaffolds =
                ImmutableSortedSet.copyOf(chassisToLevelToAttributeList.keySet());
        if (DEBUG) {
            TEST_PATHS.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x
                                                    + "\n\t"
                                                    + chassisToLevelToAttributeList.getMapMap(
                                                            x)));
        }
        if (DEBUG && SHOW_PROGRESS) {
            System.out.println("Raw scaffolds\n" + Joiners.N.join(sortedScaffolds));
        }

        if (DEBUG && SHOW_PROGRESS) {
            System.out.println("\nMinimized");
        }

        Variables variableToValue = new Variables();

        for (String chassis : sortedScaffolds) {
            SortedSet<Level> levelSet =
                    ImmutableSortedSet.copyOf(chassisToLevelToAttributeList.keySet2(chassis));
            minimizeAttributes(
                    chassisToLevelToAttributeList,
                    variableToValue,
                    chassis,
                    levelSet,
                    ruleList);
        }
        try (PrintStream out = new PrintStream(newFile)) {

            // get inverted map of variables
            out.println("# Variables");
            for (String variable : variableToValue.getVariables()) {
                out.println(variable + "=" + variableToValue.getValue(variable));
            }
            out.println("\n# Rules");
            ruleList.stream().forEach(out::println);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class Variables {
        Map<String, String> valueToVariable = new TreeMap<>();
        Map<String, String> variableToValue = new LinkedHashMap<>();

        String add(String value) {
            String result = valueToVariable.get(value);
            if (result == null) {
                result = "%V" + String.format("%03d", valueToVariable.size());
                valueToVariable.put(value, result);
                variableToValue.put(result, value);
            }
            return result;
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
            ImmutableMultimap2<String, Level, List<String>> chassisToLevelToAttributeList,
            Variables variableToValue,
            String chassis,
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

        boolean internal = false;

        Set<Level> levelSetMinusLast = Sets.newTreeSet(levelSet);
        levelSetMinusLast.remove(Iterables.getLast(levelSet));

        // get the levels we need to distinguish
        Set<Pair<Level, Level>> levelPairs = new LinkedHashSet<>();
        for (Level level1 : levelSet) {
            for (Level level2 : levelSet) {
                if (level1.compareTo(level2) >= 0) {
                    continue; // only different pairs
                }
                levelPairs.add(Pair.of(level1, level2));
            }
        }
        levelPairs = ImmutableSet.copyOf(levelPairs);

        // first step is to see if there is a single attribute that distinguishes all the levels
        // we first get maps from the attribute number to the level to the attribute.

        // example, if basic iff {{1,gregorian, 1,generic}}, we have distinguished basic from other
        // levels
        Multimap2<Integer, Level, String> attrNumTolevelToAttribute =
                Multimap2.create(TreeMap::new);
        for (Level level : levelSet) {
            Set<List<String>> attributeLists =
                    chassisToLevelToAttributeList.get(chassis, level);
            for (List<String> attributeList : attributeLists) {
                for (int i = 0; i < attributeList.size(); ++i) {
                    attrNumTolevelToAttribute.put(i, level, attributeList.get(i));
                }
            }
        }
        if (DEBUG && debugPath) {
            System.out.println("attrNumTolevelToAttribute");
            attrNumTolevelToAttribute.stream().forEach(x -> System.out.println(x.getKey1() + ", " + x.getKey2() + ", " + x.getValue()));
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
            distinguishes.entrySet().stream().forEach(x -> System.out.println(x.getKey() + ", " + x.getValue()));
            int debug = 0;
        }

        // find missing
        boolean succeeds = levelSetMinusLast.equals(distinguishes.keySet());

        if (succeeds) {
            showPathRules(
                    variableToValue,
                    chassis,
                    levelSet,
                    internal,
                    levelSetMinusLast,
                    distinguishes,
                    ruleList);
            //            boolean first = true;
            //            for (Level level : levelSetMinusLast) {
            //                if (first) {
            //                    System.out.println(PATH_PREFIX + chassis + "\t// " + levelSet);
            //                }
            //                System.out.println(LEVEL_PREFIX + level);
            //                toAttributeRules(distinguishes.get(level),
            // variableToValue).stream().forEach(x -> System.out.println(ATTR_PREFIX
            //                    + x.first
            //                    + "="
            //                    + makeItems(x.second, variableToValue)));
            //                first = false;
            //            }
            //            System.out.println(FINAL_LEVEL_PREFIX + Iterables.getLast(levelSet,
            // null));

        } else {
            Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                    getAttributeNumToLevelToAttributesCombinations(
                            chassisToLevelToAttributeList, 2, chassis, levelSet, distinguishes);
            if (DEBUG && debugPath) {
                System.out.println("\nChecking 2 attributes: " + chassis);
            }

            findDistinguishing(levelSet, levelSetMinusLast, attrNumTolevelToAttribute2, distinguishes);

            if (DEBUG && debugPath) {
                System.out.println("DISTINGUISHING: " + chassis);
                distinguishes.entrySet().stream().forEach(System.out::println);
            }
            if (levelSetMinusLast.equals(distinguishes.keySet())) {
                showPathRules(
                        variableToValue,
                        chassis,
                        levelSet,
                        internal,
                        levelSetMinusLast,
                        distinguishes,
                        ruleList);
            } else {
                if (DEBUG && debugPath) {
                    System.out.println("FAILS2");
                    System.out.println(distinguishes);
                }
                ruleList.add("#FAILS\t" + PATH_PREFIX + chassis + "\t// " + distinguishes);
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
        distinguishes.clear();
        // try for pairs
        Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                Multimap2.create(HashMap::new);
        for (Level level : levelSet) {
            Set<List<String>> attributeLists =
                    chassisToLevelToAttributeList.get(chassis, level);
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

    private static void showPathRules(
            Variables variableToValue,
            String chassis,
            SortedSet<Level> levelSet,
            boolean internal,
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
            toAttributeRules(distinguishingAttributes, variableToValue).stream()
                    .forEach(
                            x ->
                                    ruleList.add(
                                            ATTR_PREFIX
                                                    + x.first
                                                    + "="
                                                    + makeItems(x.second, variableToValue)));
        }
        ruleList.add(FINAL_LEVEL_PREFIX + Iterables.getLast(levelSet, null));
    }

    private static void findDistinguishing(
            Set<Level> levelSet,
            Set<Level> levelSetMinusLast, Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2,
            Map<Level, Delta> distinguishes) {
        for (List<Integer> attrNum : attrNumTolevelToAttribute2.keySet()) {
            for (Level level1 : levelSetMinusLast) {
                if (distinguishes.containsKey(level1)) {
                    continue; // already done
                }

                Set<List<String>> atLevel1 = attrNumTolevelToAttribute2.get(attrNum, level1);

                // These are all the sets ABOVE level1
                Set<List<String>> set2 = Sets.newHashSet();
                for (Level level2 : levelSet) {
                    if (level2.compareTo(level1) <= 0) {
                        continue;
                    }
                    set2.addAll(attrNumTolevelToAttribute2.get(attrNum, level2));
                }
                if (Collections.disjoint(atLevel1, set2)) {
                    // level1 is distinguished from level2 AND ABOVE by set1
                    distinguishes.put(level1, new Delta(attrNum, atLevel1, set2));
                } else {
                    int debug = 0;
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

    private static List<UPair<Integer, SortedSet<String>>> toAttributeRules(
            Delta delta, Variables variables) {
        // for now, we just test the first one
        // later, we can test for the inverse of the last one

        // Cases: a-b, a-c => attr1=a, attr2=b|c
        // Cases: a-b, c-b => attr1=a|c, attr2=b
        // Cases: a-b, c-d => attr1=a, attr2=b ; attr1=c, attr2=d
        switch (delta.attributeNumbers.size()) {
            default:
                throw new UnsupportedOperationException();
            case 1:
                {
                    SortedSet<String> items =
                            delta.delta1.stream()
                                    .map(x -> x.get(0))
                                    .collect(Collectors.toCollection(TreeSet::new));
                    return List.of(UPair.of(delta.attributeNumbers.get(0), items));
                    // UPair.of(delta.attributeNumbers.get(0), makeItems(items, variables)));
                }
            case 2:
                {
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
                    List<UPair<Integer, SortedSet<String>>> result = new ArrayList<>();
                    for (Entry<Set<String>, Collection<String>> entry : map2.asMap().entrySet()) {
                        // at this point the first attributes are in the values
                        //                        String attr1 =
                        // makeItems(Sets.newTreeSet(entry.getValue()), variables);
                        //                        String attr2 = makeItems(entry.getKey(),
                        // variables);
                        result.add(
                                UPair.of(
                                        delta.attributeNumbers.get(0),
                                        Sets.newTreeSet(entry.getValue())));
                        result.add(
                                UPair.of(
                                        delta.attributeNumbers.get(1), (SortedSet) entry.getKey()));
                    }
                    return result;
                }
        }
    }

    private static String makeItems(Collection<String> items, Variables variables) {
        String result = Joiners.VBAR.join(items);
        if (result.length() > MAX_REGEX_LEN) {
            return variables.add(result);
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
