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
    }


}
