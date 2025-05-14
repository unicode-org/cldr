package org.unicode.cldr.test;

import java.util.LinkedHashSet;
import java.util.Set;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

public class RelatedPathValues {
    public static final int calendarElement = 3;
    public static final int dateTypeElement = 5;
    public static final int idElement = 6;

    static final XPathParts interval =
            XPathParts.getFrozenInstance(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"MMMd\"]");

    // samples
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="Bhm"]/greatestDifference[@id="B"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats/dateFormatItem[@id="MMMd"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="full"]/dateFormat[@type="standard"]/pattern[@type="standard"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats/timeFormatLength[@type="full"]/timeFormat[@type="standard"]/pattern[@type="standard"]
    public static Set<String> getRelatedPathValues(CLDRFile cldrFile, XPathParts xparts) {
        if (xparts.size() <= idElement) {
            return Set.of();
        }
        switch (xparts.getElement(dateTypeElement)) {
            case "availableFormats":
                return forAvailable(cldrFile, xparts);
            case "intervalFormats":
                return forInterval(cldrFile, xparts);
            default:
                break;
        }
        return Set.of();
    }

    private static Set<String> forAvailable(CLDRFile cldrFile, XPathParts xparts) {
        Set<String> skeletons = new LinkedHashSet<>();
        String skeleton = xparts.getAttributeValue(idElement, "id");
        addRelated(skeleton, "G", skeletons);
        addRelated(skeleton, "E", skeletons);
        addRelated(skeleton, "v", skeletons);
        if (skeletons.isEmpty()) {
            return Set.of();
        }
        XPathParts newPath = xparts.cloneAsThawed();
        Set<String> result = new LinkedHashSet<>();
        for (String item : skeletons) {
            newPath.putAttributeValue(idElement, "id", item);
            String value = cldrFile.getStringValue(newPath.toString());
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private static String addRelated(String skeleton, String letter, Set<String> skeletons) {
        if (skeleton.contains(letter)) {
            String newItem = skeleton.replace(letter, "");
            if (newItem.length() > 1 && !newItem.equals(skeleton)) {
                skeletons.add(newItem);
            }
        }
        return null;
    }

    private static Set<String> forInterval(CLDRFile cldrFile, XPathParts xparts) {
        XPathParts newPath = interval.cloneAsThawed();
        newPath.putAttributeValue(
                calendarElement, "type", xparts.getAttributeValue(calendarElement, "type"));
        newPath.putAttributeValue(idElement, "id", xparts.getAttributeValue(idElement, "id"));
        String value = cldrFile.getStringValue(newPath.toString());
        if (value != null) {
            return Set.of(value);
        }
        return Set.of();
    }
}
