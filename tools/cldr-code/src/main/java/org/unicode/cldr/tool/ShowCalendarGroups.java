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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
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
            footprintToCalendar.put(new Footprint(calType), calType);
        }
        System.out.println(
                "Calendars\tmax days/year\tmax months/year\tmax days/month\tdays/year\tmonths/year\tdays/month");
        for (Entry<Footprint, Collection<CalType>> entry : footprintToCalendar.asMap().entrySet()) {
            System.out.println(
                    entry.getValue().stream().map(x -> x.getId()).collect(Collectors.joining(" "))
                            + "\t"
                            + entry.getKey());
        }
    }

    public static class MultimapJoiner {
        final Joiner entriesJoiner;
        final Joiner entryJoiner;
        final Joiner entryValueJoiner;

        public MultimapJoiner(Joiner entriesJoiner, Joiner entryJoiner, Joiner entryValueJoiner) {
            this.entriesJoiner = entriesJoiner;
            this.entryJoiner = entryJoiner;
            this.entryValueJoiner = entryValueJoiner;
        }

        public <K, V> String join(Multimap<K, V> multimap) {
            Function<? super Map.Entry<K, Collection<V>>, String> fii =
                    x -> entryJoiner.join(x.getKey(), entryValueJoiner.join(x.getValue()));
            List<String> list =
                    multimap.asMap().entrySet().stream().map(fii).collect(Collectors.toList());
            return entriesJoiner.join(list);
        }
    }

    static class Footprint implements Comparable<Footprint> {
        private static final Joiner SPACE_JOINER = Joiner.on(' ');
        private static final Joiner EQ_JOINER = Joiner.on('=');
        private static final Joiner COMMA_JOINER = Joiner.on(',');
        private static final Joiner TAB_JOINER = Joiner.on("\t");
        private static final MultimapJoiner MM_JOINER =
                new MultimapJoiner(SPACE_JOINER, EQ_JOINER, COMMA_JOINER);
        final int maxMonthsPerYear;
        final Set<Integer> monthsInYear;
        final int maxDaysPerYear;
        final Set<Integer> daysInYear;
        final int maxDaysPerMonth;
        final Multimap<Integer, Integer> daysInMonths;
        final int hash;

        static Date d = new Date(1900 - 1900, 0, 1, 0, 0, 0);

        public Footprint(CalType calType) {
            if (calType.equals(CalType.CHINESE)) {
                int debug = 0;
            }
            Calendar cal2 = makeCalendar(calType, TimeZone.GMT_ZONE, ULocale.ENGLISH);

            // HACK to get the right maximum number of months per year
            // Two calendar systems don't make visible the Nth month of the year,
            // and rather double up the number with a special affix.
            // Plus, they do this inconsistently

            int hack = cal2.getMaximum(Calendar.MONTH) + 1;
            final boolean isChineseCalendarBased = cal2 instanceof ChineseCalendar;
            final boolean isHebrewCalendarBased = cal2 instanceof HebrewCalendar;
            if (isChineseCalendarBased) {
                // Chinese Calendar does not allow access to the max number of months per year,
                // just the highest month number.
                hack += 1;
                // Hebrew does allow access to the max number of months per year,
                // but not the current number of months per year.
                // That requires a further hack
            }

            maxMonthsPerYear = hack;
            maxDaysPerMonth = cal2.getMaximum(Calendar.DAY_OF_MONTH);
            maxDaysPerYear = cal2.getMaximum(Calendar.DAY_OF_YEAR);
            final Set<Integer> _monthsInYear = new TreeSet<>();
            final Set<Integer> _daysInYear = new TreeSet<>();
            final Multimap<Integer, Integer> _daysInMonths = TreeMultimap.create();
            cal2.setTime(d); // year may not be gregorian

            final int startYear = cal2.get(Calendar.YEAR);
            int currYear = startYear;

            for (int year = startYear; year < startYear + 1000; ++year) {
                cal2.set(Calendar.DAY_OF_MONTH, 1);
                cal2.set(Calendar.MONTH, 0);

                final int currDaysPerYear = cal2.getActualMaximum(Calendar.DAY_OF_YEAR);
                int currMonthsInYear = cal2.getActualMaximum(Calendar.MONTH) + 1;

                // Compensate for the Chinese & Hebrew Calendars not returning the actual number of
                // months in a year
                if (isChineseCalendarBased && currDaysPerYear >= 365) {
                    currMonthsInYear += 1;
                } else if (isHebrewCalendarBased && currDaysPerYear < 365) {
                    currMonthsInYear -= 1;
                }

                _monthsInYear.add(currMonthsInYear);
                _daysInYear.add(currDaysPerYear);
                int daysLeft = currDaysPerYear;

                // use clunky method because some months in Chinese / Arabic share a numeric value
                // the 'month' variable is the nth month in the year, NOT the month with that number
                for (int month = 0; ; ++month) {
                    int daysInThisMonth = cal2.getActualMaximum(Calendar.DAY_OF_MONTH);
                    _daysInMonths.put(month + 1, daysInThisMonth);
                    daysLeft -= daysInThisMonth;

                    int oldMonth = cal2.get(Calendar.MONTH);
                    cal2.add(Calendar.MONTH, 1);
                    if (cal2.get(Calendar.MONTH) < oldMonth) { // we wrapped around
                        if (daysLeft != 0) {
                            // special hack for coptic, etc.
                            System.out.println(calType + " " + (month + 1) + " " + daysLeft);
                        }
                        break;
                    }
                }
            }
            // make immutable
            // filter out cases where the month has only the max
            Set<Integer> maxSingleton = ImmutableSet.of(maxDaysPerMonth);
            _daysInMonths.asMap().values().removeIf(value -> value.equals(maxSingleton));

            daysInYear = ImmutableSet.copyOf(_daysInYear);
            monthsInYear = ImmutableSet.copyOf(_monthsInYear);
            daysInMonths = CldrUtility.protectCollection(_daysInMonths);
            hash =
                    Objects.hash(
                            maxMonthsPerYear,
                            maxDaysPerMonth,
                            maxDaysPerYear,
                            monthsInYear,
                            daysInYear,
                            daysInMonths);
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((Footprint) obj) == 0;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public int compareTo(Footprint o) {
            return ComparisonChain.start()
                    // single fields first
                    .compare(maxDaysPerYear, o.maxDaysPerYear)
                    .compare(maxMonthsPerYear, o.maxMonthsPerYear)
                    .compare(maxDaysPerMonth, o.maxDaysPerMonth)
                    // then structures
                    .compare(daysInYear, o.daysInYear, LEX_NATURAL_INTEGER)
                    .compare(monthsInYear, o.monthsInYear, LEX_NATURAL_INTEGER)
                    .compare(daysInMonths.entries(), o.daysInMonths.entries(), LIST_ENTRY_COMP)
                    .result();
        }

        @Override
        public String toString() {
            return TAB_JOINER.join(
                    maxDaysPerYear,
                    maxMonthsPerYear,
                    maxDaysPerMonth,
                    SPACE_JOINER.join(daysInYear),
                    SPACE_JOINER.join(monthsInYear),
                    MM_JOINER.join(daysInMonths));
        }

        private static final Comparator<Map.Entry<Integer, Integer>> ENTRY_COMP =
                new Comparator<>() {
                    @Override
                    public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                        return ComparisonChain.start()
                                .compare(o1.getKey(), o2.getKey())
                                .compare(o1.getValue(), o2.getValue())
                                .result();
                    }
                };
        private static final Comparator<Iterable<Map.Entry<Integer, Integer>>> LIST_ENTRY_COMP =
                Comparators.lexicographical(ENTRY_COMP);
        private static final Comparator<Iterable<Integer>> LEX_NATURAL_INTEGER =
                Comparators.lexicographical(Comparator.<Integer>naturalOrder());
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
