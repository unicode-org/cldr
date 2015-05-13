package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.ChainedMap.M3;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;

public class GenerateSubdivisions {

    public static void main(String[] args) {
        loadIso();
        SubdivisionNode.printXml();
    }

    static class SubdivisionNode {
        static LocaleDisplayNames ENGLISH = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
        static final M3<String, String, String> NAMES = ChainedMap.of(
            new TreeMap<String,Object>(),
            new TreeMap<String,Object>(),
            String.class
            );
        static final Map<String,String> TO_COUNTRY_CODE = new TreeMap<String,String>();
        static public void addName(String code, String lang, String value) {
            int parenPos = value.indexOf("(see also separate country");
            if (parenPos >= 0) {
                String paren = value.substring(value.length()-3,value.length()-1);
                String old = TO_COUNTRY_CODE.get(code);
                if (old != null) {
                    System.out.println("Duplicate: " + code + "\t" + old + "\t" + paren);
                }
                TO_COUNTRY_CODE.put(code, paren);
                value = value.substring(0,parenPos).trim();
            }
            NAMES.put(code, lang, value);
        }
        static final SubdivisionNode BASE = new SubdivisionNode("001").addName("en", "World");

        final String code;
        final Map<String, SubdivisionNode> children = new LinkedHashMap<>();

        public SubdivisionNode(String code) {
            this.code = code;
        }
        public SubdivisionNode addName(String lang, String value) {
            NAMES.put(code, lang, value);
            return this;
        }
        static final SubdivisionNode addNode(SubdivisionNode lastSubdivision, String subdivision) {
            // "NZ-S", x
            if (lastSubdivision == null) {
                String region = subdivision.substring(0,subdivision.indexOf('-'));
                lastSubdivision = BASE.children.get(region);
                if (lastSubdivision == null) {
                    lastSubdivision = new SubdivisionNode(region).addName("en", ENGLISH.regionDisplayName(region));
                    BASE.children.put(region, lastSubdivision);
                }
                return add(lastSubdivision, subdivision);
            }
            add(lastSubdivision, subdivision);
            return lastSubdivision;
        }

        private static SubdivisionNode add(SubdivisionNode subdivisionNode1, String subdivision2) {
            SubdivisionNode subdivisionNode2 = subdivisionNode1.children.get(subdivision2);
            if (subdivisionNode2 == null) {
                subdivisionNode2 = new SubdivisionNode(subdivision2);
            }
            subdivisionNode1.children.put(subdivision2, subdivisionNode2);
            return subdivisionNode2;
        }

        private static String getName(SubdivisionNode base2) {
            Map<String, String> map = NAMES.get(base2.code);
            if (map == null) {
                return "???";
            }
            String name = map.get("en");
            if (name == null) {
                name = map.entrySet().iterator().next().getValue();
            }
            return name;
        }

