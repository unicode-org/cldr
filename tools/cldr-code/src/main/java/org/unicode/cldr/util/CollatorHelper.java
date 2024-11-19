package org.unicode.cldr.util;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public final class CollatorHelper {
    public static final Collator ROOT_ORDER = makeRootOrder();
    public static final Collator ROOT_NUMERIC = makeRootNumeric();
    public static final Collator EMOJI_COLLATOR = makeEmojiCollator();

    private static Collator makeRootOrder() {
        return Collator.getInstance(ULocale.ROOT).freeze();
    }

    private static Collator makeRootNumeric() {
        RuleBasedCollator col =
                (RuleBasedCollator) Collator.getInstance(ULocale.ROOT); // freeze below
        col.setNumericCollation(true);
        col.freeze();
        return col;
    }

    private static Collator makeEmojiCollator() {
        final RuleBasedCollator col =
                (RuleBasedCollator) Collator.getInstance(ULocale.forLanguageTag("en-u-co-emoji"));
        col.setStrength(Collator.IDENTICAL);
        col.setNumericCollation(true);
        col.freeze();
        return col;
    }
}
