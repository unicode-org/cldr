/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
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
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
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
    UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
    UOption.DESTDIR().setDefault(Utility.BASE_DIRECTORY + "diff/by_type/"), // C:/cvsdata/unicode/cldr/diff/by_type/
    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
    UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
    UOption.create("nonvalidating", 'n', UOption.NO_ARG),
    UOption.create("dtd", 'w', UOption.NO_ARG),
    UOption.create("transliterate", 'y', UOption.NO_ARG),
    UOption.create("path", 'p', UOption.REQUIRES_ARG),
  };
  private static final Matcher altProposedMatcher = CLDRFile.ALT_PROPOSED_PATTERN.matcher("");
  
  private static String timeZoneAliasDir = null;
  private static Map path_value_locales = new TreeMap();
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
    Utility.registerExtraTransliterators();
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
    
    System.out.println("Printing files");
    Transliterator toLatin = Transliterator.getInstance("any-latin");
    Transliterator toHTML = TransliteratorUtilities.toHTML;
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
    finish(out);
    System.out.println("Done in " + new RuleBasedNumberFormat(new ULocale("en"), RuleBasedNumberFormat.DURATION)
        .format((System.currentTimeMillis()-startTime)/1000.0));
  }
  
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
        if (fullPath.indexOf("[@draft=") >= 0) postFix[0] = "*";
        Map value_locales = (Map) path_value_locales.get(cleanPath);
        if (value_locales == null ) path_value_locales.put(cleanPath, value_locales = new TreeMap(standardCollation));
        Set locales = (Set) value_locales.get(value);
        if (locales == null) value_locales.put(value, locales = new TreeSet());
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
  /**
   * 
   */
  private static String getValue(CLDRFile cldrFile, String path, String fullPath) {
    String value = cldrFile.getStringValue(path);
    if (value == null) {
      System.out.println("Null value for " + path);
      return value;
    }
    if (value.length() == 0) {
      parts.set(fullPath);
      removeAttributes(parts, skipSet);
      int limit = parts.size();
      value = parts.toString(limit-1, limit);
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
      try {
        return path.substring(0,pos).replace('|','.');
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

  /**
   * 
   */
  private static PrintWriter start(PrintWriter out, String main, Set types) throws IOException {
    finish(out);
    out = BagFormatter.openUTF8Writer(options[DESTDIR].value, main + ".html");


    ShowData.getChartTemplate("By-Type Chart: " + main,
    english.getDtdVersion(), 
    "",
    //"<link rel='stylesheet' type='text/css' href='by_type.css'>" +
//    "<style type='text/css'>\r\n" +
//    "h1 {margin-bottom:1em}\r\n" +
//    "</style>\r\n",
    headerAndFooter);
    out.println(headerAndFooter[0]);
    out.println("<blockquote><p>");
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
