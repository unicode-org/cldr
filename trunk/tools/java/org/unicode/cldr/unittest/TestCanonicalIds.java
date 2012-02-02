package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row.R2;

public class TestCanonicalIds extends TestFmwk {

    // TODO consider whether we can pull the $variable stuff from other sources..
    
    static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    

    static TestAll.TestInfo testInfo = TestAll.TestInfo.getInstance();
    static Map<String, Map<String, R2<List<String>, String>>> aliasInfo = testInfo.getSupplementalDataInfo().getLocaleAliasInfo();
    
    public static void main(String[] args) {
        new TestCanonicalIds().run(args);
    }

    public void TestTimezones() {
        Set<String> bcp47Canonical = new LinkedHashSet<String>();
        Relation<R2<String, String>, String> data = testInfo.getSupplementalDataInfo().getBcp47Aliases();
        
        // the first item in each set of aliases is the primary.
        for (Entry<R2<String, String>, Set<String>> entry : data.keyValuesSet()) {
            final R2<String, String> keyType = entry.getKey();
            if ("tz".equals(keyType.get0())) {
                if (keyType.get1().isEmpty()) {
                    continue;
                }
                Set<String> aliases = entry.getValue();
                String firstAlias = aliases.iterator().next();
                bcp47Canonical.add(firstAlias);
            }
        }
        
        // check that the metadata is up to date
        
        Map<String, R2<String, String>> validityInfo = testInfo.getSupplementalDataInfo().getValidityInfo();
        String timezoneItemString = validityInfo.get("$tzid").get1();
        HashSet<String> variable = new LinkedHashSet<String>(Arrays.asList(WHITESPACE_PATTERN.split(timezoneItemString.trim())));
        if (!variable.equals(bcp47Canonical)) {
            TreeSet<String> bcp47Only = new TreeSet<String>(bcp47Canonical);
            bcp47Only.removeAll(variable);
            TreeSet<String> variableOnly = new TreeSet<String>(variable);
            variableOnly.removeAll(bcp47Canonical);
            errln("Timezones: bcp47≠validity; bcp47:\t" + bcp47Only + ";\tvalidity:\t" + variableOnly);
        }
    }
    
    public void TestForDeprecatedVariables() {
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = testInfo.getSupplementalDataInfo().getLocaleAliasInfo();
        // language, script, territory, variant, zone
        Map<String, R2<String, String>> validityInfo = testInfo.getSupplementalDataInfo().getValidityInfo();
        for (Entry<String, R2<String, String>> entry : validityInfo.entrySet()) {
            String key = entry.getKey();
            if (key.equals("$language")) {
                checkItems(aliasInfo, entry, key, "language");
            } else if (key.equals("$script")) {
                checkItems(aliasInfo, entry, key, "script");
            } else if (key.equals("$territory")) {
                checkItems(aliasInfo, entry, key, "territory");
            } else if (key.equals("$tzid")) {
                checkItems(aliasInfo, entry, key, "zone");
            }
        }
    }

    private void checkItems(Map<String, Map<String, R2<List<String>, String>>> aliasInfo, Entry<String, R2<String, String>> entry, String key,
            final String aliasKey) {
        Map<String, R2<List<String>, String>> badMap = aliasInfo.get(aliasKey);
        final String valueString = entry.getValue().get1();
        HashSet<String> values = new LinkedHashSet<String>(Arrays.asList(WHITESPACE_PATTERN.split(valueString.trim())));
        for (String value : values) {
            R2<List<String>, String> replacement = badMap.get(value);
            if (replacement != null) {
                errln("Deprecated value in " + key + ":\t" + value + " should be " + badMap.get(value).get0());
            }
        }
    }
    
    public void ZZZ() {
    }
}
