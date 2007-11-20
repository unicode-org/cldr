/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 * 
 */

package org.unicode.cldr.util;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodeMap.Composer;

/**
 * Ordered list of rules, with variables resolved before building. Use Builder to make.
 */
public class Segmenter {
	private static final boolean JDK4HACK = true;
	public static final int REGEX_FLAGS = Pattern.COMMENTS | Pattern.MULTILINE | Pattern.DOTALL;
	private static final boolean SHOW_VAR_CONTENTS = true;
	private UnicodeMap samples = new UnicodeMap();
	/**
	 * If not null, maskes off the character properties so the UnicodeSets are easier to use when debugging.
	 */
	public static final UnicodeSet DEBUG_RETAIN = null; // new UnicodeSet("[\u0000-\u00FF\u0300-\u0310]"); // null;
	
	static public interface CodePointShower {
		String show(int codePoint);
	}
	
	public static Builder make(UnicodeProperty.Factory factory, String type) {
		Builder b = new Builder(factory);
		for (int i = 0; i < cannedRules.length; ++i) {
			if (cannedRules[i][0].equals(type)) {
				for (int j = 1; j < cannedRules[i].length; ++j) {
					b.addLine(cannedRules[i][j]);
				}
				return b;
			}
		}
		return null;
	}
	
	
	/**
	 * Certain rules are generated, and have artificial numbers
	 */
	public static final double NOBREAK_SUPPLEMENTARY = 0.1, BREAK_SOT = 0.2, BREAK_EOT = 0.3, BREAK_ANY = 999;
	/**
	 * Convenience for formatting doubles
	 */
	public static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
	static {
		nf.setMinimumFractionDigits(0);
	}
	
	/**
	 * Does the rule list give a break at this point? 
	 * Also sets the rule number that matches, for return by getBreakRule.
	 * @param text
	 * @param position
	 * @return
	 */
	public boolean breaksAt(CharSequence text, int position) {
		if (position == 0) {
			breakRule = BREAK_SOT;
			return true;
		}
		if (position == text.length()) {
			breakRule = BREAK_EOT;
			return true;
		}
		// don't break in middle of surrogate
		if (UTF16.isLeadSurrogate(text.charAt(position-1)) && UTF16.isTrailSurrogate(text.charAt(position))) {
			breakRule = NOBREAK_SUPPLEMENTARY;
			return false;
		}
		for (int i = 0; i < rules.size(); ++i) {
			Rule rule = (Rule)rules.get(i);
			if (false && rule.toString().indexOf("$H2") >= 0) {
				System.out.println("Debug");
			}
			byte result = rule.matches(text, position);
			if (result != Rule.UNKNOWN_BREAK) {
				breakRule = ((Double)orders.get(i)).doubleValue();
				return result == Rule.BREAK;
			}
		}
		breakRule = BREAK_ANY;
		return true; // default
	}
	public int getRuleStatusVec(int[] ruleStatus) {
		ruleStatus[0] = 0;
		return 1;
	}
	/**
	 * Add a numbered rule.
	 * @param order
	 * @param rule
	 */
	public void add(double order, Rule rule) {
		orders.add(new Double(order));
		rules.add(rule);
	}
	public Rule get(double order) {
		int loc = orders.indexOf(new Double(order));
		if (loc < 0) return null;
		return (Rule) rules.get(loc);
	}
	/**
	 * Gets the rule number that matched at the point. Only valid after calling breaksAt
	 * @return
	 */
	public double getBreakRule() {
		return breakRule;
	}
	/**
	 * Debugging aid
	 */
	public String toString() {
		return toString(false);
	}
	public String toString(boolean showResolved) {
		String result = "";
		for (int i = 0; i < rules.size(); ++i) {
			if (i != 0) result += "\r\n";
			result += orders.get(i) + ")\t" + ((Rule)rules.get(i)).toString(showResolved);
		}
		return result;
	}
	
	/**
	 * A rule that determines the status of an offset.
	 */
	public static class Rule {
		/**
		 * Status of a breaking rule
		 */
		public static final byte NO_BREAK = -1, UNKNOWN_BREAK = 0, BREAK = 1;
		
		/**
		 * @param before pattern for the text after the offset. All variables must be resolved.
		 * @param result the break status to return when the rule is invoked
		 * @param after pattern for the text before the offset. All variables must be resolved.
		 * @param line 
		 */
		public Rule(String before, byte result, String after, String line) {
			breaks = result;
			before = ".*(" + before + ")";
			String parsing = null;
			try {
				matchPrevious = Pattern.compile(parsing = before, REGEX_FLAGS).matcher("");
				matchSucceeding = Pattern.compile(parsing = after, REGEX_FLAGS).matcher("");
			} catch (PatternSyntaxException e) {
				// Format: Unclosed character class near index 927
				int index = e.getIndex();
				throw (RuntimeException)new IllegalArgumentException("On <" + line + ">, Can't parse: " + parsing.substring(0,index)
						+ "<<<>>>" + parsing.substring(index))
						.initCause(e);
			} catch (RuntimeException e) {
				// Unclosed character class near index 927
				throw (RuntimeException)new IllegalArgumentException("On <" + line + ">, Can't parse: " + parsing)
				.initCause(e);
			}
			name = line; 
			resolved = Utility.escape(before) + (result == NO_BREAK ? " \u00D7 " : " \u00F7 ") + Utility.escape(after);
			// COMMENTS allows whitespace
		}
		
