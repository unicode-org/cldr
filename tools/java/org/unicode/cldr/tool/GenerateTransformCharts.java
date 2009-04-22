/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.tool;

import org.unicode.cldr.icu.CollectionUtilities;
import org.unicode.cldr.test.TestTransformsSimple;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.SimpleEquivalenceClass;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.RuleBasedTransliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
//import com.ibm.icu.dev.test.translit.CompoundTransliteratorTest;
import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
//import com.ibm.icu.impl.Utility;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

public class GenerateTransformCharts {

  private static final String TRANSFORM_DIRECTORY = Utility.CHART_DIRECTORY + File.separatorChar + "transforms/";
  private static final UnicodeSet NON_LATIN = new UnicodeSet("[^[:latin:][:common:][:inherited:]]");
  private static final boolean verbose = Utility.getProperty("verbose", false);

  static int[] indicScripts = { UScript.LATIN, UScript.COMMON, UScript.DEVANAGARI,
    UScript.BENGALI, UScript.GURMUKHI, UScript.GUJARATI,
    UScript.ORIYA, UScript.TAMIL, UScript.TELUGU, UScript.KANNADA,
    UScript.MALAYALAM, };
  static String[] names = new String[indicScripts.length];
  static String[] shortnames = new String[indicScripts.length];
  static UnicodeSet[] sets = new UnicodeSet[indicScripts.length];
  static Transliterator[] fallbacks = new Transliterator[indicScripts.length];
  static         UnicodeSet lengthMarks = new UnicodeSet(
  "[\u09D7\u0B56-\u0B57\u0BD7\u0C56\u0CD5-\u0CD6\u0D57\u0C55\u0CD5]");

  //static Set<String> Indic = new TreeSet(Arrays.asList(indicScripts));
  //static {
  //Indic.remove(UScript.LATIN);
  //Indic.remove(UScript.COMMON);
  //}

  static         String testString = "\u0946\u093E";
  private static boolean useICU;

  // Latin    Arabic  Bengali     Cyrillic    Devanagari  Greek   Greek/UNGEGN    Gujarati    Gurmukhi    Hangul  Hebrew  Hiragana    Kannada     Katakana    Malayalam   Oriya   Tamil   Telugu  Thai

  static Map<String,UnicodeSet> scriptExtras = new HashMap<String,UnicodeSet>();
  static {
    scriptExtras.put("Arab", new UnicodeSet("[\u0660-\u0669]"));
    scriptExtras.put("Hang", TestTransformsSimple.getRepresentativeHangul());
  }

  public static void main(String[] args) throws IOException {
    useICU = Utility.getProperty("USEICU", false);
    String filter = Utility.getProperty("filter", null);
    System.out.println("Start");
    //PrintWriter out = new PrintWriter(System.out);
    CLDRTransforms.registerCldrTransforms(null, filter, verbose ? new PrintWriter(System.out) : null);
    try {
      showAllLatin();
      //doIndic();
    } finally {
      System.out.println("Done");
      //out.close();
    }

  }

  //private static void doIndic() throws IOException {
  //populateScriptInfo();
  //
  //SimpleEquivalenceClass equivalenceClass = new SimpleEquivalenceClass(new UTF16.StringComparator(true,false,0)); // new ReverseComparator());
  //Transliterator anyToLatin = Transliterator.getInstance("any-latin");
  //
  //UnicodeSet failNorm = new UnicodeSet();
  //// Collator sc = Collator.getInstance(ULocale.ENGLISH);
  //// sc.setStrength(Collator.IDENTICAL);
  //Comparator sc = new UTF16.StringComparator(true, false, 0);
  //Set latinFail = new TreeSet(new ArrayComparator(new Comparator[] { sc, sc, sc }));
  //
  //getEquivalentCharacters(equivalenceClass, latinFail);
  //
  //printChart(equivalenceClass, anyToLatin, failNorm, latinFail);
  //}

