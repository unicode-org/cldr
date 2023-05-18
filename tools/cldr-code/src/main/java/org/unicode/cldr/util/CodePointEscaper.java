package org.unicode.cldr.util;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import java.util.Locale;

/**
 * Provide a set of code point abbreviations. Includes conversions to and from codepoints, including
 * hex. Typicaly To test whether a string could have escapes, use either:
 *
 * <ul>
 *   <li>
 */
public enum CodePointEscaper {
    // These are characters found in CLDR data fields
    // The long names don't necessarily match the formal Unicode names
    TAB(9, "tab"),
    LF(0xA, "line feed"),
    CR(0xD, "carriage return"),
    SP(0x20, "space", "ASCII space"),
    NSP(0x2009, "narrow/thin space", "Also known as ‘thin space’"),
    NBSP(0xA0, "no-break space", "Same as space, but doesn’t line wrap."),

    NNBSP(0x202F, "narrow/thin no-break space", "Same as narrow space, but doesn’t line wrap."),

    WNJ(
            0x200B,
            "allow line wrap after, aka ZWSP",
            "Invisible character allowing a line-wrap afterwards. Also known as ‘ZWSP’."),
    WJ(
            0x2060,
            "prevent line wrap",
            "Keeps adjacent characters from line-wrapping. Also known as ‘word-joiner’."),
    SHY(
            0x00AD,
            "soft hyphen",
            "Invisible character allowing a line-wrap afterwards, but appears like a hyphen in most languages."),

    ZWNJ(0x200C, "cursive non-joiner", "Breaks cursive connections, where possible."),
    ZWJ(0x200D, "cursive joiner", "Forces cursive connections, if possible."),

    ALM(
            0x061C,
            "Arabic letter mark",
            "For BIDI, invisible character that behaves like Arabic letter."),
    LRM(
            0x200E,
            "left-right mark",
            "For BIDI, invisible character that behaves like Hebrew letter."),
    RLM(0x200F, "right-left mark", "For BIDI, invisible character that behaves like Latin letter."),

    LRO(0x202D, "left-right override"),
    RLO(0x202E, "right-left override"),
    PDF(0x202C, "end override"),

    BOM(0xFEFF, "byte-order mark"),

    ANS(0x0600, "Arabic number sign"),
    ASNS(0x0601, "Arabic sanah sign"),
    AFM(0x602, "Arabic footnote marker"),
    ASFS(0x603, "Arabic safha sign"),
    SAM(0x70F, "Syriac abbreviation mark"),
    KIAQ(0x17B4, "Khmer inherent aq"),
    KIAA(0x17B5, "Khmer inherent aa"),

    RANGE('➖', "range syntax mark", "heavy minus sign"),
    ESCS('❰', "escape start", "heavy open angle bracket"),
    ESCE('❱', "escape end", "heavy close angle bracket");

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
    private final String shortName;
    private final String description;

    private CodePointEscaper(int codePoint, String shortName) {
        this.codePoint = codePoint;
        this.shortName = shortName;
        this.description = "";
    }

    private CodePointEscaper(int codePoint, String shortName, String description) {
        this.codePoint = codePoint;
        this.shortName = shortName;
        this.description = description;
    }

    public static final UnicodeSet getNamedEscapes() {
        return _fromCodePoint.keySet().freeze();
    }

    /**
     * Return long names for this character. The set is immutable and ordered, with the first name
     * being the most user-friendly.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Return a longer description, if available; otherwise ""
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /** Return the code point for this character. */
    public int getCodePoint() {
        return codePoint;
    }

    /** Returns the escaped form from the code point for this enum */
    public String codePointToEscaped() {
        return ESCAPE_START + rawCodePointToEscaped(codePoint) + ESCAPE_END;
    }

    /** Returns a code point from the escaped form <b>of a single code point</b> */
    public static int escapedToCodePoint(String value) {
        if (value.codePointAt(0) != CodePointEscaper.ESCAPE_START
                || value.codePointAt(value.length() - 1) != CodePointEscaper.ESCAPE_END) {
            throw new IllegalArgumentException(
                    "Must be of the form "
                            + CodePointEscaper.ESCAPE_START
                            + "…"
                            + CodePointEscaper.ESCAPE_END);
        }
        return rawEscapedToCodePoint(value.substring(1, value.length() - 1));
    }

    /** Returns the escaped form from a code point */
    public static String codePointToEscaped(int codePoint) {
        return ESCAPE_START + rawCodePointToEscaped(codePoint) + ESCAPE_END;
    }

    /** Returns the escaped form from a string */
    public static String toEscaped(String unescaped) {
        return toEscaped(unescaped, FORCE_ESCAPE);
    }

    /** Returns the escaped form from a string */
    public static String toEscaped(String unescaped, UnicodeSet toEscape) {
        StringBuilder result = new StringBuilder();
        unescaped
                .codePoints()
                .forEach(
                        cp -> {
                            if (!toEscape.contains(cp)) {
                                result.appendCodePoint(cp);
                            } else {
                                result.append(codePointToEscaped(cp));
                            }
                        });
        return result.toString();
    }
    /** Return unescaped string */
    public static String toUnescaped(String value) {
        StringBuilder result = null;
        int donePart = 0;
        int found = value.indexOf(ESCAPE_START);
        while (found >= 0) {
            int foundEnd = value.indexOf(ESCAPE_END, found);
            if (foundEnd < 0) {
                throw new IllegalArgumentException(
                        "Malformed escaped string, missing: " + ESCAPE_END);
            }
            if (result == null) {
                result = new StringBuilder();
            }
            result.append(value, donePart, found);
            donePart = ++foundEnd;
            result.appendCodePoint(escapedToCodePoint(value.substring(found, foundEnd)));
            found = value.indexOf(ESCAPE_START, foundEnd);
        }
        return donePart == 0 ? value : result.append(value, donePart, value.length()).toString();
    }

    public static String toExample(int codePoint) {
        CodePointEscaper cpe = _fromCodePoint.get(codePoint);
        if (cpe == null) { // hex
            return codePointToEscaped(codePoint)
                    + " "
                    + UCharacter.getName(codePoint).toLowerCase();
        } else {
            return CodePointEscaper.codePointToEscaped(cpe.codePoint)
                    + " "
                    + cpe.shortName; // TODO show hover with cpe.description
        }
    }

    /**
     * Returns a code point from an abbreviation string or hex string <b>without the escape
     * brackets</b>
     */
    public static int rawEscapedToCodePoint(CharSequence value) {
        try {
            return valueOf(value.toString().toUpperCase(Locale.ROOT)).codePoint;
        } catch (Exception e) {
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(value.toString(), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a named or hex escape: ❰" + value + "❌❱");
        }
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Illegal code point: ❰" + value + "❌❱");
        }
        return codePoint;
    }

    /**
     * Returns an abbreviation string or hex string <b>without the escape brackets</b> from a code
     * point.
     */
    public static String rawCodePointToEscaped(int codePoint) {
        CodePointEscaper result = CodePointEscaper._fromCodePoint.get(codePoint);
        return result == null
                ? Integer.toString(codePoint, 16).toUpperCase(Locale.ROOT)
                : result.toString();
    }
}
