package org.unicode.cldr.unittest;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class LanguageTest extends TestFmwk {
    final CLDRConfig testInfo = CLDRConfig.getInstance();
    final SupplementalDataInfo supplementalDataInfo = testInfo
        .getSupplementalDataInfo();
    final Map<String, String> likelyMap = supplementalDataInfo
        .getLikelySubtags();
    final HashMap<String, String> language2likely = new HashMap<String, String>();
    final HashMap<String, String> script2likely = new HashMap<String, String>();
    {
        final HashMap<String, Map<Type, String>> language2script = new HashMap<String, Map<Type, String>>();
        final HashMap<String, Map<Type, String>> language2territory = new HashMap<String, Map<Type, String>>();
        final HashMap<String, Map<Type, String>> script2language = new HashMap<String, Map<Type, String>>();
        final HashMap<String, Map<Type, String>> script2territory = new HashMap<String, Map<Type, String>>();
        final HashSet<String> scriptSet = new HashSet<String>();
        for (String language : supplementalDataInfo
            .getBasicLanguageDataLanguages()) {
            for (BasicLanguageData basic : supplementalDataInfo
                .getBasicLanguageData(language)) {
                Type type = basic.getType();
                Set<String> scripts = basic.getScripts();
                String script = null;
                if (scripts != null && scripts.size() != 0) {
                    script = scripts.iterator().next();
                    addMap(language2script, language, script, type);
                    addMap(script2language, script, language, type);
                    scriptSet.add(script);
                }
                Set<String> territories = basic.getTerritories();
                if (territories != null && territories.size() != 0) {
                    String territory = territories.iterator().next();
                    addMap(language2territory, language, territory, type);
                    if (script != null) {
                        addMap(script2territory, territory, language, type);
                    }
                }
            }
        }
        for (String language : supplementalDataInfo
            .getBasicLanguageDataLanguages()) {
            String bestScript = getBest(language2script, language, "Zzzz");
            String bestTerritory = getBest(language2territory, language, "ZZ");
            language2likely.put(language, language + "_" + bestScript + "_"
                + bestTerritory);
        }
        for (String script : scriptSet) {
            String bestLanguage = getBest(script2language, script, "und");
            String bestTerritory = getBest(script2territory, script, "ZZ");
            script2likely.put(script, bestLanguage + "_" + script + "_"
                + bestTerritory);
        }
    }

    public String getBest(
        final HashMap<String, Map<Type, String>> language2script,
        String language, String defaultValue) {
        final Map<Type, String> bestMap = language2script.get(language);
        return bestMap == null ? defaultValue : bestMap.values().iterator()
            .next();
    }

    public void addMap(HashMap<String, Map<Type, String>> hashMap,
        String language, final String script, Type type) {
        Map<Type, String> old = hashMap.get(language);
        if (old == null) {
            hashMap.put(language, old = new EnumMap<Type, String>(Type.class));
        }
        if (!old.containsKey(type)) {
            old.put(type, script);
        }
    }

    public static void main(String[] args) {
        new LanguageTest().run(args);
    }

    public void TestThatLanguagesHaveScript() {
        Set<String> needTransfer = new LinkedHashSet<String>();
        LanguageTagParser parser = new LanguageTagParser();
        Map<String, Counter2<String>> scriptToLanguageCounter = new TreeMap<String, Counter2<String>>();
        for (String language : supplementalDataInfo
            .getLanguagesForTerritoriesPopulationData()) {
            String script = parser.set(language).getScript();
            String base = parser.getLanguage();
            if (script.isEmpty()) {
                String likely = likelyMap.get(language);
                if (likely != null) {
                    script = parser.set(likely).getScript();
                } else {
                    final String data = language2likely.get(base);
                    if (data == null) {
                        errln("Language without likely script:\t" + base + "\t"
                            + getLanguageName(base));
                    } else {
                        needTransfer.add(language);
                    }
                    continue;
                }
            }
            Counter2<String> c = scriptToLanguageCounter.get(script);
            if (c == null) {
                scriptToLanguageCounter.put(script, c = new Counter2<String>());
            }
            for (String territory : supplementalDataInfo
                .getTerritoriesForPopulationData(language)) {
                PopulationData data = supplementalDataInfo
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                double lit = data.getLiteratePopulation();
                c.add(base, lit);
            }
        }
        for (String language : needTransfer) {
            addLine(language, language2likely.get(language));
        }
        for (String script : scriptToLanguageCounter.keySet()) {
            Counter2<String> c = scriptToLanguageCounter.get(script);
            String biggestLanguage = c.getKeysetSortedByCount(false).iterator()
                .next();
            logln(script + "\t" + getScriptName(script) + "\t"
                + biggestLanguage + "\t" + getLanguageName(biggestLanguage)
                + "\t" + c.getCount(biggestLanguage));
        }
    }

    public void TestScriptsWithoutLanguage() {
        if (false)
            throw new IllegalArgumentException(
                "    Remove Kana => Ainu, Bopo, Latn => Afar");
        Set<String> needTransfer = new LinkedHashSet<String>();
        Set<String> unicodeScripts = getUnicodeScripts();
        for (String script : unicodeScripts) {
            String likely = likelyMap.get("und_" + script);
            if (likely == null) {
                final String data = script2likely.get(script);
                if (data == null) {
                    errln("Script without likely language:\t" + script + "\t"
                        + getScriptName(script));
                } else {
                    needTransfer.add(script);
                }
            }
        }
        for (String script : needTransfer) {
            addLine("und_" + script, script2likely.get(script));
        }
        for (String script : needTransfer) {
            final String tag = script2likely.get(script);
            LanguageTagParser parser = new LanguageTagParser().set(tag);
            String lang = parser.getLanguage();
            logln(script + "\t" + getScriptName(script) + "\t" + lang + "\t"
                + getLanguageName(lang) + "\t*");
        }
        String[][] special = { { "Hani", "zh" }, { "Hira", "ja" },
            { "Kana", "ja" }, { "Hang", "ko" }, { "Bopo", "zh" }, };
        for (String[] scriptLang : special) {
            String script = scriptLang[0];
            final String lang = scriptLang[1];
            logln(script + "\t" + getScriptName(script) + "\t" + lang + "\t"
                + getLanguageName(lang) + "\t*");
        }

    }

    /*
     * <likelySubtag from="aa" to="aa_Latn_ET"/> <!--{ Afar; ?; ? } => { Afar;
     * Latin; Ethiopia }-->
     */
    public void addLine(String input, final String result) {
        logln("Add?:\t<likelySubtag from=\"" + input + "\" to=\"" + result
            + "\"/> <!--{ " + getLocaleName(input) + " } => { "
            + getLocaleName(result) + " }-->");
    }

    private String getLocaleName(String input) {
        LanguageTagParser parser = new LanguageTagParser().set(input);
        return (parser.getLanguage().isEmpty() ? "?" : getLanguageName(parser
            .getLanguage()))
            + "; "
            + (parser.getScript().isEmpty() ? "?" : getScriptName(parser
                .getScript()))
            + "; "
            + (parser.getRegion().isEmpty() ? "?" : testInfo.getEnglish()
                .getName(CLDRFile.TERRITORY_NAME, parser.getRegion()));
    }

    Set<String> getUnicodeScripts() {
        Set<String> scripts = new TreeSet<String>();
        int min = UCharacter.getIntPropertyMinValue(UProperty.SCRIPT);
        int max = UCharacter.getIntPropertyMaxValue(UProperty.SCRIPT);
        UnicodeSet temp = new UnicodeSet();
        for (int i = min; i <= max; ++i) {
            if (i == UScript.UNKNOWN || i == UScript.COMMON
                || i == UScript.INHERITED || i == UScript.BRAILLE) {
                continue;
            }
            if (temp.applyIntPropertyValue(UProperty.SCRIPT, i).size() != 0) {
                scripts.add(UScript.getShortName(i));
            }
        }
        return scripts;
    }

    private String getScriptName(String script) {
        String name = testInfo.getEnglish().getName("script", script);
        if (name != null && !name.equals(script)) {
            return name;
        }
        return getDescription("script", script);
    }

    public String getDescription(String type, String token) {
        try {
            testInfo.getStandardCodes();
            String name = StandardCodes.getLStreg().get(type).get(token)
                .get("Description");
            int pos = name.indexOf('▪');
            return pos < 0 ? name : name.substring(0, pos);
        } catch (Exception e) {
            return token;
        }
    }

    private String getLanguageName(String language) {
        String name = testInfo.getEnglish().getName("language", language);
        if (name != null && !name.equals(language)) {
            return name;
        }
        return getDescription("language", language);
    }
}
