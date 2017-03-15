package org.unicode.cldr.tool;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.ibm.icu.impl.Row.R2;

public class CompareSuppress {

    enum Status {
        missingSuppress, missingLikely, difference
    }

    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        config.getStandardCodes();
        Map<LstrType, Map<String, Map<LstrField, String>>> lstr = StandardCodes.getEnumLstreg();
        Map<String, Map<LstrField, String>> langData = lstr.get(LstrType.language);
        LanguageTagParser ltp = new LanguageTagParser();

        Map<String, String> langToSuppress = new TreeMap<>();
        for (Entry<String, Map<LstrField, String>> entry : langData.entrySet()) {
            String lang = entry.getKey();
            Map<LstrField, String> value = entry.getValue();
            String script = value.get(LstrField.Suppress_Script);
            if (script != null) {
                langToSuppress.put(lang, script);
            }
        }
        Set<String> langs = new TreeSet(langToSuppress.keySet());
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        Map<String, String> likely = sdi.getLikelySubtags();
        for (Entry<String, String> s : likely.entrySet()) {
            ltp.set(s.getValue());
            langs.add(ltp.getLanguage());
        }
        LikelySubtags likelyMaker = new LikelySubtags(likely);
        CLDRFile english = config.getEnglish();
        Map<String, R2<List<String>, String>> langAlias = sdi.getLocaleAliasInfo().get("language");

        for (Status status : Status.values()) {
            for (String base : langs) {
                String suppressScript = langToSuppress.get(base);

                String likelyScript = null;
                String max = likelyMaker.maximize(base);
                if (max != null) {
                    ltp.set(max);
                    likelyScript = ltp.getScript();
                }
                String prefix = langAlias.containsKey(base) ? "(dep) " : "";

                if (!Objects.equal(suppressScript, likelyScript)) {
                    switch (status) {
                    case difference:
                        if (likelyScript != null && suppressScript != null) {
                            System.out.println(prefix + status + "\t"
                                + langAndName(english, base)
                                + "\tSuppress:\t" + scriptAndName(english, suppressScript)
                                + "\tLikely:   \t" + scriptAndName(english, likelyScript));
                        }
                        break;
                    case missingLikely:
                        if (likelyScript == null) {
                            System.out.println(prefix + status + "\t"
                                + langAndName(english, base)
                                + "\t" + scriptAndName(english, suppressScript));
                        }
                        break;
                    case missingSuppress:
                        if (suppressScript == null) {
                            System.out.println(prefix + status + "\t"
                                + langAndName(english, base)
                                + "\t" + scriptAndName(english, likelyScript));
                            break;
                        }
                    }
                }
            }
            System.out.println();
        }
    }

    public static String langAndName(CLDRFile english, String base) {
        return base + "\t" + english.getName(base);
    }

    public static String scriptAndName(CLDRFile english, String suppressScript) {
        return suppressScript
            + "\t" + english.getName(CLDRFile.SCRIPT_NAME, suppressScript);
    }
}