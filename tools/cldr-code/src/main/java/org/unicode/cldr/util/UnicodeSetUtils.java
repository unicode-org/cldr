package org.unicode.cldr.util;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.ExemplarType;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class UnicodeSetUtils {

    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[➖❰❱"
        + "[:cc:][:Default_Ignorable_Code_Point:]"
        + "[:patternwhitespace:][:whitespace:]"
        + "-[[:block=tags:]-[:cn:]]" // don't quote tag characters, they are only in emoji
        + "]").freeze();

    public static final UnicodeSet EMOJI_EXCEPTIONS = new UnicodeSet("[:emoji_component:]").retainAll(TO_QUOTE).freeze();
    public static final UnicodeSet EMOJI = new UnicodeSet("[:emoji:]");
    public static final char ZERO_WIDTH_JOINER = 0x200D;
    public static final char VARIATION_SELECTOR_16 = 0xFE0F;

    static { // future proofing.
        if (EMOJI_EXCEPTIONS.size() != 2
            || !EMOJI_EXCEPTIONS.contains(ZERO_WIDTH_JOINER)
            || !EMOJI_EXCEPTIONS.contains(VARIATION_SELECTOR_16)
            ) {
            throw new IllegalArgumentException();
        }
    }

    public static class Quoter implements Function<String,String> {
        @Override
        public String apply(String source) {
            if (TO_QUOTE.containsNone(source)) {
                return source;
            }
            StringBuilder result = new StringBuilder();
            // TODO detect emoji and don't quote ZWJ, etc
            boolean afterEmojiPlus = false;
            int cp;
            for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
                cp = source.codePointAt(i);
                if (TO_QUOTE.contains(cp)) {
                    // We have some simple tests to avoid quoting in the middle of an emoji sequence
                    if (cp == ZERO_WIDTH_JOINER) {
                        afterEmojiPlus = false;
                        // quote unless right before emoji
                        if (EMOJI.contains(codePointAtBounded(source, i + Character.charCount(cp)))) {
                            result.appendCodePoint(cp);
                            continue; // skip quoting
                        }
                    } else if (cp == VARIATION_SELECTOR_16) {
                        if (afterEmojiPlus) {
                            result.appendCodePoint(cp);
                            continue;
                        }
                    } else {
                        afterEmojiPlus = false;
                    }
                    quote(cp, result);
                    continue;
                } else if (EMOJI.contains(cp)) {
                    afterEmojiPlus = true;
                } else {
                    afterEmojiPlus = false;
                }
                result.appendCodePoint(cp);
            }
            return result.toString();
        }

            /** return -1 if out of bounds, else codePointAt */
        private int codePointAtBounded(String source, int index) {
            return index < 0 || index >= source.length() ? null : source.codePointAt(index);
        }

        private void quote(int cp, StringBuilder toAppendTo) {
            String name = MAP_CP_TO_NAME.get(cp);
            toAppendTo.append("❰")
            .append(name == null ? Utility.hex(cp, 2) : name)
            .append("❱");
        }
        private void format(int cp, StringBuilder toAppendTo) {
            if (TO_QUOTE.contains(cp)) {
                quote(cp, toAppendTo);
            } else {
                toAppendTo.appendCodePoint(cp);
            }
        }
    }

    public static Quoter QUOTER = new Quoter();

    private static final BiMap<Integer, String> MAP_CP_TO_NAME = ImmutableBiMap.<Integer, String>builder()
        // escapes for syntax
        .put((int)'➖', "_")
        .put((int)'❰', "ESC_S")
        .put((int)'❱', "ESC_E")

        // regular IDs; for now restricted to what we might encounter in CLDR
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

    /**
     * FlatUnicodeSet is a basic format for use in exemplar sets and similar environments.
     * The goal is a simple format for a set of literal characters that dispenses with
     * operations and properties in favor of minimizing syntax and escaping.
     * For the syntax needed, non-ASCII characters are used to further lessen escaping,
     * and the escaping structure is semi-verbose.
     * <br>The structure is:
     * <pre>
     * uset = range (' ' range)*
     * range = codepoint+ | codepoint '➖' codepoint
     * codepoint = literal | '❰' id '❱'
     * id = hexCodePoint | character_acronym
     * hexCodePoint = [A-F0-9]{2,6}
     * character_acronym = [A-Z_]*
     * </pre>
     * So the syntax characters are space, ➖, ❰, ❱
     * To express the literals, use ❰SP❱, ❰RANGE❱, ❰ESC_S❱, and ❰ESC_E❱
     * @author markdavis
     *
     */
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
                // do compressed format, no spaces
                for (EntryRange range : t.ranges()) {
//                    if (result.length() != 0) {
//                        result.append(' ');
//                    }
                    if (range.codepoint == range.codepointEnd) {
                        QUOTER.format(range.codepoint, result);
                    } else if (range.codepoint == range.codepointEnd - 1) {
                        QUOTER.format(range.codepoint, result);
                        //result.append(' ');
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

        private static final Pattern END_OF_QUOTE = Pattern.compile("(([A-F0-9]{2,6})|([A-Z_]*))❱");

        /*
         * uset = range (' ' range)*
         * range = codepoint+ | codepoint '➖' codepoint
         * codepoint = literal | '❰' id '❱'
         * id = hexCodePoint | character_acronym
         * hexCodePoint = [A-F0-9]{2-6}
         */
        static final UnicodeSet parse(String source) {
            UnicodeSet result = new UnicodeSet();
            StringBuilder range = new StringBuilder(); // TODO optimize
            Matcher matcher = END_OF_QUOTE.matcher(source);
            int rangeStart = -1;
            int cp;
            for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
                cp = source.codePointAt(i);
                switch (cp) {
                default:
                    range.appendCodePoint(cp);
                    break;
                case ' ':
                    if (rangeStart >= 0) {
                        throw new IllegalArgumentException();
                    }
                    if (range.length() != 0) {
                        result.add(range);
                        range.setLength(0);
                    }
                    break;
                case '➖':
                    switch (range.length()) {
                    case 1:
                        break;
                    case 2: if (Character.codePointCount(range, 0, 2) == 1) {
                        break;
                    }
                    default:
                        throw new IllegalArgumentException();
                    }
                    rangeStart = cp;
                    break;
                case '❰':
                    if (rangeStart >= 0) {
                        throw new IllegalArgumentException();
                    }
                    matcher.region(i, source.length());
                    if (!matcher.lookingAt()) {
                        throw new IllegalArgumentException();
                    }
                    String group = matcher.group(2);
                    if (group != null) {
                        cp = Integer.parseInt(source);
                    } else {
                        group = matcher.group(3);
                        cp = MAP_CP_TO_NAME.inverse().get(group);
                    }
                    range.appendCodePoint(Integer.parseInt(source));
                    break;
                case '❱':
                    throw new IllegalArgumentException();
                }
            }
            if (rangeStart >= 0) {
                throw new IllegalArgumentException();
            }
            if (range.length() != 0) {
                result.add(range);
            }
            return result;
        }
    }
    // quick tests
    public static void main(String[] args) {
        Set<String> cldrLocales = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);

        FlatUnicodeFormatter fuf = new FlatUnicodeFormatter();
        String[][] tests = {
            {"[abcq]", "a b c q"},
            {"[ab{cq}]", "a b cq"},
            {"[a\\u0020]", "❰SP❱ a"},
            {"[\\u0019-!]", "❰19❱ ❰SP❱ !"},
            {"[{2️⃣} 🪷-🪺 🫃{🫃🏻}{🇿🇼} {🏴\\U000E0067\\U000E0062\\U000E0065\\U000E006E\\U000E0067\\U000E007F}]", "🇿🇼 🏴󠁧󠁢󠁥󠁮󠁧󠁿 🪷 🪸 🪹 🪺 🫃 🫃🏻 2️⃣"},
        };
        for (String[] row : tests) {
            UnicodeSet test = new UnicodeSet(row[0]);
            String expected = row[1];
            check("basic", test, fuf, expected);
        }
        Set<String> locales = ImmutableSet.of("en", "sv", "ar");
        locales = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, ImmutableSet.of(Level.MODERN));

        for (String locale : locales) {
            if (locale.equals("bgc") || locale.equals("bho")) {
                continue;
            }
            CLDRConfig CONFIG = CLDRConfig.getInstance();
            Factory factory = CONFIG.getCldrFactory();
            CLDRFile cldrFile = factory.make(locale, true);
            fuf.setLocale(locale);
            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet exemplars = cldrFile.getRawExemplarSet(type, null);
                check(locale + "\t" + type, exemplars, fuf, null);
            }
        }
    }
    private static String check(String message, UnicodeSet sample, FlatUnicodeFormatter fuf, String expected) {
        try {
            String formatted = fuf.apply(sample);
            System.out.println(message + "\t" + formatted);
            if (expected != null) {
                if (!formatted.equals(expected)) {
                    System.out.println("FAIL " + message + "\texpected " + expected + "\tactual " + formatted);
                }
            }
            UnicodeSet reversed = FlatUnicodeFormatter.parse(formatted);
            if (!reversed.equals(sample)) {
                System.err.println("\tFAIL\t" + reversed);
                throw new IllegalArgumentException("\tFAIL\t" + reversed);
            }
            return formatted;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
