package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.PatternFixer.Target;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class GeneratePickerData {
  private static final String ARCHAIC_MARKER = "\uE000";
  private static final String MAIN_SUB_SEPARATOR = ":";
  private static final String MAIN_SUBSUB_SEPARATOR = "~";

  private static final String AMERICAN = "American Scripts";

  private static final String EUROPEAN = "Other European Scripts";

  private static final String AFRICAN = "African Scripts";

  private static final String MIDDLE_EASTERN = "Middle Eastern Scripts";

  private static final String SOUTH_ASIAN = "South Asian Scripts";

  private static final String SOUTHEAST_ASIAN = "Southeast Asian Scripts";

  private static final String EAST_ASIAN = "Other East Asian Scripts";
  
  private static final UnicodeSet COMPATIBILITY = (UnicodeSet) new UnicodeSet("[:nfkcqc=n:]").freeze();

  static final boolean DEBUG = false;

  private static final UnicodeSet SKIP = (UnicodeSet) new UnicodeSet("[[:cn:][:cs:][:co:][:cc:]\uFFFC]").addAll(ScriptCategories.DEPRECATED_NEW).freeze();
  private static final UnicodeSet KNOWN_DUPLICATES = (UnicodeSet) new UnicodeSet("[:Nd:]").freeze();

  public static final UnicodeSet UNCOMMON = (UnicodeSet) new UnicodeSet(ScriptCategories.ARCHAIC).addAll(COMPATIBILITY).freeze();

  private static final UnicodeSet NAMED_CHARACTERS = (UnicodeSet) new UnicodeSet(
          "[[:Z:][:default_ignorable_code_point:][:Pd:][:cf:]]"
  ).removeAll(SKIP).freeze();
  private static final UnicodeSet MODERN_JAMO = (UnicodeSet) new UnicodeSet(
          "[\u1100-\u1112 \u1161-\u1175 \u11A8-\u11C2]"
  ).removeAll(SKIP).freeze();

  private static final UnicodeSet HST_L = (UnicodeSet) new UnicodeSet("[:HST=L:]").freeze();
  private static final UnicodeSet single = (UnicodeSet) new UnicodeSet("[[:HST=L:][:HST=V:][:HST=T:]]").freeze();
  private static final UnicodeSet syllable = (UnicodeSet) new UnicodeSet("[[:HST=LV:][:HST=LVT:]]").freeze();
  private static final UnicodeSet all = (UnicodeSet) new UnicodeSet(single).addAll(syllable).freeze();
  private static final UnicodeSet REAL_HAN = new UnicodeSet("[々〇〡-〩〸-〻]");

  //private static final UnicodeSet WHITESPACE = (UnicodeSet) new UnicodeSet("[:whitespace:]").freeze();

  static RuleBasedCollator ENGLISH_BASE = (RuleBasedCollator) Collator.getInstance(Locale.ENGLISH);
  static {
    ENGLISH_BASE.setNumericCollation(true);
  }

  static Comparator<String> ENGLISH = new MultilevelComparator<String>(
          ENGLISH_BASE,
          new UTF16.StringComparator()
  );

  static Comparator<String> buttonComparator = new MultilevelComparator<String>(
          //new UnicodeSetInclusionFirst(new UnicodeSet("[:ascii:]")),
          new UnicodeSetInclusionFirst(new UnicodeSet("[[:Letter:]&[:^NFKD_QuickCheck=N:]]")),
          new UnicodeSetInclusionFirst(new UnicodeSet("[:Letter:]")),
          ENGLISH,
          new UTF16.StringComparator()
  );

  static Comparator<String> subCategoryComparator = new Comparator<String>() {
    public int compare(String o1, String o2) {
      boolean a = o1.startsWith(ARCHAIC_MARKER);
      boolean b = o2.startsWith(ARCHAIC_MARKER);
      if (a != b) {
        return a ? 1 : -1;
      }
      return ENGLISH.compare(o1, o2);
    }
  };

  static CategoryTable CATEGORYTABLE = new CategoryTable();
  static Subheader subheader;
  static String outputDirectory;
  static String unicodeDataDirectory;
  static String localDataDirectory;
  static Renamer renamer;
  static PrintWriter renamingLog;

  public static void main(String[] args) throws Exception {
    //System.out.println(ScriptCategories.ARCHAIC);
    
    if (args[0].equals("g")) {
      generateHangulDefectives();
      return;
    }
    if (args.length < 1) {
      throw new IllegalArgumentException("First argument must be output directory");
    }
    outputDirectory = new File(args[0]).getCanonicalPath() + File.separator;
    System.out.println("Output Directory: " + outputDirectory);

    if (args.length < 2) {
      throw new IllegalArgumentException("Second argument must be Unicode data directory");
    }
    unicodeDataDirectory = new File(args[1]).getCanonicalPath() + File.separator;
    System.out.println("Unicode Data Directory: " + unicodeDataDirectory);
    
    if (args.length < 3) {
      throw new IllegalArgumentException("Third argument must be local data directory");
    }
    localDataDirectory = new File(args[2]).getCanonicalPath() + File.separator;
    System.out.println("Local Data Directory: " + localDataDirectory);
    
    renamingLog = getFileWriter("renamingLog.txt");
    
    renamer = new Renamer(localDataDirectory + "GeneratePickerData.txt");
    /*
     * NamesList-5.1.0d8.txt
/Users/markdavis/Documents/workspace/DATA/UCD/5.1.0-Update/Unihan.txt
     */

    if (DEBUG) System.out.println("Whitespace? " + new UnicodeSet("[:z:]").equals(new UnicodeSet("[:whitespace:]")));

    buildMainTable(args);
    writeMainFile(args);
    renamingLog.close();
  }

  private static void buildMainTable(String[] args) throws IOException {
    subheader = new Subheader(unicodeDataDirectory, outputDirectory);
    CATEGORYTABLE.addMainCategory("Symbol");
    CATEGORYTABLE.addMainCategory("Punctuation");
    CATEGORYTABLE.addMainCategory("Number");
    CATEGORYTABLE.addMainCategory("Invisibles");
    CATEGORYTABLE.addMainCategory("Modifier");
    CATEGORYTABLE.addMainCategory("Latin");
    CATEGORYTABLE.addMainCategory(EUROPEAN);
    CATEGORYTABLE.addMainCategory(AMERICAN);
    CATEGORYTABLE.addMainCategory(AFRICAN);
    CATEGORYTABLE.addMainCategory(MIDDLE_EASTERN);
    CATEGORYTABLE.addMainCategory(SOUTH_ASIAN);
    CATEGORYTABLE.addMainCategory(SOUTHEAST_ASIAN);
    CATEGORYTABLE.addMainCategory("Hangul");
    CATEGORYTABLE.addMainCategory(EAST_ASIAN);
    
    // for testing
    //addHan(unicodeDataDirectory + "Unihan.txt");


    addSymbols();
    CATEGORYTABLE.add("Symbol", true, "Superscript", buttonComparator, new UnicodeSet("[:dt=super:]"));
    CATEGORYTABLE.add("Symbol", true, "Subscript", buttonComparator, new UnicodeSet("[:dt=sub:]"));

    addProperty("General_Category", "Category", buttonComparator, 
            new UnicodeSet("[[\\u0000-\\U0010FFFF]-[:letter:]-[:default_ignorable_code_point:]-[:cf:]-[:whitespace:]-[:So:]" +
            "-[[:M:]-[:script=common:]-[:script=inherited:]]]"), true);

    CATEGORYTABLE.add("Invisibles", true, "Whitespace", null, new UnicodeSet("[:whitespace:]"));
    CATEGORYTABLE.add("Invisibles", true, "Format", null, new UnicodeSet("[:cf:]"));
    CATEGORYTABLE.add("Invisibles", true, "Other", null, new UnicodeSet("[[:default_ignorable_code_point:]-[:cf:]-[:whitespace:]]"));

    addLatin();
    addProperty("Script", EUROPEAN, buttonComparator, new UnicodeSet(ScriptCategories.EUROPEAN).removeAll(new UnicodeSet("[:script=latin:]")), true);
    addProperty("Script", AFRICAN, buttonComparator, ScriptCategories.AFRICAN, true);
    addProperty("Script", MIDDLE_EASTERN, buttonComparator, ScriptCategories.MIDDLE_EASTERN, true);
    addProperty("Script", SOUTH_ASIAN, buttonComparator, ScriptCategories.SOUTH_ASIAN, true);
    addProperty("Script", SOUTHEAST_ASIAN, buttonComparator, ScriptCategories.SOUTHEAST_ASIAN, true);
    addHangul();
    addHan();
    addProperty("Script", EAST_ASIAN, buttonComparator, ScriptCategories.EAST_ASIAN, true);
    addProperty("Script", AMERICAN, buttonComparator, ScriptCategories.AMERICAN, true);
    addProperty("Script", "Other Letters", buttonComparator, ScriptCategories.OTHER_SCRIPTS, true);

    // special overrides
    CATEGORYTABLE.removeAll(new UnicodeSet("[\u0CF1\u0CF2\uFDFD]"));
    CATEGORYTABLE.add(SOUTH_ASIAN, true, "Kannada", buttonComparator, new UnicodeSet("[\u0CF1\u0CF2]"));
    CATEGORYTABLE.add(MIDDLE_EASTERN, true, "Arabic", buttonComparator, new UnicodeSet("[\uFDFD]"));
    /*
U+0CF1 ( ೱ ) KANNADA SIGN JIHVAMULIYA
U+0CF2 ( ೲ ) KANNADA SIGN UPADHMANIYA
U+FDFD ( ﷽ ) ARABIC LIGATURE BISMILLAH AR-RAHMAN AR-RAHEEM
     */
  }

  static UnicodeSet LATIN = (UnicodeSet) new UnicodeSet("[:script=Latin:]").freeze();
  static Set<ULocale> SKIP_LOCALES = new HashSet();
  static {
    SKIP_LOCALES.add(new ULocale("kl"));
    SKIP_LOCALES.add(new ULocale("eo"));
  }

  private static void addLatin() {
    //CATEGORYTABLE.add("Latin", true, "All, UCA-Order", ENGLISH, LATIN);
    UnicodeSet exemplars = new UnicodeSet();
    for (ULocale loc : ULocale.getAvailableLocales()) {
      if (SKIP_LOCALES.contains(loc)) {
        continue;
      }
      final UnicodeSet exemplarSet = LocaleData.getExemplarSet(loc, UnicodeSet.CASE);
      if (!LATIN.containsSome(exemplarSet)) {
        continue;
      }
      addAndNoteNew(loc, exemplars, exemplarSet);
      RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(loc);
      //UnicodeSet collationExemplars = c.getTailoredSet();
      //addAndNoteNew(loc, exemplars, collationExemplars);
    }
    exemplars.retainAll(new UnicodeSet("[[:L:][:M:]-[:nfkcqc=n:]]"));
    CATEGORYTABLE.add("Latin", true, "Common", buttonComparator, exemplars);
    CATEGORYTABLE.add("Latin", true, "Phonetics (IPA)", buttonComparator, ScriptCategories.IPA);
    CATEGORYTABLE.add("Latin", true, "Other", null, new UnicodeSet("[:script=Latin:]")
      .removeAll(ScriptCategories.IPA).removeAll(exemplars));
  }

  private static void addAndNoteNew(ULocale title, UnicodeSet toAddTo, final UnicodeSet toAdd) {
    flatten(toAdd);
    if (toAddTo.containsAll(toAdd)) return;
    System.out.println("Adding Common\t" + title.getDisplayName() + "\t" + title.toString() + "\t" + new UnicodeSet(toAdd).removeAll(toAddTo));
    toAddTo.addAll(toAdd);
  }

  private static void writeMainFile(String[] args) throws IOException, FileNotFoundException {
    PrintWriter out = getFileWriter("CharData.java");
    out.println("package com.macchiato.client;");
    out.println("// " + new Date());
    out.println("public class CharData {");
    out.println("static String[][] CHARACTERS_TO_NAME = {");
    out.println(buildNames());
    out.println("  };\r\n" +
    "  static String[][][] CATEGORIES = {");

    out.println(CATEGORYTABLE);
    out.println("  };\r\n" +
    "}");
    out.close();
    System.out.println("DONE");
  }

  static PrintWriter getFileWriter(String filename) throws IOException, FileNotFoundException {
    File f = new File(outputDirectory + File.separator + filename);
    System.out.println("Writing: " + f.getCanonicalFile());
    PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8")));
    return out;
  }

  //  private static void getCasedScripts() {
  //    Set<String> set = new TreeSet();
  //    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:^NFKCquickcheck=N:]-[:script=common:]")); it.next();) {
  //      
  //      String script = UCharacter.getStringPropertyValue(UProperty.SCRIPT, it.codepoint, UProperty.NameChoice.LONG).toString();
  //      set.add(script);
  //    }
  //    System.out.println(set);
  //  }

  private static void addSymbols() {
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[[:So:]&[[:script=common:][:script=inherited:]]" +
    "[[:Letter:]&[:script=common:]]]")); it.next();) {
      if (COMPATIBILITY.contains(it.codepoint)) {
        CATEGORYTABLE.add("Symbol", true, "Compatibility", buttonComparator, it.codepoint, it.codepoint);
        continue;
      }
      String block = UCharacter.getStringPropertyValue(UProperty.BLOCK, it.codepoint, UProperty.NameChoice.LONG).toString();

      CATEGORYTABLE.add("Symbol", true, block, buttonComparator, it.codepoint, it.codepoint);
    }
  }

  private static void generateHangulDefectives() {
    for (int atomic = 0; atomic < 2; ++atomic) {
      for (int modern = 0; modern < 2; ++modern) {
        for (char c : new char [] {'L', 'V', 'T'}) {
          UnicodeSet uset = new UnicodeSet();
          for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:HST=" + c + ":]")); it.next();) {
            if (UCharacter.getName(it.codepoint).contains("FILLER")) continue;
            String s = it.getString();
            String d = MKD.transform(s);
            if (s.equals(d) == (atomic == 1) && MODERN_JAMO.contains(it.codepoint) == (modern == 1)) {
              uset.add(it.codepoint);
            }
          }
          System.out.print(uset.size() + " ");
          System.out.print(modern == 1 ? "Modern" : "Archaic");
          System.out.print(atomic == 1 ? " Atomic " : " Precomposed ");
          System.out.print(c + " Characters ");
          System.out.println(uset.toPattern(false));
        }
      }
    }

    int[] count = new int[10];

    String test1 = "\uAF51";
    String test2 = "\u1100\u1100\u1169\u1161\u11AF\u11A8";
    String test3 = "\u1100\uACE0\u1161\u11AF\u11A8";
    checkEquals(test1, MKC.transform(test2));
    checkEquals(MKD.transform(test1), test2);
    checkEquals(test1, MKC.transform(test3));
    checkEquals(MKD.transform(test3), test2);

    //U+11B1 ( ᆱ ) HANGUL JONGSEONG RIEUL-MIEUM, U+1101 ( ᄁ ) HANGUL CHOSEONG SSANGKIYEOK
    //⇨ U+11D1 ( ᇑ ) HANGUL JONGSEONG RIEUL-MIEUM-KIYEOK, U+1100 ( ᄀ ) HANGUL CHOSEONG KIYEOK
    final String two_two = "\u1121\u1101";
    final String three_one = "\u1122\u1100";
    checkEquals(MKD.transform(two_two), MKD.transform(three_one));
    checkEquals(MKC.transform(two_two), three_one);
    checkPair("\u1121", "\u1101", count);

    System.out.println("testing roundtrip");

    // test roundtrip
    for (UnicodeSetIterator it = new UnicodeSetIterator(all); it.next();) {
      final String a = it.getString();
      String b = MKD.transform(a);
      String c = MKC.transform(b);
      if (!a.equals(c)) {
        throw new IllegalArgumentException();
      }
      //System.out.println(a + " => " + b + " => " + c + (a.equals(c) ? "" : "  XXX"));
    }

    Map<String,String> decomp2comp = new HashMap<String,String>();

    System.out.println("find defectives");
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:script=Hangul:]")); it.next();) {
      final String comp = it.getString();
      String decomp = MKD.transform(comp);
      decomp2comp.put(decomp, comp);
    }
    // now find all examples

    for (String decomp : decomp2comp.keySet()) {
      String comp = decomp2comp.get(decomp);
      if (comp.length() == 1) continue;
      String prefix = comp.substring(0,comp.length()-1);
      if (!decomp2comp.containsKey(prefix)) {
        System.out.println("Defective: " + codeAndName(comp));
        System.out.println("\t" + toHex(prefix, false));
      }
    }

    System.out.println("testing single+all");

    for (UnicodeSetIterator it = new UnicodeSetIterator(single); it.next();) {
      final String a = it.getString();
      System.out.println(a);
      for (UnicodeSetIterator it2 = new UnicodeSetIterator(all); it2.next();) {
        final String b = it2.getString();
        checkPair(a, b, count);
      }
    }
    System.out.println("testing syllable+single");
    for (UnicodeSetIterator it = new UnicodeSetIterator(syllable); it.next();) {
      final String a = it.getString();
      System.out.println(a);
      for (UnicodeSetIterator it2 = new UnicodeSetIterator(single); it2.next();) {
        final String b = it2.getString();
        checkPair(a, b, count);
      }
    }
    for (int i = 0; i < count.length; ++i) {
      if (count[i] == 0) continue;
      System.out.println("length: " + i + " \tcount: " + count[i]);
    }
    System.out.println("DONE");
  }

  private static void checkEquals(String test1, String test2) {
    if (test1.equals(test2)) return;
    throw new IllegalArgumentException(test1 + " != " + test2);
  }

  private static void checkPair(final String a, final String b, int[] count) {
    final String ab = a+b;
    String c = MKC.transform(ab);
    if (!c.equals(ab)) {
      count[c.length()]++;
    }
  }

  public static String codeAndName(String comp) {
    return toHex(comp, false) + "(" + comp + ")" + UCharacter.getExtendedName(comp.codePointAt(0));
  }

  // U+3433 kRSUnicode  9.3
  static Pattern RAD_STROKE = Pattern.compile("U\\+([A-Z0-9]+)\\s+kRSUnicode\\s+(.*)");
  static Pattern RAD_DATA = Pattern.compile("([0-9]{1,3}\\'?)\\.([0-9]{1,2})\\s*");

  private static void addHan() throws IOException {
    String unihanFile = unicodeDataDirectory + "Unihan.txt";
    BufferedReader in = new BufferedReader(new FileReader(unihanFile));
    Matcher rad2 = RAD_STROKE.matcher("");
    Matcher rad = RAD_DATA.matcher("");
//    final boolean test = rad2.reset("U+3433 kRSUnicode  9.3").matches();
//    System.out.println("Test match: " + test);
    Map<String,UnicodeSet> index = new TreeMap<String, UnicodeSet>(ENGLISH);
    UnicodeSet remainder = new UnicodeSet("[:script=Han:]").removeAll(SKIP);
    index.put("Other", remainder);
    Map<String,String> radicalToChar = new TreeMap<String, String>();
    for (int i = 1; i < 215; ++i) {
      final int cp = 0x2F00 + (i-1);
      final int nfkc = Normalizer.normalize(cp, Normalizer.NFKC).codePointAt(0);
      final String cpString = String.valueOf((char)nfkc);
      final String radical = String.valueOf(i);
      radicalToChar.put(radical, cpString);
      mapToUnicodeSetAdd(index, radical, cp);
      mapToUnicodeSetAdd(index, radical, nfkc);
      remainder.remove(cp);
      remainder.remove(nfkc);
    }
    // special case
    mapToUnicodeSetAdd(index, "197'", 0x5364);
    remainder.remove(0x5364);

    while (true) {
      String line = in.readLine();
      if (line == null) break;
      if (rad2.reset(line).matches()) {
        int cp = Integer.parseInt(rad2.group(1), 16);
        String[] items = rad2.group(2).split("\\s");
        for (String item : items) {
          if (!rad.reset(item).matches()) {
            throw new IllegalArgumentException("Bad line: " + line);
          }
          String radical = rad.group(1);
          //          if (radical.startsWith("211")) {
          //            System.out.println(line);
          //          }
          String baseRadical = radical.endsWith("'") ? radical.substring(0, radical.length()-1) : radical;
          mapToUnicodeSetAdd(index, baseRadical, cp);
          remainder.remove(cp);
          if (rad.group(2).equals("0") && radical.endsWith("'")) {
            String radicalString = Normalizer.normalize(cp, Normalizer.NFKC);
            String old = radicalToChar.get(radical);
            if (old == null) {
              radicalToChar.put(radical, radicalString);
            } else if (!radicalString.equals(old)) {
              System.out.println("Duplicate radical: " + line + " with " + radicalString + " and " + old);
            }
          }
        }
      }
    }
    in.close();
    for (String radical : index.keySet()) {
      String mainCat = null;
      if (DEBUG) System.out.println(radical + " => " + radicalToChar.get(radical));
      String radChar = getRadicalName(radicalToChar, radical);
      String subCat = radChar + " Han";
      try {
        String radical2 = radical.endsWith("'") ? radical.substring(0, radical.length() - 1) : radical;
        int x = Integer.parseInt(radical2);
        int base = (x / 20) * 20;
        int top = base + 19;
        mainCat = "CJK (Han) " + getRadicalName(radicalToChar, Math.max(base,1)) + " - " + getRadicalName(radicalToChar, Math.min(top,214));
      } catch (Exception e) {}
      if (mainCat == null) {
        mainCat = "Symbol";
        subCat = "CJK";
      }
      final UnicodeSet values = index.get(radical);
      UnicodeSet normal = new UnicodeSet(values).removeAll(UNCOMMON);
      CATEGORYTABLE.add(mainCat, true, subCat, null, normal);
      values.retainAll(UNCOMMON);
      CATEGORYTABLE.add(mainCat, true, "Compatibility", null, values);
    }

    //    UnicodeSet temp = new UnicodeSet();
    //    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:script=Han:]").removeAll(SKIP)); it.next();) {
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
  }

  private static <T>void mapToUnicodeSetAdd(Map<T, UnicodeSet> map, T key, int cp) {
    UnicodeSet uset = map.get(key);
    if (uset == null) {
      map.put(key, uset = new UnicodeSet());
    }
    uset.add(cp);
  }

  private static String getRadicalName(Map<String, String> radicalToChar, int max) {
    // TODO Auto-generated method stub
    return getRadicalName(radicalToChar, Integer.toString(max));
  }

  private static String getRadicalName(Map<String, String> radicalToChar, String radical) {
    String simpRadical = RADICALS.get(radical + "'");
    return RADICALS.get(radical) + (simpRadical != null ? "/" + simpRadical : ""); // + " " + radical;
  }

  private static void addHangul() {
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:script=Hangul:]").removeAll(SKIP)); it.next();) {
      String str = it.getString();
      if (ScriptCategories.ARCHAIC.contains(it.codepoint)) {
        CATEGORYTABLE.add("Hangul", true, "Archaic Hangul", buttonComparator, it.codepoint, it.codepoint);
        continue;
      }
      String s = MKKD.transform(str);
      int decompCodePoint1 = s.codePointAt(0);
      if (decompCodePoint1 == '(') {
        decompCodePoint1 = s.codePointAt(1);
      }
      if (!HST_L.contains(decompCodePoint1) || it.codepoint == 0x115F || it.codepoint == 0x1160) {
        CATEGORYTABLE.add("Hangul", true, "\u1161 Vowel/Trailing/Filler", buttonComparator, it.codepoint);
        continue;
      }
      if (COMPATIBILITY.contains(it.codepoint)) {
        CATEGORYTABLE.add("Hangul", true, "Compatibility", buttonComparator, it.codepoint);
        continue;
      }
      CATEGORYTABLE.add("Hangul", true, UTF16.valueOf(decompCodePoint1) + " " + UCharacter.getExtendedName(decompCodePoint1), buttonComparator, it.codepoint);
    }
  }

  private static String buildNames() {
    StringBuilder result = new StringBuilder();
    for (UnicodeSetIterator it = new UnicodeSetIterator(NAMED_CHARACTERS); it.next();) {
      result.append("{\"" + it.getString() + "\",\"" + UCharacter.getExtendedName(it.codepoint) + "\"},\r\n");
    }
    return result.toString();
  }

  static class SimplePair {
    private String first;
    private String second;

    public SimplePair(String first, String second) {
      super();
      this.first = first;
      this.second = second;
    }
    public String getFirst() {
      return first;
    }
    public String getSecond() {
      return second;
    }
    @Override
    public int hashCode() {
      return first.hashCode() ^ second.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
      SimplePair other = (SimplePair) obj;
      return other.first.equals(first) && other.second.equals(second);
    }
    @Override
    public String toString() {
      return "{" + first + " : " + second + "}";
    }
  }

  static class CategoryTable {

    private static final String UNCOMMON_OR_VARIANTS = "- Uncommon or Variants -";
    static Map<String, Map<String, GeneratePickerData.USet>> categoryTable =  //new TreeMap<String, Map<String, USet>>(ENGLISH); // 
      new LinkedHashMap<String, Map<String, GeneratePickerData.USet>>();

    public void add(String category, boolean sortSubcategory, String subcategory, Comparator<String> sortValues, UnicodeSet values) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(values); it.next();) {
        add(category, sortSubcategory, subcategory, sortValues, it.codepoint);
      }
    }

    public void add(String category, boolean sortSubcategory, String subcategory, Comparator<String> sortValues, int startCodePoint, int endCodePoint) {
      for (int i = startCodePoint; i <= endCodePoint; ++i) {
        add(category, sortSubcategory, subcategory, sortValues, i);
      }
    }

    public void add(String category, boolean sortSubcategory, String subcategory, Comparator<String> sortValues, int codepoint) {
      //if (ADD_SUBHEAD.contains(codepoint))
      {
        String subhead = subheader.getSubheader(codepoint);
        if (subhead != null && !subhead.equalsIgnoreCase(subcategory)) {
          subcategory = subcategory + MAIN_SUBSUB_SEPARATOR + subhead;
        }
      }
      SimplePair names = renamer.rename(category, subcategory);
      final String mainCategory = names.getFirst();
      final String subCategory = names.getSecond();
      if (UNCOMMON.contains(codepoint)) {
        CATEGORYTABLE.add2(mainCategory, sortSubcategory, ARCHAIC_MARKER + subCategory, null, codepoint);
      } else {
        CATEGORYTABLE.add2(mainCategory, sortSubcategory, subCategory, sortValues, codepoint);
      }
    }

    private void add2(String category, boolean sortSubcategory, String subcategory, Comparator<String> sortValues, int values) {
      GeneratePickerData.USet oldValue = getValues(category, sortSubcategory, subcategory);
      oldValue.sorted = sortValues;
      oldValue.set.add(values);
      oldValue.set.removeAll(SKIP);
    }

    public void remove(String category, String subcategory, UnicodeSet values) {
      GeneratePickerData.USet oldValue = getValues(category, false, subcategory);
      oldValue.set.removeAll(values);
    }

    public void removeAll(UnicodeSet values) {
      for (String category : categoryTable.keySet()) {
        Map<String,GeneratePickerData.USet> sub = categoryTable.get(category);
        for (String subcategory : sub.keySet()) {
          GeneratePickerData.USet valueChars = sub.get(subcategory);
          valueChars.set.removeAll(values);
        }
      }
    }

    //    public void removeAll(int codepoint) {
    //      for (String category : categoryTable.keySet()) {
    //        Map<String,USet> sub = categoryTable.get(category);
    //        for (String subcategory : sub.keySet()) {
    //          USet valueChars = sub.get(subcategory);
    //          valueChars.set.remove(codepoint);
    //        }
    //      }
    //    }

    public void remove(String category, String subcategory, int startCodePoint, int endCodePoint) {
      GeneratePickerData.USet oldValue = getValues(category, false, subcategory);
      oldValue.set.remove(startCodePoint, endCodePoint);
    }

    public Map<String,GeneratePickerData.USet> addMainCategory(String mainCategory) {
      Map<String,GeneratePickerData.USet> sub = categoryTable.get(mainCategory);
      if (sub == null) {
        categoryTable.put(mainCategory, sub = new TreeMap<String,GeneratePickerData.USet>(ENGLISH));
      }
      return sub;
    }
    
    private GeneratePickerData.USet getValues(String category, boolean sortSubcategory, String subcategory) {
      Map<String,GeneratePickerData.USet> sub = addMainCategory(category);
      GeneratePickerData.USet oldValue = sub.get(subcategory);
      if (oldValue == null) {
        System.out.println(category + MAIN_SUB_SEPARATOR + subcategory);
        oldValue = new GeneratePickerData.USet();
        sub.put(subcategory, oldValue);
      }
      return oldValue;
    }

    public String toString() {
      int totalChars = 0, totalCompressed = 0;
      GeneratePickerData.USet soFar = new GeneratePickerData.USet();
      GeneratePickerData.USet duplicates = new GeneratePickerData.USet();
      StringBuilder result = new StringBuilder();
      for (String category : categoryTable.keySet()) {
        result.append("{{\""+ category + "\"},\r\n");
        Map<String,GeneratePickerData.USet> sub = categoryTable.get(category);
        boolean wasArchaic = false;
        for (String subcategory : sub.keySet()) {
          GeneratePickerData.USet valueChars = sub.get(subcategory);
          if (valueChars.set.isEmpty()) {
            continue;
          }
          if (!wasArchaic && subcategory.startsWith(ARCHAIC_MARKER)) {
            wasArchaic = true;
            addResult(result, USet.EMPTY, category,UNCOMMON_OR_VARIANTS);
          }
          String valueCharsString = addResult(result, valueChars, category, subcategory);

          totalChars += valueChars.set.size();
          totalCompressed += valueCharsString.length();
          //          if (valueChars.set.size() > 1000) {
          //            System.out.println("//Big class: " + category + MAIN_SUB_SEPARATOR + subcategory + MAIN_SUBSUB_SEPARATOR + valueChars.set.size());
          //          }
          UnicodeSet dups = new UnicodeSet(soFar.set).retainAll(valueChars.set);
          duplicates.set.addAll(dups);
          soFar.set.addAll(valueChars.set);
        }
        result.append("},\r\n");
      }
      // invert soFar to get missing
      duplicates.set.removeAll(KNOWN_DUPLICATES);
      duplicates.set.clear(); // don't show anymore
      soFar.set.complement().removeAll(SKIP);
      if (soFar.set.size() > 0 || duplicates.set.size() > 0) {
        result.append("{{\""+ "TODO" + "\"},\r\n");
        if (soFar.set.size() > 0) {
          addResult(result, soFar, "TODO", "Missing");
        }
        if (duplicates.set.size() > 0) {
          addResult(result, duplicates, "TODO", "Duplicates");
        }
        result.append("},\r\n");
      }
      // return results
      System.out.println("Total Chars:\t" + totalChars);
      System.out.println("Total Compressed Chars:\t" + totalCompressed);
      return result.toString();
    }

    private String addResult(StringBuilder result, GeneratePickerData.USet valueChars, String category, String subcategory) {
      if (subcategory.startsWith(ARCHAIC_MARKER)) {
        subcategory = subcategory.substring(1);
      }
      final int size = valueChars.set.size();
      String valueCharsString;
      try {
        valueCharsString = valueChars.toString();
      } catch (IllegalArgumentException e) {
        System.out.println("/*" + size + "*/" +
                " "+ category + MAIN_SUB_SEPARATOR + subcategory + "\t" + valueChars.set);
        throw e;
      }
      final int length = valueCharsString.length();
      final String quoteFixedvalueCharsString = valueCharsString.replace("\\", "\\\\").replace("\"", "\\\"");
      result.append("/*" + size + "," + length + "*/" +
              " {\""+ subcategory + "\",\"" + quoteFixedvalueCharsString + "\"},\r\n");
      System.out.println("/*" + size + "," + length + "*/" +
              " "+ category + MAIN_SUB_SEPARATOR + subcategory + "\t" + valueChars.set + ", " + toHex(valueCharsString,true));
      return valueCharsString;
    }

  }

  static {
    if (DEBUG) System.out.println("Skip: " + SKIP);
  }

  private static void addProperty(final String propertyAlias, String title, Comparator<String> sort, UnicodeSet retain, boolean addSubheader) {
    int propEnum = UCharacter.getPropertyEnum(propertyAlias);

    // get all the value strings, sorted
    UnicodeSet valueChars = new UnicodeSet();
    for (int i = UCharacter.getIntPropertyMinValue(propEnum); i <= UCharacter.getIntPropertyMaxValue(propEnum); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.LONG);

      valueChars.clear();
      valueChars.applyPropertyAlias(propertyAlias, valueAlias);
      if (DEBUG) System.out.println(valueAlias + ": " + valueChars.size() + ", " + valueChars);
      valueChars.removeAll(SKIP);
      valueChars.retainAll(retain);
      if (valueChars.size() == 0) continue;

      if (DEBUG) System.out.println("Filtered " + valueAlias + ": " + valueChars.size() + ", " + valueChars);

      for (UnicodeSetIterator it = new UnicodeSetIterator(valueChars); it.next();) {
        CATEGORYTABLE.add(title, true, valueAlias, sortItems(sort, propertyAlias, valueAlias), it.codepoint);
      }
    }
    //result.append("},");
    //System.out.println(result);
  }

  private static Comparator<String> sortItems(Comparator<String> sort, String propertyAlias, String valueAlias) {
    if (valueAlias.equals("Decimal_Number") && propertyAlias.equals("General_Category")) {
      return null;
    }
    if (valueAlias.equals("Latin")) {
      return ENGLISH;
    }
    return sort;
  }

  private static String toHex(String in, boolean javaStyle) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < in.length(); ++i) {
      result.append(toHex(in.charAt(i), javaStyle));
    }
    return result.toString();
  }

  private static String toHex(int j, boolean javaStyle) {
    if (j == '\"') {
      return "\\\"";
    } else if (j == '\\') { 
      return "\\\\";
    } else if (0x20 < j && j < 0x7F) {
      return String.valueOf((char)j);
    }
    final String hexString = Integer.toHexString(j).toUpperCase();
    int gap = 4 - hexString.length();
    if (gap < 0) {
      gap = 0;
    }
    String prefix = javaStyle ? "\\u" : "U+";
    return prefix + "000".substring(0,gap) + hexString;
  }

  static class MultilevelComparator<T> implements Comparator<T> {
    private Comparator<T>[] comparators;

    public MultilevelComparator(Comparator<T>... comparators) {
      this.comparators = comparators;
    }

    public int compare(T arg0, T arg1) {
      for (int i = 0; i < comparators.length; ++i) {
        int result = comparators[i].compare(arg0, arg1);
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }

  }

  static class UnicodeSetInclusionFirst implements Comparator<String> {
    private UnicodeSet included;
    public UnicodeSetInclusionFirst(UnicodeSet included) {
      this.included = included;
    }
    public int compare(String arg0, String arg1) {
      boolean a0 = included.containsAll(arg0);
      boolean a1 = included.containsAll(arg1);
      return a0 == a1 ? 0 : a0 ? -1 : 1;
    }

  }

  static class USet {
    public static final USet EMPTY = new USet();
    Comparator<String> sorted;
    UnicodeSet set = new UnicodeSet();

    public String toString() {
      StringBuilder result = new StringBuilder();
      if (sorted != null) {
        Set<String> set2 = new TreeSet(sorted);
        for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
          set2.add(it.getString());
        }
        //if (DEBUG) System.out.println("Sorted " + value + ": " + valueChars.size() + ", " + valueChars);
        if (set2.isEmpty()) {
          return null;
        }
        // now produce compacted string from a collection
        appendCompacted(result, set2);
      } else {
        for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.nextRange();) {
          appendRange(result, it.codepoint, it.codepointEnd);
        }
      }
      UnicodeSet reversal = getFromCompacted(result.toString());
      if (!reversal.equals(set)) {
        UnicodeSet ab = new UnicodeSet(set).removeAll(reversal);
        UnicodeSet ba = new UnicodeSet(reversal).removeAll(set);
        throw new IllegalArgumentException("Failed; in original but not restored: " + ab + "\r\nIn restored but not original: " + ba);
      }
      return result.toString();
    }

    private void appendCompacted(StringBuilder result, Set<String> set2) {
      int first = -1;
      int last = -1;
      for (String item : set2) {
        int cp = UTF16.charAt(item, 0);
        if (first == -1) {
          first = last = cp;
        } else if (cp == last + 1) {
          last = cp;
        } else {
          appendRange(result, first, last);
          first = last = cp;
        }
      }
      if (first != -1) {
        appendRange(result, first, last);
      }
    }

    private static void appendRange(StringBuilder result, int first, int last) {
      result.append(UTF16.valueOf(first));
      if (first != last) {
        int delta = 0xE000 + last - first;
        if (delta >= 0xF800) {
          throw new IllegalArgumentException("Range too large: " + toHex(first, true) + "-" + toHex(last, true));
        }
        result.appendCodePoint(delta);  
      }
    }

    UnicodeSet getFromCompacted(String in) {
      UnicodeSet result = new UnicodeSet();
      int cp;
      int first = 0;
      for (int i = 0; i < in.length(); i += Character.charCount(cp)) {
        cp = in.codePointAt(i);
        if (0xE000 <= cp && cp < 0xF800) {
          result.add(first+1, first + cp - 0xE000);
        } else {
          result.add(cp);
        }
        first = cp;
      }
      return result;
    }
  }

  /**
   * Modifies Unicode set to flatten the strings. Eg [abc{da}] => [abcd]
   * Returns the set for chaining.
   * @param exemplar1
   * @return
   */
  public static UnicodeSet flatten(UnicodeSet exemplar1) {
    UnicodeSet result = new UnicodeSet();
    boolean gotString = false;
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplar1); it.nextRange();) {
      if (it.codepoint == UnicodeSetIterator.IS_STRING) {
        result.addAll(it.string);
        gotString = true;
      } else {
        result.add(it.codepoint, it.codepointEnd);
      }
    }
    if (gotString) exemplar1.set(result);
    return exemplar1;
  }

  static String MKD_RULES =
    "\u1101 > \u1100\u1100;"+
    "\u1104 > \u1103\u1103;"+
    "\u1108 > \u1107\u1107;"+
    "\u110A > \u1109\u1109;"+
    "\u110D > \u110C\u110C;"+
    "\u1113 > \u1102\u1100;"+
    "\u1114 > \u1102\u1102;"+
    "\u1115 > \u1102\u1103;"+
    "\u1116 > \u1102\u1107;"+
    "\u1117 > \u1103\u1100;"+
    "\u1118 > \u1105\u1102;"+
    "\u1119 > \u1105\u1105;"+
    "\u111A > \u1105\u1112;"+
    "\u111B > \u1105\u110B;"+
    "\u111C > \u1106\u1107;"+
    "\u111D > \u1106\u110B;"+
    "\u111E > \u1107\u1100;"+
    "\u111F > \u1107\u1102;"+
    "\u1120 > \u1107\u1103;"+
    "\u1121 > \u1107\u1109;"+
    "\u1122 > \u1107\u1109\u1100;"+
    "\u1123 > \u1107\u1109\u1103;"+
    "\u1124 > \u1107\u1109\u1107;"+
    "\u1125 > \u1107\u1109\u1109;"+
    "\u1126 > \u1107\u1109\u110C;"+
    "\u1127 > \u1107\u110C;"+
    "\u1128 > \u1107\u110E;"+
    "\u1129 > \u1107\u1110;"+
    "\u112A > \u1107\u1111;"+
    "\u112B > \u1107\u110B;"+
    "\u112C > \u1107\u1107\u110B;"+
    "\u112D > \u1109\u1100;"+
    "\u112E > \u1109\u1102;"+
    "\u112F > \u1109\u1103;"+
    "\u1130 > \u1109\u1105;"+
    "\u1131 > \u1109\u1106;"+
    "\u1132 > \u1109\u1107;"+
    "\u1133 > \u1109\u1107\u1100;"+
    "\u1134 > \u1109\u1109\u1109;"+
    "\u1135 > \u1109\u110B;"+
    "\u1136 > \u1109\u110C;"+
    "\u1137 > \u1109\u110E;"+
    "\u1138 > \u1109\u110F;"+
    "\u1139 > \u1109\u1110;"+
    "\u113A > \u1109\u1111;"+
    "\u113B > \u1109\u1112;"+
    "\u113D > \u113C\u113C;"+
    "\u113F > \u113E\u113E;"+
    "\u1141 > \u110B\u1100;"+
    "\u1142 > \u110B\u1103;"+
    "\u1143 > \u110B\u1106;"+
    "\u1144 > \u110B\u1107;"+
    "\u1145 > \u110B\u1109;"+
    "\u1146 > \u110B\u1140;"+
    "\u1147 > \u110B\u110B;"+
    "\u1148 > \u110B\u110C;"+
    "\u1149 > \u110B\u110E;"+
    "\u114A > \u110B\u1110;"+
    "\u114B > \u110B\u1111;"+
    "\u114D > \u110C\u110B;"+
    "\u114F > \u114E\u114E;"+
    "\u1151 > \u1150\u1150;"+
    "\u1152 > \u110E\u110F;"+
    "\u1153 > \u110E\u1112;"+
    "\u1156 > \u1111\u1107;"+
    "\u1157 > \u1111\u110B;"+
    "\u1158 > \u1112\u1112;"+
    "\u115A > \u1100\u1103;"+
    "\u115B > \u1102\u1109;"+
    "\u115C > \u1102\u110C;"+
    "\u115D > \u1102\u1112;"+
    "\u115E > \u1103\u1105;"+
    "\uA960 > \u1103\u1106;"+
    "\uA961 > \u1103\u1107;"+
    "\uA962 > \u1103\u1109;"+
    "\uA963 > \u1103\u110C;"+
    "\uA964 > \u1105\u1100;"+
    "\uA965 > \u1105\u1100\u1100;"+
    "\uA966 > \u1105\u1103;"+
    "\uA967 > \u1105\u1103\u1103;"+
    "\uA968 > \u1105\u1106;"+
    "\uA969 > \u1105\u1107;"+
    "\uA96A > \u1105\u1107\u1107;"+
    "\uA96B > \u1105\u1107\u110B;"+
    "\uA96C > \u1105\u1109;"+
    "\uA96D > \u1105\u110C;"+
    "\uA96E > \u1105\u110F;"+
    "\uA96F > \u1106\u1100;"+
    "\uA970 > \u1106\u1103;"+
    "\uA971 > \u1106\u1109;"+
    "\uA972 > \u1107\u1109\u1110;"+
    "\uA973 > \u1107\u110F;"+
    "\uA974 > \u1107\u1112;"+
    "\uA975 > \u1109\u1109\u1107;"+
    "\uA976 > \u110B\u1105;"+
    "\uA977 > \u110B\u1112;"+
    "\uA978 > \u110C\u110C\u1112;"+
    "\uA979 > \u1110\u1110;"+
    "\uA97A > \u1111\u1112;"+
    "\uA97B > \u1112\u1109;"+
    "\uA97C > \u1159\u1159;"+
    "\u1162 > \u1161\u1175;"+
    "\u1164 > \u1163\u1175;"+
    "\u1166 > \u1165\u1175;"+
    "\u1168 > \u1167\u1175;"+
    "\u116A > \u1169\u1161;"+
    "\u116B > \u1169\u1161\u1175;"+
    "\u116C > \u1169\u1175;"+
    "\u116F > \u116E\u1165;"+
    "\u1170 > \u116E\u1165\u1175;"+
    "\u1171 > \u116E\u1175;"+
    "\u1174 > \u1173\u1175;"+
    "\u1176 > \u1161\u1169;"+
    "\u1177 > \u1161\u116E;"+
    "\u1178 > \u1163\u1169;"+
    "\u1179 > \u1163\u116D;"+
    "\u117A > \u1165\u1169;"+
    "\u117B > \u1165\u116E;"+
    "\u117C > \u1165\u1173;"+
    "\u117D > \u1167\u1169;"+
    "\u117E > \u1167\u116E;"+
    "\u117F > \u1169\u1165;"+
    "\u1180 > \u1169\u1165\u1175;"+
    "\u1181 > \u1169\u1167\u1175;"+
    "\u1182 > \u1169\u1169;"+
    "\u1183 > \u1169\u116E;"+
    "\u1184 > \u116D\u1163;"+
    "\u1185 > \u116D\u1163\u1175;"+
    "\u1186 > \u116D\u1167;"+
    "\u1187 > \u116D\u1169;"+
    "\u1188 > \u116D\u1175;"+
    "\u1189 > \u116E\u1161;"+
    "\u118A > \u116E\u1161\u1175;"+
    "\u118B > \u116E\u1165\u1173;"+
    "\u118C > \u116E\u1167\u1175;"+
    "\u118D > \u116E\u116E;"+
    "\u118E > \u1172\u1161;"+
    "\u118F > \u1172\u1165;"+
    "\u1190 > \u1172\u1165\u1175;"+
    "\u1191 > \u1172\u1167;"+
    "\u1192 > \u1172\u1167\u1175;"+
    "\u1193 > \u1172\u116E;"+
    "\u1194 > \u1172\u1175;"+
    "\u1195 > \u1173\u116E;"+
    "\u1196 > \u1173\u1173;"+
    "\u1197 > \u1173\u1175\u116E;"+
    "\u1198 > \u1175\u1161;"+
    "\u1199 > \u1175\u1163;"+
    "\u119A > \u1175\u1169;"+
    "\u119B > \u1175\u116E;"+
    "\u119C > \u1175\u1173;"+
    "\u119D > \u1175\u119E;"+
    "\u119F > \u119E\u1165;"+
    "\u11A0 > \u119E\u116E;"+
    "\u11A1 > \u119E\u1175;"+
    "\u11A2 > \u119E\u119E;"+
    "\u11A3 > \u1161\u1173;"+
    "\u11A4 > \u1163\u116E;"+
    "\u11A5 > \u1167\u1163;"+
    "\u11A6 > \u1169\u1163;"+
    "\u11A7 > \u1169\u1163\u1175;"+
    "\uD7B0 > \u1169\u1167;"+
    "\uD7B1 > \u1169\u1169\u1175;"+
    "\uD7B2 > \u116D\u1161;"+
    "\uD7B3 > \u116D\u1161\u1175;"+
    "\uD7B4 > \u116D\u1165;"+
    "\uD7B5 > \u116E\u1167;"+
    "\uD7B6 > \u116E\u1175\u1175;"+
    "\uD7B7 > \u1172\u1161\u1175;"+
    "\uD7B8 > \u1172\u1169;"+
    "\uD7B9 > \u1173\u1161;"+
    "\uD7BA > \u1173\u1165;"+
    "\uD7BB > \u1173\u1165\u1175;"+
    "\uD7BC > \u1173\u1169;"+
    "\uD7BD > \u1175\u1163\u1169;"+
    "\uD7BE > \u1175\u1163\u1175;"+
    "\uD7BF > \u1175\u1167;"+
    "\uD7C0 > \u1175\u1167\u1175;"+
    "\uD7C1 > \u1175\u1169\u1175;"+
    "\uD7C2 > \u1175\u116D;"+
    "\uD7C3 > \u1175\u1172;"+
    "\uD7C4 > \u1175\u1175;"+
    "\uD7C5 > \u119E\u1161;"+
    "\uD7C6 > \u119E\u1165\u1175;"+
    "\u11A9 > \u11A8\u11A8;"+
    "\u11AA > \u11A8\u11BA;"+
    "\u11AC > \u11AB\u11BD;"+
    "\u11AD > \u11AB\u11C2;"+
    "\u11B0 > \u11AF\u11A8;"+
    "\u11B1 > \u11AF\u11B7;"+
    "\u11B2 > \u11AF\u11B8;"+
    "\u11B3 > \u11AF\u11BA;"+
    "\u11B4 > \u11AF\u11C0;"+
    "\u11B5 > \u11AF\u11C1;"+
    "\u11B6 > \u11AF\u11C2;"+
    "\u11B9 > \u11B8\u11BA;"+
    "\u11BB > \u11BA\u11BA;"+
    "\u11C3 > \u11A8\u11AF;"+
    "\u11C4 > \u11A8\u11BA\u11A8;"+
    "\u11C5 > \u11AB\u11A8;"+
    "\u11C6 > \u11AB\u11AE;"+
    "\u11C7 > \u11AB\u11BA;"+
    "\u11C8 > \u11AB\u11EB;"+
    "\u11C9 > \u11AB\u11C0;"+
    "\u11CA > \u11AE\u11A8;"+
    "\u11CB > \u11AE\u11AF;"+
    "\u11CC > \u11AF\u11A8\u11BA;"+
    "\u11CD > \u11AF\u11AB;"+
    "\u11CE > \u11AF\u11AE;"+
    "\u11CF > \u11AF\u11AE\u11C2;"+
    "\u11D0 > \u11AF\u11AF;"+
    "\u11D1 > \u11AF\u11B7\u11A8;"+
    "\u11D2 > \u11AF\u11B7\u11BA;"+
    "\u11D3 > \u11AF\u11B8\u11BA;"+
    "\u11D4 > \u11AF\u11B8\u11C2;"+
    "\u11D5 > \u11AF\u11B8\u11BC;"+
    "\u11D6 > \u11AF\u11BA\u11BA;"+
    "\u11D7 > \u11AF\u11EB;"+
    "\u11D8 > \u11AF\u11BF;"+
    "\u11D9 > \u11AF\u11F9;"+
    "\u11DA > \u11B7\u11A8;"+
    "\u11DB > \u11B7\u11AF;"+
    "\u11DC > \u11B7\u11B8;"+
    "\u11DD > \u11B7\u11BA;"+
    "\u11DE > \u11B7\u11BA\u11BA;"+
    "\u11DF > \u11B7\u11EB;"+
    "\u11E0 > \u11B7\u11BE;"+
    "\u11E1 > \u11B7\u11C2;"+
    "\u11E2 > \u11B7\u11BC;"+
    "\u11E3 > \u11B8\u11AF;"+
    "\u11E4 > \u11B8\u11C1;"+
    "\u11E5 > \u11B8\u11C2;"+
    "\u11E6 > \u11B8\u11BC;"+
    "\u11E7 > \u11BA\u11A8;"+
    "\u11E8 > \u11BA\u11AE;"+
    "\u11E9 > \u11BA\u11AF;"+
    "\u11EA > \u11BA\u11B8;"+
    "\u11EC > \u11BC\u11A8;"+
    "\u11ED > \u11BC\u11A8\u11A8;"+
    "\u11EE > \u11BC\u11BC;"+
    "\u11EF > \u11BC\u11BF;"+
    "\u11F1 > \u11F0\u11BA;"+
    "\u11F2 > \u11F0\u11EB;"+
    "\u11F3 > \u11C1\u11B8;"+
    "\u11F4 > \u11C1\u11BC;"+
    "\u11F5 > \u11C2\u11AB;"+
    "\u11F6 > \u11C2\u11AF;"+
    "\u11F7 > \u11C2\u11B7;"+
    "\u11F8 > \u11C2\u11B8;"+
    "\u11FA > \u11A8\u11AB;"+
    "\u11FB > \u11A8\u11B8;"+
    "\u11FC > \u11A8\u11BE;"+
    "\u11FD > \u11A8\u11BF;"+
    "\u11FE > \u11A8\u11C2;"+
    "\u11FF > \u11AB\u11AB;"+
    "\uD7CB > \u11AB\u11AF;"+
    "\uD7CC > \u11AB\u11BE;"+
    "\uD7CD > \u11AE\u11AE;"+
    "\uD7CE > \u11AE\u11AE\u11B8;"+
    "\uD7CF > \u11AE\u11B8;"+
    "\uD7D0 > \u11AE\u11BA;"+
    "\uD7D1 > \u11AE\u11BA\u11A8;"+
    "\uD7D2 > \u11AE\u11BD;"+
    "\uD7D3 > \u11AE\u11BE;"+
    "\uD7D4 > \u11AE\u11C0;"+
    "\uD7D5 > \u11AF\u11A8\u11A8;"+
    "\uD7D6 > \u11AF\u11A8\u11C2;"+
    "\uD7D7 > \u11AF\u11AF\u11BF;"+
    "\uD7D8 > \u11AF\u11B7\u11C2;"+
    "\uD7D9 > \u11AF\u11B8\u11AE;"+
    "\uD7DA > \u11AF\u11B8\u11C1;"+
    "\uD7DB > \u11AF\u11F0;"+
    "\uD7DC > \u11AF\u11F9\u11C2;"+
    "\uD7DD > \u11AF\u11BC;"+
    "\uD7DE > \u11B7\u11AB;"+
    "\uD7DF > \u11B7\u11AB\u11AB;"+
    "\uD7E0 > \u11B7\u11B7;"+
    "\uD7E1 > \u11B7\u11B8\u11BA;"+
    "\uD7E2 > \u11B7\u11BD;"+
    "\uD7E3 > \u11B8\u11AE;"+
    "\uD7E4 > \u11B8\u11AF\u11C1;"+
    "\uD7E5 > \u11B8\u11B7;"+
    "\uD7E6 > \u11B8\u11B8;"+
    "\uD7E7 > \u11B8\u11BA\u11AE;"+
    "\uD7E8 > \u11B8\u11BD;"+
    "\uD7E9 > \u11B8\u11BE;"+
    "\uD7EA > \u11BA\u11B7;"+
    "\uD7EB > \u11BA\u11B8\u11BC;"+
    "\uD7EC > \u11BA\u11BA\u11A8;"+
    "\uD7ED > \u11BA\u11BA\u11AE;"+
    "\uD7EE > \u11BA\u11EB;"+
    "\uD7EF > \u11BA\u11BD;"+
    "\uD7F0 > \u11BA\u11BE;"+
    "\uD7F1 > \u11BA\u11C0;"+
    "\uD7F2 > \u11BA\u11C2;"+
    "\uD7F3 > \u11EB\u11B8;"+
    "\uD7F4 > \u11EB\u11B8\u11BC;"+
    "\uD7F5 > \u11F0\u11B7;"+
    "\uD7F6 > \u11F0\u11C2;"+
    "\uD7F7 > \u11BD\u11B8;"+
    "\uD7F8 > \u11BD\u11B8\u11B8;"+
    "\uD7F9 > \u11BD\u11BD;"+
    "\uD7FA > \u11C1\u11BA;"+
    "\uD7FB > \u11C1\u11C0;";

  static final String MKC_RULES = //"::MKD;"+
    "\u1107\u1109\u1100 > \u1122;"+
    "\u1107\u1109\u1103 > \u1123;"+
    "\u1107\u1109\u1107 > \u1124;"+
    "\u1107\u1109\u1109 > \u1125;"+
    "\u1107\u1109\u110C > \u1126;"+
    "\u1107\u1107\u110B > \u112C;"+
    "\u1109\u1107\u1100 > \u1133;"+
    "\u1109\u1109\u1109 > \u1134;"+
    "\u1169\u1161\u1175 > \u116B;"+
    "\u116E\u1165\u1175 > \u1170;"+
    "\u1169\u1165\u1175 > \u1180;"+
    "\u1169\u1167\u1175 > \u1181;"+
    "\u116D\u1163\u1175 > \u1185;"+
    "\u116E\u1161\u1175 > \u118A;"+
    "\u116E\u1165\u1173 > \u118B;"+
    "\u116E\u1167\u1175 > \u118C;"+
    "\u1172\u1165\u1175 > \u1190;"+
    "\u1172\u1167\u1175 > \u1192;"+
    "\u1173\u1175\u116E > \u1197;"+
    "\u1169\u1163\u1175 > \u11A7;"+
    "\u11A8\u11BA\u11A8 > \u11C4;"+
    "\u11AF\u11A8\u11BA > \u11CC;"+
    "\u11AF\u11AE\u11C2 > \u11CF;"+
    "\u11AF\u11B7\u11A8 > \u11D1;"+
    "\u11AF\u11B7\u11BA > \u11D2;"+
    "\u11AF\u11B8\u11BA > \u11D3;"+
    "\u11AF\u11B8\u11C2 > \u11D4;"+
    "\u11AF\u11B8\u11BC > \u11D5;"+
    "\u11AF\u11BA\u11BA > \u11D6;"+
    "\u11B7\u11BA\u11BA > \u11DE;"+
    "\u11BC\u11A8\u11A8 > \u11ED;"+
    "\u1105\u1100\u1100 > \uA965;"+
    "\u1105\u1103\u1103 > \uA967;"+
    "\u1105\u1107\u1107 > \uA96A;"+
    "\u1105\u1107\u110B > \uA96B;"+
    "\u1107\u1109\u1110 > \uA972;"+
    "\u1109\u1109\u1107 > \uA975;"+
    "\u110C\u110C\u1112 > \uA978;"+
    "\u1169\u1169\u1175 > \uD7B1;"+
    "\u116D\u1161\u1175 > \uD7B3;"+
    "\u116E\u1175\u1175 > \uD7B6;"+
    "\u1172\u1161\u1175 > \uD7B7;"+
    "\u1173\u1165\u1175 > \uD7BB;"+
    "\u1175\u1163\u1169 > \uD7BD;"+
    "\u1175\u1163\u1175 > \uD7BE;"+
    "\u1175\u1167\u1175 > \uD7C0;"+
    "\u1175\u1169\u1175 > \uD7C1;"+
    "\u119E\u1165\u1175 > \uD7C6;"+
    "\u11AE\u11AE\u11B8 > \uD7CE;"+
    "\u11AE\u11BA\u11A8 > \uD7D1;"+
    "\u11AF\u11A8\u11A8 > \uD7D5;"+
    "\u11AF\u11A8\u11C2 > \uD7D6;"+
    "\u11AF\u11AF\u11BF > \uD7D7;"+
    "\u11AF\u11B7\u11C2 > \uD7D8;"+
    "\u11AF\u11B8\u11AE > \uD7D9;"+
    "\u11AF\u11B8\u11C1 > \uD7DA;"+
    "\u11AF\u11F9\u11C2 > \uD7DC;"+
    "\u11B7\u11AB\u11AB > \uD7DF;"+
    "\u11B7\u11B8\u11BA > \uD7E1;"+
    "\u11B8\u11AF\u11C1 > \uD7E4;"+
    "\u11B8\u11BA\u11AE > \uD7E7;"+
    "\u11BA\u11B8\u11BC > \uD7EB;"+
    "\u11BA\u11BA\u11A8 > \uD7EC;"+
    "\u11BA\u11BA\u11AE > \uD7ED;"+
    "\u11EB\u11B8\u11BC > \uD7F4;"+
    "\u11BD\u11B8\u11B8 > \uD7F8;"+
    "\u1100\u1100 > \u1101;"+
    "\u1103\u1103 > \u1104;"+
    "\u1107\u1107 > \u1108;"+
    "\u1109\u1109 > \u110A;"+
    "\u110C\u110C > \u110D;"+
    "\u1102\u1100 > \u1113;"+
    "\u1102\u1102 > \u1114;"+
    "\u1102\u1103 > \u1115;"+
    "\u1102\u1107 > \u1116;"+
    "\u1103\u1100 > \u1117;"+
    "\u1105\u1102 > \u1118;"+
    "\u1105\u1105 > \u1119;"+
    "\u1105\u1112 > \u111A;"+
    "\u1105\u110B > \u111B;"+
    "\u1106\u1107 > \u111C;"+
    "\u1106\u110B > \u111D;"+
    "\u1107\u1100 > \u111E;"+
    "\u1107\u1102 > \u111F;"+
    "\u1107\u1103 > \u1120;"+
    "\u1107\u1109 > \u1121;"+
    "\u1107\u110C > \u1127;"+
    "\u1107\u110E > \u1128;"+
    "\u1107\u1110 > \u1129;"+
    "\u1107\u1111 > \u112A;"+
    "\u1107\u110B > \u112B;"+
    "\u1109\u1100 > \u112D;"+
    "\u1109\u1102 > \u112E;"+
    "\u1109\u1103 > \u112F;"+
    "\u1109\u1105 > \u1130;"+
    "\u1109\u1106 > \u1131;"+
    "\u1109\u1107 > \u1132;"+
    "\u1109\u110B > \u1135;"+
    "\u1109\u110C > \u1136;"+
    "\u1109\u110E > \u1137;"+
    "\u1109\u110F > \u1138;"+
    "\u1109\u1110 > \u1139;"+
    "\u1109\u1111 > \u113A;"+
    "\u1109\u1112 > \u113B;"+
    "\u113C\u113C > \u113D;"+
    "\u113E\u113E > \u113F;"+
    "\u110B\u1100 > \u1141;"+
    "\u110B\u1103 > \u1142;"+
    "\u110B\u1106 > \u1143;"+
    "\u110B\u1107 > \u1144;"+
    "\u110B\u1109 > \u1145;"+
    "\u110B\u1140 > \u1146;"+
    "\u110B\u110B > \u1147;"+
    "\u110B\u110C > \u1148;"+
    "\u110B\u110E > \u1149;"+
    "\u110B\u1110 > \u114A;"+
    "\u110B\u1111 > \u114B;"+
    "\u110C\u110B > \u114D;"+
    "\u114E\u114E > \u114F;"+
    "\u1150\u1150 > \u1151;"+
    "\u110E\u110F > \u1152;"+
    "\u110E\u1112 > \u1153;"+
    "\u1111\u1107 > \u1156;"+
    "\u1111\u110B > \u1157;"+
    "\u1112\u1112 > \u1158;"+
    "\u1100\u1103 > \u115A;"+
    "\u1102\u1109 > \u115B;"+
    "\u1102\u110C > \u115C;"+
    "\u1102\u1112 > \u115D;"+
    "\u1103\u1105 > \u115E;"+
    "\u1161\u1175 > \u1162;"+
    "\u1163\u1175 > \u1164;"+
    "\u1165\u1175 > \u1166;"+
    "\u1167\u1175 > \u1168;"+
    "\u1169\u1161 > \u116A;"+
    "\u1169\u1175 > \u116C;"+
    "\u116E\u1165 > \u116F;"+
    "\u116E\u1175 > \u1171;"+
    "\u1173\u1175 > \u1174;"+
    "\u1161\u1169 > \u1176;"+
    "\u1161\u116E > \u1177;"+
    "\u1163\u1169 > \u1178;"+
    "\u1163\u116D > \u1179;"+
    "\u1165\u1169 > \u117A;"+
    "\u1165\u116E > \u117B;"+
    "\u1165\u1173 > \u117C;"+
    "\u1167\u1169 > \u117D;"+
    "\u1167\u116E > \u117E;"+
    "\u1169\u1165 > \u117F;"+
    "\u1169\u1169 > \u1182;"+
    "\u1169\u116E > \u1183;"+
    "\u116D\u1163 > \u1184;"+
    "\u116D\u1167 > \u1186;"+
    "\u116D\u1169 > \u1187;"+
    "\u116D\u1175 > \u1188;"+
    "\u116E\u1161 > \u1189;"+
    "\u116E\u116E > \u118D;"+
    "\u1172\u1161 > \u118E;"+
    "\u1172\u1165 > \u118F;"+
    "\u1172\u1167 > \u1191;"+
    "\u1172\u116E > \u1193;"+
    "\u1172\u1175 > \u1194;"+
    "\u1173\u116E > \u1195;"+
    "\u1173\u1173 > \u1196;"+
    "\u1175\u1161 > \u1198;"+
    "\u1175\u1163 > \u1199;"+
    "\u1175\u1169 > \u119A;"+
    "\u1175\u116E > \u119B;"+
    "\u1175\u1173 > \u119C;"+
    "\u1175\u119E > \u119D;"+
    "\u119E\u1165 > \u119F;"+
    "\u119E\u116E > \u11A0;"+
    "\u119E\u1175 > \u11A1;"+
    "\u119E\u119E > \u11A2;"+
    "\u1161\u1173 > \u11A3;"+
    "\u1163\u116E > \u11A4;"+
    "\u1167\u1163 > \u11A5;"+
    "\u1169\u1163 > \u11A6;"+
    "\u11A8\u11A8 > \u11A9;"+
    "\u11A8\u11BA > \u11AA;"+
    "\u11AB\u11BD > \u11AC;"+
    "\u11AB\u11C2 > \u11AD;"+
    "\u11AF\u11A8 > \u11B0;"+
    "\u11AF\u11B7 > \u11B1;"+
    "\u11AF\u11B8 > \u11B2;"+
    "\u11AF\u11BA > \u11B3;"+
    "\u11AF\u11C0 > \u11B4;"+
    "\u11AF\u11C1 > \u11B5;"+
    "\u11AF\u11C2 > \u11B6;"+
    "\u11B8\u11BA > \u11B9;"+
    "\u11BA\u11BA > \u11BB;"+
    "\u11A8\u11AF > \u11C3;"+
    "\u11AB\u11A8 > \u11C5;"+
    "\u11AB\u11AE > \u11C6;"+
    "\u11AB\u11BA > \u11C7;"+
    "\u11AB\u11EB > \u11C8;"+
    "\u11AB\u11C0 > \u11C9;"+
    "\u11AE\u11A8 > \u11CA;"+
    "\u11AE\u11AF > \u11CB;"+
    "\u11AF\u11AB > \u11CD;"+
    "\u11AF\u11AE > \u11CE;"+
    "\u11AF\u11AF > \u11D0;"+
    "\u11AF\u11EB > \u11D7;"+
    "\u11AF\u11BF > \u11D8;"+
    "\u11AF\u11F9 > \u11D9;"+
    "\u11B7\u11A8 > \u11DA;"+
    "\u11B7\u11AF > \u11DB;"+
    "\u11B7\u11B8 > \u11DC;"+
    "\u11B7\u11BA > \u11DD;"+
    "\u11B7\u11EB > \u11DF;"+
    "\u11B7\u11BE > \u11E0;"+
    "\u11B7\u11C2 > \u11E1;"+
    "\u11B7\u11BC > \u11E2;"+
    "\u11B8\u11AF > \u11E3;"+
    "\u11B8\u11C1 > \u11E4;"+
    "\u11B8\u11C2 > \u11E5;"+
    "\u11B8\u11BC > \u11E6;"+
    "\u11BA\u11A8 > \u11E7;"+
    "\u11BA\u11AE > \u11E8;"+
    "\u11BA\u11AF > \u11E9;"+
    "\u11BA\u11B8 > \u11EA;"+
    "\u11BC\u11A8 > \u11EC;"+
    "\u11BC\u11BC > \u11EE;"+
    "\u11BC\u11BF > \u11EF;"+
    "\u11F0\u11BA > \u11F1;"+
    "\u11F0\u11EB > \u11F2;"+
    "\u11C1\u11B8 > \u11F3;"+
    "\u11C1\u11BC > \u11F4;"+
    "\u11C2\u11AB > \u11F5;"+
    "\u11C2\u11AF > \u11F6;"+
    "\u11C2\u11B7 > \u11F7;"+
    "\u11C2\u11B8 > \u11F8;"+
    "\u11A8\u11AB > \u11FA;"+
    "\u11A8\u11B8 > \u11FB;"+
    "\u11A8\u11BE > \u11FC;"+
    "\u11A8\u11BF > \u11FD;"+
    "\u11A8\u11C2 > \u11FE;"+
    "\u11AB\u11AB > \u11FF;"+
    "\u1103\u1106 > \uA960;"+
    "\u1103\u1107 > \uA961;"+
    "\u1103\u1109 > \uA962;"+
    "\u1103\u110C > \uA963;"+
    "\u1105\u1100 > \uA964;"+
    "\u1105\u1103 > \uA966;"+
    "\u1105\u1106 > \uA968;"+
    "\u1105\u1107 > \uA969;"+
    "\u1105\u1109 > \uA96C;"+
    "\u1105\u110C > \uA96D;"+
    "\u1105\u110F > \uA96E;"+
    "\u1106\u1100 > \uA96F;"+
    "\u1106\u1103 > \uA970;"+
    "\u1106\u1109 > \uA971;"+
    "\u1107\u110F > \uA973;"+
    "\u1107\u1112 > \uA974;"+
    "\u110B\u1105 > \uA976;"+
    "\u110B\u1112 > \uA977;"+
    "\u1110\u1110 > \uA979;"+
    "\u1111\u1112 > \uA97A;"+
    "\u1112\u1109 > \uA97B;"+
    "\u1159\u1159 > \uA97C;"+
    "\u1169\u1167 > \uD7B0;"+
    "\u116D\u1161 > \uD7B2;"+
    "\u116D\u1165 > \uD7B4;"+
    "\u116E\u1167 > \uD7B5;"+
    "\u1172\u1169 > \uD7B8;"+
    "\u1173\u1161 > \uD7B9;"+
    "\u1173\u1165 > \uD7BA;"+
    "\u1173\u1169 > \uD7BC;"+
    "\u1175\u1167 > \uD7BF;"+
    "\u1175\u116D > \uD7C2;"+
    "\u1175\u1172 > \uD7C3;"+
    "\u1175\u1175 > \uD7C4;"+
    "\u119E\u1161 > \uD7C5;"+
    "\u11AB\u11AF > \uD7CB;"+
    "\u11AB\u11BE > \uD7CC;"+
    "\u11AE\u11AE > \uD7CD;"+
    "\u11AE\u11B8 > \uD7CF;"+
    "\u11AE\u11BA > \uD7D0;"+
    "\u11AE\u11BD > \uD7D2;"+
    "\u11AE\u11BE > \uD7D3;"+
    "\u11AE\u11C0 > \uD7D4;"+
    "\u11AF\u11F0 > \uD7DB;"+
    "\u11AF\u11BC > \uD7DD;"+
    "\u11B7\u11AB > \uD7DE;"+
    "\u11B7\u11B7 > \uD7E0;"+
    "\u11B7\u11BD > \uD7E2;"+
    "\u11B8\u11AE > \uD7E3;"+
    "\u11B8\u11B7 > \uD7E5;"+
    "\u11B8\u11B8 > \uD7E6;"+
    "\u11B8\u11BD > \uD7E8;"+
    "\u11B8\u11BE > \uD7E9;"+
    "\u11BA\u11B7 > \uD7EA;"+
    "\u11BA\u11EB > \uD7EE;"+
    "\u11BA\u11BD > \uD7EF;"+
    "\u11BA\u11BE > \uD7F0;"+
    "\u11BA\u11C0 > \uD7F1;"+
    "\u11BA\u11C2 > \uD7F2;"+
    "\u11EB\u11B8 > \uD7F3;"+
    "\u11F0\u11B7 > \uD7F5;"+
    "\u11F0\u11C2 > \uD7F6;"+
    "\u11BD\u11B8 > \uD7F7;"+
    "\u11BD\u11BD > \uD7F9;"+
    "\u11C1\u11BA > \uD7FA;"+
    "\u11C1\u11C0 > \uD7FB;";

  static final Transliterator MKD = Transliterator.createFromRules("MKD", 
          "::NFD;"+ MKD_RULES, 
          Transliterator.FORWARD);
  static final Transliterator MKKD = Transliterator.createFromRules("MKD", 
          "::NFKD;"+ MKD_RULES, 
          Transliterator.FORWARD);
  static final Transliterator MKC = Transliterator.createFromRules("MKC", 
          "::NFD;"+ MKD_RULES + "::null;" + MKC_RULES + "::NFC;", 
          Transliterator.FORWARD);

  static final Map<String, String> RADICALS = new TreeMap();
  static {
    String[][] data = {
            {"1", "\u4E00"},
            {"2", "\u4E28"},
            {"3", "\u4E36"},
            {"4", "\u4E3F"},
            {"5", "\u4E59"},
            {"6", "\u4E85"},
            {"7", "\u4E8C"},
            {"8", "\u4EA0"},
            {"9", "\u4EBA"},
            {"10", "\u513F"},
            {"11", "\u5165"},
            {"12", "\u516B"},
            {"13", "\u5182"},
            {"14", "\u5196"},
            {"15", "\u51AB"},
            {"16", "\u51E0"},
            {"17", "\u51F5"},
            {"18", "\u5200"},
            {"19", "\u529B"},
            {"20", "\u52F9"},
            {"21", "\u5315"},
            {"22", "\u531A"},
            {"23", "\u5338"},
            {"24", "\u5341"},
            {"25", "\u535C"},
            {"26", "\u5369"},
            {"27", "\u5382"},
            {"28", "\u53B6"},
            {"29", "\u53C8"},
            {"30", "\u53E3"},
            {"31", "\u56D7"},
            {"32", "\u571F"},
            {"33", "\u58EB"},
            {"34", "\u5902"},
            {"35", "\u590A"},
            {"36", "\u5915"},
            {"37", "\u5927"},
            {"38", "\u5973"},
            {"39", "\u5B50"},
            {"40", "\u5B80"},
            {"41", "\u5BF8"},
            {"42", "\u5C0F"},
            {"43", "\u5C22"},
            {"44", "\u5C38"},
            {"45", "\u5C6E"},
            {"46", "\u5C71"},
            {"47", "\u5DDB"},
            {"48", "\u5DE5"},
            {"49", "\u5DF1"},
            {"50", "\u5DFE"},
            {"51", "\u5E72"},
            {"52", "\u5E7A"},
            {"53", "\u5E7F"},
            {"54", "\u5EF4"},
            {"55", "\u5EFE"},
            {"56", "\u5F0B"},
            {"57", "\u5F13"},
            {"58", "\u5F50"},
            {"59", "\u5F61"},
            {"60", "\u5F73"},
            {"61", "\u5FC3"},
            {"62", "\u6208"},
            {"63", "\u6236"},
            {"64", "\u624B"},
            {"65", "\u652F"},
            {"66", "\u6534"},
            {"67", "\u6587"},
            {"68", "\u6597"},
            {"69", "\u65A4"},
            {"70", "\u65B9"},
            {"71", "\u65E0"},
            {"72", "\u65E5"},
            {"73", "\u66F0"},
            {"74", "\u6708"},
            {"75", "\u6728"},
            {"76", "\u6B20"},
            {"77", "\u6B62"},
            {"78", "\u6B79"},
            {"79", "\u6BB3"},
            {"80", "\u6BCB"},
            {"81", "\u6BD4"},
            {"82", "\u6BDB"},
            {"83", "\u6C0F"},
            {"84", "\u6C14"},
            {"85", "\u6C34"},
            {"86", "\u706B"},
            {"87", "\u722A"},
            {"88", "\u7236"},
            {"89", "\u723B"},
            {"90", "\u723F"},
            {"91", "\u7247"},
            {"92", "\u7259"},
            {"93", "\u725B"},
            {"94", "\u72AC"},
            {"95", "\u7384"},
            {"96", "\u7389"},
            {"97", "\u74DC"},
            {"98", "\u74E6"},
            {"99", "\u7518"},
            {"100", "\u751F"},
            {"101", "\u7528"},
            {"102", "\u7530"},
            {"103", "\u758B"},
            {"104", "\u7592"},
            {"105", "\u7676"},
            {"106", "\u767D"},
            {"107", "\u76AE"},
            {"108", "\u76BF"},
            {"109", "\u76EE"},
            {"110", "\u77DB"},
            {"111", "\u77E2"},
            {"112", "\u77F3"},
            {"113", "\u793A"},
            {"114", "\u79B8"},
            {"115", "\u79BE"},
            {"116", "\u7A74"},
            {"117", "\u7ACB"},
            {"118", "\u7AF9"},
            {"119", "\u7C73"},
            {"120", "\u7CF8"},
            {"120'", "\u7E9F"},
            {"121", "\u7F36"},
            {"122", "\u7F51"},
            {"123", "\u7F8A"},
            {"124", "\u7FBD"},
            {"125", "\u8001"},
            {"126", "\u800C"},
            {"127", "\u8012"},
            {"128", "\u8033"},
            {"129", "\u807F"},
            {"130", "\u8089"},
            {"131", "\u81E3"},
            {"132", "\u81EA"},
            {"133", "\u81F3"},
            {"134", "\u81FC"},
            {"135", "\u820C"},
            {"136", "\u821B"},
            {"137", "\u821F"},
            {"138", "\u826E"},
            {"139", "\u8272"},
            {"140", "\u8278"},
            {"141", "\u864D"},
            {"142", "\u866B"},
            {"143", "\u8840"},
            {"144", "\u884C"},
            {"145", "\u8863"},
            {"146", "\u897E"},
            {"147", "\u898B"},
            {"147'", "\u89C1"},
            {"148", "\u89D2"},
            {"149", "\u8A00"},
            {"149'", "\u8BA0"},
            {"150", "\u8C37"},
            {"151", "\u8C46"},
            {"152", "\u8C55"},
            {"153", "\u8C78"},
            {"154", "\u8C9D"},
            {"154'", "\u8D1D"},
            {"155", "\u8D64"},
            {"156", "\u8D70"},
            {"157", "\u8DB3"},
            {"158", "\u8EAB"},
            {"159", "\u8ECA"},
            {"159'", "\u8F66"},
            {"160", "\u8F9B"},
            {"161", "\u8FB0"},
            {"162", "\u8FB5"},
            {"163", "\u9091"},
            {"164", "\u9149"},
            {"165", "\u91C6"},
            {"166", "\u91CC"},
            {"167", "\u91D1"},
            {"167'", "\u9485"},
            {"168", "\u9577"},
            {"168'", "\u957F"},
            {"169", "\u9580"},
            {"169'", "\u95E8"},
            {"170", "\u961C"},
            {"171", "\u96B6"},
            {"172", "\u96B9"},
            {"173", "\u96E8"},
            {"174", "\u9751"},
            {"175", "\u975E"},
            {"176", "\u9762"},
            {"177", "\u9769"},
            {"178", "\u97CB"},
            {"178'", "\u97E6"},
            {"179", "\u97ED"},
            {"180", "\u97F3"},
            {"181", "\u9801"},
            {"181'", "\u9875"},
            {"182", "\u98A8"},
            {"182'", "\u98CE"},
            {"183", "\u98DB"},
            {"183'", "\u98DE"},
            {"184", "\u98DF"},
            {"184'", "\u9963"},
            {"185", "\u9996"},
            {"186", "\u9999"},
            {"187", "\u99AC"},
            {"187'", "\u9A6C"},
            {"188", "\u9AA8"},
            {"189", "\u9AD8"},
            {"190", "\u9ADF"},
            {"191", "\u9B25"},
            {"192", "\u9B2F"},
            {"193", "\u9B32"},
            {"194", "\u9B3C"},
            {"195", "\u9B5A"},
            {"195'", "\u9C7C"},
            {"196", "\u9CE5"},
            {"196'", "\u9E1F"},
            {"197", "\u9E75"},
            {"197'", "\u5364"},
            {"198", "\u9E7F"},
            {"199", "\u9EA5"},
            {"199'", "\u9EA6"},
            {"200", "\u9EBB"},
            {"201", "\u9EC3"},
            {"202", "\u9ECD"},
            {"203", "\u9ED1"},
            {"204", "\u9EF9"},
            {"205", "\u9EFD"},
            {"205'", "\u9EFE"},
            {"206", "\u9F0E"},
            {"207", "\u9F13"},
            {"208", "\u9F20"},
            {"209", "\u9F3B"},
            {"210", "\u9F4A"},
            {"211", "\u9F52"},
            {"211'", "\u9F7F"},
            {"212", "\u9F8D"},
            {"212'", "\u9F99"},
            {"213", "\u9F9C"},
            {"213'", "\u9F9F"},
            {"214", "\u9FA0"}};
    for (String[] pair : data) {
      RADICALS.put(pair[0], pair[1]);
    }
  }

  //  static final String MKDP_RULES =
  //    MKD_RULES + 
  //  "\uE000 > \u1169\u1167;" +
  //  "\uE001 > \u116E\u1167;";

  //  static final String MKCP_RULES =
  //    MKC_RULES +
  //  "\u1169\u1167 > \uE000;" +
  //  "\u116E\u1167 > \uE001;";
  //  
  //  static final Transliterator MKDP = Transliterator.createFromRules("MKDP", 
  //          "::NFD;"+ MKDP_RULES, 
  //          Transliterator.FORWARD);
  //  static final Transliterator MKCP = Transliterator.createFromRules("MKCP", 
  //          "::NFD;"+ MKDP_RULES + "::null;" + MKCP_RULES + "::NFC;", 
  //          Transliterator.FORWARD);

  static Pattern IS_ARCHAIC = Pattern.compile("(Obsolete|Ancient|Archaic|Medieval|New Testament|\\bUPA\\b)", Pattern.CASE_INSENSITIVE);

  public static final UnicodeSet ADD_SUBHEAD = (UnicodeSet) new UnicodeSet("[[:S:][:P:][:M:]&[[:script=common:][:script=inherited:]]-[:nfkdqc=n:]]")
  .removeAll(ScriptCategories.ARCHAIC).freeze();

  static class Renamer {
    Map<Matcher,String> renameTable = new LinkedHashMap();

    public Renamer(String filename) throws IOException {
      getRenameData(filename);
    }

    void getRenameData(String filename) throws IOException {
      BufferedReader in = new BufferedReader(new FileReader(filename));
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        int pos = line.indexOf('#');
        if (pos >= 0) {
          line = line.substring(0,pos);
        }
        line = line.trim();
        if (line.length() == 0) {
          continue;
        }
        try {
          int breaker = line.indexOf(">");
          String source = line.substring(0,breaker).trim();
          String target = line.substring(breaker+1).trim();
          renameTable.put(Pattern.compile(source,Pattern.CASE_INSENSITIVE).matcher(""), target);
        } catch (Exception e) {
          throw (RuntimeException) new IllegalArgumentException("Problem with: " + line).initCause(e);
        }
      }
      in.close();

    }
    //  static final String[] RENAME_TABLE = {
    //    ".*Category:([^ ]*)[ ](.*) - (.*)>$2:$1 - $3",
    //    ".*Category:(.*) - (.*)>$1:$2",
    //    ".*Category:([^ ]*)[ ](.*)>$2:$1",
    //    ".*Category:(.*)>$1:Miscellaneous",
    //    "Symbol:Latin 1 Supplement - Latin-1 punctuation and symbols > Symbol:Latin-1 punctuation and symbols",
    //    "Mark:(.*) > General Diacritic:$1",
    //    "Symbol:(.*) - (.*(arrows|harpoons).*) > Arrows:$2",
    //    "Symbol:Control Pictures.*>Symbol:Control Pictures",
    //    "Symbol:(Box Drawing|Block Elements|Geometric Shapes|Miscellaneous Symbols And Arrows).*>Symbol:Geometric Shapes",
    //    "Symbol:(.*) Tiles.*>Symbol:Tiles and Dominoes",
    //    "Symbol:.*Musical.*>Symbol:Musical Symbols",
    //    "Symbol:Tai Xuan Jing Symbols.*>Symbol:Tai Xuan Jing Symbols",
    //    "Symbol:Halfwidth And Fullwidth Forms.*>Symbol:Halfwidth And Fullwidth Forms",
    //    "Punctuation:(Open|Close|Initial|Final)(.*)>General Punctuation:Paired",
    //    "Punctuation:(Dash|Connector) - (.*)>General Punctuation:$1",
    //    "Punctuation:Other - (.*)>General Punctuation:$1",
    //    "Punctuation:(.*)>General Punctuation:$1",
    //    "Symbol:.*yijing.*>Symbol:Yijing",
    //    "Symbol:Optical Character Recognition - OCR>Symbol:Miscellaneous Technical - OCR",
    //    "Symbol:.*(CJK|Ideographic|Kanbun).*>Symbol:CJK",
    //    "Symbol:.*(weather|astrologic).*>Symbol:Weather and astrological symbols",
    //    "Symbol:.*(keyboard|\\bGUI\\b).*>Symbol:Keyboard and UI symbols",
    //    "Symbol:Letterlike Symbols.*>Symbol:Letterlike Symbols",
    //    "Symbol:Modifier>General Diacritic:Modifier Symbols",
    //    //"Symbol:Miscellaneous Symbols And Arrows(.*)>Symbol:Miscellaneous Symbols$1",
    //    "Symbol:Miscellaneous Symbols - (.*)>Symbol:$1",
    //    "Symbol:Technical Symbols - (.*)>Symbol:$1",
    //    "Symbol:(Currency|Math|Dingbats|Miscellaneous Technical) - (.*)>Symbol:$1",
    //    "Symbol:Modifier - (.*)>General Diacritic:Symbol - $1",
    //    "General Diacritic:(Spacing|Enclosing) - (.*)>General Diacritic:$1",
    //    "Invisibles:(Other) - (.*)>Invisibles:$1",
    //  };
    //  /*
    //   *     if (maincategory.contains("Category")) {
    //      int pos = subcategory.indexOf(' ');
    //      if (pos < 0) {
    //        subcategory = "Misc " + subcategory;
    //        pos = subcategory.indexOf(' ');
    //      }
    //      maincategory = subcategory.substring(pos+1);
    //      subcategory = subcategory.substring(0,pos);
    //    }
    //   */
    //  static {
    //    renameTable = new LinkedHashMap();
    //    for (String row : RENAME_TABLE) {
    //      try {
    //        int breaker = row.indexOf(">");
    //        String source = row.substring(0,breaker).trim();
    //        String target = row.substring(breaker+1).trim();
    //        renameTable.put(Pattern.compile(source,Pattern.CASE_INSENSITIVE).matcher(""), target);
    //      } catch (Exception e) {
    //        throw (RuntimeException) new IllegalArgumentException("Problem with: " + row).initCause(e);
    //      }
    //    }
    //  }

    Map<SimplePair,SimplePair> renameCache = new HashMap();

    SimplePair rename(String maincategory, String subcategory) {
      final SimplePair originals = new SimplePair(maincategory, subcategory);
      SimplePair cached = renameCache.get(originals);
      if (cached != null) return cached;

      maincategory = maincategory.replace('_', ' ');
      subcategory = subcategory.replace('_', ' ');

      String lookup = maincategory + MAIN_SUB_SEPARATOR + subcategory;
      String indent = "";
      for (Matcher m : renameTable.keySet()) {
        if (m.reset(lookup).matches()) {
          String newName = renameTable.get(m);
          for (int i = 0; i <= m.groupCount(); ++i) {
            newName = newName.replace("$" + i, m.group(i));
          }
          renamingLog.println(indent + lookup + "\t=>\t" + newName);
          lookup = newName;
          indent += "\t";
        }
      }
      int pos = lookup.indexOf(':');
      maincategory = lookup.substring(0,pos);
      subcategory = lookup.substring(pos+1);

      SimplePair newGuys = new SimplePair(maincategory, subcategory);
      if (newGuys.equals(originals)) {
        newGuys = originals; // save extra object
      }
      renameCache.put(originals, newGuys);
      return newGuys;
    }

  }
}
