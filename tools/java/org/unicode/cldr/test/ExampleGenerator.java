package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleGenerator {
  private final static boolean DEBUG_SHOW_HELP = false;
  
  public enum Zoomed {
    /** For the zoomed-out view. */
    OUT,
    /** For the zoomed-in view */
    IN
  };
  
  private final static boolean CACHING = false;
  
  public final static double NUMBER_SAMPLE = 12345.6789;
  
  public final static TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
  
  public final static Date DATE_SAMPLE;
  
  private final static Date DATE_SAMPLE2;
  
  private final static String EXEMPLAR_CITY = "Europe/Rome";
  
  private String backgroundStart = "<span class='substituted'>";
  
  private String backgroundEnd = "</span>";
  
  static {
    Calendar c = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
    c.set(1999, 8, 14, 13, 25, 59); // 1999-09-13 13:25:59
    DATE_SAMPLE = c.getTime();
    c.set(1999, 9, 27, 13, 25, 59); // 1999-09-13 13:25:59
    DATE_SAMPLE2 = c.getTime();
  }
  
  private Collator col;
  
  private CLDRFile cldrFile;
  
  private Map<String, String> cache = new HashMap();
  
  private static final String NONE = "\uFFFF";
  
  private static final String backgroundStartSymbol = "\uE1234";
  
  private static final String backgroundEndSymbol = "\uE1235";
  
  // Matcher skipMatcher = Pattern.compile(
  // "/localeDisplayNames(?!"
  // ).matcher("");
  private XPathParts parts = new XPathParts();
  
  private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
  
  public String getBackgroundEnd() {
    return backgroundEnd;
  }
  
  /**
   * For setting the end of the "background" style. Default is "</span>". It is
   * used in composing patterns, so it can show the part that corresponds to the
   * value.
   * 
   * @return
   */
  public void setBackgroundEnd(String backgroundEnd) {
    this.backgroundEnd = backgroundEnd;
  }
  
  public String getBackgroundStart() {
    return backgroundStart;
  }
  
  /**
   * For setting the "background" style. Default is "<span
   * style='background-color: gray'>". It is used in composing patterns, so it
   * can show the part that corresponds to the value.
   * 
   * @return
   */
  public void setBackgroundStart(String backgroundStart) {
    this.backgroundStart = backgroundStart;
  }
  
  public ExampleGenerator(CLDRFile resolvedCLDRFile) {
    this.cldrFile = resolvedCLDRFile;
    icuServiceBuilder.setCldrFile(resolvedCLDRFile);
    col = Collator.getInstance(new ULocale(resolvedCLDRFile.getLocaleID()));
  }
  
  /**
   * Returns an example string, in html, if there is one for this path,
   * otherwise null. For use in the survey tool, an example might be returned
   * *even* if there is no value in the locale. For example, the locale might
   * have a path that Engish doesn't, but you want to return the best English
   * example. <br>
   * The result is valid HTML.
   * 
   * @param xpath
   * @param zoomed
   * @return
   */
  public String getExampleHtml(String xpath, String value, Zoomed zoomed) {
    if (value == null) {
      return null; // for now
    }
    String result = null;
    String cacheKey;
    if (CACHING) {
      cacheKey = xpath + "," + value + "," + zoomed;
      result = cache.get(cacheKey);
      if (result != null) {
        if (result == NONE) {
          return null;
        }
        return result;
      }
    }
    // result is null at this point. Get the real value if we can.
    
    main: {
      parts.set(xpath);
      if (parts.contains("dateRangePattern")) { // {0} - {1}
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        result = MessageFormat.format(value, new Object[] { setBackground(dateFormat.format(DATE_SAMPLE)), setBackground(dateFormat.format(DATE_SAMPLE2)) });
        result = finalizeBackground(result);
        break main;
      }
      if (parts.contains("timeZoneNames")) {
        if (parts.contains("regionFormat")) { // {0} Time
          String sampleTerritory = cldrFile.getName(CLDRFile.TERRITORY_NAME, "JP", false);
          result = MessageFormat.format(value, new Object[] { setBackground(sampleTerritory) });
        } else if (parts.contains("fallbackFormat")) { // {1} ({0})
          String timeFormat = setBackground(cldrFile.getStringValue("//ldml/dates/timeZoneNames/regionFormat"));
          String us = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "US", false));
          // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity
          // = Λος Άντζελες
          String LosAngeles = setBackground(cldrFile.getStringValue("//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity"));
          result = MessageFormat.format(value, new Object[] { LosAngeles, us });
          result = MessageFormat.format(timeFormat, new Object[] { result });
        } else if (parts.contains("gmtFormat")) { // GMT{0}
          result = getGMTFormat(null, value);
        } else if (parts.contains("hourFormat")) { // +HH:mm;-HH:mm
          result = getGMTFormat(value, null);
        }
        result = finalizeBackground(result);
        break main;
      }
      if (xpath.contains("/exemplarCharacters")) {
        if (zoomed == Zoomed.IN) {
          UnicodeSet unicodeSet = new UnicodeSet(value);
          if (unicodeSet.size() < 500) {
            result = CollectionUtilities.prettyPrint(unicodeSet, false, null, null, col, col);
          }
        }
        break main;
      }
      if (xpath.contains("/localeDisplayNames")) {
        if (xpath.contains("/codePatterns")) {
          parts.set(xpath);
          result = MessageFormat.format(value, new Object[] { setBackground("CODE") });
          result = finalizeBackground(result);
        } else if (xpath.contains("_") && xpath.contains("/languages")) {
          result = cldrFile.getName(parts.set(xpath).findAttributeValue("language", "type"), false);
        }
        break main;
      }
      if (parts.contains("currency") && parts.contains("symbol")) {
        String currency = parts.getAttributeValue(-2, "type");
        String fullPath = cldrFile.getFullXPath(xpath,false);
        if (fullPath != null && fullPath.contains("[@choice=\"true\"]")) {
          ChoiceFormat cf = new ChoiceFormat(value);
          value = cf.format(NUMBER_SAMPLE);
        }
        // TODO fix to use value!!
        DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency, value);
        result = x.format(NUMBER_SAMPLE);
        result = setBackground(result).replace(value, backgroundEndSymbol + value + backgroundStartSymbol);
        result = finalizeBackground(result);
        break main;
      }
      if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
        if (parts.contains("calendar")) {
          String calendar = parts.findAttributeValue("calendar", "type");
          SimpleDateFormat dateFormat;
          if (parts.contains("dateTimeFormat")) {
            SimpleDateFormat date2 = icuServiceBuilder.getDateFormat(calendar, 2, 0); // date
            SimpleDateFormat time = icuServiceBuilder.getDateFormat(calendar, 0, 2); // time
            date2.applyPattern(MessageFormat.format(value, new Object[] { setBackground(time.toPattern()), setBackground(date2.toPattern()) }));
            dateFormat = date2;
          } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id)) {
              break main;
            } else {
              dateFormat = icuServiceBuilder.getDateFormat(calendar, value);
            }
          }
          dateFormat.setTimeZone(ZONE_SAMPLE);
          result = dateFormat.format(DATE_SAMPLE);
          result = finalizeBackground(result);
        } else if (parts.contains("numbers")) {
          DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(value);
          result = numberFormat.format(NUMBER_SAMPLE);
        }
        break main;
      }
      if (parts.contains("symbol")) {
        DecimalFormat x = icuServiceBuilder.getNumberFormat(2);
        result = x.format(NUMBER_SAMPLE);
        break main;
      }
    }
    if (CACHING) {
      if (result == null) {
        cache.put(cacheKey, NONE);
      } else {
        // fix HTML, cache
        result = TransliteratorUtilities.toHTML.transliterate(result);
        cache.put(cacheKey, result);
      }
    }
    return result;
  }
  
  /**
   * Put a background on an item, skipping enclosed patterns.
   * 
   * @param sampleTerritory
   * @return
   */
  private String setBackground(String inputPattern) {
    Matcher m = PARAMETER.matcher(inputPattern);
    return backgroundStartSymbol + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol) + backgroundEndSymbol;
  }
  
  private String finalizeBackground(String input) {
    return input == null ? input : input.replace(backgroundStartSymbol + backgroundEndSymbol, "") // remove
        // null
        // runs
        .replace(backgroundEndSymbol + backgroundStartSymbol, "") // remove null
        // runs
        .replace(backgroundStartSymbol, backgroundStart).replace(backgroundEndSymbol, backgroundEnd);
  }
  
  static final Pattern PARAMETER = Pattern.compile("(\\{[0-9]\\})");
  
  private String getGMTFormat(String gmtHourString, String gmtFormat) {
    if (gmtFormat == null) {
      gmtFormat = setBackground(cldrFile.getStringValue("//ldml/dates/timeZoneNames/gmtFormat"));
    }
    boolean hoursBackground = false;
    if (gmtHourString == null) {
      hoursBackground = true;
      gmtHourString = cldrFile.getStringValue("//ldml/dates/timeZoneNames/hourFormat");
    }
    String[] plusMinus = gmtHourString.split(";");
    SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", plusMinus[0]);
    dateFormat.setTimeZone(ZONE_SAMPLE);
    String hours = dateFormat.format(DATE_SAMPLE);
    if (hoursBackground) {
      hours = setBackground(hours);
    }
    String result = MessageFormat.format(gmtFormat, new Object[] { hours });
    return result;
  }
  
  /**
   * Return a help string, in html, that should be shown in the Zoomed view.
   * Presumably at the end of each help section is something like: <br>
   * &lt;br&gt;For more information, see <a
   * href='http://unicode.org/cldr/wiki?SurveyToolHelp/characters'>help</a>.
   * <br>
   * The result is valid HTML. <br>
   * TODO: add more help, and modify to get from property or xml file for easy
   * modification.
   * 
   * @return null if none available.
   */
  public String getHelpHtml(String xpath, String value) {
    if (helpMessages == null) {
      helpMessages = new HelpMessages();
    }
    return helpMessages.find(xpath);
//  if (xpath.contains("/exemplarCharacters")) {
//  result = "The standard exemplar characters are those used in customary writing ([a-z] for English; "
//  + "the auxiliary characters are used in foreign words found in typical magazines, newspapers, &c.; "
//  + "currency auxilliary characters are those used in currency symbols, like 'US$ 1,234'. ";
//  }
//  return result == null ? null : TransliteratorUtilities.toHTML.transliterate(result);
  }
  
  HelpMessages helpMessages;
  
  private static class HelpMessages {
    List<Matcher> keys = new ArrayList();
    List<String> values = new ArrayList();
    
    enum Status {
      BASE, BEFORE_CELL, IN_CELL, IN_INSIDE_TABLE
    };
    
    StringBuilder[] currentColumn = new StringBuilder[2];
    int column = 0;
    
    HelpMessages() {
      currentColumn[0] = new StringBuilder();
      currentColumn[1] = new StringBuilder();
      BufferedReader in;
      try {
        in = Utility.getUTF8Data("test_help_messages.html");
        Status status = Status.BASE;
        int count = 0;
        int tableCount = 0;
        while (true) {
          String line = in.readLine();
          count++;
          if (line == null)
            break;
          line = line.trim();
          // watch for tr, then td. Pick up following strings.
          switch (status) {
            case BASE:
              if (line.equals("<tr>")) {
                status = Status.BEFORE_CELL;
              }
              break;
            case BEFORE_CELL:
              if (line.equals("</tr>")) {
                addHelpMessages();
                status = Status.BASE;
                break;
              }
              if (line.startsWith("<td>")) {
                status = Status.IN_CELL;
                line = line.substring(4);
              }
              // fall through
            case IN_CELL:
              boolean done = false;
              if (line.startsWith("<table")) {
                tableCount++;
                appendLine(line);
                status = Status.IN_INSIDE_TABLE;
              } else {
                if (line.endsWith("</td>")) {
                  line = line.substring(0,line.length()-5);
                  status = Status.BEFORE_CELL;
                  done = true;
                }
                appendLine(line);
                if (done) column++;
              }
              break;
            case IN_INSIDE_TABLE:
              appendLine(line);
              if (line.startsWith("<table")) {
                tableCount++;
              } else if (line.equals("</table>")) {
                tableCount--;
                if (tableCount == 0) {
                  status = Status.IN_CELL;
                }
              }
              break;
          }
        }
        in.close();
      } catch (IOException e) {
        System.err.println("Can't initialize help text");
      }
    }

    private void appendLine(String line) {
      if (line.length() != 0) {
        if (currentColumn[column].length() > 0) {
          currentColumn[column].append(" ");
        }
        currentColumn[column].append(line);
      }
    }
    
    public String find(String xpath) {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < keys.size(); ++i) {
        if (keys.get(i).reset(xpath).matches()) {
          if (result.length() != 0) {
            result.append("\r\n");
          }
          result.append(values.get(i));
        }
      }
      if (result.length() != 0) {
        return result.toString();
      }
      return null;
    }
    
    private void addHelpMessages() {
      if (DEBUG_SHOW_HELP) {
        System.out.println(currentColumn[0].toString() + " => " + currentColumn[1].toString());
      }
      if (column == 2) { // must have two columns
        try {
          Matcher m = Pattern.compile(TransliteratorUtilities.fromHTML.transliterate(currentColumn[0].toString()), Pattern.COMMENTS).matcher("");
          keys.add(m);
          values.add(currentColumn[1].toString());
        } catch (RuntimeException e) {
          System.err.println("Help file has illegal regex: " + currentColumn[0]);
        }
      }
      currentColumn[0].setLength(0);
      currentColumn[1].setLength(0);
      column = 0;
    }
  }
}