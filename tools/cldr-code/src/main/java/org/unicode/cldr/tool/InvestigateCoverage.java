package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
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
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UPair;

public class InvestigateCoverage {
    private static final boolean DEBUG = true;

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

        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        CoverageLevel2 coverage = SDI.getCoverageLevelInfo(locale);

        Map<Pair<Level, Level>, String> failures = Maps.newLinkedHashMap();

        for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
            if (path.endsWith("/alias")) {
                continue;
            }
            Level realLevel = coverage.getLevel(path);
            Level xLevel = xCoverage.getCoverage(path);
            if (realLevel != xLevel) {
                System.out.println(Joiners.TAB.join(realLevel, xLevel, path));
                // failures.put(Pair.of(realLevel, xLevel), path);
            }
        }
    }

    private static void createFile(String locale, File outputDir) {
        File newFile = new File(outputDir, locale + ".txt");
        try (PrintStream out = new PrintStream(newFile)) {

            CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
            CoverageLevel2 coverage = SDI.getCoverageLevelInfo(locale);

            Multimap2<String, Level, List<String>> pathFrameToLevelToAttributeList =
                    Multimap2.create(HashMap::new);

            for (String path : Sets.newTreeSet(cldrFile.fullIterable())) {
                if (path.endsWith("/alias")) {
                    continue;
                }
                Level level = coverage.getLevel(path);
                SplitPath splitPath = SplitPath.from(path);
                List<String> attributes = splitPath.getAttributeValues();
                pathFrameToLevelToAttributeList.put(
                        splitPath.getScaffold(), level, attributes);
            }
            pathFrameToLevelToAttributeList = pathFrameToLevelToAttributeList.createImmutable();
            SortedSet<String> sortedScaffolds = ImmutableSortedSet.copyOf(pathFrameToLevelToAttributeList.keySet());
            if (DEBUG) {
                System.out.println("Raw scaffolds\n" + Joiners.N.join(sortedScaffolds));
            }

            if (DEBUG) {
                System.out.println("\nMinimized");
            }

            Variables variableToValue = new Variables();

            for (String pathFrame : sortedScaffolds) {
                SortedSet<Level> levelSet =
                        ImmutableSortedSet.copyOf(
                                pathFrameToLevelToAttributeList.keySet2(pathFrame));
                minimizeAttributes(
                        pathFrameToLevelToAttributeList, variableToValue, pathFrame, levelSet, out);
            }

            // get inverted map
            out.println("#Variables");
            for (String variable : variableToValue.getVariables()) {
                out.println(variable + "=" + variableToValue.getValue(variable));
            }
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
    
    static Set<String> testPaths =
        ImmutableSet.of(
            "//ldml/dates/calendars/calendar[@type]/dayPeriods/dayPeriodContext[@type]/dayPeriodWidth[@type]/dayPeriod[@type]"
                );

    private static void minimizeAttributes(
            Multimap2<String, Level, List<String>> pathFrameToLevelToAttributeList,
            Variables variableToValue,
            String pathFrame,
            SortedSet<Level> levelSet,
            PrintStream printStream) {
        
        boolean debugPath = testPaths.contains(pathFrame);
        if (debugPath) {
            System.out.println(pathFrame);
            int debug = 0;
        }

        if (levelSet.size() == 1) {
            // everything is at the same level, no need to do any work!
            printStream.println(PATH_PREFIX + pathFrame); //  + "\n#\t" + levelSet);
            if (DEBUG) {
                System.out.println(pathFrame);
            }
            printStream.println(FINAL_LEVEL_PREFIX + levelSet.iterator().next());
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
                    pathFrameToLevelToAttributeList.get(pathFrame, level);
            for (List<String> attributeList : attributeLists) {
                for (int i = 0; i < attributeList.size(); ++i) {
                    attrNumTolevelToAttribute.put(i, level, attributeList.get(i));
                }
            }
        }
        if (debugPath) {
            attrNumTolevelToAttribute.stream().forEach(System.out::println);
        }
        // if the attributeNumber & attribute only maps to a single level, we have enough
        // information to distinguish that level

        Map<Level, Delta> distinguishes = Maps.newTreeMap();
        for (Integer attrNum : attrNumTolevelToAttribute.keySet()) {
            for (Level level1 : levelSetMinusLast) {
                Set<String> set1 = attrNumTolevelToAttribute.get(attrNum, level1);
                Set<String> set2 = Sets.newTreeSet();
                for (Level level2 : levelSetMinusLast) {
                    if (level2.compareTo(level1) <= 0) {
                        continue;
                    }
                    set2.addAll(attrNumTolevelToAttribute.get(attrNum, level2));
                }
                if (Collections.disjoint(set1, set2)) {
                    // level1 is distinguished from level2 by set1
                    distinguishes.put(level1, new Delta(attrNum, set1, set2));
                } else {
                    if (debugPath) {
                        System.out.println(level1 + "\t" + set1);
                        System.out.println("REST" + "\t" + set2);
                    }
                    int debug = 0;
                }
            }
        }
        // sometimes what distinguishes (eg) basic from moderate is the same as what distinguishes
        // basic from modern.
        // so we need to combine them

        if (debugPath) {
            distinguishes.entrySet().stream().forEach(System.out::println);
            int debug = 0;
        }

        // find missing
        boolean succeeds = levelSetMinusLast.equals(distinguishes.keySet());

        if (succeeds) {
            showPathRules(
                    variableToValue,
                    pathFrame,
                    levelSet,
                    internal,
                    levelSetMinusLast,
                    distinguishes,
                    printStream);
            //            boolean first = true;
            //            for (Level level : levelSetMinusLast) {
            //                if (first) {
            //                    System.out.println(PATH_PREFIX + pathFrame + "\t// " + levelSet);
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
                            pathFrameToLevelToAttributeList, 2, pathFrame, levelSet, distinguishes);

            findDistinguishing(levelSetMinusLast, attrNumTolevelToAttribute2, distinguishes);

            if (debugPath) {
                distinguishes.entrySet().stream().forEach(System.out::println);
            }
            if (levelSetMinusLast.equals(distinguishes.keySet())) {
                showPathRules(
                        variableToValue,
                        pathFrame,
                        levelSet,
                        internal,
                        levelSetMinusLast,
                        distinguishes,
                        printStream);
            } else {
                printStream.println("#FAILS\t" +
                        PATH_PREFIX
                                + pathFrame
                                + "\t// "
                                + distinguishes);
            }
        }
    }

    private static Multimap2<List<Integer>, Level, List<String>>
            getAttributeNumToLevelToAttributesCombinations(
                    Multimap2<String, Level, List<String>> pathFrameToLevelToAttributeList,
                    int count,
                    String pathFrame,
                    SortedSet<Level> levelSet,
                    Map<Level, Delta> distinguishes) {
        distinguishes.clear();
        // try for pairs
        Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2 =
                Multimap2.create(HashMap::new);
        for (Level level : levelSet) {
            Set<List<String>> attributeLists =
                    pathFrameToLevelToAttributeList.get(pathFrame, level);
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
            String pathFrame,
            SortedSet<Level> levelSet,
            boolean internal,
            Set<Level> levelSetMinusLast,
            Map<Level, Delta> distinguishes,
            PrintStream printStream) {
        boolean first = true;
        for (Level level : levelSetMinusLast) {
            if (first) {
                printStream.println(PATH_PREFIX + pathFrame); //  + "\n#\t" + levelSet);
                if (DEBUG) {
                    System.out.println(pathFrame);
                }
                first = false;
            }
            printStream.println(LEVEL_PREFIX + level);
            Delta distinguishingAttributes = distinguishes.get(level);
            toAttributeRules(distinguishingAttributes, variableToValue).stream()
                    .forEach(
                            x ->
                                    printStream.println(
                                            ATTR_PREFIX
                                                    + x.first
                                                    + "="
                                                    + makeItems(x.second, variableToValue)));
        }
        printStream.println(FINAL_LEVEL_PREFIX + Iterables.getLast(levelSet, null));
    }

    private static void findDistinguishing(
            Set<Level> levelSetMinusLast,
            Multimap2<List<Integer>, Level, List<String>> attrNumTolevelToAttribute2,
            Map<Level, Delta> distinguishes) {
        for (List<Integer> attrNum : attrNumTolevelToAttribute2.keySet()) {
            for (Level level1 : levelSetMinusLast) {

                Set<List<String>> set1 = attrNumTolevelToAttribute2.get(attrNum, level1);

                // These are all the sets ABOVE level1
                Set<List<String>> set2 = Sets.newHashSet();
                for (Level level2 : levelSetMinusLast) {
                    if (level2.compareTo(level1) <= 0) {
                        continue;
                    }
                    set2.addAll(attrNumTolevelToAttribute2.get(attrNum, level2));
                }
                if (Collections.disjoint(set1, set2)) {
                    // level1 is distinguished from level2 AND ABOVE by set1
                    distinguishes.put(level1, new Delta(attrNum, set1, set2));
                } else {
                    int debug = 0;
                }
            }
        }
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
