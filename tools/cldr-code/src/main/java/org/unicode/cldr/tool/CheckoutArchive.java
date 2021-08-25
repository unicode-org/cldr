package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;

@CLDRTool(alias = "checkout-archive", description = "Checkout CLDR archive to $ARCHIVE (usually ../cldr-archive)")
public class CheckoutArchive {
    public static void main(String args[]) throws IOException, InterruptedException {
        Path archiveDir = new File(CLDRPaths.ARCHIVE_DIRECTORY).toPath();
        if (!archiveDir.toFile().isDirectory()) {
            throw new FileNotFoundException(
                "Archive directory " + archiveDir.toString() +
                " does not exist, please create it or change the value of -DARCHIVE=");
        }
        System.out.println("Setting up in $ARCHIVE " + archiveDir.toString() + " …");
        int skip = 0;
        int created = 0;
        int err = 0;
        for (final String ver : ToolConstants.CLDR_VERSIONS) {
            final Path dirName = archiveDir.resolve("cldr-" + ver);
            if (dirName.toFile().isDirectory()) {
                skip++;
                System.out.println("# Skipping existing \t" + dirName.toString());
            } else {
                final String tag = "release-" + ver.replaceAll("\\.", "-").replaceAll("-0$", "");
                final String cmd[] = {
                    "git",
                    "worktree",
                    "add",
                    dirName.toString(),
                    tag
                };
                System.out.println("# " + String.join(" ", cmd));
                int ev = new ProcessBuilder(cmd)
                    .directory(new File(CLDRPaths.BASE_DIRECTORY))
                    .inheritIO()
                    .start()
                    .waitFor();
                if (ev != 0) {
                    err++;
                    System.err.println("Error: exit value " + ev);
                }
            }
        }
        System.out.println(String.format("Created %d and skipped %d version(s)", created, skip));
        if (err != 0) {
            throw new RuntimeException("Total errors: " + err);
        }
    }
}
