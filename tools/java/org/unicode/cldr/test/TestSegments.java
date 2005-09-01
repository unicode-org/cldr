package org.unicode.cldr.test;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Quick class for testing proposed syntax for Segments.
 * WARNING: requires Java 5
 * @author davis
 *
 */

public class TestSegments {
	
	static final UnicodeSet DEBUG_RETAIN = new UnicodeSet("[\u0000-\u00FF\u0300-\u0310]"); // null;
	
	// quick test
	public static void main(String[] args) {
		
		//quickCheck();
		
		// main test
		
		String[][] tests = {{
			"QuickCheck",
			"1) ÷ b",
			"2) × .",
			"0.5) a ×",
			"test",
			"abcbdb"
		},{
			"QuickCheck2",
			"$Letter=\\p{Alphabetic}",
			"$Digit=\\p{Digit}",
			"1) $Digit × $Digit",
			"2) $Letter × $Letter",
			"test",
			"The quick 100 brown foxes."
		},{
			"Grapheme Cluster",
			"$CR=\\p{Grapheme_Cluster_Break=CR}",
			"$LF=\\p{Grapheme_Cluster_Break=LF}",
			"$Control=\\p{Grapheme_Cluster_Break=Control}",
			"$Extend=\\p{Grapheme_Cluster_Break=Extend}",
			"$L=\\p{Grapheme_Cluster_Break=L}",
			"$V=\\p{Grapheme_Cluster_Break=V}",
			"$T=\\p{Grapheme_Cluster_Break=T}",
			"$LV=\\p{Grapheme_Cluster_Break=LV}",
			"$LVT=\\p{Grapheme_Cluster_Break=LVT}",
			"3) $CR  	×  	$LF",
			"4) ( $Control | $CR | $LF ) 	÷",
			"5) ÷ 	( $Control | $CR | $LF )",
			"6) $L 	× 	( $L | $V | $LV | $LVT )",
			"7) ( $LV | $V ) 	× 	( $V | $T )",
			"8) ( $LVT | $T) 	× 	$T",
			"9) × 	$Extend",
			"test",
			"The qui\u0300ck 100 brown foxes."
		},{
			"Word Break",
			"$Format=\\p{Word_Break=Format}",
			"$Extend=\\p{Grapheme_Cluster_Break=Extend}",
			"$X=[$Format $Extend]*",
			"$Katakana=\\p{Word_Break=Katakana} $X",
			"$ALetter=\\p{Word_Break=ALetter} $X",
			"$MidLetter=\\p{Word_Break=MidLetter} $X",
			"$MidNum=\\p{Word_Break=MidNum} $X",
			"$Numeric=\\p{Word_Break=Numeric} $X",
			"$ExtendNumLet=\\p{Word_Break=ExtendNumLet} $X",
			"4.1) × 	$Extend",
			"5)$ALetter  	×  	$ALetter",
			"6)$ALetter 	× 	$MidLetter $ALetter",
			"7)$ALetter $MidLetter 	× 	$ALetter",
			"8)$Numeric 	× 	$Numeric",
			"9)$ALetter 	× 	$Numeric",
			"10)$Numeric 	× 	$ALetter",
			"11)$Numeric $MidNum 	× 	$Numeric",
			"12)$Numeric 	× 	$MidNum $Numeric",
			"13)$Katakana 	× 	$Katakana",
			"13.1)($ALetter | $Numeric | $Katakana | $ExtendNumLet) 	× 	$ExtendNumLet",
			"13.2)$ExtendNumLet 	× 	($ALetter | $Numeric | $Katakana)",

			"test",
			"T\u0300he qui\u0300ck 100.1 brown foxes."
		}};
		NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
		nf.setMinimumFractionDigits(0);
		for (int i = 0; i < tests.length; ++i) {
			RuleListBuilder rb = new RuleListBuilder();
			System.out.println("Building: " + tests[i][0]);
			int j = 1;
			for (; j < tests[i].length; ++j) {
				String line = tests[i][j];
				if (line.equals("test")) break;
				rb.addLine(line);
			}
			System.out.println("Testing");
			RuleList rl = rb.make();
			System.out.println(rl);
			for (++j; j < tests[i].length; ++j) {
				String line = tests[i][j];
				String showingBreaks = ""; // don't bother with surrogates yet
				for (int k = 0; k <= line.length(); ++k) {
					if (rl.breaksAt(line,k)) {
						showingBreaks += "«" + nf.format(rl.getBreakRule()) + "»";
					}
					if (k < line.length()) showingBreaks += line.charAt(k);
				}
				System.out.println(showingBreaks);
			}
		}
	}

	private static void quickCheck() {
		// for quickly checking regexes in Java
		
		String[][] rtests = {{
			"(?<=a)b", "ab"
		},{
			"[$]\\p{Alpha}\\p{Alnum}*", "$Letter"
		}};
		for (int i = 0; i < rtests.length; ++i) {
			Matcher m = Pattern.compile(rtests[i][0]).matcher("");
			m.reset(rtests[i][1]);
			boolean matches = m.matches();
			System.out.println(rtests[i][0] + ",\t" + rtests[i][1] + ",\t" + matches);
		}
	}
	
	static final byte NO_BREAK = -1, UNKNOWN_BREAK = 0, BREAK = 1;
	
