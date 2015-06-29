/**
 * 
 */
package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;

public class DayPeriodInfo {
    public static final int HOUR = 60 * 60 * 1000;
    public static final int MIDNIGHT = 0;
    public static final int NOON = 12 * HOUR;
    public static final int DAY_LIMIT = 24 * HOUR;

    public enum Type {
        format("format"), 
        selection("stand-alone");
        public final String pathValue;
        private Type(String _pathValue) {
            pathValue = _pathValue;
        }
        public static Type fromString(String source) {
            return source.equals(selection.pathValue) ? selection : Type.valueOf(source);
        }
    }

    public static class Span implements Comparable<Span> {
        public final int start;
        public final int end;
        public final boolean includesEnd;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
            this.includesEnd = start == end;
        }
        @Override
        public int compareTo(Span o) {
            int diff = start - o.start;
            if (diff != 0) {
                return diff;
            }
            diff = end - o.end;
            if (diff != 0) {
                return diff;
            }
            // because includesEnd is determined by the above, we're done
            return 0;
        }
        @Override
        public boolean equals(Object obj) {
            Span other = (Span) obj;
            return start == other.start && end == other.end;
            // because includesEnd is determined by the above, we're done
        }
        @Override
        public int hashCode() {
            return start * 37 + end;
        }
    }

    public enum DayPeriod {
        midnight(MIDNIGHT, MIDNIGHT), am(MIDNIGHT, NOON), noon(NOON, NOON), pm(NOON, DAY_LIMIT),
        morning1, morning2, afternoon1, afternoon2, evening1, evening2, night1, night2;

        public final Span span;

        private DayPeriod(int start, int end) {
            span = new Span(start,end);
        }

        private DayPeriod() {
            span = null;
        }

        public static DayPeriod fromString(String value) {
            return valueOf(value);
        }

        public boolean isFixed() {
            return span != null;
        }
    };

    // the starts must be in sorted order. First must be zero. Last must be < DAY_LIMIT
    // each of these will have the same length, and correspond.
    private int[] starts;
    private boolean[] includesStart;
    private DayPeriodInfo.DayPeriod[] periods;

    public static class Builder {
        TreeMap<Row.R2<Integer, Integer>, Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>> info = new TreeMap<Row.R2<Integer, Integer>, Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>>();
        // TODO add rule test that they can't span same 12 hour time.

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
     * Return the start, end, and whether the start is included.
     * 
     * @param dayPeriod
     * @return start,end,includesStart,period
     */
    public R3<Integer, Integer, Boolean> getFirstDayPeriodInfo(DayPeriodInfo.DayPeriod dayPeriod) {
        switch (dayPeriod) {
        case am: return Row.of(0, DAY_LIMIT/2, true);
        case pm: return Row.of(DAY_LIMIT/2, DAY_LIMIT, true);
        default:
            break;
        }
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
        switch (dayPeriod) {
        case midnight: return "00:00";
        case noon: return "12:00";
        case am: return "00:00 – 12:00";
        case pm: return "12:00 – 24:00";
        }
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

    /**
     * Test if there is a problem with dayPeriod1 and dayPeriod2 having the same localization.
     * @param type1
     * @param dayPeriod1
     * @param type2 TODO
     * @param dayPeriod2
     * @return
     */
    public boolean collisionIsError(DayPeriodInfo.Type type1, DayPeriod dayPeriod1, Type type2, DayPeriod dayPeriod2) {
        if (dayPeriod1 == dayPeriod2) {
            return false;
        }
        // we use the more lenient if they are mixed types
        if (type2 == Type.format) {
            type1 = Type.format;
        }

        // At this point, they are unequal
        // The fixed cannot overlap among themselves, and for regularity, we disallow the flexible to overlap.
        final boolean fixed1 = dayPeriod1.isFixed();
        final boolean fixed2 = dayPeriod2.isFixed();
        if (fixed1 && fixed2) {
            return true;
        }
        // at this point, at least one is flexible.
        // make sure the second is not flexible.
        DayPeriod fixedOrFlexible;
        DayPeriod flexible;
        if (fixed1) {
            fixedOrFlexible = dayPeriod1;
            flexible = dayPeriod2;
        } else {
            fixedOrFlexible = dayPeriod2;
            flexible = dayPeriod1;
        }

        // TODO since periods are sorted, could optimize further

        switch (type1) {
        case format: {
            if (fixedOrFlexible.span != null) {
                if (collisionIsErrorFormat(flexible, fixedOrFlexible.span.start, fixedOrFlexible.span.end, fixedOrFlexible.span.includesEnd)) {
                    return true;
                }
            } else { // flexible
                for (int i = 1; i < starts.length; ++i) {
                    if (periods[i-1] != fixedOrFlexible) {
                        continue;
                    }
                    if (collisionIsErrorFormat(flexible, starts[i-1], starts[i] - 1, false)) { // we know flexibles are always !includesEnd
                        return true;
                    }
                }
            }
            break;
        }
        case selection: {
            if (fixedOrFlexible.span != null) {
                if (collisionIsErrorSelection(flexible, fixedOrFlexible.span.start, fixedOrFlexible.span.end, fixedOrFlexible.span.includesEnd)) {
                    return true;
                }
            } else { // flexible
                for (int i = 1; i < starts.length; ++i) {
                    if (periods[i-1] != fixedOrFlexible) {
                        continue;
                    }
                    if (collisionIsErrorSelection(flexible, starts[i-1], starts[i] - 1, false)) { // we know flexibles are always !includesEnd
                        return true;
                    }
                }
            }
            break;
        }}
        return false; // no bad collision
    }

    // Formatting has looser collision rules, because it is always paired with a time. 
    // That is, it is not a problem if two items collide,
    // if it doesn't cause a collision when paired with a time. 
    // But if 11:00 has the same format (eg 11 X) as 23:00, there IS a collision.
    // So we see if there is an overlap mod 12.
    private boolean collisionIsErrorFormat(DayPeriod dayPeriod, int fixedStart, int fixedEnd, boolean includesEnd) {
        fixedStart = fixedStart % NOON;
        fixedEnd = (fixedEnd - (includesEnd ? 0 : 1)) % NOON;
        for (int i = 1; i < starts.length; ++i) {
            if (periods[i-1] != dayPeriod) {
                // TODO since periods are sorted, could optimize further
                continue;
            }
            int flexStart = starts[i-1] % NOON;
            int flexEnd = (starts[i] - 1) % NOON; // we know flexibles are always !includesEnd
            if (fixedStart <= flexEnd && fixedEnd >= flexStart) { // overlap?
                return true;
            }
        }
        return false;
    }
    
    // Selection has stricter collision rules, because is is used to select different messages. 
    // So two types with the same localization do collide unless they have exactly the same rules.
    private boolean collisionIsErrorSelection(DayPeriod dayPeriod, int fixedStart, int fixedEnd, boolean includesEnd) {
        fixedEnd = (fixedEnd - (includesEnd ? 0 : 1));
        for (int i = 1; i < starts.length; ++i) {
            if (periods[i-1] != dayPeriod) {
                // TODO since periods are sorted, could optimize further
                continue;
            }
            int flexStart = starts[i-1];
            int flexEnd = (starts[i] - 1); // we know flexibles are always !includesEnd
            if (fixedStart != flexStart || fixedEnd != flexEnd) { // not same??
                return true;
            }
        }
        return false;
    }
}