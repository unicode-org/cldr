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
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.test.CLDRTest;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.Utility.*;

/**
 * Tool for applying modifications to the CLDR files. Use -h to see the options.
 * <p>There are some environment variables that can be used with the program
 * <br>-DSHOW_FILES=<anything> shows all create/open of files.
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
		FIX = 7,
		JOIN_ARGS = 8
		;
	
	private static final UOption[] options = {
	    UOption.HELP_H(),
	    UOption.HELP_QUESTION_MARK(),
	    UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
	    UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "main/"),
	    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	    UOption.create("join", 'j', UOption.OPTIONAL_ARG),
	    UOption.create("minimize", 'r', UOption.NO_ARG),
	    UOption.create("fix", 'f', UOption.OPTIONAL_ARG),
	    UOption.create("join-args", 'i', UOption.OPTIONAL_ARG),
	};
	
	static final String HELP_TEXT = "Use the following options" + XPathParts.NEWLINE
		+ "-h or -?\tfor this message" + XPathParts.NEWLINE
		+ "-"+options[SOURCEDIR].shortName + "\tsource directory. Default = " 
		+ Utility.getCanonicalName(Utility.MAIN_DIRECTORY) + XPathParts.NEWLINE
		+ "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
		+ "-"+options[DESTDIR].shortName + "\tdestination directory. Default = "
		+ Utility.getCanonicalName(Utility.GEN_DIRECTORY + "main/") + XPathParts.NEWLINE
		+ "-m<regex>\tto restrict the locales to what matches <regex>" + XPathParts.NEWLINE
		+ "-j<merge_dir>/X'\tto merge two sets of files together (from <source_dir>/X and <merge_dir>/X', " + XPathParts.NEWLINE
		+ "\twhere * in X' is replaced by X)." + XPathParts.NEWLINE
		+ "\tExample:-jC:\\Unicode-CVS2\\cldr\\dropbox\\to_be_merged\\missing\\missing_*" + XPathParts.NEWLINE
		+ "-i\tmerge arguments:" + XPathParts.NEWLINE
		+ "\td\tmake draft" + XPathParts.NEWLINE
		+ "\tc\tignore comments in <merge_dir> files" + XPathParts.NEWLINE
		+ "-r\tto minimize the results (removing items that inherit from parent)." + XPathParts.NEWLINE
		+ "-f\tto perform various fixes on the files (TBD: add argument to specify which ones)" + XPathParts.NEWLINE
		+ "\tn\tfix numbers" + XPathParts.NEWLINE
		+ "\tv\tvalidate codes" + XPathParts.NEWLINE
		+ "\tc\tfix CS" + XPathParts.NEWLINE
		+ "\ts\tfix stand-alone narrows" + XPathParts.NEWLINE
		+ "\tr\tfix references and standards" + XPathParts.NEWLINE
		+ "A set of bat files are also generated in <dest_dir>/diff. They will invoke a comparison program on the results.";
	
	/**
	 * Picks options and executes. Use -h to see options.
	 */
	public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP1].doesOccur) {
        	System.out.println(HELP_TEXT);
        	return;
        }
		//String sourceDir = "C:\\ICU4C\\locale\\common\\main\\";
		String mergeDir = options[JOIN].value;	// Utility.COMMON_DIRECTORY + "main/";
		String sourceDir = options[SOURCEDIR].value;	// Utility.COMMON_DIRECTORY + "main/";
		String targetDir = options[DESTDIR].value;	// Utility.GEN_DIRECTORY + "main/";
		
		SimpleLineComparator lineComparer = new SimpleLineComparator(
				SimpleLineComparator.SKIP_SPACES + SimpleLineComparator.SKIP_EMPTY + SimpleLineComparator.SKIP_CVS_TAGS);
		
		Log.setLog(targetDir + "log.txt");
		//String[] failureLines = new String[2];
		Factory cldrFactory = Factory.make(sourceDir, ".*");
		//testMinimize(cldrFactory);
		//if (true) return;
		
		Factory mergeFactory = null;
		String join_prefix = "", join_postfix = "";
		if (options[JOIN].doesOccur) {
			File temp = new File(mergeDir);
			mergeDir = temp.getParent() + File.separator;
			String filename = temp.getName();
			join_prefix = join_postfix = "";
			int pos = filename.indexOf("*");
			if (pos >= 0) {
				join_prefix = filename.substring(0,pos);
				join_postfix = filename.substring(pos+1);
			}
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
				if (!locale.startsWith(join_prefix) || !locale.endsWith(join_postfix)) continue;
				locales3.add(locale.substring(join_prefix.length(), locale.length() - join_postfix.length()));
			}
			locales.retainAll(locales3);
			System.out.println("Merging: " + locales3);
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
				CLDRFile toMergeIn = (CLDRFile) mergeFactory.make(join_prefix + test + join_postfix, false).clone();				
				if (options[JOIN_ARGS].doesOccur) {
					if (options[JOIN_ARGS].value.indexOf("d") >= 0) toMergeIn.makeDraft();
					if (options[JOIN_ARGS].value.indexOf("c") >= 0) toMergeIn.clearComments();
					if (options[JOIN_ARGS].value.indexOf("x") >= 0) removePosix(toMergeIn);
				}
				if (toMergeIn != null) {
					k.putAll(toMergeIn, k.MERGE_ADD_ALTERNATE);
				}
				// special fix
				k.removeComment(" The following are strings that are not found in the locale (currently), but need valid translations for localizing timezones. ");
			}
			if (options[FIX].doesOccur) {
				fix(k, options[FIX].value.toLowerCase(), cldrFactory);
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
	
	/**
	 * 
	 */
	private static void removePosix(CLDRFile toMergeIn) {
		Set toRemove = new HashSet();
		for (Iterator it = toMergeIn.keySet().iterator(); it.hasNext();) {
			String xpath = (String) it.next();
			if (xpath.startsWith("/ldml/posix")) toRemove.add(xpath);
		}
		toMergeIn.removeAll(toRemove, false);
	}

	abstract static class CLDRFilter {
		protected CLDRFile k;
		protected Set availableChildren;
		protected XPathParts parts = new XPathParts(null, null);
		protected XPathParts fullparts = new XPathParts(null, null);
		public abstract void handle(String xpath, Set removal, CLDRFile additions);
		public void setFile(CLDRFile k) {
			this.k = k;
		}
	}
	
	static CLDRFilter fixCS = new CLDRFilter() {
		public void handle(String xpath, Set removal, CLDRFile replacements) {
			if (!xpath.startsWith("/ldml/localeDisplayNames/territories/territory")) return;
			String type = parts.set(xpath).findAttributeValue("territory", "type");
			if ("CS".equals(type) || "SP".equals(type)) {
				String v = k.getStringValue(xpath);
				String fullXPath = k.getFullXPath(xpath);
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
				replacements.add(parts.toString(), fullparts.toString(), v);
				removal.add(xpath);
			}
		}		
	};
	
	static CLDRFilter fixNarrow = new CLDRFilter() {
		public void handle(String xpath, Set removal, CLDRFile replacements) {
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
			if (k.getStringValue(parts.toString()) != null) return;
			
			String v = k.getStringValue(xpath);
			String fullXPath = k.getFullXPath(xpath);
			fullparts.set(fullXPath);
			fullparts.setAttribute(element, "type", "stand-alone");
			replacements.add(parts.toString(), fullparts.toString(), v);
			removal.add(xpath);
		}		
	};
	
	
	static CLDRFilter fixNumbers = new CLDRFilter() {
		public void handle(String xpath, Set removal, CLDRFile replacements) {
			byte type = CLDRTest.getNumericType(xpath);
			if (type == CLDRTest.NOT_NUMERIC_TYPE) return;
			String value = k.getStringValue(xpath);
			// at this point, we only have currency formats
			boolean isPOSIX = k.getKey().indexOf("POSIX") >= 0;
			String pattern = CLDRTest.getCanonicalPattern(value, type, isPOSIX);
			if (pattern.equals(value)) return;
			replacements.add(xpath, k.getFullXPath(xpath), pattern);
		}
	};

	static CLDRFilter fixReferences = new CLDRFilter() {
		References standards = new References(true);
		References references = new References(false);
		public void setFile(CLDRFile k) {
			super.setFile(k);
			standards.clear();
			references.clear();
		}
		public void handle(String xpath, Set removal, CLDRFile replacements) {
			String value = k.getStringValue(xpath);
			String fullpath = k.getFullXPath(xpath);
			if (fullpath.indexOf("[@references=\"") < 0 && fullpath.indexOf("[@standard=\"") < 0) return;
			fullparts.set(fullpath);
			int fixCount = 0;
			for (int i = 0; i < fullparts.size(); ++i) {
				Map attributes = fullparts.getAttributes(i);
				fixCount += standards.fix(attributes, replacements);
				fixCount += references.fix(attributes, replacements);
			}
			if (fixCount >= 0) replacements.add(fullparts.toString(), value);
		}
	};
	
	private static class References {
		static String[][] keys = {{"standard", "S", "[@standard=\"true\"]"}, {"references", "R", ""}};
		UnicodeSet digits = new UnicodeSet("[0-9]");
		int referenceCounter = 0;
		Map referencesMap = new TreeMap();
		String[] keys2;
		References(boolean standard) {
			keys2 = standard ? keys[0] : keys[1];
		}
		/**
		 * 
		 */
		public void clear() {
			referenceCounter = 0;
			referencesMap.clear();
		}
		private int fix(Map attributes, CLDRFile replacements) {
			String references = (String) attributes.get(keys2[0]);
			int result = 0;
			if (references != null) {
				references = references.trim();
				if (references.startsWith("S") || references.startsWith("R")) {
					if (digits.containsAll(references.substring(1))) return 0;
				}
				String token = (String) referencesMap.get(references);
				if (token == null) {
					token = keys2[1] + (++referenceCounter);
					referencesMap.put(references, token);
					System.out.println("Adding: " + token + "\t" + references);
					replacements.add("/ldml/references/reference[@type=\"" + token + "\"]" + keys2[2], references);
					result = 1;
				}
				attributes.put(keys2[0], token);
			}
			return result;
		}
	}

	// references="http://www.stat.fi/tk/tt/luokitukset/lk/kieli_02.html"

	private static class ValuePair {
		String value;
		String fullxpath;
	}
	/**
	 * Find the set of xpaths that 
	 * (a) have all the same values (if present) in the children
	 * (b) are absent in the parent,
	 * (c) are different than what is in the fully resolved parent
	 * and add them.
	 */
	static void fixIdenticalChildren(CLDRFile.Factory cldrFactory, CLDRFile k, CLDRFile replacements) {
		String key = k.getKey();
		Set availableChildren = cldrFactory.getAvailableWithParent(key, true);
		if (availableChildren.size() == 0) return;
		Set skipPaths = new HashSet();
		Map haveSameValues = new TreeMap();
		CLDRFile resolvedFile = cldrFactory.make(key, true);
		skipPaths.addAll(k.keySet());
		for (Iterator it1 = availableChildren.iterator(); it1.hasNext();) {
			String locale = (String)it1.next();
			if (locale.indexOf("POSIX") >= 0) continue;
			CLDRFile item = cldrFactory.make(locale, true);
			for (Iterator it2 = item.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				if (skipPaths.contains(xpath)) continue;
				// skip certain elements
				if (xpath.indexOf("/identity") >= 0) continue;
				if (xpath.startsWith("/ldml/numbers/currencies/currency")) continue;
				if (xpath.indexOf("[@alt") >= 0) continue;

				// must be string vale
				ValuePair v1 = new ValuePair();
				v1.value = item.getStringValue(xpath);
				v1.fullxpath = item.getFullXPath(xpath);

				ValuePair vAlready = (ValuePair) haveSameValues.get(xpath);
				if (vAlready == null) {
					haveSameValues.put(xpath, v1);
				} else if (!v1.value.equals(vAlready.value) || !v1.fullxpath.equals(vAlready.fullxpath)) {
					skipPaths.add(xpath);
					haveSameValues.remove(xpath);
				}
			}
		}
		// at this point, haveSameValues is all kosher, so add items
		for (Iterator it = haveSameValues.keySet().iterator(); it.hasNext();) {
			String xpath = (String) it.next();
			ValuePair v = (ValuePair) haveSameValues.get(xpath);
			if (v.value.equals(resolvedFile.getStringValue(xpath))
					&& v.fullxpath.equals(resolvedFile.getFullXPath(xpath))) continue;
			replacements.add(xpath, v.fullxpath, v.value);
		}
	}

	/**
	 * Perform various fixes
	 * TODO add options to pick which one.
	 * @param options TODO
	 * @param cldrFactory TODO
	 */
	private static void fix(CLDRFile k, String options, Factory cldrFactory) {
		
		// TODO before modifying, make sure that it is fully resolved.
		// then minimize against the NEW parents
		
		Set removal = new TreeSet(CLDRFile.ldmlComparator);
		CLDRFile replacements = CLDRFile.make("temp");
		fixNumbers.setFile(k);
		fixCS.setFile(k);
		fixNarrow.setFile(k);
		fixReferences.setFile(k);
		
		for (Iterator it2 = k.keySet().iterator(); it2.hasNext();) {
			String xpath = (String) it2.next();

			// Fix number problems across locales
			// http://www.jtcsv.com/cgibin/locale-bugs?findid=180
			if (options.indexOf('n') >= 0) fixNumbers.handle(xpath, removal, replacements);
		
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
			if (options.indexOf('c') >= 0) fixCS.handle(xpath, removal, replacements);
			
			//Give best default for each language
			//http://www.jtcsv.com/cgibin/locale-bugs?findid=282
			
			// It appears that all of the current "narrow" data that we have was intended to be
			// stand-alone instead of format, and should be changed to be so in a mechanical
			// sweep.
			if (options.indexOf('s') >= 0) fixNarrow.handle(xpath, removal, replacements);
			
			// move references
			// http://www.jtcsv.com/cgibin/cldr/locale-bugs-private/data?id=445
			// My recommendation would be: collect all
			// contents of standard and references. Number the standards S001, S002,... and the
			// references R001, R002, etc. Emit
			if (options.indexOf('r') >= 0) fixReferences.handle(xpath, removal, replacements);
		}
		
		//remove bad attributes
		//		http://www.jtcsv.com/cgibin/locale-bugs?findid=351
		//Removing invalid currency codes
		//http://www.jtcsv.com/cgibin/locale-bugs?findid=323
		
		if (options.indexOf('v') >= 0) CLDRTest.checkAttributeValidity(k, null, removal);
		
		// raise identical elements
		
		if (options.indexOf('i') >= 0) fixIdenticalChildren(cldrFactory, k, replacements);
		
		// now do the actions we collected
		
		if (removal.size() != 0) {
			k.removeAll(removal, COMMENT_REMOVALS);
		}
		k.putAll(replacements, k.MERGE_REPLACE_MINE);
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