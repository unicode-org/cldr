package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.unicode.cldr.util.CLDRPaths;

public class ExtractDecimalInputs {

    static class Inputs {
        List<String> locales;
    }

    public static List<String> getAllLocales() throws IOException {
        List<String> results = new ArrayList<>();
        Path mainDirPath = Path.of(CLDRPaths.MAIN_DIRECTORY);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mainDirPath, "*.xml")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                // Remove .xml extension
                results.add(filename.substring(0, filename.length() - 4));
            }
        }
        Collections.sort(results);
        return results;
    }

    public static ImmutableSet<String> getCoreLocales() {
        return ImmutableSet.of("en_US", "fr", "de_CH", "ar", "hi", "bn", "zh", "ru", "ja");
    }


}
