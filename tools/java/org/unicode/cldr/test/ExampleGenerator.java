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
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Class to generate examples and help messages for the Survey tool (or console version).
 * 
 * @author markdavis
 * 
 */
public class ExampleGenerator {
    private static final String EXEMPLAR_CITY_LOS_ANGELES = "//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity";

    private static final boolean SHOW_ERROR = false;

    private static final Pattern URL_PATTERN = Pattern
        .compile("http://[\\-a-zA-Z0-9]+(\\.[\\-a-zA-Z0-9]+)*([/#][\\-a-zA-Z0-9]+)*");

    private final static boolean DEBUG_SHOW_HELP = false;

    private static SupplementalDataInfo supplementalDataInfo;
    private PathDescription pathDescription;

    /**
     * Zoomed status.
     * 
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

    public final static double NUMBER_SAMPLE = 123456.789;
    public final static double NUMBER_SAMPLE_WHOLE = 2345;

    public final static TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
    public final static TimeZone GMT_ZONE_SAMPLE = TimeZone.getTimeZone("Etc/GMT");

    public final static Date DATE_SAMPLE;

    private final static Date DATE_SAMPLE2;

    // private final static String EXEMPLAR_CITY = "Europe/Rome";

    private String backgroundStart = "<span class='cldr_substituted'>";
    private String backgroundEnd = "</span>";

    private static final String exampleStart = "<div class='cldr_example'>";
    private static final String exampleEnd = "</div>";
    private static final String startItalic = "<i>";
    private static final String endItalic = "</i>";

    private static final String backgroundStartSymbol = "\uE234";
    private static final String backgroundEndSymbol = "\uE235";
    private static final String backgroundTempSymbol = "\uE236";
    private static final String exampleSeparatorSymbol = "\uE237";
    private static final String startItalicSymbol = "\uE238";
    private static final String endItalicSymbol = "\uE239";

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
    private CLDRFile englishFile;
    private CoverageLevel2 coverageLevel;
    Matcher URLMatcher = URL_PATTERN.matcher("");

    private Map<String, String> cache = new HashMap<String, String>();

    private static final String NONE = "\uFFFF";

    // Matcher skipMatcher = Pattern.compile(
    // "/localeDisplayNames(?!"
    // ).matcher("");
    private XPathParts parts = new XPathParts();

    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private Set<String> singleCountryZones;

    private PluralInfo pluralInfo;

    private Map<Integer, Map<Count, Integer>> patternExamples;

    /**
     * For getting the end of the "background" style. Default is "</span>". It is
     * used in composing patterns, so it can show the part that corresponds to the
     * value.
     * 
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
     * 
     * @param resolvedCldrFile
     * @param supplementalDataDirectory
     */
    public ExampleGenerator(CLDRFile resolvedCldrFile, CLDRFile englishFile, String supplementalDataDirectory) {
        if (!resolvedCldrFile.isResolved()) throw new IllegalArgumentException("CLDRFile must be resolved");
        if (!englishFile.isResolved()) throw new IllegalArgumentException("English CLDRFile must be resolved");
        cldrFile = resolvedCldrFile;
        this.englishFile = englishFile;
        synchronized (ExampleGenerator.class) {
            if (supplementalDataInfo == null) {
                supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDataDirectory);
            }
        }
        icuServiceBuilder.setCldrFile(cldrFile);
        col = Collator.getInstance(new ULocale(cldrFile.getLocaleID()));
        coverageLevel = CoverageLevel2.getInstance(supplementalDataInfo, resolvedCldrFile.getLocaleID());

