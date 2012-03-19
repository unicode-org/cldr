package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.SupplementalDataInfo.MeasurementType;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.Transform;

/**
 * Converts CLDR locale XML files to a format suitable for outputting ICU data
 * with.
 * @author jchye
 *
 */
public class LdmlLocaleMapper {
    private static final Pattern DRAFT_PATTERN = Pattern.compile("\\[@draft=\"\\w+\"]");
    private static final Pattern TERRITORY_XPATH = Pattern.compile("//ldml/localeDisplayNames/territories/territory\\[@type=\"(\\w+)\"]");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$");

    private static RegexLookup<RegexResult> pathConverter  = null;
    private static Map<String, String> fallbackPaths;

    private SupplementalDataInfo supplementalDataInfo;
    private Factory factory;
    private Factory specialFactory;

    /**
     * A wrapper class for storing and working with the unprocessed values of a RegexResult.
     */
    private static class PathValueInfo {
        private String rbPath;
        private String rawValues;

        public PathValueInfo(String rbPath, String rawValues) {
            this.rbPath = rbPath;
            // HACK(jchye): Ideally we'd want to split raw values immediately,
            // but //ldml/dates/timeZoneNames/singleCountries contains multiple
            // values in its list attribute. We'll have to sacrifice performance
            // here and keep the value regex as a string for now.
            // Rename the value variable to avoid errors when processing the
            // other arguments.
            this.rawValues = rawValues == null ? null: rawValues.replace("$value", "{value}");
        }

        /**
         * @param arguments the arguments retrieved from the regex corresponding to this PathValueInfo
         * @return the processed rb path
         */
        public String processRbPath(String[] arguments) {
            return processString(rbPath, arguments);
        }
        
        public String[] processValues(String[] arguments, CLDRFile cldrFile, String xpath) {
            String[] result;
            if (rawValues != null) {
                String processedValue = processString(rawValues, arguments);
                result = processedValue.split("\\s+");
                for (int i = 0; i < result.length; i++) {
                    String value = result[i];
                    if (value.equals("{value}")) {
                        value = getStringValue(cldrFile, xpath);
                    } else if (value.startsWith("//ldml/")) {
                        value = getStringValue(cldrFile, value);
                    }
                    result[i] = value;
                }
            } else {
                result = new String[] { getStringValue(cldrFile, xpath) };
            }
            return result;
        }

