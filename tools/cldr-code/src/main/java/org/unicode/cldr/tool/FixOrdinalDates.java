package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.List;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

public class FixOrdinalDates {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();

    public static void main(String[] args) {
        // ldml/dates/calendars/calendar[@type="generic"]/dayOfMonths/dayOfMonthContext[@type="format"]/dayOfMonthWidth[@type="abbreviated"]/dayOfMonth[@ordinal="other"]
        // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats/dateFormatItem[@id="MMMMEddd"]

        Set<String> available =
                StandardCodes.make()
                        .getLocaleCoverageLocales(
                                Organization
                                        .cldr); // Set.of("en"); //  CLDR_FACTORY.getAvailable();
        for (String locale : available) {
            CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
            CLDRFile unresolved = cldrFile.getUnresolved();
            Multimap<String, String> attrToOrdinal = TreeMultimap.create();
            Multimap<String, String> attrToDdd = TreeMultimap.create();
            // collect data
            for (String path : unresolved) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String lastElement = parts.getElement(-1);
                String attributeValue = null;
                if (lastElement.equals("dayOfMonth")) {
                    attributeValue = parts.getAttributeValue(-1, "ordinal");
                    stash(cldrFile, parts, attrToOrdinal);
                } else if (lastElement.equals("dateFormatItem")) {
                    attributeValue = parts.getAttributeValue(-1, "id");
                    if (!attributeValue.contains("ddd")
                            || !cldrFile.getStringValue(path).contains("ddd")) {
                        continue;
                    }
                    stash(cldrFile, parts, attrToDdd);
                } else {
                    continue;
                }
            }
            // list for now
            System.out.println();
            show(locale, attrToOrdinal);
            show(locale, attrToDdd);
        }
    }

    private static void show(String locale, Multimap<String, String> attrToDdd) {
        attrToDdd.asMap().entrySet().stream()
                .forEach(x -> System.out.println(locale + "\t" + x.getKey() + "\t" + x.getValue()));
    }

    private static void stash(
            CLDRFile cldrFile, XPathParts parts, Multimap<String, String> attrToOrdinal) {
        String value = cldrFile.getStringValue(parts.toString());
        SplitPath split = SplitPath.from(parts.toString());
        List<String> attributeValues = split.getAttributeValues();
        String higherAttributes =
                Joiner.on("⏀").join(attributeValues.subList(0, attributeValues.size() - 1));
        attrToOrdinal.put(higherAttributes, value);
    }
}
