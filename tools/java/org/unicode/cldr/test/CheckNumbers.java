package org.unicode.cldr.test;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;

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
	
	static String SampleList = "{0} \u2192 \u201C\u200E{1}\u200E\u201D \u2192 {2}";
	
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
				checkPattern(path, fullPath, value, result, false);
			}
//			if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
//				checkCurrencyFormats(path, fullPath, value, result);
//			}
			
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
	
	public CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options, List result) {
        if (path.indexOf("/numbers") < 0) return this;
		try {
			if (path.indexOf("/pattern") >= 0 && path.indexOf("/patternDigit") < 0) {
				checkPattern(path, fullPath, value, result, true);
			}
			if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
				checkCurrencyFormats(path, fullPath, value, result, true);
			}
		} catch (Exception e) {
			// don't worry about errors here
		}
		return this;
	}
	
	private void checkPattern(String path, String fullPath, String value, List result, boolean generateExamples) throws ParseException {
		if (value.indexOf('\u00a4') >= 0) { // currency pattern
			DecimalFormat x = icuServiceBuilder.getCurrencyFormat("XXX");
			addSamples(x, x.toPattern(), value, result, generateExamples);
		} else {
			DecimalFormat x = icuServiceBuilder.getNumberFormat(value);
			addSamples(x, value, "", result, generateExamples);
		}
	}
	
	private void checkCurrencyFormats(String path, String fullPath, String value, List result, boolean generateExamples) throws ParseException {
		DecimalFormat x = icuServiceBuilder.getCurrencyFormat(CLDRFile.getCode(path));
		addSamples(x, x.toPattern(), value, result, generateExamples);
	}

	private ParsePosition parsePosition = new ParsePosition(0);
	
	private void addSamples(DecimalFormat x, String pattern, String context, List result, boolean generateExamples) throws ParseException {
		Object[] arguments = new Object[3];

        double sample = getRandomNumber();
		arguments[0] = String.valueOf(sample);
		String formatted = x.format(sample);
		arguments[1] = formatted;
		boolean gotFailure = false;
		try {
			parsePosition.setIndex(0);
			double parsed = x.parse(formatted, parsePosition).doubleValue();
			if (parsePosition.getIndex() != formatted.length()) {
				arguments[2] = "Couldn't parse past: " + "\u200E" + formatted.substring(0,parsePosition.getIndex()) + "\u200E";
				gotFailure = true;
			} else {
				arguments[2] = String.valueOf(parsed);
			}
		} catch (Exception e) {
			arguments[2] = e.getMessage();
			gotFailure = true;
		}
//		htmlMessage.append(pattern1)
//		.append(TransliteratorUtilities.toXML.transliterate(String.valueOf(sample)))
//		.append(pattern2)
//		.append(TransliteratorUtilities.toXML.transliterate(formatted))
//		.append(pattern3)
//		.append(TransliteratorUtilities.toXML.transliterate(String.valueOf(parsed)))
//		.append(pattern4);
		if (generateExamples || gotFailure) result.add(new CheckStatus()
				.setCause(this).setType(CheckStatus.exampleType)
				.setMessage(SampleList, arguments));
		if (generateExamples) result.add(new MyCheckStatus()
				.setFormat(x, context)
				.setCause(this).setType(CheckStatus.demoType));
	}

    /**
     * @return
     */
    private static double getRandomNumber() {
    	// min = 12345.678
        double rand = random.nextDouble();
        //System.out.println(rand);
        double sample = Math.round(rand*100000.0*1000.0)/1000.0 + 10000.0;
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
        String context;
        
        public MyCheckStatus setFormat(DecimalFormat df, String context) {
            this.df = df;
            this.context = context;
            return this;
        }
        public SimpleDemo getDemo() {
            return new MyDemo().setFormat(df, context);
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
        public MyDemo setFormat(DecimalFormat df, String context) {
            this.df = df;
            currentContext = context;
            return this;
        }
        
        protected void getArguments(Map inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            double d;
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
            	d = Double.parseDouble(currentInput);
            } catch (Exception e) {
            	currentInput = "Use English format: 1234.56";
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
                Number n = df.parse(currentFormatted, parsePosition);
                if (parsePosition.getIndex() != currentFormatted.length()) {
                	currentReparsed = "Couldn't parse past: \u200E" + currentFormatted.substring(0, parsePosition.getIndex()) + "\u200E";
                } else {
                	currentReparsed = n.toString();
                }
            } catch (Exception e) {
            	currentReparsed = "Can't parse: " + e.getMessage();
            }
        }
        
    }
}