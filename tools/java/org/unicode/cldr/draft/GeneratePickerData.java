package org.unicode.cldr.draft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class GeneratePickerData {
  private static final boolean DEBUG = false;

  private static final UnicodeSet SKIP = (UnicodeSet) new UnicodeSet("[[:cn:][:cs:][:co:][:cc:][:deprecated:][\\U00010000-\\U0010FFFF]]").freeze();

  private static final UnicodeSet NAMED_CHARACTERS = (UnicodeSet) new UnicodeSet(
  "[[:Z:][:default_ignorable_code_point:][:Pd:][:cf:]]").removeAll(SKIP).freeze();
  private static final UnicodeSet WHITESPACE = (UnicodeSet) new UnicodeSet("[:whitespace:]").freeze();

  private static final UnicodeSet ARCHAIC = (UnicodeSet) new UnicodeSet("[[:script=Bugi:][:script=Buhd:][:script=Cari:][:script=Copt:]" +
          "[:script=Cprt:][:script=Dsrt:][:script=Glag:][:script=Goth:][:script=Hano:][:script=Ital:][:script=Khar:][:script=Linb:]" +
          "[:script=Lyci:][:script=Lydi:][:script=Ogam:][:script=Osma:][:script=Phag:][:script=Phnx:][:script=Rjng:][:script=Runr:]" +
  "[:script=Shaw:][:script=Sund:][:script=Sylo:][:script=Syrc:][:script=Tagb:][:script=Tglg:][:script=Ugar:][:script=Xpeo:][:script=Xsux:]]").freeze();

  private static final UnicodeSet EUROPEAN = (UnicodeSet) new UnicodeSet("[[:script=Latin:][:script=Greek:][:script=Coptic:][:script=Cyrillic:]" +
  "[:script=Glag:][:script=Armenian:][:script=Georgian:][:script=Shavian:][:script=braille:][:ogham:][:runic:]]").freeze(); 
  private static final UnicodeSet MIDDLE_EASTERN = (UnicodeSet) new UnicodeSet(
  "[[:script=Hebrew:][:script=Arabic:][:script=Syriac:][:script=Thaana:]]").freeze(); 
  private static final UnicodeSet SOUTH_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Devanagari:][:script=Bengali:][:script=Gurmukhi:][:script=Gujarati:]" +
          "[:script=Oriya:][:script=Tamil:][:script=Telugu:][:script=Kannada:][:script=Malayalam:]" +
  "[:script=Sinhala:][:script=Tibetan:][:script=Phags-Pa:][:script=Limbu:][:script=Sylo:][:script=Kharoshthi:][:script=lepcha:][:saurashtra:]]").freeze(); 
  private static final UnicodeSet SOUTHEAST_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Thai:][:script=Lao:][:script=Myanmar:][:script=Khmer:]" +
          "[:script=Tai_Le:][:script=New Tai Lue:][:script=Tagalog:][:script=Hanunoo:][:script=Buhid:]" +
  "[:script=Tagbanwa:][:script=Buginese:][:script=Balinese:][:script=Cham:][:script=kayah li:][:script=ol chiki:][:script=rejang:][:script=sundanese:]]").freeze(); 
  private static final UnicodeSet EAST_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Bopomofo:][:script=Hiragana:][:script=Katakana:][:script=Mongolian:]" +
  "[:script=Yi:]]").freeze(); 
  private static final UnicodeSet AFRICAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Ethiopic:][:script=Osmanya:][:script=Tifinagh:]" +
  "[:script=Nko:][:script=vai:]]").freeze();
  private static final UnicodeSet AMERICAN = (UnicodeSet) new UnicodeSet(
  "[[:script=Cherokee:][:script=CANS:][:script=Deseret:]]").freeze();

  private static final UnicodeSet OTHER_SCRIPTS = (UnicodeSet) new UnicodeSet("[^[:script=common:][:script=inherited:]]")
  .removeAll(EUROPEAN)
  .removeAll(MIDDLE_EASTERN)
  .removeAll(SOUTH_ASIAN)
  .removeAll(SOUTHEAST_ASIAN)
  .removeAll(EAST_ASIAN)
  .removeAll(AFRICAN)
  .removeAll(AMERICAN)
  .removeAll(new UnicodeSet("[[:script=han:][:script=hangul:]]"))
  .freeze();

  static PrintWriter out;

  static Comparator<String> ENGLISH = Collator.getInstance(Locale.ENGLISH);
  static Comparator<String> c = new MultilevelComparator<String>(
          new UnicodeSetInclusionFirst(new UnicodeSet("[:ascii:]")),
          new UnicodeSetInclusionFirst(new UnicodeSet("[[:Letter:]&[:^NFKD_QuickCheck=N:]]")),
          new UnicodeSetInclusionFirst(new UnicodeSet("[:Letter:]")),
          //          new Comparator<String>() {
          //            public int compare(final String o1, final String o2) {
          //              final boolean nfkc1 = Normalizer.isNormalized(o1, Normalizer.NFKC, 0);
          //              final boolean nfkc2 = Normalizer.isNormalized(o2, Normalizer.NFKC, 0);
          //              return nfkc1 == nfkc2 ? 0 : nfkc1 ? -1 : 1;
          //            }
          //          },
          ENGLISH,
          new UTF16.StringComparator()
  );

  static Set<String> set2 = new TreeSet(c);
  static UnicodeSet valueChars = new UnicodeSet();
  static CategoryTable CATEGORYTABLE = new CategoryTable();

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      throw new IllegalArgumentException("Need output file name.");
    }
    File f = new File(args[0]);
    System.out.println("Writing: " + f.getCanonicalFile());

    out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8")));
    out.println("package com.macchiato.client;");
    out.println("// " + new Date());
    out.println("public class CharData {");
    out.println("static String[][] CHARACTERS_TO_NAME = {");
    out.println(buildNames());
    out.println("  };\r\n" +
    "  static String[][][] CATEGORIES = {");

    addProperty("Block", "Symbol", true, new UnicodeSet("[^[:So:]&[[:script=common:][:script=inherited:]]]"), SimpleRename);

    addProperty("General_Category", "Category", true, 
            new UnicodeSet("[[:letter:][:default_ignorable_code_point:][:cf:][:whitespace:][:So:]]"), InvertingRename);

    CATEGORYTABLE.add("Invisibles", true, "Whitespace", false, new UnicodeSet("[:whitespace:]"));
    CATEGORYTABLE.add("Invisibles", true, "Format", false, new UnicodeSet("[:cf:]"));
    CATEGORYTABLE.add("Invisibles", true, "Other", false, new UnicodeSet("[[:default_ignorable_code_point:]-[:cf:]-[:whitespace:]]"));

    addProperty("Script", "European", true, new UnicodeSet(EUROPEAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "African", true, new UnicodeSet(AFRICAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Middle Eastern", true, new UnicodeSet(MIDDLE_EASTERN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "South Asian", true, new UnicodeSet(SOUTH_ASIAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Southeast Asian", true, new UnicodeSet(SOUTHEAST_ASIAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "East Asian", true, new UnicodeSet(EAST_ASIAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addHangul();
    addHan();
    addProperty("Script", "American", true, new UnicodeSet(AMERICAN).removeAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Other Letters", true, new UnicodeSet(OTHER_SCRIPTS).removeAll(ARCHAIC).complement(), SimpleRename);

    addProperty("Script", "Archaic European", true, new UnicodeSet(EUROPEAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic African", true, new UnicodeSet(AFRICAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic Middle Eastern", true, new UnicodeSet(MIDDLE_EASTERN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic South Asian", true, new UnicodeSet(SOUTH_ASIAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic Southeast Asian", true, new UnicodeSet(SOUTHEAST_ASIAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic East Asian", true, new UnicodeSet(EAST_ASIAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic American", true, new UnicodeSet(AMERICAN).retainAll(ARCHAIC).complement(), SimpleRename);
    addProperty("Script", "Archaic Other Letters", true, new UnicodeSet(OTHER_SCRIPTS).retainAll(ARCHAIC).complement(), SimpleRename);


    out.println(CATEGORYTABLE);
    out.println("  };\r\n" +
    "}");
    out.close();
    System.out.println("DONE");
  }

  private static void addHan() {
    UnicodeSet temp = new UnicodeSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:script=Han:]").removeAll(SKIP)); it.next();) {
      temp.add(it.codepoint);
      if (temp.size() >= 1000) {
        int code = temp.charAt(0);
        CATEGORYTABLE.add("Han (CJK)", true, UTF16.valueOf(code) + " Han " + toHex(code, false), true, temp);
        temp.clear();
      }
    }
    if (temp.size() > 0) {
      int code = temp.charAt(0);
      CATEGORYTABLE.add("Han (CJK)", true, UTF16.valueOf(code) + " Han " + toHex(code, false), true, temp);
    }
  }

  private static void addHangul() {
    UnicodeSet temp = new UnicodeSet();
    Transliterator Hangul = Transliterator.createFromRules("hangul", HANGUL_RULES, Transliterator.FORWARD);
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[:script=Hangul:]").removeAll(SKIP)); it.next();) {
      String str = it.getString();
      String s = Hangul.transform(str);
      int cp = s.codePointAt(0);
      if (cp == '(') {
        cp = s.codePointAt(1);
      }
      temp.clear();
      temp.add(it.codepoint);
      CATEGORYTABLE.add("Hangul", true, UTF16.valueOf(cp) + " " + UCharacter.getExtendedName(cp), true, temp);
    }
  }

  private static String buildNames() {
    StringBuilder result = new StringBuilder();
    for (UnicodeSetIterator it = new UnicodeSetIterator(NAMED_CHARACTERS); it.next();) {
      result.append("{\"" + it.getString() + "\",\"" + UCharacter.getExtendedName(it.codepoint) + "\"},");
    }
    return result.toString();
  }

  static class CategoryTable {
    static class USet {
      boolean sorted;
      UnicodeSet set = new UnicodeSet();

      public String toString() {
        StringBuilder result = new StringBuilder();
        if (sorted) {
          set2.clear();
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
        return result.toString();
      }

    }

    static Map<String, Map<String, USet>> categoryTable =  //new TreeMap<String, Map<String, USet>>(ENGLISH); // 
      new LinkedHashMap<String, Map<String, USet>>();

    public void add(String category, boolean sortSubcategory, String subcategory, boolean sortValues, UnicodeSet values) {
      Map<String,USet> sub = categoryTable.get(category);
      if (sub == null) {
        sub = sortSubcategory ? new TreeMap<String,USet>(ENGLISH) : new LinkedHashMap<String,USet>();
        categoryTable.put(category, sub);
      }
      USet oldValue = sub.get(subcategory);
      if (oldValue == null) {
        oldValue = new USet();
        sub.put(subcategory, oldValue);
      }
      oldValue.sorted = sortValues;
      oldValue.set.addAll(values);
      oldValue.set.removeAll(SKIP);
    }

    public String toString() {
      USet soFar = new USet();
      USet duplicates = new USet();
      StringBuilder result = new StringBuilder();
      for (String category : categoryTable.keySet()) {
        result.append("{{\""+ category + "\"},\r\n");
        Map<String,USet> sub = categoryTable.get(category);
        for (String subcategory : sub.keySet()) {
          USet valueChars = sub.get(subcategory);
          result.append("/*" + valueChars.set.size() + "*/ {\""+ subcategory + "\",\"" + valueChars + "\"},\r\n");
          if (valueChars.set.size() > 1000) {
            System.out.println("//Big class: " + category + ":" + subcategory + " - " + valueChars.set.size());
          }
          UnicodeSet dups = new UnicodeSet(soFar.set).retainAll(valueChars.set);
          duplicates.set.addAll(dups);
          soFar.set.addAll(valueChars.set);
        }
        result.append("},\r\n");
      }
      // invert soFar to get missing
      soFar.set.complement().removeAll(SKIP);
      if (soFar.set.size() > 0 || duplicates.set.size() > 0) {
        result.append("{{\""+ "TODO" + "\"},\r\n");
        if (soFar.set.size() > 0) {
          result.append(" {\""+ "Missing" + "\",\"" + soFar + "\"},\r\n");
        }
        if (duplicates.set.size() > 0) {
          result.append(" {\""+ "Duplicates" + "\",\"" + duplicates + "\"},\r\n");
        }
        result.append("},\r\n");
      }
      // return results
      return result.toString();
    }
  }

  static {
    if (DEBUG) System.out.println("Skip: " + SKIP);
  }

  static class Rename {
    String[] rename(String property, String value) {
      return new String[] {property.replace('_', ' '), value.replace('_', ' ')};
    }
  }

  static Rename SimpleRename = new Rename();

  static Rename InvertingRename = new Rename() {
    String[] rename(String property, String original) {
      int pos = original.indexOf('_');
      if (pos < 0) {
        original = "Misc_" + original;
        pos = original.indexOf('_');
      }
      original = original.replace('_', ' '); 
      return new String[] {original.substring(pos+1), original.substring(0,pos)};
    }
  };

  private static void addProperty(final String propertyAlias, String title, boolean sort, UnicodeSet removals, Rename rename) {
    int propEnum = UCharacter.getPropertyEnum(propertyAlias);

    // get all the value strings, sorted
    for (int i = UCharacter.getIntPropertyMinValue(propEnum); i <= UCharacter.getIntPropertyMaxValue(propEnum); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.LONG);

      valueChars.clear();
      valueChars.applyPropertyAlias(propertyAlias, valueAlias);
      if (DEBUG) System.out.println(valueAlias + ": " + valueChars.size() + ", " + valueChars);
      valueChars.removeAll(SKIP);
      valueChars.removeAll(removals);
      if (valueChars.size() == 0) continue;

      if (DEBUG) System.out.println("Filtered " + valueAlias + ": " + valueChars.size() + ", " + valueChars);

      String[] names = rename.rename(title, valueAlias);

      CATEGORYTABLE.add(names[0], true, names[1], sortItems(sort,propertyAlias, valueAlias), valueChars);
    }
    //result.append("},");
    //System.out.println(result);
  }



  private static boolean sortItems(boolean sort, String propertyAlias, String valueAlias) {
    if (valueAlias.equals("Decimal_Number") && propertyAlias.equals("General_Category")) {
      return false;
    }
    return sort;
  }

  private static void appendCompacted(StringBuilder result, Set<String> set2) {
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

  static int rangeCount = 0;
  static final int LIMIT = 100;
  private static void appendRange(StringBuilder result, int first, int last) {
    appendRange2(result, first, last);
    //    int maxLast = LIMIT - 1 - rangeCount + first;
    //    if (last > maxLast) {
    //      appendRange2(result, first, maxLast);
    //      result.append("\", \"");
    //      rangeCount = 0;
    //      appendRange2(result, maxLast+1, last);
    //    } else {
    //      appendRange2(result, first, last);
    //    }
  }

  private static void appendRange2(StringBuilder result, int first, int last) {
    rangeCount += last - first + 1;
    if (first == last) {
      result.append(toHex(first, true));
    } else if (first + 1 == last) {
      result.append(toHex(first, true));
      result.append(toHex(last, true));
    } else {
      //result.append('\u0000');
      result.append(toHex(first, true));
      result.append('-');
      result.append(toHex(last, true));        
    }
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

  static String HANGUL_RULES = "::NFKD;" +
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
}
