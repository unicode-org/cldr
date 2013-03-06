package org.unicode.cldr.draft;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

/**
 * This class analyzes a possible identifier for script and identifier status.
 * Use it by calling setIdentifierProfile then setIdentifier.
 * At this point:
 * <ol>
 * <li>call getScripts for the specific scripts in the identifier. The identifier contains at least one character in
 * each of these.
 * <li>call getAlternates to get cases where a character is not limited to a single script. For example, it could be
 * either Katakana or Hiragana.
 * <li>call getCommonAmongAlternates to find out if any scripts are common to all the alternates.
 * <li>call getNumerics to get a representative character (with value zero) for each of the decimal number systems in
 * the identifier.
 * <li>call getRestrictionLevel to see what the UTS36 restriction level is. (This has some proposed changes from the
 * current one, however.)
 * </ol>
 * 
 * @author markdavis
 * @internal
 */
public class IdentifierInfo {

    public enum IdentifierStatus {
        /** Only ASCII characters: U+0000..U+007F **/
        ASCII,
        /**
         * All characters in each identifier must be from a single script, or
         * from the combinations: Latin + Han + Hiragana + Katakana; Latin + Han
         * + Bopomofo; or Latin + Han + Hangul. Note that this level will satisfy
         * the vast majority of Latin-script users; also that TR36 has ASCII instead of Latin.
         **/
        HIGHLY_RESTRICTIVE,
        /**
         * Allow Latin with other scripts except Cyrillic, Greek, Cherokee
         * Otherwise, the same as Highly Restrictive
         **/
        MODERATELY_RESTRICTIVE,
        /**
         * Allow arbitrary mixtures of scripts, such as Ωmega, Teχ, HλLF-LIFE,
         * Toys-Я-Us. Otherwise, the same as Moderately Restrictive
         **/
        MINIMALLY_RESTRICTIVE,
        /**
         * Any valid identifiers, including characters outside of the Identifier
         * Profile, such as I♥NY.org
         **/
        UNRESTRICTIVE
    }

    private static final UnicodeSet ASCII = new UnicodeSet(0, 0x7F).freeze();

    private String identifier;
    private final BitSet requiredScripts = new BitSet();
    private final Set<BitSet> scriptSetSet = new HashSet<BitSet>();
    private final BitSet commonAmongAlternates = new BitSet();
    private final UnicodeSet numerics = new UnicodeSet();
    private final UnicodeSet identifierProfile = new UnicodeSet(0, 0x10FFFF);

    private IdentifierInfo clear() {
        requiredScripts.clear();
        scriptSetSet.clear();
        numerics.clear();
        commonAmongAlternates.clear();
        return this;
    }

    public IdentifierInfo setIdentifierProfile(UnicodeSet identifierProfile) {
        this.numerics.set(numerics);
        return this;
    }

    public UnicodeSet getIdentifierProfile() {
        return new UnicodeSet(identifierProfile);
    }

    public IdentifierInfo setIdentifier(String identifier) {
        this.identifier = identifier;
        clear();
        BitSet temp = new BitSet(); // Will reuse this.
        int cp;
        for (int i = 0; i < identifier.length(); i += Character.charCount(i)) {
            cp = Character.codePointAt(identifier, i);
            // Store a representative character for each kind of decimal digit
            if (UCharacter.getType(cp) == UCharacterCategory.DECIMAL_DIGIT_NUMBER) {
                // Just store the zero character as a representative for comparison. Unicode guarantees it is cp - value
                numerics.add(cp - UCharacter.getNumericValue(cp));
            }
            UScript.getScriptExtensions(cp, temp);
            temp.clear(UScript.COMMON);
            temp.clear(UScript.INHERITED);
            if (temp.cardinality() == 0) {
                // HACK for older version of ICU
                requiredScripts.set(UScript.getScript(cp));
            } else if (temp.cardinality() == 1) {
                // Single script, record it.
                requiredScripts.or(temp);
            } else if (!requiredScripts.intersects(temp)
                && scriptSetSet.add(temp)) {
                // If the set hasn't been added already, add it and create new temporary for the next pass,
                // so we don't rewrite what's already in the set.
                temp = new BitSet();
            }
        }
        // Now make a final pass through to remove alternates that came before singles.
        // [Kana], [Kana Hira] => [Kana]
        // This is relatively infrequent, so doesn't have to be optimized.
        if (scriptSetSet.size() == 0) {
            commonAmongAlternates.clear();
        } else {
            commonAmongAlternates.set(0, UScript.CODE_LIMIT);
            for (Iterator<BitSet> it = scriptSetSet.iterator(); it.hasNext();) {
                final BitSet next = it.next();
                if (requiredScripts.intersects(next)) {
                    it.remove();
                } else {
                    // [[Arab Syrc Thaa]; [Arab Syrc]] => [[Arab Syrc]]
                    for (BitSet other : scriptSetSet) {
                        if (next != other && contains(next, other)) {
                            it.remove();
                            break;
                        }
                    }
                }
                commonAmongAlternates.and(next); // get the intersection.
            }
            if (commonAmongAlternates.size() == 0) {
                commonAmongAlternates.clear();
            }
        }
        // Note that the above code doesn't minimize alternatives. That is, it does not collapse
        // [[Arab Syrc Thaa]; [Arab Syrc]] to [[Arab Syrc]]
        // That would be a possible optimization, but is probably not worth the extra processing
        return this;
    }

