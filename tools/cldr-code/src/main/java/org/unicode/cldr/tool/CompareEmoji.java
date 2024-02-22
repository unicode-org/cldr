package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ibm.icu.text.Collator;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XPathParts;

public class CompareEmoji {
    private static final Splitter BAR_SPLITTER = Splitter.on("|").trimResults().omitEmptyStrings();
    static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final Factory FACTORY = CONFIG.getAnnotationsFactory();
    private static final File[] paths = {new File(CLDRPaths.ANNOTATIONS_DERIVED_DIRECTORY)};
    static final Factory FACTORY_DERIVED = SimpleFactory.make(paths, ".*");

    private static final Joiner BAR_JOINER = Joiner.on(" | ");
    private static final Collator collator = CLDRConfig.getInstance().getCollator();
    private static final String base =
            "/Users/markdavis/github/private/DATA/cldr-private/emoji_diff/";
    private static final Set<String> sorted =
            ImmutableSet.copyOf(Emoji.getAllRgi().addAllTo(new TreeSet<>(collator)));

    enum Status {
        regular,
        constructed,
        missing;

        char abbreviation() {
            return Character.toUpperCase(name().charAt(0));
        }
    }

    private static class EmojiData {
        String shortName;
        Set<String> searchKeywords;
        Status status;

        @Override
        public String toString() {
            return shortName + "; " + searchKeywords + "; " + status;
        }
    }

    public static void main(String[] args) throws IOException {
        final String locale = "zh_Hant";

        Map<String, EmojiData> annotations = getDataFor(locale);

        Map<String, Set<String>> removed = loadItems(locale, "_removed.csv", new HashMap<>());
        Map<String, Set<String>> added = loadItems(locale, "_added.csv", new HashMap<>());

        int count = 0;
        System.out.println("No.\tEmoji\tType\tName\tCommon\tRemoved\tAdded");
        for (String key : sorted) {
            String minimal = key.replace(Emoji.EMOJI_VARIANT, "");
            EmojiData v = annotations.get(minimal);
            Set<String> commonSet;
            String shortName;
            Status status;
            if (v == null) {
                commonSet = Set.of();
                shortName = "<constructed>";
                status = Status.missing;
            } else {
                commonSet = v.searchKeywords;
                shortName = v.shortName;
                status = v.status;
            }

            Set<String> removedSet = removed.get(key);
            Set<String> addedSet = added.get(key);
            if (removedSet == null && addedSet == null) {
                continue;
            }
            if (removedSet != null) {
                commonSet = Sets.difference(commonSet, removedSet);
            }
            System.out.println(
                    ++count //
                            + "\t"
                            + key //
                            + "\t"
                            + status.abbreviation() //
                            + "\t"
                            + shortName //
                            + "\t"
                            + BAR_JOINER.join(commonSet) //
                            + "\t"
                            + (removedSet == null ? "" : BAR_JOINER.join(removedSet)) //
                            + "\t"
                            + (addedSet == null ? "" : BAR_JOINER.join(addedSet)) //
                    );
        }
    }

    private static Map<String, EmojiData> getDataFor(String locale) {
        Map<String, EmojiData> result = new HashMap<>();
        CLDRFile cldrfile = FACTORY.make(locale, true);
        getDataIn(cldrfile, result, Status.regular);
        CLDRFile cldrfileDerived = FACTORY_DERIVED.make(locale, true);
        getDataIn(cldrfileDerived, result, Status.constructed);
        return result;
    }

    public static void getDataIn(CLDRFile cldrfile, Map<String, EmojiData> result, Status status) {
        for (String path : cldrfile) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String cp = parts.getAttributeValue(-1, "cp");
            if (cp == null) {
                continue;
            }
            EmojiData record = result.get(cp);
            if (record == null) {
                result.put(cp, record = new EmojiData());
                record.status = status;
            }
            boolean istts = parts.getAttributeValue(-1, "type") != null;
            String value = cldrfile.getStringValue(path);
            if (istts) {
                record.shortName = value;
            } else {
                record.searchKeywords = ImmutableSet.copyOf(BAR_SPLITTER.splitToList(value));
            }
        }
    }

    public static Map<String, Set<String>> loadItems(
            String locale, String suffix, Map<String, Set<String>> result) throws IOException {
        try (BufferedReader reader = FileUtilities.openUTF8Reader(base, locale + suffix)) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return result;
                }
                if (line.startsWith("Emoji,")) {
                    continue;
                }
                String[] split = FileUtilities.splitCommaSeparated(line);
                if (split.length < 2) {
                    continue;
                }
                String key = split[0];
                Set<String> values = new TreeSet<>(collator);
                for (int i = 1; i < split.length; ++i) {
                    values.add(split[i]);
                }
                values = ImmutableSet.copyOf(values);
                result.put(key, values);
            }
        }
    }
}
