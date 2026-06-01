package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRTreeWriter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;

@CLDRTool(
        alias = "copy-main-to-subdivisions",
        description = "Copy from common/main to common/subdivisions")
public class CopyMainToSubdivisions {

    static void copyCommonToSubdivisions() throws IOException {
        final CLDRConfig config = CLDRConfig.getInstance();
        final Factory subFactory = config.getSubdivisionFactory();
        final Factory mainFactory = config.getFullCldrFactory();
        final Set<CLDRLocale> subLocales = subFactory.getAvailableCLDRLocales();
        final Set<CLDRLocale> mainLocales = mainFactory.getAvailableCLDRLocales();
        System.out.println("# Copying from common/main to common/subdivisions");
        int totalChanges = 0;
        try (CLDRTreeWriter treeWriter = new CLDRTreeWriter(CLDRPaths.SUBDIVISIONS_DIRECTORY)) {
            for (final CLDRLocale l : mainLocales) {
                CLDRFile outSubFile = null;
                final CLDRFile mainF = mainFactory.make(l.getBaseName(), false);
                int changes = 0;
                if (subLocales.contains(l)) {
                    outSubFile = subFactory.make(l.getBaseName(), false).cloneAsThawed();
                } else {
                    // did not exist, add the locale
                    outSubFile = new CLDRFile(new SimpleXMLSource(l.getBaseName()));
                }

                for (Iterator<String> it =
                                mainF.iterator(
                                        "//ldml/localeDisplayNames/subdivisions/subdivision");
                        it.hasNext(); ) {
                    final String p = it.next();
                    final String commonValue = mainF.getStringValue(p);
                    final String subValue = outSubFile.getStringValue(p);
                    if (commonValue != null && mainF.isHere(p)) {
                        final String mainFullPath = mainF.getFullXPath(p);
                        final String subFullPath =
                                (subValue == null) ? null : outSubFile.getFullXPath(p);
                        if (subValue == null
                                || (!subValue.equals((commonValue)))
                                || !mainFullPath.equals(subFullPath)) {
                            // update it if the data OR the approval level changes.
                            changes++;
                            outSubFile.remove(
                                    p); // remove so that we make sure the full path is preserved
                            outSubFile.add(mainFullPath, commonValue);
                        }
                    }
                }
                if (changes > 0) {
                    treeWriter.write(outSubFile);
                    totalChanges += changes;
                }
            }
        }
        System.out.println(totalChanges + " total paths changed");
    }

    public static void main(String[] args) throws IOException {
        copyCommonToSubdivisions();
    }
}
