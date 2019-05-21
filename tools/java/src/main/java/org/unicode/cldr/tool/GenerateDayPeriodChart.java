package org.unicode.cldr.tool;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateDayPeriodChart {
    static final SupplementalDataInfo SUP = CLDRConfig.getInstance().getSupplementalDataInfo();
    static final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();
    static final StandardCodes SC = CLDRConfig.getInstance().getStandardCodes();
    static final int MINUTE = 60 * 1000;
    static final int HOUR = 60 * MINUTE;

//    static final DayPeriodInfo ENGLISH_DAY_INFO;
//    static final DayPeriodInfo ROOT_DAY_INFO;
//    static {
//        DayPeriodInfo.Builder dayPeriodBuilder = new DayPeriodInfo.Builder();
//        dayPeriodBuilder.add(DayPeriod.midnight, 0, true, 0, true);
//        dayPeriodBuilder.add(DayPeriod.night1, 0, false, HOUR*2, false);
//        dayPeriodBuilder.add(DayPeriod.morning1, HOUR*2, true, HOUR*12, false);
//        dayPeriodBuilder.add(DayPeriod.noon, HOUR*12, true, HOUR*12, true);
//        dayPeriodBuilder.add(DayPeriod.afternoon1, HOUR*12, false, HOUR*18, false);
//        dayPeriodBuilder.add(DayPeriod.evening1, HOUR*18, true, HOUR*21, false);
//        dayPeriodBuilder.add(DayPeriod.night1, HOUR*21, true, HOUR*24, false);
//        ENGLISH_DAY_INFO = dayPeriodBuilder.finish(new String[] {"en"});
//        ROOT_DAY_INFO = SUP.getDayPeriods("root");
//    }

    public static void main(String[] args) {

        DateFormat dt = DateFormat.getPatternInstance("HH:mm", ULocale.ENGLISH);
        dt.setTimeZone(TimeZone.GMT_ZONE);

        for (DayPeriodInfo.Type type : DayPeriodInfo.Type.values()) {

            final M4<DayPeriod, DayPeriod, String, Boolean> minimalPairs = ChainedMap.of(
                new TreeMap<DayPeriod, Object>(),
                new TreeMap<DayPeriod, Object>(),
                new TreeMap<String, Object>(),
                Boolean.class);

            EnumSet<DayPeriod> careAbout = EnumSet.noneOf(DayPeriod.class);

            M3<DayPeriod, Integer, Integer> dayPeriodToTimes = ChainedMap.of(new TreeMap<DayPeriod, Object>(), new TreeMap<Integer, Object>(), Integer.class);

            System.out.println(type);
            Set<String> dayPeriodLocales = SUP.getDayPeriodLocales(type);
            for (String locale : dayPeriodLocales) {
                DayPeriodInfo info = getFixedDayPeriodInfo(type, locale);
                if (info == null) {
                    continue;
                }
                final List<DayPeriod> periods = info.getPeriods();
                careAbout.addAll(periods);
                for (DayPeriod dp1 : periods) {
                    for (DayPeriod dp2 : periods) {
                        int comp = dp1.compareTo(dp2);
                        if (comp < 0) {
                            minimalPairs.put(dp2, dp1, locale, Boolean.TRUE);
                        } else {
                            minimalPairs.put(dp1, dp2, locale, Boolean.TRUE);
                        }
                    }
                }
                for (int i = 0; i < 24; ++i) {
                    DayPeriod period = info.getDayPeriod((i * 60 + 30) * MINUTE);
                    Integer old = dayPeriodToTimes.get(period, i);
                    dayPeriodToTimes.put(period, i, old == null ? 1 : old + 1);
                }
            }
//            careAbout.remove(DayPeriod.am);
//            careAbout.remove(DayPeriod.pm);

            System.out.print("\t\t");
            for (int i = 0; i < 24; ++i) {
                if (i == 12 || i == 0) {
                    System.out.print("\t" + dt.format((i * 60) * MINUTE));
                }
                System.out.print("\t" + dt.format((i * 60 + 30) * MINUTE));
            }
            System.out.println();
            System.out.flush();

            for (String locale : dayPeriodLocales) { // SC.getLocaleCoverageLocales("cldr", EnumSet.of(Level.MODERN))) {
                System.out.print(type + "\t" + locale + "\t" + ENGLISH.getName(locale));
                DayPeriodInfo dayPeriod = getFixedDayPeriodInfo(type, locale);
                doRow(dayPeriod);
            }

            for (DayPeriod column : careAbout) {
                System.out.print("\t" + column);
            }
            System.out.println();

            for (DayPeriod row : careAbout) {
                System.out.print(row);
                M3<DayPeriod, String, Boolean> rowValues = minimalPairs.get(row);
                for (DayPeriod column : careAbout) {
                    Map<String, Boolean> cell = rowValues == null ? null : rowValues.get(column);
                    System.out.print("\t" + (cell == null ? "" : cell.keySet()));
                }
                System.out.println();
            }

            for (int i = 0; i < 24; ++i) {
                System.out.print("\t" + dt.format((i * 60 + 30) * MINUTE));
            }
            System.out.println();
            for (DayPeriod dayPeriod : DayPeriod.values()) {
                System.out.print(dayPeriod);
                final Map<Integer, Integer> times = dayPeriodToTimes.get(dayPeriod);
                for (int i = 0; i < 24; ++i) {
                    Integer count = times == null ? null : times.get(i);
                    System.out.print("\t" + (count == null ? "-" : count));
                }
                System.out.println();
            }

            EnumSet<DayPeriod> present = EnumSet.allOf(DayPeriod.class);
            for (String locale : dayPeriodLocales) { // SC.getLocaleCoverageLocales("cldr", EnumSet.of(Level.MODERN))) {
                DayPeriodInfo dayPeriod = getFixedDayPeriodInfo(type, locale);
                present.retainAll(dayPeriod.getPeriods());
            }
            System.out.println("Present in all: " + present);
        }
    }

    public static DayPeriodInfo getFixedDayPeriodInfo(Type type, String locale) {
        return SUP.getDayPeriods(type, locale);
//        DayPeriodInfo result = locale.equals("en") || locale.startsWith("en_")
//            ? ENGLISH_DAY_INFO
//                : SUP.getDayPeriods(type, locale);
//        return result == ROOT_DAY_INFO ? null : result;
    }

    public static void doRow(DayPeriodInfo dayPeriod) {
        for (int i = 0; i < 24; ++i) {
            if (dayPeriod == null) {
                System.out.print("\t?");
                continue;
            }
            if (i == 12 || i == 0) {
                DayPeriod period = dayPeriod.getDayPeriod((i * 60) * MINUTE);
                System.out.print("\t" + period);
            }
            DayPeriod period = dayPeriod.getDayPeriod((i * 60 + 30) * MINUTE);
            System.out.print("\t" + period);
        }
        System.out.println();
        System.out.flush();
    }
}
