package org.unicode.cldr.tool;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class GenerateBcp47Tests {
    public static void main(String[] args) {
//        UnicodeSet s = new UnicodeSet("[[:di:][:whitespace:]-[:cn:][:cc:]]").complement().complement();
//        System.out.println(s);
        SupplementalDataInfo info = SupplementalDataInfo.getInstance();
        Map<R2<String, String>, String> deprecatedMap = info.getBcp47Deprecated();
        Map<R2<String, String>, String> descriptionsMap = info.getBcp47Descriptions();
        System.out.println("#Deprecated");
        for (Entry<R2<String, String>, String> entry : deprecatedMap.entrySet()) {
            if (!"true".equals(entry.getValue())) {
                continue;
            }
            R2<String, String> key = entry.getKey();
            System.out.println("{\"" + key.get0() + "\", \"" + key.get1() + "\"},");
        }
        System.out.println("#Tests");
        Relation<String, String> extension2Keys = info.getBcp47Extension2Keys();
        Relation<String, String> keys2subtypes = info.getBcp47Keys();
        Set<String> deprecatedSet = new LinkedHashSet<String>();
        for (Entry<String, Set<String>> extensionKeys : extension2Keys.keyValuesSet()) {
            String extension = extensionKeys.getKey();
            Set<String> keys = extensionKeys.getValue();
            for (String key : keys) {
                final Set<String> subtypes = keys2subtypes.get(key);
                final R2<String, String> keyBlank = Row.of(key, "");
                boolean deprecatedKey = "true".equals(deprecatedMap.get(keyBlank));
                String keyDescription = descriptionsMap.get(keyBlank);
                String non_deprecated = null;
                String deprecated = null;
                if (subtypes != null) {
                    for (String subtype : subtypes) {
                        final R2<String, String> keySubtype = Row.of(key, subtype);
                        boolean deprecatedSubtype = deprecatedKey || "true".equals(deprecatedMap.get(keySubtype));
                        String subtypeDescription = descriptionsMap.get(keySubtype);

                        if (deprecatedSubtype) {
                            if (deprecated == null) {
                                deprecatedSet.add("{\"OK\", \"en-" + extension + "-" + key + "-" + subtype + "\"}, // deprecated "
                                    + keyDescription + "; " + subtypeDescription);
                                deprecated = subtype;
                            }
                        } else {
                            if (non_deprecated == null) {
                                System.out.println("{\"OK\", \"en-" + extension + "-" + key + "-" + subtype + "\"}, // "
                                    + keyDescription + "; " + subtypeDescription);
                                non_deprecated = subtype;
                            }
                        }
                    }
                } else {
                    System.out.println("{\"OK\", \"en-" + extension + "-" + key + "-" + "SPECIAL" + "\"}, // "
                        + keyDescription);
                }
            }
        }
        for (String dep : deprecatedSet) {
            System.out.println(dep);
        }
    }
}
