package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.*;

public class TestEn001Time {

    static final String TIME_FORMAT_PREFIX =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength";
    static final String TIMEZONE_NAME_PREFIX = "//ldml/dates/timeZoneNames";
    static final String TIMEZONE_NAME_SHORT = "short";

    /** en_001 should not have standard time formats or any short timezone names */
    @Test
    void testTime() {
        Factory factory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
        CLDRFile cldrFile = factory.make("en_001", false);
        for (final String path : cldrFile.fullIterable()) {
            assertFalse(
                    path.startsWith(TIME_FORMAT_PREFIX),
                    "Path starts with " + TIME_FORMAT_PREFIX + ": " + path);
            assertFalse(
                    path.startsWith(TIMEZONE_NAME_PREFIX) && path.contains("short"),
                    "Path starts with "
                            + TIMEZONE_NAME_PREFIX
                            + " + and contains "
                            + TIMEZONE_NAME_SHORT
                            + ": "
                            + path);
        }
    }
}