        String singleCountriesPath = cldrFile.getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
        if (singleCountriesPath == null) {
            System.err.println("Failure: in " + cldrFile.getLocaleID()
                + " examplegenerator- cldrFile.getFullXPath(//ldml/dates/timeZoneNames/singleCountries)==null");
        } else {
            parts.set(singleCountriesPath);
            String listValue = parts.getAttributeValue(-1, "list");
            if (listValue == null) {
                System.err.println("Failure: in " + cldrFile.getLocaleID() + " examplegenerator- "
                    + singleCountriesPath + "  has a bad list attribute.");
            } else {
                singleCountryZones = new HashSet<String>(Arrays.asList(listValue.trim().split("\\s+")));
            }
        }
        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
        patternExamples = new HashMap<Integer, Map<Count, Integer>>();
    }

    public enum ExampleType {
        NATIVE, ENGLISH
    };

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
     * @param zoomed
     *            status (IN, or OUT) Out is a longer version only called in Zoom mode. IN is called in both.
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
                result = handleDateRangePattern(value, xpath, zoomed);
            } else if (parts.contains("timeZoneNames")) {
                result = handleTimeZoneName(xpath, value);
            } else if (parts.contains("exemplarCharacters")) {
                result = handleExemplarCharacters(value, zoomed);
            } else if (parts.contains("localeDisplayNames")) {
                result = handleDisplayNames(xpath, parts, value);
            } else if (parts.contains("currency")) {
                result = handleCurrency(xpath, value, context, type);
            } else if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
                if (parts.contains("calendar")) {
                    result = handleDateFormatItem(value);
                } else if (parts.contains("numbers")) {
                    result = handleDecimalFormat(parts, value, type);
                }
            } else if (parts.getElement(2).contains("symbols")) {
                result = handleNumberSymbol(parts, value);
            } else if (parts.contains("defaultNumberingSystem") || parts.contains("otherNumberingSystems")) {
                result = handleNumberingSystem(value);
            } else if (parts.getElement(1).equals("units")) {
                result = handleUnits(parts, xpath, value, context, type);
            } else if (parts.contains("currencyFormats") && parts.contains("unitPattern")) {
                result = formatCountValue(xpath, parts, value, context, type);
            } else if (parts.contains("intervalFormats")) {
                result = handleIntervalFormats(parts, xpath, value, context, type);
            } else if (parts.getElement(1).equals("delimiters")) {
                result = handleDelimiters(parts, xpath, value);
            } else if (parts.getElement(1).equals("listPatterns")) {
                result = handleListPatterns(parts, value);
            } else if (parts.getElement(2).equals("ellipsis")) {
                result = handleEllipsis(value);
            } else if (parts.getElement(-1).equals("monthPattern")) {
                result = handleMonthPatterns(parts, value);
            } else if (parts.getElement(-1).equals("appendItem")) {
                result = handleAppendItems(parts, value);
            } else {
                // didn't detect anything, return empty-handed
                return null;
            }
        } catch (NullPointerException e) {
            if (SHOW_ERROR) {
                e.printStackTrace();
            }
            return null;
        } catch (RuntimeException e) {
            String unchained = verboseErrors ? ("<br>" + finalizeBackground(unchainException(e))) : "";
            return "<i>Parsing error. " + finalizeBackground(e.getMessage()) + "</i>" + unchained;
        }
        result = finalizeBackground(result);

        if (CACHING) {
            if (result == null) {
                cache.put(cacheKey, NONE);
            } else {
                // fix HTML, cache
                cache.put(cacheKey, result);
            }
        }
        return result;
    }

    IntervalFormat intervalFormat = new IntervalFormat();

    static Calendar generatingCalendar = Calendar.getInstance(ULocale.US);

    private static Date getDate(int year, int month, int date, int hour, int minute, int second, TimeZone zone) {
        synchronized (generatingCalendar) {
            generatingCalendar.setTimeZone(GMT_ZONE_SAMPLE);
            generatingCalendar.set(year, month, date, hour, minute, second);
            return generatingCalendar.getTime();
        }
    }

    static Date FIRST_INTERVAL = getDate(2008, 1, 13, 5, 7, 9, GMT_ZONE_SAMPLE);
    static Map<String, Date> SECOND_INTERVAL = CldrUtility.asMap(new Object[][] {
        { "y", getDate(2009, 2, 14, 17, 8, 10, GMT_ZONE_SAMPLE) },
        { "M", getDate(2008, 2, 14, 17, 8, 10, GMT_ZONE_SAMPLE) },
        { "d", getDate(2008, 1, 14, 17, 8, 10, GMT_ZONE_SAMPLE) },
        { "a", getDate(2008, 1, 13, 17, 8, 10, GMT_ZONE_SAMPLE) },
        { "h", getDate(2008, 1, 13, 6, 8, 10, GMT_ZONE_SAMPLE) },
        { "m", getDate(2008, 1, 13, 5, 8, 10, GMT_ZONE_SAMPLE) }
    });

    private String handleIntervalFormats(XPathParts parts, String xpath, String value,
        ExampleContext context, ExampleType type) {
        if (!parts.getAttributeValue(3, "type").equals("gregorian")) {
            return null;
        }
        if (parts.getElement(6).equals("intervalFormatFallback")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            String fallbackFormat = invertBackground(setBackground(value));
            return format(fallbackFormat, dateFormat.format(FIRST_INTERVAL),
                dateFormat.format(SECOND_INTERVAL.get("y")));
        }
        String greatestDifference = parts.getAttributeValue(-1, "id");
        if (greatestDifference.equals("H")) greatestDifference = "h";
        // intervalFormatFallback
        // //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="yMd"]/greatestDifference[@id="y"]
        // find where to split the value
        intervalFormat.setPattern(value);
        return intervalFormat.format(FIRST_INTERVAL, SECOND_INTERVAL.get(greatestDifference));
    }

    private String handleDelimiters(XPathParts parts, String xpath, String value) {
        String lastElement = parts.getElement(-1);
        final String[] elements = {
            "quotationStart", "alternateQuotationStart",
            "alternateQuotationEnd", "quotationEnd" };
        String[] quotes = new String[4];
        String baseXpath = xpath.substring(0, xpath.lastIndexOf('/'));
        for (int i = 0; i < quotes.length; i++) {
            String currElement = elements[i];
            if (lastElement.equals(currElement)) {
                quotes[i] = backgroundStartSymbol + value + backgroundEndSymbol;
            } else {
                quotes[i] = cldrFile.getWinningValue(baseXpath + '/' + currElement);
            }
        }
        String example = cldrFile
            .getStringValue("//ldml/localeDisplayNames/types/type[@type=\"gregorian\"][@key=\"calendar\"]");
        // NOTE: the example provided here is partially in English because we don't
        // have a translated conversational example in CLDR.
        return invertBackground(format("{0}They said {1}" + example + "{2}.{3}", (Object[]) quotes));
    }

    private String handleListPatterns(XPathParts parts, String value) {
        String patternType = parts.getAttributeValue(-1, "type");
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        if (patternType.equals("2")) {
            return invertBackground(format(setBackground(value), territory1, territory2));
        }
        String territory3 = getValueFromFormat(pathFormat, "EG");
        String territory4 = getValueFromFormat(pathFormat, "CA");
        String listPathFormat = "//ldml/listPatterns/listPattern/listPatternPart[@type=\"{0}\"]";
        String startPattern = getPattern(listPathFormat, "start", patternType, value);
        String middlePattern = getPattern(listPathFormat, "middle", patternType, value);
        String endPattern = getPattern(listPathFormat, "end", patternType, value);

        String example = format(startPattern, territory1,
            format(middlePattern, territory2, format(endPattern, territory3, territory4)));
        return invertBackground(example);
    }

    /**
     * Helper method for handleListPatterns. Returns the pattern to be used for
     * a specified pattern type.
     * 
     * @param pathFormat
     * @param pathPatternType
     * @param valuePatternType
     * @param value
     * @return
     */
    private String getPattern(String pathFormat, String pathPatternType,
        String valuePatternType, String value) {
        return valuePatternType.equals(pathPatternType) ?
            setBackground(value) :
            getValueFromFormat(pathFormat, pathPatternType);
    }

    private String getValueFromFormat(String format, Object... arguments) {
        return cldrFile.getWinningValue(format(format, arguments));
    }

    private String handleEllipsis(String value) {
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        return invertBackground(format(setBackground(value), territory1, territory2));
    }

    /**
     * Handle miscellaneous calendar patterns.
     * 
     * @param parts
     * @param value
     * @return
     */
    private String handleMonthPatterns(XPathParts parts, String value) {
        String calendar = parts.getAttributeValue(3, "type");
        String context = parts.getAttributeValue(5, "type");
        String month = "8";
        if (!context.equals("numeric")) {
            String width = parts.getAttributeValue(6, "type");
            String xpath = "//ldml/dates/calendars/calendar[@type=\"{0}\"]/months/monthContext[@type=\"{1}\"]/monthWidth[@type=\"{2}\"]/month[@type=\"8\"]";
            month = getValueFromFormat(xpath, calendar, context, width);
        }
        return invertBackground(format(setBackground(value), month));
    }

    private String handleAppendItems(XPathParts parts, String value) {

        return null;
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
                        first.append(formatParser.quoteLiteral((String) item));
                    } else {
                        second.append(formatParser.quoteLiteral((String) item));
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
        if (parts.getElement(-1).equals("unitPattern")) {
            return formatCountValue(xpath, parts, value, context, type);
        }
        return null;
    }

    private String formatCountValue(String xpath, XPathParts parts, String value, ExampleContext context,
        ExampleType type) {
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
                // count = Count.one;
                return null;
            } else {
                try {
                    count = Count.valueOf(countString);
                } catch (Exception e) {
                    return null; // counts like 0
                }
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
                // String clippedPath = parts.toString(-1);
                // clippedPath += "/" + parts.getElement(-1);
                String fallbackPath = cldrFile.getCountPathWithFallback(xpath, count, true);
                value = cldrFile.getStringValue(fallbackPath);
            }

            // If we have a pattern, get the unit from the count
            // If we have a unit, get the pattern from the count
            // English is special; both values are retrieved based on the count.
            String unitPattern;
            String unitName;
            if (isPattern) {
                // //ldml/numbers/currencies/currency[@type="USD"]/displayName
                unitName = getUnitName(unitType, isCurrency, count);
                unitPattern = type != ExampleType.ENGLISH ? value : getUnitPattern(unitType, isCurrency, count);
            } else {
                unitPattern = getUnitPattern(unitType, isCurrency, count);
                unitName = type != ExampleType.ENGLISH ? value : getUnitName(unitType, isCurrency, count);
            }

            if (isPattern) {
                unitPattern = setBackground(unitPattern);
            } else {
                unitPattern = setBackgroundExceptMatch(unitPattern, PARAMETER_SKIP0);
            }

            MessageFormat unitPatternFormat = new MessageFormat(unitPattern);

            // get the format for the currency
            // TODO fix this for special currency overrides

            DecimalFormat unitDecimalFormat = icuServiceBuilder.getNumberFormat(1); // decimal
            // Map arguments = new HashMap();

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

            // arguments.put("quantity", example);
            // arguments.put("unit", value);
            String resultItem = unitPatternFormat.format(new Object[] { example, unitName });
            // resultItem = setBackground(resultItem).replace(unitName, backgroundEndSymbol + unitName +
            // backgroundStartSymbol);
            if (isPattern) {
                resultItem = invertBackground(resultItem);
            }

            // now add to list
            result = addExampleResult(resultItem, result);
        }
        return result;
    }

    private String addExampleResult(String resultItem, String resultToAddTo) {
        if (resultToAddTo.length() != 0) {
            resultToAddTo += exampleSeparatorSymbol;
        }
        resultToAddTo += resultItem;
        return resultToAddTo;
    }

    private String getUnitPattern(String unitType, final boolean isCurrency, Count count) {
        String unitPattern;
        String unitPatternPath = cldrFile.getCountPathWithFallback(isCurrency
            ? "//ldml/numbers/currencyFormats/unitPattern"
            : "//ldml/units/unit[@type=\"" + unitType + "\"]/unitPattern",
            count, true);
        unitPattern = cldrFile.getWinningValue(unitPatternPath);
        return unitPattern;
    }

    private String getUnitName(String unitType, final boolean isCurrency, Count count) {
        String unitName;
        String unitNamePath = cldrFile.getCountPathWithFallback(isCurrency
            ? "//ldml/numbers/currencies/currency[@type=\"USD\"]/displayName"
            : "//ldml/units/unit[@type=\"" + unitType + "\"]/unitPattern",
            count, true);
        unitName = cldrFile.getWinningValue(unitNamePath);
        return unitName;
    }

    private String handleNumberSymbol(XPathParts parts, String value) {
        String symbolType = parts.getElement(-1);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        int index = 1;// dec/percent/sci
        double numberSample = NUMBER_SAMPLE;
        String originalValue = cldrFile.getWinningValue(parts.toString());
        if (symbolType.equals("decimal") || symbolType.equals("group")) {
            index = 1;
        } else if (symbolType.equals("minusSign")) {
            index = 1;
            numberSample = -numberSample;
        } else if (symbolType.equals("percentSign")) {
            // For the perMille symbol, we reuse the percent example.
            index = 2;
            numberSample = 0.23;
        } else if (symbolType.equals("perMille")) {
            // For the perMille symbol, we reuse the percent example.
            index = 2;
            numberSample = 0.023;
            originalValue = cldrFile.getWinningValue(parts.addRelative("../percentSign").toString());
        } else if (symbolType.equals("exponential") || symbolType.equals("plusSign")) {
            index = 3;
        } else {
            // We don't need examples for standalone symbols, i.e. infinity and nan.
            // We don't have an example for the list symbol either.
            return null;
        }
        DecimalFormat x = icuServiceBuilder.getNumberFormat(index, numberSystem);
        x.setExponentSignAlwaysShown(true);
        String example = x.format(numberSample);
        example = example.replace(originalValue, backgroundEndSymbol + value + backgroundStartSymbol);
        return backgroundStartSymbol + example + backgroundEndSymbol;
    }

    private String handleNumberingSystem(String value) {
        NumberFormat x = icuServiceBuilder.getGenericNumberFormat(value);
        return x.format(NUMBER_SAMPLE_WHOLE);
    }

    private String handleTimeZoneName(String xpath, String value) {

        String result = null;
        if (parts.contains("exemplarCity")) {
            // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity
            String timezone = parts.getAttributeValue(3, "type");
            String countryCode = supplementalDataInfo.getZone_territory(timezone);
            if (countryCode == null) {
                if (value == null) {
                    result = timezone.substring(timezone.lastIndexOf('/') + 1).replace('_', ' ');
                } else {
                    result = value;
                }
                return result;
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
                boolean singleZone = singleCountryZones.contains(timezone)
                    || !supplementalDataInfo.getMultizones().contains(countryCode);
                // we show just country for singlezone countries
                if (singleZone) {
                    result = countryName;
                } else {
                    if (value == null) {
                        value = TimezoneFormatter.getFallbackName(timezone);
                    }
                    // otherwise we show the fallback with exemplar
                    String fallback = setBackground(cldrFile
                        .getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat"));
                    // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity

                    result = format(fallback, value, countryName);
                }
                // format with "{0} Time" or equivalent.
                String timeFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat"));
                result = format(timeFormat, result);
            }
        } else if (parts.contains("zone")) { // {0} Time
            result = value;
        } else if (parts.contains("regionFormat")) { // {0} Time
            result = format(value, setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "JP")));
            result = addExampleResult(
                format(value, setBackground(cldrFile.getWinningValue(EXEMPLAR_CITY_LOS_ANGELES))), result);
        } else if (parts.contains("fallbackFormat")) { // {1} ({0})
            if (value == null) {
                return result;
            }
            String timeFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat"));
            String us = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "US"));
            // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity
            String LosAngeles = setBackground(cldrFile.getWinningValue(EXEMPLAR_CITY_LOS_ANGELES));
            result = format(value, LosAngeles, us);
            result = format(timeFormat, result);
        } else if (parts.contains("fallbackRegionFormat")) {
            String us = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "US"));
            String LosAngeles = setBackground(cldrFile.getWinningValue(EXEMPLAR_CITY_LOS_ANGELES));
            result = format(value, LosAngeles, us);
        } else if (parts.contains("gmtFormat")) { // GMT{0}
            result = getGMTFormat(null, value, -8);
        } else if (parts.contains("hourFormat")) { // +HH:mm;-HH:mm
            result = getGMTFormat(value, null, -8);
        } else if (parts.contains("metazone") && !parts.contains("commonlyUsed")) { // Metazone string
            if (value != null && value.length() > 0) {
                result = getMZTimeFormat() + " " + value;
            } else {
                // TODO check for value
                if (parts.contains("generic")) {
                    String metazone_name = parts.getAttributeValue(3, "type");
                    String timezone = supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    String countryCode = supplementalDataInfo.getZone_territory(timezone);
                    String regionFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat");
                    String fallbackFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat");
                    String exemplarCity = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\""
                        + timezone + "\"]/exemplarCity");
                    if (exemplarCity == null) {
                        exemplarCity = timezone.substring(timezone.lastIndexOf('/') + 1).replace('_', ' ');
                    }
                    String countryName = cldrFile
                        .getWinningValue("//ldml/localeDisplayNames/territories/territory[@type=\"" + countryCode
                            + "\"]");
                    boolean singleZone = singleCountryZones.contains(timezone)
                        || !(supplementalDataInfo.getMultizones().contains(countryCode));

                    if (singleZone) {
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
                    // String tz_string = supplementalData.resolveParsedMetazone(metazone_name,"001");
                    String tz_string = supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    TimeZone currentZone = TimeZone.getTimeZone(tz_string);
                    int tzOffset = currentZone.getRawOffset();
                    if (parts.contains("daylight")) {
                        tzOffset += currentZone.getDSTSavings();
                    }
                    int MILLIS_PER_MINUTE = 1000 * 60;
                    int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
                    int tm_hrs = tzOffset / MILLIS_PER_HOUR;
                    int tm_mins = (tzOffset % MILLIS_PER_HOUR) / 60000; // millis per minute
                    result = setBackground(getMZTimeFormat() + " "
                        + getGMTFormat(hourFormat, gmtFormat, tm_hrs, tm_mins));
                }
            }
        }
        return result;
    }

    private static String[] DateFormatNames = { "none", "short", "medium", "long", "full" };

    private String handleDateFormatItem(String value) {
        String calendar = parts.findAttributeValue("calendar", "type");
        SimpleDateFormat dateFormat;
        if (parts.contains("dateTimeFormat")) {
            int formatLengthIndex = 2; // medium
            String formatLengthName = parts.findAttributeValue("dateTimeFormatLength", "type");
            if (formatLengthName != null) {
                for (int nameIndex = 0; nameIndex < DateFormatNames.length; nameIndex++) {
                    if (formatLengthName.equals(DateFormatNames[nameIndex])) {
                        formatLengthIndex = nameIndex;
                        break;
                    }
                }
            }
            SimpleDateFormat date2 = icuServiceBuilder.getDateFormat(calendar, formatLengthIndex, 0); // date
            SimpleDateFormat time = icuServiceBuilder.getDateFormat(calendar, 0, formatLengthIndex); // time
            date2.applyPattern(format(value, setBackground(time.toPattern()), setBackground(date2.toPattern())));
            dateFormat = date2;
        } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id) || value == null) {
                return startItalicSymbol + "n/a" + endItalicSymbol;
            } else {
                dateFormat = icuServiceBuilder.getDateFormat(calendar, value);
            }
        }
        dateFormat.setTimeZone(ZONE_SAMPLE);
        return dateFormat.format(DATE_SAMPLE);
    }

    /**
     * Creates examples for decimal formats.
     * 
     * @param value
     * @return
     */
    private String handleDecimalFormat(XPathParts parts, String value, ExampleType type) {
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(value, numberSystem);
        String countValue = parts.getAttributeValue(-1, "count");
        // Match decimal formats.
        if (countValue != null) {
            Count count = Count.valueOf(countValue);
            if (type != ExampleType.ENGLISH &&
                !pluralInfo.getCountToExamplesMap().keySet().contains(count)) {
                return startItalicSymbol + "Superfluous Plural Form" + endItalicSymbol;
            }
            Integer numberSample = getExampleForPattern(numberFormat, count);
            if (numberSample == null) {
                if (type == ExampleType.ENGLISH) {
                    int digits = numberFormat.getMinimumIntegerDigits();
                    return numberFormat.format(1.2345678 * Math.pow(10, digits - 1));
                } else {
                    return startItalicSymbol + "n/a" + endItalicSymbol;
                }
            } else {
                return numberFormat.format(numberSample.doubleValue());
            }
        } else {
            String result = numberFormat.format(NUMBER_SAMPLE);
            result = setBackgroundOnMatch(result, ALL_DIGITS);
            return result;
        }
    }

    /**
     * Calculates a numerical example to use for the specified pattern using
     * brute force (TODO: there should be a more elegant way to do this).
     * 
     * @param format
     * @param count
     * @return
     */
    private Integer getExampleForPattern(DecimalFormat format, Count count) {
        int numDigits = format.getMinimumIntegerDigits();
        int min = (int) Math.pow(10, numDigits - 1);
        int max = min * 10;
        Map<Count, Integer> examples = patternExamples.get(numDigits);
        if (examples == null) {
            patternExamples.put(numDigits, examples = new HashMap<Count, Integer>());
            Set<Count> typesLeft = new HashSet<Count>(pluralInfo.getCountToExamplesMap().keySet());
            // Add at most one example of each type.
            for (int i = min; i < max; ++i) {
                if (typesLeft.isEmpty()) break;
                Count type = Count.valueOf(pluralInfo.getPluralRules().select(i));
                if (!typesLeft.contains(type)) continue;
                examples.put(type, i);
                typesLeft.remove(type);
            }
            // Add zero as an example only if there is no other option.
            if (min == 1) {
                Count type = Count.valueOf(pluralInfo.getPluralRules().select(0));
                if (!examples.containsKey(type)) examples.put(type, 0);
            }
        }
        return examples.get(count);
    }

    private String handleCurrency(String xpath, String value, ExampleContext context, ExampleType type) {
        String currency = parts.getAttributeValue(-2, "type");
        String fullPath = cldrFile.getFullXPath(xpath, false);
        if (parts.contains("symbol")) {
            if (fullPath != null && fullPath.contains("[@choice=\"true\"]")) {
                ChoiceFormat cf = new ChoiceFormat(value);
                value = cf.format(NUMBER_SAMPLE);
            }
            String result;
            DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency, value);
            result = x.format(NUMBER_SAMPLE);
            result = setBackground(result).replace(value, backgroundEndSymbol + value + backgroundStartSymbol);
            return result;
        } else if (parts.contains("displayName")) {
            return formatCountValue(xpath, parts, value, context, type);
        }
        return null;
    }

    private String handleDateRangePattern(String value, String xpath, Zoomed zoomed) {
        String result;
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        result = format(value, setBackground(dateFormat.format(DATE_SAMPLE)),
            setBackground(dateFormat.format(DATE_SAMPLE2)));
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
        } else if (parts.contains("localeDisplayPattern")) {
            result = cldrFile.getName("uz-Arab-AF@timezone=Africa/Addis_Ababa;numbers=arab");
        } else if (parts.contains("languages")) {
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

    public String format(String format, Object... objects) {
        if (format == null) return null;
        return MessageFormat.format(format, objects);
    }

    public static final String unchainException(Exception e) {
        String stackStr = "[unknown stack]<br>";
        try {
            StringWriter asString = new StringWriter();
            e.printStackTrace(new PrintWriter(asString));
            stackStr = "<pre>" + asString.toString() + "</pre>";
        } catch (Throwable tt) {
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
        return backgroundStartSymbol + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
            + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     * 
     * @param patternToEmbed
     *            TODO
     * @param sampleTerritory
     * 
     * @return
     */
    private String setBackgroundExceptMatch(String input, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(input);
        return backgroundStartSymbol + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
            + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     * 
     * @param patternToEmbed
     *            TODO
     * @param sampleTerritory
     * 
     * @return
     */
    private String setBackgroundOnMatch(String inputPattern, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(inputPattern);
        return m.replaceAll(backgroundStartSymbol + "$1" + backgroundEndSymbol);
    }

    /**
     * This is called just before we return a result. It fixes the special characters that were added by setBackground.
     * 
     * @param input
     *            string with special characters from setBackground.
     * @param invert
     *            TODO
     * @return string with HTML for the background.
     */
    private String finalizeBackground(String input) {
        return input == null
            ? input
            : exampleStart +
                TransliteratorUtilities.toHTML.transliterate(input)
                    .replace(backgroundStartSymbol + backgroundEndSymbol, "")
                    // remove null runs
                    .replace(backgroundEndSymbol + backgroundStartSymbol, "")
                    // remove null runs
                    .replace(backgroundStartSymbol, backgroundStart)
                    .replace(backgroundEndSymbol, backgroundEnd)
                    .replace(exampleSeparatorSymbol, exampleEnd + exampleStart)
                    .replace(startItalicSymbol, startItalic)
                    .replace(endItalicSymbol, endItalic)
                + exampleEnd;
    }

    private String invertBackground(String input) {
        if (input == null) {
            return null;
        }
        input = input.replace(backgroundStartSymbol, backgroundTempSymbol)
            .replace(backgroundEndSymbol, backgroundStartSymbol)
            .replace(backgroundTempSymbol, backgroundEndSymbol);

        return backgroundStartSymbol + input + backgroundEndSymbol;
    }

    public static final Pattern PARAMETER = Pattern.compile("(\\{[0-9]\\})");
    public static final Pattern PARAMETER_SKIP0 = Pattern.compile("(\\{[1-9]\\})");
    public static final Pattern ALL_DIGITS = Pattern.compile("(\\p{Nd}+)");

    /**
     * Utility to format using a gmtHourString, gmtFormat, and an integer hours. We only need the hours because that's
     * all
     * the TZDB IDs need. Should merge this eventually into TimeZoneFormatter and call there.
     * 
     * @param gmtHourString
     * @param gmtFormat
     * @param hours
     * @return
     */
    private String getGMTFormat(String gmtHourString, String gmtFormat, int hours) {
        return getGMTFormat(gmtHourString, gmtFormat, hours, 0);
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
        String timeFormat = cldrFile
            .getWinningValue("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
        if (timeFormat == null) {
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
     * href='http://unicode.org/cldr/wiki?SurveyToolHelp/characters'>help</a>. <br>
     * The result is valid HTML. Set listPlaceholders to true to include a
     * HTML-formatted table of all placeholders required in the value.<br>
     * TODO: add more help, and modify to get from property or xml file for easy
     * modification.
     * 
     * @return null if none available.
     */
    public synchronized String getHelpHtml(String xpath, String value, boolean listPlaceholders) {

        // lazy initialization

        if (pathDescription == null) {
            Map<String, List<Set<String>>> starredPaths = new HashMap<String, List<Set<String>>>();
            Map<String, String> extras = new HashMap<String, String>();

            this.pathDescription = new PathDescription(supplementalDataInfo, englishFile, extras, starredPaths,
                PathDescription.ErrorHandling.CONTINUE);

            this.pathDescription = new PathDescription(supplementalDataInfo, englishFile, extras, starredPaths,
                PathDescription.ErrorHandling.CONTINUE);
            if (helpMessages == null) {
                helpMessages = new HelpMessages("test_help_messages.html");
            }
        }

        // now get the description

        Level level = coverageLevel.getLevel(xpath);
        String description = pathDescription.getDescription(xpath, value, level, null);
        if (description == null || description.equals("SKIP")) {
            return null;
        }
        // http://cldr.org/translation/timezones
        int start = 0;
        StringBuilder buffer = new StringBuilder();
        while (URLMatcher.reset(description).find(start)) {
            final String url = URLMatcher.group();
            buffer
                .append(TransliteratorUtilities.toHTML.transliterate(description.substring(start, URLMatcher.start())))
                .append("<a target='CLDR-ST-DOCS' href='")
                .append(url)
                .append("'>")
                .append(url)
                .append("</a>");
            start = URLMatcher.end();
        }
        buffer.append(TransliteratorUtilities.toHTML.transliterate(description.substring(start)));

        if (listPlaceholders) {
            buffer.append(pathDescription.getPlaceholderDescription(xpath));
        }

        return buffer.toString();
        // return helpMessages.find(xpath);
        // if (xpath.contains("/exemplarCharacters")) {
        // result = "The standard exemplar characters are those used in customary writing ([a-z] for English; "
        // + "the auxiliary characters are used in foreign words found in typical magazines, newspapers, &c.; "
        // + "currency auxilliary characters are those used in currency symbols, like 'US$ 1,234'. ";
        // }
        // return result == null ? null : TransliteratorUtilities.toHTML.transliterate(result);
    }

    public synchronized String getHelpHtml(String xpath, String value) {
        return getHelpHtml(xpath, value, false);
    }

    HelpMessages helpMessages;

    /**
     * Private class to get the messages from a help file.
     */
    public static class HelpMessages {
        private static final Matcher CLEANUP_BOOKMARK = Pattern.compile("[^a-zA-Z0-9]").matcher("");

        private static final MessageFormat DEFAULT_HEADER_PATTERN = new MessageFormat("<p>{0}</p>"
            + CldrUtility.LINE_SEPARATOR);

        private static final Matcher HEADER_HTML = Pattern.compile("<h[0-9]>(.*)</h[0-9]>").matcher("");

        List<Matcher> keys = new ArrayList<Matcher>();

        List<String> values = new ArrayList<String>();

        enum Status {
            BASE, BEFORE_CELL, IN_CELL, IN_INSIDE_TABLE
        };

        StringBuilder[] currentColumn = new StringBuilder[2];

        int column = 0;

        /**
         * Create a HelpMessages object from a filename.
         * The file has to be in the format of a table of <keyRegex,htmlText> pairs,
         * where the key is a keyRegex expression and htmlText is arbitrary HTML text. For example:
         * <p>
         * {@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/chart_messages.html} is used for
         * chart messages, where the key is the name of the chart.
         * <p>
         * {@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/test_help_messages.html} is used
         * for help messages in the survey tool, where the key is an xpath.
         * 
         * @param filename
         */
        public HelpMessages(String filename) {
            currentColumn[0] = new StringBuilder();
            currentColumn[1] = new StringBuilder();
            BufferedReader in;
            try {
                in = CldrUtility.getUTF8Data(filename);
                int tableCount = 0;

                boolean inContent = false;
                // if the table level is 1 (we are in the main table), then we look for <td>...</td><td>...</td>. That
                // means that we have column 1 and column 2.

                SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
                StringBuilder result = new StringBuilder();
                boolean hadPop = false;
                main: while (true) {
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

        /**
         * Get message corresponding to a key out of the file set on this object.
         * For many files, the key will be an xpath, but it doesn't have to be.
         * Note that <i>all</i> of pairs of <keyRegex,htmlText> where the key matches keyRegex
         * will be concatenated together in order to get the result.
         * 
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
         * 
         * @param key
         * @param addHeader
         *            true if you want a header formed by looking at all the hN elements.
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
                    result.insert(0, headerPattern.format(new Object[] { header.toString() }));
                }
                return result.toString();
            }
            return null;
        }

        private void addHelpMessages() {
            if (column == 2) { // must have two columns
                try {
                    // remove the first character and the last two characters, since the are >....</
                    String key = currentColumn[0].substring(1, currentColumn[0].length() - 2).trim();
                    String value = currentColumn[1].substring(1, currentColumn[1].length() - 2).trim();
                    if (DEBUG_SHOW_HELP) {
                        System.out.println("{" + key + "} => {" + value + "}");
                    }
                    Matcher m = Pattern.compile(TransliteratorUtilities.fromHTML.transliterate(key), Pattern.COMMENTS)
                        .matcher("");
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

    /**
     * Returns TRUE if Zoomed.IN results in a different value from Zoomed.OUT
     * 
     * @param xpath
     * @return
     */
    public static boolean hasDifferentZoomIn(String xpath) {
        if (xpath.startsWith("//ldml/characters")) {
            return true;
        } else {
            return false;
        }
    }
}
