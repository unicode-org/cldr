package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.LanguageData;

import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestSupplementalData {
  static CLDRFile english;
  
  public static void main(String[] args) {
    Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
    english = cldrFactory.make("en", true);
    checkAgainstLanguageScript();
  }
  
  static void checkAgainstLanguageScript() {
    StandardCodes sc = StandardCodes.make();
    SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
    Relation<String,String> otherTerritoryToLanguages = new Relation(new TreeMap(), TreeSet.class, null);
    // get other language data
    for (String language : sc.getGoodAvailableCodes("language")) {
      Set<LanguageData> newLanguageData = supplementalData.getLanguageData(language);
      if (newLanguageData != null) {
        for (LanguageData languageData : newLanguageData) {
        Set<String> territories = new TreeSet(languageData.getTerritories());
        territories.addAll(languageData.getTerritories());
        if (territories != null) {
          Set<String> scripts = new TreeSet(languageData.getScripts());
          scripts.addAll(languageData.getScripts());
          if (scripts == null || scripts.size() < 2) {
            otherTerritoryToLanguages.putAll(territories, language);
          } else {
            for (String script : scripts) {
              otherTerritoryToLanguages.putAll(territories, language + "_" + script);
            }
          }
        }
        }
      }
    }
    // compare them, listing differences
    for (String territory : sc.getGoodAvailableCodes("territory")) {
      Set<String> languages = supplementalData.getTerritoryToLanguages(territory);
      Set<String> otherLanguages = otherTerritoryToLanguages.getAll(territory);
      if (otherLanguages == null) otherLanguages = Collections.EMPTY_SET;
      if (!Utility.checkEquals(languages, otherLanguages)) {
        Set<String> languagesLeftover = new TreeSet<String>(languages);
        languagesLeftover.removeAll(otherLanguages);
        Set<String> otherLanguagesLeftover = new TreeSet<String>(otherLanguages);
        otherLanguagesLeftover.removeAll(languages);
        String territoryString = english.getName(CLDRFile.TERRITORY_NAME,territory,false);
        if (otherLanguagesLeftover.size() != 0) {
          for (String other : otherLanguagesLeftover) {
            String name = english.getName(other, false);
            System.out.println(territoryString + "\t" + territory + "\t" + name + "\t" + other);
          }
        }
      }
    } 
  }
}