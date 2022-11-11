package org.unicode.cldr.util;

import java.util.Locale;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;

/**
 * Provide a set of code point abbreviations. Includes conversions to and from codepoints, including hex.
 */
public enum CodePointEscaper {
    SP(0x20, "space", "ASCII space"),
    NBSP(0xA0, "no-break space"),

    NSP(0x2009, "narrow space", "thin space"),
    NNBSP(0x202F, "narrow no-break space", "thin no-break space"),

    ZWS(0x200B, "word non-joiner", "zero-width space"),
    WJ(0x2060, "word joiner", "zero-width no-break space"),

    ALM(0x061C, "Arabic letter mark"),
    LRM(0x200E, "left-right mark"),
    RLM(0x200F, "right-left mark"),
    ZWNJ(0x200C, "zero-width non-joiner"),
    ZWJ(0x200D, "zero-width joiner");

    private final int codePoint;
    private final Set<String> longNames;
    private CodePointEscaper(int codePoint, String... longNames) {
        this.codePoint = codePoint;
        this.longNames = ImmutableSet.copyOf(longNames);
    }
    private static final UnicodeMap<CodePointEscaper> _fromCodePoint = new UnicodeMap<>();
    static {
        for (CodePointEscaper abbr : CodePointEscaper.values()) {
            CodePointEscaper oldValue = _fromCodePoint.get(abbr.codePoint);
            if (oldValue != null) {
                throw new IllegalArgumentException("Abbreviation code points collide: " + oldValue.name() + ", " + abbr.name());
            }
            _fromCodePoint.put(abbr.codePoint, abbr);
        }
        _fromCodePoint.freeze();
    }

    /**
     * Return long names for this character. The set is immutable and ordered,
     * with the first name being the most user-friendly.
     */
    public Set<String> getLongNames() {
        return longNames;
    }
    /**
     * Return the code point for this character.
     */
    public int getCodePoint() {
        return codePoint;
    }

    /**
     * Return an Abbreviation for a string (or CharSequence), or null if not found.
     * Handles lower and uppercase input.
     */
    public static CodePointEscaper fromString(CharSequence source) {
        try {
            return valueOf(source.toString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns an Abbreviation for a codePoint, or null if not found.
     */
    public static CodePointEscaper fromCodePoint(int codePoint) {
        return _fromCodePoint.get(codePoint);
    }


    /**
     * Returns a codepoint from an abbreviation string or hex string.
     */
    public static int fromAbbreviationOrHex(CharSequence value) {
        CodePointEscaper abbreviation = CodePointEscaper.fromString(value.toString());
        if (abbreviation != null) {
            return abbreviation.codePoint;
        }
        int codePoint = Integer.parseInt(value.toString(), 16);
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Code point out of bounds: " + value);
        }
        return codePoint;
    }

    /**
     * Returns an abbreviation string or hex string from a code point.
     */
    public static String toAbbreviationOrHex(int codePoint) {
        CodePointEscaper result = CodePointEscaper.fromCodePoint(codePoint);
        return result == null
            ? Integer.toString(codePoint, 16).toUpperCase(Locale.ROOT)
                : result.toString();
    }
}