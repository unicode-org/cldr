/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Test class for trying different approaches to flexible date/time
 */
public class FlexibleDateTime {
	static String VARIABLES = "GyYuMwWdDFgEeahHKkmsSAzZ";
	List rules = new ArrayList();
	Map currentVariables = new HashMap();
	
	private class Rule {
		List matchSet = new ArrayList();
		String name;
		String format;
		//boolean hasVariables;
		
		public String toString() {
			return name + " <= " + format;
		}
		
		private Rule(String rule) {
			int eqPos = rule.indexOf('=');
			name = rule.substring(0, eqPos).trim();
			rule = format = rule.substring(eqPos+1);
			boolean inVariable = false;
			int startVariable = -1;
			for (int i = 0; i < rule.length(); ++i) {
				char ch = rule.charAt(i); // ok to not use UTF16, since only care about {...}
				if (startVariable >= 0) {
					if (ch == '}') {
						String variable = rule.substring(startVariable, i);
						matchSet.add(variable);
						//if (!hasVariables && isVariable(variable)) hasVariables = true;
						startVariable = -1;
					}
				} else {
					if (ch == '{') {
						startVariable = i+1;
					}
				}
			}
		}
		
		private boolean isVariable (String s) {
			if (s.length() == 0) return false;
			char c = s.charAt(0);
			if (VARIABLES.indexOf(c) < 0) return false;
			for (int i = 1; i < s.length(); ++i) {
				if (c != s.charAt(i)) return false;
			}
			return true;
		}
		
		private boolean process() {
			// special check. If there are pieces, and 
			// if there is a match, remove the items from the pieces, and reset the variable
			Map foundSoFar = new HashMap();
			for (int i = 0; i < matchSet.size(); ++i) {
				String matchItem = (String) matchSet.get(i);
				String part = matches(matchItem);
				if (part == null) return false;
				foundSoFar.put(matchItem, part); // e.g. h* -> hh
			}
			// we have a complete match. So substitute in the format, remove from pieces, and set the variable
			String format2 = replace(format, foundSoFar);
			// remove all (should be on Map!)
			for (Iterator it = foundSoFar.values().iterator(); it.hasNext();) {
				currentVariables.remove(it.next());
			}
			addVariable(name, format2);
			return true;
		}
		
		/**
		 * Dumb implementation for now.
		 * @param format2
		 * @param foundSoFar
		 * @return
		 */
		private String replace(String format2, Map foundSoFar) {
			for (Iterator it = foundSoFar.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				String value = (String) foundSoFar.get(key);
				format2 = replace(format2, '{' + key + '}', (String)currentVariables.get(value));
			}
			return format2;
		}

		/**
		 * @param format2
		 * @param key
		 * @param value
		 */
		private String replace(String format2, String key, String value) {
			int pos = 0;
			while (true) {
				pos = format2.indexOf(key, pos);
				if (pos < 0) return format2;
				format2 = format2.substring(0, pos) + value + format2.substring(pos+key.length());
				pos += value.length();
			}
		}

		private String matches(String matcher) {
			Matcher m = Pattern.compile(matcher).matcher("");
			for (Iterator it = currentVariables.keySet().iterator(); it.hasNext();) {
				String part = (String)it.next();
				if (m.reset(part).matches()) return part;
			}
			return null;
		}

	}
	private FlexibleDateTime() {}
	
	static FlexibleDateTime make (String[] data) {
		FlexibleDateTime result = new FlexibleDateTime();
		for (int i = 0; i < data.length; ++i) {
			result.addRule(data[i]);
		}
		return result;
	}

	/**
	 * @param string
	 */
	private void addRule(String string) {
		rules.add(new Rule(string));
	}

	/* en
	 * EEEE, MMMM d, yyyy
	 * M/d/yy
	 * h:mm:ss a z
	 * 
	 * da
	 * EEEE dd MMMM yyyy
	 * d. MMM yyyy
	 * dd-MM-yyyy
	 * 
	 * ru
	 * d MMMM yyyy '?.'
	 * dd.MM.yyyy
	 * H:mm:ss z
	 * 
	 * 
	 * 
h 1..2 11 Hour [1-12].  
H 1..2 13 Hour [0-23]. 
K 1..2 0 Hour [0-11]. 
k 1..2 24 Hour [1-24]. 

	 */
	
	public static void main(String[] args) {
		String[] data = {
				"date={d+}-{MM}-{y+}",
				"date={d+}. {MMM}",
				"date={d+} {M+}",
				"date={E+} {date}",
				"date={y+} {date}",
				"date={date} {G+}",
				"min={mm}:{ss}",
				"min={mm}",
				"time={hh}:{min}",
				"date={dd}[{hh}]",
				"datetime={date}, {time}",			
				"datetime={time}",			
				"datetime={date}",			
				// default is to pull off each item in order and follow with space.
		};
		
		String[] testData = {
				"hhmm",
				"MMddd",
				"ddhh",
				"yyyyMMMd",
				"yyyyMMddhhmmss",
				"GEEEEyyyyMMddhhmmss",
		};
		FlexibleDateTime fdt = FlexibleDateTime.make(data);
		Date now = new Date(99,11,23,1,2,3);
		for (int i = 0; i < testData.length; ++i) {
			String dfpattern = fdt.getDateFormatPattern(testData[i]);;
			System.out.println("Raw pattern: " + testData[i] + "\t=>\tLocalized Pattern: " + dfpattern);
			DateFormat df = new SimpleDateFormat(dfpattern);
			System.out.println("\tSample Input: " + now + "\t=>\tSample Results: " + df.format(now));
	}
}
	/**
	 * @param string
	 * @return
	 */
	private String getDateFormatPattern(String string) {
		getParts(string);
		String result;
		main:
	    while (true) {
			for (int i = 0; i < rules.size(); ++i) {
				Rule r = (Rule) rules.get(i);
				if (r.process()) {
					continue main; // if we get a match, start over
				}
			}
			break;
			/*
			if (currentVariables.size() > 1) {
				if (currentVariables.size() > 1) {
					throw new IllegalArgumentException(); // rule failure
				}
				continue;
			}
			// if we wound up here, then there are remaining pieces, but we can't match them.
			// pull one off and add it with intervening space
			String part = (String) pieces.get(0);
			pieces.remove(0);
			String dateValue = (String) currentVariables.get("{date}");
			if (dateValue == null) {
				currentVariables.put("{date}", part);
			} else {
				currentVariables.put("{date}", dateValue + ' ' + part);
			}
			*/
	    }
		return (String) currentVariables.get("datetime");
	}
	
	/**
	 * See if string occurs in list
	 * @param string
	 * @param pieces
	 * @return
	 */
	// TODO put in order, check for duplicates
	private void getParts(String string) {
		currentVariables.clear();
		if (string.length() == 0) return;
		int start = 1;
		int lastPos = 0;
		char last = string.charAt(lastPos);
		for (int i = 1; i < string.length(); ++i) {
			char ch = string.charAt(i);
			if (ch == last) continue;
			String part = string.substring(lastPos, i);
			addVariable(part, part);
			lastPos = i;
			last = ch;			
		}
		String part = string.substring(lastPos, string.length());
		addVariable(part, part);
	}
	
	/**
	 * @param format2
	 */
	private void addVariable(String name, String format2) {
		if (DEBUG) System.out.println("Adding " + name + "  " + format2);
		currentVariables.put(name, format2);
	}
	static final boolean DEBUG = false;
}