  //private static void printChart(SimpleEquivalenceClass equivalenceClass, Transliterator anyToLatin, UnicodeSet failNorm, Set latinFail) throws IOException {
  //// collect equivalents
  //PrintWriter pw = BagFormatter.openUTF8Writer(TRANSFORM_DIRECTORY, "Indic.html");
  //pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
  //pw.println("<title>Indic Transliteration Chart</title><style>");
  //pw.println("td { text-align: Center; font-size: 200% }");
  //pw.println("tt { font-size: 50% }");
  //pw.println("td.miss { background-color: #CCCCFF }");
  //pw.println("td.none { background-color: #FFCCCC }");
  //pw.println("</style></head><body bgcolor='#FFFFFF'>");
  //
  //pw.println("<table border='1' cellspacing='0'><tr>");
  //for (int i = 0; i < indicScripts.length; ++i) {
  //pw.print("<th width='10%'>" + shortnames[i] + "</th>");
  //}
  //pw.println("</tr>");
  //
  //Comparator mySetComparator = new CollectionOfComparablesComparator();
  //Iterator rit = equivalenceClass.getSetIterator(mySetComparator);
  //Set last = null;
  //while (rit.hasNext()) {
  //Set equivs = (Set) rit.next();
  //mySetComparator.compare(last, equivs);
  //last = equivs;
  //pw.print("<tr>");
  //// transliterate the first item into latin
  //Iterator sit = equivs.iterator();
  //String source = (String) sit.next();
  //String item = anyToLatin.transliterate(source);
  //if (source.equals(item)) {
  //item = ""; // failure
  //}
  //if (item.length() == 0) {
  //continue; //    -- we were printing these rows, but it's better presentation just to show the failures later
  //}
  //// pw.print("<td>" + item + "</td>");
  //showCell(pw, item, "");
  //// now show the other cells.
  //for (int i = 1; i < indicScripts.length; ++i) {
  //item = findItemInScript(equivs, sets[i]);
  //String classString = "";
  //if (item.equals("")) {
  //classString = " class='miss'";
  //String temp = fallbacks[i].transliterate(source);
  //if (!temp.equals("") && !temp.equals(source))
  //item = temp;
  //}
  //showCell(pw, item, classString);
  //}
  ///*
  //* Iterator sit = equivs.iterator(); while (sit.hasNext()) { String
  //* item = (String)sit.next(); pw.print("<td>" + item + "</td>"); }
  //*/
  //pw.println("</tr>");
  //}
  //pw.println("</table>");
  //if (false) {
  //pw.println("<h2>Failed Normalization</h2>");
  //
  //UnicodeSetIterator it = new UnicodeSetIterator(failNorm);
  //UnicodeSet pieces = new UnicodeSet();
  //while (it.next()) {
  //String s = UTF16.valueOf(it.codepoint);
  //String d = Normalizer.normalize(s, Normalizer.NFD, 0);
  //pw.println("Norm:" + s + ", " + com.ibm.icu.impl.Utility.hex(s)
  //+ " " + UCharacter.getName(it.codepoint) + "; " + d
  //+ ", " + com.ibm.icu.impl.Utility.hex(d) + ", ");
  //pw.println(UCharacter.getName(d.charAt(1)) + "<br>");
  //if (UCharacter.getName(d.charAt(1)).indexOf("LENGTH") >= 0)
  //pieces.add(d.charAt(1));
  //}
  //pw.println(pieces);
  //}
  //if (true) {
  //pw.println("<h2>Failed Indic-Latin-Indic Round-Trip</h2>");
  //pw.println("<table border='1' cellspacing='0'>");
  //pw
  //.println("<tr><th width='33%'>Indic</th><th width='33%'>Latin</th<th width='33%'>Indic</th></tr>");
  //
  //Iterator cit = latinFail.iterator();
  //while (cit.hasNext()) {
  //String[] badItems = (String[]) cit.next();
  //if (badItems[0].charAt(0) > 0xE000) {
  //continue;
  //}
  //pw.println("<tr>");
  ////pw.println("<th>" + badItems[0] + "</th>");
  //for (int i = 0; i < badItems.length; ++i) {
  //showCell(pw, badItems[i], " class='miss'");
  //}
  //pw.println("</tr>");
  //}
  //pw.println("</table>");
  //}
  //
  //pw.println("</table></body></html>");
  //pw.close();
  //}
  //
  //private static void getEquivalentCharacters(SimpleEquivalenceClass equivalenceClass, Set latinFail) {
  //UnicodeSet failures = new UnicodeSet();
  //for (int i = 0; i < indicScripts.length; ++i) {
  //if (indicScripts[i] == UScript.LATIN)
  //continue;
  //String source = names[i];
  //System.out.println(source);
  //UnicodeSet sourceChars = sets[i];
  //
  //for (int j = 0; j < indicScripts.length; ++j) {
  //if (i == j) {
  //continue;
  //}
  //String target = names[j];
  //Transliterator forward = getTransliterator(source, target, null);
  //Transliterator backward = getTransliterator(target, source, null);
  //UnicodeSetIterator it = new UnicodeSetIterator(sourceChars);
  //while (it.next()) {
  //if (lengthMarks.contains(it.codepoint))
  //continue;
  //String s = it.getString();
  //s = Normalizer.normalize(s, Normalizer.NFC, 0);
  //// if (!Normalizer.isNormalized(s,Normalizer.NFC,0))
  //// continue;
  ////if (!s.equals(Normalizer.normalize(s, Normalizer.NFD, 0))) {
  ////failNorm.add(it.codepoint);
  ////}
  //String t = forward.transliterate(s); // fix(forward.transliterate(s));
  //if (t.equals(testString)) {
  //System.out.println("debug");
  //}
  //
  //String r = backward.transliterate(t); // fix(backward.transliterate(t));
  //if (Normalizer.compare(s, r, 0) == 0 && Normalizer.compare(s,t,0) != 0) {
  //if (indicScripts[j] != UScript.LATIN)
  ////if (s.compareTo("9") <= 0 || t.compareTo("9")<= 0) {
  ////System.out.println(s + "\t" + t);
  ////}
  //equivalenceClass.add(s, t);
  //} else {
  //failures.add(it.codepoint);
  //if (indicScripts[j] == UScript.LATIN) {
  ////String age = UCharacter.getStringPropertyValue(UProperty.AGE, it.codepoint, 0);
  //if (!s.equals(t) && !s.equals(r)) {
  //latinFail.add(new String[] { s, t, r });
  //} else {
  //latinFail.add(new String[] { s, "", "" });
  //}
  //}
  //}
  //}
  //}
  //}
  //for (UnicodeSetIterator it = new UnicodeSetIterator(failures); it.next();) {
  //String v = it.getString();
  //equivalenceClass.add(v, v); // add singleton
  //}
  //}
  //
  //private static void populateScriptInfo() {
  //for (int i = 0; i < indicScripts.length; ++i) {
  //if (indicScripts[i] == UScript.COMMON) {
  //names[i] = "InterIndic";
  //sets[i] = new UnicodeSet(); //  - [\uE000 \uE066 \uE051-\uE054 \uE064 \uE065 \uE070 \uE073 \uE075 \uE03A]
  //fallbacks[i] = Transliterator.getInstance("null");
  //continue;
  //}
  //names[i] = UScript.getName(indicScripts[i]);
  //shortnames[i] = UScript.getShortName(indicScripts[i]);
  //sets[i] = new UnicodeSet("[[:" + names[i] + ":]&[[:L:][:M:][:Nd:]]]"); // 
  //fallbacks[i] = Transliterator.getInstance("any-" + names[i]);
  //}
  //// populate the interindic set
  //// add everything that maps FROM private use
  //for (int i = 2; i < indicScripts.length; ++i) {
  //Transliterator forward = getTransliterator(names[i], "InterIndic", null);
  //for (UnicodeSetIterator it = new UnicodeSetIterator(sets[i]); it.next();) {
  //String resultFromInterindic = it.getString();
  //if (lengthMarks.containsAll(resultFromInterindic)) continue;
  //sets[1].addAll(forward.transliterate(resultFromInterindic));
  //}
  //}
  //sets[UScript.COMMON].retainAll(new UnicodeSet("[:Co:]"));
  //System.out.println("InterIndic: " + sets[1]);
  //}

