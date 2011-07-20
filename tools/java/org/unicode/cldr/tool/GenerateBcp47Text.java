package org.unicode.cldr.tool;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class GenerateBcp47Text {
    static final boolean SHOW_ONLY_WITHOUT_DESCRIPTION = false;
    
    SupplementalDataInfo info = SupplementalDataInfo.getInstance();
    Relation<String, String> keys2subtypes = info.getBcp47Keys();
    Relation<R2<String, String>, String> keySubtype2Aliases = info.getBcp47Aliases();
    Map<R2<String, String>, String> keySubtype2Description = info.getBcp47Descriptions();
    R2<String, String> probe = Row.of("","");

    public static void main(String[] args) {
        new GenerateBcp47Text().run();
    }

    private void run() {
        PrintWriter out = new PrintWriter(System.out);
        for (Entry<String, Set<String>> keyAndSubtype : keys2subtypes.keyValuesSet()) {
            String key = keyAndSubtype.getKey();
            showRecord(out, key,"");
            Set<String> subtypes = keyAndSubtype.getValue();
            for (String subtype : subtypes) {
                showRecord(out, key, subtype);
            }
        }
        out.close();
    }

    /**
%%
Type: language
Subtag: ab
Description: Abkhazian
Added: 2005-10-16
Suppress-Script: Cyrl
%%
     * @param key
     * @param string
     */
    private void showRecord(PrintWriter out, String key, String subtype) {
        // TODO Auto-generated method stub
        probe.set0(key).set1(subtype);
        final String description = keySubtype2Description.get(probe);
        if (SHOW_ONLY_WITHOUT_DESCRIPTION && description != null) return;
        out.println("%%");
        showField(out, "Key", key);
        showField(out, "Subtype", subtype);
        showField(out, "Aliases", keySubtype2Aliases.get(probe));
        showField(out, "Description", description);
    }

    private void showField(PrintWriter out, String title, Collection<String> set) {
        showField(out, title, set==null || set.isEmpty() ? null : CollectionUtilities.join(set, ", "));
    }

    private void showField(PrintWriter out, String title, String item) {
        out.write(item == null || item.isEmpty() ? "" : title + ": " + item + "\n");
    }
}
