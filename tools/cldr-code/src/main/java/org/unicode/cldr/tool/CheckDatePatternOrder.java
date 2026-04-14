package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.OutputInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DatetimeUtilities;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.MergeLists;
import org.unicode.cldr.util.MergeLists.MergeListException;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.NestedMap.Map3;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

/**
 * This class is used to investigate consistencies or lack thereof between the stock formats,
 * available formats, and interval formats; consistencies within those types of items. Some of the
 * code may, in the future, be moved to ST checks, unit test, or other software.
 */
public class CheckDatePatternOrder {

    private static final boolean DEBUG = false;
    private static final boolean TRY_FIXING = System.getProperty("TRY_FIXING") != null;

    public enum DatetimeGroup {
        era,
        date,
        dateMMM, // string month, eg MMM or MMMM
        dow,
        time,
        dayPeriod,
        dayPeriodB, // non-am/pm day period
        zone
    }

    public enum DatetimePart {
        era("G", DatetimeGroup.era),
        year("yYurU", DatetimeGroup.date),
        quarter("Qq", DatetimeGroup.date),
        month("MLl", DatetimeGroup.date),
        week_of_year("w", DatetimeGroup.date),
        week_of_month("W", DatetimeGroup.date),
        weekday("Ece", DatetimeGroup.dow),
        day("dg", DatetimeGroup.date),
        day_of_year("D", DatetimeGroup.date),
        day_of_week_in_month("F", DatetimeGroup.date),
        dayperiod("ab", DatetimeGroup.dayPeriod),
        dayperiodL("B", DatetimeGroup.dayPeriodB),
        hour("HkhK", DatetimeGroup.time),
        minute("m", DatetimeGroup.time),
        second("sA", DatetimeGroup.time),
        fractional_second("S", DatetimeGroup.time),
        zone("vzZOVXx", DatetimeGroup.zone);

        private final String items;
        private final DatetimeGroup bdc;

        private DatetimePart(String items, DatetimeGroup bdc) {
            this.items = items;
            this.bdc = bdc;
        }

        static UnicodeMap<DatetimePart> map = new UnicodeMap<>();

        static {
            for (DatetimePart dc : values()) {
                map.putAll(new UnicodeSet().addAll(dc.items), dc);
            }
            map.freeze();
        }

        @SuppressWarnings("deprecation")
        static DatetimePart from(VariableField item) {
            return map.get(item.toString().charAt(0));
        }
    }

    @SuppressWarnings("deprecation")
    static final Comparator<VariableField> VariableFieldComparator =
            Comparator //
                    .comparing(VariableField::getType)
                    .thenComparing(VariableField::isNumeric)
                    .thenComparing(VariableField::toString);

    static final Comparator<String> PatternComparator = DatetimeUtilities.PATTERN_COMPARATOR;

    //            Comparator.comparing(
    //                    x -> getSortedVariableFields(x),
    //                    Comparators.lexicographical(VariableFieldComparator));

    static final SortedSet<VariableField> getSortedVariableFields(String pattern) {
        FormatParser fp = new DateTimePatternGenerator.FormatParser();
        fp.set(pattern);
        return fp.getItems().stream()
                .map(x -> (VariableField) x)
                .collect(ImmutableSortedSet.toImmutableSortedSet(VariableFieldComparator));
    }

