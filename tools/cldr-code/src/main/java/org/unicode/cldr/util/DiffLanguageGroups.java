package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.util.VersionInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

public class DiffLanguageGroups {
    private static final Joiner JOIN_TAB = Joiner.on('\t');
    private static final String IN = " ➡︎ ";
    static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    static final CLDRFile ENGLISH = CONFIG.getEnglish();

    enum LanguageStatus {
        TC("TC"),
        OTHER_BASIC_PLUS("OB"),
        OTHER_CLDR("OC"),
        NON_CLDR("NC"),
        NON_REGULAR("XX");

        public final String abbr;

        private LanguageStatus(String s) {
            abbr = s;
        }

        static final Set<LanguageStatus> SKIP_MISSING =
                Set.of(LanguageStatus.NON_CLDR, LanguageStatus.NON_REGULAR);
    }

    static final Map<String, LanguageStatus> LanguageToStatus;
    static final Map<LanguageStatus, Set<String>> StatusToLanguages;

    static {
        // add items to the map, most general first so the others can override
        Map<String, LanguageStatus> temp = new TreeMap<>();

        Sets.union(
                        Validity.getInstance()
                                .getStatusToCodes(LstrType.language)
                                .get(Status.regular),
                        Set.of("mul"))
                .stream()
                .forEach(x -> temp.put(x, LanguageStatus.NON_CLDR));

        CONFIG.getCldrFactory().getAvailableLanguages().stream()
                .forEach(
                        x -> {
                            if (!x.contains("_") && !x.equals("root"))
                                temp.put(x, LanguageStatus.OTHER_CLDR);
                        });

        CalculatedCoverageLevels.getInstance().getLevels().entrySet().stream()
                .forEach(
                        x -> {
                            if (!x.getKey().contains("_"))
                                temp.put(x.getKey(), LanguageStatus.OTHER_BASIC_PLUS);
                        });

        Sets.difference(
                        StandardCodes.make().getLocaleCoverageLocales(Organization.cldr),
                        StandardCodes.make().getLocaleCoverageLocales(Organization.special))
                .stream()
                .forEach(
                        x -> {
                            if (!x.contains("_")) temp.put(x, LanguageStatus.TC);
                        });
        LanguageToStatus = ImmutableMap.copyOf(temp);

        Multimap<LanguageStatus, String> temp2 = TreeMultimap.create();
        LanguageToStatus.entrySet().stream().forEach(x -> temp2.put(x.getValue(), x.getKey()));
        Map<LanguageStatus, Set<String>> temp3 = new LinkedHashMap<>();
        temp2.asMap().entrySet().forEach(x -> temp3.put(x.getKey(), new TreeSet<>(x.getValue())));
        StatusToLanguages = CldrUtility.protectCollection(temp3);
    }

    public static LanguageStatus getStatusForLanguage(String joint) {
        return CldrUtility.ifNull(LanguageToStatus.get(joint), LanguageStatus.NON_REGULAR);
    }

    static String OLD = "OLD";
    static String NEW = "NEW";

    public static void main(String[] args) {
        System.out.println(
                "Args are OLD and NEW CLDR versions. Defaults: OLD = last release, NEW = current data. Format is X.Y, eg: "
                        + ToolConstants.LAST_RELEASE_VERSION_WITH0);
        System.out.println("\nReading old supplemental: may have unrelated errors.");
        final SupplementalDataInfo oldSupplementalInfo =
                SupplementalDataInfo.getInstance(
                        CldrUtility.getPath(CLDRPaths.LAST_COMMON_DIRECTORY, "supplemental/"));
        System.out.println();

        VersionInfo oldVersion = oldSupplementalInfo.getCldrVersion();

        String oldBase = ToolConstants.LAST_RELEASE_VERSION_WITH0;
        String newBase = null;

        if (args.length > 0) {
            oldBase = args[0];
            if (args.length > 1) {
                newBase = args[1];
            }
        }
        String oldPath =
                CLDRPaths.ARCHIVE_DIRECTORY
                        + "cldr-"
                        + oldBase
                        + "/common/supplemental/languageGroup.xml";
        String newPath =
                newBase == null
                        ? CLDRPaths.COMMON_DIRECTORY + "supplemental/languageGroup.xml"
                        : CLDRPaths.ARCHIVE_DIRECTORY
                                + "cldr-"
                                + newBase
                                + "/common/supplemental/languageGroup.xml";

        OLD = "v" + oldVersion.getVersionString(1, 2);
        NEW = "V" + SDI.getCldrVersion().getVersionString(1, 2);

        System.out.println("* KEY");
        for (LanguageStatus status : LanguageStatus.values()) {
            System.out.println("\t" + status.abbr + "\t" + status.toString());
        }
        System.out.println();

        // Get OLD information

        Multimap<String, String> oldErrors = TreeMultimap.create();
        SortedMap<String, String> oldChildToParent =
                invertToMap(loadLanguageGroups(oldPath), oldErrors);
        if (!oldErrors.isEmpty()) {
            showErrors(OLD, oldErrors);
        }
        Set<String> oldSet = getAllKeysAndValues(oldChildToParent);

        // Old info
        for (Entry<LanguageStatus, Set<String>> entry : StatusToLanguages.entrySet()) {
            checkAgainstReference(OLD, entry.getKey(), entry.getValue(), oldSet);
        }

        // get NEW information

        Multimap<String, String> newErrors = TreeMultimap.create();
        SortedMap<String, String> newChildToParent =
                invertToMap(loadLanguageGroups(newPath), newErrors);
        if (!newErrors.isEmpty()) {
            showErrors(NEW, newErrors);
        }

        Set<String> newSet = getAllKeysAndValues(newChildToParent);
        for (Entry<LanguageStatus, Set<String>> entry : StatusToLanguages.entrySet()) {
            checkAgainstReference(NEW, entry.getKey(), entry.getValue(), newSet);
        }

        // Show differences

        // showDiff("Δ Removing (" + OLD + "-" + NEW + ")", Sets.difference(oldSet, newSet));
        for (LanguageStatus status : LanguageStatus.values()) {
            for (String joint : Sets.difference(oldSet, newSet)) {
                if (getStatusForLanguage(joint) != status) {
                    continue;
                }
                List<String> childToParent = getChain(joint, oldChildToParent, new ArrayList<>());
                System.out.println(
                        JOIN_TAB.join(
                                OLD,
                                getStatusForLanguage(joint).abbr,
                                show(joint),
                                "Removed",
                                childToParent.stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(IN))));
            }
        }

