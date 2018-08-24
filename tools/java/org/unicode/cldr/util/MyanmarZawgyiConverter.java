/* Copyright (C) 2007-2013 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;

/**
 * Class for detecting and converting Zawgyi-encoded data.
 */

public class MyanmarZawgyiConverter {
    // For detecting if Myanmar text is encoded with Zawgyi vs. Unicode characters.

    private static final Pattern ZAWGYI_DETECT_PATTERN = PatternCache.get(
        // A regular expression matched if text is Zawgyi encoding.
        // Using the ranges 1033-1034 or 1060-1097 will report Shan, Karen,
        // etc. as Zawgyi.
        "[\u105a\u1060-\u1097]|" // Zawgyi characters outside Unicode range
            + "[\u1033\u1034]|" // These are Mon characters
            + "\u1031\u108f|"
            + "\u1031[\u103b-\u103e]|" // Medial right after \u1031
            + "[\u102b-\u1030\u1032]\u1031|" // Vowel sign right after before \u1031
            + " \u1031| \u103b|" // Unexpected characters after a space
            + "^\u1031|^\u103b|\u1038\u103b|\u1038\u1031|"
            + "[\u102d\u102e\u1032]\u103b|\u1039[^\u1000-\u1021]|\u1039$"
            + "|\u1004\u1039[\u1001-\u102a\u103f\u104e]" // Missing ASAT in Kinzi
            + "|\u1039[^u1000-\u102a\u103f\u104e]" // 1039 not before a consonant
            // Out of order medials
            + "|\u103c\u103b|\u103d\u103b"
            + "|\u103e\u103b|\u103d\u103c"
            + "|\u103e\u103c|\u103e\u103d"
            // Bad medial combos
            + "|\u103b\u103c"
            // Out of order vowel signs
            + "|[\u102f\u1030\u102b\u102c][\u102d\u102e\u1032]"
            + "|[\u102b\u102c][\u102f\u102c]"
            // Digit before diacritic
            + "|[\u1040-\u1049][\u102b-\u103e\u102b-\u1030\u1032\u1036\u1037\u1038\u103a]"
            // Single digit 0, 7 at start
            + "|^[\u1040\u1047][^\u1040-\u1049]"
            // Second 1039 with bad followers
            + "|[\u1000-\u102a\u103f\u104e]\u1039[\u101a\u101b\u101d\u101f\u1022-\u103f]"
            // Other bad combos.
            + "|\u103a\u103e"
            + "|\u1036\u102b]"
            // multiple upper vowels
            + "|\u102d[\u102e\u1032]|\u102e[\u102d\u1032]|\u1032[\u102d\u102e]"
            // Multiple lower vowels
            + "|\u102f\u1030|\u1030\u102f"
            // Multiple A vowels
            + "|\u102b\u102c|\u102c\u102b"
            // Shan digits with vowels or medials or other signs
            + "|[\u1090-\u1099][\u102b-\u1030\u1032\u1037\u103a-\u103e]"
            // Isolated Shan digit
            + "|[\u1000-\u10f4][\u1090-\u1099][\u1000-\u104f]"
            + "|^[\u1090-\u1099][\u1000-\u102a\u103f\u104e\u104a\u104b]"
            + "|[\u1000-\u104f][\u1090-\u1099]$"
            // Diacritics with non-Burmese vowel signs
            + "|[\u105e-\u1060\u1062-\u1064\u1067-\u106d\u1071-\u1074\u1082-\u108d"
            + "\u108f\u109a-\u109d]"
            + "[\u102b-\u103e]"
            // Consonant 103a + some vowel signs
            + "|[\u1000-\u102a]\u103a[\u102d\u102e\u1032]"
            // 1031 after other vowel signs
            + "|[\u102b-\u1030\u1032\u1036-\u1038\u103a]\u1031"
            // Using Shan combining characters with other languages.
            + "|[\u1087-\u108d][\u106e-\u1070\u1072-\u1074]"
            // Non-Burmese diacritics at start, following space, or following sections
            + "|^[\u105e-\u1060\u1062-\u1064\u1067-\u106d\u1071-\u1074"
            + "\u1082-\u108d\u108f\u109a-\u109d]"
            + "|[\u0020\u104a\u104b][\u105e-\u1060\u1062-\u1064\u1067-\u106d"
            + "\u1071-\u1074\u1082-\u108d\u108f\u109a-\u109d]"
            // Wrong order with 1036
            + "|[\u1036\u103a][\u102d-\u1030\u1032]"
            // Odd stacking
            + "|[\u1025\u100a]\u1039"
            // More mixing of non-Burmese languages
            + "|[\u108e-\u108f][\u1050-\u108d]"
            // Bad diacritic combos.
            + "|\u102d-\u1030\u1032\u1036-\u1037]\u1039]"
            // Dot before subscripted consonant
            + "|[\u1000-\u102a\u103f\u104e]\u1037\u1039"
            // Odd subscript + vowel signs
            + "|[\u1000-\u102a\u103f\u104e]\u102c\u1039[\u1000-\u102a\u103f\u104e]"
            // Medials after vowels
            + "|[\u102b-\u1030\u1032][\u103b-\u103e]"
            // Medials
            + "|\u1032[\u103b-\u103e]"
            // Medial with 101b
            + "|\u101b\u103c"
            // Stacking too deeply: consonant 1039 consonant 1039 consonant
            + "|[\u1000-\u102a\u103f\u104e]\u1039[\u1000-\u102a\u103f\u104e]\u1039"
            + "[\u1000-\u102a\u103f\u104e]"
            // Stacking pattern consonant 1039 consonant 103a other vowel signs
            + "|[\u1000-\u102a\u103f\u104e]\u1039[\u1000-\u102a\u103f\u104e]"
            + "[\u102b\u1032\u103d]"
            // Odd stacking over u1021, u1019, and u1000
            + "|[\u1000\u1005\u100f\u1010\u1012\u1014\u1015\u1019\u101a]\u1039\u1021"
            + "|[\u1000\u1010]\u1039\u1019"
            + "|\u1004\u1039\u1000"
            + "|\u1015\u1039[\u101a\u101e]"
            + "|\u1000\u1039\u1001\u1036"
            + "|\u1039\u1011\u1032"
            // Vowel sign in wrong order
            + "|\u1037\u1032"
            + "|\u1036\u103b"
            // Duplicated vowel
            + "|\u102f\u102f");

