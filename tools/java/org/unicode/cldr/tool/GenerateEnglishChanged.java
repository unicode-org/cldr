package org.unicode.cldr.tool;

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

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.With;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.UnicodeSet;

public class GenerateEnglishChanged {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final File TRUNK_DIRECTORY = new File(CLDRPaths.BASE_DIRECTORY);
// TODO 
    private static final File RELEASE_DIRECTORY = new File(CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + ToolConstants.LAST_RELEASE_VERSION + ".0" + "/");
private static final boolean TRIAL = false;

    public static void main(String[] args) {
        String[] base = { "common" };
        File[] addStandardSubdirectories = CLDR_CONFIG.addStandardSubdirectories(
            CLDR_CONFIG.fileArrayFromStringArray(
                TRUNK_DIRECTORY, base));

        Factory factoryTrunk = SimpleFactory.make(addStandardSubdirectories, ".*");
        CLDRFile englishTrunk = factoryTrunk.make("en", true);

        addStandardSubdirectories = CLDR_CONFIG.addStandardSubdirectories(
            CLDR_CONFIG.fileArrayFromStringArray(
                RELEASE_DIRECTORY, base));

        Factory factoryLastRelease = SimpleFactory.make(addStandardSubdirectories, ".*");
        CLDRFile englishLastRelease = factoryLastRelease.make("en", true);

        Set<String> paths = new TreeSet<>();
        With.in(englishTrunk).toCollection(paths);
        With.in(englishLastRelease).toCollection(paths);
        PathStarrer starrer = new PathStarrer();
        final String placeholder = "×";
        starrer.setSubstitutionPattern(placeholder);

        Set<String> abbreviatedPaths = new LinkedHashSet<>();
        Multimap<String,List<String>> pathsDiffer = LinkedHashMultimap.create();
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
                String starred = starrer.set(abbrPath);
                pathsDiffer.put(starred, ImmutableList.copyOf(starrer.getAttributes()));
                //System.out.println(path + " => " + abbrPath);
            }
        }

        Matcher matcher = CheckCLDR.Phase.ALLOWED_IN_LIMITED_PATHS.matcher("");
        int errorCount = 0;
        for (String path : abbreviatedPaths) {
            if (!matcher.reset(path).lookingAt()) {
                System.out.println("Failed to match: " + path);
                errorCount++;
            }
        }
        System.out.println("Errors: " + errorCount);
        
        if (TRIAL) {
            String multipath = "(";

            for (Entry<String, Collection<List<String>>> entry : pathsDiffer.asMap().entrySet()) {
                String path = entry.getKey();
                ArrayList<Set<String>> store = null;
                for (List<String> list : entry.getValue()) {
                    // prepare the data
                    if (store == null) {
                        store = new ArrayList<Set<String>>();
                        for (int i = 0; i < list.size(); ++i) {
                            store.add(new LinkedHashSet<String>());
                        }
                    }
                    for (int i = 0; i < list.size(); ++i) {
                        store.get(i).add(list.get(i));
                    }
                }
                System.out.println(path + "\t" + store);
                path = path.replace("[", "\\[");

                for (int i = 0; i < store.size(); ++i) {
                    Set<String> attrValues = store.get(i);
                    UnicodeSet alphabet = new UnicodeSet();
                    for (String attrValue : attrValues) {
                        alphabet.addAll(attrValue);
                    }
                    //System.out.println(alphabet.toPattern(false));
                    String compressed = MinimizeRegex.compressWith(attrValues, alphabet);// (attrValues, alphabet);
                    //String compressed = MinimizeRegex.simplePattern(attrValues);// (attrValues, alphabet);
                    path = path.replaceFirst(placeholder, "(" + compressed + ")");
                }                
                multipath += "|" + path;


                System.out.println(path);
            }
            multipath += ")";
            Pattern pathPattern = Pattern.compile(multipath);
        }
        //System.out.println(compressed);
    }

    static Matcher partToRemove = Pattern.compile("("
        + "\\[@type=\"tts\"]"
        + "|/listPatternPart\\[@type=\"[^\"]*\"]"
        + "|/displayName"
        + "|/unitPattern\\[@count=\"[^\"]*\"]"
        + ")$").matcher("");

    private static String abbreviatePath(String path) {
        String result = partToRemove.reset(path).replaceAll("");
        return result;
    }
}
