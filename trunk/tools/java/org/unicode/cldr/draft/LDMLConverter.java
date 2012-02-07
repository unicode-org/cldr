package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
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
    private static Matcher                       DEBUG_FAIL_REGEX          = Pattern.compile("intervalFormats.*alias").matcher("");
    private static Matcher                       DEBUG_MATCH_REGEX          = Pattern.compile("intervalFormats.*alias").matcher("");

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

    static class RegexResult {
        final String         dir;
        private final String rbPath;
        private String       valuesReplacement;
        private String       fleshRegex;
        private String       aliasReplacement;

        public RegexResult(String source) {
            try {
                String[] parts = SEMI.split(source);
                dir = parts[0];
                rbPath = parts[1];
                for (int i = 0; i < parts.length; ++i) {
                    String part = parts[i];
                    if (part.startsWith("values=")) {
                        valuesReplacement = part.substring(7);
                    } else if (part.startsWith("alias=")) {
                        aliasReplacement = part.substring(6);
                    } else {
                        fleshRegex = part.replace("[@", "\\[@");
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Must be of form directory ; path: "
                        + source, e);
            }
        }

        public Finder getFleshRegexFinder(String[] arguments, Finder matcherFound) {
            Finder matcher;
            if (this.fleshRegex.equals("=")) {
                matcher = matcherFound;
            } else {
                String pathRegex = pathConverter.replace(this.fleshRegex, arguments);
                matcher = new RegexFinder(pathRegex);
            }
            return matcher;

        }

        public String[] getSpecialValues(String[] arguments) {
            if (valuesReplacement == null) {
                return null;
            }
            String toReplace = pathConverter.replace(valuesReplacement, arguments);
            String[] values = toReplace.split("\\s+");
            return values;
        }

        public String getRbPath(String[] arguments) {
            return pathConverter.replace(rbPath, arguments);
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

    static final Map<String, RegexResultCacheItem> regexResultCache = new HashMap<String, RegexResultCacheItem>();

    private static RegexResult getRegexResult(String path, Output<String[]> arguments, Output<Finder> matcherFound, List<String> errors) {
        RegexResult regexResult = pathConverter.get(path, null, arguments, matcherFound, errors);
        // always add
        if (regexResult != null && matcherFound != null) {
            regexResultCache.put(path, new RegexResultCacheItem(regexResult, matcherFound));
        }
        return regexResult;
    }

    /**
     * The value for the regex is a pair, with the directory and the path. There
     * is an optional 3rd parameter, which is used for "fleshing out"
     */
    public static Transform<String, RegexResult> PairValueTransform    = new Transform<String, RegexResult>() {
        public RegexResult transform(String source) {
            return new RegexResult(source);
        }
    };

    static final Matcher                         DATE_OR_TIME_FORMAT   = Pattern.compile("/(date|time)Formats/").matcher("");

    /**
     * Special hack comparator, so that RB strings come out in the right order.
     * This is only important for the order of items in arrays.
     */
    public static Comparator<String>             SpecialLDMLComparator = new Comparator<String>() {

        @Override
        /**
         * Reverse the ordering of the following:
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/displayName ; curr ; /Currencies/$1
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/symbol ; curr ; /Currencies/$1
         * and the following (time/date)
         * //ldml/dates/calendars/calendar[@type="([^"]*)"]/(dateFormats|dateTimeFormats|timeFormats)/(?:[^/\[]*)[@type="([^"]*)"]/(?:[^/\[]*)[@type="([^"]*)"]/.* ; locales ; /calendar/$1/DateTimePatterns
         */
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
                    int end0 = DATE_OR_TIME_FORMAT.end();
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
            return CLDRFile.ldmlComparator.compare((String) arg0, (String) arg1);
        }
    };

    /**
     * Loads the data in from a file. That file is of the form cldrPath ; rbPath
     */
    static RegexLookup<RegexResult>              pathConverter         = new RegexLookup<RegexResult>()
    .setPatternTransform(RegexFinderTransform)
    .setValueTransform(PairValueTransform)
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

    /**
     * Mapping from directory to path to values.
     * 
     * @author markdavis
     */
    static class MultiFileOutput {
        // Map<String,Map<String,List<String>>> multiOutput = new
        // HashMap<String,Map<String,List<String>>>();
        Map<String, Map<String, List<String>>> dir2path2values = new HashMap<String, Map<String, List<String>>>();
        Map<String, String>                    cldrPath2rbPath = new HashMap<String, String>();

        public void clear() {
            dir2path2values.clear();
            cldrPath2rbPath.clear();
        }

        public String getRbPath(String cldrPath) {
            return cldrPath2rbPath.get(cldrPath);
        }

        public Set<String> getCldrPaths() {
            return cldrPath2rbPath.keySet();
        }

        // PATH_COMPARATOR

        /**
         * The RB path,value pair actually has an array as the value. So when we
         * add to it, add to a list.
         * 
         * @param path
         * @param value
         * @param path2values
         * @return
         */
        void add(String directory, String file, String path, String value) {
            if (file != null) {
                path = "/" + file + path;
            }
            if (DEBUG)
                System.out.println("+++\t" + path + "\t" + value);
            boolean newDirectory = false;
            Map<String, List<String>> path2values = dir2path2values.get(directory);
            if (path2values == null) {
                dir2path2values.put(directory, path2values = new TreeMap<String, List<String>>(PATH_COMPARATOR));
                newDirectory = true;
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
         * @param output2
         * @param path2values
         */
        void writeRB(String dirPath, String file) {
            try {
                boolean wasSingular = false;
                String[] replacements = { "%file%", file };
                for (Entry<String, Map<String, List<String>>> dirAndPath2values : dir2path2values.entrySet()) {
                    String dir = dirAndPath2values.getKey();
                    PrintWriter out = BagFormatter.openUTF8Writer(dirPath + "/" + dir, file + ".txt");
                    out.write('\uFEFF');
                    FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
                    Map<String, List<String>> path2values = dirAndPath2values.getValue();
                    String[] lastLabels = new String[] {};

                    for (Entry<String, List<String>> entry : path2values.entrySet()) {
                        String path = entry.getKey();
                        List<String> values = entry.getValue();
                        String[] labels = path.split("/");
                        int common = getCommon(lastLabels, labels);
                        for (int i = lastLabels.length - 1; i > common; --i) {
                            if (wasSingular) {
                                wasSingular = false;
                            } else {
                                out.append(Utility.repeat(TAB, i - 1));
                            }
                            out.append("}\n");
                        }
                        for (int i = common + 1; i < labels.length; ++i) {
                            final String pad = Utility.repeat(TAB, i - 1);
                            out.append(pad);
                            out.append(quoteIfNeeded(path, labels[i]) + "{");
                            if (i != labels.length - 1) {
                                out.append('\n');
                            }
                        }
                        int maxWidth = 76;
                        boolean quote = !isIntRbPath(path);
                        if (values.size() == 1) {
                            String value = values.iterator().next();
                            if (quote) {
                                value = quoteInside(value);
                            }
                            if (value.length() <= maxWidth) {
                                appendQuoted(value, quote, out);
                                wasSingular = true;
                            } else {
                                final String pad = Utility.repeat(TAB, labels.length - 1);
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
                            final String pad = Utility.repeat(TAB, labels.length - 1);
                            out.append('\n');
                            for (String item : values) {
                                out.append(pad);
                                if (quote) {
                                    out.append('"');
                                    out.append(quoteInside(item));
                                    out.append("\"");
                                } else {
                                    out.append(item);
                                }
                                out.append(",\n");
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
                            out.append(Utility.repeat(TAB, i - 1));
                        }
                        out.append("}\n");
                    }
                    out.flush();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
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
            return dir2path2values.keySet();
        }

        public void fillFromCLDR(Factory factory, String file, Set<String> cantConvert) {
            CLDRFile cldr = factory.make(file, false);
            Output<String[]> arguments = new Output<String[]>();
            Output<Finder> matcherFound = new Output<Finder>();
            clear();
            // copy the relevant path/data to the output, changing as required.

            // first find all the items that need to be 'fleshed out'
            Set<Finder> fullMatches = new HashSet<Finder>();
            Set<String> allPaths = new TreeSet<String>(SpecialLDMLComparator);
            for (String path : cldr) { // sort them just to get the
                // "can't convert" messages in order.
                String fullPath = myGetFullXPath(cldr, path);
                RegexResult regexResult = getConvertedValues(cldr, fullPath, arguments, matcherFound, cantConvert);
                if (regexResult == null) {
                    continue;
                }
                allPaths.add(path);
                if (regexResult.fleshRegex != null) {
                    Finder matcher = regexResult.getFleshRegexFinder(arguments.value, matcherFound.value);
                    fullMatches.add(matcher);
                }
            }

            // now get all the resolved items that we need to flesh out with
            CLDRFile cldrResolved = factory.make(file, true);

            for (String path : cldrResolved) {
                for (Finder matcher : fullMatches) {
                    if (matcher.find(path, null)) {
                        allPaths.add(path);
                        break;
                    }
                }
            }

            // now convert to ICU format

            for (String path : allPaths) {
                String fullPath = myGetFullXPath(cldrResolved, path);
                String value = cldrResolved.getStringValue(path);
                addPath(cldrResolved, file, fullPath, value, arguments);
            }

            // Hack: add script, version
            hackAddExtras(cldrResolved, file);
        }

        private static String myGetFullXPath(CLDRFile cldr, String path) {
            String result = cldr.getFullXPath(path);
            return result.replace("[@draft=\"contributed\"]", "");
        }

        private void hackAddExtras(CLDRFile cldrResolved, String file) {
            UnicodeSet s = cldrResolved.getExemplarSet("", WinningChoice.WINNING);
            BitSet set = new BitSet();
            for (UnicodeSetIterator it = new UnicodeSetIterator(s); it.next();) {
                int script = UScript.getScript(it.codepoint);
                set.set(script);
            }
            set.clear(UScript.COMMON);
            set.clear(UScript.INHERITED);
            if (set.isEmpty()) {
                add("locales", file, "/LocaleScript", "Zyyy");
            } else {
                for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
                    // operate on index i here
                    String name = UScript.getShortName(i);
                    add("locales", file, "/LocaleScript", name);
                }
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
            for (String directory : directories()) {
                add(directory, file, "/Version", versionValue);
            }
            // special gorp
            /*
             * en-US MeasurementSystem:int{1} PaperSize:intvector{ 279, 216, }
             */

            String localeID = cldrResolved.getLocaleID();
            String region = localeID.equals("root") ? "001" : new LanguageTagParser().set(localeID).getRegion();

            Map<MeasurementType, Map<String, String>> regionMeasurementData = supplementalDataInfo.getTerritoryMeasurementData();
            Map<String, String> paperSizeMap = regionMeasurementData.get(MeasurementType.paperSize);
            String paperType = paperSizeMap.get(region);
            if (paperType == null) {
                // do nothing
            } else if (paperType.equals("A4")) {
                add("locales", file, "/PaperSize:intvector", "297");
                add("locales", file, "/PaperSize:intvector", "210");
            } else if (paperType.equals("US-Letter")) {
                add("locales", file, "/PaperSize:intvector", "279");
                add("locales", file, "/PaperSize:intvector", "216");
            } else {
                throw new IllegalArgumentException("Unknown paper type");
            }
            Map<String, String> measurementSystemMap = regionMeasurementData.get(MeasurementType.measurementSystem);
            String measurementSystem = measurementSystemMap.get(region);
            if (measurementSystem == null) {
                // do nothing
            } else if (measurementSystem.equals("metric")) {
                add("locales", file, "/MeasurementSystem:int", "0");
            } else if (measurementSystem.equals("US")) {
                add("locales", file, "/MeasurementSystem:int", "1");
            } else {
                throw new IllegalArgumentException("Unknown measurement system");
            }
        }

        static Matcher VERSION_MATCHER = Pattern.compile("\\$Revision:\\s*(\\d+)\\s*\\$").matcher("");

        private void addPath(CLDRFile cldr, String file, String path, String value, Output<String[]> arguments) {
            RegexResult regexResult = getConvertedValues(cldr, path, arguments, null, null);
            if (regexResult == null) {
                return;
            }
            String rbPath = regexResult.getRbPath(arguments.value);
            // special hack for commonlyUsed
            if (isIntRbPath(rbPath)) {
                value = value.equals("false") ? "0" :  value.equals("true") ? "1" : value;
            }
            cldrPath2rbPath.put(path, rbPath);
            String[] values = regexResult.getSpecialValues(arguments.value);
            if (values != null) {
                for (String valueItem : values) {
                    add(regexResult.dir, file, rbPath, valueItem);
                }
            } else {
                add(regexResult.dir, file, rbPath, value);
                // special hack for extra DateTimePatterns value; add the full
                // pattern twice.
                if (rbPath.contains("DateTimePatterns") && path.contains("/dateTimeFormats") && path.contains("[@type=\"full\"")) {
                    add(regexResult.dir, file, rbPath, value);
                }
            }
        }

        private static RegexResult getConvertedValues(CLDRFile cldr, String path, Output<String[]> arguments, Output<Finder> matcherFound,
                Set<String> cantConvert) {
            String fullPath = myGetFullXPath(cldr, path);
            if (fullPath.contains("[@draft")) {
                if (!fullPath.contains("[@draft=\"contributed\"]")) {
                    return null;
                }
            }
            List<String> errors = DEBUG_FAIL_REGEX != null && DEBUG_FAIL_REGEX.reset(path).find() ? new ArrayList<String>() : null;
            RegexResult regexResult = getRegexResult(path, arguments, matcherFound, errors);
            if (regexResult == null) {
                if (cantConvert != null) {
                    cantConvert.add(path);
                }
                if (errors != null) {
                    System.out.println("\tDEBUG\t" + CollectionUtilities.join(errors, "\n\tDEBUG\t"));
                }
                return null;
            } else if (DEBUG_MATCH_REGEX != null && DEBUG_MATCH_REGEX.reset(path).find()) {
                System.out.println("Matching:\t" + path
                        + "\n\t\twith\t" + matcherFound
                        + "\n\t\tgiving\t" + regexResult.getRbPath(arguments.value)
                        + "\n\t\tand\t" + Arrays.asList(regexResult.getSpecialValues(arguments.value))
                );
            }
            return regexResult;
        }

        /**
         * The default tab indent (actually spaces)
         */
        final static String TAB = "    ";

        /**
         * Quote numeric identifiers (because LDML2ICUConverter does).
         * 
         * @param item
         * @param item
         * @return
         */
        private static String quoteIfNeeded(String path, String item) {
            return (item.contains(":") && !isSpecialRbPath(item))
            || (path.contains("/relative") && !item.equals("relative"))
            ? quote(item)
                    : item;
            // item.isEmpty() || (ID_CHARACTERS.containsAll(item) &&
            // ID_START.contains(item.codePointAt(0))) ? item : quote(item);
        }

        /**
         * Fix characters inside strings.
         * 
         * @param item
         * @return
         */
        private static String quote(String item) {
            return new StringBuilder().append('"').append(quoteInside(item)).append('"').toString();
        }

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
            return i;
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
        String fileRegexString = args.length == 0 ? "(en|fr|ja|root)" : args[0];
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, fileRegexString);

        MultiFileOutput output = new MultiFileOutput();
        Set<String> cantConvert = new HashSet(); // use HashSet for speed

        for (String file : factory.getAvailable()) {
            output.fillFromCLDR(factory, file, cantConvert);
            output.writeRB(CldrUtility.TMP_DIRECTORY + "dropbox/mark/converter/rb/", file);
        }

        PathStarrer starrer = new PathStarrer();
        Set<String> patterns = new TreeSet<String>();
        
        // sort the can't-convert strings and print
        for (String unconverted : Builder.with(new TreeSet<String>(CLDRFile.ldmlComparator)).addAll(cantConvert).get()) {
            String starred = starrer.set(unconverted);
            if (!patterns.contains(starred)) {
                System.out.println("Can't Convert:\t" + starred + "\t" + unconverted);
                patterns.add(starred);
            }
        }

        // show the cache of mapped strings
        Map<String, RegexResultCacheItem> sorted = new TreeMap<String, RegexResultCacheItem>(CLDRFile.ldmlComparator);
        sorted.putAll(regexResultCache);
        patterns.clear();
        
        for (Entry<String, RegexResultCacheItem> pathData : sorted.entrySet()) {
            final RegexResultCacheItem value = pathData.getValue();
            if (!patterns.contains(value.pattern)) {
                System.out.println(value.pattern + "\t" + value.regexResult.rbPath + "\t" + pathData.getKey());
                patterns.add(value.pattern);
            }
        }
        Map<String, RegexResult> outputUnmatched = new TreeMap<String, RegexResult>();
        for (Entry<String, RegexResult> patternRegexResult : pathConverter.getUnmatchedPatterns(patterns, outputUnmatched).entrySet()) {
            System.out.println(patternRegexResult.getKey() + "\t" + patternRegexResult.getValue().rbPath + "\t" + "***Unmatched***");
        }
    }
}
