package org.unicode.cldr.util;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

public class CldrPathUtilities {

    public static String dayOfMonthPath(
            Count ordinal, String calendar, String context, String width) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dayOfMonths/dayOfMonthContext[@type=\""
                + context
                + "\"]/dayOfMonthWidth[@type=\""
                + width
                + "\"]/dayOfMonth[@ordinal=\""
                + ordinal
                + "\"]";
    }
}
