package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.unicode.cldr.util.CLDRPaths;

public class GenerateKeyboardCharts {

    static final String SUBDIR = "keyboards";
    static IOException copyErr = null;

    public static void main(String args[]) throws IOException {
        final boolean makeSymlink = (args.length > 0 && args[0].equals("-l"));
        if (makeSymlink) {
            System.err.println("-l: making symlinks");
        } else {
            System.err.println("(use -l to symlink)");
        }
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }
        final File kbdDir =
                new File(
                        CLDRPaths.BASE_DIRECTORY,
                        "tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/keyboard-charts");
        if (!kbdDir.exists()) {
            throw new IOException("Keyboards root dir doesn't exist: " + kbdDir);
        }
        final File staticTarg = new File(mainDir, SUBDIR + "/");
        final File staticDataTarg = new File(mainDir, SUBDIR + "/data");
        if (staticDataTarg.mkdirs()) {
            System.err.println("Created: " + staticDataTarg);
        }
        System.out.println("Copying: " + kbdDir + " to " + staticTarg);

        final String kbdStaticPrefix = kbdDir.getAbsolutePath();
        Files.walk(kbdDir.toPath())
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
                                if (!makeSymlink) {
                                    if (Files.isSymbolicLink(out)) {
                                        Files.delete(out);
                                    }
                                    Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                                } else {
                                    if (out.toFile().isFile()) {
                                        out.toFile().delete();
                                    }
                                    Files.createSymbolicLink(out, path);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.err.println("Error copying " + path);
                                // will overwrite if more than one-  but at least one will be
                                // thrown.
                                copyErr = new IOException("Error copying " + path, e);
                            }
                        });
        // rethrow any error
        if (copyErr != null) throw copyErr;
    }
}
