package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;

public class GenerateEnums {
    private static final String CODE_INDENT = "  ";

    private static final String DATA_INDENT = "    ";

    private static final String LIST_INDENT = "              ";

    private StandardCodes sc = StandardCodes.make();

    private Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");

//    private Factory supplementalFactory = Factory.make(
//        CLDRPaths.SUPPLEMENTAL_DIRECTORY, ".*");

    private Set<String> cldrCodes = new TreeSet<String>();

    // private Map enum_canonical = new TreeMap();
    private Map<String, String> enum_alpha3 = new TreeMap<String, String>();

    private Map<String, String> enum_UN = new TreeMap<String, String>();

    // private Map enum_FIPS10 = new TreeMap();

    // private Map enum_TLD = new TreeMap();

    private CLDRFile english = factory.make("en", false);

    private CLDRFile supplementalMetadata = factory.make("supplementalMetadata",
        false);

    private CLDRFile supplementalData = factory.make("supplementalData", false);

    private Relation<String, String> unlimitedCurrencyCodes;

    private Set<String> scripts = new TreeSet<String>();

    private Set<String> languages = new TreeSet<String>();

    public static void main(String[] args) throws IOException {
        GenerateEnums gen = new GenerateEnums();
        gen.showLanguageInfo();
        gen.loadCLDRData();
        gen.showCounts();
        gen.showCurrencies();
        gen.showLanguages();
        gen.showScripts();
        gen.showRegionCodeInfo();
        System.out.println("DONE");
    }

    private void showCounts() {
        System.out.format("Language Subtags: %s" + CldrUtility.LINE_SEPARATOR, sc.getGoodAvailableCodes(
            "language").size());
        System.out.format("Script Subtags: %s" + CldrUtility.LINE_SEPARATOR, sc.getGoodAvailableCodes(
            "script").size());
        System.out.format("Territory Subtags: %s" + CldrUtility.LINE_SEPARATOR, sc.getGoodAvailableCodes(
            "territory").size());
    }

    private void showCurrencies() throws IOException {
        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/currency_enum.txt");
        Log.println();
        Log.println("Currency Data");
        Log.println();
        showGeneratedCommentStart(CODE_INDENT);
        compareSets("currencies from sup.data", currencyCodes, "valid currencies",
            validCurrencyCodes);
        Set<String> unused = new TreeSet<String>(validCurrencyCodes);
        unused.removeAll(currencyCodes);
        showCurrencies(currencyCodes);
        Log.println();
        showCurrencies(unused);
        Map<String, String> sorted = new TreeMap<String, String>(Collator
            .getInstance(ULocale.ENGLISH));
        for (String code : validCurrencyCodes) {
            if (unused.contains(code) && !code.equals("CLF"))
                continue; // we include CLF for compatibility
            sorted.put(getName(code), code);
        }
        int lineLength = "  /** Belgian Franc */                                            BEF,"
            .length();
        for (String name : sorted.keySet()) {
            printRow(Log.getLog(), sorted.get(name), name, "currency", null,
                lineLength);
        }
        showGeneratedCommentEnd(CODE_INDENT);
        Log.close();
    }

    private String getName(String code) {
        String result = english.getName(CLDRFile.CURRENCY_NAME, code);
        if (result == null) {
            result = code;
            System.out.println("Failed to find: " + code);
        }
        return result;
    }

    private void showCurrencies(Set<String> both) {
        // /** Afghani */ AFN,
        for (Iterator<String> it = both.iterator(); it.hasNext();) {
            String code = it.next();
            String englishName = getName(code);
            if (englishName == null) {
            }
            Set<String> regions = unlimitedCurrencyCodes.getAll(code);
            System.out
                .println(code
                    + "\t"
                    + englishName
                    + "\t"
                    + (validCurrencyCodes.contains(code) ? currencyCodes
                        .contains(code) ? "" : "valid-only" : "supp-only")
                    + "\t"
                    + (regions != null ? regions : "unused"));
        }
    }

