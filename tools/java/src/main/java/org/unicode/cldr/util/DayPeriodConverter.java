package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.DayPeriods.DayPeriod;

import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row.R3;
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
        // generateFormat();
        generateFieldNames();
    }

    private static void generateFieldNames() {
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        Factory factory = CLDRConfig.getInstance().getFullCldrFactory();
        EnumSet<DayPeriodInfo.DayPeriod> dayPeriodSet = EnumSet.noneOf(DayPeriodInfo.DayPeriod.class);

        String prefix = getPrefix("stand-alone");

        for (String locale : sdi.getDayPeriodLocales(DayPeriodInfo.Type.format)) {
            ULocale uLocale = new ULocale(locale);
            DayPeriodInfo dayPeriodInfo = sdi.getDayPeriods(DayPeriodInfo.Type.format, locale);
            System.out.println("# " + locale);
            dayPeriodSet.clear();
            dayPeriodSet.addAll(dayPeriodInfo.getPeriods());
            final boolean fieldType = false;
            if (!fieldType) {
                //System.out.println("\t<dayPeriodContext type=\"stand-alone\">");
                //System.out.println("\t\t<dayPeriodWidth type=\"wide\">");
                CLDRFile cldrFile = factory.make(locale, false);
                for (String path : cldrFile) {
                    if (path.endsWith("/alias")) {
                        continue;
                    }
                    if (path.startsWith(prefix)) {
                        XPathParts parts = XPathParts.getFrozenInstance(path);
                        String width = parts.getAttributeValue(-2, "type");
                        DayPeriodInfo.DayPeriod period = DayPeriodInfo.DayPeriod.fromString(parts.getAttributeValue(-1, "type"));
                        String draft = parts.getAttributeValue(-1, "draft");
                        //if (period != DayPeriodInfo.DayPeriod.am || period != DayPeriodInfo.DayPeriod.pm || width.equals("wide")) {
                        System.out.println("#old: «" + cldrFile.getStringValue(path) + "»"
                            + ", width: " + width
                            + ", period: " + period
                            + (draft == null ? "" : ", draft: " + draft));
                        System.out.println("locale=" + locale
                            + " ; action=delete"
                            + " ; path=" + path);
                        //}
                    }
                }

//                CLDRFile cldrFile = factory.make(locale, true);
                addOldDayPeriod(cldrFile, DayPeriodInfo.DayPeriod.am);
                addOldDayPeriod(cldrFile, DayPeriodInfo.DayPeriod.pm);
            }
            for (DayPeriodInfo.DayPeriod dayPeriod : dayPeriodSet) {
                if (fieldType) {
                    String name = getNativeName(uLocale, dayPeriod);
                    System.out.println("\t<field type=\"dayPeriod-" + dayPeriod + "\">");
                    System.out.println("\t\t<displayName>" + name + "</displayName>");
                    System.out.println("\t</field>");
                } else {
                    String name = getNativeName(uLocale, dayPeriod);
                    //System.out.println("\t\t\t<dayPeriod type=\"" + dayPeriod + "\">" + name + "</dayPeriod>");
                    //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="narrow"]/dayPeriod[@type="am"]
//                    String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods"
//                        + "/dayPeriodContext[@type=\"stand-alone\"]"
//                        + "/dayPeriodWidth[@type=\"wide\"]"
//                        + "/dayPeriod[@type=\"" + dayPeriod + "\"]";
                    if (name != null) {
                        showModLine(locale, dayPeriod, name, false);
                    }
                }
            }
//            if (!fieldType) {
//                System.out.println("\t\t</dayPeriodWidth>");
//                System.out.println("\t</dayPeriodContext>");
//            }
            System.out.println();
        }
    }

    private static void showModLine(String locale, DayPeriodInfo.DayPeriod dayPeriod, String name, boolean draft) {
        String path = getPath("stand-alone", "wide", dayPeriod);
        if (draft) {
            path += "[@draft=\"provisional\"]";
        }
        System.out.println("locale=" + locale
            + " ; action=add"
            + " ; new_value=" + name
            + " ; new_path=" + path);
    }

    private static void addOldDayPeriod(CLDRFile cldrFile, DayPeriodInfo.DayPeriod period) {
        String amString = cldrFile.getStringValue(getPath("format", "wide", period));
        if (amString != null) {
            String locale = cldrFile.getLocaleID();
            showModLine(locale, period, amString, !locale.equals("root") && !locale.equals("en"));
            //System.out.println("\t\t\t<dayPeriod type=\"" + period + "\">" + amString + "</dayPeriod>");
        }
    }

    static String getPath(String context, String width, DayPeriodInfo.DayPeriod dayPeriod) {
        return getPrefix(context)
            + "/dayPeriodWidth[@type=\"" + width + "\"]"
            + "/dayPeriod[@type=\"" + dayPeriod + "\"]";
    }

    private static String getPrefix(String context) {
        return "//ldml/dates/calendars"
            + "/calendar[@type=\"gregorian\"]"
            + "/dayPeriods"
            + "/dayPeriodContext[@type=\"" + context + "\"]";
    }

    static void generateFormat() {
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        M3<ULocale, Integer, DayPeriodInfo.DayPeriod> allData = ChainedMap.of(new TreeMap(LanguageGroup.COMPARATOR), new TreeMap(),
            DayPeriodInfo.DayPeriod.class);
        for (String locale : sdi.getDayPeriodLocales(DayPeriodInfo.Type.selection)) {
            ULocale ulocale = new ULocale(locale);
            NoonMidnight nm = NoonMidnight.get(locale);
            boolean hasNoon = nm != null && nm.noon != null;
            boolean hasMidnight = nm != null && nm.midnight != null;
            DayPeriodInfo data = sdi.getDayPeriods(DayPeriodInfo.Type.selection, locale);
            if (hasMidnight) {
                allData.put(ulocale, 0, DayPeriodInfo.DayPeriod.midnight);
            }
            if (hasNoon) {
                allData.put(ulocale, 12 * DayPeriodInfo.HOUR, DayPeriodInfo.DayPeriod.noon);
            }
            int lastTime = -1;
            DayPeriodInfo.DayPeriod lastPeriod = null;
            for (int index = 0; index < data.getPeriodCount(); ++index) {
                R3<Integer, Boolean, DayPeriodInfo.DayPeriod> row = data.getPeriod(index);
                DayPeriodInfo.DayPeriod period = row.get2();
                int time = row.get0();
                if (!row.get1()) {
                    time += 1;
                } else if (hasMidnight && time == 0) {
                    time += 1;
                } else if (hasNoon) {
                    if (time == 12 * DayPeriodInfo.HOUR) {
                        time += 1;
                    } else if (lastTime < 12 * DayPeriodInfo.HOUR && time > 12 * DayPeriodInfo.HOUR) {
                        System.out.println(locale + ": Splitting " + lastTime + ", " + lastPeriod + ", " + time + ", " + period);
                        allData.put(ulocale, 12 * DayPeriodInfo.HOUR + 1, lastPeriod);
                    }
                }
                allData.put(ulocale, time, period);
                lastTime = time;
                lastPeriod = period;
            }
        }
        for (Entry<ULocale, Map<Integer, org.unicode.cldr.util.DayPeriodInfo.DayPeriod>> entry : allData) {
            ULocale locale = entry.getKey();
            System.out.println("\t\t<dayPeriodRules locales=\"" + locale + "\">");
            ArrayList<Entry<Integer, DayPeriodInfo.DayPeriod>> list = new ArrayList<>(entry.getValue().entrySet());
            for (int i = 0; i < list.size(); ++i) {
                Entry<Integer, org.unicode.cldr.util.DayPeriodInfo.DayPeriod> item = list.get(i);
                //System.out.println(item.getKey() + " = " + item.getValue());
                DayPeriodInfo.DayPeriod dayPeriod = item.getValue();
                int start = item.getKey();
                int end = i + 1 == list.size() ? 24 * DayPeriodInfo.HOUR : list.get(i + 1).getKey();
                String startType = "from";
                if ((start & 1) != 0) {
                    startType = "after";
                }
                start /= DayPeriodInfo.HOUR;
                end /= DayPeriodInfo.HOUR;
                System.out.println("\t\t\t<dayPeriodRule type=\""
                    + dayPeriod.toString().toLowerCase(Locale.ENGLISH)
                    + "\" "
                    + (start == end ? "at=\"" + start
                        : startType + "=\"" + start + ":00\" before=\"" + end)
                    + ":00\"/>" +
                    " <!-- " + getNativeName(locale, dayPeriod)
                    + " -->");
            }
            System.out.println("\t\t</dayPeriodRules>");
        }
    }

    void oldMain() {
//        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
//
//        for (String locale : sdi.getDayPeriodLocales(DayPeriodInfo.Type.selection)) {
//
//            StringBuilder result = new StringBuilder("\t\t<dayPeriodRules locales=\"" + locale + "\">\n";
//            for (int index = 0; index < data.getPeriodCount(); ++index) {
//                R3<Integer, Boolean, DayPeriodInfo.DayPeriod> periodData = data.getPeriod(index);
//                public String toCldr() {
//                    DayPeriod lastDayPeriod = data[0];
//                    int start = 0;
//                    for (int i = 1; i < 24; ++i) {
//                        DayPeriod dayPeriod = data[i];
//                        if (dayPeriod != lastDayPeriod) {
//                            result = addPeriod(result, lastDayPeriod, start, i);
//                            lastDayPeriod = dayPeriod;
//                            start = i;
//                        }
//                    }
//                    result = addPeriod(result, lastDayPeriod, start, 24);
//                    result += "\t\t</dayPeriodRules>";
//                    return result;
//                }
//
//                private String addPeriod(String result, DayPeriod dayPeriod, int start, int i) {
//                    result += "\t\t\t<dayPeriodRule type=\""
//                        + dayPeriod.toString().toLowerCase(Locale.ENGLISH)
//                        + "\" from=\""
//                        + start + ":00"
//                        + "\" before=\""
//                        + i + ":00"
//                        + "\"/> <!-- " + toNativeName.get(dayPeriod)
//                        + " -->\n";
//                    return result;
//                }
//
//            }
//            NoonMidnight nm = NoonMidnight.get(locale);
//
//            System.out.println(locale + "\t" + nm);
//        }
    }

    static final ULocale ROOT2 = new ULocale("root");

    private static String getNativeName(ULocale locale, DayPeriodInfo.DayPeriod dayPeriod) {
        NoonMidnight nm = NoonMidnight.get(locale.toString());
        if (nm == null) {
            return null;
        }
        if (dayPeriod == DayPeriodInfo.DayPeriod.noon) {
            return CldrUtility.ifNull(nm.noon, "missing-" + dayPeriod);
        } else if (dayPeriod == DayPeriodInfo.DayPeriod.midnight) {
            return CldrUtility.ifNull(nm.midnight, "missing-" + dayPeriod);
        }
        if (locale.equals(ROOT2)) {
            if (dayPeriod == DayPeriodInfo.DayPeriod.morning1) {
                return "am";
            } else if (dayPeriod == DayPeriodInfo.DayPeriod.afternoon1) {
                return "pm";
            }
        }
        DayInfo data = DATA.get(locale);
        if (data == null) {
            return "missing-" + dayPeriod;
        }
        DayPeriod otherDayPeriod = DayPeriod.valueOf(dayPeriod.toString().toUpperCase(Locale.ENGLISH));
        return data.toNativeName.get(otherDayPeriod);
    }

    static void writeOld() {
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

    static class NoonMidnight {
        public NoonMidnight(List<String> items) {
            locale = items.get(0);
            midnight = CldrUtility.ifEqual(items.get(1), "N/A", null);
            noon = CldrUtility.ifEqual(items.get(2), "N/A", null);
        }

        final String locale;
        final String noon;
        final String midnight;

        static NoonMidnight get(String locale) {
            return NOON_MIDNIGHT_MAP.get(locale);
        }

        @Override
        public String toString() {
            return locale + ", " + noon + ", " + midnight;
        }

        static Map<String, NoonMidnight> NOON_MIDNIGHT_MAP = new HashMap<>();
        static final String[] NOON_MIDNIGHT = {
            "en|midnight|noon",
            "af|middernag|N/A",
            "nl|middernacht|N/A",
            "de|Mitternacht|N/A",
            "da|midnat|N/A",
            "nb|midnatt|N/A",
            "sv|midnatt|N/A",
            "is|miðnætti|hádegi",
            "pt|meia-noite|meio-dia",
            "pt_PT|meia-noite|meio-dia",
            "gl|medianoite|mediodía",
            "es|medianoche|mediodía",
            "es_419|medianoche|mediodía",
            "ca|mitjanit|migdia",
            "it|mezzanotte|mezzogiorno",
            "ro|miezul nopții|amiază",
            "fr|minuit|midi",
            "fr-CA|minuit|midi",
            "hr|ponoć|podne",
            "bs|u ponoć|u podne",
            "sr|поноћ|подне",
            "sl|polnoč|poldne",
            "cs|půlnoc|poledne",
            "sk|polnoc|poludnie",
            "pl|północ|południe",
            "bg|полунощ|N/A",
            "mk|на полноќ|напладне",
            "ru|полночь|полдень",
            "uk|північ|полудень",
            "lt|vidurnaktis|vidurdienis",
            "lv|pusnakts|pusdienlaiks",
            "el|N/A|μεσημέρι",
            "fa|نیمه‌شب|ظهر",
            "hy|կեսգիշեր|կեսօր",
            "ka|შუაღამე|შუადღე",
            "sq|mesnatë|mesditë",
            "ur|آدھی رات|N/A",
            "hi|आधी रात|दोपहर",
            "bn|N/A|N/A",
            "gu|અડધી રાત|બપોર",
            "mr|मध्यरात्री|मध्यान्ह",
            "ne|मध्यरात|मध्यान्ह",
            "pa|ਅੱਧੀ ਰਾਤ|ਦੁਪਹਿਰ",
            "si|මැදියම|දවල්",
            "ta|நள்ளிரவு|நன்பகல்",
            "te|అర్థరాత్రి|మధ్యాహ్నం",
            "ml|അർദ്ധരാത്രി|ഉച്ചയ്ക്ക്",
            "kn|ಮಧ್ಯರಾತ್ರಿ|ಮಧ್ಯಾಹ್ನ",
            "zh|半夜|中午",
            "zh-TW|子夜|正午",
            "zh-HK|深夜|中午",
            "ja|真夜中|正午",
            "ko|자정|정오",
            "tr|gece yarısı|öğlen",
            "az|gecəyarı|günorta",
            "kk|түн жарымы|талтүс",
            "ky|түн ортосу|чак түш",
            "uz|yarim tun|tush",
            "et|kesköö|keskpäev",
            "fi|keskiyö|keskipäivä",
            "hu|éjfél|dél",
            "th|เที่ยงคืน|เที่ยง",
            "lo|ທ່ຽງ​ຄືນ|​ທ່ຽງ",
            "ar| منتصف الليل|ظهرا",
            "he|חצות|N/A",
            "id|tengah malam|tengah hari",
            "ms|Tengah malam|Tengah hari",
            "fil|hating-gabi|tangaling-tapat",
            "vi|nửa đêm|trưa",
            "km|អាធ្រាត្រ|ថ្ងៃ​ត្រង់",
            "sw|saa sita za usiku|saa sita za mchana",
            "zu|N/A|N/A",
            "am|እኩለ ሌሊት|ቀትር",
            "eu|gauerdia|eguerdia",
            "mn|шөнө дунд|үд дунд",
            "my|သန်းခေါင်ယံ|မွန်းတည့်",
        };
        static {
            Splitter BAR = Splitter.on('|').trimResults();
            for (String s : NOON_MIDNIGHT) {
                List<String> items = BAR.splitToList(s);
                NoonMidnight nm = new NoonMidnight(items);
                NOON_MIDNIGHT_MAP.put(items.get(0), nm);
            }
        }
    }
}
