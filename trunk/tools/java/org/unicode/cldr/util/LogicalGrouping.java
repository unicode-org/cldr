package org.unicode.cldr.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.PluralRulesUtil.KeywordStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.PluralRules;

public class LogicalGrouping {

    public static final ImmutableSet<String> metazonesDSTSet = ImmutableSet.of(
        "Acre", "Africa_Western", "Alaska", "Almaty", "Amazon",
        "America_Central", "America_Eastern", "America_Mountain", "America_Pacific", "Anadyr", "Apia",
        "Aqtau", "Aqtobe", "Arabian", "Argentina", "Argentina_Western", "Armenia",
        "Atlantic", "Australia_Central", "Australia_CentralWestern", "Australia_Eastern", "Australia_Western",
        "Azerbaijan", "Azores", "Bangladesh", "Brasilia", "Cape_Verde",
        "Chatham", "Chile", "China", "Choibalsan", "Colombia", "Cook", "Cuba", "Easter",
        "Europe_Central", "Europe_Eastern", "Europe_Western", "Falkland", "Fiji", "Georgia",
        "Greenland_Eastern", "Greenland_Western", "Hawaii_Aleutian", "Hong_Kong", "Hovd",
        "Iran", "Irkutsk", "Israel", "Japan", "Kamchatka", "Korea", "Krasnoyarsk",
        "Lord_Howe", "Macau", "Magadan", "Mauritius", "Mexico_Northwest", "Mexico_Pacific", "Mongolia", "Moscow", "New_Caledonia",
        "New_Zealand", "Newfoundland", "Noronha", "Novosibirsk", "Omsk", "Pakistan", "Paraguay", "Peru", "Philippines",
        "Pierre_Miquelon", "Qyzylorda", "Sakhalin", "Samara", "Samoa",
        "Taipei", "Tonga", "Turkmenistan", "Uruguay", "Uzbekistan",
        "Vanuatu", "Vladivostok", "Volgograd", "Yakutsk", "Yekaterinburg");

    public static final ImmutableList<String> days = ImmutableList.of("sun", "mon", "tue", "wed", "thu", "fri", "sat");

    public static final ImmutableSet<String> calendarsWith13Months = ImmutableSet.of("coptic", "ethiopic", "hebrew");
    public static final ImmutableSet<String> compactDecimalFormatLengths = ImmutableSet.of("short", "long");
    private static final ImmutableSet<String> ampm = ImmutableSet.of("am", "pm");
    private static final ImmutableSet<String> nowUnits = ImmutableSet.of("second", "second-short", "second-narrow",
        "minute", "minute-short", "minute-narrow", "hour", "hour-short", "hour-narrow");

