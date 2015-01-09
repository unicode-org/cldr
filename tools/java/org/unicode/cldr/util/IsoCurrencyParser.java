package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Utility;

public class IsoCurrencyParser {

    /**
     * Note: path is relative to CldrUtility, {@link CldrUtility#getInputStream(String)}
     */
    private static final String ISO_CURRENT_CODES_XML = "dl_iso_table_a1.xml";

    /*
     * IsoCurrencyParser doesn't currently use the historic codes list, but it could easily be modified/extended to do
     * so if we need to at some point. (JCE)
     * private static final String ISO_HISTORIC_CODES_XML = "dl_iso_tables_a3.xml";
     */

    /*
     * CLDR_EXTENSIONS_XML is stuff that would/should be in ISO, but that we KNOW for a fact to be correct.
     * Some subterritory designations that we use in CLDR, like Ascension Island or Tristan da Cunha aren't
     * used in ISO4217, so we use an extensions data file to allow our tests to validate the CLDR data properly.
     */
    private static final String CLDR_EXTENSIONS_XML = "dl_cldr_extensions.xml";

    /*
     * These corrections are country descriptions that are in the ISO4217 tables but carry a different spelling
     * in the language subtag registry.
     */
    private static final Map<String, String> COUNTRY_CORRECTIONS = CldrUtility.asMap(new String[][] {
        { Utility.unescape("R\u00C9UNION"), "RE" },
        { Utility.unescape("\u00C5LAND ISLANDS"), "AX" },
        { "BOLIVIA, PLURINATIONAL STATE OF", "BO" },
        { "CONGO, DEMOCRATIC REPUBLIC OF THE", "CD" },
        { Utility.unescape("C\u00D4TE D\u2019IVOIRE"), "CI" },
        { "CABO VERDE", "CV" },
        { Utility.unescape("CURA\u00C7AO"), "CW" },
        { "HEARD ISLAND AND McDONALD ISLANDS", "HM" },
        { Utility.unescape("INTERNATIONAL MONETARY FUND (IMF)\u00A0"), "ZZ" },
        { "IRAN, ISLAMIC REPUBLIC OF", "IR" },
        { "VIRGIN ISLANDS (BRITISH)", "VG" },
        { "VIRGIN ISLANDS (U.S.)", "VI" },
        { Utility.unescape("KOREA, DEMOCRATIC PEOPLE\u2019S REPUBLIC OF"), "KP" },
        { "KOREA, REPUBLIC OF", "KR" },
        { "MICRONESIA, FEDERATED STATES OF", "FM" },
        { "TANZANIA, UNITED REPUBLIC OF", "TZ" },
        { "Vatican City State (HOLY SEE)", "VA" },
        { Utility.unescape("LAO PEOPLE\u2019S DEMOCRATIC REPUBLIC"), "LA" },
        { "MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF", "MK" },
        { "MEMBER COUNTRIES OF THE AFRICAN DEVELOPMENT BANK GROUP", "ZZ" },
        { "MOLDOVA, REPUBLIC OF", "MD" },
        { "PALESTINE, STATE OF", "PS" },
        { "SISTEMA UNITARIO DE COMPENSACION REGIONAL DE PAGOS \"SUCRE\"", "ZZ" },
        { "VENEZUELA, BOLIVARIAN REPUBLIC OF", "VE" },
        { "EUROPEAN MONETARY CO-OPERATION FUND (EMCF)", "ZZ" },
        { "SAINT MARTIN", "MF" },
        { "SAINT BARTH\u00C9LEMY", "BL" },
    });

    static Map<String, String> iso4217CountryToCountryCode = new TreeMap<String, String>();
    static Set<String> exceptionList = new LinkedHashSet<String>();
    static {
        StandardCodes sc = StandardCodes.make();
        Set<String> countries = sc.getAvailableCodes("territory");
        for (String country : countries) {
            String name = sc.getData("territory", country);
            iso4217CountryToCountryCode.put(name.toUpperCase(Locale.ENGLISH), country);
        }
        iso4217CountryToCountryCode.putAll(COUNTRY_CORRECTIONS);
    }

