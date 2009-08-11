package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class ScriptPopulations {
    static SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    static Map<String, String> likelySubtags = supplementalDataInfo.getLikelySubtags();
    static Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    static CLDRFile english = cldrFactory.make("en", true);


    public static void main(String[] args) {
        // iterate through the language populations, picking up the script
        SupplementalDataInfo info = supplementalDataInfo;
        LanguageTagParser languageTagParser = new LanguageTagParser();
        Counter<String> langScriptLitPop = new Counter<String>();
        Counter<String> scriptLitPop = new Counter<String>();
        Map<String, OfficialStatus> bestStatus = new HashMap<String, OfficialStatus>();
        
        for (String territory : info.getTerritoriesWithPopulationData()) {
            for (String language : info.getLanguagesForTerritoryWithPopulationData(territory)) {
                PopulationData languageInfo = info.getLanguageAndTerritoryPopulationData(language, territory);
                OfficialStatus officialStatus = languageInfo.getOfficialStatus();
                String baseLanguage = languageTagParser.set(language).getLanguage();
                String script = languageTagParser.getScript();
                if (script.length() == 0) {
                  final String maxFrom = GenerateLikelySubtagTests.maximize(language, likelySubtags);
                  if (maxFrom != null) {
                    script = languageTagParser.set(maxFrom).getScript();
                  } else {
                      script = "Zzzz";
                  }
                }
                String lang = baseLanguage + "_" + script;
                long population = (long)languageInfo.getLiteratePopulation();
                langScriptLitPop.add(lang, population);
                scriptLitPop.add(script, population);
                OfficialStatus oldStatus = bestStatus.get(lang);
                if (oldStatus == null || oldStatus.compareTo(officialStatus) < 0) {
                    bestStatus.put(lang, officialStatus);
                }
            }
        }
        for (String lang : langScriptLitPop.getKeysetSortedByCount(false)) {
            String baseLanguage = languageTagParser.set(lang).getLanguage();
            String script = languageTagParser.getScript();

            OfficialStatus officialStatus = bestStatus.get(lang);
            System.out.println(
                    baseLanguage + "\t" + script 
                    + "\t" + langScriptLitPop.getCount(lang) 
                    + "\t" + english.getName(baseLanguage) 
                    + "\t" + english.getName(CLDRFile.SCRIPT_NAME, script) 
                    + "\t" + officialStatus
                    + "\t" + officialStatus.ordinal()
                    + "\t" + scriptLitPop.getCount(script)
                    );
        }
    }
}
