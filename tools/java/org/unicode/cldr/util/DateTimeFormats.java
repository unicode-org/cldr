package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.PatternInfo;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class DateTimeFormats {
    private static final String TIMES_24H_TITLE = "Times 24h";
    private static final boolean DEBUG = true;
    private static final String DEBUG_SKELETON = "y";
    private static final ULocale DEBUG_LIST_PATTERNS = ULocale.JAPANESE; // or null;

    private static final String FIELDS_TITLE = "Fields";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final String[] STOCK = { "short", "medium", "long", "full" };
    private static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER =
    {
        "G", "y", "M",
        "w", "W", "d",
        "D", "E", "F",
        "a", "h", "H",
        "m",
    };
    private static final Date SAMPLE_DATE = new Date(2012 - 1900, 0, 13, 14, 45, 59);

    private static final String SAMPLE_DATE_STRING = CldrUtility.isoFormat(SAMPLE_DATE);

    private static final Date[] SAMPLE_DATE_END = {
        // "G", "y", "M",
        null, new Date(2013 - 1900, 0, 13, 14, 45, 59), new Date(2012 - 1900, 1, 13, 14, 45, 59),
        // "w", "W", "d",
        null, null, new Date(2012 - 1900, 0, 14, 14, 45, 59),
        // "D", "E", "F",
        null, new Date(2012 - 1900, 0, 14, 14, 45, 59), null,
        // "a", "h", "H",
        new Date(2012 - 1900, 0, 13, 2, 45, 59), new Date(2012 - 1900, 0, 13, 15, 45, 59),
        new Date(2012 - 1900, 0, 13, 15, 45, 59),
        // "m",
        new Date(2012 - 1900, 0, 13, 14, 46, 59)
    };;

    private DateTimePatternGenerator generator;
    private ULocale locale;
    private ICUServiceBuilder icuServiceBuilder;
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
        this.file = file;
        locale = new ULocale(file.getLocaleID());
        icuServiceBuilder = new ICUServiceBuilder().setCldrFile(file);
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
            generator.addPattern(dateTimePattern, true, returnInfo);
            path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID
                + "\"]/timeFormats/timeFormatLength[@type=\"" +
                stock +
                "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
            dateTimePattern = file.getStringValue(path);
            generator.addPattern(dateTimePattern, true, returnInfo);
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
        // ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="day"]/displayName
        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/fields/field"))) {
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
        dateIntervalInfo.setFallbackIntervalPattern(file.getStringValue("//ldml/dates/calendars/calendar[@type=\""
            + calendarID + "\"]/dateTimeFormats/intervalFormats/intervalFormatFallback"));
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
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMMM/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMMM/y" },
        { "year month day", "yMMMMd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMMMMd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMMMd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMMMd/y" },
        { "year month day weekday", "yMMMMEEEEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMMMMEEEEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMMMEEEEd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMMMEEEEd/y" },
        { "month day", "MMMMd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "MMMMd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMMMd/M" },
        { "month day weekday", "MMMMEEEEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "MMMMEEEEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMMMEEEEd/M" },

        { "-", "Abbreviated Month" },
        { "year month<sub>a</sub>", "yMMM" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMM/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMM/y" },
        { "year month<sub>a</sub> day", "yMMMd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMMMd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMMd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMMd/y" },
        { "year month<sub>a</sub> day weekday", "yMMMEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMMMEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMMMEd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMMMEd/y" },
        { "month<sub>a</sub> day", "MMMd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "MMMd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMMd/M" },
        { "month<sub>a</sub> day weekday", "MMMEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "MMMEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMMEd/M" },

        { "-", "Numeric Month" },
        { "year month<sub>n</sub>", "yM" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yM/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yM/y" },
        { "year month<sub>n</sub> day", "yMd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMd/y" },
        { "year month<sub>n</sub> day weekday", "yMEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "yMEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "yMEd/M" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "yMEd/y" },
        { "month<sub>n</sub> day", "Md" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "Md/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "Md/M" },
        { "month<sub>n</sub> day weekday", "MEd" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "MEd/d" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MEd/M" },

        { "-", "Other Dates" },
        { "year", "y" },
        { "&nbsp;&nbsp;&nbsp;to  year+1", "y/y" },
        { "year quarter", "yQQQQ" },
        { "year quarter<sub>a</sub>", "yQQQ" },
        { "quarter", "QQQQ" },
        { "quarter<sub>a</sub>", "QQQ" },
        { "month", "MMMM" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMMM/M" },
        { "month<sub>a</sub>", "MMM" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "MMM/M" },
        { "month<sub>n</sub>", "M" },
        { "&nbsp;&nbsp;&nbsp;to  month+1", "M/M" },
        { "day", "d" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "d/d" },
        { "day weekday", "Ed" },
        { "&nbsp;&nbsp;&nbsp;to  day+1", "Ed/d" },
        { "weekday", "EEEE" },
        { "&nbsp;&nbsp;&nbsp;to  weekday+1", "EEEE/E" },
        { "weekday<sub>a</sub>", "E" },
        { "&nbsp;&nbsp;&nbsp;to  weekday+1", "E/E" },

        { "-", "Times" },
        { "hour", "j" },
        { "&nbsp;&nbsp;&nbsp;to  hour+1", "j/j" },
        { "hour minute", "jm" },
        { "&nbsp;&nbsp;&nbsp;to  minute+1", "jm/m" },
        { "&nbsp;&nbsp;&nbsp;to  hour+1", "jm/j" },
        { "hour minute second", "jms" },
        { "minute second", "ms" },
        { "minute", "m" },
        { "second", "s" },

        { "-", TIMES_24H_TITLE },
        { "hour<sub>24</sub>", "H" },
        { "&nbsp;&nbsp;&nbsp;to  hour+1", "H/H" },
        { "hour<sub>24</sub> minute", "Hm" },
        { "&nbsp;&nbsp;&nbsp;to  minute+1", "Hm/m" },
        { "&nbsp;&nbsp;&nbsp;to  hour+1", "Hm/H" },
        { "hour<sub>24</sub> minute second", "Hms" },

        // { "-", "Timezones (used alone or with Time)" },
        // { "location", "VVVV" },
        // { "generic", "vvvv" },
        // { "generic<sub>a</sub>", "v" },
        // { "specific", "zzzz" },
        // { "specific<sub>a</sub>", "z" },
        // { "gmt", "ZZZZ" },
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
            output.append("<h2>" + CldrUtility.getDoubleLinkedText("Patterns") + "</h2>\n<table class='dtf-table'>");
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
                    showRow(output, RowStyle.normal, name, skeleton,
                        comparison.getExample(skeleton), getExample(skeleton), diff.isPresent(skeleton));
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
                    showRow(output, RowStyle.normal, skeleton, skeleton,
                        comparison.getExample(skeleton), getExample(skeleton), true);
                }
            }
            output.append("</table>");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
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
        int slashPos = skeleton.indexOf('/');
        if (slashPos >= 0) {
            String mainSkeleton = skeleton.substring(0, slashPos);
            DateIntervalFormat dateIntervalFormat = new DateIntervalFormat(mainSkeleton, dateIntervalInfo,
                icuServiceBuilder.getDateFormat(calendarID, generator.getBestPattern(mainSkeleton)));
            String diffString = skeleton.substring(slashPos + 1).replace('j', 'H');
            int diffNumber = find(CALENDAR_FIELD_TO_PATTERN_LETTER, diffString);
            Date endDate = SAMPLE_DATE_END[diffNumber];
            try {
                example = dateIntervalFormat.format(new DateInterval(SAMPLE_DATE.getTime(), endDate.getTime()));
            } catch (Exception e) {
                throw new IllegalArgumentException(skeleton + ", " + endDate, e);
            }
        } else {
            if (skeleton.equals(DEBUG_SKELETON)) {
                int debug = 0;
            }
            SimpleDateFormat format = getDateFormat(skeleton);
            example = format.format(SAMPLE_DATE);
        }
        return TransliteratorUtilities.toHTML.transform(example);
    }

    public SimpleDateFormat getDateFormat(String skeleton) {
        String pattern = getBestPattern(skeleton);
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
                .append(CldrUtility.getDoubleLinkedText(link, name))
                .append("</th>");
            break;
        case header:
        case normal:
            String startCell = rowStyle == RowStyle.header ? "<th class='dtf-h'>" : "<td class='dtf-s'>";
            String endCell = rowStyle == RowStyle.header ? "</th>" : "</td>";
            if (name.equals(FIELDS_TITLE)) {
                output.append("<th class='dtf-th'>").append(name).append("</a></th>");
            } else {
                output.append("<th class='dtf-left'>" + CldrUtility.getDoubleLinkedText(skeleton, name) + "</th>");
            }
            // .append(startCell).append(skeleton).append(endCell)
            output.append(startCell).append(english).append(endCell)
                .append(startCell).append(example).append(endCell)
                .append(startCell).append(isPresent ? "&nbsp;" : "c").append(endCell);
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
            path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID +
                "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"" + skeleton +
                "\"]";
        }
        String value = file.getStringValue(path);
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

    public String getFixFromPath(String path) {
        String value = file.getStringValue(path);
        if (value == null) {
            return null;
        }
        String strid = Long.toHexString(StringId.getId(path));
        return "<a href='" + surveyUrl + "?_=" + file.getLocaleID() +
            "&strid=" + strid + "'><i>fix</i></a>";
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
            output.append("<h2>" + CldrUtility.getDoubleLinkedText("Weekdays") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/days/dayContext[@type=\"FORMAT\"]/dayWidth[@type=\"WIDTH\"]/day[@type=\"TYPE\"]",
                english, output, "sun", "mon", "tue", "wed", "thu", "fri", "sat");
            output.append("<h2>" + CldrUtility.getDoubleLinkedText("Months") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/months/monthContext[@type=\"FORMAT\"]/monthWidth[@type=\"WIDTH\"]/month[@type=\"TYPE\"]",
                english, output, "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
            output.append("<h2>" + CldrUtility.getDoubleLinkedText("Quarters") + "</h2>\n");
            addDateSubtable(
                "//ldml/dates/calendars/calendar[@type=\"CALENDAR\"]/quarters/quarterContext[@type=\"FORMAT\"]/quarterWidth[@type=\"WIDTH\"]/quarter[@type=\"TYPE\"]",
                english, output, "1", "2", "3", "4");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

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
    private static final String LOCALES = "ja";

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Factory englishFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile englishFile = englishFactory.make("en", true);
        String dateString = CldrUtility.isoFormat(new Date());

        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, LOCALES);
        System.out.println("Total locales: " + factory.getAvailableLanguages().size());
        DateTimeFormats english = new DateTimeFormats().set(englishFile, "gregorian");
        PrintWriter index = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "dates/", "index.html");
        index
            .println(
            "<html><head>\n"
                +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
                +
                "<title>Date/Time Charts</title>\n"
                +
                "</head><body><h1>Date/Time Charts</h1>"
                +
                // "<p style='float:left; text-align:left'><a href='index.html'>Index</a></p>\n" +
                "<p style='float:right; text-align:right'>"
                + dateString
                + "</p>\n"
                +
                "<p style='clear:both'><b>The charts have been incorporated into the Survey Tool, as Date/Time Review. </b></p>\n"
                +
                "<p>The following charts show typical usage of date and time formatting with the Gregorian calendar. " +
                "Please review the chart for your locale(s).</p><div style='margin:2em'>");

        Map<String, String> sorted = new TreeMap<String, String>();
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContent = sdi.getDefaultContentLocales();
        for (String localeID : factory.getAvailableLanguages()) {
            if (defaultContent.contains(localeID)) {
                System.out.println("Skipping default content: " + localeID);
                continue;
            }
            sorted.put(englishFile.getName(localeID, true), localeID);
        }

        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "dates/", "index.css");
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
            ".dtf-nopad {padding:0px; align:top}\n"
            );
        out.close();
        // http://st.unicode.org/cldr-apps/survey?_=LOCALE&x=r_datetime&calendar=gregorian
        int oldFirst = 0;
        for (Entry<String, String> nameAndLocale : sorted.entrySet()) {
            String name = nameAndLocale.getKey();
            String localeID = nameAndLocale.getValue();
            DateTimeFormats formats = new DateTimeFormats().set(factory.make(localeID, true), "gregorian");
            String filename = localeID + ".html";
            out = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "dates/", filename);
            String redirect = "http://st.unicode.org/cldr-apps/survey?_=" + localeID
                + "&x=r_datetime&calendar=gregorian";
            out.println(
                "<html><head>\n"
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
                    "<p style='float:left; text-align:left'><a href='index.html'>Index</a></p>\n"
                    +
                    "<p style='float:right; text-align:right'><i>Last Generated: "
                    + dateString
                    + "</i></p>\n"
                    +
                    "<p style='clear:both'><b>The charts have been incorporated into the Survey Tool, as Date/Time Review: "
                    +
                    "please go to <a href='"
                    + redirect
                    + "'>"
                    + redirect
                    + "</a></b>.</p>"
                    +
                    "<p>The following chart shows typical usage of date and time formatting with the Gregorian calendar. "
                    +
                    "<i>There is important information on <a href='http://cldr.unicode.org/translation/date-time-review'>Date/Time Review</a>, "
                    +
                    "so please read that page before starting!</i></p>\n");
            formats.addTable(english, out);
            formats.addDateTable(englishFile, out);
            out.println("<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>"
                +
                "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>");
            out.println("</body></html>");
            out.close();
            int first = name.codePointAt(0);
            if (oldFirst != first) {
                index.append("<hr>");
                oldFirst = first;
            } else {
                index.append(" &nbsp;");
            }
            index.append("<a href='").append(filename).append("'>").append(name).append("</a>\n");
            index.flush();
        }
        index.println("</div></body></html>");
        index.close();
    }
}
