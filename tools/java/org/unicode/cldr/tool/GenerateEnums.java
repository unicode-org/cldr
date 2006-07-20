package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import com.sun.org.apache.xerces.internal.impl.xpath.XPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GenerateEnums {
    private StandardCodes sc = StandardCodes.make();
    private Factory factory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    private Factory supplementalFactory = Factory.make(Utility.SUPPLEMENTAL_DIRECTORY, ".*");

    private Set cldrCodes = new TreeSet();
    //private Map enum_canonical = new TreeMap();
    private Map enum_alpha3 = new TreeMap();
    private Map enum_UN = new TreeMap();
    private Map enum_FIPS10 = new TreeMap();
    private Map enum_TLD = new TreeMap();
    
    private CLDRFile english = factory.make("en",false);
    private CLDRFile supplementalMetadata = factory.make("supplementalMetadata", false);
    private CLDRFile supplementalData = factory.make("supplementalData", false);
    
    public static void main(String[] args) throws IOException {
        GenerateEnums gen = new GenerateEnums();
        gen.loadRegionData();
        gen.printInfo();
        gen.printAdditionalInfo();
    }
    
    private void printAdditionalInfo() {
        System.out.println();
        System.out.println("Data for RegionUtilties");
        System.out.println();
        //     addInfo(RegionCode.US, 840, "USA", "US", "US/XX", ....); ...  are containees
        Set reordered = new TreeSet(new LengthFirstComparator());
        reordered.addAll(enum_UN.keySet());
        for (Iterator it = reordered.iterator(); it.hasNext();) {
            String region = (String)it.next();
            String cldrName = region.length()<5 ? region : region.substring(2); // fix UN name
            int un = Integer.parseInt((String) enum_UN.get(region)); // get around dumb octal syntax
            System.out.println("  addInfo(RegionCode." + region
                    + ", " + un
                    + ", " + quote(enum_alpha3.get(region))
                    + ", " + quote(enum_FIPS10.get(region))
                    + ", " + quote(enum_TLD.get(region))
                    + ", " + quote(join((List)containment.get(cldrName), "/"))
                    + ");"
                    );
        }
    }
    
    private Object join(Collection collection, String separator) {
        if (collection == null) return null;
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            if (first) first = false;
            else result.append(separator);
            result.append(it.next());
        }
        return result.toString();
    }

    static NumberFormat threeDigit = new DecimalFormat("000");
    
    public  void loadRegionData() throws IOException {
        BufferedReader codes = Utility.getUTF8Data("territory_codes.txt");
        while (true) {
            String line = codes.readLine();
            if (line == null) break;
            line = line.split("#")[0].trim();
            if (line.length() == 0) continue;
            String[] sourceValues = line.split("\\s+");
            String[] values = new String[5];
            for (int i = 0; i < values.length; ++i) {
                if (i >= sourceValues.length || sourceValues[i].equals("-")) values[i] = null;
                else values[i] = sourceValues[i];
            }
            String alpha2 = values[0];
            cldrCodes.add(alpha2);
            if (isPrivateUseRegion(alpha2)) continue;
            String numeric = values[1];
            String alpha3 = values[2];
            String internet = values[3];
            if (internet != null) internet = internet.toUpperCase();
            String fips10 = values[4];
            String enumValue = enumName(alpha2);
            enum_alpha3.put(enumValue, alpha3);
            enum_UN.put(enumValue, numeric);
            enum_FIPS10.put(enumValue, fips10);
            enum_TLD.put(enumValue, internet);
        }
        codes.close();
        
        codes = Utility.getUTF8Data("UnMacroRegions.txt");
        Map macro_name = new TreeMap();
        while (true) {
            String line = codes.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.charAt(0) < '0' || line.charAt(0) > '9') {
                System.out.println("Skipping: " + line);
                continue;
            }
            String[] sourceValues = line.split("\\s+");
            int code = Integer.parseInt(sourceValues[0]);
            String codeName = threeDigit.format(code);
            macro_name.put(codeName, line);
        }
        codes.close();

        String values = supplementalMetadata.getStringValue("//supplementalData/metadata/validity/variable[@id=\"$territory\"]", true).trim();
        String[] validTerritories = values.split("\\s+");
        for (int i = 0; i < validTerritories.length; ++i) {
            if (corrigendum.contains(validTerritories[i])) {
                System.out.println("Skipping " + validTerritories[i] + "\t\t" + getEnglishName(validTerritories[i]));
                continue; // exception, corrigendum
            }
            if (isPrivateUseRegion(validTerritories[i])) continue;
            if (validTerritories[i].charAt(0) < 'A') {// numeric
                enum_UN.put(enumName(validTerritories[i]), validTerritories[i]);
                cldrCodes.add(validTerritories[i]);
            } else {
                if (enum_alpha3.get(validTerritories[i]) == null) {
                    System.out.println("Missing alpha3 for: " + validTerritories[i]);
                }
            }
        }
        checkDuplicates(enum_UN);
        checkDuplicates(enum_alpha3);
        Set availableCodes = new TreeSet(sc.getAvailableCodes("territory"));
        compareSets("RFC", availableCodes, "CLDR", cldrCodes);
        
        Set UNValues = new TreeSet(enum_UN.values());

        for (Iterator it = macro_name.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = macro_name.get(key);
            if (!UNValues.contains(key)) {
                System.out.println("Macro " + key + "\t" + value);
            }
           
        }
        
        for (Iterator it = enum_UN.keySet().iterator(); it.hasNext();) {
            String region = (String)it.next();
            String englishName = getEnglishName(region);
            String rfcName = getRFC3066Name(region);
            if (!englishName.equals(rfcName)) {
                System.out.println("Different names: {\"" + region + "\",\t\"" + englishName + " (" + rfcName + ")\"},");
            }
        }
        
        XPathParts parts = new XPathParts();
        //<group type="001" contains="002 009 019 142 150"/> <!--World -->
        for (Iterator it = supplementalData.iterator("//supplementalData/territoryContainment/group"); it.hasNext();) {
            String path = (String)it.next();
            parts.set(path);
            String container = parts.getAttributeValue(parts.size()-1, "type");
            List contained = Arrays.asList(parts.getAttributeValue(parts.size()-1, "contains").trim().split("\\s+"));
            containment.put(container, contained);
        }
        Set startingFromWorld = new TreeSet();
        addContains("001", startingFromWorld);
        compareSets("World", startingFromWorld, "CLDR", cldrCodes);

        