  private static String findItemInScript(Set equivs, UnicodeSet scriptSet) {
    Iterator sit;
    String item;
    sit = equivs.iterator();
    item = "";
    // find the item that is all in script i
    while (sit.hasNext()) {
      String trial = (String) sit.next();
      if (!scriptSet.containsAll(trial))
        continue;
      item = trial;
      break;
    }
    return item;
  }

  private static Transliterator getTransliterator(String source, String target, String variant) {
    String id = ParsedTransformID.getId(source, target, variant);
    if (id.indexOf("InterIndic") >= 0) {
      // we depend on the transliterators being registered anyway
      id = "NFD; " + id + "; NFC";
      return Transliterator.getInstance(id);
    }
    return Transliterator.getInstance(id);
  }

  static PrintWriter index;

  private static void showAllLatin() throws IOException {
    String[] headerAndFooter = new String[2];
    ShowData.getChartTemplate("Transliteration Charts",
            null, "",
            headerAndFooter);

    index = BagFormatter.openUTF8Writer(TRANSFORM_DIRECTORY, "index.html");
    index.println(headerAndFooter[0]);
    //  index.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
    //  index.println("<title>Transliteration Charts</title><style>");
    //  index.println("</style></head><body bgcolor='#FFFFFF'>");
    //  index.println("<h1>Transliteration Charts</h1>");
    index.println("<p>The following illustrates some of the transliterations available in CLDR. " +
    "<b>Note:</b> these charts are preliminary; for more information, see below.</p.>");
    index.flush();
    index.println("<ul>");
    Set nonScripts = new TreeSet(Arrays.asList("ConjoiningJamo", "InterIndic", "Han", 
            "el", "Jamo", "JapaneseKana", "Korean", "NumericPinyin", "ThaiLogical", "ThaiSemi"));
    try {
      SimpleEquivalenceClass ec = new SimpleEquivalenceClass(UCA);
      Set<String> availableTransliterators = getAvailableTransliterators();
      Set scripts = new TreeSet();
      for (String id : availableTransliterators) {
        index.flush();
        CLDRTransforms.ParsedTransformID parsedID = new CLDRTransforms.ParsedTransformID().set(id);
        if (!hasScript(parsedID.source) || !hasScript(parsedID.target)) continue;
        if (nonScripts.contains(parsedID.source) || nonScripts.contains(parsedID.target)) {
          continue;
        }

        //      if (parsedID.source.equals("Hangul") || parsedID.target.equals("Hangul")) {} else {
        //      continue;
        //      }

        if (false && parsedID.variant != null) {
          if (!parsedID.target.equals("Latin")) ec.add(parsedID.target, parsedID.getTargetVariant());
          if (!parsedID.source.equals("Latin")) ec.add(parsedID.source, parsedID.getSourceVariant());
        }

        if (!parsedID.target.equals("Latin")) {
          if (!parsedID.source.equals("Latin")) {
            ec.add(parsedID.source, parsedID.getTargetVariant());
            ec.add(parsedID.target, parsedID.getSourceVariant());
          }
          continue;
        }
        scripts.add(parsedID.getTargetVariant());
        ec.add(parsedID.getSourceVariant(), UScript.getName(getScriptFromScriptOrLanguage(parsedID.source)));
        ec.add(parsedID.getTargetVariant(), UScript.getName(getScriptFromScriptOrLanguage(parsedID.target)));
      }

      Set alreadySeen = new HashSet();
      for (Iterator it = ec.getSetIterator(null); it.hasNext();) {
        Set scriptSet = new TreeSet(UCA);
        scriptSet.addAll((Set) it.next());
        scriptSet.removeAll(alreadySeen);
        if (scriptSet.size() <= 0) continue;
        showLatin(getName(scriptSet), scriptSet);
        alreadySeen.addAll(scriptSet);
      }
    } finally {
      index.println("</ul>");
      index.println("<h2>Key</h2><ul>");
      index.println("<li>A cell with a blue background indicates a case that doesn't roundtrip.</li>");
      index.println("<li>A cell with a red background indicates a missing case.</li>");
      index.println("<li>Hovering over each cell should show the character name, if enabled on your browser.</li>");
      index.println("</ul>");
      index.println("<h2>Known Data Issues</h2><ul>");
      index.println("<li>The CLDR data currently does not contain many language-specific transliterations. " +
      "So, for example, the Cyrillic transliteration is not very natural for English speakers.</li>");
      index.println("<li>The unmarked script transliterations to Latin are generally designed to be reversible, thus some of the transliterations use extra accents to provide for a round-trip. " +
      "(Implementations like ICU allows those to be easily stripped.)</li>");
      index.println("<li>Less common characters may be missing; as may be some characters that don't appear in isolation.</li>");
      index.println("<li>Some transliterations only work in context, which won't be visible. For example, an isolated 'a' transliterates to </li>");
      index.println("<li>There are known bugs in some of the charts, such as Hangul.</li>");
      index.println("</ul>");

      index.println("<h2>Known Chart Issues</h2><ul>");
      index.println("<li>Some browsers will not show combinations of accents correctly. See <a href='http://www.unicode.org/help/display_problems.html'>Display Problems?</a></li>");
      index.println("<li>Because the context is not taken into account, significant combinations will not show in the charts. " +
              "For example: For greek, \u03A8 shows as 'PH', when the transliteration rules will change it to 'Ph' in front of lowercase letters. " +
      "Characters that are not normally used in isolation, such as \u3041, will show as an odd format (eg with extra punctuations marks or accents).</li>");
      index.println("<li>Only the script-script charts are shown.</li>");
      index.println("<li>The display in some of the charts needs to be improved, such as Greek, Indic, and Kana. In particular, the nonLatin-Latin display needs to be merged.</li>");
      index.println("</ul>");
      index.println(headerAndFooter[1]);
      //index.println("</body></html>");
      index.close();
    }
    //showLatin("Indic", indicSet);

  }