    private void showScripts() throws IOException {
        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/script_enum.txt");
        Log.println();
        Log.println("Script Data");
        Log.println();

        showGeneratedCommentStart(CODE_INDENT);
        Map<String, String> code_replacements = new TreeMap<String, String>();
        int len = "  /** Arabic */                                        Arab,"
            .length();
        for (Iterator<String> it = scripts.iterator(); it.hasNext();) {
            String code = it.next();
            String englishName = english.getName(CLDRFile.SCRIPT_NAME, code);
            if (englishName == null)
                continue;
            printRow(Log.getLog(), code, null, "script", code_replacements, len);
            // Log.println(" /**" + englishName + "*/ " + code + ",");
        }
        showGeneratedCommentEnd(CODE_INDENT);
        Log.close();
    }

    private void showLanguageInfo() throws IOException {
        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/language_info.txt");
        System.out.println();
        System.out.println("Language Converter");
        System.out.println();
        StringBuilder buffer = new StringBuilder();
        // language information
        for (String language : sc.getAvailableCodes("language")) {
            Scope scope = Iso639Data.getScope(language);
            if (scope == Scope.PrivateUse) {
                continue;
            }
            buffer.setLength(0);
            String alpha3 = Iso639Data.toAlpha3(language);
            if (alpha3 != null) {
                buffer.append(".add(\"" + alpha3 + "\")");
            }
            Type type = Iso639Data.getType(language);
            if (type != Type.Living) {
                buffer.append(".add(Type." + type + ")");
            }
            if (scope != Scope.Individual) {
                buffer.append(".add(Scope." + scope + ")");
            }
            if (buffer.length() > 0) {
                Log.println("\t\tto(\"" + language + "\")" + buffer + ";");
            }
        }
        Log.close();
    }

    private void showLanguages() throws IOException {
        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/language_enum.txt");
        System.out.println();
        System.out.println("Language Data");
        System.out.println();

        for (Iterator<String> it = languages.iterator(); it.hasNext();) {
            String code = it.next();
            String englishName = english.getName(CLDRFile.LANGUAGE_NAME, code);
            if (englishName == null)
                continue;
            System.out.println("     /**" + englishName + "*/    " + code + ",");
        }

        showGeneratedCommentStart(LIST_INDENT);
        /*
         * get the form: "anp frr frs gsw krl zxx aa ab ace ach ada ady ae af afa
         * afh" + " ain ak akk ale alg alt am an ang apa ar arc arn arp art arw" + "
         * as ast ath aus av awa ay az ba bad bai bal ban bas bat be"
         */
        StringBuffer buffer = new StringBuffer();
        int lineLimit = 70 - LIST_INDENT.length();
        char lastChar = 0;
        for (Iterator<String> it = languages.iterator(); it.hasNext();) {
            String code = it.next();
            if (code.equals("root")) {
                continue;
            }
            if (code.charAt(0) != lastChar
                || buffer.length() + 1 + code.length() > lineLimit) {
                if (buffer.length() != 0)
                    Log.println(LIST_INDENT + "+ \"" + buffer + "\"");
                buffer.setLength(0);
                lastChar = code.charAt(0);
            }
            buffer.append(code).append(' ');
        }
        // remove the very last space
        if (buffer.charAt(buffer.length() - 1) == ' ') {
            buffer.setLength(buffer.length() - 1);
        }
        Log.println(LIST_INDENT + "+ \"" + buffer + "\"");

        showGeneratedCommentEnd(LIST_INDENT);
        Log.close();
    }

