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

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TestCollationStringByteConverter {
  public static void main(String[] args) throws Exception {
    final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
    col.setStrength(col.PRIMARY);
    CollationStringByteConverter converter = new CollationStringByteConverter(col, new StringUtf8Converter());
    Matcher<String> matcher = converter.getDictionary().getMatcher();
    if (true) {
      Iterator<Entry<CharSequence, String>> x = converter.getDictionary().getMapping();
      while (x.hasNext()) {
        Entry<CharSequence, String> entry = x.next();
        System.out.println(entry.getKey() + "\t" + Utility.hex(entry.getKey().toString())+ "\t\t" + entry.getValue() + "\t" + Utility.hex(entry.getValue().toString()));
      }
      System.out.println(converter.getDictionary().debugShow());
    }
    String[] tests = {"Abcde", "Once Upon AB Time", "\u00E0b", "A\u0300b"};
    byte[] output = new byte[1000];
    for (String test : tests) {
      String result = matcher.setText(test).convert(new StringBuffer()).toString();
      System.out.println(test + "\t\t" + result);
      int len = converter.toBytes(test, output, 0);
      for (int i = 0; i < len; ++i) {
        System.out.print(Utility.hex(output[i]&0xFF, 2) + " ");
      }
      System.out.println();
      String result2 = converter.fromBytes(output, 0, len, new StringBuilder()).toString();
      System.out.println(test + "\t?\t" + result2);
      
    }
    
    DictionaryBuilder<String> builder = new StateDictionaryBuilder<String>().setByteConverter(converter);
    Map map = new TreeMap(Dictionary.CHAR_SEQUENCE_COMPARATOR);
    map.put("ab", "found-ab");
    map.put("ß", "found-ß");
    Dictionary<String> dict = builder.make(map);
    TestStateDictionaryBuilder.tryFind( "Abcde and ab Once Upon aß AB basS Time\u00E0bA\u0300b", dict);
    
  }
}