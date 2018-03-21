package org.unicode.cldr.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyNumberInfo;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class ICUServiceBuilder {
    public static Currency NO_CURRENCY = Currency.getInstance("XXX");
    private CLDRFile cldrFile;
    private CLDRFile collationFile;
    private static Map<CLDRLocale, ICUServiceBuilder> ISBMap = new HashMap<CLDRLocale, ICUServiceBuilder>();

    private static TimeZone utc = TimeZone.getTimeZone("GMT");
    private static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", ULocale.ENGLISH);
    static {
        iso.setTimeZone(utc);
    }

    static public String isoDateFormat(Date date) {
        return iso.format(date);
    }

    public static String isoDateFormat(long value) {
        // TODO Auto-generated method stub
        return iso.format(new Date(value));
    }

    static public Date isoDateParse(String date) throws ParseException {
        return iso.parse(date);
    }

    private Map<String, SimpleDateFormat> cacheDateFormats = new HashMap<String, SimpleDateFormat>();
    private Map<String, DateFormatSymbols> cacheDateFormatSymbols = new HashMap<String, DateFormatSymbols>();
    private Map<String, NumberFormat> cacheNumberFormats = new HashMap<String, NumberFormat>();
    private Map<String, DecimalFormatSymbols> cacheDecimalFormatSymbols = new HashMap<String, DecimalFormatSymbols>();
    private SupplementalDataInfo supplementalData;

    // private Factory cldrFactory;
    // public ICUServiceBuilder setCLDRFactory(Factory cldrFactory) {
    // this.cldrFactory = cldrFactory;
    // dateFormatCache.clear();
    // return this; // for chaining
    // }

    private static int[] DateFormatValues = { -1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL };
    private static String[] DateFormatNames = { "none", "short", "medium", "long", "full" };

    public static String getDateNames(int i) {
        return DateFormatNames[i];
    }

    public static int LIMIT_DATE_FORMAT_INDEX = DateFormatValues.length;

    private static final String[] Days = { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };

    // public SimpleDateFormat getDateFormat(CLDRFile cldrFile, int dateIndex, int timeIndex) {
    // //CLDRFile cldrFile = cldrFactory.make(localeID.toString(), true);
    // return getDateFormat(dateIndex, timeIndex);
    // }

    public CLDRFile getCldrFile() {
        return cldrFile;
    }

    public CLDRFile getCollationFile() {
        return collationFile;
    }

    public ICUServiceBuilder setCldrFile(CLDRFile cldrFile) {
        if (!cldrFile.isResolved()) throw new IllegalArgumentException("CLDRFile must be resolved");
        this.cldrFile = cldrFile;
        supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();
        // SupplementalDataInfo.getInstance(this.cldrFile.getSupplementalDirectory());
        cacheDateFormats.clear();
        cacheNumberFormats.clear();
        cacheDateFormatSymbols.clear();
        cacheDecimalFormatSymbols.clear();
        return this;
    }

    public static ICUServiceBuilder forLocale(CLDRLocale locale) {

        ICUServiceBuilder result = ISBMap.get(locale);

        if (result == null) {
            result = new ICUServiceBuilder();

            if (locale != null) {
                result.cldrFile = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*").make(locale.getBaseName(), true);
                result.collationFile = Factory.make(CLDRPaths.COLLATION_DIRECTORY, ".*").makeWithFallback(locale.getBaseName());
            }
            result.supplementalData = SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
            result.cacheDateFormats.clear();
            result.cacheNumberFormats.clear();
            result.cacheDateFormatSymbols.clear();
            result.cacheDecimalFormatSymbols.clear();

            ISBMap.put(locale, result);
        }
        return result;
    }

    public RuleBasedCollator getRuleBasedCollator(String type) throws Exception {
        String rules = "";
        String collationType;
        if ("default".equals(type)) {
            String path = "//ldml/collations/defaultCollation";
            collationType = collationFile.getWinningValueWithBailey(path);
        } else {
            collationType = type;
        }
        String path = "";
        String importPath = "//ldml/collations/collation[@visibility=\"external\"][@type=\"" + collationType + "\"]/import[@type=\"standard\"]";
        if (collationFile.isHere(importPath)) {
            String fullPath = collationFile.getFullXPath(importPath);
            XPathParts xpp = new XPathParts();
            xpp.set(fullPath);
            String importSource = xpp.getAttributeValue(-1, "source");
            String importType = xpp.getAttributeValue(-1, "type");
            CLDRLocale importLocale = CLDRLocale.getInstance(importSource);
            CLDRFile importCollationFile = Factory.make(CLDRPaths.COLLATION_DIRECTORY, ".*").makeWithFallback(importLocale.getBaseName());
            path = "//ldml/collations/collation[@type=\"" + importType + "\"]/cr";
            rules = importCollationFile.getStringValue(path);

        } else {
            path = "//ldml/collations/collation[@type=\"" + collationType + "\"]/cr";
            rules = collationFile.getStringValue(path);
        }
        RuleBasedCollator col;
        if (rules != null && rules.length() > 0)
            col = new RuleBasedCollator(rules);
        else
            col = (RuleBasedCollator) RuleBasedCollator.getInstance();

        return col;
    }

    public RuleBasedCollator getRuleBasedCollator() throws Exception {
        return getRuleBasedCollator("default");
    }

    public SimpleDateFormat getDateFormat(String calendar, int dateIndex, int timeIndex) {
        return getDateFormat(calendar, dateIndex, timeIndex, null);
    }

    public SimpleDateFormat getDateFormat(String calendar, int dateIndex, int timeIndex, String numbersOverride) {
        String key = cldrFile.getLocaleID() + "," + calendar + "," + dateIndex + "," + timeIndex;
        SimpleDateFormat result = (SimpleDateFormat) cacheDateFormats.get(key);
        if (result != null) return (SimpleDateFormat) result.clone();

        String pattern = getPattern(calendar, dateIndex, timeIndex);

        result = getFullFormat(calendar, pattern, numbersOverride);
        cacheDateFormats.put(key, result);
        // System.out.println("created " + key);
        return (SimpleDateFormat) result.clone();
    }

    public SimpleDateFormat getDateFormat(String calendar, String pattern, String numbersOverride) {
        String key = cldrFile.getLocaleID() + "," + calendar + ",," + pattern + ",,," + numbersOverride;
        SimpleDateFormat result = (SimpleDateFormat) cacheDateFormats.get(key);
        if (result != null) return (SimpleDateFormat) result.clone();
        result = getFullFormat(calendar, pattern, numbersOverride);
        cacheDateFormats.put(key, result);
        // System.out.println("created " + key);
        return (SimpleDateFormat) result.clone();
    }

    public SimpleDateFormat getDateFormat(String calendar, String pattern) {
        return getDateFormat(calendar, pattern, null);
    }

    private SimpleDateFormat getFullFormat(String calendar, String pattern, String numbersOverride) {
        ULocale curLocaleWithCalendar = new ULocale(cldrFile.getLocaleID() + "@calendar=" + calendar);
        SimpleDateFormat result = new SimpleDateFormat(pattern, numbersOverride, curLocaleWithCalendar); // formatData
        // TODO Serious Hack, until ICU #4915 is fixed. => It *was* fixed in ICU 3.8, so now use current locale.(?)
        Calendar cal = Calendar.getInstance(curLocaleWithCalendar);
        // TODO look these up and set them
        // cal.setFirstDayOfWeek()
        // cal.setMinimalDaysInFirstWeek()
        cal.setTimeZone(utc);
        result.setCalendar(cal);

        result.setDateFormatSymbols((DateFormatSymbols) _getDateFormatSymbols(calendar).clone());

        // formatData.setZoneStrings();

        NumberFormat numberFormat = result.getNumberFormat();
        if (numberFormat instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) numberFormat;
            df.setGroupingUsed(false);
            df.setDecimalSeparatorAlwaysShown(false);
            df.setParseIntegerOnly(true); /* So that dd.MM.yy can be parsed */
            df.setMinimumFractionDigits(0); // To prevent "Jan 1.00, 1997.00"
        }
        result.setNumberFormat((NumberFormat) numberFormat.clone());
        // Need to put the field specific number format override formatters back in place, since
        // the previous result.setNumberFormat above nukes them.
        if (numbersOverride != null && numbersOverride.indexOf("=") != -1) {
            String[] overrides = numbersOverride.split(",");
            for (String override : overrides) {
                String[] fields = override.split("=", 2);
                if (fields.length == 2) {
                    String overrideField = fields[0].substring(0, 1);
                    ULocale curLocaleWithNumbers = new ULocale(cldrFile.getLocaleID() + "@numbers=" + fields[1]);
                    NumberFormat onf = NumberFormat.getInstance(curLocaleWithNumbers, NumberFormat.NUMBERSTYLE);
                    if (onf instanceof DecimalFormat) {
                        DecimalFormat df = (DecimalFormat) onf;
                        df.setGroupingUsed(false);
                        df.setDecimalSeparatorAlwaysShown(false);
                        df.setParseIntegerOnly(true); /* So that dd.MM.yy can be parsed */
                        df.setMinimumFractionDigits(0); // To prevent "Jan 1.00, 1997.00"
                    }
                    result.setNumberFormat(overrideField, onf);
                }
            }
        }
        return result;
    }

    private DateFormatSymbols _getDateFormatSymbols(String calendar) {
        String key = cldrFile.getLocaleID() + "," + calendar;
        DateFormatSymbols result = cacheDateFormatSymbols.get(key);
        if (result != null) return (DateFormatSymbols) result.clone();

        String[] last;
        // TODO We would also like to be able to set the new symbols leapMonthPatterns & shortYearNames
        // (related to Chinese calendar) to their currently-winning values. Until we have the necessary
        // setters (per ICU ticket #9385) we can't do that. However, we can at least use the values
        // that ICU has for the current locale, instead of using the values that ICU has for root.
        ULocale curLocaleWithCalendar = new ULocale(cldrFile.getLocaleID() + "@calendar=" + calendar);
        DateFormatSymbols formatData = new DateFormatSymbols(curLocaleWithCalendar);

        String prefix = "//ldml/dates/calendars/calendar[@type=\"" + calendar + "\"]/";

        formatData.setAmPmStrings(last = getArrayOfWinningValues(new String[] {
            getDayPeriods(prefix, "format", "wide", "am"),
            getDayPeriods(prefix, "format", "wide", "pm") }));
        checkFound(last);
        // if (last[0] == null && notGregorian) {
        // if (gregorianBackup == null) gregorianBackup = _getDateFormatSymbols("gregorian");
        // formatData.setAmPmStrings(last = gregorianBackup.getAmPmStrings());
        // }

        int minEras = (calendar.equals("chinese") || calendar.equals("dangi")) ? 0 : 1;

        List<String> temp = getArray(prefix + "eras/eraAbbr/era[@type=\"", 0, null, "\"]", minEras);
        formatData.setEras(last = (String[]) temp.toArray(new String[temp.size()]));
        if (minEras != 0) checkFound(last);
        // if (temp.size() < 2 && notGregorian) {
        // if (gregorianBackup == null) gregorianBackup = _getDateFormatSymbols("gregorian");
        // formatData.setEras(last = gregorianBackup.getEras());
        // }

        temp = getArray(prefix + "eras/eraNames/era[@type=\"", 0, null, "\"]", minEras);
        formatData.setEraNames(last = (String[]) temp.toArray(new String[temp.size()]));
        if (minEras != 0) checkFound(last);
        // if (temp.size() < 2 && notGregorian) {
        // if (gregorianBackup == null) gregorianBackup = _getDateFormatSymbols("gregorian");
        // formatData.setEraNames(last = gregorianBackup.getEraNames());
        // }

        formatData.setMonths(getArray(prefix, "month", "format", "wide"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.WIDE);
        formatData.setMonths(getArray(prefix, "month", "format", "abbreviated"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.ABBREVIATED);
        formatData.setMonths(getArray(prefix, "month", "format", "narrow"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.NARROW);

        formatData.setMonths(getArray(prefix, "month", "stand-alone", "wide"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.WIDE);
        formatData.setMonths(getArray(prefix, "month", "stand-alone", "abbreviated"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.ABBREVIATED);
        formatData.setMonths(getArray(prefix, "month", "stand-alone", "narrow"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.NARROW);

        // formatData.setWeekdays(getArray(prefix, "day", "format", "wide"));
        // if (last == null && notGregorian) {
        // if (gregorianBackup == null) gregorianBackup = _getDateFormatSymbols("gregorian");
        // formatData.setWeekdays(gregorianBackup.getWeekdays());
        // }

        formatData.setWeekdays(getArray(prefix, "day", "format", "wide"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.WIDE);
        formatData.setWeekdays(getArray(prefix, "day", "format", "abbreviated"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.ABBREVIATED);
        formatData.setWeekdays(getArray(prefix, "day", "format", "narrow"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.NARROW);

        formatData.setWeekdays(getArray(prefix, "day", "stand-alone", "wide"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.WIDE);
        formatData.setWeekdays(getArray(prefix, "day", "stand-alone", "abbreviated"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.ABBREVIATED);
        formatData.setWeekdays(getArray(prefix, "day", "stand-alone", "narrow"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.NARROW);

        // quarters

        formatData.setQuarters(getArray(prefix, "quarter", "format", "wide"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.WIDE);
        formatData.setQuarters(getArray(prefix, "quarter", "format", "abbreviated"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.ABBREVIATED);
        formatData.setQuarters(getArray(prefix, "quarter", "format", "narrow"), DateFormatSymbols.FORMAT,
            DateFormatSymbols.NARROW);

        formatData.setQuarters(getArray(prefix, "quarter", "stand-alone", "wide"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.WIDE);
        formatData.setQuarters(getArray(prefix, "quarter", "stand-alone", "abbreviated"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.ABBREVIATED);
        formatData.setQuarters(getArray(prefix, "quarter", "stand-alone", "narrow"), DateFormatSymbols.STANDALONE,
            DateFormatSymbols.NARROW);

        cacheDateFormatSymbols.put(key, formatData);
        return formatData;
    }

    /**
     * Example from en.xml
     * <dayPeriods>
     * <dayPeriodContext type="format">
     * <dayPeriodWidth type="wide">
     * <dayPeriod type="am">AM</dayPeriod>
     * <dayPeriod type="am" alt="variant">a.m.</dayPeriod>
     * <dayPeriod type="pm">PM</dayPeriod>
     * <dayPeriod type="pm" alt="variant">p.m.</dayPeriod>
     * </dayPeriodWidth>
     * </dayPeriodContext>
     * </dayPeriods>
     */
    private String getDayPeriods(String prefix, String context, String width, String type) {
        return prefix + "dayPeriods/dayPeriodContext[@type=\"" + context + "\"]/dayPeriodWidth[@type=\"" +
            width + "\"]/dayPeriod[@type=\"" + type + "\"]";
    }

    private String[] getArrayOfWinningValues(String[] xpaths) {
        String result[] = new String[xpaths.length];
        for (int i = 0; i < xpaths.length; i++) {
            result[i] = cldrFile.getWinningValueWithBailey(xpaths[i]);
        }
        checkFound(result, xpaths);
        return result;
    }

    private void checkFound(String[] last) {
        if (last == null || last.length == 0 || last[0] == null) {
            throw new IllegalArgumentException("Failed to load array");
        }
    }

    private void checkFound(String[] last, String[] xpaths) {
        if (last == null || last.length == 0 || last[0] == null) {
            throw new IllegalArgumentException("Failed to load array {" + xpaths[0] + ",...}");
        }
    }

    private String getPattern(String calendar, int dateIndex, int timeIndex) {
        String pattern;
        if (DateFormatValues[timeIndex] == -1)
            pattern = getDateTimePattern(calendar, "date", DateFormatNames[dateIndex]);
        else if (DateFormatValues[dateIndex] == -1)
            pattern = getDateTimePattern(calendar, "time", DateFormatNames[timeIndex]);
        else {
            String p0 = getDateTimePattern(calendar, "time", DateFormatNames[timeIndex]);
            String p1 = getDateTimePattern(calendar, "date", DateFormatNames[dateIndex]);
            String datetimePat = getDateTimePattern(calendar, "dateTime", DateFormatNames[dateIndex]);
            pattern = MessageFormat.format(datetimePat, (Object[]) new String[] { p0, p1 });
        }
        return pattern;
    }

    /**
     * @param calendar
     *            TODO
     *
     */
    private String getDateTimePattern(String calendar, String dateOrTime, String type) {
        type = "[@type=\"" + type + "\"]";
        String key = "//ldml/dates/calendars/calendar[@type=\"" + calendar + "\"]/"
            + dateOrTime + "Formats/"
            + dateOrTime + "FormatLength"
            + type + "/" + dateOrTime + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";
        // change standard to a choice

        String value = cldrFile.getWinningValueWithBailey(key);
        if (value == null)
            throw new IllegalArgumentException("locale: " + cldrFile.getLocaleID() + "\tpath: " + key
                + CldrUtility.LINE_SEPARATOR + "value: " + value);
        return value;
    }

    // enum ArrayType {day, month, quarter};

    private String[] getArray(String key, String type, String context, String width) {
        String prefix = key + type + "s/"
            + type + "Context[@type=\"" + context + "\"]/"
            + type + "Width[@type=\"" + width + "\"]/"
            + type + "[@type=\"";
        String postfix = "\"]";
        boolean isDay = type.equals("day");
        final int arrayCount = isDay ? 7 : type.equals("month") ? 12 : 4;
        List<String> temp = getArray(prefix, isDay ? 0 : 1, isDay ? Days : null, postfix, arrayCount);
        if (isDay) temp.add(0, "");
        String[] result = (String[]) temp.toArray(new String[temp.size()]);
        checkFound(result);
        return result;
    }

    static final Matcher gregorianMonthsMatcher = PatternCache.get(".*gregorian.*months.*").matcher("");

    private List<String> getArray(String prefix, int firstIndex, String[] itemNames, String postfix, int minimumSize) {
        List<String> result = new ArrayList<String>();
        String lastType;
        for (int i = firstIndex;; ++i) {
            lastType = itemNames != null && i < itemNames.length ? itemNames[i] : String.valueOf(i);
            String item = cldrFile.getWinningValueWithBailey(prefix + lastType + postfix);
            if (item == null) break;
            result.add(item);
        }
        // the following code didn't do anything, so I'm wondering what it was there for?
        // it's to catch errors
        if (result.size() < minimumSize) {
            throw new RuntimeException("Internal Error: ICUServiceBuilder.getArray():" + cldrFile.getLocaleID() + " "
                + prefix + lastType + postfix + " - result.size=" + result.size() + ", less than acceptable minimum "
                + minimumSize);
            // Collection s = CollectionUtilities.addAll(cldrFile.iterator(prefix), new
            // TreeSet());//cldrFile.keySet(".*gregorian.*months.*", );
            // String item = cldrFile.getWinningValueWithBailey(prefix + lastType + postfix);
            // throw new IllegalArgumentException("Can't find enough items");
        }
        /*
         * <months>
         * <monthContext type="format">
         * <monthWidth type="abbreviated">
         * <month type="1">1</month>
         */
        return result;
    }

    private static String[] NumberNames = { "integer", "decimal", "percent", "scientific" }; // // "standard", , "INR",

    public String getNumberNames(int i) {
        return NumberNames[i];
    }

    public static int LIMIT_NUMBER_INDEX = NumberNames.length;

    private static class MyCurrency extends Currency {
        String symbol;
        String displayName;
        int fractDigits;
        double roundingIncrement;

        MyCurrency(String code, String symbol, String displayName, CurrencyNumberInfo currencyNumberInfo) {
            super(code);
            this.symbol = symbol == null ? code : symbol;
            this.displayName = displayName == null ? code : displayName;
            this.fractDigits = currencyNumberInfo.getDigits();
            this.roundingIncrement = currencyNumberInfo.getRoundingIncrement();
        }

        public String getName(ULocale locale,
            int nameStyle,
            boolean[] isChoiceFormat) {

            String result = nameStyle == 0 ? this.symbol
                : nameStyle == 1 ? getCurrencyCode()
                    : nameStyle == 2 ? displayName
                        : null;
            if (result == null) throw new IllegalArgumentException();
            // snagged from currency
            if (isChoiceFormat != null) {
                isChoiceFormat[0] = false;
            }
            int i = 0;
            while (i < result.length() && result.charAt(i) == '=' && i < 2) {
                ++i;
            }
            if (isChoiceFormat != null) {
                isChoiceFormat[0] = (i == 1);
            }
            if (i != 0) {
                // Skip over first mark
                result = result.substring(1);
            }
            return result;
        }

        /**
         * Returns the rounding increment for this currency, or 0.0 if no
         * rounding is done by this currency.
         *
         * @return the non-negative rounding increment, or 0.0 if none
         * @stable ICU 2.2
         */
        public double getRoundingIncrement() {
            return roundingIncrement;
        }

        public int getDefaultFractionDigits() {
            return fractDigits;
        }

        public boolean equals(Object other) {
            MyCurrency that = (MyCurrency) other;
            return roundingIncrement == that.roundingIncrement
                && fractDigits == that.fractDigits
                && symbol.equals(that.symbol)
                && displayName.equals(that.displayName);
        }

        private int hashCode(Object other) {
            MyCurrency that = (MyCurrency) other;
            return (((int) roundingIncrement * 37 + fractDigits) * 37 + symbol.hashCode()) * 37
                + displayName.hashCode();
        }
    }

    static int CURRENCY = 0, OTHER_KEY = 1, PATTERN = 2;

    public DecimalFormat getCurrencyFormat(String currency) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(currency, CURRENCY, null, null);
    }

    public DecimalFormat getCurrencyFormat(String currency, String currencySymbol) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(currency, CURRENCY, currencySymbol, null);
    }

    public DecimalFormat getCurrencyFormat(String currency, String currencySymbol, String numberSystem) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(currency, CURRENCY, currencySymbol, numberSystem);
    }

    public DecimalFormat getLongCurrencyFormat(String currency) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(currency, CURRENCY, null, null);
    }

    public DecimalFormat getNumberFormat(int index) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(NumberNames[index], OTHER_KEY, null, null);
    }

    public DecimalFormat getNumberFormat(int index, String numberSystem) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(NumberNames[index], OTHER_KEY, null, numberSystem);
    }

    public NumberFormat getGenericNumberFormat(String ns) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        NumberFormat result = (NumberFormat) cacheNumberFormats.get(cldrFile.getLocaleID() + "@numbers=" + ns);
        if (result != null) return result;
        ULocale ulocale = new ULocale(cldrFile.getLocaleID() + "@numbers=" + ns);
        result = NumberFormat.getInstance(ulocale);
        cacheNumberFormats.put(cldrFile.getLocaleID() + "@numbers=" + ns, result);
        return result;
    }

    public DecimalFormat getNumberFormat(String pattern) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(pattern, PATTERN, null, null);
    }

    public DecimalFormat getNumberFormat(String pattern, String numberSystem) {
        // CLDRFile cldrFile = cldrFactory.make(localeID, true);
        return _getNumberFormat(pattern, PATTERN, null, numberSystem);
    }

    private DecimalFormat _getNumberFormat(String key1, int kind, String currencySymbol, String numberSystem) {
        String localeIDString = (numberSystem == null) ? cldrFile.getLocaleID() : cldrFile.getLocaleID() + "@numbers="
            + numberSystem;
        ULocale ulocale = new ULocale(localeIDString);
        String key = (currencySymbol == null) ? ulocale + "/" + key1 + "/" + kind : ulocale + "/" + key1 + "/" + kind
            + "/" + currencySymbol;
        DecimalFormat result = (DecimalFormat) cacheNumberFormats.get(key);
        if (result != null) {
            return result;
        }

        String pattern = kind == PATTERN ? key1 : getPattern(key1, kind);

        DecimalFormatSymbols symbols = _getDecimalFormatSymbols(numberSystem);
        /*
         * currencySymbol.equals(other.currencySymbol) &&
         * intlCurrencySymbol.equals(other.intlCurrencySymbol) &&
         * padEscape == other.padEscape && // [NEW]
         * monetarySeparator == other.monetarySeparator);
         */
        MyCurrency mc = null;
        if (kind == CURRENCY) {
            // in this case numberSystem is null and symbols are for the default system
            // ^^^^^ NO, that is not true.

            String prefix = "//ldml/numbers/currencies/currency[@type=\"" + key1 + "\"]/";
            // /ldml/numbers/currencies/currency[@type="GBP"]/symbol
            // /ldml/numbers/currencies/currency[@type="GBP"]

            if (currencySymbol == null) {
                currencySymbol = cldrFile.getWinningValueWithBailey(prefix + "symbol");
            }
            String currencyDecimal = cldrFile.getWinningValueWithBailey(prefix + "decimal");
            if (currencyDecimal != null) {
                (symbols = cloneIfNeeded(symbols)).setMonetaryDecimalSeparator(currencyDecimal.charAt(0));
            }
            String currencyPattern = cldrFile.getWinningValueWithBailey(prefix + "pattern");
            if (currencyPattern != null) {
                pattern = currencyPattern;
            }

            String currencyGrouping = cldrFile.getWinningValueWithBailey(prefix + "grouping");
            if (currencyGrouping != null) {
                (symbols = cloneIfNeeded(symbols)).setMonetaryGroupingSeparator(currencyGrouping.charAt(0));
            }

            // <decimal>,</decimal>
            // <group>.</group>

            // TODO This is a hack for now, since I am ignoring the possibility of quoted text next to the symbol
            if (pattern.contains(";")) { // multi pattern
                String[] pieces = pattern.split(";");
                for (int i = 0; i < pieces.length; ++i) {
                    pieces[i] = fixCurrencySpacing(pieces[i], currencySymbol);
                }
                pattern = org.unicode.cldr.util.CldrUtility.join(pieces, ";");
            } else {
                pattern = fixCurrencySpacing(pattern, currencySymbol);
            }

            CurrencyNumberInfo info = supplementalData.getCurrencyNumberInfo(key1);

            mc = new MyCurrency(key1,
                currencySymbol,
                cldrFile.getWinningValueWithBailey(prefix + "displayName"),
                info);

            // String possible = null;
            // possible = cldrFile.getWinningValueWithBailey(prefix + "decimal");
            // symbols.setMonetaryDecimalSeparator(possible != null ? possible.charAt(0) :
            // symbols.getDecimalSeparator());
            // if ((possible = cldrFile.getWinningValueWithBailey(prefix + "pattern")) != null) pattern = possible;
            // if ((possible = cldrFile.getWinningValueWithBailey(prefix + "group")) != null)
            // symbols.setGroupingSeparator(possible.charAt(0));
            // ;
        }
        result = new DecimalFormat(pattern, symbols);
        if (mc != null) {
            result.setCurrency(mc);
            result.setMaximumFractionDigits(mc.getDefaultFractionDigits());
            result.setMinimumFractionDigits(mc.getDefaultFractionDigits());
        } else {
            result.setCurrency(NO_CURRENCY);
        }

        if (false) {
            System.out.println("creating " + ulocale + "\tkey: " + key + "\tpattern "
                + pattern + "\tresult: " + result.toPattern() + "\t0=>" + result.format(0));
            DecimalFormat n2 = (DecimalFormat) NumberFormat.getScientificInstance(ulocale);
            System.out.println("\tresult: " + n2.toPattern() + "\t0=>" + n2.format(0));
        }
        if (kind == OTHER_KEY && key1.equals("integer")) {
            result.setMaximumFractionDigits(0);
            result.setDecimalSeparatorAlwaysShown(false);
            result.setParseIntegerOnly(true);
        }
        cacheNumberFormats.put(key, result);
        return result;
    }

    private String fixCurrencySpacing(String pattern, String symbol) {
        int startPos = pattern.indexOf('\u00a4');
        if (startPos > 0
            && beforeCurrencyMatch.contains(UTF16.charAt(symbol, 0))) {
            int ch = UTF16.charAt(pattern, startPos - 1);
            if (ch == '#') ch = '0';// fix pattern
            if (beforeSurroundingMatch.contains(ch)) {
                pattern = pattern.substring(0, startPos) + beforeInsertBetween + pattern.substring(startPos);
            }
        }
        int endPos = pattern.lastIndexOf('\u00a4') + 1;
        if (endPos < pattern.length()
            && afterCurrencyMatch.contains(UTF16.charAt(symbol, symbol.length() - 1))) {
            int ch = UTF16.charAt(pattern, endPos);
            if (ch == '#') ch = '0';// fix pattern
            if (afterSurroundingMatch.contains(ch)) {
                pattern = pattern.substring(0, endPos) + afterInsertBetween + pattern.substring(endPos);
            }
        }
        return pattern;
    }

    private DecimalFormatSymbols cloneIfNeeded(DecimalFormatSymbols symbols) {
        if (symbols == _getDecimalFormatSymbols(null)) {
            return (DecimalFormatSymbols) symbols.clone();
        }
        return symbols;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols(String numberSystem) {
        return (DecimalFormatSymbols) _getDecimalFormatSymbols(numberSystem).clone();
    }

    private DecimalFormatSymbols _getDecimalFormatSymbols(String numberSystem) {
        String key = (numberSystem == null) ? cldrFile.getLocaleID() : cldrFile.getLocaleID() + "@numbers="
            + numberSystem;
        DecimalFormatSymbols symbols = (DecimalFormatSymbols) cacheDecimalFormatSymbols.get(key);
        if (symbols != null) return symbols;

        symbols = new DecimalFormatSymbols();
        if (numberSystem == null) {
            numberSystem = cldrFile.getWinningValueWithBailey("//ldml/numbers/defaultNumberingSystem");
        }

        // currently constants
        // symbols.setPadEscape(cldrFile.getWinningValueWithBailey("//ldml/numbers/symbols/xxx"));
        // symbols.setSignificantDigit(cldrFile.getWinningValueWithBailey("//ldml/numbers/symbols/patternDigit"));

        symbols.setDecimalSeparator(getSymbolCharacter("decimal", numberSystem));
        // symbols.setDigit(getSymbolCharacter("patternDigit", numberSystem));
        symbols.setExponentSeparator(getSymbolString("exponential", numberSystem));
        symbols.setGroupingSeparator(getSymbolCharacter("group", numberSystem));
        symbols.setInfinity(getSymbolString("infinity", numberSystem));
        symbols.setMinusSignString(getSymbolString("minusSign", numberSystem));
        symbols.setNaN(getSymbolString("nan", numberSystem));
        symbols.setPatternSeparator(getSymbolCharacter("list", numberSystem));
        symbols.setPercentString(getSymbolString("percentSign", numberSystem));
        symbols.setPerMill(getSymbolCharacter("perMille", numberSystem));
        symbols.setPlusSignString(getSymbolString("plusSign", numberSystem));
        // symbols.setZeroDigit(getSymbolCharacter("nativeZeroDigit", numberSystem));
        String digits = supplementalData.getDigits(numberSystem);
        if (digits != null && digits.length() == 10) {
            symbols.setZeroDigit(digits.charAt(0));
        }

        try {
            symbols.setMonetaryDecimalSeparator(getSymbolCharacter("currencyDecimal", numberSystem));
        } catch (IllegalArgumentException e) {
            symbols.setMonetaryDecimalSeparator(symbols.getDecimalSeparator());
        }

        try {
            symbols.setMonetaryGroupingSeparator(getSymbolCharacter("currencyGroup", numberSystem));
        } catch (IllegalArgumentException e) {
            symbols.setMonetaryGroupingSeparator(symbols.getGroupingSeparator());
        }

        String prefix = "//ldml/numbers/currencyFormats/currencySpacing/beforeCurrency/";
        beforeCurrencyMatch = new UnicodeSet(cldrFile.getWinningValueWithBailey(prefix + "currencyMatch"));
        beforeSurroundingMatch = new UnicodeSet(cldrFile.getWinningValueWithBailey(prefix + "surroundingMatch"));
        beforeInsertBetween = cldrFile.getWinningValueWithBailey(prefix + "insertBetween");
        prefix = "//ldml/numbers/currencyFormats/currencySpacing/afterCurrency/";
        afterCurrencyMatch = new UnicodeSet(cldrFile.getWinningValueWithBailey(prefix + "currencyMatch"));
        afterSurroundingMatch = new UnicodeSet(cldrFile.getWinningValueWithBailey(prefix + "surroundingMatch"));
        afterInsertBetween = cldrFile.getWinningValueWithBailey(prefix + "insertBetween");

        cacheDecimalFormatSymbols.put(key, symbols);

        return symbols;
    }

    private char getSymbolCharacter(String key, String numsys) {
        // numsys should not be null (previously resolved to defaultNumberingSystem if necessary)
        return getSymbolString(key, numsys).charAt(0);
    }

    // TODO no longer used now that http://bugs.icu-project.org/trac/ticket/10368 is done.
    private char getHackSymbolCharacter(String key, String numsys) {
        String minusString = getSymbolString(key, numsys);
        char minusSign = (minusString.length() > 1 && isBidiMark(minusString.charAt(0))) ? minusString.charAt(1) : minusString.charAt(0);
        return minusSign;
    }

    private static boolean isBidiMark(char c) {
        return (c == '\u200E' || c == '\u200F' || c == '\u061C');
    }

    private String getSymbolString(String key, String numsys) {
        // numsys should not be null (previously resolved to defaultNumberingSystem if necessary)
        String value = null;
        try {
            value = cldrFile.getWinningValueWithBailey("//ldml/numbers/symbols[@numberSystem=\"" + numsys + "\"]/" + key);
            if (value == null || value.length() < 1) {
                throw new RuntimeException();
            }
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Illegal value <" + value + "> at "
                + "//ldml/numbers/symbols[@numberSystem='" + numsys + "']/" + key);
        }
    }

    UnicodeSet beforeCurrencyMatch;
    UnicodeSet beforeSurroundingMatch;
    String beforeInsertBetween;
    UnicodeSet afterCurrencyMatch;
    UnicodeSet afterSurroundingMatch;
    String afterInsertBetween;

    private String getPattern(String key1, int isCurrency) {
        String prefix = "//ldml/numbers/";
        String type = key1;
        if (isCurrency == CURRENCY)
            type = "currency";
        else if (key1.equals("integer")) type = "decimal";
        String path = prefix
            + type + "Formats/"
            + type + "FormatLength/"
            + type + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";

        String pattern = cldrFile.getWinningValueWithBailey(path);
        if (pattern == null)
            throw new IllegalArgumentException("locale: " + cldrFile.getLocaleID() + "\tpath: " + path);
        return pattern;
    }

    public enum Width {
        wide, abbreviated, narrow
    }

    public enum Context {
        format, stand_alone;
        public String toString() {
            return name().replace('_', '-');
        }
    }

    /**
     * Format a dayPeriod string. The dayPeriodOverride, if null, will be fetched from the file.
     * @param timeInDay
     * @param dayPeriodString
     * @return
     */
    public String formatDayPeriod(int timeInDay, Context context, Width width) {
        DayPeriodInfo dayPeriodInfo = supplementalData.getDayPeriods(DayPeriodInfo.Type.format, cldrFile.getLocaleID());
        DayPeriod period = dayPeriodInfo.getDayPeriod(timeInDay);
        String dayPeriodFormatString = getDayPeriodValue(getDayPeriodPath(period, context, width), "�", null);
        String result = formatDayPeriod(timeInDay, dayPeriodFormatString);
        return result;
    }

    public String getDayPeriodValue(String path, String fallback, Output<Boolean> real) {
        String dayPeriodFormatString = cldrFile.getStringValue(path);
        if (dayPeriodFormatString == null) {
            dayPeriodFormatString = fallback;
        }
        if (real != null) {
            Status status = new Status();
            String locale = cldrFile.getSourceLocaleID(path, status);
            real.value = status.pathWhereFound.equals(path) && cldrFile.getLocaleID().equals(locale);
        }
        return dayPeriodFormatString;
    }

    public static String getDayPeriodPath(DayPeriod period, Context context, Width width) {
        String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\""
            + context
            + "\"]/dayPeriodWidth[@type=\""
            + width
            + "\"]/dayPeriod[@type=\""
            + period
            + "\"]";
        return path;
    }

    static final String SHORT_PATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
    static final String HM_PATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"hm\"]";
    static final String BHM_PATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"Bhm\"]";

    public String formatDayPeriod(int timeInDay, String dayPeriodFormatString) {
        String pattern = null;
        if ((timeInDay % 6) != 0) { // need a better way to test for this
            // dayPeriods other than am, pm, noon, midnight (want patterns with B)
            pattern = cldrFile.getStringValue(BHM_PATH);
            if (pattern != null) {
                pattern = pattern.replace('B', '\uE000');
            }
        }
        if (pattern == null) {
            // dayPeriods am, pm, noon, midnight (want patterns with a)
            pattern = cldrFile.getStringValue(HM_PATH);
            if (pattern != null) {
                pattern = pattern.replace('a', '\uE000');
            }
        }
        if (pattern == null) {
            pattern = "h:mm \uE000";
        }
        SimpleDateFormat df = getDateFormat("gregorian", pattern);
        String formatted = df.format(timeInDay);
        String result = formatted.replace("\uE000", dayPeriodFormatString);
        return result;
    }

    public String getMinusSign(String numberSystem) {
        return _getDecimalFormatSymbols(numberSystem).getMinusSignString();
    }
}
