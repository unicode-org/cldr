package org.unicode.cldr.unittest;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;

public class TestTimeCycle {
    private static final Logger logger = Logger.getLogger(TestTimeCycle.class.getName());

    /**
     * Test that a locale's standard time format matches the region's preferred time cycle
     *
     * <p>This is similar to icu4j DateTimeGeneratorTest.testJjMapping. It uses current CLDR data,
     * not ICU data.
     */
    @Test
    public void testJjMapping() {
        final String jSkeleton = "j";
        final char[] timeCycleChars = {'H', 'h', 'K', 'k'};
        // First test that j maps correctly by region in a locale for which we do not have data.
        {
            String testLocaleID = "de_US"; // short patterns from fallback locale "de" have "HH"
            ULocale testLocale = new ULocale(testLocaleID);
            DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(testLocale);
            String jPattern = dtpg.getBestPattern(jSkeleton);
            String jPatSkeleton = dtpg.getSkeleton(jPattern);
            if (jPatSkeleton.indexOf('h')
                    < 0) { // expect US preferred cycle 'h', not H or other cycle
                logger.severe(
                        "DateTimePatternGenerator getBestPattern locale "
                                + testLocaleID
                                + ", pattern j did not use 'h'");
            }
        }

        // Next test that in all available Locales, the actual short time pattern uses the same
        // cycle as produced by 'j'

        CLDRConfig testInfo = CLDRConfig.getInstance();
        Factory cldrFactory = testInfo.getCldrFactory();
        for (String localeID : cldrFactory.getAvailable()) {
            CLDRLocale loc = CLDRLocale.getInstance(localeID);
            ICUServiceBuilder icuServiceBuilder = ICUServiceBuilder.forLocale(loc);

            // Compare ICU data version:
            // DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(uloc);

            DateTimePatternGenerator dtpg = DateTimePatternGenerator.getEmptyInstance();

            String jPattern = dtpg.getBestPattern(jSkeleton);

            // Compare ICU data version:
            // DateFormat dfmt = DateFormat.getTimeInstance(DateFormat.SHORT, uloc);

            SimpleDateFormat dfmt =
                    icuServiceBuilder.getDateFormat(LDMLConstants.GREGORIAN, jPattern);

            String shortPattern = dfmt.toPattern();

            // Now check that shortPattern and jPattern use the same hour cycle
            String jPatSkeleton = dtpg.getSkeleton(jPattern);
            String shortPatSkeleton = dtpg.getSkeleton(shortPattern);
            for (char timeCycleChar : timeCycleChars) {
                if (jPatSkeleton.indexOf(timeCycleChar) >= 0) {
                    if (shortPatSkeleton.indexOf(timeCycleChar) < 0) {
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
}
