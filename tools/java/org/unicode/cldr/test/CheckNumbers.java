package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.FormatDemo;
import org.unicode.cldr.test.CheckCLDR.SimpleDemo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;

import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckNumbers extends CheckCLDR {
	private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
	private static NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
	static {
		english.setMaximumFractionDigits(5);
	}
	private static Random random = new Random();
	
	static String SampleList = "{0} \u2192 \u200E{1}\u200E \u2192 {2}";
	
	boolean isPOSIX;
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
		isPOSIX = cldrFileToCheck.getLocaleID().indexOf("POSIX") >= 0;
		return this;
	}

	public CheckCLDR handleCheck(String path, String fullPath, String value, Map options, List result) {
        if (path.indexOf("/currencyMatch") >= 0) {
            try {
                UnicodeSet s = new UnicodeSet(value);
            } catch (Exception e) {
                CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
                .setMessage("Error in creating UnicodeSet {0}; {1}; {2}", new Object[]{value, e.getClass().getName(), e});       
                result.add(item);
            }
            return this;
        }
        if (path.indexOf("/numbers") < 0) return this;
		try {
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/patternDigit") < 0) {
				checkPattern(path, fullPath, value, result);
			}
			if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
				checkCurrencyFormats(path, fullPath, value, result);
			}
			
			byte type = getNumericType(path);
			if (type != NOT_NUMERIC_TYPE) {
				String pattern = getCanonicalPattern(value, type, isPOSIX);
				if (!pattern.equals(value)) {
					result.add(new CheckStatus()
                            .setCause(this).setType(CheckStatus.errorType)
							.setMessage("Value should be \u200E{0}\u200E", new Object[]{pattern}));				
				}
			}

		} catch (Exception e) {
			CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
			.setMessage("Error in creating number format {0}; {1}", new Object[]{e.getClass().getName(), e});    	
			result.add(item);
		}
		return this;
	}
	
	private void checkPattern(String path, String fullPath, String value, List result) throws ParseException {
		DecimalFormat x = icuServiceBuilder.getNumberFormat(value);
		addSamples(x, value, "", result);
	}
	
	private void checkCurrencyFormats(String path, String fullPath, String value, List result) throws ParseException {
		DecimalFormat x = icuServiceBuilder.getCurrencyFormat(CLDRFile.getCode(path));
		addSamples(x, x.toPattern(), value, result);
	}

	private void addSamples(DecimalFormat x, String pattern, String context, List result) throws ParseException {
		Object[] arguments = new Object[3];
		StringBuffer htmlMessage = new StringBuffer();
        FormatDemo.appendTitle(htmlMessage);
        double sample = getRandomNumber();
		String formatted = x.format(sample);
		double parsed = x.parse(formatted).doubleValue();
		arguments[0] = String.valueOf(sample);
		arguments[1] = formatted;
		arguments[2] = String.valueOf(parsed);
//		htmlMessage.append(pattern1)
//		.append(TransliteratorUtilities.toXML.transliterate(String.valueOf(sample)))
//		.append(pattern2)
//		.append(TransliteratorUtilities.toXML.transliterate(formatted))
//		.append(pattern3)
//		.append(TransliteratorUtilities.toXML.transliterate(String.valueOf(parsed)))
//		.append(pattern4);
        FormatDemo.appendLine(htmlMessage, pattern, context, arguments[0].toString(), formatted, arguments[2].toString());
		result.add(new MyCheckStatus()
				.setFormat(x)
				.setCause(this).setType(CheckStatus.exampleType)
				.setMessage(SampleList, arguments)
				.setHTMLMessage(htmlMessage.toString()));
	}

    /**
     * @return
     */
    private static double getRandomNumber() {
        double rand = random.nextDouble();
        //System.out.println(rand);
        double sample = Math.round(Math.pow(10,rand*8))/1000.0;
        if (random.nextBoolean()) sample = -sample;
        return sample;
    }

	static String pattern1 = "<table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse' style='width: 100%'>"
		+ "<tr>"
		+ "<td nowrap width='1%'>Input:</td>"
		+ "<td><input type='text' name='T1' size='50' style='width: 100%' value='";
	static String pattern2 = "'></td>"
		+ "<td nowrap width='1%'><input type='submit' value='Test' name='B1'></td>"
		+ "<td nowrap width='1%'>Formatted:</td>"
		+ "<td><input type='text' name='T2' size='50' style='width: 100%' value='";
	static String pattern3 = "'></td>"
		+ "<td nowrap width='1%'>Parsed:</td>"
		+ "<td><input type='text' name='T3' size='50' style='width: 100%' value='";
	static String pattern4 = "'></td>"
		+ "</tr>"
		+ "</table>";
	
	private static int[][] DIGIT_COUNT = {{1,2,2}, {1,0,3}, {1,0,0}, {0,0,0}};
	private static int[][] POSIX_DIGIT_COUNT = {{1,2,2}, {1,0,6}, {1,0,0}, {1,6,6}};
	public static String getCanonicalPattern(String inpattern, byte type, boolean isPOSIX) {
		// TODO fix later to properly handle quoted ;
		DecimalFormat df = new DecimalFormat(inpattern);
		int[] digits = isPOSIX ? POSIX_DIGIT_COUNT[type] : DIGIT_COUNT[type];
		df.setMinimumIntegerDigits(digits[0]);
		df.setMinimumFractionDigits(digits[1]);
		df.setMaximumFractionDigits(digits[2]);
		String pattern = df.toPattern();

		//int pos = pattern.indexOf(';');
		//if (pos < 0) return pattern + ";-" + pattern;
		return pattern;
	}
	public static final byte NOT_NUMERIC_TYPE = -1, CURRENCY_TYPE = 0, DECIMAL_TYPE = 1, PERCENT_TYPE = 2, SCIENTIFIC_TYPE = 3;
	public static final String[] TYPE_NAME = {"currency", "decimal", "percent", "scientific"};

	public static byte getNumericType(String xpath) {
		if (xpath.indexOf("/pattern") < 0) {
			return NOT_NUMERIC_TYPE;
		} else if (xpath.startsWith("//ldml/numbers/currencyFormats/")) {
			return CURRENCY_TYPE;
		} else if (xpath.startsWith("//ldml/numbers/decimalFormats/")) {
			return DECIMAL_TYPE;
		} else if (xpath.startsWith("//ldml/numbers/percentFormats/")) {
			return PERCENT_TYPE;
		} else if (xpath.startsWith("//ldml/numbers/scientificFormats/")) {
			return SCIENTIFIC_TYPE;
		} else if (xpath.startsWith("//ldml/numbers/currencies/currency/")) {
			return CURRENCY_TYPE;
		} else {
			return NOT_NUMERIC_TYPE;
		}
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

    static class MyDemo extends FormatDemo {
        private DecimalFormat df;
        String getPattern() {
            return df.toPattern();
        }
        String getRandomInput() {
            return String.valueOf(getRandomNumber());
        }
        public MyDemo setFormat(DecimalFormat df) {
            this.df = df;
            return this;
        }
        public boolean processPost(Map inout) {
            boolean result = false;
            double d;
            try {
                String pattern = (String) inout.get("pattern");
                df.applyPattern(pattern);
            } catch (Exception e) {
                result |= putIfDifferent(inout, "pattern", "Use format like: ##,###.##");
                return true;
            }
            try {
                String s = (String) inout.get("input");
                d = Double.parseDouble(s);
            } catch (Exception e) {
                result |= putIfDifferent(inout, "input", "Use English format: 1234.56");
                return true;
            }
            String formatted;
            try {
                formatted = df.format(d);
                result |= putIfDifferent(inout, "formatted", formatted);
            } catch (Exception e) {
                result |= putIfDifferent(inout, "formatted", "Can't format: " + e.getMessage());
                return true;
            }
            try {
                Number n = df.parse(formatted);
                result |= putIfDifferent(inout, "reparsed", n.toString());
            } catch (Exception e) {
                result |= putIfDifferent(inout, "reparsed", "Can't parse: " + e.getMessage());
            }
            return result;
        }
    }
}