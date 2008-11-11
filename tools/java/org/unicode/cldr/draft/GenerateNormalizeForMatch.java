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

  enum AllowedResults {WHITESPACE_CHECK, ID_STATUS_SAME, ONLY_ID}

  private static final boolean SHOW_AGE = false;

  private static final UnicodeSet SKIP_ID_CHECK = new UnicodeSet("[\u3000[:decomposition_type=wide:]]");
  
  private static AllowedResults ALLOWED_RESULTS = AllowedResults.ONLY_ID;
  private static boolean ONLY_OLD = false;
  
  private static UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]");
  private static UnicodeSet U51 = new UnicodeSet("[[:age=5.1:]-[:age=5.0:]]");
  private static UnicodeSet ID_CHARACTERS = new UnicodeSet("[[:alphabetic:][:Nd:][:m:]]");

  private static UnicodeMap nameException = new UnicodeMap();
  static {
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs:]"), "<CJK Ideograph>");
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension A:]"), "<CJK Ideograph Extension A>");
    nameException.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension B:]"), "<CJK Ideograph Extension B>");
  }
//
//  static UnicodeSet noNormalization = new UnicodeSet("[" +
//          "[:decomposition_type=super:]" +
//          "[:decomposition_type=sub:]" +
//          "[:decomposition_type=circle:]" +
//          "[:decomposition_type=Fraction:]" +
//          //"[:decomposition_type=compat:]" +
//          "[:decomposition_type=square:]" +
//  "]");

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
    UnicodeMap special_mappings = new UnicodeMap();
    // special mappings
    getExceptions(special_mappings);

    UnicodeSet assigned = new UnicodeSet("[:assigned:]");

    int linecount = 0;
    UnicodeMap mappings = new UnicodeMap();
    for (UnicodeSetIterator it = new UnicodeSetIterator(assigned); it.next();) {
      if (ONLY_OLD && U51.contains(it.codepoint)) {
        continue;
      }
      String str = it.getString();
      String other = str;
      String defaultChange = normalizeSpecial(other);
      defaultChange = UCharacter.foldCase(defaultChange, true);
      defaultChange = normalizeSpecial(defaultChange);

      String special = (String) special_mappings.getValue(it.codepoint);
      if (special == null) {
        other = defaultChange;
      } else if (special.equalsIgnoreCase("exclude")) {
        continue;
      } else if (special.equalsIgnoreCase("caseonly")) {
        other = UCharacter.foldCase(other, true);
      } else {
        other = special;
      }
      if (special != null && defaultChange.equals(other)) {
        System.out.println("UNNECESSARY " + codeAndName(str)
                //+ ";\t\twas " + codeAndName(defaultChange)
                + ";\t\tnow " + codeAndName(other)
                );
      }
      if (other.equals(str)) continue;
      mappings.put(it.codepoint, other);
    }

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
  
  private static String codeAndName(String defaultChange) {
    return hex(defaultChange," ") + " - " + UCharacter.getName(defaultChange, " ");
  }
  
  private static String normalizeSpecial(String source) {
    String result = Normalizer.normalize(source, Normalizer.NFKC, 0);
    if (result.startsWith(" ")) {
      if (!WHITESPACE.contains(source)) {
        return source;
      }
    }
    switch (ALLOWED_RESULTS) {
      default:
        throw new IllegalArgumentException("Unexpected option");
      case WHITESPACE_CHECK:
        break;
      case ID_STATUS_SAME:
        if (SKIP_ID_CHECK.containsAll(source)) {
          return result;
        }
        if (!ID_CHARACTERS.containsAll(result)) {
          if (ID_CHARACTERS.containsAll(source)) {
            return source;
          }
        }
        break;
      case ONLY_ID:
        if (SKIP_ID_CHECK.containsAll(source)) {
          return result;
        }
        if (!ID_CHARACTERS.containsAll(result)) {
          return source;
        }
        break;
    }
    return result;
  }

  private static void writeMapping(PrintWriter out, String source, String target) {
    String otherName = jimName(target);
    String age = SHOW_AGE && (U51.containsSome(source) || U51.containsSome(target)) ? "[U5.1] " : "";
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
    line = line.trim();
    if (line.length() == 0) return;
    if (line.startsWith("@")) {
      if (line.equalsIgnoreCase("@ALL")) {
        ONLY_OLD = false;
      } else if (line.equalsIgnoreCase("@ONLY_OLD")) {
          ONLY_OLD = true;
      } else {
        ALLOWED_RESULTS = AllowedResults.valueOf(line.substring(1).toUpperCase());
      }
      return;
    }

    String[] pieces = line.split("\\s*;\\s*");
    if (pieces.length == 1) {
      throw new IllegalArgumentException("Line without target: " + line);
    }
    UnicodeSet source = new UnicodeSet();
    if (UnicodeSet.resemblesPattern(pieces[0], 0)) {
      source.applyPattern(pieces[0]);
    } else {
      String[] starts = pieces[0].split("\\s*-\\s*");
      int start = Integer.parseInt(starts[0], 16);
      int end = starts.length == 1 ? start : Integer.parseInt(starts[1], 16);
      source.add(start,end);
    }
    
    final String target = pieces[1]; // pieces.length == 1 ? "" : pieces[1];
    if (target.equalsIgnoreCase("exclude") || target.equalsIgnoreCase("caseonly")) {
      mappings.putAll(source, target);
    } else if (target.equalsIgnoreCase("delete")) {
      mappings.putAll(source, "");
    } else if (target.equalsIgnoreCase("ok")) {
      mappings.putAll(source, null); // remove exception
    } else {
      final String newTarget = fromHex(target);
      mappings.putAll(source, newTarget);
//      for (int i = start; i <= end; ++i) {
//        
//        
//        if (skipIfIdentical) {
//          String oldTarget = (String)mappings.getValue(i);
//          if (newTarget.equals(oldTarget)) {
//            System.out.println("UNNEC: " + hex(UTF16.valueOf(i), " ") + "; " + hex(newTarget, " "));
//          }
//        }
//        mappings.put(i, newTarget);
//      }
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
/*

*/