  private static String getName(Set<String> scriptSet) {
    if (scriptSet.size() == 1) return scriptSet.iterator().next().toString();
    if (scriptSet.contains("Bengali")) return "Indic";
    if (scriptSet.contains("Katakana")) return "Kana";
    //if (scriptSet.contains("Greek")) return "Greek";
    return UScript.getName(getScriptFromScriptOrLanguage(scriptSet.iterator().next()));
  }

  static UnicodeSet stuffToSkip = (UnicodeSet) new UnicodeSet("[[:block=Hangul Syllables:][:NFKC_QuickCheck=No:][\\U00010000-\\U0010FFFF]]").freeze();
  static Comparator UCA;
  static {
    RuleBasedCollator UCA2 = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
    UCA2.setNumericCollation(true);
    UCA2.setStrength(UCA2.IDENTICAL);
    UCA = new CollectionUtilities.MultiComparator(UCA2, new UTF16.StringComparator(true, false, 0) );
  }

  private static void showLatin(String scriptChoice, Set<String> targetVariant) throws IOException {
    if (scriptChoice.equals("Latin")) {
      return;
    }
    CLDRTransforms.ParsedTransformID parsedID = new CLDRTransforms.ParsedTransformID();
    Set ids = new TreeSet();
    Map id_unmapped = new HashMap();
    Map id_noRoundTrip = new HashMap();
    Set latinItems = new TreeSet(UCA);

    Map nonLatinToLatin = new TreeMap(UCA);
    Set totalLatin = new TreeSet();
    Map latinToTaggedNonLatin = new TreeMap(UCA);
    Map nonLatinToLatinTagged = new TreeMap(UCA);

    UnicodeSet totalNonLatinSet = new UnicodeSet();

    // gather info
    for (Iterator<String> i = targetVariant.iterator(); i.hasNext();) {
      String nonLatinId = i.next();
      String id = "Latin-" + nonLatinId;  // put the variant in the right place
      parsedID.set(id);
      id = parsedID.reverse().toString();
      //    String id = (String)i.next();
      //    parsedID.set(id);
      //    if (!parsedID.target.equals("Latin")) continue;
      //    if (parsedID.source.equals("Han")) continue;
      String script = parsedID.source;
      int scriptCode = getScriptFromScriptOrLanguage(script);
      if (scriptCode < 0) {
        throw new IllegalArgumentException("Missing script: " + script);
      }
      //    if (scriptCode < 0) {
      //    System.out.println("Skipping id: " + script);
      //    continue;
      //    }
      UnicodeSet nonLatinUnmapped = new UnicodeSet();
      id_unmapped.put(id, nonLatinUnmapped);
      Map noRoundTrip = new TreeMap(UCA);
      id_noRoundTrip.put(id, noRoundTrip);
      ids.add(parsedID.toString());
      UnicodeSet nonLatinUnicodeSet = getNonLatinSet(script, scriptCode);
      totalNonLatinSet.addAll(nonLatinUnicodeSet);

      Transliterator nonLatin_latin;
      Transliterator latin_nonLatin;
      try {
        nonLatin_latin = getTransliterator(parsedID.source, parsedID.target, parsedID.variant);
      } catch (RuntimeException e) {
        continue; // couldn't go from nonLatin to latin
      }
      try {
        latin_nonLatin = getTransliterator(parsedID.target, parsedID.source, parsedID.variant);
      } catch (RuntimeException e) {
        latin_nonLatin = Transliterator.getInstance("null");
      }

      boolean sourceIsCased = false;
      for (UnicodeSetIterator it = new UnicodeSetIterator(nonLatinUnicodeSet); it.next();) {
        String nonLatin = it.getString();
        if (nonLatin.equals("느")) {
          System.out.println("??");
          //Transliterator.DEBUG = true;
        }
        String latin = nonLatin_latin.transliterate(nonLatin);
        //Transliterator.DEBUG = false;

        if (areSame(nonLatin, latin) || NON_LATIN.containsSome(latin)) {
          nonLatinUnmapped.add(nonLatin);
          continue;
        }
        if (latin.length() != 0) {
          latinItems.add(latin);
        }
        String backToNonLatin = latin_nonLatin.transliterate(latin);
        boolean convertsLatin = !areSame(backToNonLatin,latin);

        boolean nonLatinToLatinReversible = convertsLatin && areSame(backToNonLatin, nonLatin);

        if (!nonLatinToLatinReversible) {
          noRoundTrip.put(nonLatin, latin);
          //continue;
        } else { // does round trip, check target
          if (!sourceIsCased && !UCharacter.foldCase(nonLatin,true).equals(nonLatin)) {
            sourceIsCased = true;
          }
        }
        if (!totalLatin.contains(latin) && latin.length() > 0) {
          nonLatinToLatin.put(nonLatin, latin);
          totalLatin.add(latin);
        }

        Utility.addTreeMapChain(nonLatinToLatinTagged, nonLatin, nonLatinId, latin, areSame(nonLatin, backToNonLatin));
        if (convertsLatin) {
          String backToLatin = nonLatin_latin.transliterate(backToNonLatin);
          Utility.addTreeMapChain(latinToTaggedNonLatin, latin, nonLatinId, backToNonLatin, areSame(latin, backToLatin));
        }
      }
      for (int c = 'a'; c <= 'z'; ++c) {
        String latin = UTF16.valueOf(c);
        addToLatinMap(latinItems, latinToTaggedNonLatin, nonLatinToLatinTagged, nonLatinId, nonLatin_latin, latin_nonLatin, latin);
      }
      if (sourceIsCased) for (int c = 'A'; c <= 'Z'; ++c) {
        String latin = UTF16.valueOf(c);
        addToLatinMap(latinItems, latinToTaggedNonLatin, nonLatinToLatinTagged, nonLatinId, nonLatin_latin, latin_nonLatin, latin);
      }
    }

    String filename = "Latin-" + scriptChoice + ".html";
    filename = filename.replace('/', '-');
    index.println("<li><a href='" + filename + "'>" + scriptChoice + "</a></li>");
    PrintWriter pw = BagFormatter.openUTF8Writer(TRANSFORM_DIRECTORY, filename);
    String[] headerAndFooter = new String[2];
    ShowData.getChartTemplate("Latin-" + scriptChoice + " Transliteration Chart",
            null, "",
            //      "<style>" + Utility.LINE_SEPARATOR +
            //      "td, th { background-color: white }" + Utility.LINE_SEPARATOR +
            //      "td { text-align: Center; font-size: 200%}" + Utility.LINE_SEPARATOR +
            //      "tt { font-size: 50% }" + Utility.LINE_SEPARATOR +
            //      "td.miss { background-color: #CCCCFF }" + Utility.LINE_SEPARATOR +
            //      "td.none { background-color: #FFCCCC }" + Utility.LINE_SEPARATOR +
            //      "td.main { align: center; vertical-align: top; background-color: #DDDDDD }" + Utility.LINE_SEPARATOR +
            //      "</style>",
            headerAndFooter);
    pw.println(headerAndFooter[0]);

    //  pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
    //  pw.println("<title>Latin-" + scriptChoice + " Transliteration Chart</title>");
    //  pw.println("<style>");
    //  pw.println("td, th { background-color: white }");
    //  pw.println("td { text-align: Center; font-size: 200%}");
    //  pw.println("tt { font-size: 50% }");
    //  pw.println("td.miss { background-color: #CCCCFF }");
    //  pw.println("td.none { background-color: #FFCCCC }");
    //  pw.println("td.main { align: center; vertical-align: top; background-color: #DDDDDD }");
    //  pw.println("</style></head><body bgcolor='#FFFFFF'>");
    //  pw.println("<h1>Latin-" + scriptChoice + " Transliteration Chart</h1>");
    pw.println("<p1>This chart illustrates one or more of the transliterations in CLDR. It is not complete; in particular, it does not show examples where transliterations differ according to context.</p>" +
    "<p><b>Note:</b> This chart is preliminary; for known issues and more information (such as how to see character names), see the <a href='index.html'>index</a>.</p>");

    pw.println("<div align='center'>");
    pw.println("<table border='1' cellspacing='0' cellpadding='8'>");
    pw.println("<tr>");
    pw.println("<th>" + scriptChoice + "-Latin</th>");

    if (!latinToTaggedNonLatin.isEmpty()) {
      pw.println("<th>Latin-" + scriptChoice + "</th>");
    }
    //  pw.println("<th>Unmapped</th>");
    //  pw.println("<th>No Round Trip</th>");
    pw.println("</tr><tr><td class='main'>");

    doMainTable(parsedID, targetVariant, pw, nonLatinToLatinTagged, true, scriptChoice, totalNonLatinSet);

    if (!latinToTaggedNonLatin.isEmpty()) {
      pw.println("</td><td class='main'>");

      doMainTable(parsedID, targetVariant, pw, latinToTaggedNonLatin, false, scriptChoice, new UnicodeSet());
    }

    //  pw.println("</td><td class='main'>");
    //  
    //  pw.println("<table border='1' cellspacing='0'>");
    //  for (Iterator it2 = ids.iterator(); it2.hasNext();) {
    //  String id = (String)it2.next();
    //  UnicodeSet unmapped = (UnicodeSet) id_unmapped.get(id);
    //  if (unmapped.size() == 0) continue;
    //  parsedID.set(id);
    //  pw.println("<tr><th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th></tr>");
    //  for (UnicodeSetIterator it = new UnicodeSetIterator(unmapped); it.next();) {
    //  pw.println("<tr>");       
    //  String source = it.getString();
    //  showCell(pw, source, " class='miss'");
    //  pw.println("</tr>");
    //  }           
    //  
    //  }
    //  pw.println("</table>");
    //  
    //  pw.println("</td><td class='main'>");
    //  
    //  pw.println("<table border='1' cellspacing='0'>");
    //  for (Iterator it2 = ids.iterator(); it2.hasNext();) {
    //  String id = (String)it2.next();
    //  Map noRoundTrip = (Map) id_noRoundTrip.get(id);
    //  if (noRoundTrip.size() == 0) continue;
    //  parsedID.set(id);
    //  pw.println("<tr><th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th><th>Latin</th></tr>");
    //  for (Iterator it = noRoundTrip.keySet().iterator(); it.hasNext();) {
    //  pw.println("<tr>");       
    //  String source = (String) it.next();
    //  String target = (String)noRoundTrip.get(source);
    //  showCell(pw, source, "");
    //  showCell(pw, target, " class='miss'");
    //  pw.println("</tr>");
    //  }           
    //  }
    //  pw.println("</table>");

    pw.println("</td></tr></table></div>");

    //pw.println("</body></html>");
    pw.println(headerAndFooter[1]);
    pw.close();
  }