    @SuppressWarnings("rawtypes")
    private Object join(Collection collection, String separator) {
        if (collection == null)
            return null;
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            if (first)
                first = false;
            else
                result.append(separator);
            result.append(it.next());
        }
        return result.toString();
    }

    static NumberFormat threeDigit = new DecimalFormat("000");

    public void loadCLDRData() throws IOException {
        // BufferedReader codes = Utility.getUTF8Data("territory_codes.txt");
        // while (true) {
        // String line = codes.readLine();
        // if (line == null)
        // break;
        // line = line.split("#")[0].trim();
        // if (line.length() == 0)
        // continue;
        // String[] sourceValues = line.split("\\s+");
        // String[] values = new String[5];
        // for (int i = 0; i < values.length; ++i) {
        // if (i >= sourceValues.length || sourceValues[i].equals("-"))
        // values[i] = null;
        // else
        // values[i] = sourceValues[i];
        // }
        // String alpha2 = values[0];
        // cldrCodes.add(alpha2);
        // if (isPrivateUseRegion(alpha2))
        // continue;
        // String numeric = values[1];
        // String alpha3 = values[2];
        // String internet = values[3];
        // if (internet != null)
        // internet = internet.toUpperCase();
        // String fips10 = values[4];
        // String enumValue = enumName(alpha2);
        // enum_alpha3.put(enumValue, alpha3);
        // enum_UN.put(enumValue, numeric);
        // enum_FIPS10.put(enumValue, fips10);
        // enum_TLD.put(enumValue, internet);
        // }
        // codes.close();
        DecimalFormat threeDigits = new DecimalFormat("000");
        for (String value : supplementalDataInfo.getNumericTerritoryMapping().keySet()) {
            cldrCodes.add(value);
            if (isPrivateUseRegion(value)) continue;
            enum_UN.put(value,
                threeDigits.format(supplementalDataInfo.getNumericTerritoryMapping().getAll(value).iterator().next()));
        }
        for (String value : supplementalDataInfo.getAlpha3TerritoryMapping().keySet()) {
            cldrCodes.add(value);
            if (isPrivateUseRegion(value)) continue;
            enum_alpha3.put(value, supplementalDataInfo.getAlpha3TerritoryMapping().getAll(value).iterator().next());
        }

        BufferedReader codes = CldrUtility.getUTF8Data("UnMacroRegions.txt");
        Map<String, String> macro_name = new TreeMap<String, String>();
        while (true) {
            String line = codes.readLine();
            if (line == null)
                break;
            line = line.trim();
            if (line.length() == 0)
                continue;
            if (line.charAt(0) < '0' || line.charAt(0) > '9') {
                System.out.println("GenerateEnums: Skipping: " + line);
                continue;
            }
            String[] sourceValues = line.split("\\s+");
            int code = Integer.parseInt(sourceValues[0]);
            String codeName = threeDigit.format(code);
            macro_name.put(codeName, line);
        }
        codes.close();
//        String values = supplementalDataInfo.getValidityInfo().get("$territory").get1().trim();
        Map<Status, Set<String>> validRegions = Validity.getInstance().getStatusToCodes(LstrType.region);
        Set<String> regions = new TreeSet<String>();
        regions.addAll(validRegions.get(Status.regular));
        regions.addAll(validRegions.get(Status.macroregion));
//        String[] validTerritories = values.split("\\s+");
//        for (int i = 0; i < validTerritories.length; ++i) {
        for (String region : regions) {
            if (corrigendum.contains(region)) {
                System.out.println("Skipping " + region + "\t\t"
                    + getEnglishName(region));
                continue; // exception, corrigendum
            }
            if (isPrivateUseRegion(region))
                continue;
            if (region.charAt(0) < 'A') {// numeric
                enum_UN.put(enumName(region), region);
                cldrCodes.add(region);
            } else {
                if (enum_alpha3.get(region) == null) {
                    System.out.println("Missing alpha3 for: " + region);
                }
            }
        }
        checkDuplicates(enum_UN);
        checkDuplicates(enum_alpha3);
        Set<String> availableCodes = new TreeSet<String>(sc.getAvailableCodes("territory"));
        compareSets("RFC 4646", availableCodes, "CLDR", cldrCodes);
        Set<String> missing = new TreeSet<String>(availableCodes);
        missing.removeAll(cldrCodes);
        // don't care list: "003"
        // missing.remove("003");
        // missing.remove("172");
        // Remove the following. They don't have numeric or alpha3 codes so they can't be found.
        missing.remove("EA");
        missing.remove("EZ");
        missing.remove("IC");
        missing.remove("QU");
        missing.remove("UN");

        if (missing.size() != 0) {
            throw new IllegalArgumentException("Codes in Registry but not in CLDR: "
                + missing);
        }

        Set<String> UNValues = new TreeSet<String>(enum_UN.values());

        for (Iterator<String> it = macro_name.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = macro_name.get(key);
            if (!UNValues.contains(key)) {
                System.out.println("Macro " + key + "\t" + value);
            }

        }

        for (Iterator<String> it = enum_UN.keySet().iterator(); it.hasNext();) {
            String region = it.next();
            String englishName = getEnglishName(region);
            if (englishName == null) {
                englishName = "NULL"; // for debugging\
            }
            String rfcName = getRFC3066Name(region);
            if (!englishName.equals(rfcName)) {
                System.out.println("Different names: {\"" + region + "\",\t\""
                    + englishName + " (" + rfcName + ")\"},");
            }
        }

        XPathParts parts = new XPathParts();
        getContainment();

        DateFormat[] simpleFormats = { new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy-MM"), new SimpleDateFormat("yyyy"), };
        Date today = new Date();
        Date longAgo = new Date(1000 - 1900, 1, 1);
        currencyCodes = new TreeSet<String>();
        unlimitedCurrencyCodes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class, null);
        for (Iterator<String> it = supplementalData
            .iterator("//supplementalData/currencyData/region"); it.hasNext();) {
            String path = it.next();
            parts.set(path);
            String region = parts.findAttributeValue("region", "iso3166");
            String code = parts.findAttributeValue("currency", "iso4217");
            String to = parts.findAttributeValue("currency", "to");
            main: if (to == null) {
                unlimitedCurrencyCodes.put(code, region);
            } else {
                for (int i = 0; i < simpleFormats.length; ++i) {
                    try {
                        Date foo = simpleFormats[i].parse(to);
                        if (foo.compareTo(longAgo) < 0) {
                            System.out.println("Date Error: can't parse " + to);
                            break main;
                        } else if (foo.compareTo(today) >= 0) {
                            unlimitedCurrencyCodes.put(code, region);
                        }
                        break main;
                    } catch (ParseException e) {
                    }
                }
                System.out.println("Date Error: can't parse " + to);
            }
            currencyCodes.add(code);
        }

        validCurrencyCodes = new TreeSet<String>();
        Set<String> bcp47CurrencyCodes = supplementalDataInfo.getBcp47Keys().getAll("cu");
        for (String code : bcp47CurrencyCodes) {
            validCurrencyCodes.add(code.toUpperCase());
        }

        scripts = supplementalDataInfo.getCLDRScriptCodes();
        languages = supplementalDataInfo.getCLDRLanguageCodes();

        // Set availableCodes = new TreeSet(sc.getAvailableCodes("territory"));
        // availableCodes.add("003");
        // for (Iterator it = availableCodes.iterator(); it.hasNext();) {
        // String code = (String) next())
        // canonicalRegion_UN.put(alpha2, numeric);
        // }

        // for (Iterator it = availableCodes.iterator(); it.hasNext();) {
        // String code = (String)it.next();
        // RegionCode region = map_id_canonical_RFC.get(code);
        // if (region != null) continue; // skip others
        // region = new RegionCode(code);
        // map_id_canonical_RFC.put(code,region);
        // map_canonical_id_RFC.put(region,code);
        // if ("A".compareTo(code) > 0) {
        // map_id_canonical_UN.put(code,region);
        // map_canonical_id_UN.put(region,code);
        // } else {
        // map_id_canonical_A2.put(code,region);
        // map_canonical_id_A2.put(region,code);
        // }
        // }
        // for (Iterator it = goodAvailableCodes.iterator(); it.hasNext();) {
        // String code = (String)it.next();
        // good.add(getInstance(code));
        // }
    }

    public void getContainment() {
        XPathParts parts = new XPathParts();
        // <group type="001" contains="002 009 019 142 150"/> <!--World -->
        for (Iterator<String> it = supplementalData
            .iterator("//supplementalData/territoryContainment/group"); it.hasNext();) {
            String path = it.next();
            String fullPath = supplementalData.getFullXPath(path);
            parts.set(fullPath);
            String container = parts.getAttributeValue(parts.size() - 1, "type");
            final String containedString = parts.getAttributeValue(-1, "contains");
            List<String> contained = Arrays.asList(containedString.trim().split("\\s+"));
            containment.put(container, contained);
        }
        // fix recursiveContainment.
        // for (String region : (Collection<String>)containment.keySet()) {
        // Set temp = new LinkedHashSet();
        // addContains(region, temp);
        // recursiveContainment.put(region, temp);
        // }
        Set<String> startingFromWorld = new TreeSet<String>();
        addContains("001", startingFromWorld);
        compareSets("World", startingFromWorld, "CLDR", cldrCodes);
        // generateContains();
    }

    private void generateContains() {

        for (String region : containment.keySet()) {
            List<String> plain = containment.get(region);
            // Collection recursive = (Collection)recursiveContainment.get(region);

            String setAsString = CldrUtility.join(plain, " ");
            // String setAsString2 = recursive.equals(plain) ? "" : ", " +
            // Utility.join(recursive," ");
            Log.println("\t\tadd(\"" + region + "\", \"" + setAsString + "\");");
        }
    }

    Map<String, List<String>> containment = new TreeMap<String, List<String>>();

    // Map recursiveContainment = new TreeMap();

    private void addContains(String string, Set<String> startingFromWorld) {
        startingFromWorld.add(string);
        List<String> contained = (List<String>) containment.get(string);
        if (contained == null)
            return;
        for (Iterator<String> it = contained.iterator(); it.hasNext();) {
            addContains(it.next(), startingFromWorld);
        }
    }

    @SuppressWarnings("rawtypes")
    private void compareSets(String name, Set availableCodes, String name2,
        Set cldrCodes) {
        Set temp = new TreeSet();
        temp.addAll(availableCodes);
        temp.removeAll(cldrCodes);
        System.out.println("In " + name + " but not in " + name2 + ": " + temp);
        temp.clear();
        temp.addAll(cldrCodes);
        temp.removeAll(availableCodes);
        System.out.println("Not in " + name + " but in " + name2 + ": " + temp);
    }

    @SuppressWarnings("rawtypes")
    private void checkDuplicates(Map m) {
        Map backMap = new HashMap();
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object o = m.get(key);
            Object otherKey = backMap.get(o);
            if (otherKey != null)
                System.out.println("Collision with: " + key + ",\t" + otherKey + ",\t"
                    + o);
            else
                backMap.put(o, key);
        }
    }

    Set<String> corrigendum = new TreeSet<String>(Arrays.asList(new String[] { "QE", "833",
        "830", "172" })); // 003, 419

    private Map extraNames = CollectionUtilities.asMap(new String[][] {
        { "BU", "Burma" }, { "TP", "East Timor" }, { "YU", "Yugoslavia" },
        { "ZR", "Zaire" }, { "CD", "Congo (Kinshasa, Democratic Republic)" },
        { "CI", "Ivory Coast (Cote d'Ivoire)" },
        { "FM", "Micronesia (Federated States)" },
        { "TL", "East Timor (Timor-Leste)" },
        // {"155","Western Europe"},

    });

    private Set<String> currencyCodes;

    private Set<String> validCurrencyCodes;

    static SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo
        .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);

    /**
     * Get the RegionCode Enum
     *
     * @throws IOException
     */
    private void showRegionCodeInfo() throws IOException {
        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/region_enum.txt");
        System.out.println();
        System.out.println("Data for RegionCode");
        System.out.println();
        showGeneratedCommentStart(CODE_INDENT);

        Set<String> reordered = new TreeSet<String>(new LengthFirstComparator());
        reordered.addAll(enum_UN.keySet());
        Map<String, String> code_replacements = new TreeMap<String, String>();
        int len = "  /** Polynesia */                                    UN061,"
            .length();
        for (Iterator<String> it = reordered.iterator(); it.hasNext();) {
            String region = it.next();
            printRow(Log.getLog(), region, null, "territory", code_replacements, len);
        }
        showGeneratedCommentEnd(CODE_INDENT);
        Log.close();

        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/region_info.txt");
        Log.println();
        Log.println("Data for ISO Region Codes");
        Log.println();
        for (String territory : supplementalDataInfo
            .getTerritoriesWithPopulationData()) {
            if (territory.equals("ZZ")) {
                continue;
            }
            PopulationData popData = supplementalDataInfo
                .getPopulationDataForTerritory(territory);
            // to("ak").add(Scope.Macrolanguage).add("aka");
            Log.formatln("    addRegion(RegionCode.%s, %s, %s, %s) // %s", territory,
                format(popData.getPopulation()), format(popData
                    .getLiteratePopulation()
                    / popData.getPopulation()),
                format(popData.getGdp()), english
                    .getName("territory", territory));
            // remove all the ISO 639-3 until they are part of BCP 47
            // we need to remove in earlier pass so we have the count
            Set<String> languages = new TreeSet<String>();
            for (String language : supplementalDataInfo
                .getLanguagesForTerritoryWithPopulationData(territory)) {
                if (Iso639Data.getSource(language) == Iso639Data.Source.ISO_639_3) {
                    continue;
                }
                popData = supplementalDataInfo.getLanguageAndTerritoryPopulationData(
                    language, territory);
                if (popData.getPopulation() == 0
                    || Double.isNaN(popData.getLiteratePopulation()
                        / popData.getPopulation())) {
                    continue;
                }
                languages.add(language);
            }
            int count = languages.size();
            for (String language : languages) {
                --count; // we need to know the last one
                popData = supplementalDataInfo.getLanguageAndTerritoryPopulationData(
                    language, territory);
                Log.formatln("    .addLanguage(\"%s\", %s, %s)%s // %s", language,
                    format(popData.getPopulation()), format(popData
                        .getLiteratePopulation()
                        / popData.getPopulation()),
                    (count == 0 ? ";" : ""), english
                        .getName(language));
            }
        }
        Log.close();

        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/enum/region_converters.txt");
        Log.println();
        Log.println("Data for ISO Region Codes");
        Log.println();
        showGeneratedCommentStart(DATA_INDENT);
        // addInfo(RegionCode.US, 840, "USA", "US", "US/XX", ....); ... are
        // containees
        reordered = new TreeSet<String>(new DeprecatedAndLengthFirstComparator("territory"));
        reordered.addAll(enum_UN.keySet());
        for (Iterator<String> it = reordered.iterator(); it.hasNext();) {
            String region = it.next();
            // String cldrName = region.length() < 5 ? region : region.substring(2); // fix
            // UN
            // name
            // int un = Integer.parseInt((String) enum_UN.get(region)); // get around
            // dumb octal
            // syntax
            String isoCode = (String) enum_alpha3.get(region);
            if (isoCode == null)
                continue;
            Log.println(DATA_INDENT + "add(" + quote(isoCode) + ", " + "RegionCode."
                + region + ");");
        }
        doAliases(code_replacements);
        showGeneratedCommentEnd(DATA_INDENT);
        Log.println();
        Log.println("Data for M.49 Region Codes");
        Log.println();
        showGeneratedCommentStart(DATA_INDENT);

        for (Iterator<String> it = reordered.iterator(); it.hasNext();) {
            String region = it.next();
            // String cldrName = region.length() < 5 ? region : region.substring(2); // fix
            // UN
            // name
            int un = Integer.parseInt((String) enum_UN.get(region), 10); // get
            // around
            // dumb
            // octal
            // syntax
            Log.println(DATA_INDENT + "add(" + un + ", " + "RegionCode." + region
                + ");");
        }
        doAliases(code_replacements);

        System.out.println("Plain list");
        for (Iterator<String> it = reordered.iterator(); it.hasNext();) {
            String region = it.next();
            // String cldrName = region.length() < 5 ? region : region.substring(2); // fix
            // UN
            // name
            String newCode = code_replacements.get(region);
            if (newCode != null)
                continue;

            int un = Integer.parseInt((String) enum_UN.get(region), 10); // get
            // around
            // dumb
            // octal
            // syntax
            System.out.println(un + "\t" + region + "\t"
                + english.getName("territory", region));
        }

        showGeneratedCommentEnd(DATA_INDENT);

        getContainment();
        Log.close();
    }

    static NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);

    static NumberFormat sf = NumberFormat.getScientificInstance(Locale.ENGLISH);
    static {
        nf.setMaximumFractionDigits(3);
        sf.setMaximumFractionDigits(3);
        nf.setGroupingUsed(false);
    }

    private String format(double value) {
        double newValue = CldrUtility.roundToDecimals(value, 3);
        String option1 = nf.format(newValue);
        String option2 = sf.format(value);
        return option1.length() <= option2.length() ? option1 : option2;
    }

    private void doAliases(Map<String, String> code_replacements) {
        for (String code : code_replacements.keySet()) {
            String newCode = code_replacements.get(code);
            if (newCode.length() == 0)
                newCode = "ZZ";
            Log.println(DATA_INDENT + "addAlias(" + "RegionCode." + code + ", \""
                + newCode + "\");");
        }
    }

    private void showGeneratedCommentEnd(String indent) {
        Log.println(indent + "/* End of generated code. */");
    }

    private void showGeneratedCommentStart(String indent) {
        Log.println(indent + "/*");
        Log.println(indent
            + " * The following information is generated from a tool,");
        Log.println(indent + " * as described on");
        Log.println(indent + " * http://wiki/Main/InternationalIdentifierUpdates.");
        Log.println(indent + " * Do not edit manually.");
        Log.println(indent + " * Start of generated code.");
        Log.println(indent + " */");
    }

    public final static class LengthFirstComparator implements Comparator<Object> {
        public int compare(Object a, Object b) {
            String as = a.toString();
            String bs = b.toString();
            if (as.length() < bs.length())
                return -1;
            if (as.length() > bs.length())
                return 1;
            return as.compareTo(bs);
        }
    }

    public final class DeprecatedAndLengthFirstComparator implements Comparator<Object> {
        String type;

        DeprecatedAndLengthFirstComparator(String type) {
            this.type = type;
        }

        public int compare(Object a, Object b) {
            String as = a.toString();
            String bs = b.toString();
            String ar = getDeprecatedReplacement(type, as);
            String br = getDeprecatedReplacement(type, bs);
            // put the deprecated ones first, eg those that aren't null
            if (ar != null) {
                if (br == null)
                    return -1;
            }
            if (br != null) {
                if (ar == null)
                    return 1;
            }
            // now check the length
            if (as.length() < bs.length())
                return -1;
            if (as.length() > bs.length())
                return 1;
            return as.compareTo(bs);
        }
    }

    /**
     * Returns null if not deprecated, otherwise "" if there is no replacement,
     * otherwise the replacement.
     *
     * @return
     */
    public String getDeprecatedReplacement(String type, String cldrTypeValue) {
        if (type.equals("currency")) {
            return null;
        }
        String path = supplementalMetadata.getFullXPath(
            "//supplementalData/metadata/alias/" + type + "Alias[@type=\""
                + cldrTypeValue + "\"]",
            true);
        if (path == null)
            return null;
        String replacement = new XPathParts().set(path).findAttributeValue(
            "territoryAlias", "replacement");
        if (replacement == null)
            return "";
        return replacement;
    }

    static Transliterator doFallbacks = Transliterator.createFromRules("id",
        "[’ʻ] > ''; ", Transliterator.FORWARD);

    private void printRow(PrintWriter out, String codeName, String englishName,
        String type, Map<String, String> code_replacements, int lineLength) {
        // int numeric = Integer.parseInt((String) enum_UN.get(codeName));
        // String alpha3 = (String) enum_alpha3.get(codeName);
        String cldrName = codeName.length() < 5 ? codeName : codeName.substring(2); // fix
        // UN
        // name
        String replacement = getDeprecatedReplacement(type, cldrName);

        String resolvedEnglishName = englishName != null ? englishName : type
            .equals("territory") ? getEnglishName(codeName) : type
                .equals("currency") ? getName(codeName) : english.getName(CLDRFile.SCRIPT_NAME, codeName);
        resolvedEnglishName = doFallbacks.transliterate(resolvedEnglishName);

        String prefix = CODE_INDENT + "/** " + resolvedEnglishName; // + " - " +
        // threeDigit.format(numeric);
        String printedCodeName = codeName;
        if (replacement != null) {
            code_replacements.put(codeName, replacement);
            out.println(prefix);
            prefix = CODE_INDENT + " * @deprecated"
                + (replacement.length() == 0 ? "" : " see " + replacement);
            printedCodeName = "@Deprecated " + printedCodeName;
        }
        prefix += " */";

        if (codeName.equals("UN001")) {
            out.println();
        }
        if (prefix.length() > lineLength - (printedCodeName.length() + 1)) {
            // break at last space
            int lastFit = prefix.lastIndexOf(' ', lineLength
                - (printedCodeName.length() + 1) - 2);
            out.println(prefix.substring(0, lastFit));
            prefix = CODE_INDENT + " *" + prefix.substring(lastFit);
        }
        out.print(prefix);
        out.print(Utility.repeat(" ", (lineLength
            - (prefix.length() + printedCodeName.length() + 1))));
        out.println(printedCodeName + ",");
    }

    private String getEnglishName(String codeName) {
        if (codeName.length() > 3)
            codeName = codeName.substring(2); // fix UN name
        String name = (String) extraNames.get(codeName);
        if (name != null)
            return name;
        name = english.getName(CLDRFile.TERRITORY_NAME, codeName);
        if (name != null)
            return name;
        return codeName;
    }

    private String getRFC3066Name(String codeName) {
        if (codeName.length() > 2)
            codeName = codeName.substring(2); // fix UN name
        List<String> list = sc.getFullData("territory", codeName);
        if (list == null)
            return null;
        return (String) list.get(0);
    }

    private String enumName(String codeName) {
        return codeName.charAt(0) < 'A' ? "UN" + codeName : codeName;
    }

    static String quote(Object input) {
        if (input != null)
            return '"' + input.toString().trim() + '"';
        return null;
    }

    static boolean isPrivateUseRegion(String codeName) {
        // AA, QM..QZ, XA..XZ, ZZ - CLDR codes
        if (codeName.equals("EU") || codeName.equals("QO") || codeName.equals("ZZ")) {
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
    /*
     * <reset before="tertiary">ウ</reset> <x><context>ウ</context><t>ヽ</t></x>
     * <x><context>ｳ</context><i>ヽ</i></x>
     *
     * <x><context>う</context><i>ゝ</i></x> <x><context>ゥ</context><i>ヽ</i></x>
     * <x><context>ｩ</context><i>ヽ</i></x> <x><context>ぅ</context><i>ゝ</i></x>
     * <x><context>ヴ</context><i>ヽ</i></x>
     *
     * <x><context>ゔ</context><i>ゝ</i></x> <x><context>ウ</context><i>ヾ</i><extend>゙</extend></x>
     * <x><context>ｳ</context><i>ヾ</i><extend>゙</extend></x> <x><context>う</context><i>ゞ</i><extend>゙</extend></x>
     *
     * <x><context>ゥ</context><i>ヾ</i><extend>゙</extend></x> <x><context>ｩ</context><i>ヾ</i><extend>゙</extend></x>
     * <x><context>ぅ</context><i>ゞ</i><extend>゙</extend></x> <x><context>ヴ</context><i>ヾ</i><extend>゙</extend></x>
     *
     * <x><context>ゔ</context><i>ゞ</i><extend>゙</extend></x>
     */
}