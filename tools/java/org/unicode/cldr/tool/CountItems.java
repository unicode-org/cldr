/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;
public class CountItems {
	static Set keys = new HashSet();
	public static void main(String[] args) {
		Factory cldrFactory = CLDRFile.Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*", null);
		//CLDRKey.main(new String[]{"-mde.*"});
		int count = countItems(cldrFactory, false);
		System.out.println("Count (core): " + count);
		count = countItems(cldrFactory, true);
		System.out.println("Count (resolved): " + count);
		System.out.println("Unique XPaths: " + keys.size());
	}

	/**
	 * @param cldrFactory
	 * @param locales
	 */
	private static int countItems(Factory cldrFactory, boolean resolved) {
		int count = 0;
		Set locales = cldrFactory.getAvailable();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			CLDRFile item = cldrFactory.make(locale, resolved);
			keys.addAll(item.keySet());
			int current = item.keySet().size();
			System.out.println(locale + "\t" + current);
			count += current;
		}
		return count;
	}

}