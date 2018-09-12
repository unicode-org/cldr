package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.dev.util.CollectionUtilities;

public class CompareEn {
    public static void main(String[] args) throws IOException {
        Factory mainFactory = CLDRConfig.getInstance().getCldrFactory();
        Factory annotationsFactory = CLDRConfig.getInstance().getAnnotationsFactory();
        try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "comparison", "en.txt")) {
            out.println("Path\ten\ten_001\ten_GB\tProposed Disposition");
            for (Factory factory : Arrays.asList(mainFactory, annotationsFactory)) {
                CLDRFile en = factory.make("en", false);
                CLDRFile en_001 = factory.make("en_001", false);
                CLDRFile en_GB = factory.make("en_GB", false);

                // walk through all the new paths and values to check them.

                Set<String> paths = CollectionUtilities.addAll(en_GB.iterator(), new TreeSet<>());
                for (String path : paths) {
                    if (path.startsWith("//ldml/identity")) {
                        continue;
                    }
                    String value001 = en_001.getStringValue(path);
                    String valueGB = en_GB.getStringValue(path);
                    String value = en.getStringValue(path);

                    // drop the cases that will disappear with minimization

                    if (valueGB == null
                        || Objects.equals(value001, valueGB)
                        || (value001 == null && Objects.equals(value, valueGB))) {
                        continue;
                    }
                    out.println(path
                        + "\t" + value
                        + "\t" + value001
                        + "\t" + valueGB);
                }
            }
        }
    }
}
