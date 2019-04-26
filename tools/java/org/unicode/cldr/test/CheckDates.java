package org.unicode.cldr.test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PreferredAndAllowedHour;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.props.UnicodeProperty.PatternMatcher;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class CheckDates extends FactoryCheckCLDR {
    static boolean GREGORIAN_ONLY = CldrUtility.getProperty("GREGORIAN", false);

    ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
    NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
    PatternMatcher m;
    DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
    DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.getEmptyInstance();
    private CoverageLevel2 coverageLevel;
    private SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

    // Use the width of the character "0" as the basic unit for checking widths
    // It's not perfect, but I'm not sure that anything can be. This helps us
    // weed out some false positives in width checking, like 10月 vs. 十月
    // in Chinese, which although technically longer, shouldn't trigger an
    // error.
    private static final int REFCHAR = ApproximateWidth.getWidth("0");

    private Level requiredLevel;
    private String language;
    private String territory;

    private DayPeriodInfo dateFormatInfoFormat;

    static String[] samples = {
        // "AD 1970-01-01T00:00:00Z",
        // "BC 4004-10-23T07:00:00Z", // try a BC date: creation according to Ussher & Lightfoot. Assuming garden of
        // eden 2 hours ahead of UTC
        "2005-12-02 12:15:16",
        // "AD 2100-07-11T10:15:16Z",
    }; // keep aligned with following
    static String SampleList = "{0}"
    // + Utility.LINE_SEPARATOR + "\t\u200E{1}\u200E" + Utility.LINE_SEPARATOR + "\t\u200E{2}\u200E" +
    // Utility.LINE_SEPARATOR + "\t\u200E{3}\u200E"
    ; // keep aligned with previous

    private static final String DECIMAL_XPATH = "//ldml/numbers/symbols[@numberSystem='latn']/decimal";
    private static final Pattern HOUR_SYMBOL = PatternCache.get("H{1,2}");
    private static final Pattern MINUTE_SYMBOL = PatternCache.get("mm");
    private static final Pattern YEAR_FIELDS = PatternCache.get("(y|Y|u|U|r){1,5}");

    static String[] calTypePathsToCheck = {
        "//ldml/dates/calendars/calendar[@type=\"buddhist\"]",
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]",
        "//ldml/dates/calendars/calendar[@type=\"hebrew\"]",
        "//ldml/dates/calendars/calendar[@type=\"islamic\"]",
        "//ldml/dates/calendars/calendar[@type=\"japanese\"]",
        "//ldml/dates/calendars/calendar[@type=\"roc\"]",
    };
    static String[] calSymbolPathsWhichNeedDistinctValues = {
        // === for months, days, quarters - format wide & abbrev sets must have distinct values ===
        "/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month",
        "/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month",
        "/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day",
        "/days/dayContext[@type=\"format\"]/dayWidth[@type=\"short\"]/day",
        "/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day",
        "/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"abbreviated\"]/quarter",
        "/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter",
        // === for dayPeriods - all values for a given context/width must be distinct ===
        "/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod",
        "/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod",
        "/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod",
        "/dayPeriods/dayPeriodContext[@type=\"stand-alone\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod",
        "/dayPeriods/dayPeriodContext[@type=\"stand-alone\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod",
        "/dayPeriods/dayPeriodContext[@type=\"stand-alone\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod",
        // === for eras - all values for a given context/width should be distinct (warning) ===
        "/eras/eraNames/era",
        "/eras/eraAbbr/era", // Hmm, root eraAbbr for japanese has many dups, should we change them or drop this test?
        "/eras/eraNarrow/era", // We may need to allow dups here too
    };

    // The following calendar symbol sets need not have distinct values
    // "/months/monthContext[@type=\"format\"]/monthWidth[@type=\"narrow\"]/month",
    // "/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"abbreviated\"]/month",
    // "/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"narrow\"]/month",
    // "/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"wide\"]/month",
    // "/days/dayContext[@type=\"format\"]/dayWidth[@type=\"narrow\"]/day",
    // "/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"abbreviated\"]/day",
    // "/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day",
    // "/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"wide\"]/day",
    // "/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"narrow\"]/quarter",
    // "/quarters/quarterContext[@type=\"stand-alone\"]/quarterWidth[@type=\"abbreviated\"]/quarter",
    // "/quarters/quarterContext[@type=\"stand-alone\"]/quarterWidth[@type=\"narrow\"]/quarter",
    // "/quarters/quarterContext[@type=\"stand-alone\"]/quarterWidth[@type=\"wide\"]/quarter",

    // The above are followed by trailing pieces such as
    // "[@type=\"am\"]",
    // "[@type=\"sun\"]",
    // "[@type=\"0\"]",
    // "[@type=\"1\"]",
    // "[@type=\"12\"]",

    // Map<String, Set<String>> calPathsToSymbolSets;
    // Map<String, Map<String, String>> calPathsToSymbolMaps = new HashMap<String, Map<String, String>>();

    public CheckDates(Factory factory) {
        super(factory);
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
        // the following is a hack to work around a bug in ICU4J (the snapshot, not the released version).
        try {
            bi = BreakIterator.getCharacterInstance(new ULocale(cldrFileToCheck.getLocaleID()));
        } catch (RuntimeException e) {
            bi = BreakIterator.getCharacterInstance(new ULocale(""));
        }
        CLDRFile resolved = getResolvedCldrFileToCheck();
        flexInfo = new FlexibleDateFromCLDR(); // ought to just clear(), but not available.
        flexInfo.set(resolved);

        // load decimal path specially
        String decimal = resolved.getWinningValue(DECIMAL_XPATH);
        if (decimal != null) {
            flexInfo.checkFlexibles(DECIMAL_XPATH, decimal, DECIMAL_XPATH);
        }

        String localeID = cldrFileToCheck.getLocaleID();
        LocaleIDParser lp = new LocaleIDParser();
        territory = lp.set(localeID).getRegion();
        language = lp.getLanguage();
        if (territory == null || territory.length() == 0) {
            if (language.equals("root")) {
                territory = "001";
            } else {
                CLDRLocale loc = CLDRLocale.getInstance(localeID);
                CLDRLocale defContent = sdi.getDefaultContentFromBase(loc);
                if (defContent == null) {
                    territory = "001";
                } else {
                    territory = defContent.getCountry();
                }
                // Set territory for 12/24 hour clock to Egypt (12 hr) for ar_001
                // instead of 24 hour (exception).
                if (territory.equals("001") && language.equals("ar")) {
                    territory = "EG";
                }
            }
        }
        coverageLevel = CoverageLevel2.getInstance(sdi, localeID);
        requiredLevel = options.getRequiredLevel(localeID);

        // load gregorian appendItems
        for (Iterator<String> it = resolved.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]"); it.hasNext();) {
            String path = it.next();
            String value = resolved.getWinningValue(path);
            String fullPath = resolved.getFullXPath(path);
            try {
                flexInfo.checkFlexibles(path, value, fullPath);
            } catch (Exception e) {
                final String message = e.getMessage();
                CheckStatus item = new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(
                        message.contains("Conflicting fields") ? Subtype.dateSymbolCollision : Subtype.internalError)
                    .setMessage(message);
                possibleErrors.add(item);
            }
            // possibleErrors.add(flexInfo.getFailurePath(path));
        }
        redundants.clear();
        flexInfo.getRedundants(redundants);
        // Set baseSkeletons = flexInfo.gen.getBaseSkeletons(new TreeSet());
        // Set notCovered = new TreeSet(neededFormats);
        // if (flexInfo.preferred12Hour()) {
        // notCovered.addAll(neededHours12);
        // } else {
        // notCovered.addAll(neededHours24);
        // }
        // notCovered.removeAll(baseSkeletons);
        // if (notCovered.size() != 0) {
        // possibleErrors.add(new CheckStatus().setCause(this).setType(CheckCLDR.finalErrorType)
        // .setCheckOnSubmit(false)
        // .setMessage("Missing availableFormats: {0}", new Object[]{notCovered.toString()}));
        // }
        pathsWithConflictingOrder2sample = DateOrder.getOrderingInfo(cldrFileToCheck, resolved, flexInfo.fp);
        if (pathsWithConflictingOrder2sample == null) {
            CheckStatus item = new CheckStatus()
                .setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.internalError)
                .setMessage("DateOrder.getOrderingInfo fails");
            possibleErrors.add(item);
        }

        // calPathsToSymbolMaps.clear();
        // for (String calTypePath: calTypePathsToCheck) {
        // for (String calSymbolPath: calSymbolPathsWhichNeedDistinctValues) {
        // calPathsToSymbolMaps.put(calTypePath.concat(calSymbolPath), null);
        // }
        // }

        dateFormatInfoFormat = sdi.getDayPeriods(Type.format, cldrFileToCheck.getLocaleID());
        return this;
    }

    Map<String, Map<DateOrder, String>> pathsWithConflictingOrder2sample;

    // Set neededFormats = new TreeSet(Arrays.asList(new String[]{
    // "yM", "yMMM", "yMd", "yMMMd", "Md", "MMMd","yQ"
    // }));
    // Set neededHours12 = new TreeSet(Arrays.asList(new String[]{
    // "hm", "hms"
    // }));
    // Set neededHours24 = new TreeSet(Arrays.asList(new String[]{
    // "Hm", "Hms"
    // }));
    /**
     * hour+minute, hour+minute+second (12 & 24)
     * year+month, year+month+day (numeric & string)
     * month+day (numeric & string)
     * year+quarter
     */
    BreakIterator bi;
    FlexibleDateFromCLDR flexInfo;
    Collection<String> redundants = new HashSet<String>();
    Status status = new Status();
    PathStarrer pathStarrer = new PathStarrer();

    private String stripPrefix(String s) {
        if (s != null && s.lastIndexOf(" ") < 3) {
            return s.substring(s.lastIndexOf(" ") + 1);
        }
        return s;
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        if (fullPath == null) {
            return this; // skip paths that we don't have
        }

        if (path.indexOf("/dates") < 0
            || path.endsWith("/default")
            || path.endsWith("/alias")) {
            return this;
        }

        String sourceLocale = getCldrFileToCheck().getSourceLocaleID(path, status);

        if (!path.equals(status.pathWhereFound) || !sourceLocale.equals(getCldrFileToCheck().getLocaleID())) {
            return this;
        }

        if (value == null) {
            return this;
        }

        if (pathsWithConflictingOrder2sample != null) {
            Map<DateOrder, String> problem = pathsWithConflictingOrder2sample.get(path);
            if (problem != null) {
                CheckStatus item = new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.incorrectDatePattern)
                    .setMessage("The ordering of date fields is inconsistent with others: {0}",
                        getValues(getResolvedCldrFileToCheck(), problem.values()));
                result.add(item);
            }
        }

        try {
            if (path.indexOf("[@type=\"abbreviated\"]") >= 0) {
                String pathToWide = path.replace("[@type=\"abbreviated\"]", "[@type=\"wide\"]");
                String wideValue = getCldrFileToCheck().getWinningValueWithBailey(pathToWide);
                if (wideValue != null && isTooMuchWiderThan(value, wideValue)) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.abbreviatedDateFieldTooWide)
                        .setMessage("Abbreviated value \"{0}\" can't be longer than the corresponding wide value \"{1}\"", value,
                            wideValue);
                    result.add(item);
                }
                for (String lgPath : LogicalGrouping.getPaths(getCldrFileToCheck(), path)) {
                    String lgPathValue = getCldrFileToCheck().getWinningValueWithBailey(lgPath);
                    String lgPathToWide = lgPath.replace("[@type=\"abbreviated\"]", "[@type=\"wide\"]");
                    String lgPathWideValue = getCldrFileToCheck().getWinningValueWithBailey(lgPathToWide);
                    // This helps us get around things like "de març" vs. "març" in Catalan
                    String thisValueStripped = stripPrefix(value);
                    String wideValueStripped = stripPrefix(wideValue);
                    String lgPathValueStripped = stripPrefix(lgPathValue);
                    String lgPathWideValueStripped = stripPrefix(lgPathWideValue);
                    boolean thisPathHasPeriod = value.contains(".");
                    boolean lgPathHasPeriod = lgPathValue.contains(".");
                    if (!thisValueStripped.equalsIgnoreCase(wideValueStripped) && !lgPathValueStripped.equalsIgnoreCase(lgPathWideValueStripped) &&
                        thisPathHasPeriod != lgPathHasPeriod) {
                        CheckStatus.Type et = CheckStatus.errorType;
                        if (path.contains("dayPeriod")) {
                            et = CheckStatus.warningType;
                        }
                        CheckStatus item = new CheckStatus()
                            .setCause(this)
                            .setMainType(et)
                            .setSubtype(Subtype.inconsistentPeriods)
                            .setMessage("Inconsistent use of periods in abbreviations for this section.");
                        result.add(item);
                        break;
                    }
                }
            } else if (path.indexOf("[@type=\"narrow\"]") >= 0) {
                String pathToAbbr = path.replace("[@type=\"narrow\"]", "[@type=\"abbreviated\"]");
                String abbrValue = getCldrFileToCheck().getWinningValueWithBailey(pathToAbbr);
                if (abbrValue != null && isTooMuchWiderThan(value, abbrValue)) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.warningType) // Making this just a warning, because there are some oddball cases.
                        .setSubtype(Subtype.narrowDateFieldTooWide)
                        .setMessage("Narrow value \"{0}\" shouldn't be longer than the corresponding abbreviated value \"{1}\"", value,
                            abbrValue);
                    result.add(item);
                }
            } else if (path.indexOf("/eraNarrow") >= 0) {
                String pathToAbbr = path.replace("/eraNarrow", "/eraAbbr");
                String abbrValue = getCldrFileToCheck().getWinningValueWithBailey(pathToAbbr);
                if (abbrValue != null && isTooMuchWiderThan(value, abbrValue)) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.narrowDateFieldTooWide)
                        .setMessage("Narrow value \"{0}\" can't be longer than the corresponding abbreviated value \"{1}\"", value,
                            abbrValue);
                    result.add(item);
                }
            } else if (path.indexOf("/eraAbbr") >= 0) {
                String pathToWide = path.replace("/eraAbbr", "/eraNames");
                String wideValue = getCldrFileToCheck().getWinningValueWithBailey(pathToWide);
                if (wideValue != null && isTooMuchWiderThan(value, wideValue)) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.abbreviatedDateFieldTooWide)
                        .setMessage("Abbreviated value \"{0}\" can't be longer than the corresponding wide value \"{1}\"", value,
                            wideValue);
                    result.add(item);
                }

            }

            String failure = flexInfo.checkValueAgainstSkeleton(path, value);
            if (failure != null) {
                result.add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.illegalDatePattern)
                    .setMessage(failure));
            }

            final String collisionPrefix = "//ldml/dates/calendars/calendar";
            main: if (path.startsWith(collisionPrefix)) {
                int pos = path.indexOf("\"]"); // end of first type
                if (pos < 0 || skipPath(path)) { // skip narrow, no-calendar
                    break main;
                }
                pos += 2;
                String myType = getLastType(path);
                if (myType == null) {
                    break main;
                }
                String myMainType = getMainType(path);

                String calendarPrefix = path.substring(0, pos);
                boolean endsWithDisplayName = path.endsWith("displayName"); // special hack, these shouldn't be in
                // calendar.

                Set<String> retrievedPaths = new HashSet<String>();
                getResolvedCldrFileToCheck().getPathsWithValue(value, calendarPrefix, null, retrievedPaths);
                if (retrievedPaths.size() < 2) {
                    break main;
                }
                // ldml/dates/calendars/calendar[@type="gregorian"]/eras/eraAbbr/era[@type="0"],
                // ldml/dates/calendars/calendar[@type="gregorian"]/eras/eraNames/era[@type="0"],
                // ldml/dates/calendars/calendar[@type="gregorian"]/eras/eraNarrow/era[@type="0"]]
                Type type = null;
                DayPeriod dayPeriod = null;
                final boolean isDayPeriod = path.contains("dayPeriod");
                if (isDayPeriod) {
                    XPathParts parts = XPathParts.getFrozenInstance(fullPath);
                    type = Type.fromString(parts.getAttributeValue(5, "type"));
                    dayPeriod = DayPeriod.valueOf(parts.getAttributeValue(-1, "type"));
                }

                // TODO redo above and below in terms of parts instead of searching strings

                Set<String> filteredPaths = new HashSet<String>();
                Output<Integer> sampleError = new Output<>();

                for (String item : retrievedPaths) {
                    if (item.equals(path)
                        || skipPath(item)
                        || endsWithDisplayName != item.endsWith("displayName")) {
                        continue;
                    }
                    String otherType = getLastType(item);
                    if (myType.equals(otherType)) { // we don't care about items with the same type value
                        continue;
                    }
                    String mainType = getMainType(item);
                    if (!myMainType.equals(mainType)) { // we *only* care about items with the same type value
                        continue;
                    }
                    if (isDayPeriod) {
                        //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="am"]
                        XPathParts itemParts = XPathParts.getFrozenInstance(item);
                        Type itemType = Type.fromString(itemParts.getAttributeValue(5, "type"));
                        DayPeriod itemDayPeriod = DayPeriod.valueOf(itemParts.getAttributeValue(-1, "type"));

                        if (!dateFormatInfoFormat.collisionIsError(type, dayPeriod, itemType, itemDayPeriod, sampleError)) {
                            continue;
                        }
                    }
                    filteredPaths.add(item);
                }
                if (filteredPaths.size() == 0) {
                    break main;
                }
                Set<String> others = new TreeSet<String>();
                for (String path2 : filteredPaths) {
                    PathHeader pathHeader = getPathHeaderFactory().fromPath(path2);
                    others.add(pathHeader.getHeaderCode());
                }
                CheckStatus.Type statusType = getPhase() == Phase.SUBMISSION || getPhase() == Phase.BUILD
                    ? CheckStatus.warningType
                    : CheckStatus.errorType;
                final CheckStatus checkStatus = new CheckStatus()
                    .setCause(this)
                    .setMainType(statusType)
                    .setSubtype(Subtype.dateSymbolCollision);
                if (sampleError.value == null) {
                    checkStatus.setMessage("The date value “{0}” is the same as what is used for a different item: {1}",
                        value, others.toString());
                } else {
                    checkStatus.setMessage("The date value “{0}” is the same as what is used for a different item: {1}. Sample problem: {2}",
                        value, others.toString(), sampleError.value / DayPeriodInfo.HOUR);
                }
                result.add(checkStatus);
            }
            DateTimePatternType dateTypePatternType = DateTimePatternType.fromPath(path);
            if (DateTimePatternType.STOCK_AVAILABLE_INTERVAL_PATTERNS.contains(dateTypePatternType)) {
                boolean patternBasicallyOk = false;
                try {
                    formatParser.set(value);
                    patternBasicallyOk = true;
                } catch (RuntimeException e) {
                    String message = e.getMessage();
                    if (message.contains("Illegal datetime field:")) {
                        CheckStatus item = new CheckStatus().setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalDatePattern)
                            .setMessage(message);
                        result.add(item);
                    } else {
                        CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalDatePattern)
                            .setMessage("Illegal date format pattern {0}", new Object[] { e });
                        result.add(item);
                    }
                }
                if (patternBasicallyOk) {
                    checkPattern(dateTypePatternType, path, fullPath, value, result);
                }
            } else if (path.contains("hourFormat")) {
                int semicolonPos = value.indexOf(';');
                if (semicolonPos < 0) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.illegalDatePattern)
                        .setMessage(
                            "Value should contain a positive hour format and a negative hour format separated by a semicolon.");
                    result.add(item);
                } else {
                    String[] formats = value.split(";");
                    if (formats[0].equals(formats[1])) {
                        CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalDatePattern)
                            .setMessage("The hour formats should not be the same.");
                        result.add(item);
                    } else {
                        checkHasHourMinuteSymbols(formats[0], result);
                        checkHasHourMinuteSymbols(formats[1], result);
                    }
                }
            }
        } catch (ParseException e) {
            CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.illegalDatePattern)
                .setMessage("ParseException in creating date format {0}", new Object[] { e });
            result.add(item);
        } catch (Exception e) {
            // e.printStackTrace();
            // HACK
            if (!HACK_CONFLICTING.matcher(e.getMessage()).find()) {
                CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.illegalDatePattern)
                    .setMessage("Error in creating date format {0}", new Object[] { e });
                result.add(item);
            }
        }
        return this;
    }

    private boolean isTooMuchWiderThan(String shortString, String longString) {
        // We all 1/3 the width of the reference character as a "fudge factor" in determining the allowable width
        return ApproximateWidth.getWidth(shortString) > ApproximateWidth.getWidth(longString) + REFCHAR / 3;
    }

    /**
     * Check for the presence of hour and minute symbols.
     *
     * @param value
     *            the value to be checked
     * @param result
     *            the list to add any errors to.
     */
    private void checkHasHourMinuteSymbols(String value, List<CheckStatus> result) {
        boolean hasHourSymbol = HOUR_SYMBOL.matcher(value).find();
        boolean hasMinuteSymbol = MINUTE_SYMBOL.matcher(value).find();
        if (!hasHourSymbol && !hasMinuteSymbol) {
            result.add(createErrorCheckStatus().setMessage("The hour and minute symbols are missing from {0}.", value));
        } else if (!hasHourSymbol) {
            result.add(createErrorCheckStatus()
                .setMessage("The hour symbol (H or HH) should be present in {0}.", value));
        } else if (!hasMinuteSymbol) {
            result.add(createErrorCheckStatus().setMessage("The minute symbol (mm) should be present in {0}.", value));
        }
    }

    /**
     * Convenience method for creating errors.
     *
     * @return
     */
    private CheckStatus createErrorCheckStatus() {
        return new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
            .setSubtype(Subtype.illegalDatePattern);
    }

    public boolean skipPath(String path) {
        return path.contains("arrow")
            || path.contains("/availableFormats")
            || path.contains("/interval")
            || path.contains("/dateTimeFormat")
//            || path.contains("/dayPeriod[")
//            && !path.endsWith("=\"pm\"]")
//            && !path.endsWith("=\"am\"]")
        ;
    }

    public String getLastType(String path) {
        int secondType = path.lastIndexOf("[@type=\"");
        if (secondType < 0) {
            return null;
        }
        secondType += 8;
        int secondEnd = path.indexOf("\"]", secondType);
        if (secondEnd < 0) {
            return null;
        }
        return path.substring(secondType, secondEnd);
    }

    public String getMainType(String path) {
        int secondType = path.indexOf("\"]/");
        if (secondType < 0) {
            return null;
        }
        secondType += 3;
        int secondEnd = path.indexOf("/", secondType);
        if (secondEnd < 0) {
            return null;
        }
        return path.substring(secondType, secondEnd);
    }

    private String getValues(CLDRFile resolvedCldrFileToCheck, Collection<String> values) {
        Set<String> results = new TreeSet<String>();
        for (String path : values) {
            final String stringValue = resolvedCldrFileToCheck.getStringValue(path);
            if (stringValue != null) {
                results.add(stringValue);
            }
        }
        return "{" + CollectionUtilities.join(results, "},{") + "}";
    }

    static final Pattern HACK_CONFLICTING = PatternCache.get("Conflicting fields:\\s+M+,\\s+l");

    public CheckCLDR handleGetExamples(String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (path.indexOf("/dates") < 0 || path.indexOf("gregorian") < 0) return this;
        try {
            if (path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0
                || path.indexOf("/dateFormatItem") >= 0) {
                checkPattern2(path, value, result);
            }
        } catch (Exception e) {
            // don't worry about errors
        }
        return this;
    }

    static final SimpleDateFormat neutralFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", ULocale.ENGLISH);
    static {
        neutralFormat.setTimeZone(ExampleGenerator.ZONE_SAMPLE);
    }

    // Get Date-Time in milliseconds
    private static long getDateTimeinMillis(int year, int month, int date, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, hourOfDay, minute, second);
        return cal.getTimeInMillis();
    }

    static long date1950 = getDateTimeinMillis(1950, 0, 1, 0, 0, 0);
    static long date2010 = getDateTimeinMillis(2010, 0, 1, 0, 0, 0);
    static long date4004BC = getDateTimeinMillis(-4004, 9, 23, 2, 0, 0);
    static Random random = new Random(0);

    private void checkPattern(DateTimePatternType dateTypePatternType, String path, String fullPath, String value, List<CheckStatus> result)
        throws ParseException {
        String skeleton = dateTimePatternGenerator.getSkeletonAllowingDuplicates(value);
        String skeletonCanonical = dateTimePatternGenerator.getCanonicalSkeletonAllowingDuplicates(value);

        if (value.contains("MMM.") || value.contains("LLL.") || value.contains("E.") || value.contains("eee.")
            || value.contains("ccc.") || value.contains("QQQ.") || value.contains("qqq.")) {
            result
                .add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.incorrectDatePattern)
                    .setMessage(
                        "Your pattern ({0}) is probably incorrect; abbreviated month/weekday/quarter names that need a period should include it in the name, rather than adding it to the pattern.",
                        value));
        }
        XPathParts pathParts = XPathParts.getTestInstance(path);
        String calendar = pathParts.findAttributeValue("calendar", "type");
        String id;
        switch (dateTypePatternType) {
        case AVAILABLE:
            id = pathParts.getAttributeValue(-1, "id");
            break;
        case INTERVAL:
            id = pathParts.getAttributeValue(-2, "id");
            break;
        case STOCK:
            id = pathParts.getAttributeValue(-3, "type");
            break;
        default:
            throw new IllegalArgumentException();
        }

        if (dateTypePatternType == DateTimePatternType.AVAILABLE || dateTypePatternType == DateTimePatternType.INTERVAL) {
            String idCanonical = dateTimePatternGenerator.getCanonicalSkeletonAllowingDuplicates(id);
            if (skeleton.isEmpty()) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.incorrectDatePattern)
                    // "Internal ID ({0}) doesn't match generated ID ({1}) for pattern ({2}). " +
                    .setMessage("Your pattern ({1}) is incorrect for ID ({0}). " +
                        "You need to supply a pattern according to http://cldr.org/translation/date-time-patterns.",
                        id, value));
            } else if (!dateTimePatternGenerator.skeletonsAreSimilar(idCanonical, skeletonCanonical)) {
                String fixedValue = dateTimePatternGenerator.replaceFieldTypes(value, id);
                result
                    .add(new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.incorrectDatePattern)
                        // "Internal ID ({0}) doesn't match generated ID ({1}) for pattern ({2}). " +
                        .setMessage(
                            "Your pattern ({2}) doesn't correspond to what is asked for. Yours would be right for an ID ({1}) but not for the ID ({0}). "
                                +
                                "Please change your pattern to match what was asked, such as ({3}), with the right punctuation and/or ordering for your language. See http://cldr.org/translation/date-time-patterns.",
                            id, skeletonCanonical, value, fixedValue));
            }
            if (dateTypePatternType == DateTimePatternType.AVAILABLE) {
                // index y+w+ must correpond to pattern containing only Y+ and w+
                if (idCanonical.matches("y+w+") && !(skeleton.matches("Y+w+") || skeleton.matches("w+Y+"))) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.incorrectDatePattern)
                        .setMessage("For id {0}, the pattern ({1}) must contain fields Y and w, and no others.", id, value));
                }
                // index M+W msut correspond to pattern containing only M+/L+ and W
                if (idCanonical.matches("M+W") && !(skeletonCanonical.matches("M+W") || skeletonCanonical.matches("WM+"))) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.incorrectDatePattern)
                        .setMessage("For id {0}, the pattern ({1}) must contain fields M or L, plus W, and no others.", id, value));
                }
            }
            String failureMessage = (String) flexInfo.getFailurePath(path);
            if (failureMessage != null) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.illegalDatePattern)
                    .setMessage("{0}", new Object[] { failureMessage }));
            }
        }
        if (dateTypePatternType == DateTimePatternType.STOCK) {
            int style = 0;
            String len = pathParts.findAttributeValue("timeFormatLength", "type");
            DateOrTime dateOrTime = DateOrTime.time;
            if (len == null) {
                dateOrTime = DateOrTime.date;
                style += 4;
                len = pathParts.findAttributeValue("dateFormatLength", "type");
                if (len == null) {
                    len = pathParts.findAttributeValue("dateTimeFormatLength", "type");
                    dateOrTime = DateOrTime.dateTime;
                }
            }

            DateTimeLengths dateTimeLength = DateTimeLengths.valueOf(len.toUpperCase(Locale.ENGLISH));

            if (calendar.equals("gregorian") && !"root".equals(getCldrFileToCheck().getLocaleID())) {
                checkValue(dateTimeLength, dateOrTime, value, result);
            }
            if (dateOrTime == DateOrTime.dateTime) {
                return; // We don't need to do the rest for date/time combo patterns.
            }
            style += dateTimeLength.ordinal();
            // do regex match with skeletonCanonical but report errors using skeleton; they have corresponding field lengths
            if (!dateTimePatterns[style].matcher(skeletonCanonical).matches()
                && !calendar.equals("chinese")
                && !calendar.equals("hebrew")) {
                int i = RegexUtilities.findMismatch(dateTimePatterns[style], skeletonCanonical);
                String skeletonPosition = skeleton.substring(0, i) + "☹" + skeleton.substring(i);
                result.add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.missingOrExtraDateField)
                    .setMessage("Field is missing, extra, or the wrong length. Expected {0} [Internal: {1} / {2}]",
                        new Object[] { dateTimeMessage[style], skeletonPosition, dateTimePatterns[style].pattern() }));
            }
        } else if (dateTypePatternType == DateTimePatternType.INTERVAL) {
            if (id.contains("y")) {
                String greatestDifference = pathParts.findAttributeValue("greatestDifference", "id");
                int requiredYearFieldCount = 1;
                if ("y".equals(greatestDifference)) {
                    requiredYearFieldCount = 2;
                }
                int yearFieldCount = 0;
                Matcher yearFieldMatcher = YEAR_FIELDS.matcher(value);
                while (yearFieldMatcher.find()) {
                    yearFieldCount++;
                }
                if (yearFieldCount < requiredYearFieldCount) {
                    result.add(new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.missingOrExtraDateField)
                        .setMessage("Not enough year fields in interval pattern. Must have {0} but only found {1}",
                            new Object[] { requiredYearFieldCount, yearFieldCount }));
                }
            }
        }

        if (value.contains("G") && calendar.equals("gregorian")) {
            GyState actual = GyState.forPattern(value);
            GyState expected = getExpectedGy(getCldrFileToCheck().getLocaleID());
            if (actual != expected) {
                result.add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.unexpectedOrderOfEraYear)
                    .setMessage("Unexpected order of era/year. Expected {0}, but got {1} in 〈{2}〉 for {3}/{4}",
                        expected, actual, value, calendar, id));
            }
        }
    }

    enum DateOrTime {
        date, time, dateTime
    }

    static final Map<DateOrTime, Relation<DateTimeLengths, String>> STOCK_PATTERNS = new EnumMap<DateOrTime, Relation<DateTimeLengths, String>>(
        DateOrTime.class);

    //
    private static void add(Map<DateOrTime, Relation<DateTimeLengths, String>> stockPatterns,
        DateOrTime dateOrTime, DateTimeLengths dateTimeLength, String... keys) {
        Relation<DateTimeLengths, String> rel = STOCK_PATTERNS.get(dateOrTime);
        if (rel == null) {
            STOCK_PATTERNS.put(dateOrTime, rel = Relation.of(new EnumMap<DateTimeLengths, Set<String>>(DateTimeLengths.class), LinkedHashSet.class));
        }
        rel.putAll(dateTimeLength, Arrays.asList(keys));
    }

    /*  Ticket #4936
    value(short time) = value(hm) or value(Hm)
    value(medium time) = value(hms) or value(Hms)
    value(long time) = value(medium time+z)
    value(full time) = value(medium time+zzzz)
     */
    static {
        add(STOCK_PATTERNS, DateOrTime.time, DateTimeLengths.SHORT, "hm", "Hm");
        add(STOCK_PATTERNS, DateOrTime.time, DateTimeLengths.MEDIUM, "hms", "Hms");
        add(STOCK_PATTERNS, DateOrTime.time, DateTimeLengths.LONG, "hms*z", "Hms*z");
        add(STOCK_PATTERNS, DateOrTime.time, DateTimeLengths.FULL, "hms*zzzz", "Hms*zzzz");
        add(STOCK_PATTERNS, DateOrTime.date, DateTimeLengths.SHORT, "yMd");
        add(STOCK_PATTERNS, DateOrTime.date, DateTimeLengths.MEDIUM, "yMMMd");
        add(STOCK_PATTERNS, DateOrTime.date, DateTimeLengths.LONG, "yMMMMd", "yMMMd");
        add(STOCK_PATTERNS, DateOrTime.date, DateTimeLengths.FULL, "yMMMMEd", "yMMMEd");
    }

    static final String AVAILABLE_PREFIX = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"";
    static final String AVAILABLE_SUFFIX = "\"]";
    static final String APPEND_TIMEZONE = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/appendItems/appendItem[@request=\"Timezone\"]";

    private void checkValue(DateTimeLengths dateTimeLength, DateOrTime dateOrTime, String value, List<CheckStatus> result) {
        // Check consistency of the pattern vs. supplemental wrt 12 vs. 24 hour clock.
        if (dateOrTime == DateOrTime.time) {
            PreferredAndAllowedHour pref = sdi.getTimeData().get(territory);
            if (pref == null) {
                pref = sdi.getTimeData().get("001");
            }
            String checkForHour, clockType;
            if (pref.preferred.equals(PreferredAndAllowedHour.HourStyle.h)) {
                checkForHour = "h";
                clockType = "12";
            } else {
                checkForHour = "H";
                clockType = "24";
            }
            if (!value.contains(checkForHour)) {
                CheckStatus.Type errType = CheckStatus.errorType;
                // French/Canada is strange, they use 24 hr clock while en_CA uses 12.
                if (language.equals("fr") && territory.equals("CA")) {
                    errType = CheckStatus.warningType;
                }

                result.add(new CheckStatus().setCause(this).setMainType(errType)
                    .setSubtype(Subtype.inconsistentTimePattern)
                    .setMessage("Time format inconsistent with supplemental time data for territory \"" + territory + "\"."
                        + " Use '" + checkForHour + "' for " + clockType + " hour clock."));
            }
        }
        if (dateOrTime == DateOrTime.dateTime) {
            boolean inQuotes = false;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch == '\'') {
                    inQuotes = !inQuotes;
                }
                if (!inQuotes && (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.patternContainsInvalidCharacters)
                        .setMessage("Unquoted letter \"{0}\" in dateTime format.", ch));
                }
            }
        } else {
            Set<String> keys = STOCK_PATTERNS.get(dateOrTime).get(dateTimeLength);
            StringBuilder b = new StringBuilder();
            boolean onlyNulls = true;
            int countMismatches = 0;
            boolean errorOnMissing = false;
            String timezonePattern = null;
            Set<String> bases = new LinkedHashSet<String>();
            for (String key : keys) {
                int star = key.indexOf('*');
                boolean hasStar = star >= 0;
                String base = !hasStar ? key : key.substring(0, star);
                bases.add(base);
                String xpath = AVAILABLE_PREFIX + base + AVAILABLE_SUFFIX;
                String value1 = getCldrFileToCheck().getStringValue(xpath);
                // String localeFound = getCldrFileToCheck().getSourceLocaleID(xpath, null);  && !localeFound.equals("root") && !localeFound.equals("code-fallback")
                if (value1 != null) {
                    onlyNulls = false;
                    if (hasStar) {
                        String zone = key.substring(star + 1);
                        timezonePattern = getResolvedCldrFileToCheck().getStringValue(APPEND_TIMEZONE);
                        value1 = MessageFormat.format(timezonePattern, value1, zone);
                    }
                    if (equalsExceptWidth(value, value1)) {
                        return;
                    }
                } else {
                    // Example, if the requiredLevel for the locale is moderate,
                    // and the level for the path is modern, then we'll skip the error,
                    // but if the level for the path is basic, then we won't
                    Level pathLevel = coverageLevel.getLevel(xpath);
                    if (requiredLevel.compareTo(pathLevel) >= 0) {
                        errorOnMissing = true;
                    }
                }
                add(b, base, value1);
                countMismatches++;
            }
            if (!onlyNulls) {
                if (timezonePattern != null) {
                    b.append(" (with appendZonePattern: “" + timezonePattern + "”)");
                }
                String msg = countMismatches != 1
                    ? "{1}-{0} → “{2}” didn't match any of the corresponding flexible skeletons: [{3}]. This or the flexible patterns needs to be changed."
                    : "{1}-{0} → “{2}” didn't match the corresponding flexible skeleton: {3}. This or the flexible pattern needs to be changed.";
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.inconsistentDatePattern)
                    .setMessage(msg,
                        dateTimeLength, dateOrTime, value, b));
            } else {
                if (errorOnMissing) {
                    String msg = countMismatches != 1
                        ? "{1}-{0} → “{2}” doesn't have at least one value for a corresponding flexible skeleton {3}, which needs to be added."
                        : "{1}-{0} → “{2}” doesn't have a value for the corresponding flexible skeleton {3}, which needs to be added.";
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.missingDatePattern)
                        .setMessage(msg,
                            dateTimeLength, dateOrTime, value, CollectionUtilities.join(bases, ", ")));
                }
            }
        }
    }

    private void add(StringBuilder b, String key, String value1) {
        if (value1 == null) {
            return;
        }
        if (b.length() != 0) {
            b.append(" or ");
        }
        b.append(key + (value1 == null ? " - missing" : " → “" + value1 + "”"));
    }

    private boolean equalsExceptWidth(String value1, String value2) {
        if (value1.equals(value2)) {
            return true;
        } else if (value2 == null) {
            return false;
        }

        List<Object> items1 = new ArrayList<Object>(formatParser.set(value1).getItems()); // clone
        List<Object> items2 = formatParser.set(value2).getItems();
        if (items1.size() != items2.size()) {
            return false;
        }
        Iterator<Object> it2 = items2.iterator();
        for (Object item1 : items1) {
            Object item2 = it2.next();
            if (item1.equals(item2)) {
                continue;
            }
            if (item1 instanceof VariableField && item2 instanceof VariableField) {
                // simple test for now, ignore widths
                if (item1.toString().charAt(0) == item2.toString().charAt(0)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    static final Set<String> YgLanguages = new HashSet<String>(Arrays.asList(
        "ar", "cs", "da", "de", "en", "es", "fa", "fi", "fr", "he", "hr", "id", "it", "nb", "nl", "pt", "ru", "sv", "tr"));

    private GyState getExpectedGy(String localeID) {
        // hack for now
        int firstBar = localeID.indexOf('_');
        String lang = firstBar < 0 ? localeID : localeID.substring(0, firstBar);
        return YgLanguages.contains(lang) ? GyState.YEAR_ERA : GyState.ERA_YEAR;
    }

    enum GyState {
        YEAR_ERA, ERA_YEAR, OTHER;
        static DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();

        static synchronized GyState forPattern(String value) {
            formatParser.set(value);
            int last = -1;
            for (Object x : formatParser.getItems()) {
                if (x instanceof VariableField) {
                    int type = ((VariableField) x).getType();
                    if (type == DateTimePatternGenerator.ERA && last == DateTimePatternGenerator.YEAR) {
                        return GyState.YEAR_ERA;
                    } else if (type == DateTimePatternGenerator.YEAR && last == DateTimePatternGenerator.ERA) {
                        return GyState.ERA_YEAR;
                    }
                    last = type;
                }
            }
            return GyState.OTHER;
        }
    }

    enum DateTimeLengths {
        SHORT, MEDIUM, LONG, FULL
    };

    // The patterns below should only use the *canonical* characters for each field type:
    // y (not Y, u, U)
    // Q (not q)
    // M (not L)
    // E (not e, c)
    // a (not b, B)
    // H or h (not k or K)
    // v (not z, Z, V)
    static final Pattern[] dateTimePatterns = {
        PatternCache.get("a*(h|hh|H|HH)(m|mm)"), // time-short
        PatternCache.get("a*(h|hh|H|HH)(m|mm)(s|ss)"), // time-medium
        PatternCache.get("a*(h|hh|H|HH)(m|mm)(s|ss)(v+)"), // time-long
        PatternCache.get("a*(h|hh|H|HH)(m|mm)(s|ss)(v+)"), // time-full
        PatternCache.get("G*y{1,4}M{1,2}(d|dd)"), // date-short; allow yyy for Minguo/ROC calendar
        PatternCache.get("G*y(yyy)?M{1,3}(d|dd)"), // date-medium
        PatternCache.get("G*y(yyy)?M{1,4}(d|dd)"), // date-long
        PatternCache.get("G*y(yyy)?M{1,4}E*(d|dd)"), // date-full
        PatternCache.get(".*"), // datetime-short
        PatternCache.get(".*"), // datetime-medium
        PatternCache.get(".*"), // datetime-long
        PatternCache.get(".*"), // datetime-full
    };

    static final String[] dateTimeMessage = {
        "hours (H, HH, h, or hh), and minutes (m or mm)", // time-short
        "hours (H, HH, h, or hh), minutes (m or mm), and seconds (s or ss)", // time-medium
        "hours (H, HH, h, or hh), minutes (m or mm), and seconds (s or ss); optionally timezone (z, zzzz, v, vvvv)", // time-long
        "hours (H, HH, h, or hh), minutes (m or mm), seconds (s or ss), and timezone (z, zzzz, v, vvvv)", // time-full
        "year (y, yy, yyyy), month (M or MM), and day (d or dd); optionally era (G)", // date-short
        "year (y), month (M, MM, or MMM), and day (d or dd); optionally era (G)", // date-medium
        "year (y), month (M, ... MMMM), and day (d or dd); optionally era (G)", // date-long
        "year (y), month (M, ... MMMM), and day (d or dd); optionally day of week (EEEE or cccc) or era (G)", // date-full
    };

    public String toString(DateTimePatternGenerator.FormatParser formatParser) {
        StringBuffer result = new StringBuffer();
        for (Object x : formatParser.getItems()) {
            if (x instanceof DateTimePatternGenerator.VariableField) {
                result.append(x.toString());
            } else {
                result.append(formatParser.quoteLiteral(x.toString()));
            }
        }
        return result.toString();
    }

    private void checkPattern2(String path, String value, List<CheckStatus> result) throws ParseException {
        XPathParts pathParts = XPathParts.getTestInstance(path);
        String calendar = pathParts.findAttributeValue("calendar", "type");
        SimpleDateFormat x = icuServiceBuilder.getDateFormat(calendar, value);
        x.setTimeZone(ExampleGenerator.ZONE_SAMPLE);
        result.add(new MyCheckStatus()
            .setFormat(x)
            .setCause(this).setMainType(CheckStatus.demoType));
    }

    static final UnicodeSet XGRAPHEME = new UnicodeSet("[[:mark:][:grapheme_extend:][:punctuation:]]");
    static final UnicodeSet DIGIT = new UnicodeSet("[:decimal_number:]");

    static public class MyCheckStatus extends CheckStatus {
        private SimpleDateFormat df;

        public MyCheckStatus setFormat(SimpleDateFormat df) {
            this.df = df;
            return this;
        }

        public SimpleDemo getDemo() {
            return new MyDemo().setFormat(df);
        }
    }

    static class MyDemo extends FormatDemo {
        private SimpleDateFormat df;

        protected String getPattern() {
            return df.toPattern();
        }

        protected String getSampleInput() {
            return neutralFormat.format(ExampleGenerator.DATE_SAMPLE);
        }

        public MyDemo setFormat(SimpleDateFormat df) {
            this.df = df;
            return this;
        }

        protected void getArguments(Map<String, String> inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            Date d;
            try {
                currentPattern = inout.get("pattern");
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
                d = neutralFormat.parse(currentInput);
            } catch (Exception e) {
                currentInput = "Use neutral format like: 1993-11-31 13:49:02";
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
                Date n = df.parse(currentFormatted, parsePosition);
                if (parsePosition.getIndex() != currentFormatted.length()) {
                    currentReparsed = "Couldn't parse past: " + "\u200E"
                        + currentFormatted.substring(0, parsePosition.getIndex()) + "\u200E";
                } else {
                    currentReparsed = neutralFormat.format(n);
                }
            } catch (Exception e) {
                currentReparsed = "Can't parse: " + e.getMessage();
            }
        }

    }
}