    private Relation<String, Data> codeList = Relation.of(new TreeMap<String, Set<Data>>(), TreeSet.class, null);
    private Relation<String, String> countryToCodes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class, null);

    public static class Data implements Comparable<Object> {
        private String name;
        private String countryCode;
        private int numericCode;
        private int minor_unit;

        public Data(String countryCode, String name, int numericCode, int minor_unit) {
            this.countryCode = countryCode;
            this.name = name;
            this.numericCode = numericCode;
            this.minor_unit = minor_unit;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getName() {
            return name;
        }

        public int getNumericCode() {
            return numericCode;
        }

        public int getMinorUnit() {
            return minor_unit;
        }

        public String toString() {
            return String.format("[%s,\t%s [%s],\t%d]", name, countryCode,
                StandardCodes.make().getData("territory", countryCode), numericCode);
        }

        public int compareTo(Object o) {
            Data other = (Data) o;
            int result;
            if (0 != (result = countryCode.compareTo(other.countryCode))) return result;
            if (0 != (result = name.compareTo(other.name))) return result;
            return numericCode - other.numericCode;
        }
    }

    private static IsoCurrencyParser INSTANCE_WITHOUT_EXTENSIONS = new IsoCurrencyParser(false);
    private static IsoCurrencyParser INSTANCE_WITH_EXTENSIONS = new IsoCurrencyParser(true);

    public static IsoCurrencyParser getInstance(boolean useCLDRExtensions) {
        return useCLDRExtensions ? INSTANCE_WITH_EXTENSIONS : INSTANCE_WITHOUT_EXTENSIONS;
    }

    public static IsoCurrencyParser getInstance() {
        return getInstance(true);
    }

    public Relation<String, Data> getCodeList() {
        return codeList;
    }

    private IsoCurrencyParser(boolean useCLDRExtensions) {

        ISOCurrencyHandler isoCurrentHandler = new ISOCurrencyHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(isoCurrentHandler);
        xfr.readCLDRResource(ISO_CURRENT_CODES_XML, -1, false);
        if (useCLDRExtensions) {
            xfr.readCLDRResource(CLDR_EXTENSIONS_XML, -1, false);
        }
        if (exceptionList.size() != 0) {
            throw new IllegalArgumentException(exceptionList.toString());
        }
        codeList.freeze();
        countryToCodes.freeze();
    }

    /*
     * private Relation<String,Data> codeList = new Relation(new TreeMap(), TreeSet.class, null);
     * private String version;
     */

    public Relation<String, String> getCountryToCodes() {
        return countryToCodes;
    }

    public static String getCountryCode(String iso4217Country) {
        iso4217Country = iso4217Country.trim();
        if (iso4217Country.startsWith("\"")) {
            iso4217Country = iso4217Country.substring(1, iso4217Country.length() - 1);
        }
        String name = iso4217CountryToCountryCode.get(iso4217Country);
        if (name != null) return name;
        if (iso4217Country.startsWith("ZZ")) {
            return "ZZ";
        }
        exceptionList.add(String.format("\t\t{\"%s\", \"XXX\"}, // fix XXX and add to COUNTRY_CORRECTIONS in "
            + StackTracker.currentElement(0).getFileName() + CldrUtility.LINE_SEPARATOR, iso4217Country));
        return "ZZ";
    }

    public class ISOCurrencyHandler extends XMLFileReader.SimpleHandler {

        // This Set represents the entries in ISO4217 which we know to be bad. I have sent e-mail
        // to the ISO 4217 Maintenance agency attempting to get them removed. Once that happens,
        // we can remove these as well.
        // SVC - El Salvador Colon - not used anymore ( uses USD instead )
        // ZWL - Last Zimbabwe Dollar - abandoned due to hyper-inflation.
        Set<String> KNOWN_BAD_ISO_DATA_CODES = new TreeSet<String>(Arrays.asList("SVC", "ZWL"));
        XPathParts parts = new XPathParts();
        String country_code;
        String currency_name;
        String alphabetic_code;
        int numeric_code;
        int minor_unit;

        /**
         * Finish processing anything left hanging in the file.
         */
        public void cleanup() {
        }

        public void handlePathValue(String path, String value) {
            try {
                parts.set(path);
                String type = parts.getElement(-1);
                if (type.equals("CtryNm")) {
                    value = value.replaceAll("\n", "");
                    country_code = getCountryCode(value);
                    if (country_code == null) {
                        country_code = "ZZ";
                    }
                    alphabetic_code = "XXX";
                    numeric_code = -1;
                    minor_unit = 0;
                } else if (type.equals("CcyNm")) {
                    currency_name = value;
                } else if (type.equals("Ccy")) {
                    alphabetic_code = value;
                } else if (type.equals("CcyNbr")) {
                    try {
                        numeric_code = Integer.valueOf(value);
                    } catch (NumberFormatException ex) {
                        numeric_code = -1;
                    }
                } else if (type.equals("CcyMnrUnts")) {
                    try {
                        minor_unit = Integer.valueOf(value);
                    } catch (NumberFormatException ex) {
                        minor_unit = 2;
                    }
                }

                if (type.equals("CcyMnrUnts") && alphabetic_code.length() > 0
                    && !KNOWN_BAD_ISO_DATA_CODES.contains(alphabetic_code)) {
                    Data data = new Data(country_code, currency_name, numeric_code, minor_unit);
                    codeList.put(alphabetic_code, data);
                    countryToCodes.put(data.getCountryCode(), alphabetic_code);
                }

            } catch (Exception e) {
                throw (IllegalArgumentException) new IllegalArgumentException("path: "
                    + path + ",\tvalue: " + value).initCause(e);
            }
        }
    }
}
