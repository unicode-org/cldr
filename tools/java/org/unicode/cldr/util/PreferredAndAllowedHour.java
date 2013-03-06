package org.unicode.cldr.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.ComparisonStyle;

public final class PreferredAndAllowedHour implements Comparable<PreferredAndAllowedHour> {

    public static final UnicodeSet HOURS = new UnicodeSet("[hHkK]").freeze();

    public final char preferred;
    public final Set<Character> allowed;

    public PreferredAndAllowedHour(char preferred, Set<Character> allowed) {
        if (!allowed.contains(preferred)) {
            throw new IllegalArgumentException("Allowed (" + allowed +
                ") must contain preferred(" + preferred +
                ")");
        }
        this.allowed = Collections.unmodifiableSet(allowed);
        this.preferred = preferred;
    }

    public PreferredAndAllowedHour(String preferred2, String allowedString) {
        this(mungeOperands(preferred2, allowedString));
    }

    private static Object[] mungeOperands(String preferred2, String allowedString) {
        if (preferred2.length() != 1) {
            throw new IllegalArgumentException("Preferred be one character: " + preferred2);
        }
        LinkedHashSet<Character> allowed2 = new LinkedHashSet<Character>();
        for (int i = 0; i < allowedString.length(); ++i) {
            char c = allowedString.charAt(i);
            if (UCharacter.isWhitespace(c)) {
                continue;
            }
            allowed2.add(c);
        }
        return new Object[] { preferred2.charAt(0), allowed2 };
    }

    private PreferredAndAllowedHour(Object[] mungedOperands) {
        this((Character) mungedOperands[0], (Set<Character>) mungedOperands[1]);
    }

    @Override
    public int compareTo(PreferredAndAllowedHour arg0) {
        if (preferred < arg0.preferred) return -1;
        if (preferred > arg0.preferred) return 1;
        return UnicodeSet.compare(allowed, arg0.allowed, ComparisonStyle.LONGER_FIRST);
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return preferred + ":" + allowed;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PreferredAndAllowedHour && compareTo((PreferredAndAllowedHour) obj) == 0;
    }
}