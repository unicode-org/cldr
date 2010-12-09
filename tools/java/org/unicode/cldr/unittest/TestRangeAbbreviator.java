package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.Date;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.RangeAbbreviator;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestRangeAbbreviator {
  static final UnicodeSet DIGITS = new UnicodeSet("[:nd:]");
  
  public static void main(String[] args) throws IOException {
    Log.setLog(CldrUtility.GEN_DIRECTORY + "datePeriodTest.txt");
    ULocale[] locales = ULocale.getAvailableLocales();
    Date[] tests = { 
        new Date(107,9,15, 13, 45, 45), 
        new Date(107,9,15, 13, 45, 46), 
        new Date(107,9,15, 13, 46, 45), 
        new Date(107,9,15, 14, 45, 45), 
        new Date(107,9,16, 13, 45, 45), 
        new Date(107,10,15, 13, 45, 45), 
        new Date(108,9,16, 13, 45, 45), 
    };
    for (ULocale locale : locales) {
      if (locale.getCountry().length() != 0) {
        continue;
      }
      DateFormat dayOfWeekFormat = new SimpleDateFormat("E", locale);
      String testDayOfWeek = dayOfWeekFormat.format(tests[0]);
      if (DIGITS.containsAll(testDayOfWeek)) {
        System.out.println("Problem in locale: " + locale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")");
        System.out.println("\tDay of week not translated. ");
        continue;
      }
      
      Log.logln(locale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")");
      
      final String separator1 = " - ";
      RangeAbbreviator rangeAbbreviator = new RangeAbbreviator(locale, separator1);
      for (int dateStyle = -1; dateStyle <= DateFormat.SHORT; ++dateStyle) {
        for (int timeStyle = -1; timeStyle <= DateFormat.SHORT; ++timeStyle) {
          if (timeStyle == DateFormat.FULL || timeStyle == DateFormat.LONG|| timeStyle == DateFormat.MEDIUM || dateStyle == -1 && timeStyle == -1) {
            continue;
          }
          DateFormat dtf = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
          String formatted1 = dtf.format(tests[0]);
          String styleString = ("date:" + STYLE_NAMES[dateStyle+1])
          + (", time:" + STYLE_NAMES[timeStyle+1]);
          
          for (Date test : tests) {
            String formatted2 = dtf.format(test);
            if (formatted1.equals(formatted2)) {
              continue;
            }
            String range = rangeAbbreviator.abbreviate(formatted1, formatted2);
            rangeAbbreviator.abbreviate(formatted1, formatted2); // for debugging
            if (range.equals(formatted1 + separator1 + formatted2)) {
              continue; // no shortening
            }
            Log.logln(styleString + "\t\"" + range  + "\"\t\t<=\t\t\"" + formatted1 + "\"\t+\t\"" + formatted2 + "\"");
          }
        }
      }
      Log.logln("");
    }
  }
  static final String[] STYLE_NAMES = {"None", "Full", "Long", "Medium", "Short"};
}