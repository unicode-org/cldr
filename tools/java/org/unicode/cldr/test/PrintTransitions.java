package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Pair;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.BasicTimeZone;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Verify that all zones in a metazone have the same behavior within the
 * specified period.
 * 
 * 
 */
public class PrintTransitions {
  
  public static void main(String[] args) throws IOException {
    TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT"));

    String [] idList = TimeZone.getAvailableIDs();

    int i=0;
    Date now = new Date();
    while (i < idList.length) {

       BasicTimeZone tz = (BasicTimeZone) TimeZone.getTimeZone(idList[i]);
       TimeZoneTransition tzt;
       SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
       long TransitionTime = 0;
       tzt = tz.getNextTransition(TransitionTime,true);
       System.out.println();
       System.out.println(idList[i]);
       System.out.println("---------------------");
       int transitionCount = 0;
       while ( tzt != null && (TransitionTime < now.getTime())) {
          TransitionTime = tzt.getTime();
          StringBuffer result = new StringBuffer();
          FieldPosition pos = new FieldPosition(0);
          Calendar cal = fmt.getCalendar();
          cal.setTimeInMillis(TransitionTime);
          fmt.format(cal,result,pos);
          System.out.print(result.toString());
          TimeZoneRule from = tzt.getFrom();
          TimeZoneRule to = tzt.getTo();
          int fromOffset = (from.getRawOffset() + from.getDSTSavings()) / 3600000;
          int fromMins = ((from.getRawOffset() + from.getDSTSavings()) % 3600000) / 60000;
          if (fromMins < 0)
             fromMins *= -1;
          int toOffset = (to.getRawOffset() + to.getDSTSavings()) / 3600000;
          int toMins = ((to.getRawOffset() + to.getDSTSavings()) % 3600000) / 60000;
          if (toMins < 0)
             toMins *= -1;
          System.out.println(" from "+from.getName()+"[GMT"+fromOffset+":"+fromMins+"] to "+to.getName()+"[GMT"+toOffset+":"+toMins+"]");
          transitionCount++;
          tzt = tz.getNextTransition(TransitionTime,false);
       }
       if (transitionCount == 0) {
          TimeZoneRule [] rules = tz.getTimeZoneRules(0);
          TimeZoneRule from = rules[0];
          int fromOffset = (from.getRawOffset() + from.getDSTSavings()) / 3600000;
          int fromMins = ((from.getRawOffset() + from.getDSTSavings()) % 3600000) / 60000;
          if (fromMins < 0)
             fromMins *= -1;
          System.out.println(from.getName()+"[GMT"+fromOffset+":"+fromMins+"]");
       }
       i++;
    }
  }
  
}
