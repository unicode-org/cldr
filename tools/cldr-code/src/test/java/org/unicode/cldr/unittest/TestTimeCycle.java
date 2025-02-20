package org.unicode.cldr.unittest;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.FlexibleDateFromCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;

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

    private final String calendarType = LDMLConstants.GENERIC; /* or LDMLConstants.GREGORIAN? */

    /**
     * Test that a locale's standard time format matches the region's preferred time cycle.
     *
     * <p>In other words, test that in all available locales, the actual short time pattern uses the
     * same cycle as produced by 'j'
     *
     * <p>This is similar to icu4j DateTimeGeneratorTest.testJjMapping. It uses current CLDR data,
     * not ICU data.
     */
    @Test
    public void testJjMapping() {
        CLDRConfig testInfo = CLDRConfig.getInstance();
        Factory cldrFactory = testInfo.getCldrFactory();
        for (String localeID : cldrFactory.getAvailable()) {
            CLDRLocale loc = CLDRLocale.getInstance(localeID);
            ICUServiceBuilder icuServiceBuilder = ICUServiceBuilder.forLocale(loc);
            ULocale uloc = new ULocale(localeID);
            DateTimePatternGenerator dtpg;
            if (USE_ICU_DATA) {
                dtpg = DateTimePatternGenerator.getInstance(uloc);
            } else {
                CLDRFile cldrFile = cldrFactory.make(localeID, true);
                List<CLDRFile> parentCLDRFiles = new ArrayList<>();
                parentCLDRFiles.add(cldrFile);
                // If call DateTimePatternGenerator.getEmptyInstance, jPattern always gets "H", not
                // localized :-(
                // dtpg = DateTimePatternGenerator.getEmptyInstance();
                // If call getDTPGForCalendarType, jPattern mostly gets "HH" or "H", sometimes "H时"
                // or other localized values, but usually not the same as for USE_ICU_DATA
                dtpg =
                        new FlexibleDateFromCLDR()
                                .getDTPGForCalendarType(calendarType, parentCLDRFiles);
            }

            String jPattern = dtpg.getBestPattern(jSkeleton); // e.g., "h a" or "HH" or "H时"
            DateFormat dfmt;
            if (USE_ICU_DATA) {
                dfmt = DateFormat.getTimeInstance(DateFormat.SHORT, uloc);
            } else {
                // Problem: how to specify the equivalent of DateFormat.SHORT?
                // "...timeFormatLength[@type=\"short\"]..."?
                dfmt = icuServiceBuilder.getDateFormat(calendarType, jPattern);
            }
            String shortPattern =
                    ((SimpleDateFormat) dfmt).toPattern(); // e.g., "h:mm a" or "HH:mm"

            // Check that shortPattern and jPattern use the same hour cycle
            String jPatSkeleton = dtpg.getSkeleton(jPattern); // e.g., "ah" or "HH" or "H"
            String shortPatSkeleton = dtpg.getSkeleton(shortPattern); // e.g., "ahmm" or "HHmm"
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
                    logger.severe(
                            "locale "
                                    + localeID
                                    + ", expected j resolved char "
                                    + timeCycleChar
                                    + " to occur in short time pattern "
                                    + shortPattern
                                    + " for "
                                    + dfmtCalType);
                }
            }
        }
    }
}
