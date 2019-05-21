package org.unicode.cldr.util;

import java.util.Locale;

import org.unicode.cldr.util.XListFormatter.ListTypeLength;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class EmojiConstants {
    public static final String EMOJI_VARIANT_STRING = "\uFE0F";
    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;
    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(FIRST_REGIONAL, LAST_REGIONAL).freeze();
    public static final String KEYCAP_MARK_STRING = "\u20E3";
    public static final UnicodeSet MODIFIERS = new UnicodeSet(0x1F3FB, 0x1F3FF).freeze();
    public static final UnicodeSet HAIR = new UnicodeSet(0x1F9B0, 0x1F9B3).freeze();
    public static final char JOINER = '\u200D';
    public static final String JOINER_STRING = String.valueOf(JOINER);
    public static final UnicodeSet COMPONENTS = new UnicodeSet(EmojiConstants.MODIFIERS)
        .add(EmojiConstants.fromCodePoints(JOINER,0x1F9B0))
        .add(EmojiConstants.fromCodePoints(JOINER,0x1F9B1))
        .add(EmojiConstants.fromCodePoints(JOINER,0x1F9B2))
        .add(EmojiConstants.fromCodePoints(JOINER,0x1F9B3)) 
        .freeze();

    public static final String KISS = "💋";
    public static final String HEART = "❤";
    public static final String TAG_TERM = UTF16.valueOf(0xE007F);
    public static final String BLACK_FLAG = UTF16.valueOf(0x1F3F4);
    public static final String HANDSHAKE = UTF16.valueOf(0x1f91d);
    public static final String MALE_SIGN = "♂";
    public static final String FEMALE_SIGN = "♀";
    public static final String MAN = "👨";
    public static final String WOMAN = "👩";
    public static final String JOINER_MALE_SIGN = JOINER_STRING + MALE_SIGN;
    public static final String JOINER_FEMALE_SIGN = JOINER_STRING + FEMALE_SIGN;
    public static final UnicodeSet HAIR_EXPLICIT = new UnicodeSet("[🧔 👱]").freeze();
    
    public static final ListTypeLength COMPOSED_NAME_LIST = ListTypeLength.UNIT_SHORT;

    //public static final UnicodeSet MODIFIERS_GENDER_SIGNS = new UnicodeSet(0x1F3FB, 0x1F3FF).add(MALE_SIGN).add(FEMALE_SIGN).freeze();
    public static String getFlagCode(String s) {
        return String.valueOf((char) (s.codePointAt(0) - FIRST_REGIONAL + 'A')) + (char) (s.codePointAt(2) - FIRST_REGIONAL + 'A');
    }
    
    public static String getEmojiFromRegionCodes(String chars) {
        return new StringBuilder()
                .appendCodePoint(chars.codePointAt(0) + FIRST_REGIONAL - 'A')
                .appendCodePoint(chars.codePointAt(1) + FIRST_REGIONAL - 'A')
                .toString();
    }
    
    public static final int TAG_BASE = 0xE0000;
    public static final int TAG_TERM_CHAR = 0xE007F;

    public static String getEmojiFromSubdivisionCodes(String string) {
        string = string.toLowerCase(Locale.ROOT).replace("-","");
        StringBuilder result = new StringBuilder().appendCodePoint(0x1F3F4);
        for (int cp : CharSequences.codePoints(string)) {
            result.appendCodePoint(TAG_BASE + cp);
        }
        return result.appendCodePoint(TAG_TERM_CHAR).toString();
    }

    public static final UnicodeSet FAMILY_MARKERS = new UnicodeSet()
        .add(0x1F466, 0x1F469).add(0x1F476)
        .add(JOINER_STRING)
        .freeze(); // boy, girl, man, woman, baby
    public static final UnicodeSet REM_SKIP_SET = new UnicodeSet()
        .add(JOINER_STRING)
        .freeze();
    public static final UnicodeSet REM_GROUP_SKIP_SET = new UnicodeSet(REM_SKIP_SET)
        .add(EmojiConstants.HEART)
        .add(EmojiConstants.KISS)
        .add(EmojiConstants.HANDSHAKE)
        .add(MALE_SIGN)
        .add(FEMALE_SIGN)
        .freeze();

    public static String getTagSpec(String code) {
        StringBuilder b = new StringBuilder();
        for (int codePoint : CharSequences.codePoints(code)) {
            if (codePoint >= 0xE0020 && codePoint <= 0xE007E) {
                b.appendCodePoint(codePoint - 0xE0000);
            }
        }
        return b.toString();
    }

    // U+1F3F4 U+E0067 U+E0062 U+E0065 U+E006E U+E0067 U+E007F
    public static String toTagSeq(String subdivisionCode) {
        StringBuilder b = new StringBuilder().appendCodePoint(0x1F3F4);
        for (int i = 0; i < subdivisionCode.length(); ++i) {
            b.appendCodePoint(subdivisionCode.charAt(i) + 0xE0000);
        }
        return b.appendCodePoint(0xE007F).toString();
    }

    public static CharSequence fromCodePoints(int... codePoints) {
        return EmojiConstants.appendCodePoints(new StringBuilder(), codePoints).toString();
    }

    public static StringBuilder appendCodePoints(StringBuilder b, int... codePoints) {
        for (int i = 0; i < codePoints.length; ++i) {
            b.appendCodePoint(codePoints[i]);
        }
        return b;
    }
}