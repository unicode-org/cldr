package org.unicode.cldr.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.CLDRFileTransformer;
import org.unicode.cldr.tool.CLDRFileTransformer.LocaleTransform;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.EmojiConstants;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.XListFormatter.ListTypeLength;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
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
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Class to generate examples and help messages for the Survey tool (or console version).
 *
 * @author markdavis
 */
public class ExampleGenerator {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    private static final String ALT_STAND_ALONE = "[@alt=\"stand-alone\"]";

    private static final String EXEMPLAR_CITY_LOS_ANGELES = "//ldml/dates/timeZoneNames/zone[@type=\"America/Los_Angeles\"]/exemplarCity";

    private static final Pattern URL_PATTERN = Pattern
        .compile("http://[\\-a-zA-Z0-9]+(\\.[\\-a-zA-Z0-9]+)*([/#][\\-a-zA-Z0-9]+)*");

    final static boolean DEBUG_SHOW_HELP = false;

    private static SupplementalDataInfo supplementalDataInfo;
    private PathDescription pathDescription;

    public void setCachingEnabled(boolean enabled) {
        exCache.setCachingEnabled(enabled);
    }

    public void setCacheOnly(boolean cacheOnly) {
        exCache.setCacheOnly(cacheOnly);
    }

    public final static double NUMBER_SAMPLE = 123456.789;
    public final static double NUMBER_SAMPLE_WHOLE = 2345;

    public final static TimeZone ZONE_SAMPLE = TimeZone.getTimeZone("America/Indianapolis");
    public final static TimeZone GMT_ZONE_SAMPLE = TimeZone.getTimeZone("Etc/GMT");

    public final static Date DATE_SAMPLE;

    private final static Date DATE_SAMPLE2;
    private final static Date DATE_SAMPLE3;
    private final static Date DATE_SAMPLE4;

    private String backgroundStart = "<span class='cldr_substituted'>";
    private String backgroundEnd = "</span>";

    private static final String exampleStart = "<div class='cldr_example'>";
    private static final String exampleEnd = "</div>";
    private static final String startItalic = "<i>";
    private static final String endItalic = "</i>";
    private static final String startSup = "<sup>";
    private static final String endSup = "</sup>";

    public static final String backgroundStartSymbol = "\uE234";
    public static final String backgroundEndSymbol = "\uE235";
    private static final String backgroundTempSymbol = "\uE236";
    private static final String exampleSeparatorSymbol = "\uE237";
    private static final String startItalicSymbol = "\uE238";
    private static final String endItalicSymbol = "\uE239";
    private static final String startSupSymbol = "\uE23A";
    private static final String endSupSymbol = "\uE23B";

    /**
     * verboseErrors affects not only the verboseness of error reporting, but also, for
     * example, whether some unit tests pass or fail. The function setVerboseErrors
     * can be used to modify it. It must be initialized here to false, otherwise
     * cldr-unittest TestAll.java fails. Reference: https://unicode.org/cldr/trac/ticket/12025
     */
    private boolean verboseErrors = false;

    private Calendar calendar = Calendar.getInstance(ZONE_SAMPLE, ULocale.ENGLISH);

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

    private CLDRFile cldrFile;

    public CLDRFile getCldrFile() {
        return cldrFile;
    }

    private CLDRFile englishFile;
    Matcher URLMatcher = URL_PATTERN.matcher("");

    private ExampleCache exCache = new ExampleCache();

    /**
     * For this (locale-specific) ExampleGenerator, clear the cached examples for
     * any paths whose examples might depend on the winning value of the given path,
     * since the winning value of the given path has changed.
     *
     * @param xpath the path whose winning value has changed
     *
     * Called by TestCache.updateExampleGeneratorCache
     */
    public void updateCache(String xpath) {
        exCache.update(xpath);
    }

    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private PluralInfo pluralInfo;

    private PluralSamples patternExamples;

    private Map<String, String> subdivisionIdToName;

    /**
     * For getting the end of the "background" style. Default is "</span>". It is
     * used in composing patterns, so it can show the part that corresponds to the
     * value.
     *
     * @return
     */
    public String getBackgroundEnd() {
        return backgroundEnd;
    }

    /**
     * For setting the end of the "background" style. Default is "</span>". It is
     * used in composing patterns, so it can show the part that corresponds to the
     * value.
     *
     * @return
     */
    public void setBackgroundEnd(String backgroundEnd) {
        this.backgroundEnd = backgroundEnd;
    }

    /**
     * For getting the "background" style. Default is "<span
     * style='background-color: gray'>". It is used in composing patterns, so it
     * can show the part that corresponds to the value.
     *
     * @return
     */
    public String getBackgroundStart() {
        return backgroundStart;
    }

    /**
     * For setting the "background" style. Default is "<span
     * style='background-color: gray'>". It is used in composing patterns, so it
     * can show the part that corresponds to the value.
     *
     * @return
     */
    public void setBackgroundStart(String backgroundStart) {
        this.backgroundStart = backgroundStart;
    }

    /**
     * Set the verbosity level of internal errors.
     * For example, setVerboseErrors(true) will cause
     * full stack traces to be shown in some cases.
     */
    public void setVerboseErrors(boolean verbosity) {
        this.verboseErrors = verbosity;
    }

    private static final boolean DEBUG_EXAMPLE_GENERATOR = false;
    private String creationTime = null; // only used if DEBUG_EXAMPLE_GENERATOR

    /**
     * True if this ExampleGenerator is especially for generating "English" examples,
     * false if it is for generating "native" examples.
     */
    private boolean typeIsEnglish;

