package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class TestCanonicalIds extends TestFmwk {

    // TODO consider whether we can pull the $variable stuff from other
    // sources..

    static final Pattern WHITESPACE_PATTERN = PatternCache.get("\\s+");

    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static Map<String, Map<String, R2<List<String>, String>>> aliasInfo = testInfo
        .getSupplementalDataInfo().getLocaleAliasInfo();

    public static void main(String[] args) {
        new TestCanonicalIds().run(args);
    }

    public void TestTimezones() {
        Set<String> bcp47Canonical = new LinkedHashSet<String>();
        Relation<R2<String, String>, String> data = testInfo
            .getSupplementalDataInfo().getBcp47Aliases();
        Map<R2<String, String>, String> deprecatedData = testInfo
            .getSupplementalDataInfo().getBcp47Deprecated();

        // the first item in each set of aliases is the primary.
        for (Entry<R2<String, String>, Set<String>> entry : data.keyValuesSet()) {
            final R2<String, String> keyType = entry.getKey();
            if ("tz".equals(keyType.get0())) {
                if (keyType.get1().isEmpty()) {
                    continue;
                }
                String deprecated = deprecatedData.get(keyType);
                if ("true".equals(deprecated)) {
                    continue;
                }
                Set<String> aliases = entry.getValue();
                String firstAlias = aliases.iterator().next();
                bcp47Canonical.add(firstAlias);
            }
        }

        // check that the metadata is up to date
        // Not necessary any more, since the bcp47 data is used directly.

//        Map<String, R2<String, String>> validityInfo = testInfo
//            .getSupplementalDataInfo().getValidityInfo();
//        String timezoneItemString = validityInfo.get("$tzid").get1();
//        HashSet<String> variable = new LinkedHashSet<String>(
//            Arrays.asList(WHITESPACE_PATTERN.split(timezoneItemString
//                .trim())));
//        if (!variable.equals(bcp47Canonical)) {
//            TreeSet<String> bcp47Only = new TreeSet<String>(bcp47Canonical);
//            bcp47Only.removeAll(variable);
//            TreeSet<String> variableOnly = new TreeSet<String>(variable);
//            variableOnly.removeAll(bcp47Canonical);
//            errln("Timezones: bcp47≠validity; bcp47:\t" + bcp47Only
//                + ";\tvalidity:\t" + variableOnly);
//        }
    }

    enum Type {
        language, script, territory, zone
    }

//    public void TestForDeprecatedVariables() {
//        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = testInfo
//            .getSupplementalDataInfo().getLocaleAliasInfo();
//        // language, script, territory, variant, zone
//        Map<String, R2<String, String>> validityInfo = testInfo
//            .getSupplementalDataInfo().getValidityInfo();
//        for (Entry<String, R2<String, String>> entry : validityInfo.entrySet()) {
//            String key = entry.getKey();
//            if (key.equals("$language")) {
//                checkItems(aliasInfo, entry, key, Type.language);
//            } else if (key.equals("$script")) {
//                checkItems(aliasInfo, entry, key, Type.script);
//            } else if (key.equals("$territory")) {
//                checkItems(aliasInfo, entry, key, Type.territory);
//            } else if (key.equals("$tzid")) {
//                checkItems(aliasInfo, entry, key, Type.zone);
//            }
//        }
//    }

    private void checkItems(
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo,
        Entry<String, R2<String, String>> entry, String key, final Type type) {
        Map<String, R2<List<String>, String>> badMap = aliasInfo.get(type
            .toString());
        final String valueString = entry.getValue().get1();
        HashSet<String> values = new LinkedHashSet<String>(
            Arrays.asList(WHITESPACE_PATTERN.split(valueString.trim())));
        Set<String> emptyScripts = new TreeSet<String>();
        UnicodeSet remainingCharacters = new UnicodeSet(0, 0x10FFFF);
        UnicodeSet s = new UnicodeSet();
        for (String value : values) {
            R2<List<String>, String> replacement = badMap.get(value);
            if (replacement != null && replacement.get1().equals("deprecated")
                && !isOk(type, value)) {
                errln("Deprecated value in " + key + ":\t" + value
                    + " should be " + badMap.get(value).get0());
            }
            if (type == Type.script) {
                int scriptEnum = UScript.getCodeFromName(value);
                s.applyIntPropertyValue(UProperty.SCRIPT, scriptEnum);
                if (s.size() == 0) {
                    emptyScripts.add(value);
                } else {
                    remainingCharacters.removeAll(s);
                }
            }
        }
        if (type == Type.script) {
            final List<String> specialValues = Arrays.asList("Zmth", "Zsym",
                "Zxxx");
            emptyScripts.removeAll(specialValues);
            // Empty scripts can still be valid in CLDR, so this test is bogus
            // if (!emptyScripts.isEmpty()) {
            // errln("Remove empty scripts from $script!: " + emptyScripts);
            // }
            Set<String> missingScripts = new TreeSet<String>(specialValues);
            missingScripts.removeAll(values);
            while (remainingCharacters.size() != 0) {
                String first = remainingCharacters.iterator().next();
                int scriptEnum = UScript.getScript(first.codePointAt(0));
                missingScripts.add(UScript.getShortName(scriptEnum));
                s.applyIntPropertyValue(UProperty.SCRIPT, scriptEnum);
                remainingCharacters.removeAll(s);
            }
            if (!missingScripts.isEmpty()) {
                errln("Add missing scripts to $script!: " + emptyScripts);
            }
        }
    }

    static final long CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    private boolean isOk(Type type, String value) {
        if (type == Type.territory) {
            if (value.equals("QU")) {
                return false;
            }
            Map<String, String> regionInfo = StandardCodes.getLStreg()
                .get("region").get(value);
            if (regionInfo == null) {
                errln("Region info null for " + value);
                return false;
            }
            String deprecated = regionInfo.get("Deprecated");
            if (deprecated == null) {
                errln("No deprecated info for " + value);
                return false;
            }
            Matcher m = PatternCache.get("(\\d{4})-(\\d{2})-(\\d{2})").matcher(
                deprecated);
            if (!m.matches()) {
                errln("Bad deprecated date for " + value + ", " + deprecated);
                return false;
            }
            long deprecationYear = Integer.parseInt(m.group(1));
            if (CURRENT_YEAR - deprecationYear <= 5) {
                logln("Region " + value
                    + " is deprecated but less than 5 years...");
                return true;
            }
        } else if (type == Type.language) {
            Map<String, String> languageInfo = StandardCodes.getLStreg()
                .get("language").get(value);
            if (languageInfo == null) {
                errln("Language info null for " + value);
                return false;
            }
            String deprecated = languageInfo.get("Deprecated");
            if (deprecated == null) {
                errln("No deprecated info for " + value);
                return false;
            }
            Matcher m = PatternCache.get("(\\d{4})-(\\d{2})-(\\d{2})").matcher(
                deprecated);
            if (!m.matches()) {
                errln("Bad deprecated date for " + value + ", " + deprecated);
                return false;
            }
            long deprecationYear = Integer.parseInt(m.group(1));
            if (CURRENT_YEAR - deprecationYear <= 5) {
                logln("Language " + value
                    + " is deprecated but less than 5 years...");
                return true;
            }
        }
        return false;
    }
}
