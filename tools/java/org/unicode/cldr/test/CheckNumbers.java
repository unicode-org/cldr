package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CheckNumbers extends CheckCLDR {
	ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	
	static double[] samples = {0, -2.345, 1, 12345.678}; // keep aligned with following
	static String SampleList = "{0}, {1}, {2}, {3}"; // keep aligned with previous
	
	public CheckCLDR _check(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (path.indexOf("/numbers") < 0) return this;
		try {
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/patternDigit") < 0) {
				checkPattern(path, fullPath, value, pathParts, fullPathParts, result);
			}
			if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
				checkCurrencyFormats(path, fullPath, value, pathParts, fullPathParts, result);
			}
		} catch (Exception e) {
			CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Error in creating number format {0}; {1}", new Object[]{e.getClass().getName(), e});    	
			result.add(item);
		}
		return this;
	}
	
	private void checkPattern(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) throws ParseException {
		DecimalFormat x = icuServiceBuilder.getNumberFormat(getResolvedCldrFileToCheck(), value);
		addSamples(x, result);
	}
	
	private void checkCurrencyFormats(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) throws ParseException {
		DecimalFormat x = icuServiceBuilder.getCurrencyFormat(getResolvedCldrFileToCheck(), CLDRFile.getCode(path));
		addSamples(x, result);
	}

	private void addSamples(DecimalFormat x, List result) throws ParseException {
		Object[] arguments = new Object[samples.length];
		StringBuffer htmlMessage = new StringBuffer();
		htmlMessage.append("<table border='1' cellpadding='2'><tr><th width='33%'>Number</th><th width='34%'>Localized Format</th></tr>");
		for (int i = 0; i < samples.length; ++i) {
			String formatted = x.format(samples[i]);
			double parsed = x.parse(formatted).doubleValue();
			arguments[i] = samples[i] + " => " + formatted + " => " + parsed;
			htmlMessage.append("<tr><td>")
			.append(BagFormatter.toXML.transliterate(String.valueOf(samples[i])))
			.append("</td><td>")
			.append(BagFormatter.toXML.transliterate(formatted))
			.append("</td><td>")
			.append(BagFormatter.toXML.transliterate(String.valueOf(parsed)))
			.append("</td></tr>");
		}
		htmlMessage.append("</table>");
		CheckStatus item = new CheckStatus().setType(CheckStatus.exampleType)
		.setMessage("Samples: " + SampleList, arguments)
		.setHTMLMessage(htmlMessage.toString());
		
		result.add(item);
	}
}