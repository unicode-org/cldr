package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.MergeLists;
import org.unicode.cldr.util.MergeLists.MergeListException;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

public class CheckDatePatternOrder {
    private static final Joiner JOINER_TAB_N = Joiner.on("\n\t\t").useForNull("null");

    enum DatetimeGroup {
        era,
        date,
        dow,
        time,
        dayPeriod,
        dayPeriodL,
        zone
    }

    enum DatetimePart {
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
        dayperiodL("B", DatetimeGroup.dayPeriodL),
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

        static DatetimePart from(VariableField item) {
            return map.get(item.toString().charAt(0));
        }
    }

    static final Comparator<List<DatetimeGroup>> LongestThenOrdered =
            new Comparator<>() {

                @Override
                public int compare(List<DatetimeGroup> list1, List<DatetimeGroup> list2) {
                    int comp = list2.size() - list1.size();
                    if (comp != 0) {
                        return comp;
                    }

                    for (int i = 0; i < list1.size(); i++) {
                        int comparison = list1.get(i).compareTo(list2.get(i));
                        if (comparison != 0) {
                            return comparison;
                        }
                    }
                    return 0;
                }
            };

    static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();
    static final StandardCodes SC = StandardCodes.make();
    static final Factory CF = CLDR_CONFIG.getCldrFactory();

    private static final boolean DEBUG = false;

    public static void main(String[] args) {
        Set<String> modernModerateLocales =
                DEBUG
                        ? ImmutableSet.of("af", "en", "de", "fr", "ja")
                        : SC.getLocaleCoverageLocales(
                                Organization.cldr, EnumSet.of(Level.MODERN, Level.MODERATE));

        NameGetter namer = new NameGetter(CLDR_CONFIG.getEnglish());
        List<DatetimeGroup> ordering = new ArrayList<>();

        // initially, we are only doing gregorian

        for (String locale : modernModerateLocales) {
            Multimap<List<DatetimeGroup>, String> lists =
                    TreeMultimap.create(LongestThenOrdered, Comparator.naturalOrder());
            CLDRFile cldrFile = CF.make(locale, true);
            for (String path : cldrFile) {
                // only need to look at the following:
                // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats/dateFormatItem[@id="Bh"]
                // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/dateTimeFormatLength[@type="full"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"]
                XPathParts parts = XPathParts.getFrozenInstance(path);
                if (parts.size() < 4
                        || !"calendar".equals(parts.getElement(3))
                        || !"gregorian".equals(parts.getAttributeValue(3, "type"))) {
                    continue;
                }
                final boolean available = parts.contains("availableFormats");
                if (!available && !parts.contains("dateTimeFormat")) {
                    continue;
                }
                String id =
                        available
                                ? parts.getAttributeValue(6, "id")
                                : parts.getAttributeValue(5, "type");
                String value = cldrFile.getStringValue(path);
                FormatParser fp = new DateTimePatternGenerator.FormatParser();
                fp.set(value);
                DatetimeGroup last = null;
                ordering.clear();
                for (Object item : fp.getItems()) {
                    if (!(item instanceof VariableField)) {
                        continue;
                    }
                    DatetimePart dc = DatetimePart.from((VariableField) item);
                    if (dc.bdc != last) { // don't worry about multiple instances
                        ordering.add(dc.bdc);
                        last = dc.bdc;
                    }
                }
                if (id.equals("Ed")) {
                    continue;
                }
                if (ordering.size() > 1) {
                    lists.put(List.copyOf(ordering), id + "⇒«" + value+"»");
                }
            }

            // Now we see if there is a consistent ordering among elements within the calendar
            MergeLists<DatetimeGroup> mergeList = new MergeLists<>();
            for (List<DatetimeGroup> key : lists.keySet()) {
                try {
                    mergeList.add(key);
                } catch (Exception e) {
                    JOINER_TAB_N.join(
                            "\t###" + e.getMessage(),
                            namer.getNameFromIdentifier(locale),
                            locale,
                            key);
                }
            }
            try {
                List<DatetimeGroup> result = mergeList.merge();
                System.out.println(
                        Joiners.TAB.join(namer.getNameFromIdentifier(locale), locale, result));
            } catch (MergeListException e) {
                System.out.println(
                    JOINER_TAB_N.join(
                        Joiners.TAB.join(
                                namer.getNameFromIdentifier(locale),
                                locale,
                                e.getMessage()),
                        JOINER_TAB_N.join(filter(lists.asMap().entrySet(), e.problems))));
            }
        }
    }

    private static Iterable<? extends @Nullable Object> filter(Set<Entry<List<DatetimeGroup>, Collection<String>>> entrySet, Collection problems) {
       return entrySet.stream().filter(x -> filter(x, problems)).collect(Collectors.toList());
    }

    private static boolean filter(Entry<List<DatetimeGroup>, Collection<String>> x, Collection problems) {
        List<DatetimeGroup> list = x.getKey();
        for (Collection<DatetimeGroup> problem : (Collection<Collection<DatetimeGroup>>)problems) {
            if (containsSubsequence(list, problem)) {
                return true;
            }
        }
        return false;
    }
    
    /** 
     * Returns true if mainlist contains a sublist (not necessarily adjacent) in iteration order. They don't
     * have to be lists, as long as they have determinant order.
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
}
