package org.unicode.cldr.test;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Type;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InputStreamFactory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Simple test that loads each file in the cldr directory, thus verifying that the DTD works, and
 * also checks that the PrettyPaths work.
 *
 * @author markdavis
 */
public class QuickCheck {
    private static final Set<String> skipAttributes =
            new HashSet<>(Arrays.asList(new String[] {"alt", "draft", "references"}));

    private static String localeRegex;

    private static boolean showInfo = false;

    private static String commonDirectory;
    private static String mainDirectory;

    private static boolean resolved;

    private static Exception[] internalException = new Exception[1];

    private static boolean verbose;

    public static void main(String[] args) throws IOException {
        CLDRConfig testInfo = ToolConfig.getToolInstance();
        Factory factory = testInfo.getCldrFactory();

        Path annotationsDir = Path.of(CLDRPaths.ANNOTATIONS_DIRECTORY);
        Set<String> skipped = new TreeSet<>();
        Multimap<String, String> differs = LinkedHashMultimap.create();
        String keycapPath = "//ldml/characterLabels/characterLabel[@type=\"keycap\"]";
        // SimpleFormatter sf =
        // SimpleFormatter.compile("//ldml/annotations/annotation[@cp=\"{0}\"];");
        SimpleFormatter configLocaleValueInt =
                SimpleFormatter.compile(
                        "locale={0};\taction=add;\tnew_path=//ldml/annotations/annotation[@cp=\"{2}\u20E3\"];\tnew_value={1}");

        String[][] metadata = {
            {"bn", "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়"},
            {
                "ha", "sifili", "daya", "biyu", "uku", "hudu", "biyar", "shida", "bakwai", "takwas",
                "tara"
            },
            {"mr", "शून्य", "एक", "दोन", "तीन", "चार", "पाच", "सहा", "सात", "आठ", "नऊ"},
            {
                "om", "zeeroo", "tokko", "lama", "sadii", "afur", "shan", "jaha", "torba", "saddet",
                "sagal"
            },
            {"pa", "ਸਿਫਰ", "ਇੱਕ", "ਦੋ", "ਤਿੰਨ", "ਚਾਰ", "ਪੰਜ", "ਛੇ", "ਸੱਤ", "ਅੱਠ", "ਨੌਂ"},
            {
                "te",
                "సున్న",
                "ఒకటి",
                "రెండు",
                "మూడు",
                "నాలుగు",
                "ఐదు",
                "ఆరు",
                "ఏడు",
                "ఎనిమిది",
                "తొమ్మిది"
            },
            {
                "fil", "zero", "isa", "dalawa", "tatlo", "apat", "lima", "anim", "pito", "walo",
                "siyam"
            },
            {
                "uz", "nol", "bir", "ikki", "uch", "toʻrt", "besh", "olti", "yetti", "sakkiz",
                "toʻqqiz"
            },
            {"zh_Hant_HK", "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"},
            {
                "kn", "ಶೂನ್ಯ", "ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು",
                "ಒಂಭತ್ತು"
            },
            {
                "ml", "പൂജ്യം", "ഒന്ന്", "രണ്ട്", "മൂന്ന്", "നാല്", "അഞ്ച്", "ആറ്", "ഏഴ്", "എട്ട്",
                "ഒമ്പത്"
            }
        };
        Map<String, List<String>> metadata2 =
                Arrays.asList(metadata).stream()
                        .collect(
                                Collectors.toMap(
                                        x -> x[0],
                                        x -> Arrays.asList(Arrays.copyOfRange(x, 1, x.length)),
                                        (oldValue, newValue) ->
                                                newValue, // If duplicate keys, keep the new value
                                        TreeMap::new));

        for (File files : ImmutableSortedSet.copyOf(annotationsDir.toFile().listFiles())) {
            String name = files.getName();
            if (!name.endsWith(".xml")) {
                continue;
            }

            String localeString = name.substring(0, name.length() - 4);
            ULocale locale = new ULocale(localeString);
            RuleBasedNumberFormat formatter =
                    new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);
            Type type = ULocale.ACTUAL_LOCALE;
            ULocale actualLocale = formatter.getLocale(type);
            List<String> metaItems = metadata2.get(localeString);
            boolean haveIcu = actualLocale.equals(locale);
            if (!haveIcu && metaItems == null) {
                continue;
            }
            String keyCapValue = factory.make(localeString, true).getStringValue(keycapPath);
            addEmoji(
                    skipped,
                    differs,
                    configLocaleValueInt,
                    localeString,
                    formatter,
                    metaItems,
                    haveIcu,
                    keyCapValue);
        }
        for (Entry<String, List<String>> entry : metadata2.entrySet()) {
            String localeString = entry.getKey();
            if (!skipped.contains(localeString)) {
                continue;
            }
            String keyCapValue = factory.make(localeString, false).getStringValue(keycapPath);
            addEmoji(
                    skipped,
                    differs,
                    configLocaleValueInt,
                    localeString,
                    null,
                    entry.getValue(),
                    false,
                    keyCapValue);
        }
        differs.asMap().entrySet().stream().forEach(System.out::println);

