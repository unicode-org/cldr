package org.unicode.cldr.unittest;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class LanguageTest extends TestFmwk {
    final TestInfo testInfo = TestInfo.getInstance();
    final SupplementalDataInfo supplementalDataInfo = testInfo.getSupplementalDataInfo();
    final Map<String, String> likelyMap = supplementalDataInfo.getLikelySubtags();
    final HashMap<String,Map<Type,String>> language2scriptType = new HashMap<String,Map<Type,String>>();
    final HashMap<String,Map<Type,String>> script2languageType = new HashMap<String,Map<Type,String>>();
    {
        for (String language : supplementalDataInfo.getBasicLanguageDataLanguages()) {
            for (BasicLanguageData basic : supplementalDataInfo.getBasicLanguageData(language)) {
                Set<String> scripts = basic.getScripts();
                final String script = scripts.size() == 0 ? "Zzzz" : scripts.iterator().next();
                Set<String> territories = basic.getTerritories();
                final String territory = territories.size() == 0 ? "ZZ" : territories.iterator().next();
                Type type = basic.getType();
                final String result = language + "_" + script + "_" + territory;
                addMap(language2scriptType, language, result, type);
                addMap(script2languageType, script, result, type);
//                language2scriptType.put(language, Row.of(type, script));
//                script2languageType.put(script, Row.of(type, language));
            }
        }
    }

    public void addMap(HashMap<String, Map<Type, String>> hashMap, String language, final String script, Type type) {
        Map<Type, String> old = hashMap.get(language);
        if (old == null) {
            hashMap.put(language, old = new EnumMap<Type,String>(Type.class));
        }
        if (!old.containsKey(type)) {
            old.put(type,script);
        }
    }

    public static void main(String[] args) {
        new LanguageTest().run(args);
    }

    public void TestThatScriptsHaveLanguage() {
        Set<String> needTransfer = new LinkedHashSet<String>();
        LanguageTagParser parser = new LanguageTagParser();
        Map<String, Counter2<String>> scriptToLanguageCounter = new TreeMap();
        for (String language : supplementalDataInfo.getLanguagesForTerritoriesPopulationData()) {
            String script = parser.set(language).getScript();
            String base = parser.getLanguage();
            if (script.isEmpty()) {
                String likely = likelyMap.get(language);
                if (likely != null) {
                    script = parser.set(likely).getScript();
                } else {
                    final Map<Type, String> data = language2scriptType.get(base);
                    if (data == null) {
                        errln("Language without likely script:\t" + base + "\t" + getLanguageName(base));
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
            for (String territory : supplementalDataInfo.getTerritoriesForPopulationData(language)) {
                PopulationData data = supplementalDataInfo.getLanguageAndTerritoryPopulationData(language, territory);
                double lit = data.getLiteratePopulation();
                c.add(base, lit);
            }
        }
        for (String language : needTransfer) {
            addLine(language, language2scriptType.get(language).values().iterator().next());
        }
        for (String script : scriptToLanguageCounter.keySet()) {
            Counter2<String> c = scriptToLanguageCounter.get(script);
            String biggestLanguage = c.getKeysetSortedByCount(false).iterator().next();
            logln(script + "\t" + getScriptName(script) 
                    + "\t" + biggestLanguage + "\t" + getLanguageName(biggestLanguage) 
                    + "\t" + c.getCount(biggestLanguage));
        }
    }

    public void TestScriptsWithoutLanguage() {
        Set<String> needTransfer = new LinkedHashSet<String>();
        Set<String> unicodeScripts = getUnicodeScripts();
        for (String script : unicodeScripts) {
            String likely = likelyMap.get("und_" + script);
            if (likely == null) {
                final Map<Type, String> data = script2languageType.get(script);
                if (data == null) {
                    errln("Script without likely language:\t" + script + "\t" + getScriptName(script));
                } else {
                    needTransfer.add(script);
                }
            }
        }
        for (String script : needTransfer) {
            addLine("und_" + script, script2languageType.get(script).values().iterator().next());
        }
    }

    /*
     * <likelySubtag from="aa" to="aa_Latn_ET"/> <!--{ Afar; ?; ? } => { Afar; Latin; Ethiopia }-->
     */
    public void addLine(String input, final String result) {
        errln("Add?:\t<likelySubtag from=\"" + input + 
        		"\" to=\"" + result +
        		"\"/> <!--{ " + getLocaleName(input) +
        		" } => { " + getLocaleName(result) +
        		" }-->");
    }

    
    private String getLocaleName(String input) {
        LanguageTagParser parser = new LanguageTagParser().set(input);
        return (parser.getLanguage().isEmpty() ? "?" : getLanguageName(parser.getLanguage()))
        + "; " + (parser.getScript().isEmpty() ? "?" : getScriptName(parser.getScript()))
        + "; " + (parser.getRegion().isEmpty() ? "?" : testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, parser.getRegion()))
        ;
    }

    Set<String> getUnicodeScripts() {
        Set<String> scripts = new TreeSet<String>();
        int min = UCharacter.getIntPropertyMinValue(UProperty.SCRIPT);
        int max = UCharacter.getIntPropertyMaxValue(UProperty.SCRIPT);
        UnicodeSet temp = new UnicodeSet();
        for (int i = min; i <= max; ++i) {
            if (i == UScript.UNKNOWN || i == UScript.COMMON || i == UScript.INHERITED || i == UScript.BRAILLE) {
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
            String name = testInfo.getStandardCodes().getLStreg().get(type).get(token).get("Description");
            int pos = name.indexOf('▪');
            return pos < 0 ? name : name.substring(0,pos);
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