    /**
     * Return the set of paths that are in the same logical set as the given path
     *
     * @param path
     *            - the distinguishing xpath
     */
    public static Set<String> getPaths(CLDRFile cldrFile, String path) {
        ImmutableSet<String> metazone_string_types = ImmutableSet.of("generic", "standard", "daylight");

        Set<String> result = new TreeSet<String>();
        if (path == null) return result;
        result.add(path);
        // Figure out the plurals forms, as we will probably need them.

        XPathParts parts = new XPathParts();
        parts.set(path);

        if (path.indexOf("/metazone") > 0) {
            String metazoneName = parts.getAttributeValue(3, "type");
            if (metazonesDSTSet.contains(metazoneName)) {
                for (String str : metazone_string_types) {
                    result.add(path.substring(0, path.lastIndexOf('/') + 1) + str);
                }
            }
        } else if (path.indexOf("/days") > 0) {
            String dayName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            if (dayName != null && days.contains(dayName)) { // This is just a quick check to make sure the path is
                                                             // good.
                for (String str : days) {
                    parts.setAttribute("day", "type", str);
                    result.add(parts.toString());
                }
            }
        } else if (path.indexOf("/dayPeriods") > 0) {
            if (path.endsWith("alias")) {
                result.add(path);
            } else {
                String dayPeriodType = parts.findAttributeValue("dayPeriod", "type");

                if (ampm.contains(dayPeriodType)) {
                    for (String s : ampm) {
                        parts.setAttribute("dayPeriod", "type", s);
                        result.add(parts.toString());
                    }
                } else {
                    SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(
                        cldrFile.getSupplementalDirectory());
                    DayPeriodInfo.Type dayPeriodContext = DayPeriodInfo.Type.fromString(parts.findAttributeValue("dayPeriodContext", "type"));
                    DayPeriodInfo dpi = supplementalData.getDayPeriods(dayPeriodContext, cldrFile.getLocaleID());
                    List<DayPeriod> dayPeriods = dpi.getPeriods();
                    DayPeriod thisDayPeriod = DayPeriod.fromString(dayPeriodType);
                    if (dayPeriods.contains(thisDayPeriod)) {
                        for (DayPeriod d : dayPeriods) {
                            parts.setAttribute("dayPeriod", "type", d.name());
                            result.add(parts.toString());
                        }
                    }
                }
            }
        } else if (path.indexOf("/quarters") > 0) {
            String quarterName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            Integer quarter = quarterName == null ? 0 : Integer.valueOf(quarterName);
            if (quarter > 0 && quarter <= 4) { // This is just a quick check to make sure the path is good.
                for (Integer i = 1; i <= 4; i++) {
                    parts.setAttribute("quarter", "type", i.toString());
                    result.add(parts.toString());
                }
            }
        } else if (path.indexOf("/months") > 0) {
            String calType = parts.size() > 3 ? parts.getAttributeValue(3, "type") : null;
            String monthName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            Integer month = monthName == null ? 0 : Integer.valueOf(monthName);
            int calendarMonthMax = calendarsWith13Months.contains(calType) ? 13 : 12;
            if (month > 0 && month <= calendarMonthMax) { // This is just a quick check to make sure the path is good.
                for (Integer i = 1; i <= calendarMonthMax; i++) {
                    parts.setAttribute("month", "type", i.toString());
                    if ("hebrew".equals(calType)) {
                        parts.removeAttribute("month", "yeartype");
                    }
                    result.add(parts.toString());
                }
                if ("hebrew".equals(calType)) { // Add extra hebrew calendar leap month
                    parts.setAttribute("month", "type", Integer.toString(7));
                    parts.setAttribute("month", "yeartype", "leap");
                    result.add(parts.toString());
                }
            }
        } else if (parts.containsElement("relative")) {
            String fieldType = parts.findAttributeValue("field", "type");
            String relativeType = parts.findAttributeValue("relative", "type");
            Integer relativeValue = relativeType == null ? 999 : Integer.valueOf(relativeType);
            if (relativeValue >= -3 && relativeValue <= 3) { // This is just a quick check to make sure the path is good.
                if (!(nowUnits.contains(fieldType) && relativeValue == 0)) { // Workaround for "now", "this hour", "this minute"
                    int limit = 1;
                    if (fieldType != null && fieldType.startsWith("day")) {
                        limit = 3;
                    }
                    for (Integer i = -1 * limit; i <= limit; i++) {
                        parts.setAttribute("relative", "type", i.toString());
                        result.add(parts.toString());
                    }
                }
            }
        } else if (path.indexOf("/decimalFormatLength") > 0) {
            PluralInfo pluralInfo = getPluralInfo(cldrFile);
            Set<Count> pluralTypes = pluralInfo.getCounts();
            String decimalFormatLengthType = parts.size() > 3 ? parts.getAttributeValue(3, "type") : null;
            String decimalFormatPatternType = parts.size() > 5 ? parts.getAttributeValue(5, "type") : null;
            if (decimalFormatLengthType != null && decimalFormatPatternType != null &&
                compactDecimalFormatLengths.contains(decimalFormatLengthType)) {
                int numZeroes = decimalFormatPatternType.length() - 1;
                int baseZeroes = (numZeroes / 3) * 3;
                for (int i = 0; i < 3; i++) {
                    String patType = "1" + String.format(String.format("%%0%dd", baseZeroes + i), 0); // This gives us "baseZeroes+i" zeroes at the end.
                    parts.setAttribute(5, "type", patType);
                    for (Count count : pluralTypes) {
                        parts.setAttribute(5, "count", count.toString());
                        result.add(parts.toString());
                    }
                }
            }
        } else if (path.indexOf("[@count=") > 0) {
            PluralInfo pluralInfo = getPluralInfo(cldrFile);
            Set<Count> pluralTypes = pluralInfo.getCounts();
            String lastElement = parts.getElement(-1);
            for (Count count : pluralTypes) {
                parts.setAttribute(lastElement, "count", count.toString());
                result.add(parts.toString());
            }
        }
        return result;
    }

    /**
     * Returns the plural info for a given locale.
     */
    private static PluralInfo getPluralInfo(CLDRFile cldrFile) {
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(
            cldrFile.getSupplementalDirectory());
        return supplementalData.getPlurals(PluralType.cardinal,
            cldrFile.getLocaleID());
    }

    /**
     * @param cldrFile
     * @param path
     * @return true if the specified path is optional in the logical grouping
     *         that it belongs to.
     */
    public static boolean isOptional(CLDRFile cldrFile, String path) {
        XPathParts parts = new XPathParts().set(path);

        if (parts.containsElement("relative")) {
            String fieldType = parts.findAttributeValue("field", "type");
            String relativeType = parts.findAttributeValue("relative", "type");
            Integer relativeValue = relativeType == null ? 999 : Integer.valueOf(relativeType);
            if (fieldType != null && fieldType.startsWith("day") && Math.abs(relativeValue.intValue()) >= 2) {
                return true; // relative days +2 +3 -2 -3 are optional in a logical group.
            }
        }
        // Paths with count="(zero|one)" are optional if their usage is covered
        // fully by paths with count="(0|1)", which are always optional themselves.
        if (!path.contains("[@count=")) return false;
        String pluralType = parts.getAttributeValue(-1, "count");
        if (pluralType.equals("0") || pluralType.equals("1")) return true;
        if (!pluralType.equals("zero") && !pluralType.equals("one")) return false;

        PluralRules pluralRules = getPluralInfo(cldrFile).getPluralRules();
        parts.setAttribute(-1, "count", "0");
        Set<Double> explicits = new HashSet<Double>();
        if (cldrFile.isHere(parts.toString())) {
            explicits.add(0.0);
        }
        parts.setAttribute(-1, "count", "1");
        if (cldrFile.isHere(parts.toString())) {
            explicits.add(1.0);
        }
        if (!explicits.isEmpty()) {
            // HACK: The com.ibm.icu.text prefix is needed so that ST can find it
            // (no idea why).
            KeywordStatus status = org.unicode.cldr.util.PluralRulesUtil.getKeywordStatus(
                pluralRules, pluralType, 0, explicits, true);
            if (status == KeywordStatus.SUPPRESSED) {
                return true;
            }
        }
        return false;
    }
}
