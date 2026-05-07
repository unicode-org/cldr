package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import org.unicode.cldr.util.CLDRPaths;

public class GenerateKeyboardCharts {

    static final String SUBDIR = "keyboards";
    private static final String PATH_TO_SUBDIR = "docs/charts/" + SUBDIR;
    // subdir from CLDR_DIR to a possible node install
    private static final String NODE_SUBDIR = "tools/cldr-apps/js/node";
    static IOException copyErr = null;

    public static void main(String args[]) throws IOException {
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }
        final File kbdDir = new File(CLDRPaths.BASE_DIRECTORY, PATH_TO_SUBDIR);
        if (!kbdDir.exists()) {
            throw new IOException("Keyboards root dir doesn't exist: " + kbdDir);
        }
        final File kbdStatic = new File(kbdDir, "static");
        final File kbdStaticData = new File(kbdDir, "static/data");
        if (kbdStaticData.exists()) {
            System.out.println("Using existing Keyboard data " + kbdStaticData.getPath());
        } else {
            // attempt to build data
            System.out.println("Attempting to generate: " + kbdStaticData.getPath());
            try {
                final File nodeSubDir = new File(CLDRPaths.BASE_DIRECTORY, NODE_SUBDIR);
                if (nodeSubDir.isDirectory() && tryNodeInstall(nodeSubDir, kbdDir, kbdStaticData)) {
                    //
                } else {
                    tryNodeInstall(null, kbdDir, kbdStaticData);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.err.println(
                        "## Error, was not able to automatically build keyboard charts.");
            }
        }

        // At this point, we should already have the data
        if (!kbdStaticData.exists()) {
            System.err.println(
                    "ERROR: " + kbdStaticData + " does not exist. Keyboard charts weren't run.");
            System.err.println("See " + new File(kbdDir, "README.md") + " for help.");
            return;
        }
        final File mainChartDir = new File(mainDir, SUBDIR);
        final File staticTarg = new File(mainChartDir, "static");
        final File staticDataTarg = new File(staticTarg, "data");
        if (staticDataTarg.mkdirs()) {
            System.err.println("Created: " + staticDataTarg);
        }
        System.out.println("Copying: " + kbdStatic + " to " + staticTarg);

        Files.copy(
                new File(kbdDir, "index.html").toPath(),
                new File(mainDir, SUBDIR + "/index.html").toPath(),
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
                                // will overwrite if more than one-  but at least one will be
                                // thrown.
                                copyErr = new IOException("Error copying " + path, e);
                            }
                        });
        // rethrow any error
        if (copyErr != null) throw copyErr;
        System.out.println();
        System.out.println(
                "🖮 Success!\n🖮 Keyboard Charts updated in "
                        + kbdDir
                        + "\n🖮 and copied to "
                        + mainChartDir);
    }

    private static final boolean ON_WINDOWS =
            System.getProperty("os.name", "unknown").contains("Windows");

    private static boolean tryNodeInstall(File nodeSubDir, File kbdDir, File staticDir) {
        try {
            String npmName = ON_WINDOWS ? "npm.cmd" : "npm";
            if (nodeSubDir != null) {
                File npmExe = new File(nodeSubDir, npmName);
                if (npmExe.canExecute()) {
                    npmName = npmExe.getAbsolutePath();
                }
            }
            String nodeName = ON_WINDOWS ? "node.exe" : "node";
            if (nodeSubDir != null) {
                File nodeExe = new File(nodeSubDir, nodeName);
                if (nodeExe.canExecute()) {
                    nodeName = nodeExe.getAbsolutePath();
                }
            }
            if (ON_WINDOWS) {
                // on windows, we need to run npm in two steps
                final String cmd1[] = {npmName, "install", "--ignore-scripts=true"};
                final String cmd2[] = {nodeName, "build.mjs"};
                return tryNodeCommand(kbdDir, staticDir, cmd1)
                        && tryNodeCommand(kbdDir, staticDir, cmd2)
                        && staticDir.isDirectory();
            } else {
                final String cmd[] = {npmName, "install"};
                return tryNodeCommand(kbdDir, staticDir, cmd) && staticDir.isDirectory();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Failed to node install from " + nodeSubDir);
            return false;
        } finally {
            System.out.println("------ Done executing in " + nodeSubDir);
        }
    }

    private static boolean tryNodeCommand(File kbdDir, File staticDir, final String[] cmd)
            throws IOException, InterruptedException {
        final int timeoutSeconds = 120;
        System.out.println(
                "# Attempting: "
                        + String.join(" ", cmd)
                        + "\n# (timeout: "
                        + timeoutSeconds
                        + " seconds)");
        final Process p =
                new ProcessBuilder(cmd)
                        .directory(kbdDir)
                        .redirectError(Redirect.INHERIT)
                        .redirectOutput(Redirect.INHERIT)
                        .start();
        p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (p.isAlive()) {
            System.err.println(".. stuck");
            p.destroyForcibly();
            return false;
        }
        if (p.exitValue() != 0) {
            System.err.println(".. failed: " + p.exitValue());

            return false;
        }
        return true;
    }
}
