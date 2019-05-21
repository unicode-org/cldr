package org.unicode.cldr.icu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.icu.RegexManager.CldrArray;
import org.unicode.cldr.icu.RegexManager.PathValueInfo;
import org.unicode.cldr.icu.RegexManager.RegexResult;
import org.unicode.cldr.test.DisplayAndInputProcessor.NumericType;
import org.unicode.cldr.tool.FilterFactory;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.SupplementalDataInfo;
//import org.unicode.cldr.util.SupplementalDataInfo.MeasurementType;

import com.ibm.icu.util.Output;

/**
 * A mapper that converts locale data from CLDR to the ICU data structure.
 *
 * @author jchye
 */
public class LocaleMapper extends Mapper {
    /**
     * Map for converting enums to their integer values.
     */
    private static final Map<String, String> enumMap = Builder.with(new HashMap<String, String>())
        .put("titlecase-firstword", "1")
        .put("no-change", "0")
        .freeze();

    private static final Pattern DRAFT_PATTERN = PatternCache.get("\\[@draft=\"\\w+\"]");
    private static final Pattern TERRITORY_XPATH = PatternCache.get(
        "//ldml/localeDisplayNames/territories/territory\\[@type=\"(\\w+)\"]");
    private static final Pattern RB_DATETIMEPATTERN = PatternCache.get(
        "/calendar/(\\w++)/DateTimePatterns");

    private SupplementalDataInfo supplementalDataInfo;
    // We may use different factories for resolved or unresolved CLDRFiles depending
    // on whether filtering is required.
    private Factory unresolvedFactory;
    private Factory resolvedFactory;
    private Factory specialFactory;
    private RegexManager manager;
    private String debugXPath;

    private Set<String> deprecatedTerritories;

    /**
     * Special hack comparator, so that RB strings come out in the right order.
     * This is only important for the order of items in arrays.
     */
    private static Comparator<String> comparator = new Comparator<String>() {
        private final Pattern CURRENCY_FORMAT = PatternCache.get(
            "//ldml/numbers/currencies/currency\\[@type=\"\\w++\"]/(.++)");
        private final Pattern DATE_OR_TIME_FORMAT = PatternCache.get(
            "//ldml/dates/calendars/calendar\\[@type=\"\\w++\"]/(date|time)Formats/.*");
        private final Pattern MONTH_PATTERN = PatternCache
            .get(
                "//ldml/dates/calendars/calendar\\[@type=\"\\w++\"]/months/monthContext\\[@type=\"[\\w\\-]++\"]/monthWidth\\[@type=\"\\w++\"]/month\\[@type=\"\\d++\"](\\[@yeartype=\"leap\"])?");
        private final Pattern CONTEXT_TRANSFORM = PatternCache.get(
            "//ldml/contextTransforms/contextTransformUsage\\[@type=\"([^\"]++)\"]/contextTransform\\[@type=\"([^\"]++)\"]");

        private final String[] CURRENCY_ORDER = { "symbol", "displayName",
            "pattern[@type=\"standard\"]", "decimal", "group" };

        /**
         * Reverse the ordering of the following:
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/displayName ; curr ; /Currencies/$1
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/symbol ; curr ; /Currencies/$1
         * and the following (time/date)
         * //ldml/dates/calendars/calendar[@type="([^"]*)"]/(dateFormats|dateTimeFormats|timeFormats)/(?:[^/\[]*)[@type=
         * "([^"]*)"]/(?:[^/\[]*)[@type="([^"]*)"]/.* ; locales ; /calendar/$1/DateTimePatterns
         */
        @Override
        public int compare(String arg0, String arg1) {
            Matcher[] matchers = new Matcher[2];
            if (RegexManager.matches(CURRENCY_FORMAT, arg0, arg1, matchers)) {
                // Use ldml ordering except that symbol should be first.
                int index0 = getIndexOf(CURRENCY_ORDER, matchers[0].group(1));
                int index1 = getIndexOf(CURRENCY_ORDER, matchers[1].group(1));
                return index0 - index1;
            } else if (RegexManager.matches(DATE_OR_TIME_FORMAT, arg0, arg1, matchers)) {
                int compareValue = matchers[0].group(1).compareTo(matchers[1].group(1));
                if (compareValue != 0) return -compareValue;
            } else if (RegexManager.matches(CONTEXT_TRANSFORM, arg0, arg1, matchers)) {
                // Sort uiListOrMenu before stand-alone.
                if (matchers[0].group(1).equals(matchers[1].group(1))) {
                    return -matchers[0].group(2).compareTo(matchers[1].group(2));
                }
            } else if (RegexManager.matches(MONTH_PATTERN, arg0, arg1, matchers)) {
                // Sort leap year types after normal month types.
                String matchGroup0 = matchers[0].group(1);
                String matchGroup1 = matchers[1].group(1);
                if (matchGroup0 != matchGroup1) {
                    return matchGroup0 == null && matchGroup1 != null ? -1 : 1;
                }
            }

            return CLDRFile.getComparator(DtdType.ldml).compare(arg0, arg1);
        }
    };

