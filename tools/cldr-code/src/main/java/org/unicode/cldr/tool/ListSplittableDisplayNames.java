package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class ListSplittableDisplayNames {
    private static final Joiner JOIN_TAB = Joiner.on('\t');

    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();

    private static final Set<String> languageCores =
            ImmutableSortedSet.of(
                    "Albanian",
                    "Arabic",
                    "Aramaic",
                    "Chinese",
                    "Cree",
                    "Dari",
                    "Dutch",
                    "English",
                    "Finnish",
                    "Flemish",
                    "French",
                    "Frisian",
                    "German",
                    "Greek",
                    "Haida",
                    "Hindi",
                    "Hmong",
                    "Inuktitut",
                    "Irish",
                    "Kurdish",
                    "Mari",
                    "Ndebele",
                    "Newari",
                    "Norwegian",
                    "Ojibwa",
                    "Persian",
                    "Portuguese",
                    "Sami",
                    "Sardinian",
                    "Silesian",
                    "Sorbian",
                    "Sotho",
                    "Spanish",
                    "Swahili",
                    "Syriac",
                    "Tamazight",
                    "Tatar",
                    "Turkish",
                    "Tutchone");

    public static void main(String[] args) {
        showSplit("en", "//ldml/localeDisplayNames/languages");
        // showSplit("en", "//ldml/localeDisplayNames/territories/territory");
    }

    private static void showSplit(String locale, String pathPrefix) {
        Multimap<String, String> wordToPath = TreeMultimap.create();
        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        for (Iterator<String> it = cldrFile.iterator(pathPrefix); it.hasNext(); ) {
            String path = it.next();
            if (path.contains("[@alt")) {
                continue;
            }
            if (path.contains("_")) { // skip the compounds; we already have the information
                continue;
            }
            String fullPath = cldrFile.getFullXPath(path);
            String value = cldrFile.getStringValue(path);
            StreamSupport.stream(Splitter.on(' ').split(value).spliterator(), false)
                    .forEach(
                            x -> {
                                if (languageCores.contains(x)) {
                                    wordToPath.put(x, fullPath);
                                }
                            });
        }
        // whenever a word is from  two paths, spit it out
        wordToPath.asMap().entrySet().stream()
                .forEach(
                        x -> {
                            Collection<String> paths = x.getValue();
                            int valueCount = paths.size();
                            if (valueCount > 1) {
                                String word = x.getKey();
                                paths.stream()
                                        .forEach(
                                                path -> {
                                                    String stringValue =
                                                            cldrFile.getStringValue(path);
                                                    boolean same = word.equals(stringValue);
                                                    System.out.println(
                                                            JOIN_TAB.join(
                                                                    word,
                                                                    path,
                                                                    "<no change>",
                                                                    stringValue));
                                                    System.out.println(
                                                            JOIN_TAB.join(
                                                                    word,
                                                                    path,
                                                                    "alt='core'",
                                                                    same ? "^^^" : word));
                                                    String extResult =
                                                            same
                                                                    ? "<maybe add>"
                                                                    : remove(word, stringValue);
                                                    System.out.println(
                                                            JOIN_TAB.join(
                                                                    word,
                                                                    path,
                                                                    "menu='extension'",
                                                                    extResult));
                                                });
                            }
                        });
    }

    private static String remove(String word, String stringValue) {
        return stringValue.replace(word, "").trim().replace("  ", " ");
    }
}
