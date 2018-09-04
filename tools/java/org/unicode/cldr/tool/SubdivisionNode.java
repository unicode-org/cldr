package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.GenerateSubdivisions.SubdivisionInfo;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.WikiSubdivisionLanguages;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public class SubdivisionNode {
    static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    static final Map<String, R2<List<String>, String>> territoryAliases = SDI.getLocaleAliasInfo().get("territory");
    static final Set<String> containment = SDI.getContainers();
    static final Map<String, Map<LstrField, String>> codeToData = StandardCodes.getEnumLstreg().get(LstrType.region);

    static LocaleDisplayNames ENGLISH_ICU = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();
    static final Comparator<String> ROOT_COL;
    static {
        RuleBasedCollator _ROOT_COL = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
        _ROOT_COL.setNumericCollation(true);
        _ROOT_COL.freeze();
        ROOT_COL = (Comparator) _ROOT_COL;
    }
    static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final CLDRFile ENGLISH_CLDR = CLDR_CONFIG.getEnglish();
    static final Normalizer2 nfc = Normalizer2.getNFCInstance();

    public static String convertToCldr(String regionOrSubdivision) {
        return SubdivisionNames.isRegionCode(regionOrSubdivision) ? regionOrSubdivision.toUpperCase(Locale.ROOT)
            : regionOrSubdivision.replace("-", "").toLowerCase(Locale.ROOT);
    }

    final SubdivisionSet sset;
    final String code;
    final int level;
    final SubdivisionNode parent;
    final Map<String, SubdivisionNode> children = new TreeMap<>(ROOT_COL);

    public SubdivisionNode(String code, SubdivisionNode parent, SubdivisionSet sset) {
        this.code = code;
        this.level = parent == null ? -1 : parent.level + 1;
        this.parent = parent;
        this.sset = sset;
        sset.ID_TO_NODE.put(code, this);
    }

    public SubdivisionNode addName(String lang, String value) {
        sset.NAMES.put(code, lang, value);
        return this;
    }

    static class SubdivisionSet {

        final M3<String, String, String> NAMES = ChainedMap.of(
            new TreeMap<String, Object>(),
            new TreeMap<String, Object>(),
            String.class);
        final Map<String, String> TO_COUNTRY_CODE = new TreeMap<String, String>();
        final Relation<String, String> ID_SAMPLE = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        final Map<String, String> SUB_TO_CAT = new TreeMap<>();
        final Relation<String, String> REGION_CONTAINS = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        final Map<String, SubdivisionNode> ID_TO_NODE = new HashMap<>();

        final SubdivisionNode BASE = new SubdivisionNode("001", null, this).addName("en", "World");

        public void addName(String code, String lang, String value) {
            int parenPos = value.indexOf("(see also separate country");
            if (parenPos >= 0) {
                /*
                Error: (TestSubdivisions.java:66) : country BQ = subdivisionNL-BQ1: expected "Caribbean Netherlands", got "Bonaire"
                Error: (TestSubdivisions.java:66) : country BQ = subdivisionNL-BQ2: expected "Caribbean Netherlands", got "Saba"
                Error: (TestSubdivisions.java:66) : country BQ = subdivisionNL-BQ3: expected "Caribbean Netherlands", got "Sint Eustatius"
                Error: (TestSubdivisions.java:66) : country SJ = subdivisionNO-21: expected "Svalbard & Jan Mayen", got "Svalbard"
                Error: (TestSubdivisions.java:66) : country SJ = subdivisionNO-22: expected "Svalbard & Jan Mayen", got "Jan Mayen"
                 */
                // OLD code to guess country from comment
//              String paren = value.substring(value.length() - 3, value.length() - 1);
//                if (!paren.equals("BQ") && !paren.equals("SJ")) {
//                    String old = TO_COUNTRY_CODE.get(code);
//                    if (old != null) {
//                        System.err.println("Duplicate: " + code + "\t" + old + "\t" + paren);
//                    }
//                    TO_COUNTRY_CODE.put(code, paren);
//                }
                value = value.substring(0, parenPos).trim();
            }
            value = value.replace("*", "");
            NAMES.put(code, lang, value);
        }




        static final String[] CRUFT = {
            "Emirate",
            "Parish",
            "County",
            "District",
            "Region",
            "Province of",
            "Province",
            "Republic",
            ", Barbados",
            ", Burkina Faso",
            "Governorate",
            "Department",
            "Canton of",
            "(Région des)",
            "(Région du)",
            "(Région de la)",
            "Autonomous",
            "Archipelago of",
            "Canton",
            "kanton",
            ", Bahamas",
            "province",
            "(Région)",
            "(Région de l')",
            ", Cameroon",
            "State of",
            "State",
            "Metropolitan Borough of",
            "London Borough of",
            "Royal Borough of",
            "Borough of",
            "Borough",
            "Council of",
            "Council",
            "City of",
            ", The",
            "prefecture",
            "Prefecture",
            "municipality"
        };

        static final Pattern CRUFT_PATTERN = PatternCache.get("(?i)\\b" + CollectionUtilities.join(CRUFT, "|") + "\\b");
        static final Pattern BRACKETED = PatternCache.get("\\[.*\\]");

        static String clean(String input) {
            if (input == null) {
                return input;
            }
            // Quick & dirty
            input = BRACKETED.matcher(input).replaceAll("");
            input = CRUFT_PATTERN.matcher(input).replaceAll("");
//            for (String cruft : CRUFT) {
//                int pos = input.indexOf(cruft);
//                if (pos >= 0) {
//                    input = input.substring(0,pos) + input.substring(pos + cruft.length());
//                }
//            }
            input = input.replace("  ", " ");
            if (input.endsWith(",")) {
                input = input.substring(0, input.length() - 1);
            }
            return fixName(input);
        }



        private static void appendName(CLDRFile fileSubdivisions, final String sdCode, String name, String level) throws IOException {
            if (name == null) {
                return;
            }
            String cldrCode = convertToCldr(sdCode);
            String path = "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"" + cldrCode + "\"]";
            String oldValue = fileSubdivisions.getStringValue(path);
            if (oldValue != null) {
                return; // don't override old values
            }
            fileSubdivisions.add(path, name);
            if (level != null) {
                fileSubdivisions.addComment(path, level, CommentType.LINE);
            }
        }

        private boolean isKosher(String regionCode) {
            if (regionCode.equals("001")) {
                return false;
            }
            if (territoryAliases.containsKey(regionCode)
                || containment.contains(regionCode)
                || codeToData.get(regionCode).get(LstrField.Description).contains("Private use")) {
                Set<String> rc = REGION_CONTAINS.get(regionCode);
                if (rc != null) {
                    throw new IllegalArgumentException("? " + regionCode + ": " + rc);
                }
                return false;
            }
            return true;
        }

        private static void addChildren(Set<SubdivisionNode> ordered, Map<String, SubdivisionNode> children2) {
            TreeMap<String, SubdivisionNode> temp = new TreeMap<>(ROOT_COL);
            temp.putAll(children2);
            ordered.addAll(temp.values());
            for (SubdivisionNode n : temp.values()) {
                if (!n.children.isEmpty()) {
                    addChildren(ordered, n.children);
                }
            }
        }

        static Map<String, String> NAME_CORRECTIONS = new HashMap<>();
//      static {
//          Splitter semi = Splitter.on(';').trimResults();
//          for (String s : FileUtilities.in(ISO_COUNTRY_CODES, "en-subdivisions-corrections.txt")) {
//              if (s.startsWith("#")) {
//                  continue;
//              }
//              s = s.trim();
//              if (s.isEmpty()) {
//                  continue;
//              }
//              List<String> parts = semi.splitToList(s);
//              NAME_CORRECTIONS.put(convertToCldr(parts.get(0)), parts.get(1));
//          }
//      }


        private String getBestName(String value, boolean useIso) {
            if (value.equals("cnah")) {
                int debug = 0;
            }
            String cldrName = null;
            cldrName = NAME_CORRECTIONS.get(value);
            if (cldrName != null) {
                return fixName(cldrName);
            }
            R2<List<String>, String> subdivisionAlias = SubdivisionInfo.SUBDIVISION_ALIASES_FORMER.get(value);
            if (subdivisionAlias != null) {
                String country = subdivisionAlias.get0().get(0);
                cldrName = ENGLISH_CLDR.getName(CLDRFile.TERRITORY_NAME, country);
                if (cldrName != null) {
                    return fixName(cldrName);
                }
            }


            cldrName = SubdivisionInfo.SUBDIVISION_NAMES_ENGLISH_FORMER.get(value);
            if (cldrName != null) {
                return fixName(cldrName);
            }
            
            Collection<String> oldAliases = SubdivisionInfo.subdivisionIdToOld.get(value);
            if (oldAliases != null) {
                for (String oldAlias : oldAliases) {
                    cldrName = SubdivisionInfo.SUBDIVISION_NAMES_ENGLISH_FORMER.get(oldAlias);
                    if (cldrName != null) {
                        return fixName(cldrName);
                    }
                }
            }

            if (useIso) {
                cldrName = getIsoName(value);
                if (cldrName == null) {
                    cldrName = "UNKNOWN";
                    //throw new IllegalArgumentException("Failed to find name: " + value);
                }
                return fixName(cldrName);
            }
            return null;
        }

        private static String fixName(String name) {
            return name == null ? null : nfc.normalize(name.replace('\'', '’').replace("  ", " ").trim());
        }

        public SubdivisionSet(String sourceFile) {

            //    <country id="AD" version="16">
            //           <subdivision-code footnote="*">AD-02</subdivision-code>
            //             <subdivision-locale lang3code="eng" xml:lang="en">
            //                  <subdivision-locale-name>Otago</subdivision-locale-name>

            List<Pair<String, String>> pathValues = XMLFileReader.loadPathValues(
                sourceFile,
                new ArrayList<Pair<String, String>>(), false);
            XPathParts parts = new XPathParts();
            int maxIndent = 0;
            SubdivisionNode lastNode = null;
            String lastCode = null;
            Set<String> conflictingTargetCountries = new HashSet<>();

            for (Pair<String, String> pair : pathValues) {
                String path = pair.getFirst();
                boolean code = path.contains("/subdivision-code");
                boolean name = path.contains("/subdivision-locale-name");
                boolean nameCat = path.contains("/category-name");
                boolean relatedCountry = path.contains("/subdivision-related-country");

                //    <country id="AD" version="16">
                //       <category id="262">
                //  <category-name lang3code="fra" xml:lang="fr">paroisse</category-name>
                //  <category-name lang3code="eng" xml:lang="en">parish</category-name>
                // also languages in region...

                // new XML from ISO, so we don't have to guess the country code:
                //            <subdivision-code footnote="*">NL-BQ1</subdivision-code>
                //            <subdivision-related-country country-id="BQ" xml:lang="en">BONAIRE, SINT EUSTATIUS AND SABA</subdivision-related-country>

                if (!code && !name && !nameCat && !relatedCountry) {
                    continue;
                }
                parts.set(path);
                String value = pair.getSecond();
                if (relatedCountry) {
                    String target = parts.getAttributeValue(-1, "country-id");
                    // remove conflicting target countries
                    for (Entry<String, String> entry : TO_COUNTRY_CODE.entrySet()) {
                        if (entry.getValue().equals(target)) {
                            conflictingTargetCountries.add(target);
                            TO_COUNTRY_CODE.remove(entry.getKey(), target); // there can be at most one
                            break;
                        }
                    }
                    if (!conflictingTargetCountries.contains(target)) {
                        TO_COUNTRY_CODE.put(lastCode, target);
                        //System.out.println(lastCode + " => " + target);
                    }
                } else if (name) {
                    int elementNum = -2;
                    String lang = parts.getAttributeValue(elementNum, "xml:lang");
                    if (lang == null) {
                        lang = parts.getAttributeValue(elementNum, "lang3code");
                    }
                    addName(lastCode, lang, value);
                    //output.println(count + Utility.repeat("\t", indent) + "\tlang=" + lang + ":\t«" + value + "»\t");
                } else if (nameCat) {
                    //country-codes[@generated="2015-05-04T15:40:13.424465+02:00"]/country[@id="AD"][@version="16"]/category[@id="262"]/category-name[@lang3code="fra"][@xml:lang="fr"]
                    int elementNum = -1;
                    String lang = parts.getAttributeValue(elementNum, "xml:lang");
                    if (lang == null) {
                        lang = parts.getAttributeValue(elementNum, "lang3code");
                    }
                    String category = parts.getAttributeValue(-2, "id");
                    addName(category, lang, value);
                    //output.println(count + Utility.repeat("\t", indent) + "\tlang=" + lang + ":\t«" + value + "»\t");
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
                    value = convertToCldr(value);
                    if (countSubdivision == 1) {
                        lastNode = addNode(null, value);
                    } else {
                        lastNode = addNode(lastNode, value);
                    }
                    lastCode = value;
                    int subdivisionElement = parts.findElement("subdivision");
                    String id = parts.getAttributeValue(subdivisionElement, "category-id");
                    addIdSample(id, value);
                    //<subdivision category-id="262">//<subdivision-code footnote="*">AD-06</subdivision-code>
                    // <subdivision category-id="262">
                    //output.println(++count + Utility.repeat("\t", indent) + "code=" + value);
                }
            }
        }

        public void addIdSample(String id, String value) {
            SUB_TO_CAT.put(value, id);
            ID_SAMPLE.put(getIsoName(id), value);
        }

        final SubdivisionNode addNode(SubdivisionNode lastSubdivision, String subdivision) {
            // "NZ-S", x
            String region = SubdivisionNames.getRegionFromSubdivision(subdivision);
            REGION_CONTAINS.put(region, subdivision);
            if (lastSubdivision == null) {
                lastSubdivision = BASE.children.get(region);
                if (lastSubdivision == null) {
                    lastSubdivision = new SubdivisionNode(region, BASE, this).addName("en", ENGLISH_ICU.regionDisplayName(region));
                    BASE.children.put(region, lastSubdivision);
                }
                return add(lastSubdivision, subdivision);
            }
            add(lastSubdivision, subdivision);
            return lastSubdivision;
        }

        private SubdivisionNode add(SubdivisionNode subdivisionNode1, String subdivision2) {
            SubdivisionNode subdivisionNode2 = subdivisionNode1.children.get(subdivision2);
            if (subdivisionNode2 == null) {
                subdivisionNode2 = new SubdivisionNode(subdivision2, subdivisionNode1, this);
            }
            subdivisionNode1.children.put(subdivision2, subdivisionNode2);
            return subdivisionNode2;
        }

        private String getName(SubdivisionNode base2) {
            return getIsoName(base2.code);
        }

        private String getIsoName(String code) {
            if (code == null) {
                return null;
            }
            Map<String, String> map = NAMES.get(code);
            if (map == null) {
                return "???";
            }
            String name = map.get("en");
            if (name != null) {
                return name;
            }
            name = map.get("es");
            if (name != null) {
                return name;
            }
            name = map.get("fr");
            if (name != null) {
                return name;
            }
            if (name == null) {
                name = map.entrySet().iterator().next().getValue();
            }
            return name;
        }
        public void print(PrintWriter out) {
            print(out, 0, "", BASE);
            for (Entry<String, String> entry : TO_COUNTRY_CODE.entrySet()) {
                out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
        private void print(PrintWriter out, int indent, String prefix, SubdivisionNode base2) {
            if (!prefix.isEmpty()) {
                prefix += "\t";
            }
            prefix += base2.code;
            final String indentString = Utility.repeat("\t", 4-indent);
            out.println(prefix + indentString + getName(base2));
            if (base2.children.isEmpty()) {
                return;
            }
            for (SubdivisionNode child : base2.children.values()) {
                print(out, indent + 1, prefix, child);
            }
        }
    }

    static class SubDivisionExtractor {
        final SubdivisionSet sdset;
        final Validity validityFormer; 
        final Map<String, R2<List<String>, String>> subdivisionAliasesFormer;
        final Relation<String, String> formerRegionToSubdivisions;

        public SubDivisionExtractor(SubdivisionSet sdset, 
            Validity validityFormer, 
            Map<String, R2<List<String>, String>> subdivisionAliasesFormer, 
            Relation<String, String> formerRegionToSubdivisions) {
            this.sdset = sdset;
            this.validityFormer = validityFormer;
            this.subdivisionAliasesFormer = subdivisionAliasesFormer;
            this.formerRegionToSubdivisions = formerRegionToSubdivisions;
        }

        void printXml(Appendable output) throws IOException {

            /*
            <subdivisionContainment>
            <group type="NZ" category="island" contains="NZ-N NZ-S"/> <!-- New Zealand -->
            <group type="NZ" category="special island authority" contains="NZ-CIT"/> <!-- New Zealand -->
            <group type="NZ-N" contains="NZ-AUK NZ-BOP NZ-GIS NZ-HKB NZ-MWT NZ-NTL NZ-AUK NZ-TKI NZ-WGN NZ-WKO"/> <!-- North Island -->
            <group type="NZ-S" contains="NZ-CAN NZ-MBH NZ-STL NZ-NSN NZ-OTA NZ-TAS NZ-WTC"/> <!-- South Island -->
            </subdivisionContainment>
             */
            output.append(
                DtdType.supplementalData.header(MethodHandles.lookup().lookupClass())
                + "\t<version number=\"$Revision" /*hack to stop SVN changing this*/ + "$\"/>\n"
                + "\t<subdivisionContainment>\n");
            printXml(output, sdset.BASE, 0);
            output.append("\t</subdivisionContainment>\n</supplementalData>\n");
        }

//        private static String header(DtdType type) {
//            return "<?xml version='1.0' encoding='UTF-8' ?>\n"
//                + "<!DOCTYPE " + type // supplementalData
//                + " SYSTEM '../../" + type.dtdPath + "'>\n" // "common/dtd/ldmlSupplemental.dtd"
//                + "<!--\n"
//                + "Copyright © 1991-2013 Unicode, Inc.\n"
//                + "CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n"
//                + "For terms of use, see http://www.unicode.org/copyright.html\n"
//                + "-->\n";
//        }

        void printAliases(Appendable output) throws IOException {
            addAliases(output, sdset.TO_COUNTRY_CODE.keySet());

            // Get the old validity data
            Map<Status, Set<String>> oldSubdivisionData = validityFormer.getStatusToCodes(LstrType.subdivision);
            Set<String> missing = new TreeSet<>(ROOT_COL);
            missing.addAll(sdset.TO_COUNTRY_CODE.keySet());
            Set<String> nowValid = sdset.ID_TO_NODE.keySet();
            for (Entry<Status, Set<String>> e : oldSubdivisionData.entrySet()) {
                Status v = e.getKey();
                if (v == Status.unknown) {
                    continue;
                }
                Set<String> set = e.getValue();
                for (String sdcodeRaw : set) {
                    String sdcode = sdcodeRaw; // .toUpperCase(Locale.ROOT);
//                  sdcode = sdcode.substring(0,2) + "-" + sdcode.substring(2);
                    if (!nowValid.contains(sdcode)) {
                        missing.add(sdcode);
                    }
                }
            }
            missing.removeAll(sdset.TO_COUNTRY_CODE.keySet());
            addAliases(output, missing);
        }

        private void addAliases(Appendable output, Set<String> missing) throws IOException {
            for (String toReplace : missing) {
                List<String> replaceBy = null;
                String reason = "deprecated";
                R2<List<String>, String> aliasInfo = subdivisionAliasesFormer.get(toReplace);
                if (aliasInfo != null) {
                    replaceBy = aliasInfo.get0(); //  == null ? null : CollectionUtilities.join(aliasInfo.get0(), " ");
                    reason = aliasInfo.get1();
                    System.out.println("Adding former alias: " + toReplace + " => " + replaceBy);
                } else {
                    String replacement = sdset.TO_COUNTRY_CODE.get(toReplace);
                    if (replacement != null) {
                        replaceBy = Collections.singletonList(replacement);
                        reason = "overlong";
                        System.out.println("Adding country code alias: " + toReplace + " => " + replaceBy);
                    }
                }
                addAlias(output, toReplace, replaceBy, reason);
            }
        }

        private void addAlias(Appendable output, final String toReplace, final List<String> replaceBy, final String reason) throws IOException {
            // <languageAlias type="art_lojban" replacement="jbo" reason="deprecated"/> <!-- Lojban -->
            output.append("\t\t\t");
            if (replaceBy == null) {
                output.append("<!-- ");
            }
            output.append("<subdivisionAlias"
                + " type=\"" + toReplace + "\""
                + " replacement=\"" + (replaceBy == null ? toReplace.substring(0, 2) + "?" : CollectionUtilities.join(replaceBy, " ")) + "\""
                + " reason=\"" + reason + "\"/>"
                + (replaceBy == null ? " <!- - " : " <!-- ")
                + sdset.getBestName(toReplace, true) + " => " + (replaceBy == null ? "??" : getBestName(replaceBy, true)) + " -->"
                + "\n");
        }

        private String getBestName(List<String> replaceBy, boolean useIso) {
            StringBuilder result = new StringBuilder();
            for (String s : replaceBy) {
                if (result.length() != 0) {
                    result.append(", ");
                }
                if (SubdivisionNames.isRegionCode(s)) {
                    result.append(ENGLISH_CLDR.getName(CLDRFile.TERRITORY_NAME, s));
                } else {
                    result.append(sdset.getBestName(s, useIso));
                }
            }
            return result.toString();
        }

        private void printXml(Appendable output, SubdivisionNode base2, int indent) throws IOException {
            if (base2.children.isEmpty()) {
                return;
            }
            String type = base2.code;
            if (base2 != sdset.BASE) {
                type = convertToCldr(type);
                output.append("\t\t" + "<subgroup"
                    + " type=\"" + type + "\""
                    + " contains=\"");
                boolean first = true;
                for (String child : base2.children.keySet()) {
                    if (first) {
                        first = false;
                    } else {
                        output.append(' ');
                    }
                    String subregion = convertToCldr(child);
                    output.append(subregion);
                }
                output.append("\"/>\n");
            }
            for (SubdivisionNode child : base2.children.values()) {
                printXml(output, child, indent);
            }
        }

        public void printSamples(Appendable pw) throws IOException {
            Set<String> seen = new HashSet<>();
            for (Entry<String, Set<String>> entry : sdset.ID_SAMPLE.keyValuesSet()) {
                pw.append(entry.getKey());
                //int max = 10;
                seen.clear();
                for (String sample : entry.getValue()) {
                    String region = sample.substring(0, 2);
                    if (seen.contains(region)) {
                        continue;
                    }
                    seen.add(region);
                    pw.append(";\t" + ENGLISH_ICU.regionDisplayName(region) + ": " + sdset.getIsoName(sample)
                    + " (" + sample + ")");
                    //if (--max < 0) break;
                }
                pw.append(System.lineSeparator());
            }
        }

        public void printEnglishComp(Appendable output) throws IOException {
            Set<String> countEqual = new TreeSet<>();
            String lastCC = null;
            output.append("Country\tMID\tSubdivision\tCLDR\tISO\tWikidata\tEqual\n");
            for (Entry<String, Set<String>> entry : sdset.REGION_CONTAINS.keyValuesSet()) {
                final String countryCode = entry.getKey();
                if (!countryCode.equals(lastCC)) {
                    if (lastCC != null && countEqual.size() != 0) {
                        output.append(ENGLISH_ICU.regionDisplayName(lastCC) + "\t\t\tEquals:\t" + countEqual.size() + "\t" + countEqual + "\n");
                    }
                    countEqual.clear();
                    ;
                    lastCC = countryCode;
                }
                for (String value : entry.getValue()) {
                    String cldrName = sdset.getBestName(value, false);
                    String wiki = WikiSubdivisionLanguages.getBestWikiEnglishName(value);
                    final String iso = sdset.getIsoName(value);
                    if (iso.equals(wiki)) {
                        countEqual.add(iso);
                        continue;
                    }
                    output.append(
                        ENGLISH_ICU.regionDisplayName(countryCode)
//                        + "\t" + WikiSubdivisionLanguages.WIKIDATA_TO_MID.get(value)
                        + "\t" + cldrName
                        + "\t" + value
                        + "\t" + iso
                        + "\t" + wiki
                        + "\n");
                }
            }
            if (countEqual.size() != 0) {
                output.append(ENGLISH_ICU.regionDisplayName(lastCC) + "\t\t\tEquals:\t" + countEqual.size() + "\t" + countEqual + "\n");
            }
        }

        public void printEnglishCompFull(Appendable output) throws IOException {
            output.append("Country\tMID\tSubdivision\tCLDR\tISO\tWikidata\n");
            for (Entry<String, Set<String>> entry : sdset.REGION_CONTAINS.keyValuesSet()) {
                final String countryCode = entry.getKey();
                for (String value : entry.getValue()) {
                    String cldrName = sdset.getBestName(value, false);
                    //getBestName(value);
                    String wiki = WikiSubdivisionLanguages.getBestWikiEnglishName(value);
                    final String iso = sdset.getIsoName(value);
                    output.append(
                        ENGLISH_ICU.regionDisplayName(countryCode)
//                        + "\t" + WikiSubdivisionLanguages.WIKIDATA_TO_MID.get(value)
                        + "\t" + value
                        + "\t" + cldrName
                        + "\t" + iso
                        + "\t" + wiki
                        + "\n");
                }
            }
        }

        public void printEnglish(PrintWriter output) throws IOException {
            TreeSet<String> allRegions = new TreeSet<>();
            allRegions.addAll(codeToData.keySet());
            allRegions.addAll(formerRegionToSubdivisions.keySet()); // override

            Factory cldrFactorySubdivisions = Factory.make(CLDRPaths.SUBDIVISIONS_DIRECTORY, ".*");
            CLDRFile oldFileSubdivisions = cldrFactorySubdivisions.make("en", false);
            CLDRFile fileSubdivisions = oldFileSubdivisions.cloneAsThawed();

            // <subdivisions>
            // <subdivisiontype="NZ-AUK">Auckland</territory>
//            output.append(
//                DtdType.ldml.header(MethodHandles.lookup().lookupClass())
//                + "\t<identity>\n"
//                + "\t\t<version number=\"$Revision" /*hack to stop SVN changing this*/ + "$\"/>\n"
//                + "\t\t<language type=\"en\"/>\n"
//                + "\t</identity>\n"
//                + "\t<localeDisplayNames>\n"
//                + "\t\t<subdivisions>\n");
            Set<String> skipped = new LinkedHashSet<>();

            for (String regionCode : allRegions) {
                if (regionCode.equals("FR")) {
                    int debug = 0;
                }
                if (!sdset.isKosher(regionCode)) {
                    if (regionCode.length() != 3) {
                        skipped.add(regionCode);
                    }
                    continue;
                }
                Set<String> remainder = formerRegionToSubdivisions.get(regionCode);
                remainder = remainder == null ? Collections.emptySet() : new LinkedHashSet<>(remainder);

                SubdivisionNode regionNode = sdset.ID_TO_NODE.get(regionCode);
//                output.append("\t\t<!-- ")
//                .append(convertToCldr(regionCode)).append(" : ")
//                .append(TransliteratorUtilities.toXML.transform(ENGLISH_ICU.regionDisplayName(regionCode)));
                if (regionNode == null) {
//                    output.append(" : NO SUBDIVISIONS -->\n");
                    continue;
                }
//                output.append(" -->\n");

                Set<SubdivisionNode> ordered = new LinkedHashSet<>();
                SubdivisionSet.addChildren(ordered, regionNode.children);

                for (SubdivisionNode node : ordered) {
                    final String sdCode = node.code;
                    String name = sdset.getBestName(sdCode, true);
                    String upper = UCharacter.toUpperCase(name);
                    String title = SubdivisionNode.TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(Locale.ROOT, null, name);
                    if (name.equals(upper) || !name.equals(title)) {
                        System.out.println("Suspicious name: " + name);
                    }

                    SubdivisionNode sd = sdset.ID_TO_NODE.get(sdCode);

//                    String level = sd.level == 1 ? "" : "\t<!-- in " + sd.parent.code
//                        + " : " + TransliteratorUtilities.toXML.transform(sdset.getBestName(sd.parent.code, true)) + " -->";
                    SubdivisionSet.appendName(fileSubdivisions, sdCode, name, null);
                    remainder.remove(sdCode);
                }
                for (String sdCode : remainder) {
                    String name = sdset.getBestName(sdCode, true);
                    if (!name.equals("???")) {
                        SubdivisionSet.appendName(fileSubdivisions, sdCode, name, "\t<!-- deprecated -->");
                    }
                }
            }
//            output.append(
//                "\t\t</subdivisions>\n"
//                    + "\t</localeDisplayNames>\n"
//                    + "</ldml>");
            System.out.println("Skipping: " + skipped);
//            if (!missing.isEmpty()) {
//                throw new IllegalArgumentException("No name for: " + missing.size() + ", " + missing);
//            }
            fileSubdivisions.write(output);
        }

        public void printMissingMIDs(PrintWriter pw) {
//          for (Entry<String, String> entry : WikiSubdivisionLanguages.WIKIDATA_TO_MID.entrySet()) {
//              String mid = entry.getValue();
//              if (!mid.isEmpty()) {
//                  continue;
//              }
//              String subCode = entry.getKey();
//              String wiki = clean(getWikiName(subCode));
//              String iso = clean(getIsoName(subCode));
//              String countryCode = subCode.substring(0, 2);
//              String cat = SUB_TO_CAT.get(subCode);
//              String catName = getIsoName(cat);
//              pw.append(
//                  ENGLISH_ICU.regionDisplayName(countryCode)
//                  + "\t" + mid
//                  + "\t" + subCode
//                  + "\t" + catName
//                  + "\t" + wiki
//                  + "\t" + iso
//                  + "\n"
//                  );
//          }
        }
    }
}