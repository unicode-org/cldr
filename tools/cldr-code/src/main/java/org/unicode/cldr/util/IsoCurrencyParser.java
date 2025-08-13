package org.unicode.cldr.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

public class IsoCurrencyParser {

    private static final Set<String> KNOWN_BAD_ISO_DATA_CODES = ImmutableSet.of("SVC");

    /** Note: path is relative to CldrUtility, {@link CldrUtility#getInputStream(String)} */
    private static final String ISO_CURRENT_CODES_XML = "dl_iso_table_a1.xml";

    /** Note: path is relative to CldrUtility, {@link CldrUtility#getInputStream(String)} */
    private static final String ISO_HISTORIC_CODES_XML = "dl_iso_table_a3.xml";

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
    private static final ImmutableMap<String, String> COUNTRY_CORRECTIONS =
            new ImmutableMap.Builder<String, String>()
                    .put("UNITED ARAB EMIRATES (THE)", "AE")
                    .put(Utility.unescape("\u00C5LAND ISLANDS"), "AX")
                    .put("SAINT BARTH\u00C9LEMY", "BL")
                    .put("BOLIVIA (PLURINATIONAL STATE OF)", "BO")
                    .put("BAHAMAS (THE)", "BS")
                    .put("COCOS (KEELING) ISLANDS (THE)", "CC")
                    .put("CONGO (THE DEMOCRATIC REPUBLIC OF THE)", "CD")
                    .put("CENTRAL AFRICAN REPUBLIC (THE)", "CF")
                    .put("CONGO (THE)", "CG")
                    .put(Utility.unescape("C\u00D4TE D\u2019IVOIRE"), "CI")
                    .put("COOK ISLANDS (THE)", "CK")
                    .put("CABO VERDE", "CV")
                    .put(Utility.unescape("CURA\u00C7AO"), "CW")
                    .put("CZECHIA", "CZ")
                    .put("DOMINICAN REPUBLIC (THE)", "DO")
                    .put("FALKLAND ISLANDS (THE) [MALVINAS]", "FK")
                    .put("MICRONESIA (FEDERATED STATES OF)", "FM")
                    .put("FAROE ISLANDS (THE)", "FO")
                    .put("UNITED KINGDOM OF GREAT BRITAIN AND NORTHERN IRELAND (THE)", "GB")
                    .put("GAMBIA (THE)", "GM")
                    .put("HEARD ISLAND AND McDONALD ISLANDS", "HM")
                    .put("BRITISH INDIAN OCEAN TERRITORY (THE)", "IO")
                    .put("IRAN (ISLAMIC REPUBLIC OF)", "IR")
                    .put("COMOROS (THE)", "KM")
                    .put(Utility.unescape("KOREA (THE DEMOCRATIC PEOPLE\u2019S REPUBLIC OF)"), "KP")
                    .put("KOREA (THE REPUBLIC OF)", "KR")
                    .put("CAYMAN ISLANDS (THE)", "KY")
                    .put(Utility.unescape("LAO PEOPLE\u2019S DEMOCRATIC REPUBLIC (THE)"), "LA")
                    .put("MOLDOVA (THE REPUBLIC OF)", "MD")
                    .put("SAINT MARTIN", "MF")
                    .put("MARSHALL ISLANDS (THE)", "MH")
                    .put("MACEDONIA (THE FORMER YUGOSLAV REPUBLIC OF)", "MK")
                    .put("NORTHERN MARIANA ISLANDS (THE)", "MP")
                    .put("NETHERLANDS (THE)", "NL")
                    .put("NIGER (THE)", "NE")
                    .put("PHILIPPINES (THE)", "PH")
                    .put("PALESTINE, STATE OF", "PS")
                    .put(Utility.unescape("R\u00C9UNION"), "RE")
                    .put("RUSSIAN FEDERATION (THE)", "RU")
                    .put("SUDAN (THE)", "SD")
                    .put("ESWATINI", "SZ")
                    .put("SWAZILAND", "SZ")
                    .put("TURKS AND CAICOS ISLANDS (THE)", "TC")
                    .put("FRENCH SOUTHERN TERRITORIES (THE)", "TF")
                    .put("TAIWAN (PROVINCE OF CHINA)", "TW")
                    .put("TANZANIA, UNITED REPUBLIC OF", "TZ")
                    .put("UNITED STATES MINOR OUTLYING ISLANDS (THE)", "UM")
                    .put("UNITED STATES OF AMERICA (THE)", "US")
                    .put("HOLY SEE (THE)", "VA")
                    .put("VENEZUELA (BOLIVARIAN REPUBLIC OF)", "VE")
                    .put("VIRGIN ISLANDS (BRITISH)", "VG")
                    .put("VIRGIN ISLANDS (U.S.)", "VI")
                    .put(Utility.unescape("INTERNATIONAL MONETARY FUND (IMF)"), "ZZ")
                    .put("MEMBER COUNTRIES OF THE AFRICAN DEVELOPMENT BANK GROUP", "ZZ")
                    .put("SISTEMA UNITARIO DE COMPENSACION REGIONAL DE PAGOS \"SUCRE\"", "ZZ")
                    .put("EUROPEAN MONETARY CO-OPERATION FUND (EMCF)", "ZZ")
                    .put("ARAB MONETARY FUND", "ZZ")
                    .put("TÜRKİYE", "TR")
                    .put("TURKEY", "TR")
                    .put("BURMA", "MY")
                    .put("CZECHOSLOVAKIA", "CS")
                    .put("VIETNAM", "VN")
                    .put("YEMEN, DEMOCRATIC", "YE")
                    .put("SOUTHERN RHODESIA", "ZW")
                    .put("LAO", "LA")
                    .put("FRENCH  GUIANA", "GF")
                    .put("SAINT-BARTHÉLEMY", "BL")
                    .put("MOLDOVA, REPUBLIC OF", "MD")
                    .build();

