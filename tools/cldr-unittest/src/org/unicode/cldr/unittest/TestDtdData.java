package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;

import com.ibm.icu.dev.test.TestFmwk;

public class TestDtdData  extends TestFmwk {
    private static final String COMMON_DIR = CLDRPaths.BASE_DIRECTORY + "common/";
    static TestInfo testInfo = TestInfo.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo
        .getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestDtdData().run(args);
    }

    public void TestRegularized() {
        String[][] tests = {
            // no change
            {"//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]",
            "//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]"},
            
            // has a value & value attribute
            {"//supplementalData/plurals/pluralRanges[@locales=\"id ja\"]/pluralRange[@start=\"other\"][@end=\"other\"][@result=\"other\"]",
            "//supplementalData/plurals/pluralRanges[@locales=\"id ja\"]/pluralRange[@start=\"other\"][@end=\"other\"]",
            "//supplementalData/plurals/pluralRanges[@locales=\"id ja\"]/pluralRange_[@start=\"other\"][@end=\"other\"]/_result"},

            // has only value attributes
            {"//supplementalData/version[@number=\"$Revision: 12197 $\"][@cldrVersion=\"29\"][@unicodeVersion=\"8.0.0\"]",
            "//supplementalData/version/_cldrVersion",
            "//supplementalData/version/_unicodeVersion"},

            // no change
            {"//ldml/identity/language[@type=\"af\"]",
            "//ldml/identity/language[@type=\"af\"]"},

            // has a value & value attribute
            {"//ldml/annotations/annotation[@cp=\"[ߘ]\"][@tts=\"grinnikende gesig\"]",
            "//ldml/annotations/annotation[@cp=\"[ߘ]\"]",
            "//ldml/annotations/annotation_[@cp=\"[ߘ]\"]/_tts"},

            // has a value & value attribute
            {"//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"][@access=\"private\"]/rbnfrule[@value=\"0\"]",
            "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/_access",
            "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule",
            "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule_/_value"},

            // has a value attribute
            {"//ldmlBCP47/version[@number=\"$Revision: 12232 $\"][@cldrVersion=\"29\"]",
            "//ldmlBCP47/version/_cldrVersion"},

            // has a value & value attribute
            {"//ldmlBCP47/keyword/key[@name=\"ca\"][@description=\"Calendar algorithm key\"][@deprecated=\"false\"][@alias=\"calendar\"][@valueType=\"incremental\"]/type[@name=\"chinese\"][@description=\"Traditional Chinese calendar\"][@deprecated=\"false\"]",
            "//ldmlBCP47/keyword/key[@name=\"ca\"]/_alias",
            "//ldmlBCP47/keyword/key[@name=\"ca\"]/_description",
            "//ldmlBCP47/keyword/key[@name=\"ca\"]/_valueType",
            "//ldmlBCP47/keyword/key[@name=\"ca\"]/type[@name=\"chinese\"]/_description"},        
        };
        Map<String, String> extras = new TreeMap<>();

        for (String[] test : tests) {
            final String path = test[0];
            final Set<String> expected = new TreeSet<>(Arrays.asList(test).subList(1, Arrays.asList(test).size()));
            DtdData dtdData = DtdData.getInstance(DtdType.fromPath(path));

            XPathParts parts = XPathParts.getFrozenInstance(path);
            String fixed = dtdData.getRegularizedPaths(parts, extras);
            Set<String> actual = new TreeSet<>(extras.keySet());
            if (fixed != null) {
                actual.add(fixed);
            }
            assertEquals(path, expected, actual);
        }
    }
    
    public void TestDirectories() throws IOException {
        for (File dir : new File(COMMON_DIR).listFiles()) {
            if (dir.isDirectory() == false) {
                continue;
            }
            int maxFiles = 5;
            logln(dir.toString());
            for (File file : dir.listFiles()) {
                String name = file.getName();
                if (!name.endsWith(".xml")) {
                    continue;
                }
                List<Pair<String, String>> data = new ArrayList<>();
                for (Pair<String, String> pathValue : XMLFileReader.loadPathValues(file.toString(), data , true)) {
                    DtdType dtdTypeFromPath = DtdType.fromPath(pathValue.getFirst());
                    if (!dtdTypeFromPath.directories.contains(dir.getName())) {
                        errln("Mismatch in " + file.toString() 
                            + ": " + dtdTypeFromPath + ", " + dtdTypeFromPath.directories);
                    }
                    logln("\t" + file.getName() + "\t" + dtdTypeFromPath);
                    break;
                }
                if (--maxFiles < 0) break;
            }
        }
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
    }
}
