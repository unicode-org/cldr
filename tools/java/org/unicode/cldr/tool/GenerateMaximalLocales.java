package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Problems:
 *    "und_Hani",      "zh_Hani"
 *    "und_Sinh",     "si_Sinh"
 * @author markdavis
 *
 */
public class GenerateMaximalLocales {
  private static final boolean SHOW_ADD = false;
  enum OutputStyle {PLAINTEXT, C, C_ALT, XML};
  
  private static OutputStyle OUTPUT_STYLE = OutputStyle.XML;
  
  // set based on above
  private static final String SEPARATOR = OUTPUT_STYLE == OutputStyle.C  || OUTPUT_STYLE == OutputStyle.C_ALT ? "\r\n" : "\t";
  private static final String TAG_SEPARATOR = OUTPUT_STYLE == OutputStyle.C_ALT ? "-" : "_";
  private static final boolean FAVOR_REGION = true; // OUTPUT_STYLE == OutputStyle.C_ALT;

  static Factory factory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
  static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
  static StandardCodes standardCodes = StandardCodes.make();
  static CLDRFile english = factory.make("en", false);

  private static int errorCount;
  
  public static void main(String[] args) throws IOException {
    
    Set<String> defaultContentLocales = supplementalData.getDefaultContentLocales();
    Map<String,String> toMaximized = new TreeMap<String,String>();
    LanguageTagParser parser = new LanguageTagParser();
    
    for (String locale : defaultContentLocales) {
      String parent = parser.getParent(locale);
      toMaximized.put(parent, locale);
      if (SHOW_ADD) System.out.println("Adding: " + parent + " => " + locale + "\t\tDefaultContent");
    }
    
    for (String[] specialCase : SpecialCases) {
      toMaximized.put(specialCase[0], specialCase[1]);
      if (SHOW_ADD) System.out.println("Adding: " + specialCase[0] + " => " + specialCase[1] + "\t\tSpecial");
    }
    
    // recurse and close
    closeMapping(toMaximized);
    
   addScript(toMaximized, parser);
    
    closeMapping(toMaximized);
    
   addLanguageScript(toMaximized, parser);
    
    closeMapping(toMaximized);
    
   addLanguageCountry(toMaximized, parser);
    
    closeMapping(toMaximized);
    
    addCountries(toMaximized);
    addScript(toMaximized, parser);   
    closeMapping(toMaximized);
    closeUnd(toMaximized);
    
    addDeprecated(toMaximized);

    closeMapping(toMaximized);
    
    checkConsistency(toMaximized);

    minimize(toMaximized);
    
    if (OUTPUT_STYLE == OutputStyle.C_ALT) {
      doAlt(toMaximized);
    }
    
    System.out.println("/*\r\n To Maximize:" +
        "\r\n If using raw strings, make sure the input language/locale uses the right separator, and has the right casing." +
        "\r\n Remove the script Zzzz and the region ZZ if they occur; change an empty language subtag to 'und'." +
        "\r\n Get the language, region, and script from the cleaned-up tag, plus any variants/extensions" +
        "\r\n Try each of the following in order (where the field exists)" +
        "\r\n   Lookup language-script-region. If in the table, return the result + variants" +
        "\r\n   Lookup language-script. If in the table, return the result (substituting the original region if it exists) + variants" +
        "\r\n   Lookup language-region. If in the table, return the result (substituting the original script if it exists) + variants" +
        "\r\n   Lookup language. If in the table, return the result (substituting the original region and script if either or both exist) + variants" +
        "\r\n" +
        "\r\n Example: Input is zh-ZZZZ-SG." +
        "\r\n Normalize to zh-SG. Lookup in table. No match." +
        "\r\n Remove SG, but remember it. Lookup zh, and get the match (zh-Hans-CN). Substitute SG, and return zh-Hans-SG." +
        "\r\n" +
        "\r\n To Minimize:" +
        "\r\n First get max = maximize(input)." +
        "\r\n Then for trial in {language, language-region, language-script}" +
        "\r\n     If maximize(trial) == max, then return trial." +
        "\r\n If you don't get a match, return max." +
        "\r\n" +
        "\r\n Example: Input is zh-Hant. Maximize to get zh-Hant-TW." +
        "\r\n zh => zh-Hans-CN. No match, so continue." +
        "\r\n zh-TW => zh-Hans-TW. Match, so return zh-TW." +
        "\r\n" +
        "\r\n (A variant of this uses {language, language-script, language-region}): that is, tries script before language." +
        "\r\n toMaximal size: " + toMaximized.size() + 
        "\r\n*/"
        );
    
    printMap(toMaximized);
    
//    if (OUTPUT_STYLE != OutputStyle.XML) {
//      printMap("const MapToMinimalSubtags default_subtags[]", toMinimized, null);
//    }

    System.out.println("\r\nERRORS:\t" + errorCount + "\r\n");
    
  }
  
