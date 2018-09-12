package org.unicode.cldr.tool;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class DeriveScripts {
    private static final boolean SHOW = false;

    static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SUP = CONFIG.getSupplementalDataInfo();
    static final Multimap<String, String> LANG_TO_SCRIPT;
    static final Map<String, String> SUPPRESS;

    static {
        File[] paths = {
//            new File(CLDRPaths.MAIN_DIRECTORY), 
//            new File(CLDRPaths.SEED_DIRECTORY), 
            new File(CLDRPaths.EXEMPLARS_DIRECTORY) };
        final Factory fullCldrFactory = SimpleFactory.make(paths, ".*");
        LikelySubtags ls = new LikelySubtags();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> seen = new HashSet<>();

        Multimap<String, String> langToScript = TreeMultimap.create();

        Map<String, String> suppress = new TreeMap<>();
        final Map<String, Map<LstrField, String>> langToInfo = StandardCodes.getLstregEnumRaw().get(LstrType.language);
        for (Entry<String, Map<LstrField, String>> entry : langToInfo.entrySet()) {
            final String suppressValue = entry.getValue().get(LstrField.Suppress_Script);
            if (suppressValue != null) {
                final String langCode = entry.getKey();
                String likelyScript = ls.getLikelyScript(langCode);
                if (!likelyScript.equals("Zzzz")) {
//                    if (!suppressValue.equals(likelyScript)) {
//                        System.out.println("#" + langCode + "\tWarning: likely=" + likelyScript + ", suppress=" + suppressValue);
//                    } else {
//                        System.out.println("#" + langCode + "\tSuppress=Likely: " + suppressValue); 
//                    }
                    continue;
                }
                suppress.put(langCode, suppressValue);
            }
        }
        SUPPRESS = ImmutableMap.copyOf(suppress);

        LanguageTagCanonicalizer canon = new LanguageTagCanonicalizer();

        for (String file : fullCldrFactory.getAvailable()) {
            String langScript = ltp.set(file).getLanguage();
            if (!file.equals(langScript)) { // skip other variants
                continue;
            }
//            System.out.println(file);
//            if (!seen.add(lang)) { // add if not present
//                continue;
//            }
            String lang = canon.transform(ltp.getLanguage());
            if (lang.equals("root")) {
                continue;
            }

//            String likelyScript = ls.getLikelyScript(lang);
//            if (!likelyScript.equals("Zzzz")) {
//                continue;
//            }

            String script = "";
//            script = ltp.getScript();
//            if (!script.isEmpty()) {
//                add(langToScript, lang, script);
//                continue;
//            }

            CLDRFile cldrFile = fullCldrFactory.make(lang, false);
            UnicodeSet exemplars = cldrFile.getExemplarSet("", WinningChoice.WINNING);
            for (String s : exemplars) {
                int scriptNum = UScript.getScript(s.codePointAt(0));
                if (scriptNum != UScript.COMMON && scriptNum != UScript.INHERITED && scriptNum != UScript.UNKNOWN) {
                    script = UScript.getShortName(scriptNum);
                    break;
                }
            }
            if (!script.isEmpty()) {
                add(langToScript, lang, script);
            }
        }
        LANG_TO_SCRIPT = ImmutableMultimap.copyOf(langToScript);
    }

    private static void add(Multimap<String, String> langToScript, String lang, String script) {
        if (script != null) {
            if (langToScript.put(lang, script)) {
                if (SHOW) System.out.println("# Adding from actual exemplars: " + lang + ", " + script);
            }
        }
    }

    public static Multimap<String, String> getLanguageToScript() {
        return LANG_TO_SCRIPT;
    }

    public static void showLine(String language, String scriptField, String status) {
        CLDRFile english = CONFIG.getEnglish();
        System.out.println(language + ";\t" + scriptField + "\t# " + english.getName(CLDRFile.LANGUAGE_NAME, language)
            + ";\t" + status
            + ";\t" + Iso639Data.getScope(language)
            + ";\t" + Iso639Data.getType(language));
    }

    public static void main(String[] args) {
        LikelySubtags ls = new LikelySubtags();
        CLDRFile english = CONFIG.getEnglish();
        int count = 0;

        int i = 0;
        System.out.println("#From Suppress Script");
        for (Entry<String, String> entry : SUPPRESS.entrySet()) {
            showLine(entry.getKey(), entry.getValue(), "Suppress");
            ++i;
        }
        System.out.println("#total:\t" + i);
        i = 0;
        boolean haveMore = true;

        System.out.println("\n#From Exemplars");
        for (int scriptCount = 1; haveMore; ++scriptCount) {
            haveMore = false;
            if (scriptCount != 1) {
                System.out.println("\n#NEEDS RESOLUTION:\t" + scriptCount + " scripts");
            }
            for (Entry<String, Collection<String>> entry : getLanguageToScript().asMap().entrySet()) {
                Collection<String> scripts = entry.getValue();
                final int scriptsSize = scripts.size();
                if (scriptsSize != scriptCount) {
                    if (scriptsSize > scriptCount) {
                        haveMore = true;
                    }
                    continue;
                }

                String lang = entry.getKey();
                showLine(lang, scripts.size() == 1 ? scripts.iterator().next() : scripts.toString(), "Exemplars" + (scripts.size() == 1 ? "" : "*"));
                ++i;
                String likelyScript = scriptsSize == 1 ? "" : ls.getLikelyScript(lang);
                System.out.println(++count + "\t" + scriptsSize + "\t" + lang + "\t" + english.getName(lang)
                    + "\t" + scripts + "\t" + likelyScript
//                + "\t" + script + "\t" + english.getName(CLDRFile.SCRIPT_NAME, script)
                );
            }
            System.out.println("#total:\t" + i);
            i = 0;
        }
    }

    public static Map<String, String> getSuppress() {
        return SUPPRESS;
    }
}
