package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdData.AttributeType;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ElementType;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
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

            // has a value & value attribute
            {"//supplementalData/plurals/pluralRanges[@locales=\"id ja\"]/pluralRange[@start=\"a\"][@end=\"b\"][@result=\"c\"]",
                "//supplementalData/plurals/pluralRanges[@locales=\"id\"]/pluralRange[@end=\"b\"][@start=\"a\"]/_result==c",
                "//supplementalData/plurals/pluralRanges[@locales=\"ja\"]/pluralRange[@end=\"b\"][@start=\"a\"]/_result==c"
            },


            {"//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"am as bn\"]/pluralRule[@count=\"other\"]",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"am\"]/pluralRule[@count=\"other\"]==VALUE",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"as\"]/pluralRule[@count=\"other\"]==VALUE",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"bn\"]/pluralRule[@count=\"other\"]==VALUE"
            },        

            // no change
            {"//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]",
                "//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]==VALUE"
            },

            // has only value attributes
            {"//supplementalData/version[@number=\"$Revision: 12197 $\"][@cldrVersion=\"29\"][@unicodeVersion=\"8.0.0\"]",
                "//supplementalData/version/_cldrVersion==29",
                "//supplementalData/version/_unicodeVersion==8.0.0"
            },

            // no change
            {"//ldml/identity/language[@type=\"af\"]",
                "//ldml/identity/language[@type=\"af\"]==VALUE"
            },

//            // has a value & value attribute
//            {"//ldml/annotations/annotation[@cp=\"[ߘ]\"][@tts=\"grinnikende gesig\"]",
//                "//ldml/annotations/annotation[@cp=\"[ߘ]\"]",
//                "//ldml/annotations/annotation_[@cp=\"[ߘ]\"]/_tts"
//            },

            // has a value & value attribute
            {"//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"][@access=\"private\"]/rbnfrule[@value=\"0\"]",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/_access==private",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule==VALUE",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule_/_value==0"
            },

            // has a value attribute
            {"//ldmlBCP47/version[@number=\"$Revision: 12232 $\"][@cldrVersion=\"29\"]",
                "//ldmlBCP47/version/_cldrVersion==29"
            },

            // has a value & value attribute
            {"//ldmlBCP47/keyword/key[@name=\"ca\"][@description=\"Calendar algorithm key\"][@deprecated=\"false\"][@alias=\"calendar\"][@valueType=\"incremental\"]/type[@name=\"chinese\"][@description=\"Traditional Chinese calendar\"][@deprecated=\"false\"]",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_alias==calendar",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_description==Calendar algorithm key",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_valueType==incremental",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/type[@name=\"chinese\"]/_description==Traditional Chinese calendar"
            },        

        };
        Multimap<String, String> extras = TreeMultimap.create();

        for (String[] test : tests) {
            final String path = test[0];
            final Set<String> expected = new TreeSet<>(Arrays.asList(test).subList(1, Arrays.asList(test).size()));
            DtdData dtdData = DtdData.getInstance(DtdType.fromPath(path));

            Set<String> actual = new TreeSet<>();
            XPathParts parts = XPathParts.getFrozenInstance(path);
            Set<String> pathForValues = dtdData.getRegularizedPaths(parts, extras);
            for (Entry<String, Collection<String>> entry : extras.asMap().entrySet()) {
                for (String value : entry.getValue()) {
                    actual.add(entry.getKey() + "==" + value);
                }
            }
            if (pathForValues != null) {
                for (String item : pathForValues) {
                    actual.add(item + "==VALUE");
                }
            }
            TreeSet<String> temp = new TreeSet<String>(actual);
            temp.removeAll(expected);
            assertEquals("too many, extra:  " + path, Collections.emptySet(), temp);
            temp.clear();
            temp.addAll(expected);
            temp.removeAll(actual);
            assertEquals("too few, missing: " + path, Collections.emptySet(), temp);
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

    public void TestEmpty() {
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData dtdData = DtdData.getInstance(type);
            checkEmpty(type, dtdData.ROOT, new HashSet<Attribute>(), new HashSet<Element>());
        }
    }

    /** make sure that if the final element is empty, there is a value attribute required somewhere in the path
     * @param type 
     * @param seen 
     */
    private void checkEmpty(DtdType type, Element element, HashSet<Attribute> parentAttributes, HashSet<Element> seen) {
        if (seen.contains(element)) {
            return;
        }
        seen.add(element);
        if (element.isDeprecated()) {
            return;
        }

        HashSet<Attribute> attributes = new HashSet<>(parentAttributes);
        for (Attribute attribute : element.getAttributes().keySet()) {
            AttributeType x;
            if (!attribute.isDeprecated() 
                && attribute.getStatus() == AttributeStatus.value
                // && (attribute.mode == Mode.REQUIRED || attribute.mode == Mode.FIXED) strong test
                ) {
                attributes.add(attribute);
            }
        }
        ElementType elementType = element.getType();
        switch (elementType) {
        case EMPTY: 
            if (attributes.isEmpty()) {
                if (type == DtdType.supplementalData && element.name.equals("rgPath")) {
                    warnln(type + " - " + element + " path has neither value nor value attributes");
                    break;
                }
                errln(type + " - " + element + " path has neither value nor value attributes");
            }
            break;
        case ANY:
        case PCDATA:
            if (!attributes.isEmpty()) {
                warnln(type + " - " + element + " path has both value and value attributes: " + attributes);
            }
            break;
        case CHILDREN:
            for (Element child : element.getChildren().keySet()) {
                checkEmpty(type, child, attributes, seen);
            }
            break;
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
