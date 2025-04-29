package org.unicode.cldr.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.unicode.cldr.icu.LDMLConstants;

public class SupplementalCalendarData implements Iterable<String> {
    /** an <era> element */
    public static class EraData implements Comparable<EraData> {
        static final int INDEX = 4; // index of our element

        /** the <era> xpath */
        private final String start;

        private final String end;
        private final String code;
        private final int type;
        private final String calendarType; // for comparison
        private final long startTime;
        private final long endTime;

        private final List<String> aliases;

        EraData(final XPathValue xpath) {
            calendarType = xpath.getAttributeValue(2, LDMLConstants.TYPE);
            type = Integer.parseInt(xpath.getAttributeValue(INDEX, LDMLConstants.TYPE));

            start = xpath.getAttributeValue(INDEX, LDMLConstants.START);
            final GregorianCalendar startCalendar = dateStringToCalendar(start);
            if (startCalendar != null) {
                startTime = startCalendar.getTimeInMillis();
            } else {
                startTime = Long.MIN_VALUE;
            }

            end = xpath.getAttributeValue(INDEX, LDMLConstants.END);
            final GregorianCalendar endCalendar = dateStringToCalendar(end);
            if (endCalendar != null) {
                endTime = endCalendar.getTimeInMillis();
            } else {
                endTime = Long.MAX_VALUE;
            }

            code = xpath.getAttributeValue(INDEX, LDMLConstants.CODE);

            final String aliasString = xpath.getAttributeValue(INDEX, LDMLConstants.ALIASES);
            if (aliasString == null) {
                aliases = ImmutableList.of();
            } else {
                aliases = LIST_SPLITTER.splitToList(aliasString);
            }
        }

        private static final Splitter LIST_SPLITTER =
                Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();

