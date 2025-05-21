package org.unicode.cldr.tool;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.UnicodeSet;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.test.RelatedDatePathValues;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

public class ListProblemDates {
    private static final SetView<String> TC_LOCALES =
            Sets.difference(
                    StandardCodes.make().getLocaleCoverageLocales(Organization.cldr),
                    StandardCodes.make().getLocaleCoverageLocales(Organization.special));
    private static final String SAMPLE_ISO_DATE = "2020-01-02T03:04:05Z";
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Factory FACTORY = CLDR_CONFIG.getCldrFactory();

    private static final boolean VERBOSE = false;
    private static final boolean ALL_LOCALES = true;
    private static final boolean OTHER_CALENDARS = false;
    private static final Set<String> calendars = Set.of("gregorian");
    private static final Set<String> ROOT = Set.of("root");
    private static final Set<String> NON_ROOT = Sets.difference(TC_LOCALES, Set.of("root"));

    private static final Collection<String> targets = !ALL_LOCALES ? ROOT : TC_LOCALES;

    private static long sampleDate = Instant.parse(SAMPLE_ISO_DATE).toEpochMilli();

    public static void main(String[] args) {
        ImmutableMultimap<String, Pair<String, String>> rootIds = getIds(ROOT);
        System.out.println("# ROOT skeletons");
        System.out.println(rootIds.keySet());

        ImmutableMultimap<String, Pair<String, String>> allIds = getIds(NON_ROOT);

        Set<String> tcMinusRootIds =
                ImmutableSet.copyOf(Sets.difference(allIds.keySet(), rootIds.keySet()));
        Set<String> rootMinusTcIds =
                ImmutableSet.copyOf(Sets.difference(rootIds.keySet(), allIds.keySet()));

        System.out.println("\n# TC-Root skeletons");
        System.out.println(tcMinusRootIds);

        // comparison
        if (!tcMinusRootIds.isEmpty()) {
            System.out.println("\n# in TC minus Root");
            tcMinusRootIds.stream()
                    .map(x -> x + "\t" + bestCalendar(allIds.asMap().get(x)))
                    .forEach(System.out::println);
            System.out.println("ERROR: " + tcMinusRootIds);
        }
        if (!rootMinusTcIds.isEmpty()) {
            System.out.println("\n# in Root only");
            rootMinusTcIds.stream()
                    .map(x -> x + "\t" + bestCalendar(allIds.asMap().get(x)))
                    .forEach(System.out::println);
            System.out.println("ERROR: " + rootMinusTcIds);
        }

        System.out.println("\n# Variant lengths: root");
        showVariants(rootIds.keySet());

        System.out.println("\n# Variant lengths: other locales");
        showVariants(tcMinusRootIds);

        //        UnicodeSet allCharacters = new UnicodeSet();
        //        notRoot.stream().forEach(x -> allCharacters.addAll(x));
        //        allCharacters.freeze();
        //        System.out.println("\n# All skeleton characters:\t" + allCharacters);
        //
        //        UnicodeSet rootCharacters = new UnicodeSet();
        //        rootIds.keySet().stream().forEach(x -> rootCharacters.addAll(x));
        //        rootCharacters.freeze();
        //        System.out.println("\n# Root skeleton characters:\t" + rootCharacters);

        final Multimap<List<String>, String> coreIdXIdTolocales =
                TreeMultimap.create(
                        new CldrUtility.CollectionComparator<String>(), Comparator.naturalOrder());

        System.out.println("\n# Non-inclusions: sample = " + SAMPLE_ISO_DATE);

        System.out.println(
                Joiners.TAB.join(
                        "status",
                        "locale",
                        "calendar",
                        "skeleton",
                        "core",
                        "pattern",
                        "core",
                        "formatted",
                        "core",
                        "path",
                        "core"));

        for (String locale : targets) {

            CLDRFile cldrFile = FACTORY.make(locale, true);
            ICUServiceBuilder service = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(locale));
            Set<PathHeader> sortedPaths = new TreeSet<>();

            for (String path : cldrFile) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String alt = parts.getAttributeValue(-1, "alt");
                if (alt != null) {
                    continue;
                }

                SkeletonType idElement = SkeletonType.from(parts);
                if (SkeletonType.none == idElement) { // not a type with skeletons
                    continue;
                }
                String id = idElement.getSkeleton(parts);
                if (getCores(id).isEmpty()) {
                    continue;
                }

                String calendar = parts.getAttributeValue(3, "type");
                if (calendars.contains(calendar) == OTHER_CALENDARS) {
                    continue;
                }

                sortedPaths.add(PathHeader.getFactory().fromPath(path));
            }

