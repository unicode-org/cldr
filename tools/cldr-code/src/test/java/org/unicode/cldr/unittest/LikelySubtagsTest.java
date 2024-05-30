package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.ScriptToExemplars;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

public class LikelySubtagsTest extends TestFmwk {

    private static final Validity VALIDITY = Validity.getInstance();
    private boolean DEBUG = false;
    private static boolean SHOW_EXEMPLARS = System.getProperty("SHOW_EXEMPLARS") != null;
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            CLDR_CONFIG.getSupplementalDataInfo();
    static final Map<String, String> likely = SUPPLEMENTAL_DATA_INFO.getLikelySubtags();
    static final LikelySubtags LIKELY = new LikelySubtags();

    public static void main(String[] args) {
        new LikelySubtagsTest().run(args);
    }

    static class Tags {
        final Set<String> languages = new TreeSet<>();
        final Set<String> scripts = new TreeSet<>();
        final Set<String> regions = new TreeSet<>();
        final Set<String> scriptRegion = new TreeSet<>();
        final Set<String> languageScript = new TreeSet<>();
        final Set<String> languageRegion = new TreeSet<>();
        final Set<String> all = new TreeSet<>();
        final ChainedMap.M4<String, String, String, Boolean> languageToScriptToRegions =
                ChainedMap.of(
                        new TreeMap<String, Object>(),
                        new TreeMap<String, Object>(),
                        new TreeMap<String, Object>(),
                        Boolean.class);
        final ChainedMap.M3<String, String, Boolean> languageToRegions =
                ChainedMap.of(
                        new TreeMap<String, Object>(),
                        new TreeMap<String, Object>(),
                        Boolean.class);

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
                M3<String, String, Boolean> scriptToRegion = languageToScriptToRegions.get(lang);
                final Set<String> scriptsFor = scriptToRegion.keySet();
                final Set<String> regionsFor = languageToRegions.get(lang).keySet();

                String firstScriptNotIn = getNonEmptyNotIn(scripts, scriptsFor);
                String firstRegionNotIn = getNonEmptyNotIn(regions, regionsFor);

                languageToScriptToRegions.put(
                        lang, firstScriptNotIn, firstRegionNotIn, Boolean.TRUE);
                // clone for safety before iterating
                for (String script : new HashSet<>(scriptsFor)) {
                    languageToScriptToRegions.put(lang, script, firstRegionNotIn, Boolean.TRUE);
                }
                for (String region : new HashSet<>(regionsFor)) {
                    languageToScriptToRegions.put(lang, firstScriptNotIn, region, Boolean.TRUE);
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
            languageToScriptToRegions.put(sourceLanguage, sourceScript, sourceRegion, Boolean.TRUE);
            languageToScriptToRegions.put(sourceLanguage, sourceScript, "", Boolean.TRUE);
            languageToScriptToRegions.put(sourceLanguage, "", "", Boolean.TRUE);
            languageToRegions.put(sourceLanguage, "", Boolean.TRUE);
            if (StandardCodes.isCountry(sourceRegion)) {
                languageToScriptToRegions.put(sourceLanguage, "", sourceRegion, Boolean.TRUE);
                languageToRegions.put(sourceLanguage, sourceRegion, Boolean.TRUE);
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
        if (!sourceLtp.getRegion().isEmpty() && !StandardCodes.isCountry(sourceLtp.getRegion())) {
            return true;
        }
        maxLtp.set(max);
        for (int i = 1; i < 8; ++i) {
            if ((i & 1) != 0) {
                if (!sourceLtp.getLanguage().equals("und")) continue;
                sourceLtp.setLanguage(maxLtp.getLanguage());
            }
            if ((i & 2) != 0) {
                if (!sourceLtp.getScript().isEmpty()) continue;
                sourceLtp.setScript(maxLtp.getScript());
            }
            if ((i & 4) != 0) {
                if (!sourceLtp.getRegion().isEmpty()) continue;
                sourceLtp.setRegion(maxLtp.getRegion());
            }
            String test = sourceLtp.toString();
            final String maximize = LIKELY.maximize(test);
            if (!max.equals(maximize)) {
                // max(source) = max, max(test) ≠ max
                if (!assertEquals(
                        String.format(
                                "checkAdding: max(%s)->%s, however max(%s)->", source, max, test),
                        max,
                        maximize)) {
                    // LIKELY.maximize(test); // Could step into this for debugging.
                }
            }
            sourceLtp.set(source); // restore
        }
        return true;
    }

    public void TestCompleteness() {
        final LanguageTagParser ltp = new LanguageTagParser();
        if (DEBUG) {
            System.out.println(TAGS.languages.size() + "\t" + TAGS.languages);
            System.out.println(TAGS.scripts.size() + "\t" + TAGS.scripts);
            System.out.println(TAGS.regions.size() + "\t" + TAGS.regions);
        }
        main:
        for (Entry<String, Map<String, Map<String, Boolean>>> languageScriptRegion :
                TAGS.languageToScriptToRegions) {
            String language = languageScriptRegion.getKey();
            ltp.set(language); // clears script, region
            for (Entry<String, Map<String, Boolean>> scriptRegion :
                    languageScriptRegion.getValue().entrySet()) {
                String script = scriptRegion.getKey();
                ltp.setScript(script);
                for (String region : scriptRegion.getValue().keySet()) {
                    ltp.setRegion(region);
                    String testTag = ltp.toString();
                    // System.out.println(testTag);
                    if (!testTag.equals("und") && !checkAdding(testTag)) {
                        checkAdding(testTag); // for debugging
                        continue main;
                    }
                }
            }
        }
    }

    static Set<String> exceptions =
            new HashSet<>(
                    Arrays.asList(
                            "Zyyy", "Zinh", "Zzzz", "Brai",
                            "Cpmn")); // scripts with no default language

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
        TreeSet<String> metadataScripts = new TreeSet<>(ScriptMetadata.getScripts());
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
        metadataScripts.removeAll(
                Arrays.asList("Hans", "Hant", "Hanb", "Jamo", "Jpan", "Kore")); // remove
        // "combo"
        // scripts
        if (!metadataScripts.isEmpty()) {
            // Warning, not error, so that we can add scripts to the script metadata
            // and later update to the Unicode version that has characters for those scripts.
            warnln("Script Metadata for characters not in Unicode: " + metadataScripts);
        }
    }