  private static boolean areSame(String a, String b) {
    return Normalizer.compare(a,b,0) == 0;
  }

  private static void addToLatinMap(Set latinItems, Map latinToTaggedNonLatin, Map nonLatinToLatinTagged, String nonLatinId, Transliterator nonLatin_latin, Transliterator latin_nonLatin, String latin) {
    latinItems.add(latin);
    String nonLatin = latin_nonLatin.transliterate(latin);
    if (!nonLatin.equals(latin)) {
      String backLatin = nonLatin_latin.transliterate(nonLatin);
      Utility.addTreeMapChain(latinToTaggedNonLatin, latin, nonLatinId, nonLatin, areSame(latin, backLatin));
      String backNonLatin = latin_nonLatin.transliterate(backLatin);
      Utility.addTreeMapChain(nonLatinToLatinTagged, nonLatin, nonLatinId, backLatin, areSame(nonLatin, backNonLatin));
    }
  }

  private static UnicodeSet getNonLatinSet(String script, int scriptCode) {
    String scriptName = UScript.getShortName(scriptCode);
    UnicodeSet nonLatinUnicodeSet = new UnicodeSet("[:script=" + scriptName + ":]");

    // skip some characters
    nonLatinUnicodeSet.removeAll(stuffToSkip);

    UnicodeSet extras;
    // add NFD of characters
    extras = new UnicodeSet();
    for (UnicodeSetIterator usi = new UnicodeSetIterator(nonLatinUnicodeSet); usi.next();) {
      String d = Normalizer.normalize(usi.getString(), Normalizer.NFD, 0);
      extras.addAll(d);
    }
    extras.removeAll(nonLatinUnicodeSet);
    if (extras.size() != 0) {
      System.out.println(script + "\tAdding2: " + extras);
      nonLatinUnicodeSet.addAll(extras);
    }

    // add extras
    extras = (UnicodeSet) scriptExtras.get(scriptName);
    if (extras != null) {
      System.out.println(script + "\tAdding1: " + extras + "\n" + extras.toPattern(false));
      nonLatinUnicodeSet.addAll(extras);
    }

    return nonLatinUnicodeSet;
  }

