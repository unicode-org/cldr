package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CheckNumbers extends CheckCLDR {
	ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	static double[] samples = {0, -2.345, 12345.678};
	
	public CheckCLDR _check(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (path.indexOf("/currencies") < 0) return this;
		if (path.indexOf("/displayName") >= 0) return this;
		DecimalFormat x = icuServiceBuilder.getCurrencyFormat(getResolvedCldrFileToCheck(), value);
		Object[] arguments = new Object[samples.length];
		StringBuffer htmlMessage = new StringBuffer();
		htmlMessage.append("<table border='1' cellpadding='2'><tr><th width='33%'>Number</th><th width='34%'>Localized Format</th></tr>");
		for (int i = 0; i < samples.length; ++i) {
			String formatted = x.format(samples[i]);
			arguments[i] = samples[i] + " => " + formatted;
			htmlMessage.append("<tr><td>").append(BagFormatter.toXML.transliterate(String.valueOf(samples[i]))).append("</td><td>").append(BagFormatter.toXML.transliterate(formatted)).append("</td></tr>");
		}
		htmlMessage.append("</table>");
    	CheckStatus item = new CheckStatus().setType(CheckStatus.exampleType)
    		.setMessage("Samples: {0}, {1}, {2}", arguments)
    		.setHTMLMessage(htmlMessage.toString());
    	
    	result.add(item);
		return this;
	}
	
}