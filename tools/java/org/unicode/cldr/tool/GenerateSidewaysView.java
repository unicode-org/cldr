/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.ShowData.DataShower;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.LanguageTagParser.Fields;
import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * This is a simple class that walks through the CLDR hierarchy.
 * It gathers together all the items from all the locales that share the
 * same element chain, and thus presents a "sideways" view of the data, in files called
 * by_type/X.html, where X is a type. X may be the concatenation of more than more than
 * one element, where the file would otherwise be too large.
 * @author medavis
 */
/*
 Notes:
 http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
 http://developers.sun.com/dev/coolstuff/xml/readme.html
 http://lists.xml.org/archives/xml-dev/200007/msg00284.html
 http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */
public class GenerateSidewaysView {
  // debug flags
  static final boolean DEBUG = false;
  static final boolean DEBUG2 = false;
  static final boolean DEBUG_SHOW_ADD = false;
  static final boolean DEBUG_ELEMENT = false;
  static final boolean DEBUG_SHOW_BAT = false;
  static boolean usePrettyPath = true;
  
  static final boolean FIX_ZONE_ALIASES = true;
  
  private static final int
  HELP1 = 0,
  HELP2 = 1,
  SOURCEDIR = 2,
  DESTDIR = 3,
  MATCH = 4,
  SKIP = 5,
  TZADIR = 6,
  NONVALIDATING = 7,
  SHOW_DTD = 8,
  TRANSLIT = 9,
  PATH = 10;
  
  private static final String NEWLINE = "\n";
  
  private static final UOption[] options = {
    UOption.HELP_H(),
    UOption.HELP_QUESTION_MARK(),
    UOption.SOURCEDIR().setDefault(CldrUtility.MAIN_DIRECTORY),
    UOption.DESTDIR().setDefault(CldrUtility.CHART_DIRECTORY + File.separatorChar+  "by_type/"), // C:/cvsdata/unicode/cldr/diff/by_type/
    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
    UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
    UOption.create("nonvalidating", 'n', UOption.NO_ARG),
    UOption.create("dtd", 'w', UOption.NO_ARG),
    UOption.create("transliterate", 'y', UOption.NO_ARG),
    UOption.create("path", 'p', UOption.REQUIRES_ARG),
  };
  
  private static final Matcher altProposedMatcher = CLDRFile.ALT_PROPOSED_PATTERN.matcher("");
  private static final UnicodeSet ALL_CHARS = new UnicodeSet(0, 0x10FFFF);
  protected static final UnicodeSet COMBINING = (UnicodeSet) new UnicodeSet("[[:m:]]").freeze();
  
