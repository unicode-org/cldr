package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.PathHeader.PageId;

public class Emoji {
    public static final Collator COLLATOR = CLDRConfig.getInstance().getCollator();
    public static final String EMOJI_VARIANT = "\uFE0F";
    public static final char JOINER = '\u200D';
    public static final String JOINER_STR = "\u200D";

    public static final String FEMALE = "\u2640";
    public static final String MALE = "\u2642";
    public static final String TRANSGENDER = "\u26A7";

    public static final String COMBINING_ENCLOSING_KEYCAP = "\u20E3";
    public static final String ZWJ = "\u200D";
    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(0x1F1E6, 0x1F1FF).freeze();
    public static final UnicodeSet SKIN_MODIFIERS = new UnicodeSet("[🏻-🏿]").freeze();
    public static final UnicodeSet HAIR_MODIFIERS = new UnicodeSet("[🦰🦱🦳🦲]").freeze();
    public static final UnicodeSet TAGS = new UnicodeSet(0xE0000, 0xE007F).freeze();
    public static final UnicodeSet FAMILY = new UnicodeSet("[\u200D 👦-👩 💋 ❤]").freeze();
    public static final UnicodeSet GENDER = new UnicodeSet().add(0x2640).add(0x2642).freeze();
    public static final UnicodeSet SPECIALS =
            new UnicodeSet(
                            "["
                                    + "{🐈‍⬛}{🐻‍❄}{👨‍🍼}{👩‍🍼}{🧑‍🍼}{🧑‍🎄}{🧑‍🤝‍🧑}{🏳‍🌈} {👁‍🗨} {🏴‍☠} {🐕‍🦺} {👨‍🦯} {👨‍🦼} {👨‍🦽} {👩‍🦯} {👩‍🦼} {👩‍🦽}"
                                    + "{🏳‍⚧}{🧑‍⚕}{🧑‍⚖}{🧑‍✈}{🧑‍🌾}{🧑‍🍳}{🧑‍🎓}{🧑‍🎤}{🧑‍🎨}{🧑‍🏫}{🧑‍🏭}{🧑‍💻}{🧑‍💼}{🧑‍🔧}{🧑‍🔬}{🧑‍🚀}{🧑‍🚒}{🧑‍🦯}{🧑‍🦼}{🧑‍🦽}"
                                    + "{❤‍🔥}, {❤‍🩹}, {😮‍💨}, {😵‍💫}" // #E13.1
                                    + "]")
                    .freeze();
    // May have to add from above, if there is a failure in testAnnotationPaths. Failure will be
    // like:
    // got java.util.TreeSet<[//ldml/annotations/annotation[@cp="🏳‍⚧"][@type="tts"],
    // //ldml/annotations/annotation[@cp="🧑‍⚕"][@type="tts"], ...
    // just extract the items in "...", and change into {...} for adding above.
    // Example: //ldml/annotations/annotation[@cp="🧑‍⚕"] ==> {🧑‍⚕}
    public static final UnicodeSet MAN_WOMAN = new UnicodeSet("[👨 👩]").freeze();
    public static final UnicodeSet OBJECT =
            new UnicodeSet("[👩 🎓 🌾 🍳 🏫 🏭 🎨 🚒 ✈ 🚀 🎤 💻 🔬 💼 🔧 ⚖ ⚕]").freeze();

    static final UnicodeMap<String> emojiToMajorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> emojiToMinorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> toName = new UnicodeMap<>();

    static final UnicodeSet NEUTRAL =
            new UnicodeSet(
                            "[⛷⛹🏂-🏄🏇🏊-🏎👤👥👪-👳👶👷👼💁💂💆💇💏💑🕴🕵🗣🙅-🙇🙋🙍🙎🚣🚴-🚶🛀🛌🤦🤰🤱🤵🤷-🤾🦸🦹🧑-🧟]")
                    .freeze();
    public static final String ZWJ_HANDSHAKE_ZWJ = JOINER_STR + UTF16.valueOf(0x1F91D) + JOINER_STR;
    public static final String ZWJ_HEART_ZWJ = JOINER_STR + UTF16.valueOf(0x2764) + JOINER_STR;
    public static final UnicodeSet FULL_ZWJ_GENDER_MARKERS =
            new UnicodeSet()
                    .add(JOINER + FEMALE)
                    .add(JOINER + MALE)
                    .add(JOINER + FEMALE + EMOJI_VARIANT)
                    .add(JOINER + MALE + EMOJI_VARIANT)
                    .freeze();

