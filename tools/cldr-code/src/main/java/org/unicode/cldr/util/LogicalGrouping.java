package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PluralRulesUtil.KeywordStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Output;

public class LogicalGrouping {

    static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();

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
        "New_Zealand", "Newfoundland", "Norfolk", "Noronha", "Novosibirsk", "Omsk", "Pakistan", "Paraguay", "Peru", "Philippines",
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
     * Cache from path (String) to logical group (Set<String>)
     */
    private static Multimap<String, String> cachePathToLogicalGroup = ArrayListMultimap.create();

    /**
     * Cache from locale and path (<Pair<String, String>), to logical group (Set<String>)
     */
    private static ConcurrentHashMap<Pair<String, String>, Set<String>> cacheLocaleAndPathToLogicalGroup = new ConcurrentHashMap<>();

    /**
     * Statistics on occurrences of types of logical groups, for performance testing, debugging.
     * GET_TYPE_COUNTS should be false for production to maximize performance.
     */
    public static final boolean GET_TYPE_COUNTS = false;
    public static final ConcurrentHashMap<String, Long> typeCount = GET_TYPE_COUNTS ? new ConcurrentHashMap<>() : null;

    /**
     * GET_TYPE_FROM_PARTS is more elegant when true, but performance is a little faster when it's false.
     * This might change if XPathParts.getInstance and/or XPathParts.set are made faster.
     */
    private static final boolean GET_TYPE_FROM_PARTS = false;

    /**
     * Return a sorted set of paths that are in the same logical set as the given path
     * @param path the distinguishing xpath
     * @param pathType TODO
     *
     * @return the set of paths, or null (to be treated as equivalent to empty set)
     *
     * For example, given the path
     *
     * //ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="format"]/quarterWidth[@type="abbreviated"]/quarter[@type="1"]
     *
     * return the set of four paths
     *
     * //ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="format"]/quarterWidth[@type="abbreviated"]/quarter[@type="1"]
     * //ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="format"]/quarterWidth[@type="abbreviated"]/quarter[@type="2"]
     * //ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="format"]/quarterWidth[@type="abbreviated"]/quarter[@type="3"]
     * //ldml/dates/calendars/calendar[@type="gregorian"]/quarters/quarterContext[@type="format"]/quarterWidth[@type="abbreviated"]/quarter[@type="4"]
     *
     * Caches: Most of the calculations are independent of the locale, and can be cached on a static basis.
     * The paths that are locale-dependent are /dayPeriods and @count. Those can be computed on a per-locale basis;
     * and cached (they are shared across a number of locales).
     *
     * Reference: https://unicode.org/cldr/trac/ticket/11854
     */
    public static Set<String> getPaths(CLDRFile cldrFile, String path, Output<PathType> pathTypeOut) {
        if (path == null) {
            return null; // return null for null path
            // return new TreeSet<String>(); // return empty set for null path
        }
        XPathParts parts = null;
        PathType pathType = null;
        if (GET_TYPE_FROM_PARTS) {
            parts = XPathParts.getFrozenInstance(path);
            pathType = PathType.getPathTypeFromParts(parts);
        } else {
            /*
             * XPathParts.set is expensive, so avoid it (not needed for singletons) if !GET_TYPE_FROM_PARTS
             */
            pathType = PathType.getPathTypeFromPath(path);
        }
        if (pathTypeOut != null) {
            pathTypeOut.value = pathType;
        }

        if (GET_TYPE_COUNTS) {
            typeCount.compute(pathType.toString(), (k, v) -> (v == null) ? 1 : v + 1);
        }

        if (pathType == PathType.SINGLETON) {
            /*
             * Skip cache for PathType.SINGLETON and simply return a set of one.
             * TODO: should we ever return null instead of singleton here?
             */
            Set<String> set = new TreeSet<>();
            set.add(path);
            return set;
        }

        if (!GET_TYPE_FROM_PARTS) {
            parts = XPathParts.getFrozenInstance(path).cloneAsThawed();
        } else {
            parts = parts.cloneAsThawed();
        }

        if (PathType.isLocaleDependent(pathType)) {
            String locale = cldrFile.getLocaleID();
            Pair<String, String> key = new Pair<>(locale, path);
            if (cacheLocaleAndPathToLogicalGroup.containsKey(key)) {
                return new TreeSet<>(cacheLocaleAndPathToLogicalGroup.get(key));
            }
            Set<String> set = new TreeSet<>();
            pathType.addPaths(set, cldrFile, path, parts);
            cacheLocaleAndPathToLogicalGroup.put(key, set);
            return set;
        } else {
            /*
             * All other paths are locale-independent.
             */
            if (cachePathToLogicalGroup.containsKey(path)) {
                return new TreeSet<>(cachePathToLogicalGroup.get(path));
            }
            Set<String> set = new TreeSet<>();
            pathType.addPaths(set, cldrFile, path, parts);
            cachePathToLogicalGroup.putAll(path, set);
            return set;
        }
    }

