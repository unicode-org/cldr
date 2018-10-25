package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.ICUServiceBuilder.Context;
import org.unicode.cldr.util.ICUServiceBuilder.Width;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.PatternInfo;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class DateTimeFormats {
    private static final Date SAMPLE_DATE_DEFAULT_END = new Date(2099 - 1900, 0, 13, 14, 45, 59);
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "/verify/dates/";
    private static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    private static Map<String, PreferredAndAllowedHour> timeData = sdi.getTimeData();

    final static Options myOptions = new Options();

    enum MyOptions {
        organization(".*", "CLDR", "organization"), filter(".*", ".*", "locale filter (regex)");
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    private static final String TIMES_24H_TITLE = "Times 24h";
    private static final boolean DEBUG = false;
    private static final String DEBUG_SKELETON = "y";
    private static final ULocale DEBUG_LIST_PATTERNS = ULocale.JAPANESE; // or null;

    private static final String FIELDS_TITLE = "Fields";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final String[] STOCK = { "short", "medium", "long", "full" };
    private static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = {
        "G", "y", "M",
        "w", "W", "d",
        "D", "E", "F",
        "a", "h", "H",
        "m",
    };
    private static final Date SAMPLE_DATE = new Date(2012 - 1900, 0, 13, 14, 45, 59);

    private static final String SAMPLE_DATE_STRING = CldrUtility.isoFormat(SAMPLE_DATE);

    private static final Map<String,Date> SAMPLE_DATE_END = ImmutableMap.<String,Date>builder()
        .put("G", SAMPLE_DATE_DEFAULT_END)
        .put("y", new Date(2013 - 1900, 0, 13, 14, 45, 59))
        .put("M", new Date(2012 - 1900, 1, 13, 14, 45, 59))
        .put("w", SAMPLE_DATE_DEFAULT_END)
        .put("W", SAMPLE_DATE_DEFAULT_END)
        .put("d", new Date(2012 - 1900, 0, 14, 14, 45, 59))
        .put("D", SAMPLE_DATE_DEFAULT_END)
        .put("E", new Date(2012 - 1900, 0, 14, 14, 45, 59))
        .put("F", SAMPLE_DATE_DEFAULT_END)
        .put("a", new Date(2012 - 1900, 0, 13, 2, 45, 59))
        .put("h", new Date(2012 - 1900, 0, 13, 15, 45, 59))
        .put("H", new Date(2012 - 1900, 0, 13, 15, 45, 59))
        .put("m", SAMPLE_DATE_DEFAULT_END)
        .build();
//        // "G", "y", "M",
//        null, new Date(2013 - 1900, 0, 13, 14, 45, 59), new Date(2012 - 1900, 1, 13, 14, 45, 59),
//        // "w", "W", "d",
//        null, null, new Date(2012 - 1900, 0, 14, 14, 45, 59),
//        // "D", "E", "F",
//        null, new Date(2012 - 1900, 0, 14, 14, 45, 59), null,
//        // "a", "h", "H",
//        new Date(2012 - 1900, 0, 13, 2, 45, 59), new Date(2012 - 1900, 0, 13, 15, 45, 59),
//        new Date(2012 - 1900, 0, 13, 15, 45, 59),
//        // "m",
//        new Date(2012 - 1900, 0, 13, 14, 46, 59)

    private DateTimePatternGenerator generator;
    private ULocale locale;
    private ICUServiceBuilder icuServiceBuilder;
    private ICUServiceBuilder icuServiceBuilderEnglish = new ICUServiceBuilder().setCldrFile(CLDRConfig.getInstance().getEnglish());

    private DateIntervalInfo dateIntervalInfo = new DateIntervalInfo();
    private String calendarID;
    private CLDRFile file;

    private static String surveyUrl = CLDRConfig.getInstance().getProperty("CLDR_SURVEY_URL",
        "http://st.unicode.org/cldr-apps/survey");

    /**
     * Set a CLDRFile and calendar. Must be done before calling addTable.
     *
     * @param file
     * @param calendarID
     * @return
     */
    public DateTimeFormats set(CLDRFile file, String calendarID) {
        return set(file, calendarID, true);
    }

    /**
     * Set a CLDRFile and calendar. Must be done before calling addTable.
     *
     * @param file
     * @param calendarID
     * @return
     */
    public DateTimeFormats set(CLDRFile file, String calendarID, boolean useStock) {
        this.file = file;
        locale = new ULocale(file.getLocaleID());
        if (useStock) {
            icuServiceBuilder = new ICUServiceBuilder().setCldrFile(file);
        }
        PatternInfo returnInfo = new PatternInfo();
        XPathParts parts = new XPathParts();
        generator = DateTimePatternGenerator.getEmptyInstance();
        this.calendarID = calendarID;
        boolean haveDefaultHourChar = false;

        for (String stock : STOCK) {
            String path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID
                + "\"]/dateFormats/dateFormatLength[@type=\"" +
                stock +
                "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
            String dateTimePattern = file.getStringValue(path);
            if (useStock) {
                generator.addPattern(dateTimePattern, true, returnInfo);
            }
            path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID
                + "\"]/timeFormats/timeFormatLength[@type=\"" +
                stock +
                "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
            dateTimePattern = file.getStringValue(path);
            if (useStock) {
                generator.addPattern(dateTimePattern, true, returnInfo);
            }
            if (DEBUG
                && DEBUG_LIST_PATTERNS.equals(locale)) {
                System.out.println("* Adding: " + locale + "\t" + dateTimePattern);
            }
            if (!haveDefaultHourChar) {
                // use hour style in SHORT time pattern as the default
                // hour style for the locale
                FormatParser fp = new FormatParser();
                fp.set(dateTimePattern);
                List<Object> items = fp.getItems();
                for (int idx = 0; idx < items.size(); idx++) {
                    Object item = items.get(idx);
                    if (item instanceof VariableField) {
                        VariableField fld = (VariableField) item;
                        if (fld.getType() == DateTimePatternGenerator.HOUR) {
                            generator.setDefaultHourFormatChar(fld.toString().charAt(0));
                            haveDefaultHourChar = true;
                            break;
                        }
                    }
                }
            }
        }

        // appendItems result.setAppendItemFormat(getAppendFormatNumber(formatName), value);
        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"" + calendarID
            + "\"]/dateTimeFormats/appendItems/appendItem"))) {
            String request = parts.set(path).getAttributeValue(-1, "request");
            int requestNumber = DateTimePatternGenerator.getAppendFormatNumber(request);
            String value = file.getStringValue(path);
            generator.setAppendItemFormat(requestNumber, value);
            if (DEBUG
                && DEBUG_LIST_PATTERNS.equals(locale)) {
                System.out.println("* Adding: " + locale + "\t" + request + "\t" + value);
            }
        }

        // field names result.setAppendItemName(i, value);
        // ldml/dates/fields/field[@type="day"]/displayName
        for (String path : With.in(file.iterator("//ldml/dates/fields/field"))) {
            if (!path.contains("displayName")) {
                continue;
            }
            String type = parts.set(path).getAttributeValue(-2, "type");
            int requestNumber = find(FIELD_NAMES, type);

            String value = file.getStringValue(path);
            generator.setAppendItemName(requestNumber, value);
            if (DEBUG
                && DEBUG_LIST_PATTERNS.equals(locale)) {
                System.out.println("* Adding: " + locale + "\t" + type + "\t" + value);
            }
        }

        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"" + calendarID
            + "\"]/dateTimeFormats/availableFormats/dateFormatItem"))) {
            String key = parts.set(path).getAttributeValue(-1, "id");
            String value = file.getStringValue(path);
            if (key.equals(DEBUG_SKELETON)) {
                int debug = 0;
            }
            generator.addPatternWithSkeleton(value, key, true, returnInfo);
            if (DEBUG
                && DEBUG_LIST_PATTERNS.equals(locale)) {
                System.out.println("* Adding: " + locale + "\t" + key + "\t" + value);
            }
        }

        generator
            .setDateTimeFormat(Calendar.getDateTimePattern(Calendar.getInstance(locale), locale, DateFormat.MEDIUM));

        // ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMMMEd\"]/greatestDifference[@id=\"d\"]
        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"" + calendarID
            + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem"))) {
            String skeleton = parts.set(path).getAttributeValue(-2, "id");
            String diff = parts.set(path).getAttributeValue(-1, "id");
            int diffNumber = find(CALENDAR_FIELD_TO_PATTERN_LETTER, diff);
            String intervalPattern = file.getStringValue(path);
            dateIntervalInfo.setIntervalPattern(skeleton, diffNumber, intervalPattern);
        }
        if (useStock) {
            dateIntervalInfo.setFallbackIntervalPattern(
                file.getStringValue("//ldml/dates/calendars/calendar[@type=\""
                    + calendarID + "\"]/dateTimeFormats/intervalFormats/intervalFormatFallback"));
        }
        return this;
    }

    private static final String[] FIELD_NAMES = {
        "era", "year", "quarter", "month", "week", "week_of_month",
        "weekday", "day", "day_of_year", "day_of_week_in_month",
        "dayperiod", "hour", "minute", "second", "fractional_second", "zone"
    };

    static {
        if (FIELD_NAMES.length != DateTimePatternGenerator.TYPE_LIMIT) {
            throw new IllegalArgumentException("Internal error " + FIELD_NAMES.length + "\t"
                + DateTimePatternGenerator.TYPE_LIMIT);
        }
    }

    private <T> int find(T[] array, T item) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i].equals(item)) {
                return i;
            }
        }
        return 0;
    }

    private static final String[][] NAME_AND_PATTERN = {
        { "-", "Full Month" },
        { "year month", "yMMMM" },
        { " to  month+1", "yMMMM/M" },
        { " to  year+1", "yMMMM/y" },
        { "year month day", "yMMMMd" },
        { " to  day+1", "yMMMMd/d" },
        { " to  month+1", "yMMMMd/M" },
        { " to  year+1", "yMMMMd/y" },
        { "year month day weekday", "yMMMMEEEEd" },
        { " to  day+1", "yMMMMEEEEd/d" },
        { " to  month+1", "yMMMMEEEEd/M" },
        { " to  year+1", "yMMMMEEEEd/y" },
        { "month day", "MMMMd" },
        { " to  day+1", "MMMMd/d" },
        { " to  month+1", "MMMMd/M" },
        { "month day weekday", "MMMMEEEEd" },
        { " to  day+1", "MMMMEEEEd/d" },
        { " to  month+1", "MMMMEEEEd/M" },

        { "-", "Abbreviated Month" },
        { "year month<sub>a</sub>", "yMMM" },
        { " to  month+1", "yMMM/M" },
        { " to  year+1", "yMMM/y" },
        { "year month<sub>a</sub> day", "yMMMd" },
        { " to  day+1", "yMMMd/d" },
        { " to  month+1", "yMMMd/M" },
        { " to  year+1", "yMMMd/y" },
        { "year month<sub>a</sub> day weekday", "yMMMEd" },
        { " to  day+1", "yMMMEd/d" },
        { " to  month+1", "yMMMEd/M" },
        { " to  year+1", "yMMMEd/y" },
        { "month<sub>a</sub> day", "MMMd" },
        { " to  day+1", "MMMd/d" },
        { " to  month+1", "MMMd/M" },
        { "month<sub>a</sub> day weekday", "MMMEd" },
        { " to  day+1", "MMMEd/d" },
        { " to  month+1", "MMMEd/M" },

        { "-", "Numeric Month" },
        { "year month<sub>n</sub>", "yM" },
        { " to  month+1", "yM/M" },
        { " to  year+1", "yM/y" },
        { "year month<sub>n</sub> day", "yMd" },
        { " to  day+1", "yMd/d" },
        { " to  month+1", "yMd/M" },
        { " to  year+1", "yMd/y" },
        { "year month<sub>n</sub> day weekday", "yMEd" },
        { " to  day+1", "yMEd/d" },
        { " to  month+1", "yMEd/M" },
        { " to  year+1", "yMEd/y" },
        { "month<sub>n</sub> day", "Md" },
        { " to  day+1", "Md/d" },
        { " to  month+1", "Md/M" },
        { "month<sub>n</sub> day weekday", "MEd" },
        { " to  day+1", "MEd/d" },
        { " to  month+1", "MEd/M" },

        { "-", "Other Dates" },
        { "year", "y" },
        { " to  year+1", "y/y" },
        { "year quarter", "yQQQQ" },
        { "year quarter<sub>a</sub>", "yQQQ" },
        { "quarter", "QQQQ" },
        { "quarter<sub>a</sub>", "QQQ" },
        { "month", "MMMM" },
        { " to  month+1", "MMMM/M" },
        { "month<sub>a</sub>", "MMM" },
        { " to  month+1", "MMM/M" },
        { "month<sub>n</sub>", "M" },
        { " to  month+1", "M/M" },
        { "day", "d" },
        { " to  day+1", "d/d" },
        { "day weekday", "Ed" },
        { " to  day+1", "Ed/d" },
        { "weekday", "EEEE" },
        { " to  weekday+1", "EEEE/E" },
        { "weekday<sub>a</sub>", "E" },
        { " to  weekday+1", "E/E" },

        { "-", "Times" },
        { "hour", "j" },
        { " to  hour+1", "j/j" },
        { "hour minute", "jm" },
        { " to  minute+1", "jm/m" },
        { " to  hour+1", "jm/j" },
        { "hour minute second", "jms" },
        { "minute second", "ms" },
        { "minute", "m" },
        { "second", "s" },

        { "-", TIMES_24H_TITLE },
        { "hour<sub>24</sub>", "H" },
        { " to  hour+1", "H/H" },
        { "hour<sub>24</sub> minute", "Hm" },
        { " to  minute+1", "Hm/m" },
        { " to  hour+1", "Hm/H" },
        { "hour<sub>24</sub> minute second", "Hms" },

        { "-", "Dates and Times" },
        { "month, day, hour, minute", "Mdjm" },
        { "month, day, hour, minute", "MMMdjm" },
        { "month, day, hour, minute", "MMMMdjm" },
        { "year month, day, hour, minute", "yMdjms" },
        { "year month, day, hour, minute", "yMMMdjms" },
        { "year month, day, hour, minute", "yMMMMdjms" },
        { "year month, day, hour, minute, zone", "yMMMMdjmsv" },
        { "year month, day, hour, minute, zone (long)", "yMMMMdjmsvvvv" },

        { "-", "Relative Dates" },
        { "3 years ago", "®year-past-long-3" },
        { "2 years ago", "®year-past-long-2" },
        { "Last year", "®year-1" },
        { "This year", "®year0" },
        { "Next year", "®year1" },
        { "2 years from now", "®year-future-long-2" },
        { "3 years from now", "®year-future-long-3" },

        { "3 months ago", "®month-past-long-3" },
        { "Last month", "®month-1" },
        { "This month", "®month0" },
        { "Next month", "®month1" },
        { "3 months from now", "®month-future-long-3" },

        { "6 weeks ago", "®week-past-long-3" },
        { "Last week", "®week-1" },
        { "This week", "®week0" },
        { "Next week", "®week1" },
        { "6 weeks from now", "®week-future-long-3" },

        { "Last Sunday", "®sun-1" },
        { "This Sunday", "®sun0" },
        { "Next Sunday", "®sun1" },

        { "Last Sunday + time", "®sun-1jm" },
        { "This Sunday + time", "®sun0jm" },
        { "Next Sunday + time", "®sun1jm" },

        { "3 days ago", "®day-past-long-3" },
        { "Yesterday", "®day-1" },
        { "This day", "®day0" },
        { "Tomorrow", "®day1" },
        { "3 days from now", "®day-future-long-3" },

        { "3 days ago + time", "®day-past-long-3jm" },
        { "Last day + time", "®day-1jm" },
        { "This day + time", "®day0jm" },
        { "Next day + time", "®day1jm" },
        { "3 days from now + time", "®day-future-long-3jm" },
    };

    private class Diff {
        Set<String> availablePatterns = generator.getBaseSkeletons(new LinkedHashSet<String>());
        {
            for (Entry<String, Set<String>> pat : dateIntervalInfo.getPatterns().entrySet()) {
                for (String patDiff : pat.getValue()) {
                    availablePatterns.add(pat.getKey() + "/" + patDiff);
                }
            }
        }

        public boolean isPresent(String skeleton) {
            return availablePatterns.remove(skeleton.replace('j', generator.getDefaultHourFormatChar()));
        }
    }

    /**
     * Generate a table of date examples.
     *
     * @param comparison
     * @param output
     */
    public void addTable(DateTimeFormats comparison, Appendable output) {
        try {
            output.append("<h2>" + hackDoubleLinked("Patterns") + "</h2>\n<table class='dtf-table'>");
            Diff diff = new Diff();
            boolean is24h = generator.getDefaultHourFormatChar() == 'H';
            showRow(output, RowStyle.header, FIELDS_TITLE, "Skeleton", "English Example", "Native Example", false);
            for (String[] nameAndSkeleton : NAME_AND_PATTERN) {
                String name = nameAndSkeleton[0];
                String skeleton = nameAndSkeleton[1];
                if (skeleton.equals(DEBUG_SKELETON)) {
                    int debug = 0;
                }
                if (name.equals("-")) {
                    if (is24h && skeleton.equals(TIMES_24H_TITLE)) {
                        continue;
                    }
                    showRow(output, RowStyle.separator, skeleton, null, null, null, false);
                } else {
                    if (is24h && skeleton.contains("H")) {
                        continue;
                    }
                    showRow(output, RowStyle.normal, name, skeleton, comparison.getExample(skeleton), getExample(skeleton), diff.isPresent(skeleton));
                }
            }
            if (!diff.availablePatterns.isEmpty()) {
                showRow(output, RowStyle.separator, "Additional Patterns in Locale data", null, null, null, false);
                for (String skeleton : diff.availablePatterns) {
                    if (skeleton.equals(DEBUG_SKELETON)) {
                        int debug = 0;
                    }
                    if (is24h && (skeleton.contains("h") || skeleton.contains("a"))) {
                        continue;
                    }
                    // skip zones, day_of_year, Day of Week in Month, numeric quarter, week in month, week in year,
                    // frac.sec
                    if (skeleton.contains("v") || skeleton.contains("z")
                        || skeleton.contains("Q") && !skeleton.contains("QQ")
                        || skeleton.equals("D") || skeleton.equals("F")
                        || skeleton.equals("S")
                        || skeleton.equals("W") || skeleton.equals("w")) {
                        continue;
                    }
                    showRow(output, RowStyle.normal, skeleton, skeleton, comparison.getExample(skeleton), getExample(skeleton), true);
                }
            }
            output.append("</table>");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * Get an example from the "enhanced" skeleton.
     *
     * @param skeleton
     * @return
     */
    private String getExample(String skeleton) {
        String example;
        if (skeleton.contains("®")) {
            return getRelativeExampleFromSkeleton(skeleton);
        } else {
            int slashPos = skeleton.indexOf('/');
            if (slashPos >= 0) {
                String mainSkeleton = skeleton.substring(0, slashPos);
                DateIntervalFormat dateIntervalFormat = new DateIntervalFormat(mainSkeleton, dateIntervalInfo,
                    icuServiceBuilder.getDateFormat(calendarID, generator.getBestPattern(mainSkeleton)));
                String diffString = skeleton.substring(slashPos + 1).replace('j', 'H');
//                int diffNumber = find(CALENDAR_FIELD_TO_PATTERN_LETTER, diffString);
                Date endDate = SAMPLE_DATE_END.get(diffString);
                try {
                    example = dateIntervalFormat.format(new DateInterval(SAMPLE_DATE.getTime(), endDate.getTime()));
                } catch (Exception e) {
                    throw new IllegalArgumentException(skeleton + ", " + endDate, e);
                }
            } else {
                if (skeleton.equals(DEBUG_SKELETON)) {
                    int debug = 0;
                }
                SimpleDateFormat format = getDateFormatFromSkeleton(skeleton);
                format.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
                example = format.format(SAMPLE_DATE);
            }
        }
        return TransliteratorUtilities.toHTML.transform(example);
    }

    static final Pattern RELATIVE_DATE = PatternCache.get("®([a-z]+(?:-[a-z]+)?)+(-[a-z]+)?([+-]?\\d+)([a-zA-Z]+)?");

    class RelativePattern {
        private static final String UNIT_PREFIX = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-";
        final String type;
        final int offset;
        final String time;
        final String path;
        final String value;

        public RelativePattern(CLDRFile file, String skeleton) {
            Matcher m = RELATIVE_DATE.matcher(skeleton);
            if (m.matches()) {
                type = m.group(1);
                String length = m.group(2);
                offset = Integer.parseInt(m.group(3));
                String temp = m.group(4);
                time = temp == null ? null : temp.replace('j', generator.getDefaultHourFormatChar());

                if (-1 <= offset && offset <= 1) {
                    //ldml/dates/fields/field[@type="year"]/relative[@type="-1"]
                    path = "//ldml/dates/fields/field[@type=\"" + type + "\"]/relative[@type=\"" + offset + "\"]";
                    value = file.getStringValue(path);
                } else {
                    // //ldml/units/unit[@type="hour"]/unitPattern[@count="other"]
                    PluralInfo plurals = sdi.getPlurals(file.getLocaleID());
                    String base = UNIT_PREFIX + type + "\"]/unitPattern[@count=\"";
                    String tempPath = base + plurals.getCount(offset) + "\"]";
                    String tempValue = file.getStringValue(tempPath);
                    if (tempValue == null) {
                        tempPath = base + Count.other + "\"]";
                        tempValue = file.getStringValue(tempPath);
                    }
                    path = tempPath;
                    value = tempValue;
                }
            } else {
                throw new IllegalArgumentException(skeleton);
            }
        }
    }

    private String getRelativeExampleFromSkeleton(String skeleton) {
        RelativePattern rp = new RelativePattern(file, skeleton);
        String value = rp.value;
        if (value == null) {
            value = "ⓜⓘⓢⓢⓘⓝⓖ";
        } else {
            DecimalFormat format = icuServiceBuilder.getNumberFormat(0);
            value = value.replace("{0}", format.format(Math.abs(rp.offset)).replace("'", "''"));
        }
        if (rp.time == null) {
            return value;
        } else {
            SimpleDateFormat format2 = getDateFormatFromSkeleton(rp.time);
            format2.setTimeZone(GMT);
            String formattedTime = format2.format(SAMPLE_DATE);
            //                String length = skeleton.contains("MMMM") ? skeleton.contains("E") ? "full" : "long"
            //                    : skeleton.contains("MMM") ? "medium" : "short";
            String path2 = getDTSeparator("full");
            String datetimePattern = file.getStringValue(path2).replace("'", "");
            return MessageFormat.format(datetimePattern, formattedTime, value);
        }
    }

    private String getDTSeparator(String length) {
        String path = "//ldml/dates/calendars/calendar[@type=\"" +
            calendarID +
            "\"]/dateTimeFormats/dateTimeFormatLength[@type=\"" +
            length +
            "\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        return path;
    }

    public SimpleDateFormat getDateFormatFromSkeleton(String skeleton) {
        String pattern = getBestPattern(skeleton);
        return getDateFormat(pattern);
    }

    private SimpleDateFormat getDateFormat(String pattern) {
        SimpleDateFormat format = icuServiceBuilder.getDateFormat(calendarID, pattern);
        format.setTimeZone(GMT);
        return format;
    }

    public String getBestPattern(String skeleton) {
        String pattern = generator.getBestPattern(skeleton);
        return pattern;
    }

    enum RowStyle {
        header, separator, normal
    }

    /**
     * Show a single row
     *
     * @param output
     * @param rowStyle
     * @param name
     * @param skeleton
     * @param english
     * @param example
     * @param isPresent
     * @throws IOException
     */
    private void showRow(Appendable output, RowStyle rowStyle, String name, String skeleton, String english,
        String example, boolean isPresent)
        throws IOException {
        output.append("<tr>");
        switch (rowStyle) {
        case separator:
            String link = name.replace(' ', '_');
            output.append("<th colSpan='3' class='dtf-sep'>")
                .append(hackDoubleLinked(link, name))
                .append("</th>");
            break;
        case header:
        case normal:
            String startCell = rowStyle == RowStyle.header ? "<th class='dtf-h'>" : "<td class='dtf-s'>";
            String endCell = rowStyle == RowStyle.header ? "</th>" : "</td>";
            if (name.equals(FIELDS_TITLE)) {
                output.append("<th class='dtf-th'>").append(name).append("</a></th>");
            } else {
                String indent = "";
                if (name.startsWith(" ")) {
                    indent = "&nbsp;&nbsp;&nbsp;";
                    name = name.trim();
                }
                output.append("<th class='dtf-left'>" + indent + hackDoubleLinked(skeleton, name) + "</th>");
            }
            // .append(startCell).append(skeleton).append(endCell)
            output.append(startCell).append(english).append(endCell)
                .append(startCell).append(example).append(endCell)
            //.append(startCell).append(isPresent ? " " : "c").append(endCell)
            ;
            if (rowStyle != RowStyle.header) {
                String fix = getFix(skeleton);
                if (fix != null) {
                    output.append(startCell).append(fix).append(endCell);
                }
            }
        }
        output.append("</tr>\n");
    }

    private String getFix(String skeleton) {
        String path;
        String value;
        if (skeleton.contains("®")) {
            RelativePattern rp = new RelativePattern(file, skeleton);
            path = rp.path;
            value = rp.value;
        } else {
            skeleton = skeleton.replace('j', generator.getDefaultHourFormatChar());
            int slashPos = skeleton.indexOf('/');
            if (slashPos >= 0) {
                String mainSkeleton = skeleton.substring(0, slashPos);
                String diff = skeleton.substring(slashPos + 1);
                path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID +
                    "\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"" + mainSkeleton +
                    "\"]/greatestDifference[@id=\"" + diff +
                    "\"]";
            } else {
                path = getAvailableFormatPath(skeleton);
            }
            value = file.getStringValue(path);
        }
        if (value == null) {
            String skeleton2 = skeleton.replace("MMMM", "MMM").replace("EEEE", "E").replace("QQQQ", "QQQ");
            if (!skeleton.equals(skeleton2)) {
                return getFix(skeleton2);
            }
            if (DEBUG) {
                System.out.println("No pattern for " + skeleton + ", " + path);
            }
            return null;
        }
        return getFixFromPath(path);
    }

    private String getAvailableFormatPath(String skeleton) {
        String path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID +
            "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"" + skeleton +
            "\"]";
        return path;
    }

    public String getFixFromPath(String path) {
        String result = PathHeader.getLinkedView(surveyUrl, file, path);
        return result == null ? "" : result;
    }

    /**
     * Add a table of date comparisons
     *
     * @param english
     * @param output
     */
    public void addDateTable(CLDRFile english, Appendable output) {
        // ldml/dates/calendars/calendar[@type="gregorian"]/months/monthContext[@type="format"]/monthWidth[@type="abbreviated"]/month[@type="1"]
        // ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="stand-alone"]/quarterWidth[@type="wide"]/quarter[@type="1"]
        // ldml/dates/calendars/calendar[@type="gregorian"]/days/dayContext[@type="stand-alone"]/dayWidth[@type="abbreviated"]/day[@type="sun"]
        try {
            output.append("<h2>" + hackDoubleLinked("Weekdays") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/days/dayContext[@type=\"FORMAT\"]/dayWidth[@type=\"WIDTH\"]/day[@type=\"TYPE\"]",
                english, output, "sun", "mon", "tue", "wed", "thu", "fri", "sat");
            output.append("<h2>" + hackDoubleLinked("Months") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/months/monthContext[@type=\"FORMAT\"]/monthWidth[@type=\"WIDTH\"]/month[@type=\"TYPE\"]",
                english, output, "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
            output.append("<h2>" + hackDoubleLinked("Quarters") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/quarters/quarterContext[@type=\"FORMAT\"]/quarterWidth[@type=\"WIDTH\"]/quarter[@type=\"TYPE\"]",
                english, output, "1", "2", "3", "4");
            //            add24HourInfo();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    //    private void add24HourInfo() {
    //        PreferredAndAllowedHour timeInfo = timeData.get(locale);
    //
    //        for (String loc : fac)
    //    }

    private void addDateSubtable(String path, CLDRFile english, Appendable output, String... types) throws IOException {
        path = path.replace("CALENDAR", calendarID);
        output
            .append("<table class='dtf-table'>\n"
                +
                "<tr><th class='dtf-th'>English</th><th class='dtf-th'>Wide</th><th class='dtf-th'>Abbr.</th><th class='dtf-th'>Narrow</th></tr>"
                +
                "\n");
        for (String type : types) {
            String path1 = path.replace("TYPE", type);
            output.append("<tr>");
            boolean first = true;
            for (String width : Arrays.asList("wide", "abbreviated", "narrow")) {
                String path2 = path1.replace("WIDTH", width);
                String last = null;
                String lastPath = null;
                for (String format : Arrays.asList("format", "stand-alone")) {
                    String path3 = path2.replace("FORMAT", format);
                    if (first) {
                        String value = english.getStringValue(path3);
                        output.append("<th class='dtf-left'>").append(TransliteratorUtilities.toHTML.transform(value))
                            .append("</th>");
                        first = false;
                    }
                    String value = file.getStringValue(path3);
                    if (last == null) {
                        last = value;
                        lastPath = path3;
                    } else {
                        String lastFix = getFixFromPath(lastPath);
                        output.append("<td class='dtf-nopad'><table class='dtf-int'><tr><td>").append(
                            TransliteratorUtilities.toHTML.transform(last));
                        if (lastFix != null) {
                            output.append("</td><td class='dtf-fix'>").append(lastFix);
                        }
                        if (!value.equals(last)) {
                            String fix = getFixFromPath(path3);
                            output.append("</td></tr><tr><td>").append(TransliteratorUtilities.toHTML.transform(value));
                            if (fix != null) {
                                output.append("</td><td class='dtf-fix'>").append(fix);
                            }
                        }
                        output.append("</td></tr></table></td>");
                    }
                }
            }
            output.append("</tr>\n");
        }
        output.append("</table>\n");
    }

    private static final boolean RETIRE = false;
    private static final String LOCALES = ".*"; // "da|zh|de|ta";

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.organization, args, true);

        String organization = MyOptions.organization.option.getValue();
        String filter = MyOptions.filter.option.getValue();

        Factory englishFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, filter);
        CLDRFile englishFile = englishFactory.make("en", true);

        Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, LOCALES);
        System.out.println("Total locales: " + factory.getAvailableLanguages().size());
        DateTimeFormats english = new DateTimeFormats().set(englishFile, "gregorian");
        PrintWriter index = openIndex(DIR, "Date/Time");

        Map<String, String> sorted = new TreeMap<String, String>();
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContent = sdi.getDefaultContentLocales();
        for (String localeID : factory.getAvailableLanguages()) {
            Level level = StandardCodes.make().getLocaleCoverageLevel(organization, localeID);
            if (Level.MODERN.compareTo(level) > 0) {
                continue;
            }
            if (defaultContent.contains(localeID)) {
                System.out.println("Skipping default content: " + localeID);
                continue;
            }
            sorted.put(englishFile.getName(localeID, true), localeID);
        }

        writeCss(DIR);
        PrintWriter out;
        // http://st.unicode.org/cldr-apps/survey?_=LOCALE&x=r_datetime&calendar=gregorian
        int oldFirst = 0;
        for (Entry<String, String> nameAndLocale : sorted.entrySet()) {
            String name = nameAndLocale.getKey();
            String localeID = nameAndLocale.getValue();
            DateTimeFormats formats = new DateTimeFormats().set(factory.make(localeID, true), "gregorian");
            String filename = localeID + ".html";
            out = FileUtilities.openUTF8Writer(DIR, filename);
            String redirect = "http://st.unicode.org/cldr-apps/survey?_=" + localeID
                + "&x=r_datetime&calendar=gregorian";
            out.println(
                "<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'><html><head>\n"
                    +
                    (RETIRE ? "<meta http-equiv='REFRESH' content='0;url=" + redirect + "'>\n" : "")
                    +
                    "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
                    +
                    "<title>Date/Time Charts: "
                    + name
                    + "</title>\n"
                    +
                    "<link rel='stylesheet' type='text/css' href='index.css'>\n"
                    +
                    "</head><body><h1>Date/Time Charts: "
                    + name
                    + "</h1>"
                    +
                    "<p><a href='index.html'>Index</a></p>\n"
                    +
                    "<p>The following chart shows typical usage of date and time formatting with the Gregorian calendar. "
                    +
                    "<i>There is important information on <a target='CLDR_ST_DOCS' href='http://cldr.unicode.org/translation/date-time-review'>Date/Time Review</a>, "
                    +
                    "so please read that page before starting!</i></p>\n");
            formats.addTable(english, out);
            formats.addDateTable(englishFile, out);
            formats.addDayPeriods(englishFile, out);
            out.println(
                "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>"
                    +
                    "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>");
            out.println("</body></html>");
            out.close();
            int first = name.codePointAt(0);
            if (oldFirst != first) {
                index.append("<hr>");
                oldFirst = first;
            } else {
                index.append("  ");
            }
            index.append("<a href='").append(filename).append("'>").append(name).append("</a>\n");
            index.flush();
        }
        index.println("</div></body></html>");
        index.close();
    }

    public static PrintWriter openIndex(String directory, String title) throws IOException {
        String dateString = CldrUtility.isoFormatDateOnly(new Date());
        PrintWriter index = FileUtilities.openUTF8Writer(directory, "index.html");
        index
            .println(
                "<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'><html><head>\n"
                    +
                    "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
                    +
                    "<title>"
                    + title
                    + " Charts</title>\n"
                    +
                    "</head><body><h1>"
                    + title
                    + " Charts</h1>"
                    +
                    "<p style='float:left; text-align:left'><a href='../index.html'>Index</a></p>\n"
                    +
                    // "<p style='float:left; text-align:left'><a href='index.html'>Index</a></p>\n" +
                    "<p style='float:right; text-align:right'>"
                    + dateString
                    + "</p>\n"
                    + "<div style='clear:both; margin:2em'>");
        return index;
    }

    public static void writeCss(String directory) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(directory, "index.css");
        out.println(".dtf-table, .dtf-int {margin-left:auto; margin-right:auto; border-collapse:collapse;}\n"
            +
            ".dtf-table, .dtf-s, .dtf-nopad, .dtf-fix, .dtf-th, .dtf-h, .dtf-sep, .dtf-left, .dtf-int {border:1px solid gray;}\n"
            +
            ".dtf-th {background-color:#EEE; padding:4px}\n" +
            ".dtf-s, .dtf-nopad, .dtf-fix {padding:3px; text-align:center}\n" +
            ".dtf-sep {background-color:#EEF; text-align:center}\n" +
            ".dtf-s {text-align:center;}\n" +
            ".dtf-int {width:100%; height:100%}\n" +
            ".dtf-fix {width:1px}\n" +
            ".dtf-left {text-align:left;}\n" +
            ".dtf-nopad {padding:0px; align:top}\n" +
            ".dtf-gray {background-color:#EEF}\n");
        out.close();
    }

    public void addDayPeriods(CLDRFile englishFile, Appendable output) {
        try {
            output.append("<h2>" + hackDoubleLinked("Day Periods") + "</h2>\n");
            output
                .append("<p>Please review these and correct if needed. The Wide fields are the most important. "
                    + "To correct them, go to "
                    + getFixFromPath(ICUServiceBuilder.getDayPeriodPath(DayPeriodInfo.DayPeriod.am, Context.format, Width.wide))
                    + " and following. "
                    + "<b>Note: </b>Day Periods can be a bit tricky; "
                    + "for more information, see <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/date-time-names#TOC-Day-Periods-AM-and-PM-'>Day Periods</a>.</p>\n");
            output
                .append("<table class='dtf-table'>\n"
                    + "<tr>"
                    + "<th class='dtf-th' rowSpan='3'>DayPeriodID</th>"
                    + "<th class='dtf-th' rowSpan='3'>Time Span(s)</th>"
                    + "<th class='dtf-th' colSpan='4'>Format</th>"
                    + "<th class='dtf-th' colSpan='4'>Standalone</th>"

                    + "</tr>\n"
                    + "<tr>"
                    + "<th class='dtf-th' colSpan='2'>Wide</th>"
                    + "<th class='dtf-th'>Abbreviated</th>"
                    + "<th class='dtf-th'>Narrow</th>"
                    + "<th class='dtf-th' colSpan='2'>Wide</th>"
                    + "<th class='dtf-th'>Abbreviated</th>"
                    + "<th class='dtf-th'>Narrow</th>"
                    + "</tr>\n"
                    + "<tr>"
                    + "<th class='dtf-th'>English</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "<th class='dtf-th'>English</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "<th class='dtf-th'>Native</th>"
                    + "</tr>\n");
            DayPeriodInfo dayPeriodInfo = sdi.getDayPeriods(DayPeriodInfo.Type.format, file.getLocaleID());
            Set<DayPeriodInfo.DayPeriod> dayPeriods = new LinkedHashSet<>(dayPeriodInfo.getPeriods());
            DayPeriodInfo dayPeriodInfo2 = sdi.getDayPeriods(DayPeriodInfo.Type.format, "en");
            Set<DayPeriodInfo.DayPeriod> eDayPeriods = EnumSet.copyOf(dayPeriodInfo2.getPeriods());
            Output<Boolean> real = new Output<>();
            Output<Boolean> realEnglish = new Output<>();

            for (DayPeriodInfo.DayPeriod period : dayPeriods) {
                R3<Integer, Integer, Boolean> first = dayPeriodInfo.getFirstDayPeriodInfo(period);
                int midPoint = (first.get0() + first.get1()) / 2;
                output.append("<tr>");
                output.append("<th class='dtf-left'>").append(TransliteratorUtilities.toHTML.transform(period.toString()))
                    .append("</th>\n");
                String periods = dayPeriodInfo.toString(period);
                output.append("<th class='dtf-left'>").append(TransliteratorUtilities.toHTML.transform(periods))
                    .append("</th>\n");
                for (Context context : Context.values()) {
                    for (Width width : Width.values()) {
                        final String dayPeriodPath = ICUServiceBuilder.getDayPeriodPath(period, context, width);
                        if (width == Width.wide) {
                            String englishValue;
                            if (context == Context.format) {
                                englishValue = icuServiceBuilderEnglish.formatDayPeriod(midPoint, context, width);
                                realEnglish.value = true;
                            } else {
                                englishValue = icuServiceBuilderEnglish.getDayPeriodValue(dayPeriodPath, null, realEnglish);
                            }
                            output.append("<th class='dtf-left" + (realEnglish.value ? "" : " dtf-gray") + "'" + ">")
                                .append(getCleanValue(englishValue, width, "<i>unused</i>"))
                                .append("</th>\n");
                        }
                        String nativeValue = icuServiceBuilder.getDayPeriodValue(dayPeriodPath, "�", real);
                        if (context == Context.format) {
                            nativeValue = icuServiceBuilder.formatDayPeriod(midPoint, nativeValue);
                        }
                        output.append("<td class='dtf-left" + (real.value ? "" : " dtf-gray") + "'>")
                            .append(getCleanValue(nativeValue, width, "<i>missing</i>"))
                            .append("</td>\n");
                    }
                }
                output.append("</tr>\n");
            }
            output.append("</table>\n");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private String getCleanValue(String evalue, Width width, String fallback) {
        String replacement = width == Width.wide ? fallback : "<i>optional</i>";
        String qevalue = evalue != null ? TransliteratorUtilities.toHTML.transform(evalue) : replacement;
        return qevalue.replace("�", replacement);
    }

//    static final String SHORT_PATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
//    static final String HM_PATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"hm\"]";
//
//    private String format(CLDRFile file, String evalue, int timeInDay) {
//        String pattern = file.getStringValue(HM_PATH);
//        if (pattern == null) {
//            pattern = "h:mm \uE000";
//        } else {
//            pattern = pattern.replace('a', '\uE000');
//        }
//        SimpleDateFormat df = icuServiceBuilder.getDateFormat("gregorian", pattern);
//        String formatted = df.format(timeInDay);
//        String result = formatted.replace("\uE000", evalue);
//        return result;
//    }

    private String hackDoubleLinked(String link, String name) {
        return name;
    }

    private String hackDoubleLinked(String string) {
        return string;
    }

    static void writeIndexMap(Map<String, String> nameToFile, PrintWriter index) {
        int oldFirst = 0;
        for (Entry<String, String> entry : nameToFile.entrySet()) {
            String name = entry.getKey();
            String file = entry.getValue();
            int first = name.codePointAt(0);
            if (oldFirst != first) {
                index.append("<hr>");
                oldFirst = first;
            } else {
                index.append("  ");
            }
            index.append("<a href='").append(file).append("'>").append(name).append("</a>\n");
            index.flush();
        }
        index.println("</div></body></html>");
    }
}