  static int getFirstScript(UnicodeSet exemplars) {
    int cp;
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
      int script = UScript.getScript(it.codepoint);
      if (script == UScript.COMMON || script == UScript.INHERITED) {
        continue;
      }
      return script;
    }
    return UScript.COMMON;
  }

  static Comparator UCA;
  static {
    RuleBasedCollator UCA2 = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
    UCA2.setNumericCollation(true);
    UCA2.setStrength(UCA2.IDENTICAL);
    UCA = new com.ibm.icu.impl.MultiComparator(UCA2, new UTF16.StringComparator(true, false, 0) );
  }
  
  private static String timeZoneAliasDir = null;
  private static Map<String,Map<String,Set<String>>> path_value_locales = new TreeMap();
  private static XPathParts parts = new XPathParts(null, null);
  private static long startTime = System.currentTimeMillis();
  
  static RuleBasedCollator standardCollation = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
  static {
    standardCollation.setStrength(Collator.IDENTICAL);
    standardCollation.setNumericCollation(true);
  }
  
  private static CLDRFile english;
  private static  DataShower dataShower = new DataShower();
  private static  Matcher pathMatcher;
  
  public static void main(String[] args) throws SAXException, IOException {
    startTime = System.currentTimeMillis();
    CldrUtility.registerExtraTransliterators();
    UOption.parseArgs(args, options);
    
    pathMatcher = options[PATH].value == null ? null : Pattern.compile(options[PATH].value).matcher("");

    Factory cldrFactory = CLDRFile.Factory.make(options[SOURCEDIR].value, options[MATCH].value);
    english = cldrFactory.make("en", true);
    
    // now get the info
    
    loadInformation(cldrFactory);
    String oldMain = "";
    PrintWriter out = null;
    
    System.out.println("Getting types");
    String[] partial = {""};
    Set types = new TreeSet();
    for (Iterator it = path_value_locales.keySet().iterator(); it.hasNext();) {       	
      String path = (String)it.next();
      String main = getFileName(path, partial);
      if (!main.equals(oldMain)) {
        oldMain = main;
        types.add(main);
      }
    }
    
    System.out.println("Printing files in " + new File(options[DESTDIR].value).getAbsolutePath());
    Transliterator toLatin = Transliterator.getInstance("any-latin");
    toHTML = TransliteratorUtilities.toHTML;
    UnicodeSet BIDI_R = new UnicodeSet("[[:Bidi_Class=R:][:Bidi_Class=AL:]]");
    
    for (Iterator it = path_value_locales.keySet().iterator(); it.hasNext();) {       	
      String path = (String)it.next();
      String main = getFileName(path, partial);
      if (!main.equals(oldMain)) {
        oldMain = main;
        out = start(out, main, types);
      }
      String key = partial[0];
      String anchor = toHTML.transliterate(key);
      if (usePrettyPath) {
          String originalPath = prettyPath.getOriginal(path);
          String englishValue = english.getStringValue (originalPath);
          if (englishValue != null) key += " (English: " + englishValue + ")";
      }

      out.println("<tr><th colSpan='2' class='path'><a name=\"" + anchor + "\">" + toHTML.transliterate(key) + "</a></th><tr>");
      Map value_locales = (Map) path_value_locales.get(path);
      for (Iterator it2 = value_locales.keySet().iterator(); it2.hasNext();) {
        String value = (String)it2.next();
//        String outValue = toHTML.transliterate(value);
//        String transValue = value;
//        try {
//          transValue = toLatin.transliterate(value);
//        } catch (RuntimeException e) {
//        }
//        if (!transValue.equals(value)) {
//          outValue = "<span title='" + toHTML.transliterate(transValue) + "'>" + outValue + "</span>";
//        }
        String valueClass = " class='value'";
        if (dataShower.getBidiStyle(value).length() != 0) {
          valueClass = " class='rtl_value'";
        }
        out.println("<tr><th" + valueClass + ">" + dataShower.getPrettyValue(value) + "</th><td class='td'>");
        Set locales = (Set) value_locales.get(value);
        boolean first = true;
        for (Iterator it3 = locales.iterator(); it3.hasNext();) {
          String locale = (String)it3.next();
          if (first) first = false;
          else out.print(" ");
          if (locale.endsWith("*")) {
            locale = locale.substring(0,locale.length()-1);
            out.print("<i>\u00B7" + locale + "\u00B7</i>");
          } else {
            out.print("\u00B7" + locale + "\u00B7");         
          }
        }
        out.println("</td><tr>");
      }
    }
    out = showExemplars(out, types);
    finish(out);
    System.out.println("Done in " + new RuleBasedNumberFormat(new ULocale("en"), RuleBasedNumberFormat.DURATION)
        .format((System.currentTimeMillis()-startTime)/1000.0));
  }
  
