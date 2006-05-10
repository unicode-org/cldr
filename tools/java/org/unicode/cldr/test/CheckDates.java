package org.unicode.cldr.test;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.SimpleDemo;
import org.unicode.cldr.test.CheckNumbers.MyCheckStatus;
import org.unicode.cldr.test.CheckNumbers.MyDemo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeProperty.PatternMatcher;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class CheckDates extends CheckCLDR {
	ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	PatternMatcher m;
	
	static String[] samples = {
		//"AD 1970-01-01T00:00:00Z",
		//"BC 4004-10-23T07:00:00Z", // try a BC date: creation according to Ussher & Lightfoot. Assuming garden of eden 2 hours ahead of UTC
		"2005-12-02 12:15:16",
		//"AD 2100-07-11T10:15:16Z",
        }; // keep aligned with following
	static String SampleList = "{0}"
	    //+ "\r\n\t\u200E{1}\u200E\r\n\t\u200E{2}\u200E\r\n\t\u200E{3}\u200E"
    ; // keep aligned with previous
	
    private static final String DECIMAL_XPATH = "//ldml/numbers/symbols/decimal";

	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
        // the following is a hack to work around a bug in ICU4J (the snapshot, not the released version).
		try {
            bi = BreakIterator.getCharacterInstance(new ULocale(cldrFileToCheck.getLocaleID()));
        } catch (RuntimeException e) {
            bi = BreakIterator.getCharacterInstance(new ULocale(""));
        }
        CLDRFile resolved = getResolvedCldrFileToCheck();
        flexInfo.set(resolved);

        // load decimal path specially
        String decimal = resolved.getStringValue(DECIMAL_XPATH);
        if(decimal != null)  {
            flexInfo.checkFlexibles(DECIMAL_XPATH,decimal,DECIMAL_XPATH);
        }

        // load gregorian appendItems
        for (Iterator it = resolved.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]"); it.hasNext();) {
            String path = (String) it.next();
            String value = resolved.getStringValue(path);
            String fullPath = resolved.getFullXPath(path);
            flexInfo.checkFlexibles(path, value, fullPath);
        }
        
        redundants.clear();
        flexInfo.getRedundants(redundants);
		return this;
	}
	BreakIterator bi;
    FlexibleDateFromCLDR flexInfo = new FlexibleDateFromCLDR();
    Collection redundants = new HashSet();

	public CheckCLDR handleCheck(String path, String fullPath, String value, Map options, List result) {
		if (path.indexOf("/dates") < 0 || path.indexOf("gregorian") < 0) return this;
		try {
			if (path.indexOf("[@type=\"narrow\"]") >= 0) {
				int end = getFirstGraphemeClusterBoundary(value);
				if (end != value.length()) {
					result.add(new CheckStatus()
                            .setCause(this).setType(CheckStatus.errorType)
								.setMessage(
										"Illegal narrow value. Must be only one grapheme cluster \u200E{0}\u200E~\u200E{1}\u200E",
										new Object[]{value.substring(0,end), value.substring(end)}));
				}
			}
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0
                    || path.indexOf("/dateFormatItem") >= 0) {
				checkPattern(path, fullPath, value, result);
			}
		} catch (Exception e) {
			CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
			.setMessage("Error in creating date format {0}", new Object[]{e});    	
			result.add(item);
		}
		return this;
	}
	
	public CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options, List result) {
		if (path.indexOf("/dates") < 0 || path.indexOf("gregorian") < 0) return this;
		try {
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0
                    || path.indexOf("/dateFormatItem") >= 0) {
				checkPattern2(path, fullPath, value, result);
			}
		} catch (Exception e) {
			// don't worry about errors
		}
		return this;
	}


	//Calendar myCal = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
	TimeZone denver = TimeZone.getTimeZone("America/Denver");
	static final SimpleDateFormat neutralFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", ULocale.ENGLISH);
    static {
        neutralFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
	XPathParts pathParts = new XPathParts(null, null);
    
    static long date1950 = new Date(50,0,1,0,0,0).getTime();
    static long date2010 = new Date(110,0,1,0,0,0).getTime();
    static long date4004BC = new Date(-4004-1900,9,23,2,0,0).getTime();
    static Random random = new Random(0);
    
    static private String getRandomDate(long startDate, long endDate) {
        double guess = startDate + random.nextDouble()*(endDate - startDate);
        return neutralFormat.format(new Date((long)guess));
    }

	private void checkPattern(String path, String fullPath, String value, List result) throws ParseException {
		pathParts.set(path);
        if (pathParts.containsElement("dateFormatItem")) {
            String failureMessage = (String) flexInfo.getFailurePath(path);
            if (failureMessage != null) {
                result.add(new CheckStatus().setCause(this).setType(CheckStatus.errorType)
                .setMessage("{0}", new Object[]{failureMessage}));          
            }
            if (redundants.contains(value)) {
                result.add(new CheckStatus().setCause(this).setType(CheckStatus.errorType)
                .setMessage("Redundant with some pattern (or combination)", new Object[]{}));          
            }
        }
		String calendar = pathParts.findAttributeValue("calendar", "type");
		if (path.indexOf("\"full\"") >= 0) {
			// for date, check that era is preserved
			// TODO fix naked constants
			SimpleDateFormat y = icuServiceBuilder.getDateFormat(calendar, 4, 4);
			//String trial = "BC 4004-10-23T2:00:00Z";
			//Date dateSource = neutralFormat.parse(trial);
            Date dateSource = new Date(date4004BC);
			int year = dateSource.getYear() + 1900;
			if (year > 0) {
				year = 1-year;
				dateSource.setYear(year - 1900);
			}
			//myCal.setTime(dateSource);
			String result2 = y.format(dateSource);
			Date backAgain = y.parse(result2);
			//String isoBackAgain = neutralFormat.format(backAgain);
			if (false && path.indexOf("/dateFormat") >= 0 && year != backAgain.getYear()) {
				CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
				.setMessage("Need Era (G) in full format.", new Object[]{});			
				result.add(item);			
			}
			// TODO fix this up.
			if (path.indexOf("/timeFormat") >= 0 && y.toPattern().indexOf("v") < 0) {
				CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
				.setMessage("Need full zone (v) in full format", new Object[]{});			
				result.add(item);			
			}
		}
	}
	
	private ParsePosition parsePosition = new ParsePosition(0);
	
	private void checkPattern2(String path, String fullPath, String value, List result) throws ParseException {
		pathParts.set(path);
		String calendar = pathParts.findAttributeValue("calendar", "type");
        SimpleDateFormat x = icuServiceBuilder.getDateFormat(calendar, value);
		Object[] arguments = new Object[samples.length];
		for (int i = 0; i < samples.length; ++i) {
			String source = getRandomDate(date1950, date2010); // samples[i];
			Date dateSource = neutralFormat.parse(source);
			String formatted = x.format(dateSource);
			String reparsed;
			
			parsePosition.setIndex(0);
			Date parsed = x.parse(formatted, parsePosition);
			if (parsePosition.getIndex() != formatted.length()) {
				reparsed = "Couldn't parse past: " + formatted.substring(0,parsePosition.getIndex());
			} else {
				reparsed = neutralFormat.format(parsed);
			}

			arguments[i] = source + " \u2192 \u201C\u200E" + formatted + "\u200E\u201D \u2192 " + reparsed;
		}
        result.add(new CheckStatus()
                .setCause(this).setType(CheckStatus.exampleType)
                .setMessage(SampleList, arguments));
        result.add(new MyCheckStatus()
                .setFormat(x)
                .setCause(this).setType(CheckStatus.demoType));
	}
	

	private int getFirstGraphemeClusterBoundary(String value) {
		if (value.length() <= 1) return value.length();
		int current = 0;
		// skip any leading digits, for CJK
		//current = Utility.scan(DIGIT, value, current);		
		bi.setText(value);
		if (current != 0) bi.preceding(current+1); // get safe spot, possibly before
		current = bi.next();
		// continue collecting any additional characters that are M or grapheme extend
		current = Utility.scan(XGRAPHEME, value, current);
		// special case: allow 11 or 12
		//current = Utility.scan(DIGIT, value, current);		
		if (current != value.length() && DIGIT.containsAll(value) && value.length() == 2) {
			return value.length();
		}
		return current;
	}
	static final UnicodeSet XGRAPHEME = new UnicodeSet("[[:mark:][:grapheme_extend:]]");
	static final UnicodeSet DIGIT = new UnicodeSet("[:decimal_number:]");
    
    static public class MyCheckStatus extends CheckStatus {
        private SimpleDateFormat df;
        public MyCheckStatus setFormat(SimpleDateFormat df) {
            this.df = df;
            return this;
        }
        public SimpleDemo getDemo() {
            return new MyDemo().setFormat(df);
        }
    }

    static class MyDemo extends FormatDemo {
        private SimpleDateFormat df;
        String getPattern() {
            return df.toPattern();
        }
        String getRandomInput() {
            return getRandomDate(date1950, date2010);
        }
        public MyDemo setFormat(SimpleDateFormat df) {
            this.df = df;
            return this;
        }
        
        protected void getArguments(Map inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            boolean result = false;
            Date d;
            try {
            	currentPattern = (String) inout.get("pattern");
                if (currentPattern != null) df.applyPattern(currentPattern);
                else currentPattern = getPattern();
            } catch (Exception e) {
            	currentPattern = "Use format like: ##,###.##";
                return;
            }
            try {
            	currentInput = (String) inout.get("input");
            	if (currentInput == null) {
            		currentInput = getRandomInput();
            	}
            	d = neutralFormat.parse(currentInput);
            } catch (Exception e) {
            	currentInput = "Use neutral format like: 1993-11-31 13:49:02";
                return;
            }
            try {
            	currentFormatted = df.format(d);
            } catch (Exception e) {
            	currentFormatted = "Can't format: " + e.getMessage();
                return;
            }
            try {            	
            	parsePosition.setIndex(0);         
                Date n = df.parse(currentFormatted, parsePosition);
                if (parsePosition.getIndex() != currentFormatted.length()) {
                	currentReparsed = "Couldn't parse past: "  + "\u200E" + currentFormatted.substring(0, parsePosition.getIndex()) + "\u200E";
                } else {
                	currentReparsed = neutralFormat.format(n);
                }
            } catch (Exception e) {
            	currentReparsed = "Can't parse: " + e.getMessage();
            }
        }

    }
}
