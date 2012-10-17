/* Copyright (C) 2007-2010 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2010 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {

    public static final boolean DEBUG_DAIP = CldrUtility.getProperty("DEBUG_DAIP", false);

    public static final UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]")
        .freeze();

    public static final UnicodeSet TO_QUOTE = (UnicodeSet) new UnicodeSet(
        "[[:Cn:]" +
            "[:Default_Ignorable_Code_Point:]" +
            "[:patternwhitespace:]" +
            "[:Me:][:Mn:]]" // add non-spacing marks
    ).freeze();

    public static final Pattern NUMBER_FORMAT_XPATH = Pattern
        .compile("//ldml/numbers/.*Format\\[@type=\"standard\"]/pattern.*");
    private static final Pattern NON_DECIMAL_PERIOD = Pattern.compile("(?<![0#'])\\.(?![0#'])");
    private static final Pattern WHITESPACE_NO_NBSP_TO_NORMALIZE = Pattern.compile("\\s+"); // string of whitespace not
                                                                                            // including NBSP, i.e. [
                                                                                            // \t\n\r]+
    private static final Pattern WHITESPACE_AND_NBSP_TO_NORMALIZE = Pattern.compile("[\\s\\u00A0]+"); // string of
                                                                                                      // whitespace
                                                                                                      // including NBSP,
                                                                                                      // i.e. [
                                                                                                      // \u00A0\t\n\r]+

    private Collator col;

    private Collator spaceCol;

    private FormatParser formatDateParser = new FormatParser();

    private PrettyPrinter pp;

    final private CLDRLocale locale;
    private static final CLDRLocale MALAYALAM = CLDRLocale.getInstance("ml");
    private boolean isPosix;

    /**
     * Constructor, taking cldrFile.
     * 
     * @param cldrFileToCheck
     */
    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
        init(this.locale = CLDRLocale.getInstance(cldrFileToCheck.getLocaleID()));
    }

    void init(CLDRLocale locale) {
        isPosix = locale.toString().indexOf("POSIX") >= 0;
        col = Collator.getInstance(locale.toULocale());
        spaceCol = Collator.getInstance(locale.toULocale());

        pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
            .setCompressRanges(true)
            .setToQuote(new UnicodeSet(TO_QUOTE))
            .setOrdering(col)
            .setSpaceComparator(spaceCol);
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale) {
        init(this.locale = CLDRLocale.getInstance(locale));
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(CLDRLocale locale) {
        init(this.locale = locale);
    }

    /**
     * Process the value for display. The result is a string for display in the
     * Survey tool or similar program.
     * 
     * @param path
     * @param value
     * @param fullPath
     * @return
     */
    public synchronized String processForDisplay(String path, String value) {
        if (path.contains("exemplarCharacters")) {
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }

            value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
            value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

            // if (RTL.containsSome(value) && value.startsWith("[") && value.endsWith("]")) {
            // return "\u200E[\u200E" + value.substring(1,value.length()-2) + "\u200E]\u200E";
            // }
        } else if (path.contains("stopword")) {
            return value.trim().isEmpty() ? "NONE" : value;
        } else {
            NumericType numericType = NumericType.getNumericType(path);
            if (numericType != NumericType.NOT_NUMERIC) {
                // Canonicalize existing values that aren't canonicalized yet.
                // New values will be canonicalized on input using processInput().
                try {
                    value = getCanonicalPattern(value, numericType, isPosix);
                } catch (IllegalArgumentException e) {
                    if (DEBUG_DAIP) System.err.println("Illegal pattern: " + value);
                }
                if (numericType != NumericType.CURRENCY) {
                    value = value.replace("'", "");
                }
            }
        }
        return value;
    }

    static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();

    /**
     * Process the value for input. The result is a cleaned-up value. For example,
     * an exemplar set is modified to be in the normal format, and any missing [ ]
     * are added (a common omission on entry). If there are any failures then the
     * original value is returned, so that the proper error message can be given.
     * 
     * @param path
     * @param value
     * @param internalException
     *            TODO
     * @param fullPath
     * @return
     */
    public synchronized String processInput(String path, String value, Exception[] internalException) {
        String original = value;
        if (internalException != null) {
            internalException[0] = null;
        }
        try {
            // Normalise Malayalam characters.
            if (locale.childOf(MALAYALAM)) {
                String newvalue = normalizeMalayalam(value);
                if (DEBUG_DAIP) System.out.println("DAIP: Normalized Malayalam '" + value + "' to '" + newvalue + "'");
                value = newvalue;
            }

            // turn all whitespace sequences (including tab and newline, and NBSP for certain paths)
            // into a single space or a single NBSP depending on path.
            if ((path.contains("/dateFormatLength") && path.contains("/pattern")) ||
                path.contains("/availableFormats/dateFormatItem") ||
                path.startsWith("//ldml/dates/timeZoneNames/fallbackRegionFormat") ||
                (path.startsWith("//ldml/dates/timeZoneNames/metazone") && path.contains("/long")) ||
                path.startsWith("//ldml/dates/timeZoneNames/regionFormat") ||
                path.startsWith("//ldml/localeDisplayNames/codePatterns/codePattern") ||
                path.startsWith("//ldml/localeDisplayNames/languages/language") ||
                path.startsWith("//ldml/localeDisplayNames/territories/territory") ||
                path.startsWith("//ldml/localeDisplayNames/types/type") ||
                (path.startsWith("//ldml/numbers/currencies/currency") && path.contains("/displayName")) ||
                (path.contains("/decimalFormatLength[@type=\"long\"]") && path.contains("/pattern")) ||
                path.startsWith("//ldml/posix/messages") ||
                (path.startsWith("//ldml/units/uni") && path.contains("/unitPattern "))) {
                value = WHITESPACE_AND_NBSP_TO_NORMALIZE.matcher(value).replaceAll(" "); // replace with regular space
            } else if ((path.contains("/currencies/currency") && (path.contains("/group") || path.contains("/pattern")))
                ||
                (path.contains("/currencyFormatLength") && path.contains("/pattern")) ||
                (path.contains("/currencySpacing") && path.contains("/insertBetween")) ||
                (path.contains("/decimalFormatLength") && path.contains("/pattern")) || // i.e. the non-long ones
                (path.contains("/percentFormatLength") && path.contains("/pattern")) ||
                (path.startsWith("//ldml/numbers/symbols") && (path.contains("/group") || path.contains("/nan")))) {
                value = WHITESPACE_AND_NBSP_TO_NORMALIZE.matcher(value).replaceAll("\u00A0"); // replace with NBSP
            } else {
                // in this case don't normalize away NBSP
                value = WHITESPACE_NO_NBSP_TO_NORMALIZE.matcher(value).replaceAll(" "); // replace with regular space
            }

            // all of our values should not have leading or trailing spaces, except insertBetween
            if (!path.contains("/insertBetween") && !path.contains("/localeSeparator")) {
                value = value.trim();
            }

            // fix grouping separator if space
            if (path.startsWith("//ldml/numbers/symbols") && !path.contains("/alias")) {
                if (value.isEmpty()) {
                    value = "\u00A0";
                }
                value = value.replace(' ', '\u00A0');
            }

            // fix date patterns
            if (hasDatetimePattern(path)) {
                formatDateParser.set(value);
                String newValue = formatDateParser.toString();
                if (!value.equals(newValue)) {
                    value = newValue;
                }
            }

            NumericType numericType = NumericType.getNumericType(path);
            if (numericType != NumericType.NOT_NUMERIC) {
                if (numericType == NumericType.CURRENCY) {
                    value = value.replaceAll(" ", "\u00A0");
                } else {
                    value = value.replaceAll("([%\u00A4]) ", "$1\u00A0")
                        .replaceAll(" ([%\u00A4])", "\u00A0$1");
                    value = replace(NON_DECIMAL_PERIOD, value, "'.'");
                    if (numericType == NumericType.DECIMAL_ABBREVIATED) {
                        value = value.replaceAll("0\\.0+", "0");
                    }
                }
                value = getCanonicalPattern(value, numericType, isPosix);
            }

            // check specific cases
            if (path.contains("/exemplarCharacters")) {
                final String iValue = value;
                // clean up the user's input.
                // first, fix up the '['
                value = value.trim();

                // remove brackets and trim again before regex
                if (value.startsWith("[")) {
                    value = value.substring(1);
                }
                if (value.endsWith("]")) {
                    value = value.substring(0, value.length() - 1);
                }
                value = value.trim();

                value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
                value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

                // re-add brackets.
                value = "[" + value + "]";

                UnicodeSet exemplar = new UnicodeSet(value);
                XPathParts parts = new XPathParts().set(path);
                final String type = parts.getAttributeValue(-1, "type");
                ExemplarType exemplarType = type == null ? ExemplarType.main : ExemplarType.valueOf(type);
                value = getCleanedUnicodeSet(exemplar, pp, exemplarType);
            } else if (path.contains("stopword")) {
                if (value.equals("NONE")) {
                    value = "";
                }
            }
            return value;
        } catch (RuntimeException e) {
            if (internalException != null) {
                internalException[0] = e;
            }
            return original;
        }
    }

    private String replace(Pattern pattern, String value, String replacement) {
        String value2 = pattern.matcher(value).replaceAll(replacement);
        if (!value.equals(value2)) {
            System.out.println("\n" + value + " => " + value2);
        }
        return value2;
    }

    private static Pattern UNNORMALIZED_MALAYALAM = Pattern.compile(
        "(\u0D23|\u0D28|\u0D30|\u0D32|\u0D33|\u0D15)\u0D4D\u200D");

    private static Map<Character, Character> NORMALIZING_MAP =
        Builder.with(new HashMap<Character, Character>())
            .put('\u0D23', '\u0D7A').put('\u0D28', '\u0D7B')
            .put('\u0D30', '\u0D7C').put('\u0D32', '\u0D7D')
            .put('\u0D33', '\u0D7E').put('\u0D15', '\u0D7F').get();

    /**
     * Normalizes the Malayalam characters in the specified input.
     * 
     * @param value
     *            the input to be normalized
     * @return
     */
    private String normalizeMalayalam(String value) {
        // Normalize Malayalam characters.
        Matcher matcher = UNNORMALIZED_MALAYALAM.matcher(value);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            int start = 0;
            do {
                buffer.append(value.substring(start, matcher.start(0)));
                char codePoint = matcher.group(1).charAt(0);
                buffer.append(NORMALIZING_MAP.get(codePoint));
                start = matcher.end(0);
            } while (matcher.find());
            buffer.append(value.substring(start));
            value = buffer.toString();
        }
        return value;
    }

    static Pattern REMOVE_QUOTE1 = Pattern.compile("(\\s)(\\\\[-\\}\\]\\&])()");
    static Pattern REMOVE_QUOTE2 = Pattern.compile("(\\\\[\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    static Pattern NEEDS_QUOTE1 = Pattern.compile("(\\s|$)([-\\}\\]\\&])()");
    static Pattern NEEDS_QUOTE2 = Pattern.compile("([^\\\\])([\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    public static boolean hasDatetimePattern(String path) {
        return path.indexOf("/dates") >= 0
            && ((path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0)
                || path.indexOf("/dateFormatItem") >= 0
                || path.contains("/intervalFormatItem"));
    }

    public static String getCleanedUnicodeSet(UnicodeSet exemplar, PrettyPrinter prettyPrinter,
        ExemplarType exemplarType) {
        String value;
        prettyPrinter.setCompressRanges(exemplar.size() > 100);
        value = exemplar.toPattern(false);
        UnicodeSet toAdd = new UnicodeSet();

        for (UnicodeSetIterator usi = new UnicodeSetIterator(exemplar); usi.next();) {
            String string = usi.getString();
            if (string.equals("ß") || string.equals("İ")) {
                toAdd.add(string);
                continue;
            }
            if (exemplarType.convertUppercase) {
                string = UCharacter.toLowerCase(ULocale.ENGLISH, string);
            }
            toAdd.add(string);
            String composed = Normalizer.compose(string, false);
            if (!string.equals(composed)) {
                toAdd.add(composed);
            }
        }

        toAdd.removeAll(exemplarType.toRemove);

        if (DEBUG_DAIP && !toAdd.equals(exemplar)) {
            UnicodeSet oldOnly = new UnicodeSet(exemplar).removeAll(toAdd);
            UnicodeSet newOnly = new UnicodeSet(toAdd).removeAll(exemplar);
            System.out.println("Exemplar:\t" + exemplarType + ",\tremoved\t" + oldOnly + ",\tadded\t" + newOnly);
        }

        String fixedExemplar = prettyPrinter.format(toAdd);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar);
        if (!toAdd.equals(doubleCheck)) {
            // something went wrong, leave as is
        } else if (!value.equals(fixedExemplar)) { // put in this condition just for debugging
            if (DEBUG_DAIP) {
                System.out.println(TestMetadata.showDifference(
                    With.codePoints(value),
                    With.codePoints(fixedExemplar),
                    "\n"));
            }
            value = fixedExemplar;
        }
        return value;
    }

    /**
     * @return a canonical numeric pattern, based on the type, and the isPOSIX flag. The latter is set for en_US_POSIX.
     */
    public static String getCanonicalPattern(String inpattern, NumericType type, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;
        DecimalFormat df = new DecimalFormat(inpattern);
        if (type == NumericType.DECIMAL_ABBREVIATED) {
            return inpattern; // TODO fix when ICU bug is fixed
            // df.setMaximumFractionDigits(df.getMinimumFractionDigits());
            // df.setMaximumIntegerDigits(Math.max(1, df.getMinimumIntegerDigits()));
        } else {
            // int decimals = type == CURRENCY_TYPE ? 2 : 1;
            int[] digits = isPOSIX ? type.posixDigitCount : type.digitCount;
            df.setMinimumIntegerDigits(digits[0]);
            df.setMinimumFractionDigits(digits[1]);
            df.setMaximumFractionDigits(digits[2]);
        }
        String pattern = df.toPattern();

        // int pos = pattern.indexOf(';');
        // if (pos < 0) return pattern + ";-" + pattern;
        return pattern;
    }

    /*
     * This tests what type a numeric pattern is.
     */
    public enum NumericType {
        CURRENCY(new int[] { 1, 2, 2 }, new int[] { 1, 2, 2 }),
        DECIMAL(new int[] { 1, 0, 3 }, new int[] { 1, 0, 6 }),
        DECIMAL_ABBREVIATED(),
        PERCENT(new int[] { 1, 0, 0 }, new int[] { 1, 0, 0 }),
        SCIENTIFIC(new int[] { 0, 0, 0 }, new int[] { 1, 6, 6 }),
        NOT_NUMERIC;

        private static final Pattern NUMBER_PATH = Pattern
            .compile("//ldml/numbers/((currency|decimal|percent|scientific)Formats|currencies/currency).*");
        private int[] digitCount;
        private int[] posixDigitCount;

        private NumericType() {
        };

        private NumericType(int[] digitCount, int[] posixDigitCount) {
            this.digitCount = digitCount;
            this.posixDigitCount = posixDigitCount;
        }

        /**
         * @return the numeric type of the xpath
         */
        public static NumericType getNumericType(String xpath) {
            Matcher matcher = NUMBER_PATH.matcher(xpath);
            if (xpath.indexOf("/pattern") < 0) {
                return NOT_NUMERIC;
            } else if (matcher.matches()) {
                if (matcher.group(1).equals("currencies/currency")) {
                    return CURRENCY;
                } else {
                    NumericType type = NumericType.valueOf(matcher.group(2).toUpperCase());
                    if (type == DECIMAL && xpath.contains("=\"1000")) {
                        type = DECIMAL_ABBREVIATED;
                    }
                    return type;
                }
            } else {
                return NOT_NUMERIC;
            }
        }

        public int[] getDigitCount() {
            return digitCount;
        }

        public int[] getPosixDigitCount() {
            return posixDigitCount;
        }
    };
}
