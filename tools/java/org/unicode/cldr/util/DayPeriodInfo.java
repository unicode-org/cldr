/**
 * 
 */
package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;

public class DayPeriodInfo {
    public static final int HOUR = 60 * 60 * 1000;
    public static final int DAY_LIMIT = 24 * HOUR;

    public enum Type {
        format, selection
    }

    public enum DayPeriod {
        midnight, am, noon, pm,
        morning1, morning2, afternoon1, afternoon2, evening1, evening2, night1, night2;

//        public static final DayPeriod am = morning1;
//        public static final DayPeriod pm = afternoon1;

        public static DayPeriod fromString(String value) {

            return valueOf(value);
//            return 
//                value.equals("am") ? morning1 
//                : value.equals("pm") ? afternoon1
//                    : 
//                        DayPeriod.valueOf(value.toLowerCase(Locale.ENGLISH));
        }
    };

    // the starts must be in sorted order. First must be zero. Last must be < DAY_LIMIT
    // each of these will have the same length, and correspond.
    private int[] starts;
    private boolean[] includesStart;
    private DayPeriodInfo.DayPeriod[] periods;

    public static class Builder {
        TreeMap<Row.R2<Integer, Integer>, Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>> info = new TreeMap<Row.R2<Integer, Integer>, Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>>();

        public DayPeriodInfo.Builder add(DayPeriodInfo.DayPeriod dayPeriod, int start, boolean includesStart, int end,
            boolean includesEnd) {
            if (dayPeriod == null || start < 0 || start >= DAY_LIMIT) {
                throw new IllegalArgumentException();
            }
            R2<Integer, Integer> key = Row.of(start, includesStart ? 0 : 1);
            if (info.containsKey(key)) {
                throw new IllegalArgumentException("Overlapping Times");
            }
            info.put(key, Row.of(end, includesEnd, dayPeriod));
            return this;
        }

        public DayPeriodInfo finish(String[] locales) {
            DayPeriodInfo result = new DayPeriodInfo();
            int len = info.size();
//            if (len == 0) {
//                return result;
//            }
            result.starts = new int[len];
            result.includesStart = new boolean[len];
            result.periods = new DayPeriodInfo.DayPeriod[len];
            int i = 0;
            int lastFinish = 0;
            boolean lastFinishIncluded = false;
            for (Row.R2<Integer, Integer> start : info.keySet()) {
                result.starts[i] = start.get0();
                result.includesStart[i] = start.get1() == 0;
                if (lastFinish != result.starts[i] || lastFinishIncluded == result.includesStart[i]) {
                    throw new IllegalArgumentException("Gap or overlapping times: "
                        + formatTime(start.get0()) + "\t..\t" + formatTime(start.get1()) + "\t"
                        + formatTime(lastFinish) + "\t" + lastFinishIncluded
                        + "\t" + Arrays.asList(locales));
                }
                Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod> row = info.get(start);
                lastFinish = row.get0();
                lastFinishIncluded = row.get1();
                result.periods[i++] = row.get2();
            }
            if (len != 0) {
                if (result.starts[0] != 0 || result.includesStart[0] != true || lastFinish != DAY_LIMIT
                    || lastFinishIncluded != false) {
                    throw new IllegalArgumentException("Doesn't cover 0:00).");
                }
            }
            info.clear();
            return result;
        }
    }

    /**
     * Return the start (in millis) of the first matching day period, or -1 if no match,
     * 
     * @param dayPeriod
     * @return seconds in day
     */
    public int getFirstStartTime(DayPeriodInfo.DayPeriod dayPeriod) {
        for (int i = 0; i < periods.length; ++i) {
            if (periods[i] == dayPeriod) {
                return starts[i];
            }
        }
        return -1;
    }

    /**
     * Return the start (in millis) of the first matching day period, or -1 if no match,
     * 
     * @param dayPeriod
     * @return start,end,includesStart,period
     */
    public R3<Integer, Integer, Boolean> getFirstDayPeriodInfo(DayPeriodInfo.DayPeriod dayPeriod) {
        for (int i = 0; i < periods.length; ++i) {
            if (periods[i] == dayPeriod) {
                return Row.of(starts[i], i+1 < periods.length ? starts[i+1] : DAY_LIMIT, includesStart[i]);
            }
        }
        return null;
    }

    /**
     * Returns the day period for the time.
     * 
     * @param millisInDay
     *            If not (millisInDay > 0 && The millisInDay < DAY_LIMIT) throws exception.
     * @return corresponding day period
     */
    public DayPeriodInfo.DayPeriod getDayPeriod(int millisInDay) {
        if (millisInDay < 0) {
            throw new IllegalArgumentException("millisInDay too small");
        } else if (millisInDay > 24 * 60 * 60 * 1000) {
            throw new IllegalArgumentException("millisInDay too big");
        }
        for (int i = 0; i < starts.length; ++i) {
            int start = starts[i];
            if (start == millisInDay && includesStart[i]) {
                return periods[i];
            }
            if (start > millisInDay) {
                return periods[i - 1];
            }
        }
        return periods[periods.length - 1];
    }

    /**
     * Returns the number of periods in the day
     * 
     * @return
     */
    public int getPeriodCount() {
        return starts.length;
    }

    /**
     * For the nth period in the day, returns the start, whether the start is included, and the period ID.
     * 
     * @param index
     * @return data
     */
    public Row.R3<Integer, Boolean, DayPeriod> getPeriod(int index) {
        return Row.of(starts[index], includesStart[index], periods[index]);
    }

    public List<DayPeriod> getPeriods() {
        return Arrays.asList(periods);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < starts.length; ++i) {
            R3<Integer, Boolean, DayPeriod> period = getPeriod(i);
            Boolean included = period.get1();
            int time = period.get0();

            if (i != 0) {
                result.append('\n').append(included ? " < " : " \u2264 ");
            }
            result.append(formatTime(time))
            .append(!included ? " < " : " \u2264 ")
            .append(period.get2());
        }
        result.append("\n< 24:00");
        return result.toString();
    }

    public String toString(DayPeriod dayPeriod) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < starts.length; ++i) {
            R3<Integer, Boolean, DayPeriod> periodInfo = getPeriod(i);
            DayPeriod period = periodInfo.get2();
            if (period != dayPeriod) {
                continue;
            }
            Integer time = periodInfo.get0();
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(formatTime(time)).append(" – ");
            if (i+1 < starts.length) {
                periodInfo = getPeriod(i+1);
                time = periodInfo.get0();
                result.append(formatTime(time));
            } else {
                result.append("24:00");
            }
        }
        return result.toString();
    }

    static String formatTime(int time) {
        int minutes = time / (60 * 1000);
        int hours = minutes / 60;
        minutes -= hours * 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}