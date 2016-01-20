package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;

public final class PreferredAndAllowedHour implements Comparable<PreferredAndAllowedHour> {

    public enum HourStyle {
        h, H, k, K, hb, hB, Hb, HB;
        public static boolean isHourCharacter(String c) {
            try {
                HourStyle.valueOf(c);
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

    public PreferredAndAllowedHour(HourStyle preferred, Set<HourStyle> allowed) {
        if (preferred == null) {
            throw new NullPointerException();
        }
        if (!allowed.contains(preferred)) {
            throw new IllegalArgumentException("Allowed (" + allowed +
                ") must contain preferred(" + preferred +
                ")");
        }
        this.preferred = preferred;
        this.allowed = Collections.unmodifiableSet(new LinkedHashSet<>(allowed));
    }
    
    public PreferredAndAllowedHour(String preferred2, String allowedString) {
        this(HourStyle.valueOf(preferred2), mungeOperands(allowedString));
    }

    private static EnumSet<HourStyle> mungeSet(Set<Character> allowed) {
        EnumSet<HourStyle> temp = EnumSet.noneOf(HourStyle.class);
        for (char c : allowed) {
            temp.add(HourStyle.valueOf(String.valueOf(c)));
        }
        return temp;
    }

    static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults();
    
    private static LinkedHashSet<HourStyle> mungeOperands(String allowedString) {
        LinkedHashSet<HourStyle> allowed = new LinkedHashSet<>();
        for (String s : SPACE_SPLITTER.split(allowedString)) {
            allowed.add(HourStyle.valueOf(s));
        }
        return allowed;
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