    // Transliteration to convert Burmese text in Zawgyi-encoded string to
    // standard Unicode codepoints and ordering.
    static Transliterator zawgyiUnicodeTransliterator =
        Transliterator.getInstance("Zawgyi-my");

    /**
     * Detects Zawgyi encoding in specified input.
     *
     * @param value
     *          the string to be tested
     * @return
     *          True if text is Zawgyi encoded. False if Unicode
     */
    public static Boolean isZawgyiEncoded(String value) {
        Matcher matcher = ZAWGYI_DETECT_PATTERN.matcher(value);
        return matcher.find();
    }

    /**
     * Converts Zawgyi-encoded string into Unicode equivalent.
     *
     * @param value
     *          the Zawgyi string to be converted
     * @return
     *          the Unicode string from converstion
     */
    public static String convertZawgyiToUnicode(String value) {
        return zawgyiUnicodeTransliterator.transform(value);
    }

    /**
     * Normalizes Burmese characters in specified input, detecting and converting
     * Zawgyi encoding to Unicode form.
     *
     * @param value
     *          the string to be normalized
     * @return
     *          the normalized Unicode string
     */
    public static String standardizeMyanmar(String value) {
        if (isZawgyiEncoded(value)) {
            // Call the converter to produce a Unicode result.
            return zawgyiUnicodeTransliterator.transform(value);
        }
        return value; // Unchanged since it was not Zawgyi.
    }
}