        public int getType() {
            return type;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public String getCode() {
            return code;
        }

        public List<String> getAliases() {
            return aliases;
        }

        @Override
        public String toString() {
            return String.format(
                    "ERA %d, [%s-%s] code=%s, aliases=%s",
                    getType(), getStart(), getEnd(), getCode(), getAliases());
        }

        private static final GregorianCalendar dateStringToCalendar(String ymd) {
            if (ymd == null) return null;
            int multiplier = 1;
            if (ymd.startsWith("-")) {
                multiplier = -1;
                ymd = ymd.substring(1);
            }
            final String[] parts = ymd.split("-");
            try {
                return new GregorianCalendar(
                        multiplier * Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("While parsing date string " + ymd, nfe);
            }
        }

        private GregorianCalendar getLatest() {
            GregorianCalendar l = getEndCalendar();
            if (l == null) l = getStartCalendar();
            return l;
        }

        /** only works within the same cal system */
        @Override
        public int compareTo(EraData o) {
            return ComparisonChain.start()
                    .compare(calendarType, o.calendarType)
                    .compare(getLatestTime(), o.getLatestTime())
                    .result();
        }
    }

    /** a <calendar type=> element */
    public static class CalendarData implements Iterable<Integer>, Freezable<CalendarData> {
        static final int INDEX = 3; // index of our element

        /** the <calendarSystem> xpath */
        private String system = null;

        /** the <inheritEras> xpath */
        private String inheritEras = null;

        private Map<Integer, EraData> eras = new HashMap<Integer, EraData>();

        public String getSystemType() {
            return system;
        }

        public String getInheritEras() {
            return inheritEras;
        }

        @Override
        public Iterator<Integer> iterator() {
            return eras.keySet().iterator();
        }

        public EraData get(Integer era) {
            return eras.get(era);
        }

        @Override
        public CalendarData cloneAsThawed() {
            throw new UnsupportedOperationException("Unimplemented 'cloneAsThawed'");
        }

        @Override
        public CalendarData freeze() {
            eras = ImmutableMap.copyOf(eras);
            return this;
        }

        @Override
        public boolean isFrozen() {
            return (eras instanceof ImmutableMap);
        }

        void setSystemXPath(XPathValue xpath) {
            if (isFrozen()) throw new UnsupportedOperationException("frozen");
            system = xpath.getAttributeValue(INDEX, LDMLConstants.TYPE);
        }

        void setInheritEras(XPathValue xpath) {
            if (isFrozen()) throw new UnsupportedOperationException("frozen");
            inheritEras = xpath.getAttributeValue(INDEX, LDMLConstants.CALENDAR);
        }
    }

    private Map<String, CalendarData> typeToCalendar = new HashMap<>();

    static class Parser implements Consumer<XPathValue>, Supplier<SupplementalCalendarData> {
        private Map<String, CalendarData> typeToCalendar = new HashMap<>();

        CalendarData getCalendar(final String type) {
            return typeToCalendar.computeIfAbsent(type, ignored -> new CalendarData());
        }

        @Override
        public void accept(final XPathValue x) {
            // assert that we are in //supplementalData/calendarData/calendar…
            if (!x.getElement(2).equals(LDMLConstants.CALENDAR)) {
                throw new UnsupportedOperationException("Unsupported supplemental xpath " + x);
            }
            if (x.size() < 4) return; // top level <calendarData> or <calendarData/calendar>
            final String calendarType = x.getAttributeValue(2, LDMLConstants.TYPE);
            if (calendarType == null || calendarType.isEmpty()) {
                throw new IllegalArgumentException("No calendar type on " + x);
            }
            final CalendarData c = getCalendar(calendarType);

            final String level3 = x.getElement(3);
            switch (level3) {
                case LDMLConstants.CALENDAR_SYSTEM:
                    acceptCalendarSystem(x, c);
                    break;
                case LDMLConstants.INHERIT_ERAS:
                    acceptInheritEras(x, c);
                    break;
                case LDMLConstants.ERAS:
                    acceptEras(x, c);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported supplemental xpath " + x + " got " + level3);
            }
        }

        private void acceptEras(XPathValue x, CalendarData c) {
            if (x.size() < 5) {
                return; // top level element
            } else if (x.size() > 5) {
                // something unexpectedly deep
                throw new IllegalArgumentException("Unsupported deep era xpath " + x);
            }
            final String e = x.getElement(4);
            if (!e.equals(LDMLConstants.ERA)) {
                throw new IllegalArgumentException("Unsupported era xpath " + x);
            }
            final String type = x.getAttributeValue(4, LDMLConstants.TYPE);
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Bad calendar era, no biscuit " + x);
            }
            final Integer n = Integer.parseInt(type);
            if (c.eras.putIfAbsent(n, new EraData(x)) != null) {
                throw new IllegalArgumentException("Duplicate calendar era " + x);
            }
        }

        private void acceptInheritEras(XPathValue x, CalendarData c) {
            if (c.inheritEras != null) {
                throw new IllegalArgumentException("Duplicate calendar inheritEras: " + x);
            }
            c.setInheritEras(x);
        }

        private void acceptCalendarSystem(XPathValue x, CalendarData c) {
            if (c.system != null) {
                throw new IllegalArgumentException("Duplicate calendar system: " + x);
            }
            c.setSystemXPath(x);
        }

        final Supplier<SupplementalCalendarData> supplier =
                Suppliers.memoize(() -> new SupplementalCalendarData(typeToCalendar));

        /** Calling get() freezes the data, so, only call get() once all data is loaded. */
        @Override
        public SupplementalCalendarData get() {
            return supplier.get();
        }
    }

    private SupplementalCalendarData(Map<String, CalendarData> m) {
        // freeze all types
        m.values().forEach(c -> c.freeze());
        this.typeToCalendar = ImmutableMap.copyOf(m);
    }

    /** Calendar data for this type */
    public CalendarData get(final String t) {
        return typeToCalendar.get(t);
    }

    /** iterate over types */
    @Override
    public Iterator<String> iterator() {
        return typeToCalendar.keySet().iterator();
    }
}