//  static Comparator UCA;
//  static {
//    RuleBasedCollator UCA2 = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
//    UCA2.setNumericCollation(true);
//    UCA2.setStrength(UCA2.IDENTICAL);
//    UCA = new CollectionUtilities.MultiComparator(UCA2, new UTF16.StringComparator(true, false, 0) );
//  }

  private static PrintWriter showExemplars(PrintWriter out, Set types) throws IOException {
    showExemplars2(out, types, "misc.exemplarCharacters", "");
    showExemplars2(out, types, "misc.exemplarCharacters-index", "[@type=\"index\"]");
    //showExemplars2(out, types, "misc.exemplarCharacters-auxiliary", "[@type=\"auxiliary\"]");
    //showExemplars2(out, types, "misc.exemplarCharacters-currencySymbol", "[@type=\"currencySymbol\"]");
    return out;
  }
  
  private static PrintWriter showExemplars2(PrintWriter out, Set types, String filename, String variant) throws IOException {
    finish(out);
    out = start(out,filename, types);
    out.println("<table>");
    String cleanPath = prettyPath.getPrettyPath("//ldml/characters/exemplarCharacters" + variant);
    Map<String, Set<String>> value_locales = path_value_locales.get(cleanPath);
    
    // TODO change logic so that aux characters and currencySymbol characters work well.
    
    Map<String,UnicodeMap<Set<String>>> script_UnicodeMap = new TreeMap<String,UnicodeMap<Set<String>>>();
    //UnicodeMap mapping = new UnicodeMap();
    UnicodeSet stuffToSkip = new UnicodeSet("[:Han:]");
    
    // get the locale information
    for (String value : value_locales.keySet()) {
      UnicodeSet exemplars = new UnicodeSet(value);
      exemplars.removeAll(stuffToSkip);
      Set<String> locales = value_locales.get(value);
      String script = UScript.getName(getFirstScript(exemplars));
      UnicodeMap<Set<String>> mapping = script_UnicodeMap.get(script);
      if (mapping == null) script_UnicodeMap.put(script, mapping = new UnicodeMap());
      mapping.composeWith(exemplars, locales, setComposer);
    }
    for (String script : script_UnicodeMap.keySet()) {
      UnicodeMap<Set<String>> mapping = script_UnicodeMap.get(script);
      writeCharToLocaleMapping(out, script, mapping);
    }
    return out;
  }

  private static void writeCharToLocaleMapping(PrintWriter out, String script, UnicodeMap<Set<String>> mapping) {
    BreakIterator charBreaks = BreakIterator.getCharacterInstance(ULocale.ROOT); // TODO, make default language for script
    System.out.println("@@Exemplars for\t" + script + "\t" + mapping.keySet());
    if (script.equals("Hangul") || script.equals("Common")) {
      return; // skip these
    }
    // find out all the locales and all the characters
    Set<String> allLocales = new TreeSet<String>(UCA);
    Set<String> allChars = new TreeSet<String>(UCA);
    Set<String> allStrings = new TreeSet<String>(UCA);
    for (Set<String> locales : mapping.getAvailableValues()) {
      allLocales.addAll(locales);
      UnicodeSet unicodeSet = mapping.keySet(locales);
      for (String item : unicodeSet) {
        charBreaks.setText(item);
        int endFirst = charBreaks.next();
        if (endFirst == item.length()) {
          allChars.add(item);
        } else {
          allStrings.add(item);
        }
      }
    }
    // get the columns, and show them
    out.println("</table>");
    out.println("<table class='table'  style='width:1%'>");
    out.println("<caption>" + script + "</caption>");
    out.println("<tr><th class='path' colSpan='2'>Locale \\\u00a0Chars</td>");
    for (String item : allChars) {
      out.println("<th class='path'>" + displayCharacter(item) + "</th>");
    }
    out.println("<th class='path'>Clusters</td></tr>");

    for (String locale : allLocales) {
      out.println("<tr><th class='path'>" + cleanLocale(locale, false) + "</th><th class='path'>" + cleanLocale(locale, true) + "</th>");
      for (String item : allChars) {
        //String exemplarsWithoutBrackets = displayExemplars(item);
        if (mapping.get(item).contains(locale)) {
          out.println("<td class='path'>" + displayCharacter(item) + "</td>");
        } else {
          out.println("<td class='value'>\u00a0</td>");
        }
      }
      // now strings, if any
      out.println("<td class='path'>");
      StringBuilder strings = new StringBuilder();
      for (String item : allStrings) {
        //String exemplarsWithoutBrackets = displayExemplars(item);
        if (mapping.get(item).contains(locale)) {
          if (strings.length() != 0) strings.append(' ');
          strings.append(displayCharacter(item));
        }
      }
      out.println(displayCharacter(strings.toString()) + "</td>");

      out.println("</tr>");
    }
  }
  
  static final UnicodeSet NONSPACING = new UnicodeSet("[[:Mn:][:Me:]]").freeze();
  
  public static String displayCharacter(String item) {
    if (item.length() == 0) return "<i>none</i>";
    int ch = item.codePointAt(0);
    if (NONSPACING.contains(ch)) {
      item = "\u00a0" + item + "\u00a0";
    }
    String result = toHTML.transform(item);
    return result;
  }

  static LanguageTagParser cleanLocaleParser = new LanguageTagParser();
  static Set<Fields> allButScripts = EnumSet.allOf(Fields.class);
  static {
    allButScripts.remove(Fields.SCRIPT);
  }
  
  private static String cleanLocale(String item, boolean name) {
    if (item == null) {
      return "<i>null</i>";
    }
    boolean draft = item.endsWith("*");
    if (draft) {
      item = item.substring(0,item.length()-1);
    }
    cleanLocaleParser.set(item);
    item = cleanLocaleParser.toString(allButScripts);
    String core = item;
    item = toHTML.transform(item);
    if (name) {
      item = english.getName(core);
      item = item == null ? "<i>null</i>" : toHTML.transform(item);
    }
    if (draft) {
      item = "<i>" + item + "</i>";
    }
    return item;
  }

  private static void showExemplarRow(PrintWriter out, Set<String> allLocales, UnicodeSet lastChars, Set locales) {
    String exemplarsWithoutBrackets = displayExemplars(lastChars);
    out.println("<tr><th class='path'>" + exemplarsWithoutBrackets + "</th>");
    for (String item : allLocales) {
      String cleanItem;
      if (locales.contains(item)) {
        cleanItem = "<th class='value'>" + cleanLocale(item, false) + "</th>";
      } else {
        cleanItem = "<td class='value'>\u00a0</td>";
      }
      out.println(cleanItem);
    }
    out.println("</tr>");
  }

  private static final StringTransform MyTransform = new StringTransform() {

    public String transform(String source) {
      StringBuilder builder = new StringBuilder();
      int cp = 0;
      builder.append("<span title='");
      String prefix = "";
      for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
        cp = UTF16.charAt(source, i);
        if (i == 0) {
          if (COMBINING.contains(cp)) {
            prefix = "\u25CC";
          }
        } else {
          builder.append(" + ");
        }
        builder.append("U+").append(com.ibm.icu.impl.Utility.hex(cp,4)).append(' ').append(UCharacter.getExtendedName(cp));
      }
      builder.append("'>").append(prefix).append(source).append("</span>");
      return builder.toString();
    }
    
  };

  private static String displayExemplars(UnicodeSet lastChars) {
    String exemplarsWithoutBrackets = new PrettyPrinter()
    .setOrdering(UCA != null ? UCA : Collator.getInstance(ULocale.ROOT))
    .setSpaceComparator(UCA != null ? UCA : Collator.getInstance(ULocale.ROOT)
            .setStrength2(Collator.PRIMARY))
            .setCompressRanges(true)
            .setToQuote(ALL_CHARS)
            .setQuoter(MyTransform)
            .format(lastChars);
    exemplarsWithoutBrackets = exemplarsWithoutBrackets.substring(1, exemplarsWithoutBrackets.length() - 1);
    return exemplarsWithoutBrackets;
  }
  
