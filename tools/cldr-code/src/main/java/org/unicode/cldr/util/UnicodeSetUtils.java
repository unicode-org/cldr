package org.unicode.cldr.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.unicode.cldr.util.CLDRFile.ExemplarType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class UnicodeSetUtils {

    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[➖❰❱[:cc:][:Default_Ignorable_Code_Point:][:patternwhitespace:][:whitespace:]]").freeze();

    public static class Quoter implements Function<String,String> {
        @Override
        public String apply(String source) {
            if (TO_QUOTE.containsNone(source)) {
                return source;
            }
            StringBuilder result = new StringBuilder();
            int cp;
            // TODO detect emoji and don't quote
            for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
                cp = source.codePointAt(i);
                format(cp, result);
            }
            return result.toString();
        }

        public void format(int cp, StringBuilder toAppendTo) {
            if (TO_QUOTE.contains(cp)) {
                String name = NAME_MAP.get(cp);
                toAppendTo.append("❰")
                .append(name == null ? Utility.hex(cp, 2) : name)
                .append("❱");
            } else {
                toAppendTo.appendCodePoint(cp);
            }
        }
    }

    public static Quoter QUOTER = new Quoter();

    private static final Map<Integer, String> NAME_MAP = ImmutableMap.<Integer, String>builder()
        .put(0x0020, "SP")
        .put(0x00AD, "SHY")
        .put(0x0009, "TAB")

        .put(0x200C, "ZWNJ")
        .put(0x200D, "ZWJ")

        .put(0x061C, "ALM")
        .put(0x200E, "LRM")
        .put(0x200F, "RLM")

        .put(0x200B, "ZWSP")
        .put(0x2060, "WJ")

        .put(0x00A0, "NBSP")

        .put(0x202F, "NNBSP")
        .put(0x2009, "NSP")
        .build();

    public static class FlatUnicodeFormatter implements Function<UnicodeSet, String> {

        Collator col = Collator.getInstance(ULocale.ROOT);

        public void setLocale(String locale) {
            ICUServiceBuilder isb = null;
            try {
                isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(locale));
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
            col.setStrength(Collator.IDENTICAL);
       }

        @Override
        public String apply(UnicodeSet t) {
            StringBuilder result = new StringBuilder();
            if (t.size() > 300) {
                // do compressed format
                for (EntryRange range : t.ranges()) {
                    if (result.length() != 0) {
                        result.append(' ');
                    }
                    if (range.codepoint == range.codepointEnd) {
                        QUOTER.format(range.codepoint, result);
                    } else if (range.codepoint == range.codepointEnd - 1) {
                        QUOTER.format(range.codepoint, result);
                        result.append(' ');
                        QUOTER.format(range.codepointEnd, result);
                    } else {
                        QUOTER.format(range.codepoint, result);
                        result.append('➖');
                        QUOTER.format(range.codepointEnd, result);
                    }
                }
                for (String string : t.strings()) {
                    if (result.length() != 0) {
                        result.append(' ');
                    }
                   result.append(QUOTER.apply(string));
                }
            }

            TreeSet<String> orderedStrings = new TreeSet<>(col);
            for (String s : t) {
                orderedStrings.add(s);
            }
            for (String s : orderedStrings) {
                if (result.length() != 0) {
                    result.append(' ');
                }
                result.append(QUOTER.apply(s));
            }
            return result.toString();
        }

        // TODO add parser
    }
    // quick tests
    public static void main(String[] args) {
//        UnicodeSetPrettyPrinter pp = new UnicodeSetPrettyPrinter()
//            .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
//            .setCompressRanges(false)
//            .setQuoter(QUOTER)
//            .setToQuote(TO_QUOTE);

        Set<String> locales = ImmutableSet.of("en", "sv", "ar");
        FlatUnicodeFormatter fuf = new FlatUnicodeFormatter();

        for (String locale : locales) {
            CLDRConfig CONFIG = CLDRConfig.getInstance();
            Factory factory = CONFIG.getCldrFactory();
            CLDRFile cldrFile = factory.make(locale, true);
            fuf.setLocale(locale);
//            setForLocale(locale, pp);




            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet exemplars = cldrFile.getRawExemplarSet(type, null);
                String formatted = fuf.apply(exemplars);
                System.out.println(locale + "\t" + type + "\t" + formatted);
            }

        }

//        System.out.println(pp.format(new UnicodeSet("[\\u0000-\\u00ff]")));
//        for (int i = 0; i < 0xff; ++i) {
//            System.out.println(QUOTER.transform(new StringBuilder().appendCodePoint(i).toString()));
//        }
//        for (String s : TO_QUOTE) {
//            System.out.println(QUOTER.transform(s));
//        }

    }

//    private static void setForLocale(String locale, UnicodeSetPrettyPrinter prettyPrinter) {
//        ICUServiceBuilder isb = null;
//        Collator col;
//        try {
//            isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(locale));
//        } catch (Exception e) {
//        }
//
//        if (isb != null) {
//            try {
//                col = isb.getRuleBasedCollator();
//            } catch (Exception e) {
//                col = Collator.getInstance(ULocale.ROOT);
//            }
//        } else {
//            col = Collator.getInstance(ULocale.ROOT);
//        }
//
//        Collator spaceCol = Collator.getInstance(new ULocale(locale));
//        if (spaceCol instanceof RuleBasedCollator) {
//            ((RuleBasedCollator) spaceCol).setAlternateHandlingShifted(false);
//        }
//
//        prettyPrinter
//        .setOrdering(col)
//        .setSpaceComparator(spaceCol);
//
//    }
}
