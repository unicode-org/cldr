package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Locale;
import java.util.Set;

/**
 * A simple class representing an enumeration of possible CLDR coverage levels. Levels may change in
 * the future.
 *
 * @author davis
 */
public enum Level {
    UNDETERMINED(0, "none", 0, "�"),
    CORE(10, "G4", 100, "ⓒ"),
    BASIC(40, "G3", 80, "ⓑ"),
    MODERATE(60, "G2", 70, "ⓜ"),
    MODERN(80, "G1", 50, "🄼"),
    COMPREHENSIVE(100, "G0", 2, "🄲");

    public static final Set<Level> CORE_TO_MODERN =
            ImmutableSortedSet.of(CORE, BASIC, MODERATE, MODERN);

    @Deprecated public static final Level POSIX = BASIC;
    @Deprecated public static final Level MINIMAL = BASIC;
    @Deprecated public static final Level OPTIONAL = COMPREHENSIVE;

    private final byte level;
    private final String altName;
    private final int value;
    private String abbreviation;

    private static final Level[] VALUES = values();

    /**
     * returns value ranging from 100 (core) to 1 (optional). Most clients want getLevel instead.
     *
     * @return
     */
    public int getValue() {
        return value;
    }

    private Level(int i, String altName, int value, String abbreviation) {
        this.level = ((byte) i);
        this.altName = altName;
        this.value = value;
        this.abbreviation = abbreviation;
    }

    public static Level get(String name) {
        try {
            return Level.valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (RuntimeException e) {
            for (Level level : VALUES) {
                if (name.equalsIgnoreCase(level.altName)) {
                    return level;
                }
            }
            if (name.equalsIgnoreCase("POSIX")) {
                return POSIX;
            } else if (name.equalsIgnoreCase("MINIMAL")) {
                return MINIMAL;
            } else if (name.equalsIgnoreCase("OPTIONAL")) {
                return OPTIONAL;
            }
            return UNDETERMINED;
        }
    }

    public String getAltName() {
        return altName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static int getDefaultWeight(String organization, String desiredLocale) {
        Level level = StandardCodes.make().getLocaleCoverageLevel(organization, desiredLocale);
        if (level.compareTo(Level.MODERATE) >= 0) {
            return 4;
        }
        return 1;
    }

    /**
     * This is the numeric value used in the coverage level XML.
     *
     * @return range from 10 (core) to 100 (comprehensive).
     */
    public byte getLevel() {
        return level;
    }

    public static Level fromLevel(int level) {
        for (Level result : Level.values()) {
            if (level == result.level) {
                return result;
            }
        }

        if (level == 20) {
            return Level.POSIX;
        } else if (level == 30) {
            return Level.MINIMAL;
        } else if (level == 101) {
            return Level.OPTIONAL;
        }
        throw new IllegalArgumentException(String.valueOf(level));
    }

    public static Level fromString(String source) {
        return Level.get(source);
    }

    /**
     * Return the minimum level between two For example, Level.min(COMPREHENSIVE, MODERN) = MODERN
     *
     * @param a
     * @param b
     * @return level with the minimal getLevel() value
     */
    public static Level min(Level a, Level b) {
        return Level.fromLevel(Math.min(a.getLevel(), b.getLevel()));
    }

    /**
     * Return the maximum level between two For example, Level.min(COMPREHENSIVE, MODERN) = MODERN
     *
     * @param a
     * @param b
     * @return level with the minimal getLevel() value
     */
    public static Level max(Level a, Level b) {
        if (a == null) {
            return b;
        }
        return Level.fromLevel(Math.max(a.getLevel(), b.getLevel()));
    }

    public static Level max(Level... levels) {
        Level result = Level.UNDETERMINED;
        for (Level level : levels) {
            if (level != null) {
                result = max(result, level);
            }
        }
        return result;
    }

    /**
     * Returns true if this is at least the other level. Example: lev.isAtLeast(Level.MODERN)
     *
     * @param other
     * @return
     */
    public boolean isAtLeast(Level other) {
        return getLevel() >= other.getLevel();
    }

    /**
     * @return true if this is > other
     */
    public boolean isAbove(Level other) {
        return getLevel() > other.getLevel();
    }
}
