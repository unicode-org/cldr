/* Copyright (C) 2007-2013 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2013 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {

    private static final boolean FIX_YEARS = true;

    public static final boolean DEBUG_DAIP = CldrUtility.getProperty("DEBUG_DAIP", false);

    public static final UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]")
        .freeze();

    public static final UnicodeSet TO_QUOTE = (UnicodeSet) new UnicodeSet(
        "[[:Cn:]" +
            "[:Default_Ignorable_Code_Point:]" +
            "[:patternwhitespace:]" +
            "[:Me:][:Mn:]]" // add non-spacing marks
    ).freeze();

    public static final Pattern NUMBER_FORMAT_XPATH = Pattern
        .compile("//ldml/numbers/.*Format\\[@type=\"standard\"]/pattern.*");
    private static final Pattern APOSTROPHE_SKIP_PATHS = Pattern.compile("//ldml/("
        + "characters/.*|"
        + "delimiters/.*|"
        + "dates/.+/(pattern|intervalFormatItem|dateFormatItem).*|"
        + "units/.+/unitPattern.*|"
        + "numbers/symbols.*|"
        + "numbers/(decimal|currency|percent|scientific)Formats.+/(decimal|currency|percent|scientific)Format.*)");
    private static final Pattern NON_DECIMAL_PERIOD = Pattern.compile("(?<![0#'])\\.(?![0#'])");
    private static final Pattern WHITESPACE_NO_NBSP_TO_NORMALIZE = Pattern.compile("\\s+"); // string of whitespace not
    // including NBSP, i.e. [
    // \t\n\r]+
    private static final Pattern WHITESPACE_AND_NBSP_TO_NORMALIZE = Pattern.compile("[\\s\\u00A0]+"); // string of
    // whitespace
    // including NBSP,
    // i.e. [
    // \u00A0\t\n\r]+
    private static final UnicodeSet UNICODE_WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();

    private static final CLDRLocale MALAYALAM = CLDRLocale.getInstance("ml");
    private static final CLDRLocale ROMANIAN = CLDRLocale.getInstance("ro");
    private static final CLDRLocale CATALAN = CLDRLocale.getInstance("ca");
    private static final CLDRLocale NGOMBA = CLDRLocale.getInstance("jgo");
    private static final CLDRLocale KWASIO = CLDRLocale.getInstance("nmg");
    private static final CLDRLocale HEBREW = CLDRLocale.getInstance("he");
    private static final CLDRLocale MYANMAR = CLDRLocale.getInstance("my");
    private static final CLDRLocale GERMAN_SWITZERLAND = CLDRLocale.getInstance("de_CH");
    private static final CLDRLocale SWISS_GERMAN = CLDRLocale.getInstance("gsw");
    private static final List<String> LANGUAGES_USING_MODIFIER_APOSTROPHE = Arrays.asList("br","bss","gn","ha","lkt","mgo","moh","nnh","qu","quc","uk","uz");
    // Ş ş Ţ ţ  =>  Ș ș Ț ț
    private static final char[][] ROMANIAN_CONVERSIONS = {
        { '\u015E', '\u0218' }, { '\u015F', '\u0219' }, { '\u0162', '\u021A' },
        { '\u0163', '\u021B' } };

    private static final char[][] CATALAN_CONVERSIONS = {
        { '\u013F', '\u004C', '\u00B7' }, // Ŀ -> L·
        { '\u0140', '\u006C', '\u00B7' } }; // ŀ -> l·

    private static final char[][] NGOMBA_CONVERSIONS = {
        { '\u0251', '\u0061' }, { '\u0261', '\u0067' }, //  ɑ -> a , ɡ -> g , See ticket #5691
        { '\u2019', '\uA78C' }, { '\u02BC', '\uA78C' } }; //  Saltillo, see ticket #6805

    private static final char[][] KWASIO_CONVERSIONS = {
        { '\u0306', '\u030C' }, // See ticket #6571, use caron instead of breve
        { '\u0103', '\u01CE' }, { '\u0102', '\u01CD' }, // a-breve -> a-caron
        { '\u0115', '\u011B' }, { '\u011A', '\u01CD' }, // e-breve -> e-caron
        { '\u012D', '\u01D0' }, { '\u012C', '\u01CF' }, // i-breve -> i-caron
        { '\u014F', '\u01D2' }, { '\u014E', '\u01D1' }, // o-breve -> o-caron
        { '\u016D', '\u01D4' }, { '\u016C', '\u01D3' } // u-breve -> u-caron
    };

    private static final char[][] HEBREW_CONVERSIONS = {
        { '\'', '\u05F3' }, { '"', '\u05F4' } }; //  ' -> geresh  " -> gershayim

    // For detecting if Myanmar text is encoded with Zawgyi vs. Unicode characters.
    private static final Pattern ZAWGYI_DETECT_PATTERN = Pattern.compile(
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
            + "|\u102f\u102f"
        );

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
                + "$space([\u102e\u1037\u103a]) > \u00A0 $1 ;"
            , Transliterator.FORWARD);
    // TODO(ccorn): set a filter on this to restrict to range \u1000-\u109f ???

    private Collator col;

    private Collator spaceCol;

    private PrettyPrinter pp = null;

    final private CLDRLocale locale;
    private boolean isPosix;

    /**
     * Constructor, taking cldrFile.
     * 
     * @param cldrFileToCheck
     */
    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck, boolean needsCollator) {
        init(this.locale = CLDRLocale.getInstance(cldrFileToCheck.getLocaleID()), needsCollator);
    }

    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
        init(this.locale = CLDRLocale.getInstance(cldrFileToCheck.getLocaleID()), true);
    }

    void init(CLDRLocale locale, boolean needsCollator) {
        isPosix = locale.toString().indexOf("POSIX") >= 0;
        if (needsCollator) {
            ICUServiceBuilder isb = null;
            try {
                isb = ICUServiceBuilder.forLocale(locale);
            } catch (Exception e) {
            }

            if (isb != null) {
                try {
                    col = isb.getRuleBasedCollator();
                } catch (Exception e) {
                    col = Collator.getInstance(ULocale.ROOT);
                }
            } else {
                col = Collator.getInstance(ULocale.ROOT);
            }

            spaceCol = Collator.getInstance(locale.toULocale());
            if (spaceCol instanceof RuleBasedCollator) {
                ((RuleBasedCollator) spaceCol).setAlternateHandlingShifted(false);
            }
            pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT))
                .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .setToQuote(new UnicodeSet(TO_QUOTE))
                .setOrdering(col)
                .setSpaceComparator(spaceCol);

        }
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale, boolean needsCollator) {
        init(this.locale = CLDRLocale.getInstance(locale), needsCollator);
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale) {
        init(this.locale = CLDRLocale.getInstance(locale), true);
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(CLDRLocale locale, boolean needsCollator) {
        init(this.locale = locale, needsCollator);
    }

    /**
     * Constructor, taking locale.
     * 
     * @param locale
     */
    public DisplayAndInputProcessor(CLDRLocale locale) {
        init(this.locale = locale, true);
    }

    /**
     * Process the value for display. The result is a string for display in the
     * Survey tool or similar program.
     * 
     * @param path
     * @param value
     * @param fullPath
     * @return
     */
    public synchronized String processForDisplay(String path, String value) {
        value = Normalizer.compose(value, false); // Always normalize all text to NFC.
        if (path.contains("exemplarCharacters")) {
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }

            value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
            value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

            // if (RTL.containsSome(value) && value.startsWith("[") && value.endsWith("]")) {
            // return "\u200E[\u200E" + value.substring(1,value.length()-2) + "\u200E]\u200E";
            // }
        } else if (path.contains("stopword")) {
            return value.trim().isEmpty() ? "NONE" : value;
        } else {
            NumericType numericType = NumericType.getNumericType(path);
            if (numericType != NumericType.NOT_NUMERIC) {
                // Canonicalize existing values that aren't canonicalized yet.
                // New values will be canonicalized on input using processInput().
                try {
                    value = getCanonicalPattern(value, numericType, isPosix);
                } catch (IllegalArgumentException e) {
                    if (DEBUG_DAIP) System.err.println("Illegal pattern: " + value);
                }
                if (numericType != NumericType.CURRENCY) {
                    value = value.replace("'", "");
                }
            }
        }
        // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
        if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
             value = normalizeApostrophes(value);
        }
        return value;
    }

    static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();
    static final DateTimeCanonicalizer dtc = new DateTimeCanonicalizer(FIX_YEARS);

    /**
     * Process the value for input. The result is a cleaned-up value. For example,
     * an exemplar set is modified to be in the normal format, and any missing [ ]
     * are added (a common omission on entry). If there are any failures then the
     * original value is returned, so that the proper error message can be given.
     * 
     * @param path
     * @param value
     * @param internalException
     *            TODO
     * @param fullPath
     * @return
     */
    public synchronized String processInput(String path, String value, Exception[] internalException) {
        String original = value;
        value = Normalizer.compose(value, false); // Always normalize all input to NFC.
        if (internalException != null) {
            internalException[0] = null;
        }
        try {
            // Normalise Malayalam characters.
            if (locale.childOf(MALAYALAM)) {
                String newvalue = normalizeMalayalam(value);
                if (DEBUG_DAIP) System.out.println("DAIP: Normalized Malayalam '" + value + "' to '" + newvalue + "'");
                value = newvalue;
            } else if (locale.childOf(ROMANIAN) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeRomanian(value);
            } else if (locale.childOf(CATALAN) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeCatalan(value);
            } else if (locale.childOf(NGOMBA) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeNgomba(value);
            } else if (locale.childOf(KWASIO) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeKwasio(value);
            } else if (locale.childOf(HEBREW) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeHebrew(value);
            } else if ((locale.childOf(SWISS_GERMAN) || locale.childOf(GERMAN_SWITZERLAND)) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeSwissGerman(value);
            } else if (locale.childOf(MYANMAR) && !path.startsWith("//ldml/characters/exemplarCharacters")) {
                value = standardizeMyanmar(value);
            }

            if (UNICODE_WHITESPACE.containsSome(value)) {
                value = normalizeWhitespace(path, value);
            }

            // all of our values should not have leading or trailing spaces, except insertBetween
            if (!path.contains("/insertBetween")) {
                value = value.trim();
            }

            // fix grouping separator if space
            if (path.startsWith("//ldml/numbers/symbols") && !path.contains("/alias")) {
                if (value.isEmpty()) {
                    value = "\u00A0";
                }
                value = value.replace(' ', '\u00A0');
            }

            // fix date patterns
            DateTimePatternType datetimePatternType = DateTimePatternType.fromPath(path);
            if (DateTimePatternType.STOCK_AVAILABLE_INTERVAL_PATTERNS.contains(datetimePatternType)) {
                value = dtc.getCanonicalDatePattern(path, value, datetimePatternType);
            }

            if (path.startsWith("//ldml/numbers/currencies/currency") && path.contains("displayName")) {
                value = normalizeCurrencyDisplayName(value);
            }
            NumericType numericType = NumericType.getNumericType(path);
            if (numericType != NumericType.NOT_NUMERIC) {
                if (numericType == NumericType.CURRENCY) {
                    value = value.replaceAll(" ", "\u00A0");
                } else {
                    value = value.replaceAll("([%\u00A4]) ", "$1\u00A0")
                        .replaceAll(" ([%\u00A4])", "\u00A0$1");
                    value = replace(NON_DECIMAL_PERIOD, value, "'.'");
                    if (numericType == NumericType.DECIMAL_ABBREVIATED) {
                        value = value.replaceAll("0\\.0+", "0");
                    }
                }
                value = getCanonicalPattern(value, numericType, isPosix);
            }

            // fix [,]
            if (path.startsWith("//ldml/localeDisplayNames/languages/language")
                || path.startsWith("//ldml/localeDisplayNames/scripts/script")
                || path.startsWith("//ldml/localeDisplayNames/territories/territory")
                || path.startsWith("//ldml/localeDisplayNames/variants/variant")
                || path.startsWith("//ldml/localeDisplayNames/keys/key")
                || path.startsWith("//ldml/localeDisplayNames/types/type")) {
                value = value.replace('[', '(').replace(']', ')').replace('［', '（').replace('］', '）');
            }

            // Normalize two single quotes for the inches symbol.
            if (path.contains("/units")) {
                value = value.replace("''", "″");
            }

            // check specific cases
            if (path.contains("/exemplarCharacters")) {
                // clean up the user's input.
                // first, fix up the '['
                value = value.trim();

                // remove brackets and trim again before regex
                if (value.startsWith("[")) {
                    value = value.substring(1);
                }
                if (value.endsWith("]")) {
                    value = value.substring(0, value.length() - 1);
                }
                value = value.trim();

                value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
                value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

                // re-add brackets.
                value = "[" + value + "]";

                UnicodeSet exemplar = new UnicodeSet(value);
                XPathParts parts = new XPathParts().set(path);
                final String type = parts.getAttributeValue(-1, "type");
                ExemplarType exemplarType = type == null ? ExemplarType.main : ExemplarType.valueOf(type);
                value = getCleanedUnicodeSet(exemplar, pp, exemplarType);
            } else if (path.contains("stopword")) {
                if (value.equals("NONE")) {
                    value = "";
                }
            }

            // Normalize ellipsis data.
            if (path.startsWith("//ldml/characters/ellipsis")) {
                value = value.replace("...", "…");
            }

            // Replace Arabic presentation forms with their nominal counterparts
            value = replaceArabicPresentationForms(value);

            // Fix up any apostrophes as appropriate (Don't do so for things like date patterns...
            if (!APOSTROPHE_SKIP_PATHS.matcher(path).matches()) {
                 value = normalizeApostrophes(value);
            }
            return value;
        } catch (RuntimeException e) {
            if (internalException != null) {
                internalException[0] = e;
            }
            return original;
        }
    }

    private String normalizeWhitespace(String path, String value) {
        // turn all whitespace sequences (including tab and newline, and NBSP for certain paths)
        // into a single space or a single NBSP depending on path.
        if ((path.contains("/dateFormatLength") && path.contains("/pattern")) ||
            path.contains("/availableFormats/dateFormatItem") ||
            (path.startsWith("//ldml/dates/timeZoneNames/metazone") && path.contains("/long")) ||
            path.startsWith("//ldml/dates/timeZoneNames/regionFormat") ||
            path.startsWith("//ldml/localeDisplayNames/codePatterns/codePattern") ||
            path.startsWith("//ldml/localeDisplayNames/languages/language") ||
            path.startsWith("//ldml/localeDisplayNames/territories/territory") ||
            path.startsWith("//ldml/localeDisplayNames/types/type") ||
            (path.startsWith("//ldml/numbers/currencies/currency") && path.contains("/displayName")) ||
            (path.contains("/decimalFormatLength[@type=\"long\"]") && path.contains("/pattern")) ||
            path.startsWith("//ldml/posix/messages") ||
            (path.startsWith("//ldml/units/uni") && path.contains("/unitPattern "))) {
            value = WHITESPACE_AND_NBSP_TO_NORMALIZE.matcher(value).replaceAll(" "); // replace with regular space
        } else if ((path.contains("/currencies/currency") && (path.contains("/group") || path.contains("/pattern")))
            ||
            (path.contains("/currencyFormatLength") && path.contains("/pattern")) ||
            (path.contains("/currencySpacing") && path.contains("/insertBetween")) ||
            (path.contains("/decimalFormatLength") && path.contains("/pattern")) || // i.e. the non-long ones
            (path.contains("/percentFormatLength") && path.contains("/pattern")) ||
            (path.startsWith("//ldml/numbers/symbols") && (path.contains("/group") || path.contains("/nan")))) {
            value = WHITESPACE_AND_NBSP_TO_NORMALIZE.matcher(value).replaceAll("\u00A0"); // replace with NBSP
        } else {
            // in this case don't normalize away NBSP
            value = WHITESPACE_NO_NBSP_TO_NORMALIZE.matcher(value).replaceAll(" "); // replace with regular space
        }
        return value;
    }

    private String normalizeCurrencyDisplayName(String value) {
        StringBuilder result = new StringBuilder();
        boolean inParentheses = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                inParentheses = true;
            } else if (c == ')') {
                inParentheses = false;
            }
            if (inParentheses && c == '-') {
                c = 0x2013; /* Replace hyphen-minus with dash for date ranges */
            }
            result.append(c);
        }
        return result.toString();
    }

    private String normalizeApostrophes(String value) {
        // If our DAIP always had a CLDRFile to work with, then we could just check the exemplar set in it to see.
        // But since we don't, we just maintain the list internally and use it.
        if (LANGUAGES_USING_MODIFIER_APOSTROPHE.contains(locale.getLanguage())) {
            return value.replace('\'', '\u02bc');
        } else {
            char prev = 0;
            StringBuilder builder = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (c == '\'') {
                    if (Character.isLetter(prev)) {
                        builder.append('\u2019');
                    } else {
                        builder.append('\u2018');
                    }
                } else {
                    builder.append(c);
                }
                prev = c;
            }
            return builder.toString();
        }
    }
    private String standardizeRomanian(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : ROMANIAN_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeKwasio(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : KWASIO_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeNgomba(String value) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = value.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            boolean convertedSaltillo = false;
            for (char[] pair : NGOMBA_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    if (c == '\uA78C') {
                        convertedSaltillo = true;
                    }
                    break;
                }
            }
            if (convertedSaltillo &&
                ((i > 0 && i < charArray.length - 1 && Character.isUpperCase(charArray[i - 1]) && Character.isUpperCase(charArray[i + 1])) ||
                (i > 1 && Character.isUpperCase(charArray[i - 1]) && Character.isUpperCase(charArray[i - 2])))) {
                c = '\uA78B'; // UPPER CASE SALTILLO
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeHebrew(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            for (char[] pair : HEBREW_CONVERSIONS) {
                if (c == pair[0]) {
                    c = pair[1];
                    break;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String standardizeSwissGerman(String value) {       
        return value.replaceAll("\u00DF", "ss");
    }

    private String standardizeCatalan(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            boolean didSubstitute = false;
            for (char[] triple : CATALAN_CONVERSIONS) {
                if (c == triple[0]) {
                    builder.append(triple[1]);
                    builder.append(triple[2]);
                    didSubstitute = true;
                    break;
                }
            }
            if (!didSubstitute) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String replace(Pattern pattern, String value, String replacement) {
        String value2 = pattern.matcher(value).replaceAll(replacement);
        if (DEBUG_DAIP && !value.equals(value2)) {
            System.out.println("\n" + value + " => " + value2);
        }
        return value2;
    }

    private static Pattern UNNORMALIZED_MALAYALAM = Pattern.compile(
        "(\u0D23|\u0D28|\u0D30|\u0D32|\u0D33|\u0D15)\u0D4D\u200D");

    private static Map<Character, Character> NORMALIZING_MAP =
        Builder.with(new HashMap<Character, Character>())
            .put('\u0D23', '\u0D7A').put('\u0D28', '\u0D7B')
            .put('\u0D30', '\u0D7C').put('\u0D32', '\u0D7D')
            .put('\u0D33', '\u0D7E').put('\u0D15', '\u0D7F').get();

    /**
     * Normalizes the Malayalam characters in the specified input.
     * 
     * @param value
     *            the input to be normalized
     * @return
     */
    private String normalizeMalayalam(String value) {
        // Normalize Malayalam characters.
        Matcher matcher = UNNORMALIZED_MALAYALAM.matcher(value);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            int start = 0;
            do {
                buffer.append(value.substring(start, matcher.start(0)));
                char codePoint = matcher.group(1).charAt(0);
                buffer.append(NORMALIZING_MAP.get(codePoint));
                start = matcher.end(0);
            } while (matcher.find());
            buffer.append(value.substring(start));
            value = buffer.toString();
        }
        return value;
    }

    /**
     * Normalizes Burmese characters in specified input, detecting and converting
     * Zawgyi encoding to Unicode form.
     * 
     * @param value
     *          the string to be normalized
     * @return
     *          the normalized Unicode string.
     */
    private String standardizeMyanmar(String value) {
        Matcher matcher = ZAWGYI_DETECT_PATTERN.matcher(value);
        if (matcher.find()) {
            // Call the converter to produce a Unicode result.
            value = zawgyiUnicodeTransliterator.transform(value);
            // TODO(ccorn): check for exceptions?
            // Report bad data in the input?
        }
        return value;
    }

    static final Transform<String, String> fixArabicPresentation = Transliterator.getInstance(
        "[[:block=Arabic_Presentation_Forms_A:][:block=Arabic_Presentation_Forms_B:]] nfkc");

    /**
     * Normalizes the Arabic presentation forms characters in the specified input.
     * 
     * @param value
     *            the input to be normalized
     * @return
     */
    private String replaceArabicPresentationForms(String value) {
        value = fixArabicPresentation.transform(value);
        return value;
    }

    static Pattern REMOVE_QUOTE1 = Pattern.compile("(\\s)(\\\\[-\\}\\]\\&])()");
    static Pattern REMOVE_QUOTE2 = Pattern.compile("(\\\\[\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    static Pattern NEEDS_QUOTE1 = Pattern.compile("(\\s|$)([-\\}\\]\\&])()");
    static Pattern NEEDS_QUOTE2 = Pattern.compile("([^\\\\])([\\-\\{\\[\\&])(\\s)"); // ([^\\])([\\-\\{\\[])(\\s)

    public static String getCleanedUnicodeSet(UnicodeSet exemplar, PrettyPrinter prettyPrinter,
        ExemplarType exemplarType) {
        if (prettyPrinter == null) {
            return exemplar.toString();
        }
        String value;
        prettyPrinter.setCompressRanges(exemplar.size() > 300);
        value = exemplar.toPattern(false);
        UnicodeSet toAdd = new UnicodeSet();

        for (UnicodeSetIterator usi = new UnicodeSetIterator(exemplar); usi.next();) {
            String string = usi.getString();
            if (string.equals("ß") || string.equals("İ")) {
                toAdd.add(string);
                continue;
            }
            if (exemplarType.convertUppercase) {
                string = UCharacter.toLowerCase(ULocale.ENGLISH, string);
            }
            toAdd.add(string);
            String composed = Normalizer.compose(string, false);
            if (!string.equals(composed)) {
                toAdd.add(composed);
            }
        }

        toAdd.removeAll(exemplarType.toRemove);

        if (DEBUG_DAIP && !toAdd.equals(exemplar)) {
            UnicodeSet oldOnly = new UnicodeSet(exemplar).removeAll(toAdd);
            UnicodeSet newOnly = new UnicodeSet(toAdd).removeAll(exemplar);
            System.out.println("Exemplar:\t" + exemplarType + ",\tremoved\t" + oldOnly + ",\tadded\t" + newOnly);
        }

        String fixedExemplar = prettyPrinter.format(toAdd);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar);
        if (!toAdd.equals(doubleCheck)) {
            // something went wrong, leave as is
        } else if (!value.equals(fixedExemplar)) { // put in this condition just for debugging
            if (DEBUG_DAIP) {
                System.out.println(TestMetadata.showDifference(
                    With.codePoints(value),
                    With.codePoints(fixedExemplar),
                    "\n"));
            }
            value = fixedExemplar;
        }
        return value;
    }

    /**
     * @return a canonical numeric pattern, based on the type, and the isPOSIX flag. The latter is set for en_US_POSIX.
     */
    public static String getCanonicalPattern(String inpattern, NumericType type, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;
        DecimalFormat df = new DecimalFormat(inpattern);
        if (type == NumericType.DECIMAL_ABBREVIATED) {
            return inpattern; // TODO fix when ICU bug is fixed
            // df.setMaximumFractionDigits(df.getMinimumFractionDigits());
            // df.setMaximumIntegerDigits(Math.max(1, df.getMinimumIntegerDigits()));
        } else {
            // int decimals = type == CURRENCY_TYPE ? 2 : 1;
            int[] digits = isPOSIX ? type.posixDigitCount : type.digitCount;
            df.setMinimumIntegerDigits(digits[0]);
            df.setMinimumFractionDigits(digits[1]);
            df.setMaximumFractionDigits(digits[2]);
        }
        String pattern = df.toPattern();

        // int pos = pattern.indexOf(';');
        // if (pos < 0) return pattern + ";-" + pattern;
        return pattern;
    }

    /*
     * This tests what type a numeric pattern is.
     */
    public enum NumericType {
        CURRENCY(new int[] { 1, 2, 2 }, new int[] { 1, 2, 2 }),
        DECIMAL(new int[] { 1, 0, 3 }, new int[] { 1, 0, 6 }),
        DECIMAL_ABBREVIATED(),
        PERCENT(new int[] { 1, 0, 0 }, new int[] { 1, 0, 0 }),
        SCIENTIFIC(new int[] { 0, 0, 0 }, new int[] { 1, 6, 6 }),
        NOT_NUMERIC;

        private static final Pattern NUMBER_PATH = Pattern
            .compile("//ldml/numbers/((currency|decimal|percent|scientific)Formats|currencies/currency).*");
        private int[] digitCount;
        private int[] posixDigitCount;

        private NumericType() {
        };

        private NumericType(int[] digitCount, int[] posixDigitCount) {
            this.digitCount = digitCount;
            this.posixDigitCount = posixDigitCount;
        }

        /**
         * @return the numeric type of the xpath
         */
        public static NumericType getNumericType(String xpath) {
            Matcher matcher = NUMBER_PATH.matcher(xpath);
            if (xpath.indexOf("/pattern") < 0) {
                return NOT_NUMERIC;
            } else if (matcher.matches()) {
                if (matcher.group(1).equals("currencies/currency")) {
                    return CURRENCY;
                } else {
                    NumericType type = NumericType.valueOf(matcher.group(2).toUpperCase());
                    if (type == DECIMAL && xpath.contains("=\"1000")) {
                        type = DECIMAL_ABBREVIATED;
                    }
                    return type;
                }
            } else {
                return NOT_NUMERIC;
            }
        }

        public int[] getDigitCount() {
            return digitCount;
        }

        public int[] getPosixDigitCount() {
            return posixDigitCount;
        }
    };
}
