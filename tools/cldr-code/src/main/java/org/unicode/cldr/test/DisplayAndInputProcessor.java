/* Copyright (C) 2007-2013 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2013 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.TreeMultimap;
import com.google.myanmartools.ZawgyiDetector;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ComparatorUtilities;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

/**
 * Class for processing the input and output of CLDR data for use in the Survey Tool and other
 * tools.
 */
public class DisplayAndInputProcessor {

    /** Special PersonName paths that allow empty string, public for testing */
    public static final String NOL_START_PATH = "//ldml/personNames/nameOrderLocales";

    public static final String FSR_START_PATH = "//ldml/personNames/foreignSpaceReplacement";
    public static final String NSR_START_PATH = "//ldml/personNames/nativeSpaceReplacement";

    public static final String EMPTY_ELEMENT_VALUE = "❮EMPTY❯";

    private static final boolean FIX_YEARS = true;

    public static final boolean DEBUG_DAIP = CldrUtility.getProperty("DEBUG_DAIP", false);

    public static final UnicodeSet RTL =
            new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]").freeze();

    public static final Pattern NUMBER_SEPARATOR_PATTERN =
            Pattern.compile("//ldml/numbers/symbols.*/(decimal|group)");

    private static final Pattern APOSTROPHE_SKIP_PATHS =
            PatternCache.get(
                    "//ldml/("
                            + "localeDisplayNames/languages/language\\[@type=\"mic\"].*|"
                            + "characters/.*|"
                            + "delimiters/.*|"
                            + "dates/.+/(pattern|intervalFormatItem|dateFormatItem).*|"
                            + "units/.+/unitPattern.*|"
                            + "units/.+/durationUnitPattern.*|"
                            + "numbers/symbols.*|"
                            + "numbers/miscPatterns.*|"
                            + "numbers/(decimal|currency|percent|scientific)Formats.+/(decimal|currency|percent|scientific)Format.*)");
    private static final Pattern INTERVAL_FORMAT_PATHS =
            PatternCache.get("//ldml/dates/.+/intervalFormat(Item.*|Fallback)");
    private static final Pattern NON_DECIMAL_PERIOD = PatternCache.get("(?<![0#'])\\.(?![0#'])");

    // Pattern to match against paths that might have time formats with h or K (12-hour cycles)
    private static final Pattern HOUR_FORMAT_XPATHS =
            PatternCache.get(
                    "//ldml/dates/calendars/calendar\\[@type=\"[^\"]*\"]/("
                            + "timeFormats/timeFormatLength\\[@type=\"[^\"]*\"]/timeFormat\\[@type=\"standard\"]/pattern\\[@type=\"standard\"].*|"
                            + "dateTimeFormats/availableFormats/dateFormatItem\\[@id=\"[A-GL-Ma-gl-m]*[hK][A-Za-z]*\"].*|"
                            + "dateTimeFormats/intervalFormats/intervalFormatItem\\[@id=\"[A-GL-Ma-gl-m]*[hK][A-Za-z]*\"].*)");

    private static final Pattern AMPM_SPACE_BEFORE =
            PatternCache.get("([Khms])([ \\u00A0\\u202F]+)(a+)"); // time, space, a+
    private static final Pattern AMPM_SPACE_AFTER =
            PatternCache.get("(a+)([ \\u00A0\\u202F]+)([Kh])"); // a+, space, hour

    // Pattern to match against paths that might have date formats with y
    private static final Pattern YEAR_FORMAT_XPATHS =
            PatternCache.get(
                    "//ldml/dates/calendars/calendar\\[@type=\"[^\"]*\"]/("
                            + "dateFormats/dateFormatLength\\[@type=\"[^\"]*\"]/dateFormat\\[@type=\"standard\"]/pattern\\[@type=\"standard\"].*|"
                            + "dateTimeFormats/availableFormats/dateFormatItem\\[@id=\"[A-XZa-xz]*y[A-Za-z]*\"].*|"
                            + "dateTimeFormats/intervalFormats/intervalFormatItem\\[@id=\"[A-XZa-xz]*y[A-Za-z]*\"].*)");

    // Cyrillic year markers are or begin with (in various languages) \u0430 \u0433 \u0435 \u0436
    // \u043E \u0440 \u0441
    private static final Pattern YEAR_SPACE_YEARMARKER =
            PatternCache.get("y[ \\u00A0]+('?[агежорс])"); // y, space, Cyrillic year marker start

    public static final Pattern UNIT_NARROW_XPATHS =
            PatternCache.get(
                    "//ldml/units/unitLength\\[@type=\"narrow\"]unit\\[@type=\"[^\"]*\"]/unitPattern.*");

    public static final Pattern UNIT_SHORT_XPATHS =
            PatternCache.get(
                    "//ldml/units/unitLength\\[@type=\"short\"]unit\\[@type=\"[^\"]*\"]/unitPattern.*");

    private static final Pattern PLACEHOLDER_SPACE_AFTER =
            PatternCache.get("\\}[ \\u00A0\\u202F]+");
    private static final Pattern PLACEHOLDER_SPACE_BEFORE =
            PatternCache.get("[ \\u00A0\\u202F]+\\{");
    private static final Pattern INTERVAL_FALLBACK_RANGE = PatternCache.get("\\} [\\u2013-] \\{");

    /** string of whitespace not including NBSP, i.e. [\t\n\r]+ */
    private static final Pattern WHITESPACE_NO_NBSP_TO_NORMALIZE = PatternCache.get("\\s+"); //

    /** string of whitespace, possibly including NBSP and/or NNBSP, ie., [\u00A0\t\n\r\u202F]+ */
    private static final Pattern WHITESPACE_AND_NBSP_TO_NORMALIZE =
            PatternCache.get("[\\s\\u00A0]+");

    // Reverted 2022-12-08 from:
    // private static final Pattern WHITESPACE_AND_NBSP_TO_NORMALIZE =
    // PatternCache.get("[\\s\\u00A0\\u202F]+");

    /** one or more NBSP (or NNBSP) followed by one or more regular spaces */
    private static final Pattern NBSP_PLUS_SPACE_TO_NORMALIZE =
            PatternCache.get("\\u00A0+\\u0020+");

    // Reverted 2022-12-08 from:
    // private static final Pattern NBSP_PLUS_SPACE_TO_NORMALIZE =
    // PatternCache.get("[\\u00A0\\u202F]+\\u0020+");

    /** one or more regular spaces followed by one or more NBSP (or NNBSP) */
    private static final Pattern SPACE_PLUS_NBSP_TO_NORMALIZE =
            PatternCache.get("\\u0020+\\u00A0+");

    // Reverted 2022-12-08 from:
    // private static final Pattern SPACE_PLUS_NBSP_TO_NORMALIZE =
    // PatternCache.get("\\u0020+[\\u00A0\\u202F]+");

    // NNBSP 202F among other horizontal spaces (includes 0020, 00A0, 2009, 202F, etc.)
    private static final Pattern NNBSP_AMONG_OTHER_SPACES =
            PatternCache.get("[\\h&&[^\\u202F]]+\\u202F\\h*|\\u202F\\h+");
    // NBSP 00A0 among other horizontal spaces
    private static final Pattern NBSP_AMONG_OTHER_SPACES =
            PatternCache.get("[\\h&&[^\\u00A0]]+\\u00A0\\h*|\\u00A0\\h+");
    // THIN SPACE 2009 among other horizontal spaces
    private static final Pattern THIN_SPACE_AMONG_OTHER_SPACES =
            PatternCache.get("[\\h&&[^\\u2009]]+\\u2009\\h*|\\u2009\\h+");

    private static final Pattern INITIAL_NBSP = PatternCache.get("^[\\u00A0\\u202F]+");
    private static final Pattern FINAL_NBSP = PatternCache.get("[\\u00A0\\u202F]+$");

    private static final Pattern MULTIPLE_NBSP = PatternCache.get("\\u00A0\\u00A0+");
    // Reverted 2022-12-08 from:
    // private static final Pattern MULTIPLE_NBSP =
    // PatternCache.get("[\\u00A0\\u202F][\\u00A0\\u202F]+");

    // The following includes (among others) \u0009, \u0020, \u00A0, \u2007, \u2009, \u202F, \u3000
    private static final UnicodeSet UNICODE_WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();

    private static final CLDRLocale MALAYALAM = CLDRLocale.getInstance("ml");
    private static final CLDRLocale ROMANIAN = CLDRLocale.getInstance("ro");
    private static final CLDRLocale CATALAN = CLDRLocale.getInstance("ca");
    private static final CLDRLocale NGOMBA = CLDRLocale.getInstance("jgo");
    private static final CLDRLocale KWASIO = CLDRLocale.getInstance("nmg");
    private static final CLDRLocale HEBREW = CLDRLocale.getInstance("he");
    private static final CLDRLocale MYANMAR = CLDRLocale.getInstance("my");
    private static final CLDRLocale KYRGYZ = CLDRLocale.getInstance("ky");
    private static final CLDRLocale URDU = CLDRLocale.getInstance("ur");
    private static final CLDRLocale PASHTO = CLDRLocale.getInstance("ps");
    private static final CLDRLocale FARSI = CLDRLocale.getInstance("fa");
    private static final CLDRLocale GERMAN_SWITZERLAND = CLDRLocale.getInstance("de_CH");
    private static final CLDRLocale SWISS_GERMAN = CLDRLocale.getInstance("gsw");
    private static final CLDRLocale FF_ADLAM = CLDRLocale.getInstance("ff_Adlm");
    private static final CLDRLocale KASHMIRI = CLDRLocale.getInstance("ks");
    public static final Set<String> LANGUAGES_USING_MODIFIER_APOSTROPHE =
            new HashSet<>(
                    Arrays.asList(
                            "br", "bss", "cad", "cic", "cch", "gn", "ha", "ha_Latn", "kek", "lkt",
                            "mgo", "mic", "moh", "mus", "nnh", "qu", "quc", "uk", "uz", "uz_Latn"));

    // Ş ş Ţ ţ  =>  Ș ș Ț ț
    private static final char[][] ROMANIAN_CONVERSIONS = {
        {'\u015E', '\u0218'}, {'\u015F', '\u0219'}, {'\u0162', '\u021A'}, {'\u0163', '\u021B'}
    };

    private static final char[][] CATALAN_CONVERSIONS = {
        {'\u013F', '\u004C', '\u00B7'}, // Ŀ -> L·
        {'\u0140', '\u006C', '\u00B7'}
    }; // ŀ -> l·

    private static final char[][] NGOMBA_CONVERSIONS = {
        {'\u0251', '\u0061'}, {'\u0261', '\u0067'}, //  ɑ -> a , ɡ -> g , See ticket #5691
        {'\u2019', '\uA78C'}, {'\u02BC', '\uA78C'}
    }; //  Saltillo, see ticket #6805

    private static final char[][] KWASIO_CONVERSIONS = {
        {'\u0306', '\u030C'}, // See ticket #6571, use caron instead of breve
        {'\u0103', '\u01CE'},
        {'\u0102', '\u01CD'}, // a-breve -> a-caron
        {'\u0115', '\u011B'},
        {'\u011A', '\u01CD'}, // e-breve -> e-caron
        {'\u012D', '\u01D0'},
        {'\u012C', '\u01CF'}, // i-breve -> i-caron
        {'\u014F', '\u01D2'},
        {'\u014E', '\u01D1'}, // o-breve -> o-caron
        {'\u016D', '\u01D4'},
        {'\u016C', '\u01D3'} // u-breve -> u-caron
    };

    private static final char[][] HEBREW_CONVERSIONS = {
        {'\'', '\u05F3'}, {'"', '\u05F4'}
    }; //  ' -> geresh  " -> gershayim

    private static final char[][] KYRGYZ_CONVERSIONS = {{'ӊ', 'ң'}, {'Ӊ', 'Ң'}}; //  right modifier

    private static final char[][] URDU_PLUS_CONVERSIONS = {{'\u0643', '\u06A9'}}; //  wrong char

    private static final char[][] KASHMIRI_CONVERSIONS = {
        {'ۍ', 'ؠ'}
    }; //  wrong char (see CLDR-16595)

    private static final ZawgyiDetector detector = new ZawgyiDetector();
    private static final Transliterator zawgyiUnicodeTransliterator =
            Transliterator.getInstance("Zawgyi-my");

    private SimpleUnicodeSetFormatter pp = new SimpleUnicodeSetFormatter(); // default collator
    private UnicodeSetPrettyPrinter rawFormatter = new UnicodeSetPrettyPrinter(); // default

    private final CLDRLocale locale;
    private String scriptCode; // actual or default script code (not null after init)
    private boolean isPosix;

    private CLDRFile cldrFileForBailey = null;

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
        isPosix = locale.toString().contains("POSIX");
        if (needsCollator) {
            Collator col =
                    ComparatorUtilities.getCldrCollator(locale.toString(), Collator.IDENTICAL);
            Collator spaceCol =
                    ComparatorUtilities.getCldrCollator(locale.toString(), Collator.PRIMARY);
            pp = new SimpleUnicodeSetFormatter((Comparator) col);
            rawFormatter = UnicodeSetPrettyPrinter.from((Comparator) col, (Comparator) spaceCol);
        } else {
            pp = new SimpleUnicodeSetFormatter(); // default collator
            rawFormatter = new UnicodeSetPrettyPrinter(); // default
        }
        String script = locale.getScript();
        if (script == null || script.length() < 4) {
            SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
            script = sdi.getDefaultScript(locale.getBaseName());
            if (script == null || script.length() < 4 || script.equals("Zzzz")) {
                script = sdi.getDefaultScript(locale.getLanguage());
            }
            if (script == null || script.length() < 4) {
                script = "Zzzz";
            }
        }
        scriptCode = script;
    }

    public SimpleUnicodeSetFormatter getPrettyPrinter() {
        return pp;
    }

    /**
     * Constructor, taking ULocale and boolean.
     *
     * @param locale the ULocale
     * @param needsCollator true or false
     *     <p>Called by getProcessor, with locale = SurveyMain.TRANS_HINT_LOCALE
     */
    public DisplayAndInputProcessor(ULocale locale, boolean needsCollator) {
        init(this.locale = CLDRLocale.getInstance(locale), needsCollator);
    }

    /**
     * Constructor, taking ULocale.
     *
     * @param locale the ULocale
     */
    public DisplayAndInputProcessor(ULocale locale) {
        init(this.locale = CLDRLocale.getInstance(locale), true /* needsCollator */);
    }

    /**
     * Constructor, taking CLDRLocale and boolean.
     *
     * @param locale the CLDRLocale
     * @param needsCollator true or false
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
     * Process the value for display. The result is a string for display in the Survey tool or
     * similar program.
     *
     * @param path
     * @param value
     * @return
     */
    public synchronized String processForDisplay(String path, String value) {
        if (value == null) {
            return null;
        }
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            return value;
        }
        value = Normalizer.compose(value, false); // Always normalize all text to NFC.
        if (hasUnicodeSetValue(path)) {
            return displayUnicodeSet(value);
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
                if (numericType != NumericType.CURRENCY
                        && numericType != NumericType.CURRENCY_ABBREVIATED) {
                    value = value.replace("'", "");
                }
            }
        }
        // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
        if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
            value = normalizeApostrophes(value);
        }
        // Fix up hyphens, replacing with N-dash as appropriate
        if (INTERVAL_FORMAT_PATHS.matcher(path).matches()) {
            value =
                    normalizeIntervalHyphensAndSpaces(
                            value); // This may also adjust spaces around en dash
        } else {
            value = normalizeHyphens(value);
        }
        // Fix up possibly empty field
        if (value.isEmpty()
                && (path.startsWith(FSR_START_PATH)
                        || path.startsWith(NSR_START_PATH)
                        || path.startsWith(NOL_START_PATH))) {
            value = EMPTY_ELEMENT_VALUE;
        }
        return value;
    }

    public static boolean hasUnicodeSetValue(String path) {
        return path.startsWith("//ldml/characters/exemplarCharacters")
                || path.startsWith("//ldml/characters/parseLenients");
    }

    static final DateTimeCanonicalizer dtc = new DateTimeCanonicalizer(FIX_YEARS);

    private static final String BAR_VL = "\\|"; // U+007C VERTICAL LINE (pipe, bar) literal
    private static final String BAR_EL = "\\s+l\\s+"; // U+006C LATIN SMALL LETTER L with space
    private static final String BAR_REGEX = "(" + BAR_EL + "|[︳︱।|｜⎸⎹⏐￨❘])";
    public static final Splitter SPLIT_BAR =
            Splitter.on(Pattern.compile(BAR_REGEX)).trimResults().omitEmptyStrings();
    static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Joiner JOIN_BAR = Joiner.on(" | ");
    static final Joiner JOIN_SPACE = Joiner.on(' ');

    /**
     * Process the value for input. The result is a cleaned-up value. For example, an exemplar set
     * is modified to be in the normal format, and any missing [ ] are added (a common omission on
     * entry). If there are any failures then the original value is returned, so that the proper
     * error message can be given.
     *
     * @param path
     * @param value
     * @param internalException to be filled in if RuntimeException occurs
     * @return the possibly modified value
     */
    public synchronized String processInput(
            String path, String value, Exception[] internalException) {
        // skip processing for inheritance marker
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            return value;
        }
        final String original = value;
        value = stripProblematicControlCharacters(value);
        value = Normalizer.compose(value, false); // Always normalize all input to NFC.
        value = value.replace('\u00B5', '\u03BC'); // use the right Greek mu character
        if (internalException != null) {
            internalException[0] = null;
        }
        // for root annotations
        if (CLDRLocale.ROOT.equals(locale) && path.contains("/annotations")) {
            return value;
        }
        try {
            value = processInputMore(path, value);
        } catch (RuntimeException e) {
            if (internalException != null) {
                internalException[0] = e;
            }
            return original;
        }
        return value;
    }

    private String processInputMore(String path, String value) {
        final boolean isUnicodeSet = hasUnicodeSetValue(path);
        if (isUnicodeSet) {
            return inputUnicodeSet(path, value);
        }

        value = processLocaleSpecificInput(path, value, isUnicodeSet);

        if (UNICODE_WHITESPACE.containsSome(value)) {
            value = normalizeWhitespace(path, value);
        }

        // remove the empty value (mostly relevant for person names,
        // but prevents it showing up elsewhere by mistake
        value = value.replace(EMPTY_ELEMENT_VALUE, "");

        // all of our values should not have leading or trailing spaces, except insertBetween,
        // foreignSpaceReplacement, and anything with built-in attribute xml:space="preserve"
        if (!path.contains("/insertBetween")
                && !path.contains("/foreignSpaceReplacement")
                && !path.contains("/nativeSpaceReplacement")
                && !path.contains("[@xml:space=\"preserve\"]")
                && !isUnicodeSet) {
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
                // NOTE: the following "if ... NumericType.CURRENCY_ABBREVIATED" was false here,
                // since we know it is NumericType.CURRENCY; so now the code is commented out; if
                // anyone
                // understands what the intention was, maybe the condition should be restored
                // somehow,
                // such as with "else if"
                // if (numericType == NumericType.CURRENCY_ABBREVIATED) {
                //    value = value.replaceAll("0\\.0+", "0");
                // }
            } else {
                value =
                        value.replaceAll("([%\u00A4]) ", "$1\u00A0")
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
        //        if (isUnicodeSet) {
        //            value = inputUnicodeSet(path, value);
        //        } else
        if (path.contains("stopword")) {
            if (value.equals("NONE")) {
                value = "";
            }
        }

        // Normalize ellipsis data.
        if (path.startsWith("//ldml/characters/ellipsis")) {
            value = value.replace("...", "…");
        }

        if (path.startsWith(NOL_START_PATH)) {
            value = normalizeNameOrderLocales(value);
        }

        // Replace Arabic presentation forms with their nominal counterparts
        value = replaceArabicPresentationForms(value);

        // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
        if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
            value = normalizeApostrophes(value);
        }
        // Fix up hyphens, replacing with N-dash as appropriate
        if (INTERVAL_FORMAT_PATHS.matcher(path).matches()) {
            value =
                    normalizeIntervalHyphensAndSpaces(
                            value); // This may also adjust spaces around en dash
        } else if (!isUnicodeSet) {
            value = normalizeHyphens(value);
        }
        value = processAnnotations(path, value);
        value = normalizeZeroWidthSpace(value);
        if (VoteResolver.DROP_HARD_INHERITANCE) {
            value = replaceBaileyWithInheritanceMarker(path, value);
        }
        return value;
    }

    private String processLocaleSpecificInput(String path, String value, boolean isUnicodeSet) {
        if (locale.childOf(MALAYALAM)) {
            String newvalue = normalizeMalayalam(value);
            if (DEBUG_DAIP)
                System.out.println(
                        "DAIP: Normalized Malayalam '" + value + "' to '" + newvalue + "'");
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
            value = replaceChars(path, value, HEBREW_CONVERSIONS, false);
        } else if ((locale.childOf(SWISS_GERMAN) || locale.childOf(GERMAN_SWITZERLAND))
                && !isUnicodeSet) {
            value = standardizeSwissGerman(value);
        } else if (locale.childOf(MYANMAR) && !isUnicodeSet) {
            value = standardizeMyanmar(value);
        } else if (locale.childOf(KYRGYZ)) {
            value = replaceChars(path, value, KYRGYZ_CONVERSIONS, false);
        } else if (locale.childOf(URDU) || locale.childOf(PASHTO) || locale.childOf(FARSI)) {
            value = replaceChars(path, value, URDU_PLUS_CONVERSIONS, true);
        } else if (locale.childOf(FF_ADLAM) && !isUnicodeSet) {
            value = fixAdlamNasalization(value);
        } else if (locale.childOf(KASHMIRI)) {
            value = replaceChars(path, value, KASHMIRI_CONVERSIONS, false);
        }
        return value;
    }

    private String processAnnotations(String path, String value) {
        if (AnnotationUtil.pathIsAnnotation(path)) {
            if (path.contains(Emoji.TYPE_TTS)) {
                // The row has something like "🦓 -name" in the first column. Cf. namePath,
                // getNamePaths.
                // Normally the value is like "zebra" or "unicorn face", without "|".
                // If the user enters a value with "|",  discard anything after "|"; e.g., change "a
                // | b | c" to "a".
                value = SPLIT_BAR.split(value).iterator().next();
            } else {
                // The row has something like "🦓 –keywords" in the first column. Cf. keywordPath,
                // getKeywordPaths.
                // Normally the value is like "stripe | zebra", with "|".
                value = annotationsForDisplay(value);
            }
        }
        return value;
    }

    private String normalizeNameOrderLocales(String value) {
        value = value.replace(EMPTY_ELEMENT_VALUE, "");
        TreeSet<String> result = new TreeSet<>(SPLIT_SPACE.splitToList(value));
        result.remove(LocaleNames.ZXX);
        if (result.remove(LocaleNames.UND)) { // put und at the front
            if (result.isEmpty()) {
                return LocaleNames.UND;
            } else {
                return LocaleNames.UND + " " + JOIN_SPACE.join(result);
            }
        }
        return JOIN_SPACE.join(result);
    }

    /**
     * Strip out all code points less than U+0020 except for U+0009 tab, U+000A line feed, and
     * U+000D carriage return.
     *
     * @param s the string
     * @return the resulting string
     */
    private String stripProblematicControlCharacters(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.codePoints()
                .filter(c -> (c >= 0x20 || c == 9 || c == 0xA || c == 0xD))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static final boolean REMOVE_COVERED_KEYWORDS = true;

    /**
     * Produce a modification of the given annotation by sorting its components and filtering
     * covered keywords.
     *
     * <p>Examples: Given "b | a", return "a | b". Given "bear | panda | panda bear", return "bear |
     * panda".
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
     * Filter from the given set some keywords that include spaces, if they duplicate, or are
     * "covered by", other keywords in the set.
     *
     * <p>For example, if the set is {"bear", "panda", "panda bear"} (annotation was "bear | panda |
     * panda bear"), then remove "panda bear", treating it as "covered" since the set already
     * includes "panda" and "bear". Also, for example, if the set is {"bear", "panda", "PANDA
     * BEAR"}, then remove "PANDA BEAR" even though the casing differs.
     *
     * <p>Since casing is complex in many languages/scripts, this method does not attempt to
     * recognize all occurrences of case-insensitive matching. Instead, it first checks for
     * case-sensitive (exact) matching, then it checks for case-insensitive (loose) matching
     * according to Locale.ROOT. The intended effect is only to remove an item like "PANDA BEAR" if
     * both "panda" and "bear" are already present as individual items. The intended effect is never
     * to modify the casing of any item that is already present.
     *
     * @param sorted the set from which items may be removed
     */
    public static void filterCoveredKeywords(TreeSet<String> sorted) {
        // for now, just do single items
        HashSet<String> toRemove = new HashSet<>();

        TreeSet<String> sortedLower = new TreeSet<>();
        for (String item : sorted) {
            sortedLower.add(item.toLowerCase(Locale.ROOT));
        }
        for (String item : sorted) {
            List<String> list = SPLIT_SPACE.splitToList(item);
            if (list.size() < 2) {
                continue;
            }
            if (sorted.containsAll(list)) {
                toRemove.add(item);
            } else {
                List<String> listLower = new ArrayList<>();
                for (String s : list) {
                    listLower.add(s.toLowerCase(Locale.ROOT));
                }
                if (sortedLower.containsAll(listLower)) {
                    toRemove.add(item);
                }
            }
        }
        sorted.removeAll(toRemove);
    }

    /**
     * Given a sorted list like "BEAR | Bear ｜ PANDA | Panda | panda"，filter out any items that
     * duplicate other items aside from case, leaving only, for example, "BEAR | PANDA"
     *
     * @param sorted the set from which items may be removed
     */
    public static void filterKeywordsDifferingOnlyInCase(TreeSet<String> sorted) {
        TreeMultimap<String, String> mapFromLower = TreeMultimap.create();
        for (String item : sorted) {
            mapFromLower.put(item.toLowerCase(), item);
        }
        TreeSet<String> toRetain = new TreeSet<>();
        for (String lower : mapFromLower.keySet()) {
            Set<String> variants = mapFromLower.get(lower);
            for (String var : variants) {
                toRetain.add(var);
                break;
            }
        }
        sorted.retainAll(toRetain);
    }

    private String displayUnicodeSet(String value) {
        return pp.format(
                new UnicodeSet(value)); // will throw exception if bad format, eg missing [...]
    }

    private String inputUnicodeSet(String path, String value) {
        UnicodeSet exemplar = null;
        // hack, in case the input is called twice
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            try {
                exemplar = new UnicodeSet(value);
            } catch (Exception e2) {
                // fall through
            }
        }
        if (exemplar == null) {
            try {
                exemplar = pp.parse(value);
            } catch (Exception e) {
                // can't parse at all
                return value; // we can't throw an exception because clients won't expect it.
            }
        }
        XPathParts parts = XPathParts.getFrozenInstance(path);
        //        if (parts.getElement(2).equals("parseLenients")) {
        //            return exemplar.toPattern(false);
        //        }
        final String type = parts.getAttributeValue(-1, "type");
        ExemplarType exemplarType =
                !path.contains("exemplarCharacters")
                        ? null
                        : type == null ? ExemplarType.main : ExemplarType.from(type);
        value = getCleanedUnicodeSet(exemplar, exemplarType);
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
        // If our DAIP always had a CLDRFile to work with, then we could just check the exemplar set
        // in it to see.
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

    private String normalizeIntervalHyphensAndSpaces(String value) {
        if (value.contains("{0}")) {
            // intervalFormatFallback pattern, not handled by DateTimePatternGenerator.FormatParser
            if (scriptCode.equals("Latn")) {
                value = INTERVAL_FALLBACK_RANGE.matcher(value).replaceAll("}\u2009\u2013\u2009{");
            }
            return value;
        }
        DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
        fp.set(
                DateIntervalInfo.genPatternInfo(value, false)
                        .getFirstPart()); // first format & separator including spaces
        List<Object> items = fp.getItems();
        Object last = items.get(items.size() - 1);
        if (last instanceof String) {
            String separator =
                    last.toString(); // separator including spaces, and possibly preceding
            // literal text (. or quoted)
            String replacement = separator;
            if (scriptCode.equals("Latn")
                    && (separator.endsWith(" - ") || separator.endsWith(" \u2013 "))) {
                replacement =
                        separator.substring(0, separator.length() - 3)
                                + "\u2009\u2013\u2009"; // Per CLDR-14032,16308
            } else if (separator.contains("-")) {
                replacement = separator.replace("-", "\u2013");
            }
            if (!replacement.equals(separator)) {
                StringBuilder sb = new StringBuilder();
                sb.append(DateIntervalInfo.genPatternInfo(value, false).getFirstPart());
                if (sb.lastIndexOf(separator) >= 0) {
                    sb.delete(sb.lastIndexOf(separator), sb.length());
                    sb.append(replacement);
                    sb.append(
                            DateIntervalInfo.genPatternInfo(value, false)
                                    .getSecondPart()); // second format only
                    return sb.toString();
                }
            }
        }
        return value;
    }

    private String normalizeHyphens(String value) {
        int hyphenLocation = value.indexOf("-");
        if (hyphenLocation > 0
                && Character.isDigit(value.charAt(hyphenLocation - 1))
                && hyphenLocation < value.length() - 1
                && Character.isDigit(value.charAt(hyphenLocation + 1))) {
            return value.substring(0, hyphenLocation)
                    + "\u2013"
                    + value.substring(hyphenLocation + 1);
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

    // Use the myanmar-tools detector.
    private String standardizeMyanmar(String value) {
        if (detector.getZawgyiProbability(value) > 0.90) {
            return zawgyiUnicodeTransliterator.transform(value);
        }
        return value;
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
            if (convertedSaltillo
                    && ((i > 0
                                    && i < charArray.length - 1
                                    && Character.isUpperCase(charArray[i - 1])
                                    && Character.isUpperCase(charArray[i + 1]))
                            || (i > 1
                                    && Character.isUpperCase(charArray[i - 1])
                                    && Character.isUpperCase(charArray[i - 2])))) {
                c = '\uA78B'; // UPPER CASE SALTILLO
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String replaceChars(
            String path, String value, char[][] charsToReplace, boolean skipAuxExemplars) {
        if (skipAuxExemplars && path.contains("/exemplarCharacters[@type=\"auxiliary\"]")) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : charsToReplace) {
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

    private static final Pattern UNNORMALIZED_MALAYALAM =
            PatternCache.get("(\u0D23|\u0D28|\u0D30|\u0D32|\u0D33|\u0D15)\u0D4D\u200D");

    private static final Map<Character, Character> NORMALIZING_MAP =
            Builder.with(new HashMap<Character, Character>())
                    .put('\u0D23', '\u0D7A')
                    .put('\u0D28', '\u0D7B')
                    .put('\u0D30', '\u0D7C')
                    .put('\u0D32', '\u0D7D')
                    .put('\u0D33', '\u0D7E')
                    .put('\u0D15', '\u0D7F')
                    .get();

    /**
     * Normalizes the Malayalam characters in the specified input.
     *
     * @param value the input to be normalized
     * @return
     */
    private String normalizeMalayalam(String value) {
        // Normalize Malayalam characters.
        Matcher matcher = UNNORMALIZED_MALAYALAM.matcher(value);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            int start = 0;
            do {
                buffer.append(value, start, matcher.start(0));
                char codePoint = matcher.group(1).charAt(0);
                buffer.append(NORMALIZING_MAP.get(codePoint));
                start = matcher.end(0);
            } while (matcher.find());
            buffer.append(value.substring(start));
            value = buffer.toString();
        }
        return value;
    }

    static final Transform<String, String> fixArabicPresentation =
            Transliterator.getInstance(
                    "[[:block=Arabic_Presentation_Forms_A:][:block=Arabic_Presentation_Forms_B:]] nfkc");

    /**
     * Normalizes the Arabic presentation forms characters in the specified input.
     *
     * @param value the input to be normalized
     * @return
     */
    private String replaceArabicPresentationForms(String value) {
        value = fixArabicPresentation.transform(value);
        return value;
    }

    static Pattern ADLAM_MISNASALIZED = PatternCache.get("([𞤲𞤐])['’‘]([𞤁𞤔𞤘𞤄𞤣𞤦𞤶𞤺])");
    public static String ADLAM_NASALIZATION = "𞥋"; // U+1E94B (Unicode 12.0)

    public static String fixAdlamNasalization(String fromString) {
        return ADLAM_MISNASALIZED
                .matcher(fromString)
                .replaceAll("$1" + ADLAM_NASALIZATION + "$2"); // replace quote with 𞥋
    }

    public String getCleanedUnicodeSet(UnicodeSet exemplar, ExemplarType exemplarType) {

        if (rawFormatter == null) {
            throw new IllegalArgumentException("Formatter must not be null");
        }
        if (exemplar == null) {
            throw new IllegalArgumentException("set to be cleaned must not be null");
        }

        String value;
        // prettyPrinter.setCompressRanges(exemplar.size() > 300);
        // value = exemplar.toPattern(false);
        UnicodeSet toAdd = new UnicodeSet();

        for (UnicodeSetIterator usi = new UnicodeSetIterator(exemplar); usi.next(); ) {
            String string = usi.getString();
            if (string.equals("ß") || string.equals("İ")) {
                toAdd.add(string);
                continue;
            }
            switch (string) {
                case "\u2011":
                    toAdd.add("-");
                    break; // nobreak hyphen
                case "-":
                    toAdd.add("\u2011");
                    break; // nobreak hyphen

                case " ":
                    toAdd.add("\u00a0");
                    break; // nobreak space
                case "\u00a0":
                    toAdd.add(" ");
                    break; // nobreak space

                case "\u202F":
                    toAdd.add("\u2009");
                    break; // nobreak narrow space
                case "\u2009":
                    toAdd.add("\u202F");
                    break; // nobreak narrow space
            }
            if (exemplarType != null && exemplarType.convertUppercase) {
                string = UCharacter.toLowerCase(ULocale.ENGLISH, string);
            }
            toAdd.add(string);
            // we allow
            String composed = Normalizer.compose(string, false);
            if (!string.equals(composed)) {
                toAdd.add(composed);
            }
        }

        if (exemplarType != null) {
            toAdd.removeAll(exemplarType.toRemove);
        }
        value = rawFormatter.format(toAdd);
        return value;
    }

    static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();

    /**
     * @return a canonical numeric pattern, based on the type, and the isPOSIX flag. The latter is
     *     set for en_US_POSIX.
     */
    public static String getCanonicalPattern(String inpattern, NumericType type, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;

        if (type == NumericType.RATIONAL) {
            return inpattern
                    .replace(
                            "}{",
                            "}\u202F{") // make sure there is at least a NNBSP between numbers, so
                    // we don't get 33/4 instead of 3 3/4.
                    .replace("/", "\u2044"); // use FRACTION SLASH instead of ASCII slash
        }
        DecimalFormat df = new DecimalFormat(inpattern);
        if (type == NumericType.DECIMAL_ABBREVIATED
                || type == NumericType.CURRENCY_ABBREVIATED
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

    public void enableInheritanceReplacement(CLDRFile cldrFile) {
        cldrFileForBailey = cldrFile;
    }

    /*
     * This tests what type a numeric pattern is.
     */
    public enum NumericType {
        CURRENCY(new int[] {1, 2, 2}, new int[] {1, 2, 2}),
        CURRENCY_ABBREVIATED(),
        DECIMAL(new int[] {1, 0, 3}, new int[] {1, 0, 6}),
        DECIMAL_ABBREVIATED(),
        PERCENT(new int[] {1, 0, 0}, new int[] {1, 0, 0}),
        SCIENTIFIC(new int[] {0, 0, 0}, new int[] {1, 6, 6}),
        RATIONAL,
        NOT_NUMERIC;

        private static final Pattern NUMBER_PATH =
                Pattern.compile(
                        "//ldml/numbers/((currency|decimal|percent|scientific|rational)Formats|currencies/currency).*");
        private int[] digitCount;
        private int[] posixDigitCount;

        NumericType() {}

        NumericType(int[] digitCount, int[] posixDigitCount) {
            this.digitCount = digitCount;
            this.posixDigitCount = posixDigitCount;
        }

        /**
         * @return the numeric type of the xpath
         */
        public static NumericType getNumericType(String xpath) {
            Matcher matcher = NUMBER_PATH.matcher(xpath);
            if (xpath.contains("rational")) {
                return RATIONAL;
            } else if (!xpath.contains("/pattern")) {
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
    }

    /**
     * Turn all whitespace sequences (including tab and newline, and NBSP for certain paths) into a
     * single space or a single NBSP depending on path. Also trim initial/final NBSP, unless the
     * value is only the one character, "\u00A0"
     *
     * @param path
     * @param value
     * @return the normalized value
     */
    private String normalizeWhitespace(String path, String value) {
        PathSpaceType pst = PathSpaceType.get(path);
        if (pst == PathSpaceType.allowSp) {
            value =
                    WHITESPACE_AND_NBSP_TO_NORMALIZE
                            .matcher(value)
                            .replaceAll(" "); // replace with regular space
        } else if (pst == PathSpaceType.allowNbsp) {
            value =
                    WHITESPACE_AND_NBSP_TO_NORMALIZE
                            .matcher(value)
                            .replaceAll("\u00A0"); // replace with NBSP
            value = trimNBSP(value);
        } else if (pst == PathSpaceType.allowNNbsp) {
            value =
                    WHITESPACE_AND_NBSP_TO_NORMALIZE
                            .matcher(value)
                            .replaceAll("\u202F"); // replace with NNBSP
            value = trimNBSP(value);
        } else if (pst == PathSpaceType.allowSpOrNbsp) {
            /*
             * in this case don't normalize away NBSP
             */
            value =
                    WHITESPACE_NO_NBSP_TO_NORMALIZE
                            .matcher(value)
                            .replaceAll(" "); // replace with regular space
            /*
             * if any NBSP and regular space are adjacent, replace with NBSP
             */
            value = NBSP_PLUS_SPACE_TO_NORMALIZE.matcher(value).replaceAll("\u00A0");
            value = SPACE_PLUS_NBSP_TO_NORMALIZE.matcher(value).replaceAll("\u00A0");
            value = MULTIPLE_NBSP.matcher(value).replaceAll("\u00A0");
            value = trimNBSP(value);
        } else {
            throw new IllegalArgumentException("Unknown PathSpaceType " + pst);
        }

        // Further whitespace adjustments per CLDR-14032
        if ((scriptCode.equals("Latn") || scriptCode.equals("Cyrl") || scriptCode.equals("Grek"))
                && HOUR_FORMAT_XPATHS.matcher(path).matches()) {
            String test = AMPM_SPACE_BEFORE.matcher(value).replaceAll("$1$2"); // value without a+
            String spaceReplace = path.contains("ascii") ? "$1\u0020$3" : "$1\u202F$3";
            if (value.length() - test.length() != 4) { // exclude patterns with aaaa
                value = AMPM_SPACE_BEFORE.matcher(value).replaceAll(spaceReplace);
            }
            test = AMPM_SPACE_AFTER.matcher(value).replaceAll("$2$3"); // value without a+
            if (value.length() - test.length() != 4) { // exclude patterns with aaaa
                value = AMPM_SPACE_AFTER.matcher(value).replaceAll(spaceReplace);
            }
        }
        if (scriptCode.equals("Cyrl") && YEAR_FORMAT_XPATHS.matcher(path).matches()) {
            value = YEAR_SPACE_YEARMARKER.matcher(value).replaceAll("y\u202F$1");
        }
        if (UNIT_NARROW_XPATHS.matcher(path).matches()) {
            value = PLACEHOLDER_SPACE_AFTER.matcher(value).replaceAll("}\u202F"); // Narrow NBSP
            value = PLACEHOLDER_SPACE_BEFORE.matcher(value).replaceAll("\u202F{");
        }
        if (UNIT_SHORT_XPATHS.matcher(path).matches()) {
            value = PLACEHOLDER_SPACE_AFTER.matcher(value).replaceAll("}\u00A0"); // Regular NBSP
            value = PLACEHOLDER_SPACE_BEFORE.matcher(value).replaceAll("\u00A0{");
        }

        // Finally, replace remaining space combinations with most restrictive type CLDR-17233
        // If we have NNBSP U+202F in combination with other spaces, keep just it
        value = NNBSP_AMONG_OTHER_SPACES.matcher(value).replaceAll("\u202F");
        // Else if we have NBSP U+00A0 in combination with other spaces, keep just it
        value = NBSP_AMONG_OTHER_SPACES.matcher(value).replaceAll("\u00A0");
        // Else if we have THIN SPACE U+2009 in combination with other spaces, keep just it
        value = THIN_SPACE_AMONG_OTHER_SPACES.matcher(value).replaceAll("\u2009");

        return value;
    }

    /**
     * Delete any initial or final NBSP or NNBSP, unless the value is just NBSP or NNBSP
     *
     * @param value
     * @return the trimmed value
     */
    private String trimNBSP(String value) {
        if (!value.equals("\u00A0") && !value.equals("\u202F")) {
            value = INITIAL_NBSP.matcher(value).replaceAll("");
            value = FINAL_NBSP.matcher(value).replaceAll("");
        }
        return value;
    }

    /** Categorize xpaths according to whether they allow space, NBSP, or both */
    public enum PathSpaceType {
        allowSp,
        allowNbsp,
        allowNNbsp,
        allowSpOrNbsp;

        public static PathSpaceType get(String path) {
            if (wantsRegularSpace(path)) {
                return allowSp;
            } else if (wantsNBSP(path)) {
                return allowNbsp;
            } else if (wantsNNBSP(path)) {
                return allowNNbsp;
            } else {
                return allowSpOrNbsp;
            }
        }

        private static boolean wantsRegularSpace(String path) {
            if ((path.contains("/dateFormatLength") && path.contains("/pattern"))
                    || path.contains("/availableFormats/dateFormatItem")
                    || (path.startsWith("//ldml/dates/timeZoneNames/metazone")
                            && path.contains("/long"))
                    || path.startsWith("//ldml/dates/timeZoneNames/regionFormat")
                    || path.startsWith("//ldml/localeDisplayNames/codePatterns/codePattern")
                    || path.startsWith("//ldml/localeDisplayNames/languages/language")
                    || path.startsWith("//ldml/localeDisplayNames/territories/territory")
                    || path.startsWith("//ldml/localeDisplayNames/types/type")
                    || (path.startsWith("//ldml/numbers/currencies/currency")
                            && path.contains("/displayName"))
                    || (path.contains("/decimalFormatLength[@type=\"long\"]")
                            && path.contains("/pattern"))
                    || path.startsWith("//ldml/posix/messages")
                    || (path.startsWith("//ldml/units/uni") && path.contains("/unitPattern "))) {
                return true;
            }
            return false;
        }

        private static boolean wantsNBSP(String path) {
            if ((path.contains("/currencies/currency")
                            && (path.contains("/group") || path.contains("/pattern")))
                    || (path.contains("/currencyFormatLength") && path.contains("/pattern"))
                    || (path.contains("/currencySpacing") && path.contains("/insertBetween"))
                    || (path.contains("/decimalFormatLength") && path.contains("/pattern"))
                    || // i.e. the non-long ones
                    (path.contains("/percentFormatLength") && path.contains("/pattern"))
                    || (path.startsWith("//ldml/numbers/symbols")
                            && (path.contains("/group") || path.contains("/nan")))) {
                return true;
            }
            return false;
        }

        private static boolean wantsNNBSP(String path) {
            if ((path.contains("/dayPeriodWidth[@type=\"abbreviated\"]")
                            || path.contains("/dayPeriodWidth[@type=\"narrow\"]"))
                    && (path.contains("/dayPeriod[@type=\"am\"]")
                            || path.contains("/dayPeriod[@type=\"pm\"]"))) {
                return true;
            }
            return false;
        }
    }

    private static final Pattern ZERO_WIDTH_SPACES = PatternCache.get("\\u200B+");
    private static final Set<String> LOCALES_NOT_ALLOWING_ZWS =
            new HashSet<>(Arrays.asList("da", "fr"));

    /**
     * Remove occurrences of U+200B ZERO_WIDTH_SPACE under certain conditions
     *
     * @param value the value to be normalized
     * @return the normalized value
     *     <p>TODO: extend this method to address more concerns, after clarifying the conditions -
     *     enlarge the set LOCALES_NOT_ALLOWING_ZWS? - strip initial and final ZWS in all locales? -
     *     reduce two or more adjacent ZWS to one ZWS? - allow or prohibit ZWS by itself as currency
     *     symbol, as currently in locales kea, pt_CV, pt_PT - allow or prohibit ZWS preceding URL
     *     as in "as per [U+200B]https://www.unicode.org/reports/tr35/tr35-general.html#Annotations
     *     " Reference: https://unicode-org.atlassian.net/browse/CLDR-15976
     */
    private String normalizeZeroWidthSpace(String value) {
        if (ZERO_WIDTH_SPACES.matcher(value).find()) {
            final String localeId = locale.getBaseName();
            if (LOCALES_NOT_ALLOWING_ZWS.contains(localeId)) {
                value = ZERO_WIDTH_SPACES.matcher(value).replaceAll("");
            }
        }
        return value;
    }

    /**
     * If inheritance replacement is enabled and the value matches the Bailey (inherited) value,
     * replace the value with CldrUtility.INHERITANCE_MARKER
     *
     * <p>This is only appropriate if cldrFileForBailey != null, meaning that
     * enableInheritanceReplacement has been called -- some cost may be involved in getting
     * cldrFileForBailey and calling getBaileyValue, and some callers of DAIP may not want the
     * replacement, so the default, when enableInheritanceReplacement has not been called, is no
     * replacement
     *
     * @param path
     * @param value
     * @return the value or CldrUtility.INHERITANCE_MARKER
     */
    public String replaceBaileyWithInheritanceMarker(String path, String value) {
        if (cldrFileForBailey != null && !value.isEmpty()) {
            Output<String> pathWhereFound = new Output<>();
            Output<String> localeWhereFound = new Output<>();
            String baileyValue =
                    cldrFileForBailey.getBaileyValue(path, pathWhereFound, localeWhereFound);
            if (value.equals(baileyValue)
                    && !XMLSource.ROOT_ID.equals(localeWhereFound.value)
                    && !XMLSource.CODE_FALLBACK_ID.equals(localeWhereFound.value)) {
                return CldrUtility.INHERITANCE_MARKER;
            }
        }
        return value;
    }
}
