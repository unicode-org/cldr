package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class LikelySubtagsTest extends TestFmwk {

    private static final boolean ENABLE_REGION_TEST = false;

    public static void main(String[] args) {
        new LikelySubtagsTest().run(args);
    }

    static Set<String> exceptions = new HashSet<String>(Arrays.asList("Zyyy", "Zinh", "Zzzz", "Brai"));

    public void TestForMissingScriptMetadata() {
        TreeSet<String> metadataScripts = new TreeSet<String>(ScriptMetadata.getScripts());
        UnicodeSet current = new UnicodeSet(0, 0x10FFFF);
        UnicodeSet toRemove = new UnicodeSet();

        while (!current.isEmpty()) {
            int ch = current.charAt(0);
            int script = UScript.getScript(ch);
            String shortName = UScript.getShortName(script);
            Info i = ScriptMetadata.getInfo(shortName);
            if (i == null) {
                errln("Script Metadata is missing: " + shortName);
                continue;
            }
            if (i.likelyLanguage.equals("und") && !exceptions.contains(shortName)) {
                errln("Script has no likely language: " + shortName);
            }
            toRemove.applyIntPropertyValue(UProperty.SCRIPT, script);
            current.removeAll(toRemove);
            metadataScripts.remove(shortName);
        }
        metadataScripts.removeAll(Arrays.asList("Hans", "Hant", "Jpan", "Kore")); // remove "combo" scripts
        if (!metadataScripts.isEmpty()) {
            errln("Script Metadata for characters not in Unicode: " + metadataScripts);
        }
    }

    public void TestMissingInfoForLanguage() {
        Map<String, String> likely = TestInfo.getInstance().getSupplementalDataInfo().getLikelySubtags();
        CLDRFile english = TestInfo.getInstance().getEnglish();

        for (String language : TestInfo.getInstance().getCldrFactory().getAvailableLanguages()) {
            if (language.contains("_") || language.equals("root")) {
                continue;
            }
            String likelyExpansion = likely.get(language);
            if (likelyExpansion == null) {
                errln("Missing likely subtags for: " + language);
            } else {
                logln("Likely subtags for " + language + ":\t " + likely);
            }
            String path = CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, language);
            String englishName = english.getStringValue(path);
            if (englishName == null) {
                errln("Missing English translation for: " + language);
            }
        }
    }

    public void TestMissingInfoForRegion() {
        if (!ENABLE_REGION_TEST) {
            return;
        }
        Map<String, String> likely = TestInfo.getInstance().getSupplementalDataInfo().getLikelySubtags();
        CLDRFile english = TestInfo.getInstance().getEnglish();

        for (String region : StandardCodes.make().getGoodAvailableCodes("territory")) {
            String likelyExpansion = likely.get("und_" + region);
            if (likelyExpansion == null) {
                errln("Missing likely subtags for region: " + region);
            } else {
                logln("Likely subtags for region: " + region + ":\t " + likely);
            }
            String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, region);
            String englishName = english.getStringValue(path);
            if (englishName == null) {
                errln("Missing English translation for: " + region);
            }
        }
    }

    public void TestMissingInfoForScript() {
        Map<String, String> likely = TestInfo.getInstance().getSupplementalDataInfo().getLikelySubtags();
        TreeSet<String> sorted = new TreeSet<String>(ScriptMetadata.getScripts());
        Set<String> exceptions2 = new HashSet<String>(Arrays.asList("zh_Hans_CN"));
        for (String script : sorted) {
            if (exceptions.contains(script)
                || script.equals("Latn") || script.equals("Dsrt")) {
                // we minimize away und_X, when the code puts in en...US
                continue;
            }
            Info i = ScriptMetadata.getInfo(script);
            // System.out.println(i);
            String likelyLanguage = i.likelyLanguage;
            String originCountry = i.originCountry;
            String undScript = "und_" + script;
            String langScript = likelyLanguage + "_" + script + "_";
            String likelyExpansion = likely.get(undScript);
            if (likelyExpansion == null) {
                errln("Missing likely language for script (und_" + script + ")  should be something like:\t "
                    + showOverride(script, originCountry, langScript));
            } else if (!exceptions2.contains(likelyExpansion) && !likelyExpansion.startsWith(langScript)) {
                errln("Wrong likely language for script (und_" + script + "). Should not be " + likelyExpansion
                    + ", but something like:\t " + showOverride(script, originCountry, langScript));
            } else {
                logln("OK: " + undScript + " => " + likelyExpansion);
            }
        }
        /**
         * und_Bopo => zh_Bopo_TW
         * und_Copt => cop_Copt_EG // fix 002
         * und_Dsrt => en_Dsrt_US // fix US
         */
    }

    public String showOverride(String script, String originCountry, String langScript) {
        return "{\"und_" + script + "\", \"" + langScript + originCountry + "\"},";
    }
}
