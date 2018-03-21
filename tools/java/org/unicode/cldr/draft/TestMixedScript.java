package org.unicode.cldr.draft;

import java.util.BitSet;
import java.util.HashSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

/**
 * Simplified version of mixed-script test and number test. Spoof check should contain other checks:
 * <ol>
 * <li>Basic: No unassigned, private-use, or surrogate control points; no controls except for HTML-allowed ones (TAB,
 * LF, CR)
 * <li>Multiple NSMs: No multiple instances of the same non-spacing mark
 * <li>Scripts: only recommended scripts from Table 5a of http://unicode.org/reports/tr31/
 * <li>Characters: Use character restrictions from idmod
 * <li>Numbers: Disallow non-decimal numbers ([:Nl:][:No:]), non-NFKC numbers, and mixing numbers from two different
 * decimal systems, eg Deva + Western, or Arabic + Eastern-Arabic. (Question: should allow U+3007 ( 〇 ) IDEOGRAPHIC
 * NUMBER ZERO?)
 * </ol>
 */

public class TestMixedScript {
    /**
     * Defined levels based on http://www.unicode.org/reports/tr36/#Security_Levels_and_Alerts, but with modifications.
     * Note that Script_Extension characters are treated as any of their scripts, so references to Common or Inherited
     * only
     * include those characters that do not have Script_Extensions.
     */
    public enum MixedScriptLevel {
        /**
         * All of the text is in a single script (where strictly
         * Common/Inherited are ignored).
         */
        single,
        /**
         * The text is not single, and can contain Han+Hangul, Han+Bopomofo,
         * Han+Katakana+Hiragana (any 2 or all three)
         */
        highly_restrictive,
        /**
         * The text is not highly-restricted, and it doesn't contain Latin plus
         * certain other scripts, but otherwise allows for Latin + any single or
         * highly_restrictive. The disallowed scripts are the highly confusable Cyrillic, Greek, and Cans, plus
         * anything outside of http://unicode.org/reports/tr31/ Table 5a.<br>
         * Note: if any scripts are moved into 5a, they would have to be check for general confusability.
         */
        moderately_restrictive,
        /**
         * The text contains other combinations of scripts, or contains the Unknown script.
         */
        unrestricted
    }

    // Note: if UScript were an enum, we could avoid use of bitsets, but...
    private final BitSet singleScripts = new BitSet();
    private final HashSet<BitSet> combinations = new HashSet<BitSet>();
    private BitSet tempBitSet = new BitSet();

    /**
     * Determines the mixed-script level in the source text. For best results, the input text should be in NFKD already.
     * From http://www.unicode.org/reports/tr36/#Security_Levels_and_Alerts, but with modifications as
     * described under {@link #MixedScriptLevel}.
     * <p>
     * <b>Note:</b> thread-safe call
     * 
     * @param source
     *            Input text.
     * @return this, for chaining
     */
    public MixedScriptLevel getLevel(String source) {
        synchronized (this) {
            findScripts(source);
            MixedScriptLevel result = checkSimple();
            if (result == MixedScriptLevel.unrestricted) {
                result = checkHighlyRestricted();
                if (result == MixedScriptLevel.unrestricted) {
                    result = checkModeratelyRestricted();
                }
            }
            return result;
        }
    }

