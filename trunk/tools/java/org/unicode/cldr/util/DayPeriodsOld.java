package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.ULocale;

/**
 * This is a first-cut version just to get going. The data is hard coded until we switch over to real data in ICU.
 */
public class DayPeriodsOld {
    private static final int HOUR = 60 * 60 * 1000;

    public enum DayPeriod {
        MORNING1("EARLY_MORNING"), MORNING2("MORNING"), AFTERNOON1("EARLY_AFTERNOON"), AFTERNOON2("AFTERNOON"), EVENING1("EARLY_EVENING"), EVENING2(
            "EVENING"), NIGHT1("NIGHT"), NIGHT2("LATE_NIGHT");
        public final String name;

        DayPeriod(String name) {
            this.name = name;
        }

        public static DayPeriod get(String dayPeriod) {
            for (DayPeriod d : DayPeriod.values()) {
                if (dayPeriod.equals(d.name)) {
                    return d;
                }
            }
            return DayPeriod.valueOf(dayPeriod);
        }
    }

    /**
     * Get the category for a given time in the day.
     * @param millisInDay
     * @return
     */
    public DayPeriod get(long millisInDay) {
        long hours = millisInDay / HOUR;
        int hoursInDay = (int) (hours % 24);
        if (hoursInDay < 0) {
            hoursInDay += 24;
        }
        return timeMap[hoursInDay];
    }

    /**
     * Get the *actual* locale for the DayPeriods (eg, asking for "en-AU" may get you "en")
     * @param millisInDay
     * @return
     */
    public ULocale getLocale() {
        return locale;
    }

    /**
     * Get a sample, for showing to a localizer. The actual phrase should come out of a SELECT statement, since it may vary by message.
     * @param millisInDay
     * @return
     */
    public String getSample(DayPeriod dayPeriod) {
        return samples.get(dayPeriod);
    }

    /**
     * Return the possible DayPeriod values for this locale.
     * @return
     */
    public Set<DayPeriod> getDayPeriods() {
        return samples.keySet();
    }

    /**
     * Get an instance with a factory method. Right now, returns null if the locale data is not available.
     * @param loc
     * @return
     */
    public static DayPeriodsOld getInstance(ULocale loc) {
        ULocale base = new ULocale(loc.getLanguage());
        DayPeriodsOld result = DATA.get(base);
//        if (result == null) {
//            throw new IllegalArgumentException("No data for locale " + loc);
//        }
        return result;
    }

    /**
     * Returns the available locales. Note that regional/script variants may be mapped by getInstance to a base locale,
     * eg, en-AU => en.
     * @return
     */
    public static Set<ULocale> getAvailable() {
        return DATA.keySet();
    }

    // ===== PRIVATES =====

    private final ULocale locale;
    private final DayPeriod[] timeMap;
    private final Map<DayPeriod, String> samples;

    private DayPeriodsOld(ULocale base, DayPeriod[] map, EnumMap<DayPeriod, String> samples2) {
        locale = base;
        fix(map, samples2, DayPeriod.MORNING2, DayPeriod.MORNING1);
        fix(map, samples2, DayPeriod.AFTERNOON2, DayPeriod.AFTERNOON1);
        fix(map, samples2, DayPeriod.EVENING2, DayPeriod.EVENING1);
        fix(map, samples2, DayPeriod.NIGHT2, DayPeriod.NIGHT1);
        timeMap = map;
        samples = Collections.unmodifiableMap(samples2);
    }

    private void fix(DayPeriod[] map, EnumMap<DayPeriod, String> samples2, DayPeriod dayPeriod2, DayPeriod dayPeriod1) {
        if (samples2.containsKey(dayPeriod2) && !samples2.containsKey(dayPeriod1)) {
            samples2.put(dayPeriod1, samples2.get(dayPeriod2));
            samples2.remove(dayPeriod2);
            for (int i = 0; i < map.length; ++i) {
                if (map[i] == dayPeriod2) {
                    map[i] = dayPeriod1;
                }
            }
        }
    }

