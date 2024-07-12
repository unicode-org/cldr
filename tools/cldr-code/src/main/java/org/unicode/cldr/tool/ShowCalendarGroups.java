package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.DangiCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IndianCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TaiwanCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CldrUtility;

/** Utility to compute calendar groups based on months & days per month */
public class ShowCalendarGroups {
    // Current output: [calendars]<tab>fingerprint
    // where the fingerprint is:
    //    months, max_days_per_month, details
    // where the details are {month1=[days1, days2, ..], ...}, where the monthX = [...] are
    // suppressed for brevity when monthX = [max_days_per_month]
    //
    //    [chinese, dangi] {12, 30, {1=[29, 30], 2=[29, 30], 3=[29, 30], 4=[29, 30], 5=[29, 30],
    // 6=[29, 30], 7=[29, 30], 8=[29, 30], 9=[29, 30], 10=[29, 30], 11=[29, 30], 12=[29, 30]}}
    //    [islamic, islamic-civil, islamic-rgsa, islamic-tbla, islamic-umalqura]  {12, 30, {2=[29],
    // 4=[29], 6=[29], 8=[29], 10=[29], 12=[29, 30]}}
    //    [indian]    {12, 31, {1=[30, 31], 7=[30], 8=[30], 9=[30], 10=[30], 11=[30], 12=[30]}}
    //    [gregorian, iso8601, buddhist, japanese, roc]   {12, 31, {2=[28, 29], 4=[30], 6=[30],
    // 9=[30], 11=[30]}}
    //    [persian]   {12, 31, {7=[30], 8=[30], 9=[30], 10=[30], 11=[30], 12=[29, 30]}}
    //    [hebrew]    {13, 30, {2=[29, 30], 3=[29, 30], 4=[29], 6=[29, 30], 7=[29], 9=[29], 11=[29],
    // 13=[29]}}
    //    [coptic, ethiopic, ethiopic-amete-alem] {13, 30, {13=[5, 6]}}

    public static void main(String[] args) {
        TreeMultimap<Footprint, CalType> footprintToCalendar = TreeMultimap.create();
        for (CalType calType : CalType.values()) {
            Calendar cal2 = makeCalendar(calType, TimeZone.GMT_ZONE, ULocale.ENGLISH);
            Footprint footPrint = new Footprint(cal2);
            footprintToCalendar.put(footPrint, calType);
        }
        System.out.println();
        for (Entry<Footprint, Collection<CalType>> entry : footprintToCalendar.asMap().entrySet()) {
            System.out.println(
                    entry.getValue().stream().map(x -> x.getId()).collect(Collectors.toList())
                            + "\t"
                            + entry.getKey());
        }
    }

    static class Footprint implements Comparable<Footprint> {
        final int maxMonths;
        final int maxDaysPerMonth;
        final Multimap<Integer, Integer> daysInMonth;

        static Date d = new Date(2000 - 1900, 0, 1, 0, 0, 0);

        public Footprint(Calendar cal2) {
            maxMonths = cal2.getMaximum(Calendar.MONTH) + 1;
            maxDaysPerMonth = cal2.getMaximum(Calendar.DAY_OF_MONTH);
            final Multimap<Integer, Integer> _daysInMonth = TreeMultimap.create();
            cal2.setTime(d); // year may not be gregorian

            final int startYear = cal2.get(Calendar.YEAR);
            cal2.set(Calendar.DAY_OF_MONTH, 1);
            for (int year = startYear; year < startYear + 100; ++year) {
                cal2.set(Calendar.YEAR, year);
                for (int month = 1; month <= maxMonths; ++month) {
                    cal2.set(Calendar.MONTH, month - 1);
                    _daysInMonth.put(month, cal2.getActualMaximum(Calendar.DAY_OF_MONTH));
                }
            }
            Set<Integer> maxSingleton = ImmutableSet.of(maxDaysPerMonth);
            // filter out cases where the month has only the max
            final Multimap<Integer, Integer> _daysInMonth2 = TreeMultimap.create();
            _daysInMonth.asMap().values().removeIf(value -> value.equals(maxSingleton));
            daysInMonth = CldrUtility.protectCollection(_daysInMonth);
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((Footprint) obj) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxMonths, maxDaysPerMonth);
        }

        @Override
        public int compareTo(Footprint o) {
            return ComparisonChain.start()
                    .compare(maxMonths, o.maxMonths)
                    .compare(maxDaysPerMonth, o.maxDaysPerMonth)
                    .compare(daysInMonth.entries(), o.daysInMonth.entries(), LIST_ENTRY_COMP)
                    .result();
        }

        @Override
        public String toString() {
            return "{" + Joiner.on(", ").join(maxMonths, maxDaysPerMonth, daysInMonth) + "}";
        }

        static Comparator<Map.Entry<Integer, Integer>> ENTRY_COMP =
                new Comparator<>() {
                    @Override
                    public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                        return ComparisonChain.start()
                                .compare(o1.getKey(), o2.getKey())
                                .compare(o1.getValue(), o2.getValue())
                                .result();
                    }
                };
        static Comparator<Iterable<Map.Entry<Integer, Integer>>> LIST_ENTRY_COMP =
                Comparators.lexicographical(ENTRY_COMP);
    }

    // This is not visible in ICU, so cloning here
    static Calendar makeCalendar(CalType calType, TimeZone zone, ULocale locale) {
        Calendar cal = null;
        switch (calType) {
            case ISO8601:
            case GREGORIAN:
                cal = new GregorianCalendar(zone, locale);
                break;
            case BUDDHIST:
                cal = new BuddhistCalendar(zone, locale);
                break;
            case CHINESE:
                cal = new ChineseCalendar(zone, locale);
                break;
            case COPTIC:
                cal = new CopticCalendar(zone, locale);
                break;
            case DANGI:
                cal = new DangiCalendar(zone, locale);
                break;
            case ETHIOPIC:
                cal = new EthiopicCalendar(zone, locale);
                break;
            case ETHIOPIC_AMETE_ALEM:
                cal = new EthiopicCalendar(zone, locale);
                ((EthiopicCalendar) cal).setAmeteAlemEra(true);
                break;
            case HEBREW:
                cal = new HebrewCalendar(zone, locale);
                break;
            case INDIAN:
                cal = new IndianCalendar(zone, locale);
                break;
            case ISLAMIC_CIVIL:
            case ISLAMIC_UMALQURA:
            case ISLAMIC_TBLA:
            case ISLAMIC_RGSA:
            case ISLAMIC:
                cal = new IslamicCalendar(zone, locale);
                break;
            case JAPANESE:
                cal = new JapaneseCalendar(zone, locale);
                break;
            case PERSIAN:
                cal = new PersianCalendar(zone, locale);
                break;
            case ROC:
                cal = new TaiwanCalendar(zone, locale);
                break;

            default:
                // we must not get here, because unknown type is mapped to
                // Gregorian at the beginning of this method.
                throw new IllegalArgumentException("Unknown calendar type");
        }

        return cal;
    }
}
