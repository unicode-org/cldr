package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.unicode.cldr.test.DateOrder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateTimePatternGenerator;

public class TestDateOrder extends TestFmwk {
    public static void main(String[] args) {
        new TestDateOrder().run(args);
    }

    public void TestDateImportance() {
        XMLSource source = new SimpleXMLSource("xx");
        // add xpaths
        String fullDate = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat/pattern";
        source.putValueAtPath(fullDate, "EEEE, dd MMMM, y");
        String longDate = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"long\"]/dateFormat/pattern";
        source.putValueAtPath(longDate, "dd MMMM y");
        String mediumDate = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat/pattern";
        source.putValueAtPath(mediumDate, "dd-MMM-y");
        String shortDate = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat/pattern";
        source.putValueAtPath(shortDate, "dd/MM/yy");
        String availableFormat = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"yMd\"]";
        source.putValueAtPath(availableFormat, "M/d/y");
        String intervalFormat = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMd\"]/greatestDifference[@id=\"y\"]";
        source.putValueAtPath(intervalFormat, "d/M/y – d/M/y");
        CLDRFile cldrFile = new CLDRFile(source);
        DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
        Map<String, Map<DateOrder, String>> order = DateOrder.getOrderingInfo(
            cldrFile, cldrFile, fp);
        assertNull("There should be no conflicts", order.get(fullDate));
        Collection<String> values = order.get(availableFormat).values();
        assertEquals("There should only one conflict", 1, values.size());

        values = order.get(intervalFormat).values();
        assertTrue(
            "There should be a conflict between the interval format and available format",
            values.contains(availableFormat));

        source.putValueAtPath(fullDate, "EEEE, y MMMM dd");
        order = DateOrder.getOrderingInfo(cldrFile, cldrFile, fp);
        values = new HashSet<String>(order.get(fullDate).values()); // filter
        // duplicates
        assertEquals("There should be a conflict with other date values", 1,
            values.size());
        assertTrue("No conflict with long date", values.contains(longDate));

        values = order.get(availableFormat).values();
        assertEquals(
            "There should be conflicts with this available format and date formats",
            2, values.size());
        assertTrue("No conflict with full date", values.contains(fullDate));
        assertTrue("No conflict with short date", values.contains(shortDate));

        values = order.get(intervalFormat).values();
        assertTrue("Available format conflict not found",
            values.contains(availableFormat));
        assertTrue("Date format conflict not found", values.contains(fullDate));
    }
}