	static class Rule {
		private Matcher matchPrevious;
		private Matcher matchSucceeding;
		private String name;
		// in Java 5, this can be more efficient, and use a single regex
		// of the form "(?<= before) after". MUST then have transparent bounds
		private byte breaks;
		public Rule(String before, byte result, String after) {
			breaks = result;
			matchPrevious = Pattern.compile(".*" + before, Pattern.COMMENTS).matcher("");
			matchSucceeding = Pattern.compile(after, Pattern.COMMENTS).matcher("");
			name = before + (result == NO_BREAK ? " × " : " ÷ ") + after;
			// COMMENTS allows whitespace
		}
		public byte matches(CharSequence text, int position) {
			if (!matchAfter(matchSucceeding, text, position)) return UNKNOWN_BREAK;
			if (!matchBefore(matchPrevious, text, position)) return UNKNOWN_BREAK;
			return breaks;
		}
		public String toString() {
			return name;
		}
	}
	
	// utility, since we are using Java 1.4
	static boolean matchAfter(Matcher matcher, CharSequence text, int position) {
		return matcher.reset(text.subSequence(position, text.length())).lookingAt();
	}

	// utility, since we are using Java 1.4
	// depends on the pattern having been built with .*
	// not very efficient, but best we can do.
	static boolean matchBefore(Matcher matcher, CharSequence text, int position) {
		return matcher.reset(text.subSequence(0, position)).matches();
	}
	
	public static final float SOT = -3, EOT = -2, ANY = -1;
	
	static class RuleList {
		private List rules = new ArrayList(1);
		private List orders = new ArrayList(1);
		private float breakRule;
		
		public boolean breaksAt(CharSequence text, int position) {
			if (position == 0) {
				breakRule = SOT;
				return true;
			}
			if (position == text.length()) {
				breakRule = EOT;
				return true;
			}
			for (int i = 0; i < rules.size(); ++i) {
				Rule rule = (Rule)rules.get(i);
				byte result = rule.matches(text, position);
				if (result != UNKNOWN_BREAK) {
					breakRule = ((Float)orders.get(i)).floatValue();
					return result == BREAK;
				}
			}
			breakRule = ANY;
			return true; // default
		}
		public void add(float order, Rule rule) {
			orders.add(new Float(order));
			rules.add(rule);
		}
		public float getBreakRule() {
			return breakRule;
		}
		public String toString() {
			String result = "";
			for (int i = 0; i < rules.size(); ++i) {
				if (i != 0) result += "\r\n";
				result += orders.get(i) + ") " + rules.get(i);
			}
			return result;
		}
	}
	
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
	
	// separate builder for clarity
	
	static class RuleListBuilder {
		private Map variables = new TreeMap(LONGEST_STRING_FIRST); // sorted by length, longest first, to make substitution easy
		private Map rules = new TreeMap();
		
		RuleListBuilder addLine(String line) {
			// dumb parsing for now
			int relationPosition = line.indexOf('=');
			if (relationPosition >= 0) {
				addVariable(line.substring(0,relationPosition).trim(), line.substring(relationPosition+1).trim());
				return this;
			}
			relationPosition = line.indexOf(')');
			Float order;
			try {
				order = new Float(Float.parseFloat(line.substring(0,relationPosition).trim()));
			} catch (Exception e) {
				throw new IllegalArgumentException("Rule must be of form '1)...'");
			}
			line = line.substring(relationPosition + 1).trim();
			relationPosition = line.indexOf('÷');
			byte breaks = BREAK;
			if (relationPosition < 0) {
				relationPosition = line.indexOf('×');
				if (relationPosition < 0) throw new IllegalArgumentException("Couldn't find =, ÷, or ×");
				breaks = NO_BREAK;
			}
			addRule(order, line.substring(0,relationPosition).trim(), breaks, line.substring(relationPosition + 1).trim());		
			return this;
		}
		
		private transient Matcher identifierMatcher = Pattern.compile("[$]\\p{Alpha}\\p{Alnum}*").matcher("");
		
		RuleListBuilder addVariable(String name, String value) {
			if (!identifierMatcher.reset(name).matches()) {
				throw new IllegalArgumentException("Variable name must be $id: '" + name + "'");
			}
			value = replaceVariables(value);
			variables.put(name, value);
			return this;
		}
		RuleListBuilder addRule(Float order, String before, byte result, String after) {
			// TODO, fix properties
			// TODO, throw exception if variable not replaced
			rules.put(order, new Rule(replaceVariables(before), result, replaceVariables(after)));
			return this;	
		}
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
					UnicodeSet temp = new UnicodeSet(result, parsePosition, null);
					String insert = getInsertablePattern(temp);
					result = result.substring(0,i) + insert + result.substring(parsePosition.getIndex());
					i += insert.length() - 1; // skip over inserted stuff; -1 since the loop will add
				}
			}
			return result;
		}
		
		ParsePosition parsePosition = new ParsePosition(0);
		
		private String getInsertablePattern(UnicodeSet temp) {
			temp.complement().complement();
			temp.remove(0x10000,0x10FFFF); // TODO Fix with Hack
			if (DEBUG_RETAIN != null) {
				temp.retainAll(DEBUG_RETAIN);
				if (temp.size() == 0) temp.add(0xFFFF); // just so not empty
			}
			String insert = temp.toPattern(true);
			return insert;
		}
		
		
		RuleList make() {
			RuleList result = new RuleList();
			for (Iterator it = rules.keySet().iterator(); it.hasNext();) {
				Float key = (Float)it.next();
				result.add(key.floatValue(), (Rule)rules.get(key));
			}
			return result;
		}
	}
}