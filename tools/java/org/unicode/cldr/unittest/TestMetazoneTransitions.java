package org.unicode.cldr.unittest;

import org.unicode.cldr.util.Pair;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestMetazoneTransitions {

  private static final int printDaylightTransitions = 6;

  private static final int SECOND = 1000;
  private static final int MINUTE = 60 * SECOND;
  private static final int HOUR = 60 * MINUTE;

  static final long startDate;

  static final long endDate;

  static final SimpleDateFormat neutralFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", ULocale.ENGLISH);
  static final DecimalFormat threeDigits = new DecimalFormat("000");
  static final DecimalFormat twoDigits = new DecimalFormat("00");

  public static final Set<Integer> allOffsets = new TreeSet<Integer>();
  
  static {
    TimeZone GMT = TimeZone.getTimeZone("Etc/GMT");
    neutralFormat.setTimeZone(GMT);
    Calendar cal = Calendar.getInstance(GMT, ULocale.US);
    int year = cal.get(Calendar.YEAR);
    cal.clear(); // need to clear fractional seconds
    cal.set(1970, 0, 1, 0, 0, 0);
    startDate = cal.getTimeInMillis();
    cal.set(year + 5, 0, 1, 0, 0, 0);
    endDate = cal.getTimeInMillis();
    if (startDate != 0) {
      throw new IllegalArgumentException();
    }
  }

  public static void main(String[] args) throws Exception {
   java.util.TimeZone zone2 = java.util.TimeZone.getTimeZone("GMT+830");
   System.out.println(zone2.getID());
   zone2 = java.util.TimeZone.getTimeZone("GMT+08");
   System.out.println(zone2.getID());
   zone2 = java.util.TimeZone.getTimeZone("Etc/GMT-8");
   System.out.println(zone2.getID());
   
   
   java.util.TimeZone.setDefault( java.util.TimeZone.getTimeZone("America/Los_Angeles"));
    DateFormat javaFormat = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.MEDIUM, Locale.US);
    long start = new Date(107,0,1,0,30,0).getTime();
    start -= start % 1000; // clean up millis
    long end = new Date(108,0,1,0,30,0).getTime();
    for (long date = start; date < end; date += 15*MINUTE) {
      String formatted = javaFormat.format(date);
      Date roundTrip = javaFormat.parse(formatted);
      if (roundTrip.getTime() != date) {
        System.out.println("Java roundtrip failed for: " + formatted + "\tSource: " + new Date(date) + "\tTarget: " + roundTrip);
      }
    }
    new TestMetazoneTransitions().run();
  }

  private static class ZoneTransition implements Comparable<ZoneTransition> {
    long date;

    int offset;

    public boolean equals(Object that) {
      ZoneTransition other = (ZoneTransition) that;
      return date == other.date && offset == other.offset;
    }

    public int hashCode() {
      return (int) (date ^ (date >>> 32) ^ offset);
    }

    public ZoneTransition(long date, int offset) {
      this.date = date;
      this.offset = offset;
    }

    /**
     * Return the one with the smaller offset, or if equal then the smallest
     * time
     * 
     * @param o
     * @return
     */
    public int compareTo(ZoneTransition o) {
      int delta = offset - o.offset;
      if (delta != 0)
        return delta;
      long delta2 = date - o.date;
      return delta2 == 0 ? 0 : delta2 < 0 ? -1 : 1;
    }

    @Override
    public String toString() {
      return neutralFormat.format(date) + ": " + ((double) offset) / HOUR
          + "hrs";
    }
  }

  enum DaylightChoice {NO_DAYLIGHT, ONLY_DAYLIGHT};
  
  private static class ZoneTransitions implements Comparable<ZoneTransitions> {
    List<ZoneTransition> chronologicalList = new ArrayList<ZoneTransition>();

    public boolean equals(Object that) {
      ZoneTransitions other = (ZoneTransitions) that;
      return chronologicalList.equals(other.chronologicalList);
    }

    public int hashCode() {
      return chronologicalList.hashCode();
    }

    public ZoneTransitions(String tzid, DaylightChoice allowDaylight) {
      TimeZone zone = TimeZone.getTimeZone(tzid);
      for (long date = startDate; date < endDate; date = getTransitionAfter(zone, date)) {
        addIfDifferent(zone, date, allowDaylight);
      }
    }

    private void addIfDifferent(TimeZone zone, long date,
        DaylightChoice allowDaylight) {
      int offset = zone.getOffset(date);
      allOffsets.add(offset);
      int delta = getDSTSavings(zone, date);
      switch (allowDaylight) {
        case ONLY_DAYLIGHT:
          offset = delta;
          break;
        case NO_DAYLIGHT: 
          offset -= delta;
          break;
      }
      int size = chronologicalList.size();
      if (size > 0) {
        ZoneTransition last = chronologicalList.get(size - 1);
        if (last.offset == offset) {
          return;
        }
      }
      chronologicalList.add(new ZoneTransition(date, offset));
    }

    public int compareTo(ZoneTransitions other) {
      int minSize = Math.min(chronologicalList.size(), other.chronologicalList
          .size());
      for (int i = 0; i < minSize; ++i) {
        ZoneTransition a = chronologicalList.get(i);
        ZoneTransition b = other.chronologicalList.get(i);
        int order = a.compareTo(b);
        if (order != 0)
          return order;
      }
      return chronologicalList.size() - other.chronologicalList.size();
    }

    public String toString(String separator, int abbreviateToSize) {
      if (abbreviateToSize > 0 && chronologicalList.size() > abbreviateToSize) {
        int limit = abbreviateToSize/2;
        return CldrUtility.join(slice(chronologicalList, 0, limit), separator)
        + separator + "..." + separator
        + CldrUtility.join(slice(chronologicalList, chronologicalList.size() - limit, chronologicalList.size()), separator);
      }
      return CldrUtility.join(chronologicalList, separator);
    }

    public String toString() {
      return toString("; ", -1);
    }

    public int size() {
      // TODO Auto-generated method stub
      return chronologicalList.size();
    }

    public Pair<ZoneTransitions, ZoneTransitions> getDifferenceFrom(ZoneTransitions other) {
      int minSize = Math.min(chronologicalList.size(), other.chronologicalList
          .size());
      for (int i = 0; i < minSize; ++i) {
        ZoneTransition a = chronologicalList.get(i);
        ZoneTransition b = other.chronologicalList.get(i);
        int order = a.compareTo(b);
        if (order != 0)
          return new Pair(a,b);
      }
      if (chronologicalList.size() > other.chronologicalList.size()) {
        return new Pair(chronologicalList.get(minSize), null);
      } else if (chronologicalList.size() < other.chronologicalList.size()) {
        return new Pair(null, other.chronologicalList.get(minSize));
      } else {
        return new Pair(null, null);
      }
    }

    public ZoneTransition get(int i) {
      return chronologicalList.get(i);
    }
  }

  final static SupplementalDataInfo supplementalData = SupplementalDataInfo
      .getInstance("C:/cvsdata/unicode/cldr/common/supplemental/");

  private void run() {
    // String[] zones = TimeZone.getAvailableIDs();
    Relation<ZoneTransitions, String> partition = new Relation(new TreeMap(),
        TreeSet.class);
    Relation<ZoneTransitions, String> daylightPartition = new Relation(
        new TreeMap(), TreeSet.class);
    Map<String, String> toDaylight = new TreeMap();
    Map<ZoneTransitions, String> daylightNames = new TreeMap();
    int daylightCount = 0;

    // get the main data
    for (String zone : supplementalData.getCanonicalZones()) {
      ZoneTransitions transitions = new ZoneTransitions(zone, DaylightChoice.NO_DAYLIGHT);
      partition.put(transitions, zone);
      transitions = new ZoneTransitions(zone, DaylightChoice.ONLY_DAYLIGHT);
      if (transitions.size() > 1) {
        daylightPartition.put(transitions, zone);
      }
    }
    // now assign names
    int count = 0;
    for (ZoneTransitions transitions : daylightPartition.keySet()) {
      final String dname = "D"  + threeDigits.format(++count);
      daylightNames.put(transitions, dname);
      for (String zone : daylightPartition.getAll(transitions)) {
        toDaylight.put(zone, dname);
      }
    }
    // get the "primary" zone for each metazone
    Map<String,String> zoneToMeta = new TreeMap<String,String>();
    Map<String, Map<String, String>> metazoneToRegionToZone = supplementalData.getMetazoneToRegionToZone();
    for (String meta : metazoneToRegionToZone.keySet()) {
      Map<String, String> regionToZone = metazoneToRegionToZone.get(meta);
      String keyZone = regionToZone.get("001");
      zoneToMeta.put(keyZone, meta);
    }
    
    System.out.println();
    System.out.println("=====================================================");
    System.out.println("*** Non-Daylight Partition");
    System.out.println("=====================================================");
    System.out.println();

    count = 0;
    Set<String> noMeta = new TreeSet();
    Set<String> multiMeta = new TreeSet();
    Set<String> stableZones = new TreeSet();
    for (ZoneTransitions transitions : partition.keySet()) {

      System.out.println();
      final String nonDaylightPartitionName = "M" + threeDigits.format(++count);
      System.out.println("Non-Daylight Partition " + nonDaylightPartitionName);
      int metaCount = 0;
      Set<String> metas = new TreeSet<String>();
      for (String zone : partition.getAll(transitions)) {
        String daylightName = toDaylight.get(zone);
        String meta = zoneToMeta.get(zone);
        if (meta != null) {
          ++metaCount;
          metas.add(meta);
        }
        System.out.println("\t" + zone
            + (daylightName == null ? "" : "\t" + daylightName)
            + (meta == null ? "" : "\t\tMETA:" + meta)
            );
      }
      if (metaCount == 0) {
        noMeta.add(nonDaylightPartitionName + "{" + CldrUtility.join(partition.getAll(transitions),", ") + "}");
      } else if (metaCount > 1) {
        multiMeta.add(nonDaylightPartitionName + "{" + CldrUtility.join(metas,", ") + "}");
      }
      if (transitions.size() == 1) {
        final int offset = transitions.get(0).offset;
        allOffsets.remove(offset);
        stableZones.add(nonDaylightPartitionName + ", " + offset/(double)HOUR + "hrs " + "{" + CldrUtility.join(partition.getAll(transitions),", ") + "}");
      }
      System.out.println("\t\t" + transitions.toString(CldrUtility.LINE_SEPARATOR + "\t\t", -1));
    }
    System.out.println();
    System.out.println("*** Non-Daylight Partitions with no canonical meta");
    System.out.println("\t" + CldrUtility.join(noMeta, CldrUtility.LINE_SEPARATOR + "\t"));
    System.out.println();
    System.out.println("*** Non-Daylight Partitions with more than one canonical meta");
    System.out.println("\t" + CldrUtility.join(multiMeta, CldrUtility.LINE_SEPARATOR + "\t"));
    System.out.println();
    System.out.println("*** Stable Non-Daylight Partitions");
    System.out.println("\t" + CldrUtility.join(stableZones, CldrUtility.LINE_SEPARATOR + "\t"));
    System.out.println();
    System.out.println("*** Offsets with no stable partition");
    for (int offset : allOffsets) {
      System.out.println("\t" + offset/(double)HOUR + "hrs");
    }
    System.out.println();

    System.out.println();
    System.out.println("=====================================================");
    System.out.println("*** Daylight Partition");
    System.out.println("=====================================================");
    System.out.println();
    
    ZoneTransitions lastTransitions = null;
    String lastName = null;
    for (ZoneTransitions transitions : daylightPartition.keySet()) {
      System.out.println();
      String daylightName = daylightNames.get(transitions);
      System.out.println("Daylight Partition\t" + daylightName);
      for (String zone : daylightPartition.getAll(transitions)) {
        System.out.println("\t" + zone);
      }
      System.out.println("\t\t" + transitions.toString(CldrUtility.LINE_SEPARATOR + "\t\t", printDaylightTransitions));
      if (lastTransitions != null ) {
        Pair<ZoneTransitions, ZoneTransitions> diff = transitions.getDifferenceFrom(lastTransitions);
        System.out.println("\t\tTransition Difference from " + lastName + ":\t"+ diff);
      }
      lastTransitions = transitions;
      lastName = daylightName;
    }

  }
  
  public static <T> List<T> slice(List<T> list, int start, int limit) {
    ArrayList<T> temp = new ArrayList<T>();
    for (int i = start; i < limit; ++i) {
      temp.add(list.get(i));
    }
    return temp;
  }

  /* Methods that ought to be on TimeZone */
  /**
   * Return the next point in time after date when the zone has a different
   * offset than what it has on date. If there are no later transitions, returns
   * Long.MAX_VALUE.
   * 
   * @param zone
   *          input zone -- should be method of TimeZone
   * @param date
   *          input date, in standard millis since 1970-01-01 00:00:00 GMT
   */
  public static long getTransitionAfter(TimeZone zone, long date) {
    TimeZoneTransition transition = ((OlsonTimeZone) zone).getNextTransition(
        date, false);
    if (transition == null) {
      return Long.MAX_VALUE;
    }
    date = transition.getTime();
    return date;
  }

  /**
   * Return true if the zone is in daylight savings on the date.
   * 
   * @param zone
   *          input zone -- should be method of TimeZone
   * @param date
   *          input date, in standard millis since 1970-01-01 00:00:00 GMT
   */
  public static boolean inDaylightTime(TimeZone zone, long date) {
    return ((OlsonTimeZone) zone).inDaylightTime(new Date(date));
  }

  /**
   * Return the daylight savings offset on the given date.
   * 
   * @param zone
   *          input zone -- should be method of TimeZone
   * @param date
   *          input date, in standard millis since 1970-01-01 00:00:00 GMT
   */
  public static int getDSTSavings(TimeZone zone, long date) {
    if (!inDaylightTime(zone, date)) {
      return 0;
    }
    TimeZoneTransition transition = ((OlsonTimeZone) zone)
        .getPreviousTransition(date + 1, true);
    TimeZoneRule to = transition.getTo();
    int delta = to.getDSTSavings();
    //    if (delta != HOUR) {
    //      System.out.println("Delta " + delta/(double)HOUR + " for " + zone.getID());
    //    }
    return delta;
  }
}