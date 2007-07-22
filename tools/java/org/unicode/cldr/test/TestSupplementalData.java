package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSupplementalData {
  static CLDRFile english;
  private static SupplementalDataInfo supplementalData;
  private static StandardCodes sc;
  
  public static void main(String[] args) throws IOException {
//    genData();
//    if (true) return;
    Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
    english = cldrFactory.make("en", true);
    supplementalData = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
    sc = StandardCodes.make();

    System.out.println("Skipped Elements: " + supplementalData.getSkippedElements());
    checkAgainstLanguageScript();
    checkTerritoryMapping();
  }
  
  static Matcher numericTerritory = Pattern.compile("[0-9]{3}").matcher("");
  
  private static void checkTerritoryMapping() {
    Relation<String, String> alpha3 = supplementalData.getAlpha3TerritoryMapping();
    Set<String> temp = new TreeSet(sc.getAvailableCodes("territory"));
    for (Iterator<String> it = temp.iterator(); it.hasNext();) {
      String code = it.next();
      if (numericTerritory.reset(code).matches()) {
        it.remove();
        continue;
      }
//      if (sc.getFullData("territory", code).get(0).equals("PRIVATE USE")) {
//        it.remove();
//        continue;
//      }
    }
    showAnyDifferences("alpha3", alpha3.keySet(), "sc", temp);
  }

  private static void showAnyDifferences(String title, Set<String> set, String title2, Set<String> set2) {
    if (!set.equals(set2)) {
      showFirstMinusSecond("Failure " + title + "-" + title2 + ": ", set, set2);
      showFirstMinusSecond("Failure " + title2 + "-" + title + ": ", set2, set);
    }
  }

  private static void showFirstMinusSecond(String title, Set<String> name, Set<String> availableCodes) {
    Set<String> temp = getFirstMinusSecond(name, availableCodes);
    if (!temp.isEmpty()) {
      System.out.println(title + getFirstMinusSecond(name, availableCodes));
    }
  }

  private static Set<String> getFirstMinusSecond(Set<String> name, Set<String> availableCodes) {
    Set<String> temp = new TreeSet(name);
    temp.removeAll(availableCodes);
    return temp;
  }

  static void checkAgainstLanguageScript() {
    Relation<String,String> otherTerritoryToLanguages = new Relation(new TreeMap(), TreeSet.class, null);
    // get other language data
    for (String language : sc.getGoodAvailableCodes("language")) {
      Set<BasicLanguageData> newLanguageData = supplementalData.getBasicLanguageData(language);
      if (newLanguageData != null) {
        for (BasicLanguageData languageData : newLanguageData) {
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
        String territoryString = english.getName(CLDRFile.TERRITORY_NAME,territory);
        if (otherLanguagesLeftover.size() != 0) {
          for (String other : otherLanguagesLeftover) {
            String name = english.getName(other);
            System.out.println(territoryString + "\t" + territory + "\t" + name + "\t" + other);
          }
        }
      }
    } 
  }
  
  /** Temporary function to transform data
   * @throws IOException
   */
  public static void genData() throws IOException {
    BufferedReader codes = Utility.getUTF8Data("territory_codes.txt");
    Set<Pair> sorted = new TreeSet();
    while (true) {
      String line = codes.readLine();
      if (line == null)
        break;
      line = line.split("#")[0].trim();
      if (line.length() == 0)
        continue;
      String[] sourceValues = line.split("\\s+");
      String[] values = new String[5];
      for (int i = 0; i < values.length; ++i) {
        if (i >= sourceValues.length || sourceValues[i].equals("-"))
          values[i] = null;
        else
          values[i] = sourceValues[i];
      }
      String alpha2 = values[0];
      String numeric = values[1];
      String alpha3 = values[2];
      String internet = values[3];
      if (internet != null) {
        internet = internet.replace("/", " ");
      }
      if (internet != null)
        internet = internet.toUpperCase();
      String fips10 = values[4];
      Pair item = new Pair(alpha2, new Pair(numeric, new Pair(alpha3, new Pair(fips10, internet))));
      sorted.add(item);
    }
    for (Pair item : sorted) {
      //<territoryCodes type="CM" numeric="120" alpha3="CMR"/>
      System.out.print("<territoryCodes");
      Comparable first = item.getFirst();
      showNonNull("type", first, null);
      item = (Pair)item.getSecond();
      showNonNull("numeric", item.getFirst(), null);
      item = (Pair)item.getSecond();
      showNonNull("alpha3", item.getFirst(), null);
      item = (Pair)item.getSecond();
      showNonNull("fips10", item.getFirst(), first);
      showNonNull("internet", item.getSecond(), first);
      System.out.println("/>");
    }
    codes.close();
  }


  private static void showNonNull(String title, Object first, Object noDup) {
    if (first != null && !first.equals(noDup)) {
      System.out.print(" " + title + "=\"" + first + "\"");
    }
  }
}