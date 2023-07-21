package org.unicode.cldr.util;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;
import java.util.Comparator;

public class ComparatorUtilities {

    public static final UTF16.StringComparator CODE_POINT_COMPARATOR =
            new UTF16.StringComparator(true, false, 0);

    public static Collator getIcuCollator(String localeId, int strength) {
        return getIcuCollator(new ULocale(localeId), strength);
    }

    public static Collator getIcuCollator(ULocale localeId, int strength) {
        Collator temp = Collator.getInstance(localeId);
        temp.setStrength(strength);
        temp.freeze();
        return temp;
    }

    // TODO: decouple from ICUServiceBuilder
    public static Collator getCldrCollator(String localeId, int strength) {
        Collator col = null;
        try {
            ICUServiceBuilder isb = null;
            isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(localeId));
            col = isb.getRuleBasedCollator().setStrength2(strength).freeze();
        } catch (Exception e) {
        }
        return col != null
                ? col
                : Collator.getInstance(new ULocale(localeId)).setStrength2(strength).freeze();
    }

    public static Comparator<String> wrapForCodePoints(Comparator<String> comparator) {
        return comparator == null
                ? CODE_POINT_COMPARATOR
                : new MultiComparator<>(comparator, CODE_POINT_COMPARATOR);
    }
}
