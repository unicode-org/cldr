package org.unicode.cldr.unittest;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class TestTimeCycle {
    private static final Logger logger = Logger.getLogger(TestTimeCycle.class.getName());

    /**
     * Test that a locale's standard time format matches the region's preferred time cycle
     *
     * <p>This is essentially the same as icu4j DateTimeGeneratorTest.testJjMapping
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
        ULocale[] locales = DateFormat.getAvailableULocales();
        for (ULocale locale : locales) {
            String localeID = locale.getName();
            DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(locale);
            DateFormat dfmt = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
            String shortPattern = ((SimpleDateFormat) dfmt).toPattern();
            String jPattern = dtpg.getBestPattern(jSkeleton);
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
