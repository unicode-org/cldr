package org.unicode.cldr.util;

import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import java.util.Locale;

public class TimeDiff {
    public static String timeDiff(long a) {
        return timeDiff(a, System.currentTimeMillis());
    }

    public static String durationDiff(long a) {
        return timeDiff(System.currentTimeMillis() - a);
    }

    private static final long ONE_SECOND = 1 * 1000;
    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 3600 * 1000;
    private static final long ONE_DAY = 86400 * 1000;
    private static final double ONE_YEAR = ONE_DAY * 364.25;

    /**
     * @returns string representation of the difference between the two millisecond values
     */
    public static String timeDiff(long a, long b) {
        double del = (b - a);
        final RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(Locale.ENGLISH);

        if (del > ONE_YEAR) {
            del /= ONE_YEAR; // approximate
            int years = (int) del;
            return fmt.format(
                    years,
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.YEARS);
        } else if (del > (ONE_DAY)) {
            // more than a day: reference in days
            del /= ONE_DAY;
            int days = (int) del;
            return fmt.format(
                    days,
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.DAYS);
        } else if (del > ONE_HOUR) {
            final double hours = (b - a) / (ONE_HOUR);
            return fmt.format(
                    hours,
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.HOURS);
        } else if (del > ONE_MINUTE) {
            final double minutes = (b - a) / (ONE_MINUTE);
            return fmt.format(
                    minutes,
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.MINUTES);

        } else if (del > ONE_SECOND) {

            final double seconds = (b - a) / (ONE_SECOND);
            return fmt.format(
                    seconds,
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.SECONDS);

        } else {
            // fall back to milliseconds
            return MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NARROW)
                    .format(new Measure(del, MeasureUnit.MILLISECOND));
        }
    }
}
