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
  public static int DAY_LIMIT = 24*60*60*1000;
  public enum DayPeriod {am, pm, weeHours, earlyMorning, morning, lateMorning, noon, midDay, afternoon, lateAfternoon, earlyEvening, evening, lateEvening, earlyNight, night};
  
  // the starts must be in sorted order. First must be zero. Last must be < DAY_LIMIT
  // each of these will have the same length, and correspond.
  private int[] starts;
  private boolean[] includesStart;
  private DayPeriodInfo.DayPeriod[] periods;
  
  public static class Builder {
    TreeMap<Row.R2<Integer,Integer>,Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>> info 
    = new TreeMap<Row.R2<Integer,Integer>,Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod>>();
    
    public DayPeriodInfo.Builder add(DayPeriodInfo.DayPeriod dayPeriod, int start, boolean includesStart, int end, boolean includesEnd) {
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
      if (len == 0) {
        return result;
      }
      result.starts = new int[len];
      result.includesStart = new boolean[len];
      result.periods = new DayPeriodInfo.DayPeriod[len];
      int i = 0;
      int lastFinish = 0;
      boolean lastFinishIncluded = false;
      for (Row.R2<Integer,Integer> start : info.keySet()) {
        result.starts[i] = start.get0();
        result.includesStart[i] = start.get1() == 0;
        if (lastFinish != result.starts[i] || lastFinishIncluded == result.includesStart[i]) {
          throw new IllegalArgumentException("Gap or overlapping times: " 
                  + formatTime(start.get0()) + "\t..\t" + formatTime(start.get1()) + "\t" + formatTime(lastFinish) + "\t" + lastFinishIncluded
                  + "\t" + Arrays.asList(locales));
        }
        Row.R3<Integer, Boolean, DayPeriodInfo.DayPeriod> row = info.get(start);
        lastFinish = row.get0();
        lastFinishIncluded = row.get1();
        result.periods[i++] = row.get2();
      }
      if (result.starts[0] != 0 || result.includesStart[0] != true || lastFinish != DAY_LIMIT || lastFinishIncluded != false) {
        throw new IllegalArgumentException("Doesn't cover 0:00).");
      }
      info.clear();
      return result;
    }
  }
  /** 
   * Return the start (in millis) of the first matching day period, or -1 if no match,
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
   * Returns the day period for the time. 
   * @param millisInDay If not (millisInDay > 0 && The millisInDay < DAY_LIMIT) throws exception.
   * @return corresponding day period
   */
  public DayPeriodInfo.DayPeriod getDayPeriod(int millisInDay) {
    if (millisInDay < 0) {
      throw new IllegalArgumentException("millisInDay too small");
    }
    for (int i = 1; i < starts.length; ++i) {
      int start = starts[i];
      if (start == millisInDay && includesStart[i]) {
        return periods[i];
      }
      if (start < millisInDay) {
        return periods[i-1];
      }
    }
    if (millisInDay < 24*60*60*1000) {
      return periods[periods.length - 1];
    } else {
      throw new IllegalArgumentException("millisInDay too big");
    }
  }
  
  /**
   * Returns the number of periods in the day
   * @return
   */
  public int getPeriodCount() {
    return starts.length;
  }
  /**
   * For the nth period in the day, returns the start, whether the start is included, and the period ID.
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
  
  static String formatTime(int time) {
    int minutes = time/(60*1000);
    int hours = minutes/60;
    minutes -= hours*60;
    return String.format("%02d:%02d", hours, minutes);
  }
}