    static final Transliterator NEUTER;

    static {
        final UnicodeMap<String> TO_NEUTRAL =
                new UnicodeMap<String>()
                        .put("👦", "🧒")
                        .put("👧", "🧒")
                        .put("👨", "🧑")
                        .put("👩", "🧑")
                        .put("👴", "🧓")
                        .put("👵", "🧓")
                        .put("🤴", "🧑\u200D👑")
                        .put("👸", "🧑\u200D👑")
                        .put("🎅", "🧑\u200D🎄")
                        .put("🤶", "🧑\u200D🎄")
                        .put("💃", "🧑\u200D🎶")
                        .put("🕺", "🧑\u200D🎶")
                        .put("👫", "🧑" + ZWJ_HANDSHAKE_ZWJ + "🧑")
                        .put("👬", "🧑" + ZWJ_HANDSHAKE_ZWJ + "🧑")
                        .put("👭", "🧑" + ZWJ_HANDSHAKE_ZWJ + "🧑")
                        .put(JOINER + FEMALE + EMOJI_VARIANT, "")
                        .put(JOINER + MALE + EMOJI_VARIANT, "")
                        .put(JOINER + FEMALE, "")
                        .put(JOINER + MALE, "")
                        .freeze();
        Map<String, String> results =
                new TreeMap(Ordering.from(SupplementalDataInfo.LENGTH_FIRST).reversed());
        for (Entry<String, String> entry : TO_NEUTRAL.entrySet()) {
            results.put(entry.getKey(), entry.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : results.entrySet()) {
            sb.append(entry.getKey()).append('→').append(entry.getValue()).append(";\n");
        }
        NEUTER = Transliterator.createFromRules("foo", sb.toString(), Transliterator.FORWARD);
    }

    static {
        emojiToMajorCategory.setErrorOnReset(true);
        emojiToMinorCategory.setErrorOnReset(true);
        toName.setErrorOnReset(true);
    }
    /**
     * A mapping from a majorCategory to a unique ordering number, based on the first time it is
     * encountered.
     */
    static final Map<String, Long> majorToOrder = new HashMap<>();
    /**
     * A mapping from a minorCategory to a unique ordering number, based on the first time it is
     * encountered.
     */
    static final Map<String, Long> minorToOrder = new HashMap<>();

    static final Map<String, Long> emojiToOrder = new LinkedHashMap<>();
    static final UnicodeSet nonConstructed = new UnicodeSet();
    static final UnicodeSet allRgi = new UnicodeSet();
    static final UnicodeSet allRgiNoES = new UnicodeSet();

    static final UnicodeMap<String> restoreVariants = new UnicodeMap<>();
    static final Set<Set<String>> genderSets;
    // ߘ E1.0 grinning face
    static {
        /*
         * Example from emoji-test.txt:
         *   # group: Smileys & Emotion
         *   # subgroup: face-smiling
         *   1F600 ; fully-qualified # 😀 grinning face
         */
        Splitter semi = Splitter.on(';').trimResults();
        String majorCategory = null;
        String minorCategory = null;
        final Matcher commentMatcher =
                Pattern.compile("\\s*[\\S]+\\s+(?:E\\d*.\\d+\\s+)(.*)").matcher("");

        Map<String, String> neutralAndGenderedToNeutral = new TreeMap<>();
        for (String line : FileUtilities.in(Emoji.class, "data/emoji/emoji-test.txt")) {
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
                if (line.startsWith("group:")) {
                    majorCategory = line.substring("group:".length()).trim();
                    majorToOrder.computeIfAbsent(majorCategory, k -> (long) majorToOrder.size());
                } else if (line.startsWith("subgroup:")) {
                    minorCategory = line.substring("subgroup:".length()).trim();
                    minorToOrder.computeIfAbsent(minorCategory, k -> (long) minorToOrder.size());
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
            String typeRaw = it.next();
            // fully-qualified     # #️⃣ E0.6 keycap: #
            int hashPos = typeRaw.indexOf('#');
            if (hashPos < 0) {
                throw new IllegalArgumentException("unexpected comment format: " + typeRaw);
            }
            String type = typeRaw.substring(0, hashPos).trim();
            if (type.startsWith("fully-qualified")) {
                if (original.contains("♂")) {
                    int debug = 0;
                }
                allRgi.add(original);
                final String variantsRemoved = removeVariants(original);
                allRgiNoES.add(variantsRemoved);
                if (!original.equals(variantsRemoved)) {
                    restoreVariants.put(variantsRemoved, original);
                }
                if (!SKIN_MODIFIERS.containsSome(original)) {
                    String neutral = NEUTER.transform(original);
                    if (!neutral.equals(original)) {
                        neutralAndGenderedToNeutral.put(original, neutral);
                        neutralAndGenderedToNeutral.put(neutral, neutral);
                    }
                }
            }
            emojiToMajorCategory.put(original, majorCategory);
            emojiToMinorCategory.put(original, minorCategory);
            String comment = typeRaw.substring(hashPos + 1);
            if (!commentMatcher.reset(comment).matches()) {
                throw new IllegalArgumentException("unexpected comment format");
            }
            String name = commentMatcher.group(1);
            // The comment is now of the form:  # 😁 E0.6 beaming face with smiling eyes
            // int spacePos = comment.indexOf(' ');
            // The format changed in v15.1, so there is no version number.
            // Thus the following is commented out:
            // spacePos = comment.indexOf(' ', spacePos + 1); // get second space
            // String name = comment.substring(spacePos + 1).trim();

            toName.put(original, name);

            // add all the non-constructed values to a set for annotations

            String minimal = original.replace(EMOJI_VARIANT, "");

            // Add the order. If it is not minimal, add that also.
            if (!emojiToOrder.containsKey(original)) {
                putUnique(emojiToOrder, original, emojiToOrder.size() * 100L);
            }
            if (!emojiToOrder.containsKey(minimal)) {
                putUnique(emojiToOrder, minimal, emojiToOrder.size() * 100L);
            }

            boolean singleton = CharSequences.getSingleCodePoint(minimal) != Integer.MAX_VALUE;

            // skip constructed values
            if (minimal.contains(COMBINING_ENCLOSING_KEYCAP)
                    || REGIONAL_INDICATORS.containsSome(minimal)
                    || TAGS.containsSome(minimal)
                    || !singleton && SKIN_MODIFIERS.containsSome(minimal)
                    || !singleton && FAMILY.containsAll(minimal)) {
                // do nothing
            } else if (minimal.contains(ZWJ)) { // only do certain ZWJ sequences
                if (SPECIALS.contains(minimal)
                        || GENDER.containsSome(minimal)
                        || MAN_WOMAN.contains(minimal.codePointAt(0))
                                && OBJECT.contains(minimal.codePointBefore(minimal.length()))) {
                    nonConstructed.add(minimal);
                }
            } else if (!minimal.contains("🔟")) {
                nonConstructed.add(minimal);
            }
        }
        emojiToMajorCategory.freeze();
        emojiToMinorCategory.freeze();
        nonConstructed.add(SKIN_MODIFIERS); // needed for names
        nonConstructed.freeze();
        toName.freeze();
        allRgi.freeze();
        allRgiNoES.addAll(SKIN_MODIFIERS).addAll(HAIR_MODIFIERS).freeze();
        // hack
        for (String s :
                new UnicodeSet(
                        "[#*0-9©®‼⁉™ℹ↔-↙↩↪⌨⏏⏭-⏯ ⏱⏲⏸-⏺Ⓜ▪▫▶◀◻◼☀-☄☎☑☘☝☠☢ ☣☦☪☮☯☸-☺♀♂♟♠♣♥♦♨♻♾⚒⚔-⚗ ⚙⚛⚜⚠⚧⚰⚱⛈⛏⛑⛓⛩⛰⛱⛴⛷-⛹✂"
                                + "✈✉ ✌✍✏✒✔✖✝✡✳✴❄❇❣❤➡⤴⤵⬅-⬇〰 〽㊗㊙🅰🅱🅾🅿🈂🈷🌡🌤-🌬🌶🍽🎖🎗🎙-🎛🎞 🎟🏋-🏎🏔-🏟🏳🏵🏷🐿👁📽🕉"
                                + "🕊🕯🕰🕳-🕹🖇 🖊-🖍🖐🖥🖨🖱🖲🖼🗂-🗄🗑-🗓🗜-🗞🗡🗣🗨 🗯🗳🗺🛋🛍-🛏🛠-🛥🛩🛰🛳]")) {
            restoreVariants.put(s, s + Emoji.EMOJI_VARIANT);
        }
        restoreVariants.freeze();
        Multimap<String, String> neutralToOthers = TreeMultimap.create(COLLATOR, COLLATOR);
        Multimaps.invertFrom(Multimaps.forMap(neutralAndGenderedToNeutral), neutralToOthers);
        Set<Set<String>> toGenderGroup = new LinkedHashSet<>();
        for (Collection<String> set : neutralToOthers.asMap().values()) {
            TreeSet<String> s = new TreeSet<>(COLLATOR);
            s.addAll(set);
            toGenderGroup.add(ImmutableSet.copyOf(s));
        }
        genderSets = CldrUtility.protectCollection(toGenderGroup);
    }

    public static String removeVariants(String original) {
        return original.replace(Emoji.EMOJI_VARIANT, "");
    }

    public static Set<Set<String>> getGenderGroups() {
        return genderSets;
    }

    public static final String restoreVariants(String source) {
        String restored = restoreVariants.get(source);
        if (restored != null) {
            int debug = 0;
        }
        return restored == null ? source : restored;
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
        V oldValue = map.put(key, value);
        if (oldValue != null) {
            throw new ICUException(
                    "Attempt to change value of "
                            + map
                            + " for "
                            + key
                            + " from "
                            + oldValue
                            + " to "
                            + value);
        }
    }

    public static UnicodeSet getAllRgi() {
        return allRgi;
    }

    public static UnicodeSet getAllRgiNoES() {
        return allRgiNoES;
    }

    public static final UnicodeMap<String> EXTRA_SYMBOL_MINOR_CATEGORIES = new UnicodeMap<>();
    public static final Map<String, Long> EXTRA_SYMBOL_ORDER;
    private static final boolean DEBUG = false;

    static {
        String[][] data = {
            {"arrow", "→ ↓ ↑ ← ↔ ↕ ⇆ ⇅"},
            {"alphanum", "© ® ℗ ™ µ"},
            {"geometric", "▼ ▶ ▲ ◀ ● ○ ◯ ◊"},
            {"math", "× ÷ √ ∞ ∆ ∇ ⁻ ¹ ² ³ ≡ ∈ ⊂ ∩ ∪ ° + ± − = ≈ ≠ > < ≤ ≥ ¬ | ~"},
            {
                "punctuation",
                "§ † ‡ \\u0020  , 、 ، ; : ؛ ! ¡ ? ¿ ؟ ¶ ※ / \\ & # % ‰ ′ ″ ‴ @ * ♪ ♭ ♯ ` ´ ^ ¨ ‐ ― _ - – — • · . … 。 ‧ ・ ‘ ’ ‚ ' “ ” „ » « ( ) [ ] { } 〔 〕 〈 〉 《 》 「 」 『 』 〖 〗 【 】"
            },
            {"currency", "€ £ ¥ ₹ ₽ $ ¢ ฿ ₪ ₺ ₫ ₱ ₩ ₡ ₦ ₮ ৳ ₴ ₸ ₲ ₵ ៛ ₭ ֏ ₥ ₾ ₼ ₿ ؋ ₧ ¤"},
            {
                "other-symbol",
                "‾‽‸⁂↚↛↮↙↜↝↞↟↠↡↢↣↤↥↦↧↨↫↬↭↯↰↱↲↳↴↵↶↷↸↹↺↻↼↽↾↿⇀⇁⇂⇃⇄⇇⇈⇉⇊⇋⇌⇐⇍⇑⇒⇏⇓⇔⇎⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪⇵∀∂∃∅∉∋∎∏∑≮≯∓∕⁄∗∘∙∝∟∠∣∥∧∫∬∮∴∵∶∷∼∽∾≃≅≌≒≖≣≦≧≪≫≬≳≺≻⊁⊃⊆⊇⊕⊖⊗⊘⊙⊚⊛⊞⊟⊥⊮⊰⊱⋭⊶⊹⊿⋁⋂⋃⋅⋆⋈⋒⋘⋙⋮⋯⋰⋱■□▢▣▤▥▦▧▨▩▬▭▮▰△▴▵▷▸▹►▻▽▾▿◁◂◃◄◅◆◇◈◉◌◍◎◐◑◒◓◔◕◖◗◘◙◜◝◞◟◠◡◢◣◤◥◦◳◷◻◽◿⨧⨯⨼⩣⩽⪍⪚⪺₢₣₤₰₳₶₷₨﷼"
            },
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
        Map<String, Long> _EXTRA_SYMBOL_ORDER = new LinkedHashMap<>();
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
                throw new IllegalArgumentException(
                        "Duplicate values in " + EXTRA_SYMBOL_MINOR_CATEGORIES);
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
                throw new InternalCldrException(
                        "No minor category (aka subgroup) found for "
                                + emoji
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
                throw new InternalCldrException(
                        "No Order found for "
                                + emoji
                                + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"minor\", ...");
            }
        }
        return result;
    }

    public static long getEmojiMinorOrder(String minor) {
        Long result = minorToOrder.get(minor);
        if (result == null) {
            throw new InternalCldrException(
                    "No minor category (aka subgroup) found for "
                            + minor
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
                throw new InternalCldrException(
                        "No minor category (aka subgroup) found for "
                                + emoji
                                + ". Update emoji-test.txt to latest, and adjust PathHeader.. functionMap.put(\"major\", ...");
            }
        }
        return majorCat;
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
    public static final String TYPE_TTS = "[@type=\"tts\"]";

    public static synchronized Set<String> getNamePaths() {
        return NAME_PATHS != null ? NAME_PATHS : (NAME_PATHS = buildPaths(TYPE_TTS));
    }

    private static ImmutableSet<String> buildPaths(String suffix) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String s : Emoji.getNonConstructed()) {
            String base = "//ldml/annotations/annotation[@cp=\"" + s + "\"]" + suffix;
            builder.add(base);
        }
        return builder.build();
    }

    /**
     * Return the PageId for the given emoji, making adjustments for pages that are united in
     * emoji-test.txt but divided in Survey Tool, such as Symbols, Symbols2, and Symbols3
     *
     * @param emoji the emoji as a string
     * @return the adjusted PageId
     */
    public static PageId getPageId(String emoji) {
        final String major = getMajorCategory(emoji);
        final String minor = getMinorCategory(emoji);
        final PageId pageId = PageId.forString(major);
        final Long minorOrder = minorToOrder.get(minor);
        switch (pageId) {
            case Objects:
                return (minorOrder < minorToOrder.get("money")) ? PageId.Objects : PageId.Objects2;
            case People:
                return (minorOrder < minorToOrder.get("person-fantasy"))
                        ? PageId.People
                        : PageId.People2;
            case Symbols:
                return (minorOrder < minorToOrder.get("transport-sign"))
                        ? PageId.Symbols
                        : PageId.EmojiSymbols;
            case Travel_Places:
                return (minorOrder < minorToOrder.get("transport-ground"))
                        ? PageId.Travel_Places
                        : PageId.Travel_Places2;
            default:
                return pageId;
        }
    }
}