//  private static boolean isNextCharacter(String last, String value) {
//    if (UTF16.hasMoreCodePointsThan(last, 1)) return false;
//    if (UTF16.hasMoreCodePointsThan(value, 1)) return false;
//    int lastChar = UTF16.charAt(last,0);
//    int valueChar = UTF16.charAt(value,0);
//    return lastChar + 1 == valueChar;
//  }

  static UnicodeMap.Composer setComposer = new UnicodeMap.Composer() {
    public Object compose(int codepoint, String string, Object a, Object b) {
      if (a == null) {
        return b;
      } else if (b == null) {
        return a;
      } else {
        TreeSet<String> result = new TreeSet<String>((Set<String>)a);
        result.addAll((Set<String>)b);
        return result;
      }
    }
  };

  private static void loadInformation(Factory cldrFactory) {
    Set alllocales = cldrFactory.getAvailable();
    String[] postFix = new String[]{""};
    // gather all information
    // TODO tweek for value-laden attributes
    for (Iterator it = alllocales.iterator(); it.hasNext();) {
      String localeID = (String) it.next();
      System.out.println("Loading: " + localeID);
      System.out.flush();
      
      CLDRFile cldrFile = cldrFactory.make(localeID, localeID.equals("root"));
      if (cldrFile.isNonInheriting()) continue;
      for (Iterator it2 = cldrFile.iterator(); it2.hasNext();) {
        String path = (String) it2.next();
        if (pathMatcher != null && !pathMatcher.reset(path).matches()) {
          continue;
        }
        if (altProposedMatcher.reset(path).matches()) {
          continue;
        }
        if (path.indexOf("/alias") >= 0) continue;
        if (path.indexOf("/identity") >= 0) continue;
        if (path.indexOf("/references") >= 0) continue;
        String cleanPath = fixPath(path, postFix);
        String fullPath = cldrFile.getFullXPath(path);
        String value = getValue(cldrFile, path, fullPath);
        if (value == null) {
          continue;
        }
        if (fullPath.indexOf("[@draft=\"unconfirmed\"]") >= 0
                || fullPath.indexOf("[@draft=\"provisional\"]") >= 0) {
          postFix[0] = "*";
        }
        Map value_locales = (Map) path_value_locales.get(cleanPath);
        if (value_locales == null ) {
          path_value_locales.put(cleanPath, value_locales = new TreeMap(standardCollation));
        }
        Set locales = (Set) value_locales.get(value);
        if (locales == null) {
          value_locales.put(value, locales = new TreeSet());
        }
        locales.add(localeID + postFix[0]);
      }
    }
  }
  
  static org.unicode.cldr.util.PrettyPath prettyPath = new org.unicode.cldr.util.PrettyPath();
  /**
   * 
   */
  private static String fixPath(String path, String[] localePrefix) {
    localePrefix[0] = "";
    if (path.indexOf("[@alt=") >= 0 || path.indexOf("[@draft=") >= 0) {
      localePrefix[0] = "*";
      path = removeAttributes(path, skipSet);
    }
    if (usePrettyPath) path = prettyPath.getPrettyPath(path);
    return path;
  }
  
  private static String removeAttributes(String xpath, Set skipAttributes) {
    XPathParts parts = new XPathParts(null,null).set(xpath);
    removeAttributes(parts, skipAttributes);
    return parts.toString();
  }
  
  /**
   * 
   */
  private static void removeAttributes(XPathParts parts, Set skipAttributes) {
    for (int i = 0; i < parts.size(); ++i) {
      String element = parts.getElement(i);
      Map attributes = parts.getAttributes(i);
      for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
        String attribute = (String) it.next();
        if (skipAttributes.contains(attribute)) it.remove();
      }
    }
  }
  
  static Set skipSet = new HashSet(Arrays.asList(new String[]{"draft", "alt"}));
  
  static Status status = new Status();

  /**
   * 
   */
  private static String getValue(CLDRFile cldrFile, String path, String fullPath) {
    String value = cldrFile.getStringValue(path);
    if (value == null) {
      System.out.println("Null value for " + path);
      return value;
    }
    cldrFile.getSourceLocaleID(path, status);
    if (!path.equals(status.pathWhereFound)) {
      //value = "[" + prettyPath.getPrettyPath(status.pathWhereFound, false) + "]";
      value = null;
      return value;
    }
    if (value.length() == 0) {
      parts.set(fullPath);
      removeAttributes(parts, skipSet);
      int limit = parts.size();
      value = parts.toString(limit-1, limit);
      return value;
    }
    return value;
  }
  
  /**
   * 
   */
  private static String getFileName(String inputPath, String[] partial) {
    if (usePrettyPath) {
      String path = prettyPath.getOutputForm(inputPath);
      int pos = path.lastIndexOf('|');
      partial[0] = path.substring(pos+1);
      if (pos < 0) return path;
      try {
        String onset = path.substring(0,pos).replace('|','.');
        if (path.contains("exemplarCharacters") && path.contains("|index")) {
          return onset + "-index";
        }
        return onset;
      } catch (RuntimeException e) {
        throw (IllegalArgumentException) new IllegalArgumentException("input path: " + inputPath + "\tpretty: " + path).initCause(e);
      }
    }
    parts.set(inputPath);
    int start = 1;
    String main = parts.getElement(start);
    if (main.equals("localeDisplayNames")
        || main.equals("dates")
        || main.equals("numbers")) {
      start = 2;
      String part2 = parts.getElement(start);
      main += "_" + part2;
      if (part2.equals("calendars")) {
        start = 3;
        Map m = parts.getAttributes(start);
        part2 = (String) m.get("type");
        main += "_" + part2;				
      }
    }
    partial[0] = parts.toString(start + 1, parts.size());
    return main;
  }
  
  static String[] headerAndFooter = new String[2];
  private static Transliterator toHTML;

  /**
   * 
   */
  private static PrintWriter start(PrintWriter out, String main, Set types) throws IOException {
    finish(out);
    out = writeHeader(main);
    boolean first = true;
    String lastMain = "";
    for (Iterator it = types.iterator(); it.hasNext();) {
      String fileName = (String) it.next();
      int breakPos = fileName.indexOf('.');
      if (breakPos >= 0) {
        String mainName = fileName.substring(0,breakPos);
        String subName = fileName.substring(breakPos+1);
        if (!mainName.equals(lastMain)) {
          if (lastMain.length() != 0) {
            out.print("<br>");
          }
          out.println("<b>" + mainName + "</b>: ");
          lastMain = mainName;
        } else {
          out.println(" | ");
        }
        out.println("<a href='" + fileName + 
            ".html'>" + subName +
        "</a>");
        continue;
      }
      if (first) first = false;
      else out.println(" | ");
      out.println("<a href='" + fileName + 
          ".html'>" + fileName +
      "</a>");
    }
    out.println("</p></blockquote><table class='table'>");
    return out;
  }

  private static PrintWriter writeHeader(String main) throws IOException {
    PrintWriter out;
    out = BagFormatter.openUTF8Writer(options[DESTDIR].value, main + ".html");


    ShowData.getChartTemplate("By-Type Chart: " + main,
    ShowLanguages.CHART_DISPLAY_VERSION, 
    "",
    //"<link rel='stylesheet' type='text/css' href='by_type.css'>" +
//    "<style type='text/css'>" + Utility.LINE_SEPARATOR +
//    "h1 {margin-bottom:1em}" + Utility.LINE_SEPARATOR +
//    "</style>" + Utility.LINE_SEPARATOR,
    headerAndFooter);
    out.println(headerAndFooter[0]);
    out.println("<blockquote><p>");
    return out;
  }
  
  /**
   * 
   */
  private static void finish(PrintWriter out) {
    if (out == null) return;
    out.println("</table>");
    out.println(headerAndFooter[1]);
    out.close();
  }
  
}
