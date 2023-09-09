package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;

public class DiffLanguageGroups {
    static final String SOURCE = "S";
    static final String TARGET = "T";
    private static final String IN = " ➡︎ ";
    static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    static final CLDRFile ENGLISH = CONFIG.getEnglish();
    static final Set<String> CLDR_ORG_LANGUAGES =
            StandardCodes.make().getLocaleCoverageLocales(Organization.cldr)
                    .stream()
                    .filter(x -> !x.contains("_"))
                    .collect(Collectors.toUnmodifiableSet());
    static final Set<String> OTHER_CLDR_LANGUAGES =
        Sets.difference(CONFIG.getCldrFactory().getAvailableLanguages()
                .stream()
                .filter(x -> !x.contains("_") && !x.equals("root"))
                .collect(Collectors.toUnmodifiableSet()), CLDR_ORG_LANGUAGES);

    public static void main(String[] args) {
        final String currentPath = CLDRPaths.COMMON_DIRECTORY + "supplemental/languageGroup.xml";
        String otherPath = CLDRPaths.COMMON_DIRECTORY + "supplemental-temp/languageGroup2.xml";
        if (args.length != 0) {
            otherPath = args[0];
        }

        // Get source information
        System.out.println("*\t" + SOURCE + "=\t" + "v43");
       Multimap<String, String> currentErrors = TreeMultimap.create();

        SortedMap<String, String> currentChildToParent =
                invertToMap(loadLanguageGroups(currentPath), currentErrors);
        if (!currentErrors.isEmpty()) {
            showErrors(SOURCE, currentErrors);
        }
         Set<String> currentSet = getAllKeysAndValues(currentChildToParent);
         checkAgainstCldr("CLDR_ORG -", SOURCE, CLDR_ORG_LANGUAGES, currentSet);
         checkAgainstCldr("CLDR_Other -", SOURCE, OTHER_CLDR_LANGUAGES, currentSet);

        // get target information

        System.out.println("*\t" + TARGET + "=\t" + "PR #3249" );
       Multimap<String, String> otherErrors = TreeMultimap.create();
        SortedMap<String, String> otherChildToParent =
                invertToMap(loadLanguageGroups(otherPath), otherErrors);
        if (!otherErrors.isEmpty()) {
            showErrors(TARGET, otherErrors);
        }

        Set<String> otherSet = getAllKeysAndValues(otherChildToParent);
        checkAgainstCldr("CLDR_ORG -", SOURCE, CLDR_ORG_LANGUAGES, otherSet);
        checkAgainstCldr("CLDR_Other -", SOURCE + " OTHER", OTHER_CLDR_LANGUAGES, otherSet);

        // Show differences

        showDiff("ΔRemoving (" + SOURCE + "-" + TARGET + ")", currentSet, otherSet);
        showDiff("ΔAdding (" + TARGET + "-" + SOURCE + ")", otherSet, currentSet);

        for (String joint : Sets.intersection(currentSet, otherSet)) {
            List<String> currentChain = getChain(joint, currentChildToParent, new ArrayList<>());
            List<String> otherChain = getChain(joint, otherChildToParent, new ArrayList<>());
            if (!currentChain.equals(otherChain)) {
                System.out.println(
                        show(joint)
                                + "\tmoved from "
                                + SOURCE
                                + "\t"
                                + IN
                                + currentChain.stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(IN))
                                + " — to "
                                + TARGET
                                + " — "
                                + IN
                                + otherChain.stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(IN)));
            }
        }
    }

    private static void checkAgainstCldr(String col1, String col2, Set<String> cldrLanguages, Set<String> currentSet) {
        SetView<String> missing = Sets.difference(cldrLanguages, currentSet);
        if (!missing.isEmpty()) {
            System.out.println(
                    col1
                    + "\t"
                            + col2
                            + "\t"
                            + missing.stream().map(x -> show(x)).collect(Collectors.joining(", ")));
        }
    }

    public static void showDiff(String title, Set<String> currentSet, Set<String> otherSet) {
        SetView<String> currentMinusOther = Sets.difference(currentSet, otherSet);
        if (!currentMinusOther.isEmpty()) {
            System.out.println(
                    title
                            + "\t"
                            + currentMinusOther.size()
                            + ":\t"
                            + currentMinusOther.stream()
                                    .map(x -> show(x))
                                    .collect(Collectors.joining(", ")));
        }
    }

    static String show(String languageCode) {
        return languageCode.equals("mul")
                ? "Ω"
                : ENGLISH.getName(CLDRFile.LANGUAGE_NAME, languageCode) + " ⁅" + languageCode + "⁆";
    }

    public static void showErrors(String title, Multimap<String, String> currentErrors) {
        for (Entry<String, Collection<String>> entry : currentErrors.asMap().entrySet()) {
            System.out.println(
                    show(entry.getKey())
                            + "\thas multiple parents in "
                            + title
                            + "\t"
                            + entry.getValue().stream()
                                    .map(x -> show(x))
                                    .collect(Collectors.joining(" 𝐯𝐬 ")));
        }
    }

    private static List<String> getChain(
            String joint, Map<String, String> childToParent, List<String> result) {
        String parent = childToParent.get(joint);
        if (parent == null) {
            return result;
        }
        result.add(parent);
        return getChain(parent, childToParent, result);
    }

    public static Multimap<String, String> loadLanguageGroups(String filename) {
        Multimap<String, String> otherParentToChildren = TreeMultimap.create();

        for (Pair<String, String> item :
                XMLFileReader.loadPathValues(
                        filename, new ArrayList<Pair<String, String>>(), false)) {
            handleLanguageGroups(
                    item.getSecond(),
                    XPathParts.getFrozenInstance(item.getFirst()),
                    otherParentToChildren);
        }
        otherParentToChildren = ImmutableSetMultimap.copyOf(otherParentToChildren);
        return otherParentToChildren;
    }

    public static SortedMap<String, String> invertToMap(
            Multimap<String, String> currentParentToChildren,
            Multimap<String, String> childToParents) {
        TreeMap<String, String> childToParent = new TreeMap<>();
        for (Entry<String, String> parentToChildren : currentParentToChildren.entries()) {
            final String parent = parentToChildren.getKey();
            final String child = parentToChildren.getValue();
            String old = childToParent.put(child, parent);
            if (old != null) {
                childToParents.put(child, old);
                childToParents.put(child, parent);
            }
        }
        return ImmutableSortedMap.copyOf(childToParent);
    }

    public static Set<String> getAllKeysAndValues(Map<String, String> other) {
        Set<String> otherSet = new TreeSet<>(other.values());
        otherSet.addAll(other.keySet());
        return ImmutableSet.copyOf(otherSet);
    }

    private static boolean handleLanguageGroups(
            String value, XPathParts parts, Multimap<String, String> languageGroups) {
        String parent = parts.getAttributeValue(-1, "parent");
        List<String> children = SupplementalDataInfo.WHITESPACE_SPLTTER.splitToList(value);
        languageGroups.putAll(parent, children);
        return true;
    }
}
