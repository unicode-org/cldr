package org.unicode.cldr.unittest;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class TestBcp47Numbers extends TestFmwk {
    public static void main(String[] args) {
        new TestBcp47Numbers().run(args);
    }

    static CLDRConfig info = CLDRConfig.getInstance();

    static UnicodeMap<String> specials = new UnicodeMap<String>()
        .put('0', "latn; Western (European/ASCII) digits")
        .put('٠', "arab; Arabic-Indic digits")
        .put('۰', "arabext; Eastern Arabic-Indic digits")
        .put('႐', "mymrshan; Myanmar Shan digits")
        .put('௦', "tamldec; Modern Tamil decimal digits")
        .put('᪀', "tamldec; Tai Tham — Hora digits")
        .put('᪀', "lanahora; Tai Tham — Tai Tham Hora digits")
        .put('᪐', "lanatham; Tai Tham — Hora digits");

    public void TestScripts() {
        UnicodeSet decimals = new UnicodeSet("[[:Nd:]&[:nv=0:]-[:nfkcqc=n:]]");
        Map<String, String> decimalSystems = new TreeMap<String, String>();
        for (String s : decimals) {
            final int cp = s.codePointAt(0);
            String special = specials.get(cp);
            String shortName, longName;
            if (special != null) {
                String[] specialParts = special.split(";");
                shortName = specialParts[0].trim();
                longName = specialParts[1].trim();
            } else {
                int i = UScript.getScript(cp);
                shortName = UScript.getShortName(i).toLowerCase(Locale.ENGLISH);
                while (decimalSystems.containsKey(shortName)) {
                    shortName += "*";
                }
                longName = UScript.getName(i) + " digits";
            }
            decimalSystems.put(shortName, longName);
        }
        // special case
        decimalSystems.put("fullwide", "Full width digits");
        decimalSystems
            .put("hanidec",
                "Positional decimal system using Chinese number ideographs as digits");

        Map<String, String> typeDescription = new TreeMap<String, String>();
        final Map<R2<String, String>, String> keyTypeToDescription = info
            .getSupplementalDataInfo().getBcp47Descriptions();
        for (Entry<String, String> entry : info.getSupplementalDataInfo()
            .getBcp47Keys().keyValueSet()) {
            if (!entry.getKey().equals("nu")) {
                continue;
            }
            String description = keyTypeToDescription.get(Row.of(
                entry.getKey(), entry.getValue()));
            typeDescription.put(entry.getValue(), description);
        }
        // the bcp47 codes must cover all scripts with Nd values.
        Map<String, String> missing = new TreeMap<String, String>(
            decimalSystems);
        for (Entry<String, String> entry : typeDescription.entrySet()) {
            missing.remove(entry.getKey());
            if (decimalSystems.containsKey(entry.getKey())
                && entry.getValue().contains("non-decimal")) {
                errln("Decimal code marked as non-decimal: " + entry);
            }
        }

        for (Entry<String, String> entry : missing.entrySet()) {
            errln("Missing script codes: " + entry);
        }

        // check for extras
        Map<String, String> extras = new TreeMap<String, String>(
            typeDescription);
        for (String code : decimalSystems.keySet()) {
            extras.remove(code);
        }
        // check that all extras are non-decimal
        for (Entry<String, String> extra : extras.entrySet()) {
            if (!extra.getValue().contains("non-decimal")) {
                errln("Numeric code not marked as non-decimal: " + extra);
            }
        }
    }
}
