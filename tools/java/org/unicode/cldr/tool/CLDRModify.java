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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;
import org.unicode.cldr.util.Utility.*;

public class CLDRModify {
	static PrintWriter log;
	
	public static void main(String[] args) throws Exception {
		String sourceDir = "C:\\ICU4C\\locale\\common\\main\\";
		String targetDir = "C:\\DATA\\GEN\\cldr\\main\\";
		SimpleLineComparator lineComparer = new SimpleLineComparator(
				SimpleLineComparator.SKIP_SPACES + SimpleLineComparator.SKIP_EMPTY);

		if (true) {
			int x = lineComparer.compare("a", "b");
			x = lineComparer.compare("a", " a");
			x = lineComparer.compare("", "b");
			x = lineComparer.compare("a", "");
			x = lineComparer.compare("", "");
			
			Utility.generateBat(sourceDir, "ar_AE.xml", targetDir, "ar.xml", lineComparer);
		}
		
		PrintWriter log = BagFormatter.openUTF8Writer(targetDir, "log.txt");
		String[] failureLines = new String[2];
		Factory cldrFactory = Factory.make(sourceDir, ".*", log);
		/*String[] tests = {"de", "root", "en", "fr", "ar"
		};
		for (int i = 0; i < tests.length; ++i) 
		*/
		for (Iterator it = cldrFactory.getAvailable().iterator(); it.hasNext();)
		try {
			String test = (String) it.next();
			//testJavaSemantics();
			
			// TODO parameterize the directory and filter
			//System.out.println("C:\\ICU4C\\locale\\common\\main\\fr.xml");
			
			CLDRFile k = cldrFactory.make(test, false);
			PrintWriter pw = BagFormatter.openUTF8Writer(targetDir, test + ".xml");
			//System.out.println(CLDRFile.getAttributeOrder());
			k.write(pw);
			pw.println();
			pw.close();
			Utility.generateBat(sourceDir, test + ".xml", targetDir, test + ".xml", lineComparer);
			/*
			boolean ok = Utility.areFileIdentical(sourceDir + test + ".xml", 
					targetDir + test + ".xml", failureLines, Utility.TRIM + Utility.SKIP_SPACES);
			if (!ok) {
				System.out.println("Found differences at: ");
				System.out.println("\t" + failureLines[0]);
				System.out.println("\t" + failureLines[1]);
			}
			*/
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
	
	// <localizedPatternChars>GyMdkHmsSEDFwWahKzYeugAZ</localizedPatternChars> 
	/*
		<localizedPattern>
		 <map type="era">G</map>
		 <map type="year">y</map>
		 <map type="year_iso">Y</map>
		 <map type="year_uniform">u</map>
		 <map type="month">M</map>
		 <map type="week_in_year">w</map>
		 <map type="week_in_month">W</map>
		 <map type="day">d</map>
		 <map type="day_of_year">D</map>
		 <map type="day_of_week_in_month">F</map>
		 <map type="day_julian">g</map>
		 <map type="day_of_week">E</map>
		 <map type="day_of_week_local">e</map>
		 <map type="period_in_day">a</map>
		 <map type="hour_1_12">h</map>
		 <map type="hour_0_23">H</map>
		 <map type="hour_0_11">K</map>
		 <map type="hour_1_24">k</map>
		 <map type="minute">m</map>
		 <map type="second">s</map>
		 <map type="fractions_of_second">S</map>
		 <map type="milliseconds_in_day">A</map>
		 <map type="timezone">z</map>
		 <map type="timezone_gmt">Z</map>
		</localizedPattern>
		*/

}