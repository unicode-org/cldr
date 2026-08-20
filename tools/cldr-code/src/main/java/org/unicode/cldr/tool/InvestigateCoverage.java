package org.unicode.cldr.tool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

public class InvestigateCoverage {
    private static final boolean SHORT_RUN = true;
    private static final String DEBUG_STOP = "be";
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();
    private static final Comparator<Iterable<String>> LEX_ITERABLE_COMPARATOR =
            Ordering.natural().lexicographical();

    public static void main(String[] args) {

        Multimap2<String, Level, List<String>> pathFrameToLevelToAttributeList =
                Multimap2.create(HashMap::new);
        // note: couldn't find a good way to supply a treemap for all the levels, using a
        // LEX_ITERABLE_COMPARATOR for the List

        char lastChar = 0;
        Set<String> localesToCheck =
                SHORT_RUN
                        ? Set.of("de")
                        : StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);
        TreeSet<String> sorted = new TreeSet<>();

        for (String locale : localesToCheck) {
            char currChar = locale.charAt(0);
            if (lastChar != currChar) {
                System.out.println(locale);
                lastChar = currChar;
            }
            if (locale.equals(DEBUG_STOP)) {
                break;
            }

            CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
            CoverageLevel2 coverage = SDI.getCoverageLevelInfo(locale);
            sorted.clear();
            cldrFile.fullIterable().forEach(sorted::add);
            for (String path : cldrFile.fullIterable()) {
                if (path.endsWith("/alias")) {
                    continue;
                }
                Level level = coverage.getLevel(path);
                SplitPath splitPath = SplitPath.from(path);
                List<String> attributes = splitPath.getAttributeValues();
                pathFrameToLevelToAttributeList.put(
                        splitPath.getScaffold().replace('"', '\''), level, attributes);
            }
        }
        pathFrameToLevelToAttributeList = pathFrameToLevelToAttributeList.createImmutable();

        Map<Set<List<String>>, String> valueToVariable = new LinkedHashMap<>();
        Map<String, Set<Iterable<String>>> variableToValue = new TreeMap<>();

        for (String pathFrame : Sets.newTreeSet(pathFrameToLevelToAttributeList.keySet())) {
            Set<Level> levelSet = pathFrameToLevelToAttributeList.keySet2(pathFrame);
            int localeCount = levelSet.size();
            if (localeCount == 1) {
                continue;
            }
            minimizeAttributes(
                    pathFrameToLevelToAttributeList, valueToVariable, pathFrame, levelSet);
        }

        // get inverted map
        {
            TreeSet<Iterable<String>> sortedAttributeLists =
                    Sets.newTreeSet(LEX_ITERABLE_COMPARATOR);

            for (Entry<Set<List<String>>, String> entry : valueToVariable.entrySet()) {
                sortedAttributeLists.clear();
                sortedAttributeLists.addAll(entry.getKey());
                variableToValue.put(entry.getValue(), Set.copyOf(sortedAttributeLists));
            }
        }