//            Set availableCodes = new TreeSet(sc.getAvailableCodes("territory"));
//            availableCodes.add("003");
//            for (Iterator it = availableCodes.iterator(); it.hasNext();) {
//                String code = (String) next())
//                canonicalRegion_UN.put(alpha2, numeric);
//            }
            
//            for (Iterator it = availableCodes.iterator(); it.hasNext();) {
//                String code = (String)it.next();
//                RegionCode region = map_id_canonical_RFC.get(code);
//                if (region != null) continue; // skip others
//                region = new RegionCode(code);
//                map_id_canonical_RFC.put(code,region);
//                map_canonical_id_RFC.put(region,code);
//                if ("A".compareTo(code) > 0) {
//                    map_id_canonical_UN.put(code,region);
//                    map_canonical_id_UN.put(region,code);
//                } else {
//                    map_id_canonical_A2.put(code,region);
//                    map_canonical_id_A2.put(region,code);
//                }
//            }
//            for (Iterator it = goodAvailableCodes.iterator(); it.hasNext();) {
//                String code = (String)it.next();
//                good.add(getInstance(code));
//            }
        }

    private void compareSets(String name, Set availableCodes, String name2, Set cldrCodes) {
        Set temp = new TreeSet();
        temp.addAll(availableCodes);
        temp.removeAll(cldrCodes);
        System.out.println("In " + name + " but not in " + name2 + ": " + temp);
        temp.clear();
        temp.addAll(cldrCodes);
        temp.removeAll(availableCodes);
        System.out.println("Not in " + name + " but in " + name2 + ": " + temp);
    }
    Map containment = new TreeMap();

    private void addContains(String string, Set startingFromWorld) {
        startingFromWorld.add(string);
        List contained = (List) containment.get(string);
        if (contained == null) return;
        for (Iterator it = contained.iterator(); it.hasNext();) {
            addContains((String)it.next(), startingFromWorld);
        }
    }

    private void checkDuplicates(Map m) {
        Map backMap = new HashMap();
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object o = m.get(key);
            Object otherKey = backMap.get(o);
            if (otherKey != null ) System.out.println("Collision with: " +key + ",\t" + otherKey + ",\t" + o);
            else backMap.put(o, key);
        }
    }

    Set corrigendum = new TreeSet(Arrays.asList(new String[]{"QE", "833", "830", "172"})); // 003, 419
    
    private Map extraNames = CollectionUtilities.asMap(new String[][]{
            {"BU","Burma"},
            {"TP","East Timor"},
            {"YU","Yugoslavia"},
            {"ZR","Zaire"},
            {"CD","Congo (Kinshasa, Democratic Republic)"},
            {"CI",  "Ivory Coast (Cote d'Ivoire)"},
            {"FM",  "Micronesia (Federated States)"},
            {"TL",  "East Timor (Timor-Leste)"},
            //{"155","Western Europe"},
            
            });
        
    /**
     * does stuff
     * @deprecated see X
     */
        private void printInfo() {
            System.out.println();
            System.out.println("Data for RegionCode");
            System.out.println();
            Set reordered = new TreeSet(new LengthFirstComparator());
            reordered.addAll(enum_UN.keySet());
            for (Iterator it = reordered.iterator(); it.hasNext();) {
                String region = (String)it.next();
                printRow(region);
            }
         }

        public final static class LengthFirstComparator implements Comparator {
            public int compare(Object a, Object b) {
                String as = a.toString();
                String bs = b.toString();
                if (as.length() < bs.length()) return -1;
                if (as.length() > bs.length()) return 1;
                return as.compareTo(bs);
            }
        }
        
        private  void printRow(String codeName) {
            int numeric = Integer.parseInt((String)enum_UN.get(codeName));
            String alpha3 = (String)enum_alpha3.get(codeName);
            String deprecated = "";
            String cldrName = codeName.length()<5 ? codeName : codeName.substring(2); // fix UN name
            String path = supplementalMetadata.getFullXPath("//supplementalData/metadata/alias/territoryAlias[@type=\"" + cldrName + "\"]", true);
            String prefix = "  /** " +  getEnglishName(codeName); //  + " - " + threeDigit.format(numeric);
            String printedCodeName = codeName;
            if (path != null) {
                String replacement = new XPathParts().set(path).findAttributeValue("territoryAlias", "replacement");
                System.out.println(prefix);
                prefix = "   * @deprecated" + (replacement == null ? "" : " see " + replacement);
                printedCodeName = "@Deprecated " + printedCodeName;
            }
            prefix += " */";
           
            if (codeName.equals("UN001")) {
                System.out.println();
            }
            System.out.print(prefix);
            System.out.print("                                                           "
                    .substring(prefix.length() + printedCodeName.length()));
            System.out.println(printedCodeName 
//                    + "\t(" + numeric + 
//                    (alpha3 != null ? ", " + quote(alpha3) : "")
//                    + ")"
                    + ",");
        }

        private String getEnglishName(String codeName) {
            if (codeName.length()>3) codeName = codeName.substring(2); // fix UN name
            String name = (String)extraNames.get(codeName);
            if (name != null) return name;
            name = english.getName(CLDRFile.TERRITORY_NAME, codeName, false);
            return name;
        }
        
        private String getRFC3066Name(String codeName) {
            if (codeName.length()>2) codeName = codeName.substring(2); // fix UN name
            List list = sc.getFullData("territory", codeName);
            if (list == null) return null;
            return (String) list.get(0);
        }

        
        private String enumName(String codeName) {
            return codeName.charAt(0) < 'A' ? "UN" + codeName : codeName;
        }

        static String quote(Object input) {
            if (input != null) return '"' + input.toString().trim() + '"';
            return null;
        }
        
        static boolean isPrivateUseRegion(String codeName) {
//           AA, QM..QZ, XA..XZ, ZZ - CLDR codes
        if (codeName.equals("QU") || codeName.equals("QO") || codeName.equals("ZZ")) {
            return false;
        } else if (codeName.equals("AA") || codeName.equals("ZZ")) {
            return true;
        } else if (codeName.compareTo("QM") >= 0 && codeName.compareTo("QZ") <= 0) {
            return true;
        } else if (codeName.compareTo("XA") >= 0 && codeName.compareTo("XZ") <= 0) {
            return true;
        }
        return false;
    }
        
    }