    // HACK TO SET UP DATA
    // Will be replaced by real data table in the future

    private static final Map<ULocale, DayPeriodsOld> DATA = new LinkedHashMap<>();

    private static DayPeriodBuilder make(String locale) {
        return new DayPeriodBuilder(locale);
    }

    private static class DayPeriodBuilder {
        private final ULocale locale;
        private final DayPeriod[] timeMap = new DayPeriod[24];
        private final EnumMap<DayPeriod, String> samples = new EnumMap<>(DayPeriod.class);

        DayPeriodBuilder(String locale) {
            this.locale = new ULocale(locale);
        }

        public DayPeriodBuilder add(String dayPeriod, String localeName, int... hours) {
            DayPeriod dayPeriodEnum = DayPeriod.get(dayPeriod);
            String previous = samples.put(dayPeriodEnum, localeName);
            if (previous != null) {
                throw new IllegalArgumentException(locale + " Collision");
            }
            for (int i : hours) {
                if (timeMap[i] != null) {
                    throw new IllegalArgumentException(locale + " Collision " + i + ", " + timeMap[i] + ", " + dayPeriodEnum);
                }
                timeMap[i] = dayPeriodEnum;
            }
            return this;
        }

        public DayPeriodsOld build() {
            for (int i = 0; i < timeMap.length; ++i) {
                DayPeriod dp = timeMap[i];
                if (dp == null) {
                    throw new IllegalArgumentException(locale + " Missing item: " + i);
                }
            }
            DayPeriodsOld item = new DayPeriodsOld(locale, timeMap, samples);
            DATA.put(locale, item);
            return item;
        }
    }