            for (PathHeader ph : sortedPaths) {
                String path = ph.getOriginalPath();
                String pattern = cldrFile.getStringValue(path);
                if (pattern == null) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                SkeletonType idElement = SkeletonType.from(parts);
                String calendar = parts.getAttributeValue(3, "type");

                String id = idElement.getSkeleton(parts);
                XPathParts coreParts = parts.cloneAsThawed();

                Collection<String> cores = getCores(id);
                for (String coreId : cores) {
                    idElement.setSkeleton(coreParts, coreId);
                    String corePath = coreParts.toString();

                    String corePattern = cldrFile.getStringValue(corePath);
                    if (corePattern == null) {
                        coreIdXIdTolocales.put(List.of(calendar, id, coreId), locale);
                        continue;
                    }
                    if (!containsWithoutBridges(pattern, corePattern)) {
                        System.out.println(
                                Joiners.TAB.join(
                                        "FAIL",
                                        locale,
                                        calendar,
                                        id,
                                        coreId,
                                        pattern,
                                        corePattern,
                                        formatDate(service, calendar, pattern),
                                        formatDate(service, calendar, corePattern),
                                        path,
                                        corePath));
                    } else if (VERBOSE) {
                        System.out.println(
                                Joiners.TAB.join(
                                        "OK",
                                        locale,
                                        calendar,
                                        id,
                                        coreId,
                                        pattern,
                                        corePattern,
                                        formatDate(service, calendar, pattern),
                                        formatDate(service, calendar, corePattern),
                                        path,
                                        corePath));
                    }
                }
            }
        }
        if (false) {
            System.out.println("\n# Missing cores");
            coreIdXIdTolocales.asMap().entrySet().stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x.getKey() + "\t" + Joiners.SP.join(x.getValue())));
        }
    }

    private static String formatDate(ICUServiceBuilder service, String calendar, String pattern) {
        return service.getDateFormat(calendar, pattern).format(sampleDate);
    }

    private static boolean containsWithoutBridges(String container, String containee) {
        int start = container.indexOf(containee);
        if (start < 0) {
            return false;
        }
        if (start > 0 && bridges(container.charAt(start - 1), containee.charAt(0))) {
            return false;
        }
        int limit = start + containee.length();
        if (limit < container.length()
                && bridges(container.charAt(limit), containee.charAt(containee.length() - 1))) {
            return false;
        }
        return true;
    }

    static final UnicodeSet DATE_PATTERN_PLACEHOLDERS = new UnicodeSet("[a-zA-Z]").freeze();

    // OK to use char, because all date pattern placeholders are < U+10000

    private static boolean bridges(char char1, char char2) {
        return char1 == char2
                && DATE_PATTERN_PLACEHOLDERS.contains(char1)
                && DATE_PATTERN_PLACEHOLDERS.contains(char2);
    }

    private static Map<String, Set<String>> skeletonToCores = new HashMap<>();

    // # Root skeleton characters: [BEGHMQUWdhmsvwy]
    // # All skeleton characters:   [EGHMQZcdhmsvy]

    private static Collection<String> getCores(String skeleton) {
        return RelatedDatePathValues.getCores(skeleton);
    }

    private static void showVariants(Set<String> widthVariants) {
        Multimap<String, String> noLength = TreeMultimap.create(SKELETON_COMPARE, SKELETON_COMPARE);
        widthVariants.stream()
                .forEach(
                        x -> {
                            UnicodeSet s = new UnicodeSet().addAll(x);
                            noLength.put(s.toString(), x);
                        });

        noLength.asMap().entrySet().stream()
                .forEach(
                        x ->
                                System.out.println(
                                        x.getKey() + "\t" + Joiners.TAB.join(x.getValue())));

        UnicodeSet allCharacters = new UnicodeSet();
        widthVariants.stream().forEach(x -> allCharacters.addAll(x));
        System.out.println("\n# Skeleton characters:\t" + allCharacters);
    }

    private static String bestCalendar(Collection<Pair<String, String>> calendarLocales) {
        Multimap<String, String> calendarToLocales =
                TreeMultimap.create(BETTER_CALENDAR, BETTER_LOCALE);

        calendarLocales.stream().forEach(x -> calendarToLocales.put(x.getSecond(), x.getFirst()));
        return Joiners.TAB.join(calendarToLocales.asMap().entrySet());

        //        Output<Pair<String,String>> best = new
        // Output<>(calendarLocales.iterator().next()); // default to first
        //        calendarLocales.stream().forEach(x -> {
        //            if (better(x, best.value) > 0) {
        //                best.value=x;
        //                }});
        //        return  Joiners.TAB.join(calendarLocales.size(), best.value.getFirst(),
        // best.value.getSecond());
    }

    private static final Comparator<String> BETTER_LOCALE =
            new Comparator<>() {

                @Override
                public int compare(String o1, String o2) {
                    return ComparisonChain.start()
                            .compareTrueFirst(o1.equals("root"), o2.equals("root"))
                            .compareTrueFirst(o1.equals("en"), o2.equals("en"))
                            .compare(o1, o2)
                            .result();
                }
            };

    private static final Comparator<String> BETTER_CALENDAR =
            new Comparator<>() {

                @Override
                public int compare(String o1, String o2) {
                    return ComparisonChain.start()
                            .compareTrueFirst(o1.equals("gregorian"), o2.equals("gregorian"))
                            .compareTrueFirst(o1.equals("generic"), o2.equals("generic"))
                            .compare(o1, o2)
                            .result();
                }
            };

    private static int better(Pair<String, String> a, Pair<String, String> b) {
        return ComparisonChain.start()
                .compare(a.getFirst().equals("root"), b.getFirst().equals("root"))
                .compare(a.getFirst().equals("en"), b.getFirst().equals("en"))
                .compare(a.getSecond().equals("gregorian"), b.getSecond().equals("gregorian"))
                .compare(a.getSecond().equals("generic"), b.getSecond().equals("generic"))
                .result();
    }

    private static Comparator<String> SKELETON_COMPARE = RelatedDatePathValues.SKELETON_COMPARE;

    private static ImmutableMultimap<String, Pair<String, String>> getIds(
            Iterable<String> locales) {
        Multimap<String, Pair<String, String>> results =
                TreeMultimap.create(SKELETON_COMPARE, Comparator.naturalOrder());

        for (String locale : locales) {
            CLDRFile source = FACTORY.make(locale, false);
            for (String path : source) {
                if (!path.startsWith("//ldml/dates/calendars/")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                SkeletonType id = SkeletonType.from(parts);
                if (id == SkeletonType.none) {
                    continue;
                }
                String calendar = parts.getAttributeValue(3, "type");
                results.put(id.getSkeleton(parts), Pair.of(locale, calendar));
            }
        }
        return ImmutableMultimap.copyOf(results);
    }

    private enum SkeletonType {
        // datetimeSkeleton, should already be aligned; otherwise do later
        availableFormats(-1),
        intervalFormatItem(-2),
        none(Integer.MIN_VALUE);

        private int elementIndex;

        SkeletonType(int elementIndex) {
            this.elementIndex = elementIndex;
        }

        public static SkeletonType from(XPathParts parts) {
            if (parts.containsElement("availableFormats")) {
                return availableFormats;
                //            } else if (parts.containsElement("intervalFormatItem")) {
                //                return intervalFormatItem;
            } else {
                return none;
            }
        }

        public String getSkeleton(XPathParts parts) {
            switch (this) {
                case availableFormats:
                case intervalFormatItem:
                    return parts.getAttributeValue(elementIndex, "id");
                default:
                    throw new IllegalArgumentException();
            }
        }

        public XPathParts setSkeleton(XPathParts parts, String newSkeleton) {
            switch (this) {
                case availableFormats:
                    parts.putAttributeValue(elementIndex, "id", newSkeleton);
                    break;
                case intervalFormatItem:
                    parts.putAttributeValue(elementIndex, "id", newSkeleton);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return parts;
        }
    }
}
