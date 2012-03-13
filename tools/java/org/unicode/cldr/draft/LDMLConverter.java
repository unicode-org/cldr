package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.MeasurementType;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;

/**
 * Prototype simpler mechanism for converting to ICU Resource Bundles. The
 * format is almost entirely data-driven instead of having lots of special-case
 * code.
 * 
 * @author markdavis
 */
public class LDMLConverter {
    private static final boolean DEBUG = false;

    private static Pattern TERRITORY_XPATH = Pattern.compile("//ldml/localeDisplayNames/territories/territory\\[@type=\"(\\w+)\"]");

    /**
     * What we use as ID characters (actually ASCII would suffice).
     */
    static final UnicodeSet                      ID_CHARACTERS        = new UnicodeSet("[-:[:xid_continue:]]").freeze();
    static final UnicodeSet                      ID_START             = new UnicodeSet("[:xid_start:]").freeze();

    /**
     * The source for path regexes is much simpler if we automatically quote the
     * [ character in front of @.
     */
    public static Transform<String, RegexFinder> RegexFinderTransform = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            return new RegexFinder("^" + source.replace("[@", "\\[@") + "$");
        }
    };

    static final Pattern                         SEMI                 = Pattern.compile("\\s*+;\\s*+");

    private Factory factory;
    private Factory specialFactory;
    private String destinationDir;
    private SupplementalDataInfo supplementalDataInfo;
    private boolean usedSpecial = false; // TODO: refactor this

    static class PathValueInfo {
        private String rbPath;
        private String rawValues;

        public PathValueInfo(String rbPath, String rawValues) {
            this.rbPath = rbPath;
            // HACK(jchye): Ideally we'd want to split raw values immediately,
            // but //ldml/dates/timeZoneNames/singleCountries contains multiple
            // values in its list attribute. We'll have to sacrifice performance
            // here and keep the value regex as a string for now.
            this.rawValues = rawValues == null ? "{value}" : rawValues.replace("$value", "{value}");
        }

        public String processRbPath(String[] arguments) {
            return processString(rbPath, arguments);
        }
        
        public String[] processValues(String[] arguments, CLDRFile cldrFile, String xpath) {
            String[] result;
            if (rawValues != null) {
                // Rename the value variable to avoid errors when processing the
                // other arguments.
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
    }

    static class RegexResult implements Iterable<PathValueInfo> {
        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^\\$(\\d+)=(//.*)");
        private Set<PathValueInfo> unprocessed;
        private Map<Integer, String> requiredArgs;

        public RegexResult(String source) {
            try {
                String[] parts = SEMI.split(source);
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
     * Map for converting enums to their integer values.
     */
    private static final Map<String,String> enumMap = Builder.with(new HashMap<String,String>())
            .put("titlecase-firstword", "1").freeze();

    private static final Set<String> patternCache = new HashSet<String>();
    
    /**
     * The value for the regex is a pair, with the directory and the path. There
     * is an optional 3rd parameter, which is used for "fleshing out"
     */
    public static Transform<String, RegexResult> PairValueTransform    = new Transform<String, RegexResult>() {
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
    // TODO: this definitely shouldn't be static.
    static RegexLookup<RegexResult>              pathConverter         = new RegexLookup<RegexResult>()
            .setPatternTransform(RegexFinderTransform)
            .setValueTransform(PairValueTransform)
            .setValueMerger(RegexValueMerger)
            .loadFromFile(LDMLConverter.class, "ldml2icu.txt");
    
    /**
     * ICU paths have a simple comparison, alphabetical within a level. We do
     * have to catch the / so that it is lower than everything.
     */
    public static final Comparator<String>       PATH_COMPARATOR       =
        new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            int min = Math.min(arg0.length(), arg1.length());
            for (int i = 0; i < min; ++i) {
                int ch0 = arg0.charAt(i);
                int ch1 = arg1.charAt(i);
                int diff = ch0 - ch1;
                if (diff == 0) {
                    continue;
                }
                if (ch0 == '/') {
                    return -1;
                } else if (ch1 == '/') {
                    return 1;
                } else {
                    return diff;
                }
            }
            return arg0.length() - arg1.length();
        }
    };
    
    private Map<String,List<String[]>> rbPathToValues;

    public LDMLConverter() {
        rbPathToValues = new HashMap<String,List<String[]>>();
    }

    public void clear() {
        rbPathToValues.clear();
    }

    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     * 
     * @param path
     * @param value
     * @return
     */
    private void add(String path, String[] value) {
        List<String[]> list = rbPathToValues.get(path);
        if (list == null) {
            rbPathToValues.put(path, list = new ArrayList<String[]>(1));
        }
        list.add(value);
    }

    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     * 
     * @param path
     * @param value
     * @return
     */
    void add(String path, String value) {
        if (DEBUG) {
            System.out.println("+++\t" + path + "\t" + value);
        }
        add(path, value.split("\\s++"));
    }

    /**
     * Write a file in ICU format. LDML2ICUConverter currently has some
     * funny formatting in a few cases; don't try to match everything.
     * 
     * @param dirPath the directory to write the file to
     * @param locale  the locale that the file is being written for
     */
    void writeRB(Map<String, List<String[]>> rbValues, String dirPath, String locale) throws IOException {
        boolean wasSingular = false;
        String[] replacements = { "%file%", locale };
        PrintWriter out = BagFormatter.openUTF8Writer(dirPath, locale + ".txt");
        out.write('\uFEFF');
        FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
        if (usedSpecial) {
            out.println("/**");
            out.println(" *  ICU <specials> source: <path>/xml/main/" + locale + ".xml");
            out.println(" */");
        }
        String[] lastLabels = new String[] {};

        out.append(locale);
        List<String> sortedPaths = new ArrayList<String>(rbValues.keySet());
        Collections.sort(sortedPaths, PATH_COMPARATOR);
        for (String path : sortedPaths) {
            // Write values to file.
            String[] labels = path.split("/", -1); // Don't discard trailing slashes.
            int common = getCommon(lastLabels, labels);
            for (int i = lastLabels.length - 1; i > common; --i) {
                if (wasSingular) {
                    wasSingular = false;
                } else {
                    out.append(Utility.repeat(TAB, i));
                }
                out.append("}\n");
            }
            for (int i = common + 1; i < labels.length; ++i) {
                final String pad = Utility.repeat(TAB, i);
                out.append(pad);
                out.append(labels[i]).append('{');
                if (i != labels.length - 1) {
                    out.append('\n');
                }
            }
            boolean quote = !isIntRbPath(path);
            List<String[]> values = rbValues.get(path);
            wasSingular = appendValues(values, labels.length, quote, out);
            out.flush();
            lastLabels = labels;
        }
        // Add last closing braces.
        for (int i = lastLabels.length - 1; i > 0; --i) {
            if (wasSingular) {
                wasSingular = false;
            } else {
                out.append(Utility.repeat(TAB, i));
            }
            out.append("}\n");
        }
        out.append("}\n");
        out.close();
    }
    
    /**
     * Inserts padding and values between braces.
     * @param values
     * @param numTabs
     * @param quote
     * @param out
     * @return
     */
    private boolean appendValues(List<String[]> values, int numTabs, boolean quote, PrintWriter out) {
        String[] firstArray;
        boolean wasSingular = false;
        if (values.size() == 1) {
            if ((firstArray = values.get(0)).length == 1) {
                String value = quoteInside(firstArray[0]);
                int maxWidth = 84 - Math.min(3, numTabs) * TAB.length();
                if (value.length() <= maxWidth) {
                    // Single value for path: don't add newlines.
                    appendQuoted(value, quote, out);
                    wasSingular = true;
                } else {
                    // Value too long to fit in one line, so wrap.
                    final String pad = Utility.repeat(TAB, numTabs);
                    out.append('\n');
                    int end;
                    for (int i = 0; i < value.length(); i = end) {
                        end = goodBreak(value, i + maxWidth);
                        String part = value.substring(i, end);
                        out.append(pad);
                        appendQuoted(part, quote, out).append('\n');
                    }
                }
            } else {
                // Only one array for the rbPath, so don't add an extra set of braces.
                final String pad = Utility.repeat(TAB, numTabs);
                out.append('\n');
                appendArray(pad, firstArray, quote, out);
            }
        } else {
            final String pad = Utility.repeat(TAB, numTabs);
            out.append('\n');
            for (String[] valueArray : values) {
                if (valueArray.length == 1) {
                    // Single-value array: print normally.
                    appendArray(pad, valueArray, quote, out);
                } else {
                    // Enclose this array in braces to separate it from other
                    // values.
                    out.append(pad).append("{\n");
                    appendArray(pad + TAB, valueArray, quote, out);
                    out.append(pad).append("}\n");
                }
            }
        }
        return wasSingular;
    }
    
    private PrintWriter appendArray(String padding, String[] valueArray, boolean quote, PrintWriter out) {
        for (String value : valueArray) {
            out.append(padding);
            appendQuoted(quoteInside(value), quote, out).append(",\n");
        }
        return out;
    }

    private PrintWriter appendQuoted(String value, boolean quote, PrintWriter out) {
        if (quote) {
            return out.append('"').append(value).append('"');
        } else {
            return out.append(enumMap.containsKey(value) ? enumMap.get(value) : value);
        }
    }

    /**
     * Can a string be broken here? If not, backup until we can.
     * 
     * @param quoted
     * @param end
     * @return
     */
    private static int goodBreak(String quoted, int end) {
        if (end > quoted.length()) {
            return quoted.length();
        }
        // Don't break escaped Unicode characters.
        for (int i = end - 1; i > end - 6; i--) {
            if (quoted.charAt(i) == '\\') {
                if (quoted.charAt(i + 1) == 'u') {
                    return i;
                }
                break;
            }
        }
        while (end > 0) {
            char ch = quoted.charAt(end - 1);
            if (ch != '\\' && (ch < '\uD800' || ch > '\uDFFF')) {
                break;
            }
            --end;
        }
        return end;
    }

    /**
     * Get items
     * 
     * @return
     */
    public Set<Entry<String, List<String[]>>> entrySet() {
        return rbPathToValues.entrySet();
    }

    /**
     * Get items
     * 
     * @return
     */
    public Set<String> keySet() {
        return rbPathToValues.keySet();
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

    public void fillFromCLDR(Factory factory, String locale, Set<String> cantConvert) {
        clear();
        Set<String> deprecatedTerritories = getDeprecatedTerritories();

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

            // Add rb path.
            Output<Finder> matcherFound = new Output<Finder>();
            RegexResult regexResult = matchXPath(pathConverter, cldr, xpath, matcherFound, cantConvert);
            if (regexResult == null) continue;
            String[] arguments = matcherFound.value.getInfo();
            for (PathValueInfo info : regexResult) {
                String rbPath = info.processRbPath(arguments);
                addSubMap(rbPath, pathValueMap);
                // The immediate parent of every path should also exist.
                addSubMap(rbPath.substring(0, rbPath.lastIndexOf('/')), pathValueMap);
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
        usedSpecial = false;
        if (specialFactory != null && specialFactory.getAvailable().contains(locale)) {
            usedSpecial = true;
            CLDRFile specialCldrFile = specialFactory.make(locale, false);
            for (String xpath : specialCldrFile) {
                System.out.println(xpath + " " + specialCldrFile.getFullXPath(xpath));
                addMatchesForPath(xpath, specialCldrFile, pathValueMap, false);
            }
        }

        // Convert values to final data structure.
        for (String rbPath : pathValueMap.keySet()) {
            List<String[]> values = new ArrayList<String[]>(pathValueMap.get(rbPath).values());
            rbPathToValues.put(rbPath, values);
        }
        // Hacks
        hackAddExtras(resolvedCldr, locale);
    }
    
    private void addMatchesForPath(String xpath, CLDRFile cldrFile,
            Map<String, Map<String, String[]>> pathValueMap, boolean selective) {
        Output<Finder> matcher = new Output<Finder>();
        RegexResult regexResult = matchXPath(pathConverter,
            cldrFile, xpath, matcher, null);
        if (regexResult == null) return;
        String[] arguments = matcher.value.getInfo();
        if (!regexResult.argumentsMatch(cldrFile, arguments)) return;
        for (PathValueInfo info : regexResult) {
            String rbPath = info.processRbPath(arguments);
            // Don't add additional paths at this stage.
            if (selective && !pathValueMap.containsKey(rbPath)) continue;
            Map<String, String[]> valueMap = addSubMap(rbPath, pathValueMap);
            String[] values = info.processValues(arguments, cldrFile, xpath);
            valueMap.put(xpath, values);
        }
    }
    
    private Map<String, String[]> addSubMap(String key, Map<String, Map<String, String[]>> pathValueMap) {
        Map<String, String[]> valueMap = pathValueMap.get(key);
        if (valueMap == null) {
            valueMap = new TreeMap<String, String[]>(SpecialLDMLComparator);
            pathValueMap.put(key, valueMap);
        }
        return valueMap;
    }

    static Matcher VERSION_MATCHER = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$").matcher("");

    private static Map<String, Set<String>> localeScripts = new HashMap<String, Set<String>>();

    /**
     * Adds all mappings that couldn't be represented in the ldml2icu.txt file.
     * @param cldrResolved
     * @param locale
     */
    private void hackAddExtras(CLDRFile cldrResolved, String locale) {
        // Specify parent of non-language locales.
        String path;
        String parent = supplementalDataInfo.getExplicitParentLocale(locale);
        if (parent != null) {
            path = "/%%Parent";
            add(path, parent);
        }
        
        // <version number="$Revision: 5806 $"/>
        String versionPath = cldrResolved.getFullXPath("//ldml/identity/version");
        if (!VERSION_MATCHER.reset(versionPath).find()) {
            int failPoint = RegexUtilities.findMismatch(VERSION_MATCHER, versionPath);
            String show = versionPath.substring(0, failPoint) + "☹" + versionPath.substring(failPoint);
            throw new IllegalArgumentException("no version match with: " + show);
        }
        int versionNum = Integer.parseInt(VERSION_MATCHER.group(1));
        String versionValue = "2.0." + (versionNum / 100) + "." + (versionNum % 100);
        path = "/Version";
        add(path, versionValue);

        String localeID = cldrResolved.getLocaleID();

        // PaperSize:intvector{ 279, 216, }
        path = "/PaperSize:intvector";
        String paperType = calculateTypeToAdd(localeID, MeasurementType.paperSize);
        if (paperType == null) {
            // do nothing
        } else if (paperType.equals("A4")) {
            add(path, new String[]{"297", "210"});
        } else if (paperType.equals("US-Letter")) {
            add(path, new String[]{"279", "216"});
        } else {
            throw new IllegalArgumentException("Unknown paper type");
        }

        // MeasurementSystem:int{1}
        path = "/MeasurementSystem:int";
        String measurementSystem = calculateTypeToAdd(localeID, MeasurementType.measurementSystem);
        if (measurementSystem == null) {
            // do nothing
        } else if (measurementSystem.equals("metric")) {
            add(path, "0");
        } else if (measurementSystem.equals("US")) {
            add(path, "1");
        } else {
            throw new IllegalArgumentException("Unknown measurement system");
        }
    }
    
    private String calculateTypeToAdd(String localeID, MeasurementType measurementType) {
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

    private String getMeasurement(String localeID, MeasurementType measurementType) {
        String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();
        Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo.getTerritoryMeasurementData();
        Map<String, String> typeMap = regionMeasurementData.get(measurementType);
        return typeMap.get(region);
    }

    private static String getStringValue(CLDRFile cldrFile, String xpath) {
        String value = cldrFile.getStringValue(xpath);
        if (value == null) value = getFallbackPaths().get(xpath);
        return value;
    }

    private static Map<String, String> fallbackPaths;
    private static Map<String, String> getFallbackPaths() {
        if (fallbackPaths == null) {
            fallbackPaths = loadMapFromFile("ldml2icu_fallback_paths.txt");
        }
        return fallbackPaths;
    }
    
    /**
     * Maps ICU paths to the directories they should end up in.
     */
    private Map<String, String> dirMapping;
    private Map<String, String> getDirMapping() {
        if (dirMapping == null) {
            dirMapping = loadMapFromFile("ldml2icu_dir_mapping.txt");
        }
        return dirMapping;
    }
    
    private static Map<String, String> loadMapFromFile(String filename) {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader reader = FileUtilities.openFile(LDMLConverter.class, filename);
        String line;
        try {
            int lineNum = 1;
            while((line = reader.readLine()) != null) {
                if (line.length() == 0 || line.startsWith("#")) continue;
                String[] content = line.split(SEMI.toString());
                if (content.length != 2) {
                    throw new IllegalArgumentException("Invalid syntax of " + filename + " at line " + lineNum);
                }
                map.put(content[0], content[1]);
                lineNum++;
            }
        } catch(IOException e) {
            System.err.println("Failed to read fallback file.");
            e.printStackTrace();
        }
        return map;

    }

    protected static final Pattern DRAFT_PATTERN = Pattern.compile("\\[@draft=\"\\w+\"]");

    /**
     * @param cldr
     * @param path
     * @param matcherFound
     * @param cantConvert
     * @return the result of converting an xpath into an ICU-style path
     */
    private static RegexResult matchXPath(RegexLookup<RegexResult> lookup,
            CLDRFile cldr, String path,
            Output<Finder> matcherFound, Set<String> cantConvert) {
        String fullPath = cldr.getFullXPath(path);
        fullPath = fullPath == null ? path : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
        RegexResult result = lookup.get(fullPath, null, null, matcherFound, null);
        // Cache patterns for later analysis.
        patternCache.add(matcherFound.toString());
        if (result == null && cantConvert != null) {
            cantConvert.add(fullPath);
        }
        return result;
    }

    /**
     * The default tab indent (actually spaces)
     */
    final static String TAB = "    ";

    /**
     * Fix characters inside strings.
     * 
     * @param item
     * @return
     */
    private static String quoteInside(String item) {
        if (item.contains("\"")) {
            item = item.replace("\"", "\\\"");
        }
        return item;
    }

    /**
     * find the initial labels (from a path) that are identical.
     * 
     * @param item
     * @return
     */
    private static int getCommon(String[] lastLabels, String[] labels) {
        int min = Math.min(lastLabels.length, labels.length);
        int i;
        for (i = 0; i < min; ++i) {
            if (!lastLabels[i].equals(labels[i])) {
                return i - 1;
            }
        }
        return i - 1;
    }

    private static String processString(String value, String[] arguments) {
        if (value == null) {
            return null;
        }
        try {
            return pathConverter.replace(value, arguments);
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Error while filling out arguments in " + value + " with " + Arrays.asList(arguments), e);
        }
    }

    private static boolean isIntRbPath(String rbPath) {
        return rbPath.endsWith(":int") || rbPath.endsWith(":intvector");
    }

    private void usage() {
        System.out.println(
            "\nUsage: LDML2ICUConverter [OPTIONS] [FILES]\n" +
            "This program is used to convert LDML files to ICU data text files.\n" +
            "Please refer to the following options. Options are not case sensitive.\n" +
            "Options:\n" +
            "-s or --sourcedir          source directory for files followed by path, " +
                                       "default is current directory.\n" +
            "-d or --destdir            destination directory, followed by the path, "+
                                       "default is current directory.\n" +
            "-p or --specialsdir        source directory for files containing special data " +
                                       "followed by the path. None if not specified\n" +
            "-m or --suplementaldir     source directory for finding the supplemental data.\n" +
            "-k or --keeptogether       write locale data to one file instead of splitting.\n" +
            "-h or -? or --help         this usage text.\n" +
            "-v or --verbose            print out verbose output.\n" +
            "example: org.unicode.cldr.drafts.LDMLConverter -s xxx -d yyy en.xml");
        System.exit(-1);
    }
    
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int SPECIALSDIR = 4;
    private static final int SUPPLEMENTALDIR = 5;
    private static final int VERBOSE = 6;
    // Debugging: doesn't split up locale into separate directories.
    private static final int KEEP_TOGETHER = 7;
    
    private boolean keepTogether = false;
    private void processArgs(String[] args) {
        UOption[] options = new UOption[] {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR(),
            UOption.DESTDIR(),
            UOption.create("specialsdir", 'p', UOption.REQUIRES_ARG),
            UOption.create("supplementaldir", 'm', UOption.REQUIRES_ARG),
            UOption.VERBOSE(),
            UOption.create("keeptogether", 'k', UOption.NO_ARG)
        };

        int remainingArgs = 0;
        try {
            remainingArgs = UOption.parseArgs(args, options);
        } catch (Exception e) {
            System.out.println("Error parsing args: " + e.getMessage());
            e.printStackTrace();
            usage();
        }
        if (args.length == 0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }
        String filePattern = remainingArgs > 0 ? args[0] : ".*";
        if (options[SOURCEDIR].doesOccur) {
            factory = Factory.make(options[SOURCEDIR].value, filePattern);
        }
        destinationDir = options[DESTDIR].doesOccur ? options[DESTDIR].value : ".";
        if (options[SPECIALSDIR].doesOccur) {
            specialFactory = Factory.make(options[SPECIALSDIR].value, filePattern);
        }
        if (options[SUPPLEMENTALDIR].doesOccur) {
            supplementalDataInfo = SupplementalDataInfo.getInstance(options[SUPPLEMENTALDIR].value);
        }
        keepTogether = options[KEEP_TOGETHER].doesOccur;
    }
    
    private Map<String, Set<String>> mapDirToPaths(Set<String> paths) {
        Map<String, String> dirMapping = getDirMapping();
        Map<String, Set<String>> dirPaths = new HashMap<String, Set<String>>();
        dirPaths.put("locales", new HashSet<String>());
        for (String path : paths) {
            boolean matched = false;
            for (String prefix : dirMapping.keySet()) {
                if (path.startsWith(prefix)) {
                    String dir = dirMapping.get(prefix);
                    Set<String> filteredPaths = dirPaths.get(dir);
                    if (filteredPaths == null) {
                        filteredPaths = new HashSet<String>();
                        dirPaths.put(dir, filteredPaths);
                    }
                    filteredPaths.add(path);
                    matched = true;
                    break;
                }
                if (!matched) {
                    dirPaths.get("locales").add(path);
                }
            }
        }
        return dirPaths;
    }
    
    private void processFiles() throws IOException {
        Set<String> cantConvert = new HashSet<String>(); // use HashSet for speed
        for (String filename : factory.getAvailable()) {
            long time = System.currentTimeMillis();
            fillFromCLDR(factory, filename, cantConvert);
            if (keepTogether) {
                writeRB(rbPathToValues, destinationDir, filename);
            } else {
                Map<String, Set<String>> dirPaths = mapDirToPaths(rbPathToValues.keySet());
                for (String dir : dirPaths.keySet()) {
                    Map<String, List<String[]>> rbValues = new HashMap<String, List<String[]>>();
                    Set<String> paths = dirPaths.get(dir);
                    for (String path : paths) {
                        rbValues.put(path, rbPathToValues.get(path));
                    }
                    String dirPath = destinationDir + '/' + dir;
                    File dirFile = new File(dirPath);
                    if (!dirFile.exists()) dirFile.mkdir();
                    writeRB(rbValues, dirPath, filename);
                }
            }
            System.out.println("Converted " + filename + ".xml in " + (System.currentTimeMillis() - time) + "ms");
        }
    }

    /**
     * In this prototype, just convert one file.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // TODO Handle more than just the main directory.
        long totalTime = System.currentTimeMillis();
        LDMLConverter converter = new LDMLConverter();
        converter.processArgs(args);

        converter.processFiles();
        System.out.println("Total time taken: " + (System.currentTimeMillis() - totalTime));
/*
        // sort the can't-convert strings and print
        for (String unconverted : Builder.with(new TreeSet<String>(CLDRFile.ldmlComparator)).addAll(cantConvert).get()) {
            System.out.println("Can't Convert:\t" + unconverted);
        }
        */
    }
}
