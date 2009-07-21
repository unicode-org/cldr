package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.Row;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Row.R2;
import org.unicode.cldr.util.Row.R3;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Problems:
 *    "und_Hani",      "zh_Hani"
 *    "und_Sinh",     "si_Sinh"
 * @author markdavis
 *
 */
@SuppressWarnings("unchecked")
public class GenerateMaximalLocales {

  private static final boolean SHOW_ADD = CldrUtility.getProperty("GenerateMaximalLocalesDebug", false);
  enum OutputStyle {PLAINTEXT, C, C_ALT, XML};

  private static OutputStyle OUTPUT_STYLE = OutputStyle.valueOf(CldrUtility.getProperty("OutputStyle", "XML", "XML").toUpperCase());

  // set based on above
  private static final String SEPARATOR = OUTPUT_STYLE == OutputStyle.C  || OUTPUT_STYLE == OutputStyle.C_ALT ? CldrUtility.LINE_SEPARATOR : "\t";
  private static final String TAG_SEPARATOR = OUTPUT_STYLE == OutputStyle.C_ALT ? "-" : "_";
  //  private static final boolean FAVOR_REGION = true; // OUTPUT_STYLE == OutputStyle.C_ALT;

  private static final boolean tryDifferent = true;

  private static Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
  private static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
  private static StandardCodes standardCodes = StandardCodes.make();
  private static CLDRFile english = factory.make("en", false);

  private static int errorCount;

  public static void main(String[] args) throws IOException {
    
    printDefaultLanguagesAndScripts();

    Map<String,String> toMaximized = new TreeMap<String,String>();

    if (tryDifferent) {
      tryDifferentAlgorithm(toMaximized);

    } else {
      throw new IllegalArgumentException();
      //oldAlgorithm(toMaximized);
    }

    minimize(toMaximized);

    if (OUTPUT_STYLE == OutputStyle.C_ALT) {
      doAlt(toMaximized);
    }

    if (SHOW_ADD) System.out.println("/*" + CldrUtility.LINE_SEPARATOR + " To Maximize:" +
            CldrUtility.LINE_SEPARATOR + " If using raw strings, make sure the input language/locale uses the right separator, and has the right casing." +
            CldrUtility.LINE_SEPARATOR + " Remove the script Zzzz and the region ZZ if they occur; change an empty language subtag to 'und'." +
            CldrUtility.LINE_SEPARATOR + " Get the language, region, and script from the cleaned-up tag, plus any variants/extensions" +
            CldrUtility.LINE_SEPARATOR + " Try each of the following in order (where the field exists)" +
            CldrUtility.LINE_SEPARATOR + "   Lookup language-script-region. If in the table, return the result + variants" +
            CldrUtility.LINE_SEPARATOR + "   Lookup language-script. If in the table, return the result (substituting the original region if it exists) + variants" +
            CldrUtility.LINE_SEPARATOR + "   Lookup language-region. If in the table, return the result (substituting the original script if it exists) + variants" +
            CldrUtility.LINE_SEPARATOR + "   Lookup language. If in the table, return the result (substituting the original region and script if either or both exist) + variants" +
            CldrUtility.LINE_SEPARATOR +
            CldrUtility.LINE_SEPARATOR + " Example: Input is zh-ZZZZ-SG." +
            CldrUtility.LINE_SEPARATOR + " Normalize to zh-SG. Lookup in table. No match." +
            CldrUtility.LINE_SEPARATOR + " Remove SG, but remember it. Lookup zh, and get the match (zh-Hans-CN). Substitute SG, and return zh-Hans-SG." +
            CldrUtility.LINE_SEPARATOR +
            CldrUtility.LINE_SEPARATOR + " To Minimize:" +
            CldrUtility.LINE_SEPARATOR + " First get max = maximize(input)." +
            CldrUtility.LINE_SEPARATOR + " Then for trial in {language, language-region, language-script}" +
            CldrUtility.LINE_SEPARATOR + "     If maximize(trial) == max, then return trial." +
            CldrUtility.LINE_SEPARATOR + " If you don't get a match, return max." +
            CldrUtility.LINE_SEPARATOR +
            CldrUtility.LINE_SEPARATOR + " Example: Input is zh-Hant. Maximize to get zh-Hant-TW." +
            CldrUtility.LINE_SEPARATOR + " zh => zh-Hans-CN. No match, so continue." +
            CldrUtility.LINE_SEPARATOR + " zh-TW => zh-Hans-TW. Match, so return zh-TW." +
            CldrUtility.LINE_SEPARATOR +
            CldrUtility.LINE_SEPARATOR + " (A variant of this uses {language, language-script, language-region}): that is, tries script before language." +
            CldrUtility.LINE_SEPARATOR + " toMaximal size:\t" + toMaximized.size() + 
            CldrUtility.LINE_SEPARATOR + "*/"
    );

    printMap(toMaximized);

    //  if (OUTPUT_STYLE != OutputStyle.XML) {
    //  printMap("const MapToMinimalSubtags default_subtags[]", toMinimized, null);
    //  }

    printDefaultContent(toMaximized);

    System.out.println(CldrUtility.LINE_SEPARATOR + "ERRORS:\t" + errorCount + CldrUtility.LINE_SEPARATOR);


  }

  private static void printDefaultLanguagesAndScripts() {
    final int minTotalPopulation = 10000000;
    final int minTerritoryPopulation = 1000000;
    final double minTerritoryPercent = 1.0/3;
    Map<String,Set<R3<OfficialStatus, String, Long>>> languageToReason = new TreeMap();
    Counter<String> languageToLiteratePopulation = new Counter<String>();
    NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
    nf.setGroupingUsed(true);
    LanguageTagParser ltp = new LanguageTagParser();
    /*
     * A. X is a qualified language**, and at least one of the following is true:
     * 
     * 1. X is has official status* in any country 
     * 2. X exceeds a threshold population† of literate users worldwide: 1M 
     * 3. X exceeds a threshold population† in some country Z: 100K and 20% of Z's population†.
     * 
     * B. X is an exception explicitly approved by the committee or X has minimal
     * language coverage‡ in CLDR itself.
     */
    for (String territory : supplementalData.getTerritoriesWithPopulationData()) {
      PopulationData territoryPop = supplementalData.getPopulationDataForTerritory(territory);
      double territoryPopulation = territoryPop.getLiteratePopulation();
      for (String languageScript : supplementalData.getLanguagesForTerritoryWithPopulationData(territory)) {
        PopulationData popData = supplementalData.getLanguageAndTerritoryPopulationData(languageScript, territory);
        String language = ltp.set(languageScript).getLanguage();
        boolean add = false;
        // #1
        OfficialStatus status = popData.getOfficialStatus();
        if (status != OfficialStatus.unknown) {
          add = true;
        }
        final long literatePopulation = (long) popData.getLiteratePopulation();
        // #2
        languageToLiteratePopulation.add(language, literatePopulation);
        // #3
        if (literatePopulation > minTerritoryPopulation && literatePopulation > minTerritoryPercent * territoryPopulation) {
          add = true;
        }
        if (add) {
          add(languageToReason, language, territory, status, literatePopulation);
        }
      }
    }
    // #2, now that we have the data
    for (String language : languageToLiteratePopulation.keySet()) {
      long totalPop = languageToLiteratePopulation.getCount(language);
      if (totalPop > minTotalPopulation) {
        add(languageToReason, language, "001", OfficialStatus.unknown, totalPop);
      }
    }
    
    // Specials
    add(languageToReason, "und", "001", OfficialStatus.unknown, 0);

//    for (String language : Iso639Data.getAvailable()) {
//      Scope scope = Iso639Data.getScope(language);
//      Type type = Iso639Data.getType(language);
//      if (scope == Scope.Special) {
//        add(languageToReason, language, "001", OfficialStatus.unknown, -1);
//      }
//    }
    // print them
    
    System.out.println("Detailed Including:\t" + languageToReason.size());

    for (String language : languageToReason.keySet()) {
      Set<R3<OfficialStatus, String, Long>> reasons = languageToReason.get(language);
      System.out.append(getLanguageName(language, languageToReason))
        .append("\t")
        .append(nf.format(languageToLiteratePopulation.getCount(language)));
      for (R3<OfficialStatus, String, Long> reason : reasons) {
        String status = reason.get0().toShortString();
        System.out.append("\t")
        .append(status)
        .append("-")
        .append(reason.get1())
        .append("-")
        .append(nf.format(reason.get2()));
      }
      System.out.append("\n");
    }
    
    // now list them
    
    Set<String> others = new TreeSet();
    others.addAll(standardCodes.getGoodAvailableCodes("language"));
    others.removeAll(languageToReason.keySet());
    System.out.println("\nIncluded Languages:\t" + languageToReason.keySet().size());
    showLanguages(languageToReason.keySet(), languageToReason);
    System.out.println("\nExcluded Languages:\t" + others.size());
    showLanguages(others, languageToReason);
  }

