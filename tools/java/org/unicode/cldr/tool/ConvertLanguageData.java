package org.unicode.cldr.tool;


import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SpreadSheet;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.LocaleIDParser.Level;
import org.unicode.cldr.util.XPathParts.Comments;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ConvertLanguageData {
  
  static final double populationFactor = 1;
  static final double gdpFactor = 1;
  static final int COUNTRY_CODE = 2, LANGUAGE_POPULATION = 3, LANGUAGE_LITERACY = 4, LANGUAGE_CODE = 6, COUNTRY_POPULATION = 8, COUNTRY_LITERACY = 9, COUNTRY_GDP = 10;
  static final Map<String, CodeAndPopulation> languageToMaxCountry = new TreeMap<String, CodeAndPopulation>();
  static final Map<String, CodeAndPopulation> languageToMaxScript = new TreeMap<String, CodeAndPopulation>();
  static Map<String,String> defaultContent = new TreeMap<String,String>();

  static CLDRFile english;
  static Set locales;
  static Factory cldrFactory;
  static Set skipLocales = new HashSet(Arrays.asList("sh sh_BA sh_CS sh_YU characters supplementalData supplementalData-old supplementalData-old2 supplementalData-old3 supplementalMetadata root".split("\\s")));
  
  static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
  static NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);

  static class LanguageInfo {
    static LanguageInfo INSTANCE = new LanguageInfo();
    
    Map<String,Set<String>> languageToScripts = new TreeMap<String,Set<String>>();
    Map<String,Set<String>> languageToRegions = new TreeMap<String,Set<String>>();
    Map<String,Comments> languageToComments = new TreeMap<String,Comments>();

    Map<String,Set<String>> languageToScriptsAlt = new TreeMap<String,Set<String>>();
    Map<String,Set<String>> languageToRegionsAlt = new TreeMap<String,Set<String>>();
    Map<String,Comments> languageToCommentsAlt = new TreeMap<String,Comments>();

    private LanguageInfo() {
      cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
      Set available = cldrFactory.getAvailable();
      CLDRFile supplemental = cldrFactory.make("supplementalData", true);
      XPathParts parts = new XPathParts();
      for (Iterator it = supplemental.iterator("//supplementalData/languageData/language"); it.hasNext();) {
        String xpath = (String) it.next();
        Map x = parts.set(xpath).getAttributes(-1);
        boolean alt = x.containsKey("alt");
        String lang = (String) x.get("type");
        List scripts = getAttributeList(x, "scripts");
        if (scripts != null) {
          if (alt) {
            putAll(languageToScriptsAlt, lang, new LinkedHashSet(scripts));
          } else {
            putAll(languageToScripts, lang, new LinkedHashSet(scripts));        
          }
        }
        List regions = getAttributeList(x, "territories");
        if (regions != null) {
          if (alt) {
            putAll(languageToRegionsAlt, lang, new LinkedHashSet(regions));
          } else {
            putAll(languageToRegions, lang, new LinkedHashSet(regions));
          }
        }
      }
    }

    private List getAttributeList(Map x, String attribute) {
      List scripts = null;
      String scriptString = (String) x.get(attribute);
      if (scriptString != null) {
        scripts = Arrays.asList(scriptString.split("\\s+"));
      }
      return scripts;
    }
  }
  
  private static <K,V> void putUnique(Map<K, V> map, K key, V value) {
    V oldValue = map.get(key);
    if (oldValue != null && !oldValue.equals(value)) {
      throw new IllegalArgumentException("Duplicate value for <" + key + ">: <" + oldValue + ">, <" + value + ">");
    }
    map.put(key, value);
  }
  
  private static <K, W> void putAll(Map<K, Set<W>> map, K key, Set<W> values) {
    Set<W> oldValue = map.get(key);
    if (oldValue == null) {
      map.put(key, values);
    } else {
      oldValue.addAll(values);
    }
  }

  static class RowData implements Comparable {
    String countryCode;
    double countryGdp;
    double countryLiteracy;
    double countryPopulation;
    String languageCode;
    double languagePopulation;
    double languageLiteracy;
    
    RowData(List<String> row) throws ParseException {
      countryCode = row.get(COUNTRY_CODE);
      languagePopulation = nf.parse(row.get(LANGUAGE_POPULATION)).doubleValue();;
      languageCode = row.get(LANGUAGE_CODE);
      countryPopulation = nf.parse(row.get(COUNTRY_POPULATION)).doubleValue();
      countryGdp = nf.parse(row.get(COUNTRY_GDP)).doubleValue();
      countryLiteracy = pf.parse(row.get(COUNTRY_LITERACY)).doubleValue();
      String stringLanguageLiteracy = row.get(LANGUAGE_LITERACY);
      languageLiteracy = stringLanguageLiteracy.length() == 0 
          ? countryLiteracy 
          : pf.parse(stringLanguageLiteracy).doubleValue();
    }
    public double getLanguageLiteratePopulation() {
      return languageLiteracy * languagePopulation;
    }
    static final Comparator GENERAL_COLLATOR = new GeneralCollator();
    static final Comparator INVERSE_GENERAL = new InverseComparator(GENERAL_COLLATOR);
    public int compareTo(Object o) {
      RowData that = (RowData)o;
      int result;
      if (0 != (result = GENERAL_COLLATOR.compare(countryCode,that.countryCode))) return result;
      if (languagePopulation > that.languagePopulation) return -1; // descending
      if (languagePopulation < that.languagePopulation) return 1;
      if (0 != (result = GENERAL_COLLATOR.compare(languageCode,that.languageCode))) return result;
      return 0;
    }
    public String toString() {
      return countryCode + "\t" + languagePopulation + "\t" + languageCode + "\t" + countryPopulation + "\t" + countryGdp
      + "\t" + languageLiteracy
      + "\t" + countryLiteracy;
    }
  }

  public static void main(String[] args) throws IOException, ParseException {
    // load elements we care about
    cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    Set available = cldrFactory.getAvailable();

    Set<String> cldrParents = getCldrParents(available);
    
    List<String> failures = new ArrayList<String>();
    Map<String,RowData> localeToRowData = new TreeMap<String,RowData>();

    Set<RowData> sortedInput = getExcelData(failures, localeToRowData);
 
    // TODO sort by country code, then functionalPopulation, then language code
    // and keep the top country for each language code (even if < 1%)
    
    writeTerritoryLanguageData(failures, sortedInput);
    
    showDefaults(cldrParents, nf, defaultContent, localeToRowData);
    
    //showContent(available);

    showFailures(failures);
  }

  private static void writeTerritoryLanguageData(List<String> failures, Set<RowData> sortedInput) {
    
    System.out.println();
    System.out.println("Territory Language Data");
    System.out.println();

    String lastCountryCode = "";
    boolean first = true;
    LanguageTagParser ltp = new LanguageTagParser();
    for (RowData row : sortedInput) {
        String countryCode = row.countryCode;
        
        double countryPopulationRaw = row.countryPopulation;
        long countryPopulation = (long) roundToDecimals(countryPopulationRaw, 2);
        double languageLiteracy = row.languageLiteracy*100;
        double countryLiteracy = row.countryLiteracy*100;
        
        double countryGDPRaw = row.countryGdp;
        long countryGDP = Math.round(countryGDPRaw/gdpFactor);
        
        String languageCode = row.languageCode;
        
        double languagePopulationRaw = row.languagePopulation;
        long languagePopulation = (long) roundToDecimals(languagePopulationRaw, 2);
        
        double languagePopulationPercent = roundToDecimals(Math.min(100, Math.max(0, 
                languagePopulation*100 / (double)countryPopulation)),3);

        if (!countryCode.equals(lastCountryCode)) {
          if (first) {
            first = false;
          } else {
            System.out.println("\t\t</territory>");
          }
          System.out.print("\t\t<territory type=\"" + countryCode + "\""
              + " gdp=\"" + countryGDP + "\""
              + " literacyPercent=\"" + nf.format(countryLiteracy) + "\""
              + " population=\"" + countryPopulation + "\">");
          lastCountryCode = countryCode;
          System.out.println("\t<!--" + ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH) + "-->");
        }
          
        if (languageCode.length() != 0 
            && (languagePopulationPercent >= 1 || languagePopulationRaw > 100000 || languageCode.equals("haw"))
            ) {
          // add best case
          addBestRegion(languageCode, countryCode, languagePopulationRaw);
          String baseScriptLanguage = ltp.set(languageCode).getLanguageScript();
          if (!baseScriptLanguage.equals(languageCode)) {
            addBestRegion(baseScriptLanguage, countryCode, languagePopulationRaw);
          }
          String baseLanguage = ltp.set(baseScriptLanguage).getLanguage();
          if (!baseLanguage.equals(baseScriptLanguage)) {
            addBestRegion(baseLanguage, countryCode, languagePopulationRaw);
            addBestScript(baseLanguage, ltp.set(languageCode).getScript(), languagePopulationRaw);
          }

          System.out.print("\t\t\t<languagePopulation type=\"" + languageCode + "\""
              + (languageLiteracy != countryLiteracy ? " literacyPercent=\"" + nf.format(languageLiteracy) + "\"" : "")
              + " populationPercent=\"" + nf.format(languagePopulationPercent) + "\""
              + "/>");
          System.out.println("\t<!--" + ULocale.getDisplayName(languageCode, ULocale.ENGLISH) + "-->");
        } else {
          failures.add(row + "\tLess than 1% or no language code");
        }
        //if (first) {
          if (false) System.out.print(
              "countryCode: " + countryCode + "\t"
              + "countryPopulation: " + countryPopulation + "\t"
              + "countryGDP: " + countryGDP + "\t"
              + "languageCode: " + languageCode + "\t"
              + "languagePopulation: " + languagePopulation + "\r\n"
              );
        //}
    }
    
    System.out.println("\t\t</territory>");
  }

  private static Set<RowData> getExcelData(List<String> failures, Map<String,RowData> localeToRowData) throws IOException {
    System.out.println();
    System.out.println("Get Excel Data");
    System.out.println();

    List<List<String>> input = SpreadSheet.convert("C:\\Documents and Settings\\markdavis\\My Documents\\" +
        "Excel Stuff\\countryLanguagePopulation\\country_language_population2.txt");
    
    Set<RowData> sortedInput = new TreeSet();
    int count = 0;
    for (List<String> row : input) {
      ++count;
      if (count == 1 || row.size() <= COUNTRY_GDP) {
        failures.add(join(row,"\t") + "\tShort row");
        continue;
      }
      try {
        RowData x = new RowData(row);
        localeToRowData.put(x.languageCode + "_" + x.countryCode, x);
        sortedInput.add(x);
      } catch (ParseException e) {
        failures.add(join(row,"\t") + "\t" + join(Arrays.asList(e.getStackTrace()),";\t"));
      }
    }
    return sortedInput;
  }

  private static Set<String> getCldrParents(Set available) {
    LanguageTagParser ltp2 = new LanguageTagParser();
    Set<String> cldrParents = new TreeSet<String>();
    for (String locale : (Set<String>) available) {
      if (skipLocales.contains(locale)) continue;
      try {
        ltp2.set(locale);
      } catch (RuntimeException e) {
        System.out.println("Skipping CLDR file: " + locale);
        continue;
      }
      String locale2 = ltp2.getLanguageScript();
      if (locale2.equals("sh")) continue;
      //int lastPos = locale.lastIndexOf('_');
      //if (lastPos < 0) continue;
      //String locale2 = locale.substring(0,lastPos);
      cldrParents.add(locale2);
      languageToMaxCountry.put(locale2,null);
    }
    System.out.println("CLDR Parents: " + cldrParents);
    return cldrParents;
  }

  private static void showFailures(List<String> failures) {
    System.out.println();
    System.out.println("Failures");
    System.out.println();
    
    for (String failure : failures) {
      System.out.println(failure);
    }
  }

  private static void showContent(Set available) {
    System.out.println();
    System.out.println("CLDR Content");
    System.out.println();
    Set<String> languagesLeft = new TreeSet<String>(defaultContent.keySet());
    languagesLeft.remove("und");
    for (String languageLeft : languagesLeft) {
      System.out.println("\t\t<defaultContent type=\"" + languageLeft + "\" content=\"" + defaultContent.get(languageLeft) + "\"/>");
    }
//    Set<String> warnings = new LinkedHashSet<String>();
//    
//    CLDRFile supplemental = cldrFactory.make("supplementalData", true);
//    Comments tempComments = supplemental.getXpath_comments();
//    PrintWriter pw = new PrintWriter(System.out);
//    Comparator attributeOrdering = supplemental.getAttributeComparator();
//    Map defaultSuppressionMap = supplemental.getDefaultSuppressionMap();
//    
//    XPathParts last = new XPathParts(attributeOrdering, defaultSuppressionMap);
//    XPathParts current = new XPathParts(attributeOrdering, defaultSuppressionMap);
//    XPathParts lastFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
//    XPathParts currentFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
//
//    Set orderedSet = new TreeSet(supplemental.ldmlComparator);
//    CollectionUtilities.addAll(supplemental.iterator("//supplementalData/languageData/language"), orderedSet);
//    Set<String> languagesLeft = new TreeSet<String>(defaultContent.keySet());
//    
//    for (Iterator it2 = orderedSet.iterator(); it2.hasNext();) {
//      String xpath = (String)it2.next();
//      currentFiltered.set(xpath);
//      current.set(xpath);
//      
//      Map x = current.set(xpath).getAttributes(-1);
//      boolean alt = x.containsKey("alt");
//      String lang = (String) x.get("type");
//      String defaultValue = defaultContent.get(lang);
//      if (alt) {
//        // skip
//      } else if (defaultValue == null) {
//        warnings.add("Missing default value for " + lang);
//      } else if (!defaultValue.equals(lang)) {
//        x.put("defaultContent", defaultValue);
//        languagesLeft.remove(lang);
//      }
//
//      current.writeDifference(pw, currentFiltered, last, lastFiltered, "", tempComments);
//      // exchange pairs of parts
//      XPathParts temp = current;
//      current = last;
//      last = temp;
//      temp = currentFiltered;
//      currentFiltered = lastFiltered;
//      lastFiltered = temp;
//    }
//    pw.flush();
    
    
//    for (String warning : warnings) {
//      System.out.println(warning);
//    }

    
//    for (String localeCode : (Set<String>) available) {
//      if (skipLocales.contains(localeCode)) continue;
//      String resolvedLanguageCode = getFullyResolved(localeCode);
//     // a locale will be empty if its parent has the same resolved code
//      String parent = getProcessedParent(localeCode);
//      String resolvedParent = getFullyResolved(parent);
//      System.out.println(
//          (resolvedLanguageCode.equals(resolvedParent) ? "empty" : "")
//          + "\t" + localeCode 
//          + "\t" + resolvedLanguageCode 
//          + "\t" + parent 
//          + "\t" + ULocale.getDisplayName(localeCode, ULocale.ENGLISH));
//    }
  }
  
  public static String getProcessedParent(String localeCode) {
    if (localeCode == null || localeCode.equals("root")) return null;
    int pos = localeCode.lastIndexOf('_');
    if (pos < 0) return "root";
    LanguageTagParser ltp = new LanguageTagParser();
    String script = ltp.set(localeCode).getScript();
    if (script.length() == 0) {
      return getFullyResolved(localeCode);
    }
    return localeCode.substring(0,pos);
  }

  
  private static double roundToDecimals(double input, int places) {
    double log10 = Math.log10(input); // 15000 => 4.xxx
    double intLog10 = Math.floor(log10);
    double scale = Math.pow(10, intLog10 - places + 1);
    double factored = Math.round(input / scale) * scale;
    //System.out.println("###\t" +input + "\t" + factored);
    return factored;
  }

  private static String getFullyResolved(String languageCode) {
    String result = defaultContent.get(languageCode);
    if (result != null) return result;
    // we missed. Try taking parent and trying again
    int pos = languageCode.length() + 1;
    while (true) {
      pos = languageCode.lastIndexOf('_', pos-1);
      if (pos < 0) {
        return "***" + languageCode;
      }
      result = defaultContent.get(languageCode.substring(0,pos));
      if (result != null) {
        LanguageTagParser ltp = new LanguageTagParser().set(languageCode);
        LanguageTagParser ltp2 = new LanguageTagParser().set(result);
        String region = ltp.getRegion();
        if (region.length() == 0) {
          ltp.setRegion(ltp2.getRegion());
        }
        String script = ltp.getScript();        
        if (script.length() == 0) {
          ltp.setScript(ltp2.getScript());
        }
        return ltp.toString();
      }
    }
  }
  
  static Comparator<Iterable> firstElementComparator = new Comparator<Iterable>() {
    public int compare(Iterable o1, Iterable o2) {
      int result = ((Comparable)o1.iterator().next()).compareTo(((Comparable)o2.iterator().next()));
      assert result != 0;
      return result;
    }  
  };

  private static void showDefaults(Set<String> cldrParents, NumberFormat nf, Map<String,String> defaultContent, Map<String, RowData> localeToRowData) {

    System.out.println();
    System.out.println("Defaults");
    System.out.println();

    Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    Set<String> locales = cldrFactory.getAvailable();
    // get sets of siblings
    LocaleIDParser lidp = new LocaleIDParser();
    Set<Set<String>> siblingSets  = new TreeSet<Set<String>>(firstElementComparator);
    Set<String> needsADoin = new TreeSet<String>(locales);
    
    Set<String> deprecatedLanguages = new TreeSet();
    deprecatedLanguages.add("sh");
    Set<String> deprecatedRegions = new TreeSet();
    deprecatedRegions.add("YU");
    deprecatedRegions.add("CS");

    // first find all the language subtags that have scripts, and those we need to skip. Those are aliased-only
    Set<String> skippingItems = new TreeSet();
    Set<String> hasAScript = new TreeSet();
    Set<LocaleIDParser.Level> languageOnly = EnumSet.of(LocaleIDParser.Level.Language);
    for (String locale : locales) {
      lidp.set(locale);
      if (lidp.getScript().length() != 0) {
        hasAScript.add(lidp.getLanguage());
      }
      Set<LocaleIDParser.Level> levels = lidp.getLevels();
      // must have no variants, must have either script or region, no deprecated elements
      if (levels.contains(LocaleIDParser.Level.Variants) // no variants
          || !(levels.contains(LocaleIDParser.Level.Script) || levels.contains(LocaleIDParser.Level.Region))
          || deprecatedLanguages.contains(lidp.getLanguage()) 
          || deprecatedRegions.contains(lidp.getRegion())) {
        // skip language-only locales, and ones with variants
        needsADoin.remove(locale);
        skippingItems.add(locale);
        continue;
      }
    }
    // walk through the locales, getting the ones we care about.
    Map<String,Double> scriptLocaleToLanguageLiteratePopulation = new TreeMap();
    
    for (String locale : new TreeSet<String>(needsADoin)) {
      if (!needsADoin.contains(locale)) continue;
      lidp.set(locale);
      Set<Level> level = lidp.getLevels();
      // skip locales that need scripts and don't have them
      if (!level.contains(LocaleIDParser.Level.Script) // no script
          && hasAScript.contains(lidp.getLanguage())) {
        needsADoin.remove(locale);
        skippingItems.add(locale);
        continue;
      }
      // get siblings
      Set<String> siblingSet = lidp.getSiblings(needsADoin);
      // if it has a script and region
      if (level.contains(LocaleIDParser.Level.Script) && level.contains(LocaleIDParser.Level.Region)) {
        double languageLiteratePopulation = 0;
        for (String localeID2 : siblingSet) {
          RowData rowData = localeToRowData.get(localeID2);
          if (rowData != null) {
            languageLiteratePopulation += rowData.getLanguageLiteratePopulation();
          }
        }
        String parentID = lidp.getParent();
        scriptLocaleToLanguageLiteratePopulation.put(parentID, languageLiteratePopulation);
      }

      try {
        siblingSets.add(siblingSet);
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
      needsADoin.removeAll(siblingSet);
    }
    System.out.println("Skipping: " + skippingItems);
    if (needsADoin.size() != 0) {
      System.out.println("Missing: " + needsADoin);
    }
    
    // walk through the data
    Set<String> skippingSingletons = new TreeSet();
    Set<String> defaultLocaleContent = new TreeSet();
    
    Set<String> missingData = new TreeSet();
    for (Set<String> siblingSet : siblingSets) {
      System.out.println("***" + siblingSet);
      
      if (false & siblingSet.size() == 1) {
        skippingSingletons.add(siblingSet.iterator().next());
        continue;
      }
      // get best
      double best = Double.NEGATIVE_INFINITY;
      String bestLocale = "???";
      for (String locale : siblingSet) {
        RowData rowData = localeToRowData.get(locale);
        double languageLiteratePopulation = -1;
        if (rowData != null) {
          languageLiteratePopulation = rowData.getLanguageLiteratePopulation();
        } else {
          Double d = scriptLocaleToLanguageLiteratePopulation.get(locale);
          if (d != null) {
            languageLiteratePopulation = d;
          } else {
            missingData.add(locale);
          }
        }
        if (best < languageLiteratePopulation) {
          best = languageLiteratePopulation;
          bestLocale = locale;
        }
      }
      // show it
      System.out.format("\t%s %f\r\n", bestLocale, best);
      defaultLocaleContent.add(bestLocale);
    }
    
    System.out.format("Skipping Singletons %s\r\n", skippingSingletons);
    System.out.format("Missing Data %s\r\n", missingData);
    String sep = "\r\n\t\t\t";
    String broken = Utility.breakLines(join(defaultLocaleContent," "), sep, Pattern.compile("(\\S)\\S*").matcher(""), 80);
    
    System.out.println("\t\t<defaultContent locales=\"" + broken + "\"/>");

//    LanguageTagParser ltp = new LanguageTagParser();
//    Set<String> warnings = new LinkedHashSet();
//    for (String languageCode : languageToMaxCountry.keySet()) {
//      CodeAndPopulation best = languageToMaxCountry.get(languageCode);
//      String languageSubtag = ltp.set(languageCode).getLanguage();
//      String countryCode = "ZZ";
//      double rawLanguagePopulation = -1;
//      if (best != null) {
//        countryCode = best.code;
//        rawLanguagePopulation = best.population;
//        Set<String> regions = LanguageInfo.INSTANCE.languageToRegions.get(languageSubtag);
//        if (regions == null || !regions.contains(countryCode)) {
//          Set<String> regions2 = LanguageInfo.INSTANCE.languageToRegionsAlt.get(languageSubtag);
//          if (regions2 == null || !regions2.contains(countryCode)) {
//            warnings.add("WARNING: " + languageCode + " => " + countryCode + ", not in " + regions + "/" + regions2);
//          }
//        }
//      }
//      String resolvedLanguageCode = languageCode + "_" + countryCode;
//      ltp.set(languageCode);
//      Set<String> scripts = LanguageInfo.INSTANCE.languageToScripts.get(languageCode);
//      String script = ltp.getScript();
//      if (script.length() == 0) {
//        CodeAndPopulation bestScript = languageToMaxScript.get(languageCode);
//        if (bestScript != null) {
//          script = bestScript.code;
//          if (scripts == null || !scripts.contains(script)) {
//            warnings.add("WARNING: " + languageCode + " => " + script + ", not in " + scripts);
//          }
//        } else {
//            script = "Zzzz";
//            if (scripts == null) {
//              scripts = LanguageInfo.INSTANCE.languageToScriptsAlt.get(languageCode);
//            }
//            if (scripts != null) {
//              script = scripts.iterator().next();
//              if (scripts.size() != 1) {
//                warnings.add("WARNING: " + languageCode + " => " + scripts);
//              }
//            }
//        }
//        if (scripts == null) {
//          warnings.add("Missing scripts for: " + languageCode);
//        } else if (scripts.size() == 1){
//          script = "";
//        }
//        resolvedLanguageCode = languageCode 
//        + (script.length() == 0 ? "" : "_" + script) 
//        + "_" + countryCode;
//      }
//      
//
//      System.out.println(
//          resolvedLanguageCode
//          + "\t" + languageCode
//          + "\t" + ULocale.getDisplayName(languageCode, ULocale.ENGLISH)
//          + "\t" + countryCode
//          + "\t" + ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH)
//          + "\t" + nf.format(rawLanguagePopulation)
//          + (cldrParents.contains(languageCode) ? "\tCLDR" : "")
//          );
//      if (languageCode.length() == 0) continue;
//      defaultContent.put(languageCode, resolvedLanguageCode);
//    }
//    for (String warning : warnings) {
//      System.out.println(warning);
//    }
  }
  
  private static Object getSuppressScript(String languageCode) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String join (Collection c, String separator) {
    StringBuffer result = new StringBuffer();
    boolean first = true;
    for (Object x : c) {
      if (first) first = false;
      else result.append(separator);
      result.append(x);
    }
    return result.toString();
  }

  private static void addBestRegion(String languageCode, String countryCode, double languagePopulationRaw) {
    addBest(languageCode, languagePopulationRaw, countryCode, languageToMaxCountry);
  }
  
  private static void addBestScript(String languageCode, String scriptCode, double languagePopulationRaw) {
    addBest(languageCode, languagePopulationRaw, scriptCode, languageToMaxScript);
  }
  
  private static void addBest(String languageCode, double languagePopulationRaw, String code, Map<String, CodeAndPopulation> languageToMaxCode) {
    if (languageCode.length() == 0) {
      throw new IllegalArgumentException();
    }
    CodeAndPopulation best = languageToMaxCode.get(languageCode);
    if (best == null) {
      languageToMaxCode.put(languageCode, best = new CodeAndPopulation());
    } else if (best.population >= languagePopulationRaw) {
      return;
    }
    best.population = languagePopulationRaw;
    best.code = code;
  }
  
  static class CodeAndPopulation {
    String code = null;
    double population = Double.NaN;
    public String toString() {
      return "{" + code + "," + population + "}";
    }
  }
  
  static public class GeneralCollator implements Comparator {
    static UTF16.StringComparator cpCompare = new UTF16.StringComparator(true, false,0);
    static RuleBasedCollator UCA = (RuleBasedCollator) Collator
        .getInstance(ULocale.ROOT);
    static {
      UCA.setNumericCollation(true);
    }

    public int compare(Object o1, Object o2) {
      if (o1 == null) {
        return o2 == null ? 0 : -1;
      } else if (o2 == null) {
        return 1;
      }
      String s1 = o1.toString();
      String s2 = o2.toString();
      int result = UCA.compare(s1, s2);
      if (result != 0) return result;
      return cpCompare.compare(s1, s2);
    }
  };

  public static class InverseComparator implements Comparator {
    private Comparator other;

    public InverseComparator(Comparator other) {
      if (other == null) throw new NullPointerException();
      this.other = other;
    }

    public int compare(Object a, Object b) {
      return other.compare(b, a);
    }
  }

}