package org.unicode.cldr.tool;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.TempPrintWriter;

@CLDRTool(
    alias="write-extra-paths",
    hidden = "true",
    description = "call ExtraPaths to extract the extra paths for a locale"
)
public class WriteExtraPaths {
    public static void main(String args[]) {
        if (args.length < 1) throw new IllegalArgumentException(String.format("Usage: %s <localeID> ...", WriteExtraPaths.class.getSimpleName()));
        final File outDir = new File(CLDRPaths.GEN_DIRECTORY, "extraPaths");
        if(outDir.mkdirs()) {
            System.out.println("# mkdir: " + outDir);
        }
        for(final String localeId : args) {
            System.out.println("# Locale: " + localeId);
            final Set<String> extraPaths = CLDRConfig.getInstance().getCLDRFile(localeId, false).getRawExtraPaths();
            final TreeSet<String> sortedPaths = new TreeSet<>(extraPaths);
            final File outFile = new File(outDir, localeId+".txt");
            try (TempPrintWriter pw = new TempPrintWriter(outFile)) {
                for(final String s : sortedPaths) {
                    pw.println(s);
                }
            } finally {
                System.out.println("# Wrote: " + outFile);
            }
        }
    }
}
