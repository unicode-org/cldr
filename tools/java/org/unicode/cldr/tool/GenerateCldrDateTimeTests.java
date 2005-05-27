/*
 * Created on May 19, 2005
 * Copyright (C) 2004-2005, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.GenerateCldrTests.DataShower;
import org.unicode.cldr.tool.GenerateCldrTests.Equator;
import org.unicode.cldr.tool.GenerateCldrTests.ResultsPrinter;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;


class GenerateCldrDateTimeTests {
    Map ulocale_exemplars = new TreeMap(GenerateCldrTests.ULocaleComparator);
    Map uniqueExemplars = new HashMap();
    Set locales = new TreeSet(GenerateCldrTests.ULocaleComparator);

    UnicodeSet getExemplarSet(ULocale locale) {
        return (UnicodeSet) ulocale_exemplars.get(locale);
    }

    void show() {
        Log.logln("Showing Locales");
        Log.logln("Unique Exemplars: " + uniqueExemplars.size());
        for (Iterator it2 = ulocale_exemplars.keySet().iterator(); it2.hasNext();) {
            ULocale locale = (ULocale) it2.next();
            UnicodeSet us = getExemplarSet(locale);
            Log.logln("\t" + locale + ", " + us);
        }
    }
    static final ULocale ROOT = new ULocale("root"); // since CLDR has different root.
	private Factory cldrFactory;
	

    GenerateCldrDateTimeTests(String sourceDir, String localeRegex, boolean doResolved) {
    	this.cldrFactory = CLDRFile.Factory.make(sourceDir, ".*");
        Set s = GenerateCldrTests.getMatchingXMLFiles(sourceDir, localeRegex);
        for (Iterator it = s.iterator(); it.hasNext();) {
            getInfo((String) it.next(), doResolved);
        }
        // now do inheritance manually
        for (Iterator it = locales.iterator(); it.hasNext();) {
            ULocale locale = (ULocale) it.next();
            UnicodeSet ex = (UnicodeSet) ulocale_exemplars.get(locale);
            if (ex != null) continue;
            for (ULocale parent = locale.getFallback(); parent != null; parent = parent.getFallback()) {
                ULocale fixedParent = parent.getLanguage().length() == 0 ? ROOT : parent;
                ex = (UnicodeSet) ulocale_exemplars.get(fixedParent);
                if (ex == null) continue;
                ulocale_exemplars.put(locale, ex);
                break;
            }
        }

    }
    
    void getInfo(String locale, boolean doResolved) {
        System.out.println("Getting info for: " + locale);
        locales.add(new ULocale(locale));
        CLDRFile cldrFile = cldrFactory.make(locale, doResolved);
        //Node node = LDMLUtilities.getNode(doc, "//ldml/characters/exemplarCharacters");
        String cpath = "/ldml/characters/exemplarCharacters";
        String path = cldrFile.getFullXPath(cpath);
        if (path == null) return;
        //if (path.indexOf("[@draft=") >= 0) System.out.println("Skipping draft: " + locale + ",\t" + path);
        String exemplars = cldrFile.getStringValue(cpath);
        UnicodeSet exemplarSet = new UnicodeSet(exemplars);
        UnicodeSet fixed = (UnicodeSet) uniqueExemplars.get(exemplarSet);
        if (fixed == null) {
            uniqueExemplars.put(exemplarSet, exemplarSet);
            fixed = exemplarSet;
        }
        ulocale_exemplars.put(new ULocale(locale), fixed);
    }
    
    public static TimeZone utc = TimeZone.getTimeZone("GMT");
    public static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
        iso.setTimeZone(utc);
    }
    public static int[] DateFormatValues = {-1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
    public static String[] DateFormatNames = {"none", "short", "medium", "long", "full"};
    
    private Map dateFormatCache = new HashMap();
    
    public DateFormat getDateFormat(ULocale locale, int dateIndex, int timeIndex) {
    	String key = locale + "," + dateIndex + "," + timeIndex;
    	SimpleDateFormat result = (SimpleDateFormat) dateFormatCache.get(key);
    	if (result != null) return result;
    	if (false && dateIndex == 2 && timeIndex == 0) {
    		System.out.println("");
    	}
    	
    	CLDRFile cldrFile = cldrFactory.make(locale.toString(), true);
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
        
        
        DecimalFormat numberFormat = (DecimalFormat) getNumberFormat(locale, 1);
        numberFormat.setGroupingUsed(false);
        numberFormat.setDecimalSeparatorAlwaysShown(false);
        numberFormat.setParseIntegerOnly(true); /* So that dd.MM.yy can be parsed */
        numberFormat.setMinimumFractionDigits(0); // To prevent "Jan 1.00, 1997.00"
        
		result = new SimpleDateFormat(pattern, locale); // formatData
		result.setNumberFormat((NumberFormat)numberFormat.clone());
        result.setTimeZone(utc);
		dateFormatCache.put(key, result);
		//System.out.println("created " + key);
        return result;
    }
    
    static final String[] Days = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
    
    String[] getArray(CLDRFile cldrFile, String key, String type, String width) {
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
    // ========== DATES ==========

    Equator DateEquator = new Equator() {
        /**
         * Must both be ULocales
         */
        public boolean equals(Object o1, Object o2) {
            ULocale loc1 = (ULocale) o1;
            ULocale loc2 = (ULocale) o2;
            for (int i = 0; i < DateFormatValues.length; ++i) {
                for (int j = 0; j < DateFormatValues.length; ++j) {
                    if (i == 0 && j == 0) continue; // skip null case
                    DateFormat df1 = getDateFormat(loc1, i, j);
                    NumberFormat nf = df1.getNumberFormat();
                    nf.setCurrency(NO_CURRENCY);
                    df1.setNumberFormat(nf);
                    DateFormat df2 = getDateFormat(loc2, i, j);
                    nf = df2.getNumberFormat();
                    nf.setCurrency(NO_CURRENCY);
                    df2.setNumberFormat(nf);
                    if (!df1.equals(df2)) {
                        df1.equals(df2);
                        return false;
                    }
                }
            }
            return true;
        }
    };

    // ========== NUMBERS ==========

    Map numberFormatCache = new HashMap();
    
    static String[] NumberNames = {"integer", "decimal", "percent", "scientific"}; //// "standard", , "INR", "CHF", "GBP"
    static int firstReal = 1;
    static int firstCurrency = 4;
    
    static class MyCurrency extends Currency {
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
    }
    
    DecimalFormat getNumberFormat(ULocale ulocale, int i) {
    	String key = ulocale + "," + i;
    	DecimalFormat result = (DecimalFormat) numberFormatCache.get(key);
    	if (result != null) return result;
        CLDRFile cldrFile = cldrFactory.make(ulocale.toString(), true);

        String prefix = "/ldml/numbers/";
        String type = NumberNames[i];
        if (i < firstReal) type = NumberNames[firstReal];
        if (i >= firstCurrency) type = "currency";
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
        if (i >= firstCurrency) {
         	prefix = "/ldml/numbers/currencies/currency[@type=\"" + NumberNames[i] + "\"]/";
        	// /ldml/numbers/currencies/currency[@type="GBP"]/symbol
         	// /ldml/numbers/currencies/currency[@type="GBP"]
         	
        	mc = new MyCurrency(NumberNames[i], 
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
        
        if (false && i == firstCurrency) {
	        System.out.println("creating " + ulocale + "\tkey: " + key + "\tpattern "
	        		+ pattern + "\tresult: " + result.toPattern() + "\t0=>" + result.format(0));
	        DecimalFormat n2 = (DecimalFormat) NumberFormat.getScientificInstance(ulocale);
	        System.out.println("\tresult: " + n2.toPattern() + "\t0=>" + n2.format(0));
	        }
        if (i == firstReal-1) {
        	result.setMaximumFractionDigits(0);
        	result.setDecimalSeparatorAlwaysShown(false);
        	result.setParseIntegerOnly(true);
        }
        numberFormatCache.put(key, result);
        return result;
    }
    
    /*
     * <numbers>
-
	<symbols>
<decimal>.</decimal>
<group>,</group>
<list>;</list>
<percentSign>%</percentSign>
<nativeZeroDigit>0</nativeZeroDigit>
<patternDigit>#</patternDigit>
<plusSign>+</plusSign>
<minusSign>-</minusSign>
<exponential>E</exponential>
<perMille>‰</perMille>
<infinity>?</infinity>
<nan>?</nan>
</symbols>
-
	<decimalFormats>
-
	<decimalFormatLength>
-
	<decimalFormat>
<pattern>#,##0.###</pattern>

     */

    static Currency NO_CURRENCY = Currency.getInstance("XXX");

    Equator NumberEquator = new Equator() {
        /**
         * Must both be ULocales
         */
        public boolean equals(Object o1, Object o2) {
            ULocale loc1 = (ULocale) o1;
            ULocale loc2 = (ULocale) o2;
            for (int i = 0; i < NumberNames.length; ++i) {
                NumberFormat nf1 = getNumberFormat(loc1, i);
                //Currency old1 = nf1.getCurrency();	// major hack
                //nf1.setCurrency(NO_CURRENCY);
                NumberFormat nf2 = getNumberFormat(loc2, i);
                //Currency old2 = nf1.getCurrency();	// major hack
                //nf2.setCurrency(NO_CURRENCY);
                boolean result = nf1.equals(nf2);
                //nf1.setCurrency(old1);
                //nf2.setCurrency(old2);
                if (!result) {
                    //nf1.equals(nf2);
                    return false;
                }
            }
            return true;
        }
    };



}