    public static Set<String> getPaths(CLDRFile cldrFile, String path) {
        return getPaths(cldrFile, path, null);
    }

    /**
     * Returns the plural info for a given locale.
     */
    private static PluralInfo getPluralInfo(CLDRFile cldrFile) {
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
        XPathParts parts = XPathParts.getFrozenInstance(path);

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
        switch (pluralType) {
        case "0": case "1":
            return true;
        case "zero": case "one":
            break; // continue
//        case "many": // special case for french
//            String localeId = cldrFile.getLocaleID();
//            if (localeId.startsWith("fr")
//                && (localeId.length() == 2 || localeId.charAt(2) == '_')) {
//                return true;
//            }
//            return false;
        default:
            return false;
        }

        parts = parts.cloneAsThawed();
        PluralRules pluralRules = getPluralInfo(cldrFile).getPluralRules();
        parts.setAttribute(-1, "count", "0");
        Set<Double> explicits = new HashSet<>();
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

    /**
     * Path types for logical groupings
     */
    public enum PathType {
        SINGLETON { // no logical groups for singleton paths
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                // Do nothing. This function won't be called.
            }
        },
        METAZONE {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                String metazoneName = parts.getAttributeValue(3, "type");
                if (metazonesDSTSet.contains(metazoneName)) {
                    for (String str : ImmutableSet.of("generic", "standard", "daylight")) {
                        set.add(path.substring(0, path.lastIndexOf('/') + 1) + str);
                    }
                }
            }
        },
        DAYS {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                String dayName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
                // This is just a quick check to make sure the path is good.
                if (dayName != null && days.contains(dayName)) {
                    for (String str : days) {
                        parts.setAttribute("day", "type", str);
                        set.add(parts.toString());
                    }
                }
            }
        },
        DAY_PERIODS {
            @Override
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                if (parts.containsElement("alias")) {
                    set.add(path);
                } else {
                    String dayPeriodType = parts.findAttributeValue("dayPeriod", "type");
                    if (ampm.contains(dayPeriodType)) {
                        for (String s : ampm) {
                            parts.setAttribute("dayPeriod", "type", s);
                            set.add(parts.toString());
                        }
                    } else {
                        DayPeriodInfo.Type dayPeriodContext = DayPeriodInfo.Type.fromString(parts.findAttributeValue("dayPeriodContext", "type"));
                        DayPeriodInfo dpi = supplementalData.getDayPeriods(dayPeriodContext, cldrFile.getLocaleID());
                        List<DayPeriod> dayPeriods = dpi.getPeriods();
                        DayPeriod thisDayPeriod = DayPeriod.fromString(dayPeriodType);
                        if (dayPeriods.contains(thisDayPeriod)) {
                            for (DayPeriod d : dayPeriods) {
                                parts.setAttribute("dayPeriod", "type", d.name());
                                set.add(parts.toString());
                            }
                        }
                    }
                }
            }
        },
        QUARTERS {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                String quarterName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
                Integer quarter = quarterName == null ? 0 : Integer.valueOf(quarterName);
                if (quarter > 0 && quarter <= 4) { // This is just a quick check to make sure the path is good.
                    for (Integer i = 1; i <= 4; i++) {
                        parts.setAttribute("quarter", "type", i.toString());
                        set.add(parts.toString());
                    }
                }
            }
        },
        MONTHS {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
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
                        set.add(parts.toString());
                    }
                    if ("hebrew".equals(calType)) { // Add extra hebrew calendar leap month
                        parts.setAttribute("month", "type", Integer.toString(7));
                        parts.setAttribute("month", "yeartype", "leap");
                        set.add(parts.toString());
                    }
                }
            }
        },
        RELATIVE {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
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
                            set.add(parts.toString());
                        }
                    }
                }
            }
        },
        DECIMAL_FORMAT_LENGTH {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                PluralInfo pluralInfo = getPluralInfo(cldrFile);
                Set<Count> pluralTypes = pluralInfo.getCounts();
                String decimalFormatLengthType = parts.size() > 3 ? parts.getAttributeValue(3, "type") : null;
                String decimalFormatPatternType = parts.size() > 5 ? parts.getAttributeValue(5, "type") : null;
                if (decimalFormatLengthType != null && decimalFormatPatternType != null &&
                    compactDecimalFormatLengths.contains(decimalFormatLengthType)) {
                    int numZeroes = decimalFormatPatternType.length() - 1;
                    int baseZeroes = (numZeroes / 3) * 3;
                    for (int i = 0; i < 3; i++) {
                        // This gives us "baseZeroes+i" zeroes at the end.
                        String patType = "1" + String.format(String.format("%%0%dd", baseZeroes + i), 0);
                        parts.setAttribute(5, "type", patType);
                        for (Count count : pluralTypes) {
                            parts.setAttribute(5, "count", count.toString());
                            set.add(parts.toString());
                        }
                    }
                }
            }
        },
        COUNT {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                addCaseOnly(set, cldrFile, parts);
            }
        },
        COUNT_CASE {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                if (!GrammarInfo.getGrammarLocales().contains(cldrFile.getLocaleID())) {
                    addCaseOnly(set, cldrFile, parts);
                    return;
                }
                GrammarInfo grammarInfo = supplementalData.getGrammarInfo(cldrFile.getLocaleID());
                if (grammarInfo == null
                    || (parts.getElement(3).equals("unitLength")
                        && GrammarInfo.getUnitsToAddGrammar().contains(parts.getAttributeValue(3, "type")))) {
                    addCaseOnly(set, cldrFile, parts);
                    return;
                }
                Set<Count> pluralTypes = getPluralInfo(cldrFile).getCounts();
                Collection<String> rawCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
                setGrammarAttributes(set, parts, pluralTypes, rawCases, null);

            }
        },
        COUNT_CASE_GENDER {
            @Override
            @SuppressWarnings("unused")
            void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts) {
                if (!GrammarInfo.getGrammarLocales().contains(cldrFile.getLocaleID())) {
                    addCaseOnly(set, cldrFile, parts);
                    return;
                }
                GrammarInfo grammarInfo = supplementalData.getGrammarInfo(cldrFile.getLocaleID());
                if (grammarInfo == null) {
                    addCaseOnly(set, cldrFile, parts);
                    return;
                }
                Set<Count> pluralTypes = getPluralInfo(cldrFile).getCounts();
                Collection<String> rawCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
                Collection<String> rawGenders = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units);
                setGrammarAttributes(set, parts, pluralTypes, rawCases, rawGenders);
            }
        }

        ;

        abstract void addPaths(Set<String> set, CLDRFile cldrFile, String path, XPathParts parts);

        public void addCaseOnly(Set<String> set, CLDRFile cldrFile, XPathParts parts) {
            Set<Count> pluralTypes = getPluralInfo(cldrFile).getCounts();
            for (Count count : pluralTypes) {
                parts.setAttribute(-1, "count", count.toString());
                set.add(parts.toString());
            }
        }

        public void setGrammarAttributes(Set<String> set, XPathParts parts, Set<Count> pluralTypes, Collection<String> rawCases, Collection<String> rawGenders) {
            final String defaultGender = GrammaticalFeature.grammaticalGender.getDefault(rawGenders);
            final String defaultCase = GrammaticalFeature.grammaticalCase.getDefault(rawCases);

            if (rawCases == null || rawCases.isEmpty()) {
                rawCases = Collections.singleton(defaultCase);
            }
            if (rawGenders == null || rawGenders.isEmpty()) {
                rawGenders = Collections.singleton(defaultGender);
            }
            for (String gender : rawGenders) {
                if (gender.equals(defaultGender)) {
                    gender = null;
                }
                for (String case1 : rawCases) {
                    if (case1.equals(defaultCase)) {
                        case1 = null;
                    }
                    for (Count count : pluralTypes) {
                        parts.setAttribute(-1, "gender", gender);
                        parts.setAttribute(-1, "count", count.toString());
                        parts.setAttribute(-1, "case", case1);
                        set.add(parts.toString());
                    }
                }
            }
        }

        /**
         * Is the given PathType locale-dependent (for caching)?
         *
         * @param pathType the PathType
         * @return the boolean
         */
        private static boolean isLocaleDependent(PathType pathType) {
            /*
             * The paths that are locale-dependent are @count and /dayPeriods.
             */
            return (pathType == COUNT || pathType == DAY_PERIODS || pathType.equals(COUNT_CASE) || pathType.equals(COUNT_CASE_GENDER));
        }

        /**
         * Get the PathType from the given path
         *
         * @param path the path
         * @return the PathType
         *
         * Note: it would be more elegant and cleaner, but slower, if we used XPathParts to
         * determine the PathType. We avoid that since XPathParts.set is a performance hot spot. (NOTE: don't know if the preceding is true anymore.)
         */
        public static PathType getPathTypeFromPath(String path) {
            /*
             * Would changing the order of these tests ever change the return value?
             * Assume it could if in doubt.
             */
            if (path.indexOf("/metazone") > 0) {
                return PathType.METAZONE;
            }
            if (path.indexOf("/days") > 0) {
                return PathType.DAYS;
            }
            if (path.indexOf("/dayPeriods") > 0) {
                return PathType.DAY_PERIODS;
            }
            if (path.indexOf("/quarters") > 0) {
                return PathType.QUARTERS;
            }
            if (path.indexOf("/months") > 0) {
                return PathType.MONTHS;
            }
            if (path.indexOf("/relative[") > 0) {
                /*
                 * include "[" in "/relative[" to avoid matching "/relativeTime" or "/relativeTimePattern".
                 */
                return PathType.RELATIVE;
            }
            if (path.indexOf("/decimalFormatLength") > 0) {
                return PathType.DECIMAL_FORMAT_LENGTH;
            }
            if (path.indexOf("/unitLength[@type=\"long\"]") > 0) {
                if (path.indexOf("compoundUnitPattern1") > 0) {
                    return PathType.COUNT_CASE_GENDER;
                }
                if (path.indexOf("/unitPattern[") > 0) {
                    return PathType.COUNT_CASE;
                }
            }
            if (path.indexOf("[@count=") > 0) {
                return PathType.COUNT;
            }
            return PathType.SINGLETON;
        }

        /**
         * Get the PathType from the given XPathParts
         *
         * @param parts the XPathParts
         * @return the PathType
         * @deprecated
         */
        @Deprecated
        private static PathType getPathTypeFromParts(XPathParts parts) {
            if (true) {
                throw new UnsupportedOperationException("Code not updated. We may want to try using XPathParts in a future optimization, so leaving for now.");
            }
            /*
             * Would changing the order of these tests ever change the return value?
             * Assume it could if in doubt.
             */
            if (parts.containsElement("metazone")) {
                return PathType.METAZONE;
            }
            if (parts.containsElement("days")) {
                return PathType.DAYS;
            }
            if (parts.containsElement("dayPeriods")) {
                return PathType.DAY_PERIODS;
            }
            if (parts.containsElement("quarters")) {
                return PathType.QUARTERS;
            }
            if (parts.containsElement("months")) {
                return PathType.MONTHS;
            }
            if (parts.containsElement("relative")) {
                return PathType.RELATIVE;
            }
            if (parts.containsElement("decimalFormatLength")) {
                return PathType.DECIMAL_FORMAT_LENGTH;
            }
            if (parts.containsAttribute("count")) { // containsAttribute not containsElement
                return PathType.COUNT;
            }
            return PathType.SINGLETON;
        }
    }
}