    /**
     * Create an Example Generator. If this is shared across threads, it must be synchronized.
     *
     * @param resolvedCldrFile
     * @param englishFile
     * @param supplementalDataDirectory
     */
    public ExampleGenerator(CLDRFile resolvedCldrFile, CLDRFile englishFile, String supplementalDataDirectory) {
        if (!resolvedCldrFile.isResolved()) {
            throw new IllegalArgumentException("CLDRFile must be resolved");
        }
        if (!englishFile.isResolved()) {
            throw new IllegalArgumentException("English CLDRFile must be resolved");
        }
        this.cldrFile = resolvedCldrFile;
        this.subdivisionIdToName = EmojiSubdivisionNames.getSubdivisionIdToName(cldrFile.getLocaleID());
        this.englishFile = englishFile;
        this.typeIsEnglish = (resolvedCldrFile == englishFile);
        synchronized (ExampleGenerator.class) {
            if (supplementalDataInfo == null) {
                supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDataDirectory);
            }
        }
        icuServiceBuilder.setCldrFile(cldrFile);

        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());

        if (DEBUG_EXAMPLE_GENERATOR) {
            creationTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Calendar.getInstance().getTime());
            System.out.println("🧞‍ Created new ExampleGenerator for loc " + cldrFile.getLocaleID() + " at " + creationTime);
        }
    }

    /**
     * Get an example string, in html, if there is one for this path,
     * otherwise null. For use in the survey tool, an example might be returned
     * *even* if there is no value in the locale. For example, the locale might
     * have a path that English doesn't, but you want to return the best English
     * example. <br>
     * The result is valid HTML.
     *
     * If generating examples for an inheritance marker, use the "real" inherited value
     * to generate from. Do this BEFORE accessing the cache, which doesn't use INHERITANCE_MARKER.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @return the example HTML, or null
     */
    public String getExampleHtml(String xpath, String value) {
        if (value == null || xpath == null || xpath.endsWith("/alias")) {
            return null;
        }
        String result = null;
        try {
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                value = cldrFile.getConstructedBaileyValue(xpath, null, null);
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
            result = constructExampleHtml(xpath, value);
            cacheItem.putExample(result);
        } catch (RuntimeException e) {
            e.printStackTrace();
            String unchained = verboseErrors ? ("<br>" + finalizeBackground(unchainException(e))) : "";
            result = "<i>Parsing error. " + finalizeBackground(e.getMessage()) + "</i>" + unchained;
        }
        return result;
    }

    /**
     * Do the main work of getExampleHtml given that the result was not
     * found in the cache.
     *
     * @param xpath the path; e.g., "//ldml/dates/timeZoneNames/fallbackFormat"
     * @param value the value; e.g., "{1} [{0}]"; not necessarily the winning value
     * @return the example HTML, or null
     */
    private String constructExampleHtml(String xpath, String value) {
        String result = null;
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
                result = handleDateFormatItem(xpath, value);
            } else if (parts.contains("miscPatterns")) {
                result = handleMiscPatterns(parts, value);
            } else if (parts.contains("numbers")) {
                if (parts.contains("currencyFormat")) {
                    result = handleCurrencyFormat(parts, value);
                } else {
                    result = handleDecimalFormat(parts, value);
                }
            }
        } else if (parts.getElement(2).contains("symbols")) {
            result = handleNumberSymbol(parts, value);
        } else if (parts.contains("defaultNumberingSystem") || parts.contains("otherNumberingSystems")) {
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
            result = handleFormatPerUnit(parts, value);
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
        }
        if (result != null) {
            if (!typeIsEnglish) {
                result = addTransliteration(result, value);
            }
            result = finalizeBackground(result);
        }
        return result;
    }

    private String handleLabelPattern(XPathParts parts, String value) {
        switch (parts.getAttributeValue(-1, "type")) {
        case "category-list":
            List<String> examples = new ArrayList<>();
            CLDRFile cfile = getCldrFile();
            SimpleFormatter initialPattern = SimpleFormatter.compile(setBackground(value));
            String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, "FR");
            String regionName = cfile.getStringValue(path);
            String flagName = cfile.getStringValue("//ldml/characterLabels/characterLabel[@type=\"flag\"]");
            examples.add(invertBackground(EmojiConstants.getEmojiFromRegionCodes("FR")
                + " ⇒ " + initialPattern.format(flagName, regionName)));
            return formatExampleList(examples);
        default:
            return null;
        }
    }

    private String handleLabel(XPathParts parts, String value) {
        // "//ldml/characterLabels/characterLabel[@type=\"" + typeAttributeValue + "\"]"
        switch (parts.getAttributeValue(-1, "type")) {
        case "flag": {
            String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
            CLDRFile cfile = getCldrFile();
            List<String> examples = new ArrayList<>();
            SimpleFormatter initialPattern = SimpleFormatter.compile(cfile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
            addFlag(value2, "FR", cfile, initialPattern, examples);
            addFlag(value2, "CN", cfile, initialPattern, examples);
            addSubdivisionFlag(value2, "gbeng", initialPattern, examples);
            addSubdivisionFlag(value2, "gbsct", initialPattern, examples);
            addSubdivisionFlag(value2, "gbwls", initialPattern, examples);
            return formatExampleList(examples);
        }
        case "keycap": {
            String value2 = backgroundStartSymbol + value + backgroundEndSymbol;
            List<String> examples = new ArrayList<>();
            CLDRFile cfile = getCldrFile();
            SimpleFormatter initialPattern = SimpleFormatter.compile(cfile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
            examples.add(invertBackground(initialPattern.format(value2, "1")));
            examples.add(invertBackground(initialPattern.format(value2, "10")));
            examples.add(invertBackground(initialPattern.format(value2, "#")));
            return formatExampleList(examples);
        }
        default:
            return null;
        }
    }

    private void addFlag(String value2, String isoRegionCode, CLDRFile cfile, SimpleFormatter initialPattern, List<String> examples) {
        String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, isoRegionCode);
        String regionName = cfile.getStringValue(path);
        examples.add(invertBackground(EmojiConstants.getEmojiFromRegionCodes(isoRegionCode)
            + " ⇒ " + initialPattern.format(value2, regionName)));
    }

    private void addSubdivisionFlag(String value2, String isoSubdivisionCode, SimpleFormatter initialPattern, List<String> examples) {
        String subdivisionName = subdivisionIdToName.get(isoSubdivisionCode);
        if (subdivisionName == null) {
            subdivisionName = isoSubdivisionCode;
        }
        examples.add(invertBackground(EmojiConstants.getEmojiFromSubdivisionCodes(isoSubdivisionCode)
            + " ⇒ " + initialPattern.format(value2, subdivisionName)));
    }

    private String handleAnnotationName(XPathParts parts, String value) {
        //ldml/annotations/annotation[@cp="🦰"][@type="tts"]
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
        switch(first) {
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
                SimpleFormatter initialPattern = SimpleFormatter.compile(cfile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
                SimpleFormatter listPattern = SimpleFormatter.compile(cfile.getStringValue("//ldml/listPatterns/listPattern[@type=\"unit-short\"]/listPatternPart[@type=\"2\"]"));

                hair = EmojiConstants.JOINER_STRING + hair;
                formatPeople(cfile, first, isSkin, value2, "👩", skin, skinName, hair, hairName, initialPattern, listPattern, examples);
                formatPeople(cfile, first, isSkin, value2, "👨", skin, skinName, hair, hairName, initialPattern, listPattern, examples);
            }
            break;
        }
        return formatExampleList(examples);
    }

    private String getEmojiName(CLDRFile cfile, String skin) {
        return cfile.getStringValue("//ldml/annotations/annotation[@cp=\"" + skin + "\"][@type=\"tts\"]");
    }

    //ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"]
    private String formatGroup(String value, String sourceEmoji, String... components) {
        CLDRFile cfile = getCldrFile();
        SimpleFormatter initialPattern = SimpleFormatter.compile(cfile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
        String value2 = backgroundEndSymbol + value + backgroundStartSymbol;
        String[] names = new String[components.length];
        int i = 0;
        for (String component : components) {
            names[i++] = getEmojiName(cfile, component);
        }
        return backgroundStartSymbol + sourceEmoji + " ⇒ " + initialPattern.format(value2,
            longListPatternExample(EmojiConstants.COMPOSED_NAME_LIST.getPath(), "n/a", "n/a2", names));
    }

    private void formatPeople(CLDRFile cfile, int first, boolean isSkin, String value2, String person, String skin, String skinName,
        String hair, String hairName, SimpleFormatter initialPattern, SimpleFormatter listPattern, Collection<String> examples) {
        String cp;
        String personName = getEmojiName(cfile, person);
        StringBuilder emoji = new StringBuilder(person).appendCodePoint(first);
        cp = UTF16.valueOf(first);
        cp = isSkin ? cp : EmojiConstants.JOINER_STRING + cp;
        examples.add(person + cp + " ⇒ " + invertBackground(initialPattern.format(personName,value2)));
        emoji.setLength(0);
        emoji.append(personName);
        if (isSkin) {
            skinName = value2;
            skin = cp;
        } else {
            hairName = value2;
            hair = cp;
        }
        examples.add(person + skin + hair + " ⇒ " + invertBackground(listPattern.format(initialPattern.format(personName, skinName), hairName)));
    }

    private String handleDayPeriod(XPathParts parts, String value) {
        //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="morning1"]
        List<String> examples = new ArrayList<>();
        final String dayPeriodType = parts.getAttributeValue(5, "type");
        if (dayPeriodType == null) {
            return null; // formerly happened for some "/alias" paths
        }
        org.unicode.cldr.util.DayPeriodInfo.Type aType = dayPeriodType.equals("format") ? DayPeriodInfo.Type.format : DayPeriodInfo.Type.selection;
        DayPeriodInfo dayPeriodInfo = supplementalDataInfo.getDayPeriods(aType, cldrFile.getLocaleID());
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
                String timeFormatString = icuServiceBuilder.formatDayPeriod(time, backgroundStartSymbol + value + backgroundEndSymbol);
                examples.add(invertBackground(timeFormatString));
            }
        }
        return formatExampleList(examples.toArray(new String[examples.size()]));
    }

    private UnitLength getUnitLength(XPathParts parts) {
        return UnitLength.valueOf(parts.getAttributeValue(-3, "type").toUpperCase(Locale.ENGLISH));
    }

    private String handleFormatUnit(XPathParts parts, String value) {
        String count = parts.getAttributeValue(-1, "count");
        List<String> examples = new ArrayList<>();
        /*
         * PluralRules.FixedDecimal is deprecated, but deprecated in ICU is
         * also used to mark internal methods (which are OK for us to use in CLDR).
         */
        @SuppressWarnings("deprecation")
        FixedDecimal amount = getBest(Count.valueOf(count));
        if (amount != null) {
            DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
            examples.add(format(value, backgroundStartSymbol + numberFormat.format(amount) + backgroundEndSymbol));
        }
        if (parts.getElement(-2).equals("unit")) {
            String longUnitId = parts.getAttributeValue(-2, "type");
            UnitConverter uc = supplementalDataInfo.getUnitConverter();
            final String shortUnitId = uc.getShortId(longUnitId);
            if (UnitConverter.HACK_SKIP_UNIT_NAMES.contains(shortUnitId)) {
                return null;
            }
            final UnitId unitId = uc.createUnitId(shortUnitId);
            String width = parts.getAttributeValue(2, "type");
            String pattern = unitId.toString(getCldrFile(), width, count, "nominative", null, false);
            if (pattern != null && !value.contentEquals(pattern)) {
                examples.add(pattern);
            }
        }
        return formatExampleList(examples);
    }

    private String handleFormatPerUnit(XPathParts parts, String value) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return format(value, backgroundStartSymbol + numberFormat.format(1) + backgroundEndSymbol);
    }

    public String handleCompoundUnit(XPathParts parts) {
        UnitLength unitLength = getUnitLength(parts);
        String compoundType = parts.getAttributeValue(-2, "type");
        Count count = Count.valueOf(CldrUtility.ifNull(parts.getAttributeValue(-1, "count"), "other"));
        return handleCompoundUnit(unitLength, compoundType, count);
    }

    @SuppressWarnings("deprecation")
    public String handleCompoundUnit(UnitLength unitLength, String compoundType, Count count) {
        /**
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
        FixedDecimal amount = getBest(count);
        if (amount == null) {
            return "n/a";
        }
        FixedDecimal oneValue = new FixedDecimal(1d, 0);

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
            unit1mid = getFormattedUnit("force-newton", unitLength, oneValue, icuServiceBuilder.getNumberFormat(1).format(amount));
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
        String pathFormat = "//ldml/units/unitLength" + unitLength.typeString + "/unit[@type=\"{0}\"]/displayName";

        String meterFormat = getValueFromFormat(pathFormat, "length-meter");

        String modFormat = combinePrefix(meterFormat, compoundPattern, unitLength == UnitLength.LONG);

        return removeEmptyRuns(modFormat);
    }

    public String handleCompoundUnit1(UnitLength unitLength, Count count, String compoundPattern) {

        // we want to get a number that works for the count passed in.
        @SuppressWarnings("deprecation")
        FixedDecimal amount = getBest(count);
        if (amount == null) {
            return "n/a";
        }
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);

        @SuppressWarnings("deprecation")
        String form1 = this.pluralInfo.getPluralRules().select(amount);

        String pathFormat = "//ldml/units/unitLength" + unitLength.typeString
            + "/unit[@type=\"{0}\"]/unitPattern[@count=\"{1}\"]";

        // now pick up the meter pattern

        String meterFormat = getValueFromFormat(pathFormat, "length-meter", form1);

        // now combine them

        String modFormat = combinePrefix(meterFormat, compoundPattern, unitLength == UnitLength.LONG);

        return removeEmptyRuns(format(modFormat, numberFormat.format(amount)));
    }

    // TODO, pass in unitLength instead of last parameter, and do work in Units.combinePattern.

    public String combinePrefix(String unitFormat, String inCompoundPattern, boolean lowercaseUnitIfNoSpaceInCompound) {
        // mark the part except for the {0} as foreground
        String compoundPattern =  backgroundEndSymbol
            + inCompoundPattern.replace("{0}", backgroundStartSymbol + "{0}" + backgroundEndSymbol)
            +   backgroundStartSymbol;

        String modFormat = Units.combinePattern(unitFormat, compoundPattern, lowercaseUnitIfNoSpaceInCompound);

        return backgroundStartSymbol + modFormat + backgroundEndSymbol;
    }

    //ldml/units/unitLength[@type="long"]/compoundUnit[@type="per"]/compoundUnitPattern
    public String makeCompoundUnitPath(UnitLength unitLength, String compoundType, String patternType) {
        return "//ldml/units/unitLength" + unitLength.typeString
            + "/compoundUnit[@type=\"" + compoundType + "\"]"
            + "/" + patternType;
    }

    @SuppressWarnings("deprecation")
    private FixedDecimal getBest(Count count) {
        FixedDecimalSamples samples = pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.DECIMAL);
        if (samples == null) {
            samples = pluralInfo.getPluralRules().getDecimalSamples(count.name(), SampleType.INTEGER);
        }
        if (samples == null) {
            return null;
        }
        Set<FixedDecimalRange> samples2 = samples.getSamples();
        FixedDecimalRange range = samples2.iterator().next();
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

    private IntervalFormat intervalFormat = new IntervalFormat();

    private static Calendar generatingCalendar = Calendar.getInstance(ULocale.US);

    private static Date getDate(int year, int month, int date, int hour, int minute, int second) {
        synchronized (generatingCalendar) {
            generatingCalendar.setTimeZone(GMT_ZONE_SAMPLE);
            generatingCalendar.set(year, month, date, hour, minute, second);
            return generatingCalendar.getTime();
        }
    }

    private static Date FIRST_INTERVAL = getDate(2008, 1, 13, 5, 7, 9);
    private static Map<String, Date> SECOND_INTERVAL = CldrUtility.asMap(new Object[][] {
        { "G", getDate(1009, 2, 14, 17, 8, 10) }, // "G" mostly useful for calendars that have short eras, like Japanese
        { "y", getDate(2009, 2, 14, 17, 8, 10) },
        { "M", getDate(2008, 2, 14, 17, 8, 10) },
        { "d", getDate(2008, 1, 14, 17, 8, 10) },
        { "a", getDate(2008, 1, 13, 17, 8, 10) },
        { "h", getDate(2008, 1, 13, 6, 8, 10) },
        { "m", getDate(2008, 1, 13, 5, 8, 10) }
    });

    private String handleIntervalFormats(XPathParts parts, String value) {
        if (!parts.getAttributeValue(3, "type").equals("gregorian")) {
            return null;
        }
        if (parts.getElement(6).equals("intervalFormatFallback")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            String fallbackFormat = invertBackground(setBackground(value));
            return format(fallbackFormat, dateFormat.format(FIRST_INTERVAL),
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
            "alternateQuotationEnd", "quotationEnd" };
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
        String example = cldrFile
            .getStringValue("//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"gregorian\"]");
        // NOTE: the example provided here is partially in English because we don't
        // have a translated conversational example in CLDR.
        return invertBackground(format("{0}They said {1}" + example + "{2}.{3}", (Object[]) quotes));
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

    private String handleRegularListPatterns(XPathParts parts, String value, ListTypeLength listTypeLength) {
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
            listTypeLength.getPath(), patternType, value, territory1, territory2, territory3, territory4);
    }

    private String handleDurationListPatterns(XPathParts parts, String value, UnitLength unitWidth) {
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
            unitWidth.listTypeLength.getPath(), patternType, value, duration1, duration2, duration3, duration4);
    }

    public enum UnitLength {
        LONG(ListTypeLength.UNIT_WIDE), SHORT(ListTypeLength.UNIT_SHORT), NARROW(ListTypeLength.UNIT_NARROW);
        final String typeString;
        final ListTypeLength listTypeLength;

        UnitLength(ListTypeLength listTypeLength) {
            typeString = "[@type=\"" + name().toLowerCase(Locale.ENGLISH) + "\"]";
            this.listTypeLength = listTypeLength;
        }

        public static UnitLength from(String listPatternType) {
            if (listPatternType.equals("unit")) {
                return UnitLength.LONG;
            } else if (listPatternType.equals("unit-narrow")) {
                return UnitLength.NARROW;
            } else if (listPatternType.equals("unit-short")) {
                return UnitLength.SHORT;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String getFormattedUnit(String unitType, UnitLength unitWidth, FixedDecimal unitAmount) {
        DecimalFormat numberFormat = icuServiceBuilder.getNumberFormat(1);
        return getFormattedUnit(unitType, unitWidth, unitAmount, numberFormat.format(unitAmount));
    }

    @SuppressWarnings("deprecation")
    private String getFormattedUnit(String unitType, UnitLength unitWidth, double unitAmount) {
        return getFormattedUnit(unitType, unitWidth, new FixedDecimal(unitAmount));
    }

    @SuppressWarnings("deprecation")
    private String getFormattedUnit(String unitType, UnitLength unitWidth, FixedDecimal unitAmount, String formattedUnitAmount) {
        String form = this.pluralInfo.getPluralRules().select(unitAmount);
        String pathFormat = "//ldml/units/unitLength" + unitWidth.typeString
            + "/unit[@type=\"{0}\"]/unitPattern[@count=\"{1}\"]";
        return format(getValueFromFormat(pathFormat, unitType, form), formattedUnitAmount);
    }

    //ldml/listPatterns/listPattern/listPatternPart[@type="2"] — And
    //ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"] Short And
    //ldml/listPatterns/listPattern[@type="or"]/listPatternPart[@type="2"] or list
    //ldml/listPatterns/listPattern[@type="unit"]/listPatternPart[@type="2"]
    //ldml/listPatterns/listPattern[@type="unit-short"]/listPatternPart[@type="2"]
    //ldml/listPatterns/listPattern[@type="unit-narrow"]/listPatternPart[@type="2"]

    private String longListPatternExample(String listPathFormat, String patternType, String value, String... items) {
        String doublePattern = getPattern(listPathFormat, "2", patternType, value);
        String startPattern = getPattern(listPathFormat, "start", patternType, value);
        String middlePattern = getPattern(listPathFormat, "middle", patternType, value);
        String endPattern = getPattern(listPathFormat, "end", patternType, value);
        /*
         * DateTimePatternGenerator.FormatParser is deprecated, but deprecated in ICU is
         * also used to mark internal methods (which are OK for us to use in CLDR).
         */
        @SuppressWarnings("deprecation")
        ListFormatter listFormatter = new ListFormatter(doublePattern, startPattern, middlePattern, endPattern);
        String example = listFormatter.format((Object[]) items);
        return invertBackground(example);
    }

    /**
     * Helper method for handleListPatterns. Returns the pattern to be used for
     * a specified pattern type.
     *
     * @param pathFormat
     * @param pathPatternType
     * @param valuePatternType
     * @param value
     * @return
     */
    private String getPattern(String pathFormat, String pathPatternType, String valuePatternType, String value) {
        return valuePatternType.equals(pathPatternType) ? setBackground(value) : getValueFromFormat(pathFormat, pathPatternType);
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
            String xpath = "//ldml/dates/calendars/calendar[@type=\"{0}\"]/months/monthContext[@type=\"{1}\"]/monthWidth[@type=\"{2}\"]/month[@type=\"8\"]";
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

        SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendar, 0, DateFormat.MEDIUM, null);
        String zone = cldrFile.getStringValue("//ldml/dates/timeZoneNames/gmtZeroFormat");
        String result = format(value, setBackground(sdf.format(DATE_SAMPLE)), setBackground(zone));
        return result;
    }

    private class IntervalFormat {
        @SuppressWarnings("deprecation")
        DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
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
                System.err.println("Caught NullPointerException in IntervalFormat.setPattern, pattern = " + pattern);
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
    static final List<FixedDecimal> CURRENCY_SAMPLES = Arrays.asList(
        new FixedDecimal(1.23),
        new FixedDecimal(0),
        new FixedDecimal(2.34),
        new FixedDecimal(3.45),
        new FixedDecimal(5.67),
        new FixedDecimal(1));

    @SuppressWarnings("deprecation")
    private String formatCountValue(String xpath, XPathParts parts, String value) {
        if (!parts.containsAttribute("count")) { // no examples for items that don't format
            return null;
        }
        final PluralInfo plurals = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
        PluralRules pluralRules = plurals.getPluralRules();

        String unitType = parts.getAttributeValue(-2, "type");
        if (unitType == null) {
            unitType = "USD"; // sample for currency pattern
        }
        final boolean isPattern = parts.contains("unitPattern");
        final boolean isCurrency = !parts.contains("units");

        Count count = null;
        final LinkedHashSet<FixedDecimal> exampleCount = new LinkedHashSet<>();
        exampleCount.addAll(CURRENCY_SAMPLES);
        String countString = parts.getAttributeValue(-1, "count");
        if (countString == null) {
            // count = Count.one;
            return null;
        } else {
            try {
                count = Count.valueOf(countString);
            } catch (Exception e) {
                return null; // counts like 0
            }
        }

        // we used to just get the samples for the given keyword, but that doesn't work well any more.
        getStartEndSamples(pluralRules.getDecimalSamples(countString, SampleType.INTEGER), exampleCount);
        getStartEndSamples(pluralRules.getDecimalSamples(countString, SampleType.DECIMAL), exampleCount);

        String result = "";
        DecimalFormat currencyFormat = icuServiceBuilder.getCurrencyFormat(unitType);
        int decimalCount = currencyFormat.getMinimumFractionDigits();

        // we will cycle until we have (at most) two examples.
        Set<FixedDecimal> examplesSeen = new HashSet<>();
        int maxCount = 2;
        main:
            // If we are a currency, we will try to see if we can set the decimals to match.
            // but if nothing works, we will just use a plain sample.
            for (int phase = 0; phase < 2; ++phase) {
                for (FixedDecimal example : exampleCount) {
                    // we have to first see whether we have a currency. If so, we have to see if the count works.

                    if (isCurrency && phase == 0) {
                        example = new FixedDecimal(example.getSource(), decimalCount);
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
                        resultItem = formatCurrency(value, currency, isPattern, isCurrency, count, example);
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
    static public void getStartEndSamples(PluralRules.FixedDecimalSamples samples, Set<FixedDecimal> target) {
        if (samples != null) {
            for (FixedDecimalRange item : samples.getSamples()) {
                target.add(item.start);
                target.add(item.end);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String formatCurrency(String value, String unitType, final boolean isPattern, final boolean isCurrency, Count count,
        FixedDecimal example) {
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
            unitDecimalFormat.setMaximumFractionDigits(example.getVisibleDecimalDigitCount());
            unitDecimalFormat.setMinimumFractionDigits(example.getVisibleDecimalDigitCount());

            String formattedNumber = unitDecimalFormat.format(example.getSource());
            unitPatternFormat.setFormatByArgumentIndex(0, unitDecimalFormat);
            resultItem = unitPattern.replace("{0}", formattedNumber).replace("{1}", unitName);

            if (isPattern) {
                resultItem = invertBackground(resultItem);
            }
        }
        return resultItem;
    }

    private String addExampleResult(String resultItem, String resultToAddTo) {
        if (resultToAddTo.length() != 0) {
            resultToAddTo += exampleSeparatorSymbol;
        }
        resultToAddTo += resultItem;
        return resultToAddTo;
    }

    private String getUnitPattern(String unitType, final boolean isCurrency, Count count) {
        return cldrFile.getStringValue(isCurrency
            ? "//ldml/numbers/currencyFormats/unitPattern"  + countAttribute(count)
                : "//ldml/units/unit[@type=\"" + unitType + "\"]/unitPattern" + countAttribute(count));
    }

    private String getUnitName(String unitType, final boolean isCurrency, Count count) {
        return cldrFile.getStringValue(isCurrency
            ? "//ldml/numbers/currencies/currency[@type=\"" + unitType + "\"]/displayName" + countAttribute(count)
                : "//ldml/units/unit[@type=\"" + unitType + "\"]/unitPattern" + countAttribute(count));
    }

    public String countAttribute(Count count) {
        return "[@count=\"" + count + "\"]";
    }

    private String handleNumberSymbol(XPathParts parts, String value) {
        String symbolType = parts.getElement(-1);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present
        int index = 1;// dec/percent/sci
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
            originalValue = cldrFile.getWinningValue(parts.addRelative("../percentSign").toString());
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
            formattedValue = backgroundEndSymbol + value + digits[1] + digits[0] + backgroundStartSymbol + startSupSymbol;
            example = x.format(numberSample);
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
                    result = value;
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
                    return result; // fail, skip
                }
            } else {
                String countryName = setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode));
                boolean singleZone = !supplementalDataInfo.getMultizones().contains(countryCode);
                // we show just country for singlezone countries
                if (singleZone) {
                    result = countryName;
                } else {
                    if (value == null) {
                        value = TimezoneFormatter.getFallbackName(timezone);
                    }
                    // otherwise we show the fallback with exemplar
                    String fallback = setBackground(cldrFile
                        .getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat"));
                    // ldml/dates/timeZoneNames/zone[@type="America/Los_Angeles"]/exemplarCity

                    result = format(fallback, value, countryName);
                }
                // format with "{0} Time" or equivalent.
                String timeFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat"));
                result = format(timeFormat, result);
            }
        } else if (parts.contains("zone")) { // {0} Time
            result = value;
        } else if (parts.contains("regionFormat")) { // {0} Time
            result = format(value, setBackground(cldrFile.getName(CLDRFile.TERRITORY_NAME, "JP")));
            result = addExampleResult(
                format(value, setBackground(cldrFile.getWinningValue(EXEMPLAR_CITY_LOS_ANGELES))), result);
        } else if (parts.contains("fallbackFormat")) { // {1} ({0})
            String central = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/metazone[@type=\"America_Central\"]/long/generic"));
            String cancun = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"America/Cancun\"]/exemplarCity"));
            result = format(value, cancun, central);
        } else if (parts.contains("gmtFormat")) { // GMT{0}
            result = getGMTFormat(null, value, -8);
        } else if (parts.contains("hourFormat")) { // +HH:mm;-HH:mm
            result = getGMTFormat(value, null, -8);
        } else if (parts.contains("metazone") && !parts.contains("commonlyUsed")) { // Metazone string
            if (value != null && value.length() > 0) {
                result = getMZTimeFormat() + " " + value;
            } else {
                // TODO check for value
                if (parts.contains("generic")) {
                    String metazone_name = parts.getAttributeValue(3, "type");
                    String timezone = supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    String countryCode = supplementalDataInfo.getZone_territory(timezone);
                    String regionFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat");
                    String fallbackFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat");
                    String exemplarCity = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\""
                        + timezone + "\"]/exemplarCity");
                    if (exemplarCity == null) {
                        exemplarCity = timezone.substring(timezone.lastIndexOf('/') + 1).replace('_', ' ');
                    }
                    String countryName = cldrFile
                        .getWinningValue("//ldml/localeDisplayNames/territories/territory[@type=\"" + countryCode
                            + "\"]");
                    boolean singleZone = !(supplementalDataInfo.getMultizones().contains(countryCode));

                    if (singleZone) {
                        result = setBackground(getMZTimeFormat() + " " +
                            format(regionFormat, countryName));
                    } else {
                        result = setBackground(getMZTimeFormat() + " " +
                            format(fallbackFormat, exemplarCity, countryName));
                    }
                } else {
                    String gmtFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
                    String hourFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
                    String metazone_name = parts.getAttributeValue(3, "type");
                    // String tz_string = supplementalData.resolveParsedMetazone(metazone_name,"001");
                    String tz_string = supplementalDataInfo.getZoneForMetazoneByRegion(metazone_name, "001");
                    TimeZone currentZone = TimeZone.getTimeZone(tz_string);
                    int tzOffset = currentZone.getRawOffset();
                    if (parts.contains("daylight")) {
                        tzOffset += currentZone.getDSTSavings();
                    }
                    int MILLIS_PER_MINUTE = 1000 * 60;
                    int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
                    int tm_hrs = tzOffset / MILLIS_PER_HOUR;
                    int tm_mins = (tzOffset % MILLIS_PER_HOUR) / 60000; // millis per minute
                    result = setBackground(getMZTimeFormat() + " "
                        + getGMTFormat(hourFormat, gmtFormat, tm_hrs, tm_mins));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private String handleDateFormatItem(String xpath, String value) {

        String fullpath = cldrFile.getFullXPath(xpath);
        XPathParts parts = XPathParts.getFrozenInstance(fullpath);
        String calendar = parts.findAttributeValue("calendar", "type");

        if (parts.contains("dateTimeFormat")) {
            String dateFormatXPath = cldrFile.getWinningPath(xpath.replaceAll("dateTimeFormat", "dateFormat"));
            String timeFormatXPath = cldrFile.getWinningPath(xpath.replaceAll("dateTimeFormat", "timeFormat"));
            String dateFormatValue = cldrFile.getWinningValue(dateFormatXPath);
            String timeFormatValue = cldrFile.getWinningValue(timeFormatXPath);
            parts = XPathParts.getFrozenInstance(cldrFile.getFullXPath(dateFormatXPath));
            String dateNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            parts = XPathParts.getFrozenInstance(cldrFile.getFullXPath(timeFormatXPath));
            String timeNumbersOverride = parts.findAttributeValue("pattern", "numbers");
            SimpleDateFormat df = icuServiceBuilder.getDateFormat(calendar, dateFormatValue, dateNumbersOverride);
            SimpleDateFormat tf = icuServiceBuilder.getDateFormat(calendar, timeFormatValue, timeNumbersOverride);
            df.setTimeZone(ZONE_SAMPLE);
            tf.setTimeZone(ZONE_SAMPLE);
            String dfResult = "'" + df.format(DATE_SAMPLE) + "'";
            String tfResult = "'" + tf.format(DATE_SAMPLE) + "'";
            SimpleDateFormat dtf = icuServiceBuilder.getDateFormat(calendar,
                MessageFormat.format(value, (Object[]) new String[] { setBackground(tfResult), setBackground(dfResult) }));
            return dtf.format(DATE_SAMPLE);
        } else {
            String id = parts.findAttributeValue("dateFormatItem", "id");
            if ("NEW".equals(id) || value == null) {
                return startItalicSymbol + "n/a" + endItalicSymbol;
            } else {
                String numbersOverride = parts.findAttributeValue("pattern", "numbers");
                SimpleDateFormat sdf = icuServiceBuilder.getDateFormat(calendar, value, numbersOverride);
                sdf.setTimeZone(ZONE_SAMPLE);
                String defaultNumberingSystem = cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
                String timeSeparator = cldrFile.getWinningValue("//ldml/numbers/symbols[@numberSystem='" + defaultNumberingSystem + "']/timeSeparator");
                DateFormatSymbols dfs = sdf.getDateFormatSymbols();
                dfs.setTimeSeparatorString(timeSeparator);
                sdf.setDateFormatSymbols(dfs);
                if (id == null || id.indexOf('B') < 0) {
                    return sdf.format(DATE_SAMPLE);
                } else {
                    List<String> examples = new ArrayList<>();
                    examples.add(sdf.format(DATE_SAMPLE3));
                    examples.add(sdf.format(DATE_SAMPLE));
                    examples.add(sdf.format(DATE_SAMPLE4));
                    return formatExampleList(examples.toArray(new String[examples.size()]));
                }
            }
        }
    }

    /**
     * Creates examples for currency formats.
     *
     * @param value
     * @return
     */
    private String handleCurrencyFormat(XPathParts parts, String value) {

        String territory = getDefaultTerritory();

        String currency = supplementalDataInfo.getDefaultCurrency(territory);
        String checkPath = "//ldml/numbers/currencies/currency[@type=\"" + currency + "\"]/symbol";
        String currencySymbol = cldrFile.getWinningValue(checkPath);
        String numberSystem = parts.getAttributeValue(2, "numberSystem"); // null if not present

        DecimalFormat df = icuServiceBuilder.getCurrencyFormat(currency, currencySymbol, numberSystem);
        df.applyPattern(value);

        String countValue = parts.getAttributeValue(-1, "count");
        if (countValue != null) {
            return formatCountDecimal(df, countValue);
        }

        double sampleAmount = 1295.00;
        String example = formatNumber(df, sampleAmount);
        example = addExampleResult(formatNumber(df, -sampleAmount), example);

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
                        territory = "EG"; // Use Egypt as territory for examples in ar locale, since its default content is ar_001.
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
    private String handleDecimalFormat(XPathParts parts, String value) {
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
        String example = formatNumber(numberFormat, sampleNum1);
        example = addExampleResult(formatNumber(numberFormat, sampleNum2), example);
        // have positive and negative
        example = addExampleResult(formatNumber(numberFormat, -sampleNum2), example);
        return example;
    }

    private String formatCountDecimal(DecimalFormat numberFormat, String countValue) {
        Count count;
        try {
            count = Count.valueOf(countValue);
        } catch (Exception e) {
            String locale = getCldrFile().getLocaleID();
            PluralInfo pluralInfo = supplementalDataInfo.getPlurals(locale);
            count = pluralInfo.getCount(new FixedDecimal(countValue));
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
     * Calculates a numerical example to use for the specified pattern using
     * brute force (there should be a more elegant way to do this).
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
            DecimalFormat x = icuServiceBuilder.getCurrencyFormat(currency, value);
            result = x.format(NUMBER_SAMPLE);
            result = setBackground(result).replace(value, backgroundEndSymbol + value + backgroundStartSymbol);
            return result;
        } else if (parts.contains("displayName")) {
            return formatCountValue(xpath, parts, value);
        }
        return null;
    }

    private String handleDateRangePattern(String value) {
        String result;
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", 2, 0);
        result = format(value, setBackground(dateFormat.format(DATE_SAMPLE)),
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
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="language"]
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="script"]
            //ldml/localeDisplayNames/codePatterns/codePattern[@type="territory"]
            String type = parts.getAttributeValue(-1, "type");
            result = format(value, setBackground(
                type.equals("language") ? "ace"
                    : type.equals("script") ? "Avst"
                        : type.equals("territory") ? "057" : "CODE"));
        } else if (parts.contains("localeDisplayPattern")) {
            //ldml/localeDisplayNames/localeDisplayPattern/localePattern
            //ldml/localeDisplayNames/localeDisplayPattern/localeSeparator
            //ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern
            String element = parts.getElement(-1);
            value = setBackground(value);
            String localeKeyTypePattern = getLocaleDisplayPattern("localeKeyTypePattern", element, value);
            String localePattern = getLocaleDisplayPattern("localePattern", element, value);
            String localeSeparator = getLocaleDisplayPattern("localeSeparator", element, value);

            List<String> locales = new ArrayList<>();
            if (element.equals("localePattern")) {
                locales.add("uz-AF");
            }
            locales.add(element.equals("localeKeyTypePattern") ? "uz-Arab-u-tz-etadd" : "uz-Arab-AF");
            locales.add("uz-Arab-AF-u-tz-etadd-nu-arab");
            String[] examples = new String[locales.size()];
            for (int i = 0; i < locales.size(); i++) {
                examples[i] = invertBackground(cldrFile.getName(locales.get(i), false,
                    localeKeyTypePattern, localePattern, localeSeparator));
            }
            result = formatExampleList(examples);
        } else if (parts.contains("languages") || parts.contains("scripts") || parts.contains("territories")) {
            //ldml/localeDisplayNames/languages/language[@type="ar"]
            //ldml/localeDisplayNames/scripts/script[@type="Arab"]
            //ldml/localeDisplayNames/territories/territory[@type="CA"]
            String type = parts.getAttributeValue(-1, "type");
            if (type.contains("_")) {
                if (value != null && !value.equals(type)) {
                    result = value;
                } else {
                    result = cldrFile.getConstructedBaileyValue(xpath, null, null);
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
                        languageName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, ltp.getLanguage()));
                        if (languageName == null) {
                            languageName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, "en"));
                        }
                        if (languageName == null) {
                            languageName = ltp.getLanguage();
                        }
                    }
                    if (scriptName == null) {
                        scriptName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.SCRIPT_NAME, ltp.getScript()));
                        if (scriptName == null) {
                            scriptName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.SCRIPT_NAME, "Latn"));
                        }
                        if (scriptName == null) {
                            scriptName = ltp.getScript();
                        }
                    }
                    if (territoryName == null) {
                        territoryName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.TERRITORY_NAME, ltp.getRegion()));
                        if (territoryName == null) {
                            territoryName = cldrFile.getStringValueWithBailey(CLDRFile.getKey(CLDRFile.TERRITORY_NAME, "US"));
                        }
                        if (territoryName == null) {
                            territoryName = ltp.getRegion();
                        }
                    }
                    languageName = languageName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                    scriptName = scriptName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                    territoryName = territoryName.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');

                    String localePattern = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
                    String localeSeparator = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator");
                    String scriptTerritory = format(localeSeparator, scriptName, territoryName);
                    if (!nameType.equals("script")) {
                        examples.add(invertBackground(format(localePattern, languageName, territoryName)));
                    }
                    if (!nameType.equals("territory")) {
                        examples.add(invertBackground(format(localePattern, languageName, scriptName)));
                    }
                    examples.add(invertBackground(format(localePattern, languageName, scriptTerritory)));
                }
                Output<String> pathWhereFound = null;
                if (isStandAloneValue
                    || cldrFile.getStringValueWithBailey(xpath + ALT_STAND_ALONE, pathWhereFound = new Output<>(), null) == null
                    || !pathWhereFound.value.contains(ALT_STAND_ALONE)) {
                    // only do this if either it is a stand-alone form,
                    // or it isn't and there is no separate stand-alone form
                    // the extra check after the == null is to make sure that we don't have sideways inheritance
                    String codePattern = cldrFile.getStringValueWithBailey("//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"" + nameType + "\"]");
                    examples.add(invertBackground(format(codePattern, value)));
                }
                result = formatExampleList(examples.toArray(new String[examples.size()]));
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

    public static final String unchainException(Exception e) {
        String stackStr = "[unknown stack]<br>";
        try {
            StringWriter asString = new StringWriter();
            e.printStackTrace(new PrintWriter(asString));
            stackStr = "<pre>" + asString.toString() + "</pre>";
        } catch (Throwable tt) {
            // ...
        }
        return stackStr;
    }

    /**
     * Put a background on an item, skipping enclosed patterns.
     * @param sampleTerritory
     * @return
     */
    private String setBackground(String inputPattern) {
        if (inputPattern == null) {
            return "?";
        }
        Matcher m = PARAMETER.matcher(inputPattern);
        return backgroundStartSymbol + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
        + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     * @param patternToEmbed
     * @param sampleTerritory
     * @return
     */
    private String setBackgroundExceptMatch(String input, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(input);
        return backgroundStartSymbol + m.replaceAll(backgroundEndSymbol + "$1" + backgroundStartSymbol)
        + backgroundEndSymbol;
    }

    /**
     * Put a background on an item, skipping enclosed patterns, except for {0}
     *
     * @param patternToEmbed
     *            TODO
     * @param sampleTerritory
     *
     * @return
     */
    private String setBackgroundOnMatch(String inputPattern, Pattern patternToEmbed) {
        Matcher m = patternToEmbed.matcher(inputPattern);
        return m.replaceAll(backgroundStartSymbol + "$1" + backgroundEndSymbol);
    }

    /**
     * This adds the transliteration of a result in case it has one (i.e. sr_Cyrl -> sr_Latn).
     *
     * @param input
     *            string with special characters from setBackground.
     * @param value
     *            value to be transliterated
     * @return string with attached transliteration if there is one.
     */
    private String addTransliteration(String input, String value) {
        if (value == null) {
            return input;
        }
        for (LocaleTransform localeTransform : LocaleTransform.values()) {

            String locale = cldrFile.getLocaleID();

            if (!(localeTransform.getInputLocale().equals(locale))) {
                continue;
            }

            Factory factory = CONFIG.getCldrFactory();
            CLDRFileTransformer transformer = new CLDRFileTransformer(factory, CLDRPaths.COMMON_DIRECTORY + "transforms/");
            Transliterator transliterator = transformer.loadTransliterator(localeTransform);
            final String transliterated = transliterator.transliterate(value);
            if (!transliterated.equals(value)) {
                return backgroundStartSymbol + "[ " + transliterated + " ]" + backgroundEndSymbol + exampleSeparatorSymbol + input;
            }
        }
        return input;
    }

    /**
     * This is called just before we return a result. It fixes the special characters that were added by setBackground.
     *
     * @param input string with special characters from setBackground.
     * @param invert
     * @return string with HTML for the background.
     */
    private String finalizeBackground(String input) {
        return input == null
            ? input
                : exampleStart +
                TransliteratorUtilities.toHTML.transliterate(input)
                .replace(backgroundStartSymbol + backgroundEndSymbol, "")
                // remove null runs
                .replace(backgroundEndSymbol + backgroundStartSymbol, "")
                // remove null runs
                .replace(backgroundStartSymbol, backgroundStart)
                .replace(backgroundEndSymbol, backgroundEnd)
                .replace(exampleSeparatorSymbol, exampleEnd + exampleStart)
                .replace(startItalicSymbol, startItalic)
                .replace(endItalicSymbol, endItalic)
                .replace(startSupSymbol, startSup)
                .replace(endSupSymbol, endSup)
                + exampleEnd;
    }

    private String invertBackground(String input) {
        return input == null ? null
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

    public static final Pattern PARAMETER = PatternCache.get("(\\{(?:0|[1-9][0-9]*)\\})");
    public static final Pattern PARAMETER_SKIP0 = PatternCache.get("(\\{[1-9][0-9]*\\})");
    public static final Pattern ALL_DIGITS = PatternCache.get("(\\p{Nd}+(.\\p{Nd}+)?)");

    /**
     * Utility to format using a gmtHourString, gmtFormat, and an integer hours. We only need the hours because that's
     * all
     * the TZDB IDs need. Should merge this eventually into TimeZoneFormatter and call there.
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
            gmtFormat = setBackground(cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat"));
        }
        String[] plusMinus = gmtHourString.split(";");

        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian", plusMinus[hours >= 0 ? 0 : 1]);
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
        String timeFormat = cldrFile
            .getWinningValue(
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

    public static final char TEXT_VARIANT = '\uFE0E';

    /**
     * Return a help string, in html, that should be shown in the Zoomed view.
     * Presumably at the end of each help section is something like: <br>
     * &lt;br&gt;For more information, see <a
     * href='http://unicode.org/cldr/wiki?SurveyToolHelp/characters'>help</a>. <br>
     * The result is valid HTML. Set listPlaceholders to true to include a
     * HTML-formatted table of all placeholders required in the value.<br>
     * TODO: add more help, and modify to get from property or xml file for easy
     * modification.
     *
     * @return null if none available.
     */
    public synchronized String getHelpHtml(String xpath, String value, boolean listPlaceholders) {

        // lazy initialization

        if (pathDescription == null) {
            Map<String, List<Set<String>>> starredPaths = new HashMap<>();
            Map<String, String> extras = new HashMap<>();

            this.pathDescription = new PathDescription(supplementalDataInfo, englishFile, extras, starredPaths,
                PathDescription.ErrorHandling.CONTINUE);

            if (helpMessages == null) {
                helpMessages = new HelpMessages("test_help_messages.html");
            }
        }

        // now get the description

        Level level = CONFIG.getCoverageInfo().getCoverageLevel(xpath, cldrFile.getLocaleID());
        String description = pathDescription.getDescription(xpath, value, level, null);
        if (description == null || description.equals("SKIP")) {
            return null;
        }
        // http://cldr.org/translation/timezones
        int start = 0;
        StringBuilder buffer = new StringBuilder();
        while (URLMatcher.reset(description).find(start)) {
            final String url = URLMatcher.group();
            buffer
            .append(TransliteratorUtilities.toHTML.transliterate(description.substring(start, URLMatcher.start())))
            .append("<a target='CLDR-ST-DOCS' href='")
            .append(url)
            .append("'>")
            .append(url)
            .append("</a>");
            start = URLMatcher.end();
        }
        buffer.append(TransliteratorUtilities.toHTML.transliterate(description.substring(start)));

        if (listPlaceholders) {
            buffer.append(pathDescription.getPlaceholderDescription(xpath));
        }
        if (xpath.startsWith("//ldml/annotations/annotation")) {
            XPathParts emoji = XPathParts.getFrozenInstance(xpath);
            String cp = emoji.getAttributeValue(-1, "cp");
            String minimal = Utility.hex(cp.replace("", "")).replace(',', '_').toLowerCase(Locale.ROOT);
            buffer.append("<br><img height='64px' width='auto' src='images/emoji/emoji_" + minimal + ".png'>");
        }

        return buffer.toString();
    }

    public synchronized String getHelpHtml(String xpath, String value) {
        return getHelpHtml(xpath, value, false);
    }

    public static String simplify(String exampleHtml) {
        return simplify(exampleHtml, false);
    }

    public static String simplify(String exampleHtml, boolean internal) {
        return exampleHtml == null ? null
            : internal ? "〖" + exampleHtml
                .replace("", "❬")
            .replace("", "❭") + "〗"
            : exampleHtml
            .replace("<div class='cldr_example'>", "〖")
            .replace("</div>", "〗")
            .replace("<span class='cldr_substituted'>", "❬")
            .replace("</span>", "❭");
    }

    HelpMessages helpMessages;
}
