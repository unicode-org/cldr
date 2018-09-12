package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;

/**
 * Verify that all zones in a metazone have the same behavior within the
 * specified period.
 *
 * @author markdavis
 *
 */
public class TestMetazones {
    public static boolean DEBUG = false;

    private static final long HOUR = 3600000;
    private static final long DAY = 24 * 60 * 60 * 1000L;
    private static final long MINUTE = 60000;

    /**
     * Set if we are suppressing daylight differences in the test.
     */
    final static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance();

    // WARNING: right now, the only metazone rules are in root, so that's all we're testing.
    // if there were rules in other files, we'd have to check them to, by changing this line.
    Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, "root");

    XPathParts parts = new XPathParts();

    int errorCount = 0;

    int warningCount = 0;

    NumberFormat days = new DecimalFormat("0.000");
    NumberFormat hours = new DecimalFormat("+0.00;-0.00");
    PrintWriter log = null;
    PrintWriter errorLog = null;
    private boolean skipConsistency;
    private boolean skipPartialDays;
    private boolean noDaylight;

    public static void main(String[] args) throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT"));
        new TestMetazones().testAll();
    }

    void testAll() throws IOException {
        try {
            noDaylight = CldrUtility.getProperty("nodaylight", null) != null;
            skipPartialDays = CldrUtility.getProperty("skippartialdays", null, "") != null;
            skipConsistency = CldrUtility.getProperty("skipconsistency", null, "") != null;

            String exemplarOutFile = CldrUtility.getProperty("log", null,
                CLDRPaths.GEN_DIRECTORY + "metazoneLog.txt");
            if (exemplarOutFile != null) {
                log = FileUtilities.openUTF8Writer("", exemplarOutFile);
            }
            String errorOutFile = CldrUtility.getProperty("errors", null,
                CLDRPaths.GEN_DIRECTORY + "metazoneErrors" +
                    (noDaylight ? "-noDaylight" : "") +
                    (skipPartialDays ? "-skipPartialDays" : "")
                    + ".txt");
            if (errorOutFile != null) {
                errorLog = FileUtilities.openUTF8Writer("", errorOutFile);
            } else {
                errorLog = new PrintWriter(System.out);
            }

            for (String locale : factory.getAvailable()) {
                test(locale);
            }
        } finally {
            errorLog.println("Total Errors: " + errorCount);
            errorLog.println("Total Warnings: " + warningCount);
            if (log != null) {
                log.close();
            }
            if (errorLog != null) {
                errorLog.close();
            }
        }
    }

    /**
     * Test a locale.
     */
    void test(String locale) {
        CLDRFile file = factory.make(locale, false);
        if (!fileHasMetazones(file)) {
            return;
        }
        // testing zone information
        errorLog.println("Testing metazone info in: " + locale);
        // get the resolved version
        file = factory.make(locale, true);
        Relation<String, DateRangeAndZone> mzoneToData = Relation.<String, DateRangeAndZone> of(
            new TreeMap<String, Set<DateRangeAndZone>>(), TreeSet.class);

        Relation<String, DateRangeAndZone> zoneToDateRanges = Relation.<String, DateRangeAndZone> of(
            new TreeMap<String, Set<DateRangeAndZone>>(), TreeSet.class);

        fillMetazoneData(file, mzoneToData, zoneToDateRanges);

        checkCoverage(zoneToDateRanges);

        checkGapsAndOverlaps(zoneToDateRanges);

        checkExemplars(mzoneToData, zoneToDateRanges);
        if (skipConsistency) return;

        checkMetazoneConsistency(mzoneToData);
    }

    private void fillMetazoneData(CLDRFile file,
        Relation<String, DateRangeAndZone> mzoneToData,
        Relation<String, DateRangeAndZone> zoneToDateRanges) {
        for (String path : file) {
            if (path.contains("/usesMetazone")) {
                /*
                 * Sample: <zone type="Asia/Yerevan"> <usesMetazone to="1991-09-23"
                 * mzone="Yerevan"/> <usesMetazone from="1991-09-23" mzone="Armenia"/>
                 * </zone>
                 */
                parts.set(path);
                String from = parts.getAttributeValue(-1, "from");
                long fromDate = DateRange.parse(from, false);

                String to = parts.getAttributeValue(-1, "to");
                long toDate = DateRange.parse(to, true);

                DateRange range = new DateRange(fromDate, toDate);

                String mzone = parts.getAttributeValue(-1, "mzone");
                String zone = parts.getAttributeValue(-2, "type");

                mzoneToData.put(mzone, new DateRangeAndZone(zone, range));
                zoneToDateRanges.put(zone, new DateRangeAndZone(mzone, range));
                // errorLog.println(mzone + "\t" + new Data(zone, to, from));
            }
        }
    }

    private void checkMetazoneConsistency(
        Relation<String, DateRangeAndZone> mzoneToData) {
        errorLog.println();
        errorLog.println("*** Verify everything matches in metazones");
        errorLog.println();

        for (String mzone : mzoneToData.keySet()) {
            if (DEBUG) {
                errorLog.println(mzone);
            }
            Set<DateRangeAndZone> values = mzoneToData.getAll(mzone);
            if (DEBUG) {
                for (DateRangeAndZone value : values) {
                    errorLog.println("\t" + value);
                }
            }
            for (DateRangeAndZone value : values) {
                // quick and dirty test; make sure that everything matches over this
                // interval
                for (DateRangeAndZone value2 : values) {
                    // only do it once, so skip ones we've done the other direction
                    if (value2.compareTo(value) <= 0) {
                        continue;
                    }
                    // we have value and a different value2. Make sure that they have the
                    // same transition dates during any overlap
                    // errorLog.println("Comparing " + value + " to " + value2);
                    DateRange overlap = value.range.getOverlap(value2.range);
                    if (overlap.getExtent() == 0) {
                        continue;
                    }

                    OlsonTimeZone timezone1 = new OlsonTimeZone(value.zone);
                    OlsonTimeZone timezone2 = new OlsonTimeZone(value2.zone);
                    List<Pair<Long, Long>> list = getDifferencesOverRange(timezone1, timezone2, overlap);

                    if (list.size() != 0) {
                        errln("Zones " + showZone(value.zone) + " and " + showZone(value2.zone)
                            + " shouldn't be in the same metazone <" + mzone + "> during the period "
                            + overlap + ". " + "Sample dates:" + CldrUtility.LINE_SEPARATOR + "\t"
                            + showDifferences(timezone1, timezone2, list));
                    }
                }
            }
        }
    }

    private String showZone(String zone) {
        // TODO Auto-generated method stub
        return zone + " [" + supplementalData.getZone_territory(zone) + "]";
    }

    String showDifferences(OlsonTimeZone zone1, OlsonTimeZone zone2,
        List<Pair<Long, Long>> list) {

        StringBuffer buffer = new StringBuffer();

        int count = 0;
        boolean abbreviating = list.size() > 7;
        long totalErrorPeriod = 0;
        for (Pair<Long, Long> pair : list) {
            count++;
            long start = pair.getFirst();
            long end = pair.getSecond();
            int startDelta = getOffset(zone1, start) - getOffset(zone2, start);
            int endDelta = getOffset(zone1, end) - getOffset(zone2, end);
            if (startDelta != endDelta) {
                showDeltas(zone1, zone2, start, end);
                throw new IllegalArgumentException();
            }
            final long errorPeriod = end - start + MINUTE;
            totalErrorPeriod += errorPeriod;
            if (abbreviating) {
                if (count == 4)
                    buffer.append("..." + CldrUtility.LINE_SEPARATOR + "\t");
                if (count >= 4 && count < list.size() - 2)
                    continue;
            }

            buffer.append("delta=\t"
                + hours.format(startDelta / (double) HOUR) + " hours:\t" + DateRange.format(start) + "\tto\t" +
                DateRange.format(end) + ";\ttotal:\t" + days.format((errorPeriod) / (double) DAY) + " days"
                + CldrUtility.LINE_SEPARATOR + "\t");
        }
        buffer.append("\tTotal Period in Error:\t" + days.format((totalErrorPeriod) / (double) DAY) + " days");
        return buffer.toString();
    }

    private void showDeltas(OlsonTimeZone zone1, OlsonTimeZone zone2, long start, long end) {
        errorLog.println(zone1.getID() + ", start: " + start + ", startOffset " + getOffset(zone1, start));
        errorLog.println(zone1.getID() + ", end: " + start + ", endOffset " + getOffset(zone1, end));
        errorLog.println(zone2.getID() + ", start: " + start + ", startOffset " + getOffset(zone2, start));
        errorLog.println(zone2.getID() + ", end: " + start + ", endOffset " + getOffset(zone2, end));
    }

    /**
     * Returns a list of pairs. The delta timezone offsets for both zones should be identical between each of the points
     * in the pair
     *
     * @param zone1
     * @param zone2
     * @param overlap
     * @return
     */
    private List<Pair<Long, Long>> getDifferencesOverRange(OlsonTimeZone zone1, OlsonTimeZone zone2, DateRange overlap) {
        Set<Long> list1 = new TreeSet<Long>();
        addTransitions(zone1, zone2, overlap, list1);
        addTransitions(zone2, zone1, overlap, list1);

        // Remove any transition points that keep the same delta relationship
        List<Long> list = new ArrayList<Long>();
        int lastDelta = 0;
        for (long point : list1) {
            int offset1 = getOffset(zone1, point);
            int offset2 = getOffset(zone2, point);
            int delta = offset1 - offset2;
            if (delta != lastDelta) {
                list.add(point);
                lastDelta = delta;
            }
        }

        // now combine into a list of start/end pairs
        List<Pair<Long, Long>> result = new ArrayList<Pair<Long, Long>>();
        long lastPoint = Long.MIN_VALUE;
        for (long point : list) {
            if (lastPoint != Long.MIN_VALUE) {
                long start = lastPoint;
                long end = point - MINUTE;
                if (DEBUG && start == 25678800000L && end == 33193740000L) {
                    errorLog.println("debugStop");
                    showDeltas(zone1, zone2, start, end);
                }

                int startOffset1 = getOffset(zone1, start);
                int startOffset2 = getOffset(zone2, start);

                int endOffset1 = getOffset(zone1, end);
                int endOffset2 = getOffset(zone2, end);

                final int startDelta = startOffset1 - startOffset2;
                final int endDelta = endOffset1 - endOffset2;

                if (startDelta != endDelta) {
                    throw new IllegalArgumentException("internal error");
                }

                if (startDelta != 0) {
                    if (skipPartialDays && end - start < DAY) {
                        // do nothing
                    } else {
                        result.add(new Pair<Long, Long>(start, end)); // back up 1 minute
                    }
                }
            }
            lastPoint = point;
        }
        return result;
    }

    /**
     * My own private version so I can suppress daylight.
     *
     * @param zone1
     * @param point
     * @return
     */
    private int getOffset(OlsonTimeZone zone1, long point) {
        int offset1 = zone1.getOffset(point);
        if (noDaylight && zone1.inDaylightTime(new Date(point))) offset1 -= 3600000;
        return offset1;
    }

    private void addTransitions(OlsonTimeZone zone1, OlsonTimeZone otherZone,
        DateRange overlap, Set<Long> list) {
        long startTime = overlap.startDate;
        long endTime = overlap.endDate;
        list.add(startTime);
        list.add(endTime);
        while (true) {
            TimeZoneTransition transition = zone1.getNextTransition(startTime, false);
            if (transition == null)
                break;
            long newTime = transition.getTime();
            if (newTime > endTime) {
                break;
            }
            list.add(newTime);
            startTime = newTime;
        }
    }

    private void checkGapsAndOverlaps(
        Relation<String, DateRangeAndZone> zoneToDateRanges) {
        errorLog.println();
        errorLog.println("*** Verify no gaps or overlaps in zones");
        for (String zone : zoneToDateRanges.keySet()) {
            if (DEBUG) {
                errorLog.println(zone);
            }
            Set<DateRangeAndZone> values = zoneToDateRanges.getAll(zone);
            long last = DateRange.MIN_DATE;
            for (DateRangeAndZone value : values) {
                if (DEBUG) {
                    errorLog.println("\t" + value);
                }
                checkGapOrOverlap(last, value.range.startDate);
                last = value.range.endDate;
            }
            checkGapOrOverlap(last, DateRange.MAX_DATE);
        }
    }

    private void checkExemplars(
        Relation<String, DateRangeAndZone> mzoneToData,
        Relation<String, DateRangeAndZone> zoneToData) {

        if (log != null) {
            log.println();
            log.println("Mapping from Zones to Metazones");
            log.println();
            for (String zone : zoneToData.keySet()) {
                log.println(zone);
                for (DateRangeAndZone value : zoneToData.getAll(zone)) {
                    log.println("\t" + value.zone + "\t" + value.range);
                }
            }
            log.println();
            log.println("Mapping from Metazones to Zones");
            log.println();
        }

        errorLog.println();
        errorLog
            .println("*** Verify that every metazone has at least one zone that is always in that metazone, over the span of the metazone's existance.");
        errorLog.println();

        // get the best exemplars

        Map<String, Map<String, String>> metazoneToRegionToZone = supplementalData.getMetazoneToRegionToZone();

        for (String mzone : mzoneToData.keySet()) {
            if (DEBUG) {
                errorLog.println(mzone);
            }

            // get the best zone
            final String bestZone = metazoneToRegionToZone.get(mzone).get("001");
            if (bestZone == null) {
                errorLog.println("Metazone <" + mzone + "> is missing a 'best zone' (for 001) in supplemental data.");
            }
            Set<DateRangeAndZone> values = mzoneToData.getAll(mzone);

            Map<String, DateRanges> zoneToRanges = new TreeMap<String, DateRanges>();
            DateRanges mzoneRanges = new DateRanges();
            // first determine what the max and min dates are

            for (DateRangeAndZone value : values) {
                DateRanges ranges = zoneToRanges.get(value.zone);
                if (ranges == null) {
                    zoneToRanges.put(value.zone, ranges = new DateRanges());
                }
                ranges.add(value.range);
                mzoneRanges.add(value.range);
            }

            if (bestZone != null && !zoneToRanges.keySet().contains(bestZone)) {
                zoneToRanges.keySet().contains(bestZone);
                errorLog.println("The 'best zone' (" + showZone(bestZone) + ") for the metazone <" + mzone
                    + "> is not in the metazone!");
            }

            // now see how many there are
            int count = 0;
            if (log != null) {
                log.println(mzone + ":\t" + mzoneRanges);
            }
            for (String zone : zoneToRanges.keySet()) {
                final boolean isComplete = mzoneRanges.equals(zoneToRanges.get(zone));
                if (zone.equals(bestZone) && !isComplete) {
                    errorLog.println("The 'best zone' (" + showZone(bestZone) + ") for the metazone <" + mzone
                        + "> is only partially in the metazone!");
                }
                if (isComplete) {
                    count++;
                }
                if (log != null) {
                    log.println("\t" + zone + ":\t"
                        + supplementalData.getZone_territory(zone) + "\t"
                        + zoneToRanges.get(zone) + (isComplete ? "" : "\t\tPartial"));
                }

            }

            // show the errors
            if (count == 0) {
                errln("Metazone <" + mzone + "> does not have exemplar for whole span: " + mzoneRanges);
                for (DateRangeAndZone value : values) {
                    errorLog.println("\t" + mzone + ":\t" + value);
                    for (DateRangeAndZone mvalues : zoneToData.getAll(value.zone)) {
                        errorLog.println("\t\t\t" + showZone(value.zone) + ":\t" + mvalues);
                    }
                }
                errorLog.println("=====");
                for (String zone : zoneToRanges.keySet()) {
                    errorLog.println("\t\t\t" + zone + ":\t" + zoneToRanges.get(zone));
                }
            }
        }
    }

    private void checkCoverage(Relation<String, DateRangeAndZone> zoneToDateRanges) {
        errorLog.println();
        errorLog.println("*** Verify coverage of canonical zones");
        errorLog.println();
        Set<String> canonicalZones = supplementalData.getCanonicalZones();
        Set<String> missing = new TreeSet<String>(canonicalZones);
        missing.removeAll(zoneToDateRanges.keySet());
        for (Iterator<String> it = missing.iterator(); it.hasNext();) {
            String value = it.next();
            if (value.startsWith("Etc/")) {
                it.remove();
            }
        }
        if (missing.size() != 0) {
            errln("Missing canonical zones: " + missing);
        }
        Set<String> extras = new TreeSet<String>(zoneToDateRanges.keySet());
        extras.removeAll(canonicalZones);
        if (extras.size() != 0) {
            errln("Superfluous  zones (not canonical): " + extras);
        }
    }

    private void checkGapOrOverlap(long last, long nextDate) {
        if (last != nextDate) {
            if (last < nextDate) {
                warnln("Gap in coverage: " + DateRange.format(last) + ", "
                    + DateRange.format(nextDate));
            } else {
                errln("Overlap in coverage: " + DateRange.format(last) + ", "
                    + DateRange.format(nextDate));
            }
        }
    }

    private void errln(String string) {
        errorLog.println("ERROR: " + string);
        errorCount++;
    }

    private void warnln(String string) {
        errorLog.println("WARNING: " + string);
        warningCount++;
    }

    /**
     * Stores a range and a zone. The zone might be a timezone or metazone.
     *
     * @author markdavis
     *
     */
    static class DateRangeAndZone implements Comparable<DateRangeAndZone> {
        DateRange range;

        String zone;

        public DateRangeAndZone(String zone, String startDate, String endDate) {
            this(zone, new DateRange(startDate, endDate));
        }

        public DateRangeAndZone(String zone, DateRange range) {
            this.range = range;
            this.zone = zone;
        }

        public int compareTo(DateRangeAndZone other) {
            int result = range.compareTo(other.range);
            if (result != 0)
                return result;
            return zone.compareTo(other.zone);
        }

        public String toString() {
            return "{" + range + " => " + zone + "}";
        }
    }

    static class DateRanges {
        Set<DateRange> contents = new TreeSet<DateRange>();

        public void add(DateRange o) {
            contents.add(o);
            // now fix overlaps. Dumb implementation for now
            // they are ordered by start date, so just check that adjacent ones don't touch
            while (true) {
                boolean madeFix = false;
                DateRange last = null;
                for (DateRange range : contents) {
                    if (last != null && last.containsSome(range)) {
                        madeFix = true;
                        DateRange newRange = last.getUnion(range);
                        contents.remove(last);
                        contents.remove(range);
                        contents.add(newRange);
                    }
                    last = range;
                }
                if (!madeFix) break;
            }
        }

        boolean contains(DateRanges other) {
            for (DateRange otherRange : other.contents) {
                if (!contains(otherRange)) {
                    return false;
                }
            }
            return true;
        }

        private boolean contains(DateRange otherRange) {
            for (DateRange range : contents) {
                if (!range.containsAll(otherRange)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(Object other) {
            return contents.equals(((DateRanges) other).contents);
        }

        public int hashCode() {
            return contents.hashCode();
        }

        public String toString() {
            return contents.toString();
        }
    }

    static class DateRange implements Comparable<DateRange> {
        long startDate;

        long endDate;

        public DateRange(String startDate, String endDate) {
            this(parse(startDate, false), parse(endDate, true));
        }

        public boolean containsAll(DateRange otherRange) {
            return startDate <= otherRange.startDate && otherRange.endDate <= endDate;
        }

        /**
         * includes cases where they touch.
         *
         * @param otherRange
         * @return
         */
        public boolean containsNone(DateRange otherRange) {
            return startDate > otherRange.endDate || otherRange.startDate > endDate;
        }

        /**
         * includes cases where they touch.
         *
         * @param otherRange
         * @return
         */
        public boolean containsSome(DateRange otherRange) {
            return startDate <= otherRange.endDate && otherRange.startDate <= endDate;
        }

        public DateRange(long startDate, long endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public long getExtent() {
            return endDate - startDate;
        }

        public DateRange getOverlap(DateRange other) {
            long start = startDate;
            if (start < other.startDate) {
                start = other.startDate;
            }
            long end = endDate;
            if (end > other.endDate) {
                end = other.endDate;
            }
            // make sure we are ordered
            if (end < start) {
                end = start;
            }
            return new DateRange(start, end);
        }

        public DateRange getUnion(DateRange other) {
            long start = startDate;
            if (start > other.startDate) {
                start = other.startDate;
            }
            long end = endDate;
            if (end < other.endDate) {
                end = other.endDate;
            }
            // make sure we are ordered
            if (end < start) {
                end = start;
            }
            return new DateRange(start, end);
        }

        static long parse(String date, boolean end) {
            if (date == null)
                return end ? MAX_DATE : MIN_DATE;
            try {
                return iso1.parse(date).getTime();
            } catch (ParseException e) {
                try {
                    return iso2.parse(date).getTime();
                } catch (ParseException e2) {
                    throw new IllegalArgumentException("unexpected error in data", e);
                }
            }
        }

        static DateFormat iso1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        static DateFormat iso2 = new SimpleDateFormat("yyyy-MM-dd");

        public int compareTo(DateRange other) {
            if (startDate < other.startDate)
                return -1;
            if (startDate > other.startDate)
                return 1;
            if (endDate < other.endDate)
                return -1;
            if (endDate > other.endDate)
                return 1;
            return 0;
        }

        // Get Date-Time in milliseconds
        private static long getDateTimeinMillis(int year, int month, int date, int hourOfDay, int minute, int second) {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, date, hourOfDay, minute, second);
            return cal.getTimeInMillis();
        }

        static long MIN_DATE = getDateTimeinMillis(70, 0, 1, 0, 0, 0);

        static long MAX_DATE = getDateTimeinMillis(110, 0, 1, 0, 0, 0);

        public String toString() {
            return "{" + format(startDate) + " to " + format(endDate) + "}";
        }

        public static String format(Date date) {
            return (// date.equals(MIN_DATE) ? "-∞" : date.equals(MAX_DATE) ? "+∞" :
            iso1.format(date));
        }

        public static String format(long date) {
            return format(new Date(date));
        }

    }

    boolean fileHasMetazones(CLDRFile file) {
        for (String path : file) {
            if (path.contains("usesMetazone"))
                return true;
        }
        return false;
    }
}