package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

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
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.Attributes;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
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

    // TODO move over tests for AttributeValueValidity

    public void TestValid() {
        PathChecker pathChecker = new PathChecker(this);
        SimpleHandler simpleHandler = new SimpleHandler() {
            @Override
            public void handlePathValue(String path, String value) {
                pathChecker.checkPath(path);
            }
        };

        List<String> localesToTest = Arrays.asList("en", "root"); // , "zh", "hi", "ja", "ru", "cy"
        //Set<String> localesToTest = config.getCommonAndSeedAndMainAndAnnotationsFactory().getAvailable();
        // TODO, add all other files
//        for (String mainDirs : Arrays.asList(CLDRPaths.COMMON_DIRECTORY, CLDRPaths.SEED_DIRECTORY)) {
//            for (String stringDir : DtdType.ldml.directories) {
//                if (!stringDir.equals("main")) {
//                    continue;
//                }
//                String dir = mainDirs + stringDir + "/";
//                warnln(dir);
//                File dirFile = new File(dir);
//                if (!dirFile.exists() || !dirFile.isDirectory()) {
//                    continue;
//                }
//                for (String file : dirFile.list()) {
//                    if (!file.endsWith(".xml")) {
//                        continue;
//                    }
//                    String fullFile = dir + file;
//                    try {
//                        new XMLFileReader()
//                            .setHandler(simpleHandler)
//                            .read(fullFile, -1, true);
//                    } catch (Exception e) {
//                        throw new ICUException(fullFile, e);
//                    }
//                }
//            }
//        }

        for (String locale : localesToTest) {
            CLDRFile file = config.getCLDRFile(locale, false);
            for (String dpath : file) {
                String path = file.getFullXPath(dpath);
                pathChecker.checkPath(path);
            }
        }
        pathChecker.show(true); // isVerbose());
    }

    static class PathChecker {
        private Multimap<Row.R3<ValueStatus,String,String>,String> valueStatuses = TreeMultimap.create();
        private Counter<ValueStatus> counter = new Counter<>();
        private Set<String> seen = new HashSet<>();
        private TestLog testLog;

        public PathChecker(TestLog testLog) {
            this.testLog = testLog;
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
            DtdData dtdData = parts.getDtdData();
            for (int elementIndex = 0; elementIndex < parts.size(); ++elementIndex) {
                String element = parts.getElement(elementIndex);
                for (Entry<String, String> entry : parts.getAttributes(elementIndex).entrySet()) {
                    String attribute = entry.getKey();
                    String attrValue = entry.getValue();
                    checkAttribute(dtdData, element, attribute, attrValue);
                }
            }
        }
        
        public void checkElement(DtdData dtdData, String element, Attributes atts) {
            int length = atts.getLength();
            for (int i = 0; i < length; ++i) {
                checkAttribute(dtdData, element, atts.getQName(i), atts.getValue(i));
            }
        }

        private void checkAttribute(DtdData dtdData, String element, String attribute, String attrValue) {
            ValueStatus valueStatus = dtdData.getValueStatus(element, attribute, attrValue);
            R3<ValueStatus, String, String> row = Row.of(valueStatus, element, attribute);
            if (valueStatuses.put(row, attrValue)) {
                counter.add(valueStatus, 1);
            }
        }

        void show(boolean verbose) {
            if (counter.get(ValueStatus.invalid) != 0) {
                testLog.errln(counter.toString());
            }
            if (!verbose) {
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append("\nstatus\telement\tattribute\tattribute value\n");

            for(Entry<Row.R3<ValueStatus,String,String>, Collection<String>> entry : valueStatuses.asMap().entrySet()) {
                ValueStatus valueStatus = entry.getKey().get0();
                String elementName = entry.getKey().get1();
                String attributeName = entry.getKey().get2();
                Collection<String> validFound = entry.getValue();
                out.append(
                    valueStatus 
                    + "\t" + elementName 
                    + "\t" + attributeName 
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
                    missing.removeAll(VALIDITY.getStatusToCodes(lstr).get(LstrField.Deprecated));
                    if (!missing.isEmpty()) {
                        out.append(
                            "missing" 
                                + "\t" + elementName 
                                + "\t" + attributeName 
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
