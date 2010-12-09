package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SupplementalData;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Class to generate examples and help messages for the Survey tool (or console version).
 * @author markdavis
 *
 */
public class ExampleGenerator {
  private final static boolean DEBUG_SHOW_HELP = false;

  private static SupplementalDataInfo supplementalDataInfo;
  private SupplementalData supplementalData;
  
  /**
   * Zoomed status.
   * @author markdavis
   *
   */
  public enum Zoomed {
    /** For the zoomed-out view. */
    OUT,
    /** For the zoomed-in view */
    IN
  };

  private final static boolean CACHING = false;

  public final static double NUMBER_SAMPLE = 12345.6789;

  public final static TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
  public final static TimeZone GMT_ZONE_SAMPLE = TimeZone.getTimeZone("Etc/GMT");

  public final static Date DATE_SAMPLE;

  private final static Date DATE_SAMPLE2;

  //private final static String EXEMPLAR_CITY = "Europe/Rome";

  private String backgroundStart = "<span class='substituted'>";

  private String backgroundEnd = "</span>";
  
  private boolean verboseErrors = false;

  private Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);

  static {
    Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
    calendar.set(1999, 8, 14, 13, 25, 59); // 1999-09-13 13:25:59
    DATE_SAMPLE = calendar.getTime();
    calendar.set(1999, 9, 27, 13, 25, 59); // 1999-09-13 13:25:59
    DATE_SAMPLE2 = calendar.getTime();
  }

  private Collator col;

  private CLDRFile cldrFile;

  private Map<String, String> cache = new HashMap();

  private static final String NONE = "\uFFFF";

  private static final String backgroundStartSymbol = "\uE234";

  private static final String backgroundEndSymbol = "\uE235";

  // Matcher skipMatcher = Pattern.compile(
  // "/localeDisplayNames(?!"
  // ).matcher("");
  private XPathParts parts = new XPathParts();

  private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

  private Set<String> singleCountryZones;

  /**
   * For getting the end of the "background" style. Default is "</span>". It is
   * used in composing patterns, so it can show the part that corresponds to the
   * value.
   * @return
   */
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

  /**
   * For getting the "background" style. Default is "<span
   * style='background-color: gray'>". It is used in composing patterns, so it
   * can show the part that corresponds to the value.
   * 
   * @return
   */
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
  
  /**
   * Set the verbosity level of internal errors. 
   * For example, setVerboseErrors(true) will cause 
   * full stack traces to be shown in some cases.
   */
  public void setVerboseErrors(boolean verbosity) {
    this.verboseErrors = verbosity;
  }

  /**
   * Create an Example Generator. If this is shared across threads, it must be synchronized.
   * @param resolvedCldrFile
   * @param supplementalDataDirectory
   */
  public ExampleGenerator(CLDRFile resolvedCldrFile, String supplementalDataDirectory) {
    cldrFile = resolvedCldrFile.getResolved();
    icuServiceBuilder.setCldrFile(cldrFile);
    col = Collator.getInstance(new ULocale(cldrFile.getLocaleID()));
    synchronized (ExampleGenerator.class) {
      if (supplementalDataInfo == null) {
        supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDataDirectory);
      }
      supplementalData = new SupplementalData(supplementalDataDirectory);
    }
    String singleCountriesPath = cldrFile.getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
    if(singleCountriesPath == null) {
        System.err.println("Failure: in "+cldrFile.getLocaleID()+" examplegenerator- cldrFile.getFullXPath(//ldml/dates/timeZoneNames/singleCountries)==null");
    } else {
        parts.set(singleCountriesPath);
        singleCountryZones = new HashSet(Arrays.asList(parts.getAttributeValue(-1, "list").trim().split("\\s+")));
    }
  }

  public enum ExampleType {NATIVE, ENGLISH};
  
  public static class ExampleContext {
    private List<Double> exampleCount;

    public void setExampleCount(List<Double> exampleCount2) {
      this.exampleCount = exampleCount2;
    }

    public List<Double> getExampleCount() {
      return exampleCount;
    }
  }
  
  public String getExampleHtml(String xpath, String value, Zoomed zoomed) {
    return getExampleHtml(xpath, value, zoomed, null, null);
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
   * @param zoomed status (IN, or OUT) Out is a longer version only called in Zoom mode. IN is called in both.
   * @return
   */
  public String getExampleHtml(String xpath, String value, Zoomed zoomed, ExampleContext context, ExampleType type) {
    String cacheKey;
    String result = null;
    try {
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
      parts.set(xpath);
      if (parts.contains("dateRangePattern")) { // {0} - {1}
        return result = handleDateRangePattern(value, xpath, zoomed);
      }
      if (parts.contains("timeZoneNames")) {
        return result = handleTimeZoneName(xpath, value);
      }
      if (parts.contains("exemplarCharacters")) {
        return result = handleExemplarCharacters(value, zoomed);
      }
      if (parts.contains("localeDisplayNames")) {
        return result = handleDisplayNames(xpath, parts, value);
      }
      if (parts.contains("currency")) {
        return result = handleCurrency(xpath, value, context, type);
      }
      if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
        return result = handleDateFormatItem(value);
      }
      if (parts.contains("symbol")) {
        return result = handleNumberSymbol();
      }
      if (parts.contains("units")) {
        return result = handleUnits(parts, xpath, value, context, type);
      }
      if (parts.contains("currencyFormats") && parts.contains("unitPattern")) {
        return result = formatCountValue(xpath, parts, value, context, type);
      } 
      if (parts.contains("intervalFormats")) {
        return result = handleIntervalFormats(parts, xpath, value, context, type);
      }
      
      // didn't detect anything, return empty-handed
      return null;
      
    } catch (NullPointerException e) {
      return null;
    } catch (RuntimeException e) {
      String unchained = verboseErrors?("<br>"+unchainException(e)):"";
      return zoomed == Zoomed.OUT 
          ? "<i>internal error</i>"
          : /*TransliteratorUtilities.toHTML.transliterate*/("<i>internal error: " + e.getClass().getName() + ", " + e.getMessage() + "</i>"+unchained);
    } finally {
      if (CACHING) {
        if (result == null) {
          cache.put(cacheKey, NONE);
        } else {
          // fix HTML, cache
          result = TransliteratorUtilities.toHTML.transliterate(result);
          cache.put(cacheKey, result);
        }
      }      
    }
  }

  IntervalFormat intervalFormat = new IntervalFormat();
  
  static Calendar generatingCalendar = Calendar.getInstance(ULocale.US);
  
  private static Date getDate(int year, int month, int date, int hour, int minute, int second, TimeZone zone) {
    generatingCalendar.setTimeZone(GMT_ZONE_SAMPLE);
    generatingCalendar.set(year, month, date, hour, minute, second);
    return generatingCalendar.getTime();
  }

  static Date FIRST_INTERVAL = getDate(2008,1,13,5,7,9, GMT_ZONE_SAMPLE);
  static Map<String,Date> SECOND_INTERVAL = CldrUtility.asMap(new Object[][]{
          {"y", getDate(2009,2,14,17,8,10, GMT_ZONE_SAMPLE)},
          {"M", getDate(2008,2,14,17,8,10, GMT_ZONE_SAMPLE)},
          {"d", getDate(2008,1,14,17,8,10, GMT_ZONE_SAMPLE)},
          {"a", getDate(2008,1,13,17,8,10, GMT_ZONE_SAMPLE)},
          {"h", getDate(2008,1,13,6,8,10, GMT_ZONE_SAMPLE)},
          {"m", getDate(2008,1,13,5,8,10, GMT_ZONE_SAMPLE)}
          });


  private String handleIntervalFormats(XPathParts parts2, String xpath, String value,
          ExampleContext context, ExampleType type) {
    if (!parts2.getAttributeValue(3, "type").equals("gregorian")) {
      return null;
    }
    if (parts2.getElement(6).equals("intervalFormatFallback")) {
      return null; // TODO test this too
    }
    String greatestDifference = parts2.getAttributeValue(-1, "id");

    // intervalFormatFallback
    // //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="yMd"]/greatestDifference[@id="y"]
    // find where to split the value
    intervalFormat.setPattern(value);
    return intervalFormat.format(FIRST_INTERVAL, SECOND_INTERVAL.get(greatestDifference));
  }

  class IntervalFormat {
    DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
    SimpleDateFormat firstFormat = new SimpleDateFormat();
    SimpleDateFormat secondFormat = new SimpleDateFormat();
    StringBuilder first = new StringBuilder();
    StringBuilder second = new StringBuilder();
    BitSet letters = new BitSet();

    public String format(Date earlier, Date later) {
      return firstFormat.format(earlier) + secondFormat.format(later);
    }
    
    public IntervalFormat setPattern(String pattern) {
      formatParser.set(pattern);
      first.setLength(0);
      second.setLength(0);
      boolean doFirst = true;
      letters.clear();

      for (Object item : formatParser.getItems()) {
        if (item instanceof DateTimePatternGenerator.VariableField) {
          char c = item.toString().charAt(0);
          if (letters.get(c)) {
            doFirst = false;
          } else {
            letters.set(c);
          }
          if (doFirst) {
            first.append(item); 
          } else {
            second.append(item);
          }
        } else {
          if (doFirst) {
            first.append(formatParser.quoteLiteral((String)item)); 
          } else {
            second.append(formatParser.quoteLiteral((String)item));
          }
        }
      }
      String calendar = parts.findAttributeValue("calendar", "type");
      firstFormat = icuServiceBuilder.getDateFormat(calendar, first.toString());
      firstFormat.setTimeZone(GMT_ZONE_SAMPLE);

      secondFormat = icuServiceBuilder.getDateFormat(calendar, second.toString());
      secondFormat.setTimeZone(GMT_ZONE_SAMPLE);
      return this;
    }
  }

  private String handleUnits(XPathParts parts, String xpath, String value, ExampleContext context, ExampleType type) {
    if (parts.contains("unitPattern")) {
      return formatCountValue(xpath, parts, value, context, type);
    }
    return null;
  }

  private String formatCountValue(String xpath, XPathParts parts, String value, ExampleContext context, ExampleType type) {
    if (!parts.containsAttribute("count")) { // no examples for items that don't format
      return null;
    }
    final PluralInfo plurals = supplementalDataInfo.getPlurals(cldrFile.getLocaleID());
    String unitType = parts.getAttributeValue(-2, "type");
    if (unitType == null) {
      unitType = "USD"; // sample for currency pattern
    }
    final boolean isPattern = parts.contains("unitPattern");
    final boolean isCurrency = !parts.contains("units");

    Count count = null;
    final List<Double> exampleCount;
    if (type != ExampleType.ENGLISH) {
      String countString = parts.getAttributeValue(-1, "count");
      if (countString == null) {
        //count = Count.one;
        return null;
      } else {
        count = Count.valueOf(countString);
      }
      exampleCount = plurals.getCountToExamplesMap().get(count);
      if (context != null) {
        context.setExampleCount(exampleCount);
      }
    } else {
      exampleCount = context.getExampleCount();
    }
    String result = "";
    for (double example : exampleCount) {
      // first see if there is a unit pattern in this type.
      if (type == ExampleType.ENGLISH) {
        count = plurals.getCount(example);
      }
      if (value == null) {
        //String clippedPath = parts.toString(-1);
        //clippedPath += "/" + parts.getElement(-1);
        String fallbackPath = cldrFile.getCountPathWithFallback(xpath, count, true);
        value = cldrFile.getStringValue(fallbackPath);
      }
      
      // If we have a pattern, get the unit from the count
      // If we have a unit, get the pattern from the count
      // English is special; both values are retrieved based on the count.
      String unitPattern;
      String unitName;
      System.out.println("unitName needs to be removed before 1.7 starts");
      if (isPattern) {
        // //ldml/numbers/currencies/currency[@type="USD"]/displayName
        unitName = getUnitName(unitType, isCurrency, count);
        unitPattern = type != ExampleType.ENGLISH ? value : getUnitPattern(unitType, isCurrency, count);
      } else {
        unitPattern = getUnitPattern(unitType, isCurrency, count);
        unitName = type != ExampleType.ENGLISH ? value : getUnitName(unitType, isCurrency, count);
      }
      
      unitPattern = setBackground(unitPattern);

      MessageFormat unitPatternFormat = new MessageFormat(unitPattern);

      // get the format for the currency
      // TODO fix this for special currency overrides

      DecimalFormat unitDecimalFormat = icuServiceBuilder.getNumberFormat(1); // decimal
      //Map arguments = new HashMap();

      if (isCurrency) {
        DecimalFormat currencyFormat = icuServiceBuilder.getCurrencyFormat(unitType);
        int decimalCount = currencyFormat.getMinimumFractionDigits();

        final boolean isInteger = example == Math.round(example);
        if (!isInteger && decimalCount == 0) continue; // don't display integers for fractions

        // int currentCount = isInteger ? 0 : decimalCount;
        unitDecimalFormat.setMaximumFractionDigits(decimalCount);
        unitDecimalFormat.setMinimumFractionDigits(decimalCount);

        // finally, format the result
      } else {
        unitDecimalFormat.setMinimumFractionDigits(0);
      }
      
      unitPatternFormat.setFormatByArgumentIndex(0, unitDecimalFormat);

      //arguments.put("quantity", example);
      //arguments.put("unit", value);
      String resultItem = unitPatternFormat.format(new Object[]{example, unitName});
      //resultItem = setBackground(resultItem).replace(unitName, backgroundEndSymbol + unitName + backgroundStartSymbol);
      resultItem = finalizeBackground(resultItem, isPattern);

      // now add to list
      if (result.length() != 0) {
        result += ", ";
      }
      result += resultItem;
    }
    return result;
  }

  private String getUnitPattern(String unitType, final boolean isCurrency, Count count) {
    String unitPattern;
    String unitPatternPath = cldrFile.getCountPathWithFallback(isCurrency 
            ? "//ldml/numbers/currencyFormats/unitPattern" 
                    : "//ldml/units/unit[@type=\""+ unitType + "\"]/unitPattern", 
                    count, true);
    unitPattern = cldrFile.getWinningValue(unitPatternPath);
    return unitPattern;
  }

  private String getUnitName(String unitType, final boolean isCurrency, Count count) {
    String unitName;
    String unitNamePath = cldrFile.getCountPathWithFallback(isCurrency 
            ? "//ldml/numbers/currencies/currency[@type=\"USD\"]/displayName"
                    : "//ldml/units/unit[@type=\""+ unitType + "\"]/unitName",
                    count, true);
    unitName = cldrFile.getWinningValue(unitNamePath);
    return unitName;
  }

  private String handleNumberSymbol() {
    DecimalFormat x = icuServiceBuilder.getNumberFormat(2);
    return x.format(NUMBER_SAMPLE);
  }

  private String handleTimeZoneName(String xpath, String value) {

    String result = null;
    if (parts.contains("exemplarCity")) {
      //        ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity
      String timezone = parts.getAttributeValue(3, "type");
      String countryCode = supplementalDataInfo.getZone_territory(timezone);
      if (countryCode == null) {
        return result; // fail, skip
      }
      if (countryCode.equals("001")) {
        // GMT code, so format.
        try {
          String hourOffset = timezone.substring(timezone.contains("+") ? 8 : 7);
          int hours = Integer.parseInt(hourOffset);
          result = getGMTFormat(null, null, hours);
        } catch (RuntimeException e) {
          return result; // fail, skip
        }
      } else {
        String countryName = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode));
        boolean singleZone = singleCountryZones.contains(timezone) || !supplementalDataInfo.getMultizones().contains(countryCode);
        // we show just country for singlezone countries
        if (singleZone) {
          result = countryName;
        } else {
          if (value == null) {
            value = TimezoneFormatter.getFallbackName(timezone);
          }
          // otherwise we show the fallback with exemplar
          String fallback = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat"));
          // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity

          result = format(fallback, value, countryName);
        }
        // format with "{0} Time" or equivalent.
        String timeFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat"));
        result = format(timeFormat, result);
      }
    } else if (parts.contains("regionFormat")) { // {0} Time
      String sampleTerritory = cldrFile.getName(CLDRFile.TERRITORY_NAME, "JP");
      result = format(value, setBackground(sampleTerritory));
    } else if (parts.contains("fallbackFormat")) { // {1} ({0})
      if (value == null) {
        return result;
      }
      String timeFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat"));
      String us = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "US"));
      // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity

      String LosAngeles = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity"));
      result = format(value, LosAngeles, us);
      result = format(timeFormat, result);
    } else if (parts.contains("gmtFormat")) { // GMT{0}
      result = getGMTFormat(null, value, -8);
    } else if (parts.contains("hourFormat")) { // +HH:mm;-HH:mm
      result = getGMTFormat(value, null, -8);
    } else if (parts.contains("metazone") && !parts.contains("commonlyUsed")) { // Metazone string
      if ( value != null && value.length() > 0 ) {
        result = getMZTimeFormat() + " " + value;
      }
      else {
        // TODO check for value
        if (parts.contains("generic")) {
          String metazone_name = parts.getAttributeValue(3, "type");
          String timezone = supplementalData.resolveParsedMetazone(metazone_name,"001");
          String countryCode = supplementalDataInfo.getZone_territory(timezone);
          String regionFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat");
          String fallbackFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat");
          String exemplarCity = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\""+timezone+"\"]/exemplarCity");
          if ( exemplarCity == null )
             exemplarCity = timezone.substring(timezone.lastIndexOf('/')+1).replace('_',' ');

          String countryName = cldrFile.getWinningValue("//ldml/localeDisplayNames/territories/territory[@type=\""+countryCode+"\"]");
          boolean singleZone = singleCountryZones.contains(timezone) || !(supplementalDataInfo.getMultizones().contains(countryCode));

          if ( singleZone ) {
            result = setBackground(getMZTimeFormat() + " " + 
                      format(regionFormat, countryName));
          }
          else {
            result = setBackground(getMZTimeFormat() + " " + 
                      format(fallbackFormat, exemplarCity, countryName));
          }
        }
        else {
          String gmtFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
          String hourFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
          String metazone_name = parts.getAttributeValue(3, "type");
          String tz_string = supplementalData.resolveParsedMetazone(metazone_name,"001");
          TimeZone currentZone = TimeZone.getTimeZone(tz_string);
          int tzOffset = currentZone.getRawOffset();
          if (parts.contains("daylight")) {
             tzOffset += currentZone.getDSTSavings();
          }
          int MILLIS_PER_MINUTE = 1000 * 60;
          int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
          int tm_hrs = tzOffset / MILLIS_PER_HOUR;
          int tm_mins = ( tzOffset % MILLIS_PER_HOUR ) / 60000; // millis per minute
          result = setBackground(getMZTimeFormat() + " " + getGMTFormat( hourFormat,gmtFormat,tm_hrs,tm_mins));
        }
      }
    }
    result = finalizeBackground(result, false);
    return result;
  }

  private String handleDateFormatItem(String value) {
    String result = null;
    if (parts.contains("calendar")) {
      String calendar = parts.findAttributeValue("calendar", "type");
      SimpleDateFormat dateFormat;
      if (parts.contains("dateTimeFormat")) {
        SimpleDateFormat date2 = icuServiceBuilder.getDateFormat(calendar, 2, 0); // date
        SimpleDateFormat time = icuServiceBuilder.getDateFormat(calendar, 0, 2); // time
        date2.applyPattern(format(value, setBackground(time.toPattern()), setBackground(date2.toPattern())));
        dateFormat = date2;
      } else {
        String id = parts.findAttributeValue("dateFormatItem", "id");
        if ("NEW".equals(id) || value == null) {
          result = "<i>n/a</i>";
          return result;
        } else {
          dateFormat = icuServiceBuilder.getDateFormat(calendar, value);
        }
      }
      dateFormat.setTimeZone(ZONE_SAMPLE);
      result = dateFormat.format(DATE_SAMPLE);
      result = finalizeBackground(result, false);
    } else if (parts.contains("numbers")) {
      DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(value);
      result = numberFormat.format(NUMBER_SAMPLE);
    }
    return result;
  }

  private String handleCurrency(String xpath, String value, ExampleContext context, ExampleType type) {
    String currency = parts.getAttributeValue(-2, "type");
    String fullPath = cldrFile.getFullXPath(xpath, false);
    if (parts.contains("symbol")) {
      if (fullPath != null && fullPath.contains("[@choice=\"true\"]")) {
        ChoiceFormat cf = new ChoiceFormat(value);
        value = cf.format(NUMBER_SAMPLE);
      }
      // TODO fix to use value!!
      String result;
      DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency, value);
      result = x.format(NUMBER_SAMPLE);
      result = setBackground(result).replace(value, backgroundEndSymbol + value + backgroundStartSymbol);
      result = finalizeBackground(result, false);
      return result;
    } else if (parts.contains("displayName")) {
      return formatCountValue(xpath, parts, value, context, type);
    }
    return null;
  }

  private String handleDateRangePattern(String value, String xpath, Zoomed zoomed) {
    String result;
    SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
    result = format(value, setBackground(dateFormat.format(DATE_SAMPLE)), setBackground(dateFormat.format(DATE_SAMPLE2)));
    result = finalizeBackground(result, false);
    return result;
  }

  private String handleDisplayNames(String xpath, XPathParts parts, String value) {
    String result = null;
    if (parts.contains("codePatterns")) {
      String type = parts.getAttributeValue(-1, "type");
      result = format(value, setBackground(
              type.equals("language") ? "ace"
                      : type.equals("script") ? "Avst"
                          : type.equals("territory") ? "057" : "CODE"));
      result = finalizeBackground(result, false);
    } else if (parts.contains("localeDisplayPattern")) {
      result = cldrFile.getName("uz_Arab_AF");
    } else if (parts.contains("languages") ) {
      String type = parts.getAttributeValue(-1, "type");
      if (type.contains("_")) {
        if (value != null && !value.equals(type)) {
          result = value;
        } else {
          result = cldrFile.getName(parts.findAttributeValue("language", "type"));
        }
      }
    }
    return result;
  }

  private String handleExemplarCharacters(String value, Zoomed zoomed) {
    String result = null;
    if (value != null) {
      if (zoomed == Zoomed.IN) {
        UnicodeSet unicodeSet = new UnicodeSet(value);
        if (unicodeSet.size() < 500) {
          result = new PrettyPrinter()
        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
                .setCompressRanges(false)
                .format(unicodeSet);
        }
      }
    }
    return result;
  }
  
  public String format(String format, Object...objects ) {
    if (format == null) return null;
    return MessageFormat.format(format, objects);
  }

  public static final String unchainException(Exception e) {
    String stackStr = "[unknown stack]<br>";
    try {
        StringWriter asString = new StringWriter();
        e.printStackTrace(new PrintWriter(asString));
        stackStr = "<pre>" + asString.toString() +"</pre>";
    } catch ( Throwable tt ) {
        // ...
    }
    return stackStr;
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

  /**
   * This is called just before we return a result. It fixes the special characters that were added by setBackground.
   * @param input string with special characters from setBackground.
   * @param invert TODO
   * @return string with HTML for the background.
   */
  private String finalizeBackground(String input, boolean invert) {
    if (invert) input = backgroundEndSymbol + input + backgroundStartSymbol;
    return input == null ? input : input
            .replace(backgroundStartSymbol + backgroundEndSymbol, "") // remove null runs
            // null
            // runs
            .replace(backgroundEndSymbol + backgroundStartSymbol, "") // remove null runs
            // runs
            .replace(backgroundStartSymbol, invert ? backgroundEnd : backgroundStart)
            .replace(backgroundEndSymbol, invert ? backgroundStart : backgroundEnd);
  }

  public static final Pattern PARAMETER = Pattern.compile("(\\{[0-9]\\})");

  /**
   * Utility to format using a gmtHourString, gmtFormat, and an integer hours. We only need the hours because that's all
   * the TZDB IDs need. Should merge this eventually into TimeZoneFormatter and call there.
   * @param gmtHourString
   * @param gmtFormat
   * @param hours
   * @return
   */
  private String getGMTFormat(String gmtHourString, String gmtFormat, int hours) {
     return getGMTFormat(gmtHourString,gmtFormat,hours,0);
  }

  private String getGMTFormat(String gmtHourString, String gmtFormat, int hours, int minutes) {
    boolean hoursBackground = false;
    if (gmtHourString == null) {
      hoursBackground = true;
      gmtHourString = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
    }
    if (gmtFormat == null) {
      hoursBackground = false; // for the hours case
      gmtFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat"));
    }
    String[] plusMinus = gmtHourString.split(";");

    SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", plusMinus[hours >= 0 ? 0 : 1]);
    dateFormat.setTimeZone(ZONE_SAMPLE);
    calendar.set(1999, 9, 27, Math.abs(hours), minutes, 0); // 1999-09-13 13:25:59
    Date sample = calendar.getTime();
    String hourString = dateFormat.format(sample);
    if (hoursBackground) {
      hourString = setBackground(hourString);
    }
    String result = MessageFormat.format(gmtFormat, new Object[] { hourString });
    return result;
  }

  private String getMZTimeFormat() {
    String timeFormat = cldrFile.getWinningValue("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
    if ( timeFormat == null ) {
       timeFormat = "HH:mm";
    }
    // the following is <= because the TZDB inverts the hours
    SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", timeFormat);
    dateFormat.setTimeZone(ZONE_SAMPLE);
    calendar.set(1999, 9, 13, 13, 25, 59); // 1999-09-13 13:25:59
    Date sample = calendar.getTime();
    String result = dateFormat.format(sample);
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
    synchronized (this) {
      if (helpMessages == null) {
        helpMessages = new HelpMessages("test_help_messages.html");
      }
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

  /**
   * Private class to get the messages from a help file.
   */
  public static class HelpMessages {
    private static final Matcher CLEANUP_BOOKMARK = Pattern.compile("[^a-zA-Z0-9]").matcher("");

    private static final MessageFormat DEFAULT_HEADER_PATTERN = new MessageFormat("<p>{0}</p>" + CldrUtility.LINE_SEPARATOR);

    private static final Matcher HEADER_HTML = Pattern.compile("<h[0-9]>(.*)</h[0-9]>").matcher("");

    List<Matcher> keys = new ArrayList();

    List<String> values = new ArrayList();

    enum Status {
      BASE, BEFORE_CELL, IN_CELL, IN_INSIDE_TABLE
    };

    StringBuilder[] currentColumn = new StringBuilder[2];

    int column = 0;

    /**
     * Create a HelpMessages object from a filename.
     * The file has to be in the format of a table of <keyRegex,htmlText> pairs, 
     * where the key is a keyRegex expression and htmlText is arbitrary HTML text. For example:
     * <p>{@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/chart_messages.html}
     *  is used for chart messages, where the key is the name of the chart.
     * <p>{@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/test_help_messages.html}
     * is used for help messages in the survey tool, where the key is an xpath.
     * @param filename
     */
    public HelpMessages(String filename) {
      currentColumn[0] = new StringBuilder();
      currentColumn[1] = new StringBuilder();
      BufferedReader in;
      try {
        in = CldrUtility.getUTF8Data(filename);
        Status status = Status.BASE;
        int count = 0;
        int tableCount = 0;

        boolean inContent = false;
        // if the table level is 1 (we are in the main table), then we look for <td>...</td><td>...</td>. That means that we have column 1 and column 2.
        
        SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
        StringBuilder result = new StringBuilder();
        boolean hadPop = false;
        main:
          while (true) {
            Type x = simple.next(result);
            switch (x) {
              case ELEMENT: // with /table we pop the count
                if (SimpleHtmlParser.equals("table", result)) {
                  if (hadPop) { 
                    --tableCount;
                  } else {
                    ++tableCount;
                  }
                } else if (tableCount == 1) { 
                  if (SimpleHtmlParser.equals("tr", result)) {
                    if (hadPop) {
                      addHelpMessages();
                    }
                    column = 0;
                  } else if (SimpleHtmlParser.equals("td", result)) {
                    if (hadPop) { 
                      inContent = false;
                      ++column;
                    } else {
                      inContent = true;
                      continue main; // skip adding
                    }
                  }
                }
                break;
              case ELEMENT_POP:
                hadPop = true;
                break;
              case ELEMENT_END:
                hadPop = false;
                break;
              case DONE:
                break main;
            }
            if (inContent) {
              SimpleHtmlParser.writeResult(x, result, currentColumn[column]);
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

    /**
     * Get message corresponding to a key out of the file set on this object. 
     * For many files, the key will be an xpath, but it doesn't have to be. 
     * Note that <i>all</i> of pairs of <keyRegex,htmlText> where the key matches keyRegex
     * will be concatenated together in order to get the result.
     * @param key
     * @return
     */
    public String find(String key) {
      return find(key, DEFAULT_HEADER_PATTERN);
    }
    
    /**
     * Get message corresponding to a key out of the file set on this object. 
     * For many files, the key will be an xpath, but it doesn't have to be. 
     * Note that <i>all</i> of pairs of <keyRegex,htmlText> where the key matches keyRegex
     * will be concatenated together in order to get the result.
     * @param key
     * @param addHeader true if you want a header formed by looking at all the hN elements.
     * @return
     */
    public String find(String key, MessageFormat headerPattern) {
      StringBuilder header = new StringBuilder();
      StringBuilder result = new StringBuilder();
      int keyCount = 0;
      for (int i = 0; i < keys.size(); ++i) {
        if (keys.get(i).reset(key).matches()) {
          if (result.length() != 0) {
            result.append(CldrUtility.LINE_SEPARATOR);
          }
          String value = values.get(i);
          if (headerPattern != null) {
            HEADER_HTML.reset(value);
            int lastEnd = 0;
            StringBuilder newValue = new StringBuilder();
            while (HEADER_HTML.find()) {
              String contents = HEADER_HTML.group(1);
              if (contents.contains("<")) {
                continue; // disallow other formatting
              }
              String bookmark = "HM_" + CLEANUP_BOOKMARK.reset(contents).replaceAll("_");
              keyCount++;
              if (header.length() > 0) {
                header.append(" | ");
              }
              header.append("<a href='#").append(bookmark).append("'>").append(contents).append("</a>");
              newValue.append(value.substring(lastEnd, HEADER_HTML.start(1)));
              newValue.append("<a name='").append(bookmark).append("'>").append(contents).append("</a>");
              lastEnd = HEADER_HTML.end(1);
            }
            newValue.append(value.substring(lastEnd));
            value = newValue.toString();
          }
          result.append(value);
        }
      }
      if (result.length() != 0) {
        if (keyCount > 1) {
          result.insert(0, headerPattern.format(new Object[]{header.toString()}));
        }
        return result.toString();
      }
      return null;
    }

    private void addHelpMessages() {
      if (column == 2) { // must have two columns
        try {
          // remove the first character and the last two characters, since the are >....</
          String key = currentColumn[0].substring(1, currentColumn[0].length()-2).trim();
          String value = currentColumn[1].substring(1, currentColumn[1].length()-2).trim();
          if (DEBUG_SHOW_HELP) {
            System.out.println("{" + key + "} => {" + value + "}");
          }
          Matcher m = Pattern.compile(TransliteratorUtilities.fromHTML.transliterate(key), Pattern.COMMENTS).matcher("");
          keys.add(m);
          values.add(value);
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
