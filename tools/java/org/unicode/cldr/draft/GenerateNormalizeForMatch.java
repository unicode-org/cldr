package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.UnicodeMap;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class GenerateNormalizeForMatch {

  static UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]");
  static UnicodeSet U51 = new UnicodeSet("[[:age=5.1:]-[:age=5.0:]]");
  static UnicodeSet ALPHANUM = new UnicodeSet("[[:alphabetic:][:Nd:]]");

  static UnicodeMap nameException = new UnicodeMap();
  static {
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs:]"), "<CJK Ideograph>");
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension A:]"), "<CJK Ideograph Extension A>");
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension B:]"), "<CJK Ideograph Extension B>");
  }

  static UnicodeSet noNormalization = new UnicodeSet("[" +
          "[:decomposition_type=super:]" +
          "[:decomposition_type=sub:]" +
          "[:decomposition_type=circle:]" +
          "[:decomposition_type=Fraction:]" +
          //"[:decomposition_type=compat:]" +
          "[:decomposition_type=square:]" +
  "]");

  public static void main(String[] args) throws IOException {
    //fixOld("folding_resolved.txt", "folding_resolved_reordered.txt");
    //fixOld("normalizeForMatch-old.txt", "normalizeForMatch-oldMarked.txt");
    fixNew();
    System.out.println("DONE");
  }
  private static void fixOld(String sourceFile, String targetFile) throws IOException {
    UnicodeMap oldMap = new UnicodeMap();
    BufferedReader in = BagFormatter.openUTF8Reader(Utility.GEN_DIRECTORY+"/../normalize/", sourceFile);
    int lineNumber = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      getMappingFromSemiLine(lineNumber++, line, oldMap, false);
    }
    in.close();
    PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY+"/../normalize/", targetFile);
    for (UnicodeSetIterator it = new UnicodeSetIterator(oldMap.keySet()); it.next();) {
      String str = it.getString();
      String other = (String) oldMap.getValue(it.codepoint);
      writeMapping(out, str, other);
    }
    out.close();
  }

  static void fixNew() throws IOException {
    PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY+"/../normalize/", "normalizeForMatch.txt");
    //XEquivalenceClass equivs = new XEquivalenceClass(null);
    UnicodeSet exclusions = new UnicodeSet("[[:unassigned:][\u00DF\u0130][:Lm:]]");
    UnicodeSet inclusions = new UnicodeSet(exclusions).complement();
    int linecount = 0;
    UnicodeMap mappings = new UnicodeMap();
    for (UnicodeSetIterator it = new UnicodeSetIterator(inclusions); it.next();) {
      String str = it.getString();
      String other = str;
      if (noNormalization.contains(it.codepoint)) {
        other = UCharacter.foldCase(other, true);
      } else {
        other = normalizeSpecial(other);
        other = UCharacter.foldCase(other, true);
        other = normalizeSpecial(other);
      }
      if (other.equals(str)) continue;
      if (other.startsWith(" ")) {
        if (!WHITESPACE.contains(str)) continue;
      }
      mappings.put(it.codepoint, other);
    }
    // special mappings
    getExceptions(mappings);
    // 1E9E ; 00DF ; # LATIN CAPITAL LETTER SHARP S => LATIN SMALL LETTER SHARP S 

    // <CJK Ideograph>
    // 3038 ; 5341 ; # HANGZHOU NUMERAL TEN => <CJK Ideograph> 

    // print them
    for (UnicodeSetIterator it = new UnicodeSetIterator(mappings.keySet()); it.next();) {
      String str = it.getString();
      String other = (String) mappings.getValue(it.codepoint);
      // <CJK Ideograph>
      writeMapping(out, str, other);
      linecount++;
    }
    out.close();
  }
  private static String normalizeSpecial(String other) {
    String result = Normalizer.normalize(other, Normalizer.NFKC, 0);
    if (ALPHANUM.containsAll(result)) return result;
    return other;
  }

  private static void writeMapping(PrintWriter out, String source, String target) {
    String otherName = jimName(target);
    String age = U51.containsSome(source) || U51.containsSome(target) ? "[U5.1] " : "";
    out.println(hex(source," ")
            + " ; " + hex(target," ").replace(",", " ")
            + " ; # " + age + UCharacter.getName(source, " + ")
            + " => " + otherName
    );
  }

  private static void test() {
    for (String item : new String[]{"zh-Hant", "zh-Hans", "pt-PT", "pt-BR"}) {
      String name = ULocale.getDisplayName(item, "en");
      System.out.println(item + "\t => \t" + name);
    }
  }

  private static void getExceptions(UnicodeMap mappings) throws IOException {
    mappings.putAll(new UnicodeSet("[[:default_ignorable_code_point:]&[:assigned:]]"), "");
    mappings.put(0x0130, "\u0069");
    mappings.put(0x1E9E, "\u00DF");
    File foo = new File("normalizeForMatchExceptions.txt");
    System.out.println(foo.getCanonicalPath());
    BufferedReader in = BagFormatter.openUTF8Reader("java/org/unicode/cldr/draft/", "normalizeForMatchExceptions.txt");
    int lineNumber = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      getMappingFromSemiLine(lineNumber++, line, mappings, true);
    }
    in.close();
  }

  private static void getMappingFromSemiLine(int lineNumber, String line, UnicodeMap mappings, boolean skipIfIdentical) {
    line = line.trim();
    if (line.startsWith("\uFEFF")) {
      line = line.substring(1);
    }
    System.out.println(lineNumber + " Line: " + line);
    int commentPos = line.indexOf("#");
    if (commentPos >= 0) {
      line = line.substring(0,commentPos);
    }
    if (line.length() == 0) return;

    String[] pieces = line.split("\\s*;\\s*");
    String[] starts = pieces[0].split("\\s*-\\s*");
    int start = Integer.parseInt(starts[0], 16);
    int end = starts.length == 1 ? start : Integer.parseInt(starts[1], 16);
    final String target = pieces.length == 1 ? "" : pieces[1];
    if (target.equalsIgnoreCase("exclude")) {
      mappings.putAll(start, end, null);
    } else {
      for (int i = start; i <= end; ++i) {
        
        final String newTarget = fromHex(target);
        if (skipIfIdentical) {
          String oldTarget = (String)mappings.getValue(i);
          if (newTarget.equals(oldTarget)) {
            System.out.println("UNNEC: " + hex(UTF16.valueOf(i), " ") + "; " + hex(newTarget, " "));
          }
        }
        mappings.put(i, newTarget);
      }
    }
  }
  private static String fromHex(String spaceDelimitedHex) {
    spaceDelimitedHex = spaceDelimitedHex.trim();
    if (spaceDelimitedHex.length() == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    String[] pieces = spaceDelimitedHex.split("\\s+");
    for (String piece : pieces) {
      result.append(UTF16.valueOf(Integer.parseInt(piece, 16)));
    }
    return result.toString();
  }

  private static String jimName(String other) {
    String otherName = UTF16.countCodePoint(other) != 1 ? null 
            : (String) nameException.getValue(UTF16.charAt(other, 0));
    if (otherName == null) {
      otherName = UCharacter.getName(other, " + ");
    }
    return otherName;
  }

  public static String hex(String s, String separator) {
    StringBuffer result = new StringBuffer();
    int cp = 0;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      if (i != 0) result.append(separator);
      cp = UTF16.charAt(s, i);
      result.append(com.ibm.icu.impl.Utility.hex(cp, 4));
    }
    return result.toString();
  }
}
