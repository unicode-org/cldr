package org.unicode.cldr.unittest;

import org.unicode.cldr.icu.CollectionUtilities;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestMetazoneTransitions {
  
  private static final int HOUR = 60*60*1000;
  static final long startDate;
  static final long endDate;
  static final SimpleDateFormat neutralFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", ULocale.ENGLISH);
  static {
    TimeZone GMT = TimeZone.getTimeZone("Etc/GMT");
    neutralFormat.setTimeZone(GMT);
    Calendar cal = Calendar.getInstance(GMT, ULocale.US);
    int year = cal.get(Calendar.YEAR);
    cal.clear(); // need to clear fractional seconds
    cal.set(1970,0,1,0,0,0);
    startDate = cal.getTimeInMillis();
    cal.set(year + 5,0,1,0,0,0);
    endDate = cal.getTimeInMillis();
    if (startDate != 0) {
      throw new IllegalArgumentException();
    }
  }

  public static void main(String[] args) {
    new TestMetazoneTransitions().run();
    String[] zones = TimeZone.getAvailableIDs();
    for (String zone : zones) {
      
    }
  }
  
  private static class ZoneTransition implements Comparable<ZoneTransition> {
    long date;
    int offset;
    public boolean equals(Object that) {
      ZoneTransition other = (ZoneTransition) that;
      return date == other.date && offset == other.offset;
    }
    public int hashCode() {
      return (int)(date ^ (date >>> 32) ^ offset);
    }
    public ZoneTransition(long date, int offset) {
      super();
      // TODO Auto-generated constructor stub
      this.date = date;
      this.offset = offset;
    }
    /**
     * Return the one with the smaller offset, or if equal then the smallest time
     * @param o
     * @return
     */
    public int compareTo(ZoneTransition o) {
      int delta = offset - o.offset;
      if (delta != 0) return delta;
      long delta2 = date - o.date;
      return delta2 == 0 ? 0 : delta2 < 0 ? -1 : 1;
    }
    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return neutralFormat.format(date) + ": " + ((double)offset)/HOUR + "hrs";
    }
  }
  
  private static class ZoneTransitions implements Comparable<ZoneTransitions> {
    List<ZoneTransition> chronologicalList = new ArrayList<ZoneTransition>();
    public boolean equals(Object that) {
      ZoneTransitions other = (ZoneTransitions) that;
      return chronologicalList.equals(other.chronologicalList);
    }
    public int hashCode() {
      return chronologicalList.hashCode();
    }
    ZoneTransitions(String tzid, boolean allowDaylight) {
      OlsonTimeZone zone = (OlsonTimeZone)TimeZone.getTimeZone(tzid);
      for (long date = startDate; date < endDate; ) { // need better documentation for this. inclusive? base date?
        addIfDifferent(zone, date, allowDaylight);
        TimeZoneTransition transition = zone.getNextTransition(date, false);
        if (transition == null) {
          break;
        }
        date = transition.getTime();
      }
    }
    
    private void addIfDifferent(OlsonTimeZone zone, long date, boolean allowDaylight) {
      int offset = zone.getOffset(date);
      final boolean inDaylightTime = zone.inDaylightTime(new Date(date)); // clumsy API forces creation of Date
      if (allowDaylight) {
        offset = inDaylightTime ? HOUR : 0;
      } else if (inDaylightTime) {
        offset -= HOUR; // ugly: this works for the times we care about, and there is no other way.
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
      ZoneTransition a;
      ZoneTransition b;
      int minSize = Math.min(chronologicalList.size(), other.chronologicalList.size());
      for (int i = 0; i < minSize; ++i) {
        a = chronologicalList.get(i);
        b = other.chronologicalList.get(i);
        int order = a.compareTo(b);
        if (order != 0) return order;
      }
      return chronologicalList.size() - other.chronologicalList.size();
    }

    public String toString(String separator) {
      return Utility.join(chronologicalList, separator);
    }
    
    public String toString() {
      return toString("; ");
    }
    public int size() {
      // TODO Auto-generated method stub
      return chronologicalList.size();
    }
  }
  
  final static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance("C:/cvsdata/unicode/cldr/common/supplemental/");
  
  private void run() {
    //String[] zones = TimeZone.getAvailableIDs();
    Relation<ZoneTransitions, String> partition = new Relation(new TreeMap(), TreeSet.class);
    Relation<ZoneTransitions, String> daylightPartition = new Relation(new TreeMap(), TreeSet.class);
    Map<String, String> toDaylight = new TreeMap();
    Map<ZoneTransitions, String> daylightNames = new TreeMap();
    int daylightCount = 0;
    
    for (String zone : supplementalData.getCanonicalZones()) {
      ZoneTransitions transitions = new ZoneTransitions(zone, false);
      partition.put(transitions, zone);
      transitions = new ZoneTransitions(zone, true);
      if (transitions.size() > 1) {
        String daylightName = daylightNames.get(transitions);
        if (daylightName == null) {
          daylightNames.put(transitions, daylightName = "D" + (++daylightCount));
        }
        daylightPartition.put(transitions, zone);
        toDaylight.put(zone, daylightName);
      }
    }
    int count = 0;
    for (ZoneTransitions transitions : partition.keySet()) {
      System.out.println();
      System.out.println("Mechanical Metazone Partition " + (++count));
      System.out.println("\t" + transitions.toString("\r\n\t"));
      for (String zone : partition.getAll(transitions)) {
        String daylightName = toDaylight.get(zone);
        System.out.println("\t\t" + zone + (daylightName == null ? "" : "\t" + daylightName));
      }
    }
    System.out.println();
    System.out.println("=====================================================");
    System.out.println();
    for (ZoneTransitions transitions : daylightPartition.keySet()) {
      System.out.println();
      boolean gotName = false;
      for (String zone : daylightPartition.getAll(transitions)) {
        if (!gotName) {
          System.out.println("Daylight Partition\t" + toDaylight.get(zone));
          if (false) System.out.println("\t" + transitions.toString("\r\n\t"));
          gotName = true;
        }
        System.out.println("\t\t" + zone);
      }
    }

  }
}