  private static void showLanguages(Set<String> others, Map<String, Set<R3<OfficialStatus, String, Long>>> languageToReason) {
    Set<String> sorted = new TreeSet(Collator.getInstance(ULocale.ENGLISH));
    for (String language : others) {
      sorted.add(getLanguageName(language, languageToReason));
    }
    char last = 0;
    for (String language : sorted) {
      final char curr = language.charAt(0);
      if (last != curr) {
        System.out.println();
      } else if (last != '\u0000') {
        System.out.print(", ");
      }
      System.out.print(language);
      last = curr;
    }
    System.out.println();
  }

  private static String getLanguageName(String language,
          Map<String, Set<R3<OfficialStatus, String, Long>>> languageToReason) {
    OfficialStatus best = OfficialStatus.unknown;
    Set<R3<OfficialStatus, String, Long>> reasons = languageToReason.get(language);
    if (reasons != null) {
      for (R3<OfficialStatus, String, Long> reason : reasons) {
        final OfficialStatus currentStatus = reason.get0();
        if (best.compareTo(currentStatus) < 0) {
          best = currentStatus;
        }
      }
    }
    String status = best.toShortString();
    Scope scope = Iso639Data.getScope(language);
    if (scope == Scope.Special) {
      status = "S";
    }
    String languageFormatted = english.getName(language) + " [" + language + "]-" + status;
    return languageFormatted;
  }

  private static void add(Map<String, Set<R3<OfficialStatus, String, Long>>> languageToReason, String language, 
          String territoryRaw, OfficialStatus status, long population) {
    String territory = english.getName("territory", territoryRaw) + " [" + territoryRaw + "]";
    Set<R3<OfficialStatus, String, Long>> set = languageToReason.get(language);
    if (set == null) {
      languageToReason.put(language, set = new TreeSet<R3<OfficialStatus, String, Long>>());
    }
    set.add(Row.of(status, territory, population));
  }