		//Matcher numberMatcher = Pattern.compile("[0-9]+").matcher("");
		
		/**
		 * Match the rule against text, at a position
		 * @param text
		 * @param position
		 * @return break status
		 */
		public byte matches(CharSequence text, int position) {
			if (!matchAfter(matchSucceeding, text, position)) return UNKNOWN_BREAK;
			if (!matchBefore(matchPrevious, text, position)) return UNKNOWN_BREAK;
			return breaks;
		}
		/**
		 * Debugging aid
		 */
		public String toString() {
			return toString(false);
		}
		public String toString(boolean showResolved) {
			String result = name;
			if (showResolved) result += ": " + resolved;
			return result;
		}
		
		//============== Internals ================
		// in Java 5, this can be more efficient, and use a single regex
		// of the form "(?<= before) after". MUST then have transparent bounds
		private Matcher matchPrevious;
		private Matcher matchSucceeding;
		private String name;
		private String resolved;
		private byte breaks;		
	}
	
	/**
	 * utility, since we are using Java 1.4
	 */
	static boolean matchAfter(Matcher matcher, CharSequence text, int position) {
		return matcher.reset(text.subSequence(position, text.length())).lookingAt();
	}
	
	/**
	 * utility, since we are using Java 1.4
	 * depends on the pattern having been built with .*
	 * not very efficient, works for testing and the best we can do.
	 */
	static boolean matchBefore(Matcher matcher, CharSequence text, int position) {
		return matcher.reset(text.subSequence(0, position)).matches();
	}
	
	/**
	 * Separate the builder for clarity
	 */
	
	/**
	 * Sort the longest strings first. Used for variable lists.
	 */
	static Comparator LONGEST_STRING_FIRST = new Comparator() {
		public int compare(Object arg0, Object arg1) {
			String s0 = arg0.toString();
			String s1 = arg1.toString();
			int len0 = s0.length();
			int len1 = s1.length();
			if (len0 < len1) return 1; // longest first
			if (len0 > len1) return -1;
			// lengths equal, use string order
			return s0.compareTo(s1);
		}
	};
	
	/**
	 * Used to build RuleLists. Can be used to do inheritance, since (a) adding a variable overrides any previous value, and
	 * any variables used in its value are resolved before adding, and (b) adding a rule sorts/overrides according to numeric value.
	 */
	public static class Builder {
		private UnicodeProperty.Factory propFactory;
		private XSymbolTable symbolTable;
		private List rawVariables = new ArrayList();
		private Map xmlRules = new TreeMap();
		private Map htmlRules = new TreeMap();
		private List lastComments = new ArrayList();
		private UnicodeMap samples = new UnicodeMap();
		
		public Builder(UnicodeProperty.Factory factory) {
			propFactory = factory;
			symbolTable = new MyXSymbolTable(); // propFactory.getXSymbolTable();
			htmlRules.put(new Double(BREAK_SOT), "sot \u00F7");
			htmlRules.put(new Double(BREAK_EOT), "\u00F7 eot");
			htmlRules.put(new Double(BREAK_ANY), "\u00F7 Any");
		}
		
		// copied to make independent of ICU4J internals
        private class MyXSymbolTable extends UnicodeSet.XSymbolTable {
            public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            	if (false) System.out.println(propertyName + "=" + propertyValue);
            	UnicodeProperty prop = propFactory.getProperty(propertyName);
            	if (prop == null) return false;
            	result.clear();
            	UnicodeSet x = prop.getSet(propertyValue, result);
                return x.size() != 0;
            }
        }

		public String toString(String testName, String indent) {
			
			StringBuffer result = new StringBuffer();
			result.append(indent + "<segmentation type=\"" + testName + "\">").append("\r\n");
			result.append(indent + "\t<variables>").append("\r\n");
			for (int i = 0; i < rawVariables.size(); ++i) {
				result.append(indent + "\t\t").append(rawVariables.get(i)).append("\r\n");
			}
			result.append(indent + "\t</variables>").append("\r\n");
			result.append(indent + "\t<segmentRules>").append("\r\n");
			for (Iterator it = xmlRules.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				result.append(indent + "\t\t").append(xmlRules.get(key)).append("\r\n");
			}
			result.append(indent + "\t</segmentRules>").append("\r\n");
			for (int i = 0; i < lastComments.size(); ++i) {
				result.append(indent + "\t").append(lastComments.get(i)).append("\r\n");
			}
			result.append(indent + "</segmentation>").append("\r\n");
			return result.toString();
		}
		
