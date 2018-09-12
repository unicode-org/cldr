package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.Output;

public class DayPeriodInfo {
    public static final int HOUR = 60 * 60 * 1000;
    public static final int MIDNIGHT = 0;
    public static final int NOON = 12 * HOUR;
    public static final int DAY_LIMIT = 24 * HOUR;

    public enum Type {
        format("format"), selection("stand-alone");
        public final String pathValue;

        private Type(String _pathValue) {
            pathValue = _pathValue;
        }

        public static Type fromString(String source) {
            return selection.pathValue.equals(source) ? selection : Type.valueOf(source);
        }
    }

    public static class Span implements Comparable<Span> {
        public final int start;
        public final int end;
        public final boolean includesEnd;
        public final DayPeriod dayPeriod;

        public Span(int start, int end, DayPeriod dayPeriod) {
            this.start = start;
            this.end = end;
            this.includesEnd = start == end;
            this.dayPeriod = dayPeriod;
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

        public boolean contains(int millisInDay) {
            return start <= millisInDay && (millisInDay < end || millisInDay == end && includesEnd);
        }

        /**
         * Returns end, but if not includesEnd, adjusted down by one.
         * @return
         */
        public int getAdjustedEnd() {
            return includesEnd ? end : end - 1;
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

        @Override
        public String toString() {
            return dayPeriod + ":" + toStringPlain();
        }

        public String toStringPlain() {
            return formatTime(start) + " – " + formatTime(end) + (includesEnd ? "" : "⁻");
        }
    }

    public enum DayPeriod {
        // fixed
        midnight(MIDNIGHT, MIDNIGHT), am(MIDNIGHT, NOON), noon(NOON, NOON), pm(NOON, DAY_LIMIT),
        // flexible
        morning1, morning2, afternoon1, afternoon2, evening1, evening2, night1, night2;

        public final Span span;

        private DayPeriod(int start, int end) {
            span = new Span(start, end, this);
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

    // the arrays must be in sorted order. First must have start= zero. Last must have end = DAY_LIMIT (and !includesEnd)
    // each of these will have the same length, and correspond.
    final private Span[] spans;
    final private DayPeriodInfo.DayPeriod[] dayPeriods;
    final Relation<DayPeriod, Span> dayPeriodsToSpans = Relation.of(new EnumMap<DayPeriod, Set<Span>>(DayPeriod.class), LinkedHashSet.class);

    public static class Builder {
        TreeSet<Span> info = new TreeSet<>();

        // TODO add rule test that they can't span same 12 hour time.

        public DayPeriodInfo.Builder add(DayPeriodInfo.DayPeriod dayPeriod, int start, boolean includesStart, int end,
            boolean includesEnd) {
            if (dayPeriod == null || start < 0 || start > end || end > DAY_LIMIT
                || end - start > NOON) { // the span can't exceed 12 hours
                throw new IllegalArgumentException("Bad data");
            }
            Span span = new Span(start, end, dayPeriod);
            boolean didntContain = info.add(span);
            if (!didntContain) {
                throw new IllegalArgumentException("Duplicate data: " + span);
            }
            return this;
        }

        public DayPeriodInfo finish(String[] locales) {
            DayPeriodInfo result = new DayPeriodInfo(info, locales);
            info.clear();
            return result;
        }

        public boolean contains(DayPeriod dayPeriod) {
            for (Span span : info) {
                if (span.dayPeriod == dayPeriod) {
                    return true;
                }
            }
            return false;
        }
    }

    private DayPeriodInfo(TreeSet<Span> info, String[] locales) {
        int len = info.size();
        spans = info.toArray(new Span[len]);
        List<DayPeriod> tempPeriods = new ArrayList<>();
        // check data
        if (len != 0) {
            Span last = spans[0];
            tempPeriods.add(last.dayPeriod);
            dayPeriodsToSpans.put(last.dayPeriod, last);
            if (last.start != MIDNIGHT) {
                throw new IllegalArgumentException("Doesn't start at 0:00).");
            }
            for (int i = 1; i < len; ++i) {
                Span current = spans[i];
                if (current.start != current.end && last.start != last.end) {
                    if (current.start != last.end) {
                        throw new IllegalArgumentException("Gap or overlapping times:\t"
                            + current + "\t" + last + "\t" + Arrays.asList(locales));
                    }
                }
                tempPeriods.add(current.dayPeriod);
                dayPeriodsToSpans.put(current.dayPeriod, current);
                last = current;
            }
            if (last.end != DAY_LIMIT) {
                throw new IllegalArgumentException("Doesn't reach 24:00).");
            }
        }
        dayPeriods = tempPeriods.toArray(new DayPeriod[len]);
        dayPeriodsToSpans.freeze();
        // add an extra check to make sure that periods are unique over 12 hour spans
        for (Entry<DayPeriod, Set<Span>> entry : dayPeriodsToSpans.keyValuesSet()) {
            DayPeriod dayPeriod = entry.getKey();
            Set<Span> spanSet = entry.getValue();
            if (spanSet.size() > 0) {
                for (Span span : spanSet) {
                    int start = span.start % NOON;
                    int end = span.getAdjustedEnd() % NOON;
                    for (Span span2 : spanSet) {
                        if (span2 == span) {
                            continue;
                        }
                        // if there is overlap when mapped to 12 hours...
                        int start2 = span2.start % NOON;
                        int end2 = span2.getAdjustedEnd() % NOON;
                        // disjoint if e1 < s2 || e2 < s1
                        if (start >= end2 && start2 >= end) {
                            throw new IllegalArgumentException("Overlapping times for " + dayPeriod + ":\t"
                                + span + "\t" + span2 + "\t" + Arrays.asList(locales));
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the start (in millis) of the first matching day period, or -1 if no match,
     *
     * @param dayPeriod
     * @return seconds in day
     */
    public int getFirstStartTime(DayPeriodInfo.DayPeriod dayPeriod) {
        for (int i = 0; i < spans.length; ++i) {
            if (spans[i].dayPeriod == dayPeriod) {
                return spans[i].start;
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
        Span span = getFirstDayPeriodSpan(dayPeriod);
        return Row.of(span.start, span.end, true);
    }

    public Span getFirstDayPeriodSpan(DayPeriodInfo.DayPeriod dayPeriod) {
        switch (dayPeriod) {
        case am:
            return DayPeriod.am.span;
        case pm:
            return DayPeriod.pm.span;
        default:
            Set<Span> spanList = dayPeriodsToSpans.get(dayPeriod);
            return spanList == null ? null : dayPeriodsToSpans.get(dayPeriod).iterator().next();
        }
    }

    public Set<Span> getDayPeriodSpans(DayPeriodInfo.DayPeriod dayPeriod) {
        switch (dayPeriod) {
        case am:
            return Collections.singleton(DayPeriod.am.span);
        case pm:
            return Collections.singleton(DayPeriod.pm.span);
        default:
            return dayPeriodsToSpans.get(dayPeriod);
        }
    }

    /**
     * Returns the day period for the time.
     *
     * @param millisInDay
     *            If not (millisInDay > 0 && The millisInDay < DAY_LIMIT) throws exception.
     * @return corresponding day period
     */
    public DayPeriodInfo.DayPeriod getDayPeriod(int millisInDay) {
        if (millisInDay < MIDNIGHT) {
            throw new IllegalArgumentException("millisInDay too small");
        } else if (millisInDay >= DAY_LIMIT) {
            throw new IllegalArgumentException("millisInDay too big");
        }
        for (int i = 0; i < spans.length; ++i) {
            if (spans[i].contains(millisInDay)) {
                return spans[i].dayPeriod;
            }
        }
        throw new IllegalArgumentException("internal error");
    }

    /**
     * Returns the number of periods in the day
     *
     * @return
     */
    public int getPeriodCount() {
        return spans.length;
    }

    /**
     * For the nth period in the day, returns the start, whether the start is included, and the period ID.
     *
     * @param index
     * @return data
     */
    public Row.R3<Integer, Boolean, DayPeriod> getPeriod(int index) {
        return Row.of(getSpan(index).start, true, getSpan(index).dayPeriod);
    }

    public Span getSpan(int index) {
        return spans[index];
    }

    public List<DayPeriod> getPeriods() {
        return Arrays.asList(dayPeriods);
    }

    @Override
    public String toString() {
        return dayPeriodsToSpans.values().toString();
    }

    public String toString(DayPeriod dayPeriod) {
        switch (dayPeriod) {
        case midnight:
            return "00:00";
        case noon:
            return "12:00";
        case am:
            return "00:00 – 12:00⁻";
        case pm:
            return "12:00 – 24:00⁻";
        default:
            break;
        }
        StringBuilder result = new StringBuilder();
        for (Span span : dayPeriodsToSpans.get(dayPeriod)) {
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(span.toStringPlain());
        }
        return result.toString();
    }

    public static String formatTime(int time) {
        int minutes = time / (60 * 1000);
        int hours = minutes / 60;
        minutes -= hours * 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    // Day periods that are allowed to collide
    private static final EnumMap<DayPeriod, EnumSet<DayPeriod>> allowableCollisions = new EnumMap<DayPeriod, EnumSet<DayPeriod>>(DayPeriod.class);
    static {
        allowableCollisions.put(DayPeriod.am, EnumSet.of(DayPeriod.morning1, DayPeriod.morning2));
        allowableCollisions.put(DayPeriod.pm, EnumSet.of(DayPeriod.afternoon1, DayPeriod.afternoon2, DayPeriod.evening1, DayPeriod.evening2));
    }

    /**
     * Test if there is a problem with dayPeriod1 and dayPeriod2 having the same localization.
     * @param type1
     * @param dayPeriod1
     * @param type2 TODO
     * @param dayPeriod2
     * @param sampleError TODO
     * @return
     */
    public boolean collisionIsError(DayPeriodInfo.Type type1, DayPeriod dayPeriod1, Type type2, DayPeriod dayPeriod2,
        Output<Integer> sampleError) {
        if (dayPeriod1 == dayPeriod2) {
            return false;
        }
        if ((allowableCollisions.containsKey(dayPeriod1) && allowableCollisions.get(dayPeriod1).contains(dayPeriod2)) ||
            (allowableCollisions.containsKey(dayPeriod2) && allowableCollisions.get(dayPeriod2).contains(dayPeriod1))) {
            return false;
        }

        // we use the more lenient if they are mixed types
        if (type2 == Type.format) {
            type1 = Type.format;
        }

        // At this point, they are unequal
        // The fixed cannot overlap among themselves
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
                if (collisionIsErrorFormat(flexible, fixedOrFlexible.span, sampleError)) {
                    return true;
                }
            } else { // flexible
                for (Span s : dayPeriodsToSpans.get(fixedOrFlexible)) {
                    if (collisionIsErrorFormat(flexible, s, sampleError)) {
                        return true;
                    }
                }
            }
            break;
        }
        case selection: {
            if (fixedOrFlexible.span != null) {
                if (collisionIsErrorSelection(flexible, fixedOrFlexible.span, sampleError)) {
                    return true;
                }
            } else { // flexible
                for (Span s : dayPeriodsToSpans.get(fixedOrFlexible)) {
                    if (collisionIsErrorSelection(flexible, s, sampleError)) {
                        return true;
                    }
                }
            }
            break;
        }
        }
        return false; // no bad collision
    }

    // Formatting has looser collision rules, because it is always paired with a time.
    // That is, it is not a problem if two items collide,
    // if it doesn't cause a collision when paired with a time.
    // But if 11:00 has the same format (eg 11 X) as 23:00, there IS a collision.
    // So we see if there is an overlap mod 12.
    private boolean collisionIsErrorFormat(DayPeriod dayPeriod, Span other, Output<Integer> sampleError) {
        int otherStart = other.start % NOON;
        int otherEnd = other.getAdjustedEnd() % NOON;
        for (Span s : dayPeriodsToSpans.get(dayPeriod)) {
            int flexStart = s.start % NOON;
            int flexEnd = s.getAdjustedEnd() % NOON;
            if (otherStart <= flexEnd && otherEnd >= flexStart) { // overlap?
                if (sampleError != null) {
                    sampleError.value = Math.max(otherStart, otherEnd);
                }
                return true;
            }
        }
        return false;
    }

    // Selection has stricter collision rules, because is is used to select different messages.
    // So two types with the same localization do collide unless they have exactly the same rules.
    private boolean collisionIsErrorSelection(DayPeriod dayPeriod, Span other, Output<Integer> sampleError) {
        int otherStart = other.start;
        int otherEnd = other.getAdjustedEnd();
        for (Span s : dayPeriodsToSpans.get(dayPeriod)) {
            int flexStart = s.start;
            int flexEnd = s.getAdjustedEnd();
            if (otherStart != flexStart) { // not same??
                if (sampleError != null) {
                    sampleError.value = (otherStart + flexStart) / 2; // half-way between
                }
                return true;
            } else if (otherEnd != flexEnd) { // not same??
                if (sampleError != null) {
                    sampleError.value = (otherEnd + flexEnd) / 2; // half-way between
                }
                return true;
            }
        }
        return false;
    }
}