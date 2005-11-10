package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.SimpleDemo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CheckNumbers extends CheckCLDR {
	private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	private static NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	static {
		english.setMaximumFractionDigits(19);
	}
	private Random random = new Random();
	
	static String SampleList = "{0} => {1} => {2}";
	
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
		Object[] arguments = new Object[3];
		StringBuffer htmlMessage = new StringBuffer();
		double sample = Math.pow(2, 40*random.nextDouble()-20) ;
		String formatted = x.format(sample);
		double parsed = x.parse(formatted).doubleValue();
		arguments[0] = english.format(sample);
		arguments[1] = formatted;
		arguments[2] = english.format(parsed);
		htmlMessage.append(pattern1)
		.append(BagFormatter.toXML.transliterate(String.valueOf(sample)))
		.append(pattern2)
		.append(BagFormatter.toXML.transliterate(formatted))
		.append(pattern3)
		.append(BagFormatter.toXML.transliterate(String.valueOf(parsed)))
		.append(pattern4);
		result.add(new MyCheckStatus()
				.setFormat(x)
				.setType(CheckStatus.exampleType)
				.setMessage(SampleList, arguments)
				.setHTMLMessage(htmlMessage.toString()));
	}

	static public class MyCheckStatus extends CheckStatus {
		private DecimalFormat df;
		public MyCheckStatus setFormat(DecimalFormat df) {
			this.df = df;
			return this;
		}
		public SimpleDemo getDemo() {
			return new MyDemo().setFormat(df);
		}
	}
	
	static class MyDemo extends SimpleDemo {
		private DecimalFormat df;
		public MyDemo setFormat(DecimalFormat df) {
			this.df = df;
			return this;
		}
		public boolean processPost(Map inout) {
			boolean result = false;
			double d;
			try {
				String s = (String) inout.get("T1");
				d = Double.parseDouble(s);
			} catch (Exception e) {
				result |= putIfDifferent(inout, "T2", "Use format: 1234.56");
				return true;
			}
			String formatted;
			try {
				formatted = df.format(d);
				result |= putIfDifferent(inout, "T2", formatted);
			} catch (Exception e) {
				result |= putIfDifferent(inout, "T2", "Can't format: " + e.getMessage());
				return true;
			}
			try {
				Number n = df.parse(formatted);
				result |= putIfDifferent(inout, "T3", english.format(n));
			} catch (Exception e) {
				result |= putIfDifferent(inout, "T3", "Can't parse: " + e.getMessage());
			}
			return result;
		}
	}
	static String pattern1 = "<table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse' style='width: 100%'>"
		+ "<tr>"
		+ "<td nowrap width='1%'>Input:</td>"
		+ "<td><input type='text' name='T1' size='50' style='width: 100%' value='";
	static String pattern2 = "'></td>"
		+ "<td nowrap width='1%'><input type='submit' value='Go' name='B1'></td>"
		+ "<td nowrap width='1%'>Formatted:</td>"
		+ "<td><input type='text' name='T2' size='50' style='width: 100%' value='";
	static String pattern3 = "'></td>"
		+ "<td nowrap width='1%'>Parsed:</td>"
		+ "<td><input type='text' name='T3' size='50' style='width: 100%' value='";
	static String pattern4 = "'></td>"
		+ "</tr>"
		+ "</table>";
}