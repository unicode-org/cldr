/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import org.unicode.cldr.util.IntMap;
import org.unicode.cldr.util.IntMap.IntMapFactory;
import org.unicode.cldr.util.IntMap.BasicIntMapFactory;
import org.unicode.cldr.util.IntMap.CompactStringIntMapFactory;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestIntMap {
  static NumberFormat format = NumberFormat.getInstance();
  public static void main(String[] args) {
    Object[] factories = {new BasicIntMapFactory<String>(), new CompactStringIntMapFactory()};
    Set<String> samples = new TreeSet<String>();
    for (String langCode : ULocale.getISOLanguages()) {
      samples.add(ULocale.getDisplayLanguage(langCode, ULocale.JAPANESE));
    }
    checkSamples(factories, samples);
    UnicodeSet testSet = new UnicodeSet("[[:assigned:] - [:ideographic:] - [:Co:] - [:Cs:] - [:script=Hang:]]"); // & [\\u0000-\\u0FFF]
    
    samples.clear();
    for (UnicodeSetIterator it = new UnicodeSetIterator(testSet); it.next();) {
      String name = UCharacter.getExtendedName(it.codepoint);
      if (name != null) {
        samples.add(name);
      } 
    }
    checkSamples(factories, samples);
  }

  private static void checkSamples(Object[] factories, Set<String> samples) {
    int bytes = 0;
    for (String string : samples) {
      System.out.println(string);
      bytes += string.length()*2;
    }
    System.out.println("String count: " + format.format(samples.size()) + "\t bytes: " + format.format(bytes));
    for (Object factoryObject :  factories) {
      IntMap<String> map = ((IntMapFactory<String>)factoryObject).make(samples);
       Map<String, Integer> values = map.getValueMap(new TreeMap<String,Integer>());
      System.out.println(map.getClass().getName() + "\t" + format.format(map.approximateStorage()));
      if (!values.keySet().equals(samples)) {
        System.out.println("Missing Values!");
      }
    }
  }
}