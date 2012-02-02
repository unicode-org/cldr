package org.unicode.cldr.util;

import java.util.Locale;


/**
   * A simple class representing an enumeration of possible CLDR coverage levels. Levels may change in the future.
   * @author davis
   *
   */
  public enum Level {
    UNDETERMINED(0, "none",0),
    CORE(10,"G4", 100),
    POSIX(20,"G4", 100),
    MINIMAL(30,"G3.5", 90),
    BASIC(40,"G3", 80),
    MODERATE(60, "G2", 70),
    MODERN(80, "G1", 50),
    COMPREHENSIVE(100, "G0", 2),
    OPTIONAL(101, "optional", 1);
    
    private final byte level;
    private final String altName;
    private final int value;
    
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
        for (Level level : Level.values()) {
          if (name.equalsIgnoreCase(level.altName)) {
            return level;
          }
        }
        return UNDETERMINED;
      }
    }
    
    public String toString() {
      return this.name().toLowerCase();
    }
    
//    public int compareTo(Level o) {
//      int otherLevel = ((Level) o).level;
//      return level < otherLevel ? -1 : level > otherLevel ? 1 : 0;
//    }
    
    static final StandardCodes sc = StandardCodes.make();

    public static int getDefaultWeight(String organization, String desiredLocale) {
      Level level = sc.getLocaleCoverageLevel(organization, desiredLocale);
      if (level.compareTo(Level.MODERATE) >= 0) {
        return 4;
      }
      return 1;
    }

    public byte getLevel() {
        return level;
    }
    
    public static Level fromLevel(int level) {
        for (Level result : Level.values()) {
            if (level == result.level) {
                return result;
            }
        }
        throw new IllegalArgumentException(String.valueOf(level));
    }
  }