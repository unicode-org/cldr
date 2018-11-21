package org.unicode.cldr.tool;

import java.io.File;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;

public class RemoveEmptyCldrFiles {
    public static void main(String[] args) {
        File[] paths = { 
            new File(CLDRPaths.ANNOTATIONS_DIRECTORY)
        };
        Factory factory = SimpleFactory.make(paths, ".*");
        for (String localeId : factory.getAvailable()) {
            CLDRFile cldrFile = factory.make(localeId, false);
            File file = new File(CLDRPaths.ANNOTATIONS_DIRECTORY, localeId + ".xml");
            if (isEmpty(cldrFile)) {
                if (!file.exists()) {
                    throw new IllegalArgumentException("Missing file: " + file);
                }
                System.out.println("Deleting: " + file);
                file.delete();
            } else {
                System.out.println("Retaining: " + file);
            }
        }
    }

    private static boolean isEmpty(CLDRFile cldrFile) {
        for (String path : cldrFile) {
            if (!path.contains("/identity")) {
                return false;
            }
        }
        return true;
    }
}
