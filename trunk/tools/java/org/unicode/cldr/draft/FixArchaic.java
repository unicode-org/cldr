package org.unicode.cldr.draft;

import java.util.regex.Matcher;

import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class FixArchaic {
    static final int blockEnum = UCharacter.getPropertyEnum("block");
    static final int scriptEnum = UCharacter.getPropertyEnum("script");

    // static final UnicodeSet NO_SCRIPT = (UnicodeSet) new
    // UnicodeSet("[[:script=common:][:script=inherited:][:cn:]]"
    // ).addAll(ScriptCategories.DEPRECATED_NEW).freeze();

    public static void main(String[] args) {
        // UnicodeSet subblockArchaics = new UnicodeSet(
        // "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u03D8-\u03E1\u03F7\u03F8\u03FA\u03FB\u066E\u066F\u07E8-\u07EA\u10F1-\u10F6\u1DC0\u1DC1\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\u3165-\u318E\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF\\U00010140-\\U0001018A\\U00010190-\\U0001019B\\U0001D200-\\U0001D245]"
        // );

        checkRegex();
        return;

        // checkLatinPP();

        // fixArchaic();
    }

    // private static void fixArchaic() {
    // final UnicodeSetFormat unicodeSetFormat = new UnicodeSetFormat(Target.JAVA);

    // String result;

    // result = unicodeSetFormat.formatWithProperties(new UnicodeSet(
    // "[^[:cn:][:script=common:][:script=inherited:]]"), false, new UnicodeSet(
    // "[[:cn:][:script=common:][:script=inherited:]]"), blockEnum, scriptEnum);
    // System.out.println("Script Blocks:\t" + result);

    // return;
    // result = unicodeSetFormat.formatWithProperties(ScriptCategories.ARCHAIC_31, false,
    // new UnicodeSet("[[:cn:][:script=common:][:script=inherited:]]"), blockEnum, scriptEnum);
    // System.out.println("UAX31:\t" + result);

    // result = unicodeSetFormat.formatWithProperties(new UnicodeSet(ScriptCategories.ARCHAIC_39)
    // .removeAll(ScriptCategories.ARCHAIC_31), false, new UnicodeSet(
    // "[[:cn:][:script=common:][:script=inherited:]]"), blockEnum, scriptEnum);
    // System.out.println("UTS39:\t" + result);

    // final UnicodeSet heuristicRemainder = new UnicodeSet(ScriptCategories.ARCHAIC_HEURISTIC)
    // .removeAll(ScriptCategories.ARCHAIC_39).removeAll(ScriptCategories.ARCHAIC_31);
    // System.out.println("Raw heuristic archaics:\t" + heuristicRemainder);
    // result = unicodeSetFormat.formatWithProperties(heuristicRemainder, false, new UnicodeSet(
    // "[:cn:]"), blockEnum, scriptEnum);
    // System.out.println("Heuristic archaics:\t" + result);

    // final UnicodeSet heuristicRemainder2 = new UnicodeSet(ScriptCategories.ARCHAIC).removeAll(
    // ScriptCategories.ARCHAIC_HEURISTIC).removeAll(ScriptCategories.ARCHAIC_39).removeAll(
    // ScriptCategories.ARCHAIC_31);
    // System.out.println("Raw heuristic archaics2:\t" + heuristicRemainder2);
    // result = unicodeSetFormat.formatWithProperties(heuristicRemainder2, false, new UnicodeSet(
    // "[:cn:]"), blockEnum, scriptEnum);
    // System.out.println("Heuristic archaics2:\t" + result);
    // }

    // private static void checkLatinPP() {
    // UnicodeSet latin = new UnicodeSet("[:script=latin:]");
    // UnicodeSet latinpp = new UnicodeSet("[[:script=latin:][:script=common:][:script=inherited:]]");
    // UnicodeSet set = new UnicodeSet();
    // UnicodeSet source = new UnicodeSet("[:nfkdqc=n:]");
    // for (UnicodeSetIterator it = new UnicodeSetIterator(source); it.next();) {
    // String x = it.getString();
    // String s = Normalizer.normalize(x, Normalizer.DECOMP_COMPAT);
    // if (latinpp.containsAll(s) && latin.containsSome(s)) {
    // set.add(it.codepoint);
    // }
    // }
    // System.out.println(set);
    // }

    private static void checkRegex() {
        checkRegex(true);
        checkRegex(false);
    }

    private static void checkRegex(boolean escaped) {
        UnicodeSet failed = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            final String stri = UTF16.valueOf(i);
            String uset;
            if (i < 0x10000) {
                uset = new UnicodeSet(i, i).toPattern(escaped);
            } else {
                if (escaped) {
                    uset = "[\\u" + Integer.toHexString(stri.charAt(0)) + "\\u"
                        + Integer.toHexString(stri.charAt(1)) + "]";
                } else {
                    uset = "[" + stri + "]";
                }
            }
            Matcher m;
            try {
                m = PatternCache.get(uset).matcher(stri);
            } catch (Exception e) {
                failed.add(i);
                continue;
            }
            if (!m.matches()) {
                failed.add(i);
                continue;
            }
            if (otherRegexSucceeds(m, i)) {
                failed.add(i);
                continue;
            }
        }
        System.out.println("UnicodeSet/Java Regex differences " + escaped + ": " + failed);
    }

    private static boolean otherRegexSucceeds(Matcher m, int skip) {
        for (int i = 1; i <= 0x10FFFF; i += i) {
            if (i == skip)
                continue;
            if (m.reset(UTF16.valueOf(i)).matches()) {
                return true;
            }
        }
        return false;
    }

}
