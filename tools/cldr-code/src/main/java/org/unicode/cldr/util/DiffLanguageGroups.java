package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
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
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

public class DiffLanguageGroups {
    static final String OLD = "OLD";
    static final String NEW = "NEW";
    private static final String IN = " ➡︎ ";
    static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    static final CLDRFile ENGLISH = CONFIG.getEnglish();
    static final Set<String> CLDR_ORG_LANGUAGES =
            StandardCodes.make().getLocaleCoverageLocales(Organization.cldr).stream()
                    .filter(x -> !x.contains("_"))
                    .collect(Collectors.toUnmodifiableSet());
    static final Set<String> OTHER_CLDR_LANGUAGES =
            Sets.difference(
                    CONFIG.getCldrFactory().getAvailableLanguages().stream()
                            .filter(x -> !x.contains("_") && !x.equals("root"))
                            .collect(Collectors.toUnmodifiableSet()),
                    CLDR_ORG_LANGUAGES);

    public static void main(String[] args) {
        String newPath = CLDRPaths.COMMON_DIRECTORY + "supplemental/languageGroup.xml";
        String oldPath = CLDRPaths.COMMON_DIRECTORY + "supplemental-temp/languageGroup43.xml";
        if (args.length > 0) {
            newPath = args[0];
            if (args.length > 1) {
                oldPath = args[1];
            }
        }
        final Set<String> validRegular =
                Sets.union(
                        Validity.getInstance()
                                .getStatusToCodes(LstrType.language)
                                .get(Status.regular),
                        Set.of("mul"));

        // Get OLD information

        System.out.println("* " + OLD + " = " + "v43\t\t");
        Multimap<String, String> oldErrors = TreeMultimap.create();

        SortedMap<String, String> oldChildToParent =
                invertToMap(loadLanguageGroups(oldPath), oldErrors);
        if (!oldErrors.isEmpty()) {
            showErrors(OLD, oldErrors);
        }
        Set<String> oldSet = getAllKeysAndValues(oldChildToParent);
        checkAgainstReference(OLD + " Missing", "∉ CLDR_ORG", CLDR_ORG_LANGUAGES, oldSet);
        checkAgainstReference(OLD + " Missing", "∉ CLDR_Other", OTHER_CLDR_LANGUAGES, oldSet);
        checkAgainstReference(OLD + " Invalid", "", oldSet, validRegular);

        // get NEW information

        System.out.println("* " + NEW + " = " + "PR\t\t");
        Multimap<String, String> newErrors = TreeMultimap.create();
        SortedMap<String, String> newChildToParent =
                invertToMap(loadLanguageGroups(newPath), newErrors);
        if (!newErrors.isEmpty()) {
            showErrors(NEW, newErrors);
        }

        Set<String> newSet = getAllKeysAndValues(newChildToParent);
        checkAgainstReference(NEW + " Missing", "∉ CLDR_ORG", CLDR_ORG_LANGUAGES, newSet);
        checkAgainstReference(NEW + " Missing", "∉ CLDR_Other", OTHER_CLDR_LANGUAGES, newSet);
        checkAgainstReference(NEW + " Invalid", "", newSet, validRegular);

        // Show differences

        showDiff("Δ Removing (" + OLD + "-" + NEW + ")", Sets.difference(oldSet, newSet));

        showDiff("Δ Adding (" + NEW + "-" + OLD + ")", Sets.difference(newSet, oldSet));
        for (String joint : Sets.difference(newSet, oldSet)) {
            List<String> newChain = getChain(joint, newChildToParent, new ArrayList<>());
            System.out.println(
                    NEW
                            + " Added"
                            + "\t"
                            + show(joint)
                            + "\t"
                            + IN
                            + newChain.stream().map(x -> show(x)).collect(Collectors.joining(IN)));
        }

        Set<String> changed = new TreeSet<>();
        for (String joint : Sets.intersection(oldSet, newSet)) {
            List<String> oldChain = getChain(joint, oldChildToParent, new ArrayList<>());
            List<String> newChain = getChain(joint, newChildToParent, new ArrayList<>());
            if (!oldChain.equals(newChain)) {
                changed.add(joint);
            }
        }
        showDiff("Δ Moving (" + OLD + " to " + NEW + ")", changed);

        for (String joint : changed) {
            List<String> oldChain = getChain(joint, oldChildToParent, new ArrayList<>());
            List<String> newChain = getChain(joint, newChildToParent, new ArrayList<>());
            System.out.println(
                    OLD
                            + " Removed "
                            + "\t"
                            + show(joint)
                            + "\t"
                            + IN
                            + oldChain.stream().map(x -> show(x)).collect(Collectors.joining(IN)));
            System.out.println(
                    NEW
                            + " Moved to "
                            + "\t"
                            + show(joint)
                            + "\t"
                            + IN
                            + newChain.stream().map(x -> show(x)).collect(Collectors.joining(IN)));
        }
    }

    private static void checkAgainstReference(
            String col1, String col2, Set<String> cldrLanguages, Set<String> oldSet) {
        SetView<String> missing = Sets.difference(cldrLanguages, oldSet);
        if (!missing.isEmpty()) {
            System.out.println(
                    col1
                            + "\t"
                            + col2
                            + "\t"
                            + missing.stream().map(x -> show(x)).collect(Collectors.joining(", ")));
        }
    }

    public static void showDiff(String title, Set<String> oldMinusOther) {
        if (!oldMinusOther.isEmpty()) {
            System.out.println(
                    title
                            + "\t"
                            + oldMinusOther.size()
                            + ":\t"
                            + oldMinusOther.stream()
                                    .map(x -> show(x))
                                    .collect(Collectors.joining(", ")));
        }
    }

    static String show(String languageCode) {
        return languageCode.equals("mul")
                ? "Ω"
                : ENGLISH.getName(CLDRFile.LANGUAGE_NAME, languageCode) + " ⁅" + languageCode + "⁆";
    }

    public static void showErrors(String title, Multimap<String, String> oldErrors) {
        for (Entry<String, Collection<String>> entry : oldErrors.asMap().entrySet()) {
            System.out.println(
                    title
                            + " Multiple parents"
                            + "\t"
                            + show(entry.getKey())
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
        Multimap<String, String> newParentToChildren = TreeMultimap.create();

        for (Pair<String, String> item :
                XMLFileReader.loadPathValues(
                        filename, new ArrayList<Pair<String, String>>(), false)) {
            handleLanguageGroups(
                    item.getSecond(),
                    XPathParts.getFrozenInstance(item.getFirst()),
                    newParentToChildren);
        }
        newParentToChildren = ImmutableSetMultimap.copyOf(newParentToChildren);
        return newParentToChildren;
    }

    public static SortedMap<String, String> invertToMap(
            Multimap<String, String> oldParentToChildren, Multimap<String, String> childToParents) {
        TreeMap<String, String> childToParent = new TreeMap<>();
        for (Entry<String, String> parentToChildren : oldParentToChildren.entries()) {
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

    public static Set<String> getAllKeysAndValues(Map<String, String> newItems) {
        Set<String> newSet = new TreeSet<>(newItems.values());
        newSet.addAll(newItems.keySet());
        return ImmutableSet.copyOf(newSet);
    }

    private static boolean handleLanguageGroups(
            String value, XPathParts parts, Multimap<String, String> languageGroups) {
        String parent = parts.getAttributeValue(-1, "parent");
        List<String> children = SupplementalDataInfo.WHITESPACE_SPLTTER.splitToList(value);
        languageGroups.putAll(parent, children);
        return true;
    }
}
