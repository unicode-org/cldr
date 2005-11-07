package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class CheckDates extends CheckCLDR {
	ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	
	static String[] samples = {
		"AD 1970-01-01T00:00:00Z",
		"BC 4004-10-23T07:00:00Z", // try a BC date: creation according to Ussher & Lightfoot. Assuming garden of eden 2 hours ahead of UTC
		"AD 2005-12-02T12:15:16Z",
		"AD 2100-07-11T10:15:16Z",}; // keep aligned with following
	static String SampleList = "Samples:\r\n\t{0}\r\n\t{1}\r\n\t{2}\r\n\t{3}"; // keep aligned with previous
	
	public CheckCLDR _check(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (path.indexOf("/dates") < 0 || path.indexOf("gregorian") < 0) return this;
		try {
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0) {
				checkPattern(path, fullPath, value, pathParts, fullPathParts, result);
			}
		} catch (Exception e) {
			CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Error in creating date format", new Object[]{e});    	
			result.add(item);
		}
		return this;
	}

	//Calendar myCal = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
	TimeZone denver = TimeZone.getTimeZone("America/Denver");
	SimpleDateFormat isoBC = new SimpleDateFormat("GGG yyyy-MM-dd'T'HH:mm:ss'Z'", ULocale.ENGLISH);

	private void checkPattern(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) throws ParseException {
		pathParts.initialize(path);
		String calendar = pathParts.findAttributeValue("calendar", "type");
		DateFormat x = icuServiceBuilder.getDateFormat(getResolvedCldrFileToCheck(), calendar, value);
		addSamples(x, path.indexOf("/dateFormat") >= 0, result);
		if (path.indexOf("\"full\"") >= 0) {
			// for date, check that era is preserved
			// TODO fix naked constants
			SimpleDateFormat y = icuServiceBuilder.getDateFormat(getResolvedCldrFileToCheck(), calendar, 4, 4);
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
			if (path.indexOf("/dateFormat") >= 0 && year != backAgain.getYear()) {
				CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
				.setMessage("Need Era (G) in full format: {0} => {1}", new Object[]{trial, isoBackAgain});			
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
			arguments[i] = source + " => " + formatted + " => " + resource;
			htmlMessage.append("<tr><td>")
			.append(BagFormatter.toXML.transliterate(source))
			.append("</td><td>")
			.append(BagFormatter.toXML.transliterate(formatted))
			.append("</td><td>")
			.append(BagFormatter.toXML.transliterate(resource))
			.append("</td></tr>");
		}
		htmlMessage.append("</table>");
		CheckStatus item = new CheckStatus().setType(CheckStatus.exampleType)
		.setMessage(SampleList, arguments)
		.setHTMLMessage(htmlMessage.toString());
		
		result.add(item);
	}
}