    static final BitSet COMMON_AND_INHERITED = set(new BitSet(), UScript.COMMON, UScript.INHERITED);

    public static boolean isMultiScript(String identifier) {
        // Non-optimized code, for simplicity
        Set<BitSet> setOfScriptSets = new HashSet<BitSet>();
        BitSet temp = new BitSet();
        int cp;
        for (int i = 0; i < identifier.length(); i += Character.charCount(i)) {
            cp = Character.codePointAt(identifier, i);
            UScript.getScriptExtensions(cp, temp);
            if (temp.cardinality() == 0) {
                // HACK for older version of ICU
                final int script = UScript.getScript(cp);
                temp.set(script);
            }
            temp.andNot(COMMON_AND_INHERITED);
            if (temp.cardinality() != 0 && setOfScriptSets.add(temp)) {
                // If the set hasn't been added already, add it and create new temporary for the next pass,
                // so we don't rewrite what's already in the set.
                temp = new BitSet();
            }
        }
        if (setOfScriptSets.size() == 0) {
            return true; // trivially true
        }
        temp.clear();
        // check to see that there is at least one script common to all the sets
        boolean first = true;
        for (BitSet other : setOfScriptSets) {
            if (first) {
                temp.or(other);
                first = false;
            } else {
                temp.and(other);
            }
        }
        return temp.cardinality() != 0;
    }

