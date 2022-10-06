package org.unicode.cldr.util;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class UnicodeSetUtils {

    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[➖❰❱"
        + "[:cc:][:Default_Ignorable_Code_Point:]"
        + "[:patternwhitespace:][:whitespace:]"
        + "-[[:block=tags:]-[:cn:]]" // don't quote tag characters, they are only in emoji
        + "]").freeze();

    public static final Pattern END_OF_QUOTE = Pattern.compile("❰(?:([A-F0-9]{2,6})|([A-Z_]*)|([^❱]*))❱");

    public static UTF16.StringComparator CODEPOINT_ORDER = new StringComparator(true, false, 0);

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

        /** return 0xFFFF if out of bounds, else codePointAt */
        private int codePointAtBounded(String source, int index) {
            return index < 0 || index >= source.length() ?0xFFFF : source.codePointAt(index);
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

    static final MultiComparator<String> ROOT_COLLATOR = new MultiComparator<>((Comparator<String>) (Comparator<?>) Collator.getInstance(ULocale.ROOT), CODEPOINT_ORDER);

    /**
     * FlatUnicodeSet is a basic format for use in exemplar sets and similar environments.
     * The goal is a simple format for a set of literal characters that dispenses with
     * operations and properties in favor of minimizing syntax and escaping.
     * For the syntax needed, 3 non-ASCII characters are used to further lessen escaping,
     * and the escaping structure is semi-verbose. Those characters are ➖, ❰, ❱
     *
     * <br>The structure is:
     * <pre>
     * uset = range (' ' range)*
     * range = codepoint+ | codepoint '➖' codepoint
     * codepoint = literal | '❰' id '❱'
     * id = hexCodePoint | character_acronym
     * hexCodePoint = [A-F0-9]{2,6}
     * character_acronym = [A-Z_]*
     * </pre>
     * To express the literal syntax characters, use ❰SP❱, ❰RANGE❱, ❰ESC_S❱, and ❰ESC_E❱
     * @author markdavis
     *
     */
    public static class FlatUnicodeFormatter implements Function<UnicodeSet, String> {
        Comparator<String> col = ROOT_COLLATOR;

        public FlatUnicodeFormatter setLocale(String locale) {
            RuleBasedCollator col2 = null;
            try {
                ICUServiceBuilder isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(locale));
                if (isb != null) {
                    col2 = isb.getRuleBasedCollator();
                }
            } catch (Exception e) {
            }
            if (col2 != null) {
                col = new MultiComparator<>((Comparator<String>) (Comparator<?>) col2, CODEPOINT_ORDER);
            } else {
                col = ROOT_COLLATOR;
            }
            return this;
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


        /*
         * uset = range (' ' range)*
         * range = codepoint+ | codepoint '➖' codepoint
         * codepoint = literal | '❰' id '❱'
         * id = hexCodePoint | character_acronym
         * hexCodePoint = [A-F0-9]{2-6}
         */
        public static final UnicodeSet parse(String source) {
            UnicodeSet result = new UnicodeSet();
            StringBuilder item = new StringBuilder(); // TODO optimize
            Matcher matcher = null;
            int rangeStart = -1;
            int cp;
            final int length = source.length();
            for (int i = 0; i < length;) {
                cp = source.codePointAt(i);
                switch (cp) {
                default:
                    item.appendCodePoint(cp);
                    break;
                case ' ':
                    if (rangeStart >= 0) {
                        int cp2 = CharSequences.getSingleCodePoint(item);
                        if (cp2 == Integer.MAX_VALUE) {
                            throw new FlatUnicodeSetException("Must have exactly one character after '➖'", source, i);
                        }
                        result.add(rangeStart, cp2);
                        rangeStart=-1;
                    } else if (item.length() != 0) {
                        result.add(item);
                    }
                    item.setLength(0);
                    break;
                case '➖':
                    int cp2 = CharSequences.getSingleCodePoint(item);
                    if (cp2 == Integer.MAX_VALUE) {
                        throw new FlatUnicodeSetException("Must have exactly one character before '➖'", source, i);
                    }
                    rangeStart = cp2;
                    item.setLength(0);
                    break;
                case '❰':
                    if (matcher == null) {
                        matcher = END_OF_QUOTE.matcher(source);
                    }
                    matcher.region(i, length);
                    if (!matcher.lookingAt()) {
                        throw new FlatUnicodeSetException("'❰' without closing '❱'", source, i);
                    }
                    String hex = matcher.group(1);
                    if (hex != null) {
                        cp = Integer.parseUnsignedInt(hex, 16);
                        if (cp < 0 || cp > 0x10FFFF) {
                            throw new FlatUnicodeSetException("Illegal codepoint number", source, i);
                        }
                    } else {
                        String name = matcher.group(2);
                        if (name != null) {
                            Integer cpInt = MAP_CP_TO_NAME.inverse().get(name);
                            if (cpInt == null) {
                                throw new FlatUnicodeSetException("'❰name❱' name not recognized", source, i);
                            }
                            cp = cpInt;
                        } else {
                            throw new FlatUnicodeSetException("'❰…❱' contains invalid name or hex number", source, i);
                        }
                    }
                    i = matcher.end(0);
                    item.appendCodePoint(cp);
                    continue;
                case '❱':
                    throw new FlatUnicodeSetException("'❱' not after '❰'", source, i);
                }
                i += Character.charCount(cp);
            }
            if (rangeStart >= 0) {
                int cp2 = CharSequences.getSingleCodePoint(item);
                if (cp2 == Integer.MAX_VALUE) {
                    throw new FlatUnicodeSetException("Must have exactly one character after '➖'", source, length);
                }
                result.add(rangeStart, cp2);
            } else if (item.length() != 0) {
                result.add(item);
            }
            return result;
        }
    }

    static class FlatUnicodeSetException extends RuntimeException {
        private static final long serialVersionUID = -4565820177373342632L;
        public FlatUnicodeSetException(String message, String source, int position) {
            super(message + ": «" + source.substring(0, position+1) + "❌" + source.substring(position+1) + "»");
        }
    }

    public static String matcherGetsTo(String test, int start, int end) {
        // TODO make it work for code points
        boolean hitEnd = false;
        int lastMatch = -1;
        for (int i = start+1; i < end; ++i) {
            if (UTF16.findCodePointOffset(test, i) != i) { // skip middle of code point
                continue;
            }
            Matcher matcher = UnicodeSetUtils.END_OF_QUOTE.matcher(test);
            matcher.region(start, i);
            boolean ok = matcher.lookingAt();
            if (ok) {
                lastMatch = i;
            }
            hitEnd = matcher.hitEnd();
            if (!hitEnd) {
                return test.substring(0, i-1) + "❌" + test.substring(i-1);
            }
        }
        return test + (hitEnd ? "✅" : "❓");
    }
}
