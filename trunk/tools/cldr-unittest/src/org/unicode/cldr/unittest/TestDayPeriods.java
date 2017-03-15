package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;

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
            DayPeriodInfo dayPeriodFormat = SUPPLEMENTAL.getDayPeriods(DayPeriodInfo.Type.format, locale);
            DayPeriodInfo dayPeriodSelection = SUPPLEMENTAL.getDayPeriods(DayPeriodInfo.Type.selection, locale);

            Set<R2<Integer, DayPeriod>> sortedFormat = getSet(dayPeriodFormat);
            Set<R2<Integer, DayPeriod>> sortedSelection = getSet(dayPeriodSelection);

            assertRelation(locale + " format ⊇ selection", true, sortedFormat, CONTAINS_ALL, sortedSelection);//
            logln(locale + "\t" + CONFIG.getEnglish().getName(locale) + "\t" + dayPeriodFormat + "\t" + dayPeriodSelection);
            count += dayPeriodFormat.getPeriodCount();
        }
        assertTrue("At least some day periods exist", count > 5);
    }

    private Set<R2<Integer, DayPeriod>> getSet(DayPeriodInfo dayPeriodInfo) {
        Set<R2<Integer, DayPeriod>> sorted = new TreeSet<>();
        for (int i = 0; i < dayPeriodInfo.getPeriodCount(); ++i) {
            R3<Integer, Boolean, DayPeriod> info = dayPeriodInfo.getPeriod(i);
            int start = info.get0();
            assertEquals("Time is even hours", (int) (start / DayPeriodInfo.HOUR) * DayPeriodInfo.HOUR, (int) start);
            R2<Integer, DayPeriod> row = Row.of(start, info.get2());
            sorted.add(row);
        }
        return sorted;
    }

    @SuppressWarnings("unchecked")
    public void TestAttributes() {
        Factory factory = CONFIG.getCldrFactory();
        XPathParts parts = new XPathParts();
        HashSet<String> AMPM = new HashSet<>(Arrays.asList("am", "pm"));
        for (String locale : factory.getAvailableLanguages()) {
            DayPeriodInfo periodInfo = SUPPLEMENTAL.getDayPeriods(Type.format, locale);
            List<DayPeriod> periods = periodInfo.getPeriods();
            CLDRFile cldrFile = CONFIG.getCLDRFile(locale, false);
            for (Iterator<String> it = cldrFile.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/"); it.hasNext();) {
                String path = it.next();
                if (path.endsWith("alias")) {
                    continue;
                }
                parts.set(path);
                String type = parts.getAttributeValue(-1, "type");
                if (AMPM.contains(type)) {
                    continue;
                }
                String format = parts.getAttributeValue(-3, "type");
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
}