        public static void print() {
            print(BASE, 0);
            for (Entry<String, String> entry : TO_COUNTRY_CODE.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
        private static void print(SubdivisionNode base2, int indent) {
            final String indentString = Utility.repeat("\t", indent);
            System.out.println(indentString + base2.code 
                + "\t" + getName(base2));     
            if (base2.children.isEmpty()) {
                return;
            }
            for (SubdivisionNode child : base2.children.values()) {
                print(child,indent+1);
            }
        }
        static void printXml() {
            /*
<subdivisionContainment>
    <group type="NZ" category="island" contains="NZ-N NZ-S"/> <!-- New Zealand -->
    <group type="NZ" category="special island authority" contains="NZ-CIT"/> <!-- New Zealand -->
    <group type="NZ-N" contains="NZ-AUK NZ-BOP NZ-GIS NZ-HKB NZ-MWT NZ-NTL NZ-AUK NZ-TKI NZ-WGN NZ-WKO"/> <!-- North Island -->
    <group type="NZ-S" contains="NZ-CAN NZ-MBH NZ-STL NZ-NSN NZ-OTA NZ-TAS NZ-WTC"/> <!-- South Island -->
  </subdivisionContainment>
             */
            System.out.println("<?xml version='1.0' encoding='UTF-8' ?>\n"
                +"<!DOCTYPE supplementalData SYSTEM '../../common/dtd/ldmlSupplemental.dtd'>\n"
                +"<!--\n"
                +"Copyright © 1991-2013 Unicode, Inc.\n"
                +"CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n"
                +"For terms of use, see http://www.unicode.org/copyright.html\n"
                +"-->\n"
                +"\n"
                +"<supplementalData>\n"
                +"    <version number='$Revision: 8268 $'/>\n"
                +"    <generation date='$Date: 2013-03-01 15:26:02 +0100 (Fri, 01 Mar 2013) $'/>\n"
                +"\t<subdivisionContainment>");
            printXml(BASE, 0);
            System.out.println("\t</subdivisionContainment>\n</supplementalData>");
            for (Entry<String, String> entry : TO_COUNTRY_CODE.entrySet()) {
                // <languageAlias type="art_lojban" replacement="jbo" reason="deprecated"/> <!-- Lojban -->
                System.out.println("<subdivisionAlias"
                    + " type=\"" + entry.getKey() + "\""
                    + " replacement=\"" + entry.getValue() + "\""
                    + " reason=\"" + "overlong" + "\"/>");
            }
        }
        private static void printXml(SubdivisionNode base2, int indent) {
            if (base2.children.isEmpty()) {
                return;
            }
            String type = base2.code;
            int hyphenPos = type.indexOf('-');
            if (hyphenPos >= 0) {
                String subtype = type.substring(hyphenPos+1);
                type = type.substring(0,hyphenPos);
                System.out.print("\t\t" + "<subgroup"
                    + " type=\"" + type + "\""
                    + " subtype=\"" + subtype + "\""
                    + " contains=\"");
            } else {
                System.out.print("\t\t" + "<subgroup"
                    + " type=\"" + type + "\""
                    + " contains=\"");
            }
            boolean first = true;
            for (String child : base2.children.keySet()) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(' ');
                }
                String subregion = child.substring(child.indexOf('-')+1);
                System.out.print(subregion);
            }
            System.out.println("</subgroup>");
            for (SubdivisionNode child : base2.children.values()) {
                printXml(child,indent);
            }
        }
    }

    public static void loadIso() {
        //    <country id="AD" version="16">
        //           <subdivision-code footnote="*">AD-02</subdivision-code>
        //             <subdivision-locale lang3code="eng" xml:lang="en">
        //                  <subdivision-locale-name>Otago</subdivision-locale-name>

        List<Pair<String, String>> pathValues = XMLFileReader.loadPathValues(
            CLDRPaths.DATA_DIRECTORY + "iso/iso_country_codes.xml", 
            new ArrayList<Pair<String, String>>(), false);
        XPathParts parts = new XPathParts();
        int maxIndent = 0;
        SubdivisionNode lastNode = null;
        String lastCode = null;

        for (Pair<String, String> pair : pathValues) {
            String path = pair.getFirst();
            boolean code = path.contains("/subdivision-code");
            boolean name = path.contains("/subdivision-locale-name");
            if (!code && !name) {
                continue;
            }
            parts.set(path);
            String value = pair.getSecond();
            if (name) {
                String lang = parts.getAttributeValue(-2, "xml:lang");
                if (lang == null) {
                    lang = parts.getAttributeValue(-2, "lang3code");
                }
                SubdivisionNode.addName(lastCode, lang, value);
                //System.out.println(count + Utility.repeat("\t", indent) + "\tlang=" + lang + ":\t«" + value + "»\t");     
            } else {
                int countSubdivision = 0;
                for (int i = 0; i < parts.size(); ++i) {
                    if (parts.getElement(i).equals("subdivision")) {
                        ++countSubdivision;
                    }
                }
                if (maxIndent < countSubdivision) {
                    maxIndent = countSubdivision;
                }
                if (countSubdivision == 1) {
                    lastNode = SubdivisionNode.addNode(null, value);
                } else {
                    lastNode = SubdivisionNode.addNode(lastNode, value);
                }
                lastCode = value;
                //System.out.println(++count + Utility.repeat("\t", indent) + "code=" + value);
            }
        }
    }
}
