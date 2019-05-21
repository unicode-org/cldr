package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class RemoveEmptyCLDR {

    static final boolean PREFLIGHT = false;

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            args = new String[] { "annotations", "annotationsDerived" };
        }
        Set<String> nonEmpty = new HashSet<>();
        BiMap<String, File> toDelete = HashBiMap.create();
        int counter = 0;
        // eg /Users/markdavis/Google Drive/workspace/Generated/vxml/common/annotations
        for (String dirCommonSeed : Arrays.asList(CLDRPaths.SEED_DIRECTORY1, CLDRPaths.COMMON_DIRECTORY)) {
            System.out.println("Checking: " + dirCommonSeed);
            for (String dir : args) {
                File dirFile = new File(dirCommonSeed + dir);
                if (!dirFile.exists()) {
                    continue;
                }
                main: for (File f : dirFile.listFiles()) {
                    List<Pair<String, String>> data = new ArrayList<>();
                    String canonicalPath = f.getCanonicalPath();
                    if (!canonicalPath.endsWith(".xml") || canonicalPath.endsWith("root.xml")) {
                        continue;
                    }
                    String name = f.getName();
                    name = name.substring(0,name.length()-4); // remove .xml
                    XMLFileReader.loadPathValues(canonicalPath, data, false);
                    for (Pair<String, String> item : data) {
                        if (item.getFirst().contains("/identity")) {
                            continue;
                        }
                        System.out.println(++counter + ") NOT-EMPTY: " + canonicalPath);
                        addNameAndParents(nonEmpty, name);
                        continue main;
                    }
                    toDelete.put(name, f);
                }
            }
        }
        counter = 0;
        // keep empty files that are needed for inheritance
        for (Entry<String, File> entry : toDelete.entrySet()) {
            String name = entry.getKey();
            if (nonEmpty.contains(name)) {
                continue;
            }
            File file = entry.getValue();
            System.out.println(++counter + ") Deleting: " + file.getCanonicalPath());
            if (!PREFLIGHT) {
                file.delete();
            }
        }
    }

    private static void addNameAndParents(Set<String> nonEmpty, String name) {
        nonEmpty.add(name);
        String parent = LocaleIDParser.getParent(name);
        if (!"root".equals(parent)) {
            addNameAndParents(nonEmpty, parent);
        }
    }
}