    static {
        make("en")
            .add("NIGHT1", "night", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .add("MORNING1", "morning", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON1", "afternoon", 12, 13, 14, 15, 16, 17)
            .add("EVENING1", "evening", 18, 19, 20)
            .build();

        make("af")
            .add("MORNING", "oggend", 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "middag", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "aand", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "nag", 0, 1, 2, 3, 4)
            .build();

        make("nl")
            .add("MORNING", "ochtend", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "middag", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "avond", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "nacht", 0, 1, 2, 3, 4, 5)
            .build();

        make("de")
            .add("EARLY_MORNING", "Morgen", 5, 6, 7, 8, 9)
            .add("MORNING", "Vormittag", 10, 11)
            .add("AFTERNOON", "Mittag", 12)
            .add("EVENING", "Nachmittag", 13, 14, 15, 16, 17)
            .add("NIGHT", "Abend", 18, 19, 20, 21, 22, 23)
            .add("LATE_NIGHT", "Nacht", 0, 1, 2, 3, 4)
            .build();

        make("da")
            .add("EARLY_MORNING", "morgen", 5, 6, 7, 8, 9)
            .add("MORNING", "formiddag", 10, 11)
            .add("AFTERNOON", "eftermiddag", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "aften", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "nat", 0, 1, 2, 3, 4)
            .build();

        make("nb")
            .add("EARLY_MORNING", "morgen", 6, 7, 8, 9)
            .add("MORNING", "formiddag", 10, 11)
            .add("AFTERNOON", "ettermiddag", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "kveld", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "natt", 0, 1, 2, 3, 4, 5)
            .build();

        make("sv")
            .add("EARLY_MORNING", "morgon", 5, 6, 7, 8, 9)
            .add("MORNING", "förmiddag", 10, 11)
            .add("AFTERNOON", "eftermiddag", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "kväll", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "natt", 0, 1, 2, 3, 4)
            .build();

        make("is")
            .add("MORNING", "morgunn", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "eftir hádegi", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "kvöld", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "nótt", 0, 1, 2, 3, 4, 5)
            .build();

        make("pt")
            .add("MORNING", "manhã", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "tarde", 12, 13, 14, 15, 16, 17, 18)
            .add("EVENING", "noite", 19, 20, 21, 22, 23)
            .add("NIGHT", "madrugada", 0, 1, 2, 3, 4, 5)
            .build();

        make("gl")
            .add("EARLY_MORNING", "madrugada", 0, 1, 2, 3, 4, 5)
            .add("MORNING", "mañá", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "mediodía", 12)
            .add("EVENING", "tarde", 13, 14, 15, 16, 17, 18, 19, 20)
            .add("NIGHT", "noite", 21, 22, 23)
            .build();

        make("es")
            .add("MORNING", "mañana", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "tarde", 12, 13, 14, 15, 16, 17, 18, 19)
            .add("EVENING", "noche", 20, 21, 22, 23)
            .add("NIGHT", "madrugada", 0, 1, 2, 3, 4, 5)
            .build();

        make("ca")
            .add("EARLY_MORNING", "matinada", 0, 1, 2, 3, 4, 5)
            .add("MORNING", "matí", 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "migdia", 12)
            .add("AFTERNOON", "tarda", 13, 14, 15, 16, 17, 18)
            .add("EVENING", "vespre", 19, 20)
            .add("NIGHT", "nit", 21, 22, 23)
            .build();

        make("it")
            .add("MORNING", "mattina", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "pomeriggio", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "sera", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "notte", 0, 1, 2, 3, 4, 5)
            .build();

        make("ro")
            .add("MORNING", "dimineață", 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "după-amiază", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "seară", 18, 19, 20, 21)
            .add("NIGHT", "noapte", 0, 1, 2, 3, 4, 22, 23)
            .build();

        make("fr")
            .add("MORNING", "matin", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "après-midi", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "soir", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "nuit", 0, 1, 2, 3)
            .build();

        make("hr")
            .add("MORNING", "jutro", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "popodne", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "večer", 18, 19, 20)
            .add("NIGHT", "noć", 0, 1, 2, 3, 21, 22, 23)
            .build();

        make("bs")
            .add("MORNING", "jutro", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "popodne", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "veče", 18, 19, 20)
            .add("NIGHT", "noć", 0, 1, 2, 3, 21, 22, 23)
            .build();

        make("sr")
            .add("MORNING", "јутро", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "поподне", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "вече", 18, 19, 20)
            .add("NIGHT", "ноћ", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("sl")
            .add("EARLY_MORNING", "jutro", 6, 7, 8, 9)
            .add("MORNING", "dopoldne", 10, 11)
            .add("AFTERNOON", "popoldne", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "večer", 18, 19, 20, 21)
            .add("NIGHT", "noč", 0, 1, 2, 3, 4, 5, 22, 23)
            .build();

        make("cs")
            .add("EARLY_MORNING", "ráno", 4, 5, 6, 7, 8)
            .add("MORNING", "dopoledne", 9, 10, 11)
            .add("AFTERNOON", "odpoledne", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "večer", 18, 19, 20, 21)
            .add("NIGHT", "noc", 0, 1, 2, 3, 22, 23)
            .build();

        make("sk")
            .add("EARLY_MORNING", "ráno", 4, 5, 6, 7, 8)
            .add("MORNING", "dopoludnie", 9, 10, 11)
            .add("AFTERNOON", "popoludnie", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "večer", 18, 19, 20, 21)
            .add("NIGHT", "noc", 0, 1, 2, 3, 22, 23)
            .build();

        make("pl")
            .add("EARLY_MORNING", "rano", 6, 7, 8, 9)
            .add("MORNING", "przedpołudnie", 10, 11)
            .add("AFTERNOON", "popołudnie", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "wieczór", 18, 19, 20)
            .add("NIGHT", "noc", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("bg")
            .add("MORNING", "сутринта", 4, 5, 6, 7, 8, 9, 10)
            .add("EARLY_AFTERNOON", "на обяд", 11, 12, 13)
            .add("AFTERNOON", "следобяд", 14, 15, 16, 17)
            .add("EVENING", "вечерта", 18, 19, 20, 21)
            .add("NIGHT", "нощ", 0, 1, 2, 3, 22, 23)
            .build();

        make("mk")
            .add("EARLY_MORNING", "наутро", 4, 5, 6, 7, 8, 9)
            .add("MORNING", "претпладне", 10, 11)
            .add("AFTERNOON", "попладне", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "навечер", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "по полноќ", 0, 1, 2, 3)
            .build();

        make("ru")
            .add("MORNING", "утро", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "день", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "вечер", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "ночь", 0, 1, 2, 3)
            .build();

        make("uk")
            .add("MORNING", "ранок", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "день", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "вечір", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "ніч", 0, 1, 2, 3)
            .build();

        make("lt")
            .add("MORNING", "rytas", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "diena", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "vakaras", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "naktis", 0, 1, 2, 3, 4, 5)
            .build();

        make("lv")
            .add("MORNING", "rīts", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "pēcpusdiena", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "vakars", 18, 19, 20, 21, 22)
            .add("NIGHT", "nakts", 0, 1, 2, 3, 4, 5, 23)
            .build();

        make("el")
            .add("EARLY_MORNING", "μεσάνυχτα", 0)
            .add("MORNING", "πρωί", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "μεσημέρι", 12, 13, 14, 15, 16)
            .add("EVENING", "απόγευμα", 17, 18, 19)
            .add("NIGHT", "βράδυ", 1, 2, 3, 20, 21, 22, 23)
            .build();

        make("fa")
            .add("MORNING", "صبح", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "بعد از ظهر", 12, 13, 14, 15, 16)
            .add("EVENING", "عصر", 17, 18)
            .add("NIGHT", "شب", 0, 1, 2, 3, 19, 20, 21, 22, 23)
            .build();

        make("hy")
            .add("MORNING", "առավոտ", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "ցերեկ", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "երեկո", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "գիշեր", 0, 1, 2, 3, 4, 5)
            .build();

        make("ka")
            .add("MORNING", "დილა", 5, 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "მეორე ნახევარი", 12, 13, 14, 15, 16)
            .add("AFTERNOON", "მეირე ნახევარი", 17)
            .add("EVENING", "საღამო", 18, 19, 20)
            .add("NIGHT", "ღამე", 0, 1, 2, 3, 4, 21, 22, 23)
            .build();

        make("sq")
            .add("EARLY_MORNING", "mëngjes", 4, 5, 6, 7, 8)
            .add("MORNING", "paradite", 9, 10, 11)
            .add("AFTERNOON", "pasdite", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "mbrëmje", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "natë", 0, 1, 2, 3)
            .build();

        make("ur")
            .add("MORNING", "صبح", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "دوپہر", 12, 13, 14, 15)
            .add("AFTERNOON", "سہ پہر", 16, 17)
            .add("EVENING", "شام", 18, 19)
            .add("NIGHT", "رات", 0, 1, 2, 3, 20, 21, 22, 23)
            .build();

        make("hi")
            .add("MORNING", "सुबह", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "दोपहर", 12, 13, 14, 15)
            .add("EVENING", "शाम", 16, 17, 18, 19)
            .add("NIGHT", "रात", 0, 1, 2, 3, 20, 21, 22, 23)
            .build();

        make("bn")
            .add("EARLY_MORNING", "ভোর", 4, 5)
            .add("MORNING", "সকাল", 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "দুপুর", 12, 13, 14, 15)
            .add("AFTERNOON", "বিকাল", 16, 17)
            .add("EVENING", "সন্ধ্যা", 18, 19)
            .add("NIGHT", "রাত্রি", 0, 1, 2, 3, 20, 21, 22, 23)
            .build();

        make("gu")
            .add("MORNING", "સવાર", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "બપોર", 12, 13, 14, 15)
            .add("EVENING", "સાંજ", 16, 17, 18, 19)
            .add("NIGHT", "રાત", 0, 1, 2, 3, 20, 21, 22, 23)
            .build();

        make("mr")
            .add("EARLY_MORNING", "पहाटे", 4, 5)
            .add("MORNING", "सकाळी", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "दुपारी", 12, 13, 14, 15)
            .add("EVENING", "संध्याकाळी", 16, 17, 18, 19)
            .add("NIGHT", "रात्री", 0, 1, 2, 20, 21, 22, 23)
            .add("LATE_NIGHT", "रात्र", 3)
            .build();

        make("ne")
            .add("MORNING", "विहान", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "अपरान्ह", 12, 13, 14, 15)
            .add("AFTERNOON", "साँझ", 16, 17, 18)
            .add("EVENING", "बेलुका", 19, 20, 21)
            .add("NIGHT", "रात", 0, 1, 2, 3, 22, 23)
            .build();

        make("pa")
            .add("MORNING", "ਸਵੇਰ", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "ਦੁਪਹਿਰ", 12, 13, 14, 15)
            .add("EVENING", "ਸ਼ਾਮ", 16, 17, 18, 19, 20)
            .add("NIGHT", "ਰਾਤ", 0, 1, 2, 3, 21, 22, 23)
            .build();

        make("si")
            .add("EARLY_MORNING", "පාන්දර", 1, 2, 3, 4, 5)
            .add("MORNING", "උදේ", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "දවල්", 12, 13)
            .add("EVENING", "හවස", 14, 15, 16, 17)
            .add("NIGHT", "රෑ", 18, 19, 20, 21, 22, 23)
            .add("LATE_NIGHT", "මැදියමට පසු", 0)
            .build();

        make("ta")
            .add("EARLY_MORNING", "அதிகாலை", 3, 4)
            .add("MORNING", "காலை", 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "பிற்பகல்", 12, 13, 16, 17)
            .add("EVENING", "மாலை", 14, 15, 18, 19, 20)
            .add("NIGHT", "இரவு", 0, 1, 2, 21, 22, 23)
            .build();

        make("te")
            .add("MORNING", "ఉదయం", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "మధ్యాహ్నం", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "సాయంత్రం", 18, 19, 20)
            .add("NIGHT", "రాత్రి", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("ml")
            .add("EARLY_MORNING", "പുലർച്ചെ", 3, 4, 5)
            .add("MORNING", "രാവിലെ", 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "ഉച്ചയ്ക്ക്", 12, 13)
            .add("AFTERNOON", "ഉച്ചതിരിഞ്ഞ്", 14)
            .add("EARLY_EVENING", "വൈകുന്നേരം", 15, 16, 17)
            .add("EVENING", "സന്ധ്യയ്ക്ക്", 18)
            .add("NIGHT", "രാത്രി", 0, 1, 2, 19, 20, 21, 22, 23)
            .build();

        make("kn")
            .add("MORNING", "ಬೆಳಗ್ಗೆ", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "ಮಧ್ಯಾಹ್ನ", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "ಸಂಜೆ", 18, 19, 20)
            .add("NIGHT", "ರಾತ್ರಿ", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("zh")
            .add("EARLY_MORNING", "早上", 5, 6, 7)
            .add("MORNING", "上午", 8, 9, 10, 11)
            .add("AFTERNOON", "中午", 12)
            .add("EVENING", "下午", 13, 14, 15, 16, 17, 18)
            .add("NIGHT", "晚上", 19, 20, 21, 22, 23)
            .add("LATE_NIGHT", "凌晨", 0, 1, 2, 3, 4)
            .build();

        make("ja")
            .add("EARLY_MORNING", "朝", 6, 7, 8)
            .add("MORNING", "午前", 9, 10, 11)
            .add("EARLY_AFTERNOON", "午後", 12, 13, 14, 15)
            .add("AFTERNOON", "夕方", 16, 17, 18)
            .add("EVENING", "夜", 19, 20, 21, 22)
            .add("NIGHT", "夜中", 0, 1, 2, 3, 23)
            .add("LATE_NIGHT", "明け方", 4, 5)
            .build();

        make("ko")
            .add("EARLY_MORNING", "새벽", 3, 4, 5)
            .add("MORNING", "오전", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "오후", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "저녁", 18, 19, 20)
            .add("NIGHT", "밤", 0, 1, 2, 21, 22, 23)
            .build();

        make("tr")
            .add("EARLY_MORNING", "sabah", 6, 7, 8, 9, 10)
            .add("MORNING", "öğleden önce", 11)
            .add("EARLY_AFTERNOON", "öğleden sonra", 12, 13, 14, 15, 16, 17)
            .add("AFTERNOON", "akşamüstü", 18)
            .add("EVENING", "akşam", 19, 20)
            .add("NIGHT", "gece", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("az")
            .add("EARLY_MORNING", "sübh", 4, 5)
            .add("MORNING", "səhər", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "gündüz", 12, 13, 14, 15, 16)
            .add("EVENING", "axşamüstü", 17, 18)
            .add("NIGHT", "axşam", 19, 20, 21, 22, 23)
            .add("LATE_NIGHT", "gecə", 0, 1, 2, 3)
            .build();

        make("kk")
            .add("MORNING", "таң", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "түс", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "кеш", 18, 19, 20)
            .add("NIGHT", "түн", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("ky")
            .add("MORNING", "эртең менен", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "түштөн кийин", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "кечкурун", 18, 19, 20)
            .add("NIGHT", "түн", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("uz")
            .add("MORNING", "ertalab", 6, 7, 8, 9, 10)
            .add("AFTERNOON", "kunduz", 11, 12, 13, 14, 15, 16, 17)
            .add("EVENING", "kechqurun", 18, 19, 20, 21)
            .add("NIGHT", "tun", 0, 1, 2, 3, 4, 5, 22, 23)
            .build();

        make("et")
            .add("MORNING", "hommik", 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "pärastlõuna", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "õhtu", 18, 19, 20, 21, 22)
            .add("NIGHT", "öö", 0, 1, 2, 3, 4, 23)
            .build();

        make("fi")
            .add("EARLY_MORNING", "aamu", 5, 6, 7, 8, 9)
            .add("MORNING", "aamupäivä", 10, 11)
            .add("AFTERNOON", "iltapäivä", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "ilta", 18, 19, 20, 21, 22)
            .add("NIGHT", "yö", 0, 1, 2, 3, 4, 23)
            .build();

        make("hu")
            .add("EARLY_MORNING", "reggel", 6, 7, 8)
            .add("MORNING", "délelőtt", 9, 10, 11)
            .add("AFTERNOON", "délután", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "este", 18, 19, 20)
            .add("NIGHT", "éjjel", 0, 1, 2, 3, 21, 22, 23)
            .add("LATE_NIGHT", "hajnal", 4, 5)
            .build();

        make("th")
            .add("MORNING", "เช้า", 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "เที่ยง", 12)
            .add("AFTERNOON", "บ่าย", 13, 14, 15)
            .add("EARLY_EVENING", "เย็น", 16, 17)
            .add("EVENING", "ค่ำ", 18, 19, 20)
            .add("NIGHT", "กลางคืน", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("lo")
            .add("EARLY_MORNING", "​ກາງ​ຄືນ", 0, 1, 2, 3, 4)
            .add("MORNING", "​ເຊົ້າ", 5, 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "​ສວາຍ", 12, 13, 14, 15)
            .add("AFTERNOON", "ແລງ", 16)
            .add("EVENING", "​ແລງ", 17, 18, 19)
            .add("NIGHT", "​ຄ່ຳ", 20, 21, 22, 23)
            .build();

        make("ar")
            .add("EARLY_MORNING", "فجرا", 3, 4, 5)
            .add("MORNING", "صباحا", 6, 7, 8, 9, 10, 11)
            .add("EARLY_AFTERNOON", "ظهرا", 12)
            .add("AFTERNOON", "بعد الظهر", 13, 14, 15, 16, 17)
            .add("EVENING", "مساء", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", " منتصف الليل", 0)
            .add("LATE_NIGHT", "ليلا", 1, 2)
            .build();

        make("he")
            .add("MORNING", "בוקר", 5, 6, 7, 8, 9, 10)
            .add("EARLY_AFTERNOON", "צהריים", 11, 12, 13, 14)
            .add("AFTERNOON", "אחר הצהריים", 15, 16, 17)
            .add("EVENING", "ערב", 18, 19, 20, 21)
            .add("NIGHT", "לילה", 0, 1, 2, 3, 4, 22, 23)
            .build();

        make("id")
            .add("MORNING", "pagi", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            .add("AFTERNOON", "siang", 10, 11, 12, 13, 14)
            .add("EVENING", "sore", 15, 16, 17)
            .add("NIGHT", "malam", 18, 19, 20, 21, 22, 23)
            .build();

        make("ms")
            .add("EARLY_MORNING", "tengah malam", 0)
            .add("MORNING", "pagi", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "tengah hari", 12, 13)
            .add("EVENING", "petang", 14, 15, 16, 17, 18)
            .add("NIGHT", "malam", 19, 20, 21, 22, 23)
            .build();

        make("fil")
            .add("EARLY_MORNING", "madaling-araw", 0, 1, 2, 3, 4, 5)
            .add("MORNING", "umaga", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "tanghali", 12, 13, 14, 15)
            .add("EVENING", "hapon", 16, 17)
            .add("NIGHT", "gabi", 18, 19, 20, 21, 22, 23)
            .build();

        make("vi")
            .add("MORNING", "sáng", 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "chiều", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "tối", 18, 19, 20)
            .add("NIGHT", "đêm", 0, 1, 2, 3, 21, 22, 23)
            .build();

        make("km")
            .add("MORNING", "ព្រឹក", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "រសៀល", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "ល្ងាច", 18, 19, 20)
            .add("NIGHT", "យប់", 21, 22, 23)
            .build();

        make("sw")
            .add("EARLY_MORNING", "alfajiri", 4, 5, 6)
            .add("MORNING", "asubuhi", 7, 8, 9, 10, 11)
            .add("AFTERNOON", "mchana", 12, 13, 14, 15)
            .add("EVENING", "jioni", 16, 17, 18)
            .add("NIGHT", "usiku", 0, 1, 2, 3, 19, 20, 21, 22, 23)
            .build();

        make("zu")
            .add("EARLY_MORNING", "ntathakusa", 0, 1, 2, 3, 4, 5)
            .add("MORNING", "ekuseni", 6, 7, 8, 9)
            .add("AFTERNOON", "emini", 10, 11, 12)
            .add("EVENING", "ntambama", 13, 14, 15, 16, 17, 18)
            .add("NIGHT", "ebusuku", 19, 20, 21, 22, 23)
            .build();

        make("am")
            .add("MORNING", "ጥዋት", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "ከሰዓት በኋላ", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "ማታ", 18, 19, 20, 21, 22, 23)
            .add("NIGHT", "ሌሊት", 0, 1, 2, 3, 4, 5)
            .build();

        make("eu")
            .add("EARLY_MORNING", "goizaldea", 0, 1, 2, 3, 4, 5)
            .add("MORNING", "goiza", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "eguerdia", 12, 13)
            .add("EVENING", "arratsaldea", 14, 15, 16, 17, 18, 19, 20)
            .add("NIGHT", "gaua", 21, 22, 23)
            .build();

        make("mn")
            .add("MORNING", "өглөө", 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "өдөр", 12, 13, 14, 15, 16, 17)
            .add("EVENING", "орой", 18, 19, 20)
            .add("NIGHT", "шөнө", 0, 1, 2, 3, 4, 5, 21, 22, 23)
            .build();

        make("my")
            .add("MORNING", "နံနက်", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .add("AFTERNOON", "နေ့လည်", 12, 13, 14, 15)
            .add("EVENING", "ညနေ", 16, 17, 18)
            .add("NIGHT", "ည", 19, 20, 21, 22, 23)
            .build();
    }
}
