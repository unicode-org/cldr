/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class RandomStringGenerator {
	
	private static final UnicodeSet SUPPLEMENTARIES = new UnicodeSet(0x10000,0x10FFFF);

	private Random random = new Random(0);
	private UnicodeSet[] sets;
	private UnicodeMap map;
	private UnicodeMap shortMap;
	private UnicodeMap extendedMap;
	
	void init(UnicodeProperty.Factory factory) {
		extendedMap = new UnicodeMap();
		UnicodeMap tempMap = factory.getProperty("GraphemeClusterBreak").getUnicodeMap();
		extendedMap.putAll(tempMap.keySet("CR"), "CR");
		extendedMap.putAll(tempMap.keySet("LF"), "LF");
		extendedMap.putAll(tempMap.keySet("Extend"), "GCExtend");
		extendedMap.putAll(tempMap.keySet("Control"), "GCControl");
	}

	public RandomStringGenerator(UnicodeProperty.Factory factory, String propertyName) {
		this(factory, propertyName, false, false);
	}
	
	public RandomStringGenerator(UnicodeProperty.Factory factory, String propertyName, boolean useShortName, boolean addGCStuff) {
		this(factory, factory.getProperty(propertyName).getUnicodeMap(),
				useShortName ? ICUPropertyFactory.make().getProperty(propertyName).getUnicodeMap(true) : null,
				addGCStuff);
	}
	RandomStringGenerator(UnicodeProperty.Factory factory, UnicodeMap longNameMap, UnicodeMap shortNameMap, boolean addGCStuff) {
		init(factory);
		map = !addGCStuff ? longNameMap 
				: longNameMap.composeWith(extendedMap, MyComposer);
		shortMap = (shortNameMap == null ? longNameMap 
				: !addGCStuff ? shortNameMap 
						: shortNameMap.composeWith(extendedMap, MyComposer));
		List values = new ArrayList(map.getAvailableValues());
		sets = new UnicodeSet[values.size()];
		for (int i = 0; i < sets.length; ++i) {
			sets[i] = map.keySet(values.get(i));
			sets[i].removeAll(SUPPLEMENTARIES);
			if (Segmenter.DEBUG_REDUCE_SET_SIZE != null) {
				int first = sets[i].charAt(0);
				sets[i].retainAll(Segmenter.DEBUG_REDUCE_SET_SIZE);
				if (sets[i].size() == 0) sets[i].add(first);
			}
		}
	}
	static UnicodeMap.Composer MyComposer = new UnicodeMap.Composer(){
		public Object compose(int codePoint, String string, Object a, Object b) {
			if (a == null) return b;
			if (b == null) return a;
			return a + "_" + b;
		}		
	};
	public String getValue(int cp) {
		return (String)shortMap.getValue(cp);
	}
	public String next(int len) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < len; ++i) {
			UnicodeSet us = sets[random.nextInt(sets.length)];
			int cp = us.charAt(random.nextInt(us.size()));
			UTF16.append(result, cp);
		}
		return result.toString();
	}
}