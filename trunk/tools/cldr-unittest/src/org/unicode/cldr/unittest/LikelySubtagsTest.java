package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class LikelySubtagsTest extends TestFmwk {

    private boolean DEBUG = false;
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig
        .getInstance().getSupplementalDataInfo();
    static final Map<String, String> likely = SUPPLEMENTAL_DATA_INFO
        .getLikelySubtags();
    static final LikelySubtags LIKELY = new LikelySubtags(
        SUPPLEMENTAL_DATA_INFO, likely);

    public static void main(String[] args) {
        new LikelySubtagsTest().run(args);
    }

    static class Tags {
        final Set<String> languages = new TreeSet<String>();
        final Set<String> scripts = new TreeSet<String>();
        final Set<String> regions = new TreeSet<String>();
        final Set<String> scriptRegion = new TreeSet<String>();
        final Set<String> languageScript = new TreeSet<String>();
        final Set<String> languageRegion = new TreeSet<String>();
        final Set<String> all = new TreeSet<String>();
        final ChainedMap.M4<String, String, String, Boolean> languageToScriptToRegions = ChainedMap
            .of(new TreeMap<String, Object>(),
                new TreeMap<String, Object>(),
                new TreeMap<String, Object>(), Boolean.class);
        final ChainedMap.M3<String, String, Boolean> languageToRegions = ChainedMap
            .of(new TreeMap<String, Object>(),
                new TreeMap<String, Object>(), Boolean.class);

        public Tags() {
            final LanguageTagParser ltp = new LanguageTagParser();
            for (Entry<String, String> entry : likely.entrySet()) {
                add(ltp.set(entry.getKey()), true);
                add(ltp.set(entry.getValue()), false);
            }
            // add unfamiliar script, unfamiliar region
            for (String lang : languageToScriptToRegions.keySet()) {
                if (lang.equals("und")) {
                    continue;
                }
                M3<String, String, Boolean> scriptToRegion = languageToScriptToRegions
                    .get(lang);
                final Set<String> scriptsFor = scriptToRegion.keySet();
                final Set<String> regionsFor = languageToRegions.get(lang)
                    .keySet();

                String firstScriptNotIn = getNonEmptyNotIn(scripts, scriptsFor);
                String firstRegionNotIn = getNonEmptyNotIn(regions, regionsFor);

                languageToScriptToRegions.put(lang, firstScriptNotIn,
                    firstRegionNotIn, Boolean.TRUE);
                // clone for safety before iterating
                for (String script : new HashSet<String>(scriptsFor)) {
                    languageToScriptToRegions.put(lang, script,
                        firstRegionNotIn, Boolean.TRUE);
                }
                for (String region : new HashSet<String>(regionsFor)) {
                    languageToScriptToRegions.put(lang, firstScriptNotIn,
                        region, Boolean.TRUE);
                }
            }

            // System.out.println("all: " + all);
            // System.out.println("scriptRegion: " + scriptRegion);
            // System.out.println("languageScript: " + languageScript);
            // System.out.println("languageRegion: " + languageRegion);
        }

        private static <T> T getNonEmptyNotIn(Iterable<T> a, Set<T> b) {
            for (T x : a) {
                if (!b.contains(x) && !x.toString().isEmpty()) {
                    return x;
                }
            }
            throw new IllegalArgumentException();
        }

        void add(LanguageTagParser ltp, boolean source) {
            String sourceLanguage = ltp.getLanguage();
            String sourceScript = ltp.getScript();
            String sourceRegion = ltp.getRegion();
            languageToScriptToRegions.put(sourceLanguage, sourceScript,
                sourceRegion, Boolean.TRUE);
            languageToScriptToRegions.put(sourceLanguage, sourceScript, "",
                Boolean.TRUE);
            languageToScriptToRegions.put(sourceLanguage, "", "", Boolean.TRUE);
            languageToRegions.put(sourceLanguage, "", Boolean.TRUE);
            if (StandardCodes.isCountry(sourceRegion)) {
                languageToScriptToRegions.put(sourceLanguage, "", sourceRegion,
                    Boolean.TRUE);
                languageToRegions.put(sourceLanguage, sourceRegion,
                    Boolean.TRUE);
            }

            // capture all cases of 2 items
            if (source) {
                if (!sourceScript.isEmpty() && !sourceRegion.isEmpty()) {
                    if (!sourceLanguage.equals("und")) {
                        all.add(ltp.toString());
                    } else {
                        scriptRegion.add(ltp.toString());
                    }
                } else if (!sourceLanguage.equals("und")) {
                    if (!sourceScript.isEmpty()) {
                        languageScript.add(ltp.toString());
                    } else if (!sourceRegion.isEmpty()) {
                        languageRegion.add(ltp.toString());
                    }
                }
            }
            languages.add(sourceLanguage);
            scripts.add(sourceScript);
            if (StandardCodes.isCountry(sourceRegion) || sourceRegion.isEmpty()) {
                regions.add(sourceRegion);
            }
        }
    }

    static final Tags TAGS = new Tags();

    final LanguageTagParser maxLtp = new LanguageTagParser();
    final LanguageTagParser sourceLtp = new LanguageTagParser();

    /**
     * Return false if we should skip the language
     *
     * @param source
     * @return
     */
    public boolean checkAdding(String source) {
        // if X maps to Y, then adding a field from Y to X will still map to Y
        // Example:
        // und_AF => fa_Arab_AF
        // therefore, the following should also be true:
        // und_Arab_AF => fa_Arab_AF
        // fa_AF => fa_Arab_AF
        // fa_Arab_AF => fa_Arab_AF

        String max = LIKELY.maximize(source);
        if (!assertNotEquals("Maximize " + source, null, max)) {
            return source.contains("_");
        }
        sourceLtp.set(source);
        if (!sourceLtp.getRegion().isEmpty()
            && !StandardCodes.isCountry(sourceLtp.getRegion())) {
            return true;
        }
        maxLtp.set(max);
        for (int i = 1; i < 8; ++i) {
            if ((i & 1) != 0) {
                if (!sourceLtp.getLanguage().equals("und"))
                    continue;
                sourceLtp.setLanguage(maxLtp.getLanguage());
            }
            if ((i & 2) != 0) {
                if (!sourceLtp.getScript().isEmpty())
                    continue;
                sourceLtp.setScript(maxLtp.getScript());
            }
            if ((i & 4) != 0) {
                if (!sourceLtp.getRegion().isEmpty())
                    continue;
                sourceLtp.setRegion(maxLtp.getRegion());
            }
            String test = sourceLtp.toString();
            final String maximize = LIKELY.maximize(test);
            if (!max.equals(maximize)) {
                if (!assertEquals(source + " -> " + max + ", so testing "
                    + test, max, maximize)) {
                    LIKELY.maximize(test); // do again for debugging
                }
            }
            sourceLtp.set(source); // restore
        }
        return true;
    }

    public void TestCompleteness() {
        // if (logKnownIssue("Cldrbug:7121",
        // "Problems with likely subtags test")) {
        // return;
        // }
        // checkAdding("und_Bopo");
        // checkAdding("und_Brai");
        // checkAdding("und_Limb");
        // checkAdding("und_Cakm");
        // checkAdding("und_Shaw");

        final LanguageTagParser ltp = new LanguageTagParser();
        if (DEBUG) {
            System.out.println(TAGS.languages.size() + "\t" + TAGS.languages);
            System.out.println(TAGS.scripts.size() + "\t" + TAGS.scripts);
            System.out.println(TAGS.regions.size() + "\t" + TAGS.regions);
        }
        main: for (Entry<String, Map<String, Map<String, Boolean>>> languageScriptRegion : TAGS.languageToScriptToRegions) {
            String language = languageScriptRegion.getKey();
            ltp.set(language); // clears script, region
            for (Entry<String, Map<String, Boolean>> scriptRegion : languageScriptRegion
                .getValue().entrySet()) {
                String script = scriptRegion.getKey();
                ltp.setScript(script);
                for (String region : scriptRegion.getValue().keySet()) {
                    ltp.setRegion(region);
                    String testTag = ltp.toString();
                    // System.out.println(testTag);
                    if (!checkAdding(testTag)) {
                        continue main;
                    }
                }
            }
        }
    }

    static Set<String> exceptions = new HashSet<String>(Arrays.asList("Zyyy",
        "Zinh", "Zzzz", "Brai"));

    public void TestStability() {
        // when maximized must never change
        // first get all the subtags
        // then test all the combinations
        LanguageTagParser ltp = new LanguageTagParser();
        for (Entry<String, String> entry : likely.entrySet()) {
            ltp.set(entry.getKey());
            String sourceLanguage = ltp.getLanguage();
            if (sourceLanguage.equals("und")) {
                sourceLanguage = "";
            }
            String sourceScript = ltp.getScript();
            String sourceRegion = ltp.getRegion();
            ltp.set(entry.getValue());
            String targetLanguage = ltp.getLanguage();
            String targetScript = ltp.getScript();
            String targetRegion = ltp.getRegion();
            if (!sourceLanguage.isEmpty()) {
                assertEquals("language", sourceLanguage, targetLanguage);
            }
            if (!sourceScript.isEmpty()) {
                assertEquals("script", sourceScript, targetScript);
            }
            if (!sourceRegion.isEmpty()) {
                if (Containment.isLeaf(sourceRegion)) {
                    assertEquals("region", sourceRegion, targetRegion);
                }
            }
        }

    }

    public void TestForMissingScriptMetadata() {
        TreeSet<String> metadataScripts = new TreeSet<String>(
            ScriptMetadata.getScripts());
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
            if (i.likelyLanguage.equals("und")
                && !exceptions.contains(shortName)) {
                errln("Script has no likely language: " + shortName);
            }
            toRemove.applyIntPropertyValue(UProperty.SCRIPT, script);
            current.removeAll(toRemove);
            metadataScripts.remove(shortName);
        }
        metadataScripts
            .removeAll(Arrays.asList("Hans", "Hant", "Hanb", "Jamo", "Jpan", "Kore")); // remove
        // "combo"
        // scripts
        if (!metadataScripts.isEmpty()) {
            // Warning, not error, so that we can add scripts to the script metadata
            // and later update to the Unicode version that has characters for those scripts.
            warnln("Script Metadata for characters not in Unicode: "
                + metadataScripts);
        }
    }

    public void TestMissingInfoForLanguage() {
        CLDRFile english = CLDRConfig.getInstance().getEnglish();

        for (String language : CLDRConfig.getInstance().getCldrFactory()
            .getAvailableLanguages()) {
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
        CLDRFile english = CLDRConfig.getInstance().getEnglish();

        for (String region : StandardCodes.make().getGoodAvailableCodes(
            "territory")) {
            String likelyExpansion = likely.get("und_" + region);
            if (likelyExpansion == null) {
                if (region.equals("ZZ") || region.equals("001") || region.equals("UN")
                    || SUPPLEMENTAL_DATA_INFO.getContained(region) == null) { // not
                    // container
                    String likelyTag = LikelySubtags.maximize("und_" + region,
                        likely);
                    if (likelyTag == null || !likelyTag.startsWith("en_Latn_")) {
                        errln("Missing likely subtags for region: " + region
                            + "\t" + english.getName("territory", region));
                    }
                } else { // container
                    errln("Missing likely subtags for macroregion (fix to exclude regions having 'en'): "
                        + region
                        + "\t"
                        + english.getName("territory", region));
                }
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
        VersionInfo icuUnicodeVersion = UCharacter.getUnicodeVersion();
        TreeSet<String> sorted = new TreeSet<String>(
            ScriptMetadata.getScripts());
        Set<String> exceptions2 = new HashSet<String>(
            Arrays.asList("zh_Hans_CN"));
        for (String script : sorted) {
            if (exceptions.contains(script) || script.equals("Latn")
                || script.equals("Dsrt")) {
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
                String msg = "Missing likely language for script (und_" + script
                    + ")  should be something like:\t "
                    + showOverride(script, originCountry, langScript);
                if (i.age.compareTo(icuUnicodeVersion) <= 0) {
                    // Error: Missing data for a script in ICU's Unicode version.
                    errln(msg);
                } else {
                    // Warning: Missing data for a script in a future Unicode version.
                    warnln(msg);
                }
            } else if (!exceptions2.contains(likelyExpansion)
                && !likelyExpansion.startsWith(langScript)) {
                // if
                // (logKnownIssue("Cldrbug:7181","Missing script metadata for "
                // + script)
                // && (script.equals("Tfng") || script.equals("Brah"))) {
                // logln("Wrong likely language for script (und_" + script +
                // "). Should not be " + likelyExpansion
                // + ", but something like:\t " + showOverride(script,
                // originCountry, langScript));
                // } else {
                errln("Wrong likely language for script (und_" + script
                    + "). Should not be " + likelyExpansion
                    + ", but something like:\t "
                    + showOverride(script, originCountry, langScript));
                // }
            } else {
                logln("OK: " + undScript + " => " + likelyExpansion);
            }
        }
        /**
         * und_Bopo => zh_Bopo_TW und_Copt => cop_Copt_EG // fix 002 und_Dsrt =>
         * en_Dsrt_US // fix US
         */
    }

    public String showOverride(String script, String originCountry,
        String langScript) {
        return "{\"und_" + script + "\", \"" + langScript + originCountry
            + "\"},";
    }
}