		/**
		 * Add a line. If contains a =, is a variable definition.
		 * Otherwise, is of the form nn) rule, where nn is the number of the rule.
		 * For now, pretty lame parsing, because we can't easily determine whether =, etc is part of the regex or not.
		 * So any 'real' =, etc in a regex must be expressed with unicode escapes, \\u....
		 * @param line
		 * @return
		 */
		public boolean addLine(String line) {
			// for debugging
			if (line.startsWith("show")) {
				line = line.substring(4).trim();
				System.out.println("# " + line + ": ");
				System.out.println("\t" + replaceVariables(line));
				return false;
			}
			// dumb parsing for now
			if (line.startsWith("#")) {
				lastComments.add("<!-- " + line.substring(1).trim() + " -->");
				return false;
			}
			int relationPosition = line.indexOf('=');
			if (relationPosition >= 0) {
				addVariable(line.substring(0,relationPosition).trim(), line.substring(relationPosition+1).trim());
				return false;
			}
			relationPosition = line.indexOf(')');
			Double order;
			try {
				order = new Double(Double.parseDouble(line.substring(0,relationPosition).trim()));
			} catch (Exception e) {
				throw new IllegalArgumentException("Rule must be of form '1)...': " + line);
			}
			line = line.substring(relationPosition + 1).trim();
			relationPosition = line.indexOf('\u00F7');
			byte breaks = Segmenter.Rule.BREAK;
			if (relationPosition < 0) {
				relationPosition = line.indexOf('\u00D7');
				if (relationPosition < 0) throw new IllegalArgumentException("Couldn't find =, \u00F7, or \u00D7");
				breaks = Segmenter.Rule.NO_BREAK;
			}
			addRule(order, line.substring(0,relationPosition).trim(), breaks, line.substring(relationPosition + 1).trim(), line);		
			return true;
		}
		
		private transient Matcher whiteSpace = Pattern.compile("\\s+", REGEX_FLAGS).matcher("");
		private transient Matcher identifierMatcher = Pattern.compile("[$]\\p{Alpha}\\p{Alnum}*", REGEX_FLAGS).matcher("");
		private transient Matcher brokenIdentifierMatcher = Pattern.compile("[^$\\p{Alpha}]\\p{Alnum}", REGEX_FLAGS).matcher("");
		
		/**
		 * Add a variable and value. Resolves the internal references in the value.
		 * @param name
		 * @param value
		 * @return
		 */
		static boolean SHOW_SAMPLES = false;
		
		static class MyComposer implements UnicodeMap.Composer {
			public Object compose(int codePoint, Object a, Object b) {
				if (a == null) return b;
				if (b == null) return a;
				if (a.equals(b)) return a;
				return a + "_" + b;
			}
		}
		
		static MyComposer myComposer = new MyComposer();
		
		Builder addVariable(String name, String value) {
			if (lastComments.size() != 0) {
				rawVariables.addAll(lastComments);
				lastComments.clear();
			}
			rawVariables.add("<variable id=\"" + name + "\">" 
					+ TransliteratorUtilities.toXML.transliterate(value) + "</variable>");
			if (!identifierMatcher.reset(name).matches()) {
				throw new IllegalArgumentException("Variable name must be $id: '" + name + "'");
			}
			value = replaceVariables(value);
			try {
				parsePosition.setIndex(0);
				UnicodeSet valueSet = new UnicodeSet(value, parsePosition, symbolTable);
				if (parsePosition.getIndex() != value.length()) {
					if (SHOW_SAMPLES) System.out.println(parsePosition.getIndex() + ", " + value.length() + " -- No samples for: " + name + " = " + value);
				} else if (valueSet.size() == 0) {
					if (SHOW_SAMPLES) System.out.println("Empty -- No samples for: " + name + " = " + value);
				} else {
					String name2 = name;
					if (name2.startsWith("$")) name2 = name2.substring(1);
					composeWith(samples, valueSet, name2, myComposer);
					if (SHOW_SAMPLES) {
						System.out.println("Samples for: " + name + " = " + value);
						System.out.println("\t" + valueSet);
					}
				}
			} catch (Exception e) {}; // skip if can't do
			
			if (SHOW_VAR_CONTENTS) System.out.println(name + "=" + value);
			// verify that the value is a valid REGEX
			Pattern.compile(value, REGEX_FLAGS).matcher("");
//			if (false && name.equals("$AL")) {
//			findRegexProblem(value);
//			}
			variables.put(name, value);
			return this;
		}
		
