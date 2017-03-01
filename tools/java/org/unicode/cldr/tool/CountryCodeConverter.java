package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.LineHandler;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

public class CountryCodeConverter {

    private static final boolean SHOW_SKIP = CldrUtility.getProperty("SHOW_SKIP", false);

    private static Map<String, String> nameToCountryCode = new TreeMap<String, String>(new UTF16.StringComparator(true, true, 0));
    private static Set<String> parseErrors = new LinkedHashSet<String>();

    public static String getCodeFromName(String display) {
        String trial = display.trim().toLowerCase(Locale.ENGLISH);
        if (trial.startsWith("\"") && trial.endsWith("\"")) {
            trial = trial.substring(1, trial.length() - 2);
        }
        if (trial.startsWith("the ")) {
            trial = trial.substring(4);
        }
        String result = nameToCountryCode.get(trial);
        if ("skip".equals(result)) {
            return null;
        }
        if (result == null) {
            trial = reverseComma(display);
            if (trial != null) {
                result = nameToCountryCode.get(trial);
                // if (result != null) {
                // addName(trial, result);
                // }
            }
        }
        if (SHOW_SKIP && result == null) {
            System.out.println("Missing code; add to external/alternate_country_names.txt a line like:" +
                "\t<code>;\t<name>;\t" + display);
        }
        return result;
    }

    public static Set<String> names() {
        return nameToCountryCode.keySet();
    }

    private static String reverseComma(String display) {
        String trial;
        trial = null;
        int comma = display.indexOf(',');
        if (comma >= 0) {
            trial = display.substring(comma + 1).trim() + " " + display.substring(0, comma).trim();
        }
        return trial;
    }

    static {
        try {
            loadNames();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static void loadNames() throws IOException {
        for (String country : ULocale.getISOCountries()) {
            addName(ULocale.getDisplayCountry("und-" + country, "en"), country);
        }
        StandardCodes sc = StandardCodes.make();
        Set<String> goodAvailableCodes = sc.getGoodAvailableCodes("territory");

        for (String country : goodAvailableCodes) {
            String description = (String) sc.getFullData("territory", country).get(0);
            if (country.equals("057")) continue;
            addName(description, country);
        }
        CldrUtility.handleFile("external/alternate_country_names.txt", new MyHandler(goodAvailableCodes));
        nameToCountryCode = CldrUtility.protectCollection(nameToCountryCode);
        parseErrors = Collections.unmodifiableSet(parseErrors);
    }

    static class MyHandler implements LineHandler {
        private Set<String> goodAvailableCodes;

        public MyHandler(Set<String> goodAvailableCodes) {
            this.goodAvailableCodes = goodAvailableCodes;
        }

        public boolean handle(String line) {
            if (line.trim().length() == 0) {
                return true; // don't show skips
            }
            String[] pieces = line.split(";");
            String country = pieces[0].trim();
            if (!goodAvailableCodes.contains(country)) {

            }
            addName(pieces[2].trim(), country);
            return true;
        }
    }

    static void addName(String key, String code) {
        addName2(key, code);
        String trial = reverseComma(key);
        if (trial != null) {
            addName2(trial, code);
        }
    }

    private static void addName2(String key, String code) {
        key = key.toLowerCase(Locale.ENGLISH);
        if (key.startsWith("the ")) {
            key = key.substring(4);
        }
        String old = nameToCountryCode.get(key);
        if (old != null && !code.equals(old)) {
            System.err.println("Conflict!!" + key + "\t" + old + "\t" + code);
            return;
        }
        nameToCountryCode.put(key, code);
    }

    public static Set<String> getParseErrors() {
        return parseErrors;
    }

}
