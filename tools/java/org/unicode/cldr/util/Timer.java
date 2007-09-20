package org.unicode.cldr.util;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public final class Timer {
  long startTime;
  long duration;
  {
    start();
  }
  public void start() {
    startTime = System.currentTimeMillis();
    duration = Long.MIN_VALUE;
  }
  public long getDuration() {
    if (duration == Long.MIN_VALUE) {
      duration = System.currentTimeMillis() - startTime;
    }
    return duration;
  }
  public void stop() {
    getDuration();
  }
  public String toString() {
    return nf.format(getDuration()) + "ms";
  }
  public String toString(Timer other) {
    return toString() + " (" + pf.format((double)getDuration()/other.getDuration()) + ")";
  }
  static  NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
  static  NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);
  static {
    pf.setMaximumFractionDigits(3);
  }
}