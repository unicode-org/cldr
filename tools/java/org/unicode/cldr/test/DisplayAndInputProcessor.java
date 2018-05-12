/* Copyright (C) 2007-2013 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2013 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.MyanmarZawgyiConverter;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
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

    public static final Pattern NUMBER_SEPARATOR_PATTERN = Pattern
        .compile("//ldml/numbers/symbols.*/(decimal|group)");

    private static final Pattern APOSTROPHE_SKIP_PATHS = PatternCache.get("//ldml/("
        + "localeDisplayNames/languages/language\\[@type=\"mic\"].*|"
        + "characters/.*|"
        + "delimiters/.*|"
        + "dates/.+/(pattern|intervalFormatItem|dateFormatItem).*|"
        + "units/.+/unitPattern.*|"
        + "units/.+/durationUnitPattern.*|"
        + "numbers/symbols.*|"
        + "numbers/miscPatterns.*|"
        + "numbers/(decimal|currency|percent|scientific)Formats.+/(decimal|currency|percent|scientific)Format.*)");
    private static final Pattern INTERVAL_FORMAT_PATHS = PatternCache.get("//ldml/dates/.+/intervalFormatItem.*");
    private static final Pattern NON_DECIMAL_PERIOD = PatternCache.get("(?<![0#'])\\.(?![0#'])");
    private static final Pattern WHITESPACE_NO_NBSP_TO_NORMALIZE = PatternCache.get("\\s+"); // string of whitespace not
    // including NBSP, i.e. [
    // \t\n\r]+
    private static final Pattern WHITESPACE_AND_NBSP_TO_NORMALIZE = PatternCache.get("[\\s\\u00A0]+"); // string of
    // whitespace
    // including NBSP,
    // i.e. [
    // \u00A0\t\n\r]+
    private static final UnicodeSet UNICODE_WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();

    private static final CLDRLocale MALAYALAM = CLDRLocale.getInstance("ml");
    private static final CLDRLocale ROMANIAN = CLDRLocale.getInstance("ro");
    private static final CLDRLocale CATALAN = CLDRLocale.getInstance("ca");
    private static final CLDRLocale NGOMBA = CLDRLocale.getInstance("jgo");
    private static final CLDRLocale KWASIO = CLDRLocale.getInstance("nmg");
    private static final CLDRLocale HEBREW = CLDRLocale.getInstance("he");
    private static final CLDRLocale MYANMAR = CLDRLocale.getInstance("my");
    private static final CLDRLocale GERMAN_SWITZERLAND = CLDRLocale.getInstance("de_CH");
    private static final CLDRLocale SWISS_GERMAN = CLDRLocale.getInstance("gsw");
    public static final Set<String> LANGUAGES_USING_MODIFIER_APOSTROPHE = new HashSet<String>(
        Arrays.asList("br", "bss", "cch", "gn", "ha", "ha_Latn", "lkt", "mgo", "moh", "nnh", "qu", "quc", "uk", "uz", "uz_Latn"));

    // Ş ş Ţ ţ  =>  Ș ș Ț ț
    private static final char[][] ROMANIAN_CONVERSIONS = {
        { '\u015E', '\u0218' }, { '\u015F', '\u0219' }, { '\u0162', '\u021A' },
        { '\u0163', '\u021B' } };

    private static final char[][] CATALAN_CONVERSIONS = {
        { '\u013F', '\u004C', '\u00B7' }, // Ŀ -> L·
        { '\u0140', '\u006C', '\u00B7' } }; // ŀ -> l·

    private static final char[][] NGOMBA_CONVERSIONS = {
        { '\u0251', '\u0061' }, { '\u0261', '\u0067' }, //  ɑ -> a , ɡ -> g , See ticket #5691
        { '\u2019', '\uA78C' }, { '\u02BC', '\uA78C' } }; //  Saltillo, see ticket #6805

    private static final char[][] KWASIO_CONVERSIONS = {
        { '\u0306', '\u030C' }, // See ticket #6571, use caron instead of breve
        { '\u0103', '\u01CE' }, { '\u0102', '\u01CD' }, // a-breve -> a-caron
        { '\u0115', '\u011B' }, { '\u011A', '\u01CD' }, // e-breve -> e-caron
        { '\u012D', '\u01D0' }, { '\u012C', '\u01CF' }, // i-breve -> i-caron
        { '\u014F', '\u01D2' }, { '\u014E', '\u01D1' }, // o-breve -> o-caron
        { '\u016D', '\u01D4' }, { '\u016C', '\u01D3' } // u-breve -> u-caron
    };

    private static final char[][] HEBREW_CONVERSIONS = {
        { '\'', '\u05F3' }, { '"', '\u05F4' } }; //  ' -> geresh  " -> gershayim

    private Collator col;

    private Collator spaceCol;

    private UnicodeSetPrettyPrinter pp = null;

    final private CLDRLocale locale;
    private boolean isPosix;

    /**
     * Constructor, taking cldrFile.
     *
     * @param cldrFileToCheck
     */
    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck, boolean needsCollator) {
        init(this.locale = CLDRLocale.getInstance(cldrFileToCheck.getLocaleID()), needsCollator);
    }

    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
        init(this.locale = CLDRLocale.getInstance(cldrFileToCheck.getLocaleID()), true);
    }

    void init(CLDRLocale locale, boolean needsCollator) {
        isPosix = locale.toString().indexOf("POSIX") >= 0;
        if (needsCollator) {
            ICUServiceBuilder isb = null;
            try {
                isb = ICUServiceBuilder.forLocale(locale);
            } catch (Exception e) {
            }

            if (isb != null) {
                try {
                    col = isb.getRuleBasedCollator();
                } catch (Exception e) {
                    col = Collator.getInstance(ULocale.ROOT);
                }
            } else {
                col = Collator.getInstance(ULocale.ROOT);
            }

            spaceCol = Collator.getInstance(locale.toULocale());
            if (spaceCol instanceof RuleBasedCollator) {
                ((RuleBasedCollator) spaceCol).setAlternateHandlingShifted(false);
            }
            pp = new UnicodeSetPrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT))
                .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .setToQuote(new UnicodeSet(TO_QUOTE))
                .setOrdering(col)
                .setSpaceComparator(spaceCol);
        }
    }

    public UnicodeSetPrettyPrinter getPrettyPrinter() {
        return pp;
    }

    /**
     * Constructor, taking locale.
     *
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale, boolean needsCollator) {
        init(this.locale = CLDRLocale.getInstance(locale), needsCollator);
    }

    /**
     * Constructor, taking locale.
     *
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale) {
        init(this.locale = CLDRLocale.getInstance(locale), true);
    }

    /**
     * Constructor, taking locale.
     *
     * @param locale
     */
    public DisplayAndInputProcessor(CLDRLocale locale, boolean needsCollator) {
        init(this.locale = locale, needsCollator);
    }

    /**
     * Constructor, taking locale.
     *
     * @param locale
     */
    public DisplayAndInputProcessor(CLDRLocale locale) {
        init(this.locale = locale, true);
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
        value = Normalizer.compose(value, false); // Always normalize all text to NFC.
        if (hasUnicodeSetValue(path)) {
            value = displayUnicodeSet(value);
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
                if (numericType != NumericType.CURRENCY && numericType != NumericType.CURRENCY_ABBREVIATED) {
                    value = value.replace("'", "");
                }
            }
        }
        // Fix up any apostrophes in number symbols
        if (NUMBER_SEPARATOR_PATTERN.matcher(path).matches()) {
            value = value.replace('\'', '\u2019');
        }
        // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
        if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
            value = normalizeApostrophes(value);
        }
        // Fix up hyphens, replacing with N-dash as appropriate
        if (INTERVAL_FORMAT_PATHS.matcher(path).matches()) {
            value = normalizeIntervalHyphens(value);
        } else {
            value = normalizeHyphens(value);
        }
        return value;
    }

    private boolean hasUnicodeSetValue(String path) {
        return path.startsWith("//ldml/characters/exemplarCharacters") || path.startsWith("//ldml/characters/parseLenients");
    }

    static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();
    static final DateTimeCanonicalizer dtc = new DateTimeCanonicalizer(FIX_YEARS);

    public static final Splitter SPLIT_BAR = Splitter.on('|').trimResults().omitEmptyStrings();
    static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Joiner JOIN_BAR = Joiner.on(" | ");

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
        value = Normalizer.compose(value, false); // Always normalize all input to NFC.
        if (internalException != null) {
            internalException[0] = null;
        }
        try {
            // Normalise Malayalam characters.
            boolean isUnicodeSet = hasUnicodeSetValue(path);
            if (locale.childOf(MALAYALAM)) {
                String newvalue = normalizeMalayalam(value);
                if (DEBUG_DAIP) System.out.println("DAIP: Normalized Malayalam '" + value + "' to '" + newvalue + "'");
                value = newvalue;
            } else if (locale.childOf(ROMANIAN) && !isUnicodeSet) {
                value = standardizeRomanian(value);
            } else if (locale.childOf(CATALAN) && !isUnicodeSet) {
                value = standardizeCatalan(value);
            } else if (locale.childOf(NGOMBA) && !isUnicodeSet) {
                value = standardizeNgomba(value);
            } else if (locale.childOf(KWASIO) && !isUnicodeSet) {
                value = standardizeKwasio(value);
            } else if (locale.childOf(HEBREW) && !APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
                value = standardizeHebrew(value);
            } else if ((locale.childOf(SWISS_GERMAN) || locale.childOf(GERMAN_SWITZERLAND)) && !isUnicodeSet) {
                value = standardizeSwissGerman(value);
            } else if (locale.childOf(MYANMAR) && !isUnicodeSet) {
                value = MyanmarZawgyiConverter.standardizeMyanmar(value);
            }

            if (UNICODE_WHITESPACE.containsSome(value)) {
                value = normalizeWhitespace(path, value);
            }

            // all of our values should not have leading or trailing spaces, except insertBetween
            if (!path.contains("/insertBetween") && !isUnicodeSet) {
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
                try {
                    value = dtc.getCanonicalDatePattern(path, value, datetimePatternType);
                } catch (IllegalArgumentException ex) {
                    return value;
                }
            }

            if (path.startsWith("//ldml/numbers/currencies/currency") && path.contains("displayName")) {
                value = normalizeCurrencyDisplayName(value);
            }
            NumericType numericType = NumericType.getNumericType(path);
            if (numericType != NumericType.NOT_NUMERIC) {
                if (numericType == NumericType.CURRENCY) {
                    value = value.replaceAll(" ", "\u00A0");
                    if (numericType == NumericType.CURRENCY_ABBREVIATED) {
                        value = value.replaceAll("0\\.0+", "0");
                    }
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

            // fix [,]
            if (path.startsWith("//ldml/localeDisplayNames/languages/language")
                || path.startsWith("//ldml/localeDisplayNames/scripts/script")
                || path.startsWith("//ldml/localeDisplayNames/territories/territory")
                || path.startsWith("//ldml/localeDisplayNames/variants/variant")
                || path.startsWith("//ldml/localeDisplayNames/keys/key")
                || path.startsWith("//ldml/localeDisplayNames/types/type")) {
                value = value.replace('[', '(').replace(']', ')').replace('［', '（').replace('］', '）');
            }

            // Normalize two single quotes for the inches symbol.
            if (path.contains("/units")) {
                value = value.replace("''", "″");
            }

            // check specific cases
            if (isUnicodeSet) {
                value = inputUnicodeSet(path, value);
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

            // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
            if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
                value = normalizeApostrophes(value);
            }
            // Fix up any apostrophes in number symbols
            if (NUMBER_SEPARATOR_PATTERN.matcher(path).matches()) {
                value = value.replace('\'', '\u2019');
            }
            // Fix up hyphens, replacing with N-dash as appropriate
            if (INTERVAL_FORMAT_PATHS.matcher(path).matches()) {
                value = normalizeIntervalHyphens(value);
            } else if (!isUnicodeSet) {
                value = normalizeHyphens(value);
            }

            if (path.startsWith("//ldml/annotations/annotation")) {
                if (path.contains(Emoji.TYPE_TTS)) {
                    // The row has something like "🦓 -name" in the first column. Cf. namePath, getNamePaths.
                    // Normally the value is like "zebra" or "unicorn face", without "|".
                    // If the user enters a value with "|",  discard anything after "|"; e.g., change "a | b | c" to "a".
                    value = SPLIT_BAR.split(value).iterator().next();
                } else {
                    // The row has something like "🦓 –keywords" in the first column. Cf. keywordPath, getKeywordPaths.
                    // Normally the value is like "stripe | zebra", with "|".
                    value = annotationsForDisplay(value);
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

    private static final boolean REMOVE_COVERED_KEYWORDS = true;

    /**
     * Produce a modification of the given annotation by sorting its components and filtering covered keywords.
     * 
     * Examples: Given "b | a", return "a | b". Given "bear | panda | panda bear", return "bear | panda".
     *
     * @param value the string
     * @return the possibly modified string
     */
    private static String annotationsForDisplay(String value) {
        TreeSet<String> sorted = new TreeSet<>(Collator.getInstance(ULocale.ROOT));
        sorted.addAll(SPLIT_BAR.splitToList(value));
        if (REMOVE_COVERED_KEYWORDS) {
            filterCoveredKeywords(sorted);
        }
        value = JOIN_BAR.join(sorted);
        return value;
    }

    /**
     * Filter from the given set some keywords that include spaces, if they duplicate,
     * or are "covered by", other keywords in the set.
     * 
     * For example, if the set is {"bear", "panda", "panda bear"} (annotation was "bear | panda | panda bear"),
     * then remove "panda bear", treating it as "covered" since the set already includes "panda" and "bear".
     *
     * @param sorted the set from which items may be removed
     */
    public static void filterCoveredKeywords(TreeSet<String> sorted) {
        // for now, just do single items
        HashSet<String> toRemove = new HashSet<>();

        for (String item : sorted) {
            List<String> list = SPLIT_SPACE.splitToList(item);
            if (list.size() < 2) {
                continue;
            }
            if (sorted.containsAll(list)) {
                toRemove.add(item);
            }
        }
        sorted.removeAll(toRemove);
    }

    private String displayUnicodeSet(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }

        value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
        value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

        // if (RTL.containsSome(value) && value.startsWith("[") && value.endsWith("]")) {
        // return "\u200E[\u200E" + value.substring(1,value.length()-2) + "\u200E]\u200E";
        // }
        return value;
    }

    private String inputUnicodeSet(String path, String value) {
        // clean up the user's input.
        // first, fix up the '['
        value = value.trim();

        // remove brackets and trim again before regex
        if (value.startsWith("[")) {
            value = value.substring(1);
        }
        if (value.endsWith("]") && (!value.endsWith("\\]") || value.endsWith("\\\\]"))) {
            value = value.substring(0, value.length() - 1);
        }
        value = value.trim();

        value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
        value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

        // re-add brackets.
        value = "[" + value + "]";

        UnicodeSet exemplar = new UnicodeSet(value);
        XPathParts parts = XPathParts.getFrozenInstance(path); // new XPathParts().set(path);
        if (parts.getElement(2).equals("parseLenients")) {
            return exemplar.toPattern(false);
        }
        final String type = parts.getAttributeValue(-1, "type");
        ExemplarType exemplarType = type == null ? ExemplarType.main : ExemplarType.valueOf(type);
        value = getCleanedUnicodeSet(exemplar, pp, exemplarType);
        return value;
    }

    private String normalizeWhitespace(String path, String value) {
        // turn all whitespace sequences (including tab and newline, and NBSP for certain paths)
        // into a single space or a single NBSP depending on path.
        if ((path.contains("/dateFormatLength") && path.contains("/pattern")) ||
            path.contains("/availableFormats/dateFormatItem") ||
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
        return value;
    }

    private String normalizeCurrencyDisplayName(String value) {
        StringBuilder result = new StringBuilder();
        boolean inParentheses = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                inParentheses = true;
            } else if (c == ')') {
                inParentheses = false;
            }
            if (inParentheses && c == '-' && Character.isDigit(value.charAt(i - 1))) {
                c = 0x2013; /* Replace hyphen-minus with dash for date ranges */
            }
            result.append(c);
        }
        return result.toString();
    }

    private String normalizeApostrophes(String value) {
        // If our DAIP always had a CLDRFile to work with, then we could just check the exemplar set in it to see.
        // But since we don't, we just maintain the list internally and use it.
        if (LANGUAGES_USING_MODIFIER_APOSTROPHE.contains(locale.getLanguage())) {
            return value.replace('\'', '\u02bc');
        } else {
            char prev = 0;
            StringBuilder builder = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (c == '\'') {
                    if (Character.isLetter(prev)) {
                        builder.append('\u2019');
                    } else {
                        builder.append('\u2018');
                    }
                } else {
                    builder.append(c);
                }
                prev = c;
            }
            return builder.toString();
        }
    }

    private String normalizeIntervalHyphens(String value) {
        DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
        fp.set(DateIntervalInfo.genPatternInfo(value, false).getFirstPart());
        List<Object> items = fp.getItems();
        Object last = items.get(items.size() - 1);
        if (last instanceof String) {
            String separator = last.toString();
            if (separator.contains("-")) {
                StringBuilder sb = new StringBuilder();
                sb.append(DateIntervalInfo.genPatternInfo(value, false).getFirstPart());
                if (sb.lastIndexOf(separator) >= 0) {
                    sb.delete(sb.lastIndexOf(separator), sb.length());
                    sb.append(separator.replace("-", "\u2013"));
                    sb.append(DateIntervalInfo.genPatternInfo(value, false).getSecondPart());
                    return sb.toString();
                }
            }
        }
        return value;
    }

    private String normalizeHyphens(String value) {
        int hyphenLocation = value.indexOf("-");
        if (hyphenLocation > 0 &&
            Character.isDigit(value.charAt(hyphenLocation - 1)) &&
            hyphenLocation < value.length() - 1 &&
            Character.isDigit(value.charAt(hyphenLocation + 1))) {
            StringBuilder sb = new StringBuilder();
            sb.append(value.substring(0, hyphenLocation));
            sb.append("\u2013");
            sb.append(value.substring(hyphenLocation + 1));
            return sb.toString();
        }
        return value;
    }

    private String standardizeRomanian(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : ROMANIAN_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeKwasio(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : KWASIO_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeNgomba(String value) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = value.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            boolean convertedSaltillo = false;
            for (char[] pair : NGOMBA_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    if (c == '\uA78C') {
                        convertedSaltillo = true;
                    }
                    break;
                }
            }
            if (convertedSaltillo &&
                ((i > 0 && i < charArray.length - 1 && Character.isUpperCase(charArray[i - 1]) && Character.isUpperCase(charArray[i + 1])) ||
                    (i > 1 && Character.isUpperCase(charArray[i - 1]) && Character.isUpperCase(charArray[i - 2])))) {
                c = '\uA78B'; // UPPER CASE SALTILLO
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeHebrew(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : HEBREW_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeSwissGerman(String value) {
        return value.replaceAll("\u00DF", "ss");
    }

    private String standardizeCatalan(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            boolean didSubstitute = false;
            for (char[] triple : CATALAN_CONVERSIONS) {
                if (c == triple[0]) {
                    builder.append(triple[1]);
                    builder.append(triple[2]);
                    didSubstitute = true;
                    break;
                }
            }
            if (!didSubstitute) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String replace(Pattern pattern, String value, String replacement) {
        String value2 = pattern.matcher(value).replaceAll(replacement);
        if (DEBUG_DAIP && !value.equals(value2)) {
            System.out.println("\n" + value + " => " + value2);
        }
        return value2;
    }

    private static Pattern UNNORMALIZED_MALAYALAM = PatternCache.get(
        "(\u0D23|\u0D28|\u0D30|\u0D32|\u0D33|\u0D15)\u0D4D\u200D");

    private static Map<Character, Character> NORMALIZING_MAP = Builder.with(new HashMap<Character, Character>())
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

    static final Transform<String, String> fixArabicPresentation = Transliterator.getInstance(
        "[[:block=Arabic_Presentation_Forms_A:][:block=Arabic_Presentation_Forms_B:]] nfkc");

    /**
     * Normalizes the Arabic presentation forms characters in the specified input.
     *
     * @param value
     *            the input to be normalized
     * @return
     */
    private String replaceArabicPresentationForms(String value) {
        value = fixArabicPresentation.transform(value);
        return value;
    }

    static Pattern REMOVE_QUOTE1 = PatternCache.get("(\\s)(\\\\[-\\}\\]\\&])()");
    static Pattern REMOVE_QUOTE2 = PatternCache.get("(\\\\[\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    static Pattern NEEDS_QUOTE1 = PatternCache.get("(\\s|$)([-\\}\\]\\&])()");
    static Pattern NEEDS_QUOTE2 = PatternCache.get("([^\\\\])([\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    public static String getCleanedUnicodeSet(UnicodeSet exemplar, UnicodeSetPrettyPrinter prettyPrinter,
        ExemplarType exemplarType) {
        if (prettyPrinter == null) {
            return exemplar.toPattern(false);
        }
        String value;
        prettyPrinter.setCompressRanges(exemplar.size() > 300);
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
    static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();

    public static String getCanonicalPattern(String inpattern, NumericType type, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;

        DecimalFormat df = new DecimalFormat(inpattern);
        if (type == NumericType.DECIMAL_ABBREVIATED || type == NumericType.CURRENCY_ABBREVIATED
            || CldrUtility.INHERITANCE_MARKER.equals(inpattern)) {
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
        List<String> parts = SEMI_SPLITTER.splitToList(pattern);
        String pattern2 = parts.get(0);
        if (parts.size() > 1) {
            pattern2 += ";" + parts.get(1);
        }
        if (!pattern2.equals(pattern)) {
            pattern = pattern2;
        }
        // int pos = pattern.indexOf(';');
        // if (pos < 0) return pattern + ";-" + pattern;
        return pattern;
    }

    /*
     * This tests what type a numeric pattern is.
     */
    public enum NumericType {
        CURRENCY(new int[] { 1, 2, 2 }, new int[] { 1, 2, 2 }), CURRENCY_ABBREVIATED(), DECIMAL(new int[] { 1, 0, 3 },
            new int[] { 1, 0, 6 }), DECIMAL_ABBREVIATED(), PERCENT(new int[] { 1, 0, 0 },
                new int[] { 1, 0, 0 }), SCIENTIFIC(new int[] { 0, 0, 0 }, new int[] { 1, 6, 6 }), NOT_NUMERIC;

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
                    if (xpath.contains("=\"1000")) {
                        if (type == DECIMAL) {
                            type = DECIMAL_ABBREVIATED;
                        } else if (type == CURRENCY) {
                            type = CURRENCY_ABBREVIATED;
                        } else {
                            throw new IllegalArgumentException("Internal Error");
                        }
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