    static Map<String, String> iso4217CountryToCountryCode = new TreeMap<>();
    static Set<String> exceptionList = new LinkedHashSet<>();

    static {
        StandardCodes sc = StandardCodes.make();
        Set<String> countries = sc.getAvailableCodes("territory");
        for (String country : countries) {
            String name = sc.getData("territory", country);
            iso4217CountryToCountryCode.put(name.toUpperCase(Locale.ENGLISH), country);
        }
        iso4217CountryToCountryCode.putAll(COUNTRY_CORRECTIONS);
    }

    private Relation<String, Data> codeList =
            Relation.of(new TreeMap<String, Set<Data>>(), TreeSet.class, null);
    private Relation<String, String> countryToCodes =
            Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class, null);
    private Relation<String, Data> historicCodeList =
            Relation.of(new TreeMap<String, Set<Data>>(), TreeSet.class, null);
    private Relation<String, String> historicCountryToCodes =
            Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class, null);

    public static class Data implements Comparable<Object> {
        private String name;
        private String countryCode;
        private int numericCode;
        private int minor_unit;
        private String withdrawal_date;

        /** for modern currency */
        public Data(String countryCode, String name, int numericCode, int minor_unit) {
            this.countryCode = countryCode;
            this.name = name;
            this.numericCode = numericCode;
            this.minor_unit = minor_unit;
        }

        /** for historic currency */
        public Data(String countryCode, String name, int numericCode, String withdrawal_date) {
            this.countryCode = countryCode;
            this.name = name;
            this.numericCode = numericCode;
            this.withdrawal_date = withdrawal_date;
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

        public Date getWithdrawalDate() {
            if (withdrawal_date == null) return null;
            String split[] = withdrawal_date.split("-");
            if (split.length != 2) {
                throw new IllegalArgumentException(
                        "Trouble parsing withdrawal date " + withdrawal_date);
            }
            final GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("Etc/GMT"));
            c.clear();
            c.set(Calendar.ERA, GregorianCalendar.AD);
            c.set(Integer.parseInt(split[0]), Integer.parseInt(split[1]), 1);
            return c.getTime();
        }

        @Override
        public String toString() {
            if (withdrawal_date == null) {
                return String.format(
                        "[%s,\t%s [%s],\t%d]",
                        name,
                        countryCode,
                        StandardCodes.make().getData("territory", countryCode),
                        numericCode);
            } else {
                return String.format(
                        "[%s,\t%s [%s],\t%d\t withdrawal=%s]",
                        name,
                        countryCode,
                        StandardCodes.make().getData("territory", countryCode),
                        numericCode,
                        withdrawal_date);
            }
        }

        @Override
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
        xfr.readCLDRResource(ISO_HISTORIC_CODES_XML, -1, false);
        if (useCLDRExtensions) {
            xfr.readCLDRResource(CLDR_EXTENSIONS_XML, -1, false);
        }
        if (exceptionList.size() != 0) {
            throw new IllegalArgumentException(exceptionList.toString());
        }
        codeList.freeze();
        countryToCodes.freeze();
        historicCodeList.freeze();
        historicCountryToCodes.freeze();
    }

    /*
     * private Relation<String,Data> codeList = new Relation(new TreeMap(), TreeSet.class, null);
     * private String version;
     */

    public Relation<String, String> getCountryToCodes() {
        return countryToCodes;
    }

    public Relation<String, String> getHistoricCountryToCodes() {
        return historicCountryToCodes;
    }

    public Relation<String, Data> getHistoricCodeList() {
        return historicCodeList;
    }

    public static String getCountryCode(String iso4217Country) {
        iso4217Country = iso4217Country.replaceAll("\u00A0", ""); // remove NBSP
        iso4217Country = iso4217Country.trim();
        if (iso4217Country.startsWith("\"")) {
            iso4217Country = iso4217Country.substring(1, iso4217Country.length() - 1);
        }
        iso4217Country = iso4217Country.trim();
        String name = iso4217CountryToCountryCode.get(iso4217Country);
        if (name != null) return name;
        if (iso4217Country.startsWith("ZZ")) {
            return "ZZ";
        }
        exceptionList.add(
                String.format(
                        CldrUtility.LINE_SEPARATOR
                                + "\t\t.put(\"%s\", \"XXX\") // fix XXX and add to COUNTRY_CORRECTIONS in "
                                + StackTracker.currentElement(0).getFileName(),
                        iso4217Country));
        return "ZZ";
    }

    public class ISOCurrencyHandler extends XMLFileReader.SimpleHandler {

        // This Set represents the entries in ISO4217 which we know to be bad. I have sent e-mail
        // to the ISO 4217 Maintenance agency attempting to get them removed. Once that happens,
        // we can remove these as well.
        // SVC - El Salvador Colon - not used anymore ( uses USD instead )
        String country_code;
        String currency_name;
        String alphabetic_code;
        String withdrawal_date; // for historic
        int numeric_code;
        int minor_unit;

        /** Finish processing anything left hanging in the file. */
        public void cleanup() {}

        @Override
        public void handlePathValue(String path, String value) {
            try {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String type = parts.getElement(-1);
                if (type.equals("CtryNm")) {
                    // first item - reset all
                    value = value.replaceAll("\n", "");
                    country_code = getCountryCode(value);
                    if (country_code == null) {
                        country_code = "ZZ";
                    }
                    alphabetic_code = "XXX";
                    numeric_code = -1;
                    minor_unit = 0;
                    withdrawal_date = null;
                } else if (type.equals("CcyNm")) {
                    currency_name = value;
                } else if (type.equals("Ccy")) {
                    alphabetic_code = value;
                } else if (type.equals("CcyNbr")) {
                    try {
                        numeric_code = Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        numeric_code = -1;
                    }
                } else if (type.equals("CcyMnrUnts")) {
                    try {
                        minor_unit = Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        minor_unit = 2;
                    }
                } else if (type.equals("WthdrwlDt")) {
                    withdrawal_date = value;
                }

                // final values for _a1 and _a3 respectively
                if (type.equals("CcyMnrUnts")
                        && alphabetic_code.length() > 0
                        && !KNOWN_BAD_ISO_DATA_CODES.contains(alphabetic_code)) {
                    addCurrentData();
                } else if (type.equals("WthdrwlDt")) {
                    addHistoricData();
                }

            } catch (Exception e) {
                throw (IllegalArgumentException)
                        new IllegalArgumentException("path: " + path + ",\tvalue: " + value)
                                .initCause(e);
            }
        }

        private void addHistoricData() {
            Data data = new Data(country_code, currency_name, numeric_code, withdrawal_date);
            addHistoricData(data);
        }

        private void addCurrentData() {
            Data data = new Data(country_code, currency_name, numeric_code, minor_unit);
            addCurrentData(data);
        }

        private void addCurrentData(Data data) {
            codeList.put(alphabetic_code, data);
            countryToCodes.put(data.getCountryCode(), alphabetic_code);
        }

        private void addHistoricData(Data data) {
            historicCodeList.put(alphabetic_code, data);
            historicCountryToCodes.put(data.getCountryCode(), alphabetic_code);
        }
    }
}
