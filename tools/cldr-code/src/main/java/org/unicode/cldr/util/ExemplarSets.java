package org.unicode.cldr.util;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import java.util.BitSet;
import java.util.Locale;

public class ExemplarSets {

    public static final UnicodeSet AlwaysOK =
            new UnicodeSet(
                            "[[[:Nd:][:script=common:][:script=inherited:]-[:Default_Ignorable_Code_Point:]-[:C:] - [_]] [\u05BE \u05F3 \u066A-\u066C]"
                                    + "[[؉][་ །༌][ཱ]‎‎{য়}য়]"
                                    + // TODO Fix this Hack
                                    "-[❮❯]]")
                    .freeze(); // [\\u200c-\\u200f]
    // [:script=common:][:script=inherited:]

    public static final UnicodeSet HangulSyllables =
            new UnicodeSet("[[:Hangul_Syllable_Type=LVT:][:Hangul_Syllable_Type=LV:]]").freeze();
    public static final BitSet Japn = new BitSet();
    public static final BitSet Kore = new BitSet();

    static {
        ExemplarSets.Japn.set(UScript.HAN);
        ExemplarSets.Japn.set(UScript.HIRAGANA);
        ExemplarSets.Japn.set(UScript.KATAKANA);
        ExemplarSets.Kore.set(UScript.HAN);
        ExemplarSets.Kore.set(UScript.HANGUL);
    }

    // TODO Fix some of these characters
    private static final UnicodeSet SPECIAL_ALLOW =
            new UnicodeSet(
                            "[\u061C\\u200E\\u200F\\u200c\\u200d"
                                    + "‎‎‎[\u064B\u064E-\u0651\u0670]‎[:Nd:]‎[\u0951\u0952]‎[\u064B-\u0652\u0654-\u0657\u0670]‎[\u0A66-\u0A6F][\u0ED0-\u0ED9][\u064B-\u0652]‎[\\u02BB\\u02BC][\u0CE6-\u0CEF]‎‎[\u0966-\u096F]"
                                    + "‎‎‎[:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:] ]" // restore
                            // [:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:]
                            )
                    .freeze(); // add RLM, LRM [\u200C\u200D]‎
    private static final UnicodeSet UAllowedInExemplars =
            new UnicodeSet(
                            "[[:assigned:]-[:Z:]]") // [:alphabetic:][:Mn:][:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:]
                    .removeAll(AlwaysOK) // this will remove some
                    // [:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:] so we
                    // restore them
                    // in SPECIAL_ALLOW
                    .addAll(SPECIAL_ALLOW) // add RLM, LRM [\u200C\u200D]‎
                    .freeze();
    private static final UnicodeSet AllowedInExemplars =
            new UnicodeSet(UAllowedInExemplars)
                    .removeAll(new UnicodeSet("[[:Uppercase:]-[\u0130]]"))
                    .freeze();

    private static final UnicodeSet UAllowedInNumbers =
            new UnicodeSet(
                            "[\u00A0\u202F[:N:][:P:][:Sm:][:Letter_Number:][:Numeric_Type=Numeric:]]") // [:alphabetic:][:Mn:][:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:]
                    .addAll(SPECIAL_ALLOW) // add RLM, LRM [\u200C\u200D]‎
                    .freeze();
    private static final UnicodeSet ALLOWED_IN_NUMBERS_NOT_IN_MAIN =
            new UnicodeSet("[[:Numeric_Type=Decimal:]]").freeze();

    private static final UnicodeSet ALLOWED_IN_MAIN =
            new UnicodeSet(AllowedInExemplars).removeAll(ALLOWED_IN_NUMBERS_NOT_IN_MAIN).freeze();

    private static final UnicodeSet ALLOWED_IN_PUNCTUATION =
            new UnicodeSet("[[:P:][:S:]-[:Sc:]]").freeze();
    private static final UnicodeSet ALLOWED_IN_AUX =
            new UnicodeSet(AllowedInExemplars)
                    .addAll(ALLOWED_IN_PUNCTUATION)
                    .removeAll(AlwaysOK) // this will remove some
                    // [:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:] so we
                    // restore them
                    // in SPECIAL_ALLOW
                    .addAll(SPECIAL_ALLOW) // add RLM, LRM [\u200C\u200D]‎
                    .freeze();

    public enum ExemplarType {
        main(
                ALLOWED_IN_MAIN,
                "(specific-script - uppercase - invisibles - numbers + \u0130)",
                true),
        auxiliary(ALLOWED_IN_AUX, "(specific-script - uppercase - invisibles + \u0130)", true),
        punctuation(ALLOWED_IN_PUNCTUATION, "punctuation", false),
        punctuation_auxiliary(ALLOWED_IN_PUNCTUATION, "punctuation-auxiliary", false),
        punctuation_person(ALLOWED_IN_PUNCTUATION, "punctuation-person", false),
        numbers(UAllowedInNumbers, "(specific-script - invisibles)", false),
        numbers_auxiliary(UAllowedInNumbers, "(specific-script - invisibles)", false),
        index(UAllowedInExemplars, "(specific-script - invisibles)", false),
        ;

        public final UnicodeSet allowed;
        public final UnicodeSet toRemove;
        public final String message;
        public final boolean convertUppercase;

        ExemplarType(UnicodeSet allowed, String message, boolean convertUppercase) {
            if (!allowed.isFrozen()) {
                throw new IllegalArgumentException("Internal Error");
            }
            this.allowed = allowed;
            this.message = message;
            this.toRemove = new UnicodeSet(allowed).complement().freeze();
            this.convertUppercase = convertUppercase;
        }

        public static ExemplarType from(String name) {
            return name == null || name.isEmpty()
                    ? ExemplarType.main
                    : ExemplarType.valueOf(name.replace('-', '_').toLowerCase(Locale.ROOT));
        }
    }
}