        if (true) return;

        checkStock(factory);
        verbose = CldrUtility.getProperty("verbose", "false", "true").matches("(?i)T|TRUE");
        localeRegex = CldrUtility.getProperty("locale", ".*");

        showInfo = CldrUtility.getProperty("showinfo", "false", "true").matches("(?i)T|TRUE");

        commonDirectory = CLDRPaths.COMMON_DIRECTORY; // Utility.getProperty("common",
        // Utility.COMMON_DIRECTORY);
        // if (commonDirectory == null) commonDirectory = Utility.COMMON_DIRECTORY
        // System.out.println("Main Source Directory: " + commonDirectory +
        // "\t\t(to change, use -DSOURCE=xxx, eg
        // -DSOURCE=C:/cvsdata/unicode/cldr/incoming/proposed/main)");

        mainDirectory = CldrUtility.getProperty("main", CLDRPaths.COMMON_DIRECTORY + "/main");
        // System.out.println("Main Source Directory: " + commonDirectory +
        // "\t\t(to change, use -DSOURCE=xxx, eg
        // -DSOURCE=C:/cvsdata/unicode/cldr/incoming/proposed/main)");

        resolved = CldrUtility.getProperty("resolved", "false", "true").matches("(?i)T|TRUE");

        boolean paths = CldrUtility.getProperty("paths", "true").matches("(?i)T|TRUE");

        pretty = CldrUtility.getProperty("pretty", "true").matches("(?i)T|TRUE");

        double startTime = System.currentTimeMillis();
        checkDtds();
        double deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");

