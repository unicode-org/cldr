package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.unicode.cldr.test.CheckExemplars;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.QuickCheck;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.MetaZoneRange;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateXMB {
    private static final String DEBUG_PATH = "[@type=\"day\"]/unitPattern[@count=\"1\"]";

    static StandardCodes sc = StandardCodes.make();

    static final String DATE;
    static {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DATE = dateFormat.format(new Date());
    }
    static final String stock = "en|ar|de|es|fr|it|ja|ko|nl|pl|ru|th|tr|pt|zh|zh_Hant|bg|ca|cs|da|el|fa|fi|fil|hi|hr|hu|id|lt|lv|ro|sk|sl|sr|sv|uk|vi|he|nb|et|ms|am|bn|gu|is|kn|ml|mr|sw|ta|te|ur|eu|gl|af|zu|en_GB|es_419|pt_PT|fr_CA|zh_Hant_HK";
    private static final HashSet<String> REGION_LOCALES = new HashSet<String>(Arrays.asList(stock.split("\\|")));

    final static Options myOptions = new Options("In normal usage, you set the -t option for the target.")
        .add("target", ".*", CLDRPaths.TMP_DIRECTORY + "dropbox/xmb/",
            "The target directory for building. Will generate an English .xmb file, and .wsb files for other languages.")
        .add(
            "file",
            ".*",
            stock,
            "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
        // "^(sl|fr)$",
        .add("path", ".*", "Filter the information based on path name, using a regex argument")
        // "dates.*(pattern|available)",
        .add("content", ".*", "Filter the information based on content name, using a regex argument")
        .add("jason", ".*", "Generate JSON versions instead")
        .add("zone", null, "Show metazoneinfo and exit")
        .add("wsb", ".*", "Show metazoneinfo and exit")
        .add("kompare", ".*", CLDRPaths.BASE_DIRECTORY + "../DATA/cldr/common/google-bulk-imports",
            "Compare data with directory; generate files in -target.")
        .add("project_name", 'n', ".*", "CLDR", "The ID of the project.");

    static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
    // static Matcher contentMatcher;
    static Matcher pathMatcher;
    static RegexLookup<String> pathFindRemover = new RegexLookup<String>().loadFromFile(GenerateXMB.class,
        "xmbSkip.txt");; // .compile("//ldml/dates/calendars/calendar\\[@type=\"(?!gregorian).*").matcher("");
    static PrettyPath prettyPath = new PrettyPath();
    static int errors = 0;
    static Relation<String, String> path2errors = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);

    // enum Handling {SKIP};
    static final Matcher datePatternMatcher = PatternCache.get("dates.*(pattern|available)").matcher("");

    public static final boolean DEBUG = false;

    private static final HashSet<String> SKIP_LOCALES = new HashSet<String>(
        Arrays.asList(new String[] { "en", "root" }));

    public static String DTD_VERSION;

    private static String projectId;

    enum PlaceholderType {
        BRACES, // e.g. {NAME}
        XML, // e.g. <ph name='NAME' />
        XML_EXAMPLE // e.g. <ph name='NAME' /><ex>EXAMPLE</ex>{0}</ph>
    };

    public static void main(String[] args) throws Exception {
        myOptions.parse(args, true);
        Option option;
        option = myOptions.get("zone");
        if (option.doesOccur()) {
            showMetazoneInfo();
            return;
        }
        option = myOptions.get("file");
        String fileMatcherString = option.getValue();
        option = myOptions.get("content");
        Matcher contentMatcher = option.doesOccur() ? PatternCache.get(option.getValue()).matcher("") : null;
        option = myOptions.get("path");
        pathMatcher = option.doesOccur() ? PatternCache.get(option.getValue()).matcher("") : null;

        String targetDir = myOptions.get("target").getValue();
        countFile = FileUtilities.openUTF8Writer(targetDir + "/log/", "counts.txt");

        Factory cldrFactory1 = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory1.make("en", true);
        CLDRFile englishTop = cldrFactory1.make("en", false);
        DTD_VERSION = englishTop.getDtdVersion();

        CLDRFile root = cldrFactory1.make("en", true);

        showDefaultContents(targetDir, english);
        EnglishInfo englishInfo = new EnglishInfo(targetDir, english, root);

        option = myOptions.get("kompare");
        if (option.doesOccur()) {
            compareDirectory = option.getValue();
            compareFiles(fileMatcherString, contentMatcher, targetDir, cldrFactory1, english, englishInfo);
            return;
        }

        if (myOptions.get("wsb").doesOccur()) {
            displayWsb(myOptions.get("wsb").getValue(), englishInfo);
            return;
        }

        projectId = myOptions.get("project_name").getValue();

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

        Factory cldrFactory2 = Factory.make(CLDRPaths.MAIN_DIRECTORY, fileMatcherString);
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
        PrintWriter errorFile = FileUtilities.openUTF8Writer(targetDir + "/log/", "errors.txt");
        for (Entry<String, Set<String>> entry : path2errors.keyValuesSet()) {
            errorFile.println(entry);
        }
        errorFile.close();
        System.out.println("Errors: " + (errors + path2errors.size()));
    }

    private static void compareFiles(String fileMatcherString, Matcher contentMatcher, String targetDir,
        Factory cldrFactory1, CLDRFile english,
        EnglishInfo englishInfo) throws IOException {
        SubmittedPathFixer fixer = new SubmittedPathFixer();
        Factory cldrFactory2 = Factory.make(compareDirectory, fileMatcherString);
        PrintWriter output = null;
        PrintWriter log = FileUtilities.openUTF8Writer(targetDir + "/log/", "skipped.txt");

        for (String file : cldrFactory2.getAvailable()) {
            // System.out.println("Checking " + file);
            CLDRFile submitted = cldrFactory2.make(file, false);
            CLDRFile trunk = cldrFactory1.make(file, true);
            for (String path : With.in(submitted.iterator(null, submitted.getComparator()))) {
                if (pathMatcher != null && !pathMatcher.reset(path).matches()) {
                    continue;
                }
                String submittedValue = submitted.getStringValue(path);
                if (contentMatcher != null && !contentMatcher.reset(submittedValue).matches()) {
                    continue;
                }
                PathStatus pathStatus = shouldSkipPath(path, submittedValue);
                if (pathStatus == PathStatus.SKIP) {
                    continue;
                }

                // fix alt
                String trunkPath = fixer.fix(path, false);
                String trunkValue = trunk.getStringValue(trunkPath);
                if (CharSequences.equals(submittedValue, trunkValue)) {
                    continue;
                }
                if (output == null) {
                    output = FileUtilities.openUTF8Writer(targetDir, file + ".txt");
                    output.println("ID\tEnglish\tSource\tRelease\tDescription");
                }
                String englishValue = english.getStringValue(trunkPath);
                final PathInfo pathInfo = englishInfo.getPathInfo(trunkPath);
                String description;
                if (pathInfo == null) {
                    log.println(file + "\tDescription unavailable for " + trunkPath);
                    errors++;
                    String temp = fixer.fix(path, true);
                    englishInfo.getPathInfo(trunkPath);
                    continue;
                } else {
                    description = pathInfo.getDescription();
                }
                long id = StringId.getId(trunkPath);
                if (englishValue == null) {
                    log.println(file + "\tEmpty English for " + trunkPath);
                    errors++;
                    continue;
                }
                output.println(id + "\t" + ssquote(englishValue, false) + "\t" + ssquote(submittedValue, false) + "\t"
                    + ssquote(trunkValue, true) + "\t" + description);
            }
            if (output != null) {
                output.close();
                output = null;
            }
            log.flush();
        }
        log.close();
    }

    static Output<String[]> matches = new Output<String[]>();
    static List<String> failures = new ArrayList<String>();
    static Output<Finder> matcherFound = new Output<Finder>();

    enum PathStatus {
        SKIP, KEEP, MAYBE
    }

    public static PathStatus shouldSkipPath(String path, String value) {
        // skip if
        List<String> myFailures = null;
        if (false && path.contains("currencies") && path.contains("symbol")) {
            myFailures = failures;
        }
        String skipPath = pathFindRemover.get(path, null, matches, matcherFound, myFailures);
        if (myFailures != null && failures.size() != 0) {
            System.out.println("Failures\n\t" + CollectionUtilities.join(failures, "\n\t"));
            failures.clear();
        }
        if (skipPath == null || skipPath.equals("MAYBE")) {
            return PathStatus.MAYBE;
        } else if (skipPath.equals("VALUE")) {
            return value.equals(matches.value[1]) ? PathStatus.SKIP : PathStatus.MAYBE;
        } else if (skipPath.equals("SKIP")) {
            return PathStatus.SKIP;
        } else if (skipPath.equals("KEEP")) {
            return PathStatus.KEEP;
        }
        throw new IllegalArgumentException("Unexpected xmbSkip.txt value: " + skipPath);
    }

    private static String ssquote(String englishValue, boolean showRemoved) {
        if (englishValue == null) {
            return showRemoved ? "[removed]" : "[empty]";
        }
        englishValue = englishValue.replace("\"", "&quot;");
        return englishValue;
    }

    static class SubmittedPathFixer {
        private static final Pattern PATH_FIX = PatternCache.get("\\[@alt=\"" +
            "(?:proposed|((?!proposed)[-a-zA-Z0-9]*)-proposed)" +
            "-u\\d+-implicit[0-9.]+" +
            "(?:-proposed-u\\d+-implicit[0-9.]+)?" + // NOTE: we allow duplicated alt values because of a generation
            // bug.
            // -proposed-u971-implicit2.0
            "\"]");
        static Matcher pathFix = PATH_FIX.matcher("");

        public String fix(String path, boolean debug) {
            if (pathFix.reset(path).find()) {
                if (debug) {
                    // debug in case we get a mismatch
                    String temp = "REGEX:\t" +
                        RegexUtilities.showMismatch(PATH_FIX, path.substring(pathFix.start(0)));
                }
                final String group = pathFix.group(1);
                String replacement = group == null ? "" : "[@alt=\"" + group + "\"]";
                String trunkPath = path.substring(0, pathFix.start(0)) + replacement + path.substring(pathFix.end(0));
                // HACK because of change in CLDR defaults
                if (trunkPath.startsWith("//ldml/numbers/symbols/")) {
                    trunkPath = "//ldml/numbers/symbols[@numberSystem=\"latn\"]/"
                        + trunkPath.substring("//ldml/numbers/symbols/".length());
                }
                return trunkPath;
            }
            return path;
        }

    }

    private static void showDefaultContents(String targetDir, CLDRFile english) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(targetDir + "/log/", "locales.txt");
        String[] locales = stock.split("\\|");
        Set<R2<String, String>> sorted = new TreeSet<R2<String, String>>();
        for (String locale : locales) {
            if (locale.isEmpty()) continue;
            String name = english.getName(locale);
            R2<String, String> row = Row.of(name, locale);
            sorted.add(row);
        }
        Set<String> defaultContents = supplementalDataInfo.getDefaultContentLocales();

        for (R2<String, String> row : sorted) {
            String locale = row.get1();
            String dlocale = getDefaultContentLocale(locale, defaultContents);
            out.println(row.get0() + "\t" + locale + "\t" + english.getName(dlocale) + "\t" + dlocale);
        }
        out.close();
    }

    private static String getDefaultContentLocale(String locale, Set<String> defaultContents) {
        String best = null;
        for (String s : defaultContents) {
            if (s.startsWith(locale)) {
                if (best == null) {
                    best = s;
                } else if (s.length() < best.length()) {
                    best = s;
                }
            }
        }
        if (best == null) {
            return locale;
        }
        return best;
    }

    static final Pattern COUNT_OR_ALT_ATTRIBUTE = PatternCache.get("\\[@(count)=\"([^\"]*)\"]");
    static final Pattern PLURAL_XPATH = Pattern
        .compile("//ldml/(units/unit|numbers/(decimal|currency)Formats).*\\[@count=\"\\w+\"].*");
    static final Pattern SKIP_EXEMPLAR_TEST = PatternCache.get(
        "/(currencySpacing"
            + "|hourFormat"
            + "|exemplarCharacters"
            + "|pattern"
            + "|localizedPatternChars"
            + "|segmentations"
            + "|dateFormatItem"
            + "|references"
            + "|unitPattern"
            + "|intervalFormatItem"
            + "|localeDisplayNames/variants/"
            + "|commonlyUsed"
            + "|currency.*/symbol"
            + "|symbols/(exponential|nan))");

    static final Matcher skipExemplarTest = SKIP_EXEMPLAR_TEST.matcher("");
    static final UnicodeSet ASCII_LATIN = new UnicodeSet("[A-Za-z]").freeze();
    static final UnicodeSet LATIN = new UnicodeSet("[:sc=Latn:]").freeze();

    static final Matcher keepFromRoot = PatternCache.get("/(exemplarCity|currencies/currency.*/symbol)").matcher("");
    static final Matcher currencyDisplayName = Pattern
        .compile("/currencies/currency\\[@type=\"([^\"]*)\"]/displayName").matcher("");

    private static void writeFile(String targetDir, String localeId, EnglishInfo englishInfo, CLDRFile cldrFile,
        boolean isEnglish, boolean filter) throws IOException {

        String extension = "xml";
        XPathParts xpathParts = new XPathParts();
        Relation<String, String> reasonsToPaths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> seenStarred = new HashSet<String>();

        Relation<String, Row.R2<PathInfo, String>> countItems = Relation.of(
            new TreeMap<String, Set<Row.R2<PathInfo, String>>>(), TreeSet.class);
        Matcher countMatcher = COUNT_OR_ALT_ATTRIBUTE.matcher("");
        int lineCount = 0;
        int wordCount = 0;
        int messageCount = 0;

        StringWriter buffer = new StringWriter();
        PrintWriter out1 = new PrintWriter(buffer);
        StringWriter buffer3 = new StringWriter();
        PrintWriter out3 = new PrintWriter(buffer3);
        UnicodeSet exemplars = getExemplars(cldrFile);

        for (PathInfo pathInfo : englishInfo) {
            if (false && pathInfo.id == 46139888945574604L) { // for debugging
                System.out.println("?");
            }
            String path = pathInfo.getPath();
            String value;
            if (isEnglish) {
                value = pathInfo.englishValue;
            } else {
                value = cldrFile.getStringValue(path);
            }
            // Remove quotes from number formats (we'll put them back in during
            // post-processing).
            // TODO: we should actually call daip.processForDisplay() here, but
            // it does more stuff than we need it to do, e.g. stripping the
            // brackets from exemplarCharacters.
            if (DisplayAndInputProcessor.NUMBER_FORMAT_XPATH.matcher(path).matches()) {
                value = value.replace("'", "");
            }

            // skip root if not English
            if (!isEnglish && value != null && !keepFromRoot.reset(path).find()) { // note that mismatched script will
                // be checked later
                String locale = cldrFile.getSourceLocaleID(path, null);
                if (locale.equals("root")) {
                    reasonsToPaths.put("root", path + "\t" + value);
                    continue;
                }
                if (locale.equals(XMLSource.CODE_FALLBACK_ID)) {
                    reasonsToPaths.put("codeFallback", path + "\t" + value);
                    continue;
                }
            }
            boolean hasPlurals = PLURAL_XPATH.matcher(path).matches();
            if (filter && !hasPlurals) {
                String starred = pathInfo.getStarredPath();
                if (seenStarred.contains(starred)) {
                    continue;
                }
                seenStarred.add(starred);
            }
            if (value == null) {
                reasonsToPaths.put("missing", path + "	" + value);
                continue;
            }
            if (!isEnglish) {
                String fullPath = cldrFile.getFullXPath(path);
                if (fullPath.contains("draft")) {
                    xpathParts.set(fullPath);
                    String draftValue = xpathParts.getAttributeValue(-1, "draft");
                    if (!draftValue.equals("contributed")) {
                        reasonsToPaths.put(draftValue, path + "\t" + value);
                        continue;
                    }
                }
            }
            if (!isEnglish
                && !exemplars.containsAll(value)
                && !skipExemplarTest.reset(path).find()) {
                // check for special cases in currency names. If the code itself occurs in the name, that's ok
                // ldml/numbers/currencies/currency[@type="XXX"]/displayName
                boolean bad = true;
                if (currencyDisplayName.reset(path).find()) {
                    String code = currencyDisplayName.group(1);
                    String value2 = value.replace(code, "");
                    bad = !exemplars.containsAll(value2);
                }
                if (bad) {
                    UnicodeSet diff = new UnicodeSet().addAll(value).removeAll(exemplars);
                    reasonsToPaths.put("exemplars", path + "\t" + value + "\t" + diff);
                    continue;
                }
            }
            // String fullPath = cldrFile.getStringValue(path);
            // //ldml/units/unit[@type="day"]/unitPattern[@count="one"]
            if (hasPlurals) {
                countMatcher.reset(path).find();
                String countLessPath = countMatcher.replaceAll("");
                countItems.put(countLessPath, Row.of(pathInfo, value));
                continue;
            }
            if (!isEnglish && pathInfo.changedEnglish) {
                reasonsToPaths.put("changed-english", path);
            } else {
                writePathInfo(out1, pathInfo, value, isEnglish);
                messageCount++;
            }
            if (isEnglish) {
                writeJavaInfo(out3, pathInfo.getStringId(), pathInfo.getPath(), value);
            }
            wordCount += pathInfo.wordCount;
            ++lineCount;
        }
        R2<Integer, Integer> lineWordCount = writeCountPathInfo(out1, out3, cldrFile.getLocaleID(), countItems,
            isEnglish, filter);
        messageCount += lineWordCount.get0();
        lineCount += lineWordCount.get0();
        wordCount += lineWordCount.get1();
        if (!filter && countItems.size() != lineWordCount.get0().intValue()) {
            System.out.println(localeId + "\t" + countItems.size() + "\t" + lineWordCount.get0().intValue());
        }
        out1.flush();
        out3.flush();

        String file = LanguageCodeConverter.toGoogleLocaleId(localeId);
        String localeName = englishInfo.getName(localeId);
        PrintWriter out = FileUtilities.openUTF8Writer(targetDir, file + "." + extension);

        if (isEnglish) {
            FileCopier.copy(GenerateXMB.class, "xmb-dtd.xml", out);
//            FileUtilities.appendFile(GenerateXMB.class, "xmb-dtd.xml", out);
            out.println("<!-- " + localeName + " -->");
            out.println("<messagebundle class='" + projectId + "'> <!-- version: " + DTD_VERSION + ", date: " + DATE
                + " -->");
            out.println(buffer.toString());
            out.println("</messagebundle>");

            PrintWriter out3File = FileUtilities.openUTF8Writer(targetDir, "IdToPath.java");
            out3File.println("package org.unicode.cldr.tool;");
            out3File.println();
            out3File.println("import java.util.HashMap;");
            out3File.println();
            out3File.println("/**");
            out3File.println(" * Autogenerated by GenerateXMB for use by ConvertXTB.");
            out3File.println(" * Do not manually edit this file.");
            out3File.println(" */");
            out3File.println("public class IdToPath {");
            out3File.println("  static final HashMap<String,String> map = new HashMap<String,String>();");
            out3File.println("  public static String getPath(String id) {");
            out3File.println("      return map.get(id);");
            out3File.println("  }");
            out3File.println("  static {");
            out3File.println("      String[][] data = {");
            out3File.println(buffer3);
            out3File.println("      };");
            out3File.println("      for (String[] pair : data) {");
            out3File.println("          map.put(pair[0], pair[1]);");
            out3File.println("      }");
            out3File.println("  }");
            out3File.println("}");
            out3File.close();
        } else {

//            FileUtilities.appendFile(GenerateXMB.class, "wsb-dtd.xml", out);
            FileCopier.copy(GenerateXMB.class, "wsb-dtd.xml", out);
            out.println("<!-- " + localeName + " -->");
            out.println("<worldserverbundles lazarus_id='dummy' date='" + DATE + "'> <!-- version: " + DTD_VERSION
                + " -->");
            out.println("  <worldserverbundle project_id='" + projectId + "' message_count='" + messageCount + "'>");
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

    private static void writeJavaInfo(PrintWriter out3, String id, String path, String value) {
        out3.println("              {\"" + id + "\",\"" + path.replace("\"", "\\\"") + "\",\""
            + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"},");
    }

    private static UnicodeSet getExemplars(CLDRFile cldrFile) {
        UnicodeSet exemplars = cldrFile.getExemplarSet("", CLDRFile.WinningChoice.WINNING);
        boolean isLatin = exemplars.containsSome(ASCII_LATIN);
        exemplars.addAll(CheckExemplars.AlwaysOK);
        UnicodeSet auxExemplars = cldrFile.getExemplarSet("auxiliary", CLDRFile.WinningChoice.WINNING);
        if (auxExemplars != null) {
            exemplars.addAll(auxExemplars);
        }
        if (!isLatin) {
            exemplars.removeAll(LATIN);
        }
        exemplars.freeze();
        return exemplars;
    }

    static final Pattern COUNT_ATTRIBUTE = PatternCache.get("\\[@count=\"([^\"]*)\"]");
    static final Pattern PLURAL_NUMBER = PatternCache.get("(decimal|number)Format");

    private static Row.R2<Integer, Integer> writeCountPathInfo(PrintWriter out, PrintWriter out3, String locale,
        Relation<String, R2<PathInfo, String>> countItems, boolean isEnglish, boolean filter) {
        Matcher m = COUNT_ATTRIBUTE.matcher("");
        int wordCount = 0;
        PluralInfo pluralInfo = supplementalDataInfo.getPlurals(locale);
        int lineCount = 0;
        Set<String> errorSet = new LinkedHashSet<String>();
        for (Entry<String, Set<R2<PathInfo, String>>> entry : countItems.keyValuesSet()) {
            String countLessPath = entry.getKey();
            Map<String, String> fullValues = new TreeMap<String, String>();
            PathInfo pathInfo = null;
            String value = null;
            for (R2<PathInfo, String> entry2 : entry.getValue()) {
                PathInfo pathInfoN = entry2.get0();
                m.reset(pathInfoN.getPath()).find();
                String count = m.group(1);
                if (count.equals("other")) {
                    pathInfo = pathInfoN;
                }
                value = entry2.get1();
                fullValues.put(count, value);
            }
            if (pathInfo == null) {
                continue;
            }
            if (fullValues.size() < 2) {
                // if we don't have two count values, skip
                System.out.println(locale + "\tMust have 2 count values: " + entry.getKey());
                continue;
            }
            String fullPlurals = showPlurals(fullValues, locale, pathInfo, pluralInfo, isEnglish, errorSet);
            if (fullPlurals == null) {
                System.out.println(locale + "\tCan't format plurals for: " + entry.getKey() + "\t" + errorSet);
                errors++;
                continue;
            }

            out.println();
            out.println("    <!--    "
                // + prettyPath.getPrettyPath(pathInfo.getPath(), false) + " ;    "
                + countLessPath + "    -->");
            out.println("    <msg id='" + pathInfo.getStringId() + "' desc='" + pathInfo.description + "'");
            out.println("     >" + fullPlurals + "</msg>");
            // Use the last plural value in the loop because we only need it for example purposes.
            writeJavaInfo(out3, pathInfo.getStringId(), countLessPath, value);
            // if (!isEnglish || pathInfo.placeholderReplacements != null) {
            // out.println("\t<!-- English original:\t" + pathInfo.getEnglishValue() + "\t-->");
            // }
            out.flush();
            ++lineCount;
            wordCount += pathInfo.wordCount * 3;
            if (filter) {
                break;
            }
        }
        return Row.of(lineCount, wordCount);
    }

    static final String[] PLURAL_KEYS = { "=0", "=1", "zero", "one", "two", "few", "many", "other" };
    static final String[] EXTRA_PLURAL_KEYS = { "0", "1", "zero", "one", "two", "few", "many" };

    private static String showPlurals(Map<String, String> values,
        String locale, PathInfo pathInfo, PluralInfo pluralInfo,
        boolean isEnglish, Set<String> errorSet) {
        errorSet.clear();
        /*
         * Desired output for English XMB
         * <msg desc=
         * "[ICU Syntax] Plural forms for a number of hours. These are special messages: before translating, see cldr.org/translation/plurals."
         * >
         * {LENGTH, select,
         * abbreviated {
         * {NUMBER_OF_HOURS, plural,
         * =0 {0 hrs}
         * =1 {1 hr}
         * zero {# hrs}
         * one {# hrs}
         * two {# hrs}
         * few {# hrs}
         * many {# hrs}
         * other {# hrs}}}
         * full {
         * {NUMBER_OF_HOURS, plural,
         * =0 {0 hours}
         * =1 {1 hour}
         * zero {# hours}
         * one {# hours}
         * two {# hours}
         * few {# hours}
         * many {# hours}
         * other {# hours}}}}
         * </msg>
         *
         * NOTE: For the WSB, the format has to match the following, WITHOUT LFs
         *
         * <msg id='1431840205484292448' desc='[ICU Syntax] who is viewing?​ This message requires special attention.
         * Please follow the instructions here:
         * https://sites.google.com/a/google.com/localization-info-site/Home/training/icusyntax'>
         * <ph name='[PLURAL_NUM_USERS_OFFSET_1]' ex='Special placeholder used in [ICU Syntax] messages, see
         * instructions page.'/>
         * <ph name='[​=0]'/>No one else is viewing.
         * <ph name='[=1]'/><ph name='USERNAME' ex='Bob'/> is viewing.
         * <ph name='[=2]'/><ph name='USERNAME' ex='Bob'/> and one other are viewing.
         * <ph name='[ZERO]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[ONE]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[TWO]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[FEW]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[MANY]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[OTHER]'/><ph name='USERNAME' ex='Bob'/> and # others are viewing.
         * <ph name='[END_PLURAL]'/>
         * </msg>
         */
        Matcher matcher = PLURAL_NUMBER.matcher(pathInfo.getPath());
        String var = null;
        if (matcher.find()) {
            // Plural doesn't use placeholders so create a label.
            var = matcher.group(1).toUpperCase() + "_NUMBER";
        } else {
            var = pathInfo.getFirstVariable();
        }

        StringBuilder result = new StringBuilder();
        if (isEnglish) {
            result.append('{')
                // .append("PLURAL_")
                .append(var).append(",plural,");
        } else {
            result.append("<ph name='[PLURAL_").append(var).append("]'/>"); // ex='Special placeholder used in [ICU
            // Syntax] messages, see instructions page.'
        }
        for (String key : PLURAL_KEYS) {
            String value;
            String coreKey = key.startsWith("=") ? key.substring(1, 2) : key;
            value = values.get(coreKey);
            if (value == null) {
                if (key.startsWith("=")) {
                    String stringCount = key.substring(1);
                    // handle both =x case, and the category
                    int intCount = Integer.parseInt(stringCount);
                    Count count = pluralInfo.getCount(intCount);
                    value = values.get(count.toString());
                    if (value == null) {
                        errorSet.add("Bad key/value " + key + "='" + value + "' in " + values);
                        return null;
                    }
                    value = value.replace("{0}", stringCount);
                } else {
                    value = values.get("other");
                    if (value == null) {
                        errorSet.add("No 'other' value in " + values);
                        return null;
                    }
                }
            }
            String newValue = MessageFormat.format(MessageFormat.autoQuoteApostrophe(value),
                new Object[] { key.startsWith("=") ? key.substring(1, 2) : "#" });
            PlaceholderType type = isEnglish ? PlaceholderType.BRACES : PlaceholderType.XML;
            newValue = pathInfo.transformValue(newValue, type);
            if (isEnglish) {
                result.append("\n            ").append(key).append(" {").append(newValue).append('}');
            } else {
                String prefix = key.toUpperCase(Locale.ENGLISH);
                result.append("<!--\n        --><ph name='[").append(prefix).append("]'/>").append(newValue);
            }
        }
        if (isEnglish) {
            result.append('}');
        } else {
            result.append("<!--\n        --><ph name='[END_PLURAL]'/>");
        }
        return result.toString();
    }

    private static void writePathInfo(PrintWriter out, PathInfo pathInfo, String value, boolean isEnglish) {
        out.println();
        out.println("    <!--    " + pathInfo.getPath() + "    -->");
        out.println("    <msg id='" + pathInfo.getStringId() + "' desc='" + pathInfo.description + "'");
        PlaceholderType type = isEnglish ? PlaceholderType.XML_EXAMPLE : PlaceholderType.XML;
        String transformValue = pathInfo.transformValue(value, type);
        out.println("     >" + transformValue + "</msg>");
        value = TransliteratorUtilities.toHTML.transform(value);
        if (!value.equals(transformValue) && (!isEnglish || pathInfo.placeholders != null)) {
            out.println("    <!-- English original:    " + value + "    -->");
        }
        out.flush();
    }

    private static void writeReasons(Relation<String, String> reasonsToPaths, String targetDir, String filename)
        throws IOException {
        targetDir += "/skipped/";
        filename += ".txt";
        PrintWriter out = FileUtilities.openUTF8Writer(targetDir, filename);
        out.println("# " + DATE);
        for (Entry<String, Set<String>> reasonToSet : reasonsToPaths.keyValuesSet()) {
            for (String path : reasonToSet.getValue()) {
                out.println(reasonToSet.getKey() + "    " + path);
            }
        }
        out.close();
    }

    static class PathInfo implements Comparable<PathInfo> {
        private static final Pattern PLACEHOLDER = PatternCache.get("\\{(\\d)}");

        private final String path;
        private final Long id;
        private final String stringId;
        private final String englishValue;
        private final boolean changedEnglish;
        private final Map<String, PlaceholderInfo> placeholders;
        private final String description;
        private final String starredPath;
        private final int wordCount;

        private static final BreakIterator bi = BreakIterator.getWordInstance(ULocale.ENGLISH);
        private static final UnicodeSet ALPHABETIC = new UnicodeSet("[:Alphabetic:]");

        public PathInfo(String path, String englishValue, boolean changedEnglish,
            Map<String, PlaceholderInfo> placeholders,
            String description, String starredPath) {
            if (DEBUG_PATH != null && path.contains(DEBUG_PATH)) {
                int x = 0;
            }
            if (description == null) {
                path2errors.put(path, "missing description");
            }
            this.path = path;
            long id = StringId.getId(path);
            this.id = id;
            stringId = String.valueOf(id);
            this.englishValue = englishValue;
            this.changedEnglish = changedEnglish;
            this.placeholders = placeholders;
            this.description = description == null ? null : description.intern();
            this.starredPath = starredPath;
            // count words
            int tempCount = 0;
            bi.setText(englishValue);
            int start = bi.first();
            for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
                String word = englishValue.substring(start, end);
                if (ALPHABETIC.containsSome(word)) {
                    ++tempCount;
                }
            }
            wordCount = tempCount == 0 ? 1 : tempCount;
        }

        public String getFirstVariable() {
            // ... name='FIRST_PART_OF_TEXT' ...
            PlaceholderInfo info = placeholders.get("{0}");
            if (info == null) {
                throw new IllegalArgumentException("Missing {0} for " + this);
            }
            return info.name;
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

        public String getDescription() {
            return description;
        }

        public String getStarredPath() {
            return starredPath;
        }

        public Map<String, String> getPlaceholderReplacementsToOriginal() {
            if (placeholders == null) return null;
            Map<String, String> placeholderOutput = new LinkedHashMap<String, String>();
            for (String id : placeholders.keySet()) {
                placeholderOutput.put(id, getPlaceholderWithExample(id));
            }
            return placeholderOutput;
        }

        private String getPlaceholderWithExample(String placeholder) {
            PlaceholderInfo info = placeholders.get(placeholder);
            // <ph name='x'><ex>xxx</ex>yyy</ph>
            return "<ph name='" + info.name + "'><ex>" + info.example + "</ex>" + placeholder + "</ph>";
        }

        // static DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();

        private String transformValue(String value, PlaceholderType type) {
            value = TransliteratorUtilities.toHTML.transform(value);
            if (placeholders == null) return value;

            String placeholderFormat = "";
            switch (type) {
            case BRACES:
                placeholderFormat = "'{'{0}'}'";
                break;
            case XML:
                placeholderFormat = "<ph name=''[{0}]'' />";
                break;
            case XML_EXAMPLE:
                placeholderFormat = "<ph name=''{0}''><ex>{1}</ex>'{'{2}'}'</ph>";
                break;
            }
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuffer buffer = new StringBuffer();
            int start = 0;
            while (matcher.find()) {
                buffer.append(value.substring(start, matcher.start()));
                PlaceholderInfo info = placeholders.get(matcher.group());
                buffer.append(MessageFormat.format(placeholderFormat,
                    new Object[] { info.name, info.example, matcher.group(1) }));
                start = matcher.end();
            }
            buffer.append(value.substring(start));
            return buffer.toString();
        }

        private String replacePlaceholders(String value, String placeholderStart, String placeholderEnd) {
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuffer buffer = new StringBuffer();
            int start = 0;
            while (matcher.find()) {
                buffer.append(value.substring(start, matcher.start()));
                String name = placeholders.get(matcher.group()).name;
                buffer.append(placeholderStart).append(name).append(placeholderEnd);
                start = matcher.end();
            }
            buffer.append(value.substring(start));
            return buffer.toString();
        }

        @Override
        public int compareTo(PathInfo arg0) {
            return path.compareTo(arg0.path);
        }

        public String toString() {
            return path;
        }
    }

    static class EnglishInfo implements Iterable<PathInfo> {

        final Map<String, PathInfo> pathToPathInfo = new TreeMap<String, PathInfo>();
        final Map<Long, PathInfo> longToPathInfo = new HashMap<Long, PathInfo>();
        final CLDRFile english;

        PathInfo getPathInfo(long hash) {
            return longToPathInfo.get(hash);
        }

        public String getName(String localeId) {
            return english.getName(localeId);
        }

        PathInfo getPathInfo(String path) {
            return pathToPathInfo.get(path);
        }

        EnglishInfo(String targetDir, CLDRFile english, CLDRFile root) throws Exception {

            Map<String, String> oldPathValueMap = ReadXMB.load(CLDRPaths.BASE_DIRECTORY +
                "/cldr-tools/org/unicode/cldr/unittest/data/xmb/",
                "en.xml");

            PatternPlaceholders patternPlaceholders = PatternPlaceholders.getInstance();

            this.english = english;
            // we don't want the fully resolved paths, but we do want the direct inheritance from root.
            //Status status = new Status();
            Map<String, List<Set<String>>> starredPaths = new TreeMap<String, List<Set<String>>>();

            HashSet<String> metazonePaths = new HashSet<String>();
            // ^//ldml/dates/timeZoneNames/metazone\[@type="([^"]*)"]
            for (MetazoneInfo metazoneInfo : MetazoneInfo.METAZONE_LIST) {
                for (String item : metazoneInfo.getTypes()) {
                    String path = "//ldml/dates/timeZoneNames/metazone[@type=\"" + metazoneInfo.metazoneId + "\"]"
                        + item;
                    metazonePaths.add(path);
                }
            }

            // TODO add short countries
            HashSet<String> extraLanguages = new HashSet<String>();
            // ldml/localeDisplayNames/languages/language[@type=".*"]

            for (String langId : PathDescription.EXTRA_LANGUAGES) {
                String langPath = "//ldml/localeDisplayNames/languages/language[@type=\"" + langId + "\"]";
                extraLanguages.add(langPath);
            }

            Set<String> sorted = Builder.with(new TreeSet<String>())
                .addAll(english)
                .removeAll(
                    new Transform<String, Boolean>() {
                        public Boolean transform(String source) {
                            return source.startsWith("//ldml/dates/timeZoneNames/metazone") ? Boolean.TRUE
                                : Boolean.FALSE;
                        }
                    })
                .get();
            sorted.addAll(metazonePaths);
            if (DEBUG) {
                TreeSet<String> diffs = new TreeSet<String>(extraLanguages);
                diffs.removeAll(sorted);
                System.out.println(diffs);
            }
            sorted.addAll(extraLanguages);

            // add the extra Count items.
            Map<String, String> extras = new HashMap<String, String>();
            Matcher m = COUNT_ATTRIBUTE.matcher("");

            for (String path : sorted) {
                if (path.contains("[@count=\"")) {
                    m.reset(path).find();
                    for (String key : EXTRA_PLURAL_KEYS) {
                        String path2 = path.substring(0, m.start(1)) + key + path.substring(m.end(1));
                        extras.put(path2, path);
                    }
                }
                // if (path.contains("ellipsis")) {
                // System.out.println(path);
                // }
            }
            sorted.addAll(extras.keySet());

            Relation<String, String> reasonsToPaths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
            Set<String> missingDescriptions = new TreeSet<String>();
            //Output<String[]> pathArguments = new Output<String[]>();

            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance("en");
            RegexLookup<Boolean> coverageAllow = new RegexLookup<Boolean>()
                .add("^//ldml/localeDisplayNames/keys/key", true)
                .add("^//ldml/localeDisplayNames/languages/language\\[@type=\"(jv|zxx|gsw|eo)\"]", true)
                .add("^//ldml/localeDisplayNames/scripts/script", true)
                .add("^//ldml/localeDisplayNames/types/type", true)
                .add(
                    "^//ldml/dates/calendars/calendar\\[@type=\"[^\"]*\"]/dayPeriods/dayPeriodContext\\[@type=\"format\"]",
                    true);

            // TODO: for each count='other' path, add the other keywords and values
            PathDescription pathDescription = new PathDescription(GenerateXMB.supplementalDataInfo, english, extras,
                starredPaths, PathDescription.ErrorHandling.SKIP);

            for (String path : sorted) {
                if (DEBUG_PATH != null && path.contains(DEBUG_PATH)) {
                    int x = 0;
                }
                String value = english.getStringValue(path);
                Level level = coverageLevel.getLevel(path);
                if (value == null) {
                    value = "[EMPTY]";
                    addSkipReasons(reasonsToPaths, "empty-value", level, path, value);
                    continue;
                }
                if (pathMatcher != null
                    && !pathMatcher.reset(path).find()) {
                    addSkipReasons(reasonsToPaths, "path-parameter", level, path, value);
                    continue;
                }
                PathStatus pathStatus = shouldSkipPath(path, value);
                if (pathStatus == PathStatus.SKIP) {
                    addSkipReasons(reasonsToPaths, "path-remove", level, path, value);
                    continue;
                }

                if (level.compareTo(Level.MODERN) > 0 && pathStatus != PathStatus.KEEP) {
                    if (coverageAllow.get(path) == null) { // HACK
                        addSkipReasons(reasonsToPaths, "coverage", level, path, value);
                        continue;
                    } else {
                        addSkipReasons(reasonsToPaths, "coverage*", level, path, value);
                        continue;
                        // System.out.println("Not skipping " + path);
                    }
                }

                String description = pathDescription.getDescription(path, value, level, null);
                EnumSet<PathDescription.Status> descriptionStatus = pathDescription.getStatus();
                if (!descriptionStatus.isEmpty()) {
                    addSkipReasons(reasonsToPaths, descriptionStatus.toString(), level, path, value);
                    description = null;
                } else {
                    description = "[ICU CLDR] " + description;
                }

                String oldValue = oldPathValueMap.get(path);
                boolean changedEnglish = !value.equals(oldValue);
                PathInfo row = new PathInfo(path, value, changedEnglish, patternPlaceholders.get(path), description,
                    pathDescription.getStarredPathOutput());

                if (description == PathDescription.MISSING_DESCRIPTION) {
                    missingDescriptions.add(pathDescription.getStarredPathOutput());
                }

                Long hash = row.getId();
                if (longToPathInfo.containsKey(hash)) {
                    throw new IllegalArgumentException("Id collision for "
                        + path + " and " + longToPathInfo.get(hash).getPath());
                }
                pathToPathInfo.put(path, row);
                longToPathInfo.put(hash, row);
                if (value.contains("{0}") && patternPlaceholders.get(path) == null) {
                    System.out.println("ERROR, no placeholders for {0}...: " + path + " ; " + value);
                }
            }

            PrintWriter out = FileUtilities.openUTF8Writer(targetDir + "/log/", "en-paths.txt");
            out.println("# " + DATE);
            for (Entry<String, List<Set<String>>> starredPath : starredPaths.entrySet()) {
                out.println(starredPath.getKey() + "\t\t" + starredPath.getValue());
            }
            out.close();
            out = FileUtilities.openUTF8Writer(targetDir + "/log/", "en-missingDescriptions.txt");
            out.println("# " + DATE);
            for (String starredPath : missingDescriptions) {
                // ^//ldml/dates/timeZoneNames/zone\[@type=".*"]/exemplarCity ; ROOT timezone ; The name of a city in:
                // {0}. See cldr.org/xxxx.
                out.println(toRegexPath(starredPath) + "\t;\tDESCRIPTION\t" + starredPaths.get(starredPath));
            }
            out.close();
            writeReasons(reasonsToPaths, targetDir, "en");
        }

        private String toRegexPath(String starredPath) {
            String result = starredPath.replace("[", "\\[");
            result = result.replace("\".*\"", "\"([^\"]*)\"");
            return "^" + result;
        }

        @Override
        public Iterator<PathInfo> iterator() {
            return pathToPathInfo.values().iterator();
        }
    }

    static void addSkipReasons(Relation<String, String> reasonsToPaths, String descriptionStatus, Level level,
        String path, String value) {
        reasonsToPaths.put(descriptionStatus + "\t" + level, path + "\t" + value);
    }

    // Get Date-Time in milliseconds
    private static long getDateTimeinMillis(int year, int month, int date) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, date);
        return cal.getTimeInMillis();
    }

    static final long START_TIME = getDateTimeinMillis(2000, 1, 0);
    static final long END_TIME = getDateTimeinMillis(2015, 1, 0);
    static final long DELTA_TIME = 15 * 60 * 1000;
    static final long MIN_DAYLIGHT_PERIOD = 90L * 24 * 60 * 60 * 1000;

    static final Set<String> HAS_DAYLIGHT;
    static {
        Set<String> hasDaylightTemp = new HashSet<String>();
        Date date = new Date();
        main: for (String zoneId : sc.getCanonicalTimeZones()) {
            TimeZone zone = TimeZone.getTimeZone(zoneId);
            for (long time = START_TIME + MIN_DAYLIGHT_PERIOD; time < END_TIME; time += MIN_DAYLIGHT_PERIOD) {
                date.setTime(time);
                if (zone.inDaylightTime(date)) {
                    hasDaylightTemp.add(zoneId);
                    if (false && !zone.useDaylightTime()) {
                        System.out.println(zoneId + "\tuseDaylightTime()==false, but \tinDaylightTime(/" + date
                            + "/)==true");
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
        Set<String> singularCountries = new HashSet<String>(
            Arrays.asList("CL EC ES NZ PT AQ FM GL KI UM PF".split(" ")));

        Map<String, Set<String>> countryToZoneSet = sc.getCountryToZoneSet();

        main: for (Entry<String, Set<String>> countryZones : countryToZoneSet.entrySet()) {
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
                            if (false)
                                System.out.println(country
                                    + " Difference at: " + new Date(time)
                                    + ", " + zone.getDisplayName() + " " + (offset / 1000.0 / 60 / 60)
                                    + ", " + initial.iterator().next().getDisplayName() + " "
                                    + (firstOffset / 1000.0 / 60 / 60));
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

        static final String[] GENERIC = { "/long/generic",
            // "/short/generic"
        };
        static final String[] DAYLIGHT = { "/long/generic", "/long/standard", "/long/daylight",
            // "/short/generic", "/short/standard", "/short/daylight"
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
            // Set<String> zones = supplementalDataInfo.getCanonicalTimeZones();
            ArrayList<MetazoneInfo> result = new ArrayList<MetazoneInfo>();

            Map<String, String> zoneToCountry = sc.getZoneToCounty();

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

                // TimeZone goldenZone = TimeZone.getTimeZone(golden);

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
            return sc.getZoneToCounty().get(golden)
                + "\t" + metazoneId
                + "\t" + golden
                + "\t" + (singleCountry ? "singleCountry" : "")
                + "\t" + (hasDaylight ? "useDaylightTime" : "")
            // + ": " + zonesForCountry
            // + "\t" + regionToZone;
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

        for (boolean singleCountry : new boolean[] { false }) {
            for (boolean hasDaylight : new boolean[] { false, true }) {
                for (MetazoneInfo mzone : MetazoneInfo.METAZONE_LIST) {
                    if (mzone.hasDaylight != hasDaylight) continue;
                    if (mzone.singleCountry != singleCountry) continue;
                    System.out.println(mzone);
                }
            }
        }
    }

    private static void displayWsb(String file, EnglishInfo info) {
        try {
            String[] parts = file.split("/");
            ULocale locale = new ULocale(parts[parts.length - 2]);
            FileInputStream fis = new FileInputStream(file);
            XMLReader xmlReader = XMLFileReader.createXMLReader(false);
            xmlReader.setErrorHandler(new MyErrorHandler());
            Map<String, String> data = new TreeMap<String, String>();
            xmlReader.setContentHandler(new MyContentHandler(locale, data, info));
            InputSource is = new InputSource(fis);
            is.setSystemId(file);
            xmlReader.parse(is);
            fis.close();
            for (Entry<String, String> entity : data.entrySet()) {
                String path = entity.getKey();
                String value = entity.getValue();
                PathInfo pathInfo = info.getPathInfo(path);
                System.out.println(value + "\t" + (pathInfo == null ? "?" : pathInfo.englishValue) + "\t" + path);
            }
        } catch (SAXParseException e) {
            System.out.println("\t" + "Can't read " + file);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (SAXException e) {
            System.out.println("\t" + "Can't read " + file);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (IOException e) {
            System.out.println("\t" + "Can't read " + file);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        }
    }

    static class MyErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXException {
            System.out.println("\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            System.out.println("\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            System.out.println("\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    static class MyContentHandler implements ContentHandler {
        private static final boolean SHOW = false;
        private Map<String, String> myData;
        private EnglishInfo info;
        private PathInfo lastPathInfo;
        private StringBuilder currentText = new StringBuilder();
        private long lastId;
        private String lastPluralTag;
        private Map<String, String> pluralTags = new LinkedHashMap<String, String>();
        private Set<String> pluralKeywords;

        public MyContentHandler(ULocale locale, Map<String, String> data, EnglishInfo info) {
            myData = data;
            this.info = info;
            PluralRules rules = PluralRules.forLocale(locale);
            pluralKeywords = Builder.with(new HashSet<String>()).addAll(rules.getKeywords()).add("0").add("1").freeze();
        }

        @Override
        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
            String chars = String.valueOf(arg0, arg1, arg2);
            // if (SHOW) System.out.println("\t characters\t" + chars);
            currentText.append(chars);
        }

        @Override
        public void endDocument() throws SAXException {
            if (SHOW) System.out.println("\t endDocument\t");
        }

        @Override
        public void endElement(String arg0, String arg1, String qName) throws SAXException {
            // if (SHOW) System.out.println("\t endElement\t" + arg0 + "\t" + arg1 + "\t" + qName);
            if (qName.equals("msg")) {
                String chars = currentText.toString().replace("\n", "").trim();
                if (lastPathInfo == null) {
                    System.out.println("***Missing path info for " + lastId + "\t" + chars);
                    // myData.put("*** Missing path: " + lastId, chars);
                } else if (pluralTags.size() != 0) {
                    for (Entry<String, String> pluralTagEntry : pluralTags.entrySet()) {
                        String pluralTag = pluralTagEntry.getKey();
                        String pluralTagValue = pluralTagEntry.getValue();
                        if (pluralKeywords.contains(pluralTag)) {
                            String fixedCount = lastPathInfo.path.replace("other", pluralTag);
                            myData.put(fixedCount, pluralTagValue);
                        } else {
                            System.out.println("***Skipping " + pluralTag + "\t" + pluralTagValue);
                        }
                    }
                    // myData.put(lastPathInfo.path, pluralTags.toString());
                    pluralTags.clear();
                } else {
                    myData.put(lastPathInfo.path, chars);
                }
                currentText.setLength(0);
            }
        }

        @Override
        public void endPrefixMapping(String arg0) throws SAXException {
            if (SHOW) System.out.println("\t endPrefixMapping\t" + arg0);
        }

        @Override
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
            if (SHOW) System.out.println("\t ignorableWhitespace\t" + String.valueOf(arg0, arg1, arg2));
        }

        @Override
        public void processingInstruction(String arg0, String arg1) throws SAXException {
            if (SHOW) System.out.println("\t processingInstruction\t" + arg0 + "\t" + arg1);
        }

        @Override
        public void setDocumentLocator(Locator arg0) {
            if (SHOW) System.out.println("\t setDocumentLocator\t" + arg0);
        }

        @Override
        public void skippedEntity(String arg0) throws SAXException {
            if (SHOW) System.out.println("\t skippedEntity\t" + arg0);
        }

        @Override
        public void startDocument() throws SAXException {
            if (SHOW) System.out.println("\t startDocument\t");
        }

        @Override
        public void startElement(String arg0, String arg1, String qName, Attributes arg3) throws SAXException {
            // if (SHOW) System.out.println("\t startElement\t" + arg0 + "\t" + arg1 + "\t" + qName + "\t" +
            // showAttributes(arg3));
            if (qName.equals("msg")) {
                lastId = Long.parseLong(arg3.getValue("id"));
                lastPathInfo = info.getPathInfo(lastId);
                currentText.setLength(0);
            } else if (qName.equals("ph")) {
                String name = arg3.getValue("name");
                String original = lastPathInfo.getPlaceholderReplacementsToOriginal().get(name);
                if (original != null) {
                    currentText.append(original);
                } else if (name.startsWith("[PLURAL_")) {
                    pluralTags.clear();
                    lastPluralTag = "[START_PLURAL]";
                } else {
                    String pluralTag = PLURAL_TAGS.get(name);
                    if (pluralTag != null) {
                        String chars = currentText.toString().replace("\n", "").trim();
                        pluralTags.put(lastPluralTag, chars);
                        currentText.setLength(0);
                        lastPluralTag = pluralTag;
                    } else {
                        System.out.println("***Can't find " + name + " in "
                            + lastPathInfo.getPlaceholderReplacementsToOriginal());
                    }
                }
            }
        }

        private String showAttributes(Attributes atts) {
            String result = "";
            for (int i = 0; i < atts.getLength(); ++i) {
                result += atts.getQName(i) + "=\"" + atts.getValue(i) + "\"\t";
            }
            return result;
        }

        @Override
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {
            if (SHOW) System.out.println("\t startPrefixMapping\t" + arg0 + "\t" + arg1);
        }
    }

    static final Map<String, String> PLURAL_TAGS = Builder.with(new HashMap<String, String>())
        .put("[​=0]", "0")
        .put("[=1]", "1")
        .put("[ZERO]", PluralRules.KEYWORD_ZERO)
        .put("[ONE]", PluralRules.KEYWORD_ONE)
        .put("[TWO]", PluralRules.KEYWORD_TWO)
        .put("[FEW]", PluralRules.KEYWORD_FEW)
        .put("[MANY]", PluralRules.KEYWORD_MANY)
        .put("[OTHER]", PluralRules.KEYWORD_OTHER)
        .put("[END_PLURAL]", "")
        .freeze();

    private static String compareDirectory;
}
