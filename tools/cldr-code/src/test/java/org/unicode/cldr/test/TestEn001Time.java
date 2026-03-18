package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.*;

public class TestEn001Time {

    // TODO: for short timezone names, check that en_001 has “∅∅∅” iff en has a value, and
    // that en_001 otherwise has no short timezone names (except through inheritance).
    // Reference: https://unicode-org.atlassian.net/browse/CLDR-18909
    static final boolean CHECK_SHORT_TIMEZONE_NAMES = false;
    static final String TIME_FORMAT_PREFIX =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength";

    static final String TIMEZONE_NAME_PREFIX = "//ldml/dates/timeZoneNames";
    static final String TIMEZONE_NAME_SHORT = "short";

    /** en_001 should not have standard time formats */
    @Test
    void testTime() {
        Factory factory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
        CLDRFile cldrFile = factory.make("en_001", false);
        for (final String path : cldrFile.fullIterable()) {
            assertFalse(
                    path.startsWith(TIME_FORMAT_PREFIX),
                    "Path starts with " + TIME_FORMAT_PREFIX + ": " + path);
            if (CHECK_SHORT_TIMEZONE_NAMES) {
                assertFalse(
                        path.startsWith(TIMEZONE_NAME_PREFIX) && path.contains(TIMEZONE_NAME_SHORT),
                        "Path starts with "
                                + TIMEZONE_NAME_PREFIX
                                + " + and contains "
                                + TIMEZONE_NAME_SHORT
                                + ": "
                                + path);
            }
        }
    }
}
