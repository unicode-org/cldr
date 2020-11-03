package org.unicode.cldr.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Comparators.lexicographical;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public final class PreferredAndAllowedHour implements Comparable<PreferredAndAllowedHour> {
    // DO NOT change enum item names, they are mapped directly from data values via "valueOf".
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
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    // For splitting the style ID string.
    private static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults();

    // If this used "getter" method references it wouldn't need so much explicit generic typing.
    private static final Comparator<PreferredAndAllowedHour> COMPARATOR =
        Comparator.<PreferredAndAllowedHour, HourStyle>comparing(t -> t.preferred)
            .thenComparing(t -> t.allowed, lexicographical(Comparator.<HourStyle>naturalOrder()));

    public final HourStyle preferred;
    /** Unique allowed styles, in the order they were specified during construction. */
    public final ImmutableList<HourStyle> allowed;

    /**
     * Creates a PreferredAndAllowedHour instance with "allowed" styles derived from single
     * character IDs in the given collection. Note that the iteration order of the allowed
     * styles is retained.
     *
     * <p>This constructor is limiting, since some styles are identified by two character
     * strings, which cannot be referenced via this constructor.
     */
    public PreferredAndAllowedHour(char preferred, Set<Character> allowed) {
        this(String.valueOf(preferred), allowed.stream().map(String::valueOf));
    }

    /**
     * Creates a PreferredAndAllowedHour instance with "allowed" styles derived from a space
     * separated list of unique IDs. Note that the iteration order of the allowed styles is
     * retained, since in some situations it is necessary that the preferred style should
     * also be the first allowed style. The list of allowed style IDs must not contain
     * duplicates.
     */
    public PreferredAndAllowedHour(String preferred, String allowedString) {
        this(preferred, SPACE_SPLITTER.splitToList(allowedString).stream());
    }

    private PreferredAndAllowedHour(String preferredStyle, Stream<String> allowedStyles) {
        this.preferred = checkNotNull(HourStyle.valueOf(preferredStyle));
        this.allowed = allowedStyles.map(HourStyle::valueOf).collect(toImmutableList());
        checkArgument(allowed.stream().distinct().count() == allowed.size(),
                "Allowed (%s) must not contain duplicates", allowed);
        // Note: In *some* cases the preferred style is required to be the first style in
        // the allowed set, but not always (thus we cannot do a better check here).
        // TODO: Figure out if we can enforce preferred == first(allowed) here.
        checkArgument(allowed.contains(preferred),
                "Allowed (%s) must contain preferred (%s)", allowed, preferred);
    }

    @Override
    public int compareTo(PreferredAndAllowedHour other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return toString(ImmutableList.of("?"));
    }

    public String toString(Collection<String> regions) {
        Joiner withSpaces = Joiner.on(" ");
        return "<hours preferred=\""
            + preferred
            + "\" allowed=\""
            + withSpaces.join(allowed)
            + "\" regions=\""
            + withSpaces.join(regions)
            + "\"/>";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PreferredAndAllowedHour && compareTo((PreferredAndAllowedHour) obj) == 0;
    }
}