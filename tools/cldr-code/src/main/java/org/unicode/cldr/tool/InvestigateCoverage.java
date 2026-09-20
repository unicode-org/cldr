package org.unicode.cldr.tool;

import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.Organization;
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
            for (Level level : Sets.newTreeSet(levelSet)) {
                boolean isLast = --localeCount == 0;
                Set<List<String>> attributeLists =
                        pathFrameToLevelToAttributeList.get(pathFrame, level);
                if (!valueToVariable.containsKey(attributeLists)) {
                    valueToVariable.put(
                            attributeLists,
                            "%V"
                                    + String.format("%03d", valueToVariable.size())
                                    + (isLast ? "x" : ""));
                }
            }
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

        System.out.println("#Variables");
        for (Entry<String, Set<Iterable<String>>> entry : variableToValue.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }

        System.out.println("#Rules");
        for (String pathFrame : Sets.newTreeSet(pathFrameToLevelToAttributeList.keySet())) {
            Set<Level> levelSet = pathFrameToLevelToAttributeList.keySet2(pathFrame);
            for (Level level : Sets.newTreeSet(levelSet)) {
                Set<List<String>> attributeLists =
                        pathFrameToLevelToAttributeList.get(pathFrame, level);
                System.out.println(
                        Joiners.TAB.join(pathFrame, level, valueToVariable.get(attributeLists)));
            }
        }
    }
}