        // showDiff("Δ Adding (" + NEW + "-" + OLD + ")", Sets.difference(newSet, oldSet));
        for (LanguageStatus status : LanguageStatus.values()) {
            for (String joint : Sets.difference(newSet, oldSet)) {
                if (getStatusForLanguage(joint) != status) {
                    continue;
                }
                List<String> childToParent = getChain(joint, newChildToParent, new ArrayList<>());
                System.out.println(
                        JOIN_TAB.join(
                                NEW,
                                getStatusForLanguage(joint).abbr,
                                show(joint),
                                "Added",
                                childToParent.stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(IN))));
            }
        }

        Set<String> changed = new TreeSet<>();
        for (String joint : Sets.intersection(oldSet, newSet)) {
            List<String> oldChain = getChain(joint, oldChildToParent, new ArrayList<>());
            List<String> newChain = getChain(joint, newChildToParent, new ArrayList<>());
            if (!oldChain.equals(newChain)) {
                changed.add(joint);
            }
        }
        // showDiff("Δ Moving (" + OLD + " to " + NEW + ")", changed);

        for (LanguageStatus status : LanguageStatus.values()) {
            for (String joint : changed) {
                if (getStatusForLanguage(joint) != status) {
                    continue;
                }
                List<String> oldChain = getChain(joint, oldChildToParent, new ArrayList<>());
                List<String> newChain = getChain(joint, newChildToParent, new ArrayList<>());
                System.out.println(
                        JOIN_TAB.join(
                                OLD + "-" + NEW,
                                getStatusForLanguage(joint).abbr,
                                show(joint),
                                "Moved FROM",
                                oldChain.stream().map(x -> show(x)).collect(Collectors.joining(IN)),
                                "TO",
                                newChain.stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(IN))));
            }
        }
    }

    private static void checkAgainstReference(
            String version,
            LanguageStatus languageStatus,
            Set<String> cldrLanguages,
            Set<String> oldSet) {
        if (LanguageStatus.SKIP_MISSING.contains(languageStatus)) {
            return;
        }
        SetView<String> missing = Sets.difference(cldrLanguages, oldSet);
        if (!missing.isEmpty()) {
            System.out.println(
                    JOIN_TAB.join(
                            version,
                            languageStatus.abbr,
                            "…",
                            "Missing",
                            missing.stream().map(x -> show(x)).collect(Collectors.joining(", "))));
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

    public static String show(String languageCode) {
        return languageCode.equals("mul") ? "Ω" : getName(languageCode) + " ⁅" + languageCode + "⁆";
    }

    public static String getName(String languageCode) {
        String result =
                ENGLISH.nameGetter().getNameFromTypeEnumCode(NameType.LANGUAGE, languageCode);
        return result == null ? "(no name)" : result.replace(" (Other)", "");
    }

    public static void showErrors(String title, Multimap<String, String> oldErrors) {
        for (LanguageStatus status : LanguageStatus.values()) {
            for (Entry<String, Collection<String>> entry : oldErrors.asMap().entrySet()) {
                if (getStatusForLanguage(entry.getKey()) != status) {
                    continue;
                }
                System.out.println(
                        formatMessage(
                                title,
                                entry.getKey(),
                                "Multiple parents",
                                entry.getValue().stream()
                                        .map(x -> show(x))
                                        .collect(Collectors.joining(" 𝐯𝐬 "))));
            }
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
        List<String> children = SupplementalDataInfo.WHITESPACE_SPLITTER.splitToList(value);
        languageGroups.putAll(parent, children);
        return true;
    }

    static String formatMessage(String version, String language, String issue, String data) {
        return JOIN_TAB.join(
                version, getStatusForLanguage(language).abbr, show(language), issue, data);
    }
}
