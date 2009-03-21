/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ExtractCollationRules;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import org.unicode.cldr.icu.CollectionUtilities;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import org.unicode.cldr.util.Utility.VariableReplacer;

public class ShowData {
  private static final int HELP1 = 0, HELP2 = 1, SOURCEDIR = 2, DESTDIR = 3,
      MATCH = 4, GET_SCRIPTS = 5;

  private static final UOption[] options = { UOption.HELP_H(),
      UOption.HELP_QUESTION_MARK(),
      UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY), // C:\cvsdata/unicode\cldr\diff\summary
      UOption.DESTDIR().setDefault(Utility.CHART_DIRECTORY + "summary/"),
      UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
      UOption.create("getscript", 'g', UOption.NO_ARG), };

  static RuleBasedCollator uca = (RuleBasedCollator) Collator
      .getInstance(ULocale.ROOT);
  {
    uca.setNumericCollation(true);
  }

  static PrettyPath prettyPathMaker = new PrettyPath();

  static CLDRFile english;

  static Set locales;

  static Factory cldrFactory;

  public static void main(String[] args) throws Exception {
    // String p =
    // prettyPathMaker.getPrettyPath("//ldml/characters/exemplarCharacters[@alt=\"proposed-u151-4\"]");
    // String q = prettyPathMaker.getOriginal(p);

    double deltaTime = System.currentTimeMillis();
    try {

      UOption.parseArgs(args, options);
      String sourceDir = options[SOURCEDIR].value; // Utility.COMMON_DIRECTORY
                                                    // + "main/";
      String targetDir = options[DESTDIR].value; // Utility.GEN_DIRECTORY +
                                                  // "main/";
      cldrFactory = Factory.make(sourceDir, ".*");
      english = (CLDRFile) cldrFactory.make("en", true);

      if (options[GET_SCRIPTS].doesOccur) {
        getScripts();
        return;
      }

      Utility.registerExtraTransliterators();

//      Factory collationFactory = Factory
//          .make(sourceDir.replace("incoming/vetted/","common/") + "../collation/", ".*");
//      ExtractCollationRules collationRules = new ExtractCollationRules();

      locales = new TreeSet(cldrFactory.getAvailable());
      new Utility.MatcherFilter(options[MATCH].value).retainAll(locales);
      // Set paths = new TreeSet();
      Set prettySet = new TreeSet(uca);
      Set skipList = new HashSet(Arrays.asList(new String[] { "id" }));

      CLDRFile.Status status = new CLDRFile.Status();
      LocaleIDParser localeIDParser = new LocaleIDParser();

      Map nonDistinguishingAttributes = new LinkedHashMap();
      CLDRFile parent = null;

      for (Iterator it = locales.iterator(); it.hasNext();) {
        String locale = (String) it.next();
        if (locale.startsWith("supplem") || locale.startsWith("character"))
          continue;

        // showCollation(collationFactory, locale, collationRules);
        // if (true) continue;

        boolean doResolved = localeIDParser.set(locale).getRegion().length() == 0;
        String languageSubtag = localeIDParser.getLanguage();
        boolean isLanguageLocale = locale.equals(languageSubtag);

        CLDRFile file = (CLDRFile) cldrFactory.make(locale, doResolved);
        if (file.isNonInheriting())
          continue; // for now, skip supplementals
        boolean showParent = !isLanguageLocale;
        if (showParent) {
          parent = (CLDRFile) cldrFactory.make(
              localeIDParser.getParent(locale), true);
        }
        boolean showEnglish = !languageSubtag.equals("en");

        // put into set of simpler paths
        // and filter if necessary
        int skippedCount = 0;
        int aliasedCount = 0;
        int inheritedCount = 0;
        prettySet.clear();
        for (Iterator it2 = file.iterator(); it2.hasNext();) {
          String path = (String) it2.next();
          if (path.indexOf("/alias") >= 0) {
            skippedCount++;
            continue; // skip code fllback
          }
          if (path.indexOf("/usesMetazone") >= 0) {
            skippedCount++;
            continue; // skip code fllback
          }
          if (path.indexOf("/references") >= 0) {
            skippedCount++;
            continue; // skip references
          }
          if (path.indexOf("[@alt=\"proposed") >= 0) {
            skippedCount++;
            continue; // skip code fllback
          }
          if (path.indexOf("/identity") >= 0) {
            skippedCount++;
            continue; // skip code fllback
          }
          String prettyString = prettyPathMaker.getPrettyPath(path);
          prettySet.add(prettyString);
        }

        PrintWriter pw = BagFormatter.openUTF8Writer(targetDir, locale + ".html");

        String[] headerAndFooter = new String[2];

        getChartTemplate(
            "Locale Data Summary for " + getName(locale),
            ShowLanguages.CHART_DISPLAY_VERSION,
            // "<style type='text/css'>" + Utility.LINE_SEPARATOR +
            // "h1 {margin-bottom:1em}" + Utility.LINE_SEPARATOR +
            // ".e {background-color: #EEEEEE}" + Utility.LINE_SEPARATOR +
            // ".i {background-color: #FFFFCC}" + Utility.LINE_SEPARATOR +
            // ".v {background-color: #FFFF00}" + Utility.LINE_SEPARATOR +
            // ".a {background-color: #9999FF}" + Utility.LINE_SEPARATOR +
            // ".ah {background-color: #FF99FF}" + Utility.LINE_SEPARATOR +
            // ".h {background-color: #FF9999}" + Utility.LINE_SEPARATOR +
            // ".n {color: #999999}" + Utility.LINE_SEPARATOR +
            // ".g {background-color: #99FF99}" + Utility.LINE_SEPARATOR +
            // "</style>" + Utility.LINE_SEPARATOR +
            "<script>" + Utility.LINE_SEPARATOR
                + "if (location.href.split('?')[1].split(',')[0]=='hide') {" + Utility.LINE_SEPARATOR
                + "document.write('<style>');" + Utility.LINE_SEPARATOR
                + "document.write('.xx {display:none}');" + Utility.LINE_SEPARATOR
                + "document.write('</style>');" + Utility.LINE_SEPARATOR + "}" + Utility.LINE_SEPARATOR + "</script>",
            headerAndFooter);
        pw.println(headerAndFooter[0]);
        // pw.println("<html><head>");
        // pw.println("<meta http-equiv='Content-Type' content='text/html;
        // charset=utf-8'>");
        // pw.println("<style type='text/css'>");
        // pw.println("<!--");
        // pw.println(".e {background-color: #EEEEEE}");
        // pw.println(".i {background-color: #FFFFCC}");
        // pw.println(".v {background-color: #FFFF00}");
        // pw.println(".a {background-color: #9999FF}");
        // pw.println(".ah {background-color: #FF99FF}");
        // pw.println(".h {background-color: #FF9999}");
        // pw.println(".n {color: #999999}");
        // pw.println(".g {background-color: #99FF99}");
        // pw.println("-->");
        // pw.println("</style>");
        // pw.println("<script>");
        // pw.println("if (location.href.split('?')[1].split(',')[0]=='hide')
        // {");
        // pw.println("document.write('<style>');");
        // pw.println("document.write('.xx {display:none}');");
        // pw.println("document.write('</style>');");
        // pw.println("}");
        // pw.println("</script>");
        // pw.println("<title>" + getName(locale) + "</title>");
        // pw.println("</head><body>");
        // pw.println("<h1>" + getName(locale) + " (" + file.getDtdVersion() +
        // ")</h1>");
        showLinks(pw, locale);
        showChildren(pw, locale);
        if (doResolved) {
          pw.println("<p><b>Aliased/Inherited: </b><a href='" + locale
              + ".html?hide'>Hide</a> <a href='" + locale
              + ".html'>Show </a></p>");
        }
        pw.println("<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">");

        pw.println("<tr><th>No</th><th colSpan=3>Path</th>"
            + (showEnglish ? "<th>English</th>" : "")
            + (showParent ? "<th>Parent</th>" : "") + "<th>Native</th>"
            + "<th>D?</th>" +
            // "<th>Nat. Att.</th>" +
            // (showEnglish ? "<th>Eng. Att.</th>" : "") +
            // (showParent ? "<th>Par. Att.</th>" : "") +
            "<tr>");

        int count = 0;
        String[] oldParts = new String[4];
        for (Iterator it2 = prettySet.iterator(); it2.hasNext();) {
          String prettyPath = (String) it2.next();
          String path = prettyPathMaker.getOriginal(prettyPath);
          boolean zeroOutEnglish = path.indexOf("/references") < 0;

          String source = file.getSourceLocaleID(path, status);
          boolean isAliased = !status.pathWhereFound.equals(path);
          if (isAliased) {
            aliasedCount++;
            continue;
          }
          boolean isInherited = !source.equals(locale);
          if (isInherited) {
            inheritedCount++;
          }

          StringBuffer tempDraftRef = new StringBuffer();
          String value = file.getStringValue(path);
          String fullPath = file.getFullXPath(path);
          String nda = getNda(skipList, nonDistinguishingAttributes, file,
              path, fullPath, tempDraftRef);
          String draftRef = tempDraftRef.toString();
          if (nda.length() != 0) {
            if (value.length() != 0)
              value += "; ";
            value += nda;
          }

          String englishValue = null;
          String englishFullPath = null;
          String englishNda = null;
          if (zeroOutEnglish) {
            englishValue = englishFullPath = englishNda = "";
          }
          if (showEnglish
              && null != (englishValue = english.getStringValue(path))) {
            englishFullPath = english.getFullXPath(path);
            englishNda = getNda(skipList, nonDistinguishingAttributes, file,
                path, englishFullPath, tempDraftRef);
            if (englishNda.length() != 0) {
              if (englishValue.length() != 0)
                englishValue += "; ";
              englishValue += englishNda;
            }
          }

          String parentFullPath = null;
          String parentNda = null;
          String parentValue = null;
          if (showParent
              && (null != (parentValue = parent.getStringValue(path)))) {
            parentFullPath = parent.getFullXPath(path);
            parentNda = getNda(skipList, nonDistinguishingAttributes, parent,
                path, parentFullPath, tempDraftRef);
            if (parentNda.length() != 0) {
              if (parentValue.length() != 0)
                parentValue += "; ";
              parentValue += parentNda;
            }
          }
          prettyPath = TransliteratorUtilities.toHTML
              .transliterate(prettyPathMaker.getOutputForm(prettyPath));
          String[] pathParts = prettyPath.split("[|]");
          // count the <td>'s and pad
          // int countBreaks = Utility.countInstances(prettyPath, "</td><td>");
          // prettyPath += Utility.repeat("</td><td>", 3-countBreaks);
          prettyPath = "";
          for (int i = 0; i < 3; ++i) {
            String newPart = i < pathParts.length ? pathParts[i] : "";
            if (newPart.equals(oldParts[i])) {
              prettyPath += "</td><td class='n'>";
            } else {
              if (newPart.length() == 0) {
                prettyPath += "</td><td>";
              } else {
                prettyPath += "</td><td class='g'>";
              }
              oldParts[i] = newPart;
            }
            prettyPath += newPart;
          }
          String statusClass = isAliased ? (isInherited ? " class='ah'"
              : " class='a'") : (isInherited ? " class='h'" : "");

          pw
              .println((isAliased || isInherited ? "<tr class='xx'><td"
                  : "<tr><td")
                  + statusClass
                  + ">"
                  + (++count)
                  + prettyPath
                  // + "</td><td>" +
                  // TransliteratorUtilities.toHTML.transliterate(lastElement)
                  + showValue(showEnglish, englishValue, value)
                  + showValue(showParent, parentValue, value)
                  + (value == null ? "</td><td>n/a" : "</td><td class='v'"
                      + dataShower.getBidiStyle(value) + ">"
                      + dataShower.getPrettyValue(value))
                  + (draftRef.length() == 0 ? "</td><td>&nbsp;"
                      : "</td><td class='v'>"
                          + dataShower.getPrettyValue(draftRef))
                  // + "</td><td>" + (nda == null ? "&nbsp;" :
                  // TransliteratorUtilities.toHTML.transliterate(nda))
                  // + showValue(showEnglish, englishNda, nda)
                  // + showValue(showParent, parentNda, nda)
                  + "</td></tr>");
        }
        pw.println("</table><br><table>");
        pw.println("<tr><td class='a'>Aliased items: </td><td>" + aliasedCount
            + "</td></tr>");
        pw.println("<tr><td class='h'>Inherited items:</td><td>"
            + inheritedCount + "</td></tr>");
        if (skippedCount != 0)
          pw.println("<tr><td>Omitted items:</td><td>" + skippedCount
              + "</td></tr>");
        pw.println("</table>");

        // pw.println("</body></html>");
        pw.println(headerAndFooter[1]);
        pw.close();
      }
    } finally {
      deltaTime = System.currentTimeMillis() - deltaTime;
      System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
      System.out.println("Done");
    }
  }

  private static void getScripts() throws IOException {
    Set locales = cldrFactory.getAvailableLanguages();
    Set scripts = new TreeSet();
    XPathParts parts = new XPathParts();
    Map script_name_locales = new TreeMap();
    PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY,
        "scriptNames.txt");
    for (Iterator it = locales.iterator(); it.hasNext();) {
      String locale = (String) it.next();
      System.out.println(locale);
      CLDRFile file = cldrFactory.make(locale, false);
      if (file.isNonInheriting())
        continue;
      String localeName = file.getName(locale);
      getScripts(localeName, scripts);
      if (!scripts.contains("Latn")) {
        out
            .println(locale + "\t" + english.getName(locale) + "\t"
                + localeName);
      }
      for (Iterator it2 = UnicodeScripts.iterator(); it2.hasNext();) {
        String script = (String) it2.next();
        if (script.equals("Latn"))
          continue;
        String name = file.getName(CLDRFile.SCRIPT_NAME, script);
        if (getScripts(name, scripts).contains(script)) {
          Map names_locales = (Map) script_name_locales.get(script);
          if (names_locales == null)
            script_name_locales.put(script, names_locales = new TreeMap());
          Set localeSet = (Set) names_locales.get(name);
          if (localeSet == null)
            names_locales.put(name, localeSet = new TreeSet());
          localeSet.add(getName(locale));
        }
      }
    }
    for (Iterator it2 = UnicodeScripts.iterator(); it2.hasNext();) {
      String script = (String) it2.next();
      Object names = script_name_locales.get(script);
      out.println(script + "\t("
          + english.getName(CLDRFile.SCRIPT_NAME, script) + ")\t" + names);
    }
    out.close();
  }

  static Set UnicodeScripts = Collections.unmodifiableSet(new TreeSet(Arrays
      .asList(new String[] { "Arab", "Armn", "Bali", "Beng", "Bopo", "Brai",
          "Bugi", "Buhd", "Cans", "Cher", "Copt", "Cprt", "Cyrl", "Deva",
          "Dsrt", "Ethi", "Geor", "Glag", "Goth", "Grek", "Gujr", "Guru",
          "Hang", "Hani", "Hano", "Hebr", "Hira", "Hrkt", "Ital", "Kana",
          "Khar", "Khmr", "Knda", "Laoo", "Latn", "Limb", "Linb", "Mlym",
          "Mong", "Mymr", "Nkoo", "Ogam", "Orya", "Osma", "Phag", "Phnx",
          "Qaai", "Runr", "Shaw", "Sinh", "Sylo", "Syrc", "Tagb", "Tale",
          "Talu", "Taml", "Telu", "Tfng", "Tglg", "Thaa", "Thai", "Tibt",
          "Ugar", "Xpeo", "Xsux", "Yiii" })));

  private static Set getScripts(String exemplars, Set results) {
    results.clear();
    if (exemplars == null)
      return results;
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet()
        .addAll(exemplars)); it.next();) {
      int cp = it.codepoint;
      int script = UScript.getScript(cp);
      results.add(UScript.getShortName(script));
    }
    return results;
  }

  private static void showCollation(Factory collationFactory, String locale,
      ExtractCollationRules collationRules) {
    CLDRFile collationFile;
    try {
      collationFile = collationFactory.make(locale, false);
    } catch (RuntimeException e) {
      return; // skip
    }
    collationRules.set(collationFile);
    for (Iterator it = collationRules.iterator(); it.hasNext();) {
      String key = (String) it.next();
      System.out.println(key + ": ");
      String rules = collationRules.getRules(key);
      System.out.println(rules);
    }
  }

  private static String showValue(boolean showEnglish, String comparisonValue,
      String mainValue) {
    return !showEnglish ? "" : comparisonValue == null ? "</td><td>n/a"
        : comparisonValue.length() == 0 ? "</td><td>&nbsp;" : comparisonValue
            .equals(mainValue) ? "</td><td>=" : "</td><td class='e'"
            + dataShower.getBidiStyle(comparisonValue) + ">"
            + dataShower.getPrettyValue(comparisonValue);
  }

  static DataShower dataShower = new DataShower();

  public static class DataShower {
    static Transliterator toLatin = Transliterator.getInstance("any-latin");

    static UnicodeSet BIDI_R = new UnicodeSet(
        "[[:Bidi_Class=R:][:Bidi_Class=AL:]]");

    static String getBidiStyle(String cellValue) {
      return BIDI_R.containsSome(cellValue) ? " style='direction:rtl'" : "";
    }

    public static String getPrettyValue(String textToInsert) {
      String outValue = TransliteratorUtilities.toHTML
          .transliterate(textToInsert);
      String transValue = textToInsert;
      String span = "";
      try {
        transValue = toLatin.transliterate(textToInsert);
      } catch (RuntimeException e) {
      }
      if (!transValue.equals(textToInsert)) {
        // WARNING: we use toXML in attributes
        outValue = "<span title='"
            + TransliteratorUtilities.toXML.transliterate(transValue) + "'>"
            + outValue + "</span>";
      }
      return outValue;
    }
  }

  private static String getNda(Set skipList, Map nonDistinguishingAttributes,
      CLDRFile file, String path, String parentFullPath, StringBuffer draftRef) {
    draftRef.setLength(0);
    if (parentFullPath != null && !parentFullPath.equals(path)) {
      file.getNonDistinguishingAttributes(parentFullPath,
          nonDistinguishingAttributes, skipList);
      if (nonDistinguishingAttributes.size() != 0) {
        String parentNda = "";
        for (Iterator it = nonDistinguishingAttributes.keySet().iterator(); it
            .hasNext();) {
          String key = (String) it.next();
          String value = (String) nonDistinguishingAttributes.get(key);
          if (key.equals("draft") && !value.equals("contributed")) {
            if (draftRef.length() != 0)
              draftRef.append(",");
            draftRef.append("d");
          } else if (key.equals("alt")) {
            if (draftRef.length() != 0)
              draftRef.append(",");
            draftRef.append("a");
          } else if (key.equals("references")) {
            if (draftRef.length() != 0)
              draftRef.append(",");
            draftRef.append(nonDistinguishingAttributes.get(key));
          } else {
            if (parentNda.length() != 0)
              parentNda += ", ";
            parentNda += key + "=" + nonDistinguishingAttributes.get(key);
          }
        }
        if (parentNda.length() != 0) {
          parentNda = parentNda.replaceAll("[/]", "/\u200B");
          parentNda = "[" + parentNda + "]";
        }
        return parentNda;
      }
    }
    return "";
  }

  private static void showLinks(PrintWriter pw, String locale) {
    pw.print("<p>");
    showLinks2(pw, locale);
    pw.println("</p>");
  }

  private static void showLinks2(PrintWriter pw, String locale) {
    String parent = LocaleIDParser.getParent(locale);
    if (parent != null) {
      showLinks2(pw, parent);
      pw.print(" &gt; ");
    }
    showLocale(pw, locale);
  }

  private static void showChildren(PrintWriter pw, String locale) {
    boolean first = true;
    for (Iterator it = cldrFactory.getAvailableWithParent(locale, true)
        .iterator(); it.hasNext();) {
      String possible = (String) it.next();
      if (possible.startsWith("supplem") || possible.startsWith("character"))
        continue;
      if (LocaleIDParser.getParent(possible).equals(locale)) {
        if (first) {
          first = false;
          pw.println("<p style='margin-left:5em'>&gt; ");
        } else {
          pw.print(" | ");
        }
        showLocale(pw, possible);
      }
    }
    if (first == false) {
      pw.println("</p>");
    }
  }

  private static void showLocale(PrintWriter pw, String locale) {
    pw.println("<a href='" + locale + ".html'>" + getName(locale) + "</a>");
  }

  private static String getName(String locale) {
    String name = english.getName(locale);
    return locale + " [" + name + "]";
  }

  static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm",
      ULocale.ENGLISH);

  static String VERSION = "1.5";
  
  static public void getChartTemplate(String title, String version,
      String header, String[] headerAndFooter) throws IOException {
    if (version == null) {
      version = VERSION;
    }
    VariableReplacer langTag = new VariableReplacer()
    .add("%title%", title)
    .add("%header%", header)
    .add("%version%", version)
    .add("%date%", isoDateFormat.format(new Date()));
    BufferedReader input = Utility
        .getUTF8Data("../../tool/chart-template.html");
    StringBuffer result = new StringBuffer();
    while (true) {
      String line = input.readLine();
      if (line == null)
        break;
      String langTagPattern = langTag.replace(line);
      if (line.indexOf("%body%") >= 0) {
        headerAndFooter[0] = result.toString();
        result.setLength(0);
        continue;
      }
      result.append(langTagPattern).append(Utility.LINE_SEPARATOR);
    }
    headerAndFooter[1] = result.toString();
  }
}