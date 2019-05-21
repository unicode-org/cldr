package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class Emoji {
    public static final String EMOJI_VARIANT = "\uFE0F";
    public static final String COMBINING_ENCLOSING_KEYCAP = "\u20E3";
    public static final String ZWJ = "\u200D";
    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(0x1F1E6, 0x1F1FF).freeze();
    public static final UnicodeSet MODIFIERS = new UnicodeSet("[🏻-🏿]").freeze();
    public static final UnicodeSet TAGS = new UnicodeSet(0xE0000, 0xE007F).freeze();
    public static final UnicodeSet FAMILY = new UnicodeSet("[\u200D 👦-👩 💋 ❤]").freeze();
    public static final UnicodeSet GENDER = new UnicodeSet().add(0x2640).add(0x2642).freeze();
    public static final UnicodeSet SPECIALS = new UnicodeSet("[{🧑‍🤝‍🧑}{🏳‍🌈} {👁‍🗨} {🏴‍☠} {🐕‍🦺} {👨‍🦯} {👨‍🦼} {👨‍🦽} {👩‍🦯} {👩‍🦼} {👩‍🦽}]").freeze();
    public static final UnicodeSet MAN_WOMAN = new UnicodeSet("[👨 👩]").freeze();
    public static final UnicodeSet OBJECT = new UnicodeSet("[👩 🎓 🌾 🍳 🏫 🏭 🎨 🚒 ✈ 🚀 🎤 💻 🔬 💼 🔧 ⚖ ⚕]").freeze();

    static final UnicodeMap<String> emojiToMajorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> emojiToMinorCategory = new UnicodeMap<>();
    static final Map<String, Integer> minorToOrder = new HashMap<>();
    static final Map<String, Integer> emojiToOrder = new LinkedHashMap<>();
    static final UnicodeSet nonConstructed = new UnicodeSet();
    static final UnicodeSet allRgi = new UnicodeSet();
    static final UnicodeSet allRgiNoES = new UnicodeSet();

    static {
        /*
            # group: Smileys & People
            # subgroup: face-positive
            1F600 ; fully-qualified     # 😀 grinning face
         */
        Splitter semi = Splitter.on(';').trimResults();
        String majorCategory = null;
        String minorCategory = null;
        int minorOrder = 0;
        for (String line : FileUtilities.in(Emoji.class, "data/emoji/emoji-test.txt")) {
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
                if (line.startsWith("group:")) {
                    majorCategory = line.substring("group:".length()).trim();
                } else if (line.startsWith("subgroup:")) {
                    minorCategory = line.substring("subgroup:".length()).trim();
                    if (!minorToOrder.containsKey(minorCategory)) {
                        minorToOrder.put(minorCategory, minorOrder = minorToOrder.size());
                    }
                }
                continue;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            Iterator<String> it = semi.split(line).iterator();
            String emojiHex = it.next();
            String original = Utility.fromHex(emojiHex, 4, " ");
            String type = it.next();
            if (type.startsWith("fully-qualified")) {
                allRgi.add(original);
                allRgiNoES.add(original.replace(Emoji.EMOJI_VARIANT, ""));
            }
            emojiToMajorCategory.put(original, majorCategory);
            emojiToMinorCategory.put(original, minorCategory);
            
            // add all the non-constructed values to a set for annotations

            String minimal = original.replace(EMOJI_VARIANT, "");
            boolean singleton = CharSequences.getSingleCodePoint(minimal) != Integer.MAX_VALUE;
            if (!emojiToOrder.containsKey(minimal)) {
                emojiToOrder.put(minimal, emojiToOrder.size());
            }

            // skip constructed values
            if (minimal.contains(COMBINING_ENCLOSING_KEYCAP)
                || REGIONAL_INDICATORS.containsSome(minimal)
                || TAGS.containsSome(minimal)
                || !singleton && MODIFIERS.containsSome(minimal)
                || !singleton && FAMILY.containsAll(minimal)) {
                // do nothing
            } else if (minimal.contains(ZWJ)) { // only do certain ZWJ sequences
                if (SPECIALS.contains(minimal)
                    || GENDER.containsSome(minimal)
                    || MAN_WOMAN.contains(minimal.codePointAt(0)) && OBJECT.contains(minimal.codePointBefore(minimal.length()))) {
                    nonConstructed.add(minimal);
                }
            } else if (!minimal.contains("🔟")) {
                nonConstructed.add(minimal);
            }

        }
        emojiToMajorCategory.freeze();
        emojiToMinorCategory.freeze();
        nonConstructed.add(MODIFIERS); // needed for names
        nonConstructed.freeze();
        allRgi.freeze();
        allRgiNoES.freeze();
    }

    public static UnicodeSet getAllRgi() {
        return allRgi;
    }

    public static UnicodeSet getAllRgiNoES() {
        return allRgiNoES;
    }

    public static String getMinorCategory(String emoji) {
        String minorCat = emojiToMinorCategory.get(emoji);
        if (minorCat == null) {
            throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji 
                + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"minor\", ...");
        }
        return minorCat;
    }

    public static int getMinorToOrder(String minor) {
        Integer result = minorToOrder.get(minor);
        return result == null ? Integer.MAX_VALUE : result;
    }
    
    public static int getEmojiToOrder(String minor) {
        Integer result = emojiToOrder.get(minor);
        return result == null ? Integer.MAX_VALUE : result;
    }

    public static String getMajorCategory(String emoji) {
        String majorCat = emojiToMajorCategory.get(emoji);
        if (majorCat == null) {
            throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji 
                + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"major\", ...");
        }
        return majorCat;
    }

    public static Set<String> getMajorCategories() {
        return emojiToMajorCategory.values();
    }

    public static Set<String> getMinorCategories() {
        return emojiToMinorCategory.values();
    }

    public static UnicodeSet getNonConstructed() {
        return nonConstructed;
    }

    private static Set<String> NAME_PATHS = null;
    private static Set<String> KEYWORD_PATHS = null;
    public static final String TYPE_TTS = "[@type=\"tts\"]";

    public static synchronized Set<String> getNamePaths() {
        return NAME_PATHS != null ? NAME_PATHS : (NAME_PATHS = buildPaths(TYPE_TTS));
    }

    public static synchronized Set<String> getKeywordPaths() {
        return KEYWORD_PATHS != null ? KEYWORD_PATHS : (KEYWORD_PATHS = buildPaths(""));
    }

    private static ImmutableSet<String> buildPaths(String suffix) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String s : Emoji.getNonConstructed()) {
            String base = "//ldml/annotations/annotation[@cp=\"" + s + "\"]" + suffix;
            builder.add(base);
        }
        return builder.build();
    }
}