  private static void doAlt(Map<String, String> toMaximized) {
    // TODO Auto-generated method stub
    Map<String, String> temp = new TreeMap();
    for (String locale : toMaximized.keySet()) {
      String target = toMaximized.get(locale);
      temp.put(toAlt(locale), toAlt(target));
    }
    toMaximized.clear();
    toMaximized.putAll(temp);
  }

  static String maximize(String languageTag, Map<String, String> toMaximized) {
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
    if (region.equals("Zzzz")) {
      ltp.setScript(script = "");
      changed = true;
    }
    if (ltp.getRegion().equals("ZZ")) {
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
          return ltp.set(result).setRegion(region).toString();
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

  /**
   * Verify that we can map from each language, script, and country to something.
   * @param toMaximized
   */
  private static void checkConsistency(Map<String, String> toMaximized) {
    Map<String,String> needMappings = new TreeMap();
    LanguageTagParser parser = new LanguageTagParser();
    for (String maximized : new TreeSet<String>(toMaximized.values())) {
      parser.set(maximized);
      final String language = parser.getLanguage();
      final String script = parser.getScript();
      final String region = parser.getRegion();
      if (language.length() == 0 || script.length() == 0 || region.length() == 0) {
        failure("   { \"" + maximized + "\", \"" + maximized + "\" },   //     " + english.getName(maximized) + "\t\tFailed-Consistency");
        continue;
      }
      addIfNotIn(language, maximized, needMappings, toMaximized, "Consistency");
      addIfNotIn(language + "_" + script, maximized, needMappings, toMaximized, "Consistency");
      addIfNotIn(language + "_" + region, maximized, needMappings, toMaximized, "Consistency");
      addIfNotIn("und_" + script, maximized, needMappings, toMaximized, "Consistency");
      addIfNotIn("und_" + script + "_" + region, maximized, needMappings, toMaximized, "Consistency");
      addIfNotIn("und_" + region, maximized, needMappings, toMaximized, "Consistency");
    }
    toMaximized.putAll(needMappings);
  }

  private static void failure(String string) {
   System.out.println(string);
   errorCount++;
  }

  private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Map<String, String> otherToCheck, String kind) {
    addIfNotIn(key, value, toAdd, otherToCheck == null ? null : otherToCheck.keySet(), null, kind);
  }
  
  private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Set<String> skipKey, Set<String> skipValue, String kind) {
    if (!toAdd.containsKey(key) 
        && (skipKey == null || !skipKey.contains(key)) 
        && (skipValue == null || !skipValue.contains(value))) {
      toAdd.put(key, value);
      if (SHOW_ADD) System.out.println("Adding: " + key + " => " + value + "\t\t" + kind);
    }
  }

