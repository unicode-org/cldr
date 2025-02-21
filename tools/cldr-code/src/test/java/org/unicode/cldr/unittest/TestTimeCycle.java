package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PreferredAndAllowedHour;
import org.unicode.cldr.util.StandardCodes;
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

    private final boolean USE_ICU_DATA = false; // temporary, debugging

    /**
     * TODO: find the preferred calendar for the locale (based on data from
     * SupplementalDataInfo.getCalendars(region)), and if it is not Gregorian then also check the
     * shortTimeFormat for that. Reference: https://unicode-org.atlassian.net/browse/CLDR-13589
     */
    private final String calendarType = LDMLConstants.GREGORIAN; /* or LDMLConstants.GENERIC? */

    private final String shortTimePath =
            "//ldml/dates/calendars/calendar[@type=\""
                    + calendarType
                    + "\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";

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
        Map<String, String> likelySubtags = sdi.getLikelySubtags();
        Set<String> skippedLocales = new TreeSet<>(), worldFallback = new TreeSet<>();
        Map<CLDRLocale, String> failedLocales = new TreeMap<>();
        for (String localeID : cldrFactory.getAvailable()) {
            CLDRLocale loc = CLDRLocale.getInstance(localeID);
            CLDRFile cldrFile = cldrFactory.make(localeID, true);
            ULocale uloc = new ULocale(localeID);
            DateFormat dfmt = DateFormat.getTimeInstance(DateFormat.SHORT, uloc);
            String jPattern, shortPattern;
            DateTimePatternGenerator dtpg;
            if (USE_ICU_DATA) {
                dtpg = DateTimePatternGenerator.getInstance(uloc);
                jPattern = dtpg.getBestPattern(jSkeleton); // e.g., "h a" or "HH" or "H时"
                shortPattern = ((SimpleDateFormat) dfmt).toPattern(); // e.g., "h:mm a" or "HH:mm"
            } else {
                dtpg = DateTimePatternGenerator.getEmptyInstance();
                jPattern = getJPattern(timeData, localeID, likelySubtags, worldFallback);
                if (jPattern == null) {
                    failedLocales.put(loc, "Null jPattern");
                    continue;
                }
                shortPattern = cldrFile.getWinningValue(shortTimePath);
                if (shortPattern == null) {
                    failedLocales.put(loc, "Null shortTimePath");
                    continue;
                }
                if (CLDRLocale.ROOT.equals(loc.getParent()) && !cldrFile.isHere(shortTimePath)) {
                    skippedLocales.add(localeID);
                    continue;
                }
            }
            // Convert to skeletons to make it easier to check for hour character (eliminates
            // literals, repeats, etc.)
            String shortPatSkeleton = dtpg.getBaseSkeleton(shortPattern); // e.g., "ahm" or "Hm"
            String jPatSkeleton = dtpg.getBaseSkeleton(jPattern); // e.g., "ah" or "H"
            for (char timeCycleChar : timeCycleChars) {
                boolean has1 = jPatSkeleton.indexOf(timeCycleChar) >= 0;
                boolean has2 = shortPatSkeleton.indexOf(timeCycleChar) >= 0;
                if (has1 && !has2) {
                    String calType = USE_ICU_DATA ? dfmt.getCalendar().getType() : calendarType;
                    failedLocales.put(
                            loc,
                            calType
                                    + " expected "
                                    + timeCycleChar
                                    + " in both "
                                    + jPattern
                                    + " and "
                                    + shortPattern);
                }
            }
        }
        if (!worldFallback.isEmpty()) {
            logger.info(
                    "TestTimeCycle found "
                            + worldFallback.size()
                            + " world-fallback locales: "
                            + worldFallback);
        }
        if (!skippedLocales.isEmpty()) {
            logger.info(
                    "TestTimeCycle skipped "
                            + skippedLocales.size()
                            + " top-level locales without explicit timeFormatLength: "
                            + skippedLocales);
        }
        if (!failedLocales.isEmpty()) {
            Set<String> failedLocaleNames = new TreeSet<>();
            for (CLDRLocale loc : failedLocales.keySet()) {
                failedLocaleNames.add(loc.toString());
            }
            Set<String> intersection =
                    worldFallback.stream()
                            .filter(failedLocaleNames::contains)
                            .collect(Collectors.toSet());
            logger.info(
                    "TestTimeCycle intersection of failed and worldFallback "
                            + intersection.size()
                            + ": "
                            + intersection);
            for (Map.Entry<CLDRLocale, String> entry : failedLocales.entrySet()) {
                logger.severe(entry.getKey() + " " + entry.getValue());
            }
            fail(
                    "TestTimeCycle failed "
                            + failedLocales.size()
                            + " locales: "
                            + failedLocales.keySet());
        }
    }

    private String getJPattern(
            Map<String, PreferredAndAllowedHour> timeData,
            String localeID,
            Map<String, String> likelySubtags,
            Set<String> worldFallback) {
        PreferredAndAllowedHour prefAndAllowedHr = timeData.get(localeID);
        if (prefAndAllowedHr == null) {
            LocaleIDParser lp = new LocaleIDParser();
            String region = lp.set(localeID).getRegion();
            if (region == null || region.isEmpty()) {
                String loc2 = likelySubtags.get(localeID);
                if (loc2 != null && !loc2.isEmpty()) {
                    region = lp.set(loc2).getRegion();
                }
            }
            prefAndAllowedHr = timeData.get(region);
            if (prefAndAllowedHr == null) {
                worldFallback.add(localeID);
                prefAndAllowedHr = timeData.get(StandardCodes.NO_COUNTRY /* 001, world */);
                if (prefAndAllowedHr == null) {
                    return null;
                }
            }
        }
        return prefAndAllowedHr.preferred.base.name();
    }
}
