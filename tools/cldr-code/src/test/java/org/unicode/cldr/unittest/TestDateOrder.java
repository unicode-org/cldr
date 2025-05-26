package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.CheckDates;
import org.unicode.cldr.test.DateOrder;
import org.unicode.cldr.test.RelatedDatePathValues;
import org.unicode.cldr.test.RelatedDatePathValues.SkeletonPathType;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class TestDateOrder extends TestFmwk {
    private static final Joiner JOIN_TAB = Joiner.on('\t');

    public static void main(String[] args) {
        new TestDateOrder().run(args);
    }

    public void TestDateImportance() {

        try {
            // Build test file

            XMLSource source = new SimpleXMLSource("xx");
            // add xpaths
            String fullDate =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat/pattern";
            source.putValueAtPath(fullDate, "EEEE, dd MMMM, y");
            String longDate =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"long\"]/dateFormat/pattern";
            source.putValueAtPath(longDate, "dd MMMM y");
            String mediumDate =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat/pattern";
            source.putValueAtPath(mediumDate, "dd-MMM-y");
            String shortDate =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat/pattern";
            source.putValueAtPath(shortDate, "dd/MM/yy");
            String availableFormat =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"yMd\"]";
            source.putValueAtPath(availableFormat, "M/d/y");
            String intervalFormat =
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMd\"]/greatestDifference[@id=\"y\"]";
            source.putValueAtPath(intervalFormat, "d/M/y – d/M/y");
            CLDRFile cldrFile = new CLDRFile(source);

            // Now test

            DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
            Map<String, Map<DateOrder, String>> order =
                    DateOrder.getOrderingInfo(cldrFile, cldrFile, fp);
            assertNull("There should be no conflicts", order.get(fullDate));

            Collection<String> values = order.get(availableFormat).values();
            assertEquals("There should only one conflict", 1, values.size());

            values = order.get(intervalFormat).values();
            assertTrue(
                    "There should be a conflict between the interval format and available format",
                    values.contains(availableFormat));

            source.putValueAtPath(fullDate, "EEEE, y MMMM dd");
            order = DateOrder.getOrderingInfo(cldrFile, cldrFile, fp);
            values = new HashSet<>(order.get(fullDate).values()); // filter
            // duplicates
            assertEquals("There should be a conflict with other date values", 1, values.size());
            assertTrue("No conflict with long date", values.contains(longDate));

            values = order.get(availableFormat).values();
            assertEquals(
                    "There should be conflicts with this available format and date formats",
                    2,
                    values.size());
            assertTrue("No conflict with full date", values.contains(fullDate));
            assertTrue("No conflict with short date", values.contains(shortDate));

            values = order.get(intervalFormat).values();
            assertTrue("Available format conflict not found", values.contains(availableFormat));
            assertTrue("Date format conflict not found", values.contains(fullDate));
        } catch (Exception e) {
            warnln("Finish testing Date Order " + e.getMessage());
        }
    }

    static final String stockDatePathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength";
    static final String stockTimePathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength";
    static final String availableFormatPathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem";
    static final String intervalFormatPathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/";

    public void TestIso8601() {
        List<String> printout = null;
        if (isVerbose()) {
            printout = new ArrayList<>();
        } else {
            warnln("Use -v to see a comparison between calendars");
        }

        ICUServiceBuilder isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance("en"));
        ICUServiceBuilder isbCan = ICUServiceBuilder.forLocale(CLDRLocale.getInstance("en_CA"));
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        CLDRFile englishCan = CLDRConfig.getInstance().getCldrFactory().make("en_CA", true);
        Factory phf = PathHeader.getFactory();

        Set<PathHeader> paths = new TreeSet<>();
        for (String path : english) {
            if (!path.startsWith("//ldml/dates/calendars/calendar[@type=\"gregorian\"]")) {
                continue;
            } else if (path.startsWith(stockTimePathPrefix)
                    || path.startsWith(stockDatePathPrefix)) {
                if (!path.contains("datetimeSkeleton")) {
                    paths.add(phf.fromPath(path));
                }
            } else if (path.startsWith(availableFormatPathPrefix)) {
                paths.add(phf.fromPath(path));
            } else if (path.startsWith(intervalFormatPathPrefix)) {
                if (!path.contains("intervalFormatFallback")) {
                    paths.add(phf.fromPath(path));
                }
            } else {
                int debug = 0;
            }
        }
        Date sample = Date.from(Instant.parse("2024-01-13T19:08:09Z"));
        SimpleDateFormat neutralFormat =
                new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss X", Locale.ROOT);
        neutralFormat.setTimeZone(TimeZone.GMT_ZONE);

        for (PathHeader pathHeader : paths) {
            final String originalPath = pathHeader.getOriginalPath();
            String code = pathHeader.getCode();

            if (originalPath.startsWith(stockTimePathPrefix)) {
                code = "time-" + code;
            } else if (originalPath.startsWith(stockDatePathPrefix)) {
                code = "date-" + code;
            }
            String gregPat = english.getStringValue(originalPath);
            String isoPat =
                    english.getStringValue(originalPath.replace("\"gregorian\"", "\"iso8601\""));
            String canPat = englishCan.getStringValue(originalPath);

            String gregFormatted = null;
            String isoFormatted = null;
            String canFormatted = null;

            String sampleDate = null;

            if (originalPath.contains("intervalFormats")) {
                Date sample1 = (Date) sample.clone();
                Date sample2 = (Date) sample.clone();
                XPathParts parts = XPathParts.getFrozenInstance(originalPath);
                String greatestDifference = parts.getAttributeValue(-1, "id");

                switch (greatestDifference) {
                    case "G":
                        sample1.setYear(sample.getYear() - 3000);
                        break;
                    case "y":
                        sample2.setYear(sample.getYear() + 1);
                        break;
                    case "M":
                        sample2.setMonth(sample.getMonth() + 1);
                        break;
                    case "d":
                        sample2.setDate(sample.getDate() + 1);
                        break;
                    case "h":
                    case "H":
                        sample2.setHours(sample.getHours() + 1);
                        break;
                    case "a":
                    case "B":
                        sample1.setHours(sample.getHours() - 12);
                        break;
                    case "m":
                        sample2.setMinutes(sample.getMinutes() + 1);
                        break;
                    case "s":
                        sample2.setSeconds(sample.getSeconds() + 1);
                        break;
                    default:
                        System.out.println("Missing" + greatestDifference);
                        break;
                }
                sampleDate = neutralFormat.format(sample1) + " - " + neutralFormat.format(sample2);

                check(isoPat, Set.of(Check.dayperiod));

                List<String> parts2 = splitIntervalPattern(isoPat);
                check(
                        parts2.get(0),
                        Set.of(Check.order, Check.uniqueness)); // check first part of interval
                check(
                        parts2.get(2),
                        Set.of(Check.order, Check.uniqueness)); // check second part of interval

                gregFormatted = formatInterval(isb, sample1, sample2, "gregorian", gregPat);
                isoFormatted = formatInterval(isb, sample1, sample2, "iso8601", isoPat);
                canFormatted = formatInterval(isbCan, sample1, sample2, "gregorian", canPat);
            } else {
                check(isoPat, Set.of(Check.order, Check.uniqueness, Check.dayperiod));

                sampleDate = neutralFormat.format(sample);

                SimpleDateFormat gregFormat = isb.getDateFormat("gregorian", gregPat);
                gregFormat.setTimeZone(TimeZone.GMT_ZONE);
                SimpleDateFormat isoFormat = isb.getDateFormat("iso8601", isoPat);
                isoFormat.setTimeZone(TimeZone.GMT_ZONE);
                SimpleDateFormat caFormat = isbCan.getDateFormat("gregorian", gregPat);
                caFormat.setTimeZone(TimeZone.GMT_ZONE);

                gregFormatted = gregFormat.format(sample);
                isoFormatted = isoFormat.format(sample);
                canFormatted = caFormat.format(sample);
            }
            if (printout != null) {
                canFormatted = canFormatted.replace("a.m.", "AM").replace("p.m.", "PM");
                printout.add(
                        JOIN_TAB.join(
                                code,
                                gregPat,
                                isoPat,
                                canPat,
                                sampleDate,
                                gregFormatted,
                                isoFormatted,
                                canFormatted));
            }
        }
        if (printout != null) {
            System.out.println();
            for (String line : printout) {
                System.out.println(line);
            }
        }
    }

    static final List<Integer> expectedOrder =
            List.of(
                    DateTimePatternGenerator.ERA,
                    DateTimePatternGenerator.YEAR,
                    DateTimePatternGenerator.QUARTER,
                    DateTimePatternGenerator.MONTH,
                    DateTimePatternGenerator.DAY,
                    DateTimePatternGenerator.WEEK_OF_YEAR,
                    DateTimePatternGenerator.WEEK_OF_MONTH,
                    DateTimePatternGenerator.WEEKDAY,
                    DateTimePatternGenerator.HOUR,
                    DateTimePatternGenerator.MINUTE,
                    DateTimePatternGenerator.SECOND,
                    DateTimePatternGenerator.DAYPERIOD,
                    DateTimePatternGenerator.ZONE);

    enum Check {
        order,
        dayperiod,
        uniqueness
    }

    private void check(String isoPat, Set<Check> checks) {
        VariableField last = null;
        int lastType = -1;
        Multimap<Integer, String> types = HashMultimap.create();

        // check the order. y M is ok, because type(y) < type(M)

        for (Object p : parser.set(isoPat).getItems()) {
            if (p instanceof VariableField) {
                VariableField pv = (VariableField) p;
                final int rawType = pv.getType();
                int curType = expectedOrder.indexOf(rawType);
                if (!assertTrue(pv + ": order > 0", curType >= 0)) {
                    int debug = 0;
                }
                if (checks.contains(Check.order) && lastType != -1) {
                    assertTrue(isoPat + ": " + last + " < " + pv, lastType < curType);
                }
                last = pv;
                lastType = curType;
                types.put(rawType, pv.toString());
            }
        }

        // There is only one field of each type

        if (checks.contains(Check.uniqueness)) {
            for (Entry<Integer, Collection<String>> entry : types.asMap().entrySet()) {
                assertEquals(entry.toString(), 1, entry.getValue().size());
            }
        }

        // There is an a/B iff it is 12 hour
        if (checks.contains(Check.dayperiod)) {
            boolean hasDayPeriod = types.containsKey(DateTimePatternGenerator.DAYPERIOD);
            Collection<String> hours = types.get(DateTimePatternGenerator.HOUR);
            char firstChar =
                    hours == null || hours.isEmpty() ? '\u0000' : hours.iterator().next().charAt(0);
            boolean is12hour = firstChar == 'h' || firstChar == 'k';
            if (!assertEquals(isoPat + " has 'a' iff 12 hour", hasDayPeriod, is12hour)) {
                int debug = 0;
            }
        }
    }

    public String formatInterval(
            ICUServiceBuilder isb, Date sample, Date sample2, String calendar, String pattern) {
        List<String> parts = splitIntervalPattern(pattern);
        SimpleDateFormat gregFormat1 = isb.getDateFormat(calendar, parts.get(0));
        gregFormat1.setTimeZone(TimeZone.GMT_ZONE);

        SimpleDateFormat gregFormat2 = isb.getDateFormat(calendar, parts.get(2));
        gregFormat2.setTimeZone(TimeZone.GMT_ZONE);

        return gregFormat1.format(sample) + parts.get(1) + gregFormat2.format(sample2);
    }

    DateTimePatternGenerator.FormatParser parser = new DateTimePatternGenerator.FormatParser();

    private List<String> splitIntervalPattern(String intervalPattern) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Set<Integer> soFar = new HashSet<>();

        // we have something of the form (literal? field)* sepLiteral (field literal?)*
        // that is, there are never 2 literals in a row.
        // a literal is a sepLiteral if the field after it is already present (or rather, if its
        // type is)
        String lastString = null;

        for (Object p : parser.set(intervalPattern).getItems()) {
            if (p instanceof String) {
                lastString = (String) p;
            } else if (p instanceof VariableField) {
                VariableField pv = (VariableField) p;
                if (soFar != null && soFar.contains(pv.getType())) {
                    // we hit the first repeated field
                    result.add(current.toString());
                    current.setLength(0);
                    result.add(
                            lastString == null
                                    ? ""
                                    : lastString); // it would be strange to have "", but...
                    lastString = null;
                    soFar = null;
                } else {
                    if (soFar != null) {
                        soFar.add(pv.getType());
                    }
                    if (lastString != null) {
                        current.append(quoteIfNeeded(lastString));
                        lastString = null;
                    }
                }
                current.append(p);
            } else {
                throw new IllegalArgumentException();
            }
        }
        if (lastString != null) {
            current.append(quoteIfNeeded(lastString));
        }
        result.add(current.toString());
        if (result.size() != 3) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    static final UnicodeSet VARIABLE = new UnicodeSet("[a-zA-Z']").freeze();

    private Object quoteIfNeeded(String lastString) {
        if (VARIABLE.containsSome(lastString)) {
            lastString = lastString.replace("'", "''");
            lastString = "'" + lastString + "'";
        }
        return lastString;
    }

    public void testIso8601() {

        // ldml/dates/calendars/calendar[@type="iso8601"]/dateFormats/dateFormatLength[@type="full"]/dateFormat[@type="standard"]/pattern[@type="standard"] y MMMM d, EEEE
        // ldml/dates/calendars/calendar[@type="iso8601"]/timeFormats/timeFormatLength[@type="full"]/timeFormat[@type="standard"]/pattern[@type="standard"] HH:mm:ss zzzz
        // ldml/dates/calendars/calendar[@type="iso8601"]/dateTimeFormats/dateTimeFormatLength[@type="full"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"] {1} {0}
        // ldml/dates/calendars/calendar[@type="iso8601"]/dateTimeFormats/availableFormats/dateFormatItem[@id="MMMMW"][@count="other"] MMMM 'week' W
        // ldml/dates/calendars/calendar[@type="iso8601"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="Bh"]/greatestDifference[@id="B"] h B – h B

        // This covers all the cases, with successful and failure cases.

        String[][] tests = {
            // only the first 6 elements of the path matter
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateFormats/dateFormatLength",
                "M yy",
                "Field yy is incorrect. For a YMD (Year-First) calendar, the year field cannot be truncated to 2 digits."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateFormats/dateFormatLength",
                "M y",
                "Field M cannot come before field y. A YMD (Year-First) calendar is special: bigger fields must come before smaller ones even when it feels unnatural in your language.  Change the text separating the fields as best you can."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateFormats/dateFormatLength",
                "y M",
                null
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/timeFormats/timeFormatLength",
                "M L",
                "Field L is the same type as a previous field."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/availableFormats",
                "d M y",
                "Field d cannot come before field M. A YMD (Year-First) calendar is special: bigger fields must come before smaller ones even when it feels unnatural in your language.  Change the text separating the fields as best you can."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/availableFormats",
                "y M d",
                null
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/intervalFormats",
                "y M d - d M",
                "Field d cannot come before field M in the 2nd part of the range. A YMD (Year-First) calendar is special: bigger fields must come before smaller ones even when it feels unnatural in your language.  Change the text separating the fields as best you can."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/intervalFormats",
                "y M d - M d",
                null
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/dateTimeFormatLength",
                "{0} {1}",
                "Put the {1} field (the date) before the {1} field (the time), in a YMD (Year-First) calendar."
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/dateTimeFormats/dateTimeFormatLength",
                "{1} {0}",
                null
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/appendItems/appendItem",
                "{1} {0}",
                null
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"iso8601\"]/appendItems/appendItem",
                "{0} {1}",
                null
            },
        };
        int count = 0;
        for (String[] test : tests) {
            ++count;
            String path = test[0];
            String value = test[1];
            String expected = test[2];
            String actual = CheckDates.checkIso8601(path, value);
            assertEquals(path + " " + value, expected, actual);
        }
    }

    /** Test to make sure that only canonical skeletons are used. */
    public void testSkeletons() {
        UnicodeSet allowed = new UnicodeSet("[BEGHMQUWdhmsvwy]").freeze();
        org.unicode.cldr.util.Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        Set<String> locales =
                cldrFactory
                        .getAvailable(); // Set.of("root", "en"); //  : cldrFactory.getAvailable();
        for (String locale : locales) {
            CLDRFile source = cldrFactory.make(locale, false);
            for (String path : source) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String skeleton = null;
                SkeletonPathType stype = RelatedDatePathValues.SkeletonPathType.fromParts(parts);
                switch (stype) {
                    case na:
                        continue;
                    case available:
                    case interval:
                        skeleton = parts.getAttributeValue(RelatedDatePathValues.idElement, "id");
                        break;
                    case datetime:
                        // Don't test this yet. The datetime skeletons are *input* to get a pattern,
                        // so they can have any valid skeleton character.
                        //                        skeleton = source.getStringValue(path);
                        //                        if (skeleton == null || "↑↑↑".equals(skeleton)) {
                        //                            continue;
                        //                        }
                        continue;
                }
                if (!allowed.containsAll(skeleton)) {
                    warnln(
                            "Unexpected skeleton character in "
                                    + locale
                                    + ", "
                                    + path
                                    + ", "
                                    + skeleton
                                    + ": "
                                    + new UnicodeSet().addAll(skeleton).removeAll(allowed));
                }
            }
        }
    }
}