    public void TestMissingInfoForLanguage() {
        CLDRFile english = CLDR_CONFIG.getEnglish().getUnresolved();

        CalculatedCoverageLevels ccl = CalculatedCoverageLevels.getInstance();

        for (String language : CLDR_CONFIG.getCldrFactory().getAvailableLanguages()) {
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
                Level covLevel = ccl.getEffectiveCoverageLevel(language);
                if (covLevel == null || !covLevel.isAtLeast(Level.BASIC)) {
                    // https://unicode-org.atlassian.net/browse/CLDR-15663
                    if (logKnownIssue(
                            "CLDR-15663",
                            "English translation should not be required for sub-basic language name")) {
                        continue; // skip error
                    }
                }
                errln("Missing English translation for: " + language + " which is at " + covLevel);
            }
        }
    }

    public void TestMissingInfoForRegion() {
        CLDRFile english = CLDR_CONFIG.getEnglish();

        for (String region : StandardCodes.make().getGoodAvailableCodes("territory")) {
            String likelyExpansion = likely.get("und_" + region);
            if (likelyExpansion == null) {
                if (SUPPLEMENTAL_DATA_INFO.getContained(region) == null) { // not
                    // container
                    String likelyTag = LikelySubtags.maximize("und_" + region, likely);
                    if (likelyTag == null) { //  || !likelyTag.startsWith("en_Latn_")
                        logln(
                                "Missing likely subtags for region: "
                                        + region
                                        + "\t"
                                        + english.getName("territory", region));
                    }
                } else { // container
                    logln(
                            "Missing likely subtags for macroregion (fix to exclude regions having 'en'): "
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

    // typically historical script that don't need to  be in likely subtags

    static final Set<String> KNOWN_SCRIPTS_WITHOUT_LIKELY_SUBTAGS =
            ImmutableSet.of("Hatr", "Cpmn", "Ougr");

    public void TestMissingInfoForScript() {
        VersionInfo icuUnicodeVersion = UCharacter.getUnicodeVersion();
        TreeSet<String> sorted = new TreeSet<>(ScriptMetadata.getScripts());
        Set<String> exceptions2 =
                new HashSet<>(
                        Arrays.asList("zh_Hans_CN", "hnj_Hmnp_US", "hnj_Hmng_LA", "iu_Cans_CA"));
        for (String script : sorted) {
            if (exceptions.contains(script) || script.equals("Latn") || script.equals("Dsrt")) {
                // we minimize away und_X, when the code puts in en...US
                continue;
            }
            // Temporary exception for CLDR 46 Unicode 16 (CLDR-17226) because
            // GenerateMaximalLocales is currently not usable.
            if (script.equals("Aghb")) {
                // The script metadata for Aghb=Caucasian_Albanian changed
                // the likely region from Russia to Azerbaijan, and
                // the likely language from udi=Udi to xag=Old Udi.
                // Error: likelySubtags.xml has wrong language for script (und_Aghb).
                // Should not be udi_Aghb_RU, but Script Metadata suggests something like:
                // {"und_Aghb", "xag_Aghb_AZ"},
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
                if (!KNOWN_SCRIPTS_WITHOUT_LIKELY_SUBTAGS.contains(script)) {
                    String msg =
                            "likelySubtags.xml missing language for script (und_"
                                    + script
                                    + "). Script Metadata suggests that it should be something like:\t "
                                    + showOverride(script, originCountry, langScript);
                    if (i.age.compareTo(icuUnicodeVersion) <= 0) {
                        // Error: Missing data for a script in ICU's Unicode version.
                        errln(msg);
                    } else {
                        // Warning: Missing data for a script in a future Unicode version.
                        warnln(msg);
                    }
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
                errln(
                        "likelySubtags.xml has wrong language for script (und_"
                                + script
                                + "). Should not be "
                                + likelyExpansion
                                + ", but Script Metadata suggests something like:\t "
                                + showOverride(script, originCountry, langScript));
                // }
            } else {
                logln("OK: " + undScript + " => " + likelyExpansion);
            }
        }
        /**
         * und_Bopo => zh_Bopo_TW und_Copt => cop_Copt_EG // fix 002 und_Dsrt => en_Dsrt_US // fix
         * US
         */
    }

    public String showOverride(String script, String originCountry, String langScript) {
        return "{\"und_" + script + "\", \"" + langScript + originCountry + "\"},";
    }

    /**
     * Test two issues:
     *
     * <ul>
     *   <li>That the script of the locale's examplars matches the script derived from the locale's
     *       identifier.
     *   <li>That the union of the exemplar sets (main+aux) for all locales with the script matches
     *       what is in ltp.getResolvedScript()
     * </ul>
     *
     * Written as one test, to avoid the overhead of iterating over all locales twice.
     */
    public void testGetResolvedScriptVsExemplars() {
        Factory factory = CLDR_CONFIG.getCldrFactory();
        LanguageTagParser ltp = new LanguageTagParser();
        Multimap<String, UnicodeSet> scriptToMains = TreeMultimap.create();
        Multimap<String, UnicodeSet> scriptToAuxes = TreeMultimap.create();
        UnicodeSet collectedBad = new UnicodeSet();
        for (String locale : factory.getAvailable()) {
            if ("root".equals(locale)) {
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);
            UnicodeSet main = cldrFile.getRawExemplarSet(ExemplarType.main, WinningChoice.WINNING);
            main = checkSet("main", locale, main, collectedBad);
            UnicodeSet aux =
                    cldrFile.getRawExemplarSet(ExemplarType.auxiliary, WinningChoice.WINNING);
            aux = checkSet("aux", locale, aux, collectedBad);
            String script = null;
            int uScript = 0;
            for (String s : main) {
                uScript = UScript.getScript(s.codePointAt(0));
                if (uScript > UScript.INHERITED) {
                    script = UScript.getShortName(uScript);
                    break;
                }
            }
            if (script == null) {
                errln("No script for " + locale);
                continue;
            }
            String ltpScript = ltp.set(locale).getResolvedScript();
            switch (uScript) {
                case UScript.HAN:
                    switch (ltp.getLanguage()) {
                        case "ja":
                            script = "Jpan";
                            break;
                        case "yue":
                            script = ltp.getScript();
                            if (script.isEmpty()) {
                                script = "Hant";
                            }
                            break;
                        case "zh":
                            script = ltp.getScript();
                            if (script.isEmpty()) {
                                script = "Hans";
                            }
                            break;
                    }
                    break;
                case UScript.HANGUL:
                    switch (ltp.getLanguage()) {
                        case "ko":
                            script = "Kore";
                            break;
                    }
            }
            if (!assertEquals(locale, script, ltpScript)) {
                ltp.getResolvedScript(); // for debugging
            }
            scriptToMains.put(ltpScript, main.freeze());
            if (!aux.isEmpty()) {
                scriptToAuxes.put(ltpScript, aux.freeze());
            }
        }

        if (!collectedBad.isEmpty()) {
            warnln(
                    "Locales have "
                            + collectedBad.size()
                            + " unexpected characters in main and/or aux:\t"
                            + collectedBad.toPattern(false)
                            + "\n Use -DSHOW_EXEMPLARS for details");
        }

        // now check that ScriptToExemplars.getExemplars matches the data

        Set<String> problemScripts = new LinkedHashSet<>();
        Map<String, UnicodeSet> expected = new TreeMap<>();
        for (Entry<String, Collection<UnicodeSet>> entry : scriptToMains.asMap().entrySet()) {
            String script = entry.getKey();
            Collection<UnicodeSet> mains = entry.getValue();
            Collection<UnicodeSet> auxes = scriptToAuxes.get(script);

            UnicodeSet flattened;
            if (mains.size() <= 1 && auxes.size() <= 1) {
                continue;
            } else {
                UnicodeMap<Integer> counts = new UnicodeMap<>();
                getCounts(mains, counts);
                flattened = getUncommon(counts, mains.size());
                if (counts.size() < 32) {
                    getCounts(auxes, counts);
                    flattened = getUncommon(counts, mains.size());
                }
            }
            expected.put(script, flattened.freeze());
        }
        for (Entry<String, UnicodeSet> entry : expected.entrySet()) {
            String script = entry.getKey();
            UnicodeSet flattened = entry.getValue();

            // now compare to what we get from the cached file, to make sure the latter is up to
            // date

            if (!assertEquals(
                    script,
                    flattened.toPattern(false),
                    ScriptToExemplars.getExemplars(script).toPattern(false))) {
                problemScripts.add(script);
            }
        }

        if (!problemScripts.isEmpty()) {
            warnln(
                    "Adjust the data in scriptToExemplars.txt. Use -DSHOW_EXEMPLARS to update, or reset to expected value for: "
                            + problemScripts);
            if (SHOW_EXEMPLARS) {
                ScriptToExemplars.write(expected);
            }
        }
    }

    static final UnicodeSet MAIN_AUX_EXPECTED = new UnicodeSet("[\\p{L}\\p{M}\\p{Cf}·]").freeze();

    private UnicodeSet checkSet(
            String title, String locale, UnicodeSet main, UnicodeSet collected) {
        UnicodeSet bad = new UnicodeSet();
        for (String s : main) {
            if (!MAIN_AUX_EXPECTED.containsAll(s)) {
                bad.add(s);
            }
        }
        if (!bad.isEmpty()) {
            if (SHOW_EXEMPLARS) {
                warnln(
                        "\t"
                                + title
                                + "\tLocale\t"
                                + locale
                                + "\thas "
                                + bad.size()
                                + " unexpected exemplar characters:\t"
                                + bad.toPattern(false));
            }
            collected.addAll(bad);
        }
        return CldrUtility.flatten(new UnicodeSet(main).removeAll(bad));
    }

    /**
     * Remove items with a count equal to size (they are common to all locales), and flatten
     * (against the whole set)
     */
    private UnicodeSet getUncommon(UnicodeMap<Integer> counts, int size) {
        UnicodeSet flattenedAll =
                CldrUtility.flatten(counts.keySet()); // we flatten against the whole set
        UnicodeSet result = new UnicodeSet();
        for (String s : flattenedAll) {
            int count = counts.get(s);
            if (count != size) {
                result.add(s);
            }
        }
        return result.freeze();
    }

    private void getCounts(Collection<UnicodeSet> usets, UnicodeMap<Integer> counts) {
        for (UnicodeSet uset : usets) {
            for (String s : uset) {
                Integer old = counts.get(s);
                if (old == null) {
                    counts.put(s, 1);
                } else {
                    counts.put(s, old + 1);
                }
            }
        }
    }

    public void testUndAllScriptsAndRegions() {
        Set<String> regions = new TreeSet<>();
        Set<String> scripts = new TreeSet<>();
        Set<String> regularCountries =
                VALIDITY.getStatusToCodes(LstrType.region).get(Status.regular);
        Set<String> macroRegions =
                Set
                        .of(); // Validity.getInstance().getStatusToCodes(LstrType.region).get(Status.macroregion);

        for (String country : Sets.union(regularCountries, macroRegions)) {
            regions.add(country);
        }

        // for Scripts, just test the ones in CLDR
        for (String localeString : CLDR_CONFIG.getCldrFactory().getAvailable()) {
            if (localeString.equals("root")) {
                continue;
            }
            CLDRLocale cLocale = CLDRLocale.getInstance(localeString);
            final String script = cLocale.getScript();
            if (script.equals("Dsrt")) {
                continue; // toy script
            }
            final String country = cLocale.getCountry();
            if (!country.isEmpty() && !country.equals("001")) {
                regions.add(country);
            }
            if (!script.isEmpty()) {
                scripts.add(script);
                //                if (!country.isEmpty()) {
                //                    // we only need this if the value from script + country is
                // different from the value of script
                //                    combinations.add("und_" + script + "_" + country);
                //                }
            }
        }
        for (String script : scripts) {
            if (script.equals("Latn")) {
                assertTrue("contains und_" + script, likely.containsKey("und"));
            } else if (!assertTrue("contains und_" + script, likely.containsKey("und_" + script))) {

            }
        }
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> possibleFixes = new TreeSet<>();
        for (String region : regions) {
            final String undRegion = "und_" + region;
            if (region.equals("150") && likely.containsKey("und")) {
                // skip
            } else if (!assertTrue("contains und_" + region, likely.containsKey(undRegion))) {
                Set<String> languages =
                        SUPPLEMENTAL_DATA_INFO.getLanguagesForTerritoryWithPopulationData(region);
                double biggest = -1;
                String biggestLang = null;
                for (String language : languages) {
                    PopulationData popData =
                            SUPPLEMENTAL_DATA_INFO.getLanguageAndTerritoryPopulationData(
                                    language, region);
                    if (popData.getLiteratePopulation() > biggest) {
                        biggest = popData.getLiteratePopulation();
                        biggestLang = language;
                    }
                }
                if (biggestLang != null) {
                    ltp.set(biggestLang);
                    if (ltp.getScript().isEmpty()) {
                        String biggestMax = likely.get(biggestLang);
                        ltp.set(biggestMax);
                    }
                    ltp.setRegion(region);
                    possibleFixes.add(
                            "<likelySubtag from=\"" + undRegion + "\" to=\"" + ltp + "\"/>");
                }
            }
        }
        System.out.println("\t\t" + Joiner.on("\n\t\t").join(possibleFixes));
    }

    public void testToAttributeValidityStatus() {
        Set<String> okLanguages = VALIDITY.getStatusToCodes(LstrType.language).get(Status.regular);
        Set<String> okScripts = VALIDITY.getStatusToCodes(LstrType.script).get(Status.regular);
        Set<String> okRegions = VALIDITY.getStatusToCodes(LstrType.region).get(Status.regular);
        Multimap<String, String> badFieldsToLocales = TreeMultimap.create();
        Set<String> knownExceptions = Set.of("in", "iw", "ji", "jw", "mo", "tl");
        for (String s : likely.values()) {
            CLDRLocale cLocale = CLDRLocale.getInstance(s);
            final String language = cLocale.getLanguage();
            final String script = cLocale.getScript();
            final String region = cLocale.getCountry();
            if (!okLanguages.contains(language)) {
                if (knownExceptions.contains(language)) {
                    continue;
                }
                badFieldsToLocales.put(language, s);
            }
            if (!okScripts.contains(script)) {
                badFieldsToLocales.put(script, s);
            }
            if (!okRegions.contains(region)) {
                badFieldsToLocales.put(region, s);
            }
        }
        if (!badFieldsToLocales.isEmpty()) {
            Multimap<Status, String> statusToExamples = TreeMultimap.create();
            for (String field : badFieldsToLocales.keySet()) {
                Status status = VALIDITY.getCodeToStatus(LstrType.language).get(field);
                if (status == null) {
                    status = VALIDITY.getCodeToStatus(LstrType.script).get(field);
                }
                if (status == null) {
                    status = VALIDITY.getCodeToStatus(LstrType.region).get(field);
                }
                statusToExamples.put(status, field);
            }
            Map<String, String> fieldToOrigin = new TreeMap<>();
            for (Entry<Status, Collection<String>> entry : statusToExamples.asMap().entrySet()) {
                //                for (String value : entry.getValue()) {
                //                    String origin =
                // SUPPLEMENTAL_DATA_INFO.getLikelyOrigins().get(value);
                //                    fieldToOrigin.put(value, origin == null ? "n/a" : origin);
                //                }
                warnln("Bad status=" + entry.getKey() + " for " + entry.getValue());
            }
        }
    }

    /**
     * Test whether any of the mapping lines in likelySubtags.xml are superfluous. <br>
     * For example, with the following mappings, #2 and #3 are superfluous, since they would be
     * produced by the algorithm anyway.
     *
     * <ol>
     *   <li>ll => ll_Sss1_R1
     *   <li>ll_Sss2 => ll_Sss2_RR
     *   <li>ll_R2 => ll_Ssss_R2
     * </ol>
     *
     * On the other hand, the following are not:
     *
     * <ol>
     *   <li>ll_Sss2 => ll_Sss2_R3
     *   <li>ll_R2 => ll_Sss3_R2
     * </ol>
     */
    public void testSuperfluous() {
        Map<String, String> origins = SUPPLEMENTAL_DATA_INFO.getLikelyOrigins();

        // collect all items with same language
        LanguageTagParser ltp = new LanguageTagParser();
        TreeMap<String, TreeMap<String, String>> langToLikelySubset = new TreeMap<>();
        for (Entry<String, String> entry : likely.entrySet()) {
            String lang = ltp.set(entry.getKey()).getLanguage();
            if (lang.equals("und")) {
                continue;
            }
            TreeMap<String, String> subtree = langToLikelySubset.get(lang);
            if (subtree == null) {
                langToLikelySubset.put(lang, subtree = new TreeMap<>());
            }
            subtree.put(entry.getKey(), entry.getValue());
        }
        boolean first = true;

        for (Entry<String, TreeMap<String, String>> langAndMap : langToLikelySubset.entrySet()) {
            String lang0 = langAndMap.getKey();
            Map<String, String> goldenMap = ImmutableMap.copyOf(langAndMap.getValue());
            if (goldenMap.size() == 1) {
                continue;
            }

            // get test sets and build probe data

            Set<String> scripts = new TreeSet<>();
            scripts.add("Egyp");
            scripts.add("");
            Set<String> regions = new TreeSet<>();
            regions.add("AQ");
            regions.add("");
            for (String key : Sets.union(goldenMap.keySet(), new TreeSet<>(goldenMap.values()))) {
                scripts.add(ltp.set(key).getScript());
                regions.add(ltp.getRegion());
            }
            scripts = ImmutableSet.copyOf(scripts);
            regions = ImmutableSet.copyOf(regions);

            TreeSet<String> probeData = new TreeSet<>();
            ltp.setLanguage(lang0); // clear;
            for (String script : scripts) {
                ltp.setScript(script); // clear;
                for (String region : regions) {
                    ltp.setRegion(region);
                    probeData.add(ltp.toString());
                }
            }

            // see if the omission of a <key,value> makes no difference

            String omittableKey = null;

            for (String keyToTryOmitting : goldenMap.keySet()) {
                if (!keyToTryOmitting.contains("_")) {
                    continue;
                }
                TreeMap<String, String> mapWithOmittedKey = new TreeMap<>(goldenMap);
                mapWithOmittedKey.remove(keyToTryOmitting);

                boolean makesADifference = false;
                for (String probe : probeData) {
                    String expected = LikelySubtags.maximize(probe, goldenMap);
                    String actual = LikelySubtags.maximize(probe, mapWithOmittedKey);
                    if (!Objects.equal(expected, actual)) {
                        makesADifference = true;
                        break;
                    }
                }
                if (!makesADifference) {
                    omittableKey = keyToTryOmitting;
                    break;
                }
            }

            // show the value that doesn't make a difference
            // NOTE: there may be more than one, but it is sufficient to find one.
            if (omittableKey != null) {
                final String origin = origins.get(omittableKey);
                if (origin != null) { // only check the non-sil for now
                    logKnownIssue("CLDR-17084", "Remove superfluous lines in likelySubtags.txt");
                    continue;
                }
                if (first) {
                    warnln("\tMaps\tKey to omit\tvalue\torigin");
                    first = false;
                }
                assertFalse(
                        "\t"
                                + goldenMap
                                + "\t"
                                + omittableKey
                                + "\t"
                                + goldenMap.get(omittableKey)
                                + "\t"
                                + (origin == null ? "" : origin)
                                + "\t",
                        true);
            }
        }
    }
}