        //        System.out.println("#Variables");
        //        for (Entry<String, Set<Iterable<String>>> entry : variableToValue.entrySet()) {
        //            System.out.println(entry.getKey() + "\t" + entry.getValue());
        //        }
        //
        //        System.out.println("#Rules");
        //        for (String pathFrame : Sets.newTreeSet(pathFrameToLevelToAttributeList.keySet()))
        // {
        //            Set<Level> levelSet = pathFrameToLevelToAttributeList.keySet2(pathFrame);
        //            if (levelSet.size() == 1) {
        //                System.out.println(Joiners.TAB.join(pathFrame, levelSet.iterator().next(),
        // "ANY"));
        //                continue;
        //            }
        //            for (Level level : Sets.newTreeSet(levelSet)) {
        //                Set<List<String>> attributeLists =
        //                        pathFrameToLevelToAttributeList.get(pathFrame, level);
        //                System.out.println(
        //                        Joiners.TAB.join(pathFrame, level,
        // valueToVariable.get(attributeLists)));
        //            }
        //        }
    }

    private static void minimizeAttributes(
            Multimap2<String, Level, List<String>> pathFrameToLevelToAttributeList,
            Map<Set<List<String>>, String> valueToVariable,
            String pathFrame,
            Set<Level> levelSet) {
        boolean internal = false;
        Set<String> testPaths =
                ImmutableSet.of(
                        // "//ldml/dates/calendars/calendar[@type='%A']/dateTimeFormats/availableFormats/dateFormatItem[@id='%A']",
                        // "//ldml/dates/calendars/calendar[@type='%A']/dateTimeFormats/appendItems/appendItem[@request='%A']"
                        //
                        );

        boolean debugPath = testPaths.contains(pathFrame);
        if (debugPath) {
            int debug = 0;
        }

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

        Multimap<Pair<Level, Level>, Delta> distinguishes = HashMultimap.create();
        for (Integer attrNum : attrNumTolevelToAttribute.keySet()) {
            for (Pair<Level, Level> levelPair : levelPairs) {
                Level level1 = levelPair.getFirst();
                Level level2 = levelPair.getSecond();
                Set<String> set1 = attrNumTolevelToAttribute.get(attrNum, level1);
                Set<String> set2 = attrNumTolevelToAttribute.get(attrNum, level2);
                if (Collections.disjoint(set1, set2)) {
                    // level1 is distinguished from level2 by set1
                    distinguishes.put(Pair.of(level1, level2), new Delta(attrNum, set1, set2));
                } else {
                    if (debugPath) {
                        System.out.println(level1 + "\t" + set1);
                        System.out.println(level2 + "\t" + set2);
                    }
                    int debug = 0;
                }
            }
        }
        if (debugPath) {
            distinguishes.asMap().entrySet().stream().forEach(System.out::println);
            int debug = 0;
        }

        // find missing
        boolean succeeds = levelPairs.equals(distinguishes.keySet());

        if (succeeds) {
            TreeSet<Pair<Level, Level>> sorted = Sets.newTreeSet(distinguishes.keySet());
            int levelPairCount = 0;

            Level lastLevel = null;
            for (Pair<Level, Level> levelPair : sorted) {
                RuleBucket rb = RuleBucket.from(++levelPairCount, levelPairs.size());
                showRules(
                        internal, rb, pathFrame, levelSet, levelPair, distinguishes.get(levelPair));
                lastLevel = levelPair.getSecond();
            }
            System.out.println("level=" + lastLevel);
        } else {
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

            Multimap<Pair<Level, Level>, Delta> distinguishes2 = HashMultimap.create();

            for (List<Integer> attrNum : attrNumTolevelToAttribute2.keySet()) {
                for (Pair<Level, Level> levelPair : levelPairs) {
                    if (distinguishes.containsKey(levelPair)) {
                        continue; // already distinguished
                    }
                    Level level1 = levelPair.getFirst();
                    Level level2 = levelPair.getSecond();
                    Set<List<String>> set1 = attrNumTolevelToAttribute2.get(attrNum, level1);
                    Set<List<String>> set2 = attrNumTolevelToAttribute2.get(attrNum, level2);
                    if (Collections.disjoint(set1, set2)) {
                        // level1 is distinguished from level2 by set1
                        distinguishes2.put(Pair.of(level1, level2), new Delta(attrNum, set1, set2));
                    } else {
                        int debug = 0;
                    }
                }
            }

            succeeds =
                    levelPairs.equals(Sets.union(distinguishes.keySet(), distinguishes2.keySet()));

            if (debugPath) {
                distinguishes2.asMap().entrySet().stream().forEach(System.out::println);
            }
            int levelPairCount = 0;
            Level lastLevel = null;
            for (Pair<Level, Level> levelPair : levelPairs) {
                RuleBucket rb = RuleBucket.from(++levelPairCount, levelPairs.size());
                if (distinguishes.containsKey(levelPair)) {
                    showRules(
                            internal,
                            rb,
                            pathFrame,
                            levelSet,
                            levelPair,
                            distinguishes.get(levelPair));
                } else if (distinguishes2.containsKey(levelPair)) {
                    showRules(
                            internal,
                            rb,
                            pathFrame,
                            levelSet,
                            levelPair,
                            distinguishes2.get(levelPair));
                } else {
                    System.out.println(
                            Joiners.TAB.join(
                                    pathFrame,
                                    levelSet,
                                    levelPair.getFirst(),
                                    "vs",
                                    levelPair.getSecond(),
                                    "",
                                    "MISSING-TODO"));
                }
                lastLevel = levelPair.getSecond();
            }
            System.out.println("level=" + lastLevel);
        }

        int debug = 0;

        // TODO when we can't distinguish each combination, move to multiple attributes

        int localeCount2 = levelSet.size();

        for (Level level : Sets.newTreeSet(levelSet)) {
            boolean isLast = --localeCount2 == 0;
            Set<List<String>> attributeLists =
                    pathFrameToLevelToAttributeList.get(pathFrame, level);
            if (!valueToVariable.containsKey(attributeLists)) {
                valueToVariable.put(
                        attributeLists,
                        "%V" + String.format("%03d", valueToVariable.size()) + (isLast ? "x" : ""));
            }
        }
    }

    enum RuleBucket {
        only,
        first,
        medial,
        last;

        static RuleBucket from(int item, int max) {
            if (max == 1) {
                return only;
            } else if (item == 1) {
                return first;
            } else if (item == max) {
                return last;
            } else {
                return medial;
            }
        }
    }

    private static void showRules(
            boolean internals,
            RuleBucket rb,
            String pathFrame,
            Set<Level> levelSet,
            Pair<Level, Level> levelPair,
            Collection<Delta> deltas) {
        if (internals) {
            System.out.println(
                    Joiners.TAB.join(
                            pathFrame,
                            levelSet,
                            levelPair.getFirst(),
                            "vs",
                            levelPair.getSecond(),
                            deltas));
        } else {
            // allow for handling the last / only one specially, choosing to
            switch (rb) {
                case only:
                case first:
                    System.out.println("\npath=" + pathFrame);
                    System.out.println("level=" + levelPair.getFirst());
                    toRules(deltas).stream().forEach(System.out::println);
                    break;
                case medial:
                    System.out.println("level=" + levelPair.getFirst());
                    toRules(deltas).stream().forEach(System.out::println);
                case last:
                    System.out.println("level=" + levelPair.getFirst());
                    break;
            }
        }
    }

    private static Set<String> toRules(Collection<Delta> deltas) {
        // for now, we just test the first one
        // later, we can test for the inverse of the last one

        // just take the first delta
        Delta first = deltas.iterator().next();

        // Cases: a-b, a-c => attr1=a, attr2=b|c
        // Cases: a-b, c-b => attr1=a|c, attr2=b
        // Cases: a-b, c-d => attr1=a, attr2=b ; attr1=c, attr2=d
        switch (first.attributeNumbers.size()) {
            default:
                throw new UnsupportedOperationException();
            case 1:
                return Set.of(
                        "attr"
                                + first.attributeNumbers.get(0)
                                + "="
                                + Joiners.VBAR.join(first.delta1));
            case 2:
                {
                    Multimap<String, String> map1 = TreeMultimap.create();
                    first.delta1.stream()
                            .forEach(
                                    x -> {
                                        map1.put(x.get(0), x.get(1));
                                    });
                    Multimap<Set<String>, String> map2 =
                            TreeMultimap.create(LEX_ITERABLE_COMPARATOR, Comparator.naturalOrder());
                    for (Entry<String, Collection<String>> entry : map1.asMap().entrySet()) {
                        map2.put(Sets.newTreeSet(entry.getValue()), entry.getKey());
                    }
                    LinkedHashSet<String> result = Sets.newLinkedHashSet();
                    for (Entry<Set<String>, Collection<String>> entry : map2.asMap().entrySet()) {
                        // at this point the first attributes are in the values
                        String attr1 = Joiners.VBAR.join(Sets.newTreeSet(entry.getValue()));
                        String attr2 = Joiners.VBAR.join(entry.getKey());
                        result.add("attr" + first.attributeNumbers.get(0) + "=" + attr1);
                        result.add("attr" + first.attributeNumbers.get(1) + "=" + attr2);
                    }
                    return result;
                }
        }
    }

    static class Delta {
        final List<Integer> attributeNumbers;
        final Set<List<String>> delta1;
        final Set<List<String>> delta2;

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
            return Joiners.N.join(attributeNumbers, delta1, delta2);
        }
    }

    static ImmutableSortedSet<List<String>> sort(Iterable<List<String>> items) {
        return ImmutableSortedSet.copyOf(LEX_ITERABLE_COMPARATOR, items);
    }

    private static Set<List<String>> product(Set<String> set1, Set<String> set1a) {
        Set<List<String>> product1 = new HashSet<>();
        for (String s1 : set1) {
            for (String s2 : set1a) {
                product1.add(List.of(s1, s2));
            }
        }
        return product1;
    }
}
