package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.unicode.cldr.util.CLDRPaths;

public class GenerateKeyboardCharts {

    public static void main(String args[]) throws IOException {
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }
        final File kbdDir = new File(CLDRPaths.BASE_DIRECTORY, "docs/charts/keyboard");
        if (!kbdDir.exists()) {
            throw new IOException("Keyboards root dir doesn't exist: " + kbdDir);
        }
        final File kbdStatic = new File(kbdDir, "static");
        final File kbdStaticData = new File(kbdDir, "static/data");
        if (!kbdStaticData.exists()) {
            System.err.println(
                    "ERROR: " + kbdStaticData + " does not exist. Keyboard charts weren't run.");
            System.err.println("See " + new File(kbdDir, "README.md") + " for help.");
            return;
        }
        final File staticTarg = new File(mainDir, "keyboard/static");
        if (staticTarg.mkdirs()) {
            System.err.println("Created: " + staticTarg);
        }
        System.out.println("Copying: " + kbdStatic + " to " + staticTarg);

        Files.copy(
                new File(kbdDir, "index.html").toPath(),
                new File(mainDir, "keyboard/index.html").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        final String kbdStaticPrefix = kbdStatic.getAbsolutePath();
        Files.walk(kbdStatic.toPath())
                .forEach(
                        path -> {
                            if (!path.toFile().isFile()) return;
                            path.getParent().toFile().mkdirs();

                            System.out.println(path.toFile().getAbsolutePath());
                            /** path from static prefix */
                            final String rel =
                                    path.toFile()
                                            .getAbsolutePath()
                                            .substring(kbdStaticPrefix.length());
                            try {
                                final Path out = new File(staticTarg, rel).toPath();
                                System.out.println(" -> " + out);
                                Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.err.println("Error copying " + path);
                                System.exit(1);
                            }
                        });
    }
}
