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

    public static String stockDatetime(String calendar, String dateVsTime, String width) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/"
                + dateVsTime
                + "Formats/"
                + dateVsTime
                + "FormatLength[@type=\""
                + width
                + "\"]/"
                + dateVsTime
                + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";
    }

    public static String availableFormat(String calendar, String id) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                + id
                + "\"]";
    }

    /**
     * Return a pattern; if the id is like date-width, it is a stock pattern. Otherwise it is an
     * available ID.
     */
    public static String dateTypePattern(String calendar, String id) {
        int stock = id.indexOf('-');
        if (stock < 0) {
            return availableFormat(calendar, id);
        } else {
            return stockDatetime(calendar, id.substring(0, stock), id.substring(stock + 1));
        }
    }
}
