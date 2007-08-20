/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

import java.util.Map;
import java.util.TreeMap;


public class CollationStringByteConverter extends DictionaryStringByteConverter {

  public CollationStringByteConverter(RuleBasedCollator collation, StringByteConverter byteMaker) {
    super(getDictionary(collation), byteMaker);
    // TODO Auto-generated constructor stub
  }

  private static StateDictionary<String> getDictionary(RuleBasedCollator collation) {
    Map<CharSequence,String> map = new TreeMap<CharSequence,String>(Dictionary.CHAR_SEQUENCE_COMPARATOR);
    new CollationMapMaker().generateCollatorFolding(collation, Collator.PRIMARY, true, false, map);
    return new StateDictionaryBuilder<String>().make(map);
  }
}