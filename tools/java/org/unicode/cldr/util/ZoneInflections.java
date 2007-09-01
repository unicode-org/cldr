/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/*
 * ZoneInflections is an ordered list of inflection points between two dates,
 * typically 1970 and current year + 5. An inflection point is where the offset
 * from GMT changes; its offset is valid up until the next inflection point.
 * <p> Internally the inflection points are stored from highest
 * (at offset 0) to lowest. The ZoneInflections has no knowledge of the
 * internals of TimeZones -- public API is queried to get the information.
 */
public class ZoneInflections implements Comparable {
  static private final long SECOND = 1000;

  static private final long MINUTE = 60 * SECOND;

  static private final long HOUR = 60 * MINUTE;

  static private final double DHOUR = HOUR;

  static private final long DAY = 24 * HOUR;

  static private final long GROSS_PERIOD = 15 * DAY; // assumption is that no
                                                      // zones shift is less
                                                      // than this period

  static private final long EPSILON = 15 * MINUTE; // smallest interval we test
                                                    // to

  static private final int currentYear = new Date().getYear() + 1900;

  static private final long endDate = getDateLong(currentYear + 5, 1, 1);

  static private final long startDate = getDateLong(1970, 1, 1);

  // computed below
  private int minOffset;

  private int maxOffset;

  // ordered most recently first
  List<InflectionPoint> inflectionPoints = new ArrayList<InflectionPoint>();

  public int getMaxOffset() {
    return maxOffset;
  }

  public int getMinOffset() {
    return minOffset;
  }

  public int getMaxOffset(long afterDateTime) {
    long minSoFar = Integer.MAX_VALUE;
    for (int i = 0; i < inflectionPoints.size(); ++i) {
      InflectionPoint ip = inflectionPoints.get(i);
      if (ip.utcDateTime < afterDateTime)
        break;
      if (ip.offset < minSoFar)
        minSoFar = ip.offset;
    }
    return (int) minSoFar;
  }

  public int getMinOffset(long afterDateTime) {
    long maxSoFar = Integer.MIN_VALUE;
    for (int i = 0; i < inflectionPoints.size(); ++i) {
      InflectionPoint ip = inflectionPoints.get(i);
      if (ip.utcDateTime < afterDateTime)
        break;
      if (ip.offset > maxSoFar)
        maxSoFar = ip.offset;
    }
    return (int) maxSoFar;
  }

  public String toString() {
    return inflectionPoints.toString();
  }

  public ZoneInflections(TimeZone zone) {
    // System.out.println("Creating Inflection Points for: " + zone.getID());
    // find inflexion points; times where the offset changed
    // if (zone.getOffset(lastDate) != zone.getOffset(endDate2)) lastDate =
    // endDate2;

    // System.out.println("\tAdding: " + dtf.format(new Date(lastDate)));
    int lastOffset = zone.getOffset(endDate);
    inflectionPoints.add(new InflectionPoint(endDate, zone.getOffset(endDate)));
    long lastInflection = endDate;

    // we do a gross search, then narrow in when we find a difference from the
    // last one
    long lastDate = endDate;
    minOffset = maxOffset = zone.getOffset(lastDate);
    for (long currentDate = endDate; currentDate >= startDate; currentDate -= GROSS_PERIOD) {
      int currentOffset = zone.getOffset(currentDate);
      if (currentOffset != lastOffset) { // Binary Search
        if (currentOffset < minOffset)
          minOffset = currentOffset;
        if (currentOffset > maxOffset)
          maxOffset = currentOffset;
        long low = currentDate;
        int lowOffset = currentOffset;
        long high = lastDate;
        int highOffset = lastOffset;
        while (high - low > EPSILON) {
          long mid = (high + low) / 2;
          mid = (mid / EPSILON) * EPSILON; // round to nearest possible point
          if (mid <= low)
            mid += EPSILON;
          int midOffset = zone.getOffset(mid);
          if (midOffset == lowOffset) {
            low = mid;
          } else {
            high = mid;
          }
        }

        // System.out.println("\tAdding*: " + dtf.format(new Date(low)));
        inflectionPoints.add(new InflectionPoint(high, highOffset));
        lastInflection = low;
      }
      lastOffset = currentOffset;
      lastDate = currentDate;
    }
    // System.out.println("\tAdding: " + dtf.format(new Date(startDate)));
    inflectionPoints.add(new InflectionPoint(startDate, zone
        .getOffset(startDate)));
  }

