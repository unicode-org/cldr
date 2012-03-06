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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.CldrUtility;
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
    private static Matcher                       DEBUG_MATCH_REGEX          = Pattern.compile("currencyFormatLength").matcher("");

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
                        value = cldrFile.getStringValue(xpath);
                    } else if (value.startsWith("//ldml/")) {
                        value = cldrFile.getStringValue(value);
                    }
                    result[i] = value;
                }
            } else {
                result = new String[] { cldrFile.getStringValue(xpath) };
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
        final String         dir;
        private Set<PathValueInfo> unprocessed;
        private Map<Integer, String> requiredArgs;

        public RegexResult(String source) {
            try {
                String[] parts = SEMI.split(source);
                dir = parts[0];
                String rbPath = parts[1];
                String value = null;
                requiredArgs = new HashMap<Integer, String>();
                if (parts.length > 2) {
                    String[] rawValues = parts[2].split(",");
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
        @SuppressWarnings("unchecked")
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

            return CLDRFile.ldmlComparator.compare(arg0, arg1);
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
        add(path, value.split("\\s+"));
    }

    /**
     * Write a file in ICU format. LDML2ICUConverter currently has some
     * funny formatting in a few cases; don't try to match everything.
     * 
     * @param dirPath
     * @param locale
     */
    void writeRB(String dirPath, String locale) throws IOException {
        boolean wasSingular = false;
        String[] replacements = { "%file%", locale };
        PrintWriter out = BagFormatter.openUTF8Writer(dirPath, locale + ".txt");
        out.write('\uFEFF');
        FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
        String[] lastLabels = new String[] {};

        out.append(locale);
        List<String> sortedPaths = new ArrayList<String>(rbPathToValues.keySet());
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
            List<String[]> values = rbPathToValues.get(path);
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
                int maxWidth = 84 - numTabs * TAB.length();
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

    private static Set<String> deprecatedTerritoriesToInclude = Builder.with(new HashSet<String>())
            .add("062").add("172").add("200").add("830").add("AN").add("CS").get();
    
    private static Set<String> countriesWithoutCurrencySymbol = Builder.with(new HashSet<String>())
            .add("ALK").add("CNX").add("ILR").add("ISJ").add("MVP").add("SSP")
            .add("XSU").add("XUA").get();

    public void fillFromCLDR(Factory factory, String locale, Set<String> cantConvert) {
        clear();
        // Variables for hacks
        Set<String> deprecatedTerritories = supplementalDataInfo.getLocaleAliasInfo().get("territory").keySet();
        deprecatedTerritories.removeAll(deprecatedTerritoriesToInclude);

        // First pass through the unresolved CLDRFile to get all icu paths.
        CLDRFile cldr = factory.make(locale, false);
        Map<String,Map<String,String[]>> pathValueMap = new HashMap<String,Map<String,String[]>>();
        for (String xpath : cldr) {
            // Misc hacks.
            Matcher matcher = TERRITORY_XPATH.matcher(xpath);
            if (matcher.matches()) {
                String country = matcher.group(1);
                if (deprecatedTerritories.contains(country)) {
                    continue;
                }
            }

            // Regular conversions.
            Output<Finder> matcherFound = new Output<Finder>();
            RegexResult regexResult = getConvertedValues(pathConverter, cldr, xpath, matcherFound, cantConvert);
            if (regexResult == null) continue;
            String[] arguments = matcherFound.value.getInfo();
            if (!regexResult.argumentsMatch(cldr, arguments)) continue;
            for (PathValueInfo info : regexResult) {
                String rbPath = info.processRbPath(arguments);
                Map<String, String[]> valueMap = pathValueMap.get(rbPath);
                if (valueMap == null) {
                    valueMap = new TreeMap<String, String[]>(SpecialLDMLComparator);
                    pathValueMap.put(rbPath, valueMap);
                }
                if (!rbPathToValues.containsKey(rbPath)) {
                    rbPathToValues.put(rbPath, new ArrayList<String[]>());
                }
            }
        }

        // Get all values from the resolved CLDRFile.
        CLDRFile resolvedCldr = factory.make(locale, true);
        List<String> sortedPaths = new ArrayList<String>();
        CollectionUtilities.addAll(resolvedCldr.iterator(), sortedPaths);
        for (String xpath : sortedPaths) {
            Output<Finder> matcher = new Output<Finder>();
            RegexResult regexResult = getConvertedValues(pathConverter,
                resolvedCldr, xpath, matcher, null);
            if (regexResult == null) continue;
            String[] arguments = matcher.value.getInfo();
            if (!regexResult.argumentsMatch(resolvedCldr, arguments)) continue;
            for (PathValueInfo info : regexResult) {
                String rbPath = info.processRbPath(arguments);
                // Don't add additional paths at this stage.
                Map<String, String[]> valueMap = pathValueMap.get(rbPath);
                if (valueMap != null) {
                    String[] values = info.processValues(arguments, resolvedCldr, xpath);
                    valueMap.put(xpath, values);
                }
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

    static Matcher VERSION_MATCHER = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$").matcher("");

    /**
     * Adds all mappings that couldn't be represented in the ldml2icu.txt file.
     * @param cldrResolved
     * @param locale
     */
    private void hackAddExtras(CLDRFile cldrResolved, String locale) {
        // If countries without a currency symbol have a display name in
        // this locale, add the country code as the symbol.
        // TODO: Remove after the symbols have been added to CLDR.
        for (String country : countriesWithoutCurrencySymbol) {
            String rbPath = "/Currencies/" + country;
            List<String[]> values = rbPathToValues.get(rbPath);
            if (values != null) {
                values.add(0, new String[]{country});
            }
        }

        UnicodeSet s = cldrResolved.getExemplarSet("", WinningChoice.WINNING);
        BitSet set = new BitSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(s); it.next();) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                String character = it.getString();
                for (int i = 0; i < character.length(); i++) {
                    set.set(UScript.getScript(character.codePointAt(i)));
                }
            } else {
                set.set(UScript.getScript(it.codepoint));
            }
        }
        set.clear(UScript.COMMON);
        set.clear(UScript.INHERITED);
        String path = "/LocaleScript";
        // TODO(jchye): Uncomment this bit when we've verified that the output
        // from this class matches LDML2ICUConverter's exactly.
//        if (set.isEmpty()) {
//            add(path, "Zyyy");
//        } else {
//            for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
//                // operate on index i here
//                String name = UScript.getShortName(i);
//                add(path, name);
//            }
//        }

        // Specify parent of non-language locales.
        String parent = supplementalDataInfo.getExplicitParentLocale(locale);
        if (parent != null && !parent.equals("root")) {
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
        String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();

        // PaperSize:intvector{ 279, 216, }
        Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo.getTerritoryMeasurementData();
        Map<String, String> paperSizeMap = regionMeasurementData.get(MeasurementType.paperSize);
        String paperType = paperSizeMap.get(region);
        path = "/PaperSize:intvector";
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
        Map<String, String> measurementSystemMap = regionMeasurementData.get(MeasurementType.measurementSystem);
        String measurementSystem = measurementSystemMap.get(region);
        path = "/MeasurementSystem:int";
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

    private static final Pattern DRAFT_PATTERN = Pattern.compile("\\[@draft=\"\\w+\"]");

    /**
     * @param cldr
     * @param path
     * @param matcherFound
     * @param cantConvert
     * @return the result of converting an xpath into an ICU-style path
     */
    private static RegexResult getConvertedValues(RegexLookup<RegexResult> lookup,
            CLDRFile cldr, String path,
            Output<Finder> matcherFound, Set<String> cantConvert) {
        String fullPath = cldr.getFullXPath(path);
        fullPath = fullPath == null ? path : DRAFT_PATTERN.matcher(fullPath).replaceAll("");
        List<String> errors = DEBUG ? new ArrayList<String>() : null;
        RegexResult result = lookup.get(fullPath, null, null, matcherFound, errors);
        // Cache patterns for later analysis.
        patternCache.add(matcherFound.toString());
        if (result == null) {
            if (cantConvert != null) {
                cantConvert.add(fullPath);
            }
            if (DEBUG && errors != null) {
                System.out.println("\tDEBUG\t" + CollectionUtilities.join(errors, "\n\tDEBUG\t"));
            }
        } else {
            if (DEBUG && DEBUG_MATCH_REGEX.reset(fullPath).find()) {
                System.out.println("Matching:\t" + fullPath
                    + "\n\t\twith\t" + matcherFound.value);
            }
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

    /**
     * In this prototype, just convert one file.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // TODO Handle more than just the main directory.
        // TODO(jchye): Add commandline args.
        String fileRegexString = args.length == 0 ? "(en|root|ja|zh|ru|fr|ar|ko|he|en_AU)" : args[0];
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, fileRegexString);

        LDMLConverter converter = new LDMLConverter();
        Set<String> cantConvert = new HashSet<String>(); // use HashSet for speed

        long totalTime = System.currentTimeMillis();
        for (String locale : factory.getAvailable()) {
            long time=System.currentTimeMillis();
            converter.fillFromCLDR(factory, locale, cantConvert);
            converter.writeRB("/Users/jchye/tweaks/newicu/", locale);
            System.out.println("Converted locale " + locale + " in " + (System.currentTimeMillis() - time) + "ms");
        }
        System.out.println("Total time taken: " + (System.currentTimeMillis() - totalTime));
/*
        // sort the can't-convert strings and print
        for (String unconverted : Builder.with(new TreeSet<String>(CLDRFile.ldmlComparator)).addAll(cantConvert).get()) {
            System.out.println("Can't Convert:\t" + unconverted);
        }
        
        Map<String, RegexResult> outputUnmatched = new TreeMap<String, RegexResult>();
        for (Entry<String, RegexResult> patternRegexResult : pathConverter.getUnmatchedPatterns(patternCache, outputUnmatched).entrySet()) {
            System.out.println(patternRegexResult.getKey() + "\t" + patternRegexResult.getValue().unprocessed + "\t" + "***Unmatched***");
        }
        */
    }
}
