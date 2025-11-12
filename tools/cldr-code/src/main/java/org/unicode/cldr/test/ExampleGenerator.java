package org.unicode.cldr.test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamples;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamplesRange;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRFileOverride;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.DateConstants;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.EmojiConstants;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.NameType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.ScriptToExemplars;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.SupplementalCalendarData;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyNumberInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.XListFormatter.ListTypeLength;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.SimpleNameObject;

/**
 * Class to generate examples and help messages for the Survey tool (or console version).
 *
 * @author markdavis
 */
public class ExampleGenerator {
    private static final String FSLASH = "\u2044";
    private static final String ISOLATE_FSLASH = "\u200C\u2044\u200C";

    private static final String INTERNAL = "internal: ";
    private static final String SUBTRACTS = "➖";
    private static final String ADDS = "➕";
    private static final String HINTS = "🗝️";
    private static final String EXAMPLE_OF_INCORRECT = "❌  ";
    private static final String EXAMPLE_OF_CAUTION = "⚠️  ";

    private static final boolean DEBUG_EXAMPLE_GENERATOR = false;

    static final boolean DEBUG_SHOW_HELP = false;

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    private static final String ALT_STAND_ALONE = "[@alt=\"stand-alone\"]";

    private static final String EXEMPLAR_CITY_LOS_ANGELES =
            "//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity";

    private static final Pattern URL_PATTERN =
            Pattern.compile("http://[\\-a-zA-Z0-9]+(\\.[\\-a-zA-Z0-9]+)*([/#][\\-a-zA-Z0-9]+)*");

    private static final SupplementalDataInfo supplementalDataInfo =
            SupplementalDataInfo.getInstance();
    static final UnitConverter UNIT_CONVERTER = supplementalDataInfo.getUnitConverter();

    public static final double NUMBER_SAMPLE = 123456.789;
    public static final double NUMBER_SAMPLE_WHOLE = 2345;

    public static final TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
    public static final TimeZone GMT_ZONE_SAMPLE = TimeZone.getTimeZone("Etc/GMT");

    private static final String exampleStart = "<div class='cldr_example'>";
    private static final String exampleStartAuto = "<div class='cldr_example_auto' dir='auto'>";
    private static final String exampleStartRTL = "<div class='cldr_example_rtl' dir='rtl'>";
    private static final String exampleStartHeader = "<div class='cldr_example_rtl'>";
    private static final String exampleEnd = "</div>";
    private static final String startItalic = "<i>";
    private static final String endItalic = "</i>";
    private static final String startSup = "<sup>";
    private static final String endSup = "</sup>";
    private static final String startSub = "<sub>";
    private static final String endSub = "</sub>";
    private static final String backgroundAutoStart = "<span class='cldr_background_auto'>";
    private static final String backgroundAutoEnd = "</span>";
    private String backgroundStart = "<span class='cldr_substituted'>"; // overrideable
    private String backgroundEnd = "</span>"; // overrideable

    public static final String backgroundStartSymbol = "\uE234";
    public static final String backgroundEndSymbol = "\uE235";
    private static final String backgroundTempSymbol = "\uE236";
    private static final String exampleSeparatorSymbol = "\uE237";
    private static final String startItalicSymbol = "\uE238";
    private static final String endItalicSymbol = "\uE239";
    private static final String startSupSymbol = "\uE23A";
    private static final String endSupSymbol = "\uE23B";
    private static final String backgroundAutoStartSymbol = "\uE23C";
    private static final String backgroundAutoEndSymbol = "\uE23D";
    private static final String exampleStartAutoSymbol = "\uE23E";
    private static final String exampleStartRTLSymbol = "\uE23F";
    private static final String exampleStartHeaderSymbol = "\uE240";
    private static final String exampleEndSymbol = "\uE241";
    private static final String startSubSymbol = "\uE242";
    private static final String endSubSymbol = "\uE243";

    private static final String contextheader =
            "Key: " + backgroundAutoStartSymbol + "neutral" + backgroundAutoEndSymbol + ", RTL";

    public static final char TEXT_VARIANT = '\uFE0E';

    private static final UnicodeSet BIDI_MARKS = new UnicodeSet("[:Bidi_Control:]").freeze();

    public static final Date DATE_SAMPLE;

    private static final Date DATE_SAMPLE2;
    private static final Date DATE_SAMPLE3;
    private static final Date DATE_SAMPLE4;
    private static final Date DATE_SAMPLE5;

    static {
        Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
        calendar.set(
                1999, 8, 5, 13, 25, 59); // 1999-09-05 13:25:59 // calendar.set month is 0 based
        DATE_SAMPLE = calendar.getTime();

        calendar.set(1999, 9, 27, 13, 25, 59); // 1999-10-27 13:25:59
        DATE_SAMPLE2 = calendar.getTime();

        calendar.set(1999, 8, 5, 7, 0, 0); // 1999-09-05 07:00:00
        DATE_SAMPLE3 = calendar.getTime();

        calendar.set(1999, 8, 5, 23, 0, 0); // 1999-09-05 23:00:00
        DATE_SAMPLE4 = calendar.getTime();

        calendar.set(1999, 8, 5, 3, 25, 59); // 1999-09-05 03:25:59
        DATE_SAMPLE5 = calendar.getTime();
    }

    static final List<DecimalQuantity> CURRENCY_SAMPLES =
            ImmutableList.of(
                    DecimalQuantity_DualStorageBCD.fromExponentString("1.23"),
                    DecimalQuantity_DualStorageBCD.fromExponentString("0"),
                    DecimalQuantity_DualStorageBCD.fromExponentString("2.34"),
                    DecimalQuantity_DualStorageBCD.fromExponentString("3.45"),
                    DecimalQuantity_DualStorageBCD.fromExponentString("5.67"),
                    DecimalQuantity_DualStorageBCD.fromExponentString("1"));

    public static final Pattern PARAMETER = PatternCache.get("(\\{(?:0|[1-9][0-9]*)\\})");
    public static final Pattern PARAMETER_SKIP0 = PatternCache.get("(\\{[1-9][0-9]*\\})");
    public static final Pattern ALL_DIGITS = PatternCache.get("(\\p{Nd}+(.\\p{Nd}+)?)");

    private static final Calendar generatingCalendar = Calendar.getInstance(ULocale.US);

    private static Date getDate(int year, int month, int date, int hour, int minute, int second) {
        synchronized (generatingCalendar) {
            generatingCalendar.setTimeZone(GMT_ZONE_SAMPLE);
            generatingCalendar.set(year, month, date, hour, minute, second);
            return generatingCalendar.getTime();
        }
    }

    private static final Date FIRST_INTERVAL = getDate(2008, 10, 13, 5, 7, 9);
    private static final Map<String, Date> SECOND_INTERVAL =
            CldrUtility.asMap(
                    new Object[][] {
                        {
                            "G", getDate(1009, 11, 14, 17, 25, 35)
                        }, // "G" mostly useful for calendars that have short eras, like Japanese
                        {"y", getDate(2009, 11, 14, 17, 26, 35)},
                        {"M", getDate(2008, 11, 14, 17, 26, 35)},
                        {"d", getDate(2008, 10, 14, 17, 26, 35)},
                        {"a", getDate(2008, 10, 13, 17, 26, 35)},
                        {"h", getDate(2008, 10, 13, 6, 26, 35)},
                        {"m", getDate(2008, 10, 13, 5, 26, 35)},
                        {"s", getDate(2008, 10, 13, 5, 25, 35)}
                    });

    private static final Date FIRST_INTERVAL2 = getDate(2008, 1, 3, 5, 7, 9); // 2, 4, 6, 8, 10
    private static final Map<String, Date> SECOND_INTERVAL2 =
            CldrUtility.asMap(
                    new Object[][] {
                        {
                            "G", getDate(1009, 2, 4, 18, 8, 10)
                        }, // "G" mostly useful for calendars that have short eras, like Japanese
                        {"y", getDate(2009, 2, 4, 18, 8, 10)},
                        {"M", getDate(2008, 2, 4, 18, 8, 10)},
                        {"d", getDate(2008, 1, 4, 18, 8, 10)},
                        {"a", getDate(2008, 1, 3, 18, 8, 10)},
                        {"h", getDate(2008, 1, 3, 6, 7, 10)},
                        {"m", getDate(2008, 1, 3, 5, 7, 9)}
                    });

    public void setCachingEnabled(boolean enabled) {
        exCache.setCachingEnabled(enabled);
        icuServiceBuilder.setCachingEnabled(enabled);
    }

    /**
     * verboseErrors affects not only the verboseness of error reporting, but also, for example,
     * whether some unit tests pass or fail. The function setVerboseErrors can be used to modify it.
     * It must be initialized here to false, otherwise cldr-unittest TestAll.java fails. Reference:
     * https://unicode.org/cldr/trac/ticket/12025
     */
    private boolean verboseErrors = false;

