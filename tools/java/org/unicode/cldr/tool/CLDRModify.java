/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.Utility.*;

public class CLDRModify {
	
	private static final int
	    HELP1 = 0,
	    HELP2 = 1,
	    SOURCEDIR = 2,
	    DESTDIR = 3,
	    MATCH = 4,
	    JOIN = 5,
		MINIMIZE = 6
		;
	
	private static final UOption[] options = {
	    UOption.HELP_H(),
	    UOption.HELP_QUESTION_MARK(),
	    UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
	    UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "main/"),
	    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	    UOption.create("join", 'j', UOption.OPTIONAL_ARG),
	    UOption.create("minimize", 'r', UOption.NO_ARG),
	};
	
	public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
		//String sourceDir = "C:\\ICU4C\\locale\\common\\main\\";
		String mergeDir = options[SOURCEDIR].value + "../to_merge/";	// Utility.COMMON_DIRECTORY + "main/";
		String sourceDir = options[SOURCEDIR].value;	// Utility.COMMON_DIRECTORY + "main/";
		String targetDir = options[DESTDIR].value;	// Utility.GEN_DIRECTORY + "main/";
		SimpleLineComparator lineComparer = new SimpleLineComparator(
				SimpleLineComparator.SKIP_SPACES + SimpleLineComparator.SKIP_EMPTY);
		
		Log.setLog(targetDir + "log.txt");
		//String[] failureLines = new String[2];
		Factory cldrFactory = Factory.make(sourceDir, ".*");
		//testMinimize(cldrFactory);
		//if (true) return;
		
		Factory mergeFactory = null;
		if (options[JOIN].doesOccur) {
			mergeFactory = Factory.make(mergeDir, ".*");
		}
		/*
		Factory cldrFactory = Factory.make(sourceDir, ".*");
		Set testSet = cldrFactory.getAvailable();
		String[] quicktest = new String[] {
				"de"
				//"ar", "dz_BT",
				// "sv", "en", "de"
			};
		if (quicktest.length > 0) {
			testSet = new TreeSet(Arrays.asList(quicktest));
		}
		*/
		Set locales = new TreeSet(cldrFactory.getAvailable());
		if (mergeFactory != null) {
			Set temp = new TreeSet(mergeFactory.getAvailable());
			Set locales3 = new TreeSet();
			for (Iterator it = temp.iterator(); it.hasNext();) {
				String locale = (String)it.next();
				if (!locale.startsWith(options[JOIN].value)) continue;
				locales3.add(locale.substring(options[JOIN].value.length()));
			}
			locales.retainAll(locales3);
		}
		new Utility.MatcherFilter(options[MATCH].value).retainAll(locales);

		for (Iterator it = locales.iterator(); it.hasNext();)
		try {
			String test = (String) it.next();
			//testJavaSemantics();
			
			// TODO parameterize the directory and filter
			//System.out.println("C:\\ICU4C\\locale\\common\\main\\fr.xml");
			
			CLDRFile k = (CLDRFile) cldrFactory.make(test, false).clone();
			if (mergeFactory != null) {
				CLDRFile toMergeIn = mergeFactory.make(options[JOIN].value + test, false);
				if (toMergeIn != null) {
					k.putAll(toMergeIn, false);
				}
				// special fix
				k.removeComment(" The following are strings that are not found in the locale (currently), but need valid translations for localizing timezones. ");
			}
			if (options[MINIMIZE].doesOccur) {
				// TODO, fix identity
				String parent = CLDRFile.getParent(test);
				if (parent != null) {
					CLDRFile toRemove = cldrFactory.make(parent, true);
					k.removeDuplicates(toRemove);
				}
			}
			//System.out.println(CLDRFile.getAttributeOrder());
			
			/*if (false) {
				Map tempComments = k.getXpath_comments();

				for (Iterator it2 = tempComments.keySet().iterator(); it2.hasNext();) {
					String key = (String) it2.next();
					String comment = (String) tempComments.get(key);
					Log.logln("Writing extra comment: " + key);
					System.out.println(key + "\t comment: " + comment);
				}
			}*/

			PrintWriter pw = BagFormatter.openUTF8Writer(targetDir, test + ".xml");
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
			Log.close();
		}
		System.out.println("Done");
	}
	
	/**
	 * @param cldrFactory
	 */
	private static void testMinimize(Factory cldrFactory) {
		// quick test of following
		CLDRFile test2;
		/*
		test2 = cldrFactory.make("root", false);
		test2.show();
		System.out.println();
		System.out.println();
		*/
		test2 = cldrFactory.make("root", true);
		//test2.show();
		System.out.println();
		System.out.println();
		PrintWriter xxx = new PrintWriter(System.out);
		test2.write(xxx);
		xxx.flush();
	}

	/**
	 * @param sourceDir
	 * @param targetDir
	 * @return
	 */
	private static SimpleLineComparator testLineComparator(String sourceDir, String targetDir) {
		SimpleLineComparator lineComparer = new SimpleLineComparator(
				SimpleLineComparator.SKIP_SPACES + SimpleLineComparator.SKIP_EMPTY);

		if (false) {
			int x = lineComparer.compare("a", "b");
			x = lineComparer.compare("a", " a");
			x = lineComparer.compare("", "b");
			x = lineComparer.compare("a", "");
			x = lineComparer.compare("", "");
			x = lineComparer.compare("ab", "a b");
			
			Utility.generateBat(sourceDir, "ar_AE.xml", targetDir, "ar.xml", lineComparer);
		}
		return lineComparer;
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