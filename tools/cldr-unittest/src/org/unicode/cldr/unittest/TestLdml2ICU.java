package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.icu.NewLdml2IcuConverter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.RegexFileParser;
import org.unicode.cldr.util.RegexFileParser.RegexLineParser;
import org.unicode.cldr.util.RegexFileParser.VariableProcessor;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transform;

/**
 * Tests the parts of the Ldml2IcuConverter that uses RegexLookups to convert
 * values to ICU. Data that is converted using other methods isn't tested here.
 *
 * @author jchye
 */
public class TestLdml2ICU extends TestFmwk {
    private static final boolean DEBUG = false;

    static final CLDRConfig info = CLDRConfig.getInstance();

    private static final Transform<String, RegexFinder> XPATH_TRANSFORM = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            final String newSource = source.replace("[@", "\\[@");
            return new RegexFinder("^" + newSource + "$");
        }
    };

    public static void main(String[] args) {
        new TestLdml2ICU().run(args);
    }

    enum ExclusionType {
        UNCONVERTED, IGNORE, // May be converted or not, but we don't care
        WARNING;
        public static Transform<String, Pair<ExclusionType, String>> TRANSFORM = new Transform<String, Pair<ExclusionType, String>>() {
            public Pair<ExclusionType, String> transform(String source) {
                String value = null;
                if (source.contains(";")) {
                    String[] split = source.split("\\s*;\\s*");
                    source = split[0];
                    value = split[1];
                }
                ExclusionType type = ExclusionType
                    .valueOf(source.toUpperCase());
                return Pair.of(type, value);
            }
        };
    }

    static final RegexLookup<Pair<ExclusionType, String>> exclusions = RegexLookup
        .of(ExclusionType.TRANSFORM)
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .loadFromFile(TestLdml2ICU.class, "../util/data/testLdml2Icu.txt");

    public void TestEnglish() {
        checkLocaleRegexes("en");
    }

    public void TestArabic() {
        checkLocaleRegexes("ar");
    }

    public void TestRoot() {
        checkLocaleRegexes("root");
    }

    public void TestRussian() {
        checkLocaleRegexes("ru");
    }

    public void TestJapanese() {
        checkLocaleRegexes("ja");
    }

    public void TestTamil() {
        checkLocaleRegexes("ta");
    }

    public void TestSupplemental() {
        checkSupplementalRegexes("supplementalData");
    }

    public void TestSupplmentalMetadata() {
        checkSupplementalRegexes("supplementalMetadata");
    }

//    public void TestTelephoneCodeData() {
//        checkSupplementalRegexes("telephoneCodeData");
//    }
//
    public void TestMetaZones() {
        checkSupplementalRegexes("metaZones");
    }

    public void TestLanguageInfo() {
        checkSupplementalRegexes("languageInfo");
    }

    public void TestLikelySubtags() {
        checkSupplementalRegexes("likelySubtags");
    }

    public void TestNumberingSystems() {
        checkSupplementalRegexes("numberingSystems");
    }

    public void TestWindowsZones() {
        checkSupplementalRegexes("windowsZones");
    }

    public void TestGenderList() {
        checkSupplementalRegexes("genderList");
    }

//    public void TestPostalCodeData() {
//        checkSupplementalRegexes("postalCodeData");
//    }

    /**
     * Loads the regex files used to convert XPaths to ICU paths.
     */
    private static RegexLookup<Object> loadRegexes(String filename) {
        final RegexLookup<Object> lookup = RegexLookup.of()
            .setPatternTransform(XPATH_TRANSFORM);
        RegexFileParser parser = new RegexFileParser();
        parser.setLineParser(new RegexLineParser() {
            int patternNum = 0;

            @Override
            public void parse(String line) {
                int pos = line.indexOf(";");
                // We only care about the patterns.
                if (pos == 0)
                    return;
                String pattern = pos < 0 ? line : line.substring(0, pos).trim();
                lookup.add(pattern, patternNum++);
            }
        });
        parser.setVariableProcessor(new VariableProcessor() {
            VariableReplacer variables = new VariableReplacer();

            @Override
            public void add(String variableName, String value) {
                if (value.startsWith("//")) { // is xpath
                    value = "[^\"]++";
                }
                variables.add(variableName, value);
            }

            @Override
            public String replace(String str) {
                return variables.replace(str);
            }

        });
        parser.parse(NewLdml2IcuConverter.class, filename);
        return lookup;
    }

    /**
     * Checks conversion of XML files in the supplemental directory.
     *
     * @param name
     *            the name of the XML file to be converted (minus the extension)
     */
    private void checkSupplementalRegexes(String name) {
        RegexLookup<Object> lookup = loadRegexes("ldml2icu_supplemental.txt");
        List<Pair<String, String>> cldrData = new ArrayList<Pair<String, String>>();
        XMLFileReader.loadPathValues(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY
            + name + ".xml", cldrData, true);
        XPathParts parts = new XPathParts();
        for (Pair<String, String> pair : cldrData) {
            String xpath = CLDRFile.getNondraftNonaltXPath(pair.getFirst());
            xpath = parts.set(xpath).toString();
            checkPath(lookup, xpath, pair.getSecond());
        }
    }

    Set<String> unconverted = new HashSet<String>();

    /**
     * Checks if an xpath was matched by a RegexLookup.
     */
    private <T> void checkPath(RegexLookup<T> lookup, String xpath, String value) {
        Pair<ExclusionType, String> exclusionInfo = exclusions.get(xpath);
        ExclusionType exclusionType = null;
        if (exclusionInfo != null) {
            exclusionType = exclusionInfo.getFirst();
        }

        if (lookup.get(xpath) == null) {
            String errorMessage = "CLDR xpath  <" + xpath + "> with value <"
                + value + "> was not converted to ICU.";
            if (exclusionType == null) {
                CldrUtility.logRegexLookup(this, lookup, xpath);
                errln(errorMessage);
            } else if (exclusionType == ExclusionType.WARNING) {
                logln(errorMessage);
            } else if (exclusionType == ExclusionType.UNCONVERTED) {
                String template = xpath.replaceAll("\"[^\"]++\"", "*");
                if (!unconverted.add(template)) {
                    logln("Not converted: " + xpath);
                }
            }
        } else if (exclusionType == ExclusionType.UNCONVERTED) {
            CldrUtility.logRegexLookup(this, exclusions, xpath);
            errln("CLDR xpath <"
                + xpath
                + "> is in the exclusions list but was matched. "
                + "To make the test pass, remove the relevant regex from org/unicode/cldr/util/data/testLdml2Icu.txt");
        }
    }

    /**
     * Checks conversion of XML locale files.
     *
     * @param name
     *            the name of the XML file to be converted (minus the extension)
     */
    private void checkLocaleRegexes(String locale) {
        CLDRFile plain = info.getCldrFactory().make(locale, false,
            DraftStatus.contributed);
        RegexLookup<Object> lookup = loadRegexes("ldml2icu_locale.txt");
        for (String xpath : plain) {
            if (DEBUG && xpath.contains("defaultNumberingSystem")) {
                int debug = 0;
            }
            String fullPath = CLDRFile.getNondraftNonaltXPath(plain
                .getFullXPath(xpath));
            checkPath(lookup, fullPath, plain.getStringValue(xpath));
        }
    }
}
