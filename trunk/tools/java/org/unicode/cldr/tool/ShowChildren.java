package org.unicode.cldr.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PrettyPath;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;

public class ShowChildren {

    public static void main(String[] args) {
        System.out.println("Arguments: " + CollectionUtilities.join(args, " "));

        long startTime = System.currentTimeMillis();

        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());

        Relation<String, String> parent2children = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : locales) {
            String parent = getParentWithoutRegion(ltp, locale);
            if (!parent.equals(locale)) {
                parent2children.put(parent, locale);
            }
        }
        PrettyPath prettyPath = new PrettyPath();

        final CLDRFile english = ToolConfig.getToolInstance().getEnglish();
        Counter<String> deviations = new Counter<String>();

        for (Entry<String, Set<String>> entry : parent2children.keyValuesSet()) {
            Map<String, Relation<String, String>> path2value2locales = new TreeMap<String, Relation<String, String>>();
            String parent = entry.getKey();
            String parentName = english.getName(parent);
            CLDRFile parentFile = (CLDRFile) cldrFactory.make(parent, true);

            Set<String> children = entry.getValue();
            for (String child : children) {
                CLDRFile file = (CLDRFile) cldrFactory.make(child, false);
                for (String path : file) {
                    if (path.startsWith("//ldml/identity")
                        || path.endsWith("/alias")
                        || path.endsWith("/commonlyUsed")) {
                        continue;
                    }
                    Relation<String, String> value2locales = path2value2locales.get(path);
                    if (value2locales == null) {
                        path2value2locales.put(path,
                            value2locales = Relation.of(new LinkedHashMap<String, Set<String>>(), TreeSet.class));
                    }
                    String parentValue = parentFile.getStringValue(path);
                    if (parentValue == null) {
                        parentValue = "*MISSING*";
                    }
                    String childValue = file.getStringValue(path);
                    if (parentValue.equals(childValue)) {
                        continue;
                    }
                    value2locales.put(parentValue, parent);
                    value2locales.put(childValue, child);
                }
            }
            if (path2value2locales.size() == 0) {
                continue;
            }
            for (Entry<String, Relation<String, String>> datum : path2value2locales.entrySet()) {
                String path = datum.getKey();
                String ppath = prettyPath.getPrettyPath(path, false);
                Relation<String, String> value2locales = datum.getValue();
                for (Entry<String, Set<String>> valueAndLocales : value2locales.keyValuesSet()) {
                    System.out.println(parentName + "\t" + parent + "\t〈" + valueAndLocales.getKey() + "〉\t"
                        + valueAndLocales.getValue() + "\t" + ppath);
                }
                System.out.println();
            }
            deviations.add(parent, path2value2locales.size());
        }
        for (String locale : deviations.getKeysetSortedByKey()) {
            String parentName = english.getName(locale);
            System.out.println(parentName + "\t" + locale + "\t" + deviations.get(locale));
        }

        System.out
            .println("Done -- Elapsed time: " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
    }

    private static String getParentWithoutRegion(LanguageTagParser ltp, String locale) {
        while (true) {
            if (ltp.set(locale).getRegion().isEmpty()) {
                return locale;
            }
            locale = LocaleIDParser.getParent(locale);
        }
    }
}