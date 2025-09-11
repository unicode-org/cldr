package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.ibm.icu.impl.CalType;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.Factory;

@CLDRTool(alias = "unexpected", description = "Show unexpected date formats")
public class ShowUnexpectedDateFormats {
    private static final String[] formats = {
        "MMMEEEEd",
        "MMMMEEEEd",
        "yMMMEEEEd",
        "yMMMMEEEEd",
        "GyMMMEEEEd/d",
        "GyMMMEEEEd/G",
        "GyMMMEEEEd/M",
        "GyMMMEEEEd/y"
    };

    public static void main(String[] args) throws IOException {
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> locales = new TreeSet<>(cldrFactory.getAvailable());
        System.out.println("Checking " + locales.size() + " locales");
        Map<String, Integer> count = new TreeMap<>();
        Set<String> localesWithUnexpectedPaths = new TreeSet<>();
        for (String loc : locales) {
            CLDRFile cldrFile = cldrFactory.make(loc, false);
            for (String format : formats) {
                for (CalType calType : CalType.values()) {
                    String path = makePathFromFormat(calType, format);
                    String value = cldrFile.getStringValue(path);
                    if (value != null) {
                        System.out.println(loc + "\t" + path + "\t" + value);
                        localesWithUnexpectedPaths.add(loc);
                        Integer c = count.get(format);
                        if (c == null) {
                            c = 0;
                        }
                        count.put(format, c + 1);
                    }
                }
            }
        }
        for (String format : formats) {
            Integer c = count.get(format);
            if (c == null) {
                c = 0;
            }
            System.out.println(format + "\t" + c);
        }
        System.out.println(
                localesWithUnexpectedPaths.size()
                        + " locales with unexpected paths: "
                        + Joiner.on(" ").join(localesWithUnexpectedPaths));
    }

    private static String makePathFromFormat(CalType calType, String format) {
        if (format.contains("/")) {
            String format1 = format.substring(0, format.length() - 2);
            String format2 = format.substring(format.length() - 1);
            // Note: this path differs from the one below not only in the addition of
            // "greatestDifference", but also in having "intervalFormats/intervalFormatItem" instead
            // of "availableFormats/dateFormatItem"
            return "//ldml/dates/calendars/calendar[@type=\""
                    + calType.getId()
                    + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\""
                    + format1
                    + "\"]/greatestDifference[@id=\""
                    + format2
                    + "\"]";
        } else {
            return "//ldml/dates/calendars/calendar[@type=\""
                    + calType.getId()
                    + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                    + format
                    + "\"]";
        }
    }
}
