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

public class CLDRModify {
	static PrintWriter log;
	
	public static void main(String[] args) throws Exception {
		String target = "C:\\DATA\\GEN\\cldr\\main\\";
		PrintWriter log = BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\cldr\\main\\", "log.txt");
		try {
			//testJavaSemantics();
			
			// TODO parameterize the directory and filter
			Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*", log);
			
			PrintWriter pw = BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\cldr\\main\\", "de.xml");
			CLDRFile k = cldrFactory.make("de", false);
			k.write(pw);
			pw.close();
		} finally {
			log.close();
		}
		System.out.println("Done");
	}
	
	public static void testJavaSemantics() {
		Collator caseInsensitive = Collator.getInstance(ULocale.ROOT);
		caseInsensitive.setStrength(Collator.SECONDARY);
		Set setWithCaseInsensitive = new TreeSet(caseInsensitive);
		setWithCaseInsensitive.addAll(Arrays.asList(new String[] {"a", "b", "c"}));
		Set plainSet = new TreeSet();
		plainSet.addAll(Arrays.asList(new String[] {"a", "b", "B"}));
		System.out.println("S1 equals S2?\t" + setWithCaseInsensitive.equals(plainSet));
		System.out.println("S2 equals S1?\t" + plainSet.equals(setWithCaseInsensitive));
		setWithCaseInsensitive.removeAll(plainSet);
		System.out.println("S1 removeAll S2 is empty?\t" + setWithCaseInsensitive.isEmpty());
	}
}