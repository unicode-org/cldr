package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;

final public class UnicodeSets {
    /**
     * Return the strings that are redundant, 
     * where all of the string's code points are already in the UnicodeSet.
     * <br>Example: in [abc{ab}{ad}], {ab} is redundant, but {ad} is not.
     * Also checks where a combination of the string's substrings are otherwise
     * in the UnicodeSet.
     * <br>Example: in [abc{ab}{ad}{abd}], {abd} is also redundant.
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
}
