package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map.Entry;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRPaths;

public class TestFilenameCollisions extends TestFmwk {
    public static void main(String[] args) {
        new TestFilenameCollisions().run(args);
    }

    /**
     * Test for filename collisions. This include exact matches, but also cases that are easily
     * confusable:<br>
     * case-insensitive matches, and names with "test" either at the start or end.
     *
     * @throws IOException
     */
    public void testUniqueness() throws IOException {
        Path root = Path.of(CLDRPaths.BASE_DIRECTORY);
        Multimap<String, Path> mmap = TreeMultimap.create();
        Files.walk(root)
                .forEach(
                        x -> {
                            String name = x.getName(x.getNameCount() - 1).toString();
                            if (name.endsWith(".java")) {
                                name =
                                        name.substring(0, name.length() - 5)
                                                .toLowerCase(Locale.ROOT);
                                if (name.endsWith("test")) {
                                    name = "test" + name.substring(0, name.length() - 4);
                                }
                                if (!name.equals("testall")) {
                                    mmap.put(name, root.relativize(x));
                                }
                            }
                        });
        for (Entry<String, Collection<Path>> entry : mmap.asMap().entrySet()) {
            Collection<Path> filenames = entry.getValue();
            if (!assertEquals(
                    "files with same name-skeleton «" + entry.getKey() + "»",
                    1,
                    filenames.size())) {
                System.out.println(Joiner.on("\n").join(filenames));
            }
        }
    }
}
