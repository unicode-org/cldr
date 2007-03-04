package org.unicode.cldr.util;

import com.ibm.icu.text.MessageFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class SupplementalDataInfo {

  /**
   * Struct for return data
   */
  public static class PopulationData {
    private double population;

    private double populationLiteracyPercent;

    private double gdp;

    public double getGdp() {
      return gdp;
    }

    public double getLiteratePopulation() {
      return populationLiteracyPercent;
    }

    public double getPopulation() {
      return population;
    }

    public PopulationData setGdp(double gdp) {
      this.gdp = gdp;
      return this;
    }

    public PopulationData setPopulationLiteracyPercent(double literatePopulation) {
      this.populationLiteracyPercent = literatePopulation;
      return this;
    }

    public PopulationData setPopulation(double population) {
      this.population = population;
      return this;
    }

    public PopulationData set(PopulationData other) {
      if (other == null) {
        population = populationLiteracyPercent = gdp = Double.NaN;
      } else {
        population = other.population;
        populationLiteracyPercent = other.populationLiteracyPercent;
        gdp = other.gdp;
      }
      return this;
    }

    public void add(PopulationData other) {
      population += other.population;
      populationLiteracyPercent = (population * populationLiteracyPercent + other.population * other.populationLiteracyPercent) / population;
      gdp += other.gdp;
    }

    public String toString() {
      return MessageFormat.format("[pop: {0,number,#,##0},\t lit%: {0,number,#,##0.00},\t gdp: {0,number,#,##0}]", new Object[] { population, populationLiteracyPercent, gdp });
    }
  }

  static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  public static class LanguageData implements Comparable<LanguageData> {
    public enum Type {
      primary, secondary
    };

    private Type type = Type.primary;
    
    private Set<String> scripts = Collections.EMPTY_SET;

    private Set<String> territories = Collections.EMPTY_SET;

    public Type getType() {
      return type;
    }

    public LanguageData setType(Type type) {
      this.type = type;
      return this;
    }

    public LanguageData setScripts(String scriptTokens) {
      return setScripts(scriptTokens == null ? null : Arrays.asList(WHITESPACE_PATTERN.split(scriptTokens)));
    }
    
    public LanguageData setTerritories(String territoryTokens) {
      return setTerritories(territoryTokens == null ? null : Arrays.asList(WHITESPACE_PATTERN.split(territoryTokens)));
    }
    
    public LanguageData setScripts(Collection<String> scriptTokens) {
      // TODO add error checking
      scripts = scriptTokens == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(new TreeSet(scriptTokens));
      return this;
    }

    public LanguageData setTerritories(Collection<String> territoryTokens) {
      // TODO add error checking
      territories = territoryTokens == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(new TreeSet(territoryTokens));
      return this;
    }

    public LanguageData set(LanguageData other) {
      scripts = other.scripts;
      territories = other.territories;
      return this;
    }

    public Set<String> getScripts() {
      return scripts;
    }

    public Set<String> getTerritories() {
      return territories;
    }

    public String toString(String languageSubtag) {
      if (scripts.size() == 0 && territories.size() == 0) return "";
      return "\t\t<language type=\"" + languageSubtag + "\"" +
      (scripts.size() == 0 ? "" : " scripts=\"" + Utility.join(scripts, " ") + "\"") +
      (territories.size() == 0 ? "" : " territories=\"" + Utility.join(territories, " ") + "\"") + 
      (type == Type.primary ? "" : " alt=\"" + type + "\"") + 
      "/>";
    }

    public int compareTo(LanguageData o) {
      int result;
      if (0 != (result = type.compareTo(o.type))) return result;
      if (0 != (result = Utility.compare(scripts, o.scripts))) return result;
      if (0 != (result = Utility.compare(territories, o.territories))) return result;
      return 0;
    }
  }

  private Map<String, PopulationData> territoryToPopulationData = new TreeMap();

  private Map<String, Relation<String, PopulationData>> territoryToLanguageData = new TreeMap();

  private Relation<String, LanguageData> languageToLanguageData = new Relation(new TreeMap(), TreeSet.class);

  private Map<String, LanguageData> languageToLanguageData2 = new TreeMap();

  //Relation(new TreeMap(), TreeSet.class, null);
  private Map<String, PopulationData> languageToPopulation = new TreeMap();

  private Map<String, PopulationData> baseLanguageToPopulation = new TreeMap();

  private Set<String> allLanguages = new TreeSet();
  
  private Relation<String,String> containment = new Relation(new TreeMap(), TreeSet.class);

  public SupplementalDataInfo(String supplementalFileName) {
    XMLFileReader xfr = new XMLFileReader().setHandler(new MyHandler());
    xfr.read(supplementalFileName, -1, true);
    // now make stuff safe
    allLanguages.addAll(languageToPopulation.keySet());
    allLanguages.addAll(baseLanguageToPopulation.keySet());
    allLanguages = Collections.unmodifiableSet(allLanguages);
    
    containment.freeze();
    languageToLanguageData.freeze();
  }

  class MyHandler extends XMLFileReader.SimpleHandler {
    XPathParts parts = new XPathParts();

    LanguageTagParser languageTagParser = new LanguageTagParser();

    public void handlePathValue(String path, String value) {
      try {
        parts.set(path);
        String secondLevel = parts.getElement(1);
        // copy the rest from ShowLanguages later
        if (secondLevel.equals("territoryInfo")) {
          //        <territoryInfo>
          //        <territory type="AD" gdp="1840000000" literacyPercent="100" population="66000"> <!--Andorra-->
          //        <languagePopulation type="ca" populationPercent="50"/>  <!--Catalan-->

          Map<String, String> territoryAttributes = parts.getAttributes(2);
          String territory = territoryAttributes.get("type");
          double territoryPopulation = parseDouble(territoryAttributes.get("population"));
          double territoryLiteracyPercent = parseDouble(territoryAttributes.get("literacyPercent")) / 100.0;
          double territoryGdp = parseDouble(territoryAttributes.get("gdp"));
          if (territoryToPopulationData.get(territory) == null) {
            territoryToPopulationData.put(territory, new PopulationData().setPopulation(territoryPopulation).setPopulationLiteracyPercent(territoryLiteracyPercent).setGdp(territoryGdp));
          }
          if (parts.size() > 3) {

            Map<String, String> languageInTerritoryAttributes = parts.getAttributes(3);
            String language = languageInTerritoryAttributes.get("type");
            double languageLiteracyPercent = parseDouble(languageInTerritoryAttributes.get("literacyPercent"));
            if (Double.isNaN(languageLiteracyPercent))
              languageLiteracyPercent = territoryLiteracyPercent;
            double languagePopulationPercent = parseDouble(languageInTerritoryAttributes.get("populationPercent")) / 100;
            double languagePopulation = languagePopulationPercent * territoryPopulation;
            double languageGdp = languagePopulationPercent * territoryGdp;

            // store
            Relation<String, PopulationData> territoryLanguageToPopulation = territoryToLanguageData.get(territory);
            if (territoryLanguageToPopulation == null) {
              territoryToLanguageData.put(territory, territoryLanguageToPopulation = new Relation(new TreeMap(), HashSet.class, null));
            }
            PopulationData newData = new PopulationData().setPopulation(languagePopulation).setPopulationLiteracyPercent(languageLiteracyPercent).setGdp(languageGdp);
            territoryLanguageToPopulation.put(language, newData);

            // now collect data for languages globally
            PopulationData data = languageToPopulation.get(language);
            if (data == null)
              languageToPopulation.put(language, data = new PopulationData());
            data.add(newData);
            String baseLanguage = languageTagParser.set(language).getLanguage();
            if (!baseLanguage.equals(language)) {
              data = baseLanguageToPopulation.get(baseLanguage);
              if (data == null)
                baseLanguageToPopulation.put(baseLanguage, data = new PopulationData());
              data.add(newData);
            }
          }
          return;
        }
        if (secondLevel.equals("languageData")) {
          //        <languageData>
          //        <language type="aa" scripts="Latn" territories="DJ ER ET"/> <!--  Reflecting submitted data, cldrbug #1013 -->
          //        <language type="ab" scripts="Cyrl" territories="GE" alt="secondary"/>
          String language = (String) parts.getAttributeValue(2, "type");
          LanguageData languageData = new LanguageData();
          languageData.setType(parts.getAttributeValue(2, "alt") == null ? LanguageData.Type.primary : LanguageData.Type.secondary);
          languageData.setScripts(parts.getAttributeValue(2, "scripts")).setTerritories(parts.getAttributeValue(2, "territories"));
          languageToLanguageData.put(language, languageData);
          return;
        }
        if (secondLevel.equals("generation") || secondLevel.equals("version")) {
          // skip
          return;
        }
        if (secondLevel.equals("territoryContainment")) {
          // <group type="001" contains="002 009 019 142 150"/>
          containment.putAll(parts.getAttributeValue(-1,"type"), Arrays.asList(parts.getAttributeValue(-1,"contains").split("\\s+")));
          return;
        }
        if (!skippedElements.contains(secondLevel)) {
          skippedElements.add(secondLevel);
          System.out.println("TODO: Skipped Element: " + secondLevel + " - ... " + path + "...");
        }
        //System.out.println("Skipped Element: " + path);
      } catch (Exception e) {
        throw (IllegalArgumentException) new IllegalArgumentException("path: " + path + ",\tvalue: " + value).initCause(e);
      }
    }

    private double parseDouble(String literacyString) {
      return literacyString == null ? Double.NaN : Double.parseDouble(literacyString);
    }
  }

  Set<String> skippedElements = new TreeSet();
  
  /**
   * Get the population data for a language.
   * @param language
   * @param output
   * @return
   */
  public PopulationData getLanguagePopulationData(String language, PopulationData output) {
    PopulationData result = languageToPopulation.get(language);
    if (result == null) {
      result = baseLanguageToPopulation.get(language);
    }
    output.set(result);
    return output;
  }

  public Set<String> getLanguages() {
    return allLanguages;
  }

  public Set<String> getTerritoryToLanguages(String territory) {
    Relation<String, PopulationData> result = territoryToLanguageData.get(territory);
    if (result == null) {
      return Collections.EMPTY_SET;
    }
    return result.keySet();
  }

  public Set<LanguageData> getLanguageData(String language) {
    return languageToLanguageData.getAll(language);
  }
  
  public Set<String> getLanguageDataLanguages() {
    return languageToLanguageData.keySet();
  }
  
  public Set<String> getContained(String territoryCode) {
    return containment.getAll(territoryCode);
  }
  
  public Set<String> getContainers() {
    return containment.keySet();
  }
}