package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.QuickCheck;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.MetaZoneRange;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateXMB {
    static StandardCodes sc = StandardCodes.make();

    static final String DATE; 
    static {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DATE = dateFormat.format(new Date());
    }

    static final Matcher starAttributeMatcher = Pattern.compile("=\"([^\"]*)\"").matcher("");
    static final String stock = "en|ar|de|es|fr|it|ja|ko|nl|pl|ru|th|tr|pt|zh|zh_Hant|bg|ca|cs|da|el|fa|fi|fil|hi|hr|hu|id|lt|lv|ro|sk|sl|sr|sv|uk|vi|he|nb|et|ms|am|bn|gu|is|kn|ml|mr|sw|ta|te|ur|eu|gl|af|zu|en_GB|es_419|pt_BR|fr_CA";
    private static final HashSet<String> REGION_LOCALES = new HashSet<String>(Arrays.asList(stock.split("|")));

    final static Options myOptions = new Options()
    .add("target", ".*", CldrUtility.TMP_DIRECTORY + "dropbox/xmb/", "The target directory for building. Will generate an English .xmb file, and .wsb files for other languages.")
    .add("file", ".*", stock, "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
    // "^(sl|fr)$", 
    .add("path", ".*", "Filter the information based on path name, using a regex argument") // "dates.*(pattern|available)", 
    .add("content", ".*", "Filter the information based on content name, using a regex argument")
    .add("jason", ".*", "Generate JSON versions instead")
    .add("zone", null, "Show metazoneinfo and exit")
    ;

    static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
    static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // darn'd checked exceptions
        }
    }
    static Matcher contentMatcher;
    static Matcher pathMatcher;
    static PrettyPath prettyPath = new PrettyPath();

    //enum Handling {SKIP};
    static RegexLookup<String> pathHandling = new RegexLookup<String>().loadFromFile(GenerateXMB.class, "xmbHandling.txt");
    static final Matcher datePatternMatcher = Pattern.compile("dates.*(pattern|available)").matcher("");

    public static final boolean DEBUG = false;

    private static final HashSet<String> SKIP_LOCALES = new HashSet<String>(Arrays.asList(new String[]{"en", "root"}));

    public static void main(String[] args) throws Exception {
        myOptions.parse(args, true);
        Option option;
        option = myOptions.get("zone");
        if (option.doesOccur()) {
            showMetazoneInfo();
            return;
        }
        option = myOptions.get("file");
        String fileMatcherString = option.doesOccur() ? option.getValue() : ".*";
        option = myOptions.get("content");
        contentMatcher = option.doesOccur() ? Pattern.compile(option.getValue()).matcher("") : null;
        option = myOptions.get("path");
        pathMatcher = option.doesOccur() ? Pattern.compile(option.getValue()).matcher("") : null;

        String targetDir = myOptions.get("target").getValue();
        countFile = BagFormatter.openUTF8Writer(targetDir + "/log/", "counts.txt");


        Factory cldrFactory1 = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory1.make("en", true);
        CLDRFile root = cldrFactory1.make("en", true);

        EnglishInfo englishInfo = new EnglishInfo(targetDir, english, root);
        writeFile(targetDir, "en", englishInfo, english, true, false);
        writeFile(targetDir + "/filtered/", "en", englishInfo, english, true, true);

        // TODO:
        // Replace {0}... with placeholders (Mostly done, but need better examples)
        // Replace datetime fields (MMM, L, ...) with placeholders
        // Skip items that we don't need translated (most language names, script names, deprecated region names, etc.
        // Add descriptions
        // Add pages with detailed descriptions, and links from the descriptions
        // Represent the items with count= as ICUSyntax
        // Filter items that we don't want to get translated, and add others that we need even if not in English
        // Rewire items that are in undistinguished attributes
        // Test each xml file for validity
        // Generate strings that let the user choose the placeholder style hh vs HH,...???

        Factory cldrFactory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, fileMatcherString);
        LanguageTagParser ltp = new LanguageTagParser();
        
        for (String file : cldrFactory2.getAvailable()) {
            if (SKIP_LOCALES.contains(file)) {
                continue;
            }

            // skip all locales with regions (with certain exceptions)
            if (ltp.set(file).getRegion().length() != 0) {
                if (!REGION_LOCALES.contains(file)) {
                    continue;
                }
            }
            
            // skip anything without plural rules
            final PluralInfo plurals = supplementalDataInfo.getPlurals(file, false);
            if (plurals == null) {
                System.out.println("Skipping " + file + ", no plural rules");
                continue;
            }
            
            CLDRFile cldrFile = cldrFactory2.make(file, true);
            writeFile(targetDir + "/wsb/", file, englishInfo, cldrFile, false, false);
            writeFile(targetDir + "/wsb/filtered/", file, englishInfo, cldrFile, false, true);
            countFile.flush();
        }
        countFile.close();
    }

    static final Pattern COUNT_OR_ALT_ATTRIBUTE = Pattern.compile("\\[@(count)=\"([^\"]*)\"]");

    private static void writeFile(String targetDir, String file, EnglishInfo englishInfo, CLDRFile cldrFile, boolean isEnglish, boolean filter) throws IOException {
        String extension = "xml";
        XPathParts xpathParts = new XPathParts();
        Relation<String, String> reasonsToPaths = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class);
        Set<String> seenStarred = new HashSet<String>();

        Relation<String, Row.R2<PathInfo, String>> countItems = Relation.of(new TreeMap<String, Set<Row.R2<PathInfo, String>>>(), TreeSet.class);
        Matcher countMatcher = COUNT_OR_ALT_ATTRIBUTE.matcher("");
        int lineCount = 0;
        int wordCount = 0;
        
        StringWriter buffer = new StringWriter();
        PrintWriter out1 = new PrintWriter(buffer);

        for (PathInfo pathInfo : englishInfo) {
            String path = pathInfo.getPath();
            // skip root
            String locale = cldrFile.getSourceLocaleID(path, null);
            if (locale.equals("root")) {
                continue;
            }
            String value = cldrFile.getStringValue(path);
            boolean isUnits = path.startsWith("//ldml/units/unit");
            if (filter && !isUnits) {
                String starred = pathInfo.getStarredPath();
                if (seenStarred.contains(starred)) {
                    continue;
                }
                seenStarred.add(starred);
            }
            if (value == null) {
                reasonsToPaths.put("missing", path);
                continue;
            }
            String fullPath = cldrFile.getFullXPath(path);
            if (fullPath.contains("draft")) {
                xpathParts.set(fullPath);
                String draftValue = xpathParts.getAttributeValue(-1, "draft");
                if (!draftValue.equals("contributed")) {
                    reasonsToPaths.put(draftValue, path);
                    continue;
                }
            }
            //String fullPath = cldrFile.getStringValue(path);
            // //ldml/units/unit[@type="day"]/unitPattern[@count="one"]
            if (isUnits) {
                countMatcher.reset(path).find();
                String countLessPath = countMatcher.replaceAll("");
                countItems.put(countLessPath, Row.of(pathInfo, value));
                continue;
            }
            writePathInfo(out1, pathInfo, value, isEnglish);
            wordCount += pathInfo.wordCount;
            ++lineCount;
        }
        wordCount += writeCountPathInfo(out1, cldrFile.getLocaleID(), countItems, isEnglish, filter);
        lineCount += countItems.size();
        out1.flush();
        
        PrintWriter out = BagFormatter.openUTF8Writer(targetDir, file + "." + extension);

        if (isEnglish) {
            FileUtilities.appendFile(GenerateXMB.class, "xmb-dtd.xml", out);
            out.println("<messagebundle class='CLDR'> <!-- " + DATE + "-->");
            out.println(buffer.toString());
            out.println("</messagebundle>");
        } else {
            FileUtilities.appendFile(GenerateXMB.class, "wsb-dtd.xml", out);
            out.println("<worldserverbundles lazarus_id='dummy' date='" + DATE + "'>");
            out.println("  <worldserverbundle project_id='CLDR' message_count='" + lineCount + "'>");
            out.println(buffer.toString());
            out.println("  </worldserverbundle>");
            out.println("</worldserverbundles>");
        }
        out.close();
        QuickCheck.check(new File(targetDir, file + "." + extension));
        if (!filter) {
            countFile.println(file + "\t" + lineCount + "\t" + wordCount);
        }
        if (!isEnglish && !filter) {
            writeReasons(reasonsToPaths, targetDir, file);
        }
    }

    static final Pattern COUNT_ATTRIBUTE = Pattern.compile("\\[@count=\"([^\"]*)\"]");

    private static int writeCountPathInfo(PrintWriter out, String locale, Relation<String, R2<PathInfo, String>> countItems, boolean isEnglish, boolean filter) {
        Matcher m = COUNT_ATTRIBUTE.matcher("");
        int wordCount = 0;
        PluralInfo pluralInfo = supplementalDataInfo.getPlurals(locale);

        for (Entry<String, Set<R2<PathInfo, String>>> entry : countItems.keyValuesSet()) {
            String countLessPath = entry.getKey();
            Map<String,String> fullValues = new TreeMap<String,String>();
            PathInfo pathInfo = null;
            for (R2<PathInfo, String> entry2 : entry.getValue()) {
                PathInfo pathInfoN = entry2.get0();
                m.reset(pathInfoN.getPath()).find();
                String count = m.group(1);
                if (count.equals("other")) {
                    pathInfo = pathInfoN;
                }
                String value = entry2.get1();
                fullValues.put(count, value);
            }
            if (pathInfo == null) {
                continue;
            }
            if (fullValues.size() < 2) {
                // if we don't have two count values, skipe
                System.out.println(locale + "\tMust have 2 count values: " + entry.getKey());
                continue;
            }
            String var = pathInfo.getFirstVariable();
            String fullPlurals = showPlurals(var, fullValues, locale, pluralInfo);
            if (fullPlurals == null) {
                System.out.println("Can't format plurals for: " + entry.getKey());
                continue;
            }

            out.println();
            if (isEnglish) {
                out.println("\t<!--\t" 
                        //+ prettyPath.getPrettyPath(pathInfo.getPath(), false) + " ;\t" 
                        + countLessPath + "\t-->");
            } 
            out.println("\t<msg id='" + pathInfo.getStringId() + "' desc='" + pathInfo.description + "'");
            out.println("\t >" + fullPlurals + "</msg>");
            //            if (!isEnglish || pathInfo.placeholderReplacements != null) {
            //                out.println("\t<!-- English original:\t" + pathInfo.getEnglishValue() + "\t-->");
            //            }
            out.flush();
            wordCount += pathInfo.wordCount * 3;
            if (filter) {
                break;
            }
        }
        return wordCount;
    }

    static final String[] PLURAL_KEYS = {"=0", "=1", "zero", "one", "two", "few", "many", "other"};
    static final String[] EXTRA_PLURAL_KEYS = {"zero", "one", "two", "few", "many"};

    private static String showPlurals(String var, Map<String,String> values, String locale, PluralInfo pluralInfo) {
        /*
        <msg desc="[ICU Syntax] Plural forms for a number of hours. These are special messages: before translating, see cldr.org/translation/plurals.">
         {LENGTH, select,
          abbreviated {
           {NUMBER_OF_HOURS, plural,
            =0 {0 hrs}
            =1 {1 hr}
            zero {# hrs}
            one {# hrs}
            two {# hrs}
            few {# hrs}
            many {# hrs}
            other {# hrs}}}
          full {
           {NUMBER_OF_HOURS, plural,
            =0 {0 hours}
            =1 {1 hour}
            zero {# hours}
            one {# hours}
            two {# hours}
            few {# hours}
            many {# hours}
            other {# hours}}}}
         </msg>
         */
        StringBuilder result = new StringBuilder();
        result.append('{').append(var).append(", plural,");
        for (String key : PLURAL_KEYS) {
            String value;
            value = values.get(key);
            if (value == null) {
                if (key.startsWith("=")) {
                    String stringCount = key.substring(1);
                    int intCount = Integer.parseInt(stringCount);
                    Count count = pluralInfo.getCount(intCount);
                    value = values.get(count.toString());
                    if (value == null) {
                        return null;
                    }
                    value = value.replace("{0}", stringCount);
                } else {
                    value = values.get("other");
                    if (value == null) {
                        return null;
                    }
                }
            }
            String newValue = MessageFormat.format(MessageFormat.autoQuoteApostrophe(value), new Object[] {key.startsWith("=") ? key.substring(1,2) : "#"});
            result.append("\n\t\t\t").append(key).append(" {").append(newValue).append('}');
        }
        return result.append('}').toString();
    }

    private static void writePathInfo(PrintWriter out, PathInfo pathInfo, String value, boolean isEnglish) {
        String path = pathInfo.getPath();
        out.println();
        if (isEnglish) {
            out.println("\t<!--\t" 
                    //+ prettyPath.getPrettyPath(pathInfo.getPath(), false) + " ;\t" 
                    + pathInfo.getPath() + "\t-->");
        } 
        out.println("\t<msg id='" + pathInfo.getStringId() + "' desc='" + pathInfo.description + "'");
        out.println("\t >" + pathInfo.transformValue(path, value, isEnglish) + "</msg>");
        if (!isEnglish || pathInfo.placeholderReplacements != null) {
            out.println("\t<!-- English original:\t" + pathInfo.getEnglishValue() + "\t-->");
        }
        out.flush();
    }

    private static void writeReasons(Relation<String, String> reasonsToPaths, String targetDir, String filename) throws IOException {
        targetDir += "/skipped/";
        filename += ".txt";
        PrintWriter out = BagFormatter.openUTF8Writer(targetDir, filename);
        out.println("# " + DATE);
        for (Entry<String, Set<String>> reasonToSet : reasonsToPaths.keyValuesSet()) {
            for (String path : reasonToSet.getValue()) {
                out.println(reasonToSet.getKey() + "\t" + path);
            }
        }
        out.close();
    }


    static class PathInfo implements Comparable<PathInfo>{
        private final String path;
        private final Long id;
        private final String stringId;
        private final String englishValue;
        private final Map<String, String> placeholderReplacements;
        private final String description;
        private final String starredPath;
        private final int wordCount;
        
        static final BreakIterator bi = BreakIterator.getWordInstance(ULocale.ENGLISH);
        static final UnicodeSet ALPHABETIC = new UnicodeSet("[:Alphabetic:]");

        public PathInfo(String path, String englishValue, Map<String, String> placeholderReplacements, String description, String starredPath) {
            this.path = path;
            long id = getId(path);
            this.id = id;
            stringId = String.valueOf(id);
            this.englishValue = englishValue;
            this.placeholderReplacements = placeholderReplacements;
            this.description = description.intern();
            this.starredPath = starredPath;
            // count words
            int tempCount = 0;
            bi.setText(englishValue);
            int start = bi.first();
            for (int end = bi.next();
                 end != BreakIterator.DONE;
                 start = end, end = bi.next()) {
                 String word = englishValue.substring(start,end);
                 if (ALPHABETIC.containsSome(word)) {
                     ++tempCount;
                 }
            }
            wordCount = tempCount == 0 ? 1 : tempCount;
        }

        static final Pattern VARIABLE_NAME = Pattern.compile("name='([^']*)'");
        public String getFirstVariable() {
            //... name='FIRST_PART_OF_TEXT' ...
            String placeHolder = placeholderReplacements.get("{0}");
            Matcher m = VARIABLE_NAME.matcher(placeHolder);
            if (!m.find()) {
                throw new IllegalArgumentException("Missing name in " + placeHolder);
            }
            return m.group(1);
        }

        public String getPath() {
            return path;
        }

        public Long getId() {
            return id;
        }

        public String getStringId() {
            return stringId;
        }

        public String getEnglishValue() {
            return englishValue;
        }

        private static long getId(String path) {
            byte[] hash = digest.digest(path.getBytes()); // safe, because it is all ascii
            long result = 0;
            for (int i = 0; i < 8; ++i) {
                result <<= 8;
                result ^= hash[i];
            }
            // mash the top bit to make things easier
            result &= 0x7FFFFFFFFFFFFFFFL;
            return result;
        }

        public String getDescription() {
            return description;
        }

        public String getStarredPath() {
            return starredPath;
        }

        public Map<String, String> getPlaceholderReplacements() {
            return placeholderReplacements;
        }

        //static DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();

        private String transformValue(String path, String value, boolean isEnglish) {
            String result = TransliteratorUtilities.toHTML.transform(value);
            if (placeholderReplacements == null) {
                // skip
            } else if (result.contains("{0}")) {
                // TODO: fix for quoting
                result = replacePlaceholders(result, placeholderReplacements, isEnglish);
//            } else {
//                formatParser.set(value);
//                StringBuilder buffer = new StringBuilder();
//                for (Object item : formatParser.getItems()) {
//                    if (item instanceof DateTimePatternGenerator.VariableField) {
//                        String variable = item.toString();
//                        String replacement = placeholderReplacements.get(variable);
//                        if (replacement == null) {
//                            throw new IllegalArgumentException("Missing placeholder for " + variable);
//                        }
//                        buffer.append(replacement);
//                    } else {
//                        buffer.append(item);
//                    }
//                }
//                result = buffer.toString();
            }
            return result;
        }

        private String replacePlaceholders(String result, Map<String, String> argumentsFromPath, boolean isEnglish) {
            for (Entry<String, String> entry : argumentsFromPath.entrySet()) {
                String replacement = entry.getValue();
                String value = (isEnglish ? replacement : replacement.replaceAll("<ex>.*</ph>", "</ph>"));
                result = result.replace(entry.getKey(), value);
            }
            return result;
        }
        
        @Override
        public int compareTo(PathInfo arg0) {
            return path.compareTo(arg0.path);
        }
        
        public String toString() {
            return path;
        }
    }

    private static final String MISSING_DESCRIPTION = "Before translating, please see cldr.org/translation.";


    static class EnglishInfo implements Iterable<PathInfo> {

        private static final Set<String> EXTRA_LANGUAGES = new TreeSet<String>(Arrays.asList("ace en zh ja de ru es ko fr pl ar pt it tr nl cs id th sv vi ro nb hu fi he bg da et sk sr el lt hr lv fa sl uk ca is ms fil az sq eu ka gl hi be af mk la hy mn fy kk ta bn kn mt lb ur uz eo si km ky cy ne ku sw oc mi fo jv te ml ug pa mr ga ps gu my lo yo su tg ht am tt gd qu bo dv to sd sa iu or bho gn".split(" ")));

        Map<String, PathInfo> pathToPathInfo = new TreeMap();
        Map<Long, PathInfo> longToPathInfo = new HashMap();

        EnglishInfo(String targetDir, CLDRFile english, CLDRFile root) throws Exception {
            // we don't want the fully resolved paths, but we do want the direct inheritance from root.
            Status status = new Status();
            Map<String, List<Set<String>>> starredPaths = new TreeMap<String,List<Set<String>>>();

            Merger<Map<String, String>> merger = new MyMerger();
            RegexLookup<Map<String,String>> patternPlaceholders = RegexLookup.of(null, new MapTransform(), merger)
            .loadFromFile(GenerateXMB.class, "xmbPlaceholders.txt");
            
            HashSet<String> metazonePaths = new HashSet<String>();
            //^//ldml/dates/timeZoneNames/metazone\[@type="([^"]*)"]
            for (MetazoneInfo metazoneInfo : MetazoneInfo.METAZONE_LIST) {
                for (String item : metazoneInfo.getTypes()) {
                    String path = "//ldml/dates/timeZoneNames/metazone[@type=\"" + metazoneInfo.metazoneId + "\"]" + item;
                    metazonePaths.add(path);
                }
            }
            
            // TODO add short countries
            HashSet<String> extraLanguages = new HashSet<String>();
            //ldml/localeDisplayNames/languages/language[@type=".*"]
            
            for (String langId : EXTRA_LANGUAGES) {
                String langPath = "//ldml/localeDisplayNames/languages/language[@type=\"" + langId + "\"]";
                extraLanguages.add(langPath);
            }

            Set<String> sorted = Builder.with(new TreeSet<String>())
            .addAll(english)
            .removeAll(
                    new Transform<String,Boolean>() {
                        public Boolean transform(String source) {
                            return source.startsWith("//ldml/dates/timeZoneNames/metazone") ? Boolean.TRUE : Boolean.FALSE;
                        }
                    }
            )
            .get();
            sorted.addAll(metazonePaths);
            if (DEBUG) {
                TreeSet<String> diffs = new TreeSet<String>(extraLanguages);
                diffs.removeAll(sorted);
                System.out.println(diffs);
            }
            sorted.addAll(extraLanguages);

            // add the extra Count items.
            Map<String,String> extras = new HashMap<String,String>();
            Matcher m = COUNT_ATTRIBUTE.matcher("");

            for (String path : sorted) {
                if (path.contains("[@count=\"other\"]")) {
                    m.reset(path).find();
                    for (String key : EXTRA_PLURAL_KEYS) {
                        String path2 = path.substring(0,m.start(1)) + key + path.substring(m.end(1));
                        extras.put(path2,path);
                    }
                }
            }
            sorted.addAll(extras.keySet());

            Map<String, String> zone_country = sc.getZoneToCounty();
            Relation<String, String> reasonsToPaths = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class);
            Set<String> allMetazones = supplementalDataInfo.getAllMetazones();
            XPathParts parts = new XPathParts();
            CldrUtility.Output<String> starredPathOutput = new CldrUtility.Output<String>();
            Set<String> missingDescriptions = new TreeSet<String>();
            CldrUtility.Output<String[]> pathArguments = new CldrUtility.Output<String[]>();
            
            Matcher metazoneMatcher = Pattern.compile("//ldml/dates/timeZoneNames/metazone\\[@type=\"([^\"]*)\"]/(.*)/(.*)").matcher("");

            // TODO: for each count='other' path, add the other keywords and values

            for (String path : sorted) {
                if (pathMatcher != null 
                        && !pathMatcher.reset(path).find()) {
                    reasonsToPaths.put("path-parameter", path);
                    continue;
                }
                String description = pathHandling.get(path, pathArguments);
                if (description == null) {
                    description = MISSING_DESCRIPTION;
                } else if ("SKIP".equals(description)) {
                    reasonsToPaths.put("SKIP", path);
                    continue;
                }
                //                String localeWhereFound = english.getSourceLocaleID(path, status);
                //                if (!status.pathWhereFound.equals(path)) {
                //                    reasonsToPaths.put("alias", path);
                //                    continue;
                //                }
                String value = english.getStringValue(path);
                if (value == null) { // a count item?
                    String xpath = extras.get(path);
                    if (xpath != null) {
                        value = english.getStringValue(xpath);
                    } else {
                        if (path.contains("/metazone")) {
                            metazoneMatcher.reset(path).matches();
                            String name = metazoneMatcher.group(1);
                            String type = metazoneMatcher.group(3);
                            value = name.replace('_', ' ') + (type.equals("generic") ? "" : type.equals("daylight") ? " Summer" : " Winter") + " Time";
                            // System.out.println("Missing:\t" + path + " :\t" + value);
                        }
                    }
                }
                if (value.length() == 0) {
                    reasonsToPaths.put("empty-content", path);
                    continue;
                }
                if (contentMatcher != null && !contentMatcher.reset(value).find()) {
                    reasonsToPaths.put("content-parameter", path);
                    continue;
                }

                List<String> attributes = addStarredInfo(starredPaths, path, starredPathOutput);

                // In special cases, only use if there is a root value (languageNames, ...
                if (description.startsWith("ROOT")) {
                    int typeEnd = description.indexOf(';');
                    String type = description.substring(4,typeEnd).trim();
                    description = description.substring(typeEnd+1).trim();

                    boolean isMetazone = type.equals("metazone");
                    String code = attributes.get(0);
                    boolean isRootCode = isRootCode(code, allMetazones, type, isMetazone);
                    if (!isRootCode) {
                        reasonsToPaths.put("code", path);
                        continue;
                    }
                    if (isMetazone) {
                        parts.set(path);
                        String daylightType = parts.getElement(-1);
                        daylightType =  daylightType.equals("daylight") ? "summer" : daylightType.equals("standard") ? "winter" : daylightType;
                        String length = parts.getElement(-2);
                        length = length.equals("long") ? "" : "abbreviated ";
                        code = code + ", " + length + daylightType + " form";
                    }
                    description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), new Object[]{code});
                } else if (path.contains("exemplarCity")) {
                    String regionCode = zone_country.get(attributes.get(0));
                    String englishRegionName = english.getName(CLDRFile.TERRITORY_NAME, regionCode);
                    description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), new Object[]{englishRegionName});
                } else if (description != MISSING_DESCRIPTION){
                    description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), pathArguments.value);
                }

                Map<String, String> placeholders = patternPlaceholders.get(path);
                PathInfo row = new PathInfo(path, value, placeholders, description, starredPathOutput.value);
                if (description == MISSING_DESCRIPTION) {
                    missingDescriptions.add(starredPathOutput.value);
                }

                Long hash = row.getId();
                if (longToPathInfo.containsKey(hash)) {
                    throw new IllegalArgumentException("Id collision for "
                            + path + " and " + longToPathInfo.get(hash).getPath());
                }
                pathToPathInfo.put(path, row);
                longToPathInfo.put(hash, row);
                if (value.contains("{0}") && patternPlaceholders.get(path) == null) {
                    System.out.println("ERROR: " + path + " ; " + value);
                }
            }
            
            PrintWriter out = BagFormatter.openUTF8Writer(targetDir + "/log/", "en-paths.txt");
            out.println("# " + DATE);
            for (Entry<String, List<Set<String>>> starredPath : starredPaths.entrySet()) {
                out.println(starredPath.getKey() + "\t\t" + starredPath.getValue());
            }
            out.close();
            out = BagFormatter.openUTF8Writer(targetDir + "/log/", "en-missingDescriptions.txt");
            out.println("# " + DATE);
            for (String starredPath : missingDescriptions) {
                // ^//ldml/dates/timeZoneNames/zone\[@type=".*"]/exemplarCity ; ROOT timezone ; The name of a city in: {0}. See cldr.org/xxxx.
                out.println(toRegexPath(starredPath) + "\t;\tDESCRIPTION\t" + starredPaths.get(starredPath));
            }
            out.close();
            writeReasons(reasonsToPaths, targetDir, "en");
        }

        private boolean isRootCode(String code, Set<String> allMetazones, String type, boolean isMetazone) {
            Set<String> codes = isMetazone ? allMetazones  : type.equals("timezone") ? sc.getCanonicalTimeZones() : sc.getSurveyToolDisplayCodes(type); 
            // end
            boolean isRootCode = codes.contains(code) || code.contains("_");
            if (!isRootCode && type.equals("language") 
                    && EXTRA_LANGUAGES.contains(code)) {
                isRootCode = true;
            }
            return isRootCode;
        }

        private String toRegexPath(String starredPath) {
            String result = starredPath.replace("[", "\\[");
            result = result.replace("\".*\"", "\"([^\"]*)\"");
            return "^" + result;
        }

        private List<String> addStarredInfo(Map<String, List<Set<String>>> starredPaths, String path, CldrUtility.Output<String> starredPathOutput) {
            GenerateXMB.starAttributeMatcher.reset(path);
            StringBuilder starredPath = new StringBuilder();
            List<String> attributes = new ArrayList<String>();
            int lastEnd = 0;
            while (GenerateXMB.starAttributeMatcher.find()) {
                int start = GenerateXMB.starAttributeMatcher.start(1);
                int end = GenerateXMB.starAttributeMatcher.end(1);
                starredPath.append(path.substring(lastEnd, start));
                starredPath.append(".*");

                attributes.add(path.substring(start, end));
                lastEnd = end;
            }
            starredPath.append(path.substring(lastEnd));
            String starredPathString = starredPath.toString().intern();
            starredPathOutput.value = starredPathString;

            List<Set<String>> attributeList = starredPaths.get(starredPathString);
            if (attributeList == null) {
                starredPaths.put(starredPathString, attributeList = new ArrayList<Set<String>>());
            }
            int i = 0;
            for (String attribute : attributes) {
                if (attributeList.size() <= i) {
                    TreeSet<String> subset = new TreeSet<String>();
                    subset.add(attribute);
                    attributeList.add(subset);
                } else {
                    Set<String> subset = attributeList.get(i);
                    subset.add(attribute);
                }
                ++i;
            }
            return attributes;
        }

        @Override
        public Iterator<PathInfo> iterator() {
            return pathToPathInfo.values().iterator();
        }
    }

    static final class MapTransform implements Transform<String, Map<String,String>> {

        @Override
        public Map<String, String> transform(String source) {
            Map<String, String> result = new LinkedHashMap<String, String>();
            try {
                String[] parts = source.split(";\\s+");
                for (String part : parts) {
                    int equalsPos = part.indexOf('=');
                    String id = part.substring(0, equalsPos).trim();
                    String name = part.substring(equalsPos+1).trim();
                    int spacePos = name.indexOf(' ');
                    String example;
                    if (spacePos >= 0) {
                        example = name.substring(spacePos+1).trim();
                        name = name.substring(0,spacePos).trim();
                    } else {
                        example = "";
                    }

                    String old = result.get(id);
                    if (old != null) {
                        throw new IllegalArgumentException("Key occurs twice: " + id + "=" + old + "!=" + name);
                    }
                    // <ph name='x'><ex>xxx</ex>yyy</ph>
                    result.put(id, "<ph name='" + name + "'><ex>" + example+ "</ex>" + id +  "</ph>");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse " + source, e);
            }
            for (Entry<String, String> entry : result.entrySet()) {
                if (DEBUG) System.out.println(entry);
            }
            return result;
        }

    }

    private static final class MyMerger implements Merger<Map<String, String>> {
        @Override
        public Map<String, String> merge(Map<String, String> a, Map<String, String> into) {
            // check unique
            for (String key : a.keySet()) {
                if (into.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate placeholder: " + key);
                }
            }
            into.putAll(a);
            return into;
        }
    }

    static final long START_TIME = new Date(2000-1900, 1-1, 0).getTime();
    static final long END_TIME = new Date(2015-1900, 1-1, 0).getTime();
    static final long DELTA_TIME = 15 * 60 * 1000;
    static final long MIN_DAYLIGHT_PERIOD = 90L * 24 * 60 * 60 * 1000;
    
    static final Set<String> HAS_DAYLIGHT;
    static {
        Set<String> hasDaylightTemp = new HashSet<String>();
        Date date = new Date();
        main:
        for (String zoneId : sc.getCanonicalTimeZones()) {
            TimeZone zone = TimeZone.getTimeZone(zoneId);
            for (long time = START_TIME + MIN_DAYLIGHT_PERIOD; time < END_TIME; time += MIN_DAYLIGHT_PERIOD) {
                date.setTime(time);
                if (zone.inDaylightTime(date)) {
                    hasDaylightTemp.add(zoneId);
                    if (false && !zone.useDaylightTime()) {
                        System.out.println(zoneId + "\tuseDaylightTime()==false, but \tinDaylightTime(/" + date + "/)==true");
                    }
                    continue main;
                }
            }
        }
        HAS_DAYLIGHT = Collections.unmodifiableSet(hasDaylightTemp);
    }

    static final Set<String> SINGULAR_COUNTRIES;

    private static PrintWriter countFile;
    static {
        // start with certain special-case countries
        Set<String> singularCountries = new HashSet<String>(Arrays.asList("CL EC ES NZ PT AQ FM GL KI UM PF".split(" ")));

        Map<String,Set<String>> countryToZoneSet = sc.getCountryToZoneSet();

        main:
        for (Entry<String, Set<String>> countryZones : countryToZoneSet.entrySet()) {
            String country = countryZones.getKey();
            if (country.equals("001")) {
                continue;
            }
            Set<String> zones = countryZones.getValue();
            if (zones.size() == 1) {
                singularCountries.add(country);
                continue;
            }
            // make a set of sets
            List<TimeZone> initial = new ArrayList<TimeZone>();
            for (String s : zones) {
                initial.add(TimeZone.getTimeZone(s));
            }
            // now cycle through the times and see if we find any differences
            for (long time = START_TIME; time < END_TIME; time += DELTA_TIME) {
                int firstOffset = Integer.MIN_VALUE;
                for (TimeZone zone : initial) {
                    int offset = zone.getOffset(time);
                    if (firstOffset == Integer.MIN_VALUE) {
                        firstOffset = offset;
                    } else {
                        if (firstOffset != offset) {
                            if (false) System.out.println(country 
                            + " Difference at: " + new Date(time) 
                            + ", " + zone.getDisplayName() + " " + (offset/1000.0/60/60)
                            + ", " + initial.iterator().next().getDisplayName() + " " + (firstOffset/1000.0/60/60));
                            continue main;
                        }
                    }
                }
            }
            singularCountries.add(country);
        }
        SINGULAR_COUNTRIES = Collections.unmodifiableSet(singularCountries);
    }

    static final class MetazoneInfo {

        /**
         * @param metazoneId
         * @param singleCountry
         * @param hasDaylight
         * @param zonesForCountry 
         * @param regionToZone 
         */
        public MetazoneInfo(String metazoneId, String golden, boolean singleCountry, boolean hasDaylight) {
            this.golden = golden;
            this.metazoneId = metazoneId;
            this.singleCountry = singleCountry;
            this.hasDaylight = hasDaylight;
        }

        static final String[] GENERIC = {"/long/generic", 
            //"/short/generic"
            };
        static final String[] DAYLIGHT = {"/long/generic", "/long/standard", "/long/daylight", 
            //"/short/generic", "/short/standard", "/short/daylight"
            };
        
        public String[] getTypes() {
            return hasDaylight ? DAYLIGHT : GENERIC;
        }

        private final String metazoneId;
        private final String golden;
        private final boolean singleCountry;
        private final boolean hasDaylight;

        static final List<MetazoneInfo> METAZONE_LIST;
        static {
            //Set<String> zones = supplementalDataInfo.getCanonicalTimeZones();
            ArrayList<MetazoneInfo> result = new ArrayList<MetazoneInfo>();

            Map<String,String> zoneToCountry = sc.getZoneToCounty();
            Map<String,Set<String>> countryToZoneSet = sc.getCountryToZoneSet();

            Map<String, Map<String, String>> metazoneToRegionToZone = supplementalDataInfo.getMetazoneToRegionToZone();
            for (String metazone : supplementalDataInfo.getAllMetazones()) {
                Map<String, String> regionToZone = metazoneToRegionToZone.get(metazone);
                String golden = regionToZone.get("001");
                if (golden == null) {
                    throw new IllegalArgumentException("Missing golden zone " + metazone + ", " + regionToZone);
                }
                String region = zoneToCountry.get(golden);
                boolean isSingleCountry = SINGULAR_COUNTRIES.contains(region);
                if (isSingleCountry) {
                    continue;
                }
                
                //TimeZone goldenZone = TimeZone.getTimeZone(golden);

                Set<SupplementalDataInfo.MetaZoneRange> metazoneRanges = supplementalDataInfo.getMetaZoneRanges(golden);
                if (metazoneRanges == null) {
                    throw new IllegalArgumentException("Missing golden zone " + metazone + ", " + regionToZone);
                }
                MetazoneInfo item = new MetazoneInfo(metazone, golden, isSingleCountry, HAS_DAYLIGHT.contains(golden));
                result.add(item);
            }
            METAZONE_LIST = Collections.unmodifiableList(result);
        }

        public String toString() {
            return 
            sc.getZoneToCounty().get(golden)
            + "\t" + metazoneId 
            + "\t" + golden
            + "\t" + (singleCountry ? "singleCountry" : "") 
            + "\t" + (hasDaylight ? "useDaylightTime" : "") 
            //+ ": " + zonesForCountry 
            //+ "\t" + regionToZone;
            ;
        }
    }

    static void showMetazoneInfo() {
        System.out.println("\nZones in multiple metazones\n");

        for (String zone : sc.getCanonicalTimeZones()) {
            Set<SupplementalDataInfo.MetaZoneRange> metazoneRanges = supplementalDataInfo.getMetaZoneRanges(zone);
            if (metazoneRanges == null) {
                System.out.println("Zone doesn't have metazone! " + zone);
                continue;
            }
            if (metazoneRanges.size() != 1) {
                for (MetaZoneRange range : metazoneRanges) {
                    System.out.println(zone + ":\t" + range);
                }
                System.out.println();
            }
        }

        System.out.println("\nMetazoneInfo\n");

        for (boolean singleCountry : new boolean[] {false}) {
            for (boolean hasDaylight : new boolean[] {false, true}) {
                for (MetazoneInfo mzone : MetazoneInfo.METAZONE_LIST) {
                    if (mzone.hasDaylight != hasDaylight) continue;
                    if (mzone.singleCountry != singleCountry) continue;
                    System.out.println(mzone);
                }
            }
        }
    }
}
