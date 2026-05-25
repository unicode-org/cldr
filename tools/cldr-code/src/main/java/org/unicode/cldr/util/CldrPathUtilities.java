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

    public static String intervalFormat(String calendar, String id, String subId) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\""
                + id
                + "\"]/greatestDifference[@id=\""
                + subId
                + "\"]";
    }

    /**
     * Return a pattern path; if the id is like date-width, it is a stock pattern. Otherwise it is
     * an available ID.
     */
    public static String dateTypePattern(String calendar, String id) {
        int stock = id.indexOf('-');
        if (stock < 0) {
            return availableFormat(calendar, id);
        } else {
            return stockDatetime(calendar, id.substring(0, stock), id.substring(stock + 1));
        }
    }

    public enum IntervalSeparatorType {
        numeric("MMMd", "d"),
        non_numeric("yMMM", "M"),
        mixed("yMMMd", "M"),
        fallback("yMMM", "y");
        public final String id;
        public final String subId;

        IntervalSeparatorType(String id, String subId) {
            this.id = id;
            this.subId = subId;
        }

        public static IntervalSeparatorType from(String source) {
            return source.equals("non-numeric") ? non_numeric : valueOf(source);
        }

        @Override
        public String toString() {
            return this == non_numeric ? "non-numeric" : name();
        }
    }

    public static String intervalSeparator(String calendar, IntervalSeparatorType separatorType) {
        return separatorType == IntervalSeparatorType.fallback
                ? intervalFormatFallback(calendar)
                : intervalSeparator(calendar, separatorType.name());
    }

    public static String intervalSeparator(String calendar, String separatorType) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dateTimeFormats/intervalFormats/intervalFormatRanges/intervalFormatRange[@type=\""
                + separatorType
                + "\"]";
    }

    public static String intervalFormatFallback(String calendar) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dateTimeFormats/intervalFormats/intervalFormatFallback";
    }
}
