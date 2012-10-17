package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.DisplayAndInputProcessor.NumericType;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckNumbers extends FactoryCheckCLDR {
    private static final UnicodeSet FORBIDDEN_NUMERIC_PATTERN_CHARS = new UnicodeSet("[[:n:]-[0]]");

    /**
     * If you are going to use ICU services, then ICUServiceBuilder will allow you to create
     * them entirely from CLDR data, without using the ICU data.
     */
    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private Set<Count> pluralTypes;
    private PathHeader.Factory pathHeaderFactory;
    /**
     * A number formatter used to show the English format for comparison.
     */
    private static NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
    static {
        english.setMaximumFractionDigits(5);
    }

    /**
     * Providing random numbers for some of the tests
     */
    private static Random random = new Random();

    private static Pattern ALLOWED_INTEGER = Pattern.compile("1(0+)");
    private static Pattern COMMA_ABUSE = Pattern.compile(",[0#]([^0#]|$)");
    private static final String decimalFormatXpath =
        "//ldml/numbers/decimalFormats[@numberSystem=\"%s\"]/decimalFormatLength[@type=\"%s\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"%s\"][@count=\"%s\"]";

    /**
     * A MessageFormat string. For display, anything variable that contains strings that might have BIDI
     * characters in them needs to be surrounded by \u200E.
     */
    static String SampleList = "{0} \u2192 \u201C\u200E{1}\u200E\u201D \u2192 {2}";

    /**
     * Special flag for POSIX locale.
     */
    boolean isPOSIX;

    public CheckNumbers(Factory factory) {
        super(factory);
        pathHeaderFactory = PathHeader.getFactory(factory.make("en", true));
    }

    /**
     * Whenever your test needs initialization, override setCldrFileToCheck.
     * It is called for each new file needing testing. The first two lines will always
     * be the same; checking for null, and calling the super.
     */
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
        isPOSIX = cldrFileToCheck.getLocaleID().indexOf("POSIX") >= 0;
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(
            getFactory().getSupplementalDirectory());
        PluralInfo pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
        pluralTypes = pluralInfo.getCountToExamplesMap().keySet();

        return this;
    }

    /**
     * This is the method that does the check. Notice that for performance, you should try to
     * exit as fast as possible except where the path is one that you are testing.
     */
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have
        // Do a quick check on the currencyMatch, to make sure that it is a proper UnicodeSet
        if (path.indexOf("/currencyMatch") >= 0) {
            try {
                UnicodeSet s = new UnicodeSet(value);
            } catch (Exception e) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidCurrencyMatchSet)
                    .setMessage("Error in creating UnicodeSet {0}; {1}; {2}",
                        new Object[] { value, e.getClass().getName(), e }));
            }
            return this;
        }
        // quick bail from all other cases
        if (path.indexOf("/numbers") < 0) return this;

        // Here is the main test. Notice that we pick up any exceptions, so that we can
        // give a reasonable error message.
        try {
            if (path.indexOf("/pattern") >= 0 && path.indexOf("/patternDigit") < 0) {
                checkPattern(path, fullPath, value, result, false);
            }
            // if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
            // checkCurrencyFormats(path, fullPath, value, result);
            // }

            // for the numeric cases, make sure the pattern is canonical
            NumericType type = NumericType.getNumericType(path);
            if (type == NumericType.NOT_NUMERIC) {
                return this; // skip
            }

            // check all
            if (FORBIDDEN_NUMERIC_PATTERN_CHARS.containsSome(value)) {
                UnicodeSet chars = new UnicodeSet().addAll(value);
                chars.retainAll(FORBIDDEN_NUMERIC_PATTERN_CHARS);
                result.add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.illegalCharactersInNumberPattern)
                    .setMessage("Pattern contains forbidden characters: \u200E{0}\u200E",
                        new Object[] { chars.toPattern(false) }));
            }

            // get the final type
            XPathParts parts = new XPathParts().set(path);
            String lastType = parts.getAttributeValue(-1, "type");
            int zeroCount = 0;
            // it can only be null or an integer of the form 10+
            if (lastType != null && !lastType.equals("standard")) {
                Matcher matcher = ALLOWED_INTEGER.matcher(lastType);
                if (matcher.matches()) {
                    zeroCount = matcher.end(1) - matcher.start(1); // number of ascii zeros
                } else {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.badNumericType)
                        .setMessage("The type of a numeric pattern must be missing or of the form 10...."));
                }
            }

            // Check for consistency in short/long decimal formats.
            if (parts.containsElement("decimalFormatLength") && parts.containsAttribute("count")) {
                checkDecimalFormatConsistency(parts, path, value, result);
            }

            // Check for sane usage of grouping separators.
            if (COMMA_ABUSE.matcher(value).find()) {
                result
                    .add(new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.tooManyGroupingSeparators)
                        .setMessage(
                            "Grouping separator (,) should not be used to group tens. Check if a decimal symbol (.) should have been used instead."));
            } else {
                // check that we have a canonical pattern
                String pattern = getCanonicalPattern(value, type, zeroCount, isPOSIX);
                if (!pattern.equals(value)) {
                    result.add(new CheckStatus()
                        .setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.numberPatternNotCanonical)
                        .setMessage("Value should be \u200E{0}\u200E", new Object[] { pattern }));
                }
            }
            // Make sure currency patterns contain a currency symbol
            if (type == NumericType.CURRENCY) {
                String[] currencyPatterns = value.split(";", 2);
                for (int i = 0; i < currencyPatterns.length; i++) {
                    if (currencyPatterns[i].indexOf("\u00a4") < 0)
                        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.currencyPatternMissingCurrencySymbol)
                            .setMessage("Currency formatting pattern must contain a currency symbol."));
                }
            }

            // Make sure percent formatting patterns contain a percent symbol
            if (type == NumericType.PERCENT) {
                if (value.indexOf("%") < 0)
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.percentPatternMissingPercentSymbol)
                        .setMessage("Percentage formatting pattern must contain a % symbol."));
            }

        } catch (Exception e) {
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.illegalNumberFormat)
                .setMessage("Error in creating number format {0}; {1}",
                    new Object[] { e.getClass().getName(), e }));
        }
        return this;
    }

    /**
     * Override this method if you are going to provide examples of usage.
     * Only needed for more complicated cases, like number patterns.
     */
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
            // don't worry about errors here, they'll be caught above.
        }
        return this;
    }

    private void checkDecimalFormatConsistency(XPathParts parts, String path, String value, List<CheckStatus> result) {
        // Look for duplicates of decimal formats with the same number
        // system and type.
        // Decimal formats of the same type should have the same number
        // of integer digits in all the available plural forms.
        DecimalFormat format = new DecimalFormat(value);
        int numIntegerDigits = format.getMinimumIntegerDigits();
        String countString = parts.getAttributeValue(-1, "count");
        Count thisCount = null;
        try {
            thisCount = Count.valueOf(countString);
        } catch (Exception e) {
            // can happen if count is numeric literal, like "1"
        }
        CLDRFile resolvedFile = getResolvedCldrFileToCheck();
        Set<String> inconsistentItems = new TreeSet<String>();
        Set<Count> otherCounts = new HashSet<Count>(pluralTypes);
        if (thisCount != null) {
            otherCounts.remove(thisCount);
        }
        for (Count count : otherCounts) {
            parts.setAttribute("pattern", "count", count.toString());
            String otherPattern = resolvedFile.getWinningValue(parts.toString());
            if (otherPattern == null) continue;
            format = new DecimalFormat(otherPattern);
            if (format.getMinimumIntegerDigits() != numIntegerDigits) {
                PathHeader pathHeader = pathHeaderFactory.fromPath(parts.toString());
                inconsistentItems.add(pathHeader.getHeaderCode());
            }
        }
        if (inconsistentItems.size() > 0) {
            // Get label for items of this type by removing the count.
            PathHeader pathHeader = pathHeaderFactory.fromPath(path.substring(0, path.lastIndexOf('[')));
            String groupHeaderString = pathHeader.getHeaderCode();
            boolean isWinningValue = resolvedFile.getWinningValue(path).equals(value);
            result.add(new CheckStatus().setCause(this)
                .setMainType(isWinningValue ? CheckStatus.errorType : CheckStatus.warningType)
                .setSubtype(Subtype.inconsistentPluralFormat)
                .setMessage("All values for {0} must the same number of digits. " +
                    "The number of zeros in this pattern is inconsistent with the following: {1}.",
                    groupHeaderString,
                    inconsistentItems.toString()));
        }
    }

    /**
     * This method builds a decimal format (based on whether the pattern is for currencies or not)
     * and tests samples.
     */
    private void checkPattern(String path, String fullPath, String value, List result, boolean generateExamples)
        throws ParseException {
        if (value.indexOf('\u00a4') >= 0) { // currency pattern
            DecimalFormat x = icuServiceBuilder.getCurrencyFormat("XXX");
            addOrTestSamples(x, x.toPattern(), value, result, generateExamples);
        } else {
            DecimalFormat x = icuServiceBuilder.getNumberFormat(value);
            addOrTestSamples(x, value, "", result, generateExamples);
        }
    }

    /**
     * Check some currency patterns.
     */
    private void checkCurrencyFormats(String path, String fullPath, String value, List result, boolean generateExamples)
        throws ParseException {
        DecimalFormat x = icuServiceBuilder.getCurrencyFormat(CLDRFile.getCode(path));
        addOrTestSamples(x, x.toPattern(), value, result, generateExamples);
    }

    /**
     * Generates some samples. If we are producing examples, these are used for that; otherwise
     * they are just tested.
     */
    private void addOrTestSamples(DecimalFormat x, String pattern, String context, List result, boolean generateExamples)
        throws ParseException {
        // Object[] arguments = new Object[3];
        //
        // double sample = getRandomNumber();
        // arguments[0] = String.valueOf(sample);
        // String formatted = x.format(sample);
        // arguments[1] = formatted;
        // boolean gotFailure = false;
        // try {
        // parsePosition.setIndex(0);
        // double parsed = x.parse(formatted, parsePosition).doubleValue();
        // if (parsePosition.getIndex() != formatted.length()) {
        // arguments[2] = "Couldn't parse past: " + "\u200E" + formatted.substring(0,parsePosition.getIndex()) +
        // "\u200E";
        // gotFailure = true;
        // } else {
        // arguments[2] = String.valueOf(parsed);
        // }
        // } catch (Exception e) {
        // arguments[2] = e.getMessage();
        // gotFailure = true;
        // }
        // htmlMessage.append(pattern1)
        // .append(TransliteratorUtilities.toXML.transliterate(String.valueOf(sample)))
        // .append(pattern2)
        // .append(TransliteratorUtilities.toXML.transliterate(formatted))
        // .append(pattern3)
        // .append(TransliteratorUtilities.toXML.transliterate(String.valueOf(parsed)))
        // .append(pattern4);
        // if (generateExamples || gotFailure) {
        // result.add(new CheckStatus()
        // .setCause(this).setType(CheckStatus.exampleType)
        // .setMessage(SampleList, arguments));
        // }
        if (generateExamples) {
            result.add(new MyCheckStatus()
                .setFormat(x, context)
                .setCause(this).setMainType(CheckStatus.demoType));
        }
    }

    /**
     * Generate a randome number for testing, with a certain number of decimal places, and
     * half the time negative
     */
    private static double getRandomNumber() {
        // min = 12345.678
        double rand = random.nextDouble();
        // System.out.println(rand);
        double sample = Math.round(rand * 100000.0 * 1000.0) / 1000.0 + 10000.0;
        if (random.nextBoolean()) sample = -sample;
        return sample;
    }

    /*
     * static String pattern1 =
     * "<table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse' style='width: 100%'>"
     * + "<tr>"
     * + "<td nowrap width='1%'>Input:</td>"
     * + "<td><input type='text' name='T1' size='50' style='width: 100%' value='";
     * static String pattern2 = "'></td>"
     * + "<td nowrap width='1%'><input type='submit' value='Test' name='B1'></td>"
     * + "<td nowrap width='1%'>Formatted:</td>"
     * + "<td><input type='text' name='T2' size='50' style='width: 100%' value='";
     * static String pattern3 = "'></td>"
     * + "<td nowrap width='1%'>Parsed:</td>"
     * + "<td><input type='text' name='T3' size='50' style='width: 100%' value='";
     * static String pattern4 = "'></td>"
     * + "</tr>"
     * + "</table>";
     */

    /**
     * Produce a canonical pattern, which will vary according to type and whether it is posix or not.
     * 
     * @param path
     */
    public static String getCanonicalPattern(String inpattern, NumericType type, int zeroCount, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;
        DecimalFormat df = new DecimalFormat(inpattern);
        String pattern;

        if (zeroCount == 0) {
            int[] digits = isPOSIX ? type.getPosixDigitCount() : type.getDigitCount();
            df.setMinimumIntegerDigits(digits[0]);
            df.setMinimumFractionDigits(digits[1]);
            df.setMaximumFractionDigits(digits[2]);
            pattern = df.toPattern();
        } else { // of form 1000. Result must be 0+(.0+)?
            df.setMaximumFractionDigits(df.getMinimumFractionDigits());
            int minimumIntegerDigits = df.getMinimumIntegerDigits();
            if (minimumIntegerDigits < 1) minimumIntegerDigits = 1;
            df.setMaximumIntegerDigits(minimumIntegerDigits);
            pattern = df.toPattern().replace("#", ""); // HACK: brute-force remove any remaining #.
        }

        // int pos = pattern.indexOf(';');
        // if (pos < 0) return pattern + ";-" + pattern;
        return pattern;
    }

    /**
     * You don't normally need this, unless you are doing a demo also.
     */
    static public class MyCheckStatus extends CheckStatus {
        private DecimalFormat df;
        String context;

        public MyCheckStatus setFormat(DecimalFormat df, String context) {
            this.df = df;
            this.context = context;
            return this;
        }

        public SimpleDemo getDemo() {
            return new MyDemo().setFormat(df);
        }
    }

    /**
     * Here is how to do a demo.
     * You provide the function getArguments that takes in-and-out parameters.
     */
    static class MyDemo extends FormatDemo {
        private DecimalFormat df;

        protected String getPattern() {
            return df.toPattern();
        }

        protected String getSampleInput() {
            return String.valueOf(ExampleGenerator.NUMBER_SAMPLE);
        }

        public MyDemo setFormat(DecimalFormat df) {
            this.df = df;
            return this;
        }

        protected void getArguments(Map inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            double d;
            try {
                currentPattern = (String) inout.get("pattern");
                if (currentPattern != null)
                    df.applyPattern(currentPattern);
                else
                    currentPattern = getPattern();
            } catch (Exception e) {
                currentPattern = "Use format like: ##,###.##";
                return;
            }
            try {
                currentInput = (String) inout.get("input");
                if (currentInput == null) {
                    currentInput = getSampleInput();
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
                    currentReparsed = "Couldn't parse past: \u200E"
                        + currentFormatted.substring(0, parsePosition.getIndex()) + "\u200E";
                } else {
                    currentReparsed = n.toString();
                }
            } catch (Exception e) {
                currentReparsed = "Can't parse: " + e.getMessage();
            }
        }

    }
}
