package org.unicode.cldr.unittest;

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
        String availableFormat =  "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"Md\"]";
        source.putValueAtPath(availableFormat, "M/d/y");
        CLDRFile cldrFile = new CLDRFile(source);
        DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
        Map<String, Map<DateOrder, String>> order = DateOrder.getOrderingInfo(cldrFile, cldrFile, fp);
        assertNull("There should be no conflicts", order.get(fullDate));
        assertEquals("There should be conflicts for available formats ", 1, order.get(availableFormat).size());

        source.putValueAtPath(fullDate, "EEEE, y MMMM dd");
        order = DateOrder.getOrderingInfo(cldrFile, cldrFile, fp);
        assertEquals("There should be conflicts with this date format", 3, order.get(fullDate).values().size());
        assertEquals("There should be conflicts with this available format", 2, order.get(availableFormat).values().size());
    }
}
