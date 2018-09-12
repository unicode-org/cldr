package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.util.CollectionUtilities;

public final class PreferredAndAllowedHour implements Comparable<PreferredAndAllowedHour> {

    public enum HourStyle {
        H, Hb(H), HB(H), k, h, hb(h), hB(h), K;
        public final HourStyle base;

        HourStyle() {
            base = this;
        }

        HourStyle(HourStyle base) {
            this.base = base;
        }

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
    public final List<HourStyle> allowed;

    public PreferredAndAllowedHour(char preferred, Collection<Character> allowed) {
        this(HourStyle.valueOf(String.valueOf(preferred)), mungeSet(allowed));
    }

    public PreferredAndAllowedHour(Collection<HourStyle> allowed) {
        this(allowed.iterator().next(), allowed);
    }

    public PreferredAndAllowedHour(HourStyle preferred, Collection<HourStyle> allowed) {
        if (preferred == null) {
            throw new NullPointerException();
        }
        if (!allowed.contains(preferred)) {
            throw new IllegalArgumentException("Allowed (" + allowed +
                ") must contain preferred(" + preferred +
                ")");
        }
        this.preferred = preferred;
        this.allowed = ImmutableList.copyOf(new LinkedHashSet<>(allowed));
    }

    public PreferredAndAllowedHour(String preferred2, String allowedString) {
        this(HourStyle.valueOf(preferred2), mungeOperands(allowedString));
    }

    private static EnumSet<HourStyle> mungeSet(Collection<Character> allowed) {
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
        return CollectionUtilities.compare(allowed.iterator(), arg0.allowed.iterator());
    }

    @Override
    public String toString() {
        return toString(Collections.singleton("?"));
    }

    public String toString(Collection<String> regions) {
        return "<hours preferred=\""
            + preferred
            + "\" allowed=\""
            + CollectionUtilities.join(allowed, " ")
            + "\" regions=\""
            + CollectionUtilities.join(regions, " ")
            + "\"/>";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PreferredAndAllowedHour && compareTo((PreferredAndAllowedHour) obj) == 0;
    }
}