  private static void printDefaultContent(Map<String, String> toMaximized) throws IOException {

    Set<String> defaultLocaleContent = new TreeSet<String>();

    // go through all the cldr locales, and add default contents
    // now computed from toMaximized
    Set<String> available = factory.getAvailable();
    Relation<String,String> toChildren = new Relation(new TreeMap(), TreeSet.class);
    LanguageTagParser ltp = new LanguageTagParser();

    //System.out.println(maximize("az_Latn_AZ", toMaximized));
    Set<String> hasScript = new TreeSet<String>();

    // first get a mapping to children
    for (String locale : available) {
      if (locale.equals("root")) {
        continue;
      }
      if (ltp.set(locale).getVariants().size() != 0) {
        continue;
      }
      String parent = LanguageTagParser.getParent(locale);
      if (ltp.getScript().length() != 0) {
        hasScript.add(parent);
      }
      if (parent.equals("root")) {
        continue;
      }
      toChildren.put(parent, locale);
    }

    // if any have a script, then throw out any that don't have a script (they're aliases)
    Set<String> toRemove = new TreeSet<String>();
    for (String locale : hasScript) {
      toRemove.clear();
      Set<String> children = toChildren.getAll(locale);
      for (String child : children) {
        if (ltp.set(child).getScript().length() == 0) {
          toRemove.add(child);
        }
      }
      if (toRemove.size() != 0) {
        if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t" + toRemove + "\tfrom\t" + children);
        toChildren.removeAll(locale, toRemove);
      }
    }


    // we add a child as a default locale if it has the same maximization
    main:
      for (String locale : toChildren.keySet()) {
        if (locale.equals("en")) {
          // special case English (because of Deseret)
          defaultLocaleContent.add("en_US");
          continue;
        }
        String maximized = maximize(locale, toMaximized);
        if (maximized == null) {
          if (SHOW_ADD) System.out.println("Missing maximized:\t" + locale);
          continue;
        }
        Set<String> children = toChildren.getAll(locale);
        Map<String,String> debugStuff = new TreeMap<String, String>();
        for (String child : children) {
          String maximizedChild = maximize(child, toMaximized);
          if (maximized.equals(maximizedChild)) {
            defaultLocaleContent.add(child);
            continue main;
          }
          debugStuff.put(child, maximizedChild);
        }
        if (SHOW_ADD) System.out.println("Can't find maximized: " + locale + "=" + maximized
                + "\tin\t" + debugStuff);
      }


    Log.setLogNoBOM(CldrUtility.GEN_DIRECTORY + "/supplemental/supplementalMetadata.xml");
    BufferedReader oldFile = BagFormatter.openUTF8Reader(CldrUtility.SUPPLEMENTAL_DIRECTORY, "supplementalMetadata.xml");
    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*<defaultContent locales=\"\\s*"), Log.getLog(), false);



    String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t";
    String broken = CldrUtility.breakLines(CldrUtility.join(defaultLocaleContent," "), sep, Pattern.compile("(\\S)\\S*").matcher(""), 80);

    Log.println("\t\t<defaultContent locales=\"" + broken + "\"");
    Log.println("\t\t/>");

    //  Log.println("</supplementalData>");
    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*/>\\s*"), null, false);
    CldrUtility.copyUpTo(oldFile, null, Log.getLog(), false);

    Log.close();
    oldFile.close();
  }

  //  private static void oldAlgorithm(Map<String,String> toMaximized) {
  //    Set<String> defaultContentLocales = supplementalData.getDefaultContentLocales();
  //    LanguageTagParser parser = new LanguageTagParser();
  //    for (String locale : defaultContentLocales) {
  //      String parent = parser.getParent(locale);
  //      toMaximized.put(parent, locale);
  //      if (SHOW_ADD) System.out.println("Adding:\t" + parent + "\t=>\t" + locale + "\t\tDefaultContent");
  //    }
  //
  //    for (String[] specialCase : SpecialCases) {
  //      toMaximized.put(specialCase[0], specialCase[1]);
  //      if (SHOW_ADD) System.out.println("Adding:\t" + specialCase[0] + "\t=>\t" + specialCase[1] + "\t\tSpecial");
  //    }
  //
  //    // recurse and close
  //    closeMapping(toMaximized);
  //
  //    addScript(toMaximized, parser);
  //
  //    closeMapping(toMaximized);
  //
  //    addLanguageScript(toMaximized, parser);
  //
  //    closeMapping(toMaximized);
  //
  //    addLanguageCountry(toMaximized, parser);
  //
  //    closeMapping(toMaximized);
  //
  //    addCountries(toMaximized);
  //    addScript(toMaximized, parser);   
  //    closeMapping(toMaximized);
  //    closeUnd(toMaximized);
  //    
  //    addDeprecated(toMaximized);
  //
  //    closeMapping(toMaximized);
  //
  //    checkConsistency(toMaximized);
  //  }

  private static class MaxData {
    Relation<String,Row.R3<Double,String,String>> languages = new Relation(new TreeMap(), TreeSet.class);
    Map<String,Counter<String>> languagesToScripts = new TreeMap();
    Map<String,Counter<String>> languagesToRegions = new TreeMap();

    Relation<String,Row.R3<Double,String,String>> scripts = new Relation(new TreeMap(), TreeSet.class);
    Map<String,Counter<String>> scriptsToLanguages = new TreeMap();
    Map<String,Counter<String>> scriptsToRegions = new TreeMap();

    Relation<String,Row.R3<Double,String,String>> regions = new Relation(new TreeMap(), TreeSet.class);
    Map<String,Counter<String>> regionsToLanguages = new TreeMap();
    Map<String,Counter<String>> regionsToScripts = new TreeMap();

    Relation<Row.R2<String, String>,Row.R2<Double,String>> languageScripts = new Relation(new TreeMap(), TreeSet.class);
    Relation<Row.R2<String, String>,Row.R2<Double,String>> scriptRegions = new Relation(new TreeMap(), TreeSet.class);
    Relation<Row.R2<String, String>,Row.R2<Double,String>> languageRegions = new Relation(new TreeMap(), TreeSet.class);

    void add(String language, String script, String region, Double order) {
      languages.put(language, Row.of(order, script, region));
      //addCounter(languagesToScripts, language, script, order);
      //addCounter(languagesToRegions, language, region, order);

      scripts.put(script, Row.of(order, language, region));
      //addCounter(scriptsToLanguages, script, language, order);
      //addCounter(scriptsToRegions, script, region, order);

      regions.put(region, Row.of(order, language, script));
      //addCounter(regionsToLanguages, region, language, order);
      //addCounter(regionsToScripts, region, script, order);

      languageScripts.put(Row.of(language,script), Row.of(order, region));
      scriptRegions.put(Row.of(script,region), Row.of(order, language));
      languageRegions.put(Row.of(language,region), Row.of(order, script));

      if (SHOW_ADD) System.out.println("Data:\t" + language + "\t" + script + "\t" + region + "\t" + order);
    }
    //    private void addCounter(Map<String, Counter<String>> map, String key, String key2, Double count) {
    //      Counter<String> counter = map.get(key);
    //      if (counter == null) {
    //        map.put(key, counter = new Counter<String>());
    //      }
    //      counter.add(key2, count.longValue());
    //    }
  }

  private static final double MIN_UNOFFICIAL_LANGUAGE_SIZE = 10000000;
  private static final double MIN_UNOFFICIAL_LANGUAGE_PROPORTION = 0.20;
  private static final double MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE = 100000;
  private static final double UNOFFICIAL_SCALE_DOWN = 1.0;

  private static final Map<String,String> LANGUAGE_OVERRIDES = CldrUtility.asMap(new String[][]{
          {"es", "es_Latn_ES"},
          {"es_Latn", "es_Latn_ES"},
          {"az", "az_Latn_AZ"},
          {"az_Cyrl", "az_Cyrl_AZ"},
          {"mn", "mn_Cyrl_MN"},
          {"mn_Cyrl", "mn_Cyrl_MN"},
          {"sw", "sw_Latn_TZ"},
          {"sw_Latn", "sw_Latn_TZ"},
          {"und", "en_Latn_US"},
          {"und_Hani", "zh_Hans_CN"},
          {"und_Hani_CN", "zh_Hans_CN"},
          {"zh_Hani", "zh_Hans_CN"},
          {"trv", "trv_Latn_TW"}, // should fix by having tr_TW in repository
          //{"und_Latn_MK", "sq_Latn_MK"}, // because Albanian not official language
          {"pa_Arab", "pa_Arab_PK"},
          {"pa_PK", "pa_Arab_PK"},
          {"ps", "ps_Arab_AF"},
          {"ps_Arab", "ps_Arab_AF"},
  });

  //  private static final Map<String,String> LANGUAGE_SEGMENT_OVERRIDES = Utility.asMap(new String[][]{
  //          {"es_Latn_ES", "1e10"},
  //          {"en_Latn_US", "1e10"},
  //          {"trv_Latn_TW", "1e10"},
  //          //"pa_Arab_PK"
  //  });


  private static NumberFormat percent = NumberFormat.getPercentInstance();
  private static NumberFormat number = NumberFormat.getIntegerInstance();

  private static void tryDifferentAlgorithm(Map<String, String> toMaximized) {
    // we are going to try a different approach.
    // first gather counts for maximized values
    //Set<Row.R3<String,String,String>,Double> rowsToCounts = new TreeMap();
    MaxData maxData = new MaxData();
    Set<String> cldrLocales = factory.getAvailable();
    Set<String> otherTerritories = new TreeSet(standardCodes.getGoodAvailableCodes("territory"));

    // process all the information to get the top values for each triple.
    // each of the combinations of 1 or 2 components gets to be a key.
    for (String region : supplementalData.getTerritoriesWithPopulationData()) {
      otherTerritories.remove(region);
      PopulationData regionData = supplementalData.getPopulationDataForTerritory(region);
      final double literateTerritoryPopulation = regionData.getLiteratePopulation();
      // we need any unofficial language to meet a certain absolute size requirement and proportion size requirement.
      // so the bar is x percent of the population, reset up to y absolute size.
      double minimalLiteratePopulation = literateTerritoryPopulation*MIN_UNOFFICIAL_LANGUAGE_PROPORTION;
      if (minimalLiteratePopulation < MIN_UNOFFICIAL_LANGUAGE_SIZE) {
        minimalLiteratePopulation = MIN_UNOFFICIAL_LANGUAGE_SIZE;
      }

      for (String writtenLanguage : supplementalData.getLanguagesForTerritoryWithPopulationData(region)) {
        PopulationData data = supplementalData.getLanguageAndTerritoryPopulationData(writtenLanguage, region);
        final double literatePopulation = data.getLiteratePopulation();
        double order = -literatePopulation; // negative so we get the inverse order

        if (data.getOfficialStatus() == OfficialStatus.unknown) {
          final String locale = writtenLanguage + "_" + region;
          if (literatePopulation >= minimalLiteratePopulation) {
            // ok, skip
          } else if (literatePopulation >= MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE && cldrLocales.contains(locale)) {
            // ok, skip
          } else {
            if (SHOW_ADD) System.out.println("Skipping:\t" + writtenLanguage + "\t" + region + "\t" + english.getName(locale)
                    + "\t-- too small:\t" + number.format(literatePopulation));
            continue;
          }
          order *= UNOFFICIAL_SCALE_DOWN;
          if (SHOW_ADD) System.out.println("Retaining\t" + writtenLanguage + "\t" + region + "\t" + english.getName(locale) 
                  + "\t" + number.format(literatePopulation)
                  + "\t" + percent.format(literatePopulation/literateTerritoryPopulation)
                  + (cldrLocales.contains(locale) ? "\tin-CLDR" : "")
          );
        }
        String script;
        String language = writtenLanguage;
        final int pos = writtenLanguage.indexOf('_');
        if (pos > 0) {
          language = writtenLanguage.substring(0,pos);
          script = writtenLanguage.substring(pos+1);
        } else {
          script = getScriptForLocale2(language);
        }
        maxData.add(language, script, region, order);
      }
    }

    // add others, with English default
    for (String region : otherTerritories) {
      if (region.length() == 3) continue; // FIX ONCE WE ADD REGIONS
      maxData.add("en", "Latn", region, 1.0);
    }

    // add override segments
    //    double higherThanAny = -1e12;
    //    for (String locale : LANGUAGE_SEGMENT_OVERRIDES) {
    //      String[] parts = locale.split("_");
    //      maxData.add(parts[0], parts[1], parts[2], higherThanAny);
    //      higherThanAny += 1; // lower slightly for next one
    //    }

    // now, get the best for each one
    for (String language : maxData.languages.keySet()) {
      R3<Double, String, String> value = maxData.languages.getAll(language).iterator().next();
      final Comparable script = value.get1();
      final Comparable region = value.get2();
      add(language, language + "_" + script + "_" + region, toMaximized, "L->SR");
    }
    for (String language : maxData.languagesToScripts.keySet()) {
      String script = maxData.languagesToScripts.get(language).getKeysetSortedByCount(true).iterator().next();
      add(language, language + "_" + script, toMaximized, "L->S");
    }
    for (String language : maxData.languagesToRegions.keySet()) {
      String region = maxData.languagesToRegions.get(language).getKeysetSortedByCount(true).iterator().next();
      add(language, language + "_" + region, toMaximized, "L->R");
    }

    for (String script : maxData.scripts.keySet()) {
       R3<Double, String, String> value = maxData.scripts.getAll(script).iterator().next();
      final Comparable language = value.get1();
      final Comparable region = value.get2();
      add("und_" + script, language + "_" + script + "_" + region, toMaximized, "S->LR");
    }
    for (String script : maxData.scriptsToLanguages.keySet()) {
      String language = maxData.scriptsToLanguages.get(script).getKeysetSortedByCount(true).iterator().next();
      add("und_" + script, language + "_" + script, toMaximized, "S->L");
    }
    for (String script : maxData.scriptsToRegions.keySet()) {
      String region = maxData.scriptsToRegions.get(script).getKeysetSortedByCount(true).iterator().next();
      add("und_" + script, "und_" + script + "_" + region, toMaximized, "S->R");
    }

    for (String region : maxData.regions.keySet()) {
       R3<Double, String, String> value = maxData.regions.getAll(region).iterator().next();
      final Comparable language = value.get1();
      final Comparable script = value.get2();
      add("und_" + region, language + "_" + script + "_" + region, toMaximized, "R->LS");
    }
    for (String region : maxData.regionsToLanguages.keySet()) {
      String language = maxData.regionsToLanguages.get(region).getKeysetSortedByCount(true).iterator().next();
      add("und_" + region, language + "_" + region, toMaximized, "R->L");
    }
    for (String region : maxData.regionsToScripts.keySet()) {
      String script = maxData.regionsToScripts.get(region).getKeysetSortedByCount(true).iterator().next();
      add("und_" + region, "und_" + script + "_" + region, toMaximized, "R->S");
    }

    for (R2<String, String> languageScript : maxData.languageScripts.keySet()) {
      R2<Double, String> value = maxData.languageScripts.getAll(languageScript).iterator().next();
      final Comparable language = languageScript.get0();
      final Comparable script = languageScript.get1();
      final Comparable region = value.get1();
      add(language + "_" + script, language + "_" + script + "_" + region, toMaximized, "LS->R");
    }

    for (R2<String, String> scriptRegion : maxData.scriptRegions.keySet()) {
      R2<Double, String> value = maxData.scriptRegions.getAll(scriptRegion).iterator().next();
      final Comparable script = scriptRegion.get0();
      final Comparable region = scriptRegion.get1();
      final Comparable language = value.get1();
      add("und_" + script + "_" + region, language + "_" + script + "_" + region, toMaximized, "SR->L");
    }

    for (R2<String, String> languageRegion : maxData.languageRegions.keySet()) {
      R2<Double, String> value = maxData.languageRegions.getAll(languageRegion).iterator().next();
      final Comparable language = languageRegion.get0();
      final Comparable region = languageRegion.get1();
      final Comparable script = value.get1();
      add(language + "_" + region, language + "_" + script + "_" + region, toMaximized, "LR->S");
    }

    // add overrides
    for (String key : LANGUAGE_OVERRIDES.keySet()) {
      add(key, LANGUAGE_OVERRIDES.get(key), toMaximized, "OVERRIDE");
    }
  }


  private static void doAlt(Map<String, String> toMaximized) {
    // TODO Auto-generated method stub
    Map<String, String> temp = new TreeMap();
    for (String locale : toMaximized.keySet()) {
      String target = toMaximized.get(locale);
      temp.put(toAlt(locale, true), toAlt(target, true));
    }
    toMaximized.clear();
    toMaximized.putAll(temp);
  }

  private static String maximize(String languageTag, Map<String, String> toMaximized) {
    LanguageTagParser ltp = new LanguageTagParser();

    // clean up the input by removing Zzzz, ZZ, and changing "" into und.
    ltp.set(languageTag);
    String language = ltp.getLanguage();
    String region = ltp.getRegion();
    String script = ltp.getScript();
    boolean changed = false;
    if (language.equals("")) {
      ltp.setLanguage(language = "und");
      changed = true;
    }
    if (region.equals(UNKNOWN_SCRIPT)) {
      ltp.setScript(script = "");
      changed = true;
    }
    if (ltp.getRegion().equals(UNKNOWN_REGION)) {
      ltp.setRegion(region = "");
      changed = true;
    }
    if (changed) {
      languageTag = ltp.toString();
    }
    // check whole
    String result = toMaximized.get(languageTag);
    if (result != null) {
      return result;
    }
    // try empty region
    if (region.length() != 0) {
      result = toMaximized.get(ltp.setRegion("").toString());
      if (result != null) {
        return ltp.set(result).setRegion(region).toString();
      }
      ltp.setRegion(region); // restore
    }
    // try empty script
    if (script.length() != 0) {
      result = toMaximized.get(ltp.setScript("").toString());
      if (result != null) {
        return ltp.set(result).setScript(script).toString();
      }
      // try empty script and region
      if (region.length() != 0) {
        result = toMaximized.get(ltp.setRegion("").toString());
        if (result != null) {
          return ltp.set(result).setScript(script).setRegion(region).toString();
        }
      }
    }
    if (!language.equals("und") && script.length() != 0 && region.length() != 0) {
      return languageTag; // it was ok, and we couldn't do anything with it
    }
    return null; // couldn't maximize
  }

  public static String minimize(String input, Map<String, String> toMaximized, boolean favorRegion) {
    if (input.equals("nb_Latn_SJ")) {
      System.out.print(""); // debug
    }
    String maximized = maximize(input, toMaximized);
    LanguageTagParser ltp = new LanguageTagParser().set(maximized);
    String language = ltp.getLanguage();
    String region = ltp.getRegion();
    String script = ltp.getScript();
    // try building up from shorter to longer, and find the first  that matches
    // could be more optimized, but for this code we want simplest
    String[] trials = {language, 
            language + TAG_SEPARATOR + (favorRegion ? region : script), 
            language + TAG_SEPARATOR + (!favorRegion ? region : script)};
    for (String trial : trials) {
      String newMaximized = maximize(trial, toMaximized);
      if (maximized.equals(newMaximized)) {
        return trial;
      }
    }
    return maximized;
  }

  //  /**
  //   * Verify that we can map from each language, script, and country to something.
  //   * @param toMaximized
  //   */
  //  private static void checkConsistency(Map<String, String> toMaximized) {
  //    Map<String,String> needMappings = new TreeMap();
  //    LanguageTagParser parser = new LanguageTagParser();
  //    for (String maximized : new TreeSet<String>(toMaximized.values())) {
  //      parser.set(maximized);
  //      final String language = parser.getLanguage();
  //      final String script = parser.getScript();
  //      final String region = parser.getRegion();
  //      if (language.length() == 0 || script.length() == 0 || region.length() == 0) {
  //        failure("   { \"" + maximized + "\", \"" + maximized + "\" },   //     " + english.getName(maximized) + "\t\tFailed-Consistency");
  //        continue;
  //      }
  //      addIfNotIn(language, maximized, needMappings, toMaximized, "Consistency");
  //      addIfNotIn(language + "_" + script, maximized, needMappings, toMaximized, "Consistency");
  //      addIfNotIn(language + "_" + region, maximized, needMappings, toMaximized, "Consistency");
  //      addIfNotIn("und_" + script, maximized, needMappings, toMaximized, "Consistency");
  //      addIfNotIn("und_" + script + "_" + region, maximized, needMappings, toMaximized, "Consistency");
  //      addIfNotIn("und_" + region, maximized, needMappings, toMaximized, "Consistency");
  //    }
  //    toMaximized.putAll(needMappings);
  //  }

  //  private static void failure(String string) {
  //    System.out.println(string);
  //    errorCount++;
  //  }

  //  private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Map<String, String> otherToCheck, String kind) {
  //    addIfNotIn(key, value, toAdd, otherToCheck == null ? null : otherToCheck.keySet(), null, kind);
  //  }

  //  private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Set<String> skipKey, Set<String> skipValue, String kind) {
  //    if (!key.equals(value)
  //            && !toAdd.containsKey(key) 
  //            && (skipKey == null || !skipKey.contains(key)) 
  //            && (skipValue == null || !skipValue.contains(value))) {
  //      add(key, value, toAdd, kind);
  //    }
  //  }

  private static void add(String key, String value, Map<String, String> toAdd, String kind) {
    if (key.equals("und_Latn_ZA")) {
      System.out.println("*debug*");
    }
    String oldValue = toAdd.get(key);
    if (oldValue != null && value.equals(oldValue)) {
      if (SHOW_ADD) System.out.println("Replacing:\t" + key + "\t=>\t" + oldValue);
    }
    toAdd.put(key, value);
    if (SHOW_ADD) System.out.println("Adding:\t" + key + "\t=>\t" + value + "\t\t" + kind);
  }

  //  private static void addCountries(Map<String, String> toMaximized) {
  //    Map <String, Map<String, Double>> scriptToLanguageToSize = new TreeMap();
  //
  //    for (String territory : supplementalData.getTerritoriesWithPopulationData()) {
  //      Set<String> languages = supplementalData.getLanguagesForTerritoryWithPopulationData(territory);
  //      String biggestOfficial = null;
  //      double biggest = -1;
  //      for (String language : languages) {
  //        PopulationData info = supplementalData.getLanguageAndTerritoryPopulationData(language, territory);
  //        // add to info about script
  //
  //        String script = getScriptForLocale(language);
  //        if (script != null) {
  //          Map<String, Double> languageInfo = scriptToLanguageToSize.get(script);
  //          if (languageInfo == null) scriptToLanguageToSize.put(script, languageInfo = new TreeMap());
  //          String baseLanguage = language;
  //          int pos = baseLanguage.indexOf('_');
  //          if (pos >= 0) {
  //            baseLanguage = baseLanguage.substring(0,pos);
  //          }
  //          Double size = languageInfo.get(baseLanguage);
  //          languageInfo.put(baseLanguage, (size == null ? 0 : size) + info.getLiteratePopulation());
  //        }
  //
  //
  //        final OfficialStatus officialStatus = info.getOfficialStatus();
  //        if (officialStatus == OfficialStatus.de_facto_official || officialStatus == OfficialStatus.official) {
  //          double size2 = info.getLiteratePopulation();
  //          if (biggest < size2) {
  //            biggest = size2;
  //            biggestOfficial = language;
  //          }
  //        }
  //      }
  //      if (biggestOfficial != null) {
  //        final String replacementTag = "und_" + territory;
  //        String maximized = biggestOfficial + "_" + territory;
  //        toMaximized.put(replacementTag, maximized);
  //        if (SHOW_ADD) System.out.println("Adding:\t" + replacementTag + "\t=>\t" + maximized + "\t\tLanguage-Territory");
  //      }
  //    }
  //
  //    for  (String script : scriptToLanguageToSize.keySet()) {
  //      String biggestOfficial = null;
  //      double biggest = -1;
  //
  //      final Map<String, Double> languageToSize = scriptToLanguageToSize.get(script);
  //      for (String language : languageToSize.keySet()) {
  //        double size = languageToSize.get(language);
  //        if (biggest < size) {
  //          biggest = size;
  //          biggestOfficial = language;
  //        }
  //      }
  //      if (biggestOfficial != null) {
  //        final String replacementTag = "und_" + script;
  //        String maximized = biggestOfficial + "_" + script;
  //        toMaximized.put(replacementTag, maximized);
  //        if (SHOW_ADD) System.out.println("Adding:\t" + replacementTag + "\t=>\t" + maximized + "\t\tUnd-Script");
  //      }
  //    }
  //  }

  //  private static void closeUnd(Map<String, String> toMaximized) {
  //    Map<String,String> toAdd = new TreeMap<String,String>();
  //    for (String oldSource : toMaximized.keySet()) {
  //      String maximized = toMaximized.get(oldSource);
  //      if (!maximized.startsWith("und")) {
  //        int pos = maximized.indexOf("_");
  //        if (pos >= 0) {
  //          addIfNotIn( "und" + maximized.substring(pos), maximized, toAdd, toMaximized, "CloseUnd");
  //        }
  //      }
  //    }
  //    toMaximized.putAll(toAdd);
  //  }

  /**
   * Generate tags where the deprecated values map to the expanded values
   * @param toMaximized
   */
  //  private static void addDeprecated(Map<String, String> toMaximized) {
  //    Map<String, Map<String, List<String>>> typeToTagToReplacement = supplementalData.getLocaleAliasInfo();
  //    LanguageTagParser temp = new LanguageTagParser();
  //    LanguageTagParser tagParsed = new LanguageTagParser();
  //    LanguageTagParser replacementParsed = new LanguageTagParser();
  //    Map<String,String> toAdd = new TreeMap<String,String>();
  //    while (true) {
  //      toAdd.clear();
  //      for (String type : typeToTagToReplacement.keySet()) {
  //        if (type.equals("variant") || type.equals("zone")) continue;
  //        boolean addUnd = !type.equals("language");
  //
  //        Map<String, List<String>> tagToReplacement = typeToTagToReplacement.get(type);
  //        System.out.println("*" + type + " = " + tagToReplacement);
  //
  //        for (String tag: tagToReplacement.keySet()) {
  //
  //          final List<String> list = tagToReplacement.get(tag);
  //          if (list == null) continue; // we don't have any information
  //          String replacement = list.get(0);
  //
  //          // only do multiples
  //          if (tag.contains("_") || !replacement.contains("_")) {
  //            continue;
  //          }
  //
  //          // we now have a tag and a replacement value
  //          // make parsers that we can use
  //          try {
  //            tagParsed.set(addUnd ? "und-" + tag : tag);
  //            replacementParsed.set(addUnd ? "und-" + replacement : replacement);
  //          } catch (RuntimeException e) {
  //            continue;
  //          }
  //          addIfNotIn(tag, replacement, toAdd, toMaximized,"Deprecated");
  //
  //          for (String locale : toMaximized.keySet()) {
  //            String maximized = toMaximized.get(locale);
  //            addIfMatches(temp.set(locale), maximized, replacementParsed,  tagParsed, toAdd, toMaximized);
  //            addIfMatches(temp.set(maximized), maximized, replacementParsed,  tagParsed, toAdd, toMaximized);
  //          }
  //        }
  //      }
  //      if (toAdd.size() == 0) {
  //        break;
  //      }
  //      toMaximized.putAll(toAdd);
  //    }
  //  }

  //  private static void addIfMatches(LanguageTagParser locale, String maximized, LanguageTagParser tagParsed, LanguageTagParser replacementParsed, Map<String, String> toAdd, Map<String, String> toMaximized) {
  //    if (!tagParsed.getLanguage().equals(locale.getLanguage()) && !tagParsed.getLanguage().equals("und")) {
  //      return;
  //    }
  //    if (!tagParsed.getScript().equals(locale.getScript()) && !tagParsed.getScript().equals("")) {
  //      return;
  //    }
  //    if (!tagParsed.getRegion().equals(locale.getRegion()) && !tagParsed.getRegion().equals("")) {
  //      return;
  //    }
  //    if (!replacementParsed.getLanguage().equals("und")) {
  //      locale.setLanguage(replacementParsed.getLanguage());
  //    }
  //    if (!replacementParsed.getScript().equals("")) {
  //      locale.setScript(replacementParsed.getScript());
  //    }
  //    if (!replacementParsed.getRegion().equals("")) {
  //      locale.setRegion(replacementParsed.getRegion());
  //    }
  //    addIfNotIn(locale.toString(), maximized, toAdd, toMaximized,"Deprecated");
  //  }

  //  private static int getSubtagPosition(String locale, String subtags) {
  //    int pos = -1;
  //    while (true) {
  //      pos = locale.indexOf(subtags, pos + 1);
  //      if (pos < 0) return -1;
  //      // make sure boundaries are ok
  //      if (pos != 0) {
  //        char charBefore = locale.charAt(pos-1);
  //        if (charBefore != '_' && charBefore != '_') return -1;
  //      }
  //      int limit = pos + subtags.length();
  //      if (limit != locale.length()) {
  //        char charAfter = locale.charAt(limit);
  //        if (charAfter != '_' && charAfter != '_') return -1;
  //      }
  //      return pos;
  //    }
  //  }

  /* Format
  const DefaultSubtags default_subtags[] = {
    {
      // Afar => Afar (Latin, Ethiopia)
      "aa",
      "aa_Latn_ET"
    },{
      // Afrikaans => Afrikaans (Latin, South Africa)
      "af",
      "af_Latn_ZA"
    },{
   */

  private static void printMap(Map<String, String> fluffup) throws IOException {
    PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY, 
            "/supplemental/likelySubtags" + (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
    String spacing = OUTPUT_STYLE == OutputStyle.PLAINTEXT ? "\t" : " ";
    String header = OUTPUT_STYLE != OutputStyle.XML ? "const MapToMaximalSubtags default_subtags[] = {"
            : "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + CldrUtility.LINE_SEPARATOR
            + "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">" + CldrUtility.LINE_SEPARATOR
            + "<supplementalData>" + CldrUtility.LINE_SEPARATOR
            + "    <version number=\"$" +
            "Revision$\"/>" + CldrUtility.LINE_SEPARATOR
            + "    <generation date=\"$" +  // separate
            "Date$\"/>" + CldrUtility.LINE_SEPARATOR
            + "    <likelySubtags>";
    String footer = OUTPUT_STYLE  != OutputStyle.XML ? SEPARATOR + "};" 
            : "    </likelySubtags>" + CldrUtility.LINE_SEPARATOR
            + "</supplementalData>";
    out.println(header);
    boolean first = true;
    for (String printingLocale : fluffup.keySet()) {
      String printingTarget = fluffup.get(printingLocale);
      String comment = printingName(printingLocale, spacing) + spacing + "=>" + spacing + printingName(printingTarget, spacing);

      if (OUTPUT_STYLE == OutputStyle.XML) {
        out.println("      <likelySubtag from=\"" +  printingLocale +
                "\" to=\"" + printingTarget + "\"" + 
                "/>" + spacing + "<!--" +comment + "-->");
      } else {
        if (first) {
          first = false;
        } else {
          out.print(",");
        }
        if (comment.length() > 70 && SEPARATOR.equals(CldrUtility.LINE_SEPARATOR)) {
          comment = printingName(printingLocale, spacing) +SEPARATOR + "    // " + spacing + "=>" + spacing + printingName(printingTarget, spacing);
        }
        out.print(
                "  {"
                + SEPARATOR + "    // " + comment
                + SEPARATOR + "    \"" + printingLocale + "\","
                + SEPARATOR + "    \"" + printingTarget + "\""
                + CldrUtility.LINE_SEPARATOR + "  }"
        );
      }
    }
    out.println(footer);
    out.close();
  }

  public static String printingName(String locale, String spacing) {
    if (locale == null) {
      return null;
    }
    LanguageTagParser parser = new LanguageTagParser().set(locale);
    String lang = parser.getLanguage();
    String script = parser.getScript();
    String region = parser.getRegion();
    return "{" + spacing +
    (lang.equals("und") ? "?" : english.getName(CLDRFile.LANGUAGE_NAME, lang)) + ";" + spacing +
    (script == null || script.equals("") ? "?" : english.getName(CLDRFile.SCRIPT_NAME, script)) + ";" + spacing +
    (region == null || region.equals("") ? "?" : english.getName(CLDRFile.TERRITORY_NAME, region)) + spacing + "}";
  }

  private static final String[][] ALT_REVERSAL = {
    {"nb", "no"},
    {"no", "nb"},
    {"he", "iw"},
    {"iw", "he"},
  };

  public static String toAlt(String locale, boolean change) {
    if (!change || locale == null) {
      return locale;
    }
    String firstTag = getFirstTag(locale);
    for (String[] pair : ALT_REVERSAL) {
      if (firstTag.equals(pair[0])) {
        locale = pair[1] + locale.substring(pair[1].length());
        break;
      }
    }
    locale = locale.replace("_", "-");
    return locale;
  }

  private static String getFirstTag(String locale) {
    int pos = locale.indexOf('_');
    return pos < 0 ? locale : locale.substring(0,pos);
  }

  //  private static Map<String, String> getBackMapping(Map<String, String> fluffup) {
  //    Relation<String,String> backMap = new Relation(new TreeMap(), TreeSet.class, BEST_LANGUAGE_COMPARATOR);
  //    for (String source : fluffup.keySet()) {
  //      if (source.startsWith("und")) {
  //        continue;
  //      }
  //      String maximized = fluffup.get(source);
  //      backMap.put(maximized, source); // put in right order
  //    }
  //    Map<String,String> returnBackMap = new TreeMap();
  //    for (String maximized : backMap.keySet()) {
  //      final Set<String> all = backMap.getAll(maximized);
  //      final String minimized = all.iterator().next();
  //      returnBackMap.put(maximized, minimized);
  //    }
  //    return returnBackMap;
  //  }

  /**
   * Language tags are presumed to share the first language, except possibly "und". Best is least
   */
  //  private static Comparator BEST_LANGUAGE_COMPARATOR = new Comparator<String>() {
  //    LanguageTagParser p1 = new LanguageTagParser();
  //    LanguageTagParser p2 = new LanguageTagParser();
  //    public int compare(String o1, String o2) {
  //      if (o1.equals(o2)) return 0;
  //      p1.set(o1);
  //      p2.set(o2);
  //      String lang1 = p1.getLanguage();
  //      String lang2 = p2.getLanguage();
  //
  //      // compare languages first
  //      // put und at the end
  //      int result = lang1.compareTo(lang2);
  //      if (result != 0) {
  //        if (lang1.equals("und")) return 1;
  //        if (lang2.equals("und")) return -1;
  //        return result;
  //      }
  //
  //      // now scripts and regions.
  //      // if they have different numbers of fields, the shorter wins.
  //      // If there are two fields, region is lowest.
  //      // The simplest way is to just compare scripts first
  //      // so zh-TW < zh-Hant, because we first compare "" to Hant
  //      String script1 = p1.getScript();
  //      String script2 = p2.getScript();
  //      int scriptOrder = script1.compareTo(script2);
  //      if (scriptOrder != 0) return scriptOrder;
  //
  //      String region1 = p1.getRegion();
  //      String region2 = p2.getRegion();
  //      int regionOrder = region1.compareTo(region2);
  //      if (regionOrder != 0) return regionOrder;
  //
  //      return o1.compareTo(o2);
  //    }
  //
  //  };

  private static void minimize(Map<String, String> fluffup) {
    LanguageTagParser parser = new LanguageTagParser();
    LanguageTagParser targetParser = new LanguageTagParser();
    Set<String> removals = new TreeSet<String>();
    while (true) {
      removals.clear();
      for (String locale : fluffup.keySet()) {
        String target = fluffup.get(locale);
        if (targetParser.set(target).getRegion().equals(UNKNOWN_REGION)) {
          removals.add(locale);
          if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target +"\t\t - Unknown Region in target");
          continue;            
        }
        if (targetParser.getScript().equals(UNKNOWN_SCRIPT)) {
          removals.add(locale);
          if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target +"\t\t - Unknown Script in target");
          continue;            
        }

        String region = parser.set(locale).getRegion();
        if (region.length() != 0) {
          if (region.equals(UNKNOWN_REGION)) {
            removals.add(locale);
            if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target +"\t\t - Unknown Region in source");
            continue;            
          }
          parser.setRegion("");
          String newLocale = parser.toString();
          String newTarget = fluffup.get(newLocale);
          if (newTarget != null) {
            newTarget = targetParser.set(newTarget).setRegion(region).toString();
            if (target.equals(newTarget)) {
              removals.add(locale);
              if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target +"\t\tRedundant with " + newLocale);
              continue;
            }
          }
        }
        String script = parser.set(locale).getScript();
        if (locale.equals("und_Latn_ZA")) {
          System.out.println("*debug*");
        }
        if (script.length() != 0) {
          if (script.equals(UNKNOWN_SCRIPT)) {
            removals.add(locale);
            if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target +"\t\t - Unknown Script");
            continue;            
          }
          parser.setScript("");
          String newLocale = parser.toString();
          String newTarget = fluffup.get(newLocale);
          if (newTarget != null) {
            newTarget = targetParser.set(newTarget).setScript(script).toString();
            if (target.equals(newTarget)) {
              removals.add(locale);
              if (SHOW_ADD) System.out.println("Removing:\t" + locale + "\t=>\t" + target + "\t\tRedundant with " + newLocale);
              continue;
            }
          }
        }
      }
      if (removals.size() == 0) {
        break;
      }
      for (String locale : removals) {
        fluffup.remove(locale);
      }
    }
  }

  //  private static void addLanguageScript(Map<String, String> fluffup, LanguageTagParser parser) {
  //    // add script
  //    Map<String, String> temp = new TreeMap<String, String>();
  //    while (true) {
  //      temp.clear();
  //      for (String target : new TreeSet<String>(fluffup.values())) {
  //        parser.set(target);
  //        final String territory = parser.getRegion();
  //        if (territory.length() == 0) {
  //          continue;
  //        }
  //        parser.setRegion("");
  //        String possibleSource = parser.toString();
  //        if (fluffup.containsKey(possibleSource)) {
  //          continue;
  //        }
  //        String other = temp.get(possibleSource);
  //        if (other != null) {
  //          if (!target.equals(other)) {
  //            System.out.println("**Failure with multiple sources in addLanguageScript: "
  //                    + possibleSource + "\t=>\t" + target + ", " + other);
  //          }
  //          continue;
  //        }
  //        temp.put(possibleSource, target);
  //        if (SHOW_ADD) System.out.println("Adding:\t" + possibleSource + "\t=>\t" + target + "\t\tLanguage-Script");
  //      }
  //      if (temp.size() == 0) {
  //        break;
  //      }
  //      fluffup.putAll(temp);
  //    }
  //
  //  }

  //  private static void addLanguageCountry(Map<String, String> fluffup, LanguageTagParser parser) {
  //    // add script
  //    Map<String, String> temp = new TreeMap<String, String>();
  //    while (true) {
  //      temp.clear();
  //      for (String target : new TreeSet<String>(fluffup.values())) {
  //        parser.set(target);
  //        String script = parser.getScript();
  //        if (script.length() == 0) {
  //          continue;
  //        }
  //        parser.setScript("");
  //        String possibleSource = parser.toString();
  //        if (fluffup.containsKey(possibleSource)) {
  //          continue;
  //        }
  //        String other = temp.get(possibleSource);
  //
  //        if (other != null) {
  //          if (!target.equals(other)) {
  //            script = getScriptForLocale(possibleSource);
  //            if (script == null) {
  //              System.out.println("**Failure with multiple sources in addLanguageCountry: "
  //                      + possibleSource + "\t=>\t" + target + ", " + other);
  //              continue; // error message in routine
  //            }
  //            parser.setScript(script);
  //            target = parser.toString();
  //          }
  //        }
  //
  //        temp.put(possibleSource, target);
  //        if (SHOW_ADD) System.out.println("Adding:\t" + possibleSource + "\t=>\t" + target + "\t\tLanguageCountry");
  //      }
  //      if (temp.size() == 0) {
  //        break;
  //      }
  //      fluffup.putAll(temp);
  //    }
  //
  //  }

  //  private static void  addScript(Map<String, String> fluffup, LanguageTagParser parser) {
  //    // add script
  //    Map<String, String> temp = new TreeMap<String, String>();
  //    while (true) {
  //      temp.clear();
  //      Set skipTarget = fluffup.keySet();
  //      for (String locale : fluffup.keySet()) {
  //        String target = fluffup.get(locale);
  //        parser.set(target);
  //        if (parser.getScript().length() != 0) {
  //          continue;
  //        }
  //        String script = getScriptForLocale(target);
  //
  //        if (script == null) {
  //          continue; // error message in routine
  //        }
  //        parser.setScript(script);
  //        String furtherTarget = parser.toString();
  //        addIfNotIn(target, furtherTarget, temp, fluffup, "Script");
  //      }
  //      if (temp.size() == 0) {
  //        break;
  //      }
  //      fluffup.putAll(temp);
  //    }
  //  }

  //  private static String getScriptForLocale(String locale) {
  //    String result = getScriptForLocale2(locale);
  //    if (result != null) return result;
  //    int pos = locale.indexOf('_');
  //    if (pos >= 0) {
  //      result = getScriptForLocale2(locale.substring(0,pos));
  //    }
  //    return result;
  //  }

  private static String UNKNOWN_SCRIPT = "Zzzz";
  private static String UNKNOWN_REGION = "ZZ";

  private static String getScriptForLocale2(String locale) {
    String result = localeToScriptCache.get(locale);
    if (result != null) {
      return result;
    }
    try {
      Set<BasicLanguageData> data = supplementalData.getBasicLanguageData(locale);
      if (data != null) {
        for (BasicLanguageData datum : data) {
          final Set<String> scripts = datum.getScripts();
          if (scripts.size() == 0) {
            continue;
          }
          String script = scripts.iterator().next();
          if (datum.getType() == BasicLanguageData.Type.primary) {
            return result = script;
          } else if (result == null) {
            result = script;
          }
        }
        if (result != null) {
          return result;
        }
      }
      CLDRFile cldrFile;
      try {
        cldrFile = factory.make(locale, true);
      } catch (RuntimeException e) {
        return result = UNKNOWN_SCRIPT;
      }
      UnicodeSet exemplars = getExemplarSet(cldrFile, "");
      Set<String> CLDRScripts = getScriptsFromUnicodeSet(exemplars);
      CLDRScripts.remove(UNKNOWN_SCRIPT);
      if (CLDRScripts.size() == 1) {
        return result = CLDRScripts.iterator().next();
      } else if (CLDRScripts.size() == 0) {
        System.out.println("**Failed to get script for:\t" + locale);
        return result = UNKNOWN_SCRIPT;
      } else {
        System.out.println("**Failed, too many scripts for:\t" + locale + ", " + CLDRScripts);
        return result = UNKNOWN_SCRIPT;
      }
    } finally {
      localeToScriptCache.put(locale, result);
      if (SHOW_ADD) System.out.println("Script:\t" + locale + "\t" + english.getName(locale) + "\t=>\t" + result + "\t" + english.getName(CLDRFile.SCRIPT_NAME, result));
    }
  }

  //  private static Map<String, String> closeMapping(Map<String, String> fluffup) {
  //    if (SHOW_ADD) System.out.flush();
  //    Map<String,String> temp = new TreeMap<String,String>();
  //    while (true) {
  //      temp.clear();
  //      for (String locale : fluffup.keySet()) {
  //        String target = fluffup.get(locale);
  //        if (target.equals("si_Sinh") || target.equals("zh-Hani")) {
  //          System.out.println("????");
  //        }
  //        String furtherTarget = fluffup.get(target);
  //        if (furtherTarget == null) {
  //          continue;
  //        }
  //        addIfNotIn(locale, furtherTarget, temp, null, "Close");
  //      }
  //      if (temp.size() == 0) {
  //        break;
  //      }
  //      fluffup.putAll(temp);
  //    }
  //    if (SHOW_ADD) System.out.flush();
  //    return temp;
  //  }

  public static Set<String> getScriptsFromUnicodeSet(UnicodeSet exemplars) {
    // use bits first, since that's faster
    BitSet scriptBits = new BitSet();
    boolean show = false;
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
      if (show)
        System.out.println(Integer.toHexString(it.codepoint));
      if (it.codepoint != UnicodeSetIterator.IS_STRING) {
        scriptBits.set(UScript.getScript(it.codepoint));
      } else {
        int cp;
        for (int i = 0; i < it.string.length(); i += UTF16.getCharCount(cp)) {
          scriptBits.set(UScript.getScript(cp = UTF16.charAt(it.string, i)));
        }
      }
    }
    scriptBits.clear(UScript.COMMON);
    scriptBits.clear(UScript.INHERITED);
    Set<String> scripts = new TreeSet<String>();
    for (int j = 0; j < scriptBits.size(); ++j) {
      if (scriptBits.get(j)) {
        scripts.add(UScript.getShortName(j));
      }
    }
    return scripts;
  }

  public static UnicodeSet getExemplarSet(CLDRFile cldrfile, String type) {
    if (type.length() != 0)
      type = "[@type=\"" + type + "\"]";
    String v = cldrfile.getStringValue("//ldml/characters/exemplarCharacters"
            + type);
    if (v == null)
      return new UnicodeSet();
    return new UnicodeSet(v);
  }

  //  private static String[][] SpecialCases = {
  //    { "zh_Hani", "zh_Hans_CN"},
  //    { "si_Sinh", "si_Sinh_LK"},
  //    { "ii", "ii_CN"}, // Sichuan Yi (Yi)
  //    { "iu", "iu_CA"}, //    Inuktitut (Unified Canadian Aboriginal Syllabics)
  //    { "und", "en"}, //    English default
  //  };

  private static String[][] SpecialScripts = {
    { "chk", "Latn"},   //     Chuukese (Micronesia)
    { "fil", "Latn"},        //    Filipino (Philippines)"
    { "ko", "Kore"}, //    Korean (North Korea)
    { "ko_KR", "Kore"}, //    Korean (North Korea)
    { "pap", "Latn"}, //     Papiamento (Netherlands Antilles)
    { "pau", "Latn"}, //     Palauan (Palau)
    { "su", "Latn"}, //    Sundanese (Indonesia)
    { "tet", "Latn"}, //     Tetum (East Timor)
    { "tk", "Latn"}, //    Turkmen (Turkmenistan)
    { "ty", "Latn"}, //    Tahitian (French Polynesia)
    { "ja", "Jpan"}, //    Tahitian (French Polynesia)
    { "und", "Latn"}, //    Tahitian (French Polynesia)
  };
  private static Map<String,String> localeToScriptCache = new TreeMap();
  static {
    for (String[] pair : SpecialScripts) {
      localeToScriptCache.put(pair[0], pair[1]);
    }
    for (String language : standardCodes.getAvailableCodes("language")) {
      Map<String, String> info = standardCodes.getLangData("language", language);
      String script = info.get("Suppress-Script");
      if (script != null) {
        localeToScriptCache.put(language, script);
      }
    }
  }
}