	    public static UnicodeMap composeWith(UnicodeMap target, UnicodeSet set, Object value, Composer composer) {
	    	for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
	    		int i = it.codepoint;
	    		Object v1 = target.getValue(i);
	    		Object v3 = composer.compose(i, v1, value);
	    		if (v1 != v3 && (v1 == null || !v1.equals(v3))) target.put(i, v3);
	    	}
	    	return target;
	    }

		private void findRegexProblem(String value) {
			UnicodeSet us = new UnicodeSet(value);
			// progressively get larger and larger set
			String parsing = null;
			try {
				for (int i = 0; i < us.size(); ++i) {
					UnicodeSet temp = new UnicodeSet(us).retain(0, us.charAt(i));
					parsing = getInsertablePattern(temp);
					Pattern.compile(parsing, REGEX_FLAGS).matcher("");
				}
			} catch (PatternSyntaxException e) {
				// Format: Unclosed character class near index 927
				int index = e.getIndex();
				throw (RuntimeException)new IllegalArgumentException("Can't parse: " + parsing.substring(0,index)
						+ "<<<>>>" + parsing.substring(index))
						.initCause(e);
			}
		}
		/**
		 * Add a numbered rule, already broken into the parts before and after.
		 * @param order
		 * @param before
		 * @param result
		 * @param after
		 * @param line 
		 * @return
		 */
		Builder addRule(Double order, String before, byte result, String after, String line) {
			if (brokenIdentifierMatcher.reset(line).find()) {
				int pos = brokenIdentifierMatcher.start();
				throw new IllegalArgumentException("Illegal identifier at:" + line.substring(0,pos) + "<<>>" + line.substring(pos));
			}
			line = whiteSpace.reset(line).replaceAll(" ");
			// insert comments before current line, in order.
			if (lastComments.size() != 0) {
				double increment = 0.0001;
				double temp = order.doubleValue() - increment*lastComments.size();
				for (int i = 0; i < lastComments.size(); ++i) {
					Double position = new Double(temp);
					if (xmlRules.containsKey(position)) {
						System.out.println("WARNING: Overriding rule " + position);
					}
					xmlRules.put(position, lastComments.get(i));
					temp += increment;
				}
				lastComments.clear();
			}
			htmlRules.put(order, TransliteratorUtilities.toHTML.transliterate(line));
			xmlRules.put(order, "<rule id=\"" + Segmenter.nf.format(order) + "\""
					//+ (flagItems.reset(line).find() ? " normative=\"true\"" : "")
					+ "> " + TransliteratorUtilities.toXML.transliterate(line) + " </rule>");
			rules.put(order, new Segmenter.Rule(replaceVariables(before), result, replaceVariables(after), line));
			return this;	
		}
		
		/**
		 * Return a RuleList from what we have currently.
		 * @return
		 */
		public Segmenter make() {
			Segmenter result = new Segmenter();
			for (Iterator it = rules.keySet().iterator(); it.hasNext();) {
				Double key = (Double)it.next();
				result.add(key.doubleValue(), (Segmenter.Rule)rules.get(key));
			}
			result.samples = samples;
			return result;
		}
		
		// ============== internals ===================
		private Map variables = new TreeMap(LONGEST_STRING_FIRST); // sorted by length, longest first, to make substitution easy
		private Map rules = new TreeMap();
		
		/**
		 * A workhorse. Replaces all variable references: anything of the form $id.
		 * Flags an error if anything of that form is not a variable.
		 * Since we are using Java regex, the properties support
		 * are extremely week. So replace them by literals. 
		 * @param input
		 * @return
		 */
		private String replaceVariables(String input) {
			// to do, optimize
			String result = input;
			int position = -1;
			main:
				while (true) {
					position = result.indexOf('$', position);
					if (position < 0) break;
					for (Iterator it = variables.keySet().iterator(); it.hasNext();) {
						String name = (String)it.next();
						if (result.regionMatches(position, name, 0, name.length())) {
							String value = (String)variables.get(name);
							result = result.substring(0,position) + value + result.substring(position + name.length());
							position += value.length(); // don't allow overlap
							continue main;
						}
					}
					if (identifierMatcher.reset(result.substring(position)).lookingAt()) {
						throw new IllegalArgumentException("Illegal variable at: '" + result.substring(position) + "'");
					}
				}
			// replace properties
			// TODO really dumb parse for now, fix later
			for (int i = 0; i < result.length(); ++i) {
				if (UnicodeSet.resemblesPattern(result, i)) {
					parsePosition.setIndex(i);
					UnicodeSet temp = new UnicodeSet(result, parsePosition, symbolTable);
					String insert = getInsertablePattern(temp);
					result = result.substring(0,i) + insert + result.substring(parsePosition.getIndex());
					i += insert.length() - 1; // skip over inserted stuff; -1 since the loop will add
				}
			}
			return result;
		}
		
		transient ParsePosition parsePosition = new ParsePosition(0);
		
		/**
		 * Transform a unicode pattern into stuff we can use in Java.
		 * @param temp
		 * @return
		 */
		private String getInsertablePattern(UnicodeSet temp) {
			temp.complement().complement();
			if (JDK4HACK) temp.remove(0x10000,0x10FFFF); // TODO Fix with Hack
			if (DEBUG_RETAIN != null) {
				temp.retainAll(DEBUG_RETAIN);
				if (temp.size() == 0) temp.add(0xFFFF); // just so not empty
			}
			
			String result = toPattern(temp, JavaRegexShower);
			// double check the pattern!!
			UnicodeSet reversal = new UnicodeSet(result);
			if (!reversal.equals(temp)) throw new IllegalArgumentException("Failure on UnicodeSet print");
			return result;
		}
		
		static UnicodeSet JavaRegex_uxxx = new UnicodeSet("[[:White_Space:][:defaultignorablecodepoint:]#]"); // hack to fix # in Java
		static UnicodeSet JavaRegex_slash = new UnicodeSet("[[:Pattern_White_Space:]" +
		"\\[\\]\\-\\^\\&\\\\\\{\\}\\$\\:]");		
		static CodePointShower JavaRegexShower = new CodePointShower() {
			public String show(int codePoint) {
				if (JavaRegex_uxxx.contains(codePoint)) {
					if (codePoint > 0xFFFF) {
						return "\\u" + Utility.hex(UTF16.getLeadSurrogate(codePoint))
						+ "\\u" + Utility.hex(UTF16.getTrailSurrogate(codePoint));
					}
					return "\\u" + Utility.hex(codePoint);
				}
				if (JavaRegex_slash.contains(codePoint)) return "\\" + UTF16.valueOf(codePoint);
				return UTF16.valueOf(codePoint);
			}
		};
		
		private static String toPattern(UnicodeSet temp, CodePointShower shower) {
			StringBuffer result = new StringBuffer();
			result.append('[');
			for (UnicodeSetIterator it = new UnicodeSetIterator(temp); it.nextRange();) {
				// three cases: single, adjacent, range
				int first = it.codepoint;
				result.append(shower.show(first++));
				if (first > it.codepointEnd) continue;
				if (first != it.codepointEnd) result.append('-');
				result.append(shower.show(it.codepointEnd));
			}
			result.append(']');
			return result.toString();
		}
		
		public Map getVariables() {
			return Collections.unmodifiableMap(variables);
		}

		public List getRules() {
			List result = new ArrayList();
			for (Iterator it = htmlRules.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				result.add(key + ")\t" + htmlRules.get(key));
			}
			return result;
		}
	}
	
	//============== Internals ================
	
	private List rules = new ArrayList(1);
	private List orders = new ArrayList(1);
	private double breakRule;
	public UnicodeMap getSamples() {
		return samples;
	}
	
	static String[][] cannedRules = {
		{
			"GraphemeClusterBreak",
			"$CR=\\p{Grapheme_Cluster_Break=CR}",
			"$LF=\\p{Grapheme_Cluster_Break=LF}",
			"$Control=\\p{Grapheme_Cluster_Break=Control}",
			"$Extend=\\p{Grapheme_Cluster_Break=Extend}",
			"$L=\\p{Grapheme_Cluster_Break=L}",
			"$V=\\p{Grapheme_Cluster_Break=V}",
			"$T=\\p{Grapheme_Cluster_Break=T}",
			"$LV=\\p{Grapheme_Cluster_Break=LV}",
			"$LVT=\\p{Grapheme_Cluster_Break=LVT}",
			"3) $CR  	\u00D7  	$LF",
			"4) ( $Control | $CR | $LF ) 	\u00F7",
			"5) \u00F7 	( $Control | $CR | $LF )",
			"6) $L 	\u00D7 	( $L | $V | $LV | $LVT )",
			"7) ( $LV | $V ) 	\u00D7 	( $V | $T )",
			"8) ( $LVT | $T) 	\u00D7 	$T",
			"9) \u00D7 	$Extend"
		},{
			"LineBreak",
			"# Variables",
			"$AI=\\p{Line_Break=Ambiguous}",
			"$AL=\\p{Line_Break=Alphabetic}",
			"$B2=\\p{Line_Break=Break_Both}",
			"$BA=\\p{Line_Break=Break_After}",
			"$BB=\\p{Line_Break=Break_Before}",
			"$BK=\\p{Line_Break=Mandatory_Break}",
			"$CB=\\p{Line_Break=Contingent_Break}",
			"$CL=\\p{Line_Break=Close_Punctuation}",
			"$CM=\\p{Line_Break=Combining_Mark}",
			"$CR=\\p{Line_Break=Carriage_Return}",
			"$EX=\\p{Line_Break=Exclamation}",
			"$GL=\\p{Line_Break=Glue}",
			"$H2=\\p{Line_Break=H2}",
			"$H3=\\p{Line_Break=H3}",
			"$HY=\\p{Line_Break=Hyphen}",
			"$ID=\\p{Line_Break=Ideographic}",
			"$IN=\\p{Line_Break=Inseparable}",
			"$IS=\\p{Line_Break=Infix_Numeric}",
			"$JL=\\p{Line_Break=JL}",
			"$JT=\\p{Line_Break=JT}",
			"$JV=\\p{Line_Break=JV}",
			"$LF=\\p{Line_Break=Line_Feed}",
			"$NL=\\p{Line_Break=Next_Line}",
			"$NS=\\p{Line_Break=Nonstarter}",
			"$NU=\\p{Line_Break=Numeric}",
			"$OP=\\p{Line_Break=Open_Punctuation}",
			"$PO=\\p{Line_Break=Postfix_Numeric}",
			"$PR=\\p{Line_Break=Prefix_Numeric}",
			"$QU=\\p{Line_Break=Quotation}",
			"$SA=\\p{Line_Break=Complex_Context}",
			"$SG=\\p{Line_Break=Surrogate}",
			"$SP=\\p{Line_Break=Space}",
			"$SY=\\p{Line_Break=Break_Symbols}",
			"$WJ=\\p{Line_Break=Word_Joiner}",
			"$XX=\\p{Line_Break=Unknown}",
			"$ZW=\\p{Line_Break=ZWSpace}",
			"# LB 1  Assign a line breaking class to each code point of the input. ",
			"# Resolve AI, CB, SA, SG, and XX into other line breaking classes depending on criteria outside the scope of this algorithm.",
			"# NOTE: CB is ok to fall through, but must handle others here.",
			//"show $AL",
			"$AL=[$AI $AL $XX $SA $SG]",
			//"show $AL",
			//"$oldAL=$AL", // for debugging
			"# WARNING: Fixes for Rule 9",
			"# Treat X CM* as if it were X.",
			"# Where X is any line break class except SP, BK, CR, LF, NL or ZW.",
			"$X=$CM*",
			"$AI=($AI $X)",
			"$AL=($AL $X)",
			"$B2=($B2 $X)",
			"$BA=($BA $X)",
			"$BB=($BB $X)",
			"$CB=($CB $X)",
			"$CL=($CL $X)",
			"$CM=($CM $X)",
			"$CM=($CM $X)",
			"$GL=($GL $X)",
			"$H2=($H2 $X)",
			"$H3=($H3 $X)",
			"$HY=($HY $X)",
			"$ID=($ID $X)",
			"$IN=($IN $X)",
			"$IS=($IS $X)",
			"$JL=($JL $X)",
			"$JT=($JT $X)",
			"$JV=($JV $X)",
			"$NS=($NS $X)",
			"$NU=($NU $X)",
			"$OP=($OP $X)",
			"$PO=($PO $X)",
			"$PR=($PR $X)",
			"$QU=($QU $X)",
			"$SA=($SA $X)",
			"$SG=($SG $X)",
			"$SY=($SY $X)",
			"$WJ=($WJ $X)",
			"$XX=($XX $X)",
			"# OUT OF ORDER ON PURPOSE",
			"# LB 10  Treat any remaining combining mark as AL.",
			"$AL=($AL | ^ $CM | (?<=[$SP $BK $CR $LF $NL $ZW]) $CM)",
			
			"# LB 4  Always break after hard line breaks (but never between CR and LF).",
			"4) $BK \u00F7",
			"# LB 5  Treat CR followed by LF, as well as CR, LF and NL as hard line breaks.",
			"5.01) $CR \u00D7 $LF",
			"5.02) $CR \u00F7",
			"5.03) $LF \u00F7",
			"5.04) $NL \u00F7",
			"# LB 6  Do not break before hard line breaks.",
			"6) \u00D7 ( $BK | $CR | $LF | $NL )",
			"# LB 7  Do not break before spaces or zero-width space.",
			"7.01) \u00D7 $SP",
			"7.02) \u00D7 $ZW",
			"# LB 8  Break after zero-width space.",
			"8) $ZW \u00F7",
			"# LB 9  Do not break a combining character sequence; treat it as if it has the LB class of the base character",
			"# in all of the following rules. (Where X is any line break class except SP, BK, CR, LF, NL or ZW.)",
			"9) [^$SP $BK $CR $LF $NL $ZW] \u00D7 $CM",
			"#WARNING: this is done by modifying the variable values for all but SP.... That is, $AL is really ($AI $CM*)!",
			"# LB 11  Do not break before or after WORD JOINER and related characters.",
			"11.01) \u00D7 $WJ",
			"11.02) $WJ \u00D7",
			"# LB 12  Do not break before or after NBSP and related characters.",
			//"12.01) [^$SP] \u00D7 $GL",
      "12) $GL \u00D7",
      "12.1) [^$SP, $BA, $HY] \u00D7 $GL",
			
			"# LB 13  Do not break before \u2018]\u2019 or \u2018!\u2019 or \u2018;\u2019 or \u2018/\u2019, even after spaces.",
			"# Using customization 7.",
			"13.01) [^$NU] \u00D7 $CL",
			"13.02) \u00D7 $EX",
			"13.03) [^$NU] \u00D7 $IS",
			"13.04) [^$NU] \u00D7 $SY",
			"#LB 14  Do not break after \u2018[\u2019, even after spaces.",
			"14) $OP $SP* \u00D7",
			"# LB 15  Do not break within \u2018\"[\u2019, even with intervening spaces.",
			"15) $QU $SP* \u00D7 $OP",
			"# LB 16  Do not break between closing punctuation and a nonstarter (lb=NS), even with intervening spaces.",
			"16) $CL $SP* \u00D7 $NS",
			"# LB 17  Do not break within \u2018\u2014\u2014\u2019, even with intervening spaces.",
			"17) $B2 $SP* \u00D7 $B2",
			"# LB 18  Break after spaces.",
			"18) $SP \u00F7",
			"# LB 19  Do not break before or after \u2018\"\u2019.",
			"19.01)  \u00D7 $QU",
			"19.02) $QU \u00D7",
			"# LB 20  Break before and after unresolved CB.",
			"20.01)  \u00F7 $CB",
			"20.02) $CB \u00F7",
			"# LB 21  Do not break before hyphen-minus, other hyphens, fixed-width spaces, small kana and other non-starters, or after acute accents.",
			"21.01) \u00D7 $BA",
			"21.02) \u00D7 $HY",
			"21.03) \u00D7 $NS",
			"21.04) $BB \u00D7",
			"# LB 22  Do not break between two ellipses, or between letters or numbers and ellipsis.",
			//"show $AL",
			"22.01) $AL \u00D7 $IN",
			"22.02) $ID \u00D7 $IN",
			"22.03) $IN \u00D7 $IN",
			"22.04) $NU \u00D7 $IN",
			"# LB 23  Do not break within \u2018a9\u2019, \u20183a\u2019, or \u2018H%\u2019.",
			"23.01) $ID \u00D7 $PO",
			"23.02) $AL \u00D7 $NU",
			"23.03) $NU \u00D7 $AL",
			"# LB 24  Do not break between prefix and letters or ideographs.",
			"24.01) $PR \u00D7 $ID",
			"24.02) $PR \u00D7 $AL",
			"24.03) $PO \u00D7 $AL",
			"# Using customization 7",
			"# LB 18  Do not break between the following pairs of classes.",
			"# LB 18-alternative: $PR? ( $OP | $HY )? $NU ($NU | $SY | $IS)* $CL? $PO?",
			"# Insert \u00D7 every place it could go. However, make sure that at least one thing is concrete, otherwise would cause $NU to not break before or after ",
			"25.01) ($PR | $PO) \u00D7 ( $OP | $HY )? $NU",
			"25.02) ( $OP | $HY ) \u00D7 $NU",
			"25.03) $NU \u00D7 ($NU | $SY | $IS)",
			"25.04) $NU ($NU | $SY | $IS)* \u00D7 ($NU | $SY | $IS | $CL)",
			"25.05) $NU ($NU | $SY | $IS)* $CL? \u00D7 ($PO | $PR)",
			
			"#LB 26 Do not break a Korean syllable.",
			"26.01) $JL  \u00D7 $JL | $JV | $H2 | $H3",
			"26.02) $JV | $H2 \u00D7 $JV | $JT",
			"26.03) $JT | $H3 \u00D7 $JT",
			"# LB 27 Treat a Korean Syllable Block the same as ID.",
			"27.01) $JL | $JV | $JT | $H2 | $H3 \u00D7 $IN",
			"27.02) $JL | $JV | $JT | $H2 | $H3  \u00D7 $PO",
			"27.03) $PR \u00D7 $JL | $JV | $JT | $H2 | $H3",
			"# LB 28  Do not break between alphabetics (\"at\").",
			"28) $AL \u00D7 $AL",
			"# LB 29  Do not break between numeric punctuation and alphabetics (\"e.g.\").",
			"29) $IS \u00D7 $AL",
			"# LB 30  Do not break between letters, numbers or ordinary symbols and opening or closing punctuation.",
			"30.01) ($AL | $NU) \u00D7 $OP",
			"30.02) $CL \u00D7 ($AL | $NU)",
		},{
			"SentenceBreak",
			"$CR=\\p{Sentence_Break=CR}",
			"$LF=\\p{Sentence_Break=LF}",
			"$Extend=\\p{Sentence_Break=Extend}",
			"$Format=\\p{Sentence_Break=Format}",
			"$Sep=\\p{Sentence_Break=Sep}",
			"$Sp=\\p{Sentence_Break=Sp}",
			"$Lower=\\p{Sentence_Break=Lower}",
			"$Upper=\\p{Sentence_Break=Upper}",
			"$OLetter=\\p{Sentence_Break=OLetter}",
			"$Numeric=\\p{Sentence_Break=Numeric}",
			"$ATerm=\\p{Sentence_Break=ATerm}",
			"$STerm=\\p{Sentence_Break=STerm}",
      "$Close=\\p{Sentence_Break=Close}",
      "$SContinue=\\p{Sentence_Break=SContinue}",
			"$Any=.",
			//"# subtract Format from Control, since we don't want to break before/after",
			//"$Control=[$Control-$Format]", 
			"# Expresses the negation in rule 8; can't do this with normal regex, but works with UnicodeSet, which is all we need.",
			//"$NotStuff=[^$OLetter $Upper $Lower $Sep]",
			"# $ATerm and $Sterm are temporary, to match ICU until UTC decides.",
			
			"# WARNING: For Rule 5, now add format and extend to everything but Sep",
			"$X=[$Format $Extend]*",
			//"$X=$Extend* $Format*",
			"$Sp=($Sp $X)",
			"$Lower=($Lower $X)",
			"$Upper=($Upper $X)",
			"$OLetter=($OLetter $X)",
			"$Numeric=($Numeric $X)",
			"$ATerm=($ATerm $X)",
			"$STerm=($STerm $X)",
      "$Close=($Close $X)",
      "$SContinue=($SContinue $X)",
			
			"# Do not break within CRLF",
			"3) $CR  	\u00D7  	$LF",
			"# Break after paragraph separators.",
			"4) ($Sep | $CR | $LF)  	\u00F7",
			//"3.4) ( $Control | $CR | $LF ) 	\u00F7",
			//"3.5) \u00F7 	( $Control | $CR | $LF )",
			"# Ignore Format and Extend characters, except when they appear at the beginning of a region of text.",
			"# (See Section 6.2 Grapheme Cluster and Format Rules.)",
			"# WARNING: Implemented as don't break before format (except after linebreaks),",
			"# AND add format and extend in all variables definitions that appear after this point!",
			//"3.91) [^$Control | $CR | $LF] \u00D7 	$Extend",
			"5) [^ $Sep $CR $LF] \u00D7 [$Format $Extend]", 
			"# Do not break after ambiguous terminators like period, if immediately followed by a number or lowercase letter,",
			"# is between uppercase letters, or if the first following letter (optionally after certain punctuation) is lowercase.",
			"# For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.",
			"6) $ATerm 	\u00D7 	$Numeric",
			"7) $Upper $ATerm 	\u00D7 	$Upper",
			"8) $ATerm $Close* $Sp* 	\u00D7 	[^$OLetter $Upper $Lower $Sep $CR $LF]* $Lower",
			"8.1) ($STerm | $ATerm) $Close* $Sp* 	\u00D7 	($SContinue | $STerm | $ATerm)",
			"#Break after sentence terminators, but include closing punctuation, trailing spaces, and (optionally) a paragraph separator.",
			"9) ( $STerm | $ATerm ) $Close* 	\u00D7 	( $Close | $Sp | $Sep | $CR | $LF )",
			"# Note the fix to $Sp*, $Sep?",
			"10) ( $STerm | $ATerm ) $Close* $Sp* 	\u00D7 	( $Sp | $Sep | $CR | $LF )",
			"11) ( $STerm | $ATerm ) $Close* $Sp* ($Sep | $CR | $LF)? \u00F7",
			"#Otherwise, do not break",
			"12) \u00D7 	$Any",
		},{
			"WordBreak",
			"$CR=\\p{Grapheme_Cluster_Break=CR}",
			"$LF=\\p{Grapheme_Cluster_Break=LF}",
			"$Control=\\p{Grapheme_Cluster_Break=Control}",
			"$Extend=\\p{Grapheme_Cluster_Break=Extend}",
			//"$NEWLINE=[$CR $LF \\u0085 \\u000B \\u000C \\u2028 \\u2029]",
			"$Sep=\\p{Sentence_Break=Sep}",
			"# Now normal variables",
			"$Format=\\p{Word_Break=Format}",
			"$Katakana=\\p{Word_Break=Katakana}",
			"$ALetter=\\p{Word_Break=ALetter}",
			"$MidLetter=\\p{Word_Break=MidLetter}",
      "$MidNum=\\p{Word_Break=MidNum}",
      "$MidNumLet=\\p{Word_Break=MidNumLet}",
			"$Numeric=\\p{Word_Break=Numeric}",
			"$ExtendNumLet=\\p{Word_Break=ExtendNumLet}",
			
			"# WARNING: For Rule 4: Fixes for GC, Format",
			//"# Subtract Format from Control, since we don't want to break before/after",
			//"$Control=[$Control-$Format]", 
			"# Add format and extend to everything",
			"$X=[$Format $Extend]*",
			//"$X= ($Extend | $Format)*",
			"$Katakana=($Katakana $X)",
			"$ALetter=($ALetter $X)",
			"$MidLetter=($MidLetter $X)",
      "$MidNum=($MidNum $X)",
      "$MidNumLet=($MidNumLet $X)",
			"$Numeric=($Numeric $X)",
			"$ExtendNumLet=($ExtendNumLet $X)",
			//"# Do not break within CRLF",
			"3) $CR  	\u00D7  	$LF",
			//"3.4) ( $Control | $CR | $LF ) 	\u00F7",
			//"3.5) \u00F7 	( $Control | $CR | $LF )",
			//"3.9) \u00D7 	$Extend",
			//"3.91) [^$Control | $CR | $LF] \u00D7 	$Extend",
			"# Ignore Format and Extend characters, except when they appear at the beginning of a region of text.",
			"# (See Section 6.2 Grapheme Cluster and Format Rules.)",
			"# WARNING: Implemented as don't break before format (except after linebreaks),",
			"# AND add format and extend in all variables definitions that appear after this point!",
			//"4) \u00D7 [$Format $Extend]", 
			"4) [^ $Sep $CR $LF ] \u00D7 [$Format $Extend]", 
			"# Vanilla rules",
			"5)$ALetter  	\u00D7  	$ALetter",
			"6)$ALetter 	\u00D7 	($MidLetter | $MidNumLet) $ALetter",
			"7)$ALetter ($MidLetter | $MidNumLet) 	\u00D7 	$ALetter",
			"8)$Numeric 	\u00D7 	$Numeric",
			"9)$ALetter 	\u00D7 	$Numeric",
			"10)$Numeric 	\u00D7 	$ALetter",
			"11)$Numeric ($MidNum | $MidNumLet) 	\u00D7 	$Numeric",
			"12)$Numeric 	\u00D7 	($MidNum | $MidNumLet) $Numeric",
			"13)$Katakana 	\u00D7 	$Katakana",
			"13.1)($ALetter | $Numeric | $Katakana | $ExtendNumLet) 	\u00D7 	$ExtendNumLet",
			"13.2)$ExtendNumLet 	\u00D7 	($ALetter | $Numeric | $Katakana)",
			//"#15.1,100)$ALetter \u00F7",
			//"#15.1,100)$Numeric \u00F7",
			//"#15.1,100)$Katakana \u00F7",
			//"#15.1,100)$Ideographic \u00F7",
			
		}};
}