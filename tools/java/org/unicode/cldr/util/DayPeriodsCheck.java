package org.unicode.cldr.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.DayPeriods.DayPeriod;

import com.ibm.icu.util.ULocale;

public class DayPeriodsCheck {

    private static final int HOUR = 60*60*1000;

    // QUICK CHECK
    public static void main(String[] args) {
        Set<ULocale> localesToCheck = new LinkedHashSet<>();
        if (args.length == 0) {
            args = new String[] {"diff"};
        }
        for (String arg : args) {
            localesToCheck.clear();
            switch(arg) {
            case "chart":
                localesToCheck.addAll(DayPeriods.getAvailable());
                // add a couple of strange locales for checking
                localesToCheck.add(new ULocale("en-Arab"));
                localesToCheck.add(new ULocale("und"));
                for (ULocale locale : localesToCheck) {
                    DayPeriods dayPeriods = DayPeriods.getInstance(locale);
                    if (dayPeriods == null) {
                        System.out.println("No data for locale; not supported");
                        continue;
                    }
                    System.out.print(locale.getDisplayName(ULocale.ENGLISH));
                    for (int i = 0; i < 24*HOUR; i += HOUR) {
                        DayPeriod dayPeriod = dayPeriods.get(i);
                        System.out.print("\t" + dayPeriod); //  + "\t" + dayPeriods.getSample(dayPeriod));
                    }
                    System.out.println();
                }
                break;
            case "diff":
                for (ULocale locale : DayPeriods.getAvailable()) {
                    DayPeriods dayPeriods = DayPeriods.getInstance(locale);
                    DayPeriodsOld dayPeriodsOld = DayPeriodsOld.getInstance(locale);
                    System.out.println(locale.getDisplayName(ULocale.ENGLISH));
                    for (int i = 0; i < 24*HOUR; i += HOUR) {
                        DayPeriod dayPeriod = dayPeriods.get(i);
                        DayPeriodsOld.DayPeriod dayPeriodOld = dayPeriodsOld.get(i);
                        String sampleOld = dayPeriodsOld.getSample(dayPeriodOld);
                        String sample = dayPeriods.getSample(dayPeriod);
                        boolean needLn = false;
                        if (!dayPeriodOld.toString().equals(dayPeriod.toString())) {
                            System.out.print("\t" + i/HOUR + "..\t" + dayPeriodOld + " → " + dayPeriod);
                            needLn = true;
                            if (!sampleOld.equals(sample)) {
                                System.out.print("\t\t" + sampleOld + " → " +  sample);
                            } else {
                                System.out.print("\t\t" +  sample);
                            }
                        } else if (!sampleOld.equals(sample)) {
                            System.out.print("\t" + i/HOUR + "..\t" + dayPeriod + "\t\t" + sampleOld + " → " +  sample);
                            needLn = true;
                        }
                        if (needLn) {
                            System.out.println();                            
                        }
                    }
                }
                break;
            case "list":
                localesToCheck.addAll(DayPeriods.getAvailable());
                for (ULocale locale : localesToCheck) {
                    DayPeriods dayPeriods = DayPeriods.getInstance(locale);
                    System.out.println(locale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")");
                    DayPeriod lastDayPeriod = null;
                    Set<DayPeriod> periods = dayPeriods.getDayPeriods();
                    for (int i = 0; i < 24; ++i) {
                        DayPeriod dayPeriod = dayPeriods.get(i*HOUR);
                        if (dayPeriod != lastDayPeriod) {
                            show(i, dayPeriods, dayPeriod);
                            lastDayPeriod = dayPeriod;
                        }
                    }
                    DayPeriod end = dayPeriods.get(0);
                    if (end != lastDayPeriod) {
                        show(24, dayPeriods, end);
                    }
                    System.out.println();
                }
                break;
            }
        }
    }

    private static void show(int i, DayPeriods dayPeriods, DayPeriod dayPeriod) {
        System.out.println("||" + i + "..||" + dayPeriod + "||" + dayPeriods.getSample(dayPeriod) + "||");
    }
}