  public int compareTo(ZoneInflections other, OutputLong mostRecentDateTime) {
    mostRecentDateTime.value = 0;
    if (other == null) {
      mostRecentDateTime.value = get(0).utcDateTime;
      return 1;
    }
    ZoneInflections that = (ZoneInflections) other;
    int minLength = inflectionPoints.size();
    if (minLength < that.inflectionPoints.size())
      minLength = that.inflectionPoints.size();
    for (int i = 0; i < minLength; ++i) {
      InflectionPoint ip1 = get(i);
      InflectionPoint ip2 = that.get(i);
      if (ip1.offset == ip2.offset && ip1.utcDateTime == ip2.utcDateTime)
        continue;
      if (ip1.offset != ip2.offset) {
        // back up a bit
        mostRecentDateTime.value = Math.max(ip1.utcDateTime, ip2.utcDateTime)
            - EPSILON;
      } else if (ip1.utcDateTime > ip2.utcDateTime) {
        // offsets are the same, but start times are different
        // in that case, find the next inflection point for the shorter one.
        mostRecentDateTime.value = ip1.utcDateTime - EPSILON;
        ip1 = get(i + 1);
      } else {
        // ditto but reversed
        mostRecentDateTime.value = ip2.utcDateTime - EPSILON;
        ip2 = that.get(i + 1);
      }
      return ip1.offset > ip2.offset ? 1 : ip1.offset < ip2.offset ? -1 : 0;
    }
    mostRecentDateTime.value = Long.MIN_VALUE;
    return 0;
  }

  InflectionPoint get(int i) {
    return (InflectionPoint) inflectionPoints.get(i);
  }

  int size() {
    return inflectionPoints.size();
  }

  private transient OutputLong temp = new OutputLong(0);

  public int compareTo(Object o) {
    return compareTo((ZoneInflections) o, temp);
  }

  public static class InflectionPoint implements Comparable {
    static final long NONE = Long.MIN_VALUE;

    public long utcDateTime;

    public int offset;

    public String toString() {
      return ICUServiceBuilder.isoDateFormat(new Date(utcDateTime)) + ";"
          + formatHours((int) offset);
    }

    public InflectionPoint(long utcDateTime, int offset) {
      this.utcDateTime = utcDateTime;
      this.offset = offset;
    }

    /*
     * public long mostRecentDifference(InflectionPoint other) { InflectionPoint
     * that = (InflectionPoint) other; if (utcDateTime != that.utcDateTime ||
     * offset != that.offset) { return Math.max(utcDateTime, that.utcDateTime); }
     * return NONE; }
     */
    public int compareTo(Object o) {
      InflectionPoint that = (InflectionPoint) o;
      if (utcDateTime < that.utcDateTime)
        return -1;
      if (utcDateTime > that.utcDateTime)
        return 1;
      if (offset < that.offset)
        return -1;
      if (offset > that.offset)
        return 1;
      return 0;
    }
  }

  public static long getDateLong(int year, int month, int day) {
    return new Date(year - 1900, month - 1, day).getTime();
  }

  static private final NumberFormat nf = NumberFormat.getInstance(ULocale.US);

  static public String formatHours(int hours) {
    return nf.format(hours / ZoneInflections.DHOUR);
  }

  public static class OutputLong implements Comparable {
    public long value;

    public OutputLong(long value) {
      this.value = value;
    }

    public int compareTo(Object o) {
      OutputLong that = (OutputLong) o;
      return value < that.value ? -1 : value > that.value ? 1 : 0;
    }
  }

  /**
   * Return the inflection points during a given period. The points are copies
   * of what is in the data, so it is safe to modify them
   * 
   * @param inflections2
   * @param time
   * @param time2
   * @return
   */
  public List<InflectionPoint> getInflectionPoints(long start, long end) {
    List<InflectionPoint> results = new ArrayList<InflectionPoint>();
    for (InflectionPoint inflectionPoint : inflectionPoints) {
      if (inflectionPoint.utcDateTime <= start) {
        results.add(new InflectionPoint(start, inflectionPoint.offset));
        break;
      } else if (inflectionPoint.utcDateTime < end) {
        results.add(new InflectionPoint(inflectionPoint.utcDateTime,
            inflectionPoint.offset));
      }
    }
    return results;
  }
}