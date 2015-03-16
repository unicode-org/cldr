package org.unicode.cldr.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.util.DayPeriods.DayPeriod;

import com.ibm.icu.util.ULocale;

public class DayPeriodConverter {
    private static final boolean TO_CODE = true;

    // HACK TO SET UP DATA
    // Will be replaced by real data table in the future

    static class DayInfo {
        ULocale locale;
        DayPeriods.DayPeriod[] data = new DayPeriod[24];
        Map<DayPeriod, String> toNativeName = new EnumMap<DayPeriod, String>(DayPeriod.class);
        Map<String, DayPeriod> toDayPeriod = new HashMap<String, DayPeriod>();

        @Override
        public String toString() {
            String result = "make(\"" + locale + "\"";
            DayPeriod lastDayPeriod = null;
            for (int i = 0; i < 24; ++i) {
                DayPeriod dayPeriod = data[i];
                if (dayPeriod != lastDayPeriod) {
                    result += ")\n.add(\""
                        + dayPeriod
                        + "\", \""
                        + toNativeName.get(dayPeriod)
                        + "\"";
                    lastDayPeriod = dayPeriod;
                }
                result += ", " + i;
            }
            result += ")\n.build();\n";
            /*
            make("en")
            .add("MORNING", "morning", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "afternoon", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "evening", 18, 19, 20)
            .add("NIGHT", "night", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();
             */
            return result;
        }

        public String toCldr() {
            String result = "\t\t<dayPeriodRules locales=\"" + locale + "\">\n";
            DayPeriod lastDayPeriod = data[0];
            int start = 0;
            for (int i = 1; i < 24; ++i) {
                DayPeriod dayPeriod = data[i];
                if (dayPeriod != lastDayPeriod) {
                    result = addPeriod(result, lastDayPeriod, start, i);
                    lastDayPeriod = dayPeriod;
                    start = i;
                }
            }
            result = addPeriod(result, lastDayPeriod, start, 24);
            result += "\t\t</dayPeriodRules>";
            return result;
        }

        private String addPeriod(String result, DayPeriod dayPeriod, int start, int i) {
            result += "\t\t\t<dayPeriodRule type=\""
                + dayPeriod.toString().toLowerCase(Locale.ENGLISH)
                + "\" from=\""
                + start + ":00"
                + "\" before=\""
                + i + ":00"
                + "\"/> <!-- " + toNativeName.get(dayPeriod)
                + " -->\n";
            return result;
        }
    }

    static final Map<ULocale, DayInfo> DATA = new LinkedHashMap<>();
    static {
        for (String[] x : DayPeriodData.RAW_DATA) {
            ULocale locale = new ULocale(x[0]);
            int start = Integer.parseInt(x[1]);
            DayPeriod dayPeriod = DayPeriod.valueOf(x[2]);
            String nativeName = x[3].trim();
            DayInfo data = DATA.get(locale);
            if (data == null) {
                DATA.put(locale, data = new DayInfo());
            }
            data.locale = locale;
            for (int i = start; i < 24; ++i) {
                data.data[i] = dayPeriod;
            }
            String old = data.toNativeName.get(dayPeriod);
            if (old != null && !old.equals(nativeName)) {
                throw new IllegalArgumentException(locale + " inconsistent native name for "
                    + dayPeriod + ", old: «" + old + "», new: «" + nativeName + "»");
            }
            DayPeriod oldDp = data.toDayPeriod.get(nativeName);
            if (oldDp != null && oldDp != dayPeriod) {
                throw new IllegalArgumentException(locale + " inconsistent day periods for name «"
                    + nativeName + "», old: " + oldDp + ", new: " + dayPeriod);
            }
            data.toDayPeriod.put(nativeName, dayPeriod);
            data.toNativeName.put(dayPeriod, nativeName);
        }
    }

    public static void main(String[] args) {
        System.out.println("\t<dayPeriodRuleSet type=\"selection\">");
        for (Entry<ULocale, DayInfo> foo : DATA.entrySet()) {
            check(foo.getKey(), foo.getValue());
            System.out.println(foo.getValue().toCldr());
        }
        System.out.println("\t</dayPeriodRuleSet>");
    }

    private static void check(ULocale locale, DayInfo value) {
        check(locale, DayPeriod.MORNING1, DayPeriod.MORNING2, value);
        check(locale, DayPeriod.AFTERNOON1, DayPeriod.AFTERNOON2, value);
        check(locale, DayPeriod.EVENING1, DayPeriod.EVENING2, value);
        check(locale, DayPeriod.NIGHT1, DayPeriod.NIGHT2, value);
        DayPeriod lastDp = value.data[23];
        for (DayPeriod dp : value.data) {
            if (lastDp.compareTo(dp) > 0) {
                if ((lastDp == DayPeriod.NIGHT1 || lastDp == DayPeriod.NIGHT2) && dp == DayPeriod.MORNING1) {
                } else {
                    throw new IllegalArgumentException(locale + " " + lastDp + " > " + dp);
                }
            }
            lastDp = dp;
        }
    }

    private static void check(ULocale locale, DayPeriod morning1, DayPeriod morning2, DayInfo value) {
        if (value.toNativeName.containsKey(morning2) && !value.toNativeName.containsKey(morning1)) {
            throw new IllegalArgumentException(locale + " Contains " + morning2 + ", but not " + morning1);
        }
    }
}
