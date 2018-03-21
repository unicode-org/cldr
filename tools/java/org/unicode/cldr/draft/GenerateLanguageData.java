package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.NumberFormat;

public class GenerateLanguageData {

    SupplementalDataInfo info = SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
    Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");

    CLDRFile english = cldrFactory.make("en", true);
    CLDRFile root = cldrFactory.make("root", true);
    StandardCodes sc = StandardCodes.make();
    NumberFormat nf = NumberFormat.getInstance();
    NumberFormat pf = NumberFormat.getPercentInstance();

    public static void main(String[] args) throws IOException {
        new GenerateLanguageData().run();
    }

    private void run() throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "langData/", "generatedLanguageData.txt")) {
            Counter2<String> langToPopulation = new Counter2<String>();
            // Counter2<String> langToGDP = new Counter2<String>();
            LanguageTagParser ltp = new LanguageTagParser();
            Map<String, String> languageNameToCode = new TreeMap<String, String>();
            for (String languageCode : info.getLanguages()) {
                languageNameToCode.put(english.getName(languageCode), languageCode);
            }
            out.println("\n@sheet:CLDR County Data");
            out.println("code\tgdp\tlit-pop\tpopulation\tliteracy");

            for (String territory : info.getTerritoriesWithPopulationData()) {
                PopulationData terrData = info.getPopulationDataForTerritory(territory);
                out.println(territory
                    + "\t" + terrData.getGdp()
                    + "\t" + terrData.getLiteratePopulation()
                    + "\t" + terrData.getPopulation()
                    + "\t" + (terrData.getLiteratePopulationPercent() / 100));
            }

            out.flush();
            out.println("\n@sheet:CLDR Language Data");
            out.println("LC\tName\tCC\tName\tStatus\tLitPop");

            Map<String, Counter2<String>> langToCountriesOfficial = new TreeMap<>();

            for (String languageCode : info.getLanguages()) {
                String languageName = english.getName(languageCode);

                String baseLanguage = languageCode;
                ltp.set(languageCode);
                String script = ltp.getScript();
                if (script.length() != 0 && !script.startsWith("Han")) {
                    baseLanguage = ltp.getLanguage();
                }

                Set<String> territories = info.getTerritoriesForPopulationData(languageCode);
                if (territories == null) continue;
//            out.println("langName\tcode\tregionName\tcode\tregionPop"
//                + "\t" + "regionLitPop"
//                + "\t" + nf.format(literatePopulationLangRegion)
//                + "\t" + pf.format(factor)
//                + "\t" + pf.format(status)
//                );
                for (String territory : territories) {
                    PopulationData terrData = info.getPopulationDataForTerritory(territory);
                    String territoryName = english.getName(CLDRFile.TERRITORY_NAME, territory);

                    PopulationData data = info.getLanguageAndTerritoryPopulationData(languageCode, territory);
                    double literatePopulationLangRegion = data.getLiteratePopulation();
                    double pop = literatePopulationLangRegion;
                    langToPopulation.add(baseLanguage, pop);
                    OfficialStatus status = data.getOfficialStatus();
                    if (status.compareTo(OfficialStatus.official_minority) >= 0) {
                        Counter2<String> counter = langToCountriesOfficial.get(baseLanguage);
                        if (counter == null) {
                            langToCountriesOfficial.put(baseLanguage, counter = new Counter2<>());
                        }
                        counter.add(territory, literatePopulationLangRegion);
                    }

                    double populationRegion = terrData.getPopulation();
                    double literatePopulationRegion = terrData.getLiteratePopulation();
                    double factor = literatePopulationLangRegion / literatePopulationRegion;

                    //out.println("LC\tName\tCC\tName\tStatus\tLitPop\tblank\t%LitPop(CC)");

                    out.println(fixLang(languageCode)
                        + "\t" + languageName
                        + "\t" + territory
                        + "\t" + territoryName
                        + "\t" + status
                        + "\t" + literatePopulationLangRegion
//                        + "\t" + ""
//                        + "\t" + factor
                    );
                    // double gdp = terrData.getGdp() * factor;
                    // if (!Double.isNaN(gdp)) {
                    // langToGDP.add(baseLanguage, gdp);
                    // }
                }
            }

            out.flush();
            out.println("\n@sheet:CLDR Lang-Countries");
            out.println("LangCode\tRegionCode");
            Set<String> missing = new TreeSet<>(info.getLanguages());
            for (Entry<String, Counter2<String>> entry : langToCountriesOfficial.entrySet()) {
                Counter2<String> regions = entry.getValue();
                ArrayList<String> top = new ArrayList<>(regions.getKeysetSortedByCount(false));

                String lang = entry.getKey();
                out.println(fixLang(lang)
                    + "\t" + (top.size() < 6 ? CollectionUtilities.join(top, ", ")
                        : CollectionUtilities.join(top.subList(0, 5), ", ") + ", …"));
                missing.remove(lang);
            }
            for (String lang : missing) {
                out.println(fixLang(lang)
                    + "\tnone");
            }
            // for (String language :langToPopulation.keySet()) {
            // out.println(
            // english.getName(language)
            // + "\t" + language
            // + "\t" + langToPopulation.getCount(language)
            // + "\t" + langToGDP.getCount(language)
            // );
            // }
        }
    }

    private String fixLang(String key) {
        return key.replace('_', '-');
    }
}
