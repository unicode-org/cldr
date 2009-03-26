/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.tool.GenerateLikelySubtagTests;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Supports testing of paths in a file to see if they meet the coverage levels according to the UTS.<br>
 * Call init() exactly once with the supplementatData file and the supplementalMetadata file.<br>
 * For each new file, call setFile()<br>
 * Then for each path in the file, call getCoverageLevel(). You can then compare that to the desired level.<br>
 * There is a utility routine that will get the requiredLevel from the organization.
 * For an example, see CheckCoverage.
 * @author davis
 *
 */
public class CoverageLevel {
  private static final Pattern MINIMAL_PATTERNS = Pattern.compile(
        "/(" +
        "characters/exemplarCharacters(?!\\[@type=\"currencySymbol\"])" +
        "|calendar\\[\\@type\\=\"gregorian\"\\].*(" +
          "\\[@type=\"format\"].*\\[@type=\"(wide|abbreviated)\"]" +
          "|\\[@type=\"stand-alone\"].*\\[@type=\"narrow\"]" +
          "|/pattern" +
//          "|/dateFormatItem" +
          ")" +
        "|numbers/(" +
          "symbols" + // /(decimal/group)
          "|.*/pattern(?!Digit)" +
          ")" +
        "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
        "|unitPattern" + // "|units/unit.*/unitPattern" +
        "|intervalFormatFallback" + 
        ")");

  private static final Pattern BASIC_PATTERNS = Pattern.compile(
        "/(" +
        "measurementSystemName" +
        "|localeDisplayPattern" +
        "|characters/exemplarCharacters*(?!\\[@type=\"currencySymbol\"])" +
        "|delimiters" +
        "|codePattern" +
        "|calendar\\[\\@type\\=\"gregorian\"\\].*(" +
        "\\[@type=\"format\"].*\\[@type=\"(wide|abbreviated)\"]" +
        "|\\[@type=\"stand-alone\"].*\\[@type=\"narrow\"]" +
        "|/eraAbbr" +
        "|/pattern" +
        "|/dateFormatItem" +
        "|/fields(?!.*relative.*(2|3))" +
        ")" +
        "|numbers/symbols/(decimal/group)" +
        "|timeZoneNames/(hourFormat|gmtFormat|regionFormat|fallbackFormat)" +
        "|fallback(?![a-zA-Z])" +
        ")");

  public static final String EUROPEAN_UNION = "QU";
  
  /**
   * A simple class representing an enumeration of possible CLDR coverage levels. Levels may change in the future.
   * @author davis
   *
   */
  public enum Level {
    UNDETERMINED(0, "none"),
    POSIX(20,"G4"),
    MINIMAL(30,"G3.5"),
    BASIC(40,"G3"),
    MODERATE(60, "G2"),
    MODERN(80, "G1"),
    COMPREHENSIVE(100, "G0"),
    OPTIONAL(101, "optional");
    
    private byte level;
    private String altName;
    
    private Level(int i, String altName) {
      level = (byte) i;
      this.altName = altName;
    }
    
    public static Level get(String name) {
      try {
        return Level.valueOf(name.toUpperCase(Locale.ENGLISH));
      } catch (RuntimeException e) {
        for (Level level : Level.values()) {
          if (name.equalsIgnoreCase(level.altName)) {
            return level;
          }
        }
        return UNDETERMINED;
      }
    }
    
    public String toString() {
      return this.name().toLowerCase();
    }
    
//    public int compareTo(Level o) {
//      int otherLevel = ((Level) o).level;
//      return level < otherLevel ? -1 : level > otherLevel ? 1 : 0;
//    }
    
    public static int getDefaultWeight(String organization, String desiredLocale) {
      Level level = sc.getLocaleCoverageLevel(organization, desiredLocale);
      if (level.compareTo(Level.MODERATE) >= 0) {
        return 4;
      }
      return 1;
    }

  }
  
  private static Object sync = new Object();
  
  // commmon stuff, set once
  private static Map<String, Map<String, String>> coverageData = new TreeMap<String, Map<String, String>>();
  private static Map<String, Level> base_language_level = new TreeMap<String, Level>();
  private static Map<String, Level> base_script_level = new TreeMap<String, Level>();
  private static Map<String, Level> base_territory_level = new TreeMap<String, Level>();
  private static Map<String, Level> base_currency_level = new TreeMap<String, Level>();
  private static Map<String, Level> base_timezone_level = new TreeMap<String, Level>();
  
  private static Set<String> minimalCurrencies;
  private static Set<String> minimalTimezones;
  private static Set<String> moderateTimezones;
  
  private static Set<String> euroCountries;
  private static Set<String> territoryContainment = new TreeSet<String>();
  private static Set<String> euroLanguages = new TreeSet<String>();
  
  private static Relation<String,String> language_scripts = new Relation(new TreeMap(), TreeSet.class);;
  
  private static Relation<String,String> language_territories = new Relation(new TreeMap(), TreeSet.class);
  private static Relation<String,String> fallback_language_territories = new Relation(new TreeMap(), TreeSet.class);
  private static Relation<String,String> territory_languages = new Relation(new TreeMap(), TreeSet.class);
  
  private static Set<String> modernLanguages = new TreeSet<String>();
  private static Set<String> modernScripts = new TreeSet<String>();
  private static Set<String> modernTerritories = new TreeSet<String>();
  //private static Map locale_requiredLevel = new TreeMap();
  private static Map<String,Set<String>> territory_currency = new TreeMap<String,Set<String>>();
  private static Map<String,Set<String>> territory_timezone = new TreeMap<String,Set<String>>();
  private static Map<String,Set<String>> territory_calendar = new TreeMap<String,Set<String>>();
  private static Set<String> modernCurrencies = new TreeSet<String>();
  
  private static Map<String,Level> posixCoverage = new TreeMap<String, Level>();
  // current stuff, set according to file
  
  private boolean initialized = false;
  
  private transient LocaleIDParser parser = new LocaleIDParser();
  
  private transient XPathParts parts = new XPathParts(null, null);
  
  private Map<String, Level> language_level = new TreeMap<String, Level>();
  
  private Map<String, Level> script_level = new TreeMap<String, Level>();
  private Map<String, Level> zone_level = new TreeMap<String, Level>();
  private Map<String, CoverageLevel.Level> metazone_level = new TreeMap<String, Level>();
  
  private Map<String, Level> territory_level = new TreeMap<String, Level>();
  private Map<String, Level> currency_level = new TreeMap<String, Level>();
  private Map<String, Level> calendar_level = new TreeMap<String, Level>();

