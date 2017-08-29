package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;

public class RemoveEmptyCLDR {
    public static void main(String[] args) throws IOException {
        // eg /Users/markdavis/Google Drive/workspace/Generated/vxml/common/annotations
        String dir = args[0];
        main:
            for (File f : new File(dir).listFiles()) {
                List<Pair<String, String>> data = new ArrayList<>();
                String canonicalPath = f.getCanonicalPath();
                XMLFileReader.loadPathValues(canonicalPath, data, false);
                for (Pair<String, String> item : data) {
                    if (item.getFirst().contains("/identity")) {
                        continue;
                    }
                    continue main;
                }
                f.delete();
            }
    }
}