  private static void addCountries(Map<String, String> toMaximized) {
    Map <String, Map<String, Double>> scriptToLanguageToSize = new TreeMap();
    
    for (String territory : supplementalData.getTerritoriesWithPopulationData()) {
      Set<String> languages = supplementalData.getLanguagesForTerritoryWithPopulationData(territory);
      String biggestOfficial = null;
      double biggest = -1;
      for (String language : languages) {
        PopulationData info = supplementalData.getLanguageAndTerritoryPopulationData(language, territory);
        // add to info about script

        String script = getScriptForLocale(language);
        if (script != null) {
          Map<String, Double> languageInfo = scriptToLanguageToSize.get(script);
          if (languageInfo == null) scriptToLanguageToSize.put(script, languageInfo = new TreeMap());
          String baseLanguage = language;
          int pos = baseLanguage.indexOf('_');
          if (pos >= 0) {
            baseLanguage = baseLanguage.substring(0,pos);
          }
          Double size = languageInfo.get(baseLanguage);
          languageInfo.put(baseLanguage, (size == null ? 0 : size) + info.getLiteratePopulation());
        }


        final OfficialStatus officialStatus = info.getOfficialStatus();
        if (officialStatus == OfficialStatus.de_facto_official || officialStatus == OfficialStatus.official) {
          double size2 = info.getLiteratePopulation();
          if (biggest < size2) {
            biggest = size2;
            biggestOfficial = language;
          }
        }
      }
      if (biggestOfficial != null) {
        final String replacementTag = "und_" + territory;
        String maximized = biggestOfficial + "_" + territory;
        toMaximized.put(replacementTag, maximized);
        if (SHOW_ADD) System.out.println("Adding: " + replacementTag + " => " + maximized + "\t\tLanguage-Territory");
      }
    }
    
    for  (String script : scriptToLanguageToSize.keySet()) {
      String biggestOfficial = null;
      double biggest = -1;

      final Map<String, Double> languageToSize = scriptToLanguageToSize.get(script);
      for (String language : languageToSize.keySet()) {
        double size = languageToSize.get(language);
        if (biggest < size) {
          biggest = size;
          biggestOfficial = language;
        }
      }
      if (biggestOfficial != null) {
        final String replacementTag = "und_" + script;
        String maximized = biggestOfficial + "_" + script;
        toMaximized.put(replacementTag, maximized);
        if (SHOW_ADD) System.out.println("Adding: " + replacementTag + " => " + maximized + "\t\tUnd-Script");
      }
    }
  }

  private static void closeUnd(Map<String, String> toMaximized) {
    Map<String,String> toAdd = new TreeMap<String,String>();
    for (String oldSource : toMaximized.keySet()) {
      String maximized = toMaximized.get(oldSource);
      if (!maximized.startsWith("und")) {
        int pos = maximized.indexOf("_");
        if (pos >= 0) {
          addIfNotIn( "und" + maximized.substring(pos), maximized, toAdd, toMaximized, "CloseUnd");
        }
      }
    }
    toMaximized.putAll(toAdd);
  }

  /**
   * Generate tags where the deprecated values map to the expanded values
   * @param toMaximized
   */
  private static void addDeprecated(Map<String, String> toMaximized) {
    Map<String, Map<String, List<String>>> typeToTagToReplacement = supplementalData.getLocaleAliasInfo();
    LanguageTagParser p1 = new LanguageTagParser();
    LanguageTagParser p2 = new LanguageTagParser();
    Map<String,String> toAdd = new TreeMap<String,String>();
    while (true) {
      toAdd.clear();
      for (String locale : toMaximized.keySet()) {
        String maximized = toMaximized.get(locale);
        for (String type : typeToTagToReplacement.keySet()) {
          Map<String, List<String>> tagToReplacement = typeToTagToReplacement.get(type);
          for (String tag: tagToReplacement.keySet()) {
            final List<String> list = tagToReplacement.get(tag);
            if (list == null) continue; // we don't have any informatoin
            String replacement = list.get(0);
            
            int pos = getSubtagPosition(locale, replacement);
            if (pos < 0) continue; // no match
            
            // we have a match, try a replacement
            p1.set(locale);
            if (pos == 0) {
              p2.set(tag);
            } else {
              p2.set("und-" + tag);
            }
            // reset fields that should be replaced
            /* Examples
             <languageAlias type="zh-guoyu" replacement="zh-cmn"/> <!-- Mandarin or Standard Chinese -->
             <languageAlias type="in" replacement="id"/> <!-- Indonesian -->
             <languageAlias type="sh" replacement="sr_Latn"/> <!-- Serbo-Croatian -->
             <languageAlias type="sr_CS" replacement="sr_Cyrl_CS"/>
             <territoryAlias type="BQ" replacement="AQ"/> <!-- CLDR: British Antarctic Territory -->
             */
            String f2 = p2.getLanguage();
            if (!f2.equals("und")) p1.setLanguage(f2);
            f2 = p2.getScript();
            if (f2.length() != 0) p2.setScript(f2);
            f2 = p2.getRegion();
            if (f2.length() != 0) p2.setRegion(f2);
            String replacementTag = p1.toString();
            addIfNotIn(replacementTag, maximized, toAdd, toMaximized,"Deprecated");            
          }
        }
      }
      if (toAdd.size() == 0) {
        break;
      }
      toMaximized.putAll(toAdd);
    }
  }

