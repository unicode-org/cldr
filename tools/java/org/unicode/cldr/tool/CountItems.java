/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

public class CountItems {
	static Set keys = new HashSet();
	public static void main(String[] args) {
		Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
		//CLDRKey.main(new String[]{"-mde.*"});
		int count = countItems(cldrFactory, false);
		System.out.println("Count (core): " + count);
		count = countItems(cldrFactory, true);
		System.out.println("Count (resolved): " + count);
		System.out.println("Unique XPaths: " + keys.size());
	}

	/**
	 * @param cldrFactory
	 * @param resolved
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