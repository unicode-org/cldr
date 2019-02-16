package org.unicode.cldr.util;

import java.util.Locale;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * A simple class representing an enumeration of possible CLDR coverage levels. Levels may change in the future.
 *
 * @author davis
 *
 */
public enum Level {
    UNDETERMINED(0, "none", 0), CORE(10, "G4", 100),
    // POSIX(20, "G4", 100),
    // MINIMAL(30, "G3.5", 90),
    BASIC(40, "G3", 80), MODERATE(60, "G2", 70), MODERN(80, "G1", 50), COMPREHENSIVE(100, "G0", 2);
    //OPTIONAL(101, "optional", 1);

    public static final Set<Level> CORE_TO_MODERN = ImmutableSet.of(CORE, BASIC, MODERATE, MODERN);
    
    @Deprecated
    public static final Level POSIX = BASIC;
    @Deprecated
    public static final Level MINIMAL = BASIC;
    @Deprecated
    public static final Level OPTIONAL = COMPREHENSIVE;

    private final byte level;
    private final String altName;
    private final int value;

    private static final Level[] VALUES = values();

    /**
     * returns value ranging from 100 (core) to 1 (optional). Most clients want getLevel instead.
     *
     * @return
     */
    public int getValue() {
        return value;
    }

    private Level(int i, String altName, int value) {
        this.level = ((byte) i);
        this.altName = altName;
        this.value = value;
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

    public String toString() {
        return this.name().toLowerCase();
    }

    // public int compareTo(Level o) {
    // int otherLevel = ((Level) o).level;
    // return level < otherLevel ? -1 : level > otherLevel ? 1 : 0;
    // }

    static final StandardCodes sc = StandardCodes.make();

    public static int getDefaultWeight(String organization, String desiredLocale) {
        Level level = sc.getLocaleCoverageLevel(organization, desiredLocale);
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
}