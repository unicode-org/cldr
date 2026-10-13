package org.unicode.cldr.test;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Function;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.With;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
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
 *  codepoint = "⦕" [A-Fa-f0-9]+ "⦖"
 *  codepoint = literal
 *
 *  The { is already used in UnicodeSet for strings, and familiar to vetters. ⦕ was chosen to be avoid special use of \\u or \x
 *
 *  TODO parse and format hex in strings
 * @author markdavis
 */
public class SimpleUnicodeSetFormatter {
    public static final UnicodeSet FORCE_HEX = new UnicodeSet("[[:c:][:z:][:whitespace:]]");
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
        this.comparator = col;
        this.forceHex = forceHex == null ? FORCE_HEX : forceHex;
    }

    static Normalizer2 nfc = Normalizer2.getNFCInstance();

    public String format(UnicodeSet input) {
        StringBuilder result = new StringBuilder();
        TreeSet<String> sorted = transformAndAddAllTo(input, x -> nfc.normalize(x), new TreeSet<>(comparator));

        int firstOfRange = -2;
        int lastOfRange = -2;
        for (String item : sorted) {
            int cp = CharSequences.getSingleCodePoint(item);
            if (cp == Integer.MAX_VALUE) {
                if (lastOfRange >= 0) {
                    if (firstOfRange != lastOfRange) {
                        result.append(firstOfRange + 1 != lastOfRange ? '-' : ' ');
                        appendWithHex(result, lastOfRange, forceHex);
                    }
                    firstOfRange = lastOfRange = -2;
                }
                if (result.length() > 0) {
                    result.append(' ');
                }
                appendWithHex(result.append('{'), item, forceHex).append('}');
            } else if (lastOfRange == cp - 1) {
                ++lastOfRange;
            } else {
                if (firstOfRange != lastOfRange) {
                    result.append(firstOfRange + 1 != lastOfRange ? '-' : ' ');
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
            result.append(firstOfRange + 1 != lastOfRange ? '-' : ' ');
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
            ap.append('⦕').append(Utility.hex(cp, 1)).append('⦖');
        }
        return ap;
    }

    public static String showInvisible(UnicodeSet input, UnicodeSet forceHex) {
        return appendWithHex(new StringBuilder(), input.toPattern(false), forceHex).toString();
    }

    private enum State {start, haveCp, haveHyphen, haveCurly, haveHex, haveHyphenHex}

    public UnicodeSet parse(String input) {
        UnicodeSet result = new UnicodeSet();
        StringBuilder b = new StringBuilder();
        State state = State.start;
        int hex = 0;
        int last = -1; // -1 indicates can't have -x for a range after it.
        for (int cp : With.codePointArray(input)) {

            switch (state) {
            case start:
                switch (cp) {
                case ' ':
                    break;
                case '{':
                    state=State.haveCurly; break;
                case '⦕':
                    state=State.haveHex; break;
                default:
                    result.add(cp); last=cp; state=State.haveCp; break;
                }
                break;
            case haveCp:
                switch (cp) {
                case ' ':
                    state=State.start; break;
                case '{':
                    state=State.haveCurly; break;
                case '⦕':
                    state=State.haveHex; break;
                case '-':
                    state=State.haveHyphen; break;
                default:
                    result.add(cp); last=cp; state=State.haveCp; break;
                }
                break;
            case haveHyphen:
                switch (cp) {
                case ' ':
                    result.add('-'); state=State.start; break; // failure
                case '⦕':
                    state=State.haveHyphenHex; break;
                default:
                    result.add(last+1, cp); last=-1; state=State.start; break;
                }
                break;
            case haveCurly:
                switch (cp) {
                case '}':
                    result.add(b); b.setLength(0); state=State.start;
                    break;
                case ' ':
                    b.insert(0, '{'); result.add(b); state=State.start;
                    break; // failure
                    // TODO {a-c should be { followed by range a-b
                default:
                    b.appendCodePoint(cp); break;
                }
                break;
            case haveHex:
            case haveHyphenHex:
                switch (cp) {
                case '0':  case '1':  case '2':  case '3':  case '4':  case '5': case '6': case '7': case '8': case '9':
                    hex *= 16;
                    hex += cp - '0';
                    if (hex > 0x10FFFF) {
                        throw new IllegalArgumentException("⦕hex > 0x10FFFF");
                    }
                    b.appendCodePoint(cp); // just for recovery
                    break;
                case 'a':  case 'b':  case 'c':  case 'd':  case 'e':  case 'f':
                    hex *= 16;
                    hex += cp - 'a' + 10;
                    if (hex > 0x10FFFF) {
                        throw new IllegalArgumentException("⦕hex > 0x10FFFF");
                    }
                    b.appendCodePoint(cp); // just for recovery
                    break;
                case 'A':  case 'B':  case 'C':  case 'D':  case 'E':  case 'F':
                    hex *= 16;
                    hex += cp - 'A' + 10;
                    if (hex > 0x10FFFF) {
                        throw new IllegalArgumentException("⦕hex > 0x10FFFF");
                    }
                    b.appendCodePoint(cp); // just for recovery
                    break;
                default:
                    b.insert(0, '⦕'); result.add(b); state=State.start;
                    break; // failure
                case '⦖':
                    // TODO check out of bounds hex
                    if (state == State.haveHyphenHex) {
                        result.add(last+1, hex); last=-1;  state=State.start;
                    } else {
                        result.add(hex); last=hex; state=State.haveCp;
                    }
                    hex = 0;
                    b.setLength(0);
                    break;
                }
                break;
            }
        }
        return result;
    }

    // TODO Move into unit test

    public static void main(String[] args) {
        final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter(BASIC_COLLATOR, null);

        check(susf, "", ExemplarType.main, new UnicodeSet("[a-b]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[-a-ceg-h{ef}]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D-\\u200f]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D\\u200e]"), false);
        // TODO also allow hex in strings check(susf, "", ExemplarType.main, new UnicodeSet("[{\\u200D\\u200e}]"), false);

        boolean showAnyway = true;
        for (String locale : cldrFactory.getAvailableLanguages()) {
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet expected = cldrFile.getExemplarSet(type, WinningChoice.WINNING);
                expected = transform(expected, x -> nfc.normalize(x)); //current CLDR might not be normalized
                check(susf, locale, type, expected, showAnyway);
            }
        }
    }


    public static void check(SimpleUnicodeSetFormatter susf, String locale, ExemplarType type, UnicodeSet expected, boolean showAnyway) {
        String formatted = susf.format(expected);
        UnicodeSet roundtrip = susf.parse(formatted);
        final boolean isOk = expected.equals(roundtrip);
        if (showAnyway || !isOk) {
            UnicodeSet roundtrip_source = new UnicodeSet(roundtrip).removeAll(expected);
            UnicodeSet source_roundtrip = new UnicodeSet(expected).removeAll(roundtrip);
            System.out.println(locale + "\t" + type + "\tsource:  \t" + showInvisible(expected, FORCE_HEX));
            System.out.println(locale + "\t" + type + "\tformatted:\t" + formatted);
            if (!roundtrip_source.isEmpty()) System.out.println(locale + "\t" + type + "\tFAIL, roundtrip-source:  \t" + showInvisible(roundtrip_source, FORCE_HEX));
            if (!source_roundtrip.isEmpty()) System.out.println(locale + "\t" + type + "\tFAIL, source_roundtrip:  \t" + showInvisible(source_roundtrip, FORCE_HEX));

            if (!isOk) {
                String formattedDebug = susf.format(expected);
                UnicodeSet roundtripDebug = susf.parse(formatted);
            }
        }
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
            String t = function.apply(s);
            target.add(t);
        }
        return target;
    }

}
