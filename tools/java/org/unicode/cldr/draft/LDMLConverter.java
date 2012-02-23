package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.MeasurementType;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Prototype simpler mechanism for converting to ICU Resource Bundles. The
 * format is almost entirely data-driven instead of having lots of special-case
 * code.
 * 
 * @author markdavis
 */
public class LDMLConverter {
    private static boolean                       DEBUG                = false;
    /**
     * For debugging, set this to match a CLDR path. If there is no match in the Regex lookup, you'll see where it fails.
     */
    private static Matcher                       DEBUG_FAIL_REGEX          = Pattern.compile("intervalFormats.*alias").matcher("");
    private static Matcher                       DEBUG_MATCH_REGEX          = Pattern.compile("dateFormats/dateFormatLength").matcher("");
    private static Pattern CURRENCY_XPATH = Pattern.compile("//ldml/numbers/currencies/currency\\[@type=\"(\\w+)\"]/(displayName|symbol)");
    private static Pattern TERRITORY_XPATH = Pattern.compile("//ldml/localeDisplayNames/territories/territory\\[@type=\"(\\w+)\"]");

    static SupplementalDataInfo                  supplementalDataInfo = SupplementalDataInfo.getInstance();

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

    static final Pattern                         SEMI                 = Pattern.compile("\\s*;\\s*");

    static class PathValueInfo {
        private String rbPath;
        private String value;
        private String key;
        private boolean isFallback;

        public PathValueInfo(String rbPath, String value, String key, boolean isFallback) {
            this.rbPath = rbPath;
            this.value = value;
            this.key = key;
            this.isFallback = isFallback;
        }
        
        public String getRbPath() { return rbPath; }
        
        public String getValue() { return value; }
        
        public String getKey() { return key; }

        public boolean valueIsFallback() { return isFallback; }
        
