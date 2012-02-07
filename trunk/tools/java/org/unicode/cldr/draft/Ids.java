package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public abstract class Ids implements Comparable<Ids> {

  static final UnicodeSet ALLOWED = new UnicodeSet("[[:Unified_Ideograph:]]").freeze(); // [:Block=CJK Radicals Supplement:][:Block=Kangxi Radicals:]
  static final UnicodeSet FULL_ALLOWED = new UnicodeSet("\\p{Block=Ideographic Description Characters}").addAll(ALLOWED).freeze();
  static final UnicodeSet FULL_ALLOWED_AND_PU = new UnicodeSet("[:General_Category=Private_Use:]").addAll(FULL_ALLOWED).freeze();
  static final UnicodeSet MAIN_CJK = new UnicodeSet("[[:block=CJK_Unified_Ideographs:]]").freeze();
  static final Normalizer2 nfc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
  static final char BAD_CHAR = '\uFFFF';
  static NumberFormat nf = NumberFormat.getInstance();
  static {
    nf.setGroupingUsed(true);
  }

  static class Hacks {
    Counter<Integer> badChars = new Counter<Integer>();
    Map<String,Integer> hackCodes = new HashMap<String, Integer>();
    List<String> hackStrings = new ArrayList<String>();
    Map<String,String> hackStringSample = new HashMap<String, String>();
    UnicodeMap<String> remap = new UnicodeMap<String>();
    UnicodeSet radicals = new UnicodeSet();
    String line = "";
    Relation<String,String> problems = new Relation(new LinkedHashMap(), LinkedHashSet.class);

    void addProblem(String problem) {
      if (problem != null) {
        problems.put(line, problem);
      }
    }

    Integer getHackCode(String item) {
      Integer fake = hackCodes.get(item);
      if (fake == null) {
        fake = 0xE000 + hackCodes.size();
        hackCodes.put(item, fake);
        hackStrings.add(item);
      }
      addProblem(item.equals("X-FFFF") ? "Trunc" : item.startsWith("X-") ? item : null);
      return fake;
    }

    public int getHackCode(int cp) {
      hacks.badChars.add(cp, 1);
      return getHackCode("Illegal-" + UTF16.valueOf(cp) + "-" + Utility.hex(cp));
    }

    public String getHackString(int codepoint) {
      int index = codepoint - 0xE000;
      return index < 0 || index >= hackStrings.size() ? null : hackStrings.get(index);
    }

    public void addBadChar(int cp) {
      badChars.add(cp, 1);
    }
  }

  static Hacks hacks = new Hacks();

  protected int codepoint;

  static class Visitor {
    public void atCodePoint(int codepoint) {}
    public void atRelation(int codepoint) {}
    public void atComponent(Ids first) {}
  }

  public boolean equals(Object other) {
    if (getClass() != other.getClass()) {
      return false;
    }
    return codepoint == ((Ids)other).codepoint;
  }

  public int hashCode() {
    return codepoint;
  }

  public static Ids parse(CharSequence s) {
    CodePoints codePoints = new CodePoints(s);
    Ids result = parse(codePoints);
    if (codePoints.next()) {
      hacks.addProblem("Superfluous chars at " + charAndHex(codePoints.getCodePoint()));
    }
    return result;
  }

  public void visit(Visitor visitor) {
    visitor.atCodePoint(codepoint);
  }

  public UnicodeSet addChars(UnicodeSet results, UnicodeMap<Ids> data) {
    results.add(codepoint);
    if (data != null) {
      Ids other = data.get(this.codepoint);
      if (other != null && !(other instanceof Leaf)) {
        return other.addChars(results, data);
      }
    }
    return results;
  }

  public boolean contains(int codepoint, UnicodeMap<Ids> data) {
    if (data != null) {
      Ids other = data.get(this.codepoint);
      if (other != null && !(other instanceof Leaf)) {
        return other.contains(codepoint, data);
      }
    }
    return (this.codepoint == codepoint);
  }

  public int size() {
    return 1;
  }

  abstract boolean minimize(Map<Ids, Integer> reverseData);

  abstract String toString(UnicodeMap<Ids> data, boolean replace, int maxLevel);

  protected String showCodepoint(UnicodeMap<Ids> data, boolean replace, int maxLevel) {
    Ids result = data.get(codepoint);
    if (result instanceof Leaf) {
      result = null;
    }
    return result == null ? UTF16.valueOf(codepoint)
            : !replace ? "*" + UTF16.valueOf(codepoint)
                    : maxLevel <= 0 ? "†" + UTF16.valueOf(codepoint)
                            : result.toString(data, replace, maxLevel-1);
  }

  protected static Ids parse(CodePoints codePoints) {
    codePoints.next();
    int cp = codePoints.getCodePoint();
    switch(cp) {
    // double
    case '\u2FF0':
    case '\u2FF1':
    case '\u2FF4':
    case '\u2FF5':
    case '\u2FF6':
    case '\u2FF7':
    case '\u2FF8':
    case '\u2FF9':
    case '\u2FFA':
    case '\u2FFB':
      return new Dual(cp, codePoints);
      // triple
    case '\u2FF2':
    case '\u2FF3':
      return new Trial(cp, codePoints);
    case '&':
      StringBuffer ncr = new StringBuffer();
      while (codePoints.next()) {
        cp = codePoints.getCodePoint();
        if (cp == ';') {
          return new Leaf(ncr.toString());
        }
        ncr.appendCodePoint(cp);
      }
      throw new IllegalArgumentException("NCR too short: " + ncr);
    default:
      return new Leaf(cp);
    }
  }

  static class LeafCounter extends Visitor {
    UnicodeMap<Ids> data;
    Counter<Integer> counter = new Counter<Integer>();
    Counter<Integer> relationCounter = new Counter<Integer>();
    public void clear() {
      counter.clear();
    }
    public void atCodePoint(int codepoint) {
      if (data != null) {
        Ids result = data.get(codepoint);
        if (result != null) {
          result.visit(this);
          return;
        }
      }
      counter.add(codepoint, 1);
    }
    public void atComponent(Ids first) {

    }
    public void atRelation(int codepoint) {
      relationCounter.add(codepoint, 1);
    }
  }

  static final class Leaf extends Ids {
    Leaf(int cp) {
      String nfcForm = nfc.normalize(UTF16.valueOf(cp));
      if (nfcForm.codePointCount(0, nfcForm.length()) != 1) {
        throw new IllegalArgumentException("NFC form is too long:\t" + Utility.hex(nfcForm));
      }
      cp = nfcForm.codePointAt(0);
      String revised = hacks.remap.get(cp);
      if (revised != null) {
        cp = revised.codePointAt(0);
      }
      if (!ALLOWED.contains(cp)) {
        cp = hacks.getHackCode(cp);
      }
      codepoint = cp;
    }

    public Leaf(String string) {
      codepoint = hacks.getHackCode(string);
    }

    // all else inherited
    public String toString() {
      //      if (codepoint >= 0xE000 && codepoint <= 0xEFFF) {
      //        return hacks.getHackString(codepoint);
      //      }
      return UTF16.valueOf(codepoint);
    }
    public int compareTo(Ids o) {
      if (o instanceof Leaf) {
        return codepoint - o.codepoint;
      } else {
        return -1;
      }
    }
    String toString(UnicodeMap<Ids> data, boolean replace, int maxLevel) {
      return showCodepoint(data, replace, maxLevel);
    }
    boolean minimize(Map<Ids, Integer> reverseData) {
      return false;
    }
  }

  static class Dual extends Ids {
    Ids first;
    Ids second;
    Dual(int cp, CodePoints codePoints) {
      codepoint = cp;
      first = Ids.parse(codePoints);
      second = Ids.parse(codePoints);
    }

    public boolean equals(Object other) {
      if (!super.equals(other)) return false;
      Dual that = (Dual) other;
      return first.equals(that.first) && second.equals(that.second); 
    }

    public int hashCode() {
      return codepoint ^ (37 * first.hashCode() ^ (37 * second.hashCode()));
    }

    public void visit(Visitor visitor) {
      visitor.atRelation(codepoint);
      visitor.atComponent(first);
      first.visit(visitor);
      visitor.atComponent(second);
      second.visit(visitor);
    }

    public String toString() {
      return "{" + UTF16.valueOf(codepoint) + first.toString() + second.toString() + "}";
    }
    public String toString(UnicodeMap<Ids> data, boolean replace, int maxLevel) {
      return "{" + showCodepoint(data, replace, maxLevel-1) + first.toString(data, replace, maxLevel-1) + second.toString(data,  replace, maxLevel-1) + "}";
    }
    public int compareTo(Ids o) {
      if (o instanceof Dual) {
        int diff = codepoint - o.codepoint;
        if (diff != 0) return diff;
        Dual other = (Dual) o;
        diff = first.compareTo(other.first);
        if (diff != 0) return diff;
        diff = second.compareTo(other.second);
        return diff;
      } else {
        return o instanceof Leaf ? 1 : -1;
      }
    }
    public UnicodeSet addChars(UnicodeSet results, UnicodeMap<Ids> data) {
      super.addChars(results, data);
      first.addChars(results, data);
      return second.addChars(results, data);
    }
    public boolean contains(int codepoint, UnicodeMap<Ids> data) {
      return super.contains(codepoint, data) || first.contains(codepoint, data) || second.contains(codepoint, data);
    }
    public int size() {
      return 1 + first.size() + second.size();
    }
    boolean minimize(Map<Ids, Integer> reverseData) {
      boolean result = false;
      if (!(first instanceof Leaf)) {
        Integer replacement = reverseData.get(first);
        if (replacement != null) {
          first = new Leaf(replacement);
          result = true;
        }
      }
      if (!(second instanceof Leaf)) {
        Integer replacement = reverseData.get(second);
        if (replacement != null) {
          second = new Leaf(replacement);
          result = true;
        }
      }
      return result;
    }
  }

  static final class Trial extends Dual {
    Ids third;
    Trial(int cp, CodePoints codePoints) {
      super(cp, codePoints);
      third = Ids.parse(codePoints);
    }
    public boolean equals(Object other) {
      if (!super.equals(other)) return false;
      Trial that = (Trial) other;
      return third.equals(that.third); 
    }

    public void visit(Visitor visitor) {
      visitor.atRelation(codepoint);
      visitor.atComponent(first);
      first.visit(visitor);
      visitor.atComponent(second);
      second.visit(visitor);
      visitor.atComponent(third);
      third.visit(visitor);
    }

    public int hashCode() {
      return codepoint ^ (37 * first.hashCode() ^ (37 * second.hashCode() ^ (37 * third.hashCode())));
    }
    public String toString() {
      return "{" + UTF16.valueOf(codepoint) + first.toString() + second.toString() + third.toString() + "}";
    }
    public String toString(UnicodeMap<Ids> data, boolean replace, int maxLevel) {
      return "{" + showCodepoint(data, replace, maxLevel-1) + first.toString(data, replace, maxLevel-1) + second.toString(data,  replace, maxLevel-1) + third.toString(data,  replace, maxLevel-1) + "}";
    }
    public int compareTo(Ids o) {
      if (o instanceof Trial) {
        int diff = codepoint - o.codepoint;
        if (diff != 0) return diff;
        Trial other = (Trial) o;
        diff = first.compareTo(other.first);
        if (diff != 0) return diff;
        diff = second.compareTo(other.second);
        if (diff != 0) return diff;
        diff = third.compareTo(other.third);
        return diff;
      } else {
        return 1;
      }
    }
    public UnicodeSet addChars(UnicodeSet results, UnicodeMap<Ids> data) {
      super.addChars(results, data);
      return third.addChars(results, data);
    }
    public boolean contains(int codepoint, UnicodeMap<Ids> data) {
      return super.contains(codepoint, data) || third.contains(codepoint, data);
    }
    public int size() {
      return super.size() + third.size();
    }
    boolean minimize(Map<Ids, Integer> reverseData) {
      boolean result = super.minimize(reverseData);
      if (!(third instanceof Leaf)) {
        Integer replacement = reverseData.get(third);
        if (replacement != null) {
          third = new Leaf(replacement);
          result = true;
        }
      }
      return result;
    }
  }

  static Comparator<Ids> IdsComparator = new Comparator<Ids>() {
    public int compare(Ids o1, Ids o2) {
      int diff = o1.size() - o2.size();
      if (diff != 0) return diff;
      return o1.compareTo(o2);
    }
  };
  /*
U+2FF0 ( ⿰ ) IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO RIGHT
U+2FF1 ( ⿱ ) IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO BELOW
U+2FF2 ( ⿲ ) IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO MIDDLE AND RIGHT
U+2FF3 ( ⿳ ) IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO MIDDLE AND BELOW
U+2FF4 ( ⿴ ) IDEOGRAPHIC DESCRIPTION CHARACTER FULL SURROUND
U+2FF5 ( ⿵ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM ABOVE
U+2FF6 ( ⿶ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM BELOW
U+2FF7 ( ⿷ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LEFT
U+2FF8 ( ⿸ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER LEFT
U+2FF9 ( ⿹ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER RIGHT
U+2FFA ( ⿺ ) IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LOWER LEFT
U+2FFB ( ⿻ ) IDEOGRAPHIC DESCRIPTION CHARACTER OVERLAID
   */

  static PrettyPrinter pp = new PrettyPrinter().setOrdering(RadicalStroke.RadicalStrokeComparator).setCompressRanges(false);

  public static void main(String[] args) throws IOException {

    TreeSet<String> sortedByRadicalStroke = new TreeSet<String>(RadicalStroke.RadicalStrokeComparator);
    for (String s : ALLOWED) {
      sortedByRadicalStroke.add(s);
    }

    //System.out.println(Ids.parse("⿱一⿰⿵冂丶⿵冂丶"));
    //System.out.println(Ids.parse("⿹&CDP-8BBF;一"));
    String dirString = "/Users/markdavis/Documents/workspace/DATA/ids/";
    File dir = new File(dirString);
    UnicodeMap<Ids> data = new UnicodeMap<Ids>();
    UnicodeSet mapsToSelf = new UnicodeSet();
    UnicodeSet charsInIds = new UnicodeSet();

    for (String file : dir.list()) {
      int counter = 0;
      BufferedReader in = BagFormatter.openUTF8Reader(dirString, file);
      boolean radicals = file.startsWith("CJK Radicals");
      boolean corrections = file.startsWith("X-Corrections");
      Map<Integer,String> radicalToBase = new TreeMap();

      while (true) {
        String line = in.readLine();
        if (line == null) {
          break;
        }
        ++counter;
        if (radicals) {
          if (line.startsWith("#")) continue;
          if (line.startsWith(";")) continue;
          // ;Radical Number,Status,Unified Ideo,Hex,Radical,Hex,Name,Conf. Char,Hex,Unified Ideo. has NO Remaining Strokes in Unihan
          // 5,Variant,,#VALUE!,⺄,U+2E84,SECOND_THREE
          // 6,Main,亅,U+4E85,⼅,U+2F05,HOOK,,,𠄌

          String[] split = line.split(",");
          // we depend on the file being set up so that the first integer radical has a Main. Detect bad cases.
          double radical = Double.parseDouble(split[0]);
          int baseRadical = (int) radical;
          String base = radicalToBase.get(baseRadical);
          if (base == null) {
            base = split[2];
            if (!"Main".equals(split[1]) || radical != baseRadical || !ALLOWED.containsAll(base)) {
              throw new IllegalArgumentException("Bad radical file");
            }
            radicalToBase.put(baseRadical, base);
            hacks.radicals.add(base);
          }
          if (!base.equals(split[2]) && split[2].length() != 0) {
            if (!ALLOWED.containsAll(base)) {
              throw new IllegalArgumentException("Bad radical file");
            }
            hacks.remap.put(split[2],base);
          }
          if (!base.equals(split[4]) && split[4].length() != 0) {
            if (!ALLOWED.containsAll(base)) {
              throw new IllegalArgumentException("Bad radical file");
            }
            hacks.remap.put(split[4],base);
          }
          continue;
        }
        // IDS files
        if (line.startsWith(";;")) continue;
        if (line.startsWith("U+4E0E")) {
          System.out.println("debug?");
        }
        hacks.line = line;
        String[] split = line.split("\\s+");
        Integer key = split[1].codePointAt(0);
        Ids ids;
        try {
          ids = Ids.parse(split[2]);
        } catch (Exception e) {
          out.println(nf.format(counter) + ")\t" + line + "\tERROR:\t" + e.getMessage());
          continue;
        }
        if (ids.codepoint == key) {
          mapsToSelf.add(key);
          continue;
        }
        if (!corrections) {
          Ids oldIds = data.get(key);
          if (oldIds != null) {
            out.println("Conflicting value for: " + key + "\t" + oldIds + "\t" + ids);
          }
        }
        data.put(key, ids);
      }
    }

    openFile("problems-parsing.txt", ";@ Parsing Problems:\t" + nf.format(hacks.problems.size()));
    int counter = 0;
    for (String s : hacks.problems.keySet()) {
      out.println(nf.format(++counter) + ")\t" + s + "\t" + hacks.problems.getAll(s));
    }

    openFile("problems-self.txt",";@ Character maps to self in IDS data:\t" + nf.format(mapsToSelf.size()));
    out.println(pp.format(mapsToSelf));


    openFile("problems-bad.txt",";@ Bad Chars in IDS data:\t" + nf.format(hacks.badChars.getTotal()));
    out.println(";count ; char ; (hex) ; name\n");
    for (Integer cp : hacks.badChars.getKeysetSortedByKey()) {
      if (cp == 0xFFFF) {
        out.println(hacks.badChars.getCount(cp) + "\t" + "TRUNCATED");
      } else {
        out.println(hacks.badChars.getCount(cp) + "\t" + charAndHex(cp) + "\t" + UCharacter.getName(cp));
      }
    }
    TreeSet<String> sortedUCA = new TreeSet(Collator.getInstance(ULocale.ROOT));
    for (String s : hacks.hackStrings) {
      if (!s.startsWith("X-")) {
        sortedUCA.add(s);
      }
    }
    openFile("problems-missing-components.txt",";@ Missing Components in IDS data:\t" + nf.format(sortedUCA.size()) + "\n");
    String tempStr = sortedUCA.toString();
    out.println(tempStr);

    // Get data for minimizing
    Map<Ids,Integer> reverseData2 = new HashMap();
    for (String key : data) {
      Ids value = data.get(key);
      Integer oldKey = reverseData2.get(value);
      if (oldKey == null || betterThan(key.codePointAt(0), oldKey)) {
        reverseData2.put(value, key.codePointAt(0));
      }
    }

    openFile("unihan-nostrokes.txt",";@ Unihan characters with no strokes besides radical");
    out.println("\n;Adding to IDS where missing");
    counter = 0;
    out.println(";line-number ; char ; (hex) ; radical ; set-with-no-extra-strokes\n");
    UnicodeMap<Integer> sameRadical = new UnicodeMap<Integer>();
    for (String s : sortedByRadicalStroke) {
      Integer strokes = RadicalStroke.SINGLETON.charToRemainingStrokes.get(s);
      if (strokes != null && strokes == 0) {
        Integer radical = RadicalStroke.SINGLETON.charToRadical.get(s);
        sameRadical.put(s, radical);
        if (!data.containsKey(s) && radical != s.codePointAt(0)) {
          data.put(s, new Leaf(radical));
        }
      }
    }
    for (Integer value : sameRadical.values()) {
      out.println(nf.format(++counter) + ")\t"
              + charAndHex(value) + "\t" + pp.format(new UnicodeSet(sameRadical.getSet(value)).remove(value)));
    }

    openFile("minimalization.txt",";@ Minimized IDS Data");
    out.println("; Replacing any subcomponent of an IDS by a character with that IDS.");
    out.println(";line-number ; char ; (hex) ; minimized-ids ; old-ids\n");
    counter = 0;
    for (Ids ids : reverseData2.keySet()) {
      if (ids.size() < 3) continue;
      Integer cp = reverseData2.get(ids);
      String old = ids.toString();
      if (ids.minimize(reverseData2)) {
        out.println(nf.format(++counter) + ")\t"
                + charAndHex(cp) + "\t" + ids + "\t" + old);
      }
    }

    //    counter = 0;
    //    for (String s : hacks.hackStringSample.keySet()) {
    //      out.println(nf.format(++counter) + ")\t" + "HACKS:\t" + charAndHex(s) + "\t" + hacks.hackStringSample.get(s));
    //    }

    Relation<Ids,Integer> reverseData = new Relation(new HashMap(), LinkedHashSet.class);
    UnicodeMap<UnicodeSet> charsToContainingChars = new UnicodeMap<UnicodeSet>();

    // get other data

    for (String key : data) {
      Ids value = data.get(key);
      reverseData.put(value,key.codePointAt(0));

      value.addChars(charsInIds.clear(), null);
      charsInIds.remove(key);

      for (UnicodeSetIterator it = new UnicodeSetIterator(charsInIds); it.next();) {
        UnicodeSet containingSet = charsToContainingChars.get(it.codepoint);
        if (containingSet == null) {
          charsToContainingChars.put(it.codepoint, containingSet = new UnicodeSet());
        }
        containingSet.add(key);
      }
    }

    openFile("same-ids.txt",";@ Characters with same IDS");
    out.println(";line-number ; charset ; (hex) ; ids\n");
    counter = 0;
    for (Ids ids : reverseData.keySet()) {
      Set<Integer> set = reverseData.getAll(ids);
      if (set.size() == 1) continue;
      UnicodeSet uset = new UnicodeSet();
      for (int i : set) {
        uset.add(i);
      }
      out.println(nf.format(++counter) + ")\t"
              + pp.format(uset) + "\t" + ids);
    }

    openFile("chars-to-ids.txt",";@ Characters to IDS");
    out.println("; Sorted by total-strokes, then radical (from Unihan data)");
    out.println(";line-number ; total-strokes/radical ; char ; (hex) ; ids-contains-radical? ; ids");
    counter = 0;

    UnicodeSet missingInfo = new UnicodeSet();
    int oldStrokes = 0;
    int oldRadical = 0;
    for (String containingChar : sortedByRadicalStroke) {
      Ids ids = data.get(containingChar);
      if (ids == null) {
        missingInfo.add(containingChar);
        continue;
      }
      Integer radical = RadicalStroke.SINGLETON.charToRadical.get(containingChar);
      if (radical == null) radical = '?';
      Integer strokes = RadicalStroke.SINGLETON.charToTotalStrokes.get(containingChar);
      if (strokes == null) strokes = 0;
      if (radical != oldRadical || strokes != oldStrokes) {
        out.println();
        oldRadical = radical;
        oldStrokes = strokes;
      }
      //      if (ids instanceof Leaf && ids.codepoint == containingChar.codePointAt(0)) {
      //        continue;
      //      }
      out.println(nf.format(++counter) + ")\t"
              + strokes + UTF16.valueOf(radical)
              + "\t" + charAndHex(containingChar)
              + "\t" + (ids != null && ids.contains(radical, data) ? "" : "N")
              + "\t" + ids);
    }

    openFile("chars-without-ids.txt",";@ Characters without Ids data\t" + nf.format(missingInfo.size()));
    out.println(";\tcount ; total-strokes/radical ; set-without-ids\n");
    counter = 0;
    oldStrokes = 0;
    oldRadical = 0;
    TreeSet<String> sortedByRadicalStroke2 = new TreeSet<String>(RadicalStroke.RadicalStrokeComparator);
    for (String s : missingInfo) {
      sortedByRadicalStroke2.add(s);
    }
    UnicodeSet temp = new UnicodeSet();
    for (String containingChar : sortedByRadicalStroke2) {
      Integer radical = RadicalStroke.SINGLETON.charToRadical.get(containingChar);
      if (radical == containingChar.codePointAt(0)) {
        continue; // skip radicals
      }
      if (radical == null) radical = '?';
      Integer strokes = RadicalStroke.SINGLETON.charToTotalStrokes.get(containingChar);
      if (strokes == null) strokes = 0;
      if (radical != oldRadical || strokes != oldStrokes) {
        if (temp.size() != 0) {
          out.println(nf.format(++counter) + ")\t" + oldStrokes + UTF16.valueOf(oldRadical) + "\t" + pp.format(temp));
        }
        oldRadical = radical;
        oldStrokes = strokes;
        temp.clear();
      }
      temp.add(containingChar);
      //      if (ids instanceof Leaf && ids.codepoint == containingChar.codePointAt(0)) {
      //        continue;
      //      }
    }
    out.println(nf.format(++counter) + ")\t" + oldStrokes + UTF16.valueOf(oldRadical) + "\t" + pp.format(temp));


    openFile("expanded-ids.txt",";@ Characters to IDS-Expansion");
    out.println("; Shows the recursive expansion of IDS, if different");
    out.println(";line-number ; char ; (hex) ; ids ; expanded-ids\n");
    TreeSet<Ids> sortedIds = new TreeSet<Ids>(IdsComparator);
    sortedIds.addAll(data.getAvailableValues());
    LeafCounter leafCounter = new LeafCounter();
    leafCounter.data = data;
    counter = 0;
    for (String containingChar : sortedByRadicalStroke) {
      Ids ids = data.get(containingChar);
      if (ids == null) continue;
      ids.visit(leafCounter);
      String idsSimple = ids.toString();
      String idsString = ids.toString(data, true, 20);
      if (!idsSimple.equals(idsString)) {
        out.println(nf.format(++counter) + ")\t"
                + charAndHex(containingChar)
                + "\t" + idsSimple
                + "\t" + idsString
        );
      }
    }
    openFile("chars-in-expanded-ids.txt",";@ Characters present in expanded IDS:\t" + nf.format(leafCounter.counter.size()));
    UnicodeSet allowedExpanded = new UnicodeSet();
    UnicodeSet radicalsAllowed = new UnicodeSet();
    for (Integer cp : leafCounter.counter.getKeysetSortedByCount(false)) {
      if (ALLOWED.contains(cp)) {
        if (hacks.radicals.contains(cp)) {
          radicalsAllowed.add(cp);
        } else {
          allowedExpanded.add(cp);
        }
      }
    }
    out.println("; Radicals:\t" + radicalsAllowed.toPattern(false));
    out.println("; Other:\t" + allowedExpanded.toPattern(false));
    for (Integer cp : leafCounter.counter.getKeysetSortedByCount(false)) {
      out.println(nf.format(leafCounter.counter.get(cp)) + "\t" + charAndHex(cp));
    }

    openFile("chars-to-containers.txt",";@ Characters to Containers");
    out.println("; A container is one whose IDS expansion contains the character directly.");
    out.println("; This doesn't include recursive containing (just to keep the size down).");
    out.println(";line-number ; char ; (hex) ; set-containing-char\n");
    counter = 0;
    UnicodeSet inOtherChar = new UnicodeSet();
    for (String containedChar : sortedByRadicalStroke) {
      UnicodeSet keyset = charsToContainingChars.get(containedChar);
      if (keyset == null) {
        continue;
      }
      inOtherChar.add(containedChar);
      out.println(nf.format(++counter) + ")\t"
              + charAndHex(containedChar) + "\t" + pp.format(keyset));
    }

    int limitHack = 0xE000 + hacks.hackStrings.size();
    for (int containedChar = 0xE000; containedChar < limitHack; ++containedChar) {
      UnicodeSet keyset = charsToContainingChars.get(containedChar);
      if (keyset == null) {
        continue;
      }
      out.println(nf.format(++counter) + ")\t"
              + charAndHex(containedChar) + "\t" + pp.format(keyset));
    }


    openFile("chars-in-ids.txt",";@ Characters present in some IDS:\t" + nf.format(inOtherChar.size()));
    out.println(pp.format(inOtherChar));

    out.close();
    System.out.println("DONE");
  }

  static PrintWriter out = null;
  private static void openFile(String filename, String header) throws IOException {
    if (out != null) {
      out.close();
    }
    out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY+"/../ids/", filename);
    out.print('\uFEFF');
    out.println(header);
  }

  private static boolean betterThan(int cp1, int cp2) {
    return MAIN_CJK.contains(cp1) && !MAIN_CJK.contains(cp2);
  }

  private static String charAndHex(UnicodeSet uset) {
    return pp.format(uset) + "\t(" + uset + ")";
  }

  private static String charAndHex(int codepoint) {
    String hack = hacks.getHackString(codepoint);
    return UTF16.valueOf(codepoint) + "\t(" + (hack == null ? Utility.hex(codepoint) : hack) + ")";
  }

  private static String charAndHex(String codepoint) {
    return codepoint + "\t(" + Utility.hex(codepoint) + ")";
  }

}
