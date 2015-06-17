package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.GenerateBirth.Versions;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CharacterFallbacks;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DiscreteComparator;
import org.unicode.cldr.util.DiscreteComparator.Ordering;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ElementType;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InputStreamFactory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

public class TestBasic extends TestFmwkPlus {

    private static final boolean TEST_VERSIONS = false;

    static TestInfo testInfo = TestInfo.getInstance();

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo
        .getSupplementalDataInfo();
    
    /**
     * Simple test that loads each file in the cldr directory, thus verifying
     * that the DTD works, and also checks that the PrettyPaths work.
     * 
     * @author markdavis
     */

    public static void main(String[] args) {
        new TestBasic().run(args);
    }

    private static final Set<String> skipAttributes = new HashSet<String>(
        Arrays.asList("alt", "draft", "references"));

    private final String localeRegex = CldrUtility.getProperty("locale", ".*");
    private final Set<String> eightPointLocales = new TreeSet<String>(Arrays.asList("ar ca cs da de el es fi fr he hi hr hu id it ja ko lt lv nb nl pl pt pt_PT ro ru sk sl sr sv th tr uk vi zh zh_Hant".split(" ")));

    private final String mainDirectory = CLDRPaths.MAIN_DIRECTORY;

    // private final boolean showForceZoom = Utility.getProperty("forcezoom",
    // false);

    private final boolean resolved = CldrUtility.getProperty("resolved", false);

    private final Exception[] internalException = new Exception[1];

    public void TestDtds() throws IOException {
        Relation<Row.R2<DtdType, String>, String> foundAttributes = Relation
            .of(new TreeMap<Row.R2<DtdType, String>, Set<String>>(),
                TreeSet.class);
        final CLDRConfig config = CLDRConfig.getInstance();
        final File basedir = config.getCldrBaseDirectory();
        List<TimingInfo> data = new ArrayList<>();

        for (String subdir : config.getCLDRDataDirectories()) {
            checkDtds(new File(basedir, subdir), 0, foundAttributes, data);
        }
        if (foundAttributes.size() > 0) {
            showFoundElements(foundAttributes);
        }
        if (isVerbose()) {
            long totalBytes = 0;
            long totalNanos = 0;
            for (TimingInfo i : data) {
                long length = i.file.length();
                totalBytes += length;
                totalNanos += i.nanos;
                logln(i.nanos + "\t" + length + "\t" + i.file);
            }
            logln(totalNanos + "\t" + totalBytes);
        }
    }

