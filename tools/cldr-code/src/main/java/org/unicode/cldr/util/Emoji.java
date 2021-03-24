package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class Emoji {
    public static final String EMOJI_VARIANT = "\uFE0F";
    public static final String COMBINING_ENCLOSING_KEYCAP = "\u20E3";
    public static final String ZWJ = "\u200D";
    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(0x1F1E6, 0x1F1FF).freeze();
    public static final UnicodeSet MODIFIERS = new UnicodeSet("[🏻-🏿]").freeze();
    public static final UnicodeSet TAGS = new UnicodeSet(0xE0000, 0xE007F).freeze();
    public static final UnicodeSet FAMILY = new UnicodeSet("[\u200D 👦-👩 💋 ❤]").freeze();
    public static final UnicodeSet GENDER = new UnicodeSet().add(0x2640).add(0x2642).freeze();
    public static final UnicodeSet SPECIALS = new UnicodeSet("["
        + "{🐈‍⬛}{🐻‍❄}{👨‍🍼}{👩‍🍼}{🧑‍🍼}{🧑‍🎄}{🧑‍🤝‍🧑}{🏳‍🌈} {👁‍🗨} {🏴‍☠} {🐕‍🦺} {👨‍🦯} {👨‍🦼} {👨‍🦽} {👩‍🦯} {👩‍🦼} {👩‍🦽}"
        + "{🏳‍⚧}{🧑‍⚕}{🧑‍⚖}{🧑‍✈}{🧑‍🌾}{🧑‍🍳}{🧑‍🎓}{🧑‍🎤}{🧑‍🎨}{🧑‍🏫}{🧑‍🏭}{🧑‍💻}{🧑‍💼}{🧑‍🔧}{🧑‍🔬}{🧑‍🚀}{🧑‍🚒}{🧑‍🦯}{🧑‍🦼}{🧑‍🦽}"
        + "{❤‍🔥}, {❤‍🩹}, {😮‍💨}, {😵‍💫}" // #E13.1
        + "]").freeze();
    // May have to add from above, if there is a failure in testAnnotationPaths. Failure will be like:
    // got java.util.TreeSet<[//ldml/annotations/annotation[@cp="🏳‍⚧"][@type="tts"], //ldml/annotations/annotation[@cp="🧑‍⚕"][@type="tts"], ...
    // just extract the items in "...", and change into {...} for adding above.
    // Example: //ldml/annotations/annotation[@cp="🧑‍⚕"] ==> {🧑‍⚕}
    public static final UnicodeSet MAN_WOMAN = new UnicodeSet("[👨 👩]").freeze();
    public static final UnicodeSet OBJECT = new UnicodeSet("[👩 🎓 🌾 🍳 🏫 🏭 🎨 🚒 ✈ 🚀 🎤 💻 🔬 💼 🔧 ⚖ ⚕]").freeze();

    static final UnicodeMap<String> emojiToMajorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> emojiToMinorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> toName = new UnicodeMap<>();
    static {
        emojiToMajorCategory.setErrorOnReset(true);
        emojiToMinorCategory.setErrorOnReset(true);
        toName.setErrorOnReset(true);
    }
    /**
     * A mapping from a majorCategory to a unique ordering number, based on the first time it is encountered.
     */
    static final Map<String, Long> majorToOrder = new HashMap<>();
    /**
     * A mapping from a minorCategory to a unique ordering number, based on the first time it is encountered.
     */
    static final Map<String, Long> minorToOrder = new HashMap<>();
    static final Map<String, Long> emojiToOrder = new LinkedHashMap<>();
    static final UnicodeSet nonConstructed = new UnicodeSet();
    static final UnicodeSet allRgi = new UnicodeSet();
    static final UnicodeSet allRgiNoES = new UnicodeSet();

    static {
        /*
            # group: Smileys & People
            # subgroup: face-positive
            1F600 ; fully-qualified     # 😀 grinning face
         */
        Splitter semi = Splitter.on(CharMatcher.anyOf(";#")).trimResults();
        String majorCategory = null;
        String minorCategory = null;
        long majorOrder = 0;
        long minorOrder = 0;
        //Multimap<Pair<Integer,Integer>,String> majorPlusMinorToEmoji = TreeMultimap.create();
        for (String line : FileUtilities.in(Emoji.class, "data/emoji/emoji-test.txt")) {
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
                if (line.startsWith("group:")) {
                    majorCategory = line.substring("group:".length()).trim();
                    Long oldMajorOrder = majorToOrder.get(majorCategory);
                    if (oldMajorOrder == null) {
                        majorToOrder.put(majorCategory, majorOrder = majorToOrder.size());
                    } else {
                        majorOrder = oldMajorOrder;
                    }
                } else if (line.startsWith("subgroup:")) {
                    minorCategory = line.substring("subgroup:".length()).trim();
                    Long oldMinorOrder = minorToOrder.get(minorCategory);
                    if (oldMinorOrder == null) {
                        minorToOrder.put(minorCategory, minorOrder = minorToOrder.size());
                    } else {
                        minorOrder = oldMinorOrder;
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
            if (emojiHex.contains("1F48F")) {
                int debug = 0;
            }

            String original = Utility.fromHex(emojiHex, 4, " ");
            if (original.contains("💏")) {
                if (false) {
                    System.out.println(original + "\t" + Utility.hex(original));
                }
            }

            String type = it.next();
            if (type.startsWith("fully-qualified")) {
                allRgi.add(original);
                allRgiNoES.add(original.replace(Emoji.EMOJI_VARIANT, ""));
            }
            emojiToMajorCategory.put(original, majorCategory);
            emojiToMinorCategory.put(original, minorCategory);
            String comment = it.next();
            // The comment is now of the form:  # 😁 E0.6 beaming face with smiling eyes
            int spacePos = comment.indexOf(' ');
            spacePos = comment.indexOf(' ', spacePos+1); // get second space
            String name = comment.substring(spacePos+1).trim();
            toName.put(original, name);

            // add all the non-constructed values to a set for annotations

            String minimal = original.replace(EMOJI_VARIANT, "");

            // Add the order. If it is not minimal, add that also.
            if (!emojiToOrder.containsKey(original)) {
                putUnique(emojiToOrder, original, emojiToOrder.size()*100L);
            }
            if (!emojiToOrder.containsKey(minimal)) {
                putUnique(emojiToOrder, minimal, emojiToOrder.size()*100L);
            }
            //
            // majorPlusMinorToEmoji.put(Pair.of(majorOrder, minorOrder), minimal);

            boolean singleton = CharSequences.getSingleCodePoint(minimal) != Integer.MAX_VALUE;
//            if (!emojiToOrder.containsKey(minimal)) {
//                emojiToOrder.put(minimal, emojiToOrder.size());
//            }

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
//        for (Entry<Pair<Integer,Integer>, String> entry : majorPlusMinorToEmoji.entries()) {
//            String minimal = entry.getValue();
//            emojiToOrder.put(minimal, emojiToOrder.size());
//        }
        emojiToMajorCategory.freeze();
        emojiToMinorCategory.freeze();
        nonConstructed.add(MODIFIERS); // needed for names
        nonConstructed.freeze();
        toName.freeze();
        allRgi.freeze();
        allRgiNoES.freeze();
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
        V oldValue = map.put(key, value);
        if (oldValue != null) {
            throw new ICUException("Attempt to change value of " + map
                + " for " + key
                + " from " + oldValue
                + " to " + value
                );
        }
    }

    public static UnicodeSet getAllRgi() {
        return allRgi;
    }

    public static UnicodeSet getAllRgiNoES() {
        return allRgiNoES;
    }

    public static final UnicodeMap<String> EXTRA_SYMBOL_MINOR_CATEGORIES = new UnicodeMap<>();
    public static final Map<String,Long> EXTRA_SYMBOL_ORDER;
    private static final boolean DEBUG = false;
    static {
        String[][] data = {
            {"arrow", "→ ↓ ↑ ← ↔ ↕ ⇆ ⇅"},
            {"alphanum", "© ® ℗ ™ µ"},
            {"geometric", "▼ ▶ ▲ ◀ ● ○ ◯ ◊"},
            {"math", "× ÷ √ ∞ ∆ ∇ ⁻ ¹ ² ³ ≡ ∈ ⊂ ∩ ∪ ° + ± − = ≈ ≠ > < ≤ ≥ ¬ | ~"},
            {"punctuation", "§ † ‡ \\u0020  , 、 ، ; : ؛ ! ¡ ? ¿ ؟ ¶ ※ / \\ & # % ‰ ′ ″ ‴ @ * ♪ ♭ ♯ ` ´ ^ ¨ ‐ ― _ - – — • · . … 。 ‧ ・ ‘ ’ ‚ ' “ ” „ » « ( ) [ ] { } 〔 〕 〈 〉 《 》 「 」 『 』 〖 〗 【 】"},
            {"currency", "€ £ ¥ ₹ ₽ $ ¢ ฿ ₪ ₺ ₫ ₱ ₩ ₡ ₦ ₮ ৳ ₴ ₸ ₲ ₵ ៛ ₭ ֏ ₥ ₾ ₼ ₿ ؋"},
            {"other-symbol", "‾‽‸⁂↚↛↮↙↜↝↞↟↠↡↢↣↤↥↦↧↨↫↬↭↯↰↱↲↳↴↵↶↷↸↹↺↻↼↽↾↿⇀⇁⇂⇃⇄⇇⇈⇉⇊⇋⇌⇐⇍⇑⇒⇏⇓⇔⇎⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪⇵∀∂∃∅∉∋∎∏∑≮≯∓∕⁄∗∘∙∝∟∠∣∥∧∫∬∮∴∵∶∷∼∽∾≃≅≌≒≖≣≦≧≪≫≬≳≺≻⊁⊃⊆⊇⊕⊖⊗⊘⊙⊚⊛⊞⊟⊥⊮⊰⊱⋭⊶⊹⊿⋁⋂⋃⋅⋆⋈⋒⋘⋙⋮⋯⋰⋱■□▢▣▤▥▦▧▨▩▬▭▮▰△▴▵▷▸▹►▻▽▾▿◁◂◃◄◅◆◇◈◉◌◍◎◐◑◒◓◔◕◖◗◘◙◜◝◞◟◠◡◢◣◤◥◦◳◷◻◽◿⨧⨯⨼⩣⩽⪍⪚⪺₢₣₤₰₳₶₷₨﷼"},
        };
        // get the maximum suborder for each subcategory
        Map<String, Long> subcategoryToMaxSuborder = new HashMap<>();
        for (String[] row : data) {
            final String subcategory = row[0];
            for (Entry<String, String> entry : emojiToMinorCategory.entrySet()) {
                if (entry.getValue().equals(subcategory)) {
                    String emoji = entry.getKey();
                    Long order = emojiToOrder.get(emoji);
                    Long currentMax = subcategoryToMaxSuborder.get(subcategory);
                    if (currentMax == null || currentMax < order) {
                        subcategoryToMaxSuborder.put(subcategory, order);
                    }
                }
            }
        }
        if (DEBUG) System.out.println(subcategoryToMaxSuborder);
        Map<String,Long> _EXTRA_SYMBOL_ORDER = new LinkedHashMap<>();
        for (String[] row : data) {
            final String subcategory = row[0];
            final String characters = row[1];

            List<String> items = new ArrayList<>();
            for (int cp : With.codePointArray(characters)) {
                if (cp != ' ') {
                    items.add(With.fromCodePoint(cp));
                }
            }
            final UnicodeSet uset = new UnicodeSet().addAll(items);
            if (uset.containsSome(EXTRA_SYMBOL_MINOR_CATEGORIES.keySet())) {
                throw new IllegalArgumentException("Duplicate values in " + EXTRA_SYMBOL_MINOR_CATEGORIES);
            }
            EXTRA_SYMBOL_MINOR_CATEGORIES.putAll(uset, subcategory);
            long count = subcategoryToMaxSuborder.get(subcategory);
            for (String s : items) {
                ++count;
                _EXTRA_SYMBOL_ORDER.put(s, count);
            }
            subcategoryToMaxSuborder.put(subcategory, count);
        }
        if (DEBUG) System.out.println(_EXTRA_SYMBOL_ORDER);
        EXTRA_SYMBOL_MINOR_CATEGORIES.freeze();
        EXTRA_SYMBOL_ORDER = ImmutableMap.copyOf(_EXTRA_SYMBOL_ORDER);
    }

    public static String getMinorCategory(String emoji) {
        String minorCat = emojiToMinorCategory.get(emoji);
        if (minorCat == null) {
            minorCat = EXTRA_SYMBOL_MINOR_CATEGORIES.get(emoji);
            if (minorCat == null) {
                throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji
                    + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"minor\", ...");
            }
        }
        return minorCat;
    }

    public static String getName(String emoji) {
        return toName.get(emoji);
    }

    public static long getEmojiToOrder(String emoji) {
        Long result = emojiToOrder.get(emoji);
        if (result == null) {
            result = EXTRA_SYMBOL_ORDER.get(emoji);
            if (result == null) {
                throw new InternalCldrException("No Order found for " + emoji
                    + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"minor\", ...");
            }
        }
        return result;
    }

    public static long getEmojiMinorOrder(String minor) {
        Long result = minorToOrder.get(minor);
        if (result == null) {
            throw new InternalCldrException("No minor category (aka subgroup) found for " + minor
                + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"minor\", ...");
        }
        return result;
    }

    public static String getMajorCategory(String emoji) {
        String majorCat = emojiToMajorCategory.get(emoji);
        if (majorCat == null) {
            if (EXTRA_SYMBOL_MINOR_CATEGORIES.containsKey(emoji)) {
                majorCat = "Symbols";
            } else {
                throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji
                    + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"major\", ...");
            }
        }
        return majorCat;
    }

    public static Set<String> getMajorCategories() {
        return emojiToMajorCategory.values();
    }

    public static Set<String> getMinorCategories() {
        return emojiToMinorCategory.values();
    }

    public static Set<String> getMinorCategoriesWithExtras() {
        Set<String> result = new LinkedHashSet<>(emojiToMinorCategory.values());
        result.addAll(EXTRA_SYMBOL_MINOR_CATEGORIES.getAvailableValues());
        return ImmutableSet.copyOf(result);
    }

    public static UnicodeSet getEmojiInMinorCategoriesWithExtras(String minorCategory) {
        return new UnicodeSet(emojiToMinorCategory.getSet(minorCategory))
            .addAll(EXTRA_SYMBOL_MINOR_CATEGORIES.getSet(minorCategory))
            .freeze();
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