    private final Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);

    private final CLDRFile cldrFile;

    private final CLDRFile englishFile;
    private CLDRFile cyrillicFile;
    private CLDRFile japanFile;

    private final BestMinimalPairSamples bestMinimalPairSamples;

    private final ExampleCache exCache = new ExampleCache();

    private final ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private final PluralInfo pluralInfo;

    private final GrammarInfo grammarInfo;

    private PluralSamples patternExamples;

    private final Map<String, String> subdivisionIdToName;

    private String creationTime = null; // only used if DEBUG_EXAMPLE_GENERATOR

    private final IntervalFormat intervalFormat = new IntervalFormat();

    private PathDescription pathDescription;

    /**
     * True if this ExampleGenerator is especially for generating "English" examples, false if it is
     * for generating "native" examples.
     */
    private final boolean typeIsEnglish;

    /** True if this ExampleGenerator is for RTL locale. */
    private final boolean isRTL;

    HelpMessages helpMessages;

    // map relativeTimePattern counts to possible numeric examples.
    // For few , many, and other there is not a single number that is in that category for
    // all locales, so we provide a list of values that might be good examples and use the
    // first that is in the category for the locale. Decimal fractions should be at the end.
    public static final Map<String, List<String>> COUNTS =
            new HashMap<>() {
                {
                    put("zero", List.of("0"));
                    put("one", List.of("1"));
                    put("two", List.of("2"));
                    put("few", List.of("3" /*gv*/, "20" /*gv*/));
                    put(
                            "many",
                            List.of(
                                    "11" /*ar pl ru uk*/,
                                    "1000000" /*ca es fr it pt*/,
                                    "6" /*cy*/,
                                    "7" /*ga*/,
                                    "21" /*kw*/,
                                    "0.5" /*cs sk lt*/));
                    put(
                            "other",
                            List.of(
                                    "100" /*ar cs de en es fr he it*/,
                                    "22" /*lv prg*/,
                                    "23" /*gv*/,
                                    "14" /*ceb fil tl*/,
                                    "0.5" /*pl ru uk*/));
                }
            };

    private static final CaseMap.Title TITLECASE = CaseMap.toTitle().wholeString().noLowercase();

    public CLDRFile getCldrFile() {
        return cldrFile;
    }

    /**
     * For this (locale-specific) ExampleGenerator, clear the cached examples for any paths whose
     * examples might depend on the winning value of the given path, since the winning value of the
     * given path has changed.
     *
     * @param xpath the path whose winning value has changed
     *     <p>Called by TestCache.updateExampleGeneratorCache
     */
    public void updateCache(String xpath) {
        exCache.update(xpath);
        if (ICUServiceBuilder.ISB_CAN_CLEAR_CACHE) {
            icuServiceBuilder.clearCache();
        }
    }

    /**
     * For getting the end of the "background" style. Default is "</span>". It is used in composing
     * patterns, so it can show the part that corresponds to the value.
     *
     * @return
     */
    public String getBackgroundEnd() {
        return backgroundEnd;
    }

    /**
     * For setting the end of the "background" style. Default is "</span>". It is used in composing
     * patterns, so it can show the part that corresponds to the value.
     */
    public void setBackgroundEnd(String backgroundEnd) {
        this.backgroundEnd = backgroundEnd;
    }

    /**
     * For getting the "background" style. Default is "<span style='background-color: gray'>". It is
     * used in composing patterns, so it can show the part that corresponds to the value.
     *
     * @return
     */
    public String getBackgroundStart() {
        return backgroundStart;
    }

    /**
     * For setting the "background" style. Default is "<span style='background-color: gray'>". It is
     * used in composing patterns, so it can show the part that corresponds to the value.
     */
    public void setBackgroundStart(String backgroundStart) {
        this.backgroundStart = backgroundStart;
    }

    /**
     * Set the verbosity level of internal errors. For example, setVerboseErrors(true) will cause
     * full stack traces to be shown in some cases.
     */
    public void setVerboseErrors(boolean verbosity) {
        this.verboseErrors = verbosity;
    }

    /**
     * Create an Example Generator. If this is shared across threads, it must be synchronized.
     *
     * @param resolvedCldrFile
     */
    public ExampleGenerator(CLDRFile resolvedCldrFile) {
        this(resolvedCldrFile, CLDRConfig.getInstance().getEnglish());
    }

    /**
     * Create an Example Generator. If this is shared across threads, it must be synchronized.
     *
     * @param resolvedCldrFile
     * @param englishFile
     */
    public ExampleGenerator(CLDRFile resolvedCldrFile, CLDRFile englishFile) {
        if (!resolvedCldrFile.isResolved()) {
            throw new IllegalArgumentException("CLDRFile must be resolved");
        }
        if (!englishFile.isResolved()) {
            throw new IllegalArgumentException("English CLDRFile must be resolved");
        }
        cldrFile = resolvedCldrFile;
        final String localeId = cldrFile.getLocaleID();
        subdivisionIdToName = EmojiSubdivisionNames.getSubdivisionIdToName(localeId);
        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, localeId);
        grammarInfo =
                supplementalDataInfo.getGrammarInfo(localeId); // getGrammarInfo can return null
        this.englishFile = englishFile;
        this.typeIsEnglish = (resolvedCldrFile == englishFile);
        icuServiceBuilder.setCldrFile(resolvedCldrFile);

        bestMinimalPairSamples = new BestMinimalPairSamples(cldrFile, icuServiceBuilder, false);

        String characterOrder = cldrFile.getStringValue("//ldml/layout/orientation/characterOrder");
        this.isRTL = (characterOrder != null && characterOrder.equals("right-to-left"));

        if (DEBUG_EXAMPLE_GENERATOR) {
            creationTime =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(Calendar.getInstance().getTime());
            System.out.println(
                    "🧞‍ Created new ExampleGenerator for loc " + localeId + " at " + creationTime);
        }
    }

    /**
     * Get an example string, in html, if there is one for this path, otherwise null. For use in the
     * survey tool, an example might be returned *even* if there is no value in the locale. For
     * example, the locale might have a path that English doesn't, but you want to return the best
     * English example. <br>
     * The result is valid HTML.
     *
     * <p>If generating examples for an inheritance marker, use the "real" inherited value to
     * generate from. Do this BEFORE accessing the cache, which doesn't use INHERITANCE_MARKER.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @return the example HTML, or null
     */
    public String getExampleHtml(String xpath, String value) {
        return getExampleHtmlExtended(xpath, value, false /* nonTrivial */);
    }

    /**
     * Same as getExampleHtml but return null if the result would simply be the given value plus
     * some markup
     *
     * <p>For example, for path = //ldml/localeDisplayNames/languages/language[@type="nl_BE"] and
     * value = "Flemish", getExampleHtml returns "<div class='cldr_example'>Flemish</div>", which is
     * trivial. Maybe there is some context in which such trivial examples are useful -- if not,
     * getExampleHtml should be revised to be the same as getNonTrivialExampleHtml and there won't
     * be a need for this distinct method.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @return the example HTML, or null
     */
    public String getNonTrivialExampleHtml(String xpath, String value) {
        return getExampleHtmlExtended(xpath, value, true /* nonTrivial */);
    }

    private String getExampleHtmlExtended(String xpath, String value, boolean nonTrivial) {
        if (value == null || xpath == null || xpath.endsWith("/alias")) {
            return null;
        }
        String result;
        try {
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                value = cldrFile.getBaileyValue(xpath, null, null);
                if (value == null) {
                    /*
                     * This can happen for some paths, such as
                     * //ldml/dates/timeZoneNames/metazone[@type="Mawson"]/short/daylight
                     */
                    return null;
                }
            }
            result =
                    exCache.computeIfAbsent(
                            xpath, value, (star, x, v) -> constructExampleHtml(x, v, nonTrivial));
        } catch (RuntimeException e) {
            String unchained =
                    verboseErrors ? ("<br>" + finalizeBackground(unchainException(e))) : "";
            result = "<i>Parsing error. " + finalizeBackground(e.getMessage()) + "</i>" + unchained;
        }
        return result;
    }

    /**
     * Do the main work of getExampleHtml given that the result was not found in the cache.
     *
     * <p>Creates a list that the handlers in constructExampleHtmlExtended can add examples to, and
     * then formats the example list appropriately.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @param nonTrivial true if we should avoid returning a trivial example (just value wrapped in
     *     markup)
     * @return the example HTML, or null
     */
    private String constructExampleHtml(String xpath, String value, boolean nonTrivial) {
        List<String> examples = new ArrayList<>();
        constructExampleHtmlExtended(xpath, value, examples);
        String result = formatExampleList(examples);
        if (result != null) { // Handle the outcome
            if (nonTrivial && value.equals(result)) {
                result = null;
            } else {
                result = finalizeBackground(result);
            }
        }
        return result;
    }

    private void constructExampleHtmlExtended(String xpath, String value, List<String> examples) {
        boolean showContexts =
                isRTL || BIDI_MARKS.containsSome(value); // only used for certain example types
        /*
         * Need getInstance, not getFrozenInstance here: some functions such as handleNumberSymbol
         * expect to call functions like parts.addRelative which throw exceptions if parts is frozen.
         */
        XPathParts parts = XPathParts.getFrozenInstance(xpath).cloneAsThawed();
        if (parts.contains("dateRangePattern")) { // {0} - {1}
            handleDateRangePattern(value, examples);
        } else if (parts.contains("timeZoneNames")) {
            handleTimeZoneName(parts, value, examples);
        } else if (parts.contains("localeDisplayNames")) {
            handleDisplayNames(xpath, parts, value, examples);
        } else if (parts.contains("currency")) {
            handleCurrency(xpath, parts, value, examples);
        } else if (parts.contains("eras")) {
            handleEras(parts, value, examples);
        } else if (parts.contains("quarters")) {
            handleQuarters(parts, value, examples);
        } else if (parts.contains("relative")
                || parts.contains("relativeTime")
                || parts.contains("relativePeriod")) {
            handleRelative(xpath, parts, value, examples);
        } else if (parts.contains("dayPeriods")) {
            handleDayPeriod(parts, value, examples);
        } else if (parts.contains("monthContext")) {
            handleDateSymbol(parts, value, examples);
        } else if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
            if (parts.contains("calendar")) {
                handleDateFormatItem(xpath, value, showContexts, examples);
            } else if (parts.contains("miscPatterns")) {
                handleMiscPatterns(parts, value, examples);
            } else if (parts.contains("numbers")) {
                if (parts.contains("currencyFormat")) {
                    handleCurrencyFormat(parts, value, showContexts, examples);
                } else if (!parts.contains("rationalFormats")) {
                    handleDecimalFormat(parts, value, showContexts, examples);
                }
            }
        } else if (parts.contains("rationalFormats")) {
            handleRationalFormat(parts, value, showContexts, examples);
        } else if (parts.contains("minimumGroupingDigits")) {
            handleMinimumGrouping(parts, value, examples);
        } else if (parts.getElement(2).contains("symbols")) {
            handleNumberSymbol(parts, value, examples);
        } else if (parts.contains("defaultNumberingSystem")
                || parts.contains("otherNumberingSystems")) {
            handleNumberingSystem(value, examples);
        } else if (parts.contains("currencyFormats") && parts.contains("unitPattern")) {
            formatCountValue(xpath, parts, value, examples);
        } else if (parts.getElement(-1).equals("compoundUnitPattern")) {
            handleCompoundUnit(parts, examples);
        } else if (parts.getElement(-1).equals("compoundUnitPattern1")
                || parts.getElement(-1).equals("unitPrefixPattern")) {
            handleCompoundUnit1(parts, value, examples);
        } else if (parts.getElement(-2).equals("unit")
                && (parts.getElement(-1).equals("unitPattern")
                        || parts.getElement(-1).equals("displayName"))) {
            handleFormatUnit(parts, value, examples);
        } else if (parts.getElement(-1).equals("perUnitPattern")) {
            handleFormatPerUnit(value, examples);
        } else if (parts.getElement(-2).equals("minimalPairs")) {
            handleMinimalPairs(parts, value, examples);
        } else if (parts.getElement(-1).equals("durationUnitPattern")) {
            handleDurationUnit(value, examples);
        } else if (parts.contains("intervalFormats")) {
            handleIntervalFormats(parts, value, examples);
        } else if (parts.getElement(1).equals("delimiters")) {
            handleDelimiters(parts, xpath, value, examples);
        } else if (parts.getElement(1).equals("listPatterns")) {
            handleListPatterns(parts, value, examples);
        } else if (parts.getElement(2).equals("ellipsis")) {
            handleEllipsis(parts.getAttributeValue(-1, "type"), value, examples);
        } else if (parts.getElement(-1).equals("monthPattern")) {
            handleMonthPatterns(parts, value, examples);
        } else if (parts.getElement(-1).equals("appendItem")) {
            handleAppendItems(parts, value, examples);
        } else if (parts.getElement(-1).equals("annotation")) {
            handleAnnotationName(parts, value, examples);
        } else if (parts.getElement(-1).equals("characterLabel")) {
            handleLabel(parts, value, examples);
        } else if (parts.getElement(-1).equals("characterLabelPattern")) {
            handleLabelPattern(parts, value, examples);
        } else if (parts.getElement(1).equals("personNames")) {
            handlePersonName(parts, value, examples);
        } else if (parts.getElement(-1).equals("exemplarCharacters")
                || parts.getElement(-1).equals("parseLenient")) {
            handleUnicodeSet(parts, xpath, value, examples);
        }
    }

    // Note: may want to change to locale's order; if so, these would be instance fields
    static final SimpleUnicodeSetFormatter SUSF =
            new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR);
    static final SimpleUnicodeSetFormatter SUSFNS =
            new SimpleUnicodeSetFormatter(
                    SimpleUnicodeSetFormatter.BASIC_COLLATOR,
                    CodePointEscaper.FORCE_ESCAPE_WITH_NONSPACING);
    static final String LRM = "\u200E";
    static final UnicodeSet NEEDS_LRM = new UnicodeSet("[:BidiClass=R:]").freeze();
    private static final boolean SHOW_NON_SPACING_IN_UNICODE_SET = true;

    /**
     * Add examples for UnicodeSets. First, show a hex format of non-spacing marks if there are any,
     * then show delta to the winning value if there are any.
     */
    private void handleUnicodeSet(
            XPathParts parts, String xpath, String value, List<String> examples) {
        UnicodeSet valueSet;
        try {
            valueSet = new UnicodeSet(value);
        } catch (Exception e) {
            return;
        }
        String winningValue = cldrFile.getWinningValue(xpath);
        if (!winningValue.equals(value)) {
            // show delta
            final UnicodeSet winningSet = new UnicodeSet(winningValue);
            UnicodeSet value_minus_winning = new UnicodeSet(valueSet).removeAll(winningSet);
            UnicodeSet winning_minus_value = new UnicodeSet(winningSet).removeAll(valueSet);
            if (!value_minus_winning.isEmpty()) {
                examples.add(LRM + ADDS + " " + SUSF.format(value_minus_winning));
            }
            if (!winning_minus_value.isEmpty()) {
                examples.add(LRM + SUBTRACTS + " " + SUSF.format(winning_minus_value));
            }
        }
        if (parts.containsAttributeValue("type", "auxiliary")) {
            LanguageTagParser ltp = new LanguageTagParser();
            String ltpScript = ltp.set(cldrFile.getLocaleID()).getResolvedScript();
            UnicodeSet exemplars = ScriptToExemplars.getExemplars(ltpScript);
            UnicodeSet main = cldrFile.getExemplarSet(ExemplarType.main, WinningChoice.WINNING);
            UnicodeSet mainAndAux = new UnicodeSet(main).addAll(valueSet);
            if (!mainAndAux.containsAll(exemplars)) {
                examples.add(
                        LRM
                                + HINTS
                                + " "
                                + SUSF.format(new UnicodeSet(exemplars).removeAll(mainAndAux)));
            }
        }
        if (SHOW_NON_SPACING_IN_UNICODE_SET
                && valueSet.containsSome(CodePointEscaper.FORCE_ESCAPE)) {
            for (String nsm : new UnicodeSet(valueSet).retainAll(CodePointEscaper.FORCE_ESCAPE)) {
                examples.add(CodePointEscaper.toExample(nsm.codePointAt(0)));
            }
        }
        examples.add(setBackground(INTERNAL) + valueSet.toPattern(false)); // internal format
    }

    /**
     * Holds a map and an object that are relatively expensive to build, so we don't want to do that
     * on each call. TODO clean up the synchronization model.
     */
    private static class PersonNamesCache implements ExampleCache.ClearableCache {
        Map<PersonNameFormatter.SampleType, SimpleNameObject> sampleNames = null;
        PersonNameFormatter personNameFormatter = null;

        @Override
        public void clear() {
            sampleNames = null;
            personNameFormatter = null;
        }

        Map<PersonNameFormatter.SampleType, SimpleNameObject> getSampleNames(CLDRFile cldrFile) {
            synchronized (this) {
                if (sampleNames == null) {
                    sampleNames = PersonNameFormatter.loadSampleNames(cldrFile);
                }
                return sampleNames;
            }
        }

        PersonNameFormatter getPersonNameFormatter(CLDRFile cldrFile) {
            synchronized (this) {
                if (personNameFormatter == null) {
                    personNameFormatter = new PersonNameFormatter(cldrFile);
                }
                return personNameFormatter;
            }
        }

        @Override
        public String toString() {
            return "["
                    + (sampleNames == null ? "" : Joiner.on('\n').join(sampleNames.entrySet()))
                    + ", "
                    + (personNameFormatter == null ? "" : personNameFormatter.toString())
                    + "]";
        }
    }

    /** Register the cache, so that it gets cleared when any of the paths change */
    PersonNamesCache personNamesCache =
            exCache.registerCache(
                    new PersonNamesCache(),
                    "//ldml/personNames/sampleName[@item=\"([^\"]*+)\"]/nameField[@type=\"([^\"]*+)\"]",
                    "//ldml/personNames/initialPattern[@type=\"([^\"]*+)\"]",
                    "//ldml/personNames/foreignSpaceReplacement",
                    "//ldml/personNames/nativeSpaceReplacement",
                    "//ldml/personNames/personName[@order=\"([^\"]*+)\"][@length=\"([^\"]*+)\"][@usage=\"([^\"]*+)\"][@formality=\"([^\"]*+)\"]/namePattern");

    private static final Function<String, String> BACKGROUND_TRANSFORM =
            x -> backgroundStartSymbol + x + backgroundEndSymbol;

    private void handlePersonName(XPathParts parts, String value, List<String> examples) {
        // ldml/personNames/personName[@order="givenFirst"][@length="long"][@usage="addressing"][@style="formal"]/namePattern => {prefix} {surname}
        String debugState = "start";
        try {
            FormatParameters formatParameters =
                    new FormatParameters(
                            PersonNameFormatter.Order.from(parts.getAttributeValue(2, "order")),
                            PersonNameFormatter.Length.from(parts.getAttributeValue(2, "length")),
                            PersonNameFormatter.Usage.from(parts.getAttributeValue(2, "usage")),
                            PersonNameFormatter.Formality.from(
                                    parts.getAttributeValue(2, "formality")));
            final CLDRFile cldrFile2 = getCldrFile();
            switch (parts.getElement(2)) {
                case "nameOrderLocales":
                    NameGetter nameGetter2 = cldrFile2.nameGetter();
                    for (String localeId : PersonNameFormatter.SPLIT_SPACE.split(value)) {
                        final String name =
                                localeId.equals("und")
                                        ? "«any other»"
                                        : nameGetter2.getNameFromIdentifier(localeId);
                        examples.add(localeId + " = " + name);
                    }
                    break;
                case "personName":
                    Map<PersonNameFormatter.SampleType, SimpleNameObject> sampleNames =
                            personNamesCache.getSampleNames(cldrFile2);
                    PersonNameFormatter personNameFormatter =
                            personNamesCache.getPersonNameFormatter(cldrFile2);

                    // We might need the alt, however: String alt = parts.getAttributeValue(-1,
                    // "alt");

                    boolean lastIsNative = false;
                    for (Entry<PersonNameFormatter.SampleType, SimpleNameObject>
                            typeAndSampleNameObject : sampleNames.entrySet()) {
                        NamePattern namePattern = NamePattern.from(0, value); // get the first one
                        final boolean isNative = typeAndSampleNameObject.getKey().isNative();
                        if (isNative != lastIsNative) {
                            final String title =
                                    isNative
                                            ? "🟨 Native name and script:"
                                            : "🟧 Foreign name and native script:";
                            examples.add(startItalicSymbol + title + endItalicSymbol);
                            lastIsNative = isNative;
                        }
                        debugState = "<NamePattern.from: " + namePattern;
                        final FallbackFormatter fallbackInfo =
                                personNameFormatter.getFallbackInfo();
                        debugState = "<getFallbackInfo: " + fallbackInfo;
                        final NameObject nameObject =
                                new PersonNameFormatter.TransformingNameObject(
                                        typeAndSampleNameObject.getValue(), BACKGROUND_TRANSFORM);
                        String result =
                                namePattern.format(nameObject, formatParameters, fallbackInfo);
                        debugState = "<namePattern.format: " + result;
                        examples.add(result);
                    }
                    // Extra names
                    final String script =
                            new LikelySubtags().getLikelyScript(cldrFile.getLocaleID());
                    Output<Boolean> haveHeaderLine = new Output<>(false);

                    if (!script.equals("Latn")) {
                        formatSampleName(formatParameters, englishFile, examples, haveHeaderLine);
                    }
                    if (!script.equals("Cyrl")) {
                        formatSampleName(
                                formatParameters, PersonNameScripts.Cyrl, examples, haveHeaderLine);
                    }
                    if (!script.equals("Jpan")) {
                        formatSampleName(
                                formatParameters, PersonNameScripts.Jpan, examples, haveHeaderLine);
                    }
                    break;
            }
        } catch (Exception e) {
            StringBuffer stackTrace;
            try (StringWriter sw = new StringWriter();
                    PrintWriter p = new PrintWriter(sw)) {
                e.printStackTrace(p);
                stackTrace = sw.getBuffer();
            } catch (Exception e2) {
                stackTrace = new StringBuffer("internal error");
            }
            examples.add(
                    "Internal error: " + e.getMessage() + "\n" + debugState + "\n" + stackTrace);
        }
    }

    enum PersonNameScripts {
        Latn,
        Cyrl,
        Jpan
    }

    public void formatSampleName(
            FormatParameters formatParameters,
            PersonNameScripts script,
            List<String> examples,
            Output<Boolean> haveHeaderLine) {
        switch (script) {
            case Cyrl:
                if (cyrillicFile == null) {
                    cyrillicFile = CLDRConfig.getInstance().getCldrFactory().make("uk", true);
                }
                formatSampleName(formatParameters, cyrillicFile, examples, haveHeaderLine);
                break;
            case Jpan:
                if (japanFile == null) {
                    japanFile = CLDRConfig.getInstance().getCldrFactory().make("ja", true);
                }
                formatSampleName(formatParameters, japanFile, examples, haveHeaderLine);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void formatSampleName(
            FormatParameters formatParameters,
            final CLDRFile cldrFile2,
            List<String> examples,
            Output<Boolean> haveHeaderLine) {
        PersonNameFormatter formatter2 = new PersonNameFormatter(cldrFile2);
        Map<PersonNameFormatter.SampleType, SimpleNameObject> sampleNames2 =
                PersonNameFormatter.loadSampleNames(cldrFile2);
        SimpleNameObject sampleName =
                getBestAvailable(
                        sampleNames2,
                        PersonNameFormatter.SampleType.nativeFull,
                        PersonNameFormatter.SampleType.nativeGGS);
        if (sampleName != null) {
            String result2 =
                    formatter2.format(
                            new PersonNameFormatter.TransformingNameObject(
                                    sampleName, BACKGROUND_TRANSFORM),
                            formatParameters);
            if (result2 != null) {
                if (!haveHeaderLine.value) {
                    haveHeaderLine.value = Boolean.TRUE;
                    examples.add(
                            startItalicSymbol + "🟥 Foreign name and script:" + endItalicSymbol);
                }
                examples.add(result2);
            }
        }
    }

    private SimpleNameObject getBestAvailable(
            Map<PersonNameFormatter.SampleType, SimpleNameObject> sampleNamesMap,
            PersonNameFormatter.SampleType... sampleTypes) {
        for (PersonNameFormatter.SampleType sampleType : sampleTypes) {
            SimpleNameObject result = sampleNamesMap.get(sampleType);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void handleLabelPattern(XPathParts parts, String value, List<String> examples) {
        if ("category-list".equals(parts.getAttributeValue(-1, "type"))) {
            CLDRFile cfile = getCldrFile();
            SimpleFormatter initialPattern = SimpleFormatter.compile(setBackground(value));
            String path = NameType.TERRITORY.getKeyPath("FR");
            String regionName = cfile.getStringValue(path);
            String flagName =
                    cfile.getStringValue("//ldml/characterLabels/characterLabel[@type=\"flag\"]");
            examples.add(
                    invertBackground(
                            EmojiConstants.getEmojiFromRegionCodes("FR")
                                    + " ⇒ "
                                    + initialPattern.format(flagName, regionName)));
        }
    }

    private void handleLabel(XPathParts parts, String value, List<String> examples) {
        // "//ldml/characterLabels/characterLabel[@type=\"" + typeAttributeValue + "\"]"
        switch (parts.getAttributeValue(-1, "type")) {
            case "flag":
                {
                    String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
                    CLDRFile cfile = getCldrFile();
                    SimpleFormatter initialPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                    addFlag(value2, "FR", cfile, initialPattern, examples);
                    addFlag(value2, "CN", cfile, initialPattern, examples);
                    addSubdivisionFlag(value2, "gbeng", initialPattern, examples);
                    addSubdivisionFlag(value2, "gbsct", initialPattern, examples);
                    addSubdivisionFlag(value2, "gbwls", initialPattern, examples);
                    return;
                }
            case "keycap":
                {
                    String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
                    CLDRFile cfile = getCldrFile();
                    SimpleFormatter initialPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                    examples.add(invertBackground(initialPattern.format(value2, "1")));
                    examples.add(invertBackground(initialPattern.format(value2, "10")));
                    examples.add(invertBackground(initialPattern.format(value2, "#")));
                    return;
                }
            default:
                return;
        }
    }

    private void addFlag(
            String value2,
            String isoRegionCode,
            CLDRFile cfile,
            SimpleFormatter initialPattern,
            List<String> examples) {
        String path = NameType.TERRITORY.getKeyPath(isoRegionCode);
        String regionName = cfile.getStringValue(path);
        examples.add(
                invertBackground(
                        EmojiConstants.getEmojiFromRegionCodes(isoRegionCode)
                                + " ⇒ "
                                + initialPattern.format(value2, regionName)));
    }

    private void addSubdivisionFlag(
            String value2,
            String isoSubdivisionCode,
            SimpleFormatter initialPattern,
            List<String> examples) {
        String subdivisionName = subdivisionIdToName.get(isoSubdivisionCode);
        if (subdivisionName == null) {
            subdivisionName = isoSubdivisionCode;
        }
        examples.add(
                invertBackground(
                        EmojiConstants.getEmojiFromSubdivisionCodes(isoSubdivisionCode)
                                + " ⇒ "
                                + initialPattern.format(value2, subdivisionName)));
    }

    private void handleAnnotationName(XPathParts parts, String value, List<String> examples) {
        // ldml/annotations/annotation[@cp="🦰"][@type="tts"]
        // skip anything but the name
        if (!"tts".equals(parts.getAttributeValue(-1, "type"))) {
            return;
        }
        String cp = parts.getAttributeValue(-1, "cp");
        if (cp == null || cp.isEmpty()) {
            return;
        }
        int first = cp.codePointAt(0);
        switch (first) {
            case 0x1F46A: // 👪  U+1F46A FAMILY
                examples.add(formatGroup(value, "👨‍👩‍👧‍👦", "👨", "👩", "👧", "👦"));
                examples.add(formatGroup(value, "👩‍👩‍👦", "👩", "👩", "👦"));
                break;
            case 0x1F48F: // 💏  U+1F48F KISS 👩👨
                examples.add(formatGroup(value, "👩‍❤️‍💋‍👨", "👩", "👨"));
                examples.add(formatGroup(value, "👩‍❤️‍💋‍👩", "👩", "👩"));
                break;
            case 0x1F491: // 💑  U+1F491     COUPLE WITH HEART
                examples.add(formatGroup(value, "👩‍❤️‍👨", "👩", "👨"));
                examples.add(formatGroup(value, "👩‍❤️‍👩", "👩", "👩"));
                break;
            default:
                boolean isSkin = EmojiConstants.MODIFIERS.contains(first);
                if (isSkin || EmojiConstants.HAIR.contains(first)) {
                    String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
                    CLDRFile cfile = getCldrFile();
                    String skin = "🏽";
                    String hair = "🦰";
                    String skinName = getEmojiName(cfile, skin);
                    String hairName = getEmojiName(cfile, hair);
                    if (hairName == null) {
                        hair = "[missing]";
                    }
                    SimpleFormatter initialPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                    SimpleFormatter listPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/listPatterns/listPattern[@type=\"unit-short\"]/listPatternPart[@type=\"2\"]"));

                    hair = EmojiConstants.JOINER_STRING + hair;
                    formatPeople(
                            cfile,
                            first,
                            isSkin,
                            value2,
                            "👩",
                            skin,
                            skinName,
                            hair,
                            hairName,
                            initialPattern,
                            listPattern,
                            examples);
                    formatPeople(
                            cfile,
                            first,
                            isSkin,
                            value2,
                            "👨",
                            skin,
                            skinName,
                            hair,
                            hairName,
                            initialPattern,
                            listPattern,
                            examples);
                }
                break;
        }
    }

    private String getEmojiName(CLDRFile cfile, String skin) {
        return cfile.getStringValue(
                "//ldml/annotations/annotation[@cp=\"" + skin + "\"][@type=\"tts\"]");
    }

    // ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"]
    private String formatGroup(String value, String sourceEmoji, String... components) {
        CLDRFile cfile = getCldrFile();
        SimpleFormatter initialPattern =
                SimpleFormatter.compile(
                        cfile.getStringValue(
                                "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
        String value2 = backgroundEndSymbol + value + backgroundStartSymbol;
        String[] names = new String[components.length];
        int i = 0;
        for (String component : components) {
            names[i++] = getEmojiName(cfile, component);
        }
        return backgroundStartSymbol
                + sourceEmoji
                + " ⇒ "
                + initialPattern.format(
                        value2,
                        longListPatternExample(
                                EmojiConstants.COMPOSED_NAME_LIST.getPath(), "n/a", "n/a2", names));
    }

    private void formatPeople(
            CLDRFile cfile,
            int first,
            boolean isSkin,
            String value2,
            String person,
            String skin,
            String skinName,
            String hair,
            String hairName,
            SimpleFormatter initialPattern,
            SimpleFormatter listPattern,
            Collection<String> examples) {
        String cp;
        String personName = getEmojiName(cfile, person);
        StringBuilder emoji = new StringBuilder(person).appendCodePoint(first);
        cp = UTF16.valueOf(first);
        cp = isSkin ? cp : EmojiConstants.JOINER_STRING + cp;
        examples.add(
                person + cp + " ⇒ " + invertBackground(initialPattern.format(personName, value2)));
        emoji.setLength(0);
        emoji.append(personName);
        if (isSkin) {
            skinName = value2;
            skin = cp;
        } else {
            hairName = value2;
            hair = cp;
        }
        examples.add(
                person
                        + skin
                        + hair
                        + " ⇒ "
                        + invertBackground(
                                listPattern.format(
                                        initialPattern.format(personName, skinName), hairName)));
    }

    private void handleDayPeriod(XPathParts parts, String value, List<String> examples) {
        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        final String dayPeriodType = parts.getAttributeValue(5, "type");
        if (dayPeriodType == null) {
            return; // formerly happened for some "/alias" paths
        }
        org.unicode.cldr.util.DayPeriodInfo.Type aType =
                dayPeriodType.equals("format")
                        ? DayPeriodInfo.Type.format
                        : DayPeriodInfo.Type.selection;
        DayPeriodInfo dayPeriodInfo =
                supplementalDataInfo.getDayPeriods(aType, cldrFile.getLocaleID());
        String periodString = parts.getAttributeValue(-1, "type");
        if (periodString == null) {
            return; // formerly happened for some "/alias" paths
        }
        DayPeriod dayPeriod = DayPeriod.valueOf(periodString);
        String periods = dayPeriodInfo.toString(dayPeriod);
        examples.add(periods);
        if ("format".equals(dayPeriodType)) {
            if (value == null) {
                value = "�";
            }
            R3<Integer, Integer, Boolean> info = dayPeriodInfo.getFirstDayPeriodInfo(dayPeriod);
            if (info != null) {
                int time = (((info.get0() + info.get1()) % DayPeriodInfo.DAY_LIMIT) / 2);
                String timeFormatString =
                        icuServiceBuilder.formatDayPeriod(
                                time, backgroundStartSymbol + value + backgroundEndSymbol);
                examples.add(invertBackground(timeFormatString));
            }
        }
    }

    private void handleDateSymbol(XPathParts parts, String value, List<String> examples) {
        // Currently only called for month names, can expand in the future to handle other symbols.
        // The idea is to show format months in a yMMMM?d date format, and stand-alone months in a
        // yMMMM? format.
        String length = parts.findAttributeValue("monthWidth", "type"); // wide, abbreviated, narrow
        if (length.equals("narrow")) {
            return; // no examples for narrow
        }
        String context = parts.findAttributeValue("monthContext", "type"); // format, stand-alone
        String calendarId =
                parts.findAttributeValue("calendar", "type"); // gregorian, islamic, hebrew, ...
        String monthNumId =
                parts.findAttributeValue("month", "type"); // 1-based: 1, 2, 3, ... 12 or 13

        final String[] skeletons = {"yMMMMd", "yMMMd", "yMMMM", "yMMM"};
        int skeletonIndex = (length.equals("wide")) ? 0 : 1;
        if (!context.equals("format")) {
            skeletonIndex += 2;
        }
        String checkPath =
                "//ldml/dates/calendars/calendar[@type=\""
                        + calendarId
                        + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                        + skeletons[skeletonIndex]
                        + "\"]";
        String dateFormat = cldrFile.getWinningValue(checkPath);
        if (dateFormat == null || dateFormat.indexOf("MMM") < 0) {
            // If we do not have the desired width (might be missing for MMMM) or
            // the desired format does not have alpha months (in some locales liks 'cs'
            // skeletons for MMM have pattern with M), then try the other width for same
            // context by adjusting skeletonIndex.
            skeletonIndex = (length.equals("wide")) ? skeletonIndex + 1 : skeletonIndex - 1;
            checkPath =
                    "//ldml/dates/calendars/calendar[@type=\""
                            + calendarId
                            + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                            + skeletons[skeletonIndex]
                            + "\"]";
            dateFormat = cldrFile.getWinningValue(checkPath);
        }
        if (dateFormat == null) {
            return;
        }
        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendarId, dateFormat);
        sdf.setTimeZone(ZONE_SAMPLE);
        DateFormatSymbols dfs = sdf.getDateFormatSymbols();
        // We do not know whether dateFormat is using MMMM, MMM, LLLL or LLL so
        // override all of them in our DateFormatSymbols. The DATE_SAMPLE is for
        // month 9 "September", offset of 8 in the months arrays, so override that.
        String[] monthNames = dfs.getMonths(DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE);
        monthNames[8] = value;
        dfs.setMonths(monthNames, DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE);
        monthNames = dfs.getMonths(DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED);
        monthNames[8] = value;
        dfs.setMonths(monthNames, DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED);
        monthNames = dfs.getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE);
        monthNames[8] = value;
        dfs.setMonths(monthNames, DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE);
        monthNames = dfs.getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED);
        monthNames[8] = value;
        dfs.setMonths(monthNames, DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED);
        sdf.setDateFormatSymbols(dfs);
        examples.add(sdf.format(DATE_SAMPLE));
    }

    private void handleMinimalPairs(
            XPathParts parts, String minimalPattern, List<String> examples) {
        Output<String> output = new Output<>();
        String count;
        String otherCount;
        String sample;
        String sampleBad;
        String locale = getCldrFile().getLocaleID();

        switch (parts.getElement(-1)) {
            case "ordinalMinimalPairs": // ldml/numbers/minimalPairs/ordinalMinimalPairs[@count="one"]
                count = parts.getAttributeValue(-1, "ordinal");
                sample =
                        bestMinimalPairSamples.getPluralOrOrdinalSample(
                                PluralType.ordinal,
                                count); // Pick a unit that exhibits the most variation
                otherCount = getOtherCount(locale, PluralType.ordinal, count);
                sampleBad =
                        bestMinimalPairSamples.getPluralOrOrdinalSample(
                                PluralType.ordinal,
                                otherCount); // Pick a unit that exhibits the most variation
                break;

            case "pluralMinimalPairs": // ldml/numbers/minimalPairs/pluralMinimalPairs[@count="one"]
                count = parts.getAttributeValue(-1, "count");
                sample =
                        bestMinimalPairSamples.getPluralOrOrdinalSample(
                                PluralType.cardinal,
                                count); // Pick a unit that exhibits the most variation
                otherCount = getOtherCount(locale, PluralType.cardinal, count);
                sampleBad =
                        bestMinimalPairSamples.getPluralOrOrdinalSample(
                                PluralType.cardinal,
                                otherCount); // Pick a unit that exhibits the most variation
                break;

            case "caseMinimalPairs": // ldml/numbers/minimalPairs/caseMinimalPairs[@case="accusative"]
                String gCase = parts.getAttributeValue(-1, "case");
                sample =
                        bestMinimalPairSamples.getBestUnitWithCase(
                                gCase, output); // Pick a unit that exhibits the most variation
                sampleBad = getOtherCase(sample);
                break;

            case "genderMinimalPairs": // ldml/numbers/minimalPairs/genderMinimalPairs[@gender="feminine"]
                String gender = parts.getAttributeValue(-1, "gender");
                sample = bestMinimalPairSamples.getBestUnitWithGender(gender, output);
                String otherGender = getOtherGender(gender);
                sampleBad = bestMinimalPairSamples.getBestUnitWithGender(otherGender, output);
                break;
            default:
                return;
        }
        String formattedUnit =
                format(minimalPattern, backgroundStartSymbol + sample + backgroundEndSymbol);
        examples.add(formattedUnit);
        if (sampleBad == null) {
            sampleBad = "n/a";
        }
        formattedUnit =
                format(minimalPattern, backgroundStartSymbol + sampleBad + backgroundEndSymbol);
        examples.add(EXAMPLE_OF_INCORRECT + formattedUnit);
    }

    private String getOtherGender(String gender) {
        if (gender == null || grammarInfo == null) {
            return null;
        }
        Collection<String> unitGenders =
                grammarInfo.get(
                        GrammaticalTarget.nominal,
                        GrammaticalFeature.grammaticalGender,
                        GrammaticalScope.units);
        for (String otherGender : unitGenders) {
            if (!gender.equals(otherGender)) {
                return otherGender;
            }
        }
        return null;
    }

    private String getOtherCase(String sample) {
        if (sample == null) {
            return null;
        }
        Collection<String> unitCases =
                grammarInfo.get(
                        GrammaticalTarget.nominal,
                        GrammaticalFeature.grammaticalCase,
                        GrammaticalScope.units);
        Output<String> output = new Output<>();
        for (String otherCase : unitCases) {
            String sampleBad =
                    bestMinimalPairSamples.getBestUnitWithCase(
                            otherCase, output); // Pick a unit that exhibits the most variation
            if (!sample.equals(sampleBad)) { // caution: sampleBad may be null
                return sampleBad;
            }
        }
        return null;
    }

    private static String getOtherCount(String locale, PluralType ordinal, String count) {
        String otherCount = null;
        if (!Objects.equals(count, "other")) {
            otherCount = "other";
        } else {
            PluralInfo rules = SupplementalDataInfo.getInstance().getPlurals(ordinal, locale);
            Set<String> counts = rules.getAdjustedCountStrings();
            for (String tryCount : counts) {
                if (!tryCount.equals("other")) {
                    otherCount = tryCount;
                    break;
                }
            }
        }
        return otherCount;
    }

    private UnitLength getUnitLength(XPathParts parts) {
        return UnitLength.valueOf(parts.getAttributeValue(-3, "type").toUpperCase(Locale.ENGLISH));
    }

    private void handleFormatUnit(XPathParts parts, String unitPattern, List<String> examples) {
        // Sample:
        // //ldml/units/unitLength[@type="long"]/unit[@type="duration-day"]/unitPattern[@count="one"][@case="accusative"]

        String longUnitId = parts.getAttributeValue(-2, "type");
        final String shortUnitId = UNIT_CONVERTER.getShortId(longUnitId);
        if (UnitConverter.HACK_SKIP_UNIT_NAMES.contains(shortUnitId)) {
            return;
        }

        if (parts.getElement(-1).equals("unitPattern")) {
            String count = parts.getAttributeValue(-1, "count");
            DecimalQuantity amount = getBest(Count.valueOf(count));

            if (amount != null) {
                if (shortUnitId.equals("light-speed")) {
                    Map<String, String> overrideMap =
                            ImmutableMap.of(parts.toString(), unitPattern);
                    // add examples showing usage
                    examples.add("Used as a fallback in the following:");
                    CLDRFileOverride cldrFileOverride = new CLDRFileOverride(cldrFile, overrideMap);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-second", amount);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-minute", amount);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-hour", amount);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-day", amount);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-week", amount);
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-speed-month", amount);
                    examples.add("Compare with:");
                    addFormattedUnitsConstructed(
                            cldrFileOverride, examples, parts, "light-year", amount);
                    return;
                } else {
                    addFormattedUnits(examples, parts, unitPattern, shortUnitId, amount);
                }
            }
        }
        // add related units
        Map<Rational, String> relatedUnits =
                UNIT_CONVERTER.getRelatedExamples(
                        shortUnitId, UnitConverter.getExampleUnitSystems(cldrFile.getLocaleID()));
        String unitSystem = null;
        boolean first = true;
        for (Entry<Rational, String> relatedUnitInfo : relatedUnits.entrySet()) {
            if (unitSystem == null) {
                Set<UnitSystem> systems = UNIT_CONVERTER.getSystemsEnum(shortUnitId);
                unitSystem = UnitSystem.getSystemsDisplay(systems);
            }
            Rational relatedValue = relatedUnitInfo.getKey();
            String relatedUnit = relatedUnitInfo.getValue();
            Set<UnitSystem> systems = UNIT_CONVERTER.getSystemsEnum(relatedUnit);
            String relation = "≡";
            String relatedValueDisplay = relatedValue.toString(FormatStyle.approx);
            if (relatedValueDisplay.startsWith("~")) {
                relation = "≈";
                relatedValueDisplay = relatedValueDisplay.substring(1);
            }
            if (first) {
                if (!examples.isEmpty()) {
                    examples.add(""); // add blank line
                }
                first = false;
            }
            examples.add(
                    String.format(
                            "1 %s%s %s %s %s%s",
                            shortUnitId,
                            unitSystem,
                            relation,
                            relatedValueDisplay,
                            relatedUnit,
                            UnitSystem.getSystemsDisplay(systems)));
        }
    }

    private void addFormattedUnitsConstructed(
            CLDRFile file,
            List<String> examples,
            XPathParts parts,
            String shortUnitId,
            DecimalQuantity amount) {
        UnitId unitId = UNIT_CONVERTER.createUnitId(shortUnitId);
        // ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"one\"]
        String pattern =
                unitId.toString(
                        file,
                        parts.getAttributeValue(2, "type"),
                        parts.getAttributeValue(-1, "count"),
                        null,
                        null,
                        true);
        addSimpleFormattedUnits(examples, pattern, amount);
    }

    /**
     * Handles paths like:<br>
     * //ldml/units/unitLength[@type="long"]/unit[@type="volume-fluid-ounce-imperial"]/unitPattern[@count="other"]
     */
    public void addFormattedUnits(
            List<String> examples,
            XPathParts parts,
            String unitPattern,
            final String shortUnitId,
            DecimalQuantity amount) {
        /*
         * PluralRules.FixedDecimal is deprecated, but deprecated in ICU is
         * also used to mark internal methods (which are OK for us to use in CLDR).
         */
        String formattedAmount = addSimpleFormattedUnits(examples, unitPattern, amount);

        if (parts.getElement(-2).equals("unit")) {
            if (unitPattern != null) {
                String gCase = parts.getAttributeValue(-1, "case");
                if (gCase == null) {
                    gCase = GrammaticalFeature.grammaticalCase.getDefault(null);
                }
                Collection<String> unitCaseInfo = null;
                if (grammarInfo != null) {
                    unitCaseInfo =
                            grammarInfo.get(
                                    GrammaticalTarget.nominal,
                                    GrammaticalFeature.grammaticalCase,
                                    GrammaticalScope.units);
                }
                String minimalPattern =
                        cldrFile.getStringValue(
                                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\""
                                        + gCase
                                        + "\"]");
                if (minimalPattern != null) {
                    String composed =
                            format(
                                    unitPattern,
                                    backgroundStartSymbol + formattedAmount + backgroundEndSymbol);
                    examples.add(
                            backgroundStartSymbol
                                    + format(
                                            minimalPattern,
                                            backgroundEndSymbol + composed + backgroundStartSymbol)
                                    + backgroundEndSymbol);
                    // get contrasting case
                    if (unitCaseInfo != null && !unitCaseInfo.isEmpty()) {
                        String constrastingCase =
                                getConstrastingCase(unitPattern, gCase, unitCaseInfo, parts);
                        if (constrastingCase != null) {
                            minimalPattern =
                                    cldrFile.getStringValue(
                                            "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\""
                                                    + constrastingCase
                                                    + "\"]");
                            composed =
                                    format(
                                            unitPattern,
                                            backgroundStartSymbol
                                                    + formattedAmount
                                                    + backgroundEndSymbol);
                            examples.add(
                                    EXAMPLE_OF_INCORRECT
                                            + backgroundStartSymbol
                                            + format(
                                                    minimalPattern,
                                                    backgroundEndSymbol
                                                            + composed
                                                            + backgroundStartSymbol)
                                            + backgroundEndSymbol);
                        }
                    } else {
                        examples.add(EXAMPLE_OF_CAUTION + "️No Case Minimal Pair available yet️");
                    }
                }
            }
        }
    }

    private String addSimpleFormattedUnits(
            List<String> examples, String unitPattern, DecimalQuantity amount) {
        DecimalFormat numberFormat;
        String formattedAmount;
        numberFormat = icuServiceBuilder.getNumberFormat(1);
        formattedAmount = numberFormat.format(amount.toBigDecimal());

        examples.add(
                format(unitPattern, backgroundStartSymbol + formattedAmount + backgroundEndSymbol));
        return formattedAmount;
    }

    private String getConstrastingCase(
            String unitPattern, String gCase, Collection<String> unitCaseInfo, XPathParts parts) {
        for (String otherCase : unitCaseInfo) {
            if (otherCase.equals(gCase)) {
                continue;
            }
            parts.putAttributeValue(-1, "case", "nominative".equals(otherCase) ? null : otherCase);
            String otherValue = cldrFile.getStringValue(parts.toString());
            if (otherValue != null && !otherValue.equals(unitPattern)) {
                return otherCase;
            }
        }
        return null;
    }

    private void handleFormatPerUnit(String value, List<String> examples) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        examples.add(
                format(
                        value,
                        backgroundStartSymbol + numberFormat.format(1) + backgroundEndSymbol));
    }

    public void handleCompoundUnit(XPathParts parts, List<String> examples) {
        UnitLength unitLength = getUnitLength(parts);
        String compoundType = parts.getAttributeValue(-2, "type");
        Count count =
                Count.valueOf(CldrUtility.ifNull(parts.getAttributeValue(-1, "count"), "other"));
        handleCompoundUnit(unitLength, compoundType, count, examples);
    }

    @SuppressWarnings("deprecation")
    public void handleCompoundUnit(
            UnitLength unitLength, String compoundType, Count count, List<String> examples) {
        /*
             *  <units>
        <unitLength type="long">
            <alias source="locale" path="../unitLength[@type='short']"/>
        </unitLength>
        <unitLength type="short">
            <compoundUnit type="per">
                <unitPattern count="other">{0}/{1}</unitPattern>
            </compoundUnit>

             *  <compoundUnit type="per">
                <unitPattern count="one">{0}/{1}</unitPattern>
                <unitPattern count="other">{0}/{1}</unitPattern>
            </compoundUnit>
         <unit type="length-m">
                <unitPattern count="one">{0} meter</unitPattern>
                <unitPattern count="other">{0} meters</unitPattern>
            </unit>

             */

        // we want to get a number that works for the count passed in.
        DecimalQuantity amount = getBest(count);
        if (amount == null) {
            examples.add("n/a");
            return;
        }
        DecimalQuantity oneValue = DecimalQuantity_DualStorageBCD.fromExponentString("1");

        String unit1mid;
        String unit2mid;
        switch (compoundType) {
            default:
                examples.add("n/a");
                return;
            case "per":
                unit1mid = getFormattedUnit("length-meter", unitLength, amount);
                unit2mid = getFormattedUnit("duration-second", unitLength, oneValue, "");
                break;
            case "times":
                unit1mid =
                        getFormattedUnit(
                                "force-newton",
                                unitLength,
                                oneValue,
                                icuServiceBuilder.getNumberFormat(1).format(amount.toBigDecimal()));
                unit2mid = getFormattedUnit("length-meter", unitLength, amount, "");
                break;
        }
        String unit1 = backgroundStartSymbol + unit1mid.trim() + backgroundEndSymbol;
        String unit2 = backgroundStartSymbol + unit2mid.trim() + backgroundEndSymbol;

        String form = this.pluralInfo.getPluralRules().select(amount);
        // we rebuild a path, because we may have changed it.
        String perPath = makeCompoundUnitPath(unitLength, compoundType, "compoundUnitPattern");
        examples.add(format(getValueFromFormat(perPath, form), unit1, unit2));
    }

    public void handleCompoundUnit1(
            XPathParts parts, String compoundPattern, List<String> examples) {
        UnitLength unitLength = getUnitLength(parts);
        String pathCount = parts.getAttributeValue(-1, "count");
        if (pathCount == null) {
            handleCompoundUnit1Name(unitLength, compoundPattern, examples);
        } else {
            handleCompoundUnit1(unitLength, Count.valueOf(pathCount), compoundPattern, examples);
        }
    }

    private void handleCompoundUnit1Name(
            UnitLength unitLength, String compoundPattern, List<String> examples) {
        String pathFormat =
                "//ldml/units/unitLength"
                        + unitLength.typeString
                        + "/unit[@type=\"{0}\"]/displayName";

        String meterFormat = getValueFromFormat(pathFormat, "length-meter");

        String modFormat =
                combinePrefix(meterFormat, compoundPattern, unitLength == UnitLength.LONG);

        examples.add(removeEmptyRuns(modFormat));
    }

    public void handleCompoundUnit1(
            UnitLength unitLength, Count count, String compoundPattern, List<String> examples) {

        // we want to get a number that works for the count passed in.
        DecimalQuantity amount = getBest(count);
        if (amount == null) {
            examples.add("n/a");
            return;
        }
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);

        @SuppressWarnings("deprecation")
        String form1 = this.pluralInfo.getPluralRules().select(amount);

        String pathFormat =
                "//ldml/units/unitLength"
                        + unitLength.typeString
                        + "/unit[@type=\"{0}\"]/unitPattern[@count=\"{1}\"]";

        // now pick up the meter pattern

        String meterFormat = getValueFromFormat(pathFormat, "length-meter", form1);

        // now combine them

        String modFormat =
                combinePrefix(meterFormat, compoundPattern, unitLength == UnitLength.LONG);

        examples.add(
                removeEmptyRuns(format(modFormat, numberFormat.format(amount.toBigDecimal()))));
    }

    // TODO, pass in unitLength instead of last parameter, and do work in Units.combinePattern.

    public String combinePrefix(
            String unitFormat, String inCompoundPattern, boolean lowercaseUnitIfNoSpaceInCompound) {
        // mark the part except for the {0} as foreground
        String compoundPattern =
                backgroundEndSymbol
                        + inCompoundPattern.replace(
                                "{0}", backgroundStartSymbol + "{0}" + backgroundEndSymbol)
                        + backgroundStartSymbol;

        String modFormat =
                Units.combinePattern(unitFormat, compoundPattern, lowercaseUnitIfNoSpaceInCompound);

        return backgroundStartSymbol + modFormat + backgroundEndSymbol;
    }

    // ldml/units/unitLength[@type="long"]/compoundUnit[@type="per"]/compoundUnitPattern
    public String makeCompoundUnitPath(
            UnitLength unitLength, String compoundType, String patternType) {
        return "//ldml/units/unitLength"
                + unitLength.typeString
                + "/compoundUnit[@type=\""
                + compoundType
                + "\"]"
                + "/"
                + patternType;
    }

    @SuppressWarnings("deprecation")
    private DecimalQuantity getBest(Count count) {
        DecimalQuantitySamples samples =
                pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.DECIMAL);
        if (samples == null) {
            samples =
                    pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.INTEGER);
        }
        if (samples == null) {
            return null;
        }
        Set<DecimalQuantitySamplesRange> samples2 = samples.getSamples();
        DecimalQuantitySamplesRange range = samples2.iterator().next();
        return range.end;
    }

    private void handleMiscPatterns(XPathParts parts, String value, List<String> examples) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(0);
        String start = backgroundStartSymbol + numberFormat.format(99) + backgroundEndSymbol;
        if ("range".equals(parts.getAttributeValue(-1, "type"))) {
            String end = backgroundStartSymbol + numberFormat.format(144) + backgroundEndSymbol;
            examples.add(format(value, start, end));
        } else {
            examples.add(format(value, start));
        }
    }

    private void handleIntervalFormats(XPathParts parts, String value, List<String> examples) {
        if (parts.getElement(6).equals("intervalFormatFallback")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            String fallbackFormat = invertBackground(setBackground(value));
            examples.add(
                    format(
                            fallbackFormat,
                            dateFormat.format(FIRST_INTERVAL),
                            dateFormat.format(SECOND_INTERVAL.get("y"))));
            return;
        }
        addIntervalExample(FIRST_INTERVAL, SECOND_INTERVAL, parts, value, examples);
        addIntervalExample(FIRST_INTERVAL2, SECOND_INTERVAL2, parts, value, examples);
        Set<String> relatedPatterns = RelatedDatePathValues.getRelatedPathValues(cldrFile, parts);
        if (!relatedPatterns.isEmpty()) {
            examples.add("Related Flexible Dates:");
            for (String pattern : relatedPatterns) {
                SimpleDateFormat sdf =
                        icuServiceBuilder.getDateFormat(
                                parts.getAttributeValue(
                                        RelatedDatePathValues.calendarElement, "type"),
                                pattern);
                examples.add(sdf.format(FIRST_INTERVAL));
                examples.add(sdf.format(FIRST_INTERVAL2));
            }
        }
        // de-dup, just in case
        LinkedHashSet<String> dedup = new LinkedHashSet<>(examples);
        examples.clear();
        examples.addAll(dedup);
    }

    private void addIntervalExample(
            Date earlier,
            Map<String, Date> laterMap,
            XPathParts parts,
            String value,
            List<String> examples) {
        String greatestDifference = parts.getAttributeValue(-1, "id");

        // Choose an example interval suitable for the symbol. If testing years, use an interval
        // of more than one year, and so forth. For the purpose of choosing the interval,
        // "H" is equivalent to "h", and so forth; map to a symbol that occurs in SECOND_INTERVAL.

        if (greatestDifference.equals("H")) { // Hour [0-23]
            greatestDifference = "h"; // Hour [1-12]
        } else if (greatestDifference.equals("B") // flexible day periods
                || greatestDifference.equals("b")) { // am, pm, noon, midnight
            greatestDifference = "a"; // AM, PM
        }
        // Example:
        // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="yMd"]/greatestDifference[@id="y"]
        // Find where to split the value
        intervalFormat.setPattern(parts, value);
        Date later = laterMap.get(greatestDifference);
        if (later != null) {

            // The later variable may be null for some less-frequently used symbols such
            // as "Q" (Quarter).
            // Reference:
            // https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table
            // For now, such paths do not get examples.

            examples.add(intervalFormat.format(earlier, later));
        }
    }

    private void handleDelimiters(
            XPathParts parts, String xpath, String value, List<String> examples) {
        String lastElement = parts.getElement(-1);
        final String[] elements = {
            "quotationStart", "alternateQuotationStart",
            "alternateQuotationEnd", "quotationEnd"
        };
        String[] quotes = new String[4];
        String baseXpath = xpath.substring(0, xpath.lastIndexOf('/'));
        for (int i = 0; i < quotes.length; i++) {
            String currElement = elements[i];
            if (lastElement.equals(currElement)) {
                quotes[i] = backgroundStartSymbol + value + backgroundEndSymbol;
            } else {
                quotes[i] = cldrFile.getWinningValue(baseXpath + '/' + currElement);
            }
        }
        String example =
                cldrFile.getStringValue(
                        "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"gregorian\"]");
        // NOTE: the example provided here is partially in English because we don't
        // have a translated conversational example in CLDR.
        examples.add(
                invertBackground(
                        format("{0}They said {1}" + example + "{2}.{3}", (Object[]) quotes)));
    }

    private void handleListPatterns(XPathParts parts, String value, List<String> examples) {
        // listPatternType is either "duration" or null/other list
        String listPatternType = parts.getAttributeValue(-2, "type");
        if (listPatternType == null || !listPatternType.contains("unit")) {
            handleRegularListPatterns(parts, value, ListTypeLength.from(listPatternType), examples);
        } else {
            handleDurationListPatterns(parts, value, UnitLength.from(listPatternType), examples);
        }
    }

    private void handleRegularListPatterns(
            XPathParts parts, String value, ListTypeLength listTypeLength, List<String> examples) {
        String patternType = parts.getAttributeValue(-1, "type");
        if (patternType == null) {
            return; // formerly happened for some "/alias" paths
        }
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        if (patternType.equals("2")) {
            examples.add(invertBackground(format(setBackground(value), territory1, territory2)));
            return;
        }
        String territory3 = getValueFromFormat(pathFormat, "EG");
        String territory4 = getValueFromFormat(pathFormat, "CA");
        examples.add(
                longListPatternExample(
                        listTypeLength.getPath(),
                        patternType,
                        value,
                        territory1,
                        territory2,
                        territory3,
                        territory4));
    }

    private void handleDurationListPatterns(
            XPathParts parts, String value, UnitLength unitWidth, List<String> examples) {
        String patternType = parts.getAttributeValue(-1, "type");
        if (patternType == null) {
            return; // formerly happened for some "/alias" paths
        }
        String duration1 = getFormattedUnit("duration-day", unitWidth, 4);
        String duration2 = getFormattedUnit("duration-hour", unitWidth, 2);
        if (patternType.equals("2")) {
            examples.add(invertBackground(format(setBackground(value), duration1, duration2)));
            return;
        }
        String duration3 = getFormattedUnit("duration-minute", unitWidth, 37);
        String duration4 = getFormattedUnit("duration-second", unitWidth, 23);
        examples.add(
                longListPatternExample(
                        unitWidth.listTypeLength.getPath(),
                        patternType,
                        value,
                        duration1,
                        duration2,
                        duration3,
                        duration4));
    }

    public enum UnitLength {
        LONG(ListTypeLength.UNIT_WIDE),
        SHORT(ListTypeLength.UNIT_SHORT),
        NARROW(ListTypeLength.UNIT_NARROW);
        final String typeString;
        final ListTypeLength listTypeLength;

        UnitLength(ListTypeLength listTypeLength) {
            typeString = "[@type=\"" + name().toLowerCase(Locale.ENGLISH) + "\"]";
            this.listTypeLength = listTypeLength;
        }

        public static UnitLength from(String listPatternType) {
            switch (listPatternType) {
                case "unit":
                    return UnitLength.LONG;
                case "unit-narrow":
                    return UnitLength.NARROW;
                case "unit-short":
                    return UnitLength.SHORT;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private String getFormattedUnit(
            String unitType, UnitLength unitWidth, DecimalQuantity unitAmount) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return getFormattedUnit(
                unitType, unitWidth, unitAmount, numberFormat.format(unitAmount.toBigDecimal()));
    }

    private String getFormattedUnit(String unitType, UnitLength unitWidth, double unitAmount) {
        return getFormattedUnit(
                unitType, unitWidth, new DecimalQuantity_DualStorageBCD(unitAmount));
    }

    @SuppressWarnings("deprecation")
    private String getFormattedUnit(
            String unitType,
            UnitLength unitWidth,
            DecimalQuantity unitAmount,
            String formattedUnitAmount) {
        String form = this.pluralInfo.getPluralRules().select(unitAmount);
        String pathFormat =
                "//ldml/units/unitLength"
                        + unitWidth.typeString
                        + "/unit[@type=\"{0}\"]/unitPattern[@count=\"{1}\"]";
        return format(getValueFromFormat(pathFormat, unitType, form), formattedUnitAmount);
    }

    // ldml/listPatterns/listPattern/listPatternPart[@type="2"] — And
    // ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"] Short And
    // ldml/listPatterns/listPattern[@type="or"]/listPatternPart[@type="2"] or list
    // ldml/listPatterns/listPattern[@type="unit"]/listPatternPart[@type="2"]
    // ldml/listPatterns/listPattern[@type="unit-short"]/listPatternPart[@type="2"]
    // ldml/listPatterns/listPattern[@type="unit-narrow"]/listPatternPart[@type="2"]

    private String longListPatternExample(
            String listPathFormat, String patternType, String value, String... items) {
        String doublePattern = getPattern(listPathFormat, "2", patternType, value);
        String startPattern = getPattern(listPathFormat, "start", patternType, value);
        String middlePattern = getPattern(listPathFormat, "middle", patternType, value);
        String endPattern = getPattern(listPathFormat, "end", patternType, value);
        /*
         * DateTimePatternGenerator.FormatParser is deprecated, but deprecated in ICU is
         * also used to mark internal methods (which are OK for us to use in CLDR).
         */
        @SuppressWarnings("deprecation")
        ListFormatter listFormatter =
                new ListFormatter(doublePattern, startPattern, middlePattern, endPattern);
        String example = listFormatter.format((Object[]) items);
        return invertBackground(example);
    }

    /**
     * Helper method for handleListPatterns. Returns the pattern to be used for a specified pattern
     * type.
     *
     * @param pathFormat
     * @param pathPatternType
     * @param valuePatternType
     * @param value
     * @return
     */
    private String getPattern(
            String pathFormat, String pathPatternType, String valuePatternType, String value) {
        return valuePatternType.equals(pathPatternType)
                ? setBackground(value)
                : getValueFromFormat(pathFormat, pathPatternType);
    }

    private String getValueFromFormat(String format, Object... arguments) {
        return cldrFile.getWinningValue(format(format, arguments));
    }

    public void handleEllipsis(String type, String value, List<String> examples) {
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        //  <ellipsis type="word-final">{0} …</ellipsis>
        //  <ellipsis type="word-initial">… {0}</ellipsis>
        //  <ellipsis type="word-medial">{0} … {1}</ellipsis>
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        if (territory1 == null || territory2 == null) {
            return;
        }
        // if it isn't a word, break in the middle
        if (!type.contains("word")) {
            territory1 = clip(territory1, 0, 1);
            territory2 = clip(territory2, 1, 0);
        }
        if (type.contains("initial")) {
            territory1 = territory2;
        }
        examples.add(invertBackground(format(setBackground(value), territory1, territory2)));
    }

    public static String clip(String text, int clipStart, int clipEnd) {
        BreakIterator bi = BreakIterator.getCharacterInstance();
        bi.setText(text);
        for (int i = 0; i < clipStart; ++i) {
            bi.next();
        }
        int start = bi.current();
        bi.last();
        for (int i = 0; i < clipEnd; ++i) {
            bi.previous();
        }
        int end = bi.current();
        return start >= end ? text : text.substring(start, end);
    }

    /**
     * Handle miscellaneous calendar patterns.
     *
     * @param parts
     * @param value
     * @return
     */
    private void handleMonthPatterns(XPathParts parts, String value, List<String> examples) {
        String calendar = parts.getAttributeValue(3, "type");
        String context = parts.getAttributeValue(5, "type");
        String month = "8";
        if (!context.equals("numeric")) {
            String width = parts.getAttributeValue(6, "type");
            String xpath =
                    "//ldml/dates/calendars/calendar[@type=\"{0}\"]/months/monthContext[@type=\"{1}\"]/monthWidth[@type=\"{2}\"]/month[@type=\"8\"]";
            month = getValueFromFormat(xpath, calendar, context, width);
        }
        examples.add(invertBackground(format(setBackground(value), month)));
    }

    private void handleAppendItems(XPathParts parts, String value, List<String> examples) {
        String request = parts.getAttributeValue(-1, "request");
        if (!"Timezone".equals(request)) {
            return;
        }
        String calendar = parts.getAttributeValue(3, "type");

        SimpleDateFormat sdf =
                icuServiceBuilder.getDateFormat(calendar, 0, DateFormat.MEDIUM, null);
        String zone = cldrFile.getStringValue("//ldml/dates/timeZoneNames/gmtZeroFormat");
        examples.add(format(value, setBackground(sdf.format(DATE_SAMPLE)), setBackground(zone)));
    }

    public class IntervalFormat {
        @SuppressWarnings("deprecation")
        DateTimePatternGenerator.FormatParser formatParser =
                new DateTimePatternGenerator.FormatParser();

        SimpleDateFormat firstFormat = new SimpleDateFormat();
        SimpleDateFormat secondFormat = new SimpleDateFormat();
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        BitSet letters = new BitSet();

        public String format(Date earlier, Date later) {
            if (earlier == null || later == null) {
                return null;
            }
            if (later.compareTo(earlier) < 0) {
                /*
                 * Swap so earlier is earlier than later.
                 * This is necessary for "G" (Era) given the current FIRST_INTERVAL, SECOND_INTERVAL
                 */
                Date tmp = earlier;
                earlier = later;
                later = tmp;
            }
            return firstFormat.format(earlier) + secondFormat.format(later);
        }

        @SuppressWarnings("deprecation")
        public IntervalFormat setPattern(XPathParts parts, String pattern) {
            if (formatParser == null || pattern == null) {
                return this;
            }
            try {
                formatParser.set(pattern);
            } catch (NullPointerException e) {
                /*
                 * This has been observed to occur, within ICU, for unknown reasons.
                 */
                System.err.println(
                        "Caught NullPointerException in IntervalFormat.setPattern, pattern = "
                                + pattern);
                e.printStackTrace();
                return null;
            }
            first.setLength(0);
            second.setLength(0);
            boolean doFirst = true;
            letters.clear();

            for (Object item : formatParser.getItems()) {
                if (item instanceof DateTimePatternGenerator.VariableField) {
                    char c = item.toString().charAt(0);
                    if (letters.get(c)) {
                        doFirst = false;
                    } else {
                        letters.set(c);
                    }
                    if (doFirst) {
                        first.append(item);
                    } else {
                        second.append(item);
                    }
                } else {
                    if (doFirst) {
                        first.append(formatParser.quoteLiteral((String) item));
                    } else {
                        second.append(formatParser.quoteLiteral((String) item));
                    }
                }
            }
            String calendar = parts.findAttributeValue("calendar", "type");
            firstFormat = icuServiceBuilder.getDateFormat(calendar, first.toString());
            firstFormat.setTimeZone(GMT_ZONE_SAMPLE);

            secondFormat = icuServiceBuilder.getDateFormat(calendar, second.toString());
            secondFormat.setTimeZone(GMT_ZONE_SAMPLE);
            return this;
        }
    }

    private void handleDurationUnit(String value, List<String> examples) {
        DateFormat df = this.icuServiceBuilder.getDateFormat("gregorian", value.replace('h', 'H'));
        df.setTimeZone(TimeZone.GMT_ZONE);
        long time = ((5 * 60 + 37) * 60 + 23) * 1000;
        try {
            examples.add(df.format(new Date(time)));
        } catch (IllegalArgumentException e) {
            // e.g., Illegal pattern character 'o' in "aɖabaƒoƒo m:ss"
            return;
        }
    }

    @SuppressWarnings("deprecation")
    private void formatCountValue(
            String xpath, XPathParts parts, String value, List<String> examples) {
        if (!parts.containsAttribute("count")) { // no examples for items that don't format
            return;
        }
        final PluralInfo plurals =
                supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
        PluralRules pluralRules = plurals.getPluralRules();

        String unitType = parts.getAttributeValue(-2, "type");
        if (unitType == null) {
            unitType = "USD"; // sample for currency pattern
        }
        final boolean isPattern = parts.contains("unitPattern");
        final boolean isCurrency = !parts.contains("units");

        Count count;
        final LinkedHashSet<DecimalQuantity> exampleCount = new LinkedHashSet<>(CURRENCY_SAMPLES);
        String countString = parts.getAttributeValue(-1, "count");
        if (countString == null) {
            return;
        } else {
            try {
                count = Count.valueOf(countString);
            } catch (Exception e) {
                return; // counts like 0
            }
        }

        // we used to just get the samples for the given keyword, but that doesn't work well any
        // more.
        getStartEndSamples(
                pluralRules.getDecimalSamples(countString, SampleType.INTEGER), exampleCount);
        getStartEndSamples(
                pluralRules.getDecimalSamples(countString, SampleType.DECIMAL), exampleCount);

        String result = "";
        DecimalFormat currencyFormat = icuServiceBuilder.getCurrencyFormat(unitType);
        int decimalCount = currencyFormat.getMinimumFractionDigits();

        // Unless/until DecimalQuantity overrides hashCode() or implements Comparable, we
        // should use a concrete collection type for examplesSeen for which .contains() only
        // relies on DecimalQuantity.equals() . The reason is that the default hashCode()
        // implementation for DecimalQuantity may return false when .equals() returns true.
        Collection<DecimalQuantity> examplesSeen = new ArrayList<>();

        // we will cycle until we have (at most) two examples.
        int maxCount = 2;
        main:
        // If we are a currency, we will try to see if we can set the decimals to match.
        // but if nothing works, we will just use a plain sample.
        for (int phase = 0; phase < 2; ++phase) {
            for (DecimalQuantity example : exampleCount) {
                // we have to first see whether we have a currency. If so, we have to see if the
                // count works.

                if (isCurrency && phase == 0) {
                    DecimalQuantity_DualStorageBCD newExample =
                            new DecimalQuantity_DualStorageBCD();
                    newExample.copyFrom(example);
                    newExample.setMinFraction(decimalCount);
                    example = newExample;
                }
                // skip if we've done before (can happen because of the currency reset)
                if (examplesSeen.contains(example)) {
                    continue;
                }
                examplesSeen.add(example);
                // skip if the count isn't appropriate
                if (!pluralRules.select(example).equals(count.toString())) {
                    continue;
                }

                if (value == null) {
                    String fallbackPath = cldrFile.getCountPathWithFallback(xpath, count, true);
                    value = cldrFile.getStringValue(fallbackPath);
                }
                String resultItem;

                resultItem = formatCurrency(value, unitType, isPattern, isCurrency, count, example);
                // now add to list
                result = addExampleResult(resultItem, result);
                if (isPattern) {
                    String territory = getDefaultTerritory();
                    String currency = supplementalDataInfo.getDefaultCurrency(territory);
                    if (currency.equals(unitType)) {
                        currency = "EUR";
                        if (currency.equals(unitType)) {
                            currency = "JAY";
                        }
                    }
                    resultItem =
                            formatCurrency(value, currency, isPattern, isCurrency, count, example);
                    // now add to list
                    result = addExampleResult(resultItem, result);
                }
                if (--maxCount < 1) {
                    break main;
                }
            }
        }
        examples.add(result.isEmpty() ? null : result);
    }

    @SuppressWarnings("deprecation")
    public static void getStartEndSamples(
            DecimalQuantitySamples samples, Set<DecimalQuantity> target) {
        if (samples != null) {
            for (DecimalQuantitySamplesRange item : samples.getSamples()) {
                target.add(item.start);
                target.add(item.end);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String formatCurrency(
            String value,
            String unitType,
            final boolean isPattern,
            final boolean isCurrency,
            Count count,
            DecimalQuantity example) {
        String resultItem;
        {
            // If we have a pattern, get the unit from the count
            // If we have a unit, get the pattern from the count
            // English is special; both values are retrieved based on the count.
            String unitPattern;
            String unitName;
            if (isPattern) {
                // //ldml/numbers/currencies/currency[@type="USD"]/displayName
                unitName = getUnitName(unitType, isCurrency, count);
                unitPattern = typeIsEnglish ? getUnitPattern(unitType, isCurrency, count) : value;
            } else {
                unitPattern = getUnitPattern(unitType, isCurrency, count);
                unitName = typeIsEnglish ? getUnitName(unitType, isCurrency, count) : value;
            }

            if (isPattern) {
                unitPattern = setBackground(unitPattern);
            } else {
                unitPattern = setBackgroundExceptMatch(unitPattern, PARAMETER_SKIP0);
            }

            MessageFormat unitPatternFormat = new MessageFormat(unitPattern);

            // get the format for the currency
            // TODO fix this for special currency overrides

            DecimalFormat unitDecimalFormat = icuServiceBuilder.getNumberFormat(1); // decimal
            unitDecimalFormat.setMaximumFractionDigits((int) example.getPluralOperand(Operand.v));
            unitDecimalFormat.setMinimumFractionDigits((int) example.getPluralOperand(Operand.v));

            String formattedNumber = unitDecimalFormat.format(example.toDouble());
            unitPatternFormat.setFormatByArgumentIndex(0, unitDecimalFormat);
            resultItem = unitPattern.replace("{0}", formattedNumber).replace("{1}", unitName);

            if (isPattern) {
                resultItem = invertBackground(resultItem);
            }
        }
        return resultItem;
    }

    private String addExampleResult(String resultItem, String resultToAddTo) {
        return addExampleResult(resultItem, resultToAddTo, false);
    }

    private String addExampleResult(String resultItem, String resultToAddTo, boolean showContexts) {
        if (!showContexts) {
            if (resultToAddTo.length() != 0) {
                resultToAddTo += exampleSeparatorSymbol;
            }
            resultToAddTo += resultItem;
        } else {
            resultToAddTo +=
                    exampleStartAutoSymbol
                            + resultItem
                            + exampleEndSymbol; // example in neutral context
            resultToAddTo +=
                    exampleStartRTLSymbol + resultItem + exampleEndSymbol; // example in RTL context
        }
        return resultToAddTo;
    }

    private String getUnitPattern(String unitType, final boolean isCurrency, Count count) {
        return cldrFile.getStringValue(
                isCurrency
                        ? "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/unitPattern"
                                + countAttribute(count)
                        : "//ldml/units/unit[@type=\""
                                + unitType
                                + "\"]/unitPattern"
                                + countAttribute(count));
    }

    private String getUnitName(String unitType, final boolean isCurrency, Count count) {
        return cldrFile.getStringValue(
                isCurrency
                        ? "//ldml/numbers/currencies/currency[@type=\""
                                + unitType
                                + "\"]/displayName"
                                + countAttribute(count)
                        : "//ldml/units/unit[@type=\""
                                + unitType
                                + "\"]/unitPattern"
                                + countAttribute(count));
    }

    public String countAttribute(Count count) {
        return "[@count=\"" + count + "\"]";
    }

    private void handleMinimumGrouping(XPathParts parts, String value, List<String> examples) {
        String numberSystem = cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
        String checkPath =
                "//ldml/numbers/decimalFormats[@numberSystem=\""
                        + numberSystem
                        + "\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String decimalFormat = cldrFile.getWinningValue(checkPath);
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(decimalFormat, numberSystem);
        numberFormat.setMinimumGroupingDigits(Integer.parseInt(value));

        double sampleNum1 = 543.21;
        double sampleNum2 = 6543.21;
        double sampleNum3 = 76543.21;
        examples.add(formatNumber(numberFormat, sampleNum1));
        examples.add(formatNumber(numberFormat, sampleNum2));
        examples.add(formatNumber(numberFormat, sampleNum3));
    }

    private void handleNumberSymbol(XPathParts parts, String value, List<String> examples) {
        String symbolType = parts.getElement(-1);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        int index; // dec/percent/sci
        double numberSample = NUMBER_SAMPLE;
        String originalValue = cldrFile.getWinningValue(parts.toString());
        boolean isSuperscripting = false;
        if (symbolType.equals("decimal") || symbolType.equals("group")) {
            index = 1;
        } else if (symbolType.equals("minusSign")) {
            index = 1;
            numberSample = -numberSample;
        } else if (symbolType.equals("percentSign")) {
            // For the perMille symbol, we reuse the percent example.
            index = 2;
            numberSample = 0.23;
        } else if (symbolType.equals("perMille")) {
            // For the perMille symbol, we reuse the percent example.
            index = 2;
            numberSample = 0.023;
            originalValue =
                    cldrFile.getWinningValue(parts.addRelative("../percentSign").toString());
        } else if (symbolType.equals("approximatelySign")) {
            // Substitute the approximately symbol in for the minus sign.
            index = 1;
            numberSample = -numberSample;
            originalValue = cldrFile.getWinningValue(parts.addRelative("../minusSign").toString());
        } else if (symbolType.equals("exponential") || symbolType.equals("plusSign")) {
            index = 3;
        } else if (symbolType.equals("superscriptingExponent")) {
            index = 3;
            isSuperscripting = true;
        } else {
            // We don't need examples for standalone symbols, i.e. infinity and nan.
            // We don't have an example for the list symbol either.
            return;
        }
        DecimalFormat x = icuServiceBuilder.getNumberFormat(index, numberSystem);
        String example;
        String formattedValue;
        if (isSuperscripting) {
            DecimalFormatSymbols symbols = x.getDecimalFormatSymbols();
            char[] digits = symbols.getDigits();
            x.setDecimalFormatSymbols(symbols);
            x.setNegativeSuffix(endSupSymbol + x.getNegativeSuffix());
            x.setPositiveSuffix(endSupSymbol + x.getPositiveSuffix());
            x.setExponentSignAlwaysShown(false);

            // Don't set the exponent directly because future examples for items
            // will be affected as well.
            originalValue = symbols.getExponentSeparator();
            formattedValue =
                    backgroundEndSymbol
                            + value
                            + digits[1]
                            + digits[0]
                            + backgroundStartSymbol
                            + startSupSymbol;
        } else {
            x.setExponentSignAlwaysShown(true);
            formattedValue = backgroundEndSymbol + value + backgroundStartSymbol;
        }
        example = x.format(numberSample);
        example = example.replace(originalValue, formattedValue);
        examples.add(backgroundStartSymbol + example + backgroundEndSymbol);
    }

    private void handleNumberingSystem(String value, List<String> examples) {
        NumberFormat x = icuServiceBuilder.getGenericNumberFormat(value);
        x.setGroupingUsed(false);
        examples.add(x.format(NUMBER_SAMPLE_WHOLE));
    }

    private void handleTimeZoneName(XPathParts parts, String value, List<String> examples) {
        String result = null;
        if (parts.contains("exemplarCity")) {
            // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity
            String timezone = parts.getAttributeValue(3, "type");
            String countryCode = supplementalDataInfo.getZone_territory(timezone);
            if (countryCode == null) {
                if (value == null) {
                    result = timezone.substring(timezone.lastIndexOf('/') + 1).replace('_', ' ');
                } else {
                    result = value; // trivial -- is this beneficial?
                }
                examples.add(result);
                return;
            }
            if (countryCode.equals("001")) {
                // GMT code, so format.
                try {
                    String hourOffset = timezone.substring(timezone.contains("+") ? 8 : 7);
                    int hours = Integer.parseInt(hourOffset);
                    result = getGMTFormat(null, null, hours);
                } catch (RuntimeException e) {
                    return; // fail, skip
                }
            } else {
                result =
                        setBackground(
                                cldrFile.nameGetter()
                                        .getNameFromTypeEnumCode(NameType.TERRITORY, countryCode));
            }
        } else if (parts.contains("zone")) { // {0} Time
            result = value; // trivial -- is this beneficial?
        } else if (parts.contains("regionFormat")) { // {0} Time
            result =
                    format(
                            value,
                            setBackground(
                                    cldrFile.nameGetter()
                                            .getNameFromTypeEnumCode(NameType.TERRITORY, "JP")));
            result =
                    addExampleResult(
                            format(
                                    value,
                                    setBackground(
                                            cldrFile.getWinningValue(EXEMPLAR_CITY_LOS_ANGELES))),
                            result);
        } else if (parts.contains("fallbackFormat")) { // {1} ({0})
            String central =
                    setBackground(
                            cldrFile.getWinningValue(
                                    "//ldml/dates/timeZoneNames/metazone[@type=\"America_Central\"]/long/generic"));
            String cancun =
                    setBackground(
                            cldrFile.getWinningValue(
                                    "//ldml/dates/timeZoneNames/zone[@type=\"America/Cancun\"]/exemplarCity"));
            result = format(value, cancun, central);
        } else if (parts.contains("gmtFormat")) { // GMT{0}
            result = getGMTFormat(null, value, -8);
        } else if (parts.contains("hourFormat")) { // +HH:mm;-HH:mm
            result = getGMTFormat(value, null, -8);
        } else if (parts.contains("metazone")
                && !parts.contains("commonlyUsed")) { // Metazone string
            if (value != null && value.length() > 0) {
                result = getMZTimeFormat() + " " + value;
            } else {
                // TODO check for value
                if (parts.contains("generic")) {
                    String metazone_name = parts.getAttributeValue(3, "type");
                    String timezone =
                            supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    String countryCode = supplementalDataInfo.getZone_territory(timezone);
                    String regionFormat =
                            cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat");
                    String countryName =
                            cldrFile.getWinningValue(
                                    "//ldml/localeDisplayNames/territories/territory[@type=\""
                                            + countryCode
                                            + "\"]");
                    result =
                            setBackground(
                                    getMZTimeFormat() + " " + format(regionFormat, countryName));
                } else {
                    String gmtFormat =
                            cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
                    String hourFormat =
                            cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
                    String metazone_name = parts.getAttributeValue(3, "type");
                    String tz_string =
                            supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    TimeZone currentZone = TimeZone.getTimeZone(tz_string);
                    int tzOffset = currentZone.getRawOffset();
                    if (parts.contains("daylight")) {
                        tzOffset += currentZone.getDSTSavings();
                    }
                    long tm_hrs = tzOffset / DateConstants.MILLIS_PER_HOUR;
                    long tm_mins =
                            (tzOffset % DateConstants.MILLIS_PER_HOUR)
                                    / DateConstants.MILLIS_PER_MINUTE;
                    result =
                            setBackground(
                                    getMZTimeFormat()
                                            + " "
                                            + getGMTFormat(
                                                    hourFormat,
                                                    gmtFormat,
                                                    (int) tm_hrs,
                                                    (int) tm_mins));
                }
            }
        }
        examples.add(result);
    }

    @SuppressWarnings("deprecation")
    private void handleDateFormatItem(
            String xpath, String value, boolean showContexts, List<String> examples) {
        // Get here if parts contains "calendar" and either of "pattern", "dateFormatItem"

        String fullpath = cldrFile.getFullXPath(xpath);
        XPathParts parts = XPathParts.getFrozenInstance(fullpath);
        String calendar = parts.findAttributeValue("calendar", "type");

        if (parts.contains("dateTimeFormat")) { // date-time combining patterns
            // ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"]
            // ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="atTime"]/pattern[@type="standard"]
            // ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="relative"]/pattern[@type="standard"]
            String formatType =
                    parts.findAttributeValue("dateTimeFormat", "type"); // "standard" or "atTime"
            String length =
                    parts.findAttributeValue(
                            "dateTimeFormatLength", "type"); // full, long, medium, short

            // For non-relative types, show
            // - date (of same length) with a single full time, or long time (abbreviated zone) if
            // the date is short
            // - date (of same length) with a single short time
            // For the standard patterns, add
            // - date (of same length) with a short time range
            // - relative date with a short time range
            // For the relative patterns, add
            // - relative date with a single short time

            // ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/pattern[@type="standard"]
            // ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/pattern[@type="standard"][@numbers="*"]
            SimpleDateFormat df = cldrFile.getDateFormat(calendar, length, icuServiceBuilder);
            df.setTimeZone(ZONE_SAMPLE);

            // ldml/dates/calendars/calendar[@type="*"]/timeFormats/timeFormatLength[@type="*"]/timeFormat[@type="standard"]/pattern[@type="standard"]
            // ldml/dates/calendars/calendar[@type="*"]/timeFormats/timeFormatLength[@type="*"]/timeFormat[@type="standard"]/pattern[@type="standard"][@numbers="*"] // not currently used
            SimpleDateFormat tlf =
                    (!length.equals("short"))
                            ? cldrFile.getTimeFormat(calendar, "full", icuServiceBuilder)
                            : cldrFile.getTimeFormat(calendar, "long", icuServiceBuilder);

            if (tlf == null) {
                return;
            }

            tlf.setTimeZone(ZONE_SAMPLE);

            SimpleDateFormat tsf = cldrFile.getTimeFormat(calendar, "short", icuServiceBuilder);
            tsf.setTimeZone(ZONE_SAMPLE);

            // ldml/dates/fields/field[@type="day"]/relative[@type="0"] // "today"
            String relativeDayXPath =
                    cldrFile.getWinningPath(
                            "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"0\"]");
            String relativeDayValue = cldrFile.getWinningValue(relativeDayXPath);

            String dfResult = df.format(DATE_SAMPLE);
            String tlfResult = tlf.format(DATE_SAMPLE);
            String tsfResult = tsf.format(DATE_SAMPLE); // DATE_SAMPLE is in the afternoon

            if (!formatType.contentEquals("relative")) {
                // Handle date plus a single full time.
                // We need to process the dateTimePattern as a date pattern (not only a message
                // format)
                // so
                // we handle it with SimpleDateFormat, plugging the date and time formats in as
                // literal
                // text.
                examples.add(
                        cldrFile.glueDateTimeFormatWithGluePattern(
                                setBackground(dfResult),
                                setBackground(tlfResult),
                                calendar,
                                value,
                                icuServiceBuilder));

                // Handle date plus a single short time.
                examples.add(
                        cldrFile.glueDateTimeFormatWithGluePattern(
                                setBackground(dfResult),
                                setBackground(tsfResult),
                                calendar,
                                value,
                                icuServiceBuilder));
            }
            if (formatType.contentEquals("standard")) {
                // Examples for standard pattern

                // Create a time range (from morning to afternoon, using short time formats). For
                // simplicity we format
                // using the intervalFormatFallback pattern (should be reasonable for time range
                // morning to evening).
                int dtfLengthOffset = xpath.indexOf("dateTimeFormatLength");
                if (dtfLengthOffset > 0) {
                    String intervalFormatFallbackXPath =
                            xpath.substring(0, dtfLengthOffset)
                                    .concat("intervalFormats/intervalFormatFallback");
                    String intervalFormatFallbackValue =
                            cldrFile.getWinningValue(intervalFormatFallbackXPath);
                    String tsfAMResult = tsf.format(DATE_SAMPLE3); // DATE_SAMPLE3 is in the morning
                    String timeRange = format(intervalFormatFallbackValue, tsfAMResult, tsfResult);

                    // Handle date plus short time range

                    examples.add(
                            cldrFile.glueDateTimeFormatWithGluePattern(
                                    setBackground(dfResult),
                                    setBackground(timeRange),
                                    calendar,
                                    value,
                                    icuServiceBuilder));

                    // Handle relative date plus short time range
                    examples.add(
                            cldrFile.glueDateTimeFormatWithGluePattern(
                                    setBackground(relativeDayValue),
                                    setBackground(timeRange),
                                    calendar,
                                    value,
                                    icuServiceBuilder));
                }
            }
            if (formatType.contentEquals("relative")) {
                // Examples for atTime pattern

                // Handle relative date plus a single short time.
                examples.add(
                        cldrFile.glueDateTimeFormatWithGluePattern(
                                setBackground(relativeDayValue),
                                setBackground(tsfResult),
                                calendar,
                                value,
                                icuServiceBuilder));
            }

            return;
        } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id) || value == null) {
                examples.add(startItalicSymbol + "n/a" + endItalicSymbol);
                return;
            } else {
                String numbersOverride = parts.findAttributeValue("pattern", "numbers");
                SimpleDateFormat sdf =
                        icuServiceBuilder.getDateFormat(calendar, value, numbersOverride);
                String defaultNumberingSystem =
                        cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
                String timeSeparator =
                        cldrFile.getWinningValue(
                                "//ldml/numbers/symbols[@numberSystem='"
                                        + defaultNumberingSystem
                                        + "']/timeSeparator");
                DateFormatSymbols dfs = sdf.getDateFormatSymbols();
                dfs.setTimeSeparatorString(timeSeparator);
                sdf.setDateFormatSymbols(dfs);
                if (id == null || id.indexOf('B') < 0) {
                    sdf.setTimeZone(ZONE_SAMPLE);
                    // Standard date/time format, or availableFormat without dayPeriod
                    if (value.contains("MMM") || value.contains("LLL")) {
                        // alpha month, do not need context examples
                        examples.add(sdf.format(DATE_SAMPLE));
                        addRelatedAvailable(parts, calendar, numbersOverride, dfs, examples);
                        return;
                    } else {
                        // Use contextExamples if showContexts T
                        String example =
                                showContexts
                                        ? exampleStartHeaderSymbol
                                                + contextheader
                                                + exampleEndSymbol
                                        : "";
                        String sup_twelve_example = sdf.format(DATE_SAMPLE);
                        String sub_ten_example = sdf.format(DATE_SAMPLE5);
                        example = addExampleResult(sup_twelve_example, example, showContexts);
                        if (!sup_twelve_example.equals(sub_ten_example)) {
                            example = addExampleResult(sub_ten_example, example, showContexts);
                        }
                        examples.add(example);
                        addRelatedAvailable(parts, calendar, numbersOverride, dfs, examples);
                        return;
                    }
                } else {
                    DayPeriodInfo dayPeriodInfo =
                            supplementalDataInfo.getDayPeriods(
                                    DayPeriodInfo.Type.format, cldrFile.getLocaleID());
                    Set<DayPeriod> dayPeriods = new LinkedHashSet<>(dayPeriodInfo.getPeriods());
                    for (DayPeriod dayPeriod : dayPeriods) {
                        if (dayPeriod.equals(
                                DayPeriod.midnight)) { // suppress midnight, see ICU-12278 bug
                            continue;
                        }
                        R3<Integer, Integer, Boolean> info =
                                dayPeriodInfo.getFirstDayPeriodInfo(dayPeriod);
                        if (info != null) {
                            int time =
                                    ((info.get0() + info.get1())
                                            / 2); // dayPeriod endpoints overlap, midpoint to
                            // disambiguate
                            String formatted = sdf.format(time);
                            examples.add(formatted);
                        }
                    }
                    return;
                }
            }
        }
    }

    private void addRelatedAvailable(
            XPathParts parts,
            String calendar,
            String numbersOverride,
            DateFormatSymbols dfs,
            List<String> examples) {
        Set<String> related = RelatedDatePathValues.getRelatedPathValues(cldrFile, parts);
        if (!related.isEmpty()) {
            examples.add("Related formats:");
            related.stream()
                    .forEach(
                            x -> {
                                SimpleDateFormat sdf2 =
                                        icuServiceBuilder.getDateFormat(
                                                calendar, x, numbersOverride);
                                sdf2.setDateFormatSymbols(dfs);
                                sdf2.setTimeZone(ZONE_SAMPLE);
                                String example2 = sdf2.format(DATE_SAMPLE);
                                if (!examples.contains(example2)) {
                                    examples.add(example2);
                                }
                            });
        }
    }

    // Simple check whether the currency symbol has letters on one or both sides
    private boolean symbolIsLetters(String currencySymbol, boolean onBothSides) {
        int len = currencySymbol.length();
        if (len == 0) {
            return false;
        }
        int limitChar = currencySymbol.codePointAt(0);
        if (UCharacter.isLetter(limitChar)) {
            if (!onBothSides) {
                return true;
            }
        } else if (onBothSides) {
            return false;
        }
        if (len > 1) {
            limitChar = currencySymbol.codePointAt(len - 1);
            return UCharacter.isLetter(limitChar);
        }
        return false;
    }

    /**
     * Creates examples for currency formats.
     *
     * @param value
     * @return
     */
    private void handleCurrencyFormat(
            XPathParts parts, String value, boolean showContexts, List<String> examples) {

        String example =
                showContexts ? exampleStartHeaderSymbol + contextheader + exampleEndSymbol : "";
        String territory = getDefaultTerritory();

        String currency = supplementalDataInfo.getDefaultCurrency(territory);
        String checkPath = "//ldml/numbers/currencies/currency[@type=\"" + currency + "\"]/symbol";
        String currencySymbol = cldrFile.getWinningValue(checkPath);
        String altValue = parts.getAttributeValue(-1, "alt");
        boolean altAlpha = (altValue != null && altValue.equals("alphaNextToNumber"));
        if (altAlpha && !symbolIsLetters(currencySymbol, true)) {
            // If this example is for alt="alphaNextToNumber" and the default currency symbol
            // does not have letters on both sides, need to use a fully alphabetic one.
            currencySymbol = currency;
        }

        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        DecimalFormat df =
                icuServiceBuilder.getCurrencyFormat(currency, currencySymbol, numberSystem);

        String typeValue = parts.getAttributeValue(-1, "type");
        boolean noCompact =
                typeValue != null && value.equals("0"); // A "0" value means that we don't compact
        int minIntegerDigits = typeValue.length();

        fixDecimalFormatForCompact(df, noCompact, value, currency, minIntegerDigits);

        // this is used for compact integers, to get the samples for that match the size and count

        String countValue = parts.getAttributeValue(-1, "count");
        if (countValue != null) {
            examples.add(formatCountDecimal(df, countValue));
            return;
        }

        double sampleAmount = 1295.00;
        example = addExampleResult(formatNumber(df, sampleAmount), example, showContexts);
        example = addExampleResult(formatNumber(df, -sampleAmount), example, showContexts);

        if (showContexts && !altAlpha) {
            // If this example is not for alt="alphaNextToNumber", then if the currency symbol
            // above has letters (strong dir) add another example with non-letter symbol
            // (weak or neutral), or vice versa
            if (symbolIsLetters(currencySymbol, false)) {
                currency = "EUR";
                checkPath = "//ldml/numbers/currencies/currency[@type=\"" + currency + "\"]/symbol";
                currencySymbol = cldrFile.getWinningValue(checkPath);
            } else {
                currencySymbol = currency;
            }
            df = icuServiceBuilder.getCurrencyFormat(currency, currencySymbol, numberSystem);
            df.applyPattern(value);
            example = addExampleResult(formatNumber(df, sampleAmount), example, showContexts);
            example = addExampleResult(formatNumber(df, -sampleAmount), example, showContexts);
        }

        examples.add(example);
    }

    private void fixDecimalFormatForCompact(
            DecimalFormat df,
            boolean noCompact,
            String value,
            String currency,
            int minIntegerDigits) {
        if (noCompact) {
            df.setMinimumIntegerDigits(minIntegerDigits);
        } else { // we do have a regular compact form
            df.applyPattern(value);

            // getCurrencyFormat sets digits, but applyPattern seems to overwrite it,
            // so fix it again here if there is a currency
            if (currency != null) {
                SupplementalDataInfo supplementalData = CONFIG.getSupplementalDataInfo();
                CurrencyNumberInfo info = supplementalData.getCurrencyNumberInfo(currency);
                int digits = info.getDigits();
                df.setMinimumFractionDigits(digits);
                df.setMaximumFractionDigits(digits);
            }
        }
    }

    private String getDefaultTerritory() {
        CLDRLocale loc;
        String territory = "US";
        if (!typeIsEnglish) {
            loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
            territory = loc.getCountry();
            if (territory == null || territory.length() == 0) {
                loc = supplementalDataInfo.getDefaultContentFromBase(loc);
                if (loc != null) {
                    territory = loc.getCountry();
                    if (territory.equals("001") && loc.getLanguage().equals("ar")) {
                        territory =
                                "EG"; // Use Egypt as territory for examples in ar locale, since its
                        // default content is ar_001.
                    }
                }
            }
            if (territory == null || territory.length() == 0) {
                territory = "US";
            }
        }
        return territory;
    }

    /**
     * Creates examples for decimal formats.
     *
     * @param value
     * @return
     */
    private void handleDecimalFormat(
            XPathParts parts, String value, boolean showContexts, List<String> examples) {
        String example =
                showContexts ? exampleStartHeaderSymbol + contextheader + exampleEndSymbol : "";
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        String countValue = parts.getAttributeValue(-1, "count");

        String typeValue = parts.getAttributeValue(-1, "type");
        boolean noCompact =
                typeValue != null && value.equals("0"); // A "0" value means that we don't compact
        int minIntegerDigits = typeValue.length();

        DecimalFormat numberFormat =
                noCompact
                        ? icuServiceBuilder.getNumberFormat(ICUServiceBuilder.integer, numberSystem)
                        : icuServiceBuilder.getNumberFormat(value, numberSystem);

        fixDecimalFormatForCompact(numberFormat, noCompact, value, null, minIntegerDigits);

        // this is used for compact integers, to get the samples for that size

        if (countValue != null) {
            examples.add(formatCountDecimal(numberFormat, countValue));
            return;
        }

        double sampleNum1 = 5.43;
        double sampleNum2 = NUMBER_SAMPLE;
        if (parts.getElement(4).equals("percentFormat")) {
            sampleNum1 = 0.0543;
        }
        example = addExampleResult(formatNumber(numberFormat, sampleNum1), example, showContexts);
        example = addExampleResult(formatNumber(numberFormat, sampleNum2), example, showContexts);
        // have positive and negative
        example = addExampleResult(formatNumber(numberFormat, -sampleNum2), example, showContexts);
        examples.add(example);
    }

    /**
     * Creates examples for decimal formats.
     *
     * @param value
     * @return
     */
    private void handleRationalFormat(
            XPathParts parts, String value, boolean showContexts, List<String> examples) {
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        boolean isLatin = numberSystem == null || numberSystem.equals("latn");
        DecimalFormat numberFormat =
                isLatin ? null : icuServiceBuilder.getNumberFormat("0", numberSystem);

        String element = parts.getElement(-1);
        String num;
        String den;

        switch (element) {
            case "rationalPattern":
                Pair<String, String> simpleFractionPair = null;
                List<Pair<String, String>> fractionPairs = new ArrayList<>();
                Pair<String, String> extraPair = null;
                if (isLatin) {
                    num = "1";
                    den = "2";
                    extraPair = Pair.of("¹", "₂");
                } else {
                    num = numberFormat.format(1);
                    den = numberFormat.format(2);
                }
                simpleFractionPair = Pair.of(num, den);
                fractionPairs.add(simpleFractionPair);
                fractionPairs.add(
                        Pair.of(
                                startSupSymbol + num + endSupSymbol,
                                startSubSymbol + den + endSubSymbol));
                if (extraPair != null) {
                    fractionPairs.add(extraPair);
                }

                // for the simple case, we add an example with an isolated fraction slash, to show
                // what "plain" numbers would look like.
                examples.add(
                        value.replace(FSLASH, ISOLATE_FSLASH)
                                .replace("{0}", setBackground(simpleFractionPair.getFirst()))
                                .replace("{1}", setBackground(simpleFractionPair.getSecond())));

                // We then add all of them without isolating the fraction slash
                for (Pair<String, String> pair : fractionPairs) {
                    examples.add(
                            value.replace("{0}", setBackground(pair.getFirst()))
                                    .replace("{1}", setBackground(pair.getSecond())));
                }
                break;
            case "integerAndRationalPattern":
                boolean superSub =
                        "superSub"
                                .equals(parts.getAttributeValue(-1, "alt")); // null if not present
                // get rationalPattern
                XPathParts parts2 =
                        parts.cloneAsThawed()
                                .setElement(-1, "rationalPattern")
                                .setAttribute(-1, "alt", null);
                String rationalPart = cldrFile.getStringValue(parts2.toString());
                Set<String> fractions = new LinkedHashSet<>();
                String base;
                String extra = null;
                if (isLatin) {
                    base = "3";
                    num = "1";
                    den = "2";
                    extra = "½";
                } else {
                    base = numberFormat.format(3);
                    num = numberFormat.format(1);
                    den = numberFormat.format(2);
                }
                if (!superSub) {
                    // put WG around elements so that the fraction slash doesn't change their
                    // formatting
                    fractions.add(
                            rationalPart
                                    .replace(FSLASH, ISOLATE_FSLASH)
                                    .replace("{0}", num)
                                    .replace("{1}", den));
                }
                if (extra != null) {
                    fractions.add(extra);
                }
                fractions.add(
                        startSupSymbol
                                + num
                                + endSupSymbol
                                + FSLASH
                                + startSubSymbol
                                + den
                                + endSubSymbol);

                for (String r : fractions) {
                    examples.add(
                            value.replace("{0}", setBackground(base))
                                    .replace("{1}", setBackground(r)));
                }
                break;
            case "rationalUsage":
                return;
        }
    }

    /**
     * The number of digits is based on the minimum integer digits in the format
     *
     * @param numberFormat
     * @param countValue
     * @return
     */
    private String formatCountDecimal(DecimalFormat numberFormat, String countValue) {
        Count count;
        try {
            count = Count.valueOf(countValue);
        } catch (Exception e) {
            String locale = getCldrFile().getLocaleID();
            PluralInfo pluralInfo = supplementalDataInfo.getPlurals(locale);
            count =
                    pluralInfo.getCount(
                            DecimalQuantity_DualStorageBCD.fromExponentString(countValue));
        }
        // The number of digits is based on the minimum integer digits
        Double numberSample = getExampleForPattern(numberFormat, count);
        if (numberSample == null) {
            // Ideally, we would suppress the value in the survey tool.
            // However, until we switch over to the ICU samples, we are not guaranteed
            // that "no samples" means "can't occur". So we manufacture something.
            // int digits = numberFormat.getMinimumIntegerDigits();
            numberSample = 0d;
        }
        String temp = String.valueOf(numberSample);
        int fractionLength = temp.endsWith(".0") ? 0 : temp.length() - temp.indexOf('.') - 1;
        if (fractionLength != numberFormat.getMaximumFractionDigits()) {
            numberFormat = (DecimalFormat) numberFormat.clone(); // for safety
            numberFormat.setMinimumFractionDigits(fractionLength);
            numberFormat.setMaximumFractionDigits(fractionLength);
        }
        return formatNumber(numberFormat, numberSample);
    }

    private String formatNumber(DecimalFormat format, double value) {
        String example = format.format(value);
        return setBackgroundOnMatch(example, ALL_DIGITS);
    }

    /**
     * Calculates a numerical example to use for the specified pattern using brute force (there
     * should be a more elegant way to do this). The number of digits is
     * format.getMinimumIntegerDigits()
     *
     * @param format
     * @param count
     * @return
     */
    private Double getExampleForPattern(DecimalFormat format, Count count) {
        if (patternExamples == null) {
            patternExamples = PluralSamples.getInstance(cldrFile.getLocaleID());
        }
        int numDigits = format.getMinimumIntegerDigits();
        Map<Count, Double> samples = patternExamples.getSamples(numDigits);
        if (samples == null) {
            return null;
        }
        return samples.get(count);
    }

    private void handleCurrency(
            String xpath, XPathParts parts, String value, List<String> examples) {
        String currency = parts.getAttributeValue(-2, "type");
        String fullPath = cldrFile.getFullXPath(xpath, false);
        if (parts.contains("symbol")) {
            if (fullPath != null && fullPath.contains("[@choice=\"true\"]")) {
                ChoiceFormat cf = new ChoiceFormat(value);
                value = cf.format(NUMBER_SAMPLE);
            }
            String result;
            if (value == null) {
                throw new NullPointerException(
                        cldrFile.getSourceLocation(fullPath)
                                + ": "
                                + cldrFile.getLocaleID()
                                + ": "
                                + ": Error: no currency symbol for "
                                + currency);
            }
            DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency, value);
            result = x.format(NUMBER_SAMPLE);
            result =
                    setBackground(result)
                            .replace(value, backgroundEndSymbol + value + backgroundStartSymbol);
            examples.add(result);
        } else if (parts.contains("displayName")) {
            formatCountValue(xpath, parts, value, examples);
        }
        return;
    }

    private void handleDateRangePattern(String value, List<String> examples) {
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        examples.add(
                format(
                        value,
                        setBackground(dateFormat.format(DATE_SAMPLE)),
                        setBackground(dateFormat.format(DATE_SAMPLE2))));
    }

    /** Add examples for eras. Generates a sample date based on this info and formats it */
    private void handleEras(XPathParts parts, String value, List<String> examples) {
        String calendarId = parts.getAttributeValue(3, "type");
        String type = parts.getAttributeValue(-1, "type");
        String id = calendarId;
        if (id.equals("generic") || id.equals("iso8601")) {
            id = "gregorian"; // use Gregorian eras, 'generic' is not in the data
        }
        final SupplementalCalendarData.CalendarData calendarData =
                supplementalDataInfo.getCalendarData().get(id);

        if (calendarData == null) {
            throw new IllegalArgumentException("Could not load supplementalCalendarData for " + id);
        }
        final int typeIndex = Integer.parseInt(type);

        final SupplementalCalendarData.EraData eraData = calendarData.get(typeIndex);
        if (eraData == null) {
            return; // no era data
        }
        GregorianCalendar startCal = eraData.getStartCalendar();
        GregorianCalendar endCal = eraData.getEndCalendar();

        final SupplementalCalendarData.EraData eminusone = calendarData.get(typeIndex - 1);
        final SupplementalCalendarData.EraData eplusone = calendarData.get(typeIndex + 1);

        SupplementalCalendarData.EraData prevEra = null;
        SupplementalCalendarData.EraData nextEra = null;

        // see if we can find the 'prev' and 'next' era by date
        if (eminusone != null && eminusone.compareTo(eraData) < 0) {
            prevEra = eminusone;
        } else if (eplusone != null && eplusone.compareTo(eraData) < 0) {
            prevEra = eplusone;
        }
        if (eminusone != null && eminusone.compareTo(eraData) > 0) {
            nextEra = eminusone;
        } else if (eplusone != null && eplusone.compareTo(eraData) > 0) {
            nextEra = eplusone;
        }

        if (startCal == null && prevEra != null && prevEra.getEnd() != null) {
            startCal = prevEra.getEndCalendar();
            // shift forward so we are in the next era
            startCal.setTimeInMillis(startCal.getTimeInMillis() + (DateConstants.MILLIS_PER_DAY));
        }
        if (endCal == null && nextEra != null && nextEra.getStart() != null) {
            endCal = nextEra.getStartCalendar();
            // shift backward so we are in the prev era
            endCal.setTimeInMillis(endCal.getTimeInMillis() - (DateConstants.MILLIS_PER_DAY));
        }

        GregorianCalendar sampleDate = null;

        if (startCal != null && endCal != null) {
            // roll back a day to not hit the edge
            sampleDate = endCal;
            sampleDate.setTimeInMillis(
                    sampleDate.getTimeInMillis() - (DateConstants.MILLIS_PER_DAY));
        } else if (startCal == null && endCal != null) {
            // roll back a day to not hit the edge
            sampleDate = endCal;
            sampleDate.setTimeInMillis(
                    sampleDate.getTimeInMillis() - (DateConstants.MILLIS_PER_DAY));
        } else if (startCal != null && endCal == null) {
            sampleDate = new GregorianCalendar(2002, 6, 15); // CLDR repo root commit
            if (sampleDate.before(startCal)) {
                sampleDate = startCal;
                sampleDate.setTimeInMillis(
                        sampleDate.getTimeInMillis() + (DateConstants.MILLIS_PER_DAY));
            }
        } else {
            // System.err.println("No good date for " + eraData);
            // TODO: should be an error in TestSupplementalDataInfo
            sampleDate = null;
        }

        if (sampleDate == null) return; // could not find the time

        final Date sample = sampleDate.getTime();

        String skeleton = "Gy";
        String checkPath =
                "//ldml/dates/calendars/calendar[@type=\""
                        + calendarId
                        + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                        + skeleton
                        + "\"]";
        String dateFormat = cldrFile.getWinningValue(checkPath);
        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendarId, dateFormat);
        DateFormatSymbols dfs = sdf.getDateFormatSymbols();
        String[] eraNames = dfs.getEras();
        eraNames[typeIndex] = value; // overwrite era to current value
        dfs.setEras(eraNames);
        sdf.setDateFormatSymbols(dfs);
        examples.add(sdf.format(sample));
    }

    /**
     * Add examples for quarters for the gregorian calendar, matching each quarter type (1, 2, 3, 4)
     * to a corresponding sample month and formatting an example with that date
     */
    void handleQuarters(XPathParts parts, String value, List<String> examples) {
        String calendarId = parts.getAttributeValue(3, "type");
        if (!calendarId.equals("gregorian")) {
            return;
        }
        String width = parts.findAttributeValue("quarterWidth", "type");
        if (width.equals("narrow")) {
            return;
        }
        String context = parts.findAttributeValue("quarterContext", "type");
        String type = parts.getAttributeValue(-1, "type"); // 1-indexed
        int quarterIndex = Integer.parseInt(type) - 1;
        String skeleton = (width.equals("wide")) ? "yQQQQ" : "yQQQ";
        String checkPath =
                "//ldml/dates/calendars/calendar[@type=\""
                        + calendarId
                        + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                        + skeleton
                        + "\"]";
        String dateFormat = cldrFile.getWinningValue(checkPath);
        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendarId, dateFormat);
        DateFormatSymbols dfs = sdf.getDateFormatSymbols();
        int widthVal = width.equals("abbreviated") ? 0 : 1;
        String[] quarterNames = dfs.getQuarters(0, widthVal); // 0 for formatting
        quarterNames[quarterIndex] = value;
        dfs.setQuarters(quarterNames, 0, widthVal);
        sdf.setDateFormatSymbols(dfs);
        sdf.setTimeZone(ZONE_SAMPLE);
        final int[] monthSamples = new int[] {1, 4, 7, 10}; // {feb, may, oct, nov}
        int month = monthSamples[quarterIndex];
        calendar.set(1999, month, 5, 13, 25, 59);
        Date sample = calendar.getTime();
        examples.add(sdf.format(sample));
    }

    /* Add relative date/time examples, choosing appropriate
     * patterns as needed for relative dates vs relative times.
     * Additionally, for relativeTimePattern items, ensure that
     * numeric example corresponds to the count represented by the item.
     */
    private void handleRelative(
            String xpath, XPathParts parts, String value, List<String> examples) {
        String skeleton;
        String type = parts.findAttributeValue("field", "type");
        if (type.startsWith("hour")) {
            skeleton = "Hm";
        } else if (type.startsWith("minute") || type.startsWith("second")) {
            skeleton = "ms";
        } else if (type.startsWith("year")
                || type.startsWith("month")
                || type.startsWith("quarter")) {
            skeleton = "yMMMM";
        } else {
            skeleton = "MMMMd";
        }
        String availableFormatPath =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                        + skeleton
                        + "\"]";
        String dateFormat = cldrFile.getWinningValue(availableFormatPath);
        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat("gregorian", dateFormat);
        String sampleDate = sdf.format(DATE_SAMPLE);

        String contextTransformPath =
                "//ldml/contextTransforms/contextTransformUsage[@type=\"relative\"]/contextTransform[@type=\"stand-alone\"]";
        String contextTransformValue = cldrFile.getWinningValue(contextTransformPath);
        String example1 = sampleDate + " (" + value + ")"; // value in middle-of-sentence usage
        if (contextTransformValue != null && contextTransformValue.equals("titlecase-firstword")) {
            value =
                    TITLECASE.apply(
                            Locale.forLanguageTag(getCldrFile().getLocaleID()),
                            null,
                            value); // locale-sensitive titlecasing
        }
        String example2 =
                value
                        + " ("
                        + sampleDate
                        + ")"; // value in stand-alone usage, titlecasing per contextTransform

        examples.add("Set letter case for top example:");
        if (parts.contains("relativeTimePattern")) { // has placeholder
            String count = parts.getAttributeValue(-1, "count");
            // Pick an appropriate example for this count, depends on the locale's plural rules
            List<String> exampleCounts = COUNTS.get(count);
            String exampleCount = exampleCounts.get(0); // default example for count
            // if default example, does not work for the locale, override below
            if (exampleCounts.size() > 1) {
                int exampleCountSize = exampleCounts.size();
                DecimalQuantitySamples samples =
                        pluralInfo.getPluralRules().getDecimalSamples(count, SampleType.INTEGER);
                if (samples == null) {
                    // this locale has no integer samples for this count so use the decimal fraction
                    // example at the end of the list
                    exampleCount = exampleCounts.get(exampleCountSize - 1);
                } else {
                    Map<Count, String> countToStringExamplesMap =
                            pluralInfo.getCountToStringExamplesMap();
                    String stringExamples = countToStringExamplesMap.get(Count.valueOf(count));
                    // skip the default value already set
                    for (int i = 1; i < exampleCountSize; i++) {
                        String exampleCountTest = exampleCounts.get(i);
                        if (stringExamples.contains(exampleCountTest)) {
                            exampleCount = exampleCountTest;
                            break;
                        }
                    }
                }
            }
            if (!exampleCount.contains(".")) {
                DecimalFormat df = icuServiceBuilder.getNumberFormat("0");
                exampleCount = df.format(Integer.parseInt(exampleCount));
            } else {
                DecimalFormat df = icuServiceBuilder.getNumberFormat("0.0");
                exampleCount = df.format(Double.parseDouble(exampleCount));
            }
            examples.add(invertBackground(format(setBackground(example1), exampleCount)));
            examples.add(invertBackground(format(setBackground(example2), exampleCount)));
        } else {
            examples.add(format(example1));
            examples.add(format(example2));
        }
        examples.add("See letter case instructions at right.");
    }

    /**
     * @param elementToOverride the element that is to be overridden
     * @param element the overriding element
     * @param value the value to override element with
     * @return
     */
    private String getLocaleDisplayPattern(String elementToOverride, String element, String value) {
        final String localeDisplayPatternPath = "//ldml/localeDisplayNames/localeDisplayPattern/";
        if (elementToOverride.equals(element)) {
            return value;
        } else {
            return cldrFile.getWinningValue(localeDisplayPatternPath + elementToOverride);
        }
    }

    private void handleDisplayNames(
            String xpath, XPathParts parts, String value, List<String> examples) {
        String result = null;
        if (parts.contains("codePatterns")) {
            // ldml/localeDisplayNames/codePatterns/codePattern[@type="language"]
            // ldml/localeDisplayNames/codePatterns/codePattern[@type="script"]
            // ldml/localeDisplayNames/codePatterns/codePattern[@type="territory"]
            String type = parts.getAttributeValue(-1, "type");
            result =
                    format(
                            value,
                            setBackground(
                                    type.equals("language")
                                            ? "ace"
                                            : type.equals("script")
                                                    ? "Avst"
                                                    : type.equals("territory") ? "057" : "CODE"));
            examples.add(result);
            return;
        } else if (parts.contains("localeDisplayPattern")) {
            // ldml/localeDisplayNames/localeDisplayPattern/localePattern
            // ldml/localeDisplayNames/localeDisplayPattern/localeSeparator
            // ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern
            String element = parts.getElement(-1);
            value = setBackground(value);
            String localeKeyTypePattern =
                    getLocaleDisplayPattern("localeKeyTypePattern", element, value);
            String localePattern = getLocaleDisplayPattern("localePattern", element, value);
            String localeSeparator = getLocaleDisplayPattern("localeSeparator", element, value);
            List<String> locales = new ArrayList<>();
            if (element.equals("localePattern")) {
                locales.add("uz-AF");
            }
            locales.add(
                    element.equals("localeKeyTypePattern") ? "uz-Arab-u-tz-etadd" : "uz-Arab-AF");
            locales.add("uz-Arab-AF-u-tz-etadd-nu-arab");
            // String[] examples = new String[locales.size()];
            NameGetter nameGetter = cldrFile.nameGetter();
            for (int i = 0; i < locales.size(); i++) {
                examples.add(
                        invertBackground(
                                nameGetter.getNameFromIdentifierEtc(
                                        locales.get(i),
                                        NameGetter.NameOpt.DEFAULT,
                                        localeKeyTypePattern,
                                        localePattern,
                                        localeSeparator)));
            }
            return;
        } else if (parts.contains("languages")
                || parts.contains("scripts")
                || parts.contains("territories")) {
            // ldml/localeDisplayNames/languages/language[@type="ar"]
            // ldml/localeDisplayNames/scripts/script[@type="Arab"]
            // ldml/localeDisplayNames/territories/territory[@type="CA"]
            String type = parts.getAttributeValue(-1, "type");
            if (type.contains("_")) {
                if (value != null && !value.equals(type)) {
                    result = value; // trivial -- is this beneficial?
                } else {
                    result = cldrFile.getBaileyValue(xpath, null, null);
                }
                examples.add(result);
                return;
            } else {
                value = setBackground(value);

                String menuAttr = parts.getAttributeValue(-1, "menu");
                if (menuAttr != null) { // show core plus extension
                    String core, extension;
                    XPathParts other = parts.cloneAsThawed();
                    switch (menuAttr) {
                        case "core":
                            core = value;
                            extension =
                                    cldrFile.getStringValue(
                                            other.setAttribute(-1, "menu", "extension").toString());
                            break;
                        default:
                            core =
                                    cldrFile.getStringValue(
                                            other.setAttribute(-1, "menu", "core").toString());
                            extension = value;
                            break;
                    }
                    String localePattern =
                            getCldrFile()
                                    .getStringValue(
                                            "//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
                    examples.add(
                            invertBackground(MessageFormat.format(localePattern, core, extension)));
                    return;
                }

                String nameType = parts.getElement(3);

                Map<String, String> likely = supplementalDataInfo.getLikelySubtags();
                String alt = parts.getAttributeValue(-1, "alt");
                boolean isStandAloneValue = "stand-alone".equals(alt);
                if (!isStandAloneValue) {
                    // only do this if the value is not a stand-alone form
                    String tag = "language".equals(nameType) ? type : "und_" + type;
                    String max = LikelySubtags.maximize(tag, likely);
                    if (max == null) {
                        return;
                    }
                    LanguageTagParser ltp = new LanguageTagParser().set(max);
                    String languageName = null;
                    String scriptName = null;
                    String territoryName = null;
                    if (nameType.equals("language")) {
                        languageName = value;
                    } else if (nameType.equals("script")) {
                        scriptName = value;
                    } else {
                        territoryName = value;
                    }
                    if (languageName == null) {
                        languageName =
                                cldrFile.getStringValueWithBailey(
                                        NameType.LANGUAGE.getKeyPath(ltp.getLanguage()));
                        if (languageName == null) {
                            languageName =
                                    cldrFile.getStringValueWithBailey(
                                            NameType.LANGUAGE.getKeyPath("en"));
                        }
                        if (languageName == null) {
                            languageName = ltp.getLanguage();
                        }
                    }
                    if (scriptName == null) {
                        scriptName =
                                cldrFile.getStringValueWithBailey(
                                        NameType.SCRIPT.getKeyPath(ltp.getScript()));
                        if (scriptName == null) {
                            scriptName =
                                    cldrFile.getStringValueWithBailey(
                                            NameType.SCRIPT.getKeyPath("Latn"));
                        }
                        if (scriptName == null) {
                            scriptName = ltp.getScript();
                        }
                    }
                    if (territoryName == null) {
                        territoryName =
                                cldrFile.getStringValueWithBailey(
                                        NameType.TERRITORY.getKeyPath(ltp.getRegion()));
                        if (territoryName == null) {
                            territoryName =
                                    cldrFile.getStringValueWithBailey(
                                            NameType.TERRITORY.getKeyPath("US"));
                        }
                        if (territoryName == null) {
                            territoryName = ltp.getRegion();
                        }
                    }
                    languageName =
                            languageName
                                    .replace('(', '[')
                                    .replace(')', ']')
                                    .replace('（', '［')
                                    .replace('）', '］');
                    scriptName =
                            scriptName
                                    .replace('(', '[')
                                    .replace(')', ']')
                                    .replace('（', '［')
                                    .replace('）', '］');
                    territoryName =
                            territoryName
                                    .replace('(', '[')
                                    .replace(')', ']')
                                    .replace('（', '［')
                                    .replace('）', '］');

                    String localePattern =
                            cldrFile.getStringValueWithBailey(
                                    "//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
                    String localeSeparator =
                            cldrFile.getStringValueWithBailey(
                                    "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator");
                    String scriptTerritory = format(localeSeparator, scriptName, territoryName);
                    if (!nameType.equals("script")) {
                        examples.add(
                                invertBackground(
                                        format(localePattern, languageName, territoryName)));
                    }
                    if (!nameType.equals("territory")) {
                        examples.add(
                                invertBackground(format(localePattern, languageName, scriptName)));
                    }
                    examples.add(
                            invertBackground(format(localePattern, languageName, scriptTerritory)));
                }
                Output<String> pathWhereFound;
                if (isStandAloneValue
                        || cldrFile.getStringValueWithBailey(
                                        xpath + ALT_STAND_ALONE,
                                        pathWhereFound = new Output<>(),
                                        null)
                                == null
                        || !pathWhereFound.value.contains(ALT_STAND_ALONE)) {
                    // only do this if either it is a stand-alone form,
                    // or it isn't and there is no separate stand-alone form
                    // the extra check after the == null is to make sure that we don't have sideways
                    // inheritance
                    String codePattern =
                            cldrFile.getStringValueWithBailey(
                                    "//ldml/localeDisplayNames/codePatterns/codePattern[@type=\""
                                            + nameType
                                            + "\"]");
                    examples.add(invertBackground(format(codePattern, value)));
                }
                return;
            }
        } else if (parts.getElement(-1).equals("type")) {
            // <keys><key type="collation">Calendar</key>
            // <types><type key="collation" type="dictionary">Dictionary Sort Order</type>
            // <types><type key="collation" type="dictionary" scope="core">Dictionary</type>
            String kpath = "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
            String ktppath = "//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern";
            String ktpath =
                    "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"dictionary\"]";
            String ktspath =
                    "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"dictionary\"][@scope=\"core\"]";
            String key = parts.getAttributeValue(-1, "key");
            String type = parts.getAttributeValue(-1, "type");
            String scope = parts.getAttributeValue(-1, "scope");

            String keyPath = kpath.replace("collation", key);
            if (scope == null) {
                // TBD, show contrast
            } else {
                String keyName = getCldrFile().getStringValue(keyPath);
                examples.add(setBackground(keyName));
                examples.add(setBackground("   others…"));
                examples.add(setBackground("   ") + value);
                examples.add(setBackground("   …others"));
                String keyTypePattern = getCldrFile().getStringValue(ktppath);
                String combined =
                        invertBackground(
                                MessageFormat.format(
                                        keyTypePattern, keyName, setBackground(value)));
                examples.add(combined);
            }
        }
    }

    private String formatExampleList(String[] examples) {
        String result = examples[0];
        for (int i = 1, len = examples.length; i < len; i++) {
            result = addExampleResult(examples[i], result);
        }
        return result;
    }

    /**
     * Return examples formatted as string, with null returned for null or empty examples.
     *
     * @param examples
     * @return
     */
    public String formatExampleList(Collection<String> examples) {
        if (examples == null || examples.isEmpty()) {
            return null;
        }
        String result = "";
        boolean first = true;
        for (String example : examples) {
            if (first) {
                result = example;
                first = false;
            } else {
                result = addExampleResult(example, result);
            }
        }
        return result;
    }

    public static String format(String format, Object... objects) {
        if (format == null) return null;
        return MessageFormat.format(format, objects);
    }

    public static String unchainException(Exception e) {
        String stackStr = "[unknown stack]<br>";
        try {
            StringWriter asString = new StringWriter();
            e.printStackTrace(new PrintWriter(asString));
            stackStr = "<pre>" + asString + "</pre>";
        } catch (Throwable tt) {
            // ...
        }
        return stackStr;
    }

    /**
     * Put a background on an item, skipping enclosed patterns.
     *
     * @param inputPattern
     * @return
     */
    private String setBackground(String inputPattern) {
        if (inputPattern == null) {
            return "?";
        }
        Matcher m = PARAMETER.matcher(inputPattern);
        return backgroundStartSymbol
                + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
                + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     *
     * @param input
     * @param patternToEmbed
     * @return
     */
    private String setBackgroundExceptMatch(String input, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(input);
        return backgroundStartSymbol
                + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
                + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     *
     * @param inputPattern
     * @param patternToEmbed
     * @return
     */
    private String setBackgroundOnMatch(String inputPattern, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(inputPattern);
        return m.replaceAll(backgroundStartSymbol + "$1" + backgroundEndSymbol);
    }

    /**
     * This is called just before we return a result. It fixes the special characters that were
     * added by setBackground.
     *
     * @param input string with special characters from setBackground.
     * @return string with HTML for the background.
     */
    private String finalizeBackground(String input) {
        if (input == null) {
            return null;
        }
        String coreString =
                TransliteratorUtilities.toHTML
                        .transliterate(input)
                        .replace(backgroundStartSymbol + backgroundEndSymbol, "")
                        // remove null runs
                        .replace(backgroundEndSymbol + backgroundStartSymbol, "")
                        // remove null runs
                        .replace(backgroundStartSymbol, backgroundStart)
                        .replace(backgroundEndSymbol, backgroundEnd)
                        .replace(backgroundAutoStartSymbol, backgroundAutoStart)
                        .replace(backgroundAutoEndSymbol, backgroundAutoEnd)
                        .replace(exampleSeparatorSymbol, exampleEnd + exampleStart)
                        .replace(exampleStartAutoSymbol, exampleStartAuto)
                        .replace(exampleStartRTLSymbol, exampleStartRTL)
                        .replace(exampleStartHeaderSymbol, exampleStartHeader)
                        .replace(exampleEndSymbol, exampleEnd)
                        .replace(startItalicSymbol, startItalic)
                        .replace(endItalicSymbol, endItalic)
                        .replace(startSupSymbol, startSup)
                        .replace(endSupSymbol, endSup)
                        .replace(startSubSymbol, startSub)
                        .replace(endSubSymbol, endSub);
        // If we are not showing context, we use exampleSeparatorSymbol between examples,
        // and then need to add the initial exampleStart and final exampleEnd.
        return (input.contains(exampleStartAutoSymbol))
                ? coreString
                : exampleStart + coreString + exampleEnd;
    }

    private String invertBackground(String input) {
        return input == null
                ? null
                : backgroundStartSymbol
                        + input.replace(backgroundStartSymbol, backgroundTempSymbol)
                                .replace(backgroundEndSymbol, backgroundStartSymbol)
                                .replace(backgroundTempSymbol, backgroundEndSymbol)
                        + backgroundEndSymbol;
    }

    private String removeEmptyRuns(String input) {
        return input.replace(backgroundStartSymbol + backgroundEndSymbol, "")
                .replace(backgroundEndSymbol + backgroundStartSymbol, "");
    }

    /**
     * Utility to format using a gmtHourString, gmtFormat, and an integer hours. We only need the
     * hours because that's all the TZDB IDs need. Should merge this eventually into
     * TimeZoneFormatter and call there.
     *
     * @param gmtHourString
     * @param gmtFormat
     * @param hours
     * @return
     */
    private String getGMTFormat(String gmtHourString, String gmtFormat, int hours) {
        return getGMTFormat(gmtHourString, gmtFormat, hours, 0);
    }

    private String getGMTFormat(String gmtHourString, String gmtFormat, int hours, int minutes) {
        boolean hoursBackground = false;
        if (gmtHourString == null) {
            hoursBackground = true;
            gmtHourString = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
        }
        if (gmtFormat == null) {
            hoursBackground = false; // for the hours case
            gmtFormat =
                    setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat"));
        }
        String[] plusMinus = gmtHourString.split(";");

        SimpleDateFormat dateFormat =
                icuServiceBuilder.getDateFormat("gregorian", plusMinus[hours >= 0 ? 0 : 1]);
        dateFormat.setTimeZone(ZONE_SAMPLE);
        calendar.set(1999, 9, 27, Math.abs(hours), minutes, 0); // 1999-09-13 13:25:59
        Date sample = calendar.getTime();
        String hourString = dateFormat.format(sample);
        if (hoursBackground) {
            hourString = setBackground(hourString);
        }
        String result = format(gmtFormat, hourString);
        return result;
    }

    private String getMZTimeFormat() {
        String timeFormat =
                cldrFile.getWinningValue(
                        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
        if (timeFormat == null) {
            timeFormat = "HH:mm";
        }
        // the following is <= because the TZDB inverts the hours
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", timeFormat);
        dateFormat.setTimeZone(ZONE_SAMPLE);
        calendar.set(1999, 9, 13, 13, 25, 59); // 1999-09-13 13:25:59
        Date sample = calendar.getTime();
        String result = dateFormat.format(sample);
        return result;
    }

    /**
     * Return a help string, in html, that should be shown in the Zoomed view. Presumably at the end
     * of each help section is something like: <br>
     * &lt;br&gt;For more information, see <a
     * href='http://unicode.org/cldr/wiki?SurveyToolHelp/characters'>help</a>. <br>
     * The result is valid HTML. Set listPlaceholders to true to include a HTML-formatted table of
     * all placeholders required in the value.<br>
     * TODO: add more help, and modify to get from property or xml file for easy modification.
     *
     * @return null if none available.
     */
    public synchronized String getHelpHtml(String xpath, String value) {
        // lazy initialization
        if (pathDescription == null) {
            Map<String, List<Set<String>>> starredPaths = new HashMap<>();
            Map<String, String> extras = new HashMap<>();

            this.pathDescription =
                    new PathDescription(
                            supplementalDataInfo,
                            englishFile,
                            extras,
                            starredPaths,
                            PathDescription.ErrorHandling.CONTINUE);

            if (helpMessages == null) {
                helpMessages = new HelpMessages("test_help_messages.html");
            }
        }

        // now get the description

        // Level level = CONFIG.getCoverageInfo().getCoverageLevel(xpath, cldrFile.getLocaleID());
        String description = pathDescription.getDescription(xpath, value, null);
        if (description == null || description.equals("SKIP")) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(description);
        if (AnnotationUtil.pathIsAnnotation(xpath)) {
            final String cp = AnnotationUtil.getEmojiFromXPath(xpath);
            final String hex = Utility.hex(cp, 4, " U+");
            if (AnnotationUtil.haveEmojiImageFile(cp)) {
                final String fn = AnnotationUtil.calculateEmojiImageFilename(cp);
                buffer.append(
                        "\n\n<br><img class='emojiImage' height='64px' width='auto' src='images/emoji/"
                                + fn
                                + "'"
                                + " title='U+"
                                + hex
                                + "'>\n");
            } // else no image available
        }
        return buffer.toString();
    }

    public static String simplify(String exampleHtml) {
        return simplify(exampleHtml, false);
    }

    public static String simplify(String exampleHtml, boolean internal) {
        if (exampleHtml == null) {
            return null;
        }
        if (internal) {
            return "〖"
                    + exampleHtml
                            .replace(backgroundStartSymbol, "❬")
                            .replace(backgroundEndSymbol, "❭")
                    + "〗";
        }
        int startIndex = exampleHtml.indexOf(exampleStartHeader);
        if (startIndex >= 0) {
            int endIndex = exampleHtml.indexOf(exampleEnd, startIndex);
            if (endIndex > startIndex) {
                // remove header for context examples
                endIndex += exampleEnd.length();
                String head = exampleHtml.substring(0, startIndex);
                String tail = exampleHtml.substring(endIndex);
                exampleHtml = head + tail;
            }
        }
        return exampleHtml
                .replace("<div class='cldr_example'>", "〖")
                .replace("<div class='cldr_example_auto' dir='auto'>", "【")
                .replace("<div class='cldr_example_rtl' dir='rtl'>", "【⃪")
                .replace("</div>", "〗")
                .replace("<span class='cldr_substituted'>", "❬")
                .replace("</span>", "❭");
    }
}
