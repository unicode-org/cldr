/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.text.RuleBasedCollator;

public class CollationStringByteConverter extends DictionaryStringByteConverter {

    public CollationStringByteConverter(RuleBasedCollator collation, StringByteConverter byteMaker) {
        super(getDictionaryInfo(collation, byteMaker), byteMaker);
        // TODO Auto-generated constructor stub
    }

    private static StateDictionary<String> getDictionaryInfo(RuleBasedCollator collation, StringByteConverter byteMaker) {
        Map<CharSequence, String> map = new TreeMap<CharSequence, String>(Dictionary.CHAR_SEQUENCE_COMPARATOR);
        new CollationMapMaker().generateCollatorFolding(collation, map);
        return new StateDictionaryBuilder<String>().setByteConverter(byteMaker)
            .setIntMapFactory(new IntMap.CompactStringIntMapFactory()).make(map);
    }
}