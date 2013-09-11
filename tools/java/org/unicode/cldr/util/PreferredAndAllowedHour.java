package org.unicode.cldr.util;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;

public final class PreferredAndAllowedHour implements Comparable<PreferredAndAllowedHour> {

    public enum HourStyle {
        h, H, k, K;
        public static boolean isHourCharacter(char c) {
            try {
                HourStyle.valueOf(String.valueOf(c));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public final HourStyle preferred;
    public final Set<HourStyle> allowed;

    public PreferredAndAllowedHour(char preferred, Set<Character> allowed) {
        this(HourStyle.valueOf(String.valueOf(preferred)), mungeSet(allowed));
    }

    private static EnumSet<HourStyle> mungeSet(Set<Character> allowed) {
        EnumSet<HourStyle> temp = EnumSet.noneOf(HourStyle.class);
        for (char c : allowed) {
            temp.add(HourStyle.valueOf(String.valueOf(c)));
        }
        return temp;
    }

    public PreferredAndAllowedHour(HourStyle preferred, Set<HourStyle> allowed) {
        this.preferred = preferred;
        if (preferred == null) {
            throw new NullPointerException();
        }
        this.allowed = allowed;
        if (!this.allowed.contains(this.preferred)) {
            throw new IllegalArgumentException("Allowed (" + allowed +
                ") must contain preferred(" + preferred +
                ")");
        }
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
        int diff = preferred.compareTo(arg0.preferred);
        if (diff != 0) return diff;
        return CollectionUtilities.compare(allowed, arg0.allowed);
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