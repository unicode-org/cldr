/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Value;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Test class for trying different approaches to flexible date/time
 */
public class FlexibleDateTime {
	static boolean SHOW_MATCHING = false;
	static boolean SHOW2 = false;

	List rules = new ArrayList();
	Map currentVariables = new LinkedHashMap();
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for(Iterator it = rules.iterator(); it.hasNext();) {
			if (first) first = false;
			else buffer.append("\r\n");
			buffer.append(it.next());
		}
		return buffer.toString();
	}

	static class OOConverter {
		FormatParser fp = new FormatParser();
		
		public String convertOODate(String source, String locale) {
			if (source.length() == 0) return "";
			source = source.replace('"', '\''); // fix quoting convention
			StringBuffer buffer = new StringBuffer();
			fp.set(source);
			for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
				Object item = it.next();
				if (item instanceof VariableField) {
					buffer.append(handleOODate(item.toString(), locale));
				} else {
					buffer.append(item);
				}
			}
			return buffer.toString();
		}
	
		private String handleOODate(String string, String locale) {
			// preprocess hack for *localized* strings
			if (locale.startsWith("de")) {
				if (string.startsWith("T")) string = string.replace('T','D');
				if (string.startsWith("J")) string = string.replace('J','Y');
			} else if (locale.startsWith("nl")) {
				if (string.startsWith("J")) string = string.replace('J','Y');
			} else if (locale.startsWith("fi")) {
				if (string.startsWith("K")) string = string.replace('K','M');
				if (string.startsWith("V")) string = string.replace('V','Y');
				if (string.startsWith("P")) string = string.replace('P','D');
			} else if (locale.startsWith("fr")) {
				if (string.startsWith("J")) string = string.replace('J','D');
				if (string.startsWith("A")) string = string.replace('A','Y');
			} else if (locale.startsWith("es") || locale.startsWith("pt")) {
				if (string.startsWith("A")) string = string.replace('A','Y');
			} else if (locale.startsWith("it")) {
				if (string.startsWith("A")) string = string.replace('A','Y');
				if (string.startsWith("G")) string = string.replace('G','D');
			}
			if (string.startsWith("M")) return string;
			if (string.startsWith("A")) return string.replace('A','y'); // best we can do for now
			if (string.startsWith("Y") || string.startsWith("W") || 
					string.equals("D") || string.equals("DD")) return string.toLowerCase();
			if (string.equals("DDD") || string.equals("NN")) return "EEE";
			if (string.equals("DDDD") || string.equals("NNN")) return "EEEE";
			if (string.equals("NNNN")) return "EEEE, ";
			if (string.equals("G")) return "G"; // best we can do for now
			if (string.equals("GG")) return "G";
			if (string.equals("GGG")) return "G"; // best we can do for now
			if (string.equals("E")) return "y";
			if (string.equals("EE") || string.equals("R")) return "yy";
			if (string.equals("RR")) return "Gyy";
			if (string.startsWith("Q")) return '\'' + string + '\'';
			char c = string.charAt(0);
			if (c < 0x80 && UCharacter.isLetter(c)) return string.replace(c,'x');
			return string;
		}
	
		public String convertOOTime(String source, String locale) {
			if (source.length() == 0) return "";
			source = source.replace('"', '\''); // fix quoting convention
			int isAM = source.indexOf("AM/PM");
			if (isAM >= 0) {
				source = source.substring(0,isAM) + "a" + source.substring(isAM+5);
			}
			StringBuffer buffer = new StringBuffer();
			fp.set(source);
			for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
				Object item = it.next();
				if (item instanceof VariableField) {
					buffer.append(handleOOTime(item.toString(), locale, isAM >= 0));
				} else {
					buffer.append(item);
				}
			}
			return buffer.toString();
		}
		
		private String handleOOTime(String string, String locale, boolean isAM) {
			char c = string.charAt(0);
			switch (c) {
			case 'h': case 'H': case 't': case 'T': case 'u': case 'U':
				return string.replace(c, isAM ? 'h' : 'H');
			case 'M': case 'S': return string.toLowerCase();
			case '0': return string.replace('0','S'); // ought to be more sophisticated, but this should work for normal stuff.
			case 'a': case 's': case 'm': return string; // ok as is
			default: return "x"; // cause error
			}
		}
		private String convertToRule(String string) {
			fp.set(string);
			StringBuffer buffer = new StringBuffer();
			Set additions = new HashSet();
			for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
				Object item = it.next();
				if (item instanceof VariableField) {
					String s = item.toString();
					if (s.startsWith("a")) {
						buffer.append(s);
					} else {
						buffer.append('{' + s + '}');
					}
				} else {
					buffer.append(item);
				}
			}
			for (Iterator it = additions.iterator(); it.hasNext();) {
				buffer.insert(0,it.next());
			}
			return buffer.toString();
		}
	}
	static Date TEST_DATE = new Date(104,8,13,23,58,59);
	
	static Comparator VariableFieldComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			Collection a = (Collection)o1;
			Collection b = (Collection)o2;
			if (a.size() != b.size()) {
				if (a.size() < b.size()) return 1;
				return -1;
			}
			Iterator itb = b.iterator();
			for (Iterator ita = a.iterator(); ita.hasNext();) {
				String aa = (String) ita.next();
				String bb = (String) itb.next();
				int result = -aa.compareTo(bb);
				if (result != 0) return result;
			}
			return 0;
		}
	};
	
	static void getOOData() {
		OOConverter ooConverter = new OOConverter();
		Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\open_office\\main\\", ".*");
		Set locales = cldrFactory.getAvailable();
		Map dateList = new TreeMap(VariableFieldComparator);
		Map timeList = new TreeMap(VariableFieldComparator);
		List ruleList = new ArrayList();
		Map patterns = new LinkedHashMap();
		FormatParser fp = new FormatParser();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			System.out.println();
			System.out.println(locale);
			CLDRFile item = cldrFactory.make(locale, false);
			timeList.clear();
			dateList.clear();
			patterns.clear();
			for (Iterator it2 = item.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				if (xpath.indexOf("/special") >= 0) continue;
				boolean isDate = xpath.indexOf("/dateFormat/") >= 0 || xpath.indexOf("/dateFormat[@") >= 0;
				boolean isTime = xpath.indexOf("/timeFormat/") >= 0 || xpath.indexOf("/timeFormat[@") >= 0;
				if (isDate || isTime) {
					Value value = item.getValue(xpath);
					String pattern = value.getStringValue();
					String oldPattern = pattern;
					if (oldPattern.indexOf('[') >= 0) continue;
					pattern = isDate ? ooConverter.convertOODate(pattern, locale) 
							: ooConverter.convertOOTime(pattern, locale);
					if (SHOW2) System.out.print("\t" + (isDate ? "Date" : "Time") + ": " + oldPattern + "\t" + pattern + "\t");
					try {
						DateFormat d = new SimpleDateFormat(pattern);
						if (SHOW2) System.out.print(d.format(TEST_DATE));
						fp.set(pattern);
						Object original;
						Collection fields = fp.getFields(null);
						if (fields.size() == 0) {
							System.out.println("\tempty fields " + pattern);
							continue;
						}
						if (isDate) original = putNoReplace(dateList, fields, pattern);
						else original = putNoReplace(timeList, fields, pattern);
						if (original != null) {
							System.out.println("\tnot overriding " + original + " with " + pattern);
							continue;
						}
						patterns.put(oldPattern, pattern);
					} catch (Exception e) {
						if (SHOW2) System.out.print(e.getLocalizedMessage());
					}
					if (SHOW2) System.out.println();
				}
			}
			ruleList.clear();
			for (Iterator it2 = dateList.keySet().iterator(); it2.hasNext();) {
				String p = dateList.get(it2.next()).toString();
				System.out.println("\t\t<pattern>" + p + "</pattern>");
				ruleList.add("date=" + ooConverter.convertToRule(p));
			}
			for (Iterator it2 = timeList.keySet().iterator(); it2.hasNext();) {
				String p = timeList.get(it2.next()).toString();
				System.out.println("\t\t<pattern>" + p + "</pattern>");
				ruleList.add("time=" + ooConverter.convertToRule(p));
			}
			FlexibleDateTime fdt = FlexibleDateTime.make(ruleList);
			System.out.println(fdt);
			for (Iterator it2 = patterns.keySet().iterator(); it2.hasNext();) {
				String op = (String) it2.next();
				String p = (String) patterns.get(op);
				String key = fp.set(p).getFieldString();
				if (key.length() == 0) continue;
				try {
					String result = fdt.getDateFormatPattern(key);
					if (!p.equals(result)) { // .replace('h', 'H')
						System.out.println("\tno round trip: " + op + "\t=>\t" + p + "\t=>\t" + key + "\t=>\t" + result);
						boolean oldValue = SHOW_MATCHING;
						SHOW_MATCHING = true;
						result = fdt.getDateFormatPattern(key);
						SHOW_MATCHING = oldValue;
					} else {
						try {
							String formatted = new SimpleDateFormat(result).format(TEST_DATE);
							System.out.println("testing: " + op + "\t=>\t" + p + "\t=>\t" + key + "\t=>\t" + result
									+ "\t=>\t" + formatted);
							//System.out.println(\t<datetimeFormatp + "\t=>\t" + key);
						} catch (RuntimeException e1) {
							System.out.println("testing: " + op + "\t=>\t" + p + "\t=>\t" + key + "\t=>\t" + result
									+ "\t=>\t" + e1.getMessage());
						}
					}
				} catch (RuntimeException e) {
					System.out.println("\tfailure with " + op + "\t=>\t" + p + "\t=>\t" + key + "\t=>\t" + e.getMessage());
				}
			}				
		}
	}
	
	private static Object putNoReplace(Map m, Object key, Object value) {
		Object current = m.get(key);
		if (current != null) return current;
		m.put(key, value);
		return null;
	}

	/**
	 * @param ruleList
	 * @return
	 */
	private static FlexibleDateTime make(List ruleList) {
		String[] data = new String[ruleList.size()];
		ruleList.toArray(data);
		return make(data);
	}

	private static class Variable {
		String variable;
		Variable(String variable) {
			this.variable = variable;
		}
		public String toString() {
			return "{" + variable + "}";
		}
	}

	private class Rule {
		String name;
		String original;
		List matchSet = new ArrayList();
		List format = new ArrayList();
		//boolean hasVariables;
		
		public String toString() {
			return "{" + matchSet + "} in rule: " + name + " <= " + format + " (" + original + ")";
		}
		
		private Rule(String rule) {
			original = rule;
			int eqPos = rule.indexOf('=');
			name = rule.substring(0, eqPos).trim();
			rule = rule.substring(eqPos+1);
			boolean inVariable = false;
			int startVariable = -1;
			String formatPiece = "";
			for (int i = 0; i < rule.length(); ++i) {
				char ch = rule.charAt(i); // ok to not use UTF16, since only care about {...}
				if (startVariable >= 0) {
					if (ch == '}') {
						String variable = rule.substring(startVariable, i);
						if (variable.startsWith("*")) {
							variable = variable.substring(1);
						} else {
							format.add(new Variable(variable));
						}
						matchSet.add(variable);
						//if (!hasVariables && isVariable(variable)) hasVariables = true;
						startVariable = -1;
					}
				} else {
					if (ch == '{') {
						startVariable = i+1;
						if (formatPiece.length() != 0) {
							format.add(formatPiece);
							formatPiece = "";
						}						
					} else {
						formatPiece += ch;
					}
				}
			}
			if (formatPiece.length() != 0) format.add(formatPiece);
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
			// special check. We don't replace a name unless it is either found in the parts, or empty.
			if (foundSoFar.get(name) == null && currentVariables.get(name) != null) {
				return false;
			}
			// special check. We don't match date/time unless all of it matches
			if (name.equals("datetime")) {
				if (foundSoFar.size() != currentVariables.size()) return false;
			}
			
			Map old = null;
			if (SHOW_MATCHING) {
				old = new LinkedHashMap(currentVariables);
			}
			
			// we have a complete match. So substitute in the format, remove from pieces, and set the variable
			String format2 = replace(format, foundSoFar);
			// remove all (should be on Map!)
			for (Iterator it = foundSoFar.values().iterator(); it.hasNext();) {
				currentVariables.remove(it.next());
			}
			addVariable(name, format2);

			if (SHOW_MATCHING) {
				//LinkedHashSet s = new LinkedHashSet(currentVariables.keySet());
				//LinkedHashSet f = new LinkedHashSet(foundSoFar.values());
				//s.removeAll(f);
				System.out.println(old + ", \t" + foundSoFar.values() + " matched " + this + " \tResults: " + currentVariables);
			}

			return true;
		}
		
		/**
		 * Dumb implementation for now.
		 * @param formatList
		 * @param foundSoFar
		 * @return
		 */
		private String replace(List formatList, Map foundSoFar) {
			StringBuffer result = new StringBuffer();
			for (Iterator it = formatList.iterator(); it.hasNext();) {
				Object part = it.next();
				if (part instanceof String) {
					result.append(part);
				} else {
					String key = ((Variable)part).variable;
					String value = (String) foundSoFar.get(key);
					String variable = (String)currentVariables.get(value);
					result.append(variable);
				}
			}
			return result.toString();
		}

		/**
		 * @param format2
		 * @param key
		 * @param value
		 */
		/*
		private String replace(String format2, String key, String value) {
			int pos = 0;
			while (true) {
				pos = format2.indexOf(key, pos);
				if (pos < 0) return format2;
				format2 = format2.substring(0, pos) + value + format2.substring(pos+key.length());
				pos += value.length();
			}
		}
		*/

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
	
	static String[] FALLBACK_RULES ={
			"date={G+|y+|Y+|u+|M+|w+|W+|d+|D+|F+|g+|E+}",
			"date={date} {G+|y+|Y+|u+|M+|w+|W+|d+|D+|F+|g+|E+}",
			"time={a+|H+|m+|s+|S+|A+|z+|Z+}",
			"time={time} {a+|H+|m+|s+|S+|A+|z+|Z+}",
			"datetime={time|date}",
	};
	
	static FlexibleDateTime make (String[] data) {
		FlexibleDateTime result = new FlexibleDateTime();
		for (int i = 0; i < data.length; ++i) {
			result.addRule(data[i]);
		}
		for (int i = 0; i < FALLBACK_RULES.length; ++i) {
			result.addRule(FALLBACK_RULES[i]);
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
		getOOData();
		String[] data = {
				"yyyy={*y+}yyyy", // force all years to be 4 letters
				"time={s+}.{S+}",
				"time={m+}:{time}",
				"time={*H+}hh:{time} a",
				"date={*d+}dd-{MM}-{y+}",
				"date={d+}. {MMM}",
				"date={d+} {M+}",
				"date={d+} [{H+}]",
				"date={date} {M+}",
				"date={date} {y+}",
				"date={date} {G+}",
				"date={E+} {date}",
				"datetime={date}, {time}",			
		};
		
		String[] testData = {
				"dMMy",
				"HHmm",
				"yyyyHHmm",
				"MMdd",
				"ddHH",
				"yyyyMMMd",
				"yyyyMMddHHmmss",
				"GEEEEyyyyMMddHHmmss",
				"GyyyyYYYYuuuuMMMMwwWddDDDFgEEEEaHHmmssSSSAAAAAAAzZ",
		};
		FlexibleDateTime fdt = FlexibleDateTime.make(data);
		Date now = new Date(99,11,23,1,2,3);
		for (int i = 0; i < testData.length; ++i) {
			System.out.println("Raw pattern: " + testData[i]);
			String dfpattern = fdt.getDateFormatPattern(testData[i]);
			System.out.println("\t=>\tLocalized Pattern: " + dfpattern);
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
	    	// start from the beginning whenever we get a match
			for (int i = 0; i < rules.size(); ++i) {
				Rule r = (Rule) rules.get(i);
				if (r.process()) {
					continue main; // if we get a match, start over
				}
			}
			break; // stop when we hit no matchs on the way through
	    }
		// we made it all the way through. There ought to be nothing but a datetime left.
		if (currentVariables.size() != 1) throw new IllegalArgumentException("Failed generation: " + currentVariables);
		
		return (String) currentVariables.get("datetime");
	}
	
	static class VariableField {
		private String string;
		VariableField(String string) {
			this.string = string;
		}
		public String toString() {
			return string;
		}
	}
	static class FormatParser {
		private List items = new ArrayList();
		private char quoteChar = '\'';
		
		FormatParser set(String string) {
			items.clear();
			if (string.length() == 0) return this;
			int start = 1;
			int lastPos = 0;
			char last = string.charAt(lastPos);
			boolean lastIsVar = isVariableField(last);
			boolean inQuote = last == quoteChar;;
			// accumulate any sequence of unquoted ASCII letters as a variable
			// anything else as a string (with quotes retained)
			for (int i = 1; i < string.length(); ++i) {
				char ch = string.charAt(i);
				if (ch == quoteChar) {
					inQuote = !inQuote;
				}
				boolean chIsVar = !inQuote && isVariableField(ch);
				// break between ASCII letter and any non-equal letter
				if (ch == last && lastIsVar == chIsVar) continue;
				String part = string.substring(lastPos, i);
				if (lastIsVar) {
					items.add(new VariableField(part));
				} else {
					items.add(part);
				}
				lastPos = i;
				last = ch;
				lastIsVar = chIsVar;
			}
			String part = string.substring(lastPos, string.length());
			if (lastIsVar) {
				items.add(new VariableField(part));
			} else {
				items.add(part);
			}
			return this;
		}
		/**
		 * @param pattern
		 * @param newParam TODO
		 * @return
		 */
		public Collection getFields(Collection output) {
			if (output == null) output = new TreeSet();
			main:
			for (Iterator it = items.iterator(); it.hasNext();) {
				Object item = it.next();
				if (item instanceof VariableField) {
					String s = item.toString();
					switch(s.charAt(0)) {
						case 'Q': continue main; // HACK
						case 'a': continue main; // remove
						//case 'h': s = s.replace('h', 'H'); break;
						case 'k': s = s.replace('k', 'H'); break;
						case 'K': s = s.replace('K', 'h'); break;
					}
					output.add(s);
				}
			}
			//System.out.println(output);
			return output;
		}
		/**
		 * @return
		 */
		public String getFieldString() {
			Set set = (Set)getFields(null);
			StringBuffer result = new StringBuffer();
			for (Iterator it = set.iterator(); it.hasNext();) {
				String item = (String) it.next();
				result.append(item);
			}
			return result.toString();
		}
		/**
		 * @param last
		 * @return
		 */
		private boolean isVariableField(char last) {
			return last <= 'z' && last >= '0' && (last <= '9' || last >= 'a' || (last >= 'A' && last <= 'Z'));
		}
		public List getItems() {
			return Collections.unmodifiableList(items);
		}
	}
	
	/**
	 * See if string occurs in list
	 * @param string
	 * @param pieces
	 * @return
	 */
	// TODO put in order, check for duplicates
	private void getParts(String string) {
		String[] weights = new String[VARIABLES.size()];
		currentVariables.clear();
		if (string.length() == 0) return;
		int start = 1;
		int lastPos = 0;
		char last = string.charAt(lastPos);
		for (int i = 1; i < string.length(); ++i) {
			char ch = string.charAt(i);
			if (ch == last) continue;
			VARIABLES.getPart(string.substring(lastPos, i), weights);
			lastPos = i;
			last = ch;			
		}
		VARIABLES.getPart(string.substring(lastPos, string.length()), weights);
		// add to the list in REVERSE sorted order. Will unreverse later
		for (int i = weights.length - 1; i >= 0; --i) {
			if (weights[i] == null) continue;
			addVariable(weights[i], weights[i]);
		}
	}
	
	static class VariableList {
		String items = "";
		int[] maxlengths = new int[50];
		VariableList add(char c, int maxLen) {
			maxlengths[items.length()] = maxLen;
			items += c;
			return this;
		}
		/**
		 * @param part
		 * @return
		 */
		public void getPart(String part, String[] weights) {
			int weight = find(part.charAt(0), part.length());
			if (weight < 0) throw new IllegalArgumentException("Illegal Field: " + part);
			weights[weight] = part;
		}
		int find(char c, int len) {
			int weight = items.indexOf(c);
			if (weight < 0) return -1;
			if (len < 1 || len > maxlengths[weight]) return -1;
			return weight;
		}
		int size() {
			return items.length();
		}
	}
	
	static VariableList VARIABLES = new VariableList()
		.add('G', 1)
		.add('y', 5)
		.add('Y', 5)
		.add('u', 5)
		.add('M', 5)
		.add('w', 2)
		.add('W', 1)
		.add('d', 2)
		.add('D', 3)
		.add('F', 1)
		.add('g', Integer.MAX_VALUE)
		.add('E', 5)
		.add('a', 1)
		.add('H', 2)
		.add('h', 2)
		.add('m', 2)
		.add('s', 2)
		.add('S', Integer.MAX_VALUE)
		.add('A', Integer.MAX_VALUE)
		.add('z', 4)
		.add('Z', 2);

	/**
	 * @param format2
	 */
	private void addVariable(String name, String format2) {
		if (DEBUG) System.out.println("Adding " + name + "  " + format2);
		currentVariables.put(name, format2);
	}
	static final boolean DEBUG = false;
}