    /**
     * Looks for a string in an array
     *
     * @param order
     *            the array to be searched
     * @param key
     *            the string to be searched for
     * @return the index of the string if found, -1 if not found
     */
    private static int getIndexOf(String[] order, String key) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(key)) return i;
        }
        return -1;
    }

    /**
     * LocaleMapper constructor.
     *
     * @param factory
     *            the factory containing the CLDR data to be converted
     * @param specialFactory
     *            a factory containing any additional CLDR data
     * @param supplementalDataInfo
     *            SupplementalDataInfo object
     * @param useAltValues
     *            true if alt path filtering should be performed
     * @param organization
     *            the organization to filter the data by
     *            (null if coverage filtering is not needed)
     */
    public LocaleMapper(Factory factory, Factory specialFactory,
        SupplementalDataInfo supplementalDataInfo, boolean useAltValues,
        String organization) {
        manager = new RegexManager("ldml2icu_locale.txt");
        unresolvedFactory = resolvedFactory = factory;
        // If filtering is required, filter all unresolved CLDRFiles for use in
        // fillFromCldr(). We don't filter the resolved CLDRFiles by organization
        // coverage level because
        // some rbPaths (e.g. /calendar/x/DateTimePatterns) have a fixed number
        // of values that must always be present regardless of filtering.
        if (useAltValues || organization != null) {
            unresolvedFactory = FilterFactory.load(factory, organization, useAltValues);
            resolvedFactory = FilterFactory.load(factory, null, useAltValues);
        }
        this.specialFactory = specialFactory;
        this.supplementalDataInfo = supplementalDataInfo;
    }

    /**
     * @return the set of locales available for processing by this mapper
     */
    @Override
    public Set<String> getAvailable() {
        return unresolvedFactory.getAvailable();
    }

    /**
     * @param filename
     * @return true if a special XML file with the specified filename is available.
     */
    private boolean hasSpecialFile(String filename) {
        return specialFactory != null && specialFactory.getAvailable().contains(filename);
    }

    /**
     * @return the set of deprecated territories to be ignored. Remove when no longer
     *         present in CLDR data.
     */
    private Set<String> getDeprecatedTerritories() {
        if (deprecatedTerritories == null) {
            deprecatedTerritories = Builder.with(
                supplementalDataInfo.getLocaleAliasInfo().get("territory").keySet())
                .remove("062").remove("172").remove("200").remove("830")
                .remove("AN").remove("CS").remove("QU").get();
        }
        return deprecatedTerritories;
    }

    /**
     * Fills an IcuData object using the CLDR data for the specified locale.
     *
     * @param locale
     * @return the filled IcuData object
     */
    @Override
    public IcuData[] fillFromCldr(String locale) {
        Set<String> deprecatedTerritories = getDeprecatedTerritories();
        CLDRFile resolvedCldr = resolvedFactory.make(locale, true);
        RegexLookup<RegexResult> pathConverter = manager.getPathConverter(resolvedCldr);

        // First pass through the unresolved CLDRFile to get all icu paths.
        CLDRFile cldr = unresolvedFactory.make(locale, false);
        Map<String, CldrArray> pathValueMap = new HashMap<String, CldrArray>();
        Set<String> validRbPaths = new HashSet<String>();
        for (String xpath : cldr) {
            // Territory hacks to be removed once CLDR data is fixed.
            Matcher matcher = TERRITORY_XPATH.matcher(xpath);
            if (matcher.matches()) {
                String country = matcher.group(1);
                if (deprecatedTerritories.contains(country)) {
                    continue;
                }
            }

            // Add rb paths.
            Output<Finder> matcherFound = new Output<Finder>();
            Output<String[]> firstInfo = new Output<>();
            RegexResult regexResult = matchXPath(pathConverter, cldr, xpath, matcherFound, firstInfo);
            if (regexResult == null) continue;
//            String[] arguments = matcherFound.value.getInfo();
            String[] arguments = firstInfo.value;
            for (PathValueInfo info : regexResult) {
                String rbPath = info.processRbPath(arguments);
                validRbPaths.add(rbPath);
                // The immediate parent of every path should also exist.
                validRbPaths.add(rbPath.substring(0, rbPath.lastIndexOf('/')));
            }
        }

        // Get all values from the resolved CLDRFile.
        for (String xpath : resolvedCldr) {
            // Since the unresolved CLDRFile may have been modified, use it
            // to add values instead of the resolved CLDRFile if possible.
            CLDRFile fileToUse = cldr.getStringValue(xpath) == null ? resolvedCldr : cldr;
            addMatchesForPath(xpath, fileToUse, validRbPaths, pathConverter, pathValueMap);
        }

        // Add fallback paths if necessary.
        manager.addFallbackValues(resolvedCldr, pathValueMap);

        // Add special values to file.
        boolean hasSpecial = hasSpecialFile(locale);
        if (hasSpecial) {
            CLDRFile specialCldrFile = specialFactory.make(locale, false);
            for (String xpath : specialCldrFile) {
                if (resolvedCldr.isHere(xpath)) continue;
                addMatchesForPath(xpath, specialCldrFile, null, pathConverter, pathValueMap);
            }
        }

        for (String rbPath : pathValueMap.keySet()) {
            // HACK: DateTimePatterns needs a duplicate of the medium
            // dateTimeFormat (formerly indicated using dateTimeFormats/default).
            // This hack can be removed when ICU no longer requires it.
            Matcher matcher = RB_DATETIMEPATTERN.matcher(rbPath);
            if (matcher.matches()) {
                String calendar = matcher.group(1);
                CldrArray valueList = RegexManager.getCldrArray(rbPath, pathValueMap);
                // Create a dummy xpath to sort the value in front of the other date time formats.
                String basePath = "//ldml/dates/calendars/calendar[@type=\"" + calendar + "\"]/dateTimeFormats";
                String mediumFormatPath = basePath
                    + "/dateTimeFormatLength[@type=\"medium\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                valueList.add(basePath,
                    getStringValue(resolvedCldr, mediumFormatPath),
                    null);
            }
        }

        // HACK: Fill missing narrow era values with their abbreviated versions.
        CldrArray narrowEras = pathValueMap.get("/calendar/japanese/eras/narrow");
        CldrArray abbreviatedEras = pathValueMap.get("/calendar/japanese/eras/abbreviated");
        if (narrowEras != null && abbreviatedEras != null) {
            narrowEras.addAll(abbreviatedEras);
        }

        IcuData icuData = new IcuData("common/main/" + locale + ".xml", locale, true, enumMap);
        if (hasSpecial) {
            icuData.setFileComment("ICU <specials> source: <path>/common/main/" + locale + ".xml");
        }
        fillIcuData(pathValueMap, comparator, icuData);

        // More hacks
        hackAddExtras(resolvedCldr, locale, icuData);
        return new IcuData[] { icuData };
    }

    private void fillIcuData(Map<String, CldrArray> pathValueMap,
        Comparator<String> comparator, IcuData icuData) {
        // Convert values to final data structure.
        for (String rbPath : pathValueMap.keySet()) {
            icuData.addAll(rbPath, pathValueMap.get(rbPath).sortValues(comparator));
        }
    }

    public static String getFullXPath(String xpath, CLDRFile cldrFile) {
        String fullPath = cldrFile.getFullXPath(xpath);
        return fullPath == null ? xpath : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
    }

    /**
     * @param cldr
     * @param path
     * @param matcherFound
     * @param firstInfo
     * @return the result of converting an xpath into an ICU-style path
     */
    private RegexResult matchXPath(RegexLookup<RegexResult> lookup,
        CLDRFile cldr, String path,
        Output<Finder> matcherFound, Output<String[]> firstInfo) {
        String fullPath = cldr.getFullXPath(path);
        fullPath = fullPath == null ? path : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
        List<String> debugResults = isDebugXPath(fullPath) ? new ArrayList<String>() : null;
        Output<String[]> info = new Output<>();
        RegexResult result = lookup.get(fullPath, null, info, matcherFound, debugResults);
        if (debugResults != null) {
            if (result == null) {
                RegexManager.printLookupResults(fullPath, debugResults);
            } else {
                System.out.println(fullPath + " successfully matched");
            }
        }
        if (firstInfo != null && info.value != null) {
            firstInfo.value = info.value;
        }
        return result;
    }

    /**
     * Attempts to match an xpath and adds the results of a successful match to
     * the specified map
     *
     * @param xpath
     *            the xpath to be matched
     * @param cldrFile
     *            the CLDR file to get locale data from
     * @param validRbPaths
     *            the set of valid rbPaths that the result must belong
     *            to, null if such a requirement does not exist
     * @param pathValueMap
     *            the map that the results will be added to
     */
    private void addMatchesForPath(String xpath, CLDRFile cldrFile,
        Set<String> validRbPaths, RegexLookup<RegexResult> pathConverter,
        Map<String, CldrArray> pathValueMap) {
        Output<Finder> matcher = new Output<Finder>();
        Output<String[]> firstInfo = new Output<>();
        RegexResult regexResult = matchXPath(pathConverter,
            cldrFile, xpath, matcher, firstInfo);
        if (regexResult == null) return;
//        String[] arguments = matcher.value.getInfo();
        String[] arguments = firstInfo.value;
        String cldrValue = getStringValue(cldrFile, xpath);
        for (PathValueInfo info : regexResult) {
            String rbPath = info.processRbPath(arguments);
            // Don't add additional paths at this stage.
            if (validRbPaths != null && !validRbPaths.contains(rbPath)) continue;
            CldrArray valueList = RegexManager.getCldrArray(rbPath, pathValueMap);
            List<String> values = info.processValues(arguments, cldrValue);
            String baseXPath = info.processXPath(arguments, xpath);
            String groupKey = info.processGroupKey(arguments);
            valueList.put(baseXPath, values, groupKey);
        }
    }

    /**
     * @param cldrFile
     * @param xpath
     * @return the value of the specified xpath (fallback or otherwise)
     */
    private String getStringValue(CLDRFile cldrFile, String xpath) {
        String value = cldrFile.getStringValue(xpath);
        // HACK: DAIP doesn't currently make spaces in currency formats non-breaking.
        // Remove this when fixed.
        if (NumericType.getNumericType(xpath) == NumericType.CURRENCY) {
            value = value.replace(' ', '\u00A0');
        }
        return value;
    }

    /**
     * Adds all mappings that couldn't be represented in the ldml2icu.txt file.
     *
     * @param cldrResolved
     * @param locale
     */
    private void hackAddExtras(CLDRFile cldrResolved, String locale, IcuData icuData) {
        // Specify parent of non-language locales.
        String parent = supplementalDataInfo.getExplicitParentLocale(locale);
        if (parent != null) {
            icuData.add("/%%Parent", parent);
        }

        // <version number="$Revision: 5806 $"/>
        String version = cldrResolved.getFullXPath("//ldml/identity/version");
        icuData.add("/Version", MapperUtils.formatVersion(version));

        // PaperSize:intvector{ 279, 216, } - now in supplemental
        // MeasurementSystem:int{1} - now in supplemental

        // Default calendar.
        String localeID = cldrResolved.getLocaleID();
        String calendar = getCalendarIfDifferent(localeID);
        if (calendar != null) {
            icuData.add("/calendar/default", calendar);
        }
    }

    /**
     * Returns the default calendar to be used for a locale. If the default
     * calendar for the parent locale is the same, null is returned.
     */
    private String getCalendarIfDifferent(String localeID) {
        String calendar = getCalendar(localeID);
        if (calendar == null) return null;
        String parent = LocaleIDParser.getParent(localeID);
        String parentCalendar = null;
        while (parentCalendar == null && parent != null) {
            parentCalendar = getCalendar(parent);
            parent = LocaleIDParser.getParent(parent);
        }
        return calendar.equals(parentCalendar) ? null : calendar;
    }

    /**
     * Returns the default calendar to be used for a locale, if any.
     */
    private String getCalendar(String localeID) {
        LanguageTagParser parser = new LanguageTagParser().set(localeID);
        String region = localeID.equals("root") ? "001" : parser.getRegion();
        if (region.equals("")) {
            localeID = supplementalDataInfo.getLikelySubtags().get(parser.getLanguage());
            if (localeID == null) {
                throw new RuntimeException("Likely subtag not found for " + parser.getLanguage());
            }
            parser.set(localeID);
            region = parser.getRegion();
            if (region == null) region = "001";
        }
        List<String> calendars = supplementalDataInfo.getCalendars(region);
        return calendars == null ? null : calendars.get(0);
    }

    //private String getMeasurementToDisplay(String localeID, MeasurementType measurementType) {...} // deleted

    /**
     * @param localeID
     * @param measurementType
     *            the type of measurement required
     * @return the measurement of the specified locale
     */
//    private String getMeasurement(String localeID, MeasurementType measurementType) {
//        String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();
//        Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo
//            .getTerritoryMeasurementData();
//        Map<String, String> typeMap = regionMeasurementData.get(measurementType);
//        return typeMap.get(region);
//    }     //not used

    /**
     * Sets xpath to monitor for debugging purposes.
     * @param debugXPath
     */
    public void setDebugXPath(String debugXPath) {
        this.debugXPath = debugXPath;
    }

    /**
     * @param xpath
     * @return true if the xpath is to be debugged
     */
    boolean isDebugXPath(String xpath) {
        return debugXPath == null ? false : xpath.startsWith(debugXPath);
    }

    @Override
    public Makefile generateMakefile(Collection<String> aliases) {
        Makefile makefile = new Makefile("GENRB");
        makefile.addSyntheticAlias(aliases);
        makefile.addAliasSource();
        makefile.addSource(sources);
        return makefile;
    }
}