  static Map<String,String> language_to_script = Utility.asMap(new String[][]{
          {"Jamo", "Hangul"},
          {"Amharic", "Ethiopic"},
          {"Azerbaijani", "Cyrillic"},
          {"Belarusian", "Cyrillic"},
          {"Bulgarian", "Cyrillic"},
          {"Kazakh", "Cyrillic"},
          {"Kirghiz", "Cyrillic"},
          {"Macedonian", "Cyrillic"},
          {"Maldivian", "Thaana"},
          {"Mongolian", "Cyrillic"}, // NOTE -- language and script are different
          {"Pashto", "Arabic"},
          {"Persian", "Arabic"},
          {"Russian", "Cyrillic"},
          {"Serbian", "Cyrillic"},
          {"Turkmen", "Cyrillic"},
          {"Ukrainian", "Cyrillic"},
          {"Uzbek", "Cyrillic"},
  });

  private static int getScriptFromScriptOrLanguage(String script) {
    int pos = script.indexOf('/');
    if (pos >= 0) {
      script = script.substring(0,pos);
    }
    String newScript = language_to_script.get(script);
    if (newScript != null) {
      script = newScript;
    }
    int codeFromName = UScript.getCodeFromName(script);
    return codeFromName;
  }

  private static void doMainTable(CLDRTransforms.ParsedTransformID parsedID, Set<String> ids_old, PrintWriter pw,  Map<String,Map<String,Map<String,Boolean>>> xToTagToYToRoundtrip, boolean fromNonLatinToLatin, String scriptChoice, UnicodeSet totalNonLatinSet ) {

    Set<String> extras = new TreeSet(UCA);
    for (UnicodeSetIterator it = new UnicodeSetIterator(totalNonLatinSet); it.next();) {
      extras.add(it.getString());
    }

    // find the ids that actually occur
    Set<String> ids = new TreeSet(UCA);
    for (String x : xToTagToYToRoundtrip.keySet()) {
      ids.addAll(xToTagToYToRoundtrip.get(x).keySet());
    }
    if (ids.isEmpty()) {
      return;
    }
    System.out.println("ids: " + ids);

    pw.println("<table border='1' cellspacing='0'><tr>");
    if (fromNonLatinToLatin) {
      pw.println("<th>" + scriptChoice + "</th>");
    } else {
      pw.println("<th>Latin</th>");   
    }
    for (String id : ids) {
      id = id.replace("/", "/​");  // add code to allow linebreak
      pw.println("<th>" + (fromNonLatinToLatin ? id + "<br>to Latin" : "Latin to<br>" + id) + "</th>");       
    }  
    pw.println("</tr>");

    for (String x : xToTagToYToRoundtrip.keySet()) {
      pw.println("<tr>");
      showCell(pw, x, "");
      extras.remove(x);
      Map<String,Map<String,Boolean>> idToYToRoundTrip = xToTagToYToRoundtrip.get(x);
      for (String id : ids) {
        Map<String,Boolean> yToRoundTrip = idToYToRoundTrip.get(id);
        if (yToRoundTrip == null || yToRoundTrip.keySet().isEmpty()) {
          showCell(pw, "", "");
          continue;
        }
        boolean multiple = yToRoundTrip.keySet().size() != 1;
        if (multiple) {
          pw.println("<td><div align='center'><table><tr>");
        }
        for (String y : yToRoundTrip.keySet()) {
          // will be 1 or zero
          boolean roundTrip = yToRoundTrip.get(y);
          showCell(pw, y, roundTrip ?  "" : " class='miss'");
        }
        if (multiple) {
          pw.println("</tr></table></div></td>");
        }
      }
      pw.println("</tr>");
    }
    // print the unmapped items
    if (extras.size() != 0) {
      pw.println("<tr><th>Unmapped</th></tr>");   
      for (String x : extras) {
        pw.println("<tr>");
        showCell(pw, x, "");
        //      for (String id : ids) {
        //        showCell(pw, "", "");
        //      }
        pw.println("</tr>");
      }
    }

    pw.println("</table>");

  }

