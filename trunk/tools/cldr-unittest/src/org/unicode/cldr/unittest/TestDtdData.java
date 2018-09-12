package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
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
import com.ibm.icu.dev.util.CollectionUtilities;

public class TestDtdData extends TestFmwk {
    private static final String COMMON_DIR = CLDRPaths.BASE_DIRECTORY + "common/";
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo
        .getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestDtdData().run(args);
    }

    public void TestRegularized() {
        String[][] tests = {

            // has a value & value attribute
            { "//supplementalData/plurals/pluralRanges[@locales=\"id ja\"]/pluralRange[@start=\"a\"][@end=\"b\"][@result=\"c\"]",
                "//supplementalData/plurals/pluralRanges[@locales=\"id\"]/pluralRange[@end=\"b\"][@start=\"a\"]/_result==c",
                "//supplementalData/plurals/pluralRanges[@locales=\"ja\"]/pluralRange[@end=\"b\"][@start=\"a\"]/_result==c"
            },

            { "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"am as bn\"]/pluralRule[@count=\"other\"]",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"am\"]/pluralRule[@count=\"other\"]==VALUE",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"as\"]/pluralRule[@count=\"other\"]==VALUE",
                "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\"bn\"]/pluralRule[@count=\"other\"]==VALUE"
            },

            // no change
            { "//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]",
                "//supplementalData/primaryZones/primaryZone[@iso3166=\"CL\"]==VALUE"
            },

            // has only value attributes
            { "//supplementalData/version[@number=\"$Revision: 12197 $\"][@cldrVersion=\"29\"][@unicodeVersion=\"8.0.0\"]",
                "//supplementalData/version/_cldrVersion==29",
                "//supplementalData/version/_unicodeVersion==8.0.0"
            },

            // no change
            { "//ldml/identity/language[@type=\"af\"]",
                "//ldml/identity/language[@type=\"af\"]==VALUE"
            },

//            // has a value & value attribute
//            {"//ldml/annotations/annotation[@cp=\"[ߘ]\"][@tts=\"grinnikende gesig\"]",
//                "//ldml/annotations/annotation[@cp=\"[ߘ]\"]",
//                "//ldml/annotations/annotation_[@cp=\"[ߘ]\"]/_tts"
//            },

            // has a value & value attribute
            { "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"][@access=\"private\"]/rbnfrule[@value=\"0\"]",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/_access==private",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule==VALUE",
                "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\"2d-year\"]/rbnfrule_/_value==0"
            },

            // has a value attribute
            { "//ldmlBCP47/version[@number=\"$Revision: 12232 $\"][@cldrVersion=\"29\"]",
                "//ldmlBCP47/version/_cldrVersion==29"
            },

            // has a value & value attribute
            { "//ldmlBCP47/keyword/key[@name=\"ca\"][@description=\"Calendar algorithm key\"][@deprecated=\"false\"][@alias=\"calendar\"][@valueType=\"incremental\"]/type[@name=\"chinese\"][@description=\"Traditional Chinese calendar\"][@deprecated=\"false\"]",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_alias==calendar",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_description==Calendar algorithm key",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_valueType==incremental",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/type[@name=\"chinese\"]/_description==Traditional Chinese calendar",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/_deprecated==false",
                "//ldmlBCP47/keyword/key[@name=\"ca\"]/type[@name=\"chinese\"]/_deprecated==false"
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
                for (Pair<String, String> pathValue : XMLFileReader.loadPathValues(file.toString(), data, true)) {
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

    public void TestValueAttributesWithChildren() {
        Multimap<String, String> m = TreeMultimap.create();
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData dtdData = DtdData.getInstance(type);
            Element special = dtdData.getElementFromName().get("special");
            checkEmpty(m, type, dtdData.ROOT, special, new HashSet<Element>(),
                new ArrayList<Element>(Arrays.asList(dtdData.ROOT)));
        }
        Collection<String> items = m.get("error");
        if (items != null) {
            for (String item : items) {
                errln(item);
            }
        }
        if (isVerbose()) {
            items = m.get("warn");
            if (items != null) {
                for (String item : items) {
                    warnln(item);
                }
            }
        }
    }

    /** make sure that if the final element is empty, there is a value attribute required somewhere in the path
     * @param m 
     * @param type 
     * @param seen 
     */
    private void checkEmpty(Multimap<String, String> m, DtdType type, Element element, Element special, HashSet<Element> seen,
        List<Element> parents) {
        if (seen.contains(element)) {
            return;
        }
        seen.add(element);
        if (element.isDeprecated()) {
            return;
        }

        HashSet<Attribute> valueAttributes = new LinkedHashSet<>();
        HashSet<Attribute> distAttributes = new LinkedHashSet<>();
        for (Attribute attribute : element.getAttributes().keySet()) {
            if (attribute.isDeprecated()) continue;
            switch (attribute.getStatus()) {
            case value:
                //if (attribute.mode == Mode.REQUIRED || attribute.mode == Mode.FIXED) {//strong test
                valueAttributes.add(attribute);
                break;
            case distinguished:
                distAttributes.add(attribute);
                break;
            }
        }
        ElementType elementType = element.getType();
        switch (elementType) {
        case EMPTY:
            if (valueAttributes.isEmpty()) {
                if (!distAttributes.isEmpty()) {
                    m.put("warn", type + "\t||" + showPath(parents) + "||path has neither value NOR value attributes NOR dist. attrs.||");
                } else {
                    m.put("error", "\t||" + showPath(parents) + "||path has neither value NOR value attributes||");
                }
            }
            break;
        case ANY:
        case PCDATA:
            if (!valueAttributes.isEmpty()) {
                m.put("warn", "\t||" + showPath(parents) + "||path has both value AND value attributes||" + valueAttributes + "||");
            }
            break;
        case CHILDREN:
            // first remove deprecateds, and special
            List<Element> children = new ArrayList<>(element.getChildren().keySet());
            for (Iterator<Element> it = children.iterator(); it.hasNext();) {
                Element child = it.next();
                if (child.equals(special) || child.isDeprecated()) {
                    it.remove();
                }
            }

            // if no children left, treat like EMPTY
            if (children.isEmpty()) {
                if (valueAttributes.isEmpty()) {
                    errln(type + "\t|| " + showPath(parents) + "||path has neither value NOR value attributes||");
                }
                break;
            }
            if (!valueAttributes.isEmpty()) {
                switch (element.getName()) {
                case "ruleset":
                    logKnownIssue("cldrbug:8909", "waiting for RBNF to use data");
                    break;
                case "key":
                case "territory":
                case "transform":
                    logKnownIssue("cldrbug:9982", "Lower priority fixes to bad xml");
                    break;
                default:
                    m.put("error", "\t||" + showPath(parents) + "||path has both children AND value attributes"
                        + "||" + valueAttributes
                        + "||" + children + "||");
                    break;
                }
            }
            for (Element child : children) {
                parents.add(child);
                checkEmpty(m, type, child, special, seen, parents);
                parents.remove(parents.size() - 1);
            }
            break;
        }
    }

    private String showPath(List<Element> parents) {
        return "!//" + CollectionUtilities.join(parents, "/");
    }

    public void TestNewDtdData() {
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData dtdData = DtdData.getInstance(type);
            for (Element element : dtdData.getElements()) {
                boolean orderedNew = dtdData.isOrdered(element.name);
                boolean orderedOld = isOrderedOld(element.name, type);
                assertEquals("isOrdered " + type + ":" + element, orderedOld, orderedNew);
                boolean deprecatedNew = dtdData.isDeprecated(element.name, "*", "*");
                boolean deprecatedOld = SUPPLEMENTAL_DATA_INFO.isDeprecated(type, element.name, "*", "*");
                assertEquals("isDeprecated " + type + ":" + element, deprecatedOld, deprecatedNew);

                for (Attribute attribute : element.getAttributes().keySet()) {
                    boolean distinguishedNew = dtdData.isDistinguishing(element.name, attribute.name);
                    boolean distinguishedOld = isDistinguishingOld(type, element.name, attribute.name);
                    assertEquals("isDistinguished " + type
                        + ": elementName.equals(\"" + element.name + "\") && attribute.equals(\"" + attribute.name + "\")", distinguishedOld, distinguishedNew);
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

//    public void TestNonLeafValues() {
//        for (DtdType type : DtdType.values()) {
//            if (type == DtdType.ldmlICU) {
//                continue;
//            }
//            DtdData dtdData = DtdData.getInstance(type);
//            for (Element element : dtdData.getElements()) {
//                if (element.isDeprecated()) {
//                    continue;
//                }
//                switch (element.getType()) {
//                case PCDATA:
//                case EMPTY: continue;
//                case ANY:
//                }
//                for (Attribute attribute : element.getAttributes().keySet()) {
//                    if (attribute.isDeprecated()) {
//                        continue;
//                    }
//                    switch (attribute.getStatus()) {
//                    case value: 
//                        errln(type + "\t" + element + "\t" + attribute + "\t");
//                    }
//                }
//            }
//        }
//        
//    }

    // TESTING CODE
    static final Set<String> orderedElements = Collections.unmodifiableSet(new HashSet<String>(Arrays
        .asList(
            // can prettyprint with TestAttributes

            // DTD: ldml
            // <collation> children
            "base", "optimize", "rules", "settings", "suppress_contractions",

            // <rules> children
            "i", "ic", "p", "pc", "reset", "s", "sc", "t", "tc", "x",

            // <x> children
            "context", "extend", "i", "ic", "p", "pc", "s", "sc", "t", "tc",
            "last_non_ignorable", "last_secondary_ignorable", "last_tertiary_ignorable",

            // <variables> children
            "variable",

            // <rulesetGrouping> children
            "ruleset",

            // <ruleset> children
            "rbnfrule",

            // <exceptions> children (deprecated, use 'suppressions')
            "exception",

            // <suppressions> children
            "suppression",

            // DTD: supplementalData
            // <territory> children
            // "languagePopulation",

            // <postalCodeData> children
            // "postCodeRegex",

            // <characters> children
            // "character-fallback",

            // <character-fallback> children
            // "character",

            // <character> children
            "substitute", // may occur multiple times

            // <transform> children
            "comment", "tRule",

            // <validity> children
            // both of these don't need to be ordered, but must delay changes until after isDistinguished always uses
            // the dtd type
            "attributeValues", // attribute values shouldn't need ordering, as long as these are distinguishing:
            // elements="zoneItem" attributes="type"
            "variable", // doesn't need to be ordered

            // <pluralRules> children
            "pluralRule",

            // <codesByTerritory> children
            // "telephoneCountryCode", // doesn't need ordering, as long as code is distinguishing, telephoneCountryCode
            // code="376"

            // <numberingSystems> children
            // "numberingSystem", // doesn't need ordering, since id is distinguishing

            // <metazoneInfo> children
            // "timezone", // doesn't need ordering, since type is distinguishing

            "attributes", // shouldn't need this in //supplementalData/metadata/suppress/attributes, except that the
            // element is badly designed

            "languageMatch",

            "exception", // needed for new segmentations
            "coverageLevel", // needed for supplemental/coverageLevel.xml
            "coverageVariable", // needed for supplemental/coverageLevel.xml
            "substitute" // needed for characters.xml
    )));

    public static boolean isOrderedOld(String element, DtdType type) {
        return orderedElements.contains(element);
    }

    public boolean isDistinguishingOld(DtdType dtdType, String elementName, String attribute) {
        switch (dtdType) {
        case ldml:
            return attribute.equals("_q")
                || attribute.equals("key")
                || attribute.equals("indexSource")
                || attribute.equals("request")
                || attribute.equals("count")
                || attribute.equals("id")
                || attribute.equals("registry")
                || attribute.equals("alt")
                || attribute.equals("mzone")
                || attribute.equals("from")
                || attribute.equals("to")
                || attribute.equals("value") && !elementName.equals("rbnfrule")
                || attribute.equals("yeartype")
                || attribute.equals("numberSystem")
                || attribute.equals("parent")
                || elementName.equals("annotation") && attribute.equals("cp")
                || (attribute.equals("type")
                    && !elementName.equals("default")
                    && !elementName.equals("measurementSystem")
                    && !elementName.equals("mapping")
                    && !elementName.equals("abbreviationFallback")
                    && !elementName.equals("preferenceOrdering"))
                || (elementName.equals("parseLenients") && (attribute.equals("scope") || attribute.equals("level")))
                || (elementName.equals("parseLenient") && attribute.equals("sample"))
                || (elementName.equals("ordinalMinimalPairs") && attribute.equals("ordinal"))
                || (elementName.equals("styleName") && attribute.equals("subtype"));
        case ldmlBCP47:
            return attribute.equals("_q")
                //|| attribute.equals("alias")
                || attribute.equals("name")
                || attribute.equals("extension");
        case supplementalData:
            return attribute.equals("_q")
                || (elementName.equals("matchVariable") && attribute.equals("id"))
                || attribute.equals("iso4217")
                || attribute.equals("iso3166")
                || attribute.equals("code")
                || (attribute.equals("type") && !elementName.equals("calendarSystem") && !elementName.equals("mapZone")
                    && !elementName.equals("numberingSystem") && !elementName.equals("variable"))
                || attribute.equals("id") && elementName.equals("variable")
                || attribute.equals("alt")
                || attribute.equals("dtds")
                || attribute.equals("idStatus")
                || elementName.equals("deprecatedItems")
                    && (attribute.equals("type") || attribute.equals("elements") || attribute.equals("attributes") || attribute.equals("values"))
                || elementName.equals("character")
                    && attribute.equals("value")
                || elementName.equals("dayPeriodRules")
                    && attribute.equals("locales")
                || elementName.equals("dayPeriodRule")
                    && (attribute.equals("type"))
                || elementName.equals("metazones") && attribute.equals("type")
                || elementName.equals("subgroup") && attribute.equals("subtype")
                || elementName.equals("mapZone")
                    && (attribute.equals("other") || attribute.equals("territory"))
                || elementName.equals("postCodeRegex")
                    && attribute.equals("territoryId")
                || elementName.equals("calendarPreference")
                    && attribute.equals("territories")
                || elementName.equals("minDays")
                    && attribute.equals("count")
                || elementName.equals("firstDay")
                    && attribute.equals("day")
                || elementName.equals("weekendStart")
                    && attribute.equals("day")
                || elementName.equals("weekendEnd")
                    && attribute.equals("day")
                || elementName.equals("measurementSystem")
                    && attribute.equals("category")
                || elementName.equals("unitPreferences")
                    && (attribute.equals("category") || attribute.equals("usage") || attribute.equals("scope"))
                || elementName.equals("unitPreference")
                    && attribute.equals("regions")
                || elementName.equals("distinguishingItems")
                    && attribute.equals("attributes")
                || elementName.equals("codesByTerritory")
                    && attribute.equals("territory")
                || elementName.equals("currency")
                    && attribute.equals("iso4217")
                || elementName.equals("territoryAlias")
                    && attribute.equals("type")
                || elementName.equals("territoryCodes")
                    && attribute.equals("type")
                || elementName.equals("group")
                    && (attribute.equals("status")) //  || attribute.equals("grouping")
                || elementName.equals("plurals")
                    && attribute.equals("type")
                || elementName.equals("pluralRules")
                    && attribute.equals("locales")
                || elementName.equals("pluralRule")
                    && attribute.equals("count")
                || elementName.equals("pluralRanges")
                    && attribute.equals("locales")
                || elementName.equals("pluralRange")
                    && (attribute.equals("start") || attribute.equals("end"))
                || elementName.equals("hours")
                    && (attribute.equals("preferred") || attribute.equals("allowed"))
                || elementName.equals("personList") && attribute.equals("type")
                || elementName.equals("likelySubtag")
                    && attribute.equals("from")
                || elementName.equals("rgPath")
                    && attribute.equals("path")
                || elementName.equals("timezone")
                    && attribute.equals("type")
                || elementName.equals("usesMetazone")
                    && (attribute.equals("to") || attribute.equals("from")) // attribute.equals("mzone") ||
                || elementName.equals("numberingSystem")
                    && attribute.equals("id")
                || elementName.equals("group")
                    && attribute.equals("type")
                || elementName.equals("currency")
                    && attribute.equals("from")
                || elementName.equals("currency")
                    && attribute.equals("to")
                || elementName.equals("currency")
                    && attribute.equals("iso4217")
                || (elementName.equals("parentLocale") || elementName.equals("languageGroup"))
                    && attribute.equals("parent")
                || elementName.equals("currencyCodes")
                    && attribute.equals("type")
                || elementName.equals("approvalRequirement")
                    && (attribute.equals("locales") || attribute.equals("paths"))
                || elementName.equals("weekOfPreference")
                    && attribute.equals("locales")
                || elementName.equals("coverageVariable")
                    && attribute.equals("key")
                || elementName.equals("coverageLevel")
                    && (attribute.equals("inLanguage") || attribute.equals("inScript") || attribute.equals("inTerritory") || attribute.equals("match"))
                || elementName.equals("languageMatch")
                    && (attribute.equals("desired") || attribute.equals("supported"))
                || (elementName.equals("transform") && (attribute.equals("source") || attribute.equals("target") || attribute.equals("direction") || attribute
                    .equals("variant")));

        case keyboard:
            return attribute.equals("_q")
                || elementName.equals("keyboard") && attribute.equals("locale")
                || elementName.equals("keyMap") && attribute.equals("modifiers")
                || elementName.equals("map") && attribute.equals("iso")
                || elementName.equals("map") && attribute.equals("optional")
                || elementName.equals("map") && attribute.equals("longpress-status")
                || elementName.equals("transforms") && attribute.equals("type")
                || elementName.equals("transform") && attribute.equals("from")
                || elementName.equals("import") && attribute.equals("path")
                || elementName.equals("reorder") && attribute.equals("before")
                || elementName.equals("reorder") && attribute.equals("from")
                || elementName.equals("reorder") && attribute.equals("after")
                || elementName.equals("layer") && attribute.equals("modifier")
                || elementName.equals("switch") && attribute.equals("iso")
                || elementName.equals("transform") && attribute.equals("before")
                || elementName.equals("transform") && attribute.equals("after")
                || elementName.equals("backspace") && attribute.equals("before")
                || elementName.equals("backspace") && attribute.equals("from")
                || elementName.equals("backspace") && attribute.equals("after")
                || elementName.equals("vkeys") && attribute.equals("type")
                || elementName.equals("flick") && attribute.equals("directions")
                || elementName.equals("row") && attribute.equals("keys")
                || elementName.equals("vkey") && attribute.equals("iso")
                || elementName.equals("display") && attribute.equals("to")
                || elementName.equals("flicks") && attribute.equals("iso");

        case platform:
            return attribute.equals("_q")
                || elementName.equals("platform") && attribute.equals("id")
                || elementName.equals("map") && attribute.equals("keycode");
        case ldmlICU:
            return false;
        default:
            throw new IllegalArgumentException("Type is wrong: " + dtdType);
        }
        // if (result != matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true)) {
        // matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true);
        // throw new IllegalArgumentException("Failed: " + elementName + ", " + attribute);
        // }
    }

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public static Set<String> getSerialElements() {
        return orderedElements;
    }
}