        @Override
        public String toString() { return rbPath + "=" + value; }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof PathValueInfo) {
                PathValueInfo otherInfo = (PathValueInfo)o;
                return rbPath.equals(otherInfo.rbPath)
                        && value.equals(otherInfo.value)
                        && key.equals(otherInfo.key)
                        && isFallback == otherInfo.isFallback;
            } else {
                return false;
            }
        }

        public PathValueInfo process(String[] arguments) {
            String rbPath = processString(this.rbPath, arguments);
            String value = processString(this.value, arguments);
            String key = processString(this.key, arguments);
            return new PathValueInfo(rbPath, value, key, isFallback);
        }
    }

    static class RegexResult implements Iterable<PathValueInfo> {
        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^\\$(\\d+)=(//.*)");
        final String         dir;
        private Set<PathValueInfo> unprocessed;
        private Map<Integer, String> requiredArgs;

        public RegexResult(String source) {
            try {
                String[] parts = SEMI.split(source);
                dir = parts[0];
                unprocessed = new HashSet<PathValueInfo>();
                requiredArgs = new HashMap<Integer, String>();
                String rbPath = parts[1];
                String value = null;
                String key = null;
                boolean isFallback = false;
                if (parts.length > 2) {
                    String[] rawValues = parts[2].split(",");
                    for (String rawValue : rawValues) {
                        Matcher argMatcher = ARGUMENT_PATTERN.matcher(rawValue);
                        if (rawValue.startsWith("values=")) {
                            value = rawValue.substring(7);
                        } else if (rawValue.startsWith("fallback")) {
                            isFallback = true;
                        } else if (rawValue.startsWith("key=")) {
                            key = rawValue.substring(4);
                        } else if (argMatcher.matches()) {
                            requiredArgs.put(Integer.parseInt(argMatcher.group(1)),
                                    argMatcher.group(2));
                        }
                    }
                }
                unprocessed.add(new PathValueInfo(rbPath, value, key, isFallback));
            } catch (Exception e) {
                throw new IllegalArgumentException("Must be of form directory ; path: "
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
        
        public List<PathValueInfo> processInfo(String[] arguments) {
            List<PathValueInfo> processed = new ArrayList<PathValueInfo>();
            for (PathValueInfo info : unprocessed) {
                processed.add(info.process(arguments));
            }
            return processed;
        }

        public boolean argumentsMatch(CLDRFile file, String[] arguments) {
            for (int argNum : requiredArgs.keySet()) {
                if (arguments.length <= argNum) {
                    throw new IllegalArgumentException("Argument " + argNum + " mising");
                }
                String argFromCldr = file.getStringValue(requiredArgs.get(argNum));
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
     * Cache the results for debugging
     */
    static final class RegexResultCacheItem {
        public RegexResultCacheItem(RegexResult regexResult, Output<Finder> matcherFound) {
            this.regexResult = regexResult;
            this.pattern = matcherFound.value.toString();
        }
        final RegexResult regexResult;
        final String pattern;
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
        private final Pattern MONTH_PATTERN = Pattern.compile(".*/month\\[@type=\"(\\d+)\"](\\[@yeartype=\"leap\"])?$");
        /**
         * Reverse the ordering of the following:
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/displayName ; curr ; /Currencies/$1
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/symbol ; curr ; /Currencies/$1
         * and the following (time/date)
         * //ldml/dates/calendars/calendar[@type="([^"]*)"]/(dateFormats|dateTimeFormats|timeFormats)/(?:[^/\[]*)[@type="([^"]*)"]/(?:[^/\[]*)[@type="([^"]*)"]/.* ; locales ; /calendar/$1/DateTimePatterns
         */
        @Override
        public int compare(String arg0, String arg1) {
            if (arg0.startsWith("//ldml/numbers/currencies/currency")
                    && arg1.startsWith("//ldml/numbers/currencies/currency")) {
                int last0 = arg0.lastIndexOf('/');
                int last1 = arg1.lastIndexOf('/');
                if (last0 == last1 && arg0.regionMatches(0, arg1, 0, last1)) {
                    return -arg0.substring(last0, arg0.length()).compareTo(
                            arg1.substring(last1, arg1.length()));
                }
            }
            if (arg0.startsWith("//ldml/dates/calendars/calendar")
                    && arg1.startsWith("//ldml/dates/calendars/calendar")
            ) {
                if (DATE_OR_TIME_FORMAT.reset(arg0).find()) {
                    int start0 = DATE_OR_TIME_FORMAT.start();
                    if (DATE_OR_TIME_FORMAT.reset(arg1).find()) {
                        int start1 = DATE_OR_TIME_FORMAT.start();
                        int end1 = DATE_OR_TIME_FORMAT.end();
                        if (start0 == start1 && arg0.regionMatches(0, arg1, 0, start1)
                                && !arg0.regionMatches(0, arg1, 0, end1)) {
                            return -arg0.substring(start0, arg0.length()).compareTo(
                                    arg1.substring(start1, arg1.length()));
                        }
                    }
                }
            }

            // Sort leap year types after normal month types.
            Matcher matcher0 = MONTH_PATTERN.matcher(arg0);
            if (matcher0.matches()) {
                Matcher matcher1 = MONTH_PATTERN.matcher(arg1);
                if (matcher1.matches() && matcher0.group(2) != matcher1.group(2)) {
                    return matcher0.group(2) == null && matcher1.group(2) != null ? -1 : 1;
                }
            }

            return CLDRFile.ldmlComparator.compare((String) arg0, (String) arg1);
        }
    };

    /**
     * Loads the data in from a file. That file is of the form cldrPath ; rbPath
     */
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
    
    // TODO(jchye): remove these two maps.
    Map<String, Map<String, List<String>>> dir2path2values = new HashMap<String, Map<String, List<String>>>();
    Map<String, String>                    cldrPath2rbPath = new HashMap<String, String>();

    private Map<String,List<String>> rbPathToValues;
    private Map<String,Set<String>> dirToPaths;

    public LDMLConverter() {
        rbPathToValues = new HashMap<String,List<String>>();
        dirToPaths = new HashMap<String, Set<String>>();
    }

    public void clear() {
        dirToPaths.clear();
        rbPathToValues.clear();
        dir2path2values.clear();
        cldrPath2rbPath.clear();
    }

    public String getRbPath(String cldrPath) {
        return cldrPath2rbPath.get(cldrPath);
    }

    public Set<String> getCldrPaths() {
        return cldrPath2rbPath.keySet();
    }

    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     * 
     * @param directory the directory that the path and value will end up in
     * @param file
     * @param path
     * @param value
     * @return
     */
    void add(String directory, String file, String path, String value) {
        if (file != null) {
            path = "/" + file + path;
        }
        if (DEBUG) {
            System.out.println("+++\t" + path + "\t" + value);
        }
        Map<String, List<String>> path2values = dir2path2values.get(directory);
        if (path2values == null) {
            dir2path2values.put(directory, path2values = new TreeMap<String, List<String>>(PATH_COMPARATOR));
        }
        List<String> list = path2values.get(path);
        if (list == null) {
            path2values.put(path, list = new ArrayList<String>(1));
        }
        list.add(value);
        // special hack for date formats.

    }

    /**
     * Write a file in ICU format. LDML2ICUConverter currently has some
     * funny formatting in a few cases; don't try to match everything.
     * 
     * @param dirPath
     * @param file
     */
    void writeRB(String dirPath, String file) {
        try {
            boolean wasSingular = false;
            String[] replacements = { "%file%", file };
            for (Entry<String, Set<String>> dirInfo : dirToPaths.entrySet()) {
                String dir = dirInfo.getKey();
                PrintWriter out = BagFormatter.openUTF8Writer(dirPath + "/" + dir, file + ".txt");
                out.write('\uFEFF');
                FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
                Set<String> paths = dirInfo.getValue();
                String[] lastLabels = new String[] {};

                out.append(file);
                for (String path : paths) {
                    List<String> values = rbPathToValues.get(path);
                    if (values == null) {
                        System.out.println("WARNING: path has null values:" + path);
                        continue;
                    }

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
                    int maxWidth = 76;
                    boolean quote = !isIntRbPath(path);
                    if (values.size() == 1) {
                        String value = getValue(values.iterator().next(), quote);
                        if (value.length() <= maxWidth) {
                            appendQuoted(value, quote, out);
                            wasSingular = true;
                        } else {
                            final String pad = Utility.repeat(TAB, labels.length);
                            out.append('\n');
                            int end;
                            for (int i = 0; i < value.length(); i = end) {
                                end = goodBreak(value, i + maxWidth);
                                String part = value.substring(i, end);
                                out.append(pad);
                                appendQuoted(part, quote, out).append('\n');
                            }
                            wasSingular = false;
                        }
                    } else {
                        final String pad = Utility.repeat(TAB, labels.length);
                        out.append('\n');
                        for (String item : values) {
                            out.append(pad);
                            item = getValue(item, quote);
                            appendQuoted(item, quote, out).append(",\n");
                        }
                        wasSingular = false;
                    }
                    out.flush();
                    lastLabels = labels;
                }
                // finish last
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getValue(String value, boolean quote) {
        if (quote) {
            value = quoteInside(value);
        } else if (enumMap.containsKey(value)){
            value = enumMap.get(value);
        }
        return value;
    }

    private PrintWriter appendQuoted(String value, boolean quote, PrintWriter out) {
        if (quote) {
            return out.append('"').append(value).append('"');
        } else {
            return out.append(value);
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
    public Set<Entry<String, Map<String, List<String>>>> entrySet() {
        return dir2path2values.entrySet();
    }

    public Set<String> directories() {
        return dirToPaths.keySet();
    }
    
    // sorter and primary key for each value in an RB path.
    private class RbValueKey implements Comparable<RbValueKey> {
        private String xpath;
        private String key;
        public RbValueKey(String xpath, String key) {
            this.xpath = xpath;
            this.key = key == null ? xpath : key;
        }
        
        public RbValueKey(String xpath) {
            this(xpath, null);
        }
        
        @Override
        public int compareTo(RbValueKey other) {
            int keyCompare;
            try {
                // Compare keys according to their numerical value.
                int keyNum = Integer.parseInt(key);
                int otherNum = Integer.parseInt(other.key);
                keyCompare = keyNum - otherNum;
            } catch (NumberFormatException nfe) {
                // At least one of the keys is not a number.
                keyCompare = key.compareTo(other.key);
            }
            // Keys are unique.
            if (keyCompare == 0) return 0;
            // The same xpath is allowed for multiple keys.
            int xpathCompare = SpecialLDMLComparator.compare(this.xpath, other.xpath);
            return xpathCompare == 0 ? keyCompare : xpathCompare;
        }
    }

    private static Set<String> deprecatedTerritoriesToInclude = Builder.with(new HashSet<String>())
            .add("062").add("172").add("200").add("830").add("AN").add("CS").get();

    public void fillFromCLDR(Factory factory, String locale, Set<String> cantConvert) {
        CLDRFile cldr = factory.make(locale, false);
        clear();
        // Variables for hacks
        Set<String> currencyPaths = new HashSet<String>();
        Set<String> deprecatedTerritories = supplementalDataInfo.getLocaleAliasInfo().get("territory").keySet();
        deprecatedTerritories.removeAll(deprecatedTerritoriesToInclude);

        List<String> sortedPaths = new ArrayList<String>();
        CollectionUtilities.addAll(cldr.iterator(), sortedPaths);
        Collections.sort(sortedPaths, SpecialLDMLComparator);
        Map<Finder, RegexResult> fallbacks = new HashMap<Finder, RegexResult>();
        // First pass through the CLDRFile to get icu paths and values.
        Map<String, Map<RbValueKey,String>> pathValueMap = new HashMap<String,Map<RbValueKey,String>>();        
        for (String xpath : sortedPaths) {
            // Misc hacks.
            Matcher matcher = TERRITORY_XPATH.matcher(xpath);
            if (matcher.matches()) {
                String country = matcher.group(1);
                if (deprecatedTerritories.contains(country)) {
                    continue;
                }
            }
            matcher = CURRENCY_XPATH.matcher(xpath);
            if (matcher.matches()) {
                currencyPaths.add(matcher.group(1));
            }

            // Regular conversions.
            String fullPath = myGetFullXPath(cldr, xpath);
            List<Finder> matchersFound = new ArrayList<Finder>();
            List<RegexResult> resultList = getConvertedValues(cldr, fullPath, matchersFound, cantConvert);
            if (resultList == null) continue;
            for (int i = 0; i < resultList.size(); i++) {
                RegexResult regexResult = resultList.get(i);
                Finder pathMatcher = matchersFound.get(i);
                String[] arguments = pathMatcher.getInfo();
                if (!regexResult.argumentsMatch(cldr, arguments)) continue;
                // Cache pattern for later analysis.
                patternCache.add(pathMatcher.toString());
                List<PathValueInfo> processedInfo = regexResult.processInfo(arguments);
                for (PathValueInfo info : processedInfo) {
                    String rbPath = info.getRbPath();
                    String value = info.getValue();
                    String key = info.getKey() == null ? xpath : info.getKey();
                    // TODO: can we move this anywhere else?
                    addPathToDir(regexResult.dir, rbPath);
                    if (info.valueIsFallback()) {
                        fallbacks.put(pathMatcher, regexResult);
                        continue;
                    }
                    
                    if (value != null) {
                        String[] valueArray = value.split("\\s+");
                        if (valueArray.length > 1) {
                            for (int j = 0; j < valueArray.length; j++) {
                                RbValueKey rbKey = new RbValueKey(xpath, j + "");
                                value = valueArray[j];
                                addToMapInMap(rbPath, rbKey, value, pathValueMap);
                            }
                        } else {
                            // Get value of xpath
                            if (value.startsWith("//ldml/")) {
                                value = cldr.getStringValue(value);
                            }
                            RbValueKey rbKey = new RbValueKey(xpath, key);
                            addToMapInMap(rbPath, rbKey, value, pathValueMap);
                        }
                    } else {
                        value = cldr.getStringValue(xpath);
                        RbValueKey rbKey = new RbValueKey(xpath, key);
                        addToMapInMap(rbPath, rbKey, value, pathValueMap);
                    }
                }
            }
        }

        // Hack to get fallback currency information from the resolved CLDRFile.
        CLDRFile resolvedCldr = factory.make(locale, true);
        sortedPaths.clear();
        CollectionUtilities.addAll(resolvedCldr.iterator(), sortedPaths);
        Collections.sort(sortedPaths, SpecialLDMLComparator);
        for (String country : currencyPaths) {
            String rbPath = "/Currencies/" + country;
            String symbolPath = "//ldml/numbers/currencies/currency[@type=\"" + country + "\"]/symbol";
            String value = resolvedCldr.getStringValue(symbolPath);
            if (value == null) value = country;
            addToMapInMap(rbPath, new RbValueKey(symbolPath), value, pathValueMap);
            
            String displayNamePath = "//ldml/numbers/currencies/currency[@type=\"" + country + "\"]/displayName";
            value = resolvedCldr.getStringValue(displayNamePath);
            if (value == null) value = country;
            addToMapInMap(rbPath, new RbValueKey(displayNamePath), value, pathValueMap);
        }
        
        // Fill out alias fallbacks.
        RegexLookup<RegexResult> resolvedLookup = new RegexLookup<RegexResult>();
        for (Finder finder : fallbacks.keySet()) {
            resolvedLookup.add(finder, fallbacks.get(finder));
        }
        for (String xpath : sortedPaths) {
            List<Finder> matchers = new ArrayList<Finder>();
            List<RegexResult> resultList = resolvedLookup.getAll(xpath, null, matchers, null);
            for (int i = 0; i < resultList.size(); i++) {
                RegexResult regexResult = resultList.get(i);
                String[] arguments = matchers.get(i).getInfo();
                List<PathValueInfo> processedInfo = regexResult.processInfo(arguments);
                for (PathValueInfo info : processedInfo) {
                    if (!info.valueIsFallback()) continue;
                    if (dirToPaths.get(regexResult.dir).contains(info.getRbPath())) {  // Don't add additional paths at this stage
                        RbValueKey rbKey = new RbValueKey(xpath, info.getKey());
                        addToMapInMap(info.getRbPath(), rbKey, resolvedCldr.getStringValue(xpath), pathValueMap);
                    }
                }
            }
        }

        // Convert value maps into list form.
        rbPathToValues.clear();
        for (String rbPath: pathValueMap.keySet()) {
            Map<RbValueKey,String> values = pathValueMap.get(rbPath);
            rbPathToValues.put(rbPath, new ArrayList<String>(values.values()));
        }

        // Hacks
        hackAddExtras(resolvedCldr, locale, rbPathToValues);
    }
    
    private void addToMapInMap(String key, RbValueKey subkey, String value, Map<String, Map<RbValueKey, String>> map) {
        Map<RbValueKey, String> subMap = map.get(key);
        if (subMap == null) {
            subMap = new TreeMap<RbValueKey, String>();
            map.put(key, subMap);
        }
        // Don't overwrite older values.
        if (!subMap.containsKey(subkey)) {
            subMap.put(subkey, value);
        }
    }
    
    private void addToMapOfLists(String key, String value, Map<String,List<String>> map) {
        List<String> list = map.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(key, list);
        }
        list.add(value);
    }

    private static final Pattern DRAFT_PATTERN = Pattern.compile("\\[@draft=\"\\w+\"]");
    private static String myGetFullXPath(CLDRFile cldr, String path) {
        String fullPath = cldr.getFullXPath(path);
        return fullPath == null ? path : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
    }
    
    private void addPathToDir(String dir, String path) {
        Set<String> dirPaths = dirToPaths.get(dir);
        if (dirPaths == null) {
            dirPaths = new TreeSet<String>(PATH_COMPARATOR);
            dirToPaths.put(dir, dirPaths);
        }
        dirPaths.add(path);
    }

    /**
     * Adds all mappings that couldn't be represented in the ldml2icu.txt file.
     * @param cldrResolved
     * @param locale
     */
    private void hackAddExtras(CLDRFile cldrResolved, String locale, Map<String,List<String>> rbPathToValues) {
        UnicodeSet s = cldrResolved.getExemplarSet("", WinningChoice.WINNING);
        BitSet set = new BitSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(s); it.next();) {
            int script = UScript.getScript(it.codepoint);
            set.set(script);
        }
        set.clear(UScript.COMMON);
        set.clear(UScript.INHERITED);
        String path = "/LocaleScript";
        String localeDir = "locales";
        // TODO(jchye): Uncomment this bit when we've verified that the output
        // from this class matches LDML2ICUConverter's exactly.
//            addPathToDir(locales, path);
//            if (set.isEmpty()) {
//                addToMapOfLists(path, "Zyyy", rbPathToValues);
//            } else {
//                for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
//                    // operate on index i here
//                    String name = UScript.getShortName(i);
//                    addToMapOfLists(path, name, rbPathToValues);
//                }
//            }

        // Specify parent of non-language locales.
        String parent = supplementalDataInfo.getExplicitParentLocale(locale);
        if (parent != null && !parent.equals("root")) {
            path = "/%%Parent";
            addPathToDir(localeDir, path);
            addToMapOfLists(path, parent, rbPathToValues);
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
        for (String directory : directories()) {
            addPathToDir(directory, path);
        }
        addToMapOfLists(path, versionValue, rbPathToValues);

        String localeID = cldrResolved.getLocaleID();
        String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();

        // PaperSize:intvector{ 279, 216, }
        Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo.getTerritoryMeasurementData();
        Map<String, String> paperSizeMap = regionMeasurementData.get(MeasurementType.paperSize);
        String paperType = paperSizeMap.get(region);
        path = "/PaperSize:intvector";
        if (paperType == null) {
            // do nothing
        } else if (paperType.equals("A4")) {
            addPathToDir(localeDir, path);
            addToMapOfLists(path, "297", rbPathToValues);
            addToMapOfLists(path, "210", rbPathToValues);
        } else if (paperType.equals("US-Letter")) {
            addPathToDir(localeDir, path);
            addToMapOfLists(path, "279", rbPathToValues);
            addToMapOfLists(path, "216", rbPathToValues);
        } else {
            throw new IllegalArgumentException("Unknown paper type");
        }

        // MeasurementSystem:int{1}
        Map<String, String> measurementSystemMap = regionMeasurementData.get(MeasurementType.measurementSystem);
        String measurementSystem = measurementSystemMap.get(region);
        path = "/MeasurementSystem:int";
        if (measurementSystem == null) {
            // do nothing
        } else if (measurementSystem.equals("metric")) {
            addPathToDir(localeDir, path);
            addToMapOfLists(path, "0", rbPathToValues);
        } else if (measurementSystem.equals("US")) {
            addPathToDir(localeDir, path);
            addToMapOfLists(path, "1", rbPathToValues);
        } else {
            throw new IllegalArgumentException("Unknown measurement system");
        }
    }

    static Matcher VERSION_MATCHER = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$").matcher("");

    /**
     * @param cldr
     * @param path
     * @param arguments
     * @param matcherFound
     * @param cantConvert
     * @return the result of converting an xpath into an ICU-style path
     */
    private static List<RegexResult> getConvertedValues(CLDRFile cldr, String path, List<Finder> matchersFound,
            Set<String> cantConvert) {
        String fullPath = myGetFullXPath(cldr, path);
        if (fullPath.contains("[@draft")) {
            // [@draft="contributed"] should already have been removed.
            return null;
        }
        List<String> errors = DEBUG_FAIL_REGEX.reset(path).find() ? new ArrayList<String>() : null;
        List<RegexResult> resultList = pathConverter.getAll(path, null, matchersFound, errors);
        if (resultList.size() == 0) {
            if (cantConvert != null) {
                cantConvert.add(path);
            }
            if (errors != null) {
                System.out.println("\tDEBUG\t" + CollectionUtilities.join(errors, "\n\tDEBUG\t"));
            }
            return null;
        } else if (DEBUG_MATCH_REGEX.reset(path).find()) {
            System.out.println("Matching:\t" + path
                    + "\n\t\twith\t" + matchersFound
            );
        }
        return resultList;
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

    public static boolean isSpecialRbPath(String path) {
        return path.endsWith(":alias") || isIntRbPath(path);
    }

    /**
     * In this prototype, just convert one file.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // TODO Handle more than just the main directory.
        // TODO(jchye): Add commandline args.
        String fileRegexString = args.length == 0 ? "(fr)" : args[0];
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, fileRegexString);

        LDMLConverter converter = new LDMLConverter();
        Set<String> cantConvert = new HashSet(); // use HashSet for speed

        for (String file : factory.getAvailable()) {
            converter.fillFromCLDR(factory, file, cantConvert);
            converter.writeRB("/Users/jchye/tweaks/newicu/", file);
        }

        // sort the can't-convert strings and print
        for (String unconverted : Builder.with(new TreeSet<String>(CLDRFile.ldmlComparator)).addAll(cantConvert).get()) {
            System.out.println("Can't Convert:\t" + unconverted);
        }
        
        Map<String, RegexResult> outputUnmatched = new TreeMap<String, RegexResult>();
        for (Entry<String, RegexResult> patternRegexResult : pathConverter.getUnmatchedPatterns(patternCache, outputUnmatched).entrySet()) {
            System.out.println(patternRegexResult.getKey() + "\t" + patternRegexResult.getValue().unprocessed + "\t" + "***Unmatched***");
        }
    }
}
