package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleGenerator {
  public enum Zoomed {OUT, IN};
  final double NUMBER_SAMPLE = 12345.6789;
  final static TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
  final static Date DATE_SAMPLE;
  static {
    Calendar c = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
    c.set(1999,8,14,13,25,59); // 1999-09-13 13:25:59
    DATE_SAMPLE = c.getTime();
  }
  
  CLDRFile resolvedCLDRFile;
//  Matcher skipMatcher = Pattern.compile(
//      "/localeDisplayNames(?!"
//      ).matcher("");
  XPathParts parts = new XPathParts();
  ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

  public ExampleGenerator(CLDRFile resolvedCLDRFile) {
    this.resolvedCLDRFile = resolvedCLDRFile;
    icuServiceBuilder.setCldrFile(resolvedCLDRFile);
  }

  /**
   * Returns an example if there is one for this path, otherwise null. For use
   * in the survey tool, an example might be returned *even* if there is no value
   * in the locale. For example, the locale might have a path that Engish
   * doesn't, but you want to return the best English example.
   * 
   * @param xpath
   * @param zoomed TODO
   * @return
   */
  public String getExample(String xpath, String value, Zoomed zoomed) {
    if (xpath.contains("/localeDisplayNames")) {
      if (xpath.contains("/codePatterns")) {
        parts.set(xpath);
        return MessageFormat.format(value,new Object[]{"CODE"});
      }
      return null;
    }
    parts.set(xpath);
    if (parts.contains("currency") && parts.contains("symbol")) {
      String currency = parts.getAttributeValue(-2,"type");
      DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency);
      return x.format(12345.6789);
    }
    if (parts.contains("pattern")) {
      if (parts.contains("calendar")) {
        SimpleDateFormat x = icuServiceBuilder.getDateFormat(parts.findAttributeValue("calendar", "type"), value);
        x.setTimeZone(ZONE_SAMPLE);
        return x.format(DATE_SAMPLE);
      } else if (parts.contains("numbers")) {
        DecimalFormat x = icuServiceBuilder.getNumberFormat(value);
        return x.format(NUMBER_SAMPLE);
      }
    }
    if (parts.contains("symbol")) {
      DecimalFormat x = icuServiceBuilder.getNumberFormat(2);
      return x.format(NUMBER_SAMPLE);
    }
    return null;
  }
  
  /**
   * Return a help string, in html, that should be shown in the Zoomed view.
   * Presumably at the end of each help section is something like:
   * <br> &lt;br&gt;For more information, see <a href='http://unicode.org/cldr/wiki?SurveyToolHelp/characters'>help</a>.
   * <br>TODO: add more help, and modify to get from property or xml file for easy modification.
   * @return null if none available.
   */
  public String getHtmlHelp(String xpath, String value) {
    if (xpath.contains("/exemplarCharacters")) {
      return "The standard exemplar characters are those used in customary writing ([a-z] for English; " +
          "the auxiliary characters are used in foreign words found in typical magazines, newspapers, etc.; " +
          "currency auxilliary characters are those used in currency symbols, like 'US$ 1,234'. ";
    }
    return null;
  }
}