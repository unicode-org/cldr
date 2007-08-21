/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CollationStringByteConverter;
import org.unicode.cldr.util.Dictionary;
import org.unicode.cldr.util.StateDictionaryBuilder;
import org.unicode.cldr.util.StringUtf8Converter;
import org.unicode.cldr.util.TestStateDictionaryBuilder;
import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.util.ULocale;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TestCollationStringByteConverter {
  
  /**
   * Implements a CharSequence that is logically based on another source, which
   * may require incremental building. You should avoid calling length(), since
   * that will grow the buffer internally.
   * 
   * @author markdavis
   * 
   */
  public static interface CharList extends CharSequence {
    public boolean hasCharAt(int index);

    public char charAt(int index);

    public int sourceOffset(int index);

    public CharSequence subSequence(int start, int end);
  }
  
  /**
   * Simple wrapper for CharSequence
   * @author markdavis
   *
   */
  public static class CharListWrapper <T extends CharSequence> implements CharList {
    protected T source;
    
    public CharListWrapper(T source) {
      this.source = source;
    }
    public boolean hasCharAt(int index) {
      return index < source.length();
    }
    public char charAt(int index) {
      return source.charAt(index);
    }
    public int sourceOffset(int index) {
      return index;
    }
    public CharSequence subSequence(int start, int end) {
      return source.subSequence(start, end);
    }
    public CharSequence sourceSubSequence(int start, int end) {
      return source.subSequence(start, end);
    }
    public int length() {
      return source.length();
    }
  }
  
  static class DictionaryCharList<T extends CharSequence> extends CharListWrapper<T> {
    protected boolean failOnLength = false;
    protected StringBuilder buffer = new StringBuilder();
    protected int[] sourceOffsets;
    protected Matcher<T> matcher;
    protected boolean atEnd;
    
    public DictionaryCharList(Dictionary<T> dictionary, T source) {
      super(source);
      matcher = dictionary.getMatcher().setText(source);
      atEnd = source.length() == 0;
      sourceOffsets = new int[source.length()];
    }
    
    public int length() {
      if (!atEnd) {
        if (failOnLength) {
          throw new IllegalArgumentException("Shouldn't be calling length");
        }
        growToOffset(Integer.MAX_VALUE);
      }
      return buffer.length();
    }
    
    public boolean hasCharAt(int index) {
      if (index >= buffer.length()) {
        if (atEnd) {
          return false;
        }
        growToOffset(index + 1);
        return index < buffer.length();
      }
      return true;
    }
    
    public char charAt(int index) {
      if (index >= buffer.length()) {
        if (!atEnd) {
          growToOffset(index + 1);
        }
      }
      return buffer.charAt(index);
    }
    
    // sourceOffsets contains valid entries up to buffer.length() + 1.
    private void growToOffset(int offset) {
      int length = buffer.length();
      while (length < offset && !atEnd) {
        Status status = matcher.next(Filter.LONGEST_MATCH);
        int currentOffset = matcher.getOffset();
        final int matchEnd = matcher.getMatchEnd();
        if (status == Status.MATCH) {
          final T replacement = matcher.getMatchValue();
          setOffsets(length + 1, replacement.length(), matchEnd);
          buffer.append(replacement);
          length = buffer.length();
          matcher.setOffset(matchEnd);
        } else {
          setOffsets(length + 1, 1, currentOffset + 1);
          buffer.append(source.charAt(currentOffset));
          length = buffer.length();
          matcher.nextOffset();
        }
        atEnd = matcher.getOffset() >= source.length();
      }
    }

    private void setOffsets(final int start, final int count, final int value) {
      final int length = start + count;
      if (sourceOffsets.length < length) {
        int newCapacity = sourceOffsets.length * 2 + 1;
        if (newCapacity < length + 50) {
          newCapacity = length + 50;
        }
        int[] temp = new int[newCapacity];
        System.arraycopy(sourceOffsets, 0, temp, 0, sourceOffsets.length);
        sourceOffsets = temp;
      }
      for (int i = start; i < length; ++i) {
        sourceOffsets[i] = value;
      }
    }
    
    public int sourceOffset(int offset) {
      if (offset > buffer.length()) {
        growToOffset(offset);
        if (offset > buffer.length()) {
          throw new ArrayIndexOutOfBoundsException(offset);
        }
      }
      return sourceOffsets[offset];
    }
    
    public CharSequence subSequence(int start, int end) {
      return buffer.subSequence(start, end);
    }

    public CharSequence sourceSubSequence(int start, int end) {
      // TODO Auto-generated method stub
      return source.subSequence(sourceOffset(start), sourceOffset(end));
    }
  }
  
  public static void main(String[] args) throws Exception {
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
      System.out.println("Length: " + gcs.length());
    }
    check();
  }
  
  public static void check() throws Exception {
    final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
    col.setStrength(col.PRIMARY);
    CollationStringByteConverter converter = new CollationStringByteConverter(col, new StringUtf8Converter());
    Matcher<String> matcher = converter.getDictionary().getMatcher();
//    if (true) {
//      Iterator<Entry<CharSequence, String>> x = converter.getDictionary().getMapping();
//      while (x.hasNext()) {
//        Entry<CharSequence, String> entry = x.next();
//        System.out.println(entry.getKey() + "\t" + Utility.hex(entry.getKey().toString())+ "\t\t" + entry.getValue() + "\t" + Utility.hex(entry.getValue().toString()));
//      }
//      System.out.println(converter.getDictionary().debugShow());
//    }
    String[] tests = {"Abcde", "Once Upon AB Time", "\u00E0b", "A\u0300b"};
    byte[] output = new byte[1000];
    for (String test : tests) {
      DictionaryCharList<String> dcl = new DictionaryCharList(converter.getDictionary(), test);
      String result = matcher.setText(new DictionaryCharList(converter.getDictionary(), test)).convert(new StringBuffer()).toString();
      System.out.println(test + "\t\t" + result);
      int len = converter.toBytes(test, output, 0);
      for (int i = 0; i < len; ++i) {
        System.out.print(Utility.hex(output[i]&0xFF, 2) + " ");
      }
      System.out.println();
      String result2 = converter.fromBytes(output, 0, len, new StringBuilder()).toString();
      System.out.println(test + "\t?\t" + result2);
      
    }
    
    DictionaryBuilder<String> builder = new StateDictionaryBuilder<String>(); // .setByteConverter(converter);
    Map map = new TreeMap(Dictionary.CHAR_SEQUENCE_COMPARATOR);
    map.put("ab", "found-ab");
    map.put("ss", "found-ss"); // ß
    Dictionary<String> dict = builder.make(map);
    TestStateDictionaryBuilder.tryFind(new DictionaryCharList(converter.getDictionary(),  "Abcde and ab Once Upon aß AB basS Time\u00E0bA\u0300b"), dict, Filter.ALL);
    
  }
}