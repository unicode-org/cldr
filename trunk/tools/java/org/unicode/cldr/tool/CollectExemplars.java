package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.HashSet;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class CollectExemplars {
    public static void main(String[] args) {
        final String stock = "en|ar|de|es|fr|it|ja|ko|nl|pl|ru|th|tr|pt|zh|zh_Hant|bg|ca|cs|da|el|fa|fi|fil|hi|hr|hu|id|lt|lv|ro|sk|sl|sr|sv|uk|vi|he|nb|et|ms|am|bn|gu|is|kn|ml|mr|sw|ta|te|ur|eu|gl|af|zu|en_GB|es_419|pt_PT|fr_CA|zh_HK";
        final HashSet<String> REGION_LOCALES = new HashSet<String>(Arrays.asList(stock.split("\\|")));
        UnicodeSet target = new UnicodeSet();
        add("special", null, new UnicodeSet("[㐀-䶵一-鿌﨎﨏﨑﨓﨔﨟﨡﨣﨤﨧-﨩𠀀-𪛖 𪜀-𫜴𫝀-𫠝]"), target);
        for (ULocale locale : ULocale.getAvailableLocales()) {
            if (REGION_LOCALES.contains(locale.toString())) {
                UnicodeSet mainExemplars = LocaleData.getExemplarSet(locale, UnicodeSet.CASE);
                add("main", locale, mainExemplars, target);
            }
        }
        for (ULocale locale : ULocale.getAvailableLocales()) {
            if (REGION_LOCALES.contains(locale.toString())) {
                UnicodeSet auxExemplars = LocaleData.getExemplarSet(locale, UnicodeSet.CASE, LocaleData.ES_AUXILIARY);
                add("aux", locale, auxExemplars, target);
            }
        }
        System.out.println("Added\t" + target.toPattern(false));
        UnicodeSet toRemove = new UnicodeSet();
        main: for (String s : target) {
            int len = s.codePointCount(0, s.length());
            if (len > 1) {
                int cp;
                for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
                    cp = s.codePointAt(0);
                    if (!target.contains(cp)) {
                        continue main;
                    }
                }
                toRemove.add(s);
            }
        }
        removing("Collapsing", target, toRemove);
        removing("whitespace, controls, symbol, punct", target, new UnicodeSet("[[:z:][:c:][:s:][:p:]]"));
        removing("numbers", target, new UnicodeSet("[:n:]"));
        add("ASCII numbers", null, new UnicodeSet("[0-9]"), target);
        add("joiners", null, new UnicodeSet("[:join_controls:]"), target);
        UnicodeSet exclude = new UnicodeSet("[[:sc=common:][:sc=hebr:][:sc=zinh:]&[:mn:]]");
        removing("certain non-spacing", target, exclude);

        System.out.println("Result\t" + target.toPattern(false));
    }

    public static void removing(String title, UnicodeSet target, UnicodeSet toRemove) {
        UnicodeSet diff = new UnicodeSet(toRemove).retainAll(target);
        System.out.println(title + "\t" + diff.toPattern(false));
        target.removeAll(diff);
    }

    private static void add(String title, ULocale locale, UnicodeSet mainExemplars,
        UnicodeSet target) {
        if (!target.containsAll(mainExemplars)) {
            UnicodeSet diff = new UnicodeSet(mainExemplars).removeAll(target);
            System.out.println(locale + "\t" + title + "\tadding\t" + diff.toPattern(false));
            target.addAll(diff);
        }
    }
}
