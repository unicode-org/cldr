package org.unicode.cldr.icu;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.ant.CLDRConverterTool.Alias;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.MeasurementType;

/**
 * A mapper that converts locale data from CLDR to the ICU data structure.
 * @author jchye
 */
public class LocaleMapper extends LdmlMapper {
    public static final String ALIAS_PATH = "/\"%%ALIAS\"";

    /**
     * Map for converting enums to their integer values.
     */
    private static final Map<String,String> enumMap = Builder.with(new HashMap<String,String>())
            .put("titlecase-firstword", "1").freeze();

    private static final Pattern DRAFT_PATTERN = Pattern.compile("\\[@draft=\"\\w+\"]");
    private static final Pattern TERRITORY_XPATH = Pattern.compile(
            "//ldml/localeDisplayNames/territories/territory\\[@type=\"(\\w+)\"]");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$");
    private static final Pattern RB_DATETIMEPATTERN = Pattern.compile(
            "/calendar/(\\w++)/DateTimePatterns");

    private SupplementalDataInfo supplementalDataInfo;
    private Factory factory;
    private Factory specialFactory;

    private Set<String> deprecatedTerritories;

    /**
     * Special hack comparator, so that RB strings come out in the right order.
     * This is only important for the order of items in arrays.
     */
    private static Comparator<String> comparator = new Comparator<String>() {
        private final Pattern CURRENCY_FORMAT = Pattern.compile(
            "//ldml/numbers/currencies/currency\\[@type=\"\\w++\"]/(.++)");
        private final Pattern DATE_OR_TIME_FORMAT = Pattern.compile(
                "//ldml/dates/calendars/calendar\\[@type=\"\\w++\"]/(date|time)Formats/.*");
        private final Pattern MONTH_PATTERN = Pattern.compile(
                "//ldml/dates/calendars/calendar\\[@type=\"\\w++\"]/months/monthContext\\[@type=\"[\\w\\-]++\"]/monthWidth\\[@type=\"\\w++\"]/month\\[@type=\"\\d++\"](\\[@yeartype=\"leap\"])?");
        private final Pattern CONTEXT_TRANSFORM = Pattern.compile(
                "//ldml/contextTransforms/contextTransformUsage\\[@type=\"([^\"]++)\"]/contextTransform\\[@type=\"([^\"]++)\"]");

        private final String[] CURRENCY_ORDER = {"symbol", "displayName",
                "pattern[@type=\"standard\"]", "decimal", "group"};
        /**
         * Reverse the ordering of the following:
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/displayName ; curr ; /Currencies/$1
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/symbol ; curr ; /Currencies/$1
         * and the following (time/date)
         * //ldml/dates/calendars/calendar[@type="([^"]*)"]/(dateFormats|dateTimeFormats|timeFormats)/(?:[^/\[]*)[@type="([^"]*)"]/(?:[^/\[]*)[@type="([^"]*)"]/.* ; locales ; /calendar/$1/DateTimePatterns
         */
        @SuppressWarnings("unchecked")
        @Override
        public int compare(String arg0, String arg1) {
            Matcher[] matchers = new Matcher[2];
            if (matches(CURRENCY_FORMAT, arg0, arg1, matchers)) {
                // Use ldml ordering except that symbol should be first.
                int index0 = getIndexOf(CURRENCY_ORDER, matchers[0].group(1));
                int index1 = getIndexOf(CURRENCY_ORDER, matchers[1].group(1));
                return index0 - index1;
            } else if (matches(DATE_OR_TIME_FORMAT, arg0, arg1, matchers)) {
                int compareValue = matchers[0].group(1).compareTo(matchers[1].group(1));
                if (compareValue != 0) return -compareValue;
            } else if (matches(CONTEXT_TRANSFORM, arg0, arg1, matchers)) {
                // Sort uiListOrMenu before stand-alone.
                if (matchers[0].group(1).equals(matchers[1].group(1))) {
                    return -matchers[0].group(2).compareTo(matchers[1].group(2));
                }
            } else if (matches(MONTH_PATTERN, arg0, arg1, matchers)) {
                // Sort leap year types after normal month types.
                String matchGroup0 = matchers[0].group(1);
                String matchGroup1 = matchers[1].group(1);
                if (matchGroup0 != matchGroup1) {
                    return matchGroup0 == null && matchGroup1 != null ? -1 : 1;
                }
            }

            return CLDRFile.ldmlComparator.compare(arg0, arg1);
        }
    };
    
