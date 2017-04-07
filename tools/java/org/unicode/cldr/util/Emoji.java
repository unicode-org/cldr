package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;

public class Emoji {
    static final UnicodeMap<String> emojiToMajorCategory = new UnicodeMap<>();
    static final UnicodeMap<String> emojiToMinorCategory = new UnicodeMap<>();
    static final Map<String,Integer> minorToOrder = new HashMap<>();
    static {
        /*
            # group: Smileys & People
            # subgroup: face-positive
            1F600 ; fully-qualified     # 😀 grinning face
         */
        Splitter semi = Splitter.on(';').trimResults();
        String majorCategory = null;
        String minorCategory = null;
        for (String line : FileUtilities.in(Emoji.class, "data/emoji/emoji-test.txt")) {
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
                if (line.startsWith("group:")) {
                    majorCategory = line.substring("group:".length()).trim();
                } else if (line.startsWith("subgroup:")) {
                    minorCategory = line.substring("subgroup:".length()).trim();
                    if (!minorToOrder.containsKey(minorCategory)) {
                        minorToOrder.put(minorCategory, minorToOrder.size());
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
//            String type = it.next();
//            if (!type.startsWith("fully-qualified")) {
//                continue;
//            }
            String emoji = Utility.fromHex(emojiHex, 4, " ");
            emojiToMajorCategory.put(emoji, majorCategory);
            emojiToMinorCategory.put(emoji, minorCategory);
        }
        emojiToMajorCategory.freeze();
        emojiToMinorCategory.freeze();
    }
    
    public static String getMinorCategory(String emoji) {
        return CldrUtility.ifNull(emojiToMinorCategory.get(emoji),"Component");
    }
    
    public static int getMinorToOrder(String minor) {
        Integer result = minorToOrder.get(minor);
        return result == null ? Integer.MAX_VALUE : result;
    }
    
    public static String getMajorCategory(String emoji) {
        return CldrUtility.ifNull(emojiToMajorCategory.get(emoji),"Component");
    }
    
    public static Set<String> getMajorCategories() {
        return emojiToMajorCategory.values();
    }

    public static Set<String> getMinorCategories() {
        return emojiToMinorCategory.values();
    }

    public static void main(String[] args) {
        System.out.println(getMajorCategory("😀"));
        System.out.println(getMinorCategory("😀"));
        System.out.println(getMajorCategories());
        for (String minor : getMinorCategories()) {
            System.out.println(minor + "\t" + getMinorToOrder(minor));
        }
    }
}