    static final class LongestFirst<T extends Comparable<T>, U extends Collection<T>>
            implements Comparator<U> {

        @Override
        public int compare(U o1, U o2) {
            int comp = o1.size() - o2.size();
            if (comp != 0) {
                return comp;
            }
            Iterator<T> it1 = o1.iterator();
            Iterator<T> it2 = o2.iterator();
            while (it1.hasNext()) {
                int comparison = it1.next().compareTo(it2.next());
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }
    }

    static final Comparator<List<DatetimeGroup>> LongestThenOrderedDTGList = new LongestFirst<>();
    static final Comparator<List<DatetimeGroup>> LongestThenOrderedStringList =
            new LongestFirst<>();

    static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();
    static final StandardCodes SC = StandardCodes.make();
    static final Factory CF = CLDR_CONFIG.getCldrFactory();

    public static final boolean mappedEqual(
            String a, String b, Function<String, String> normalizer) {
        return Objects.equal(normalizer.apply(a), (normalizer.apply(b)));
    }

    public static final Function<String, String> FIX_SPACE =
            x -> x == null ? null : x.replaceAll("\\h+", " ");

    static class DataGathered {

        Set<String> badStockskeletons = new LinkedHashSet<>();
        Set<String> goodStockSkeletons = new LinkedHashSet<>();

        Counter<String> filteredCount = new Counter<>();
        Counter<String> filteredPatternCount = new Counter<>();
        Multimap<List<DatetimeGroup>, String> orderingToLocales =
                TreeMultimap.create(LongestThenOrderedDTGList, Comparator.naturalOrder());
        Multimap<List<DatetimeGroup>, String> conflictingOrderingsToLocales =
                TreeMultimap.create(LongestThenOrderedDTGList, Comparator.naturalOrder());
        int patternCount = 0;
        Multimap<String, List<String>> replacements = LinkedHashMultimap.create();
        List<String> mergeData = new ArrayList<>();

        void gatherDataFor(String locale, String calendar) {
            DataGathered dg = this;
            Multimap<List<DatetimeGroup>, String> lists =
                    TreeMultimap.create(LongestThenOrderedDTGList, Comparator.naturalOrder());
            CLDRFile cldrFile = CF.make(locale, true);

            // Gather patterns where the stock value would not be generated by the availableFormats

            DatetimeUtilities.DatePatternInfo dtiRaw =
                    DatetimeUtilities.DatePatternInfo.from(cldrFile, calendar);
            // try fixing data!
            DatetimeUtilities.DatePatternInfo dti =
                    TRY_FIXING ? dtiRaw.getWithAvailableReplaced(locale, dg.replacements) : dtiRaw;

            if (!dti.stockMatchesGenerated()) {
                dg.badStockskeletons.add(dti.getStockDelta(locale));
            } else {
                dg.goodStockSkeletons.add(locale);
            }

            SetView<Entry<String, String>> union =
                    Sets.union(
                            dti.getAvailableSkeletonToPattern().entrySet(),
                            dti.getStockSkeletonToPattern().entrySet());
            for (Entry<String, String> skeletonAndPattern : union) {
                String skeleton = skeletonAndPattern.getKey();
                if (skeleton.equals("Ed")) { // hack, because these appear to be randomish
                    continue;
                }
                String pattern = skeletonAndPattern.getValue();
                List<DatetimeGroup> ordering = getOrderingFromPattern(pattern);
                if (ordering.size() > 1) {
                    lists.put(List.copyOf(ordering), skeleton + "⇒«" + pattern + "»");
                }
            }

            // Now we see if there is a consistent ordering among elements within the calendar
            String mergeMessage = "";
            MergeLists<DatetimeGroup> mergeList =
                    new MergeLists<>(new TreeSet<DatetimeGroup>(), new TreeSet<DatetimeGroup>());
            for (List<DatetimeGroup> key : lists.keySet()) {
                try {
                    mergeList.add(key);
                } catch (Exception e) {
                    // These are all strange cases, like day month era year => [date era date]
                    // Skip for now
                }
            }
            try {
                List<DatetimeGroup> result = mergeList.merge();
                mergeData.add(
                        Joiners.TAB.join(
                                namer.getNameFromIdentifier(locale), locale, result, "none"));
                dg.filteredCount.add(locale, 0);
                dg.orderingToLocales.put(result, locale);
            } catch (MergeListException e) {
                List<List<DatetimeGroup>> minimizedProblems =
                        minimize(createTypedList(e.problems, DatetimeGroup.class));
                List<DatetimeGroup> intersection =
                        minimizedProblems.stream()
                                .collect(
                                        () -> new ArrayList<>(minimizedProblems.get(0)),
                                        ArrayList::retainAll,
                                        ArrayList::retainAll);

                final List<Entry<List<DatetimeGroup>, Collection<String>>> filtered =
                        filter(lists.asMap().entrySet(), minimizedProblems);
                dg.filteredCount.add(locale, filtered.size() - 1);
                filtered.stream()
                        .forEach(
                                x -> {
                                    dg.filteredPatternCount.add(locale, x.getValue().size());
                                    dg.conflictingOrderingsToLocales.put(x.getKey(), locale);
                                });

                mergeData.add(
                        mergeMessage
                                + Joiners.TAB.join(
                                        namer.getNameFromIdentifier(locale),
                                        locale,
                                        e.partialResult + "👹" + e.orderedWorkingSet,
                                        intersection,
                                        minimizedProblems));

                filtered.stream()
                        .forEach(
                                x ->
                                        mergeData.add(
                                                Joiners.TAB.join(
                                                        namer.getNameFromIdentifier(locale),
                                                        locale,
                                                        x)));
            }
            dg.patternCount +=
                    (dti.getAvailableSkeletonToPattern().size()
                            + dti.getStockSkeletonToPattern().size());
        }

        void showData() {
            System.out.println("\nMerge Data\n");
            mergeData.stream().forEach(System.out::println);

            System.out.println("\nReplacements\n");
            replacements.asMap().entrySet().stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x.getKey() + Joiner.on("\n\t").join(x.getValue())));

            System.out.println("\nOK Orders to locales\n");
            orderingToLocales.asMap().entrySet().stream()
                    .forEach(x -> System.out.println(x.getKey() + "\t" + x.getValue()));

            System.out.println("\nConflict count to locales\n");
            filteredCount.getEntrySetSortedByCount(false, null).stream()
                    .forEach(
                            x -> {
                                if (x.get0() > 0)
                                    System.out.println(
                                            Joiners.TAB.join(
                                                    x.get0(),
                                                    x.get1(),
                                                    namer.getNameFromIdentifier(x.get1())));
                            });
            System.out.println("\nConflict order totals\t" + filteredCount.getTotal());
            System.out.println("\nConflict pattern totals\t" + filteredPatternCount.getTotal());
            System.out.println("\nTotal Patterns:\t" + patternCount);

            System.out.println("\nConflicting orders to locales\n");
            conflictingOrderingsToLocales.asMap().entrySet().stream()
                    .forEach(x -> System.out.println(x.getKey() + "\t" + x.getValue()));

            System.out.println("\nStock conflicts with Available\t" + badStockskeletons.size());
            System.out.println(
                    Joiners.TAB.join(
                                    "Not conflicting",
                                    goodStockSkeletons.size(),
                                    goodStockSkeletons)
                            + "\n");
            System.out.println(DatetimeUtilities.DatePatternInfo.header("Locale"));
            badStockskeletons.stream().forEach(System.out::println);
        }
    }

    static NameGetter namer = new NameGetter(CLDR_CONFIG.getEnglish());

    public static void main(String[] args) {
        System.out.println("Arguments: outlying, missing, or order.");
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing arg");
        }
        for (String arg : args) {
            switch (arg) {
                case "outlying":
                    checkForOutlyingPatterns2();
                    break;
                case "missing":
                    checkMissingDateFormatItems();
                    break;
                case "order":
                    checkDateOrder();
                    break;
                case "misc":
                    misc();
                    break;
                default:
                    throw new IllegalArgumentException("Illegal arg: " + arg);
            }
        }
    }

    private static void checkForOutlyingPatterns2() {
        Map<Pair<String, CheckStyle>, Multimap<String, String>> idToCheckStyleToCalendarToLocales =
                new HashMap<>();
        for (CheckStyle cs : CheckStyle.values()) {
            Map<String, Multimap<String, String>> temp = checkForOutlyingPatterns(cs);
            System.out.println(temp.size());
            for (Entry<String, Multimap<String, String>> entry : temp.entrySet()) {
                String id = entry.getKey();
                Multimap<String, String> calendarToLocales = entry.getValue();
                idToCheckStyleToCalendarToLocales.put(Pair.of(id, cs), calendarToLocales);
            }
        }
        // sort for display
        final Comparator<Pair<String, CheckStyle>> PAIR_COMPARATOR =
                Comparator.comparing(
                                (Pair<String, CheckStyle> p) -> p.getFirst(), PatternComparator)
                        .thenComparing(Pair::getSecond, Comparator.naturalOrder());

        TreeSet<Pair<String, CheckStyle>> sorted = new TreeSet<>(PAIR_COMPARATOR);
        sorted.addAll(idToCheckStyleToCalendarToLocales.keySet());
        for (Pair<String, CheckStyle> key : sorted) {
            Map<String, Collection<String>> calendarToLocales =
                    idToCheckStyleToCalendarToLocales.get(key).asMap();
            calendarToLocales.remove("iso8601");
            if (calendarToLocales.isEmpty()) {
                continue;
            }
            System.out.println(
                    Joiners.TAB.join(
                            key.getFirst(),
                            key.getSecond(),
                            Joiners.SP.join(calendarToLocales.entrySet())));
        }
    }

    private static void checkDateOrder() {
        // initially, we are only doing gregorian
        Set<String> modernModerateLocales =
                DEBUG
                        ? ImmutableSet.of("af", "en", "de", "fr", "ja")
                        : SC.getLocaleCoverageLocales(
                                Organization.cldr, EnumSet.of(Level.MODERN, Level.MODERATE));

        DataGathered dg = new DataGathered();

        for (String locale : modernModerateLocales) {
            dg.gatherDataFor(locale, "gregorian");
        }
        dg.showData();
    }

    @SuppressWarnings("deprecation")
    private static List<DatetimeGroup> getOrderingFromPattern(String pattern) {
        FormatParser fp = new DateTimePatternGenerator.FormatParser();
        fp.set(pattern);
        DatetimeGroup last = null;
        List<DatetimeGroup> ordering = new ArrayList<>();
        for (Object item : fp.getItems()) {
            if (!(item instanceof VariableField)) {
                continue;
            }
            final VariableField vf = (VariableField) item;
            DatetimePart dc = DatetimePart.from(vf);
            DatetimeGroup bdc = dc.bdc;
            // change numeric date to dateN
            if (dc == DatetimePart.month && !vf.isNumeric()) {
                bdc = DatetimeGroup.dateMMM;
            }

            if (bdc != last) { // don't worry about multiple instances
                // also change sequences of date dateN or dateN date to dateN
                if (last == DatetimeGroup.date && bdc == DatetimeGroup.dateMMM) { // patch up
                    ordering.set(ordering.size() - 1, bdc);
                    last = DatetimeGroup.dateMMM;
                } else if (last == DatetimeGroup.dateMMM && bdc == DatetimeGroup.date) {
                    // skip
                } else {
                    ordering.add(bdc);
                    last = bdc;
                }
            }
        }
        return ordering;
    }

    private static <T> List<Entry<List<DatetimeGroup>, Collection<String>>> filter(
            Set<Entry<List<DatetimeGroup>, Collection<String>>> entrySet, List<List<T>> problems) {
        return entrySet.stream().filter(x -> filter(x, problems)).collect(Collectors.toList());
    }

    @SuppressWarnings("deprecation")
    private static <T> List<List<T>> minimize(List<List<T>> problems) {
        // MergeLists stops at the first point where there is a problem.
        // However, we can refine the list down to just the items that have direct conflicts because
        // of a cycle:
        // That is, where A < B <...< C < A
        // So if X is not part of a cycle, we can eliminate it.
        // Simplest case is X is always at the end
        // Example:
        //        [era, date, dow]=[GyMEd⇒«G y-MM-dd, E»]
        //        [dow, date, era]=[GyMMMEd⇒«E dd MMM y G»]
        //        [dow, time, dayPeriod]=[Ehms⇒«E hh:mm:ss a», Ehm⇒«E hh:mm a», Eh⇒«E h a»]
        //        [dow, time, dayPeriodL]=[EBhms⇒«E hh:mm:ss B», EBhm⇒«E hh:mm B», EBh⇒«E h B»]
        //        [time, dayPeriod, zone]=[hmsv⇒«h:mm:ss a v», hmv⇒«h:mm a v», hv⇒«h a v»]

        // not particularly optimized, but we don't care.
        // find out which items are only the end, by creating a multimap
        while (true) {
            Multimap<T, T> afterItems = LinkedHashMultimap.create();
            problems.stream()
                    .forEach(
                            x -> {
                                addAfter(x, afterItems);
                            });
            List<List<T>> revision = new ArrayList<List<T>>();
            OutputInt shorter = new OutputInt(0);
            // remove all the items that have nothing after
            problems.stream()
                    .forEach(
                            x -> {
                                if (!afterItems.containsKey(x.get(x.size() - 1))) {
                                    if (x.size() > 2) {
                                        revision.add(x.subList(0, x.size() - 1));
                                    }
                                    shorter.value = 1;
                                } else {
                                    revision.add(x);
                                }
                            });
            if (shorter.value == 0) {
                break;
            }
            problems = revision; // repeat until we can't shorten
        }
        // remove duplicates
        return List.copyOf(new LinkedHashSet<>(problems));
    }

    private static <T> void addAfter(List<T> x, Multimap<T, T> afterItems) {
        if (x.size() > 1) {
            T first = x.get(0);
            List<T> after = x.subList(1, x.size());
            afterItems.putAll(first, after);
            addAfter(after, afterItems);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> boolean filter(Entry<List<T>, Collection<String>> x, Collection problems) {
        List<T> list = x.getKey();
        for (Collection<T> problem : (Collection<Collection<T>>) problems) {
            if (containsSubsequence(list, problem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if mainlist contains a sublist (not necessarily adjacent) in iteration order.
     * They don't have to be lists, as long as they have determinant order.
     */
    public static <T> boolean containsSubsequence(Collection<T> mainList, Collection<T> subList) {
        if (subList.isEmpty()) return true;

        Iterator<T> mainIter = mainList.iterator();
        for (T target : subList) {
            boolean foundMatch = false;
            while (mainIter.hasNext()) {
                final T source = mainIter.next();
                if (Objects.equal(source, target)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) return false; // Target element not found in remaining list
        }
        return true;
    }

    // Need this hack because exceptions can't use generics

    @SuppressWarnings("rawtypes")
    public static <T> List<List<T>> createTypedList(
            Collection<Collection> sourceCollection, Class<T> classType) {
        // Use Java 8 streams to filter and cast each element
        List<List<T>> resultList =
                sourceCollection.stream()
                        .map(ArrayList<T>::new) // Create a new ArrayList from each inner collection
                        .collect(Collectors.toList()); // Collect the results into an outer List
        return resultList;
    }

    public static boolean equalIgnoringSpaceVariants(String pattern, String patternForBase) {
        return mappedEqual(pattern, patternForBase, FIX_SPACE);
    }

    public static <K, V> Map<K, Set<V>> from(Multimap<K, V> source) {
        LinkedHashMap<K, Set<V>> result = new LinkedHashMap<>();
        for (Entry<K, Collection<V>> entry : source.asMap().entrySet()) {
            result.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }
        return ImmutableMap.copyOf(result);
    }

    private static Map<String, Set<String>> getCalendarToIds(CLDRFile cldrFile) {
        CLDRFile rootResolved = cldrFile;
        Multimap<String, String> calendarToIds = TreeMultimap.create();

        for (String path : rootResolved.iterableWithoutExtras()) {
            if (!path.contains("availableFormats")) {
                continue;
            }
            // ldml/dates/calendars/calendar[@type="chinese"]/dateTimeFormats/availableFormats/dateFormatItem[@id="Bh"];
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String calendar = parts.findAttributes("calendar").get("type");
            String id = parts.findAttributes("dateFormatItem").get("id");
            calendarToIds.put(calendar, id);
        }
        return from(calendarToIds);
    }

    enum CheckStyle {
        root,
        englishMinusRoot,
        othersMinusEnglish
    }

    private static Map<String, Multimap<String, String>> checkForOutlyingPatterns(
            CheckStyle checkStyle) {
        System.out.println(checkStyle);
        Map<String, Set<String>> baseCalendarToIds;
        Set<String> sorted;
        switch (checkStyle) {
            case root:
                {
                    baseCalendarToIds = new TreeMap<>();
                    sorted = Set.of("root");
                    break;
                }
            case englishMinusRoot:
                {
                    baseCalendarToIds = getCalendarToIds(CF.make("root", true));
                    sorted = Set.of("en");
                    break;
                }
            case othersMinusEnglish:
                {
                    baseCalendarToIds = getCalendarToIds(CF.make("en", true));
                    Set<String> levelsToLocales =
                            StandardCodes.make()
                                    .getLevelsToLocalesFor(Organization.cldr)
                                    .get(Level.MODERN);
                    sorted = new TreeSet<>(levelsToLocales);
                    break;
                }
            default:
                throw new IllegalArgumentException();
        }
        Map<String, Multimap<String, String>> missingIdCalendarLocale = new TreeMap<>();
        for (String locale : sorted) {
            Map<String, Set<String>> calendarToIds = getCalendarToIds(CF.make(locale, false));
            System.out.println(locale);
            for (Entry<String, Set<String>> entry : calendarToIds.entrySet()) {
                String calendar = entry.getKey();
                Set<String> ids = entry.getValue();
                Set<String> rootIds =
                        baseCalendarToIds.isEmpty() ? Set.of() : baseCalendarToIds.get(calendar);
                SetView<String> missing = Sets.difference(ids, rootIds);
                for (String item : missing) {
                    Multimap<String, String> submap = missingIdCalendarLocale.get(item);
                    if (submap == null) {
                        missingIdCalendarLocale.put(item, submap = TreeMultimap.create());
                    }
                    submap.put(calendar, locale);
                }
            }
        }
        return missingIdCalendarLocale;
    }

    private static Multimap<String, SkeletonList> getCalendarToSkeletonLists(CLDRFile cldrFile) {
        CLDRFile rootResolved = cldrFile;
        Multimap<String, SkeletonList> calendarToIds = TreeMultimap.create();

        for (String path : rootResolved.iterableWithoutExtras()) {
            if (!path.contains("availableFormats")) {
                continue;
            }
            // ldml/dates/calendars/calendar[@type="chinese"]/dateTimeFormats/availableFormats/dateFormatItem[@id="Bh"];
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String calendar = parts.findAttributes("calendar").get("type");
            String id = parts.findAttributes("dateFormatItem").get("id");
            calendarToIds.put(calendar, SkeletonList.from(id));
        }
        return calendarToIds;
    }

    private static void checkMissingDateFormatItems() {
        CLDRFile root = CF.make("root", false);
        // get all the calendar data
        Set<String> calendars = new TreeSet<>();
        for (String path : root.iterableWithoutExtras()) {
            if (!path.contains("availableFormats")) {
                continue;
            }
            // ldml/dates/calendars/calendar[@type="chinese"]/dateTimeFormats/availableFormats/dateFormatItem[@id="Bh"];
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String calendar = parts.findAttributes("calendar").get("type");
            calendars.add(calendar);
        }

        // add aliases

        Multimap<String, SkeletonList> calendarToIds =
                getCalendarToSkeletonLists(CF.make("root", true));

        Set<SkeletonList> fullList =
                Set.of("GyMMMEd", "GyMEd", "Ehmsv", "EBhmsv", "EHmsv").stream()
                        .map(x -> SkeletonList.from(x))
                        .collect(Collectors.toSet());
        for (Entry<String, Collection<SkeletonList>> entry : calendarToIds.asMap().entrySet()) {
            String calendar = entry.getKey();
            Set<SkeletonList> ids = Set.copyOf(entry.getValue());
            System.out.println(calendar + "—all\t" + new TreeSet<>(ids));

            Multimap<SkeletonList, SkeletonList> failureToReason = TreeMultimap.create();
            Map<SkeletonList, Boolean> found = new HashMap<>();

            // check for full lists:
            SetView<SkeletonList> notHandled = Sets.difference(fullList, ids);

            notHandled.stream()
                    .forEach(
                            x -> {
                                System.out.println("missing 1\t" + x);
                                found.put(x, false);
                            });

            // check that every set has every subset of size-1 in the set
            for (SkeletonList id : ids) {
                if (id.data.size() <= 2) {
                    continue;
                }
                for (String item : id.data) {
                    SetView<String> diff = Sets.difference(id.data, Set.of(item));
                    String pattern =
                            Joiners.ES.join(
                                    diff); // used to just create from the diff, but oddities
                    // happened.
                    SkeletonList subset = new SkeletonList(pattern);

                    if (found.containsKey(subset)) {
                        continue;
                    }
                    boolean ok = subset.isInvalid() || ids.contains(subset);
                    found.put(subset, ok);
                    if (!ok) {
                        failureToReason.put(subset, id);
                        // debugging
                        subset.isInvalid();
                        ids.contains(subset);
                    }
                }
            }
            failureToReason.asMap().entrySet().forEach(x -> System.out.println("missing 2\t" + x));
        }
    }

    static class SkeletonList implements Comparable<SkeletonList> {
        String sortKey;
        Set<String> data = new TreeSet<>();
        static ConcurrentHashMap<String, SkeletonList> cache = new ConcurrentHashMap<>();

        SkeletonList(String pattern) {
            FormatParser fp = new DateTimePatternGenerator.FormatParser();
            fp.set(pattern);
            Set<String> _data = new TreeSet<>();
            for (Object item : fp.getItems()) {
                _data.add(item.toString()); // since these are all skeletons, there are no literals
            }
            data = ImmutableSet.copyOf(_data);
            sortKey = Joiner.on("").join(_data);
        }

        public int size() {
            return data.size();
        }

        static SkeletonList from(String pattern) {
            return cache.computeIfAbsent(pattern, x -> new SkeletonList(x));
        }

        @Override
        public boolean equals(Object obj) {
            return sortKey.equals(((SkeletonList) obj).sortKey);
        }

        @Override
        public int compareTo(SkeletonList other) {
            return sortKey.compareTo(other.sortKey);
        }

        @Override
        public String toString() {
            return data.toString();
        }

        @Override
        public int hashCode() {
            return sortKey.hashCode();
        }

        boolean isInvalid() {
            return data.contains("G") && !data.contains("y")
                    || data.contains("y")
                            && data.contains("d")
                            && !(data.contains("M") || data.contains("MMM"))
                    || data.contains("E")
                            && !data.contains("d")
                            && !data.contains("h")
                            && !data.contains("H")
                    || data.contains("s")
                            && !data.contains("m")
                            && (data.contains("h") || data.contains("H"))
                    || data.contains("v") && !data.contains("h") && !data.contains("H")
                    || data.contains("B") && !data.contains("h");
        }
    }

    static void misc() {
        Map3<String, Integer, String, Integer> foo = Map3.create(ConcurrentSkipListMap::new);
        int i2 = 0;
        for (String s : Arrays.asList("c", "b", "a")) {
            for (Integer i : Arrays.asList(2, 0, 1)) {
                for (String s2 : Arrays.asList("x", "z", "y")) {
                    foo.put(s, i, s2, i2++);
                }
            }
        }
        foo.stream().forEach(System.out::println);
    }
}
