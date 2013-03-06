/* Copyright (C) 2007-2010 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2010 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {

    private static final boolean FIX_YEARS = true;

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
    static final DateTimeCanonicalizer dtc = new DateTimeCanonicalizer(FIX_YEARS);

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
            DateTimePatternType datetimePatternType = DateTimePatternType.fromPath(path);
            if (DateTimePatternType.STOCK_AVAILABLE_INTERVAL_PATTERNS.contains(datetimePatternType)) {
                value = dtc.getCanonicalDatePattern(path, value, datetimePatternType);
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

            // Normalize ellipsis data.
            if (path.startsWith("//ldml/characters/ellipsis")) {
                value = value.replace("...", "…");
            }

            // Replace Arabic presentation forms with their nominal counterparts
            value = replaceArabicPresentationForms(value);
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
        if (DEBUG_DAIP && !value.equals(value2)) {
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
    private static Map<Integer, Integer> ARABIC_PRESENTATION_FORMS_MAP =
        Builder.with(new HashMap<Integer, Integer>())
            .put(0xFB50, 0x0671)
            .put(0xFB51, 0x0671)
            .put(0xFB52, 0x067B)
            .put(0xFB53, 0x067B)
            .put(0xFB54, 0x067B)
            .put(0xFB55, 0x067B)
            .put(0xFB56, 0x067E)
            .put(0xFB57, 0x067E)
            .put(0xFB58, 0x067E)
            .put(0xFB59, 0x067E)
            .put(0xFB5A, 0x0680)
            .put(0xFB5B, 0x0680)
            .put(0xFB5C, 0x0680)
            .put(0xFB5D, 0x0680)
            .put(0xFB5E, 0x067A)
            .put(0xFB5F, 0x067A)
            .put(0xFB60, 0x067A)
            .put(0xFB61, 0x067A)
            .put(0xFB62, 0x067F)
            .put(0xFB63, 0x067F)
            .put(0xFB64, 0x067F)
            .put(0xFB65, 0x067F)
            .put(0xFB66, 0x0679)
            .put(0xFB67, 0x0679)
            .put(0xFB68, 0x0679)
            .put(0xFB69, 0x0679)
            .put(0xFB6A, 0x06A4)
            .put(0xFB6B, 0x06A4)
            .put(0xFB6C, 0x06A4)
            .put(0xFB6D, 0x06A4)
            .put(0xFB6E, 0x06A6)
            .put(0xFB6F, 0x06A6)
            .put(0xFB70, 0x06A6)
            .put(0xFB71, 0x06A6)
            .put(0xFB72, 0x0684)
            .put(0xFB73, 0x0684)
            .put(0xFB74, 0x0684)
            .put(0xFB75, 0x0684)
            .put(0xFB76, 0x0683)
            .put(0xFB77, 0x0683)
            .put(0xFB78, 0x0683)
            .put(0xFB79, 0x0683)
            .put(0xFB7A, 0x0686)
            .put(0xFB7B, 0x0686)
            .put(0xFB7C, 0x0686)
            .put(0xFB7D, 0x0686)
            .put(0xFB7E, 0x0687)
            .put(0xFB7F, 0x0687)
            .put(0xFB80, 0x0687)
            .put(0xFB81, 0x0687)
            .put(0xFB82, 0x068D)
            .put(0xFB83, 0x068D)
            .put(0xFB84, 0x068C)
            .put(0xFB85, 0x068C)
            .put(0xFB86, 0x068E)
            .put(0xFB87, 0x068E)
            .put(0xFB88, 0x0688)
            .put(0xFB89, 0x0688)
            .put(0xFB8A, 0x0698)
            .put(0xFB8B, 0x0698)
            .put(0xFB8C, 0x0691)
            .put(0xFB8D, 0x0691)
            .put(0xFB8E, 0x06A9)
            .put(0xFB8F, 0x06A9)
            .put(0xFB90, 0x06A9)
            .put(0xFB91, 0x06A9)
            .put(0xFB92, 0x06AF)
            .put(0xFB93, 0x06AF)
            .put(0xFB94, 0x06AF)
            .put(0xFB95, 0x06AF)
            .put(0xFB96, 0x06B3)
            .put(0xFB97, 0x06B3)
            .put(0xFB98, 0x06B3)
            .put(0xFB99, 0x06B3)
            .put(0xFB9A, 0x06B1)
            .put(0xFB9B, 0x06B1)
            .put(0xFB9C, 0x06B1)
            .put(0xFB9D, 0x06B1)
            .put(0xFB9E, 0x06BA)
            .put(0xFB9F, 0x06BA)
            .put(0xFBA0, 0x06BB)
            .put(0xFBA1, 0x06BB)
            .put(0xFBA2, 0x06BB)
            .put(0xFBA3, 0x06BB)
            .put(0xFBA4, 0x06C0)
            .put(0xFBA5, 0x06C0)
            .put(0xFBA6, 0x06C1)
            .put(0xFBA7, 0x06C1)
            .put(0xFBA8, 0x06C1)
            .put(0xFBA9, 0x06C1)
            .put(0xFBAA, 0x06BE)
            .put(0xFBAB, 0x06BE)
            .put(0xFBAC, 0x06BE)
            .put(0xFBAD, 0x06BE)
            .put(0xFBAE, 0x06D2)
            .put(0xFBAF, 0x06D2)
            .put(0xFBB0, 0x06D3)
            .put(0xFBB1, 0x06D3)
            .put(0xFBD3, 0x06AD)
            .put(0xFBD4, 0x06AD)
            .put(0xFBD5, 0x06AD)
            .put(0xFBD6, 0x06AD)
            .put(0xFBD7, 0x06C7)
            .put(0xFBD8, 0x06C7)
            .put(0xFBD9, 0x06C6)
            .put(0xFBDA, 0x06C6)
            .put(0xFBDB, 0x06C8)
            .put(0xFBDC, 0x06C8)
            .put(0xFBDD, 0x0677)
            .put(0xFBDE, 0x06CB)
            .put(0xFBDF, 0x06CB)
            .put(0xFBE0, 0x06C5)
            .put(0xFBE1, 0x06C5)
            .put(0xFBE2, 0x06C9)
            .put(0xFBE3, 0x06C9)
            .put(0xFBE4, 0x06D0)
            .put(0xFBE5, 0x06D0)
            .put(0xFBE6, 0x06D0)
            .put(0xFBE7, 0x06D0)
            .put(0xFBE8, 0x0649)
            .put(0xFBE9, 0x0649)
            .put(0xFBFC, 0x06CC)
            .put(0xFBFD, 0x06CC)
            .put(0xFBFE, 0x06CC)
            .put(0xFBFF, 0x06CC)
            .put(0xFE80, 0x0621)
            .put(0xFE81, 0x0622)
            .put(0xFE82, 0x0622)
            .put(0xFE83, 0x0623)
            .put(0xFE84, 0x0623)
            .put(0xFE85, 0x0624)
            .put(0xFE86, 0x0624)
            .put(0xFE87, 0x0625)
            .put(0xFE88, 0x0625)
            .put(0xFE89, 0x0626)
            .put(0xFE8A, 0x0626)
            .put(0xFE8B, 0x0626)
            .put(0xFE8C, 0x0626)
            .put(0xFE8D, 0x0627)
            .put(0xFE8E, 0x0627)
            .put(0xFE8F, 0x0628)
            .put(0xFE90, 0x0628)
            .put(0xFE91, 0x0628)
            .put(0xFE92, 0x0628)
            .put(0xFE93, 0x0629)
            .put(0xFE94, 0x0629)
            .put(0xFE95, 0x062A)
            .put(0xFE96, 0x062A)
            .put(0xFE97, 0x062A)
            .put(0xFE98, 0x062A)
            .put(0xFE99, 0x062B)
            .put(0xFE9A, 0x062B)
            .put(0xFE9B, 0x062B)
            .put(0xFE9C, 0x062B)
            .put(0xFE9D, 0x062C)
            .put(0xFE9E, 0x062C)
            .put(0xFE9F, 0x062C)
            .put(0xFEA0, 0x062C)
            .put(0xFEA1, 0x062D)
            .put(0xFEA2, 0x062D)
            .put(0xFEA3, 0x062D)
            .put(0xFEA4, 0x062D)
            .put(0xFEA5, 0x062E)
            .put(0xFEA6, 0x062E)
            .put(0xFEA7, 0x062E)
            .put(0xFEA8, 0x062E)
            .put(0xFEA9, 0x062F)
            .put(0xFEAA, 0x062F)
            .put(0xFEAB, 0x0630)
            .put(0xFEAC, 0x0630)
            .put(0xFEAD, 0x0631)
            .put(0xFEAE, 0x0631)
            .put(0xFEAF, 0x0632)
            .put(0xFEB0, 0x0632)
            .put(0xFEB1, 0x0633)
            .put(0xFEB2, 0x0633)
            .put(0xFEB3, 0x0633)
            .put(0xFEB4, 0x0633)
            .put(0xFEB5, 0x0634)
            .put(0xFEB6, 0x0634)
            .put(0xFEB7, 0x0634)
            .put(0xFEB8, 0x0634)
            .put(0xFEB9, 0x0635)
            .put(0xFEBA, 0x0635)
            .put(0xFEBB, 0x0635)
            .put(0xFEBC, 0x0635)
            .put(0xFEBD, 0x0636)
            .put(0xFEBE, 0x0636)
            .put(0xFEBF, 0x0636)
            .put(0xFEC0, 0x0636)
            .put(0xFEC1, 0x0637)
            .put(0xFEC2, 0x0637)
            .put(0xFEC3, 0x0637)
            .put(0xFEC4, 0x0637)
            .put(0xFEC5, 0x0638)
            .put(0xFEC6, 0x0638)
            .put(0xFEC7, 0x0638)
            .put(0xFEC8, 0x0638)
            .put(0xFEC9, 0x0639)
            .put(0xFECA, 0x0639)
            .put(0xFECB, 0x0639)
            .put(0xFECC, 0x0639)
            .put(0xFECD, 0x063A)
            .put(0xFECE, 0x063A)
            .put(0xFECF, 0x063A)
            .put(0xFED0, 0x063A)
            .put(0xFED1, 0x0641)
            .put(0xFED2, 0x0641)
            .put(0xFED3, 0x0641)
            .put(0xFED4, 0x0641)
            .put(0xFED5, 0x0642)
            .put(0xFED6, 0x0642)
            .put(0xFED7, 0x0642)
            .put(0xFED8, 0x0642)
            .put(0xFED9, 0x0643)
            .put(0xFEDA, 0x0643)
            .put(0xFEDB, 0x0643)
            .put(0xFEDC, 0x0643)
            .put(0xFEDD, 0x0644)
            .put(0xFEDE, 0x0644)
            .put(0xFEDF, 0x0644)
            .put(0xFEE0, 0x0644)
            .put(0xFEE1, 0x0645)
            .put(0xFEE2, 0x0645)
            .put(0xFEE3, 0x0645)
            .put(0xFEE4, 0x0645)
            .put(0xFEE5, 0x0646)
            .put(0xFEE6, 0x0646)
            .put(0xFEE7, 0x0646)
            .put(0xFEE8, 0x0646)
            .put(0xFEE9, 0x0647)
            .put(0xFEEA, 0x0647)
            .put(0xFEEB, 0x0647)
            .put(0xFEEC, 0x0647)
            .put(0xFEED, 0x0648)
            .put(0xFEEE, 0x0648)
            .put(0xFEEF, 0x0649)
            .put(0xFEF0, 0x0649)
            .put(0xFEF1, 0x064A)
            .put(0xFEF2, 0x064A)
            .put(0xFEF3, 0x064A)
            .put(0xFEF4, 0x064A)
            .freeze();

    /**
     * Normalizes the Arabic presentation forms characters in the specified input.
     * 
     * @param value
     *            the input to be normalized
     * @return
     */
    private String replaceArabicPresentationForms(String value) {
        UCharacterIterator it = UCharacterIterator.getInstance(value);
        StringBuffer buffer = new StringBuffer();
        int cp;
        while ((cp = it.nextCodePoint()) != UCharacterIterator.DONE) {
            switch (UCharacter.UnicodeBlock.of(cp).getID()) {
                case UCharacter.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A_ID:
                case UCharacter.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B_ID:
                    Integer result = ARABIC_PRESENTATION_FORMS_MAP.get(cp);
                    if (result != null) {
                        cp = result;
                    }
                    buffer.appendCodePoint(cp);
                    break;
                default:
                    buffer.appendCodePoint(cp);
            }
        }
        value = buffer.toString();
        return value;
    }

    static Pattern REMOVE_QUOTE1 = Pattern.compile("(\\s)(\\\\[-\\}\\]\\&])()");
    static Pattern REMOVE_QUOTE2 = Pattern.compile("(\\\\[\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    static Pattern NEEDS_QUOTE1 = Pattern.compile("(\\s|$)([-\\}\\]\\&])()");
    static Pattern NEEDS_QUOTE2 = Pattern.compile("([^\\\\])([\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

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