    private void checkDtds(File directoryFile, int level,
        Relation<R2<DtdType, String>, String> foundAttributes,
        List<TimingInfo> data) throws IOException {
        boolean deepCheck = getInclusion() >= 10;
        File[] listFiles = directoryFile.listFiles();
        String canonicalPath = directoryFile.getCanonicalPath();
        String indent = Utility.repeat("\t", level);
        if (listFiles == null) {
            throw new IllegalArgumentException(indent + "Empty directory: "
                + canonicalPath);
        }
        logln("Checking files for DTD errors in: " + indent + canonicalPath);
        for (File fileName : listFiles) {
            String name = fileName.getName();
            if (CLDRConfig.isJunkFile(name)) {
                continue;
            } else if (fileName.isDirectory()) {
                checkDtds(fileName, level + 1, foundAttributes, data);
            } else if (name.endsWith(".xml")) {
                data.add(check(fileName));
                if (deepCheck // takes too long to do all the time
                    // fileName.getCanonicalPath().compareTo("/Users/markdavis/workspace/cldr/common/supplemental")
                    // >= 0
                    ) {
                    CLDRFile cldrfile = CLDRFile.loadFromFile(fileName, "temp",
                        DraftStatus.unconfirmed);
                    for (String xpath : cldrfile) {
                        String fullPath = cldrfile.getFullXPath(xpath);
                        if (fullPath == null) {
                            fullPath = cldrfile.getFullXPath(xpath);
                            assertNotNull("", fullPath);
                            continue;
                        }
                        XPathParts parts = XPathParts
                            .getFrozenInstance(fullPath);
                        DtdType type = parts.getDtdData().dtdType;
                        for (int i = 0; i < parts.size(); ++i) {
                            String element = parts.getElement(i);
                            if (element.equals("reset")) {
                                int debug = 1;
                            }
                            R2<DtdType, String> typeElement = Row.of(type,
                                element);
                            if (parts.getAttributeCount(i) == 0) {
                                foundAttributes.put(typeElement, "NONE");
                            } else {
                                for (String attribute : parts
                                    .getAttributeKeys(i)) {
                                    foundAttributes.put(typeElement, attribute);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void showFoundElements(
        Relation<Row.R2<DtdType, String>, String> foundAttributes) {
        Relation<Row.R2<DtdType, String>, String> theoryAttributes = Relation
            .of(new TreeMap<Row.R2<DtdType, String>, Set<String>>(),
                TreeSet.class);
        for (DtdType type : DtdType.values()) {
            DtdData dtdData = DtdData.getInstance(type);
            for (Element element : dtdData.getElementFromName().values()) {
                String name = element.getName();
                Set<Attribute> attributes = element.getAttributes().keySet();
                R2<DtdType, String> typeElement = Row.of(type, name);
                if (attributes.isEmpty()) {
                    theoryAttributes.put(typeElement, "NONE");
                } else {
                    for (Attribute attribute : attributes) {
                        theoryAttributes.put(typeElement, attribute.name);
                    }
                }
            }
        }
        Relation<String, R3<Boolean, DtdType, String>> attributesToTypeElementUsed = Relation
            .of(new TreeMap<String, Set<R3<Boolean, DtdType, String>>>(),
                LinkedHashSet.class);

        for (Entry<R2<DtdType, String>, Set<String>> s : theoryAttributes
            .keyValuesSet()) {
            R2<DtdType, String> typeElement = s.getKey();
            Set<String> theoryAttributeSet = s.getValue();
            DtdType type = typeElement.get0();
            String element = typeElement.get1();
            if (element.equals("ANY") || element.equals("PCDATA")) {
                continue;
            }
            boolean deprecatedElement = SUPPLEMENTAL_DATA_INFO.isDeprecated(
                type, element, "*", "*");
            String header = type + "\t" + element + "\t"
                + (deprecatedElement ? "X" : "") + "\t";
            Set<String> usedAttributes = foundAttributes.get(typeElement);
            Set<String> unusedAttributes = new LinkedHashSet<String>(
                theoryAttributeSet);
            if (usedAttributes == null) {
                System.out.println(header
                    + "<NOT-FOUND>\t\t"
                    + siftDeprecated(type, element, unusedAttributes,
                        attributesToTypeElementUsed, false));
                continue;
            }
            unusedAttributes.removeAll(usedAttributes);
            System.out.println(header
                + siftDeprecated(type, element, usedAttributes,
                    attributesToTypeElementUsed, true)
                    + "\t"
                    + siftDeprecated(type, element, unusedAttributes,
                        attributesToTypeElementUsed, false));
        }

        System.out.println("Undeprecated Attributes\t");
        for (Entry<String, R3<Boolean, DtdType, String>> s : attributesToTypeElementUsed
            .keyValueSet()) {
            R3<Boolean, DtdType, String> typeElementUsed = s.getValue();
            System.out.println(s.getKey() + "\t" + typeElementUsed.get0()
                + "\t" + typeElementUsed.get1() + "\t"
                + typeElementUsed.get2());
        }
    }

    private String siftDeprecated(
        DtdType type,
        String element,
        Set<String> attributeSet,
        Relation<String, R3<Boolean, DtdType, String>> attributesToTypeElementUsed,
        boolean used) {
        StringBuilder b = new StringBuilder();
        StringBuilder bdep = new StringBuilder();
        for (String attribute : attributeSet) {
            String attributeName = "«"
                + attribute
                + (CLDRFile.isDistinguishing(type, element, attribute) ? "*"
                    : "") + "»";
            if (SUPPLEMENTAL_DATA_INFO.isDeprecated(type, element, attribute,
                "*")) {
                if (bdep.length() != 0) {
                    bdep.append(" ");
                }
                bdep.append(attributeName);
            } else {
                if (b.length() != 0) {
                    b.append(" ");
                }
                b.append(attributeName);
                if (!"NONE".equals(attribute)) {
                    attributesToTypeElementUsed.put(attribute,
                        Row.of(used, type, element));
                }
            }
        }
        return b.toString() + "\t" + bdep.toString();
    }

    class MyErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXException {
            errln("error: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            errln("fatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            errln("warning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    private class TimingInfo {
        File file;
        long nanos;
    }

    public TimingInfo check(File systemID) {
        long start = System.nanoTime();
        try (InputStream fis = InputStreamFactory.createInputStream(systemID)) {
            // FileInputStream fis = new FileInputStream(systemID);
            XMLReader xmlReader = XMLFileReader.createXMLReader(true);
            xmlReader.setErrorHandler(new MyErrorHandler());
            InputSource is = new InputSource(fis);
            is.setSystemId(systemID.toString());
            xmlReader.parse(is);
            // fis.close();
        } catch (SAXException | IOException e) {
            errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t"
                + e.getMessage());
        }
        // catch (SAXParseException e) {
        // errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" +
        // e.getMessage());
        // } catch (IOException e) {
        // errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" +
        // e.getMessage());
        // }
        TimingInfo timingInfo = new TimingInfo();
        timingInfo.nanos = System.nanoTime() - start;
        timingInfo.file = systemID;
        return timingInfo;
    }

    public void TestCurrencyFallback() {
        XPathParts parts = new XPathParts();
        Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
        Set<String> currencies = StandardCodes.make().getAvailableCodes(
            "currency");

        final UnicodeSet CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS = (UnicodeSet) new UnicodeSet(
            "[[:sc:]-[\\u0000-\\u00FF]]").freeze();

        CharacterFallbacks fallbacks = CharacterFallbacks.make();

        for (String locale : cldrFactory.getAvailable()) {
            CLDRFile file = cldrFactory.make(locale, false);
            if (file.isNonInheriting())
                continue;

            final UnicodeSet OK_CURRENCY_FALLBACK = (UnicodeSet) new UnicodeSet(
                "[\\u0000-\\u00FF]").addAll(safeExemplars(file, ""))
                .addAll(safeExemplars(file, "auxiliary"))
                .addAll(safeExemplars(file, "currencySymbol")).freeze();
            UnicodeSet badSoFar = new UnicodeSet();

            for (Iterator<String> it = file.iterator(); it.hasNext();) {
                String path = it.next();
                if (path.endsWith("/alias")) {
                    continue;
                }
                String value = file.getStringValue(path);

                // check for special characters

                if (CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS.containsSome(value)) {

                    parts.set(path);
                    if (!parts.getElement(-1).equals("symbol")) {
                        continue;
                    }
                    // We don't care about fallbacks for narrow currency symbols
                    if ("narrow".equals(parts.getAttributeValue(-1, "alt"))) {
                        continue;
                    }
                    String currencyType = parts.getAttributeValue(-2, "type");

                    UnicodeSet fishy = new UnicodeSet().addAll(value)
                        .retainAll(CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS)
                        .removeAll(badSoFar);
                    for (UnicodeSetIterator it2 = new UnicodeSetIterator(fishy); it2
                        .next();) {
                        final int fishyCodepoint = it2.codepoint;
                        List<String> fallbackList = fallbacks
                            .getSubstitutes(fishyCodepoint);

                        String nfkc = Normalizer.normalize(fishyCodepoint,
                            Normalizer.NFKC);
                        if (!nfkc.equals(UTF16.valueOf(fishyCodepoint))) {
                            if (fallbackList == null) {
                                fallbackList = new ArrayList<String>();
                            } else {
                                fallbackList = new ArrayList<String>(
                                    fallbackList); // writable
                            }
                            fallbackList.add(nfkc);
                        }
                        // later test for all Latin-1
                        if (fallbackList == null) {
                            errln("Locale:\t" + locale
                                + ";\tCharacter with no fallback:\t"
                                + it2.getString() + "\t"
                                + UCharacter.getName(fishyCodepoint));
                            badSoFar.add(fishyCodepoint);
                        } else {
                            String fallback = null;
                            for (String fb : fallbackList) {
                                if (OK_CURRENCY_FALLBACK.containsAll(fb)) {
                                    if (!fb.equals(currencyType)
                                        && currencies.contains(fb)) {
                                        errln("Locale:\t"
                                            + locale
                                            + ";\tCurrency:\t"
                                            + currencyType
                                            + ";\tFallback converts to different code!:\t"
                                            + fb
                                            + "\t"
                                            + it2.getString()
                                            + "\t"
                                            + UCharacter
                                            .getName(fishyCodepoint));
                                    }
                                    if (fallback == null) {
                                        fallback = fb;
                                    }
                                }
                            }
                            if (fallback == null) {
                                errln("Locale:\t"
                                    + locale
                                    + ";\tCharacter with no good fallback (exemplars+Latin1):\t"
                                    + it2.getString() + "\t"
                                    + UCharacter.getName(fishyCodepoint));
                                badSoFar.add(fishyCodepoint);
                            } else {
                                logln("Locale:\t" + locale
                                    + ";\tCharacter with good fallback:\t"
                                    + it2.getString() + " "
                                    + UCharacter.getName(fishyCodepoint)
                                    + " => " + fallback);
                                // badSoFar.add(fishyCodepoint);
                            }
                        }
                    }
                }
            }
        }
    }

    public void TestAbstractPaths() {
        Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
        CLDRFile english = cldrFactory.make("en", true);
        Map<String, Counter<Level>> abstactPaths = new TreeMap<String, Counter<Level>>();
        RegexTransform abstractPathTransform = new RegexTransform(
            RegexTransform.Processing.ONE_PASS).add("//ldml/", "")
            .add("\\[@alt=\"[^\"]*\"\\]", "").add("=\"[^\"]*\"", "=\"*\"")
            .add("([^]])\\[", "$1\t[").add("([^]])/", "$1\t/")
            .add("/", "\t");

        for (String locale : getInclusion() <= 5 ? eightPointLocales : cldrFactory.getAvailable()) {
            // if (locale.equals("root") && !localeRegex.equals("root"))
            // continue;
            CLDRFile file = cldrFactory.make(locale, resolved);
            if (file.isNonInheriting())
                continue;
            logln(locale + "\t-\t" + english.getName(locale));

            for (Iterator<String> it = file.iterator(); it.hasNext();) {
                String path = it.next();
                if (path.endsWith("/alias")) {
                    continue;
                }
                // collect abstracted paths
                String abstractPath = abstractPathTransform.transform(path);
                Level level = SUPPLEMENTAL_DATA_INFO.getCoverageLevel(path,
                    locale);
                if (level == Level.OPTIONAL) {
                    level = Level.COMPREHENSIVE;
                }
                Counter<Level> row = abstactPaths.get(abstractPath);
                if (row == null) {
                    abstactPaths.put(abstractPath, row = new Counter<Level>());
                }
                row.add(level, 1);
            }
        }
        logln(CldrUtility.LINE_SEPARATOR + "Abstract Paths");
        for (Entry<String, Counter<Level>> pathInfo : abstactPaths.entrySet()) {
            String path = pathInfo.getKey();
            Counter<Level> counter = pathInfo.getValue();
            logln(counter.getTotal() + "\t" + getCoverage(counter) + "\t"
                + path);
        }
    }

    private CharSequence getCoverage(Counter<Level> counter) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Level level : counter.getKeysetSortedByKey()) {
            if (first) {
                first = false;
            } else {
                result.append(' ');
            }
            result.append("L").append(level.ordinal()).append("=")
            .append(counter.get(level));
        }
        return result;
    }

    // public void TestCLDRFileCache() {
    // long start = System.nanoTime();
    // Factory cldrFactory = testInfo.getCldrFactory();
    // String unusualLocale = "hi";
    // CLDRFile file = cldrFactory.make(unusualLocale, true);
    // long afterOne = System.nanoTime();
    // logln("First: " + (afterOne-start));
    // CLDRFile file2 = cldrFactory.make(unusualLocale, true);
    // long afterTwo = System.nanoTime();
    // logln("Second: " + (afterTwo-afterOne));
    // }
    //
    public void TestPaths() {
        Relation<String, String> distinguishing = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> nonDistinguishing = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        XPathParts parts = new XPathParts();
        Factory cldrFactory = testInfo.getCldrFactory();
        CLDRFile english = cldrFactory.make("en", true);

        Relation<String, String> pathToLocale = Relation.of(
            new TreeMap<String, Set<String>>(CLDRFile
                .getComparator(DtdType.ldml)), TreeSet.class, null);
        Set<String> localesToTest = getInclusion() <= 5 ? eightPointLocales : cldrFactory.getAvailable();
        for (String locale : localesToTest ) {
            CLDRFile file = cldrFactory.make(locale, resolved);
            DtdType dtdType = null;
            if (file.isNonInheriting())
                continue;
            DisplayAndInputProcessor displayAndInputProcessor = new DisplayAndInputProcessor(
                file, false);

            logln(locale + "\t-\t" + english.getName(locale));

            for (Iterator<String> it = file.iterator(); it.hasNext();) {
                String path = it.next();
                if (dtdType == null) {
                    dtdType = DtdType.fromPath(path);
                }

                if (path.endsWith("/alias")) {
                    continue;
                }
                String value = file.getStringValue(path);
                if (value == null) {
                    throw new IllegalArgumentException(locale
                        + "\tError: in null value at " + path);
                }

                String displayValue = displayAndInputProcessor
                    .processForDisplay(path, value);
                if (!displayValue.equals(value)) {
                    logln("\t"
                        + locale
                        + "\tdisplayAndInputProcessor changes display value <"
                        + value + ">\t=>\t<" + displayValue + ">\t\t"
                        + path);
                }
                String inputValue = displayAndInputProcessor.processInput(path,
                    value, internalException);
                if (internalException[0] != null) {
                    errln("\t" + locale
                        + "\tdisplayAndInputProcessor internal error <"
                        + value + ">\t=>\t<" + inputValue + ">\t\t" + path);
                    internalException[0].printStackTrace(System.out);
                }
                if (isVerbose() && !inputValue.equals(value)) {
                    displayAndInputProcessor.processInput(path, value,
                        internalException); // for
                    // debugging
                    logln("\t"
                        + locale
                        + "\tdisplayAndInputProcessor changes input value <"
                        + value + ">\t=>\t<" + inputValue + ">\t\t" + path);
                }

                pathToLocale.put(path, locale);

                // also check for non-distinguishing attributes
                if (path.contains("/identity"))
                    continue;

                String fullPath = file.getFullXPath(path);
                parts.set(fullPath);
                for (int i = 0; i < parts.size(); ++i) {
                    if (parts.getAttributeCount(i) == 0)
                        continue;
                    String element = parts.getElement(i);
                    for (String attribute : parts.getAttributeKeys(i)) {
                        if (skipAttributes.contains(attribute))
                            continue;
                        if (CLDRFile.isDistinguishing(dtdType, element, attribute)) {
                            distinguishing.put(element, attribute);
                        } else {
                            nonDistinguishing.put(element, attribute);
                        }
                    }
                }
            }
        }

        if (isVerbose()) {
            System.out.format("Distinguishing Elements: %s"
                + CldrUtility.LINE_SEPARATOR, distinguishing);
            System.out.format("Nondistinguishing Elements: %s"
                + CldrUtility.LINE_SEPARATOR, nonDistinguishing);
            System.out.format("Skipped %s" + CldrUtility.LINE_SEPARATOR,
                skipAttributes);
        }
    }

    /**
     * The verbose output shows the results of 1..3 \u00a4 signs.
     */
    public void checkCurrency() {
        Map<String, Set<R2<String, Integer>>> results = new TreeMap<String, Set<R2<String, Integer>>>(
            Collator.getInstance(ULocale.ENGLISH));
        for (ULocale locale : ULocale.getAvailableLocales()) {
            if (locale.getCountry().length() != 0) {
                continue;
            }
            for (int i = 1; i < 4; ++i) {
                NumberFormat format = getCurrencyInstance(locale, i);
                for (Currency c : new Currency[] { Currency.getInstance("USD"),
                    Currency.getInstance("EUR"),
                    Currency.getInstance("INR") }) {
                    format.setCurrency(c);
                    final String formatted = format.format(12345.67);
                    Set<R2<String, Integer>> set = results.get(formatted);
                    if (set == null) {
                        results.put(formatted,
                            set = new TreeSet<R2<String, Integer>>());
                    }
                    set.add(Row.of(locale.toString(), Integer.valueOf(i)));
                }
            }
        }
        for (String formatted : results.keySet()) {
            logln(formatted + "\t" + results.get(formatted));
        }
    }

    private static NumberFormat getCurrencyInstance(ULocale locale, int type) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        if (type > 1) {
            DecimalFormat format2 = (DecimalFormat) format;
            String pattern = format2.toPattern();
            String replacement = "\u00a4\u00a4";
            for (int i = 2; i < type; ++i) {
                replacement += "\u00a4";
            }
            pattern = pattern.replace("\u00a4", replacement);
            format2.applyPattern(pattern);
        }
        return format;
    }

    private UnicodeSet safeExemplars(CLDRFile file, String string) {
        final UnicodeSet result = file.getExemplarSet(string,
            WinningChoice.NORMAL);
        return result != null ? result : new UnicodeSet();
    }

    public void TestAPath() {
        // <month type="1">1</month>
        String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";
        CLDRFile root = testInfo.getRoot();
        logln("path: " + path);
        String fullpath = root.getFullXPath(path);
        logln("fullpath: " + fullpath);
        String value = root.getStringValue(path);
        logln("value: " + value);
        Status status = new Status();
        String source = root.getSourceLocaleID(path, status);
        logln("locale: " + source);
        logln("status: " + status);
    }

    public void TestDefaultContents() {
        Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO
            .getDefaultContentLocales();
        Relation<String, String> parentToChildren = Relation
            .<String, String> of(new TreeMap<String, Set<String>>(),
                TreeSet.class);
        for (String child : testInfo.getCldrFactory().getAvailable()) {
            if (child.equals("root")) {
                continue;
            }
            String localeParent = LocaleIDParser.getParent(child);
            parentToChildren.put(localeParent, child);
        }
        for (String locale : defaultContents) {
            CLDRFile cldrFile;
            try {
                cldrFile = testInfo.getCldrFactory().make(locale, false);
            } catch (RuntimeException e) {
                logln("Can't open default content file:\t" + locale);
                continue;
            }
            // we check that the default content locale is always empty
            for (Iterator<String> it = cldrFile.iterator(); it.hasNext();) {
                String path = it.next();
                if (path.contains("/identity")) {
                    continue;
                }
                errln("Default content file not empty:\t" + locale);
                showDifferences(locale);
                break;
            }
        }

        // check that if a locale has any children, that exactly one of them is
        // the default content

        for (String locale : defaultContents) {

            if (locale.equals("en_US") || locale.equals("ca_ES")) {
                continue; // en_US_POSIX or ca_ES_VALENCIA
            }
            Set<String> children = parentToChildren.get(locale);
            if (children != null) {
                Set<String> defaultContentChildren = new LinkedHashSet<String>(
                    children);
                defaultContentChildren.retainAll(defaultContents);
                if (defaultContentChildren.size() != 1) {
                    if (defaultContentChildren.isEmpty()) {
                        errln("Locale has children but is missing default contents locale: "
                            + locale + ", children: " + children);
                    } else {
                        errln("Locale has too many defaultContent locales!!: "
                            + locale + ", defaultContents: "
                            + defaultContentChildren);
                    }
                }
            }
        }

        // check that each default content locale is likely-subtag equivalent to
        // its parent.

        for (String locale : defaultContents) {
            String maxLocale = LikelySubtags.maximize(locale, likelyData);
            String localeParent = LocaleIDParser.getParent(locale);
            String maxLocaleParent = LikelySubtags.maximize(localeParent,
                likelyData);
            if (locale.equals("ar_001")) {
                logln("Known exception to likelyMax(locale=" + locale + ")"
                    + " == " + "likelyMax(defaultContent=" + localeParent
                    + ")");
                continue;
            }
            assertEquals("likelyMax(locale=" + locale + ")" + " == "
                + "likelyMax(defaultContent=" + localeParent + ")",
                maxLocaleParent, maxLocale);
        }

    }

    static final Map<String, String> likelyData = SUPPLEMENTAL_DATA_INFO
        .getLikelySubtags();

    public void TestLikelySubtagsComplete() {
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            if (locale.equals("root")) {
                continue;
            }
            String maxLocale = LikelySubtags.maximize(locale, likelyData);
            if (maxLocale == null) {
                errln("Locale missing likely subtag: " + locale);
                continue;
            }
            ltp.set(maxLocale);
            if (ltp.getLanguage().isEmpty() || ltp.getScript().isEmpty()
                || ltp.getRegion().isEmpty()) {
                errln("Locale has defective likely subtag: " + locale + " => "
                    + maxLocale);
            }
        }
    }

    private void showDifferences(String locale) {
        CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, false);
        final String localeParent = LocaleIDParser.getParent(locale);
        CLDRFile parentFile = testInfo.getCldrFactory()
            .make(localeParent, true);
        int funnyCount = 0;
        for (Iterator<String> it = cldrFile.iterator("",
            cldrFile.getComparator()); it.hasNext();) {
            String path = it.next();
            if (path.contains("/identity")) {
                continue;
            }
            final String fullXPath = cldrFile.getFullXPath(path);
            if (fullXPath.contains("[@draft=\"unconfirmed\"]")
                || fullXPath.contains("[@draft=\"provisional\"]")) {
                funnyCount++;
                continue;
            }
            logln("\tpath:\t" + path);
            logln("\t\t" + locale + " value:\t<"
                + cldrFile.getStringValue(path) + ">");
            final String parentFullPath = parentFile.getFullXPath(path);
            logln("\t\t" + localeParent + " value:\t<"
                + parentFile.getStringValue(path) + ">");
            logln("\t\t" + locale + " fullpath:\t" + fullXPath);
            logln("\t\t" + localeParent + " fullpath:\t" + parentFullPath);
        }
        logln("\tCount of non-approved:\t" + funnyCount);
    }

    enum MissingType {
        plurals, main_exemplars, no_main, collation, index_exemplars, punct_exemplars
    }

    public void TestCoreData() {
        Set<String> availableLanguages = testInfo.getCldrFactory()
            .getAvailableLanguages();
        PluralInfo rootRules = SUPPLEMENTAL_DATA_INFO.getPlurals(
            PluralType.cardinal, "root");
        EnumSet<MissingType> errors = EnumSet.of(MissingType.collation);
        EnumSet<MissingType> warnings = EnumSet.of(MissingType.collation,
            MissingType.index_exemplars, MissingType.punct_exemplars);

        Set<String> collations = new HashSet<String>();
        XPathParts parts = new XPathParts();

        // collect collation info
        Factory collationFactory = Factory.make(CLDRPaths.COLLATION_DIRECTORY,
            ".*", DraftStatus.contributed);
        for (String localeID : collationFactory.getAvailable()) {
            // if (localeID.equals("root")) {
            // CLDRFile cldrFile = collationFactory.make(localeID, false,
            // DraftStatus.contributed);
            // for (String path : cldrFile) {
            // if (path.startsWith("//ldml/collations")) {
            // String fullPath = cldrFile.getFullXPath(path);
            // String valid = parts.set(fullPath).getAttributeValue(1,
            // "validSubLocales");
            // for (String validSub : valid.trim().split("\\s+")) {
            // if (isTopLevel(validSub)) {
            // collations.add(validSub);
            // }
            // }
            // break; // done with root
            // }
            // }
            // } else
            if (isTopLevel(localeID)) {
                collations.add(localeID);
            }
        }
        logln(collations.toString());

        Set<String> allLanguages = Builder.with(new TreeSet<String>())
            .addAll(collations).addAll(availableLanguages).freeze();

        for (String localeID : allLanguages) {
            if (localeID.equals("root")) {
                continue; // skip script locales
            }
            if (!isTopLevel(localeID)) {
                continue;
            }

            errors.clear();
            warnings.clear();

            String name = "Locale:" + localeID + " ("
                + testInfo.getEnglish().getName(localeID) + ")";

            if (!collations.contains(localeID)) {
                warnings.add(MissingType.collation);
                logln(name + " is missing " + MissingType.collation.toString());
            }

            try {
                CLDRFile cldrFile = testInfo.getCldrFactory().make(localeID,
                    false, DraftStatus.contributed);

                String wholeFileAlias = cldrFile.getStringValue("//ldml/alias");
                if (wholeFileAlias != null) {
                    logln("Whole-file alias:" + name);
                    continue;
                }

                PluralInfo pluralInfo = SUPPLEMENTAL_DATA_INFO.getPlurals(
                    PluralType.cardinal, localeID);
                if (pluralInfo == rootRules) {
                    logln(name + " is missing "
                        + MissingType.plurals.toString());
                    warnings.add(MissingType.plurals);
                }
                UnicodeSet main = cldrFile.getExemplarSet("",
                    WinningChoice.WINNING);
                if (main == null || main.isEmpty()) {
                    errln("  " + name + " is missing "
                        + MissingType.main_exemplars.toString());
                    errors.add(MissingType.main_exemplars);
                }
                UnicodeSet index = cldrFile.getExemplarSet("index",
                    WinningChoice.WINNING);
                if (index == null || index.isEmpty()) {
                    logln(name + " is missing "
                        + MissingType.index_exemplars.toString());
                    warnings.add(MissingType.index_exemplars);
                }
                UnicodeSet punctuation = cldrFile.getExemplarSet("punctuation",
                    WinningChoice.WINNING);
                if (punctuation == null || punctuation.isEmpty()) {
                    logln(name + " is missing "
                        + MissingType.punct_exemplars.toString());
                    warnings.add(MissingType.punct_exemplars);
                }
            } catch (Exception e) {
                errln("  " + name + " is missing main locale data.");
                errors.add(MissingType.no_main);
            }

            // report errors

            if (errors.isEmpty() && warnings.isEmpty()) {
                logln(name + ": No problems...");
            }
        }
    }

    private boolean isTopLevel(String localeID) {
        return "root".equals(LocaleIDParser.getParent(localeID));
    }

    /**
     * Tests that every dtd item is connected from root
     */
    public void TestDtdCompleteness() {
        for (DtdType type : DtdType.values()) {
            DtdData dtdData = DtdData.getInstance(type);
            Set<Element> descendents = new LinkedHashSet();
            dtdData.getDescendents(dtdData.ROOT, descendents);
            Set<Element> elements = dtdData.getElements();
            if (!elements.equals(descendents)) {
                for (Element e : elements) {
                    if (!descendents.contains(e) && !e.equals(dtdData.PCDATA)
                        && !e.equals(dtdData.ANY)) {
                        if (e.name.equals("usesMetazone")) {
                            logKnownIssue("cldrbug:6768",
                                "catch orphan usesMetazone");
                        } else {
                            errln(type + ": Element " + e
                                + " not contained in descendents of ROOT.");
                        }
                    }
                }
                for (Element e : descendents) {
                    if (!elements.contains(e)) {
                        errln(type + ": Element " + e
                            + ", descendent of ROOT, not in elements.");
                    }
                }
            }
            LinkedHashSet<Element> all = new LinkedHashSet<Element>(descendents);
            all.addAll(elements);
            Set<Attribute> attributes = dtdData.getAttributes();
            for (Attribute a : attributes) {
                if (!elements.contains(a.element)) {
                    errln(type + ": Attribute " + a + " isn't for any element.");
                }
            }
        }
    }

    public void TestBasicDTDCompatibility() {
        if (logKnownIssue("Cldrbug:6903",
            "Need the directory before enabling this test")) {
            return;
        }

        final String oldCommon = CldrUtility
            .getProperty("oldCommon",
                "/Users/markdavis/Documents/workspace/cldr-archive/cldr-22.1/common");

        // set up exceptions
        Set<String> changedToEmpty = new HashSet<String>(
            Arrays.asList(new String[] { "version", "languageCoverage",
                "scriptCoverage", "territoryCoverage",
                "currencyCoverage", "timezoneCoverage",
            "skipDefaultLocale" }));
        Set<String> PCDATA = new HashSet<String>();
        PCDATA.add("PCDATA");
        Set<String> EMPTY = new HashSet<String>();
        EMPTY.add("EMPTY");
        Set<String> VERSION = new HashSet<String>();
        VERSION.add("version");

        // test all DTDs
        for (DtdType dtd : DtdType.values()) {
            try {
                ElementAttributeInfo oldDtd = ElementAttributeInfo.getInstance(
                    oldCommon, dtd);
                ElementAttributeInfo newDtd = ElementAttributeInfo
                    .getInstance(dtd);

                if (oldDtd == newDtd) {
                    continue;
                }
                Relation<String, String> oldElement2Children = oldDtd
                    .getElement2Children();
                Relation<String, String> newElement2Children = newDtd
                    .getElement2Children();

                Relation<String, String> oldElement2Attributes = oldDtd
                    .getElement2Attributes();
                Relation<String, String> newElement2Attributes = newDtd
                    .getElement2Attributes();

                for (String element : oldElement2Children.keySet()) {
                    Set<String> oldChildren = oldElement2Children
                        .getAll(element);
                    Set<String> newChildren = newElement2Children
                        .getAll(element);
                    if (newChildren == null) {
                        errln("Old " + dtd + " contains element not in new: <"
                            + element + ">");
                        continue;
                    }
                    Set<String> funny = containsInOrder(newChildren,
                        oldChildren);
                    if (funny != null) {
                        if (changedToEmpty.contains(element)
                            && oldChildren.equals(PCDATA)
                            && newChildren.equals(EMPTY)) {
                            // ok, skip
                        } else {
                            errln("Old " + dtd + " element <" + element
                                + "> has children Missing/Misordered:\t"
                                + funny + "\n\t\tOld:\t" + oldChildren
                                + "\n\t\tNew:\t" + newChildren);
                        }
                    }

                    Set<String> oldAttributes = oldElement2Attributes
                        .getAll(element);
                    if (oldAttributes == null) {
                        oldAttributes = Collections.emptySet();
                    }
                    Set<String> newAttributes = newElement2Attributes
                        .getAll(element);
                    if (newAttributes == null) {
                        newAttributes = Collections.emptySet();
                    }
                    if (!newAttributes.containsAll(oldAttributes)) {
                        LinkedHashSet<String> missing = new LinkedHashSet<String>(
                            oldAttributes);
                        missing.removeAll(newAttributes);
                        if (element.equals(dtd.toString())
                            && missing.equals(VERSION)) {
                            // ok, skip
                        } else {
                            errln("Old " + dtd + " element <" + element
                                + "> has attributes Missing:\t" + missing
                                + "\n\t\tOld:\t" + oldAttributes
                                + "\n\t\tNew:\t" + newAttributes);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                errln("Failure with " + dtd);
            }
        }
    }

    private <T> Set<T> containsInOrder(Set<T> superset, Set<T> subset) {
        if (!superset.containsAll(subset)) {
            LinkedHashSet<T> missing = new LinkedHashSet<T>(subset);
            missing.removeAll(superset);
            return missing;
        }
        // ok, we know that they are subsets, try order
        Set<T> result = null;
        DiscreteComparator<T> comp = new DiscreteComparator.Builder<T>(
            Ordering.ARBITRARY).add(superset).get();
        T last = null;
        for (T item : subset) {
            if (last != null) {
                int order = comp.compare(last, item);
                if (order != -1) {
                    if (result == null) {
                        result = new HashSet<T>();
                        result.add(last);
                        result.add(item);
                    }
                }
            }
            last = item;
        }
        return result;
    }

    public void TestDtdCompatibility() {
        if (logKnownIssue("Cldrbug:6768", "catch orphan usesMetazone")) {
            return;
        }
        for (DtdType type : DtdType.values()) {
            DtdData dtdData = DtdData.getInstance(type);
            Map<String, Element> currentElementFromName = dtdData
                .getElementFromName();

            // current has no orphan
            Set<Element> orphans = new LinkedHashSet<Element>(dtdData
                .getElementFromName().values());
            orphans.remove(dtdData.ROOT);
            orphans.remove(dtdData.PCDATA);
            orphans.remove(dtdData.ANY);
            Set<String> elementsWithoutAlt = new TreeSet<String>();
            Set<String> elementsWithoutDraft = new TreeSet<String>();
            Set<String> elementsWithoutAlias = new TreeSet<String>();
            Set<String> elementsWithoutSpecial = new TreeSet<String>();

            for (Element element : dtdData.getElementFromName().values()) {
                Set<Element> children = element.getChildren().keySet();
                orphans.removeAll(children);
                if (type == DtdType.ldml
                    && !SUPPLEMENTAL_DATA_INFO.isDeprecated(type,
                        element.name, "*", "*")) {
                    if (element.getType() == ElementType.PCDATA) {
                        if (element.getAttributeNamed("alt") == null) {
                            elementsWithoutAlt.add(element.name);
                        }
                        if (element.getAttributeNamed("draft") == null) {
                            elementsWithoutDraft.add(element.name);
                        }
                    }
                    if (children.size() != 0 && !"alias".equals(element.name)) {
                        if (element.getChildNamed("alias") == null) {
                            elementsWithoutAlias.add(element.name);
                        }
                        if (element.getChildNamed("special") == null) {
                            elementsWithoutSpecial.add(element.name);
                        }
                    }
                }
            }
            assertEquals(type + " DTD Must not have orphan elements",
                Collections.EMPTY_SET, orphans);
            assertEquals(type
                + " DTD elements with PCDATA must have 'alt' attributes",
                Collections.EMPTY_SET, elementsWithoutAlt);
            assertEquals(type
                + " DTD elements with PCDATA must have 'draft' attributes",
                Collections.EMPTY_SET, elementsWithoutDraft);
            assertEquals(type
                + " DTD elements with children must have 'alias' elements",
                Collections.EMPTY_SET, elementsWithoutAlias);
            assertEquals(
                type
                + " DTD elements with children must have 'special' elements",
                Collections.EMPTY_SET, elementsWithoutSpecial);

            for (Versions version : Versions.values()) {
                if (version == Versions.trunk) {
                    continue;
                } else if (version == Versions.v1_1_1) {
                    break;
                }
                DtdData dtdDataOld;
                try {
                    dtdDataOld = DtdData.getInstance(type, version.toString());
                } catch (IllegalArgumentException e) {
                    boolean tooOld = false;
                    switch (type) {
                    case ldmlBCP47:
                        tooOld = version.compareTo(Versions.v1_7_2) >= 0;
                        break;
                    case keyboard:
                    case platform:
                        tooOld = version.compareTo(Versions.v22_1) >= 0;
                        break;
                    default:
                        break;
                    }
                    if (tooOld) {
                        continue;
                    } else {
                        throw e;
                    }
                }
                // verify that if E is in dtdDataOld, then it is in dtdData, and
                // has at least the same children and attributes
                for (Entry<String, Element> entry : dtdDataOld
                    .getElementFromName().entrySet()) {
                    Element oldElement = entry.getValue();
                    Element newElement = currentElementFromName.get(entry
                        .getKey());
                    if (assertNotNull(type
                        + " DTD for trunk must be superset of v" + version
                        + ", and must contain «" + oldElement.getName()
                        + "»", newElement)) {
                        // TODO Check order also
                        for (Element oldChild : oldElement.getChildren()
                            .keySet()) {
                            Element newChild = newElement
                                .getChildNamed(oldChild.getName());
                            assertNotNull(
                                type + " DTD - Children of «"
                                    + newElement.getName()
                                    + "» must be superset of v"
                                    + version + ", and must contain «"
                                    + oldChild.getName() + "»",
                                    newChild);
                        }
                        for (Attribute oldAttribute : oldElement
                            .getAttributes().keySet()) {
                            Attribute newAttribute = newElement
                                .getAttributeNamed(oldAttribute.getName());
                            assertNotNull(
                                type + " DTD - Attributes of «"
                                    + newElement.getName()
                                    + "» must be superset of v"
                                    + version + ", and must contain «"
                                    + oldAttribute.getName() + "»",
                                    newAttribute);

                        }
                    }
                }
            }
        }
    }

    /**
     * Compare each path to each other path for every single file in CLDR
     */
    public void TestDtdComparison() {
        // try some simple paths for regression

        sortPaths(
            DtdData.getInstance(DtdType.ldml).getDtdComparator(null),
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/dateTimeFormatLength[@type=\"full\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats");

        sortPaths(
            DtdData.getInstance(DtdType.supplementalData).getDtdComparator(
                null),
                "//supplementalData/territoryContainment/group[@type=\"419\"][@contains=\"013 029 005\"][@grouping=\"true\"]",
            "//supplementalData/territoryContainment/group[@type=\"003\"][@contains=\"021 013 029\"][@grouping=\"true\"]");

        checkDtdComparatorForResource("TestBasic_ja.xml", DtdType.ldmlICU);
    }

    public void TestDtdComparisonsAll() {
        if (getInclusion() <= 5) { // Only run this test in exhaustive mode.
            return;
        }
        for (File file : CLDRConfig.getInstance().getAllCLDRFilesEndingWith(".xml")) {
            checkDtdComparatorFor(file, null);
        }
    }

    public void checkDtdComparatorForResource(String fileToRead,
        DtdType overrideDtdType) {
        MyHandler myHandler = new MyHandler(overrideDtdType);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        try {
            myHandler.fileName = fileToRead;
            xfr.read(myHandler.fileName, TestBasic.class, -1, true);
            logln(myHandler.fileName);
        } catch (Exception e) {
            Throwable t = e;
            StringBuilder b = new StringBuilder();
            String indent = "";
            while (t != null) {
                b.append(indent).append(t.getMessage());
                indent = indent.isEmpty() ? "\n\t\t" : indent + "\t";
                t = t.getCause();
            }
            errln(b.toString());
            return;
        }
        DtdData dtdData = DtdData.getInstance(myHandler.dtdType);
        sortPaths(dtdData.getDtdComparator(null), myHandler.data);
    }

    public void checkDtdComparatorFor(File fileToRead, DtdType overrideDtdType) {
        MyHandler myHandler = new MyHandler(overrideDtdType);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        try {
            myHandler.fileName = fileToRead.getCanonicalPath();
            xfr.read(myHandler.fileName, -1, true);
            logln(myHandler.fileName);
        } catch (Exception e) {
            Throwable t = e;
            StringBuilder b = new StringBuilder();
            String indent = "";
            while (t != null) {
                b.append(indent).append(t.getMessage());
                indent = indent.isEmpty() ? "\n\t\t" : indent + "\t";
                t = t.getCause();
            }
            errln(b.toString());
            return;
        }
        DtdData dtdData = DtdData.getInstance(myHandler.dtdType);
        sortPaths(dtdData.getDtdComparator(null), myHandler.data);
    }

    static class MyHandler extends XMLFileReader.SimpleHandler {
        private String fileName;
        private DtdType dtdType;
        private final Set<String> data = new LinkedHashSet<>();

        public MyHandler(DtdType overrideDtdType) {
            dtdType = overrideDtdType;
        }

        public void handlePathValue(String path, String value) {
            if (dtdType == null) {
                try {
                    dtdType = DtdType.fromPath(path);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Can't read " + fileName, e);
                }
            }
            data.add(path);
        }
    }

    public void sortPaths(Comparator<String> dc, Collection<String> paths) {
        String[] array = paths.toArray(new String[paths.size()]);
        sortPaths(dc, array);
    }

    public void sortPaths(Comparator<String> dc, String... array) {
        Arrays.sort(array, 0, array.length, dc);
    }

    public void TestNewDtdData() {
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData dtdData = DtdData.getInstance(type);
            for (Element element : dtdData.getElements()) {
                boolean orderedNew = dtdData.isOrdered(element.name);
                boolean orderedOld = DtdData.isOrderedOld(element.name, type);
                assertEquals("isOrdered " + type + ":" + element, orderedOld, orderedNew);
                boolean deprecatedNew = dtdData.isDeprecated(element.name, "*", "*");
                boolean deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecated(type, element.name, "*", "*");
                assertEquals("isDeprecated " + type + ":" + element, deprecatedOld, deprecatedNew);

                for (Attribute attribute : element.getAttributes().keySet()) {
                    boolean distinguishedNew = dtdData.isDistinguishing(element.name, attribute.name);
                    boolean distinguishedOld = dtdData.isDistinguishingOld(element.name, attribute.name);
                    assertEquals("isDistinguished " + type + ":" + attribute, distinguishedOld, distinguishedNew);
                    deprecatedNew = dtdData.isDeprecated(element.name, attribute.name, "*");
                    deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecated(type, element.name, attribute.name, "*");
                    assertEquals("isDeprecated " + type + ":" + attribute, deprecatedOld, deprecatedNew);
                    for (String value : attribute.values.keySet()) {
                        deprecatedNew = dtdData.isDeprecated(element.name, attribute.name, value);
                        deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecated(type, element.name, attribute.name, value);
                        assertEquals("isDeprecated " + type + ":" + attribute + ":" + value, deprecatedOld, deprecatedNew);
                    }
                }
            }
        }
        if (isVerbose()) {
            checkTime();
        }
    }

    private void checkTime() {
        final int ITERATIONS = 200;
        Timer t = new Timer();
        for (int i = ITERATIONS; i > 0; --i) {
            // check old
            for (DtdType type : DtdType.values()) {
                if (type == DtdType.ldmlICU) {
                    continue;
                }
                DtdData dtdData = DtdData.getInstance(type);
                for (Element element : dtdData.getElements()) {
                    boolean orderedOld = DtdData.isOrderedOld(element.name, type);
                    boolean deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecatedOld(type, element.name, "*", "*");
                    for (Attribute attribute : element.getAttributes().keySet()) {
                        boolean distinguishedOld = dtdData.isDistinguishingOld(element.name, attribute.name);
                        deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecatedOld(type, element.name, attribute.name, "*");
                        for (String value : attribute.values.keySet()) {
                            deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecatedOld(type, element.name, attribute.name, value);
                        }
                    }
                }
            }
        }
        long time1 = t.stop();
        logln("old: " + t.toString(ITERATIONS));
        Timer t2 = new Timer();
        // now new
        for (int i = ITERATIONS; i > 0; --i) {
            for (DtdType type : DtdType.values()) {
                if (type == DtdType.ldmlICU) {
                    continue;
                }
                DtdData dtdData = DtdData.getInstance(type);
                for (Element element : dtdData.getElements()) {
                    boolean orderedNew = dtdData.isOrdered(element.name);
                    boolean deprecatedNew = dtdData.isDeprecated(element.name, "*", "*");

                    for (Attribute attribute : element.getAttributes().keySet()) {
                        boolean distinguishedNew = dtdData.isDistinguishing(element.name, attribute.name);
                        deprecatedNew = dtdData.isDeprecated(element.name, attribute.name, "*");
                        for (String value : attribute.values.keySet()) {
                            deprecatedNew = dtdData.isDeprecated(element.name, attribute.name, value);
                        }
                    }
                }
            }
        }
        logln("new: " + t2.toString(ITERATIONS, time1));
    }
}
