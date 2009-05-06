/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.RandomStringGenerator;
import org.unicode.cldr.util.Segmenter;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.Segmenter.Rule.Breaks;

import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeProperty;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Quick class for testing proposed syntax for Segments.
 * TODO doesn't yet handle supplementaries. It looks like even Java 5 won't help, since it doesn't have syntax for them.
 * Will have to change [...X-Y] into ([...] | X1 [Y1-\uDFFF] | [X2-X3][\uDC00-\uDFFF] | X4[\uD800-Y2)
 * where the X1,Y1 is the first surrogate pair, and X4,Y2 is the last (2nd and 3rd ranges are only if X4 != X2).
 * @author davis
 */

public class TestSegments {
	private static final boolean TESTING = true;
	static String indent = "\t\t";
	
	// static String indent = "";
	
	/**
	 * Shows the rule that caused the result at each offset.
	 */
	private static final boolean DEBUG_SHOW_MATCHES = false;
	private static final boolean SHOW_RULE_LIST = false;
	private static final int monkeyLimit = 1000, monkeyStringCount = 10;
	
	private static final Matcher flagItems = Pattern.compile(
	"[$](BK|CR|LF|CM|NL|WJ|ZW|GL|SP|CB)").matcher("");
	
	/**
	 * Quick test of features for debugging
	 * @param args unused
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
    Log.setLogNoBOM(Utility.GEN_DIRECTORY + "/segments/root.xml");
    Log.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    Log.println("<!DOCTYPE ldml SYSTEM \"../../common/dtd/ldml.dtd"\">");
    Log.println("<ldml>");
    Log.println("\t<identity>");
    Log.println("\t\t<version number=\"$Revision$\"/>");
    Log.println("\t\t<generation date=\"$Date$\"/>");
    Log.println("\t\t<language type=\"root\"/>");
    Log.println("\t</identity>");
    Log.println("\t<segmentations>");
    
		if (args.length == 0) args = new String[] {"GraphemeClusterBreak", "LineBreak", "SentenceBreak", "WordBreak"};
		List testChoice = Arrays.asList(args);
		
		UnicodeProperty.Factory propFactory = ICUPropertyFactory.make();
		
		// grab the rules, build a RuleList, and run against the test samples.
		
		for (int i = 0; i < tests.length; ++i) {
			String testName = tests[i][0];
			if (!testChoice.contains(testName)) continue;
			int j = 1;
			Segmenter.Builder rb = Segmenter.make(propFactory, testName);
			if (rb != null) {
				System.out.println("Found: " + testName);
			} else {
				rb = new Segmenter.Builder(propFactory);
			}
			if (TESTING) {
				System.out.println();
				System.out.println();
				System.out.println("Building: " + testName);
			}
			for (; j < tests[i].length; ++j) {
				String line = tests[i][j];
				if (line.equals("test")) break;
				rb.addLine(line);
			}
			
			Log.print(rb.toString(testName, indent));
			if (!TESTING) continue;
			System.out.println();
			System.out.println("Testing");
			Segmenter rl = rb.make();
			Collection values = rl.getSamples().getAvailableValues();
			System.out.println("Value Partition: " + values);

			if (false) debugRule(rb);
			
			if (SHOW_RULE_LIST) System.out.println(rl.toString(true));
			for (++j; j < tests[i].length; ++j) {
				System.out.println();
				String line = tests[i][j];
				if (line.startsWith("compare")) {
					doCompare(propFactory, rl, line);
					break;
				}
				String showingBreaks = ""; // don't bother with surrogates yet
				for (int k = 0; k <= line.length(); ++k) {
					if (rl.breaksAt(line,k)) {
						showingBreaks += '|';
					} 
					if (DEBUG_SHOW_MATCHES && rl.getBreakRule() >= 0) {
						showingBreaks += "\u00AB" + Segmenter.nf.format(rl.getBreakRule()) + "\u00BB";
					}
					if (k < line.length()) showingBreaks += line.charAt(k);
				}
				System.out.println(showingBreaks);
			}
		}
    Log.println("\t</segmentations>");
    Log.println("</ldml>");
		Log.close();
		System.out.println();
		System.out.println("Done");
	}
	
	private static void debugRule(Segmenter.Builder rb) {
		Segmenter.Rule rule = rb.make().get(16.01);
		String oldAL = (String)rb.getVariables().get("$oldAL");
		UnicodeSet oldALSet = new UnicodeSet(oldAL);
		String testStr = "\uA80D/\u0745\u2026";
		for (int k = 0; k < testStr.length(); ++k) {
			boolean inside = oldALSet.contains(testStr.charAt(k));
			System.out.println(k + ": " + inside + com.ibm.icu.impl.Utility.escape(""+testStr.charAt(k)));
		}
		Breaks m = rule.matches(testStr, 3);
	}
	
	private static void doCompare(UnicodeProperty.Factory factory, Segmenter rl, String line) {
		RandomStringGenerator rsg;
		RuleBasedBreakIterator icuBreak;
		if (line.equals("compareGrapheme")) {
			rsg = new RandomStringGenerator(factory, "GraphemeClusterBreak");
			icuBreak = (RuleBasedBreakIterator) BreakIterator.getCharacterInstance();
		} else if (line.equals("compareWord")) {
			rsg = new RandomStringGenerator(factory, "WordBreak", false, true);
			icuBreak = (RuleBasedBreakIterator) BreakIterator.getWordInstance();
		} else if (line.equals("compareSentence")) {
			rsg = new RandomStringGenerator(factory, "SentenceBreak", false, true);
			icuBreak = (RuleBasedBreakIterator) BreakIterator.getSentenceInstance();
		} else if (line.equals("compareLine")) {
			rsg = new RandomStringGenerator(factory, "LineBreak", true, false);
			icuBreak = (RuleBasedBreakIterator) BreakIterator.getLineInstance();
		} else {
			throw new IllegalArgumentException("Bad tag: " + line);
		}
		System.out.println("Monkey Test: " + line + "\t icuBreaks = $\t ruleBreaks =@");
		boolean gotDot = false;
		int limit = monkeyLimit/monkeyStringCount;
		for (int i = 0; i < limit; ++i) {
			if ((i % 100) == 0) {
				System.out.println(); System.out.flush();
				System.out.print(i*monkeyStringCount);
			}
			System.out.print('.');
			gotDot = true;
			
			String test = rsg.next(monkeyStringCount);
			icuBreak.setText(test);
			int[] icuStatus = new int[20];
			int[] ruleStatus = new int[20];
			for (int j = 0; j <= test.length(); ++j) {
				boolean icuBreakResults = icuBreak.isBoundary(j);
				boolean ruleListResults = rl.breaksAt(test, j);
				if (icuBreakResults == ruleListResults) continue;
				if (gotDot) {
					System.out.println();
					gotDot = false;
				}
				System.out.println(line + "\tMismatch at Line\t" + i 
						+ ",\toffset\t" + j 
						+ ",\twith Rule\t" + rl.getBreakRule()
						+ ":\t" + (icuBreakResults ? "ICU Breaks, CLDR Doesn't" : "ICU Doesn't, CLDR Breaks"));
				System.out.println(showResults(test, j, rsg, icuBreakResults));
				rl.breaksAt(test, j); // for debugging
			}
		}		
	}
	
	private static String showStatus(int[] icuStatus, int icuStatusLen) {
		String result = "";
		for (int i = 0; i < icuStatusLen; ++i) {
			if (result.length() != 0) result += ", ";
			result += icuStatus[i];
		}
		return "[" + result + "]";
	}
	
	static boolean equalStatus(int[] status1, int len1, int[] status2, int len2) {
		if (len1 != len2) return false;
		for (int i = 0; i < len1; ++i) {
			if (status1[i] != status2[i]) return false;
		}
		return true;
	}
	
	private static String showResults(String test, int j, RandomStringGenerator rsg, boolean icuBreakResults) {
		StringBuffer results = new StringBuffer();
		int cp;
		for (int i = 0; i < test.length(); i += UTF16.getCharCount(cp)) {
			if (i == j) results.append(icuBreakResults ? "<" + Utility.LINE_SEPARATOR + "$ >" : "<" + Utility.LINE_SEPARATOR + "@ >");
			cp = UTF16.charAt(test, i);
			results.append("[" + rsg.getValue(cp) + ":" + com.ibm.icu.impl.Utility.escape(UTF16.valueOf(cp)) + "]");
		}
		if (test.length() == j) results.append(icuBreakResults ? "<" + Utility.LINE_SEPARATOR + "$ >" : "<" + Utility.LINE_SEPARATOR + "@ >");
		return results.toString();
	}
	
	/**
	 * For quickly checking regex syntax implications in Java
	 */
	private static boolean quickCheck() {
		String[][] rtests = {{
			".*" + new UnicodeSet("[\\p{Grapheme_Cluster_Break=LVT}]").complement().complement(), "\u001E\uC237\u1123\n\uC91B"
		},{
			"(?<=a)b", "ab"
		},{
			"[$]\\p{Alpha}\\p{Alnum}*", "$Letter"
		}};
		for (int i = 0; i < rtests.length; ++i) {
			Matcher m = Pattern.compile(rtests[i][0], Segmenter.REGEX_FLAGS).matcher("");
			m.reset(rtests[i][1]);
			boolean matches = m.matches();
			System.out.println(rtests[i][0] + ",\t" + rtests[i][1] + ",\t" + matches);
		}
		return false;
	}
	
	static final String[][] tests = {{
		"QuickCheck",
		"1) \u00F7 b",
		"2) \u00D7 .",
		"0.5) a \u00D7",
		"test",
		"abcbdb"
	},{
		"QuickCheck2",
		"$Letter=\\p{Alphabetic}",
		"$Digit=\\p{Digit}",
		"1) $Digit \u00D7 $Digit",
		"2) $Letter \u00D7 $Letter",
		"test",
		"The quick 100 brown foxes."
	},{
		"GraphemeClusterBreak",
		"test",
		"The qui\u0300ck 100 brown foxes.",
		"compareGrapheme"
	},{
		"LineBreak",
		"test",
		"\uCD40\u1185",
		"http://www.cs.tut.fi/%7Ejkorpela/html/nobr.html?abcd=high&hijk=low#anchor",
		"T\u0300he qui\u0300ck 100.1 brown" + Utility.LINE_SEPARATOR + "\u0300foxes. And the beginning. \"Hi?\" Nope! or not.",
		"compareLine"
	},{
		"SentenceBreak",
		"test",
		"T\u0300he qui\u0300ck 100.1 brown" + Utility.LINE_SEPARATOR + "\u0300foxes. And the beginning. \"Hi?\" Nope! or not.",
		"compareSentence"
	},{
		"WordBreak",
		"test",
		"T\u0300he qui\u0300ck 100.1 brown" + Utility.LINE_SEPARATOR + "\u0300foxes.",
		"compareWord"
	}};
}
