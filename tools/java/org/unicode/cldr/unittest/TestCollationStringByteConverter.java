/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import org.unicode.cldr.util.ByteString;
import org.unicode.cldr.util.CharList;
import org.unicode.cldr.util.CollationStringByteConverter;
import org.unicode.cldr.util.Dictionary;
import org.unicode.cldr.util.LenientDateParser;
import org.unicode.cldr.util.StateDictionaryBuilder;
import org.unicode.cldr.util.StringUtf8Converter;
import org.unicode.cldr.util.TestStateDictionaryBuilder;
import org.unicode.cldr.util.CharUtilities.CharListWrapper;
import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.DictionaryCharList;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;
import org.unicode.cldr.util.LenientDateParser.Parser;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.util.ULocale;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TestCollationStringByteConverter {
  
  
//  static interface PartCharSequence {
//    /**
//     * Replace index < x.length() with hasCharAt(index)
//     * @param index
//     * @return
//     */
//    boolean hasCharAt(int index);
//    char charAt(int index);
//    public PartCharSequence subSequence2(int start, int end);
//    /**
//     * Returns a subsequence going to the end.
//     * @param start
//     * @return
//     */
//    public PartCharSequence subSequence2(int start);
//    /**
//     * Return the length known so far. If hasCharAt(getKnownLength()) == false, then it is the real length.
//     * @return
//     */
//    public int getKnownLength();
//}
//
//static class PartCharSequenceWrapper implements PartCharSequence {
//    CharSequence source;
//
//    public boolean equals(Object anObject) {
//        return source.equals(anObject);
//    }
//
//    public int hashCode() {
//        return source.hashCode();
//    }
//
//    public PartCharSequenceWrapper(CharSequence source) {
//        this.source = source;
//    }
//
//    public char charAt(int index) {
//        return source.charAt(index);
//    }
//
//    public PartCharSequence subSequence2(int beginIndex, int endIndex) {
//        return new PartCharSequenceWrapper(source.subSequence(beginIndex, endIndex));
//    }
//
//    public PartCharSequence subSequence2(int beginIndex) {
//        return new PartCharSequenceWrapper(source.subSequence(beginIndex, source.length()));
//    }
//
//    /* (non-Javadoc)
//     * @see com.ibm.icu.text.RuleBasedCollator.PartCharSequence#hasCharAt(int)
//     */
//    public boolean hasCharAt(int index) {
//        return index < source.length();
//    }
//    /* (non-Javadoc)
//     * @see com.ibm.icu.text.RuleBasedCollator.PartCharSequence#getKnownLength()
//     */
//    public int getKnownLength() {
//        return source.length();
//    }
//}

  
 
  public static void main(String[] args) throws Exception {
    check2();
    if (true) return;
    checkBasic();
    check();
  }

  private static void checkBasic() {
    System.out.println(ULocale.getDisplayName("en_GB", "en"));
    
    DictionaryBuilder<CharSequence> builder = new StateDictionaryBuilder<CharSequence>();
    Map map = new TreeMap(Dictionary.CHAR_SEQUENCE_COMPARATOR);
    map.put("a", "ABC");
    map.put("bc", "B"); // ß
    Dictionary<CharSequence> dict = builder.make(map);
    String[] tests = { "a/bc", "bc", "a", "d", "", "abca"};
    for (String test : tests) {
      System.out.println("TRYING: " + test);
      DictionaryCharList gcs = new DictionaryCharList(dict, test);
      for (int i = 0; gcs.hasCharAt(i); ++i) {
        char c = gcs.charAt(i);
        final int sourceOffset = gcs.sourceOffset(i);
        final CharSequence sourceSubSequence = gcs.sourceSubSequence(i, i+1);
        System.out.println(i + "\t" + c  + "\t" + sourceOffset + "\t" + sourceSubSequence);
      }
      gcs.hasCharAt(Integer.MAX_VALUE);
      System.out.println("Length: " + gcs.getKnownLength());
    }
  }
  
  private static void check2() {
    Map<CharSequence,CharSequence> map = new TreeMap<CharSequence,CharSequence>();
    map.put("j", "J");
    map.put("july", "JULY");
    map.put("june", "JUNE"); // ß
    map.put("august", "AUGUST"); // ß
    DictionaryBuilder<CharSequence> builder = new StateDictionaryBuilder<CharSequence>();
    Dictionary<CharSequence> dict = builder.make(map);
    System.out.println(map);
    System.out.println(dict);
    Matcher m = dict.getMatcher();
    System.out.println(m.setText("a").next(Filter.LONGEST_UNIQUE) + "\t" + m + "\t" + m.nextUniquePartial());
    System.out.println(m.setText("j").next(Filter.LONGEST_UNIQUE) + "\t" + m + "\t" + m.nextUniquePartial());
    System.out.println(m.setText("ju").next(Filter.LONGEST_UNIQUE) + "\t" + m + "\t" + m.nextUniquePartial());
    System.out.println(m.setText("jul").next(Filter.LONGEST_UNIQUE) + "\t" + m + "\t" + m.nextUniquePartial());


    LenientDateParser ldp = LenientDateParser.getInstance(ULocale.FRENCH);
    Parser parser = ldp.getParser();
    System.out.println(parser.debugShow());
    String[] tests = {"jan 12, 1963 1942 au"};
    ParsePosition parsePosition = new ParsePosition(0);
    for (String test : tests) {
      parsePosition.setIndex(0);
      Date d = parser.parse(test, parsePosition);
      System.out.println(test + "\t=>\t" + d + "\t" + show(test,parsePosition) + "\t" + parser);
    }
  }

  private static String show(String test, ParsePosition parsePosition) {
    return "{" + test.substring(0,parsePosition.getIndex()) + "|" + test.substring(parsePosition.getIndex())
    + (parsePosition.getErrorIndex() == -1 ? "" : "/" + parsePosition.getErrorIndex())  + "}";
  }

  public static void check() throws Exception {
    final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
    col.setStrength(col.PRIMARY);
    col.setAlternateHandlingShifted(true);
    CollationStringByteConverter converter = new CollationStringByteConverter(col, new StringUtf8Converter()); // new ByteString(true)
    Matcher<String> matcher = converter.getDictionary().getMatcher();
//    if (true) {
//      Iterator<Entry<CharSequence, String>> x = converter.getDictionary().getMapping();
//      while (x.hasNext()) {
//        Entry<CharSequence, String> entry = x.next();
//        System.out.println(entry.getKey() + "\t" + Utility.hex(entry.getKey().toString())+ "\t\t" + entry.getValue() + "\t" + Utility.hex(entry.getValue().toString()));
//      }
//      System.out.println(converter.getDictionary().debugShow());
//    }
    String[] tests = {"ab", "abc", "ss", "ß", "Abcde", "Once Upon AB Time", "\u00E0b", "A\u0300b"};
    byte[] output = new byte[1000];
    for (String test : tests) {
      DictionaryCharList<String> dcl = new DictionaryCharList(converter.getDictionary(), test);
      String result = matcher.setText(new DictionaryCharList(converter.getDictionary(), test)).convert(new StringBuffer()).toString();
      System.out.println(test + "\t=>\t" + result);
     int len = converter.toBytes(test, output, 0);
     String result2 = converter.fromBytes(output, 0, len, new StringBuilder()).toString();
     System.out.println(test + "\t(bytes) =>\t" + result2);
      for (int i = 0; i < len; ++i) {
        System.out.print(Utility.hex(output[i]&0xFF, 2) + " ");
      }
      System.out.println();
      RuleBasedCollator c;
    }
    
    DictionaryBuilder<String> builder = new StateDictionaryBuilder<String>(); // .setByteConverter(converter);
    Map map = new TreeMap(Dictionary.CHAR_SEQUENCE_COMPARATOR);
    map.put("ab", "found-ab");
    map.put("abc", "found-ab");
    map.put("ss", "found-ss"); // ß
    Dictionary<String> dict = builder.make(map);
    final String string = "Abcde and ab c Upon aß AB basS Time\u00E0bA\u0300b";
    DictionaryCharList x = new DictionaryCharList(converter.getDictionary(),  string);
    x.hasCharAt(Integer.MAX_VALUE); // force growth
    System.out.println("Internal: " + x.sourceSubSequence(0, x.getKnownLength()));
    
    TestStateDictionaryBuilder.tryFind(string, new DictionaryCharList(converter.getDictionary(),  string), dict, Filter.ALL);
    
    TestStateDictionaryBuilder.tryFind(string, new DictionaryCharList(converter.getDictionary(),  string), dict, Filter.LONGEST_MATCH);
    
  }
}