package org.unicode.cldr.util;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public final class CollatorHelper {
    public static final RuleBasedCollator EMOJI_COLLATOR = makeEmojiCollator();
    public static final RuleBasedCollator ROOT_COLLATOR = makeRootCollator();
    public static final RuleBasedCollator ROOT_IDENTICAL = makeRootIdentical();
    public static final RuleBasedCollator ROOT_NUMERIC = makeRootNumeric();
    public static final RuleBasedCollator ROOT_NUMERIC_IDENTICAL = makeRootNumericIdentical();
    public static final RuleBasedCollator ROOT_PRIMARY = makeRootPrimary();
    public static final RuleBasedCollator ROOT_PRIMARY_SHIFTED = makeRootPrimaryShifted();
    public static final RuleBasedCollator ROOT_SECONDARY = makeRootSecondary();

    private static RuleBasedCollator makeEmojiCollator() {
        ULocale uLocale = ULocale.forLanguageTag("en-u-co-emoji");
        RuleBasedCollator col = (RuleBasedCollator) RuleBasedCollator.getInstance(uLocale);
        col.setStrength(RuleBasedCollator.IDENTICAL);
        col.setNumericCollation(true);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootCollator() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootIdentical() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setStrength(Collator.IDENTICAL);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootNumeric() {
        RuleBasedCollator col = (RuleBasedCollator) RuleBasedCollator.getInstance(ULocale.ROOT);
        col.setNumericCollation(true);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootNumericIdentical() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setStrength(Collator.IDENTICAL);
        col.setNumericCollation(true);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootPrimary() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setStrength(Collator.PRIMARY);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootPrimaryShifted() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setStrength(Collator.PRIMARY);
        col.setAlternateHandlingShifted(true);
        return (RuleBasedCollator) col.freeze();
    }

    private static RuleBasedCollator makeRootSecondary() {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setStrength(Collator.SECONDARY);
        return (RuleBasedCollator) col.freeze();
    }
}
