package org.unicode.cldr.test;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeProperty.PatternMatcher;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckDates extends FactoryCheckCLDR {
    static boolean GREGORIAN_ONLY = CldrUtility.getProperty("GREGORIAN", false);

    ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
    NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);
    PatternMatcher m;
    DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
    DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.getEmptyInstance();

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
    private static final Pattern HOUR_SYMBOL = Pattern.compile("H{1,2}");
    private static final Pattern MINUTE_SYMBOL = Pattern.compile("mm");

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

    // Day periods that are allowed to have duplicate names with only a warning
    private static final Map<String, String> dayPeriodsEquivMap = new HashMap<String, String>();
    static {
        dayPeriodsEquivMap.put("[@type=\"am\"]", "[@type=\"morning\"]");
        dayPeriodsEquivMap.put("[@type=\"morning\"]", "[@type=\"am\"]");
        dayPeriodsEquivMap.put("[@type=\"noon\"]", "[@type=\"midDay\"]");
        dayPeriodsEquivMap.put("[@type=\"midDay\"]", "[@type=\"noon\"]");
        dayPeriodsEquivMap.put("[@type=\"pm\"]", "[@type=\"afternoon\"]");
        dayPeriodsEquivMap.put("[@type=\"afternoon\"]", "[@type=\"pm\"]");
    }

    // Map<String, Set<String>> calPathsToSymbolSets;
    // Map<String, Map<String, String>> calPathsToSymbolMaps = new HashMap<String, Map<String, String>>();

    public CheckDates(Factory factory) {
        super(factory);
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        pathHeaderFactory = PathHeader.getFactory(getDisplayInformation());

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

        // load gregorian appendItems
        for (Iterator it = resolved.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]"); it.hasNext();) {
            String path = (String) it.next();
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
    Collection redundants = new HashSet();
    Status status = new Status();
    PathStarrer pathStarrer = new PathStarrer();
    PathHeader.Factory pathHeaderFactory;

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (fullPath == null) {
            return this; // skip paths that we don't have
        }

        if (path.indexOf("/dates") < 0
            || path.endsWith("/default")
            || path.endsWith("/alias")) {
            return this;
        }

        final String sourceLocaleID = getCldrFileToCheck().getSourceLocaleID(path, status);

        if (!path.equals(status.pathWhereFound)) {
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

            if (path.indexOf("[@type=\"abbreviated\"]") >= 0 && value.length() > 0) {
                String pathToWide = path.replace("[@type=\"abbreviated\"]", "[@type=\"wide\"]");
                String wideValue = getCldrFileToCheck().getStringValue(pathToWide);
                if (wideValue != null && value.length() > wideValue.length()) {
                    CheckStatus item = new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.abbreviatedDateFieldTooWide)
                        .setMessage("Illegal abbreviated value {0}, can't be longer than wide value {1}", value,
                            wideValue);
                    result.add(item);
                }
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
                Set<String> filteredPaths = new HashSet<String>();
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
                    filteredPaths.add(item);
                }
                if (filteredPaths.size() == 0) {
                    break main;
                }
                Set<String> others = new TreeSet<String>();
                for (String path2 : filteredPaths) {
                    PathHeader pathHeader = pathHeaderFactory.fromPath(path2);
                    others.add(pathHeader.getHeaderCode());
                }
                String statusType = CheckStatus.errorType;
                result.add(new CheckStatus()
                    .setCause(this)
                    .setMainType(statusType)
                    .setSubtype(Subtype.dateSymbolCollision)
                    .setMessage("The date value “{0}” is the same as what is used for a different item: {1}", value,
                        others.toString()));

            }

            // result.add(new CheckStatus()
            // .setCause(this).setMainType(statusType).setSubtype(Subtype.dateSymbolCollision)
            // .setMessage("Date symbol value {0} duplicates an earlier symbol in the same set, for {1}", value,
            // typeForPrev));

            // // Test for duplicate date symbol names (in format wide/abbrev months/days/quarters, or any context/width
            // dayPeriods/eras)
            // int truncateAt = path.lastIndexOf("[@type="); // want path without any final [@type="sun"], [@type="12"],
            // etc.
            // if ( truncateAt >= 0 ) {
            // String truncPath = path.substring(0,truncateAt);
            // if ( calPathsToSymbolMaps.containsKey(truncPath) ) {
            // // Need to check whether this symbol duplicates another
            // String type = path.substring(truncateAt); // the final part e.g. [@type="am"]
            // Map<String, String> mapForThisPath = calPathsToSymbolMaps.get(truncPath);
            // if ( mapForThisPath == null ) {
            // mapForThisPath = new HashMap<String, String>();
            // mapForThisPath.put(value, type);
            // calPathsToSymbolMaps.put(truncPath, mapForThisPath);
            // } else if ( !mapForThisPath.containsKey(value) ) {
            // mapForThisPath.put(value, type);
            // calPathsToSymbolMaps.put(truncPath, mapForThisPath);
            // } else {
            // // this value duplicates a previous one in the same set. May be only a warning.
            // String statusType = CheckStatus.errorType;
            // String typeForPrev = mapForThisPath.get(value);
            // if (path.contains("/eras/")) {
            // statusType = CheckStatus.warningType;
            // } else if (path.contains("/dayPeriods/")) {
            // // certain duplicates only merit a warning:
            // // "am" and "morning", "noon" and "midDay", "pm" and "afternoon"
            // String typeEquiv = dayPeriodsEquivMap.get(type);
            // if ( typeForPrev.equals(typeEquiv) ) {
            // statusType = CheckStatus.warningType;
            // }
            // }
            // result.add(new CheckStatus()
            // .setCause(this).setMainType(statusType).setSubtype(Subtype.dateSymbolCollision)
            // .setMessage("Date symbol value {0} duplicates an earlier symbol in the same set, for {1}", value,
            // typeForPrev));
            // }
            // }
            // }

            if (path.indexOf("[@type=\"narrow\"]") >= 0 && !path.contains("dayPeriod")
                && !path.contains("monthPatterns")) {
                int end = isNarrowEnough(value, bi);
                String locale = getCldrFileToCheck().getLocaleID();
                // Per cldrbug 1456, skip the following test for Thai (or should we instead just change errorType to
                // warningType in this case?)
                if (end != value.length() && !locale.equals("th") && !locale.startsWith("th_")) {
                    result
                        .add(new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.narrowDateFieldTooWide)
                            .setMessage(
                                "Illegal narrow value. Must be only one grapheme cluster: \u200E{0}\u200E would be ok, but has extra \u200E{1}\u200E",
                                new Object[] { value.substring(0, end), value.substring(end) }));
                }
            }
            if (DisplayAndInputProcessor.hasDatetimePattern(path)) {
                boolean patternBasicallyOk = false;
                try {
                    if (!path.contains("intervalFormatItem")) {
                        SimpleDateFormat sdf = new SimpleDateFormat(value);
                    }
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
                    checkPattern(path, fullPath, value, result);
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
            || path.contains("/dayPeriod[")
            && !path.endsWith("=\"pm\"]")
            && !path.endsWith("=\"am\"]");
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

    static final Pattern HACK_CONFLICTING = Pattern.compile("Conflicting fields:\\s+M+,\\s+l");

    public CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options, List result) {
        if (path.indexOf("/dates") < 0 || path.indexOf("gregorian") < 0) return this;
        try {
            if (path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0
                || path.indexOf("/dateFormatItem") >= 0) {
                checkPattern2(path, fullPath, value, result);
            }
        } catch (Exception e) {
            // don't worry about errors
        }
        return this;
    }

    // Calendar myCal = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
    // TimeZone denver = TimeZone.getTimeZone("America/Denver");
    static final SimpleDateFormat neutralFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", ULocale.ENGLISH);
    static {
        neutralFormat.setTimeZone(ExampleGenerator.ZONE_SAMPLE);
    }
    XPathParts pathParts = new XPathParts(null, null);

    static long date1950 = new Date(50, 0, 1, 0, 0, 0).getTime();
    static long date2010 = new Date(110, 0, 1, 0, 0, 0).getTime();
    static long date4004BC = new Date(-4004 - 1900, 9, 23, 2, 0, 0).getTime();
    static Random random = new Random(0);

    static private String getRandomDate(long startDate, long endDate) {
        double guess = startDate + random.nextDouble() * (endDate - startDate);
        return neutralFormat.format(new Date((long) guess));
    }

    private void checkPattern(String path, String fullPath, String value, List result) throws ParseException {
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

        pathParts.set(path);
        final boolean isIntervalFormat = pathParts.contains("intervalFormatItem");
        if (pathParts.containsElement("dateFormatItem") || isIntervalFormat) {
            int idIndex = isIntervalFormat ? -2 : -1;
            String id = pathParts.getAttributeValue(idIndex, "id");
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
                    .add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.incorrectDatePattern)
                        // "Internal ID ({0}) doesn't match generated ID ({1}) for pattern ({2}). " +
                        .setMessage(
                            "Your pattern ({2}) doesn't correspond to what is asked for. Yours would be right for an ID ({1}) but not for the ID ({0}). "
                                +
                                "Please change your pattern to match what was asked, such as ({3}), with the right punctuation and/or ordering for your language. See http://cldr.org/translation/date-time-patterns.",
                            id, skeletonCanonical, value, fixedValue));
            }
            String failureMessage = (String) flexInfo.getFailurePath(path);
            if (failureMessage != null) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.illegalDatePattern)
                    .setMessage("{0}", new Object[] { failureMessage }));
            }

            // if (redundants.contains(value)) {
            // result.add(new CheckStatus().setCause(this).setType(CheckStatus.errorType)
            // .setMessage("Redundant with some pattern (or combination)", new Object[]{}));
            // }
        }
        // String calendar = pathParts.findAttributeValue("calendar", "type");
        // if (path.indexOf("\"full\"") >= 0) {
        // // for date, check that era is preserved
        // // TODO fix naked constants
        // SimpleDateFormat y = icuServiceBuilder.getDateFormat(calendar, 4, 4);
        // //String trial = "BC 4004-10-23T2:00:00Z";
        // //Date dateSource = neutralFormat.parse(trial);
        // Date dateSource = new Date(date4004BC);
        // int year = dateSource.getYear() + 1900;
        // if (year > 0) {
        // year = 1-year;
        // dateSource.setYear(year - 1900);
        // }
        // //myCal.setTime(dateSource);
        // String result2 = y.format(dateSource);
        // Date backAgain;
        // try {
        //
        // backAgain = y.parse(result2,parsePosition);
        // } catch (ParseException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // //String isoBackAgain = neutralFormat.format(backAgain);
        //
        // if (false && path.indexOf("/dateFormat") >= 0 && year != backAgain.getYear()) {
        // CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
        // .setMessage("Need Era (G) in full format.", new Object[]{});
        // result.add(item);
        // }

        // formatParser.set(value);
        // String newValue = toString(formatParser);
        // if (!newValue.equals(value)) {
        // CheckStatus item = new CheckStatus().setType(CheckStatus.warningType)
        // .setMessage("Canonical form would be {0}", new Object[]{newValue});
        // result.add(item);
        // }
        // find the variable fields

        int style = 0;
        String len = pathParts.findAttributeValue("timeFormatLength", "type");
        if (len == null) {
            style += 4;
            len = pathParts.findAttributeValue("dateFormatLength", "type");
            if (len == null) {
                return; // skip the rest!!
            }
        }

        DateTimeLengths dateTimeLength = DateTimeLengths.valueOf(len.toUpperCase(Locale.ENGLISH));
        style += dateTimeLength.ordinal();
        // do regex match with skeletonCanonical but report errors using skeleton; they have corresponding field lengths
        if (!dateTimePatterns[style].matcher(skeletonCanonical).matches()
            && !pathParts.findAttributeValue("calendar", "type").equals("chinese")
            && !pathParts.findAttributeValue("calendar", "type").equals("hebrew")) {
            int i = RegexUtilities.findMismatch(dateTimePatterns[style], skeletonCanonical);
            String skeletonPosition = skeleton.substring(0, i) + "☹" + skeleton.substring(i);
            result.add(new CheckStatus()
                .setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.missingOrExtraDateField)
                .setMessage("Field is missing, extra, or the wrong length. Expected {0} [Internal: {1} / {2}]",
                    new Object[] { dateTimeMessage[style], skeletonPosition, dateTimePatterns[style].pattern() }));
        }

        // TODO fix this up.
        // if (path.indexOf("/timeFormat") >= 0 && y.toPattern().indexOf("v") < 0) {
        // CheckStatus item = new CheckStatus().setType(CheckCLDR.finalErrorType)
        // .setMessage("Need full zone (v) in full format", new Object[]{});
        // result.add(item);
        // }
    }

    enum DateTimeLengths {
        SHORT, MEDIUM, LONG, FULL
    };

    // The patterns below should only use the *canonical* characters for each field type:
    // y (not Y, u, U)
    // Q (not q)
    // M (not L)
    // E (not e, c)
    // H or h (not k or K)
    // v (not z, Z, V)
    static final Pattern[] dateTimePatterns = {
        Pattern.compile("(h|hh|H|HH)(m|mm)"), // time-short
        Pattern.compile("(h|hh|H|HH)(m|mm)(s|ss)"), // time-medium
        Pattern.compile("(h|hh|H|HH)(m|mm)(s|ss)(v+)"), // time-long
        Pattern.compile("(h|hh|H|HH)(m|mm)(s|ss)(v+)"), // time-full
        Pattern.compile("G*y{1,4}M{1,2}(d|dd)"), // date-short; allow yyy for Minguo/ROC calendar
        Pattern.compile("G*y(yyy)?M{1,3}(d|dd)"), // date-medium
        Pattern.compile("G*y(yyy)?M{1,4}(d|dd)"), // date-long
        Pattern.compile("G*y(yyy)?M{1,4}E*(d|dd)"), // date-full
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

    private ParsePosition parsePosition = new ParsePosition(0);

    private void checkPattern2(String path, String fullPath, String value, List result) throws ParseException {
        pathParts.set(path);
        String calendar = pathParts.findAttributeValue("calendar", "type");
        SimpleDateFormat x = icuServiceBuilder.getDateFormat(calendar, value);
        x.setTimeZone(ExampleGenerator.ZONE_SAMPLE);

        // Object[] arguments = new Object[samples.length];
        // for (int i = 0; i < samples.length; ++i) {
        // String source = getRandomDate(date1950, date2010); // samples[i];
        // Date dateSource = neutralFormat.parse(source);
        // String formatted = x.format(dateSource);
        // String reparsed;
        //
        // parsePosition.setIndex(0);
        // Date parsed = x.parse(formatted, parsePosition);
        // if (parsePosition.getIndex() != formatted.length()) {
        // reparsed = "Couldn't parse past: " + formatted.substring(0,parsePosition.getIndex());
        // } else {
        // reparsed = neutralFormat.format(parsed);
        // }
        //
        // arguments[i] = source + " \u2192 \u201C\u200E" + formatted + "\u200E\u201D \u2192 " + reparsed;
        // }
        // result.add(new CheckStatus()
        // .setCause(this).setType(CheckStatus.exampleType)
        // .setMessage(SampleList, arguments));
        result.add(new MyCheckStatus()
            .setFormat(x)
            .setCause(this).setMainType(CheckStatus.demoType));
    }

    public static int isNarrowEnough(String value, BreakIterator bi) {
        if (value.length() <= 1) return value.length();
        int current = 0;
        // skip any leading digits, for CJK
        current = DIGIT.findIn(value, current, true);

        bi.setText(value);
        if (current != 0) bi.preceding(current + 1); // get safe spot, possibly before
        current = bi.next();
        if (current == bi.DONE) {
            return value.length();
        }
        current = bi.next();
        if (current == bi.DONE) {
            return value.length();
        }
        // continue collecting any additional characters that are M or grapheme extend
        current = XGRAPHEME.findIn(value, current, true);
        // special case: allow 11 or 12
        // current = Utility.scan(DIGIT, value, current);
        // if (current != value.length() && DIGIT.containsAll(value) && value.length() == 2) {
        // return value.length();
        // }
        return current;
    }

    static final UnicodeSet XGRAPHEME = new UnicodeSet("[[:mark:][:grapheme_extend:]]");
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

        protected void getArguments(Map inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            boolean result = false;
            Date d;
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
