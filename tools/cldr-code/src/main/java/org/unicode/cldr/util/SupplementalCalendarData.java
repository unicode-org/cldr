package org.unicode.cldr.util;

import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.unicode.cldr.icu.LDMLConstants;

public class SupplementalCalendarData implements Iterable<String> {
    /** an <era> element */
    static class EraData {
        static final int INDEX = 4; // index of our element

        /** the <era> xpath */
        private final XPathValue xpath;

        EraData(final XPathValue xpath) {
            this.xpath = xpath;
            // TODO: validate (in unit test)
        }

        public int getType() {
            return Integer.parseInt(xpath.getAttributeValue(INDEX, LDMLConstants.TYPE));
        }

        public String getStart() {
            return xpath.getAttributeValue(INDEX, LDMLConstants.START);
        }

        public String getEnd() {
            return xpath.getAttributeValue(INDEX, LDMLConstants.END);
        }

        public String getCode() {
            return xpath.getAttributeValue(INDEX, LDMLConstants.CODE);
        }

        public String[] getAliases() {
            final String s = xpath.getAttributeValue(INDEX, LDMLConstants.ALIASES);
            if (s == null) return "".split(" "); // empty array
            return s.split(" ");
        }

        @Override
        public String toString() {
            return String.format(
                    "ERA %d, [%s-%s] code=%s, aliases=%s",
                    getType(), getStart(), getEnd(), getCode(), String.join(",", getAliases()));
        }
    }

    /** a <calendar type=> element */
    static class CalendarData implements Iterable<Integer> {
        static final int INDEX = 3; // index of our element

        /** the <calendarSystem> xpath */
        private XPathValue system = null;

        /** the <inheritEras> xpath */
        private XPathValue inheritEras = null;

        private Map<Integer, EraData> eras = new HashMap<Integer, EraData>();

        public String getSystemType() {
            if (system == null) return null;
            return system.getAttributeValue(INDEX, LDMLConstants.TYPE);
        }

        public String getInheritEras() {
            if (inheritEras == null) return null;
            return inheritEras.getAttributeValue(INDEX, LDMLConstants.CALENDAR);
        }

        @Override
        public Iterator<Integer> iterator() {
            return eras.keySet().iterator();
        }

        public EraData get(Integer era) {
            return eras.get(era);
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
            c.inheritEras = x;
        }

        private void acceptCalendarSystem(XPathValue x, CalendarData c) {
            if (c.system != null) {
                throw new IllegalArgumentException("Duplicate calendar system: " + x);
            }
            c.system = x;
        }

        @Override
        public SupplementalCalendarData get() {
            return new SupplementalCalendarData(typeToCalendar);
        }
    }

    private SupplementalCalendarData(Map<String, CalendarData> m) {
        // TODO: freeze all types
        // m.forEach(c -> c.freeze());
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
