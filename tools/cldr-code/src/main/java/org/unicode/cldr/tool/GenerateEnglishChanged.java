package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

public class GenerateEnglishChanged {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final File TRUNK_DIRECTORY = new File(CLDRPaths.BASE_DIRECTORY);
    // TODO
    private static final File RELEASE_DIRECTORY =
            new File(
                    CLDRPaths.ARCHIVE_DIRECTORY
                            + "cldr-"
                            + ToolConstants.LAST_RELEASE_VERSION
                            + "/");
    private static final boolean TRIAL = false;

    public static void main(String[] args) {
        String[] base = {"common"};
        File[] addStandardSubdirectories =
                CLDR_CONFIG.addStandardSubdirectories(
                        CLDRConfig.fileArrayFromStringArray(TRUNK_DIRECTORY, base));

        Factory factoryTrunk = SimpleFactory.make(addStandardSubdirectories, ".*");
        CLDRFile englishTrunk = factoryTrunk.make("en", true);

        addStandardSubdirectories =
                CLDR_CONFIG.addStandardSubdirectories(
                        CLDRConfig.fileArrayFromStringArray(RELEASE_DIRECTORY, base));

        Factory factoryLastRelease = SimpleFactory.make(addStandardSubdirectories, ".*");
        CLDRFile englishLastRelease = factoryLastRelease.make("en", true);

        Set<String> paths = new TreeSet<>();
        With.in(englishTrunk).toCollection(paths);
        With.in(englishLastRelease).toCollection(paths);
        final String placeholder = "×";

        Set<String> abbreviatedPaths = new LinkedHashSet<>();
        Multimap<String, List<String>> pathsDiffer = LinkedHashMultimap.create();
        for (String path : paths) {
            String valueTrunk = englishTrunk.getStringValue(path);
            if (valueTrunk == null) { // new, handled otherwise
                continue;
            }
            String valueLastRelease = englishLastRelease.getStringValue(path);
            if (valueLastRelease == null) { // missing, handled otherwise
                continue;
            }
            if (!valueTrunk.equals(valueLastRelease)) {
                String abbrPath = abbreviatePath(path);
                if (pathsDiffer.containsKey(abbrPath)) {
                    continue;
                }
                abbreviatedPaths.add(abbrPath);
                String starred = PathStarrer.getWithPattern(abbrPath, placeholder);
                pathsDiffer.put(
                        starred,
                        ImmutableList.copyOf(
                                XPathParts.getFrozenInstance(abbrPath).getAttributeValues()));
                // System.out.println(path + " => " + abbrPath);
            }
        }

        int errorCount = 0;
        for (String path : abbreviatedPaths) {
            if (!SubmissionLocales.pathAllowedInLimitedSubmission(path)) {
                System.out.println("Failed to match: " + path);
                errorCount++;
            }
        }
        System.out.println("Errors: " + errorCount);

        if (TRIAL) {

            for (Entry<String, Collection<List<String>>> entry : pathsDiffer.asMap().entrySet()) {
                String path = entry.getKey();
                ArrayList<Set<String>> store = null;
                for (List<String> list : entry.getValue()) {
                    // prepare the data
                    if (store == null) {
                        store = new ArrayList<>();
                        for (int i = 0; i < list.size(); ++i) {
                            store.add(new LinkedHashSet<>());
                        }
                    }
                    for (int i = 0; i < list.size(); ++i) {
                        store.get(i).add(list.get(i));
                    }
                }
                System.out.println(path + "\t" + store);
                path = path.replace("[", "\\[");
                if (store != null) {
                    for (Set<String> attrValues : store) {
                        UnicodeSet alphabet = new UnicodeSet();
                        for (String attrValue : attrValues) {
                            alphabet.addAll(attrValue);
                        }
                        // System.out.println(alphabet.toPattern(false));
                        String compressed =
                                MinimizeRegex.compressWith(
                                        attrValues, alphabet); // (attrValues, alphabet);
                        // String compressed = MinimizeRegex.simplePattern(attrValues);//
                        // (attrValues,
                        // alphabet);
                        path = path.replaceFirst(placeholder, "(" + compressed + ")");
                    }
                }
                System.out.println(path);
            }
        }
    }

    static Matcher partToRemove =
            Pattern.compile(
                            "("
                                    + "\\[@type=\"tts\"]"
                                    + "|/listPatternPart\\[@type=\"[^\"]*\"]"
                                    + "|/displayName"
                                    + "|/unitPattern\\[@count=\"[^\"]*\"]"
                                    + ")$")
                    .matcher("");

    private static String abbreviatePath(String path) {
        return partToRemove.reset(path).replaceAll("");
    }
}