    public boolean hasMixedNumberSystems(String identifier) {
        int cp;
        UnicodeSet numerics = new UnicodeSet();
        for (int i = 0; i < identifier.length(); i += Character.charCount(i)) {
            cp = Character.codePointAt(identifier, i);
            // Store a representative character for each kind of decimal digit
            switch (UCharacter.getType(cp)) {
            case UCharacterCategory.DECIMAL_DIGIT_NUMBER:
                // Just store the zero character as a representative for comparison.
                // Unicode guarantees it is cp - value
                numerics.add(cp - UCharacter.getNumericValue(cp));
                break;
            case UCharacterCategory.OTHER_NUMBER:
            case UCharacterCategory.LETTER_NUMBER:
                throw new IllegalArgumentException("Should not be in identifiers.");
            }
        }
        return numerics.size() > 1;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BitSet getScripts() {
        return (BitSet) requiredScripts.clone();
    }

    public Set<BitSet> getAlternates() {
        Set<BitSet> result = new HashSet<BitSet>();
        for (BitSet item : scriptSetSet) {
            result.add((BitSet) item.clone());
        }
        return result;
    }

    public UnicodeSet getNumerics() {
        return new UnicodeSet(numerics);
    }

    public BitSet getCommonAmongAlternates() {
        return (BitSet) commonAmongAlternates.clone();
    }

    // BitSet doesn't support "contains(...)", so we have inverted constants
    // They are private; they can't be made immutable in Java.
    private final static BitSet JAPANESE = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.HIRAGANA,
        UScript.KATAKANA);
    private final static BitSet CHINESE = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.BOPOMOFO);
    private final static BitSet KOREAN = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.HANGUL);
    private final static BitSet CONFUSABLE_WITH_LATIN = set(new BitSet(), UScript.CYRILLIC, UScript.GREEK,
        UScript.CHEROKEE);

    public IdentifierStatus getRestrictionLevel() {
        if (!identifierProfile.containsAll(identifier) || getNumerics().size() > 1) {
            return IdentifierStatus.UNRESTRICTIVE;
        }
        if (ASCII.containsAll(identifier)) {
            return IdentifierStatus.ASCII;
        }
        BitSet temp = new BitSet();
        temp.or(requiredScripts);
        temp.clear(UScript.COMMON);
        temp.clear(UScript.INHERITED);
        // This is a bit tricky. We look at a number of factors.
        // The number of scripts in the text.
        // Plus 1 if there is some commonality among the alternates (eg [Arab Thaa]; [Arab Syrc])
        // Plus number of alternates otherwise (this only works because we only test cardinality up to 2.)
        final int cardinalityPlus = temp.cardinality() + (commonAmongAlternates.isEmpty() ? scriptSetSet.size() : 1);
        if (cardinalityPlus < 2) {
            return IdentifierStatus.HIGHLY_RESTRICTIVE;
        }
        if (containsWithAlternates(JAPANESE, temp)
            || containsWithAlternates(CHINESE, temp)
            || containsWithAlternates(KOREAN, temp)) {
            return IdentifierStatus.HIGHLY_RESTRICTIVE;
        }
        if (cardinalityPlus == 2
            && temp.get(UScript.LATIN)
            && !temp.intersects(CONFUSABLE_WITH_LATIN)) {
            return IdentifierStatus.MODERATELY_RESTRICTIVE;
        }
        return IdentifierStatus.MINIMALLY_RESTRICTIVE;
    }

    @Override
    public String toString() {
        return identifier + ", " + identifierProfile.toPattern(false)
            + ", " + getRestrictionLevel()
            + ", " + displayScripts(requiredScripts)
            + ", " + displayAlternates(scriptSetSet)
            + ", " + numerics.toPattern(false);
    }

    private boolean containsWithAlternates(BitSet container, BitSet containee) {
        if (!contains(container, containee)) {
            return false;
        }
        for (BitSet alternatives : scriptSetSet) {
            if (!container.intersects(alternatives)) {
                return false;
            }
        }
        return true;
    }

    public static String displayAlternates(Collection<BitSet> alternates) {
        StringBuilder result = new StringBuilder();
        for (BitSet item : alternates) {
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(displayScripts(item));
        }
        return result.toString();
    }

    public static String displayScripts(BitSet scripts) {
        StringBuilder result = new StringBuilder("[");
        for (int i = scripts.nextSetBit(0); i >= 0; i = scripts.nextSetBit(i + 1)) {
            if (result.length() != 1) {
                result.append(' ');
            }
            result.append(UScript.getShortName(i));
        }
        return result.append("]").toString();
    }

    public static BitSet parseScripts(String scriptsString) {
        BitSet result = new BitSet();
        for (String item : scriptsString.trim().split(",?\\s+")) {
            if (!item.isEmpty()) {
                result.set(UScript.getCodeFromName(item));
            }
        }
        return result;
    }

    public static Set<BitSet> parseAlternates(String scriptsSetString) {
        Set<BitSet> result = new HashSet<BitSet>();
        for (String item : scriptsSetString.trim().split("\\s*;\\s*")) {
            if (!item.isEmpty()) {
                result.add(parseScripts(item));
            }
        }
        return result;
    }

    /**
     * Test containment. Should be a method on BitSet...
     * 
     * @param container
     * @param containee
     * @return
     */
    public static final boolean contains(BitSet container, BitSet containee) {
        for (int i = containee.nextSetBit(0); i >= 0; i = containee.nextSetBit(i + 1)) {
            if (!container.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets a number of values at once. Should be on BitSet.
     * 
     * @param container
     * @param containee
     * @return
     * @return
     */
    public static final BitSet set(BitSet bitset, int... values) {
        for (int value : values) {
            bitset.set(value);
        }
        return bitset;
    }
}
