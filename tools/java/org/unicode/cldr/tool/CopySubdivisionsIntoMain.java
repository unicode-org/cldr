package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class CopySubdivisionsIntoMain {
    private static final String TARGET_DIR = CLDRPaths.MAIN_DIRECTORY; // CLDRPaths.GEN_DIRECTORY + "sub-main/";

    public static void main(String[] args) {
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        Set<String> target = ImmutableSet.of("gbeng", "gbsct", "gbwls");
        // TODO make target according to language/region in future
        
        for (String locale : SubdivisionNames.getAvailableLocales()) {
            SubdivisionNames sdn = new SubdivisionNames(locale);
            Set<String> keySet = sdn.keySet();
            if (Collections.disjoint(target, keySet)) {
                continue;
            }
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(locale, false);
            } catch (Exception e) {
                System.out.println("Not in main, skipping for now: " + locale);
                continue;
            }
            boolean added = false;
            for (String key : target) {
                String path = SubdivisionNames.getPathFromCode(key);
                // skip if no new value
                String name = sdn.get(key);
                if (name == null) {
                    continue;
                }
                // don't copy if present already
                String oldValue = cldrFile.getStringValue(path);
                if (oldValue != null) {
                    continue;
                }
                if (!added) {
                    cldrFile = cldrFile.cloneAsThawed();
                    added = true;
                }
                cldrFile.add(path, name);
                System.out.println("Adding " + locale + ": " + path + "\t=«" + name + "»");
            }
            if (added) {
                try (PrintWriter pw = FileUtilities.openUTF8Writer(TARGET_DIR, locale + ".xml")) {
                    cldrFile.write(pw);
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
        }
    }
}
