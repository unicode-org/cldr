package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ICUUncheckedIOException;

public class TestLdml2IcuCompleteness extends TestFmwk {

    private static final String CLDR_BASE = CLDRConfig.getInstance().getCldrBaseDirectory() + "/common";
    private static final String ICU_BASE = CLDRConfig.getInstance().getCldrBaseDirectory() + "/../icu4c/data/"; // "/Users/markdavis/workspace/icu4c/data";

    static Multimap<String, File> icuFiles = TreeMultimap.create();
    static Multimap<String, File> cldrFiles = TreeMultimap.create();

    static Set<String> SKIP_DIRS = ImmutableSet.of(
        "transforms", "translit", // temp
        "unidata",
        "sprep",
        "mappings");
    static Set<String> SKIP_FILES = ImmutableSet.of(
        "attributeValueValidity",
        "tzdbNames");

    static {
        fillMap(new File(ICU_BASE), ".txt", icuFiles);
//        for (Entry<String, Collection<File>> s : icuFiles.asMap().entrySet()) {
//            System.out.println(s.getKey() + "\t\t" + s.getValue());
//        }

        fillMap(new File(CLDR_BASE), ".xml", cldrFiles);
        for (Entry<String, Collection<File>> s : cldrFiles.asMap().entrySet()) {
            System.out.println(s.getKey() + "\t\t" + s.getValue());
        }
    }

    static void fillMap(File file, String suffix, Multimap<String, File> fileList) {
        try {
            for (File f : file.listFiles()) {
                String name = f.getName();
                if (f.isDirectory()) {
                    if (SKIP_DIRS.contains(name)) {
                        continue;
                    }
                    fillMap(f, suffix, fileList);
                } else {
                    if (!name.endsWith(suffix)) {
                        continue;
                    }
                    // hack
                    if (name.endsWith("dict.txt") || name.contains("readme")) {
                        continue;
                    }
                    name = name.substring(0, name.length() - suffix.length());
                    if (SKIP_FILES.contains(name)) {
                        continue;
                    }
                    fileList.put(name, f.getParentFile().getCanonicalFile());
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        new TestLdml2IcuCompleteness().run(args);
    }

    public void TestFiles() {
        verifySuperset("cldrFiles-icuFiles", cldrFiles.keySet(), icuFiles.keySet());
        verifySuperset("icuFiles-cldrFiles", icuFiles.keySet(), cldrFiles.keySet());
    }

    private void verifySuperset(String title, Set<String> set1, Set<String> set2) {
        LinkedHashSet<String> diff = new LinkedHashSet<>(set1);
        diff.removeAll(set2);
        assertEquals(title, Collections.EMPTY_SET, diff);
    }

    public void TestLocales() {
        for (Entry<String, Collection<File>> entry : cldrFiles.asMap().entrySet()) {
            String file = entry.getKey();
            Set<String> values = collectCldrValues(file, entry.getValue());
        }
    }

    // TODO flesh out
    private Set<String> collectCldrValues(String file, Collection<File> value) {
        for (File f : value) {
            //org.unicode.cldr.util.XMLFileReader.loadPathValues(filename, data, validating)
        }
        return null;
    }
}
