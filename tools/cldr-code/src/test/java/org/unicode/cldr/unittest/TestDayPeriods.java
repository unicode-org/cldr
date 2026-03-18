package org.unicode.cldr.unittest;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class TestDayPeriods extends TestFmwkPlus {

    CLDRConfig CONFIG = CLDRConfig.getInstance();
    SupplementalDataInfo SUPPLEMENTAL = CONFIG.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestDayPeriods().run(args);
    }

    @SuppressWarnings("unchecked")
    public void TestBasicDayPeriods() {
        int count = 0;
        for (String locale : SUPPLEMENTAL.getDayPeriodLocales(DayPeriodInfo.Type.format)) {
            DayPeriodInfo dayPeriodFormat =
                    SUPPLEMENTAL.getDayPeriods(DayPeriodInfo.Type.format, locale);
            DayPeriodInfo dayPeriodSelection =
                    SUPPLEMENTAL.getDayPeriods(DayPeriodInfo.Type.selection, locale);

            Set<R2<Integer, DayPeriod>> sortedFormat = getSet(dayPeriodFormat);
            Set<R2<Integer, DayPeriod>> sortedSelection = getSet(dayPeriodSelection);

            assertRelation(
                    locale + " format ⊇ selection",
                    true,
                    sortedFormat,
                    CONTAINS_ALL,
                    sortedSelection); //
            logln(
                    locale
                            + "\t"
                            + CONFIG.getEnglish().nameGetter().getNameFromIdentifier(locale)
                            + "\t"
                            + dayPeriodFormat
                            + "\t"
                            + dayPeriodSelection);
            count += dayPeriodFormat.getPeriodCount();
        }
        assertTrue("At least some day periods exist", count > 5);
    }

    private Set<R2<Integer, DayPeriod>> getSet(DayPeriodInfo dayPeriodInfo) {
        Set<R2<Integer, DayPeriod>> sorted = new TreeSet<>();
        for (int i = 0; i < dayPeriodInfo.getPeriodCount(); ++i) {
            R3<Integer, Boolean, DayPeriod> info = dayPeriodInfo.getPeriod(i);
            int start = info.get0();
            assertEquals(
                    "Time is even hours", start / DayPeriodInfo.HOUR * DayPeriodInfo.HOUR, start);
            R2<Integer, DayPeriod> row = Row.of(start, info.get2());
            sorted.add(row);
        }
        return sorted;
    }

    @SuppressWarnings("unchecked")
    public void TestAttributes() {
        Factory factory = CONFIG.getCldrFactory();
        HashSet<String> AMPM = new HashSet<>(Arrays.asList("am", "pm"));
        for (String locale : factory.getAvailableLanguages()) {
            DayPeriodInfo periodInfo = SUPPLEMENTAL.getDayPeriods(Type.format, locale);
            List<DayPeriod> periods = periodInfo.getPeriods();
            CLDRFile cldrFile = CONFIG.getCLDRFile(locale, false);
            for (Iterator<String> it =
                            cldrFile.iterator(
                                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/");
                    it.hasNext(); ) {
                String path = it.next();
                if (path.endsWith("alias")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String type = parts.getAttributeValue(-1, "type");
                if (AMPM.contains(type)) {
                    continue;
                }
                DayPeriod period;
                try {
                    period = DayPeriodInfo.DayPeriod.fromString(type);
                } catch (Exception e) {
                    assertNull(locale + " : " + type, e);
                    continue;
                }
                assertRelation(locale, true, periods, TestFmwkPlus.CONTAINS, period);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void TestForBothDayPeriodTypes() {
        Set<String> localesWithFormatRules =
                SUPPLEMENTAL.getDayPeriodLocales(DayPeriodInfo.Type.format);
        Set<String> localesWithSelectionRules =
                SUPPLEMENTAL.getDayPeriodLocales(DayPeriodInfo.Type.selection);
        Set<String> localesWithFormatButNotSelectionRules =
                new HashSet<String>(localesWithFormatRules);
        localesWithFormatButNotSelectionRules.removeAll(localesWithSelectionRules);
        if (!localesWithFormatButNotSelectionRules.isEmpty()) {
            errln(
                    "Locales that have dayPeriod format rules but lack selection rules: "
                            + localesWithFormatButNotSelectionRules);
        }
        Set<String> localesWithSelectionButNotFormatRules =
                new HashSet<String>(localesWithSelectionRules);
        localesWithSelectionButNotFormatRules.removeAll(localesWithFormatRules);
        if (!localesWithSelectionButNotFormatRules.isEmpty()) {
            errln(
                    "Locales that have dayPeriod selection rules but lack format rules: "
                            + localesWithSelectionButNotFormatRules);
        }
    }

    private Map<DayPeriodInfo, Set<String[]>> dayPeriodFormatToLocales =
            new TreeMap<DayPeriodInfo, Set<String[]>>();
    private Map<DayPeriodInfo, Set<String[]>> dayPeriodSelectionToLocales =
            new TreeMap<DayPeriodInfo, Set<String[]>>();

    @SuppressWarnings("unchecked")
    public void TestForRuleSetConsolidation() {
        // We need to check the actual entries in dayPeriods.xml instead of accessing the data
        // through SupplementalDataInfo, which just maps each locale to its dayPeriod rules (for
        // each type)
        DayPeriodDataHandler dayPeriodDataHandler = new DayPeriodDataHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(dayPeriodDataHandler);
        String pathToDayPeriodData = CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY + "dayPeriods.xml";
        xfr.read(pathToDayPeriodData, -1, true);
        dayPeriodDataHandler.cleanup();

        // Now check whether the same DayPeriodInfo is used for multiple locale groups
        for (DayPeriodInfo dpi : dayPeriodFormatToLocales.keySet()) {
            Set<String[]> localesSet = dayPeriodFormatToLocales.get(dpi);
            if (localesSet.size() > 1) {
                errln(
                        "Same dayPeriod format rules used for multiple locale groups:"
                                + GetLocalesSetString(localesSet));
            }
        }
        for (DayPeriodInfo dpi : dayPeriodSelectionToLocales.keySet()) {
            Set<String[]> localesSet = dayPeriodSelectionToLocales.get(dpi);
            if (localesSet.size() > 1) {
                errln(
                        "Same dayPeriod selection rules used for multiple locale groups:"
                                + GetLocalesSetString(localesSet));
            }
        }
    }

    private String GetLocalesSetString(Set<String[]> localesSet) {
        String localesSetString = "";
        for (String[] locales : localesSet) {
            localesSetString = localesSetString.concat(" [");
            for (String locale : locales) {
                localesSetString = localesSetString.concat(" " + locale);
            }
            localesSetString = localesSetString.concat(" ]");
        }
        return localesSetString;
    }

    // The DayPeriodInfo parsing logic below is adapted from SupplementalDataInfo addDayPeriodPath,
    // addDayPeriodInfo etc.

    private transient String lastDayPeriodLocales = null;
    private transient DayPeriodInfo.Type lastDayPeriodType = null;
    private transient DayPeriodInfo.Builder dayPeriodBuilder = new DayPeriodInfo.Builder();
    static Pattern PARSE_TIME = PatternCache.get("(\\d\\d?):(\\d\\d)");

    class DayPeriodDataHandler extends XMLFileReader.SimpleHandler {
        public void cleanup() { // Finish processing anything left in the file
            if (lastDayPeriodLocales != null) {
                addDayPeriodInfo();
            }
        }

        @Override
        public void handlePathValue(String path, String value) {
            try {
                if (!path.contains("dayPeriodRuleSet")) {
                    return;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String typeString = parts.getAttributeValue(1, "type");
                String locales = parts.getAttributeValue(2, "locales").trim();
                DayPeriodInfo.Type type =
                        (typeString == null)
                                ? DayPeriodInfo.Type.format
                                : DayPeriodInfo.Type.valueOf(typeString.trim());
                if (!locales.equals(lastDayPeriodLocales) || type != lastDayPeriodType) {
                    if (lastDayPeriodLocales != null) {
                        addDayPeriodInfo();
                    }
                    lastDayPeriodLocales = locales;
                    lastDayPeriodType = type;
                }
                DayPeriod dayPeriod;
                try {
                    dayPeriod = DayPeriod.fromString(parts.getAttributeValue(-1, "type"));
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    return;
                }
                String at = parts.getAttributeValue(-1, "at");
                String from = parts.getAttributeValue(-1, "from");
                String after = parts.getAttributeValue(-1, "after");
                String to = parts.getAttributeValue(-1, "to");
                String before = parts.getAttributeValue(-1, "before");
                if (at != null) {
                    if (from != null || after != null || to != null || before != null) {
                        throw new IllegalArgumentException();
                    }
                    from = at;
                    to = at;
                } else if ((from == null) == (after == null) || (to == null) == (before == null)) {
                    throw new IllegalArgumentException();
                }
                boolean includesStart = from != null;
                boolean includesEnd = to != null;
                int start = parseTime(includesStart ? from : after);
                int end = parseTime(includesEnd ? to : before);
                // Check if any periods contain 0, e.g. 1700 - 300
                if (start > end) {
                    dayPeriodBuilder.add(
                            dayPeriod, start, includesStart, parseTime("24:00"), includesEnd);
                    dayPeriodBuilder.add(
                            dayPeriod, parseTime("0:00"), includesStart, end, includesEnd);
                } else {
                    dayPeriodBuilder.add(dayPeriod, start, includesStart, end, includesEnd);
                }
            } catch (Exception e) {
                throw (IllegalArgumentException)
                        new IllegalArgumentException("path: " + path + ",\tvalue: " + value)
                                .initCause(e);
            }
        }

        private int parseTime(String string) {
            Matcher matcher = PARSE_TIME.matcher(string);
            if (!matcher.matches()) {
                throw new IllegalArgumentException();
            }
            return (Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2)))
                    * 60
                    * 1000;
        }

        private void addDayPeriodInfo() {
            String[] locales = lastDayPeriodLocales.split("\\s+");
            DayPeriodInfo dpi = dayPeriodBuilder.finish(locales);
            // fix the rest of this
            Map<DayPeriodInfo, Set<String[]>> dayPeriodToLocales =
                    (lastDayPeriodType == DayPeriodInfo.Type.format)
                            ? dayPeriodFormatToLocales
                            : dayPeriodSelectionToLocales;
            Set<String[]> localesSet = dayPeriodToLocales.get(dpi);
            if (localesSet == null) {
                localesSet = new HashSet<String[]>();
                localesSet.add(locales);
                dayPeriodToLocales.put(dpi, localesSet);
            } else {
                localesSet.add(locales);
            }
        }
    }
}
