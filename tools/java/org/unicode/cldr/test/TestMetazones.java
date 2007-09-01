package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Verify that all zones in a metazone have the same behavior within the
 * specified period.
 * 
 * @author markdavis
 * 
 */
public class TestMetazones {
  
  /**
   * Set if we are suppressing daylight differences in the test.
   */
  static boolean noDaylight = false;

  final static SupplementalDataInfo supplementalData = SupplementalDataInfo
      .getInstance("C:/cvsdata/unicode/cldr/common/supplemental/");

  // WARNING: right now, the only metazone rules are in root, so that's all we're testing.
  // if there were rules in other files, we'd have to check them to, by changing this line.
  Factory factory = Factory.make(Utility.MAIN_DIRECTORY, "root");

  XPathParts parts = new XPathParts();

  int errorCount = 0;

  int warningCount = 0;
  
  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT"));
    new TestMetazones().testAll();
  }

  void testAll() {
    for (String locale : factory.getAvailable()) {
      test(locale);
    }
    System.out.println("Total Errors: " + errorCount);
    System.out.println("Total Warnings: " + warningCount);
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
    System.out.println("Testing metazone info in: " + locale);
    // get the resolved version
    file = file.make(locale, true);
    Relation<String, DateRangeAndZone> mzoneToData = new Relation(
        new TreeMap(), TreeSet.class);

    Relation<String, DateRangeAndZone> zoneToDateRanges = new Relation(
        new TreeMap(), TreeSet.class);

    fillMetazoneData(file, mzoneToData, zoneToDateRanges);

    checkCoverage(zoneToDateRanges);

    checkGapsAndOverlaps(zoneToDateRanges);

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
        // System.out.println(mzone + "\t" + new Data(zone, to, from));
      }
    }
  }

  private void checkMetazoneConsistency(
      Relation<String, DateRangeAndZone> mzoneToData) {
    System.out.println();
    System.out.println("Verify everything matches in metazones");

    for (String mzone : mzoneToData.keySet()) {
      if (false) System.out.println(mzone);
      Set<DateRangeAndZone> values = mzoneToData.getAll(mzone);
      if (false) {
        for (DateRangeAndZone value : values) {
          System.out.println("\t" + value);
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
          // System.out.println("Comparing " + value + " to " + value2);
          DateRange overlap = value.range.getOverlap(value2.range);
          if (overlap.getExtent() == 0) {
            continue;
          }

          OlsonTimeZone timezone1 = new OlsonTimeZone(value.zone);
          OlsonTimeZone timezone2 = new OlsonTimeZone(value2.zone);
          List<Long> list = getDifferencesOverRange(timezone1, timezone2, overlap);
          
          if (list.size() != 0) {
            errln("Zones " + value.zone + " and " + value2.zone
                + " shouldn't be in the same metazone <" + mzone + "> during the period "
                + overlap + ". " + "Sample dates:" + "\r\n\t"
                + showDifferences(timezone1, timezone2, list));
          }
        }
      }
    }
  }

  String showDifferences(OlsonTimeZone zone1, OlsonTimeZone zone2,
      List<Long> list) {
    // Add all the transition points for both of them
    

    StringBuffer buffer = new StringBuffer();

    int count = 0;
    boolean abbreviating = list.size() > 7;
    for (long point : list) {
      count++;
      if (abbreviating) {
        if (count == 4)
          buffer.append("...\r\n\t");
        if (count >= 4 && count < list.size() - 2)
          continue;
      }
      int offset1 = zone1.getOffset(point);
      int offset2 = zone2.getOffset(point);
      String ending = count == list.size() ? " hours" : " hours, up to \r\n\t";
      buffer.append(DateRange.format(point) + ": delta="
          + ((offset1 - offset2) / 3600000.0) + ending);
    }
    return buffer.toString();
  }

  private List<Long> getDifferencesOverRange(OlsonTimeZone zone1, OlsonTimeZone zone2, DateRange overlap) {
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
    return list;
  }

  /**
   * My own private version so I can suppress daylight.
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
    System.out.println();
    System.out.println("Verify no gaps or overlaps in zones");
    for (String zone : zoneToDateRanges.keySet()) {
      if (false)
        System.out.println(zone);
      Set<DateRangeAndZone> values = zoneToDateRanges.getAll(zone);
      long last = DateRange.MIN_DATE;
      for (DateRangeAndZone value : values) {
        if (false)
          System.out.println("\t" + value);
        checkGapOrOverlap(last, value.range.startDate);
        last = value.range.endDate;
      }
      checkGapOrOverlap(last, DateRange.MAX_DATE);
    }
  }

  private void checkCoverage(Relation<String, DateRangeAndZone> zoneToDateRanges) {
    System.out.println("Check for coverage of canonical zones");
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
      warnln("Missing canonical zones: " + missing);
    }
    Set<String> extras = new TreeSet<String>(zoneToDateRanges.keySet());
    extras.removeAll(canonicalZones);
    if (extras.size() != 0) {
      warnln("Superfluous  zones (not canonical): " + extras);
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
    System.out.println("ERROR: " + string);
    errorCount++;
  }

  private void warnln(String string) {
    System.out.println("WARNING: " + string);
    warningCount++;
  }

  /**
   * Stores a range and a zone. The zone might be a timezone or metazone.
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

  static class DateRange implements Comparable<DateRange> {
    long startDate;

    long endDate;

    public DateRange(String startDate, String endDate) {
      this(parse(startDate, false), parse(endDate, true));
    }

    public DateRange(long startDate, long endDate) {
      this.startDate = startDate;
      this.endDate = endDate;
      if (startDate > endDate) {
        throw new IllegalArgumentException("Out of order dates: " + startDate
            + ", " + endDate);
      }
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

    static long MIN_DATE = new Date(70, 0, 1, 0, 0, 0).getTime();

    static long MAX_DATE = new Date(110, 0, 1, 0, 0, 0).getTime();

    public String toString() {
      return "{" + format(startDate) + " to " + format(endDate) + "}";
    }

    public static String format(Date date) {
      return (//date.equals(MIN_DATE) ? "-∞" : date.equals(MAX_DATE) ? "+∞" : 
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