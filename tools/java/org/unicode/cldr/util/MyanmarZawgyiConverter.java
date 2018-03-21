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
    static final Transform<String, String> zawgyiUnicodeTransliterator =
        // Transliteration rules, 07-Jan-2014.
        Transliterator.createFromRules("zawgyi-unicode",
            // Modern Burmese digits & Unicode code points.
            "$nondigits = [^\u1040-\u1049];"
                + "$space = ' ';"
                + "$consonant = [\u1000-\u1021];"
                + "$vowelsign = [\u102B-\u1030\u1032];"
                + "$umedial = [\u103B-\u103E];"
                + "$vowelmedial = [\u102B-\u1030\u1032\u103B-\u103F];"
                + "$ukinzi = \u1004\u103A\u1039;"
                + "$zmedialra = [\u103B\u107E-\u1084];"
                // #### STAGE (1): CODEPOINT MAPPING FROM ZAWGYI TO UNICODE
                + "($consonant) \u103A \u1064 > $ukinzi $1 \u103B;"
                + "($consonant) \u1064 > $ukinzi $1;"
                + "\u1064 > $ukinzi;"
                + "($consonant) \u108b > $ukinzi $1 \u102D;"
                + "($consonant) \u108C > $ukinzi $1 \u102E;"
                + "($consonant) \u108D > $ukinzi $1 \u1036;"
                + "($consonant) \u103A \u1033 \u108B > $ukinzi $1 \u103B \u102D \u102F;"
                + "($consonant) \u103A \u108b > $ukinzi $1 \u103B \u102D ;"
                + "($consonant) \u103A \u108C \u1033 > $ukinzi $1 \u103B \u102E \u102F;"
                + "($consonant) \u103A \u108C > $ukinzi $1 \u103B \u102E ;"
                + "($consonant) \u103A \u108D > $ukinzi $1 \u103B \u1036 ;"
                + "($consonant) \u103A \u108e > $1 \u103B \u102D \u1036 ;"
                + "\u108B > $ukinzi \u102D ;"
                + "\u108C > $ukinzi \u102E ;"
                + "\u108D > $ukinzi \u1036 ;"
                + "\u106A ($vowelsign) \u1038 > \u1025 $1 \u1038 ;"
                + "\u106A > \u1009 ;"
                + "\u106B > \u100A ;"
                + "\u108F > \u1014 ;"
                + "\u1090 > \u101B ;"
                + "\u1086 > \u103F ;"
                + "\u103A > \u103B ;"
                + "\u107D > \u103B ;"
                + "\u103C \u108A > \u103D \u103E;"
                + "\u103C > \u103D ;"
                + "\u108A > \u103D \u103E ;"
                + "\u103D > \u103E ;"
                + "\u1087 > \u103E ;"
                + "\u1088 > \u103E \u102F ;"
                + "\u1089 > \u103E \u1030 ;"
                + "\u1039 > \u103A ;"
                + "\u1033 > \u102F ;"
                + "\u1034 > \u1030 ;"
                + "\u105A > \u102B \u103A ;"
                + "\u108E > \u102D \u1036 ;"
                + "\u1031 \u1094 ($consonant) \u103D > $1 \u103E \u1031 \u1037 ;"
                + "\u1094 > \u1037 ;"
                + "\u1095 > \u1037 ;"
                + "\u1025 \u1061 > \u1009 \u1039 \u1001;"
                + "\u1025 \u1062 > \u1009 \u1039 \u1002;"
                + "\u1025 \u1065 > \u1009 \u1039 \u1005;"
                + "\u1025 \u1068 > \u1009 \u1039 \u1007;"
                + "\u1025 \u1076 > \u1009 \u1039 \u1013;"
                + "\u1025 \u1078 > \u1009 \u1039 \u1015;"
                + "\u1025 \u107A > \u1009 \u1039 \u1017;"
                + "\u1025 \u1079 > \u1009 \u1039 \u1016;"
                + "\u1060 > \u1039 \u1000 ;"
                + "\u1061 > \u1039 \u1001 ;"
                + "\u1062 > \u1039 \u1002 ;"
                + "\u1063 > \u1039 \u1003 ;"
                + "\u1065 > \u1039 \u1005 ;"
                + "\u1066 > \u1039 \u1006 ;"
                + "\u1067 > \u1039 \u1006 ;"
                + "\u1068 > \u1039 \u1007 ;"
                + "\u1069 > \u1039 \u1008 ;"
                + "\u106C > \u1039 \u100B ;"
                + "\u106D > \u1039 \u100C ;"
                + "\u1070 > \u1039 \u100F ;"
                + "\u1071 > \u1039 \u1010 ;"
                + "\u1072 > \u1039 \u1010 ;"
                + "\u1096 > \u1039 \u1010 \u103D;"
                + "\u1073 > \u1039 \u1011 ;"
                + "\u1074 > \u1039 \u1011 ;"
                + "\u1075 > \u1039 \u1012 ;"
                + "\u1076 > \u1039 \u1013 ;"
                + "\u1077 > \u1039 \u1014 ;"
                + "\u1078 > \u1039 \u1015 ;"
                + "\u1079 > \u1039 \u1016 ;"
                + "\u107A > \u1039 \u1017 ;"
                + "\u107B > \u1039 \u1018 ;"
                + "\u1093 > \u1039 \u1018 ;"
                + "\u107C > \u1039 \u1019 ;"
                + "\u1085 > \u1039 \u101C ;"
                + "\u106E > \u100D\u1039\u100D ;"
                + "\u106F > \u100D\u1039\u100E ;"
                + "\u1091 > \u100F\u1039\u100D ;"
                + "\u1092 > \u100B\u1039\u100C ;"
                + "\u1097 > \u100B\u1039\u100B ;"
                + "\u104E > \u104E\u1004\u103A\u1038 ;"
                //#### STAGE (2): POST REORDERING RULES FOR UNICODE RENDERING
                + "::Null;"
                + "\u1044 \u103a > | \u104E \u103A ;"
                + "($nondigits) \u1040 ([\u102B-\u103F]) > $1 \u101D $2;"
                + "\u1031 \u1040 ($nondigits) > \u1031 \u101D $1;"
                + "\u1025 \u103A > \u1009 \u103A;"
                + "\u1025 \u102E > \u1026;"
                + "\u1037\u103A > \u103A\u1037;"
                + "\u1036 ($umedial*) ($vowelsign+) > $1 $2 \u1036 ;"
                + "([\u102B\u102C\u102F\u1030]) ([\u102D\u102E\u1032]) > $2 $1;"
                + "\u103C ($consonant) > $1 \u103C;"

                //#### Stage 3
                + "::Null;"
                + "([\u1031]+) $ukinzi ($consonant) > $ukinzi $2 $1;"
                + "([\u1031]+) ($consonant) ($umedial+) > $2 $3 $1;"
                + "([\u1031]+) ($consonant) } [^\u103B\u103C\u103D\u103E] > $2 $1;"
                + "\u103C \u103A \u1039 ($consonant) > \u103A \u1039 $1 \u103C;"
                + "\u1036 ($umedial+) > $1 \u1036;"
                // #### Stage 4
                + "::Null;"
                + "([\u103C\u103D\u103E]+) \u103B > \u103B $1;"
                + "([\u103D\u103E]+) \u103C > \u103C $1;"
                + "\u103E\u103D > \u103D\u103E ;"
                + "([\u1031]+) ($vowelsign*) \u1039 ($consonant) > \u1039 $3 $1 $2;"
                + "($vowelsign+) \u1039 ($consonant) > \u1039 $2 $1;"
                + "($umedial*) ([\u1031]+) ($umedial*) > $1 $3 $2;"
                + "\u1037 ([\u102D-\u1030\u1032\u1036]) > $1 \u1037;"
                + "\u1037 ($umedial+) > $1 \u1037;"
                + "($vowelsign+) ($umedial+) > $2 $1;"
                + "($consonant) ([\u102B-\u1032\u1036\u103B-\u103E]) \u103A ($consonant)> $1 \u103A $2 $3;"
                // #### Stage 5.  More reorderings
                + "::Null;"
                + "([\u1031]+) ($umedial+) > $2 $1;"
                + "($vowelsign) ($umedial) > $2 $1;"
                + "([\u103C\u103D\u103E]) \u103B > \u103B $1;"
                + "([\u103D\u103E]) \u103C > \u103C $1;"
                + "\u103E\u103D > \u103D\u103E ;"
                + "\u1038 ([$vowelmedial]) > $1 \u1038;"
                + "\u1038 ([\u1036\u1037\u103A]) > $1 \u1038;"
                // ### Stage 6
                + "::Null;"
                + "($consonant) \u103B \u103A > $1 \u103A \u103B;"
                + "([\u103C\u103D\u103E]) \u103B > \u103B $1;"
                + "([\u103D\u103E]) \u103C > \u103C $1;"
                + "\u103E\u103D > \u103D\u103E ;"
                + "([\u102D-\u1030\u1032]) \u103A ($consonant) \u103A > $1 $2 \u103A;"
                + "\u102F \u103A > \u102F;"
                + "\u102D \u102E > \u102E;"
                + "\u102F \u1030 > \u102F;"
                + "\u102B [\u102B]+ > \u102B;"
                + "\u102C [\u102C]+ > \u102C;"
                + "\u102D [\u102D]+ > \u102D;"
                + "\u102E [\u102E]+ > \u102E;"
                + "\u102F [\u102F]+ > \u102F;"
                + "\u1030 [\u1030]+ > \u1030;"
                + "\u1031 [\u1031]+ > \u1031;"
                + "\u1032 [\u1032]+ > \u1032;"
                + "\u103A [\u103A]+ > \u103A;"
                + "\u103B [\u103B]+ > \u103B;"
                + "\u103C [\u103C]+ > \u103C;"
                + "\u103D [\u103D]+ > \u103D;"
                + "\u103E [\u103E]+ > \u103E;"
                // Try to correctly render diacritics after a space.
                + "$space([\u102e\u1037\u103a]) > \u00A0 $1 ;",
            Transliterator.FORWARD);

    // TODO(ccorn): set a filter on this to restrict to range \u1000-\u109f ???

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
