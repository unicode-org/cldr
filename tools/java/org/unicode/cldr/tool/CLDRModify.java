/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.test.CLDRTest;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.StringValue;
import org.unicode.cldr.util.CLDRFile.Value;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.Utility.*;

/**
 * Tool for applying modifications to the CLDR files. Use -h to see the options.
 */
public class CLDRModify {
	static final boolean COMMENT_REMOVALS = false; // append removals as comments
	// TODO make this into input option.
	
	private static final int
	    HELP1 = 0,
	    HELP2 = 1,
	    SOURCEDIR = 2,
	    DESTDIR = 3,
	    MATCH = 4,
	    JOIN = 5,
		MINIMIZE = 6,
		FIX = 7
		;
	
	private static final UOption[] options = {
	    UOption.HELP_H(),
	    UOption.HELP_QUESTION_MARK(),
	    UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
	    UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "main/"),
	    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	    UOption.create("join", 'j', UOption.OPTIONAL_ARG),
	    UOption.create("minimize", 'r', UOption.NO_ARG),
	    UOption.create("fix", 'f', UOption.NO_ARG),
	};
	
	static final String HELP_TEXT = "Use the following options" + XPathParts.NEWLINE
	+ "-h or -?\tfor this message" + XPathParts.NEWLINE
	+ "-m<regex>\tto restrict the locales to what matches <regex>" + XPathParts.NEWLINE
		+ "-j<prefix>\tto merge two sets of files together (from <source_dir>/X and <source_dir>/../to_merge/<prefix>X)" + XPathParts.NEWLINE
		+ "-r\tto minimize the results (removing items that inherit from parent)." + XPathParts.NEWLINE
		+ "-f\tto perform various fixes on the files (TBD: add argument to specify which ones)" + XPathParts.NEWLINE;
	
	/**
	 * Picks options and executes.
	 */
	public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP1].doesOccur) {
        	System.out.println(HELP_TEXT
        			+ "-"+options[SOURCEDIR].shortName + "\tsource directory. Default = " 
						+ new File(Utility.MAIN_DIRECTORY).getCanonicalPath() + XPathParts.NEWLINE
        			+ "-"+options[DESTDIR].shortName + "\tdestination directory. Default = "
						+ new File(Utility.GEN_DIRECTORY + "main/").getCanonicalPath() + XPathParts.NEWLINE
        			);
        	return;
        }
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
			if (options[FIX].doesOccur) {
				fix(k);
			}
			if (options[MINIMIZE].doesOccur) {
				// TODO, fix identity
				String parent = CLDRFile.getParent(test);
				if (parent != null) {
					CLDRFile toRemove = cldrFactory.make(parent, true);
					k.removeDuplicates(toRemove, COMMENT_REMOVALS);
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
	
	abstract static class CLDRFilter {
		protected XPathParts parts = new XPathParts(null, null);
		protected XPathParts fullparts = new XPathParts(null, null);
		public abstract void handle(CLDRFile k, String xpath, Set removal, CLDRFile additions);
	}
	
	static CLDRFilter fixCS = new CLDRFilter() {
		public void handle(CLDRFile k, String xpath, Set removal, CLDRFile replacements) {
			if (!xpath.startsWith("/ldml/localeDisplayNames/territories/territory")) return;
			String type = parts.set(xpath).findAttribute("territory", "type");
			if ("CS".equals(type) || "SP".equals(type)) {
				Value v = k.getValue(xpath);
				String fullXPath = v.getFullXPath();
				fullparts.set(fullXPath);
				if (type.equals("CS")) {
					parts.setAttribute("territory", "type", "200");
					fullparts.setAttribute("territory", "type", "200");
				} else {
					parts.setAttribute("territory", "type", "CS");
					fullparts.setAttribute("territory", "type", "CS");
					parts.setAttribute("territory", "draft", "true");
					fullparts.setAttribute("territory", "draft", "true");
				}
				replacements.add(parts.toString(), fullparts.toString(), v.getStringValue());
				removal.add(xpath);
			}
		}		
	};
	
	static CLDRFilter fixNarrow = new CLDRFilter() {
		public void handle(CLDRFile k, String xpath, Set removal, CLDRFile replacements) {
			if (xpath.indexOf("[@type=\"narrow\"]") < 0) return;
			parts.set(xpath);
			String element = "";
			if (parts.findElement("dayContext") >= 0) {
				element = "dayContext";
			} else if (parts.findElement("monthContext") >= 0) {
				element = "monthContext";				
			} else return;
			
			// change the element type UNLESS it conflicts
			parts.setAttribute(element, "type", "stand-alone");
			if (k.getValue(parts.toString()) != null) return;
			
			Value v = k.getValue(xpath);
			String fullXPath = v.getFullXPath();
			fullparts.set(fullXPath);
			fullparts.setAttribute(element, "type", "stand-alone");
			replacements.add(parts.toString(), fullparts.toString(), v.getStringValue());
			removal.add(xpath);
		}		
	};
	
	static CLDRFilter fixNumbers = new CLDRFilter() {
		public void handle(CLDRFile k, String xpath, Set removal, CLDRFile replacements) {
			byte type = CLDRTest.getNumericType(xpath);
			if (type == CLDRTest.NOT_NUMERIC_TYPE) return;
			CLDRFile.StringValue value = (StringValue) k.getValue(xpath);
			// at this point, we only have currency formats
			boolean isPOSIX = k.getKey().indexOf("POSIX") >= 0;
			String pattern = CLDRTest.getCanonicalPattern(value.getStringValue(), type, isPOSIX);
			if (pattern.equals(value.getStringValue())) return;
			replacements.add(xpath, value.getFullXPath(), pattern);
		}
	};
	
	/**
	 * Perform various fixes
	 * TODO add options to pick which one.
	 */
	private static void fix(CLDRFile k) {
		
		// TODO before modifying, make sure that it is fully resolved.
		// then minimize against the NEW parents
		
		Set removal = new TreeSet(CLDRFile.ldmlComparator);
		CLDRFile replacements = CLDRFile.make("temp");
		
		for (Iterator it2 = k.keySet().iterator(); it2.hasNext();) {
			String xpath = (String) it2.next();

			// Fix number problems across locales
			// http://www.jtcsv.com/cgibin/locale-bugs?findid=180
			//fixNumbers.handle(k, xpath, removal, replacements);
		
			//Before removing SP, do the following!
			//http://www.jtcsv.com/cgibin/locale-bugs?findid=351, 353
			/*
			<territory type="CS">Czechoslovakia</territory>
			=>
			<territory type="200">Czechoslovakia</territory>
	
			<territory type="SP">Serbia</territory>
			=>
			<territory type="CS" draft="true">Serbia</territory> <!-- should be serbia & montegro -->
			*/
			fixCS.handle(k, xpath, removal, replacements);
			
			//Give best default for each language
			//http://www.jtcsv.com/cgibin/locale-bugs?findid=282
			
			// It appears that all of the current "narrow" data that we have was intended to be
			// stand-alone instead of format, and should be changed to be so in a mechanical
			// sweep.
			//fixNarrow.handle(k, xpath, removal, replacements);
			
			// move references
			// http://www.jtcsv.com/cgibin/cldr/locale-bugs-private/data?id=445
			// My recommendation would be: collect all
			// contents of standard and references. Number the standards S001, S002,... and the
			// references R001, R002, etc. Emit
		}
		
		//remove bad attributes
		//		http://www.jtcsv.com/cgibin/locale-bugs?findid=351
		//Removing invalid currency codes
		//http://www.jtcsv.com/cgibin/locale-bugs?findid=323
		
		CLDRTest.checkAttributeValidity(k, null, removal);
		
		
		// now do the actions we collected
		
		if (removal.size() != 0) {
			k.removeAll(removal, COMMENT_REMOVALS);
		}
		k.putAll(replacements, false);
	}

	/**
	 * Internal
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
	 * Internal
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

	/**
	 * Internal
	 */
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