package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Function;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Goal is a very simple format for UnicodeSet, that keeps vetters from having to know about \ for quoting
 * There are 3 special characters, but we make sure that they don't have to be quoted if separated by spaces
 *  -   a range, but if between two code points
 *  {   start of a string, but only if followed by a }, with no intervening spaces
 *  ⦕   start of hex, but only if followed by [A-Fa-f0-9]+ ⦖
 *
 *  EBNF
 *  result = item + (" "? + (string | range | codepoint))*
 *  string = "{" literal+ "} // literal must contain no spaces
 *  range = codepoint "-" codepoint
 *  codepoint = "⦕" [A-F0-9]+ "⦖" // escaped
 *  codepoint = literal
 *
 *  The { is already used in UnicodeSet for strings, and familiar to vetters. ⦕ was chosen to be avoid special use of \\u or \x
 *
 *  TODO parse and format hex in strings
 * @author markdavis
 */
public class SimpleUnicodeSetFormatter {
    private static final char RANGE = '➖';
    private static final char SESC = '❰';
    private static final char EESC = '❱';
    private static final char SSTR = '⦕';
    private static final char ESTR = '⦖';

    public static final UnicodeSet FORCE_HEX = new UnicodeSet("[[:c:][:z:][:whitespace:]]")
        .add(RANGE).add(SESC).add(EESC).add(SSTR).add(ESTR).freeze();

    public static Normalizer2 nfc = Normalizer2.getNFCInstance();

    public static final Comparator<String> BASIC_COLLATOR;
    static {
        Collator temp = Collator.getInstance(ULocale.ROOT);
        temp.setStrength(Collator.IDENTICAL);
        temp.freeze();
        BASIC_COLLATOR = (Comparator<String>) (Comparator<?>) temp;
    }

    private final Comparator<String> comparator;
    private final UnicodeSet forceHex;

    /**
     * Create a simple formatter, with a comparator for the ordering and a UnicodeSet of characters that are to use hex
     * @param col
     * @param forceHex
     */
    public SimpleUnicodeSetFormatter(Comparator<String> col, UnicodeSet forceHex) {
        // collate, but preserve non-equivalents
        this.comparator = new MultiComparator(col, new UTF16.StringComparator(true, false, 0));
        this.forceHex = forceHex == null ? FORCE_HEX : forceHex;
    }

    public String format(UnicodeSet input) {
        StringBuilder result = new StringBuilder();
        TreeSet<String> sorted = transformAndAddAllTo(input, null, new TreeSet<>(comparator)); // x -> nfc.normalize(x)

        int firstOfRange = -2;
        int lastOfRange = -2;
        for (String item : sorted) {
            int cp = CharSequences.getSingleCodePoint(item);
            if (cp == Integer.MAX_VALUE) {
                if (lastOfRange >= 0) {
                    if (firstOfRange != lastOfRange) {
                        result.append(firstOfRange + 1 != lastOfRange ? RANGE : ' ');
                        appendWithHex(result, lastOfRange, forceHex);
                    }
                    firstOfRange = lastOfRange = -2;
                }
                if (result.length() > 0) {
                    result.append(' ');
                }
                appendWithHex(result.append(SSTR), item, forceHex).append(ESTR);
            } else if (lastOfRange == cp - 1) {
                ++lastOfRange;
            } else {
                if (firstOfRange != lastOfRange) {
                    result.append(firstOfRange + 1 != lastOfRange ? RANGE : ' ');
                    appendWithHex(result, lastOfRange, forceHex);
                }
                if (result.length() > 0) {
                    result.append(' ');
                }
                appendWithHex(result, cp, forceHex);
                firstOfRange = lastOfRange = cp;
            }
        }
        if (firstOfRange != lastOfRange) {
            result.append(firstOfRange + 1 != lastOfRange ? RANGE : ' ');
            appendWithHex(result, lastOfRange, forceHex);
        }
        return result.toString();
    }

    public static final StringBuilder appendWithHex(StringBuilder ap, CharSequence s, UnicodeSet forceHex) {
        for (int cp : With.codePointArray(s)) {
            appendWithHex(ap, cp, forceHex);
        }
        return ap;
    }

    public static StringBuilder appendWithHex(StringBuilder ap, int cp, UnicodeSet forceHex) {
        if (!forceHex.contains(cp)) {
            ap.appendCodePoint(cp);
        } else {
            ap.append(SESC).append(CodePointEscaper.toAbbreviationOrHex(cp)).append(EESC);
        }
        return ap;
    }

    private enum State {start, haveCp, haveHyphen, haveCurly, haveHex, haveHyphenHex}

    public UnicodeSet parse(String input) {
        UnicodeSet result = new UnicodeSet();
        StringBuilder b = new StringBuilder();
        State state = State.start;
        StringBuilder toEscape = new StringBuilder();
        int last = -1; // -1 indicates can't have -x for a range after it.
        for (int cp : With.codePointArray(input)) {

            switch (state) {
            case start:
                switch (cp) {
                case ' ':
                    break;
                case SSTR:
                    state=State.haveCurly; break;
                case SESC:
                    state=State.haveHex; break;
                default:
                    result.add(cp); last=cp; state=State.haveCp; break;
                }
                break;
            case haveCp:
                switch (cp) {
                case ' ':
                    state=State.start; break;
                case SSTR:
                    state=State.haveCurly; break;
                case SESC:
                    state=State.haveHex; break;
                case RANGE:
                    state=State.haveHyphen; break;
                default:
                    result.add(cp); last=cp; state=State.haveCp; break;
                }
                break;
            case haveHyphen:
                switch (cp) {
                case ' ':
                    result.add(RANGE); state=State.start; break; // failure
                case SESC:
                    state=State.haveHyphenHex; break;
                default:
                    result.add(last+1, cp); last=-1; state=State.start; break;
                }
                break;
            case haveCurly:
                switch (cp) {
                case ESTR:
                    result.add(b); b.setLength(0); state=State.start;
                    break;
                case ' ':
                    b.insert(0, SSTR); result.add(b); state=State.start;
                    break; // failure
                    // TODO {a-c should be { followed by range a-b
                default:
                    b.appendCodePoint(cp); break;
                }
                break;
            case haveHex:
            case haveHyphenHex:
                switch (cp) {
                case EESC:
                    int newCp;
                    try {
                        newCp = CodePointEscaper.fromAbbreviationOrHex(toEscape);
                    } catch (Exception e) {
                        CodePointEscaper.fromAbbreviationOrHex(toEscape); // for debugging
                        throw e;
                    }
                    if (state == State.haveHyphenHex) {
                        result.add(last+1, newCp); last=-1;  state=State.start;
                    } else {
                        result.add(newCp); last=newCp; state=State.haveCp;
                    }
                    toEscape.setLength(0);
                    b.setLength(0);
                    break;
                default:
                    toEscape.appendCodePoint(cp);
                    break;
                }
                break;
            }
        }
        return result;
    }

    public static UnicodeSet transform(UnicodeSet expected, Function<String,String> function) {
        UnicodeSet result = new UnicodeSet();
        for (String s : expected) {
            String t = function.apply(s);
            result.add(t);
        }
        return result;
    }

    public static <T extends Collection<String>> T transformAndAddAllTo(UnicodeSet expected, Function<String,String> function, T target) {
        for (String s : expected) {
            String t = function == null ? s : function.apply(s);
            target.add(t);
        }
        return target;
    }

}
