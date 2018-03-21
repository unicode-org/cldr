package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class IsoRegionData {
    static Map<String, String> _numeric = new HashMap<String, String>();
    static Map<String, String> _alpha3 = new HashMap<String, String>();
    static Map<String, String> _fips10 = new HashMap<String, String>();
    static Map<String, String> _internet = new HashMap<String, String>();
    static Set<String> other_internet = new TreeSet<String>();
    static Set<String> available = new HashSet<String>();

    static final UnicodeSet NMTOKEN = new UnicodeSet(
        "[\\-.0-\\:A-Z_a-z\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u037D\\u037F-\\u1FFF\\u200C\\u200D\\u203F\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\U00010000-\\U000EFFFF]")
            .freeze();

    static {
        /*
         * # RFC3066; UN Numeric; ISO3166 Alpha-3, internet, FIPS-10
         * # whitespace delimited: - for empty
         * # See http://unstats.un.org/unsd/methods/m49/m49regin.htm
         * # and http://www.iso.org/iso/en/prods-services/iso3166ma/01whats-new/index.html
         * # See also http://www.cia.gov/cia/publications/factbook/appendix/appendix-d.html
         * # and http://data.iana.org/TLD/tlds-alpha-by-domain.txt for the latest domains
         * # and http://www.iana.org/cctld/cctld-whois.htm
         * # and https://www.icmwg.org/ccwg/documents/ISO3166-FIPS10-A2-Mapping/3166-1-A2--to-FIPS10-A2-mapping.htm
         * # for FIPS: http://earth-info.nga.mil/gns/html/fips_files.html
         * RS 688 SRB rs RB
         */
        try {
            BufferedReader codes;
            codes = CldrUtility.getUTF8Data("tlds-alpha-by-domain.txt");

            while (true) {
                String line = codes.readLine();
                if (line == null)
                    break;
                line = line.split("#")[0].trim();
                if (line.length() == 0)
                    continue;
                // if (line.startsWith("XN--")) {
                // try {
                // line = Punycode.decode(line.substring(4), null).toString();
                // if (!NMTOKEN.containsAll(line)) {
                // System.err.println("!NMTOKEN:" + line);
                // continue;
                // }
                // } catch (StringPrepParseException e) {
                // throw new IllegalArgumentException(e);
                // }
                // }
                other_internet.add(line);
            }
            codes.close();

            Set<String> errors = new LinkedHashSet<String>();
            codes = CldrUtility.getUTF8Data("territory_codes.txt");
            while (true) {
                String line = codes.readLine();
                if (line == null)
                    break;
                line = line.split("#")[0].trim();
                if (line.length() == 0)
                    continue;
                String[] sourceValues = line.split("\\s+");
                String[] values = new String[5];
                for (int i = 0; i < values.length; ++i) {
                    if (i >= sourceValues.length || sourceValues[i].equals("-")) {
                        values[i] = null;
                    } else {
                        values[i] = sourceValues[i];
                    }
                }
                String alpha2 = values[0];
                String numeric = values[1];
                String alpha3 = values[2];
                String internet = values[3];
                if (internet != null) {
                    internet = internet.toUpperCase();
                    LinkedHashSet<String> internetStrings = new LinkedHashSet<String>(
                        Arrays.asList(internet.split("/")));
                    if (!other_internet.containsAll(internetStrings)) {
                        errors.addAll(internetStrings);
                        errors.removeAll(other_internet);
                    }
                    other_internet.removeAll(internetStrings);
                    internet = CollectionUtilities.join(internetStrings, " ");
                }
                String fips10 = values[4];
                _numeric.put(alpha2, numeric);
                _alpha3.put(alpha2, alpha3);
                _fips10.put(alpha2, fips10);
                _internet.put(alpha2, internet);
            }
            codes.close();
            if (errors.size() != 0) {
                throw new IllegalArgumentException("Internet values illegal: " + errors);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        _internet.put("ZZ", CollectionUtilities.join(other_internet, " "));

        other_internet = Collections.unmodifiableSet(other_internet);

        available.addAll(_numeric.keySet());
        available.addAll(_alpha3.keySet());
        available.addAll(_fips10.keySet());
        available.addAll(_internet.keySet());

        _numeric = Collections.unmodifiableMap(_numeric);
        _alpha3 = Collections.unmodifiableMap(_alpha3);
        _fips10 = Collections.unmodifiableMap(_fips10);
        _internet = Collections.unmodifiableMap(_internet);
        available = Collections.unmodifiableSet(available);
    }

    public static String getNumeric(String countryCodeAlpha2) {
        return _numeric.get(countryCodeAlpha2);
    }

    public static String get_alpha3(String countryCodeAlpha2) {
        return _alpha3.get(countryCodeAlpha2);
    }

    public static String get_fips10(String countryCodeAlpha2) {
        return _fips10.get(countryCodeAlpha2);
    }

    public static String get_internet(String countryCodeAlpha2) {
        return _internet.get(countryCodeAlpha2);
    }

    public static Set<String> getOtherInternet() {
        return other_internet;
    }

    public static Set<String> getAvailable() {
        return available;
    }
}
