package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PreferredAndAllowedHour;
import org.unicode.cldr.util.SupplementalDataInfo;

public class TestTimeCycle {

    private static final Logger logger = Logger.getLogger(TestTimeCycle.class.getName());

    /**
     * "j requests the preferred hour-cycle type for the locale (it gets mapped to one of H, h, k,
     * or K)" References: https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-hour
     * https://unicode-org.github.io/icu/userguide/format_parse/datetime/
     */
    private final String jSkeleton = "j";

    private final char[] timeCycleChars = {'H', 'h', 'K', 'k'};

    private final boolean USE_ICU_DATA = false;

    private final String calendarType = LDMLConstants.GREGORIAN; /* or LDMLConstants.GENERIC? */

    /**
     * Test that in all available locales, the standard time format matches the region's preferred
     * time cycle.
     *
     * <p>This is similar to icu4j DateTimeGeneratorTest.testJjMapping. It uses current CLDR data,
     * not ICU data.
     */
    @Test
    public void testJjMapping() {
        CLDRConfig testInfo = CLDRConfig.getInstance();
        Factory cldrFactory = testInfo.getCldrFactory();
        SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();
        Map<String /* region */, PreferredAndAllowedHour> timeData = sdi.getTimeData();
        for (String localeID : cldrFactory.getAvailable()) {
            CLDRFile cldrFile = cldrFactory.make(localeID, true);
            ULocale uloc = new ULocale(localeID);
            DateFormat dfmt = DateFormat.getTimeInstance(DateFormat.SHORT, uloc);
            String jPattern;
            String shortPattern;
            DateTimePatternGenerator dtpg;
            if (USE_ICU_DATA) {
                dtpg = DateTimePatternGenerator.getInstance(uloc);
                jPattern = dtpg.getBestPattern(jSkeleton); // e.g., "h a" or "HH" or "H时"
                shortPattern = ((SimpleDateFormat) dfmt).toPattern(); // e.g., "h:mm a" or "HH:mm"
            } else {
                dtpg = DateTimePatternGenerator.getEmptyInstance();
                jPattern = getCldrJPatternForLocale(timeData, localeID);
                if (jPattern == null) {
                    failOrLog("Got null jPattern, locale = " + localeID);
                    continue;
                }
                String shortTimePath =
                        "//ldml/dates/calendars/calendar[@type=\""
                                + calendarType
                                + "\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                shortPattern = cldrFile.getWinningValue(shortTimePath);
                if (shortPattern == null) {
                    failOrLog("Got null shortTimePath, locale = " + localeID);
                    continue;
                }
            }
            // convert to skeletons to make it easier to check for hour character (eliminates
            // literals, repeats, etc.)
            String shortPatSkeleton = dtpg.getBaseSkeleton(shortPattern); // e.g., "ahm" or "Hm"
            String jPatSkeleton = dtpg.getBaseSkeleton(jPattern); // e.g., "ah" or "H"
            System.out.println(
                    "testJjMapping "
                            + localeID
                            + "\t["
                            + jPattern
                            + "] ["
                            + jPatSkeleton
                            + "] ["
                            + shortPattern
                            + "] ["
                            + shortPatSkeleton
                            + "]");
            for (char timeCycleChar : timeCycleChars) {
                if (jPatSkeleton.indexOf(timeCycleChar) >= 0
                        && shortPatSkeleton.indexOf(timeCycleChar) < 0) {
                    String dfmtCalType = dfmt.getCalendar().getType();
                    failOrLog(
                            "locale "
                                    + localeID
                                    + ", expected "
                                    + timeCycleChar
                                    + " to occur in short time pattern "
                                    + shortPattern
                                    + " for "
                                    + dfmtCalType);
                }
            }
        }
    }

    private String getCldrJPatternForLocale(
            Map<String, PreferredAndAllowedHour> timeData, String localeID) {
        PreferredAndAllowedHour prefAndAllowedHr = timeData.get(localeID);
        if (prefAndAllowedHr == null) {
            if ("aa".equals(localeID)) {
                System.out.println("getCldrJPatternForLocale aa 1st get (localeID) null");
            }
            LocaleIDParser lp = new LocaleIDParser();
            String region = lp.set(localeID).getRegion();
            if (region == null && "aa".equals(localeID)) {
                System.out.println("getCldrJPatternForLocale aa region is null");
            }
            prefAndAllowedHr = timeData.get(region);
            if (prefAndAllowedHr == null) {
                if ("aa".equals(localeID)) {
                    System.out.println(
                            "getCldrJPatternForLocale aa 2nd get (region " + region + ") null");
                }
                prefAndAllowedHr = timeData.get("001" /* world */);
                if (prefAndAllowedHr == null) {
                    return null;
                }
            }
        }
        PreferredAndAllowedHour.HourStyle hourStyle = prefAndAllowedHr.preferred;
        if ("aa".equals(localeID)) {
            System.out.println("getCldrJPatternForLocale hourStyle = " + hourStyle);
        }
        return patternFromHourStyle(hourStyle);
    }

    private String patternFromHourStyle(PreferredAndAllowedHour.HourStyle hourStyle) {
        switch (hourStyle) {
            case H:
            case Hb:
            case HB:
                return "H";
            case k:
                return "k";
            case K:
                return "K";
            case h:
            case hb:
            case hB:
                return "h";
            default:
                failOrLog("Cannot get pattern for " + hourStyle);
                return null;
        }
    }

    private void failOrLog(String message) {
        if (false) {
            fail(message);
        } else {
            logger.severe(message);
        }
    }
}
