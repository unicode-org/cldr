package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.Locale;
import java.util.Set;

/**
 * Provide a set of code point abbreviations. Includes conversions to and from codepoints, including
 * hex.
 */
public enum CodePointEscaper {
    // These are characters found in CLDR data fields
    // The long names don't necessarily match the formal Unicode names
    TAB(9, "tab"),
    LF(0xA, "line feed"),
    CR(0xD, "carriage return"),
    SP(0x20, "space", "ASCII space"),
    NBSP(0xA0, "no-break space"),

    NSP(0x2009, "narrow space", "thin space"),
    NNBSP(0x202F, "narrow no-break space", "thin no-break space"),

    ZWNJ(0x200C, "zero-width non-joiner"),
    ZWJ(0x200D, "zero-width joiner"),

    ZWSP(0x200B, "word non-joiner", "zero-width space"),
    ZWNBSP(0x2060, "word joiner", "zero-width no-break space"),

    ALM(0x061C, "Arabic letter mark"),
    LRM(0x200E, "left-right mark"),
    RLM(0x200F, "right-left mark"),

    LRO(0x202D, "left-right override"),
    RLO(0x202E, "right-left override"),
    PDF(0x202C, "end override"),

    SHY(0x00AD, "soft hyphen"),
    BOM(0xFEFF, "byte-order mark"),

    ANS(0x0600, "Arabic number sign"),
    ASNS(0x0601, "Arabic sanah sign"),
    AFM(0x602, "Arabic footnote marker"),
    ASFS(0x603, "Arabic safha sign"),
    SAM(0x70F, "Syriac abbreviation mark"),
    KIAQ(0x17B4, "Khmer inherent aq"),
    KIAA(0x17B5, "Khmer inherent aa"),

    RANGE('➖', "range syntax mark", "heavy minus sign"),
    ESCS('⦕', "escape start", "double open paren angle"),
    ESCE('⦖', "escape end", "double close paren angle");

    public static final char RANGE_SYNTAX = (char) RANGE.getCodePoint();
    public static final char ESCAPE_START = (char) ESCS.getCodePoint();
    public static final char ESCAPE_END = (char) ESCE.getCodePoint();

    /** Assemble the reverse mapping */
    private static final UnicodeMap<CodePointEscaper> _fromCodePoint = new UnicodeMap<>();

    static {
        for (CodePointEscaper abbr : CodePointEscaper.values()) {
            CodePointEscaper oldValue = _fromCodePoint.get(abbr.codePoint);
            if (oldValue != null) {
                throw new IllegalArgumentException(
                        "Abbreviation code points collide: "
                                + oldValue.name()
                                + ", "
                                + abbr.name());
            }
            _fromCodePoint.put(abbr.codePoint, abbr);
        }
        _fromCodePoint.freeze();
    }

    /** Characters that need escaping */
    public static final UnicodeSet EMOJI_INVISIBLES =
            new UnicodeSet("[\\uFE0F\\U000E0020-\\U000E007F]").freeze();

    public static final UnicodeSet FORCE_ESCAPE =
            new UnicodeSet("[[:DI:][:Pat_WS:][:WSpace:][:C:][:Z:]]")
                    .addAll(getNamedEscapes())
                    .removeAll(EMOJI_INVISIBLES)
                    .freeze();

    public static final UnicodeSet NON_SPACING = new UnicodeSet("[[:Mn:][:Me:]]").freeze();

    public static final UnicodeSet FORCE_ESCAPE_WITH_NONSPACING =
            new UnicodeSet(FORCE_ESCAPE).addAll(NON_SPACING).freeze();

    private final int codePoint;
    private final Set<String> longNames;

    private CodePointEscaper(int codePoint, String... longNames) {
        this.codePoint = codePoint;
        this.longNames = ImmutableSet.copyOf(longNames);
    }

    public static final UnicodeSet getNamedEscapes() {
        return _fromCodePoint.keySet().freeze();
    }

    /**
     * Return long names for this character. The set is immutable and ordered, with the first name
     * being the most user-friendly.
     */
    public Set<String> getLongNames() {
        return longNames;
    }
    /** Return the code point for this character. */
    public int getCodePoint() {
        return codePoint;
    }

    /**
     * Return an Abbreviation for a string (or CharSequence), or null if not found. Handles lower
     * and uppercase input.
     */
    public static CodePointEscaper fromString(CharSequence source) {
        try {
            return valueOf(source.toString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns an Abbreviation for a codePoint, or null if not found. */
    public static CodePointEscaper fromCodePoint(int codePoint) {
        return _fromCodePoint.get(codePoint);
    }

    /** Returns a codepoint from an abbreviation string or hex string. */
    public static int fromAbbreviationOrHex(CharSequence value) {
        CodePointEscaper abbreviation = CodePointEscaper.fromString(value.toString());
        if (abbreviation != null) {
            return abbreviation.codePoint;
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(value.toString(), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a named or hex escape: ⦕" + value + "❌⦖");
        }
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Illegal code point: ⦕" + value + "❌⦖");
        }
        return codePoint;
    }

    /** Returns an abbreviation string or hex string from a code point. */
    public static String toAbbreviationOrHex(int codePoint) {
        CodePointEscaper result = CodePointEscaper.fromCodePoint(codePoint);
        return result == null
                ? Integer.toString(codePoint, 16).toUpperCase(Locale.ROOT)
                : result.toString();
    }
}
