package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
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
		"AD 1970-01-01T00:00:00Z",
		"BC 4004-10-23T07:00:00Z", // try a BC date: creation according to Ussher & Lightfoot. Assuming garden of eden 2 hours ahead of UTC
		"AD 2005-12-02T12:15:16Z",
		"AD 2100-07-11T10:15:16Z",}; // keep aligned with following
	static String SampleList = "Samples:\r\n\t\u200E{0}\u200E\r\n\t\u200E{1}\u200E\r\n\t\u200E{2}\u200E\r\n\t\u200E{3}\u200E"; // keep aligned with previous
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
		bi = BreakIterator.getCharacterInstance(new ULocale(cldrFileToCheck.getLocaleID()));
		return this;
	}
	BreakIterator bi;

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

	//Calendar myCal = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
	TimeZone denver = TimeZone.getTimeZone("America/Denver");
	SimpleDateFormat isoBC = new SimpleDateFormat("GGG yyyy-MM-dd'T'HH:mm:ss'Z'", ULocale.ENGLISH);
	XPathParts pathParts = new XPathParts(null, null);

	private void checkPattern(String path, String fullPath, String value, List result) throws ParseException {
		pathParts.set(path);
		String calendar = pathParts.findAttributeValue("calendar", "type");
		DateFormat x = icuServiceBuilder.getDateFormat(calendar, value);
		addSamples(x, path.indexOf("/dateFormat") >= 0, result);
		if (path.indexOf("\"full\"") >= 0) {
			// for date, check that era is preserved
			// TODO fix naked constants
			SimpleDateFormat y = icuServiceBuilder.getDateFormat(calendar, 4, 4);
			String trial = "BC 4004-10-23T2:00:00Z";
			Date dateSource = isoBC.parse(trial);
			int year = dateSource.getYear() + 1900;
			if (year > 0) {
				year = 1-year;
				dateSource.setYear(year - 1900);
			}
			//myCal.setTime(dateSource);
			String result2 = y.format(dateSource);
			Date backAgain = y.parse(result2);
			String isoBackAgain = isoBC.format(backAgain);
			if (false && path.indexOf("/dateFormat") >= 0 && year != backAgain.getYear()) {
				CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
				.setMessage("Need Era (G) in full format: \u200E{0}\u200E \u2192 \u200E{1}\u200E", new Object[]{trial, isoBackAgain});			
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
	
	private void addSamples(DateFormat x, boolean isDate, List result) throws ParseException {
		Object[] arguments = new Object[samples.length];
		StringBuffer htmlMessage = new StringBuffer();
		htmlMessage.append("<table border='1' cellpadding='2'><tr><th width='33%'>Number</th><th width='34%'>Localized Format</th></tr>");
		for (int i = 0; i < samples.length; ++i) {
			String source = samples[i];
			Date dateSource = isoBC.parse(source);
			String formatted = x.format(dateSource);
			Date parsed = x.parse(formatted);
			String resource = isoBC.format(parsed);
			arguments[i] = source + " \u2192 \u200E" + formatted + "\u200E \u2192 " + resource;
			htmlMessage.append("<tr><td>")
			.append(TransliteratorUtilities.toXML.transliterate(source))
			.append("</td><td>")
			.append(TransliteratorUtilities.toXML.transliterate(formatted))
			.append("</td><td>")
			.append(TransliteratorUtilities.toXML.transliterate(resource))
			.append("</td></tr>");
		}
		htmlMessage.append("</table>");
		CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.exampleType)
		.setMessage(SampleList, arguments)
		.setHTMLMessage(htmlMessage.toString());
		
		result.add(item);
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
}