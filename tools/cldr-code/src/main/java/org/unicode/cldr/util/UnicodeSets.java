package org.unicode.cldr.util;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class UnicodeSets {
    /**
     * Return the strings that are redundant, where all of the string's code points are already in
     * the UnicodeSet. <br>
     * Example: in [abc{ab}{ad}], {ab} is redundant, but {ad} is not. Also checks where a
     * combination of the string's substrings are otherwise in the UnicodeSet. <br>
     * Example: in [abc{ab}{ad}{abd}], {abd} is also redundant.
     *
     * @param source
     * @return
     */
    public static Set<String> getRedundantStrings(UnicodeSet source) {
        Collection<String> strings = source.strings();
        if (strings.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = null;
        int cp;
        main:
        for (String s : strings) {
            for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
                cp = s.codePointAt(i);
                if (!source.contains(cp)) {
                    continue main;
                }
            }
            if (result == null) { // lazy creation
                result = new HashSet<>();
            }
            result.add(s);
        }
        if (result == null) {
            result = Collections.emptySet();
        }
        // now check remaining strings
        UnicodeSet remaining = new UnicodeSet(source);
        remaining.removeAll(result);
        for (String s : remaining.strings()) {
            UnicodeSet temp = new UnicodeSet(remaining).remove(s);
            int position = temp.span(s, SpanCondition.CONTAINED);
            if (position == s.length()) {
                result.add(s);
            }
        }
        return result;
    }

    /** Modifies a to replace all its strings, adding their codepoints individually */
    public static UnicodeSet flatten(UnicodeSet a) {
        a.stringStream().forEach(x -> a.addAll(x));
        return a.removeAllStrings();
    }

    /** Modifies a to replace any of its strings that are covered by individual code points. */
    public static UnicodeSet flattenUnnecessary(UnicodeSet a) {
        Set<String> setCopy = new HashSet<>(a.strings());
        a.removeAllStrings();

        for (String s : setCopy) {
            if (!a.containsAll(s)) {
                a.add(s);
            }
        }
        return a;
    }

    /**
     * Behaves like a.containsAll(b), except that each string is counted as being contained if all
     * of its code points are contained. Thus [a b c].containsAll([a {bc}]). <br>
     * Note that a.removeAllFlattening(b).isEmpty IFF b.containsAllFlattening(a).
     */
    public static boolean containsAllFlattening(UnicodeSet a, final UnicodeSet b) {
        return a.containsAll(b)
                || b.rangeStream().allMatch(x -> a.contains(x.codepoint, x.codepointEnd))
                        && b.stringStream().allMatch(x -> a.containsAll(x));
    }

    /**
     * Behaves like a.removeAll(b), except that it also removes each string in a whose code points
     * are all in b. <br>
     * Note that a.removeAllFlattening(b).isEmpty IFF b.containsAllFlattening(a).
     */
    public static UnicodeSet removeAllFlattening(UnicodeSet a, final UnicodeSet b) {
        return a.removeAll(b)
                .removeAll(
                        a.stringStream()
                                .filter(x -> !b.containsAll(x))
                                .collect(Collectors.toSet()));
    }
}
