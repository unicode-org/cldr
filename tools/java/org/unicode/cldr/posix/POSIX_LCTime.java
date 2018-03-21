/*
 **********************************************************************
 * Copyright (c) 2004-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 **********************************************************************
 */

package org.unicode.cldr.posix;

import java.io.PrintWriter;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.text.NumberingSystem;

public class POSIX_LCTime {
    String abday[];
    String day[];
    String abmon[];
    String mon[];
    String d_t_fmt;
    String d_fmt;
    String t_fmt;
    String am_pm[];
    String t_fmt_ampm;
    String alt_digits[];

    // platform specific
    String date_fmt;
    String nlldate;

    public POSIX_LCTime(CLDRFile doc, POSIXVariant variant) {
        abday = new String[7];
        day = new String[7];
        abmon = new String[12];
        mon = new String[12];
        am_pm = new String[2];
        alt_digits = new String[100];

        String[] days = { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };
        String SearchLocation;

        for (int i = 0; i < 7; i++) {
            // Get each value for abbreviated day names ( abday )
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='abbreviated']/day[@type='"
                + days[i] + "']";
            abday[i] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));

            // Get each value for full month names ( day )
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='wide']/day[@type='"
                + days[i] + "']";
            day[i] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));
        }

        for (int i = 0; i < 12; i++) {
            // Get each value for abbreviated month names ( abmon )
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/months/monthContext[@type='format']/monthWidth[@type='abbreviated']/month[@type='"
                + String.valueOf(i + 1) + "']";
            abmon[i] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));

            // Get each value for full month names ( mon )
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/months/monthContext[@type='format']/monthWidth[@type='wide']/month[@type='"
                + String.valueOf(i + 1) + "']";
            mon[i] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));
        }

        // alt_digits
        alt_digits[0] = "";
        String numsys = doc.getWinningValue("//ldml/numbers/defaultNumberingSystem");
        if (numsys != null && !numsys.equals("latn")) {
            NumberingSystem ns = NumberingSystem.getInstanceByName(numsys);
            String nativeZeroDigit = ns.getDescription().substring(0, 1);
            // Character ThisDigit;
            alt_digits[0] = POSIXUtilities.POSIXCharName(nativeZeroDigit);
            char base_value = nativeZeroDigit.charAt(0);
            for (short i = 1; i < 10; i++)
                alt_digits[i] = POSIXUtilities.POSIXCharName(Character.toString(((char) ((short) base_value + i))));
            for (short i = 10; i < 100; i++)
                alt_digits[i] = alt_digits[i / 10] + alt_digits[i % 10];
        }

        // t_fmt -
        SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern[@type='standard']";
        t_fmt = POSIXUtilities.POSIXDateTimeFormat(doc.getWinningValue(SearchLocation), alt_digits[0].length() > 0,
            variant);
        t_fmt = t_fmt.replaceAll("\"", "/\""); // excaping of " in strings

        if (t_fmt.indexOf("%p") >= 0)
            t_fmt_ampm = t_fmt;
        else {
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms']";
            t_fmt_ampm = doc.getWinningValue(SearchLocation);

            if (t_fmt_ampm != null) {
                t_fmt_ampm = POSIXUtilities.POSIXDateTimeFormat(t_fmt_ampm, alt_digits[0].length() > 0, variant);
                t_fmt_ampm = t_fmt_ampm.replaceAll("\"", "/\""); // excaping of " in strings
            }

            if (t_fmt_ampm.indexOf("%p") < 0)
                t_fmt_ampm = "";

        }
        // d_fmt -
        SearchLocation = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]" +
            "/dateFormats/dateFormatLength[@type=\"short\"]" +
            "/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        d_fmt = POSIXUtilities.POSIXDateTimeFormat(doc.getWinningValue(SearchLocation), alt_digits[0].length() > 0,
            variant);
        d_fmt = d_fmt.replaceAll("\"", "/\""); // excaping of " in strings

        // d_t_fmt -
        SearchLocation = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]" +
            "/dateTimeFormats/dateTimeFormatLength[@type=\"long\"]" +
            "/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        d_t_fmt = doc.getWinningValue(SearchLocation);

        SearchLocation = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]" +
            "/timeFormats/timeFormatLength[@type=\"long\"]" +
            "/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        d_t_fmt = d_t_fmt.replaceAll("\\{0\\}", doc.getWinningValue(SearchLocation));

        SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']" +
            "/dateFormats/dateFormatLength[@type=\"long\"]" +
            "/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        d_t_fmt = d_t_fmt.replaceAll("\\{1\\}", doc.getWinningValue(SearchLocation));

        d_t_fmt = POSIXUtilities.POSIXDateTimeFormat(d_t_fmt, alt_digits[0].length() > 0, variant);
        d_t_fmt = POSIXUtilities.POSIXCharNameNP(d_t_fmt);
        d_t_fmt = d_t_fmt.replaceAll("\"", "/\""); // excaping of " in strings

        // am_pm
        SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']" +
            "/dayPeriods/dayPeriodContext[@type='format']/dayPeriodWidth[@type='wide']" +
            "/dayPeriod[@type='am']";
        am_pm[0] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));

        SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']" +
            "/dayPeriods/dayPeriodContext[@type='format']/dayPeriodWidth[@type='wide']" +
            "/dayPeriod[@type='pm']";
        am_pm[1] = POSIXUtilities.POSIXCharName(doc.getWinningValue(SearchLocation));

        if (variant.platform.equals(POSIXVariant.SOLARIS)) {
            // date_fmt
            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/dateTimeFormatLength/dateTimeFormat/pattern";
            date_fmt = doc.getWinningValue(SearchLocation);

            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='full']/timeFormat/pattern";
            date_fmt = date_fmt.replaceAll("\\{0\\}", doc.getWinningValue(SearchLocation));

            SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateFormats/dateFormatLength[@type='full']/dateFormat/pattern";
            date_fmt = date_fmt.replaceAll("\\{1\\}", doc.getWinningValue(SearchLocation));

            date_fmt = POSIXUtilities.POSIXDateTimeFormat(date_fmt, alt_digits[0].length() > 0, variant);
            date_fmt = POSIXUtilities.POSIXCharNameNP(date_fmt);
            date_fmt = date_fmt.replaceAll("\"", "/\""); // excaping of " in strings

        } else if (variant.platform.equals(POSIXVariant.AIX)) {
            // nlldate - prefer 0 padded date, if only alt nodes are found then an exception is thrown, in this case use
            // fallback
            nlldate = "";
            String SearchLocations[] = {
                "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='yyyyMMMdd']",
                "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='yyyyMMMd']" };
            for (int i = 0; i < SearchLocations.length; i++) {
                nlldate = doc.getWinningValue(SearchLocation); // throws exception if only alt nodes found, returns null
                // if nothing found
                if (nlldate != null) break;
            }

            if (nlldate != null)
                nlldate = POSIXUtilities.POSIXDateTimeFormat(nlldate, alt_digits[0].length() > 0, variant);

            // if no match found or erroneous data, use default dd MMM yyyy
            if ((nlldate.indexOf("%d") < 0) || (nlldate.indexOf("%b") < 0) || (nlldate.indexOf("%Y") < 0)) {
                nlldate = "%d %b %Y"; // dd MMM yyyy
                nlldate = nlldate.replaceAll("\"", "/\""); // excaping of " in strings
            }
        }

    }

    public void write(PrintWriter out, POSIXVariant variant) {

        out.println("*************");
        out.println("LC_TIME");
        out.println("*************");
        out.println();

        // abday
        out.print("abday   \"");
        for (int i = 0; i < 7; i++) {
            out.print(abday[i]);
            if (i < 6) {
                out.println("\";/");
                out.print("        \"");
            } else
                out.println("\"");
        }
        out.println();

        // day
        out.print("day     \"");
        for (int i = 0; i < 7; i++) {
            out.print(day[i]);
            if (i < 6) {
                out.println("\";/");
                out.print("        \"");
            } else
                out.println("\"");
        }
        out.println();

        // abmon
        out.print("abmon   \"");
        for (int i = 0; i < 12; i++) {
            out.print(abmon[i]);
            if (i < 11) {
                out.println("\";/");
                out.print("        \"");
            } else
                out.println("\"");
        }
        out.println();

        // mon
        out.print("mon     \"");
        for (int i = 0; i < 12; i++) {
            out.print(mon[i]);
            if (i < 11) {
                out.println("\";/");
                out.print("        \"");
            } else
                out.println("\"");
        }
        out.println();

        // d_fmt
        out.println("d_fmt    \"" + d_fmt + "\"");
        out.println();

        // t_fmt
        out.println("t_fmt    \"" + t_fmt + "\"");
        out.println();

        // d_t_fmt
        out.println("d_t_fmt  \"" + d_t_fmt + "\"");
        out.println();

        // am_pm
        out.println("am_pm    \"" + am_pm[0] + "\";\"" + am_pm[1] + "\"");
        out.println();

        // t_fmt_ampm
        out.println("t_fmt_ampm  \"" + t_fmt_ampm + "\"");
        out.println();

        if (variant.platform.equals(POSIXVariant.SOLARIS)) {
            // date_fmt
            out.println("date_fmt    \"" + date_fmt + "\"");
            out.println();
        } else if (variant.platform.equals(POSIXVariant.AIX)) {
            // nlldate
            out.println("nlldate    \"" + nlldate + "\"");
            out.println();
        }

        // alt_digits
        if (!alt_digits[0].equals("")) {
            out.print("alt_digits \"");
            for (int i = 0; i < 100; i++) {
                out.print(alt_digits[i]);
                if (i < 99) {
                    out.println("\";/");
                    out.print("           \"");
                } else
                    out.println("\"");
            }
        }
        out.println("END LC_TIME");

    }
}