  private static void doMainTable2(CLDRTransforms.ParsedTransformID parsedID, Set ids, PrintWriter pw,  Map<String,String> mapping, boolean latinAtEnd ) {

    pw.println("<table border='1' cellspacing='0'><tr>");
    if (!latinAtEnd) pw.println("<th>Latin</th>");       
    for (Iterator it2 = ids.iterator(); it2.hasNext();) {
      parsedID.set((String)it2.next());
      pw.println("<th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th>");       
    }
    if (latinAtEnd) pw.println("<th>Latin</th>");       
    pw.println("</tr>");

    for (String source : mapping.keySet()) {
      String target = mapping.get(source);

      pw.println("<tr>");
      showCell(pw, source, "");
      showCell(pw, target, "");
      pw.println("</tr>");
    }
    pw.println("</table>");
  }

  private static class TranslitStatus {
    String source;
    String back;

    public TranslitStatus(String source, String back) {
      this.source = source;
      this.back = back;
    }
  }

  public static boolean hasScript(String string) {
    return getScriptFromScriptOrLanguage(string) >= 0;
  }

  private static Set getAvailableTransliterators() {
    Set results = new HashSet();
    for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements();) {
      results.add(e.nextElement());
    }
    return results;
  }

  private static UnicodeSet BIDI_R = (UnicodeSet) new UnicodeSet("[[:Bidi_Class=R:][:Bidi_Class=AL:]]").freeze();

  private static void showCell(PrintWriter pw, String item, String classString) {
    String backup = item;
    String name = getName(item, "; ");
    if (item.equals("")) {
      backup = "\u00a0";
      classString = " class='none'";
      name = "{missing}";
    } else if (item.charAt(0) >= '\uE000') {
      backup = "\u00a0";
    }

    name = TransliteratorUtilities.toXML.transliterate(name);
    backup = TransliteratorUtilities.toHTML.transliterate(backup);
    String dir = BIDI_R.containsSome(backup) ? " style='direction:rtl'" : "";
    pw.print("<td" + classString + dir + " title='" + name + "'>\u00a0\u00a0"
            + backup + "\u00a0\u00a0<br><tt>"
            + com.ibm.icu.impl.Utility.hex(item) + "</tt></td>");
  }

  public static String fix(String s) {
    if (s.equals("\u0946\u093E"))
      return "\u094A";
    if (s.equals("\u0C46\u0C3E"))
      return "\u0C4A";
    if (s.equals("\u0CC6\u0CBE"))
      return "\u0CCA";

    if (s.equals("\u0947\u093E"))
      return "\u094B";
    if (s.equals("\u0A47\u0A3E"))
      return "\u0A4B";
    if (s.equals("\u0AC7\u0ABE"))
      return "\u0ACB";
    if (s.equals("\u0C47\u0C3E"))
      return "\u0C4B";
    if (s.equals("\u0CC7\u0CBE"))
      return "\u0CCB";

    // return Normalizer.normalize(s,Normalizer.NFD,0);
    return s;
  }

  public static String getName(String s, String separator) {
    int cp;
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(s, i);
      if (i != 0)
        sb.append(separator);
      sb.append(UCharacter.getName(cp));
    }
    return sb.toString();
  }

  static public class CollectionOfComparablesComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1 == null) {
        if (o2 == null) return 0;
        return -1;
      } else if (o2 == null) {
        return 1;
      }
      Iterator i1 = ((Collection) o1).iterator();
      Iterator i2 = ((Collection) o2).iterator();
      while (i1.hasNext() && i2.hasNext()) {
        Comparable a = (Comparable) i1.next();
        Comparable b = (Comparable) i2.next();
        int result = a.compareTo(b);
        if (result != 0) {
          return result;
        }
      }
      // if we run out, the shortest one is first
      if (i1.hasNext())
        return 1;
      if (i2.hasNext())
        return -1;
      return 0;
    }

  }

  static class ReverseComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      String a = o1.toString();
      char a1 = a.charAt(0);
      String b = o2.toString();
      char b1 = b.charAt(0);
      if (a1 < 0x900 && b1 > 0x900)
        return -1;
      if (a1 > 0x900 && b1 < 0x900)
        return +1;
      return a.compareTo(b);
    }
  }


}
