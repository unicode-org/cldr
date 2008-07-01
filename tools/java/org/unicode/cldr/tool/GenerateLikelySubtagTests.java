package org.unicode.cldr.tool;

import org.unicode.cldr.tool.GenerateMaximalLocales.OutputStyle;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GenerateLikelySubtagTests {
  private static final String TAG_SEPARATOR = "_";
  private static final String SEPARATOR = Utility.LINE_SEPARATOR;
  private static final boolean DEBUG = true;
  private static final OutputStyle OUTPUT_STYLE = OutputStyle.XML;
  private static PrintWriter out;

  public static void main(String[] args) throws IOException {
    out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY, 
            "test/supplemental/likelySubtagTests" +  (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
     if (OUTPUT_STYLE == OutputStyle.C) {
       out.println("// START");
     } else {
      out.println("<?xml version='1.0' encoding='UTF-8' ?>" + Utility.LINE_SEPARATOR +
        "<!DOCTYPE cldrTest SYSTEM 'http://www.unicode.org/cldr/dtd/1.5.1/cldrTest.dtd'>" + Utility.LINE_SEPARATOR +
        "<!-- For information, see readme.html -->" + Utility.LINE_SEPARATOR +
         "<cldrTest version='1.5.1' base='aa'>" + Utility.LINE_SEPARATOR +
         "  <likelySubtags>");
     }
    SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
    Map<String, String> likelySubtags = supplementalData.getLikelySubtags();
    
    if (OUTPUT_STYLE == OutputStyle.C) {
      writeTestLine2("FROM", "ADD-LIKELY", "REMOVE-LIKELY");
    }
    Set<String> testedAlready = new HashSet();
    
    for (final String from : likelySubtags.keySet()) {
      final String to = likelySubtags.get(from);
      final String max = writeTestLine(from, likelySubtags);
      if (!to.equals(max)) {
        throw new IllegalArgumentException();
      }
      testedAlready.add(to);
    }
    LanguageTagParser ltp = new LanguageTagParser();
    for (String lang : new String[] {"und", "es", "zh", "art"}) { // 
      ltp.setLanguage(lang);
      for (String script : new String[] {"", "Zzzz", "Latn", "Hans", "Hant", "Moon"}) {
        ltp.setScript(script);
       for (String region : new String[] {"", "ZZ", "CN", "TW", "HK", "AQ"}) {
         ltp.setRegion(region);
         String tag = ltp.toString();
         if (testedAlready.contains(tag)) {
           continue;
         }
         writeTestLine(tag, likelySubtags);
         testedAlready.add(tag);
        }
      }
    }
    if (OUTPUT_STYLE == OutputStyle.C) {
      out.println(Utility.LINE_SEPARATOR + "// END");
    } else {
     out.println("  </likelySubtags>" + Utility.LINE_SEPARATOR + "</cldrTest>");
    }
    out.close();
  }

  private static String writeTestLine(final String from, Map<String, String> likelySubtags) {
    final String maxFrom = maximize(from, likelySubtags);
    final String minFrom = minimize(from, likelySubtags, true);
    writeTestLine2(from, maxFrom, minFrom);
    return maxFrom;
  }

  private static void writeTestLine2(final String from, final String maxFrom, final String minFrom) {
    if (OUTPUT_STYLE == OutputStyle.C) {
    out.print(
        "  {"
        //+ SEPARATOR + "    // " + comment
        + SEPARATOR + "    " + getItem(from) +","
        + SEPARATOR + "    " + getItem(maxFrom) +","
        + SEPARATOR + "    " + getItem(minFrom) //+","
        + Utility.LINE_SEPARATOR + "  },");
    } else {
      out.println("    <!-- " + printNameOrError(from) + " \u2192 " + 
          printNameOrError(maxFrom) + " \u2192 " +
          printNameOrError(minFrom) +" -->");
      out.println("    <result input='" + getNameOrError(from) +
          "' add='" + getNameOrError(maxFrom) +
          "' remove='" + getNameOrError(minFrom) + "'/>");
    }
    
  }

  private static String printNameOrError(final String maxFrom) {
    String result = GenerateMaximalLocales.printingName(maxFrom,"");
    if (result == null) {
      return "ERROR";
    }
    return result;
  }

  private static String getNameOrError(final String from) {
    String result = GenerateMaximalLocales.toAlt(from, true);
    if (result == null) {
      return "ERROR";
    }
    return result;
  }
  
  private static String getItem(String from) {
    final String toAlt = getNameOrError(from);
    if (toAlt == null) {
      return null;
    }
    return "\"" +toAlt + "\"";
  }
  
  public static String maximize(String languageTag, Map<String, String> toMaximized) {
    LanguageTagParser ltp = new LanguageTagParser();
    if (DEBUG && languageTag.equals("es" + TAG_SEPARATOR + "Hans" + TAG_SEPARATOR + "CN")) {
      System.out.print(""); // debug
    }
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
    if (script.equals("Zzzz")) {
      ltp.setScript(script = "");
      changed = true;
    }
    if (region.equals("ZZ")) {
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
//    if (!language.equals("und") && script.length() != 0 && region.length() != 0) {
//      return languageTag; // it was ok, and we couldn't do anything with it
//    }
    return null; // couldn't maximize
  }
  
  public static String minimize(String input, Map<String, String> toMaximized, boolean favorRegion) {
    String maximized = maximize(input, toMaximized);
    if (maximized == null) {
      return null;
    }
    if (DEBUG && maximized.equals("sr" + TAG_SEPARATOR + "Latn" + TAG_SEPARATOR + "RS")) {
      System.out.print(""); // debug
    }
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

}