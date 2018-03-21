package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;

public class RemoveEmptyCLDR {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[] { "annotations", "annotationsDerived" };
        }
        // eg /Users/markdavis/Google Drive/workspace/Generated/vxml/common/annotations
        for (String dir : args) {
            main: for (File f : new File(CLDRPaths.COMMON_DIRECTORY + dir).listFiles()) {
                List<Pair<String, String>> data = new ArrayList<>();
                String canonicalPath = f.getCanonicalPath();
                if (!canonicalPath.endsWith(".xml") || canonicalPath.endsWith("root.xml")) {
                    continue;
                }
                XMLFileReader.loadPathValues(canonicalPath, data, false);
                for (Pair<String, String> item : data) {
                    if (item.getFirst().contains("/identity")) {
                        continue;
                    }
                    System.out.println("Skipping: " + canonicalPath);
                    continue main;
                }
                System.out.println("Deleting: " + canonicalPath);
                f.delete();
            }
        }
    }
}
