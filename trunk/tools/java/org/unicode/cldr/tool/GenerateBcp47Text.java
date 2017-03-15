package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class GenerateBcp47Text {
    static final boolean SHOW_ONLY_WITHOUT_DESCRIPTION = false;

    static SupplementalDataInfo info = SupplementalDataInfo.getInstance();
    static Map<R2<String, String>, String> deprecatedMap = info.getBcp47Deprecated();

    Relation<String, String> extension2Keys = info.getBcp47Extension2Keys();
    Relation<String, String> keys2subtypes = info.getBcp47Keys();
    Relation<R2<String, String>, String> keySubtype2Aliases = info.getBcp47Aliases();
    Map<R2<String, String>, String> keySubtype2Description = info.getBcp47Descriptions();
    Map<R2<String, String>, String> keySubtype2Since = info.getBcp47Since();
    R2<String, String> probe = Row.of("", "");

    public static void main(String[] args) throws IOException {
        new GenerateBcp47Text().run();
    }

    private void run() throws IOException {
        for (Entry<String, Set<String>> extensionAndKeys : extension2Keys.keyValuesSet()) {

            String extension = extensionAndKeys.getKey();
            PrintWriter out = FileUtilities.openUTF8Writer(FormattedFileWriter.CHART_TARGET_DIR, "bcp47-" + extension + ".txt");
            showField(out, "Version", ToolConstants.CHART_DISPLAY_VERSION);
            showField(out, "Extension", extension);

            Set<String> keys = extensionAndKeys.getValue();
            for (String key : keys) {
                showRecord(out, extension, key, "");
            }
            for (Entry<String, Set<String>> keyAndSubtype : keys2subtypes.keyValuesSet()) {
                String key = keyAndSubtype.getKey();
                if (!keys.contains(key)) {
                    continue;
                }
                Set<String> subtypes = keyAndSubtype.getValue();
                for (String subtype : subtypes) {
                    showRecord(out, extension, key, subtype);
                }
            }
            out.close();
        }
    }

    /**
     * %%
     * Type: language
     * Subtag: ab
     * Description: Abkhazian
     * Added: 2005-10-16
     * Suppress-Script: Cyrl
     * %%
     *
     * @param key
     * @param string
     */
    private void showRecord(PrintWriter out, String extension, String key, String subtype) {
        // TODO Auto-generated method stub
        probe.set0(key).set1(subtype);
        final String description = keySubtype2Description.get(probe);
        final String since = keySubtype2Since.get(probe);
        if (SHOW_ONLY_WITHOUT_DESCRIPTION && description != null) return;
        out.println("%%");
        // showField(out, "Extension", extension);
        showField(out, "Key", key);
        showField(out, "Subtype", subtype);
        showField(out, "Aliases", keySubtype2Aliases.get(probe));
        showField(out, "Description", description);
        showField(out, "Since", since);
        String deprecatedValue = deprecatedMap.get(Row.of(key, subtype));
        if (!"false".equals(deprecatedValue)) {
            showField(out, "Deprecated", deprecatedValue);
        }
    }

    private void showField(PrintWriter out, String title, Collection<String> set) {
        showField(out, title, set == null || set.isEmpty() ? null : CollectionUtilities.join(set, ", "));
    }

    private void showField(PrintWriter out, String title, String item) {
        out.write(item == null || item.isEmpty() ? "" : title + ": " + item + System.lineSeparator());
    }
}