  private static Map<String,String> defaultTerritory = new TreeMap<String,String>();
  
  private static StandardCodes sc = StandardCodes.make();
  
  boolean exemplarsContainA_Z = false;

  private boolean currencyExemplarsContainA_Z;
  
  private static boolean euroCountriesMissing = false; // Set to TRUE if eurocountries weren't produced by init.
  
  /**
   * Used by the coverage & survey tools.
   * @param file only used to get at the supplemental data, since from any CLDRFile you can get to siblings
   * @param options optional parameters
   * @param cause TODO
   * @param possibleErrors if there are errors or warnings, those are added (as CheckStatus objects) to this list.
   * @return 
   */
  public CoverageLevel setFile(CLDRFile file, Map options, CheckCLDR cause, List possibleErrors) {
    synchronized (sync) {
      if (!initialized) {
        CLDRFile supplementalMetadata = file.getSupplementalMetadata();
        CLDRFile supplementalData = file.getSupplementalData();
        init(supplementalData, supplementalMetadata);
        initMetazoneCoverage(file, supplementalData, supplementalMetadata);
        initPosixCoverage(file.getLocaleID(), supplementalData);
        initialized = true;
      }
    }
    boolean exemplarsContainA_Z = false;
    UnicodeSet exemplars;
    try {
        exemplars = file.getResolved().getExemplarSet("", CLDRFile.WinningChoice.WINNING); // need to use resolved version to get exemplars
    } catch(IllegalArgumentException iae) {
      possibleErrors.add(new CheckStatus()
      .setCause(cause).setMainType(CheckStatus.errorType).setSubtype(Subtype.couldNotAccessExemplars)
          .setMessage("Could not get exemplar set: " + iae.toString()));
      return this;
    }
    
    if(exemplars == null) {
        throw new InternalCldrException("'"+file.getLocaleID()+"'.getExemplarSet() returned null.");
    }
    
    UnicodeSet auxexemplars = file.getExemplarSet("auxiliary", CLDRFile.WinningChoice.WINNING);
    if (auxexemplars != null) exemplars.addAll(auxexemplars);
    exemplarsContainA_Z = exemplars.contains('A','Z');
    
    boolean currencyExemplarsContainA_Z = false;
    auxexemplars = file.getExemplarSet("currencySymbol", CLDRFile.WinningChoice.WINNING);
    if (auxexemplars != null) currencyExemplarsContainA_Z = auxexemplars.contains('A','Z');

    setFile(file.getLocaleID(), exemplarsContainA_Z, currencyExemplarsContainA_Z, options, cause, possibleErrors);
    return this;
  }
  
  /**
   * Utility for getting the *default* required coverage level for a locale.
   * @param localeID
   * @param options
   * @return
   */
  public Level getRequiredLevel(String localeID, Map<String,String> options) {
    Level result;
    // see if there is an explicit level
    String localeType = options.get("CoverageLevel.requiredLevel");
    if (localeType != null) {
      result = Level.get(localeType);
      if (result != Level.UNDETERMINED) {
        return result;
      }
    }
    // otherwise, see if there is an organization level
    return sc.getLocaleCoverageLevel(options.get("CoverageLevel.localeType"), localeID);
  }
  
