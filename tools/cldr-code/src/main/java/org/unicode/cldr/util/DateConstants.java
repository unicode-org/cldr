package org.unicode.cldr.util;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import java.util.Date;
import java.util.Locale;

/** Some date related constants */
public class DateConstants {
    /** Right now. A single constant so that it is consistent across callers. */
    public static final Date NOW = new Date();

    public static final long MILLIS_PER_SECOND = 60 * 1000;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    public static final long MILLIS_PER_MONTH = MILLIS_PER_DAY * 30;

    private static final Date getRecentHistory(Date d) {
        Calendar gc = GregorianCalendar.getInstance(TimeZone.GMT_ZONE, Locale.ENGLISH);
        gc.clear();
        gc.setTime(d);
        gc.set(Calendar.MONTH, -8); // 8 months prior
        return gc.getTime();
    }

    /** A date a little less than 8 months ago. */
    public static final Date RECENT_HISTORY = getRecentHistory(NOW);
}
