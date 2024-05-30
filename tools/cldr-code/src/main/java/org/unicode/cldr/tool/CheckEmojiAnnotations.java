package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.XPathParts;

public class CheckEmojiAnnotations {
    private static final Joiner JOIN_BAR = Joiner.on(" | ");

    public static void main(String[] args) {
        boolean chooseEmoji = true; // false to get the non-emoji

        UnicodeSet rgi = Emoji.getAllRgi();
        UnicodeSet rgiNoVariant = Emoji.getAllRgiNoES();
        CLDRFile root = CLDRConfig.getInstance().getAnnotationsFactory().make("en", false);
        UnicodeSet rootEmoji = new UnicodeSet();
        for (String path : root) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String cp = parts.getAttributeValue(-1, "cp");
            if (cp != null && rgiNoVariant.contains(cp) == chooseEmoji) {
                rootEmoji.add(cp);
            }
        }
        rootEmoji.freeze();

        UnicodeMap<Annotations> english = Annotations.getData("en");
        Map<String, UnicodeSet> keywordToEmoji = new TreeMap<>();
        UnicodeSet allUnclean = new UnicodeSet();

        for (Annotations entry : english.values()) {
            Set<String> keywords = entry.getKeywords();
            UnicodeSet emoji = english.getSet(entry);
            emoji.retainAll(rootEmoji);
            UnicodeSet emojiRestored = new UnicodeSet();
            for (String emojiItem : emoji) {
                emojiRestored.add(Emoji.restoreVariants(emojiItem));
            }
            UnicodeSet unclean = new UnicodeSet(emojiRestored).removeAll(rgi);
            allUnclean.add(unclean);

            emojiRestored = emojiRestored.retainAll(rgi);
            if (emojiRestored.isEmpty()) {
                continue;
            }

            for (String keyword : keywords) {
                UnicodeSet value = keywordToEmoji.get(keyword);
                if (value == null) {
                    keywordToEmoji.put(keyword, value = new UnicodeSet());
                }
                value.addAll(emojiRestored);
            }
        }
        CldrUtility.protectCollection(keywordToEmoji);

        int count = 0;
        System.out.println("### Emoji to Keywords");
        TreeSet<String> sortedRootEmoji = new TreeSet<>(Emoji.COLLATOR);
        rootEmoji.addAllTo(sortedRootEmoji);
        for (String emoji : sortedRootEmoji) {
            String restored = Emoji.restoreVariants(emoji);
            Set<String> keywords = english.get(emoji).getKeywords();
            System.out.println(
                    ++count + "\t" + restored + "\t" + emoji + "\t" + JOIN_BAR.join(keywords));
        }

        UnicodeSet toEscape =
                new UnicodeSet(CodePointEscaper.FORCE_ESCAPE)
                        .remove(CodePointEscaper.ZWJ.getCodePoint())
                        .remove(CodePointEscaper.RANGE.getCodePoint())
                        .freeze();
        SimpleUnicodeSetFormatter suf = new SimpleUnicodeSetFormatter(null, toEscape);

        allUnclean =
                allUnclean
                        .retainAll(rgiNoVariant)
                        .removeAll(Emoji.SKIN_MODIFIERS)
                        .removeAll(Emoji.HAIR_MODIFIERS);
        if (!allUnclean.isEmpty()) {
            throw new IllegalArgumentException("Missing " + suf.format(allUnclean));
        }

        System.out.println("### Keywords to Emoji");

        count = 0;
        for (Entry<String, UnicodeSet> entry : keywordToEmoji.entrySet()) {
            System.out.println(
                    ++count + "\t" + entry.getKey() + "\t" + suf.format(entry.getValue()));
        }

        System.out.println("### Gender Variants");

        for (Set<String> entry : Emoji.getGenderGroups()) {
            // find common keywords
            Set<String> common = null;
            Set<String> cleanEntry = new TreeSet<>();
            for (String s : entry) {
                if (!rootEmoji.contains(Emoji.removeVariants(s))) {
                    continue;
                }
                Annotations anno = getAnnotations(english, s);
                if (anno == null) {
                    continue;
                }
                cleanEntry.add(s);
                if (common == null) {
                    System.out.println();
                    common = new TreeSet<>();
                    common.addAll(anno.getKeywords());
                } else {
                    common.retainAll(anno.getKeywords());
                }
            }
            // now show them
            if (cleanEntry.size() > 1) {
                for (String s : cleanEntry) {
                    Annotations anno = getAnnotations(english, s);
                    String removed = Emoji.removeVariants(s);
                    System.out.println(
                            s
                                    + "\t"
                                    + removed
                                    + "\t"
                                    + anno.getShortName()
                                    + "\t"
                                    + JOIN_BAR.join(common)
                                    + "\t"
                                    + JOIN_BAR.join(Sets.difference(anno.getKeywords(), common)));
                }
            }
        }
    }

    public static Annotations getAnnotations(UnicodeMap<Annotations> english, String s) {
        Annotations anno = english.get(s);
        if (anno == null) {
            anno = english.get(s.replace(Emoji.EMOJI_VARIANT, ""));
        }
        return anno;
    }
}
