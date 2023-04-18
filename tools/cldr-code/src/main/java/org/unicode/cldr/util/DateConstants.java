package org.unicode.cldr.util;

import java.util.Date;

/** Some date related constants */
public class DateConstants {
    /** Right now. A single constant so that it is consistent across callers. */
    public static final Date NOW = new Date();

    public static final long MILLIS_PER_SECOND = 60 * 1000;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    public static final long MILLIS_PER_MONTH = MILLIS_PER_DAY * 30;

    /** A date a little less than 8 months ago. */
    public static final Date RECENT_HISTORY = new Date(NOW.getTime() - (MILLIS_PER_MONTH * 8));
}
