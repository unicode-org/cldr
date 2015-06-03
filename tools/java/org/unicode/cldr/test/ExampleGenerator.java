package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.CLDRFileTransformer;
import org.unicode.cldr.tool.CLDRFileTransformer.LocaleTransform;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
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
    private static final String ALT_STAND_ALONE = "[@alt=\"stand-alone\"]";

    private static final String EXEMPLAR_CITY_LOS_ANGELES = "//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity";

    private static final boolean SHOW_ERROR = false;

    private static final Pattern URL_PATTERN = Pattern
        .compile("http://[\\-a-zA-Z0-9]+(\\.[\\-a-zA-Z0-9]+)*([/#][\\-a-zA-Z0-9]+)*");

    private final static boolean DEBUG_SHOW_HELP = false;

    private static SupplementalDataInfo supplementalDataInfo;
    private PathDescription pathDescription;

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
    private static final String startSup = "<sup>";
    private static final String endSup = "</sup>";

    private static final String backgroundStartSymbol = "\uE234";
    private static final String backgroundEndSymbol = "\uE235";
    private static final String backgroundTempSymbol = "\uE236";
    private static final String exampleSeparatorSymbol = "\uE237";
    private static final String startItalicSymbol = "\uE238";
    private static final String endItalicSymbol = "\uE239";
    private static final String startSupSymbol = "\uE23A";
    private static final String endSupSymbol = "\uE23B";

    private boolean verboseErrors = false;

    private Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);

    static {
        Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
        calendar.set(1999, 8, 14, 13, 25, 59); // 1999-09-13 13:25:59
        DATE_SAMPLE = calendar.getTime();
        calendar.set(1999, 9, 27, 13, 25, 59); // 1999-09-13 13:25:59
        DATE_SAMPLE2 = calendar.getTime();
    }

    private CLDRFile cldrFile;

    public CLDRFile getCldrFile() {
        return cldrFile;
    }

    private CLDRFile englishFile;
    Matcher URLMatcher = URL_PATTERN.matcher("");

    private Map<String, String> cache = new HashMap<String, String>();

    private static final String NONE = "\uFFFF";

    // Matcher skipMatcher = Pattern.compile(
    // "/localeDisplayNames(?!"
    // ).matcher("");
    private XPathParts parts = new XPathParts();

    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private PluralInfo pluralInfo;

    private PluralSamples patternExamples;

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

        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
    }

    public enum ExampleType {
        NATIVE, ENGLISH
    };

    public static class ExampleContext {
        private Collection<FixedDecimal> exampleCount;

        public void setExampleCount(Collection<FixedDecimal> exampleCount2) {
            this.exampleCount = exampleCount2;
        }

        public Collection<FixedDecimal> getExampleCount() {
            return exampleCount;
        }
    }

    public String getExampleHtml(String xpath, String value) {
        return getExampleHtml(xpath, value, null, null);
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
     * @return
     */
    public String getExampleHtml(String xpath, String value, ExampleContext context, ExampleType type) {
        String cacheKey;
        String result = null;
        try {
            if (CACHING) {
                cacheKey = xpath + "," + value;
                result = cache.get(cacheKey);
                if (result != null) {
                    if (result == NONE) {
                        return null;
                    }
                    return result;
                }
            }
            // If generating examples for an inheritance marker, then we need to find the
            // "real" value to generate from.
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                if (type.equals(ExampleType.ENGLISH)) {
                    value = englishFile.getConstructedBaileyValue(xpath, null, null);                  
                } else {
                    value = cldrFile.getConstructedBaileyValue(xpath, null, null);
                }
            }
            
            // result is null at this point. Get the real value if we can.
            parts.set(xpath);
            if (parts.contains("dateRangePattern")) { // {0} - {1}
                result = handleDateRangePattern(value, xpath);
            } else if (parts.contains("timeZoneNames")) {
                result = handleTimeZoneName(xpath, value);
            } else if (parts.contains("localeDisplayNames")) {
                result = handleDisplayNames(xpath, parts, value);
            } else if (parts.contains("currency")) {
                result = handleCurrency(xpath, value, context, type);
            } else if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
                if (parts.contains("calendar")) {
                    result = handleDateFormatItem(xpath, value);
                } else if (parts.contains("miscPatterns")) {
                    result = handleMiscPatterns(parts, value);
                } else if (parts.contains("numbers")) {
                    if (parts.contains("currencyFormat")) {
                        result = handleCurrencyFormat(parts, value, type);
                    } else {
                        result = handleDecimalFormat(parts, value, type);
                    }
                }
            } else if (parts.getElement(2).contains("symbols")) {
                result = handleNumberSymbol(parts, value);
            } else if (parts.contains("defaultNumberingSystem") || parts.contains("otherNumberingSystems")) {
                result = handleNumberingSystem(value);
            } else if (parts.contains("currencyFormats") && parts.contains("unitPattern")) {
                result = formatCountValue(xpath, parts, value, context, type);
            } else if (parts.getElement(-2).equals("compoundUnit")) {
                result = handleCompoundUnit(getUnitLength(),
                    Count.valueOf(parts.getAttributeValue(-1, "count")),
                    value);
            } else if (parts.getElement(-1).equals("unitPattern")) {
                result = handleFormatUnit(getUnitLength(), Count.valueOf(parts.getAttributeValue(-1, "count")), value);
            } else if (parts.getElement(-1).equals("durationUnitPattern")) {
                result = handleDurationUnit(value);
            } else if (parts.contains("intervalFormats")) {
                result = handleIntervalFormats(parts, xpath, value, context, type);
            } else if (parts.getElement(1).equals("delimiters")) {
                result = handleDelimiters(parts, xpath, value);
            } else if (parts.getElement(1).equals("listPatterns")) {
                result = handleListPatterns(parts, value);
            } else if (parts.getElement(2).equals("ellipsis")) {
                result = handleEllipsis(parts.getAttributeValue(-1, "type"), value);
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

        String test = parts.getElement(-1);

        //add transliteration if one exists
        if (type == ExampleType.NATIVE && result != null) {
            result = addTransliteration(result, value);
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

    private UnitLength getUnitLength() {
        return UnitLength.valueOf(parts.getAttributeValue(-3, "type").toUpperCase(Locale.ENGLISH));
    }

    private String handleFormatUnit(UnitLength unitLength, Count count, String value) {
        FixedDecimal amount = getBest(count);
        if (amount == null) {
            return "n/a";
        }
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return format(value, backgroundStartSymbol + numberFormat.format(amount) + backgroundEndSymbol);
    }

    public String handleCompoundUnit(UnitLength unitLength, Count count, String value) {
        /**
         *  <units>
        <unitLength type="long">
            <alias source="locale" path="../unitLength[@type='short']"/>
        </unitLength>
        <unitLength type="short">
            <compoundUnit type="per">
                <unitPattern count="other">{0}/{1}</unitPattern>
            </compoundUnit>

         *  <compoundUnit type="per">
                <unitPattern count="one">{0}/{1}</unitPattern>
                <unitPattern count="other">{0}/{1}</unitPattern>
            </compoundUnit>
         <unit type="length-m">
                <unitPattern count="one">{0} meter</unitPattern>
                <unitPattern count="other">{0} meters</unitPattern>
            </unit>

         */

        // we want to get a number that works for the count passed in.
        FixedDecimal amount = getBest(count);
        if (amount == null) {
            return "n/a";
        }
        String unit1 = backgroundStartSymbol + getFormattedUnit("length-meter", unitLength, amount) + backgroundEndSymbol;
        String unit2 = backgroundStartSymbol + getFormattedUnit("duration-second", unitLength, new FixedDecimal(1d, 0), "").trim() + backgroundEndSymbol;
        // TODO fix hack
        String form = this.pluralInfo.getPluralRules().select(amount);
        String perPath = "//ldml/units/unitLength" + unitLength.typeString
            + "/compoundUnit[@type=\"per\"]"
            + "/compoundUnitPattern";
        //ldml/units/unitLength[@type="long"]/compoundUnit[@type="per"]/compoundUnitPattern
        return format(getValueFromFormat(perPath, form), unit1, unit2);
    }

    private FixedDecimal getBest(Count count) {
        FixedDecimalSamples samples = pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.DECIMAL);
        if (samples == null) {
            samples = pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.INTEGER);
        }
        if (samples == null) {
            return null;
        }
        Set<FixedDecimalRange> samples2 = samples.getSamples();
        FixedDecimalRange range = samples2.iterator().next();
        return range.end;
    }

    private String handleMiscPatterns(XPathParts parts2, String value) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(0);
        String start = backgroundStartSymbol + numberFormat.format(99) + backgroundEndSymbol;
        if ("range".equals(parts.getAttributeValue(-1, "type"))) {
            String end = backgroundStartSymbol + numberFormat.format(144) + backgroundEndSymbol;
            return format(value, start, end);
        } else {
            return format(value, start);
        }
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
        // listPatternType is either "duration" or null
        String listPatternType = parts.getAttributeValue(-2, "type");
        if (listPatternType == null) {
            return handleRegularListPatterns(parts, value);
        }
        else {
            return handleDurationListPatterns(parts, value, UnitLength.from(listPatternType));
        }
    }

    private String handleRegularListPatterns(XPathParts parts, String value) {
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
        return longListPatternExample(
            listPathFormat, patternType, value, territory1, territory2, territory3, territory4);
    }

    private String handleDurationListPatterns(XPathParts parts, String value, UnitLength unitWidth) {
        String patternType = parts.getAttributeValue(-1, "type");
        String duration1 = getFormattedUnit("duration-day", unitWidth, 4);
        String duration2 = getFormattedUnit("duration-hour", unitWidth, 2);
        if (patternType.equals("2")) {
            return invertBackground(format(setBackground(value), duration1, duration2));
        }
        String duration3 = getFormattedUnit("duration-minute", unitWidth, 37);
        String duration4 = getFormattedUnit("duration-second", unitWidth, 23);
        String listPathFormat = "//ldml/listPatterns/listPattern"
            + unitWidth.listTypeLength.typeString
            + "/listPatternPart[@type=\"{0}\"]";
        return longListPatternExample(
            listPathFormat, patternType, value, duration1, duration2, duration3, duration4);
    }

    public enum ListTypeLength {
        NORMAL(""),
        UNIT_WIDE("[@type=\"unit\"]"),
        UNIT_SHORT("[@type=\"unit-short\"]"),
        UNIT_NARROW("[@type=\"unit-narrow\"]");
        final String typeString;

        ListTypeLength(String typeString) {
            this.typeString = typeString;
        }
    }

    public enum UnitLength {
        LONG(ListTypeLength.UNIT_WIDE),
        SHORT(ListTypeLength.UNIT_SHORT),
        NARROW(ListTypeLength.UNIT_NARROW);
        final String typeString;
        final ListTypeLength listTypeLength;

        UnitLength(ListTypeLength listTypeLength) {
            typeString = "[@type=\"" + name().toLowerCase(Locale.ENGLISH) + "\"]";
            this.listTypeLength = listTypeLength;
        }

        public static UnitLength from(String listPatternType) {
            if (listPatternType.equals("unit")) {
                return UnitLength.LONG;
            } else if (listPatternType.equals("unit-narrow")) {
                return UnitLength.NARROW;
            } else if (listPatternType.equals("unit-short")) {
                return UnitLength.SHORT;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private String getFormattedUnit(String unitType, UnitLength unitWidth, FixedDecimal unitAmount) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return getFormattedUnit(unitType, unitWidth, unitAmount, numberFormat.format(unitAmount));
    }

    private String getFormattedUnit(String unitType, UnitLength unitWidth, double unitAmount) {
        return getFormattedUnit(unitType, unitWidth, new FixedDecimal(unitAmount));
    }

    private String getFormattedUnit(String unitType, UnitLength unitWidth, FixedDecimal unitAmount, String formattedUnitAmount) {
        String form = this.pluralInfo.getPluralRules().select(unitAmount);
        String pathFormat = "//ldml/units/unitLength" + unitWidth.typeString
            + "/unit[@type=\"{0}\"]/unitPattern[@count=\"{1}\"]";
        return format(getValueFromFormat(pathFormat, unitType, form), formattedUnitAmount);
    }

    private String longListPatternExample(String listPathFormat, String patternType, String value, String territory1, String territory2, String territory3,
        String territory4) {
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

    public String handleEllipsis(String type, String value) {
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        //  <ellipsis type="word-final">{0} …</ellipsis>
        //  <ellipsis type="word-initial">… {0}</ellipsis>
        //  <ellipsis type="word-medial">{0} … {1}</ellipsis>
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        // if it isn't a word, break in the middle
        if (!type.contains("word")) {
            territory1 = clip(territory1, 0, 1);
            territory2 = clip(territory2, 1, 0);
        }
        if (type.contains("initial")) {
            territory1 = territory2;
        }
        return invertBackground(format(setBackground(value), territory1, territory2));
    }

    public static String clip(String text, int clipStart, int clipEnd) {
        BreakIterator bi = BreakIterator.getCharacterInstance();
        bi.setText(text);
        for (int i = 0; i < clipStart; ++i) {
            bi.next();
        }
        int start = bi.current();
        bi.last();
        for (int i = 0; i < clipEnd; ++i) {
            bi.previous();
        }
        int end = bi.current();
        return start >= end ? text : text.substring(start, end);
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
        String request = parts.getAttributeValue(-1, "request");
        if (!"Timezone".equals(request)) {
            return null;
        }
        String calendar = parts.getAttributeValue(3, "type");

        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendar, 0, DateFormat.MEDIUM, null);
        String zone = cldrFile.getStringValue("//ldml/dates/timeZoneNames/gmtZeroFormat");
        String result = format(value, setBackground(sdf.format(DATE_SAMPLE)), setBackground(zone));
        return result;
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

    private String handleDurationUnit(String value) {
        //            ULocale locale = new ULocale(this.icuServiceBuilder.getCldrFile().getLocaleID());
        //            SimpleDateFormat df = new SimpleDateFormat(value.replace('h', 'H'), locale);
        DateFormat df = this.icuServiceBuilder.getDateFormat("gregorian", value.replace('h', 'H'));
        df.setTimeZone(TimeZone.GMT_ZONE);
        long time = ((5 * 60 + 37) * 60 + 23) * 1000;
        return df.format(new Date(time));
    }

    static final List<FixedDecimal> CURRENCY_SAMPLES = Arrays.asList(
        new FixedDecimal(1.23),
        new FixedDecimal(0),
        new FixedDecimal(2.34),
        new FixedDecimal(3.45),
        new FixedDecimal(5.67),
        new FixedDecimal(1)
        );

    private String formatCountValue(String xpath, XPathParts parts, String value, ExampleContext context,
        ExampleType type) {
        if (!parts.containsAttribute("count")) { // no examples for items that don't format
            return null;
        }
        final PluralInfo plurals = supplementalDataInfo.getPlurals(cldrFile.getLocaleID());
        PluralRules pluralRules = plurals.getPluralRules();

        String unitType = parts.getAttributeValue(-2, "type");
        if (unitType == null) {
            unitType = "USD"; // sample for currency pattern
        }
        final boolean isPattern = parts.contains("unitPattern");
        final boolean isCurrency = !parts.contains("units");

        Count count = null;
        final LinkedHashSet<FixedDecimal> exampleCount = new LinkedHashSet();
        exampleCount.addAll(CURRENCY_SAMPLES);
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

        // we used to just get the samples for the given keyword, but that doesn't work well any more.
        getStartEndSamples(pluralRules.getDecimalSamples(countString, SampleType.INTEGER), exampleCount);
        getStartEndSamples(pluralRules.getDecimalSamples(countString, SampleType.DECIMAL), exampleCount);

        if (context != null) {
            context.setExampleCount(exampleCount);
        }
        String result = "";
        DecimalFormat currencyFormat = icuServiceBuilder.getCurrencyFormat(unitType);
        int decimalCount = currencyFormat.getMinimumFractionDigits();

        // we will cycle until we have (at most) two examples.
        Set<FixedDecimal> examplesSeen = new HashSet<FixedDecimal>();
        int maxCount = 2;
        main:
            // If we are a currency, we will try to see if we can set the decimals to match.
            // but if nothing works, we will just use a plain sample.
            for (int phase = 0; phase < 2; ++phase) {
                int check = 0;
                for (FixedDecimal example : exampleCount) {
                    // we have to first see whether we have a currency. If so, we have to see if the count works.

                    if (isCurrency && phase == 0) {
                        example = new FixedDecimal(example.source, decimalCount);
                    }
                    // skip if we've done before (can happen because of the currency reset)
                    if (examplesSeen.contains(example)) {
                        continue;
                    }
                    examplesSeen.add(example);
                    // skip if the count isn't appropriate
                    if (!pluralRules.select(example).equals(count.toString())) {
                        continue;
                    }

                    if (value == null) {
                        String fallbackPath = cldrFile.getCountPathWithFallback(xpath, count, true);
                        value = cldrFile.getStringValue(fallbackPath);
                    }
                    String resultItem;

                    resultItem = formatCurrency(value, type, unitType, isPattern, isCurrency, count, example);
                    // now add to list
                    result = addExampleResult(resultItem, result);
                    if (isPattern) {
                        String territory = getDefaultTerritory(type);
                        String currency = supplementalDataInfo.getDefaultCurrency(territory);
                        if (currency.equals(unitType)) {
                            currency = "EUR";
                            if (currency.equals(unitType)) {
                                currency = "JAY";
                            }
                        }
                        resultItem = formatCurrency(value, type, currency, isPattern, isCurrency, count, example);
                        // now add to list
                        result = addExampleResult(resultItem, result);

                    }
                    if (--maxCount < 1) {
                        break main;
                    }
                }
            }
        return result.isEmpty() ? null : result;
    }

    static public void getStartEndSamples(PluralRules.FixedDecimalSamples samples, Set<FixedDecimal> target) {
        if (samples != null) {
            for (FixedDecimalRange item : samples.getSamples()) {
                target.add(item.start);
                target.add(item.end);
            }
        }
    }

    private String formatCurrency(String value, ExampleType type, String unitType, final boolean isPattern, final boolean isCurrency, Count count,
        FixedDecimal example) {
        String resultItem;
        {
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
            unitDecimalFormat.setMaximumFractionDigits(example.getVisibleDecimalDigitCount());
            unitDecimalFormat.setMinimumFractionDigits(example.getVisibleDecimalDigitCount());

            String formattedNumber = unitDecimalFormat.format(example.source);
            unitPatternFormat.setFormatByArgumentIndex(0, unitDecimalFormat);
            resultItem = unitPattern.replace("{0}", formattedNumber).replace("{1}", unitName);

            if (isPattern) {
                resultItem = invertBackground(resultItem);
            }
        }
        return resultItem;
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
        String unitNamePath = cldrFile.getCountPathWithFallback(isCurrency
            ? "//ldml/numbers/currencies/currency[@type=\"" + unitType + "\"]/displayName"
                : "//ldml/units/unit[@type=\"" + unitType + "\"]/unitPattern",
                count, true);
        return cldrFile.getWinningValue(unitNamePath);
    }

    private String handleNumberSymbol(XPathParts parts, String value) {
        String symbolType = parts.getElement(-1);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        int index = 1;// dec/percent/sci
        double numberSample = NUMBER_SAMPLE;
        String originalValue = cldrFile.getWinningValue(parts.toString());
        boolean isSuperscripting = false;
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
        } else if (symbolType.equals("superscriptingExponent")) {
            index = 3;
            isSuperscripting = true;
        } else {
            // We don't need examples for standalone symbols, i.e. infinity and nan.
            // We don't have an example for the list symbol either.
            return null;
        }
        DecimalFormat x = icuServiceBuilder.getNumberFormat(index, numberSystem);
        String example;
        String formattedValue;
        if (isSuperscripting) {
            DecimalFormatSymbols symbols = x.getDecimalFormatSymbols();
            char[] digits = symbols.getDigits();
            x.setDecimalFormatSymbols(symbols);
            x.setNegativeSuffix(endSupSymbol + x.getNegativeSuffix());
            x.setPositiveSuffix(endSupSymbol + x.getPositiveSuffix());
            x.setExponentSignAlwaysShown(false);

            // Don't set the exponent directly because future examples for items
            // will be affected as well.
            originalValue = symbols.getExponentSeparator();
            formattedValue = backgroundEndSymbol + value + digits[1] + digits[0] + backgroundStartSymbol + startSupSymbol;
            example = x.format(numberSample);
        } else {
            x.setExponentSignAlwaysShown(true);
            formattedValue = backgroundEndSymbol + value + backgroundStartSymbol;
        }
        example = x.format(numberSample);
        example = example.replace(originalValue, formattedValue);
        return backgroundStartSymbol + example + backgroundEndSymbol;
    }

    private String handleNumberingSystem(String value) {
        NumberFormat x = icuServiceBuilder.getGenericNumberFormat(value);
        x.setGroupingUsed(false);
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
                boolean singleZone = !supplementalDataInfo.getMultizones().contains(countryCode);
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
            String central = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/metazone[@type=\"America_Central\"]/long/generic"));
            String cancun = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"America/Cancun\"]/exemplarCity"));
            result = format(value, cancun, central);
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
                    boolean singleZone = !(supplementalDataInfo.getMultizones().contains(countryCode));

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

    private String handleDateFormatItem(String xpath, String value) {

        String fullpath = cldrFile.getFullXPath(xpath);
        parts.set(fullpath);

        String calendar = parts.findAttributeValue("calendar", "type");

        if (parts.contains("dateTimeFormat")) {
            String dateFormatXPath = cldrFile.getWinningPath(xpath.replaceAll("dateTimeFormat", "dateFormat"));
            String timeFormatXPath = cldrFile.getWinningPath(xpath.replaceAll("dateTimeFormat", "timeFormat"));
            String dateFormatValue = cldrFile.getWinningValue(dateFormatXPath);
            String timeFormatValue = cldrFile.getWinningValue(timeFormatXPath);
            parts.set(cldrFile.getFullXPath(dateFormatXPath));
            String dateNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            parts.set(cldrFile.getFullXPath(timeFormatXPath));
            String timeNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            SimpleDateFormat df = icuServiceBuilder.getDateFormat(calendar, dateFormatValue, dateNumbersOverride);
            SimpleDateFormat tf = icuServiceBuilder.getDateFormat(calendar, timeFormatValue, timeNumbersOverride);
            df.setTimeZone(ZONE_SAMPLE);
            tf.setTimeZone(ZONE_SAMPLE);
            String dfResult = "'" + df.format(DATE_SAMPLE) + "'";
            String tfResult = "'" + tf.format(DATE_SAMPLE) + "'";
            SimpleDateFormat dtf = icuServiceBuilder.getDateFormat(calendar,
                MessageFormat.format(value, (Object[]) new String[] { setBackground(tfResult), setBackground(dfResult) }));
            return dtf.format(DATE_SAMPLE);
        } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id) || value == null) {
                return startItalicSymbol + "n/a" + endItalicSymbol;
            } else {
                String numbersOverride = parts.findAttributeValue("pattern", "numbers");
                SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendar, value, numbersOverride);
                sdf.setTimeZone(ZONE_SAMPLE);
                String defaultNumberingSystem = cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
                String timeSeparator = cldrFile.getWinningValue("//ldml/numbers/symbols[@numberSystem='"+defaultNumberingSystem+"']/timeSeparator");
                DateFormatSymbols dfs = sdf.getDateFormatSymbols();
                dfs.setTimeSeparatorString(timeSeparator);
                sdf.setDateFormatSymbols(dfs);
                return sdf.format(DATE_SAMPLE);
            }
        }
    }

    /**
     * Creates examples for currency formats.
     * 
     * @param value
     * @return
     */
    private String handleCurrencyFormat(XPathParts parts, String value, ExampleType type) {

        String territory = getDefaultTerritory(type);

        String currency = supplementalDataInfo.getDefaultCurrency(territory);
        String checkPath = "//ldml/numbers/currencies/currency[@type=\"" + currency + "\"]/symbol";
        String currencySymbol = cldrFile.getWinningValue(checkPath);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present

        DecimalFormat df = icuServiceBuilder.getCurrencyFormat(currency, currencySymbol, numberSystem);
        df.applyPattern(value);

        String countValue = parts.getAttributeValue(-1, "count");
        if (countValue != null) {
            return formatCountDecimal(df, countValue);
        }

        double sampleAmount = 1295.00;
        String example = formatNumber(df, sampleAmount);
        example = addExampleResult(formatNumber(df, -sampleAmount), example);

        return example;
    }

    private String getDefaultTerritory(ExampleType type) {
        CLDRLocale loc;
        String territory = "US";
        if (ExampleType.NATIVE.equals(type)) {
            loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
            territory = loc.getCountry();
            if (territory == null || territory.length() == 0) {
                loc = supplementalDataInfo.getDefaultContentFromBase(loc);
                territory = loc.getCountry();
            }
            if (territory == null || territory.length() == 0) {
                territory = "US";
            }
        }
        return territory;
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
        if (countValue != null) {
            return formatCountDecimal(numberFormat, countValue);
        }

        double sampleNum1 = 5.43;
        double sampleNum2 = NUMBER_SAMPLE;
        if (parts.getElement(4).equals("percentFormat")) {
            sampleNum1 = 0.0543;
        }
        String example = formatNumber(numberFormat, sampleNum1);
        example = addExampleResult(formatNumber(numberFormat, sampleNum2), example);
        // have positive and negative
        example = addExampleResult(formatNumber(numberFormat, -sampleNum2), example);
        return example;
    }

    private String formatCountDecimal(DecimalFormat numberFormat, String countValue) {
        Count count = Count.valueOf(countValue);
        Double numberSample = getExampleForPattern(numberFormat, count);
        if (numberSample == null) {
            // Ideally, we would suppress the value in the survey tool.
            // However, until we switch over to the ICU samples, we are not guaranteed
            // that "no samples" means "can't occur". So we manufacture something.
            int digits = numberFormat.getMinimumIntegerDigits();
            numberSample = (double) Math.round(1.2345678901234 * Math.pow(10, digits - 1));
        }
        String temp = String.valueOf(numberSample);
        int fractionLength = temp.endsWith(".0") ? 0 : temp.length() - temp.indexOf('.') - 1;
        if (fractionLength != numberFormat.getMaximumFractionDigits()) {
            numberFormat = (DecimalFormat) numberFormat.clone(); // for safety
            numberFormat.setMinimumFractionDigits(fractionLength);
            numberFormat.setMaximumFractionDigits(fractionLength);
        }
        return formatNumber(numberFormat, numberSample);
    }

    private String formatNumber(DecimalFormat format, double value) {
        String example = format.format(value);
        return setBackgroundOnMatch(example, ALL_DIGITS);
    }

    /**
     * Calculates a numerical example to use for the specified pattern using
     * brute force (TODO: there should be a more elegant way to do this).
     * 
     * @param format
     * @param count
     * @return
     */
    private Double getExampleForPattern(DecimalFormat format, Count count) {
        if (patternExamples == null) {
            patternExamples = PluralSamples.getInstance(cldrFile.getLocaleID());
        }
        int numDigits = format.getMinimumIntegerDigits();
        Map<Count, Double> samples = patternExamples.getSamples(numDigits);
        //        int min = (int) Math.pow(10, numDigits - 1);
        //        int max = min * 10;
        //        Map<Count, Integer> examples = patternExamples.get(numDigits);
        //        if (examples == null) {
        //            patternExamples.put(numDigits, examples = new HashMap<Count, Integer>());
        //            Set<Count> typesLeft = new HashSet<Count>(pluralInfo.getCountToExamplesMap().keySet());
        //            // Add at most one example of each type.
        //            for (int i = min; i < max; ++i) {
        //                if (typesLeft.isEmpty()) break;
        //                Count type = Count.valueOf(pluralInfo.getPluralRules().select(i));
        //                if (!typesLeft.contains(type)) continue;
        //                examples.put(type, i);
        //                typesLeft.remove(type);
        //            }
        //            // Add zero as an example only if there is no other option.
        //            if (min == 1) {
        //                Count type = Count.valueOf(pluralInfo.getPluralRules().select(0));
        //                if (!examples.containsKey(type)) examples.put(type, 0);
        //            }
        //        }
        return samples.get(count);
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

    private String handleDateRangePattern(String value, String xpath) {
        String result;
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        result = format(value, setBackground(dateFormat.format(DATE_SAMPLE)),
            setBackground(dateFormat.format(DATE_SAMPLE2)));
        return result;
    }

    /**
     * @param elementToOverride the element that is to be overridden
     * @param element the overriding element
     * @param value the value to override element with
     * @return
     */
    private String getLocaleDisplayPattern(String elementToOverride, String element, String value) {
        final String localeDisplayPatternPath = "//ldml/localeDisplayNames/localeDisplayPattern/";
        if (elementToOverride.equals(element)) {
            return value;
        } else {
            return cldrFile.getWinningValue(localeDisplayPatternPath + elementToOverride);
        }
    }

    private String handleDisplayNames(String xpath, XPathParts parts, String value) {
        String result = null;
        if (parts.contains("codePatterns")) {
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="language"]
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="script"]
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="territory"]
            String type = parts.getAttributeValue(-1, "type");
            result = format(value, setBackground(
                type.equals("language") ? "ace"
                    : type.equals("script") ? "Avst"
                        : type.equals("territory") ? "057" : "CODE"));
        } else if (parts.contains("localeDisplayPattern")) {
            //ldml/localeDisplayNames/localeDisplayPattern/localePattern
            //ldml/localeDisplayNames/localeDisplayPattern/localeSeparator
            //ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern
            String element = parts.getElement(-1);
            value = setBackground(value);
            String localeKeyTypePattern = getLocaleDisplayPattern("localeKeyTypePattern", element, value);
            String localePattern = getLocaleDisplayPattern("localePattern", element, value);
            String localeSeparator = getLocaleDisplayPattern("localeSeparator", element, value);

            List<String> locales = new ArrayList<String>();
            if (element.equals("localePattern")) {
                locales.add("uz-AF");
            }
            locales.add(element.equals("localeKeyTypePattern") ?
                "uz-Arab@timezone=Africa/Addis_Ababa" : "uz-Arab-AF");
            locales.add("uz-Arab-AF@timezone=Africa/Addis_Ababa;numbers=arab");
            String[] examples = new String[locales.size()];
            for (int i = 0; i < locales.size(); i++) {
                examples[i] = invertBackground(cldrFile.getName(locales.get(i), false,
                    localeKeyTypePattern, localePattern, localeSeparator));
            }
            result = formatExampleList(examples);
        } else if (parts.contains("languages") || parts.contains("scripts") || parts.contains("territories")) {
            //ldml/localeDisplayNames/languages/language[@type="ar"]
            //ldml/localeDisplayNames/scripts/script[@type="Arab"]
            //ldml/localeDisplayNames/territories/territory[@type="CA"]
            String type = parts.getAttributeValue(-1, "type");
            if (type.contains("_")) {
                if (value != null && !value.equals(type)) {
                    result = value;
                } else {
                    result = cldrFile.getConstructedBaileyValue(xpath, null, null);
                }
            } else {
                value = setBackground(value);
                List<String> examples = new ArrayList<String>();
                String nameType = parts.getElement(3);

                Map<String, String> likely = supplementalDataInfo.getLikelySubtags();
                String alt = parts.getAttributeValue(-1, "alt");
                boolean isStandAloneValue = "stand-alone".equals(alt);
                if (!isStandAloneValue) {
                    // only do this if the value is not a stand-alone form
                    String tag = "language".equals(nameType) ? type : "und_" + type;
                    String max = LikelySubtags.maximize(tag, likely);
                    if (max == null) {
                        return null;
                    }
                    LanguageTagParser ltp = new LanguageTagParser().set(max);
                    String languageName = null;
                    String scriptName = null;
                    String territoryName = null;
                    if (nameType.equals("language")) {
                        languageName = value;
                    } else if (nameType.equals("script")) {
                        scriptName = value;
                    } else {
                        territoryName = value;
                    }
                    if (languageName == null) {
                        languageName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, ltp.getLanguage()));
                    }
                    if (scriptName == null) {
                        scriptName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.SCRIPT_NAME, ltp.getScript()));
                    }
                    if (territoryName == null) {
                        territoryName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.TERRITORY_NAME, ltp.getRegion()));
                    }
                    languageName = languageName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                    scriptName = scriptName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                    territoryName = territoryName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');

                    String localePattern = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
                    String localeSeparator = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator");
                    String scriptTerritory = format(localeSeparator, scriptName, territoryName);
                    if (!nameType.equals("script")) {
                        examples.add(invertBackground(format(localePattern, languageName, territoryName)));
                    }
                    if (!nameType.equals("territory")) {
                        examples.add(invertBackground(format(localePattern, languageName, scriptName)));
                    }
                    examples.add(invertBackground(format(localePattern, languageName, scriptTerritory)));
                } else {
                    int x = 0; // debugging
                }
                if (isStandAloneValue || cldrFile.getStringValueWithBailey(xpath + ALT_STAND_ALONE) == null) {
                    // only do this if either it is a stand-alone form,
                    // or it isn't and there is no separate stand-alone form
                    String codePattern = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"" + nameType + "\"]");
                    examples.add(invertBackground(format(codePattern, value)));
                } else {
                    int x = 0; // debugging
                }
                result = formatExampleList(examples.toArray(new String[examples.size()]));
            }
        }
        return result;
    }

    private String formatExampleList(String[] examples) {
        String result = examples[0];
        for (int i = 1, len = examples.length; i < len; i++) {
            result = addExampleResult(examples[i], result);
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
     * This adds the transliteration of a result in case it has one (i.e. sr_Cyrl -> sr_Latn).
     * 
     * @param input
     *            string with special characters from setBackground.
     * @param value
     *            value to be transliterated
     * @return string with attached transliteration if there is one.
     */
    private String addTransliteration(String input, String value) {
        if (value == null) {
            return input;
        }
        for (LocaleTransform localeTransform : LocaleTransform.values()) {

            String locale = cldrFile.getLocaleID();

            if (!(localeTransform.getInputLocale().equals(locale))) {
                continue;
            }

            Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            CLDRFileTransformer transformer = new CLDRFileTransformer(factory, CLDRPaths.COMMON_DIRECTORY + "transforms/");
            Transliterator transliterator = transformer.loadTransliterator(localeTransform);
            return backgroundStartSymbol + "[ " + transliterator.transliterate(value) + " ]" + backgroundEndSymbol + exampleSeparatorSymbol + input;
        }
        return input;
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
                .replace(startSupSymbol, startSup)
                .replace(endSupSymbol, endSup)
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
    public static final Pattern ALL_DIGITS = Pattern.compile("(\\p{Nd}+(.\\p{Nd}+)?)");

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
        String result = format(gmtFormat, hourString);
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

        Level level = CLDRConfig.getInstance().getCoverageInfo().getCoverageLevel(xpath, cldrFile.getLocaleID());
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

    public static String simplify(String exampleHtml) {
        return simplify(exampleHtml, false);
    }

    public static String simplify(String exampleHtml, boolean internal) {
        return exampleHtml == null ? null
            : internal ? "〖" + exampleHtml
                .replace("", "❬")
                .replace("", "❭") + "〗"
                : exampleHtml
                .replace("<div class='cldr_example'>", "〖")
                .replace("</div>", "〗")
                .replace("<span class='cldr_substituted'>", "❬")
                .replace("</span>", "❭");
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
}