  /**
   * Separate interface for the configuration tool. Note: init() must be called exactly once
   * before calling this.
   * @param localeID the localeID for the file
   * @param exemplarsContainA_Z true if the union of the exemplar sets contains A-Z
   * @param currencyExemplarsContainA_Z TODO
   * @param options optional parameters
   * @param cause TODO
   * @param possibleErrors if there are errors or warnings, those are added (as CheckStatus objects) to this list.
   */
  public void setFile(String localeID, boolean exemplarsContainA_Z, boolean currencyExemplarsContainA_Z, Map options, CheckCLDR cause, List possibleErrors) {
    this.exemplarsContainA_Z = exemplarsContainA_Z;
    this.currencyExemplarsContainA_Z = currencyExemplarsContainA_Z;
    
    parser.set(localeID);
    String language = parser.getLanguage();
    
    // do the work of putting together the coverage info
    language_level.clear();
    script_level.clear();
    currency_level.clear();
    zone_level.clear();
    calendar_level.clear();
    
    
    language_level.putAll(base_language_level);
    
    script_level.putAll(base_script_level);
    try {
      Set<String> scriptsForLanguage = language_scripts.getAll(language);
      if (scriptsForLanguage != null && scriptsForLanguage.size() > 1) {
        putAll(script_level, scriptsForLanguage, CoverageLevel.Level.MINIMAL, true);
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    
    territory_level.putAll(base_territory_level);
    Set<String> mainTerritories = language_territories.getAll(language);
    putAll(territory_level, mainTerritories, CoverageLevel.Level.MINIMAL, true);
    if (mainTerritories == null || mainTerritories.size() == 0) {
      mainTerritories = fallback_language_territories.getAll(language);
      putAll(territory_level, mainTerritories, CoverageLevel.Level.MINIMAL, true);
    }

    
    {
      language_level.put(language, CoverageLevel.Level.MINIMAL);
      String script = parser.getScript();
      if (script != null) {
        script_level.put(script, CoverageLevel.Level.MINIMAL);
      }
      String territory = parser.getRegion();
      if (territory != null) {
        territory_level.put(territory, CoverageLevel.Level.MINIMAL);
      }
    }
    
    // special cases for EU
    if (euroLanguages.contains(language)) {
      setIfBetter(language_level, euroLanguages, CoverageLevel.Level.MODERATE, true);
      setIfBetter(territory_level, euroCountries, CoverageLevel.Level.MODERATE, true);
    }
    // special case pt_BR, zh_Hant, zh_Hans
    Level x = language_level.get("zh");
    if (x != null) {
      language_level.put("zh_Hans", x);
      language_level.put("zh_Hant", x);
    }
    x = language_level.get("pt");
    if (x != null) {
      language_level.put("pt_BR", x);
    }
    
    setIfBetter(territory_level, territoryContainment, CoverageLevel.Level.MODERATE, true);
    
    // set currencies, timezones according to territory level
    // HOWEVER, make the currency level at most BASIC
    putAll(currency_level, modernCurrencies, CoverageLevel.Level.MODERN, true);
    for (Iterator it = territory_level.keySet().iterator(); it.hasNext();) {
      String territory = (String) it.next();
      CoverageLevel.Level level = (CoverageLevel.Level) territory_level.get(territory);
      if (level.compareTo(CoverageLevel.Level.BASIC) < 0) level = CoverageLevel.Level.BASIC;
      Set currencies = (Set) territory_currency.get(territory);
      setIfBetter(currency_level, currencies, level, false);
      Set timezones = (Set) territory_timezone.get(territory);
      if (timezones != null) {
        // only worry about the ones that are "moderate"
        timezones.retainAll(moderateTimezones);
        setIfBetter(zone_level, timezones, level, false);
      }
    }
    
    // set the calendars only by the direct territories for the language
    calendar_level.put("gregorian", CoverageLevel.Level.BASIC);

    if (mainTerritories == null) {
      possibleErrors.add(new CheckStatus()
      .setCause(cause).setMainType(CheckStatus.warningType).setSubtype(Subtype.missingLanguageTerritoryInfo)
          .setMessage("Missing language->territory information in supplemental data!"));
    } else for (Iterator it = mainTerritories.iterator(); it.hasNext();) {
      String territory = (String) it.next();
      setIfBetter(calendar_level, (Collection) territory_calendar.get(territory), CoverageLevel.Level.BASIC, true);
    }
    
    setIfBetter(currency_level, minimalCurrencies, Level.MINIMAL, true);
    setIfBetter(zone_level, minimalTimezones, Level.MINIMAL, true);
    
    if (CheckCoverage.DEBUG) {
      System.out.println("language_level: " + language_level);               
      System.out.println("script_level: " + script_level);
      System.out.println("territory_level: " + territory_level);
      System.out.println("currency_level: " + currency_level);
      System.out.println("euroCountries: " + euroCountries);
      System.out.println("euroLanguages: " + euroLanguages);
      System.out.flush();
    }
    
    // A complaint.
    if(euroCountriesMissing) {
      possibleErrors.add(new CheckStatus()
      .setCause(cause).setMainType(CheckStatus.errorType).setSubtype(Subtype.missingEuroCountryInfo)
          .setMessage("Missing euro country information- '" + EUROPEAN_UNION + "' missing in territory codes?"));
    }
  }
  
  /**
   * For each items in keyset, targetMap.put(item, value);
   * @param targetMap
   * @param keyset
   * @param value
   * @param override TODO
   */
  private static void putAll(Map targetMap, Collection keyset, Object value, boolean override) {
    if (keyset == null) return;
    for (Iterator it2 = keyset.iterator(); it2.hasNext();) {
      Object item = it2.next();
      if (!override & targetMap.get(item) != null) {
        continue;
      }
      targetMap.put(item, value);
    }
  }
  
  private static void addAllToCollectionValue(Map targetMap, Collection keyset, Object value, Class classForNew) {
    if (keyset == null) return;
    for (Iterator it2 = keyset.iterator(); it2.hasNext();) {
      addToValueSet(targetMap, it2.next(), value, classForNew);
    }
  }
  
  private static <K,V> void addToValueSet(Map<K,Set<V>> targetMap, K key, V value, Class<Set<V>> classForNew) {
    Set<V> valueSet = targetMap.get(key);
    if (valueSet == null) try {
      targetMap.put(key, valueSet = classForNew.newInstance());
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot create collection with " + classForNew.getName());
    }
    valueSet.add(value);
  }
  
  private void setIfBetter(Map targetMap, Collection keyCollection, CoverageLevel.Level level, boolean show) {
    if (keyCollection == null) return;
    for (Iterator it2 = keyCollection.iterator(); it2.hasNext();) {
      Object item = it2.next();
      setIfBetter(targetMap, item, level, show);
    }
  }

  private void setIfBetter(Map targetMap, Object item, CoverageLevel.Level level, boolean show) {
    CoverageLevel.Level old = (CoverageLevel.Level) targetMap.get(item);
    if (old == null || level.compareTo(old) < 0) {
      if (CheckCoverage.DEBUG_SET && show) System.out.println("\t" + item + "\t(" + old + " \u2192 " + level + ")");
      targetMap.put(item, level);
    }
  }
  
  Matcher basicPatterns = BASIC_PATTERNS.matcher("");
  
  Matcher minimalPatterns = MINIMAL_PATTERNS.matcher("");
  
  // //ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="day"]/relative[@type="2"]

  public CoverageLevel.Level getCoverageLevel(String fullPath) {
    return getCoverageLevel(fullPath, null);
  }
  /**
   * Returns the coverage level of the path. This is the least level at which the path is required.
   * @param fullPath (with all information). If this is null, distinguishedPath must not be
   * @param distinguishedPath the normal key for a cldr file. If this is null, fullPath must not be.
   * @return the coverage level. UNDETERMINED is returned if there is not enough information to determine the level (initially
   * we only look at the long lists of display names, currencies, timezones, etc.).
   */
  public CoverageLevel.Level getCoverageLevel(String fullPath, String distinguishedPath) {
    if (fullPath != null && fullPath.contains("/alias")) {
      return CoverageLevel.Level.UNDETERMINED; // skip
    }
    if (distinguishedPath == null) {
      distinguishedPath = CLDRFile.getDistinguishingXPath(fullPath,null,false);
    }
    
    // the minimal level is posix, so test for that.
    CoverageLevel.Level result = posixCoverage.get(distinguishedPath); // default the level to POSIX
    if (result != null) {
      return result;
    }
    
    // the next check we make is for certain basic patterns, based on a regex
    if (minimalPatterns.reset(distinguishedPath).find()) {
      return CoverageLevel.Level.MINIMAL;
    }

    // the next check we make is for certain basic patterns, based on a regex
    if (basicPatterns.reset(distinguishedPath).find()) {
      return CoverageLevel.Level.BASIC;
    }
    
    // we now do some more complicated tests that depend on the type
    parts.set(distinguishedPath);
    String lastElement = parts.getElement(-1);
    String type = parts.getAttributeValue(-1, "type");
    String part1 = parts.getElement(1);
    
    if (lastElement.equals("exemplarCity")) {
      type = parts.getAttributeValue(-2, "type"); // it's one level up
      if (exemplarsContainA_Z && !type.equals("Etc/Unknown")) {
        result = CoverageLevel.Level.OPTIONAL;
      } else {
        result = (CoverageLevel.Level) zone_level.get(type);
      }
    } else if (part1.equals("localeDisplayNames")) {
      if (lastElement.equals("language")) {
        // <language type=\"aa\">Afar</language>"
        result = language_level.get(type);
      } else if (lastElement.equals("territory")) {
        result = (CoverageLevel.Level) territory_level.get(type);
      } else if (lastElement.equals("script")) {
        result = (CoverageLevel.Level) script_level.get(type);
      } else if (lastElement.equals("type")) {
        String key = parts.getAttributeValue(-1, "key");
        if (key.equals("calendar")) {
          result = (CoverageLevel.Level) calendar_level.get(type);
        }
      }
      // <types><type type="big5han" key="collation">Traditional Chinese (Big5)</type>
    } else if (distinguishedPath.contains("metazone")) {
       result = metazone_level.get(distinguishedPath);
    } else if (part1.equals("numbers")) {
      /*
       * <numbers> ? <currencies> ? <currency type="BRL"> <displayName draft="true">Brazilian Real</displayName>
       */
      if (currencyExemplarsContainA_Z && lastElement.equals("symbol")) {
        result = CoverageLevel.Level.OPTIONAL;
      } else if (lastElement.equals("displayName") || lastElement.equals("symbol")) {
        if (parts.getAttributeValue(-1, "count") != null) {
          result = Level.UNDETERMINED;
        } else {
          String currency = parts.getAttributeValue(-2, "type");
          result = (CoverageLevel.Level) currency_level.get(currency);
        }
      }
    } else if (part1.equals("identity")) {
      result = Level.UNDETERMINED;
    } else if (lastElement.equals("greatestDifference")) {
      String calendar = parts.getAttributeValue(3, "type");
      if (calendar.equals("gregorian")) {
        String id = parts.getAttributeValue(-2, "id");
        if (INTERVAL_FORMATS.contains(id)) {
          result = CoverageLevel.Level.BASIC;
        }
      }
    } else if (lastElement.equals("dateFormatItem")) {
      String calendar = parts.getAttributeValue(3, "type");
      if (calendar.equals("gregorian")) {
        String id = parts.getAttributeValue(-1, "id");
        if (DATE_FORMAT_ITEM_IDS.contains(id)) {
          result = CoverageLevel.Level.BASIC;
        }
      }
    } else {
      // System.out.println("Skipping\t" + fullPath);
    }
    
    if (result == null) {
       if (distinguishedPath.contains("metazone") || distinguishedPath.contains("usesMetazone")) {
         result = CoverageLevel.Level.OPTIONAL;
       }
       else {
         result = CoverageLevel.Level.COMPREHENSIVE;
       }
    }
   
    return result;
  }
  
  static final Set DATE_FORMAT_ITEM_IDS = new HashSet<String>(Arrays.asList(
          "Hm  M  MEd  MMM  MMMEd  MMMMEd  MMMMd  MMMd  Md  d  ms  y  yM  yMEd  yMMM  yMMMEd  yMMMM  yQ  yQQQ".split("\\s+")));
  static final Set INTERVAL_FORMATS = new HashSet<String>(Arrays.asList(
          "M MEd MMM MMMEd MMMM MMMd Md d h hm hmv hv y yM yMEd yMMM yMMMEd yMMMM yMMMd yMd".split("\\s+")));
  
  //
  
  // ========== Initialization Stuff ===================

  /**
   * Should only be called once.
   */
  public static void init(CLDRFile supplementalData, CLDRFile supplementalMetadata) {
    try {
      
      getMetadata(supplementalMetadata);
      getData(supplementalData);
      
      // put into an easier form to use
      
      Map type_languages = (Map) coverageData.get("languageCoverage");
      Utility.putAllTransposed(type_languages, base_language_level);
      Map type_scripts = (Map) coverageData.get("scriptCoverage");
      Utility.putAllTransposed(type_scripts, base_script_level);
      Map type_territories = (Map) coverageData.get("territoryCoverage");
      Utility.putAllTransposed(type_territories, base_territory_level);
      
      Map type_currencies = (Map) coverageData.get("currencyCoverage");
      Utility.putAllTransposed(type_territories, base_currency_level);
      Map type_timezones = (Map) coverageData.get("timezoneCoverage");
      Utility.putAllTransposed(type_territories, base_timezone_level);

      minimalCurrencies = (Set) type_currencies.get(CoverageLevel.Level.MINIMAL);
      minimalTimezones = (Set) type_timezones.get(CoverageLevel.Level.MINIMAL);
      moderateTimezones = (Set) type_timezones.get(CoverageLevel.Level.MODERATE);
      
      // add the modern stuff, after doing both of the above
      
      //modernLanguages.removeAll(base_language_level.keySet());
      putAll(base_language_level, modernLanguages, CoverageLevel.Level.MODERN, false);
      //putAll(base_language_level, sc.getGoodAvailableCodes("language"), CoverageLevel.Level.COMPREHENSIVE, false);
      
      //modernScripts.removeAll(base_script_level.keySet());
      putAll(base_script_level, modernScripts, CoverageLevel.Level.MODERN, false);
      //putAll(base_script_level, sc.getGoodAvailableCodes("script"), CoverageLevel.Level.COMPREHENSIVE, false);
      
      //modernTerritories.removeAll(base_territory_level.keySet());
      putAll(base_territory_level, modernTerritories, CoverageLevel.Level.MODERN, false);
      //putAll(base_territory_level, sc.getGoodAvailableCodes("territory"), CoverageLevel.Level.COMPREHENSIVE, false);
      
      //if(euroCountries != null) {
      for (Iterator it = euroCountries.iterator(); it.hasNext();) {
        String territory = (String) it.next();
        Collection languages = territory_languages.getAll(territory);
        euroLanguages.addAll(languages);
      }
      //}
      
      if (false) {
        for (Iterator it = territory_currency.keySet().iterator(); it
        .hasNext();) {
          String territory = (String) it.next();
          System.out.print(ULocale.getDisplayCountry("und_"
              + territory, ULocale.ENGLISH)
              + "\t" + territory
              + "\t\u2192\t");
          Collection languages = territory_languages.getAll(territory);
          if (languages == null || languages.size() == 0) {
            System.out.print("-NONE-");
          } else for (Iterator it2 = languages.iterator(); it2.hasNext();) {
            String language = (String) it2.next();
            System.out.print(ULocale.getDisplayLanguage(
                language, ULocale.ENGLISH)
                + " (" + language + ")"
                + ";\t");
          }
          System.out.println();
        }
      }
      
      
      if (CheckCoverage.DEBUG) {
        System.out.println("base_language_level: " + base_language_level);               
        System.out.println("base_script_level: " + base_script_level);
        System.out.println("base_territory_level: " + base_territory_level);
        System.out.flush();
      }
    } catch (RuntimeException e) {
      throw e; // just for debugging
    }
  }
  
  private void initPosixCoverage(String localeID, CLDRFile supplementalData){
    parser.set(localeID);
    //String language = parser.getLanguage();
    String territory = parser.getRegion();
    //String language = parser.getLanguage();
    //String script = parser.getScript();
    //String scpt = parser.getScript();
    
    // we have to have the name for our own locale
//  posixCoverage.put("//ldml/localeDisplayNames/languages/language[@type=\""+language+"\"]", Level.POSIX);
//  if (script != null) {
//  posixCoverage.put("//ldml/localeDisplayNames/scripts/script[@type=\""+language+"\"]", Level.POSIX);
//  }
//  if (territory != null) {
//  posixCoverage.put("//ldml/localeDisplayNames/territories/territory[@type=\""+language+"\"]", Level.POSIX);
//  }
    // TODO fix version
    // this won't actually work. Values in the file are of the form:
    
    //      supplementalData[@version="1.4"]/currencyData/region[@iso3166="MG"]/currency[@from="1983-11-01"][@iso4217="MGA"]
    //Need to walk through the file and pick out a from/to values that are valid for now. May be multiple also!!
    String currencySymbol = supplementalData.getWinningValue("//supplementalData[@version=\"1.4\"]/currencyData/region[@iso3166=\""+territory+"\"]/currency");
//  if (currencySymbol == null) {
//  throw new IllegalArgumentException("Internal Error: can't find currency for region: " + territory);
//  }
    //String fractions = supplementalData.getStringValue("//supplementalData/currencyData/fractions/info[@iso4217='"+currencySymbol+"']");
    posixCoverage.put("//ldml/posix/messages/yesstr", Level.POSIX);
    posixCoverage.put("//ldml/posix/messages/nostr", Level.POSIX);
    posixCoverage.put("//ldml/characters/exemplarCharacters", Level.POSIX);
    posixCoverage.put("//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/numbers/currencies/currency[@type=\""+currencySymbol+"\"]/symbol", Level.POSIX);
    posixCoverage.put("//ldml/numbers/currencies/currency[@type=\""+currencySymbol+"\"]/decimal", Level.POSIX);
    posixCoverage.put("//ldml/numbers/currencies/currency[@type=\""+currencySymbol+"\"]/group", Level.POSIX);
    posixCoverage.put("//ldml/numbers/symbols/decimal", Level.POSIX);
    posixCoverage.put("//ldml/numbers/symbols/group", Level.POSIX);
//    posixCoverage.put("//ldml/numbers/symbols/plusSign", Level.POSIX); // the inheritance from root is almost always right, so don't require
//    posixCoverage.put("//ldml/numbers/symbols/minusSign", Level.POSIX); // the inheritance from root is almost always right, so don't require
    posixCoverage.put("//ldml/numbers/symbols/decimal", Level.POSIX);
    posixCoverage.put("//ldml/numbers/symbols/group", Level.POSIX);
    posixCoverage.put("//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat/pattern", Level.POSIX);
//    posixCoverage.put("//ldml/numbers/symbols/nativeZeroDigit", Level.POSIX); // the inheritance from root is almost always right, so don't require
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"medium\"]/timeFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength/dateTimeFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"long\"]/timeFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"long\"]/dateFormat/pattern", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/am", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/pm", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"sun\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"mon\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"tue\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"wed\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"thu\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"fri\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"sat\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"mon\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"tue\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"wed\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"thu\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"fri\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sat\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"2\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"3\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"4\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"5\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"6\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"7\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"8\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"9\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"10\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"11\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"12\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"2\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"4\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"5\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"6\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"7\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"8\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"9\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"10\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"11\"]", Level.POSIX);
    posixCoverage.put("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"12\"]", Level.POSIX);
    //posixCoverage.put("//ldml/collations/collation[@type=\"standard\"]/settings", Level.POSIX);
    //posixCoverage.put("//ldml/collations/collation[@type=\"standard\"]/rules", Level.POSIX);
  }
  
  private void initMetazoneCoverage(CLDRFile file, CLDRFile supplementalData, CLDRFile supplementalMetadata) {
    String metazone_prefix = "//ldml/dates/timeZoneNames/metazone";
    String localeID = file.getLocaleID();
    parser.set(localeID);
    String territory = parser.getRegion();

    if ( territory == null || territory.length() == 0) {
       territory = defaultTerritory.get(localeID);
       if (territory == null) {
         territory = "ZZ";
       }
    }

    Set timezones = (Set) territory_timezone.get(territory);

    Set usedMetazones = new HashSet();
    usedMetazones.clear();

    if ( timezones != null && !timezones.isEmpty() ) {

      for (Iterator it = timezones.iterator(); it.hasNext();) {
        String tz = (String) it.next();
        XPathParts parts = new XPathParts(null,null);
        String usedMetazone = file.getResolved().getCurrentMetazone(tz);
        if ( !usedMetazones.contains(usedMetazone)) {
          usedMetazones.add(usedMetazone);
        }
      }
    }
    
    metazone_level.clear();

    /* Tier 1 - Most populous and commonly used metazones - Basic coverage */

    String [][] Basic_Metazones = {
       { "America_Eastern", "generic" },
       { "America_Central", "generic" },
       { "America_Mountain", "generic" },
       { "America_Pacific", "generic" },
       { "Atlantic", "generic" },
       { "Europe_Central", "daylight" },
       { "Europe_Eastern", "daylight" },
       { "Europe_Western", "daylight" },
       { "GMT", "standard" }};
    
    for ( int i = 0 ; i < Basic_Metazones.length ; i++ ) {
      metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/long/standard", Level.BASIC);
      metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/short/standard", Level.BASIC);
      if ( Basic_Metazones[i][1].equals("generic") || Basic_Metazones[i][1].equals("daylight")) {
        metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/long/daylight", Level.BASIC);
        metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/short/daylight", Level.BASIC);
        if ( Basic_Metazones[i][1].equals("generic")) {
          metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/long/generic", Level.BASIC);
          metazone_level.put(metazone_prefix+"[@type=\""+Basic_Metazones[i][0]+"\"]/short/generic", Level.BASIC);
        }
      }
    } 

    /* Tier 2 - Moderate Coverage level for metazones */

    String [][] Moderate_Metazones = {
       { "Africa_Central", "standard" },
       { "Africa_Eastern", "standard" },
       { "Africa_Southern", "standard" },
       { "Africa_Western", "daylight" },
       { "Arabian", "generic" },
       { "China", "standard" },
       { "India", "standard" },
       { "Israel", "daylight" },
       { "Japan", "standard" },
       { "Korea", "standard" }};

    for ( int i = 0 ; i < Moderate_Metazones.length ; i++ ) {
      
      CoverageLevel.Level level;
      if ( usedMetazones.contains(Moderate_Metazones[i][0]))
         level = Level.BASIC;
      else
         level = Level.MODERATE;

      metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/long/standard", level);
      metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/short/standard", level);
      if ( Moderate_Metazones[i][1].equals("generic") || Moderate_Metazones[i][1].equals("daylight")) {
        metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/long/daylight", level);
        metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/short/daylight", level);
        if ( Moderate_Metazones[i][1].equals("generic")) {
          metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/long/generic", level);
          metazone_level.put(metazone_prefix+"[@type=\""+Moderate_Metazones[i][0]+"\"]/short/generic", level);
        }
      }
    } 

    String [][] Modern_Metazones = {
       { "Acre", "daylight" },
       { "Amazon", "daylight" },
       { "Australia_Central", "generic" },
       { "Australia_Eastern", "generic" },
       { "Australia_Western", "generic" },
       { "Australia_CentralWestern", "daylight" },
       { "Brasilia", "daylight" },
       { "Hong_Kong", "daylight" },
       { "Indochina", "standard" },
       { "Indonesia_Central", "standard" },
       { "Indonesia_Eastern", "standard" },
       { "Indonesia_Western", "standard" },
       { "Moscow", "daylight" },
       { "New_Zealand", "generic" }};

    for ( int i = 0 ; i < Modern_Metazones.length ; i++ ) {

      CoverageLevel.Level level;
      if ( usedMetazones.contains(Modern_Metazones[i][0]))
         level = Level.BASIC;
      else
         level = Level.MODERN;

      metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/long/standard", level);
      metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/short/standard", level);
      if ( Modern_Metazones[i][1].equals("generic") || Modern_Metazones[i][1].equals("daylight")) {
        metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/long/daylight", level);
        metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/short/daylight", level);
        if ( Modern_Metazones[i][1].equals("generic")) {
          metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/long/generic", level);
          metazone_level.put(metazone_prefix+"[@type=\""+Modern_Metazones[i][0]+"\"]/short/generic", level);
        }
      }
    } 
    /* 4th Tier - Comprehensive Level */

    String [][] Comprehensive_Metazones = {
       { "Afghanistan", "standard" },
       { "Alaska", "generic" },
       { "Anadyr", "daylight" },
       { "Argentina", "daylight" },
       { "Armenia", "daylight" },
       { "Azerbaijan", "daylight" },
       { "Azores", "daylight" },
       { "Bangladesh", "standard" },
       { "Bhutan", "standard" },
       { "Bolivia", "standard" },
       { "Brunei", "standard" },
       { "Cape_Verde", "daylight" },
       { "Chamorro", "standard" },
       { "Chatham", "daylight" },
       { "Chile", "daylight" },
       { "Choibalsan", "daylight" },
       { "Christmas", "standard" },
       { "Cocos", "standard" },
       { "Colombia", "daylight" },
       { "Cook", "standard" },
       { "Cuba", "daylight" },
       { "Davis", "standard" },
       { "DumontDUrville", "standard" },
       { "East_Timor", "standard" },
       { "Easter", "daylight" },
       { "Ecuador", "standard" },
       { "Falkland", "daylight" },
       { "Fiji", "daylight" },
       { "French_Guiana", "standard" },
       { "French_Southern", "standard" },
       { "Galapagos", "standard" },
       { "Gambier", "standard" },
       { "Georgia", "daylight" },
       { "Gilbert_Islands", "standard" },
       { "Greenland_Eastern", "daylight" },
       { "Greenland_Western", "daylight" },
       { "Guam", "standard" },
       { "Guyana", "standard" },
       { "Hawaii_Aleutian", "generic" },
       { "Hawaii", "standard" },
       { "Hovd", "daylight" },
       { "Indian_Ocean", "standard" },
       { "Iran", "daylight" },
       { "Irkutsk", "daylight" },
       { "Kamchatka", "daylight" },
       { "Kazakhstan_Eastern", "standard" },
       { "Kazakhstan_Western", "standard" },
       { "Kosrae", "standard" },
       { "Krasnoyarsk", "daylight" },
       { "Kyrgystan", "standard" },
       { "Line_Islands", "standard" },
       { "Lord_Howe", "daylight" },
       { "Magadan", "daylight" },
       { "Malaysia", "standard" },
       { "Maldives", "standard" },
       { "Marquesas", "standard" },
       { "Marshall_Islands", "standard" },
       { "Mauritius", "daylight" },
       { "Mawson", "standard" },
       { "Mongolia", "daylight" },
       { "Nauru", "standard" },
       { "Nepal", "standard" },
       { "Newfoundland", "generic" },
       { "New_Caledonia", "daylight" },
       { "Niue", "standard" },
       { "Norfolk", "standard" },
       { "Noronha", "daylight" },
       { "North_Mariana", "standard" },
       { "Novosibirsk", "daylight" },
       { "Omsk", "daylight" },
       { "Pakistan", "daylight" },
       { "Palau", "standard" },
       { "Papua_New_Guinea", "standard" },
       { "Paraguay", "daylight" },
       { "Peru", "daylight" },
       { "Philippines", "daylight" },
       { "Phoenix_Islands", "standard" },
       { "Pierre_Miquelon", "daylight" },
       { "Pitcairn", "standard" },
       { "Ponape", "standard" },
       { "Reunion", "standard" },
       { "Rothera", "standard" },
       { "Sakhalin", "daylight" },
       { "Samara", "daylight" },
       { "Samoa", "standard" },
       { "Seychelles", "standard" },
       { "Singapore", "standard" },
       { "Solomon", "standard" },
       { "South_Georgia", "standard" },
       { "Suriname", "standard" },
       { "Syowa", "standard" },
       { "Tahiti", "standard" },
       { "Tajikistan", "standard" },
       { "Tonga", "daylight" },
       { "Truk", "standard" },
       { "Turkmenistan", "daylight" },
       { "Tuvalu", "standard" },
       { "Uruguay", "daylight" },
       { "Uzbekistan", "daylight" },
       { "Vanuatu", "daylight" },
       { "Venezuela", "standard" },
       { "Vladivostok", "daylight" },
       { "Volgograd", "daylight" },
       { "Vostok", "standard" },
       { "Wake", "standard" },
       { "Wallis", "standard" },
       { "Yakutsk", "daylight" },
       { "Yekaterinburg", "daylight" }};

    for ( int i = 0 ; i < Comprehensive_Metazones.length ; i++ ) {

      CoverageLevel.Level level;
      if ( usedMetazones.contains(Comprehensive_Metazones[i][0]))
         level = Level.BASIC;
      else
         level = Level.COMPREHENSIVE;

      metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/long/standard", level);
      metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/short/standard", level);
      if ( Comprehensive_Metazones[i][1].equals("generic") || Comprehensive_Metazones[i][1].equals("daylight")) {
        metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/long/daylight", level);
        metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/short/daylight", level);
        if ( Comprehensive_Metazones[i][1].equals("generic")) {
          metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/long/generic", level);
          metazone_level.put(metazone_prefix+"[@type=\""+Comprehensive_Metazones[i][0]+"\"]/short/generic", level);
        }
      }
    } 
  }

  private static void getMetadata(CLDRFile metadata) {
    XPathParts parts = new XPathParts();
    LanguageTagParser languageTagParser = new LanguageTagParser();
    for (Iterator it = metadata.iterator(); it.hasNext();) {
      String path = (String) it.next();
      path = metadata.getFullXPath(path);
      parts.set(path);
      String lastElement = parts.getElement(-1);
      //Map attributes = parts.getAttributes(-1);
      String type = parts.getAttributeValue(-1, "type");
      if (parts.containsElement("coverageAdditions")) {
        // System.out.println(path);
        // System.out.flush();
        //String value = metadata.getStringValue(path);
        // <languageCoverage type="basic" values="de en es fr it ja
        // pt ru zh"/>
        CoverageLevel.Level level = CoverageLevel.Level.get(type);
        String values = parts.getAttributeValue(-1, "values");
        Utility.addTreeMapChain(coverageData, 
            lastElement, level,
            new TreeSet(Arrays.asList(values.split("\\s+"))));
      }
      else if (parts.containsElement("defaultContent")) {
        String defContent = parts.getAttributeValue(-1, "locales");
        String [] defLocales = defContent.split(" ");
        for ( int i = 0 ; i < defLocales.length ; i++ ) {
//           int pos = defLocales[i].lastIndexOf('_');
//           String defLang = defLocales[i].substring(0,pos);
//           String defTerr = defLocales[i].substring(pos+1);
//           if ( defTerr.length() == 2 ) {
//              defaultTerritory.put(defLang,defTerr);
//           }
          // not sure what the above code was trying to do, but it did not put a value in for "uz" which caused the line:
          //        territory = defaultTerritory.get(localeID);
          // to still return null, and thus causes a null exception.
          // Am guessing that what it is trying to do is get the language subtag.
          languageTagParser.set(defLocales[i]);
          defaultTerritory.put(languageTagParser.getLanguage(), languageTagParser.getRegion());
        } 
      }
    }
  }
  
  private static Set multizoneTerritories = null;
  
  private static void getData(CLDRFile data) {
    
    
    // Get the modern languages, scripts, and territories from a more accurate source
    
    Set<String> officialModernLanguages = new HashSet<String>();
    Set<String> officialModernTerritories = new HashSet<String>();
    Set<String> officialModernScripts = new HashSet<String>();
    SupplementalDataInfo info = SupplementalDataInfo.getInstance(data.getSupplementalDirectory());
    Map<String, String> likelySubtags = info.getLikelySubtags();
    LanguageTagParser languageTagParser = new LanguageTagParser();

    for (String territory : info.getTerritoriesWithPopulationData()) {
      for (String language : info.getLanguagesForTerritoryWithPopulationData(territory)) {
        PopulationData languageInfo = info.getLanguageAndTerritoryPopulationData(language, territory);
        OfficialStatus officialStatus = languageInfo.getOfficialStatus();
        if (officialStatus != OfficialStatus.unknown) {
          officialModernLanguages.add(language);
          officialModernTerritories.add(territory);
          
          String baseLanguage = languageTagParser.set(language).getLanguage();
          String script = languageTagParser.getScript();
          
          language_territories.put(baseLanguage, territory);
          territory_languages.put(territory, baseLanguage);
          if (script.length() == 0) {
            final String maxFrom = GenerateLikelySubtagTests.maximize(language, likelySubtags);
            if (maxFrom != null) {
              script = languageTagParser.set(maxFrom).getScript();
            }
          }
          officialModernScripts.add(script);
          language_scripts.put(baseLanguage, script);
        }
      }
    }
    officialModernScripts.add("Kore");
    officialModernScripts.add("Hani");
    officialModernScripts.add("Japn");
    // protect, just to avoid errors
    officialModernLanguages = Collections.unmodifiableSet(officialModernLanguages);
    officialModernTerritories = Collections.unmodifiableSet(officialModernTerritories);
    officialModernScripts = Collections.unmodifiableSet(officialModernScripts);
    
    
    modernTerritories = officialModernTerritories;

    String lastCurrencyYear = Integer.toString(1900 + new Date().getYear() - 2);
    
    XPathParts parts = new XPathParts();
    
    // TODO change to use SupplementalDataInfo

    // optimization -- don't get the paths in sorted order.
    // data.iterator(null, CLDRFile.ldmlComparator)
    for (Iterator it = data.iterator(); it.hasNext();) {
      String path = (String) it.next();
      if (false) {
        System.out.println(path);
        System.out.flush();
      }
      //String value = metadata.getStringValue(path);
      path = data.getFullXPath(path);
      parts.set(path);
      String lastElement = parts.getElement(-1);
      //Map attributes = parts.getAttributes(-1);
      String type = parts.getAttributeValue(-1, "type");
      //System.out.println(path);
      if (lastElement.equals("zoneItem")) {
        if (multizoneTerritories == null) {
          //Map multiAttributes = parts.getAttributes(-2);
          String multizone = parts.getAttributeValue(-2, "multizone");
          multizoneTerritories = new TreeSet(Arrays.asList(multizone.split("\\s+")));
        }
        //<zoneItem type="Africa/Abidjan" territory="CI"/>
        String territory = parts.getAttributeValue(-1, "territory");
        // if (!multizoneTerritories.contains(territory)) continue;
        Set territories = (Set) territory_timezone.get(territory);
        if (territories == null) territory_timezone.put(territory, territories = new TreeSet());
        territories.add(type);
      } else if (lastElement.equals("calendarPreference") && parts.containsElement("calendarPreferenceData")) {
        
        // Samples of XML to handle:
        //  .../calendarPreferenceData/calendarPreference[@territories="001"][@ordering="gregorian"]
        // territories="AE BH DJ DZ EH ER IQ JO KM KW LB LY MA MR OM PS QA SA SD SY TD TN YE"
        
        Set<String> values = new TreeSet<String>(
              Arrays.asList((parts.getAttributeValue(-1, "territories").trim()).split("\\s+")));
        if (values.contains("")) {
          throw new IllegalArgumentException("calendarData/calendar/ illegal territories string in:" + path);
        }
        Set<String> calendars = new TreeSet<String>(
                Arrays.asList((parts.getAttributeValue(-1, "ordering").trim()).split("\\s+")));
        
        for (String calendar : calendars) {
          Utility.addTreeMapChain(coverageData, "calendar", calendar, values);
          addAllToCollectionValue(territory_calendar, values, calendar, TreeSet.class);
        }
      } else if (parts.containsElement("languageData")) {
        // <language type="ab" scripts="Cyrl" territories="GE"
        // alt="secondary"/>
        String alt = parts.getAttributeValue(-1, "alt");
        if (alt != null) {
          // get fallback territories
          String territories = parts.getAttributeValue(-1, "territories");
          if (territories != null) {
            Set territorySet = new TreeSet(Arrays
                .asList(territories
                    .split("\\s+")));
            fallback_language_territories.putAll(type, territorySet);
          }
          continue;
        }
        if (officialModernLanguages.contains(type)) {
          modernLanguages.add(type);
        } else {
          //System.out.println("SKIPPING " + type);
          continue;
        }
        String scripts = parts.getAttributeValue(-1, "scripts");
        if (scripts != null) {
          Set<String> scriptSet = new TreeSet(Arrays.asList(scripts
                  .split("\\s+")));
          for (String script : scriptSet) {
            if (officialModernScripts.contains(script)) {
              modernScripts.add(script);
              language_scripts.put(type, script);
            } else {
              System.out.println("SKIPPING " + script);
              continue;
            }
          }
        }
//        String territories = parts.getAttributeValue(-1, "territories");
//        if (territories != null) {
//          Set<String> territorySet = new TreeSet(Arrays
//              .asList(territories
//                  .split("\\s+")));
//          for (String territory : territorySet) {
//            if (officialModernTerritories.contains(territory)) {
//              modernTerritories.add(territory);
//            } else {
//              System.out.println("*SKIPPING " + type);
//            }
//          }
          //Utility.addTreeMapChain(language_territories, type, territorySet);
          //addAllToCollectionValue(territory_languages, territorySet, type, ArrayList.class);
        //}
      } else if (parts.containsElement("currencyData") && lastElement.equals("currency")) {
        //         <region iso3166="AM"><currency iso4217="AMD" from="1993-11-22"/>
        // if the 'to' value is less than 10 years, it is not modern
        String to = parts.getAttributeValue(-1, "to");
        String currency = parts.getAttributeValue(-1, "iso4217");
        if (to == null || to.compareTo(lastCurrencyYear) >= 0) {
          modernCurrencies.add(currency);
          // only add current currencies to must have list
          if (to == null) {
            String region = parts.getAttributes(-2).get("iso3166");
            Set currencies = (Set) territory_currency.get(region);
            if (currencies == null) territory_currency.put(region, currencies = new TreeSet());
            currencies.add(currency);
          }
        }
      } else if (parts.containsElement("territoryContainment")) {
        if (!type.equals("172")) {
          territoryContainment.add(type);
        }
        if (type.equals(EUROPEAN_UNION)) {
          euroCountries = new TreeSet(Arrays.asList((parts.getAttributeValue(-1, "contains")).split("\\s+")));
        }
      }
    }

    
//    Set<String> old = modernLanguages;
//    modernCurrencies = new TreeSet();
//    for (String language : old) {
//      Type type = Iso639Data.getType(language);
//      if (type == Type.Living) {
//        //System.out.println("*Adding " + language + "\t" + Iso639Data.getNames(language));
//        modernLanguages.add(language);
//      } else {
//        System.out.println("*Skipping " + language + "\t" + Iso639Data.getNames(language));
//      }
//    }
//    if (false) for (String language : Iso639Data.getAvailable()) {
//      if (old.contains(language)) continue;
//      if (Iso639Data.getSource(language) == Source.ISO_639_3) continue;
//      Type type = Iso639Data.getType(language);
//      if (type == Type.Living) {
//        System.out.println("**Living " + language + "\t" + Iso639Data.getNames(language));
//      } else {
//        System.out.println("**Nonliving " + language + "\t" + Iso639Data.getNames(language));
//      }
//    }
    
    if(euroCountries == null) {
      euroCountries = new TreeSet(); // placate other parts of the code
      euroCountriesMissing = true;
    }    
  }
  public void checkPosixCoverage(String path, String fullPath, String value,
      Map options, List result, CLDRFile file, CLDRFile resolved) {
    
    // skip all items that are in anything but raw codes
    String source = resolved.getSourceLocaleID(path, null);
    if (!source.equals(XMLSource.CODE_FALLBACK_ID) && !(source.equals("root") && isValueCode(fullPath, value))){
      return;
    }
    
    if(path == null) { 
      throw new InternalCldrException("Empty path!");
    } else if(file == null) {
      throw new InternalCldrException("no file to check!");
    }
    //parts.set(fullPath);
    //parts.equals()
    // check to see if the level is good enough
    CoverageLevel.Level level = (CoverageLevel.Level) posixCoverage.get(fullPath);
    
    if (level==null || level == CoverageLevel.Level.UNDETERMINED) return; // continue if we don't know what the status is
    if (Level.POSIX.compareTo(level) >= 0) {
      result.add(new CheckStatus().setMainType(CheckStatus.errorType).setSubtype(Subtype.insufficientCoverage)
          .setMessage("Needed to meet {0} coverage level.", new Object[] { level }));
    }
  }
  private boolean isValueCode(String xpath, String value){
    try{
      Integer.parseInt(value);
      if(xpath.indexOf("nativeZeroDigit")>0){
        return false;
      }
      return true;
    }catch(NumberFormatException ex){
      
    }
    return false;
  }
}