  private static int getSubtagPosition(String locale, String subtags) {
    int pos = -1;
    while (true) {
      pos = locale.indexOf(subtags, pos + 1);
      if (pos < 0) return -1;
      // make sure boundaries are ok
      if (pos != 0) {
        char charBefore = locale.charAt(pos-1);
        if (charBefore != '_' && charBefore != '_') return -1;
      }
      int limit = pos + subtags.length();
      if (limit != locale.length()) {
        char charAfter = locale.charAt(limit);
        if (charAfter != '_' && charAfter != '_') return -1;
      }
      return pos;
    }
  }

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
    PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY, 
        "likelySubtags" + (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
    String spacing = OUTPUT_STYLE == OutputStyle.PLAINTEXT ? "\t" : " ";
    String header = OUTPUT_STYLE != OutputStyle.XML ? "const MapToMaximalSubtags default_subtags[] = {"
      : "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
    + "<!DOCTYPE supplementalData SYSTEM \"http://www.unicode.org/cldr/dtd/1.5.1/ldmlSupplemental.dtd\">\r\n"
    + "<supplementalData>\r\n"
    + "    <version number=\"$Revision$\"/>\r\n"
    + "    <generation date=\"$Date$\"/>\r\n"
    + "    <likelySubtags>";
    String footer = OUTPUT_STYLE  != OutputStyle.XML ? SEPARATOR + "};" 
        : "    </likelySubtags>\r\n"
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
        if (comment.length() > 70 && SEPARATOR.equals("\r\n")) {
          comment = printingName(printingLocale, spacing) +SEPARATOR + "    // " + spacing + "=>" + spacing + printingName(printingTarget, spacing);
        }
        out.print(
            "  {"
            + SEPARATOR + "    // " + comment
            + SEPARATOR + "    \"" + printingLocale + "\","
            + SEPARATOR + "    \"" + printingTarget + "\""
            + "\r\n" + "  }"
        );
      }
    }
    out.println(footer);
    out.close();
  }
  
  private static String printingName(String locale, String spacing) {
    LanguageTagParser parser = new LanguageTagParser().set(locale);
    String lang = parser.getLanguage();
    String script = parser.getScript();
    String region = parser.getRegion();
    return "{" + spacing +
      (lang.equals("und") ? "?" : english.getName(CLDRFile.LANGUAGE_NAME, lang)) + ";" + spacing +
      (script == null || script.equals("") ? "?" : english.getName(CLDRFile.SCRIPT_NAME, script)) + ";" + spacing +
      (region == null || region.equals("") ? "?" : english.getName(CLDRFile.TERRITORY_NAME, region)) + spacing + "}";
  }

  static final String[][] ALT_REVERSAL = {
    {"nb", "no"},
    {"no", "nb"},
    {"he", "iw"},
    {"iw", "he"},
  };
  
  private static String toAlt(String locale) {
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
  
  static String getFirstTag(String locale) {
    int pos = locale.indexOf('_');
    return pos < 0 ? locale : locale.substring(0,pos);
  }

  private static Map<String, String> getBackMapping(Map<String, String> fluffup) {
    Relation<String,String> backMap = new Relation(new TreeMap(), TreeSet.class, BEST_LANGUAGE_COMPARATOR);
    for (String source : fluffup.keySet()) {
      if (source.startsWith("und")) {
        continue;
      }
      String maximized = fluffup.get(source);
      backMap.put(maximized, source); // put in right order
    }
    Map<String,String> returnBackMap = new TreeMap();
    for (String maximized : backMap.keySet()) {
      final Set<String> all = backMap.getAll(maximized);
      final String minimized = all.iterator().next();
      returnBackMap.put(maximized, minimized);
    }
    return returnBackMap;
  }
  
  /**
   * Language tags are presumed to share the first language, except possibly "und". Best is least
   */
  static Comparator BEST_LANGUAGE_COMPARATOR = new Comparator<String>() {
    LanguageTagParser p1 = new LanguageTagParser();
    LanguageTagParser p2 = new LanguageTagParser();
    public int compare(String o1, String o2) {
      if (o1.equals(o2)) return 0;
      p1.set(o1);
      p2.set(o2);
      String lang1 = p1.getLanguage();
      String lang2 = p2.getLanguage();
      
      // compare languages first
      // put und at the end
      int result = lang1.compareTo(lang2);
      if (result != 0) {
        if (lang1.equals("und")) return 1;
        if (lang2.equals("und")) return -1;
        return result;
      }
      
      // now scripts and regions.
      // if they have different numbers of fields, the shorter wins.
      // If there are two fields, region is lowest.
      // The simplest way is to just compare scripts first
      // so zh-TW < zh-Hant, because we first compare "" to Hant
      String script1 = p1.getScript();
      String script2 = p2.getScript();
      int scriptOrder = script1.compareTo(script2);
      if (scriptOrder != 0) return scriptOrder;
      
      String region1 = p1.getRegion();
      String region2 = p2.getRegion();
      int regionOrder = region1.compareTo(region2);
      if (regionOrder != 0) return regionOrder;
      
      return o1.compareTo(o2);
    }
    
  };

  private static void minimize(Map<String, String> fluffup) {
    LanguageTagParser parser = new LanguageTagParser();
    LanguageTagParser targetParser = new LanguageTagParser();
    Set<String> removals = new TreeSet<String>();
    while (true) {
      removals.clear();
      for (String locale : fluffup.keySet()) {
        String target = fluffup.get(locale);
        String region = parser.set(locale).getRegion();
        if (region.length() != 0) {
          parser.setRegion("");
          String newLocale = parser.toString();
          String newTarget = fluffup.get(newLocale);
          if (newTarget != null) {
            newTarget = targetParser.set(newTarget).setRegion(region).toString();
            if (target.equals(newTarget)) {
              removals.add(locale);
              if (SHOW_ADD) System.out.println("Removing: " + locale + " => " + target +"\t\tRedundant with " + newLocale);
              continue;
            }
          }
        }
        String script = parser.set(locale).getScript();
        if (locale.equals("und_Latn_ZA")) {
          System.out.println("*debug*");
        }
        if (script.length() != 0) {
          parser.setScript("");
          String newLocale = parser.toString();
          String newTarget = fluffup.get(newLocale);
          if (newTarget != null) {
            newTarget = targetParser.set(newTarget).setScript(script).toString();
            if (target.equals(newTarget)) {
              removals.add(locale);
              if (SHOW_ADD) System.out.println("Removing: " + locale + " => " + target + "\t\tRedundant with " + newLocale);
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

  private static void addLanguageScript(Map<String, String> fluffup, LanguageTagParser parser) {
    // add script
    Map<String, String> temp = new TreeMap<String, String>();
    while (true) {
      temp.clear();
      for (String target : new TreeSet<String>(fluffup.values())) {
        parser.set(target);
        final String territory = parser.getRegion();
        if (territory.length() == 0) {
          continue;
        }
        parser.setRegion("");
        String possibleSource = parser.toString();
        if (fluffup.containsKey(possibleSource)) {
          continue;
        }
        String other = temp.get(possibleSource);
        if (other != null) {
          if (!target.equals(other)) {
            System.out.println("**Failure with multiple sources in addLanguageScript: "
                + possibleSource + " => " + target + ", " + other);
          }
          continue;
        }
        temp.put(possibleSource, target);
        if (SHOW_ADD) System.out.println("Adding: " + possibleSource + " => " + target + "\t\tLanguage-Script");
      }
      if (temp.size() == 0) {
        break;
      }
      fluffup.putAll(temp);
    }

  }

  private static void addLanguageCountry(Map<String, String> fluffup, LanguageTagParser parser) {
    // add script
    Map<String, String> temp = new TreeMap<String, String>();
    while (true) {
      temp.clear();
      for (String target : new TreeSet<String>(fluffup.values())) {
        parser.set(target);
        String script = parser.getScript();
        if (script.length() == 0) {
          continue;
        }
        parser.setScript("");
        String possibleSource = parser.toString();
        if (fluffup.containsKey(possibleSource)) {
          continue;
        }
        String other = temp.get(possibleSource);
        
        if (other != null) {
          if (!target.equals(other)) {
            script = getScriptForLocale(possibleSource);
            if (script == null) {
              System.out.println("**Failure with multiple sources in addLanguageCountry: "
                  + possibleSource + " => " + target + ", " + other);
              continue; // error message in routine
            }
            parser.setScript(script);
            target = parser.toString();
           }
        }
 
        temp.put(possibleSource, target);
        if (SHOW_ADD) System.out.println("Adding: " + possibleSource + " => " + target + "\t\tLanguageCountry");
      }
      if (temp.size() == 0) {
        break;
      }
      fluffup.putAll(temp);
    }

  }

  private static void  addScript(Map<String, String> fluffup, LanguageTagParser parser) {
    // add script
    Map<String, String> temp = new TreeMap<String, String>();
    while (true) {
      temp.clear();
      Set skipTarget = fluffup.keySet();
      for (String locale : fluffup.keySet()) {
        String target = fluffup.get(locale);
        parser.set(target);
        if (parser.getScript().length() != 0) {
          continue;
        }
        String script = getScriptForLocale(target);

        if (script == null) {
          continue; // error message in routine
        }
        parser.setScript(script);
        String furtherTarget = parser.toString();
        addIfNotIn(target, furtherTarget, temp, fluffup, "Script");
      }
      if (temp.size() == 0) {
        break;
      }
      fluffup.putAll(temp);
    }
  }

  private static String getScriptForLocale(String locale) {
    String result = getScriptForLocale2(locale);
    if (result != null) return result;
    int pos = locale.indexOf('_');
    if (pos >= 0) {
      result = getScriptForLocale2(locale.substring(0,pos));
    }
    return result;
  }
  
  private static String getScriptForLocale2(String locale) {
    String cached = localeToScriptCache.get(locale);
    if (cached != null) {
      return cached;
    }
    CLDRFile cldrFile;
    try {
      cldrFile = factory.make(locale, true);
    } catch (RuntimeException e) {
     return null;
    }
    UnicodeSet exemplars = getExemplarSet(cldrFile, "");
    Set<String> CLDRScripts = getScriptsFromUnicodeSet(exemplars);
    CLDRScripts.remove("Zzzz");
    if (CLDRScripts.size() == 1) {
      String script = CLDRScripts.iterator().next();
      localeToScriptCache.put(locale, script);
      return script;
    } else if (CLDRScripts.size() == 0) {
      System.out.println("**Failed to get script for: " + locale);
      return null;
    } else {
      System.out.println("**Failed, too many scripts for: " + locale + ", " + CLDRScripts);
      return null;
    }     
  }

  private static Map<String, String> closeMapping(Map<String, String> fluffup) {
    if (SHOW_ADD) System.out.flush();
    Map<String,String> temp = new TreeMap<String,String>();
    while (true) {
      temp.clear();
      for (String locale : fluffup.keySet()) {
        String target = fluffup.get(locale);
        if (target.equals("si_Sinh") || target.equals("zh-Hani")) {
          System.out.println("????");
        }
        String furtherTarget = fluffup.get(target);
        if (furtherTarget == null) {
          continue;
        }
        addIfNotIn(locale, furtherTarget, temp, null, "Close");
      }
      if (temp.size() == 0) {
        break;
      }
      fluffup.putAll(temp);
    }
    if (SHOW_ADD) System.out.flush();
    return temp;
  }
  
  public static Set<String> getScriptsFromUnicodeSet(UnicodeSet exemplars) {
    // use bits first, since that's faster
    BitSet scriptBits = new BitSet();
    boolean show = false;
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
      if (show)
        System.out.println(Integer.toHexString(it.codepoint));
      if (it.codepoint != it.IS_STRING) {
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
  
  static String[][] SpecialCases = {
    { "zh_Hani", "zh_Hans_CN"},
    { "si_Sinh", "si_Sinh_LK"},
    { "ii", "ii_CN"}, // Sichuan Yi (Yi)
    { "iu", "iu_CA"}, //    Inuktitut (Unified Canadian Aboriginal Syllabics)
    { "und", "en"}, //    English default
  };
  
  static String[][] SpecialScripts = {
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
  static Map<String,String> localeToScriptCache = new TreeMap();
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