    /**
     * Looks for a string in an array
     * @param order the array to be searched
     * @param key the string to be searched for
     * @return the index of the string if found, -1 if not found
     */
    private static int getIndexOf(String[] order, String key) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(key)) return i;
        }
        return -1;
    }

    public LocaleMapper(Factory factory, Factory specialFactory,
            SupplementalDataInfo supplementalDataInfo) {
        super("ldml2icu_locale.txt");
        this.factory = factory;
        this.specialFactory = specialFactory;
        this.supplementalDataInfo = supplementalDataInfo;
    }

    /**
     * @return the set of locales available for processing by this mapper
     */
    public Set<String> getAvailable() {
        return factory.getAvailable();
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
     * present in CLDR data.
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
     * @param locale
     * @return the filled IcuData object
     */
    public IcuData fillFromCLDR(String locale) {
        Set<String> deprecatedTerritories = getDeprecatedTerritories();
        CLDRFile resolvedCldr = factory.make(locale, true);
        RegexLookup<RegexResult> pathConverter = getPathConverter(resolvedCldr);

        // First pass through the unresolved CLDRFile to get all icu paths.
        CLDRFile cldr = factory.make(locale, false);
        Map<String,CldrArray> pathValueMap = new HashMap<String,CldrArray>();
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
            RegexResult regexResult = matchXPath(pathConverter, cldr, xpath, matcherFound);
            if (regexResult == null) continue;
            String[] arguments = matcherFound.value.getInfo();
            for (PathValueInfo info : regexResult) {
                String rbPath = info.processRbPath(arguments);
                validRbPaths.add(rbPath);
                // The immediate parent of every path should also exist.
                validRbPaths.add(rbPath.substring(0, rbPath.lastIndexOf('/')));
            }
        }
        
        // Get all values from the resolved CLDRFile.
        for (String xpath : resolvedCldr) {
            addMatchesForPath(xpath, resolvedCldr, validRbPaths, pathConverter, pathValueMap);
        }

        // Add fallback paths if necessary.
        addFallbackValues(resolvedCldr, pathValueMap);

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
                CldrArray valueList = getCldrArray(rbPath, pathValueMap);
                // Create a dummy xpath to sort the value in front of the other date time formats.
                String basePath = "//ldml/dates/calendars/calendar[@type=\"" + calendar + "\"]/dateTimeFormats";
                String mediumFormatPath = basePath + "/dateTimeFormatLength[@type=\"medium\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
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
        icuData.setHasSpecial(hasSpecial);
        fillIcuData(pathValueMap, comparator, icuData);

        // More hacks
        hackAddExtras(resolvedCldr, locale, icuData);
        return icuData;
    }

    /**
     * Creates an IcuData object for an aliased locale.
     * NOTE: this method is not currently used because the -w parameter in
     * LDML2ICUConverter already writes aliases. When we get around to deprecating
     * the LDML2ICUConverter, use this to write aliases instead..
     */
    public IcuData fillFromCldr(Alias alias) {
        // TODO: is this method actually needed?
        String from = alias.from;
        String to = alias.to;
        String xpath = alias.xpath;
        if (!factory.getAvailable().contains(to)) {
            System.err.println(to + " doesn't exist, skipping alias " + from);
            return null;
        }

        if (from == null || to == null) {
            System.err.println("Malformed alias - no 'from' or 'to': from=\"" +
                    from + "\" to=\"" + to + "\"");
            return null;
        }

        if (to.indexOf('@') != -1 && xpath == null) {
            System.err.println("Malformed alias - '@' but no xpath: from=\"" +
                    from + "\" to=\"" + to + "\"");
            return null;
        }

        IcuData icuData = new IcuData("icu-locale-deprecates.xml & build.xml", from, true);
        System.out.println("aliased " + from + " to " + to);
        RegexLookup<RegexResult> pathConverter = getPathConverter();
        if (xpath == null) {
            Map<String,CldrArray> pathValueMap = new HashMap<String,CldrArray>();
            addMatchesForPath(xpath, null, null, pathConverter, pathValueMap);
            fillIcuData(pathValueMap, comparator, icuData);
        } else {
            icuData.add("/\"%%ALIAS\"", to.substring(0, to.indexOf('@')));
        }
        return icuData;
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
     * @return the result of converting an xpath into an ICU-style path
     */
    private static RegexResult matchXPath(RegexLookup<RegexResult> lookup,
            CLDRFile cldr, String path,
            Output<Finder> matcherFound) {
        String fullPath = cldr.getFullXPath(path);
        fullPath = fullPath == null ? path : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
        RegexResult result = lookup.get(fullPath, null, null, matcherFound, null);
        return result;
    }

    /**
     * Attempts to match an xpath and adds the results of a successful match to
     * the specified map
     * @param xpath the xpath to be matched
     * @param cldrFile the CLDR file to get locale data from
     * @param validRbPaths the set of valid rbPaths that the result must belong
     *        to, null if such a requirement does not exist
     * @param pathValueMap the map that the results will be added to
     */
    private void addMatchesForPath(String xpath, CLDRFile cldrFile,
            Set<String> validRbPaths, RegexLookup<RegexResult> pathConverter,
            Map<String, CldrArray> pathValueMap) {
        Output<Finder> matcher = new Output<Finder>();
        RegexResult regexResult = matchXPath(pathConverter,
            cldrFile, xpath, matcher);
        if (regexResult == null) return;
        String[] arguments = matcher.value.getInfo();
        for (PathValueInfo info : regexResult) {
            String rbPath = info.processRbPath(arguments);
            // Don't add additional paths at this stage.
            if (validRbPaths != null && !validRbPaths.contains(rbPath)) continue;
            CldrArray valueList = getCldrArray(rbPath, pathValueMap);
            List<String> values = info.processValues(arguments, cldrFile, xpath);
            String groupKey = info.processGroupKey(arguments);
            valueList.add(xpath, values, groupKey);
        }
    }
    
    /**
     * Adds all mappings that couldn't be represented in the ldml2icu.txt file.
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
        String versionPath = cldrResolved.getFullXPath("//ldml/identity/version");
        Matcher versionMatcher = VERSION_PATTERN.matcher(versionPath);
        if (!versionMatcher.find()) {
            int failPoint = RegexUtilities.findMismatch(versionMatcher, versionPath);
            String show = versionPath.substring(0, failPoint) + "☹" + versionPath.substring(failPoint);
            throw new IllegalArgumentException("no version match with: " + show);
        }
        int versionNum = Integer.parseInt(versionMatcher.group(1));
        String versionValue = "2.0." + (versionNum / 100) + "." + (versionNum % 100);
        icuData.add("/Version", versionValue);

        // PaperSize:intvector{ 279, 216, }
        String localeID = cldrResolved.getLocaleID();
        String path = "/PaperSize:intvector";
        String paperType = getMeasurementToDisplay(localeID, MeasurementType.paperSize);
        if (paperType == null) {
            // do nothing
        } else if (paperType.equals("A4")) {
            icuData.add(path, new String[]{"297", "210"});
        } else if (paperType.equals("US-Letter")) {
            icuData.add(path, new String[]{"279", "216"});
        } else {
            throw new IllegalArgumentException("Unknown paper type");
        }

        // MeasurementSystem:int{1}
        path = "/MeasurementSystem:int";
        String measurementSystem = getMeasurementToDisplay(localeID, MeasurementType.measurementSystem);
        if (measurementSystem == null) {
            // do nothing
        } else if (measurementSystem.equals("metric")) {
            icuData.add(path, "0");
        } else if (measurementSystem.equals("US")) {
            icuData.add(path, "1");
        } else {
            throw new IllegalArgumentException("Unknown measurement system");
        }
    }
    
    /**
     * Returns the measurement to be displayed for the specified locale and
     * measurement type. Measurements should not be displayed if the immediate
     * parent of the locale has the same measurement as the locale.
     * @param localeID
     * @param measurementType
     * @return the measurement to be displayed, or null if it should not be displayed
     */
    private String getMeasurementToDisplay(String localeID, MeasurementType measurementType) {
        String type = getMeasurement(localeID, measurementType);
        if (type == null) return null;
        // Don't add type if a parent has the same value for that type.
        String parent = LocaleIDParser.getParent(localeID);
        String parentType = null;
        while (parentType == null && parent != null) {
            parentType = getMeasurement(parent, measurementType);
            parent = LocaleIDParser.getParent(parent);
        }
        return type.equals(parentType) ? null : type;
    }

    /**
     * @param localeID
     * @param measurementType the type of measurement required
     * @return the measurement of the specified locale
     */
    private String getMeasurement(String localeID, MeasurementType measurementType) {
        String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();
        Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo.getTerritoryMeasurementData();
        Map<String, String> typeMap = regionMeasurementData.get(measurementType);
        return typeMap.get(region);
    }

}
