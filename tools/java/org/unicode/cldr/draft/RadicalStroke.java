package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.util.FileUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class RadicalStroke {
  // U+3433 kRSUnicode  9.3
  private static Pattern RAD_STROKE = Pattern.compile("U\\+([A-Z0-9]+)\\s+kRSUnicode\\s+(.*)");
  private static Pattern RAD_DATA = Pattern.compile("([0-9]{1,3}\\'?)\\.([0-9]{1,2})\\s*");

  private static Pattern TOTAL_STROKE = Pattern.compile("U\\+([A-Z0-9]+)\\s+kTotalStrokes\\s+(.*)");
  private static Pattern IICORE = Pattern.compile("U\\+([A-Z0-9]+)\\s+kIICore\\s+(.*)");

  public static RadicalStroke SINGLETON = new RadicalStroke();

  Map<Integer,Map<String,Map<Integer,UnicodeSet>>> radStrokesToRadToRemainingStrokes;
  UnicodeSet remainder;
  UnicodeSet iiCoreSet = new UnicodeSet();
  UnicodeMap<Integer> charToTotalStrokes = new UnicodeMap();
  UnicodeMap<Integer> charToRemainingStrokes = new UnicodeMap();
  UnicodeMap<Integer> charToRadical = new UnicodeMap();

  private RadicalStroke() {
    try {
      // load the radicals
      for (Integer cjk :  ScriptCategories.RADICAL_CHAR2STROKES.keySet()) {
        charToTotalStrokes.put(cjk, ScriptCategories.RADICAL_CHAR2STROKES.get(cjk));
      }
      Matcher radStrokeMatcher = RAD_STROKE.matcher("");
      Matcher radDataMatcher = RAD_DATA.matcher("");
      Matcher iiCore = IICORE.matcher("");
      radStrokesToRadToRemainingStrokes = new TreeMap<Integer, Map<String,Map<Integer,UnicodeSet>>>();
      remainder = ScriptCategories.parseUnicodeSet("[:script=Han:]").removeAll(GeneratePickerData.SKIP);
      String dataDir = CldrUtility.getProperty("DATA_DIR", "/Users/markdavis/Documents/workspace35/DATA/UCD/5.2.0-Update/Unihan/");

      String unihanFile = GeneratePickerData.unicodeDataDirectory + "Unihan.txt";
      BufferedReader in = new BufferedReader(
              new FileReader(
                      Subheader.getFileNameFromPattern(
                              dataDir, "Unihan_RadicalStrokeCounts.*\\.txt")));

      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (iiCore.reset(line).matches()) {
          int cp = Integer.parseInt(iiCore.group(1),16);
          iiCoreSet.add(cp);
        } else if (radStrokeMatcher.reset(line).matches()) {
          int cp = Integer.parseInt(radStrokeMatcher.group(1), 16);
          String[] items = radStrokeMatcher.group(2).split("\\s");
          for (String item : items) {
            if (!radDataMatcher.reset(item).matches()) {
              throw new IllegalArgumentException("Bad line: " + line);
            }
            String radical = radDataMatcher.group(1);
            int radicalChar = ScriptCategories.RADICAL_NUM2CHAR.get(radical);
            charToRadical.put(cp,radicalChar);
            int radicalStrokes = ScriptCategories.RADICAL_CHAR2STROKES.get(radicalChar);
            int remainingStrokes = Integer.parseInt(radDataMatcher.group(2));
            charToTotalStrokes.put(cp, radicalStrokes + remainingStrokes);
            charToRemainingStrokes.put(cp, remainingStrokes);

            //          if (radical.startsWith("211")) {
            //            System.out.println(line);
            //          }
            //String baseRadical = radical.endsWith("'") ? radical.substring(0, radical.length()-1) : radical;
            RadicalStroke.mapToUnicodeSetAdd(radStrokesToRadToRemainingStrokes, radicalStrokes, radical, remainingStrokes, cp);
            remainder.remove(cp);
            //          if (radDataMatcher.group(2).equals("0") && radical.endsWith("'")) {
            //            String radicalString = Normalizer.normalize(cp, Normalizer.NFKC);
            //            String old = radicalToChar.get(radical);
            //            if (old == null) {
            //              radicalToChar.put(radical, radicalString);
            //            } else if (!radicalString.equals(old)) {
            //              System.out.println("Duplicate radical: " + line + " with " + radicalString + " and " + old);
            //            }
            //          }
          }
        }
      }
      in.close();

      // fix the compat characters that didn't have strokes
      remainder.retainAll(GeneratePickerData.COMPATIBILITY);

      remainder.freeze();
      charToTotalStrokes.freeze();
      charToRemainingStrokes.freeze();
      charToRadical.freeze();

      radStrokesToRadToRemainingStrokes = CldrUtility.protectCollection(radStrokesToRadToRemainingStrokes);

      //    UnicodeSet temp = new UnicodeSet();
      //    for (UnicodeSetIterator it = new UnicodeSetIterator(ScriptCategories.parseUnicodeSet("[:script=Han:]").removeAll(SKIP)); it.next();) {
      //      temp.add(it.codepoint);
      //      if (temp.size() >= 800) {
      //        int code = temp.charAt(0);
      //        CATEGORYTABLE.add("Han (CJK)", false, UTF16.valueOf(code) + " Han " + toHex(code, false), false, temp);
      //        temp.clear();
      //      }
      //    }
      //    if (temp.size() > 0) {
      //      int code = temp.charAt(0);
      //      CATEGORYTABLE.add("Han (CJK)", false, UTF16.valueOf(code) + " Han " + toHex(code, false), false, temp);
      //    }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  static <T>void mapToUnicodeSetAdd(Map<Integer, Map<String, Map<Integer, UnicodeSet>>> index,
          int radicalStrokes, String radicalChar, int remainingStrokes, int cp) {
    Map<String, Map<Integer, UnicodeSet>> subIndex = index.get(radicalStrokes);
    if (subIndex == null) {
      index.put(radicalStrokes, subIndex = new TreeMap<String, Map<Integer, UnicodeSet>>(GeneratePickerData.UCA));
    }
    Map<Integer, UnicodeSet> uset = subIndex.get(radicalChar);
    if (uset == null) {
      subIndex.put(radicalChar, uset = new TreeMap<Integer, UnicodeSet>());
    }
    UnicodeSet uset2 = uset.get(remainingStrokes);
    if (uset2 == null) {
      uset.put(remainingStrokes, uset2 = new UnicodeSet());
    }
    uset2.add(cp);
  }
  
  static Comparator RadicalStrokeComparator = new Comparator() {
    CodePoints cps1 = new CodePoints("");
    CodePoints cps2 = new CodePoints("");
    
    public int compare(Object o1, Object o2) {
      cps1.reset((CharSequence)o1);
      cps2.reset((CharSequence)o2);
      boolean n1 = cps1.next();
      boolean n2 = cps2.next();
      // shorter strings are less
      if (!n1) {
        return n2 ? -1 : 0;
      } else if (!n2) {
        return 1;
      }
      int cp1 = cps1.getCodePoint();
      int cp2 = cps2.getCodePoint();
      
      // lower stroke counts are less (null counts as zero)
      Integer s1 = SINGLETON.charToTotalStrokes.get(cp1);
      Integer s2 = SINGLETON.charToTotalStrokes.get(cp2);
      if (s1 == null && s2 == null) {
        // no info, return codepoint order
        return cp1 - cp2;
      }
      int ss1 = s1 == null ? 0 : s1;
      int ss2 = s2 == null ? 0 : s2;
      if (ss1 < ss2) return -1;
      if (ss1 > ss2) return 1;
      
      Integer r1 = SINGLETON.charToRadical.get(cp1);
      Integer r2 = SINGLETON.charToRadical.get(cp2);
      if (r1 < r2) return -1;
      if (r1 > r2) return 1;
      // no other diff, return codepoint order
      return cp1 - cp2;
    }
  };
  
}
