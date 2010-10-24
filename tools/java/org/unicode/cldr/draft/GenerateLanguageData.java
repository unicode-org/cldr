package org.unicode.cldr.draft;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Row;
import com.ibm.icu.text.NumberFormat;

public class GenerateLanguageData {

    SupplementalDataInfo info = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");

    CLDRFile english = cldrFactory.make("en", true);
    CLDRFile root = cldrFactory.make("root", true);
    StandardCodes sc = StandardCodes.make();
    NumberFormat nf = NumberFormat.getInstance();
    NumberFormat pf = NumberFormat.getPercentInstance();

    public static void main(String[] args) {
        new GenerateLanguageData().run();
    }

    private void run() {
        Counter2<String> langToPopulation = new Counter2();
        Counter2<String> langToGDP = new Counter2();
        LanguageTagParser ltp = new LanguageTagParser();
        Map<String,String> languageNameToCode = new TreeMap();
        for (String languageCode :  info.getLanguages()) {
            languageNameToCode.put(english.getName(languageCode), languageCode);
        }
        
        for (String languageName : languageNameToCode.keySet()) {
            String languageCode = languageNameToCode.get(languageName);
            
            String baseLanguage = languageCode;
            ltp.set(languageCode);
            String script = ltp.getScript();
            if (script.length() != 0 && !script.startsWith("Han")) {
                baseLanguage = ltp.getLanguage();
            }
            Set<String> territories = info.getTerritoriesForPopulationData(languageCode);
            if (territories == null) continue;
            for (String territory : territories) {
                PopulationData terrData = info.getPopulationDataForTerritory(territory);
                String territoryName = english.getName(english.TERRITORY_NAME, territory);

                PopulationData data = info.getLanguageAndTerritoryPopulationData(languageCode, territory);
                double literatePopulationLangRegion = data.getLiteratePopulation();
                double pop = literatePopulationLangRegion;
                langToPopulation.add(baseLanguage, pop);

                double populationRegion = terrData.getPopulation();
                double literatePopulationRegion = terrData.getLiteratePopulation();
                double factor = literatePopulationLangRegion/literatePopulationRegion;
                System.out.println(languageName
                        + "\t" + languageCode
                        + "\t" + territoryName
                        + "\t" + territory
                        + "\t" + nf.format(populationRegion)
                        + "\t" + nf.format(literatePopulationRegion)
                        + "\t" + nf.format(literatePopulationLangRegion)
                        + "\t" + pf.format(factor)
                        );
//                double gdp = terrData.getGdp() * factor;
//                if (!Double.isNaN(gdp)) {
//                    langToGDP.add(baseLanguage, gdp);
//                }
            }
        }
        if (false) for (String language :langToPopulation.keySet()) {
            System.out.println(
                    english.getName(language) 
                    + "\t" + language 
                  
                    + "\t" + langToPopulation.getCount(language)
                    + "\t" + langToGDP.getCount(language)
                    );
        }
    }
}
