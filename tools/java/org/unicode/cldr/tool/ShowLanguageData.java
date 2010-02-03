package org.unicode.cldr.tool;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.SupplementalData;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class ShowLanguageData {
  
  static SupplementalDataInfo data = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
  static CLDRFile english = CLDRFile.make("en", CldrUtility.MAIN_DIRECTORY, true);
  
  public static void main(String[] args) {
    
    Map<String,Counter<String>> map = new TreeMap<String,Counter<String>>();
    
    for (String language : data.getLanguagesForTerritoriesPopulationData()) {
      if (language.equals("und")) {
        continue;
      }
      for (String territory : data.getTerritoriesForPopulationData(language)) {
        Counter<String> langCounter = map.get(territory);
        if (langCounter == null) {
          map.put(territory, langCounter = new Counter<String>());
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
                + "\t" + territory + "\t" + english.getName(english.TERRITORY_NAME,territory) 
                + "\t" + litPop/(double)total);
      }
    }
  }
}
