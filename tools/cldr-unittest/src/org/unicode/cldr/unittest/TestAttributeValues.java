package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.unicode.cldr.tool.VerifyAttributeValues;
import org.unicode.cldr.tool.VerifyAttributeValues.Errors;
import org.unicode.cldr.util.AttributeValueValidity;
import org.unicode.cldr.util.AttributeValueValidity.AttributeValueSpec;
import org.unicode.cldr.util.AttributeValueValidity.MatcherPattern;
import org.unicode.cldr.util.AttributeValueValidity.Status;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.ValueStatus;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageInfo;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XMLFileReader.FilterBomInputStream;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.Attributes;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;

public class TestAttributeValues extends TestFmwk {
    private static final Validity VALIDITY = Validity.getInstance();
    private static final File BASE_DIR = new File(CLDRPaths.BASE_DIRECTORY);
    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Splitter SEMI_SPACE = Splitter.on(';').trimResults().omitEmptyStrings();
    private static final CLDRConfig config = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestAttributeValues().run(args);
    }

    public void TestValid() {
        // short- circuits for testing. null means do all
        Set<DtdType> checkTypes = null ; // ImmutableSet.of(DtdType.supplementalData, DtdType.keyboard, DtdType.platform); 
        ImmutableSet<ValueStatus> showStatuses = null ; // ImmutableSet.of(ValueStatus.invalid, ValueStatus.unknown);

        for (DtdType dtdType : checkTypes == null ? DtdType.STANDARD_SET : checkTypes) {
            PathChecker pathChecker = new PathChecker(this, DtdData.getInstance(dtdType));
            for (String mainDirs : Arrays.asList(CLDRPaths.COMMON_DIRECTORY, CLDRPaths.SEED_DIRECTORY)) {
                Set<String> files = new TreeSet<>();
                for (String stringDir : dtdType.directories) {
                    addXMLFiles(dtdType, mainDirs + stringDir, files);
                    if (isVerbose()) {
                        warnln(mainDirs + stringDir);
                    }
                }
                int x = files.size();
                files
                .parallelStream()
                .forEach(file -> checkFile(pathChecker, file));
            }
            pathChecker.show(isVerbose(), showStatuses);
        }
//        List<String> localesToTest = Arrays.asList("en", "root"); // , "zh", "hi", "ja", "ru", "cy"
//        Set<String> localesToTest = config.getCommonAndSeedAndMainAndAnnotationsFactory().getAvailable();
//        // TODO, add all other files

//        for (String locale : localesToTest) {
//            CLDRFile file = config.getCLDRFile(locale, false);
//            for (String dpath : file) {
//                String path = file.getFullXPath(dpath);
//                pathChecker.checkPath(path);
//            }
//        }
    }


    static final Set<String> CLDR_LOCALES = ImmutableSortedSet.copyOf(StandardCodes.make()
        .getLocaleCoverageLocales(Organization.cldr)
        .stream()
        .map(x -> x + ".xml")
        .collect(Collectors.toSet()));

    private void addXMLFiles(DtdType dtdType, String path, Set<String> files) {
        File dirFile = new File(path);
        if (!dirFile.exists()) {
            return;
        }
        if (!dirFile.isDirectory()) {
            if (getInclusion() <= 5 
                && dtdType == DtdType.ldml) {
                if (path.contains("/annotationsDerived/")) {
                    return;
                }
                String ending = path.substring(path.lastIndexOf('/')+1);
                if (!CLDR_LOCALES.contains(ending)) {
                    return;
                }
            }
            files.add(path);
        } else {
            for (String file : dirFile.list()) {
                addXMLFiles(dtdType, path + "/" + file, files);
            }
        }
    }


    private void checkFile(PathChecker pathChecker, String fullFile) {
        if (!fullFile.endsWith(".xml")) {
            return;
        }
        pathChecker.fileCount.incrementAndGet();
        //System.out.println(fullFile);
        XMLInputFactory f = XMLInputFactory.newInstance();
        int _elementCount = 0;
        int _attributeCount = 0;

        try {
            // should convert these over to new io.
            try (InputStream fis0 = new FileInputStream(fullFile);
                InputStream fis = new FilterBomInputStream(fis0);
                InputStreamReader inputStreamReader = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                ) {
                XMLStreamReader r = f.createXMLStreamReader(fullFile, bufferedReader);
                String element = null;
                while(r.hasNext()) {
                    switch(r.next()){
                    case XMLStreamReader.START_ELEMENT:
                        element = r.getLocalName();
                        ++_elementCount;
                        int attributeSize = r.getAttributeCount();
                        for (int i = 0; i < attributeSize; ++i) {
                            ++_attributeCount;
                            String attribute = r.getAttributeLocalName(i);
                            String attributeValue = r.getAttributeValue(i);
                            pathChecker.checkAttribute(element, attribute, attributeValue);
                        }
                        break;
                    }
                }
                //XMLFileReader.read("noId", inputStreamReader, -1, true, myHandler);
            } catch (IOException e) {
                throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + fullFile).initCause(e);
            }
        } catch (Exception e) {
            throw new ICUException(fullFile, e);
        }
        pathChecker.elementCount.addAndGet(_elementCount);
        pathChecker.attributeCount.addAndGet(_attributeCount);
    }

    static class PathChecker {
        private final Multimap<Row.R3<ValueStatus,String,String>,String> valueStatuses = TreeMultimap.create();
        private final Counter<ValueStatus> counter = new Counter<>();
        private final Set<String> seen = new HashSet<>();
        private final Map<String,Map<String,Map<String,Boolean>>> seenEAV = new ConcurrentHashMap<>();
        private final TestFmwk testLog;
        private final DtdData dtdData;
        private final Multimap<String, String> needsTesting;
        private final Map<String,String> matchValues;

        private final AtomicInteger fileCount = new AtomicInteger();
        private final AtomicInteger elementCount = new AtomicInteger();
        private final AtomicInteger attributeCount = new AtomicInteger();

        public PathChecker(TestFmwk testLog, DtdData dtdData) {
            this.testLog = testLog;
            this.dtdData = dtdData;
            Map<String,String> _matchValues = new TreeMap<>();
            needsTesting = dtdData.getNonEnumerated(_matchValues);
            matchValues = ImmutableMap.copyOf(_matchValues);
        }

        private void checkPath(String path) {
            if (seen.contains(path)) {
                return;
            }
            seen.add(path);
            if (path.contains("length-point")) {
                int debug = 0;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            for (int elementIndex = 0; elementIndex < parts.size(); ++elementIndex) {
                String element = parts.getElement(elementIndex);
                for (Entry<String, String> entry : parts.getAttributes(elementIndex).entrySet()) {
                    String attribute = entry.getKey();
                    String attrValue = entry.getValue();
                    checkAttribute(element, attribute, attrValue);
                }
            }
        }

        public void checkElement(String element, Attributes atts) {
            int length = atts.getLength();
            for (int i = 0; i < length; ++i) {
                checkAttribute(element, atts.getQName(i), atts.getValue(i));
            }
        }

        private void checkAttribute(String element, String attribute, String attrValue) {
            // skip cases we know we don't need to test
            if (!needsTesting.containsEntry(element, attribute)) {
                return;
            }
            // check if we've seen the EAV yet
            // we don't need to synchronize because a miss isn't serious
            Map<String, Map<String, Boolean>> sub = seenEAV.get(element);
            if (sub == null) {
                Map<String, Map<String, Boolean>> subAlready = seenEAV.putIfAbsent(element, sub = new ConcurrentHashMap<>());
                if (subAlready != null) {
                    sub = subAlready; // discards empty map
                }
            }
            Map<String, Boolean> set = sub.get(attribute);
            if (set == null) {
                Map<String, Boolean> setAlready = sub.putIfAbsent(attribute, set = new ConcurrentHashMap<>());
                if (setAlready != null) {
                    set = setAlready; // discards empty map
                }
            }
            if (set.putIfAbsent(attrValue, Boolean.TRUE) != null) {
                return;
            };

            // get the status & store
            ValueStatus valueStatus = dtdData.getValueStatus(element, attribute, attrValue);
            R3<ValueStatus, String, String> row = Row.of(valueStatus, element, attribute);
            synchronized (valueStatuses) {
                if (valueStatuses.put(row, attrValue)) {
                    counter.add(valueStatus, 1);
                }
            }
        }

        void show(boolean verbose, ImmutableSet<ValueStatus> retain) {
//          if (testLog.logKnownIssue("cldrbug 10120", "Don't enable error until complete")) {
//              testLog.warnln("Counts: " + counter.toString());
//          } else 
            for (ValueStatus valueStatus : ValueStatus.values()) {
                if (valueStatus != ValueStatus.valid && counter.get(valueStatus) != 0) {
                    testLog.errln("Problems with " + valueStatus 
                        + ", use -v for details: " + counter.toString());
                }
            }

            if (!verbose) {
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append("\n");

            out.append("fileCount:\t" + dtdData.dtdType + "\t" + fileCount + "\n");
            out.append("elementCount:\t" + dtdData.dtdType + "\t" + elementCount + "\n");
            out.append("attributeCount:\t" + dtdData.dtdType + "\t" + attributeCount + "\n");
            out.append("status\tdtdType\telement\tattribute\tmatch\t#attr values\tattr values\n");

            for(Entry<Row.R3<ValueStatus,String,String>, Collection<String>> entry : valueStatuses.asMap().entrySet()) {
                ValueStatus valueStatus = entry.getKey().get0();
                if (retain != null && !retain.contains(valueStatus)) {
                    continue;
                }
                String elementName = entry.getKey().get1();
                String attributeName = entry.getKey().get2();
                String matchValue = matchValues.get(elementName + "\t" + attributeName);
                Collection<String> validFound = entry.getValue();
                out.append(
                    valueStatus 
                    + "\t" + dtdData.dtdType 
                    + "\t" + elementName 
                    + "\t" + attributeName 
                    + "\t" + (matchValue == null ? "" : matchValue)
                    + "\t" + validFound.size()
                    + "\t" + CollectionUtilities.join(validFound, ", ")
                    + "\n"
                    );
                if (valueStatus == ValueStatus.valid) try {
                    LstrType lstr = LstrType.valueOf(elementName);
                    Map<String, Validity.Status> codeToStatus = VALIDITY.getCodeToStatus(lstr);
                    Set<String> missing = new TreeSet<>(codeToStatus.keySet());
                    if (lstr == LstrType.variant) {
                        for (String item : validFound) {
                            missing.remove(item.toLowerCase(Locale.ROOT));
                        }
                    } else {
                        missing.removeAll(validFound);
                    }
                    Set<String> deprecated = VALIDITY.getStatusToCodes(lstr).get(LstrField.Deprecated);
                    if (deprecated != null) {
                        missing.removeAll(deprecated);
                    }
                    if (!missing.isEmpty()) {
                        out.append(
                            "missing" 
                                + "\t" + dtdData.dtdType 
                                + "\t" + elementName 
                                + "\t" + attributeName 
                                + "\t" + "" 
                                + "\t" + "" 
                                + "\t" + CollectionUtilities.join(missing, ", ")
                                + "\n"
                            );
                    }
                } catch (Exception e) {}
            }
            testLog.warnln(out.toString());
        }
    }

    public void xTestA() {
        MatcherPattern mp = AttributeValueValidity.getMatcherPattern("$language");
        for (String language : LanguageInfo.getAvailable()) {
            if (mp.matches(language, null)) {
                LanguageInfo languageInfo = LanguageInfo.get(language);
                show(language, languageInfo);
            }
        }
    }

    private void show(String language, LanguageInfo languageInfo) {
        logln(language
            + "\t" + config.getEnglish().getName(CLDRFile.LANGUAGE_NAME, language)
            + "\t" + languageInfo);
    }

//    public void TestAttributeValueValidity() {
//        for (String test : Arrays.asList(
//            "supplementalData;     territoryAlias;     replacement;    AA")) {
//            quickTest(test);
//        }
//    }

    private Status quickTest(String test) {
        List<String> parts = SEMI_SPACE.splitToList(test);
        Output<String> reason = new Output<>();
        Status value = AttributeValueValidity.check(DtdData.getInstance(DtdType.valueOf(parts.get(0))), parts.get(1), parts.get(2), parts.get(3), reason);
        if (value != Status.ok) {
            errln(test + "\t" + value + "\t" + reason);
        }
        return value;
    }

    public void oldTestSingleFile() {
        Errors errors = new Errors();
        Set<AttributeValueSpec> missing = new TreeSet<>();
        VerifyAttributeValues.check(CLDRPaths.MAIN_DIRECTORY + "en.xml", errors, missing);
        for (AttributeValueSpec entry1 : missing) {
            errln("Missing Tests: " + entry1);
        }
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            errln(item.get0() + "; \t" + item.get2() + "; \t" + item.get1());
        }
    }

    public void oldTestCoreValidity() {
        int maxPerDirectory = getInclusion() <= 5 ? 20 : Integer.MAX_VALUE;
        Matcher fileMatcher = null;
        Set<AttributeValueSpec> missing = new LinkedHashSet<>();
        Errors errors = new Errors();
        VerifyAttributeValues.findAttributeValues(BASE_DIR, maxPerDirectory, fileMatcher, errors, missing, isVerbose() ? getErrorLogPrintWriter() : null);

        int count = 0;
        for (Entry<AttributeValidityInfo, String> entry : AttributeValueValidity.getReadFailures().entrySet()) {
            errln("Read error: " + ++count + "\t" + entry.getKey() + " => " + entry.getValue());
        }

        count = 0;
        for (R3<DtdType, String, String> entry1 : AttributeValueValidity.getTodoTests()) {
            warnln("Unfinished Test: " + ++count + "\t" + new AttributeValueSpec(entry1.get0(), entry1.get1(), entry1.get2(), "").toString());
        }

        count = 0;
        for (AttributeValueSpec entry1 : missing) {
            errln("Missing Test: " + entry1);
        }

        count = 0;
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            if ("deprecated".equals(item.get2()))
                errln("Deprecated: " + ++count
                    + "; \t" + item.get0()
                    + "; \t" + item.get1().type
                    + "; \t" + item.get1().element
                    + "; \t" + item.get1().attribute
                    + "; \t" + item.get1().attributeValue
                    + "; \t" + item.get2());
        }

        count = 0;
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            if (!"deprecated".equals(item.get2()))
                errln("Invalid: " + ++count
                    + "; \t" + item.get0()
                    + "; \t" + item.get1().type
                    + "; \t" + item.get1().element
                    + "; \t" + item.get1().attribute
                    + "; \t" + item.get1().attributeValue
                    + "; \t" + item.get2());
        }
    }
}
