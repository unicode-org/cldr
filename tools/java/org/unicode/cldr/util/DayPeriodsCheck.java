package org.unicode.cldr.util;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.DayPeriods.DayPeriod;

import com.ibm.icu.util.ULocale;

public class DayPeriodsCheck {

    private static final int HOUR = 60 * 60 * 1000;
    static boolean html = false;

    static final Comparator<Enum<?>> ENUM_COMPARATOR = new Comparator<Enum<?>>() {
        @Override
        public int compare(Enum<?> o1, Enum<?> o2) {
            return o1.ordinal() - o2.ordinal();
        }
    };

    // QUICK CHECK
    public static void main(String[] args) {
        Set<ULocale> localesToCheck = new LinkedHashSet<>();
        if (args.length == 0) {
            args = new String[] { "groups" };
        }
        html = false;
        for (String arg : args) {
            localesToCheck.clear();
            switch (arg) {
            case "groups":
                StandardCodes sc = CLDRConfig.getInstance().getStandardCodes();
                CLDRFile english = CLDRConfig.getInstance().getEnglish();
                english.getName(LanguageGroup.uralic.iso);

                for (LanguageGroup group : LanguageGroup.values()) {
                    System.out.println(group + "\t" + group.iso + "\t" + english.getName(group.iso));
                }

                for (LanguageGroup group : LanguageGroup.values()) {
                    for (ULocale loc : LanguageGroup.getLocales(group)) {
                        Level level = sc.getLocaleCoverageLevel("cldr", loc.toString());
                        System.out.println(group + "\t" + loc + "\t" + loc.getDisplayLanguage(ULocale.ENGLISH) + "\t" + level);
                    }
                }
                Set<ULocale> missing = new TreeSet<>();
                for (String s : CLDRConfig.getInstance().getCldrFactory().getAvailableLanguages()) {
                    ULocale loc = new ULocale(s);
                    loc = loc.getScript().isEmpty() ? loc : new ULocale(loc.getLanguage());
                    if (LanguageGroup.getExplicit().contains(loc)) {
                        continue;
                    }
                    missing.add(loc);
                }
                for (ULocale loc : missing) {
                    Level level = sc.getLocaleCoverageLevel("cldr", loc.toString());
                    if (level == Level.UNDETERMINED) continue;
                    System.out.println("?\t" + loc + "\t" + loc.getDisplayLanguage(ULocale.ENGLISH) + "\t" + level);
                }
                for (ULocale loc : missing) {
                    Level level = sc.getLocaleCoverageLevel("cldr", loc.toString());
                    if (level != Level.UNDETERMINED || "root".equals(loc.toString())) continue;
                    System.out.println("?\t" + loc + "\t" + loc.getDisplayLanguage(ULocale.ENGLISH) + "\t" + level);
                }

                break;
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
                    for (int i = 0; i < 24 * HOUR; i += HOUR) {
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
                    for (int i = 0; i < 24 * HOUR; i += HOUR) {
                        DayPeriod dayPeriod = dayPeriods.get(i);
                        DayPeriodsOld.DayPeriod dayPeriodOld = dayPeriodsOld.get(i);
                        String sampleOld = dayPeriodsOld.getSample(dayPeriodOld);
                        String sample = dayPeriods.getSample(dayPeriod);
                        boolean needLn = false;
                        if (!dayPeriodOld.toString().equals(dayPeriod.toString())) {
                            System.out.print(locale + "\t" + i / HOUR + "..\t" + dayPeriodOld + " → " + dayPeriod);
                            needLn = true;
                            if (!sampleOld.equals(sample)) {
                                System.out.print("\t\t" + sampleOld + "\t→\t" + sample);
                            } else {
                                System.out.print("\t\t" + sample);
                            }
                        } else if (!sampleOld.equals(sample)) {
                            System.out.print(locale + "\t" + i / HOUR + "..\t" + dayPeriod + "\t\t" + sampleOld + "\t→\t" + sample);
                            needLn = true;
                        }
                        if (needLn) {
                            System.out.println();
                        }
                    }
                }
                break;
            case "html":
                html = true;
                System.out
                    .println(
                        "<table border=\"1\" bordercolor=\"#888\" cellspacing=\"0\" style=\"border-collapse: collapse; border-color: rgb(136, 136, 136); border-width: 1px;\">\n"
                            + "<tbody>");
            case "list":
                localesToCheck.addAll(DayPeriods.getAvailable());
                for (ULocale locale : localesToCheck) {
                    DayPeriods dayPeriods = DayPeriods.getInstance(locale);
                    if (html) {
                        System.out.println("<tr><th>" + LanguageGroup.get(locale) +
                            "</th><th colSpan='2'><h4>" + locale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")</h4></tr></th></tr>");
                    } else {
                        System.out.println(LanguageGroup.get(locale) + "\t" + locale.getDisplayName(ULocale.ENGLISH) + "\t(" + locale + ")");
                    }
                    int start = 0;
                    DayPeriod lastDayPeriod = dayPeriods.get(start * HOUR);
                    //Set<DayPeriod> periods = dayPeriods.getDayPeriods();
                    for (int i = 1; i < 24; ++i) {
                        DayPeriod dayPeriod = dayPeriods.get(i * HOUR);
                        if (dayPeriod != lastDayPeriod) {
                            show(locale, start, i - 1, dayPeriods, lastDayPeriod);
                            lastDayPeriod = dayPeriod;
                            start = i;
                        }
                    }
                    show(locale, start, 23, dayPeriods, lastDayPeriod);
                }
                if (html) {
                    System.out.println("</tbody>\n</table>");
                }
                break;
            }
        }
    }

    private static void show(ULocale locale, int start, int limit, DayPeriods dayPeriods, DayPeriod dayPeriod) {
        if (html) {
            System.out.println("<tr><td>" + format(start) + ":00 – " + format(limit) + ":59</td><td>" + dayPeriod + "</td><td>"
                + dayPeriods.getSample(dayPeriod) + "</td></tr>");
        } else {
            System.out.println("||" + LanguageGroup.get(locale) + "||" + locale.getDisplayName(ULocale.ENGLISH) + "||" + locale + "||" + dayPeriod + "||"
                + start + ":00 – " + limit + ":59||" + dayPeriods.getSample(dayPeriod) + "||");
        }
    }

    private static String format(int start) {
        return start < 10 ? "0" + start : "" + start;
    }
}
