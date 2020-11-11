package org.unicode.cldr.tool;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class ShowLanguageData {

    static SupplementalDataInfo data = SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    static CLDRFile english = SimpleFactory.makeFile("en", CLDRPaths.MAIN_DIRECTORY, true);

    public static void main(String[] args) {

        Map<String, Counter<String>> map = new TreeMap<>();

        for (String language : data.getLanguagesForTerritoriesPopulationData()) {
            if (language.equals("und")) {
                continue;
            }
            for (String territory : data.getTerritoriesForPopulationData(language)) {
                Counter<String> langCounter = map.get(territory);
                if (langCounter == null) {
                    map.put(territory, langCounter = new Counter<>());
                }
                PopulationData popData = data.getLanguageAndTerritoryPopulationData(language, territory);
                OfficialStatus status = popData.getOfficialStatus();
                if (!status.isMajor()) {
                    continue;
                }
                long litPop = (long) popData.getLiteratePopulation();
                langCounter.add(language, litPop);
            }
        }
        for (String territory : map.keySet()) {
            Counter<String> langCounter = map.get(territory);
            long total = langCounter.getTotal();
            if (total == 0) {
                continue;
            }
            for (String language : langCounter.getKeysetSortedByCount(false)) {
                long litPop = langCounter.getCount(language);
                System.out.println(language + "\t" + english.getName(language)
                    + "\t" + territory + "\t" + english.getName(CLDRFile.TERRITORY_NAME, territory)
                    + "\t" + litPop / (double) total);
            }
        }
    }
}