        if (paths) {
            System.out.println("Checking paths");
            checkPaths();
            deltaTime = System.currentTimeMillis() - startTime;
            System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
            System.out.println("Basic Test Passes");
        }
    }

    private static void addEmoji(
            Set<String> skipped,
            Multimap<String, String> differs,
            SimpleFormatter configLocaleValueInt,
            String localeString,
            RuleBasedNumberFormat formatter,
            List<String> metaItems,
            boolean haveIcu,
            String keyCapValue) {
        for (int i = 0; i < 10; ++i) {
            String icu = haveIcu ? formatter.format(i) : null;
            String meta = metaItems == null ? null : metaItems.get(i);
            String best = icu != null ? icu : meta;
            if (best == null) {
                skipped.add(localeString);
                continue;
            }
            if (icu != null & meta != null) {
                differs.put(
                        localeString,
                        i
                                + " icu:"
                                + icu
                                + " "
                                + (!meta.equals(icu) ? "=" : "!=")
                                + " meta:"
                                + meta);
            }
            String iValue = String.valueOf(i);
            List<String> valueList = Arrays.asList(iValue, best, keyCapValue);
            String path =
                    configLocaleValueInt.format(
                            localeString, Joiners.VBAR_SP.join(valueList), iValue);
            System.out.println(path);
        }
    }

    private static void checkDtds() throws IOException {
        checkDtds(commonDirectory + "supplemental");
        checkDtds(commonDirectory + "collation");
        checkDtds(commonDirectory + "main");
        checkDtds(commonDirectory + "rbnf");
        checkDtds(commonDirectory + "segments");
        checkDtds(commonDirectory + "../test");
        checkDtds(commonDirectory + "transforms");
    }

    private static void checkDtds(String directory) throws IOException {
        File directoryFile = new File(directory);
        File[] listFiles = directoryFile.listFiles();
        String normalizedPath = PathUtilities.getNormalizedPathString(directoryFile);
        if (listFiles == null) {
            throw new IllegalArgumentException("Empty directory: " + normalizedPath);
        }
        System.out.println("Checking files for DTD errors in: " + normalizedPath);
        for (File fileName : listFiles) {
            if (!fileName.toString().endsWith(".xml")) {
                continue;
            }
            check(fileName);
        }
    }

    static class MyErrorHandler implements ErrorHandler {
        @Override
        public void error(SAXParseException exception) throws SAXException {
            System.out.println("\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            System.out.println("\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            System.out.println("\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    public static void check(File systemID) {
        try (InputStream fis = InputStreamFactory.createInputStream(systemID)) {
            //            FileInputStream fis = new FileInputStream(systemID);
            XMLReader xmlReader = XMLFileReader.createXMLReader(true);
            xmlReader.setErrorHandler(new MyErrorHandler());
            InputSource is = new InputSource(fis);
            is.setSystemId(systemID.toString());
            xmlReader.parse(is);
            //            fis.close();
        } catch (SAXException | IOException e) { // SAXParseException is a Subtype of SaxException
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        }
        //        catch (SAXException e) {
        //            System.out.println("\t" + "Can't read " + systemID);
        //            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        //        } catch (IOException e) {
        //            System.out.println("\t" + "Can't read " + systemID);
        //            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        //        }
    }

    static Matcher skipPaths =
            PatternCache.get("/identity" + "|/alias" + "|\\[@alt=\"proposed").matcher("");

    private static boolean pretty;

    private static void checkPaths() {
        Relation<String, String> distinguishing =
                Relation.<String, String>of(
                        new TreeMap<String, Set<String>>(), TreeSet.class, null);
        Relation<String, String> nonDistinguishing =
                Relation.<String, String>of(
                        new TreeMap<String, Set<String>>(), TreeSet.class, null);
        Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
        CLDRFile english = cldrFactory.make("en", true);

        Relation<String, String> pathToLocale =
                Relation.of(
                        new TreeMap<String, Set<String>>(CLDRFile.getComparator(DtdType.ldml)),
                        TreeSet.class,
                        null);
        for (String locale : cldrFactory.getAvailable()) {
            // if (locale.equals("root") && !localeRegex.equals("root"))
            // continue;
            CLDRFile file;
            try {
                file = cldrFactory.make(locale, resolved);
            } catch (Exception e) {
                System.out.println("\nfatalError: " + e.getMessage());
                continue;
            }
            if (file.isNonInheriting()) continue;
            DisplayAndInputProcessor displayAndInputProcessor =
                    new DisplayAndInputProcessor(file, false);

            System.out.println(
                    locale + "\t-\t" + english.nameGetter().getNameFromIdentifier(locale));
            DtdType dtdType = null;

            for (String path : file) {
                if (path.endsWith("/alias")) {
                    continue;
                }
                String value = file.getStringValue(path);
                if (value == null) {
                    throw new IllegalArgumentException(
                            locale + "\tError: in null value at " + path);
                }
                String displayValue = displayAndInputProcessor.processForDisplay(path, value);
                if (!displayValue.equals(value)) {
                    System.out.println(
                            "\t"
                                    + locale
                                    + "\tdisplayAndInputProcessor changes display value <"
                                    + value
                                    + ">\t=>\t<"
                                    + displayValue
                                    + ">\t\t"
                                    + path);
                }
                String inputValue =
                        displayAndInputProcessor.processInput(path, value, internalException);
                if (internalException[0] != null) {
                    System.out.println(
                            "\t"
                                    + locale
                                    + "\tdisplayAndInputProcessor internal error <"
                                    + value
                                    + ">\t=>\t<"
                                    + inputValue
                                    + ">\t\t"
                                    + path);
                    internalException[0].printStackTrace(System.out);
                }
                if (verbose && !inputValue.equals(value)) {
                    displayAndInputProcessor.processInput(
                            path, value, internalException); // for debugging
                    System.out.println(
                            "\t"
                                    + locale
                                    + "\tdisplayAndInputProcessor changes input value <"
                                    + value
                                    + ">\t=>\t<"
                                    + inputValue
                                    + ">\t\t"
                                    + path);
                }

                pathToLocale.put(path, locale);

                // also check for non-distinguishing attributes
                if (path.contains("/identity")) continue;

                // make sure we don't have problem alts
                if (path.contains("proposed")) {
                    String sourceLocale = file.getSourceLocaleID(path, null);
                    if (locale.equals(sourceLocale)) {
                        String nonAltPath = CLDRFile.getNondraftNonaltXPath(path);
                        if (!path.equals(nonAltPath)) {
                            String nonAltLocale = file.getSourceLocaleID(nonAltPath, null);
                            String nonAltValue = file.getStringValue(nonAltPath);
                            if (nonAltValue == null || !locale.equals(nonAltLocale)) {
                                System.out.println(
                                        "\t"
                                                + locale
                                                + "\tProblem alt=proposed <"
                                                + value
                                                + ">\t\t"
                                                + path);
                            }
                        }
                    }
                }

                String fullPath = file.getFullXPath(path);
                XPathParts parts = XPathParts.getFrozenInstance(fullPath);
                if (dtdType == null) {
                    dtdType = DtdType.valueOf(parts.getElement(0));
                }
                for (int i = 0; i < parts.size(); ++i) {
                    if (parts.getAttributeCount(i) == 0) continue;
                    String element = parts.getElement(i);
                    for (String attribute : parts.getAttributeKeys(i)) {
                        if (skipAttributes.contains(attribute)) continue;
                        if (CLDRFile.isDistinguishing(dtdType, element, attribute)) {
                            distinguishing.put(element, attribute);
                        } else {
                            nonDistinguishing.put(element, attribute);
                        }
                    }
                }
            }
        }
        System.out.println();

        System.out.format(
                "Distinguishing Elements: %s" + CldrUtility.LINE_SEPARATOR, distinguishing);
        System.out.format(
                "Nondistinguishing Elements: %s" + CldrUtility.LINE_SEPARATOR, nonDistinguishing);
        System.out.format("Skipped %s" + CldrUtility.LINE_SEPARATOR, skipAttributes);

        if (pretty) {
            if (showInfo) {
                System.out.println(
                        CldrUtility.LINE_SEPARATOR
                                + "Showing Path to PrettyPath mapping"
                                + CldrUtility.LINE_SEPARATOR);
            }
            PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
            Set<String> badPaths = new TreeSet<>();
            for (String path : pathToLocale.keySet()) {
                String prettied = prettyPath.getPrettyPath(path, false);
                if (showInfo) System.out.println(prettied + "\t\t" + path);
                if (prettied.contains("%%") && !path.contains("/alias")) {
                    badPaths.add(path);
                }
            }
            // now remove root

            if (showInfo) {
                System.out.println(
                        CldrUtility.LINE_SEPARATOR
                                + "Showing Paths not in root"
                                + CldrUtility.LINE_SEPARATOR);
            }

            CLDRFile root = cldrFactory.make("root", true);
            for (String path : root) {
                pathToLocale.removeAll(path);
            }
            if (showInfo)
                for (String path : pathToLocale.keySet()) {
                    if (skipPaths.reset(path).find()) {
                        continue;
                    }
                    System.out.println(path + "\t" + pathToLocale.getAll(path));
                }

            if (badPaths.size() != 0) {
                System.out.println(
                        "Error: "
                                + badPaths.size()
                                + " Paths were not prettied: use -DSHOW and look for ones with %% in them.");
            }
        }
    }

    static void checkStock(Factory factory) {
        String[][] items = {
            {"full", "yMMMMEEEEd", "jmmsszzzz"},
            {"long", "yMMMMd", "jmmssz"},
            {"medium", "yMMMd", "jmmss"},
            {"short", "yMd", "jmm"},
        };
        String calendarID = "gregorian";
        String datetimePathPrefix =
                "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/";

        int total = 0;
        int mismatch = 0;
        LanguageTagParser ltp = new LanguageTagParser();
        Iterable<String> locales =
                StandardCodes.make()
                        .getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
        for (String locale : locales) {
            if (!ltp.set(locale).getRegion().isEmpty()) {
                continue;
            }
            CLDRFile file = factory.make(locale, false);
            DateTimeFormats dtf = new DateTimeFormats(file, "gregorian", false);
            for (String[] stockInfo : items) {
                String length = stockInfo[0];
                // ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="full"]/dateFormat[@type="standard"]/pattern[@type="standard"]
                String path =
                        datetimePathPrefix
                                + "dateFormats/dateFormatLength[@type=\""
                                + length
                                + "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String stockDatePattern = file.getStringValue(path);
                String flexibleDatePattern = dtf.getBestPattern(stockInfo[1]);
                mismatch +=
                        showStatus(
                                ++total,
                                locale,
                                "date",
                                length,
                                stockInfo[1],
                                stockDatePattern,
                                flexibleDatePattern);
                path =
                        datetimePathPrefix
                                + "timeFormats/timeFormatLength[@type=\""
                                + length
                                + "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String stockTimePattern = file.getStringValue(path);
                String flexibleTimePattern = dtf.getBestPattern(stockInfo[2]);
                mismatch +=
                        showStatus(
                                ++total,
                                locale,
                                "time",
                                length,
                                stockInfo[2],
                                stockTimePattern,
                                flexibleTimePattern);
            }
        }
        System.out.println("Mismatches:\t" + mismatch + "\tTotal:\t" + total);
    }

    static final Date SAMPLE_DATE = new Date(2013 - 1900, 1 - 1, 29, 13, 59, 59);

    private static int showStatus(
            int total,
            String locale,
            String type,
            String length,
            String skeleton,
            String stockPattern,
            String flexiblePattern) {
        ULocale ulocale = new ULocale(locale);
        DateFormatSymbols dfs = new DateFormatSymbols(ulocale); // just use ICU for now
        boolean areSame = Objects.equals(stockPattern, flexiblePattern);
        System.out.println(
                total
                        + "\t"
                        + (areSame ? "ok" : "diff")
                        + "\t"
                        + locale
                        + "\t"
                        + type
                        + "\t"
                        + length
                        + "\t"
                        + skeleton
                        + "\t"
                        + stockPattern
                        + "\t"
                        + (areSame ? "" : flexiblePattern)
                        + "\t'"
                        + new SimpleDateFormat(stockPattern, dfs, ulocale).format(SAMPLE_DATE)
                        + "\t'"
                        + (areSame
                                ? ""
                                : new SimpleDateFormat(flexiblePattern, dfs, ulocale)
                                        .format(SAMPLE_DATE)));
        return areSame ? 0 : 1;
    }
}
