package org.unicode.cldr.util;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.CLDRFile.Factory;

public class ICUServiceBuilder {
    public static Currency NO_CURRENCY = Currency.getInstance("XXX");

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

    private Map dateFormatCache = new HashMap();
	
//    private Factory cldrFactory;
//	public ICUServiceBuilder setCLDRFactory(Factory cldrFactory) {
//		this.cldrFactory = cldrFactory;
//		dateFormatCache.clear();
//		return this; // for chaining
//	}
	
    private static int[] DateFormatValues = {-1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
    private static String[] DateFormatNames = {"none", "short", "medium", "long", "full"};
    
    public static String getDateNames(int i) {
    	return DateFormatNames[i];
    }

    public static int LIMIT_DATE_FORMAT_INDEX = DateFormatValues.length;
    
    private static final String[] Days = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

//    public SimpleDateFormat getDateFormat(CLDRFile cldrFile, int dateIndex, int timeIndex) {
//    	//CLDRFile cldrFile = cldrFactory.make(localeID.toString(), true);
//    	return getDateFormat(cldrFile, dateIndex, timeIndex);
//    }
    
    public SimpleDateFormat getDateFormat(CLDRFile cldrFile, int dateIndex, int timeIndex) {
    	String key = cldrFile.getLocaleID() + "," + dateIndex + "," + timeIndex;
    	SimpleDateFormat result = (SimpleDateFormat) dateFormatCache.get(key);
    	if (result != null) return (SimpleDateFormat) result.clone();
    	if (false && dateIndex == 2 && timeIndex == 0) {
    		System.out.println("");
    	}
    	
        //Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale.toString(), false, false, false);
        //Node dates = LDMLUtilities.getNode(doc, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]");

        String pattern;
        if (DateFormatValues[timeIndex] == -1) pattern = getDateTimePattern(cldrFile, "date", DateFormatNames[dateIndex]);
        else if (DateFormatValues[dateIndex] == -1) pattern = getDateTimePattern(cldrFile, "time", DateFormatNames[timeIndex]);
        else {
        	String p1 = getDateTimePattern(cldrFile, "date", DateFormatNames[dateIndex]);
        	String p2 = getDateTimePattern(cldrFile, "time", DateFormatNames[timeIndex]);
        	String p3 = getDateTimePattern(cldrFile, "dateTime", "");
        	pattern = MessageFormat.format(p3, new String[]{p2, p1});
        }
        
        DateFormatSymbols formatData = new DateFormatSymbols();
        String prefix = "/ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]";
        formatData.setAmPmStrings(new String[] {
        		cldrFile.getStringValue(prefix + "/am"),
				cldrFile.getStringValue(prefix + "/pm")});
        
        formatData.setEras(new String[] {
        		cldrFile.getStringValue(prefix + "/eras/eraAbbr/era[@type=\"0\"]"),
				cldrFile.getStringValue(prefix + "/eras/eraAbbr/era[@type=\"1\"]")});
        
        //formatData.setLocalPatternChars("");
        
        formatData.setMonths(getArray(cldrFile, prefix, "month", "wide"));
        formatData.setShortMonths(getArray(cldrFile, prefix, "month", "narrow"));
        formatData.setShortWeekdays(getArray(cldrFile, prefix, "day", "narrow"));
        formatData.setWeekdays(getArray(cldrFile, prefix, "day", "wide"));
        //formatData.setZoneStrings();   
        
        
        DecimalFormat numberFormat = (DecimalFormat) getNumberFormat(cldrFile, 1);
        numberFormat.setGroupingUsed(false);
        numberFormat.setDecimalSeparatorAlwaysShown(false);
        numberFormat.setParseIntegerOnly(true); /* So that dd.MM.yy can be parsed */
        numberFormat.setMinimumFractionDigits(0); // To prevent "Jan 1.00, 1997.00"
        
		result = new SimpleDateFormat(pattern, new ULocale(cldrFile.getLocaleID())); // formatData
		result.setNumberFormat((NumberFormat)numberFormat.clone());
        result.setTimeZone(utc);
		dateFormatCache.put(key, result);
		//System.out.println("created " + key);
        return result;
    }

    /**
	 * 
	 */
	private static String getDateTimePattern(CLDRFile cldrFile, String dateOrTime, String type) {
		if (type.length() > 0) type = "[@type=\"" + type + "\"]";
		String key = "/ldml/dates/calendars/calendar[@type=\"gregorian\"]/"
			+ dateOrTime + "Formats/" 
			+ dateOrTime + "FormatLength"
			+ type + "/" + dateOrTime + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";
		
		String value = cldrFile.getStringValue(key);
		if (value == null) throw new IllegalArgumentException("locale: " + cldrFile.getLocaleID() + "\tpath: " + key + "\r\nvalue: " + value);
		return value;
	}
    
    private String[] getArray(CLDRFile cldrFile, String key, String type, String width) {
    	boolean isMonth = type.equals("month");
    	int length = isMonth ? 12 : 7;
    	String[] result = new String[length];
    	for (int i = 0; i < length; ++i) {
    		String lastType = isMonth ? String.valueOf(i) : Days[i];
    		String item = cldrFile.getStringValue(key +
    				"/" + type + "s/"
					+ type + "Context[@type=\"format\"]/"
					+ type + "Width[@type=\"" + width + "\"]/"
					+ type + "[@type=\"" + lastType + "\"]");
    	}
    	/* <months>
		<monthContext type="format">
		<monthWidth type="abbreviated">
		<month type="1">1</month>
		*/
    	return result;
    }
    
    private Map numberFormatCache = new HashMap();
    
    private static String[] NumberNames = {"integer", "decimal", "percent", "scientific"}; //// "standard", , "INR", "CHF", "GBP"
    private static int firstReal = 1;
    private static int firstCurrency = 4;
    
    public String getNumberNames(int i) {
    	return NumberNames[i];
    }
    
    public static int LIMIT_NUMBER_INDEX = NumberNames.length;
    
    private static class MyCurrency extends Currency {
    	String symbol;
    	String displayName;
    	int fractDigits;
    	double roundingIncrement;
    	MyCurrency(String code, String symbol, String displayName, String fractDigits, String roundingIncrement) {
    		super(code);
    		this.symbol = symbol == null ? code : symbol;
    		this.displayName = displayName == null ? code : displayName;
    		this.fractDigits = fractDigits == null ? 2 : Integer.parseInt(fractDigits);
    		this.roundingIncrement = roundingIncrement == null ? 0.0 
    				: Integer.parseInt(roundingIncrement) * Math.pow(10.0, -this.fractDigits);
    	}
    	public String getName(ULocale locale,
                int nameStyle,
                boolean[] isChoiceFormat) {

    		String result = nameStyle == 0 ? this.symbol
    				: nameStyle == 1 ? getCurrencyCode()
    				: nameStyle == 2 ? displayName
    				: null;
    	    if (result == null) throw new IllegalArgumentException();
    	    isChoiceFormat[0] = result.indexOf('|') >= 0; // hack, but should work
    	    return result;
    	}
    	
    	/**
         * Returns the rounding increment for this currency, or 0.0 if no
         * rounding is done by this currency.
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
        	return (((int)roundingIncrement*37 + fractDigits)*37 + symbol.hashCode())*37
				+ displayName.hashCode();
        }
    }
    
    public DecimalFormat getCurrencyFormat(CLDRFile cldrFile, String currency) {
        //CLDRFile cldrFile = cldrFactory.make(localeID, true);
    	return _getNumberFormat(cldrFile, currency, true);
    }

    public DecimalFormat getNumberFormat(CLDRFile cldrFile, int index) {
        //CLDRFile cldrFile = cldrFactory.make(localeID, true);
    	return _getNumberFormat(cldrFile, NumberNames[index], false);
    }

    private DecimalFormat _getNumberFormat(CLDRFile cldrFile, String key1, boolean isCurrency) {
    	ULocale ulocale = new ULocale(cldrFile.getLocaleID());
    	String key = ulocale + "," + key1;
    	DecimalFormat result = (DecimalFormat) numberFormatCache.get(key);
    	if (result != null) return result;

        String prefix = "/ldml/numbers/";
        String type = key1;
        if (isCurrency) type = "currency";
        else if (key1.equals("integer")) type = "decimal";
        String path = prefix
		+ type + "Formats/" 
		+ type + "FormatLength/"
		+ type + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";
        
        String pattern = cldrFile.getStringValue(path);
        if (pattern == null) throw new IllegalArgumentException("locale: " + ulocale + "\tpath: " + path);
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        
        // currently constants
        // symbols.setPadEscape(cldrFile.getStringValue("/ldml/numbers/symbols/xxx"));
        // symbols.setSignificantDigit(cldrFile.getStringValue("/ldml/numbers/symbols/patternDigit"));

        symbols.setDecimalSeparator(cldrFile.getStringValue("/ldml/numbers/symbols/decimal").charAt(0));
        symbols.setDigit(cldrFile.getStringValue("/ldml/numbers/symbols/patternDigit").charAt(0));
        symbols.setExponentSeparator(cldrFile.getStringValue("/ldml/numbers/symbols/exponential"));
        symbols.setGroupingSeparator(cldrFile.getStringValue("/ldml/numbers/symbols/group").charAt(0));
        symbols.setInfinity(cldrFile.getStringValue("/ldml/numbers/symbols/infinity"));
        symbols.setMinusSign(cldrFile.getStringValue("/ldml/numbers/symbols/minusSign").charAt(0));
        symbols.setNaN(cldrFile.getStringValue("/ldml/numbers/symbols/nan"));
        symbols.setPatternSeparator(cldrFile.getStringValue("/ldml/numbers/symbols/list").charAt(0));
        symbols.setPercent(cldrFile.getStringValue("/ldml/numbers/symbols/percentSign").charAt(0));
        symbols.setPerMill(cldrFile.getStringValue("/ldml/numbers/symbols/perMille").charAt(0));
        symbols.setPlusSign(cldrFile.getStringValue("/ldml/numbers/symbols/plusSign").charAt(0));
        symbols.setZeroDigit(cldrFile.getStringValue("/ldml/numbers/symbols/nativeZeroDigit").charAt(0));

        MyCurrency mc = null;
        if (isCurrency) {
         	prefix = "/ldml/numbers/currencies/currency[@type=\"" + key1 + "\"]/";
        	// /ldml/numbers/currencies/currency[@type="GBP"]/symbol
         	// /ldml/numbers/currencies/currency[@type="GBP"]
         	
        	mc = new MyCurrency(key1, 
        			cldrFile.getStringValue(prefix + "symbol"), 
        			cldrFile.getStringValue(prefix + "displayName"),
					null, null);
        	
        	String possible = null;
        	possible = cldrFile.getStringValue(prefix + "decimal"); 
            symbols.setMonetaryDecimalSeparator(possible != null ? possible.charAt(0) : symbols.getDecimalSeparator());
            if ((possible = cldrFile.getStringValue(prefix + "pattern")) != null) pattern = possible;
            if ((possible = cldrFile.getStringValue(prefix + "group")) != null) symbols.setGroupingSeparator(possible.charAt(0));
            //; 
        }
        result = new DecimalFormat(pattern, symbols);
        if (mc != null) {
        	result.setCurrency(mc);
        } else {
        	result.setCurrency(NO_CURRENCY);
        }
        
        if (false) {
	        System.out.println("creating " + ulocale + "\tkey: " + key + "\tpattern "
	        		+ pattern + "\tresult: " + result.toPattern() + "\t0=>" + result.format(0));
	        DecimalFormat n2 = (DecimalFormat) NumberFormat.getScientificInstance(ulocale);
	        System.out.println("\tresult: " + n2.toPattern() + "\t0=>" + n2.format(0));
	        }
        if (key1.equals("integer")) {
        	result.setMaximumFractionDigits(0);
        	result.setDecimalSeparatorAlwaysShown(false);
        	result.setParseIntegerOnly(true);
        }
        numberFormatCache.put(key, result);
        return result;
    }

}