    /**
     * It would be nice if ICU had a constant set for this, generated at build time.
     */
    static final UnicodeSet HAS_EXTENSIONS;
    static {
        BitSet bitSet = new BitSet();
        UnicodeSet temp = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            UScript.getScriptExtensions(i, bitSet);
            if (bitSet.cardinality() > 0) {
                temp.add(i);
            }
        }
        HAS_EXTENSIONS = temp.freeze();
    }

    private boolean findScripts(String source) {
        singleScripts.clear();
        combinations.clear();
        tempBitSet.clear();

        // find all the scripts and combinations
        int cp;
        for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
            cp = source.codePointAt(i);
            if (HAS_EXTENSIONS.contains(cp)) {
                BitSet ext = new BitSet();
                UScript.getScriptExtensions(cp, ext);
                combinations.add(ext);
            } else {
                int script = UScript.getScript(cp);
                if (script == UScript.UNKNOWN) {
                    return false; // all unassigned character
                }
                if (script != UScript.COMMON && script != UScript.INHERITED) { // skip common/inherited
                    singleScripts.set(script);
                }
            }
        }
        return true;
    }

    private static final BitSet ALL_SCRIPTS = new BitSet();
    static {
        ALL_SCRIPTS.set(UScript.COMMON, UScript.CODE_LIMIT, true);
    }

    private MixedScriptLevel checkSimple() {
        // quick check on simple cases
        int singleScriptsCount = singleScripts.cardinality();
        // we can have a single script, and it occurs in all the combinations,
        // or we can have no single script, and there is one script common across all combinations.
        if (singleScriptsCount <= 1) {
            if (combinations.size() == 0) {
                return MixedScriptLevel.single;
            } else if (singleScriptsCount == 1) {
                // Ensure that the single script is in fact the one.
                skip: {
                    int single = singleScripts.nextSetBit(0);
                    for (BitSet combo : combinations) {
                        if (!combo.get(single)) {
                            break skip;
                        }
                    }
                    return MixedScriptLevel.single;
                }
            } else { // the count is zero, so make sure there is overlap
                tempBitSet.or(ALL_SCRIPTS); // set to all true to start with.
                // We successively AND in all the combos
                for (BitSet combo : combinations) {
                    tempBitSet.and(combo);
                }
                if (!tempBitSet.isEmpty()) {
                    return MixedScriptLevel.single;
                }
            }
        }
        return MixedScriptLevel.unrestricted;
    }

    private MixedScriptLevel checkHighlyRestricted() {
        // see if it matches a particular level
        loop: for (ScriptMatch match : ALLOWED) {
            // the match has to contain all the singleScripts
            if (!match.contains(singleScripts)) {
                continue loop;
            }
            // the match has to intersect all the combinations
            for (BitSet combo : combinations) {
                if (!match.intersects(combo)) {
                    continue loop;
                }
            }
            return match.level;
        }
        return MixedScriptLevel.unrestricted;
    }

    static final BitSet DISALLOWED_WITH_LATIN = new BitSet();
    static {
        // use Table 5a of http://unicode.org/reports/tr31/ excluding certain confusable scripts
        DISALLOWED_WITH_LATIN.xor(ALL_SCRIPTS);
        DISALLOWED_WITH_LATIN.clear(UScript.ARABIC);
        DISALLOWED_WITH_LATIN.clear(UScript.ARMENIAN);
        DISALLOWED_WITH_LATIN.clear(UScript.BENGALI);
        DISALLOWED_WITH_LATIN.clear(UScript.BOPOMOFO);
        // ALLOWED_WITH_LATIN.set(UScript.CYRILLIC); excluded, too confusable
        DISALLOWED_WITH_LATIN.clear(UScript.DEVANAGARI);
        DISALLOWED_WITH_LATIN.clear(UScript.ETHIOPIC);
        DISALLOWED_WITH_LATIN.clear(UScript.GEORGIAN);
        // ALLOWED_WITH_LATIN.set(UScript.GREEK); excluded, too confusable
        DISALLOWED_WITH_LATIN.clear(UScript.GUJARATI);
        DISALLOWED_WITH_LATIN.clear(UScript.GURMUKHI);
        DISALLOWED_WITH_LATIN.clear(UScript.HAN);
        DISALLOWED_WITH_LATIN.clear(UScript.HANGUL);
        DISALLOWED_WITH_LATIN.clear(UScript.HEBREW);
        DISALLOWED_WITH_LATIN.clear(UScript.HIRAGANA);
        DISALLOWED_WITH_LATIN.clear(UScript.KANNADA);
        DISALLOWED_WITH_LATIN.clear(UScript.KATAKANA);
        DISALLOWED_WITH_LATIN.clear(UScript.KHMER);
        DISALLOWED_WITH_LATIN.clear(UScript.LAO);
        DISALLOWED_WITH_LATIN.clear(UScript.LATIN);
        DISALLOWED_WITH_LATIN.clear(UScript.MALAYALAM);
        DISALLOWED_WITH_LATIN.clear(UScript.MYANMAR);
        DISALLOWED_WITH_LATIN.clear(UScript.ORIYA);
        DISALLOWED_WITH_LATIN.clear(UScript.SINHALA);
        DISALLOWED_WITH_LATIN.clear(UScript.TAMIL);
        DISALLOWED_WITH_LATIN.clear(UScript.TELUGU);
        DISALLOWED_WITH_LATIN.clear(UScript.THAANA);
        DISALLOWED_WITH_LATIN.clear(UScript.THAI);
        DISALLOWED_WITH_LATIN.clear(UScript.TIBETAN);
        // ALLOWED_WITH_LATIN.set(UScript.CANADIAN_ABORIGINAL); excluded, too confusable
        DISALLOWED_WITH_LATIN.clear(UScript.MONGOLIAN);
        DISALLOWED_WITH_LATIN.clear(UScript.TIFINAGH);
        DISALLOWED_WITH_LATIN.clear(UScript.YI);
    }

    private MixedScriptLevel checkModeratelyRestricted() {
        // if we were to remove Latin, it would be single or highly restricted
        // but exclude highly confusable scripts
        if (!singleScripts.get(UScript.LATIN)
            || DISALLOWED_WITH_LATIN.intersects(singleScripts)) {
            return MixedScriptLevel.unrestricted;
        }
        singleScripts.clear(UScript.LATIN);
        MixedScriptLevel result = checkSimple();
        if (result == MixedScriptLevel.unrestricted) {
            result = checkHighlyRestricted();
        }
        singleScripts.set(UScript.LATIN); // restore the value, just in case we change the code later
        // if we found a result, reset to moderately_restricted
        return result == MixedScriptLevel.unrestricted ? result : MixedScriptLevel.moderately_restrictive;
    }

    static class ScriptMatch {
        private final BitSet match;
        private final MixedScriptLevel level;

        public ScriptMatch(MixedScriptLevel level, int... scripts) {
            this.level = level;
            match = new BitSet();
            for (int script : scripts) {
                match.set(script);
            }
        }

        public boolean intersects(BitSet other) {
            return match.intersects(other);
        }

        public boolean contains(BitSet singleScripts) {
            return containsAll(match, singleScripts);
        }
    }

    // ugly, bitset doesn't have contains!
    public static boolean containsAll(BitSet a, BitSet b) {
        for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
            if (!a.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static final ScriptMatch[] ALLOWED = {
        new ScriptMatch(MixedScriptLevel.highly_restrictive, UScript.HAN, UScript.KATAKANA, UScript.HIRAGANA),
        new ScriptMatch(MixedScriptLevel.highly_restrictive, UScript.HAN, UScript.BOPOMOFO),
        new ScriptMatch(MixedScriptLevel.highly_restrictive, UScript.HAN, UScript.HANGUL),
    };

    private final static UnicodeSet BAD_NUMBERS = new UnicodeSet("[[:Nl:][:No:]]").freeze();
    private final static UnicodeSet DECIMAL_NUMBERS = new UnicodeSet("[:Nd:]").freeze();

    public enum NumberStatus {
        ok, non_nfkc_cf, mixedDecimals, nonDecimalNumbers
    }

    /**
     * Test numbers to see whether or not they are decimal, and if so, whether from different systems. Returns the first
     * error found, or 'ok'.
     * 
     * @param text
     * @return
     */
    public NumberStatus getNumberStatus(String text) {
        int base = -1;
        int cp;
        for (int i = 0; i < text.length(); i += Character.charCount(cp)) {
            cp = text.codePointAt(i);
            if (DECIMAL_NUMBERS.contains(cp)) {
                if (UCharacter.getIntPropertyValue(cp, UProperty.CHANGES_WHEN_NFKC_CASEFOLDED) != 0) {
                    return NumberStatus.non_nfkc_cf;
                }
                int newBase = cp - UCharacter.getNumericValue(cp); // this gets the zero value since we are guaranteed
                                                                   // all Nd's are in sequence
                if (newBase != base) {
                    if (base != -1) {
                        return NumberStatus.mixedDecimals;
                    }
                    base = newBase;
                }
            } else if (BAD_NUMBERS.contains(cp)) {
                return NumberStatus.nonDecimalNumbers;
            }
        }
        return NumberStatus.ok;
    }

    /**
     * Quick and dirty test; change to use test framework
     */
    public static void main(String[] args) {
        testLevels();
        testNumbers();
    }

    private static void testNumbers() {
        String[][] tests = {
            // pairs: expected value, then test string
            { "ok", "1234ab23" },
            { "ok", "٦ab٦" },
            { "mixedDecimals", "6٦" },
            { "mixedDecimals", "٦۶" },
            { "nonDecimalNumbers", "〢" },
            { "non_nfkc_cf", "𝟎" },
            { "nonDecimalNumbers", "⓵" },
        };
        TestMixedScript tester = new TestMixedScript();
        for (String[] testPair : tests) {
            NumberStatus expected = NumberStatus.valueOf(testPair[0]);
            NumberStatus actual = tester.getNumberStatus(testPair[1]);
            System.out.println((actual == expected ? "ok" : "BAD") + "\t" + actual + "\t" + expected + "\t"
                + testPair[1]);
        }
    }

    private static void testLevels() {
        String[][] tests = {
            // pairs: expected value, then test string
            { "moderately_restrictive", "aーb" }, // katakana, hiragana, plus script extension (30FC)

            // script extension tests
            { "unrestricted", "αーβ" }, // katakana, hiragana, plus script extension (30FC)
            { "moderately_restrictive", "aーb" }, // katakana, hiragana, plus script extension (30FC)
            { "moderately_restrictive", "aアーb" }, // katakana, hiragana, plus script extension (30FC)
            { "highly_restrictive", "㐀アーあ" }, // katakana, hiragana, plus script extension (30FC)
            { "single", "〆ー" }, // two overlapping script_extension characters
            { "unrestricted", "᠅ー" }, // two non-overlapping script_extension characters
            { "moderately_restrictive", "a᠅" }, // Latin + {Mongolian,Phags_Pa}

            // regular tests
            { "single", "ab cd" },
            { "highly_restrictive", "㐀ㄅ" }, // bopomofo
            { "highly_restrictive", "㐀가" }, // hangul
            { "highly_restrictive", "㐀あ" }, // hiragana
            { "highly_restrictive", "㐀ア" }, // katakana
            { "highly_restrictive", "㐀アあ" }, // katakana, hiragana
            { "moderately_restrictive", "aकb" }, // Latin+Deva
            { "unrestricted", "㐀アあ가" }, // katakana, hiragana, hangul
            { "unrestricted", "αa" }, // Latin+Greek
            { "unrestricted", "αकb" }, // Latin+Greek+Deva
        };
        TestMixedScript tester = new TestMixedScript();
        for (String[] testPair : tests) {
            MixedScriptLevel expected = MixedScriptLevel.valueOf(testPair[0]);
            MixedScriptLevel actual = tester.getLevel(testPair[1]);
            System.out.println((actual == expected ? "ok" : "BAD") + "\t" + actual + "\t" + expected + "\t"
                + testPair[1]);
        }
    }
}
