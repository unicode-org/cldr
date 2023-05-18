package org.unicode.cldr.test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
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
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.DateConstants;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.EmojiConstants;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.UnitConverter;
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

    private static final String contextheader =
            "Key: " + backgroundAutoStartSymbol + "neutral" + backgroundAutoEndSymbol + ", RTL";

    public static final char TEXT_VARIANT = '\uFE0E';

    private static final UnicodeSet BIDI_MARKS = new UnicodeSet("[:Bidi_Control:]").freeze();

    public static final Date DATE_SAMPLE;

    private static final Date DATE_SAMPLE2;
    private static final Date DATE_SAMPLE3;
    private static final Date DATE_SAMPLE4;

    static {
        Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);
        calendar.set(1999, 8, 5, 13, 25, 59); // 1999-08-05 13:25:59
        DATE_SAMPLE = calendar.getTime();
        calendar.set(1999, 9, 27, 13, 25, 59); // 1999-09-27 13:25:59
        DATE_SAMPLE2 = calendar.getTime();

        calendar.set(1999, 8, 5, 7, 0, 0); // 1999-08-5 07:00:00
        DATE_SAMPLE3 = calendar.getTime();
        calendar.set(1999, 8, 5, 23, 0, 0); // 1999-08-5 23:00:00
        DATE_SAMPLE4 = calendar.getTime();
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

    private static final Date FIRST_INTERVAL = getDate(2008, 1, 13, 5, 7, 9);
    private static final Map<String, Date> SECOND_INTERVAL =
            CldrUtility.asMap(
                    new Object[][] {
                        {
                            "G", getDate(1009, 2, 14, 17, 8, 10)
                        }, // "G" mostly useful for calendars that have short eras, like Japanese
                        {"y", getDate(2009, 2, 14, 17, 8, 10)},
                        {"M", getDate(2008, 2, 14, 17, 8, 10)},
                        {"d", getDate(2008, 1, 14, 17, 8, 10)},
                        {"a", getDate(2008, 1, 13, 17, 8, 10)},
                        {"h", getDate(2008, 1, 13, 6, 8, 10)},
                        {"m", getDate(2008, 1, 13, 5, 8, 10)}
                    });

    public void setCachingEnabled(boolean enabled) {
        exCache.setCachingEnabled(enabled);
    }

    public void setCacheOnly(boolean cacheOnly) {
        exCache.setCacheOnly(cacheOnly);
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
        icuServiceBuilder.setCldrFile(cldrFile);

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
            ExampleCache.ExampleCacheItem cacheItem = exCache.new ExampleCacheItem(xpath, value);
            result = cacheItem.getExample();
            if (result != null) {
                return result;
            }
            result = constructExampleHtml(xpath, value, nonTrivial);
            cacheItem.putExample(result);
        } catch (RuntimeException e) {
            e.printStackTrace();
            String unchained =
                    verboseErrors ? ("<br>" + finalizeBackground(unchainException(e))) : "";
            result = "<i>Parsing error. " + finalizeBackground(e.getMessage()) + "</i>" + unchained;
        }
        return result;
    }

    /**
     * Do the main work of getExampleHtml given that the result was not found in the cache.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @param nonTrivial true if we should avoid returning a trivial example (just value wrapped in
     *     markup)
     * @return the example HTML, or null
     */
    private String constructExampleHtml(String xpath, String value, boolean nonTrivial) {
        String result = null;
        boolean showContexts =
                isRTL || BIDI_MARKS.containsSome(value); // only used for certain example types
        /*
         * Need getInstance, not getFrozenInstance here: some functions such as handleNumberSymbol
         * expect to call functions like parts.addRelative which throw exceptions if parts is frozen.
         */
        XPathParts parts = XPathParts.getFrozenInstance(xpath).cloneAsThawed();
        if (parts.contains("dateRangePattern")) { // {0} - {1}
            result = handleDateRangePattern(value);
        } else if (parts.contains("timeZoneNames")) {
            result = handleTimeZoneName(parts, value);
        } else if (parts.contains("localeDisplayNames")) {
            result = handleDisplayNames(xpath, parts, value);
        } else if (parts.contains("currency")) {
            result = handleCurrency(xpath, parts, value);
        } else if (parts.contains("dayPeriods")) {
            result = handleDayPeriod(parts, value);
        } else if (parts.contains("pattern") || parts.contains("dateFormatItem")) {
            if (parts.contains("calendar")) {
                result = handleDateFormatItem(xpath, value, showContexts);
            } else if (parts.contains("miscPatterns")) {
                result = handleMiscPatterns(parts, value);
            } else if (parts.contains("numbers")) {
                if (parts.contains("currencyFormat")) {
                    result = handleCurrencyFormat(parts, value, showContexts);
                } else {
                    result = handleDecimalFormat(parts, value, showContexts);
                }
            }
        } else if (parts.getElement(2).contains("symbols")) {
            result = handleNumberSymbol(parts, value);
        } else if (parts.contains("defaultNumberingSystem")
                || parts.contains("otherNumberingSystems")) {
            result = handleNumberingSystem(value);
        } else if (parts.contains("currencyFormats") && parts.contains("unitPattern")) {
            result = formatCountValue(xpath, parts, value);
        } else if (parts.getElement(-1).equals("compoundUnitPattern")) {
            result = handleCompoundUnit(parts);
        } else if (parts.getElement(-1).equals("compoundUnitPattern1")
                || parts.getElement(-1).equals("unitPrefixPattern")) {
            result = handleCompoundUnit1(parts, value);
        } else if (parts.getElement(-1).equals("unitPattern")) {
            result = handleFormatUnit(parts, value);
        } else if (parts.getElement(-1).equals("perUnitPattern")) {
            result = handleFormatPerUnit(value);
        } else if (parts.getElement(-2).equals("minimalPairs")) {
            result = handleMinimalPairs(parts, value);
        } else if (parts.getElement(-1).equals("durationUnitPattern")) {
            result = handleDurationUnit(value);
        } else if (parts.contains("intervalFormats")) {
            result = handleIntervalFormats(parts, value);
        } else if (parts.getElement(1).equals("delimiters")) {
            result = handleDelimiters(parts, xpath, value);
        } else if (parts.getElement(1).equals("listPatterns")) {
            result = handleListPatterns(parts, value);
        } else if (parts.getElement(2).equals("ellipsis")) {
            result = handleEllipsis(parts.getAttributeValue(-1, "type"), value);
        } else if (parts.getElement(-1).equals("monthPattern")) {
            result = handleMonthPatterns(parts, value);
        } else if (parts.getElement(-1).equals("appendItem")) {
            result = handleAppendItems(parts, value);
        } else if (parts.getElement(-1).equals("annotation")) {
            result = handleAnnotationName(parts, value);
        } else if (parts.getElement(-1).equals("characterLabel")) {
            result = handleLabel(parts, value);
        } else if (parts.getElement(-1).equals("characterLabelPattern")) {
            result = handleLabelPattern(parts, value);
        } else if (parts.getElement(1).equals("personNames")) {
            result = handlePersonName(parts, value);
        } else if (parts.getElement(-1).equals("exemplarCharacters")
                || parts.getElement(-1).equals("parseLenient")) {
            result = handleUnicodeSet(parts, xpath, value);
        }

        // Handle the outcome
        if (result != null) {
            if (nonTrivial && value.equals(result)) {
                result = null;
            } else {
                result = finalizeBackground(result);
            }
        }
        return result;
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
    private String handleUnicodeSet(XPathParts parts, String xpath, String value) {
        ArrayList<String> examples = new ArrayList<>();
        UnicodeSet valueSet;
        try {
            valueSet = new UnicodeSet(value);
        } catch (Exception e) {
            return null;
        }
        String winningValue = cldrFile.getWinningValue(xpath);
        if (!winningValue.equals(value)) {
            // show delta
            final UnicodeSet winningSet = new UnicodeSet(winningValue);
            UnicodeSet value_minus_winning = new UnicodeSet(valueSet).removeAll(winningSet);
            UnicodeSet winning_minus_value = new UnicodeSet(winningSet).removeAll(valueSet);
            if (!value_minus_winning.isEmpty()) {
                examples.add(LRM + "➕ " + SUSF.format(value_minus_winning));
            }
            if (!winning_minus_value.isEmpty()) {
                examples.add(LRM + "➖ " + SUSF.format(winning_minus_value));
            }
        }
        if (SHOW_NON_SPACING_IN_UNICODE_SET
                && valueSet.containsSome(CodePointEscaper.NON_SPACING)) {
            for (String nsm : new UnicodeSet(valueSet).retainAll(CodePointEscaper.FORCE_ESCAPE)) {
                examples.add(CodePointEscaper.toExample(nsm.codePointAt(0)));
            }
        }
        examples.add(setBackground("internal: ") + valueSet.toPattern(false)); // internal format
        return formatExampleList(examples);
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
                    "//ldml/personNames/sampleName[@item=\"*\"]/nameField[@type=\"*\"]",
                    "//ldml/personNames/initialPattern[@type=\"*\"]",
                    "//ldml/personNames/foreignSpaceReplacement",
                    "//ldml/personNames/nativeSpaceReplacement",
                    "//ldml/personNames/personName[@order=\"*\"][@length=\"*\"][@usage=\"*\"][@formality=\"*\"]/namePattern");

    private static final Function<String, String> BACKGROUND_TRANSFORM =
            x -> backgroundStartSymbol + x + backgroundEndSymbol;

    private String handlePersonName(XPathParts parts, String value) {
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

            List<String> examples = null;
            final CLDRFile cldrFile2 = getCldrFile();
            switch (parts.getElement(2)) {
                case "nameOrderLocales":
                    examples = new ArrayList<>();
                    for (String localeId : PersonNameFormatter.SPLIT_SPACE.split(value)) {
                        final String name =
                                localeId.equals("und")
                                        ? "«any other»"
                                        : cldrFile2.getName(localeId);
                        examples.add(localeId + " = " + name);
                    }
                    break;
                case "initialPattern":
                    return null;
                case "sampleName":
                    return null;
                case "personName":
                    examples = new ArrayList<>();
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
            return formatExampleList(examples);
        } catch (Exception e) {
            StringBuffer stackTrace;
            try (StringWriter sw = new StringWriter();
                    PrintWriter p = new PrintWriter(sw)) {
                e.printStackTrace(p);
                stackTrace = sw.getBuffer();
            } catch (Exception e2) {
                stackTrace = new StringBuffer("internal error");
            }
            return "Internal error: " + e.getMessage() + "\n" + debugState + "\n" + stackTrace;
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

    private String handleLabelPattern(XPathParts parts, String value) {
        if ("category-list".equals(parts.getAttributeValue(-1, "type"))) {
            List<String> examples = new ArrayList<>();
            CLDRFile cfile = getCldrFile();
            SimpleFormatter initialPattern = SimpleFormatter.compile(setBackground(value));
            String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, "FR");
            String regionName = cfile.getStringValue(path);
            String flagName =
                    cfile.getStringValue("//ldml/characterLabels/characterLabel[@type=\"flag\"]");
            examples.add(
                    invertBackground(
                            EmojiConstants.getEmojiFromRegionCodes("FR")
                                    + " ⇒ "
                                    + initialPattern.format(flagName, regionName)));
            return formatExampleList(examples);
        }
        return null;
    }

    private String handleLabel(XPathParts parts, String value) {
        // "//ldml/characterLabels/characterLabel[@type=\"" + typeAttributeValue + "\"]"
        switch (parts.getAttributeValue(-1, "type")) {
            case "flag":
                {
                    String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
                    CLDRFile cfile = getCldrFile();
                    List<String> examples = new ArrayList<>();
                    SimpleFormatter initialPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                    addFlag(value2, "FR", cfile, initialPattern, examples);
                    addFlag(value2, "CN", cfile, initialPattern, examples);
                    addSubdivisionFlag(value2, "gbeng", initialPattern, examples);
                    addSubdivisionFlag(value2, "gbsct", initialPattern, examples);
                    addSubdivisionFlag(value2, "gbwls", initialPattern, examples);
                    return formatExampleList(examples);
                }
            case "keycap":
                {
                    String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
                    List<String> examples = new ArrayList<>();
                    CLDRFile cfile = getCldrFile();
                    SimpleFormatter initialPattern =
                            SimpleFormatter.compile(
                                    cfile.getStringValue(
                                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                    examples.add(invertBackground(initialPattern.format(value2, "1")));
                    examples.add(invertBackground(initialPattern.format(value2, "10")));
                    examples.add(invertBackground(initialPattern.format(value2, "#")));
                    return formatExampleList(examples);
                }
            default:
                return null;
        }
    }

    private void addFlag(
            String value2,
            String isoRegionCode,
            CLDRFile cfile,
            SimpleFormatter initialPattern,
            List<String> examples) {
        String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, isoRegionCode);
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

    private String handleAnnotationName(XPathParts parts, String value) {
        // ldml/annotations/annotation[@cp="🦰"][@type="tts"]
        // skip anything but the name
        if (!"tts".equals(parts.getAttributeValue(-1, "type"))) {
            return null;
        }
        String cp = parts.getAttributeValue(-1, "cp");
        if (cp == null || cp.isEmpty()) {
            return null;
        }
        Set<String> examples = new LinkedHashSet<>();
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
        return formatExampleList(examples);
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

    private String handleDayPeriod(XPathParts parts, String value) {
        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        List<String> examples = new ArrayList<>();
        final String dayPeriodType = parts.getAttributeValue(5, "type");
        if (dayPeriodType == null) {
            return null; // formerly happened for some "/alias" paths
        }
        org.unicode.cldr.util.DayPeriodInfo.Type aType =
                dayPeriodType.equals("format")
                        ? DayPeriodInfo.Type.format
                        : DayPeriodInfo.Type.selection;
        DayPeriodInfo dayPeriodInfo =
                supplementalDataInfo.getDayPeriods(aType, cldrFile.getLocaleID());
        String periodString = parts.getAttributeValue(-1, "type");
        if (periodString == null) {
            return null; // formerly happened for some "/alias" paths
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
        return formatExampleList(examples.toArray(new String[0]));
    }

    private String handleMinimalPairs(XPathParts parts, String minimalPattern) {
        List<String> examples = new ArrayList<>();

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
                return null;
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
        return formatExampleList(examples);
    }

    private String getOtherGender(String gender) {
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
            if (!sampleBad.equals(sample)) {
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

    private String handleFormatUnit(XPathParts parts, String unitPattern) {
        // Sample:
        // //ldml/units/unitLength[@type="long"]/unit[@type="duration-day"]/unitPattern[@count="one"][@case="accusative"]

        String count = parts.getAttributeValue(-1, "count");
        List<String> examples = new ArrayList<>();
        /*
         * PluralRules.FixedDecimal is deprecated, but deprecated in ICU is
         * also used to mark internal methods (which are OK for us to use in CLDR).
         */
        DecimalQuantity amount = getBest(Count.valueOf(count));
        if (amount == null) {
            return null;
        }
        DecimalFormat numberFormat;
        String formattedAmount;
        numberFormat = icuServiceBuilder.getNumberFormat(1);
        formattedAmount = numberFormat.format(amount.toBigDecimal());
        examples.add(
                format(unitPattern, backgroundStartSymbol + formattedAmount + backgroundEndSymbol));

        if (parts.getElement(-2).equals("unit")) {
            String longUnitId = parts.getAttributeValue(-2, "type");
            final String shortUnitId = UNIT_CONVERTER.getShortId(longUnitId);
            if (UnitConverter.HACK_SKIP_UNIT_NAMES.contains(shortUnitId)) {
                return null;
            }
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
        return formatExampleList(examples);
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

    private String handleFormatPerUnit(String value) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return format(value, backgroundStartSymbol + numberFormat.format(1) + backgroundEndSymbol);
    }

    public String handleCompoundUnit(XPathParts parts) {
        UnitLength unitLength = getUnitLength(parts);
        String compoundType = parts.getAttributeValue(-2, "type");
        Count count =
                Count.valueOf(CldrUtility.ifNull(parts.getAttributeValue(-1, "count"), "other"));
        return handleCompoundUnit(unitLength, compoundType, count);
    }

    @SuppressWarnings("deprecation")
    public String handleCompoundUnit(UnitLength unitLength, String compoundType, Count count) {
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
            return "n/a";
        }
        DecimalQuantity oneValue = DecimalQuantity_DualStorageBCD.fromExponentString("1");

        String unit1mid;
        String unit2mid;
        switch (compoundType) {
            default:
                return "n/a";
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
        return format(getValueFromFormat(perPath, form), unit1, unit2);
    }

    public String handleCompoundUnit1(XPathParts parts, String compoundPattern) {
        UnitLength unitLength = getUnitLength(parts);
        String pathCount = parts.getAttributeValue(-1, "count");
        if (pathCount == null) {
            return handleCompoundUnit1Name(unitLength, compoundPattern);
        } else {
            return handleCompoundUnit1(unitLength, Count.valueOf(pathCount), compoundPattern);
        }
    }

    private String handleCompoundUnit1Name(UnitLength unitLength, String compoundPattern) {
        String pathFormat =
                "//ldml/units/unitLength"
                        + unitLength.typeString
                        + "/unit[@type=\"{0}\"]/displayName";

        String meterFormat = getValueFromFormat(pathFormat, "length-meter");

        String modFormat =
                combinePrefix(meterFormat, compoundPattern, unitLength == UnitLength.LONG);

        return removeEmptyRuns(modFormat);
    }

    public String handleCompoundUnit1(UnitLength unitLength, Count count, String compoundPattern) {

        // we want to get a number that works for the count passed in.
        DecimalQuantity amount = getBest(count);
        if (amount == null) {
            return "n/a";
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

        return removeEmptyRuns(format(modFormat, numberFormat.format(amount.toBigDecimal())));
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

    private String handleMiscPatterns(XPathParts parts, String value) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(0);
        String start = backgroundStartSymbol + numberFormat.format(99) + backgroundEndSymbol;
        if ("range".equals(parts.getAttributeValue(-1, "type"))) {
            String end = backgroundStartSymbol + numberFormat.format(144) + backgroundEndSymbol;
            return format(value, start, end);
        } else {
            return format(value, start);
        }
    }

    private String handleIntervalFormats(XPathParts parts, String value) {
        if (!parts.getAttributeValue(3, "type").equals("gregorian")) {
            return null;
        }
        if (parts.getElement(6).equals("intervalFormatFallback")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            String fallbackFormat = invertBackground(setBackground(value));
            return format(
                    fallbackFormat,
                    dateFormat.format(FIRST_INTERVAL),
                    dateFormat.format(SECOND_INTERVAL.get("y")));
        }
        String greatestDifference = parts.getAttributeValue(-1, "id");
        /*
         * Choose an example interval suitable for the symbol. If testing years, use an interval
         * of more than one year, and so forth. For the purpose of choosing the interval,
         * "H" is equivalent to "h", and so forth; map to a symbol that occurs in SECOND_INTERVAL.
         */
        if (greatestDifference.equals("H")) { // Hour [0-23]
            greatestDifference = "h"; // Hour [1-12]
        } else if (greatestDifference.equals("B") // flexible day periods
                || greatestDifference.equals("b")) { // am, pm, noon, midnight
            greatestDifference = "a"; // AM, PM
        }
        // intervalFormatFallback
        // //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="yMd"]/greatestDifference[@id="y"]
        // find where to split the value
        intervalFormat.setPattern(parts, value);
        Date later = SECOND_INTERVAL.get(greatestDifference);
        if (later == null) {
            /*
             * This may still happen for some less-frequently used symbols such as "Q" (Quarter),
             * if they ever occur in the data.
             * Reference: https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table
             * For now, such paths do not get examples.
             */
            return null;
        }
        return intervalFormat.format(FIRST_INTERVAL, later);
    }

    private String handleDelimiters(XPathParts parts, String xpath, String value) {
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
        return invertBackground(
                format("{0}They said {1}" + example + "{2}.{3}", (Object[]) quotes));
    }

    private String handleListPatterns(XPathParts parts, String value) {
        // listPatternType is either "duration" or null/other list
        String listPatternType = parts.getAttributeValue(-2, "type");
        if (listPatternType == null || !listPatternType.contains("unit")) {
            return handleRegularListPatterns(parts, value, ListTypeLength.from(listPatternType));
        } else {
            return handleDurationListPatterns(parts, value, UnitLength.from(listPatternType));
        }
    }

    private String handleRegularListPatterns(
            XPathParts parts, String value, ListTypeLength listTypeLength) {
        String patternType = parts.getAttributeValue(-1, "type");
        if (patternType == null) {
            return null; // formerly happened for some "/alias" paths
        }
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        if (patternType.equals("2")) {
            return invertBackground(format(setBackground(value), territory1, territory2));
        }
        String territory3 = getValueFromFormat(pathFormat, "EG");
        String territory4 = getValueFromFormat(pathFormat, "CA");
        return longListPatternExample(
                listTypeLength.getPath(),
                patternType,
                value,
                territory1,
                territory2,
                territory3,
                territory4);
    }

    private String handleDurationListPatterns(
            XPathParts parts, String value, UnitLength unitWidth) {
        String patternType = parts.getAttributeValue(-1, "type");
        if (patternType == null) {
            return null; // formerly happened for some "/alias" paths
        }
        String duration1 = getFormattedUnit("duration-day", unitWidth, 4);
        String duration2 = getFormattedUnit("duration-hour", unitWidth, 2);
        if (patternType.equals("2")) {
            return invertBackground(format(setBackground(value), duration1, duration2));
        }
        String duration3 = getFormattedUnit("duration-minute", unitWidth, 37);
        String duration4 = getFormattedUnit("duration-second", unitWidth, 23);
        return longListPatternExample(
                unitWidth.listTypeLength.getPath(),
                patternType,
                value,
                duration1,
                duration2,
                duration3,
                duration4);
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

    public String handleEllipsis(String type, String value) {
        String pathFormat = "//ldml/localeDisplayNames/territories/territory[@type=\"{0}\"]";
        //  <ellipsis type="word-final">{0} …</ellipsis>
        //  <ellipsis type="word-initial">… {0}</ellipsis>
        //  <ellipsis type="word-medial">{0} … {1}</ellipsis>
        String territory1 = getValueFromFormat(pathFormat, "CH");
        String territory2 = getValueFromFormat(pathFormat, "JP");
        // if it isn't a word, break in the middle
        if (!type.contains("word")) {
            territory1 = clip(territory1, 0, 1);
            territory2 = clip(territory2, 1, 0);
        }
        if (type.contains("initial")) {
            territory1 = territory2;
        }
        return invertBackground(format(setBackground(value), territory1, territory2));
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
    private String handleMonthPatterns(XPathParts parts, String value) {
        String calendar = parts.getAttributeValue(3, "type");
        String context = parts.getAttributeValue(5, "type");
        String month = "8";
        if (!context.equals("numeric")) {
            String width = parts.getAttributeValue(6, "type");
            String xpath =
                    "//ldml/dates/calendars/calendar[@type=\"{0}\"]/months/monthContext[@type=\"{1}\"]/monthWidth[@type=\"{2}\"]/month[@type=\"8\"]";
            month = getValueFromFormat(xpath, calendar, context, width);
        }
        return invertBackground(format(setBackground(value), month));
    }

    private String handleAppendItems(XPathParts parts, String value) {
        String request = parts.getAttributeValue(-1, "request");
        if (!"Timezone".equals(request)) {
            return null;
        }
        String calendar = parts.getAttributeValue(3, "type");

        SimpleDateFormat sdf =
                icuServiceBuilder.getDateFormat(calendar, 0, DateFormat.MEDIUM, null);
        String zone = cldrFile.getStringValue("//ldml/dates/timeZoneNames/gmtZeroFormat");
        return format(value, setBackground(sdf.format(DATE_SAMPLE)), setBackground(zone));
    }

    private class IntervalFormat {
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

    private String handleDurationUnit(String value) {
        DateFormat df = this.icuServiceBuilder.getDateFormat("gregorian", value.replace('h', 'H'));
        df.setTimeZone(TimeZone.GMT_ZONE);
        long time = ((5 * 60 + 37) * 60 + 23) * 1000;
        try {
            return df.format(new Date(time));
        } catch (IllegalArgumentException e) {
            // e.g., Illegal pattern character 'o' in "aɖabaƒoƒo m:ss"
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String formatCountValue(String xpath, XPathParts parts, String value) {
        if (!parts.containsAttribute("count")) { // no examples for items that don't format
            return null;
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
            return null;
        } else {
            try {
                count = Count.valueOf(countString);
            } catch (Exception e) {
                return null; // counts like 0
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
        return result.isEmpty() ? null : result;
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
                        ? "//ldml/numbers/currencyFormats/unitPattern" + countAttribute(count)
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

    private String handleNumberSymbol(XPathParts parts, String value) {
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
            return null;
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
        return backgroundStartSymbol + example + backgroundEndSymbol;
    }

    private String handleNumberingSystem(String value) {
        NumberFormat x = icuServiceBuilder.getGenericNumberFormat(value);
        x.setGroupingUsed(false);
        return x.format(NUMBER_SAMPLE_WHOLE);
    }

    private String handleTimeZoneName(XPathParts parts, String value) {
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
                return result;
            }
            if (countryCode.equals("001")) {
                // GMT code, so format.
                try {
                    String hourOffset = timezone.substring(timezone.contains("+") ? 8 : 7);
                    int hours = Integer.parseInt(hourOffset);
                    result = getGMTFormat(null, null, hours);
                } catch (RuntimeException e) {
                    return null; // fail, skip
                }
            } else {
                result = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode));
            }
        } else if (parts.contains("zone")) { // {0} Time
            result = value; // trivial -- is this beneficial?
        } else if (parts.contains("regionFormat")) { // {0} Time
            result = format(value, setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "JP")));
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
        return result;
    }

    @SuppressWarnings("deprecation")
    private String handleDateFormatItem(String xpath, String value, boolean showContexts) {
        // Get here if parts contains "calendar" and either of "pattern", "dateFormatItem"

        String fullpath = cldrFile.getFullXPath(xpath);
        XPathParts parts = XPathParts.getFrozenInstance(fullpath);
        String calendar = parts.findAttributeValue("calendar", "type");

        if (parts.contains("dateTimeFormat")) { // date-time combining patterns
            String dateFormatXPath =
                    cldrFile.getWinningPath(
                            xpath.replaceAll("dateTimeFormat", "dateFormat")
                                    .replaceAll("atTime", "standard"));
            String timeFormatXPath =
                    cldrFile.getWinningPath(
                            xpath.replaceAll("dateTimeFormat", "timeFormat")
                                    .replaceAll("atTime", "standard"));
            String dateFormatValue = cldrFile.getWinningValue(dateFormatXPath);
            String timeFormatValue = cldrFile.getWinningValue(timeFormatXPath);
            parts = XPathParts.getFrozenInstance(cldrFile.getFullXPath(dateFormatXPath));
            String dateNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            parts = XPathParts.getFrozenInstance(cldrFile.getFullXPath(timeFormatXPath));
            String timeNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            SimpleDateFormat df =
                    icuServiceBuilder.getDateFormat(calendar, dateFormatValue, dateNumbersOverride);
            SimpleDateFormat tf =
                    icuServiceBuilder.getDateFormat(calendar, timeFormatValue, timeNumbersOverride);
            df.setTimeZone(ZONE_SAMPLE);
            tf.setTimeZone(ZONE_SAMPLE);
            String dfResult = "'" + df.format(DATE_SAMPLE) + "'";
            String tfResult = "'" + tf.format(DATE_SAMPLE) + "'";
            SimpleDateFormat dtf =
                    icuServiceBuilder.getDateFormat(
                            calendar,
                            MessageFormat.format(
                                    value,
                                    (Object[])
                                            new String[] {
                                                setBackground(tfResult), setBackground(dfResult)
                                            }));
            return dtf.format(DATE_SAMPLE);
        } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id) || value == null) {
                return startItalicSymbol + "n/a" + endItalicSymbol;
            } else {
                String numbersOverride = parts.findAttributeValue("pattern", "numbers");
                SimpleDateFormat sdf =
                        icuServiceBuilder.getDateFormat(calendar, value, numbersOverride);
                sdf.setTimeZone(ZONE_SAMPLE);
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
                    // Standard date/time format, or availableFormat without dayPeriod
                    if (value.contains("MMM") || value.contains("LLL")) {
                        // alpha month, do not need context examples
                        return sdf.format(DATE_SAMPLE);
                    } else {
                        // Use contextExamples if showContexts T
                        String example =
                                showContexts
                                        ? exampleStartHeaderSymbol
                                                + contextheader
                                                + exampleEndSymbol
                                        : "";
                        example = addExampleResult(sdf.format(DATE_SAMPLE), example, showContexts);
                        return example;
                    }
                } else {
                    List<String> examples = new ArrayList<>();
                    examples.add(sdf.format(DATE_SAMPLE3));
                    examples.add(sdf.format(DATE_SAMPLE));
                    examples.add(sdf.format(DATE_SAMPLE4));
                    return formatExampleList(examples.toArray(new String[0]));
                }
            }
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
    private String handleCurrencyFormat(XPathParts parts, String value, boolean showContexts) {

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
        df.applyPattern(value);

        String countValue = parts.getAttributeValue(-1, "count");
        if (countValue != null) {
            return formatCountDecimal(df, countValue);
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

        return example;
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
    private String handleDecimalFormat(XPathParts parts, String value, boolean showContexts) {
        String example =
                showContexts ? exampleStartHeaderSymbol + contextheader + exampleEndSymbol : "";
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(value, numberSystem);
        String countValue = parts.getAttributeValue(-1, "count");
        if (countValue != null) {
            return formatCountDecimal(numberFormat, countValue);
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
        return example;
    }

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
        Double numberSample = getExampleForPattern(numberFormat, count);
        if (numberSample == null) {
            // Ideally, we would suppress the value in the survey tool.
            // However, until we switch over to the ICU samples, we are not guaranteed
            // that "no samples" means "can't occur". So we manufacture something.
            int digits = numberFormat.getMinimumIntegerDigits();
            numberSample = (double) Math.round(1.2345678901234 * Math.pow(10, digits - 1));
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
     * should be a more elegant way to do this).
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

    private String handleCurrency(String xpath, XPathParts parts, String value) {
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
            return result;
        } else if (parts.contains("displayName")) {
            return formatCountValue(xpath, parts, value);
        }
        return null;
    }

    private String handleDateRangePattern(String value) {
        String result;
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        result =
                format(
                        value,
                        setBackground(dateFormat.format(DATE_SAMPLE)),
                        setBackground(dateFormat.format(DATE_SAMPLE2)));
        return result;
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

    private String handleDisplayNames(String xpath, XPathParts parts, String value) {
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
            String[] examples = new String[locales.size()];
            for (int i = 0; i < locales.size(); i++) {
                examples[i] =
                        invertBackground(
                                cldrFile.getName(
                                        locales.get(i),
                                        false,
                                        localeKeyTypePattern,
                                        localePattern,
                                        localeSeparator));
            }
            result = formatExampleList(examples);
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
            } else {
                value = setBackground(value);
                List<String> examples = new ArrayList<>();
                String nameType = parts.getElement(3);

                Map<String, String> likely = supplementalDataInfo.getLikelySubtags();
                String alt = parts.getAttributeValue(-1, "alt");
                boolean isStandAloneValue = "stand-alone".equals(alt);
                if (!isStandAloneValue) {
                    // only do this if the value is not a stand-alone form
                    String tag = "language".equals(nameType) ? type : "und_" + type;
                    String max = LikelySubtags.maximize(tag, likely);
                    if (max == null) {
                        return null;
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
                                        CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, ltp.getLanguage()));
                        if (languageName == null) {
                            languageName =
                                    cldrFile.getStringValueWithBailey(
                                            CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, "en"));
                        }
                        if (languageName == null) {
                            languageName = ltp.getLanguage();
                        }
                    }
                    if (scriptName == null) {
                        scriptName =
                                cldrFile.getStringValueWithBailey(
                                        CLDRFile.getKey(CLDRFile.SCRIPT_NAME, ltp.getScript()));
                        if (scriptName == null) {
                            scriptName =
                                    cldrFile.getStringValueWithBailey(
                                            CLDRFile.getKey(CLDRFile.SCRIPT_NAME, "Latn"));
                        }
                        if (scriptName == null) {
                            scriptName = ltp.getScript();
                        }
                    }
                    if (territoryName == null) {
                        territoryName =
                                cldrFile.getStringValueWithBailey(
                                        CLDRFile.getKey(CLDRFile.TERRITORY_NAME, ltp.getRegion()));
                        if (territoryName == null) {
                            territoryName =
                                    cldrFile.getStringValueWithBailey(
                                            CLDRFile.getKey(CLDRFile.TERRITORY_NAME, "US"));
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
                result = formatExampleList(examples.toArray(new String[0]));
            }
        }
        return result;
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
    private String formatExampleList(Collection<String> examples) {
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
                        .replace(endSupSymbol, endSup);
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

        Level level = CONFIG.getCoverageInfo().getCoverageLevel(xpath, cldrFile.getLocaleID());
        String description = pathDescription.getDescription(xpath, value, null);
        if (description == null || description.equals("SKIP")) {
            return null;
        }
        int start = 0;
        StringBuilder buffer = new StringBuilder();

        Matcher URLMatcher = URL_PATTERN.matcher("");
        while (URLMatcher.reset(description).find(start)) {
            final String url = URLMatcher.group();
            buffer.append(
                            TransliteratorUtilities.toHTML.transliterate(
                                    description.substring(start, URLMatcher.start())))
                    .append("<a target='CLDR-ST-DOCS' href='")
                    .append(url)
                    .append("'>")
                    .append(url)
                    .append("</a>");
            start = URLMatcher.end();
        }
        buffer.append(TransliteratorUtilities.toHTML.transliterate(description.substring(start)));
        if (AnnotationUtil.pathIsAnnotation(xpath)) {
            XPathParts emoji = XPathParts.getFrozenInstance(xpath);
            String cp = emoji.getAttributeValue(-1, "cp");
            String minimal = Utility.hex(cp).replace(',', '_').toLowerCase(Locale.ROOT);
            buffer.append(
                    "<br><img height='64px' width='auto' src='images/emoji/emoji_"
                            + minimal
                            + ".png'>");
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
