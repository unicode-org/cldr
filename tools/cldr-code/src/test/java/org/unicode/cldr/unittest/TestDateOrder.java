package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.test.DateOrder;
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
    }

    static final String stockPathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength";
    static final String availableFormatPathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem";
    static final String intervalFormatPathPrefix =
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/";

    public void TestIso8601() {
        ICUServiceBuilder isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance("en"));
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        Factory phf = PathHeader.getFactory();
        Set<PathHeader> paths = new TreeSet<>();
        for (String path : english) {
            if (!path.startsWith("//ldml/dates/calendars/calendar[@type=\"gregorian\"]")) {
                continue;
            } else if (path.startsWith(stockPathPrefix)) {
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
        Date sample = new Date(2024 - 1900, 0, 9, 19, 8, 9);
        System.out.println();
        for (PathHeader pathHeader : paths) {
            final String originalPath = pathHeader.getOriginalPath();
            String gregPat = english.getStringValue(originalPath);
            String isoPat =
                    english.getStringValue(originalPath.replace("\"gregorian\"", "\"iso8601\""));
            String gregFormatted = null;
            String isoFormatted = null;

            if (originalPath.contains("intervalFormats")) {
                Date sample1 = (Date) sample.clone();
                Date sample2 = (Date) sample.clone();
                XPathParts parts = XPathParts.getFrozenInstance(originalPath);
                String greatestDifference = parts.getAttributeValue(-1, "id");

                switch (greatestDifference) {
                    case "G":
                        sample1.setYear(-sample2.getYear());
                        break;
                    case "y":
                        sample2.setYear(sample2.getYear() + 1);
                        break;
                    case "M":
                        sample2.setMonth(sample2.getMonth() + 1);
                        break;
                    case "d":
                        sample2.setDate(sample2.getDate() + 1);
                        break;
                    case "h":
                    case "H":
                        sample2.setHours(sample2.getHours() + 1);
                        break;
                    case "a":
                    case "B":
                        sample2.setHours(sample2.getHours() + 12);
                        break;
                    case "m":
                        sample2.setMinutes(sample2.getMinutes() + 1);
                        break;
                    case "s":
                        sample2.setSeconds(sample2.getSeconds() + 1);
                        break;
                    default:
                        System.out.println("Missing" + greatestDifference);
                        break;
                }

                gregFormatted = formatInterval(isb, sample1, sample2, gregPat);
                isoFormatted = formatInterval(isb, sample1, sample2, isoPat);
            } else {
                SimpleDateFormat gregFormat = isb.getDateFormat("gregorian", gregPat);
                SimpleDateFormat isoFormat = isb.getDateFormat("iso8601", isoPat);

                gregFormatted = gregFormat.format(sample);
                isoFormatted = isoFormat.format(sample);
            }
            System.out.println(
                    JOIN_TAB.join(
                            pathHeader.getCode(), gregPat, gregFormatted, isoPat, isoFormatted));
        }
    }

    public String formatInterval(ICUServiceBuilder isb, Date sample, Date sample2, String gregPat) {
        List<String> parts = splitIntervalPattern(gregPat);
        SimpleDateFormat gregFormat1 = isb.getDateFormat("gregorian", parts.get(0));
        SimpleDateFormat gregFormat2 = isb.getDateFormat("gregorian", parts.get(2));
        return gregFormat1.format(sample) + parts.get(1) + gregFormat2.format(sample2);
    }

    private List<String> splitIntervalPattern(String intervalPattern) {
        DateTimePatternGenerator.FormatParser parser = new DateTimePatternGenerator.FormatParser();
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
}
