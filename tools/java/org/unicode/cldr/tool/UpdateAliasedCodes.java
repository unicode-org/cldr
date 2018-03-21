package org.unicode.cldr.tool;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Row.R2;

public class UpdateAliasedCodes {
    static CLDRConfig config = CLDRConfig.getInstance();
    static SupplementalDataInfo sdi = config.getSupplementalDataInfo();

    public static void main(String[] args) {
        Map<String, R2<List<String>, String>> togood = sdi.getLocaleAliasInfo().get("subdivision");
        // first find non-transitive cases
        for (Entry<String, R2<List<String>, String>> entry : togood.entrySet()) {
            String badCode = entry.getKey();
            R2<List<String>, String> replacement = entry.getValue();
            for (String repl : replacement.get0()) {
                R2<List<String>, String> better = togood.get(repl);
                if (better != null) {
                    System.out.println("Supp.Metadata mapping bad: " + badCode + " => " + replacement + "; " + repl + " should be " + better);
                }
            }
        }

        for (String locale : SubdivisionNames.getAvailableLocales()) {
            if (!"en".equals(locale)) {
                continue;
            }
            Map<String, String> ok = new TreeMap<>();
            Map<String, String> fixed = new TreeMap<>();
            SubdivisionNames names = new SubdivisionNames(locale);

            for (Entry<String, String> subdivisionAndName : names.entrySet()) {
                String code = subdivisionAndName.getKey();
                if (code.equals("AS")) {
                    int debug = 0;
                }
                String name = subdivisionAndName.getValue();
                if (name.isEmpty()) {
                    continue; // should never occur
                }
                R2<List<String>, String> aliasInfo = togood.get(code);
                if (aliasInfo == null) {
                    ok.put(code, name);
                    continue;
                }
                List<String> replacements = aliasInfo.get0();
                if (replacements.size() != 1) {
                    ok.put(code, name);
                    continue;
                }
                String goodCode = replacements.get(0);
                if (goodCode.length() == 2) {
                    // regular code, skip
                    continue;
                }

                // see if there is a conflict

                String oldName = names.get(goodCode);
                if (oldName == null || oldName.isEmpty()) {
                    fixed.put(goodCode, name);
                } else if (oldName.equals(name)) {
                    ok.put(code, name);
                } else {
                    System.out.println(locale + " Conflicting name "
                        + "<old:«" + code + "»«" + name + "»>"
                        + "<repl:«" + goodCode + "»«" + oldName + "»>");
                }
            }
            System.out.println(locale + "\t" + fixed);
        }
    }
}
