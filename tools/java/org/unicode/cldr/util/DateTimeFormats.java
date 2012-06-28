package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
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
    private static final boolean DEBUG = false;

    private static final String FIELDS_TITLE = "Fields";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final String[] STOCK = {"short", "medium", "long", "full"};
    private static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = 
    {
        "G", "y", "M",
        "w", "W", "d", 
        "D", "E", "F",
        "a", "h", "H",
        "m",
    };
    private static final Date SAMPLE_DATE = new Date(2012-1900, 0, 13, 14, 45, 59);

    private static final String SAMPLE_DATE_STRING = CldrUtility.isoFormat(SAMPLE_DATE);

    private static final Date[] SAMPLE_DATE_END =     {
        //        "G", "y", "M",
        null, new Date(2013-1900, 0, 13, 14, 45, 59), new Date(2012-1900, 1, 13, 14, 45, 59),
        //        "w", "W", "d", 
        null, null, new Date(2012-1900, 0, 14, 14, 45, 59),
        //        "D", "E", "F",
        null, new Date(2012-1900, 0, 14, 14, 45, 59), null,
        //        "a", "h", "H",
        new Date(2012-1900, 0, 13, 2, 45, 59), new Date(2012-1900, 0, 13, 15, 45, 59), new Date(2012-1900, 0, 13, 15, 45, 59),
        //        "m",
        new Date(2012-1900, 0, 13, 14, 46, 59)
    };
    ;


    private DateTimePatternGenerator generator;
    private ULocale locale;
    private ICUServiceBuilder icuServiceBuilder;
    private DateIntervalInfo dateIntervalInfo = new DateIntervalInfo();
    private String calendarID;
    private CLDRFile file;

    /**
     * Set a CLDRFile and calendar. Must be done before calling addTable.
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
            String path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateFormats/dateFormatLength[@type=\"" +
                stock +
                "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
            String dateTimePattern = file.getStringValue(path);
            generator.addPattern(dateTimePattern, true, returnInfo);
            path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/timeFormats/timeFormatLength[@type=\"" +
                stock +
                "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
            dateTimePattern = file.getStringValue(path);
            generator.addPattern(dateTimePattern, true, returnInfo);
            if (!haveDefaultHourChar) {
                // use hour style in SHORT time pattern as the default
                // hour style for the locale
                FormatParser fp = new FormatParser();
                fp.set(dateTimePattern);
                List<Object> items = fp.getItems();
                for (int idx = 0; idx < items.size(); idx++) {
                    Object item = items.get(idx);
                    if (item instanceof VariableField) {
                        VariableField fld = (VariableField)item;
                        if (fld.getType() == DateTimePatternGenerator.HOUR) {
                            generator.setDefaultHourFormatChar(fld.toString().charAt(0));
                            haveDefaultHourChar = true;
                            break;
                        }
                    }
                }
            }
        }


        // appendItems                  result.setAppendItemFormat(getAppendFormatNumber(formatName), value);

        // field names                     result.setAppendItemName(i, value);


        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/availableFormats/dateFormatItem"))) {
            String key = parts.set(path).getAttributeValue(-1, "id");
            String value = file.getStringValue(path);
            generator.addPatternWithSkeleton(value, key, true, returnInfo);
        }

        generator.setDateTimeFormat(Calendar.getDateTimePattern(Calendar.getInstance(locale), locale, DateFormat.MEDIUM));

        //ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMMMEd\"]/greatestDifference[@id=\"d\"]
        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem"))) {
            String skeleton = parts.set(path).getAttributeValue(-2, "id");
            String diff = parts.set(path).getAttributeValue(-1, "id");
            int diffNumber = find(CALENDAR_FIELD_TO_PATTERN_LETTER, diff);
            String intervalPattern = file.getStringValue(path);
            dateIntervalInfo.setIntervalPattern(skeleton, diffNumber, intervalPattern);
        }
        dateIntervalInfo.setFallbackIntervalPattern(file.getStringValue("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/intervalFormats/intervalFormatFallback"));
        return this;
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
        { "- interval with month+1", "yMMMM/M" },
        { "- interval with year+1", "yMMMM/y" },
        { "year month day", "yMMMMd" },
        { "- interval with day+1", "yMMMMd/d" },
        { "- interval with month+1", "yMMMMd/M" },
        { "- interval with year+1", "yMMMMd/y" },
        { "year month day weekday", "yMMMMEEEEd" },
        { "- interval with day+1", "yMMMMEEEEd/d" },
        { "- interval with month+1", "yMMMMEEEEd/M" },
        { "- interval with year+1", "yMMMMEEEEd/y" },
        { "month day", "MMMMd" },
        { "- interval with day+1", "MMMMd/d" },
        { "- interval with month+1", "MMMMd/M" },
        { "month day weekday", "MMMMEEEEd" },
        { "- interval with day+1", "MMMMEEEEd/d" },
        { "- interval with month+1", "MMMMEEEEd/M" },

        { "-", "Abbreviated Month" },
        { "year month<sub>a</sub>", "yMMM" },
        { "- interval with month+1", "yMMM/M" },
        { "- interval with year+1", "yMMM/y" },
        { "year month<sub>a</sub> day", "yMMMd" },
        { "- interval with day+1", "yMMMd/d" },
        { "- interval with month+1", "yMMMd/M" },
        { "- interval with year+1", "yMMMd/y" },
        { "year month<sub>a</sub> day weekday", "yMMMEd" },
        { "- interval with day+1", "yMMMEd/d" },
        { "- interval with month+1", "yMMMEd/M" },
        { "- interval with year+1", "yMMMEd/y" },
        { "month<sub>a</sub> day", "MMMd" },
        { "- interval with day+1", "MMMd/d" },
        { "- interval with month+1", "MMMd/M" },
        { "month<sub>a</sub> day weekday", "MMMEd" },
        { "- interval with day+1", "MMMEd/d" },
        { "- interval with month+1", "MMMEd/M" },

        { "-", "Numeric Month" },
        { "year month<sub>n</sub>", "yM" },
        { "- interval with month+1", "yM/M" },
        { "- interval with year+1", "yM/y" },
        { "year month<sub>n</sub> day", "yMd" },
        { "- interval with day+1", "yMd/d" },
        { "- interval with month+1", "yMd/M" },
        { "- interval with year+1", "yMd/y" },
        { "year month<sub>n</sub> day weekday", "yMEd" },
        { "- interval with day+1", "yMEd/d" },
        { "- interval with month+1", "yMEd/M" },
        { "- interval with year+1", "yMEd/y" },
        { "month<sub>n</sub> day", "Md" },
        { "- interval with day+1", "Md/d" },
        { "- interval with month+1", "Md/M" },
        { "month<sub>n</sub> day weekday", "MEd" },
        { "- interval with day+1", "MEd/d" },
        { "- interval with month+1", "MEd/M" },

        { "-", "Other Dates" },
        { "year", "y" },
        { "- interval with year+1", "y/y" },
        { "year quarter", "yQQQQ" },
        { "year quarter<sub>a</sub>", "yQQQ" },
        { "quarter", "QQQQ" },
        { "quarter<sub>a</sub>", "QQQ" },
        { "month", "MMMM" },
        { "- interval with month+1", "MMMM/M" },
        { "month<sub>a</sub>", "MMM" },
        { "- interval with month+1", "MMM/M" },
        { "month<sub>n</sub>", "M" },
        { "- interval with month+1", "M/M" },
        { "day", "d" },
        { "- interval with day+1", "d/d" },
        { "weekday", "EEEE" },
        { "- interval with weekday+1", "EEEE/E" },
        { "weekday<sub>a</sub>", "E" },
        { "- interval with weekday+1", "E/E" },

        { "-", "Times" },
        { "hour", "j" },
        { "- interval with hour+1", "j/j" },
        { "hour minute", "jm" },
        { "- interval with minute+1", "jm/m" },
        { "- interval with hour+1", "jm/j" },
        { "hour minute second", "jms" },
        { "minute second", "ms" },
        { "minute", "m" },
        { "second", "s" },

        { "-", "Times 24h" },
        { "hour<sub>24</sub>", "H" },
        { "- interval with hour+1", "H/H" },
        { "hour<sub>24</sub> minute", "Hm" },
        { "- interval with minute+1", "Hm/m" },
        { "- interval with hour+1", "Hm/H" },
        { "hour<sub>24</sub> minute second", "Hms" },

        //        { "-", "Timezones (used alone or with Time)" },
        //        { "location", "VVVV" },
        //        { "generic", "vvvv" },
        //        { "generic<sub>a</sub>", "v" },
        //        { "specific", "zzzz" },
        //        { "specific<sub>a</sub>", "z" },
        //        { "gmt", "ZZZZ" },
    };


    /**
     * Generate a table of date examples.
     * @param comparison
     * @param output
     */
    public void addTable(DateTimeFormats comparison, Appendable output) {
        try {
            output.append("<table>");
            showRow(output, RowStyle.header, FIELDS_TITLE, "Skeleton", "English Example", "Native Example");
            for (String[] nameAndSkeleton : NAME_AND_PATTERN) {
                String name = nameAndSkeleton[0];
                String skeleton = nameAndSkeleton[1];
                if (name.equals("-")) {
                    showRow(output, RowStyle.separator, skeleton, null, null, null);
                } else {
                    showRow(output, RowStyle.normal, name, skeleton, 
                        comparison.getExample(skeleton), getExample(skeleton));
                }
            }
            output.append("</table>");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get an example from the "enhanced" skeleton.
     * @param skeleton
     * @return
     */
    private String getExample(String skeleton) {
        String example;
        int slashPos = skeleton.indexOf('/');
        if (slashPos >= 0) {
            skeleton = skeleton.replace('j', generator.getDefaultHourFormatChar());
            String mainSkeleton = skeleton.substring(0,slashPos);
            DateIntervalFormat dateIntervalFormat = new DateIntervalFormat(mainSkeleton, dateIntervalInfo, generator, 
                icuServiceBuilder.getDateFormat(calendarID, generator.getBestPattern(mainSkeleton)));
            int diffNumber = find(CALENDAR_FIELD_TO_PATTERN_LETTER, skeleton.substring(slashPos+1));
            Date endDate = SAMPLE_DATE_END[diffNumber];
            try {
                example = dateIntervalFormat.format(new DateInterval(SAMPLE_DATE.getTime(), endDate.getTime()));
            } catch (Exception e) {
                throw new IllegalArgumentException(skeleton + ", " + endDate,e);
            }
        } else {
            String pattern = generator.getBestPattern(skeleton);
            SimpleDateFormat format = icuServiceBuilder.getDateFormat(calendarID, pattern);
            format.setTimeZone(GMT);
            example = format.format(SAMPLE_DATE);
        }
        return TransliteratorUtilities.toHTML.transform(example);
    }

    enum RowStyle {header, separator, normal}

    /**
     * Show a single row
     * @param output
     * @param rowStyle
     * @param name
     * @param skeleton
     * @param english
     * @param example
     * @throws IOException
     */
    private void showRow(Appendable output, RowStyle rowStyle, String name, String skeleton, String english, String example)
        throws IOException {
        output.append("<tr>");
        switch (rowStyle) {
        case separator:
            String link = name.replace(' ', '_');
            output.append("<th colSpan='3' class='dtf-sep'><a href='#").append(link).append("' name='").append(link).append("'>")
            .append(name).append("</a></th>");
            break;
        case header:
        case normal:
            String startCell = rowStyle == RowStyle.header ? "<th class='dtf-h'>" : "<td class='dtf-s'>";
            String endCell = rowStyle == RowStyle.header  ? "</th>" : "</td>";
            if (name.equals(FIELDS_TITLE)) {
                output.append("<th>").append(name).append("</a></th>");
            } else {
                output.append("<th><a " +
                    "href='#" + skeleton + "' " +
                    "name='" + skeleton + "' " +
                    ">").append(name).append("</a></th>");
            }
            //.append(startCell).append(skeleton).append(endCell)
            output.append(startCell).append(english).append(endCell)
            .append(startCell).append(example).append(endCell);
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
            String mainSkeleton = skeleton.substring(0,slashPos);
            String diff = skeleton.substring(slashPos+1);
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
        String strid = Long.toHexString(StringId.getId(path));
        return "<a href='http://st.unicode.org/cldr-apps/survey?_=" + file.getLocaleID() +
            "&strid=" + strid + "'><i>fix</i></a>";
    }

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Factory englishFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile englishFile = englishFactory.make("en", true);
        String dateString = CldrUtility.isoFormat(new Date());

        Factory factory = Factory.make(CldrUtility.TMP2_DIRECTORY + "vxml/common/main/", ".*");
        System.out.println("Total locales: " + factory.getAvailableLanguages().size());
        DateTimeFormats english = new DateTimeFormats().set(englishFile, "gregorian");
        PrintWriter index = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "dates/", "index.html");
        index.println(
            "<html><head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>Date/Time Charts</title>\n" +
                "</head><body><h1>Date/Time Charts</h1>" +
                //"<p style='float:left; text-align:left'><a href='index.html'>Index</a></p>\n" +
                "<p style='float:right; text-align:right'>" + dateString + "</p>\n" +
                "<p style='clear:both'>The following charts show typical usage of date and time formatting with the Gregorian calendar. " +
            "Please review the chart for your locale(s).</p><div style='margin:2em'>");

        Map<String, String> sorted = new TreeMap<String,String>();
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContent = sdi.getDefaultContentLocales();
        for (String localeID : factory.getAvailableLanguages()) {
            if (defaultContent.contains(localeID)) {
                System.out.println("Skipping default content: " + localeID);
                continue;
            }
            sorted.put(englishFile.getName(localeID,true), localeID);
        }

        int oldFirst = 0;
        for (Entry<String, String> nameAndLocale : sorted.entrySet()) {
            String name = nameAndLocale.getKey();
            String localeID = nameAndLocale.getValue();
            DateTimeFormats formats = new DateTimeFormats().set(factory.make(localeID, true), "gregorian");
            String filename = localeID + ".html";
            PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "dates/", filename);
            out.println(
                "<html><head>\n" +
                    "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                    "<title>Date/Time Charts: " + name + "</title>\n" +
                    "<style type='text/css'>\n" +
                    "table, td, th {border-collapse:collapse; border:1px solid gray;}\n" +
                    "table {margin-left:auto; margin-right:auto}\n" +
                    "th {text-align:left; background-color:#EEE; padding:4px}\n" +
                    "td {padding:3px}\n" +
                    ".dtf-sep {background-color:#EEF; text-align:center}\n" +
                    ".dtf-s {text-align:center;}\n" +
                    "</style>\n" +
                    "</head><body><h1>Date/Time Charts: " + name + "</h1>" +
                    "<p style='float:left; text-align:left'><a href='index.html'>Index</a></p>\n" +
                    "<p style='float:right; text-align:right'>" + dateString + "</p>\n" +
                    "<p style='clear:both'>The following chart shows typical usage of date and time formatting with the Gregorian calendar. " +
                    "If any of these look incorrect for your language <i>or are inconsistent with other rows!</i>, please fix them as described on " +
                    "<a href='http://cldr.unicode.org/translation/date-time-review'>Date/Time Review</a>." +
                    "</p>\n" +
                    "<p>The base date is " + SAMPLE_DATE_STRING + ". The date/time intervals have that as the starting date. " +
                "The 'Times 24h' is only relevant where the preferred format is 12 hour.</p>");
            formats.addTable(english, out);
            out.println("<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>" +
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