        @Override
        public String toString() { return rbPath + "=" + rawValues; }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof PathValueInfo) {
                PathValueInfo otherInfo = (PathValueInfo)o;
                return rbPath.equals(otherInfo.rbPath)
                        && rawValues.equals(otherInfo.rawValues);
            } else {
                return false;
            }
        }

        private String processString(String value, String[] arguments) {
            if (value == null) {
                return null;
            }
            try {
                return getPathConverter().replace(value, arguments);
            } catch(ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("Error while filling out arguments in " + value + " with " + Arrays.asList(arguments), e);
            }
        }
    }

    static class RegexResult implements Iterable<PathValueInfo> {
        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^\\$(\\d+)=(//.*)");
        private Set<PathValueInfo> unprocessed;
        private Map<Integer, String> requiredArgs;

        public RegexResult(String source) {
            try {
                String[] parts = LDMLConverter.SEMI.split(source);
                String rbPath = parts[0];
                String value = null;
                requiredArgs = new HashMap<Integer, String>();
                if (parts.length > 1) {
                    String[] rawValues = parts[1].split(",");
                    for (String rawValue : rawValues) {
                        Matcher argMatcher = ARGUMENT_PATTERN.matcher(rawValue);
                        if (rawValue.startsWith("values=")) {
                            value = rawValue.substring(7);
                        } else if (argMatcher.matches()) {
                            requiredArgs.put(Integer.parseInt(argMatcher.group(1)),
                                    argMatcher.group(2));
                        }
                    }
                }
                unprocessed = new HashSet<PathValueInfo>();
                unprocessed.add(new PathValueInfo(rbPath, value));
            } catch (Exception e) {
                throw new IllegalArgumentException("Error while parsing "
                        + source, e);
            }
        }
        
        /**
         * Merges this result with another RegexResult.
         * @param otherResult
         */
        public void merge(RegexResult otherResult) {
            for (PathValueInfo struct : otherResult.unprocessed) {
                unprocessed.add(struct);
            }
        }

        /**
         * Each RegexResult is only accessible if its corresponding regex
         * matched. However, there may be additional requirements imposed in order
         * for it to be a valid match, i.e. the arguments retrieved from the regex
         * must also match. This method checks that specified arguments match
         * the requirements for this RegexResult.
         * @param file
         * @param arguments
         * @return true if the arguments matched
         */
        public boolean argumentsMatch(CLDRFile file, String[] arguments) {
            for (int argNum : requiredArgs.keySet()) {
                if (arguments.length <= argNum) {
                    throw new IllegalArgumentException("Argument " + argNum + " mising");
                }
                String argFromCldr = getStringValue(file, requiredArgs.get(argNum));
                if (argFromCldr != null && !arguments[argNum].equals(argFromCldr)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Iterator<PathValueInfo> iterator() {
            return unprocessed.iterator();
        }
    }

    /**
     * The value for the regex is a pair, with the directory and the path. There
     * is an optional 3rd parameter, which is used for "fleshing out"
     */
    private static Transform<String, RegexResult> PairValueTransform    = new Transform<String, RegexResult>() {
        public RegexResult transform(String source) {
            return new RegexResult(source);
        }
    };
    
    private static Merger<RegexResult> RegexValueMerger = new Merger<RegexResult>() {
        @Override
        public RegexResult merge(RegexResult a, RegexResult into) {
            into.merge(a);
            return into;
        }
    };
    
    /**
     * The source for path regexes is much simpler if we automatically quote the
     * [ character in front of @.
     */
    private static Transform<String, RegexFinder> RegexFinderTransform = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            return new RegexFinder("^" + source.replace("[@", "\\[@") + "$");
        }
    };

    /**
     * Special hack comparator, so that RB strings come out in the right order.
     * This is only important for the order of items in arrays.
     */
    private static Comparator<String> SpecialLDMLComparator = new Comparator<String>() {
        private final Matcher DATE_OR_TIME_FORMAT   = Pattern.compile("/(date|time)Formats/").matcher("");
        private final Pattern MONTH_PATTERN = Pattern.compile("month\\[@type=\"(\\d++)\"](\\[@yeartype=\"leap\"])?$");
        private final Pattern CONTEXT_TRANSFORM = Pattern.compile("//ldml/contextTransforms/contextTransformUsage\\[@type=\"([^\"]++)\"]/contextTransform\\[@type=\"([^\"]++)\"]");

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
            // TODO: Optimize comparator.
            if (arg0.startsWith("//ldml/numbers/currencies/currency")
                    && arg1.startsWith("//ldml/numbers/currencies/currency")) {
                int last0 = arg0.lastIndexOf('/');
                int last1 = arg1.lastIndexOf('/');
                // Use ldml ordering except that symbol should be first.
                if (last0 == last1 && arg0.regionMatches(0, arg1, 0, last1)) {
                    if (arg0.substring(last0 + 1).equals("symbol")) {
                        return -1;
                    } else if (arg1.substring(last1 + 1).equals("symbol")) {
                        return 1;
                    }
                }
            } else if (arg0.startsWith("//ldml/dates/calendars/calendar")
                    && arg1.startsWith("//ldml/dates/calendars/calendar")
            ) {
                if (DATE_OR_TIME_FORMAT.reset(arg0).find()) {
                    int start0 = DATE_OR_TIME_FORMAT.start();
                    if (DATE_OR_TIME_FORMAT.reset(arg1).find()) {
                        int start1 = DATE_OR_TIME_FORMAT.start();
                        int end1 = DATE_OR_TIME_FORMAT.end();
                        if (start0 == start1 && arg0.regionMatches(0, arg1, 0, start1)
                                && !arg0.regionMatches(0, arg1, 0, end1)) {
                            return -arg0.substring(start0).compareTo(
                                    arg1.substring(start1));
                        }
                    }
                }
            } else if (arg0.startsWith("//ldml/contextTransforms")) {
                // Sort uiListOrMenu before stand-alone.
                Matcher matcher0 = CONTEXT_TRANSFORM.matcher(arg0);
                Matcher matcher1 = CONTEXT_TRANSFORM.matcher(arg1);
                    if (matcher0.matches() && matcher1.matches()
                            && matcher0.group(1).equals(matcher1.group(1))) {
                        return -matcher0.group(2).compareTo(matcher1.group(2));
                }
            }

            // Sort leap year types after normal month types.
            Matcher matcher0 = MONTH_PATTERN.matcher(arg0);
            if (matcher0.find()) {
                Matcher matcher1 = MONTH_PATTERN.matcher(arg1);
                if (matcher1.find() && matcher0.group(2) != matcher1.group(2)) {
                    return matcher0.group(2) == null && matcher1.group(2) != null ? -1 : 1;
                }
            }

            return CLDRFile.ldmlComparator.compare(arg0, arg1);
        }
    };

    /**
     * Loads the data in from a file. That file is of the form cldrPath ; rbPath
     */
    public static RegexLookup<RegexResult> getPathConverter() {
        if (pathConverter == null) {
            pathConverter = new RegexLookup<RegexResult>()
                .setPatternTransform(RegexFinderTransform)
                .setValueTransform(PairValueTransform)
                .setValueMerger(RegexValueMerger)
                .loadFromFile(LDMLConverter.class, "ldml2icu.txt");
        }
        return pathConverter;
    }
    
    public LdmlLocaleMapper(Factory factory, Factory specialFactory,
            SupplementalDataInfo supplementalDataInfo) {
        this.factory = factory;
        this.specialFactory = specialFactory;
        this.supplementalDataInfo = supplementalDataInfo;
    }

    public Set<String> getAvailable() {
        return factory.getAvailable();
    }
    
    private boolean hasSpecialFile(String filename) {
        return specialFactory != null && specialFactory.getAvailable().contains(filename);
    }

    private Set<String> deprecatedTerritories;
    private Set<String> getDeprecatedTerritories() {
        if (deprecatedTerritories == null) {
            deprecatedTerritories = Builder.with(
                supplementalDataInfo.getLocaleAliasInfo().get("territory").keySet())
                .remove("062").remove("172").remove("200").remove("830")
                .remove("AN").remove("CS").remove("QU").get();
        }
        return deprecatedTerritories;
    }

    public IcuData fillFromCLDR(String locale) {
        Set<String> deprecatedTerritories = getDeprecatedTerritories();
        RegexLookup<RegexResult> pathConverter = getPathConverter();

        // First pass through the unresolved CLDRFile to get all icu paths.
        CLDRFile cldr = factory.make(locale, false);
        Map<String,Map<String,String[]>> pathValueMap = new HashMap<String,Map<String,String[]>>();
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
                getSubMap(rbPath, pathValueMap);
                // The immediate parent of every path should also exist.
                getSubMap(rbPath.substring(0, rbPath.lastIndexOf('/')), pathValueMap);
            }
        }
        
        // Get all values from the resolved CLDRFile.
        CLDRFile resolvedCldr = factory.make(locale, true);
        Set<String> resolvedPaths = new HashSet<String>();
        CollectionUtilities.addAll(resolvedCldr.iterator(), resolvedPaths);
        resolvedPaths.addAll(getFallbackPaths().keySet());
        for (String xpath : resolvedPaths) {
            addMatchesForPath(xpath, resolvedCldr, pathValueMap, true);
        }

        // Add special values to file.
        IcuData icuData = new IcuData("common/main/" + locale + ".xml", locale);
        if (hasSpecialFile(locale)) {
            icuData.setHasSpecial(true);
            CLDRFile specialCldrFile = specialFactory.make(locale, false);
            for (String xpath : specialCldrFile) {
                addMatchesForPath(xpath, specialCldrFile, pathValueMap, false);
            }
        }

        // Convert values to final data structure.
        for (String rbPath : pathValueMap.keySet()) {
            List<String[]> values = new ArrayList<String[]>(pathValueMap.get(rbPath).values());
            if (values.size() > 0) {
                icuData.addAll(rbPath, values);
            }
        }
        // Hacks
        hackAddExtras(resolvedCldr, locale, icuData);
        return icuData;
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

    private void addMatchesForPath(String xpath, CLDRFile cldrFile,
            Map<String, Map<String, String[]>> pathValueMap, boolean selective) {
        Output<Finder> matcher = new Output<Finder>();
        RegexResult regexResult = matchXPath(getPathConverter(),
            cldrFile, xpath, matcher);
        if (regexResult == null) return;
        String[] arguments = matcher.value.getInfo();
        if (!regexResult.argumentsMatch(cldrFile, arguments)) return;
        for (PathValueInfo info : regexResult) {
            String rbPath = info.processRbPath(arguments);
            // Don't add additional paths at this stage.
            if (selective && !pathValueMap.containsKey(rbPath)) continue;
            Map<String, String[]> valueMap = getSubMap(rbPath, pathValueMap);
            String[] values = info.processValues(arguments, cldrFile, xpath);
            valueMap.put(xpath, values);
        }
    }
    
    private Map<String, String[]> getSubMap(String key, Map<String, Map<String, String[]>> pathValueMap) {
        Map<String, String[]> valueMap = pathValueMap.get(key);
        if (valueMap == null) {
            valueMap = new TreeMap<String, String[]>(SpecialLDMLComparator);
            pathValueMap.put(key, valueMap);
        }
        return valueMap;
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

    /**
     * @param cldrFile
     * @param xpath
     * @return the value of the specified xpath (fallback or otherwise)
     */
    private static String getStringValue(CLDRFile cldrFile, String xpath) {
        String value = cldrFile.getStringValue(xpath);
        if (value == null) value = getFallbackPaths().get(xpath);
        return value;
    }

    /**
     * Returns a mapping of fallback paths to their values. Fallback values are
     * used when the CLDR file doesn't contain a path that happens to have a fallback.
     * @return a mapping of fallback paths to their values
     */
    private static Map<String, String> getFallbackPaths() {
        if (fallbackPaths == null) {
            fallbackPaths = LDMLConverter.loadMapFromFile("ldml2icu_fallback_paths.txt");
        }
        return fallbackPaths;
    }
}
